import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { LabelValue } from './analytics-api.service';

export interface CustomerSummary {
  totalCustomers: number;
  customersWithOrders: number;
  returningCustomers: number;
  inactiveCustomers: number;
  avgPurchaseValue: number;
  avgOrdersPerCustomer: number;
}

export interface CustomerRow {
  customerId: number;
  companyName: string;
  territory: string;
  orderCount: number;
  lifetimeRevenue: number;
  avgOrderValue: number;
  segment: string;
}

export interface OrderRow {
  orderNumber: string;
  orderDate: string;
  customerName: string;
  totalDue: number;
  itemCount: number;
}

export interface CustomerDetail extends CustomerRow {
  emailAddress: string | null;
  orders: OrderRow[];
}

@Injectable({ providedIn: 'root' })
export class CustomerAnalyticsApiService {
  private readonly baseUrl = `${environment.apiBaseUrl}/analytics/customers`;

  constructor(private readonly http: HttpClient) {}

  summary(): Observable<CustomerSummary> {
    return this.http.get<CustomerSummary>(`${this.baseUrl}/summary`);
  }

  geographicDistribution(): Observable<LabelValue[]> {
    return this.http.get<LabelValue[]>(`${this.baseUrl}/geographic-distribution`);
  }

  customers(search?: string, segment?: string, sortBy = 'revenue', sortDir = 'desc'): Observable<CustomerRow[]> {
    const params: Record<string, string> = { sortBy, sortDir };
    if (search) params['search'] = search;
    if (segment) params['segment'] = segment;
    return this.http.get<CustomerRow[]>(this.baseUrl, { params });
  }

  detail(id: number): Observable<CustomerDetail> {
    return this.http.get<CustomerDetail>(`${this.baseUrl}/${id}`);
  }
}
