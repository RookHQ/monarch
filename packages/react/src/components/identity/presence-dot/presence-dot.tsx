import { forwardRef, type HTMLAttributes } from 'react';
import { cx } from '../../../internal/cx.js';

/**
 * A person's presence. `busy` and `dnd` share the danger colour; `dnd` reads
 * "Do not disturb". Matches Avatar's `status` values plus `dnd`.
 */
export type PresenceStatus = 'online' | 'away' | 'busy' | 'dnd' | 'offline';

/** A custom status message set by the person ("In a meeting"). */
export interface PresenceCustomStatus {
  /** A single emoji shown before the text. Decorative in the dot itself. */
  emoji?: string | undefined;
  /** The status text. */
  text?: string | undefined;
}

export interface PresenceDotMessages {
  online: string;
  away: string;
  busy: string;
  dnd: string;
  offline: string;
  /** Joins the presence and the custom status: `${presence} · ${status}`. */
  withStatus: (presence: string, status: string) => string;
}

const DEFAULT_MESSAGES: PresenceDotMessages = {
  online: 'Online',
  away: 'Away',
  busy: 'Busy',
  dnd: 'Do not disturb',
  offline: 'Offline',
  withStatus: (presence, status) => `${presence} · ${status}`,
};

/** The text a presence and custom status read as, e.g. "Away · 🌴 On holiday". */
export function presenceLabel(
  status: PresenceStatus,
  customStatus?: PresenceCustomStatus | null,
  messages?: Partial<PresenceDotMessages>,
): string {
  const m = { ...DEFAULT_MESSAGES, ...messages };
  const base = m[status];
  const custom = customStatus
    ? [customStatus.emoji, customStatus.text].filter((part) => part && part.trim()).join(' ')
    : '';
  return custom ? m.withStatus(base, custom) : base;
}

export interface PresenceDotProps extends Omit<HTMLAttributes<HTMLSpanElement>, 'children'> {
  /** The presence to show. */
  status: PresenceStatus;
  /** Custom status, added to the text alternative and the hover title. */
  customStatus?: PresenceCustomStatus | null | undefined;
  /** `sm` 8px (lists, avatars up to 24px), `md` 10px (default), `lg` 12px. */
  size?: 'sm' | 'md' | 'lg' | undefined;
  /**
   * Sit on the bottom-end corner of the nearest positioned parent (an avatar
   * wrapper with `position: relative`), with a ring in the surface colour.
   */
  overlay?: boolean | undefined;
  /**
   * Hide the text alternative from assistive technology, when the same
   * information is already in nearby text. Default false.
   */
  decorative?: boolean | undefined;
  /** Show the text as a native hover title. Default true. */
  showTitle?: boolean | undefined;
  /** Interface text, for translation. */
  messages?: Partial<PresenceDotMessages> | undefined;
}

/**
 * A small dot for a person's presence: online, away, busy or do not disturb,
 * offline. Its text ("Away · In a meeting") is read by screen readers, so the
 * colour is never the only signal; in forced-colours mode each status also
 * gets its own shape. Data in: the app decides the presence; the dot never
 * subscribes to anything.
 */
export const PresenceDot = forwardRef<HTMLSpanElement, PresenceDotProps>(function PresenceDot(
  {
    status,
    customStatus,
    size = 'md',
    overlay = false,
    decorative = false,
    showTitle = true,
    messages,
    className,
    ...rest
  },
  ref,
) {
  const label = presenceLabel(status, customStatus, messages);
  return (
    <span
      ref={ref}
      title={showTitle ? label : undefined}
      {...rest}
      className={cx('mn-presence-dot', className)}
      data-status={status}
      data-size={size}
      data-overlay={overlay ? '' : undefined}
      aria-hidden={decorative ? true : rest['aria-hidden']}
    >
      {decorative ? null : <span className="mn-visually-hidden">{label}</span>}
    </span>
  );
});
