import { useState } from 'react';
import { AiActionLog, type AiActionLogEntry } from '@rookcom/monarch-react/ai';
import type { ComponentDocs } from '../../../internal/component-docs.js';

const NOW = Date.UTC(2026, 0, 15, 12, 0);
const RESEARCH = { id: 'research', name: 'Research agent' };
const SCHEDULER = { id: 'scheduler', name: 'Scheduling agent' };

const ENTRIES: AiActionLogEntry[] = [
  {
    id: '1',
    at: NOW - 4 * 60_000,
    agent: RESEARCH,
    action: 'added a summary to',
    target: 'Quarterly report',
    href: '#quarterly-report',
  },
  {
    id: '2',
    at: NOW - 50 * 60_000,
    agent: SCHEDULER,
    action: 'moved the team check-in to Friday',
  },
  {
    id: '3',
    at: NOW - 3 * 3_600_000,
    agent: RESEARCH,
    action: 'tagged',
    target: '12 records',
    undone: true,
  },
];

function UndoDemo() {
  const [entries] = useState<AiActionLogEntry[]>(() =>
    ENTRIES.map((e, i) =>
      i < 2
        ? {
            ...e,
            undoable: true,
            onUndo: () => new Promise<void>((resolve) => setTimeout(resolve, 800)),
          }
        : e,
    ),
  );
  return <AiActionLog entries={entries} now={NOW} />;
}

export default {
  id: 'ai-action-log',
  title: 'AiActionLog',
  summary: 'An audit list of what AI agents did, with the agent marked as the actor and Undo.',
  sections: {
    purpose: {
      summary:
        'Shows what AI agents changed, when and where, so people can review and reverse an agent’s work and never confuse it with something a person did.',
      useWhen: [
        'An agent acts on records, documents or schedules and people need a record of it.',
        'An agent settings page or an assistant panel shows recent agent activity with Undo.',
      ],
      avoidWhen: [
        'Activity by people, or mixed people and automations: use ActivityTimeline.',
        'Undo right after one AI edit: use AiUndoBanner.',
        'The steps of a run in progress: use AiChainOfSteps or AiTaskProgress.',
      ],
    },
    designPrinciples: {
      items: [
        'The actor is always visibly an AI agent: an agent avatar (rounded square with a Bot badge) and an "AI agent" tag.',
        'Each entry reads as a sentence: agent, action, target, then when.',
        'Reversible actions offer Undo in place; undone actions stay in the log, struck through and marked "Undone".',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'The ref goes to the root `<div>`; native `<div>` attributes pass through.',
        'Undo shows only when an entry has both `undoable` and `onUndo` and is not `undone`. A successful undo is remembered inside the component; also set `undone` in your data so it survives a reload.',
      ],
    },
    typedProperties: { source: 'typescript' },
    variants: { items: [], notes: ['One layout; entries are shown in the order given.'] },
    states: {
      items: [
        {
          name: 'Undo pending',
          description:
            'While the promise from `onUndo` is pending the button shows a spinner and `aria-busy`; further clicks are ignored.',
        },
        {
          name: 'Undone',
          attribute: 'data-undone (on the entry)',
          description: 'The action and target are struck through and "Undone" follows the time.',
        },
        {
          name: 'Undo failed',
          description: 'The rejection is announced through an alert and the Undo button stays.',
        },
        { name: 'Empty', description: 'No entries: shows "No AI activity yet."' },
      ],
    },
    interaction: {
      items: [
        'Target links open the target; unsafe URLs are dropped and the target shows as text.',
        'Undo calls `onUndo`; when it resolves the button is removed and focus moves to the entry so it is not lost.',
      ],
    },
    keyboard: {
      items: [
        { keys: 'Tab', action: 'Moves through target links and Undo buttons.' },
        { keys: 'Enter / Space', action: 'Activates the focused link or Undo button.' },
      ],
    },
    accessibility: {
      role: 'list (an `<ol>`)',
      name: '`messages.label`, default "AI activity"',
      items: [
        'Each entry reads "Research agent (AI agent) added a summary to Quarterly report, 4 minutes ago".',
        'The avatar is decorative because the agent’s name is in the text.',
        'Undo buttons are named per entry ("Undo: Research agent added a summary to Quarterly report") so they are distinct in a list of buttons.',
        'A successful undo is announced politely; a failure assertively.',
      ],
    },
    responsive: {
      items: [
        'Text wraps and the Undo button stays beside it; fits a 390px screen without horizontal scroll.',
        'Undo uses Button, which grows to a 44px touch target on coarse pointers.',
        'RTL: logical properties throughout.',
      ],
    },
    theme: {
      items: [
        'The "AI agent" tag uses `--mn-color-ai-subtle`, `--mn-color-ai-border` and `--mn-color-ai-strong`.',
        'Entries are divided by `--mn-color-border-subtle`; times are `--mn-color-text-muted`.',
        'Forced colours: CanvasText borders and a LinkText target link.',
      ],
    },
    examples: {
      items: [
        { title: 'Log', stack: true, render: () => <AiActionLog entries={ENTRIES} now={NOW} /> },
        { title: 'With Undo', stack: true, render: () => <UndoDemo /> },
        { title: 'Empty', stack: true, render: () => <AiActionLog entries={[]} /> },
      ],
    },
    doDont: {
      do: [
        'Write `action` as a verb phrase that reads after the agent’s name ("moved the team check-in to Friday").',
        'Persist `undone` once an undo succeeds.',
      ],
      dont: [
        'Do not mix people’s actions into this log; the actor is always an agent.',
        'Do not offer Undo for actions that cannot be fully reversed.',
      ],
    },
    related: {
      items: [
        { id: 'activity-timeline', reason: 'Activity by people and automations.' },
        { id: 'ai-undo-banner', reason: 'Undo right after a single AI edit.' },
        { id: 'ai-agent-avatar', reason: 'The agent picture used for each actor.' },
        { id: 'ai-permission-scope', reason: 'What the agents are allowed to do.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: ['button', 'relative-time', 'ai-agent-avatar'],
    },
    testing: {
      unit: [
        'Lists entries as sentences with an "AI agent" actor tag, target link and relative time; ref and props forwarding.',
        'Drops unsafe target links.',
        'Undo by keyboard: pending state ignores repeat clicks, success removes the button, keeps focus on the entry and announces it.',
        'A failed undo is announced in an alert; no Undo for undone or non-undoable entries.',
        'Empty state and translated strings.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
