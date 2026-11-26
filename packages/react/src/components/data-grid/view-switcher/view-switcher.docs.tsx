import { useState } from 'react';
import { ViewSwitcher, type ViewSwitcherBuiltIn } from '@rookcom/monarch-react/data';
import type { ComponentDocs } from '../../../internal/component-docs.js';
// The scenario of the primary example, shared with iOS and Android (CROSS-PLATFORM.md section 8).
import fixture from './view-switcher.fixture.json';

function Controlled() {
  const [view, setView] = useState<ViewSwitcherBuiltIn>(
    fixture.data.selected as ViewSwitcherBuiltIn,
  );
  return (
    <div style={{ display: 'grid', gap: 8 }}>
      <ViewSwitcher value={view} onValueChange={setView} />
      <p style={{ margin: 0 }}>Showing the {view} view.</p>
    </div>
  );
}

export default {
  id: 'view-switcher',
  title: 'ViewSwitcher',
  summary: 'Table, board, calendar and gallery views as one segmented control.',
  sections: {
    purpose: {
      summary:
        'Collections that can be shown several ways (a table, a board, a calendar, a gallery) need one consistent switch with recognisable icons. ViewSwitcher is a SegmentedControl with built-in labels and icons for the common views. Needed by: Stride (issues shown as a list or a board on the project page, `app/(workspace)/w/[slug]/projects/[id]/page.tsx`).',
      useWhen: [
        'The same records can be shown in 2–5 layouts.',
        'In a TableToolbar (`view` prop).',
      ],
      avoidWhen: [
        'Switching between saved filters or queues: use SavedViews or a menu (Tickets’ inbox view menu).',
        'Each option shows different content: use Tabs.',
      ],
    },
    designPrinciples: {
      items: [
        'Radio group semantics: one Tab stop, arrows move and select.',
        'Icons plus labels; labels collapse to icons on narrow screens but remain the accessible names.',
        'Built-in names keep wording and icons the same in every product.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        '`viewSwitcherOptions(views, messages)` returns the SegmentedControl options it renders.',
        'Generic over the view value: `ViewSwitcher<"list" | "map">` for your own values.',
        'The ref goes to the radiogroup `<div>`.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        'Built-in views: `table`, `board`, `calendar`, `gallery`, `list`, `timeline`. Rename them with `messages`.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'labels',
          values: ['auto', 'show', 'hide'],
          description: '`auto` hides text below 40rem; `hide` is icons only with tooltips.',
        },
        { prop: 'size', values: ['sm', 'md', 'lg'], description: 'Control height. Default `md`.' },
      ],
    },
    states: {
      items: [
        { name: 'Selected', attribute: 'aria-checked="true"', description: 'The current view.' },
        { name: 'Disabled', attribute: 'disabled', description: 'A view or the whole control.' },
      ],
    },
    interaction: {
      items: ['Clicking a view selects it and calls `onValueChange`.'],
    },
    keyboard: {
      items: [
        { keys: 'Tab', action: 'Focuses the selected view.' },
        { keys: 'ArrowLeft / ArrowRight / ArrowUp / ArrowDown', action: 'Moves and selects.' },
        { keys: 'Home / End', action: 'First or last view.' },
      ],
    },
    accessibility: {
      role: 'radiogroup with radios',
      name: '“View” by default (`aria-label` or `aria-labelledby` override it).',
      items: ['Icons are decorative; each radio is named by its label even when it is hidden.'],
    },
    responsive: {
      items: [
        'Below 40rem labels collapse to icons (with `labels="auto"`), so four views fit at 390 px.',
        'Each view is at least 44 px wide and tall on touch screens.',
      ],
    },
    theme: {
      items: [
        'Uses SegmentedControl’s track and selected styles.',
        'Forced colours: icons use CanvasText; the selected view gets a Highlight outline.',
      ],
    },
    examples: {
      items: [
        { title: 'Default views', render: () => <Controlled /> },
        {
          title: 'Icons only, custom set',
          render: () => (
            <ViewSwitcher labels="hide" defaultValue="list" views={['list', 'board', 'timeline']} />
          ),
        },
      ],
    },
    doDont: {
      do: ['Keep the chosen view in the URL or saved view so it survives reloads.'],
      dont: [
        'Do not offer more than five views.',
        'Do not use it to filter records; it only changes the layout.',
      ],
    },
    related: {
      items: [
        { id: 'segmented-control', reason: 'The control it is built on.' },
        { id: 'table-toolbar', reason: 'Hosts it next to search and columns.' },
        { id: 'kanban-board', reason: 'The board view.' },
        { id: 'calendar-month-view', reason: 'The calendar view.' },
      ],
    },
    dependencies: { packages: ['react'], components: ['segmented-control'] },
    testing: {
      unit: [
        'Default views as a labelled radio group with icons.',
        'Arrow keys, Home and End select; one tab stop.',
        'Custom views, counts, hidden labels and messages; option builder.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
