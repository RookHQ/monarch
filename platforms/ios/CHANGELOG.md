# Changelog

Monarch for iOS has the version of `@rookcom/monarch-react`, in the `0.14.x`
patch series, and shares it with Monarch for Android. Each release is a plain
`0.14.N` git tag, which Swift Package Manager reads. The format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and the rules are in
[native release and maintenance](../../docs/governance/native-release-and-maintenance.md#changelog-standard).

## 0.14.6 (unreleased)

The first release of Monarch for iOS. Not tagged yet, so not yet available by
package URL; see "Installing" in the README. Work done before the version was
unified was numbered `0.1.0` and was never released.

### Added

- Thirteen components that were documented as web only now ship on iOS, each with a catalogue page, tests and a
  docs preview: `MonarchLogoCloud`, `MonarchMarketingHero`, `MonarchMarketingSection` (with
  `MonarchMarketingSectionHeader`, `MonarchFeatureGrid`, `MonarchProductFrame`, `MonarchStatStrip`, `MonarchFaq` and
  `MonarchCtaBand`), `MonarchSiteShell` and `MonarchSiteFooter`, `MonarchMegaMenu` (panels as popovers),
  `MonarchPricingSection`, `MonarchRookBar` (Rook-scoped; menus in a full-height sheet), `MonarchSkipLink` (moves
  VoiceOver focus through `AccessibilityFocusState`), `MonarchCareersJobList` (details in a sheet), `MonarchChatDock`
  (a floating launcher that opens the chat in a half-height sheet), `MonarchComparisonSection`,
  `MonarchCookieConsentBanner` (opt-in, opt-out and notice regimes) and `MonarchEmbeddedAppFrame` (loading, ready and
  failed states around the app's own content). Nothing existing changed.

- Parity additions (round 13, lane B), new overloads and modifiers only, nothing existing changed:
  `View.monarchHelperAbove(_:)` shows the helper text of `MonarchFormField` and `MonarchTextField` between the label and the
  control, as the web forms do; `MonarchRadioCardGroup.init(_:selection:options:columns:compactColumns:isRequired:)` keeps
  columns on a compact width and shows the required mark (side by side, an option with an icon is a tile with the icon over
  the text). Behaviour that follows the web example without an API change: `MonarchAiModelPicker` opens a dropdown under its
  trigger (a popover, also on iPhone) instead of a sheet; `MonarchAiConversationList` shows New conversation first and an inline
  "Search" field instead of the navigation search drawer (its built-in search text is now "Search"); `MonarchCodeBlock` keeps
  the file name, Wrap lines and Copy on one row; `MonarchEditableCell` shows only the field while editing (Return saves, Escape
  cancels, Save and Cancel stay as VoiceOver actions); `MonarchDataGrid` and `MonarchDataTable` fit their columns to the width
  of a phone when they can, with smaller text when they must, and scroll with the first column pinned only when they cannot;
  `MonarchDocumentRelations` and `MonarchDuplicatePanel` use compact pills and plain rows; `MonarchDecisionCard` shows
  choices without descriptions as a row of tiles; `MonarchDeviceList` rows are tiles with an icon tile.

- `MonarchPasswordInput.init(_:text:prompt:helper:error:purpose:isRequired:isRevealed:revealLabel:)` shows the required mark; `MonarchMessageComposer.init(_:text:placeholder:showsEmojiButton:showsFormatting:labels:onSend:)` can hide the formatting row. Both are new overloads; nothing existing changed.

- `MonarchAiPromptTemplatePickerLabels.next` and `.pickFirst`: the Continue button and the "Choose a template to continue." hint
  that the picker now shows under its list, as the web does. Existing calls compile.
- Parity additions, all with defaults so existing calls compile: `MonarchButtonSize.xs`; the type roles
  `MonarchTextStyle.bodySmall` and `.bodyExtraSmall`; `MonarchBacklogBucket.badgeTone` and `.actionTitle` with
  `MonarchBacklogPlanner(onBucketAction:)`; `tone` on `MonarchSearchResult` and `MonarchSearchResultItem`;
  `group` and `status` on `MonarchAssigneePerson`; `group` on `MonarchAiContextItem`;
  `MonarchResizablePanels(collapsesFirst:)`; `MonarchProseTable` with `MonarchProseStyle.key` and `.highlight`;
  `MonarchConditionOperator.anyOf` and `.noneOf`, `MonarchConditionValue.options`,
  `MonarchConditionFieldType.tags` and `MonarchConditionField.group`;
  `MonarchAiMemoryPanelLabels.forgetSelected`; `MonarchAiToolApproval(argumentOrder:)`. A `switch` over
  `MonarchButtonSize`, `MonarchTextStyle`, `MonarchConditionOperator`, `MonarchConditionValue` or
  `MonarchConditionFieldType` needs the new cases or a `default`.
- Parity additions (round 6), all with defaults so existing calls compile: `MonarchInstallVerifyLabels.emailDeveloper`, and the
  developer handoff is now a copy button plus an Email them link (the built-in text of `shareInstructions` is "Copy instructions
  for your developer"); `MonarchPendingInvitesListLabels.resend` and `.revoke`, with Copy link, Resend and Revoke as labelled buttons;
  `MonarchSlashMenu(visibleRows:)`; `MonarchLinkedDocsPanel` shows Link doc and Add link as buttons and the space filter as chips.
- `EnvironmentValues.monarchStartsPresented` (default `false`): when `true`, `MonarchCombobox`, `MonarchEntityPicker`, `MonarchSpaceSwitcher` and `MonarchReceiptPreview` start with
  their picker open, so docs previews and snapshots show the open state. It changes nothing for an app that does not set it.
- Optional product `MonarchFontsPoppins` (`MonarchPoppins`,
  `MonarchTypography.poppins`): the Poppins typeface, SIL Open Font License 1.1,
  with the licence text. It registers its font files itself. The default
  typeface stays the system font.
- The Monarch visual layer: `MonarchSelectField` with `MonarchSelectStyle`
  (`.field` and `.compact`), `MonarchSegmentedControl`, `MonarchListRow`, a
  radio style, and field, button and tab styles at the Monarch heights (field
  44 pt at the default size, small 40, large 52; the hit area stays 44 pt). The
  system switch stays, tinted. Sheets, menus and pickers stay the system's.
- Docs previews and sample apps use SF Symbols in the regular weight. The
  library bundles no icon files.
- DocC reference of `MonarchUI`, published at `/ios/api/` on the docs site. It
  covers the stable API; experimental declarations are listed in
  `api/MonarchUI.experimental.txt` and on the component pages.
- Release: a plain `0.14.N` tag, created by the release workflow once the npm
  release of that version exists and the Native workflow has passed; Swift
  Package Manager reads it through the generated root `Package.swift`.
- `ToolbarContent.monarchFlat()` draws a bar item without the iOS 26 glass pill, and `View.monarchSearchable(text:prompt:)` keeps a search field in the navigation bar instead of a floating pill over the rows.

- Swift package `Monarch` with the libraries `MonarchTokens` (generated
  constants, Foundation only) and `MonarchUI` (SwiftUI). iOS 17 and later,
  Swift 6 language mode, no dependencies.
- Theme: `MonarchTheme` in the environment (`\.monarchTheme`), installed by
  `monarchTheme(scheme:contrast:accent:typography:)`. Scheme, contrast and
  accent resolve to one of the four themes by a pure function
  (`MonarchAppearance.resolve`). Increase Contrast selects the high-contrast
  themes. The modifier sets `tint`.
- Foundations: nine type roles (`MonarchTextStyle`, `monarchTextStyle(_:)`)
  that scale with Dynamic Type, spacing, radii, elevation
  (`monarchElevation(_:fill:in:)`), motion (`MonarchMotion`) and `MonarchTone`.
- Accessibility helpers: `monarchMinimumHitTarget()`, `monarchFocusRing(_:in:)`
  and `monarchAnimation(_:easing:value:)`, which honours Reduce Motion.
- Components: `MonarchButtonStyle` and `MonarchButton`, `MonarchTextField`,
  `MonarchCheckboxToggleStyle`, `MonarchCard`, `MonarchDivider`,
  `MonarchProgressViewStyle` (linear), `MonarchIcon`, `MonarchBadge` and
  `MonarchBanner`.
- Documented native adoption, with nothing shipped: `Toggle` (switch),
  `Picker` (single choice), circular `ProgressView`, and navigation, lists,
  sheets, menus and date pickers.
- English strings for the built-in accessibility text, one table per
  component.
- `inventory/<id>.json`, one documentation file per web component id, with a
  JSON Schema. `components.json`, which the documentation site reads, is
  generated from them. Every example is compiled and rendered by the tests.
- Catalogue app in `Sample/`, with navigation by family and one page per
  component.
- Structure for parallel work: components are grouped by family
  (`Components/<Family>/<Name>/`), and the inventory, compiled examples, test
  fixture registry and sample navigation are generated by
  `Scripts/generate.py` from per-component files, so adding a component only
  adds files. See `docs/ADDING-COMPONENTS.md`.
- `Scripts/verify.sh`: inventory validation, lint, host build and tests,
  public API baseline, simulator build and tests, documentation build and the
  sample build; `--component <Name>` for a fast per-component loop and
  `--local` for a working copy. `Scripts/merge-check.sh` regenerates
  everything and updates the API baseline after a merge.
- Experimental API is marked by the compiler: the public declarations of an
  experimental component are `@_spi(Experimental) public` and are visible only
  to a file that writes `@_spi(Experimental) import MonarchUI`. The status of
  each component is the same as on Android (`inventory/<id>.json`, checked by
  `npm run native:status:check`), and every entry has a `layer`
  (`components` or `patterns`).
- Shared model names with Android: `MonarchCondition`, `MonarchConditionField`,
  `MonarchConditionFieldType`, `MonarchConditionOperator`,
  `MonarchConditionValue`, `MonarchConditionProblem` and
  `MonarchConditionMatch` for conditions (`MonarchQueryGroup` and
  `MonarchQueryCombinator` stay for the query builder), and the typed
  `MonarchActionProblem` from `MonarchActionEditor.problems(of:types:)`, with
  `MonarchActionEditor.message(for:in:labels:)` for its text.
- Time zone parameters are `TimeZone?`, where `nil` is the device zone
  (`MonarchTimeOffPanel`); `MonarchIncidentTimeline` defaults to UTC. A time
  zone in a stored or sent model stays an IANA `String`.

### Changed

- Experimental declarations (65 components, 276 declarations) are
  `@_spi(Experimental) public`, so the stable API baseline lists only stable
  API. MONARCH-COMPAT: an app that uses an experimental component adds
  `@_spi(Experimental) import MonarchUI` to the files that use it; stable API is
  unaffected.
- Shared names with Android: the condition model (`MonarchCondition`,
  `MonarchConditionField`, `MonarchConditionFieldType`,
  `MonarchConditionOperator`, `MonarchConditionValue`,
  `MonarchConditionProblem`, `MonarchConditionMatch`) replaces the `MonarchQuery*`
  names for conditions, and `MonarchActionEditor.problems(of:types:)` returns
  typed `MonarchActionProblem` values. MONARCH-COMPAT: source that used the
  earlier names does not compile; rename them.
- Time zone parameters of components are `TimeZone?`, where `nil` is
  `TimeZone.autoupdatingCurrent`; `MonarchIncidentTimeline` defaults to UTC.
  MONARCH-COMPAT: pass a `TimeZone` where an IANA `String` was passed.
- The AI chat surfaces follow the web and Android: context chips sit above
  the thread, the chat page opens on its history list in compact width, and the
  thread panel's bottom bar (typing indicator and composer) shows in every
  state. MONARCH-COMPAT: an app that relied on the earlier order or default
  sees the web's order.
- The field label sits above the box, the focus ring alone shows focus, and
  disabled controls use opaque quiet colours. MONARCH-COMPAT: snapshot tests of
  an app that includes Monarch components need new baselines; roles, labels and
  parameters did not change.
- The dark theme's subtle background and default border are now distinct from
  the surface and muted fills (the tokens fix of `@rookcom/monarch-tokens`).
  MONARCH-COMPAT: dark screens show slightly different greys; no action needed.
- `MonarchAiGhostText`, `MiniMonth`, the import wizard and the share dialog are
  stable (`available`) now.
