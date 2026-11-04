# @rookcom/monarch-tokens

## 0.4.0

### Minor Changes

- 220a201: The categorical data palette (`--mn-color-data-1` … `-5`) is now chosen per theme and contrast-checked: every slot meets 3:1 on every chart background (4.5:1 in high contrast), and neighbouring slots stay distinguishable under normal vision, protanopia and deuteranopia. Dark-theme values are deeper and high-contrast themes get their own values. Adds `dataPalette(theme)` and `DATA_PALETTE_ORDER`.

## 0.3.0

### Minor Changes

- 02ded2b: Marketing layer. Tokens: fluid display scale, lead size, section rhythm, content widths, derived gradients (`--mn-gradient-wash`, `--mn-gradient-accent`, `--mn-gradient-inverse`), inverse-surface roles (`text-inverse-muted`, `border-inverse`) and the `strong-inverse` intent role, all contrast-tested for every product. React: SiteHeader, MarketingHero, MarketingSection, MarketingSectionHeader, FeatureGrid, ProductFrame, StatStrip, CTABand, FAQ and SiteFooter from the new `@rookcom/monarch-react/marketing` entry; Accordion from the root entry.

  Fix: a product's light-theme accent now wins over the light theme regardless of CSS load order (the light theme selector uses `:where()`).

- 31753b0: Brand theme engine: `deriveTheme(hex)` derives a full accent token set (every intent role plus `--mn-color-text-link` and `--mn-color-focus-ring`) for the light, dark and both high-contrast themes from one brand colour, with the same contrast guarantees as product accents (4.5:1, 7:1 in high contrast). Pure, no DOM. `brandThemeCss(theme, { selector })` renders it as CSS that follows the Monarch theme selectors, `parseHexColor()` validates input, and invalid colours fall back instead of throwing. `compat: true` adds the legacy `--primary` variables.

## 0.2.3

### Patch Changes

- Rook Developers is out of Monarch's scope (owner decision), so its product accent is no longer generated: `css/products/developers.css` is removed. No Rook app imports it.

## 0.2.2

### Patch Changes

- Legacy bridge: `--primary` now uses the accent's readable `strong` colour wherever it also works as a fill with the same text colour, so `text-primary` meets 4.5:1 on every surface (16 of 20 products). Dark-text brands (gold, orange) keep their fill. The Tailwind preset is now assignable to Tailwind 3's `Config` without a cast.

## 0.2.1

### Patch Changes

- Accent and status `strong` colours are now guaranteed readable (4.5:1, 7:1 in high contrast) on `bg-muted` as well, where tables, selected rows and toolbars sit; several brands' strong text is slightly darker as a result. `appearanceScript()` honours Rook's `data-rook-theme-routes`, so public routes stay light when an app adopts it.

## 0.2.0

### Minor Changes

- Packages move to the public npm registry as `@rookcom/monarch-*` (were `@rookhq/monarch-*` on GitHub Packages). Apps install with plain `npm ci`; no registry token is needed. Releases are published from CI with npm trusted publishing and provenance.

### Patch Changes

- a2d52a3: Tokens: contrast is now measured on 8-bit colour as browsers paint it, so no pair passes in maths and fails on screen (Rook Blue `info` was 4.48:1 in the browser).

  Conformance: recognises the new components and patterns as Monarch primitives; `monarch-ignore-next-line` skips comment-only lines so it can sit next to `eslint-disable-next-line`; 4-digit numbers like `Order #1042` are no longer reported as colours.
