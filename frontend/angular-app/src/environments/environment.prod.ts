export const environment = {
  // Two separate subdomains in production (sellixai.adityajagtap.com for the
  // SPA, sellixai-api.adityajagtap.com for the API) — not path-based on one
  // origin, so this has to be absolute, and the backend's CORS_ALLOWED_ORIGINS
  // must include the frontend's origin (see docker-compose.prod.yml).
  apiBaseUrl: 'https://sellixai-api.adityajagtap.com/api',
};
