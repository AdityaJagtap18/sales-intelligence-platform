import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { SalesFilterParams } from './sales-api.service';

export interface ReportDefinition {
  key: string;
  name: string;
  category: string;
  description: string;
}

export interface ReportResult {
  executionId: number;
  reportKey: string;
  reportName: string;
  status: string;
  columns: string[];
  rows: Record<string, unknown>[];
  generatedAt: string;
  errorMessage: string | null;
}

export interface ReportExecution {
  id: number;
  reportKey: string;
  reportName: string;
  status: string;
  rowCount: number | null;
  errorMessage: string | null;
  generatedAt: string;
}

@Injectable({ providedIn: 'root' })
export class ReportsApiService {
  private readonly baseUrl = `${environment.apiBaseUrl}/reports`;

  constructor(private readonly http: HttpClient) {}

  definitions(): Observable<ReportDefinition[]> {
    return this.http.get<ReportDefinition[]>(`${this.baseUrl}/definitions`);
  }

  generate(key: string, filters: SalesFilterParams = {}): Observable<ReportResult> {
    const params: Record<string, string> = {};
    if (filters.from) params['from'] = filters.from;
    if (filters.to) params['to'] = filters.to;
    if (filters.category) params['category'] = filters.category;
    if (filters.territory) params['territory'] = filters.territory;
    return this.http.post<ReportResult>(`${this.baseUrl}/${key}/generate`, null, { params });
  }

  history(): Observable<ReportExecution[]> {
    return this.http.get<ReportExecution[]>(`${this.baseUrl}/history`);
  }

  exportUrl(key: string, filters: SalesFilterParams = {}): string {
    const params = new URLSearchParams();
    if (filters.from) params.set('from', filters.from);
    if (filters.to) params.set('to', filters.to);
    if (filters.category) params.set('category', filters.category);
    if (filters.territory) params.set('territory', filters.territory);
    return `${this.baseUrl}/${key}/export?${params.toString()}`;
  }
}
