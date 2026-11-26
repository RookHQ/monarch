# Deprecation policy

Short form. The full policy, with codemods and the conformance rule, is
[DEPRECATION-POLICY.md](DEPRECATION-POLICY.md).

1. Mark: add the symbol to `registry/deprecations.json` with `since`,
   `removeIn`, `replacement` and a reason; add `@deprecated` JSDoc.
2. Warn: the conformance rule `api/deprecated` reports usage at `warn`
   from the next release; the changelog lists it.
3. Wait: at least one release and 60 days.
4. Remove: in the next major (on 0.x, the next minor), with the migration
   note in `docs/guides/migration.md`.

An export that only moves to another entry follows the same steps, listed
under `importPaths` in `registry/deprecations.json`; the old path keeps
working until `removeIn`, and `monarch check --fix` rewrites the import. In
effect now: the Rook-specific exports moved to `@rookcom/monarch-react/rook`
in 0.14.5 and leave `/shell`, `/patterns` and `/ai` in 0.15.0.

The legacy bridge (`css/compat`) is deprecated from 0.1.0; its removal
release is decided once every app's `legacy/bridge-variable` count is 0.
