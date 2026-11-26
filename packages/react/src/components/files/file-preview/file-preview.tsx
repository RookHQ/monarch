'use client';

import { forwardRef, useState, type HTMLAttributes, type ReactNode } from 'react';
import { cx } from '../../../internal/cx.js';
import { isSafeUrl } from '../../../internal/sanitize.js';
import { Button } from '../../actions/button/index.js';
import { FileTypeIcon, getFileTypeFamily } from '../file-type-icon/index.js';

/** How a file is shown. */
export type FilePreviewKind = 'image' | 'pdf' | 'text' | 'video' | 'audio' | 'none';

/** The preview kind for a file, from its MIME type and name. */
export function filePreviewKind(file: {
  name?: string | undefined;
  type?: string | undefined;
}): FilePreviewKind {
  const family = getFileTypeFamily(file);
  if (family === 'image' || family === 'pdf' || family === 'video' || family === 'audio')
    return family;
  if (family === 'text' || family === 'code') return 'text';
  return 'none';
}

/** Every string a file preview shows or announces. */
export interface FilePreviewMessages {
  /** Accessible name of the preview frame or media. */
  previewOf: (name: string) => string;
  /** Shown when the type cannot be previewed. */
  unavailable: string;
  /** Shown when the preview failed to load. */
  failed: string;
  /** Text of the download link. */
  download: string;
  /** Accessible name of the download link. */
  downloadName: (name: string) => string;
}

export const FILE_PREVIEW_MESSAGES: FilePreviewMessages = {
  previewOf: (name) => `Preview of ${name}`,
  unavailable: 'There is no preview for this file type.',
  failed: 'The preview could not be loaded.',
  download: 'Download',
  downloadName: (name) => `Download ${name}`,
};

const PREVIEW_PROTOCOLS = ['http', 'https', 'blob'];

export interface FilePreviewProps extends Omit<HTMLAttributes<HTMLDivElement>, 'children'> {
  /** The file name, used for the type, labels and the download name. */
  name: string;
  /** Where the file is. Only http(s), blob and relative URLs are loaded. */
  src?: string | undefined;
  /** MIME type. Decides the preview with the name. */
  contentType?: string | undefined;
  /** Force a preview kind instead of detecting it. `none` shows the download fallback. */
  kind?: FilePreviewKind | undefined;
  /**
   * Text content to show for text and code files. Without it, text files are
   * shown in a sandboxed frame (no scripts) from `src`.
   */
  text?: string | undefined;
  /** Alternative text for an image. Default: the file name. Use `''` when a caption next to it says the same. */
  alt?: string | undefined;
  /** WebVTT captions for a video (http(s), blob or relative). */
  captionsSrc?: string | undefined;
  /** Language of the captions. Default `en`. */
  captionsLang?: string | undefined;
  /** URL for the download link. Default: `src`. */
  downloadHref?: string | undefined;
  /** Show the download link under every preview, not only when there is none. Default false. */
  alwaysShowDownload?: boolean | undefined;
  /** Replaces the default fallback (icon, message and download link). */
  fallback?: ReactNode;
  /** Translated strings. */
  messages?: Partial<FilePreviewMessages> | undefined;
}

/**
 * Shows a file’s content in place: images, PDFs in the browser’s own viewer,
 * text, video and audio. Anything else, or anything that fails to load,
 * falls back to the type icon and a download link.
 */
export const FilePreview = forwardRef<HTMLDivElement, FilePreviewProps>(function FilePreview(
  {
    name,
    src,
    contentType,
    kind: kindProp,
    text,
    alt,
    captionsSrc,
    captionsLang = 'en',
    downloadHref,
    alwaysShowDownload = false,
    fallback,
    messages: messagesProp,
    className,
    ...rest
  },
  ref,
) {
  const m = { ...FILE_PREVIEW_MESSAGES, ...messagesProp };
  const [failedSrc, setFailedSrc] = useState<string | null>(null);
  const safeSrc = src && isSafeUrl(src, PREVIEW_PROTOCOLS) ? src : undefined;
  const download =
    (downloadHref && isSafeUrl(downloadHref, PREVIEW_PROTOCOLS) ? downloadHref : undefined) ??
    safeSrc;
  const detected = kindProp ?? filePreviewKind({ name, type: contentType });
  const failed = safeSrc !== undefined && failedSrc === safeSrc;
  const canShow = !failed && (detected === 'text' ? text !== undefined || !!safeSrc : !!safeSrc);
  const kind: FilePreviewKind = canShow ? detected : 'none';
  const onError = () => setFailedSrc(safeSrc ?? null);
  const label = m.previewOf(name);

  const downloadLink = download ? (
    <Button asChild size="sm" variant="secondary">
      <a href={download} download={name} aria-label={m.downloadName(name)}>
        {m.download}
      </a>
    </Button>
  ) : null;

  let body: ReactNode;
  switch (kind) {
    case 'image':
      body = (
        <img
          className="mn-file-preview__image"
          src={safeSrc}
          alt={alt ?? name}
          decoding="async"
          onError={onError}
        />
      );
      break;
    case 'pdf':
      // The browser's own PDF viewer. <object> shows its children where no
      // viewer is available (most phones), so the download is the fallback.
      body = (
        <object
          className="mn-file-preview__frame"
          data={safeSrc}
          type="application/pdf"
          title={label}
          aria-label={label}
        >
          <Fallback name={name} message={m.unavailable} action={downloadLink} />
        </object>
      );
      break;
    case 'text':
      body =
        text !== undefined ? (
          // A focusable scroll area so keyboard users can scroll long text.
          // eslint-disable-next-line jsx-a11y/no-noninteractive-tabindex
          <pre className="mn-file-preview__text" tabIndex={0} aria-label={label}>
            {text}
          </pre>
        ) : (
          <iframe
            className="mn-file-preview__frame"
            src={safeSrc}
            title={label}
            // No scripts, forms, popups or same-origin access.
            sandbox=""
            referrerPolicy="no-referrer"
          />
        );
      break;
    case 'video':
      body = (
        // Captions come from `captionsSrc`; without them the product must provide a transcript.
        // eslint-disable-next-line jsx-a11y/media-has-caption
        <video
          className="mn-file-preview__media"
          src={safeSrc}
          controls
          preload="metadata"
          aria-label={label}
          onError={onError}
        >
          {captionsSrc && isSafeUrl(captionsSrc, PREVIEW_PROTOCOLS) ? (
            <track kind="captions" src={captionsSrc} srcLang={captionsLang} default />
          ) : null}
        </video>
      );
      break;
    case 'audio':
      body = (
        // eslint-disable-next-line jsx-a11y/media-has-caption
        <audio
          className="mn-file-preview__audio"
          src={safeSrc}
          controls
          preload="metadata"
          aria-label={label}
          onError={onError}
        />
      );
      break;
    default:
      body = fallback ?? (
        <Fallback
          name={name}
          contentType={contentType}
          message={failed ? m.failed : m.unavailable}
          action={downloadLink}
        />
      );
  }

  return (
    <div
      ref={ref}
      {...rest}
      className={cx('mn-file-preview', className)}
      data-kind={kind}
      data-failed={failed || undefined}
    >
      {body}
      {alwaysShowDownload && kind !== 'none' && downloadLink ? (
        <div className="mn-file-preview__footer">{downloadLink}</div>
      ) : null}
    </div>
  );
});

function Fallback({
  name,
  contentType,
  message,
  action,
}: {
  name: string;
  contentType?: string | undefined;
  message: string;
  action: ReactNode;
}) {
  return (
    <div className="mn-file-preview__fallback">
      <FileTypeIcon name={name} contentType={contentType} size="lg" />
      <p className="mn-file-preview__name">{name}</p>
      <p className="mn-file-preview__message">{message}</p>
      {action}
    </div>
  );
}
