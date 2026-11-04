# Deprecation policy

How a Monarch API (export, prop, variant value, `mn-*` class, `data-*`
attribute, `--mn-*` variable or token) is retired without breaking a Rook
product by surprise. Extends [deprecation.md](deprecation.md) and
[versioning.md](versioning.md); if they disagree, this page wins.

## Stages

| Stage      | What happens                                                                                                                                                                                                                         | Minimum time                               |
| ---------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ | ------------------------------------------ |
| 1. Propose | Issue or ADR naming the API, the reason, the replacement and the products affected (from `monarch registry verify` and conformance reports).                                                                                         | —                                          |
| 2. Mark    | Add an entry to [`registry/deprecations.json`](../../registry/deprecations.json) (`name`, `package`, `since`, `replacement`, `message`) and an `@deprecated` JSDoc tag. Changelog lists it. The registry shows `status: deprecated`. | Ships in a minor release                   |
| 3. Warn    | The conformance rule `api/deprecated` reports every usage at `warn` in apps that import the package. A codemod or a written migration step is published.                                                                             | At least one minor release **and** 60 days |
| 4. Remove  | Deleted in the next major, with a migration note in `docs/guides/migration.md`, and the entry removed from `deprecations.json` in the same release.                                                                                  | Only after stage 3, and only in a major    |

Monarch is on 0.x, where a minor is breaking for consumers
([versioning](versioning.md#compatibility-matrix): apps pin exact versions).
While on 0.x, "major" above means the next minor that the compatibility
matrix marks as a new supported line.

## Codemods

- A deprecation that renames or moves something mechanically (an export,
  prop, class or variable) ships a codemod with the warn stage, or a reason
  in the changeset why it cannot.
- Codemods live in the repository, are tested against fixtures, and are
  idempotent. Run them with `npx` against a product checkout; they never
  touch files outside the given root.
- Behavioural changes that cannot be automated get a step-by-step section in
  `docs/guides/migration.md` with before and after code.

No codemod exists yet, because nothing has been deprecated. The first
deprecation that needs one adds the tooling in the same PR.

## The conformance rule

`api/deprecated` (severity `warn`, in `@rookcom/monarch-conformance`) reads
`registry/deprecations.json` and reports every usage of a deprecated name in
files that import the package. It is what makes stage 3 visible in each
product's CI and in its measured status file. Rules:

1. A deprecation is not real until it is in `deprecations.json`; a JSDoc tag
   alone is not enough.
2. Removal is blocked while any registered product's latest measured status
   still reports `api/deprecated` findings for that name, unless a maintainer
   records the exception and its reason in the removal ADR.
3. Products may not suppress `api/deprecated` with `monarch-ignore`; they
   migrate or request a time-limited exception
   ([exceptions](exceptions.md)).

## Legacy bridge

The legacy bridge (`css/compat`) is deprecated from 0.1.0. Its removal
release is decided once every app's `legacy/bridge-variable` count is 0.
