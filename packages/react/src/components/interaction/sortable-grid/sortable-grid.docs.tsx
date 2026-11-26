import { useState } from 'react';
import { Badge } from '@rookcom/monarch-react';
import { SortableGrid } from '@rookcom/monarch-react/data';
import type { ComponentDocs } from '../../../internal/component-docs.js';

interface Shortcut {
  id: string;
  name: string;
  detail: string;
  pinned?: boolean;
}

const SHORTCUTS: Shortcut[] = [
  { id: 'inbox', name: 'Inbox', detail: '12 unread' },
  { id: 'calendar', name: 'Calendar', detail: '3 events today' },
  { id: 'reports', name: 'Reports', detail: 'Updated hourly' },
  { id: 'files', name: 'Files', detail: '48 items' },
  { id: 'people', name: 'People', detail: '9 teams' },
  { id: 'settings', name: 'Settings', detail: 'Workspace' },
];

function TilesDemo() {
  const [tiles, setTiles] = useState(SHORTCUTS);
  return (
    <div style={{ width: '100%' }}>
      <SortableGrid
        aria-label="Shortcuts"
        items={tiles}
        onReorder={setTiles}
        minItemWidth="10rem"
        getKey={(t) => t.id}
        getLabel={(t) => t.name}
        renderItem={(t) => (
          <span style={{ display: 'grid', gap: 2 }}>
            <strong>{t.name}</strong>
            <span style={{ color: 'var(--mn-color-text-muted)' }}>{t.detail}</span>
          </span>
        )}
      />
    </div>
  );
}

function ColumnsDemo() {
  const [tiles, setTiles] = useState(
    SHORTCUTS.map((t) => (t.id === 'inbox' ? { ...t, pinned: true } : t)),
  );
  return (
    <div style={{ width: '100%', maxWidth: 480 }}>
      <SortableGrid
        aria-label="Pinned shortcuts"
        items={tiles}
        onReorder={setTiles}
        columns={2}
        isItemDisabled={(t) => t.pinned === true}
        getKey={(t) => t.id}
        getLabel={(t) => t.name}
        renderItem={(t, { disabled }) => (
          <span style={{ display: 'flex', gap: 8, alignItems: 'center', flexWrap: 'wrap' }}>
            {t.name}
            {disabled ? <Badge>Pinned</Badge> : null}
          </span>
        )}
      />
    </div>
  );
}

export default {
  id: 'sortable-grid',
  title: 'SortableGrid',
  summary:
    'A grid of tiles reordered by dragging a handle or with the arrow keys in two dimensions, with every step announced.',
  sections: {
    purpose: {
      summary:
        'Lets people arrange tiles (shortcuts, links, widgets, photos) in their own order, by pointer or keyboard, with the same result. Rook Links (`src/views/Grid.tsx`) uses @dnd-kit with `rectSortingStrategy` for its tile grid; SortableGrid replaces it without a dependency. It extends ReorderableList’s model to two dimensions. Needed by: Rook Links.',
      useWhen: [
        'The order of tiles in a grid is user data the app saves.',
        'A launcher, gallery or dashboard of equal-sized tiles.',
      ],
      avoidWhen: [
        'A single column: use ReorderableList.',
        'Moving cards between columns (a board): use KanbanBoard.',
        'Widgets of different sizes the user resizes: use DashboardGrid.',
      ],
    },
    designPrinciples: {
      items: [
        'Same model as ReorderableList: drag only from the handle, nothing is saved until the drop, one `onReorder` per finished move.',
        'Two-dimensional keyboard: Left/Right move one place, Up/Down move a whole row, as on screen.',
        'Pointer drops into the slot whose centre is nearest, so it feels the same in any direction.',
        'Every step is announced with the position (“position 5 of 8”).',
        'Tiles glide to their place (FLIP) unless the user prefers reduced motion.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'Generic over the item type: `SortableGrid<T>` with `getKey`, `getLabel` and `renderItem(item, { index, active, disabled })`.',
        'Renders `<div class="mn-sortable-grid">` with a `<ul class="mn-sortable-grid__list">` (the ref and the rest of the props), one `<li class="mn-sortable-grid__item">` per tile with a DragHandle, the hidden instructions and an assertive LiveRegion.',
        'Use `moveItem(list, from, to)` from `@rookcom/monarch-react/data` to move items elsewhere.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        'Migrating from @dnd-kit: `DndContext` + `SortableContext items strategy={rectSortingStrategy}` + `useSortable` per tile becomes one `<SortableGrid items getKey getLabel renderItem onReorder>`; `onDragEnd` with `arrayMove` becomes `onReorder(items)`; sensors and `sortableKeyboardCoordinates` are built in.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'minItemWidth',
          values: ['12rem (default)', 'CSS length', 'number of px'],
          description: 'As many columns as fit tiles of this width.',
        },
        {
          prop: 'columns',
          values: ['number'],
          description: 'A fixed number of columns; also the row step for Up/Down.',
        },
        {
          prop: 'isItemDisabled / disabled',
          values: ['(item) => boolean', 'boolean'],
          description: 'Lock single tiles (others move past them) or the whole grid.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Picked up (keyboard)',
          attribute: 'data-active="keyboard" on the tile, aria-pressed="true" on the handle',
          description: 'Raised shadow and accent border.',
        },
        {
          name: 'Dragging (pointer)',
          attribute: 'data-active="pointer" on the tile, data-dragging="pointer" on the root',
          description: 'The tile follows the pointer; others make room.',
        },
        { name: 'Locked', attribute: 'data-disabled on the tile', description: 'Disabled handle.' },
        { name: 'Focus', description: 'Handles show the Monarch focus ring.' },
      ],
    },
    interaction: {
      items: [
        'Pointer: press the handle and move more than 4px; the grid reorders as the tile’s centre nears another slot; release to drop; pointercancel cancels.',
        'Keyboard moves are previewed and committed on drop; Escape, Tab away or focus leaving the grid cancels.',
        'Rows for Up/Down are counted from the layout, so they follow the wrapped grid at any width; `columns` fixes them.',
        'Controlled with `items` + `onReorder`, or uncontrolled with `defaultItems`.',
      ],
    },
    keyboard: {
      items: [
        { keys: 'Tab', action: 'Moves between handles.' },
        { keys: 'Space / Enter', action: 'Picks up the focused tile; pressed again, drops it.' },
        {
          keys: 'ArrowLeft / ArrowRight',
          action: 'Moves the picked-up tile one place (swapped in RTL).',
        },
        { keys: 'ArrowUp / ArrowDown', action: 'Moves it one row up or down.' },
        { keys: 'Home / End', action: 'Moves it to the first or last place.' },
        { keys: 'Escape', action: 'Cancels the move and restores the order.' },
      ],
    },
    accessibility: {
      role: 'list of tiles with button handles',
      name: 'Give the grid an `aria-label`. Each handle is “Reorder <label>” (`messages.handleLabel`).',
      items: [
        'One shared, visually hidden instruction describes every handle.',
        'Pick-up, moves, drop and cancel are announced in an assertive live region.',
        'Focus stays on the moved tile’s handle after each keyboard step.',
        'Checked with axe in jsdom.',
      ],
    },
    responsive: {
      items: [
        'Auto columns never exceed the container, so one column fits a 320px screen.',
        'Handles grow to 44px on coarse pointers (DragHandle).',
      ],
    },
    theme: {
      items: [
        'Tiles on `--mn-color-bg-surface` with `--mn-color-border-subtle`; the active tile uses `--mn-color-accent-border` and `--mn-shadow-md`.',
        'Transitions removed under reduced motion; forced-colours mode outlines the active tile in Highlight.',
      ],
    },
    examples: {
      items: [
        { title: 'Drag or keyboard in two dimensions', stack: true, render: () => <TilesDemo /> },
        { title: 'Two columns with a locked tile', stack: true, render: () => <ColumnsDemo /> },
      ],
    },
    doDont: {
      do: [
        'Give every tile a short, unique `getLabel`.',
        'Save once in `onReorder` and roll back if the save fails.',
      ],
      dont: [
        'Do not make the whole tile draggable; keep the handle.',
        'Do not use it for boards or for tiles of different sizes.',
      ],
    },
    related: {
      items: [
        { id: 'reorderable-list', reason: 'The single-column version.' },
        { id: 'drag-handle', reason: 'The handle each tile uses.' },
        { id: 'kanban-board', reason: 'Moving cards between columns.' },
        { id: 'grid', reason: 'A static grid of tiles.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: ['reorderable-list', 'drag-handle', 'live-region'],
    },
    testing: {
      unit: [
        'Named list, labelled and described handles; axe.',
        'Keyboard: pick up, Right, Down by a measured row, edges, drop calls onReorder once with `{ item, from, to }`; announcements.',
        'Escape restores; Home/End; arrows do nothing before pick-up; Tab away cancels.',
        '`columns` row step; RTL Left/Right.',
        'Controlled and snapping back; locked tiles and a disabled grid.',
        'Pointer: threshold, nearest slot in 2D, data-active, drop commits; pointercancel cancels.',
        'Translated messages; minimum width variable; server rendering.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
