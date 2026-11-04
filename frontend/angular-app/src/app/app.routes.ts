import { Routes } from '@angular/router';
import { ShellComponent } from './core/layout/shell/shell.component';
import { PlaceholderPageComponent } from './shared/components/placeholder-page/placeholder-page.component';

interface PlaceholderData {
  title: string;
  description: string;
  icon: string;
  phase: string;
}

function placeholder(data: PlaceholderData) {
  return {
    loadComponent: () => import('./shared/components/placeholder-page/placeholder-page.component').then((m) => m.PlaceholderPageComponent),
    data,
  };
}

export const routes: Routes = [
  {
    path: '',
    component: ShellComponent,
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'overview/executive' },

      {
        path: 'overview/executive',
        loadComponent: () => import('./features/overview/executive-overview.component').then((m) => m.ExecutiveOverviewComponent),
        data: { title: 'Executive Overview' },
      },
      { path: 'overview/sales-dashboard', ...placeholder({ title: 'Sales Dashboard', description: 'Interactive sales performance dashboards against real Gold-layer data.', icon: 'bar-chart', phase: 'Phase 9 — Business Intelligence' }) },
      { path: 'overview/reports', ...placeholder({ title: 'Reports', description: 'Saved and scheduled business reports.', icon: 'file-text', phase: 'Phase 9 — Business Intelligence' }) },

      { path: 'analytics/sales-explorer', ...placeholder({ title: 'Sales Explorer', description: 'Ad-hoc exploration of sales data across dimensions.', icon: 'compass', phase: 'Phase 9 — Business Intelligence' }) },
      { path: 'analytics/customers', ...placeholder({ title: 'Customer Analytics', description: 'Customer segmentation and purchasing behavior.', icon: 'users', phase: 'Phase 9 — Business Intelligence' }) },
      { path: 'analytics/products', ...placeholder({ title: 'Product Analytics', description: 'Product and category performance.', icon: 'box', phase: 'Phase 9 — Business Intelligence' }) },
      { path: 'analytics/forecasting', ...placeholder({ title: 'Forecasting', description: 'AI-assisted revenue and demand forecasting.', icon: 'trending-up', phase: 'Phase 9 — Business Intelligence' }) },
      { path: 'analytics/anomaly-detection', ...placeholder({ title: 'Anomaly Detection', description: 'Investigate detected anomalies across business metrics.', icon: 'alert-triangle', phase: 'Phase 9 — Business Intelligence' }) },

      { path: 'data-platform/pipelines', ...placeholder({ title: 'Data Pipelines', description: 'Pipeline registry and configuration.', icon: 'git-branch', phase: 'Phase 5 — Pipeline Management' }) },
      {
        path: 'data-platform/pipeline-runs',
        loadComponent: () => import('./features/data-platform/pipeline-runs.component').then((m) => m.PipelineRunsComponent),
        data: { title: 'Pipeline Runs' },
      },
      { path: 'data-platform/sources', ...placeholder({ title: 'Data Sources', description: 'Registered source systems feeding the platform.', icon: 'plug', phase: 'Phase 4 — Core Data Engineering' }) },
      {
        path: 'data-platform/context-lake',
        loadComponent: () => import('./features/data-platform/context-lake.component').then((m) => m.ContextLakeComponent),
        data: { title: 'Context Lake' },
      },
      {
        path: 'data-platform/catalog',
        loadComponent: () => import('./features/data-platform/catalog.component').then((m) => m.CatalogComponent),
        data: { title: 'Data Catalog' },
      },
      {
        path: 'data-platform/lineage',
        loadComponent: () => import('./features/data-platform/lineage.component').then((m) => m.LineageComponent),
        data: { title: 'Data Lineage' },
      },
      { path: 'data-platform/quality', ...placeholder({ title: 'Data Quality', description: 'Data quality checks and reconciliation status.', icon: 'shield', phase: 'Phase 5 — Pipeline Management' }) },

      { path: 'intelligence/knowledge-graph', ...placeholder({ title: 'Knowledge Graph', description: 'Business and technical entity relationships.', icon: 'share', phase: 'Phase 7 — Knowledge Graph' }) },
      { path: 'intelligence/graph-explorer', ...placeholder({ title: 'Graph Explorer', description: 'Interactive graph exploration and queries.', icon: 'search', phase: 'Phase 7 — Knowledge Graph' }) },
      { path: 'intelligence/prompt-library', ...placeholder({ title: 'Prompt Library', description: 'Create, version and organize reusable AI prompts.', icon: 'message-circle', phase: 'Phase 8 — Prompt Library & AI' }) },
      { path: 'intelligence/prompt-playground', ...placeholder({ title: 'Prompt Playground', description: 'Execute prompts against local Ollama models.', icon: 'terminal', phase: 'Phase 8 — Prompt Library & AI' }) },
      { path: 'intelligence/ai-insights', ...placeholder({ title: 'AI Insights', description: 'Grounded, source-cited AI analysis across the platform.', icon: 'lightbulb', phase: 'Phase 8 — Prompt Library & AI' }) },

      { path: 'observability/system-health', ...placeholder({ title: 'System Health', description: 'Infrastructure and service health overview.', icon: 'heart', phase: 'Phase 5 — Pipeline Management & Observability' }) },
      { path: 'observability/pipeline-monitoring', ...placeholder({ title: 'Pipeline Monitoring', description: 'Pipeline run status, alerts and reliability scoring.', icon: 'bar-chart-2', phase: 'Phase 5 — Pipeline Management & Observability' }) },
      { path: 'observability/logs-traces', ...placeholder({ title: 'Logs & Traces', description: 'Centralized structured logs and distributed traces.', icon: 'file-text', phase: 'Phase 5 — Pipeline Management & Observability' }) },
      { path: 'observability/alerts', ...placeholder({ title: 'Alerts', description: 'Pipeline, data quality and infrastructure alerts.', icon: 'bell', phase: 'Phase 5 — Pipeline Management & Observability' }) },
      { path: 'observability/metrics', ...placeholder({ title: 'Metrics', description: 'Platform and business metrics explorer.', icon: 'sliders', phase: 'Phase 5 — Pipeline Management & Observability' }) },

      { path: 'admin/settings', ...placeholder({ title: 'Settings', description: 'Application configuration.', icon: 'settings', phase: 'Ongoing' }) },
      { path: 'admin/integrations', ...placeholder({ title: 'Integrations', description: 'Connected sources and external services.', icon: 'grid-plus', phase: 'Ongoing' }) },
      { path: 'admin/model-configuration', ...placeholder({ title: 'Model Configuration', description: 'Configure local LLM and embedding models.', icon: 'cpu', phase: 'Phase 8 — Prompt Library & AI' }) },
      { path: 'admin/users', ...placeholder({ title: 'User Management', description: 'Users, roles and access control.', icon: 'user', phase: 'Phase 10 — Production Validation' }) },

      { path: '**', redirectTo: 'overview/executive' },
    ],
  },
];
