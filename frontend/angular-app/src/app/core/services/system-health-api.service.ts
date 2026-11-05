import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface ComponentHealth {
  name: string;
  category: string;
  status: 'UP' | 'DOWN' | 'DEGRADED' | 'NOT_CONFIGURED';
  latencyMs: number | null;
  message: string | null;
}

export interface SystemHealth {
  overallStatus: string;
  checkedAt: string;
  components: ComponentHealth[];
}

@Injectable({ providedIn: 'root' })
export class SystemHealthApiService {
  private readonly baseUrl = `${environment.apiBaseUrl}/observability/system-health`;

  constructor(private readonly http: HttpClient) {}

  check(): Observable<SystemHealth> {
    return this.http.get<SystemHealth>(this.baseUrl);
  }
}
