import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { ExecutiveOverviewComponent } from './executive-overview.component';
import { AnalyticsApiService, AnomalyRow, LabelValue, RevenueTrendPoint, SalesSummary } from '../../core/services/analytics-api.service';

describe('ExecutiveOverviewComponent', () => {
  const summary: SalesSummary = {
    totalRevenue: 956303.59,
    totalOrders: 32,
    avgOrderValue: 29884.49,
    totalCustomers: 847,
    totalProducts: 295,
    topCategoryName: 'Touring Bikes',
    topCategoryRevenue: 220655.38,
    topCategorySharePct: 23.07,
  };
  const trendPoints: RevenueTrendPoint[] = Array.from({ length: 32 }, (_, i) => ({
    label: `SO${71774 + i}`,
    value: 1000 + i,
    expected: 1000,
    anomaly: i === 31,
  }));
  const anomalies: AnomalyRow[] = [
    { metric: 'Order Value', date: 'SO71784', impact: 'High', direction: 'up', confidence: 82 },
  ];
  const topCustomers: LabelValue[] = [
    { label: 'Action Bicycle Specialists', value: 119960.82 },
    { label: 'Metropolitan Bicycle Supply', value: 108597.95 },
  ];

  async function createComponent() {
    await TestBed.configureTestingModule({
      imports: [ExecutiveOverviewComponent],
      providers: [
        {
          provide: AnalyticsApiService,
          useValue: {
            salesSummary: () => of(summary),
            revenueTrend: () => of(trendPoints),
            revenueByCategory: () => of([]),
            revenueByTopCustomers: () => of(topCustomers),
            anomalies: () => of(anomalies),
          },
        },
      ],
    }).compileComponents();

    const fixture = TestBed.createComponent(ExecutiveOverviewComponent);
    fixture.detectChanges();
    return fixture.componentInstance;
  }

  it('only marks a sparse subset of the 32 trend points to show a label, to avoid x-axis overflow', async () => {
    const component = await createComponent();

    const series = component.revenueSeries();
    const shown = series.filter((p: any) => p.showLabel);

    expect(series.length).toBe(32);
    expect(shown.length).toBeLessThan(10);
    expect(series[0].showLabel).toBeTrue();
    expect(series[series.length - 1].showLabel).toBeTrue();
  });

  it('always shows the label for an anomaly point even when it falls outside the regular stride', async () => {
    const component = await createComponent();

    const series = component.revenueSeries();
    expect(series[31].anomaly).toBeTrue();
    expect(series[31].showLabel).toBeTrue();
  });

  it('sums the top-customer donut values into a formatted total', async () => {
    const component = await createComponent();

    expect(component.topCustomersTotal).toBe('$228,559');
  });

  it('maps anomaly impact to the matching badge tone', async () => {
    const component = await createComponent();

    expect(component.impactTone('High')).toBe('error');
    expect(component.impactTone('Medium')).toBe('warning');
    expect(component.impactTone('Low')).toBe('info');
  });
});
