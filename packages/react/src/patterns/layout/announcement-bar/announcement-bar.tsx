'use client';

import { forwardRef, type HTMLAttributes, type ReactNode } from 'react';
import { setRefs, useDismissible } from '../../../internal/dismissible.js';
import { cx } from '../../../internal/cx.js';
import { CloseGlyph } from '../../../internal/icons.js';
import { isSafeUrl } from '../../../internal/sanitize.js';
import { IconButton } from '../../../components/actions/button/index.js';

/** A link at the end of the announcement. */
export interface AnnouncementBarAction {
  /** Link text, e.g. "Read the release notes". */
  label: ReactNode;
  /** Destination. Unsafe URLs (such as `javascript:`) are dropped. */
  href: string;
  /** Open in a new tab (with `rel="noopener noreferrer"`). */
  external?: boolean | undefined;
}

export interface AnnouncementBarMessages {
  /** Accessible name of the bar's region. Default "Announcement". */
  label: string;
  /** Accessible name of the close button. Default "Dismiss announcement". */
  dismiss: string;
  /** Hidden text after a link that opens a new tab. Default "(opens in a new tab)". */
  newTab: string;
}

export const defaultAnnouncementBarMessages: AnnouncementBarMessages = {
  label: 'Announcement',
  dismiss: 'Dismiss announcement',
  newTab: '(opens in a new tab)',
};

export interface AnnouncementBarProps extends HTMLAttributes<HTMLDivElement> {
  /** The announcement: one short sentence. */
  children: ReactNode;
  /** `accent` (the product colour), `neutral`, `inverse` (high emphasis), `info` or `warning`. Default `accent`. */
  tone?: 'accent' | 'neutral' | 'inverse' | 'info' | 'warning' | undefined;
  /** A decorative icon before the text. */
  icon?: ReactNode;
  /** A link after the text. */
  action?: AnnouncementBarAction | undefined;
  /** Replaces `action` with your own element, such as a router Link. */
  actionSlot?: ReactNode;
  /** Shows a close button. Default `true`. */
  dismissible?: boolean | undefined;
  /** Controlled dismissed state. The bar renders nothing while `true`. */
  dismissed?: boolean | undefined;
  /**
   * Uncontrolled initial state. Read a stored dismissal here (on the server
   * from a cookie, or after mount) so a dismissed bar stays hidden.
   */
  defaultDismissed?: boolean | undefined;
  /**
   * Called when the person closes the bar. Persist the dismissal here
   * (cookie, account setting, local storage keyed by announcement id);
   * the bar itself stores nothing.
   */
  onDismiss?: (() => void) | undefined;
  /** Keeps the bar at the top of the viewport while scrolling. Default `false`. */
  sticky?: boolean | undefined;
  messages?: Partial<AnnouncementBarMessages> | undefined;
}

/**
 * A slim, dismissible bar across the top of a page for one timely
 * announcement (a launch, a scheduled maintenance window, a policy change),
 * with an optional link. Dismissal is reported through `onDismiss` so the
 * product decides where to remember it.
 */
export const AnnouncementBar = forwardRef<HTMLDivElement, AnnouncementBarProps>(
  function AnnouncementBar(
    {
      children,
      tone = 'accent',
      icon,
      action,
      actionSlot,
      dismissible = true,
      dismissed: dismissedProp,
      defaultDismissed = false,
      onDismiss,
      sticky = false,
      messages,
      className,
      ...rest
    },
    ref,
  ) {
    const m = { ...defaultAnnouncementBarMessages, ...messages };
    // Dismissing moves focus to the next control (or <main>) before the bar
    // unmounts, so keyboard and screen-reader users do not land on <body>.
    const { shown, dismiss, node } = useDismissible<HTMLDivElement>({
      open: dismissedProp === undefined ? undefined : !dismissedProp,
      defaultOpen: !defaultDismissed,
      onDismiss,
    });
    if (!shown) return null;

    const href = action && isSafeUrl(action.href) ? action.href : undefined;
    const link =
      actionSlot ??
      (action && href ? (
        <a
          className="mn-announcement-bar__link"
          href={href}
          {...(action.external && { target: '_blank', rel: 'noopener noreferrer' })}
        >
          {action.label}
          <span aria-hidden className="mn-announcement-bar__arrow">
            →
          </span>
          {action.external ? (
            <span className="mn-announcement-bar__hidden"> {m.newTab}</span>
          ) : null}
        </a>
      ) : null);

    return (
      <div
        ref={(el) => setRefs(el, ref, node)}
        role="region"
        aria-label={m.label}
        {...rest}
        className={cx('mn-announcement-bar', className)}
        data-tone={tone}
        data-sticky={sticky || undefined}
      >
        <div className="mn-announcement-bar__inner">
          <p className="mn-announcement-bar__message">
            {icon ? (
              <span className="mn-announcement-bar__icon" aria-hidden>
                {icon}
              </span>
            ) : null}
            <span className="mn-announcement-bar__text">{children}</span>
            {link ? <span className="mn-announcement-bar__action">{link}</span> : null}
          </p>
          {dismissible ? (
            <IconButton
              className="mn-announcement-bar__close"
              size="sm"
              variant="ghost"
              label={m.dismiss}
              icon={<CloseGlyph />}
              onClick={dismiss}
            />
          ) : null}
        </div>
      </div>
    );
  },
);
