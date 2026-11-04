import { Skeleton } from '@rookcom/monarch-react';
import type { ComponentDocs } from '../../../internal/component-docs.js';

export default {
  id: 'skeleton',
  title: 'Skeleton',
  summary: 'Loading placeholder shapes.',
  sections: {
    purpose: {
      summary: 'A placeholder shape that holds the layout while content loads.',
      useWhen: ['Building a custom loading layout that mirrors the final content.'],
      avoidWhen: [
        'A standard list, card grid or detail view: use LoadingState, which also announces loading.',
      ],
    },
    designPrinciples: {
      items: [
        'Hidden from assistive technology: mark the loading region `aria-busy="true"` and announce completion separately.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: ['`width` and `height` are merged into `style`.'],
    },
    typedProperties: { source: 'typescript' },
    variants: {
      items: [
        {
          prop: 'shape',
          values: ['rect', 'text', 'circle'],
          description:
            'Default `rect`. `text` is 0.75em high; `circle` is fully rounded. Exposed as `data-shape`.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Animating',
          description: 'A shimmer animation runs; it is turned off with reduced motion.',
        },
      ],
    },
    interaction: { items: ['None.'] },
    keyboard: { items: [], notes: ['Not interactive, so there is no keyboard behaviour.'] },
    accessibility: {
      role: 'none (aria-hidden)',
      items: ['Always `aria-hidden`.', '`prefers-reduced-motion: reduce` stops the shimmer.'],
    },
    responsive: {
      items: ['Sizes come from `width` and `height`; percentages follow the container.'],
    },
    theme: { items: ['Fill is `--mn-color-bg-muted`, so it adapts to every theme.'] },
    examples: {
      items: [
        {
          title: 'Shapes',
          render: () => (
            <>
              <Skeleton shape="circle" width="2.5rem" height="2.5rem" />
              <Skeleton shape="text" width="12rem" />
              <Skeleton width="8rem" height="2rem" />
            </>
          ),
          code: `<Skeleton shape="circle" width="2.5rem" height="2.5rem" />
<Skeleton shape="text" width="12rem" />
<Skeleton width="8rem" height="2rem" />`,
        },
      ],
    },
    doDont: {
      do: ['Match the skeleton to the shape of the content that will replace it.'],
      dont: ['Do not rely on Skeleton to tell screen-reader users that content is loading.'],
    },
    related: {
      items: [
        { id: 'loading-state', reason: 'Ready-made skeleton layouts with an announcement.' },
        { id: 'spinner', reason: 'Indeterminate progress in a small space.' },
      ],
    },
    dependencies: { packages: [], components: [] },
    testing: {
      unit: ['Is hidden from assistive technology.'],
      axe: true,
      ssr: true,
      browser: ['Visual baselines: /__gallery'],
    },
  },
} satisfies ComponentDocs;
