import { useState } from 'react';
import { CommentThread, type CommentItem } from '@rookcom/monarch-react/collaboration';
import type { ComponentDocs } from '../../../internal/component-docs.js';

const NOW = '2026-03-10T12:00:00Z';
const PRIYA = { name: 'Priya Raman', seed: 'u1' };
const TOM = { name: 'Tom Becker', seed: 'u2' };
const LEE = { name: 'Lee Park', seed: 'u3' };

const START: CommentItem[] = [
  {
    id: 'c1',
    author: PRIYA,
    body: 'Should this say "billed every quarter" instead?',
    createdAt: '2026-03-10T09:12:00Z',
  },
  {
    id: 'c2',
    author: TOM,
    body: 'Yes. Monthly billing was retired last year.',
    createdAt: '2026-03-10T09:40:00Z',
    canDelete: true,
  },
];

// Each example thread has its own starter, so each region has its own name.
const LONG: CommentItem[] = [
  { ...(START[0] as CommentItem), author: LEE },
  ...[
    'Agreed.',
    'Updated the copy.',
    'Checked the pricing table too.',
    'Legal is fine with it.',
    'Shipping tomorrow.',
    'Thanks all.',
  ].map((body, i) => ({
    id: `r${i}`,
    author: i % 2 ? LEE : TOM,
    body,
    createdAt: `2026-03-10T10:${10 + i * 5}:00Z`,
  })),
];

const RESOLVED: CommentItem[] = [
  {
    id: 'c1',
    author: TOM,
    body: 'Can we drop the footnote under the pricing table?',
    createdAt: '2026-03-10T08:30:00Z',
  },
  {
    id: 'c2',
    author: PRIYA,
    body: 'Done. The terms page covers it.',
    createdAt: '2026-03-10T08:55:00Z',
  },
];

function ThreadDemo() {
  const [comments, setComments] = useState(START);
  const [resolved, setResolved] = useState(false);
  return (
    <CommentThread
      comments={comments}
      quote="Billed monthly, cancel any time."
      resolved={resolved}
      onResolvedChange={setResolved}
      onReply={(body) =>
        new Promise<void>((done) =>
          setTimeout(() => {
            setComments((list) => [
              ...list,
              { id: `c${list.length + 1}`, author: LEE, body, createdAt: NOW, canDelete: true },
            ]);
            done();
          }, 600),
        )
      }
      onDeleteComment={(id) => setComments((list) => list.filter((c) => c.id !== id))}
      now={NOW}
      locale="en-US"
    />
  );
}

export default {
  id: 'comment-thread',
  title: 'CommentThread',
  summary:
    'One threaded discussion: the starting comment, replies with older ones folded, an optional quote, Resolve / Reopen and a reply box that stays busy while saving.',
  sections: {
    purpose: {
      summary:
        'Shows a single discussion inline, next to the thing it is about (a document passage, a design, a field, a ticket), with replies and resolution. Needed by: Rook Dossier (DocumentEditor: comment threads opened from `data-comment-thread` highlights and the new-comment composer).',
      useWhen: [
        'A discussion anchored to one item, shown in place: a popover on a highlight, a card under a field, a ticket sidebar.',
        'Review flows where threads are resolved when settled.',
      ],
      avoidWhen: [
        'Browsing many threads with Open / Resolved filters: use CommentsPanel.',
        'Chat-style conversations: use MessageTimeline and MessageComposer.',
        'A thread opened beside a chat message: use ThreadPanel.',
      ],
    },
    designPrinciples: {
      items: [
        'The product saves: every action is a callback, and a returned promise keeps the control busy and reports failure without losing the draft.',
        'The start of the conversation and the latest replies are always visible; the middle folds behind "Show N earlier replies".',
        'Resolved threads stay readable but muted, and say how to reply again instead of hiding the box silently.',
        'Deleting asks first, with the shared ConfirmDialog.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        "Comments use CommentsPanel's `CommentItem` and `CommentAuthor` types, so one data model serves both.",
        'Comment bodies are rendered as given (`ReactNode`); Monarch does not sanitise HTML, the product does.',
        'Pass `now` and `locale` for identical relative times on the server and the client.',
      ],
    },
    typedProperties: { source: 'typescript' },
    variants: {
      items: [
        {
          prop: 'resolved',
          values: ['false', 'true'],
          description:
            'Muted thread with Reopen instead of the reply box. Exposed as `data-resolved`.',
        },
        {
          prop: 'quote',
          values: ['ReactNode'],
          description: 'The passage the thread is about, shown above it.',
        },
        {
          prop: 'visibleReplies',
          values: ['number'],
          description: 'How many latest replies stay visible before older ones fold (default 3).',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Sending',
          attribute: 'aria-busy on the send button',
          description:
            'The box is read-only and the button shows a spinner until the promise settles.',
        },
        {
          name: 'Failed',
          attribute: 'aria-invalid on the reply box',
          description: "An alert says it didn't work; the draft is kept.",
        },
        {
          name: 'Resolved',
          attribute: 'data-resolved',
          description: 'Muted quote and bodies, Reopen button, hint instead of the box.',
        },
        {
          name: 'Folded',
          description: '"Show N earlier replies" between the first comment and the latest replies.',
        },
      ],
    },
    interaction: {
      items: [
        'Enter sends the reply; Shift+Enter adds a new line; IME composition is respected.',
        'Showing earlier replies moves focus to the first revealed reply.',
        'Resolve / Reopen and sending announce their result in a polite live region.',
        'Delete (on comments with `canDelete`) opens a danger confirmation; its promise shows progress in the dialog.',
      ],
    },
    keyboard: {
      items: [
        {
          keys: 'Tab',
          action:
            'Moves through Resolve, delete buttons, Show earlier replies, the reply box and Send.',
        },
        { keys: 'Enter (in the reply box)', action: 'Sends the reply.' },
        { keys: 'Shift+Enter', action: 'New line in the reply.' },
        {
          keys: 'Enter / Space (on buttons)',
          action: 'Resolve, reopen, delete or reveal replies.',
        },
        { keys: 'Escape (in the dialog)', action: 'Cancels a delete and returns focus.' },
      ],
    },
    accessibility: {
      role: 'region (section) containing an ordered list of comments',
      name: '"Thread started by {author}" (`messages.label`).',
      items: [
        'Comments are an ordered list; avatars are decorative because the author name is visible.',
        'The reply box is named "Reply" and described by the Enter hint (and the error when sending fails).',
        'Resolve is a toggle button (`aria-pressed`).',
        'Delete buttons are named "Delete comment by {author}".',
        'Every string is translatable through `messages`.',
      ],
    },
    responsive: {
      items: [
        'Single column; the quote and Resolve wrap, long words break, at 320px nothing scrolls sideways.',
        'Reply indentation shrinks on narrow screens.',
        'Send and delete are 44px on coarse pointers.',
      ],
    },
    theme: {
      items: [
        'Surface, border, accent (quote) and text tokens, matching CommentsPanel.',
        'Forced colours: border and quote rule use CanvasText; focus uses Highlight.',
        'Nothing animates apart from the shared button spinner, which follows reduced motion.',
      ],
    },
    examples: {
      items: [
        { title: 'Open thread with a quote', stack: true, render: () => <ThreadDemo /> },
        {
          title: 'Long thread, folded',
          stack: true,
          render: () => (
            <CommentThread comments={LONG} now={NOW} locale="en-US" onReply={() => {}} />
          ),
        },
        {
          title: 'Resolved',
          stack: true,
          render: () => (
            <CommentThread
              comments={RESOLVED}
              resolved
              onResolvedChange={() => {}}
              onReply={() => {}}
              now={NOW}
              locale="en-US"
            />
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Return the save promise from `onReply` so the box stays busy and errors keep the draft.',
        'Set `canDelete` only on comments the person may delete.',
        'Pass `quote` when the thread is about a passage.',
      ],
      dont: [
        'Render untrusted HTML in comment bodies without sanitising it.',
        'Hide resolved threads entirely; let people reopen them.',
        'Nest threads inside threads.',
      ],
    },
    related: {
      items: [
        {
          id: 'comments-panel',
          reason: 'Lists many threads with Open / Resolved filters; shares the comment types.',
        },
        { id: 'thread-panel', reason: 'Chat threads beside a message.' },
        { id: 'reaction-bar', reason: 'Reactions under a comment.' },
        { id: 'mention-picker', reason: 'Add @mentions to the reply box.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: ['avatar', 'button', 'confirm-dialog', 'relative-time', 'text-field'],
      notes: ['Uses the `CommentItem` and `CommentAuthor` types from comments-panel (type-only).'],
    },
    testing: {
      unit: [
        'Region name, root and reply items, quote, reply box name and hint, Resolve toggle; no axe violations.',
        'Enter sends, Shift+Enter adds a line, busy until the promise settles, clears and announces.',
        'Failed reply keeps the draft and shows an alert.',
        'Resolve and reopen, resolved hint instead of the box.',
        'Folding earlier replies and focus moving to the first revealed one.',
        'Delete asks for confirmation first.',
        'Translated label; no box without `onReply`.',
        'Renders on the server.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
