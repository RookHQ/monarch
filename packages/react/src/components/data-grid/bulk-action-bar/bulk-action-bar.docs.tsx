import { useState } from 'react';
import { BulkActionBar, DataTable, type DataTableColumn } from '@rookcom/monarch-react/data';
import type { ComponentDocs } from '../../../internal/component-docs.js';

interface Member {
  id: string;
  name: string;
  team: string;
  role: string;
}

const MEMBERS: Member[] = [
  { id: 'm1', name: 'Ana Silva', team: 'Support', role: 'Agent' },
  { id: 'm2', name: 'Ben Okafor', team: 'Support', role: 'Lead' },
  { id: 'm3', name: 'Chen Wei', team: 'Billing', role: 'Agent' },
  { id: 'm4', name: 'Dev Patel', team: 'Sales', role: 'Manager' },
];

const COLUMNS: DataTableColumn<Member>[] = [
  { id: 'name', header: 'Name' },
  { id: 'team', header: 'Team' },
  { id: 'role', header: 'Role' },
];

function WithTable() {
  const [selected, setSelected] = useState<string[]>(['m1', 'm3']);
  const [log, setLog] = useState('');
  const act = (what: string) => () => setLog(`${what}: ${selected.length} members`);
  return (
    <div style={{ display: 'grid', gap: 8 }}>
      <BulkActionBar
        count={selected.length}
        totalCount={MEMBERS.length}
        onClear={() => setSelected([])}
        actions={[
          { id: 'team', label: 'Move to team', onSelect: act('Moved') },
          { id: 'role', label: 'Change role', onSelect: act('Role changed') },
          { id: 'export', label: 'Export', onSelect: act('Exported') },
          { id: 'remove', label: 'Remove', tone: 'danger', onSelect: act('Removed') },
        ]}
      />
      <DataTable
        columns={COLUMNS}
        rows={MEMBERS}
        getRowId={(r) => r.id}
        getRowLabel={(r) => r.name}
        caption="Members"
        selectable
        selectedRowIds={selected}
        onSelectedRowIdsChange={setSelected}
      />
      <p style={{ margin: 0, fontSize: 13 }}>{log || 'Pick an action.'}</p>
    </div>
  );
}

function Busy() {
  return (
    <BulkActionBar
      count={12}
      onClear={() => {}}
      busy
      actions={[
        { id: 'archive', label: 'Archive', onSelect: () => {} },
        { id: 'tag', label: 'Add tag', onSelect: () => {} },
      ]}
    />
  );
}

export default {
  id: 'bulk-action-bar',
  title: 'BulkActionBar',
  summary:
    'Toolbar shown while items are selected: count, actions with an overflow menu, clear selection and a polite announcement.',
  sections: {
    purpose: {
      summary:
        'Gives people the actions that apply to everything they selected in a table or list, says how many items are selected, and clears the selection. It appears with the first selection and disappears when the selection is empty.',
      useWhen: [
        'A DataTable or list supports multi-select and several actions apply to the selection.',
        'The selection count should be visible and announced.',
      ],
      avoidWhen: [
        'One action per row: use DataTable `rowActions`.',
        'Page-level actions unrelated to a selection: use PageHeader or Toolbar.',
      ],
    },
    designPrinciples: {
      items: [
        'Selection-aware: hidden at 0, the count is always the first thing read.',
        'The app owns the selection and runs the actions; the bar renders and reports. Confirmations, previews and progress for long jobs stay in the app (ConfirmDialog, Toast).',
        'A promise returned by an action makes that button busy and blocks the others, so an action cannot run twice.',
        'One tab stop: arrow keys move between its controls (WAI-ARIA toolbar).',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'Imported from `@rookcom/monarch-react/data`. The ref is the outer `div`, which also receives focus after "Clear selection" unless `returnFocusRef` is given.',
        '`BulkAction`: `id`, `label`, `icon`, `onSelect` (may return a promise), `tone`, `disabled`.',
        '`messages` translates the count ("3 of 120 selected"), "Selection cleared", "Clear selection", "More" and the toolbar name.',
      ],
    },
    typedProperties: { source: 'typescript' },
    variants: {
      items: [
        {
          prop: 'placement',
          values: ['inline', 'sticky'],
          description:
            '`sticky` keeps the bar at the bottom of the scroll area with a stronger shadow (`data-placement`).',
        },
        {
          prop: 'maxVisibleActions',
          values: ['number'],
          description: 'Actions after this many (default 3) go into the "More" menu.',
        },
        {
          prop: 'tone (action)',
          values: ['default', 'danger'],
          description: 'Danger actions use the subtle danger button and danger menu item.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Closed',
          attribute: 'data-state="closed"',
          description: 'Nothing selected: only the live region renders.',
        },
        {
          name: 'Open',
          attribute: 'data-state="open"',
          description: 'Something is selected: the toolbar is shown.',
        },
        {
          name: 'Busy',
          attribute: 'aria-busy',
          description:
            '`busy`, or an action whose promise has not settled: that button shows a spinner and the rest are disabled.',
        },
      ],
    },
    interaction: {
      items: [
        'Pressing an action calls its `onSelect`; overflow actions are in a "More" menu.',
        '"Clear selection" calls `onClear` and moves focus to `returnFocusRef` (or the bar container) so it is never lost.',
        'Count changes are announced politely ("2 of 4 selected", then "Selection cleared").',
        'Extra controls (property pickers) can be passed as children.',
      ],
    },
    keyboard: {
      items: [
        { keys: 'Tab', action: 'Moves into the toolbar (one stop) and out again.' },
        {
          keys: 'Arrow Left / Right',
          action: 'Moves between the toolbar’s controls, wrapping (RTL aware).',
        },
        { keys: 'Home / End', action: 'First / last control.' },
        { keys: 'Enter / Space', action: 'Runs the focused action or opens "More".' },
      ],
    },
    accessibility: {
      role: 'toolbar',
      name: '"Bulk actions" (`messages.label`).',
      items: [
        'A persistent polite status region announces count changes, so the announcement works even when the bar first appears.',
        'The visible count is plain text inside the toolbar; icons are decorative.',
        'Busy actions keep focus (aria-disabled via Button `loading`).',
      ],
    },
    responsive: {
      items: [
        'Wraps onto several lines; below 480px the actions take their own full-width row under the count and Clear.',
      ],
    },
    theme: {
      items: [
        'Raised surface, border and shadow tokens, so it reads in every theme and contrast mode.',
        'The entrance animation is removed with `prefers-reduced-motion: reduce`.',
      ],
    },
    examples: {
      items: [
        {
          title: 'With DataTable selection',
          stack: true,
          render: () => <WithTable />,
        },
        {
          title: 'Busy',
          stack: true,
          render: () => <Busy />,
        },
      ],
    },
    doDont: {
      do: [
        'Confirm destructive bulk actions and report partial failures (Toast).',
        'Return a promise from long actions so the bar blocks double runs.',
        'Pass `totalCount` when "select all matching" is possible.',
      ],
      dont: [
        'Do not show the bar without a way to clear the selection.',
        'Do not put more than a few actions in view; let the rest overflow.',
      ],
    },
    related: {
      items: [
        { id: 'data-table', reason: 'Supplies the selection.' },
        { id: 'toolbar', reason: 'Actions that do not depend on a selection.' },
        { id: 'confirm-dialog', reason: 'Confirm destructive bulk actions.' },
        { id: 'toast', reason: 'Report results and offer undo.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: ['button', 'dropdown-menu'],
    },
    testing: {
      unit: [
        'Hidden until DataTable rows are selected; announces "1 of 3 selected", then the new count.',
        'Clear empties the selection, announces it and keeps focus (container or returnFocusRef).',
        'Runs actions; overflow menu; a pending promise blocks other actions and sets aria-busy.',
        'Roving tabindex: one tab stop, arrows wrap, Home/End.',
        'Translated messages and sticky placement; server rendering open and closed.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
