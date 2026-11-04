import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';

export interface DonutDatum {
  label: string;
  value: number;
}

interface DonutSegment extends DonutDatum {
  pct: number;
  color: string;
  dashArray: string;
  dashOffset: number;
}

const SERIES_VARS = [
  'var(--chart-series-1)',
  'var(--chart-series-2)',
  'var(--chart-series-3)',
  'var(--chart-series-4)',
  'var(--chart-series-5)',
  'var(--chart-series-6)',
];

@Component({
  selector: 'app-donut-chart',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './donut-chart.component.html',
  styleUrl: './donut-chart.component.scss',
})
export class DonutChartComponent {
  @Input() data: DonutDatum[] = [];
  @Input() centerLabel = '';
  @Input() centerValue = '';
  @Input() clickable = false;
  @Output() sliceClick = new EventEmitter<DonutDatum>();

  onClick(d: DonutDatum): void {
    if (this.clickable) {
      this.sliceClick.emit(d);
    }
  }

  private readonly circumference = 2 * Math.PI * 15.9155;

  get total(): number {
    return this.data.reduce((sum, d) => sum + d.value, 0) || 1;
  }

  get segments(): DonutSegment[] {
    let cumulative = 0;
    return this.data.map((d, i) => {
      const pct = (d.value / this.total) * 100;
      const dashArray = `${(pct / 100) * this.circumference} ${this.circumference}`;
      const dashOffset = (-cumulative / 100) * this.circumference;
      cumulative += pct;
      return { ...d, pct, color: SERIES_VARS[i % SERIES_VARS.length], dashArray, dashOffset };
    });
  }
}
