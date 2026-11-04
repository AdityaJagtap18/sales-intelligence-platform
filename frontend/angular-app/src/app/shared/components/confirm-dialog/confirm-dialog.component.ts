import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';

/** Confirmation modal for destructive/irreversible actions (retry, disable source, deactivate user, etc.). */
@Component({
  selector: 'app-confirm-dialog',
  standalone: true,
  imports: [CommonModule],
  template: `
    @if (open) {
      <div class="confirm-backdrop" (click)="cancel.emit()"></div>
      <div class="confirm-dialog" role="alertdialog">
        <h4>{{ title }}</h4>
        <p>{{ message }}</p>
        <div class="confirm-actions">
          <button type="button" class="btn-secondary" (click)="cancel.emit()">{{ cancelLabel }}</button>
          <button type="button" class="btn-danger" (click)="confirm.emit()">{{ confirmLabel }}</button>
        </div>
      </div>
    }
  `,
  styles: [
    `
    .confirm-backdrop { position: fixed; inset: 0; background: rgba(5, 7, 12, 0.6); z-index: 50; }
    .confirm-dialog {
      position: fixed; top: 50%; left: 50%; transform: translate(-50%, -50%);
      width: min(380px, 90vw); background: var(--color-surface-card); border: 1px solid var(--color-border);
      border-radius: var(--radius-md); padding: var(--space-5); z-index: 51;
    }
    .confirm-dialog h4 { margin: 0 0 var(--space-2); color: var(--color-text-primary); font-size: var(--font-size-md); }
    .confirm-dialog p { margin: 0 0 var(--space-4); color: var(--color-text-secondary); font-size: var(--font-size-sm); }
    .confirm-actions { display: flex; justify-content: flex-end; gap: var(--space-2); }
    .btn-secondary, .btn-danger {
      padding: 7px var(--space-4); border-radius: var(--radius-sm); font-size: var(--font-size-sm); font-weight: 600; cursor: pointer;
    }
    .btn-secondary { background: var(--color-surface-card-hover); border: 1px solid var(--color-border); color: var(--color-text-secondary); }
    .btn-danger { background: var(--color-error); border: 1px solid var(--color-error); color: #fff; }
    `,
  ],
})
export class ConfirmDialogComponent {
  @Input() open = false;
  @Input() title = 'Are you sure?';
  @Input() message = 'This action cannot be undone.';
  @Input() confirmLabel = 'Confirm';
  @Input() cancelLabel = 'Cancel';
  @Output() confirm = new EventEmitter<void>();
  @Output() cancel = new EventEmitter<void>();
}
