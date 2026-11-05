import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { PanelComponent } from '../../shared/components/panel/panel.component';
import { BadgeComponent } from '../../shared/components/badge/badge.component';
import { IconComponent } from '../../shared/components/icon/icon.component';
import { DataTableComponent, TableColumn } from '../../shared/components/data-table/data-table.component';
import { EmptyStateComponent } from '../../shared/components/empty-state/empty-state.component';
import { DrawerComponent } from '../../shared/components/drawer/drawer.component';
import { PipelineDefinitionApiService } from '../../core/services/pipeline-definition-api.service';
import { PipelineDefinitionRow } from '../../core/services/data-source-api.service';
import { PipelineRun } from '../../core/services/pipeline-api.service';

@Component({
  selector: 'app-data-pipelines',
  standalone: true,
  imports: [CommonModule, PanelComponent, BadgeComponent, IconComponent, DataTableComponent, EmptyStateComponent, DrawerComponent],
  template: `
    <div class="page">
      <app-panel title="Pipeline Registry" subtitle="Databricks Jobs this app can trigger and track">
        @if (definitions().length) {
          <app-data-table [columns]="columns" [rows]="rows()" [clickableRows]="true" (rowClick)="openDetail($event)" />
        } @else {
          <app-empty-state icon="plug" title="No pipeline is registered"
            description="Set DATABRICKS_JOB_ID and DATABRICKS_HOST/TOKEN in the backend configuration, then restart — a default pipeline definition is seeded automatically." />
        }
      </app-panel>
    </div>

    <app-drawer [open]="detail() !== null" [title]="detail()?.name ?? ''" (close)="detail.set(null)">
      @if (detail()) {
        <div class="detail-grid">
          <div><span class="detail-label">Type</span><span>{{ detail()!.pipelineType }}</span></div>
          <div><span class="detail-label">Source</span><span>{{ detail()!.sourceName }}</span></div>
          <div><span class="detail-label">Destination</span><span>{{ detail()!.destinationName }}</span></div>
          <div><span class="detail-label">Last Run</span><span>{{ detail()!.lastRunStatus ?? 'Never run' }}</span></div>
        </div>
        <p class="description">{{ detail()!.description }}</p>
        <div class="detail-actions">
          <button type="button" class="btn-primary" (click)="trigger()" [disabled]="triggering()">
            <app-icon name="play" [size]="14" /> {{ triggering() ? 'Triggering…' : 'Trigger Run' }}
          </button>
          <button type="button" class="btn-secondary" (click)="goToMonitoring()">
            <app-icon name="bar-chart-2" [size]="14" /> Open in Pipeline Monitoring
          </button>
        </div>
        @if (triggerError()) {
          <app-empty-state icon="alert-triangle" tone="error" title="Could not trigger this pipeline" [description]="triggerError()!" />
        }

        <h4>Run History</h4>
        @if (runs().length) {
          <app-data-table [columns]="runColumns" [rows]="runRows()" />
        } @else {
          <app-empty-state title="No runs yet" />
        }
      }
    </app-drawer>
  `,
  styles: [
    `
    .page { display: flex; flex-direction: column; gap: var(--space-5); }
    .detail-grid { display: grid; grid-template-columns: 1fr 1fr; gap: var(--space-3); margin-bottom: var(--space-3); }
    .detail-label { display: block; font-size: var(--font-size-xs); color: var(--color-text-muted); margin-bottom: 2px; }
    .description { font-size: var(--font-size-sm); color: var(--color-text-secondary); }
    .detail-actions { display: flex; gap: var(--space-2); margin: var(--space-3) 0; }
    .btn-primary, .btn-secondary {
      display: inline-flex; align-items: center; gap: 6px;
      padding: 7px var(--space-4); border-radius: var(--radius-sm); font-size: var(--font-size-sm); font-weight: 600; cursor: pointer;
    }
    .btn-primary { background: var(--color-accent); border: none; color: #fff; }
    .btn-primary:disabled { opacity: 0.6; cursor: not-allowed; }
    .btn-secondary { background: var(--color-surface-card-hover); border: 1px solid var(--color-border); color: var(--color-text-secondary); }
    h4 { color: var(--color-text-primary); font-size: var(--font-size-sm); margin: var(--space-4) 0 var(--space-2); }
    `,
  ],
})
export class DataPipelinesComponent implements OnInit {
  readonly definitions = signal<PipelineDefinitionRow[]>([]);
  readonly detail = signal<PipelineDefinitionRow | null>(null);
  readonly runs = signal<PipelineRun[]>([]);
  readonly triggering = signal(false);
  readonly triggerError = signal<string | null>(null);

  readonly columns: TableColumn[] = [
    { key: 'name', label: 'Pipeline' },
    { key: 'pipelineType', label: 'Type' },
    { key: 'sourceName', label: 'Source' },
    { key: 'destinationName', label: 'Destination' },
    { key: 'lastRunStatusLabel', label: 'Last Run' },
    { key: 'lastRunAt', label: 'Last Executed' },
  ];

  readonly runColumns: TableColumn[] = [
    { key: 'databricksRunId', label: 'Databricks Run ID' },
    { key: 'lifecycleState', label: 'State' },
    { key: 'resultState', label: 'Result' },
    { key: 'startedAt', label: 'Started At' },
    { key: 'completedAt', label: 'Completed At' },
  ];

  constructor(private readonly api: PipelineDefinitionApiService, private readonly router: Router) {}

  ngOnInit(): void {
    this.reload();
  }

  reload(): void {
    this.api.list().subscribe({ next: (d) => this.definitions.set(d) });
  }

  rows(): Record<string, unknown>[] {
    return this.definitions().map((d) => ({ ...d, lastRunStatusLabel: d.lastRunStatus ?? 'Never run', lastRunAt: d.lastRunAt ?? '—' }));
  }

  runRows(): Record<string, unknown>[] {
    return this.runs().map((r) => ({ ...r, resultState: r.resultState ?? '—', completedAt: r.completedAt ?? '—' }));
  }

  openDetail(row: Record<string, any>): void {
    const def = this.definitions().find((d) => d.id === row['id']) ?? null;
    this.detail.set(def);
    this.triggerError.set(null);
    if (def) {
      this.loadRuns(def.id);
    }
  }

  loadRuns(id: number): void {
    this.api.runs(id).subscribe({ next: (r) => this.runs.set(r) });
  }

  trigger(): void {
    const def = this.detail();
    if (!def) return;
    this.triggering.set(true);
    this.triggerError.set(null);
    this.api.trigger(def.id).subscribe({
      next: () => {
        this.triggering.set(false);
        this.loadRuns(def.id);
        this.reload();
      },
      error: (err) => {
        this.triggering.set(false);
        this.triggerError.set(err?.error?.message ?? 'Databricks is unreachable or misconfigured — check DATABRICKS_HOST/TOKEN.');
      },
    });
  }

  goToMonitoring(): void {
    this.router.navigate(['/observability/pipeline-monitoring']);
  }
}
