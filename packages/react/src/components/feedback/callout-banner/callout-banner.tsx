'use client';

import { forwardRef, useId, type HTMLAttributes, type ReactNode } from 'react';
import { cx } from '../../../internal/cx.js';
import { setRefs, useDismissible } from '../../../internal/dismissible.js';
import {
  CloseGlyph,
  ErrorGlyph,
  InfoGlyph,
  SuccessGlyph,
  WarningGlyph,
} from '../../../internal/icons.js';
import { IconButton } from '../../actions/button/index.js';

export type CalloutBannerTone = 'neutral' | 'info' | 'accent' | 'success' | 'warning' | 'danger';

export interface CalloutBannerProps extends Omit<HTMLAttributes<HTMLElement>, 'title'> {
  /**
   * Colour and default icon. `accent` uses the product accent, for trials,
   * upgrades and announcements. Default `info`.
   */
  tone?: CalloutBannerTone;
  /**
   * `page`: a full-width strip across the top of the app or page (trial,
   * maintenance, billing), with no rounded corners. `section`: a rounded
   * callout inside the content. Default `section`.
   */
  placement?: 'page' | 'section';
  /**
   * `scope`: a compact, neutral one-line notice that the view is narrowed
   * ("Showing tickets in Support"), usually with an action that clears it.
   * Default `callout`.
   */
  variant?: 'callout' | 'scope';
  /** Bold first line. */
  title?: ReactNode;
  /** Replaces the tone icon. Pass `null` for none. */
  icon?: ReactNode;
  /** Buttons or links, for example "Upgrade" or "Retry". */
  actions?: ReactNode;
  /**
   * `end` puts actions on the trailing edge (below the text on narrow
   * screens); `inline` puts them right after the text, like a sentence that
   * ends in a link. Default `end`.
   */
  actionPlacement?: 'end' | 'inline';
  /**
   * Makes the banner a landmark, so screen-reader users can jump to it:
   * `region` (`<section>`) or `complementary` (`<aside>`). Named by `title`,
   * or by `aria-label` when there is no title. Default `none` (`<div>`).
   */
  landmark?: 'none' | 'region' | 'complementary';
  /**
   * Announce the banner's text when it appears: `polite` (role="status") or
   * `assertive` (role="alert", only for errors that block progress). Leave
   * unset for a banner that is present when the page loads.
   */
  live?: 'polite' | 'assertive';
  /** Shows a close button. Implied by `onDismiss`. */
  dismissible?: boolean;
  /**
   * Called when the user dismisses the banner (and shows the close button).
   * Store "don't show again" here: Monarch keeps nothing.
   */
  onDismiss?: () => void;
  /** Controlled visibility. Leave unset to let the banner hide itself on dismiss. */
  open?: boolean;
  /** Initial visibility when uncontrolled. Default true. */
  defaultOpen?: boolean;
  /** Accessible name of the close button. Default "Dismiss". */
  dismissLabel?: string;
}

const toneIcon = {
  neutral: <InfoGlyph />,
  info: <InfoGlyph />,
  accent: <InfoGlyph />,
  success: <SuccessGlyph />,
  warning: <WarningGlyph />,
  danger: <ErrorGlyph />,
} as const;

/**
 * A page- or section-level message about the account, the product or the
 * current view: trials, maintenance windows, billing problems, a failed sync,
 * a narrowed scope. Optionally dismissible, with an action slot and a
 * landmark role.
 */
export const CalloutBanner = forwardRef<HTMLElement, CalloutBannerProps>(function CalloutBanner(
  {
    tone = 'info',
    placement = 'section',
    variant = 'callout',
    title,
    icon,
    actions,
    actionPlacement = 'end',
    landmark = 'none',
    live,
    dismissible,
    onDismiss,
    open,
    defaultOpen,
    dismissLabel = 'Dismiss',
    className,
    children,
    ...rest
  },
  ref,
) {
  const titleId = useId();
  const { shown, dismiss, node } = useDismissible<HTMLElement>({ open, defaultOpen, onDismiss });
  if (!shown) return null;

  const Root = landmark === 'region' ? 'section' : landmark === 'complementary' ? 'aside' : 'div';
  const scope = variant === 'scope';
  const glyph = icon === undefined ? (scope ? null : toneIcon[tone]) : icon;
  const role = live === 'assertive' ? 'alert' : live === 'polite' ? 'status' : undefined;
  const labelled = landmark !== 'none' && title && !rest['aria-label'] ? titleId : undefined;

  const actionsNode = actions ? <div className="mn-callout-banner__actions">{actions}</div> : null;

  return (
    <Root
      ref={(el: HTMLElement | null) => setRefs(el, ref, node)}
      aria-labelledby={labelled}
      {...rest}
      className={cx('mn-callout-banner', className)}
      data-tone={tone}
      data-placement={placement}
      data-variant={variant}
      data-action-placement={actionPlacement}
    >
      {glyph ? (
        <span className="mn-callout-banner__icon" aria-hidden>
          {glyph}
        </span>
      ) : null}
      {/* The live role sits on the text, so a landmark root keeps its own role. */}
      <div className="mn-callout-banner__main">
        <div className="mn-callout-banner__text" role={role}>
          {title ? (
            <p className="mn-callout-banner__title" id={titleId}>
              {title}
            </p>
          ) : null}
          {children ? <div className="mn-callout-banner__body">{children}</div> : null}
        </div>
        {actionPlacement === 'inline' ? actionsNode : null}
      </div>
      {actionPlacement === 'end' ? actionsNode : null}
      {dismissible || onDismiss ? (
        <IconButton
          className="mn-callout-banner__dismiss"
          size="xs"
          label={dismissLabel}
          icon={<CloseGlyph />}
          onClick={dismiss}
        />
      ) : null}
    </Root>
  );
});
