# ADR 0011: Strategic families are in scope ahead of product demand

Status: accepted (2026-11-07), wave 10 (destructive and confirmation system)

## Context

The scope rule in [COMPONENT-SELECTION](../architecture/COMPONENT-SELECTION.md)
admits a component when three or more products need it, or two at P0. The
rule keeps product-specific workflows out of Monarch, and it works for
components that products have already built.

It fails for families where waiting for demand is the problem:

- **Destructive and confirmation.** Rook products open 56 native browser
  dialogs (`window.confirm`, `alert`, `prompt`) across 12 applications, and
  delete-account and delete-workspace flows exist as several diverging copies.
  Each product's copy is counted as "one product's need", so the shared
  version would only enter scope after the copies have spread further.
- **AI.** No product has an AI surface built well enough to count, yet several
  are about to need one. Measuring demand would mean waiting until each
  product has hand-rolled its own.
- **Dashboards.** Every product has a `home` dashboard, each built from
  scratch, with no shared widget frame to count.

## Decision

Add a fourth registry scope, **`strategic`**, for components in a family that
Rook leadership designates ahead of demand.

- Strategic families and their components are listed in
  [`registry/strategic.json`](../../registry/strategic.json): family name,
  wave, the decision record, and per component its id, name, category, layer
  and entry point.
- `scripts/registry/build-components.mjs` adds every listed id to
  `registry/components.json` with `scope: "strategic"` and the family's wave,
  whatever its product count. An id must not also appear in
  `component-demand.json` or `roadmap-waves.json`; the build fails if it does.
- Strategic components follow the same contract as every other component:
  one folder per id, tests, axe, SSR, a complete docs page, `npm run verify`.
  Strategic scope only changes _when_ a component may be built, not _how_.
- A strategic family is added by an ADR that names the evidence and the wave.

The first strategic family is **Destructive & confirmation** (wave 10):
`prompt-dialog`, `choice-dialog`, `unsaved-changes-guard`, `undo-toast`,
`step-up-auth-dialog`, `type-to-confirm-input`, `consequence-list`,
`danger-zone`, and the flows `delete-workspace-flow`, `delete-account-flow`,
`transfer-ownership-flow`, `leave-workspace-flow`, `remove-member-flow`,
`bulk-destructive-flow`, `gdpr-erase-flow`, `deactivate-flow` and
`revoke-access-flow`. `ConfirmDialog` and `useConfirm` already exist and are
completed in the same wave (warning tone, acknowledgement checkbox). The
conformance rule `dialogs/no-native-dialog` ships with it.

AI and Dashboard families are expected to follow with their own ADRs when
their waves start.

## Consequences

- Monarch can build a family before products copy it, which is the point.
- The registry can tell strategic work apart from demand-driven work, so the
  gap analysis still reports demand honestly: the gap summary has a separate
  "Strategic" column.
- The risk is building components no product adopts. Each strategic family
  must name the product surfaces it replaces (for wave 10, the native dialog
  call sites and the duplicated delete flows), and the conformance rule makes
  progress measurable per product.
