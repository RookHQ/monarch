import { useState } from 'react';
import { Badge } from '@rookcom/monarch-react';
import { ReorderableList } from '@rookcom/monarch-react/data';
import type { ComponentDocs } from '../../../internal/component-docs.js';

interface Stage {
  id: string;
  name: string;
  count: number;
  locked?: boolean;
}

const STAGES: Stage[] = [
  { id: 'new', name: 'New', count: 12 },
  { id: 'contacted', name: 'Contacted', count: 8 },
  { id: 'proposal', name: 'Proposal sent', count: 5 },
  { id: 'negotiation', name: 'Negotiation', count: 3 },
];

function StagesDemo() {
  const [stages, setStages] = useState(STAGES);
  return (
    <div style={{ width: '100%', maxWidth: 420 }}>
      <ReorderableList
        aria-label="Pipeline stages"
        items={stages}
        onReorder={setStages}
        getKey={(s) => s.id}
        getLabel={(s) => s.name}
        renderItem={(s) => (
          <span style={{ display: 'flex', justifyContent: 'space-between', gap: 8 }}>
            <span>{s.name}</span>
            <Badge>{s.count}</Badge>
          </span>
        )}
      />
    </div>
  );
}

function ButtonsDemo() {
  const [fields, setFields] = useState([
    { id: 'name', name: 'Full name', locked: true },
    { id: 'email', name: 'Email address' },
    { id: 'phone', name: 'Phone number' },
    { id: 'notes', name: 'Anything else?' },
  ]);
  return (
    <div style={{ width: '100%', maxWidth: 420 }}>
      <ReorderableList
        aria-label="Form fields"
        items={fields}
        onReorder={setFields}
        moveButtons
        isItemDisabled={(f) => f.locked === true}
        getKey={(f) => f.id}
        getLabel={(f) => f.name}
        renderItem={(f, { disabled }) => (
          <span>
            {f.name}
            {disabled ? ' (locked)' : null}
          </span>
        )}
      />
    </div>
  );
}

export default {
  id: 'reorderable-list',
  title: 'ReorderableList',
  summary:
    'A list reordered by dragging a handle (mouse, pen, touch) or fully from the keyboard, with every step announced.',
  sections: {
    purpose: {
      summary:
        'Lets people put a short list in their own order, such as pipeline stages, form fields, banners or approval steps, by drag or keyboard, with the same result. Import from `@rookcom/monarch-react/data`.',
      useWhen: [
        'The order of a list is user data the app saves: stages, sections, fields, priorities.',
        'A settings list where “Move up / Move down” buttons are the accessible fallback today.',
      ],
      avoidWhen: [
        'Moving cards between columns (a board): this is a single vertical list.',
        'Sorting by a value: offer a sort control instead of manual order.',
        'Very long or virtualised lists: auto-scrolling while dragging is not provided.',
      ],
    },
    designPrinciples: {
      items: [
        'Drag only from the grip handle, so text in the row stays selectable and buttons in it stay clickable.',
        'Keyboard is first-class, not a fallback: the same move happens with Space, arrows and Space, and Escape always undoes it.',
        'Say what happened: “Picked up”, “moved to position 3 of 5”, “dropped”, “cancelled” go to a live region.',
        'Nothing is saved until the drop: `onReorder` fires once per finished move with the new order, ready for one API call.',
        'Rows glide to their new place (a FLIP transition) unless the user prefers reduced motion.',
        'No drag-and-drop library: pointer events with capture cover mouse, pen and touch in a few hundred lines.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'Generic over the item type: `ReorderableList<T>` with `getKey`, `getLabel` and `renderItem(item, { index, active, disabled })`.',
        'Renders `<div class="mn-reorderable-list">` with a `<ul>` (the ref and the rest of the props go to the list) and two visually hidden helpers: the instructions and the live region.',
        '`onReorder(items, { item, from, to })` gets a new array; `moveItem(list, from, to)` is exported for apps that move items elsewhere (for example after an undo).',
      ],
    },
    typedProperties: { source: 'typescript' },
    variants: {
      items: [
        {
          prop: 'moveButtons',
          values: ['false', 'true'],
          description:
            'Adds Move up / Move down buttons to each row (disabled at the ends): a single-click alternative for switch and voice users.',
        },
        {
          prop: 'isItemDisabled / disabled',
          values: ['(item) => boolean', 'boolean'],
          description: 'Locks single items (others can still move past them) or the whole list.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Picked up (keyboard)',
          attribute: 'data-active="keyboard" on the row, aria-pressed="true" on the handle',
          description: 'Raised shadow and accent border; the handle is tinted.',
        },
        {
          name: 'Dragging (pointer)',
          attribute: 'data-active="pointer" on the row, data-dragging="pointer" on the root',
          description: 'The row follows the pointer with a raised shadow; others make room.',
        },
        {
          name: 'Locked',
          attribute: 'data-disabled on the row, disabled handle and buttons',
          description: 'The handle shows the disabled colour and a not-allowed cursor.',
        },
        {
          name: 'Focus',
          description: 'Handles and buttons show the Monarch focus ring on `:focus-visible`.',
        },
      ],
    },
    interaction: {
      items: [
        'Pointer: press the handle and move more than 4px to start; the row follows the pointer and the list reorders as its centre passes others; release to drop. pointercancel (for example a system gesture) cancels.',
        'The handle sets `touch-action: none`, so on touch screens dragging the handle never scrolls the page; the rest of the row scrolls normally.',
        'Keyboard moves are previewed in place and committed on drop; Escape, Tab away, or focus leaving the list cancels and restores the order.',
        'Controlled with `items` + `onReorder`, or uncontrolled with `defaultItems`. A controlled list that does not update `items` snaps back.',
        'If the moving item disappears from `items` mid-move, the move ends quietly.',
      ],
    },
    keyboard: {
      items: [
        { keys: 'Tab', action: 'Moves between handles (and move buttons).' },
        { keys: 'Space / Enter', action: 'Picks up the focused item; pressed again, drops it.' },
        {
          keys: 'ArrowUp / ArrowDown (or Left / Right)',
          action: 'Moves the picked-up item one place.',
        },
        { keys: 'Home / End', action: 'Moves the picked-up item to the start or end.' },
        {
          keys: 'Escape',
          action: 'Cancels the move (keyboard or pointer) and restores the order.',
        },
      ],
    },
    accessibility: {
      role: 'list with button handles',
      name: 'Give the list an `aria-label` (“Pipeline stages”). Each handle is “Reorder <label>” (`messages.handleLabel`).',
      items: [
        'Every handle is described by one visually hidden instruction (`aria-describedby`).',
        'Pick-up, each move, drop and cancel are announced in an assertive live region with the position (“position 3 of 5”).',
        'Focus stays on the moved item’s handle after every keyboard move and button press (or its handle when the button became disabled).',
        'Move buttons are named per item (“Move Email address up”).',
        'Verified with axe in jsdom and in Chromium, Firefox and WebKit on /components/reorderable-list.',
      ],
    },
    responsive: {
      items: [
        'Rows fill their container and wrap long content; nothing has a fixed width, so it works at 320px.',
        'On coarse pointers handles and move buttons grow to `--mn-control-md` (40px) touch targets.',
      ],
    },
    theme: {
      items: [
        'Monarch tokens only: surface rows with a subtle border; the active row uses `--mn-color-accent-border` and `--mn-shadow-md`.',
        'Row transitions are removed under `prefers-reduced-motion: reduce` and by the FLIP code itself.',
        'Forced-colours mode outlines the active row in Highlight.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Drag or keyboard',
          stack: true,
          render: () => <StagesDemo />,
          code: `const [stages, setStages] = useState(initial);
<ReorderableList
  aria-label="Pipeline stages"
  items={stages}
  onReorder={(next, { item, to }) => { setStages(next); api.moveStage(item.id, to); }}
  getKey={(s) => s.id}
  getLabel={(s) => s.name}
  renderItem={(s) => <>{s.name} <Badge>{s.count}</Badge></>}
/>`,
        },
        {
          title: 'Move buttons and a locked item',
          stack: true,
          render: () => <ButtonsDemo />,
          code: `<ReorderableList
  aria-label="Form fields"
  items={fields}
  onReorder={setFields}
  moveButtons
  isItemDisabled={(f) => f.locked}
  getKey={(f) => f.id}
  getLabel={(f) => f.name}
  renderItem={(f) => f.name}
/>`,
        },
      ],
    },
    doDont: {
      do: [
        'Give every item a short, unique `getLabel`: it is what screen-reader users hear.',
        'Save once in `onReorder`, and roll back (pass the old `items`) if the save fails.',
        'Turn on `moveButtons` in settings screens where reordering is the main task.',
      ],
      dont: [
        'Do not put drag handlers on the whole row; keep the handle.',
        'Do not reorder on every intermediate move: wait for `onReorder`.',
        'Do not use it for boards with columns or for lists of hundreds of rows.',
      ],
    },
    related: {
      items: [
        {
          id: 'data-table',
          reason: 'Tabular data the user sorts by a column rather than by hand.',
        },
        { id: 'stepper', reason: 'Shows progress through an order that is fixed, not edited.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: [],
    },
    testing: {
      unit: [
        'Named list, labelled and described handles, axe.',
        'Keyboard: Space picks up, arrows move with focus kept, Space drops and calls onReorder once with `{ item, from, to }`; each step announced.',
        'Escape cancels and restores; Home/End; moves stop at the edges; arrows do nothing before pick-up; Tab away cancels.',
        'Controlled: follows `items`, and snaps back when the parent ignores onReorder.',
        'Move buttons: disabled at the ends, announce, keep focus on the moved item; axe.',
        'Locked items and a disabled list; other items move past a locked one.',
        'Pointer: threshold before a drag, reorder by row centres, data-active, drop commits; pointercancel and Escape cancel.',
        'Translated messages; `moveItem` clamps and copies; renders on the server.',
      ],
      axe: true,
      ssr: true,
      browser: [
        'Visual baselines: /__gallery/reorderable-list',
        'Cross-browser axe: /components/reorderable-list',
      ],
    },
  },
} satisfies ComponentDocs;
