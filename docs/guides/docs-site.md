# Docs site and visual regression

`apps/docs` is the Monarch documentation and playground. It is a normal
consumer of the published packages (Vite + React, imports from the built
`dist` entry points) so it doubles as the integration test for them.

## Run

```bash
npm run build          # packages first; the site imports their dist
npm run build:docs     # apps/docs/dist: client build, server build, prerender
npm run docs:budget    # performance budget (add --report for every page and chunk)
npm run docs:links     # internal link check over the prerendered pages
npm run dev -w @rookcom/monarch-docs   # http://localhost:5173
```

## What is on it

- Home: the hero from the Monarch launch page, drawn in tokens, and live previews.
- Foundations: introduction, colours (live swatches with contrast ratios for the
  current theme, contrast and product accent), typography, tokens, theming.
- Components and patterns: an overview grid with a live thumbnail per docs file,
  and one page per component folder at `/components/<id>/`, rendered from its
  `<id>.docs.tsx` ([docs-file contract](../architecture/SOURCE-LAYOUT.md#docs-file-contract)).
  The first example is shown large; every example has "Show code"; API and props
  tables come from `@rookcom/monarch-react/manifest.json`.
- Guides rendered from `docs/guides/*.md` at build time (no copy to drift).
- Search (⌘K / Ctrl K): Monarch's `CommandPalette` over every page.
- `#/__gallery`, `#/__gallery/<id>` and `#/__marketing`: no chrome; the screenshot surfaces.
  They stay in the fragment of the site root and render in the browser only.

The top bar's theme switcher sets the shared `rook_theme` cookie; the
appearance popover also sets `mn_contrast` and the previewed `data-mn-product`,
so any example can be viewed as any Rook product. Pre-paint scripts apply all
three before first paint (`system` theme and contrast by default).

## Source layout

```
apps/docs/
  plugins/          monarch-docs.ts (Vite plugin), extract-examples.ts (+ test), guides.ts (Markdown at build time)
  public/           icons, web manifest (copied as is)
  scripts/          prerender.mjs (static HTML for every page, 404, sitemap, robots)
  src/
    main.tsx        client entry: hydrates a prerendered page, or renders from scratch
    entry-server.tsx  server entry for the prerender step
    fonts/          Poppins woff2 subsets and their licence (OFL.txt)
    app/            app.tsx (providers, hidden routes), routes.tsx (lazy pages), navigation.ts
    layout/         docs-shell, top-bar, sidebar, page-frame, toc, site-footer
    features/       appearance/, search/, code/ (code block, highlighter), examples/, brand/, content/
    pages/          home/, foundations/, components/, guides/, gallery.tsx (+ gallery-ids.ts)
    lib/            docs.ts (index + lazy docs files), router, appearance, manifest, registry, facts
    marketing/      the #/__marketing reference page
    styles/         fonts, base, gallery (baseline surface), shell, content, examples, home, foundations, component-page
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

## Addresses and prerendering

Every page has a real address under the base path (`DOCS_BASE`, `/` locally,
`/monarch/` on GitHub Pages): `/components/button/`, with in-page anchors as
ordinary fragments (`/components/button/#keyboard`). `npm run build:docs`
runs three steps:

1. `vite build`: the client bundle, with a build manifest (`dist/.vite`).
2. `vite build --ssr src/entry-server.tsx`: the same app for Node.
3. `scripts/prerender.mjs`: renders every route with `react-dom/static` to
   `dist/<route>/index.html`, with its title, description, canonical link and
   preload links for exactly the chunks and styles that page needs. It also
   writes `404.html` (with `noindex`), the renamed addresses in
   `ROUTE_ALIASES`, `sitemap.xml` and `robots.txt`.

`DOCS_SITE_URL` is the public address used for canonical links and the
sitemap (default `https://monarchui.dev` + the base path; CI sets the
GitHub Pages address).

In the browser, `main.tsx` hydrates the prerendered page when it is the one
being opened (`#root` carries `data-route`), so the HTML on screen becomes
interactive without being rendered again. Anything else renders from
scratch: the screenshot routes, and old hash-router links such as
`#/components/button#keyboard`, which are first replaced with the page's real
address. The headline example of each component page is part of the HTML,
so the top of the page never shifts when it becomes interactive; the other
live previews mount in the browser when they near the viewport
(`use-near-viewport.ts`). The first client render uses the default appearance and
reads the stored one in a layout effect, before paint; the pre-paint script
has already applied it to `<html>`.

Rules for page code that keep hydration exact:

- render nothing that depends on `window`, `document`, cookies, storage, the
  date or the platform; read those in an effect (or a layout effect when the
  first paint must already show them, as the search shortcut does);
- examples must be deterministic (they already are for the visual
  baselines): no current date, random value or default locale in `render`;
- render live examples only through `ExampleCard` or `ComponentCard`;
- `npm run test:a11y` scans every page in a far-away time zone and another
  locale, and fails on any console error or warning, which is how a
  hydration mismatch shows up.

## Performance

- **Fonts.** Poppins is self-hosted (`src/fonts`, `src/styles/fonts.css`): the
  upstream latin and latin-ext subsets of 400, 500, 600 and 700 as `woff2`,
  `font-display: swap`, split by `unicode-range`. The latin 400 and 600 files
  are preloaded. No request leaves the site.
- **JavaScript.** Every page is its own chunk, and each component page loads
  only its own docs file. The search palette, the appearance panel and the
  navigation drawer load on first use (and start loading on hover or focus of
  their button). Guides are rendered to HTML at build time, so the Markdown
  parser never ships. Each component page carries the manifest entries of its
  own exports instead of the whole manifest. Hovering or focusing a link to
  another page starts loading that page's chunk and docs file.
- **Rendering.** Apart from each component page's headline example, previews
  on the overview, home and component pages mount near the viewport into
  frames of reserved height; overview families far
  from the viewport use `content-visibility: auto`. Hero animations use only
  `transform` and `opacity`.

### Performance budget

`npm run docs:budget` (`scripts/docs/check-performance-budget.mjs`) reads the
production build and fails when:

| Check                                                    | Budget (gzip) |
| -------------------------------------------------------- | ------------- |
| Home page: JavaScript loaded before any interaction      | 156 KB        |
| Home page: CSS                                           | 64 KB         |
| Any other page: JavaScript loaded before any interaction | 290 KB        |
| Any other page: CSS                                      | 60 KB         |
| The entry chunk                                          | 135 KB        |
| Any other chunk                                          | 125 KB        |

"Loaded before any interaction" is what the page's prerendered HTML loads:
the entry script and every `modulepreload` and stylesheet link. The budget
also fails when a page loads, before interaction, code it must not: another
page's docs file, the search palette, a lazily opened panel or the Markdown
parser on any page; the editor, charts or the command palette on any page
that is not their own component page. It checks the sources listed in each
chunk's source map, so a module cannot slip in under another chunk's name.
The home-page limits were raised from 150 and 60 KB in wave 10 (28 new
components). Two things grow with the catalog on every page: the eager page
index (`virtual:monarch-docs-index`, about 70 bytes gzip per component) and
the single component stylesheet. On the way to 500 components the index must
load lazily and the stylesheet split per entry, rather than raising the limits
again.
`--report` prints every page and the 25 largest chunks with gzip and brotli
sizes. Raise a limit only in a reviewed PR that says why.

### Link check

`npm run docs:links` (`scripts/docs/check-links.mjs`) checks every link and
asset reference in the prerendered pages that stays on the site: the page or
file must exist and a `#fragment` must name an element on that page.

### Hosting

The site is static files. Recommended headers for any host:

| Path                                    | `Cache-Control`                       |
| --------------------------------------- | ------------------------------------- |
| `assets/*` (content-hashed file names)  | `public, max-age=31536000, immutable` |
| HTML pages, `sitemap.xml`, `robots.txt` | `public, max-age=0, must-revalidate`  |
| icons and `site.webmanifest`            | `public, max-age=86400`               |

Serve text files with brotli or gzip, `404.html` for unknown addresses (with
status 404), and `<route>/index.html` for `<route>/`. `robots.txt` and the
sitemap only work for search engines at the root of an origin, so a sub-path
deployment such as GitHub Pages at `/monarch/` needs them at the origin root
too.

### Search, answers and AI crawlers

Every page ships what a search engine, an answer engine and an AI assistant
need, built into the HTML (`scripts/prerender.mjs`, `scripts/seo.mjs`):

- Title, description, canonical link, `robots` with large previews, and Open
  Graph / Twitter tags with the share card (`public/og.png`, 1200×630; redo it
  with `npm run docs:og`).
- JSON-LD: `WebSite`, `Organization` and `SoftwareSourceCode` on the home page,
  `TechArticle` and `BreadcrumbList` on every other page. All values are
  counted from the site's own data.
- The page body is in the HTML itself. React's streaming output (a placeholder
  plus a hidden block moved by a script) is resolved at build time, so
  crawlers that do not run scripts read the content, and people do not wait for
  the swap.
- A Markdown copy of every page (`<route>/index.md`, including all example
  code), `llms.txt` (an index) and `llms-full.txt` (everything in one file).
  On the server, `Accept: text/markdown` at a normal address returns the
  Markdown.
- `robots.txt` that names the search and AI crawlers, a sitemap with `lastmod`
  dates, and `/.well-known/security.txt` (renew its `Expires` date yearly).

`npm run docs:seo` fails the build if any page lacks one of these.

## Tests

| Command                      | Project                                        | What                                                                                                                                                                                                                                    |
| ---------------------------- | ---------------------------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `npm run test:a11y`          | `a11y-chromium`, `a11y-firefox`, `a11y-webkit` | axe-core on every route (every component page included) × light/dark with no console errors or warnings, prerendering and hydration, old hash addresses, search, Show code, keyboard flow, theme/product controls, in all three engines |
| `npm run test:visual`        | `visual`                                       | per-component screenshots (see [visual testing](visual-testing.md)); Linux only, skipped on other platforms                                                                                                                             |
| `npm run test:visual:update` |                                                | regenerate snapshots after an intended visual change                                                                                                                                                                                    |

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
