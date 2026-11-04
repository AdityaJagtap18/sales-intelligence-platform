-- Runs once, on first container init (empty data volume) only.
-- No application tables yet — those are created by Spring Boot/Flyway
-- starting Phase 4+, once there's a real schema to version. This just
-- ensures the extension used for generated UUID primary keys is present
-- before anything else tries to depend on it.
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
