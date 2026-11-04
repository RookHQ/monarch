# Monarch documentation

Index of the documents in `docs/`. Tables marked _generated_ are produced by
`npm run docs:generate` from the registry; edit the inputs, not the tables.

## Architecture

| Document                                                                        | What it answers                                                                           |
| ------------------------------------------------------------------------------- | ----------------------------------------------------------------------------------------- |
| [COMPONENT-SELECTION](architecture/COMPONENT-SELECTION.md)                      | Who Monarch serves and how a component earns a place in the library (_generated_ counts)  |
| [COMPONENT-TAXONOMY](architecture/COMPONENT-TAXONOMY.md)                        | The 22 categories, the 5 layers, naming rules, where each in-scope component sits         |
| [COMPONENT-GAP-ANALYSIS](architecture/COMPONENT-GAP-ANALYSIS.md)                | Every in-scope component against today's exports (_generated_)                            |
| [COMPATIBILITY](architecture/COMPATIBILITY.md)                                  | Supported package versions, peer dependencies and browsers                                |
| [PACKAGE-ARCHITECTURE](architecture/PACKAGE-ARCHITECTURE.md)                    | Packages, subpath entry points, dependency direction                                      |
| [SOURCE-LAYOUT](architecture/SOURCE-LAYOUT.md)                                  | Component folders, required files, CSS order, docs-file contract, `npm run new:component` |
| [COMPONENT-IMPLEMENTATION-ROADMAP](roadmap/COMPONENT-IMPLEMENTATION-ROADMAP.md) | Build order in nine waves                                                                 |

The component registry is [`registry/components.json`](../registry/components.json)
([schema](../registry/components.schema.json)), generated from
[`registry/component-demand.json`](../registry/component-demand.json) and the
package manifest.

## Governance

| Document                                                         | What it covers                                                     |
| ---------------------------------------------------------------- | ------------------------------------------------------------------ |
| [COMPONENT-GOVERNANCE](governance/COMPONENT-GOVERNANCE.md)       | Ownership, the promotion rule, lifecycle, review gates, versioning |
| [CONTRIBUTION-GUIDELINES](governance/CONTRIBUTION-GUIDELINES.md) | Proposing, building, testing and documenting a component           |
| [DEPRECATION-POLICY](governance/DEPRECATION-POLICY.md)           | Deprecation stages, timelines, codemods, the conformance rule      |
| [versioning](governance/versioning.md)                           | Semver rules, release channels, compatibility matrix               |
| [deprecation](governance/deprecation.md)                         | The short form of the deprecation policy                           |
| [exceptions](governance/exceptions.md)                           | Time-limited, owned deviations from conformance rules              |

## Guides, decisions, status

- Guides: [getting started](guides/getting-started.md), [theming](guides/theming.md),
  [migration](guides/migration.md), [component contract](guides/component-contract.md),
  [patterns](guides/patterns.md), [conformance](guides/conformance.md),
  [brand](guides/brand.md), [docs site](guides/docs-site.md).
- Decisions: [adr/](adr).
- Status: [ROADMAP](ROADMAP.md).
