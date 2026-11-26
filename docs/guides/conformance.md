# Conformance

`@rookcom/monarch-conformance` is a static checker that reports where a product departs from Monarch. It also validates the product registry and records each product's adoption status. It has no runtime dependencies and runs offline against a checkout.

It reports **counts** (and counts per 1,000 scanned lines). It never reports a score or percentage.

It works in any app, inside Rook or not. `--product` is optional: it names a Rook product (its source paths and exceptions come from the registry). Compatibility and deprecation data are bundled into the package, so the version check works without a registry checkout. An unknown product is a warning, not an error. Teams outside Rook can pass `--accent #hex` for their brand colour; the CLI then points to `deriveTheme` and `brandThemeCss` (see [theming](theming.md)).

```sh
npx monarch check                                       # any app, no product needed
npx monarch check --accent '#3355aa'                     # outside Rook, with your brand colour
npx monarch check --root . --product tickets --registry path/to/monarch/registry
npx monarch check --fix                                  # apply safe fixes, then report
npx monarch check --format sarif --out monarch.sarif
npx monarch check --write-baseline .monarch/baseline.json   # once, at adoption
npx monarch check --baseline .monarch/baseline.json          # in CI from then on
```

Exit codes: `0` pass, `1` new error-severity findings (or an invalid registry), `2` usage error.

## What is scanned

- Extensions: `.ts .tsx .js .jsx .mjs .cjs .css .scss`.
- Always skipped: `node_modules .next .git dist build out coverage .turbo .vercel .open-next .wrangler storybook-static generated __generated__`, `*.d.ts`, `*.min.*`, `*.generated.*`, files over 1 MB.
- Excluded by default: tests and stories (`**/*.test.*`, `**/*.spec.*`, `**/__tests__/**`, `**/__mocks__/**`, `**/*.stories.*`), because they legitimately contain colour literals and unusual markup.
- Roots: `include` from `monarch.config.json`, otherwise the product's `source.paths` from the registry, otherwise the whole repository.
- Whole-line comments (`//`, `/*`, `*`, `{/*`) are not scanned.

Scanning is lexical: regular expressions plus a small JSX tag reader that balances braces. It is tuned to miss an edge case rather than report a false one. Please report false positives as bugs.

## Rules

| Rule                                    | Default | What it finds                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                   |
| --------------------------------------- | ------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `color/no-raw-palette`                  | error   | Tailwind palette classes (`text-red-500`, `hover:bg-slate-50/50`, `from-blue-600`, `bg-white/10`) and arbitrary colour classes (`bg-[#fff]`, `text-[rgb(…)]`). Token-backed classes like `bg-[hsl(var(--primary))]` are allowed. Also checks CSS `@apply`.                                                                                                                                                                                                                                                                                                                                                                                                      |
| `color/no-hard-coded-color`             | error   | Hex, `rgb()`, `hsl()`, `oklch()` … literals in code and CSS. Skips anchors (`href="#add"`), `url(#id)`, issue numbers (`#1234 created`), and design-token definitions, which `tokens/local-token-definitions` reports instead. App-local variables (`--rs-bg: #15181f`) are still checked.                                                                                                                                                                                                                                                                                                                                                                      |
| `typography/no-arbitrary-type`          | warn    | `text-[13px]`, `font-[550]`, `leading-[1.1]`, `tracking-[…]`, inline `fontFamily`, CSS `font-family` that isn't `var(--…)`/`inherit`.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                           |
| `spacing/off-grid`                      | warn    | Arbitrary padding, margin, gap, space and inset values that aren't on the 4px grid or its 2px half-step, in classes and in CSS `padding`/`margin`/`gap`. Allows `var()`, `calc()`, `%`, viewport units, `em`, and 0/1px hairlines.                                                                                                                                                                                                                                                                                                                                                                                                                              |
| `tokens/local-token-definitions`        | error   | CSS blocks defining design tokens (`--background`, `--primary`, `--radius`, `--sidebar-*`, `--chart-N` …). One finding per block, with a count.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                 |
| `components/duplicated-primitive`       | warn    | `components/{ui,primitives,common}/<name>.tsx` (or `<name>/index.tsx`) where the name is a Monarch component. The list is generated from the component exports of `@rookcom/monarch-react` at build time (`npm run registry:conformance`), so it follows every release; Rook-specific exports are left out.                                                                                                                                                                                                                                                                                                                                                     |
| `a11y/img-alt`                          | error   | `<img>` without `alt`. Tags that spread props are skipped.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                      |
| `a11y/click-without-key`                | error   | `onClick` on `div`, `span`, `li`, `tr`, `td` … without both `role` and `onKey*`. Skips `aria-hidden` elements, spread props, and handlers that only call `stopPropagation()`.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                   |
| `a11y/positive-tabindex`                | error   | `tabIndex` greater than 0.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                      |
| `a11y/outline-none-without-ring`        | error   | `outline-none` with no focus replacement (`focus-visible:ring-*`, `focus:bg-*`, `data-[highlighted]:…` …) within three lines.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                   |
| `overrides/monarch-internals`           | error   | Selectors targeting `.mn-*` classes, and redefining `--mn-*` variables in CSS or inline styles. The variables a component page documents as public (`--mn-toast-offset-block-start`, `--mn-toast-offset-block-end`, `--mn-toast-offset-inline-end`) are not reported.                                                                                                                                                                                                                                                                                                                                                                                           |
| `deps/monarch-version`                  | mixed   | Reads `package.json` and the compatibility data (bundled, or `registry/compatibility.json` with `--registry`). Unsupported or unknown Monarch packages are errors, deprecated versions are warnings, and "not adopted" or "newer version available" are info.                                                                                                                                                                                                                                                                                                                                                                                                   |
| `api/deprecated`                        | warn    | Uses of names listed in `registry/deprecations.json`, in files that import from that package. Also an import of an export that moved to another entry from its old path (`RookRail` from `/shell` instead of `/rook`); `--fix` rewrites the path.                                                                                                                                                                                                                                                                                                                                                                                                               |
| `dialogs/no-native-dialog`              | error   | `window.confirm`, `alert`, `prompt` (also via `globalThis`/`self`) and bare `confirm(`/`alert(`/`prompt(` calls. Skips names the file binds itself, such as `const { confirm } = useConfirm()`, member calls (`dialog.confirm()`), method definitions, strings and comments. Replace with `useConfirm`, `usePrompt`, `useChoice`, `useUndoToast` or a toast; see [destructive actions](destructive-actions.md).                                                                                                                                                                                                                                                 |
| `styles/subpath-css`                    | warn    | An import from a subpath entry (`@rookcom/monarch-react/patterns`, `/data`, `/charts`, `/ai`, `/shell` …) when no scanned file loads `@rookcom/monarch-react/styles.css`, `styles/<entry>.css`, or the `styles/components/<id>.css` sheet of every component used. `styles/core.css` does not include subpath components. One finding per entry, at the first import, naming the import to add. Type-only imports, hooks and helpers are ignored; a namespace or dynamic import is reported only when nothing of that entry is styled. Stylesheets loaded outside scanned files (a `<link>` in HTML) are not seen: suppress with a reason or turn the rule off. |
| `imports/package-path`                  | warn    | Imports that reach into a Monarch package's `dist/` folder (`@rookcom/monarch-tokens/dist/css/monarch.css`, `@rookcom/monarch-react/dist/entries/ai.js`). These paths are not in the package's `exports` and can move in any release. Fixed by `--fix`.                                                                                                                                                                                                                                                                                                                                                                                                         |
| `legacy/bridge-variable`                | info    | Direct `var(--primary)`-style use of legacy variables. Tracked so the legacy bridge can eventually be removed.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                  |
| `governance/suppression-without-reason` | warn    | A suppression comment with no `-- reason`. The suppression is ignored.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                          |
| `governance/expired-exception`          | error   | A registry exception past its `expires` date. It no longer suppresses anything.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                 |

## Fixes

Some findings carry a machine-applicable fix. `monarch check --fix` applies the **safe** ones and then reports what is left; `--fix=all` also applies the ones that need **review**. In `--format json`, each finding has `help`, `docs` and, when known, `fix`: `{ description, safety, edits: [{ file, line, column, length, text }] }`.

| Rule                            | Fix                                                                                                                                                                  | Safety                                                         |
| ------------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------- | -------------------------------------------------------------- |
| `imports/package-path`          | The public subpath (`@rookcom/monarch-tokens/css`, `@rookcom/monarch-react/ai`).                                                                                     | safe                                                           |
| `styles/subpath-css`            | The missing stylesheet import, after the last Monarch stylesheet import in the root layout or global CSS (a file that imports `@rookcom/monarch-react` styles wins). | safe                                                           |
| `color/no-hard-coded-color`     | `var(--mn-color-…)` when the hex equals exactly one semantic colour in the light theme (product accents excluded).                                                   | safe when the token has that value in every theme, else review |
| `api/deprecated`                | The replacement name from the deprecation entry.                                                                                                                     | review (check the import)                                      |
| `api/deprecated` (moved export) | The new import path. A statement on one line that mixes moved and other names is split in two; one over several lines is reported without a fix.                     | safe                                                           |

## ESLint plugin

The same package ships an ESLint 9 plugin (flat config) at
`@rookcom/monarch-conformance/eslint`, for feedback in the editor while code
is written. `monarch check` stays the whole-project check for CI: it also
reads CSS, `package.json` and the registry, which a per-file linter does not.
`eslint` is an optional peer of the package; nothing is installed for it.

```js
// eslint.config.js
import monarch from '@rookcom/monarch-conformance/eslint';

export default [
  monarch.configs.recommended, // Rook apps
  // monarch.configs.general,  // teams outside Rook: also reports Rook-specific exports
];
```

Each config is one flat-config object for `**/*.{js,jsx,mjs,cjs,ts,tsx,mts,cts}`
that registers the plugin as `monarch`. TypeScript and JSX files need a parser
in your config as usual (`typescript-eslint`).

| Rule                               | recommended | general | What it reports                                                                                                                                                                                                           | Fix                                                                             |
| ---------------------------------- | ----------- | ------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------- |
| `monarch/no-deep-imports`          | error       | error   | An import, re-export, `import()` or `require()` of `@rookcom/monarch-*/dist/…`.                                                                                                                                           | Fix: the public path, when one is known.                                        |
| `monarch/require-entry-styles`     | warn        | warn    | An import of components from a subpath entry (`@rookcom/monarch-react/ai`) whose stylesheet the app does not load. It needs to be told what the app loads; without options it reports nothing.                            | None: the import belongs in another file. The message names it.                 |
| `monarch/no-raw-colors`            | error       | error   | In `className`, `class`, `style` and class helpers (`cn`, `clsx`, `cva`, `twMerge` …): Tailwind palette classes (`bg-red-500`), arbitrary colour classes (`bg-[#fff]`) and hex, `rgb()`, `hsl()` literals.                | Suggestion: `var(--mn-color-…)` when the hex equals exactly one semantic token. |
| `monarch/no-arbitrary-spacing`     | warn        | warn    | Arbitrary spacing and type classes: `p-[7px]`, `text-[13px]`. Spacing on the 4px grid or its 2px half-step is allowed, as in `monarch check`; set `allowOnGrid: false` to report it too.                                  | Suggestion: the scale class (`p-[8px]` → `p-2`) with `allowOnGrid: false`.      |
| `monarch/prefer-monarch-component` | warn        | warn    | A component declared at the top of a module with the name of a Monarch component (`function Button`, `const Dialog = …`). A file that imports that Monarch component (a wrapper) is left alone. Option `allow: ['Link']`. | None. The message gives the import line.                                        |
| `monarch/no-deprecated-api`        | warn        | warn    | A deprecated export, and an export imported from a path it moved away from. The data is bundled with the package.                                                                                                         | Fix: the replacement, imported under the old local name; or the new path.       |
| `monarch/no-rook-specific`         | off         | warn    | An import of an export with `scope: "rook"` in the manifest, of `@rookcom/monarch-react/rook`, or of a Rook product accent stylesheet.                                                                                    | None. The message names the general component to use.                           |
| `monarch/require-accessible-name`  | error       | error   | Monarch's `IconButton` without `label`, and `Button` with no text and no `aria-label` or `aria-labelledby`. Elements with spread props are skipped.                                                                       | None.                                                                           |

`monarch/require-entry-styles` reads no files on its own. Give it the
stylesheets the app loads, the files that load them, or both:

```js
{
  rules: {
    'monarch/require-entry-styles': [
      'warn',
      {
        // Read these files (relative to where ESLint runs) for Monarch stylesheet imports.
        rootFiles: ['app/layout.tsx', 'app/globals.css'],
        // Or list the sheets directly.
        styles: ['@rookcom/monarch-react/styles/core.css'],
      },
    ],
  },
}
```

Rule names differ from the CLI's because ESLint rules are per file and read
the syntax tree, while `monarch check` is lexical and project-wide. They agree
on what is a finding: the plugin uses the CLI's patterns, token data,
component list and deprecation data.

## Starting a new app: `create`

```sh
npx @rookcom/monarch-conformance create my-app --template next-app
npx @rookcom/monarch-conformance create my-app --template vite-react --accent '#3355aa'
```

`monarch create <dir> --template next-app|vite-react [--name NAME] [--accent HEX]`
copies a starter into a new or empty directory and prints what to run next.
It never runs an install and refuses a directory that has files in it.

Both starters ([`templates/starters`](../../templates/starters)) are small and
complete: tokens and core styles loaded once, the appearance script (with a
CSP nonce in the Next.js starter), `DirectionProvider`, a page of core
components, an assistant page wired to `mockTransport`, Tailwind 4 through
`@rookcom/monarch-tokens/css/tailwind-v4`, an ESLint flat config with
`monarch.configs.general`, `monarch.config.json` and `AGENTS.md`. They use the
neutral accent. With `--accent`, the colour goes into `brand.json` and
`scripts/brand.mjs` derives an accessible theme from it with `deriveTheme` and
`brandThemeCss` before each `dev` and `build`.

## Figma Code Connect: `figma-connect`

```sh
npx monarch figma-connect --map figma/map.json --out figma
```

Writes one `<Component>.figma.tsx` per entry of your map (component name →
Figma node URL), from the component manifest: props with a fixed set of values
become `figma.enum`, booleans `figma.boolean`, strings `figma.string` and
`children` `figma.children`. Existing files are kept unless `--force`.
Placeholder URLs are refused.

Monarch does not publish a Figma library. Publishing the generated files needs
your own Figma library and the Figma CLI; the steps are in
[`figma/README.md`](../../figma/README.md).

## Finding components: `search` and `add`

Both read the component manifest the app has installed (`@rookcom/monarch-react/manifest.json`), so answers match the version in use. Without one, they use the copy bundled with the CLI.

```sh
npx monarch search date picker       # ranked list; general components before Rook-specific ones
npx monarch search select --json     # full manifest entries
npx monarch add Select               # import line, stylesheets, a minimal example, related exports, docs link
npx monarch add Select --write       # also adds missing stylesheet imports next to your Monarch CSS imports
```

## Instructions for coding agents: `init-agents`

```sh
npx monarch init-agents              # writes AGENTS.md, CLAUDE.md, .cursor/rules/monarch.mdc, .github/copilot-instructions.md
npx monarch init-agents --force      # overwrite files that exist
```

Existing files are kept unless you pass `--force`. The templates live in [`templates/agents`](../../templates/agents) and ship with the package.

## MCP server: `monarch mcp`

`monarch mcp` is a [Model Context Protocol](https://modelcontextprotocol.io) server over stdio. Coding agents call it to look Monarch up instead of guessing. It answers for the app it starts in (`--root` to change), using the app's installed manifest and tokens. It has no dependencies and sends nothing over the network.

| Tool                | What it returns                                                                                                                     |
| ------------------- | ----------------------------------------------------------------------------------------------------------------------------------- |
| `search_components` | Exports matching a query, general components first.                                                                                 |
| `get_component`     | Props with types, defaults and allowed values; the import line; the stylesheets; a minimal example; related exports; the docs page. |
| `list_tokens`       | Tokens from `@rookcom/monarch-tokens/tokens.json`, filtered by a path prefix.                                                       |
| `get_theme`         | `deriveTheme(brand, { ai })` for light, dark and both more-contrast modes, and the CSS from `brandThemeCss`.                        |
| `check_code`        | Conformance findings for a snippet or a file or folder of the app, with `help`, `docs` and fixes.                                   |

Install the CLI in the app first (`npm i -D -E @rookcom/monarch-conformance`), then add the server:

**Claude Code**

```sh
claude mcp add monarch -- npx monarch mcp
```

Or commit `.mcp.json` at the app root so the whole team gets it:

```json
{ "mcpServers": { "monarch": { "command": "npx", "args": ["monarch", "mcp"] } } }
```

**Cursor**: `.cursor/mcp.json` in the project:

```json
{ "mcpServers": { "monarch": { "command": "npx", "args": ["monarch", "mcp"] } } }
```

**VS Code** (GitHub Copilot agent mode): `.vscode/mcp.json`:

```json
{ "servers": { "monarch": { "type": "stdio", "command": "npx", "args": ["monarch", "mcp"] } } }
```

Run `npx monarch mcp` from a terminal to check it starts: it prints one line to stderr and waits for messages on stdin.

## Configuration: `monarch.config.json`

```json
{
  "product": "tickets",
  "include": ["src"],
  "exclude": ["src/legacy/**"],
  "rules": { "typography/no-arbitrary-type": "error", "legacy/bridge-variable": "off" }
}
```

Rule settings are `off`, `info`, `warn` or `error`. Raising a severity is fine. Lowering one for a whole product should go through an exception instead, so it has an owner and an expiry.

## Inline suppressions

```tsx
{
  /* monarch-ignore-next-line color/no-raw-palette -- partner badge uses the partner's brand colour */
}
<span className="bg-blue-600">…</span>;
```

```ts
// monarch-ignore-next-line color/no-hard-coded-color, typography/* -- OG images cannot read CSS variables
```

A suppression applies to the next line only. It can name several rules, or a group with `/*`. It must give a reason after `--`. Without a reason it is ignored and reported.

## Exceptions (registry)

Larger or longer-lived deviations live in the product's registry entry, where they are reviewed like code:

```json
"exceptions": [
  {
    "rule": "color/no-hard-coded-color",
    "paths": ["src/app/api/og/**", "src/app/layout.tsx"],
    "owner": "team-dots",
    "reason": "OG images and themeColor meta need literal colours",
    "expires": "2027-03-31",
    "approvedBy": "design-systems"
  }
]
```

`rule` can be exact or a group (`color/*`). `paths` are globs relative to the repository root. An active exception suppresses matching findings, and the report lists it with the number it suppressed. When `expires` (`YYYY-MM-DD`) passes, the exception stops applying and becomes a `governance/expired-exception` error.

## Baseline ratchet (how existing apps adopt)

The 21 registered products start with hundreds of existing findings, so `check` can be run against a baseline:

1. `monarch check --write-baseline .monarch/baseline.json` and commit the file. It records error and warning counts per file and rule.
2. In CI, run `monarch check --baseline .monarch/baseline.json`. For each (file, rule) pair, findings up to the baseline count are marked as known. Only findings above the count are "new", and only new errors fail the build.
3. When the report says some groups are below baseline, regenerate the baseline to lock in the improvement. Counts should only go down.

## CI

```yaml
- name: Monarch conformance
  run: npx monarch check --baseline .monarch/baseline.json --format sarif --out monarch.sarif
- name: Upload to code scanning
  if: always()
  uses: github/codeql-action/upload-sarif@v3
  with:
    sarif_file: monarch.sarif
    category: monarch
```

SARIF results carry `baselineState` (`new`/`unchanged`) and a stable fingerprint. That means alerts survive line moves. Every finding that has a machine fix carries it as a SARIF `fixes` entry (`artifactChanges` with `replacements`), and every rule has its own `helpUri`. Use `--format json` for machine processing. The JSON includes the tool version, root, git commit, files and lines scanned, counts by rule and severity, density per 1,000 lines, exceptions, and every finding.

## Registry commands

```sh
monarch registry validate --registry registry
monarch registry verify   --registry registry --product my-app --checkout ../my-app
monarch registry verify   --registry registry --all --checkouts checkouts.json   # { "my-app": "/path", … }
monarch registry report   --registry registry --out STATUS.md
```

- **validate** checks every `registry/products/*.json` against `registry/product.schema.json`. It uses a built-in validator for the keyword subset the schema uses, and refuses unknown keywords rather than skipping them. It also checks that the id matches the file name, and that ids, domains and repositories are unique.
- **verify** is read-only against the checkout. It writes `registry/status/<id>.json` with the commit and dirty flag, detected framework versions, Monarch packages, adoption, compatibility, and conformance counts. Status files are generated. Never edit them by hand. Commit them only in a registry you own; this repository does not publish any.
- **report** renders the status files as a Markdown table.

### How registry verify decides adoption

| Status        | Condition                                                                                                                                                |
| ------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `not-adopted` | No `@rookcom/monarch-*` dependency in `package.json`.                                                                                                    |
| `partial`     | A Monarch dependency exists, but the source doesn't import `@rookcom/monarch-tokens/css`, or local token blocks remain, or duplicated components remain. |
| `adopted`     | A Monarch dependency, a Monarch CSS import, zero local token blocks, and zero duplicated components.                                                     |

Palette and accessibility findings do not block `adopted`. They are tracked through counts and the baseline ratchet.

Compatibility compares the lowest version each `package.json` range allows against the peer ranges in `registry/compatibility.json`:

- **`compatible`**: all present peers are in range.
- **`needs-migration`**: at least one peer is out of range. For example, a React version below 18.3. Tailwind 4 is supported through `@rookcom/monarch-tokens/css/tailwind-v4` instead of the preset; the status notes say so.
- **`unknown`**: React is missing, or a version spec can't be read.
