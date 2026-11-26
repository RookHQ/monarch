import { Badge } from '@rookcom/monarch-react';
import { HighlightText, SearchResultItem, queryTerms } from '@rookcom/monarch-react/data';
import type { ComponentDocs } from '../../../internal/component-docs.js';
import { CalendarIcon, InboxIcon, UsersIcon } from '../../../internal/docs-demo.js';

export default {
  id: 'search-result-item',
  title: 'SearchResultItem',
  summary: 'One search result: title link, breadcrumb path, highlighted snippet, icon and meta.',
  sections: {
    purpose: {
      summary:
        'Shows one search hit so people can judge it before opening it: what it is, where it lives and why it matched, with the matched words marked. Highlighting is done on plain text, so result data can never inject markup. Needed by: Rook Intellect (workspace search page), Rook Dossier (knowledge search snippets), Rook Chat (message search highlights).',
      useWhen: [
        'Rendering a single result in a custom results layout.',
        'Showing why something matched, with the query words or backend match ranges marked.',
        'Highlighting matches anywhere else with `HighlightText` (for example in a table cell).',
      ],
      avoidWhen: [
        'A full results page with groups, states and keyboard navigation: use SearchResults, which renders these items for you.',
        'Results inside a command palette: CommandPalette has its own item rows.',
      ],
    },
    designPrinciples: {
      items: [
        'Text in, text out: titles, paths and snippets are strings, and highlights are `<mark>` elements built from them, never HTML from the server.',
        'One target per result: the title is the link (or button), and the whole row is clickable through a stretched hit area, so there is one Tab stop and one accessible name.',
        'Matches are marked by weight and a tinted background, not colour alone.',
        'The path and snippet describe the link (`aria-describedby`), so a screen reader hears the title first and the context after.',
        'No router dependency: pass your router link as `linkComponent`.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        '`SearchResultItem` forwards its ref to the root `<div>`; `targetRef` reaches the link or button.',
        '`HighlightText({ text, terms?, ranges?, markTags? })` renders text with `<mark>` around matches. `highlightSegments(text, options)` returns the runs as data, and `queryTerms(query)` splits what someone typed into the words to highlight.',
        'Highlight sources, in priority order: `markTags` (backend `<mark>` markers such as PostgreSQL `ts_headline`), then `ranges`, then `terms`.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        'Migrating from app code: Rook Chat’s `Highlight({ text, terms })` maps to `HighlightText` with the same props; Rook Dossier’s `Snippet html={hit.snippet}` maps to `snippet={hit.snippet}` with `snippetMarkTags`. The apps have no shadcn/ui equivalent; their results are hand-built `<Link>` rows.',
      ],
    },
    variants: {
      items: [],
      notes: [
        'No visual variants. The title renders as a link with a safe `href`, as a button with only `onSelect`, or as text with neither (`data-interactive` is set for the first two).',
      ],
    },
    states: {
      items: [
        {
          name: 'Interactive',
          attribute: 'data-interactive',
          description: 'The row has a link or button; it shows a hover background.',
        },
        {
          name: 'Focus',
          description:
            'When the title link has keyboard focus the whole row shows the Monarch focus ring and a subtle background.',
        },
        {
          name: 'Matched text',
          description: 'Matched runs are `<mark>` elements with semibold text on the warning tint.',
        },
      ],
    },
    interaction: {
      items: [
        'Clicking anywhere on the row activates the title link or button.',
        '`onSelect` runs before link navigation, for analytics or closing a search overlay.',
        'Content in `meta` sits above the stretched hit area, so buttons placed there stay clickable.',
        'Unsafe URLs (`javascript:`, `data:` and so on) are dropped; the title becomes a button if `onSelect` is set, or plain text.',
      ],
    },
    keyboard: {
      items: [
        { keys: 'Tab', action: 'Moves to the result’s link or button.' },
        { keys: 'Enter', action: 'Opens the link or activates the button.' },
        { keys: 'Space', action: 'Activates the button (button results only).' },
      ],
      notes: ['Arrow-key movement between results is provided by SearchResults.'],
    },
    accessibility: {
      role: 'link (or button)',
      name: 'The `title` text, including any highlighted parts.',
      items: [
        'The path and snippet are the target’s accessible description.',
        'The path is announced with a visually hidden "Location:" prefix (`messages.path`) and commas between segments; the visible "/" separators are hidden.',
        'The icon is decorative (`aria-hidden`).',
        'In forced-colors mode marks use the system `Mark`/`MarkText` colours and links use `LinkText`.',
      ],
    },
    responsive: {
      items: [
        'Title, path and snippet wrap long words (`overflow-wrap: anywhere`); the snippet is clamped to three lines.',
        'Below 30rem the meta moves under the text, aligned with it, so nothing scrolls sideways at 390px.',
        'On coarse pointers the row is at least 44px tall.',
      ],
    },
    theme: {
      items: [
        'Title uses `--mn-color-text-link`; path and snippet use `--mn-color-text-muted`.',
        'Marks use `--mn-color-warning-subtle` with `--mn-color-text-default`.',
        'Hover and focus use `--mn-color-bg-subtle` and `--mn-color-focus-ring`.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Link result with path and highlighted snippet',
          stack: true,
          render: () => (
            <SearchResultItem
              title="Travel policy"
              href="#travel-policy"
              path={['Handbook', 'Policies']}
              snippet="Book travel at least two weeks ahead and add the trip to the shared calendar."
              terms={queryTerms('travel calendar')}
              icon={<InboxIcon />}
              meta={<Badge>Document</Badge>}
            />
          ),
        },
        {
          title: 'Backend match ranges and mark markers',
          stack: true,
          render: () => (
            <SearchResultItem
              title="Quarterly planning notes"
              titleRanges={[[0, 9]]}
              href="#planning"
              path={['Team space', 'Meetings']}
              snippet="Agreed the <mark>quarterly</mark> goals and owners for each project."
              snippetMarkTags
              icon={<CalendarIcon />}
              meta="Updated 3 days ago"
            />
          ),
        },
        {
          title: 'Button result (no URL)',
          stack: true,
          render: () => (
            <SearchResultItem
              title="Alex Kim"
              snippet="Product designer · Design team"
              terms={['alex']}
              icon={<UsersIcon />}
              onSelect={() => {}}
            />
          ),
        },
        {
          title: 'HighlightText on its own',
          render: () => (
            <p>
              <HighlightText text="Shared calendar settings" terms={['calendar']} />
            </p>
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Pass plain text and let the component mark matches.',
        'Use `snippetMarkTags` when your backend already returns `<mark>` markers.',
        'Keep the path short: the two or three nearest containers.',
      ],
      dont: [
        'Render a backend snippet with `dangerouslySetInnerHTML`.',
        'Put a second link in the title; add extra actions in `meta`.',
        'Use colour alone to show a match.',
      ],
    },
    related: {
      items: [
        {
          id: 'search-results',
          reason: 'Lists these items in groups with states and keyboard navigation.',
        },
        { id: 'search-input', reason: 'The field that produces the query.' },
        { id: 'command-palette', reason: 'Quick search and commands in an overlay.' },
      ],
    },
    dependencies: { packages: ['react'], components: [] },
    testing: {
      unit: [
        'highlightSegments: case-insensitive terms, merged ranges, escaped regular-expression characters, `<mark>` markers only.',
        'Untrusted markup in text renders as text (no elements created).',
        'Link name and description (path and snippet); unsafe URLs fall back to a button; Enter activates.',
        'Router link component and translated path label.',
        'Server rendering.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
