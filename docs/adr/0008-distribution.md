# ADR 0008: Publish to public npm as @rookcom, with trusted publishing

Status: accepted (2026-11-03), amended when the repository became open source under MIT. Supersedes the earlier GitHub Packages proposal.

## Context

Application builds run in CI and in containers. Installing from GitHub Packages
would need a read token in every app's build, plus a per-package access grant
for each consuming repository. The packages contain no secrets: tokens,
components and a rule list.

## Decision

- Publish `@rookcom/monarch-tokens`, `@rookcom/monarch-react` and
  `@rookcom/monarch-conformance` to the public npm registry (org `rookcom`).
  Apps install with plain `npm ci`; no token anywhere in app builds.
- CI publishes with **npm trusted publishing** (GitHub OIDC, `id-token: write`),
  so no npm token is stored anywhere. The source repository is public, so
  releases are published with npm provenance attestations
  (`NPM_CONFIG_PROVENANCE`), which let anyone verify which commit and workflow
  produced a package.
- Channels: `canary` (every merge with pending changesets,
  `0.0.0-canary-<timestamp>`), `latest` (stable, via the Changesets version PR).
- Apps pin exact versions and upgrade by PR.
- License: **MIT** (`Copyright (c) 2026 Rook Platforms`). Every package ships
  the `LICENSE` file. The Rook name and logos are trademarks of Rook Platforms
  and are not covered by the MIT license.

## Consequences

The source of every published version is public. Only `dist`, `README.md` and
`LICENSE` are listed in each package's `files`.
