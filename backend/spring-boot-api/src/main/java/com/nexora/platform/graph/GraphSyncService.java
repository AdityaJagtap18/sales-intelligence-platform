package com.nexora.platform.graph;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;

import com.nexora.platform.contextlake.CatalogDataset;
import com.nexora.platform.contextlake.CatalogDatasetRepository;
import com.nexora.platform.contextlake.Document;
import com.nexora.platform.contextlake.DocumentRepository;
import com.nexora.platform.contextlake.QdrantClient;
import com.nexora.platform.databricks.DatabricksSqlClient;

/**
 * Populates Neo4j from the same read paths Phases 4-6 already built: Gold
 * tables via {@link DatabricksSqlClient}, and the Postgres
 * {@code catalog_dataset}/{@code document} tables. No new Databricks or
 * Postgres access path is added for this.
 */
@Service
public class GraphSyncService {

    private static final Logger log = LoggerFactory.getLogger(GraphSyncService.class);

    private static final String CUSTOMERS_SQL =
            "SELECT Customer_ID, First_Name, Last_Name, Company_Name, Email_Address FROM workspace.gold.customer";
    private static final String ADDRESSES_SQL =
            "SELECT Address_ID, Address_Line1, City, State_Province, Country_Region FROM workspace.gold.address";
    private static final String PRODUCTS_SQL =
            "SELECT Product_ID, Name, Product_Number, Color, List_Price, Product_Category_ID FROM workspace.gold.product";
    private static final String CATEGORIES_SQL =
            "SELECT Product_Category_ID, Name, Parent_Product_Category_ID FROM workspace.gold.productcategory";
    private static final String ORDERS_SQL =
            "SELECT Sales_Order_ID, Sales_Order_Number, Order_Date, Customer_ID, Total_Due FROM workspace.gold.salesorderheader";
    private static final String ORDER_LINES_SQL =
            "SELECT Sales_Order_ID, Product_ID, Order_Qty, Unit_Price FROM workspace.gold.salesorderdetail";
    private static final String CUSTOMER_ADDRESS_SQL =
            "SELECT Customer_ID, Address_ID, Address_Type FROM workspace.gold.customeraddress";

    private static final Map<String, String> DATASET_NAME_BY_LABEL = Map.of(
            "Customer", "gold.customer",
            "Address", "gold.address",
            "Product", "gold.product",
            "ProductCategory", "gold.productcategory",
            "SalesOrder", "gold.salesorderheader");

    private static final String MERGE_CUSTOMERS = "UNWIND $rows AS row MERGE (n:Customer {customerId: row.customerId}) SET n += row";
    private static final String MERGE_ADDRESSES = "UNWIND $rows AS row MERGE (n:Address {addressId: row.addressId}) SET n += row";
    private static final String MERGE_PRODUCTS = "UNWIND $rows AS row MERGE (n:Product {productId: row.productId}) SET n += row";
    private static final String MERGE_CATEGORIES = "UNWIND $rows AS row MERGE (n:ProductCategory {categoryId: row.categoryId}) SET n += row";
    private static final String MERGE_ORDERS = "UNWIND $rows AS row MERGE (n:SalesOrder {salesOrderId: row.salesOrderId}) SET n += row";
    private static final String MERGE_DATASETS = "UNWIND $rows AS row MERGE (n:Dataset {name: row.name}) SET n += row";
    private static final String MERGE_DOCUMENTS = "UNWIND $rows AS row MERGE (n:Document {documentId: row.documentId}) SET n += row";

    private static final String REL_PLACED = """
            UNWIND $rows AS row
            MATCH (c:Customer {customerId: row.customerId})
            MATCH (o:SalesOrder {salesOrderId: row.salesOrderId})
            MERGE (c)-[:PLACED]->(o)
            """;
    private static final String REL_CONTAINS = """
            UNWIND $rows AS row
            MATCH (o:SalesOrder {salesOrderId: row.salesOrderId})
            MATCH (p:Product {productId: row.productId})
            MERGE (o)-[r:CONTAINS]->(p)
            SET r.quantity = row.quantity, r.unitPrice = row.unitPrice
            """;
    private static final String REL_BELONGS_TO = """
            UNWIND $rows AS row
            MATCH (p:Product {productId: row.productId})
            MATCH (cat:ProductCategory {categoryId: row.categoryId})
            MERGE (p)-[:BELONGS_TO]->(cat)
            """;
    private static final String REL_HAS_ADDRESS = """
            UNWIND $rows AS row
            MATCH (c:Customer {customerId: row.customerId})
            MATCH (a:Address {addressId: row.addressId})
            MERGE (c)-[r:HAS_ADDRESS]->(a)
            SET r.addressType = row.addressType
            """;
    private static final String REL_SUBCATEGORY_OF = """
            UNWIND $rows AS row
            MATCH (cat:ProductCategory {categoryId: row.categoryId})
            MATCH (parent:ProductCategory {categoryId: row.parentCategoryId})
            MERGE (cat)-[:SUBCATEGORY_OF]->(parent)
            """;
    private static final String REL_MENTIONS_PRODUCT = """
            UNWIND $rows AS row
            MATCH (doc:Document {documentId: row.documentId})
            MATCH (p:Product {productId: row.entityId})
            MERGE (doc)-[:MENTIONS]->(p)
            """;
    private static final String REL_MENTIONS_CATEGORY = """
            UNWIND $rows AS row
            MATCH (doc:Document {documentId: row.documentId})
            MATCH (cat:ProductCategory {categoryId: row.entityId})
            MERGE (doc)-[:MENTIONS]->(cat)
            """;

    private final DatabricksSqlClient sqlClient;
    private final DocumentRepository documentRepository;
    private final CatalogDatasetRepository catalogDatasetRepository;
    private final QdrantClient qdrantClient;
    private final Driver driver;

    public GraphSyncService(DatabricksSqlClient sqlClient, DocumentRepository documentRepository,
            CatalogDatasetRepository catalogDatasetRepository, QdrantClient qdrantClient, Driver driver) {
        this.sqlClient = sqlClient;
        this.documentRepository = documentRepository;
        this.catalogDatasetRepository = catalogDatasetRepository;
        this.qdrantClient = qdrantClient;
        this.driver = driver;
    }

    /**
     * Without a uniqueness constraint, MERGE has no index to lock on, so two
     * concurrent syncs (the scheduled one and a manual POST /graph/sync, say)
     * can each pass the "does this node exist" check before either commits
     * and create duplicates. The constraint also backs every MERGE with an
     * index instead of a full node scan.
     */
    @PostConstruct
    public void ensureConstraints() {
        try (Session session = driver.session()) {
            session.executeWrite(tx -> {
                tx.run("CREATE CONSTRAINT customer_id IF NOT EXISTS FOR (n:Customer) REQUIRE n.customerId IS UNIQUE");
                tx.run("CREATE CONSTRAINT address_id IF NOT EXISTS FOR (n:Address) REQUIRE n.addressId IS UNIQUE");
                tx.run("CREATE CONSTRAINT product_id IF NOT EXISTS FOR (n:Product) REQUIRE n.productId IS UNIQUE");
                tx.run("CREATE CONSTRAINT category_id IF NOT EXISTS FOR (n:ProductCategory) REQUIRE n.categoryId IS UNIQUE");
                tx.run("CREATE CONSTRAINT order_id IF NOT EXISTS FOR (n:SalesOrder) REQUIRE n.salesOrderId IS UNIQUE");
                tx.run("CREATE CONSTRAINT document_id IF NOT EXISTS FOR (n:Document) REQUIRE n.documentId IS UNIQUE");
                tx.run("CREATE CONSTRAINT dataset_name IF NOT EXISTS FOR (n:Dataset) REQUIRE n.name IS UNIQUE");
                return null;
            });
        } catch (Exception e) {
            log.warn("Failed to ensure Neo4j constraints: {}", e.getMessage());
        }
    }

    @Scheduled(fixedDelayString = "${graph.sync-interval-ms:600000}", initialDelay = 20000)
    public void scheduledSync() {
        try {
            sync();
        } catch (Exception e) {
            log.warn("Failed to sync the knowledge graph: {}", e.getMessage());
        }
    }

    public GraphSyncResultDto sync() {
        List<Map<String, Object>> customers = new ArrayList<>();
        for (List<String> r : sqlClient.executeQuery(CUSTOMERS_SQL)) {
            customers.add(nodeMap(
                    "customerId", longAt(r, 0), "firstName", strAt(r, 1), "lastName", strAt(r, 2),
                    "companyName", strAt(r, 3), "email", strAt(r, 4)));
        }

        List<Map<String, Object>> addresses = new ArrayList<>();
        for (List<String> r : sqlClient.executeQuery(ADDRESSES_SQL)) {
            addresses.add(nodeMap(
                    "addressId", longAt(r, 0), "addressLine1", strAt(r, 1), "city", strAt(r, 2),
                    "stateProvince", strAt(r, 3), "country", strAt(r, 4)));
        }

        List<Map<String, Object>> products = new ArrayList<>();
        for (List<String> r : sqlClient.executeQuery(PRODUCTS_SQL)) {
            products.add(nodeMap(
                    "productId", longAt(r, 0), "name", strAt(r, 1), "productNumber", strAt(r, 2),
                    "color", strAt(r, 3), "listPrice", doubleAt(r, 4), "categoryId", longFromDouble(r, 5)));
        }

        List<Map<String, Object>> categories = new ArrayList<>();
        for (List<String> r : sqlClient.executeQuery(CATEGORIES_SQL)) {
            categories.add(nodeMap(
                    "categoryId", longAt(r, 0), "name", strAt(r, 1), "parentCategoryId", longFromDouble(r, 2)));
        }

        List<Map<String, Object>> orders = new ArrayList<>();
        for (List<String> r : sqlClient.executeQuery(ORDERS_SQL)) {
            orders.add(nodeMap(
                    "salesOrderId", longAt(r, 0), "orderNumber", strAt(r, 1), "orderDate", strAt(r, 2),
                    "customerId", longAt(r, 3), "totalDue", doubleAt(r, 4)));
        }

        List<Map<String, Object>> orderLines = new ArrayList<>();
        for (List<String> r : sqlClient.executeQuery(ORDER_LINES_SQL)) {
            orderLines.add(nodeMap(
                    "salesOrderId", longAt(r, 0), "productId", longAt(r, 1), "quantity", longAt(r, 2),
                    "unitPrice", doubleAt(r, 3)));
        }

        List<Map<String, Object>> customerAddresses = new ArrayList<>();
        for (List<String> r : sqlClient.executeQuery(CUSTOMER_ADDRESS_SQL)) {
            customerAddresses.add(nodeMap(
                    "customerId", longAt(r, 0), "addressId", longAt(r, 1), "addressType", strAt(r, 2)));
        }

        List<Map<String, Object>> datasets = new ArrayList<>();
        for (CatalogDataset d : catalogDatasetRepository.findAll()) {
            datasets.add(nodeMap("name", d.getTableName(), "rowCount", d.getRowCount()));
        }

        List<Map<String, Object>> documents = new ArrayList<>();
        for (Document d : documentRepository.findAll()) {
            if (Document.STATUS_INDEXED.equals(d.getStatus())) {
                documents.add(nodeMap("documentId", d.getId(), "filename", d.getFilename(), "chunkCount", d.getChunkCount()));
            }
        }

        try (Session session = driver.session()) {
            session.executeWrite(tx -> {
                tx.run(MERGE_CUSTOMERS, Map.of("rows", customers));
                tx.run(MERGE_ADDRESSES, Map.of("rows", addresses));
                tx.run(MERGE_PRODUCTS, Map.of("rows", products));
                tx.run(MERGE_CATEGORIES, Map.of("rows", categories));
                tx.run(MERGE_ORDERS, Map.of("rows", orders));
                tx.run(MERGE_DATASETS, Map.of("rows", datasets));
                tx.run(MERGE_DOCUMENTS, Map.of("rows", documents));

                tx.run(REL_PLACED, Map.of("rows", orders));
                tx.run(REL_CONTAINS, Map.of("rows", orderLines));
                tx.run(REL_BELONGS_TO, Map.of("rows", products.stream().filter(m -> m.containsKey("categoryId")).toList()));
                tx.run(REL_HAS_ADDRESS, Map.of("rows", customerAddresses));
                tx.run(REL_SUBCATEGORY_OF, Map.of("rows", categories.stream().filter(m -> m.containsKey("parentCategoryId")).toList()));

                for (Map.Entry<String, String> entry : DATASET_NAME_BY_LABEL.entrySet()) {
                    tx.run("MATCH (d:Dataset {name: $name}) MATCH (n:" + entry.getKey() + ") MERGE (n)-[:SOURCED_FROM]->(d)",
                            Map.of("name", entry.getValue()));
                }
                return null;
            });

            int mentions = syncMentions(session, products, categories);

            log.info("Knowledge graph sync complete: {} customers, {} addresses, {} products, {} categories, "
                            + "{} orders, {} order lines, {} documents, {} mentions",
                    customers.size(), addresses.size(), products.size(), categories.size(),
                    orders.size(), orderLines.size(), documents.size(), mentions);

            return new GraphSyncResultDto(customers.size(), addresses.size(), products.size(), categories.size(),
                    orders.size(), orderLines.size(), documents.size(), mentions);
        }
    }

    /**
     * Links each Context Lake document to the products/categories its chunk
     * text names — a plain case-insensitive substring match, not a new NLP
     * dependency, per the Phase 7 scope.
     */
    private int syncMentions(Session session, List<Map<String, Object>> products, List<Map<String, Object>> categories) {
        List<Map<String, Object>> chunks = qdrantClient.scrollAll();
        if (chunks.isEmpty()) {
            return 0;
        }

        List<Map<String, Object>> productMentions = new ArrayList<>();
        List<Map<String, Object>> categoryMentions = new ArrayList<>();

        for (Map<String, Object> chunk : chunks) {
            Object payloadObj = chunk.get("payload");
            if (!(payloadObj instanceof Map<?, ?> payload)) {
                continue;
            }
            Object textObj = payload.get("text");
            Object docIdObj = payload.get("documentId");
            if (textObj == null || docIdObj == null) {
                continue;
            }
            String text = textObj.toString().toLowerCase();
            long documentId = Long.parseLong(docIdObj.toString());

            for (Map<String, Object> product : products) {
                String name = (String) product.get("name");
                if (name != null && name.length() > 3 && text.contains(name.toLowerCase())) {
                    productMentions.add(Map.of("documentId", documentId, "entityId", product.get("productId")));
                }
            }
            for (Map<String, Object> category : categories) {
                String name = (String) category.get("name");
                if (name != null && name.length() > 3 && text.contains(name.toLowerCase())) {
                    categoryMentions.add(Map.of("documentId", documentId, "entityId", category.get("categoryId")));
                }
            }
        }

        List<Map<String, Object>> dedupedProductMentions = productMentions.stream().distinct().toList();
        List<Map<String, Object>> dedupedCategoryMentions = categoryMentions.stream().distinct().toList();

        if (dedupedProductMentions.isEmpty() && dedupedCategoryMentions.isEmpty()) {
            return 0;
        }

        session.executeWrite(tx -> {
            if (!dedupedProductMentions.isEmpty()) {
                tx.run(REL_MENTIONS_PRODUCT, Map.of("rows", dedupedProductMentions));
            }
            if (!dedupedCategoryMentions.isEmpty()) {
                tx.run(REL_MENTIONS_CATEGORY, Map.of("rows", dedupedCategoryMentions));
            }
            return null;
        });

        return dedupedProductMentions.size() + dedupedCategoryMentions.size();
    }

    private static Map<String, Object> nodeMap(Object... kv) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) {
            if (kv[i + 1] != null) {
                map.put((String) kv[i], kv[i + 1]);
            }
        }
        return map;
    }

    private static String strAt(List<String> row, int i) {
        return row.get(i);
    }

    private static Long longAt(List<String> row, int i) {
        String v = row.get(i);
        return v == null ? null : Long.parseLong(v);
    }

    private static Double doubleAt(List<String> row, int i) {
        String v = row.get(i);
        return v == null ? null : Double.parseDouble(v);
    }

    /** Some nullable FK columns come back from Databricks as DOUBLE, not LONG. */
    private static Long longFromDouble(List<String> row, int i) {
        String v = row.get(i);
        return v == null ? null : (long) Double.parseDouble(v);
    }
}
