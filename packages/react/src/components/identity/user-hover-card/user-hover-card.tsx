'use client';

import {
  cloneElement,
  forwardRef,
  useCallback,
  useId,
  useRef,
  useState,
  type ElementRef,
  type FocusEvent as ReactFocusEvent,
  type HTMLAttributes,
  type PointerEvent as ReactPointerEvent,
  type ReactElement,
  type ReactNode,
} from 'react';
import { cx } from '../../../internal/cx.js';
import { Avatar, type AvatarStatus } from '../avatar/index.js';
import { HoverCard, HoverCardContent, HoverCardTrigger } from '../../overlays/hover-card/index.js';

interface TriggerChildProps {
  id?: string;
  'aria-describedby'?: string | undefined;
  onFocus?: ((event: ReactFocusEvent<HTMLElement>) => void) | undefined;
  onPointerDown?: ((event: ReactPointerEvent<HTMLElement>) => void) | undefined;
}

/** The person a UserHoverCard previews. */
export interface UserHoverCardUser {
  /** Display name. Also the avatar's initials fallback. */
  name: string;
  /** Profile photo URL. Unsafe or broken URLs fall back to initials. */
  avatarSrc?: string | null | undefined;
  /** Stable id used to pick the fallback avatar colour. */
  id?: string | undefined;
  /** Handle shown under the name, e.g. "@sam". */
  handle?: string | undefined;
  /** Email address shown under the name. */
  email?: string | undefined;
  /** Job title or role, e.g. "Design lead". */
  title?: ReactNode;
  /** Presence dot on the avatar. */
  status?: AvatarStatus | undefined;
  /** A short status line, e.g. "In a meeting until 3 pm". */
  statusText?: ReactNode;
}

/** Visible strings, for translation. */
export interface UserHoverCardMessages {
  /** Shown while `loading`. Default "Loading profile". */
  loading: string;
  /** Accessible name of the card. Default `Profile of ${name}`. */
  cardLabel: (name: string) => string;
}

export const defaultUserHoverCardMessages: UserHoverCardMessages = {
  loading: 'Loading profile',
  cardLabel: (name) => `Profile of ${name}`,
};

export interface UserHoverCardProps extends Omit<
  HTMLAttributes<HTMLDivElement>,
  'children' | 'title'
> {
  /** The person to preview. */
  user: UserHoverCardUser;
  /**
   * The element that opens the card on hover and keyboard focus, usually a
   * link to the profile or a name button. It must accept a ref and be
   * focusable. It keeps its own click behaviour.
   */
  children: ReactElement;
  /** Buttons or links at the bottom of the card, e.g. "Message" and "View profile". */
  actions?: ReactNode;
  /** Extra details between the header and the actions, e.g. team or local time. */
  details?: ReactNode;
  /** Shows a loading line instead of `details`, for profiles fetched on open. */
  loading?: boolean | undefined;
  /** Milliseconds the pointer must rest on the trigger before the card opens. Default 500. */
  openDelay?: number | undefined;
  /** Milliseconds before the card closes after the pointer leaves. Default 200. */
  closeDelay?: number | undefined;
  /** Controlled open state. */
  open?: boolean | undefined;
  /** Initial open state when uncontrolled. */
  defaultOpen?: boolean | undefined;
  /** Called when the card opens or closes. Use it to fetch the profile lazily. */
  onOpenChange?: ((open: boolean) => void) | undefined;
  /** Side of the trigger the card appears on. Default `bottom`. */
  side?: 'top' | 'right' | 'bottom' | 'left' | undefined;
  /** Alignment against the trigger. Default `start`. */
  align?: 'start' | 'center' | 'end' | undefined;
  /** Visible strings, for translation. */
  messages?: Partial<UserHoverCardMessages> | undefined;
}

/**
 * A profile preview (photo, name, handle, title and status, plus optional
 * details and actions) that opens when the pointer rests on a person's name
 * or avatar, or when it receives keyboard focus. Built on HoverCard.
 */
export const UserHoverCard = forwardRef<ElementRef<typeof HoverCardContent>, UserHoverCardProps>(
  function UserHoverCard(
    {
      user,
      children,
      actions,
      details,
      loading = false,
      openDelay = 500,
      closeDelay = 200,
      open: openProp,
      defaultOpen = false,
      onOpenChange,
      side = 'bottom',
      align = 'start',
      messages,
      className,
      ...rest
    },
    ref,
  ) {
    const m = { ...defaultUserHoverCardMessages, ...messages };
    const [openState, setOpenState] = useState(defaultOpen);
    const open = openProp ?? openState;
    const openRef = useRef(open);
    openRef.current = open;
    const touched = useRef(false);
    const nameId = useId();
    const detailId = useId();

    const setOpen = useCallback(
      (next: boolean) => {
        // HoverCard's own delayed open can follow the immediate one on focus.
        if (next === openRef.current) return;
        openRef.current = next;
        if (openProp === undefined) setOpenState(next);
        onOpenChange?.(next);
      },
      [openProp, onOpenChange],
    );

    // HoverCard opens on focus after `openDelay`; this card opens on keyboard focus
    // at once (never after a tap), as it always has.
    const child = children as ReactElement<TriggerChildProps>;
    const trigger = cloneElement(child, {
      // Added to the trigger's own description, never in place of it.
      'aria-describedby':
        cx(child.props['aria-describedby'], open ? detailId : undefined) || undefined,
      onPointerDown: (event: ReactPointerEvent<HTMLElement>) => {
        child.props.onPointerDown?.(event);
        touched.current = event.pointerType === 'touch';
      },
      onFocus: (event: ReactFocusEvent<HTMLElement>) => {
        child.props.onFocus?.(event);
        if (touched.current) {
          touched.current = false;
          return;
        }
        setOpen(true);
      },
    });

    return (
      <HoverCard open={open} onOpenChange={setOpen} openDelay={openDelay} closeDelay={closeDelay}>
        <HoverCardTrigger>{trigger}</HoverCardTrigger>
        <HoverCardContent
          ref={ref}
          side={side}
          align={align}
          role="dialog"
          aria-labelledby={undefined}
          aria-label={m.cardLabel(user.name)}
          {...rest}
          className={cx('mn-user-hover-card', className)}
          data-loading={loading || undefined}
        >
          <div className="mn-user-hover-card__header">
            <Avatar
              name={user.name}
              src={user.avatarSrc ?? null}
              size="lg"
              decorative
              {...(user.id ? { seed: user.id } : {})}
              {...(user.status ? { status: user.status } : {})}
            />
            <div className="mn-user-hover-card__identity" id={detailId}>
              <p className="mn-user-hover-card__name" id={nameId}>
                {user.name}
              </p>
              {user.handle || user.email ? (
                <p className="mn-user-hover-card__handle">{user.handle ?? user.email}</p>
              ) : null}
              {user.title ? <p className="mn-user-hover-card__title">{user.title}</p> : null}
              {user.statusText ? (
                <p className="mn-user-hover-card__status">{user.statusText}</p>
              ) : null}
            </div>
          </div>
          {loading ? (
            <p className="mn-user-hover-card__loading" role="status">
              {m.loading}
            </p>
          ) : details ? (
            <div className="mn-user-hover-card__details">{details}</div>
          ) : null}
          {actions ? <div className="mn-user-hover-card__actions">{actions}</div> : null}
        </HoverCardContent>
      </HoverCard>
    );
  },
);
