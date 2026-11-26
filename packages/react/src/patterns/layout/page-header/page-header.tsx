import { forwardRef, type HTMLAttributes, type ReactNode } from 'react';
import { cx } from '../../../internal/cx.js';
import { BackLink } from '../../../components/navigation/back-link/index.js';
import { PageHeaderActionBar, type PageHeaderAction } from './page-header.actions.js';

export type { PageHeaderAction } from './page-header.actions.js';

export interface PageHeaderProps extends Omit<HTMLAttributes<HTMLElement>, 'title'> {
  /** Page title, rendered as the page's single h1. */
  title: ReactNode;
  description?: ReactNode;
  /** Breadcrumb or eyebrow text above the title. */
  eyebrow?: ReactNode;
  /** A Breadcrumb above the title. Rendered before `eyebrow` when both are given. */
  breadcrumbs?: ReactNode;
  /**
   * A back link above the title: any node (usually a BackLink with `asChild`
   * around a router link). Takes precedence over `backHref`.
   */
  back?: ReactNode;
  /** Renders a BackLink to this URL above the title. Use `back` for router links. */
  backHref?: string;
  /** Text of the `backHref` link, naming the destination ("All domains"). Default "Back". */
  backLabel?: ReactNode;
  /** Primary and secondary actions. Keep one `primary` Button. */
  actions?: ReactNode;
  /** The page's main action, placed last in the action row. Stays visible at every width. */
  primaryAction?: ReactNode;
  /**
   * Secondary actions as data. The first `maxVisibleActions` show as buttons
   * on wide screens and the rest go into a "More actions" menu. Below 640px
   * all of them move into the menu.
   */
  secondaryActions?: PageHeaderAction[];
  /** How many secondary actions show as buttons on wide screens. Default 2. */
  maxVisibleActions?: number;
  /** Accessible name of the overflow menu button. Default "More actions". */
  moreActionsLabel?: string;
  /** A TabsList or TabsNav for section navigation below the title. */
  tabs?: ReactNode;
  /** Badge or status next to the title. */
  meta?: ReactNode;
  /** A status badge next to the title, before `meta` (Published, Draft, Active). */
  status?: ReactNode;
  /**
   * A secondary line under the title: an address with a CopyButton, an id,
   * "Updated 5 min ago". Rendered before `description`.
   */
  details?: ReactNode;
  /** A notice above the header (an Alert or CalloutBanner), for example "This page is archived". */
  banner?: ReactNode;
  /**
   * Stick to the top of the scroll container with a blurred backdrop. The
   * offset is `--mn-page-header-sticky-top` (default 0).
   */
  sticky?: boolean;
  /**
   * For a page embedded in another shell (App Settings): the title becomes
   * visually hidden but stays the heading, and the outer padding is removed.
   */
  embedded?: boolean;
  /** Heading element. `h1` (default) for a page; `h2` when embedded under another h1. */
  headingLevel?: 'h1' | 'h2';
}

/**
 * Top of every main view: title, context, and the actions that apply to the
 * whole page. Server-safe; only the overflow menu is a client component.
 */
export const PageHeader = forwardRef<HTMLElement, PageHeaderProps>(function PageHeader(
  {
    title,
    description,
    eyebrow,
    breadcrumbs,
    back,
    backHref,
    backLabel = 'Back',
    actions,
    primaryAction,
    secondaryActions,
    maxVisibleActions = 2,
    moreActionsLabel = 'More actions',
    tabs,
    meta,
    status,
    details,
    banner,
    sticky = false,
    embedded = false,
    headingLevel: H = 'h1',
    className,
    ...rest
  },
  ref,
) {
  const backNode = back ?? (backHref ? <BackLink href={backHref}>{backLabel}</BackLink> : null);
  const secondary = secondaryActions ?? [];
  const hasActions = Boolean(actions) || Boolean(primaryAction) || secondary.length > 0;
  return (
    <header
      ref={ref}
      {...rest}
      data-sticky={sticky || undefined}
      data-embedded={embedded || undefined}
      className={cx('mn-page-header', className)}
    >
      {banner ? <div className="mn-page-header__banner">{banner}</div> : null}
      {backNode ? <div className="mn-page-header__back">{backNode}</div> : null}
      {breadcrumbs ? <div className="mn-page-header__breadcrumbs">{breadcrumbs}</div> : null}
      {eyebrow ? <div className="mn-page-header__eyebrow">{eyebrow}</div> : null}
      <div className="mn-page-header__row">
        <div className="mn-page-header__text">
          <div className="mn-page-header__title-row">
            <H className={cx('mn-page-header__title', embedded && 'mn-visually-hidden')}>{title}</H>
            {status ? <div className="mn-page-header__status">{status}</div> : null}
            {meta ? <div className="mn-page-header__meta">{meta}</div> : null}
          </div>
          {details ? <div className="mn-page-header__details">{details}</div> : null}
          {description ? <p className="mn-page-header__description">{description}</p> : null}
        </div>
        {hasActions ? (
          <div className="mn-page-header__actions">
            {actions}
            {secondary.length > 0 ? (
              <PageHeaderActionBar
                actions={secondary}
                maxVisible={maxVisibleActions}
                moreLabel={moreActionsLabel}
              />
            ) : null}
            {primaryAction}
          </div>
        ) : null}
      </div>
      {tabs ? <div className="mn-page-header__tabs">{tabs}</div> : null}
    </header>
  );
});
