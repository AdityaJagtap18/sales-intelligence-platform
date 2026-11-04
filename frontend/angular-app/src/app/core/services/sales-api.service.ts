import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { LabelValue } from './analytics-api.service';

export interface SalesFilterParams {
  from?: string;
  to?: string;
  category?: string;
  territory?: string;
}

export interface SalesSummary2 {
  totalRevenue: number;
  totalOrders: number;
  totalCustomers: number;
  avgOrderValue: number;
  revenueGrowthPct: number | null;
  periodLabel: string;
}

export interface SalesOrderDetail {
  orderNumber: string;
  orderDate: string;
  customer: string;
  territory: string;
  status: string;
  totalDue: number;
  lineItemCount: number;
}

export interface FilterOptions {
  categories: string[];
  territories: string[];
  minOrderDate: string | null;
  maxOrderDate: string | null;
}

@Injectable({ providedIn: 'root' })
export class SalesApiService {
  private readonly baseUrl = `${environment.apiBaseUrl}/sales`;

  constructor(private readonly http: HttpClient) {}

  private params(filters: SalesFilterParams): Record<string, string> {
    const out: Record<string, string> = {};
    if (filters.from) out['from'] = filters.from;
    if (filters.to) out['to'] = filters.to;
    if (filters.category) out['category'] = filters.category;
    if (filters.territory) out['territory'] = filters.territory;
    return out;
  }

  summary(filters: SalesFilterParams): Observable<SalesSummary2> {
    return this.http.get<SalesSummary2>(`${this.baseUrl}/summary`, { params: this.params(filters) });
  }

  revenueByCategory(filters: SalesFilterParams): Observable<LabelValue[]> {
    return this.http.get<LabelValue[]>(`${this.baseUrl}/revenue-by-category`, { params: this.params(filters) });
  }

  revenueByTerritory(filters: SalesFilterParams): Observable<LabelValue[]> {
    return this.http.get<LabelValue[]>(`${this.baseUrl}/revenue-by-territory`, { params: this.params(filters) });
  }

  topProducts(filters: SalesFilterParams): Observable<LabelValue[]> {
    return this.http.get<LabelValue[]>(`${this.baseUrl}/top-products`, { params: this.params(filters) });
  }

  topCustomers(filters: SalesFilterParams): Observable<LabelValue[]> {
    return this.http.get<LabelValue[]>(`${this.baseUrl}/top-customers`, { params: this.params(filters) });
  }

  orderStatus(filters: SalesFilterParams): Observable<LabelValue[]> {
    return this.http.get<LabelValue[]>(`${this.baseUrl}/order-status`, { params: this.params(filters) });
  }

  orders(filters: SalesFilterParams): Observable<SalesOrderDetail[]> {
    return this.http.get<SalesOrderDetail[]>(`${this.baseUrl}/orders`, { params: this.params(filters) });
  }

  filterOptions(): Observable<FilterOptions> {
    return this.http.get<FilterOptions>(`${this.baseUrl}/filter-options`);
  }

  exportOrdersUrl(filters: SalesFilterParams): string {
    const params = new URLSearchParams(this.params(filters) as Record<string, string>);
    return `${this.baseUrl}/orders/export?${params.toString()}`;
  }
}
