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
import { Button } from '../../actions/button/index.js';
import { Progress } from '../../data-display/progress-bar/index.js';
import { InlineNotice } from '../../feedback/inline-notice/index.js';
import { Field, FieldDescription, FieldLabel } from '../../forms/form-field/index.js';
import { FileUpload, formatBytes, type FileUploadMessages } from '../file-upload/index.js';

/** The kinds of media the field takes. */
export type MediaKind = 'image' | 'video';

const VIDEO_EXT = /\.(mp4|m4v|mov|webm|ogv|avi)(?:[?#]|$)/i;

/** Guesses whether a URL or MIME type is a video (by `video/` type or extension); otherwise image. */
export function mediaKindOf(source: string): MediaKind {
  const s = source.trim().toLowerCase();
  if (s.startsWith('video/') || s.startsWith('data:video/') || VIDEO_EXT.test(s)) return 'video';
  return 'image';
}

/**
 * Reads a video file's length in seconds from its metadata, in the browser.
 * Uses a temporary object URL that is revoked afterwards.
 */
export function readVideoDuration(file: Blob): Promise<number> {
  return new Promise((resolve, reject) => {
    if (typeof document === 'undefined' || typeof URL.createObjectURL !== 'function') {
      reject(new Error('readVideoDuration needs a browser.'));
      return;
    }
    const url = URL.createObjectURL(file);
    const video = document.createElement('video');
    video.preload = 'metadata';
    const done = () => URL.revokeObjectURL(url);
    video.onloadedmetadata = () => {
      done();
      resolve(video.duration);
    };
    video.onerror = () => {
      done();
      reject(new Error('The video’s length could not be read.'));
    };
    video.src = url;
  });
}

/** What `onUpload` resolves with: the stored file's URL, and optional warnings to show. */
export type MediaUploadResult = string | { url: string; warnings?: string[] | undefined };

/** Passed to `onUpload` with the file. */
export interface MediaUploadContext {
  kind: MediaKind;
  /** Report progress, 0–100. Without calls the bar is indeterminate. */
  onProgress: (percent: number) => void;
  /** Aborted when the person cancels or the component unmounts. */
  signal: AbortSignal;
}

/** Visible and announced strings. Override them to translate. */
export interface MediaUploadMessages {
  dropImage: string;
  dropMedia: string;
  browseImage: string;
  browseMedia: string;
  imageHint: (maxSize?: string) => string;
  videoHint: (maxSize?: string, maxDuration?: string) => string;
  videoNotAllowed: (name: string) => string;
  tooLarge: (name: string, maxSize: string) => string;
  tooLong: (name: string, maxDuration: string) => string;
  durationUnreadable: (name: string) => string;
  checking: (name: string) => string;
  uploading: (name: string) => string;
  uploaded: (kind: MediaKind) => string;
  uploadFailed: string;
  cancel: string;
  cancelled: string;
  replace: (kind: MediaKind) => string;
  remove: (kind: MediaKind) => string;
  removed: (kind: MediaKind) => string;
  previewAlt: string;
  warnings: string;
  formatDuration: (seconds: number) => string;
}

export const DEFAULT_MEDIA_UPLOAD_MESSAGES: MediaUploadMessages = {
  dropImage: 'Drop an image here',
  dropMedia: 'Drop an image or video here',
  browseImage: 'Choose image',
  browseMedia: 'Choose image or video',
  imageHint: (max) => `PNG, JPG, GIF or WebP${max ? ` up to ${max}` : ''}`,
  videoHint: (max, duration) =>
    `MP4, MOV or WebM${max ? ` up to ${max}` : ''}${duration ? `, ${duration} at most` : ''}`,
  videoNotAllowed: (name) => `${name} is a video. Only images can be added here.`,
  tooLarge: (name, max) => `${name} is larger than ${max}.`,
  tooLong: (name, max) => `${name} is longer than ${max}.`,
  durationUnreadable: (name) => `The length of ${name} could not be read.`,
  checking: (name) => `Checking ${name}…`,
  uploading: (name) => `Uploading ${name}`,
  uploaded: (kind) => (kind === 'video' ? 'Video uploaded' : 'Image uploaded'),
  uploadFailed: 'The upload failed. Try again.',
  cancel: 'Cancel upload',
  cancelled: 'Upload cancelled',
  replace: (kind) => (kind === 'video' ? 'Replace video' : 'Replace image'),
  remove: (kind) => (kind === 'video' ? 'Remove video' : 'Remove image'),
  removed: (kind) => (kind === 'video' ? 'Video removed' : 'Image removed'),
  previewAlt: 'Uploaded image',
  warnings: 'Uploaded with warnings',
  formatDuration: (s) =>
    s >= 60 ? `${Math.floor(s / 60)}:${String(s % 60).padStart(2, '0')} min` : `${s} s`,
};

export interface MediaUploadProps extends Omit<
  HTMLAttributes<HTMLDivElement>,
  'onChange' | 'defaultValue'
> {
  /** Visible label. */
  label: ReactNode;
  /** Help text under the label. */
  description?: ReactNode;
  /** Current media URL (controlled). `null` or `''` is empty. */
  value?: string | null | undefined;
  /** Starting URL when uncontrolled. */
  defaultValue?: string | null | undefined;
  /** Kind of the current URL. Default: guessed from the URL with `mediaKindOf`. */
  valueKind?: MediaKind | undefined;
  /** Called with the new URL after an upload, or `null` after Remove. */
  onChange?:
    ((url: string | null, details: { kind: MediaKind | null; file?: File }) => void) | undefined;
  /**
   * Stores the file and resolves with its URL. Report progress through
   * `onProgress` and stop when `signal` aborts. Reject to show an error.
   */
  onUpload: (file: File, context: MediaUploadContext) => Promise<MediaUploadResult>;
  /** Which kinds are accepted. Default `['image']`. */
  kinds?: MediaKind[] | undefined;
  /** Image types (an `accept` string). Default JPEG, PNG, GIF and WebP. */
  imageAccept?: string | undefined;
  /** Video types (an `accept` string). Default MP4, QuickTime and WebM. */
  videoAccept?: string | undefined;
  /** Largest image, in bytes. */
  maxImageSize?: number | undefined;
  /** Largest video, in bytes. */
  maxVideoSize?: number | undefined;
  /** Longest video, in seconds (read from the file's metadata before uploading). */
  maxVideoDuration?: number | undefined;
  /**
   * Why videos cannot be added here, shown under the hint when `kinds`
   * excludes video (for example a plan limit, in the product's words).
   * Dropped videos are then refused with a clear message.
   */
  videoUnavailable?: ReactNode;
  /** Runs before upload, for example to compress or resize. */
  transform?: ((file: File, kind: MediaKind) => Promise<File>) | undefined;
  /** Text alternative for an image preview. */
  previewAlt?: string | undefined;
  disabled?: boolean | undefined;
  /** Override visible and announced strings. */
  messages?: Partial<MediaUploadMessages> | undefined;
  /** Strings for the inner FileUpload. */
  fileUploadMessages?: Partial<FileUploadMessages> | undefined;
}

type Status =
  | { state: 'idle' }
  | { state: 'checking'; name: string }
  | { state: 'uploading'; name: string; progress: number | null };

/**
 * A single image (or video) field: drop or choose a file, see it checked
 * (type, size, video length), watch it upload, then preview it with Replace
 * and Remove. The product stores the file in `onUpload`; Monarch does no
 * network or compression work.
 */
export const MediaUpload = forwardRef<HTMLDivElement, MediaUploadProps>(function MediaUpload(
  {
    label,
    description,
    value: valueProp,
    defaultValue = null,
    valueKind,
    onChange,
    onUpload,
    kinds = ['image'],
    imageAccept = 'image/jpeg,image/png,image/gif,image/webp',
    videoAccept = 'video/mp4,video/quicktime,video/webm',
    maxImageSize,
    maxVideoSize,
    maxVideoDuration,
    videoUnavailable,
    transform,
    previewAlt,
    disabled = false,
    messages: messagesProp,
    fileUploadMessages,
    className,
    ...rest
  },
  ref,
) {
  const m = { ...DEFAULT_MEDIA_UPLOAD_MESSAGES, ...messagesProp };
  const [own, setOwn] = useState<string | null>(defaultValue);
  const value = (valueProp === undefined ? own : valueProp) || null;
  const kind: MediaKind | null = value ? (valueKind ?? mediaKindOf(value)) : null;
  const [status, setStatus] = useState<Status>({ state: 'idle' });
  const [error, setError] = useState<string | null>(null);
  const [warnings, setWarnings] = useState<string[]>([]);
  const [announce, setAnnounce] = useState('');
  const controller = useRef<AbortController | null>(null);
  const replaceInput = useRef<HTMLInputElement>(null);
  const replaceButton = useRef<HTMLButtonElement>(null);
  const previewId = useId();
  const root = useRef<HTMLDivElement | null>(null);
  // Where focus goes after the control it was on disappears (upload finished, media removed).
  const focusNext = useRef<'replace' | 'browse' | null>(null);

  useEffect(() => () => controller.current?.abort(), []);

  useEffect(() => {
    const target = focusNext.current;
    if (!target) return;
    const el =
      target === 'replace'
        ? replaceButton.current
        : root.current?.querySelector<HTMLButtonElement>('.mn-file-upload__browse');
    if (!el) return;
    focusNext.current = null;
    el.focus();
  });

  const allowImage = kinds.includes('image');
  const allowVideo = kinds.includes('video');
  // Include video types when videos are refused with a reason, so the refusal can say why.
  const accept = [
    allowImage ? imageAccept : null,
    allowVideo || videoUnavailable ? videoAccept : null,
  ]
    .filter(Boolean)
    .join(',');

  const kindOfFile = (file: File): MediaKind => mediaKindOf(file.type || file.name);

  const validate = (file: File): string | null => {
    const k = kindOfFile(file);
    if (k === 'video' && !allowVideo) return m.videoNotAllowed(file.name);
    const max = k === 'video' ? maxVideoSize : maxImageSize;
    if (max !== undefined && file.size > max) return m.tooLarge(file.name, formatBytes(max));
    return null;
  };

  const setValue = (url: string | null, details: { kind: MediaKind | null; file?: File }) => {
    if (valueProp === undefined) setOwn(url);
    onChange?.(url, details);
  };

  const start = async (file: File) => {
    const fileKind = kindOfFile(file);
    controller.current?.abort();
    const ac = new AbortController();
    controller.current = ac;
    setError(null);
    setWarnings([]);
    setAnnounce('');
    try {
      if (fileKind === 'video' && maxVideoDuration !== undefined) {
        setStatus({ state: 'checking', name: file.name });
        let seconds: number;
        try {
          seconds = await readVideoDuration(file);
        } catch {
          throw new Error(m.durationUnreadable(file.name));
        }
        if (seconds > maxVideoDuration)
          throw new Error(m.tooLong(file.name, m.formatDuration(maxVideoDuration)));
      }
      if (ac.signal.aborted) return;
      const prepared = transform ? await transform(file, fileKind) : file;
      if (ac.signal.aborted) return;
      setStatus({ state: 'uploading', name: file.name, progress: null });
      const result = await onUpload(prepared, {
        kind: fileKind,
        signal: ac.signal,
        onProgress: (percent) => {
          if (!ac.signal.aborted)
            setStatus({
              state: 'uploading',
              name: file.name,
              progress: Math.max(0, Math.min(100, percent)),
            });
        },
      });
      if (ac.signal.aborted) return;
      const url = typeof result === 'string' ? result : result.url;
      const resultWarnings = typeof result === 'string' ? [] : (result.warnings ?? []);
      setWarnings(resultWarnings);
      setStatus({ state: 'idle' });
      setAnnounce(m.uploaded(fileKind));
      focusNext.current = 'replace';
      setValue(url, { kind: fileKind, file });
    } catch (e) {
      if (ac.signal.aborted) return;
      setStatus({ state: 'idle' });
      focusNext.current = value ? 'replace' : 'browse';
      setError(e instanceof Error && e.message ? e.message : m.uploadFailed);
    } finally {
      if (controller.current === ac) controller.current = null;
    }
  };

  const cancel = () => {
    controller.current?.abort();
    controller.current = null;
    setStatus({ state: 'idle' });
    focusNext.current = value ? 'replace' : 'browse';
    setAnnounce(m.cancelled);
  };

  const remove = () => {
    const was = kind;
    setWarnings([]);
    setError(null);
    setAnnounce(was ? m.removed(was) : '');
    focusNext.current = 'browse';
    setValue(null, { kind: null });
  };

  const busy = status.state !== 'idle';
  const imageMax = maxImageSize !== undefined ? formatBytes(maxImageSize) : undefined;
  const videoMax = maxVideoSize !== undefined ? formatBytes(maxVideoSize) : undefined;
  const hint = (
    <>
      {allowImage ? (
        <span className="mn-media-upload__hint-line">{m.imageHint(imageMax)}</span>
      ) : null}
      {allowVideo ? (
        <span className="mn-media-upload__hint-line">
          {m.videoHint(
            videoMax,
            maxVideoDuration !== undefined ? m.formatDuration(maxVideoDuration) : undefined,
          )}
        </span>
      ) : null}
      {!allowVideo && videoUnavailable ? (
        <span className="mn-media-upload__hint-line" data-tone="muted">
          {videoUnavailable}
        </span>
      ) : null}
    </>
  );

  return (
    <div
      ref={(el) => {
        root.current = el;
        if (typeof ref === 'function') ref(el);
        else if (ref) ref.current = el;
      }}
      {...rest}
      data-state={busy ? status.state : value ? 'filled' : 'empty'}
      className={cx('mn-media-upload', className)}
    >
      <Field disabled={disabled} invalid={!!error}>
        <FieldLabel>{label}</FieldLabel>
        {description ? <FieldDescription>{description}</FieldDescription> : null}

        {value && !busy ? (
          <div className="mn-media-upload__preview" id={previewId}>
            {kind === 'video' ? (
              // A preview of the person's own upload: there is no caption track to offer.
              // eslint-disable-next-line jsx-a11y/media-has-caption
              <video
                className="mn-media-upload__media"
                src={value}
                controls
                playsInline
                preload="metadata"
              />
            ) : (
              <img
                className="mn-media-upload__media"
                src={value}
                alt={previewAlt ?? m.previewAlt}
              />
            )}
            <div className="mn-media-upload__actions">
              <Button
                ref={replaceButton}
                size="sm"
                disabled={disabled}
                onClick={() => replaceInput.current?.click()}
              >
                {m.replace(kind ?? 'image')}
              </Button>
              <Button size="sm" variant="danger-subtle" disabled={disabled} onClick={remove}>
                {m.remove(kind ?? 'image')}
              </Button>
              <input
                ref={replaceInput}
                type="file"
                className="mn-visually-hidden"
                tabIndex={-1}
                aria-hidden
                accept={accept}
                disabled={disabled}
                onChange={(event) => {
                  const file = event.currentTarget.files?.[0];
                  event.currentTarget.value = '';
                  if (!file) return;
                  const problem = validate(file);
                  if (problem) setError(problem);
                  else void start(file);
                }}
              />
            </div>
          </div>
        ) : busy ? (
          <div className="mn-media-upload__progress">
            <Progress
              label={
                status.state === 'checking' ? m.checking(status.name) : m.uploading(status.name)
              }
              value={status.state === 'uploading' ? status.progress : null}
            />
            <div className="mn-media-upload__progress-row">
              <span className="mn-media-upload__progress-text">
                {status.state === 'checking'
                  ? m.checking(status.name)
                  : `${m.uploading(status.name)}${
                      status.progress !== null ? ` · ${Math.round(status.progress)}%` : ''
                    }`}
              </span>
              <Button size="sm" variant="ghost" onClick={cancel}>
                {m.cancel}
              </Button>
            </div>
          </div>
        ) : (
          <FileUpload
            accept={accept}
            maxFiles={1}
            validate={validate}
            disabled={disabled}
            hint={hint}
            messages={{
              label: allowVideo ? m.dropMedia : m.dropImage,
              browse: allowVideo ? m.browseMedia : m.browseImage,
              ...fileUploadMessages,
            }}
            onFiles={(accepted) => {
              setError(null);
              const file = accepted[0];
              if (file) void start(file);
            }}
          />
        )}
      </Field>

      {error ? (
        <InlineNotice tone="danger" live="assertive" className="mn-media-upload__error">
          {error}
        </InlineNotice>
      ) : null}
      {warnings.length > 0 ? (
        <InlineNotice tone="warning" title={m.warnings} className="mn-media-upload__warnings">
          <ul className="mn-media-upload__warning-list">
            {warnings.map((w, i) => (
              <li key={i}>{w}</li>
            ))}
          </ul>
        </InlineNotice>
      ) : null}
      <span className="mn-visually-hidden" aria-live="polite">
        {announce}
      </span>
    </div>
  );
});
