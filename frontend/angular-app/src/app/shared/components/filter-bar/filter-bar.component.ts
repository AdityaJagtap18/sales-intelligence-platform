import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { IconComponent } from '../icon/icon.component';

export interface DateRangeChange {
  from: string | null;
  to: string | null;
}

@Component({
  selector: 'app-filter-bar',
  standalone: true,
  imports: [CommonModule, FormsModule, IconComponent],
  template: `
    <div class="filter-bar">
      <div class="filter-popover-wrapper">
        <button class="filter-chip" type="button" (click)="open = !open">
          <app-icon name="calendar" [size]="14" />
          <span>{{ dateRangeLabel }}</span>
          <app-icon name="chevron-down" [size]="14" />
        </button>
        @if (open) {
          <div class="popover">
            <div class="field">
              <label>From</label>
              <input type="date" [(ngModel)]="from" [min]="minDate ?? undefined" [max]="maxDate ?? undefined" />
            </div>
            <div class="field">
              <label>To</label>
              <input type="date" [(ngModel)]="to" [min]="minDate ?? undefined" [max]="maxDate ?? undefined" />
            </div>
            <div class="actions">
              <button type="button" class="btn-ghost" (click)="clear()">Clear</button>
              <button type="button" class="btn-primary" (click)="apply()">Apply</button>
            </div>
          </div>
        }
      </div>
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
    .filter-popover-wrapper {
      position: relative;
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
      cursor: pointer;
    }
    .filter-chip:hover {
      border-color: var(--color-border-strong);
      color: var(--color-text-primary);
    }
    .popover {
      position: absolute;
      top: calc(100% + 6px);
      left: 0;
      z-index: 20;
      display: flex;
      flex-direction: column;
      gap: var(--space-2);
      padding: var(--space-3);
      background: var(--color-surface-card);
      border: 1px solid var(--color-border);
      border-radius: var(--radius-sm);
      box-shadow: 0 8px 24px rgba(0, 0, 0, 0.2);
      min-width: 220px;
    }
    .field { display: flex; flex-direction: column; gap: 4px; }
    .field label { font-size: var(--font-size-xs); color: var(--color-text-secondary); }
    .field input {
      background: var(--color-surface-base);
      border: 1px solid var(--color-border);
      border-radius: var(--radius-sm);
      color: var(--color-text-primary);
      padding: 6px var(--space-2);
      font-size: var(--font-size-sm);
    }
    .actions { display: flex; justify-content: flex-end; gap: var(--space-2); margin-top: var(--space-1); }
    .btn-ghost, .btn-primary {
      border-radius: var(--radius-sm);
      padding: 6px var(--space-3);
      font-size: var(--font-size-sm);
      cursor: pointer;
      border: 1px solid var(--color-border);
    }
    .btn-ghost { background: transparent; color: var(--color-text-secondary); }
    .btn-primary { background: var(--color-accent); color: var(--color-text-on-accent, #fff); border-color: var(--color-accent); }
    `,
  ],
})
export class FilterBarComponent {
  @Input() dateRangeLabel = 'This month';
  @Input() minDate: string | null = null;
  @Input() maxDate: string | null = null;
  @Output() rangeChange = new EventEmitter<DateRangeChange>();

  open = false;
  from: string | undefined;
  to: string | undefined;

  apply(): void {
    this.open = false;
    this.rangeChange.emit({ from: this.from ?? null, to: this.to ?? null });
  }

  clear(): void {
    this.from = this.to = undefined;
    this.open = false;
    this.rangeChange.emit({ from: null, to: null });
  }
}
