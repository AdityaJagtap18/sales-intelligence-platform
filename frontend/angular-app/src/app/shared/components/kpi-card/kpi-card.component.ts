import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { IconComponent } from '../icon/icon.component';

@Component({
  selector: 'app-kpi-card',
  standalone: true,
  imports: [CommonModule, IconComponent],
  templateUrl: './kpi-card.component.html',
  styleUrl: './kpi-card.component.scss',
})
export class KpiCardComponent {
  @Input() label = '';
  @Input() value = '';
  @Input() icon = 'grid';
  @Input() deltaLabel = '';
  @Input() trend: 'up' | 'down' | 'flat' = 'up';
  @Input() positiveIsGood = true;
  @Input() sparkline: number[] = [];

  get trendTone(): 'success' | 'error' | 'neutral' {
    if (this.trend === 'flat') return 'neutral';
    const isGood = this.trend === 'up' ? this.positiveIsGood : !this.positiveIsGood;
    return isGood ? 'success' : 'error';
  }

  get sparklinePoints(): string {
    if (!this.sparkline?.length) return '';
    const w = 100;
    const h = 32;
    const min = Math.min(...this.sparkline);
    const max = Math.max(...this.sparkline);
    const range = max - min || 1;
    const step = w / (this.sparkline.length - 1 || 1);
    return this.sparkline
      .map((v, i) => `${(i * step).toFixed(1)},${(h - ((v - min) / range) * h).toFixed(1)}`)
      .join(' ');
  }
}
