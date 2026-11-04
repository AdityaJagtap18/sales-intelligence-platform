import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-skeleton',
  standalone: true,
  imports: [CommonModule],
  template: `<div class="skeleton" [style.height.px]="height" [style.width]="width"></div>`,
  styles: [
    `
    .skeleton {
      border-radius: var(--radius-sm);
      background: linear-gradient(90deg, var(--color-surface-card) 25%, var(--color-surface-card-hover) 50%, var(--color-surface-card) 75%);
      background-size: 200% 100%;
      animation: skeleton-shimmer 1.4s ease-in-out infinite;
    }
    @keyframes skeleton-shimmer {
      0% { background-position: 200% 0; }
      100% { background-position: -200% 0; }
    }
    `,
  ],
})
export class SkeletonComponent {
  @Input() height = 16;
  @Input() width = '100%';
}
