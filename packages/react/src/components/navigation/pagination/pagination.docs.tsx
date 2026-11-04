import { useState } from 'react';
import { Pagination } from '@rookcom/monarch-react';
import { incomplete, type ComponentDocs } from '../../../internal/component-docs.js';

function PaginationDemo({ compact = false }: { compact?: boolean }) {
  const [page, setPage] = useState(5);
  return compact ? (
    <Pagination
      variant="compact"
      label="Compact pagination"
      page={page}
      pageCount={8}
      pageSize={20}
      totalItems={143}
      onPageChange={setPage}
    />
  ) : (
    <Pagination page={page} pageCount={20} onPageChange={setPage} />
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
        'Pagination is a function component; it does not forward a ref.',
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
      name: '`label` prop, default "Pagination".',
      items: [
        'Page buttons are named "Page N"; arrows "Previous page" and "Next page".',
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
          code: `const [page, setPage] = useState(5);
<Pagination page={page} pageCount={20} onPageChange={setPage} />
<Pagination
  variant="compact"
  label="Compact pagination"
  page={page}
  pageCount={8}
  pageSize={20}
  totalItems={143}
  onPageChange={setPage}
/>`,
        },
      ],
    },
    doDont: {
      do: [
        'Pass `pageSize` and `totalItems` to the compact variant so it shows the item range.',
        'Give each Pagination on one page a distinct `label`, so the navigation landmarks can be told apart.',
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
      packages: [],
      components: ['button'],
    },
    testing: {
      unit: [
        '`pageWindow` returns a stable window with gaps.',
        'Is a labelled navigation; the current page has aria-current; arrows and page buttons call onPageChange.',
        'Compact variant shows the item range and disables previous on the first page.',
        'No axe violations in jsdom.',
        'Renders on the server.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
