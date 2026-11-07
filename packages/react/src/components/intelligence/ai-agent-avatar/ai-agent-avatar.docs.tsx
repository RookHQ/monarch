import { Avatar } from '@rookcom/monarch-react';
import { AiAgentAvatar } from '@rookcom/monarch-react/ai';
import type { ComponentDocs } from '../../../internal/component-docs.js';

const research = { id: 'research', name: 'Research agent', role: 'Finds and checks sources' };
const writer = { id: 'writer', name: 'Writing agent', codename: 'Quill' };

export default {
  id: 'ai-agent-avatar',
  title: 'AiAgentAvatar',
  summary: 'An AI agent’s picture: a rounded square with a Bot badge and a working ring.',
  sections: {
    purpose: {
      summary:
        'Shows which AI agent is acting and what it is doing, in a shape that can never be mistaken for a person’s avatar.',
      useWhen: [
        'Identifying an agent in a thread header, roster, handoff marker, action log or agent card.',
        'Showing at a glance that an agent is working, waiting for someone, or has failed.',
      ],
      avoidWhen: [
        'A person, team or organisation: use Avatar.',
        'Marking a piece of content as AI-generated: use AiGeneratedBadge.',
      ],
    },
    designPrinciples: {
      items: [
        'Agents are rounded squares with a Bot badge; people are circles. The difference holds even without colour.',
        'State is in the accessible name and, with `showState`, in visible text; the ring only reinforces it.',
        'Only the working ring moves, and it stops under reduced motion.',
        'Without an image the face shows initials from the codename or name on the AI tint, so agents stay distinguishable.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'The ref goes to the root `<span>`; native `<span>` attributes pass through.',
        '`aiAgentInitials(agent)` and `resolveAiAgentAvatarMessages(messages)` are exported for components that compose the avatar.',
      ],
    },
    typedProperties: { source: 'typescript' },
    variants: {
      items: [
        {
          prop: 'size',
          values: ['xs', 'sm', 'md', 'lg', 'xl', 'number'],
          description:
            '20, 24, 32 (default), 40 and 56px, or any pixel size. At `xs` the Bot badge is hidden; the shape and tint carry the identity.',
        },
        {
          prop: 'showState',
          values: ['false', 'true'],
          description: 'Adds the state as visible text beside the picture.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Idle',
          attribute: 'data-state="idle"',
          description: 'No ring; the name reads "Research agent, AI agent".',
        },
        {
          name: 'Working',
          attribute: 'data-state="working"',
          description:
            'A pulsing AI-coloured ring (still under reduced motion); ", Working" is read.',
        },
        {
          name: 'Waiting',
          attribute: 'data-state="waiting"',
          description: 'A dashed warning ring; ", Waiting for you" is read.',
        },
        {
          name: 'Error',
          attribute: 'data-state="error"',
          description: 'A danger ring and danger badge; ", Error" is read.',
        },
        {
          name: 'Image failed',
          description: 'If `agent.avatar` fails to load, the initials show instead.',
        },
      ],
    },
    interaction: {
      items: [],
      notes: ['Not interactive. Wrap it in a button or link if it should act.'],
    },
    keyboard: { items: [], notes: ['Not focusable.'] },
    accessibility: {
      role: 'img (the picture)',
      name: '`messages.label(name, kind, state)`, default "Research agent, AI agent, Working"',
      items: [
        'Every agent is announced as an AI agent, so assistive technology never presents it as a person.',
        'With `decorative`, for when the name is visible beside it, the picture is hidden but a non-idle state is still read as visually hidden text.',
        'The image has empty alt text; the name is on the wrapper.',
        'State never depends on colour or motion alone: the ring styles differ (solid, dashed) and the state is text.',
      ],
    },
    responsive: {
      items: [
        'Fixed size per preset; the optional state text does not wrap.',
        'RTL: the badge sits at the inline end and follows the writing direction.',
      ],
    },
    theme: {
      items: [
        'Face on `--mn-color-ai-subtle` with `--mn-color-ai-strong` initials and an `--mn-color-ai-border` edge; badge on `--mn-color-ai-solid`.',
        'Rings: `--mn-color-ai-solid` (working, `--mn-duration-shimmer` pulse), `--mn-color-warning-strong` dashed (waiting), `--mn-color-danger-strong` (error).',
        'Forced colours draw the face, badge and ring in CanvasText.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Sizes',
          render: () => (
            <>
              <AiAgentAvatar agent={research} size="xs" />
              <AiAgentAvatar agent={research} size="sm" />
              <AiAgentAvatar agent={research} />
              <AiAgentAvatar agent={research} size="lg" />
              <AiAgentAvatar agent={research} size="xl" />
            </>
          ),
        },
        {
          title: 'States',
          render: () => (
            <>
              <AiAgentAvatar agent={writer} size="lg" showState />
              <AiAgentAvatar agent={writer} size="lg" state="working" showState />
              <AiAgentAvatar agent={writer} size="lg" state="waiting" showState />
              <AiAgentAvatar agent={writer} size="lg" state="error" showState />
            </>
          ),
        },
        {
          title: 'Agent beside a person',
          render: () => (
            <>
              <Avatar name="Sam Lee" size="lg" />
              <AiAgentAvatar agent={research} size="lg" state="working" />
            </>
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Use it for every agent, so people learn the shape means AI.',
        'Set `decorative` when the agent’s name is already visible next to it.',
      ],
      dont: [
        'Do not use Avatar with a robot picture for an agent; it would look like a person.',
        'Do not leave an agent in `working` after it has stopped.',
      ],
    },
    related: {
      items: [
        { id: 'avatar', reason: 'People, teams and organisations.' },
        { id: 'ai-agent-card', reason: 'The agent with its description, tools and access.' },
        { id: 'ai-multi-agent-roster', reason: 'Several agents and what each is doing.' },
        { id: 'ai-generated-badge', reason: 'Marks content as AI-generated.' },
      ],
    },
    dependencies: { packages: ['react'], components: [] },
    testing: {
      unit: [
        'Accessible name "Research agent, AI agent" plus the state when not idle; ref and props forwarding.',
        'Visible state text with `showState`; decorative mode hides the picture but still reads a non-idle state.',
        'Image with empty alt that falls back to initials on error; codename initials; numeric size; translated strings.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
