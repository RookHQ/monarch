# Compatibility and support policy

What Monarch supports, and how that is communicated. The machine-readable form
is [`registry/compatibility.json`](../../registry/compatibility.json); the
`monarch` CLI reads it for the `deps/monarch-version` rule and for
`monarch registry verify`.

## Package versions

| Package                        | Current line | Notes                                          |
| ------------------------------ | ------------ | ---------------------------------------------- |
| `@rookcom/monarch-react`       | `0.14.x`     | Stays on 0.14.x; needs tokens `>=0.5.3 <1.0.0` |
| `@rookcom/monarch-tokens`      | `0.5.x`      | No runtime dependencies                        |
| `@rookcom/monarch-conformance` | `0.5.x`      | Node only, no runtime dependencies             |

Each package is supported on its current line. Packages are pre-1.0, and
Monarch follows one 0.x policy: a patch release carries fixes and additive
features and never a breaking change; a breaking change ships only in a
minor, with a migration note in [migration](../guides/migration.md). See
[versioning](../governance/versioning.md) for the full rules and release
channels, and the [deprecation policy](../governance/DEPRECATION-POLICY.md)
for how APIs are retired.

`@rookcom/monarch-react` 0.14.x declares `@rookcom/monarch-tokens`
`>=0.5.3 <1.0.0` as a peer dependency. Install tokens 0.5.3 or later.

The root and part `mn-*` class names listed in the manifest (`className`) are
public and stable within a minor; internal modifier classes and `--_*` or
undocumented variables are not.

## Peer dependencies

| Peer                 | Supported range           | Notes                                            |
| -------------------- | ------------------------- | ------------------------------------------------ |
| `react`, `react-dom` | `^18.3.0 \|\| ^19.0.0`    | Components are server-render safe                |
| `tailwindcss`        | `>=3.4 <4` for the preset | Tailwind 4 uses `css/tailwind-v4`, no preset     |
| `next`               | `>=14`                    | Checked only when the consuming app uses Next.js |
| Tiptap packages      | `^3.30.4`                 | Optional; only for the `./editor` subpath        |

Node `>=20.11` is required to run the conformance CLI and the repository's
tooling.

Framework set-up (Next.js, Vite, React Router, Tailwind 3 and 4) is in
[frameworks](../guides/frameworks.md).

## Browsers

The accessibility suite (axe-core plus keyboard flows) runs in Chromium,
Firefox and WebKit on every change. Visual baselines are Chromium only,
because pixel output legitimately differs between engines. Minimum browser
versions are in [browser support](../governance/browser-support.md).

## Adoption status

Whether a given application "uses Monarch" is decided by measurement, never by
declaration: `monarch registry verify` scans a real checkout and classifies
adoption and compatibility from what it finds
([conformance guide](../guides/conformance.md#how-registry-verify-decides-adoption)).
Per-application results are the application owners' data and are not published
in this repository.
