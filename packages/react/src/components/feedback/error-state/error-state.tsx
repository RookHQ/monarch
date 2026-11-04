import { forwardRef, type ReactNode } from 'react';
import { cx } from '../../../internal/cx.js';
import { Button } from '../../actions/button/index.js';
import { CopyButton } from '../../actions/copy-button/index.js';
import { EmptyState, type EmptyStateProps } from '../empty-state/index.js';
import {
  ErrorGlyph,
  LockGlyph,
  OfflineGlyph,
  SearchGlyph,
  WarningGlyph,
  WrenchGlyph,
} from '../../../internal/icons.js';

/**
 * What went wrong, in terms the interface can act on. The product maps its
 * own errors to a kind (see `errorKindFromStatus`) and still writes the copy.
 *
 * - `error`: something failed; retrying may help (the default).
 * - `forbidden`: the person may not see this; retrying will not help.
 * - `not-found`: the thing does not exist (or no longer does).
 * - `offline`: the request never reached the server.
 * - `server`: the server failed; retrying later may help.
 * - `maintenance`: the service is deliberately unavailable for a while.
 */
export type ErrorKind = 'error' | 'forbidden' | 'not-found' | 'offline' | 'server' | 'maintenance';

/** Kinds where a retry cannot change the outcome, so no retry button is shown. */
const NO_RETRY: ReadonlySet<ErrorKind> = new Set(['forbidden', 'not-found']);

export interface ErrorKindOptions {
  /** True when the browser reports no network (`navigator.onLine === false`). */
  offline?: boolean;
}

/**
 * Maps an HTTP status to an `ErrorKind`. Pure and framework-free: pass the
 * status from your own API client. `0`, `undefined` with `offline`, or
 * `offline: true` give `offline`; 401 and 403 give `forbidden`; 404 and 410
 * give `not-found`; 5xx give `server`; everything else gives `error`.
 * `maintenance` is never inferred: the product decides when it applies.
 */
export function errorKindFromStatus(
  status?: number | null,
  options: ErrorKindOptions = {},
): ErrorKind {
  if (options.offline || status === 0) return 'offline';
  if (status == null) return 'error';
  if (status === 401 || status === 403) return 'forbidden';
  if (status === 404 || status === 410) return 'not-found';
  if (status >= 500 && status <= 599) return 'server';
  return 'error';
}

const KIND_ICON: Record<ErrorKind, () => ReactNode> = {
  error: () => <WarningGlyph />,
  forbidden: () => <LockGlyph />,
  'not-found': () => <SearchGlyph />,
  offline: () => <OfflineGlyph />,
  server: () => <ErrorGlyph />,
  maintenance: () => <WrenchGlyph />,
};

/** The default decorative icon for an error kind (used by ErrorState and ErrorPage). */
export function errorKindIcon(kind: ErrorKind): ReactNode {
  return KIND_ICON[kind]();
}

export interface ErrorStateProps extends Omit<EmptyStateProps, 'actions' | 'icon' | 'kind'> {
  /** Retry handler; renders a "Try again" button (not for `forbidden` or `not-found`). */
  onRetry?: () => void;
  retryLabel?: string;
  /** Shows the retry button as busy while a retry is in flight. */
  retrying?: boolean;
  /** Extra actions next to retry. */
  actions?: ReactNode;
  /** Replaces the kind's default icon. */
  icon?: ReactNode;
  /** What went wrong. Sets the default icon and whether retry is offered. Exposed as `data-kind`. */
  kind?: ErrorKind;
  /**
   * The request id from the failed response, shown in monospace with a copy
   * button so the person can quote it to support.
   */
  requestId?: string;
  /** Label before the request id. Default "Request ID". */
  requestIdLabel?: string;
}

/** Something failed to load. Say what failed and offer a retry. */
export const ErrorState = forwardRef<HTMLDivElement, ErrorStateProps>(function ErrorState(
  {
    title,
    description,
    onRetry,
    retryLabel = 'Try again',
    retrying = false,
    actions,
    icon,
    kind = 'error',
    requestId,
    requestIdLabel = 'Request ID',
    className,
    children,
    ...rest
  },
  ref,
) {
  const canRetry = onRetry && !NO_RETRY.has(kind);
  return (
    <EmptyState
      ref={ref}
      {...rest}
      role="alert"
      className={cx('mn-error-state', className)}
      data-kind={kind === 'error' ? undefined : kind}
      icon={icon ?? errorKindIcon(kind)}
      title={title}
      description={description}
      actions={
        canRetry || actions ? (
          <>
            {canRetry ? (
              <Button variant="primary" loading={retrying} onClick={onRetry}>
                {retryLabel}
              </Button>
            ) : null}
            {actions}
          </>
        ) : undefined
      }
    >
      {children}
      {requestId ? (
        <p className="mn-error-state__request-id">
          <span>{requestIdLabel}:</span> <code>{requestId}</code>
          <CopyButton value={requestId} iconOnly size="xs" label={`Copy ${requestIdLabel}`} />
        </p>
      ) : null}
    </EmptyState>
  );
});
