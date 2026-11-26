import { AiHandoff } from '@rookcom/monarch-react/ai';
import type { ComponentDocs } from '../../../internal/component-docs.js';

const research = { id: 'research', name: 'Research agent' };
const writer = { id: 'writer', name: 'Writing agent' };
const now = new Date('2026-03-02T12:00:00Z');

export default {
  id: 'ai-handoff',
  title: 'AiHandoff',
  summary: 'An inline thread marker where work moves to another agent or to a person.',
  sections: {
    purpose: {
      summary:
        'Makes it visible, in the thread, that a different agent or a person has taken over the work, and why, so nobody wonders who is answering now.',
      useWhen: [
        'A coordinating agent routes the conversation to a specialist agent.',
        'An agent escalates to a person (an approval, a judgement call, a support colleague).',
      ],
      avoidWhen: [
        'A day change, context reset or summary: use AiContextDivider.',
        'The agent simply asks the person a question: that is a normal message.',
      ],
    },
    designPrinciples: {
      items: [
        'One line in the flow of the thread, not a message bubble: it records an event.',
        'Agents are rounded squares and people are circles, so the direction of the handoff is visible at a glance.',
        'The sentence carries the meaning; pictures and arrow are decorative.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'The ref goes to the root `<div role="note">`; native `<div>` attributes pass through.',
        '`isAiHandoffHuman(target)` tells a person from an agent.',
      ],
    },
    typedProperties: { source: 'typescript' },
    variants: {
      items: [
        {
          prop: 'to',
          values: ['AiAgentRef', "{ kind: 'human', name }"],
          description:
            'To an agent: AiAgentAvatar and an AI border. To a person: Avatar (circle) and a neutral border.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Target kind',
          attribute: 'data-to="agent" | "human"',
          description: 'Whether the work went to another agent or to a person.',
        },
      ],
    },
    interaction: { items: [], notes: ['Not interactive.'] },
    keyboard: { items: [], notes: ['Nothing to focus; the time has no tooltip here.'] },
    accessibility: {
      role: 'note',
      name: '`messages.label`, default "Handoff"',
      items: [
        'Reads as one sentence: "Research agent (AI agent) handed off to Sam Lee. Reason: … 3 minutes ago". Agent names carry a visually hidden "(AI agent)"; people do not.',
        'The sentence is a function of the two names, so translations can reorder it.',
        'Avatars and arrow are hidden from assistive technology.',
        'The time is a `<time datetime>` element.',
      ],
    },
    responsive: {
      items: [
        'The text wraps; below 30rem the decorative side rules are dropped so it fits a 390px screen.',
        'RTL: logical properties, and the arrow flips with the writing direction.',
      ],
    },
    theme: {
      items: [
        'Dashed side rules and border in `--mn-color-ai-border`; a person target uses `--mn-color-border-default`.',
        'Sentence in `--mn-color-text-default`, reason in muted text, time in subtle text.',
        'Forced colours draw rules, border and text in CanvasText.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Agent to agent',
          stack: true,
          render: () => (
            <AiHandoff
              from={research}
              to={writer}
              reason="Sources are ready to draft from"
              at={new Date('2026-03-02T11:57:00Z')}
              now={now}
            />
          ),
        },
        {
          title: 'Agent to a person',
          stack: true,
          render: () => (
            <AiHandoff
              from={writer}
              to={{ kind: 'human', name: 'Sam Lee' }}
              reason="Needs approval before sending"
              at={new Date('2026-03-02T11:40:00Z')}
              now={now}
            />
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Give a short reason whenever the handoff was the agent’s choice.',
        'Put it at the point in the thread where the new agent starts answering.',
      ],
      dont: [
        'Do not mark every internal tool call as a handoff; only changes of who is responsible.',
        'Do not present a person as an agent, or the other way round.',
      ],
    },
    related: {
      items: [
        { id: 'ai-agent-avatar', reason: 'The agent pictures.' },
        { id: 'ai-context-divider', reason: 'Other markers in a thread.' },
        { id: 'ai-multi-agent-roster', reason: 'Every agent involved and what each is doing.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: ['avatar', 'relative-time', 'ai-agent-avatar'],
    },
    testing: {
      unit: [
        'A labelled note reading "… (AI agent) handed off to … (AI agent)", the reason and the relative time; ref and props forwarding.',
        'A person target is not called an agent and uses Avatar.',
        'Translated strings, including a reordered sentence.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
