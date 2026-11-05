import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { PanelComponent } from '../../shared/components/panel/panel.component';
import { IconComponent } from '../../shared/components/icon/icon.component';
import { DataTableComponent, TableColumn } from '../../shared/components/data-table/data-table.component';
import { EmptyStateComponent } from '../../shared/components/empty-state/empty-state.component';
import { LogsTracesApiService, LogEntry, Trace } from '../../core/services/logs-traces-api.service';

@Component({
  selector: 'app-logs-traces',
  standalone: true,
  imports: [CommonModule, PanelComponent, IconComponent, DataTableComponent, EmptyStateComponent],
  template: `
    <div class="page">
      <app-empty-state icon="file-text" title="Application-level logs and HTTP traces"
        description="This stack has no Loki/Tempo/Jaeger backend configured, so this shows this Spring Boot process's own real log events and recent HTTP request traces — not cross-service distributed tracing." />

      <app-panel title="Recent Logs" [subtitle]="logs().length + ' of up to 500 kept in memory'">
        <div panelActions>
          <button type="button" class="btn-secondary" (click)="reload()"><app-icon name="refresh" [size]="14" /> Refresh</button>
        </div>
        @if (logs().length) {
          <app-data-table [columns]="logColumns" [rows]="logRows()" />
        } @else {
          <app-empty-state title="No log events captured yet" />
        }
      </app-panel>

      <app-panel title="Recent HTTP Traces" subtitle="Actuator httpexchanges — real requests/responses handled by this API">
        @if (traces().length) {
          <app-data-table [columns]="traceColumns" [rows]="traceRows()" />
        } @else {
          <app-empty-state title="No HTTP traces captured yet" />
        }
      </app-panel>
    </div>
  `,
  styles: [
    `
    .page { display: flex; flex-direction: column; gap: var(--space-5); }
    .btn-secondary {
      display: inline-flex; align-items: center; gap: 6px;
      padding: 7px var(--space-4); border-radius: var(--radius-sm); font-size: var(--font-size-sm); font-weight: 600; cursor: pointer;
      background: var(--color-surface-card-hover); border: 1px solid var(--color-border); color: var(--color-text-secondary);
    }
    `,
  ],
})
export class LogsTracesComponent implements OnInit {
  readonly logs = signal<LogEntry[]>([]);
  readonly traces = signal<Trace[]>([]);

  readonly logColumns: TableColumn[] = [
    { key: 'timestamp', label: 'Time' },
    { key: 'level', label: 'Level' },
    { key: 'logger', label: 'Logger' },
    { key: 'message', label: 'Message' },
  ];

  readonly traceColumns: TableColumn[] = [
    { key: 'timestamp', label: 'Time' },
    { key: 'method', label: 'Method' },
    { key: 'uri', label: 'Path' },
    { key: 'status', label: 'Status' },
    { key: 'durationMs', label: 'Duration (ms)', align: 'right' },
  ];

  constructor(private readonly api: LogsTracesApiService) {}

  ngOnInit(): void {
    this.reload();
  }

  reload(): void {
    this.api.logs().subscribe({ next: (l) => this.logs.set(l) });
    this.api.traces().subscribe({ next: (t) => this.traces.set(t) });
  }

  logRows(): Record<string, unknown>[] {
    return this.logs().map((l) => ({ ...l, logger: this.shortLogger(l.logger) }));
  }

  traceRows(): Record<string, unknown>[] {
    return this.traces().map((t) => ({ ...t, status: t.status ?? '—', durationMs: t.durationMs ?? '—' }));
  }

  shortLogger(name: string): string {
    const parts = name.split('.');
    return parts.length > 2 ? parts.slice(-2).join('.') : name;
  }
}
