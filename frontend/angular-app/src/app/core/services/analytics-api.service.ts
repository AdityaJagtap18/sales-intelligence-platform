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
  metric: string;
  date: string;
  impact: 'High' | 'Medium' | 'Low';
  direction: 'up' | 'down';
  confidence: number;
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

@Injectable({ providedIn: 'root' })
export class AnalyticsApiService {
  private readonly baseUrl = `${environment.apiBaseUrl}/analytics`;

  constructor(private readonly http: HttpClient) {}

  salesSummary(): Observable<SalesSummary> {
    return this.http.get<SalesSummary>(`${this.baseUrl}/sales-summary`);
  }

  revenueTrend(): Observable<RevenueTrendPoint[]> {
    return this.http.get<RevenueTrendPoint[]>(`${this.baseUrl}/revenue-trend`);
  }

  revenueByCategory(): Observable<LabelValue[]> {
    return this.http.get<LabelValue[]>(`${this.baseUrl}/revenue-by-category`);
  }

  revenueByTopCustomers(): Observable<LabelValue[]> {
    return this.http.get<LabelValue[]>(`${this.baseUrl}/revenue-by-top-customers`);
  }

  anomalies(): Observable<AnomalyRow[]> {
    return this.http.get<AnomalyRow[]>(`${this.baseUrl}/anomalies`);
  }

  orders(): Observable<OrderRow[]> {
    return this.http.get<OrderRow[]>(`${this.baseUrl}/orders`);
  }

  products(): Observable<ProductPerformance[]> {
    return this.http.get<ProductPerformance[]>(`${this.baseUrl}/products`);
  }
}
