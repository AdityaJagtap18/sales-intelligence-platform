import { Component, Input } from '@angular/core';
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
    <div class="bar-row" *ngFor="let d of data">
      <span class="bar-label">{{ d.label }}</span>
      <div class="bar-track">
        <div class="bar-fill" [style.width.%]="(d.value / maxValue) * 100"></div>
      </div>
      <span class="bar-value">{{ d.value }}%</span>
    </div>
  `,
  styleUrl: './horizontal-bar-chart.component.scss',
})
export class HorizontalBarChartComponent {
  @Input() data: BarDatum[] = [];

  get maxValue(): number {
    return Math.max(...this.data.map((d) => d.value), 1);
  }
}
