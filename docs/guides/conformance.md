# Conformance

`@rookcom/monarch-conformance` is a static checker that reports where a product departs from Monarch. It also validates the product registry and records each product's adoption status. It has no runtime dependencies and runs offline against a checkout.

It reports **counts** (and counts per 1,000 scanned lines). It never reports a compliance score or percentage.

```sh
npx monarch check --root . --product tickets --registry path/to/monarch/registry
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

| Rule                                    | Default | What it finds                                                                                                                                                                                                                                                                                                                                                                                                   |
| --------------------------------------- | ------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `color/no-raw-palette`                  | error   | Tailwind palette classes (`text-red-500`, `hover:bg-slate-50/50`, `from-blue-600`, `bg-white/10`) and arbitrary colour classes (`bg-[#fff]`, `text-[rgb(…)]`). Token-backed classes like `bg-[hsl(var(--primary))]` are allowed. Also checks CSS `@apply`.                                                                                                                                                      |
| `color/no-hard-coded-color`             | error   | Hex, `rgb()`, `hsl()`, `oklch()` … literals in code and CSS. Skips anchors (`href="#add"`), `url(#id)`, issue numbers (`#1234 created`), and design-token definitions, which `tokens/local-token-definitions` reports instead. App-local variables (`--rs-bg: #15181f`) are still checked.                                                                                                                      |
| `typography/no-arbitrary-type`          | warn    | `text-[13px]`, `font-[550]`, `leading-[1.1]`, `tracking-[…]`, inline `fontFamily`, CSS `font-family` that isn't `var(--…)`/`inherit`.                                                                                                                                                                                                                                                                           |
| `spacing/off-grid`                      | warn    | Arbitrary padding, margin, gap, space and inset values that aren't on the 4px grid or its 2px half-step, in classes and in CSS `padding`/`margin`/`gap`. Allows `var()`, `calc()`, `%`, viewport units, `em`, and 0/1px hairlines.                                                                                                                                                                              |
| `tokens/local-token-definitions`        | error   | CSS blocks defining design tokens (`--background`, `--primary`, `--radius`, `--sidebar-*`, `--chart-N` …). One finding per block, with a count.                                                                                                                                                                                                                                                                 |
| `components/duplicated-primitive`       | warn    | `components/{ui,primitives,common}/<name>.tsx` (or `<name>/index.tsx`) where the name is a Monarch component (see `src/monarch-components.ts`).                                                                                                                                                                                                                                                                 |
| `a11y/img-alt`                          | error   | `<img>` without `alt`. Tags that spread props are skipped.                                                                                                                                                                                                                                                                                                                                                      |
| `a11y/click-without-key`                | error   | `onClick` on `div`, `span`, `li`, `tr`, `td` … without both `role` and `onKey*`. Skips `aria-hidden` elements, spread props, and handlers that only call `stopPropagation()`.                                                                                                                                                                                                                                   |
| `a11y/positive-tabindex`                | error   | `tabIndex` greater than 0.                                                                                                                                                                                                                                                                                                                                                                                      |
| `a11y/outline-none-without-ring`        | error   | `outline-none` with no focus replacement (`focus-visible:ring-*`, `focus:bg-*`, `data-[highlighted]:…` …) within three lines.                                                                                                                                                                                                                                                                                   |
| `overrides/monarch-internals`           | error   | Selectors targeting `.mn-*` classes, and redefining `--mn-*` variables in CSS or inline styles.                                                                                                                                                                                                                                                                                                                 |
| `deps/monarch-version`                  | mixed   | Reads `package.json` and `registry/compatibility.json`. Unsupported or unknown Monarch packages are errors, deprecated versions are warnings, and "not adopted" or "newer version available" are info.                                                                                                                                                                                                          |
| `api/deprecated`                        | warn    | Uses of names listed in `registry/deprecations.json`, in files that import from that package.                                                                                                                                                                                                                                                                                                                   |
| `dialogs/no-native-dialog`              | error   | `window.confirm`, `alert`, `prompt` (also via `globalThis`/`self`) and bare `confirm(`/`alert(`/`prompt(` calls. Skips names the file binds itself, such as `const { confirm } = useConfirm()`, member calls (`dialog.confirm()`), method definitions, strings and comments. Replace with `useConfirm`, `usePrompt`, `useChoice`, `useUndoToast` or a toast; see [destructive actions](destructive-actions.md). |
| `legacy/bridge-variable`                | info    | Direct `var(--primary)`-style use of legacy variables. Tracked so the legacy bridge can eventually be removed.                                                                                                                                                                                                                                                                                                  |
| `governance/suppression-without-reason` | warn    | A suppression comment with no `-- reason`. The suppression is ignored.                                                                                                                                                                                                                                                                                                                                          |
| `governance/expired-exception`          | error   | A registry exception past its `expires` date. It no longer suppresses anything.                                                                                                                                                                                                                                                                                                                                 |

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

SARIF results carry `baselineState` (`new`/`unchanged`) and a stable fingerprint. That means alerts survive line moves. Use `--format json` for machine processing. The JSON includes the tool version, root, git commit, files and lines scanned, counts by rule and severity, density per 1,000 lines, exceptions, and every finding.

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
- **`needs-migration`**: at least one peer is out of range. For example, Tailwind 4 needs the CSS-only path because the preset targets Tailwind 3.
- **`unknown`**: React is missing, or a version spec can't be read.
