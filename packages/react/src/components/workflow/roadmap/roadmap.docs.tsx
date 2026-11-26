import { Roadmap, type RoadmapGroup, type RoadmapItem } from '@rookcom/monarch-react/patterns';
import type { ComponentDocs } from '../../../internal/component-docs.js';

const TODAY = '2026-01-14';

const GROUPS: RoadmapGroup[] = [
  {
    id: 'growth',
    label: 'Grow repeat customers',
    start: '2026-01-01',
    end: '2026-06-30',
    health: 'on-track',
  },
  {
    id: 'quality',
    label: 'Fewer support tickets',
    start: '2026-02-01',
    end: '2026-08-31',
    health: 'at-risk',
  },
];

const PROJECTS: RoadmapItem[] = [
  {
    id: 'p1',
    label: 'Loyalty emails',
    groupId: 'growth',
    start: '2026-01-05',
    end: '2026-03-20',
    progress: 40,
    progressText: '8/20',
    health: 'on-track',
  },
  {
    id: 'p2',
    label: 'Referral page',
    groupId: 'growth',
    start: '2026-03-01',
    end: '2026-05-15',
    progress: 10,
    progressText: '1/10',
    health: null,
  },
  {
    id: 'p3',
    label: 'Help centre search',
    groupId: 'quality',
    start: '2026-02-02',
    end: '2026-04-30',
    progress: 25,
    progressText: '3/12',
    health: 'at-risk',
  },
  {
    id: 'p4',
    label: 'Crash fixes',
    groupId: 'quality',
    start: '2026-01-12',
    end: '2026-02-27',
    progress: 70,
    progressText: '14/20',
    health: 'off-track',
  },
  { id: 'p5', label: 'Billing revamp', progressText: '0/6' },
];

export default {
  id: 'roadmap',
  title: 'Roadmap',
  summary:
    'Projects over months and quarters, grouped by initiative, with progress, reported health, milestones and dependencies.',
  sections: {
    purpose: {
      summary:
        'Gives leads and stakeholders a long-range view of projects: when each runs, how far along it is, what its lead last reported, and which projects block others. Needed by: Stride (Roadmap page and initiative pages).',
      useWhen: [
        'Several projects or epics across teams need to be seen over quarters.',
        'Projects roll up into initiatives, themes or goals.',
        'Health and progress matter more than individual task dates.',
      ],
      avoidWhen: [
        'Scheduling individual tasks by dragging: use GanttTimeline directly.',
        'Planning a sprint: use BacklogPlanner.',
        'A status-only overview: use a DataTable or KanbanBoard.',
      ],
    },
    designPrinciples: {
      items: [
        'Built on GanttTimeline: the same scale, zoom, navigation, table view and keyboard model, preset for portfolio planning.',
        'Read-only by default: a roadmap is for reading; pass `onChange` only where people really replan here.',
        'Health is a word and a dot, never colour alone; "No health reported" is said, not left blank.',
        'Undated projects stay visible in their group as "No dates", so nothing silently disappears.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'The ref and other attributes go to the GanttTimeline root, which also has the `mn-roadmap` class.',
        'Timeline props such as `dependencies`, `milestones`, `today`, `zoom`, `defaultStart`, `toolbarExtra` and `locale` pass through.',
        '`messages` covers health labels and the group count; `timelineMessages` covers the timeline (label "Roadmap", row header "Project" by default).',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        'Migrating from the Stride roadmap page (`app/(workspace)/w/[slug]/roadmap/page.tsx`): `data.initiatives` → `groups` (`name` → `label`, `health` with `ON_TRACK`/`AT_RISK`/`OFF_TRACK` → `on-track`/`at-risk`/`off-track`); `data.projects` → `items` (`startDate`/`targetDate` → `start`/`end`, the first initiative id → `groupId`, `completedCount/issueCount` → `progress` and `progressText`, `health`); `data.milestones` → `milestones` (`targetDate` → `date`, `projectId` → `itemId`, `completedAt` → `done`); `data.dependencies` → `dependencies` (`fromProjectId`/`toProjectId` → `from`/`to`). Links to projects → `onOpen`. Team and initiative filters stay in the page header; zoom and Earlier/Today/Later are built in.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'defaultZoom',
          values: ['month', 'quarter', 'year'],
          description: 'Default `quarter` (nine months visible).',
        },
        {
          prop: 'onChange',
          values: ['unset', 'set'],
          description:
            'Unset (default): read-only. Set: bars can be moved and resized as in GanttTimeline.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Health',
          description:
            'On track, at risk or off track as a badge with a dot; at risk and off track also tint the bar.',
        },
        { name: 'No health', description: 'A dash with hidden "No health reported".' },
        {
          name: 'No dates / out of view',
          description: 'Rows say "No dates", "Ended …" or "Starts …" instead of a bar.',
        },
        {
          name: 'Read-only',
          attribute: 'data-readonly',
          description: 'Bars open projects but cannot be dragged.',
        },
      ],
    },
    interaction: {
      items: [
        'Clicking a bar or pressing Enter on it calls `onOpen` with the project.',
        'Zoom, Earlier/Today/Later and the chart/table switch work as in GanttTimeline.',
      ],
    },
    keyboard: {
      items: [
        { keys: 'Tab', action: 'Moves through the toolbar and the project bars.' },
        { keys: 'ArrowUp / ArrowDown', action: 'Previous or next project bar.' },
        { keys: 'Enter / Space', action: 'Open the project.' },
      ],
      notes: ['With `onChange`, the GanttTimeline move and resize keys apply too.'],
    },
    accessibility: {
      role: 'group (chart) with buttons; table in the table view',
      name: '"Roadmap"; each bar is named with the project, its dates and progress.',
      items: [
        'Health and counts are text in the row headings.',
        'The table view lists every project with dates, duration, progress and dependencies.',
        'Inherits the GanttTimeline announcements and focus behaviour.',
      ],
    },
    responsive: {
      items: [
        'The chart scrolls within its own area with the project names pinned (20rem wide, 8.5rem below 40rem), so it fits 390px.',
        'Prefer the table view on very small screens; it scrolls sideways inside its frame.',
      ],
    },
    theme: {
      items: [
        'Semantic tokens only, through GanttTimeline and Badge: accent bars, warning and danger tints for health, muted text for progress counts.',
        'Light, dark, high-contrast and forced colours apply as in GanttTimeline.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Projects by initiative',
          stack: true,
          render: () => (
            <Roadmap
              items={PROJECTS}
              groups={GROUPS}
              today={TODAY}
              defaultStart="2026-01-01"
              dependencies={[{ id: 'd1', from: 'p4', to: 'p3' }]}
              milestones={[{ id: 'm1', label: 'Launch', date: '2026-03-20', itemId: 'p1' }]}
              onOpen={() => {}}
            />
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Show health exactly as last reported, and say when none was.',
        'Group projects by the initiative or goal they serve.',
      ],
      dont: [
        'Do not use the roadmap for day-to-day task scheduling.',
        'Do not hide undated projects; they are part of the plan.',
      ],
    },
    related: {
      items: [
        { id: 'gantt-timeline', reason: 'The editable timeline this is built on.' },
        { id: 'goal-tree', reason: 'Goals and key results behind the initiatives.' },
        { id: 'backlog-planner', reason: 'Near-term planning into iterations.' },
        { id: 'status-badge', reason: 'Status labels in lists.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: ['badge', 'gantt-timeline'],
    },
    testing: {
      unit: [
        'Groups with counts and health, rows with progress text and health, bar tones and names, undated rows; axe passes.',
        'Read-only opening; optional editing passes the roadmap item back.',
        'Roadmap and timeline translations; table view; server rendering.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
