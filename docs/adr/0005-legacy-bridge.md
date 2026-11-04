# ADR 0005: A generated legacy bridge for shadcn variable names

Status: accepted (2026-11-03). Bridge is deprecated from day one.

## Decision

`@rookcom/monarch-tokens/css/compat` defines `--background`, `--primary`,
`--muted-foreground` and the rest in shadcn's channel format, generated from
Monarch tokens. Rook's own meaning is kept: `secondary` and `accent` map to
`info` (blue), as in every Rook app today.

## Consequences

An app adopts Monarch tokens without touching its components, then migrates
usage to `--mn-*` / `mn-*` classes over time. Conformance reports remaining
legacy usage as `legacy/bridge-variable` (info) so the remaining work is visible.
The bridge is removed in a future major version with a migration guide.
