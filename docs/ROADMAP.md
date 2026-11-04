# Roadmap and status

Status reflects what is in this repository, not intent.

| Phase                   | Status                                                                                                                                                                               |
| ----------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| 1. Foundation           | Done. Tokens bound to the Rook brand kit; contrast measured on 8-bit colour as browsers paint it                                                                                     |
| 2. Component engine     | Done: Toast, Table, Combobox, Breadcrumb, Pagination, Progress, Link and the rest of the catalogue in the docs site                                                                  |
| 3. Experience patterns  | Done: PageHeader, Toolbar, FilterBar, SettingsSection/Row, DescriptionList, SplitLayout, StatCard, ErrorState, LoadingState ([patterns.md](guides/patterns.md))                      |
| 4. Governance           | Conformance engine, product registry, reusable `conformance.yml`, policies                                                                                                           |
| 5. Documentation        | Guides, ADRs, `AGENTS.md`, `llms.txt`, generated manifest, docs site in `apps/docs`                                                                                                  |
| 6. Production hardening | Lint, strict types, dependency boundaries, unit/interaction/SSR tests, axe route checks in three browsers, visual baselines in a pinned Playwright container, secret scanning, audit |
| 7. Release              | Published to npm as `@rookcom/*` under the MIT license; CI publishes with trusted publishing and provenance                                                                          |

## Components

The full, current list is generated into
[COMPONENT-GAP-ANALYSIS](architecture/COMPONENT-GAP-ANALYSIS.md) and the docs
site; the build order is in
[COMPONENT-IMPLEMENTATION-ROADMAP](roadmap/COMPONENT-IMPLEMENTATION-ROADMAP.md).

## Next

1. Finish the remaining roadmap waves, one family per branch.
2. Keep every Rook application on the current minor release, one app per PR,
   each with a conformance baseline wired into its CI.
3. Broaden data-grid and date-picker coverage as demand appears.
