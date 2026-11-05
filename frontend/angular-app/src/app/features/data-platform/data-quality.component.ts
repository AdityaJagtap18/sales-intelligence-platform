import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { KpiCardComponent } from '../../shared/components/kpi-card/kpi-card.component';
import { PanelComponent } from '../../shared/components/panel/panel.component';
import { BadgeComponent } from '../../shared/components/badge/badge.component';
import { IconComponent } from '../../shared/components/icon/icon.component';
import { DataTableComponent, TableColumn } from '../../shared/components/data-table/data-table.component';
import { EmptyStateComponent } from '../../shared/components/empty-state/empty-state.component';
import { DataQualityApiService, QualitySummary, QualityRunHistory } from '../../core/services/data-quality-api.service';

@Component({
  selector: 'app-data-quality',
  standalone: true,
  imports: [CommonModule, KpiCardComponent, PanelComponent, BadgeComponent, IconComponent, DataTableComponent, EmptyStateComponent],
  template: `
    <div class="page">
      @if (loadError()) {
        <app-empty-state icon="alert-triangle" tone="error" title="Could not load data quality results"
          [description]="loadError()!" />
      } @else if (!summary() || summary()!.runId === null) {
        <app-empty-state icon="shield" title="No data quality runs found yet"
          description="workspace.nexora.data_quality_report has no rows. Execute checks below to run the Bronze-to-Silver pipeline that produces them." />
        <div class="toolbar">
          <button type="button" class="btn-primary" (click)="execute()" [disabled]="executing()">
            {{ executing() ? 'Running…' : 'Execute Checks' }}
          </button>
        </div>
      } @else {
        <div class="kpi-grid">
          <app-kpi-card label="Quality Score" [value]="(summary()!.qualityScorePct ?? 0) + '%'" icon="shield" [deltaLabel]="'Run ' + summary()!.runId" />
          <app-kpi-card label="Total Checks" [value]="summary()!.totalChecks.toString()" icon="activity" deltaLabel="latest run" />
          <app-kpi-card label="Passed" [value]="summary()!.passed.toString()" icon="trending-up" deltaLabel="checks" />
          <app-kpi-card label="Failed" [value]="summary()!.failed.toString()" icon="alert-triangle" deltaLabel="checks" />
          <app-kpi-card label="Skipped" [value]="summary()!.skipped.toString()" icon="box" deltaLabel="checks" />
        </div>

        <app-panel title="Dataset Status">
          <div panelActions>
            <button type="button" class="btn-primary" (click)="execute()" [disabled]="executing()">
              {{ executing() ? 'Running…' : 'Execute Checks' }}
            </button>
            <a class="btn-export" [href]="exportUrl()" download><app-icon name="download" [size]="14" /> Export CSV</a>
          </div>
          @if (summary()!.byDataset.length) {
            <app-data-table [columns]="datasetColumns" [rows]="datasetRows()" />
          } @else {
            <app-empty-state title="No datasets in this run" />
          }
        </app-panel>

        <app-panel title="Check Details">
          @if (summary()!.checks.length) {
            <app-data-table [columns]="checkColumns" [rows]="summary()!.checks" />
          } @else {
            <app-empty-state title="No checks recorded" />
          }
        </app-panel>
      }

      <app-panel title="Quality Run History">
        @if (history().length) {
          <app-data-table [columns]="historyColumns" [rows]="history()" />
        } @else {
          <app-empty-state title="No run history" />
        }
      </app-panel>
    </div>
  `,
  styles: [
    `
    .page { display: flex; flex-direction: column; gap: var(--space-5); }
    .kpi-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(170px, 1fr)); gap: var(--space-4); }
    .toolbar { margin-top: var(--space-3); display: flex; justify-content: center; }
    .btn-primary, .btn-export {
      display: inline-flex; align-items: center; gap: 6px;
      padding: 7px var(--space-4); border-radius: var(--radius-sm); font-size: var(--font-size-sm); font-weight: 600; cursor: pointer;
    }
    .btn-primary { background: var(--color-accent); border: none; color: #fff; margin-right: var(--space-2); }
    .btn-primary:disabled { opacity: 0.6; cursor: not-allowed; }
    .btn-export { color: var(--color-accent); text-decoration: none; background: transparent; border: none; }
    `,
  ],
})
export class DataQualityComponent implements OnInit {
  readonly summary = signal<QualitySummary | null>(null);
  readonly history = signal<QualityRunHistory[]>([]);
  readonly executing = signal(false);
  readonly loadError = signal<string | null>(null);

  readonly datasetColumns: TableColumn[] = [
    { key: 'tableName', label: 'Table' },
    { key: 'total', label: 'Checks', align: 'right' },
    { key: 'passed', label: 'Passed', align: 'right' },
    { key: 'failed', label: 'Failed', align: 'right' },
    { key: 'skipped', label: 'Skipped', align: 'right' },
    { key: 'overallStatus', label: 'Status' },
  ];

  readonly checkColumns: TableColumn[] = [
    { key: 'tableName', label: 'Table' },
    { key: 'checkName', label: 'Check' },
    { key: 'status', label: 'Status' },
  ];

  readonly historyColumns: TableColumn[] = [
    { key: 'runId', label: 'Run ID' },
    { key: 'totalChecks', label: 'Checks', align: 'right' },
    { key: 'passed', label: 'Passed', align: 'right' },
    { key: 'failed', label: 'Failed', align: 'right' },
    { key: 'skipped', label: 'Skipped', align: 'right' },
  ];

  constructor(private readonly api: DataQualityApiService, private readonly router: Router) {}

  ngOnInit(): void {
    this.reload();
  }

  reload(): void {
    this.api.latest().subscribe({
      next: (s) => this.summary.set(s),
      error: () => this.loadError.set('Could not reach the Spring Boot API or the Databricks SQL Warehouse.'),
    });
    this.api.history().subscribe({ next: (h) => this.history.set(h) });
  }

  datasetRows(): Record<string, unknown>[] {
    return (this.summary()?.byDataset ?? []) as unknown as Record<string, unknown>[];
  }

  execute(): void {
    this.executing.set(true);
    this.api.execute().subscribe({
      next: () => {
        this.executing.set(false);
        this.router.navigate(['/observability/pipeline-monitoring']);
      },
      error: () => this.executing.set(false),
    });
  }

  exportUrl(): string {
    return this.api.exportUrl();
  }
}
