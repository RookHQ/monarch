'use client';

import {
  forwardRef,
  useCallback,
  useEffect,
  useRef,
  useState,
  type HTMLAttributes,
  type KeyboardEvent,
  type Ref,
} from 'react';
import { cx } from '../../../internal/cx.js';
import { DashGlyph, PlusGlyph } from '../../../internal/icons.js';
import { Button, IconButton } from '../../actions/button/index.js';
import { CallIcon } from '../call-controls/index.js';
import { CallVideo } from '../call-tile/index.js';

/** Every string a screen share view shows or announces. */
export interface ScreenShareViewMessages {
  /** Visible presenter label. */
  presenting: (name: string) => string;
  /** Visible label when the local person is sharing. */
  youArePresenting: string;
  /** Accessible name of the shared screen region. */
  region: (name: string) => string;
  /** Accessible name of the controls group. */
  controls: string;
  /** Shown while there is no stream yet. */
  waiting: string;
  /** Shown instead of the picture when you are sharing (avoids the mirror tunnel). */
  localPreviewHidden: string;
  /** Button that shows your own share anyway. */
  showPreview: string;
  fit: string;
  zoomIn: string;
  zoomOut: string;
  /** Visible and spoken zoom level. */
  zoomLevel: (percent: number) => string;
  pictureInPicture: string;
  exitPictureInPicture: string;
  fullscreen: string;
  exitFullscreen: string;
  stopSharing: string;
}

export const SCREEN_SHARE_VIEW_MESSAGES: ScreenShareViewMessages = {
  presenting: (name) => `${name} is presenting`,
  youArePresenting: 'You are presenting',
  region: (name) => `Screen shared by ${name}`,
  controls: 'Shared screen controls',
  waiting: 'Waiting for the shared screen…',
  localPreviewHidden:
    'Your screen is being shared. The preview is hidden to avoid a mirror effect.',
  showPreview: 'Show preview',
  fit: 'Fit to view',
  zoomIn: 'Zoom in',
  zoomOut: 'Zoom out',
  zoomLevel: (percent) => `${percent}%`,
  pictureInPicture: 'Picture-in-picture',
  exitPictureInPicture: 'Exit picture-in-picture',
  fullscreen: 'Full screen',
  exitFullscreen: 'Exit full screen',
  stopSharing: 'Stop sharing',
};

/** Zoom steps of the shared screen; 1 is fit. */
export const SCREEN_SHARE_ZOOM_LEVELS = [1, 1.5, 2, 3] as const;

export interface ScreenShareViewProps extends Omit<HTMLAttributes<HTMLDivElement>, 'children'> {
  /** The shared screen. `null` while it is starting. Attached as `srcObject` after mount. */
  stream: MediaStream | null;
  /** Who is sharing. */
  presenterName: string;
  /** The local person is the one sharing: the picture is hidden by default and Stop sharing is offered. */
  isLocal?: boolean | undefined;
  /** Stops sharing (shown when `isLocal`). */
  onStopSharing?: (() => void) | undefined;
  /** Zoom factor (controlled). 1 fits the whole screen in the view. */
  zoom?: number | undefined;
  /** Initial zoom (uncontrolled). Default 1. */
  defaultZoom?: number | undefined;
  /** Called with the new zoom. */
  onZoomChange?: ((zoom: number) => void) | undefined;
  /** Hide the zoom, picture-in-picture and full-screen controls. */
  hideControls?: boolean | undefined;
  /** Translated strings. */
  messages?: Partial<ScreenShareViewMessages> | undefined;
}

type PipDocument = Document & {
  pictureInPictureEnabled?: boolean;
  pictureInPictureElement?: Element | null;
  exitPictureInPicture?: () => Promise<void>;
};
type PipVideo = HTMLVideoElement & { requestPictureInPicture?: () => Promise<unknown> };

function assignRef<T>(ref: Ref<T> | undefined, value: T | null) {
  if (typeof ref === 'function') ref(value);
  else if (ref) (ref as { current: T | null }).current = value;
}

/**
 * Shows a shared screen with who is presenting, fit and zoom (scroll to pan
 * when zoomed), picture-in-picture and full screen where the browser supports
 * them. When you are the presenter, your own picture is hidden by default to
 * avoid the mirror tunnel, and Stop sharing is offered.
 */
export const ScreenShareView = forwardRef<HTMLDivElement, ScreenShareViewProps>(
  function ScreenShareView(
    {
      stream,
      presenterName,
      isLocal = false,
      onStopSharing,
      zoom: zoomProp,
      defaultZoom = 1,
      onZoomChange,
      hideControls = false,
      messages: messagesProp,
      className,
      ...rest
    },
    ref,
  ) {
    const m = { ...SCREEN_SHARE_VIEW_MESSAGES, ...messagesProp };
    const rootRef = useRef<HTMLDivElement | null>(null);
    const videoRef = useRef<HTMLVideoElement | null>(null);
    const [innerZoom, setInnerZoom] = useState(defaultZoom);
    const zoom = zoomProp ?? innerZoom;
    const [showLocal, setShowLocal] = useState(false);
    const [support, setSupport] = useState({ pip: false, fullscreen: false });
    const [inPip, setInPip] = useState(false);
    const [inFullscreen, setInFullscreen] = useState(false);

    // Feature detection after mount, so the server and first client render match.
    useEffect(() => {
      const doc = document as PipDocument;
      setSupport({
        pip:
          !!doc.pictureInPictureEnabled &&
          typeof (HTMLVideoElement.prototype as PipVideo).requestPictureInPicture === 'function',
        fullscreen:
          typeof HTMLElement.prototype.requestFullscreen === 'function' &&
          !!document.fullscreenEnabled,
      });
      const onFs = () => setInFullscreen(document.fullscreenElement === rootRef.current);
      document.addEventListener('fullscreenchange', onFs);
      return () => document.removeEventListener('fullscreenchange', onFs);
    }, []);

    const setZoom = useCallback(
      (next: number) => {
        const levels = SCREEN_SHARE_ZOOM_LEVELS;
        const clamped = Math.min(levels[levels.length - 1] ?? 3, Math.max(1, next));
        if (zoomProp === undefined) setInnerZoom(clamped);
        onZoomChange?.(clamped);
      },
      [zoomProp, onZoomChange],
    );
    const step = (dir: 1 | -1) => {
      const levels = [...SCREEN_SHARE_ZOOM_LEVELS];
      const next =
        dir > 0
          ? levels.find((l) => l > zoom + 1e-6)
          : [...levels].reverse().find((l) => l < zoom - 1e-6);
      if (next !== undefined) setZoom(next);
    };

    const togglePip = async () => {
      const doc = document as PipDocument;
      const video = videoRef.current as PipVideo | null;
      try {
        if (doc.pictureInPictureElement) {
          await doc.exitPictureInPicture?.();
          setInPip(false);
        } else if (video?.requestPictureInPicture) {
          await video.requestPictureInPicture();
          setInPip(true);
          video.addEventListener('leavepictureinpicture', () => setInPip(false), { once: true });
        }
      } catch {
        setInPip(false);
      }
    };

    const toggleFullscreen = () => {
      if (document.fullscreenElement) void document.exitFullscreen?.().catch(() => undefined);
      else void rootRef.current?.requestFullscreen?.().catch(() => undefined);
    };

    const onKeyDown = (e: KeyboardEvent<HTMLDivElement>) => {
      // Ctrl/Cmd with +, - or 0 is the browser's own page zoom: leave it alone.
      if (e.ctrlKey || e.metaKey || e.altKey) return;
      if (e.key === '+' || e.key === '=') {
        e.preventDefault();
        step(1);
      } else if (e.key === '-') {
        e.preventDefault();
        step(-1);
      } else if (e.key === '0') {
        e.preventDefault();
        setZoom(1);
      }
    };

    const hidden = isLocal && !showLocal;
    const label = isLocal ? m.youArePresenting : m.presenting(presenterName);
    const percent = Math.round(zoom * 100);

    return (
      <div
        ref={(el) => {
          rootRef.current = el;
          assignRef(ref, el);
        }}
        {...rest}
        className={cx('mn-screen-share-view', className)}
        data-zoomed={zoom > 1 || undefined}
        data-local={isLocal || undefined}
        data-fullscreen={inFullscreen || undefined}
      >
        <div className="mn-screen-share-view__bar">
          <span className="mn-screen-share-view__presenter">
            <CallIcon name="screen" width={16} height={16} />
            <span className="mn-screen-share-view__presenter-name">{label}</span>
          </span>
          <div className="mn-screen-share-view__controls" role="group" aria-label={m.controls}>
            {!hideControls && !hidden && stream ? (
              <>
                <IconButton
                  size="sm"
                  label={m.zoomOut}
                  icon={<DashGlyph />}
                  disabled={zoom <= 1}
                  onClick={() => step(-1)}
                />
                <span className="mn-screen-share-view__zoom" aria-live="polite">
                  {m.zoomLevel(percent)}
                </span>
                <IconButton
                  size="sm"
                  label={m.zoomIn}
                  icon={<PlusGlyph />}
                  disabled={
                    zoom >= (SCREEN_SHARE_ZOOM_LEVELS[SCREEN_SHARE_ZOOM_LEVELS.length - 1] ?? 3)
                  }
                  onClick={() => step(1)}
                />
                <Button size="sm" variant="ghost" disabled={zoom === 1} onClick={() => setZoom(1)}>
                  {m.fit}
                </Button>
                {support.pip ? (
                  <IconButton
                    size="sm"
                    label={inPip ? m.exitPictureInPicture : m.pictureInPicture}
                    aria-pressed={inPip}
                    icon={<CallIcon name="pip" width={16} height={16} />}
                    onClick={() => void togglePip()}
                  />
                ) : null}
                {support.fullscreen ? (
                  <IconButton
                    size="sm"
                    label={inFullscreen ? m.exitFullscreen : m.fullscreen}
                    icon={
                      <CallIcon
                        name={inFullscreen ? 'fullscreen-exit' : 'fullscreen'}
                        width={16}
                        height={16}
                      />
                    }
                    onClick={toggleFullscreen}
                  />
                ) : null}
              </>
            ) : null}
            {isLocal && onStopSharing ? (
              <Button size="sm" variant="danger" onClick={onStopSharing}>
                {m.stopSharing}
              </Button>
            ) : null}
          </div>
        </div>
        {hidden ? (
          <div className="mn-screen-share-view__notice">
            <p>{m.localPreviewHidden}</p>
            {stream ? (
              <Button size="sm" variant="secondary" onClick={() => setShowLocal(true)}>
                {m.showPreview}
              </Button>
            ) : null}
          </div>
        ) : (
          // Scrollable when zoomed: focusable so keyboard users can pan with the arrow keys.
          // eslint-disable-next-line jsx-a11y/no-noninteractive-element-interactions
          <div
            className="mn-screen-share-view__viewport"
            role="region"
            aria-label={m.region(presenterName)}
            // eslint-disable-next-line jsx-a11y/no-noninteractive-tabindex
            tabIndex={0}
            onKeyDown={onKeyDown}
          >
            {stream ? (
              <CallVideo
                ref={videoRef}
                stream={stream}
                fit="contain"
                className="mn-screen-share-view__video"
                style={zoom > 1 ? { width: `${percent}%`, height: `${percent}%` } : undefined}
              />
            ) : (
              <p className="mn-screen-share-view__waiting">{m.waiting}</p>
            )}
          </div>
        )}
      </div>
    );
  },
);
