import { useCallback, useState } from 'react';
import { Button } from '@rookcom/monarch-react';
import {
  MessageRow,
  MessageTimeline,
  type MessageTimelineItem,
} from '@rookcom/monarch-react/collaboration';
import type { ComponentDocs } from '../../../internal/component-docs.js';

const NOW = Date.parse('2026-03-02T12:00:00Z');
const PEOPLE: Record<string, string> = {
  ana: 'Ana Lopez',
  ben: 'Ben Okafor',
  chloe: 'Chloé Martin',
};
const LINES = [
  'Morning! Is the launch checklist up to date?',
  'Mostly. Legal sign-off is still open.',
  'I’ll chase it after standup.',
  'Thanks. The press kit is in the shared folder.',
  'Looks good. One typo on page two.',
  'Fixed, and the screenshots are refreshed.',
  'Great. Shall we freeze copy at 3pm?',
  'Works for me.',
];

type Demo = MessageTimelineItem & { text: string };

/** Minutes before `endAt` for each message: pairs close together group, the first ones fall on the previous day. */
const OFFSETS = [960, 957, 300, 240, 238, 120, 30, 28];
const OLDER_OFFSETS = [60, 58, 20, 18];

function history(count: number, endAt = NOW - 30 * 60_000): Demo[] {
  const authors = ['ana', 'ana', 'ben', 'chloe', 'chloe', 'ana', 'ben', 'ben'];
  const offsets = count === OFFSETS.length ? OFFSETS : OLDER_OFFSETS;
  return Array.from({ length: count }, (_, i) => ({
    id: `m${endAt}-${i}`,
    time: endAt - (offsets[i] ?? 0) * 60_000,
    authorId: authors[i % authors.length] ?? 'ana',
    text: LINES[i % LINES.length] ?? '',
  }));
}

const renderMessage = (m: Demo, ctx: { grouped: boolean }) => (
  <MessageRow
    author={{ name: PEOPLE[m.authorId ?? ''] ?? 'Someone', seed: m.authorId ?? 'unknown' }}
    time={m.time}
    now={NOW}
    grouped={ctx.grouped}
    timeDisplay="clock"
    timeZone="UTC"
  >
    {m.text}
  </MessageRow>
);

function LiveDemo() {
  const [items, setItems] = useState<Demo[]>(() => history(8));
  const [older, setOlder] = useState(2);
  const [loadingOlder, setLoadingOlder] = useState(false);
  const loadOlder = useCallback(() => {
    setLoadingOlder(true);
    setTimeout(() => {
      setItems((list) => {
        const first = Number(list[0]?.time ?? NOW);
        const page = history(4, first - 3 * 3_600_000).map((m, i) => ({
          ...m,
          id: `o${older}-${i}`,
        }));
        return [...page, ...list];
      });
      setOlder((n) => n - 1);
      setLoadingOlder(false);
    }, 700);
  }, [older]);
  const send = () =>
    setItems((list) => [
      ...list,
      { id: `n${list.length}`, time: NOW, authorId: 'ana', text: 'New message from the demo.' },
    ]);
  return (
    <div style={{ width: '100%', display: 'grid', gap: 8 }}>
      <div
        style={{
          height: 340,
          border: '1px solid var(--mn-color-border-subtle)',
          borderRadius: 'var(--mn-radius-lg)',
          overflow: 'hidden',
        }}
      >
        <MessageTimeline
          items={items}
          renderItem={renderMessage}
          firstUnreadId={items[items.length - 2]?.id ?? null}
          hasOlder={older > 0}
          loadingOlder={loadingOlder}
          onLoadOlder={loadOlder}
          header={<p style={{ margin: 0, fontSize: 13 }}>This is the start of #launch.</p>}
          status="Ben Okafor is typing…"
          now={NOW}
          timeZone="UTC"
        />
      </div>
      <div>
        <Button size="sm" onClick={send}>
          Add a message
        </Button>
      </div>
    </div>
  );
}

export default {
  id: 'message-timeline',
  title: 'MessageTimeline',
  summary:
    'Scrolling conversation: day separators, a “New messages” divider, older history on scroll-up and stick-to-bottom.',
  sections: {
    purpose: {
      summary:
        'Holds a conversation’s messages in a scrolling log that behaves like chat: it opens at the newest message, follows new ones while the reader is at the bottom, loads older history as they scroll up without the view jumping, and announces arrivals politely.',
      useWhen: [
        'Channels, direct messages and threads.',
        'Ticket and request conversations, interleaved with system rows.',
        'Any reverse-chronological thread whose newest item sits next to a composer.',
      ],
      avoidWhen: [
        'Feeds read top-down, newest first (activity, notifications): use ActivityTimeline.',
        'Performance: MessageTimeline is not virtualised; every loaded message stays in the DOM. For histories of many thousands of loaded messages (Rook Chat), keep your virtualiser (react-virtuoso) and render MessageRow with the exported `MessageTimelineDaySeparator`, `MessageTimelineUnreadDivider`, `shouldGroupMessages` and `formatMessageDay`.',
      ],
    },
    designPrinciples: {
      items: [
        'The reader’s place is sacred: prepending older pages and late-loading images never move what they are reading.',
        'Follow new messages only when the reader is at the bottom; otherwise count them on a “Jump to latest” button.',
        'One live region for messages, a separate status for typing, so nothing is announced twice.',
        'Data and fetching stay in the product; the timeline only asks for older pages through `onLoadOlder`.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'Generic over your item type: items need `id`, `time` and an optional `authorId`; `renderItem(item, { grouped, index, previous })` draws each one.',
        '`handleRef` exposes `scrollToBottom()` and `scrollToItem(id)`; the ref is forwarded to the root `<div>`.',
        '`shouldGroupMessages`, `formatMessageDay`, `MessageTimelineDaySeparator` and `MessageTimelineUnreadDivider` are exported for products that keep their own virtualiser (react-virtuoso and similar).',
        'Not virtualised: every loaded message is in the DOM. Paging keeps that bounded for typical conversations.',
      ],
    },
    typedProperties: { source: 'typescript' },
    variants: {
      items: [
        {
          prop: 'stickToBottom',
          values: ['true', 'false'],
          description: 'Follow new messages while at the bottom. Off for read-only transcripts.',
        },
        {
          prop: 'jumpToLatest',
          values: ['true', 'false'],
          description: 'Floating “Jump to latest” (or “3 new messages”) button while scrolled up.',
        },
      ],
    },
    states: {
      items: [
        { name: 'Loading', description: 'Skeleton rows; the log is `aria-busy`.' },
        {
          name: 'Error',
          description: 'The `error` node replaces the list (for example InlineErrorRetry).',
        },
        { name: 'Empty', description: '`empty`, or `header` when no empty node is given.' },
        {
          name: 'Loading older',
          description:
            'LoadMore at the top shows “Loading older messages…”; the log is busy so old history is not read as new.',
        },
        {
          name: 'Older failed',
          description: '“Couldn’t load older messages.” with Retry; scroll loading pauses.',
        },
        {
          name: 'Scrolled up',
          description: 'Jump button with a count of messages that arrived meanwhile.',
        },
        {
          name: 'Highlighted',
          attribute: 'data-highlighted',
          description: 'A deep-linked message flashes (a steady tint with reduced motion).',
        },
      ],
    },
    interaction: {
      items: [
        'Opens at the newest message, or at `scrollToId` when given.',
        'Scrolling within `loadOlderThreshold` (240px) of the top calls `onLoadOlder` once per load; it is also called while the history is shorter than the view.',
        'When older items are prepended the scroll offset grows by exactly their height, so the view stays still.',
        'New items scroll into view only when the reader was at the bottom; `onAtBottomChange` reports reaching or leaving it (for marking read).',
        'Jump to latest scrolls down (instantly with reduced motion) and keeps focus in the log.',
      ],
    },
    keyboard: {
      items: [
        { keys: 'Tab', action: 'Focuses the log, then the controls inside each message.' },
        {
          keys: 'Arrow Up / Down, Page Up / Down',
          action: 'Scroll the focused log (native scrolling).',
        },
        {
          keys: 'Enter / Space',
          action: 'On “Load older messages” or “Jump to latest”, activates it.',
        },
      ],
    },
    accessibility: {
      role: 'log',
      name: '“Messages” by default (`messages.label`).',
      items: [
        '`role="log"` with `aria-live="polite"` and `aria-relevant="additions"`: new messages are read without interrupting.',
        'While older history loads and settles the log is `aria-busy`, so prepended pages are not announced as new.',
        'Typing and other transient text goes in a separate `role="status"` outside the log.',
        'Day separators and the unread divider are `role="separator"` with their text as the name.',
        'The log is a focusable scroll region, so keyboard users can scroll it.',
      ],
    },
    responsive: {
      items: [
        'Fills its container’s height; give the parent a height (flex child or fixed).',
        'Few messages sit at the bottom, next to the composer.',
        'Rows reflow at any width down to 320px; the jump button stays in the corner.',
      ],
    },
    theme: {
      items: [
        'Separators use `border-subtle` and `text-muted`; the unread divider uses `danger-strong` and `danger-border`.',
        'The jump button is a raised surface with `shadow-md`; the deep-link flash uses `accent-subtle`.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Conversation with older history',
          render: () => <LiveDemo />,
        },
        {
          title: 'In your own virtualiser',
          render: () => (
            <p style={{ margin: 0, fontSize: 13 }}>
              Use the exported parts inside react-virtuoso or similar.
            </p>
          ),
          code: `itemContent={(i, m) => {
  const prev = messages[i - 1];
  const newDay = !prev || formatMessageDay(prev.time) !== formatMessageDay(m.time);
  return (
    <>
      {newDay && <MessageTimelineDaySeparator>{formatMessageDay(m.time)}</MessageTimelineDaySeparator>}
      <MessageRow grouped={!newDay && shouldGroupMessages(prev, m)} … />
    </>
  );
}}`,
        },
      ],
    },
    doDont: {
      do: [
        'Keep `firstUnreadId` fixed while the conversation is open.',
        'Put typing indicators in `status`, never in the log.',
        'Give the timeline a bounded height so it can scroll.',
      ],
      dont: [
        'Do not reverse the items: pass them oldest first.',
        'Do not change item ids after sending; reconcile the pending id with the server id in place.',
        'Do not call `onLoadOlder` work twice: return early while a page is in flight.',
      ],
    },
    related: {
      items: [
        { id: 'message-row', reason: 'Each message.' },
        { id: 'message-composer', reason: 'The input under the timeline (planned).' },
        { id: 'load-more', reason: 'Used for the “Load older messages” control.' },
        { id: 'activity-timeline', reason: 'Top-down event feeds.' },
      ],
    },
    dependencies: { packages: ['react'], components: ['load-more', 'skeleton'] },
    testing: {
      unit: [
        'Polite log with day separators, unread divider and grouping rules.',
        'Opens at the bottom and follows appended messages while there; not with `stickToBottom={false}`.',
        'Keeps the view still when older messages are prepended.',
        'Loads older near the top once per load, from the button, and until the view is filled; pauses after an error and retries.',
        'Counts messages that arrive while scrolled up; Jump to latest scrolls down and keeps focus.',
        'Typing status outside the log; header, footer, empty, loading and error states.',
        'Deep link scroll and highlight; imperative handle; focusable scroll region; translations.',
        '`shouldGroupMessages` and `formatMessageDay`.',
      ],
      axe: true,
      ssr: true,
      browser: [
        'Visual baselines: /__gallery/message-timeline',
        'Cross-browser axe: /components/message-timeline',
      ],
    },
  },
} satisfies ComponentDocs;
