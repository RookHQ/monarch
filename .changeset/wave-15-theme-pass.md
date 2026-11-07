---
'@rookcom/monarch-react': patch
'@rookcom/monarch-tokens': patch
---

Wave 15 theme pass: every component stylesheet now has a forced-colors block and every animated one a reduced-motion block (enforced by the CSS build); disabled text reaches 2.5:1 on every surface up to `bg-muted`; avatar statuses keep distinct shapes in forced colours; a generated contrast matrix (`docs/architecture/CONTRAST-MATRIX.md`) is checked by `npm run verify`.
