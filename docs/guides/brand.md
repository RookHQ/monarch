# Rook brand in Monarch

Source of truth: rookhq.com/brand-kit (logos in the brand kit; colours and
usage below are quoted from it). Monarch binds these once, in
`packages/tokens/src/primitives.ts` (`brand`), and derives everything else.

| Name        | Hex       | Brand-kit usage                               | Monarch role                                               |
| ----------- | --------- | --------------------------------------------- | ---------------------------------------------------------- |
| Rook Red    | `#DC2E3E` | Primary brand colour, call-to-action elements | `danger` intent; default `accent` when no product is set   |
| Rook Blue   | `#0096D4` | Secondary brand colour, links, information    | `info` intent; legacy `--secondary` / `--accent`           |
| Rook Green  | `#28AF4C` | Success states, positive indicators           | `success` intent                                           |
| Rook Yellow | `#F4B400` | Warning states, highlights                    | `warning` intent                                           |
| Rook Violet | `#6366F1` | Accent colour, premium features               | available as `brand.violet`; available as a product accent |

Typography: Poppins everywhere (`--mn-font-sans` reads `--font-poppins` from
`next/font`, with system fallbacks). See ADR 0007 for the open question on a
data-dense UI face.

## What the brand kit does not decide

Product accents (teal, green, blue, indigo, orange, magenta, …) are product decisions recorded in
`registry/products/<id>.json`. Monarch derives accessible variants; it does
not choose them. Where a brand colour fails WCAG with white text (Yellow,
and Red at 4.66:1 is close) Monarch uses dark text or a slightly darker fill
and records the adjustment in `contrast-report.json`.

## Monarch's own identity

"Monarch" is expressed in the name and in restraint: white and soft-neutral
surfaces, generous spacing, one accent per view, the blue→violet→purple→
pink→orange spectrum (`--mn-gradient-spectrum`) for rare brand moments only.
No crowns, no gold.
