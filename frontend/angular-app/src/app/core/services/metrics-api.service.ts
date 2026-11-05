import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface MetricSummary {
  name: string;
  type: string;
  seriesCount: number;
}

export interface MetricSample {
  tags: Record<string, string>;
  statistic: string;
  value: number;
}

export interface MetricDetail {
  name: string;
  samples: MetricSample[];
}

@Injectable({ providedIn: 'root' })
export class MetricsApiService {
  private readonly baseUrl = `${environment.apiBaseUrl}/observability/metrics`;

  constructor(private readonly http: HttpClient) {}

  list(): Observable<MetricSummary[]> {
    return this.http.get<MetricSummary[]>(this.baseUrl);
  }

  detail(name: string): Observable<MetricDetail> {
    return this.http.get<MetricDetail>(`${this.baseUrl}/${encodeURIComponent(name)}`);
  }
}
