import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface ContextDocument {
  id: number;
  filename: string;
  contentType: string;
  sizeBytes: number;
  status: 'PENDING' | 'INDEXED' | 'FAILED';
  chunkCount: number;
  errorMessage: string | null;
  uploadedAt: string;
}

export interface SearchResult {
  documentId: number;
  filename: string;
  chunkIndex: number;
  text: string;
  score: number;
}

/** Talks only to Spring Boot — MinIO/Qdrant/Ollama are never called directly from Angular. */
@Injectable({ providedIn: 'root' })
export class ContextLakeApiService {
  private readonly baseUrl = `${environment.apiBaseUrl}/documents`;

  constructor(private readonly http: HttpClient) {}

  listDocuments(): Observable<ContextDocument[]> {
    return this.http.get<ContextDocument[]>(this.baseUrl);
  }

  upload(file: File): Observable<ContextDocument> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<ContextDocument>(this.baseUrl, formData);
  }

  search(query: string, limit = 5): Observable<SearchResult[]> {
    return this.http.get<SearchResult[]>(`${this.baseUrl}/search`, { params: { q: query, limit } });
  }
}
