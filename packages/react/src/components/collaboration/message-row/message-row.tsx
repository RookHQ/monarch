'use client';

import { forwardRef, useId, type HTMLAttributes, type ReactNode } from 'react';
import { cx } from '../../../internal/cx.js';
import { ErrorGlyph, RetryGlyph } from '../../../internal/icons.js';
import { Avatar } from '../../identity/avatar/index.js';
import { RelativeTime, type DateInput } from '../../data-display/relative-time/index.js';

export interface MessageAuthor {
  /** Display name. Shown above the first message of a group and read by screen readers on every message. */
  name: string;
  /** Avatar image. Initials are shown without it. */
  avatarSrc?: string | null;
  /** Stable id for the avatar colour, so a renamed person keeps their colour. */
  seed?: string;
}

export interface MessageReaction {
  /** The emoji or short code shown on the chip, e.g. `👍`. Also the value passed to `onReactionToggle`. */
  emoji: string;
  /** How many people reacted. */
  count: number;
  /** The current user reacted. Shown pressed. */
  reacted?: boolean;
  /** Spoken name of the emoji, e.g. "thumbs up". Default: the emoji itself. */
  label?: string;
  /** Who reacted, for the chip's title, e.g. "Ana, Ben and 3 others". */
  title?: string;
}

export type MessageRowStatus = 'sent' | 'pending' | 'failed';
export type MessageRowVariant = 'default' | 'internal' | 'system';

export interface MessageRowMessages {
  /** Shown while `status="pending"`. Default "Sending…". */
  sending: string;
  /** Shown when `status="failed"` without an `error`. Default "Not sent". */
  failed: string;
  retry: string;
  discard: string;
  /** The edited marker. Default "(edited)". */
  edited: string;
  /** Spoken name of the edited marker when it opens the edit history. Default "Edited. Show edit history". */
  editedHistory: string;
  /** Body of a deleted message. Default "This message was deleted." */
  deleted: string;
  /** Thread button text. Default "1 reply" / "{n} replies". */
  replies: (count: number) => string;
  /** Name of the actions group. Default "Message actions". */
  actions: string;
  /** Name of the reactions group. Default "Reactions". */
  reactions: string;
  /** Spoken name of one reaction chip. Default "{label}, {count}". */
  reaction: (label: string, count: number) => string;
}

const DEFAULT_MESSAGES: MessageRowMessages = {
  sending: 'Sending…',
  failed: 'Not sent',
  retry: 'Retry',
  discard: 'Discard',
  edited: '(edited)',
  editedHistory: 'Edited. Show edit history',
  deleted: 'This message was deleted.',
  replies: (n) => (n === 1 ? '1 reply' : `${n} replies`),
  actions: 'Message actions',
  reactions: 'Reactions',
  reaction: (label, count) => `${label}, ${count}`,
};

export interface MessageRowProps extends Omit<HTMLAttributes<HTMLElement>, 'children'> {
  author: MessageAuthor;
  /** When the message was sent. A Date, epoch milliseconds or an ISO 8601 string. */
  time: DateInput;
  /** The message body: text, rich content, attachments. Monarch does not sanitise HTML; the product does. */
  children?: ReactNode;
  /**
   * A follow-up from the same author: hides the avatar and header and shows
   * the time in the gutter on hover and focus. The author name stays in the
   * accessible name. MessageTimeline sets this for you.
   */
  grouped?: boolean;
  /**
   * `internal` tints the row for notes only teammates can see (with a tag in
   * `tags`); `system` is a centred, compact line for server-written events.
   */
  variant?: MessageRowVariant;
  /** Delivery state. `pending` dims the body; `failed` shows the error with Retry and Discard. */
  status?: MessageRowStatus;
  /** Why sending failed. Default `messages.failed`. */
  error?: ReactNode;
  onRetry?: () => void;
  onDiscard?: () => void;
  /** Shows the edited marker. */
  edited?: boolean;
  /** Makes the edited marker a button that opens the edit history. */
  onShowEdits?: () => void;
  /** Replaces the body with the deleted placeholder and hides reactions and actions. */
  deleted?: boolean;
  /** Briefly highlights the row, for example after a deep link. */
  highlighted?: boolean;
  /** Small tags after the author name: audience ("Internal"), role ("Guest", "App"), pinned. */
  tags?: ReactNode;
  /** Replaces the avatar, for example an app or bot glyph. */
  avatar?: ReactNode;
  reactions?: readonly MessageReaction[];
  /** Called with the emoji when a reaction chip is pressed. Without it the chips are read-only. */
  onReactionToggle?: (emoji: string) => void;
  /** Extra control after the reaction chips, typically an "Add reaction" button. */
  reactionAction?: ReactNode;
  /** Thread reply count. Shown as a button when `onOpenThread` is set. */
  replyCount?: number;
  onOpenThread?: () => void;
  /**
   * Per-message controls (react, reply, edit, more). Rendered in a group
   * that appears on hover and whenever focus is inside the row, so it is
   * reachable with Tab; always visible on touch screens.
   */
  actions?: ReactNode;
  /** Content below the body: attachments, link previews, read receipts. */
  footer?: ReactNode;
  /** Reference time for the relative timestamp. Pass it for identical server and client output. */
  now?: DateInput;
  locale?: string;
  timeZone?: string;
  /**
   * `relative` ("5 minutes ago", default) or `clock` ("14:05"). Both render
   * a `<time datetime>` with the full date in `title`.
   */
  timeDisplay?: 'relative' | 'clock';
  /** Translations of the built-in text. */
  messages?: Partial<MessageRowMessages>;
}

function toDate(input: DateInput): Date {
  return input instanceof Date ? input : new Date(input);
}

function absoluteTime(date: Date, locale?: string, timeZone?: string): string {
  if (Number.isNaN(date.getTime())) return '';
  return new Intl.DateTimeFormat(locale, {
    dateStyle: 'full',
    timeStyle: 'short',
    ...(timeZone ? { timeZone } : {}),
  }).format(date);
}

function clockTime(date: Date, locale?: string, timeZone?: string): string {
  if (Number.isNaN(date.getTime())) return '—';
  return new Intl.DateTimeFormat(locale, {
    timeStyle: 'short',
    ...(timeZone ? { timeZone } : {}),
  }).format(date);
}

/**
 * One message in a conversation: avatar, author, time and body, with
 * edited, pending, failed and deleted states, reactions, a thread reply
 * count and per-message actions. Consecutive messages from one author are
 * shown `grouped`.
 */
export const MessageRow = forwardRef<HTMLElement, MessageRowProps>(function MessageRow(
  {
    author,
    time,
    children,
    grouped = false,
    variant = 'default',
    status = 'sent',
    error,
    onRetry,
    onDiscard,
    edited = false,
    onShowEdits,
    deleted = false,
    highlighted = false,
    tags,
    avatar,
    reactions,
    onReactionToggle,
    reactionAction,
    replyCount,
    onOpenThread,
    actions,
    footer,
    now,
    locale,
    timeZone,
    timeDisplay = 'relative',
    messages: messagesProp,
    className,
    ...rest
  },
  ref,
) {
  const m = { ...DEFAULT_MESSAGES, ...messagesProp };
  const id = useId();
  const authorId = `${id}-author`;
  const timeId = `${id}-time`;
  const date = toDate(time);
  const title = absoluteTime(date, locale, timeZone);

  const timeEl = (compact: boolean) =>
    timeDisplay === 'clock' || compact ? (
      <time
        id={timeId}
        className="mn-message-row__time"
        dateTime={Number.isNaN(date.getTime()) ? undefined : date.toISOString()}
        title={title}
        suppressHydrationWarning
      >
        {clockTime(date, locale, timeZone)}
      </time>
    ) : (
      <RelativeTime
        id={timeId}
        className="mn-message-row__time"
        date={date}
        tooltip={false}
        title={title}
        {...(now !== undefined ? { now } : {})}
        {...(locale !== undefined ? { locale } : {})}
        {...(timeZone !== undefined ? { timeZone } : {})}
      />
    );

  if (variant === 'system') {
    return (
      <article
        ref={ref}
        {...rest}
        aria-labelledby={rest['aria-labelledby'] ?? `${id}-body ${timeId}`}
        className={cx('mn-message-row', className)}
        data-variant="system"
        data-highlighted={highlighted || undefined}
      >
        <div className="mn-message-row__system">
          <span id={`${id}-body`} className="mn-message-row__system-body">
            {children}
          </span>
          {timeEl(false)}
        </div>
      </article>
    );
  }

  const hasReactions = !deleted && reactions !== undefined && reactions.length > 0;
  const showThread = replyCount !== undefined && replyCount > 0;
  const showActions = actions !== undefined && actions !== null && !deleted && status === 'sent';

  return (
    <article
      ref={ref}
      {...rest}
      aria-labelledby={rest['aria-labelledby'] ?? `${authorId} ${timeId}`}
      aria-busy={status === 'pending' || undefined}
      className={cx('mn-message-row', className)}
      data-variant={variant}
      data-grouped={grouped || undefined}
      data-status={status}
      data-deleted={deleted || undefined}
      data-highlighted={highlighted || undefined}
    >
      <div className="mn-message-row__gutter">
        {grouped ? (
          <span className="mn-message-row__gutter-time">{timeEl(true)}</span>
        ) : (
          (avatar ?? (
            <Avatar
              name={author.name}
              src={author.avatarSrc ?? null}
              size="md"
              decorative
              {...(author.seed !== undefined ? { seed: author.seed } : {})}
            />
          ))
        )}
      </div>
      <div className="mn-message-row__main">
        {grouped ? (
          <span id={authorId} className="mn-visually-hidden">
            {author.name}
          </span>
        ) : (
          <div className="mn-message-row__header">
            <span id={authorId} className="mn-message-row__author">
              {author.name}
            </span>
            {tags ? <span className="mn-message-row__tags">{tags}</span> : null}
            {timeEl(false)}
          </div>
        )}
        <div className="mn-message-row__body">
          {deleted ? <p className="mn-message-row__deleted">{m.deleted}</p> : children}
          {edited && !deleted ? (
            onShowEdits ? (
              <button
                type="button"
                className="mn-message-row__edited"
                aria-label={m.editedHistory}
                onClick={onShowEdits}
              >
                {m.edited}
              </button>
            ) : (
              <span className="mn-message-row__edited">{m.edited}</span>
            )
          ) : null}
        </div>
        {footer && !deleted ? <div className="mn-message-row__footer">{footer}</div> : null}
        {status === 'pending' ? (
          <p className="mn-message-row__status" role="status">
            {m.sending}
          </p>
        ) : null}
        {status === 'failed' ? (
          <div className="mn-message-row__status" data-status="failed" role="alert">
            <ErrorGlyph className="mn-message-row__status-icon" />
            <span>{error ?? m.failed}</span>
            {onRetry ? (
              <button type="button" className="mn-message-row__status-action" onClick={onRetry}>
                <RetryGlyph />
                {m.retry}
              </button>
            ) : null}
            {onDiscard ? (
              <button
                type="button"
                className="mn-message-row__status-action"
                data-tone="muted"
                onClick={onDiscard}
              >
                {m.discard}
              </button>
            ) : null}
          </div>
        ) : null}
        {hasReactions || (reactionAction && !deleted) ? (
          <div className="mn-message-row__reactions" role="group" aria-label={m.reactions}>
            {reactions?.map((r) => {
              const name = m.reaction(r.label ?? r.emoji, r.count);
              return onReactionToggle ? (
                <button
                  key={r.emoji}
                  type="button"
                  className="mn-message-row__reaction"
                  aria-pressed={r.reacted ?? false}
                  aria-label={name}
                  title={r.title}
                  data-reacted={r.reacted || undefined}
                  onClick={() => onReactionToggle(r.emoji)}
                >
                  <span aria-hidden>{r.emoji}</span>
                  <span aria-hidden className="mn-message-row__reaction-count">
                    {r.count}
                  </span>
                </button>
              ) : (
                <span
                  key={r.emoji}
                  className="mn-message-row__reaction"
                  role="img"
                  aria-label={name}
                  title={r.title}
                  data-reacted={r.reacted || undefined}
                >
                  <span aria-hidden>{r.emoji}</span>
                  <span aria-hidden className="mn-message-row__reaction-count">
                    {r.count}
                  </span>
                </span>
              );
            })}
            {deleted ? null : reactionAction}
          </div>
        ) : null}
        {showThread ? (
          onOpenThread ? (
            <button type="button" className="mn-message-row__thread" onClick={onOpenThread}>
              {m.replies(replyCount)}
            </button>
          ) : (
            <span className="mn-message-row__thread">{m.replies(replyCount)}</span>
          )
        ) : null}
      </div>
      {showActions ? (
        <div className="mn-message-row__actions" role="group" aria-label={m.actions}>
          {actions}
        </div>
      ) : null}
    </article>
  );
});
