'use client';

import { forwardRef, useId, type HTMLAttributes, type ReactNode } from 'react';
import { cx } from '../../../internal/cx.js';
import { CloseGlyph, RetryGlyph } from '../../../internal/icons.js';
import { isSafeUrl } from '../../../internal/sanitize.js';
import { formatBytes } from '../file-upload/index.js';
import { FileTypeIcon } from '../file-type-icon/index.js';
import type { UploadStatus } from '../upload-queue/index.js';

/** Every string an attachment chip shows or announces. */
export interface AttachmentChipMessages {
  /** Name of the remove button. */
  remove: (name: string) => string;
  /** Name of the retry button. */
  retry: (name: string) => string;
  /** Accessible name of the progress bar. */
  uploading: (name: string) => string;
  /** Shown while waiting to start. */
  queued: string;
  /** Shown when the upload failed and no `error` text is given. */
  failed: string;
  /** Shown when the upload was cancelled. */
  cancelled: string;
  /** Visible percentage while uploading. */
  percent: (value: number) => string;
}

export const ATTACHMENT_CHIP_MESSAGES: AttachmentChipMessages = {
  remove: (name) => `Remove ${name}`,
  retry: (name) => `Retry ${name}`,
  uploading: (name) => `Uploading ${name}`,
  queued: 'Waiting',
  failed: 'Upload failed',
  cancelled: 'Cancelled',
  percent: (value) => `${Math.round(value)}%`,
};

export interface AttachmentChipProps extends Omit<HTMLAttributes<HTMLDivElement>, 'children'> {
  /** File name, shown truncated in the middle-safe way (full name in a tooltip). */
  name: string;
  /** Size in bytes, shown after the name when the file is not uploading or failed. */
  size?: number | undefined;
  /** MIME type, used with the name to pick the type icon. */
  contentType?: string | undefined;
  /** Upload state. Default `done`. Same values as UploadQueue. */
  status?: UploadStatus | undefined;
  /** Upload progress 0–100 while `status` is `uploading`. Omit for an indeterminate upload. */
  progress?: number | undefined;
  /** Why the upload failed, shown instead of the size when `status` is `failed`. */
  error?: string | undefined;
  /**
   * Where the file can be opened or downloaded once uploaded. The name becomes
   * a link; only http(s), mailto, blob and relative URLs are linked.
   */
  href?: string | undefined;
  /** Download the link target instead of navigating (adds the `download` attribute). */
  download?: boolean | undefined;
  /** A thumbnail or other visual to show instead of the type icon. Decorative. */
  icon?: ReactNode;
  /** Removes the attachment. Without it there is no remove button. */
  onRemove?: (() => void) | undefined;
  /** Retries a failed upload. Shown only when `status` is `failed`. */
  onRetry?: (() => void) | undefined;
  /** Disables the buttons (for example while the message is sending). */
  disabled?: boolean | undefined;
  /** Translated strings. */
  messages?: Partial<AttachmentChipMessages> | undefined;
}

/**
 * A compact chip for one attached file: type icon, name, size, upload
 * progress or failure, and a remove button. Used in composers, forms and
 * ticket replies, alone or in a list.
 */
export const AttachmentChip = forwardRef<HTMLDivElement, AttachmentChipProps>(
  function AttachmentChip(
    {
      name,
      size,
      contentType,
      status = 'done',
      progress,
      error,
      href,
      download = false,
      icon,
      onRemove,
      onRetry,
      disabled = false,
      messages: messagesProp,
      className,
      ...rest
    },
    ref,
  ) {
    const messages = { ...ATTACHMENT_CHIP_MESSAGES, ...messagesProp };
    const metaId = useId();
    const pct =
      progress === undefined ? undefined : Math.min(100, Math.max(0, Math.round(progress)));

    let meta: string | undefined;
    if (status === 'uploading') meta = pct === undefined ? undefined : messages.percent(pct);
    else if (status === 'queued') meta = messages.queued;
    else if (status === 'failed') meta = error ?? messages.failed;
    else if (status === 'cancelled') meta = messages.cancelled;
    else if (size !== undefined) meta = formatBytes(size);

    const safeHref =
      status === 'done' && href && isSafeUrl(href, ['http', 'https', 'mailto', 'blob'])
        ? href
        : undefined;

    return (
      <div
        ref={ref}
        {...rest}
        className={cx('mn-attachment-chip', className)}
        data-status={status}
        data-disabled={disabled || undefined}
      >
        <span className="mn-attachment-chip__icon" aria-hidden>
          {icon ?? <FileTypeIcon name={name} contentType={contentType} size="sm" decorative />}
        </span>
        <span className="mn-attachment-chip__body">
          {safeHref ? (
            <a
              className="mn-attachment-chip__name"
              href={safeHref}
              title={name}
              aria-describedby={meta ? metaId : undefined}
              {...(download
                ? { download: name }
                : { target: '_blank', rel: 'noopener noreferrer' })}
            >
              {name}
            </a>
          ) : (
            <span className="mn-attachment-chip__name" title={name}>
              {name}
            </span>
          )}
          {meta ? (
            <span
              id={metaId}
              className="mn-attachment-chip__meta"
              role={status === 'failed' ? 'alert' : undefined}
            >
              {meta}
            </span>
          ) : null}
        </span>
        {status === 'uploading' ? (
          <span
            className="mn-attachment-chip__progress"
            role="progressbar"
            aria-label={messages.uploading(name)}
            aria-valuemin={0}
            aria-valuemax={100}
            aria-valuenow={pct}
            data-indeterminate={pct === undefined || undefined}
          >
            <span
              className="mn-attachment-chip__bar"
              style={pct === undefined ? undefined : { inlineSize: `${pct}%` }}
            />
          </span>
        ) : null}
        {status === 'failed' && onRetry ? (
          <button
            type="button"
            className="mn-attachment-chip__action"
            aria-label={messages.retry(name)}
            disabled={disabled}
            onClick={onRetry}
          >
            <RetryGlyph />
          </button>
        ) : null}
        {onRemove ? (
          <button
            type="button"
            className="mn-attachment-chip__action"
            aria-label={messages.remove(name)}
            disabled={disabled}
            onClick={onRemove}
          >
            <CloseGlyph />
          </button>
        ) : null}
      </div>
    );
  },
);
