import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface Alert {
  id: number;
  sourceModule: string;
  severity: 'CRITICAL' | 'WARNING' | 'INFO';
  title: string;
  message: string;
  status: 'OPEN' | 'ACKNOWLEDGED' | 'RESOLVED';
  createdAt: string;
  updatedAt: string;
  resolvedAt: string | null;
  acknowledgedBy: string | null;
}

@Injectable({ providedIn: 'root' })
export class AlertsApiService {
  private readonly baseUrl = `${environment.apiBaseUrl}/observability/alerts`;

  constructor(private readonly http: HttpClient) {}

  list(status?: string, severity?: string): Observable<Alert[]> {
    const params: Record<string, string> = {};
    if (status) params['status'] = status;
    if (severity) params['severity'] = severity;
    return this.http.get<Alert[]>(this.baseUrl, { params });
  }

  acknowledge(id: number, acknowledgedBy: string): Observable<Alert> {
    return this.http.post<Alert>(`${this.baseUrl}/${id}/acknowledge`, { acknowledgedBy });
  }

  resolve(id: number): Observable<Alert> {
    return this.http.post<Alert>(`${this.baseUrl}/${id}/resolve`, null);
  }
}
