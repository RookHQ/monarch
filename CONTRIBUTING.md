# Contributing

Thanks for helping improve Monarch. By participating you agree to follow the
[Code of Conduct](CODE_OF_CONDUCT.md). To report a vulnerability, use the
private process in [SECURITY.md](SECURITY.md), not a public issue.

## License of contributions

Monarch is released under the [MIT License](LICENSE). By submitting a pull
request you agree that your contribution is licensed under the same terms, and
that you have the right to submit it (it is your own work, or you have
permission to contribute it under the MIT license). Please do not include code,
assets or text copied from sources with an incompatible license, and do not
include confidential material.

## Getting started

1. Fork the repository and clone your fork.
2. `nvm use && npm ci && npm run verify`.
3. Branch from `main`. One concern per PR. For anything larger than a small
   fix, open an issue first so we can agree on the approach.
4. Components: see `docs/guides/component-contract.md`. A component is not
   done without typed props, docs comment, keyboard/focus handling, tests
   (interaction + axe + SSR), and a `mn-*` stylesheet using only tokens. The
   folder layout is enforced; see `docs/architecture/SOURCE-LAYOUT.md`.
5. Tokens: never add a raw colour to a component. Add primitives in
   `primitives.ts`, meaning in `themes.ts`; contrast tests must pass.
6. Add a changeset (`npm run changeset`) when you change a published package.
   Breaking changes need an ADR and a migration note.
7. Open a pull request. CI must be green and a maintainer reviews it.
   `CODEOWNERS` lists the review owners.

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

1. Merge PRs with changesets to `main`; CI publishes a `canary` for each.
2. When ready for stable: `npm run version-packages`, review the version and
   changelog diff, commit `chore: version packages`, push to `main`.
3. CI publishes the new versions to npm with trusted publishing and provenance
   (no token is stored anywhere) and pushes the release tags.
