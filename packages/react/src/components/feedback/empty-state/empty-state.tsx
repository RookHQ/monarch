import { forwardRef, type HTMLAttributes, type ReactNode } from 'react';
import { cx } from '../../../internal/cx.js';
import { SuccessGlyph } from '../../../internal/icons.js';
import { Button } from '../../actions/button/index.js';

/**
 * Why the view is empty. It changes the default icon tint and, for
 * `filtered`, adds a clear-filters action; the copy always comes from the
 * product.
 *
 * - `first-run`: nothing has been created yet (the default).
 * - `filtered`: a search or filter matched nothing; offer a way back.
 * - `success`: an "all clear" state, such as an empty queue or nothing to fix.
 */
export type EmptyStateKind = 'first-run' | 'filtered' | 'success';

export type EmptyStateHeadingLevel = 'h1' | 'h2' | 'h3' | 'h4' | 'h5' | 'h6' | 'p';

export interface EmptyStateProps extends Omit<HTMLAttributes<HTMLDivElement>, 'title'> {
  /** Decorative icon in a tinted tile. Hidden from assistive technology. */
  icon?: ReactNode;
  title: ReactNode;
  description?: ReactNode;
  /** Primary and secondary actions, usually Buttons. */
  actions?: ReactNode;
  /**
   * Title element. Default `h3`. Match the page outline (no skipped levels);
   * use `p` where a heading would be wrong, such as inside a table cell.
   */
  headingLevel?: EmptyStateHeadingLevel;
  /**
   * `inline` (default) sits inside a card or table; `page` fills an empty
   * view; `compact` is a tighter block for table bodies, side panels and
   * small cards.
   */
  size?: 'inline' | 'page' | 'compact';
  /**
   * `stack` (default) centres icon, text and actions in a column; `line` is a
   * single left-aligned row (icon, text, actions) for a list or dashboard
   * section that has nothing in it.
   */
  layout?: 'stack' | 'line';
  /** Why the view is empty. Exposed as `data-kind`. Default `first-run`. */
  kind?: EmptyStateKind;
  /**
   * With `kind="filtered"`, renders a "Clear filters" button before any other
   * actions. The product resets its own filters.
   */
  onClearFilters?: () => void;
  /** Label of the clear-filters button. Default "Clear filters". */
  clearFiltersLabel?: string;
  /**
   * A short hint under the actions, such as a keyboard shortcut (compose it
   * with Kbd). Shown in small muted text.
   */
  tip?: ReactNode;
  /** Extra content after the actions, such as a list of next steps. */
  children?: ReactNode;
}

/**
 * Shown when there is nothing to display yet. Say what will appear here and
 * offer the action that creates it.
 */
export const EmptyState = forwardRef<HTMLDivElement, EmptyStateProps>(function EmptyState(
  {
    icon,
    title,
    description,
    actions,
    headingLevel: Heading = 'h3',
    size = 'inline',
    layout = 'stack',
    kind = 'first-run',
    onClearFilters,
    clearFiltersLabel = 'Clear filters',
    tip,
    className,
    children,
    ...rest
  },
  ref,
) {
  const glyph = icon ?? (kind === 'success' ? <SuccessGlyph /> : null);
  const clear =
    kind === 'filtered' && onClearFilters ? (
      <Button onClick={onClearFilters}>{clearFiltersLabel}</Button>
    ) : null;
  const allActions =
    clear || actions ? (
      <>
        {clear}
        {actions}
      </>
    ) : null;
  return (
    <div
      ref={ref}
      data-kind={kind === 'first-run' ? undefined : kind}
      {...rest}
      className={cx('mn-empty-state', className)}
      data-size={size}
      data-layout={layout === 'stack' ? undefined : layout}
    >
      {glyph ? (
        <div className="mn-empty-state__icon" aria-hidden>
          {glyph}
        </div>
      ) : null}
      {layout === 'line' ? (
        <div className="mn-empty-state__text">
          <Heading className="mn-empty-state__title">{title}</Heading>
          {description ? <p className="mn-empty-state__description">{description}</p> : null}
        </div>
      ) : (
        <>
          <Heading className="mn-empty-state__title">{title}</Heading>
          {description ? <p className="mn-empty-state__description">{description}</p> : null}
        </>
      )}
      {allActions ? <div className="mn-empty-state__actions">{allActions}</div> : null}
      {children}
      {tip ? <p className="mn-empty-state__tip">{tip}</p> : null}
    </div>
  );
});
