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
- iOS and Android (each a section of the top bar): Monarch for iOS and Monarch
  for Android, from the native packages under `platforms/`
  ([Native platform pages](#native-platform-pages)). The native guides they
  share (`/native/<guide>/`) are listed in both sidebars under one address.
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
  plugins/          monarch-docs.ts (Vite plugin), extract-examples.ts (+ test), guides.ts (Markdown at build time),
                    native.ts (+ test): the Android and iOS data
  public/           icons, web manifest (copied as is)
  scripts/          prerender.mjs (static HTML for every page, 404, sitemap, robots)
  src/
    main.tsx        client entry: hydrates a prerendered page, or renders from scratch
    entry-server.tsx  server entry for the prerender step
    fonts/          Poppins woff2 subsets and their licence (OFL.txt)
    app/            app.tsx (providers, hidden routes), routes.tsx (lazy pages), navigation.ts
    layout/         docs-shell, top-bar, sidebar, page-frame, toc, site-footer
    features/       appearance/, search/, code/ (code block, highlighter), examples/, brand/, content/, native/
    pages/          home/, foundations/, components/, guides/, android/, ios/, native/, gallery.tsx (+ gallery-ids.ts)
    lib/            docs.ts (index + lazy docs files), router, appearance, manifest, registry, facts
    marketing/      the #/__marketing reference page
    styles/         fonts, base, gallery (baseline surface), shell, content, examples, home, foundations, component-page, native
```

## Native platform pages

Everything the site says about Android and iOS is read at build time from the
files the native packages own, by `plugins/native.ts`. Nothing is copied into
the site, and no count or version is typed in.

| Address                                      | What it shows                                                                                                                                                                     | Read from                                                                                                           |
| -------------------------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------- |
| `/android/`, `/ios/`                         | The landing page of each platform: one layout (`features/native/platform-landing.tsx`), the words per platform in `platform-landing-content.ts`, screenshots of the catalogue app | Counts from the inventory, the install snippet from the README, compiled inventory examples, `public/native/*.webp` |
| `/android/get-started/`, `/ios/get-started/` | Requirements, installation, the first screen, the theme and the tokens                                                                                                            | Sections of `platforms/<platform>/README.md`, the version catalog, `Package.swift`, the changelog                   |
| `/android/guide/`, `/ios/guide/`             | The long form: approach, previews, examples, accessibility, adaptive layouts, reference, API policy, platform terms                                                               | The same, and the inventory entries with no web page (theme, icon)                                                  |
| `/android/components/`, `/ios/components/`   | The catalogue: every inventory entry and every planned component, by family                                                                                                       | `platforms/<platform>/inventory/*.json`, `platforms/parity.json`                                                    |
| `/android/releases/`, `/ios/releases/`       | Release notes                                                                                                                                                                     | `platforms/<platform>/CHANGELOG.md`                                                                                 |
| `/native/<guide>/`                           | `adaptive-layout`, `accessibility`, `compatibility`, `contributing`                                                                                                               | `docs/guides/native-*.md`, `docs/architecture/NATIVE-COMPATIBILITY.md`                                              |
| `/components/<id>/#android`, `#ios`          | The component on each platform, as two sections of its existing page (no second page per platform)                                                                                | The inventory entry of that id, or its plan in `platforms/parity.json` when there is no entry yet                   |

The landing pages' pictures are drawn by the sample apps from one scenario (the
demo workspace Acme Studio): the showcase screen on a phone and a tablet, eight
category tiles and three small cards beside the hero phone, light and dark: `apps/docs/scripts/capture-native-screens.sh
[ios|android|all]` captures them (the iOS Simulator with the status bar set to
9:41; a Robolectric test, `LandingScreensTest`, on Android) and writes WebP
files of at most 150 KB to `apps/docs/public/native/`, with their sizes in
`src/features/native/native-screens.json`. The sections of the old one-page
`/ios/` and `/android/` moved to `get-started` and `guide` under the same ids;
an old link such as `/ios/#installation` is sent there by the landing page
(`src/features/native/moved-sections.ts`).

Rules the pages follow:

- **Status is never guessed.** A component with an inventory entry shows the
  entry's status (available, experimental, deprecated). A component that is
  only in `platforms/parity.json` is shown as planned, with the planned
  coverage, and never as available. Web-only coverage shows no status.
- **Text about installing and using a platform is the README's own.** The
  landing pages embed README sections by heading (`README_SECTIONS` in
  `plugins/native.ts`), so a change to the README changes the page. A heading
  that is renamed is left out and a link to the README stands in; add the new
  slug to `README_SECTIONS`. Commands quoted outside those sections
  (`README_COMMANDS`) are shown only while the README still contains them.
- **Missing data never fails the build.** No inventory folder (the generated
  `components.json` is read instead, if it is in the same shape), no
  changelog, no guide file: the pages say less. A guide whose file does not
  exist has no route, no navigation entry and no links to it. An inventory
  file that is not in the shape of `inventory/schema.json` is skipped with a
  build warning.
- **Previews are images of the real components or nothing.** When
  `platforms/<platform>/previews/<id>.light.png` or `<id>.dark.png` exists,
  the entry shows it (the one matching the site theme, with its width and
  height, loaded lazily). Without a file nothing is drawn in its place.
- **Per-component data loads per component.** Each `<id>.docs.tsx` gets a
  `loadNative` export that imports `virtual:monarch-native-component/<id>`, a
  chunk of its own with both platforms' entries. Only the component's page
  loads it (`loadNative()` in `src/features/native/native-sections.tsx`); overview cards, the gallery
  and every other page do not. The sections are in the prerendered HTML, but
  their code and data are not preloaded. In the browser the page keeps that
  HTML exactly as it is (`NativeSlot` in `src/pages/components/native-slot.tsx`
  renders the same HTML as the element's content, so React hydrates around it)
  until the sections near the viewport; then it loads the code and the data
  and replaces the static sections with live ones, with the same markup, in
  one commit. They do not count towards the page's initial JavaScript, and a
  slow connection never holds up the rest of the page.
- Kotlin and Swift code is highlighted by a small highlighter beside the TSX
  one (`src/features/code/highlight-native.ts`), which only the native pages
  load: in the browser for inventory examples, at build time for guides and
  README sections. Code blocks carry their language (`data-lang`), which the
  Markdown copies use for the fence.
- The iOS and Android pages add as little as possible to the entry chunk, which
  every page loads: their paths and labels (`src/app/navigation.ts`) and
  their page modules, found by naming convention (`platformPages` in
  `src/app/routes.tsx`). Descriptions
  and search keywords are in `src/app/native-nav-details.ts`, read by the
  search index and the prerender step.

To add a native guide, add its file to `NATIVE_GUIDES` (`plugins/native.ts`)
and `GUIDE_ROUTES` (`plugins/guides.ts`), a page module in `src/pages/native`,
named `<id>-page.tsx` and exporting `<Id>Page`, and its entry in
`NATIVE_GUIDE_ITEMS` (`src/app/navigation.ts`), `NATIVE_NAV_DETAILS`
(`src/app/native-nav-details.ts`) and `nativeGuideRoutes()`
(`tests/helpers.ts`).

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

| Check                                                         | Budget (gzip) |
| ------------------------------------------------------------- | ------------- |
| Home page: JavaScript loaded before any interaction           | 150 KB        |
| Home page: CSS                                                | 60 KB         |
| Any other page: JavaScript loaded before any interaction      | 290 KB        |
| `/components/block-editor` (composes every editor block)      | 336 KB        |
| `/components/{rich-text-editor,message-composer,table-block}` | 300 KB        |
| Any other page: CSS                                           | 60 KB         |
| The entry chunk                                               | 135 KB        |
| The Tiptap and ProseMirror chunk (editor pages only)          | 140 KB        |
| Any other chunk                                               | 125 KB        |

The editor limits were raised in 0.14, when the editor moved to Tiptap 3 for a
security fix ([ADR 0015](../adr/0015-tiptap-3.md)); Tiptap 3 is about 24 KB
gzip larger than Tiptap 2.

"Loaded before any interaction" is what the page's prerendered HTML loads:
the entry script and every `modulepreload` and stylesheet link. The budget
also fails when a page loads, before interaction, code it must not: another
page's docs file, the search palette, a lazily opened panel or the Markdown
parser on any page; the editor, charts or the command palette on any page
that is not their own component page. It checks the sources listed in each
chunk's source map, so a module cannot slip in under another chunk's name.
Nothing on the home page grows with the catalog: page details (loaded only by
the overview; component pages take their exports from their own docs chunk)
and the map of docs-file loaders load lazily, and pages load `styles/core.css` plus the
per-component sheet (`styles/components/<id>.css`) of every subpath component
they import, through the `monarch-entry-styles` Vite plugin, so CSS no longer
grows with the catalog.
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
