import { LoadingState } from '@rookcom/monarch-react';
import type { ComponentDocs } from '../../../internal/component-docs.js';

export default {
  id: 'loading-state',
  title: 'LoadingState',
  summary: 'A region is loading: a layout-shaped skeleton or a labelled spinner, announced once.',
  sections: {
    purpose: {
      summary:
        'The placeholder shown while a list, table, card grid or detail view loads. It marks the region busy and announces what is loading once.',
      useWhen: [
        'A list with avatars is loading (`list`, the default).',
        'A table or plain list is loading (`rows`).',
        'A card grid is loading (`cards`).',
        'A detail view is loading (`detail`).',
        'The shape of the result is unknown, or a whole card or page is loading (`spinner`).',
      ],
      avoidWhen: [
        'A single control is busy: use Button `loading` or Spinner.',
        'Loading failed: use ErrorState or InlineErrorRetry.',
        'There is no data: use EmptyState.',
      ],
    },
    designPrinciples: {
      items: [
        'Match the shape of what is coming, so the page does not jump when it arrives.',
        'Announce the label once and hide the skeleton shapes from assistive technology.',
        'A spinner always comes with visible text naming what is loading; never a bare spinner on a page.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'All native `<div>` attributes pass through, and the ref is forwarded to the root `<div>`.',
        'Before 0.4 LoadingState lived in the error-state folder; the export, props and class names (`mn-loading-state`, `__row`, `__lines`) are unchanged.',
      ],
    },
    typedProperties: { source: 'typescript' },
    variants: {
      items: [
        {
          prop: 'variant',
          values: ['list', 'rows', 'cards', 'detail', 'spinner'],
          description:
            '`list` (default) stacks avatar + two text lines; `rows` stacks bordered text rows (table and list-row preset); `cards` a grid of blocks; `detail` a title and paragraphs; `spinner` a centred spinner with the label shown. Exposed as `data-variant`.',
        },
        {
          prop: 'rows',
          values: ['number'],
          description:
            'Rows for `list`, `rows` and `cards`. Default 4; negative values render none.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Loading',
          attribute: 'aria-busy="true"',
          description: 'The root is always marked busy; unmount it when the content arrives.',
        },
      ],
    },
    interaction: {
      items: [],
      notes: ['Not interactive.'],
    },
    keyboard: {
      items: [],
      notes: ['Not interactive and not focusable.'],
    },
    accessibility: {
      role: 'status inside an aria-busy region',
      name: 'The `label` prop, e.g. "Loading invoices".',
      items: [
        'The label renders in a role="status" element: visually hidden for skeleton variants, visible next to the spinner for `spinner`.',
        'Skeleton shapes and the spinner are `aria-hidden`.',
        'Do not nest LoadingState inside another aria-busy region; announce completion from the loaded content if it matters.',
        'Verified with axe in jsdom and in Chromium, Firefox and WebKit on /components/loading-state.',
      ],
    },
    responsive: {
      items: [
        '`cards` uses an auto-fill grid with a 14rem minimum column that shrinks to one full-width column below 14rem.',
        'Skeleton widths are percentages, so rows fit any container down to 320px.',
      ],
    },
    theme: {
      items: [
        'Skeleton shapes use the Skeleton component (muted surface tokens); `rows` dividers use `--mn-color-border-subtle`.',
        'The spinner label uses `--mn-color-text-muted`.',
        'Under `prefers-reduced-motion: reduce` the skeleton shimmer stops and the spinner slows (from Skeleton and Spinner).',
      ],
    },
    examples: {
      items: [
        {
          title: 'List and rows',
          stack: true,
          render: () => (
            <>
              <LoadingState label="Loading contacts" rows={3} />
              <LoadingState label="Loading invoices" variant="rows" rows={3} />
            </>
          ),
        },
        {
          title: 'Cards and detail',
          stack: true,
          render: () => (
            <>
              <LoadingState label="Loading projects" variant="cards" rows={3} />
              <LoadingState label="Loading project" variant="detail" />
            </>
          ),
        },
        {
          title: 'Spinner',
          stack: true,
          render: () => <LoadingState label="Loading report…" variant="spinner" />,
        },
      ],
    },
    doDont: {
      do: [
        'Give it a label naming what is loading.',
        'Pick the variant that matches the content that will replace it.',
      ],
      dont: [
        'Do not nest LoadingState inside another aria-busy region.',
        'Do not leave it mounted after the content arrives.',
      ],
    },
    related: {
      items: [
        { id: 'skeleton', reason: 'The placeholder shapes LoadingState is built from.' },
        { id: 'spinner', reason: 'A single busy control.' },
        { id: 'error-state', reason: 'Loading failed.' },
        { id: 'empty-state', reason: 'Loaded, but nothing to show.' },
      ],
    },
    dependencies: { packages: ['react'], components: ['skeleton', 'spinner'] },
    testing: {
      unit: [
        'Announces its label as a status inside an aria-busy root.',
        'Default props keep the 0.3 markup and class names; the ref is forwarded.',
        '`rows` renders plain text rows without avatars; `cards` and `detail` render their shapes.',
        '`spinner` shows the label as visible status text and hides the spinner from assistive technology.',
        'Negative `rows` render no rows.',
      ],
      axe: true,
      ssr: true,
      browser: ['Visual baselines: /__gallery', 'Cross-browser axe: /components/loading-state'],
    },
  },
} satisfies ComponentDocs;
