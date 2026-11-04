/**
 * Dismissal rules shared by the modal overlays (Dialog, Sheet). Radix calls
 * these handlers before it closes the overlay; calling `preventDefault()`
 * keeps it open.
 */

/**
 * An element that owns an open popup of its own (a combobox or a menu button
 * with `aria-expanded="true"`). Escape pressed there belongs to that popup:
 * closing the whole overlay as well would throw away the user's work.
 *
 * Radix popups (Monarch Select, Combobox, Popover, DropdownMenu) are already
 * stacked layers and never reach this check; it covers hand-built ones.
 */
const OPEN_POPUP_OWNER =
  '[role="combobox"][aria-expanded="true"], [aria-haspopup][aria-expanded="true"]:not([aria-haspopup="false"])';

export function ownsOpenPopup(target: EventTarget | null): boolean {
  return (
    typeof Element !== 'undefined' &&
    target instanceof Element &&
    target.closest(OPEN_POPUP_OWNER) !== null
  );
}

export interface DismissRules {
  /** Nothing dismisses the overlay (an action is in progress). */
  busy: boolean;
  /** A pointer down outside the content may close it. */
  closeOnOutsideClick: boolean;
  /** Escape is ignored while focus is on an element whose own popup is open. */
  escapeGuard: boolean;
}

/** Wraps a consumer's Radix handlers with the dismissal rules. */
export function dismissHandlers<K extends Event, O extends Event>(
  rules: DismissRules,
  handlers: {
    onEscapeKeyDown?: ((event: K) => void) | undefined;
    onInteractOutside?: ((event: O) => void) | undefined;
  },
): { onEscapeKeyDown: (event: K) => void; onInteractOutside: (event: O) => void } {
  return {
    onEscapeKeyDown(event) {
      handlers.onEscapeKeyDown?.(event);
      if (rules.busy || (rules.escapeGuard && ownsOpenPopup(event.target))) {
        event.preventDefault();
      }
    },
    onInteractOutside(event) {
      handlers.onInteractOutside?.(event);
      if (rules.busy || !rules.closeOnOutsideClick) event.preventDefault();
    },
  };
}
