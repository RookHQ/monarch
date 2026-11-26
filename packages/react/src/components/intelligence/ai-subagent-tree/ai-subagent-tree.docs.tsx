import { useState } from 'react';
import { AiSubagentTree, type AiSubagentNode } from '@rookcom/monarch-react/ai';
import type { ComponentDocs } from '../../../internal/component-docs.js';

const RUN: AiSubagentNode[] = [
  {
    id: 'lead',
    name: 'Lead',
    role: 'Plans the quarterly report and hands out the work',
    status: 'working',
    activity: 'Waiting for two sub-agents',
    stepsDone: 2,
    stepsTotal: 5,
    startedAt: '2026-01-01T12:00:00Z',
    children: [
      {
        id: 'research',
        name: 'Researcher',
        role: 'Collect the figures for each region',
        status: 'working',
        activity: 'Reading the sales export',
        stepsDone: 3,
        stepsTotal: 7,
        toolCalls: 12,
        startedAt: '2026-01-01T12:01:00Z',
        children: [
          {
            id: 'fetch',
            name: 'Fetcher',
            role: 'Download the exports',
            status: 'done',
            toolCalls: 4,
            startedAt: '2026-01-01T12:01:00Z',
            endedAt: '2026-01-01T12:03:00Z',
            summary: 'Downloaded 4 exports',
          },
          { id: 'parse', name: 'Parser', role: 'Turn the exports into tables', status: 'waiting' },
        ],
      },
      {
        id: 'write',
        name: 'Writer',
        role: 'Draft the summary',
        status: 'needs-input',
        startedAt: '2026-01-01T12:02:00Z',
      },
      {
        id: 'check',
        name: 'Checker',
        role: 'Compare the totals with last quarter',
        status: 'done',
        stepsDone: 4,
        stepsTotal: 4,
        startedAt: '2026-01-01T12:01:00Z',
        endedAt: '2026-01-01T12:04:00Z',
        summary: 'All totals match',
      },
    ],
  },
];

const ENDED: AiSubagentNode[] = [
  {
    id: 'lead',
    name: 'Lead',
    status: 'failed',
    summary: 'Stopped because the import could not finish',
    startedAt: '2026-01-01T12:00:00Z',
    endedAt: '2026-01-01T12:06:00Z',
    children: [
      {
        id: 'import',
        name: 'Importer',
        status: 'failed',
        summary: 'The file has no header row',
        toolCalls: 3,
      },
      { id: 'tidy', name: 'Tidier', status: 'stopped', summary: 'Stopped by you' },
    ],
  },
];

/** Replaces one agent's fields anywhere in the tree. */
function patch(
  agents: readonly AiSubagentNode[],
  id: string,
  change: Partial<AiSubagentNode>,
): AiSubagentNode[] {
  return agents.map((a) => ({
    ...a,
    ...(a.id === id ? change : {}),
    ...(a.children ? { children: patch(a.children, id, change) } : {}),
  }));
}

function SteerDemo() {
  const [agents, setAgents] = useState(RUN);
  const [selectedId, setSelectedId] = useState<string | null>('write');
  const [opened, setOpened] = useState('');
  return (
    <div>
      <AiSubagentTree
        agents={agents}
        locale="en-GB"
        timeZone="UTC"
        selectedId={selectedId}
        onSelect={setSelectedId}
        onStop={(id) =>
          setAgents((a) => patch(a, id, { status: 'stopped', summary: 'Stopped by you' }))
        }
        onRespond={(id) =>
          setAgents((a) => patch(a, id, { status: 'working', activity: 'Drafting the summary' }))
        }
        onOpen={(_, agent) => setOpened(agent.name)}
      />
      {opened ? <p>Your app would now open the transcript of {opened}.</p> : null}
    </div>
  );
}

export default {
  id: 'ai-subagent-tree',
  title: 'AiSubagentTree',
  summary: 'A lead agent and the sub-agents it handed work to, as a tree with status and actions.',
  sections: {
    purpose: {
      summary:
        'Shows a multi-agent run as a tree: the lead agent, the sub-agents it handed work to, what each is doing, how far it is and what it produced, so a person can follow the run and stop, open or answer any agent.',
      useWhen: [
        'An agent delegates work to other agents and the person should see who is doing what.',
        'A person needs to stop one branch of a run, or answer the one agent that is waiting for them, without stopping everything.',
        'Sub-agents hand work on to further sub-agents, to any depth.',
      ],
      avoidWhen: [
        'The agents work side by side with no hand-offs between them: use AiMultiAgentRoster.',
        'One agent works through a list of steps: use AiTaskProgress or AiChainOfSteps.',
        'You are showing the timed trace of a finished run: use AiAgentRunTimeline.',
        'The hierarchy is not agents (folders, departments): use TreeView.',
      ],
    },
    designPrinciples: {
      items: [
        'Status is always an icon and a word, never colour alone.',
        'Nothing is stopped, opened or answered until the person presses a button; Stop is its own explicit button and shows only while the agent can still be stopped.',
        'Whatever is active is in view: an agent that is working or needs input, and every agent above it, is open by default.',
        'The component holds no run state and never fetches: agents come in through props, actions go out through callbacks.',
        'Built on TreeView, so the keyboard model is the same as every other Monarch tree.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'The ref goes to the root `<div>`; native attributes pass through.',
        '`flattenSubagentTree(agents)` returns every agent in reading order with its level, parent, position and set size; `countSubagentStatuses(agents)` returns the count per status and the total; `defaultExpandedSubagentIds(agents)` returns the ids open by default.',
        'Uncontrolled and without `defaultExpandedIds`, an agent that becomes active later is opened once; if the person closes it, it stays closed. With `expandedIds` the product owns expansion entirely.',
        '`onSelect`, `onStop`, `onOpen` and `onRespond` receive the id and the agent object.',
        '`partId` and `messageId` on an agent link it to a conversation part (for a hand-off made through a tool call, its `toolCallId`). They are written on the row content as `data-part-id` and `data-message-id`; with `onOpenPart` a selected, linked agent gets a "Show in conversation" button that hands both ids to the app.',
        '`aiTraceToSubagentTree(spans, { runStartedAt })` from AiTraceView builds these agents from trace spans, for example from `otelSpansToAiTrace`.',
        'Names are isolated with `<bdi>`, and roles, activity and results use `dir="auto"`, so right-to-left text written by a model reads correctly in a left-to-right page.',
        'Pass `locale` and `timeZone` for identical server and client rendering of numbers and times.',
        'Tree items are rendered flat, each with `aria-level`, `aria-setsize` and `aria-posinset` (the form of the tree pattern TreeView uses), so there are no `role="group"` wrappers.',
      ],
    },
    typedProperties: { source: 'typescript' },
    variants: {
      items: [],
      notes: [
        'No variant props. What an agent row shows follows from its `status` and the optional fields it has.',
      ],
    },
    states: {
      items: [
        {
          name: 'Agent status',
          attribute: 'data-status (on the row content and the status chip)',
          description:
            'waiting, working, needs-input, done, failed or stopped. Sets the icon, the word and the chip tone.',
        },
        {
          name: 'Selected',
          attribute: 'aria-selected / data-selected (on the tree item)',
          description: 'The agent whose actions show under the tree.',
        },
        {
          name: 'Expanded',
          attribute: 'aria-expanded (on tree items with sub-agents)',
          description: 'Whether the agent’s sub-agents are shown.',
        },
        {
          name: 'Empty',
          attribute: 'data-empty (on the root)',
          description: 'No agents: the header reads "No agents yet" and no tree is rendered.',
        },
      ],
    },
    interaction: {
      items: [
        'Clicking a row, or Enter or Space on it, selects the agent and calls `onSelect(id, agent)`. Moving focus with the arrow keys does not select.',
        'Clicking the chevron expands or collapses an agent without selecting it.',
        'The selected agent gets an action bar under the tree: Respond (only when it needs input), Open, and Stop (only while waiting or working). Each shows only when its callback is given.',
        'After Stop, focus returns to the agent’s row, so it is not lost when the Stop button goes away.',
        'The header line counts the whole tree, closed branches included.',
      ],
    },
    keyboard: {
      items: [
        {
          keys: 'Tab',
          action: 'Enters the tree at one item, then moves to the action bar, then out.',
        },
        { keys: 'Up / Down', action: 'Moves to the previous or next visible agent.' },
        {
          keys: 'Right',
          action: 'Expands a closed agent; on an open one, moves to its first sub-agent.',
        },
        {
          keys: 'Left',
          action: 'Collapses an open agent; otherwise moves to the agent that handed it the work.',
        },
        { keys: 'Home / End', action: 'Moves to the first or last visible agent.' },
        {
          keys: 'Letters',
          action: 'Moves to the next agent whose name starts with what was typed.',
        },
        { keys: '*', action: 'Expands every sibling of the focused agent.' },
        { keys: 'Enter / Space', action: 'Selects the focused agent; activates a focused button.' },
      ],
    },
    accessibility: {
      role: 'tree with treeitem rows (WAI-ARIA tree view pattern), inside a plain root `<div>`',
      name: '`label`, default "Agents" (`messages.label`)',
      items: [
        'Every row carries `aria-level`, `aria-setsize`, `aria-posinset`, `aria-selected` and, with sub-agents, `aria-expanded`. One row is in the tab order (roving tabindex).',
        'A row is named by everything it shows: name, status word, role, activity, progress, times and result.',
        'Status icons are decorative; the word beside each carries the status.',
        'Status changes are announced once each through a polite live region ("Researcher: Done"); the first render is never announced. An agent that joins already needing input or failed is announced too.',
        'The action bar is a `role="group"` named "Actions for <name>" and holds real buttons that follow the tree in the tab order.',
        'The lines between levels are CSS only and carry no meaning.',
      ],
    },
    responsive: {
      items: [
        'Names, status chips, roles and summaries wrap; nothing scrolls sideways at 390px.',
        'Below 30rem (screen or `mn-ai` container) the action buttons share the full width and the indent per level shrinks.',
        'Rows and action buttons are 44px touch targets on coarse pointers.',
        'RTL: logical properties throughout; indentation and guide lines follow the start side.',
      ],
    },
    theme: {
      items: [
        'Status chips: `--mn-color-ai-subtle` / `--mn-color-ai-strong` while working, warning tokens when input is needed, success for done, danger for failed, `--mn-color-bg-muted` for waiting and stopped.',
        'Selection, focus ring and guide lines come from TreeView’s tokens.',
        'Forced colours: chips become bordered CanvasText on Canvas, the selected row gets a Highlight outline and guide lines use CanvasText.',
        'The working spinner is static under reduced motion (Spinner handles it).',
      ],
    },
    examples: {
      items: [
        {
          title: 'A run in progress',
          stack: true,
          render: () => <AiSubagentTree agents={RUN} locale="en-GB" timeZone="UTC" />,
        },
        {
          title: 'Select, stop, open and respond',
          stack: true,
          render: () => <SteerDemo />,
        },
        {
          title: 'A run that ended',
          stack: true,
          render: () => (
            <AiSubagentTree
              agents={ENDED}
              locale="en-GB"
              timeZone="UTC"
              defaultExpandedIds={['lead']}
              defaultSelectedId="import"
              onOpen={() => {}}
            />
          ),
        },
        {
          title: 'No agents yet',
          stack: true,
          render: () => <AiSubagentTree agents={[]} />,
        },
      ],
    },
    doDont: {
      do: [
        'Keep ids stable for the whole run, so selection, expansion and announcements follow the same agent.',
        'Write `activity` as a short present-tense phrase and `summary` as the outcome in one line.',
        'Confirm in your `onStop` handler when stopping loses work that cannot be resumed.',
      ],
      dont: [
        'Do not stop, answer or open anything from `onSelect`; selecting only shows the actions.',
        'Do not put secrets, raw prompts or personal data in `activity` or `summary`.',
        'Do not announce status changes from your own code as well; the tree already does, once each.',
      ],
    },
    related: {
      items: [
        { id: 'tree-view', reason: 'The tree this is built on, for any other hierarchy.' },
        { id: 'ai-multi-agent-roster', reason: 'Agents side by side, without hand-offs.' },
        { id: 'ai-task-progress', reason: 'One long task with steps and a bar.' },
        { id: 'ai-agent-run-timeline', reason: 'The timed trace of a run.' },
        { id: 'ai-handoff', reason: 'Handing a conversation to another agent or a person.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: ['button', 'tree-view', 'spinner'],
    },
    testing: {
      unit: [
        'Renders a named tree with level, set size, position and expanded state on every row, plus the run summary; ref and props forwarding.',
        'Rows show the status word, activity only while working, "3 of 7 steps", tool calls, times and the result once ended.',
        'Default expansion of active branches; a branch that becomes active opens once and stays closed after the person closes it; controlled `expandedIds` and `defaultExpandedIds`.',
        'Full keyboard model: one tab stop, Up/Down, Home/End, Right and Left to expand, collapse and move between parent and child, typeahead, Enter and Space to select.',
        'Controlled and uncontrolled selection with `onSelect`.',
        'Stop, Open and Respond appear only when they apply, work from the keyboard, and Stop returns focus to the row.',
        'Status changes are announced once, politely, never on first render; helpers; translated strings and locale numbers; empty state.',
        'Conversation links: `data-part-id` and `data-message-id`, and "Show in conversation" calling `onOpenPart` only for a linked agent.',
        'Names use `<bdi>`; roles, activity and results have `dir="auto"`.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
