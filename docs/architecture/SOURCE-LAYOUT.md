# Source layout of `@rookcom/monarch-react` (Monarch 0.4)

Every component lives in its own folder with its stylesheet, tests and
documentation. Folders are grouped by **layer** (components or patterns) and
**family** (the taxonomy category). The layout is enforced by
`npm run verify`; this page is the contract that check implements.

Related: [PACKAGE-ARCHITECTURE](PACKAGE-ARCHITECTURE.md) (packages and entry
points), [COMPONENT-TAXONOMY](COMPONENT-TAXONOMY.md) (categories, layers,
naming), [CONTRIBUTION-GUIDELINES](../governance/CONTRIBUTION-GUIDELINES.md)
(the path from proposal to merge).

## Tree

```
packages/react/
  src/
    index.ts                      root entry "."; re-exports folder indexes only
    entries/<name>.ts             future subpath entries (./data, ./charts, ...); none yet
    internal/                     shared helpers: cx, icons; docs-only files below
      shell/                      parts the ./shell components share (see "The ./shell entry")
      component-docs.ts           the docs-file contract (excluded from the build)
      docs-demo.tsx               demo icons for docs examples (excluded from the build)
    styles/
      order.json                  cascade order of every stylesheet (see "CSS order")
      foundation.css              global foundation: focus ring, tap highlight, keyframes, .mn-visually-hidden
      menu.css  choice.css        styles shared by several components (.mn-menu, .mn-choice)
    components/<family>/<id>/     primitives and components
    patterns/<family>/<id>/       patterns (templates go here until they need their own root)
  test/
    api.test.ts                   public API snapshot (package-wide tests only)
    a11y.ts  setup.ts             test helpers
  scripts/
    build-css.ts                  dist/styles.css from styles/order.json
    manifest.ts                   dist/manifest.json from the TypeScript declarations
    check-components.ts           npm run components:check
    new-component.ts              npm run new:component
    component-folders.ts          the layout model all of the above share
```

Today's folders (top three levels):

```
components/
  actions/        button
  data-display/   badge  card  description-list  progress-bar  stat-card
  data-grid/      data-table
  feedback/       alert  empty-state  error-page  error-state  inline-error-retry
                  loading-state  skeleton  spinner  toast
  forms/          checkbox  combobox  form-field  select  switch  text-field
  identity/       avatar
  layout/         separator  visually-hidden
  navigation/     breadcrumb  link  pagination  tabs
  overlays/       dialog  dropdown-menu  popover  tooltip
  typography/     kbd
patterns/
  actions/        toolbar
  layout/         page-header  settings-section  split-layout
  search/         filter-bar
```

## Naming

| Part       | Rule                                                                                                                                                                                                                                      |
| ---------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `<id>`     | The registry id from [`registry/components.json`](../../registry/components.json), kebab-case. The folder name is the merge key with the registry, so `Field` lives in `form-field/`, `Input` in `text-field/`, `Table` in `data-table/`. |
| `<family>` | The registry category without its number: `03-actions` → `actions`. One of the 22 [taxonomy categories](COMPONENT-TAXONOMY.md#categories).                                                                                                |
| root       | Registry layer `primitive` or `component` → `src/components`; `pattern` or `template` → `src/patterns`.                                                                                                                                   |
| exports    | PascalCase of the id (`status-badge` → `StatusBadge`), parts prefixed with the parent (`TabsList`), per the [taxonomy naming rules](COMPONENT-TAXONOMY.md#naming-rules).                                                                  |
| CSS        | One `mn-<id>` root class, parts as `mn-<id>__part`, state in `data-*` attributes; both are public API.                                                                                                                                    |

Legacy folders keep a file that exports more than one registry entry rather
than splitting it, because the manifest's `className` is read from the file
and must not change: `filter-bar/` also exports `FilterChip` (`filter-chips`).
New components get one folder per registry id. (`LoadingState` used to live in
`error-state/`, which gave it the wrong manifest `className`; 0.4 moved it to
`feedback/loading-state/` with the same export, props and class names.)

## Required files

| File              | Required                                                    | Contents                                                                                                                                 |
| ----------------- | ----------------------------------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------- |
| `<id>.tsx`        | yes                                                         | The component. Typed props extending the native element, `forwardRef`, `'use client'` only if it needs state, effects or Radix.          |
| `<id>.css`        | yes, unless listed in `NO_OWN_CSS` (`component-folders.ts`) | Only `--mn-*` variables (the build rejects raw colours); listed in `styles/order.json`.                                                  |
| `<id>.test.tsx`   | yes                                                         | Behaviour, keyboard and focus (user-event), axe (`expectNoA11yViolations` from `test/a11y.ts`), and server rendering (`renderToString`). |
| `<id>.docs.tsx`   | yes                                                         | The documentation page, typed by the [docs-file contract](#docs-file-contract).                                                          |
| `index.ts`        | yes                                                         | `export * from './<id>.js';` — the folder's only public door.                                                                            |
| `<id>.<part>.tsx` | optional                                                    | Extra source files for a large component (`data-grid.column.tsx`). No sub-folders.                                                       |

Exempt from their own CSS: `visually-hidden` (`.mn-visually-hidden` is a
foundation utility apps use directly) and `dropdown-menu` (`.mn-menu` is shared
with Select and Combobox in `styles/menu.css`).

Tests and docs files are compiled by `typecheck` but excluded from the
published build (`tsconfig.build.json`), so `dist` contains only runtime code.

## Import rules

Enforced by [`.dependency-cruiser.cjs`](../../.dependency-cruiser.cjs)
(`npm run deps:check`):

| Rule                                    | A file in …                        | may import                                                                                      |
| --------------------------------------- | ---------------------------------- | ----------------------------------------------------------------------------------------------- |
| `component-folder-boundary`             | `src/components/<family>/<id>/`    | its own folder, `src/internal`, other components' `index.ts` only (never a deep file), npm deps |
| `pattern-folder-boundary`               | `src/patterns/<family>/<id>/`      | the same, plus other patterns' `index.ts`                                                       |
| `components-do-not-import-patterns`     | `src/components`, `src/internal`   | never `src/patterns`                                                                            |
| `internal-is-a-leaf`                    | `src/internal`                     | only `src/internal` (except `src/internal/shell`, next row)                                     |
| `internal-shell-uses-component-indexes` | `src/internal/shell`               | `src/internal` and components' `index.ts` only (never patterns or entries)                      |
| `entries-import-folder-indexes`         | `src/index.ts`, `src/entries/*.ts` | folder `index.ts` files and `src/internal` only                                                 |
| `nothing-imports-an-entry`              | anything in `src`                  | never an entry (one subpath never pulls in another)                                             |
| `docs-files-use-the-public-package`     | `*.docs.tsx`                       | components only from `'@rookcom/monarch-react'`, so the docs site never bundles a second copy   |
| `no-test-code-in-src`                   | runtime source                     | never tests, docs files, test helpers or the docs-only files in `src/internal`                  |
| `react-uses-tokens-public-api-only`     | `packages/react/src`               | tokens only through `@rookcom/monarch-tokens`                                                   |

## CSS order

`dist/styles.css` is the files in
[`src/styles/order.json`](../../packages/react/src/styles/order.json),
concatenated in that order inside `@layer monarch.components`. The order is
explicit, never the file system's, so moving a folder cannot reorder the
cascade. `scripts/build-css.ts` fails the build when:

- a `.css` file under `src` is not in `order.json`, is listed twice, or a
  listed file does not exist;
- two stylesheets define the same top-level `.mn-*` block: the first `.mn-`
  class of every selector, reduced to its BEM block (`.mn-dialog__close`
  belongs to `mn-dialog`). `styles/foundation.css` is exempt, as the shared base;
- two stylesheets define the same `@keyframes`;
- a stylesheet contains a raw colour.

The order today: `styles/foundation.css` first, then the components in the
order the 0.3 grouped files had them (actions, forms, display, overlays,
navigation, data, feedback, patterns), with `styles/choice.css` after Switch
and `styles/menu.css` after Dialog. **Append a new component's stylesheet at
the end of the list** (the scaffold does); move it earlier only if another
stylesheet must override it, and say why in the PR.

## Docs-file contract

Type: **`ComponentDocs`**, in
[`packages/react/src/internal/component-docs.ts`](../../packages/react/src/internal/component-docs.ts).
Each `<id>.docs.tsx` ends with `export default { … } satisfies ComponentDocs;`.

```ts
{
  id: 'tabs',             // the folder name
  title: 'Tabs',          // the primary export's name
  summary: 'Line and pill tab sets.',
  sections: {             // all 16, in this order on the page
    purpose, designPrinciples, apiReference, typedProperties, variants, states,
    interaction, keyboard, accessibility, responsive, theme, examples, doDont,
    related, dependencies, testing,
  },
}
```

- **API reference and properties are generated.** Both are
  `{ source: 'typescript', notes?: [...] }`; the page builds them from
  `dist/manifest.json` (`scripts/manifest.ts`, TypeScript compiler API) for the
  folder's exports (from the registry), so a prop's doc comment is its
  documentation.
- **Examples are live.** Each `{ title, render, code, stack? }` renders on the
  page and in the gallery. Import components from `'@rookcom/monarch-react'`.
  Demo icons come from `src/internal/docs-demo.tsx`; never sparkle icons.
- **List sections** (`variants`, `states`, `interaction`, `keyboard`,
  `responsive`, `theme`, …) may have an empty `items` list only with `notes`
  saying why ("Not interactive").
- **`dependencies`** must equal what the runtime files import (npm packages and
  other component folders); the check compares them.
- **`testing`** claims are checked: `axe: true` needs `expectNoA11yViolations`
  in the test, `ssr: true` needs `renderToString`.
- **`related`** ids must exist in the registry (planned ones are shown as planned).
- **Truth only.** A section that cannot be written truthfully is
  `incomplete('why', partial?)`, which renders a notice. Only the 36 pre-0.4
  folders (`LEGACY_FOLDERS`) may use it; new components ship complete docs.
  Scaffold `TODO` markers fail the check.

Reference docs: [button](../../packages/react/src/components/actions/button/button.docs.tsx),
[form-field](../../packages/react/src/components/forms/form-field/form-field.docs.tsx),
[tabs](../../packages/react/src/components/navigation/tabs/tabs.docs.tsx).

The docs site (`apps/docs`) has one page per docs file at
`#/components/<id>` (old `#/components/field` and `#/components/input` links
redirect). The registry records `docs.status`: `complete`, `legacy-incomplete`,
`missing` or `planned`.

## The `./shell` entry

The Rook app shell (`RookRail`, `AppShell`, `IdentityBar`, `ThemeToggle`,
`SpaceSwitcher`, `WorkspaceSwitcher`) ships from `@rookcom/monarch-react/shell`
([`src/entries/shell.ts`](../../packages/react/src/entries/shell.ts)). It is an
ordinary subpath entry: its folders sit in the usual families
(`components/identity/space-switcher`, `patterns/navigation/rook-rail`, …),
its stylesheets are in `src/styles/order.json` and `dist/styles.css`, its
exports are in `EXPORT_GROUPS`, and its registry entries say
`package: @rookcom/monarch-react`, `entry: ./shell`. Docs files import from
`'@rookcom/monarch-react/shell'`.

The one difference is `src/internal/shell/`: the cookie store (`rook_rail`,
`rook_theme`), the link component, the shell glyphs, and two parts several
shell components share, the account menu (`AccountMenuItems`, `ShellAccount`)
and a button with a tooltip (`TipButton`). Those two compose
`dropdown-menu` and `tooltip`, so `src/internal/shell` may import components
through their `index.ts` (`internal-shell-uses-component-indexes`); the rest of
`src/internal` stays a leaf. Its shared stylesheet is
`src/styles/shell-account.css` (`.mn-shell-account`).

## Adding a component

```sh
npm run new:component -- --id <registry-id> --family <family>   # --dry-run to preview
```

The scaffold reads the registry entry (name, layer, category, entry,
products) and refuses unknown ids, ids that already ship, ids for another
package (`@rookcom/monarch-tokens`), a family that does not
match the category, and entries whose index does not exist yet. It writes the
folder (typed `forwardRef` stub, CSS, test with render + axe + SSR, docs file
with TODOs, `index.ts`) and wires three places:

1. the export line in the entry index (`src/index.ts` for `.`);
2. the stylesheet at the end of `src/styles/order.json`;
3. `{ id, exports: [Name] }` in `EXPORT_GROUPS`
   (`scripts/registry/build-components.mjs`); add further exports (parts, hooks)
   there and to the entry's export line as you write them.

Then:

1. Build the component and its tests; replace every TODO in the docs file.
2. `npm run build`, then update the API snapshot deliberately:
   `npx vitest run -u packages/react/test/api.test.ts`.
3. `npm run registry:components && npm run docs:generate` — the registry
   entry moves from `planned` to `stable` with `source` and a docs route.
4. Visual baselines: add the id to `GALLERY` in
   [`apps/docs/src/pages/gallery-ids.ts`](../../apps/docs/src/pages/gallery-ids.ts)
   if it should be screenshotted. Each component is shot on its own
   (`apps/docs/tests/__screenshots__/visual/gallery/<id>/`), so this adds PNGs
   and changes no other component's baselines. Baselines are Linux only:
   generate them in CI (run the CI workflow with `update_snapshots: true` and
   commit only your component's files from the artifact). See
   [visual testing](../guides/visual-testing.md).
   Cross-browser axe
   picks up the new `#/components/<id>` page automatically
   (`apps/docs/tests/helpers.ts` `componentRoutes`).
5. `npm run changeset` (minor) and `npm run verify`.

A component whose registry entry is a subpath (`./data`, `./scheduling`, …)
needs that entry first: `src/entries/<name>.ts`, a `package.json` export and
an `ENTRY_FILES` mapping, in their own reviewed PR
([PACKAGE-ARCHITECTURE](PACKAGE-ARCHITECTURE.md)).

## How `verify` enforces this

| Step                                | Fails when                                                                                                                                                                                                                                    |
| ----------------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `npm run build` (`build-css.ts`)    | a stylesheet is missing from or doubled in `order.json`, two files define the same `.mn-*` block or keyframes, or a raw colour appears                                                                                                        |
| `npm run typecheck`                 | a docs file does not satisfy `ComponentDocs`                                                                                                                                                                                                  |
| `npm run deps:check`                | any import rule above is broken                                                                                                                                                                                                               |
| `npm run components:check`          | a folder is misplaced (family, layer, package), misnamed, missing a required file, has stray files, is not exported, or its docs file is missing a section, empty, has TODOs, misuses `legacy-incomplete`, or misstates dependencies or tests |
| `npm test`                          | a colocated test or the API snapshot fails                                                                                                                                                                                                    |
| `npm run registry:components:check` | an export is not in `EXPORT_GROUPS`, a group spans folders, an export comes from no folder, or the committed registry is stale                                                                                                                |
| `npm run docs:generate:check`       | the generated tables in `docs/` are stale                                                                                                                                                                                                     |
