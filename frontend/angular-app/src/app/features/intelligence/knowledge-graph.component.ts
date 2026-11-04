import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';

import { PanelComponent } from '../../shared/components/panel/panel.component';
import { IconComponent } from '../../shared/components/icon/icon.component';
import { GraphApiService, GraphStats, GraphSyncResult } from '../../core/services/graph-api.service';

const LABEL_ICON: Record<string, string> = {
  Customer: 'users',
  Address: 'database',
  Product: 'box',
  ProductCategory: 'layers',
  SalesOrder: 'file-text',
  Document: 'file-text',
  Dataset: 'database',
};

@Component({
  selector: 'app-knowledge-graph',
  standalone: true,
  imports: [CommonModule, RouterLink, PanelComponent, IconComponent],
  templateUrl: './knowledge-graph.component.html',
  styleUrl: './knowledge-graph.component.scss',
})
export class KnowledgeGraphComponent implements OnInit {
  readonly stats = signal<GraphStats | null>(null);
  readonly syncing = signal(false);
  readonly lastSyncResult = signal<GraphSyncResult | null>(null);
  readonly errorMessage = signal<string | null>(null);

  constructor(private readonly graphApi: GraphApiService) {}

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.graphApi.stats().subscribe({
      next: (stats) => {
        this.stats.set(stats);
        this.errorMessage.set(null);
      },
      error: () => this.errorMessage.set('Could not load graph stats from the Spring Boot API.'),
    });
  }

  sync(): void {
    this.syncing.set(true);
    this.graphApi.sync().subscribe({
      next: (result) => {
        this.syncing.set(false);
        this.lastSyncResult.set(result);
        this.load();
      },
      error: () => {
        this.syncing.set(false);
        this.errorMessage.set('Failed to sync the knowledge graph from Databricks + Context Lake.');
      },
    });
  }

  get labelCards(): Array<{ label: string; count: number; icon: string }> {
    const byLabel = this.stats()?.nodeCountsByLabel ?? {};
    return Object.entries(byLabel).map(([label, count]) => ({
      label,
      count,
      icon: LABEL_ICON[label] ?? 'share',
    }));
  }
}
