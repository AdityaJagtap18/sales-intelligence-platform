import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface SalesSummary {
  totalRevenue: number;
  totalOrders: number;
  avgOrderValue: number;
  totalCustomers: number;
  totalProducts: number;
  topCategoryName: string;
  topCategoryRevenue: number;
  topCategorySharePct: number;
}

export interface RevenueTrendPoint {
  label: string;
  value: number;
  expected: number;
  anomaly: boolean;
}

export interface LabelValue {
  label: string;
  value: number;
}

export interface AnomalyRow {
  key: string;
  metric: string;
  date: string;
  impact: 'High' | 'Medium' | 'Low';
  direction: 'up' | 'down';
  confidence: number;
  expectedValue: number;
  observedValue: number;
  acknowledgementStatus: 'ACKNOWLEDGED' | 'RESOLVED' | null;
  acknowledgementNote: string | null;
}

export interface OrderRow {
  orderNumber: string;
  orderDate: string;
  customerName: string;
  totalDue: number;
  itemCount: number;
}

export interface ProductPerformance {
  name: string;
  category: string;
  unitsSold: number;
  revenue: number;
}

export interface DateRange {
  from?: string;
  to?: string;
}

@Injectable({ providedIn: 'root' })
export class AnalyticsApiService {
  private readonly baseUrl = `${environment.apiBaseUrl}/analytics`;

  constructor(private readonly http: HttpClient) {}

  private rangeParams(range?: DateRange): Record<string, string> {
    const params: Record<string, string> = {};
    if (range?.from) params['from'] = range.from;
    if (range?.to) params['to'] = range.to;
    return params;
  }

  salesSummary(range?: DateRange): Observable<SalesSummary> {
    return this.http.get<SalesSummary>(`${this.baseUrl}/sales-summary`, { params: this.rangeParams(range) });
  }

  revenueTrend(): Observable<RevenueTrendPoint[]> {
    return this.http.get<RevenueTrendPoint[]>(`${this.baseUrl}/revenue-trend`);
  }

  revenueByCategory(range?: DateRange): Observable<LabelValue[]> {
    return this.http.get<LabelValue[]>(`${this.baseUrl}/revenue-by-category`, { params: this.rangeParams(range) });
  }

  revenueByTopCustomers(range?: DateRange): Observable<LabelValue[]> {
    return this.http.get<LabelValue[]>(`${this.baseUrl}/revenue-by-top-customers`, { params: this.rangeParams(range) });
  }

  anomalies(severity?: string, metric?: string): Observable<AnomalyRow[]> {
    const params: Record<string, string> = {};
    if (severity) params['severity'] = severity;
    if (metric) params['metric'] = metric;
    return this.http.get<AnomalyRow[]>(`${this.baseUrl}/anomalies`, { params });
  }

  acknowledgeAnomaly(key: string, status: 'ACKNOWLEDGED' | 'RESOLVED', note?: string): Observable<unknown> {
    return this.http.post(`${this.baseUrl}/anomalies/${encodeURIComponent(key)}/acknowledge`, { status, note });
  }

  orders(range?: DateRange): Observable<OrderRow[]> {
    return this.http.get<OrderRow[]>(`${this.baseUrl}/orders`, { params: this.rangeParams(range) });
  }

  products(range?: DateRange): Observable<ProductPerformance[]> {
    return this.http.get<ProductPerformance[]>(`${this.baseUrl}/products`, { params: this.rangeParams(range) });
  }
}
