'use client';

import {
  forwardRef,
  useCallback,
  useEffect,
  useRef,
  useState,
  type HTMLAttributes,
  type Ref,
  type VideoHTMLAttributes,
} from 'react';
import { cx } from '../../../internal/cx.js';
import { Avatar } from '../../identity/avatar/index.js';
import { CallIcon } from '../call-controls/index.js';

/* ---------------------------------------------------------------- video */

export interface CallVideoProps extends Omit<VideoHTMLAttributes<HTMLVideoElement>, 'src'> {
  /** The stream to show. Attached as `srcObject` after mount. */
  stream: MediaStream | null;
  /** Mirror horizontally, for your own camera. */
  mirror?: boolean | undefined;
  /** `cover` crops to fill (cameras); `contain` letterboxes (shared screens). Default `cover`. */
  fit?: 'cover' | 'contain' | undefined;
}

function assignRef<T>(ref: Ref<T> | undefined, value: T | null) {
  if (typeof ref === 'function') ref(value);
  else if (ref) (ref as { current: T | null }).current = value;
}

/**
 * A muted, inline, autoplaying `<video>` for a `MediaStream`. Pictures only:
 * play the audio from a separate audio element so muting a tile never mutes
 * the person. Attaches the stream in an effect, so it is SSR-safe.
 */
export const CallVideo = forwardRef<HTMLVideoElement, CallVideoProps>(function CallVideo(
  { stream, mirror = false, fit = 'cover', className, ...rest },
  ref,
) {
  const local = useRef<HTMLVideoElement | null>(null);
  useEffect(() => {
    const el = local.current;
    if (el && el.srcObject !== stream) el.srcObject = stream;
  }, [stream]);
  const setRef = useCallback(
    (el: HTMLVideoElement | null) => {
      local.current = el;
      assignRef(ref, el);
    },
    [ref],
  );
  return (
    <video
      ref={setRef}
      autoPlay
      playsInline
      muted
      {...rest}
      data-mirror={mirror || undefined}
      data-fit={fit}
      className={cx('mn-call-video', className)}
    />
  );
});

/* -------------------------------------------------------------- quality */

/** Connection quality grade, from the product's stats. */
export type CallQuality = 'good' | 'fair' | 'poor' | 'unknown';

/** Visible and announced strings for the connection-quality bars. */
export interface CallQualityMessages {
  good: string;
  fair: string;
  poor: string;
}

const QUALITY_MESSAGES: CallQualityMessages = {
  good: 'Good connection',
  fair: 'Unstable connection',
  poor: 'Poor connection',
};

const BARS: Record<Exclude<CallQuality, 'unknown'>, number> = { good: 3, fair: 2, poor: 1 };

export interface CallQualityBarsProps extends Omit<HTMLAttributes<HTMLSpanElement>, 'children'> {
  /** The grade to show. `unknown` renders nothing. */
  quality: CallQuality;
  messages?: Partial<CallQualityMessages> | undefined;
}

/** Three signal bars with an accessible label ("Poor connection"). */
export function CallQualityBars({ quality, messages, className, ...rest }: CallQualityBarsProps) {
  if (quality === 'unknown') return null;
  const label = { ...QUALITY_MESSAGES, ...messages }[quality];
  const bars = BARS[quality];
  return (
    <span
      role="img"
      aria-label={label}
      title={label}
      {...rest}
      data-quality={quality}
      className={cx('mn-call-quality', className)}
    >
      {[1, 2, 3].map((i) => (
        <span key={i} className="mn-call-quality__bar" data-on={i <= bars || undefined} />
      ))}
    </span>
  );
}

/* ----------------------------------------------------------------- tile */

/** A short-lived reaction. A new `at` replays it. */
export interface CallReaction {
  emoji: string;
  /** When it was sent (ms since epoch). Reactions older than 4 s are not shown. */
  at: number;
}

/** Visible and announced strings. Override them to translate. */
export interface CallTileMessages extends CallQualityMessages {
  you: (name: string) => string;
  muted: string;
  sharingScreen: string;
  handRaised: string;
  handRaisedBy: (name: string) => string;
  pin: (name: string) => string;
  unpin: (name: string) => string;
  fullscreen: (name: string) => string;
  reacted: (name: string, emoji: string) => string;
}

export const DEFAULT_CALL_TILE_MESSAGES: CallTileMessages = {
  ...QUALITY_MESSAGES,
  you: (name) => `${name} (you)`,
  muted: 'Muted',
  sharingScreen: 'Sharing screen',
  handRaised: 'Hand raised',
  handRaisedBy: (name) => `${name} raised a hand`,
  pin: (name) => `Pin ${name}`,
  unpin: (name) => `Unpin ${name}`,
  fullscreen: (name) => `Show ${name} full screen`,
  reacted: (name, emoji) => `${name} reacted ${emoji}`,
};

export interface CallTileProps extends Omit<HTMLAttributes<HTMLDivElement>, 'children'> {
  /** The participant's display name. Names the tile and its controls. */
  name: string;
  /** Picture shown when there is no video. */
  avatarUrl?: string | null | undefined;
  /** The participant's media. Only its video is used. */
  stream?: MediaStream | null | undefined;
  /**
   * Show the video. Default: true when the stream has a live video track.
   * Pass false while connecting, in low data mode or when the camera is off.
   */
  showVideo?: boolean | undefined;
  /** This is you: adds "(you)" and mirrors your camera. */
  self?: boolean | undefined;
  /** Microphone is off: shows a muted icon. */
  micOff?: boolean | undefined;
  /** Currently speaking: highlights the tile. */
  speaking?: boolean | undefined;
  /** Hand raised: shows a badge. */
  handRaised?: boolean | undefined;
  /** The stream is a shared screen: letterboxed, never mirrored. */
  screen?: boolean | undefined;
  /** Short status in the name bar, for example "Reconnecting…" or "Presenting". */
  status?: string | undefined;
  /** Colour of `status`. */
  statusTone?: 'neutral' | 'warning' | 'danger' | undefined;
  /** Connection quality bars. */
  quality?: CallQuality | undefined;
  /** A reaction that floats up for three seconds. */
  reaction?: CallReaction | null | undefined;
  /** Pinned to the main spot. */
  pinned?: boolean | undefined;
  /** Shows a Pin / Unpin button that calls this with the next state. */
  onPinnedChange?: ((pinned: boolean) => void) | undefined;
  /**
   * Shows a full-screen button. `true` uses the Fullscreen API on the tile;
   * a function replaces that. Default true for `normal` and `large` tiles.
   */
  fullscreen?: boolean | (() => void) | undefined;
  /** `small` is the filmstrip size: smaller avatar, no hover actions. Default `normal`. */
  size?: 'small' | 'normal' | 'large' | undefined;
  /** Receives the `<video>` element, for example for picture-in-picture. */
  videoRef?: Ref<HTMLVideoElement> | undefined;
  /** Override visible and announced strings. */
  messages?: Partial<CallTileMessages> | undefined;
}

function hasLiveVideo(stream: MediaStream | null | undefined) {
  if (!stream || typeof stream.getVideoTracks !== 'function') return false;
  return stream.getVideoTracks().some((t) => t.readyState === 'live');
}

/**
 * One participant in a call: their video (or avatar), name, microphone,
 * hand, connection and reactions, with Pin and full-screen actions.
 */
export const CallTile = forwardRef<HTMLDivElement, CallTileProps>(function CallTile(
  {
    name,
    avatarUrl,
    stream = null,
    showVideo,
    self = false,
    micOff = false,
    speaking = false,
    handRaised = false,
    screen = false,
    status,
    statusTone = 'neutral',
    quality,
    reaction,
    pinned = false,
    onPinnedChange,
    fullscreen,
    size = 'normal',
    videoRef,
    messages: messagesProp,
    className,
    ...rest
  },
  ref,
) {
  const m = { ...DEFAULT_CALL_TILE_MESSAGES, ...messagesProp };
  const box = useRef<HTMLDivElement | null>(null);
  const [floating, setFloating] = useState<CallReaction | null>(null);

  useEffect(() => {
    if (!reaction || Date.now() - reaction.at > 4000) return;
    setFloating(reaction);
    const timer = window.setTimeout(() => setFloating(null), 3000);
    return () => window.clearTimeout(timer);
  }, [reaction]);

  const video = showVideo ?? hasLiveVideo(stream);
  const label = self ? m.you(name) : name;
  const canFullscreen = (fullscreen ?? size !== 'small') !== false;

  const goFullscreen = () => {
    if (typeof fullscreen === 'function') {
      fullscreen();
      return;
    }
    const el = box.current;
    if (!el || typeof document === 'undefined') return;
    if (document.fullscreenElement) void document.exitFullscreen?.().catch(() => undefined);
    else void el.requestFullscreen?.().catch(() => undefined);
  };

  return (
    <div
      ref={(el) => {
        box.current = el;
        assignRef(ref, el);
      }}
      role="group"
      aria-label={label}
      {...rest}
      data-size={size}
      data-speaking={speaking || undefined}
      data-video={video || undefined}
      data-pinned={pinned || undefined}
      data-screen={screen || undefined}
      className={cx('mn-call-tile', className)}
    >
      {video && stream ? (
        <CallVideo
          ref={videoRef}
          stream={stream}
          mirror={self && !screen}
          fit={screen ? 'contain' : 'cover'}
          className="mn-call-tile__video"
        />
      ) : (
        <div className="mn-call-tile__avatar">
          <Avatar
            name={name}
            src={avatarUrl ?? null}
            size={size === 'small' ? 'md' : 'xl'}
            decorative
          />
        </div>
      )}

      {floating && (
        <span key={floating.at} className="mn-call-tile__reaction" aria-hidden="true">
          {floating.emoji}
        </span>
      )}
      <span className="mn-visually-hidden" aria-live="polite">
        {floating ? m.reacted(name, floating.emoji) : ''}
      </span>

      {handRaised && (
        <span className="mn-call-tile__hand" role="img" aria-label={m.handRaisedBy(name)}>
          <CallIcon name="hand" width={14} height={14} />
          <span aria-hidden="true">{m.handRaised}</span>
        </span>
      )}

      {size !== 'small' && (onPinnedChange || canFullscreen) && (
        <div className="mn-call-tile__actions">
          {onPinnedChange && (
            <button
              type="button"
              className="mn-call-tile__action"
              aria-label={pinned ? m.unpin(name) : m.pin(name)}
              aria-pressed={pinned}
              title={pinned ? m.unpin(name) : m.pin(name)}
              onClick={() => onPinnedChange(!pinned)}
            >
              <CallIcon name={pinned ? 'pin-off' : 'pin'} width={16} height={16} />
            </button>
          )}
          {canFullscreen && (
            <button
              type="button"
              className="mn-call-tile__action"
              aria-label={m.fullscreen(name)}
              title={m.fullscreen(name)}
              onClick={goFullscreen}
            >
              <CallIcon name="expand" width={16} height={16} />
            </button>
          )}
        </div>
      )}

      <div className="mn-call-tile__bar">
        {micOff && (
          <span role="img" aria-label={m.muted} className="mn-call-tile__muted">
            <CallIcon name="mic-off" width={14} height={14} />
          </span>
        )}
        {screen && (
          <span role="img" aria-label={m.sharingScreen}>
            <CallIcon name="screen" width={14} height={14} />
          </span>
        )}
        <span className="mn-call-tile__name">{label}</span>
        {(status || (quality && quality !== 'unknown')) && (
          <span className="mn-call-tile__meta">
            {status && (
              <span className="mn-call-tile__status" data-tone={statusTone}>
                {status}
              </span>
            )}
            {quality && <CallQualityBars quality={quality} messages={m} />}
          </span>
        )}
      </div>
    </div>
  );
});
