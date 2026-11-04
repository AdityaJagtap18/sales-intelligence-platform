import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { IconComponent } from '../icon/icon.component';

@Component({
  selector: 'app-pagination',
  standalone: true,
  imports: [CommonModule, IconComponent],
  template: `
    @if (totalPages > 1) {
      <div class="pagination">
        <span class="pagination-summary">Page {{ page + 1 }} of {{ totalPages }} ({{ totalItems }} items)</span>
        <div class="pagination-controls">
          <button type="button" [disabled]="page === 0" (click)="goTo(page - 1)">
            <app-icon name="chevron-left" [size]="14" />
          </button>
          <button type="button" [disabled]="page >= totalPages - 1" (click)="goTo(page + 1)">
            <app-icon name="chevron-right" [size]="14" />
          </button>
        </div>
      </div>
    }
  `,
  styles: [
    `
    .pagination { display: flex; align-items: center; justify-content: space-between; padding-top: var(--space-3); gap: var(--space-3); }
    .pagination-summary { font-size: var(--font-size-sm); color: var(--color-text-secondary); }
    .pagination-controls { display: flex; gap: var(--space-2); }
    button {
      display: flex; align-items: center; justify-content: center;
      width: 28px; height: 28px;
      background: var(--color-surface-card); border: 1px solid var(--color-border);
      border-radius: var(--radius-sm); color: var(--color-text-secondary); cursor: pointer;
    }
    button:disabled { opacity: 0.4; cursor: not-allowed; }
    button:not(:disabled):hover { border-color: var(--color-border-strong); color: var(--color-text-primary); }
    `,
  ],
})
export class PaginationComponent {
  @Input() page = 0;
  @Input() pageSize = 10;
  @Input() totalItems = 0;
  @Output() pageChange = new EventEmitter<number>();

  get totalPages(): number {
    return Math.max(1, Math.ceil(this.totalItems / this.pageSize));
  }

  goTo(page: number): void {
    if (page >= 0 && page < this.totalPages) {
      this.pageChange.emit(page);
    }
  }
}
