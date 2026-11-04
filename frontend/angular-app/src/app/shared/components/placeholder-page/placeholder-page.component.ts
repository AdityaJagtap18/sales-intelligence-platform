import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { IconComponent } from '../icon/icon.component';

@Component({
  selector: 'app-placeholder-page',
  standalone: true,
  imports: [CommonModule, IconComponent],
  template: `
    <div class="placeholder">
      <div class="placeholder-icon">
        <app-icon [name]="icon" [size]="28" />
      </div>
      <h2>{{ title }}</h2>
      <p>{{ description }}</p>
      <span class="phase-tag">Planned — {{ phase }}</span>
    </div>
  `,
  styles: [
    `
    .placeholder {
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      text-align: center;
      min-height: 60vh;
      gap: var(--space-3);
      color: var(--color-text-secondary);
    }
    .placeholder-icon {
      width: 56px;
      height: 56px;
      border-radius: var(--radius-lg);
      background: var(--color-surface-card);
      border: 1px solid var(--color-border);
      display: flex;
      align-items: center;
      justify-content: center;
      color: var(--color-accent);
      margin-bottom: var(--space-2);
    }
    h2 {
      color: var(--color-text-primary);
      font-size: var(--font-size-lg);
    }
    p {
      max-width: 440px;
      font-size: var(--font-size-sm);
    }
    .phase-tag {
      margin-top: var(--space-2);
      font-size: var(--font-size-xs);
      font-weight: 600;
      color: var(--color-accent-hover);
      background: var(--color-accent-soft);
      padding: 4px 10px;
      border-radius: 999px;
    }
    `,
  ],
})
export class PlaceholderPageComponent {
  @Input() title = 'Coming soon';
  @Input() description = 'This module is scoped in the master plan and will be built in a later phase.';
  @Input() icon = 'grid';
  @Input() phase = 'future phase';
}
