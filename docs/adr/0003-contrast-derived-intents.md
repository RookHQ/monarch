# ADR 0003: Accent and status colours are derived, with contrast guaranteed at build time

Status: accepted (2026-11-03)

## Context

Every product has its own brand accent. Measured on 2026-11-03, 10 of 20 apps
put white text on an accent below 4.5:1.

## Decision

A product registers one base colour. `deriveIntent()` produces nine roles per
theme (solid, hover, active, on-solid, subtle, subtle-hover, strong, on-strong,
border) and guarantees:

- solid (and hover/active) vs the page canvas: distinguishable (≥ 1.5:1) in
  standard themes, ≥ 4.5:1 in high contrast. WCAG 1.4.11 does not require 3:1
  for a filled control whose text identifies it, and demanding it would turn
  Rook Yellow into a brown fill.
- on-solid vs solid (and hover/active): ≥ 4.5:1 (≥ 7:1 high contrast)
- strong vs canvas, surface, subtle: ≥ 4.5:1 (≥ 7:1 high contrast)

Order of preference for the solid fill, chosen to keep the brand colour
recognisable: (1) brand fill with white text; (2) fill darkened by at most 10
lightness points with white text (Rook Blue and several product accents);
(3) brand fill with dark text (Rook Yellow and the gold and orange product
accents); (4) move the fill away from the canvas with whichever
text colour works. Hue and saturation never change. The same function derives
danger, success, warning and info, so statuses behave identically across
products.

Every adjustment is written to `contrast-report.json` in plain language.
`test/contrast.test.ts` checks every product in every theme.

## Consequences

Some brand fills look slightly darker, and gold/orange buttons use dark
text. That is the accessible version of the brand, documented per product
rather than hidden. High-contrast themes may darken further.
