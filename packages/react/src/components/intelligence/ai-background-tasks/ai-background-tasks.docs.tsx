import { useState } from 'react';
import { AiBackgroundTasks, type AiBackgroundTask } from '@rookcom/monarch-react/ai';
import type { ComponentDocs } from '../../../internal/component-docs.js';
// The scenario of the primary example, shared with iOS and Android (CROSS-PLATFORM.md section 8).
import fixture from './ai-background-tasks.fixture.json';

const NOW = fixture.data.now;

const TASKS = fixture.data.tasks as AiBackgroundTask[];

/** The product owns the tasks; the tray only reports what the person asked for. */
function ManagedDemo() {
  const [tasks, setTasks] = useState(TASKS);
  const [last, setLast] = useState('Nothing yet');
  const patch = (id: string, change: Partial<AiBackgroundTask>) =>
    setTasks((all) => all.map((t) => (t.id === id ? { ...t, ...change } : t)));
  return (
    <div style={{ display: 'grid', gap: 'var(--mn-space-3)' }}>
      <AiBackgroundTasks
        inline
        tasks={tasks}
        now={NOW}
        onOpen={(task) => setLast(`Open: ${task.title}`)}
        onRespond={(task) => patch(task.id, { status: 'running', step: 'Booking the first date' })}
        onCancel={(task) => patch(task.id, { status: 'cancelled', endedAt: NOW })}
        onRetry={(task) => patch(task.id, { status: 'queued', error: undefined })}
        onDismiss={(task) => setTasks((all) => all.filter((t) => t.id !== task.id))}
        onClearFinished={(finished) =>
          setTasks((all) => all.filter((t) => !finished.some((f) => f.id === t.id)))
        }
      />
      <p style={{ margin: 0, fontSize: 'var(--mn-text-xs)' }}>Last action: {last}</p>
    </div>
  );
}

export default {
  id: 'ai-background-tasks',
  title: 'AiBackgroundTasks',
  summary:
    'A tray of background agent work: running and waiting counts, grouped tasks, progress and actions.',
  sections: {
    purpose: {
      summary:
        'Keeps long agent tasks visible after the person closes the chat or moves to another page: a small button shows how many tasks are running and whether any is waiting on them, and opens a panel where each task can be opened, answered, cancelled, retried or dismissed.',
      useWhen: [
        'Agents keep working after the conversation is out of view, and the person needs one place to find that work again.',
        'Several tasks can run at once and some may stop to ask a question.',
        'A sidebar or dashboard should list the same tasks permanently: use `inline`.',
      ],
      avoidWhen: [
        'One task shown in full where it was started, with its steps: use AiTaskProgress.',
        'A reply that streams in a few seconds: use AiThinkingIndicator.',
        'File uploads: use UploadQueue. General notifications: use a notification centre.',
      ],
    },
    designPrinciples: {
      items: [
        'What needs the person comes first: "Needs you" is the top section and has its own count on the button.',
        'Status is always an icon and words; colour only supports them.',
        'Only real news is announced: a task finishing, failing or asking for input. Progress, step text and the ticking clock are silent.',
        'The tray never runs, cancels or retries anything: every action is an explicit button that calls back, and a button appears only when its callback is given.',
        'Built from existing parts: Button, Badge, Progress, Spinner and Popover.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'The ref goes to the root `<div>`; native attributes pass through. The popover panel is rendered in a portal, the live region stays in the root.',
        '`groupAiBackgroundTasks(tasks)` is the pure helper the tray uses: it returns `{ needsInput, running, finished }`, each sorted, for a product that wants its own layout or counts.',
        '`formatAiBackgroundTaskElapsed(ms, locale)` formats the elapsed text ("3 min 12 sec").',
        'Pass `now` for identical server and client rendering; without it elapsed and end times appear after mount.',
        'A task that arrives already finished is not announced; only a status change of a task the tray has seen is.',
      ],
    },
    typedProperties: { source: 'typescript' },
    variants: {
      items: [
        {
          prop: 'inline',
          values: ['false', 'true'],
          description:
            'Default: a trigger button that opens the panel in a popover. `inline` renders the panel in place, with no button, for a sidebar.',
        },
        {
          prop: 'align',
          values: ['start', 'center', 'end'],
          description: 'Which edge of the trigger the popover lines up with.',
        },
      ],
      notes: [
        'Each task shows the look of its `status`: queued, running, needs-input, succeeded, failed or cancelled.',
      ],
    },
    states: {
      items: [
        {
          name: 'Open',
          attribute: 'data-open',
          description: 'The popover is open (also `aria-expanded` on the trigger).',
        },
        {
          name: 'Busy',
          attribute: 'data-busy',
          description: 'At least one task is running.',
        },
        {
          name: 'Needs attention',
          attribute: 'data-attention',
          description: 'At least one task is waiting on the person.',
        },
        {
          name: 'Inline',
          attribute: 'data-inline',
          description: 'The panel is rendered in place.',
        },
        {
          name: 'Task status',
          attribute: 'data-status (on each task)',
          description: 'The status of that task; sets its edge and icon colour.',
        },
        {
          name: 'Indeterminate',
          description:
            'A running task with no `progress` (or with `indeterminate`) shows an animated bar without a value; Progress stops it under reduced motion.',
        },
        {
          name: 'Empty',
          description: 'With no tasks the panel says "No background tasks".',
        },
      ],
    },
    interaction: {
      items: [
        'The trigger opens and closes the popover; Escape or a click outside closes it and focus returns to the trigger.',
        'Open calls `onOpen(task)`; Respond (tasks that need input) calls `onRespond(task)`; Cancel (queued, running and waiting tasks) calls `onCancel(task)`; Retry (failed tasks) calls `onRetry(task)`; Dismiss (finished tasks) calls `onDismiss(task)`.',
        '"Clear finished" calls `onClearFinished(finished)` with the finished tasks. You update `tasks`; the tray shows what you pass.',
        'Elapsed time ticks once a second only while the panel is visible and a task is running; with `now` or under reduced motion it does not tick.',
        'When an action removes the row that had focus, focus moves to the panel instead of being lost.',
      ],
    },
    keyboard: {
      items: [
        { keys: 'Tab / Shift+Tab', action: 'Moves through the trigger and every button.' },
        {
          keys: 'Enter / Space',
          action: 'Opens the popover from the trigger; activates the focused button.',
        },
        { keys: 'Escape', action: 'Closes the popover and returns focus to the trigger.' },
      ],
    },
    accessibility: {
      role: 'A button that opens a `dialog` (popover); `group` when inline. Tasks are list items in a list per section.',
      name: 'The trigger reads its text and counts, e.g. "Background tasks 2 running 1 needs you"; the panel is named by its heading.',
      items: [
        'Counts on the trigger are text in badges, never a bare dot or colour.',
        'Each section is a heading with its count and a list named by that heading.',
        'Every task has status text beside a decorative icon; the progress bar is named "<title> progress" and reads a percentage when known.',
        'Row buttons say which task they act on ("Cancel: Summarise 40 contracts") and start with their visible text.',
        'One polite live region announces a task finishing, failing or needing input, once per change; it stays mounted while the popover is closed, so news arrives even then.',
        'Opening the popover moves focus to the named panel.',
      ],
    },
    responsive: {
      items: [
        'The popover is at most 24rem wide and never wider than the viewport; it scrolls when the list is long.',
        'Under 30rem (screen or the surrounding AI container) rows tighten and the actions share the width.',
        'Buttons grow to 44px touch targets on coarse pointers.',
        'RTL: logical properties throughout, the status edge follows the start side.',
      ],
    },
    theme: {
      items: [
        'Task edge and icon use `--mn-color-ai-*` while running, and the warning, success and danger tokens for waiting, done and failed.',
        'Forced colours use CanvasText for borders, icons and secondary text, and Highlight for the panel focus ring.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Trigger and popover',
          stage: true,
          render: () => (
            <AiBackgroundTasks
              defaultOpen
              tasks={TASKS}
              now={NOW}
              align="start"
              onOpen={() => {}}
              onRespond={() => {}}
              onCancel={() => {}}
              onRetry={() => {}}
              onDismiss={() => {}}
              onClearFinished={() => {}}
            />
          ),
        },
        {
          title: 'Inline, for a sidebar',
          stack: true,
          render: () => (
            <AiBackgroundTasks
              inline
              headingLevel={3}
              tasks={TASKS.slice(0, 3)}
              now={NOW}
              onOpen={() => {}}
              onRespond={() => {}}
              onCancel={() => {}}
            />
          ),
        },
        {
          title: 'Nothing running',
          render: () => <AiBackgroundTasks tasks={[]} align="start" />,
        },
        { title: 'Managed by the product', stack: true, render: () => <ManagedDemo /> },
      ],
    },
    doDont: {
      do: [
        'Pass `notify={useAiRunNotifications({ onInApp: toast }).notify}` and ask for notification permission from a button, so people hear about long runs in another tab.',
        'Keep the tray in the app shell, so it survives closing the chat and changing page.',
        'Confirm in `onCancel` when cancelling loses work or money already spent.',
        'Give `step`, `result` and `error` in plain language the person can act on.',
      ],
      dont: [
        'Do not ask for notification permission on page load.',
        'Do not start, retry or cancel work without the person pressing the button.',
        'Do not send a `progress` you cannot measure; leave it out and the bar is indeterminate.',
        'Do not announce progress from your own code; only status changes are worth interrupting for.',
      ],
    },
    related: {
      items: [
        { id: 'ai-task-progress', reason: 'One task in full, with its steps.' },
        { id: 'ai-agent-run-timeline', reason: 'The trace of a finished run.' },
        { id: 'ai-multi-agent-roster', reason: 'The agents themselves and what each is doing.' },
        { id: 'popover', reason: 'The floating panel underneath.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: ['button', 'badge', 'progress-bar', 'relative-time', 'spinner', 'popover'],
    },
    testing: {
      unit: [
        '`groupAiBackgroundTasks` groups, sorts and leaves its input alone; `formatAiBackgroundTaskElapsed` formats seconds, minutes and hours.',
        'Trigger name carries the running, queued and waiting counts; ref, class and props forwarding.',
        'Opens from the keyboard, focuses the named panel, closes with Escape and restores focus; controlled and uncontrolled `open`.',
        'Inline panel: headed lists per section, status text, agent, step, result, error, progress value, indeterminate bar, elapsed and end times.',
        'Each action appears only where it applies and reports its task; no buttons without callbacks; focus moves to the panel when a row is dismissed.',
        'Announces finishing, failing and needing input once, never progress, cancels or tasks that arrive finished.',
        'The clock ticks only while a visible task runs; not when closed, with `now`, or under reduced motion.',
        'Empty state, translated strings, locale and heading level.',
        '`notify` is called once when a task succeeds, fails or needs input.',
        '`useAiRunNotifications` shows a system notification only while the page is hidden and permission is granted, otherwise uses the in-app fallback; `requestPermission` is never called by itself.',
        'An action that keeps its button (Open) does not pull focus into the panel on a later update; task titles and details have `dir="auto"`.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
