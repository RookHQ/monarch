# Branching

Monarch is a public design system that products install from npm, so a change is tried before it is released.
Work moves through two long-lived branches.

```
feat/forms/date-picker-range ──┐
fix/overlays/dialog-focus-trap ┼──► uat ──► main
docs/theming-guide ────────────┘     │        │
                              canary on npm  stable on npm
```

| Branch | Purpose                                                                                    | Who can merge                                              | npm      |
| ------ | ------------------------------------------------------------------------------------------ | ---------------------------------------------------------- | -------- |
| `main` | What is released. Always matches the latest stable version.                                | The maintainer, by pull request from `uat` (or `hotfix/*`) | `latest` |
| `uat`  | Acceptance. Everything that is going into the next release is merged and tried here first. | The maintainer, by pull request                            | `canary` |

Nobody pushes directly to `main` or `uat`. Both require a pull request, passing checks (`verify`, `secrets`,
`Build docs site`, `branch-policy`, `Native / tokens`, `Native / android`, `Native / ios`; the last three are described in
[native release and maintenance](native-release-and-maintenance.md#required-ci-checks)), a code-owner review (see `.github/CODEOWNERS`), resolved conversations,
no force-pushes and no deletion. Only a repository admin can bypass the review requirement, and that
is reserved for the maintainer.

## Branch names

Short-lived branches are lowercase and start with a type:

| Prefix      | Use                                 | Example                  |
| ----------- | ----------------------------------- | ------------------------ |
| `feat/`     | A new component, prop or token      | `feat/date-picker-range` |
| `fix/`      | A bug fix                           | `fix/dialog-focus-trap`  |
| `docs/`     | Documentation only                  | `docs/theming-guide`     |
| `refactor/` | No behaviour change                 | `refactor/button-styles` |
| `test/`     | Tests only                          | `test/table-keyboard`    |
| `perf/`     | Performance                         | `perf/virtual-list`      |
| `ci/`       | Workflows and tooling               | `ci/cache-playwright`    |
| `chore/`    | Dependencies, housekeeping          | `chore/upgrade-vitest-5` |
| `revert/`   | Undoing a change                    | `revert/button-radius`   |
| `release/`  | Versioning a release                | `release/0.13.0`         |
| `hotfix/`   | An urgent fix on a released version | `hotfix/menu-crash`      |

`dependabot/*` is created by Dependabot. Contributors working from a fork may name their branch freely.
Names are enforced by a repository ruleset, and `branch-policy` checks every pull request.

## Component categories

Monarch has one category per folder under `packages/react/src/components` (and `patterns`):
`actions`, `billing`, `charts`, `collaboration`, `crm`, `data-display`, `data-grid`, `feedback`, `files`, `forms`, `geo`, `identity`, `intelligence`, `interaction`, `layout`, `navigation`, `overlays`, `scheduling`, `search`, `technical`, `typography`, `workflow`.

Do **not** keep a long-lived branch per category. Branches that live for weeks drift away from `uat`,
collect merge conflicts, and hide work from the acceptance step, which is why trunk-based teams avoid them.
Put the category in the name of the short-lived branch instead, and let automation do the rest:

- **Branch name:** `feat/forms/date-picker-range`, `fix/overlays/dialog-focus-trap`. The middle part is
  optional and, when present, must be a real category (`branch-policy` checks it against the folders).
- **Labels:** every pull request is labelled `category: forms` (and so on) from the files it changes, plus
  `area: tokens`, `area: docs` or `area: tooling`. A change across categories gets several labels.
- **Owners:** `.github/CODEOWNERS` has one line per category, so a category can get its own reviewer later.
- **Changesets:** one changeset per change, naming the package; mention the category in its summary.

## How a change travels

1. Branch from `uat`: `git switch uat && git pull && git switch -c feat/your-change`.
2. Open a pull request into `uat`. Add a changeset if a published package changed.
3. After it is merged (squash), CI publishes a `canary`. Try it in a product.
4. To release: on `release/<version>` run `npm run version-packages`, merge it into `uat`, then open a pull
   request from `uat` into `main`. Merge it with a **merge commit** so `uat` and `main` never diverge. CI publishes the stable version.
5. Urgent fix on a released version: branch `hotfix/<name>` from `main`, pull request into `main`, then open a
   pull request from `main` into `uat` so the fix is not lost.

## Dependencies

Dependabot opens one grouped pull request a week against `uat` for minor and patch updates. Major versions are
upgraded on purpose by the maintainer on a `chore/` branch. Security updates still arrive on their own.
