import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface LineageStage {
  stage: string;
  system: string;
  location: string;
  rowCount: number | null;
  detail: string;
}

export interface Lineage {
  table: string;
  stages: LineageStage[];
}

@Injectable({ providedIn: 'root' })
export class LineageApiService {
  private readonly baseUrl = `${environment.apiBaseUrl}/lineage`;

  constructor(private readonly http: HttpClient) {}

  listTables(): Observable<string[]> {
    return this.http.get<string[]>(`${this.baseUrl}/tables`);
  }

  getLineage(table: string): Observable<Lineage> {
    return this.http.get<Lineage>(`${this.baseUrl}/${table}`);
  }
}
