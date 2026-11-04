import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { AnalyticsApiService, SalesSummary } from './analytics-api.service';
import { environment } from '../../../environments/environment';

describe('AnalyticsApiService', () => {
  let service: AnalyticsApiService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [AnalyticsApiService],
    });
    service = TestBed.inject(AnalyticsApiService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('requests the sales summary from the analytics base URL and returns it unchanged', () => {
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
    let result: SalesSummary | undefined;

    service.salesSummary().subscribe((r) => (result = r));

    const req = httpMock.expectOne(`${environment.apiBaseUrl}/analytics/sales-summary`);
    expect(req.request.method).toBe('GET');
    req.flush(summary);

    expect(result).toEqual(summary);
  });

  it('requests anomalies from the correct endpoint', () => {
    service.anomalies().subscribe();

    const req = httpMock.expectOne(`${environment.apiBaseUrl}/analytics/anomalies`);
    expect(req.request.method).toBe('GET');
    req.flush([]);
  });
});
