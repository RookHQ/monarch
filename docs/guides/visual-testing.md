# Visual testing

Visual baselines catch unintended visual changes: CI compares each gallery
component against a committed screenshot and fails on a difference above 0.2%
of pixels.

## What is screenshotted

Each `GALLERY` id (`apps/docs/src/pages/gallery-ids.ts`) is rendered alone at
`#/__gallery/<id>` and captured as an element, so one component's changes never
move another's pixels.

| Shot                                | Why                                                 |
| ----------------------------------- | --------------------------------------------------- |
| light, 1280                         | the default look                                    |
| dark, 1280                          | the other colour scheme                             |
| hc-dark, 1280                       | the strongest contrast mode                         |
| light, 390                          | the phone layout                                    |
| base, calendar, chat accents, light | only for `button`, `switch`, `tabs`, `progress-bar` |

Product accents are colour-only and are verified for every component and theme
by the token contrast tests (`packages/tokens/test`), so screenshotting every
accent for every component adds files without adding coverage. Open overlays
(menus, dialogs, popovers) have their own shots under `overlays/`, and the
marketing reference page has five full-page shots.

## Linux only

Baselines are committed for Linux only, the pinned CI container
(`mcr.microsoft.com/playwright:v1.56.1-noble`). Text rasterisation differs
between operating systems, so a second set for macOS would double the files
without being checked anywhere. On macOS the visual project is skipped. To
compare against your own local screenshots while working, run
`MONARCH_VISUAL_LOCAL=1 npm run test:visual:update` once and then
`MONARCH_VISUAL_LOCAL=1 npm run test:visual`; local `-darwin.png` files are
git-ignored.

## Updating baselines

Baselines must be generated in the pinned container, so contributors do not
commit screenshots from their own machines. If your change intentionally alters
how a component looks, say so in the pull request: a maintainer regenerates the
baselines on your branch and the new images are reviewed in the PR like code.
(Pull requests from forks cannot run the update workflow, which needs write
access to the branch.)

Maintainers run the **Update visual baselines** workflow (`workflow_dispatch`,
write access required) on the branch:

```sh
gh workflow run update-visual-baselines.yml --ref <branch>
```

It regenerates the Linux screenshots in the pinned container, commits only the
files that changed back to the branch (as `github-actions[bot]`) and starts CI
on the new commit. Pull the branch afterwards (`git pull`) and review the
images in the PR.

A change to one component must only change that component's folder. If other
folders change, find out why before merging.

The workflow is two jobs. The first runs the branch's code (install, build,
tests) with a read-only token. The second never runs your branch's code: it
accepts only `-linux.png` files from the first job's artifact, copies them in,
commits and pushes. It refuses to run on `main`.

Fallback: run the CI workflow with `update_snapshots: true`, download the
`visual-snapshots-linux` artifact and copy in only your component's files.

## Size budget

Each component adds 4 PNGs (a few tens of KB each). `npm run visual:budget`
(part of `npm run verify` and CI) enforces it on the git-tracked files:

| Rule                         | Limit                          |
| ---------------------------- | ------------------------------ |
| Platforms                    | only `-linux.png` is committed |
| Images per gallery component | 7 (4 standard + 3 accents)     |
| Size of one image            | 1 MB                           |
| Whole baseline folder        | 30 MB                          |

Today it is about 490 images and 18 MB. Raise a limit only in a reviewed PR
that explains what new coverage needs it. Before adding variants, check
whether a unit or token test can cover the case instead.
