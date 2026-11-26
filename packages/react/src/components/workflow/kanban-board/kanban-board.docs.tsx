import { useState } from 'react';
import { Badge, IconButton } from '@rookcom/monarch-react';
import { KanbanBoard, type KanbanColumn } from '@rookcom/monarch-react/collaboration';
import type { ComponentDocs } from '../../../internal/component-docs.js';
import { PlusIcon } from '../../../internal/docs-demo.js';

interface Task {
  id: string;
  title: string;
  tag: string;
  tone: 'neutral' | 'info' | 'warning' | 'success';
}

const TASKS: KanbanColumn<Task>[] = [
  {
    id: 'backlog',
    title: 'Backlog',
    items: [
      { id: 'k1', title: 'Draft release notes', tag: 'Docs', tone: 'neutral' },
      { id: 'k2', title: 'Audit colour contrast', tag: 'Design', tone: 'info' },
      { id: 'k3', title: 'Plan onboarding survey', tag: 'Research', tone: 'neutral' },
    ],
  },
  {
    id: 'doing',
    title: 'In progress',
    wipLimit: 2,
    items: [
      { id: 'k4', title: 'Fix export timeout', tag: 'Bug', tone: 'warning' },
      { id: 'k5', title: 'Billing settings page', tag: 'Feature', tone: 'info' },
      { id: 'k6', title: 'Update help articles', tag: 'Docs', tone: 'neutral' },
    ],
  },
  {
    id: 'review',
    title: 'Review',
    items: [{ id: 'k7', title: 'Keyboard shortcuts', tag: 'Feature', tone: 'info' }],
  },
  { id: 'done', title: 'Done', items: [] },
];

function TaskCard({ task }: { task: Task }) {
  return (
    <div style={{ display: 'grid', gap: 6 }}>
      <span style={{ fontWeight: 500 }}>{task.title}</span>
      <span>
        <Badge tone={task.tone}>{task.tag}</Badge>
      </span>
    </div>
  );
}

function BoardDemo() {
  const [columns, setColumns] = useState(TASKS);
  return (
    <KanbanBoard
      aria-label="Sprint board"
      columns={columns}
      onMove={(_move, next) => setColumns(next)}
      getItemId={(t) => t.id}
      getItemLabel={(t) => t.title}
      renderCard={(t) => <TaskCard task={t} />}
    />
  );
}

/** Review → Done only; Done is final. Saving "Fix export timeout" fails, to show rollback. */
const ALLOWED: Record<string, string[]> = {
  backlog: ['doing'],
  doing: ['backlog', 'review'],
  review: ['doing', 'done'],
  done: [],
};

function RulesDemo() {
  const [columns, setColumns] = useState<KanbanColumn<Task>[]>(
    TASKS.map(({ wipLimit: _limit, ...c }) => c),
  );
  return (
    <KanbanBoard
      aria-label="Review pipeline"
      columns={columns}
      moveMenu
      canMove={(_t, to, from) => ALLOWED[from]?.includes(to) ?? false}
      onMove={(move, next) => {
        const previous = columns;
        setColumns(next);
        return new Promise<void>((resolve, reject) =>
          setTimeout(() => {
            if (move.itemId === 'k4') {
              setColumns(previous);
              reject(new Error('Saving failed'));
            } else resolve();
          }, 600),
        );
      }}
      getItemId={(t) => t.id}
      getItemLabel={(t) => t.title}
      renderCard={(t, state) => (
        <div style={{ display: 'grid', gap: 6 }}>
          <span style={{ fontWeight: 500 }}>{t.title}</span>
          <span>
            <Badge tone={state.pending ? 'info' : t.tone}>
              {state.pending ? 'Saving…' : t.tag}
            </Badge>
          </span>
        </div>
      )}
    />
  );
}

function CollapsibleDemo() {
  const [collapsed, setCollapsed] = useState<string[]>(['done']);
  return (
    <KanbanBoard
      aria-label="Content board"
      defaultColumns={TASKS.map((c) => ({
        ...c,
        actions: <IconButton size="xs" icon={<PlusIcon />} label={`Add a card to ${c.title}`} />,
      }))}
      collapsible
      collapsedColumnIds={collapsed}
      onCollapsedColumnIdsChange={setCollapsed}
      enforceWipLimits
      dragFrom="card"
      getItemId={(t) => t.id}
      getItemLabel={(t) => t.title}
      renderCard={(t) => <TaskCard task={t} />}
    />
  );
}

export default {
  id: 'kanban-board',
  title: 'KanbanBoard',
  summary:
    'Columns of cards moved by drag (mouse, pen, touch) or keyboard, with counts, WIP limits, collapse and optimistic moves.',
  sections: {
    purpose: {
      summary:
        'Shows work as cards in columns, such as stages or statuses, and lets people move cards between and within columns by dragging or from the keyboard. The product renders the cards and saves each move.',
      useWhen: [
        'Items move through a small set of stages: a task board, a hiring pipeline, a deal pipeline, filings in review.',
        'The position of a card within its column carries meaning (priority or rank) and people reorder it.',
        'A stage has a work-in-progress limit people need to see.',
      ],
      avoidWhen: [
        'The order of a single list is all that changes: use ReorderableList.',
        'There are many columns of attributes to compare across rows: use DataTable.',
        'Stages are a fixed sequence the user walks through once: use Stepper.',
      ],
    },
    designPrinciples: {
      items: [
        'Cards are a slot: the product draws them with `renderCard`; the board adds the grip and, optionally, the "Move to…" menu around them.',
        'Every drag has a keyboard equivalent and a single-pointer alternative (`moveMenu`, WCAG 2.5.7). Nothing is drag-only.',
        'Moves are announced at every step in a live region, in the same words ReorderableList uses.',
        'Persistence belongs to the product: `onMove` gets the move and the next columns; returning a promise makes the move optimistic with rollback.',
        'Rules are injected, never built in: `canMove` and `enforceWipLimits` decide which columns accept a card, and refused columns say so while dragging.',
        'Colour is never the only signal: an over-limit count also shows a warning icon and "count / limit", and refused columns change border style and show text.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        '`KanbanBoard` is generic in the card type: `<KanbanBoard<Task> … />`. `getItemId`, `getItemLabel` and `renderCard` are required.',
        '`columns` (controlled) or `defaultColumns` (uncontrolled): `{ id, title, items, wipLimit?, icon?, actions?, footer?, empty? }`.',
        '`onMove(move, nextColumns)`: `move` is `{ item, itemId, fromColumnId, fromIndex, toColumnId, toIndex }`. Return a promise for an optimistic move; a rejection puts the card back, announces the failure and calls `onMoveError`.',
        'A controlled board keeps a resolved move on screen until the next `columns` prop arrives, so a refetch can land without the card flickering back.',
        'The ref is the root `<div>`, which becomes a `region` landmark when given `aria-label` or `aria-labelledby`. Other div attributes pass through.',
        'The drag helpers (`kanban-board.drag.ts`) are internal; they are written to be shared with ReorderableList once both have landed.',
      ],
    },
    typedProperties: { source: 'typescript' },
    variants: {
      items: [
        {
          prop: 'dragFrom',
          values: ['handle', 'card'],
          description:
            '`handle` (default): drag from the grip. `card`: a mouse or pen can drag from anywhere on the card except its buttons, links and fields; touch keeps using the grip so the column still scrolls.',
        },
        {
          prop: 'moveMenu',
          values: ['true', 'false'],
          description:
            'Adds a "Move to…" menu button to each card listing the other columns; refused columns are disabled. The card goes to the end of the chosen column.',
        },
        {
          prop: 'collapsible',
          values: ['true', 'false'],
          description:
            'Adds a collapse toggle to every column header. A collapsed column is a narrow strip with its title and count, and still accepts dropped cards.',
        },
        {
          prop: 'enforceWipLimits',
          values: ['true', 'false'],
          description:
            'Columns at or over their `wipLimit` refuse new cards. Without it, limits are shown but not enforced.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Dragging (pointer)',
          attribute: 'data-dragging="pointer" on the root and the card',
          description:
            'A copy of the card follows the pointer; the card’s slot shows as a dashed placeholder where it would land.',
        },
        {
          name: 'Dragging (keyboard)',
          attribute: 'data-dragging="keyboard", aria-pressed="true" on the grip',
          description:
            'The picked-up card is raised with an accent border and moves as keys are pressed.',
        },
        {
          name: 'Drop target',
          attribute: 'data-drop="allowed" | "blocked", data-over',
          description:
            'While dragging, the column under the card is highlighted; columns that refuse the card get a dashed border and "Not allowed here".',
        },
        {
          name: 'WIP limit',
          attribute: 'data-wip="at" | "over"',
          description:
            'The count turns warning at the limit and danger over it, with a warning icon, and the text alternative says so.',
        },
        {
          name: 'Pending',
          attribute: 'data-pending, aria-busy',
          description: 'The card’s move is waiting for the `onMove` promise; it is shown dimmed.',
        },
        {
          name: 'Collapsed',
          attribute: 'data-collapsed',
          description: 'The column is a narrow vertical strip; its list is hidden.',
        },
        {
          name: 'Disabled',
          attribute: 'data-disabled',
          description:
            '`disabled` or `isItemDisabled`: the grip is disabled and the card cannot move.',
        },
      ],
    },
    interaction: {
      items: [
        'A press on the grip becomes a drag after the pointer moves 4px, so clicks and taps still work. The card lands in the column under the pointer, between the cards whose centres it passes.',
        'Dropping outside every column, or pressing Escape, cancels and the card returns. Dropping on a refusing column also cancels and says why.',
        'Near the edge of the board or a column, dragging scrolls it.',
        'With `dragFrom="card"`, the click that ends a drag does not reach the card, so the card is not opened by accident.',
        'Keyboard: Space or Enter on the grip picks the card up; arrows move it; Space or Enter drops; Escape or Tab cancels. Focus stays on the card’s grip as it changes column. Collapsed and refusing columns are passed over.',
        'A drop into a collapsed column (pointer or menu) moves focus to that column’s expand button.',
        '`onMove` runs once per finished move, never while dragging.',
      ],
    },
    keyboard: {
      items: [
        {
          keys: 'Tab',
          action: 'Moves between grips, menu buttons, column toggles and the card content.',
        },
        { keys: 'Space / Enter', action: 'On a grip: pick the card up; again: drop it.' },
        {
          keys: 'ArrowUp / ArrowDown',
          action: 'While picked up: move the card within its column.',
        },
        {
          keys: 'ArrowLeft / ArrowRight',
          action:
            'While picked up: move the card to the previous or next column that accepts it (mirrored in right-to-left).',
        },
        { keys: 'Home / End', action: 'While picked up: move to the top or bottom of the column.' },
        { keys: 'Escape', action: 'Cancel the move (keyboard or pointer); the card goes back.' },
        { keys: 'Tab (while picked up)', action: 'Cancels the move, then moves focus on.' },
      ],
      notes: ['The "Move to…" menu follows the DropdownMenu keyboard model.'],
    },
    accessibility: {
      role: 'region (board, when labelled), group per column, list of listitems',
      name: 'Give the board an `aria-label`, such as "Sprint board". Each column is labelled by its heading, and its list by the same heading.',
      items: [
        'Each grip is a button named "Move {card}" and described by the instructions; `aria-pressed` is true while it is picked up.',
        'An assertive live region announces pick-up, every move (column, position of total), drop, cancel, refusals and failed saves.',
        'The header count has a text alternative: "3 cards, limit 2, over the limit".',
        'The pointer copy is `aria-hidden` and `inert`; screen readers only meet the real card.',
        'The "Move to…" menu is a single-pointer alternative to dragging (WCAG 2.5.7 Dragging Movements).',
        'Column titles are headings (`headingLevel`, default 3) so people can jump between columns. Columns are labelled groups, not landmarks, so several boards on a page do not repeat landmark names.',
        'Verified with axe in jsdom and in Chromium, Firefox and WebKit on /components/kanban-board.',
      ],
    },
    responsive: {
      items: [
        'Columns sit in one row that scrolls sideways; each is `--mn-kanban-board-column-width` (18rem) wide but never more than 85% of the board, so the next column peeks in on a phone.',
        'On touch screens columns snap to their start when scrolled (not while dragging).',
        'Give the board a height to make columns scroll on their own; otherwise they grow with their cards.',
        'Touch drags from the grip, which has `touch-action: none`; the rest of the card scrolls normally.',
      ],
    },
    theme: {
      items: [
        'Colours are Monarch semantic tokens only, so light, dark and more-contrast themes apply automatically.',
        'Columns use `--mn-color-bg-subtle`, cards `--mn-color-bg-surface` with `--mn-shadow-xs`; the pointer copy uses `--mn-shadow-lg`.',
        'The drop target uses `--mn-color-accent-subtle` and `--mn-color-accent-strong`; WIP tones use the warning and danger subtle/strong pairs.',
        'Reduced motion removes transitions and the tilt of the pointer copy. Forced colours outline the drop target and placeholder with `Highlight`.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Board with a WIP limit',
          stack: true,
          render: () => <BoardDemo />,
        },
        {
          title: 'Allowed moves, Move to menu and optimistic saving',
          stack: true,
          render: () => <RulesDemo />,
        },
        {
          title: 'Collapsible columns, enforced limits, drag from the card',
          stack: true,
          render: () => <CollapsibleDemo />,
        },
      ],
    },
    doDont: {
      do: [
        'Name the board with `aria-label` and give every card a short, unique `getItemLabel`.',
        'Return the save promise from `onMove` so a failed save puts the card back and is announced.',
        'Express workflow rules with `canMove` so refused columns are shown and skipped, rather than undoing moves after the fact.',
        'Turn on `moveMenu` when cards are small or used on touch screens.',
      ],
      dont: [
        'Do not make the whole card the only way to move it: keep the grip, which is the keyboard path.',
        'Do not put many columns of data on a card; open a detail view instead.',
        'Do not hide business rules in the board: stage gates and side effects (such as asking for a reason) stay in the product, after `onMove`.',
      ],
    },
    related: {
      items: [
        { id: 'reorderable-list', reason: 'One list to reorder, with the same keyboard model.' },
        { id: 'data-table', reason: 'Rows with many attributes to sort and compare.' },
        {
          id: 'dropdown-menu',
          reason: 'The "Move to…" menu, and card actions inside `renderCard`.',
        },
        { id: 'stepper', reason: 'A fixed sequence of steps the user goes through once.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: ['dropdown-menu'],
      notes: [
        'Dragging uses pointer events only; no drag-and-drop library.',
        '`--mn-kanban-board-column-width` sets the column width (default 18rem).',
      ],
    },
    testing: {
      unit: [
        'Columns, headings, counts and WIP state render; axe passes.',
        'Keyboard: pick up, move within and across columns, Home/End, drop; announcements at each step; focus stays on the moved card; right-to-left mirrors the arrows.',
        'Escape and Tab cancel and restore the order; nothing is saved.',
        'canMove and enforced WIP limits refuse columns, which are marked and skipped, with a "cannot move" announcement when none is left.',
        'Optimistic moves: pending state while the promise runs, rollback and announcement on rejection, commit on resolve (uncontrolled), held until new columns (controlled).',
        'Controlled boards apply `onMove` columns; the Move to menu moves to the end of a column, disables refused columns and restores focus.',
        'Collapse (uncontrolled and controlled) with aria-expanded; keyboard moves skip collapsed columns.',
        'Pointer: drag threshold, cross-column and within-column drops, cancel outside, on a refusing column and with Escape, drag from the card body but not from its controls or by touch, click suppression, drop into a collapsed column.',
        'Disabled cards and board; renderCard state; translated messages; server rendering; drag helper maths.',
      ],
      axe: true,
      ssr: true,
      browser: [
        'Visual baselines: /__gallery/kanban-board',
        'Cross-browser axe: /components/kanban-board',
      ],
    },
  },
} satisfies ComponentDocs;
