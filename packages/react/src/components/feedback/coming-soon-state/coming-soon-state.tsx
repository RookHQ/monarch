import { forwardRef, type ReactNode } from 'react';
import { cx } from '../../../internal/cx.js';
import { ClockGlyph } from '../../../internal/icons.js';
import { EmptyState, type EmptyStateProps } from '../empty-state/index.js';

/** Visible strings, for translation. */
export interface ComingSoonStateMessages {
  /** Title when none is given. Default "Coming soon". */
  title: string;
  /** Text of the badge shown above a feature title. Default "Coming soon". */
  badge: string;
}

export const COMING_SOON_STATE_MESSAGES: ComingSoonStateMessages = {
  title: 'Coming soon',
  badge: 'Coming soon',
};

export interface ComingSoonStateProps extends Omit<
  EmptyStateProps,
  'title' | 'kind' | 'onClearFilters' | 'clearFiltersLabel'
> {
  /**
   * The feature's name, e.g. "Status pages". When given, a "Coming soon"
   * badge sits above it and is read as part of the heading. When omitted,
   * the title is simply "Coming soon".
   */
  title?: ReactNode;
  /** What the feature will do, in a sentence. */
  description?: ReactNode;
  /** A short list of what is planned, one item per line. */
  highlights?: readonly ReactNode[];
  /** Accessible name of the highlights list, e.g. "What is planned". Optional. */
  highlightsLabel?: string;
  /**
   * Hide the badge (`false`), e.g. when the page header already says the
   * feature is not ready. Default `true` when there is a `title`.
   */
  badge?: boolean;
  /** Visible strings, for translation. */
  messages?: Partial<ComingSoonStateMessages>;
}

/**
 * A placeholder for a feature that is planned but not available yet: a
 * clear "Coming soon" heading, what the feature will do, an optional list
 * of what is planned and an optional action such as "Notify me". Built on
 * EmptyState, so it sits in a card, a tab or a whole page.
 */
export const ComingSoonState = forwardRef<HTMLDivElement, ComingSoonStateProps>(
  function ComingSoonState(
    {
      title,
      highlights,
      highlightsLabel,
      badge = true,
      icon,
      messages: messagesProp,
      className,
      children,
      ...rest
    },
    ref,
  ) {
    const messages = { ...COMING_SOON_STATE_MESSAGES, ...messagesProp };
    const heading =
      title === undefined || title === null ? (
        messages.title
      ) : badge ? (
        <>
          <span className="mn-coming-soon-state__badge">
            {messages.badge}
            <span className="mn-visually-hidden">:</span>
          </span>{' '}
          {title}
        </>
      ) : (
        title
      );
    return (
      <EmptyState
        ref={ref}
        {...rest}
        icon={icon === undefined ? <ClockGlyph /> : icon}
        title={heading}
        className={cx('mn-coming-soon-state', className)}
      >
        {highlights && highlights.length > 0 ? (
          <ul className="mn-coming-soon-state__highlights" aria-label={highlightsLabel}>
            {highlights.map((item, index) => (
              <li key={index} className="mn-coming-soon-state__highlight">
                {item}
              </li>
            ))}
          </ul>
        ) : null}
        {children}
      </EmptyState>
    );
  },
);
