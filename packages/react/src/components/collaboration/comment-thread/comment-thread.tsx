'use client';

import {
  forwardRef,
  useId,
  useRef,
  useState,
  type FormEvent,
  type HTMLAttributes,
  type KeyboardEvent,
  type ReactNode,
  type SVGProps,
} from 'react';
import { cx } from '../../../internal/cx.js';
import { CheckGlyph } from '../../../internal/icons.js';
import { SendGlyph } from '../../../internal/shell/panel-icons.js';
import { Avatar } from '../../identity/avatar/index.js';
import { Button, IconButton } from '../../actions/button/index.js';
import { Textarea } from '../../forms/text-field/index.js';
import { RelativeTime, type DateInput } from '../../data-display/relative-time/index.js';
import { useConfirm } from '../../overlays/confirm-dialog/index.js';
import type { CommentAuthor, CommentItem } from '../comments-panel/index.js';

export interface CommentThreadMessages {
  /** Accessible name of the thread. Default "Thread started by {author}". */
  label: (author: string) => string;
  /** Label of the quote. Default "Quoted text". */
  quote: string;
  /** Button that reveals hidden earlier replies. Default "Show {count} earlier replies". */
  showEarlier: (count: number) => string;
  /** Accessible name of the reply box. Default "Reply". */
  reply: string;
  replyPlaceholder: string;
  /** Accessible name of the send button. Default "Send reply". */
  send: string;
  /** Hint under the reply box. Default "Enter to send, Shift+Enter for a new line." */
  replyHint: string;
  resolve: string;
  reopen: string;
  /** Shown instead of the reply box while resolved. */
  resolvedHint: string;
  /** Announced after resolving or reopening. */
  resolvedNotice: string;
  reopenedNotice: string;
  /** Announced after a reply is posted. Default "Reply sent". */
  sentNotice: string;
  /** Shown when `onReply` or `onResolvedChange` rejects. */
  actionFailed: string;
  /** Accessible name of a comment's delete button. */
  deleteComment: (author: string) => string;
  deleteTitle: string;
  deleteDescription: string;
  deleteConfirm: string;
  cancel: string;
}

const DEFAULT_MESSAGES: CommentThreadMessages = {
  label: (author) => `Thread started by ${author}`,
  quote: 'Quoted text',
  showEarlier: (n) => (n === 1 ? 'Show 1 earlier reply' : `Show ${n} earlier replies`),
  reply: 'Reply',
  replyPlaceholder: 'Reply…',
  send: 'Send reply',
  replyHint: 'Enter to send, Shift+Enter for a new line.',
  resolve: 'Resolve',
  reopen: 'Reopen',
  resolvedHint: 'This thread is resolved. Reopen it to reply.',
  resolvedNotice: 'Thread resolved',
  reopenedNotice: 'Thread reopened',
  sentNotice: 'Reply sent',
  actionFailed: 'That didn’t work. Try again.',
  deleteComment: (author) => `Delete comment by ${author}`,
  deleteTitle: 'Delete this comment?',
  deleteDescription: 'It is removed for everyone. This can’t be undone.',
  deleteConfirm: 'Delete comment',
  cancel: 'Cancel',
};

export interface CommentThreadProps extends Omit<HTMLAttributes<HTMLElement>, 'children'> {
  /** The comments, oldest first. The first one starts the thread; the rest are replies. */
  comments: readonly CommentItem[];
  /** The passage the thread is about, shown as a quote above it. */
  quote?: ReactNode;
  /** The thread is resolved: muted, and the reply box is replaced by a hint. Exposed as `data-resolved`. */
  resolved?: boolean | undefined;
  /**
   * Resolve (`true`) or reopen (`false`). A returned promise keeps the button
   * busy; a rejection shows `messages.actionFailed`. Without it there is no
   * Resolve button.
   */
  onResolvedChange?: ((resolved: boolean) => unknown) | undefined;
  /**
   * Post a reply. A returned promise keeps the box busy; it clears on success
   * and shows `messages.actionFailed` on rejection, keeping the draft.
   * Without it there is no reply box.
   */
  onReply?: ((body: string) => unknown) | undefined;
  /** Delete a comment that has `canDelete`, after a confirmation dialog. */
  onDeleteComment?: ((commentId: string) => unknown) | undefined;
  /**
   * Replies shown before the rest fold behind "Show N earlier replies": the
   * starting comment and the latest replies stay visible. Default 3.
   */
  visibleReplies?: number | undefined;
  /** Reference time for relative timestamps. Pass it for identical server and client output. */
  now?: DateInput | undefined;
  locale?: string | undefined;
  /** Interface text, for translation. */
  messages?: Partial<CommentThreadMessages> | undefined;
}

function isThenable(v: unknown): v is PromiseLike<unknown> {
  return !!v && typeof (v as { then?: unknown }).then === 'function';
}

function TrashGlyph(props: SVGProps<SVGSVGElement>) {
  return (
    <svg
      viewBox="0 0 16 16"
      fill="none"
      stroke="currentColor"
      strokeWidth="1.5"
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
      focusable="false"
      {...props}
    >
      <path d="M2.5 4.5h11M6.5 4.5V3h3v1.5M4 4.5l.6 8.5h6.8l.6-8.5M6.75 7v4M9.25 7v4" />
    </svg>
  );
}

function authorAvatar(author: CommentAuthor) {
  return (
    <Avatar
      name={author.name}
      src={author.avatarSrc ?? null}
      {...(author.seed ? { seed: author.seed } : {})}
      size="sm"
      decorative
    />
  );
}

/**
 * One threaded discussion: the starting comment, its replies (older ones
 * folded), an optional quote, Resolve / Reopen and a reply box. Use it
 * inline beside the thing being discussed; CommentsPanel lists many
 * threads. The product saves; returned promises keep controls busy.
 */
export const CommentThread = forwardRef<HTMLElement, CommentThreadProps>(function CommentThread(
  {
    comments,
    quote,
    resolved = false,
    onResolvedChange,
    onReply,
    onDeleteComment,
    visibleReplies = 3,
    now,
    locale,
    messages: messagesProp,
    className,
    ...rest
  },
  ref,
) {
  const m = { ...DEFAULT_MESSAGES, ...messagesProp };
  const id = useId();
  const [draft, setDraft] = useState('');
  const [busy, setBusy] = useState<'reply' | 'resolve' | null>(null);
  const [failed, setFailed] = useState(false);
  const [notice, setNotice] = useState('');
  const [expanded, setExpanded] = useState(false);
  const firstRevealed = useRef<HTMLLIElement>(null);
  const { confirm, element: confirmElement } = useConfirm();

  const [root, ...replies] = comments;
  const keep = Math.max(0, Math.floor(visibleReplies));
  const folded = expanded || replies.length <= keep + 1 ? 0 : replies.length - keep;
  const shownReplies = replies.slice(folded);

  const run = async (kind: 'reply' | 'resolve', work: () => unknown, onDone: () => void) => {
    setFailed(false);
    setNotice('');
    let result: unknown;
    try {
      result = work();
    } catch {
      setFailed(true);
      return;
    }
    if (!isThenable(result)) {
      onDone();
      return;
    }
    setBusy(kind);
    try {
      await result;
      onDone();
    } catch {
      setFailed(true);
    } finally {
      setBusy(null);
    }
  };

  const sendReply = () => {
    if (!onReply || busy) return;
    const body = draft.trim();
    if (!body) return;
    void run(
      'reply',
      () => onReply(body),
      () => {
        setDraft('');
        setNotice(m.sentNotice);
      },
    );
  };

  const setResolved = (value: boolean) => {
    if (!onResolvedChange || busy) return;
    void run(
      'resolve',
      () => onResolvedChange(value),
      () => setNotice(value ? m.resolvedNotice : m.reopenedNotice),
    );
  };

  const deleteComment = async (comment: CommentItem) => {
    if (!onDeleteComment) return;
    await confirm({
      title: m.deleteTitle,
      description: m.deleteDescription,
      tone: 'danger',
      confirmLabel: m.deleteConfirm,
      cancelLabel: m.cancel,
      onConfirm: () => onDeleteComment(comment.id),
    });
  };

  const onSubmit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    sendReply();
  };
  const onDraftKeyDown = (event: KeyboardEvent<HTMLTextAreaElement>) => {
    if (event.key === 'Enter' && !event.shiftKey && !event.nativeEvent.isComposing) {
      event.preventDefault();
      sendReply();
    }
  };

  const reveal = () => {
    setExpanded(true);
    // The button disappears; move focus to the first reply it revealed.
    requestAnimationFrame(() => firstRevealed.current?.focus());
  };

  const renderComment = (comment: CommentItem, kind: 'root' | 'reply', focusTarget = false) => (
    <li
      key={comment.id}
      ref={focusTarget ? firstRevealed : undefined}
      tabIndex={focusTarget ? -1 : undefined}
      className="mn-comment-thread__comment"
      data-kind={kind}
    >
      {authorAvatar(comment.author)}
      <div className="mn-comment-thread__main">
        <div className="mn-comment-thread__head">
          <span className="mn-comment-thread__author">{comment.author.name}</span>
          <RelativeTime
            className="mn-comment-thread__time"
            date={comment.createdAt}
            {...(now !== undefined ? { now } : {})}
            {...(locale ? { locale } : {})}
          />
          {onDeleteComment && comment.canDelete ? (
            <IconButton
              size="xs"
              className="mn-comment-thread__delete"
              label={m.deleteComment(comment.author.name)}
              icon={<TrashGlyph />}
              onClick={() => void deleteComment(comment)}
            />
          ) : null}
        </div>
        <div className="mn-comment-thread__body">{comment.body}</div>
      </div>
    </li>
  );

  const hintId = `${id}-hint`;
  const errorId = `${id}-error`;
  const canReply = !!onReply && !resolved;

  return (
    <section
      ref={ref}
      aria-label={root ? m.label(root.author.name) : undefined}
      {...rest}
      className={cx('mn-comment-thread', className)}
      data-resolved={resolved ? '' : undefined}
    >
      {quote || onResolvedChange ? (
        <div className="mn-comment-thread__top">
          {quote ? (
            <blockquote className="mn-comment-thread__quote" aria-label={m.quote}>
              {quote}
            </blockquote>
          ) : null}
          {onResolvedChange ? (
            <Button
              variant="ghost"
              size="sm"
              className="mn-comment-thread__resolve"
              leadingIcon={resolved ? undefined : <CheckGlyph />}
              loading={busy === 'resolve'}
              aria-pressed={resolved}
              onClick={() => setResolved(!resolved)}
            >
              {resolved ? m.reopen : m.resolve}
            </Button>
          ) : null}
        </div>
      ) : null}
      <ol className="mn-comment-thread__comments">
        {root ? renderComment(root, 'root') : null}
        {folded > 0 ? (
          <li className="mn-comment-thread__fold">
            <Button variant="ghost" size="sm" className="mn-comment-thread__more" onClick={reveal}>
              {m.showEarlier(folded)}
            </Button>
          </li>
        ) : null}
        {shownReplies.map((c, i) => renderComment(c, 'reply', expanded && i === 0))}
      </ol>
      {canReply ? (
        <form className="mn-comment-thread__reply" onSubmit={onSubmit}>
          <Textarea
            className="mn-comment-thread__input"
            aria-label={m.reply}
            aria-describedby={failed ? `${hintId} ${errorId}` : hintId}
            aria-invalid={failed || undefined}
            placeholder={m.replyPlaceholder}
            rows={2}
            value={draft}
            onChange={(e) => setDraft(e.target.value)}
            onKeyDown={onDraftKeyDown}
            readOnly={busy === 'reply'}
          />
          <IconButton
            type="submit"
            variant="primary"
            size="sm"
            className="mn-comment-thread__send"
            label={m.send}
            icon={<SendGlyph />}
            loading={busy === 'reply'}
            disabled={!draft.trim()}
          />
          <p id={hintId} className="mn-comment-thread__hint">
            {m.replyHint}
          </p>
        </form>
      ) : resolved && onReply ? (
        <p className="mn-comment-thread__hint">{m.resolvedHint}</p>
      ) : null}
      {failed ? (
        <p id={errorId} role="alert" className="mn-comment-thread__error">
          {m.actionFailed}
        </p>
      ) : null}
      <p className="mn-visually-hidden" role="status" aria-live="polite">
        {notice}
      </p>
      {confirmElement}
    </section>
  );
});
