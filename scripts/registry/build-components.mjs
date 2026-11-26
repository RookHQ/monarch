// Generates registry/components.json, the machine-readable component registry.
//
//   npm run registry:components         write it
//   npm run registry:components:check   fail if it is stale or invalid (CI)
//
// Inputs (never hand-edit the output; change these instead):
//   registry/component-demand.json        how many products need each component, and capability gaps
//   packages/react/dist/manifest.json      what Monarch exports today (npm run build)
//   registry/roadmap-waves.json            wave, package and entry point per component
//   registry/strategic.json                strategic families, in scope ahead of demand (ADR 0011)
//   registry/deprecations.json             deprecated exports
//   packages/react/src/**/<id>/             component folders and their <id>.docs.tsx
//
// Scope rule (docs/roadmap/COMPONENT-IMPLEMENTATION-ROADMAP.md): a component is
// in scope when 3+ products need it, or 2+ need it at P0. Components in a
// strategic family (registry/strategic.json, ADR 0011) are in scope with
// scope "strategic" whatever their product count.
import { existsSync, readFileSync, writeFileSync } from 'node:fs';
import { fileURLToPath, pathToFileURL } from 'node:url';
import { dirname, join, relative } from 'node:path';
import * as prettier from 'prettier';
import { assertSupported, validate } from '../../packages/conformance/src/schema.ts';
import {
  docsFileOf,
  listFolders,
  loadDocs,
  valueExports,
} from '../../packages/react/scripts/component-folders.ts';
import { DOC_SECTIONS, isIncomplete } from '../../packages/react/src/internal/component-docs.ts';

export const ROOT = join(dirname(fileURLToPath(import.meta.url)), '..', '..');
const read = (p) => JSON.parse(readFileSync(join(ROOT, p), 'utf8'));

export const OUTPUT = 'registry/components.json';
const SCHEMA = 'registry/components.schema.json';

/**
 * Every Monarch export belongs to exactly one registry entry. A new export of a
 * component folder that already has a group here joins it automatically; any
 * other new export must be added here, and the build fails otherwise, so the
 * registry can never silently miss a shipped component. `meta` is only for entries with no demand data.
 */
const EXPORT_GROUPS = [
  { id: 'alert', exports: ['Alert'] },
  { id: 'avatar', exports: ['Avatar', 'AvatarGroup', 'initials', 'avatarPaletteIndex'] },
  { id: 'badge', exports: ['Badge'] },
  { id: 'breadcrumb', exports: ['Breadcrumb'] },
  { id: 'button', exports: ['Button', 'IconButton'] },
  {
    id: 'card',
    exports: ['Card', 'CardContent', 'CardDescription', 'CardFooter', 'CardHeader', 'CardTitle'],
  },
  {
    id: 'checkbox',
    exports: ['Checkbox'],
    meta: { name: 'Checkbox', category: '04-forms', layer: 'primitive' },
  },
  { id: 'combobox', exports: ['Combobox', 'matchComboboxOption'] },
  {
    id: 'cx',
    exports: ['cx'],
    meta: { name: 'cx (class name utility)', category: '01-layout', layer: 'primitive' },
  },
  { id: 'description-list', exports: ['DescriptionList', 'DescriptionItem'] },
  {
    id: 'direction-provider',
    exports: ['DirectionProvider'],
    meta: { name: 'DirectionProvider (text direction)', category: '01-layout', layer: 'primitive' },
  },
  {
    id: 'dialog',
    exports: [
      'Dialog',
      'DialogBody',
      'DialogClose',
      'DialogContent',
      'DialogDescription',
      'DialogFooter',
      'DialogHeader',
      'DialogTitle',
      'DialogTrigger',
    ],
  },
  {
    id: 'dropdown-menu',
    exports: [
      'DropdownMenu',
      'DropdownMenuCheckboxItem',
      'DropdownMenuContent',
      'DropdownMenuGroup',
      'DropdownMenuItem',
      'DropdownMenuLabel',
      'DropdownMenuRadioGroup',
      'DropdownMenuRadioItem',
      'DropdownMenuSeparator',
      'DropdownMenuSub',
      'DropdownMenuSubContent',
      'DropdownMenuSubTrigger',
      'DropdownMenuTrigger',
    ],
  },
  { id: 'empty-state', exports: ['EmptyState'] },
  {
    id: 'error-state',
    exports: ['ErrorState', 'ErrorStateBoundary', 'errorKindFromStatus', 'errorKindIcon'],
  },
  {
    id: 'form-field',
    exports: [
      'Field',
      'FieldDescription',
      'FieldError',
      'FieldLabel',
      'NativeSelect',
      'useFieldContext',
      'useFieldControl',
    ],
  },
  {
    id: 'filter-bar',
    exports: [
      'FilterBar',
      'FilterMenu',
      'readFilterParams',
      'writeFilterParams',
      'defaultFilterBarMessages',
    ],
  },
  { id: 'filter-chips', exports: ['FilterChip'] },
  { id: 'text-field', exports: ['Input', 'Textarea'] },
  {
    id: 'kbd',
    exports: ['Kbd'],
    meta: { name: 'Kbd', category: '02-typography', layer: 'primitive' },
  },
  {
    id: 'link',
    exports: ['Link'],
    meta: { name: 'Link', category: '05-navigation', layer: 'primitive' },
  },
  { id: 'loading-state', exports: ['LoadingState'] },
  { id: 'page-header', exports: ['PageHeader'] },
  { id: 'pagination', exports: ['Pagination', 'pageWindow'] },
  {
    id: 'popover',
    exports: ['Popover', 'PopoverAnchor', 'PopoverClose', 'PopoverContent', 'PopoverTrigger'],
    meta: { name: 'Popover', category: '09-overlays', layer: 'component' },
  },
  { id: 'progress-bar', exports: ['Progress'] },
  {
    id: 'select',
    exports: [
      'Select',
      'SelectContent',
      'SelectGroup',
      'SelectItem',
      'SelectLabel',
      'SelectSeparator',
      'SelectTrigger',
      'SelectValue',
    ],
    meta: { name: 'Select', category: '04-forms', layer: 'component' },
  },
  {
    id: 'separator',
    exports: ['Separator'],
    meta: { name: 'Separator', category: '01-layout', layer: 'primitive' },
  },
  { id: 'settings-section', exports: ['SettingsSection', 'SettingsRow', 'SettingsActionRow'] },
  { id: 'skeleton', exports: ['Skeleton'] },
  {
    id: 'spinner',
    exports: ['Spinner'],
    meta: { name: 'Spinner', category: '08-feedback', layer: 'primitive' },
  },
  {
    id: 'split-layout',
    exports: ['SplitLayout'],
    meta: { name: 'Split Layout', category: '01-layout', layer: 'pattern' },
  },
  { id: 'stat-card', exports: ['StatCard', 'StatCardGroup'] },
  { id: 'switch', exports: ['Switch'] },
  {
    id: 'data-table',
    exports: [
      'Table',
      'TableBody',
      'TableCaption',
      'TableCell',
      'TableHead',
      'TableHeader',
      'TableRow',
      'TableSelectCell',
      'SortButton',
      'DataTable',
      'DataTableColumnChooser',
      'useDataTableColumns',
      'sortRows',
      'nextSort',
      'columnChooserColumns',
      'defaultDataTableMessages',
      'defaultColumnChooserMessages',
    ],
  },
  {
    id: 'tabs',
    exports: [
      'Tabs',
      'TabsContent',
      'TabsFilter',
      'TabsLink',
      'TabsList',
      'TabsNav',
      'TabsTrigger',
      'useTabsUrlBinding',
    ],
  },
  { id: 'toast', exports: ['ToastProvider', 'useToast', 'toast', 'getErrorMessage'] },
  {
    id: 'toolbar',
    exports: ['Toolbar', 'ToolbarGroup'],
    meta: { name: 'Toolbar', category: '03-actions', layer: 'pattern' },
  },
  { id: 'tooltip', exports: ['Tooltip', 'TooltipProvider'] },
  {
    id: 'visually-hidden',
    exports: ['VisuallyHidden'],
    meta: { name: 'Visually Hidden', category: '01-layout', layer: 'primitive' },
  },
  { id: 'copy-button', exports: ['CopyButton', 'useCopyToClipboard', 'writeToClipboard'] },
  { id: 'copy-field', exports: ['CopyField'] },
  { id: 'secret-field', exports: ['SecretField'] },
  {
    id: 'status-badge',
    exports: [
      'StatusBadge',
      'StatusRegistryProvider',
      'builtInStatuses',
      'checkStatusColor',
      'defineStatuses',
      'formatStatusLabel',
      'resolveStatus',
    ],
  },
  { id: 'segmented-control', exports: ['SegmentedControl'] },
  { id: 'relative-time', exports: ['RelativeTime', 'formatRelativeTime', 'relativeTimeParts'] },
  { id: 'money', exports: ['Money', 'formatMoney', 'currencyDigits'] },
  { id: 'load-more', exports: ['LoadMore', 'useLoadMore'] },
  { id: 'back-link', exports: ['BackLink'] },
  { id: 'search-input', exports: ['SearchInput'] },
  { id: 'tag-input', exports: ['TagInput'] },
  {
    id: 'username-availability-input',
    exports: ['UsernameAvailabilityInput', 'useAsyncValidation'],
  },
  { id: 'otp-pin-input', exports: ['OTPPinInput'] },
  { id: 'location-fields', exports: ['LocationFields'] },
  { id: 'error-page', exports: ['ErrorPage'] },
  { id: 'inline-error-retry', exports: ['InlineErrorRetry'] },
  { id: 'confirm-dialog', exports: ['ConfirmDialog', 'useConfirm'] },
  {
    id: 'sheet',
    exports: [
      'Sheet',
      'SheetTrigger',
      'SheetClose',
      'SheetContent',
      'SheetHeader',
      'SheetTitle',
      'SheetDescription',
      'SheetBody',
      'SheetFooter',
    ],
  },
  {
    id: 'responsive-dialog',
    exports: [
      'ResponsiveDialog',
      'ResponsiveDialogTrigger',
      'ResponsiveDialogClose',
      'ResponsiveDialogContent',
    ],
  },
  { id: 'inline-edit-text', exports: ['InlineEditText'] },
  { id: 'section-header', exports: ['SectionHeader', 'SectionHeaderLink'] },
  { id: 'switch-settings-list', exports: ['SwitchSettingsItem', 'SwitchSettingsList'] },
  { id: 'callout-banner', exports: ['CalloutBanner'] },
  { id: 'inline-notice', exports: ['InlineNotice'] },
  {
    id: 'accordion',
    exports: ['Accordion', 'AccordionItem', 'AccordionTrigger', 'AccordionContent'],
    meta: { name: 'Accordion', category: '06-data-display', layer: 'component' },
  },
  // @rookcom/monarch-react/marketing (docs/guides/marketing.md)
  { id: 'marketing-site-shell', exports: ['SiteHeader', 'SiteFooter'] },
  { id: 'marketing-hero', exports: ['MarketingHero'] },
  {
    id: 'marketing-section-blocks',
    exports: [
      'MarketingSection',
      'MarketingSectionHeader',
      'FeatureGrid',
      'ProductFrame',
      'StatStrip',
      'CTABand',
      'FAQ',
    ],
  },
  { id: 'entity-picker', exports: ['EntityPicker'] },
  { id: 'assignee-picker', exports: ['AssigneePicker'] },
  {
    id: 'timezone-select',
    exports: [
      'TimezoneSelect',
      'getTimezoneInfo',
      'listTimezones',
      'formatUtcOffset',
      'canonicalTimezone',
    ],
  },
  { id: 'duration-input', exports: ['DurationInput', 'parseDuration', 'formatDuration'] },
  { id: 'sparkline', exports: ['Sparkline'] },
  { id: 'chart-frame', exports: ['ChartFrame', 'chartColor'] },
  { id: 'line-chart', exports: ['LineChart'] },
  { id: 'bar-chart', exports: ['BarChart'] },
  { id: 'mini-month', exports: ['MiniMonth'] },
  { id: 'date-picker', exports: ['DatePicker'] },
  { id: 'date-time-picker', exports: ['DateTimePicker'] },
  { id: 'date-range-picker', exports: ['DateRangePicker'] },
  // Wave 4 group A (./data)
  { id: 'bulk-action-bar', exports: ['BulkActionBar', 'defaultBulkActionBarMessages'] },
  { id: 'saved-views', exports: ['SavedViews', 'defaultSavedViewsMessages'] },
  {
    id: 'command-palette',
    exports: [
      'CommandPalette',
      'defaultCommandFilter',
      'useHotkey',
      'formatHotkey',
      'matchesHotkey',
      'isTypingTarget',
    ],
  },
  { id: 'reorderable-list', exports: ['ReorderableList', 'moveItem'] },
  { id: 'stepper', exports: ['Stepper', 'stepStatus'] },
  { id: 'event-chip', exports: ['EventChip'] },
  { id: 'agenda-list', exports: ['AgendaList'] },
  { id: 'calendar-month-view', exports: ['CalendarMonthView'] },
  { id: 'time-grid', exports: ['TimeGrid'] },
  { id: 'weekly-hours-editor', exports: ['WeeklyHoursEditor', 'validateWeeklyHours'] },
  // Wave 6 group B: workflow and files.
  { id: 'kanban-board', exports: ['KanbanBoard'] },
  {
    id: 'file-upload',
    exports: ['FileUpload', 'validateFiles', 'fileMatchesAccept', 'formatBytes'],
  },
  { id: 'upload-queue', exports: ['UploadQueue', 'countUploads', 'totalUploadProgress'] },
  { id: 'onboarding-checklist', exports: ['OnboardingChecklist'] },
  // Collaboration: conversations and activity (wave 6 group A).
  { id: 'message-row', exports: ['MessageRow'] },
  {
    id: 'message-timeline',
    exports: [
      'MessageTimeline',
      'MessageTimelineDaySeparator',
      'MessageTimelineUnreadDivider',
      'shouldGroupMessages',
      'formatMessageDay',
    ],
  },
  {
    id: 'mention-picker',
    exports: [
      'MentionPicker',
      'useMentionPicker',
      'useMentionTextarea',
      'formatMentionToken',
      'parseMentionTokens',
      'findMentionQuery',
      'getCaretRect',
    ],
  },
  { id: 'notification-center', exports: ['NotificationCenter'] },
  { id: 'activity-timeline', exports: ['ActivityTimeline'] },
  { id: 'audit-log-viewer', exports: ['AuditLogViewer', 'auditLogToCsv'] },
  // @rookcom/monarch-react/editor (wave 9)
  { id: 'rich-text-editor', exports: ['RichTextEditor', 'RICH_TEXT_DEFAULT_TOOLBAR'] },
  { id: 'message-composer', exports: ['MessageComposer', 'MESSAGE_COMPOSER_TOOLBAR'] },
  { id: 'delta-indicator', exports: ['DeltaIndicator', 'formatDelta'] },
  { id: 'bar-list', exports: ['BarList'] },
  { id: 'donut-chart', exports: ['DonutChart'] },
  { id: 'stacked-proportion-bar', exports: ['StackedProportionBar', 'formatShare'] },
  // Wave 8 group B: flow patterns.
  {
    id: 'custom-domain-connect',
    exports: ['CustomDomainConnect', 'normalizeDomain', 'domainSyntaxError'],
  },
  { id: 'member-row', exports: ['MemberRow', 'MemberList'] },
  {
    id: 'schema-driven-form',
    exports: [
      'SchemaDrivenForm',
      'schemaDefaultValues',
      'isSchemaFieldVisible',
      'validateSchemaField',
      'validateSchemaForm',
      'formatSchemaValue',
    ],
  },
  { id: 'template-picker', exports: ['TemplatePicker', 'templateMatches'] },
  { id: 'section-card', exports: ['SectionCard'] },
  { id: 'record-header', exports: ['RecordHeader'] },
  {
    id: 'split-inbox-layout',
    exports: ['SplitInboxLayout', 'SplitInboxContextToggle', 'useSplitInboxLayout'],
  },
  {
    id: 'theme-toggle',
    exports: ['ThemeToggle', 'applyThemePreference', 'ROOK_THEME_COOKIE', 'ROOK_THEME_EVENT'],
  },
  {
    id: 'rook-rail',
    exports: ['RookRail', 'rookRailScript', 'ROOK_RAIL_COOKIE', 'ROOK_RAIL_EVENT'],
  },
  { id: 'space-switcher', exports: ['SpaceSwitcher'] },
  { id: 'workspace-switcher', exports: ['WorkspaceSwitcher'] },
  { id: 'support-panel', exports: ['SupportPanel', 'useSupportChat', 'SUPPORT_MESSAGE_MAX'] },
  {
    id: 'chat-dock',
    exports: [
      'ChatDock',
      'createChatDockBridge',
      'encodeChatEmbedMessage',
      'decodeChatEmbedMessage',
      'cleanOpenDetail',
      'chatPageContext',
      'CHAT_EMBED_PROTOCOL',
      'CHAT_EMBED_VERSION',
      'DEFAULT_CHAT_SANDBOX',
      'DEFAULT_CHAT_ALLOW',
    ],
  },
  {
    id: 'embedded-app-frame',
    exports: [
      'EmbeddedAppFrame',
      'useEmbedReady',
      'encodeAppEmbedMessage',
      'decodeAppEmbedChildMessage',
      'decodeAppEmbedHostMessage',
      'APP_EMBED_PROTOCOL',
      'APP_EMBED_VERSION',
      'MAX_EMBED_HEIGHT',
      'DEFAULT_EMBED_SANDBOX',
    ],
  },
  { id: 'no-app-access', exports: ['NoAppAccess'] },
  { id: 'linked-docs-panel', exports: ['LinkedDocsPanel', 'parseLinkUrl'] },
  { id: 'identity-bar', exports: ['IdentityBar'] },
  { id: 'app-shell', exports: ['AppShell'] },
  { id: 'prompt-dialog', exports: ['PromptDialog', 'usePrompt'] },
  { id: 'choice-dialog', exports: ['ChoiceDialog', 'useChoice'] },
  {
    id: 'unsaved-changes-guard',
    exports: ['UnsavedChangesDialog', 'UnsavedChangesGuard', 'useUnsavedChangesGuard'],
  },
  { id: 'undo-toast', exports: ['useUndoToast'] },
  { id: 'step-up-auth-dialog', exports: ['StepUpAuthDialog', 'useStepUpAuth'] },
  { id: 'type-to-confirm-input', exports: ['TypeToConfirmInput', 'phraseMatches'] },
  { id: 'consequence-list', exports: ['CONSEQUENCE_LIST_MESSAGES', 'ConsequenceList'] },
  { id: 'wizard-dialog', exports: ['WizardDialog'] },
  { id: 'danger-zone', exports: ['DangerZone', 'DangerZoneItem'] },
  {
    id: 'delete-workspace-flow',
    exports: ['DELETE_WORKSPACE_FLOW_MESSAGES', 'DeleteWorkspaceFlow'],
  },
  { id: 'delete-account-flow', exports: ['DELETE_ACCOUNT_FLOW_MESSAGES', 'DeleteAccountFlow'] },
  {
    id: 'transfer-ownership-flow',
    exports: ['TRANSFER_OWNERSHIP_FLOW_MESSAGES', 'TransferOwnershipFlow'],
  },
  { id: 'leave-workspace-flow', exports: ['LEAVE_WORKSPACE_FLOW_MESSAGES', 'LeaveWorkspaceFlow'] },
  { id: 'remove-member-flow', exports: ['REMOVE_MEMBER_FLOW_MESSAGES', 'RemoveMemberFlow'] },
  { id: 'bulk-destructive-flow', exports: ['BulkDestructiveFlow'] },
  { id: 'gdpr-erase-flow', exports: ['GDPR_ERASE_FLOW_MESSAGES', 'GdprEraseFlow'] },
  { id: 'deactivate-flow', exports: ['DeactivateFlow'] },
  { id: 'revoke-access-flow', exports: ['RevokeAccessFlow'] },
  {
    id: 'consent-manager',
    exports: [
      'ConsentAnalytics',
      'ConsentGate',
      'ConsentProvider',
      'DEFAULT_CONSENT_CATEGORIES',
      'DEFAULT_CONSENT_REGIMES',
      'cookieConsentStorage',
      'localStorageConsentStorage',
      'readGlobalPrivacyControl',
      'resolveConsentRegime',
      'useConsent',
      'useOptionalConsent',
    ],
  },
  { id: 'cookie-consent-banner', exports: ['CookieConsentBanner'] },
  { id: 'consent-preferences', exports: ['ConsentPreferences', 'ConsentSettingsLink'] },
  { id: 'privacy-choices-link', exports: ['PrivacyChoicesLink'] },
  { id: 'marketing-consent-checkbox', exports: ['MarketingConsentCheckbox'] },
  { id: 'terms-acceptance', exports: ['TermsAcceptance', 'termsVersions'] },
  { id: 'terms-update-dialog', exports: ['TermsUpdateDialog', 'formatEffectiveDate'] },
  { id: 'data-export-request', exports: ['DataExportRequest'] },
  { id: 'consent-history', exports: ['ConsentHistory'] },
  {
    id: 'data-subject-request-form',
    exports: ['DEFAULT_DATA_SUBJECT_REQUEST_TYPES', 'DataSubjectRequestForm'],
  },
  {
    id: 'ai-conversation',
    exports: [
      'applyStreamEvent',
      'chatCompletionsMapper',
      'contentBlockMessagesMapper',
      'createSseTransport',
      'isStreaming',
      'mapRookIntelligenceEvent',
      'messageText',
      'mockTransport',
      'readServerSentEvents',
      'readSseFrames',
      'rookIntelligenceTransport',
      'textDeltas',
      'toChatMessages',
      'useAiConversation',
    ],
  },
  {
    id: 'ai-scroll-anchor',
    exports: ['AI_SCROLL_ANCHOR_THRESHOLD', 'AiScrollAnchor', 'useAiScrollAnchor'],
  },
  { id: 'ai-thread', exports: ['AiThread'] },
  { id: 'ai-message', exports: ['AiMessageView'] },
  { id: 'ai-message-actions', exports: ['AiMessageActions'] },
  { id: 'ai-prompt-input', exports: ['AiPromptInput', 'findTrigger', 'usePromptDraft'] },
  {
    id: 'ai-streaming-text',
    exports: ['AI_STREAMING_TEXT_MESSAGES', 'AiStreamingText', 'completeStreamingMarkdown'],
  },
  {
    id: 'ai-markdown',
    exports: [
      'AI_MARKDOWN_MESSAGES',
      'AiMarkdown',
      'blockText',
      'inlineText',
      'parseInline',
      'parseMarkdownBlocks',
      'parseMarkdownIncremental',
    ],
  },
  {
    id: 'ai-code-block',
    exports: [
      'AI_CODE_BLOCK_MESSAGES',
      'AiCodeBlock',
      'codeLanguageName',
      'resolveCodeLanguage',
      'tokenizeCode',
    ],
  },
  { id: 'ai-thinking-indicator', exports: ['AiThinkingIndicator'] },
  { id: 'ai-typing-dots', exports: ['AiTypingDots'] },
  { id: 'ai-stop-button', exports: ['AiStopButton', 'AiStopGlyph'] },
  { id: 'ai-error-message', exports: ['AiErrorMessage', 'aiErrorKind'] },
  {
    id: 'ai-reasoning',
    exports: ['AI_REASONING_MESSAGES', 'AiReasoning', 'formatReasoningDuration'],
  },
  { id: 'ai-chain-of-steps', exports: ['AI_CHAIN_OF_STEPS_MESSAGES', 'AiChainOfSteps'] },
  { id: 'ai-plan', exports: ['AI_PLAN_MESSAGES', 'AiPlan'] },
  {
    id: 'ai-tool-call',
    exports: [
      'AI_TOOL_CALL_MESSAGES',
      'AiToolCall',
      'formatToolValue',
      'humanizeToolName',
      'toolCallDuration',
      'toolLabel',
    ],
  },
  { id: 'ai-tool-approval', exports: ['AI_TOOL_APPROVAL_MESSAGES', 'AiToolApproval'] },
  { id: 'ai-tool-result-card', exports: ['AI_TOOL_RESULT_CARD_MESSAGES', 'AiToolResultCard'] },
  { id: 'ai-edit-proposal', exports: ['AiEditProposal', 'diffWords'] },
  { id: 'ai-inline-diff', exports: ['AiInlineDiff', 'aiInlineDiffChanges', 'resolveAiInlineDiff'] },
  { id: 'ai-undo-banner', exports: ['AI_UNDO_BANNER_MESSAGES', 'AiUndoBanner', 'useAiUndoToast'] },
  {
    id: 'ai-citation',
    exports: [
      'AI_CITATION_MESSAGES',
      'AI_SOURCE_KIND_LABELS',
      'AI_SOURCE_LINK_REL',
      'AiCitation',
      'AiSourceGlyph',
      'aiSafeSourceUrl',
      'aiSourceHost',
      'aiSourceIndex',
      'aiSourceMeta',
    ],
  },
  { id: 'ai-source-list', exports: ['AI_SOURCE_LIST_MESSAGES', 'AiSourceList'] },
  {
    id: 'ai-source-preview',
    exports: ['AI_SOURCE_PREVIEW_MESSAGES', 'AiSourcePreview', 'findQuoteRange'],
  },
  {
    id: 'ai-confidence-meter',
    exports: ['AiConfidenceMeter', 'DEFAULT_CONFIDENCE_THRESHOLDS', 'aiConfidenceLevel'],
  },
  { id: 'ai-generated-badge', exports: ['AiBotGlyph', 'AiGeneratedBadge'] },
  { id: 'ai-disclaimer', exports: ['AI_DISCLAIMER_MESSAGES', 'AiDisclaimer'] },
  {
    id: 'ai-feedback',
    exports: ['AI_FEEDBACK_DOWN_REASONS', 'AI_FEEDBACK_MESSAGES', 'AiFeedback'],
  },
  { id: 'ai-summary-card', exports: ['AI_SUMMARY_CARD_MESSAGES', 'AiSummaryCard'] },
  { id: 'ai-insight-callout', exports: ['AI_INSIGHT_CALLOUT_MESSAGES', 'AiInsightCallout'] },
  {
    id: 'ai-suggested-actions',
    exports: ['AI_SUGGESTED_ACTIONS_MESSAGES', 'AiSuggestedActions', 'aiActionNeedsApproval'],
  },
  { id: 'ai-autofill-field', exports: ['AiAutofillField'] },
  { id: 'ai-draft-queue', exports: ['AiDraftQueue'] },
  {
    id: 'ai-score',
    exports: [
      'AI_SCORE_MESSAGES',
      'AiScore',
      'DEFAULT_AI_SCORE_BANDS',
      'aiScoreBand',
      'aiScoreBandRanges',
    ],
  },
  { id: 'ai-assistant-panel', exports: ['AiAssistantPanel'] },
  {
    id: 'ai-launcher',
    exports: ['AiLauncher', 'ariaKeyShortcut', 'matchesAiShortcut', 'parseAiShortcut'],
  },
  { id: 'ai-message-branch', exports: ['AI_MESSAGE_BRANCH_MESSAGES', 'AiMessageBranch'] },
  { id: 'ai-user-message-edit', exports: ['AI_USER_MESSAGE_EDIT_MESSAGES', 'AiUserMessageEdit'] },
  { id: 'ai-welcome', exports: ['AI_WELCOME_MESSAGES', 'AiWelcome'] },
  { id: 'ai-starter-prompts', exports: ['AI_STARTER_PROMPTS_MESSAGES', 'AiStarterPrompts'] },
  {
    id: 'ai-follow-up-suggestions',
    exports: ['AI_FOLLOW_UP_SUGGESTIONS_MESSAGES', 'AiFollowUpSuggestions'],
  },
  {
    id: 'ai-conversation-list',
    exports: ['AI_CONVERSATION_LIST_MESSAGES', 'AiConversationList', 'groupAiConversations'],
  },
  {
    id: 'ai-conversation-header',
    exports: ['AI_CONVERSATION_HEADER_MESSAGES', 'AiConversationHeader'],
  },
  { id: 'ai-context-divider', exports: ['AI_CONTEXT_DIVIDER_MESSAGES', 'AiContextDivider'] },
  {
    id: 'ai-prompt-toolbar',
    exports: ['AiPromptToolbar', 'AiPromptToolbarButton', 'AiPromptToolbarToggle'],
  },
  {
    id: 'ai-slash-commands',
    exports: [
      'AI_SLASH_COMMANDS_MESSAGES',
      'AiSlashCommands',
      'combineAiPromptMenus',
      'filterAiSlashCommands',
      'useAiSlashCommands',
    ],
  },
  {
    id: 'ai-mention-context',
    exports: [
      'AI_MENTION_CONTEXT_MESSAGES',
      'AiMentionContext',
      'aiMentionToContextChip',
      'useAiMentionContext',
    ],
  },
  {
    id: 'ai-context-chips',
    exports: [
      'AI_CONTEXT_CHIPS_MESSAGES',
      'AiContextChips',
      'AiContextKindGlyph',
      'aiContextChipWeight',
      'formatAiContextTokens',
    ],
  },
  {
    id: 'ai-attachment-tray',
    exports: [
      'AI_ATTACHMENT_TRAY_MESSAGES',
      'AiAttachmentTray',
      'aiAttachmentStatus',
      'aiAttachmentTrayFiles',
    ],
  },
  { id: 'ai-mode-switcher', exports: ['AI_MODE_SWITCHER_MESSAGES', 'AiModeSwitcher'] },
  { id: 'ai-model-picker', exports: ['AI_MODEL_PICKER_MESSAGES', 'AiModelPicker'] },
  {
    id: 'ai-tone-picker',
    exports: ['AI_DEFAULT_TONES', 'AI_TONE_CUSTOM', 'AI_TONE_PICKER_MESSAGES', 'AiTonePicker'],
  },
  {
    id: 'ai-prompt-template-picker',
    exports: [
      'AI_PROMPT_TEMPLATE_PICKER_MESSAGES',
      'AiPromptTemplatePicker',
      'extractPromptVariables',
      'fillPromptTemplate',
      'promptTemplateVariables',
    ],
  },
  {
    id: 'ai-voice-input',
    exports: [
      'AI_VOICE_INPUT_MESSAGES',
      'AiVoiceInput',
      'canRecordAudio',
      'createRecordingRecognition',
      'getSpeechRecognition',
    ],
  },
  { id: 'ai-inline-prompt', exports: ['AI_INLINE_PROMPT_MESSAGES', 'AiInlinePrompt'] },
  { id: 'ai-response-skeleton', exports: ['AI_RESPONSE_SKELETON_MESSAGES', 'AiResponseSkeleton'] },
  { id: 'ai-structured-answer', exports: ['AI_STRUCTURED_ANSWER_MESSAGES', 'AiStructuredAnswer'] },
  {
    id: 'ai-table-answer',
    exports: [
      'AI_TABLE_ANSWER_MESSAGES',
      'AiTableAnswer',
      'inferColumnType',
      'inferColumns',
      'toCsv',
    ],
  },
  {
    id: 'ai-agent-avatar',
    exports: [
      'AI_AGENT_AVATAR_MESSAGES',
      'AiAgentAvatar',
      'aiAgentInitials',
      'resolveAiAgentAvatarMessages',
    ],
  },
  { id: 'ai-agent-card', exports: ['AI_AGENT_CARD_MESSAGES', 'AiAgentCard'] },
  { id: 'ai-agent-picker', exports: ['AI_AGENT_PICKER_MESSAGES', 'AiAgentPicker', 'aiAgentKey'] },
  { id: 'ai-handoff', exports: ['AI_HANDOFF_MESSAGES', 'AiHandoff', 'isAiHandoffHuman'] },
  {
    id: 'ai-multi-agent-roster',
    exports: ['AI_MULTI_AGENT_ROSTER_MESSAGES', 'AiMultiAgentRoster'],
  },
  { id: 'ai-task-progress', exports: ['AI_TASK_PROGRESS_MESSAGES', 'AiTaskProgress'] },
  {
    id: 'ai-agent-run-timeline',
    exports: ['AI_AGENT_RUN_TIMELINE_MESSAGES', 'AiAgentRunTimeline', 'aiRunTotals'],
  },
  {
    id: 'ai-permission-scope',
    exports: ['AI_PERMISSION_SCOPE_MESSAGES', 'AiPermissionScope', 'aiAccessAtLeast'],
  },
  { id: 'ai-action-log', exports: ['AI_ACTION_LOG_MESSAGES', 'AiActionLog'] },
  { id: 'ai-usage-meter', exports: ['AiUsageMeter', 'aiUsageState'] },
  { id: 'ai-safety-notice', exports: ['AI_SAFETY_NOTICE_MESSAGES', 'AiSafetyNotice'] },
  { id: 'ai-memory-chip', exports: ['AI_MEMORY_CHIP_MESSAGES', 'AiMemoryChip', 'shortMemoryText'] },
  {
    id: 'ai-memory-panel',
    exports: ['AI_MEMORY_PANEL_MESSAGES', 'AiMemoryPanel', 'groupAiMemories'],
  },
  { id: 'ai-context-window-meter', exports: ['AiContextWindowMeter', 'aiContextWindowState'] },
  {
    id: 'ai-knowledge-scope',
    exports: ['AiKnowledgeScope', 'aiKnowledgeScopeState', 'toggleAiKnowledgeSource'],
  },
  { id: 'ai-related-entities', exports: ['AiRelatedEntities', 'sortAiRelatedEntities'] },
  {
    id: 'ai-artifact-card',
    exports: ['AI_ARTIFACT_CARD_MESSAGES', 'AiArtifactCard', 'AiArtifactKindGlyph'],
  },
  {
    id: 'ai-artifact-canvas',
    exports: ['AI_ARTIFACT_CANVAS_MESSAGES', 'AiArtifactCanvas', 'aiArtifactSrcDoc'],
  },
  {
    id: 'ai-artifact-versions',
    exports: [
      'AI_ARTIFACT_DIFF_MESSAGES',
      'AI_ARTIFACT_VERSIONS_MESSAGES',
      'AiArtifactDiff',
      'AiArtifactVersions',
      'diffLines',
    ],
  },
  {
    id: 'ai-ghost-text',
    exports: [
      'AI_GHOST_TEXT_MESSAGES',
      'AiGhostText',
      'aiGhostTextNextWord',
      'applyAiRewrite',
      'useAiCompletion',
      'useAiRewrite',
    ],
  },
  {
    id: 'ai-generative-ui',
    exports: [
      'AI_GENERATIVE_UI_MESSAGES',
      'AiGenerativeUi',
      'createAiUiRegistry',
      'sanitizeAiUiProps',
      'validateAiUiProps',
    ],
  },
  { id: 'ai-image-generation', exports: ['AiImageGeneration'] },
  { id: 'ai-chat-page', exports: ['AiChatPage'] },
  { id: 'ai-copilot-layout', exports: ['AiCopilotLayout', 'useAiSelectionContext'] },
  { id: 'ai-agent-builder', exports: ['AiAgentBuilder', 'EMPTY_AI_AGENT_DEFINITION'] },
  { id: 'ai-eval-review', exports: ['AiEvalReview', 'EMPTY_AI_EVAL_REVIEW'] },
  { id: 'ai-onboarding-tour', exports: ['AiOnboardingTour'] },
  { id: 'ai-settings-section', exports: ['AiSettingsSection', 'DEFAULT_AI_RETENTION_OPTIONS'] },
  { id: 'rating-picker', exports: ['RATING_PICKER_MESSAGES', 'RatingPicker'] },
  { id: 'presence-dot', exports: ['PresenceDot', 'presenceLabel'] },
  { id: 'my-status', exports: ['MyStatus'] },
  {
    id: 'create-workspace-dialog',
    exports: ['CreateWorkspaceDialog', 'defaultCreateWorkspaceDialogMessages'],
  },
  {
    id: 'workspace-onboarding-form',
    exports: [
      'WorkspaceOnboardingForm',
      'defaultWorkspaceOnboardingFormMessages',
      'defaultWorkspaceTypes',
    ],
  },
  {
    id: 'app-access-picker',
    exports: ['AppAccessPicker', 'AppAccessSummary', 'defaultAppAccessPickerMessages'],
  },
  {
    id: 'session-list',
    exports: ['SessionList', 'defaultSessionListMessages', 'describeUserAgent'],
  },
  { id: 'device-list', exports: ['DeviceList', 'defaultDeviceListMessages'] },
  { id: 'security-alert-card', exports: ['SecurityAlertCard', 'defaultSecurityAlertCardMessages'] },
  { id: 'two-factor-setup', exports: ['TwoFactorSetup', 'defaultTwoFactorSetupMessages'] },
  { id: 'connected-apps-list', exports: ['ConnectedAppsList', 'defaultConnectedAppsListMessages'] },
  { id: 'sign-in-with-rook-button', exports: ['SignInWithRookButton'] },
  { id: 'info-tooltip', exports: ['InfoTooltip'] },
  { id: 'coming-soon-state', exports: ['COMING_SOON_STATE_MESSAGES', 'ComingSoonState'] },
  { id: 'celebration', exports: ['Celebration', 'CelebrationPanel'] },
  { id: 'tree-view', exports: ['TreeView', 'defaultTreeViewMessages'] },
  {
    id: 'score-ring',
    exports: ['ScoreRing', 'defaultScoreRingMessages', 'defaultScoreRingThresholds'],
  },
  { id: 'provenance', exports: ['Provenance', 'defaultProvenanceMessages'] },
  {
    id: 'freshness-indicator',
    exports: ['FreshnessIndicator', 'defaultFreshnessIndicatorMessages', 'freshnessStatus'],
  },
  { id: 'favicon', exports: ['Favicon', 'faviconHostname', 'faviconLetter', 'isSafeFaviconSrc'] },
  { id: 'column-chooser', exports: ['ColumnChooser', 'defaultColumnChooserPanelMessages'] },
  { id: 'column-menu', exports: ['ColumnMenu', 'defaultColumnMenuMessages', 'orderColumns'] },
  { id: 'cursor-list', exports: ['CursorList', 'defaultCursorListMessages'] },
  {
    id: 'import-wizard',
    exports: [
      'ImportWizard',
      'defaultImportWizardMessages',
      'normalizeHeader',
      'parseCsv',
      'suggestMapping',
      'toImportRecords',
    ],
  },
  {
    id: 'preview-rows',
    exports: ['PreviewRows', 'countPreviewRows', 'defaultPreviewRowsMessages', 'previewRowStatus'],
  },
  { id: 'duplicate-panel', exports: ['DuplicatePanel', 'defaultDuplicatePanelMessages'] },
  {
    id: 'merge-dialog',
    exports: ['MergeDialog', 'defaultMergeChoice', 'defaultMergeDialogMessages'],
  },
  { id: 'heatmap', exports: ['Heatmap', 'HeatmapLegend', 'heatmapScale'] },
  { id: 'calendar-heatmap', exports: ['CalendarHeatmap'] },
  {
    id: 'uptime-bar',
    exports: ['DEFAULT_UPTIME_STATUS_LABELS', 'UptimeBar', 'UptimeLegend', 'uptimeStatus'],
  },
  { id: 'force-graph', exports: ['ForceGraph', 'forceLayout'] },
  {
    id: 'world-map',
    exports: ['WORLD_MAP_CENTROIDS', 'WORLD_MAP_SHAPES', 'WORLD_MAP_VIEWBOX', 'WorldMap'],
  },
  { id: 'favorites-list', exports: ['FAVORITES_LIST_MESSAGES', 'FavoriteToggle', 'FavoritesList'] },
  {
    id: 'keyboard-shortcuts-sheet',
    exports: [
      'KEYBOARD_SHORTCUTS_SHEET_MESSAGES',
      'KeyboardShortcutsSheet',
      'describeShortcutKeys',
    ],
  },
  { id: 'mega-menu', exports: ['MEGA_MENU_MESSAGES', 'MegaMenu'] },
  { id: 'device-frame', exports: ['DeviceFrame'] },
  { id: 'code-block', exports: ['CodeBlock', 'CodeTabs'] },
  {
    id: 'quick-switcher',
    exports: ['QUICK_SWITCHER_MESSAGES', 'QuickSwitcher', 'quickSwitcherFilter'],
  },
  { id: 'comments-panel', exports: ['CommentsPanel'] },
  {
    id: 'emoji-picker',
    exports: [
      'EmojiPicker',
      'EmojiPickerPopover',
      'applySkinTone',
      'defaultEmojiCategories',
      'pushRecentEmoji',
      'searchEmoji',
    ],
  },
  { id: 'typing-indicator', exports: ['TypingIndicator', 'formatTyping'] },
  { id: 'read-receipt', exports: ['ReadReceipt'] },
  { id: 'thread-panel', exports: ['ThreadPanel'] },
  { id: 'virtual-message-list', exports: ['VIRTUAL_MESSAGE_LIST_MESSAGES', 'VirtualMessageList'] },
  {
    id: 'link-card',
    exports: [
      'LINK_CARD_MAX_RATIO',
      'LINK_CARD_MESSAGES',
      'LINK_CARD_MIN_RATIO',
      'LinkCard',
      'LinkCardList',
      'LinkCardSkeleton',
      'linkCardImageRatio',
    ],
  },
  {
    id: 'slash-menu',
    exports: [
      'SLASH_MENU_MESSAGES',
      'SlashMenu',
      'filterSlashMenuItems',
      'findSlashQuery',
      'groupSlashMenuItems',
      'slashMenuOptionId',
      'useSlashMenu',
      'useSlashMenuKeys',
    ],
  },
  { id: 'share-dialog', exports: ['SHARE_DIALOG_MESSAGES', 'SHARE_EXPIRY_OPTIONS', 'ShareDialog'] },
  { id: 'post-card', exports: ['POST_CARD_MESSAGES', 'PostCard', 'truncateExcerpt'] },
  { id: 'inbox-row', exports: ['INBOX_ROW_MESSAGES', 'InboxList', 'InboxRow', 'formatCompactAge'] },
  { id: 'decision-card', exports: ['DECISION_CARD_MESSAGES', 'DecisionCard'] },
  {
    id: 'call-stage',
    exports: [
      'CallStage',
      'CallTimer',
      'DEFAULT_CALL_STAGE_MESSAGES',
      'callGridColumns',
      'chooseCallLayout',
      'formatCallDuration',
    ],
  },
  {
    id: 'call-tile',
    exports: ['CallQualityBars', 'CallTile', 'CallVideo', 'DEFAULT_CALL_TILE_MESSAGES'],
  },
  {
    id: 'call-controls',
    exports: [
      'CallControlButton',
      'CallControls',
      'CallIcon',
      'DEFAULT_CALL_CONTROLS_MESSAGES',
      'DEFAULT_CALL_REACTIONS',
      'useCallShortcuts',
    ],
  },
  {
    id: 'pre-join-lobby',
    exports: ['DEFAULT_PRE_JOIN_LOBBY_MESSAGES', 'PreJoinLobby', 'useAudioLevel'],
  },
  { id: 'incoming-call', exports: ['DEFAULT_INCOMING_CALL_MESSAGES', 'IncomingCall'] },
  {
    id: 'device-picker',
    exports: ['DEFAULT_DEVICE_PICKER_MESSAGES', 'DevicePicker', 'useMediaDevices'],
  },
  { id: 'mini-call', exports: ['DEFAULT_MINI_CALL_MESSAGES', 'MiniCall'] },
  {
    id: 'call-stats-panel',
    exports: [
      'CallStatsPanel',
      'DEFAULT_CALL_STATS_PANEL_MESSAGES',
      'formatCallBytes',
      'formatCallRate',
    ],
  },
  {
    id: 'version-history-panel',
    exports: [
      'DEFAULT_VERSION_HISTORY_PANEL_MESSAGES',
      'VersionHistoryPanel',
      'collapseVersionDiff',
      'diffVersionLines',
      'versionDiffStats',
    ],
  },
  {
    id: 'document-relations',
    exports: [
      'DEFAULT_DOCUMENT_RELATIONS_MESSAGES',
      'DEFAULT_DOCUMENT_RELATION_TYPES',
      'DocumentRelations',
    ],
  },
  { id: 'page-tree', exports: ['DEFAULT_PAGE_TREE_MESSAGES', 'PageTree'] },
  {
    id: 'media-upload',
    exports: ['DEFAULT_MEDIA_UPLOAD_MESSAGES', 'MediaUpload', 'mediaKindOf', 'readVideoDuration'],
  },
  {
    id: 'video-thumbnail',
    exports: [
      'DEFAULT_VIDEO_THUMBNAIL_MESSAGES',
      'VideoThumbnail',
      'captureVideoFrame',
      'formatVideoDuration',
    ],
  },
  { id: 'slot-picker', exports: ['SlotPicker'] },
  { id: 'booking-flow', exports: ['BookingFlow', 'useBookingFlow'] },
  { id: 'reschedule-dialog', exports: ['RescheduleDialog'] },
  { id: 'time-off-panel', exports: ['TimeOffPanel'] },
  { id: 'calendar-toolbar', exports: ['CalendarToolbar'] },
  { id: 'event-popover', exports: ['EventPopover'] },
  { id: 'shift-roster', exports: ['ShiftRoster'] },
  { id: 'business-hours-editor', exports: ['BusinessHoursEditor', 'validateBusinessHours'] },
  { id: 'team-calendar', exports: ['TeamCalendar'] },
  {
    id: 'gantt-timeline',
    exports: ['DEFAULT_GANTT_TIMELINE_MESSAGES', 'GanttTimeline', 'shiftGanttDates'],
  },
  { id: 'roadmap', exports: ['DEFAULT_ROADMAP_MESSAGES', 'Roadmap'] },
  {
    id: 'backlog-planner',
    exports: ['BacklogPlanner', 'DEFAULT_BACKLOG_PLANNER_MESSAGES', 'moveBacklogItem'],
  },
  {
    id: 'stage-journey',
    exports: [
      'DEFAULT_STAGE_HISTORY_MESSAGES',
      'DEFAULT_STAGE_JOURNEY_MESSAGES',
      'StageHistory',
      'StageJourney',
    ],
  },
  { id: 'approval-timeline', exports: ['ApprovalTimeline'] },
  {
    id: 'approval-chain-editor',
    exports: ['ApprovalChainEditor', 'summarizeApprovalChain', 'validateApprovalChain'],
  },
  { id: 'approval-queue', exports: ['ApprovalQueue'] },
  { id: 'rule-builder', exports: ['RuleBuilder', 'createRuleDraft', 'validateRule'] },
  {
    id: 'condition-editor',
    exports: [
      'ConditionEditor',
      'conditionOperatorTakesValue',
      'conditionOperatorsFor',
      'createCondition',
      'evaluateCondition',
      'evaluateConditions',
      'validateCondition',
      'validateConditions',
    ],
  },
  {
    id: 'action-editor',
    exports: ['ActionEditor', 'createAction', 'validateAction', 'validateActions'],
  },
  {
    id: 'form-builder',
    exports: [
      'FormBuilder',
      'formFieldHasOptions',
      'formFieldToConditionField',
      'validateFormDefinition',
    ],
  },
  {
    id: 'field-renderer',
    exports: ['FieldRenderer', 'isFormFieldValueEmpty', 'isFormFieldVisible', 'validateFormField'],
  },
  {
    id: 'sequence-builder',
    exports: [
      'SequenceBuilder',
      'findTemplateVariables',
      'renderSequenceTemplate',
      'validateSequence',
    ],
  },
  {
    id: 'sla-indicator',
    exports: [
      'DEFAULT_SLA_MESSAGES',
      'SlaIndicator',
      'SlaTargetList',
      'formatSlaDuration',
      'resolveSlaState',
    ],
  },
  {
    id: 'escalation-policy-editor',
    exports: ['EscalationPolicyEditor', 'validateEscalationPolicy'],
  },
  { id: 'org-chart', exports: ['DEFAULT_ORG_CHART_MESSAGES', 'OrgChart'] },
  { id: 'goal-tree', exports: ['DEFAULT_GOAL_TREE_MESSAGES', 'GoalTree'] },
  {
    id: 'running-timer',
    exports: ['DEFAULT_RUNNING_TIMER_MESSAGES', 'RunningTimer', 'formatElapsed'],
  },
  { id: 'timesheet-grid', exports: ['DEFAULT_TIMESHEET_GRID_MESSAGES', 'TimesheetGrid'] },
  {
    id: 'timesheet-diff',
    exports: [
      'DEFAULT_TIMESHEET_DIFF_MESSAGES',
      'TimesheetDiff',
      'diffTimesheetEntries',
      'timesheetDiffTotals',
    ],
  },
  {
    id: 'line-items-editor',
    exports: ['LineItemsEditor', 'computeLineItemTotals', 'createLineItem', 'parseDecimal'],
  },
  { id: 'payslip-view', exports: ['PayslipView'] },
  {
    id: 'subscriptions-table',
    exports: ['SubscriptionsTable', 'formatBillingInterval', 'subscriptionStatuses'],
  },
  { id: 'renewals-panel', exports: ['RenewalsPanel', 'renewalStatuses'] },
  { id: 'savings-panel', exports: ['SavingsPanel'] },
  { id: 'receipt-preview', exports: ['ReceiptPreview'] },
  { id: 'usage-meter', exports: ['UsageMeter', 'usageLevel'] },
  { id: 'method-badge', exports: ['MethodBadge', 'methodTone'] },
  { id: 'lifecycle-badge', exports: ['LifecycleBadge', 'formatLifecycleLabel'] },
  { id: 'key-created-dialog', exports: ['KeyCreatedDialog'] },
  { id: 'scope-selector', exports: ['ScopeSelector'] },
  {
    id: 'webhook-deliveries',
    exports: ['WebhookDeliveries', 'filterWebhookDeliveries', 'webhookDeliveryStatuses'],
  },
  { id: 'log-drawer', exports: ['LogDrawer', 'httpStatusTone'] },
  {
    id: 'dns-record-table',
    exports: ['DnsProviderGuide', 'DnsRecordTable', 'defaultDnsProviders', 'relativeDnsName'],
  },
  {
    id: 'domain-progress',
    exports: ['DomainProgress', 'domainPollMs', 'domainStepStates', 'formatDnsSnapshot'],
  },
  { id: 'install-verify', exports: ['InstallVerify'] },
  {
    id: 'status-page-view',
    exports: ['StatusPageView', 'deriveOverallState', 'formatIncidentDuration'],
  },
  {
    id: 'incident-timeline',
    exports: ['IncidentTimeline', 'formatIncidentTime', 'resolveIncidentStage'],
  },
  { id: 'report-viewer', exports: ['REPORT_VIEWER_MESSAGES', 'ReportViewer'] },
  {
    id: 'download-report-button',
    exports: ['DOWNLOAD_REPORT_BUTTON_MESSAGES', 'DownloadReportButton'],
  },
  {
    id: 'dashboard-grid',
    exports: ['DASHBOARD_GRID_MESSAGES', 'DashboardGrid', 'resolveDashboardLayout'],
  },
  { id: 'widget', exports: ['WIDGET_MESSAGES', 'Widget'] },
  { id: 'kpi-row', exports: ['KPIRow'] },
  { id: 'insight-strip', exports: ['INSIGHT_STRIP_MESSAGES', 'InsightStrip', 'sortInsights'] },
  {
    id: 'period-selector',
    exports: ['PERIOD_SELECTOR_MESSAGES', 'PeriodSelector', 'formatPeriodRange', 'resolvePeriod'],
  },
  { id: 'today-panel', exports: ['TODAY_PANEL_MESSAGES', 'TodayPanel'] },
  { id: 'activity-widget', exports: ['ACTIVITY_WIDGET_MESSAGES', 'ActivityWidget'] },
  { id: 'leaderboard', exports: ['LEADERBOARD_MESSAGES', 'Leaderboard', 'rankEntries'] },
  {
    id: 'help-center-home',
    exports: ['HelpCenterHome', 'filterHelpArticles', 'groupHelpArticles'],
  },
  { id: 'csat-survey', exports: ['CSATSurvey'] },
  { id: 'careers-job-list', exports: ['CareersJobList'] },
  {
    id: 'rook-bar',
    exports: [
      'RookBar',
      'RookBarAppCatalog',
      'RookBarFeaturePanel',
      'RookBarLinkList',
      'RookBarPanelProvider',
      'RookBarStoriesPanel',
      'useRookBarPanel',
    ],
  },
  { id: 'docs-panel', exports: ['DocsPanel'] },
  { id: 'notifications-bell', exports: ['NotificationsBell'] },
  { id: 'availability-toggle', exports: ['AvailabilityToggle'] },
  { id: 'button-group', exports: ['ButtonGroup'] },
  { id: 'split-button', exports: ['SplitButton'] },
  { id: 'toggle-button', exports: ['ToggleButton'] },
  { id: 'toggle-group', exports: ['ToggleGroup', 'ToggleGroupItem'] },
  { id: 'menu-button', exports: ['MenuButton', 'MenuButtonItems'] },
  { id: 'share-button', exports: ['ShareButton', 'emailShareTarget'] },
  {
    id: 'number-input',
    exports: [
      'NUMBER_INPUT_MESSAGES',
      'NumberInput',
      'decimalPlaces',
      'parseNumber',
      'roundTo',
      'stepValue',
    ],
  },
  { id: 'currency-input', exports: ['CurrencyInput', 'fromMinorUnits', 'toMinorUnits'] },
  {
    id: 'phone-input',
    exports: ['PHONE_INPUT_MESSAGES', 'PhoneInput', 'isValidE164', 'parsePhoneNumber', 'toE164'],
  },
  { id: 'password-input', exports: ['PASSWORD_INPUT_MESSAGES', 'PasswordInput'] },
  {
    id: 'password-strength-meter',
    exports: [
      'DEFAULT_PASSWORD_RULES',
      'PASSWORD_STRENGTH_METER_MESSAGES',
      'PasswordStrengthMeter',
      'scorePassword',
    ],
  },
  { id: 'multi-select', exports: ['MultiSelect'] },
  { id: 'radio-group', exports: ['RadioGroup', 'RadioGroupItem', 'useRadioGroupContext'] },
  { id: 'radio-card-group', exports: ['RadioCardGroup'] },
  { id: 'checkbox-group', exports: ['CheckboxGroup'] },
  {
    id: 'slider',
    exports: [
      'Slider',
      'defaultLargeStep',
      'isRtl',
      'percentOf',
      'sliderKeyValue',
      'snapToStep',
      'valueFromPointer',
    ],
  },
  { id: 'range-slider', exports: ['RANGE_SLIDER_MESSAGES', 'RangeSlider'] },
  {
    id: 'color-picker',
    exports: [
      'COLOR_PICKER_MESSAGES',
      'ColorPicker',
      'DEFAULT_COLOR_SWATCHES',
      'isHexColor',
      'normalizeHex',
    ],
  },
  { id: 'input-group', exports: ['InputGroup', 'InputGroupAddon', 'InputGroupButton'] },
  { id: 'form-actions-bar', exports: ['FormActionsBar'] },
  { id: 'autosave-indicator', exports: ['AutosaveIndicator', 'useAutosave'] },
  {
    id: 'context-menu',
    exports: [
      'ContextMenu',
      'ContextMenuCheckboxItem',
      'ContextMenuContent',
      'ContextMenuGroup',
      'ContextMenuItem',
      'ContextMenuLabel',
      'ContextMenuRadioGroup',
      'ContextMenuRadioItem',
      'ContextMenuSeparator',
      'ContextMenuSub',
      'ContextMenuSubContent',
      'ContextMenuSubTrigger',
      'ContextMenuTrigger',
    ],
  },
  { id: 'hover-card', exports: ['HoverCard', 'HoverCardContent', 'HoverCardTrigger'] },
  {
    id: 'drawer',
    exports: [
      'Drawer',
      'DrawerBody',
      'DrawerClose',
      'DrawerContent',
      'DrawerDescription',
      'DrawerFooter',
      'DrawerHeader',
      'DrawerTitle',
      'DrawerTrigger',
      'shouldCloseDrawer',
    ],
  },
  { id: 'peek-panel', exports: ['PeekPanel'] },
  { id: 'popconfirm', exports: ['Popconfirm'] },
  { id: 'lightbox', exports: ['Lightbox'] },
  { id: 'progress-circle', exports: ['ProgressCircle'] },
  { id: 'save-status-indicator', exports: ['SaveStatusIndicator', 'defaultSaveStatusMessages'] },
  { id: 'tag', exports: ['Tag', 'TagGroup'] },
  { id: 'metric', exports: ['Metric', 'formatMetricValue'] },
  { id: 'key-value', exports: ['KeyValue', 'KeyValueItem'] },
  { id: 'collapsible', exports: ['Collapsible', 'CollapsibleContent', 'CollapsibleTrigger'] },
  { id: 'truncated-text', exports: ['TruncatedText'] },
  { id: 'status-dot', exports: ['StatusDot'] },
  { id: 'sidebar-nav', exports: ['SIDEBAR_NAV_MESSAGES', 'SidebarNav'] },
  { id: 'top-nav', exports: ['TOP_NAV_MESSAGES', 'TopNav'] },
  { id: 'bottom-tab-bar', exports: ['BOTTOM_TAB_BAR_MESSAGES', 'BottomTabBar'] },
  { id: 'skip-link', exports: ['SKIP_LINK_MESSAGES', 'SkipLink'] },
  {
    id: 'table-of-contents',
    exports: ['TABLE_OF_CONTENTS_MESSAGES', 'TableOfContents', 'collectHeadings', 'useScrollSpy'],
  },
  { id: 'overflow-tabs', exports: ['OVERFLOW_TABS_MESSAGES', 'OverflowTabs'] },
  { id: 'stack', exports: ['Stack', 'layoutGap'] },
  { id: 'inline', exports: ['Inline'] },
  { id: 'grid', exports: ['Grid'] },
  { id: 'container', exports: ['Container'] },
  { id: 'scroll-area', exports: ['ScrollArea'] },
  {
    id: 'resizable-panels',
    exports: ['ResizableHandle', 'ResizablePanel', 'ResizablePanelGroup', 'ResizablePanels'],
  },
  { id: 'sticky', exports: ['Sticky'] },
  { id: 'heading', exports: ['HEADING_DEFAULT_SIZES', 'Heading'] },
  { id: 'text', exports: ['Text'] },
  { id: 'inline-code', exports: ['InlineCode'] },
  { id: 'prose', exports: ['Prose', 'ProseExclude'] },
  {
    id: 'tabular-number',
    exports: ['TABULAR_NUMBER_MESSAGES', 'TabularNumber', 'formatTabularNumber'],
  },
  { id: 'sortable-grid', exports: ['SortableGrid'] },
  { id: 'drag-handle', exports: ['DragHandle'] },
  {
    id: 'appearance-panel',
    exports: ['AppearancePanel', 'DEFAULT_APPEARANCE', 'MONARCH_CONTRAST_COOKIE'],
  },
  { id: 'live-region', exports: ['AnnouncerProvider', 'LiveRegion', 'useAnnouncer'] },
  {
    id: 'roving-focus-group',
    exports: ['ROVING_ITEM_ATTRIBUTE', 'RovingFocusGroup', 'useRovingFocus'],
  },
  { id: 'focus-trap', exports: ['FocusTrap', 'getFocusableElements', 'useFocusTrap'] },
  { id: 'data-grid', exports: ['DataGrid', 'defaultDataGridMessages'] },
  {
    id: 'editable-cell',
    exports: ['EditableCell', 'defaultEditableCellMessages', 'parseEditableCellDraft'],
  },
  { id: 'row-actions-menu', exports: ['RowActionsMenu', 'defaultRowActionsMenuMessages'] },
  { id: 'table-toolbar', exports: ['TableToolbar', 'defaultTableToolbarMessages'] },
  {
    id: 'view-switcher',
    exports: ['ViewSwitcher', 'defaultViewSwitcherMessages', 'viewSwitcherOptions'],
  },
  { id: 'virtual-list', exports: ['VirtualList', 'defaultVirtualListMessages', 'useVirtualizer'] },
  {
    id: 'column-mapping',
    exports: [
      'ColumnMapping',
      'columnMappingIssues',
      'defaultColumnMappingMessages',
      'mappingFromSuggestions',
      'normalizeColumnName',
      'suggestColumnMapping',
      'toFieldMapping',
    ],
  },
  { id: 'area-chart', exports: ['AreaChart'] },
  { id: 'stacked-bar-chart', exports: ['StackedBarChart'] },
  { id: 'funnel-chart', exports: ['FunnelChart'] },
  { id: 'gauge', exports: ['Gauge'] },
  { id: 'burndown-chart', exports: ['BurndownChart'] },
  { id: 'chart-legend', exports: ['ChartLegend'] },
  { id: 'reaction-bar', exports: ['ReactionBar'] },
  { id: 'comment-thread', exports: ['CommentThread'] },
  { id: 'presence-avatars', exports: ['PresenceAvatars'] },
  {
    id: 'screen-share-view',
    exports: ['SCREEN_SHARE_VIEW_MESSAGES', 'SCREEN_SHARE_ZOOM_LEVELS', 'ScreenShareView'],
  },
  { id: 'audio-level-meter', exports: ['AUDIO_LEVEL_METER_MESSAGES', 'AudioLevelMeter'] },
  { id: 'recording-indicator', exports: ['RECORDING_INDICATOR_MESSAGES', 'RecordingIndicator'] },
  {
    id: 'block-editor',
    exports: [
      'BLOCK_EDITOR_BLOCKS',
      'BlockEditor',
      'DEFAULT_BLOCK_EDITOR_MESSAGES',
      'DEFAULT_BLOCK_EDITOR_SLASH_MESSAGES',
    ],
  },
  {
    id: 'editor-toolbar',
    exports: [
      'DEFAULT_EDITOR_TOOLBAR_MESSAGES',
      'EDITOR_TOOLBAR_DEFAULT_ITEMS',
      'EDITOR_TOOLBAR_INLINE_ITEMS',
      'EditorToolbar',
    ],
  },
  {
    id: 'bubble-menu',
    exports: ['BubbleMenu', 'DEFAULT_BUBBLE_MENU_MESSAGES', 'defaultBubbleMenuShouldShow'],
  },
  {
    id: 'callout-block',
    exports: [
      'CALLOUT_TONES',
      'CalloutBlock',
      'CalloutBlockExtension',
      'DEFAULT_CALLOUT_BLOCK_MESSAGES',
    ],
  },
  {
    id: 'columns-block',
    exports: [
      'COLUMNS_MAX_RATIO',
      'COLUMNS_MIN_RATIO',
      'ColumnNode',
      'ColumnsBlock',
      'ColumnsBlockExtension',
      'DEFAULT_COLUMNS_BLOCK_MESSAGES',
      'clampColumnsRatio',
    ],
  },
  {
    id: 'toggle-block',
    exports: [
      'DEFAULT_TOGGLE_BLOCK_MESSAGES',
      'ToggleBlock',
      'ToggleBlockExtension',
      'ToggleContentNode',
      'ToggleSummaryNode',
    ],
  },
  {
    id: 'embed-block',
    exports: [
      'DEFAULT_EMBED_BLOCK_MESSAGES',
      'DEFAULT_EMBED_PROVIDERS',
      'EMBED_DEFAULT_SANDBOX',
      'EmbedBlock',
      'EmbedBlockExtension',
      'resolveEmbed',
    ],
  },
  {
    id: 'table-block',
    exports: [
      'DEFAULT_TABLE_BLOCK_MESSAGES',
      'TableBlock',
      'TableBlockExtension',
      'TableCellNode',
      'TableHeaderNode',
      'TableRowNode',
    ],
  },
  {
    id: 'page-outline',
    exports: ['DEFAULT_PAGE_OUTLINE_MESSAGES', 'PageOutline', 'collectOutline', 'outlineStats'],
  },
  {
    id: 'diff-viewer',
    exports: [
      'DEFAULT_DIFF_VIEWER_MESSAGES',
      'DiffViewer',
      'diffStats',
      'diffTextLines',
      'diffTextWords',
    ],
  },
  { id: 'file-card', exports: ['FILE_CARD_MESSAGES', 'FileCard'] },
  { id: 'file-list', exports: ['FILE_LIST_MESSAGES', 'FileList'] },
  { id: 'file-preview', exports: ['FILE_PREVIEW_MESSAGES', 'FilePreview', 'filePreviewKind'] },
  { id: 'drop-zone', exports: ['DROP_ZONE_MESSAGES', 'DropZone'] },
  {
    id: 'avatar-upload',
    exports: [
      'AVATAR_UPLOAD_MAX_ZOOM',
      'AVATAR_UPLOAD_MESSAGES',
      'AVATAR_UPLOAD_MIN_ZOOM',
      'AvatarUpload',
      'avatarCropRect',
    ],
  },
  { id: 'attachment-chip', exports: ['ATTACHMENT_CHIP_MESSAGES', 'AttachmentChip'] },
  {
    id: 'file-type-icon',
    exports: [
      'FILE_TYPE_FAMILIES',
      'FILE_TYPE_ICON_MESSAGES',
      'FileTypeGlyph',
      'FileTypeIcon',
      'fileExtension',
      'getFileTypeFamily',
    ],
  },
  { id: 'issue-card', exports: ['ISSUE_CARD_MESSAGES', 'IssueCard', 'isIssueOverdue'] },
  {
    id: 'status-picker',
    exports: ['STATUS_CATEGORIES', 'STATUS_PICKER_MESSAGES', 'StatusGlyph', 'StatusPicker'],
  },
  {
    id: 'priority-picker',
    exports: [
      'PRIORITY_LEVELS',
      'PRIORITY_PICKER_MESSAGES',
      'PriorityGlyph',
      'PriorityPicker',
      'priorityFromNumber',
      'priorityToNumber',
    ],
  },
  {
    id: 'dependency-picker',
    exports: ['DEPENDENCY_PICKER_MESSAGES', 'DEPENDENCY_RELATIONS', 'DependencyPicker'],
  },
  { id: 'user-hover-card', exports: ['UserHoverCard', 'defaultUserHoverCardMessages'] },
  { id: 'role-picker', exports: ['RolePicker', 'defaultRolePickerMessages'] },
  { id: 'permission-matrix', exports: ['PermissionMatrix', 'defaultPermissionMatrixMessages'] },
  {
    id: 'invite-members-dialog',
    exports: ['InviteMembersDialog', 'defaultInviteMembersDialogMessages'],
  },
  {
    id: 'pending-invites-list',
    exports: ['PendingInvitesList', 'defaultPendingInvitesListMessages'],
  },
  { id: 'account-menu', exports: ['AccountMenu', 'defaultAccountMenuMessages'] },
  {
    id: 'impersonation-banner',
    exports: ['ImpersonationBanner', 'defaultImpersonationBannerMessages'],
  },
  { id: 'passkey-manager', exports: ['PasskeyManager', 'defaultPasskeyManagerMessages'] },
  {
    id: 'recovery-codes',
    exports: ['RecoveryCodes', 'defaultRecoveryCodesMessages', 'formatRecoveryCodes'],
  },
  {
    id: 'oauth-consent-screen',
    exports: ['OAuthConsentScreen', 'defaultOAuthConsentScreenMessages'],
  },
  { id: 'sign-in-form', exports: ['SignInForm', 'defaultSignInFormMessages', 'plainAuthLink'] },
  { id: 'sign-up-form', exports: ['SignUpForm', 'defaultSignUpFormMessages'] },
  { id: 'password-reset-form', exports: ['PasswordResetForm', 'defaultPasswordResetFormMessages'] },
  {
    id: 'email-verification-banner',
    exports: [
      'EmailVerificationBanner',
      'defaultEmailVerificationBannerMessages',
      'useResendCooldown',
    ],
  },
  {
    id: 'country-select',
    exports: [
      'COUNTRY_CODES',
      'CountrySelect',
      'getCountryFlag',
      'getCountryName',
      'listCountries',
    ],
  },
  { id: 'region-select', exports: ['BUILT_IN_REGIONS', 'RegionSelect', 'getBuiltInRegions'] },
  {
    id: 'currency-select',
    exports: [
      'CURRENCY_CODES',
      'CurrencySelect',
      'getCurrencyName',
      'getCurrencySymbol',
      'listCurrencies',
    ],
  },
  {
    id: 'language-select',
    exports: [
      'LANGUAGE_TAGS',
      'LanguageSelect',
      'canonicalLanguageTag',
      'getLanguageName',
      'listLanguages',
    ],
  },
  {
    id: 'phone-country-select',
    exports: ['PHONE_COUNTRIES', 'PhoneCountrySelect', 'getPhoneCountry', 'matchPhoneCountry'],
  },
  { id: 'address-autocomplete', exports: ['AddressAutocomplete', 'toAddressValue'] },
  { id: 'faceted-filter-panel', exports: ['FacetedFilterPanel', 'facetActiveCount'] },
  { id: 'search-results', exports: ['SearchResults'] },
  {
    id: 'search-result-item',
    exports: ['HighlightText', 'SearchResultItem', 'highlightSegments', 'queryTerms'],
  },
  {
    id: 'query-builder',
    exports: [
      'QueryBuilder',
      'createQueryGroup',
      'evaluateQuery',
      'flattenQueryRules',
      'validateQuery',
    ],
  },
  {
    id: 'pricing-table',
    exports: [
      'BillingPeriodToggle',
      'PricingTable',
      'defaultPricingTableMessages',
      'groupPricingFeatures',
      'planPriceFor',
      'useBillingPeriod',
    ],
  },
  { id: 'plan-card', exports: ['PlanCard', 'defaultPlanCardMessages'] },
  { id: 'invoice-view', exports: ['InvoiceView', 'defaultInvoiceViewMessages'] },
  {
    id: 'payment-method-card',
    exports: [
      'PaymentMethodCard',
      'cardBrandNames',
      'cardExpiryStatus',
      'defaultPaymentMethodCardMessages',
    ],
  },
  { id: 'code-snippet-tabs', exports: ['CodeSnippetTabs'] },
  {
    id: 'json-viewer',
    exports: ['JSONViewer', 'JSON_VIEWER_MESSAGES', 'formatJsonPath', 'stringifyJsonValue'],
  },
  { id: 'api-key-row', exports: ['ApiKeyRow'] },
  {
    id: 'log-viewer',
    exports: ['LOG_LEVELS', 'LOG_VIEWER_MESSAGES', 'LogViewer', 'formatLogTime'],
  },
  { id: 'pricing-section', exports: ['PricingSection', 'defaultPricingSectionMessages'] },
  { id: 'comparison-section', exports: ['ComparisonSection', 'defaultComparisonSectionMessages'] },
  { id: 'logo-cloud', exports: ['LogoCloud', 'defaultLogoCloudMessages'] },
  { id: 'changelog-feed', exports: ['ChangelogFeed', 'defaultChangelogFeedMessages'] },
  { id: 'announcement-bar', exports: ['AnnouncementBar', 'defaultAnnouncementBarMessages'] },
  {
    id: 'dashboard-operational',
    exports: [
      'DashboardOperational',
      'defaultDashboardOperationalMessages',
      'isDashboardSlotConfig',
    ],
  },
  { id: 'dashboard-pipeline', exports: ['DashboardPipeline', 'defaultDashboardPipelineMessages'] },
  {
    id: 'dashboard-portfolio',
    exports: ['DashboardPortfolio', 'defaultDashboardPortfolioMessages'],
  },
  {
    id: 'dashboard-personal-work',
    exports: ['DashboardPersonalWork', 'defaultDashboardPersonalWorkMessages'],
  },
  { id: 'settings-page', exports: ['SettingsPage', 'defaultSettingsPageMessages'] },
  { id: 'record-page', exports: ['RecordPage', 'defaultRecordPageMessages'] },
  { id: 'auth-page', exports: ['AuthPage', 'defaultAuthPageMessages'] },
  {
    id: 'ai-connector-picker',
    exports: [
      'AI_CONNECTOR_PICKER_MESSAGES',
      'AiConnectorPicker',
      'aiConnectorCounts',
      'filterAiConnectors',
      'isAiConnectorActive',
    ],
  },
  {
    id: 'ai-background-tasks',
    exports: [
      'AI_BACKGROUND_TASKS_MESSAGES',
      'AiBackgroundTasks',
      'formatAiBackgroundTaskElapsed',
      'groupAiBackgroundTasks',
      'useAiRunNotifications',
    ],
  },
  {
    id: 'ai-subagent-tree',
    exports: [
      'AI_SUBAGENT_STATUSES',
      'AI_SUBAGENT_TREE_MESSAGES',
      'AiSubagentTree',
      'countSubagentStatuses',
      'defaultExpandedSubagentIds',
      'flattenSubagentTree',
    ],
  },
  {
    id: 'ai-spend-cap',
    exports: [
      'AI_SPEND_CAP_MESSAGES',
      'AiSpendCap',
      'aiSpendAmountFromMajor',
      'aiSpendAmountToMajor',
      'aiSpendCapState',
      'formatAiSpendAmount',
    ],
  },
  {
    id: 'ai-voice-mode',
    exports: [
      'AI_VOICE_MODE_MESSAGES',
      'AiVoiceMode',
      'formatAiVoiceDuration',
      'useAiVoiceSession',
    ],
  },
  {
    id: 'ai-claim-sources',
    exports: [
      'AI_CLAIM_SOURCES_MESSAGES',
      'AiClaimSources',
      'aiClaimCounts',
      'aiClaimNeedsChecking',
      'aiClaimSummary',
    ],
  },
  {
    id: 'ai-trace-view',
    exports: [
      'AI_TRACE_VIEW_MESSAGES',
      'AiTraceView',
      'aiTraceDefaultExpandedIds',
      'aiTraceTotals',
      'formatAiTraceCost',
      'formatAiTraceDuration',
    ],
  },
  {
    id: 'ai-shared-session',
    exports: [
      'AI_SHARED_SESSION_MESSAGES',
      'AiSharedSession',
      'aiSharedSessionStatus',
      'aiSharedSessionStatusLine',
      'groupAiSharedSessionParticipants',
      'resolveAiSharedSessionMessages',
    ],
  },
  {
    id: 'ai-computer-view',
    exports: [
      'AI_COMPUTER_VIEW_MESSAGES',
      'AiComputerView',
      'aiComputerViewActionDetail',
      'aiComputerViewPointPosition',
    ],
  },
];

/**
 * Registry entries shipped by another package (not a React component folder).
 * Each export must appear in that package's index; the docs page is a guide.
 */
const PACKAGE_EXPORT_GROUPS = [
  {
    id: 'brand-theme-engine',
    package: '@rookcom/monarch-tokens',
    index: 'packages/tokens/src/index.ts',
    source: 'packages/tokens/src/brand.ts',
    docs: { route: '/theming', guide: 'docs/guides/theming.md' },
    exports: ['brandThemeCss', 'deriveTheme', 'parseHexColor'],
  },
];

/** Public utilities that live in src/internal rather than a component folder. */
const INTERNAL_EXPORTS = new Set(['cx', 'DirectionProvider']);

const ACRONYMS = { otp: 'OTP', url: 'URL', ui: 'UI', kpi: 'KPI' };
/** Registry names are derived from the id so they stay generic; the first
 * research finding's name is often product-flavoured ("Modal Dialog"). */
const titleFor = (id) =>
  id
    .split('-')
    .map((w) => ACRONYMS[w] ?? w[0].toUpperCase() + w.slice(1))
    .join(' ');

export const inScope = (c) => c.productCount >= 3 || (c.productCount >= 2 && c.priority === 'P0');

/**
 * Component folders (docs/architecture/SOURCE-LAYOUT.md): where each export
 * lives and whether its docs page is complete. Docs files are imported, so the
 * package must be built first (they import '@rookcom/monarch-react').
 */
export async function componentFolders() {
  const out = [];
  for (const f of listFolders()) {
    const docs = docsFileOf(f) ? await loadDocs(f) : undefined;
    const sections = docs?.sections ?? {};
    const status = !docs
      ? 'missing'
      : DOC_SECTIONS.every((s) => sections[s] && !isIncomplete(sections[s]))
        ? 'complete'
        : 'legacy-incomplete';
    out.push({
      id: f.id,
      source: `packages/react/src/${f.rel}`,
      exports: valueExports(join(f.dir, 'index.ts')),
      docs: status,
    });
  }
  if (out.length === 0) throw new Error('no component folders found in packages/react/src');
  return out;
}

export async function loadInputs() {
  const manifestPath = 'packages/react/dist/manifest.json';
  if (!existsSync(join(ROOT, manifestPath)))
    throw new Error(`${manifestPath} missing: run \`npm run build\` first`);
  return {
    consolidated: read('registry/component-demand.json'),
    manifest: read(manifestPath),
    waves: read('registry/roadmap-waves.json'),
    strategic: read('registry/strategic.json'),
    deprecations: read('registry/deprecations.json').deprecations,
    folders: await componentFolders(),
    packageIndexes: Object.fromEntries(
      PACKAGE_EXPORT_GROUPS.map((g) => [g.index, readFileSync(join(ROOT, g.index), 'utf8')]),
    ),
    guides: Object.fromEntries(
      PACKAGE_EXPORT_GROUPS.map((g) => [g.docs.guide, existsSync(join(ROOT, g.docs.guide))]),
    ),
  };
}

export function buildRegistry(inputs) {
  const { consolidated, manifest, waves, deprecations, folders } = inputs;
  const strategic = inputs.strategic ?? { families: [] };
  const packageIndexes = inputs.packageIndexes ?? {};
  const guides = inputs.guides ?? {};
  const discovery = new Map(consolidated.components.map((c) => [c.id, c]));
  const exportNames = manifest.components.map((c) => c.name);
  const errors = [];

  // Exports → entry
  const exportsById = new Map();
  const metaById = new Map();
  const seen = new Set();
  for (const g of EXPORT_GROUPS) {
    for (const e of g.exports) {
      if (!exportNames.includes(e))
        errors.push(`EXPORT_GROUPS lists ${e}, which the manifest does not export`);
      if (seen.has(e)) errors.push(`${e} is in two EXPORT_GROUPS`);
      seen.add(e);
    }
    exportsById.set(g.id, g.exports.filter((e) => exportNames.includes(e)).sort());
    if (g.meta) metaById.set(g.id, g.meta);
    else if (
      !discovery.has(g.id) &&
      !strategic.families.some((f) => f.components.some((c) => c.id === g.id))
    )
      errors.push(`EXPORT_GROUPS ${g.id} needs meta (no demand data)`);
  }
  // An export of a component folder whose id already has a group joins that
  // group: the folder's index.ts is the source of truth for what it ships.
  const groupIds = new Set(EXPORT_GROUPS.map((g) => g.id));
  for (const f of folders) {
    if (!groupIds.has(f.id)) continue;
    const extra = f.exports.filter((e) => !seen.has(e) && exportNames.includes(e));
    if (!extra.length) continue;
    for (const e of extra) seen.add(e);
    exportsById.set(f.id, [...(exportsById.get(f.id) ?? []), ...extra].sort());
  }
  for (const e of exportNames)
    if (!seen.has(e)) errors.push(`export ${e} is not in EXPORT_GROUPS (build-components.mjs)`);

  // Strategic families → entry (ADR 0011)
  const strategicById = new Map();
  for (const f of strategic.families)
    for (const c of f.components) {
      if (strategicById.has(c.id)) errors.push(`strategic.json: ${c.id} appears twice`);
      if (discovery.has(c.id))
        errors.push(`strategic.json: ${c.id} has demand data; keep it in one input only`);
      strategicById.set(c.id, { ...c, wave: f.wave, family: f.family });
    }

  // Waves → entry
  const waveById = new Map();
  for (const w of waves.waves)
    for (const g of w.groups)
      for (const id of g.ids) {
        if (waveById.has(id)) errors.push(`roadmap-waves.json: ${id} appears twice`);
        if (!discovery.has(id)) errors.push(`roadmap-waves.json: ${id} has no demand data`);
        if (strategicById.has(id))
          errors.push(`roadmap-waves.json: ${id} is strategic; its wave comes from strategic.json`);
        waveById.set(id, { wave: w.wave, package: g.package, entry: g.entry });
      }

  const folderByExport = new Map();
  for (const f of folders)
    for (const e of f.exports) {
      if (folderByExport.has(e)) errors.push(`${e} is exported by two component folders`);
      folderByExport.set(e, f);
    }
  for (const e of exportNames)
    if (!folderByExport.has(e) && !INTERNAL_EXPORTS.has(e))
      errors.push(`export ${e} does not come from a component folder`);
  for (const g of EXPORT_GROUPS) {
    const homes = new Set(g.exports.map((e) => folderByExport.get(e)?.id));
    if (homes.size > 1)
      errors.push(
        `EXPORT_GROUPS ${g.id} spans folders ${[...homes].join(', ')}; keep a group in one folder`,
      );
  }
  // Only deprecated exports: an export that moved to another entry
  // (`importPaths` in deprecations.json) is not deprecated itself.
  const deprecated = new Set(
    deprecations.filter((d) => d.package === '@rookcom/monarch-react').map((d) => d.name),
  );

  const packageGroups = new Map();
  for (const g of PACKAGE_EXPORT_GROUPS) {
    const text = packageIndexes[g.index] ?? '';
    for (const e of g.exports)
      if (!new RegExp(`\\b${e}\\b`).test(text))
        errors.push(`PACKAGE_EXPORT_GROUPS ${g.id}: ${g.index} does not export ${e}`);
    if (!discovery.has(g.id)) errors.push(`PACKAGE_EXPORT_GROUPS ${g.id} has no demand data`);
    packageGroups.set(g.id, g);
    exportsById.set(g.id, [...g.exports].sort());
  }

  const ids = new Set([
    ...consolidated.components.filter(inScope).map((c) => c.id),
    ...waveById.keys(),
    ...strategicById.keys(),
    ...exportsById.keys(),
  ]);

  const nameRe = new Map(exportNames.map((n) => [n, new RegExp(`\\b${n}\\b`)]));
  const entries = [];
  for (const id of [...ids].sort()) {
    const d = discovery.get(id);
    const st = strategicById.get(id);
    const meta = metaById.get(id) ?? st;
    const exps = exportsById.get(id) ?? [];
    const w =
      waveById.get(id) ??
      (st ? { wave: st.wave, package: '@rookcom/monarch-react', entry: st.entry } : undefined);
    const scoped = d ? inScope(d) : false;
    const missing = d ? d.missingInMonarch : [];

    let gap;
    if (exps.length)
      gap = d
        ? missing.length
          ? 'implemented-incomplete'
          : 'implemented-complete'
        : 'not-assessed';
    else if (
      d &&
      (d.layer === 'product-extension' || (!scoped && d.gapDraft === 'product-extension'))
    )
      gap = 'product-extension';
    else gap = 'missing';

    if (scoped && !w && gap !== 'implemented-complete')
      errors.push(`${id} is in scope and ${gap} but has no wave in roadmap-waves.json`);

    const folder = exps.map((e) => folderByExport.get(e)).find(Boolean);
    const buildsOn = d
      ? exportNames
          .filter(
            (n) => !exps.includes(n) && d.monarchEquivalent.some((s) => nameRe.get(n).test(s)),
          )
          .sort()
      : [];

    entries.push({
      id,
      name: meta?.name ?? titleFor(id),
      category: d?.category ?? meta.category,
      layer: d?.layer ?? meta.layer,
      package: w?.package ?? '@rookcom/monarch-react',
      entry: w?.entry ?? '.',
      exports: exps,
      status: statusOf({
        deprecated: exps.some((e) => deprecated.has(e)),
        shipped: exps.length > 0,
        gap,
        docs: folder?.docs,
      }),
      scope: st ? 'strategic' : scoped ? 'in-scope' : w ? 'roadmap' : 'shipped',
      sourceProducts: d?.sourceProducts ?? [],
      productCount: d?.productCount ?? 0,
      priority: d?.priority ?? 'none',
      wave: w?.wave ?? 0,
      gap,
      missingCapabilities: gap === 'implemented-complete' ? [] : missing,
      buildsOn,
      ...(folder
        ? { source: folder.source }
        : packageGroups.has(id)
          ? { source: packageGroups.get(id).source }
          : {}),
      docs: folder
        ? { route: `/components/${folder.id}`, status: folder.docs }
        : packageGroups.has(id)
          ? {
              route: packageGroups.get(id).docs.route,
              status: guides[packageGroups.get(id).docs.guide] ? 'complete' : 'missing',
            }
          : { route: `/components/${id}`, status: exps.length ? 'missing' : 'planned' },
    });
  }

  const count = (list, key) =>
    Object.fromEntries(
      Object.entries(
        list.reduce((acc, e) => ((acc[e[key]] = (acc[e[key]] ?? 0) + 1), acc), {}),
      ).sort(([a], [b]) => (a < b ? -1 : 1)),
    );
  const scopedEntries = entries.filter((e) => e.scope === 'in-scope');
  const registry = {
    $schema: './components.schema.json',
    $description:
      'Generated by scripts/registry/build-components.mjs. Do not edit; change the inputs and run `npm run registry:components`.',
    monarchReactVersion: manifest.version,
    sources: {
      demand: 'registry/component-demand.json',
      manifest: 'packages/react/dist/manifest.json',
      waves: 'registry/roadmap-waves.json',
      strategic: 'registry/strategic.json',
      deprecations: 'registry/deprecations.json',
      docs: 'packages/react/src/**/<id>.docs.tsx',
    },
    summary: {
      entries: entries.length,
      demandComponents: consolidated.components.length,
      byScope: count(entries, 'scope'),
      inScopeByGap: count(scopedEntries, 'gap'),
      inScopeByWave: count(scopedEntries, 'wave'),
      byStatus: count(entries, 'status'),
    },
    components: entries,
  };
  return { registry, errors };
}

export async function formatFor(file, text) {
  const options = (await prettier.resolveConfig(join(ROOT, file))) ?? {};
  return prettier.format(text, { ...options, filepath: join(ROOT, file) });
}

/**
 * Release status of a registry entry, from data the registry already holds:
 *   deprecated  an export is in registry/deprecations.json
 *   planned     nothing is exported yet
 *   incomplete  exported, but products recorded capabilities it still lacks (gap)
 *   beta        exported and capable, but its docs page is not complete yet
 *   stable      exported, no recorded gap, complete docs
 */
export function statusOf({ deprecated, shipped, gap, docs }) {
  if (deprecated) return 'deprecated';
  if (!shipped) return 'planned';
  if (gap === 'implemented-incomplete') return 'incomplete';
  if (docs !== undefined && docs !== 'complete') return 'beta';
  return 'stable';
}

/** Statuses of an entry that ships today. */
export const SHIPPED_STATUSES = new Set(['stable', 'beta', 'incomplete']);

/**
 * The directive's 500-component catalog (registry/catalog.json, ADR 0014): an
 * id is complete when its registry entry, or the entry of the folder that
 * ships it (`shippedBy`), ships (stable, beta or incomplete). Every id must resolve to an entry.
 */
export function catalogProgress(registry) {
  const status = new Map(registry.components.map((c) => [c.id, c.status]));
  const errors = [];
  let complete = 0;
  const { components } = read('registry/catalog.json');
  for (const { id, shippedBy } of components) {
    const s = status.get(shippedBy ?? id);
    if (s === undefined)
      errors.push(
        `registry/catalog.json: ${id} has no registry entry${shippedBy ? ` (shippedBy ${shippedBy})` : ''}`,
      );
    else if (SHIPPED_STATUSES.has(s)) complete += 1;
  }
  return { complete, total: components.length, errors };
}

async function main() {
  const check = process.argv.includes('--check');
  const { registry, errors } = buildRegistry(await loadInputs());
  const catalog = catalogProgress(registry);
  errors.push(...catalog.errors);
  const schema = read(SCHEMA);
  assertSupported(schema);
  errors.push(...validate(registry, schema).map((e) => `${OUTPUT}: ${e}`));
  if (errors.length) {
    for (const e of errors) console.error(`error: ${e}`);
    process.exit(1);
  }
  const text = await formatFor(OUTPUT, JSON.stringify(registry, null, 2));
  const target = join(ROOT, OUTPUT);
  if (check) {
    const current = existsSync(target) ? readFileSync(target, 'utf8') : '';
    if (current !== text) {
      console.error(
        `${OUTPUT} is stale: run \`npm run registry:components\` and commit the result`,
      );
      process.exit(1);
    }
    console.log(
      `${OUTPUT} is current (${registry.components.length} entries; catalog ${catalog.complete} / ${catalog.total} complete)`,
    );
    return;
  }
  writeFileSync(target, text);
  console.log(`${relative(ROOT, target)}: ${registry.components.length} entries`);
  console.log(`catalog: ${catalog.complete} / ${catalog.total} complete`);
  console.log(JSON.stringify(registry.summary, null, 2));
}

if (import.meta.url === pathToFileURL(process.argv[1]).href) await main();
