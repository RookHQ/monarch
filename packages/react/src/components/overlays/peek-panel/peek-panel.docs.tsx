import { useState } from 'react';
import { Button, PeekPanel } from '@rookcom/monarch-react';
import type { ComponentDocs } from '../../../internal/component-docs.js';
// The scenario of the primary example, shared with iOS and Android (CROSS-PLATFORM.md section 8).
import fixture from './peek-panel.fixture.json';

const ORDERS = fixture.data.orders;

function OrdersWithPreview() {
  const [index, setIndex] = useState<number | null>(fixture.data.selected);
  const order = index === null ? null : ORDERS[index];
  return (
    <div style={{ display: 'flex', flexWrap: 'wrap', alignItems: 'flex-start', gap: 16 }}>
      <ul style={{ display: 'grid', gap: 4, margin: 0, padding: 0, listStyle: 'none' }}>
        {ORDERS.map((o, i) => (
          <li key={o.id}>
            <Button
              variant={i === index ? 'secondary' : 'ghost'}
              aria-current={i === index || undefined}
              onClick={() => setIndex(i)}
            >
              {o.id}
            </Button>
          </li>
        ))}
      </ul>
      <PeekPanel
        open={order != null}
        onOpenChange={(open: boolean) => !open && setIndex(null)}
        title={order?.id}
        description={order?.status}
        onPrevious={() => setIndex((i) => (i ?? 0) - 1)}
        onNext={() => setIndex((i) => (i ?? 0) + 1)}
        hasPrevious={index !== null && index > 0}
        hasNext={index !== null && index < ORDERS.length - 1}
        position={index === null ? undefined : { index: index + 1, total: ORDERS.length }}
        href="#orders"
        footer={<Button variant="primary">{fixture.data.footer}</Button>}
      >
        <p>Total {order?.total}</p>
      </PeekPanel>
    </div>
  );
}

export default {
  id: 'peek-panel',
  title: 'PeekPanel',
  summary:
    'A non-modal side panel that previews a record next to its list, with previous and next.',
  sections: {
    purpose: {
      summary:
        'Lets people look at a record without leaving the list: the preview opens at the side, they step to the previous or next record, and closing it puts them back on the row they came from. Needed by: Rook People (`src/components/documents/DocumentDetailPanel.tsx` builds its own detail panel next to the documents list).',
      useWhen: [
        'Triage: reading records one after another from a list or table.',
        'A quick look at a record whose full page has more than people need right now.',
      ],
      avoidWhen: [
        'Editing that must be finished or cancelled before doing anything else: use Sheet or Dialog (modal).',
        'A permanent list-and-detail screen: use SplitInboxLayout or ResizablePanels.',
        'Phones, where a full page is clearer: link to the record page (PeekPanel becomes full screen there anyway).',
      ],
    },
    designPrinciples: {
      items: [
        'Keeps list context: non-modal, no backdrop, no focus trap, no scroll lock. The list behind stays clickable.',
        'Escape and the close button return focus to the row that opened it.',
        'Stepping through records never loses focus: the previous and next buttons use `aria-disabled` at the ends instead of `disabled`.',
        'The full record is one link away ("Open full record"), checked with `isSafeUrl`.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'Forwards its ref to the `<section role="dialog" aria-modal="false">`; native attributes pass through.',
        'Controlled only: the list owns which record is open. Render it always and toggle `open`.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        'Role choice: a non-modal `dialog` (WAI-ARIA APG) rather than `complementary`, because it opens on demand, takes focus and closes with Escape; `aria-modal="false"` tells assistive technology the page behind is still available.',
        '`linkComponent` renders the full-record link with your router, e.g. Next.js `Link`.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'placement',
          values: ['overlay', 'inline'],
          description:
            '`overlay` (default) floats at the inline end of the viewport; `inline` is a normal block for a split layout. `data-placement`.',
        },
        {
          prop: 'size',
          values: ['sm', 'md', 'lg'],
          description:
            'Overlay width 24, 32 (default) or 44rem, capped at the screen. `data-size`.',
        },
      ],
    },
    states: {
      items: [
        { name: 'Closed', description: 'Nothing is rendered.' },
        {
          name: 'At the first or last record',
          attribute: 'aria-disabled="true" (previous / next)',
          description: 'The button stays focusable but does nothing.',
        },
        {
          name: 'Focus',
          description:
            'The panel itself receives focus on open (`tabindex="-1"`) and shows the focus ring on `:focus-visible`.',
        },
      ],
    },
    interaction: {
      items: [
        'Opening moves focus to the panel (unless `focusOnOpen` is false) and remembers the element that had focus.',
        'The previous and next buttons call `onPrevious` and `onNext`; the heading is a polite live region, so the new title is announced.',
        'Closing with Escape or the close button calls `onOpenChange(false)` and returns focus to the remembered element. Closing from outside (another row) leaves focus alone.',
        'Escape is ignored while focus is on a control whose own popup is open.',
      ],
    },
    keyboard: {
      items: [
        { keys: 'Escape', action: 'Closes the panel and returns focus to the row.' },
        { keys: 'J / Alt+ArrowDown', action: 'Next record (J not while typing in a field).' },
        { keys: 'K / Alt+ArrowUp', action: 'Previous record (K not while typing in a field).' },
        {
          keys: 'Tab',
          action: 'Moves through the panel and on to the rest of the page; there is no trap.',
        },
      ],
      notes: [
        'Shortcuts work only while focus is inside the panel; turn them off with `shortcuts={false}`. They are exposed with `aria-keyshortcuts`.',
      ],
    },
    accessibility: {
      role: 'dialog (aria-modal="false")',
      name: 'The `title` heading (`aria-labelledby`); `description` becomes the description.',
      items: [
        'The title is an `<h2>` so the panel appears in the heading outline.',
        'Icon buttons have names from `messages` ("Previous", "Next", "Close preview").',
        'The position ("2 of 3") is text, not colour or a progress bar.',
      ],
    },
    responsive: {
      items: [
        'Below 640px the overlay takes the full screen width.',
        'It keeps clear of notches and the home bar with the safe-area insets.',
        'On coarse pointers the header buttons and link are 44px tall.',
        'Uses logical properties and a mirrored slide-in, so it opens from the left in right-to-left layouts.',
      ],
    },
    theme: {
      items: [
        'Surface `--mn-color-bg-raised`, border `--mn-color-border-subtle`, shadow `--mn-shadow-xl`, layer `--mn-z-rail` (below menus and popovers).',
        'The slide-in uses `--mn-duration-slow`; reduced motion removes it.',
        'In forced colours the panel gets a `CanvasText` border, buttons `ButtonText`, the link `LinkText`.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Preview orders from a list',
          stage: true,
          render: () => <OrdersWithPreview />,
        },
      ],
    },
    doDont: {
      do: [
        'Pass `position` so people know where they are in the list.',
        'Give `href` so the full record is one step away.',
        'Turn `focusOnOpen` off when people arrow through a table and the panel follows the selection.',
      ],
      dont: [
        'Don’t put a form that must be saved or discarded in a PeekPanel; use a modal Sheet.',
        'Don’t open a PeekPanel from inside another PeekPanel.',
        'Don’t hide the list behind a backdrop; that defeats the purpose.',
      ],
    },
    related: {
      items: [
        { id: 'sheet', reason: 'A modal side panel for tasks that need full attention.' },
        { id: 'drawer', reason: 'A modal bottom panel for phones with drag-to-close.' },
        { id: 'split-inbox-layout', reason: 'A permanent list-and-detail screen.' },
        { id: 'data-table', reason: 'The list a PeekPanel usually previews from.' },
      ],
    },
    dependencies: { packages: ['react'], components: [] },
    testing: {
      unit: [
        'Opens as a named non-modal dialog with focus inside, position and full-record link; the list stays usable.',
        'Steps through records with the buttons, keeping focus at the ends with `aria-disabled`.',
        'J/K and Alt+Arrow shortcuts, but not J/K while typing in a field.',
        'Escape and the close button close it and return focus to the row.',
        '`focusOnOpen={false}` leaves focus on the list; labels translate through `messages`.',
        'Does not link unsafe URLs; renders nothing when closed.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
