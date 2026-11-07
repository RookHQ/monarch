import { useState } from 'react';
import { Button, Field, FieldLabel, Input } from '@rookcom/monarch-react';
import { RunningTimer } from '@rookcom/monarch-react/patterns';
import type { ComponentDocs } from '../../../internal/component-docs.js';

function PanelDemo() {
  const [startedAt, setStartedAt] = useState<number | null>(null);
  const [note, setNote] = useState('');
  return (
    <RunningTimer
      startedAt={startedAt}
      label={note || undefined}
      onStart={() => setStartedAt(Date.now())}
      onStop={() => setStartedAt(null)}
      onStopAt={() => setStartedAt(null)}
      onDiscard={() => setStartedAt(null)}
    >
      <Field>
        <FieldLabel>What are you working on?</FieldLabel>
        <Input value={note} onChange={(e) => setNote(e.target.value)} />
      </Field>
    </RunningTimer>
  );
}

function IndicatorDemo() {
  const [startedAt, setStartedAt] = useState<number | null>(() => Date.now() - 42 * 60_000);
  return (
    <div style={{ display: 'flex', gap: 8, alignItems: 'center' }}>
      <RunningTimer
        variant="indicator"
        startedAt={startedAt}
        label="DOC-14"
        description="DOC-14 Update the onboarding guide"
        onOpen={() => {}}
        onStop={() => setStartedAt(null)}
        onDiscard={() => setStartedAt(null)}
        onStopAt={() => setStartedAt(null)}
      />
      {startedAt === null ? (
        <Button size="sm" onClick={() => setStartedAt(Date.now())}>
          Start again
        </Button>
      ) : null}
    </div>
  );
}

export default {
  id: 'running-timer',
  title: 'RunningTimer',
  summary:
    'A stopwatch for time tracking: a top-bar chip or a panel with Start, Stop, "I stopped earlier" and a confirmed Discard.',
  sections: {
    purpose: {
      summary:
        'Shows a running time-tracking timer and the actions around it: start, stop and log, stop at an earlier time, and discard. The product stores the timer and creates the entry. Needed by: Stride (TimerIndicator in the top bar), People (RunningTimer on My timesheet).',
      useWhen: [
        'People track time live against a task, issue or project.',
        'A running timer must stay visible while people move around the app (the indicator).',
        'Someone may forget to stop a timer and needs to correct the end time.',
      ],
      avoidWhen: [
        'Time is entered after the fact as durations: use TimesheetGrid or DurationInput.',
        'A countdown to a deadline: use SlaIndicator.',
      ],
    },
    designPrinciples: {
      items: [
        'The clock ticks silently: `role="timer"` is never announced; only starting and stopping are, in a polite live region.',
        'Destructive and corrective actions are deliberate: Discard asks for confirmation (ConfirmDialog) and "I stopped earlier" validates the time.',
        'Data and decisions stay in the product: the timer only reports `startedAt` and calls back; promises returned from callbacks drive busy and error states.',
        'Hydration-safe: pass `now` for identical server markup; otherwise the clock starts after mount.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'The ref is the root `<div>`. Other div attributes pass through.',
        'Every callback may return a promise: the button shows a spinner until it settles and a rejected promise shows its message in an alert.',
        '`formatElapsed(seconds)` is exported for lists of running timers that must match the clock.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        'Migrating from Stride `TimerIndicator` (`components/time/TimerIndicator.tsx`): `variant="indicator"`; `timer.startedAt` → `startedAt`; `timer.issue.identifier` → `label`; the issue title → `description`; `openIssue` → `onOpen`; `stop.mutate({})` → `onStop`; `stop.mutate({ discard: true })` → `onDiscard` (now confirmed first). `useElapsed`/`fmtClock` → built in (`formatElapsed`). The query, toasts and cache invalidation stay in `useTimer`.',
        'Migrating from People `RunningTimer` (`components/timesheets/RunningTimer.tsx`): `timer.startedAt` → `startedAt`; the project, activity and note fields → `children`; Start → `onStart` (`startDisabled` for `requireProject`); Stop → `onStop`; "I stopped earlier" → `onStopAt(date)` (validation built in); the discard ConfirmDialog → `onDiscard`; `warnAfterMinutes` → `warnAfter` in milliseconds. `config.policy.allowTimer` stays in the product (do not render the timer).',
      ],
    },
    variants: {
      items: [
        {
          prop: 'variant',
          values: ['panel', 'indicator'],
          description:
            '`panel` (default): a large clock, slot for entry fields, and buttons. `indicator`: a compact chip with an open button, stop button and a menu; hidden while stopped unless `showWhenStopped`.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Stopped',
          attribute: 'no data-running',
          description:
            'Clock at 0:00:00 in muted text; Start shown. The indicator renders only its live region.',
        },
        {
          name: 'Running',
          attribute: 'data-running',
          description:
            'Clock ticks every second in the accent colour; Stop and the optional actions are shown.',
        },
        {
          name: 'Long running',
          attribute: 'data-long',
          description:
            'Past `warnAfter` (12 hours by default): a warning note (panel) or warning colours (indicator).',
        },
        {
          name: 'Busy',
          attribute: 'aria-busy on the button',
          description:
            'A callback promise is running or `pending` is set: buttons are disabled and the active one shows a spinner.',
        },
        { name: 'Error', description: 'A rejected callback shows its message in an alert.' },
      ],
    },
    interaction: {
      items: [
        'Start and Stop call `onStart` and `onStop`; the product then changes `startedAt`.',
        '"I stopped earlier" opens a dialog with a date-time field, prefilled an hour after the start (or now). The time must be at least a minute after the start and not in the future; the worked duration is shown as you type.',
        'Discard opens a danger confirmation; it is only called after confirming.',
        'In the indicator, the label opens what is timed (`onOpen`), the square stops, and the chevron opens the other actions.',
      ],
    },
    keyboard: {
      items: [
        {
          keys: 'Tab',
          action: 'Moves between the open button, stop button, menu button, fields and actions.',
        },
        { keys: 'Enter / Space', action: 'Activates the focused button.' },
        { keys: 'ArrowDown / ArrowUp', action: 'In the indicator menu: moves between actions.' },
        { keys: 'Escape', action: 'Closes the menu or dialog and returns focus to its trigger.' },
      ],
      notes: [
        'Menus and dialogs follow the DropdownMenu, Dialog and ConfirmDialog keyboard models.',
      ],
    },
    accessibility: {
      role: 'timer for the panel clock; buttons; status for announcements',
      name: 'The indicator\'s open button is named "Timer running on DOC-14, 0:42:10 elapsed. Open"; icon-only buttons have labels.',
      items: [
        'The clock is `role="timer"`, which is not live, so screen readers are not interrupted every second.',
        'A polite live region announces "Timer running" and "Timer stopped" when the state changes, including when the indicator hides.',
        'The state is also in visually hidden text next to the clock, not only in colour.',
        'The stopped-at field sits in a Field with its label, error and duration description wired to it.',
      ],
    },
    responsive: {
      items: [
        'The panel wraps: clock, fields and actions stack at 390px; fields form a grid that fits the width.',
        'Below 30rem the indicator hides the label and keeps the clock and buttons.',
        'On coarse pointers the indicator is 44px tall and its icon buttons 44px wide.',
      ],
    },
    theme: {
      items: [
        'Semantic tokens only: accent subtle/strong/border while running, warning subtle/strong/border when long running, danger for Stop.',
        'Light, dark and high-contrast themes apply automatically; forced colours draw a Highlight border round a running indicator.',
        'Reduced motion removes hover transitions.',
      ],
    },
    examples: {
      items: [
        { title: 'Panel with entry fields', stack: true, render: () => <PanelDemo /> },
        { title: 'Top-bar indicator', render: () => <IndicatorDemo /> },
      ],
    },
    doDont: {
      do: [
        'Return the save promise from `onStop` and `onStopAt` so failures are shown and buttons stay busy.',
        'Give the indicator a short `label` (an issue key) and a longer `description`.',
        'Offer `onStopAt` wherever timers can run overnight.',
      ],
      dont: [
        'Do not announce the ticking clock or put it in a live region.',
        'Do not discard without the built-in confirmation.',
        'Do not run two timers per person; the product decides what starting a second one does.',
      ],
    },
    related: {
      items: [
        { id: 'timesheet-grid', reason: 'Entering and reviewing a week of durations.' },
        { id: 'duration-input', reason: 'Typing a single duration by hand.' },
        { id: 'confirm-dialog', reason: 'The discard confirmation.' },
        { id: 'sla-indicator', reason: 'A countdown to a promised deadline.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: [
        'button',
        'confirm-dialog',
        'dialog',
        'dropdown-menu',
        'form-field',
        'text-field',
      ],
    },
    testing: {
      unit: [
        'Stopped and running panels, Start, Stop, busy and failure states; axe passes.',
        'Discard is confirmed first; "I stopped earlier" validates and returns the chosen time.',
        'Long-running warning; polite announcements of start and stop only; ticking with fake timers.',
        'Indicator: hidden while stopped, open/stop buttons, keyboard menu, plain chip; translated messages; server rendering.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
