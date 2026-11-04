package com.nexora.platform.graph;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import org.neo4j.driver.Driver;
import org.neo4j.driver.Record;
import org.neo4j.driver.Session;
import org.neo4j.driver.Value;
import org.neo4j.driver.types.Node;
import org.neo4j.driver.types.Path;
import org.neo4j.driver.types.Relationship;
import org.springframework.stereotype.Service;

/**
 * Read side of the knowledge graph: stats for the overview page, entity
 * pickers, and a bounded-depth traversal for the Graph Explorer. Angular
 * only ever reaches these endpoints, never raw Cypher.
 */
@Service
public class GraphQueryService {

    private static final Map<String, String> LABEL_BY_TYPE = Map.of(
            "customer", "Customer",
            "address", "Address",
            "product", "Product",
            "category", "ProductCategory",
            "order", "SalesOrder",
            "document", "Document",
            "dataset", "Dataset");

    private static final Map<String, String> ID_PROPERTY_BY_TYPE = Map.of(
            "customer", "customerId",
            "address", "addressId",
            "product", "productId",
            "category", "categoryId",
            "order", "salesOrderId",
            "document", "documentId",
            "dataset", "name");

    private final Driver driver;

    public GraphQueryService(Driver driver) {
        this.driver = driver;
    }

    public GraphStatsDto stats() {
        try (Session session = driver.session()) {
            long nodeCount = session.run("MATCH (n) RETURN count(n) AS c").single().get("c").asLong();
            long relCount = session.run("MATCH ()-[r]->() RETURN count(r) AS c").single().get("c").asLong();
            Map<String, Long> byLabel = new LinkedHashMap<>();
            for (Record record : session.run(
                    "MATCH (n) UNWIND labels(n) AS label RETURN label, count(*) AS c ORDER BY label").list()) {
                byLabel.put(record.get("label").asString(), record.get("c").asLong());
            }
            return new GraphStatsDto(nodeCount, relCount, byLabel);
        }
    }

    public List<GraphEntityRefDto> listEntities(String type, int limit) {
        String label = labelFor(type);
        String idProperty = idPropertyFor(type);
        String displayExpr = displayExpressionFor(type);
        String cypher = String.format(
                "MATCH (n:%s) RETURN n.%s AS id, %s AS label ORDER BY label LIMIT $limit",
                label, idProperty, displayExpr);

        try (Session session = driver.session()) {
            List<GraphEntityRefDto> refs = new ArrayList<>();
            for (Record record : session.run(cypher, Map.of("limit", limit)).list()) {
                Value idValue = record.get("id");
                if (idValue.isNull()) {
                    continue;
                }
                refs.add(new GraphEntityRefDto(String.valueOf(idValue.asObject()), record.get("label").asString()));
            }
            return refs;
        }
    }

    public GraphTraversalDto traverse(String type, String id, int depth) {
        String label = labelFor(type);
        String idProperty = idPropertyFor(type);
        int boundedDepth = Math.max(1, Math.min(depth, 3));
        Object idValue = "dataset".equals(type.toLowerCase()) ? id : Long.parseLong(id);

        // SOURCED_FROM is excluded here: every node of a type shares the same
        // Dataset node, so it's a hub that would make any two-hop traversal
        // fan out to the whole dataset instead of a meaningful entity path.
        // It stays visible on the Catalog/Lineage pages instead.
        String cypher = String.format(
                "MATCH (start:%s {%s: $id}) "
                        + "OPTIONAL MATCH path = (start)-[:PLACED|CONTAINS|BELONGS_TO|HAS_ADDRESS|SUBCATEGORY_OF|MENTIONS*1..%d]-(other) "
                        + "RETURN start, path LIMIT 300",
                label, idProperty, boundedDepth);

        Map<String, GraphNodeDto> nodes = new LinkedHashMap<>();
        Map<String, GraphEdgeDto> edges = new LinkedHashMap<>();

        try (Session session = driver.session()) {
            List<Record> result = session.run(cypher, Map.of("id", idValue)).list();
            if (result.isEmpty()) {
                throw new NoSuchElementException("Unknown entity: " + type + "/" + id);
            }
            for (Record record : result) {
                addNode(nodes, record.get("start").asNode());
                Value pathValue = record.get("path");
                if (pathValue.isNull()) {
                    continue;
                }
                Path path = pathValue.asPath();
                for (Node node : path.nodes()) {
                    addNode(nodes, node);
                }
                for (Relationship rel : path.relationships()) {
                    addEdge(edges, rel);
                }
            }
        }
        return new GraphTraversalDto(List.copyOf(nodes.values()), List.copyOf(edges.values()));
    }

    private void addNode(Map<String, GraphNodeDto> nodes, Node node) {
        String id = node.elementId();
        nodes.computeIfAbsent(id, k -> new GraphNodeDto(id, node.labels().iterator().next(), node.asMap()));
    }

    private void addEdge(Map<String, GraphEdgeDto> edges, Relationship rel) {
        String id = rel.elementId();
        edges.computeIfAbsent(id, k -> new GraphEdgeDto(
                id, rel.type(), rel.startNodeElementId(), rel.endNodeElementId(), rel.asMap()));
    }

    private String labelFor(String type) {
        String label = LABEL_BY_TYPE.get(type.toLowerCase());
        if (label == null) {
            throw new NoSuchElementException("Unknown entity type: " + type);
        }
        return label;
    }

    private String idPropertyFor(String type) {
        return ID_PROPERTY_BY_TYPE.get(type.toLowerCase());
    }

    private String displayExpressionFor(String type) {
        return switch (type.toLowerCase()) {
            case "customer" -> "coalesce(n.companyName, trim(coalesce(n.firstName,'') + ' ' + coalesce(n.lastName,'')), toString(n.customerId))";
            case "product" -> "coalesce(n.name, toString(n.productId))";
            case "category" -> "coalesce(n.name, toString(n.categoryId))";
            case "order" -> "coalesce(n.orderNumber, toString(n.salesOrderId))";
            case "document" -> "coalesce(n.filename, toString(n.documentId))";
            case "address" -> "coalesce(n.city, n.addressLine1, toString(n.addressId))";
            case "dataset" -> "n.name";
            default -> throw new NoSuchElementException("Unknown entity type: " + type);
        };
    }
}
