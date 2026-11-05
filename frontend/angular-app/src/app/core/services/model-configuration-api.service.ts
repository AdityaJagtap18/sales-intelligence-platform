import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface ModelConfiguration {
  activeChatModel: string;
  defaultChatModel: string;
  embeddingModel: string;
  availableModels: string[];
  ollamaStatus: 'UP' | 'DOWN';
  updatedAt: string | null;
}

@Injectable({ providedIn: 'root' })
export class ModelConfigurationApiService {
  private readonly baseUrl = `${environment.apiBaseUrl}/model-configuration`;

  constructor(private readonly http: HttpClient) {}

  current(): Observable<ModelConfiguration> {
    return this.http.get<ModelConfiguration>(this.baseUrl);
  }

  setChatModel(model: string): Observable<ModelConfiguration> {
    return this.http.post<ModelConfiguration>(`${this.baseUrl}/chat-model`, { model });
  }
}
