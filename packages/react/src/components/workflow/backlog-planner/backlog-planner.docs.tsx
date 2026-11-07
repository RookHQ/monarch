import { useState } from 'react';
import { Badge, Button } from '@rookcom/monarch-react';
import { BacklogPlanner, type BacklogBucket } from '@rookcom/monarch-react/patterns';
import type { ComponentDocs } from '../../../internal/component-docs.js';

interface Item {
  id: string;
  key: string;
  title: string;
  points: number | null;
}

const START: BacklogBucket<Item>[] = [
  {
    id: 'it-14',
    title: 'Iteration 14',
    subtitle: '12 – 23 Jan',
    badge: (
      <Badge tone="accent" size="sm">
        Active
      </Badge>
    ),
    capacity: 13,
    items: [
      { id: '1', key: 'WEB-21', title: 'Fix the sign-in redirect', points: 3 },
      { id: '2', key: 'WEB-24', title: 'Export reports as CSV', points: 5 },
      { id: '3', key: 'WEB-30', title: 'Keyboard shortcuts help', points: 2 },
    ],
  },
  {
    id: 'it-15',
    title: 'Iteration 15',
    subtitle: '26 Jan – 6 Feb',
    capacity: 13,
    actions: <Button size="xs">Start</Button>,
    items: [{ id: '4', key: 'WEB-33', title: 'Dark mode for charts', points: 8 }],
    empty: 'Move items here to plan them into this iteration.',
  },
  {
    id: 'backlog',
    title: 'Backlog',
    items: [
      { id: '5', key: 'WEB-35', title: 'Audit log filters', points: 5 },
      { id: '6', key: 'WEB-36', title: 'Bulk archive', points: null },
      { id: '7', key: 'WEB-41', title: 'Saved views', points: 3 },
    ],
  },
];

function Row({ item }: { item: Item }) {
  return (
    <span style={{ display: 'flex', gap: 8, minWidth: 0 }}>
      <span style={{ fontFamily: 'var(--mn-font-mono)', opacity: 0.7 }}>{item.key}</span>
      <span style={{ overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
        {item.title}
      </span>
    </span>
  );
}

function PlannerDemo() {
  const [buckets, setBuckets] = useState(START);
  return (
    <BacklogPlanner
      aria-label="Iteration planning"
      buckets={buckets}
      getItemId={(i) => i.id}
      getItemLabel={(i) => `${i.key} ${i.title}`}
      getItemPoints={(i) => i.points}
      renderItem={(i) => <Row item={i} />}
      onMove={(_move, next) => setBuckets(next)}
    />
  );
}

export default {
  id: 'backlog-planner',
  title: 'BacklogPlanner',
  summary:
    'Backlog and iteration planning: buckets stacked top to bottom with items moved by keyboard, pointer or a Move to menu, and points against capacity.',
  sections: {
    purpose: {
      summary:
        "Plans work into time-boxed buckets: the active and planned iterations above the backlog, with items reordered by priority and moved between buckets, and each bucket's points compared with its capacity. Needed by: Stride (BacklogPlanner on a team's backlog page).",
      useWhen: [
        'A team pulls ranked work from a backlog into sprints, iterations or releases.',
        'Capacity in points (or any unit) matters when planning.',
        'Priority order inside each bucket is meaningful and edited here.',
      ],
      avoidWhen: [
        'Work moves through statuses, not time boxes: use KanbanBoard.',
        'One list to reorder: use ReorderableList.',
        'Dates and dependencies matter more than buckets: use GanttTimeline.',
      ],
    },
    designPrinciples: {
      items: [
        'Every drag has a keyboard path and a single-pointer path: the grip (Space, arrows across buckets, Space) and the "Move to…" menu.',
        'Moves are announced at each step in an assertive live region, in the same words as KanbanBoard.',
        'Capacity is in words: the meter has value text and an over-capacity note says by how much.',
        'The product owns persistence and rules: `onMove` gets the move and the next buckets; `canMove` refuses buckets.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        '`BacklogPlanner` is generic in the item type: `getItemId`, `getItemLabel` and `renderItem` are required.',
        '`onMove(move, nextBuckets)`: return a promise to keep the move on screen while saving; a rejection puts the item back and announces it. Without `onMove` the planner is read-only.',
        'Each bucket can carry a `subtitle`, `badge`, `actions`, `capacity`, `empty` text and a `footer` (a quick-add form).',
        '`moveBacklogItem(buckets, id, { bucketId, index }, getItemId)` is exported for applying a move to your own state.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        'Migrating from Stride `BacklogPlanner` (`components/sprints/BacklogPlanner.tsx`): each open sprint → a bucket (`name` → `title`, dates → `subtitle`, Active pill → `badge`, Edit/Start buttons and the Open link → `actions`, `capacity`), then a `backlog` bucket with QuickAddBacklog as `footer`. `PlanRow` content (priority, key, status, title, estimate, assignee pickers) → `renderItem`; `issue.estimate` → `getItemPoints`. The dnd-kit `onEnd` patch (`sprintId`, `position.afterId/beforeId`) → compute from `move` and `next` in `onMove`. Collapse state → `collapsed`/`onCollapsedChange`. The filter box, sprint dialog and empty state stay in the product. dnd-kit is no longer needed.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'moveMenu',
          values: ['true', 'false'],
          description:
            '`true` (default): each row has a "Move to…" menu listing the other buckets; refused buckets are disabled.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Picked up (keyboard)',
          attribute: 'data-dragging="keyboard", aria-pressed on the grip',
          description: 'The row has an accent outline and moves with the arrow keys.',
        },
        {
          name: 'Dragging (pointer)',
          attribute: 'data-dragging="pointer"',
          description: 'The row is raised and moves live between rows and buckets.',
        },
        {
          name: 'Refused bucket',
          attribute: 'data-refused',
          description: 'While moving, buckets `canMove` refuses are dashed and skipped.',
        },
        {
          name: 'Saving',
          attribute: 'data-pending, aria-busy',
          description: 'The moved row waits for the `onMove` promise.',
        },
        {
          name: 'Over capacity',
          attribute: 'data-over-capacity',
          description: 'Danger-coloured capacity and a warning line with the overage.',
        },
        {
          name: 'Collapsed',
          attribute: 'data-collapsed',
          description: 'Only the header shows; keyboard moves skip it.',
        },
      ],
    },
    interaction: {
      items: [
        'Pointer: press the grip and move 4px to start; the row follows between rows and buckets; release to drop; Escape cancels.',
        'Keyboard: Space or Enter on the grip picks up; ArrowUp/ArrowDown move, continuing into the bucket above or below; Space or Enter drops; Escape or Tab cancels.',
        'Menu: "Move to…" puts the item at the end of the chosen bucket.',
        '`onMove` runs once per finished move, never while dragging.',
      ],
    },
    keyboard: {
      items: [
        { keys: 'Tab', action: 'Moves between bucket toggles, grips, row content and menus.' },
        { keys: 'Space / Enter', action: 'On a grip: pick up; again: drop.' },
        {
          keys: 'ArrowUp / ArrowDown',
          action:
            'While picked up: move, crossing into the previous or next bucket that accepts the item.',
        },
        { keys: 'Home / End', action: 'While picked up: top or bottom of the current bucket.' },
        { keys: 'Escape / Tab', action: 'While picked up: cancel; the item returns.' },
      ],
      notes: ['The "Move to…" menu follows the DropdownMenu keyboard model.'],
    },
    accessibility: {
      role: 'region per bucket (labelled by its heading), list of listitems',
      name: 'Each bucket is named by its title; give the planner an `aria-label` if it shares the page.',
      items: [
        'Grips are named "Move {item}" and described by the keyboard instructions; `aria-pressed` is true while picked up.',
        'Pick-up, each step, drop, cancel and failed saves are announced.',
        'Capacity is a meter with value text such as "18 of 20 points"; unestimated items say "Not estimated".',
        'The menu is a single-pointer alternative to dragging (WCAG 2.5.7).',
      ],
    },
    responsive: {
      items: [
        'Buckets are full-width and stack, so the planner fits 390px; header actions wrap to their own line below 30rem.',
        'On coarse pointers grips, menus, toggles and rows are 44px; the grip has `touch-action: none` so the page still scrolls elsewhere.',
      ],
    },
    theme: {
      items: [
        'Semantic tokens only: bg subtle headers, bg surface rows, accent for picked-up rows, warning subtle/border for the over-capacity note, danger strong for over-capacity numbers.',
        'Light, dark and high-contrast themes apply automatically; forced colours outline moving rows in Highlight.',
        'Reduced motion removes transitions.',
      ],
    },
    examples: {
      items: [{ title: 'Iterations and backlog', stack: true, render: () => <PlannerDemo /> }],
    },
    doDont: {
      do: [
        'Give every item a short, unique `getItemLabel`, such as its key and title.',
        'Return the save promise from `onMove` so a failed save puts the item back.',
        "Put the quick-add form in the backlog bucket's `footer`.",
      ],
      dont: [
        'Do not put many controls in a row; open the item for details.',
        'Do not block planning over capacity; warn, as the planner does.',
      ],
    },
    related: {
      items: [
        { id: 'kanban-board', reason: 'Items moving through status columns.' },
        { id: 'reorderable-list', reason: 'A single list to reorder.' },
        { id: 'roadmap', reason: 'Longer-range plans by period.' },
        { id: 'progress-bar', reason: 'The capacity meter on its own.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: ['dropdown-menu', 'progress-bar'],
      notes: ['Dragging uses pointer events on the window; no drag-and-drop library.'],
    },
    testing: {
      unit: [
        'moveBacklogItem maths.',
        'Headers with counts, points, unestimated items and the capacity meter; axe passes.',
        'Keyboard moves across buckets with announcements and focus kept; Escape cancels; canMove skips buckets.',
        'Move to menu with disabled buckets; optimistic save with rollback and announcement.',
        'Over-capacity note; read-only without onMove; collapse; pointer drag across buckets and Escape cancel; server rendering.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
