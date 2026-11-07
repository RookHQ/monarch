'use client';

import {
  forwardRef,
  useEffect,
  useState,
  type ButtonHTMLAttributes,
  type CSSProperties,
  type HTMLAttributes,
  type Ref,
} from 'react';
import { cx } from '../../../internal/cx.js';

export interface CaptureVideoFrameOptions {
  /**
   * Second to capture. Default: one second in, or 10% of the duration for
   * clips shorter than ten seconds.
   */
  time?: number | undefined;
  /** CORS mode for the temporary `<video>`. Default `anonymous`. */
  crossOrigin?: 'anonymous' | 'use-credentials' | undefined;
  /** Image type. Default `image/jpeg`. */
  type?: string | undefined;
  /** Image quality 0–1 for lossy types. Default 0.8. */
  quality?: number | undefined;
  /** Cancels the capture. */
  signal?: AbortSignal | undefined;
}

/**
 * Captures one frame of a video as a data URL, in the browser only. Loads the
 * video in a detached, muted `<video>`, seeks, draws to a canvas and cleans
 * up. Rejects when the video cannot load, the server does not allow
 * cross-origin reads (a tainted canvas), or it is aborted.
 */
export function captureVideoFrame(
  src: string,
  {
    time,
    crossOrigin = 'anonymous',
    type = 'image/jpeg',
    quality = 0.8,
    signal,
  }: CaptureVideoFrameOptions = {},
): Promise<string> {
  return new Promise((resolve, reject) => {
    if (typeof document === 'undefined') {
      reject(new Error('captureVideoFrame needs a browser.'));
      return;
    }
    const video = document.createElement('video');
    let done = false;
    const cleanup = () => {
      done = true;
      video.removeAttribute('src');
      video.load();
      signal?.removeEventListener('abort', onAbort);
    };
    const fail = (error: unknown) => {
      if (done) return;
      cleanup();
      reject(error instanceof Error ? error : new Error(String(error)));
    };
    const onAbort = () => fail(new DOMException('Aborted', 'AbortError'));
    if (signal?.aborted) {
      reject(new DOMException('Aborted', 'AbortError'));
      return;
    }
    signal?.addEventListener('abort', onAbort);
    video.crossOrigin = crossOrigin;
    video.muted = true;
    video.playsInline = true;
    video.preload = 'auto';
    video.addEventListener('error', () => fail(new Error('The video could not be loaded.')));
    video.addEventListener('loadeddata', () => {
      const duration = Number.isFinite(video.duration) ? video.duration : 0;
      const at = time ?? Math.min(1, duration * 0.1);
      video.currentTime = Math.max(0, Math.min(at, Math.max(0, duration - 0.05)));
    });
    video.addEventListener('seeked', () => {
      if (done) return;
      try {
        const canvas = document.createElement('canvas');
        canvas.width = video.videoWidth;
        canvas.height = video.videoHeight;
        const ctx = canvas.getContext('2d');
        if (!ctx || !canvas.width || !canvas.height) throw new Error('No frame to draw.');
        ctx.drawImage(video, 0, 0, canvas.width, canvas.height);
        const url = canvas.toDataURL(type, quality);
        cleanup();
        resolve(url);
      } catch (error) {
        fail(error);
      }
    });
    video.src = src;
    video.load();
  });
}

/** Formats seconds as `m:ss` or `h:mm:ss`. */
export function formatVideoDuration(seconds: number): string {
  const s = Math.max(0, Math.round(seconds));
  const h = Math.floor(s / 3600);
  const m = Math.floor((s % 3600) / 60);
  const ss = String(s % 60).padStart(2, '0');
  return h ? `${h}:${String(m).padStart(2, '0')}:${ss}` : `${m}:${ss}`;
}

/** Visible and announced strings. Override them to translate. */
export interface VideoThumbnailMessages {
  /** Accessible name of the button when `onPlay` is set. */
  play: (alt: string) => string;
  /** Spoken duration, appended to the name. */
  duration: (formatted: string) => string;
  /** Text alternative used when `alt` is empty. */
  fallbackAlt: string;
}

export const DEFAULT_VIDEO_THUMBNAIL_MESSAGES: VideoThumbnailMessages = {
  play: (alt) => `Play ${alt}`,
  duration: (formatted) => `duration ${formatted}`,
  fallbackAlt: 'Video',
};

type RootProps = Omit<HTMLAttributes<HTMLElement>, 'children' | 'onPlay'> &
  Pick<ButtonHTMLAttributes<HTMLButtonElement>, 'disabled' | 'type'>;

export interface VideoThumbnailProps extends RootProps {
  /** The video. Used to capture a frame when there is no `thumbnailUrl`. */
  src?: string | undefined;
  /** A ready-made poster image. Skips frame capture. */
  thumbnailUrl?: string | null | undefined;
  /** What the video shows, for the image's text alternative and the play button's name. */
  alt: string;
  /** Makes the thumbnail a button that calls this, named "Play <alt>". */
  onPlay?: (() => void) | undefined;
  /** Shows the round play icon in the middle. Default true. */
  showPlayIcon?: boolean | undefined;
  /** Length in seconds, shown as a badge (`1:05`). */
  duration?: number | undefined;
  /** CSS aspect ratio. Default `16 / 9`. */
  aspectRatio?: string | undefined;
  /** `cover` crops to fill (default); `contain` letterboxes. */
  fit?: 'cover' | 'contain' | undefined;
  /** Second to capture when generating a frame. */
  captureTime?: number | undefined;
  /** CORS mode for frame capture. Default `anonymous`. */
  crossOrigin?: 'anonymous' | 'use-credentials' | undefined;
  /** Called when no frame could be captured (the placeholder stays). */
  onCaptureError?: ((error: Error) => void) | undefined;
  /** Override visible and announced strings. */
  messages?: Partial<VideoThumbnailMessages> | undefined;
}

function PlayGlyph() {
  return (
    <svg viewBox="0 0 24 24" width="24" height="24" aria-hidden="true" focusable="false">
      <path
        d="M8 5.5v13a1 1 0 0 0 1.5.86l10.5-6.5a1 1 0 0 0 0-1.72L9.5 4.64A1 1 0 0 0 8 5.5z"
        fill="currentColor"
      />
    </svg>
  );
}

/**
 * A poster for a video: the given thumbnail, or a frame captured from the
 * video in the browser, with an optional play icon and duration badge. With
 * `onPlay` it is a button. Renders the placeholder on the server and captures
 * after hydration.
 */
export const VideoThumbnail = forwardRef<HTMLElement, VideoThumbnailProps>(function VideoThumbnail(
  {
    src,
    thumbnailUrl,
    alt,
    onPlay,
    showPlayIcon = true,
    duration,
    aspectRatio = '16 / 9',
    fit = 'cover',
    captureTime,
    crossOrigin = 'anonymous',
    onCaptureError,
    messages: messagesProp,
    className,
    style,
    disabled,
    type,
    ...rest
  },
  ref,
) {
  const m = { ...DEFAULT_VIDEO_THUMBNAIL_MESSAGES, ...messagesProp };
  const [captured, setCaptured] = useState<string | null>(null);
  const [failed, setFailed] = useState(false);

  useEffect(() => {
    setCaptured(null);
    setFailed(false);
    if (thumbnailUrl || !src) return;
    const controller = new AbortController();
    captureVideoFrame(src, { time: captureTime, crossOrigin, signal: controller.signal })
      .then((url) => {
        if (!controller.signal.aborted) setCaptured(url);
      })
      .catch((error: Error) => {
        if (controller.signal.aborted) return;
        setFailed(true);
        onCaptureError?.(error);
      });
    return () => controller.abort();
    // onCaptureError is a notification; re-capturing when it changes identity is wasteful.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [src, thumbnailUrl, captureTime, crossOrigin]);

  const poster = thumbnailUrl || captured;
  const name = alt.trim() || m.fallbackAlt;
  const durationText = duration !== undefined ? formatVideoDuration(duration) : undefined;
  const rootStyle = { ...style, aspectRatio } as CSSProperties;
  const state = poster ? 'ready' : failed || !src ? 'placeholder' : 'loading';

  const body = (
    <>
      {poster ? (
        <img
          className="mn-video-thumbnail__image"
          src={poster}
          alt={onPlay ? '' : name}
          data-fit={fit}
          draggable={false}
        />
      ) : (
        <span
          className="mn-video-thumbnail__placeholder"
          role={onPlay ? undefined : 'img'}
          aria-label={onPlay ? undefined : name}
        >
          <PlayGlyph />
        </span>
      )}
      {showPlayIcon && poster ? (
        <span className="mn-video-thumbnail__play" aria-hidden="true">
          <PlayGlyph />
        </span>
      ) : null}
      {durationText ? (
        <span className="mn-video-thumbnail__duration" aria-hidden={onPlay ? true : undefined}>
          {durationText}
        </span>
      ) : null}
    </>
  );

  if (onPlay) {
    const label = durationText ? `${m.play(name)}, ${m.duration(durationText)}` : m.play(name);
    return (
      <button
        ref={ref as Ref<HTMLButtonElement>}
        type={type ?? 'button'}
        disabled={disabled}
        aria-label={label}
        {...rest}
        data-state={state}
        className={cx('mn-video-thumbnail', className)}
        style={rootStyle}
        onClick={onPlay}
      >
        {body}
      </button>
    );
  }
  return (
    <div
      ref={ref as Ref<HTMLDivElement>}
      {...rest}
      data-state={state}
      className={cx('mn-video-thumbnail', className)}
      style={rootStyle}
    >
      {body}
    </div>
  );
});
