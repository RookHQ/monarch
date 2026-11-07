import { useState } from 'react';
import { AiAgentPicker, type AiAgentPickerOption } from '@rookcom/monarch-react/ai';
import type { ComponentDocs } from '../../../internal/component-docs.js';

const AGENTS: AiAgentPickerOption[] = [
  {
    agent: { id: 'research', name: 'Research agent', role: 'Finds and summarises sources' },
    description: 'Reads documents in this space and answers with citations.',
    capabilities: ['Summarises', 'Cites sources'],
    tools: [{ name: 'Search documents' }, { name: 'Read file' }],
  },
  {
    agent: { id: 'writer', name: 'Writing agent', role: 'Drafts and edits text' },
    description: 'Drafts replies and tightens wording in the tone you choose.',
    capabilities: ['Drafts', 'Rewrites'],
    tools: [{ name: 'Insert text' }],
    state: 'working',
  },
  {
    agent: { id: 'scheduler', name: 'Scheduling agent', role: 'Plans meetings' },
    description: 'Finds times that work and drafts invitations. Not enabled in this space.',
    disabled: true,
  },
];

function Demo({ layout }: { layout?: 'list' | 'grid' }) {
  const [value, setValue] = useState<string | null>('research');
  return <AiAgentPicker agents={AGENTS} value={value} onChange={setValue} layout={layout} />;
}

export default {
  id: 'ai-agent-picker',
  title: 'AiAgentPicker',
  summary: 'Choose one AI agent from compact agent cards, as a keyboard-friendly radio group.',
  sections: {
    purpose: {
      summary:
        'Lets a person choose which AI agent to work with while seeing what each one is for, what it can do and how many tools it has, instead of picking from bare names.',
      useWhen: [
        'Starting a conversation or task with one of a few agents.',
        'Settings where one agent is the default for a space or app.',
      ],
      avoidWhen: [
        'Choosing a model rather than an agent: use AiModelPicker.',
        'Dozens of agents that need searching: use Combobox with agent names.',
        'Several agents at once: use a checkbox list.',
      ],
    },
    designPrinciples: {
      items: [
        'Choose with context: each option is a compact AiAgentCard, so the description and capabilities are visible before choosing.',
        'One tab stop: the group behaves like native radios, so it is quick to cross with Tab.',
        'Unavailable agents stay visible but disabled, so people know they exist.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'Controlled: pass `value` and update it in `onChange`. The value is `agent.id`, or `agent.name` when the agent has no id (`aiAgentKey`).',
        'The ref goes to the `radiogroup` element; native `<div>` attributes pass through, including `aria-label` and `aria-labelledby`.',
      ],
    },
    typedProperties: { source: 'typescript' },
    variants: {
      items: [
        {
          prop: 'layout',
          values: ['list', 'grid'],
          description:
            '`list` (default) stacks the cards; `grid` wraps them into columns at least 16rem wide.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Chosen',
          attribute: 'data-checked (on the option)',
          description: 'The selected agent: AI-tinted card and a check.',
        },
        {
          name: 'Disabled option',
          attribute: 'data-disabled (on the option)',
          description: 'Shown dimmed, skipped by arrow keys, not choosable.',
        },
        {
          name: 'Disabled group',
          attribute: 'data-disabled',
          description: 'Every option is disabled.',
        },
        { name: 'Empty', description: 'With no agents, `messages.empty` is shown.' },
      ],
    },
    interaction: {
      items: [
        'Clicking an option chooses it and moves focus to it.',
        'Choosing the already chosen agent does not call `onChange` again.',
      ],
    },
    keyboard: {
      items: [
        {
          keys: 'Tab',
          action:
            'Moves into the group (to the chosen agent, or the first available one) and out again.',
        },
        {
          keys: 'Arrow Down / Arrow Right',
          action: 'Moves to and chooses the next available agent, wrapping.',
        },
        {
          keys: 'Arrow Up / Arrow Left',
          action: 'Moves to and chooses the previous available agent, wrapping.',
        },
        { keys: 'Home / End', action: 'Chooses the first or last available agent.' },
        { keys: 'Space / Enter', action: 'Chooses the focused agent.' },
      ],
    },
    accessibility: {
      role: 'radiogroup with radio options',
      name: '`aria-label` / `aria-labelledby`, or `messages.label` ("Choose an agent")',
      items: [
        'Each radio is named by the agent’s name and described by its description, so the whole card is not read as the name.',
        '`aria-checked` marks the chosen agent and `aria-disabled` an unavailable one.',
        'Roving tabindex: only one option is in the tab order.',
        'The check icon is decorative; the chosen state is in `aria-checked`.',
      ],
    },
    responsive: {
      items: [
        'Cards wrap their text and the grid collapses to one column on narrow screens, so it fits 390px.',
        'Options are at least 44px tall on coarse pointers.',
        'RTL: logical properties; the check sits in the inline-end corner.',
      ],
    },
    theme: {
      items: [
        'Chosen option: `--mn-color-ai-solid` border and check on a `--mn-color-ai-subtle` card.',
        'Focus ring `--mn-color-focus-ring` at `--mn-focus-ring-width`.',
        'Forced colours: the chosen card and check use Highlight; disabled options use GrayText.',
      ],
    },
    examples: {
      items: [
        { title: 'List', stack: true, render: () => <Demo /> },
        { title: 'Grid', stack: true, render: () => <Demo layout="grid" /> },
      ],
    },
    doDont: {
      do: [
        'Give every agent a one-line description of what it is for.',
        'Keep unavailable agents visible and say why in their description.',
      ],
      dont: [
        'Do not use it for more than a handful of agents; offer search instead.',
        'Do not change the chosen agent without the person acting.',
      ],
    },
    related: {
      items: [
        { id: 'ai-agent-card', reason: 'The full card each option summarises.' },
        { id: 'ai-agent-avatar', reason: 'The agent picture inside each option.' },
        { id: 'ai-model-picker', reason: 'Choose a model rather than an agent.' },
        { id: 'combobox', reason: 'Searchable choice from many options.' },
      ],
    },
    dependencies: { packages: ['react'], components: ['ai-agent-card'] },
    testing: {
      unit: [
        'Renders a named radiogroup; radios named by agent name and described by description; aria-checked, aria-disabled and roving tabindex; ref and props forwarding.',
        'Click chooses and focuses; disabled agents and a disabled group are ignored.',
        'Arrow keys, Home and End move and choose, skipping disabled agents and wrapping; Space chooses; Tab leaves.',
        'Empty message, grid layout, translated strings, `aria-labelledby`, `aiAgentKey` fallback to name.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
