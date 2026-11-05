import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface ReleaseNote {
  version: string;
  title: string;
  added: string[];
  changed: string[];
  fixed: string[];
}

export interface ReleaseNotesResponse {
  currentVersion: string;
  notes: ReleaseNote[];
}

@Injectable({ providedIn: 'root' })
export class ReleaseNotesApiService {
  private readonly baseUrl = `${environment.apiBaseUrl}/release-notes`;

  constructor(private readonly http: HttpClient) {}

  get(): Observable<ReleaseNotesResponse> {
    return this.http.get<ReleaseNotesResponse>(this.baseUrl);
  }
}
