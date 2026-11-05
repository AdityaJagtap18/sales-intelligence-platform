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
      {
        path: 'overview/sales-dashboard',
        loadComponent: () => import('./features/overview/sales-dashboard.component').then((m) => m.SalesDashboardComponent),
        data: { title: 'Sales Dashboard' },
      },
      {
        path: 'overview/reports',
        loadComponent: () => import('./features/overview/reports.component').then((m) => m.ReportsComponent),
        data: { title: 'Reports' },
      },

      {
        path: 'analytics/sales-explorer',
        loadComponent: () => import('./features/analytics/sales-explorer.component').then((m) => m.SalesExplorerComponent),
        data: { title: 'Sales Explorer' },
      },
      {
        path: 'analytics/customers',
        loadComponent: () => import('./features/analytics/customer-analytics.component').then((m) => m.CustomerAnalyticsComponent),
        data: { title: 'Customer Analytics' },
      },
      {
        path: 'analytics/products',
        loadComponent: () => import('./features/analytics/product-analytics.component').then((m) => m.ProductAnalyticsComponent),
        data: { title: 'Product Analytics' },
      },
      {
        path: 'analytics/forecasting',
        loadComponent: () => import('./features/analytics/forecasting.component').then((m) => m.ForecastingComponent),
        data: { title: 'Forecasting' },
      },
      {
        path: 'analytics/anomaly-detection',
        loadComponent: () => import('./features/analytics/anomaly-detection.component').then((m) => m.AnomalyDetectionComponent),
        data: { title: 'Anomaly Detection' },
      },

      {
        path: 'data-platform/pipelines',
        loadComponent: () => import('./features/data-platform/data-pipelines.component').then((m) => m.DataPipelinesComponent),
        data: { title: 'Data Pipelines' },
      },
      {
        path: 'data-platform/pipeline-runs',
        loadComponent: () => import('./features/data-platform/pipeline-runs.component').then((m) => m.PipelineRunsComponent),
        data: { title: 'Pipeline Runs' },
      },
      {
        path: 'data-platform/sources',
        loadComponent: () => import('./features/data-platform/data-sources.component').then((m) => m.DataSourcesComponent),
        data: { title: 'Data Sources' },
      },
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
      {
        path: 'data-platform/quality',
        loadComponent: () => import('./features/data-platform/data-quality.component').then((m) => m.DataQualityComponent),
        data: { title: 'Data Quality' },
      },

      {
        path: 'intelligence/knowledge-graph',
        loadComponent: () => import('./features/intelligence/knowledge-graph.component').then((m) => m.KnowledgeGraphComponent),
        data: { title: 'Knowledge Graph' },
      },
      {
        path: 'intelligence/graph-explorer',
        loadComponent: () => import('./features/intelligence/graph-explorer.component').then((m) => m.GraphExplorerComponent),
        data: { title: 'Graph Explorer' },
      },
      {
        path: 'intelligence/prompt-library',
        loadComponent: () => import('./features/intelligence/prompt-library.component').then((m) => m.PromptLibraryComponent),
        data: { title: 'Prompt Library' },
      },
      {
        path: 'intelligence/prompt-playground',
        loadComponent: () => import('./features/intelligence/prompt-playground.component').then((m) => m.PromptPlaygroundComponent),
        data: { title: 'Prompt Playground' },
      },
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
