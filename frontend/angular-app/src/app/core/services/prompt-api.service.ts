import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface PromptSummary {
  id: number;
  name: string;
  description: string | null;
  latestVersionNumber: number;
  createdAt: string;
}

export interface PromptVersion {
  id: number;
  versionNumber: number;
  template: string;
  createdAt: string;
}

export interface PromptDetail {
  id: number;
  name: string;
  description: string | null;
  createdAt: string;
  versions: PromptVersion[];
}

export type PromptRating = 'PASS' | 'FAIL' | 'UNRATED';

export interface PromptRun {
  id: number;
  promptVersionId: number;
  versionNumber: number;
  renderedPrompt: string;
  output: string | null;
  model: string;
  rating: PromptRating;
  latencyMs: number | null;
  errorMessage: string | null;
  createdAt: string;
}

export interface CreatePromptRequest {
  name: string;
  description?: string;
  template: string;
}

@Injectable({ providedIn: 'root' })
export class PromptApiService {
  private readonly baseUrl = `${environment.apiBaseUrl}/prompts`;

  constructor(private readonly http: HttpClient) {}

  list(): Observable<PromptSummary[]> {
    return this.http.get<PromptSummary[]>(this.baseUrl);
  }

  create(request: CreatePromptRequest): Observable<PromptDetail> {
    return this.http.post<PromptDetail>(this.baseUrl, request);
  }

  get(id: number): Observable<PromptDetail> {
    return this.http.get<PromptDetail>(`${this.baseUrl}/${id}`);
  }

  addVersion(id: number, template: string): Observable<PromptVersion> {
    return this.http.post<PromptVersion>(`${this.baseUrl}/${id}/versions`, { template });
  }

  listRuns(id: number): Observable<PromptRun[]> {
    return this.http.get<PromptRun[]>(`${this.baseUrl}/${id}/runs`);
  }

  run(id: number, versionId: number | null, input: string): Observable<PromptRun> {
    return this.http.post<PromptRun>(`${this.baseUrl}/${id}/run`, { versionId, input });
  }

  rate(runId: number, rating: PromptRating): Observable<PromptRun> {
    return this.http.put<PromptRun>(`${this.baseUrl}/runs/${runId}/rating`, { rating });
  }
}
