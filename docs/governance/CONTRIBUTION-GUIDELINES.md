# Contribution guidelines: components

How to propose, build, test and document a Monarch component in this
repository. Start with [CONTRIBUTING](../../CONTRIBUTING.md) for setup and
releases; this page is the component path. Rules for AI agents are in
[AGENTS.md](../../AGENTS.md) and apply to people too.

## 1. Propose

1. Check [`registry/components.json`](../../registry/components.json) and the
   [gap analysis](../architecture/COMPONENT-GAP-ANALYSIS.md). If the component
   is already planned, work in its roadmap wave instead of proposing it again.
2. Open a **Component or pattern proposal** issue. Describe who needs it and
   the interaction it covers (links to public examples help), the proposed API,
   and the accessibility model (role, keyboard, accessible name).
3. A maintainer records the demand in
   [`registry/component-demand.json`](../../registry/component-demand.json):
   how many products need it, a priority, the nearest existing Monarch
   component and the capabilities Monarch lacks. Use generic capability names;
   the file holds no product source references.
4. Run `npm run registry:components && npm run docs:generate`.
   The component is eligible when the registry shows `scope: in-scope`
   (3+ products, or 2+ at P0; see
   [governance](COMPONENT-GOVERNANCE.md#promotion-rule)).
5. A maintainer assigns it a wave, package and entry point in
   [`registry/roadmap-waves.json`](../../registry/roadmap-waves.json),
   following [PACKAGE-ARCHITECTURE](../architecture/PACKAGE-ARCHITECTURE.md).

Do not propose a component for one product's business workflow. Keep it in
the product, built from Monarch parts.

## 2. Build

Follow the [component contract](../guides/component-contract.md) and the
[source layout](../architecture/SOURCE-LAYOUT.md). In short:

- Start with `npm run new:component -- --id <registry-id> --family <family>`.
  It creates `packages/react/src/components/<family>/<id>/` (patterns under
  `src/patterns/`) with `<id>.tsx`, `<id>.css`, `<id>.test.tsx`,
  `<id>.docs.tsx` and `index.ts`, and wires the entry export, the CSS order
  and `EXPORT_GROUPS`. See [adding a component](../architecture/SOURCE-LAYOUT.md#adding-a-component).
- Styles use only `--mn-*` variables; the build rejects raw colours and any
  stylesheet missing from `src/styles/order.json`. Shared helpers go in
  `src/internal`; other components are imported only through their
  `index.ts` (`npm run deps:check`).
- Typed props extending the native element; variants as string unions;
  required accessible names enforced by the type (`label` on icon-only
  controls).
- One `mn-<id>` root class and `data-*` state attributes. Both are public
  API.
- No business logic, no data fetching, no product names in generic
  components (see [taxonomy naming rules](../architecture/COMPONENT-TAXONOMY.md#naming-rules)).
- SSR-safe: no browser globals at module scope; `'use client'` only where
  state or Radix needs it.
- Export from the package entry (`src/index.ts`, or the subpath entry for
  `/data`, `/charts`, and so on) and list every export name in
  `EXPORT_GROUPS` in
  [`scripts/registry/build-components.mjs`](../../scripts/registry/build-components.mjs);
  the scaffold adds the first, and the registry build fails until the rest are there.
- No sparkle icons (`Sparkles`, `Sparkle`, `WandSparkles`, `Wand2`). For AI
  features use the lucide `Bot` icon; `icons/no-sparkles` enforces it.

## 3. Test

| What                                      | Where                                                                                     | Command               |
| ----------------------------------------- | ----------------------------------------------------------------------------------------- | --------------------- |
| Interaction, keyboard, focus (user-event) | `<id>/<id>.test.tsx`, next to the component                                               | `npm test`            |
| Accessible names and roles (axe in jsdom) | same file, `expectNoA11yViolations`                                                       | `npm test`            |
| Server rendering                          | same file, `renderToString`                                                               | `npm test`            |
| Public API snapshot                       | `packages/react/test/api.test.ts`                                                         | `npm test`            |
| Axe in Chromium, Firefox, WebKit          | `apps/docs/tests/a11y.spec.ts` (every component page, automatically)                      | `npm run test:a11y`   |
| Visual baselines (Linux, 4 per component) | `apps/docs/tests/visual.spec.ts` (ids in `GALLERY`, `apps/docs/src/pages/gallery-ids.ts`) | `npm run test:visual` |

Visual baselines are generated in CI (run the CI workflow manually with
`update_snapshots: true`, download the artifact, commit it), so they match
the pinned Linux container. See [docs-site guide](../guides/docs-site.md).

The browser tests start their own docs preview on port 4173. When several
worktrees run tests on one machine, give each its own port
(`MONARCH_DOCS_PORT=4301 npm run test:visual`); a running server is reused
only with `MONARCH_REUSE_SERVER=1`.

## 4. Document

1. Write `<id>.docs.tsx` to the
   [docs-file contract](../architecture/SOURCE-LAYOUT.md#docs-file-contract)
   (`ComponentDocs`): all 16 sections of
   [governance](COMPONENT-GOVERNANCE.md#review-gates), with live examples. The
   docs site renders it at `#/components/<id>`. API and props tables come from
   the manifest, which the build generates from your TypeScript doc comments:
   the comment on a prop is its documentation.
2. `npm run components:check` fails on a missing section, a leftover TODO,
   a dependency list that does not match the imports, or a testing claim the
   test does not back. New components may not mark sections
   `legacy-incomplete`.
3. Examples use realistic but neutral content. Never invent product
   statistics, pricing or copy.
4. Run `npm run registry:components` so the registry records the docs route.

## 5. Open the PR

- `npm run verify` passes locally.
- `npm run changeset` for any change under `packages/*/src`. Breaking changes
  need an ADR in `docs/adr`, a note in `docs/guides/migration.md`, and the
  [deprecation policy](DEPRECATION-POLICY.md) first.
- Commit the regenerated `registry/components.json` and docs tables; never
  hand-edit them.
- One concern per PR. Fill in the pull request template.
