import { useState } from 'react';
import { Badge } from '@rookcom/monarch-react';
import {
  GanttTimeline,
  type GanttDependency,
  type GanttItem,
  type GanttMilestone,
} from '@rookcom/monarch-react/patterns';
import type { ComponentDocs } from '../../../internal/component-docs.js';

const TODAY = '2026-01-14';

const TASKS: GanttItem[] = [
  {
    id: 'brief',
    label: 'Write the brief',
    start: '2026-01-05',
    end: '2026-01-09',
    progress: 100,
    tone: 'success',
  },
  { id: 'design', label: 'Design', start: '2026-01-12', end: '2026-01-23', progress: 60 },
  {
    id: 'build',
    label: 'Build',
    start: '2026-01-21',
    end: '2026-02-13',
    conflict: 'Starts before Design ends',
  },
  { id: 'api', label: 'API changes', parentId: 'build', start: '2026-01-26', end: '2026-02-04' },
  { id: 'ui', label: 'Screens', parentId: 'build', start: '2026-02-02', end: '2026-02-13' },
  { id: 'launch', label: 'Launch', end: '2026-02-20', meta: <Badge size="sm">Fixed date</Badge> },
  { id: 'research', label: 'Pricing research' },
];
const LINKS: GanttDependency[] = [
  { id: 'l1', from: 'brief', to: 'design' },
  { id: 'l2', from: 'design', to: 'build', conflict: true },
  { id: 'l3', from: 'api', to: 'ui', kind: 'start-to-start' },
];
const MILESTONES: GanttMilestone[] = [
  { id: 'm1', label: 'Design review', date: '2026-01-23' },
  { id: 'm2', label: 'Beta', date: '2026-02-09' },
];

function EditableDemo() {
  const [items, setItems] = useState(TASKS);
  return (
    <GanttTimeline
      items={items}
      dependencies={LINKS}
      milestones={MILESTONES}
      today={TODAY}
      defaultStart="2026-01-05"
      onChange={(item, dates) =>
        setItems((all) => all.map((i) => (i.id === item.id ? { ...i, ...dates } : i)))
      }
      onOpen={() => {}}
    />
  );
}

export default {
  id: 'gantt-timeline',
  title: 'GanttTimeline',
  summary:
    'Items as bars on a calendar, moved and resized by pointer or keyboard, with dependency arrows, milestones, a today line, zoom and a table view.',
  sections: {
    purpose: {
      summary:
        'Plans and adjusts work over time: each item is a bar from its start to its end date, dependencies are arrows, milestones are diamonds, and people move or resize bars by dragging, from the keyboard or in the table. Needed by: Stride (IssueTimeline on projects and initiatives, Roadmap), and any app scheduling phases or tasks.',
      useWhen: [
        'Dates and overlaps matter: tasks, phases or issues with start and due dates.',
        'Dependencies between items must be visible, and broken ones flagged.',
        'People reschedule by moving bars rather than editing fields one by one.',
      ],
      avoidWhen: [
        'Work flows through statuses without dates: use KanbanBoard.',
        'Planning into fixed iterations: use BacklogPlanner.',
        'Events at times of day: use TimeGrid or CalendarMonthView.',
      ],
    },
    designPrinciples: {
      items: [
        'Nothing is pointer-only: every bar is a button with keyboard moves and resizes, and the table view lists and edits every date (WCAG 2.5.7).',
        'Conflicts are words, a dashed outline and an icon, never colour alone; dependency arrows that break are dashed too.',
        'The timeline never reschedules on its own: it reports `onChange` and the product decides, with optimistic display and rollback.',
        'No extra libraries: calendar maths in UTC days, bars positioned with CSS, arrows in a small SVG.',
        'Hydration-safe: pass `today` for identical server markup; otherwise today is read after mount.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'The ref is the root `<div>`; other div attributes pass through. Set `--mn-gantt-timeline-height` (36rem) and `--mn-gantt-timeline-label-width` (18rem; 8.5rem below 40rem).',
        'Dates are ISO day strings (`2026-01-12`), read as UTC days. An item with only `start` or only `end` is a one-day marker; without either it is listed under "Unscheduled" (or kept as a "No dates" row with `undated="row"`). A parent without dates shows a bracket spanning its children.',
        '`onChange(item, { start, end })` runs once per finished drag, keystroke or table edit. Return a promise to keep the new dates while saving; a rejection restores them and announces it.',
        '`shiftGanttDates(item, mode, days)` is exported so products can apply the same move rules.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        'Migrating from Stride `IssueTimeline` (`components/timeline/IssueTimeline.tsx`): `data.issues` → `items` (`identifier` + `title` → `label`, `title` node with the key and status icon; `startDate` → `start`, `dueDate` → `end`, `parentId` unchanged, closed issues → `tone: "neutral"`, `canEditIn` false → `editable: false`, `conflictsByIssue` → `conflict`); `data.dependencies` → `dependencies` (`fromId`/`toId` → `from`/`to`, `START_TO_START` → `start-to-start`, `FINISH_TO_FINISH` → `finish-to-finish`, conflicting edges → `conflict: true`); `data.milestones` → `milestones` (`targetDate` → `date`, `completedAt` → `done`); `update.mutate({ patch })` → `onChange` (map `start`/`end` back to `startDate`/`dueDate`); `openIssue` → `onOpen`. Zoom `week`/`month`/`quarter` are kept and `year` added (`zoomLevels`). The conflict notice above the chart stays in the product.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'zoomLevels / zoom',
          values: ['week', 'month', 'quarter', 'year'],
          description:
            'Visible window: 4 weeks, 13 weeks, 9 months or 2 years. Default offered: month, quarter, year.',
        },
        {
          prop: 'defaultView',
          values: ['chart', 'table'],
          description:
            'The table lists every item with dates, duration, progress and dependencies; editable timelines get date fields.',
        },
        {
          prop: 'undated',
          values: ['list', 'row'],
          description:
            'Where items without dates go: a list under the chart, or rows saying "No dates".',
        },
        {
          prop: 'onChange',
          values: ['set', 'unset'],
          description:
            'With it bars can be dragged, resized and nudged; without it the timeline is read-only.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Editable',
          attribute: 'data-editable on the bar',
          description:
            'Grab cursor and resize handles; the keyboard instructions are its description.',
        },
        {
          name: 'Dragging',
          attribute: 'data-dragging',
          description: 'The bar follows the pointer by whole days, raised with a large shadow.',
        },
        {
          name: 'Saving',
          attribute: 'aria-busy',
          description: 'The new dates are shown, faded, until the `onChange` promise settles.',
        },
        {
          name: 'Conflict',
          attribute: 'data-conflict',
          description:
            'Danger tone, dashed outline, warning icon and the message in the accessible name.',
        },
        {
          name: 'Point',
          attribute: 'data-shape="point"',
          description: 'A one-date item drawn as a diamond.',
        },
        {
          name: 'Read-only',
          attribute: 'data-readonly on the root',
          description: 'No `onChange`: bars only open and navigate.',
        },
      ],
    },
    interaction: {
      items: [
        'Drag a bar to move it, or its edges to change the start or end; dates snap to whole days and save once on release. Escape cancels.',
        'Clicking a bar (not the click that ends a drag) calls `onOpen`.',
        'Earlier and Later step the window; Today brings it back; the zoom control changes the scale.',
        'Parents collapse and expand their sub-items from the row heading.',
        'In the table view, editable items have start and end date fields that save on leaving the field.',
      ],
    },
    keyboard: {
      items: [
        { keys: 'Tab', action: 'Moves through the toolbar, row toggles and bars.' },
        { keys: 'ArrowLeft / ArrowRight', action: 'On a bar: move it a day earlier or later.' },
        { keys: 'Shift + ArrowLeft / ArrowRight', action: 'On a bar: change the end date.' },
        { keys: 'Alt + ArrowLeft / ArrowRight', action: 'On a bar: change the start date.' },
        { keys: 'ArrowUp / ArrowDown', action: 'Go to the previous or next bar.' },
        { keys: 'Enter / Space', action: 'Open the item (`onOpen`).' },
        { keys: 'Escape', action: 'Cancel a pointer drag.' },
      ],
      notes: ['Zoom and view switches follow the SegmentedControl keyboard model.'],
    },
    accessibility: {
      role: 'group (chart) with buttons; table in the table view',
      name: '"Timeline" (`messages.label`); each bar is named "Design, 12 Jan 2026 to 23 Jan 2026, 60% complete" plus any conflict.',
      items: [
        'Editable bars are described by the keyboard instructions; each keyboard change is announced politely.',
        'Milestones are images with names ("Milestone Beta, 9 Feb 2026"); gridlines, the today line and arrows are hidden, and the table view states every date and dependency.',
        'Failed saves are announced and the bar goes back.',
        'Row toggles are named "Expand {item}"/"Collapse {item}" with `aria-expanded`.',
      ],
    },
    responsive: {
      items: [
        'The chart scrolls inside its own area; the row labels stay pinned and narrow to 8.5rem below 40rem, so it fits 390px without page scrolling.',
        'The toolbar wraps; on coarse pointers rows are 44px tall and toggles 44px wide.',
        'On touch, bars that can be edited take the drag; the page still scrolls from the labels, header and empty grid.',
      ],
    },
    theme: {
      items: [
        'Semantic tokens only: bars use the accent, success, warning, danger or neutral subtle/solid pairs; weekends bg subtle; today accent solid.',
        'Light, dark and high-contrast themes apply automatically; forced colours draw bars in CanvasText and progress, done milestones and today in Highlight.',
        'Reduced motion removes the toggle rotation; dragging has no animation.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Editable plan with dependencies and milestones',
          stack: true,
          render: () => <EditableDemo />,
        },
        {
          title: 'Read-only, table view first',
          stack: true,
          render: () => (
            <GanttTimeline items={TASKS} dependencies={LINKS} today={TODAY} defaultView="table" />
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Give every item a short `label`; it names the bar.',
        'Pass `today` from the server when rendering there.',
        'Send scheduling problems from the server as `conflict` text.',
      ],
      dont: [
        'Do not hide the table view; it is the accessible way to read every date.',
        'Do not reschedule dependants automatically without telling people; show conflicts instead.',
      ],
    },
    related: {
      items: [
        { id: 'roadmap', reason: 'A grouped, read-only portfolio view built on this timeline.' },
        { id: 'backlog-planner', reason: 'Planning into iterations instead of dates.' },
        { id: 'kanban-board', reason: 'Status-based flow without dates.' },
        { id: 'calendar-month-view', reason: 'Events on days of a month.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: ['button', 'segmented-control', 'text-field'],
      notes: [
        'No date or chart library: UTC-day maths in `gantt-timeline.dates.ts`, bars in CSS, arrows in SVG.',
      ],
    },
    testing: {
      unit: [
        'Date maths and move/resize rules.',
        'Bars, points, conflicts, milestones, today line, dependencies, nesting and unscheduled items with accessible names; axe passes.',
        'Keyboard move, end and start resize, bar-to-bar navigation and announcements.',
        'Pointer move and edge resize saving once; Escape cancels; failed saves roll back and are announced.',
        'Read-only opening by click and Enter; zoom, navigation and collapse; table view with editable dates; groups, row meta and off-screen items; server rendering with and without `today`.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
