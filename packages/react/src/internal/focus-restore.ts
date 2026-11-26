/**
 * Keeps keyboard focus through a pending submit.
 *
 * A form that disables its fields while a request runs takes focus away from
 * the field the person pressed Enter in: browsers move focus off an element
 * that stops being focusable, and it lands on `<body>`. When the request then
 * fails, the next Tab starts from the top of the page.
 *
 * Call `rememberFocus()` before the fields are disabled, and the function it
 * returns once they are enabled again (after the render that ends the pending
 * state, so inside a `setTimeout(…, 0)`).
 */
export function rememberFocus(): () => void {
  if (typeof document === 'undefined') return () => {};
  const active = document.activeElement;
  const target = active instanceof HTMLElement && active !== document.body ? active : null;
  return () => {
    if (!target || !target.isConnected) return;
    const now = document.activeElement;
    // The person (or the form) put focus somewhere else meanwhile: leave it there.
    if (now && now !== document.body && now !== target) return;
    target.focus();
  };
}
