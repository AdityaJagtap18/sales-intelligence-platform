import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface LogEntry {
  timestamp: string;
  level: string;
  logger: string;
  thread: string;
  message: string;
}

export interface Trace {
  timestamp: string;
  method: string;
  uri: string;
  status: number | null;
  durationMs: number | null;
}

@Injectable({ providedIn: 'root' })
export class LogsTracesApiService {
  private readonly baseUrl = `${environment.apiBaseUrl}/observability`;

  constructor(private readonly http: HttpClient) {}

  logs(): Observable<LogEntry[]> {
    return this.http.get<LogEntry[]>(`${this.baseUrl}/logs`);
  }

  traces(): Observable<Trace[]> {
    return this.http.get<Trace[]>(`${this.baseUrl}/traces`);
  }
}
