'use client';

import {
  forwardRef,
  useCallback,
  type HTMLAttributes,
  type KeyboardEvent,
  type ReactNode,
} from 'react';
import { cx } from '../../../internal/cx.js';
import { useDirection } from '../../../internal/direction.js';

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
  'a[href],button:not([disabled]),input:not([disabled]),select:not([disabled]),textarea:not([disabled]),[tabindex]:not([tabindex="-1"])';

/** Controls that use the arrow keys, Home and End themselves: a caret or a list of options. */
function ownsCaretKeys(target: HTMLElement): boolean {
  if (target instanceof HTMLInputElement)
    return !['checkbox', 'radio', 'button', 'submit', 'reset', 'file', 'image'].includes(
      target.type,
    );
  return (
    target instanceof HTMLTextAreaElement ||
    target instanceof HTMLSelectElement ||
    target.isContentEditable
  );
}

/**
 * `role="toolbar"` with roving focus: Tab enters the toolbar, Left/Right
 * move between controls, Home/End jump. Text inputs keep their own arrow keys.
 */
export const Toolbar = forwardRef<HTMLDivElement, ToolbarProps>(function Toolbar(
  { label, search, filters, end, className, children, onKeyDown, ...rest },
  ref,
) {
  const readDir = useDirection();
  const handleKey = useCallback(
    (e: KeyboardEvent<HTMLDivElement>) => {
      onKeyDown?.(e);
      if (e.defaultPrevented) return;
      if (!['ArrowLeft', 'ArrowRight', 'Home', 'End'].includes(e.key)) return;
      // Alt+Left is the browser's Back; modified keys are never the toolbar's.
      if (e.altKey || e.ctrlKey || e.metaKey) return;
      // A search field keeps Home and End for its caret, as it keeps the arrows.
      if (ownsCaretKeys(e.target as HTMLElement)) return;
      // In a right-to-left toolbar the left arrow moves to the next control.
      const rtl = readDir(e.currentTarget) === 'rtl';
      const forward = rtl ? 'ArrowLeft' : 'ArrowRight';
      const back = rtl ? 'ArrowRight' : 'ArrowLeft';
      const items = Array.from(e.currentTarget.querySelectorAll<HTMLElement>(FOCUSABLE)).filter(
        (el) => el.closest('[role="toolbar"]') === e.currentTarget,
      );
      if (items.length === 0) return;
      const i = items.indexOf(document.activeElement as HTMLElement);
      let next = i;
      if (e.key === forward) next = Math.min(i + 1, items.length - 1);
      if (e.key === back) next = Math.max(i - 1, 0);
      if (e.key === 'Home') next = 0;
      if (e.key === 'End') next = items.length - 1;
      if (next !== i) {
        e.preventDefault();
        items[next]?.focus();
      }
    },
    [onKeyDown, readDir],
  );
  return (
    <div className={cx('mn-toolbar-wrap', className)}>
      <div
        {...rest}
        ref={ref}
        role="toolbar"
        aria-label={label}
        className="mn-toolbar"
        onKeyDown={handleKey}
      >
        {search ? <div className="mn-toolbar__search">{search}</div> : null}
        {children ? <div className="mn-toolbar__group">{children}</div> : null}
        {end ? <div className="mn-toolbar__end">{end}</div> : null}
      </div>
      {filters ? <div className="mn-toolbar__filters">{filters}</div> : null}
    </div>
  );
});
Toolbar.displayName = 'Toolbar';

/** Visual grouping inside a Toolbar. */
export const ToolbarGroup = forwardRef<HTMLDivElement, HTMLAttributes<HTMLDivElement>>(
  function ToolbarGroup({ className, ...rest }, ref) {
    return <div {...rest} ref={ref} role="group" className={cx('mn-toolbar__group', className)} />;
  },
);
ToolbarGroup.displayName = 'ToolbarGroup';
