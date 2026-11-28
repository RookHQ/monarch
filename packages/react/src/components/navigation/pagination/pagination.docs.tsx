import { useState } from 'react';
import { Pagination } from '@rookcom/monarch-react';
import { incomplete, type ComponentDocs } from '../../../internal/component-docs.js';
// The scenario of the primary example, shared with iOS and Android (CROSS-PLATFORM.md section 8).
import fixture from './pagination.fixture.json';

function PaginationDemo({ compact = false }: { compact?: boolean }) {
  const [page, setPage] = useState(fixture.data.page);
  return compact ? (
    <Pagination
      variant="compact"
      label={fixture.data.compact.label}
      page={page}
      pageCount={fixture.data.compact.pageCount}
      pageSize={fixture.data.compact.pageSize}
      totalItems={fixture.data.compact.totalItems}
      onPageChange={setPage}
    />
  ) : (
    <Pagination page={page} pageCount={fixture.data.full.pageCount} onPageChange={setPage} />
  );
}

export default {
  id: 'pagination',
  title: 'Pagination',
  summary: 'Numbered or compact page navigation for long lists.',
  sections: {
    purpose: {
      summary: 'Moves through a list that is split into pages.',
      useWhen: [
        'A table or list is paged on the server or client and the user jumps between pages.',
      ],
      avoidWhen: ['The list grows as the user scrolls or asks for more: use a load-more control.'],
    },
    designPrinciples: {
      items: [
        'Controlled: the app owns `page` and fetches data in `onPageChange`.',
        'The full variant always shows the first and last page and a window around the current one (`pageWindow`).',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        '`pageWindow(page, pageCount, radius = 1)` is exported for apps that render their own page list; it returns page numbers and `"gap"` markers.',
        'Pagination forwards its ref to the `<nav>`; other HTML attributes (`id`, `data-*`, `aria-*`) pass through to it as well.',
        '`messages` takes any subset of `PaginationMessages`; missing keys fall back to `PAGINATION_MESSAGES` (English). Defaults: `label` "Pagination", `previous` "Previous page", `next` "Next page", `page(n)` "Page n", `range(start, end, total)` "1–20 of 143", `pageOf(page, pageCount)` "Page 1 of 8".',
        'Message functions receive numbers already formatted for `locale`, as strings.',
        '`locale` is a BCP 47 tag used to format page numbers and counts. Without it, numbers use `en-US` on the server and first render, then the runtime locale once hydrated.',
        'The `label` prop wins over `messages.label`.',
      ],
    },
    typedProperties: { source: 'typescript' },
    variants: {
      items: [
        {
          prop: 'variant',
          values: ['full', 'compact'],
          description:
            '`full` (default) shows numbered pages between previous/next buttons; `compact` shows "1–20 of 143" (or "Page 1 of 8" without `pageSize` and `totalItems`) with the arrows.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Current page',
          attribute: 'aria-current="page"',
          description: 'Highlighted page button.',
        },
        { name: 'First / last page', description: 'Previous or next button is disabled.' },
        { name: 'Gap', description: 'Skipped ranges render as an `aria-hidden` "…".' },
      ],
    },
    interaction: {
      items: [
        'Previous, next and page buttons call `onPageChange` with the target page.',
        'Seven or fewer pages are all shown; more pages collapse into gaps.',
        'The arrow icons point the reading direction, so they mirror in right-to-left layouts.',
      ],
    },
    keyboard: {
      items: [
        { keys: 'Tab', action: 'Moves through the previous, page and next buttons.' },
        { keys: 'Enter / Space', action: 'Goes to that page.' },
      ],
    },
    accessibility: {
      role: 'navigation',
      name: '`label` prop, then `messages.label`, default "Pagination".',
      items: [
        'Page buttons are named "Page N"; arrows "Previous page" and "Next page". All three names come from `messages`, so they can be translated.',
        'The compact range text is a polite live region, so page changes are announced.',
      ],
    },
    responsive: incomplete(
      'No width-specific rules; use the compact variant on narrow layouts. Wrapping of the full variant is not specified.',
    ),
    theme: {
      items: [
        'Colours are Monarch semantic tokens only, so light, dark and more-contrast themes apply automatically.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Full and compact',
          stack: true,
          render: () => (
            <>
              <PaginationDemo />
              <PaginationDemo compact />
            </>
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Pass `pageSize` and `totalItems` to the compact variant so it shows the item range.',
        'Give each Pagination on one page a distinct `label`, so the navigation landmarks can be told apart.',
        'Pass `messages` and `locale` together when the interface is translated, so the words and the numbers match.',
      ],
      dont: ['Do not mix pagination and infinite scroll on the same list.'],
    },
    related: {
      items: [
        { id: 'load-more', reason: 'Appends the next page instead of replacing it.' },
        { id: 'data-table', reason: 'The list Pagination usually controls.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: ['button'],
    },
    testing: {
      unit: [
        '`pageWindow` returns a stable window with gaps.',
        'Is a labelled navigation; the current page has aria-current; arrows and page buttons call onPageChange.',
        'Compact variant shows the item range and disables previous on the first page.',
        'Translates every string through `messages` and formats numbers for `locale`, including the compact range.',
        'Forwards the ref and rest props to the nav.',
        'Marks the arrow icons so CSS can mirror them in RTL.',
        'No axe violations in jsdom.',
        'Renders on the server.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
