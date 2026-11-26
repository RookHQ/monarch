# @rookcom/monarch-docs

## 0.0.30

### Patch Changes

- d54a464: **New**

  - `@rookcom/monarch-react/rook`: every Rook-specific export (RookBar, RookRail, SignInWithRookButton, the Rook Intelligence transport, the `ROOK_*` constants) now has its own entry and `styles/rook.css`. The old paths in `/shell`, `/patterns` and `/ai` still work and are deprecated until 0.15.0; `monarch check --fix` rewrites the imports.
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

- Updated dependencies [d54a464]
  - @rookcom/monarch-react@0.14.5

## 0.0.29

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

- Updated dependencies [6850b3e]
- Updated dependencies [6850b3e]
  - @rookcom/monarch-react@0.14.4
  - @rookcom/monarch-tokens@0.5.4

## 0.0.28

### Patch Changes

- Updated dependencies [f7b9220]
  - @rookcom/monarch-tokens@0.5.3

## 0.0.27

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

- Updated dependencies [10bb3da]
  - @rookcom/monarch-react@0.14.3

## 0.0.26

### Patch Changes

- 213dab8: The cookie consent banner, preferences dialog and analytics tag loader load after hydration, so they are no longer in every page's initial JS on the hosted site (about 5 KB gzip less per page).
- Updated dependencies [defb79a]
  - @rookcom/monarch-react@0.14.2

## 0.0.25

### Patch Changes

- Updated dependencies [8cff175]
  - @rookcom/monarch-react@0.14.1

## 0.0.24

### Patch Changes

- Updated dependencies [3c21527]
  - @rookcom/monarch-react@0.14.0

## 0.0.23

### Patch Changes

- Updated dependencies [efcd409]
  - @rookcom/monarch-react@0.13.0

## 0.0.22

### Patch Changes

- Updated dependencies [4209315]
  - @rookcom/monarch-react@0.12.5

## 0.0.21

### Patch Changes

- Updated dependencies [a189084]
  - @rookcom/monarch-react@0.12.4

## 0.0.20

### Patch Changes

- Updated dependencies [8c405e9]
- Updated dependencies [19b9368]
  - @rookcom/monarch-react@0.12.3

## 0.0.19

### Patch Changes

- Updated dependencies [9da8331]
  - @rookcom/monarch-react@0.12.2

## 0.0.18

### Patch Changes

- Updated dependencies [719093a]
  - @rookcom/monarch-react@0.12.1

## 0.0.17

### Patch Changes

- Updated dependencies [03eb7b0]
  - @rookcom/monarch-react@0.12.0

## 0.0.16

### Patch Changes

- Updated dependencies [e459697]
- Updated dependencies [0fb9bfb]
- Updated dependencies [558e7d2]
  - @rookcom/monarch-react@0.11.0

## 0.0.15

### Patch Changes

- Updated dependencies [568ede9]
- Updated dependencies [4ad3972]
  - @rookcom/monarch-tokens@0.5.2
  - @rookcom/monarch-react@0.10.0

## 0.0.14

### Patch Changes

- Updated dependencies [8e75a9d]
  - @rookcom/monarch-react@0.9.2

## 0.0.13

### Patch Changes

- Updated dependencies [e1da23c]
  - @rookcom/monarch-react@0.9.1

## 0.0.12

### Patch Changes

- Updated dependencies [1fd5ca3]
- Updated dependencies [1fd5ca3]
  - @rookcom/monarch-react@0.9.0

## 0.0.11

### Patch Changes

- Updated dependencies [0524fc4]
- Updated dependencies [1cdb3c6]
- Updated dependencies [83b36a7]
  - @rookcom/monarch-react@0.8.0
  - @rookcom/monarch-tokens@0.5.1

## 0.0.10

### Patch Changes

- Updated dependencies [b5b4170]
- Updated dependencies [b5b4170]
  - @rookcom/monarch-react@0.7.0

## 0.0.9

### Patch Changes

- Updated dependencies [9224222]
- Updated dependencies [656151a]
- Updated dependencies [2582d2f]
  - @rookcom/monarch-tokens@0.5.0
  - @rookcom/monarch-react@0.6.0

## 0.0.8

### Patch Changes

- Updated dependencies [0e0e6b3]
  - @rookcom/monarch-tokens@0.4.1
  - @rookcom/monarch-react@0.5.1

## 0.0.7

### Patch Changes

- Updated dependencies [8f00260]
- Updated dependencies [1737667]
- Updated dependencies [8ca80c3]
- Updated dependencies [4fba214]
  - @rookcom/monarch-react@0.5.0

## 0.0.6

### Patch Changes

- Updated dependencies [7bdb3f9]
- Updated dependencies [6c76186]
- Updated dependencies [fc9e8ef]
- Updated dependencies [3d9042e]
- Updated dependencies [5d3b107]
- Updated dependencies [220a201]
- Updated dependencies [220a201]
- Updated dependencies [e34c1d6]
- Updated dependencies [4dd5871]
- Updated dependencies [f27a125]
- Updated dependencies [012f4e4]
  - @rookcom/monarch-react@0.4.0
  - @rookcom/monarch-tokens@0.4.0

## 0.0.5

### Patch Changes

- Updated dependencies [e6865ac]
- Updated dependencies [02ded2b]
- Updated dependencies [59005b3]
- Updated dependencies [d83f6e5]
- Updated dependencies [6a19dbb]
- Updated dependencies [c2156fc]
- Updated dependencies [31753b0]
- Updated dependencies [31753b0]
- Updated dependencies [a69e649]
- Updated dependencies [dfba367]
  - @rookcom/monarch-react@0.3.0
  - @rookcom/monarch-tokens@0.3.0

## 0.0.4

### Patch Changes

- Updated dependencies
  - @rookcom/monarch-tokens@0.2.3

## 0.0.3

### Patch Changes

- Updated dependencies
  - @rookcom/monarch-tokens@0.2.2

## 0.0.2

### Patch Changes

- Updated dependencies
  - @rookcom/monarch-tokens@0.2.1

## 0.0.1

### Patch Changes

- Updated dependencies [a2d52a3]
- Updated dependencies
- Updated dependencies [8177b95]
  - @rookcom/monarch-tokens@0.2.0
  - @rookcom/monarch-react@0.2.0
