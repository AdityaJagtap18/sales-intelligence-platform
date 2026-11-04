import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { SalesExplorerComponent } from './sales-explorer.component';
import { AnalyticsApiService, OrderRow, SalesSummary } from '../../core/services/analytics-api.service';

describe('SalesExplorerComponent', () => {
  const orders: OrderRow[] = [
    { orderNumber: 'SO71784', orderDate: '2008-06-01', customerName: 'Action Bicycle Specialists', totalDue: 119960.82, itemCount: 43 },
    { orderNumber: 'SO71936', orderDate: '2008-06-01', customerName: 'Metropolitan Bicycle Supply', totalDue: 108597.95, itemCount: 46 },
  ];
  const summary: SalesSummary = {
    totalRevenue: 228558.77,
    totalOrders: 2,
    avgOrderValue: 114279.385,
    totalCustomers: 2,
    totalProducts: 10,
    topCategoryName: 'Bikes',
    topCategoryRevenue: 228558.77,
    topCategorySharePct: 100,
  };

  async function createComponent() {
    await TestBed.configureTestingModule({
      imports: [SalesExplorerComponent],
      providers: [
        {
          provide: AnalyticsApiService,
          useValue: { salesSummary: () => of(summary), orders: () => of(orders) },
        },
      ],
    }).compileComponents();

    const fixture = TestBed.createComponent(SalesExplorerComponent);
    fixture.detectChanges();
    return fixture.componentInstance;
  }

  it('shows every order when the search box is empty', async () => {
    const component = await createComponent();

    expect(component.rows.length).toBe(2);
  });

  it('filters rows by a case-insensitive match on customer name', async () => {
    const component = await createComponent();

    component.search.set('metropolitan');

    expect(component.rows.length).toBe(1);
    expect(component.rows[0].orderNumber).toBe('SO71936');
  });

  it('filters rows by a case-insensitive match on order number', async () => {
    const component = await createComponent();

    component.search.set('so71784');

    expect(component.rows.length).toBe(1);
    expect(component.rows[0].customerName).toBe('Action Bicycle Specialists');
  });

  it('formats the total due as a dollar amount on each row', async () => {
    const component = await createComponent();

    expect(component.rows[0].totalDueFormatted).toBe('$119,960.82');
  });
});
