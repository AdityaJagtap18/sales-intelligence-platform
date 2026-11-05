import { NavGroup } from './nav.model';

/**
 * Full application navigation per master-spec §6. Routes for modules not yet
 * built in later phases still resolve — to the shared "coming soon"
 * placeholder — so the nav tree never has dead links.
 */
export const NAV_TREE: NavGroup[] = [
  {
    label: 'Overview',
    items: [
      { label: 'Executive Overview', path: '/overview/executive', icon: 'grid' },
      { label: 'Sales Dashboard', path: '/overview/sales-dashboard', icon: 'bar-chart' },
      { label: 'Reports', path: '/overview/reports', icon: 'file-text' },
    ],
  },
  {
    label: 'Analytics',
    items: [
      { label: 'Sales Explorer', path: '/analytics/sales-explorer', icon: 'compass' },
      { label: 'Customer Analytics', path: '/analytics/customers', icon: 'users' },
      { label: 'Product Analytics', path: '/analytics/products', icon: 'box' },
      { label: 'Forecasting', path: '/analytics/forecasting', icon: 'trending-up' },
      { label: 'Anomaly Detection', path: '/analytics/anomaly-detection', icon: 'alert-triangle' },
    ],
  },
  {
    label: 'Data Platform',
    items: [
      { label: 'Data Pipelines', path: '/data-platform/pipelines', icon: 'git-branch' },
      { label: 'Pipeline Runs', path: '/data-platform/pipeline-runs', icon: 'activity' },
      { label: 'Data Sources', path: '/data-platform/sources', icon: 'plug' },
      { label: 'Context Lake', path: '/data-platform/context-lake', icon: 'database' },
      { label: 'Data Catalog', path: '/data-platform/catalog', icon: 'layers' },
      { label: 'Data Lineage', path: '/data-platform/lineage', icon: 'link' },
      { label: 'Data Quality', path: '/data-platform/quality', icon: 'shield' },
    ],
  },
  {
    label: 'Intelligence',
    items: [
      { label: 'Knowledge Graph', path: '/intelligence/knowledge-graph', icon: 'share' },
      { label: 'Graph Explorer', path: '/intelligence/graph-explorer', icon: 'search' },
      { label: 'Prompt Library', path: '/intelligence/prompt-library', icon: 'message-circle' },
      { label: 'Prompt Playground', path: '/intelligence/prompt-playground', icon: 'terminal' },
      { label: 'AI Insights', path: '/intelligence/ai-insights', icon: 'lightbulb' },
    ],
  },
  {
    label: 'Observability',
    items: [
      { label: 'System Health', path: '/observability/system-health', icon: 'heart' },
      { label: 'Pipeline Monitoring', path: '/observability/pipeline-monitoring', icon: 'bar-chart-2' },
      { label: 'Logs & Traces', path: '/observability/logs-traces', icon: 'file-text' },
      { label: 'Alerts', path: '/observability/alerts', icon: 'bell' },
      { label: 'Metrics', path: '/observability/metrics', icon: 'sliders' },
    ],
  },
  {
    label: 'Administration',
    items: [
      { label: 'Settings', path: '/admin/settings', icon: 'settings' },
      { label: 'Integrations', path: '/admin/integrations', icon: 'grid-plus' },
      { label: 'Model Configuration', path: '/admin/model-configuration', icon: 'cpu' },
      { label: 'User Management', path: '/admin/users', icon: 'user' },
      { label: 'Release Notes', path: '/admin/release-notes', icon: 'file-text' },
    ],
  },
];
