import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { PanelComponent } from '../../shared/components/panel/panel.component';
import { BadgeComponent, BadgeTone } from '../../shared/components/badge/badge.component';
import { IconComponent } from '../../shared/components/icon/icon.component';
import { EmptyStateComponent } from '../../shared/components/empty-state/empty-state.component';
import { SystemHealthApiService, SystemHealth } from '../../core/services/system-health-api.service';

@Component({
  selector: 'app-system-health',
  standalone: true,
  imports: [CommonModule, PanelComponent, BadgeComponent, IconComponent, EmptyStateComponent],
  template: `
    <div class="page">
      @if (loadError()) {
        <app-empty-state icon="alert-triangle" tone="error" title="Could not load system health"
          [description]="loadError()!" />
      } @else if (health()) {
        <div class="overall">
          <app-icon [name]="overallIcon()" [size]="22" />
          <div>
            <div class="overall-status">Overall: <app-badge [tone]="toneFor(health()!.overallStatus)">{{ health()!.overallStatus }}</app-badge></div>
            <div class="checked-at">Checked {{ health()!.checkedAt | date: 'medium' }}</div>
          </div>
          <button type="button" class="btn-secondary" (click)="reload()" [disabled]="loading()">
            <app-icon name="refresh" [size]="14" /> {{ loading() ? 'Checking…' : 'Recheck now' }}
          </button>
        </div>

        <div class="component-grid">
          @for (c of health()!.components; track c.name) {
            <app-panel [title]="c.name" [subtitle]="c.category">
              <div class="component-body">
                <app-badge [tone]="toneFor(c.status)">{{ c.status }}</app-badge>
                @if (c.latencyMs !== null) {
                  <span class="latency">{{ c.latencyMs }} ms</span>
                }
              </div>
              @if (c.message) {
                <p class="message">{{ c.message }}</p>
              }
            </app-panel>
          }
        </div>
      }
    </div>
  `,
  styles: [
    `
    .page { display: flex; flex-direction: column; gap: var(--space-5); }
    .overall { display: flex; align-items: center; gap: var(--space-3); padding: var(--space-4); background: var(--color-surface-card); border: 1px solid var(--color-border); border-radius: var(--radius-md); }
    .overall-status { font-size: var(--font-size-md); font-weight: 600; color: var(--color-text-primary); display: flex; align-items: center; gap: 8px; }
    .checked-at { font-size: var(--font-size-xs); color: var(--color-text-muted); margin-top: 2px; }
    .btn-secondary {
      margin-left: auto; display: inline-flex; align-items: center; gap: 6px;
      padding: 7px var(--space-4); border-radius: var(--radius-sm); font-size: var(--font-size-sm); font-weight: 600; cursor: pointer;
      background: var(--color-surface-card-hover); border: 1px solid var(--color-border); color: var(--color-text-secondary);
    }
    .btn-secondary:disabled { opacity: 0.6; cursor: not-allowed; }
    .component-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(240px, 1fr)); gap: var(--space-4); }
    .component-body { display: flex; align-items: center; gap: var(--space-3); }
    .latency { font-size: var(--font-size-xs); color: var(--color-text-muted); }
    .message { margin-top: var(--space-2); font-size: var(--font-size-sm); color: var(--color-text-secondary); }
    `,
  ],
})
export class SystemHealthComponent implements OnInit {
  readonly health = signal<SystemHealth | null>(null);
  readonly loading = signal(false);
  readonly loadError = signal<string | null>(null);

  constructor(private readonly api: SystemHealthApiService) {}

  ngOnInit(): void {
    this.reload();
  }

  reload(): void {
    this.loading.set(true);
    this.api.check().subscribe({
      next: (h) => {
        this.health.set(h);
        this.loading.set(false);
        this.loadError.set(null);
      },
      error: () => {
        this.loading.set(false);
        this.loadError.set('Could not reach the Spring Boot API.');
      },
    });
  }

  overallIcon(): string {
    const status = this.health()?.overallStatus;
    return status === 'UP' ? 'check-circle' : status === 'DEGRADED' ? 'alert-triangle' : 'alert-triangle';
  }

  toneFor(status: string): BadgeTone {
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
}
