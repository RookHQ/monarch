'use client';

import {
  forwardRef,
  useCallback,
  useEffect,
  useLayoutEffect,
  useRef,
  useState,
  type HTMLAttributes,
  type ReactNode,
  type RefObject,
} from 'react';
import { cx } from '../../../internal/cx.js';
import { RetryGlyph, WarningGlyph } from '../../../internal/icons.js';
import { Button } from '../../actions/button/index.js';

// useLayoutEffect warns during server rendering; the effect only matters in the browser.
const useIsoLayoutEffect = typeof window === 'undefined' ? useEffect : useLayoutEffect;

export type InlineErrorRetryFocusTarget =
  RefObject<HTMLElement | null> | (() => HTMLElement | null | undefined);

export interface InlineErrorRetryProps extends Omit<HTMLAttributes<HTMLDivElement>, 'children'> {
  /** The failure in plain words, e.g. "Your earnings didn't load." Supplied by the product. */
  children: ReactNode;
  /**
   * Called by Try again. Return a promise (a refetch) and the button stays busy
   * until it settles; or control the busy state with `retrying`.
   */
  onRetry: () => unknown;
  /** Controlled busy state. When set, the returned promise is not tracked. */
  retrying?: boolean;
  /** Retry button text. Default "Try again". */
  retryLabel?: string;
  /** Announced politely when a retry starts. Default "Retrying". */
  retryingMessage?: string;
  /**
   * `inline` (default) is a compact row inside a card or section; `banner` is
   * a tinted, bordered strip, for example above stale data that is still shown.
   */
  variant?: 'inline' | 'banner';
  /** `danger` (default) when nothing could load; `warning` when stale data is still shown. */
  tone?: 'danger' | 'warning';
  /** Replaces the warning icon. Pass `null` for none. Decorative. */
  icon?: ReactNode | null;
  /** Extra actions after Try again, such as a link to a status page. */
  actions?: ReactNode;
  /**
   * Where focus goes if this row unmounts while it holds focus, which usually
   * means the retry worked and the content replaced it. Pass a ref or a
   * function returning the element (the loaded region's heading, say).
   */
  returnFocusTo?: InlineErrorRetryFocusTarget;
}

const isPromise = (v: unknown): v is PromiseLike<unknown> =>
  typeof v === 'object' && v !== null && typeof (v as { then?: unknown }).then === 'function';

/**
 * A compact "this part didn't load" row with Try again. It is announced when
 * it appears and again when a retry fails; focus stays on Try again while the
 * retry runs and moves to `returnFocusTo` if the row goes away.
 */
export const InlineErrorRetry = forwardRef<HTMLDivElement, InlineErrorRetryProps>(
  function InlineErrorRetry(
    {
      children,
      onRetry,
      retrying,
      retryLabel = 'Try again',
      retryingMessage = 'Retrying',
      variant = 'inline',
      tone = 'danger',
      icon,
      actions,
      returnFocusTo,
      className,
      ...rest
    },
    forwardedRef,
  ) {
    const [tracked, setTracked] = useState(false);
    // Incremented each time a retry settles while the row is still shown, so
    // the message is re-inserted into the alert and announced again.
    const [attempt, setAttempt] = useState(0);
    const controlled = retrying !== undefined;
    const busy = controlled ? retrying : tracked;

    const root = useRef<HTMLDivElement | null>(null);
    const mounted = useRef(true);
    const focusTarget = useRef(returnFocusTo);
    focusTarget.current = returnFocusTo;

    const setRef = useCallback(
      (node: HTMLDivElement | null) => {
        root.current = node;
        if (typeof forwardedRef === 'function') forwardedRef(node);
        else if (forwardedRef) forwardedRef.current = node;
      },
      [forwardedRef],
    );

    // Controlled: a busy → idle transition while mounted is a failed retry.
    const wasBusy = useRef(busy);
    useEffect(() => {
      if (controlled && wasBusy.current && !busy) setAttempt((n) => n + 1);
      wasBusy.current = busy;
    }, [busy, controlled]);

    useIsoLayoutEffect(() => {
      mounted.current = true;
      const node = root.current;
      return () => {
        mounted.current = false;
        const active = typeof document === 'undefined' ? null : document.activeElement;
        if (!node || !active || !node.contains(active)) return;
        // Our node is still in the DOM here; focus the target once the new
        // content has committed.
        setTimeout(() => {
          const t = focusTarget.current;
          const el = typeof t === 'function' ? t() : t?.current;
          el?.focus();
        }, 0);
      };
    }, []);

    const handleRetry = () => {
      if (busy) return;
      const result = onRetry();
      if (controlled || !isPromise(result)) return;
      setTracked(true);
      const settle = () => {
        if (!mounted.current) return;
        setTracked(false);
        setAttempt((n) => n + 1);
      };
      result.then(settle, settle);
    };

    const glyph = icon === undefined ? <WarningGlyph /> : icon;

    return (
      <div
        ref={setRef}
        {...rest}
        className={cx('mn-inline-error-retry', className)}
        data-variant={variant}
        data-tone={tone}
        data-retrying={busy || undefined}
      >
        {glyph ? (
          <span className="mn-inline-error-retry__icon" aria-hidden>
            {glyph}
          </span>
        ) : null}
        <span role="alert" className="mn-inline-error-retry__message">
          <span key={attempt}>{children}</span>
        </span>
        <span className="mn-inline-error-retry__actions">
          <Button
            size="sm"
            loading={busy}
            leadingIcon={<RetryGlyph />}
            onClick={handleRetry}
            className="mn-inline-error-retry__retry"
          >
            {retryLabel}
          </Button>
          {actions}
        </span>
        <span role="status" className="mn-visually-hidden">
          {busy ? retryingMessage : ''}
        </span>
      </div>
    );
  },
);
