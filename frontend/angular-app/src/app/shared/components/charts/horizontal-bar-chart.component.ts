import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';

export interface BarDatum {
  label: string;
  value: number;
}

@Component({
  selector: 'app-horizontal-bar-chart',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="bar-row" *ngFor="let d of data" [class.clickable]="clickable" (click)="onClick(d)">
      <span class="bar-label">{{ d.label }}</span>
      <div class="bar-track">
        <div class="bar-fill" [style.width.%]="(d.value / maxValue) * 100"></div>
      </div>
      <span class="bar-value">{{ valuePrefix }}{{ d.value.toLocaleString() }}{{ valueSuffix }}</span>
    </div>
  `,
  styleUrl: './horizontal-bar-chart.component.scss',
})
export class HorizontalBarChartComponent {
  @Input() data: BarDatum[] = [];
  /** Preserves the original "share %" rendering by default; pass '' with a valuePrefix of '$' for currency bars. */
  @Input() valueSuffix = '%';
  @Input() valuePrefix = '';
  @Input() clickable = false;
  @Output() barClick = new EventEmitter<BarDatum>();

  get maxValue(): number {
    return Math.max(...this.data.map((d) => d.value), 1);
  }

  onClick(d: BarDatum): void {
    if (this.clickable) {
      this.barClick.emit(d);
    }
  }
}
