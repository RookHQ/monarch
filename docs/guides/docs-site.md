# Docs site and visual regression

`apps/docs` is the Monarch documentation and playground. It is a normal
consumer of the published packages (Vite + React, imports from the built
`dist` entry points) so it doubles as the integration test for them.

## Run

```bash
npm run build          # packages first; the site imports their dist
npm run build:docs     # apps/docs/dist
npm run dev -w @rookcom/monarch-docs   # http://localhost:5173
```

## What is on it

- Home: the hero from the Monarch launch page, drawn in tokens, and live previews.
- Foundations: introduction, colours (live swatches with contrast ratios for the
  current theme, contrast and product accent), typography, tokens, theming.
- Components and patterns: an overview grid with a live thumbnail per docs file,
  and one page per component folder at `#/components/<id>`, rendered from its
  `<id>.docs.tsx` ([docs-file contract](../architecture/SOURCE-LAYOUT.md#docs-file-contract)).
  The first example is shown large; every example has "Show code"; API and props
  tables come from `@rookcom/monarch-react/manifest.json`.
- Guides rendered from `docs/guides/*.md` at build time (no copy to drift).
- Search (⌘K / Ctrl K): Monarch's `CommandPalette` over every page.
- `#/__gallery`, `#/__gallery/<id>` and `#/__marketing`: no chrome; the screenshot surfaces.

The top bar's theme switcher sets the shared `rook_theme` cookie; the
appearance popover also sets `mn_contrast` and the previewed `data-mn-product`,
so any example can be viewed as any Rook product. Pre-paint scripts apply all
three before first paint (`system` theme and contrast by default).

## Source layout

```
apps/docs/
  plugins/          monarch-docs.ts (Vite plugin), extract-examples.ts (+ test)
  src/
    app/            app.tsx (providers, hidden routes), routes.tsx (lazy pages), navigation.ts
    layout/         docs-shell, top-bar, sidebar, page-frame, toc, site-footer
    features/       appearance/, search/, code/ (code block, highlighter), examples/, brand/, content/
    pages/          home/, foundations/, components/, guides/, gallery.tsx (+ gallery-ids.ts)
    lib/            docs.ts (index + lazy docs files), router, appearance, manifest, registry, facts
    marketing/      the #/__marketing reference page
    styles/         base, gallery (baseline surface), shell, content, examples, home, foundations, component-page
```

## Example code ("Show code")

Example code is never written by hand. `plugins/monarch-docs.ts` adds an
`exampleSource` export to every `<id>.docs.tsx` when the site builds:
`extract-examples.ts` reads the file with the TypeScript compiler API and, for
each example, emits the imports it uses, the file-level declarations it depends
on (resolved with the type checker, transitively, in file order) and the body of
`render`. A title that is not a string literal, a spread into `items`, or a
`render` that is not an inline function fails the build with the file and line.
`npm test` (project `docs`) checks every docs file: one snippet per example,
same titles, none empty. The plugin also serves `virtual:monarch-docs-index`
(ids, titles, summaries, registry exports), so the shell loads without the docs
files, which load per page.

## Tests

| Command                      | Project                                        | What                                                                                                                                                 |
| ---------------------------- | ---------------------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------- |
| `npm run test:a11y`          | `a11y-chromium`, `a11y-firefox`, `a11y-webkit` | axe-core on every route (every component page included) × light/dark, search, Show code, keyboard flow, theme/product controls, in all three engines |
| `npm run test:visual`        | `visual`                                       | per-component screenshots (see [visual testing](visual-testing.md)); Linux only, skipped on other platforms                                          |
| `npm run test:visual:update` |                                                | regenerate snapshots after an intended visual change                                                                                                 |

Snapshots live in `apps/docs/tests/__screenshots__/visual`, Linux only, and
are reviewed in the PR like code. [Visual testing](visual-testing.md) explains
the matrix and how to update them. Threshold is 0.2% of pixels. Visual
baselines are Chromium only, because pixel output legitimately differs between
engines; accessibility and keyboard behaviour are what must match in every
browser.

CI runs in the pinned `mcr.microsoft.com/playwright:v1.56.1-noble` container
(all three browsers and fixed fonts), as four parallel legs: axe in Chromium,
Firefox and WebKit, and the visual baselines.

Browser notes:

- WebKit on macOS follows Safari's default of tabbing only to form controls,
  so the keyboard test presses Option+Tab there. On Linux WebKit, Tab reaches
  links as in the other engines.
- WebKit's text metrics make some guide code blocks overflow horizontally.
  Every guide `<pre>` is a focusable, uniquely named region so it can be
  scrolled from the keyboard in any browser (axe
  `scrollable-region-focusable`).

The site is checked by the conformance engine (`monarch check --root
apps/docs`, config in `apps/docs/monarch.config.json`). It deploys to GitHub
Pages on pushes to `main` once the repository variable `DOCS_PAGES=true` is
set (Settings, Pages, source: GitHub Actions).
