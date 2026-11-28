# Deprecation policy

Short form. The full policy, with codemods and the conformance rule, is
[DEPRECATION-POLICY.md](DEPRECATION-POLICY.md).

1. Mark: add the symbol to `registry/deprecations.json` with `since`,
   `removeNoEarlierThan`, `replacement` and a reason; add `@deprecated` JSDoc.
2. Warn: the conformance rule `api/deprecated` reports usage at `warn`
   from the next release; the changelog lists it.
3. Wait: at least 90 days and two patch releases after the release that
   deprecates.
4. Remove: in the next `0.14.N` patch after the window, with the migration
   note in `docs/guides/migration.md`. Write the date as "removed no earlier
   than <date> (90-day window)"; there is no 0.15.0.

An export that only moves to another entry follows the same steps, listed
under `importPaths` in `registry/deprecations.json`; the old path keeps
working until `removeNoEarlierThan`, and `monarch check --fix` rewrites the import. In
effect now: the Rook-specific exports moved to `@rookcom/monarch-react/rook`
in 0.14.5 (2026-11-13); `/shell`, `/patterns` and `/ai` keep them until
they are removed no earlier than 2027-02-11 (90-day window), not before
0.14.7.

The legacy bridge (`css/compat`) is deprecated from 0.1.0; its removal
release is decided once every app's `legacy/bridge-variable` count is 0.
