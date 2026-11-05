import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface RelatedEntity {
  type: string;
  name: string;
}

export interface Citation {
  documentId: number;
  filename: string;
  chunkIndex: number;
  score: number;
  snippet: string;
  relatedEntities: RelatedEntity[];
}

export interface AiInsightResponse {
  runId: number;
  question: string;
  answer: string | null;
  citations: Citation[];
  status: 'ANSWERED' | 'NO_SOURCES' | 'INSUFFICIENT_CONTEXT' | 'GENERATION_FAILED';
  note: string | null;
}

export interface AiInsightHistoryEntry {
  id: number;
  question: string;
  status: string;
  citationCount: number;
  askedAt: string;
}

@Injectable({ providedIn: 'root' })
export class AiInsightsApiService {
  private readonly baseUrl = `${environment.apiBaseUrl}/ai-insights`;

  constructor(private readonly http: HttpClient) {}

  ask(question: string): Observable<AiInsightResponse> {
    return this.http.post<AiInsightResponse>(`${this.baseUrl}/ask`, { question });
  }

  history(): Observable<AiInsightHistoryEntry[]> {
    return this.http.get<AiInsightHistoryEntry[]>(`${this.baseUrl}/history`);
  }
}
