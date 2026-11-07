# @rookcom/monarch-react

## 0.7.0

### Minor Changes

- b5b4170: Waves 12–13 in `@rookcom/monarch-react/ai`:

  - Reasoning and agents: `AiReasoning`, `AiChainOfSteps`, `AiPlan`.
  - Tools and human-in-the-loop: `AiToolCall`, `AiToolApproval`, `AiToolResultCard`, `AiEditProposal` (with `diffWords`), `AiInlineDiff`, `AiUndoBanner`/`useAiUndoToast`.
  - Trust: `AiCitation`, `AiSourceList`, `AiSourcePreview`, `AiConfidenceMeter`, `AiGeneratedBadge`, `AiDisclaimer`, `AiFeedback`.
  - Ambient AI: `AiSummaryCard`, `AiInsightCallout` (honest rule vs AI origin), `AiSuggestedActions`, `AiAutofillField`, `AiDraftQueue`, `AiScore`, `AiAssistantPanel`, `AiLauncher`.

- b5b4170: Wave 14 completes the AI catalog in `@rookcom/monarch-react/ai` (85 components and templates in all):

  - Conversation: `AiMessageBranch`, `AiUserMessageEdit`, `AiWelcome`, `AiStarterPrompts`, `AiFollowUpSuggestions`, `AiConversationList`, `AiConversationHeader`, `AiContextDivider`.
  - Input: `AiPromptToolbar`, `useAiSlashCommands`, `useAiMentionContext`, `AiContextChips`, `AiAttachmentTray`, `AiModeSwitcher`, `AiModelPicker`, `AiTonePicker`, `AiPromptTemplatePicker`, `AiVoiceInput`, `AiInlinePrompt`.
  - Rendering: `AiResponseSkeleton`, `AiStructuredAnswer`, `AiTableAnswer` (with `toCsv`).
  - Agents: `AiAgentAvatar`, `AiAgentCard`, `AiAgentPicker`, `AiHandoff`, `AiMultiAgentRoster`, `AiTaskProgress`, `AiAgentRunTimeline`, `AiPermissionScope`, `AiActionLog`.
  - Usage, safety, memory: `AiUsageMeter`, `AiSafetyNotice`, `AiMemoryChip`, `AiMemoryPanel`, `AiContextWindowMeter`, `AiKnowledgeScope`, `AiRelatedEntities`.
  - Artifacts: `AiArtifactCard`, `AiArtifactCanvas` (HTML only in an empty-sandbox iframe), `AiArtifactVersions`, `AiGenerativeUi` (allow-listed registry, sanitised props), `AiImageGeneration`.
  - Templates: `AiChatPage`, `AiCopilotLayout`, `AiAgentBuilder`, `AiEvalReview`; plus `AiOnboardingTour` and `AiSettingsSection`.

## 0.6.0

### Minor Changes

- 656151a: Wave 10: destructive and confirmation system (strategic scope, ADR 0011) and privacy and compliance (ADR 0012).

  - New components: `PromptDialog`/`usePrompt`, `ChoiceDialog`/`useChoice`, `UnsavedChangesGuard`/`useUnsavedChangesGuard`/`UnsavedChangesDialog`, `useUndoToast`, `StepUpAuthDialog`/`useStepUpAuth`, `TypeToConfirmInput`/`phraseMatches`, `ConsequenceList`, `WizardDialog`, `DangerZone`/`DangerZoneItem`.
  - New flows in `./patterns`: `DeleteWorkspaceFlow`, `DeleteAccountFlow`, `TransferOwnershipFlow`, `LeaveWorkspaceFlow`, `RemoveMemberFlow`, `BulkDestructiveFlow`, `GdprEraseFlow`, `DeactivateFlow`, `RevokeAccessFlow`.
  - `ConfirmDialog`: `tone="warning"` and a required `acknowledgement` checkbox.
  - `Toast`: `onDismiss` now always fires, even when cleanup runs before the close renders.
  - Conformance: new `dialogs/no-native-dialog` rule (error) for `window.confirm`, `alert` and `prompt`.
  - Privacy and compliance: `ConsentProvider`/`useConsent`/`ConsentGate` with jurisdiction-based regimes (opt-in, opt-out, notice), Global Privacy Control and versioned consent records; `CookieConsentBanner`, `ConsentPreferences`/`ConsentSettingsLink`, `PrivacyChoicesLink`, `MarketingConsentCheckbox`, `TermsAcceptance`, `TermsUpdateDialog`, `DataExportRequest`, `ConsentHistory`, and `DataSubjectRequestForm` in `./patterns`.

- 2582d2f: Wave 11: AI foundations (ADR 0013), in the new `@rookcom/monarch-react/ai` entry.

  - Vendor-neutral message model (`AiMessage`, `AiPart`) and `useAiConversation` with `rookIntelligenceTransport` and `mockTransport`.
  - Components: `AiThread`, `AiMessageView`, `AiMessageActions`, `AiPromptInput`, `AiStreamingText`, `AiMarkdown`, `AiCodeBlock`, `AiThinkingIndicator`, `AiTypingDots`, `AiStopButton`, `AiErrorMessage`, `useAiScrollAnchor`.
  - Tokens: an `ai` intent (contrast-guaranteed in all four themes), `--mn-gradient-ai`, thinking, tool, citation, diff and confidence roles, and stream motion tokens.

### Patch Changes

- 9224222: Clearer description and README for everyone using Monarch.

## 0.5.1

### Patch Changes

- 0e0e6b3: The packages are open source under the MIT license. The npm metadata of the earlier tokens and conformance releases still said `UNLICENSED`; this release declares `MIT`, ships the `LICENSE` file, and adds a License section to each README.

## 0.5.0

### Minor Changes

- 8f00260: Add the remaining `@rookcom/monarch-react/shell` components: `SupportPanel` (the rail's Help & support panel with in-app support chat through an injected Support Chat API client, help articles, contact options and the status page, plus `useSupportChat`), `ChatDock` (the Rook Chat launcher and kept-alive embed panel, `rook-chat-embed` v1 with origin-checked messaging), `EmbeddedAppFrame` (App Settings embed contract v1 host with sandbox, auto-height, ready handshake and focus handoff, plus the `useEmbedReady` child hook), `NoAppAccess` (the no-access screen on ErrorPage with request access, contact admin and switch workspace) and `LinkedDocsPanel` (docs and links linked to a record).
- 1737667: New subpath entry `@rookcom/monarch-react/shell`: the Rook app chrome every Rook product shares. Its styles are in `@rookcom/monarch-react/styles.css` and its exports in `manifest.json`; registry entries say `entry: ./shell`.

  Wave 7 group A components:

  - `RookRail` (with `rookRailScript`, `ROOK_RAIL_COOKIE`, `ROOK_RAIL_EVENT`): the 48px icon rail that expands to labels and remembers that in the shared `rook_rail` cookie (cookie I/O can be injected). It has brand, workspace and Space switcher slots, sections, the Workspace block with Help & support above it, quick actions, a theme toggle slot, notifications, the account menu, tooltips, arrow-key navigation, and a mobile drawer with a focus trap.
  - `AppShell`: the rail, an optional identity bar and the main region, with a skip link, landmarks and contained scrolling.
  - `IdentityBar`: product name, start, search, actions and notifications slots, plus the account menu.
  - `ThemeToggle` (with `applyThemePreference`, `ROOK_THEME_COOKIE`, `ROOK_THEME_EVENT`): light, dark and system. The choice is saved in the shared `rook_theme` cookie (name and domain are configurable) and applied the same way as the tokens pre-paint script. Hydration-safe.
  - `SpaceSwitcher` and `WorkspaceSwitcher`: searchable popover pickers built on EntityPicker, with the current item, create and manage links, and full keyboard support.

  The manifest now lists SCREAMING_CASE constants such as `ROOK_THEME_COOKIE` as `utility`, not `component`.

- 8ca80c3: Flow patterns (wave 8) in `@rookcom/monarch-react/patterns`: `CustomDomainConnect` (enter a domain with client-side syntax checks, copy DNS records, verify, and show pending, verifying, active or failed with a reason; product-supplied checks and provider hints; `normalizeDomain`, `domainSyntaxError`), `MemberRow` and `MemberList` (avatar, name, email, role select, invited/active/suspended status, resend and confirmed remove), `SchemaDrivenForm` (text, number, select, checkbox, switch, date and textarea fields from a typed schema with required and validation rules, sections, conditional visibility and a read-only view; `schemaDefaultValues`, `isSchemaFieldVisible`, `validateSchemaField`, `validateSchemaForm`, `formatSchemaValue`) and `TemplatePicker` (radio-card gallery with categories, search, a blank option, a preview pane and arrow-key grid navigation; `templateMatches`).
- 4fba214: Wave 9: `@rookcom/monarch-react/editor`, the rich-text layer built on Tiptap 2.27.3 (optional peer dependencies, needed only by this entry).

  - `RichTextEditor`: APG toolbar with `aria-pressed` toggles (bold, italic, strike, code, headings, lists, checklist, link, quote, code block, file), Markdown shortcuts, paste cleaning and safe links, a link popover (⌘/Ctrl+K), placeholder, character limit and counter, read-only and disabled modes, HTML or JSON values, an `onFiles` upload hook, @mentions through `useMentionPicker`/`MentionPicker`, and a static server render.
  - `MessageComposer`: the chat and ticket reply box on RichTextEditor, with Enter (or ⌘/Ctrl+Enter) to send, an attachment row, emoji and action slots, a pending send button, draft persistence, edit and reply-to modes, typing callbacks, disabled-with-reason and a product-supplied mode switch (public reply / internal note).

## 0.4.0

### Minor Changes

- 7bdb3f9: Calendar views in `@rookcom/monarch-react/scheduling`: `EventChip` (one event with colour, time and status, as a button or link), `AgendaList` (events grouped by day with sticky headings and arrow-key movement), `CalendarMonthView` (month grid with "+N more" popovers, keyboard grid navigation and a narrow dot layout), `TimeGrid` (day and week view with overlap layout, all-day row, now line, working-hours shading, keyboard and pointer slot creation, and drag to move) and `WeeklyHoursEditor` (weekly availability with several intervals per day, copy to all days and overlap validation, plus `validateWeeklyHours`).
- 6c76186: Date pickers in `@rookcom/monarch-react/scheduling`: `MiniMonth` (keyboard-navigable month grid following the WAI-ARIA APG date-picker grid, with min/max, disabled days, locale week start, week numbers and event dots), `DatePicker` (typed dates in the locale's format plus a calendar popover, presets, clearable, Field errors), `DateTimePicker` (date plus a 12- or 24-hour time with a step and a time-zone slot; `YYYY-MM-DDTHH:mm` values) and `DateRangePicker` (typed or picked ranges on one or two months with hover preview, presets and min/max length). Values are ISO strings with no time zone, built on internal Intl-based date helpers with no date library.
- fc9e8ef: Pickers (wave 3): Combobox gains multiple selection with chips, grouped options with media and hints, async `onSearch` with debounce, abort and error/retry, a create option, a clear option, `limit`, prefix ranking and an `inline` mode (additive; existing props unchanged). New `EntityPicker` and `AssigneePicker` in the root entry, and `TimezoneSelect` and `DurationInput` (with `getTimezoneInfo`, `listTimezones`, `formatUtcOffset`, `canonicalTimezone`, `parseDuration`, `formatDuration`) in `@rookcom/monarch-react/scheduling`.
- 3d9042e: Wave 4 data components in `@rookcom/monarch-react/data`:

  - **DataTable** (new, column-driven): single and multi sorting, row selection with select-all-on-page, range selection and "select all matching", column visibility, order and resizing (`DataTableColumnChooser`, `useDataTableColumns`), sticky header and first column, density, client or server-driven sorting and pagination, row actions, expandable rows, loading/empty/error states, WAI-ARIA grid keyboard navigation and a card layout for narrow screens. Helpers `sortRows` and `nextSort`. The `Table*` parts stay in the root entry and gain `stickyFirstColumn` and `maxHeight`.
  - **FilterBar / FilterChip** (additive): `search`, `end`, `resultCount`, `activeCount`, `landmark`, `scrollable` and `collapseBelow` (bottom sheet) on FilterBar; toggle (`aria-pressed`), `count`, popover `editor` and `removeLabel` on FilterChip; new `FilterMenu` and `readFilterParams` / `writeFilterParams`.
  - **BulkActionBar** (new): count, actions with overflow, clear selection, polite announcements and a roving-tabindex toolbar.
  - **SavedViews** (new): view switcher with save, save as new, rename, delete, set default and an "Edited" marker; persistence through callbacks.

- 5d3b107: Wave 4 flows: `CommandPalette` (with `useHotkey`, `formatHotkey`, `matchesHotkey`, `isTypingTarget`, `defaultCommandFilter`) and `ReorderableList` (with `moveItem`) in `@rookcom/monarch-react/data`; `Stepper` (with `stepStatus`) in `@rookcom/monarch-react/patterns`.
- 220a201: Adds the first charts to `@rookcom/monarch-react/charts`: `ChartFrame` (title, description, controls slot, legend, loading / empty / error states and an accessible data table with "View as table"), `LineChart` (line and area, multiple series, gaps, reference lines, crosshair tooltip on hover, touch and keyboard, legend toggles, shared crosshair), `BarChart` (grouped or stacked, vertical or horizontal, value labels, highlight, drill-in) and `Sparkline` (line, area or sparkbar with gaps, invert, thresholds and a text alternative), plus the `chartColor` palette helper and the shared chart types. Hand-written SVG, no new dependencies.
- e34c1d6: Wave 5 metrics. New in `@rookcom/monarch-react/charts`: `DeltaIndicator` (up, down or flat change with sign, arrow, spoken direction and a configurable good direction) with the `formatDelta` helper, `BarList` (ranked list with inline bars, links, value formatting, missing values, limit, loading and empty states), `DonutChart` (on ChartFrame: palette slices, centre total, value and share legend, Other folding, data table) and `StackedProportionBar` (one bar of proportions with a legend list, minimum segment width, capacity remainder, and an opt-in status mode where every segment takes a status `tone`) with the `formatShare` helper. `StatCard` gains, additively, Intl value formatting (Money for currency, compact notation), `change` through DeltaIndicator, `unit`, `hint`, `help` popover, `icon`, `sparkline` slot, `tone`, `size`, `variant`, `href` drill-down, loading, empty and error states and `description-list` semantics; new `StatCardGroup` lays out a responsive KPI grid.
- 4dd5871: Wave 6 conversations and activity in `@rookcom/monarch-react/collaboration`: `MessageRow` (author, time, body, grouped, edited, pending, failed with retry, deleted, internal and system variants, reactions, thread replies, hover and focus actions), `MessageTimeline` (polite `role="log"`, day separators, "New messages" divider, older history on scroll-up with the view held still, stick-to-bottom, jump to latest; plus `shouldGroupMessages`, `formatMessageDay` and the separator parts for virtualised lists), `MentionPicker` with `useMentionTextarea` and the editor-agnostic `useMentionPicker` (caret-anchored listbox, async search with abort, people and channel groups, `formatMentionToken` and `parseMentionTokens`), `NotificationCenter` (bell with unread count, All and Unread tabs, day groups, mark one or all read, load more, empty state), `ActivityTimeline` (actor, verb, object, time and marker, day groups, expandable details, filter slot, load more) and `AuditLogViewer` (table of actor, action, target, IP and device, details drawer with field changes, filters slot, export with `auditLogToCsv`, cursor pagination).
- f27a125: Workflow and files (wave 6): `KanbanBoard`, `FileUpload` (with `validateFiles`, `fileMatchesAccept`, `formatBytes`) and `UploadQueue` (with `countUploads`, `totalUploadProgress`) in `@rookcom/monarch-react/collaboration`, and `OnboardingChecklist` in `@rookcom/monarch-react/patterns`. KanbanBoard moves cards by pointer (mouse, pen, touch) or keyboard with announcements, shows counts and WIP limits, collapses columns, offers a "Move to…" menu and makes `onMove` optimistic with rollback.
- 012f4e4: Page structure (wave 8): `PageHeader` gains a back link (`back`, `backHref`), `breadcrumbs`, `status`, a `details` line, a `banner`, `primaryAction` and data-driven `secondaryActions` that collapse into a "More actions" menu (all of them below 640px), `sticky` and `embedded` modes, `headingLevel`, print-hidden actions and a ref. `SettingsSection` gains a Save/Discard footer with `dirty`, `saving` and a polite unsaved-changes status, `stickyFooter`, `loading` and `no-access` states, and a new `SettingsActionRow` for danger-zone actions. `DescriptionList` gains `columns={4}` and `columns="auto"`, inline items that stack on narrow screens, and on `DescriptionItem` copyable values, an empty-value placeholder and clamped values with "Show more"; both forward refs. New in `@rookcom/monarch-react/patterns`: `SectionCard` (titled card with header actions, footer, loading/empty/error states, collapsible), `RecordHeader` (record detail header with avatar, editable title, status, facts, actions, prev/next, state banner and tabs) and `SplitInboxLayout` with `SplitInboxContextToggle` and `useSplitInboxLayout` (list, detail and context panes, resizable by pointer and keyboard, collapsible, single-pane with back navigation below 768px, context drawer below 1280px, F6 pane navigation). All changes to existing components are additive.

## 0.3.0

### Minor Changes

- 02ded2b: Marketing layer. Tokens: fluid display scale, lead size, section rhythm, content widths, derived gradients (`--mn-gradient-wash`, `--mn-gradient-accent`, `--mn-gradient-inverse`), inverse-surface roles (`text-inverse-muted`, `border-inverse`) and the `strong-inverse` intent role, all contrast-tested for every product. React: SiteHeader, MarketingHero, MarketingSection, MarketingSectionHeader, FeatureGrid, ProductFrame, StatStrip, CTABand, FAQ and SiteFooter from the new `@rookcom/monarch-react/marketing` entry; Accordion from the root entry.

  Fix: a product's light-theme accent now wins over the light theme regardless of CSS load order (the light theme selector uses `:where()`).

- 59005b3: Wave 1 (actions group): add `CopyButton` with `useCopyToClipboard` and `writeToClipboard`, `CopyField`, `SecretField`, `StatusBadge` with a typed status registry (`builtInStatuses`, `defineStatuses`, `StatusRegistryProvider`, `resolveStatus`, `formatStatusLabel`) and a contrast guard for custom colours (`checkStatusColor`), and `SegmentedControl` (radio group with roving focus) with its multi-select sibling `ToggleGroup` (`aria-pressed`).
- d83f6e5: Wave 1 core components and upgrades:

  - **InlineEditText** (new): click or Enter to edit, Enter or blur saves, Escape cancels; async save with pending and error states (the draft is kept), `validate`, controlled `editing`, `multiline` with Save/Cancel, and an `onSaveError` / `error` path for product-owned conflict handling.
  - **SectionHeader** and **SectionHeaderLink** (new): unboxed section title with description, meta, actions, a trailing "View all" link, a busy state and a configurable heading level; with `children` it renders the labelled `<section>`.
  - **SwitchSettingsList** and **SwitchSettingsItem** (new): on/off settings that save per row, with optimistic toggle, rollback on failure, retry, and announced "Saved" / error results.
  - **Button**: `success`, `warning` and `brand` variants. `brand` takes `brandColor` and derives fill and text for every theme with the tokens contrast engine (`deriveIntent`), so text meets WCAG AA (7:1 in more contrast). `@rookcom/monarch-tokens` is now imported at runtime (it was already a peer dependency).
  - **Field**: `layout="row"` for a switch (or checkbox) with a description beside it, and **NativeSelect**, the platform `<select>` wired to Field, with a compact `size="sm"` and inline `label` for toolbars.
  - **Tabs**: `count` / `countTone` on TabsTrigger, `scrollable` on TabsList, **TabsNav** + **TabsLink** for route tabs (`aria-current`, router-agnostic `asChild`), **TabsFilter** for filter tabs without per-tab panels, and **useTabsUrlBinding** to bind any tab set to injected URL state.

  All changes to existing exports are additive; existing props, classes and markup are unchanged.

- 6a19dbb: Wave 1 display components:

  - **RelativeTime** (new): `<time datetime>` with `Intl.RelativeTimeFormat` text, the absolute time on hover and focus, optional live updates, injectable `now` for SSR, and `locale` / `timeZone`. Helpers `formatRelativeTime` and `relativeTimeParts`.
  - **Money** (new): `Intl.NumberFormat` currency amounts with minor units (bigint and decimal strings exact), compact notation with an accessible full value, sign display, accounting sign and an opt-in negative tone. Helpers `formatMoney` and `currencyDigits`.
  - **LoadMore** and **useLoadMore** (new): cursor pagination footer with loading, error and retry, end of list, polite announcements and optional auto-load that keeps the button.
  - **BackLink** (new): parent-page link with a back arrow; works with any router through `asChild`.
  - **Avatar** (additive): forwards its ref; numeric `size`, `seed`, `palette="tokens"`, `fallback="icon"` and `icon`, `status` with `statusLabel`; new `AvatarGroup` with "+N" overflow and `avatarPaletteIndex`. A failed image is retried when `src` changes, and a name with no letters or digits now shows the icon instead of "?".
  - **Progress** (additive): `semantics="meter"` (`role="meter"`, `thresholds`, over-limit danger tone), stacked `segments`, and `valueText`. The default progress bar output is unchanged.

  Note: meters (`semantics="meter"`) default to a new `neutral` tone instead of the product accent, so a normal quota never reads as a warning or error.

- c2156fc: Add five input components: `SearchInput` (icon, clear button, debounce, pending state, shortcut), `TagInput` (chips, Enter/comma entry, paste splitting, validator, suggestions listbox, keyboard removal), `UsernameAvailabilityInput` with the `useAsyncValidation` hook (injected debounced validator, status adornment, announced result), `OTPPinInput` (configurable length, paste, `autocomplete="one-time-code"`, numeric keypad, `onComplete`, mask) and `LocationFields` (country / region / city / postal code with the data source injected through `LocationAdapter`).
- 31753b0: Wave 2 (notices): add `CalloutBanner` (page- or section-level banner with tones, `page`/`section` placement, a `scope` variant, an action slot with `end` or `inline` placement, a landmark choice, `live` announcements, and controlled or uncontrolled dismissal that leaves persistence to the product) and `InlineNotice` (one-line tone message with a trailing action, `subtle`/`plain` variants, tone-based `live` and dismissal). Toast: fix the live-region bug (the toast `<li>` no longer carries `role="status"`/`role="alert"`, which broke the viewport list and announced twice; politeness now follows the tone through Radix's announcer, overridable with `politeness`); the region is named with its hotkey ("Notifications (F8)"); new `hotkey`, `durationScale` (WCAG 2.2.1), `limit` and `dismissLabel` provider props; toasts with an action default to 10s; Dismiss buttons are described by their toast title; `action.altText`, `onDismiss`, `toast.success/error/warning/info` shorthands, `dismiss()` for all; and `getErrorMessage()` to turn API errors into readable text.
- a69e649: Wave 2 overlays: new `ConfirmDialog` (with `useConfirm`), `Sheet` and `ResponsiveDialog`. `DialogContent` gains `footer`, `busy`, `closeOnOutsideClick`, `escapeGuard` and `fullScreenOnPhone`; destructive `DropdownMenuItem`s now tint their icon too, and the DropdownMenu docs are complete.

  Behaviour note: `escapeGuard` is on by default for `DialogContent` (and `SheetContent`). Escape pressed while focus is on a combobox or menu button whose own popup is open (`aria-expanded="true"`) no longer also closes the dialog; only the popup closes. This only changes what was previously a bug. Pass `escapeGuard={false}` to restore the old behaviour.

- dfba367: Wave 2 feedback states: new `ErrorPage` (404, 403, 500, offline and maintenance pages that work without the app shell) and `InlineErrorRetry` (compact inline failure with a pending retry, re-announcement and focus return). `EmptyState` gains `kind` (first-run, filtered with Clear filters, success), `layout="line"`, `size="compact"`, `tip`, children and more heading levels. `ErrorState` gains `kind`, `retrying`, `requestId`, plus `ErrorStateBoundary`, `errorKindFromStatus` and `errorKindIcon`. `LoadingState` moves to its own `loading-state` folder (same export, props and class names) and gains the `rows` and `spinner` variants. All three now forward refs.

### Patch Changes

- e6865ac: Input, Textarea and every control built on `.mn-input` (Select trigger, Combobox, SearchInput) now use `box-sizing: border-box`, so `width: 100%` fills the container instead of overflowing it by its padding and border in apps without a global box-sizing reset.

## 0.2.0

### Minor Changes

- Packages move to the public npm registry as `@rookcom/monarch-*` (were `@rookhq/monarch-*` on GitHub Packages). Apps install with plain `npm ci`; no registry token is needed. Releases are published from CI with npm trusted publishing and provenance.
- 8177b95: Add Toast (ToastProvider, useToast), Table primitives with sortable headers and selection cells, Breadcrumb, Pagination, Combobox, Separator, Progress and Link, plus the first experience patterns: PageHeader, Toolbar, FilterBar/FilterChip, SettingsSection/SettingsRow, DescriptionList, SplitLayout, StatCard, ErrorState and LoadingState. DropdownMenuItem now supports `asChild`.
