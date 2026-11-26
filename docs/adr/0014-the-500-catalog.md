# ADR 0014: The 500-component catalog is strategic

Status: accepted (2026-11-07), waves 16–19, under [ADR 0011](0011-strategic-scope.md)

## Context

The AI-era directive commits Monarch to 500 complete components: 121 that
existed, 119 promoted from local copies in Rook apps, 175 new and 85 AI. Every
Rook frontend still ships its own `components/ui` folder; the goal is that
each app can delete it. Most of the 274 remaining non-AI catalog components
have no demand record, because each app built its own copy instead of asking
for a shared one.

## Decision

Every catalog component is in strategic scope (`registry/strategic.json`),
grouped into families by wave:

| Wave | Family                                                                                                                  |
| ---- | ----------------------------------------------------------------------------------------------------------------------- |
| 16   | Cross-app promotions: components Rook apps already built locally, generalised from the best copy; and the dashboard kit |
| 17   | Catalog core: actions, forms, overlays, feedback, data display, navigation, layout, typography, interaction             |
| 18   | Catalog domain: workflow, scheduling, data grid, charts, collaboration, calls, content, files                           |
| 19   | Catalog business: identity, security, billing, developer, geo, search, marketing, page templates                        |

Entry points keep the root entry (and `styles/core.css`) for generic
primitives only. Domain components go to subpaths, so an app that does not
use them neither bundles their code nor loads their CSS:

| Entry             | Families                                                                                                            |
| ----------------- | ------------------------------------------------------------------------------------------------------------------- |
| `.`               | actions, forms and inputs, overlays, feedback, data display, navigation, layout, typography, interaction            |
| `./data`          | data grid and records, search and filter, files, geo                                                                |
| `./charts`        | charts and visualisation                                                                                            |
| `./scheduling`    | scheduling and time                                                                                                 |
| `./collaboration` | collaboration and messaging, calls and media                                                                        |
| `./editor`        | content and editor blocks                                                                                           |
| `./patterns`      | workflow and planning, billing, dashboard kit, identity and people, security and account, developer, page templates |
| `./marketing`     | marketing and public pages                                                                                          |
| `./shell`         | Rook shell parts (rook bar, docs panel, notifications bell, availability)                                           |

A promoted component is built by reading every app copy listed in the
directive, generalising the best one to the component contract, and then
deleting the copies during the app migrations (Part VII).

`use-confirm` is already shipped by the `confirm-dialog` folder
(`useConfirm`), so it is not a separate folder.

## Consequences

- The registry shows each catalog component as `planned` until its folder
  ships, so the "N / 500 complete" count is measured, not declared.
- Promotions change app behaviour only when the apps migrate; until then the
  app copies keep working.
