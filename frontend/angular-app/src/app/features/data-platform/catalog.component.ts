import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';

import { PanelComponent } from '../../shared/components/panel/panel.component';
import { IconComponent } from '../../shared/components/icon/icon.component';
import { DataTableComponent, TableColumn } from '../../shared/components/data-table/data-table.component';
import { CatalogApiService, CatalogDataset } from '../../core/services/catalog-api.service';

@Component({
  selector: 'app-catalog',
  standalone: true,
  imports: [CommonModule, PanelComponent, IconComponent, DataTableComponent],
  templateUrl: './catalog.component.html',
  styleUrl: './catalog.component.scss',
})
export class CatalogComponent implements OnInit {
  readonly datasets = signal<CatalogDataset[]>([]);
  readonly refreshing = signal(false);
  readonly errorMessage = signal<string | null>(null);

  readonly columns: TableColumn[] = [
    { key: 'tableName', label: 'Dataset' },
    { key: 'rowCount', label: 'Rows', align: 'right' },
    { key: 'columnCount', label: 'Columns', align: 'right' },
    { key: 'columnNames', label: 'Schema' },
    { key: 'lastRefreshedAt', label: 'Last Refreshed' },
  ];

  get rows() {
    return this.datasets().map((d) => ({
      ...d,
      columnCount: d.columns.length,
      columnNames: d.columns.map((c) => c.name).join(', '),
    }));
  }

  constructor(private readonly catalogApi: CatalogApiService) {}

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.catalogApi.listDatasets().subscribe({
      next: (datasets) => {
        this.datasets.set(datasets);
        this.errorMessage.set(null);
      },
      error: () => this.errorMessage.set('Could not load the dataset catalog from the Spring Boot API.'),
    });
  }

  refresh(): void {
    this.refreshing.set(true);
    this.catalogApi.refresh().subscribe({
      next: () => {
        this.refreshing.set(false);
        this.load();
      },
      error: () => {
        this.refreshing.set(false);
        this.errorMessage.set('Failed to refresh the catalog from Databricks.');
      },
    });
  }
}
