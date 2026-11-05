import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { PipelineRun } from './pipeline-api.service';

export interface PipelineReliability {
  definitionId: number;
  name: string;
  totalRuns: number;
  successfulRuns: number;
  failedRuns: number;
  successRatePct: number | null;
  lastRunStatus: string | null;
}

@Injectable({ providedIn: 'root' })
export class PipelineMonitoringApiService {
  private readonly baseUrl = `${environment.apiBaseUrl}/observability/pipeline-monitoring`;

  constructor(private readonly http: HttpClient) {}

  reliability(): Observable<PipelineReliability[]> {
    return this.http.get<PipelineReliability[]>(`${this.baseUrl}/reliability`);
  }

  runs(): Observable<PipelineRun[]> {
    return this.http.get<PipelineRun[]>(`${this.baseUrl}/runs`);
  }

  trigger(definitionId: number): Observable<PipelineRun> {
    return this.http.post<PipelineRun>(`${this.baseUrl}/trigger/${definitionId}`, null);
  }
}
