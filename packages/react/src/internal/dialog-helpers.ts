// Helpers shared by the promise-based dialogs (ConfirmDialog, PromptDialog,
// ChoiceDialog, StepUpAuthDialog, WizardDialog).

/** True for promises and other thenables, so a callback can be sync or async. */
export function isThenable(value: unknown): value is PromiseLike<unknown> {
  return (
    typeof value === 'object' &&
    value !== null &&
    typeof (value as { then?: unknown }).then === 'function'
  );
}

/**
 * Where focus should return when the dialog closes: the focused element, or,
 * when that is an item of a menu that is about to close, the menu's trigger.
 */
export function focusReturnTarget(): HTMLElement | null {
  if (typeof document === 'undefined') return null;
  const active = document.activeElement instanceof HTMLElement ? document.activeElement : null;
  const menuOwner = active?.closest('[role="menu"]')?.getAttribute('aria-labelledby');
  const trigger = menuOwner ? document.getElementById(menuOwner) : null;
  return trigger ?? active;
}
