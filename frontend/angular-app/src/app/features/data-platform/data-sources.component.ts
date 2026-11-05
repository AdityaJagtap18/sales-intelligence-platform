import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { PanelComponent } from '../../shared/components/panel/panel.component';
import { BadgeComponent } from '../../shared/components/badge/badge.component';
import { IconComponent } from '../../shared/components/icon/icon.component';
import { DataTableComponent, TableColumn } from '../../shared/components/data-table/data-table.component';
import { EmptyStateComponent } from '../../shared/components/empty-state/empty-state.component';
import { DrawerComponent } from '../../shared/components/drawer/drawer.component';
import { ConfirmDialogComponent } from '../../shared/components/confirm-dialog/confirm-dialog.component';
import {
  DataSourceApiService,
  DataSourceRow,
  PipelineDefinitionRow,
} from '../../core/services/data-source-api.service';

@Component({
  selector: 'app-data-sources',
  standalone: true,
  imports: [CommonModule, FormsModule, PanelComponent, BadgeComponent, IconComponent, DataTableComponent, EmptyStateComponent, DrawerComponent, ConfirmDialogComponent],
  template: `
    <div class="page">
      <app-panel title="Registered Sources">
        <div panelActions>
          <button type="button" class="btn-primary" (click)="openCreate()">
            <app-icon name="grid-plus" [size]="14" /> Register Source
          </button>
        </div>
        @if (sources().length) {
          <app-data-table [columns]="columns" [rows]="rows()" [clickableRows]="true" (rowClick)="openDetail($event)" />
        } @else {
          <app-empty-state title="No data sources registered yet" description="Register Databricks, Postgres, or file-based sources here." />
        }
      </app-panel>
    </div>

    <app-drawer [open]="formOpen()" [title]="editing() ? 'Edit Source' : 'Register Source'" (close)="formOpen.set(false)">
      <form (ngSubmit)="save()">
        <label>Name</label>
        <input type="text" [(ngModel)]="form.name" name="name" required />
        <label>Type</label>
        <select [(ngModel)]="form.sourceType" name="sourceType" [disabled]="!!editing()">
          <option value="DATABRICKS">Databricks</option>
          <option value="POSTGRESQL">PostgreSQL</option>
          <option value="CSV">CSV</option>
          <option value="JSON">JSON</option>
          <option value="PARQUET">Parquet</option>
        </select>
        <label>Configuration (JSON, secrets are masked on read)</label>
        <textarea [(ngModel)]="form.connectionConfigJson" name="config" rows="4" placeholder='&#123;"host": "...", "password": "..."&#125;'></textarea>
        <div class="form-actions">
          <button type="button" class="btn-secondary" (click)="formOpen.set(false)">Cancel</button>
          <button type="submit" class="btn-primary">Save</button>
        </div>
      </form>
    </app-drawer>

    <app-drawer [open]="detail() !== null" [title]="detail()?.name ?? ''" (close)="detail.set(null)">
      @if (detail()) {
        <div class="detail-grid">
          <div><span class="detail-label">Type</span><span>{{ detail()!.sourceType }}</span></div>
          <div><span class="detail-label">Status</span><app-badge [tone]="tone(detail()!.enabled)">{{ detail()!.enabled ? 'Enabled' : 'Disabled' }}</app-badge></div>
          <div><span class="detail-label">Last Test</span><span>{{ detail()!.lastTestStatus ?? 'Never tested' }}</span></div>
          <div><span class="detail-label">Last Tested At</span><span>{{ detail()!.lastTestedAt ?? '—' }}</span></div>
        </div>
        <p class="config-summary">{{ detail()!.configSummary }}</p>
        @if (detail()!.lastTestMessage) {
          <p class="test-message">{{ detail()!.lastTestMessage }}</p>
        }
        <div class="detail-actions">
          <button type="button" class="btn-secondary" (click)="testConnection(detail()!.id)">Test Connection</button>
          <button type="button" class="btn-secondary" (click)="openEdit(detail()!)">Edit</button>
          @if (detail()!.enabled) {
            <button type="button" class="btn-danger" (click)="confirmDisable.set(detail()!.id)">Disable</button>
          } @else {
            <button type="button" class="btn-secondary" (click)="enable(detail()!.id)">Enable</button>
          }
        </div>
        <h4>Associated Pipelines</h4>
        @if (associatedPipelines().length) {
          <ul class="pipeline-list">
            @for (p of associatedPipelines(); track p.id) { <li>{{ p.name }}</li> }
          </ul>
        } @else {
          <app-empty-state title="No pipelines reference this source yet" />
        }
      }
    </app-drawer>

    <app-confirm-dialog
      [open]="confirmDisable() !== null"
      title="Disable this source?"
      message="Disabling stops it from being used by new pipeline runs. It can be re-enabled at any time."
      confirmLabel="Disable"
      (cancel)="confirmDisable.set(null)"
      (confirm)="disable()"
    />
  `,
  styles: [
    `
    .page { display: flex; flex-direction: column; gap: var(--space-5); }
    .btn-primary, .btn-secondary, .btn-danger {
      display: inline-flex; align-items: center; gap: 6px;
      padding: 7px var(--space-4); border-radius: var(--radius-sm); font-size: var(--font-size-sm); font-weight: 600; cursor: pointer;
    }
    .btn-primary { background: var(--color-accent); border: none; color: #fff; }
    .btn-secondary { background: var(--color-surface-card-hover); border: 1px solid var(--color-border); color: var(--color-text-secondary); }
    .btn-danger { background: var(--color-error); border: none; color: #fff; }
    form { display: flex; flex-direction: column; gap: var(--space-2); }
    label { font-size: var(--font-size-xs); color: var(--color-text-secondary); margin-top: var(--space-2); }
    input, select, textarea {
      background: var(--color-surface-card); border: 1px solid var(--color-border); border-radius: var(--radius-sm);
      color: var(--color-text-primary); padding: 7px var(--space-3); font-size: var(--font-size-sm);
    }
    .form-actions { display: flex; justify-content: flex-end; gap: var(--space-2); margin-top: var(--space-4); }
    .detail-grid { display: grid; grid-template-columns: 1fr 1fr; gap: var(--space-3); margin-bottom: var(--space-3); }
    .detail-label { display: block; font-size: var(--font-size-xs); color: var(--color-text-muted); margin-bottom: 2px; }
    .config-summary { font-size: var(--font-size-sm); color: var(--color-text-secondary); word-break: break-word; background: var(--color-surface-card); padding: var(--space-2); border-radius: var(--radius-sm); }
    .test-message { font-size: var(--font-size-sm); color: var(--color-text-secondary); }
    .detail-actions { display: flex; gap: var(--space-2); margin: var(--space-3) 0; }
    h4 { color: var(--color-text-primary); font-size: var(--font-size-sm); margin: var(--space-3) 0 var(--space-2); }
    .pipeline-list { margin: 0; padding-left: var(--space-4); color: var(--color-text-secondary); font-size: var(--font-size-sm); }
    `,
  ],
})
export class DataSourcesComponent implements OnInit {
  readonly sources = signal<DataSourceRow[]>([]);
  readonly detail = signal<DataSourceRow | null>(null);
  readonly formOpen = signal(false);
  readonly editing = signal<DataSourceRow | null>(null);
  readonly confirmDisable = signal<number | null>(null);
  readonly associatedPipelines = signal<PipelineDefinitionRow[]>([]);

  form = { name: '', sourceType: 'DATABRICKS', connectionConfigJson: '' };

  readonly columns: TableColumn[] = [
    { key: 'name', label: 'Name' },
    { key: 'sourceType', label: 'Type' },
    { key: 'enabledLabel', label: 'Status' },
    { key: 'lastTestStatus', label: 'Last Test' },
    { key: 'lastSuccessfulIngestionAt', label: 'Last Ingestion' },
  ];

  constructor(private readonly api: DataSourceApiService) {}

  ngOnInit(): void {
    this.reload();
  }

  reload(): void {
    this.api.list().subscribe({ next: (s) => this.sources.set(s) });
  }

  rows(): Record<string, unknown>[] {
    return this.sources().map((s) => ({
      ...s,
      enabledLabel: s.enabled ? 'Enabled' : 'Disabled',
      lastTestStatus: s.lastTestStatus ?? 'Never tested',
      lastSuccessfulIngestionAt: s.lastSuccessfulIngestionAt ?? '—',
    }));
  }

  openCreate(): void {
    this.editing.set(null);
    this.form = { name: '', sourceType: 'DATABRICKS', connectionConfigJson: '' };
    this.formOpen.set(true);
  }

  openEdit(source: DataSourceRow): void {
    this.editing.set(source);
    this.form = { name: source.name, sourceType: source.sourceType, connectionConfigJson: '' };
    this.formOpen.set(true);
  }

  save(): void {
    const req = { name: this.form.name, sourceType: this.form.sourceType, connectionConfigJson: this.form.connectionConfigJson || undefined };
    const editing = this.editing();
    const obs = editing ? this.api.update(editing.id, req) : this.api.register(req);
    obs.subscribe({
      next: () => {
        this.formOpen.set(false);
        this.reload();
      },
    });
  }

  openDetail(row: Record<string, any>): void {
    const source = this.sources().find((s) => s.id === row['id']) ?? null;
    this.detail.set(source);
    if (source) {
      this.api.pipelines(source.id).subscribe({ next: (p) => this.associatedPipelines.set(p) });
    }
  }

  testConnection(id: number): void {
    this.api.testConnection(id).subscribe({
      next: (updated) => {
        this.detail.set(updated);
        this.reload();
      },
    });
  }

  enable(id: number): void {
    this.api.enable(id).subscribe({ next: (u) => { this.detail.set(u); this.reload(); } });
  }

  disable(): void {
    const id = this.confirmDisable();
    if (id == null) return;
    this.api.disable(id).subscribe({
      next: (u) => {
        this.detail.set(u);
        this.confirmDisable.set(null);
        this.reload();
      },
    });
  }

  tone(enabled: boolean): 'success' | 'neutral' {
    return enabled ? 'success' : 'neutral';
  }
}
