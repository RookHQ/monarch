'use client';

import {
  forwardRef,
  useEffect,
  useId,
  useImperativeHandle,
  useRef,
  useState,
  type HTMLAttributes,
  type RefObject,
} from 'react';
import { cx } from '../../../internal/cx.js';
import { CloseGlyph, ErrorGlyph, RetryGlyph } from '../../../internal/icons.js';
import { isSafeImageSrc } from '../../../internal/sanitize.js';
import { Button, IconButton } from '../../actions/button/index.js';
import { Progress } from '../../data-display/progress-bar/index.js';
import { formatBytes } from '../../files/file-upload/index.js';
import type { AiPromptAttachment } from '../ai-prompt-input/index.js';

/** One attached file in the tray. */
export interface AiAttachmentTrayFile {
  /** Stable id, passed to `onRemove` and `onRetry`. */
  id: string;
  name: string;
  /** Size in bytes. */
  size: number;
  /** MIME type, e.g. `image/png` or `application/pdf`. */
  type: string;
  /** Upload progress from 0 to 1. Omit (or 1) when the file is ready. */
  progress?: number | undefined;
  /** Why the upload failed. `true` shows the default message. Shows Retry when `onRetry` is set. */
  error?: string | boolean | undefined;
  /** Thumbnail URL (http, https, a relative path or `blob:`). Other schemes are ignored. */
  previewUrl?: string | undefined;
  /** The file itself. Images get a thumbnail from it when `previewUrl` is not set. */
  file?: File | undefined;
}

/** Where a file is: `uploading` (has progress below 1), `error` or `ready`. */
export type AiAttachmentStatus = 'uploading' | 'error' | 'ready';

/** A file's status from its `progress` and `error`. */
export function aiAttachmentStatus(file: AiAttachmentTrayFile): AiAttachmentStatus {
  if (file.error) return 'error';
  if (typeof file.progress === 'number' && file.progress < 1) return 'uploading';
  return 'ready';
}

/**
 * Maps AiPromptInput's attachments to tray files, for its `renderAttachments`:
 * `renderAttachments={(a, remove) => <AiAttachmentTray files={aiAttachmentTrayFiles(a)} onRemove={remove} />}`.
 */
export function aiAttachmentTrayFiles(
  attachments: readonly AiPromptAttachment[],
): AiAttachmentTrayFile[] {
  return attachments.map(({ id, file }) => ({
    id,
    name: file.name,
    size: file.size,
    type: file.type,
    file,
  }));
}

/** Every string AiAttachmentTray shows or announces. */
export interface AiAttachmentTrayMessages {
  /** Name of the list. */
  label: string;
  /** Short type label when the extension is unknown. */
  fileType: string;
  /** Formats a size in bytes. */
  size: (bytes: number) => string;
  /** Visible progress, e.g. "Uploading 45%". */
  uploading: (percent: number) => string;
  /** Accessible name of a file's progress bar. */
  progressLabel: (name: string) => string;
  /** Shown when `error` is `true`. */
  error: string;
  retry: string;
  /** Accessible name of the retry button. */
  retryLabel: (name: string) => string;
  /** Accessible name of the remove button. */
  remove: (name: string) => string;
  /** Announced politely after a file is removed. */
  removed: (name: string) => string;
  /** Announced politely when an upload finishes. */
  ready: (name: string) => string;
  /** Announced assertively when an upload fails. */
  failed: (name: string, reason: string) => string;
}

export const AI_ATTACHMENT_TRAY_MESSAGES: AiAttachmentTrayMessages = {
  label: 'Attachments',
  fileType: 'File',
  size: formatBytes,
  uploading: (pct) => `Uploading ${pct}%`,
  progressLabel: (name) => `Uploading ${name}`,
  error: 'Upload failed',
  retry: 'Retry',
  retryLabel: (name) => `Retry uploading ${name}`,
  remove: (name) => `Remove ${name}`,
  removed: (name) => `${name} removed`,
  ready: (name) => `${name} uploaded`,
  failed: (name, reason) => `${name}: ${reason}`,
};

export interface AiAttachmentTrayProps extends Omit<HTMLAttributes<HTMLDivElement>, 'children'> {
  /** The attached files, in the order they were added. */
  files: readonly AiAttachmentTrayFile[];
  /**
   * Removes a file. Take it out of `files`; focus moves to the next file,
   * else the previous, else `fallbackFocusRef` (or the tray).
   */
  onRemove: (id: string) => void;
  /** Retries a failed upload. Without it failed files show no Retry button. */
  onRetry?: ((id: string) => void) | undefined;
  /** Where focus goes when the last file is removed, typically the prompt textarea. */
  fallbackFocusRef?: RefObject<HTMLElement | null> | undefined;
  /** Override any string. */
  messages?: Partial<AiAttachmentTrayMessages> | undefined;
}

const extensionOf = (name: string) => {
  const dot = name.lastIndexOf('.');
  return dot > 0 && dot < name.length - 1
    ? name
        .slice(dot + 1)
        .toUpperCase()
        .slice(0, 4)
    : '';
};

/**
 * The files attached to a prompt, as tiles with a thumbnail or type badge,
 * name and size, upload progress, and errors with Retry. Each tile has a
 * remove button and focus stays in the tray when one is removed. Works on its
 * own or as AiPromptInput's `renderAttachments` (see `aiAttachmentTrayFiles`).
 */
export const AiAttachmentTray = forwardRef<HTMLDivElement, AiAttachmentTrayProps>(
  function AiAttachmentTray(
    { files, onRemove, onRetry, fallbackFocusRef, messages: messagesProp, className, ...rest },
    ref,
  ) {
    const m = { ...AI_ATTACHMENT_TRAY_MESSAGES, ...messagesProp };
    const uid = useId();
    const root = useRef<HTMLDivElement>(null);
    useImperativeHandle(ref, () => root.current as HTMLDivElement, []);
    const removeButtons = useRef(new Map<string, HTMLButtonElement>());
    const removed = useRef<{ id: string; index: number; name: string } | null>(null);
    const [polite, setPolite] = useState('');
    const [assertive, setAssertive] = useState('');
    const statuses = useRef(new Map<string, AiAttachmentStatus>());

    // Object URLs for image files without a preview URL, made after mount (SSR-safe) and revoked.
    const [objectUrls, setObjectUrls] = useState<Record<string, string>>({});
    const urlKey = files
      .filter((f) => f.file && !f.previewUrl && f.type.startsWith('image/'))
      .map((f) => f.id)
      .join('|');
    useEffect(() => {
      if (typeof URL === 'undefined' || typeof URL.createObjectURL !== 'function') return;
      const made: Record<string, string> = {};
      for (const f of files) {
        if (f.file && !f.previewUrl && f.type.startsWith('image/')) {
          made[f.id] = URL.createObjectURL(f.file);
        }
      }
      setObjectUrls(made);
      return () => {
        for (const url of Object.values(made)) URL.revokeObjectURL(url);
      };
      // Recreate only when the set of image files changes.
      // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [urlKey]);

    // Announce finished and failed uploads once, on the transition.
    useEffect(() => {
      const seen = statuses.current;
      const next = new Map<string, AiAttachmentStatus>();
      for (const f of files) {
        const status = aiAttachmentStatus(f);
        const before = seen.get(f.id);
        if (before && before !== status) {
          if (status === 'error') {
            setAssertive(m.failed(f.name, typeof f.error === 'string' ? f.error : m.error));
          } else if (status === 'ready' && before === 'uploading') {
            setPolite(m.ready(f.name));
          }
        }
        next.set(f.id, status);
      }
      statuses.current = next;
      // Messages are read at transition time; files drive the effect.
      // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [files]);

    // When a removed file leaves `files`, keep focus in the tray.
    useEffect(() => {
      const r = removed.current;
      if (!r || files.some((f) => f.id === r.id)) return;
      removed.current = null;
      setPolite(m.removed(r.name));
      const candidates = [...files.slice(r.index), ...files.slice(0, r.index).reverse()];
      const target = candidates.map((f) => removeButtons.current.get(f.id)).find(Boolean);
      if (target) target.focus();
      else (fallbackFocusRef?.current ?? root.current)?.focus();
      // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [files]);

    return (
      <div
        ref={root}
        tabIndex={-1}
        {...rest}
        className={cx('mn-ai-attachment-tray', className)}
        data-empty={files.length === 0 || undefined}
      >
        {files.length ? (
          <ul className="mn-ai-attachment-tray__list" aria-label={m.label}>
            {files.map((f, index) => {
              const status = aiAttachmentStatus(f);
              const preview = f.previewUrl ?? objectUrls[f.id];
              const thumb = preview && isSafeImageSrc(preview) ? preview : undefined;
              const pct = Math.round(Math.min(1, Math.max(0, f.progress ?? 1)) * 100);
              const errorText = typeof f.error === 'string' && f.error ? f.error : m.error;
              const errorId = `${uid}-${index}-error`;
              return (
                <li
                  key={f.id}
                  className="mn-ai-attachment-tray__item"
                  data-status={status}
                  aria-busy={status === 'uploading' || undefined}
                >
                  <span className="mn-ai-attachment-tray__preview" aria-hidden>
                    {thumb ? (
                      <img className="mn-ai-attachment-tray__thumb" src={thumb} alt="" />
                    ) : (
                      <span className="mn-ai-attachment-tray__type">
                        {extensionOf(f.name) || m.fileType}
                      </span>
                    )}
                  </span>
                  <span className="mn-ai-attachment-tray__body">
                    <span className="mn-ai-attachment-tray__name" title={f.name}>
                      {f.name}
                    </span>
                    <span className="mn-ai-attachment-tray__meta">
                      {status === 'error' ? (
                        <span id={errorId} className="mn-ai-attachment-tray__error">
                          <ErrorGlyph aria-hidden />
                          {errorText}
                        </span>
                      ) : (
                        <>
                          <span>{m.size(f.size)}</span>
                          {status === 'uploading' ? (
                            <span className="mn-ai-attachment-tray__percent">
                              {m.uploading(pct)}
                            </span>
                          ) : null}
                        </>
                      )}
                    </span>
                    {status === 'uploading' ? (
                      <Progress
                        className="mn-ai-attachment-tray__progress"
                        size="sm"
                        value={pct}
                        label={m.progressLabel(f.name)}
                      />
                    ) : null}
                  </span>
                  <span className="mn-ai-attachment-tray__actions">
                    {status === 'error' && onRetry ? (
                      <Button
                        size="xs"
                        variant="ghost"
                        leadingIcon={<RetryGlyph />}
                        aria-label={m.retryLabel(f.name)}
                        aria-describedby={errorId}
                        className="mn-ai-attachment-tray__retry"
                        onClick={() => onRetry(f.id)}
                      >
                        {m.retry}
                      </Button>
                    ) : null}
                    <IconButton
                      ref={(el) => {
                        if (el) removeButtons.current.set(f.id, el);
                        else removeButtons.current.delete(f.id);
                      }}
                      size="xs"
                      label={m.remove(f.name)}
                      icon={<CloseGlyph />}
                      className="mn-ai-attachment-tray__remove"
                      onClick={() => {
                        removed.current = { id: f.id, index, name: f.name };
                        onRemove(f.id);
                      }}
                    />
                  </span>
                </li>
              );
            })}
          </ul>
        ) : null}
        <span role="status" className="mn-visually-hidden">
          {polite}
        </span>
        <span role="alert" className="mn-visually-hidden">
          {assertive}
        </span>
      </div>
    );
  },
);
