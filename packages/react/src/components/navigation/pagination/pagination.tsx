'use client';

import { forwardRef, type HTMLAttributes } from 'react';
import { cx } from '../../../internal/cx.js';
import { ChevronRightGlyph } from '../../../internal/icons.js';
import { numberFormat, useClientLocale } from '../../../internal/dates/client-time.js';
import { IconButton } from '../../actions/button/index.js';

/** Every string Pagination shows or announces. Numbers arrive already formatted for the locale. */
export interface PaginationMessages {
  /** Accessible name of the nav. Default "Pagination". */
  label: string;
  /** Default "Previous page". */
  previous: string;
  /** Default "Next page". */
  next: string;
  /** Name of a page button. Default "Page 3". */
  page: (page: string) => string;
  /** Compact range text. Default "1–20 of 143". */
  range: (start: string, end: string, total: string) => string;
  /** Compact text without `pageSize`/`totalItems`. Default "Page 1 of 8". */
  pageOf: (page: string, pageCount: string) => string;
}

export const PAGINATION_MESSAGES: PaginationMessages = {
  label: 'Pagination',
  previous: 'Previous page',
  next: 'Next page',
  page: (page) => `Page ${page}`,
  range: (start, end, total) => `${start}–${end} of ${total}`,
  pageOf: (page, pageCount) => `Page ${page} of ${pageCount}`,
};

export interface PaginationProps extends Omit<HTMLAttributes<HTMLElement>, 'children'> {
  /** 1-based current page. */
  page: number;
  pageCount: number;
  onPageChange: (page: number) => void;
  /** `full` shows numbered pages; `compact` shows "1–20 of 143" with arrows. */
  variant?: 'full' | 'compact';
  /** Needed for the compact range text. */
  pageSize?: number;
  totalItems?: number;
  /** Accessible name of the nav. Wins over `messages.label`. */
  label?: string;
  /** BCP 47 locale for the numbers. Default: `en-US` until hydrated, then the runtime locale. */
  locale?: string;
  /** Translated strings; missing keys fall back to English. */
  messages?: Partial<PaginationMessages>;
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

/**
 * Page navigation: numbered pages (`full`) or a range with arrows
 * (`compact`). Arrows point the reading direction, so they flip in RTL.
 */
export const Pagination = forwardRef<HTMLElement, PaginationProps>(function Pagination(
  {
    page,
    pageCount,
    onPageChange,
    variant = 'full',
    pageSize,
    totalItems,
    label,
    locale: localeProp,
    messages: messagesProp,
    className,
    ...rest
  },
  ref,
) {
  const m = { ...PAGINATION_MESSAGES, ...messagesProp };
  const locale = useClientLocale(localeProp);
  const n = (value: number) => numberFormat(locale).format(value);
  const name = label ?? m.label;
  const prev = (
    <IconButton
      label={m.previous}
      size="sm"
      disabled={page <= 1}
      onClick={() => onPageChange(page - 1)}
      icon={<ChevronRightGlyph className="mn-pagination__icon" data-direction="previous" />}
    />
  );
  const next = (
    <IconButton
      label={m.next}
      size="sm"
      disabled={page >= pageCount}
      onClick={() => onPageChange(page + 1)}
      icon={<ChevronRightGlyph className="mn-pagination__icon" data-direction="next" />}
    />
  );

  if (variant === 'compact') {
    const start =
      pageSize && totalItems ? Math.min((page - 1) * pageSize + 1, totalItems) : undefined;
    const end = pageSize && totalItems ? Math.min(page * pageSize, totalItems) : undefined;
    return (
      <nav
        ref={ref}
        {...rest}
        aria-label={name}
        className={cx('mn-pagination', className)}
        data-variant="compact"
      >
        <p className="mn-pagination__range" aria-live="polite">
          {start !== undefined && end !== undefined && totalItems !== undefined
            ? m.range(n(start), n(end), n(totalItems))
            : m.pageOf(n(page), n(pageCount))}
        </p>
        {prev}
        {next}
      </nav>
    );
  }

  return (
    <nav
      ref={ref}
      {...rest}
      aria-label={name}
      className={cx('mn-pagination', className)}
      data-variant="full"
    >
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
                aria-label={m.page(n(p))}
                onClick={() => onPageChange(p)}
              >
                {n(p)}
              </button>
            </li>
          ),
        )}
      </ol>
      {next}
    </nav>
  );
});
Pagination.displayName = 'Pagination';
