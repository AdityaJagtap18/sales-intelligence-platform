import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { IconComponent } from '../icon/icon.component';

@Component({
  selector: 'app-filter-bar',
  standalone: true,
  imports: [CommonModule, IconComponent],
  template: `
    <div class="filter-bar">
      <button class="filter-chip" type="button">
        <app-icon name="file-text" [size]="14" />
        <span>{{ dateRangeLabel }}</span>
      </button>
      <button class="filter-chip" type="button">
        <app-icon name="sliders" [size]="14" />
        <span>Filters</span>
        <app-icon name="chevron-down" [size]="14" />
      </button>
      <ng-content />
    </div>
  `,
  styles: [
    `
    .filter-bar {
      display: flex;
      align-items: center;
      gap: var(--space-2);
    }
    .filter-chip {
      display: flex;
      align-items: center;
      gap: 6px;
      padding: 7px var(--space-3);
      background: var(--color-surface-card);
      border: 1px solid var(--color-border);
      border-radius: var(--radius-sm);
      color: var(--color-text-secondary);
      font-size: var(--font-size-sm);
      font-weight: 500;
    }
    .filter-chip:hover {
      border-color: var(--color-border-strong);
      color: var(--color-text-primary);
    }
    `,
  ],
})
export class FilterBarComponent {
  @Input() dateRangeLabel = 'This month';
}
