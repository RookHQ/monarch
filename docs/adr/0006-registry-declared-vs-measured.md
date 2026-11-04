# ADR 0006: The registry separates declared facts from measured facts

Status: accepted (2026-11-03)

## Decision

`registry/products/<id>.json` holds what a person declares: name, domain,
accent, source paths, approved exceptions. `registry/status/<id>.json`
holds what a tool measured: framework versions, Monarch packages, adoption,
compatibility, conformance counts, commit and timestamp. Status files are only
written by `monarch registry verify` against a real checkout. This repository
publishes only the declared product data; measured status belongs to each
product's own checkout and CI.

## Consequences

"Adopted" can never be claimed by editing a file. Product accents in tokens are
generated from the registry, so the registry is the single source of truth.
