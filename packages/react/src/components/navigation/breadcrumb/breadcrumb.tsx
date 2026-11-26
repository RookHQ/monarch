'use client';

import { Fragment, forwardRef, type HTMLAttributes, type ReactNode } from 'react';
import { cx } from '../../../internal/cx.js';
import type { MonarchLinkComponent } from '../../../internal/link.js';
import { ChevronRightGlyph } from '../../../internal/icons.js';
import { numberFormat, useClientLocale } from '../../../internal/dates/client-time.js';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from '../../overlays/dropdown-menu/index.js';

export interface BreadcrumbItem {
  label: ReactNode;
  /** Omit on the current page. */
  href?: string;
}

/** Every string Breadcrumb announces. Numbers arrive already formatted for the locale. */
export interface BreadcrumbMessages {
  /** Accessible name of the nav. Default "Breadcrumb". */
  label: string;
  /** Name of the collapsed-levels menu button. Default "Show 2 more levels". */
  more: (count: string) => string;
}

export const BREADCRUMB_MESSAGES: BreadcrumbMessages = {
  label: 'Breadcrumb',
  more: (count) => `Show ${count} more levels`,
};

export interface BreadcrumbProps extends Omit<HTMLAttributes<HTMLElement>, 'children'> {
  items: BreadcrumbItem[];
  /** Show at most this many items; the middle ones collapse into a menu. Default 4. */
  maxVisible?: number;
  /** Link component, e.g. Next.js `Link`. Defaults to `a`. */
  linkAs?: MonarchLinkComponent;
  /** Accessible name of the nav. Wins over `messages.label`. */
  label?: string;
  /** BCP 47 locale for the hidden-level count. Default: `en-US` until hydrated, then the runtime locale. */
  locale?: string;
  /** Translated strings; missing keys fall back to English. */
  messages?: Partial<BreadcrumbMessages>;
}

/**
 * Location trail. The last item is the current page (`aria-current="page"`).
 * Separators point the reading direction, so they flip in RTL.
 */
export const Breadcrumb = forwardRef<HTMLElement, BreadcrumbProps>(function Breadcrumb(
  {
    items,
    maxVisible = 4,
    linkAs: LinkComp = 'a',
    label,
    locale: localeProp,
    messages: messagesProp,
    className,
    ...rest
  },
  ref,
) {
  const m = { ...BREADCRUMB_MESSAGES, ...messagesProp };
  const locale = useClientLocale(localeProp);
  const collapse = items.length > maxVisible;
  const head = collapse ? items.slice(0, 1) : items;
  const hidden = collapse ? items.slice(1, items.length - (maxVisible - 2)) : [];
  const tail = collapse ? items.slice(items.length - (maxVisible - 2)) : [];
  const visible: Array<BreadcrumbItem | 'menu'> = collapse ? [...head, 'menu', ...tail] : head;

  return (
    <nav
      ref={ref}
      {...rest}
      aria-label={label ?? m.label}
      className={cx('mn-breadcrumb', className)}
    >
      <ol className="mn-breadcrumb__list">
        {visible.map((item, i) => {
          const last = i === visible.length - 1;
          return (
            <Fragment key={i}>
              <li className="mn-breadcrumb__item">
                {item === 'menu' ? (
                  <DropdownMenu>
                    <DropdownMenuTrigger
                      className="mn-breadcrumb__more"
                      aria-label={m.more(numberFormat(locale).format(hidden.length))}
                    >
                      …
                    </DropdownMenuTrigger>
                    <DropdownMenuContent>
                      {hidden.map((h, j) =>
                        h.href ? (
                          <DropdownMenuItem key={j} asChild>
                            <LinkComp href={h.href}>{h.label}</LinkComp>
                          </DropdownMenuItem>
                        ) : (
                          // A level without a page is shown, not linked.
                          <DropdownMenuItem key={j} disabled>
                            {h.label}
                          </DropdownMenuItem>
                        ),
                      )}
                    </DropdownMenuContent>
                  </DropdownMenu>
                ) : last || !item.href ? (
                  <span className="mn-breadcrumb__current" aria-current={last ? 'page' : undefined}>
                    {item.label}
                  </span>
                ) : (
                  <LinkComp href={item.href} className="mn-breadcrumb__link">
                    {item.label}
                  </LinkComp>
                )}
              </li>
              {!last ? (
                <li aria-hidden className="mn-breadcrumb__sep">
                  <ChevronRightGlyph />
                </li>
              ) : null}
            </Fragment>
          );
        })}
      </ol>
    </nav>
  );
});
Breadcrumb.displayName = 'Breadcrumb';
