# Theming

## Selectors

| Theme               | Selector on `<html>`                                              |
| ------------------- | ----------------------------------------------------------------- |
| light               | default (`:root`) or `data-mn-theme="light"`                      |
| dark                | `.dark` or `data-mn-theme="dark"`                                 |
| high-contrast light | `data-mn-contrast="more"`                                         |
| high-contrast dark  | `.dark` + `data-mn-contrast="more"`                               |
| product accent      | `data-mn-product="<id>"` (any element; nest to re-theme a region) |
| neutral accent      | `data-mn-product="neutral"` (with `css/products/neutral.css`)     |

## Token groups

- Surfaces: `--mn-color-bg-{canvas,surface,raised,subtle,muted,emphasis,inverse}`
- Text: `--mn-color-text-{default,muted,subtle,disabled,inverse,link}`
- Borders: `--mn-color-border-{subtle,default,strong,input,inverse}`
- Intents `accent | danger | success | warning | info | ai`, each with
  `solid, solid-hover, solid-active, on-solid, subtle, subtle-hover, strong, on-strong, border, strong-inverse`
- AI roles: `--mn-color-ai-{thinking,tool,citation,diff-add,diff-add-strong,diff-remove,diff-remove-strong}`,
  `--mn-color-confidence-{low,medium,high}`, `--mn-gradient-ai`
- `--mn-color-focus-ring`, `--mn-focus-ring-width`, `--mn-color-overlay-scrim`
- Data: `--mn-color-data-1..5`
- Type: `--mn-font-{sans,mono}`, `--mn-text-*`, `--mn-leading-*`, `--mn-weight-*`, `--mn-tracking-*`
- Layout: `--mn-space-*`, `--mn-radius-*`, `--mn-control-*`, `--mn-layout-*`, `--mn-z-*`, `--mn-breakpoint-*`
- Motion: `--mn-duration-*` (0ms under reduced motion), `--mn-ease-*`
- Elevation: `--mn-shadow-{xs,sm,md,lg,xl}`

Rule of thumb: `solid` for fills with `on-solid` text; `strong` for text,
icons and rings on surfaces; `subtle` for tinted backgrounds with `strong`
text; `border` for intent outlines.

## Guarantees

Every theme × product pair is tested: text ≥ 4.5:1 (7:1 in high contrast),
non-text UI ≥ 3:1, focus ring ≥ 3:1 against surfaces. Run
`npm test -w @rookcom/monarch-tokens` or read `contrast-report.json`.

## Adding a product

Add `registry/products/<id>.json` (schema in `product.schema.json`), run
`npm run build`. The build generates `css/products/<id>.css`, the TS
`products` map and the contrast report; the tests fail if the accent cannot
be made accessible.

## Teams outside Rook: the neutral accent

`monarch.css` uses Rook Red, the Rook brand's primary colour, as the accent
when no product is set. A team that is not building a Rook product can switch
to a neutral slate blue with no brand meaning:

```css
@import '@rookcom/monarch-tokens/css';
@import '@rookcom/monarch-tokens/css/products/neutral';
```

```html
<html data-mn-product="neutral"></html>
```

- **How it selects.** Exactly like a product file: `[data-mn-product="neutral"]`,
  with the dark and high-contrast blocks (`.dark [data-mn-product="neutral"]`
  and so on) at a higher specificity than the theme blocks, so it wins in any
  load order. Set the attribute on `<html>` for the whole page, or on any
  element to re-theme a region. `appearanceScript({ product: 'neutral' })`
  sets it before first paint.
- **Why an attribute, not a new default.** Importing the file changes nothing
  until the attribute is set, so it cannot clash with a product attribute
  already on the page, and it follows the same contract and tests as every
  product file. `monarch.css` itself is unchanged.
- **Same guarantees as a product accent.** It is derived by the same contrast
  engine and tested in every theme (`packages/tokens/test/neutral.test.ts`):
  text on fills ≥ 4.5:1 (7:1 in high contrast), links and focus rings
  readable on every surface, and distinct from the AI purple and the danger,
  success and warning colours.
- **Not a registry product.** `neutral` is not in `registry/products`, the
  `products` map or the conformance product list, and the build refuses a
  registry product with that id. In code, `neutralAccent` and
  `NEUTRAL_ACCENT_ID` are exported from `@rookcom/monarch-tokens`.

### Your own brand colour, at build time

A team with a brand colour of its own should derive the accent from it
instead of using the neutral one. `deriveTheme` (below) is pure, so it can run
once in a build script and write a static stylesheet:

```ts
// scripts/brand-css.ts, run before the app build
import { writeFileSync } from 'node:fs';
import { brandThemeCss, deriveTheme } from '@rookcom/monarch-tokens';

const theme = deriveTheme('#0f766e');
if (!theme.valid) throw new Error('Brand colour must be #rgb or #rrggbb');
writeFileSync('src/styles/brand.css', brandThemeCss(theme)); // :root, all four themes
```

Import `brand.css` after `@rookcom/monarch-tokens/css`. It sets the accent on
`:root` for every theme, in `@layer monarch.tokens`, so no attribute is
needed, and `modes[id].report` says what was adjusted to meet contrast.

## Customer brand colours (`deriveTheme`)

When a customer picks a colour (a booking page, a support portal, a community
or careers page), derive the accent from it at runtime instead of trusting it:

```ts
import { brandThemeCss, deriveTheme } from '@rookcom/monarch-tokens';

const theme = deriveTheme(workspace.brandColor); // '#rgb' or '#rrggbb'
theme.valid; // false when the value was not a hex colour (the fallback was used)

// A stylesheet that follows the user's theme (.dark, data-mn-contrast="more"):
const css = brandThemeCss(theme, { selector: '.booking-page' });

// Or inline, for a surface that is always one theme:
<div style={theme.modes.light.vars}>…</div>;
```

- **Pure.** No DOM, no globals, no cache: the same input always gives the
  same output, on the server or in the browser.
- **Same engine as product accents.** Each of the four themes runs
  `deriveIntent`, so a customer colour gets the guarantees above:
  `on-solid` on every fill ≥ 4.5:1 (7:1 in high contrast), `strong` (links,
  icons, focus ring) on canvas, surface, subtle, muted and the accent's own
  tints ≥ 4.5:1 (7:1), and a fill that stays visible on the canvas. The fill
  may be darkened or switched to dark text to get there; `modes[id].report`
  says what changed, in plain language.
- **Output.** `modes.light | dark | hc-light | hc-dark`, each with `accent`
  (HSL roles), `report` and `vars`: every `--mn-color-accent-*` role plus
  `--mn-color-text-link` and `--mn-color-focus-ring`. Those two are restated
  because a custom property's `var()` resolves where it is declared (`:root`),
  so re-theming a region would otherwise leave links and focus rings on the
  page accent. `deriveTheme(hex, { compat: true })` adds the legacy
  `--primary` variables.
- **Invalid input never throws.** It falls back to `options.fallback`, or the
  Monarch default accent, and sets `valid: false`.
- **Selectors.** `brandThemeCss` scopes each theme block like the product
  files: `<selector>` for light, `.dark <selector>` / `[data-mn-theme="dark"]`
  and `[data-mn-contrast="more"]` (on an ancestor or the element itself), in
  `@layer monarch.tokens` unless `layer: false`.
- `parseHexColor(value)` validates a colour field before saving it.

### A custom AI colour (`ai`)

AI content (agent avatars, AI labels, suggestions, citations, the assistant
button) uses one shared purple, the intelligence hue from ADR 0013, so it
looks the same in every Rook product. Rook products keep it. A library or
product outside Rook can set its own AI colour from one hex value:

```ts
const theme = deriveTheme('#0096d4', { ai: '#e8590c' });

theme.ai?.distinct; // true when the AI colour is far enough from the others
theme.ai?.warnings; // plain-language notes to show whoever picked the colours
const css = brandThemeCss(theme, { selector: ':root' });
```

- **Same contrast guarantees as the accent.** The AI colour runs through
  `deriveIntent` in all four themes, so every `--mn-color-ai-*` role meets
  the targets above. `modes[id].ai` holds the roles and `modes[id].aiReport`
  says what changed.
- **Checked for distance.** The AI fill is measured against the accent and
  the danger, success, warning and info fills in every theme (OKLab ΔE ×100).
  Below `AI_MIN_DISTANCE` (10, the floor the Monarch purple meets against
  every Rook product accent), `distinct` is false and `warnings` names the
  colour and theme it is too close to. It is a warning, not an error:
  AI content always carries a text label and the Bot icon as well.
- **Output.** `vars` gains every `--mn-color-ai-*` role plus
  `--mn-color-ai-thinking`, `--mn-color-ai-citation` and `--mn-gradient-ai`,
  restated for the same `var()` reason as links. Diff and tool colours stay
  on the status colours.
- **Invalid input never throws.** The Monarch purple is kept, `valid` is
  false and `warnings` says why. Without the option, `theme.ai` is null and
  nothing AI-related is emitted.

Tested in `packages/tokens/test/brand.test.ts` across more than 1,300
inputs (every 15° of hue at saturations 0–100 and lightness 0–100, plus
white, black, pure yellow and other edge cases) in all four themes.

## Tailwind CSS

**Tailwind 3**: use the preset.

```ts
// tailwind.config.ts
import monarch from '@rookcom/monarch-tokens/tailwind';
export default { presets: [monarch], content: ['./src/**/*.{ts,tsx}'] };
```

**Tailwind 4**: there is no JavaScript config; import the Monarch theme in
the CSS file that imports Tailwind:

```css
@layer theme, base, monarch, components, utilities;
@import 'tailwindcss';
@import '@rookcom/monarch-tokens/css';
@import '@rookcom/monarch-tokens/css/tailwind-v4';
```

- **Same class names.** `css/tailwind-v4.css` is generated from the Tailwind 3
  preset, so `bg-mn-surface`, `text-mn-fg-muted`, `border-mn-line`,
  `bg-mn-accent-solid`, `text-mn-ai-strong`, `bg-mn-data-1`, `shadow-mn-md`,
  `h-control-md`, `rounded-lg`, `duration-fast`, `ease-standard`, `z-modal`,
  `animate-fade-in` and the `dark:` and `rail-expanded:` variants mean the same
  in both versions. Opacity modifiers (`bg-mn-surface/50`) work too.
  Tailwind 4 also gets every other Monarch colour (`bg-mn-ai-thinking`,
  `text-mn-confidence-high`, `bg-mn-overlay-scrim`) and every duration and
  easing (`duration-shimmer`, `ease-stream`).
- **Runtime switching.** The file uses `@theme inline`, so each utility reads
  the Monarch variable (`background-color: var(--mn-color-bg-surface)`).
  Theme, contrast and product accent switch at runtime, also on nested
  regions. A plain `@theme` would resolve the variable once, on `:root`.
- **Cascade layers.** Tailwind 4 writes its CSS into the layers `theme`,
  `base`, `components` and `utilities`; Monarch uses `monarch.reset`,
  `monarch.tokens`, `monarch.base` and `monarch.components`. The first
  stylesheet that names a layer fixes its place, so the `@layer` line goes
  first, before any import: Tailwind's preflight (`base`) then sits below
  Monarch, and utilities stay above Monarch components, as in Tailwind 3.
  Without it, Monarch would land after `utilities` and component styles would
  beat utility classes.
- **Legacy shadcn names.** Apps still on `bg-primary` or
  `text-muted-foreground` add `@import '@rookcom/monarch-tokens/css/compat';`
  and `@import '@rookcom/monarch-tokens/css/tailwind-v4-compat';`.
  Deprecated, like `css/compat`.

## Design tools: DTCG and Figma

The build writes the tokens in the W3C Design Tokens Community Group format,
stable version 2025.10:

| File                                               | What it holds                                              |
| -------------------------------------------------- | ---------------------------------------------------------- |
| `tokens.dtcg.json`                                 | One self-contained file: primitives plus the light theme   |
| `tokens.resolver.json`                             | DTCG resolver: the primitives set, then a `theme` modifier |
| `dtcg/primitives.json`                             | Primitives only                                            |
| `dtcg/semantic.{light,dark,hc-light,hc-dark}.json` | Semantic tokens for one theme (one file per mode)          |
| `figma-variables.json`                             | Figma Variables, two collections, four modes               |

All are package exports, for example
`@rookcom/monarch-tokens/tokens.dtcg.json` and
`@rookcom/monarch-tokens/dtcg/semantic.dark.json`.

- **Values.** Colours are `{ "colorSpace": "srgb", "components": [r, g, b], "hex": "#rrggbb" }`
  (plus `alpha` for the scrim and shadows); dimensions `{ "value": 0.25, "unit": "rem" }`
  (Monarch's own units, rem or px); durations `{ "value": 120, "unit": "ms" }`;
  easings cubic Bézier arrays; font families arrays of names (`["Poppins", "ui-sans-serif", …]`);
  shadows arrays of layer objects.
- **Aliases.** A semantic token aliases a primitive when the theme uses that
  primitive exactly (`"{color.neutral.900}"`) and another semantic token where
  the CSS does (`color.text.link` → `{color.accent.strong}`); otherwise it is
  a literal, because most intent colours are computed for contrast. Every
  semantic token carries its CSS variable in
  `$extensions["com.rookhq.monarch"].cssVar`.
- **Themes are modes.** `tokens.resolver.json` lists the primitives set, then
  the `theme` modifier with the contexts `light`, `dark`, `hc-light` and
  `hc-dark`, each pointing at its `dtcg/semantic.<theme>.json`. The accent is
  the default accent (Rook Red); product, neutral and `deriveTheme` accents
  are CSS only.
- **Not in DTCG.** Fluid `clamp()` sizes (display, section, widths), `em`
  letter spacing and gradients have no DTCG type and stay in the CSS.
  `tokens.json`, the earlier token file, is unchanged.

### Importing into Figma

`figma-variables.json` is the request body for Figma's REST endpoint
`POST /v1/files/:file_key/variables`. It creates two collections:
`Primitives` (one mode, hidden from publishing, numbers in px) and `Semantic`
with the modes `Light`, `Dark`, `HC Light` and `HC Dark`. Semantic variables
alias primitives wherever the DTCG files do, have scopes (fills, text,
strokes) and show the CSS variable as their web code syntax.

1. Open the Figma file and copy its key from the URL (`figma.com/design/<file_key>/…`).
2. Create a personal access token with the `file_variables:write` scope. Writing
   variables through the REST API needs a Figma plan that offers it (at the
   time of writing, Enterprise).
3. Post the file once, into a file that does not have these collections yet
   (IDs in it are temporary; Figma assigns real ones):

   ```sh
   curl -X POST "https://api.figma.com/v1/files/<file_key>/variables" \
     -H "X-Figma-Token: $FIGMA_TOKEN" \
     -H "Content-Type: application/json" \
     --data @node_modules/@rookcom/monarch-tokens/dist/figma-variables.json
   ```

4. To update later, delete the two collections and post again.

Without REST access, use a Figma plugin that imports DTCG files: load
`dtcg/primitives.json` into one collection, then each
`dtcg/semantic.<theme>.json` as a mode of a second collection. Check that the
plugin reads DTCG 2025.10 colour objects and aliases across files.

## Overriding

Monarch CSS lives in `@layer monarch.*`, so any unlayered app rule wins.
Deliberate overrides are allowed but reported by conformance
(`overrides/monarch-internals`) so they stay visible. Prefer a registry
exception with an owner and expiry. The few variables a component page
documents as public, such as Toast's `--mn-toast-offset-*`, are meant to be
set by apps and are not reported.

## Rules every component follows in each theme

The measured ratios for every pairing are in
[CONTRAST-MATRIX](../architecture/CONTRAST-MATRIX.md), generated by
`npm run contrast` and checked by `npm run verify`.

### Dark is not inverted light

- Elevation uses lighter surfaces, not heavier shadows:
  `bg-canvas` < `bg-surface` < `bg-raised` in luminance (checked).
- No pure black canvas in the standard dark theme; borders carry more of the
  structure than in light.
- Charts, diffs (`--mn-color-ai-diff-*`) and status fills are derived per
  theme, so they keep their contrast on dark surfaces.

### High contrast is its own design

- Every interactive boundary has a solid 1–2px border; nothing relies on a
  shadow, tint or gradient alone. Surfaces are flat by design.
- The focus ring is 3px with an offset and uses the text colour.
- Selected states use a border plus an icon or weight, never fill alone.
- `--mn-gradient-ai` is a solid colour; the "thinking" shimmer is static text.
- Disabled uses `text-disabled` plus a text or icon cue.

### Forced colours (Windows)

Every component stylesheet has a `@media (forced-colors: active)` block that
maps its meaningful boundaries and states to system colours: `ButtonText` /
`CanvasText` borders for controls and surfaces, `Highlight` /
`HighlightText` for selected and current items, `GrayText` for disabled,
`LinkText` for links. Icons use `currentColor`; SVG that carries meaning
(charts, meters, progress) uses system colours or a border so the shape still
reads.

### Reduced motion

Every duration token collapses to `0ms` under `prefers-reduced-motion: reduce`,
and every animated stylesheet also has its own reduced-motion block for
keyframes and literal durations: no shimmer, no caret blink, no spinning, no
smooth auto-scroll, no celebration motion. Fades up to 120ms are allowed.

### Disabled text

`text-disabled` is exempt from WCAG contrast, but Monarch keeps it at 2.5:1 or
more on every surface up to `bg-muted`, so a disabled label is still legible,
and never uses it as the only cue that something is disabled.
