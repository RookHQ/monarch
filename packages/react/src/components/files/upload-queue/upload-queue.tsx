'use client';

import {
  forwardRef,
  useEffect,
  useId,
  useLayoutEffect,
  useRef,
  useState,
  type HTMLAttributes,
  type ReactNode,
} from 'react';
import { cx } from '../../../internal/cx.js';
import { CloseGlyph, ErrorGlyph, RetryGlyph, SuccessGlyph } from '../../../internal/icons.js';
import { Button, IconButton } from '../../actions/button/index.js';
import { Progress } from '../../data-display/progress-bar/index.js';
import { formatBytes } from '../file-upload/index.js';

/** Where an upload is. */
export type UploadStatus = 'queued' | 'uploading' | 'done' | 'failed' | 'cancelled';

/** One upload, as reported by the product. */
export interface UploadQueueItem {
  /** Stable, unique id. */
  id: string;
  /** File name shown in the row and used in labels. */
  name: string;
  /** Size in bytes. Shown, and used to weight the total progress. */
  size?: number;
  status: UploadStatus;
  /** 0–100 while uploading. Omit for an indeterminate bar. */
  progress?: number;
  /** Why it failed, shown under the name. */
  error?: string;
  /** Whether Retry is offered for a failed or cancelled upload. Default true. */
  retryable?: boolean;
  /** Image URL for a thumbnail, such as an object URL of the file. */
  thumbnail?: string;
  /** Icon shown when there is no thumbnail. Decorative. */
  icon?: ReactNode;
  /** Extra status line, such as "Checking for viruses". Replaces the default status text. */
  detail?: ReactNode;
}

/** Labels, statuses and announcements. Override them to translate. */
export interface UploadQueueMessages {
  title: string;
  queued: string;
  uploading: (percent?: number) => string;
  done: string;
  failed: string;
  cancelled: string;
  cancel: (name: string) => string;
  retry: string;
  retryLabel: (name: string) => string;
  remove: (name: string) => string;
  clearFinished: string;
  progressLabel: (name: string) => string;
  totalLabel: string;
  /** The header summary, e.g. "2 of 5 uploaded · 1 failed". */
  summary: (counts: UploadQueueCounts) => string;
  announceDone: (name: string) => string;
  announceFailed: (name: string, error?: string) => string;
  announceFinished: (counts: UploadQueueCounts) => string;
  formatBytes: (bytes: number) => string;
}

/** How many uploads are in each status. */
export interface UploadQueueCounts {
  total: number;
  queued: number;
  uploading: number;
  done: number;
  failed: number;
  cancelled: number;
}

const DEFAULT_MESSAGES: UploadQueueMessages = {
  title: 'Uploads',
  queued: 'Waiting',
  uploading: (p) => (p === undefined ? 'Uploading…' : `Uploading… ${p}%`),
  done: 'Uploaded',
  failed: 'Upload failed',
  cancelled: 'Cancelled',
  cancel: (name) => `Cancel upload of ${name}`,
  retry: 'Retry',
  retryLabel: (name) => `Retry upload of ${name}`,
  remove: (name) => `Remove ${name} from the list`,
  clearFinished: 'Clear finished',
  progressLabel: (name) => `Upload progress of ${name}`,
  totalLabel: 'Total upload progress',
  summary: (c) =>
    [
      `${c.done} of ${c.total - c.cancelled} uploaded`,
      c.failed ? `${c.failed} failed` : '',
      c.cancelled ? `${c.cancelled} cancelled` : '',
    ]
      .filter(Boolean)
      .join(' · '),
  announceDone: (name) => `${name} uploaded.`,
  announceFailed: (name, error) => `${name} failed${error ? `: ${error}` : ''}.`,
  announceFinished: (c) =>
    c.failed
      ? `Uploads finished: ${c.done} uploaded, ${c.failed} failed.`
      : `All uploads finished: ${c.done} uploaded.`,
  formatBytes,
};

const ACTIVE: ReadonlySet<UploadStatus> = new Set(['queued', 'uploading']);

/** Counts uploads by status. */
export function countUploads(items: readonly UploadQueueItem[]): UploadQueueCounts {
  const c: UploadQueueCounts = {
    total: items.length,
    queued: 0,
    uploading: 0,
    done: 0,
    failed: 0,
    cancelled: 0,
  };
  for (const i of items) c[i.status]++;
  return c;
}

/**
 * Overall progress, 0–100, of the uploads that can still finish (cancelled
 * and failed ones are left out). Weighted by size when every upload has one.
 */
export function totalUploadProgress(items: readonly UploadQueueItem[]): number {
  const live = items.filter((i) => i.status !== 'cancelled' && i.status !== 'failed');
  if (!live.length) return 0;
  const bySize = live.every((i) => (i.size ?? 0) > 0);
  let sum = 0;
  let weight = 0;
  for (const i of live) {
    const w = bySize ? (i.size as number) : 1;
    const p = i.status === 'done' ? 100 : i.status === 'uploading' ? (i.progress ?? 0) : 0;
    sum += w * Math.max(0, Math.min(100, p));
    weight += w;
  }
  return Math.round(sum / weight);
}

export interface UploadQueueProps extends Omit<HTMLAttributes<HTMLElement>, 'children'> {
  /** The uploads, in display order. The product updates status and progress. */
  items: readonly UploadQueueItem[];
  /** Cancel a queued or uploading file. The button shows only when this is set. */
  onCancel?: (id: string) => void;
  /** Retry a failed or cancelled file. */
  onRetry?: (id: string) => void;
  /** Remove a finished, failed or cancelled file from the list. */
  onRemove?: (id: string) => void;
  /** Remove every finished, failed and cancelled file. Shows a header button. */
  onClearFinished?: () => void;
  /** Show the header with the title, summary and total progress. Default true. */
  showSummary?: boolean;
  /** Heading level of the title. Default 3. */
  headingLevel?: 2 | 3 | 4 | 5 | 6;
  /** Override labels and announcements. */
  messages?: Partial<UploadQueueMessages>;
}

/**
 * The list of files being uploaded: per-file progress and status, cancel,
 * retry and remove, and a total summary. Completion and failures are
 * announced. The product does the transfer and reports progress back.
 */
export const UploadQueue = forwardRef<HTMLElement, UploadQueueProps>(function UploadQueue(
  {
    items,
    onCancel,
    onRetry,
    onRemove,
    onClearFinished,
    showSummary = true,
    headingLevel = 3,
    messages: messagesProp,
    className,
    ...rest
  },
  ref,
) {
  const m = { ...DEFAULT_MESSAGES, ...messagesProp };
  const Heading = `h${headingLevel}` as 'h3';
  const id = useId();
  const titleId = `${id}-title`;
  const counts = countUploads(items);
  const active = counts.queued + counts.uploading;
  const finished = counts.done + counts.failed + counts.cancelled;
  const total = totalUploadProgress(items);

  // Announce transitions to done and failed, and the end of the batch.
  const [announcement, setAnnouncement] = useState('');
  const previous = useRef<Map<string, UploadStatus> | null>(null);
  useEffect(() => {
    const before = previous.current;
    previous.current = new Map(items.map((i) => [i.id, i.status]));
    if (!before) return;
    const parts: string[] = [];
    let changed = false;
    for (const i of items) {
      const was = before.get(i.id);
      if (was === i.status) continue;
      if (i.status === 'done') parts.push(m.announceDone(i.name));
      if (i.status === 'failed') parts.push(m.announceFailed(i.name, i.error));
      if (was !== undefined && ACTIVE.has(was) && !ACTIVE.has(i.status)) changed = true;
    }
    if (changed && active === 0 && items.length > 1) {
      parts.push(m.announceFinished(counts));
    }
    if (parts.length) setAnnouncement(parts.join(' '));
    // Messages are read fresh each time; items drive the effect.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [items]);

  // When a row is removed with focus inside it, focus moves to the next row.
  const listRef = useRef<HTMLUListElement | null>(null);
  const headingRef = useRef<HTMLHeadingElement | null>(null);
  const removedAt = useRef<number | null>(null);
  useLayoutEffect(() => {
    const at = removedAt.current;
    if (at === null) return;
    removedAt.current = null;
    const rows = listRef.current?.querySelectorAll<HTMLLIElement>(':scope > li') ?? [];
    const row = rows[Math.min(at, rows.length - 1)];
    const target = row?.querySelector<HTMLButtonElement>('button:not(:disabled)');
    if (target) target.focus();
    else headingRef.current?.focus();
  }, [items]);

  const remove = (itemId: string, index: number) => {
    removedAt.current = index;
    onRemove?.(itemId);
  };

  return (
    <section
      ref={ref}
      aria-labelledby={showSummary ? titleId : undefined}
      aria-label={showSummary ? undefined : m.title}
      {...rest}
      className={cx('mn-upload-queue', className)}
      data-active={active > 0 || undefined}
    >
      {showSummary ? (
        <header className="mn-upload-queue__header">
          <div className="mn-upload-queue__heading">
            <Heading ref={headingRef} id={titleId} tabIndex={-1} className="mn-upload-queue__title">
              {m.title}
            </Heading>
            {items.length ? <p className="mn-upload-queue__summary">{m.summary(counts)}</p> : null}
          </div>
          {onClearFinished && finished > 0 ? (
            <Button size="xs" variant="ghost" onClick={onClearFinished}>
              {m.clearFinished}
            </Button>
          ) : null}
          {active > 0 ? (
            <Progress
              className="mn-upload-queue__total"
              size="sm"
              value={total}
              label={m.totalLabel}
              valueText={`${total}%`}
            />
          ) : null}
        </header>
      ) : null}
      <ul ref={listRef} className="mn-upload-queue__list">
        {items.map((item, index) => {
          const isActive = ACTIVE.has(item.status);
          const canRetry =
            (item.status === 'failed' || item.status === 'cancelled') && item.retryable !== false;
          const percent =
            item.progress === undefined
              ? undefined
              : Math.round(Math.max(0, Math.min(100, item.progress)));
          const statusText =
            item.status === 'queued'
              ? m.queued
              : item.status === 'uploading'
                ? m.uploading(percent)
                : item.status === 'done'
                  ? m.done
                  : item.status === 'failed'
                    ? (item.error ?? m.failed)
                    : m.cancelled;
          return (
            <li key={item.id} className="mn-upload-queue__item" data-status={item.status}>
              <span className="mn-upload-queue__media" aria-hidden>
                {item.thumbnail ? (
                  <img className="mn-upload-queue__thumb" src={item.thumbnail} alt="" />
                ) : (
                  (item.icon ?? <FileGlyph />)
                )}
              </span>
              <div className="mn-upload-queue__body">
                <p className="mn-upload-queue__name">
                  <span className="mn-upload-queue__filename">{item.name}</span>
                  {item.size !== undefined ? (
                    <span className="mn-upload-queue__size">{m.formatBytes(item.size)}</span>
                  ) : null}
                </p>
                <p className="mn-upload-queue__status" data-status={item.status}>
                  {item.status === 'done' ? <SuccessGlyph /> : null}
                  {item.status === 'failed' ? <ErrorGlyph /> : null}
                  <span>{item.detail ?? statusText}</span>
                </p>
                {item.status === 'uploading' ? (
                  <Progress
                    className="mn-upload-queue__progress"
                    size="sm"
                    value={percent ?? null}
                    label={m.progressLabel(item.name)}
                  />
                ) : null}
              </div>
              <div className="mn-upload-queue__actions">
                {canRetry && onRetry ? (
                  <Button
                    size="xs"
                    variant="ghost"
                    leadingIcon={<RetryGlyph />}
                    aria-label={m.retryLabel(item.name)}
                    onClick={() => onRetry(item.id)}
                  >
                    {m.retry}
                  </Button>
                ) : null}
                {isActive && onCancel ? (
                  <IconButton
                    size="xs"
                    icon={<CloseGlyph />}
                    label={m.cancel(item.name)}
                    onClick={() => onCancel(item.id)}
                  />
                ) : null}
                {!isActive && onRemove ? (
                  <IconButton
                    size="xs"
                    icon={<CloseGlyph />}
                    label={m.remove(item.name)}
                    onClick={() => remove(item.id, index)}
                  />
                ) : null}
              </div>
            </li>
          );
        })}
      </ul>
      <span className="mn-visually-hidden" aria-live="polite" aria-atomic="true">
        {announcement}
      </span>
    </section>
  );
});

function FileGlyph() {
  return (
    <svg
      width="16"
      height="16"
      viewBox="0 0 16 16"
      fill="none"
      stroke="currentColor"
      strokeWidth="1.5"
      strokeLinejoin="round"
      aria-hidden
      focusable={false}
    >
      <path d="M9 1.75H4.5a1.25 1.25 0 0 0-1.25 1.25v10a1.25 1.25 0 0 0 1.25 1.25h7a1.25 1.25 0 0 0 1.25-1.25V5.5L9 1.75Z" />
      <path d="M9 1.75V5.5h3.75" />
    </svg>
  );
}
