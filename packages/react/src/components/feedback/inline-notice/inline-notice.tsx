'use client';

import { forwardRef, type HTMLAttributes, type ReactNode } from 'react';
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

export type InlineNoticeTone = 'neutral' | 'info' | 'success' | 'warning' | 'danger';

export interface InlineNoticeProps extends Omit<HTMLAttributes<HTMLDivElement>, 'title'> {
  /** Colour and default icon. Default `info`. */
  tone?: InlineNoticeTone;
  /**
   * `subtle`: a tinted strip. `plain`: icon and coloured text only, for tight
   * spots such as under a field or inside a table row. Default `subtle`.
   */
  variant?: 'subtle' | 'plain';
  /** A short bold lead-in on the same line ("Conflict."). */
  title?: ReactNode;
  /** Replaces the tone icon. Pass `null` for none. */
  icon?: ReactNode;
  /** One small action on the trailing edge, e.g. a `size="xs"` Button. */
  action?: ReactNode;
  /**
   * Announce the notice when it appears. `true` picks by tone: `danger` is
   * assertive (role="alert"), every other tone polite (role="status").
   * Leave unset for a notice that is there when the view loads.
   */
  live?: boolean | 'polite' | 'assertive';
  /** Shows a close button. Implied by `onDismiss`. */
  dismissible?: boolean;
  /** Called when the user dismisses the notice; persistence stays with the product. */
  onDismiss?: () => void;
  /** Controlled visibility. Leave unset to let the notice hide itself on dismiss. */
  open?: boolean;
  /** Initial visibility when uncontrolled. Default true. */
  defaultOpen?: boolean;
  /** Accessible name of the close button. Default "Dismiss". */
  dismissLabel?: string;
}

const toneIcon = {
  neutral: <InfoGlyph />,
  info: <InfoGlyph />,
  success: <SuccessGlyph />,
  warning: <WarningGlyph />,
  danger: <ErrorGlyph />,
} as const;

/**
 * A one-line message attached to the thing it is about: a form, a card, a
 * row or a panel. Smaller than Alert, with no body block: icon, optional bold
 * lead-in, one sentence and at most one action.
 */
export const InlineNotice = forwardRef<HTMLDivElement, InlineNoticeProps>(function InlineNotice(
  {
    tone = 'info',
    variant = 'subtle',
    title,
    icon,
    action,
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
  const { shown, dismiss, node } = useDismissible<HTMLDivElement>({ open, defaultOpen, onDismiss });
  if (!shown) return null;

  const politeness =
    live === true ? (tone === 'danger' ? 'assertive' : 'polite') : live || undefined;
  const role =
    politeness === 'assertive' ? 'alert' : politeness === 'polite' ? 'status' : undefined;
  const glyph = icon === undefined ? toneIcon[tone] : icon;

  return (
    <div
      ref={(el: HTMLDivElement | null) => setRefs(el, ref, node)}
      {...rest}
      className={cx('mn-inline-notice', className)}
      data-tone={tone}
      data-variant={variant}
    >
      {glyph ? (
        <span className="mn-inline-notice__icon" aria-hidden>
          {glyph}
        </span>
      ) : null}
      <p className="mn-inline-notice__message" role={role}>
        {title ? <strong className="mn-inline-notice__title">{title}</strong> : null}
        {title && children ? ' ' : null}
        {children}
      </p>
      {action ? <span className="mn-inline-notice__action">{action}</span> : null}
      {dismissible || onDismiss ? (
        <IconButton
          className="mn-inline-notice__dismiss"
          size="xs"
          label={dismissLabel}
          icon={<CloseGlyph />}
          onClick={dismiss}
        />
      ) : null}
    </div>
  );
});
