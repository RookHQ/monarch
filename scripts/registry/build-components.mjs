// Generates registry/components.json, the machine-readable component registry.
//
//   npm run registry:components         write it
//   npm run registry:components:check   fail if it is stale or invalid (CI)
//
// Inputs (never hand-edit the output; change these instead):
//   registry/component-demand.json        how many products need each component, and capability gaps
//   packages/react/dist/manifest.json      what Monarch exports today (npm run build)
//   registry/roadmap-waves.json            wave, package and entry point per component
//   registry/deprecations.json             deprecated exports
//   packages/react/src/**/<id>/             component folders and their <id>.docs.tsx
//
// Scope rule (docs/roadmap/COMPONENT-IMPLEMENTATION-ROADMAP.md): a component is
// in scope when 3+ products need it, or 2+ need it at P0.
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
 * Every Monarch export belongs to exactly one registry entry. New exports must
 * be added here; the build fails otherwise, so the registry can never silently
 * miss a shipped component. `meta` is only for entries with no demand data.
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
  { id: 'toast', exports: ['ToastProvider', 'useToast', 'getErrorMessage'] },
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
  { id: 'segmented-control', exports: ['SegmentedControl', 'ToggleGroup'] },
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
const INTERNAL_EXPORTS = new Set(['cx']);

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
    else if (!discovery.has(g.id)) errors.push(`EXPORT_GROUPS ${g.id} needs meta (no demand data)`);
  }
  for (const e of exportNames)
    if (!seen.has(e)) errors.push(`export ${e} is not in EXPORT_GROUPS (build-components.mjs)`);

  // Waves → entry
  const waveById = new Map();
  for (const w of waves.waves)
    for (const g of w.groups)
      for (const id of g.ids) {
        if (waveById.has(id)) errors.push(`roadmap-waves.json: ${id} appears twice`);
        if (!discovery.has(id)) errors.push(`roadmap-waves.json: ${id} has no demand data`);
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
    ...exportsById.keys(),
  ]);

  const nameRe = new Map(exportNames.map((n) => [n, new RegExp(`\\b${n}\\b`)]));
  const entries = [];
  for (const id of [...ids].sort()) {
    const d = discovery.get(id);
    const meta = metaById.get(id);
    const exps = exportsById.get(id) ?? [];
    const w = waveById.get(id);
    const scoped = d ? inScope(d) : false;
    const missing = d ? d.missingInMonarch : [];

    let gap;
    if (exps.length)
      gap = d
        ? missing.length
          ? 'implemented-incomplete'
          : 'implemented-complete'
        : 'not-assessed';
    else if (d.layer === 'product-extension' || (!scoped && d.gapDraft === 'product-extension'))
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
      status: exps.some((e) => deprecated.has(e))
        ? 'deprecated'
        : exps.length
          ? 'stable'
          : 'planned',
      scope: scoped ? 'in-scope' : w ? 'roadmap' : 'shipped',
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

async function main() {
  const check = process.argv.includes('--check');
  const { registry, errors } = buildRegistry(await loadInputs());
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
    console.log(`${OUTPUT} is current (${registry.components.length} entries)`);
    return;
  }
  writeFileSync(target, text);
  console.log(`${relative(ROOT, target)}: ${registry.components.length} entries`);
  console.log(JSON.stringify(registry.summary, null, 2));
}

if (import.meta.url === pathToFileURL(process.argv[1]).href) await main();
