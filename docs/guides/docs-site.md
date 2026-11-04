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

- Guides rendered from `docs/guides/*.md` at build time (no copy to drift).
- Tokens: live swatches with contrast ratios for the current theme, contrast
  and product accent; brand colours; type, spacing, radius, elevation, motion.
- Components: one page per component folder at `#/components/<id>`, rendered
  from its `<id>.docs.tsx` ([docs-file contract](../architecture/SOURCE-LAYOUT.md#docs-file-contract));
  API and props tables come from `@rookcom/monarch-react/manifest.json`,
  examples are live and copyable.
- Products: the brand accent and colour schemes each Rook product declares in `registry/products`.
- `#/__gallery`: every example on one page, no chrome; the screenshot surface.

Header controls set the shared `rook_theme` cookie, the `mn_contrast` cookie
and `data-mn-product`, so any example can be viewed as any Rook product.

## Tests

| Command                      | Project                                        | What                                                                                                        |
| ---------------------------- | ---------------------------------------------- | ----------------------------------------------------------------------------------------------------------- |
| `npm run test:a11y`          | `a11y-chromium`, `a11y-firefox`, `a11y-webkit` | axe-core on 15 routes × light/dark, keyboard flow, theme/product controls, in all three engines             |
| `npm run test:visual`        | `visual`                                       | per-component screenshots (see [visual testing](visual-testing.md)); Linux only, skipped on other platforms |
| `npm run test:visual:update` |                                                | regenerate snapshots after an intended visual change                                                        |

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
