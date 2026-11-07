'use client';
import { forwardRef, useId, type ElementType, type HTMLAttributes, type ReactNode } from 'react';
import { cx } from '../../../internal/cx.js';
import { ChevronRightGlyph, MoreGlyph } from '../../../internal/icons.js';
import { IconButton } from '../../actions/button/index.js';
import { RelativeTime, type DateInput } from '../relative-time/index.js';
import { EmptyState } from '../../feedback/empty-state/index.js';
import { InlineErrorRetry } from '../../feedback/inline-error-retry/index.js';
import { Skeleton } from '../../feedback/skeleton/index.js';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from '../../overlays/dropdown-menu/index.js';

/** One entry in a widget's overflow menu. */
export interface WidgetMenuItem {
  /** Stable key. */
  id: string;
  label: ReactNode;
  onSelect: () => void;
  /** Decorative icon before the label. */
  icon?: ReactNode | undefined;
  disabled?: boolean | undefined;
  /** `danger` for destructive actions such as "Remove from dashboard". Pair it with a confirmation. */
  tone?: 'default' | 'danger' | undefined;
  /** Draw a separator before this item. */
  separatorBefore?: boolean | undefined;
}

/** The words Widget shows and speaks. Override them to translate. */
export interface WidgetMessages {
  /** Accessible name of the overflow menu button. Receives the plain-text title, or "" when the title is not text. */
  menu: (title: string) => string;
  /** Visible text of the view-all link. */
  viewAll: string;
  /**
   * Accessible name of the view-all link, which should start with the visible
   * `viewAll` text. Receives the plain-text title, or "" when the title is not text.
   */
  viewAllLabel: (title: string) => string;
  /** Spoken name of the loading placeholder. Receives the plain-text title. */
  loading: (title: string) => string;
  /** Default error text when `error` is `true`. */
  error: string;
  /** Default empty title when `empty` is `true`. */
  empty: string;
  /** Text before the last-updated time, e.g. "Updated". */
  updated: string;
}

export const WIDGET_MESSAGES: WidgetMessages = {
  menu: (title) => (title ? `More actions for ${title}` : 'More actions'),
  viewAll: 'View all',
  viewAllLabel: (title) => (title ? `View all: ${title}` : 'View all'),
  loading: (title) => (title ? `Loading ${title}` : 'Loading'),
  error: 'This section could not be loaded.',
  empty: 'Nothing to show yet',
  updated: 'Updated',
};

/** How many grid columns a widget spans inside a DashboardGrid on wide screens. */
export type WidgetSpan = 1 | 2 | 3 | 4 | 'full';

export interface WidgetProps extends Omit<HTMLAttributes<HTMLElement>, 'title'> {
  /** The widget's heading, e.g. "Team load". It names the region. */
  title: ReactNode;
  /** Heading level of the title. Default `2` (the dashboard page title is the `h1`). */
  headingLevel?: 2 | 3 | 4 | 5 | 6 | undefined;
  /** A line under the title, e.g. "Approve or send back without leaving this page". */
  description?: ReactNode;
  /** The period the figures cover, shown beside the title, e.g. "Last 30 days". */
  period?: ReactNode;
  /** Decorative icon before the title. */
  icon?: ReactNode;
  /** Controls in the header, before the menu, e.g. a small SegmentedControl. */
  actions?: ReactNode;
  /** Overflow menu entries (refresh, export, remove). Shown behind a "More actions" button. */
  menu?: readonly WidgetMenuItem[] | undefined;
  /** Adds a "View all" link in the header to the full list or report. */
  viewAllHref?: string | undefined;
  /** Component used for `viewAllHref`, e.g. a router link. Default `a`. */
  linkComponent?: ElementType | undefined;
  /** Shows a placeholder instead of the body and sets `aria-busy`. */
  loading?: boolean | undefined;
  /** Replaces the default loading placeholder (three text lines). */
  skeleton?: ReactNode;
  /**
   * Replaces the body with this failure. `true` uses `messages.error`. A slow or
   * failing widget never blocks the rest of the dashboard.
   */
  error?: ReactNode;
  /** With `error`, shows Try again calling this. Return the refetch promise to keep the button busy. */
  onRetry?: (() => unknown) | undefined;
  /**
   * There is nothing to show. `true` renders a one-line EmptyState with
   * `messages.empty`; a node renders that node instead of the body.
   */
  empty?: ReactNode;
  /** When the data was last refreshed. Shown in the footer as "Updated 3 minutes ago". */
  updatedAt?: DateInput | undefined;
  /** Reference time for `updatedAt`, for byte-identical server rendering. */
  now?: DateInput | undefined;
  /** BCP 47 locale for `updatedAt`. */
  locale?: string | undefined;
  /** Content under the body, e.g. a note on what the figures include. */
  footer?: ReactNode;
  /** Columns spanned inside a DashboardGrid on wide screens. Default `1`. */
  span?: WidgetSpan | undefined;
  /** `card` (default) has a border and surface; `plain` has neither, for a widget inside another surface. */
  variant?: 'card' | 'plain' | undefined;
  /** Strings, for localisation. */
  messages?: Partial<WidgetMessages> | undefined;
  children?: ReactNode;
}

function plainText(node: ReactNode): string {
  return typeof node === 'string' || typeof node === 'number' ? String(node) : '';
}

/**
 * The frame every dashboard widget sits in: a titled region with an optional
 * period, header actions, overflow menu and view-all link, and its own
 * loading, empty and error states so one failing widget never blocks the page.
 */
export const Widget = forwardRef<HTMLElement, WidgetProps>(function Widget(
  {
    title,
    headingLevel = 2,
    description,
    period,
    icon,
    actions,
    menu,
    viewAllHref,
    linkComponent: LinkComponent = 'a',
    loading = false,
    skeleton,
    error,
    onRetry,
    empty,
    updatedAt,
    now,
    locale,
    footer,
    span,
    variant,
    messages: messagesProp,
    className,
    children,
    ...rest
  },
  ref,
) {
  const messages = { ...WIDGET_MESSAGES, ...messagesProp };
  const titleId = useId();
  const descriptionId = useId();
  const Heading = `h${headingLevel}` as const;
  const text = plainText(title);
  const hasError = !loading && error !== undefined && error !== null && error !== false;
  const isEmpty = !loading && !hasError && empty !== undefined && empty !== null && empty !== false;
  const state = loading ? 'loading' : hasError ? 'error' : isEmpty ? 'empty' : undefined;

  let body: ReactNode;
  if (loading) {
    body = skeleton ?? (
      <div className="mn-widget__skeleton" role="img" aria-label={messages.loading(text)}>
        <Skeleton shape="text" width="60%" />
        <Skeleton shape="text" width="90%" />
        <Skeleton shape="text" width="75%" />
      </div>
    );
  } else if (hasError) {
    const message = error === true ? messages.error : error;
    body = onRetry ? (
      <InlineErrorRetry onRetry={onRetry}>{message}</InlineErrorRetry>
    ) : (
      <p className="mn-widget__error" role="alert">
        {message}
      </p>
    );
  } else if (isEmpty) {
    body =
      empty === true ? (
        <EmptyState size="compact" layout="line" headingLevel="p" title={messages.empty} />
      ) : (
        empty
      );
  } else {
    body = children;
  }

  const hasMenu = menu !== undefined && menu.length > 0;

  return (
    <section
      ref={ref}
      aria-labelledby={titleId}
      aria-describedby={description ? descriptionId : undefined}
      aria-busy={loading || undefined}
      {...rest}
      className={cx('mn-widget', className)}
      data-state={state}
      data-span={span !== undefined && span !== 1 ? String(span) : undefined}
      data-variant={variant}
    >
      <header className="mn-widget__header">
        <div className="mn-widget__heading">
          <Heading id={titleId} className="mn-widget__title">
            {icon ? (
              <span className="mn-widget__icon" aria-hidden>
                {icon}
              </span>
            ) : null}
            <span className="mn-widget__title-text">{title}</span>
          </Heading>
          {period ? <span className="mn-widget__period">{period}</span> : null}
        </div>
        {actions || viewAllHref || hasMenu ? (
          <div className="mn-widget__actions">
            {actions}
            {viewAllHref ? (
              <LinkComponent
                className="mn-widget__view-all"
                href={viewAllHref}
                aria-label={messages.viewAllLabel(text)}
              >
                {messages.viewAll}
                <span className="mn-widget__view-all-icon" aria-hidden>
                  <ChevronRightGlyph />
                </span>
              </LinkComponent>
            ) : null}
            {hasMenu ? (
              <DropdownMenu>
                <DropdownMenuTrigger asChild>
                  <IconButton
                    className="mn-widget__menu"
                    size="sm"
                    variant="ghost"
                    label={messages.menu(text)}
                    icon={<MoreGlyph />}
                  />
                </DropdownMenuTrigger>
                <DropdownMenuContent align="end">
                  {menu.map((item) => [
                    item.separatorBefore ? (
                      <DropdownMenuSeparator key={`${item.id}-separator`} />
                    ) : null,
                    <DropdownMenuItem
                      key={item.id}
                      onSelect={item.onSelect}
                      {...(item.disabled !== undefined && { disabled: item.disabled })}
                      {...(item.tone !== undefined && { tone: item.tone })}
                      {...(item.icon !== undefined && { icon: item.icon })}
                    >
                      {item.label}
                    </DropdownMenuItem>,
                  ])}
                </DropdownMenuContent>
              </DropdownMenu>
            ) : null}
          </div>
        ) : null}
        {description ? (
          <p id={descriptionId} className="mn-widget__description">
            {description}
          </p>
        ) : null}
      </header>
      <div className="mn-widget__body">{body}</div>
      {footer || (updatedAt !== undefined && !loading) ? (
        <footer className="mn-widget__footer">
          {footer}
          {updatedAt !== undefined && !loading ? (
            <p className="mn-widget__updated">
              {messages.updated}{' '}
              <RelativeTime
                date={updatedAt}
                tooltip={false}
                live
                {...(now !== undefined && { now })}
                {...(locale !== undefined && { locale })}
              />
            </p>
          ) : null}
        </footer>
      ) : null}
    </section>
  );
});
