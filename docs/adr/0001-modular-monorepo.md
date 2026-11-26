# ADR 0001: One repository, three independently versioned packages

Status: accepted (2026-11-03)

## Context

Monarch must serve 21+ apps that deploy independently. Apps need tokens without
React (marketing site, future non-React surfaces), React components, and a
governance tool that runs in CI with no design runtime.

## Decision

An npm-workspaces monorepo (npm because every Rook app uses npm) with three
packages, each versioned and published separately:

- `@rookcom/monarch-tokens`: framework-independent. CSS variables, JSON, TS API, Tailwind v3 preset.
- `@rookcom/monarch-react`: components. Peer-depends on tokens.
- `@rookcom/monarch-conformance`: CLI. No runtime dependencies; never imports design code.

Boundaries are enforced by dependency-cruiser (`npm run deps:check`).

## Consequences

No package exists without a separate consumer or release cadence. A docs site
and patterns package are added only when they have real content. Changesets
handles per-package versions and changelogs.
