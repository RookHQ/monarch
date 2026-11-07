import { useState } from 'react';
import { Badge } from '@rookcom/monarch-react';
import { TodayPanel, type TodayTask } from '@rookcom/monarch-react/patterns';
import type { ComponentDocs } from '../../../internal/component-docs.js';

const today = '2026-11-07';
const events = [
  {
    id: 'e1',
    title: 'Weekly review',
    start: '2026-11-07T09:30:00Z',
    end: '2026-11-07T10:00:00Z',
    detail: 'Room 2',
  },
  {
    id: 'e2',
    title: 'Interview: product designer',
    start: '2026-11-07T13:00:00Z',
    end: '2026-11-07T14:00:00Z',
  },
];
const initialTasks: TodayTask[] = [
  { id: 't1', title: 'Send the quarterly proposal', detail: 'Northwind account', href: '#t1' },
  { id: 't2', title: 'Approve 3 time-off requests', overdue: true, due: '2 days late' },
  {
    id: 't3',
    title: 'Review the release notes',
    meta: (
      <Badge tone="info" size="sm">
        High
      </Badge>
    ),
  },
];

function ToggleDemo() {
  const [tasks, setTasks] = useState(initialTasks);
  return (
    <TodayPanel
      today={today}
      timeZone="UTC"
      events={events}
      tasks={tasks}
      onTaskToggle={(id, done) =>
        setTasks((list) => list.map((t) => (t.id === id ? { ...t, done } : t)))
      }
      maxTasks={5}
      tasksHref="#my-work"
    />
  );
}

export default {
  id: 'today-panel',
  title: 'TodayPanel',
  summary: 'Today’s agenda beside the tasks due today, overdue first, with optional checkboxes.',
  sections: {
    purpose: {
      summary:
        'Answers "what does today hold for me?" on a personal home page: today’s meetings as an agenda next to the tasks due today, overdue ones first, ticked off in place. Needed by: Stride (my-work), People (home), Recruit (home), Tickets, Dots.',
      useWhen: [
        'A personal home page should open on the person’s day.',
        'Tasks and meetings come from the app and should sit side by side.',
      ],
      avoidWhen: [
        'A multi-day schedule: use AgendaList or a calendar view.',
        'Approvals with approve and reject buttons: use ApprovalQueue.',
        'Insights about the business: use InsightStrip.',
      ],
    },
    designPrinciples: {
      items: [
        'Overdue first, and said in words ("Overdue"), not only in red.',
        'One calm empty state when the day is clear, instead of two empty boxes.',
        'Ticking a task off is a checkbox the app controls; nothing disappears by itself.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'Events are CalendarEvents rendered by AgendaList for `today`; leave `events` out for a tasks-only panel, `tasks` out for an agenda-only panel.',
        'Renders a `<section>` named by its heading; native attributes pass through and the ref is forwarded.',
        '`TODAY_PANEL_MESSAGES` holds the English defaults.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        'Migrating from the app copy: Recruit "Needs you today" (`components/home/HiringHome.tsx` `NeedsYou`/`buildRows`) rows → `tasks` with `lead` (the interview time), `title`, `detail` and `href`; its `Loadable` status → `loading`, `error` + `onRetry`; "Nothing is waiting on you." → `empty`. Stride "Due this week" (`components/home/MyWorkHome.tsx` `dueLabel`) → `tasks` with `overdue` and `due` text. People home `TodayCard` attendance stays in People; its leave and meetings feed `events`.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'headingLevel',
          values: ['2', '3', '4'],
          description: 'Level of the panel heading; section headings are one below.',
        },
        {
          prop: 'showDate',
          values: ['true', 'false'],
          description: 'Today’s date beside the heading.',
        },
      ],
    },
    states: {
      items: [
        { name: 'Ready', description: 'Agenda and tasks side by side (stacked when narrow).' },
        {
          name: 'Overdue task',
          attribute: 'data-overdue on the task',
          description: 'Listed first with an "Overdue" tag.',
        },
        {
          name: 'Done task',
          attribute: 'data-done on the task',
          description: 'Ticked and struck through, still listed.',
        },
        {
          name: 'Empty',
          attribute: 'data-state="empty"',
          description: 'Nothing scheduled and nothing due: one success line.',
        },
        {
          name: 'Loading / Error',
          attribute: 'data-state',
          description: 'Placeholder lines with `aria-busy`, or the error with Try again.',
        },
      ],
    },
    interaction: {
      items: [
        'Ticking a checkbox calls `onTaskToggle(id, done)`; the app updates `done`.',
        'Task titles with `href` are links (`linkComponent` for a router).',
        'Event chips call `onEventClick` or follow their `href`.',
        'With `maxTasks` and `tasksHref`, "N more" links to the full list.',
      ],
    },
    keyboard: {
      items: [
        {
          keys: 'Tab / Shift+Tab',
          action: 'Moves through event chips, checkboxes, task links and the more link.',
        },
        { keys: 'Space', action: 'Ticks or clears the focused checkbox.' },
        { keys: 'Enter', action: 'Opens the focused link or event.' },
      ],
    },
    accessibility: {
      role: 'region',
      name: 'The heading, default "Today"',
      items: [
        'Schedule and Due today are sub-sections with their own headings.',
        'Each checkbox is named "Mark "{title}" done".',
        'Overdue is a text tag, not colour alone.',
        'The date is a `<time>` element with the ISO date.',
      ],
    },
    responsive: {
      items: [
        'Agenda and tasks sit side by side from 44rem of panel width and stack below it.',
        'Below 24rem the due tags drop under the task title; nothing scrolls sideways at 390px.',
        'Task rows and links are at least 44px tall on coarse pointers.',
      ],
    },
    theme: {
      items: [
        'Rules `--mn-color-border-subtle`, titles `--mn-color-text-default`, detail `--mn-color-text-muted`; the overdue tag is a danger Badge.',
        'In forced colours rules are CanvasText and links LinkText.',
      ],
    },
    examples: {
      items: [
        { title: 'Agenda and tasks with checkboxes', stack: true, render: () => <ToggleDemo /> },
        {
          title: 'Tasks only, as a "Needs you today" list',
          stack: true,
          render: () => (
            <TodayPanel
              today={today}
              title="Needs you today"
              showDate={false}
              tasks={[
                {
                  id: 'a',
                  lead: '10:30',
                  title: 'Jordan Lee',
                  detail: 'Video interview · Product designer',
                  href: '#a',
                },
                {
                  id: 'b',
                  title: '2 candidates waiting on a decision',
                  detail: 'Interviewed, nothing else booked',
                  href: '#b',
                },
              ]}
            />
          ),
        },
        {
          title: 'A clear day',
          stack: true,
          render: () => <TodayPanel today={today} events={[]} tasks={[]} />,
        },
      ],
    },
    doDont: {
      do: [
        'Pass `today` and `timeZone` from the workspace when they differ from the device.',
        'Keep task titles short and specific.',
        'Link each task to where it is done.',
      ],
      dont: [
        'Do not hide done tasks the moment they are ticked; let the app decide when they leave.',
        'Do not list next week’s work here; link to it with `tasksHref`.',
      ],
    },
    related: {
      items: [
        { id: 'agenda-list', reason: 'A multi-day agenda.' },
        { id: 'approval-queue', reason: 'Requests to approve or reject.' },
        { id: 'widget', reason: 'Frame the panel with a menu and view-all link.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: [
        'agenda-list',
        'badge',
        'checkbox',
        'empty-state',
        'inline-error-retry',
        'skeleton',
      ],
    },
    testing: {
      unit: [
        'Region with today’s date, agenda and task sections; props and ref forwarded.',
        'Overdue tasks first with a text tag; task links.',
        'Named checkboxes call onTaskToggle from the keyboard.',
        'maxTasks with a more link; single empty state and per-section empty text.',
        'Loading, error with retry, title, heading levels and messages.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
