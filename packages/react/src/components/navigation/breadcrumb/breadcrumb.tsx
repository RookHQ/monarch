'use client';

import { Fragment, type AnchorHTMLAttributes, type ElementType, type ReactNode } from 'react';
import { cx } from '../../../internal/cx.js';
import { ChevronRightGlyph } from '../../../internal/icons.js';
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

export interface BreadcrumbProps {
  items: BreadcrumbItem[];
  /** Show at most this many items; the middle ones collapse into a menu. Default 4. */
  maxVisible?: number;
  /** Link component, e.g. Next.js `Link`. Defaults to `a`. */
  linkAs?: ElementType<AnchorHTMLAttributes<HTMLAnchorElement>>;
  /** Accessible name of the nav. */
  label?: string;
  className?: string;
}

/** Location trail. The last item is the current page (`aria-current="page"`). */
export function Breadcrumb({
  items,
  maxVisible = 4,
  linkAs: LinkComp = 'a',
  label = 'Breadcrumb',
  className,
}: BreadcrumbProps) {
  const collapse = items.length > maxVisible;
  const head = collapse ? items.slice(0, 1) : items;
  const hidden = collapse ? items.slice(1, items.length - (maxVisible - 2)) : [];
  const tail = collapse ? items.slice(items.length - (maxVisible - 2)) : [];
  const visible: Array<BreadcrumbItem | 'menu'> = collapse ? [...head, 'menu', ...tail] : head;

  return (
    <nav aria-label={label} className={cx('mn-breadcrumb', className)}>
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
                      aria-label={`Show ${hidden.length} more levels`}
                    >
                      …
                    </DropdownMenuTrigger>
                    <DropdownMenuContent>
                      {hidden.map((h, j) => (
                        <DropdownMenuItem key={j} asChild>
                          <LinkComp href={h.href}>{h.label}</LinkComp>
                        </DropdownMenuItem>
                      ))}
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
}
