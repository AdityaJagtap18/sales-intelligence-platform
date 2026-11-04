import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface PipelineRun {
  id: number;
  jobId: number;
  databricksRunId: number;
  lifecycleState: string;
  resultState: string | null;
  stateMessage: string | null;
  triggeredBy: string;
  startedAt: string;
  updatedAt: string;
  completedAt: string | null;
}

/**
 * Talks only to Spring Boot's /pipelines endpoints — never to Databricks
 * directly. Spring Boot is the sole holder of Databricks credentials (ADR-005).
 */
@Injectable({ providedIn: 'root' })
export class PipelineApiService {
  private readonly baseUrl = `${environment.apiBaseUrl}/pipelines`;

  constructor(private readonly http: HttpClient) {}

  triggerRun(): Observable<PipelineRun> {
    return this.http.post<PipelineRun>(`${this.baseUrl}/run`, {});
  }

  listRuns(): Observable<PipelineRun[]> {
    return this.http.get<PipelineRun[]>(`${this.baseUrl}/runs`);
  }

  getRun(id: number): Observable<PipelineRun> {
    return this.http.get<PipelineRun>(`${this.baseUrl}/runs/${id}`);
  }
}
