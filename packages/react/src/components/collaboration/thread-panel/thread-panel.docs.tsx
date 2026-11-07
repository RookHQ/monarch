import { useState, type FormEvent } from 'react';
import { Button, Textarea } from '@rookcom/monarch-react';
import { MessageRow, ThreadPanel } from '@rookcom/monarch-react/collaboration';
import type { ComponentDocs } from '../../../internal/component-docs.js';

const NOW = new Date('2026-03-10T12:00:00Z');

interface Reply {
  id: string;
  name: string;
  body: string;
  time: Date;
}

const START: Reply[] = [
  {
    id: 'r1',
    name: 'Ben Okafor',
    body: 'In the shared release folder, next to the checklist.',
    time: new Date('2026-03-10T11:02:00Z'),
  },
  {
    id: 'r2',
    name: 'Ben Okafor',
    body: 'I can move last quarter’s notes there too.',
    time: new Date('2026-03-10T11:03:00Z'),
  },
  {
    id: 'r3',
    name: 'Chen Li',
    body: 'Sounds good to me.',
    time: new Date('2026-03-10T11:20:00Z'),
  },
];

function Composer({ onSend }: { onSend: (text: string) => void }) {
  const [text, setText] = useState('');
  const submit = (event: FormEvent) => {
    event.preventDefault();
    if (!text.trim()) return;
    onSend(text.trim());
    setText('');
  };
  return (
    <form
      onSubmit={submit}
      style={{ display: 'flex', gap: 'var(--mn-space-2)', alignItems: 'end' }}
    >
      <Textarea
        aria-label="Reply"
        placeholder="Reply…"
        rows={1}
        value={text}
        onChange={(e) => setText(e.target.value)}
        style={{ flex: 1, minWidth: 0 }}
      />
      <Button type="submit" variant="primary" size="sm" disabled={!text.trim()}>
        Send
      </Button>
    </form>
  );
}

function Basic() {
  const [replies, setReplies] = useState(START);
  const [following, setFollowing] = useState(true);
  return (
    <div style={{ width: '100%', maxWidth: '26rem', height: '34rem' }}>
      <ThreadPanel
        replyCount={replies.length}
        following={following}
        onFollowingChange={setFollowing}
        onClose={() => {}}
        root={
          <MessageRow
            author={{ name: 'Ana Ruiz' }}
            time={new Date('2026-03-10T11:00:00Z')}
            now={NOW}
          >
            Where should the launch notes live?
          </MessageRow>
        }
        composer={
          <Composer
            onSend={(body) =>
              setReplies((list) => [
                ...list,
                { id: `r${list.length + 1}`, name: 'You', body, time: NOW },
              ])
            }
          />
        }
      >
        {replies.map((r, i) => (
          <MessageRow
            key={r.id}
            author={{ name: r.name }}
            time={r.time}
            now={NOW}
            grouped={replies[i - 1]?.name === r.name}
          >
            {r.body}
          </MessageRow>
        ))}
      </ThreadPanel>
    </div>
  );
}

function States() {
  return (
    <div style={{ display: 'grid', gap: 'var(--mn-space-6)', width: '100%', maxWidth: '26rem' }}>
      <div style={{ height: '12rem' }}>
        <ThreadPanel replyCount={0} status="loading" onClose={() => {}} />
      </div>
      <div style={{ height: '12rem' }}>
        <ThreadPanel replyCount={0} status="error" onRetry={() => {}} onClose={() => {}} />
      </div>
      <div style={{ height: '12rem' }}>
        <ThreadPanel replyCount={0} status="deleted" onClose={() => {}} />
      </div>
    </div>
  );
}

export default {
  id: 'thread-panel',
  title: 'ThreadPanel',
  summary:
    'Side panel for one conversation thread: root message, reply count, replies pinned to the newest, follow toggle and reply box.',
  sections: {
    purpose: {
      summary:
        'Chat-style products open replies to a message beside the conversation. ThreadPanel is that panel: header with Follow and Close, the root message, a reply count, a scrolling log of replies that stays on the newest one, and the reply box, with loading, error and deleted states. The product supplies the rows, the composer and the data. Needed by: Chat (conversation threads), and any Rook app with threaded replies.',
      useWhen: [
        'Beside a conversation, for the replies to one message.',
        'In a Sheet on small screens, at full width.',
      ],
      avoidWhen: [
        'Comments anchored to a passage of a document, with resolve: use CommentsPanel.',
        'The main conversation with thousands of messages: use VirtualMessageList or MessageTimeline.',
      ],
    },
    designPrinciples: {
      items: [
        'Slots, not data plumbing: `root`, `children`, `typing` and `composer` take ready-made MessageRows, a TypingIndicator and a MessageComposer, so the thread looks exactly like the conversation.',
        'Stay with the newest reply: new replies keep the view at the bottom unless the person has scrolled up to read.',
        'Read position belongs to the product: `onRepliesSeen` fires after replies have been on screen, and the product marks the thread read.',
        'Every state has a way out: errors offer Try again, a deleted thread offers Close.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'Forwards its ref to the `section`. Exported from `@rookcom/monarch-react/collaboration`.',
        'Also exported: the `ThreadPanelStatus` and `ThreadPanelMessages` types.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        'Pass `grouped` to reply rows with `shouldGroupMessages` (from MessageTimeline) so follow-ups from the same author collapse.',
        'The Follow toggle appears with `onFollowingChange`; `following` is its pressed state.',
        'Migrating from the app copy (Chat `ThreadPanel`): `conversation` and `rootId` stay in the product, which renders `data.root` into `root` and the replies plus pending items into `children` (MessageRow with `status="pending"` / `"failed"`); `isLoading` / `isError` / `root.deletedAt` become `status`; `refetch` becomes `onRetry`; the follow query and mutation become `following` and `onFollowingChange` (return the mutation promise; the panel announces the result instead of a toast); `markThreadRead` moves to `onRepliesSeen`; `data.truncated` becomes `notice`; `TypingIndicator` and `Composer` go into `typing` and `composer`; `onClose` is unchanged.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'status',
          values: ['ready', 'loading', 'error', 'deleted'],
          description: 'What the body shows.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Status',
          attribute: 'data-status="ready" | "loading" | "error" | "deleted"',
          description: 'Ready shows the thread; the others replace the body.',
        },
        {
          name: 'Loading',
          attribute: 'aria-busy="true"',
          description: 'A labelled spinner in the replies log.',
        },
        { name: 'Error', description: 'The error text, with Try again when `onRetry` is given.' },
        { name: 'Deleted', description: 'A notice with Close when `onClose` is given.' },
        {
          name: 'Following',
          attribute: 'aria-pressed="true"',
          description: 'The Follow toggle reads "Following" in the accent colour.',
        },
        {
          name: 'Follow pending',
          attribute: 'aria-busy="true"',
          description: 'While the promise from `onFollowingChange` is pending.',
        },
      ],
    },
    interaction: {
      items: [
        'Opens scrolled to the newest reply. New replies keep it there while the person is within a few lines of the bottom.',
        'Follow toggles notifications for the thread; the result is announced, and a failure is announced too.',
        '`onRepliesSeen(count)` fires after `seenDelay` (400ms) while the page is visible, and again when the count changes.',
        'The composer stays pinned below the replies; focus it on open by giving it `autoFocus` in the product.',
      ],
    },
    keyboard: {
      items: [
        {
          keys: 'Tab',
          action: 'Follow, Close, the replies log, controls inside rows, then the composer.',
        },
        { keys: 'Enter / Space', action: 'Toggle Follow, close, retry.' },
        { keys: '↑ / ↓, Page Up / Page Down', action: 'Scroll the replies log when it has focus.' },
      ],
    },
    accessibility: {
      role: 'region (`section` labelled by its heading) containing a `log`',
      name: 'The heading (`messages.title`, default "Thread"); the log is named "Replies".',
      items: [
        'The replies log is focusable so keyboard users can scroll it, and `role="log"` lets screen readers announce new replies politely.',
        'Follow is a toggle button (`aria-pressed`); its result is announced in a polite live region.',
        'Loading is a labelled status; load errors are alerts.',
      ],
    },
    responsive: {
      items: [
        'Fills its container’s height; header, footer and composer stay fixed while the replies scroll.',
        'At 390px it works at full width (in a Sheet or a stacked layout); Button and IconButton give 44px targets on touch screens.',
      ],
    },
    theme: {
      items: [
        'Surface, border and muted text tokens; Following uses the accent strong colour.',
        'Forced colours keep the divider, focus outline and Following state visible. Nothing animates beyond the shared Button and Spinner.',
      ],
    },
    examples: {
      items: [
        {
          title: 'A thread with replies, Follow and a reply box',
          render: () => <Basic />,
        },
        {
          title: 'Loading, error and deleted',
          stack: true,
          render: () => <States />,
        },
      ],
    },
    doDont: {
      do: [
        'Render replies with MessageRow and group follow-ups with `shouldGroupMessages`.',
        'Mark the thread read in `onRepliesSeen`, not on mount.',
        'Return the promise from `onFollowingChange` so the toggle shows progress and failures.',
      ],
      dont: [
        'Do not render a second scroll container inside `children`.',
        'Do not show a toast for follow changes as well; the panel already announces them.',
      ],
    },
    related: {
      items: [
        { id: 'message-row', reason: 'The root message and each reply.' },
        { id: 'message-composer', reason: 'The reply box.' },
        { id: 'typing-indicator', reason: 'Who is replying, above the composer.' },
        { id: 'message-timeline', reason: 'The main conversation and `shouldGroupMessages`.' },
        { id: 'comments-panel', reason: 'Resolvable comment threads on a document.' },
        { id: 'sheet', reason: 'Hosts the panel on small screens.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: ['button', 'inline-error-retry', 'spinner'],
    },
    testing: {
      unit: [
        'Labelled region with root, reply count, focusable Replies log and composer slot.',
        'Close button; Follow toggle with aria-pressed, busy while pending, result and failure announced.',
        'Loading, error with Try again, deleted with Close.',
        'onRepliesSeen after the delay and again when the count changes.',
        'Pinned to the newest reply when replies arrive; notice, typing slot, translated text; SSR.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
