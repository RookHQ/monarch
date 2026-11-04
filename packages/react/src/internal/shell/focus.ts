/** Focus helpers shared by the shell's frame hosts. */

const FOCUSABLE =
  'a[href], area[href], button:not([disabled]), input:not([disabled]):not([type="hidden"]), select:not([disabled]), textarea:not([disabled]), iframe, [contenteditable="true"], [tabindex]:not([tabindex="-1"])';

function visible(el: HTMLElement): boolean {
  // jsdom has no layout: treat everything as visible there.
  if (el.hidden || el.closest('[hidden],[inert]')) return false;
  return (
    el.getClientRects().length > 0 ||
    typeof navigator === 'undefined' ||
    /jsdom/i.test(navigator.userAgent)
  );
}

/** Tabbable elements inside `root`, in document order. */
export function tabbables(root: ParentNode): HTMLElement[] {
  return Array.from(root.querySelectorAll<HTMLElement>(FOCUSABLE)).filter(
    (el) => el.tabIndex >= 0 && visible(el),
  );
}

/**
 * Moves focus to the tabbable element just after (`forward`) or before
 * (`backward`) `anchor` in the document. Returns false when there is none.
 */
export function focusBeside(anchor: HTMLElement, direction: 'forward' | 'backward'): boolean {
  const all = tabbables(anchor.ownerDocument);
  const after = direction === 'forward';
  const pick = after
    ? all.find(
        (el) =>
          !anchor.contains(el) &&
          anchor.compareDocumentPosition(el) & Node.DOCUMENT_POSITION_FOLLOWING,
      )
    : [...all]
        .reverse()
        .find(
          (el) =>
            !anchor.contains(el) &&
            anchor.compareDocumentPosition(el) & Node.DOCUMENT_POSITION_PRECEDING,
        );
  if (!pick) return false;
  pick.focus();
  return true;
}
