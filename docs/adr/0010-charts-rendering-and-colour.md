# ADR 0010: Charts are hand-written SVG on a tokenised, contrast-checked palette

Status: accepted (2026-11-04), wave 5 group A (ChartFrame, LineChart, BarChart, Sparkline)

## Context

About ten Rook products draw charts today. Almost all of them hand-roll SVG;
only one uses a charting library. The recurring gaps are the same everywhere:
hover-only tooltips, no keyboard access, no data-table alternative,
hard-coded hex colours that skip dark mode, and a different "nice ticks" helper
in each product.

Monarch needs charts that are accessible by default, follow every theme,
render on the server and stay small, because `./charts` is loaded by
dashboards in every product.

## Decision

### Rendering: hand-written SVG, no dependency

Charts are plain React + SVG. The shared internals live in
`packages/react/src/internal/charts/` (import through its `index.ts`):

| Module                 | Job                                                                                            |
| ---------------------- | ---------------------------------------------------------------------------------------------- |
| `scale.ts`             | linear scale, nice 1/2/2.5/5 ticks, extent, label thinning, SSR-safe text width estimate       |
| `path.ts`              | line and area paths that break at gaps, monotone cubic (Fritsch–Carlson), far-end-rounded bars |
| `layout.ts`            | value-axis layout, tick count per height, nearest-index search, interaction labels             |
| `format.ts`            | cached `Intl.NumberFormat` / `Intl.DateTimeFormat` with a `locale` prop                        |
| `palette.ts`           | `ChartColor` slots → `--mn-color-data-*` tokens                                                |
| `summary.ts`           | text summaries (points, low / high / latest, totals), translatable                             |
| `use-chart-pointer.ts` | one model for hover, touch and keyboard inspection                                             |
| `use-element-width.ts` | container width via ResizeObserver, undefined on the server                                    |
| `parts.tsx`            | legend, tooltip, live region, value and category axes, reference line, data table              |

Their styles are `styles/chart.css` (`.mn-chart__*`), shared by every chart.

Why not a dependency:

- **Evidence.** Products already draw these shapes by hand; none needs
  brushing, zooming, geo projections or force layouts. The chart types in
  wave 5 are lines, areas, bars, sparklines, donuts and bar lists.
- **Bundle.** recharts is on the order of 100 kB gzipped with its d3 sub-packages;
  visx and nivo are similar or larger. Wave 5 group A (four components plus
  the internals) is about 16 kB gzipped before minification, and it ships in
  a subpath that only dashboards load.
- **SSR.** Responsive containers in chart libraries render nothing (or a
  0×0 box) on the server and measure on mount. Our charts render real SVG on
  the server at a 640px fallback width with a `viewBox`, so they paint before
  hydration, then re-lay out at the measured width. Server and first client
  render are identical, so hydration matches.
- **Accessibility.** Libraries give SVG and leave semantics to the caller.
  We need one tab stop with arrow-key inspection, a live region, a data table
  and generated summaries, which is easier to own than to retrofit.
- **Theming.** Every mark is coloured by a CSS variable (`--_c` set to a data
  token), so themes, high contrast and product accents need no JavaScript.

d3-scale and d3-shape were considered as small helpers. We need one linear
scale, one tick algorithm and two curve types (about 150 lines in total), so
the dependency is not worth its update and audit cost. If a later chart needs
band, log or time-interval ticks beyond this, revisit `d3-scale` alone.

### Colour: the data tokens, contrast-checked per theme

Series colours are the five categorical tokens `--mn-color-data-1` … `-5`
(blue, orange, violet, pink, purple). Products pick a slot (`ChartColor` =
`1`–`5` or `neutral`), never a colour value. Status hues (red, green, amber)
are not in the palette: danger, success and warning are never "series 4".
Slots are assigned in order and never cycled. A sixth series and later get
`neutral`, because the right fix is to fold into "Other" or split the chart.

The tokens existed in 0.3 (one spectrum step per scheme) but were not
checked. In dark mode, pink and purple sat 11.3 ΔE apart under normal vision
(hard to tell apart), four of the five were outside the recommended dark
lightness band, and high-contrast themes used the standard values. 0.4 changes
how they are derived (`dataPalette()` in `packages/tokens/src/css.ts`):

1. Each slot starts at a chosen step per scheme: light 600 for every hue;
   dark blue 500, orange 700, violet 400, pink 500, purple 500.
2. Each is then darkened (light) or lightened (dark) only as far as needed to
   meet the theme's non-text target on every chart background
   (`bg-canvas`, `bg-surface`, `bg-raised`, `bg-subtle`): 3:1 in standard
   themes and 4.5:1 in high contrast (WCAG 1.4.11, stricter for HC).

`packages/tokens/test/contrast.test.ts` proves, for all four themes: five
slots, no status hue, the contrast target on every background, and adjacent
slots at least ΔE 15 apart under normal vision and ΔE 8 under simulated
protanopia and deuteranopia (Machado et al. 2009, OKLab ×100). The external
dataviz palette validator agrees (light worst adjacent pair: protan 14.6, normal
19.8; dark: protan 12.8, normal 16.1; all checks pass in all four themes).

Visible changes: light orange moves from `#f26507` to `#f06407` (just enough
for 3:1 on the canvas); dark blue, orange, pink and purple are one or more
steps deeper; high-contrast light darkens blue, orange and pink, and high-contrast dark lightens purple, to
reach 4.5:1. The Avatar `palette="tokens"` tints and ProgressBar data segments
use these tokens, so their dark-theme screenshots change slightly. Avatar text
on its tint stays at 8.4:1 or more.

Charts add a second encoding wherever identity matters: every legend entry has
a text label, `dashed` series draw dashed lines or outlined bars, and the
data table is always available.

### Interaction and accessibility

- The plot is one tab stop (`role="group"`, `aria-roledescription="chart"`),
  named by a generated summary (or `summary`) and described by a keyboard hint.
- Arrow keys, Home/End and PageUp/PageDown move the inspected point; Escape
  hides it; Enter/Space trigger drill-in. Pointer and touch drive the same
  index, so the tooltip and crosshair are identical for every input.
- The tooltip is visual (`aria-hidden`). Keyboard inspection is announced
  through a polite live region; pointer hovering is not announced.
- The SVG is `aria-hidden`; ChartFrame always renders the data table for
  screen readers and shows it with "View as table".
- Reduced motion: the only motion is the tooltip fade and bar dimming, which
  use duration tokens that become 0ms.
- Responsive by container width (ResizeObserver), tested down to 320px; x labels
  thin out and horizontal-bar labels truncate.
- No `window` or `document` access during render.

### Deliberately out

- **Dual axis.** Two scales on one chart mislead; LineChart's
  `activeIndex` / `onActiveIndexChange` sync the crosshair across separate
  charts instead.
- **Stacked areas, log scales, brushing and zoom.** No product evidence yet.

## Consequences

- Wave 5 group B (bar-list, donut-chart, stacked-proportion-bar,
  delta-indicator, stat-card) builds on ChartFrame and the same internals and
  palette.
- Charts own their maths. Bugs in ticks or paths are ours to fix, and these
  modules are covered through the component tests.
- Dark data colours are deeper than in 0.3. Products that hard-coded the old
  values should move to the tokens.
