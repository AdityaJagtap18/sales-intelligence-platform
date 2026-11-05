import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { PanelComponent } from '../../shared/components/panel/panel.component';
import { BadgeComponent, BadgeTone } from '../../shared/components/badge/badge.component';
import { IconComponent } from '../../shared/components/icon/icon.component';
import { EmptyStateComponent } from '../../shared/components/empty-state/empty-state.component';
import { SystemHealthApiService, SystemHealth } from '../../core/services/system-health-api.service';
import { DataSourceApiService, DataSourceRow } from '../../core/services/data-source-api.service';

/**
 * Integrations is deliberately not a third tracking system: it composes
 * System Health's real connectivity checks (infrastructure this app talks
 * to directly) with the Data Sources registry (business data sources
 * registered under Data Platform) into one view, rather than re-querying
 * either independently.
 */
@Component({
  selector: 'app-integrations',
  standalone: true,
  imports: [CommonModule, PanelComponent, BadgeComponent, IconComponent, EmptyStateComponent],
  template: `
    <div class="page">
      <app-panel title="Infrastructure Connections" subtitle="From System Health — the external services this app talks to directly">
        <div panelActions>
          <button type="button" class="btn-secondary" (click)="openSystemHealth()"><app-icon name="heart" [size]="14" /> Open System Health</button>
        </div>
        @if (health()) {
          <div class="integration-grid">
            @for (c of health()!.components; track c.name) {
              <div class="integration-card">
                <div class="name">{{ c.name }}</div>
                <div class="category">{{ c.category }}</div>
                <app-badge [tone]="healthTone(c.status)">{{ c.status }}</app-badge>
              </div>
            }
          </div>
        } @else {
          <app-empty-state title="Loading…" />
        }
      </app-panel>

      <app-panel title="Registered Data Sources" subtitle="From Data Platform > Data Sources — business data feeding this app">
        <div panelActions>
          <button type="button" class="btn-secondary" (click)="openDataSources()"><app-icon name="database" [size]="14" /> Open Data Sources</button>
        </div>
        @if (sources().length) {
          <div class="integration-grid">
            @for (s of sources(); track s.id) {
              <div class="integration-card">
                <div class="name">{{ s.name }}</div>
                <div class="category">{{ s.sourceType }}</div>
                <app-badge [tone]="sourceTone(s)">{{ s.enabled ? (s.lastTestStatus ?? 'Not tested') : 'Disabled' }}</app-badge>
              </div>
            }
          </div>
        } @else {
          <app-empty-state icon="plug" title="No data sources registered"
            description="Register one under Data Platform > Data Sources." />
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
    .integration-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: var(--space-3); }
    .integration-card { display: flex; flex-direction: column; gap: 6px; padding: var(--space-3); background: var(--color-surface-card-hover); border: 1px solid var(--color-border); border-radius: var(--radius-sm); }
    .name { font-size: var(--font-size-sm); font-weight: 600; color: var(--color-text-primary); }
    .category { font-size: var(--font-size-xs); color: var(--color-text-muted); }
    `,
  ],
})
export class IntegrationsComponent implements OnInit {
  readonly health = signal<SystemHealth | null>(null);
  readonly sources = signal<DataSourceRow[]>([]);

  constructor(
    private readonly healthApi: SystemHealthApiService,
    private readonly sourcesApi: DataSourceApiService,
    private readonly router: Router,
  ) {}

  ngOnInit(): void {
    this.healthApi.check().subscribe({ next: (h) => this.health.set(h) });
    this.sourcesApi.list().subscribe({ next: (s) => this.sources.set(s) });
  }

  openSystemHealth(): void {
    this.router.navigate(['/observability/system-health']);
  }

  openDataSources(): void {
    this.router.navigate(['/data-platform/sources']);
  }

  healthTone(status: string): BadgeTone {
    switch (status) {
      case 'UP':
        return 'success';
      case 'DOWN':
        return 'error';
      case 'DEGRADED':
        return 'warning';
      default:
        return 'neutral';
    }
  }

  sourceTone(s: DataSourceRow): BadgeTone {
    if (!s.enabled) return 'neutral';
    if (s.lastTestStatus === 'OK') return 'success';
    if (s.lastTestStatus === 'FAILED') return 'error';
    return 'neutral';
  }
}
