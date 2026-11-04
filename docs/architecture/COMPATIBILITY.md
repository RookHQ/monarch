# Compatibility and support policy

What Monarch supports, and how that is communicated. The machine-readable form
is [`registry/compatibility.json`](../../registry/compatibility.json); the
`monarch` CLI reads it for the `deps/monarch-version` rule and for
`monarch registry verify`.

## Package versions

Each release line is supported on its current minor version (for example
`>=0.5.0 <0.6.0` for `@rookcom/monarch-react` while it is `0.5.x`). Packages are
pre-1.0, so a minor release may contain breaking changes; each one is called
out in the changelog and, where it needs a migration, in
[migration](../guides/migration.md). See [versioning](../governance/versioning.md)
for the semver rules and release channels, and the
[deprecation policy](../governance/DEPRECATION-POLICY.md) for how APIs are
retired.

`@rookcom/monarch-react` requires a matching `@rookcom/monarch-tokens` minor
version (its peer range is in `package.json`).

## Peer dependencies

| Peer                 | Supported range | Notes                                            |
| -------------------- | --------------- | ------------------------------------------------ |
| `react`, `react-dom` | `>=18.3 <20`    | Components are server-render safe                |
| `tailwindcss`        | `>=3.4 <4`      | Only if you use the Tailwind preset              |
| `next`               | `>=14`          | Checked only when the consuming app uses Next.js |
| Tiptap packages      | `^2.27.3`       | Optional; only for the `./editor` subpath        |

Node `>=20.11` is required to run the conformance CLI and the repository's
tooling.

## Browsers

The accessibility suite (axe-core plus keyboard flows) runs in Chromium,
Firefox and WebKit on every change. Visual baselines are Chromium only,
because pixel output legitimately differs between engines. Evergreen versions
of those three engines are supported.

## Adoption status

Whether a given application "uses Monarch" is decided by measurement, never by
declaration: `monarch registry verify` scans a real checkout and classifies
adoption and compatibility from what it finds
([conformance guide](../guides/conformance.md#how-registry-verify-decides-adoption)).
Per-application results are the application owners' data and are not published
in this repository.
