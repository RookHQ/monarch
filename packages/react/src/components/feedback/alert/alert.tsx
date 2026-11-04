import type { HTMLAttributes, ReactNode } from 'react';
import { cx } from '../../../internal/cx.js';
import { InfoGlyph, SuccessGlyph, WarningGlyph } from '../../../internal/icons.js';

export interface AlertProps extends Omit<HTMLAttributes<HTMLDivElement>, 'title'> {
  tone?: 'neutral' | 'info' | 'success' | 'warning' | 'danger';
  title?: ReactNode;
  /** Replaces the default tone icon. Pass `null` for none. */
  icon?: ReactNode;
  /** Actions on the trailing edge, e.g. a Button or a dismiss IconButton. */
  actions?: ReactNode;
  /**
   * Set when the alert appears in response to something the user did, so it
   * is announced. `assertive` only for errors that block progress.
   */
  live?: 'polite' | 'assertive';
}

const defaultIcon = {
  neutral: <InfoGlyph />,
  info: <InfoGlyph />,
  success: <SuccessGlyph />,
  warning: <WarningGlyph />,
  danger: <WarningGlyph />,
} as const;

/** Inline message about the state of a page or section. */
export function Alert({
  tone = 'info',
  title,
  icon,
  actions,
  live,
  className,
  children,
  ...rest
}: AlertProps) {
  const role = live === 'assertive' ? 'alert' : live === 'polite' ? 'status' : undefined;
  const glyph = icon === undefined ? defaultIcon[tone] : icon;
  return (
    <div {...rest} role={rest.role ?? role} className={cx('mn-alert', className)} data-tone={tone}>
      {glyph ? (
        <span className="mn-alert__icon" aria-hidden>
          {glyph}
        </span>
      ) : null}
      <div className="mn-alert__body">
        {title ? <p className="mn-alert__title">{title}</p> : null}
        {children ? <div className="mn-alert__content">{children}</div> : null}
      </div>
      {actions ? <div className="mn-alert__actions">{actions}</div> : null}
    </div>
  );
}
