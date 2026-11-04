import { useCallback, useEffect, useRef, useState, type MutableRefObject, type Ref } from 'react';

/**
 * Where focus goes when a dismissible message removes itself. Without this,
 * pressing its close button drops focus to <body> and keyboard users lose
 * their place (WCAG 2.4.3 Focus Order).
 */
const TABBABLE = [
  'a[href]',
  'area[href]',
  'button:not([disabled])',
  'input:not([disabled]):not([type="hidden"])',
  'select:not([disabled])',
  'textarea:not([disabled])',
  'iframe',
  '[contenteditable=""]',
  '[contenteditable="true"]',
  '[tabindex]',
].join(',');

function isTabbable(el: Element): el is HTMLElement {
  if (!(el instanceof HTMLElement)) return false;
  if (el.tabIndex < 0 || el.closest('[hidden], [inert], [aria-hidden="true"]')) return false;
  return true;
}

/**
 * The first tabbable element after `node` in document order (outside it), or
 * the last one before it. Called before the node is removed.
 */
export function focusTargetAfterDismiss(node: Element): HTMLElement | null {
  const doc = node.ownerDocument;
  const all = [...doc.querySelectorAll(TABBABLE)].filter(
    (el): el is HTMLElement => !node.contains(el) && isTabbable(el),
  );
  const after = all.find(
    (el) => node.compareDocumentPosition(el) & Node.DOCUMENT_POSITION_FOLLOWING,
  );
  if (after) return after;
  const before = all.filter(
    (el) => node.compareDocumentPosition(el) & Node.DOCUMENT_POSITION_PRECEDING,
  );
  return before[before.length - 1] ?? null;
}

/** Sets every ref to the same node. */
export function setRefs<T>(node: T, ...refs: (Ref<T> | undefined)[]) {
  for (const r of refs) {
    if (typeof r === 'function') r(node);
    else if (r) (r as MutableRefObject<T>).current = node;
  }
}

export interface DismissibleOptions {
  open?: boolean | undefined;
  defaultOpen?: boolean | undefined;
  onDismiss?: (() => void) | undefined;
}

/**
 * Open state for a message the user can close: controlled (`open`) or
 * uncontrolled (`defaultOpen`). `dismiss()` calls `onDismiss`, hides the
 * message when uncontrolled, and moves focus to the next tabbable element if
 * focus was inside it. Persistence ("don't show again") stays with the
 * product, in `onDismiss`.
 */
export function useDismissible<T extends HTMLElement>({
  open,
  defaultOpen = true,
  onDismiss,
}: DismissibleOptions) {
  const [inner, setInner] = useState(defaultOpen);
  const shown = open ?? inner;
  const node = useRef<T | null>(null);
  const pending = useRef<HTMLElement | null>(null);
  const [tick, setTick] = useState(0);

  const dismiss = useCallback(() => {
    const root = node.current;
    pending.current =
      root && root.contains(root.ownerDocument.activeElement)
        ? focusTargetAfterDismiss(root)
        : null;
    if (open === undefined) setInner(false);
    setTick((n) => n + 1);
    onDismiss?.();
  }, [open, onDismiss]);

  useEffect(() => {
    if (!pending.current) return;
    // A controlled parent that keeps the message open keeps focus where it is.
    if (!shown) pending.current.focus();
    pending.current = null;
  }, [shown, tick]);

  return { shown, dismiss, node };
}
