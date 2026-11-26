'use client';

import {
  forwardRef,
  useCallback,
  useEffect,
  useId,
  useMemo,
  useRef,
  useState,
  type HTMLAttributes,
  type ReactNode,
} from 'react';
import { cx } from '../../../internal/cx.js';
import { CloseGlyph, RetryGlyph, BotGlyph } from '../../../internal/icons.js';
import { isSafeUrl } from '../../../internal/sanitize.js';
import { Button, IconButton } from '../../actions/button/index.js';
import {
  useUndoToast,
  type UndoableOptions,
  type UndoOutcome,
  type UndoToastMessages,
} from '../../feedback/undo-toast/index.js';

/** One field the AI changed. */
export interface AiChange {
  /** The field's name, e.g. "Status". */
  label: ReactNode;
  /** The value before the change. */
  before: ReactNode;
  /** The value the AI set. */
  after: ReactNode;
}

/** Every user-visible string in AiUndoBanner. */
export interface AiUndoBannerMessages {
  /** Read before the title so the banner is identified as an AI change. Default "AI change:". */
  aiPrefix: string;
  /** Undo button. Default "Undo". */
  undo: string;
  /** Audit link text. Default "View in audit log". */
  audit: string;
  /** Dismiss button label. Default "Dismiss". */
  dismiss: string;
  /** Summary of the changes disclosure when there are more than three. */
  showChanges: (count: number) => string;
  /** Read between the old and the new value. Default "changed from" / "to". */
  changedFrom: string;
  changedTo: string;
  /** Shown and announced after a successful undo. Default "Changes undone". */
  undone: string;
  /** Shown when `onUndo` fails. Default "Couldn’t undo. Try again." */
  undoFailed: string;
  /** Shown when the Undo window has closed. Default "Undo is no longer available." */
  expired: string;
}

export const AI_UNDO_BANNER_MESSAGES: AiUndoBannerMessages = {
  aiPrefix: 'AI change:',
  undo: 'Undo',
  audit: 'View in audit log',
  dismiss: 'Dismiss',
  showChanges: (count) => `Show ${count} changes`,
  changedFrom: 'changed from',
  changedTo: 'to',
  undone: 'Changes undone',
  undoFailed: 'Couldn’t undo. Try again.',
  expired: 'Undo is no longer available.',
};

/** Where the banner is in its life. */
export type AiUndoBannerState = 'active' | 'undoing' | 'undone' | 'failed' | 'expired';

export interface AiUndoBannerProps extends Omit<HTMLAttributes<HTMLDivElement>, 'title'> {
  /** What the AI did, in the past tense: "AI changed 3 fields". */
  title: ReactNode;
  /** One line of detail. */
  description?: ReactNode;
  /** The fields that changed, before and after. More than three collapse into a disclosure. */
  changes?: readonly AiChange[] | undefined;
  /**
   * Reverts the change. May return a promise: Undo shows a spinner until it
   * settles, then the banner says "Changes undone" (or offers Undo again).
   */
  onUndo: () => unknown;
  /** Link to the audit log entry for this change. Unsafe URLs are dropped. */
  auditHref?: string | undefined;
  /**
   * Milliseconds Undo stays available. The countdown pauses while the
   * pointer is over the banner or focus is inside it. Default: no limit.
   */
  duration?: number | undefined;
  /** Called once when `duration` runs out without an undo. */
  onExpire?: (() => void) | undefined;
  /** Shows a Dismiss button. */
  onDismiss?: (() => void) | undefined;
  /** Replaces any of the English strings. */
  messages?: Partial<AiUndoBannerMessages> | undefined;
}

/** Runs out after `duration` ms of unpaused time. */
function useExpiry(duration: number | undefined, active: boolean, onExpire: () => void) {
  const [paused, setPaused] = useState(false);
  const [expired, setExpired] = useState(false);
  const left = useRef(duration ?? Infinity);
  const expire = useRef(onExpire);
  expire.current = onExpire;

  useEffect(() => {
    left.current = duration ?? Infinity;
    setExpired(false);
  }, [duration]);

  useEffect(() => {
    if (!active || paused || expired || !Number.isFinite(left.current)) return;
    const started = Date.now();
    const t = setTimeout(() => {
      left.current = 0;
      setExpired(true);
      expire.current();
    }, left.current);
    return () => {
      clearTimeout(t);
      left.current = Math.max(0, left.current - (Date.now() - started));
    };
  }, [active, paused, expired, duration]);

  return { expired, setPaused };
}

/**
 * An inline banner after the AI changed something on the person's behalf
 * ("AI changed 3 fields"): what changed, before and after, an Undo and a link
 * to the audit log. With `duration`, Undo is offered for a limited time that
 * pauses while the person is reading it. For a toast instead, use
 * `useAiUndoToast()`.
 */
export const AiUndoBanner = forwardRef<HTMLDivElement, AiUndoBannerProps>(function AiUndoBanner(
  {
    title,
    description,
    changes,
    onUndo,
    auditHref,
    duration,
    onExpire,
    onDismiss,
    messages: messagesProp,
    className,
    onPointerEnter,
    onPointerLeave,
    onFocus,
    onBlur,
    ...rest
  },
  ref,
) {
  const messages = { ...AI_UNDO_BANNER_MESSAGES, ...messagesProp };
  const titleId = `${useId()}-title`;
  const [undo, setUndo] = useState<'idle' | 'undoing' | 'undone' | 'failed'>('idle');
  const mounted = useRef(true);
  useEffect(() => {
    mounted.current = true;
    return () => {
      mounted.current = false;
    };
  }, []);
  const { expired, setPaused } = useExpiry(duration, undo === 'idle' || undo === 'failed', () =>
    onExpire?.(),
  );
  const state: AiUndoBannerState =
    undo === 'undone' ? 'undone' : expired ? 'expired' : undo === 'idle' ? 'active' : undo;
  const audit = auditHref && isSafeUrl(auditHref) ? auditHref : undefined;
  const rootRef = useRef<HTMLDivElement | null>(null);
  const hover = useRef(false);
  const focus = useRef(false);
  const syncPause = () => setPaused(hover.current || focus.current);

  // Undo goes away once used or expired; keep focus in the banner, not on the body.
  useEffect(() => {
    if (
      (state === 'undone' || state === 'expired') &&
      typeof document !== 'undefined' &&
      document.activeElement === document.body &&
      focus.current
    )
      rootRef.current?.focus();
  }, [state]);

  const runUndo = () => {
    if (undo === 'undoing' || undo === 'undone' || expired) return;
    setUndo('undoing');
    let result: unknown;
    try {
      result = onUndo();
    } catch {
      setUndo('failed');
      return;
    }
    Promise.resolve(result).then(
      () => mounted.current && setUndo('undone'),
      () => mounted.current && setUndo('failed'),
    );
  };

  const list =
    changes && changes.length ? (
      <ul className="mn-ai-undo-banner__changes">
        {changes.map((c, i) => (
          <li key={i} className="mn-ai-undo-banner__change">
            <span className="mn-ai-undo-banner__change-label">{c.label}</span>{' '}
            <span className="mn-visually-hidden">{messages.changedFrom} </span>
            <del className="mn-ai-undo-banner__before">{c.before}</del>
            <span className="mn-ai-undo-banner__arrow" aria-hidden>
              {' → '}
            </span>
            <span className="mn-visually-hidden"> {messages.changedTo} </span>
            <ins className="mn-ai-undo-banner__after">{c.after}</ins>
          </li>
        ))}
      </ul>
    ) : null;

  const status =
    state === 'undone'
      ? messages.undone
      : state === 'failed'
        ? messages.undoFailed
        : state === 'expired'
          ? messages.expired
          : '';
  const canUndo = state === 'active' || state === 'undoing' || state === 'failed';

  return (
    // The pointer and focus listeners only pause the Undo countdown; they are not interactions.
    // eslint-disable-next-line jsx-a11y/no-noninteractive-element-interactions
    <div
      ref={(node) => {
        rootRef.current = node;
        if (typeof ref === 'function') ref(node);
        else if (ref) ref.current = node;
      }}
      role="group"
      aria-labelledby={titleId}
      tabIndex={canUndo ? undefined : -1}
      {...rest}
      className={cx('mn-ai-undo-banner', className)}
      data-state={state}
      onPointerEnter={(e) => {
        hover.current = true;
        syncPause();
        onPointerEnter?.(e);
      }}
      onPointerLeave={(e) => {
        hover.current = false;
        syncPause();
        onPointerLeave?.(e);
      }}
      onFocus={(e) => {
        focus.current = true;
        syncPause();
        onFocus?.(e);
      }}
      onBlur={(e) => {
        if (!e.currentTarget.contains(e.relatedTarget as Node | null)) {
          focus.current = false;
          syncPause();
        }
        onBlur?.(e);
      }}
    >
      <span className="mn-ai-undo-banner__icon" aria-hidden>
        <BotGlyph />
      </span>
      <div className="mn-ai-undo-banner__content">
        <p id={titleId} className="mn-ai-undo-banner__title">
          <span className="mn-visually-hidden">{messages.aiPrefix} </span>
          {title}
        </p>
        {description ? <div className="mn-ai-undo-banner__description">{description}</div> : null}
        {list && changes && changes.length > 3 ? (
          <details className="mn-ai-undo-banner__details">
            <summary className="mn-ai-undo-banner__summary">
              {messages.showChanges(changes.length)}
            </summary>
            {list}
          </details>
        ) : (
          list
        )}
        <p
          role="status"
          className={cx('mn-ai-undo-banner__status', status ? undefined : 'mn-visually-hidden')}
        >
          {status}
        </p>
      </div>
      <div className="mn-ai-undo-banner__actions">
        {canUndo ? (
          <Button
            size="sm"
            variant="secondary"
            leadingIcon={<RetryGlyph />}
            loading={state === 'undoing'}
            onClick={runUndo}
          >
            {messages.undo}
          </Button>
        ) : null}
        {audit ? (
          <a className="mn-ai-undo-banner__audit" href={audit} rel="noopener noreferrer nofollow">
            {messages.audit}
          </a>
        ) : null}
        {onDismiss ? (
          <IconButton
            size="sm"
            label={messages.dismiss}
            icon={<CloseGlyph />}
            className="mn-ai-undo-banner__dismiss"
            onClick={onDismiss}
          />
        ) : null}
      </div>
    </div>
  );
});

export interface AiUndoableOptions extends Omit<UndoableOptions, 'description'> {
  /** One line of detail. Default: the labels of `changes`, comma-separated, when they are strings. */
  description?: ReactNode;
  /** The fields the AI changed; their labels become the default description. */
  changes?: readonly AiChange[] | undefined;
}

export interface UseAiUndoToastResult {
  /**
   * Shows "<title>. Undo" for an AI change the app has already applied.
   * Resolves `'undone'` or `'committed'`, like `useUndoToast().undoable`.
   */
  undoable: (options: AiUndoableOptions) => Promise<UndoOutcome>;
}

/**
 * The timed toast variant of AiUndoBanner, built on `useUndoToast`: "AI
 * changed 3 fields · Undo" for a few seconds, then the change stands.
 * Needs `ToastProvider`.
 */
export function useAiUndoToast(
  options: { messages?: Partial<UndoToastMessages> | undefined } = {},
): UseAiUndoToastResult {
  const { undoable: base } = useUndoToast(options.messages ? { messages: options.messages } : {});
  const undoable = useCallback(
    ({ changes, description, ...opts }: AiUndoableOptions) => {
      const labels = changes?.map((c) => c.label);
      const fallback =
        labels && labels.length && labels.every((l) => typeof l === 'string')
          ? labels.join(', ')
          : undefined;
      const detail = description ?? fallback;
      return base(detail === undefined ? opts : { ...opts, description: detail });
    },
    [base],
  );
  return useMemo(() => ({ undoable }), [undoable]);
}
