# Theming

## Selectors

| Theme               | Selector on `<html>`                                              |
| ------------------- | ----------------------------------------------------------------- |
| light               | default (`:root`) or `data-mn-theme="light"`                      |
| dark                | `.dark` or `data-mn-theme="dark"`                                 |
| high-contrast light | `data-mn-contrast="more"`                                         |
| high-contrast dark  | `.dark` + `data-mn-contrast="more"`                               |
| product accent      | `data-mn-product="<id>"` (any element; nest to re-theme a region) |

## Token groups

- Surfaces: `--mn-color-bg-{canvas,surface,raised,subtle,muted,emphasis,inverse}`
- Text: `--mn-color-text-{default,muted,subtle,disabled,inverse,link}`
- Borders: `--mn-color-border-{subtle,default,strong,input}`
- Intents `accent | danger | success | warning | info`, each with
  `solid, solid-hover, solid-active, on-solid, subtle, subtle-hover, strong, on-strong, border`
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

Tested in `packages/tokens/test/brand.test.ts` across more than 1,300
inputs (every 15° of hue at saturations 0–100 and lightness 0–100, plus
white, black, pure yellow and other edge cases) in all four themes.

## Overriding

Monarch CSS lives in `@layer monarch.*`, so any unlayered app rule wins.
Deliberate overrides are allowed but reported by conformance
(`overrides/monarch-internals`) so they stay visible. Prefer a registry
exception with an owner and expiry.
