import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

export interface AreaLinePoint {
  label: string;
  value: number;
  expected?: number;
  anomaly?: boolean;
  showLabel?: boolean;
}

@Component({
  selector: 'app-area-line-chart',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './area-line-chart.component.html',
  styleUrl: './area-line-chart.component.scss',
})
export class AreaLineChartComponent {
  @Input() points: AreaLinePoint[] = [];

  private readonly w = 600;
  private readonly h = 220;
  private readonly padTop = 10;
  private readonly padBottom = 24;

  private scaleY(v: number, min: number, max: number): number {
    const range = max - min || 1;
    return this.padTop + (1 - (v - min) / range) * (this.h - this.padTop - this.padBottom);
  }

  private scaleX(i: number): number {
    const n = this.points.length - 1 || 1;
    return (i / n) * this.w;
  }

  get bounds() {
    const values = this.points.flatMap((p) => [p.value, p.expected ?? p.value]);
    return { min: Math.min(...values, 0), max: Math.max(...values, 1) };
  }

  get linePath(): string {
    const { min, max } = this.bounds;
    return this.points
      .map((p, i) => `${i === 0 ? 'M' : 'L'} ${this.scaleX(i).toFixed(1)} ${this.scaleY(p.value, min, max).toFixed(1)}`)
      .join(' ');
  }

  get expectedPath(): string {
    const { min, max } = this.bounds;
    const pts = this.points.filter((p) => p.expected !== undefined);
    if (!pts.length) return '';
    return this.points
      .map((p, i) =>
        p.expected !== undefined
          ? `${i === 0 ? 'M' : 'L'} ${this.scaleX(i).toFixed(1)} ${this.scaleY(p.expected, min, max).toFixed(1)}`
          : '',
      )
      .filter(Boolean)
      .join(' ');
  }

  get areaPath(): string {
    const line = this.linePath;
    if (!line) return '';
    const lastX = this.scaleX(this.points.length - 1);
    const firstX = this.scaleX(0);
    const baseline = this.h - this.padBottom;
    return `${line} L ${lastX.toFixed(1)} ${baseline} L ${firstX.toFixed(1)} ${baseline} Z`;
  }

  get anomalyMarkers(): { x: number; y: number }[] {
    const { min, max } = this.bounds;
    return this.points
      .map((p, i) => ({ p, i }))
      .filter(({ p }) => p.anomaly)
      .map(({ p, i }) => ({ x: this.scaleX(i), y: this.scaleY(p.value, min, max) }));
  }

  get viewBox(): string {
    return `0 0 ${this.w} ${this.h}`;
  }

  get labelPositions(): { x: number; label: string }[] {
    return this.points
      .map((p, i) => ({ x: this.scaleX(i), label: p.label, show: p.showLabel ?? true }))
      .filter((p) => p.show);
  }
}
