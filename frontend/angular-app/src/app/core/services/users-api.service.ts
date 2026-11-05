import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface AppUser {
  id: number;
  username: string;
  role: 'ADMIN' | 'ANALYST' | 'VIEWER';
  enabled: boolean;
  createdAt: string;
  lastLoginAt: string | null;
}

export interface CreateUserRequest {
  username: string;
  password: string;
  role: string;
}

@Injectable({ providedIn: 'root' })
export class UsersApiService {
  private readonly baseUrl = `${environment.apiBaseUrl}/users`;

  constructor(private readonly http: HttpClient) {}

  list(): Observable<AppUser[]> {
    return this.http.get<AppUser[]>(this.baseUrl);
  }

  create(request: CreateUserRequest): Observable<AppUser> {
    return this.http.post<AppUser>(this.baseUrl, request);
  }

  setRole(id: number, role: string): Observable<AppUser> {
    return this.http.post<AppUser>(`${this.baseUrl}/${id}/role`, { role });
  }

  setEnabled(id: number, enabled: boolean): Observable<AppUser> {
    return this.http.post<AppUser>(`${this.baseUrl}/${id}/enabled`, { enabled });
  }

  resetPassword(id: number, newPassword: string): Observable<AppUser> {
    return this.http.post<AppUser>(`${this.baseUrl}/${id}/reset-password`, { newPassword });
  }
}
