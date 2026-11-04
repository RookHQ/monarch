import { Badge } from '@rookcom/monarch-react';
import type { ComponentDocs } from '../../../internal/component-docs.js';

export default {
  id: 'badge',
  title: 'Badge',
  summary: 'Status and metadata labels in six tones.',
  sections: {
    purpose: {
      summary: 'A short status or metadata label, such as a ticket state, a count or a version.',
      useWhen: [
        'Showing the status of a row or record (`subtle`, the default).',
        'Showing a count (`solid`) or neutral metadata (`outline`).',
      ],
      avoidWhen: [
        'The label is an action: use Button.',
        'The message needs a sentence of explanation: use Alert.',
      ],
    },
    designPrinciples: {
      items: [
        'Never the only way to convey meaning: the text carries the status, colour only reinforces it.',
        'Tone names the meaning (`success`, `danger`), not the colour.',
      ],
    },
    apiReference: { source: 'typescript', notes: ['All native `<span>` attributes pass through.'] },
    typedProperties: { source: 'typescript' },
    variants: {
      items: [
        {
          prop: 'tone',
          values: ['neutral', 'accent', 'info', 'success', 'warning', 'danger'],
          description: 'Semantic colour. Default `neutral`. Exposed as `data-tone`.',
        },
        {
          prop: 'variant',
          values: ['subtle', 'solid', 'outline'],
          description:
            '`subtle` (default) for status in tables, `solid` for counts, `outline` for metadata. Exposed as `data-variant`.',
        },
        {
          prop: 'size',
          values: ['sm', 'md'],
          description: 'Default `md`. Exposed as `data-size`.',
        },
        { prop: 'dot', values: ['true', 'false'], description: 'Adds a leading status dot.' },
      ],
    },
    states: {
      items: [
        {
          name: 'Static',
          description: 'Badge has no interactive states; it is a plain `<span>`.',
        },
      ],
    },
    interaction: { items: ['None. Badge is not focusable and has no event handling of its own.'] },
    keyboard: { items: [], notes: ['Not interactive, so there is no keyboard behaviour.'] },
    accessibility: {
      role: 'none (a plain span)',
      name: 'Its text content.',
      items: [
        'The status dot is `aria-hidden`, so only the text is announced.',
        'Keep the text meaningful on its own; colour is never the only signal.',
      ],
    },
    responsive: { items: ['No breakpoints: the badge sizes to its text at every width.'] },
    theme: {
      items: [
        'Colours are semantic tone tokens, so light, dark and more-contrast themes apply automatically.',
        '`accent` follows the product accent; the other tones do not change per product.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Tones and variants',
          render: () => (
            <>
              {(['neutral', 'accent', 'info', 'success', 'warning', 'danger'] as const).map((t) => (
                <Badge key={t} tone={t} dot>
                  {t}
                </Badge>
              ))}
              <Badge tone="success" variant="solid">
                3
              </Badge>
              <Badge tone="info" variant="outline">
                Beta
              </Badge>
              <Badge size="sm">v0.1.0</Badge>
            </>
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Use `subtle` badges for status inside tables and lists.',
        'Keep labels to one or two words.',
      ],
      dont: [
        'Do not rely on the tone colour alone to convey meaning.',
        'Do not make a badge clickable; wrap the action in a Button instead.',
      ],
    },
    related: {
      items: [
        { id: 'status-badge', reason: 'Planned: a badge driven by a typed status registry.' },
        { id: 'alert', reason: 'A message that needs a sentence, not a label.' },
      ],
    },
    dependencies: { packages: [], components: [] },
    testing: {
      unit: ['Renders tone as `data-tone` and hides the dot from assistive technology.'],
      axe: true,
      ssr: true,
      browser: ['Visual baselines: /__gallery'],
    },
  },
} satisfies ComponentDocs;
