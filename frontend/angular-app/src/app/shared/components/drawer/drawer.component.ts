import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { IconComponent } from '../icon/icon.component';

/** Detail drawer, slides in from the right. Used for record detail views (customer, run, anomaly, alert). */
@Component({
  selector: 'app-drawer',
  standalone: true,
  imports: [CommonModule, IconComponent],
  template: `
    @if (open) {
      <div class="drawer-backdrop" (click)="close.emit()"></div>
      <aside class="drawer">
        <header class="drawer-header">
          <h3>{{ title }}</h3>
          <button type="button" class="drawer-close" (click)="close.emit()">
            <app-icon name="x" [size]="16" />
          </button>
        </header>
        <div class="drawer-body">
          <ng-content />
        </div>
      </aside>
    }
  `,
  styles: [
    `
    .drawer-backdrop {
      position: fixed; inset: 0; background: rgba(5, 7, 12, 0.6); z-index: 40;
    }
    .drawer {
      position: fixed; top: 0; right: 0; bottom: 0; width: min(480px, 100vw);
      background: var(--color-surface-sidebar); border-left: 1px solid var(--color-border);
      z-index: 41; display: flex; flex-direction: column; box-shadow: -8px 0 24px rgba(0,0,0,0.3);
    }
    .drawer-header {
      display: flex; align-items: center; justify-content: space-between;
      padding: var(--space-4); border-bottom: 1px solid var(--color-border);
    }
    .drawer-header h3 { margin: 0; font-size: var(--font-size-md); color: var(--color-text-primary); }
    .drawer-close {
      background: transparent; border: none; color: var(--color-text-secondary); cursor: pointer;
      display: flex; padding: var(--space-1);
    }
    .drawer-close:hover { color: var(--color-text-primary); }
    .drawer-body { padding: var(--space-4); overflow-y: auto; flex: 1; }
    `,
  ],
})
export class DrawerComponent {
  @Input() open = false;
  @Input() title = '';
  @Output() close = new EventEmitter<void>();
}
