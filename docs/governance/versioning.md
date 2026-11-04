# Versioning and compatibility

Semantic versioning per package, managed by Changesets.

**Breaking (major):** removing or renaming an export, prop, variant value,
`mn-*` class, `data-*` attribute or `--mn-*` variable; changing a default;
raising a peer-dependency floor; removing a theme selector. Each needs an
ADR, a deprecation period of at least one minor release with a runtime or
lint warning, and a migration note.

**Minor:** new components, props, variants, tokens; new rules at `warn` or
below; visual refinements that keep contrast guarantees.

**Patch:** fixes with no API or token-name change.

Token _values_ may change in a minor (they are design, not API), except
where a change would break a contrast guarantee, which is a bug.

## Channels

| Tag      | When                                         | Who uses it               |
| -------- | -------------------------------------------- | ------------------------- |
| `canary` | every merge to `main` (`0.0.0-canary-<sha>`) | Monarch docs site, pilots |
| `next`   | release candidates                           | apps testing an upgrade   |
| `latest` | stable                                       | all apps, pinned exact    |

Apps pin exact versions and upgrade by PR. Breaking changes never reach an
app without a version bump it chose to take.

## Compatibility matrix

`registry/compatibility.json` records supported peer ranges per Monarch
version. `monarch registry verify` compares each app against it and reports
`compatible | needs-migration | unknown`.
