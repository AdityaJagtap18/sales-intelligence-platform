import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface ForecastPoint {
  label: string;
  actual: number | null;
  predicted: number | null;
  lowerBound: number | null;
  upperBound: number | null;
}

export interface ForecastRun {
  id: number | null;
  metric: string;
  horizonPeriods: number;
  status: 'COMPLETED' | 'INSUFFICIENT_DATA';
  method: string | null;
  mae: number | null;
  mape: number | null;
  reason: string | null;
  historical: ForecastPoint[];
  predicted: ForecastPoint[];
  createdAt: string | null;
}

@Injectable({ providedIn: 'root' })
export class ForecastingApiService {
  private readonly baseUrl = `${environment.apiBaseUrl}/analytics/forecasting`;

  constructor(private readonly http: HttpClient) {}

  generate(metric: 'REVENUE' | 'ORDER_VOLUME', horizon: number): Observable<ForecastRun> {
    return this.http.post<ForecastRun>(`${this.baseUrl}/generate`, null, { params: { metric, horizon: String(horizon) } });
  }

  history(): Observable<ForecastRun[]> {
    return this.http.get<ForecastRun[]>(`${this.baseUrl}/history`);
  }
}
