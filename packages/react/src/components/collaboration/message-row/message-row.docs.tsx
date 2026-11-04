import { useState } from 'react';
import { Badge, IconButton } from '@rookcom/monarch-react';
import { MessageRow, type MessageReaction } from '@rookcom/monarch-react/collaboration';
import type { ComponentDocs } from '../../../internal/component-docs.js';
import { MoreIcon, PencilIcon } from '../../../internal/docs-demo.js';

const NOW = '2026-03-02T12:00:00Z';

function ReactionsDemo() {
  const [reactions, setReactions] = useState<MessageReaction[]>([
    { emoji: '👍', label: 'thumbs up', count: 3, reacted: true, title: 'You, Ben and Chloé' },
    { emoji: '🎉', label: 'party popper', count: 1 },
  ]);
  const toggle = (emoji: string) =>
    setReactions((list) =>
      list.map((r) =>
        r.emoji === emoji
          ? { ...r, reacted: !r.reacted, count: r.count + (r.reacted ? -1 : 1) }
          : r,
      ),
    );
  return (
    <div style={{ width: '100%' }}>
      <MessageRow
        author={{ name: 'Ana Lopez' }}
        time="2026-03-02T11:52:00Z"
        now={NOW}
        reactions={reactions}
        onReactionToggle={toggle}
        replyCount={4}
        onOpenThread={() => {}}
        actions={
          <>
            <IconButton size="xs" label="Edit message" icon={<PencilIcon />} />
            <IconButton size="xs" label="More actions" icon={<MoreIcon />} />
          </>
        }
      >
        <p>The release notes are ready for review. Comments by Thursday, please.</p>
      </MessageRow>
    </div>
  );
}

export default {
  id: 'message-row',
  title: 'MessageRow',
  summary:
    'One message: avatar, author, time and body, with edited, pending and failed states, reactions, replies and actions.',
  sections: {
    purpose: {
      summary:
        'Shows one message in a conversation, comment thread or ticket reply list, with the states real messaging needs: grouped follow-ups, sending, failed with retry, edited, deleted, internal notes and system lines.',
      useWhen: [
        'Chat channels, direct messages and threads.',
        'Ticket and request conversations, including internal notes.',
        'Comment threads on records, inside a MessageTimeline or any list.',
      ],
      avoidWhen: [
        'A feed of system events with verbs and objects ("Ana changed the status"): use ActivityTimeline.',
        'A notification that leads elsewhere: use NotificationCenter.',
      ],
    },
    designPrinciples: {
      items: [
        'The body is the product’s: Monarch lays out the row and never parses or sanitises message content.',
        'Every message is named by its author and time, even when grouped, so a screen reader never hears an anonymous line.',
        'Actions appear on hover and whenever focus is inside the row, so the keyboard reaches them without a hidden mode; touch screens always show them.',
        'Failure is recoverable in place: the error, Retry and Discard sit under the message that failed.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'The ref is forwarded to the root `<article>`.',
        '`messages` translates every built-in string; `replies` and `reaction` are functions for plurals.',
      ],
    },
    typedProperties: { source: 'typescript' },
    variants: {
      items: [
        {
          prop: 'variant',
          values: ['default', 'internal', 'system'],
          description:
            '`internal` tints the row and draws a warning edge for teammate-only notes (add an audience tag in `tags`); `system` is a centred one-line event.',
        },
        {
          prop: 'grouped',
          values: ['false', 'true'],
          description:
            'Follow-up from the same author: no avatar or header, the time appears in the gutter on hover and focus.',
        },
        {
          prop: 'timeDisplay',
          values: ['relative', 'clock'],
          description:
            '“5 minutes ago” (RelativeTime) or “14:05”. Both carry the full date in `title`.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Pending',
          attribute: 'data-status="pending"',
          description: 'Body muted, “Sending…” in a status region, actions hidden, `aria-busy`.',
        },
        {
          name: 'Failed',
          attribute: 'data-status="failed"',
          description:
            'The error (or “Not sent”) in an alert, with Retry and Discard when their callbacks are given.',
        },
        {
          name: 'Edited',
          description: '“(edited)” after the body; a button when `onShowEdits` opens the history.',
        },
        {
          name: 'Deleted',
          attribute: 'data-deleted',
          description:
            '“This message was deleted.” replaces the body; reactions, footer and actions are dropped.',
        },
        {
          name: 'Highlighted',
          attribute: 'data-highlighted',
          description: 'Accent tint for a deep-linked or searched message.',
        },
      ],
    },
    interaction: {
      items: [
        'Reaction chips are toggle buttons when `onReactionToggle` is set, otherwise read-only.',
        'The reply count calls `onOpenThread`.',
        'Retry and Discard call `onRetry` and `onDiscard`; the product resends or removes the message.',
        'The actions group becomes visible on pointer hover and whenever focus enters the row.',
      ],
    },
    keyboard: {
      items: [
        {
          keys: 'Tab',
          action:
            'Moves through the row’s controls: edited marker, reactions, replies, then the actions.',
        },
        { keys: 'Enter / Space', action: 'Activates the focused reaction, reply count or action.' },
      ],
      notes: ['The row itself is not a tab stop; only its controls are.'],
    },
    accessibility: {
      role: 'article',
      name: 'Labelled by the author name and the time (`aria-labelledby`), also when grouped.',
      items: [
        'Grouped rows keep a visually hidden author name.',
        'Reaction chips use `aria-pressed` and a spoken name such as “thumbs up, 3”; pass `label` for each emoji.',
        'The time is a `<time datetime>` with the full date in `title`.',
        'Pending text is a polite status; a failure is an alert so the sender hears it.',
        'Actions sit in a named group (“Message actions”).',
      ],
    },
    responsive: {
      items: [
        'Header items wrap under the name on narrow screens; long words in the body break.',
        'On touch screens (`hover: none`) the actions stay visible at the end of the row and the grouped time is shown.',
      ],
    },
    theme: {
      items: [
        'Hover and focus use `bg-subtle`, internal notes `warning-subtle` with a `warning-border` edge, highlight `accent-subtle`.',
        'Pressed reactions use the accent tokens; failed text `danger-strong`. Forced colours outline the highlighted row.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Reactions, replies and actions',
          render: () => <ReactionsDemo />,
        },
        {
          title: 'Grouping, edited and states',
          stack: true,
          render: () => (
            <div style={{ width: '100%' }}>
              <MessageRow author={{ name: 'Ben Okafor' }} time="2026-03-02T11:40:00Z" now={NOW}>
                Draft is in the shared folder.
              </MessageRow>
              <MessageRow
                author={{ name: 'Ben Okafor' }}
                time="2026-03-02T11:41:00Z"
                now={NOW}
                grouped
                edited
              >
                Updated the second section too.
              </MessageRow>
              <MessageRow
                author={{ name: 'Ben Okafor' }}
                time="2026-03-02T11:58:00Z"
                now={NOW}
                grouped
                status="pending"
              >
                Sending this one now…
              </MessageRow>
              <MessageRow
                author={{ name: 'Ben Okafor' }}
                time="2026-03-02T11:59:00Z"
                now={NOW}
                grouped
                status="failed"
                error="No connection. Not sent."
                onRetry={() => {}}
                onDiscard={() => {}}
              >
                And the summary slide.
              </MessageRow>
              <MessageRow
                author={{ name: 'Chloé Martin' }}
                time="2026-03-02T10:00:00Z"
                now={NOW}
                deleted
              />
            </div>
          ),
        },
        {
          title: 'Internal note and system line',
          stack: true,
          render: () => (
            <div style={{ width: '100%' }}>
              <MessageRow
                author={{ name: 'Dev Patel' }}
                time="2026-03-02T09:10:00Z"
                now={NOW}
                variant="internal"
                tags={
                  <Badge tone="warning" size="sm">
                    Internal
                  </Badge>
                }
              >
                Customer is on the legacy plan. Check billing before replying.
              </MessageRow>
              <MessageRow
                author={{ name: 'System' }}
                time="2026-03-02T09:12:00Z"
                now={NOW}
                variant="system"
              >
                Dev Patel assigned this request to the Billing team
              </MessageRow>
            </div>
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Render rows inside MessageTimeline, which sets `grouped` for you.',
        'Give every reaction a spoken `label` and a `title` listing who reacted.',
        'Put the audience (“Internal”, “Reply to customer”) in `tags` as text, not colour alone.',
      ],
      dont: [
        'Do not put unsanitised HTML in the body; sanitise in the product.',
        'Do not hide important actions only in the hover group; keep primary actions such as Reply reachable elsewhere too on small screens.',
        'Do not use the system variant for user-written content.',
      ],
    },
    related: {
      items: [
        {
          id: 'message-timeline',
          reason: 'The scrolling conversation that groups and separates rows.',
        },
        { id: 'message-composer', reason: 'Writing the next message (planned).' },
        { id: 'activity-timeline', reason: 'System events with actor, verb and object.' },
        { id: 'avatar', reason: 'The author picture.' },
        { id: 'relative-time', reason: 'The timestamp.' },
      ],
    },
    dependencies: { packages: ['react'], components: ['avatar', 'relative-time'] },
    testing: {
      unit: [
        'Names the article by author and time, also when grouped, and renders a `<time datetime>` with the full date.',
        'Edited marker, and the edit-history button when `onShowEdits` is set.',
        'Pending status, failed alert with Retry and Discard, default failure text.',
        'Reaction toggles with `aria-pressed` and spoken counts; read-only reactions.',
        'Thread count, keyboard-reachable actions group, deleted placeholder.',
        'Internal, highlighted, system and clock variants; translated text; invalid time.',
      ],
      axe: true,
      ssr: true,
      browser: [
        'Visual baselines: /__gallery/message-row',
        'Cross-browser axe: /components/message-row',
      ],
    },
  },
} satisfies ComponentDocs;
