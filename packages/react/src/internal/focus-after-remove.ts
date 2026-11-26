'use client';

import { useCallback, useEffect, useRef } from 'react';

export interface FocusAfterRemoveOptions {
  /** Selector of the editor element that outlives the row (matched with `closest`). */
  root: string;
  /** Selector of one row inside the root. */
  row: string;
  /** Selector of the control to focus inside the row that takes the removed row's place. */
  target: string;
  /** Selector inside the root to focus when no row is left (for example the add button). */
  fallback?: string;
}

interface Pending extends FocusAfterRemoveOptions {
  origin: Element;
  rootNode: Element;
  index: number;
}

/**
 * Keeps keyboard focus in a list editor when a row is removed. Removing the
 * row unmounts the button that had focus, which drops focus to the page body
 * and sends keyboard and screen reader users back to the top of the page.
 *
 * Call the returned function with the control that was activated, right
 * before the change is reported. After the next render, when that control is
 * gone and nothing else took focus, focus goes to the row now at the same
 * position (or the last row), or to `fallback` when the list is empty. A
 * control outside the rows that disappears (an add button hidden at the
 * limit) sends focus to the last row.
 */
export function useFocusAfterRemove(): (origin: Element, options: FocusAfterRemoveOptions) => void {
  const pending = useRef<Pending | null>(null);

  useEffect(() => {
    const p = pending.current;
    if (!p) return;
    pending.current = null;
    // The change was not applied, or the whole editor went away.
    if (p.origin.isConnected || !p.rootNode.isConnected) return;
    const doc = p.rootNode.ownerDocument;
    const active = doc.activeElement;
    if (active && active !== doc.body) return;
    const rows = [...p.rootNode.querySelectorAll(p.row)];
    const row = rows[Math.min(p.index, rows.length - 1)];
    const inRow = row?.querySelector<HTMLElement>(p.target);
    // A disabled control cannot take focus (remove buttons at the minimum count).
    const next =
      inRow && !inRow.matches(':disabled')
        ? inRow
        : p.fallback
          ? p.rootNode.querySelector<HTMLElement>(p.fallback)
          : null;
    next?.focus();
  });

  return useCallback((origin: Element, options: FocusAfterRemoveOptions) => {
    const rootNode = origin.closest(options.root);
    if (!rootNode) return;
    // A control outside the rows (an add button that hides at the limit) leads to the last row.
    const row = origin.closest(options.row);
    const index = row
      ? Math.max(0, [...rootNode.querySelectorAll(options.row)].indexOf(row))
      : Number.MAX_SAFE_INTEGER;
    pending.current = { ...options, origin, rootNode, index };
  }, []);
}
