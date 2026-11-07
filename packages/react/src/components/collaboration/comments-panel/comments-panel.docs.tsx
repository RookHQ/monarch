import { useState } from 'react';
import { CommentsPanel, type CommentThread } from '@rookcom/monarch-react/collaboration';
import type { ComponentDocs } from '../../../internal/component-docs.js';

const NOW = new Date('2026-03-10T12:00:00Z');

const START: CommentThread[] = [
  {
    id: 't1',
    quote: 'The rollout starts in the second week of the quarter.',
    comments: [
      {
        id: 'c1',
        author: { name: 'Ana Ruiz' },
        body: 'Can we confirm the date with the support team first?',
        createdAt: '2026-03-10T09:30:00Z',
      },
      {
        id: 'c2',
        author: { name: 'Ben Okafor' },
        body: 'Confirmed. They are ready from the Monday.',
        createdAt: '2026-03-10T11:10:00Z',
        canDelete: true,
      },
    ],
  },
  {
    id: 't2',
    quote: 'Budget summary table',
    comments: [
      {
        id: 'c3',
        author: { name: 'Chen Li' },
        body: 'The totals row is missing the last line item.',
        createdAt: '2026-03-09T15:00:00Z',
        canDelete: true,
      },
    ],
  },
  {
    id: 't3',
    quote: 'Glossary',
    resolved: true,
    comments: [
      {
        id: 'c4',
        author: { name: 'Dana Novak' },
        body: 'Added the missing terms.',
        createdAt: '2026-03-08T10:00:00Z',
      },
    ],
  },
];

function Basic() {
  const [threads, setThreads] = useState(START);
  const [count, setCount] = useState(10);
  return (
    <div style={{ width: '100%', maxWidth: '24rem', height: '32rem' }}>
      <CommentsPanel
        threads={threads}
        now={NOW}
        onClose={() => {}}
        onReply={(threadId, body) => {
          setCount((n) => n + 1);
          setThreads((list) =>
            list.map((t) =>
              t.id === threadId
                ? {
                    ...t,
                    comments: [
                      ...t.comments,
                      {
                        id: `c${count}`,
                        author: { name: 'You' },
                        body,
                        createdAt: NOW,
                        canDelete: true,
                      },
                    ],
                  }
                : t,
            ),
          );
        }}
        onResolvedChange={(threadId, resolved) =>
          setThreads((list) => list.map((t) => (t.id === threadId ? { ...t, resolved } : t)))
        }
        onDeleteComment={(threadId, commentId) =>
          setThreads((list) =>
            list
              .map((t) =>
                t.id === threadId
                  ? { ...t, comments: t.comments.filter((c) => c.id !== commentId) }
                  : t,
              )
              .filter((t) => t.comments.length > 0),
          )
        }
      />
    </div>
  );
}

function States() {
  return (
    <div
      style={{
        display: 'grid',
        gap: 'var(--mn-space-6)',
        width: '100%',
        maxWidth: '24rem',
      }}
    >
      <div style={{ height: '16rem' }}>
        <CommentsPanel threads={[]} messages={{ title: 'Comments (empty)' }} />
      </div>
      <div style={{ height: '16rem' }}>
        <CommentsPanel threads={null} messages={{ title: 'Comments (loading)' }} />
      </div>
      <div style={{ height: '12rem' }}>
        <CommentsPanel
          threads={null}
          error="Comments didn’t load."
          onRetry={() => {}}
          messages={{ title: 'Comments (error)' }}
        />
      </div>
    </div>
  );
}

function ReadOnlyThread() {
  return (
    <div style={{ width: '100%', maxWidth: '24rem', height: '24rem' }}>
      <CommentsPanel
        threads={START}
        now={NOW}
        defaultSelectedThreadId="t1"
        messages={{ title: 'Comments (read-only)' }}
      />
    </div>
  );
}

export default {
  id: 'comments-panel',
  title: 'CommentsPanel',
  summary:
    'Side panel of comment threads on a document or record: Open and Resolved, quoted passage, reply, resolve and confirmed delete.',
  sections: {
    purpose: {
      summary:
        'Documents and records collect discussion about a passage or a field. CommentsPanel lists those threads, opens one with its quote and replies, and lets people reply, resolve, reopen and delete their comments, with the data and saving left to the product. Needed by: Dossier (document comments), and any Rook app that adds inline or record comments.',
      useWhen: [
        'Beside an editor or a record page, for threads anchored to a passage or a field.',
        'For review flows where threads are resolved when done and kept for reference.',
      ],
      avoidWhen: [
        'A live conversation with many messages: use ThreadPanel with MessageRow.',
        'A history of changes made to a record: use ActivityTimeline.',
      ],
    },
    designPrinciples: {
      items: [
        'Data in, intent out: `threads` comes from the product; reply, resolve and delete are callbacks. The panel never fetches.',
        'Editor-agnostic: opening a thread calls `onSelectedThreadChange`, and the product highlights and scrolls to the passage in its own editor.',
        'Two views in one panel: the list of threads and one thread in full, with focus moving between them so keyboard users never lose their place.',
        'Destructive actions are confirmed: deleting a comment opens a ConfirmDialog, never `window.confirm`.',
        'Promise-aware: a returned promise keeps the button busy; a rejection keeps the text and shows an error.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'Forwards its ref to the `aside`. Exported from `@rookcom/monarch-react/collaboration`.',
        'Also exported: the `CommentThread`, `CommentItem`, `CommentAuthor` and `CommentsFilter` types.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        '`threads={null}` is loading. Threads are filtered by `resolved` for the Open / Resolved switch, so passing only the current set also works.',
        '`filter` and `selectedThreadId` are controlled or uncontrolled (`defaultFilter`, `defaultSelectedThreadId`).',
        'The reply box appears with `onReply` on threads that are open and not `canReply: false`; Resolve / Reopen with `onResolvedChange`; Delete on comments with `canDelete` and `onDeleteComment`.',
        'Migrating from the app copy (Dossier `CommentsPanel`): `documentId` and the data hooks stay in the product, which passes `threads` (the old `useComments` data; `createdBy` becomes the first comment’s `author`, `resolvedAt` becomes `resolved`); `editor` is gone (do the `markRange` select-and-scroll in `onSelectedThreadChange`, and update the comment mark in `onResolvedChange`); `initialThreadId` becomes `selectedThreadId` or `defaultSelectedThreadId`; `onClose` is unchanged; the reply, resolve and delete mutations become `onReply`, `onResolvedChange` and `onDeleteComment` (return the mutation promise).',
      ],
    },
    variants: {
      items: [],
      notes: [
        'No visual variants. Capabilities follow the callbacks: without `onReply`, `onResolvedChange` and `onDeleteComment` the panel is read-only.',
      ],
    },
    states: {
      items: [
        {
          name: 'List or thread',
          attribute: 'data-view="list" | "thread"',
          description: 'Whether the list of threads or one thread is shown.',
        },
        {
          name: 'Filter',
          attribute: 'data-filter="open" | "resolved"',
          description: 'Which threads are listed.',
        },
        {
          name: 'Resolved thread',
          attribute: 'data-resolved',
          description:
            'Muted quote, Reopen instead of Resolve, and a hint instead of the reply box.',
        },
        {
          name: 'Loading',
          attribute: 'aria-busy="true"',
          description: 'Skeleton rows inside a status.',
        },
        { name: 'Empty', description: 'An empty state per filter, with a hint for Open.' },
        {
          name: 'Busy',
          attribute: 'aria-busy="true"',
          description: 'On Send or Resolve while the returned promise is pending.',
        },
        {
          name: 'Error',
          description:
            'Loading errors show with Try again (`onRetry`); a failed reply or resolve is an alert and the text is kept.',
        },
      ],
    },
    interaction: {
      items: [
        'Choosing a thread opens it and calls `onSelectedThreadChange(id)`; All comments goes back and calls it with `null`.',
        'Switching Open / Resolved goes back to the list and calls `onFilterChange`.',
        'Enter sends a reply, Shift+Enter adds a line. The box clears when the reply succeeds.',
        'Resolve or Reopen returns to the list once it succeeds and announces the change.',
        'Delete asks for confirmation; the dialog shows progress and any error from `onDeleteComment`.',
      ],
    },
    keyboard: {
      items: [
        {
          keys: 'Tab',
          action: 'Close, the Open / Resolved switch, then threads or the thread controls.',
        },
        { keys: '← / →', action: 'Move between Open and Resolved (SegmentedControl).' },
        { keys: 'Enter / Space', action: 'Open a thread, go back, resolve, reopen, delete.' },
        { keys: 'Enter', action: 'Send the reply.' },
        { keys: 'Shift+Enter', action: 'New line in the reply.' },
        { keys: 'Escape', action: 'Cancel the delete confirmation; focus returns to Delete.' },
      ],
    },
    accessibility: {
      role: 'complementary (`aside` labelled by its heading)',
      name: 'The heading (`messages.title`, default "Comments").',
      items: [
        'Opening a thread moves focus to All comments; going back returns focus to that thread in the list.',
        'The quote is a labelled `blockquote`; comments are an ordered list.',
        'Delete buttons are named "Delete comment by <author>".',
        'The reply box is labelled and described by its keyboard hint; a failed send sets `aria-invalid` and shows an alert.',
        'Resolve and reopen are announced in a polite live region.',
      ],
    },
    responsive: {
      items: [
        'Fills its column and scrolls its body; at 390px it can sit in a Sheet at full width.',
        'Long names, quotes and comments wrap; list quotes clamp to two lines.',
        'Thread rows and delete buttons are 44px on touch screens.',
      ],
    },
    theme: {
      items: [
        'Surface, border and muted text tokens; the quote uses the accent subtle colours, muted when resolved.',
        'Forced colours keep the quote bar, hover and focus outlines visible. The row hover transition is removed under reduced motion.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Comments beside a document',
          render: () => <Basic />,
        },
        {
          title: 'Empty, loading and error',
          stack: true,
          render: () => <States />,
        },
        {
          title: 'Read-only thread',
          render: () => <ReadOnlyThread />,
        },
      ],
    },
    doDont: {
      do: [
        'Return the mutation promise from the callbacks so the panel can show progress and keep text on failure.',
        'Highlight and scroll to the quoted passage in `onSelectedThreadChange`.',
        'Set `canDelete` only on comments the person may delete; check again on the server.',
      ],
      dont: [
        'Do not fetch inside the panel or pass an editor instance to it.',
        'Do not skip the confirmation for deleting a comment.',
        'Do not use it for chat conversations.',
      ],
    },
    related: {
      items: [
        { id: 'thread-panel', reason: 'A chat thread with message rows and a composer.' },
        { id: 'message-row', reason: 'One chat message.' },
        { id: 'rich-text-editor', reason: 'The editor the comments are anchored to.' },
        { id: 'confirm-dialog', reason: 'The delete confirmation.' },
        { id: 'activity-timeline', reason: 'A record’s history of changes.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: [
        'avatar',
        'button',
        'confirm-dialog',
        'empty-state',
        'inline-error-retry',
        'relative-time',
        'segmented-control',
        'skeleton',
        'text-field',
      ],
    },
    testing: {
      unit: [
        'Labelled complementary region; open threads listed; resolved hidden; close button.',
        'Open / Resolved switch with onFilterChange; empty state per filter.',
        'Open a thread: focus to All comments, quote labelled; Back returns focus to the row.',
        'Reply: Enter sends, Shift+Enter new line, busy while pending, clears on success; failure keeps text with an alert.',
        'Resolve announces and returns to the list; resolved thread shows Reopen and no reply box.',
        'Delete asks for confirmation before calling onDeleteComment.',
        'Loading, error with Try again, translated text, SSR.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
