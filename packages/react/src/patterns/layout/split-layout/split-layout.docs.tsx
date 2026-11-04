import { useState } from 'react';
import { Button, EmptyState, SplitLayout } from '@rookcom/monarch-react';
import type { ComponentDocs } from '../../../internal/component-docs.js';

function Inbox() {
  const [open, setOpen] = useState<string | null>(null);
  return (
    <div style={{ height: '16rem', width: '100%' }}>
      <SplitLayout
        listLabel="Conversations"
        detailLabel="Conversation"
        detailOpen={open !== null}
        onBack={() => setOpen(null)}
        list={
          <div style={{ display: 'grid', gap: '0.5rem', padding: '0.5rem' }}>
            {['Billing question', 'Password reset'].map((t) => (
              <Button key={t} variant="ghost" onClick={() => setOpen(t)}>
                {t}
              </Button>
            ))}
          </div>
        }
        detail={
          open ? (
            <p style={{ padding: '1rem' }}>{open}</p>
          ) : (
            <EmptyState title="No conversation selected" description="Pick one from the list." />
          )
        }
      />
    </div>
  );
}

export default {
  id: 'split-layout',
  title: 'SplitLayout',
  summary: 'List and detail panes side by side, one pane at a time on narrow screens.',
  sections: {
    purpose: {
      summary:
        'Two-pane list + detail layout for inbox-style views. On wide screens both panes show; below 768px only one shows, chosen by `detailOpen`, with an optional back button.',
      useWhen: ['Browsing a list while reading one item (an inbox, a chat list, a document list).'],
      avoidWhen: ['Detail that needs the full page: navigate to a detail page instead.'],
    },
    designPrinciples: {
      items: [
        'No routing inside: the app decides what is open and passes `detailOpen`.',
        'Each pane is a labelled region so screen-reader users can jump between them.',
        'Each pane scrolls on its own.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'Renders a div with two `<section>` panes; native div attributes pass through. No ref is forwarded.',
      ],
    },
    typedProperties: { source: 'typescript' },
    variants: {
      items: [
        {
          prop: 'listWidth',
          values: ['sm', 'md', 'lg'],
          description:
            'List pane width on wide screens: 18rem, 22rem (default) or 28rem. Exposed as `data-list-width`.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Detail open',
          attribute: 'data-detail-open',
          description: 'Below 768px, shows the detail pane instead of the list.',
        },
      ],
    },
    interaction: {
      items: [
        'With `onBack`, the detail pane renders an IconButton (label `backLabel`, default "Back to list") that calls it.',
        'The back button is hidden from 768px up.',
      ],
    },
    keyboard: {
      items: [
        {
          keys: 'Tab',
          action: 'Moves through list content, then detail content (including the back button).',
        },
      ],
    },
    accessibility: {
      role: 'region (two labelled sections)',
      name: '`listLabel` (default "List") and `detailLabel` (default "Detail").',
      items: [
        'Give both panes specific labels, for example "Tickets" and "Ticket".',
        'Focus is not moved when `detailOpen` changes; the app should move it to the opened detail.',
      ],
    },
    responsive: {
      items: [
        'Below 768px: one column; the list shows unless `detailOpen`, then the detail shows.',
        'From 768px: list column at `listWidth`, detail fills the rest.',
        'The layout fills its container height (`height: 100%`); give the container a height.',
      ],
    },
    theme: { items: ['Pane divider and back-bar borders use Monarch border tokens.'] },
    examples: {
      items: [
        {
          title: 'Inbox',
          stack: true,
          render: () => <Inbox />,
        },
      ],
    },
    doDont: {
      do: [
        'Show an EmptyState in the detail pane when nothing is selected.',
        'Pass `onBack` so narrow screens can return to the list.',
      ],
      dont: ['Do not nest SplitLayouts.', 'Do not leave the container without a height.'],
    },
    related: {
      items: [
        { id: 'split-inbox-layout', reason: 'Planned three-pane inbox template (wave 8).' },
        { id: 'empty-state', reason: 'Placeholder for an empty detail pane.' },
        { id: 'page-header', reason: 'Sits above the layout.' },
      ],
    },
    dependencies: { packages: [], components: ['button'] },
    testing: {
      unit: ['Both panes are labelled regions.', 'The back button calls onBack.'],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
