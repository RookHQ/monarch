'use client';

import { cx } from '../../../internal/cx.js';
import { ChevronRightGlyph } from '../../../internal/icons.js';
import { IconButton } from '../../actions/button/index.js';

export interface PaginationProps {
  /** 1-based current page. */
  page: number;
  pageCount: number;
  onPageChange: (page: number) => void;
  /** `full` shows numbered pages; `compact` shows "1–20 of 143" with arrows. */
  variant?: 'full' | 'compact';
  /** Needed for the compact range text. */
  pageSize?: number;
  totalItems?: number;
  /** Accessible name of the nav. */
  label?: string;
  className?: string;
}

/** Which page numbers to show: first, last, and a window around the current page. */
export function pageWindow(page: number, pageCount: number, radius = 1): Array<number | 'gap'> {
  if (pageCount <= 7) return Array.from({ length: pageCount }, (_, i) => i + 1);
  const pages = new Set<number>([1, pageCount]);
  for (let p = page - radius; p <= page + radius; p++) if (p >= 1 && p <= pageCount) pages.add(p);
  if (page <= 3) [2, 3, 4].forEach((p) => pages.add(p));
  if (page >= pageCount - 2)
    [pageCount - 3, pageCount - 2, pageCount - 1].forEach((p) => pages.add(p));
  const sorted = [...pages].sort((a, b) => a - b);
  const out: Array<number | 'gap'> = [];
  sorted.forEach((p, i) => {
    const prev = sorted[i - 1];
    if (prev !== undefined && p - prev > 1) out.push('gap');
    out.push(p);
  });
  return out;
}

export function Pagination({
  page,
  pageCount,
  onPageChange,
  variant = 'full',
  pageSize,
  totalItems,
  label = 'Pagination',
  className,
}: PaginationProps) {
  const prev = (
    <IconButton
      label="Previous page"
      size="sm"
      disabled={page <= 1}
      onClick={() => onPageChange(page - 1)}
      icon={<ChevronRightGlyph style={{ transform: 'rotate(180deg)' }} />}
    />
  );
  const next = (
    <IconButton
      label="Next page"
      size="sm"
      disabled={page >= pageCount}
      onClick={() => onPageChange(page + 1)}
      icon={<ChevronRightGlyph />}
    />
  );

  if (variant === 'compact') {
    const start =
      pageSize && totalItems ? Math.min((page - 1) * pageSize + 1, totalItems) : undefined;
    const end = pageSize && totalItems ? Math.min(page * pageSize, totalItems) : undefined;
    return (
      <nav aria-label={label} className={cx('mn-pagination', className)} data-variant="compact">
        <p className="mn-pagination__range" aria-live="polite">
          {start !== undefined && end !== undefined
            ? `${start}–${end} of ${totalItems}`
            : `Page ${page} of ${pageCount}`}
        </p>
        {prev}
        {next}
      </nav>
    );
  }

  return (
    <nav aria-label={label} className={cx('mn-pagination', className)} data-variant="full">
      {prev}
      <ol className="mn-pagination__pages">
        {pageWindow(page, pageCount).map((p, i) =>
          p === 'gap' ? (
            <li key={`gap-${i}`} className="mn-pagination__gap" aria-hidden>
              …
            </li>
          ) : (
            <li key={p}>
              <button
                type="button"
                className="mn-pagination__page"
                aria-current={p === page ? 'page' : undefined}
                aria-label={`Page ${p}`}
                onClick={() => onPageChange(p)}
              >
                {p}
              </button>
            </li>
          ),
        )}
      </ol>
      {next}
    </nav>
  );
}
