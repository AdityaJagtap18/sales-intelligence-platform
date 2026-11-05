import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface SettingEntry {
  key: string;
  value: string;
}

export interface SettingsSection {
  section: string;
  entries: SettingEntry[];
}

@Injectable({ providedIn: 'root' })
export class SettingsApiService {
  private readonly baseUrl = `${environment.apiBaseUrl}/settings`;

  constructor(private readonly http: HttpClient) {}

  effectiveConfiguration(): Observable<SettingsSection[]> {
    return this.http.get<SettingsSection[]>(this.baseUrl);
  }
}
