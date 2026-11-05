import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { PanelComponent } from '../../shared/components/panel/panel.component';
import { BadgeComponent, BadgeTone } from '../../shared/components/badge/badge.component';
import { IconComponent } from '../../shared/components/icon/icon.component';
import { EmptyStateComponent } from '../../shared/components/empty-state/empty-state.component';
import { AlertsApiService, Alert } from '../../core/services/alerts-api.service';

@Component({
  selector: 'app-alerts',
  standalone: true,
  imports: [CommonModule, PanelComponent, BadgeComponent, IconComponent, EmptyStateComponent],
  template: `
    <div class="page">
      <app-panel title="Alerts" subtitle="Generated from real pipeline failures, data quality checks, anomalies and system health — re-evaluated every minute">
        <div panelActions>
          <select class="filter-select" [value]="statusFilter()" (change)="setStatus($any($event.target).value)">
            <option value="">All statuses</option>
            <option value="OPEN">Open</option>
            <option value="ACKNOWLEDGED">Acknowledged</option>
            <option value="RESOLVED">Resolved</option>
          </select>
          <select class="filter-select" [value]="severityFilter()" (change)="setSeverity($any($event.target).value)">
            <option value="">All severities</option>
            <option value="CRITICAL">Critical</option>
            <option value="WARNING">Warning</option>
            <option value="INFO">Info</option>
          </select>
          <button type="button" class="btn-secondary" (click)="reload()"><app-icon name="refresh" [size]="14" /> Refresh</button>
        </div>

        @if (alerts().length) {
          <div class="alert-list">
            @for (a of alerts(); track a.id) {
              <div class="alert-row" [class.resolved]="a.status === 'RESOLVED'">
                <app-badge [tone]="severityTone(a.severity)">{{ a.severity }}</app-badge>
                <div class="alert-body">
                  <div class="alert-title">{{ a.title }}</div>
                  <div class="alert-message">{{ a.message }}</div>
                  <div class="alert-meta">{{ a.sourceModule }} · opened {{ a.createdAt | date: 'short' }}
                    @if (a.acknowledgedBy) { · ack by {{ a.acknowledgedBy }} }
                    @if (a.resolvedAt) { · resolved {{ a.resolvedAt | date: 'short' }} }
                  </div>
                </div>
                <app-badge [tone]="statusTone(a.status)">{{ a.status }}</app-badge>
                <div class="alert-actions">
                  @if (a.status === 'OPEN') {
                    <button type="button" class="btn-secondary" (click)="acknowledge(a)">Acknowledge</button>
                  }
                  @if (a.status !== 'RESOLVED') {
                    <button type="button" class="btn-primary" (click)="resolve(a)">Resolve</button>
                  }
                </div>
              </div>
            }
          </div>
        } @else {
          <app-empty-state icon="bell" title="No alerts match this filter"
            description="Alerts are created automatically when a real condition is detected — a failed pipeline run, failing data quality checks, a high-confidence anomaly, or a DOWN system-health component." />
        }
      </app-panel>
    </div>
  `,
  styles: [
    `
    .page { display: flex; flex-direction: column; gap: var(--space-5); }
    .filter-select {
      padding: 6px var(--space-3); border-radius: var(--radius-sm); border: 1px solid var(--color-border);
      background: var(--color-surface-card-hover); color: var(--color-text-secondary); font-size: var(--font-size-sm);
    }
    .btn-secondary, .btn-primary {
      display: inline-flex; align-items: center; gap: 6px;
      padding: 7px var(--space-4); border-radius: var(--radius-sm); font-size: var(--font-size-sm); font-weight: 600; cursor: pointer;
    }
    .btn-secondary { background: var(--color-surface-card-hover); border: 1px solid var(--color-border); color: var(--color-text-secondary); }
    .btn-primary { background: var(--color-accent); border: none; color: #fff; }
    .alert-list { display: flex; flex-direction: column; gap: var(--space-3); }
    .alert-row { display: flex; align-items: flex-start; gap: var(--space-3); padding: var(--space-3); background: var(--color-surface-card-hover); border: 1px solid var(--color-border); border-radius: var(--radius-sm); }
    .alert-row.resolved { opacity: 0.6; }
    .alert-body { flex: 1; min-width: 0; }
    .alert-title { font-size: var(--font-size-sm); font-weight: 600; color: var(--color-text-primary); }
    .alert-message { font-size: var(--font-size-sm); color: var(--color-text-secondary); margin-top: 2px; }
    .alert-meta { font-size: var(--font-size-xs); color: var(--color-text-muted); margin-top: 4px; }
    .alert-actions { display: flex; gap: var(--space-2); flex-shrink: 0; }
    `,
  ],
})
export class AlertsComponent implements OnInit {
  readonly alerts = signal<Alert[]>([]);
  readonly statusFilter = signal('');
  readonly severityFilter = signal('');

  constructor(private readonly api: AlertsApiService) {}

  ngOnInit(): void {
    this.reload();
  }

  reload(): void {
    this.api.list(this.statusFilter() || undefined, this.severityFilter() || undefined)
      .subscribe({ next: (a) => this.alerts.set(a) });
  }

  setStatus(value: string): void {
    this.statusFilter.set(value);
    this.reload();
  }

  setSeverity(value: string): void {
    this.severityFilter.set(value);
    this.reload();
  }

  acknowledge(a: Alert): void {
    // No authentication exists yet (Group 5 introduces it) — attribute to the
    // UI itself rather than inventing a named user.
    this.api.acknowledge(a.id, 'observability-ui').subscribe({ next: () => this.reload() });
  }

  resolve(a: Alert): void {
    this.api.resolve(a.id).subscribe({ next: () => this.reload() });
  }

  severityTone(severity: string): BadgeTone {
    switch (severity) {
      case 'CRITICAL':
        return 'error';
      case 'WARNING':
        return 'warning';
      default:
        return 'info';
    }
  }

  statusTone(status: string): BadgeTone {
    switch (status) {
      case 'OPEN':
        return 'error';
      case 'ACKNOWLEDGED':
        return 'warning';
      default:
        return 'success';
    }
  }
}
