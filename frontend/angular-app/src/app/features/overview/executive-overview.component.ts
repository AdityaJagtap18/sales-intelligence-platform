import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { KpiCardComponent } from '../../shared/components/kpi-card/kpi-card.component';
import { PanelComponent } from '../../shared/components/panel/panel.component';
import { BadgeComponent } from '../../shared/components/badge/badge.component';
import { FilterBarComponent } from '../../shared/components/filter-bar/filter-bar.component';
import { IconComponent } from '../../shared/components/icon/icon.component';
import { AreaLineChartComponent } from '../../shared/components/charts/area-line-chart.component';
import { HorizontalBarChartComponent } from '../../shared/components/charts/horizontal-bar-chart.component';
import { DonutChartComponent } from '../../shared/components/charts/donut-chart.component';
import { DataTableComponent, TableColumn } from '../../shared/components/data-table/data-table.component';
import {
  MOCK_KPIS,
  MOCK_REVENUE_SERIES,
  MOCK_REVENUE_BY_CHANNEL,
  MOCK_USERS_BY_SEGMENT,
  MOCK_RECENT_ANOMALIES,
  MOCK_AI_INSIGHT,
} from './mock/executive-overview.mock';

@Component({
  selector: 'app-executive-overview',
  standalone: true,
  imports: [
    CommonModule,
    KpiCardComponent,
    PanelComponent,
    BadgeComponent,
    FilterBarComponent,
    IconComponent,
    AreaLineChartComponent,
    HorizontalBarChartComponent,
    DonutChartComponent,
    DataTableComponent,
  ],
  templateUrl: './executive-overview.component.html',
  styleUrl: './executive-overview.component.scss',
})
export class ExecutiveOverviewComponent {
  readonly kpis = MOCK_KPIS;
  readonly revenueSeries = MOCK_REVENUE_SERIES;
  readonly revenueByChannel = MOCK_REVENUE_BY_CHANNEL;
  readonly usersBySegment = MOCK_USERS_BY_SEGMENT;
  readonly recentAnomalies = MOCK_RECENT_ANOMALIES;
  readonly aiInsight = MOCK_AI_INSIGHT;

  readonly anomalyColumns: TableColumn[] = [
    { key: 'metric', label: 'Metric' },
    { key: 'date', label: 'Date' },
    { key: 'impact', label: 'Impact' },
    { key: 'confidence', label: 'AI Confidence', align: 'right' },
  ];

  impactTone(impact: string): 'error' | 'warning' | 'info' {
    if (impact === 'High') return 'error';
    if (impact === 'Medium') return 'warning';
    return 'info';
  }
}
