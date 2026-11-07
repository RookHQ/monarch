'use client';

import {
  forwardRef,
  useEffect,
  useId,
  useRef,
  useState,
  type HTMLAttributes,
  type ReactNode,
} from 'react';
import { cx } from '../../../internal/cx.js';
import { BellGlyph, CloseGlyph } from '../../../internal/icons.js';
import { Button, IconButton } from '../../actions/button/index.js';
import { InlineErrorRetry } from '../../feedback/inline-error-retry/index.js';
import { Spinner } from '../../feedback/spinner/index.js';

/** What the thread body shows. */
export type ThreadPanelStatus = 'ready' | 'loading' | 'error' | 'deleted';

export interface ThreadPanelMessages {
  /** Panel heading. Default "Thread". */
  title: string;
  close: string;
  /** Follow toggle text when not following. Default "Follow". */
  follow: string;
  /** Follow toggle text when following. Default "Following". */
  following: string;
  /** Announced after following. */
  followedNotice: string;
  /** Announced after unfollowing. */
  unfollowedNotice: string;
  /** Announced when the follow change fails. */
  followFailed: string;
  /** Divider between the root message and the replies. */
  replyCount: (count: number) => string;
  /** Name of the scrolling replies region. Default "Replies". */
  replies: string;
  loading: string;
  /** Shown with `status="error"`. */
  error: string;
  /** Shown with `status="deleted"`. */
  deleted: string;
}

const DEFAULT_MESSAGES: ThreadPanelMessages = {
  title: 'Thread',
  close: 'Close thread',
  follow: 'Follow',
  following: 'Following',
  followedNotice: 'You’ll be notified about replies',
  unfollowedNotice: 'You won’t be notified about replies',
  followFailed: 'Thread notifications didn’t change. Try again.',
  replyCount: (n) => (n === 1 ? '1 reply' : `${n} replies`),
  replies: 'Replies',
  loading: 'Loading thread…',
  error: 'This thread couldn’t be loaded.',
  deleted: 'This thread was deleted.',
};

/** Distance from the bottom, in pixels, within which new replies keep the view pinned to the end. */
const PIN_THRESHOLD = 80;

export interface ThreadPanelProps extends Omit<HTMLAttributes<HTMLElement>, 'title'> {
  /** The message the thread answers, usually a `MessageRow`. Shown above the reply count. */
  root?: ReactNode;
  /** The replies, oldest first, usually `MessageRow`s (pass `grouped` from `shouldGroupMessages`). */
  children?: ReactNode;
  /** Number of replies, for the divider and to keep the view pinned to the newest reply. */
  replyCount: number;
  /** `loading` shows a spinner, `error` an error with Try again, `deleted` a notice with Close. Default `ready`. */
  status?: ThreadPanelStatus | undefined;
  /** Called by Try again when `status="error"`. A returned promise keeps the button busy. */
  onRetry?: (() => unknown) | undefined;
  /** Shows the close button. Also offered when the thread was deleted. */
  onClose?: (() => void) | undefined;
  /** The person follows the thread (gets notified about replies). Shows the Follow toggle with `onFollowingChange`. */
  following?: boolean | undefined;
  /**
   * Called with the new value when Follow is pressed. A returned promise keeps
   * the toggle busy; the result is announced, and a rejection announces
   * `messages.followFailed`.
   */
  onFollowingChange?: ((following: boolean) => unknown) | undefined;
  /** A note after the replies, e.g. "Showing the first 500 replies." */
  notice?: ReactNode;
  /** Above the composer: a typing indicator, for example. */
  typing?: ReactNode;
  /** The reply box, usually a `MessageComposer`. Pinned to the bottom of the panel. */
  composer?: ReactNode;
  /** Extra header controls before the close button. */
  headerActions?: ReactNode;
  /**
   * Called (after `seenDelay`) when the panel has shown `replyCount` replies
   * while the page is visible: the moment to mark the thread read.
   */
  onRepliesSeen?: ((replyCount: number) => void) | undefined;
  /** Milliseconds the replies must stay on screen before `onRepliesSeen`. Default 400. */
  seenDelay?: number | undefined;
  /** Heading level of the title. Default `h2`. */
  headingLevel?: 'h2' | 'h3' | 'h4' | undefined;
  /** Interface text, for translation. */
  messages?: Partial<ThreadPanelMessages> | undefined;
}

function isThenable(v: unknown): v is PromiseLike<unknown> {
  return !!v && typeof (v as { then?: unknown }).then === 'function';
}

/**
 * The side panel for one conversation thread: the root message, a reply
 * count divider, the replies in a scrolling log that stays pinned to the
 * newest reply, and the reply box. It owns layout, focus and announcements;
 * the product supplies the message rows, the composer and the data.
 */
export const ThreadPanel = forwardRef<HTMLElement, ThreadPanelProps>(function ThreadPanel(
  {
    root,
    children,
    replyCount,
    status = 'ready',
    onRetry,
    onClose,
    following = false,
    onFollowingChange,
    notice,
    typing,
    composer,
    headerActions,
    onRepliesSeen,
    seenDelay = 400,
    headingLevel = 'h2',
    messages: messagesProp,
    className,
    ...rest
  },
  ref,
) {
  const m = { ...DEFAULT_MESSAGES, ...messagesProp };
  const Heading = headingLevel;
  const headingId = `${useId()}-title`;
  const scrollRef = useRef<HTMLDivElement>(null);
  const pinned = useRef(true);
  const [followBusy, setFollowBusy] = useState(false);
  const [announcement, setAnnouncement] = useState('');

  const ready = status === 'ready';

  // Keep the newest reply in view: on first show and when replies arrive while pinned.
  useEffect(() => {
    const el = scrollRef.current;
    if (!el || !ready || !pinned.current) return;
    el.scrollTop = el.scrollHeight;
  }, [replyCount, ready]);

  // Read position: what stays on screen while the page is visible counts as seen.
  // The callback is read at fire time so a new function identity does not restart the delay.
  const seenRef = useRef(onRepliesSeen);
  seenRef.current = onRepliesSeen;
  const wantsSeen = !!onRepliesSeen;
  useEffect(() => {
    if (!wantsSeen || !ready || typeof document === 'undefined') return;
    if (document.visibilityState !== 'visible') return;
    const timer = setTimeout(() => seenRef.current?.(replyCount), seenDelay);
    return () => clearTimeout(timer);
  }, [wantsSeen, replyCount, ready, seenDelay]);

  const onScroll = () => {
    const el = scrollRef.current;
    if (!el) return;
    pinned.current = el.scrollHeight - el.scrollTop - el.clientHeight <= PIN_THRESHOLD;
  };

  const toggleFollow = async () => {
    if (!onFollowingChange || followBusy) return;
    const next = !following;
    setAnnouncement('');
    let result: unknown;
    try {
      result = onFollowingChange(next);
      if (isThenable(result)) {
        setFollowBusy(true);
        await result;
      }
      setAnnouncement(next ? m.followedNotice : m.unfollowedNotice);
    } catch {
      setAnnouncement(m.followFailed);
    } finally {
      setFollowBusy(false);
    }
  };

  let body: ReactNode;
  if (status === 'loading') {
    body = (
      <div className="mn-thread-panel__state">
        <Spinner label={m.loading} />
      </div>
    );
  } else if (status === 'error') {
    body = (
      <div className="mn-thread-panel__state">
        {onRetry ? (
          <InlineErrorRetry onRetry={onRetry}>{m.error}</InlineErrorRetry>
        ) : (
          <p role="alert" className="mn-thread-panel__state-text">
            {m.error}
          </p>
        )}
      </div>
    );
  } else if (status === 'deleted') {
    body = (
      <div className="mn-thread-panel__state" role="status">
        <p className="mn-thread-panel__state-text">{m.deleted}</p>
        {onClose ? (
          <Button size="sm" variant="secondary" onClick={onClose}>
            {m.close}
          </Button>
        ) : null}
      </div>
    );
  } else {
    body = (
      <>
        {root ? <div className="mn-thread-panel__root">{root}</div> : null}
        <div className="mn-thread-panel__divider">
          <span className="mn-thread-panel__count">{m.replyCount(replyCount)}</span>
        </div>
        <div className="mn-thread-panel__replies">{children}</div>
        {notice ? <p className="mn-thread-panel__notice">{notice}</p> : null}
      </>
    );
  }

  return (
    <section
      ref={ref}
      aria-labelledby={headingId}
      {...rest}
      className={cx('mn-thread-panel', className)}
      data-status={status}
    >
      <div className="mn-thread-panel__header">
        <Heading id={headingId} className="mn-thread-panel__title">
          {m.title}
        </Heading>
        <div className="mn-thread-panel__actions">
          {onFollowingChange && ready ? (
            <Button
              size="sm"
              variant="ghost"
              className="mn-thread-panel__follow"
              aria-pressed={following}
              data-following={following ? '' : undefined}
              leadingIcon={<BellGlyph />}
              loading={followBusy}
              onClick={() => void toggleFollow()}
            >
              {following ? m.following : m.follow}
            </Button>
          ) : null}
          {headerActions}
          {onClose ? (
            <IconButton size="sm" label={m.close} icon={<CloseGlyph />} onClick={onClose} />
          ) : null}
        </div>
      </div>
      <div
        ref={scrollRef}
        className="mn-thread-panel__scroll"
        role="log"
        aria-live="polite"
        aria-relevant="additions"
        aria-label={m.replies}
        aria-busy={status === 'loading' || undefined}
        // A scrolling history must be reachable by keyboard (WCAG 2.1.1).
        // eslint-disable-next-line jsx-a11y/no-noninteractive-tabindex
        tabIndex={0}
        onScroll={onScroll}
      >
        {body}
      </div>
      {typing || composer ? (
        <div className="mn-thread-panel__footer">
          {typing ? <div className="mn-thread-panel__typing">{typing}</div> : null}
          {composer}
        </div>
      ) : null}
      <p className="mn-visually-hidden" role="status" aria-live="polite">
        {announcement}
      </p>
    </section>
  );
});
