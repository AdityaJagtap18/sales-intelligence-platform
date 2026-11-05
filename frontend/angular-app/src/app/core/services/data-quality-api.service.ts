import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { PipelineRun } from './pipeline-api.service';

export interface QualityCheckRow {
  tableName: string;
  checkName: string;
  status: 'PASS' | 'FAIL' | 'SKIPPED';
}

export interface DatasetStatus {
  tableName: string;
  total: number;
  passed: number;
  failed: number;
  skipped: number;
  overallStatus: string;
}

export interface QualitySummary {
  runId: string | null;
  totalChecks: number;
  passed: number;
  failed: number;
  skipped: number;
  qualityScorePct: number | null;
  byDataset: DatasetStatus[];
  checks: QualityCheckRow[];
}

export interface QualityRunHistory {
  runId: string;
  totalChecks: number;
  passed: number;
  failed: number;
  skipped: number;
}

@Injectable({ providedIn: 'root' })
export class DataQualityApiService {
  private readonly baseUrl = `${environment.apiBaseUrl}/data-quality`;

  constructor(private readonly http: HttpClient) {}

  latest(): Observable<QualitySummary> {
    return this.http.get<QualitySummary>(`${this.baseUrl}/latest`);
  }

  history(): Observable<QualityRunHistory[]> {
    return this.http.get<QualityRunHistory[]>(`${this.baseUrl}/history`);
  }

  runDetail(runId: string): Observable<QualitySummary> {
    return this.http.get<QualitySummary>(`${this.baseUrl}/runs/${encodeURIComponent(runId)}`);
  }

  execute(): Observable<PipelineRun> {
    return this.http.post<PipelineRun>(`${this.baseUrl}/execute`, null);
  }

  exportUrl(): string {
    return `${this.baseUrl}/export`;
  }
}
