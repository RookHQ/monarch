# Native parity map

Status: plan. Only wave 0 is being implemented. An entry in the map does not
mean the component exists on Android or iOS.

Monarch for Android (Jetpack Compose, Material 3) and Monarch for iOS (SwiftUI)
are to reach full parity with the web catalogue. Parity means that every one of
the 515 documented web components and patterns has a defined answer on both
platforms. It does not mean 515 native components: a web component existing is
not a reason to build a native one ([NATIVE-PLATFORMS](NATIVE-PLATFORMS.md)).

The map is [`platforms/parity.json`](../../platforms/parity.json). It was
written from the docs files under `packages/react/src` (one `<id>.docs.tsx` per
component folder). What has actually shipped is recorded separately, in
`platforms/android/components.json` and `platforms/ios/components.json`.

## How to read an entry

```json
{
  "id": "range-slider",
  "family": "forms",
  "layer": "components",
  "status": "available",
  "title": "RangeSlider",
  "android": {
    "coverage": "style",
    "name": "MonarchTheme",
    "uses": "Material 3 RangeSlider",
    "note": "…"
  },
  "ios": {
    "coverage": "build",
    "name": "MonarchRangeSlider",
    "uses": "Custom two-thumb track …",
    "note": "…"
  },
  "wave": 1,
  "complexity": "M",
  "dependsOn": ["slider"]
}
```

- `id`, `family`, `title`: the web component, as on disk and in its docs file.
- `layer`, `status`: `components` or `patterns` (ADR 0017), and the shared
  status (`available`, `experimental`). Both platforms have the same value;
  `npm run native:status:check` fails when an inventory differs from the map.
- `android`, `ios`: how the platform covers it. The two may differ.
- `name`: the Monarch API, for `build` and `style`; otherwise `null`. Names are
  the same on both platforms where the coverage is the same. A `style` entry
  on Android that needs no code beyond the theme is named `MonarchTheme`.
- `uses`: the platform API the answer stands on. Exact for `native`.
- `note`: what changes from the web, and the reason for `web-only`.
- `wave`: build order. A component never depends on a later wave.
- `complexity`: `S` (under half a day, simple visual), `M`, `L`, `XL`,
  averaged over the two platforms.
- `dependsOn`: ids of other catalogue components this one is made from.
- `rook`: `true` for exports that are Rook-scoped in the web manifest.

Coverage types:

| Coverage   | Meaning                                                           | Class in NATIVE-PLATFORMS |
| ---------- | ----------------------------------------------------------------- | ------------------------- |
| `build`    | Monarch builds a component: custom, or a wrapper with its own API | B (wrapper) or C (custom) |
| `style`    | Monarch ships a style or theme layer on the platform control      | B (style)                 |
| `native`   | The platform component or API is the answer; Monarch documents it | A                         |
| `compose`  | A documented composition of other Monarch native components       | none: a recipe            |
| `web-only` | No meaning in a native app; the reason is in `note`               | none                      |

## Coverage per platform

| Coverage   | Android | iOS |
| ---------- | ------: | --: |
| `build`    |     362 | 354 |
| `style`    |      19 |  17 |
| `native`   |      45 |  59 |
| `compose`  |      76 |  72 |
| `web-only` |      13 |  13 |
| Total      |     515 | 515 |

Code that Monarch has to write (`build` plus `style`): 381 on Android and 371
on iOS. Documentation only (`native` plus `compose`): 121 on Android and 131 on
iOS. The two platforms have a different coverage type for 41 components, mostly
where one platform has a control the other lacks (a checkbox, chips and a range
slider on Android; a disclosure group, an outline list, Quick Look and Swift
Charts on iOS).

Rook-scoped entries: `sign-in-with-rook-button`, `rook-bar`, `rook-rail`.

## Waves

Waves order the work so that dependencies come first and the most common app
building blocks come early. A wave follows the web families, with a few
components brought forward because earlier waves need them (for example
`avatar`, `tag`, `dialog`, `sheet`, `dropdown-menu`, `search-input`,
`date-picker`), and a few moved back because they need later ones
(`phone-input`, `schema-driven-form`, `report-viewer`, `ai-settings-section`).

| Wave | Scope                                             | Components |   S |   M |   L |  XL |
| ---: | ------------------------------------------------- | ---------: | --: | --: | --: | --: |
|    0 | Foundation (in progress)                          |         12 |  10 |   2 |   0 |   0 |
|    1 | Core forms, actions, feedback, layout, typography |         83 |  63 |  20 |   0 |   0 |
|    2 | Navigation, overlays, data display, interaction   |         60 |  35 |  18 |   7 |   0 |
|    3 | Identity, search, files, workflow                 |         73 |  17 |  45 |  11 |   0 |
|    4 | Collaboration, scheduling, billing, technical     |         72 |  12 |  40 |  20 |   0 |
|    5 | Intelligence (AI)                                 |         91 |  24 |  46 |  21 |   0 |
|    6 | Data grid, charts, geo, editor and other XL items |         57 |   9 |  21 |  13 |  14 |
|    7 | Patterns and compositions                         |         67 |  21 |  24 |  22 |   0 |
|      | Total                                             |        515 | 191 | 216 |  94 |  14 |

Families in each wave (number of components in brackets):

- Wave 0: actions (1), data-display (3), feedback (2), forms (4), layout (1),
  typography (1).
- Wave 1: actions (8), data-display (2), feedback (15), forms (28), identity
  (2), interaction (4), layout (10), navigation (1), overlays (4), scheduling
  (2), search (1), typography (6).
- Wave 2: charts (2), data-display (21), data-grid (1), interaction (3), layout
  (2), navigation (13), overlays (15), scheduling (1), search (1), workflow
  (1).
- Wave 3: data-grid (1), files (16), forms (1), identity (29), interaction (1),
  search (7), workflow (18).
- Wave 4: billing (12), collaboration (32), data-display (2), layout (1),
  scheduling (10), technical (15).
- Wave 5: intelligence (91).
- Wave 6: charts (16), collaboration (1), data-grid (14), files (8), forms (1),
  geo (8), scheduling (4), workflow (5).
- Wave 7: data-display (1), intelligence (1), patterns/actions (1),
  patterns/collaboration (2), patterns/crm (1), patterns/data-grid (2),
  patterns/identity (17), patterns/intelligence (4), patterns/layout (26),
  patterns/navigation (2), patterns/overlays (1), patterns/scheduling (1),
  patterns/search (1), patterns/technical (3), patterns/workflow (3), technical
  (1).

Coverage per wave:

| Wave | Android build | style | native | compose | web-only | iOS build | style | native | compose | web-only |
| ---: | ------------: | ----: | -----: | ------: | -------: | --------: | ----: | -----: | ------: | -------: |
|    0 |             8 |     4 |      0 |       0 |        0 |         6 |     4 |      2 |       0 |        0 |
|    1 |            47 |    13 |     19 |       4 |        0 |        47 |     9 |     23 |       4 |        0 |
|    2 |            37 |     1 |     14 |       6 |        2 |        33 |     3 |     17 |       5 |        2 |
|    3 |            64 |     0 |      1 |       7 |        1 |        61 |     0 |      5 |       6 |        1 |
|    4 |            64 |     0 |      3 |       5 |        0 |        65 |     0 |      2 |       5 |        0 |
|    5 |            89 |     1 |      1 |       0 |        0 |        90 |     0 |      1 |       0 |        0 |
|    6 |            52 |     0 |      2 |       3 |        0 |        52 |     0 |      4 |       1 |        0 |
|    7 |             1 |     0 |      5 |      51 |       10 |         0 |     1 |      5 |      51 |       10 |

## Web-only components

13 components have no native answer on either platform. `web-only` is used only
where a native equivalent makes no sense, not to reduce the work.

- `chat-dock` (collaboration): Embeds the Rook Chat web app in an iframe that
  stays alive behind a floating launcher. A native app opens the Rook Chat app
  or builds its screens from the collaboration components.
- `cookie-consent-banner` (identity): Native apps store no cookies and have no
  page for a banner to cover. First-run consent uses consent-preferences, and
  on iOS the system tracking prompt.
- `careers-job-list` (layout): A section of a public careers website.
  Recruiting pages are web content and are not part of a product app.
- `comparison-section` (layout): A marketing section of a public web page
  comparing the product with alternatives. Store listings, not app screens,
  carry this content.
- `logo-cloud` (layout): A marketing section of customer logos for public web
  pages. No product app screen shows it.
- `marketing-hero` (layout): The opening section of a public web page, built on
  the page h1 and fluid display type that native does not generate. Store
  listings and onboarding screens take its place.
- `marketing-section-blocks` (layout): Body sections of public marketing pages
  (feature grid, stat strip, CTA band, FAQ). Marketing pages are web content.
- `marketing-site-shell` (layout): Header and footer of a public website with
  link columns. A native app has no site chrome.
- `pricing-section` (layout): A marketing band for public web pages. In-app
  plan choice uses plan-card and pricing-table under the store's billing rules.
- `rook-bar` (layout): The Rook company bar on each app's public web pages,
  with hover panels. Native apps have no public pages; Rook-scoped.
- `mega-menu` (navigation): Site navigation for wide public web pages with
  hover panels. A native app's top-level navigation is the tab bar, rail or
  drawer.
- `skip-link` (navigation): Exists to bypass repeated links in a page's tab
  order. TalkBack and VoiceOver move by heading and container, and native
  screens have no repeated page chrome to skip.
- `embedded-app-frame` (technical): An iframe with a postMessage handshake
  between Rook web apps (App Settings embed contract). The contract is a
  browser one; a native app links to the other app or builds the screen
  natively.

## XL items

14 components are XL. Each needs its own design note before work starts.

- `bar-chart` (charts, wave 6): Android Compose Canvas (custom). iOS Swift
  Charts: Chart + BarMark.
- `force-graph` (charts, wave 6): Android Compose Canvas + custom force
  simulation, transformable for zoom and pan. iOS SwiftUI Canvas + custom force
  simulation, MagnifyGesture and DragGesture.
- `line-chart` (charts, wave 6): Android Compose Canvas (custom chart core:
  scales, axes, ticks, touch inspection). iOS Swift Charts: Chart + LineMark +
  RuleMark.
- `rich-text-editor` (collaboration, wave 6): Android BasicTextField with
  TextFieldState and AnnotatedString spans (custom editing model). iOS
  TextEditor bound to AttributedString on iOS 26; UITextView with TextKit 2
  below that.
- `data-grid` (data-grid, wave 6): Android Custom LazyColumn with a shared
  horizontal scroll state, pinned columns and cell focus. iOS Custom Grid in
  two-axis ScrollView with pinned headers; SwiftUI.Table does not edit cells.
- `block-editor` (files, wave 6): Android LazyColumn of block composables, each
  with its own text state. iOS List of block views, each with its own text
  state.
- `world-map` (geo, wave 6): Android Compose Canvas with bundled simplified
  country paths. iOS SwiftUI Canvas with bundled simplified country paths.
- `calendar-month-view` (scheduling, wave 6): Android Custom month grid with
  event layout, HorizontalPager between months. iOS Custom Grid month with
  event layout, paged ScrollView between months.
- `shift-roster` (scheduling, wave 6): Android Two-axis scrolling grid with
  drag to assign. iOS Two-axis scrolling grid with drag to assign.
- `team-calendar` (scheduling, wave 6): Android Two-axis scrolling grid with
  spanning bars. iOS Two-axis scrolling grid with spanning bars.
- `time-grid` (scheduling, wave 6): Android Custom Layout with overlap columns,
  now line and drag to create or move. iOS Custom Layout with overlap columns,
  now line and drag to create or move.
- `gantt-timeline` (workflow, wave 6): Android Compose Canvas and custom Layout
  with two-axis scroll, drag and resize. iOS Canvas and custom Layout with
  two-axis scroll, drag and resize.
- `org-chart` (workflow, wave 6): Android Custom tree Layout with transformable
  zoom and pan. iOS Custom tree Layout with MagnifyGesture and scroll.
- `roadmap` (workflow, wave 6): Android Timeline Layout on the gantt core. iOS
  Timeline Layout on the gantt core.

## Rules for changing the map

- The map is the plan, and it is the one source the status check reads.
  `status`, `layer`, and each platform's `coverage` and `name` are written
  from the two inventories by `node scripts/native/status-check.mjs
--write-parity` (then format with Prettier); the check fails when an
  inventory entry and the map disagree. `uses`, `note`, `wave`, `complexity`
  and `dependsOn` are edited by hand, except that a `compose` entry gains the
  `dependsOn` of its recipes in either inventory. Shipping a component adds it
  to the platform's `components.json` and to the changelog.
- Every web component has exactly one entry with both platforms. A new
  `<id>.docs.tsx` on the web needs a new entry in the same pull request; a
  removed one removes its entry.
- Changing a coverage type needs a reason in the pull request. Moving an entry
  to `web-only` needs a one-line justification in `note` and a maintainer's
  approval, and is never done to shorten a wave.
- `native` entries name the exact platform API in `uses`. If the API needs a
  newer OS than [NATIVE-COMPATIBILITY](NATIVE-COMPATIBILITY.md) supports, the
  entry becomes `build` or says so in `note`.
- `compose` entries list every part in `dependsOn`. If a recipe grows its own
  state or API, it becomes `build`.
- `dependsOn` only names ids in the map, and never an id in a later wave.
  Bring the dependency forward or move the dependant back.
- Names follow `Monarch` plus the id in PascalCase, the same on both
  platforms. A different name needs a reason (`MonarchBanner` for `alert` and
  `MonarchDivider` for `separator` already shipped in wave 0).
- Complexity is an estimate. Correct it when a component is designed, not when
  it is finished.
- The file is formatted by Prettier and sorted by family, then id.

Checks:

```sh
find packages/react/src -name '*.docs.tsx' | wc -l
jq '.components | length' platforms/parity.json
jq '[.components[].id] | group_by(.) | map(select(length > 1))' platforms/parity.json
jq '[.components[].id] as $ids | [.components[] | .dependsOn[] | select(IN($ids[]) | not)]' platforms/parity.json
```

The first two numbers must match and the last two must print `[]`.
