# ADR-002: Neo4j as the Knowledge Graph store

## Status
Accepted

## Context
Module E requires modeling business entities (customers, orders, products, territories) and technical entities (datasets, pipelines, documents, prompts) plus their relationships, with interactive exploration and provenance tracking (source fact vs. inferred relationship, per Image 3's solid/dotted edge convention).

## Decision
Use Neo4j as a dedicated graph database rather than modeling relationships as foreign keys in PostgreSQL or as an in-memory graph computed on demand.

## Consequences
- Relationship/traversal queries (e.g., "all datasets derived from this pipeline, three hops out") are native Cypher queries instead of recursive SQL CTEs.
- Adds one more service to the Compose stack (justified: graph traversal is a named, central module, not incidental).
- Sync direction is one-way (Postgres/MinIO facts → Neo4j) to avoid dual-write consistency problems; Neo4j is never the system of record for data that also lives in Postgres.
- Cytoscape.js (or equivalent) renders the graph client-side from data the Spring Boot API fetches from Neo4j — no direct browser-to-Neo4j connection.
