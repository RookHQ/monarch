'use client';

import {
  forwardRef,
  useCallback,
  useEffect,
  useRef,
  useState,
  type HTMLAttributes,
  type KeyboardEvent as ReactKeyboardEvent,
  type PointerEvent as ReactPointerEvent,
} from 'react';
import { cx } from '../../../internal/cx.js';
import { CallControlButton, CallIcon } from '../call-controls/index.js';
import { CallTimer, type CallParticipant } from '../call-stage/index.js';
import { CallTile, type CallTileMessages } from '../call-tile/index.js';

/** Distance from the viewport's right and bottom edges, in CSS pixels. */
export interface MiniCallPosition {
  right: number;
  bottom: number;
}

/** Visible and announced strings. Override them to translate. */
export interface MiniCallMessages extends Partial<CallTileMessages> {
  region: string;
  waiting: string;
  people: (count: number) => string;
  duration: string;
  mute: string;
  unmute: string;
  expand: string;
  leave: string;
  move: string;
}

export const DEFAULT_MINI_CALL_MESSAGES: MiniCallMessages = {
  region: 'Call (minimised)',
  waiting: 'Waiting for others…',
  people: (count) => `${count} in call`,
  duration: 'Call duration',
  mute: 'Mute',
  unmute: 'Unmute',
  expand: 'Expand call',
  leave: 'Leave call',
  move: 'Move call window. Use the arrow keys.',
};

const MARGIN = 12;
const STEP = 24;

export interface MiniCallProps extends Omit<HTMLAttributes<HTMLDivElement>, 'children'> {
  /** Who to show, usually the active speaker. Without one it says it is waiting. */
  participant?: CallParticipant | null | undefined;
  /** When the call started; shows a running timer. */
  startedAt?: string | number | Date | undefined;
  /** Everyone on the call, yourself included. */
  count?: number | undefined;
  /** Microphone state; the mute button shows when `onMicChange` is set. */
  micOn?: boolean | undefined;
  onMicChange?: ((next: boolean) => void) | undefined;
  /** Shows an Expand button; double-click also expands. */
  onExpand?: (() => void) | undefined;
  /** Shows a Leave button. */
  onLeave?: (() => void) | undefined;
  /**
   * `floating` (default) is a fixed, draggable window; `inline` renders in
   * place with no dragging (for a sidebar or a docs page).
   */
  placement?: 'floating' | 'inline' | undefined;
  /** Controlled position of the floating window. */
  position?: MiniCallPosition | undefined;
  /** Starting position when uncontrolled. Default 16px from the right and bottom. */
  defaultPosition?: MiniCallPosition | undefined;
  /** Called when a drag or a keyboard move ends, for example to remember it. */
  onPositionChange?: ((position: MiniCallPosition) => void) | undefined;
  /** Override visible and announced strings. */
  messages?: Partial<MiniCallMessages> | undefined;
}

/**
 * A minimised call: a small floating window showing whoever is talking, with
 * the timer, mute, expand and leave, so people keep working during a call.
 * Drag it with a pointer, or focus its handle and use the arrow keys.
 */
export const MiniCall = forwardRef<HTMLDivElement, MiniCallProps>(function MiniCall(
  {
    participant,
    startedAt,
    count,
    micOn = true,
    onMicChange,
    onExpand,
    onLeave,
    placement = 'floating',
    position: positionProp,
    defaultPosition = { right: 16, bottom: 16 },
    onPositionChange,
    messages: messagesProp,
    className,
    style,
    onDoubleClick,
    ...rest
  },
  ref,
) {
  const m = { ...DEFAULT_MINI_CALL_MESSAGES, ...messagesProp };
  const box = useRef<HTMLDivElement | null>(null);
  const [own, setOwn] = useState(defaultPosition);
  const pos = positionProp ?? own;
  const posRef = useRef(pos);
  posRef.current = pos;
  const drag = useRef<{ x: number; y: number; right: number; bottom: number } | null>(null);
  const [live, setLive] = useState<MiniCallPosition | null>(null);
  const floating = placement === 'floating';

  const clamp = useCallback((p: MiniCallPosition): MiniCallPosition => {
    const el = box.current;
    if (!el || typeof window === 'undefined') return p;
    const maxRight = Math.max(MARGIN, window.innerWidth - el.offsetWidth - MARGIN);
    const maxBottom = Math.max(MARGIN, window.innerHeight - el.offsetHeight - MARGIN);
    return {
      right: Math.round(Math.min(Math.max(MARGIN, p.right), maxRight)),
      bottom: Math.round(Math.min(Math.max(MARGIN, p.bottom), maxBottom)),
    };
  }, []);

  const commit = useCallback(
    (p: MiniCallPosition) => {
      setOwn(p);
      onPositionChange?.(p);
    },
    [onPositionChange],
  );

  // Keep it on screen after the viewport changes.
  useEffect(() => {
    if (!floating) return;
    const onResize = () => {
      const next = clamp(posRef.current);
      if (next.right !== posRef.current.right || next.bottom !== posRef.current.bottom)
        commit(next);
    };
    onResize();
    window.addEventListener('resize', onResize);
    return () => window.removeEventListener('resize', onResize);
  }, [floating, clamp, commit]);

  const onPointerDown = (event: ReactPointerEvent<HTMLDivElement>) => {
    if (!floating || event.button > 0) return;
    if ((event.target as HTMLElement).closest('button')) return;
    drag.current = { x: event.clientX, y: event.clientY, right: pos.right, bottom: pos.bottom };
    event.currentTarget.setPointerCapture?.(event.pointerId);
  };
  const onPointerMove = (event: ReactPointerEvent<HTMLDivElement>) => {
    const d = drag.current;
    if (!d) return;
    setLive(
      clamp({ right: d.right - (event.clientX - d.x), bottom: d.bottom - (event.clientY - d.y) }),
    );
  };
  const onPointerUp = () => {
    if (drag.current && live) commit(live);
    drag.current = null;
    setLive(null);
  };

  const onHandleKey = (event: ReactKeyboardEvent<HTMLButtonElement>) => {
    const delta: Record<string, [number, number]> = {
      ArrowLeft: [STEP, 0],
      ArrowRight: [-STEP, 0],
      ArrowUp: [0, STEP],
      ArrowDown: [0, -STEP],
    };
    const d = delta[event.key];
    if (!d) return;
    event.preventDefault();
    commit(clamp({ right: pos.right + d[0], bottom: pos.bottom + d[1] }));
  };

  const shown = live ?? pos;
  const { id: _id, ...person }: CallParticipant = participant ?? { id: '', name: '' };

  return (
    // Dragging is a pointer shortcut; the Move handle gives keyboard users the same control.
    // eslint-disable-next-line jsx-a11y/no-noninteractive-element-interactions
    <div
      ref={(el) => {
        box.current = el;
        if (typeof ref === 'function') ref(el);
        else if (ref) ref.current = el;
      }}
      role="region"
      aria-label={m.region}
      {...rest}
      data-placement={placement}
      data-dragging={live ? '' : undefined}
      className={cx('mn-mini-call', className)}
      style={floating ? { ...style, right: shown.right, bottom: shown.bottom } : style}
      onPointerDown={onPointerDown}
      onPointerMove={onPointerMove}
      onPointerUp={onPointerUp}
      onPointerCancel={onPointerUp}
      onDoubleClick={(event) => {
        onDoubleClick?.(event);
        if (!(event.target as HTMLElement).closest('button')) onExpand?.();
      }}
    >
      <div className="mn-mini-call__view">
        {participant ? (
          <CallTile {...person} fullscreen={false} messages={m} className="mn-mini-call__tile" />
        ) : (
          <p className="mn-mini-call__waiting">{m.waiting}</p>
        )}
      </div>
      <div className="mn-mini-call__bar">
        {floating && (
          <button
            type="button"
            className="mn-mini-call__handle"
            aria-label={m.move}
            title={m.move}
            onKeyDown={onHandleKey}
          >
            <svg viewBox="0 0 24 24" width="16" height="16" aria-hidden="true" focusable="false">
              {[9, 15].flatMap((x) =>
                [6, 12, 18].map((y) => (
                  <circle key={`${x}-${y}`} cx={x} cy={y} r="1.5" fill="currentColor" />
                )),
              )}
            </svg>
          </button>
        )}
        {startedAt !== undefined && <CallTimer since={startedAt} label={m.duration} />}
        {count !== undefined && <span className="mn-mini-call__count">{m.people(count)}</span>}
        <div className="mn-mini-call__actions">
          {onMicChange && (
            <CallControlButton
              size="sm"
              label={micOn ? m.mute : m.unmute}
              pressed={!micOn}
              tone={micOn ? 'default' : 'off'}
              icon={<CallIcon name={micOn ? 'mic' : 'mic-off'} />}
              onClick={() => onMicChange(!micOn)}
            />
          )}
          {onExpand && (
            <CallControlButton
              size="sm"
              label={m.expand}
              icon={<CallIcon name="expand" />}
              onClick={onExpand}
            />
          )}
          {onLeave && (
            <CallControlButton
              size="sm"
              tone="danger"
              label={m.leave}
              icon={<CallIcon name="phone-off" />}
              onClick={onLeave}
            />
          )}
        </div>
      </div>
    </div>
  );
});
