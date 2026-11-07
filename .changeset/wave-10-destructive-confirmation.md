---
'@rookcom/monarch-react': minor
'@rookcom/monarch-conformance': minor
---

Wave 10: destructive and confirmation system (strategic scope, ADR 0011) and privacy and compliance (ADR 0012).

- New components: `PromptDialog`/`usePrompt`, `ChoiceDialog`/`useChoice`, `UnsavedChangesGuard`/`useUnsavedChangesGuard`/`UnsavedChangesDialog`, `useUndoToast`, `StepUpAuthDialog`/`useStepUpAuth`, `TypeToConfirmInput`/`phraseMatches`, `ConsequenceList`, `WizardDialog`, `DangerZone`/`DangerZoneItem`.
- New flows in `./patterns`: `DeleteWorkspaceFlow`, `DeleteAccountFlow`, `TransferOwnershipFlow`, `LeaveWorkspaceFlow`, `RemoveMemberFlow`, `BulkDestructiveFlow`, `GdprEraseFlow`, `DeactivateFlow`, `RevokeAccessFlow`.
- `ConfirmDialog`: `tone="warning"` and a required `acknowledgement` checkbox.
- `Toast`: `onDismiss` now always fires, even when cleanup runs before the close renders.
- Conformance: new `dialogs/no-native-dialog` rule (error) for `window.confirm`, `alert` and `prompt`.
- Privacy and compliance: `ConsentProvider`/`useConsent`/`ConsentGate` with jurisdiction-based regimes (opt-in, opt-out, notice), Global Privacy Control and versioned consent records; `CookieConsentBanner`, `ConsentPreferences`/`ConsentSettingsLink`, `PrivacyChoicesLink`, `MarketingConsentCheckbox`, `TermsAcceptance`, `TermsUpdateDialog`, `DataExportRequest`, `ConsentHistory`, and `DataSubjectRequestForm` in `./patterns`.
