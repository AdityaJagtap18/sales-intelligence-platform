import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';

import { PanelComponent } from '../../shared/components/panel/panel.component';
import { IconComponent } from '../../shared/components/icon/icon.component';
import { GraphApiService, GraphEntityRef, GraphTraversal, GraphNode } from '../../core/services/graph-api.service';

interface EntityTypeOption {
  value: string;
  display: string;
  icon: string;
}

const ENTITY_TYPES: EntityTypeOption[] = [
  { value: 'customer', display: 'Customer', icon: 'users' },
  { value: 'order', display: 'Sales Order', icon: 'file-text' },
  { value: 'product', display: 'Product', icon: 'box' },
  { value: 'category', display: 'Product Category', icon: 'layers' },
  { value: 'document', display: 'Document', icon: 'file-text' },
  { value: 'address', display: 'Address', icon: 'database' },
  { value: 'dataset', display: 'Dataset', icon: 'database' },
];

const TYPE_BY_LABEL: Record<string, string> = {
  Customer: 'customer',
  SalesOrder: 'order',
  Product: 'product',
  ProductCategory: 'category',
  Document: 'document',
  Address: 'address',
  Dataset: 'dataset',
};

const ID_PROPERTY_BY_LABEL: Record<string, string> = {
  Customer: 'customerId',
  SalesOrder: 'salesOrderId',
  Product: 'productId',
  ProductCategory: 'categoryId',
  Document: 'documentId',
  Address: 'addressId',
  Dataset: 'name',
};

interface PositionedNode extends GraphNode {
  x: number;
  y: number;
  depth: number;
}

interface PositionedEdge {
  id: string;
  type: string;
  x1: number;
  y1: number;
  x2: number;
  y2: number;
  midX: number;
  midY: number;
}

const COLUMN_WIDTH = 220;
const ROW_HEIGHT = 86;
const MARGIN = 70;

@Component({
  selector: 'app-graph-explorer',
  standalone: true,
  imports: [CommonModule, PanelComponent, IconComponent],
  templateUrl: './graph-explorer.component.html',
  styleUrl: './graph-explorer.component.scss',
})
export class GraphExplorerComponent implements OnInit {
  readonly entityTypes = ENTITY_TYPES;
  readonly selectedType = signal('customer');
  readonly entities = signal<GraphEntityRef[]>([]);
  readonly selectedEntityId = signal<string | null>(null);
  readonly depth = signal(2);
  readonly traversal = signal<GraphTraversal | null>(null);
  readonly loadingEntities = signal(false);
  readonly loadingTraversal = signal(false);
  readonly errorMessage = signal<string | null>(null);

  constructor(private readonly graphApi: GraphApiService) {}

  ngOnInit(): void {
    this.onTypeChange(this.selectedType());
  }

  onTypeChange(type: string): void {
    this.selectedType.set(type);
    this.loadingEntities.set(true);
    this.traversal.set(null);
    this.graphApi.listEntities(type).subscribe({
      next: (entities) => {
        this.loadingEntities.set(false);
        this.entities.set(entities);
        if (entities.length > 0) {
          this.selectedEntityId.set(entities[0].id);
          this.explore(type, entities[0].id);
        } else {
          this.selectedEntityId.set(null);
          this.errorMessage.set(`No ${type} nodes in the graph yet — click Sync Graph on the overview page.`);
        }
      },
      error: () => {
        this.loadingEntities.set(false);
        this.errorMessage.set(`Could not load ${type} entities from the Spring Boot API.`);
      },
    });
  }

  onEntityChange(id: string): void {
    this.selectedEntityId.set(id);
    this.explore(this.selectedType(), id);
  }

  explore(type: string, id: string): void {
    this.loadingTraversal.set(true);
    this.graphApi.getEntity(type, id, this.depth()).subscribe({
      next: (traversal) => {
        this.loadingTraversal.set(false);
        this.traversal.set(traversal);
        this.errorMessage.set(null);
      },
      error: () => {
        this.loadingTraversal.set(false);
        this.errorMessage.set('Could not load that entity’s neighborhood from the graph.');
      },
    });
  }

  /** Clicking a node in the diagram re-centers the explorer on it. */
  onNodeClick(node: GraphNode): void {
    const type = TYPE_BY_LABEL[node.label];
    const idProperty = ID_PROPERTY_BY_LABEL[node.label];
    if (!type || !idProperty) return;
    const id = String(node.properties[idProperty]);

    if (type !== this.selectedType()) {
      this.selectedType.set(type);
      this.graphApi.listEntities(type).subscribe({
        next: (entities) => this.entities.set(entities),
      });
    }
    this.selectedEntityId.set(id);
    this.explore(type, id);
  }

  nodeTitle(node: GraphNode): string {
    const idProperty = ID_PROPERTY_BY_LABEL[node.label];
    const props = node.properties;
    if (node.label === 'Customer') {
      return (props['companyName'] as string) || `${props['firstName'] ?? ''} ${props['lastName'] ?? ''}`.trim() || String(props[idProperty]);
    }
    if (node.label === 'SalesOrder') return (props['orderNumber'] as string) ?? `Order ${props[idProperty]}`;
    return (props['name'] as string) ?? (props['filename'] as string) ?? (props['city'] as string) ?? String(props[idProperty] ?? node.id);
  }

  nodeIcon(label: string): string {
    const type = TYPE_BY_LABEL[label];
    return this.entityTypes.find((t) => t.value === type)?.icon ?? 'share';
  }

  get positionedNodes(): PositionedNode[] {
    const traversal = this.traversal();
    if (!traversal || traversal.nodes.length === 0) return [];

    const adjacency = new Map<string, string[]>();
    for (const node of traversal.nodes) adjacency.set(node.id, []);
    for (const edge of traversal.edges) {
      adjacency.get(edge.startNodeId)?.push(edge.endNodeId);
      adjacency.get(edge.endNodeId)?.push(edge.startNodeId);
    }

    const rootId = traversal.nodes[0].id;
    const depthById = new Map<string, number>([[rootId, 0]]);
    const queue = [rootId];
    while (queue.length > 0) {
      const current = queue.shift()!;
      const currentDepth = depthById.get(current)!;
      for (const next of adjacency.get(current) ?? []) {
        if (!depthById.has(next)) {
          depthById.set(next, currentDepth + 1);
          queue.push(next);
        }
      }
    }

    const columns = new Map<number, string[]>();
    for (const node of traversal.nodes) {
      const d = depthById.get(node.id) ?? 0;
      if (!columns.has(d)) columns.set(d, []);
      columns.get(d)!.push(node.id);
    }

    const positioned: PositionedNode[] = [];
    for (const d of [...columns.keys()].sort((a, b) => a - b)) {
      const ids = columns.get(d)!;
      ids.forEach((id, i) => {
        const node = traversal.nodes.find((n) => n.id === id)!;
        positioned.push({
          ...node,
          depth: d,
          x: MARGIN + d * COLUMN_WIDTH,
          y: MARGIN + i * ROW_HEIGHT,
        });
      });
    }
    return positioned;
  }

  get positionedEdges(): PositionedEdge[] {
    const traversal = this.traversal();
    if (!traversal) return [];
    const byId = new Map(this.positionedNodes.map((n) => [n.id, n]));
    return traversal.edges
      .map((edge) => {
        const start = byId.get(edge.startNodeId);
        const end = byId.get(edge.endNodeId);
        if (!start || !end) return null;
        return {
          id: edge.id,
          type: edge.type,
          x1: start.x,
          y1: start.y,
          x2: end.x,
          y2: end.y,
          midX: (start.x + end.x) / 2,
          midY: (start.y + end.y) / 2,
        };
      })
      .filter((e): e is PositionedEdge => e !== null);
  }

  get svgWidth(): number {
    const nodes = this.positionedNodes;
    if (nodes.length === 0) return 600;
    return Math.max(...nodes.map((n) => n.x)) + 140;
  }

  get svgHeight(): number {
    const nodes = this.positionedNodes;
    if (nodes.length === 0) return 300;
    return Math.max(...nodes.map((n) => n.y)) + 80;
  }
}
