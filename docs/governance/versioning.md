# Versioning and compatibility

Each package is versioned on its own, managed by Changesets.

## The 0.x policy

Every Monarch package is pre-1.0. While that is true:

- `@rookcom/monarch-react`, and Monarch for Android and iOS with it, stay in
  the `0.14.x` patch series. Semantic versioning allows any change before
  1.0; Monarch limits itself to the rules below. There is no `0.15.0`.
- **Patch** (`0.14.2` → `0.14.3`): fixes and additive features, such as new
  components, props, variants, tokens, or rules at `warn` or below.
- **Removal** of a deprecated API also ships in a patch, and only after its
  window: 90 days **and** two patch releases after the release that
  deprecated it ([deprecation policy](DEPRECATION-POLICY.md)). Each removal
  has a migration note in [migration](../guides/migration.md), an entry in
  the changelog and, for an API change, an ADR. A break that cannot be
  preceded by a deprecation is an exception, recorded in an ADR.
- The peer range of `@rookcom/monarch-react` for `@rookcom/monarch-tokens` is
  `>=0.5.3 <1.0.0`.

From 1.0 the usual semantic versioning applies: additive features in a
minor, breaking changes only in a major.

## What counts as breaking

Removing or renaming an export, prop, variant value, public `mn-*` class,
`data-*` attribute or public `--mn-*` variable; changing a default; raising a
third-party peer-dependency floor (React, Tiptap); removing a theme selector;
moving an export to a different entry point. Each needs a deprecation period
first (see the [deprecation policy](DEPRECATION-POLICY.md)).

Raising the `@rookcom/monarch-tokens` floor of `@rookcom/monarch-react` to
the tokens version its components already rely on is a fix, not a breaking
change: the old range allowed a tokens version that lacked what the
components use. `0.14.x` relies on the data palette (tokens 0.4.0), the AI
tokens (0.5.0), the cascade-layer fix (0.5.2) and the `ai` brand option
(0.5.3), so its range is `>=0.5.3 <1.0.0`.

Token _values_ may change in any release (they are design, not API), except
where a change would break a contrast guarantee, which is a bug.

## `mn-*` class names

The root and part `mn-*` class names listed in the manifest (`className`) are
public and follow the deprecation policy; internal modifier classes and `--_*` or
undocumented variables are not.

Public variables are the theme tokens (`--mn-color-*`, `--mn-space-*` and the
other groups in [theming](../guides/theming.md)) and any variable a
component page documents as public (such as `--mn-toast-offset-*`).

## Channels

| Tag      | When                                                                            | Who uses it               |
| -------- | ------------------------------------------------------------------------------- | ------------------------- |
| `canary` | every push to `uat` or `main` with unreleased changesets (`0.0.0-canary-<sha>`) | Monarch docs site, pilots |
| `latest` | stable, published when a release is merged into `main`                          | all apps, pinned exact    |

Apps pin exact versions and upgrade by PR. Breaking changes never reach an
app without a version bump it chose to take.

## Compatibility matrix

`registry/compatibility.json` records supported versions and peer ranges.
`monarch check` reads it for `deps/monarch-version`, and
`monarch registry verify` compares each app against it and reports
`compatible | needs-migration | unknown`. See
[COMPATIBILITY](../architecture/COMPATIBILITY.md).
