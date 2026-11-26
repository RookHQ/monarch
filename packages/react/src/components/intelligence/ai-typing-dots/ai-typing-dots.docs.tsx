import { AiTypingDots } from '@rookcom/monarch-react/ai';
import type { ComponentDocs } from '../../../internal/component-docs.js';

export default {
  id: 'ai-typing-dots',
  title: 'AiTypingDots',
  summary: 'A compact three-dot "assistant is typing" indicator.',
  sections: {
    purpose: {
      summary:
        'Shows that the assistant is writing in places with no room for a label: a chat dock, a conversation list row, a collapsed panel header.',
      useWhen: [
        'A dock or list row needs to show that a reply is on its way.',
        'A small message bubble is waiting for its first token.',
      ],
      avoidWhen: [
        'The main conversation view, where a label helps: use AiThinkingIndicator.',
        'Non-AI loading: use Spinner.',
      ],
    },
    designPrinciples: {
      items: [
        'Small and familiar: three dots in the AI colour, nothing else.',
        'The meaning is always available to assistive technology through a hidden label.',
        'Motion is decoration only and stops under reduced motion and in forced colours.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: ['Native `<span>` attributes pass through and the ref is forwarded to the root.'],
    },
    typedProperties: { source: 'typescript' },
    variants: {
      items: [
        {
          prop: 'size',
          values: ['sm', 'md'],
          description:
            '`sm` (4px dots) for docks and rows, `md` (6px, default) for bubbles. Exposed as `data-size`.',
        },
      ],
    },
    states: {
      items: [
        { name: 'Typing', description: 'The dots rise and fade in turn.' },
        {
          name: 'Static',
          description:
            'Under `prefers-reduced-motion: reduce` and `forced-colors: active` the dots stand still at a steady opacity.',
        },
      ],
    },
    interaction: { items: ['Not interactive.'] },
    keyboard: { items: [], notes: ['Not focusable and has no keyboard interaction.'] },
    accessibility: {
      role: 'status (a visually hidden live region inside the root)',
      name: 'The label, default "Assistant is typing"',
      items: [
        'The label is announced politely once, shortly after the dots mount.',
        'With `announce={false}` the label is plain hidden text, for when another live region already reports the state.',
        'The dots are `aria-hidden`.',
      ],
    },
    responsive: { items: ['Inline and fixed size; it fits in any row and never wraps.'] },
    theme: {
      items: [
        'Dots in `--mn-color-ai-strong`, timed from `--mn-duration-shimmer`.',
        'CanvasText in forced colours.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Sizes',
          render: () => (
            <>
              <AiTypingDots size="sm" />
              <AiTypingDots />
            </>
          ),
        },
        {
          title: 'In a list row',
          render: () => (
            <span
              style={{ display: 'inline-flex', alignItems: 'center', gap: 'var(--mn-space-2)' }}
            >
              Project planning
              <AiTypingDots size="sm" announce={false} label="Reply in progress" />
            </span>
          ),
        },
      ],
    },
    doDont: {
      do: ['Remove it as soon as text starts streaming.', 'Localise `label`.'],
      dont: [
        'Do not announce from several rows at once; pass `announce={false}` in lists.',
        'Do not use it for non-AI loading.',
      ],
    },
    related: {
      items: [
        {
          id: 'ai-thinking-indicator',
          reason: 'The labelled indicator for the main conversation view.',
        },
        { id: 'spinner', reason: 'Non-AI loading.' },
      ],
    },
    dependencies: { packages: ['react'], components: [] },
    testing: {
      unit: [
        'Three decorative dots, ref and props forwarding.',
        'Announces the default and a custom label in a polite status; `sm` size.',
        'With `announce={false}` the label is hidden text and there is no live region.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
