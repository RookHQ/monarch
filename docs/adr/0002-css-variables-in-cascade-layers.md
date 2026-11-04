# ADR 0002: Tokens are CSS custom properties inside cascade layers

Status: accepted (2026-11-03)

## Decision

All tokens are emitted as `--mn-*` custom properties inside
`@layer monarch.tokens`. Components live in `@layer monarch.components`. Every
Monarch stylesheet declares the order
`monarch.tokens, monarch.base, monarch.components` first.

Themes switch by attribute or class on `<html>`: `.dark` (kept because every
Rook app's Tailwind `dark:` variant uses it), `data-mn-theme`,
`data-mn-contrast="more"`, and `data-mn-product`. "System" preferences are
resolved before paint by `appearanceScript()` instead of media queries in CSS,
which keeps selector precedence deterministic: theme blocks are emitted in a fixed order (light, dark, high-contrast light, high-contrast dark) and product blocks add `[data-mn-product]` to each theme selector, so product + theme always beats theme alone.

## Consequences

- Rendering never depends on Monarch being online: the CSS ships inside each app's bundle.
- Unlayered app CSS beats Monarch, so deliberate overrides are possible and visible to the conformance engine (`overrides/monarch-internals`).
- Reduced motion is handled once: all `--mn-duration-*` collapse to 0ms.
