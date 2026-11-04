import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { PanelComponent } from '../../shared/components/panel/panel.component';
import { BadgeComponent } from '../../shared/components/badge/badge.component';
import { AreaLineChartComponent, AreaLinePoint } from '../../shared/components/charts/area-line-chart.component';
import { DataTableComponent, TableColumn } from '../../shared/components/data-table/data-table.component';
import { EmptyStateComponent } from '../../shared/components/empty-state/empty-state.component';
import { ForecastingApiService, ForecastRun } from '../../core/services/forecasting-api.service';

@Component({
  selector: 'app-forecasting',
  standalone: true,
  imports: [CommonModule, FormsModule, PanelComponent, BadgeComponent, AreaLineChartComponent, DataTableComponent, EmptyStateComponent],
  template: `
    <div class="page">
      <app-panel title="Generate Forecast">
        <div class="toolbar">
          <select [(ngModel)]="metric">
            <option value="REVENUE">Revenue</option>
            <option value="ORDER_VOLUME">Order Volume</option>
          </select>
          <select [(ngModel)]="horizon">
            <option [ngValue]="2">2 periods ahead</option>
            <option [ngValue]="4">4 periods ahead</option>
            <option [ngValue]="8">8 periods ahead</option>
          </select>
          <button type="button" class="btn-primary" (click)="generate()" [disabled]="generating()">
            {{ generating() ? 'Generating…' : 'Generate Forecast' }}
          </button>
        </div>

        @if (current()) {
          @if (current()!.status === 'INSUFFICIENT_DATA') {
            <app-empty-state icon="alert-triangle" title="Forecasting cannot be performed" [description]="current()!.reason ?? ''" />
          } @else {
            <div class="metrics-row">
              <div><span class="metric-label">Method</span><span>{{ current()!.method }}</span></div>
              <div><span class="metric-label">MAE</span><span>{{ current()!.mae }}</span></div>
              <div><span class="metric-label">MAPE</span><span>{{ current()!.mape }}%</span></div>
            </div>
            <app-area-line-chart [points]="chartPoints()" />
          }
        } @else {
          <app-empty-state title="No forecast generated yet" description="Choose a metric and horizon, then Generate Forecast." />
        }
      </app-panel>

      <app-panel title="Forecast Run History">
        @if (history().length) {
          <app-data-table [columns]="historyColumns" [rows]="historyRows()" />
        } @else {
          <app-empty-state title="No forecast runs yet" />
        }
      </app-panel>
    </div>
  `,
  styles: [
    `
    .page { display: flex; flex-direction: column; gap: var(--space-5); }
    .toolbar { display: flex; gap: var(--space-3); margin-bottom: var(--space-4); align-items: center; }
    select {
      background: var(--color-surface-card); border: 1px solid var(--color-border); border-radius: var(--radius-sm);
      color: var(--color-text-primary); padding: 7px var(--space-3); font-size: var(--font-size-sm);
    }
    .btn-primary {
      background: var(--color-accent); border: none; border-radius: var(--radius-sm); color: #fff;
      padding: 7px var(--space-4); font-size: var(--font-size-sm); font-weight: 600; cursor: pointer;
    }
    .btn-primary:disabled { opacity: 0.6; cursor: not-allowed; }
    .metrics-row { display: flex; gap: var(--space-5); margin-bottom: var(--space-4); }
    .metric-label { display: block; font-size: var(--font-size-xs); color: var(--color-text-muted); }
    `,
  ],
})
export class ForecastingComponent implements OnInit {
  readonly current = signal<ForecastRun | null>(null);
  readonly history = signal<ForecastRun[]>([]);
  readonly generating = signal(false);

  metric: 'REVENUE' | 'ORDER_VOLUME' = 'REVENUE';
  horizon = 4;

  readonly historyColumns: TableColumn[] = [
    { key: 'metric', label: 'Metric' },
    { key: 'status', label: 'Status' },
    { key: 'method', label: 'Method' },
    { key: 'mae', label: 'MAE', align: 'right' },
    { key: 'mape', label: 'MAPE %', align: 'right' },
    { key: 'createdAt', label: 'Run At' },
  ];

  constructor(private readonly api: ForecastingApiService) {}

  ngOnInit(): void {
    this.loadHistory();
  }

  generate(): void {
    this.generating.set(true);
    this.api.generate(this.metric, this.horizon).subscribe({
      next: (run) => {
        this.current.set(run);
        this.generating.set(false);
        this.loadHistory();
      },
      error: () => this.generating.set(false),
    });
  }

  loadHistory(): void {
    this.api.history().subscribe({ next: (h) => this.history.set(h) });
  }

  historyRows(): Record<string, unknown>[] {
    return this.history().map((h) => ({ ...h, method: h.method ?? '—', mae: h.mae ?? '—', mape: h.mape ?? '—' }));
  }

  chartPoints(): AreaLinePoint[] {
    const run = this.current();
    if (!run) return [];
    const historical = run.historical.map((p) => ({ label: p.label, value: p.actual ?? 0, expected: p.predicted ?? 0, anomaly: false }));
    const predicted = run.predicted.map((p) => ({ label: p.label, value: p.predicted ?? 0, expected: p.predicted ?? 0, anomaly: false }));
    return [...historical, ...predicted];
  }
}
