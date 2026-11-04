# ADR 0004: Component styles are plain CSS, not Tailwind classes

Status: accepted (2026-11-03)

## Context

Apps run Tailwind 3 (19), Tailwind 4 (1), and future apps may use neither.
Shipping Tailwind class strings would require every app's Tailwind to scan
`node_modules` and would break on Tailwind 4.

## Decision

Components render stable `mn-*` class names and `data-*` state attributes.
`@rookcom/monarch-react/styles.css` is plain CSS that references only `--mn-*`
variables. The build fails if a component stylesheet contains a raw colour.

## Consequences

Works with any styling setup and survives Tailwind upgrades. `className` still
merges, and because Monarch is layered, an app utility deliberately overrides.
