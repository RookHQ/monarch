'use client';

import { forwardRef, useState, type HTMLAttributes } from 'react';
import { cx } from '../../../internal/cx.js';
import { Avatar, initials, type AvatarSize } from '../../identity/avatar/index.js';
import {
  PresenceDot,
  presenceLabel,
  type PresenceCustomStatus,
  type PresenceDotMessages,
  type PresenceStatus,
} from '../../identity/presence-dot/index.js';
import { Popover, PopoverContent, PopoverTrigger } from '../../overlays/popover/index.js';

/** Someone who is here now. */
export interface PresencePerson {
  /** Stable id, used as the React key and passed back from `onPersonClick`. */
  id: string;
  /** Display name. */
  name: string;
  /** Avatar image. Initials are shown without it. */
  avatarSrc?: string | null | undefined;
  /** Stable id for the avatar colour, so a renamed person keeps their colour. Default `id`. */
  seed?: string | undefined;
  /** Presence. Default `online`. */
  status?: PresenceStatus | undefined;
  /** Custom status message, added to the spoken and hover text. */
  customStatus?: PresenceCustomStatus | null | undefined;
  /** What they are doing here, such as "Editing" or "Viewing slide 4". */
  activity?: string | undefined;
}

export interface PresenceAvatarsMessages {
  /** Accessible name of the stack. Default "{count} people here" ("1 person here"). */
  label: (count: number) => string;
  /** Accessible name of the "+N" button. Default "Show {count} more". */
  more: (count: number) => string;
  /** Name of the overflow list. Default "Everyone here". */
  list: string;
  /**
   * Spoken and hover text for one person: "{name}, {presence}" plus the activity.
   * For a face button showing initials (no picture), `initials` is passed too, so the
   * button's name contains the letters it shows (WCAG 2.5.3 Label in Name):
   * "Priya Raman, PR, Online, Editing".
   */
  person: (
    name: string,
    presence: string,
    activity: string | undefined,
    initials?: string | undefined,
  ) => string;
}

const DEFAULT_MESSAGES: PresenceAvatarsMessages = {
  label: (n) => (n === 1 ? '1 person here' : `${n} people here`),
  more: (n) => `Show ${n} more`,
  list: 'Everyone here',
  person: (name, presence, activity, shown) =>
    [name, shown, presence, activity].filter(Boolean).join(', '),
};

export interface PresenceAvatarsProps extends Omit<HTMLAttributes<HTMLDivElement>, 'children'> {
  /** Who is here, in display order (put the most relevant first). */
  people: readonly PresencePerson[];
  /** Show at most this many avatars, then a "+N" button listing the rest. Default 4. */
  max?: number | undefined;
  /** Avatar size. Default `sm` (24px). */
  size?: AvatarSize | undefined;
  /** Makes each person a button, for example to jump to their cursor or open their profile. */
  onPersonClick?: ((person: PresencePerson) => void) | undefined;
  /** Interface text, for translation. */
  messages?: Partial<PresenceAvatarsMessages> | undefined;
  /** Presence words ("Online", "Away"…), for translation. */
  presenceMessages?: Partial<PresenceDotMessages> | undefined;
}

/**
 * Who is here now: an overlapping stack of avatars, each with a presence
 * dot, and a "+N" button that lists everyone else. Each person is named
 * with their presence and activity, so it never relies on the dot colour.
 */
export const PresenceAvatars = forwardRef<HTMLDivElement, PresenceAvatarsProps>(
  function PresenceAvatars(
    { people, max = 4, size = 'sm', onPersonClick, messages, presenceMessages, className, ...rest },
    ref,
  ) {
    const m = { ...DEFAULT_MESSAGES, ...messages };
    const [open, setOpen] = useState(false);
    const limit = Math.max(0, Math.floor(max));
    // A "+1" button takes the same room as the avatar it hides, so show that avatar instead.
    const visibleCount =
      people.length - limit === 1 ? people.length : Math.min(limit, people.length);
    const visible = people.slice(0, visibleCount);
    const overflow = people.slice(visibleCount);
    const dotSize = size === 'xs' || size === 'sm' ? 'sm' : size === 'md' ? 'md' : 'lg';

    // The letters a face shows when it has no picture (Avatar shows an icon when there are none).
    const shownInitials = (p: PresencePerson) => {
      const letters = initials(p.name);
      return !p.avatarSrc && /\p{L}|\p{N}/u.test(letters) ? letters : undefined;
    };
    const textOf = (p: PresencePerson, withInitials = false) =>
      m.person(
        p.name,
        presenceLabel(p.status ?? 'online', p.customStatus, presenceMessages),
        p.activity,
        withInitials ? shownInitials(p) : undefined,
      );

    const face = (p: PresencePerson) => (
      <>
        <Avatar
          name={p.name}
          src={p.avatarSrc ?? null}
          seed={p.seed ?? p.id}
          size={size}
          decorative
        />
        <PresenceDot
          status={p.status ?? 'online'}
          size={dotSize}
          overlay
          decorative
          showTitle={false}
        />
      </>
    );

    return (
      <div
        ref={ref}
        role="group"
        aria-label={m.label(people.length)}
        {...rest}
        className={cx('mn-presence-avatars', className)}
        data-size={size}
        data-interactive={onPersonClick ? '' : undefined}
      >
        <ul className="mn-presence-avatars__stack">
          {visible.map((p) => {
            const text = textOf(p);
            return (
              <li
                key={p.id}
                className="mn-presence-avatars__item"
                data-status={p.status ?? 'online'}
              >
                {onPersonClick ? (
                  <button
                    type="button"
                    className="mn-presence-avatars__person"
                    aria-label={textOf(p, true)}
                    title={text}
                    onClick={() => onPersonClick(p)}
                  >
                    {face(p)}
                  </button>
                ) : (
                  <span
                    className="mn-presence-avatars__person"
                    role="img"
                    aria-label={text}
                    title={text}
                  >
                    {face(p)}
                  </span>
                )}
              </li>
            );
          })}
        </ul>
        {overflow.length > 0 ? (
          <Popover open={open} onOpenChange={setOpen}>
            <PopoverTrigger asChild>
              <button
                type="button"
                className="mn-presence-avatars__more"
                aria-label={m.more(overflow.length)}
              >
                <span aria-hidden="true">{`+${overflow.length}`}</span>
              </button>
            </PopoverTrigger>
            <PopoverContent
              className="mn-presence-avatars__popover"
              aria-label={m.list}
              side="bottom"
              align="end"
              collisionPadding={8}
            >
              <ul className="mn-presence-avatars__list" aria-label={m.list}>
                {people.map((p) => {
                  const presence = presenceLabel(
                    p.status ?? 'online',
                    p.customStatus,
                    presenceMessages,
                  );
                  const body = (
                    <>
                      <span className="mn-presence-avatars__face">{face(p)}</span>
                      <span className="mn-presence-avatars__text">
                        <span className="mn-presence-avatars__name">{p.name}</span>
                        <span className="mn-presence-avatars__status">
                          {p.activity ? `${presence} · ${p.activity}` : presence}
                        </span>
                      </span>
                    </>
                  );
                  return (
                    <li key={p.id} className="mn-presence-avatars__row">
                      {onPersonClick ? (
                        <button
                          type="button"
                          className="mn-presence-avatars__row-button"
                          onClick={() => {
                            setOpen(false);
                            onPersonClick(p);
                          }}
                        >
                          {body}
                        </button>
                      ) : (
                        <div className="mn-presence-avatars__row-body">{body}</div>
                      )}
                    </li>
                  );
                })}
              </ul>
            </PopoverContent>
          </Popover>
        ) : null}
      </div>
    );
  },
);
