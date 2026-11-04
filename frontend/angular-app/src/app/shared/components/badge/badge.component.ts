import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

export type BadgeTone = 'success' | 'error' | 'warning' | 'info' | 'neutral' | 'accent';

@Component({
  selector: 'app-badge',
  standalone: true,
  imports: [CommonModule],
  template: `<span class="badge" [class]="'tone-' + tone"><ng-content /></span>`,
  styles: [
    `
    .badge {
      display: inline-flex;
      align-items: center;
      gap: 4px;
      padding: 3px 9px;
      border-radius: 999px;
      font-size: var(--font-size-xs);
      font-weight: 600;
      line-height: 1.4;
      white-space: nowrap;
    }
    .tone-success { background: var(--color-success-soft); color: var(--color-success); }
    .tone-error { background: var(--color-error-soft); color: var(--color-error); }
    .tone-warning { background: var(--color-warning-soft); color: var(--color-warning); }
    .tone-info { background: var(--color-info-soft); color: var(--color-info); }
    .tone-accent { background: var(--color-accent-soft); color: var(--color-accent-hover); }
    .tone-neutral { background: var(--color-surface-card-hover); color: var(--color-text-secondary); }
    `,
  ],
})
export class BadgeComponent {
  @Input() tone: BadgeTone = 'neutral';
}
