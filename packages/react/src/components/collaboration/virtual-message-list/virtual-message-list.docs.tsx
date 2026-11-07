import { useRef, useState } from 'react';
import { Button } from '@rookcom/monarch-react';
import {
  MessageRow,
  VirtualMessageList,
  type VirtualMessageItem,
  type VirtualMessageListHandle,
} from '@rookcom/monarch-react/collaboration';
import type { ComponentDocs } from '../../../internal/component-docs.js';

interface DemoMessage extends VirtualMessageItem {
  author: string;
  body: string;
}

const PEOPLE = ['Alex Morgan', 'Sam Lee', 'Riley Chen'];
const LINES = [
  'Morning! Is the release still on for Thursday?',
  'Yes, the checklist is nearly done.',
  'I will update the notes after lunch.',
  'Can someone review the new onboarding copy?',
  'Looks good to me.',
  'Shipping the fix now.',
];
const START = new Date(2026, 4, 1, 9).getTime();

/** Built on demand: a long, deterministic history. */
function makeHistory(from: number, count: number): DemoMessage[] {
  return Array.from({ length: count }, (_, k) => {
    const n = from + k;
    const author = PEOPLE[Math.floor(n / 3) % PEOPLE.length] ?? 'Alex Morgan';
    return {
      id: `msg-${n}`,
      time: new Date(START + n * 7 * 60_000),
      authorId: author,
      author,
      body: `${LINES[n % LINES.length] ?? ''} (#${n + 1})`,
    };
  });
}

const NOW = new Date(START + 2000 * 7 * 60_000);

function LongHistoryDemo() {
  const [items, setItems] = useState(() => makeHistory(1500, 500));
  const [loadingOlder, setLoadingOlder] = useState(false);
  const handle = useRef<VirtualMessageListHandle>(null);
  const first = Number(items[0]?.id.slice(4) ?? 0);
  return (
    <div style={{ display: 'grid', gap: 'var(--mn-space-2)', width: '100%' }}>
      <div style={{ display: 'flex', gap: 'var(--mn-space-2)', flexWrap: 'wrap' }}>
        <Button
          size="sm"
          variant="secondary"
          onClick={() =>
            setItems((list) => [...list, ...makeHistory(Number(list.at(-1)?.id.slice(4)) + 1, 1)])
          }
        >
          Receive a message
        </Button>
        <Button size="sm" variant="ghost" onClick={() => handle.current?.scrollToItem('msg-1600')}>
          Go to message 1601
        </Button>
      </div>
      <div
        style={{
          height: '24rem',
          border: '1px solid var(--mn-color-border-subtle)',
          borderRadius: 'var(--mn-radius-lg)',
        }}
      >
        <VirtualMessageList
          items={items}
          handleRef={handle}
          now={NOW}
          firstUnreadId="msg-1995"
          hasOlder={first > 0}
          loadingOlder={loadingOlder}
          onLoadOlder={() => {
            setLoadingOlder(true);
            setTimeout(() => {
              setItems((list) => [
                ...makeHistory(Math.max(0, first - 200), Math.min(200, first)),
                ...list,
              ]);
              setLoadingOlder(false);
            }, 500);
          }}
          renderItem={(m, { grouped }) => (
            <MessageRow author={{ name: m.author }} time={m.time} grouped={grouped} now={NOW}>
              {m.body}
            </MessageRow>
          )}
        />
      </div>
    </div>
  );
}

export default {
  id: 'virtual-message-list',
  title: 'VirtualMessageList',
  summary: 'A windowed conversation log for long histories, with measured variable heights.',
  sections: {
    purpose: {
      summary:
        'Shows a conversation of thousands of messages smoothly by rendering only the messages near the view. Rows can be any height (images, link previews, code) and are measured as they render. It keeps MessageTimeline’s behaviour: day separators, a "New messages" divider, older pages loaded on scroll-up with the view held still, new messages followed at the bottom, a jump-to-latest button with a count, and deep links. Built without dependencies. Needed by: Chat (conversation MessageList, previously react-virtuoso).',
      useWhen: [
        'A conversation, thread or log can grow past a few hundred messages.',
        'Messages vary in height and new ones arrive live.',
      ],
      avoidWhen: [
        'Short conversations (up to a few hundred messages): MessageTimeline is simpler and keeps every message in the page for find-in-page.',
        'Uniform rows in a table: use a data table with its own virtualisation.',
      ],
    },
    designPrinciples: {
      items: [
        'The reader’s place is sacred: older pages and rows that change height above the view never move what is being read.',
        'Follow the bottom only when the reader is there; otherwise count what arrived and offer a way back.',
        'Quiet while scrolling: the log is not a live region, because rows mount as you scroll; arrivals are announced separately.',
        'Same API as MessageTimeline, so switching between them is a one-line change.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        '`renderItem(item, { grouped, index, previous })` renders each row, usually a MessageRow. Items need `id`, `time` and optionally `authorId`.',
        '`handleRef` gives `scrollToBottom()` and `scrollToItem(id)`; `scrollToItem` renders the row first if it is outside the window.',
        '`estimatedItemHeight`, `overscan` and `initialRenderCount` tune the windowing. `VIRTUAL_MESSAGE_LIST_MESSAGES` holds the English defaults.',
        'The list fills its parent: give the parent a height (a flex child with `min-height: 0` works).',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        'Migrating from the app copy (Chat `components/chat/MessageList.tsx`): drop react-virtuoso. `data` → `items` (add `time: createdAt` and `authorId`); `itemContent` → `renderItem`, with the day separator, "New" divider and `shouldGroup` handled by the list (`firstUnreadId` = the first message after `lastReadSeq`). `firstItemIndex` bookkeeping is no longer needed: prepend older pages to `items`. `startReached` → `onLoadOlder` with `hasOlder` and `loadingOlder` (`history.hasNextPage`, `isFetchingNextPage`); `atBottomStateChange` → `onAtBottomChange`; `followOutput` → `stickToBottom`; `atBottomThreshold` and `increaseViewportBy` → `atBottomThreshold` and `overscan`. The Header (intro) and Footer (pending messages, read receipt) become `header` and `footer`; `focusSeq` → `scrollToId` plus `highlightedId`. Loading and error screens become `loading` and `error`.',
      ],
    },
    variants: {
      items: [],
      notes: ['No visual variants; it looks exactly like MessageTimeline.'],
    },
    states: {
      items: [
        { name: 'Loading', description: 'Skeleton rows; the log is `aria-busy`.' },
        { name: 'Error', description: 'Your `error` content in place of the list.' },
        { name: 'Empty', description: '`empty`, or `header` when it is not given.' },
        {
          name: 'Loading older',
          description:
            'A loading line at the top; the log is `aria-busy` while the prepend settles.',
        },
        {
          name: 'Scrolled up',
          description:
            'A "Jump to latest" button, or "3 new messages" when messages arrived below.',
        },
        {
          name: 'Highlighted',
          attribute: 'data-highlighted on the row',
          description: 'A short flash for a deep-linked message (steady under reduced motion).',
        },
      ],
    },
    interaction: {
      items: [
        'Opens at the newest message, or at `scrollToId`.',
        'Scrolling near the top calls `onLoadOlder`; the prepended messages appear above without moving the view.',
        'New messages are followed while the reader is within `atBottomThreshold` of the bottom.',
        'Rows that grow after rendering (pictures loading) are re-measured and the view is kept steady.',
        'Jump to latest scrolls down and moves focus to the log.',
      ],
    },
    keyboard: {
      items: [
        { keys: 'Tab', action: 'Focuses the log, then the controls inside the rendered messages.' },
        {
          keys: 'Arrow Up / Arrow Down, Page Up / Page Down, Home / End',
          action: 'Scroll the focused log; rows render as they come into view.',
        },
        {
          keys: 'Enter on Jump to latest',
          action: 'Scrolls to the newest message and focuses the log.',
        },
      ],
    },
    accessibility: {
      role: 'log (with aria-live="off") plus a polite status',
      name: '"Messages" (messages.label)',
      items: [
        'The scroller is focusable so keyboard users can scroll it.',
        'Day separators and the "New messages" divider are separators with names.',
        'Arriving messages are announced as "2 new messages" through the status, together with your `status` content (a typing indicator).',
        'Messages outside the window are not in the page, so find-in-page and screen-reader browse mode only see the rendered part; offer search for long histories.',
      ],
    },
    responsive: {
      items: [
        'Fills its parent’s width and height; rows wrap their own content at 390px.',
        'The jump button is 44px tall on coarse pointers.',
        'Window size follows the view height, which is re-read when the list resizes.',
      ],
    },
    theme: {
      items: [
        'Separators use `--mn-color-border-subtle` and `--mn-color-text-muted`; the unread divider `--mn-color-danger-strong` and `--mn-color-danger-border`.',
        'Jump button: `--mn-color-bg-raised`, `--mn-color-border-default`, `--mn-shadow-md`.',
        'In forced colours separators draw in CanvasText and the highlight becomes an outline; animations stop under reduced motion.',
      ],
    },
    examples: {
      items: [{ title: '2,000-message history', stack: true, render: () => <LongHistoryDemo /> }],
    },
    doDont: {
      do: [
        'Give stable ids; they key the measurements and the scroll anchor.',
        'Prepend older pages and append new messages; do not re-sort the array.',
        'Set `estimatedItemHeight` close to a typical row to reduce settling.',
      ],
      dont: [
        'Do not wrap it in another scrolling element.',
        'Do not use it for a few dozen messages; MessageTimeline is simpler.',
        'Do not make the log live yourself; rows mounting while scrolling would be read out.',
      ],
    },
    related: {
      items: [
        { id: 'message-timeline', reason: 'The non-virtualised version with the same API.' },
        { id: 'message-row', reason: 'The usual row.' },
        { id: 'typing-indicator', reason: 'Goes in `status`.' },
        { id: 'read-receipt', reason: 'Goes in `footer`.' },
      ],
    },
    dependencies: { packages: ['react'], components: ['load-more', 'skeleton'] },
    testing: {
      unit: [
        'Before layout and on the server: the newest rows only, with spacer padding, separators and grouping.',
        'Loading, error and empty states.',
        'With layout: windowing around the view, following the bottom, arrival announcement.',
        'Loading older near the top, leaving the bottom, jump button with count and focus.',
        'Holding the view still when older messages are prepended; scrolling to an item through the handle.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
