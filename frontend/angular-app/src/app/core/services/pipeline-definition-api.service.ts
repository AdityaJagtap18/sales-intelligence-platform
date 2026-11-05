import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { PipelineDefinitionRow } from './data-source-api.service';
import { PipelineRun } from './pipeline-api.service';

@Injectable({ providedIn: 'root' })
export class PipelineDefinitionApiService {
  private readonly baseUrl = `${environment.apiBaseUrl}/pipelines/definitions`;

  constructor(private readonly http: HttpClient) {}

  list(): Observable<PipelineDefinitionRow[]> {
    return this.http.get<PipelineDefinitionRow[]>(this.baseUrl);
  }

  runs(id: number): Observable<PipelineRun[]> {
    return this.http.get<PipelineRun[]>(`${this.baseUrl}/${id}/runs`);
  }

  trigger(id: number): Observable<PipelineRun> {
    return this.http.post<PipelineRun>(`${this.baseUrl}/${id}/trigger`, null);
  }

  retry(id: number): Observable<PipelineRun> {
    return this.http.post<PipelineRun>(`${this.baseUrl}/${id}/retry`, null);
  }
}
