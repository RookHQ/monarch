import { useState } from 'react';
import { Button } from '@rookcom/monarch-react';
import { DashboardGrid, Widget, type DashboardGridItem } from '@rookcom/monarch-react/patterns';
import type { ComponentDocs } from '../../../internal/component-docs.js';

const demoItems: DashboardGridItem[] = [
  {
    id: 'figures',
    label: 'Key figures',
    span: 'full',
    content: (
      <Widget title="Key figures">
        <p style={{ margin: 0 }}>128 open · 94% within target</p>
      </Widget>
    ),
  },
  {
    id: 'load',
    label: 'Team load',
    span: 2,
    content: (
      <Widget title="Team load">
        <p style={{ margin: 0 }}>12 open across 4 people.</p>
      </Widget>
    ),
  },
  {
    id: 'activity',
    label: 'Recent activity',
    content: (
      <Widget title="Recent activity">
        <p style={{ margin: 0 }}>3 updates today.</p>
      </Widget>
    ),
  },
];

function ArrangeDemo() {
  const [editing, setEditing] = useState(false);
  const [layout, setLayout] = useState<string[]>(['figures', 'load', 'activity']);
  return (
    <div style={{ display: 'grid', gap: 'var(--mn-space-3)' }}>
      <div>
        <Button
          size="sm"
          variant={editing ? 'primary' : 'secondary'}
          onClick={() => setEditing(!editing)}
        >
          {editing ? 'Done arranging' : 'Arrange'}
        </Button>
      </div>
      <DashboardGrid
        items={demoItems}
        layout={layout}
        onLayoutChange={setLayout}
        editing={editing}
      />
      <p style={{ margin: 0, fontSize: 'var(--mn-text-xs)' }}>Saved layout: {layout.join(', ')}</p>
    </div>
  );
}

export default {
  id: 'dashboard-grid',
  title: 'DashboardGrid',
  summary: 'Responsive grid of dashboard widgets that people can rearrange by drag or keyboard.',
  sections: {
    purpose: {
      summary:
        'Lays out a dashboard’s widgets in one column on phones, two at medium widths and up to four on wide screens, with widgets spanning several columns. Optionally lets each person rearrange their dashboard by dragging or with the keyboard, and hands the new order to the app to store. Needed by: Pulse, Tickets, Dots, Recruit, GTM, Atlas, Intellect, Stride, People.',
      useWhen: [
        'A home or dashboard page shows several independent widgets.',
        'People should be able to put the widgets they use most first.',
      ],
      avoidWhen: [
        'A row of figures only: use KPIRow.',
        'A form or settings page: use the page layout patterns.',
        'Arbitrary resizing and placement: this grid keeps an order, not coordinates.',
      ],
    },
    designPrinciples: {
      items: [
        'Order, not coordinates: a layout is a list of ids, easy to store per person and robust when widgets are added or removed.',
        'Responds to its own width (container queries), so it works inside a split view as well as full width.',
        'Every drag has a keyboard and a single-click equivalent, and every move is announced.',
        'The app owns persistence: the grid only reports the new order.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'Without `items` the grid lays out `children` as given; a child Widget’s `span` still applies.',
        '`resolveDashboardLayout(items, layout)` reconciles a stored layout with the current items.',
        '`DASHBOARD_GRID_MESSAGES` holds the English defaults; the instructions and announcements mirror ReorderableList.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        'Migrating from the app copy: no app has a reorderable dashboard yet. The hand-written grids (`grid gap-4 lg:grid-cols-5` with `lg:col-span-3` in People dashboards, `lg:grid-cols-[7fr_5fr]` in Dots, `lg:grid-cols-5` in GTM, the two-column Tickets and Stride homes) become `<DashboardGrid columns={…}>` with each widget’s `span`. To let people rearrange, pass `items`, `layout` (read from the app’s user preferences) and `onLayoutChange` (write them back).',
      ],
    },
    variants: {
      items: [
        {
          prop: 'columns',
          values: ['1', '2', '3', '4'],
          description:
            'Columns from 64rem of grid width. Medium widths use two, narrow one. Default 3.',
        },
        {
          prop: 'editing',
          values: ['false', 'true'],
          description:
            'Arrange mode: a handle and Move earlier / Move later buttons on each widget.',
        },
      ],
    },
    states: {
      items: [
        { name: 'Static', description: 'Widgets in the saved order.' },
        {
          name: 'Arranging',
          attribute: 'data-editing',
          description: 'Each widget is outlined and shows its arrange bar.',
        },
        {
          name: 'Moving',
          attribute: 'data-active on the cell, data-dragging on the grid',
          description:
            'A widget is picked up by keyboard or pointer; the order shown is a preview until it is dropped.',
        },
      ],
    },
    interaction: {
      items: [
        'Drag a widget by its handle; it moves as the pointer passes over another widget and the new order is reported on release.',
        'A press on the handle without moving is not a move.',
        'Move earlier / Move later move one place at once and report it; at either end focus moves to the handle.',
        '`onLayoutChange` is called once per finished move, never during a drag.',
      ],
    },
    keyboard: {
      items: [
        { keys: 'Space / Enter (on a handle)', action: 'Picks the widget up, or drops it.' },
        { keys: 'Arrow Left / Arrow Up', action: 'Moves the picked-up widget one place earlier.' },
        { keys: 'Arrow Right / Arrow Down', action: 'Moves it one place later.' },
        { keys: 'Home / End', action: 'Moves it to the first or last place.' },
        { keys: 'Escape', action: 'Cancels the move and restores the order.' },
      ],
      notes: ['Moving focus to another control while a widget is picked up drops it where it is.'],
    },
    accessibility: {
      role: 'list (with `items`); group when `aria-label` is given to a children grid',
      name: '`aria-label`, default "Dashboard"',
      items: [
        'Each widget is a list item, so its position ("2 of 5") is announced.',
        'Handles are buttons named "Move {label}", described by the instructions, with `aria-pressed` while picked up.',
        'Pick-up, each step, drop and cancel are announced in a polite live region.',
        'Move earlier / Move later give a single-click alternative to dragging (WCAG 2.5.7).',
      ],
    },
    responsive: {
      items: [
        'One column below 40rem of grid width, two up to 64rem, `columns` above; spans clamp to the columns available.',
        'Arrange controls are at least 44px on coarse pointers, and the handle has `touch-action: none` so dragging does not scroll.',
      ],
    },
    theme: {
      items: [
        'Arrange outlines use `--mn-color-border-strong`; the moving widget `--mn-color-accent-solid` on `--mn-color-accent-subtle`.',
        'In forced colours outlines are CanvasText and the moving widget Highlight. Arrange bars are hidden when printing.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Rearrangeable dashboard with a stored layout',
          stack: true,
          render: () => <ArrangeDemo />,
        },
        {
          title: 'Static grid from children',
          stack: true,
          render: () => (
            <DashboardGrid columns={3} aria-label="Overview">
              <Widget title="Pipeline" span={2}>
                <p style={{ margin: 0 }}>18 open deals.</p>
              </Widget>
              <Widget title="Renewals">
                <p style={{ margin: 0 }}>4 due this month.</p>
              </Widget>
            </DashboardGrid>
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Store the layout per person and pass it back as `layout`.',
        'Give every item a short, unique `label`: it names the handle and the announcements.',
        'Offer an "Arrange" toggle rather than leaving handles on all the time.',
      ],
      dont: [
        'Do not put interactive content in a handle; it is a single button.',
        'Do not rely on drag alone; keep the move buttons for people who cannot drag.',
        'Do not reorder the DOM differently from the visual order (no dense packing).',
      ],
    },
    related: {
      items: [
        { id: 'widget', reason: 'The frame each grid item usually is.' },
        { id: 'reorderable-list', reason: 'Reordering a vertical list rather than a grid.' },
        { id: 'kpi-row', reason: 'A row of figures, often the first full-width item.' },
      ],
    },
    dependencies: { packages: ['react'], components: ['button', 'reorderable-list'] },
    testing: {
      unit: [
        'Children grid with spans and a named group; props and ref forwarded.',
        'Items in layout order with clamped spans; stored layouts reconciled.',
        'Arrange mode handles described by the instructions and end buttons disabled.',
        'Keyboard pick-up, move, drop and Escape cancel with announcements and focus kept on the handle.',
        'Single-click move buttons, uncontrolled defaultLayout, pointer drag and a press without movement.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
