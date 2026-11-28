import { Avatar, Button } from '@rookcom/monarch-react';
import { AiAgentCard } from '@rookcom/monarch-react/ai';
import type { ComponentDocs } from '../../../internal/component-docs.js';
// The scenario of the primary example, shared with iOS and Android (CROSS-PLATFORM.md section 8).
import fixture from './ai-agent-card.fixture.json';

const research = fixture.data.agent;

export default {
  id: 'ai-agent-card',
  title: 'AiAgentCard',
  summary: 'What an AI agent is, what it can do, which tools it calls and what data it sees.',
  sections: {
    purpose: {
      summary:
        'Tells people what an AI agent is before they rely on it: its role, what it can do, the tools it may call, the data it can read and who is responsible for it. A compact variant summarises an agent in lists and pickers.',
      useWhen: [
        'An agent directory, an "About this agent" panel or the confirmation before enabling an agent.',
        'A summary row of an agent in a list (`variant="compact"`).',
      ],
      avoidWhen: [
        'Choosing one agent from several: use AiAgentPicker, which renders compact cards as a radio group.',
        'Just the agent’s picture next to a message: use AiAgentAvatar.',
        'Editing what an agent may access: use AiPermissionScope.',
      ],
    },
    designPrinciples: {
      items: [
        'Be explicit about reach: tools and data access are listed in plain words, not hidden behind a link.',
        'Never mistaken for a person: rounded-square AiAgentAvatar with a Bot badge, and "AI agent" after the role.',
        'Empty sections are left out rather than shown as "None".',
        'The compact variant keeps the same order (avatar, name, role, description, capabilities) so it scans like the full card.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'The ref goes to the root element (`<article>` by default, `<div>` with `as="div"`); native attributes pass through.',
        '`nameId` and `descriptionId` let a surrounding control (such as AiAgentPicker’s radios) label itself from the card.',
      ],
    },
    typedProperties: { source: 'typescript' },
    variants: {
      items: [
        {
          prop: 'variant',
          values: ['full', 'compact'],
          description:
            '`full` shows description, capabilities, tools, data access, owner and actions. `compact` shows avatar, name, role, a two-line description, capability tags and a tool count.',
        },
        {
          prop: 'headingLevel',
          values: ['2', '3', '4', '5', '6', 'none'],
          description: 'Heading level of the name (default 3); `none` renders a plain line.',
        },
        {
          prop: 'as',
          values: ['article', 'div'],
          description:
            '`article` (default) is named by the agent’s name; use `div` inside a control.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Agent state',
          attribute: 'data-state',
          description:
            '`idle`, `working`, `waiting` or `error`, shown on the avatar as a ring and in its accessible name.',
        },
        { name: 'Variant', attribute: 'data-variant', description: '`full` or `compact`.' },
      ],
    },
    interaction: {
      items: [
        'Not interactive itself; anything passed in `actions` or `owner` keeps its own behaviour.',
      ],
    },
    keyboard: {
      items: [],
      notes: [
        'Nothing to focus in the card itself; Tab reaches the buttons or links in `actions`.',
      ],
    },
    accessibility: {
      role: 'article (or none with `as="div"`)',
      name: 'The agent’s name, through `aria-labelledby` on the heading',
      items: [
        'The name is a heading (level 3 by default) so the card can be found by heading navigation.',
        'Capabilities, tools and data access are lists named by their visible titles ("What it can do", "Tools", "Data it can see").',
        'The owner is a description list ("Owner" / name).',
        'The avatar is decorative because the name is visible; a non-idle state is still read.',
      ],
    },
    responsive: {
      items: [
        'Fills its container; every text wraps, so it fits a 390px screen without horizontal scroll.',
        'The compact description clamps to two lines.',
        'RTL: logical properties throughout; tool markers sit on the inline-start side.',
      ],
    },
    theme: {
      items: [
        'Surface `--mn-color-bg-surface` with a `--mn-color-border-subtle` border and `--mn-radius-lg`.',
        'Capability tags use the AI intent: `--mn-color-ai-subtle`, `--mn-color-ai-border`, `--mn-color-ai-strong`.',
        'Forced colours: borders and secondary text become CanvasText.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Full card',
          stack: true,
          render: () => (
            <AiAgentCard
              agent={research}
              description={fixture.data.description}
              capabilities={fixture.data.capabilities}
              tools={fixture.data.tools}
              dataAccess={fixture.data.dataAccess}
              owner={
                <>
                  <Avatar name={fixture.data.owner} size="xs" decorative />
                  {fixture.data.owner}
                </>
              }
              actions={<Button size="sm">{fixture.data.action.label}</Button>}
            />
          ),
        },
        {
          title: 'Compact, working',
          stack: true,
          render: () => (
            <AiAgentCard
              variant="compact"
              state="working"
              agent={{ id: 'writer', name: 'Writing agent', role: 'Drafts and edits text' }}
              description="Drafts replies and tightens wording in the tone you choose."
              capabilities={['Drafts', 'Rewrites']}
              tools={[{ name: 'Insert text' }, { name: 'Read selection' }]}
            />
          ),
        },
      ],
    },
    doDont: {
      do: [
        'List every tool and data source the agent can actually use.',
        'Name a responsible owner for agents shared with a team.',
      ],
      dont: [
        'Do not describe capabilities the agent does not have.',
        'Do not use a person’s Avatar or profile card for an agent.',
      ],
    },
    related: {
      items: [
        { id: 'ai-agent-avatar', reason: 'The agent’s picture and state, used in the header.' },
        { id: 'ai-agent-picker', reason: 'Choose one agent from compact cards.' },
        { id: 'ai-permission-scope', reason: 'Edit what the agent may access.' },
        { id: 'card', reason: 'A general-purpose card.' },
      ],
    },
    dependencies: { packages: ['react'], components: ['ai-agent-avatar'] },
    testing: {
      unit: [
        'Renders an article named by its heading with capabilities, tools, data access, owner and actions; ref, className and props forwarding.',
        'Shows the agent state on the avatar; omits empty sections.',
        'Compact variant: tool count and capability tags, no data access or owner; `as="div"`, `headingLevel="none"`, external `nameId` and `descriptionId`.',
        'Translated strings and heading level.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
