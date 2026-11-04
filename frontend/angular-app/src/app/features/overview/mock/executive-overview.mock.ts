import { AreaLinePoint } from '../../../shared/components/charts/area-line-chart.component';
import { BarDatum } from '../../../shared/components/charts/horizontal-bar-chart.component';
import { DonutDatum } from '../../../shared/components/charts/donut-chart.component';

/**
 * MOCK DATA — Phase 2 only.
 * No pipeline, warehouse, or Gold-layer dataset exists yet (that's Phase 4+),
 * so every number on this screen is illustrative. Every consumer of this
 * file must keep the "Mock data" badge visible in the UI per engineering
 * principle #15 (clearly identify mock data) — see executive-overview.component.html.
 */

export interface KpiMock {
  label: string;
  value: string;
  icon: string;
  deltaLabel: string;
  trend: 'up' | 'down';
  positiveIsGood: boolean;
  sparkline: number[];
}

export const MOCK_KPIS: KpiMock[] = [
  { label: 'Total Revenue', value: '$2,485,300', icon: 'database', deltaLabel: '18.6% vs prior period', trend: 'up', positiveIsGood: true, sparkline: [40, 55, 48, 62, 58, 70, 64, 80] },
  { label: 'Users', value: '1,236,333', icon: 'users', deltaLabel: '12.4% vs prior period', trend: 'up', positiveIsGood: true, sparkline: [30, 34, 33, 40, 44, 46, 52, 58] },
  { label: 'Conversion Rate', value: '3.42%', icon: 'trending-up', deltaLabel: '8.7% vs prior period', trend: 'up', positiveIsGood: true, sparkline: [20, 25, 22, 28, 30, 27, 33, 36] },
  { label: 'Avg. Order Value', value: '$98.45', icon: 'box', deltaLabel: '6.1% vs prior period', trend: 'up', positiveIsGood: true, sparkline: [50, 48, 52, 51, 55, 53, 58, 60] },
  { label: 'Churn Rate', value: '1.32%', icon: 'alert-triangle', deltaLabel: '9.3% vs prior period', trend: 'down', positiveIsGood: true, sparkline: [60, 56, 58, 50, 47, 44, 40, 38] },
];

function buildRevenueSeries(): AreaLinePoint[] {
  const base = [210, 230, 350, 300, 260, 240, 220, 210, 235, 260, 250, 240, 230, 250, 260, 255, 245, 235, 240, 250, 255, 260, 250, 240, 230, 220, 245, 395, 330, 300, 290];
  return base.map((v, i) => {
    const day = i + 1;
    return {
      label: day === 1 || day % 5 === 0 ? `May ${day}` : '',
      showLabel: day === 1 || day % 5 === 0,
      value: v,
      expected: Math.round(v * (0.85 + (i % 4) * 0.02)),
      anomaly: day === 29,
    };
  });
}

export const MOCK_REVENUE_SERIES: AreaLinePoint[] = buildRevenueSeries();

export const MOCK_REVENUE_BY_CHANNEL: BarDatum[] = [
  { label: 'Organic', value: 42.1 },
  { label: 'Paid Search', value: 23.7 },
  { label: 'Email', value: 15.8 },
  { label: 'Referral', value: 9.3 },
  { label: 'Social', value: 5.1 },
  { label: 'Direct', value: 4.0 },
];

export const MOCK_USERS_BY_SEGMENT: DonutDatum[] = [
  { label: 'Enterprise', value: 36.4 },
  { label: 'Growth', value: 28.1 },
  { label: 'Starter', value: 17.7 },
  { label: 'Freemium', value: 11.3 },
  { label: 'Trial', value: 6.5 },
];

export interface AnomalyRow {
  metric: string;
  date: string;
  impact: 'High' | 'Medium' | 'Low';
  direction: 'up' | 'down';
  confidence: number;
}

export const MOCK_RECENT_ANOMALIES: AnomalyRow[] = [
  { metric: 'Checkout Requests', date: 'May 29, 2026', impact: 'High', direction: 'up', confidence: 92 },
  { metric: 'Discount Requests', date: 'May 24, 2026', impact: 'Medium', direction: 'up', confidence: 86 },
  { metric: 'Discount Claims', date: 'May 18, 2026', impact: 'Medium', direction: 'down', confidence: 78 },
  { metric: 'Reconciliation Disputes', date: 'May 12, 2026', impact: 'Low', direction: 'up', confidence: 65 },
  { metric: 'Range Order Cases', date: 'May 7, 2026', impact: 'Low', direction: 'down', confidence: 60 },
];

export const MOCK_AI_INSIGHT = {
  title: 'Unusual spike in revenue detected on May 29.',
  body: 'This is driven by a surge in New Users from Organic Search and a 23% increase in Avg. Order Value.',
  confidence: 92,
  factors: [
    { label: 'New Users (Organic Search)', delta: '+67%', weight: 90 },
    { label: 'Avg. Order Value Increase', delta: '+23%', weight: 55 },
    { label: 'Marketing Campaign: Spring Sale', delta: '+18%', weight: 40 },
  ],
};
