import { Button, EmptyState, Kbd } from '@rookcom/monarch-react';
import type { ComponentDocs } from '../../../internal/component-docs.js';
import { ListIcon, PlusIcon } from '../../../internal/docs-demo.js';

export default {
  id: 'empty-state',
  title: 'EmptyState',
  summary: 'What will appear here, and the action that creates it.',
  sections: {
    purpose: {
      summary:
        'Shown when there is nothing to display: says what will appear and offers the action that creates it, or a way back when a filter matched nothing.',
      useWhen: [
        'A list, table or view has no items yet (`kind="first-run"`, the default).',
        'A search or filter returns nothing (`kind="filtered"` with `onClearFilters`).',
        'A queue is empty because the work is done (`kind="success"`).',
        'A dashboard section or table body is empty and needs a one-line note (`layout="line"`, `size="compact"`).',
      ],
      avoidWhen: [
        'Loading failed: use ErrorState or InlineErrorRetry.',
        'Content is still loading: use LoadingState.',
        'A whole route is missing or forbidden: use ErrorPage.',
      ],
    },
    designPrinciples: {
      items: [
        'Say what will appear here, then offer the action that creates it. The product writes every word; the component only lays it out.',
        'First-run and filtered states read differently: first-run explains and invites, filtered offers a way back (Clear filters) rather than a create action.',
        'The heading level is configurable so the state fits the page outline; `p` avoids a heading inside a table cell.',
        'One accent-tinted icon tile at most; `success` switches it to the success tint. The rest stays neutral.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'All native `<div>` attributes pass through, and the ref is forwarded to the root `<div>`.',
        '`children` render after the actions (for example a short list of next steps); `tip` renders last in small muted text.',
      ],
    },
    typedProperties: { source: 'typescript' },
    variants: {
      items: [
        {
          prop: 'size',
          values: ['inline', 'page', 'compact'],
          description:
            '`inline` (default) sits inside a card or table; `page` adds vertical padding to fill a view; `compact` tightens padding, icon and title for table bodies and side panels. Exposed as `data-size`.',
        },
        {
          prop: 'layout',
          values: ['stack', 'line'],
          description:
            '`stack` (default) centres everything in a column; `line` is one left-aligned row of icon, text and actions. `line` is exposed as `data-layout="line"`.',
        },
        {
          prop: 'kind',
          values: ['first-run', 'filtered', 'success'],
          description:
            '`filtered` adds a Clear filters button when `onClearFilters` is set; `success` uses the success tint and a check icon when no `icon` is given. Non-default kinds are exposed as `data-kind`.',
        },
        {
          prop: 'headingLevel',
          values: ['h1', 'h2', 'h3', 'h4', 'h5', 'h6', 'p'],
          description: 'Title element. Default `h3`.',
        },
      ],
    },
    states: {
      items: [
        { name: 'First run', description: 'The default: icon, title, description and actions.' },
        {
          name: 'Filtered',
          attribute: 'data-kind="filtered"',
          description:
            'Clear filters (secondary button, label `clearFiltersLabel`) is rendered before the other actions.',
        },
        {
          name: 'Success',
          attribute: 'data-kind="success"',
          description: 'Success-tinted icon tile; a check icon when no icon is passed.',
        },
      ],
    },
    interaction: {
      items: [
        'Clear filters calls `onClearFilters`; the product resets its own filters and the list re-renders.',
        'Other actions keep their own behaviour.',
      ],
    },
    keyboard: {
      items: [
        {
          keys: 'Tab / Shift+Tab',
          action: 'Moves through Clear filters and the actions, in that order.',
        },
        { keys: 'Enter / Space', action: 'Activates the focused action.' },
      ],
      notes: ['The empty state itself is not focusable.'],
    },
    accessibility: {
      role: 'none; the title is a heading (or a paragraph with `headingLevel="p"`)',
      items: [
        'The icon tile is `aria-hidden`; meaning is carried by the title.',
        'The title is a real heading (h3 by default); pick the level that continues the page outline instead of skipping levels.',
        'No live region: an empty state is page content, not an announcement. When it replaces results after a filter change, the product announces the result count itself.',
        'Verified with axe in jsdom (each kind and layout) and in Chromium, Firefox and WebKit on /components/empty-state.',
      ],
    },
    responsive: {
      items: [
        '`stack` is centred with a 28rem maximum width; text wraps at narrow widths and actions wrap onto new lines.',
        '`line` wraps: the text takes the remaining width (12rem minimum) and the actions drop below it at 320px.',
      ],
    },
    theme: {
      items: [
        'The icon tile uses the accent subtle and strong tokens, so it follows the product accent; `success` uses the success tokens.',
        'Colours are Monarch semantic tokens only, so light, dark and more-contrast themes apply automatically.',
      ],
    },
    examples: {
      items: [
        {
          title: 'First run',
          stack: true,
          render: () => (
            <EmptyState
              icon={<PlusIcon />}
              title="No tickets yet"
              description="New tickets from your inbox and portal appear here."
              actions={
                <>
                  <Button variant="primary">New ticket</Button>
                  <Button>Import</Button>
                </>
              }
              tip={
                <>
                  Press <Kbd>N</Kbd> to create a ticket from anywhere.
                </>
              }
            />
          ),
        },
        {
          title: 'Filtered and all clear',
          stack: true,
          render: () => (
            <>
              <EmptyState
                kind="filtered"
                size="compact"
                title="No results match these filters"
                description="Try a different search or clear the filters."
                onClearFilters={() => {}}
              />
              <EmptyState
                kind="success"
                size="compact"
                title="You're all caught up"
                description="New items will appear here."
              />
            </>
          ),
        },
        {
          title: 'One line',
          stack: true,
          render: () => (
            <EmptyState
              layout="line"
              icon={<ListIcon />}
              headingLevel="p"
              title="No activity this week"
              description="Changes to records show up here."
              actions={<Button size="sm">View all activity</Button>}
            />
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Describe what will appear, not that nothing is there.',
        'Offer at most one primary action.',
        'Use `kind="filtered"` when a filter is active, so the way out is Clear filters, not Create.',
        'Keep next-step logic (which action to suggest) in the product and pass the result as `actions`.',
      ],
      dont: [
        'Do not use an empty state to report an error.',
        'Do not reuse the first-run copy for a filtered result: "No tickets yet" is wrong when tickets exist but are filtered out.',
      ],
    },
    related: {
      items: [
        { id: 'error-state', reason: 'Something failed to load.' },
        { id: 'loading-state', reason: 'Content is on its way.' },
        { id: 'inline-error-retry', reason: 'One section failed and can be retried in place.' },
        { id: 'error-page', reason: 'A whole route is missing, forbidden or down.' },
      ],
    },
    dependencies: { packages: ['react'], components: ['button'] },
    testing: {
      unit: [
        'Renders the title as an h3 by default; `headingLevel` changes it, `p` renders no heading.',
        'Default props keep the 0.3 markup (no `data-kind` or `data-layout`; decorative icon).',
        'Filtered renders Clear filters first and activates it from the keyboard; other kinds never render it.',
        'Success shows a check icon unless one is passed; `line` and `compact` are exposed as data attributes.',
        'Tip and children render after the actions; the ref is forwarded.',
      ],
      axe: true,
      ssr: true,
      browser: ['Visual baselines: /__gallery', 'Cross-browser axe: /components/empty-state'],
    },
  },
} satisfies ComponentDocs;
