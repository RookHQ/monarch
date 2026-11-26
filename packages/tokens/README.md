# @rookcom/monarch-tokens

The foundation of Monarch, an open-source design system for everyone who
builds. Colour, type, spacing, radius, elevation and motion as CSS variables,
with light, dark and high-contrast themes. Bring one brand colour and
`deriveTheme` builds a palette that meets WCAG 2.2 AA contrast in every theme;
pass `{ ai: '#hex' }` to set your own AI colour, with the same contrast
guarantees and a check that it stays distinct from your other colours. Works with any framework.

Exports:

- `./css` (all themes), `./css/base`, `./css/compat` (legacy shadcn names,
  deprecated), `./css/products/<id>` (product accents).
- `./css/products/neutral`: a neutral slate-blue accent for teams outside
  Rook, selected with `data-mn-product="neutral"`. With a brand colour of your
  own, use `deriveTheme(hex)` and `brandThemeCss` at build time instead.
- `./tailwind` (Tailwind 3 preset) and `./css/tailwind-v4` (Tailwind 4
  `@theme inline`, same class names), plus `./css/tailwind-v4-compat` for the
  legacy names.
- `./tokens.dtcg.json`, `./tokens.resolver.json` and `./dtcg/*` (W3C DTCG
  2025.10, one file per theme), `./figma-variables.json` (body for Figma's
  `POST /v1/files/:file_key/variables`), `./tokens.json`,
  `./contrast-report.json`.
- `./appearance` (pre-paint boot script) and a TS API (`themes`,
  `resolveTheme`, `contrast`, `brand`, `products`, `deriveTheme`,
  `neutralAccent`).

Tailwind 4, in the CSS file that imports Tailwind:

```css
@layer theme, base, monarch, components, utilities;
@import 'tailwindcss';
@import '@rookcom/monarch-tokens/css';
@import '@rookcom/monarch-tokens/css/tailwind-v4';
```

Docs: ../../docs/guides/theming.md (themes, the neutral accent, Tailwind 3
and 4, DTCG and Figma import).

## License

[Apache-2.0](https://github.com/RookHQ/monarch/blob/main/LICENSE) © 2026 Rook Platforms. See [NOTICE](https://github.com/RookHQ/monarch/blob/main/NOTICE). The Rook and Monarch names and logos are trademarks of Rook Platforms and are not covered by this license; see [TRADEMARK.md](https://github.com/RookHQ/monarch/blob/main/TRADEMARK.md).
