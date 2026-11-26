# Instructions for AI coding agents

This file is read by AI coding agents and similar tools working in this
repository or in an application that consumes Monarch. Apps can copy a short
version of these rules into their own repository with
`npx monarch init-agents` (templates in `templates/agents/`).

## If you are building UI in an app that uses Monarch

Find components in the package and on the docs site, not in this repository:

- **Manifest:** `@rookcom/monarch-react/manifest.json`
  (`node_modules/@rookcom/monarch-react/dist/manifest.json`). One entry per
  export with `import` (the exact import line), `css` (the stylesheets it
  needs), `props`, `className`, `docs` and `scope`.
- **Docs:** `https://monarchui.dev/components/<id>/`, with a Markdown copy at
  `https://monarchui.dev/components/<id>/index.md`. For a whole entry point:
  `https://monarchui.dev/llms.txt` and `https://monarchui.dev/llms/<entry>.txt`
  (`core`, `ai`, `charts`, `data`, `patterns`, `shell`, `scheduling`,
  `collaboration`, `editor`, `marketing`, `rook`).
- **CLI** (`npm i -D -E @rookcom/monarch-conformance`):
  `npx monarch search <query>` finds a component, `npx monarch add <Component>`
  prints its import, CSS and a minimal example, and `npx monarch mcp` serves
  the same data to MCP clients (tools `search_components`, `get_component`,
  `list_tokens`, `get_theme`, `check_code`).
- **ESLint** (flat config, ESLint 9):
  `import monarch from '@rookcom/monarch-conformance/eslint'` and add
  `monarch.configs.recommended` (or `monarch.configs.general` outside Rook).
  The `monarch/*` rules report deep imports, raw colours, arbitrary spacing,
  local copies of components, deprecated import paths and icon-only buttons
  with no name while you write.
- **New app:** `npx @rookcom/monarch-conformance create <dir> --template
next-app|vite-react [--accent <hex>]` copies a working starter
  (`templates/starters/`); it never runs an install.
- **Figma:** `npx monarch figma-connect --map figma/map.json` writes Code
  Connect files from the manifest (`figma/README.md`). Monarch publishes no
  Figma library.

Rules:

1. Use Monarch components before writing any new UI primitive.
2. Exports with `scope: "rook"` in the manifest (the Rook bar and rail,
   `SignInWithRookButton`, `rookIntelligenceTransport`, the `ROOK_*`
   constants) encode Rook platform contracts. Import them from
   `@rookcom/monarch-react/rook`; the old paths (`/shell`, `/patterns`,
   `/ai`) are deprecated and stop exporting them in 0.15.0. Outside Rook, use
   the general components instead
   ([rook-specific-exports](https://github.com/RookHQ/monarch/blob/main/docs/guides/rook-specific-exports.md)).
   `monarch search` lists general components first.
3. Colour only through tokens: `--mn-*` variables or Tailwind classes
   `bg-mn-surface`, `text-mn-fg-muted`, `border-mn-line`,
   `bg-mn-accent-solid`, `text-mn-danger-strong`, and so on. Never
   `bg-red-500`, `#hex`, `rgb()`, or arbitrary `[...]` colours.
4. Type and spacing: Tailwind's named scale (`text-sm`, `p-4`, `gap-2`),
   never `text-[13px]` or `p-[7px]`.
5. Status meaning: `danger`, `success`, `warning` and `info` carry status;
   `accent` is the product's brand. Do not use the accent to mean a status.
6. Accent: a Rook product loads `@rookcom/monarch-tokens/css/products/<id>`.
   An app outside Rook loads `@rookcom/monarch-tokens/css/products/neutral`
   and sets `<html data-mn-product="neutral">` (or
   `appearanceScript({ product: 'neutral' })`), or derives its own brand
   with `deriveTheme(hex)` and `brandThemeCss` at build time.
7. Accessibility is not optional: every icon-only control has a label
   (`IconButton label=`), every input sits in a `Field` with a `FieldLabel`,
   every `Dialog` has a `DialogTitle`, and destructive actions use
   `variant="danger"` plus a confirmation.
8. Never call `window.confirm`, `alert` or `prompt`. Use `useConfirm()`,
   `usePrompt()`, `useChoice()` or `useUndoToast()`.
   `dialogs/no-native-dialog` enforces it.
9. When you import from a subpath (`@rookcom/monarch-react/patterns`, `/data`,
   `/charts`, `/ai`, `/shell`, `/rook` …) and the app loads `styles/core.css` rather
   than `styles.css`, also load the stylesheets listed in the manifest's
   `css` field once in the root layout; core.css does not style subpath
   components. `styles/subpath-css` reports it.
10. Style Monarch components through props, tokens and the root and part
    `mn-*` classes listed in the manifest (`className`). Do not target
    modifier classes or `--_*` variables, do not copy Monarch source into
    the app, and do not redefine `--mn-*` or `--primary`-style variables
    locally.
11. Before finishing, run `npx monarch check` (add `--product <id>` in a Rook
    product; it is optional) and fix new findings. Do not add
    `monarch-ignore` without a reason.

## Icons

Monarch does not use sparkle icons for AI (`Sparkles`, `Sparkle`,
`WandSparkles`, `Wand2`); use the lucide `Bot` icon. `icons/no-sparkles`
reports them.

## If you are changing Monarch itself

- Run `npm run verify` before claiming anything works. It runs format, lint,
  build, the bundle-size budgets, typecheck, dependency boundaries, 230+
  tests and registry validation.
- Bundle size: `npm run size:check` fails when an entry or a component grows
  past its budget in `registry/size-budgets.json`. If the growth is meant,
  run `npm run size:check -- --update` and commit the file
  ([performance budgets](docs/governance/performance-budgets.md)).
- Package entries are listed in `ENTRY_FILES`
  (`packages/react/scripts/component-folders.ts`). `./rook` is an alias
  entry: it only re-exports Rook-specific exports that another entry owns.
- Tokens: raw values go in `packages/tokens/src/primitives.ts` only;
  meaning in `themes.ts`. The contrast tests in `test/contrast.test.ts` must
  pass for every product in `registry/products` and every theme.
- Components: one folder per component, `src/{components,patterns}/<family>/<id>/`,
  created with `npm run new:component -- --id <id> --family <family>` and laid
  out as in [SOURCE-LAYOUT](docs/architecture/SOURCE-LAYOUT.md): `<id>.tsx`,
  plain CSS in `<id>.css` using only `--mn-*` variables (the build rejects raw
  colours and any stylesheet not in `src/styles/order.json`), stable `mn-*`
  class names and `data-*` state attributes, `<id>.test.tsx` covering
  keyboard, focus, accessible names (axe) and SSR, `<id>.docs.tsx` with all 16
  sections, and `index.ts`. Import other components only through their
  `index.ts`. Update the API snapshot deliberately. The full contract is
  [component-contract](docs/guides/component-contract.md).
- Versions follow [versioning](docs/governance/versioning.md): a patch carries
  fixes and additive features, never a breaking change; `@rookcom/monarch-react`
  stays on 0.14.x. Public API changes need a changeset; breaking changes need
  an ADR in `docs/adr` and a note in `docs/guides/migration.md`.
- Never hand-edit generated registry files.
- Never write a score or percentage. Report counts.
- Do not claim a product has adopted Monarch. Only `monarch registry verify`
  decides that, from a real checkout.
- Reference: [frameworks](docs/guides/frameworks.md),
  [accessibility statement](docs/governance/accessibility-statement.md),
  [accessibility conformance report](docs/governance/vpat.md),
  [browser support](docs/governance/browser-support.md),
  [i18n](docs/governance/i18n.md),
  [dependencies](docs/governance/dependencies.md).
