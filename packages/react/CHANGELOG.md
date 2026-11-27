# @rookcom/monarch-react

## 0.14.5

### Patch Changes

- d54a464: **New**

  - `@rookcom/monarch-react/rook`: every Rook-specific export (RookBar, RookRail, SignInWithRookButton, the Rook Intelligence transport, the `ROOK_*` constants) now has its own entry and `styles/rook.css`. The old paths in `/shell`, `/patterns` and `/ai` still work and are deprecated, and are removed no earlier than 2027-02-11 (90-day window); `monarch check --fix` rewrites the imports.
  - Realtime voice: `createRealtimeVoiceSession` (WebRTC, caller-supplied signalling, reconnect, barge-in), `createPcmAudioPlayer` for streamed audio and `createVoiceActivityDetector`.
  - `AiGhostText`, `useAiCompletion` and `useAiRewrite`: inline completion with Tab to accept, word-by-word accept and screen-reader announcements.
  - `otelSpansToAiTrace`: OpenTelemetry GenAI spans for AiTraceView (which now windows long traces), AiAgentRunTimeline and AiSubagentTree, with links to conversation parts.
  - `useAiContextUsage` with a pluggable token counter; usage gains cache-read, cache-write and reasoning tokens.
  - Memory in the protocol: a `memory` stream event and message part, `onMemoryEvent`, and `applyAiMemoryEvents` for AiMemoryPanel.
  - Offline and slow networks in `useAiConversation`: messages sent offline queue and send on reconnect (`offlineQueue: false` restores the error), a "taking longer than usual" notice after `slowAfterMs` (10 s), and Resume for a dropped reply.
  - AiComputerView: a live `stream`, and take-over with `onPointer` / `onKey` forwarding and Hand back.
  - The manifest lists a gzip `size` per export, and prop types no longer contain build-machine paths.

  **Fixes from a full audit of every component family** (about 300 defects, each with a test). The ones most likely to matter:

  - Links from data are checked before they render (`javascript:` and similar) in SubscriptionsTable, SavingsPanel, RenewalsPanel, DocumentRelations, PageTree, InstallVerify, EmbeddedAppFrame, SecurityAlertCard, DataExportRequest, FavoritesList, PageHeader actions, CareersJobList, HelpCenterHome, BarList, EventChip, TodayPanel, Leaderboard, LinkedDocsPanel, PostCard, NotificationCenter and SupportPanel.
  - Money: `1.234,56` parses as 1234.56; line totals round exact halves away from zero.
  - NumberInput and CurrencyInput accept their own formatted value in right-to-left locales; PhoneInput, OTPPinInput and PhoneCountrySelect accept digits from other scripts.
  - Hidden form inputs of disabled controls no longer submit (NumberInput, PhoneInput, SegmentedControl, RatingPicker, Slider, RangeSlider, ColorPicker, TagInput). Combobox, PhoneInput and OTPPinInput honour `readOnly`. Keys pressed during IME composition are ignored in Combobox, TagInput, SearchInput and AddressAutocomplete.
  - FocusTrap works with selects, menus and popovers portalled out of it. Popover and Tooltip follow DirectionProvider. Focus is kept after removing rows, chips, reactions and notifications.
  - DataTable: sticky header with a sticky first column no longer stacks headers; `aria-rowindex`; shift-range selection survives sorting. VirtualList and VirtualMessageList measure first-render rows.
  - ImportWizard's problem CSV neutralises formula cells. List editors no longer reuse the id of a saved row. TimesheetGrid saves a cell once. CommandPalette no longer traps the arrow keys on an item listed twice.
  - Scheduling: a time in a spring-forward gap moves forward west of UTC too; MiniMonth keeps focus when the month changes; AgendaList honours `days`; unreadable dates and unknown zones or locales never throw in render.
  - Server rendering: components that format dates or numbers without `locale`/`timeZone` render en-US / UTC until hydrated, then the viewer's.
  - Uploads: AvatarUpload no longer saves twice; a page-scope DropZone no longer delivers files twice; MediaUpload Replace enforces `accept`; a file dropped on a disabled or full zone no longer navigates the browser away.
  - Right-to-left: arrow keys mirror in DashboardGrid, OrgChart, Toolbar, CallControls, EmojiPicker, JSONViewer, UptimeBar and TagInput; chart text keeps its anchors; model-authored text gets `dir="auto"` in 16 more AI components.

  **Behaviour changes to check**

  - `AiPart` has a new `memory` member and `UseAiConversationResult` has `offline`, `slow`, `queued` and `cancelQueued`.
  - AiActionLog, AiConversationList and AiTableAnswer show 200 items with "show more"; AiSourceList and AiStructuredAnswer roots are groups, not regions.
  - KeyboardShortcutsSheet and the SplitInboxLayout drawer open at the inline end (mirrored in right-to-left).
  - SecretField uses `autocomplete="new-password"` while masked; sign-in and sign-up inputs carry `name`.
  - `validateFormField` treats whitespace-only text as empty.

## 0.14.4

### Patch Changes

- 6850b3e: **AI components**

  - Stream race fixed: sending, regenerating, approving a tool or resetting while a reply streams no longer leaves the new reply marked stopped. Each run owns its abort controller and frame scheduler.
  - Full tool lifecycle: `tool-input-delta` events (streamed arguments), parallel tool calls, a client tool-result path (`submitToolResult`, `onToolCall`, `toolPolicy`), and an `awaiting-result` tool state so vendor-format tool calls no longer end as Cancelled. Request builders serialise tool calls, tool results and attachments for the chat-completions and content-block formats.
  - Finish reasons: a `finish` event and `message.finishReason` (`stop`, `length`, `content-filter`, `refusal`, `tool-use`), with notices for truncated, filtered and refused replies. Redacted reasoning and streamed citations are mapped.
  - Errors: HTTP 401/403 → `auth`, 400/422 → `bad-request`, 429 → `rate-limited` (was `rate-limit`), 5xx retryable; unknown errors are no longer retryable; `retryAfterMs` drives the countdown; every transport and hook string is localisable through `messages`, and raw exception text is never shown.
  - SSE: a `\r\n` split across network chunks no longer breaks frames; resumed streams are de-duplicated by event id; `resumeRun` resumes by run id.
  - Attachments: `uploadFile` turns files into parts with progress; the chat page, assistant panel and copilot layout forward files. Without `uploadFile`, sending files is refused with `attachments-unsupported` instead of silently dropping them.
  - Tool approval renders in messages (with optional `risk` and "always allow"), and sending is blocked while an approval waits (`canSend`).
  - Streaming performance: stable message callbacks (`useAiMessageWiring`), cached day keys and rows in AiThread, incremental code tokenising, no full re-parse of streaming code blocks.
  - Screen readers: the thread speaks Markdown as text (code and tables named), "Response complete" follows the whole reply, and error alerts are read once.
  - Untrusted output: generative UI checks every string prop by shape at any depth and supports per-component JSON Schemas (`describeAiUiRegistry` for prompts), streamed props and an `emit`/`onAction` channel; the artifact HTML preview makes links inert and strips refresh, base, scripts and handlers; AiMarkdown gains `allowImageHosts`, `resolveImageUrl`, protocol-relative link blocking and visible link hosts.
  - New stream formats: AI SDK UI-message stream, Responses-style events and AG-UI, each with a request builder. A testing kit (`createStreamFixture`, `sseFromEvents` with chunk-split fuzzing, `replayTransport`, `simulateConversation`). Run notifications (`useAiRunNotifications`) and a browser voice session with barge-in (`createBrowserVoiceSession`, `useAiVoiceSession`).
  - Model text gets `dir="auto"`; the assistant panel's `side` is logical and mirrors in right-to-left pages.

  Behaviour changes to check: the error codes above; new union members `awaiting-result` (tool state) and `awaiting-tool-result` (status) for exhaustive maps; Retry now retries the failed reply in place.

  **Components**

  - `DirectionProvider` (exported from the root) and one shared direction hook: Sheet `side="start" | "end"`, mirrored TreeView and MegaMenu arrows, flipped Pagination, Breadcrumb and menu chevrons, logical CSS throughout.
  - React 18: about 60 components now forward refs (Card, Badge, Alert, Kbd, Skeleton, Spinner, Table parts and more).
  - Server rendering: calendars, inbox rows, activity and notification groups, DatePicker text and chart formatters render a stable server value and switch to the viewer's zone and locale after mount; relative ages tick.
  - Pagination and Breadcrumb: `messages` and `locale` (`PAGINATION_MESSAGES`, `BREADCRUMB_MESSAGES`); DataTable passes `paginationMessages` and `locale` through.
  - Fixes: Tooltip controlled/uncontrolled warning in the rail and SidebarNav (new Tooltip `disabled` prop); ThemeToggle applies a theme changed in another app; DataTable row lookup is O(1); ForceGraph uses Barnes-Hut above 300 nodes and skips simulation on the server; the RookBar phone menu is a real modal dialog; BusinessHoursEditor keeps errors on the right row; support chat no longer duplicates messages; day groups merge regardless of order; TimeGrid drag respects the visible hours; SlotPicker Up/Down move by row; mention `trigger` updates; AnnouncementBar returns focus; SiteHeader closes menus on navigation (`pathname`, `onNavigate`); UnsavedChangesGuard `onNavigate` for client routers; charts no longer throw on very large series; the editor's link policy uses Tiptap 3's `isAllowedUri`.
  - AppShell's default main id is now generated (`useId`); an explicit `mainId` still works.

  **Manifest and docs**

  - `manifest.json` includes props from Radix and other dependencies, `default` and `values` per prop, the real root class, `usesClient` (`clientOnly` is deprecated), and per-export `import`, `css`, `componentCss`, `docs` and `scope`. The docs show defaults and values, every component page names its stylesheets, and `llms.txt` links per-entry files.
  - Peer dependency: `@rookcom/monarch-tokens >=0.5.3`.

## 0.14.3

### Patch Changes

- 10bb3da: Intelligence (`@rookcom/monarch-react/ai`):

  - AiMessageView renders every part type by default: a `plan` part is a read-only step list, and `artifact`, `ui` and `edit-proposal` parts render as AiArtifactCard, AiGenerativeUi and AiEditProposal once `onOpenArtifact`, `uiRegistry` or `getEditHunks` is passed. It is memoised, so a streaming reply no longer re-renders the finished messages above it; pass stable callbacks. New `messages.plan`.
  - `createSseTransport`: one connection core with retry before the first event, resume after a dropped connection (`Last-Event-ID`, when the server numbers its events), an idle timeout and optional tool responses. `chatCompletionsMapper` and `contentBlockMessagesMapper` read the two common stream formats; `readSseFrames` and `toChatMessages` are exported. `rookIntelligenceTransport` runs on the same core and gains `retry` and `idleTimeoutMs`; HTTP 4xx errors other than 429 are no longer marked retryable.
  - AI components inside AiAssistantPanel use their narrow layout for the panel's width (a container named `mn-ai`), not only on narrow screens.
  - `createRecordingRecognition` and `canRecordAudio`: a record-and-transcribe recognizer for AiVoiceInput where the browser has no speech recognition.
  - AiMarkdown keeps maths (`\(…\)`, `\[…\]`, `$$…$$`) as source instead of reading it as Markdown, with a new `renderMath` prop; display maths arrives in `renderCode` as `lang === 'math'`. AiStreamingText passes `renderMath` through.
  - New components: AiConnectorPicker, AiBackgroundTasks, AiSubagentTree, AiSpendCap, AiVoiceMode, AiClaimSources, AiTraceView, AiSharedSession and AiComputerView.

  Every device shape (all components):

  - Touch target floor: controls drawn smaller than 24px (Checkbox, Switch, radio buttons, chip remove buttons, breadcrumb and citation links, editor task checkboxes and others) keep their look and get an invisible 24px tap area (WCAG 2.2, 2.5.8). EventChip is 24px tall on touch screens; a one-character tab and a short Gantt bar are never under 24px.
  - SettingsSection, DangerZone and AiSettingsSection put the header beside the card by their own width, not the screen's: on a settings page with a sidebar the card was squeezed to a sliver on a landscape tablet.
  - Nothing overflows a 360px phone: CallStage, IncomingCall, VersionHistoryPanel, AiRelatedEntities and the LineItemsEditor totals keep their padding inside their width; GoalTree actions and SearchResultItem meta wrap under the text; the BusinessHoursEditor date field keeps a usable width; EmojiPicker never exceeds the screen.
  - Docs site: no sideways scroll on a landscape tablet or on pages with long names and import paths.
  - Fields and the native input inside composite fields (combobox, date picker, tag input) shrink with their row: in Linux and Android fonts an input's built-in width pushed a field past a 360px phone. SlotPicker, InstallVerify, AiSpendCap and the BusinessHoursEditor hours use a single shrinkable column; InstallVerify's handoff buttons wrap; MiniCall never exceeds its container; ReorderableList (and so AiPlan) drops its move buttons under the item on a narrow screen instead of squeezing the text.
  - AiChatPage shows its phone layout from the first paint: before the script ran, a phone briefly showed the 16rem history rail and squeezed the chat to a sliver.
  - New test: `npm run test:devices` loads every component page on five device shapes (a 320px small phone, and a phone and a tablet upright and on their side) and fails on overflow, sideways page scroll, a tap area under 24px or a console error.

## 0.14.2

### Patch Changes

- defb79a: MiniMonth, and so DatePicker and DateRangePicker, can jump to a far month or year without paging month by month: the month title opens a month picker and its year opens a year picker (12 years a page, arrow keys, `min` and `max` respected). The arrows still move one month. New `labels` for the picker text.

## 0.14.1

### Patch Changes

- 8cff175: Toast (feedback): the toast region can be moved clear of a docked panel without touching Monarch internals. `ToastProvider` takes `offset={{ blockStart, blockEnd, inlineEnd }}` (numbers are pixels, strings any CSS length), and a host stylesheet can set the same public variables on any ancestor: `--mn-toast-offset-block-end`, `--mn-toast-offset-block-start` and `--mn-toast-offset-inline-end`. Each defaults to `0`, so placement is unchanged unless they are set. The region now also adds the safe-area insets, so with `viewport-fit=cover` toasts stay clear of a notch or home indicator, and the right positions sit at the inline end (left in right-to-left pages, swiped away to the left).

## 0.14.0

### Minor Changes

- 3c21527: Editor (collaboration, files): `@rookcom/monarch-react/editor` now runs on Tiptap 3, and its optional `@tiptap/*` peer dependencies are `^3.30.4` (was `^2.27.3`). Every Tiptap 2 release, and Tiptap 3 before 3.30.4, lets crafted content set `__proto__` through `mergeAttributes` in `@tiptap/core`; Tiptap 2 will not be fixed, so the only way to stop apps installing the vulnerable code is to require the fixed version.

  This is breaking for apps that install Tiptap: upgrade every `@tiptap/*` package to 3.30.4 or later, and replace `@tiptap/extension-placeholder` and `@tiptap/extension-character-count` with `@tiptap/extensions`, and `@tiptap/extension-task-list` and `@tiptap/extension-task-item` with `@tiptap/extension-list`. Apps that never install Tiptap have nothing to do. Steps are in `docs/guides/migration.md` ("Upgrading to 0.14") and the reasoning in ADR 0015.

  Monarch's own props, exports, classes and data attributes do not change, and `RichTextEditor` keeps the same schema, HTML and keys: the extensions Tiptap 3's StarterKit now adds (underline, a trailing empty paragraph, list keymap and a second link) are turned off. The Tiptap types it passes through (`RichTextEditorInstance`, the `extensions` prop, the editor given to `EditorToolbar`, `BubbleMenu`, `PageOutline` and the block extensions) are Tiptap 3 types. The read-only `RichTextEditor` still has no textbox role. Tiptap 3 is about 24 KB gzip larger, so `RichTextEditor` is now about 155 KB min+gzip.

## 0.13.0

### Minor Changes

- efcd409: Fixes for four problems apps hit when adopting Monarch.

  - Tables and other sideways-scrolling boxes no longer make phone pages scroll sideways. Visually hidden text (an `sr-only` label, `VisuallyHidden`) inside a horizontally scrolling Table escaped the scroll box because the box was not a containing block. `.mn-table-wrap` and every other Monarch container that scrolls horizontally now sets `position: relative`. Nothing else about their layout changes.
  - SidebarNav items take `badgeTone`: `'accent'` (the default, unchanged: a solid bubble for unread or new things, read as "4 new") or `'neutral'` (a quiet muted count styled like a Tabs count, read as the number alone and not drawn as a dot when the sidebar is collapsed). Use `badgeTone: 'neutral'` for counts such as items per category. The default stays `'accent'` because changing a default is a breaking change under the versioning policy. New optional `messages.count` sets the spoken text of neutral counts. New exported type `SidebarNavBadgeTone`.
  - `linkAs` and `linkComponent` now accept Next.js `Link`. The old type made `href` optional, which Next's `Link` (whose `href` is required) does not accept, so `linkAs={Link}` failed to type-check. New exported types `MonarchLinkComponent` (`'a'` or a component that takes a string `href`) and `MonarchLinkProps` are used by Breadcrumb, BottomTabBar, FavoritesList, MegaMenu, SidebarNav and TopNav `linkAs`, and by every shell `linkComponent`. `SidebarNavLinkComponent` and `ShellLinkComponent` remain as aliases. Only `'a'` is accepted as a tag name; other tags never made sense as a link. In Breadcrumb's collapsed menu, a level without `href` is now shown as a disabled item rather than a link with no target.
  - `monarch check` has a new rule, `styles/subpath-css` (warn). It reports components imported from a subpath entry (`@rookcom/monarch-react/patterns`, `/data`, `/charts`, `/ai`, `/shell` …) when no file in the project loads `styles.css`, `styles/<entry>.css` or the matching `styles/components/<id>.css`, and names the import to add. `styles/core.css` alone does not style subpath components. The getting-started guide now has an "if you import from X, also load Y" table.

## 0.12.5

### Patch Changes

- 4209315: ConsentProvider: a stored choice now carries over when the rule that applies changes. A refusal holds under any rule and a choice made under `opt-in` holds under any rule, so a visitor whose location was unknown on one visit and known on the next is not re-enabled or re-asked unexpectedly; an acknowledgement under a lighter rule is still re-asked where consent must be asked for.

## 0.12.4

### Patch Changes

- a189084: CookieConsentBanner: the floating card now uses the same solid raised surface as Dialog and Popover (no translucency or blur), and `bottom-center` is the recommended placement.

## 0.12.3

### Patch Changes

- 8c405e9: Consent: a modern floating-card cookie banner (`position` `bottom-start`, `bottom-end`, `bottom-center`); `ConsentPreferences` `mode="choice"` for products with a single optional category; `ConsentCategory.askFirst` for tools that stay off until the person turns them on; `ConsentProvider` `locating` (decide nothing until the visitor's location is known) and `expiry` (a refusal is remembered 180 days and an acceptance 365 before asking again; the cookie record now lasts 365 days); `ConsentAnalytics` `clarityCategory` and per-tool cookie removal.
- 19b9368: Reword documentation and registry copy for the consent, privacy-request and terms components so it describes what each component does, with one plain line on each page that Monarch provides the interface and what a product must do under any law depends on its own legal advice. The strategic family is now named "Privacy and consent" and the erase flow's display name is "Erase Data Flow". No exports or component ids change.

## 0.12.2

### Patch Changes

- 9da8331: Consent: `ConsentProvider` takes `sync` to keep the choice on the person's account across devices and sites, and the new `ConsentAnalytics` loads Google Analytics or Tag Manager and Microsoft Clarity only after analytics consent, with Google Consent Mode v2 and Clarity consent signals, and removes their cookies when consent is withdrawn.

## 0.12.1

### Patch Changes

- 719093a: Dialog: when the first control is a tab, focus the dialog on open instead of drawing a focus ring on the first tab. DropdownMenuItem and Tabs: development warnings when block elements are placed inside a menu item (they stack) and when tabs are restyled with `data-[state=…]` utility classes (they draw a second indicator).

## 0.12.0

### Minor Changes

- 03eb7b0: Add a global `toast` export that works outside React (API clients, stores, plain modules). It has the same calls as `useToast().toast` plus `toast.dismiss(id?)`, goes to the most recently mounted `ToastProvider`, and queues calls made before one mounts. `toast()` now also accepts `toast(title, options?)`.

## 0.11.0

### Minor Changes

- e459697: Accessibility fixes found by running axe on every docs page in three engines:

  - Accessible names now contain the visible text (WCAG 2.5.3): AccountMenu trigger, AiLauncher, starter prompts, conversation list, mention and slash-command options, SlashMenu options (label plus description), FormBuilder rows, NotificationsBell, RunningTimer (default open label now starts with the visible task and time), ShiftRoster cells and PresenceAvatars faces (`messages.person` gets an optional `initials` argument). `FormBuilder` `messages.edit` is deprecated.
  - TreeView, AiKnowledgeScope and AiAgentBuilder render trees in the flat ARIA form (`aria-level`, `aria-setsize`, `aria-posinset`) instead of nesting children inside parent items.
  - Parts inside one document are no longer landmarks: RuleBuilder, InvoiceView, PayslipView, HelpCenterHome groups and CallStatsPanel. CodeTabs regions are named "<sample>, <set label>". HelpCenterHome's search form is named.
  - Contrast: PresenceAvatars and ConnectedAppsList grey out instead of fading, CallTile's name bar is solid, RecordingIndicator uses the inverse pair in dark, AiLauncher's shortcut is full opacity, SignInWithRookButton keeps contrast while loading, disabled Tag labels use the muted text colour.
  - AiMentionContext's results become a focusable named region while they scroll, and keep the highlighted option in view. EmbedBlock moves fullscreen into `allow` when a provider sets `allow`.
  - Table: while it overflows, the scrolling wrapper becomes a focusable region named by the table (or the new optional `scrollLabel`), so keyboard users can scroll it.
  - Heatmap: values on the two strongest steps sit on a surface chip, so they reach 4.5:1 on every palette slot in every theme.

### Patch Changes

- 0fb9bfb: Per-component stylesheets (`styles/components/<id>.css`) now include the styles of every class the component renders, not only of the folders it imports: Select, EntityPicker, RolePicker and the other pickers render an `.mn-input` trigger and now carry its styles. Apps that load `core.css` were not affected.
- 558e7d2: `.mn-input` sets `appearance: none`, so Select and picker triggers (buttons styled as inputs) no longer keep the platform button look underneath in WebKit on Linux, where their value text measured white on grey.

## 0.10.0

### Minor Changes

- 4ad3972: RookBar: what apps had to work around.

  - `position="fixed"` pins the bar to the top of the window, full width. `zIndex` (or the `--mn-rook-bar-z` custom property) sets its stacking level; the default is still `var(--mn-z-sticky)`.
  - `colorScheme="light" | "dark"` forces a scheme on the bar, its panels, the phone sheet and the account menu, which renders in a portal and so never saw a `data-mn-theme` on the bar.
  - Signed out, the primary action (Sign up) stays beside the menu button from 40rem; the other account controls still move into the sheet under 64rem.
  - `messages.accountShort` and `messages.signOutShort` label the account actions in the phone sheet. They default to `account` and `signOut`.
  - RookBarAppCatalog `asideAction={{ label, href, external? }}` renders a full-width Button link at the end of the aside, and an app's `iconSrc` renders an icon image that Monarch sizes.
  - Every element RookBar renders has an `mn-` class (search field, icon and story images, glyphs), so `styles/tailwind-preflight.css` covers the whole bar. Monarch's internal glyphs now carry `mn-glyph`, and radio inputs in MergeDialog, ImportWizard and AIEvalReview, the AvatarUpload zoom slider, and the SignInWithRookButton and TwoFactorSetup images gained `mn-` classes for the same reason.

### Patch Changes

- 568ede9: Every stylesheet now declares `monarch.reset` as the first Monarch layer. Tailwind 3 apps wrap `@tailwind base` in `@layer monarch.reset { }`, so preflight sits below Monarch in any load order while utilities still win on Monarch elements. `styles/tailwind-preflight.css` is superseded: loaded after utilities it cancelled them on Monarch elements.

## 0.9.2

### Patch Changes

- 8e75a9d: Add `styles/tailwind-preflight.css` for Tailwind 3 apps. Tailwind's unlayered preflight beat Monarch's layered styles, so primary buttons lost their fill and inputs their borders. Load the sheet right after `@tailwind base` (getting-started §4).

## 0.9.1

### Patch Changes

- e1da23c: Fix components with container queries growing very tall, with empty space below them, inside flex and grid parents (DangerZone, TodayPanel, AgendaList, WebhookDeliveries, LineItemsEditor, DashboardGrid and 19 more). Inline-size containment hid their content's width from layout, so a parent sized them about 0px wide; they now fill the available width and fall back to 20rem where there is none.

## 0.9.0

### Minor Changes

- 1fd5ca3: `ToggleGroup` is now its own component (`toggle-group`) with single and multiple selection, `ToggleGroupItem` children, and the old `options`/`value[]` props (except `block`). It replaces the multiple-choice `ToggleGroup` that shipped inside `segmented-control`.

  `CommentThread` from `@rookcom/monarch-react/collaboration` is now the threaded-discussion component. The comments panel's thread data type is exported as `CommentThreadData`.

- 1fd5ca3: Waves 17–19: 143 catalog components (core, domain and business families), so the registry now counts 508 stable entries. New per-component stylesheets for every folder (`styles/components/<id>.css`) plus `styles/base.css`. `PhoneCountrySelect` moves to the root entry, `AppearancePanel` to `./shell` and `SortableGrid` to `./data`. Diff helpers in `./data` are renamed `diffTextLines` and `diffTextWords`.

## 0.8.0

### Minor Changes

- 83b36a7: Wave 16: 127 components promoted from Rook apps (ADR 0014) and the dashboard kit — workflow (Gantt timeline, roadmap, backlog planner, stage journey, org chart, goal tree, timers, timesheets, SLA, approvals, rule/form/sequence builders, escalation policy), collaboration (comments, threads, emoji, typing, receipts, virtual message list, link cards, slash menu, share dialog, post card, inbox row, decision card), calls (stage, tile, controls, lobby, incoming, devices, mini call, stats), files and content (version history, relations, page tree, media upload, video thumbnail), scheduling (slot picker, booking flow, reschedule, time off, toolbar, event popover, shift roster, business hours, team calendar), developer and billing, data grid (column menu/chooser, cursor list, import wizard, preview rows, duplicates, merge), data display (tree view, score ring, provenance, freshness, favicon), charts (heatmap, calendar heatmap, uptime bar, force graph, world map), identity and security, shell (rook bar, docs panel, notifications bell, availability, presence, my status), marketing (help center, CSAT, careers), primitives (rating picker, info tooltip, coming soon, celebration, favorites, shortcuts sheet, mega menu, code block, quick switcher) and the dashboard kit (grid, widget, KPI row, insight strip, period selector, today panel, activity, leaderboard, report viewer, download button).

### Patch Changes

- 0524fc4: `DangerZone`, `SettingsSection` and `AiSettingsSection` no longer stretch their card to the height of a taller parent; the card is as tall as its rows.
- 1cdb3c6: Wave 15 theme pass: every component stylesheet now has a forced-colors block and every animated one a reduced-motion block (enforced by the CSS build); disabled text reaches 2.5:1 on every surface up to `bg-muted`; avatar statuses keep distinct shapes in forced colours; a generated contrast matrix (`docs/architecture/CONTRAST-MATRIX.md`) is checked by `npm run verify`.

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
