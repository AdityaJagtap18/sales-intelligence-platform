# Reference Analysis

Five images were supplied. Image 1 is the sole authority for visual identity (theme, color, typography, chrome, components). Images 2–5 are functional/layout references only — their *information architecture* is reused, their *skin* is discarded and replaced with Image 1's design tokens.

## Image 1 — NEXORA AI Dashboard (PRIMARY, visual authority)

Dark enterprise analytics shell. This is the design system source:

- **Background layers**: near-black navy app background, slightly lighter navy sidebar/topbar, dark slate (`#1a1f2e`-ish) cards with a 1px subtle border — three distinct surface levels, no gradients.
- **Accent**: single restrained orange (`~#E8733F`/`#F0803C` range) for active nav indicator, primary buttons, highlight badges ("High Impact"), chart primary series, progress bars. No secondary accent colors beyond semantic green (success/up), red (error/down), amber (warning).
- **Typography**: one sans-serif family throughout (Inter or equivalent), tight letter-spacing, clear size hierarchy: large KPI numbers, medium card titles, small muted metadata/timestamps.
- **Top bar**: product logo + name, global "Ask AI / search metrics" field with `⌘K` hint, notification bell, help, user avatar + plan badge.
- **Sidebar**: collapsible, grouped sections with uppercase muted group labels (ANALYTICS, AI & INSIGHTS, DATA MANAGEMENT, CONFIGURATION), minimal line icons, orange active-state pill.
- **Cards**: KPI cards (icon badge, value, sparkline, delta vs. prior period); the "AI Insight" card is a distinct bordered callout with a confidence bar and an impact badge — this exact pattern is the template for our AI Insights module everywhere in the app.
- **Charts**: line/area chart with an "expected vs actual" dashed comparison line and an anomaly highlight band + marker — directly informs the Anomaly Detection module. Horizontal bar chart (Revenue by Channel) and donut chart (Users by Segment) use the same orange-led palette with low-saturation secondary colors only inside the donut.
- **Tables**: borderless row dividers, colored status chips (High/Medium/Low impact, confidence % pill colored by threshold).

**Token mapping → `docs/architecture` design tokens (to be built in Phase 2):** background/navy, surface/slate, border/subtle, accent/orange (primary, hover, active), text/primary (white), text/muted (gray), status/success, status/error, status/warning, status/info.

## Image 2 — Data Catalog (light theme) — functional reference for **Data Catalog / Context Lake**

Reused concepts (re-skinned to Image 1's dark palette, not its light one):
- Left filter rail: Asset type (Tables & Views, Dashboards, Pipelines, ML models, Workbook), Sources (per-connector counts), Quality status (Urgent/High risk/No incidents/Not monitored), Owners — maps directly onto our Dataset Management filters (layer, source system, owner, classification).
- Asset list cards: type icon + badge, last-run status (success/failure/running), one-line description, incident severity pill, tag/term/domain counts, source-system badge, "data usage" indicator, owner avatars. This becomes our Context Lake dataset card layout.
- Top-right "Auto coverage" action and "Metadata refresh" control — informs a "Sync metadata" action on the Context Lake toolbar.
- Applies to: **Module D (Context Lake)** dataset browser and metadata catalog screens.

## Image 3 — Enterprise Knowledge Graph + AI Assistant/Memories panel — functional reference for **Knowledge Graph** and **Context Lake ingestion**

Reused concepts:
- Force-directed graph with color-coded domains (Finance, HR, Operations, Policies, Customers, Products, People) as larger hub nodes with smaller typed leaf nodes (document, metric, record icons) connected by solid (direct) vs. dotted (cross-domain/inferred) edges — this dotted-vs-solid distinction is exactly the "source facts vs. inferred relationships" rule the spec requires, and we carry it forward literally.
- Top filter bar: domain filter, type filter, "Hide isolated" toggle — becomes our Graph Explorer filter bar.
- Left rail grouping ingestion primitives (Connector, Document Loader, Document Processing, Embedding, Data Processing, Memory, Retriever, Vector Store, Graph Store) — this is a near 1:1 map to our Context Lake + Knowledge Graph pipeline: Connector→Data Sources, Document Loader/Processing→Document Context ingestion, Embedding/Vector Store→Qdrant, Graph Store→Neo4j, Retriever→semantic search, Memory→out of scope for MVP (no agent memory requirement in spec).
- Right-side "Assistant" chat panel + "Memories" panel — informs **Module G (AI Insights)** as a docked side panel pattern rather than a full-page chatbot, consistent with the spec's "avoid building an unrestricted AI chatbot as the default interface."
- Applies to: **Module E (Knowledge Graph)** primarily; its ingestion sidebar also shapes **Module D**'s "register data source" flow.

## Image 4 — AI Prompt Manager (dark theme) — functional reference for **Prompt Library**

Reused concepts almost directly (already dark, closest to Image 1 of the secondary set, but still re-themed to our exact tokens, not its own purple/green accents):
- Left rail: "All Prompts" / "Untagged" plus a nested, collapsible tag tree with counts (assistant▸researcher/reviewer, coding▸artefact/orchestrator/planning/practice/role, productOwner▸Simulation, projectManagement▸improve/risks, roles▸team) — becomes our category/tag navigation.
- Toolbar: search, sort (Created Date), Tags manager, Import, Export, "+ New Prompt" — all carried into our Prompt Library toolbar as-is functionally.
- Prompt card: title + filename-style subtitle, version chip (v1/v2, with a small history icon when >1 version), short description, colored tag pills, usage stats ("Used: N times", "Last Used", "Created"), and a 4-icon action row (preview/run, duplicate, edit, delete) — this is the direct template for our Prompt Library grid and for Module F's versioning/usage metadata requirements.
- Applies to: **Module F (Prompt Library)** card grid, tag tree, and toolbar; the version chip pattern also informs how we surface prompt version history.

## Image 5 — Observability mockup (light theme) — functional reference for **Observability Center**

Reused concepts:
- Two donut KPI tiles side by side (Pipeline run status: Success/Failed; Alert severity: Critical/High/Medium/Low) — becomes our Observability Center's top summary row.
- Pipeline table: name + owning team, "Alerts" shown as "open/total" fraction colored by severity, "Total (Last Run)" with recency/running state, and a per-row micro bar-chart sparkline of recent runs (colored per run outcome) — this is the direct template for our Pipeline Monitoring table.
- Floating "Data Reliability Score" card with a single aggregate percentage plus a stacked bar chart broken into Data Quality / Reconciliation / Data Drift / Schema Drift — informs a composite reliability-score widget for the Data Quality Monitoring screen.
- Applies to: **Module C (Observability Center)**, specifically Pipeline Monitoring and Data Quality Monitoring views.

## Module → reference map (summary)

| Module | Primary layout/functional reference | Visual skin |
|---|---|---|
| Executive Overview | Image 1 (native) | Image 1 |
| Data Pipeline Management | Image 5 (table/run-history pattern) | Image 1 |
| Observability Center | Image 5 (donuts, reliability score, run table) | Image 1 |
| Context Lake | Image 2 (catalog filters/cards) + Image 3 (ingestion sidebar) | Image 1 |
| Knowledge Graph | Image 3 (graph, domain colors, filter bar, assistant panel) | Image 1 |
| Prompt Library | Image 4 (card grid, tag tree, toolbar, versioning) | Image 1 |
| AI Insights | Image 1 (AI Insight card) + Image 3 (docked assistant panel) | Image 1 |

## Conflict rule applied

Wherever a secondary image's own colors/typography/component chrome would conflict with Image 1 (e.g., Image 2's and Image 5's light backgrounds, Image 3's purple/teal/green node palette, Image 4's purple/green accent buttons), Image 1 wins outright — only the *information structure* (what's grouped with what, what data a card/table/filter shows) is carried over, never the secondary image's own color or type choices.
