# Dependency policy

What the published packages depend on, how a dependency is added or updated,
and which licences are allowed.

## What each package depends on

**`@rookcom/monarch-tokens`**: no runtime dependencies. It works with any
framework or none.

**`@rookcom/monarch-conformance`**: no runtime dependencies. It needs Node
`>=20.11` and never imports the design packages
([ADR 0001](../adr/0001-modular-monorepo.md)).

**`@rookcom/monarch-react`**:

- Runtime dependencies, all Radix primitives (MIT):
  `@radix-ui/react-accordion`, `@radix-ui/react-checkbox`,
  `@radix-ui/react-dialog`, `@radix-ui/react-dropdown-menu`,
  `@radix-ui/react-popover`, `@radix-ui/react-progress`,
  `@radix-ui/react-select`, `@radix-ui/react-separator`,
  `@radix-ui/react-slot`, `@radix-ui/react-switch`, `@radix-ui/react-tabs`,
  `@radix-ui/react-toast` and `@radix-ui/react-tooltip`. Their own
  dependencies (Floating UI, `react-remove-scroll` and others) are listed in
  [THIRD_PARTY_NOTICES.md](../../THIRD_PARTY_NOTICES.md).
- Peer dependencies: `react` and `react-dom` (`^18.3.0 || ^19.0.0`) and
  `@rookcom/monarch-tokens` (`>=0.5.3 <1.0.0` for 0.14.x).
- Optional peer dependencies, needed only for the `./editor` entry: the
  Tiptap 3 packages (`@tiptap/core`, `@tiptap/pm`, `@tiptap/react`,
  `@tiptap/starter-kit`, `@tiptap/extensions`, `@tiptap/extension-image`,
  `@tiptap/extension-link`, `@tiptap/extension-list`,
  `@tiptap/extension-mention` and `@tiptap/suggestion`), all `^3.30.4`. They
  are not bundled; apps that never import `./editor` never need them
  ([ADR 0015](../adr/0015-tiptap-3.md)).

Monarch draws its own icons, charts and date logic, so there is no icon,
chart or date library in the tree.

## Adding a dependency

A new runtime dependency is the exception. Before adding one:

1. Open an issue or note it in the pull request: what it does, why Monarch
   should not do it itself, its size, and how well it is maintained.
2. It must be under a permissive licence compatible with the Apache License 2.0 (see below).
3. It must render on the server and must not touch `window` or `document`
   at module scope.
4. If only one entry needs it, make it an optional peer dependency used by
   that entry alone, as Tiptap is for `./editor`.
5. `@rookcom/monarch-tokens` and `@rookcom/monarch-conformance` stay without
   runtime dependencies.
6. Declare every package that `src` imports in that package's
   `package.json`; do not rely on a package that only arrives through
   another dependency.
7. Update [THIRD_PARTY_NOTICES.md](../../THIRD_PARTY_NOTICES.md) in the same
   pull request, and add a changeset.

`npm run deps:check` (dependency-cruiser) enforces the direction between the
packages: `react` may import `tokens`; `conformance` imports neither.

## Updating dependencies

- Dependabot checks npm packages, GitHub Actions, the Android Gradle build
  (`platforms/android`) and the Swift package (`platforms/ios`) every Monday
  and opens pull requests against `uat`
  ([branching](branching.md#dependencies)).
  Minor and patch updates are grouped (Radix together, development tooling
  together).
- Major versions are not opened by Dependabot. A maintainer upgrades them on
  purpose on a `chore/` branch. Security updates still arrive on their own.
- `@playwright/test` is updated by hand, together with the CI container image
  it must match.
- The lockfile is committed. CI runs `npm audit --omit=dev --audit-level=high`
  and a secret scan on every change, and checks that the build is
  reproducible.
- Raising a peer-dependency floor follows the
  [versioning policy](versioning.md).

## Licences

Every runtime dependency of the published packages and of the documentation
site must be under a permissive licence compatible with the Apache License
2.0 (MIT, BSD, ISC, 0BSD or Apache-2.0; today MIT or 0BSD). No GPL,
LGPL or AGPL code is used or shipped. The documentation site also bundles
one BSD-2-Clause package through the editor examples, with its licence text
reproduced in the notices.

[THIRD_PARTY_NOTICES.md](../../THIRD_PARTY_NOTICES.md) lists every runtime
dependency with its licence and copyright holder. Licences are checked in
review when the file is updated; there is no automated licence check in CI
yet.
