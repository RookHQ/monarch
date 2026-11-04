import { useState } from 'react';
import { Button, FilterBar, FilterChip, SearchInput } from '@rookcom/monarch-react';
import { FilterMenu } from '@rookcom/monarch-react/data';
import type { ComponentDocs } from '../../../internal/component-docs.js';

const INITIAL = [
  { name: 'Status', value: 'Open' },
  { name: 'Assignee', value: 'Me' },
  { name: 'Priority', value: 'High' },
];

function ActiveFilters() {
  const [filters, setFilters] = useState(INITIAL);
  return (
    <FilterBar {...(filters.length ? { onClearAll: () => setFilters([]) } : {})}>
      {filters.map((f) => (
        <FilterChip
          key={f.name}
          name={f.name}
          value={f.value}
          onRemove={() => setFilters((all) => all.filter((x) => x.name !== f.name))}
        />
      ))}
    </FilterBar>
  );
}

const STATUSES = ['Open', 'Pending', 'Closed'];

function FullBar() {
  const [query, setQuery] = useState('');
  const [status, setStatus] = useState<string | null>('Open');
  const [owner, setOwner] = useState<string | null>(null);
  const [statusOpen, setStatusOpen] = useState(false);
  const active = [status, owner].filter(Boolean).length;
  return (
    <FilterBar
      landmark
      label="Filter tickets"
      search={
        <SearchInput
          aria-label="Search tickets"
          placeholder="Search tickets"
          size="sm"
          value={query}
          onValueChange={setQuery}
          debounce={250}
        />
      }
      activeCount={active}
      resultCount={`${active ? 18 : 64} results`}
      end={
        <Button size="sm" variant="ghost">
          Newest first
        </Button>
      }
      add={
        <FilterMenu
          options={[
            { value: 'status', label: 'Status', disabled: status !== null },
            { value: 'owner', label: 'Owner', disabled: owner !== null },
          ]}
          onSelect={(v) => {
            if (v === 'status') {
              setStatus('Open');
              setStatusOpen(true);
            } else setOwner('Me');
          }}
        />
      }
      {...(active
        ? {
            onClearAll: () => {
              setStatus(null);
              setOwner(null);
            },
          }
        : {})}
    >
      {status ? (
        <FilterChip
          name="Status"
          value={status}
          open={statusOpen}
          onOpenChange={setStatusOpen}
          onRemove={() => setStatus(null)}
          editor={
            <div style={{ display: 'grid', gap: 4 }}>
              {STATUSES.map((s) => (
                <Button
                  key={s}
                  size="xs"
                  variant={s === status ? 'primary' : 'ghost'}
                  onClick={() => {
                    setStatus(s);
                    setStatusOpen(false);
                  }}
                >
                  {s}
                </Button>
              ))}
            </div>
          }
        />
      ) : null}
      {owner ? <FilterChip name="Owner" value={owner} onRemove={() => setOwner(null)} /> : null}
    </FilterBar>
  );
}

function QuickFilters() {
  const [on, setOn] = useState<Record<string, boolean>>({ mine: true });
  const chips = [
    { id: 'mine', name: 'Assigned to me', count: 12 },
    { id: 'unread', name: 'Unread', count: 5 },
    { id: 'vip', name: 'VIP customers', count: 3 },
    { id: 'sla', name: 'SLA at risk', count: 2 },
  ];
  return (
    <FilterBar label="Quick filters" scrollable>
      {chips.map((c) => (
        <FilterChip
          key={c.id}
          name={c.name}
          count={c.count}
          pressed={!!on[c.id]}
          onPressedChange={(p) => setOn((all) => ({ ...all, [c.id]: p }))}
        />
      ))}
    </FilterBar>
  );
}

export default {
  id: 'filter-bar',
  title: 'FilterBar',
  summary:
    'Filter row: search, removable, editable or toggle FilterChips, an "Add filter" menu, "Clear all", a result count and trailing controls.',
  sections: {
    purpose: {
      summary:
        'Shows which filters are applied to a list and lets people change them: search, add a filter, edit or remove one, toggle quick filters, or clear everything, with the result count beside it. FilterChip is one filter; FilterMenu is the "Add filter" menu.',
      useWhen: [
        'A list or table has filters applied and the user needs to see, change and remove them.',
        'Quick one-tap filters ("Assigned to me", "Unread") with counts: FilterChip in toggle mode.',
      ],
      avoidWhen: [
        'Switching between mutually exclusive views: use Tabs or SegmentedControl.',
        'Tags on a record: use Badge.',
      ],
    },
    designPrinciples: {
      items: [
        'Chips show field and value ("Status: Open") so filters read without context.',
        'The app owns filter state and the filter definitions; FilterBar renders it and reports changes. Pickers (selects, date ranges, people) go in a chip’s `editor`.',
        '"Clear all" appears only when the app passes `onClearAll`.',
        'Toggle chips use `aria-pressed`, never colour alone.',
        'URL state is router-agnostic: `readFilterParams` / `writeFilterParams` convert to and from search params.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'FilterBar and FilterChip are exported from `@rookcom/monarch-react` (unchanged) and `@rookcom/monarch-react/data`; FilterMenu, `readFilterParams` and `writeFilterParams` from `@rookcom/monarch-react/data`.',
        'FilterChip has three modes: removable/read-only (default), editable (`editor` opens a popover from the name and value), and toggle (`pressed`, `defaultPressed` or `onPressedChange` make the whole chip a pressed button).',
        'They render plain elements and pass native attributes through; no refs are forwarded.',
        '`messages` translates "2 filters", "Filters (n)", the sheet title and "Done".',
      ],
    },
    typedProperties: { source: 'typescript' },
    variants: {
      items: [
        {
          prop: 'FilterChip mode',
          values: ['read-only', 'removable (onRemove)', 'editable (editor)', 'toggle (pressed)'],
          description:
            'Toggle chips are `data-variant="toggle"` with `data-pressed`; editable chips are `data-editable`.',
        },
        {
          prop: 'count (FilterChip)',
          values: ['number'],
          description: 'A count after the label, e.g. matching items.',
        },
        {
          prop: 'landmark',
          values: ['true', 'false'],
          description: '`role="search"` instead of `role="group"`.',
        },
        {
          prop: 'scrollable',
          values: ['true', 'false'],
          description:
            'Chips stay on one line that scrolls sideways; the pressed chip scrolls into view.',
        },
        {
          prop: 'collapseBelow',
          values: ['number (px)'],
          description:
            'Below that width of the bar, chips, the add control and "Clear all" move into a bottom Sheet opened by "Filters (n)".',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Pressed',
          attribute: 'aria-pressed, data-pressed',
          description: 'Toggle chip is on: accent border and fill.',
        },
        {
          name: 'Editing',
          attribute: 'aria-expanded',
          description: 'The chip’s popover is open (controlled with `open`/`onOpenChange`).',
        },
        {
          name: 'Collapsed',
          attribute: 'data-collapsed',
          description: 'Narrow bar: filters are in the sheet.',
        },
        {
          name: 'Hover and focus',
          description:
            'Chip buttons and "Clear all" have hover backgrounds and a visible focus ring.',
        },
      ],
    },
    interaction: {
      items: [
        'The remove button calls `onRemove`; "Clear all" calls `onClearAll`.',
        'Pressing an editable chip opens its `editor` in a popover; Escape closes it and returns focus to the chip.',
        'Pressing a toggle chip flips it and calls `onPressedChange`.',
        'FilterMenu lists the fields; choosing one calls `onSelect` (open the new chip’s editor with `open`).',
        '`resultCount` changes are announced politely; debounce search with SearchInput’s `debounce`.',
        'Order: search, active count, chips, add control, "Clear all", then result count and `end` at the trailing edge.',
      ],
    },
    keyboard: {
      items: [
        {
          keys: 'Tab',
          action: 'Moves through search, chip buttons, remove buttons, add and "Clear all".',
        },
        { keys: 'Enter / Space', action: 'Removes, toggles, opens a chip editor, or clears all.' },
        { keys: 'Escape', action: 'Closes a chip editor, the add menu or the filter sheet.' },
        { keys: 'Arrow keys', action: 'Move within the FilterMenu.' },
      ],
    },
    accessibility: {
      role: 'group, or search with `landmark`',
      name: '`label`, default "Active filters".',
      items: [
        'Each remove button is labelled "Remove <name> filter" (`removeLabel` overrides it).',
        'Toggle chips are buttons with `aria-pressed`; their count is part of the name.',
        'Chip editors are labelled popovers ("Status filter").',
        'The result count is a status region; the narrow-screen button states how many filters are active.',
        'Icons are decorative.',
      ],
    },
    responsive: {
      items: [
        'Chips wrap onto new lines by default; the search field shrinks to fit.',
        '`scrollable` keeps one swipeable line; `collapseBelow` moves filters into a bottom sheet on phones.',
      ],
    },
    theme: {
      items: [
        'Chip, pressed, count and focus colours use Monarch semantic tokens, so every theme and contrast mode applies.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Removable filters',
          render: () => <ActiveFilters />,
        },
        {
          title: 'Search, add filter, editable chip, result count',
          stack: true,
          render: () => <FullBar />,
        },
        {
          title: 'Quick filters (toggle chips with counts)',
          render: () => <QuickFilters />,
        },
      ],
    },
    doDont: {
      do: [
        'Move focus somewhere sensible after a removal (the list or the next chip).',
        'Hide "Clear all" when nothing is applied.',
        'Keep filters in the URL with `writeFilterParams` so views can be shared and reloaded.',
      ],
      dont: [
        'Do not use chips for values the user cannot remove without explaining why.',
        'Do not use toggle chips for mutually exclusive options; use SegmentedControl or Tabs.',
      ],
    },
    related: {
      items: [
        {
          id: 'data-table',
          reason: 'The table the filters narrow; pass `filtered` for its empty state.',
        },
        { id: 'saved-views', reason: 'Saves a filter combination as a named view.' },
        { id: 'search-input', reason: 'The debounced search field for the `search` slot.' },
        { id: 'toolbar', reason: 'Place the FilterBar in its `filters` slot.' },
        { id: 'filter-chips', reason: 'FilterChip registry entry, documented here.' },
        { id: 'segmented-control', reason: 'Mutually exclusive choices.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: ['button', 'dropdown-menu', 'popover', 'sheet'],
    },
    testing: {
      unit: [
        'Remove buttons are labelled per filter and call onRemove; "Clear all" calls onClearAll; default group name.',
        'Toggle chips expose aria-pressed and counts, controlled and uncontrolled.',
        'Editable chip opens a labelled popover and Escape returns focus.',
        'Search, active count, result status region, trailing slot and landmark role.',
        'collapseBelow moves filters into a sheet with Clear all and Done; focus returns to the trigger.',
        'FilterMenu adds a field by keyboard; readFilterParams / writeFilterParams round-trip.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
