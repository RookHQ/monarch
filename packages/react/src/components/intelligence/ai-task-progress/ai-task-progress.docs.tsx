import { useEffect, useState } from 'react';
import { AiTaskProgress, type AiStep, type AiTaskStatus } from '@rookcom/monarch-react/ai';
import { Button } from '@rookcom/monarch-react';
import type { ComponentDocs } from '../../../internal/component-docs.js';

const NOW = Date.UTC(2026, 0, 1, 12, 0, 0);

const LABELS = [
  'Collect the documents',
  'Summarise each document',
  'Write the overview',
  'Check the figures',
];

function stepsAt(progress: number, failAt?: number): AiStep[] {
  return LABELS.map((label, i) => ({
    id: String(i),
    label,
    state:
      failAt !== undefined && i === failAt
        ? 'failed'
        : failAt !== undefined && i > failAt
          ? 'skipped'
          : i < progress
            ? 'done'
            : i === progress
              ? 'active'
              : 'pending',
  }));
}

function LiveDemo() {
  const [step, setStep] = useState(-1);
  const [status, setStatus] = useState<AiTaskStatus>('running');
  const [notify, setNotify] = useState(false);
  const [startedAt, setStartedAt] = useState(NOW);
  useEffect(() => {
    if (step < 0 || status !== 'running') return;
    if (step >= LABELS.length) {
      setStatus('done');
      return;
    }
    const t = setTimeout(() => setStep((s) => s + 1), 1500);
    return () => clearTimeout(t);
  }, [step, status]);
  if (step < 0) {
    return (
      <Button
        size="sm"
        onClick={() => {
          setStartedAt(Date.now());
          setStatus('running');
          setStep(0);
        }}
      >
        Start the task
      </Button>
    );
  }
  return (
    <AiTaskProgress
      title="Summarising 12 documents"
      steps={stepsAt(step)}
      startedAt={startedAt}
      etaMs={Math.max(0, (LABELS.length - step) * 1500)}
      status={status}
      notifyWhenDone={notify}
      onNotifyChange={setNotify}
      onCancel={() => setStatus('cancelled')}
      onMinimize={() => setStep(-1)}
    />
  );
}

export default {
  id: 'ai-task-progress',
  title: 'AiTaskProgress',
  summary: 'A long-running agent task: status, bar, time left, steps, cancel and notify.',
  sections: {
    purpose: {
      summary:
        'Shows a task an agent runs for minutes rather than seconds: how far it is, how long is left, which step it is on, and lets the person cancel it, tuck it away or ask to be told when it is done.',
      useWhen: [
        'An agent works on a background task (a batch of documents, an import, a research run) the person may leave and come back to.',
        'You can report steps, a completion fraction or an estimate of time left.',
      ],
      avoidWhen: [
        'A reply that streams in a few seconds: use AiThinkingIndicator or AiStreamingText.',
        'Steps inside a single message with no task-level controls: use AiChainOfSteps.',
        'A file upload: use UploadQueue.',
      ],
    },
    designPrinciples: {
      items: [
        'Status is always in words beside the title, never colour alone.',
        'Only status changes are announced; the steps announce their own progress, so nothing is said twice.',
        'Controls appear only while they can act: Cancel and "Notify me when done" go away once the task ends.',
        'Built from existing parts: Progress, RelativeTime, Button and AiChainOfSteps.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'The ref goes to the root `<section>`; native attributes pass through.',
        '`progress` wins over the step count; with neither, a running task shows an indeterminate bar.',
        'Pass `now` for identical server and client rendering of the start time.',
      ],
    },
    typedProperties: { source: 'typescript' },
    variants: {
      items: [
        {
          prop: 'status',
          values: ['running', 'paused', 'done', 'failed', 'cancelled'],
          description:
            'Sets the status text, the bar tone (accent, warning, success, danger, neutral) and which controls show.',
        },
      ],
      notes: ['Controls appear only when their callbacks are given.'],
    },
    states: {
      items: [
        {
          name: 'Status',
          attribute: 'data-status',
          description: 'The current status; `aria-busy="true"` while running.',
        },
        {
          name: 'Notify on',
          attribute: 'aria-pressed (on the notify button)',
          description: '"Notify me when done" is on.',
        },
        {
          name: 'Indeterminate',
          description:
            'Running with no `progress` and no steps: the bar animates without a value (Progress handles reduced motion).',
        },
      ],
    },
    interaction: {
      items: [
        'Cancel calls `onCancel`; confirm in your handler if cancelling loses work.',
        'The bell toggle calls `onNotifyChange(!notifyWhenDone)`; you deliver the notification.',
        'Minimize calls `onMinimize`, e.g. to move the task into a tray.',
        'A finished step list folds under a summary (AiChainOfSteps); pass `stepsProps` to change that.',
      ],
    },
    keyboard: {
      items: [
        {
          keys: 'Tab',
          action: 'Moves through Minimize, the folded step summary, Notify and Cancel.',
        },
        { keys: 'Enter / Space', action: 'Activates the focused control.' },
      ],
    },
    accessibility: {
      role: 'region (a `<section>`)',
      name: 'The visible `title` heading',
      items: [
        'The progress bar is named "<title> progress" and reads "40% complete" when a value is known.',
        'Status changes are announced once ("Import: Done") through a polite live region; a failure goes through `role="alert"`. The initial status is never announced.',
        'The notify control is a toggle button with `aria-pressed`.',
        'The minimize button has an accessible name from `messages.minimize`.',
      ],
    },
    responsive: {
      items: [
        'Title, status, timing and actions wrap; at 390px the actions share the width.',
        'Actions grow to 44px touch targets on coarse pointers.',
        'RTL: logical properties throughout, the accent edge follows the start side.',
      ],
    },
    theme: {
      items: [
        'AI edge `--mn-color-ai-border` and status chip `--mn-color-ai-subtle` / `--mn-color-ai-strong` while running; success, warning and danger tokens for done, paused and failed.',
        'Forced colours use CanvasText borders and a Highlight fill on the pressed notify toggle.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Running with steps',
          stack: true,
          render: () => (
            <AiTaskProgress
              title="Summarising 12 documents"
              steps={stepsAt(1)}
              startedAt={NOW - 3 * 60_000}
              now={NOW}
              etaMs={5 * 60_000}
              status="running"
              onCancel={() => {}}
              onNotifyChange={() => {}}
            />
          ),
        },
        {
          title: 'Bar only, indeterminate',
          stack: true,
          render: () => (
            <AiTaskProgress
              title="Indexing the workspace"
              startedAt={NOW - 20_000}
              now={NOW}
              status="running"
            />
          ),
        },
        {
          title: 'Failed',
          stack: true,
          render: () => (
            <AiTaskProgress
              title="Comparing quarterly figures"
              steps={stepsAt(2, 2)}
              startedAt={NOW - 8 * 60_000}
              now={NOW}
              status="failed"
            />
          ),
        },
        { title: 'Live', stack: true, render: () => <LiveDemo /> },
      ],
    },
    doDont: {
      do: [
        'Give an estimate only when you have one; "About 4 minutes left" that keeps growing erodes trust.',
        'Let people leave: pair Minimize and Notify with a place to find the task again.',
      ],
      dont: [
        'Do not announce every step from your own code; AiChainOfSteps already does.',
        'Do not hide Cancel on a task that spends money or changes data.',
      ],
    },
    related: {
      items: [
        { id: 'ai-chain-of-steps', reason: 'The step list inside.' },
        { id: 'ai-agent-run-timeline', reason: 'The trace of a finished run.' },
        { id: 'progress-bar', reason: 'The bar on its own.' },
        { id: 'ai-multi-agent-roster', reason: 'Several agents and what each is doing.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: ['button', 'progress-bar', 'relative-time', 'ai-chain-of-steps'],
    },
    testing: {
      unit: [
        'Renders a region named by the title with status text, a named progress bar with value text, start time, time left and steps; ref and props forwarding.',
        'Uses an explicit `progress`, derives it from steps, and is indeterminate with neither.',
        'Notify toggle reports the next value with `aria-pressed`; Cancel works from the keyboard; Minimize calls back.',
        'Cancel and Notify disappear once finished.',
        'Announces status changes politely and failures assertively, never the initial status; translated strings.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
