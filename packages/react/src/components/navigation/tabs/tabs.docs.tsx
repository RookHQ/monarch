import { useState } from 'react';
import {
  Tabs,
  TabsContent,
  TabsFilter,
  TabsLink,
  TabsList,
  TabsNav,
  TabsTrigger,
} from '@rookcom/monarch-react';
import type { ComponentDocs } from '../../../internal/component-docs.js';

const STATUS = [
  { value: 'all', label: 'All', count: 24 },
  { value: 'open', label: 'Open', count: 6, countTone: 'accent' as const },
  { value: 'waiting', label: 'Waiting', count: 3, countTone: 'warning' as const },
  { value: 'closed', label: 'Closed', count: 15 },
];

function FilterDemo() {
  const [status, setStatus] = useState('open');
  return (
    <TabsFilter
      aria-label="Status"
      options={STATUS}
      scrollable
      value={status}
      onValueChange={setStatus}
    >
      Showing {STATUS.find((s) => s.value === status)?.label.toLowerCase()} requests.
    </TabsFilter>
  );
}

const SECTIONS = [
  'Overview',
  'Activity',
  'Files',
  'Members',
  'Integrations',
  'Billing',
  'Audit log',
  'Settings',
];

export default {
  id: 'tabs',
  title: 'Tabs',
  summary: 'Line and pill tab sets, link tabs, filter tabs, counts and scrolling lists.',
  sections: {
    purpose: {
      summary:
        'Switches between panels of related content in the same place, showing one panel at a time.',
      useWhen: [
        'A page or card has sections that are peers and the user looks at one at a time (`line`).',
        'A compact switch between views of the same data, such as Day / Week / Month (`pill`).',
        'Sections of a record are separate routes but should look like tabs: TabsNav + TabsLink.',
        'Tabs filter one list rather than switching panels ("All · Open · Closed" with counts): TabsFilter.',
      ],
      avoidWhen: [
        'Each section is a separate page and should not look like tabs: use ordinary navigation links.',
        'The content is a sequence of steps: tabs imply free order.',
        'The user must compare panels side by side.',
      ],
    },
    designPrinciples: {
      items: [
        '`line` for page sections, `pill` for compact view switches.',
        'Short, noun labels that do not wrap (`white-space: nowrap`).',
        'The active tab is shown by text colour plus an accent underline (`line`) or a raised surface (`pill`), never by colour alone.',
        'Semantics follow behaviour: panels use the tab pattern, pages use links with `aria-current`, filters use tabs with one shared panel.',
        'No router dependency: link tabs render your router’s link with `asChild`, URL state is injected through `value` / `onValueChange` (or `useTabsUrlBinding`).',
        'A count belongs in the tab’s name, so it is plain text inside the trigger, not a decorative badge.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        '`Tabs` is the Radix Tabs Root: `value` / `defaultValue` / `onValueChange`, `orientation`, `activationMode` and `dir` come from @radix-ui/react-tabs.',
        '`TabsList`, `TabsTrigger` and `TabsContent` forward their refs to the Radix parts.',
        '`TabsNav` renders a `<nav>` (ref to it) and `TabsLink` an `<a>` (or the child with `asChild`); all anchor attributes pass through.',
        '`TabsFilter` renders a wrapper `<div>` (ref) with the tablist and, when given `children`, one shared tabpanel. Its `options` are data: `{ value, label, count?, countTone?, disabled? }`.',
        '`useTabsUrlBinding({ value, onChange, values, defaultValue, clearDefault })` returns `{ value, onValueChange }` to spread on Tabs or TabsFilter: unknown URL values fall back to the default, and choosing the default writes `null` so the param is removed.',
      ],
    },
    typedProperties: { source: 'typescript' },
    variants: {
      items: [
        {
          prop: 'variant (TabsList)',
          values: ['line', 'pill'],
          description:
            '`line` (default) draws a bottom border with an accent underline on the active tab; `pill` is an inline, muted track with the active tab on a raised surface. Exposed as `data-variant` on the list. Also on TabsNav and TabsFilter.',
        },
        {
          prop: 'scrollable (TabsList, TabsNav, TabsFilter)',
          values: ['true', 'false'],
          description:
            'The list scrolls sideways instead of overflowing when tabs do not fit (`data-scrollable`). The line border becomes an inset shadow so the underline is not clipped, and the focus ring is drawn inside.',
        },
        {
          prop: 'count / countTone (TabsTrigger, TabsLink, TabsFilter options)',
          values: ['ReactNode', 'neutral', 'accent', 'info', 'success', 'warning', 'danger'],
          description:
            'A count bubble after the label (`.mn-tabs__count`, `data-tone`). Part of the accessible name.',
        },
        {
          prop: 'active (TabsLink)',
          values: ['true', 'false'],
          description: 'The current page: `aria-current="page"` and `data-state="active"`.',
        },
        {
          prop: 'activationMode (TabsFilter)',
          values: ['automatic', 'manual'],
          description:
            '`automatic` (default) applies a filter when its tab gets focus; `manual` waits for Enter or Space.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Active',
          attribute: 'data-state="active", aria-selected="true"',
          description:
            'Default text colour; accent underline (`line`) or surface and shadow (`pill`).',
        },
        { name: 'Inactive', attribute: 'data-state="inactive"', description: 'Muted text colour.' },
        { name: 'Hover', description: 'Inactive triggers take the default text colour.' },
        {
          name: 'Disabled',
          attribute: 'data-disabled, disabled',
          description: 'Disabled text colour and a not-allowed cursor; skipped by arrow keys.',
        },
        {
          name: 'Focus',
          description: 'Triggers and panels show the Monarch focus ring on `:focus-visible` only.',
        },
        {
          name: 'Current page (TabsLink)',
          attribute: 'aria-current="page", data-state="active"',
          description: 'Styled exactly like an active tab.',
        },
      ],
    },
    interaction: {
      items: [
        'Clicking a trigger selects it and shows its panel.',
        'By default a tab is selected as soon as it receives focus (Radix `activationMode="automatic"`); pass `activationMode="manual"` to select only with Enter or Space.',
        'Only the active panel is rendered; inactive panels are unmounted unless `forceMount` is set on TabsContent.',
        'Controlled with `value` + `onValueChange`, or uncontrolled with `defaultValue`. TabsFilter works the same way; uncontrolled it starts on the first enabled option.',
        'TabsLink navigates like any link; the app decides which one is `active` from its router.',
        'TabsFilter has no per-tab panels. With `children`, one shared panel is labelled by the selected tab; without, the tabs carry no `aria-controls`.',
        'In a scrollable list, focusing a tab scrolls it into view.',
      ],
    },
    keyboard: {
      items: [
        { keys: 'Tab', action: 'Moves focus to the active tab, then into the active panel.' },
        {
          keys: 'ArrowRight / ArrowLeft',
          action:
            'Moves to the next or previous enabled tab (horizontal orientation), wrapping at the ends.',
        },
        { keys: 'ArrowDown / ArrowUp', action: 'Same, when `orientation="vertical"`.' },
        { keys: 'Home / End', action: 'Moves to the first or last enabled tab.' },
        { keys: 'Enter / Space', action: 'Selects the focused tab in manual activation mode.' },
        {
          keys: 'Tab (TabsNav)',
          action:
            'Link tabs are ordinary links: Tab moves between them and Enter follows the link. There is no arrow-key roving, because they are not a tablist.',
        },
      ],
      notes: [
        'TabsFilter implements the same model as Tabs: ArrowLeft/Right (mirrored in right-to-left), Home/End, disabled tabs skipped, one tab stop.',
      ],
    },
    accessibility: {
      role: 'tablist, tab and tabpanel',
      name: 'Give TabsList an `aria-label` (or `aria-labelledby`) naming the set, such as "Views".',
      items: [
        'Triggers expose `aria-selected` and `aria-controls`; panels are labelled by their trigger.',
        'Roving tabindex: only the active tab is in the Tab order.',
        'The active panel is focusable so keyboard users can reach content that has no focusable element.',
        'Verified with axe in jsdom and in Chromium, Firefox and WebKit on /components/tabs.',
        'TabsNav is a `navigation` landmark and requires `aria-label`; TabsLink marks the current page with `aria-current="page"`, never `aria-selected`.',
        'TabsFilter requires `aria-label`; tabs expose `aria-selected`, and `aria-controls` only when the shared panel exists.',
        'Counts are text inside the trigger, so "Open 6" is announced; pass a string such as "6 new" when the number alone is unclear.',
      ],
    },
    responsive: {
      items: [
        'The list is a single flex row that does not wrap and labels do not wrap. Set `scrollable` when the tabs may not fit at 320px; otherwise keep the set small or use `pill`.',
        'Trigger height comes from `--mn-control-md` and does not change with viewport width.',
        'A `scrollable` list takes the width of its container (`contain: inline-size`), so it never widens the layout; give it a block or full-width parent.',
      ],
    },
    theme: {
      items: [
        'Colours are Monarch semantic tokens only, so light, dark and more-contrast themes apply automatically.',
        'The `line` underline uses `--mn-color-accent-strong`, so it follows the product accent.',
        'The `pill` track uses `--mn-color-bg-muted` and the active pill `--mn-color-bg-surface` with `--mn-shadow-xs`.',
        'Count tones use each intent’s `subtle` and `strong` tokens, which meet text contrast in every theme.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Line and pill',
          stack: true,
          render: () => (
            <>
              <Tabs defaultValue="board">
                <TabsList aria-label="Views">
                  <TabsTrigger value="board">Board</TabsTrigger>
                  <TabsTrigger value="list">List</TabsTrigger>
                  <TabsTrigger value="timeline" disabled>
                    Timeline
                  </TabsTrigger>
                </TabsList>
                <TabsContent value="board">Board view</TabsContent>
                <TabsContent value="list">List view</TabsContent>
              </Tabs>
              <Tabs defaultValue="week">
                <TabsList variant="pill" aria-label="Range">
                  <TabsTrigger value="day">Day</TabsTrigger>
                  <TabsTrigger value="week">Week</TabsTrigger>
                  <TabsTrigger value="month">Month</TabsTrigger>
                </TabsList>
                <TabsContent value="day">Day</TabsContent>
                <TabsContent value="week">Week</TabsContent>
                <TabsContent value="month">Month</TabsContent>
              </Tabs>
            </>
          ),
        },
        {
          title: 'Counts',
          stack: true,
          render: () => (
            <Tabs defaultValue="open">
              <TabsList aria-label="Requests">
                <TabsTrigger value="open" count={6} countTone="accent">
                  Open
                </TabsTrigger>
                <TabsTrigger value="waiting" count={3} countTone="warning">
                  Waiting
                </TabsTrigger>
                <TabsTrigger value="closed" count={15}>
                  Closed
                </TabsTrigger>
              </TabsList>
              <TabsContent value="open">Open requests</TabsContent>
              <TabsContent value="waiting">Waiting requests</TabsContent>
              <TabsContent value="closed">Closed requests</TabsContent>
            </Tabs>
          ),
        },
        {
          title: 'Link tabs',
          stack: true,
          render: () => (
            <TabsNav aria-label="Project sections">
              <TabsLink href="#/components/tabs" active>
                Overview
              </TabsLink>
              <TabsLink href="#/components/tabs" count={4}>
                Issues
              </TabsLink>
              <TabsLink href="#/components/tabs">Settings</TabsLink>
            </TabsNav>
          ),
        },
        {
          title: 'Filter tabs',
          stack: true,
          render: () => <FilterDemo />,
        },
        {
          title: 'Scrollable',
          stack: true,
          render: () => (
            <div style={{ width: '100%', maxWidth: 320 }}>
              <Tabs defaultValue="Overview">
                <TabsList aria-label="Workspace sections" scrollable>
                  {SECTIONS.map((s) => (
                    <TabsTrigger key={s} value={s}>
                      {s}
                    </TabsTrigger>
                  ))}
                </TabsList>
                {SECTIONS.map((s) => (
                  <TabsContent key={s} value={s}>
                    {s}
                  </TabsContent>
                ))}
              </Tabs>
            </div>
          ),
        },
        {
          title: 'URL binding',
          stack: true,
          render: () => (
            <p>
              <code>useTabsUrlBinding</code> takes the value your router read and a setter, and
              returns <code>value</code> / <code>onValueChange</code> for Tabs or TabsFilter.
            </p>
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Name the tab list with `aria-label`.',
        'Keep one TabsContent per TabsTrigger, with matching `value`s.',
        'Use `manual` activation when showing a panel is expensive (for example it fetches data).',
      ],
      dont: [
        'Do not use tabs for sequential steps or for primary site navigation.',
        'Do not nest tab sets inside a tab panel.',
        'Do not put essential actions only inside a non-default tab.',
        'Do not use Tabs (role tab) for links between pages: use TabsNav so the current page is announced and the URL works.',
        'Do not hand-roll `role="tab"` buttons for filters: TabsFilter gives the keyboard model for free.',
      ],
    },
    related: {
      items: [
        {
          id: 'segmented-control',
          reason:
            'SegmentedControl / ToggleGroup pick a value with radio or pressed semantics; use them for a compact view or option switch, and TabsFilter when the choice filters a list shown as tabs.',
        },
        {
          id: 'filter-chips',
          reason: 'Several independent filters at once, rather than one exclusive view.',
        },
        { id: 'page-header', reason: 'Has a `tabs` slot for page-level section tabs.' },
        {
          id: 'breadcrumb',
          reason: 'Shows location across pages rather than sections of one page.',
        },
      ],
    },
    dependencies: {
      packages: ['@radix-ui/react-slot', '@radix-ui/react-tabs', 'react'],
      components: [],
    },
    testing: {
      unit: [
        'Tab focuses the active tab; ArrowRight moves focus and selects the next tab.',
        'The selected tab has `aria-selected="true"` and its panel is shown.',
        'No axe violations in jsdom.',
        'Renders on the server.',
        'Counts are part of the tab name and carry their tone; `scrollable` is exposed; `asChild` still works without a count.',
        'TabsNav/TabsLink: navigation landmark, `aria-current="page"` on the active link, router link via `asChild`, Tab moves between links, no tab roles; SSR.',
        'TabsFilter uncontrolled: roving tab stop, arrows skip disabled and wrap, Home/End, selection follows focus, no aria-controls without a panel.',
        'TabsFilter controlled with a shared panel and manual activation: panel labelled by the selected tab, Enter selects; one tab stop for an unknown value; SSR.',
        'useTabsUrlBinding: unknown and missing values fall back to the default, the default writes null unless `clearDefault` is false, unknown values are ignored, and it drives Tabs from injected URL state.',
      ],
      axe: true,
      ssr: true,
      browser: ['Visual baselines: /__gallery', 'Cross-browser axe: /components/tabs'],
    },
  },
} satisfies ComponentDocs;
