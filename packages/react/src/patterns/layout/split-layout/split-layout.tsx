'use client';

import type { HTMLAttributes, ReactNode } from 'react';
import { cx } from '../../../internal/cx.js';
import { IconButton } from '../../../components/actions/button/index.js';
import { ChevronRightGlyph } from '../../../internal/icons.js';

export interface SplitLayoutProps extends HTMLAttributes<HTMLDivElement> {
  /** The list pane (e.g. ticket list). */
  list: ReactNode;
  /** The detail pane. When empty, pass an EmptyState. */
  detail: ReactNode;
  listLabel?: string;
  detailLabel?: string;
  /** Width of the list pane on wide screens. */
  listWidth?: 'sm' | 'md' | 'lg';
  /**
   * Below the `md` breakpoint only one pane shows. `detailOpen` picks which;
   * `onBack` renders a back button in the detail pane.
   */
  detailOpen?: boolean;
  onBack?: () => void;
  backLabel?: string;
}

/** List + detail two-pane layout (Tickets inbox, Chat, Dossier). No routing inside. */
export function SplitLayout({
  list,
  detail,
  listLabel = 'List',
  detailLabel = 'Detail',
  listWidth = 'md',
  detailOpen = false,
  onBack,
  backLabel = 'Back to list',
  className,
  ...rest
}: SplitLayoutProps) {
  return (
    <div
      {...rest}
      data-list-width={listWidth}
      data-detail-open={detailOpen || undefined}
      className={cx('mn-split', className)}
    >
      <section aria-label={listLabel} className="mn-split__list">
        {list}
      </section>
      <section aria-label={detailLabel} className="mn-split__detail">
        {onBack ? (
          <div className="mn-split__back">
            <IconButton
              label={backLabel}
              icon={<ChevronRightGlyph style={{ transform: 'rotate(180deg)' }} />}
              onClick={onBack}
            />
          </div>
        ) : null}
        {detail}
      </section>
    </div>
  );
}
