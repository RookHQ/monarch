import { AiGeneratedBadge } from '@rookcom/monarch-react/ai';
import type { ComponentDocs } from '../../../internal/component-docs.js';

export default {
  id: 'ai-generated-badge',
  title: 'AiGeneratedBadge',
  summary: 'A provenance badge that marks content written or changed by AI.',
  sections: {
    purpose: {
      summary:
        'Tells people, honestly and at a glance, that a piece of content came from AI: written by it, written with its help, or written by it and then changed by a person. The model and time are one hover or focus away.',
      useWhen: [
        'A field, summary, reply draft or record value was produced by an AI feature.',
        'A person edited AI output and the record should still say where it started (`kind="edited"`).',
        'You need a compact provenance marker in a table cell or list row (`size="sm"`, `iconOnly`).',
      ],
      avoidWhen: [
        'The whole surface is an assistant conversation: AiMessage already identifies AI turns.',
        'The value came from a rule or an import, not AI: say so in text instead of using this badge.',
        'You want to show how sure the AI is: use AiConfidenceMeter next to it.',
      ],
    },
    designPrinciples: {
      items: [
        'Provenance is shown, not hidden: AI output carries this badge wherever it is displayed, not only where it was created.',
        'The Bot glyph and the AI intent colour identify AI; never a sparkle icon.',
        'The label is always text, so the meaning never depends on colour or the icon.',
        'Details (model, time) are supplementary and live in the tooltip and the accessible text, so the badge stays small.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'Native `<span>` attributes pass through and the ref is forwarded to the badge.',
        '`at` accepts a `Date`, epoch milliseconds or an ISO 8601 string; an invalid value is ignored.',
        '`AiBotGlyph` is exported for other AI surfaces that need the same glyph.',
      ],
    },
    typedProperties: { source: 'typescript' },
    variants: {
      items: [
        {
          prop: 'kind',
          values: ['generated', 'assisted', 'edited'],
          description:
            'The label: "AI generated", "AI assisted" or "Edited after AI". `edited` also draws a dashed border. Exposed as `data-kind`.',
        },
        {
          prop: 'size',
          values: ['md', 'sm'],
          description: '`sm` for table cells and dense lists. Exposed as `data-size`.',
        },
        {
          prop: 'iconOnly',
          values: ['false', 'true'],
          description:
            'Shows only the Bot glyph. The label stays in the accessible text and moves into the tooltip. Exposed as `data-icon-only`.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'With details',
          attribute: 'tabindex="0"',
          description:
            'When `model`, `at`, `tooltip` or `iconOnly` gives it something to show, the badge becomes a tab stop and opens a tooltip on hover and focus.',
        },
        {
          name: 'Plain',
          description: 'With nothing extra to show it is static text and not focusable.',
        },
      ],
    },
    interaction: {
      items: [
        'Hovering or focusing the badge opens the tooltip with the model and time, or the custom `tooltip`.',
        'The badge has no action of its own; it never opens anything else.',
      ],
    },
    keyboard: {
      items: [
        { keys: 'Tab', action: 'Focuses the badge when it has a tooltip, which opens it.' },
        { keys: 'Escape', action: 'Closes the tooltip.' },
      ],
      notes: ['A badge without a tooltip is not focusable.'],
    },
    accessibility: {
      role: 'none (inline text)',
      name: 'The label, then the model and time, e.g. "AI generated, By model-x, 4 Mar 2026"',
      items: [
        'The label is real text; with `iconOnly` it is visually hidden, so screen readers always hear it.',
        'The model and time are read after the label from visually hidden text, so they do not depend on the tooltip.',
        'The Bot glyph is decorative (`aria-hidden`).',
        'It is focusable only when a tooltip exists, so keyboard users can reach what mouse users can see.',
      ],
    },
    responsive: {
      items: [
        'The badge never wraps; a long custom label is truncated with an ellipsis inside the available width, so it cannot cause horizontal scroll at 390px.',
      ],
    },
    theme: {
      items: [
        '`--mn-color-ai-subtle` background, `--mn-color-ai-strong` text and glyph, `--mn-color-ai-border` border, in all four themes.',
        'In forced colours it becomes a CanvasText outline on Canvas.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Kinds',
          render: () => (
            <>
              <AiGeneratedBadge kind="generated" />
              <AiGeneratedBadge kind="assisted" />
              <AiGeneratedBadge kind="edited" />
            </>
          ),
        },
        {
          title: 'With model and time',
          render: () => <AiGeneratedBadge model="model-x" at="2026-03-04T10:00:00Z" />,
        },
        {
          title: 'Compact, for tables',
          render: () => (
            <>
              <AiGeneratedBadge size="sm" />
              <AiGeneratedBadge size="sm" iconOnly model="model-x" />
            </>
          ),
        },
        {
          title: 'Next to generated content',
          stack: true,
          render: () => (
            <p style={{ margin: 0 }}>
              Meeting notes summary <AiGeneratedBadge size="sm" kind="edited" />
            </p>
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Keep the badge on AI output after it is saved, not only while it is a suggestion.',
        'Switch to `kind="edited"` once a person changes AI output.',
        'Pass `model` and `at` when you have them.',
      ],
      dont: [
        'Do not use it for content from rules or imports.',
        'Do not put essential information in `tooltip`.',
        'Do not replace the Bot glyph with a sparkle icon.',
      ],
    },
    related: {
      items: [
        { id: 'ai-confidence-meter', reason: 'Shows how sure the AI is.' },
        { id: 'ai-autofill-field', reason: 'Uses this badge on AI-filled form fields.' },
        { id: 'badge', reason: 'The general status and metadata badge.' },
        { id: 'tooltip', reason: 'Shows the details on hover and focus.' },
      ],
    },
    dependencies: { packages: ['react'], components: ['tooltip'] },
    testing: {
      unit: [
        'Label, data attributes, decorative glyph and ref forwarding for every kind.',
        'Reads model and time after the label and shows them in a tooltip on keyboard focus; Escape closes it.',
        'Not focusable without a tooltip; invalid times are ignored.',
        'Icon-only keeps the label in hidden text and the tooltip; custom tooltip, label and messages.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
