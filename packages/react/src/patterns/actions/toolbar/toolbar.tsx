'use client';

import { useCallback, type HTMLAttributes, type KeyboardEvent, type ReactNode } from 'react';
import { cx } from '../../../internal/cx.js';

export interface ToolbarProps extends HTMLAttributes<HTMLDivElement> {
  /** Accessible name, e.g. "Ticket list tools". Required by the toolbar role. */
  label: string;
  /** Search or filter input area on the leading edge. */
  search?: ReactNode;
  /** Filter chips row rendered under the toolbar. */
  filters?: ReactNode;
  /** Controls on the trailing edge (view switch, density, export). */
  end?: ReactNode;
  children?: ReactNode;
}

const FOCUSABLE =
  'a[href],button:not([disabled]),input:not([disabled]),select:not([disabled]),[tabindex]:not([tabindex="-1"])';

/**
 * `role="toolbar"` with roving focus: Tab enters the toolbar, Left/Right
 * move between controls, Home/End jump. Text inputs keep their own arrow keys.
 */
export function Toolbar({
  label,
  search,
  filters,
  end,
  className,
  children,
  onKeyDown,
  ...rest
}: ToolbarProps) {
  const handleKey = useCallback(
    (e: KeyboardEvent<HTMLDivElement>) => {
      onKeyDown?.(e);
      if (e.defaultPrevented) return;
      const target = e.target as HTMLElement;
      const isText =
        target instanceof HTMLInputElement &&
        !['checkbox', 'radio', 'button'].includes(target.type);
      if (isText && (e.key === 'ArrowLeft' || e.key === 'ArrowRight')) return;
      if (!['ArrowLeft', 'ArrowRight', 'Home', 'End'].includes(e.key)) return;
      const items = Array.from(e.currentTarget.querySelectorAll<HTMLElement>(FOCUSABLE)).filter(
        (el) => el.closest('[role="toolbar"]') === e.currentTarget,
      );
      if (items.length === 0) return;
      const i = items.indexOf(document.activeElement as HTMLElement);
      let next = i;
      if (e.key === 'ArrowRight') next = Math.min(i + 1, items.length - 1);
      if (e.key === 'ArrowLeft') next = Math.max(i - 1, 0);
      if (e.key === 'Home') next = 0;
      if (e.key === 'End') next = items.length - 1;
      if (next !== i) {
        e.preventDefault();
        items[next]?.focus();
      }
    },
    [onKeyDown],
  );
  return (
    <div className={cx('mn-toolbar-wrap', className)}>
      <div {...rest} role="toolbar" aria-label={label} className="mn-toolbar" onKeyDown={handleKey}>
        {search ? <div className="mn-toolbar__search">{search}</div> : null}
        {children ? <div className="mn-toolbar__group">{children}</div> : null}
        {end ? <div className="mn-toolbar__end">{end}</div> : null}
      </div>
      {filters ? <div className="mn-toolbar__filters">{filters}</div> : null}
    </div>
  );
}

/** Visual grouping inside a Toolbar. */
export function ToolbarGroup({ className, ...rest }: HTMLAttributes<HTMLDivElement>) {
  return <div {...rest} role="group" className={cx('mn-toolbar__group', className)} />;
}
