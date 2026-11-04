# Changesets

Every change to a published package needs a changeset (`npm run changeset`).
`patch` for fixes, `minor` for additions, `major` for anything in
docs/governance/versioning.md's breaking list. CI fails a PR that touches
`packages/*/src` without one.
