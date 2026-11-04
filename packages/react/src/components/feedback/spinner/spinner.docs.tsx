import { Spinner } from '@rookcom/monarch-react';
import type { ComponentDocs } from '../../../internal/component-docs.js';

export default {
  id: 'spinner',
  title: 'Spinner',
  summary: 'Indeterminate progress.',
  sections: {
    purpose: {
      summary: 'A small indeterminate progress indicator.',
      useWhen: ['A short wait inside a control or small area (Button uses it while loading).'],
      avoidWhen: [
        'Progress is measurable: use Progress.',
        'A whole region is loading: use LoadingState.',
      ],
    },
    designPrinciples: {
      items: [
        'Labelled means announced: with `label` it is a status; without, it is decorative.',
        'Server-component safe: no state or effects.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: ['All native `<span>` attributes except children pass through.'],
    },
    typedProperties: { source: 'typescript' },
    variants: {
      items: [
        {
          prop: 'size',
          values: ['sm', 'md', 'lg'],
          description: '0.875rem, 1rem (default), 1.5rem. Exposed as `data-size`.',
        },
      ],
    },
    states: {
      items: [
        { name: 'Spinning', description: 'Always animating; slower (1.6s) with reduced motion.' },
      ],
    },
    interaction: { items: ['None.'] },
    keyboard: { items: [], notes: ['Not interactive, so there is no keyboard behaviour.'] },
    accessibility: {
      role: 'status when labelled; otherwise aria-hidden',
      name: '`label` becomes `aria-label`.',
      items: ['Leave `label` off inside a busy button, which sets `aria-busy` itself.'],
    },
    responsive: { items: ['Fixed size per `size`; no breakpoints.'] },
    theme: { items: ['Drawn with `currentColor`, so it takes the surrounding text colour.'] },
    examples: {
      items: [
        {
          title: 'Sizes',
          render: () => (
            <>
              <Spinner size="sm" label="Loading small" />
              <Spinner label="Loading" />
              <Spinner size="lg" label="Loading large" />
            </>
          ),
          code: `<Spinner label="Loading tickets" />`,
        },
      ],
    },
    doDont: {
      do: ['Give a standalone spinner a `label` that names what is loading.'],
      dont: ['Do not label a spinner inside a loading Button; it would be announced twice.'],
    },
    related: {
      items: [
        { id: 'progress-bar', reason: 'Measurable progress.' },
        { id: 'button', reason: 'Shows a spinner while `loading`.' },
      ],
    },
    dependencies: { packages: [], components: [] },
    testing: {
      unit: ['Is a status only when labelled; otherwise aria-hidden.'],
      axe: true,
      ssr: true,
      browser: ['Visual baselines: /__gallery'],
    },
  },
} satisfies ComponentDocs;
