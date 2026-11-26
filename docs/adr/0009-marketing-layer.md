# ADR 0009: A marketing layer, derived from the same tokens

Status: proposed (2026-11-03), awaiting design approval

## Context

The first product to adopt Monarch tokens still kept the bespoke look of its
public pages: hand-picked gradients, a near-black section colour, light-weight
oversized headings and hundreds of hard-coded colours. Monarch had nothing for
landing pages, so there was nothing to adopt. Every Rook product has public
pages; without a shared layer each one invents its own.

## Decision

Add a marketing layer to the existing packages (no new package):

- **Tokens:** a fluid display scale (`--mn-display-{sm,md,lg,xl}`, Poppins 600,
  tight tracking), `--mn-lead`, section rhythm (`--mn-section-*`), content
  widths (`--mn-width-*`), and gradients that reference semantic colours
  (`--mn-gradient-wash`, `-accent`, `-inverse`) so every theme and product
  gets its own without hand-picked values.
- **Inverse surfaces:** new roles `text-inverse-muted`, `border-inverse` and
  an intent role `strong-inverse`, guaranteed at 4.5:1 (7:1 high contrast) on
  `bg-inverse`, on the tinted gradient corner and on cards inside the band,
  for every product. Inverse sections remap semantic variables locally, so
  any component placed inside stays readable. In dark themes the band becomes
  an elevated dark surface rather than flipping to light.
- **Components:** SiteHeader, MarketingHero, MarketingSection, MarketingSectionHeader,
  FeatureGrid, ProductFrame, StatStrip, CTABand, Accordion, FAQ, SiteFooter.
- **Reference composition:** the docs site's `/__marketing` page, under
  visual regression in three themes and four accents at two widths.

Also fixed while building it: the light theme selector had the same
specificity as a product's light accent, so the accent lost whenever a bundler
placed product CSS first (it did in the docs site). Light now uses `:where()`
and a test proves product accents win in every theme regardless of order.

## Consequences

Product sites migrate by replacing their bespoke sections with these
components and deleting local colours. Brand expression is constrained on
purpose: one accent per product, the spectrum only for Rook brand moments.
