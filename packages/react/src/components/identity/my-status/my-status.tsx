'use client';

import { forwardRef, useEffect, useState, type MouseEventHandler, type Ref } from 'react';
import { Avatar } from '../avatar/index.js';
import {
  PresenceDot,
  presenceLabel,
  type PresenceCustomStatus,
  type PresenceDotMessages,
  type PresenceStatus,
} from '../presence-dot/index.js';
import { cx } from '../../../internal/cx.js';
import { ShellLink, type ShellLinkComponent } from '../../../internal/shell/link.js';

/** A custom status with an optional end time, after which it is not shown. */
export interface MyStatusCustomStatus extends PresenceCustomStatus {
  /** When the status clears itself (ISO string, Date or epoch ms). */
  until?: string | number | Date | null | undefined;
}

export interface MyStatusMessages extends PresenceDotMessages {
  /** Read before the name, naming what the control opens. Default "Your status and settings". */
  label: string;
  /** Shown when there is no custom status and the person is online. Default "Set a status". */
  setStatus: string;
}

const DEFAULT_MESSAGES: Omit<MyStatusMessages, keyof PresenceDotMessages> = {
  label: 'Your status and settings',
  setStatus: 'Set a status',
};

export interface MyStatusProps {
  /** The signed-in person's display name. */
  name: string;
  /** Avatar image. Initials are shown without it. */
  avatarUrl?: string | null | undefined;
  /** Current presence. */
  presence: PresenceStatus;
  /** Custom status; hidden once `until` has passed. */
  customStatus?: MyStatusCustomStatus | null | undefined;
  /**
   * The time `until` is compared with. Default: now, read again when `until`
   * passes so the status clears itself. Pass it for stable server rendering.
   */
  now?: Date | undefined;
  /** Where the control goes (status or preferences page). Renders a link. */
  href?: string | undefined;
  /** Link element for client navigation with `href`. Default `<a>`. */
  linkComponent?: ShellLinkComponent | undefined;
  /** Without `href`: a button that runs this (open a status dialog). */
  onClick?: MouseEventHandler<HTMLElement> | undefined;
  className?: string | undefined;
  /** Interface text, for translation. Presence names are shared with PresenceDot. */
  messages?: Partial<MyStatusMessages> | undefined;
}

function untilMs(until: MyStatusCustomStatus['until']): number | null {
  if (until === null || until === undefined) return null;
  const t = until instanceof Date ? until.getTime() : new Date(until).getTime();
  return Number.isFinite(t) ? t : null;
}

/** The longest delay `setTimeout` takes; a longer one fires at once. */
const MAX_TIMER = 2_147_483_647;

/**
 * The signed-in person's avatar, presence and custom status, as a single
 * link or button at the top of a sidebar that opens their status settings.
 * An expired status is hidden; with no status it invites "Set a status". The
 * app owns the data and the page or dialog it opens.
 */
export const MyStatus = forwardRef<HTMLAnchorElement | HTMLButtonElement, MyStatusProps>(
  function MyStatus(
    {
      name,
      avatarUrl,
      presence,
      customStatus,
      now,
      href,
      linkComponent,
      onClick,
      className,
      messages: messagesProp,
    },
    ref,
  ) {
    const messages = { ...DEFAULT_MESSAGES, ...messagesProp };
    const until = untilMs(customStatus?.until);
    const fixedNow = now?.getTime();
    // Without a `now` prop, render again when `until` passes, so a status set to end at
    // 2 pm disappears at 2 pm instead of staying until something else re-renders.
    const [tick, setTick] = useState(0);
    useEffect(() => {
      if (fixedNow !== undefined || until === null) return;
      const wait = until - Date.now();
      if (wait <= 0) return;
      const timer = setTimeout(() => setTick((n) => n + 1), Math.min(MAX_TIMER, wait + 50));
      return () => clearTimeout(timer);
    }, [fixedNow, until, tick]);
    const expired = until !== null && until <= (fixedNow ?? Date.now());
    const active =
      customStatus && (customStatus.text?.trim() || customStatus.emoji?.trim()) && !expired
        ? customStatus
        : null;
    const presenceText = presenceLabel(presence, null, messagesProp);
    const showsPresence = !active && presence !== 'online' && presence !== 'offline';
    const line = active
      ? [active.emoji, active.text].filter((p) => p && p.trim()).join(' ')
      : showsPresence
        ? presenceText
        : messages.setStatus;

    const inner = (
      <>
        <span className="mn-visually-hidden">{messages.label}: </span>
        <span className="mn-my-status__avatar">
          <Avatar name={name} src={avatarUrl ?? null} size="xs" decorative />
          <PresenceDot
            status={presence}
            overlay
            size="sm"
            showTitle={false}
            // Read after the name instead (below), so the name comes first.
            decorative
          />
        </span>
        <span className="mn-my-status__text">
          <span className="mn-my-status__name">{name}</span>
          <span className="mn-visually-hidden">{showsPresence ? ', ' : `, ${presenceText}, `}</span>
          <span
            className="mn-my-status__line"
            data-empty={active || showsPresence ? undefined : ''}
          >
            {line}
          </span>
        </span>
      </>
    );

    const common = {
      className: cx('mn-my-status', className),
      'data-presence': presence,
      'data-has-status': active ? '' : undefined,
    };

    if (href) {
      return (
        <ShellLink
          ref={ref as Ref<HTMLAnchorElement>}
          as={linkComponent}
          href={href}
          onClick={onClick}
          {...common}
        >
          {inner}
        </ShellLink>
      );
    }
    return (
      <button ref={ref as Ref<HTMLButtonElement>} type="button" onClick={onClick} {...common}>
        {inner}
      </button>
    );
  },
);
