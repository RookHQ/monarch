'use client';

import {
  forwardRef,
  useCallback,
  useId,
  useState,
  type HTMLAttributes,
  type ReactNode,
} from 'react';
import { cx } from '../../../internal/cx.js';
import { ChevronDownGlyph } from '../../../internal/icons.js';
import { SectionHeader } from '../section-header/index.js';
import { EmptyState } from '../../feedback/empty-state/index.js';
import { ErrorState } from '../../feedback/error-state/index.js';
import { LoadingState, type LoadingStateVariant } from '../../feedback/loading-state/index.js';

export type SectionCardState = 'ready' | 'loading' | 'empty' | 'error';

export interface SectionCardProps extends Omit<HTMLAttributes<HTMLElement>, 'title'> {
  /** The section title, rendered as a heading that labels the section. */
  title: ReactNode;
  /** A sentence under the title explaining the section. */
  description?: ReactNode;
  /** A short secondary line: a count, a date range, "Updated 5 min ago". */
  meta?: ReactNode;
  /** Decorative icon before the title. Hidden from assistive technology. */
  icon?: ReactNode;
  /** Buttons or a menu at the end of the header. */
  actions?: ReactNode;
  /** A trailing "View all" link (SectionHeaderLink), after `actions`. */
  link?: ReactNode;
  /** Heading element. Default `h2`. */
  headingLevel?: 'h2' | 'h3' | 'h4' | 'h5' | 'h6';
  /** Id for the heading. Generated when omitted. */
  headingId?: string;
  /** Content of the footer bar: secondary actions, a summary, a pager. */
  footer?: ReactNode;
  /**
   * What the body shows: the children (`ready`, default), a skeleton
   * (`loading`), an EmptyState (`empty`) or an ErrorState (`error`). Exposed as `data-state`.
   */
  state?: SectionCardState;
  /** Announced while loading. Default "Loading". */
  loadingLabel?: string;
  /** Skeleton shape while loading. Default `rows`. */
  loadingVariant?: LoadingStateVariant;
  /** Replaces the default empty state (for example an EmptyState with actions). */
  empty?: ReactNode;
  /** Title of the default empty state. Default "Nothing here yet". */
  emptyTitle?: ReactNode;
  emptyDescription?: ReactNode;
  /** Title of the error state. Default "This section didn’t load". */
  errorTitle?: ReactNode;
  errorDescription?: ReactNode;
  /** Shows a retry button in the error state. */
  onRetry?: () => void;
  /** The retry is in flight. */
  retrying?: boolean;
  /**
   * The content is refreshing in place: a spinner after the title and
   * `aria-busy` on the section, while the current content stays visible.
   */
  busy?: boolean;
  /** `danger` for a destructive zone (danger border and title). Exposed as `data-tone`. */
  tone?: 'default' | 'danger';
  /** No body padding, for a table or list that runs edge to edge. */
  flush?: boolean;
  /** The title becomes a disclosure button that shows and hides the body. */
  collapsible?: boolean;
  /** Expanded state (controlled). Only with `collapsible`. */
  open?: boolean;
  /** Initial expanded state (uncontrolled). Default `true`. */
  defaultOpen?: boolean;
  onOpenChange?: (open: boolean) => void;
  children?: ReactNode;
}

/**
 * A titled card for one section of a page or dashboard: header with
 * actions and a "View all" link, body, optional footer, and built-in
 * loading, empty and error states. Optionally collapsible.
 */
export const SectionCard = forwardRef<HTMLElement, SectionCardProps>(function SectionCard(
  {
    title,
    description,
    meta,
    icon,
    actions,
    link,
    headingLevel = 'h2',
    headingId,
    footer,
    state = 'ready',
    loadingLabel = 'Loading',
    loadingVariant = 'rows',
    empty,
    emptyTitle = 'Nothing here yet',
    emptyDescription,
    errorTitle = 'This section didn’t load',
    errorDescription,
    onRetry,
    retrying = false,
    busy = false,
    tone = 'default',
    flush = false,
    collapsible = false,
    open: openProp,
    defaultOpen = true,
    onOpenChange,
    className,
    children,
    ...rest
  },
  ref,
) {
  const auto = useId();
  const id = headingId ?? `mn-section-card-${auto}`;
  const bodyId = `${id}-body`;
  const [openState, setOpenState] = useState(defaultOpen);
  const controlled = openProp !== undefined;
  const open = !collapsible || (controlled ? openProp : openState);
  const toggle = useCallback(() => {
    const next = !open;
    if (!controlled) setOpenState(next);
    onOpenChange?.(next);
  }, [open, controlled, onOpenChange]);

  const titleContent = (
    <>
      {icon ? (
        <span className="mn-section-card__icon" aria-hidden="true">
          {icon}
        </span>
      ) : null}
      <span className="mn-section-card__title-text">{title}</span>
    </>
  );
  const heading = collapsible ? (
    <button
      type="button"
      className="mn-section-card__toggle"
      aria-expanded={open}
      aria-controls={bodyId}
      onClick={toggle}
    >
      {titleContent}
      <ChevronDownGlyph className="mn-section-card__chevron" aria-hidden="true" />
    </button>
  ) : (
    titleContent
  );

  let body: ReactNode;
  if (state === 'loading') {
    body = <LoadingState label={loadingLabel} variant={loadingVariant} rows={3} />;
  } else if (state === 'empty') {
    body = empty ?? (
      <EmptyState
        size="compact"
        headingLevel="p"
        title={emptyTitle}
        description={emptyDescription}
      />
    );
  } else if (state === 'error') {
    body = (
      <ErrorState
        size="compact"
        headingLevel="p"
        title={errorTitle}
        description={errorDescription}
        retrying={retrying}
        {...(onRetry ? { onRetry } : {})}
      />
    );
  } else {
    body = children;
  }

  return (
    <section
      ref={ref}
      {...rest}
      aria-labelledby={id}
      aria-busy={busy || state === 'loading' || undefined}
      data-state={state}
      data-tone={tone}
      data-flush={flush || undefined}
      data-collapsible={collapsible || undefined}
      data-open={open || undefined}
      className={cx('mn-section-card', className)}
    >
      <SectionHeader
        className="mn-section-card__header"
        title={heading}
        description={description}
        meta={meta}
        actions={actions}
        link={link}
        headingLevel={headingLevel}
        headingId={id}
        busy={busy}
      />
      {collapsible || (body !== undefined && body !== null && body !== false) ? (
        <div id={bodyId} className="mn-section-card__body" hidden={!open}>
          {body}
        </div>
      ) : null}
      {footer && open ? <div className="mn-section-card__footer">{footer}</div> : null}
    </section>
  );
});
