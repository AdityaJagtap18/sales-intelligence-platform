import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { ProductAnalyticsComponent } from './product-analytics.component';
import { AnalyticsApiService, ProductPerformance } from '../../core/services/analytics-api.service';

describe('ProductAnalyticsComponent', () => {
  const products: ProductPerformance[] = [
    { name: 'Touring-1000', category: 'Touring Bikes', unitsSold: 60, revenue: 37591.49 },
    { name: 'Mountain-200', category: 'Mountain Bikes', unitsSold: 42, revenue: 20778.84 },
  ];

  async function createComponent(apiProducts: ProductPerformance[]) {
    await TestBed.configureTestingModule({
      imports: [ProductAnalyticsComponent],
      providers: [
        {
          provide: AnalyticsApiService,
          useValue: { products: () => of(apiProducts) },
        },
      ],
    }).compileComponents();

    const fixture = TestBed.createComponent(ProductAnalyticsComponent);
    fixture.detectChanges();
    return fixture.componentInstance;
  }

  it('normalizes the top-8 chart values to a percentage share of their combined revenue', async () => {
    const component = await createComponent(products);

    const chart = component.topProductsChart;

    const total = products.reduce((sum, p) => sum + p.revenue, 0);
    const expectedFirstShare = Math.round((products[0].revenue / total) * 1000) / 10;
    expect(chart[0].value).toBe(expectedFirstShare);
    expect(chart[0].label).toBe('Touring-1000');
  });

  it('formats each row with a dollar-formatted revenue string for the data table', async () => {
    const component = await createComponent(products);

    const rows = component.rows;

    expect(rows[0].revenueFormatted).toBe('$37,591.49');
  });

  it('does not divide by zero when there are no products yet', async () => {
    const component = await createComponent([]);

    expect(component.topProductsChart).toEqual([]);
  });
});
