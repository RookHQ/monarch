'use client';

import {
  Fragment,
  useCallback,
  useRef,
  useState,
  type CSSProperties,
  type KeyboardEvent,
  type ReactElement,
  type ReactNode,
} from 'react';
import * as DialogPrimitive from '@radix-ui/react-dialog';
import { cx } from '../../../internal/cx.js';
import {
  ArrowDownGlyph,
  ChevronRightGlyph,
  CloseGlyph,
  DashGlyph,
  PlusGlyph,
} from '../../../internal/icons.js';
import { isSafeImageSrc } from '../../../internal/sanitize.js';
import { useControllable } from '../../../internal/use-controllable.js';
import { useDirection } from '../../../internal/direction.js';
import { stagedDismiss, useOverlayStage } from '../../../internal/overlay-stage.js';

/** One image or video in a Lightbox. */
export interface LightboxItem {
  /** Address of the full-size media. Must be http(s), a same-origin path or `blob:`; anything else is not loaded. */
  src: string;
  /**
   * Text alternative: what the image shows, for people who cannot see it
   * ("Whiteboard sketch of the checkout flow"). Required. For a video it
   * becomes the player's accessible name.
   */
  alt: string;
  /** Visible caption under the media. */
  caption?: ReactNode;
  /** `image` (default) or `video`. Videos play in the native player and cannot be zoomed. */
  kind?: 'image' | 'video' | undefined;
  /** Address of a file to download (for example the original), shown as a Download link. Same URL rules as `src`. */
  downloadUrl?: string | undefined;
  /** Caption tracks (WebVTT) for a video. Each `src` follows the same URL rules as `src`. */
  tracks?:
    { src: string; srcLang: string; label: string; kind?: 'captions' | 'subtitles' }[] | undefined;
  /** Intrinsic size, so the layout does not jump while the image loads. */
  width?: number | undefined;
  height?: number | undefined;
}

/** The words Lightbox shows and speaks. Override them to translate. */
export interface LightboxMessages {
  /** The viewer's accessible name. Default "Media viewer". */
  title: string;
  /** Default "Previous". */
  previous: string;
  /** Default "Next". */
  next: string;
  /** Default "Zoom in". */
  zoomIn: string;
  /** Default "Zoom out". */
  zoomOut: string;
  /** Accessible name of the button that shows the zoom level and resets it, followed by the level ("Reset zoom, 150%"). Default "Reset zoom". */
  resetZoom: string;
  /** Default "Close". */
  close: string;
  /** Default "Download". */
  download: string;
  /** Where the item sits. Default "2 of 5". */
  position: (index: number, total: number) => string;
  /** Shown instead of media whose address is not allowed. */
  blocked: string;
}

const DEFAULT_MESSAGES: LightboxMessages = {
  title: 'Media viewer',
  previous: 'Previous',
  next: 'Next',
  zoomIn: 'Zoom in',
  zoomOut: 'Zoom out',
  resetZoom: 'Reset zoom',
  close: 'Close',
  download: 'Download',
  position: (index, total) => `${index} of ${total}`,
  blocked: 'This file can’t be shown.',
};

export interface LightboxProps {
  /** The images and videos, in order. */
  items: LightboxItem[];
  /** The item shown (controlled, 0-based). Pair with `onIndexChange`. */
  index?: number | undefined;
  /** The item shown first (uncontrolled). Default 0. */
  defaultIndex?: number | undefined;
  /** Called when the previous/next buttons or keys change the item. */
  onIndexChange?: ((index: number) => void) | undefined;
  /** Whether the viewer is open (controlled). Pair with `onOpenChange`. */
  open?: boolean | undefined;
  /** Whether the viewer starts open (uncontrolled). Default `false`. */
  defaultOpen?: boolean | undefined;
  /** Called when the viewer opens or closes. */
  onOpenChange?: ((open: boolean) => void) | undefined;
  /** An element that opens the viewer, such as a thumbnail button. */
  trigger?: ReactElement | undefined;
  /** Wrap from the last item to the first and back. Default `false`. */
  loop?: boolean | undefined;
  /** Largest zoom factor. Default 4. */
  maxZoom?: number | undefined;
  /** Words to show and speak; merged over the English defaults. */
  messages?: Partial<LightboxMessages> | undefined;
  /** Class on the viewer's content element. */
  className?: string | undefined;
}

const ZOOM_STEP = 0.5;

function clampIndex(i: number, count: number) {
  return Math.min(Math.max(i, 0), Math.max(count - 1, 0));
}

/**
 * A full-screen viewer for images and videos: previous and next, zoom,
 * captions and a position count. It is a modal dialog (Radix Dialog), so focus
 * is trapped, Escape closes it and focus returns to the thumbnail that opened
 * it. Every item needs alt text.
 */
export function Lightbox({
  items,
  index: indexProp,
  defaultIndex = 0,
  onIndexChange,
  open: openProp,
  defaultOpen = false,
  onOpenChange,
  trigger,
  loop = false,
  maxZoom = 4,
  messages: messagesProp,
  className,
}: LightboxProps) {
  const readDir = useDirection();
  const staged = useOverlayStage();
  const messages = { ...DEFAULT_MESSAGES, ...messagesProp };
  const [open, setOpen] = useControllable(openProp, defaultOpen, onOpenChange);
  const [rawIndex, setIndex] = useControllable(indexProp, defaultIndex, onIndexChange);
  // Zoom belongs to one item: stepping to another item starts unzoomed.
  const [zoomState, setZoomState] = useState({ index: -1, zoom: 1 });
  const contentRef = useRef<HTMLDivElement>(null);
  const count = items.length;
  const index = clampIndex(rawIndex, count);
  const item = items[index];
  const isImage = item !== undefined && (item.kind ?? 'image') === 'image';
  const safe = item !== undefined && isSafeImageSrc(item.src);
  const zoomable = isImage && safe;
  const limit = Math.max(1, maxZoom);
  const zoom = zoomState.index === index ? zoomState.zoom : 1;
  const setZoom = (z: number) => setZoomState({ index, zoom: Math.min(limit, Math.max(1, z)) });
  const handleOpenChange = (next: boolean) => {
    setZoomState({ index: -1, zoom: 1 });
    setOpen(next);
  };

  const go = useCallback(
    (delta: number) => {
      if (count < 2) return;
      let next = index + delta;
      if (loop) next = (next + count) % count;
      next = clampIndex(next, count);
      if (next !== index) setIndex(next);
    },
    [count, index, loop, setIndex],
  );
  const canPrevious = count > 1 && (loop || index > 0);
  const canNext = count > 1 && (loop || index < count - 1);

  const handleKeyDown = (event: KeyboardEvent<HTMLDivElement>) => {
    if (event.defaultPrevented || event.altKey || event.ctrlKey || event.metaKey) return;
    const target = event.target as HTMLElement;
    // The native video player keeps its own keys.
    if (target.tagName === 'VIDEO') return;
    const rtl = readDir(contentRef.current) === 'rtl';
    switch (event.key) {
      case 'ArrowLeft':
      case 'ArrowRight': {
        // While zoomed in, arrows scroll the image instead.
        if (zoom > 1) return;
        const forward = (event.key === 'ArrowRight') !== rtl;
        go(forward ? 1 : -1);
        break;
      }
      case 'Home':
        setIndex(0);
        break;
      case 'End':
        setIndex(Math.max(count - 1, 0));
        break;
      case '+':
      case '=':
        if (!zoomable) return;
        setZoom(zoom + ZOOM_STEP);
        break;
      case '-':
        if (!zoomable) return;
        setZoom(zoom - ZOOM_STEP);
        break;
      case '0':
        if (!zoomable) return;
        setZoom(1);
        break;
      default:
        return;
    }
    event.preventDefault();
  };

  const download =
    item?.downloadUrl !== undefined && isSafeImageSrc(item.downloadUrl) ? item.downloadUrl : null;

  let media: ReactNode = null;
  if (item && !safe) {
    media = <p className="mn-lightbox__blocked">{messages.blocked}</p>;
  } else if (item && isImage) {
    media = (
      <img
        key={item.src}
        className="mn-lightbox__image"
        src={item.src}
        alt={item.alt}
        width={item.width}
        height={item.height}
        draggable={false}
      />
    );
  } else if (item) {
    media = (
      // Captions come from `item.tracks`; the item decides whether it has any.
      // eslint-disable-next-line jsx-a11y/media-has-caption
      <video
        key={item.src}
        className="mn-lightbox__video"
        src={item.src}
        aria-label={item.alt}
        controls
        playsInline
        preload="metadata"
        width={item.width}
        height={item.height}
      >
        {item.tracks
          ?.filter((t) => isSafeImageSrc(t.src))
          .map((t) => (
            <track
              key={t.src}
              kind={t.kind ?? 'captions'}
              src={t.src}
              srcLang={t.srcLang}
              label={t.label}
            />
          ))}
      </video>
    );
  }

  const Portal = staged ? Fragment : DialogPrimitive.Portal;
  return (
    <DialogPrimitive.Root
      open={open}
      onOpenChange={handleOpenChange}
      {...(staged ? { modal: false } : {})}
    >
      {trigger ? <DialogPrimitive.Trigger asChild>{trigger}</DialogPrimitive.Trigger> : null}
      <Portal>
        <DialogPrimitive.Overlay className="mn-overlay" />
        <DialogPrimitive.Content
          ref={contentRef}
          {...(staged
            ? {
                ...stagedDismiss(staged, {}),
                onOpenAutoFocus: (event: Event) => event.preventDefault(),
              }
            : {})}
          className={cx('mn-lightbox', className)}
          onKeyDown={handleKeyDown}
          data-zoomed={zoom > 1 || undefined}
          aria-describedby={undefined}
        >
          <DialogPrimitive.Title className="mn-visually-hidden">
            {messages.title}
          </DialogPrimitive.Title>
          <div className="mn-lightbox__toolbar">
            {count > 1 ? (
              // Polite, so stepping announces the position and the new item's alt text.
              <span className="mn-lightbox__position" aria-live="polite" aria-atomic="true">
                {messages.position(index + 1, count)}
                {item ? <span className="mn-visually-hidden">: {item.alt}</span> : null}
              </span>
            ) : (
              <span />
            )}
            <div className="mn-lightbox__tools">
              {zoomable ? (
                <>
                  <button
                    type="button"
                    className="mn-lightbox__button"
                    aria-label={messages.zoomOut}
                    aria-keyshortcuts="-"
                    aria-disabled={zoom <= 1 || undefined}
                    onClick={() => setZoom(zoom - ZOOM_STEP)}
                  >
                    <DashGlyph />
                  </button>
                  <button
                    type="button"
                    className="mn-lightbox__button mn-lightbox__zoom-level"
                    // The name holds the level it shows, so speech input can say what is on screen.
                    aria-label={`${messages.resetZoom}, ${Math.round(zoom * 100)}%`}
                    aria-keyshortcuts="0"
                    aria-disabled={zoom === 1 || undefined}
                    onClick={() => setZoom(1)}
                  >
                    <span aria-hidden="true">{Math.round(zoom * 100)}%</span>
                  </button>
                  <button
                    type="button"
                    className="mn-lightbox__button"
                    aria-label={messages.zoomIn}
                    aria-keyshortcuts="+"
                    aria-disabled={zoom >= limit || undefined}
                    onClick={() => setZoom(zoom + ZOOM_STEP)}
                  >
                    <PlusGlyph />
                  </button>
                </>
              ) : null}
              {download ? (
                <a
                  className="mn-lightbox__button"
                  href={download}
                  download
                  aria-label={messages.download}
                >
                  <ArrowDownGlyph />
                </a>
              ) : null}
              <DialogPrimitive.Close className="mn-lightbox__button" aria-label={messages.close}>
                <CloseGlyph />
              </DialogPrimitive.Close>
            </div>
          </div>
          <div
            className="mn-lightbox__stage"
            style={{ '--_zoom': zoom } as CSSProperties}
            // Scrollable with the keyboard while zoomed in.
            tabIndex={zoom > 1 ? 0 : undefined}
            role={zoom > 1 ? 'group' : undefined}
            aria-label={zoom > 1 ? item?.alt : undefined}
          >
            {media}
          </div>
          {count > 1 ? (
            <>
              <button
                type="button"
                className="mn-lightbox__button mn-lightbox__nav"
                data-direction="previous"
                aria-label={messages.previous}
                aria-disabled={!canPrevious || undefined}
                onClick={() => go(-1)}
              >
                <ChevronRightGlyph />
              </button>
              <button
                type="button"
                className="mn-lightbox__button mn-lightbox__nav"
                data-direction="next"
                aria-label={messages.next}
                aria-disabled={!canNext || undefined}
                onClick={() => go(1)}
              >
                <ChevronRightGlyph />
              </button>
            </>
          ) : null}
          {item?.caption ? <div className="mn-lightbox__caption">{item.caption}</div> : null}
        </DialogPrimitive.Content>
      </Portal>
    </DialogPrimitive.Root>
  );
}
