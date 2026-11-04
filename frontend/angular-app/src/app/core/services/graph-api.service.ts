import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface GraphNode {
  id: string;
  label: string;
  properties: Record<string, unknown>;
}

export interface GraphEdge {
  id: string;
  type: string;
  startNodeId: string;
  endNodeId: string;
  properties: Record<string, unknown>;
}

export interface GraphTraversal {
  nodes: GraphNode[];
  edges: GraphEdge[];
}

export interface GraphStats {
  nodeCount: number;
  relationshipCount: number;
  nodeCountsByLabel: Record<string, number>;
}

export interface GraphEntityRef {
  id: string;
  label: string;
}

export interface GraphSyncResult {
  customers: number;
  addresses: number;
  products: number;
  categories: number;
  orders: number;
  orderLines: number;
  documents: number;
  mentions: number;
}

@Injectable({ providedIn: 'root' })
export class GraphApiService {
  private readonly baseUrl = `${environment.apiBaseUrl}/graph`;

  constructor(private readonly http: HttpClient) {}

  sync(): Observable<GraphSyncResult> {
    return this.http.post<GraphSyncResult>(`${this.baseUrl}/sync`, {});
  }

  stats(): Observable<GraphStats> {
    return this.http.get<GraphStats>(`${this.baseUrl}/stats`);
  }

  listEntities(type: string, limit = 50): Observable<GraphEntityRef[]> {
    return this.http.get<GraphEntityRef[]>(`${this.baseUrl}/entities/${type}`, { params: { limit } });
  }

  getEntity(type: string, id: string, depth = 2): Observable<GraphTraversal> {
    return this.http.get<GraphTraversal>(`${this.baseUrl}/entity/${type}/${id}`, { params: { depth } });
  }
}
