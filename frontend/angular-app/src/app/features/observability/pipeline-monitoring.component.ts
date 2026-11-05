import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { PanelComponent } from '../../shared/components/panel/panel.component';
import { BadgeComponent, BadgeTone } from '../../shared/components/badge/badge.component';
import { IconComponent } from '../../shared/components/icon/icon.component';
import { DataTableComponent, TableColumn } from '../../shared/components/data-table/data-table.component';
import { EmptyStateComponent } from '../../shared/components/empty-state/empty-state.component';
import {
  PipelineMonitoringApiService,
  PipelineReliability,
} from '../../core/services/pipeline-monitoring-api.service';
import { PipelineRun } from '../../core/services/pipeline-api.service';

@Component({
  selector: 'app-pipeline-monitoring',
  standalone: true,
  imports: [CommonModule, PanelComponent, BadgeComponent, IconComponent, DataTableComponent, EmptyStateComponent],
  template: `
    <div class="page">
      <app-panel title="Pipeline Reliability" subtitle="Success rate per registered pipeline, from real Databricks run history">
        @if (reliability().length) {
          <div class="reliability-grid">
            @for (r of reliability(); track r.definitionId) {
              <div class="reliability-card">
                <div class="name">{{ r.name }}</div>
                <div class="rate">
                  @if (r.successRatePct !== null) {
                    <span [class]="'rate-value ' + rateClass(r.successRatePct)">{{ r.successRatePct | number: '1.0-0' }}%</span>
                  } @else {
                    <span class="rate-value none">No completed runs</span>
                  }
                </div>
                <div class="counts">{{ r.totalRuns }} runs · {{ r.successfulRuns }} ok · {{ r.failedRuns }} failed</div>
                <app-badge [tone]="toneFor(r.lastRunStatus)">{{ r.lastRunStatus ?? 'Never run' }}</app-badge>
              </div>
            }
          </div>
        } @else {
          <app-empty-state icon="bar-chart-2" title="No pipelines registered"
            description="Register a pipeline under Data Platform > Data Pipelines first." />
        }
      </app-panel>

      <app-panel title="Run History" subtitle="All Databricks job runs this app has triggered or observed">
        <div panelActions>
          <button type="button" class="btn-secondary" (click)="reload()" [disabled]="loading()">
            <app-icon name="refresh" [size]="14" /> Refresh
          </button>
        </div>
        @if (runs().length) {
          <app-data-table [columns]="runColumns" [rows]="runRows()" />
        } @else {
          <app-empty-state title="No pipeline runs yet" />
        }
      </app-panel>
    </div>
  `,
  styles: [
    `
    .page { display: flex; flex-direction: column; gap: var(--space-5); }
    .reliability-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(220px, 1fr)); gap: var(--space-4); }
    .reliability-card { display: flex; flex-direction: column; gap: 6px; padding: var(--space-3); background: var(--color-surface-card-hover); border: 1px solid var(--color-border); border-radius: var(--radius-sm); }
    .name { font-size: var(--font-size-sm); font-weight: 600; color: var(--color-text-primary); }
    .rate-value { font-size: var(--font-size-xl); font-weight: 700; }
    .rate-value.good { color: var(--color-success); }
    .rate-value.warn { color: var(--color-warning); }
    .rate-value.bad { color: var(--color-error); }
    .rate-value.none { font-size: var(--font-size-sm); color: var(--color-text-muted); font-weight: 500; }
    .counts { font-size: var(--font-size-xs); color: var(--color-text-muted); }
    .btn-secondary {
      display: inline-flex; align-items: center; gap: 6px;
      padding: 7px var(--space-4); border-radius: var(--radius-sm); font-size: var(--font-size-sm); font-weight: 600; cursor: pointer;
      background: var(--color-surface-card-hover); border: 1px solid var(--color-border); color: var(--color-text-secondary);
    }
    .btn-secondary:disabled { opacity: 0.6; cursor: not-allowed; }
    `,
  ],
})
export class PipelineMonitoringComponent implements OnInit {
  readonly reliability = signal<PipelineReliability[]>([]);
  readonly runs = signal<PipelineRun[]>([]);
  readonly loading = signal(false);

  readonly runColumns: TableColumn[] = [
    { key: 'databricksRunId', label: 'Databricks Run ID' },
    { key: 'pipelineDefinitionId', label: 'Pipeline Def. ID' },
    { key: 'lifecycleState', label: 'State' },
    { key: 'resultState', label: 'Result' },
    { key: 'triggeredBy', label: 'Triggered By' },
    { key: 'startedAt', label: 'Started At' },
    { key: 'completedAt', label: 'Completed At' },
  ];

  constructor(private readonly api: PipelineMonitoringApiService) {}

  ngOnInit(): void {
    this.reload();
  }

  reload(): void {
    this.loading.set(true);
    this.api.reliability().subscribe({ next: (r) => this.reliability.set(r) });
    this.api.runs().subscribe({
      next: (r) => {
        this.runs.set(r);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  runRows(): Record<string, unknown>[] {
    return this.runs().map((r) => ({
      ...r,
      pipelineDefinitionId: r.pipelineDefinitionId ?? '—',
      resultState: r.resultState ?? '—',
      completedAt: r.completedAt ?? '—',
    }));
  }

  rateClass(pct: number): string {
    if (pct >= 90) return 'good';
    if (pct >= 60) return 'warn';
    return 'bad';
  }

  toneFor(status: string | null): BadgeTone {
    if (!status) return 'neutral';
    if (status === 'SUCCESS') return 'success';
    if (status === 'PENDING' || status === 'RUNNING') return 'info';
    return 'error';
  }
}
