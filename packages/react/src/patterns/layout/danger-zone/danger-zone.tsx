'use client';

import {
  createContext,
  forwardRef,
  useContext,
  useId,
  type HTMLAttributes,
  type ReactNode,
} from 'react';
import { cx } from '../../../internal/cx.js';
import { LockGlyph } from '../../../internal/icons.js';

type ZoneHeadingLevel = 'h2' | 'h3' | 'h4';
type ItemTitleElement = 'h3' | 'h4' | 'h5' | 'p';

const ITEM_TITLE: Record<ZoneHeadingLevel, ItemTitleElement> = { h2: 'h3', h3: 'h4', h4: 'h5' };

const ItemTitleContext = createContext<ItemTitleElement>('p');

export interface DangerZoneProps extends Omit<HTMLAttributes<HTMLElement>, 'title'> {
  /** Section heading. Default "Danger zone". */
  heading?: ReactNode;
  /**
   * Heading element. Default `h2`; use `h3` when the zone sits under another
   * h2. Item titles are the next level down.
   */
  headingLevel?: ZoneHeadingLevel;
  /** One or two sentences under the heading, such as who can use these actions. */
  description?: ReactNode;
  /** The `DangerZoneItem` rows. */
  children?: ReactNode;
}

/**
 * The settings section that holds irreversible actions: delete a workspace,
 * transfer ownership, leave, delete an account. A titled card with a danger
 * edge whose rows are `DangerZoneItem`s.
 */
export const DangerZone = forwardRef<HTMLElement, DangerZoneProps>(function DangerZone(
  { heading = 'Danger zone', headingLevel = 'h2', description, className, children, ...rest },
  ref,
) {
  const id = useId();
  const H = headingLevel;
  return (
    <section
      ref={ref}
      aria-labelledby={`${id}-heading`}
      {...rest}
      className={cx('mn-danger-zone', className)}
    >
      <div className="mn-danger-zone__header">
        <H id={`${id}-heading`} className="mn-danger-zone__heading">
          {heading}
        </H>
        {description ? <p className="mn-danger-zone__description">{description}</p> : null}
      </div>
      <div className="mn-danger-zone__card">
        <ItemTitleContext.Provider value={ITEM_TITLE[headingLevel]}>
          {children}
        </ItemTitleContext.Provider>
      </div>
    </section>
  );
});

export interface DangerZoneItemProps extends Omit<HTMLAttributes<HTMLDivElement>, 'title'> {
  /** What the action does: "Delete this workspace", "Transfer ownership". */
  title: ReactNode;
  /** The consequence, in one or two sentences. */
  description?: ReactNode;
  /**
   * The control that starts the action, usually a `Button variant="danger"`
   * that opens a `ConfirmDialog` or a flow.
   */
  action: ReactNode;
  /**
   * Why the action is unavailable ("You are the only owner"). Shown as text
   * under the description and added to the row's description; render the
   * action disabled yourself. Exposed as `data-disabled`.
   */
  disabledReason?: ReactNode;
}

/**
 * One irreversible action in a `DangerZone`: title and consequence, with the
 * action beside them, or below them in a narrow container. The row is a
 * group named by its title and described by its description and any
 * `disabledReason`.
 */
export const DangerZoneItem = forwardRef<HTMLDivElement, DangerZoneItemProps>(
  function DangerZoneItem({ title, description, action, disabledReason, className, ...rest }, ref) {
    const id = useId();
    const T = useContext(ItemTitleContext);
    const hasReason = disabledReason !== undefined && disabledReason !== null;
    const describedBy =
      [description ? `${id}-description` : null, hasReason ? `${id}-reason` : null]
        .filter(Boolean)
        .join(' ') || undefined;
    return (
      <div
        ref={ref}
        role="group"
        aria-labelledby={`${id}-title`}
        aria-describedby={describedBy}
        {...rest}
        data-disabled={hasReason || undefined}
        className={cx('mn-danger-zone-item', className)}
      >
        <div className="mn-danger-zone-item__text">
          <T id={`${id}-title`} className="mn-danger-zone-item__title">
            {title}
          </T>
          {description ? (
            <p id={`${id}-description`} className="mn-danger-zone-item__description">
              {description}
            </p>
          ) : null}
          {hasReason ? (
            <p id={`${id}-reason`} className="mn-danger-zone-item__reason">
              <LockGlyph className="mn-danger-zone-item__reason-icon" />
              <span>{disabledReason}</span>
            </p>
          ) : null}
        </div>
        <div className="mn-danger-zone-item__action">{action}</div>
      </div>
    );
  },
);
