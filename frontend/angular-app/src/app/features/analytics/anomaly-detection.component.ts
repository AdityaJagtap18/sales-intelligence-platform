import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { PanelComponent } from '../../shared/components/panel/panel.component';
import { BadgeComponent } from '../../shared/components/badge/badge.component';
import { DataTableComponent, TableColumn } from '../../shared/components/data-table/data-table.component';
import { EmptyStateComponent } from '../../shared/components/empty-state/empty-state.component';
import { DrawerComponent } from '../../shared/components/drawer/drawer.component';
import { AnalyticsApiService, AnomalyRow } from '../../core/services/analytics-api.service';

/**
 * Dedicated Anomaly Detection page. Calls the same /api/analytics/anomalies
 * endpoint (AnomalyDetectionController -> AnalyticsService#anomalies()) that
 * Executive Overview's recent-anomalies panel uses — same source of truth,
 * per the spec.
 */
@Component({
  selector: 'app-anomaly-detection',
  standalone: true,
  imports: [CommonModule, FormsModule, PanelComponent, BadgeComponent, DataTableComponent, EmptyStateComponent, DrawerComponent],
  template: `
    <div class="page">
      <app-panel title="Detected Anomalies" subtitle="Order-value z-score anomalies — same engine as Executive Overview">
        <div class="toolbar">
          <select [(ngModel)]="severity" (ngModelChange)="reload()">
            <option [ngValue]="undefined">All severities</option>
            <option value="High">High</option>
            <option value="Medium">Medium</option>
            <option value="Low">Low</option>
          </select>
          <select [(ngModel)]="metric" (ngModelChange)="reload()">
            <option [ngValue]="undefined">All metrics</option>
            <option value="Order Value">Order Value</option>
          </select>
        </div>
        @if (anomalies().length) {
          <app-data-table [columns]="columns" [rows]="rows()" [clickableRows]="true" (rowClick)="openDetail($event)" />
        } @else {
          <app-empty-state title="No anomalies match these filters" description="No order values fell outside the z-score threshold for the current filter." />
        }
      </app-panel>
    </div>

    <app-drawer [open]="selected() !== null" title="Anomaly Detail" (close)="selected.set(null)">
      @if (selected()) {
        <div class="detail-grid">
          <div><span class="detail-label">Metric</span><span>{{ selected()!.metric }}</span></div>
          <div><span class="detail-label">Order</span><span>{{ selected()!.date }}</span></div>
          <div><span class="detail-label">Severity</span><app-badge [tone]="tone(selected()!.impact)">{{ selected()!.impact }}</app-badge></div>
          <div><span class="detail-label">Direction</span><span>{{ selected()!.direction }}</span></div>
          <div><span class="detail-label">Confidence</span><span>{{ selected()!.confidence }}%</span></div>
          <div><span class="detail-label">Expected Value</span><span>{{ selected()!.expectedValue }}</span></div>
          <div><span class="detail-label">Observed Value</span><span>{{ selected()!.observedValue }}</span></div>
        </div>
        <p class="explanation">
          This order's value deviates from the mean order value by more than the 1.3 z-score threshold used across the platform
          (see Executive Overview and AnalyticsService#anomalies()).
        </p>

        @if (selected()!.acknowledgementStatus) {
          <app-badge tone="success">{{ selected()!.acknowledgementStatus }}</app-badge>
          @if (selected()!.acknowledgementNote) {
            <p class="note">{{ selected()!.acknowledgementNote }}</p>
          }
        } @else {
          <textarea [(ngModel)]="note" placeholder="Optional note..." rows="2"></textarea>
          <div class="ack-actions">
            <button type="button" class="btn-secondary" (click)="acknowledge('ACKNOWLEDGED')">Acknowledge</button>
            <button type="button" class="btn-primary" (click)="acknowledge('RESOLVED')">Resolve</button>
          </div>
        }
      }
    </app-drawer>
  `,
  styles: [
    `
    .page { display: flex; flex-direction: column; gap: var(--space-5); }
    .toolbar { display: flex; gap: var(--space-3); margin-bottom: var(--space-3); }
    select, textarea {
      background: var(--color-surface-card); border: 1px solid var(--color-border); border-radius: var(--radius-sm);
      color: var(--color-text-primary); padding: 7px var(--space-3); font-size: var(--font-size-sm);
    }
    textarea { width: 100%; resize: vertical; margin: var(--space-3) 0; }
    .detail-grid { display: grid; grid-template-columns: 1fr 1fr; gap: var(--space-3); margin-bottom: var(--space-4); }
    .detail-label { display: block; font-size: var(--font-size-xs); color: var(--color-text-muted); margin-bottom: 2px; }
    .explanation { font-size: var(--font-size-sm); color: var(--color-text-secondary); margin-bottom: var(--space-4); }
    .note { font-size: var(--font-size-sm); color: var(--color-text-secondary); }
    .ack-actions { display: flex; gap: var(--space-2); }
    .btn-secondary, .btn-primary {
      padding: 7px var(--space-4); border-radius: var(--radius-sm); font-size: var(--font-size-sm); font-weight: 600; cursor: pointer;
    }
    .btn-secondary { background: var(--color-surface-card-hover); border: 1px solid var(--color-border); color: var(--color-text-secondary); }
    .btn-primary { background: var(--color-accent); border: none; color: #fff; }
    `,
  ],
})
export class AnomalyDetectionComponent implements OnInit {
  readonly anomalies = signal<AnomalyRow[]>([]);
  readonly selected = signal<AnomalyRow | null>(null);

  severity: string | undefined;
  metric: string | undefined;
  note = '';

  readonly columns: TableColumn[] = [
    { key: 'metric', label: 'Metric' },
    { key: 'date', label: 'Order' },
    { key: 'impact', label: 'Severity' },
    { key: 'direction', label: 'Direction' },
    { key: 'confidence', label: 'Confidence', align: 'right' },
    { key: 'acknowledgementStatus', label: 'Status' },
  ];

  constructor(private readonly api: AnalyticsApiService) {}

  ngOnInit(): void {
    this.reload();
  }

  reload(): void {
    this.api.anomalies(this.severity, this.metric).subscribe({ next: (a) => this.anomalies.set(a) });
  }

  rows(): Record<string, unknown>[] {
    return this.anomalies().map((a) => ({ ...a, acknowledgementStatus: a.acknowledgementStatus ?? 'Open' }));
  }

  openDetail(row: Record<string, any>): void {
    this.note = '';
    this.selected.set(this.anomalies().find((a) => a.key === row['key']) ?? null);
  }

  acknowledge(status: 'ACKNOWLEDGED' | 'RESOLVED'): void {
    const current = this.selected();
    if (!current) return;
    this.api.acknowledgeAnomaly(current.key, status, this.note || undefined).subscribe({
      next: () => {
        this.selected.set(null);
        this.reload();
      },
    });
  }

  tone(impact: string): 'error' | 'warning' | 'info' {
    if (impact === 'High') return 'error';
    if (impact === 'Medium') return 'warning';
    return 'info';
  }
}
