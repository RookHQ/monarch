# Deprecation policy

How a Monarch API (export, prop, variant value, `mn-*` class, `data-*`
attribute, `--mn-*` variable or token) is retired without breaking a Rook
product by surprise. Extends [deprecation.md](deprecation.md) and
[versioning.md](versioning.md); if they disagree, this page wins.

## Stages

| Stage      | What happens                                                                                                                                                                                                                         | Minimum time                          |
| ---------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ | ------------------------------------- |
| 1. Propose | Issue or ADR naming the API, the reason, the replacement and the products affected (from `monarch registry verify` and conformance reports).                                                                                         | —                                     |
| 2. Mark    | Add an entry to [`registry/deprecations.json`](../../registry/deprecations.json) (`name`, `package`, `since`, `replacement`, `message`) and an `@deprecated` JSDoc tag. Changelog lists it. The registry shows `status: deprecated`. | Ships in any release (a patch on 0.x) |
| 3. Warn    | The conformance rule `api/deprecated` reports every usage at `warn` in apps that import the package. A codemod or a written migration step is published.                                                                             | 90 days **and** two patch releases    |
| 4. Remove  | Deleted in the next `0.14.N` patch after the window, with a migration note in `docs/guides/migration.md`, and the entry removed from `deprecations.json` in the same release.                                                        | Only after stage 3                    |

Monarch is on 0.x and stays in the `0.14.x` patch series, which semantic
versioning allows before 1.0 ([0.x policy](versioning.md#the-0x-policy)). A
removal therefore ships in a patch, once the window has passed: no sooner
than 90 days **and** two patch releases after the release that deprecated the
API. Deprecation notices, JSDoc tags and changelog entries give the date in
the form "removed no earlier than <date> (90-day window)". None says "removed
in 0.15.0". From 1.0, removal ships only in a major.

## Codemods

- A deprecation that renames or moves something mechanically (an export,
  prop, class or variable) ships a codemod with the warn stage, or a reason
  in the changeset why it cannot.
- Codemods live in the repository, are tested against fixtures, and are
  idempotent. Run them with `npx` against a product checkout; they never
  touch files outside the given root.
- Behavioural changes that cannot be automated get a step-by-step section in
  `docs/guides/migration.md` with before and after code.

The first codemod is built into the conformance tools: `monarch check --fix`
and the ESLint rule `monarch/no-deprecated-api` rewrite the import path of an
export that moved to another entry (see the next section).

## Exports that move to another entry

Moving an export to a different entry point is a breaking change
([versioning](versioning.md#what-counts-as-breaking)), so it follows the same
stages, with one difference: the export itself is not deprecated, only the
old import path.

1. The export is added to the new entry and stays in the old one, where its
   re-export carries an `@deprecated` JSDoc tag naming the new path.
2. An entry goes into the `importPaths` list of
   [`registry/deprecations.json`](../../registry/deprecations.json): `names`,
   `from`, `to`, `since`, `removeNoEarlierThan` (and `pattern`, a regular expression for
   the related types). The registry status of the component does not change.
3. `api/deprecated` and `monarch/no-deprecated-api` report an import of a
   moved name from the old path, with a safe fix that changes only the path.
4. The old path stops exporting the names in a `0.14.N` patch released after
   the date in `removeNoEarlierThan`, never earlier and never inside the
   window, and the `importPaths` entry is removed in that release.

In effect today: the Rook-specific exports moved to
`@rookcom/monarch-react/rook` in 0.14.5 (2026-11-13); `/shell`, `/patterns`
and `/ai` keep exporting them until they are removed no earlier than
2027-02-11 (90-day window), not before 0.14.7
([rook-specific exports](../guides/rook-specific-exports.md#moving-to-rook)).

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
