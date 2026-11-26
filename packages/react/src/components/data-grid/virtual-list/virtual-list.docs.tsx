import { VirtualList } from '@rookcom/monarch-react/data';
import type { ComponentDocs } from '../../../internal/component-docs.js';

interface Contact {
  id: string;
  name: string;
  team: string;
}

const TEAMS = ['Design', 'Finance', 'Support', 'Engineering'];
const CONTACTS: Contact[] = Array.from({ length: 5000 }, (_, i) => ({
  id: `c${i}`,
  name: `Contact ${i + 1}`,
  team: TEAMS[i % TEAMS.length] ?? 'Design',
}));

function ContactRow({ contact }: { contact: Contact }) {
  return (
    <div style={{ display: 'flex', justifyContent: 'space-between', padding: '10px 12px' }}>
      <span>{contact.name}</span>
      <span style={{ color: 'var(--mn-color-text-muted)' }}>{contact.team}</span>
    </div>
  );
}

export default {
  id: 'virtual-list',
  title: 'VirtualList',
  summary: 'A windowed list that renders only the items near the view, with measured heights.',
  sections: {
    purpose: {
      summary:
        'Long collections (thousands of contacts, tickets, log lines) stay fast because only the items near the view are in the DOM. Heights are measured, so items can vary. The `useVirtualizer` hook behind it also powers DataGrid. Needed by: Tickets (inbox list, today a hand-rolled fixed-height window in `components/inbox/InboxList.tsx`), Chat (VirtualMessageList shares the approach).',
      useWhen: [
        'A list can grow past a few hundred items.',
        'Infinite paging: `onEndReached` asks for the next page near the end.',
        'A custom windowed layout: use `useVirtualizer` directly.',
      ],
      avoidWhen: [
        'Fewer than ~200 items: render a plain list (find-in-page and printing then see everything).',
        'Tabular data with columns: use DataGrid (virtualized) or DataTable.',
        'Chat history that sticks to the bottom: use VirtualMessageList.',
      ],
    },
    designPrinciples: {
      items: [
        'The DOM holds a window, the semantics hold the whole list: every item carries `aria-setsize` and `aria-posinset`.',
        'Heights are measured with ResizeObserver and kept by key, so re-sorting keeps them.',
        'Server rendering and first paint show the first `initialRenderCount` items.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        '`useVirtualizer({ count, getScrollElement, estimateSize, getItemKey?, overscan?, initialCount?, scrollMargin?, keepMounted? })` returns `{ items, totalSize, measureElement, scrollToIndex, getOffset, visibleRange }`.',
        '`handleRef` exposes `scrollToIndex(index, { align, behavior })` and `scrollToKey(key)`.',
        'The ref goes to the scrolling element.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        'Migrating from the Tickets inbox window (`visibleRange`/`scrollToRow` with a fixed `ROW_HEIGHT`): `items` + `renderItem` replace the manual slice; `estimatedItemHeight={ROW_HEIGHT}`; the `fetchNextPage` effect becomes `onEndReached` with `loading={isFetchingNextPage}`; `scrollToRow` becomes `handleRef.current.scrollToIndex(i)`.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'semantics',
          values: ['list', 'none'],
          description: '`none` for items that bring their own semantics (articles, cards).',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Empty',
          attribute: 'data-empty',
          description: 'No items: the `empty` content shows.',
        },
        {
          name: 'Loading',
          attribute: 'data-loading',
          description: 'A "Loading more" row at the end, or a spinner status when empty.',
        },
      ],
    },
    interaction: {
      items: [
        'Scrolling renders the items that come into view; items that leave are removed.',
        '`onEndReached` fires once per list length when the end is within `endReachedThreshold` px.',
      ],
    },
    keyboard: {
      items: [
        { keys: 'Tab', action: 'Focuses the scroll area (then the controls inside items).' },
        {
          keys: 'ArrowUp / ArrowDown / PageUp / PageDown / Home / End',
          action: 'Scroll the list.',
        },
      ],
    },
    accessibility: {
      role: 'group containing a list of listitems',
      name: '`label` names the group.',
      items: [
        'Each item has `aria-setsize` (the full count) and `aria-posinset`, so screen readers announce "item 5,001 of 10,000".',
        'Loading is announced through a status region.',
        'The scroll area is focusable so keyboard users can scroll it.',
      ],
    },
    responsive: {
      items: [
        'Fills its parent by default; set `height` for a fixed area.',
        'Items stretch to the width, so it works at 390 px.',
      ],
    },
    theme: {
      items: [
        'Only a focus ring and the loading row are styled; items are yours.',
        'Forced colours: the focus ring uses Highlight.',
      ],
    },
    examples: {
      items: [
        {
          title: '5,000 contacts',
          stack: true,
          render: () => (
            <VirtualList
              label="Contacts"
              items={CONTACTS}
              getItemKey={(c) => c.id}
              height={320}
              estimatedItemHeight={41}
              renderItem={(c) => <ContactRow contact={c} />}
            />
          ),
        },
        {
          title: 'Empty',
          render: () => (
            <VirtualList
              label="Contacts"
              items={[] as Contact[]}
              getItemKey={(c) => c.id}
              height={120}
              renderItem={(c) => <ContactRow contact={c} />}
              empty={<p style={{ padding: 16 }}>No contacts yet.</p>}
            />
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Give every item a stable key from your data.',
        'Set `estimatedItemHeight` close to the real height to keep the scrollbar steady.',
      ],
      dont: [
        'Do not put the list in a container without a height: it then renders everything.',
        'Do not rely on Ctrl+F for items that are not rendered; offer search.',
      ],
    },
    related: {
      items: [
        { id: 'data-grid', reason: 'Virtualized grid with columns, built on the same hook.' },
        { id: 'virtual-message-list', reason: 'Chat history that sticks to the bottom.' },
        { id: 'load-more', reason: 'A button instead of automatic paging.' },
      ],
    },
    dependencies: { packages: ['react'], components: ['spinner'] },
    testing: {
      unit: [
        'Renders a window of 10,000 items with list semantics and positions; focusable scroll area.',
        'Follows the scroll position; scrollToKey; onEndReached once per length; loading and empty states; semantics="none".',
        'useVirtualizer keeps requested indexes mounted and computes offsets.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
