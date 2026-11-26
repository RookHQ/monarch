import { useState } from 'react';
import { Badge } from '@rookcom/monarch-react';
import {
  StageHistory,
  StageJourney,
  type JourneyStage,
  type JourneyTransition,
  type StageHistoryEntry,
} from '@rookcom/monarch-react/patterns';
import type { ComponentDocs } from '../../../internal/component-docs.js';

const NOW = Date.UTC(2026, 0, 12, 10, 0);
const DAY = 86_400_000;

const STAGES: JourneyStage[] = [
  {
    id: 'draft',
    label: 'Draft',
    group: 'Prepare',
    description: 'The application is being written.',
  },
  {
    id: 'review',
    label: 'Review',
    group: 'Prepare',
    description: 'A second person checks the draft.',
  },
  {
    id: 'filed',
    label: 'Filed',
    group: 'Prosecute',
    description: 'Submitted; waiting for the office to respond.',
  },
  {
    id: 'examined',
    label: 'Examined',
    group: 'Prosecute',
    description: 'The office has issued its report.',
  },
  { id: 'granted', label: 'Granted', group: 'Maintain', description: 'Protection is in force.' },
];

const NEXT: Record<string, JourneyTransition[]> = {
  draft: [{ id: 'review', label: 'Review' }],
  review: [
    { id: 'filed', label: 'Filed', needsInput: true },
    { id: 'draft', label: 'Draft', kind: 'back' },
  ],
  filed: [
    { id: 'examined', label: 'Examined' },
    { id: 'withdrawn', label: 'Withdrawn', kind: 'exit' },
    { id: 'review', label: 'Review', kind: 'back' },
  ],
  examined: [
    { id: 'granted', label: 'Granted' },
    { id: 'filed', label: 'Filed', kind: 'back' },
  ],
  granted: [],
};

const HISTORY: StageHistoryEntry[] = [
  {
    id: 'h3',
    from: 'Review',
    to: 'Filed',
    at: NOW - 3 * DAY,
    actor: 'Sam Lee',
    meta: '2 deadlines added',
  },
  {
    id: 'h2',
    from: 'Filed',
    to: 'Review',
    at: NOW - 5 * DAY,
    actor: 'Sam Lee',
    backward: true,
    note: 'Drawings were missing.',
  },
  { id: 'h1', to: 'Draft', at: NOW - 9 * DAY, actor: 'Automation' },
];

function JourneyDemo() {
  const [current, setCurrent] = useState('filed');
  return (
    <StageJourney
      stages={STAGES}
      current={current}
      terminal={current === 'granted' ? 'success' : undefined}
      enteredAt={NOW - 3 * DAY}
      now={NOW}
      currentMeta={<Badge tone="info">Prosecute</Badge>}
      transitions={NEXT[current] ?? []}
      onTransition={(t) => {
        if (t.id === 'withdrawn')
          return Promise.reject(new Error('Withdrawing needs a reason. Use the record menu.'));
        return new Promise<void>((resolve) =>
          setTimeout(() => {
            setCurrent(t.id);
            resolve();
          }, 400),
        );
      }}
    />
  );
}

export default {
  id: 'stage-journey',
  title: 'StageJourney',
  summary:
    'Where an item is in its pipeline or lifecycle, how long it has been there and what can happen next, with a timeline of past moves.',
  sections: {
    purpose: {
      summary:
        'Shows the stages of a pipeline grouped by phase, the current stage with its meaning and time in stage, and buttons for the moves the product allows; `StageHistory` lists every past move with who made it and why. Needed by: Intellect (JourneySection and StageHistory on a filing), Dots (client lifecycle stages), Recruit (candidate pipeline stage on an application).',
      useWhen: [
        'A record moves through a defined pipeline (filing, lifecycle, hiring) and its page should say where it is and what is next.',
        'Moves are governed by rules the server decides; the page offers only the allowed ones.',
        'People need an audit trail of stage moves with reasons.',
      ],
      avoidWhen: [
        'Many records in many stages at once: use KanbanBoard.',
        'A one-time wizard the user walks through: use Stepper.',
        'Only the stage name is needed in a list: use a Badge or StatusBadge.',
      ],
    },
    designPrinciples: {
      items: [
        'The server decides: the component offers `transitions` it is given and calls `onTransition`; forms that a move needs stay in the product.',
        'One primary move: the first forward move is the primary button; exits are quiet and the back move is labelled "Back to …".',
        'Status is in words: done and current stages carry hidden text, a terminal outcome says "Completed" or "Closed", not only colour.',
        "The history explains itself: from → to, when, who, and the reason in the mover's words.",
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        '`StageJourney` renders a `<section>` labelled by "Now: {stage}" (full) and forwards its ref and attributes. Use `variant="strip"` for the stage strip alone.',
        '`StageHistory` renders an `<ol>` of moves; give it an `aria-label` or a heading nearby.',
        'Times use RelativeTime; pass `now` for identical server and client markup.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        'Migrating from Intellect `JourneySection` (`components/intellect/filings/record/JourneySection.tsx`): `j.stages` → `stages` (`key` → `id`, `phase` → `group` with your phase label); `j.stage.key` → `current`; `j.stage.terminal` → `terminal`; `j.stage.enteredAt` → `enteredAt`; the phase ToneBadge → `currentMeta`; `j.next` → `transitions` (`undo` → `kind: "back"`, `exit` → `kind: "exit"`, `requires.some(!present)` → `needsInput`); `go(t)` → `onTransition` (MoveDialog and quickMove stay in the product); the checklist and spawn button → `children`.',
        'Migrating from Intellect `StageHistory`: `history` → `entries` (`fromLabel` → `from`, `toLabel` → `to`, `at`, `actorName` or source name → `actor`, deadlines count → `meta`, `data.backward` → `backward`, `note`).',
        'Dots `LifecycleBadge`/`LifecycleControl` (`components/crm/lifecycle.tsx`): on the client page use `variant="strip"` with the workspace stages; the inline select stays a NativeSelect fed by `allowedNext`.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'variant',
          values: ['full', 'strip'],
          description:
            '`full` (default): stage strip, current-stage card and moves. `strip`: the stage strip only, for headers and summaries.',
        },
        {
          prop: 'terminal',
          values: ['success', 'closed'],
          description:
            'The current stage ends the journey; the card and chip use success or neutral emphasis and say so.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Done stage',
          attribute: 'data-status="done"',
          description: 'Accent-subtle chip with a check and hidden "done".',
        },
        {
          name: 'Current stage',
          attribute: 'data-status="current", aria-current="step"',
          description: 'Solid accent chip with hidden "current stage".',
        },
        {
          name: 'Upcoming stage',
          attribute: 'data-status="upcoming"',
          description: 'Outlined, muted chip.',
        },
        {
          name: 'Terminal',
          attribute: 'data-terminal="success" | "closed"',
          description: 'Success or neutral emphasis on the current chip and card.',
        },
        {
          name: 'Moving',
          attribute: 'aria-busy on the button',
          description:
            'The chosen move shows a spinner; others are disabled until the promise settles.',
        },
        {
          name: 'Move failed',
          description: 'The rejection message is shown in an alert in the card.',
        },
        {
          name: 'Moved back',
          attribute: 'data-backward (history)',
          description: 'A history entry that went backwards: grey dot and "(moved back)".',
        },
      ],
    },
    interaction: {
      items: [
        'Choosing a move calls `onTransition`; while its promise runs the other moves are disabled.',
        'When `current` changes, "Moved to {stage}" is announced politely and any old error clears.',
        'The stage strip scrolls sideways on narrow screens and can be scrolled with the keyboard once focused.',
      ],
    },
    keyboard: {
      items: [
        {
          keys: 'Tab',
          action: 'Moves to the stage strip (for scrolling) and then through the move buttons.',
        },
        { keys: 'ArrowLeft / ArrowRight', action: 'With the strip focused, scrolls it.' },
        { keys: 'Enter / Space', action: 'Chooses the focused move.' },
      ],
    },
    accessibility: {
      role: 'region (section labelled "Now: {stage}"), group for the strip and the moves',
      name: 'The current-stage heading; the strip is labelled "Stages" and the moves "Next steps".',
      items: [
        'Stages are an ordered list with `aria-current="step"` on the current one and hidden done/current text.',
        'Move buttons are named "Move to {stage}", with "(asks for details)" when a form will follow.',
        'Changes are announced in a polite live region; failures in an alert.',
        'History arrows are hidden; screen readers hear "Filed to Review".',
      ],
    },
    responsive: {
      items: [
        'The strip keeps its stages on one line and scrolls inside itself at 390px; the card and buttons wrap.',
        'On coarse pointers move buttons are at least 44px tall.',
      ],
    },
    theme: {
      items: [
        'Semantic tokens only: accent solid/subtle/border for current and done, success solid/subtle/border for a successful end, bg emphasis for a closed end.',
        'Light, dark and high-contrast themes apply automatically; forced colours draw the current stage in Highlight.',
        'No motion beyond the Button spinner, which respects reduced motion.',
      ],
    },
    examples: {
      items: [
        { title: 'Journey with moves', stack: true, render: () => <JourneyDemo /> },
        {
          title: 'Strip only, closed',
          stack: true,
          render: () => (
            <StageJourney stages={STAGES} current="examined" variant="strip" terminal="closed" />
          ),
        },
        {
          title: 'Stage history',
          stack: true,
          render: () => <StageHistory aria-label="Stage history" entries={HISTORY} now={NOW} />,
        },
      ],
    },
    doDont: {
      do: [
        'Offer only moves the server will accept, and mark those that ask for details.',
        'Group stages by phase when there are more than five.',
        'Record a reason for backward moves and show it in StageHistory.',
      ],
      dont: [
        "Do not let people click stages in the strip to jump; moves go through the buttons and the product's rules.",
        'Do not use colour alone for a terminal outcome.',
      ],
    },
    related: {
      items: [
        {
          id: 'stepper',
          reason: 'A one-time sequence the user completes, such as a setup wizard.',
        },
        { id: 'kanban-board', reason: 'Many items across stages, moved by dragging.' },
        {
          id: 'activity-timeline',
          reason: 'Every kind of event on a record, not only stage moves.',
        },
        { id: 'lifecycle-badge', reason: 'A compact stage label in lists.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: ['button', 'relative-time'],
    },
    testing: {
      unit: [
        'Stages grouped by phase with done/current/upcoming states, hidden status text and time in stage; axe passes.',
        'Forward, exit and back moves with names and variants; onTransition receives the move.',
        'Busy state, failure alert, announcement and terminal outcome when `current` changes.',
        'Strip variant and translated strings; history entries (forward, backward, first stage, actor, meta, note) and empty state; server rendering.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
