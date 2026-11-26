# Performance budgets

Monarch keeps two size budgets. Both run in `npm run verify` and in CI, and
both fail the build instead of warning.

| Budget              | What it measures                                            | Where the limits live                                                             | Command               |
| ------------------- | ----------------------------------------------------------- | --------------------------------------------------------------------------------- | --------------------- |
| Package (this page) | Every entry and every component of `@rookcom/monarch-react` | [`registry/size-budgets.json`](../../registry/size-budgets.json)                  | `npm run size:check`  |
| Docs site           | Initial JavaScript and CSS of each page of the docs site    | `scripts/docs/check-performance-budget.mjs` ([docs site](../guides/docs-site.md)) | `npm run docs:budget` |

## What `size:check` measures

For each package entry (`.`, `./ai`, `./charts`, `./rook` …) and each
component folder, the script bundles that module on its own, minifies it with
esbuild and gzips the result. Imports of other packages (React, Radix, Tiptap,
`@rookcom/monarch-tokens`) are left external, so a number is the cost of
Monarch's own code, not of its dependencies.

- A component's size includes the Monarch components it composes. `Combobox`
  includes the menu and input code it uses, because an app that imports only
  `Combobox` downloads that too. Sizes therefore do not add up to the entry's
  size.
- The numbers are bytes of JavaScript. Stylesheets are not included.
- esbuild is already installed in this repository (tsx and Vite depend on
  it). If it cannot be loaded, the script gzips the unminified module graph
  instead, says so, and refuses to compare against budgets recorded with
  esbuild.

## The budget file

`registry/size-budgets.json` is generated. Do not edit it by hand, except for
`coreHardCap`.

```json
{
  "method": "esbuild-minify-gzip",
  "coreHardCap": 131072,
  "entries": { ".": { "gzip": 101234, "budget": 111358 } },
  "components": { "button": { "gzip": 1830, "budget": 2013 } }
}
```

The numbers above only show the shape.

- `gzip`: the size when the budgets were last updated.
- `budget`: `gzip` × 1.10, rounded up. The check fails one byte above it.
- `coreHardCap`: the ceiling of the root entry (`.`). It is set once, a
  quarter above the root entry's size at that time and rounded up to a whole
  KiB, and `--update` never raises it.

## When the check fails

`npm run size:check` prints what is over budget, largest overshoot first. It
also fails when a component has no budget yet (a new component) or a budget
has no component (a removed one), so the file never goes stale.

1. Look for the cause first: a new dependency pulled into a component, a
   large constant, a component in the root entry that belongs in a subpath.
2. If the growth is intended, run `npm run size:check -- --update` after
   `npm run build`, and commit `registry/size-budgets.json` in the same pull
   request. The diff shows reviewers every size that moved.
3. If the root entry is over `coreHardCap`, move code to a subpath entry. Raise
   the cap only by editing the number, with the reason in the pull request.

A new component gets its first budget from `--update` in the pull request that
adds it.

## Sizes in the manifest

`@rookcom/monarch-react/manifest.json` carries `size: { "gzip": <bytes> }` on
every export that comes from a component folder, copied from the budget file
when the package is built. `monarch search --json`, `monarch add --json` and
the MCP tool `get_component` return it with the rest of the entry. Exports of
one folder share one number: it is the size of the folder's module.

## What is not measured

- Runtime performance (render time, interaction latency). There is no
  automated benchmark for it yet.
- The size of dependencies. Their versions and licences are tracked in
  [dependencies](dependencies.md).
- What an app really ships. That depends on the app's bundler and on which
  exports it uses; the budgets are an upper bound per import, measured the
  same way every time.
