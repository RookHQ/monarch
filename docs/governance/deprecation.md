# Deprecation policy

Short form. The full policy, with codemods and the conformance rule, is
[DEPRECATION-POLICY.md](DEPRECATION-POLICY.md).

1. Mark: add the symbol to `registry/deprecations.json` with `since`,
   `removeIn`, `replacement` and a reason; add `@deprecated` JSDoc.
2. Warn: the conformance rule `api/deprecated` reports usage at `warn`
   from the next minor release; the changelog lists it.
3. Wait: at least one minor release and 60 days.
4. Remove: in the next major, with the migration note.

The legacy bridge (`css/compat`) is deprecated from 0.1.0; its removal
major is decided once every app's `legacy/bridge-variable` count is 0.
