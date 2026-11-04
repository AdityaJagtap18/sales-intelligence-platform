import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { PanelComponent } from '../../shared/components/panel/panel.component';
import { BadgeComponent } from '../../shared/components/badge/badge.component';
import { IconComponent } from '../../shared/components/icon/icon.component';
import { DataTableComponent, TableColumn } from '../../shared/components/data-table/data-table.component';
import { EmptyStateComponent } from '../../shared/components/empty-state/empty-state.component';
import { ReportsApiService, ReportDefinition, ReportResult, ReportExecution } from '../../core/services/reports-api.service';

@Component({
  selector: 'app-reports',
  standalone: true,
  imports: [CommonModule, PanelComponent, BadgeComponent, IconComponent, DataTableComponent, EmptyStateComponent],
  template: `
    <div class="page">
      <app-panel title="Available Reports">
        <div class="report-grid">
          @for (def of definitions(); track def.key) {
            <button type="button" class="report-card" [class.active]="activeKey() === def.key" (click)="generate(def.key)">
              <span class="report-category">{{ def.category }}</span>
              <span class="report-name">{{ def.name }}</span>
              <span class="report-description">{{ def.description }}</span>
            </button>
          }
        </div>
      </app-panel>

      @if (generating()) {
        <app-panel title="Generating report…"><app-empty-state title="Running report against live data…" /></app-panel>
      } @else if (result()) {
        <app-panel [title]="result()!.reportName">
          <div panelActions>
            <button type="button" class="btn-secondary" (click)="goToSalesDashboard()">
              <app-icon name="bar-chart" [size]="14" /> Open in Sales Dashboard
            </button>
            <a class="btn-export" [href]="exportUrl()" download>
              <app-icon name="download" [size]="14" /> Export CSV
            </a>
          </div>
          @if (result()!.status === 'FAILED') {
            <app-empty-state icon="alert-triangle" tone="error" title="Report generation failed" [description]="result()!.errorMessage ?? ''" />
          } @else if (result()!.rows.length) {
            <app-data-table [columns]="resultColumns()" [rows]="result()!.rows" />
          } @else {
            <app-empty-state title="This report returned no rows for the current data" />
          }
        </app-panel>
      }

      <app-panel title="Report Execution History">
        @if (history().length) {
          <app-data-table [columns]="historyColumns" [rows]="history()" />
        } @else {
          <app-empty-state title="No reports generated yet" />
        }
      </app-panel>
    </div>
  `,
  styles: [
    `
    .page { display: flex; flex-direction: column; gap: var(--space-5); }
    .report-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(220px, 1fr)); gap: var(--space-3); }
    .report-card {
      display: flex; flex-direction: column; gap: 4px; text-align: left;
      background: var(--color-surface-card); border: 1px solid var(--color-border); border-radius: var(--radius-md);
      padding: var(--space-4); cursor: pointer; color: var(--color-text-primary);
    }
    .report-card:hover { border-color: var(--color-border-strong); }
    .report-card.active { border-color: var(--color-accent); }
    .report-category { font-size: var(--font-size-xs); color: var(--color-accent); font-weight: 600; text-transform: uppercase; }
    .report-name { font-size: var(--font-size-base); font-weight: 600; }
    .report-description { font-size: var(--font-size-sm); color: var(--color-text-secondary); }
    .btn-secondary, .btn-export {
      display: inline-flex; align-items: center; gap: 6px; font-size: var(--font-size-sm); font-weight: 600;
      padding: 6px var(--space-3); border-radius: var(--radius-sm); cursor: pointer; text-decoration: none; margin-left: var(--space-2);
    }
    .btn-secondary { background: var(--color-surface-card-hover); border: 1px solid var(--color-border); color: var(--color-text-secondary); }
    .btn-export { color: var(--color-accent); border: none; background: transparent; }
    `,
  ],
})
export class ReportsComponent implements OnInit {
  readonly definitions = signal<ReportDefinition[]>([]);
  readonly result = signal<ReportResult | null>(null);
  readonly history = signal<ReportExecution[]>([]);
  readonly generating = signal(false);
  readonly activeKey = signal<string | null>(null);

  readonly historyColumns: TableColumn[] = [
    { key: 'reportName', label: 'Report' },
    { key: 'status', label: 'Status' },
    { key: 'rowCount', label: 'Rows', align: 'right' },
    { key: 'generatedAt', label: 'Generated At' },
  ];

  constructor(private readonly api: ReportsApiService, private readonly router: Router) {}

  ngOnInit(): void {
    this.api.definitions().subscribe({ next: (d) => this.definitions.set(d) });
    this.loadHistory();
  }

  generate(key: string): void {
    this.activeKey.set(key);
    this.generating.set(true);
    this.api.generate(key).subscribe({
      next: (r) => {
        this.result.set(r);
        this.generating.set(false);
        this.loadHistory();
      },
      error: () => this.generating.set(false),
    });
  }

  loadHistory(): void {
    this.api.history().subscribe({ next: (h) => this.history.set(h) });
  }

  resultColumns(): TableColumn[] {
    return (this.result()?.columns ?? []).map((c) => ({ key: c, label: c }));
  }

  exportUrl(): string {
    return this.result() ? this.api.exportUrl(this.result()!.reportKey) : '';
  }

  goToSalesDashboard(): void {
    this.router.navigate(['/overview/sales-dashboard']);
  }
}
