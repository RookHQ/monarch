import { useState } from 'react';
import {
  TimeOffPanel,
  type TimeOffDraft,
  type TimeOffEntry,
} from '@rookcom/monarch-react/scheduling';
import type { ComponentDocs } from '../../../internal/component-docs.js';

const NOW = new Date('2026-11-04T12:00:00Z');

const START: TimeOffEntry[] = [
  {
    id: '1',
    personId: 'p1',
    personName: 'Alex Kim',
    allDay: true,
    start: '2026-11-09',
    end: '2026-11-13',
    type: 'Annual leave',
  },
  {
    id: '2',
    personId: null,
    allDay: true,
    start: '2026-12-25',
    end: '2026-12-26',
    reason: 'Office closed',
  },
  {
    id: '3',
    personId: 'p2',
    personName: 'Sam Lee',
    allDay: false,
    start: '2026-11-05T09:00:00Z',
    end: '2026-11-05T13:00:00Z',
    reason: 'Training',
  },
  {
    id: '4',
    personId: 'p2',
    personName: 'Sam Lee',
    allDay: true,
    start: '2026-10-12',
    end: '2026-10-12',
  },
];

const PEOPLE = [
  { value: 'p1', label: 'Alex Kim' },
  { value: 'p2', label: 'Sam Lee' },
];

function Demo() {
  const [entries, setEntries] = useState<TimeOffEntry[]>(START);
  return (
    <div style={{ width: '100%', maxWidth: 640 }}>
      <TimeOffPanel
        entries={entries}
        timeZone="Europe/London"
        locale="en-GB"
        now={NOW}
        people={PEOPLE}
        allowClosure
        onAdd={(d: TimeOffDraft) =>
          new Promise<void>((resolve) =>
            setTimeout(() => {
              setEntries((list) => [
                ...list,
                {
                  ...d,
                  id: String(list.length + 1),
                  personName: PEOPLE.find((p) => p.value === d.personId)?.label,
                },
              ]);
              resolve();
            }, 500),
          )
        }
        onRemove={(e: TimeOffEntry) => setEntries((list) => list.filter((x) => x.id !== e.id))}
      />
    </div>
  );
}

export default {
  id: 'time-off-panel',
  title: 'TimeOffPanel',
  summary: 'List, add and remove time off and closures, with past entries on request.',
  sections: {
    purpose: {
      summary:
        'Manages periods when someone (or everyone) is unavailable: holidays, sickness, training, closures. Needed by: Calendar (availability time off), People (leave and holidays), Tickets (agent away periods).',
      useWhen: [
        'Settings or availability pages that block bookings or assignments for periods of time.',
      ],
      avoidWhen: [
        'Requesting leave with approval chains and balances: build that form in the product; show the result here.',
        'Seeing the whole team across a month: use TeamCalendar.',
      ],
    },
    designPrinciples: {
      items: [
        'Data in, callbacks out: `onAdd(draft)` and `onRemove(entry)` save; promises keep the form or confirmation pending.',
        'Removing always asks first (ConfirmDialog, danger tone).',
        'Times are entered in a stated zone and handed back as UTC instants; all-day periods stay as dates.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'Renders a `<section>` (ref) named by its heading.',
        'Entries are all-day (`start`/`end` days, end inclusive) or timed (instants with an offset).',
        '`people` fills “Who” (hidden for one person); `allowClosure` adds “Everyone” (`personId: null`); `types` adds a Type select.',
        '`showPast`/`onShowPastChange` (controlled) or `defaultShowPast`; hidden past entries are decided with `now`.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        'Migrating from the app copy (Calendar `availability/TimeOffPanel`): the queries and mutations stay in the product; `TimeOff` rows → `entries` (`userId` → `personId`, `user.name` → `personName`, all-day when both ends are midnight → `allDay: true` with inclusive days); `createTimeOff` → `onAdd` (the draft has `personId`, `allDay`, `start`, `end`, `reason`); `deleteTimeOff` → `onRemove`; `canDelete` → `canRemove`; the overlap warning → `notice`; workspace closure → `allowClosure`; settings zone and time format → `timeZone`/`hourCycle`.',
        'People’s leave UI keeps its request drawer and approvals; its holiday and leave lists can render through this panel with `types` and `status`.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'onAdd',
          values: ['set', 'unset'],
          description: 'Shows the Add button and dialog.',
        },
        {
          prop: 'onRemove',
          values: ['set', 'unset'],
          description: 'Shows remove buttons (filtered by `canRemove`).',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Past',
          attribute: 'data-past (item)',
          description: 'Muted name and a “Past” badge; hidden unless “Include past”.',
        },
        { name: 'Closure', description: 'Warning badge; “Everyone” as the name.' },
        { name: 'Loading', attribute: 'aria-busy', description: 'Placeholder rows.' },
        { name: 'Error', description: 'Danger alert with an optional retry.' },
        { name: 'Empty', description: 'EmptyState.' },
        { name: 'Invalid form', description: 'Inline message when the end is before the start.' },
      ],
    },
    interaction: {
      items: [
        'Add opens a dialog that starts clean each time: first person, all day, today.',
        'Changing “From” past “Until” moves “Until” with it.',
        'Remove asks for confirmation; the dialog stays pending while `onRemove` runs.',
      ],
    },
    keyboard: {
      items: [
        { keys: 'Tab', action: 'Add, Include past, each remove button.' },
        { keys: 'Space', action: 'Toggles Include past and All day.' },
        { keys: 'Escape', action: 'Closes the dialogs unless saving.' },
      ],
    },
    accessibility: {
      role: 'region (section)',
      name: 'The heading, “Time off”.',
      items: [
        'Remove buttons are named with the person and dates.',
        'The form’s fields have visible labels; errors use `role="alert"`.',
        'All strings are in `messages`.',
      ],
    },
    responsive: {
      items: [
        'Header actions wrap under the title; date fields stack below 24rem; nothing scrolls sideways at 390px.',
      ],
    },
    theme: { items: ['Card surface, Badge tones and Alert from tokens; every theme applies.'] },
    examples: {
      items: [
        { title: 'Add and remove', stack: true, render: () => <Demo /> },
        {
          title: 'Read-only, with a notice',
          stack: true,
          render: () => (
            <div style={{ width: '100%', maxWidth: 640 }}>
              <TimeOffPanel
                entries={START}
                timeZone="Europe/London"
                locale="en-GB"
                now={NOW}
                notice="2 appointments fall in these periods and were not cancelled."
              />
            </div>
          ),
        },
      ],
    },
    doDont: {
      do: ['Tell people what time off blocks (bookings, assignments) in `messages.description`.'],
      dont: [
        'Do not show private reasons to people who may not see them.',
        'Do not remove without the confirmation.',
      ],
    },
    related: {
      items: [
        { id: 'team-calendar', reason: 'The month view of the same data.' },
        { id: 'business-hours-editor', reason: 'Weekly hours and holidays.' },
        { id: 'confirm-dialog', reason: 'The removal confirmation.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: [
        'button',
        'badge',
        'alert',
        'empty-state',
        'skeleton',
        'checkbox',
        'form-field',
        'text-field',
        'confirm-dialog',
        'dialog',
        'date-picker',
        'date-time-picker',
      ],
    },
    testing: {
      unit: [
        'Upcoming list with types, closures, timed ranges and statuses; axe clean.',
        'Include past.',
        'Remove through a danger confirmation; `canRemove`.',
        'Add all day with closure, type and reason; dialog axe clean.',
        'Add timed, converted from the zone; a rejection keeps the form.',
        'Loading, error with retry and empty; renders on the server.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
