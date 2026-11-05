import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface DataSourceRow {
  id: number;
  name: string;
  sourceType: 'DATABRICKS' | 'CSV' | 'JSON' | 'PARQUET' | 'POSTGRESQL';
  configSummary: string;
  enabled: boolean;
  lastTestStatus: string | null;
  lastTestMessage: string | null;
  lastTestedAt: string | null;
  lastSuccessfulIngestionAt: string | null;
}

export interface CreateDataSourceRequest {
  name: string;
  sourceType: string;
  connectionConfigJson?: string;
}

export interface PipelineDefinitionRow {
  id: number;
  name: string;
  description: string | null;
  pipelineType: string;
  sourceName: string;
  destinationName: string;
  dataSourceId: number | null;
  lastRunStatus: string | null;
  lastRunAt: string | null;
  lastRunDurationMs: number | null;
}

@Injectable({ providedIn: 'root' })
export class DataSourceApiService {
  private readonly baseUrl = `${environment.apiBaseUrl}/sources`;

  constructor(private readonly http: HttpClient) {}

  list(): Observable<DataSourceRow[]> {
    return this.http.get<DataSourceRow[]>(this.baseUrl);
  }

  register(request: CreateDataSourceRequest): Observable<DataSourceRow> {
    return this.http.post<DataSourceRow>(this.baseUrl, request);
  }

  update(id: number, request: CreateDataSourceRequest): Observable<DataSourceRow> {
    return this.http.put<DataSourceRow>(`${this.baseUrl}/${id}`, request);
  }

  disable(id: number): Observable<DataSourceRow> {
    return this.http.post<DataSourceRow>(`${this.baseUrl}/${id}/disable`, null);
  }

  enable(id: number): Observable<DataSourceRow> {
    return this.http.post<DataSourceRow>(`${this.baseUrl}/${id}/enable`, null);
  }

  testConnection(id: number): Observable<DataSourceRow> {
    return this.http.post<DataSourceRow>(`${this.baseUrl}/${id}/test-connection`, null);
  }

  pipelines(id: number): Observable<PipelineDefinitionRow[]> {
    return this.http.get<PipelineDefinitionRow[]>(`${this.baseUrl}/${id}/pipelines`);
  }
}
