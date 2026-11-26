import { AiResponseSkeleton } from '@rookcom/monarch-react/ai';
import type { ComponentDocs } from '../../../internal/component-docs.js';

export default {
  id: 'ai-response-skeleton',
  title: 'AiResponseSkeleton',
  summary:
    'Placeholder shaped like the coming answer (text, cards or table) while the model starts.',
  sections: {
    purpose: {
      summary:
        'Fills the gap between sending a prompt and the first streamed token with a placeholder shaped like the answer that is coming, so the layout does not jump when it arrives and people can see that work has started.',
      useWhen: [
        'An answer is requested and nothing has streamed yet.',
        'A generated card, table or summary is loading in place, outside a chat thread.',
      ],
      avoidWhen: [
        'Text is already streaming: show the text itself.',
        'Short waits inside a chat bubble: AiTypingDots or AiThinkingIndicator are lighter.',
        'Non-AI loading: use Skeleton or LoadingState.',
      ],
    },
    designPrinciples: {
      items: [
        'Shape matches the expected answer to avoid layout shift.',
        'The shimmer uses the AI subtle tint, so it reads as AI work without decoration.',
        'Widths are fixed, so server and client render identically.',
        'Motion stops entirely under reduced motion; the bars stay.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: ['The ref goes to the root `<div>`; native `<div>` attributes pass through.'],
    },
    typedProperties: { source: 'typescript' },
    variants: {
      items: [
        {
          prop: 'shape',
          values: ['text', 'cards', 'table'],
          description:
            '`text` (default): paragraph lines with a short last line. `cards`: a responsive grid of card outlines. `table`: a header row and body rows. Exposed as `data-shape`.',
        },
      ],
      notes: [
        '`lines` sets the number of lines, cards or table rows; `columns` sets table columns.',
      ],
    },
    states: {
      items: [
        { name: 'Shape', attribute: 'data-shape', description: 'See Variants.' },
        {
          name: 'Busy',
          attribute: 'aria-busy',
          description: 'Always `true`: the component exists only while waiting.',
        },
      ],
    },
    interaction: {
      items: [],
      notes: ['Not interactive. Replace it with the answer when the first token arrives.'],
    },
    keyboard: { items: [], notes: ['Not focusable.'] },
    accessibility: {
      role: 'status',
      name: 'None; its visually hidden text (“Generating response…”) is the content',
      items: [
        'The bars are `aria-hidden`; only the label is read.',
        'The status is polite, so it never interrupts.',
        'Pair it with the thread’s own announcements; do not also announce from the app.',
      ],
    },
    responsive: {
      items: [
        'Bars use percentage widths of the container.',
        'Cards wrap from several columns down to one at 390px; table columns shrink evenly.',
        'The shimmer sweeps from the start edge in RTL.',
      ],
    },
    theme: {
      items: [
        'Bars: `--mn-color-bg-muted` with an `--mn-color-ai-subtle` sweep over `--mn-duration-shimmer`.',
        'Cards and table: `--mn-color-border-subtle` on `--mn-color-bg-surface` / `--mn-color-bg-subtle`.',
        'Reduced motion: no animation and no gradient. Forced colours: GrayText bars and CanvasText borders.',
      ],
    },
    examples: {
      items: [
        { title: 'Text (default)', stack: true, render: () => <AiResponseSkeleton /> },
        { title: 'Cards', stack: true, render: () => <AiResponseSkeleton shape="cards" /> },
        {
          title: 'Table',
          stack: true,
          render: () => <AiResponseSkeleton shape="table" lines={3} columns={4} />,
        },
      ],
    },
    doDont: {
      do: [
        'Choose the shape of the answer you expect.',
        'Replace it in place with the real content.',
      ],
      dont: [
        'Do not keep it after text starts streaming.',
        'Do not show several skeletons for one answer.',
      ],
    },
    related: {
      items: [
        { id: 'ai-thinking-indicator', reason: 'Labelled “Thinking…” status inside a thread.' },
        { id: 'ai-typing-dots', reason: 'Compact waiting indicator.' },
        { id: 'skeleton', reason: 'General loading placeholders.' },
      ],
    },
    dependencies: { packages: ['react'], components: [] },
    testing: {
      unit: [
        'Busy status with hidden label and decorative bars; ref and props forwarding.',
        'Line, card, row and column counts follow the props.',
        'Label localisation; identical server renders.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
