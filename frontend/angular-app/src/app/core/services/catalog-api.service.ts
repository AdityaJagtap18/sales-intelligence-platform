import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface ColumnInfo {
  name: string;
  dataType: string;
}

export interface CatalogDataset {
  id: number;
  tableName: string;
  rowCount: number;
  columns: ColumnInfo[];
  lastRefreshedAt: string;
}

/** Talks only to Spring Boot — never queries Databricks directly from Angular. */
@Injectable({ providedIn: 'root' })
export class CatalogApiService {
  private readonly baseUrl = `${environment.apiBaseUrl}/catalog`;

  constructor(private readonly http: HttpClient) {}

  listDatasets(): Observable<CatalogDataset[]> {
    return this.http.get<CatalogDataset[]>(`${this.baseUrl}/datasets`);
  }

  refresh(): Observable<{ synced: number }> {
    return this.http.post<{ synced: number }>(`${this.baseUrl}/refresh`, {});
  }
}
