export const environment = {
  // Spring Boot is the control plane (ADR-005) — Angular only ever talks to
  // this, never to Databricks directly.
  apiBaseUrl: 'http://localhost:8081/api',
};
