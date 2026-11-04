# Component governance

How a component gets into Monarch, who decides, what it must pass, and how it
changes. Applies to `@rookcom/monarch-react`, including its `./shell` and
`./editor` (planned) entries; tokens follow the same gates plus the contrast
tests.

## Ownership

- **Monarch maintainers** (the repository owners listed on GitHub) own every
  public component, token, registry file and governance document. Every change
  needs their review.
- **Product teams own product logic.** Data fetching, permissions, workflow
  rules and validation stay in the product. A component that needs them takes
  them as props or callbacks.
- **Product extensions stay in the product** until promoted (below). The
  owning product team maintains them and must build them only from Monarch
  foundations: tokens, primitives and components. They never introduce their
  own visual system.
- **Measured facts belong to tools.** Registry status files (`registry/status/*.json`, written by
  `monarch registry verify` in projects that keep their own registry) are
  never edited by hand; `registry/components.json` only by
  `npm run registry:components`. Neither is edited by hand
  ([ADR 0006](../adr/0006-registry-declared-vs-measured.md)).

## Promotion rule

A component enters the Monarch catalogue when **3 or more Rook products need
it, or 2 or more need it at P0** (P0 = its absence blocks adoption). Need is
shown by evidence: the interaction implemented separately in several products,
recorded by maintainers as counts in
[`registry/component-demand.json`](../../registry/component-demand.json).
Opinion and example lists are not evidence.

A component below the rule is recorded, not built. It is revisited when a
new product's evidence pushes it over the line; the
[gap analysis](../architecture/COMPONENT-GAP-ANALYSIS.md#roadmap-components-below-the-scope-rule)
lists the ones named in the roadmap.

Exceptions to the rule need a maintainer decision recorded in the roadmap
with the reason (for example, a part that an in-scope family cannot ship
without, such as the chart frame).

## Lifecycle

| Status       | Meaning                                                                       | In the registry                    |
| ------------ | ----------------------------------------------------------------------------- | ---------------------------------- |
| `planned`    | In scope and assigned to a roadmap wave; not exported                         | `status: planned`, `wave` 1–9      |
| `beta`       | Exported, gates passed, API may still change in a minor release               | reserved; no component is beta yet |
| `stable`     | Exported; API changes follow semver                                           | `status: stable`                   |
| `deprecated` | Still exported, listed in `registry/deprecations.json`, scheduled for removal | `status: deprecated`               |

The registry builder has no beta input yet. Add one (an explicit list in
`scripts/registry/build-components.mjs`, plus a `@beta` doc tag) in the same
PR as the first beta component.

## Review gates

A component is not exported, and a status does not move to `stable`, until
all of these pass in CI:

1. **`npm run verify`**: formatting, lint, build, strict typecheck,
   dependency boundaries, unit/interaction/SSR tests, registry validation, and
   the generated-file checks (`registry:components:check`,
   `docs:generate:check`).
2. **Accessibility in three browsers**: `npm run test:a11y` runs axe against
   the docs-site route for the component in Chromium, Firefox and WebKit.
   Zero violations; no rule disabled without a written reason in the test.
3. **Keyboard and focus** behaviour covered by interaction tests
   (user-event), per the [component contract](../guides/component-contract.md).
4. **Visual baselines**: `npm run test:visual` screenshots the component in
   every theme and the reference product accents at desktop and phone width,
   in the pinned Playwright container. New baselines come from the CI
   workflow's `update_snapshots` run, never from a laptop.
5. **API snapshot** (`packages/react/test/api.test.ts`) updated deliberately,
   with a changeset.
6. **Documentation page** with all 16 sections the docs-file contract requires:
   purpose; design principles; API reference; typed properties; variants;
   states; interaction behaviour; keyboard support; accessibility; responsive
   behaviour; theme support; usage examples; do and don't; related
   components; dependencies; testing coverage. The props table is generated
   from the TypeScript declarations through the manifest, not written by hand.
7. **Conformance**: Monarch checks itself (`monarch check --root
packages/react`) and the docs site (`--root apps/docs`) with no new
   findings.

Today's docs site has one page per family with generated props and
examples; it does not yet carry all 16 sections for any component. Existing
components are brought up to the 16-section page as their family is touched
in its roadmap wave.

## Versioning

[versioning.md](versioning.md) is the policy: semver per package via
Changesets; removing or renaming an export, prop, variant, `mn-*` class,
`data-*` attribute or `--mn-*` variable is breaking and needs an ADR, a
deprecation period and a migration note. Breaking changes follow the
[deprecation policy](DEPRECATION-POLICY.md). Apps pin exact versions and
upgrade by PR.

Moving an existing export to a different entry point (for example from the
root to `/data`) is breaking. The roadmap therefore keeps current exports at
their entry point and adds new components to the new subpaths.

## Registry

[`registry/components.json`](../../registry/components.json) is the single
machine-readable list of components: id, category, layer, package and entry,
exports, status, scope, products, priority, wave, gap, missing capabilities,
docs route and accessibility notes. It validates against
[`components.schema.json`](../../registry/components.schema.json). The
builder fails when a new export is not assigned to an entry, or when an
in-scope component that is not complete has no wave, so the registry cannot
drift from the code.
