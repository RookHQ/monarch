import { useState } from 'react';
import { Badge, Button } from '@rookcom/monarch-react';
import { SearchResults, type SearchResultGroup } from '@rookcom/monarch-react/data';
import type { ComponentDocs } from '../../../internal/component-docs.js';
import { CalendarIcon, InboxIcon, UsersIcon } from '../../../internal/docs-demo.js';
// The scenario of the primary example, shared with iOS and Android (CROSS-PLATFORM.md section 8).
import fixture from './search-results.fixture.json';

interface FixtureItem {
  id: string;
  title: string;
  path?: string[];
  snippet?: string;
  meta?: string;
}
const [documents, events, people] = fixture.data.groups as unknown as readonly [
  { id: string; label: string; total: number; items: readonly [FixtureItem, FixtureItem] },
  { id: string; label: string; items: readonly [FixtureItem] },
  { id: string; label: string; items: readonly [FixtureItem] },
];
const [policy, checklist] = documents.items;
const [planning] = events.items;
const [alex] = people.items;

const GROUPS: SearchResultGroup[] = [
  {
    id: documents.id,
    label: documents.label,
    icon: <InboxIcon />,
    total: documents.total,
    onShowMore: () => {},
    items: [
      {
        id: policy.id,
        title: policy.title,
        href: '#travel-policy',
        ...(policy.path && { path: policy.path }),
        ...(policy.snippet && { snippet: policy.snippet }),
        meta: <Badge>{policy.meta}</Badge>,
      },
      {
        id: checklist.id,
        title: checklist.title,
        href: '#travel-checklist',
        ...(checklist.path && { path: checklist.path }),
        ...(checklist.snippet && { snippet: checklist.snippet }),
      },
    ],
  },
  {
    id: events.id,
    label: events.label,
    icon: <CalendarIcon />,
    items: [
      {
        id: planning.id,
        title: planning.title,
        href: '#planning-review',
        ...(planning.path && { path: planning.path }),
        ...(planning.meta && { meta: planning.meta }),
      },
    ],
  },
  {
    id: people.id,
    label: people.label,
    icon: <UsersIcon />,
    items: [
      {
        id: alex.id,
        title: alex.title,
        href: '#alex',
        ...(alex.snippet && { snippet: alex.snippet }),
      },
    ],
  },
];

function StatesDemo() {
  const [state, setState] = useState<'ready' | 'loading' | 'empty' | 'error'>('loading');
  return (
    <div style={{ display: 'grid', gap: 'var(--mn-space-3)' }}>
      <div style={{ display: 'flex', flexWrap: 'wrap', gap: 'var(--mn-space-2)' }}>
        {(['loading', 'ready', 'empty', 'error'] as const).map((s) => (
          <Button key={s} size="sm" aria-pressed={state === s} onClick={() => setState(s)}>
            {s}
          </Button>
        ))}
      </div>
      <SearchResults
        query="travel"
        loading={state === 'loading'}
        error={state === 'error' ? 'The search service did not respond.' : undefined}
        onRetry={() => setState('ready')}
        groups={state === 'ready' ? GROUPS : []}
      />
    </div>
  );
}

export default {
  id: 'search-results',
  title: 'SearchResults',
  summary:
    'Grouped search results with counts, highlighted matches, states and arrow-key navigation.',
  sections: {
    purpose: {
      summary:
        'The body of a search page: results grouped by type with counts, the query words highlighted, and the loading, empty and error states every search needs, with keyboard navigation built in. Needed by: Rook Intellect (workspace search page with Documents/Brands/Organizations groups), Rook Dossier (knowledge home search with `<mark>` snippets).',
      useWhen: [
        'A dedicated search page or panel showing results across several types.',
        'A single flat list of results that should highlight the query and handle its own states (`results`).',
      ],
      avoidWhen: [
        'Jump-to navigation in an overlay: use CommandPalette or QuickSwitcher.',
        'Results that are records with columns to sort: use DataTable.',
        'Filtering a list already on screen: use FilterBar or TabsFilter.',
      ],
    },
    designPrinciples: {
      items: [
        'Every state is designed: skeletons while loading, an empty state that names the query, an error with retry, and dimmed results while refreshing.',
        'One live summary ("6 results for “travel”") tells screen-reader users what happened without moving focus.',
        'The whole result list is one Tab stop; arrow keys move within it, as in a listbox, but results stay real links so they open in new tabs and work without JavaScript.',
        'Group counts are the total matches, not just the ones shown, with "Show all" when there are more.',
        'Highlighting is text-only (SearchResultItem), so result data cannot inject markup.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'Forwards its ref to the root `<div>`; other div attributes pass through.',
        'Give `groups` for grouped results or `results` for one flat list. Groups with no items are not rendered.',
        'State precedence: `error` → results (dimmed while `loading`) → skeletons while `loading` → empty.',
        'Each result is a `SearchResultData` (`id`, `title`, `href?`, `path?`, `snippet?`, `titleRanges?`, `snippetRanges?`, `snippetMarkTags?`, `icon?`, `meta?`) rendered with SearchResultItem.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        'Migrating from app code: the apps build this by hand from `EmptyState`, `ListSkeleton`, `ErrorPanel` and `<section>`/`<ul>` per group (for example `rookintellect-frontend/src/app/(workspace)/w/[slug]/search/page.tsx`). Map `isFetching && !results.length` to `loading`, `error` + `refetch` to `error` + `onRetry`, the GROUPS table to `groups`, and Next.js `Link` to `linkComponent`. There is no shadcn/ui component for this.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'groups / results',
          values: ['grouped', 'flat'],
          description: 'Labelled sections with headings and counts, or one list without headings.',
        },
        {
          prop: 'headingLevel',
          values: ['h2', 'h3', 'h4', 'h5', 'h6'],
          description: 'Heading level of group labels and the empty-state title. Default `h2`.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Ready',
          attribute: 'data-state="ready"',
          description: 'Results are shown with the visible summary.',
        },
        {
          name: 'Loading',
          attribute: 'data-state="loading", aria-busy="true"',
          description:
            'No results yet: skeleton rows (`loadingRows`) and a hidden "Searching…" status.',
        },
        {
          name: 'Refreshing',
          attribute: 'data-refreshing, aria-busy="true"',
          description: 'New results are loading while the previous ones stay visible and dimmed.',
        },
        {
          name: 'Empty',
          attribute: 'data-state="empty"',
          description: 'An EmptyState naming the query, or your `empty` content.',
        },
        {
          name: 'Error',
          attribute: 'data-state="error"',
          description: 'A danger Alert with the message and, with `onRetry`, a "Try again" button.',
        },
      ],
    },
    interaction: {
      items: [
        'Clicking a result opens its link; `onSelect(result, group)` runs first.',
        'Focus position is remembered: Tab back into the list returns to the last focused result.',
        '"Show all N in Group" calls the group’s `onShowMore` when the group has more matches than it shows.',
      ],
    },
    keyboard: {
      items: [
        { keys: 'Tab', action: 'Moves into the results (to the current result) and out again.' },
        {
          keys: 'Arrow Down / Arrow Up',
          action: 'Moves to the next or previous result, across groups.',
        },
        { keys: 'Home / End', action: 'Moves to the first or last result.' },
        { keys: 'Enter', action: 'Opens the focused result.' },
      ],
    },
    accessibility: {
      role: 'region per group (section labelled by its heading); links or buttons per result',
      name: 'Each group is named by its heading (label and count).',
      items: [
        'A `role="status"` summary announces the result count, "Searching…", or the empty message.',
        'Errors are announced with `role="alert"`.',
        'Roving `tabIndex` keeps one result in the Tab order; results remain ordinary links for screen-reader link lists.',
        'Skeletons are hidden from assistive technology; the root has `aria-busy` while loading.',
      ],
    },
    responsive: {
      items: [
        'Single column at every width; result rows wrap their text and move meta below at narrow widths, so nothing scrolls sideways at 390px.',
        'Rows and the show-all button are at least 44px tall on coarse pointers.',
      ],
    },
    theme: {
      items: [
        'Group headings use `--mn-color-border-subtle` dividers and a `--mn-color-bg-muted` count pill.',
        'The refreshing dim is an opacity change, with no transition under reduced motion.',
        'States reuse Alert, EmptyState and Skeleton, so they follow those components’ tokens.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Grouped results',
          stack: true,
          render: () => <SearchResults groups={GROUPS} query={fixture.data.query} />,
        },
        {
          title: 'Flat list with backend highlights',
          stack: true,
          render: () => (
            <SearchResults
              query="budget"
              results={[
                {
                  id: 'r1',
                  title: 'Budget review',
                  href: '#budget-review',
                  path: ['Finance'],
                  snippet: 'Notes from the <mark>budget</mark> review for next quarter.',
                  snippetMarkTags: true,
                },
                {
                  id: 'r2',
                  title: 'Event budget template',
                  href: '#template',
                  path: ['Templates'],
                },
              ]}
            />
          ),
        },
        { title: 'Loading, empty and error states', stack: true, render: () => <StatesDemo /> },
      ],
    },
    doDont: {
      do: [
        'Pass `total` when a group shows only the first few matches, and `onShowMore` to see the rest.',
        'Keep showing old results with `loading` while a new query runs.',
        'Pass your router link as `linkComponent` for client-side navigation.',
      ],
      dont: [
        'Render your own spinner or "no results" text beside it; the states are built in.',
        'Put interactive controls inside result titles.',
        'Use it for a command palette; arrow keys here move focus, not a virtual selection.',
      ],
    },
    related: {
      items: [
        { id: 'search-result-item', reason: 'The row each result is rendered with.' },
        { id: 'search-input', reason: 'The field that drives the query.' },
        { id: 'faceted-filter-panel', reason: 'Facet filters beside the results.' },
        { id: 'command-palette', reason: 'Quick search in an overlay.' },
        { id: 'empty-state', reason: 'Used for the no-results state.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: ['alert', 'button', 'empty-state', 'skeleton', 'search-result-item'],
    },
    testing: {
      unit: [
        'Groups are regions named by label and count; empty groups are skipped; live summary; highlights; show-all button.',
        'One Tab stop; Arrow Down/Up, Home and End move across groups and update the roving tab index.',
        'Enter calls onSelect with the result and group.',
        'Loading skeleton with aria-busy, refreshing keeps results, empty state names the query, error with retry.',
        'Message overrides and server rendering.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
