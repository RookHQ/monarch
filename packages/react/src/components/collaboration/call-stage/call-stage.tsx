'use client';

import {
  forwardRef,
  useEffect,
  useId,
  useState,
  type CSSProperties,
  type HTMLAttributes,
  type ReactNode,
} from 'react';
import { cx } from '../../../internal/cx.js';
import { CallIcon, type CallViewMode } from '../call-controls/index.js';
import { CallTile, type CallTileMessages, type CallTileProps } from '../call-tile/index.js';

/* ---------------------------------------------------------------- timer */

const pad = (n: number) => String(n).padStart(2, '0');

/** Formats elapsed milliseconds as `mm:ss`, or `h:mm:ss` after an hour. */
export function formatCallDuration(ms: number): string {
  const s = Math.max(0, Math.floor(ms / 1000));
  const h = Math.floor(s / 3600);
  const m = Math.floor((s % 3600) / 60);
  return h ? `${h}:${pad(m)}:${pad(s % 60)}` : `${pad(m)}:${pad(s % 60)}`;
}

const toMs = (value: string | number | Date) =>
  value instanceof Date ? value.getTime() : typeof value === 'number' ? value : Date.parse(value);

export interface CallTimerProps extends Omit<HTMLAttributes<HTMLElement>, 'children'> {
  /** When the call started (ISO string, ms since epoch or Date). */
  since: string | number | Date;
  /** Accessible name. Default "Call duration". */
  label?: string | undefined;
}

/**
 * A ticking call duration (`04:12`). It is a `timer`, so screen readers do not
 * announce every second. Renders `00:00` on the server and starts ticking
 * after hydration.
 */
export const CallTimer = forwardRef<HTMLTimeElement, CallTimerProps>(function CallTimer(
  { since, label = 'Call duration', className, ...rest },
  ref,
) {
  const start = toMs(since);
  const [now, setNow] = useState<number | null>(null);
  useEffect(() => {
    setNow(Date.now());
    const timer = window.setInterval(() => setNow(Date.now()), 1000);
    return () => window.clearInterval(timer);
  }, []);
  const iso = Number.isFinite(start) ? new Date(start).toISOString() : undefined;
  return (
    <time
      ref={ref}
      role="timer"
      aria-label={label}
      dateTime={iso}
      {...rest}
      className={cx('mn-call-timer', className)}
    >
      {formatCallDuration(now === null || !Number.isFinite(start) ? 0 : now - start)}
    </time>
  );
});

/* --------------------------------------------------------------- layout */

/** Where tiles go: an even grid, or one main tile with a filmstrip. */
export type CallLayout =
  | { kind: 'grid'; ids: string[]; cols: number }
  | { kind: 'spotlight'; mainId: string; strip: string[] };

export interface CallLayoutInput {
  /** Your participant id, if you are in the list. */
  selfId?: string | null | undefined;
  /** Every participant id in display order (yourself included). */
  ids: string[];
  mode: CallViewMode;
  pinnedId?: string | null | undefined;
  /** Whoever is sharing a screen. */
  sharerId?: string | null | undefined;
  activeSpeakerId?: string | null | undefined;
}

/** Grid columns for a number of tiles: 1, 2 up to four people, then 3. */
export function callGridColumns(count: number): number {
  if (count <= 1) return 1;
  if (count <= 4) return 2;
  return 3;
}

/**
 * Chooses the layout. A pin wins; then someone else's shared screen; then, in
 * speaker mode, the active speaker (never yourself); then, in automatic mode
 * with one other person, that person large and you small. Otherwise a grid.
 * Pure, so products can use it to decide how much video to request.
 */
export function chooseCallLayout({
  selfId,
  ids,
  mode,
  pinnedId,
  sharerId,
  activeSpeakerId,
}: CallLayoutInput): CallLayout {
  const others = ids.filter((id) => id !== selfId);
  const main =
    (pinnedId && ids.includes(pinnedId) ? pinnedId : null) ??
    (sharerId && sharerId !== selfId && others.includes(sharerId) ? sharerId : null) ??
    (mode === 'speaker' && others.length > 0
      ? activeSpeakerId && others.includes(activeSpeakerId)
        ? activeSpeakerId
        : (others[0] ?? null)
      : null) ??
    (mode === 'auto' && others.length === 1 ? (others[0] ?? null) : null);
  if (main) return { kind: 'spotlight', mainId: main, strip: ids.filter((id) => id !== main) };
  return { kind: 'grid', ids, cols: callGridColumns(ids.length) };
}

/* ---------------------------------------------------------------- stage */

/** One person on the call, as CallStage shows them. */
export interface CallParticipant extends Pick<
  CallTileProps,
  | 'name'
  | 'avatarUrl'
  | 'stream'
  | 'showVideo'
  | 'self'
  | 'micOff'
  | 'speaking'
  | 'handRaised'
  | 'screen'
  | 'status'
  | 'statusTone'
  | 'quality'
  | 'reaction'
> {
  /** Stable id: used for layout, pinning and keys. */
  id: string;
}

/** Where the stage is shown. */
export type CallSurface = 'stage' | 'docked' | 'pip';

/** Visible and announced strings. Override them to translate. */
export interface CallStageMessages extends CallTileMessages {
  region: string;
  duration: string;
  waiting: string;
  inCall: (count: number) => string;
  lowData: string;
  reconnecting: string;
  mutedHint: string;
  pictureInPicture: string;
  shrink: string;
  expand: string;
  minimise: string;
}

export const DEFAULT_CALL_STAGE_MESSAGES: Omit<CallStageMessages, keyof CallTileMessages> = {
  region: 'Call',
  duration: 'Call duration',
  waiting: 'Waiting for others…',
  inCall: (count) => `${count} in call`,
  lowData: 'Low data',
  reconnecting: 'You’re offline. Trying to reconnect…',
  mutedHint: 'You’re muted. Press M or hold Space to talk.',
  pictureInPicture: 'Picture-in-picture',
  shrink: 'Shrink call to the corner',
  expand: 'Expand call',
  minimise: 'Minimise call',
};

export interface CallStageProps extends Omit<HTMLAttributes<HTMLElement>, 'title'> {
  /** Everyone on the call, yourself included (`self: true`), in display order. */
  participants: CallParticipant[];
  /** Heading, for example the conversation or meeting name. Also names the region. */
  title?: ReactNode;
  /** When the call started; shows a running timer. */
  startedAt?: string | number | Date | undefined;
  /** Layout mode. Default `auto`. */
  mode?: CallViewMode | undefined;
  /** The pinned participant, shown large. */
  pinnedId?: string | null | undefined;
  /** Shows Pin buttons on the tiles (on the `stage` surface) and receives the next pin. */
  onPinnedIdChange?: ((id: string | null) => void) | undefined;
  /** Who is talking, for speaker mode. */
  activeSpeakerId?: string | null | undefined;
  /** Replaces the automatic layout from `chooseCallLayout`. */
  layout?: CallLayout | undefined;
  /**
   * `stage` fills its container, `docked` is a compact corner panel, `pip` a
   * picture-in-picture window. Compact surfaces cap the grid at two columns
   * and hide tile actions. Default `stage`.
   */
  surface?: CallSurface | undefined;
  /** Shows Shrink (stage), Expand and Minimise (docked) buttons. */
  onSurfaceChange?: ((surface: 'stage' | 'docked' | 'mini') => void) | undefined;
  /** Shows a picture-in-picture button. */
  onPictureInPicture?: (() => void) | undefined;
  /** Shows a "Low data" badge. */
  lowData?: boolean | undefined;
  /** Shows the offline notice while the connection recovers. */
  reconnecting?: boolean | undefined;
  /** A problem to show as an alert, for example a connection that failed. */
  error?: ReactNode;
  /** Shows the "You're muted" hint, when the product detects speech while muted. */
  speakingWhileMuted?: boolean | undefined;
  /** The controls row, usually `<CallControls … />`. */
  controls?: ReactNode;
  /** Receives each tile's `<video>` element, for picture-in-picture. */
  onVideoElement?: ((id: string, el: HTMLVideoElement | null) => void) | undefined;
  /** Override visible and announced strings. */
  messages?: Partial<CallStageMessages> | undefined;
}

/**
 * The call itself: a header with the title, timer and head count, everyone's
 * tiles in a grid or spotlight layout, status notices and the controls. It
 * takes MediaStreams and callbacks only; signalling stays in the product.
 */
export const CallStage = forwardRef<HTMLElement, CallStageProps>(function CallStage(
  {
    participants,
    title,
    startedAt,
    mode = 'auto',
    pinnedId = null,
    onPinnedIdChange,
    activeSpeakerId = null,
    layout: layoutProp,
    surface = 'stage',
    onSurfaceChange,
    onPictureInPicture,
    lowData = false,
    reconnecting = false,
    error,
    speakingWhileMuted = false,
    controls,
    onVideoElement,
    messages: messagesProp,
    className,
    ...rest
  },
  ref,
) {
  const m = { ...DEFAULT_CALL_STAGE_MESSAGES, ...messagesProp } as CallStageMessages;
  const titleId = useId();
  const compact = surface !== 'stage';
  const byId = new Map(participants.map((p) => [p.id, p]));
  const self = participants.find((p) => p.self);
  const layout =
    layoutProp ??
    chooseCallLayout({
      selfId: self?.id,
      ids: participants.map((p) => p.id),
      mode,
      pinnedId,
      sharerId: participants.find((p) => p.screen)?.id,
      activeSpeakerId,
    });
  const others = participants.filter((p) => !p.self).length;

  const tile = (id: string, size: 'large' | 'normal' | 'small') => {
    const p = byId.get(id);
    if (!p) return null;
    const { id: _id, ...person } = p;
    return (
      <CallTile
        key={id}
        {...person}
        size={size}
        pinned={pinnedId === id}
        onPinnedChange={
          compact || !onPinnedIdChange ? undefined : (next) => onPinnedIdChange(next ? id : null)
        }
        fullscreen={compact ? false : undefined}
        videoRef={onVideoElement ? (el) => onVideoElement(id, el) : undefined}
        messages={messagesProp}
      />
    );
  };

  const cols = layout.kind === 'grid' ? (compact ? Math.min(layout.cols, 2) : layout.cols) : 1;

  return (
    <section
      ref={ref}
      aria-labelledby={title ? titleId : undefined}
      aria-label={title ? undefined : m.region}
      {...rest}
      data-surface={surface}
      data-layout={layout.kind}
      className={cx('mn-call-stage', className)}
    >
      <header className="mn-call-stage__header">
        <div className="mn-call-stage__heading">
          {title ? (
            <h2 id={titleId} className="mn-call-stage__title">
              {title}
            </h2>
          ) : null}
          <p className="mn-call-stage__meta">
            {startedAt !== undefined && (
              <>
                <CallTimer since={startedAt} label={m.duration} />
                <span aria-hidden="true">·</span>
              </>
            )}
            <span aria-live="polite">
              {others === 0 ? m.waiting : m.inCall(participants.length)}
            </span>
            {lowData && <span className="mn-call-stage__badge">{m.lowData}</span>}
          </p>
        </div>
        <div className="mn-call-stage__tools">
          {surface !== 'pip' && onPictureInPicture && (
            <button
              type="button"
              className="mn-call-stage__tool"
              aria-label={m.pictureInPicture}
              title={m.pictureInPicture}
              onClick={onPictureInPicture}
            >
              <CallIcon name="pip" width={16} height={16} />
            </button>
          )}
          {surface === 'stage' && onSurfaceChange && (
            <button
              type="button"
              className="mn-call-stage__tool"
              aria-label={m.shrink}
              title={m.shrink}
              onClick={() => onSurfaceChange('docked')}
            >
              <CallIcon name="shrink" width={16} height={16} />
            </button>
          )}
          {surface === 'docked' && onSurfaceChange && (
            <>
              <button
                type="button"
                className="mn-call-stage__tool"
                aria-label={m.expand}
                title={m.expand}
                onClick={() => onSurfaceChange('stage')}
              >
                <CallIcon name="expand" width={16} height={16} />
              </button>
              <button
                type="button"
                className="mn-call-stage__tool"
                aria-label={m.minimise}
                title={m.minimise}
                onClick={() => onSurfaceChange('mini')}
              >
                <CallIcon name="chevron-down" width={16} height={16} />
              </button>
            </>
          )}
        </div>
      </header>

      {reconnecting && (
        <p className="mn-call-stage__notice" data-tone="warning" role="status">
          <CallIcon name="offline" width={14} height={14} />
          {m.reconnecting}
        </p>
      )}
      {error ? (
        <div className="mn-call-stage__notice" data-tone="danger" role="alert">
          {error}
        </div>
      ) : null}

      <div className="mn-call-stage__body">
        {layout.kind === 'grid' ? (
          <div className="mn-call-stage__grid" style={{ '--_cols': cols } as CSSProperties}>
            {layout.ids.map((id) => tile(id, 'normal'))}
          </div>
        ) : (
          <div className="mn-call-stage__spotlight">
            <div className="mn-call-stage__main">{tile(layout.mainId, 'large')}</div>
            {layout.strip.length > 0 && (
              <div className="mn-call-stage__strip">
                {layout.strip.map((id) => (
                  <div key={id} className="mn-call-stage__strip-item">
                    {tile(id, 'small')}
                  </div>
                ))}
              </div>
            )}
          </div>
        )}
        {speakingWhileMuted && (
          <p className="mn-call-stage__hint" role="status">
            {m.mutedHint}
          </p>
        )}
      </div>

      {controls ? <div className="mn-call-stage__controls">{controls}</div> : null}
    </section>
  );
});
