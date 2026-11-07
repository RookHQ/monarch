# @rookcom/monarch-conformance

## 0.4.0

### Minor Changes

- 656151a: Wave 10: destructive and confirmation system (strategic scope, ADR 0011) and privacy and compliance (ADR 0012).

  - New components: `PromptDialog`/`usePrompt`, `ChoiceDialog`/`useChoice`, `UnsavedChangesGuard`/`useUnsavedChangesGuard`/`UnsavedChangesDialog`, `useUndoToast`, `StepUpAuthDialog`/`useStepUpAuth`, `TypeToConfirmInput`/`phraseMatches`, `ConsequenceList`, `WizardDialog`, `DangerZone`/`DangerZoneItem`.
  - New flows in `./patterns`: `DeleteWorkspaceFlow`, `DeleteAccountFlow`, `TransferOwnershipFlow`, `LeaveWorkspaceFlow`, `RemoveMemberFlow`, `BulkDestructiveFlow`, `GdprEraseFlow`, `DeactivateFlow`, `RevokeAccessFlow`.
  - `ConfirmDialog`: `tone="warning"` and a required `acknowledgement` checkbox.
  - `Toast`: `onDismiss` now always fires, even when cleanup runs before the close renders.
  - Conformance: new `dialogs/no-native-dialog` rule (error) for `window.confirm`, `alert` and `prompt`.
  - Privacy and compliance: `ConsentProvider`/`useConsent`/`ConsentGate` with jurisdiction-based regimes (opt-in, opt-out, notice), Global Privacy Control and versioned consent records; `CookieConsentBanner`, `ConsentPreferences`/`ConsentSettingsLink`, `PrivacyChoicesLink`, `MarketingConsentCheckbox`, `TermsAcceptance`, `TermsUpdateDialog`, `DataExportRequest`, `ConsentHistory`, and `DataSubjectRequestForm` in `./patterns`.

### Patch Changes

- 9224222: Clearer description and README for everyone using Monarch.

## 0.3.1

### Patch Changes

- 0e0e6b3: The packages are open source under the MIT license. The npm metadata of the earlier tokens and conformance releases still said `UNLICENSED`; this release declares `MIT`, ships the `LICENSE` file, and adds a License section to each README.

## 0.3.0

### Minor Changes

- New rule `icons/no-sparkles` (error): Rook does not use sparkle icons. Flags `Sparkles`, `Sparkle`, `WandSparkles` and its old name `Wand2` imported from lucide-react, Radix icons, react-icons or Heroicons (aliases included). Use `Bot` for AI features.

## 0.2.0

### Minor Changes

- a2d52a3: Tokens: contrast is now measured on 8-bit colour as browsers paint it, so no pair passes in maths and fails on screen (Rook Blue `info` was 4.48:1 in the browser).

  Conformance: recognises the new components and patterns as Monarch primitives; `monarch-ignore-next-line` skips comment-only lines so it can sit next to `eslint-disable-next-line`; 4-digit numbers like `Order #1042` are no longer reported as colours.

- Packages move to the public npm registry as `@rookcom/monarch-*` (were `@rookhq/monarch-*` on GitHub Packages). Apps install with plain `npm ci`; no registry token is needed. Releases are published from CI with npm trusted publishing and provenance.

### Patch Changes

- 6918d34: Fewer false positives in two accessibility rules. `a11y/click-without-key` no longer flags option, menuitem, tab, treeitem and gridcell items in files that use `aria-activedescendant`, where the focused container handles the keys. `a11y/outline-none-without-ring` accepts a `focus-within` ring, outline, border or shadow on a wrapper up to 15 lines above. Across the 20 registered apps this removes 12 and 21 findings respectively, all checked by hand.
