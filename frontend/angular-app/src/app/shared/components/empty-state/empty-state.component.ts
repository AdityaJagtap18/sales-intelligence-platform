import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { IconComponent } from '../icon/icon.component';

/** Shared empty/error/not-configured state — used wherever a list/chart has no data to show. */
@Component({
  selector: 'app-empty-state',
  standalone: true,
  imports: [CommonModule, IconComponent],
  template: `
    <div class="empty-state" [class.error]="tone === 'error'">
      <app-icon [name]="icon" [size]="22" />
      <p class="empty-title">{{ title }}</p>
      @if (description) {
        <p class="empty-description">{{ description }}</p>
      }
      <ng-content />
    </div>
  `,
  styles: [
    `
    .empty-state {
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      gap: var(--space-2);
      padding: var(--space-8) var(--space-4);
      text-align: center;
      color: var(--color-text-secondary);
    }
    .empty-state.error { color: var(--color-error); }
    .empty-title { font-size: var(--font-size-base); font-weight: 600; color: var(--color-text-primary); margin: 0; }
    .empty-state.error .empty-title { color: var(--color-error); }
    .empty-description { font-size: var(--font-size-sm); margin: 0; max-width: 420px; }
    `,
  ],
})
export class EmptyStateComponent {
  @Input() icon = 'inbox';
  @Input() title = 'No data available';
  @Input() description = '';
  @Input() tone: 'default' | 'error' = 'default';
}
