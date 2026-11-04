package com.nexora.platform.contextlake;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.nexora.platform.databricks.DatabricksSqlClient;

@ExtendWith(MockitoExtension.class)
class CatalogSyncServiceTest {

    @Mock
    private DatabricksSqlClient sqlClient;
    @Mock
    private CatalogDatasetRepository repository;

    private CatalogSyncService service() {
        return new CatalogSyncService(sqlClient, repository);
    }

    @Test
    void syncCreatesANewDatasetRowForATablePostgresHasNeverSeenBefore() {
        when(sqlClient.executeQuery(anyString()))
                .thenReturn(List.of(
                        List.of("customer", "Customer_ID", "bigint"),
                        List.of("customer", "Company_Name", "string")))
                .thenReturn(List.of(List.of("customer", "847")));
        when(repository.findByTableName("gold.customer")).thenReturn(Optional.empty());
        when(repository.save(any(CatalogDataset.class))).thenAnswer(inv -> inv.getArgument(0));

        int count = service().sync();

        assertThat(count).isEqualTo(1);
        ArgumentCaptor<CatalogDataset> captor = ArgumentCaptor.forClass(CatalogDataset.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getTableName()).isEqualTo("gold.customer");
        assertThat(captor.getValue().getRowCount()).isEqualTo(847L);
        assertThat(captor.getValue().getColumnsJson()).contains("Customer_ID").contains("Company_Name");
    }

    @Test
    void syncUpdatesAnExistingDatasetRowInPlaceRatherThanCreatingADuplicate() {
        CatalogDataset existing = new CatalogDataset("gold.customer", 1L, "[]", Instant.EPOCH);
        when(sqlClient.executeQuery(anyString()))
                .thenReturn(List.of(List.of("customer", "Customer_ID", "bigint")))
                .thenReturn(List.of(List.of("customer", "900")));
        when(repository.findByTableName("gold.customer")).thenReturn(Optional.of(existing));
        when(repository.save(any(CatalogDataset.class))).thenAnswer(inv -> inv.getArgument(0));

        service().sync();

        verify(repository).save(existing);
        assertThat(existing.getRowCount()).isEqualTo(900L);
    }

    @Test
    void syncIsANoOpWhenUnityCatalogReportsNoGoldTables() {
        when(sqlClient.executeQuery(anyString())).thenReturn(List.of());

        int count = service().sync();

        assertThat(count).isZero();
        verify(repository, never()).save(any(CatalogDataset.class));
    }

    @Test
    void syncDefaultsToZeroRowCountForATableMissingFromTheRowCountQuery() {
        when(sqlClient.executeQuery(anyString()))
                .thenReturn(List.of(List.of("empty_table", "Id", "bigint")))
                .thenReturn(List.of());
        when(repository.findByTableName("gold.empty_table")).thenReturn(Optional.empty());
        when(repository.save(any(CatalogDataset.class))).thenAnswer(inv -> inv.getArgument(0));

        service().sync();

        ArgumentCaptor<CatalogDataset> captor = ArgumentCaptor.forClass(CatalogDataset.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getRowCount()).isZero();
    }
}
