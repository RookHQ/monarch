'use client';

import { forwardRef, useMemo, useRef, type ElementRef, type ReactNode } from 'react';
import { cx } from '../../../internal/cx.js';
import { focusReturnTarget } from '../../../internal/dialog-helpers.js';
import { DescriptionItem, DescriptionList } from '../../data-display/description-list/index.js';
import { RelativeTime, type DateInput } from '../../data-display/relative-time/index.js';
import { StatusBadge, type StatusTone } from '../../data-display/status-badge/index.js';
import { InlineErrorRetry } from '../../feedback/inline-error-retry/index.js';
import { Skeleton } from '../../feedback/skeleton/index.js';
import {
  Sheet,
  SheetBody,
  SheetContent,
  SheetDescription,
  SheetHeader,
  SheetTitle,
  type SheetContentProps,
} from '../../overlays/sheet/index.js';

/** The request a log entry describes. Every part is optional. */
export interface LogDrawerRequest {
  /** HTTP method or operation verb, e.g. `POST`. */
  method?: string | undefined;
  /** Path, route or operation name, e.g. `/v1/orders/{id}`. */
  path?: string | undefined;
  /** HTTP status code of the response. */
  status?: number | null | undefined;
  /** Short error code returned with the status, e.g. `rate_limited`. */
  errorCode?: string | null | undefined;
  /** Request ID, shown with a copy button. */
  id?: string | undefined;
  /** When the request happened. */
  at?: DateInput | undefined;
  /** How long it took, in milliseconds. */
  durationMs?: number | null | undefined;
}

/** One extra fact about the entry. */
export interface LogDrawerField {
  /** Stable key. Default the label when it is text. */
  id?: string | undefined;
  /** The term ("Credential", "Origin"). */
  label: ReactNode;
  /** The value. Empty values show a dash read as "Not set". */
  value: ReactNode;
  /** Adds a copy button that copies this text. */
  copyValue?: string | undefined;
  /** Monospace value (ids, hosts, codes). */
  mono?: boolean | undefined;
}

/** Every visible string, with English defaults. */
export interface LogDrawerMessages {
  title: string;
  close: string;
  requestId: string;
  copyRequestId: string;
  status: string;
  time: string;
  duration: string;
  operation: string;
  formatDuration: (ms: number) => string;
  loading: string;
  loadError: string;
}

const DEFAULT_MESSAGES: LogDrawerMessages = {
  title: 'Request details',
  close: 'Close',
  requestId: 'Request ID',
  copyRequestId: 'Copy request ID',
  status: 'Status',
  time: 'Time',
  duration: 'Duration',
  operation: 'Operation',
  formatDuration: (ms) => `${Math.round(ms).toLocaleString()} ms`,
  loading: 'Loading details',
  loadError: 'The details did not load.',
};

/** The tone for an HTTP status: 2xx success, 3xx info, 4xx warning, 5xx danger. */
export function httpStatusTone(status: number): StatusTone {
  if (status >= 500) return 'danger';
  if (status >= 400) return 'warning';
  if (status >= 300) return 'info';
  if (status >= 200) return 'success';
  return 'neutral';
}

export interface LogDrawerProps extends Omit<
  SheetContentProps,
  'title' | 'children' | 'closeLabel'
> {
  /** Whether the drawer is open. Usually `selected !== null`. */
  open: boolean;
  /** Called when the drawer asks to close (Escape, outside click, close button). */
  onOpenChange?: ((open: boolean) => void) | undefined;
  /** Replaces `messages.title`; the drawer's accessible name. */
  title?: ReactNode;
  /** Text under the title, e.g. what is and is not recorded. */
  description?: ReactNode;
  /** The request summary: method, path, status, ID, time and duration. */
  request?: LogDrawerRequest | undefined;
  /** More facts, listed after the request summary. */
  fields?: readonly LogDrawerField[] | undefined;
  /** Anything else, after the fields: a payload viewer, headers, a code block. */
  children?: ReactNode;
  /** The entry is loading: shows placeholders. */
  loading?: boolean;
  /** The entry failed to load: shows this message (or `messages.loadError` for `true`) with Try again. */
  error?: ReactNode;
  /** Called by Try again after a load error. */
  onRetry?: (() => unknown) | undefined;
  /** Locale for numbers and times. Default the browser's. */
  locale?: string | undefined;
  /** "Now" for relative times (tests and server rendering). */
  now?: DateInput | undefined;
  /** Replaces any of the default English strings. */
  messages?: Partial<LogDrawerMessages> | undefined;
}

/**
 * A side drawer with the full record of one log entry (an API request, a
 * webhook delivery, a job run): method, path, status, request ID with copy,
 * time and duration, then any facts and content the product adds. Opens from
 * the right and fills the screen on phones.
 */
export const LogDrawer = forwardRef<ElementRef<typeof SheetContent>, LogDrawerProps>(
  function LogDrawer(
    {
      open,
      onOpenChange,
      title,
      description,
      request,
      fields,
      children,
      loading = false,
      error,
      onRetry,
      locale,
      now,
      messages: messagesProp,
      side = 'right',
      size = 'md',
      onOpenAutoFocus,
      onCloseAutoFocus,
      className,
      ...rest
    },
    ref,
  ) {
    const messages = useMemo(() => ({ ...DEFAULT_MESSAGES, ...messagesProp }), [messagesProp]);
    const returnFocus = useRef<HTMLElement | null>(null);
    const status = request?.status ?? null;
    const operation = request?.method || request?.path;

    let content: ReactNode;
    if (loading) {
      content = (
        <div className="mn-log-drawer__loading" aria-busy="true">
          <span className="mn-visually-hidden" role="status">
            {messages.loading}
          </span>
          {[0, 1, 2, 3, 4].map((i) => (
            <Skeleton key={i} shape="text" height="2.25rem" />
          ))}
        </div>
      );
    } else if (error) {
      content = (
        <InlineErrorRetry onRetry={onRetry ?? (() => undefined)}>
          {error === true ? messages.loadError : error}
        </InlineErrorRetry>
      );
    } else {
      content = (
        <>
          <DescriptionList columns={1} className="mn-log-drawer__facts">
            {operation ? (
              <DescriptionItem term={messages.operation}>
                <span className="mn-log-drawer__operation">
                  {request?.method ? (
                    <span
                      className="mn-log-drawer__method"
                      data-method={request.method.toUpperCase()}
                    >
                      {request.method.toUpperCase()}
                    </span>
                  ) : null}
                  {request?.path ? (
                    <code className="mn-log-drawer__mono">{request.path}</code>
                  ) : null}
                </span>
              </DescriptionItem>
            ) : null}
            {status !== null ? (
              <DescriptionItem term={messages.status}>
                <span className="mn-log-drawer__status">
                  <StatusBadge
                    status={String(status)}
                    statuses={{
                      [String(status)]: { label: String(status), tone: httpStatusTone(status) },
                    }}
                    size="sm"
                  />
                  {request?.errorCode ? (
                    <code className="mn-log-drawer__mono mn-log-drawer__muted">
                      {request.errorCode}
                    </code>
                  ) : null}
                </span>
              </DescriptionItem>
            ) : null}
            {request?.id ? (
              <DescriptionItem
                term={messages.requestId}
                copyable={request.id}
                copyLabel={messages.copyRequestId}
              >
                <code className="mn-log-drawer__mono">{request.id}</code>
              </DescriptionItem>
            ) : null}
            {request?.at !== undefined ? (
              <DescriptionItem term={messages.time}>
                <RelativeTime
                  date={request.at}
                  tooltip
                  {...(locale ? { locale } : {})}
                  {...(now !== undefined ? { now } : {})}
                />
              </DescriptionItem>
            ) : null}
            {request?.durationMs != null ? (
              <DescriptionItem term={messages.duration}>
                <span className="mn-log-drawer__number">
                  {messages.formatDuration(request.durationMs)}
                </span>
              </DescriptionItem>
            ) : null}
            {fields?.map((field, index) => (
              <DescriptionItem
                key={field.id ?? (typeof field.label === 'string' ? field.label : index)}
                term={field.label}
                {...(field.copyValue ? { copyable: field.copyValue } : {})}
              >
                {field.mono &&
                field.value !== null &&
                field.value !== undefined &&
                field.value !== '' ? (
                  <code className="mn-log-drawer__mono">{field.value}</code>
                ) : (
                  field.value
                )}
              </DescriptionItem>
            ))}
          </DescriptionList>
          {children ? <div className="mn-log-drawer__extra">{children}</div> : null}
        </>
      );
    }

    return (
      <Sheet open={open} {...(onOpenChange ? { onOpenChange } : {})}>
        <SheetContent
          ref={ref}
          {...(description ? {} : { 'aria-describedby': undefined })}
          {...rest}
          side={side}
          size={size}
          closeLabel={messages.close}
          onOpenAutoFocus={(event) => {
            // Usually opened from a table row, not a SheetTrigger: remember what had focus.
            returnFocus.current = focusReturnTarget();
            onOpenAutoFocus?.(event);
          }}
          onCloseAutoFocus={(event) => {
            onCloseAutoFocus?.(event);
            const target = returnFocus.current;
            if (event.defaultPrevented || !target?.isConnected) return;
            event.preventDefault();
            target.focus();
          }}
          data-status-tone={status !== null ? httpStatusTone(status) : undefined}
          className={cx('mn-log-drawer', className)}
        >
          <SheetHeader>
            <SheetTitle>{title ?? messages.title}</SheetTitle>
            {description ? <SheetDescription>{description}</SheetDescription> : null}
          </SheetHeader>
          <SheetBody className="mn-log-drawer__body">{content}</SheetBody>
        </SheetContent>
      </Sheet>
    );
  },
);
