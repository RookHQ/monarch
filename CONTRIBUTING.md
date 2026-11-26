# Contributing

Thanks for helping improve Monarch. By participating you agree to follow the
[Code of Conduct](CODE_OF_CONDUCT.md). To report a vulnerability, use the
private process in [SECURITY.md](SECURITY.md), not a public issue.

## License of contributions

Monarch is released under the [Apache License, Version 2.0](LICENSE). As
section 5 of the license states, any contribution you submit is licensed under
the same terms, including its patent grant. By submitting a pull request you
also confirm that you have the right to submit it (it is your own work, or you
have permission to contribute it under the Apache License 2.0). Contributions
do not grant any right to the Rook or Monarch names or logos;
see [TRADEMARK.md](TRADEMARK.md). Please do not include code,
assets or text copied from sources with an incompatible license, and do not
include confidential material.

## Getting started

1. Fork the repository and clone your fork.
2. `nvm use && npm ci && npm run verify`.
3. Branch from `uat` and open your pull request against `uat` (see
   [docs/governance/branching.md](docs/governance/branching.md)). Name the
   branch `feat/…`, `fix/…`, `docs/…` or `chore/…`. One concern per PR. For
   anything larger than a small fix, open an issue first so we can agree on
   the approach.
4. Components: see `docs/guides/component-contract.md`. A component is not
   done without typed props, docs comment, keyboard/focus handling, tests
   (interaction + axe + SSR), and a `mn-*` stylesheet using only tokens. The
   folder layout is enforced; see `docs/architecture/SOURCE-LAYOUT.md`.
5. Tokens: never add a raw colour to a component. Add primitives in
   `primitives.ts`, meaning in `themes.ts`; contrast tests must pass.
6. Add a changeset (`npm run changeset`) when you change a published package.
   Pick the bump with the 0.x policy below. Breaking changes need an ADR and
   a migration note.
7. Open a pull request. CI must be green and a maintainer reviews it.
   `CODEOWNERS` lists the review owners.

## Versions (the 0.x policy)

Every package is pre-1.0. A **patch** carries fixes and additive features
(new components, props, variants, tokens) and never a breaking change. A
breaking change ships only in a **minor**, with a migration note in
[docs/guides/migration.md](docs/guides/migration.md). The current
`@rookcom/monarch-react` line stays on 0.14.x, so its changesets are
`patch`. The root and part `mn-*` class names listed in the manifest
(`className`) are public and stable within a minor; internal modifier
classes and `--_*` or undocumented variables are not. Details:
[docs/governance/versioning.md](docs/governance/versioning.md).

## New components

New components follow the path in
[docs/governance/CONTRIBUTION-GUIDELINES.md](docs/governance/CONTRIBUTION-GUIDELINES.md),
starting with a proposal issue.

## Visual baselines

Screenshot baselines are Linux-only and must be generated in the pinned CI
container (see [docs/guides/visual-testing.md](docs/guides/visual-testing.md)).
Do not commit screenshots made on your machine. If your change intentionally
alters how something looks, say so in the pull request: a maintainer runs the
**Update visual baselines** workflow on your branch (it needs write access, so
contributors from forks cannot run it) and the regenerated images are reviewed
in the PR. A change to one component should only change that component's
folder of baselines.

## Releasing (maintainers)

1. Merge PRs with changesets into `uat`; CI publishes a `canary` for each.
   Try the canary in a product before releasing.
2. When ready for stable: on a `release/<version>` branch from `uat` run
   `npm run version-packages`, review the version and changelog diff, commit
   `chore: version packages`, and merge it into `uat`.
3. Open a pull request from `uat` into `main` and merge it with a merge
   commit. CI publishes the new versions to npm with trusted publishing and
   provenance (no token is stored anywhere) and pushes the release tags.
4. An urgent fix on a released version goes on `hotfix/<name>` from `main`,
   into `main`, and is then merged back into `uat`.
