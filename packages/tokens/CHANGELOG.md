# @rookcom/monarch-tokens

## 0.5.4

### Patch Changes

- 6850b3e: Tailwind 4: `css/tailwind-v4.css` (export `./css/tailwind-v4`) is an `@theme inline` block generated from the Tailwind 3 preset, so `bg-mn-surface`, `text-mn-fg-muted`, `shadow-mn-md`, `h-control-md`, `duration-fast` and the `dark:` and `rail-expanded:` variants keep their names, and every utility reads the Monarch variable so themes, contrast and product accents still switch at runtime. It also offers the AI roles, confidence colours and every duration and easing. `css/tailwind-v4-compat.css` adds the legacy shadcn names. The theming guide documents the `@layer theme, base, monarch, components, utilities;` order that keeps utilities above Monarch components.

  Neutral accent for teams outside Rook: `css/products/neutral.css`, selected with `data-mn-product="neutral"` like a product file, a slate blue derived and contrast-tested in all four themes. It is not a registry product, and the build refuses a registry product named `neutral`. `neutralAccent` and `NEUTRAL_ACCENT_ID` are exported.

  Design tokens in the W3C DTCG 2025.10 format: `tokens.dtcg.json` (primitives plus the light theme), `dtcg/primitives.json`, `dtcg/semantic.<theme>.json` for each theme and `tokens.resolver.json` tying them together with a `theme` modifier. Semantic tokens alias primitives where they match exactly. `figma-variables.json` is a request body for Figma's `POST /v1/files/:file_key/variables`: a `Primitives` collection and a `Semantic` collection with `Light`, `Dark`, `HC Light` and `HC Dark` modes. `tokens.json` and every existing stylesheet are unchanged.

## 0.5.3

### Patch Changes

- f7b9220: `deriveTheme(brand, { ai: '#hex' })` sets a custom AI colour for products and libraries outside Rook. The AI colour runs through the same contrast engine as the accent in all four themes, and is checked for distance (OKLab ΔE ×100 ≥ `AI_MIN_DISTANCE`, 10) from the accent and the status colours: `theme.ai` reports `distinct`, the closest colour and theme, and plain-language `warnings`. `vars` and `brandThemeCss` gain the `--mn-color-ai-*` roles, `--mn-color-ai-thinking`, `--mn-color-ai-citation` and `--mn-gradient-ai`. Invalid values keep the Monarch purple. Without the option nothing changes, so Rook products keep the shared intelligence purple. Also adds `colorDistance(a, b)`.

## 0.5.2

### Patch Changes

- 568ede9: Every stylesheet now declares `monarch.reset` as the first Monarch layer. Tailwind 3 apps wrap `@tailwind base` in `@layer monarch.reset { }`, so preflight sits below Monarch in any load order while utilities still win on Monarch elements. `styles/tailwind-preflight.css` is superseded: loaded after utilities it cancelled them on Monarch elements.

## 0.5.1

### Patch Changes

- 1cdb3c6: Wave 15 theme pass: every component stylesheet now has a forced-colors block and every animated one a reduced-motion block (enforced by the CSS build); disabled text reaches 2.5:1 on every surface up to `bg-muted`; avatar statuses keep distinct shapes in forced colours; a generated contrast matrix (`docs/architecture/CONTRAST-MATRIX.md`) is checked by `npm run verify`.

## 0.5.0

### Minor Changes

- 2582d2f: Wave 11: AI foundations (ADR 0013), in the new `@rookcom/monarch-react/ai` entry.

  - Vendor-neutral message model (`AiMessage`, `AiPart`) and `useAiConversation` with `rookIntelligenceTransport` and `mockTransport`.
  - Components: `AiThread`, `AiMessageView`, `AiMessageActions`, `AiPromptInput`, `AiStreamingText`, `AiMarkdown`, `AiCodeBlock`, `AiThinkingIndicator`, `AiTypingDots`, `AiStopButton`, `AiErrorMessage`, `useAiScrollAnchor`.
  - Tokens: an `ai` intent (contrast-guaranteed in all four themes), `--mn-gradient-ai`, thinking, tool, citation, diff and confidence roles, and stream motion tokens.

### Patch Changes

- 9224222: Clearer description and README for everyone using Monarch.

## 0.4.1

### Patch Changes

- 0e0e6b3: The packages are open source under the MIT license. The npm metadata of the earlier tokens and conformance releases still said `UNLICENSED`; this release declares `MIT`, ships the `LICENSE` file, and adds a License section to each README.

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
