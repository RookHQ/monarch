'use client';

import {
  forwardRef,
  useEffect,
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
import { CheckGlyph, ChevronRightGlyph, CloseGlyph } from '../../../internal/icons.js';
import { ChatGlyph, SendGlyph } from '../../../internal/shell/panel-icons.js';
import { Avatar } from '../../identity/avatar/index.js';
import { Button, IconButton } from '../../actions/button/index.js';
import { EmptyState } from '../../feedback/empty-state/index.js';
import { InlineErrorRetry } from '../../feedback/inline-error-retry/index.js';
import { Skeleton } from '../../feedback/skeleton/index.js';
import { SegmentedControl } from '../../forms/segmented-control/index.js';
import { Textarea } from '../../forms/text-field/index.js';
import { RelativeTime, type DateInput } from '../../data-display/relative-time/index.js';
import { useConfirm } from '../../overlays/confirm-dialog/index.js';

/** Someone who wrote a comment. */
export interface CommentAuthor {
  /** Display name. */
  name: string;
  /** Avatar image. Initials are shown without it. */
  avatarSrc?: string | null | undefined;
  /** Stable id for the avatar colour, so a renamed person keeps their colour. */
  seed?: string | undefined;
}

/** One comment inside a thread. */
export interface CommentItem {
  id: string;
  author: CommentAuthor;
  /** The comment text or rich content. Monarch does not sanitise HTML; the product does. */
  body: ReactNode;
  /** When it was written. A Date, epoch milliseconds or an ISO 8601 string. */
  createdAt: DateInput;
  /** Shows the Delete button for this comment (with `onDeleteComment`). Default false. */
  canDelete?: boolean | undefined;
}

/** A discussion anchored to a passage of the document (or to the whole record). */
export interface CommentThread {
  id: string;
  /** The passage the thread is about, shown as a quote. Leave out for a general comment. */
  quote?: ReactNode;
  /** The comments, oldest first. The first one starts the thread. */
  comments: readonly CommentItem[];
  /** Last activity, shown in the list. Default: the newest comment's `createdAt`. */
  updatedAt?: DateInput | undefined;
  /** The thread is resolved. It shows under Resolved and cannot be replied to until reopened. */
  resolved?: boolean | undefined;
  /** Shows Resolve / Reopen for this thread (with `onResolvedChange`). Default true. */
  canResolve?: boolean | undefined;
  /** Shows the reply box for this thread (with `onReply`). Default true. */
  canReply?: boolean | undefined;
}

/** Which threads the panel lists. */
export type CommentsFilter = 'open' | 'resolved';

export interface CommentsPanelMessages {
  /** Panel heading. Default "Comments". */
  title: string;
  close: string;
  /** Name of the Open / Resolved switch. Default "Show". */
  filter: string;
  open: string;
  resolved: string;
  /** Back from a thread to the list. Default "All comments". */
  back: string;
  loading: string;
  emptyOpen: string;
  emptyResolved: string;
  /** Shown under the empty title. Default "Select text to start a discussion." */
  emptyHint: string;
  /** Count under each thread in the list. */
  commentCount: (count: number) => string;
  /** Label of the quote in a thread. Default "Quoted text". */
  quote: string;
  resolve: string;
  reopen: string;
  /** Announced after a thread is resolved. */
  resolvedNotice: string;
  /** Announced after a thread is reopened. */
  reopenedNotice: string;
  /** Shown instead of the reply box on a resolved thread. */
  resolvedHint: string;
  reply: string;
  replyPlaceholder: string;
  send: string;
  /** Hint under the reply box. */
  replyHint: string;
  /** Shown when `onReply` or `onResolvedChange` rejects. */
  actionFailed: string;
  /** Accessible name of a comment's delete button. */
  deleteComment: (author: string) => string;
  deleteTitle: string;
  deleteDescription: string;
  deleteConfirm: string;
  cancel: string;
}

const DEFAULT_MESSAGES: CommentsPanelMessages = {
  title: 'Comments',
  close: 'Close comments',
  filter: 'Show',
  open: 'Open',
  resolved: 'Resolved',
  back: 'All comments',
  loading: 'Loading comments…',
  emptyOpen: 'No open comments',
  emptyResolved: 'No resolved comments',
  emptyHint: 'Select text to start a discussion.',
  commentCount: (n) => (n === 1 ? '1 comment' : `${n} comments`),
  quote: 'Quoted text',
  resolve: 'Resolve',
  reopen: 'Reopen',
  resolvedNotice: 'Thread resolved',
  reopenedNotice: 'Thread reopened',
  resolvedHint: 'This thread is resolved. Reopen it to reply.',
  reply: 'Reply',
  replyPlaceholder: 'Reply…',
  send: 'Send reply',
  replyHint: 'Enter to send, Shift+Enter for a new line.',
  actionFailed: 'That didn’t work. Try again.',
  deleteComment: (author) => `Delete comment by ${author}`,
  deleteTitle: 'Delete this comment?',
  deleteDescription: 'It is removed for everyone. This can’t be undone.',
  deleteConfirm: 'Delete comment',
  cancel: 'Cancel',
};

export interface CommentsPanelProps extends Omit<HTMLAttributes<HTMLElement>, 'title'> {
  /** The threads to list. `null` while loading. Threads are filtered by `resolved` for the Open / Resolved switch. */
  threads: readonly CommentThread[] | null;
  /** Loading the threads failed. Shown as an inline error; with `onRetry` it has Try again. */
  error?: ReactNode;
  onRetry?: (() => unknown) | undefined;
  /** Open or resolved threads (controlled). */
  filter?: CommentsFilter | undefined;
  /** Initial filter when uncontrolled. Default `open`. */
  defaultFilter?: CommentsFilter | undefined;
  /** Called when the person switches between Open and Resolved. Fetch the other set here if you load them separately. */
  onFilterChange?: ((filter: CommentsFilter) => void) | undefined;
  /** The thread shown in full (controlled). `null` shows the list. */
  selectedThreadId?: string | null | undefined;
  /** Initial thread when uncontrolled, e.g. the one whose highlight was clicked. */
  defaultSelectedThreadId?: string | null | undefined;
  /**
   * Called when a thread is opened from the list (with its id) or closed (with
   * `null`). Use it to select and scroll to the quoted passage in the editor.
   */
  onSelectedThreadChange?: ((threadId: string | null) => void) | undefined;
  /**
   * Post a reply. Return a promise to keep the box busy until it settles; the
   * box clears on success and shows `messages.actionFailed` on rejection.
   * Without it there is no reply box.
   */
  onReply?: ((threadId: string, body: string) => unknown) | undefined;
  /**
   * Resolve (`true`) or reopen (`false`) a thread. A returned promise keeps the
   * button busy. On success the panel goes back to the list and announces it.
   * Without it there is no Resolve button.
   */
  onResolvedChange?: ((threadId: string, resolved: boolean) => unknown) | undefined;
  /**
   * Delete a comment (shown on comments with `canDelete`). The panel asks for
   * confirmation first; a returned promise shows progress in the dialog and a
   * rejection shows its message there.
   */
  onDeleteComment?: ((threadId: string, commentId: string) => unknown) | undefined;
  /** Shows a close button in the header. */
  onClose?: (() => void) | undefined;
  /** Heading level of the title. Default `h2`. */
  headingLevel?: 'h2' | 'h3' | 'h4' | undefined;
  /** Reference time for relative timestamps. Pass it for identical server and client output. */
  now?: DateInput | undefined;
  locale?: string | undefined;
  /** Interface text, for translation. */
  messages?: Partial<CommentsPanelMessages> | undefined;
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

function lastActivity(thread: CommentThread): DateInput | undefined {
  return thread.updatedAt ?? thread.comments[thread.comments.length - 1]?.createdAt;
}

/**
 * The comments side panel for a document or record: Open and Resolved
 * threads, each with its quoted passage, replies, Resolve / Reopen and
 * confirmed delete. The product supplies the threads and does the saving;
 * the panel never fetches and knows nothing about the editor, so selecting a
 * thread calls `onSelectedThreadChange` for the product to highlight the passage.
 */
export const CommentsPanel = forwardRef<HTMLElement, CommentsPanelProps>(function CommentsPanel(
  {
    threads,
    error,
    onRetry,
    filter: filterProp,
    defaultFilter = 'open',
    onFilterChange,
    selectedThreadId: selectedProp,
    defaultSelectedThreadId = null,
    onSelectedThreadChange,
    onReply,
    onResolvedChange,
    onDeleteComment,
    onClose,
    headingLevel = 'h2',
    now,
    locale,
    messages: messagesProp,
    className,
    ...rest
  },
  ref,
) {
  const m = { ...DEFAULT_MESSAGES, ...messagesProp };
  const Heading = headingLevel;
  const id = useId();
  const headingId = `${id}-title`;

  const [filterState, setFilterState] = useState<CommentsFilter>(defaultFilter);
  const filter = filterProp ?? filterState;
  const [selectedState, setSelectedState] = useState<string | null>(defaultSelectedThreadId);
  const selectedId = selectedProp !== undefined ? selectedProp : selectedState;

  const [draft, setDraft] = useState('');
  const [busy, setBusy] = useState<'reply' | 'resolve' | null>(null);
  const [actionError, setActionError] = useState(false);
  const [notice, setNotice] = useState('');
  const { confirm, element: confirmElement } = useConfirm();

  const listRefs = useRef(new Map<string, HTMLButtonElement>());
  const backRef = useRef<HTMLButtonElement>(null);
  const returnTo = useRef<string | null>(null);
  const moveFocus = useRef<'thread' | 'list' | null>(null);

  const visible = (threads ?? []).filter((t) => !!t.resolved === (filter === 'resolved'));
  const thread = selectedId ? visible.find((t) => t.id === selectedId) : undefined;

  const select = (next: string | null) => {
    if (selectedProp === undefined) setSelectedState(next);
    onSelectedThreadChange?.(next);
  };

  // A new thread starts with an empty reply box and no stale error.
  useEffect(() => {
    setDraft('');
    setActionError(false);
  }, [thread?.id]);

  // Focus follows the view: into the thread when opened, back to its row when closed.
  useEffect(() => {
    const target = moveFocus.current;
    moveFocus.current = null;
    if (target === 'thread' && thread) backRef.current?.focus();
    if (target === 'list' && !thread && returnTo.current) {
      listRefs.current.get(returnTo.current)?.focus();
    }
  });

  const open = (threadId: string) => {
    moveFocus.current = 'thread';
    setNotice('');
    select(threadId);
  };
  const back = () => {
    returnTo.current = thread?.id ?? null;
    moveFocus.current = 'list';
    select(null);
  };

  const changeFilter = (next: CommentsFilter) => {
    if (filterProp === undefined) setFilterState(next);
    onFilterChange?.(next);
    if (selectedId) select(null);
  };

  const run = async (kind: 'reply' | 'resolve', work: () => unknown, onDone: () => void) => {
    setActionError(false);
    let result: unknown;
    try {
      result = work();
    } catch {
      setActionError(true);
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
      setActionError(true);
    } finally {
      setBusy(null);
    }
  };

  const sendReply = () => {
    if (!thread || !onReply || busy) return;
    const body = draft.trim();
    if (!body) return;
    void run(
      'reply',
      () => onReply(thread.id, body),
      () => setDraft(''),
    );
  };

  const setResolved = (value: boolean) => {
    if (!thread || !onResolvedChange || busy) return;
    const threadId = thread.id;
    void run(
      'resolve',
      () => onResolvedChange(threadId, value),
      () => {
        setNotice(value ? m.resolvedNotice : m.reopenedNotice);
        returnTo.current = null;
        select(null);
      },
    );
  };

  const deleteComment = async (threadId: string, comment: CommentItem) => {
    if (!onDeleteComment) return;
    await confirm({
      title: m.deleteTitle,
      description: m.deleteDescription,
      tone: 'danger',
      confirmLabel: m.deleteConfirm,
      cancelLabel: m.cancel,
      onConfirm: () => onDeleteComment(threadId, comment.id),
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

  const hintId = `${id}-hint`;
  const errorId = `${id}-error`;

  let body: ReactNode;
  if (error) {
    body = (
      <div className="mn-comments-panel__state">
        {onRetry ? (
          <InlineErrorRetry onRetry={onRetry}>{error}</InlineErrorRetry>
        ) : (
          <p role="alert" className="mn-comments-panel__error">
            {error}
          </p>
        )}
      </div>
    );
  } else if (threads === null) {
    body = (
      <div className="mn-comments-panel__state" role="status" aria-label={m.loading}>
        <Skeleton shape="rect" height="3.5rem" />
        <Skeleton shape="rect" height="3.5rem" />
        <Skeleton shape="rect" height="3.5rem" />
      </div>
    );
  } else if (thread) {
    const canResolve = !!onResolvedChange && thread.canResolve !== false;
    const canReply = !!onReply && thread.canReply !== false && !thread.resolved;
    body = (
      <div className="mn-comments-panel__thread" data-resolved={thread.resolved ? '' : undefined}>
        <div className="mn-comments-panel__thread-bar">
          <Button
            ref={backRef}
            variant="ghost"
            size="sm"
            className="mn-comments-panel__back"
            leadingIcon={<ChevronRightGlyph className="mn-comments-panel__back-icon" />}
            onClick={back}
          >
            {m.back}
          </Button>
          {canResolve ? (
            <Button
              variant="ghost"
              size="sm"
              leadingIcon={thread.resolved ? undefined : <CheckGlyph />}
              loading={busy === 'resolve'}
              onClick={() => setResolved(!thread.resolved)}
            >
              {thread.resolved ? m.reopen : m.resolve}
            </Button>
          ) : null}
        </div>
        {thread.quote ? (
          <blockquote className="mn-comments-panel__quote" aria-label={m.quote}>
            {thread.quote}
          </blockquote>
        ) : null}
        <ol className="mn-comments-panel__comments">
          {thread.comments.map((comment) => (
            <li key={comment.id} className="mn-comments-panel__comment">
              <Avatar
                name={comment.author.name}
                src={comment.author.avatarSrc ?? null}
                {...(comment.author.seed ? { seed: comment.author.seed } : {})}
                size="sm"
                decorative
              />
              <div className="mn-comments-panel__comment-main">
                <div className="mn-comments-panel__comment-head">
                  <span className="mn-comments-panel__author">{comment.author.name}</span>
                  <RelativeTime
                    className="mn-comments-panel__time"
                    date={comment.createdAt}
                    {...(now !== undefined ? { now } : {})}
                    {...(locale ? { locale } : {})}
                  />
                  {onDeleteComment && comment.canDelete ? (
                    <IconButton
                      size="xs"
                      className="mn-comments-panel__delete"
                      label={m.deleteComment(comment.author.name)}
                      icon={<TrashGlyph />}
                      onClick={() => void deleteComment(thread.id, comment)}
                    />
                  ) : null}
                </div>
                <div className="mn-comments-panel__comment-body">{comment.body}</div>
              </div>
            </li>
          ))}
        </ol>
        {canReply ? (
          <form className="mn-comments-panel__reply" onSubmit={onSubmit}>
            <Textarea
              className="mn-comments-panel__input"
              aria-label={m.reply}
              aria-describedby={actionError ? `${hintId} ${errorId}` : hintId}
              aria-invalid={actionError || undefined}
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
              className="mn-comments-panel__send"
              label={m.send}
              icon={<SendGlyph />}
              loading={busy === 'reply'}
              disabled={!draft.trim()}
            />
            <p id={hintId} className="mn-comments-panel__hint">
              {m.replyHint}
            </p>
          </form>
        ) : thread.resolved ? (
          <p className="mn-comments-panel__hint mn-comments-panel__resolved-hint">
            {m.resolvedHint}
          </p>
        ) : null}
        {actionError ? (
          <p id={errorId} role="alert" className="mn-comments-panel__error">
            {m.actionFailed}
          </p>
        ) : null}
      </div>
    );
  } else if (visible.length === 0) {
    body = (
      <EmptyState
        className="mn-comments-panel__empty"
        size="compact"
        headingLevel="p"
        icon={<ChatGlyph />}
        title={filter === 'open' ? m.emptyOpen : m.emptyResolved}
        description={filter === 'open' ? m.emptyHint : undefined}
      />
    );
  } else {
    body = (
      <ul className="mn-comments-panel__list">
        {visible.map((t) => {
          const starter = t.comments[0];
          const name = starter?.author.name ?? '';
          const when = lastActivity(t);
          return (
            <li key={t.id}>
              <button
                type="button"
                ref={(el) => {
                  if (el) listRefs.current.set(t.id, el);
                  else listRefs.current.delete(t.id);
                }}
                className="mn-comments-panel__item"
                onClick={() => open(t.id)}
              >
                {starter ? (
                  <Avatar
                    name={name}
                    src={starter.author.avatarSrc ?? null}
                    {...(starter.author.seed ? { seed: starter.author.seed } : {})}
                    size="sm"
                    decorative
                  />
                ) : null}
                <span className="mn-comments-panel__item-main">
                  <span className="mn-comments-panel__author">{name}</span>
                  {t.quote ? <q className="mn-comments-panel__item-quote">{t.quote}</q> : null}
                  <span className="mn-comments-panel__item-meta">
                    {m.commentCount(t.comments.length)}
                    {when !== undefined ? (
                      <>
                        {' · '}
                        <RelativeTime
                          date={when}
                          tooltip={false}
                          {...(now !== undefined ? { now } : {})}
                          {...(locale ? { locale } : {})}
                        />
                      </>
                    ) : null}
                  </span>
                </span>
              </button>
            </li>
          );
        })}
      </ul>
    );
  }

  return (
    <aside
      ref={ref}
      aria-labelledby={headingId}
      {...rest}
      className={cx('mn-comments-panel', className)}
      data-filter={filter}
      data-view={thread ? 'thread' : 'list'}
    >
      <div className="mn-comments-panel__header">
        <Heading id={headingId} className="mn-comments-panel__title">
          {m.title}
        </Heading>
        {onClose ? (
          <IconButton size="sm" label={m.close} icon={<CloseGlyph />} onClick={onClose} />
        ) : null}
      </div>
      <div className="mn-comments-panel__filter">
        <SegmentedControl<CommentsFilter>
          aria-label={m.filter}
          size="sm"
          block
          value={filter}
          onValueChange={changeFilter}
          options={[
            { value: 'open', label: m.open },
            { value: 'resolved', label: m.resolved },
          ]}
        />
      </div>
      <div className="mn-comments-panel__body" aria-busy={threads === null || undefined}>
        {body}
      </div>
      <p className="mn-visually-hidden" role="status" aria-live="polite">
        {notice}
      </p>
      {confirmElement}
    </aside>
  );
});
