'use client';

import { forwardRef, useEffect, useId, useRef, type HTMLAttributes, type ReactNode } from 'react';
import { cx } from '../../../internal/cx.js';
import { Button } from '../../actions/button/index.js';
import { CopyButton } from '../../actions/copy-button/index.js';
import { errorKindIcon } from '../error-state/index.js';

/** The page-level failures ErrorPage covers. */
export type ErrorPageKind = 'not-found' | 'forbidden' | 'server' | 'offline' | 'maintenance';

const NUMERAL: Partial<Record<ErrorPageKind, string>> = {
  'not-found': '404',
  forbidden: '403',
  server: '500',
};

export interface ErrorPageProps extends Omit<HTMLAttributes<HTMLElement>, 'title'> {
  /** Which failure this page reports. Sets the default icon and numeral. Default `server`. */
  kind?: ErrorPageKind;
  /** What happened, in plain words. Supplied by the product. */
  title: ReactNode;
  /** What the person can do next. Supplied by the product; never a raw error message. */
  description?: ReactNode;
  /**
   * The large faint numeral behind the icon. Defaults to 404, 403 or 500 for
   * those kinds and none for `offline` and `maintenance`. Pass `false` to hide
   * it. Decorative: drawn by CSS and hidden from assistive technology, so say
   * the same thing in the title.
   */
  numeral?: string | false;
  /** Replaces the kind's default icon. Pass `null` for none. Decorative. */
  icon?: ReactNode | null;
  /** The product's links and buttons, e.g. "Go to dashboard" (Button `asChild` around a router link). */
  actions?: ReactNode;
  /** Renders a primary retry button before `actions`. Use for `server`, `offline` and `maintenance`. */
  onRetry?: () => void;
  /** Retry button text. Default "Try again". */
  retryLabel?: string;
  /** Shows the retry button as busy while a retry is in flight. */
  retrying?: boolean;
  /**
   * With `kind="offline"` and `onRetry`, calls `onRetry` once when the browser
   * reports it is back online. Default true.
   */
  retryWhenOnline?: boolean;
  /** How to reach help, supplied by the product (a sentence with a link or email). */
  support?: ReactNode;
  /** The request id of the failed request, shown with a copy button. */
  requestId?: string;
  /** Label before the request id. Default "Request ID". */
  requestIdLabel?: string;
  /** The product's logo or wordmark, shown at the top when there is no app shell. */
  brand?: ReactNode;
  /**
   * `true` (default) fills the viewport with its own background, for a root
   * not-found or error route that renders without the app shell. `false` sits
   * inside the shell's content area.
   */
  fullScreen?: boolean;
  /**
   * The root element. Default `main` when `fullScreen` (the page has no other
   * main landmark) and `section` otherwise (the shell already has one).
   */
  as?: 'main' | 'section' | 'div';
  /** Title element. Default `h1`. */
  headingLevel?: 'h1' | 'h2';
  /**
   * Moves focus to the title on mount, so a screen reader starts reading the
   * error after a client-side navigation. Default false.
   */
  focusOnMount?: boolean;
}

/**
 * A whole page that failed: not found, forbidden, server error, offline or
 * maintenance. Works with or without the app shell. The product supplies
 * every word, link and support contact.
 */
export const ErrorPage = forwardRef<HTMLElement, ErrorPageProps>(function ErrorPage(
  {
    kind = 'server',
    title,
    description,
    numeral,
    icon,
    actions,
    onRetry,
    retryLabel = 'Try again',
    retrying = false,
    retryWhenOnline = true,
    support,
    requestId,
    requestIdLabel = 'Request ID',
    brand,
    fullScreen = true,
    as,
    headingLevel: Heading = 'h1',
    focusOnMount = false,
    className,
    children,
    'aria-labelledby': labelledBy,
    ...rest
  },
  ref,
) {
  const Root = as ?? (fullScreen ? 'main' : 'section');
  const titleId = `${useId()}-title`;
  const titleRef = useRef<HTMLHeadingElement>(null);
  const retryRef = useRef(onRetry);
  retryRef.current = onRetry;

  useEffect(() => {
    if (focusOnMount) titleRef.current?.focus();
  }, [focusOnMount]);

  const watchOnline = kind === 'offline' && retryWhenOnline && Boolean(onRetry);
  useEffect(() => {
    if (!watchOnline) return;
    const back = () => retryRef.current?.();
    window.addEventListener('online', back, { once: true });
    return () => window.removeEventListener('online', back);
  }, [watchOnline]);

  const shown = numeral === false ? null : (numeral ?? NUMERAL[kind] ?? null);
  const glyph = icon === undefined ? errorKindIcon(kind) : icon;

  return (
    <Root
      ref={ref as never}
      {...rest}
      aria-labelledby={Root === 'div' ? labelledBy : (labelledBy ?? titleId)}
      className={cx('mn-error-page', className)}
      data-kind={kind}
      data-full-screen={fullScreen || undefined}
    >
      {brand ? <div className="mn-error-page__brand">{brand}</div> : null}
      <div className="mn-error-page__body">
        {shown ? (
          // Drawn by CSS from data-numeral: it is decoration, not text, so it
          // never reaches assistive technology or contrast checks.
          <div className="mn-error-page__numeral" aria-hidden data-numeral={shown} />
        ) : null}
        <div className="mn-error-page__content" data-numeral={shown ? '' : undefined}>
          {glyph ? (
            <div className="mn-error-page__icon" aria-hidden>
              {glyph}
            </div>
          ) : null}
          <Heading id={titleId} ref={titleRef} tabIndex={-1} className="mn-error-page__title">
            {title}
          </Heading>
          {description ? <p className="mn-error-page__description">{description}</p> : null}
          {onRetry || actions ? (
            <div className="mn-error-page__actions">
              {onRetry ? (
                <Button variant="primary" loading={retrying} onClick={onRetry}>
                  {retryLabel}
                </Button>
              ) : null}
              {actions}
            </div>
          ) : null}
          {children}
          {requestId ? (
            <p className="mn-error-page__request-id">
              <span>{requestIdLabel}:</span> <code>{requestId}</code>
              <CopyButton value={requestId} iconOnly size="xs" label={`Copy ${requestIdLabel}`} />
            </p>
          ) : null}
          {support ? <div className="mn-error-page__support">{support}</div> : null}
        </div>
      </div>
    </Root>
  );
});
