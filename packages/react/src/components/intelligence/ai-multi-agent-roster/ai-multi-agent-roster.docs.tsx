import { useEffect, useState } from 'react';
import { Button } from '@rookcom/monarch-react';
import { AiMultiAgentRoster, type AiRosterMember } from '@rookcom/monarch-react/ai';
import type { ComponentDocs } from '../../../internal/component-docs.js';

const team: AiRosterMember[] = [
  {
    agent: { id: 'lead', name: 'Coordinator', role: 'Plans the work' },
    state: 'working',
    task: 'Splitting the request into parts',
  },
  {
    agent: { id: 'research', name: 'Research agent', role: 'Finds sources' },
    state: 'waiting',
    task: 'Needs read access to the shared drive',
  },
  {
    agent: { id: 'analyst', name: 'Analysis agent', role: 'Checks the numbers' },
    state: 'working',
    task: 'Comparing quarterly figures',
  },
  { agent: { id: 'writer', name: 'Writing agent', role: 'Drafts the summary' }, state: 'idle' },
  { agent: { id: 'review', name: 'Review agent', role: 'Checks tone' }, state: 'idle' },
];

const STAGES: AiRosterMember['state'][][] = [
  ['working', 'idle', 'idle'],
  ['idle', 'working', 'idle'],
  ['idle', 'idle', 'working'],
  ['idle', 'idle', 'idle'],
];

function LiveDemo() {
  const [stage, setStage] = useState(-1);
  useEffect(() => {
    if (stage < 0 || stage >= STAGES.length - 1) return;
    const t = setTimeout(() => setStage((s) => s + 1), 1500);
    return () => clearTimeout(t);
  }, [stage]);
  const states = STAGES[Math.max(0, stage)] ?? [];
  return (
    <div style={{ display: 'grid', gap: 'var(--mn-space-3)', justifyItems: 'start' }}>
      <Button size="sm" onClick={() => setStage(0)} disabled={stage >= 0 && stage < 3}>
        Run the team
      </Button>
      <AiMultiAgentRoster
        agents={team
          .filter((m) => ['research', 'analyst', 'writer'].includes(m.agent.id ?? ''))
          .map((m, i) => ({ agent: m.agent, state: stage < 0 ? 'idle' : (states[i] ?? 'idle') }))}
      />
    </div>
  );
}

export default {
  id: 'ai-multi-agent-roster',
  title: 'AiMultiAgentRoster',
  summary: 'Every agent in a task and what each is doing, as a list or a picture stack.',
  sections: {
    purpose: {
      summary:
        'When several agents work on one request, shows who is involved, what each is doing, and which one needs the person, so a multi-agent run is understandable at a glance.',
      useWhen: [
        'A coordinator agent delegates to specialist agents and the person should see the team.',
        'A header or toolbar needs a compact "who is working" indicator (`variant="stack"`).',
      ],
      avoidWhen: [
        'One agent working through steps: use AiChainOfSteps or AiTaskProgress.',
        'People in a workspace: use AvatarGroup.',
      ],
    },
    designPrinciples: {
      items: [
        'Agents that need attention (error, waiting) stand out with a tinted row and come first in the stack, so they are never hidden behind "+N".',
        'Every state is text as well as a ring.',
        'Changes are announced; the initial state is not.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'The ref goes to the root `<div>`; native `<div>` attributes pass through.',
        'Agents are matched by `agent.id` (or `name`) between renders to decide what to announce.',
      ],
    },
    typedProperties: { source: 'typescript' },
    variants: {
      items: [
        {
          prop: 'variant',
          values: ['list', 'stack'],
          description:
            '`list` (default): a row per agent with role, task and state. `stack`: overlapping AiAgentAvatars (up to `max`, then "+N") beside the summary.',
        },
        {
          prop: 'showSummary',
          values: ['true', 'false'],
          description: 'The "5 agents · 2 working · 1 waiting for you" line.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Agent state',
          attribute: 'data-state (on each row)',
          description:
            '`idle`, `working`, `waiting` (warning tint) and `error` (danger tint), each also as visible text.',
        },
        {
          name: 'Layout',
          attribute: 'data-variant',
          description: '`list` or `stack`.',
        },
        { name: 'Empty', description: 'No agents: shows `messages.empty`.' },
      ],
    },
    interaction: { items: [], notes: ['Not interactive; it reports status.'] },
    keyboard: { items: [], notes: ['Nothing to focus.'] },
    accessibility: {
      role: 'list',
      name: '`messages.label`, default "Agents"',
      items: [
        'List rows read "Coordinator (AI agent) Plans the work Splitting the request, Working"; the pictures are hidden because the text says the same.',
        'In the stack each picture is an image named "Research agent, AI agent, Waiting for you"; "+N" has the hidden agents and states as visually hidden text.',
        'A polite live region announces "Research agent: Error" when states change; turn it off with `announce={false}` if the page announces progress itself.',
      ],
    },
    responsive: {
      items: [
        'Names, roles and tasks wrap; the state stays on one line at the end, so it fits a 390px screen.',
        'The stack wraps its summary below the pictures when space runs out.',
        'RTL: logical properties; the stack overlaps toward the inline start.',
      ],
    },
    theme: {
      items: [
        'Waiting rows on `--mn-color-warning-subtle`, error rows on `--mn-color-danger-subtle`; state text in the matching `-strong` token, working in `--mn-color-ai-strong`.',
        'Pictures are AiAgentAvatar, with its AI tokens and rings.',
        'Forced colours outline rows that need attention and use CanvasText.',
      ],
    },
    examples: {
      items: [
        { title: 'List', stack: true, render: () => <AiMultiAgentRoster agents={team} /> },
        {
          title: 'Stack',
          render: () => <AiMultiAgentRoster agents={team} variant="stack" max={3} />,
        },
        { title: 'Live', stack: true, render: () => <LiveDemo /> },
      ],
    },
    doDont: {
      do: [
        'Give each working agent a short `task` so people know what it is doing.',
        'Use the stack in headers and the list in a side panel or details view.',
      ],
      dont: [
        'Do not change agent ids between updates, or every update is announced as new.',
        'Do not include people in the roster; it is for agents.',
      ],
    },
    related: {
      items: [
        { id: 'ai-agent-avatar', reason: 'Each agent’s picture and state ring.' },
        { id: 'ai-handoff', reason: 'Marks in the thread where work moves between agents.' },
        { id: 'ai-task-progress', reason: 'Progress of one long-running task.' },
        { id: 'avatar', reason: 'People, and AvatarGroup for several people.' },
      ],
    },
    dependencies: { packages: ['react'], components: ['ai-agent-avatar'] },
    testing: {
      unit: [
        'Labelled list with name, "(AI agent)", role, task and state text per row, summary counts; ref and props forwarding.',
        'Announces state changes, not the initial state.',
        'Stack orders agents needing attention first, names each picture and lists the hidden ones behind "+N".',
        'Empty state, hidden summary, no live region with `announce={false}`, translated strings.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
