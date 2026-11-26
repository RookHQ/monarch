'use client';

import {
  forwardRef,
  useEffect,
  useRef,
  type ForwardedRef,
  type HTMLAttributes,
  type RefObject,
} from 'react';
import { cx } from '../../../internal/cx.js';
import { useComposedRef } from '../../../internal/compose-refs.js';

/** Root class, declared before any other `mn-` string so dist/manifest.json reads it as the root. */
const ROOT = 'mn-focus-trap';

const FOCUSABLE = [
  'a[href]',
  'area[href]',
  'button:not(:disabled)',
  'input:not(:disabled):not([type="hidden"])',
  'select:not(:disabled)',
  'textarea:not(:disabled)',
  'iframe',
  'audio[controls]',
  'video[controls]',
  'summary',
  '[contenteditable]:not([contenteditable="false"])',
  '[tabindex]',
].join(',');

/**
 * The elements inside `container` that Tab reaches, in tab order: no disabled,
 * hidden or inert elements and nothing with a negative `tabindex`.
 */
export function getFocusableElements(container: HTMLElement): HTMLElement[] {
  const all = Array.from(container.querySelectorAll<HTMLElement>(FOCUSABLE)).filter(
    (el) =>
      el.tabIndex >= 0 &&
      !el.closest('[hidden],[inert],[aria-hidden="true"]') &&
      !(
        el instanceof HTMLInputElement &&
        el.type === 'radio' &&
        !el.checked &&
        radioGroupHasChecked(el)
      ),
  );
  // Positive tabindex first (in order), then document order, as browsers do.
  const positive = all.filter((el) => el.tabIndex > 0).sort((a, b) => a.tabIndex - b.tabIndex);
  return [...positive, ...all.filter((el) => el.tabIndex === 0)];
}

function radioGroupHasChecked(el: HTMLInputElement) {
  if (!el.name) return false;
  const scope = el.form ?? el.ownerDocument;
  return Array.from(scope.querySelectorAll<HTMLInputElement>('input[type="radio"]')).some(
    (r) => r.name === el.name && r.checked,
  );
}

export interface UseFocusTrapOptions {
  /** Trap only while true. Default true. Turning it off returns focus. */
  active?: boolean | undefined;
  /**
   * What gets focus when the trap turns on: `'first'` (default) the first
   * focusable element, `'container'` the container itself, a ref to a
   * specific element, or `false` to leave focus where it is (it is still
   * pulled inside on the next Tab).
   */
  initialFocus?: 'first' | 'container' | RefObject<HTMLElement | null> | false | undefined;
  /** Move focus back to the element that had it before, when the trap turns off. Default true. */
  returnFocus?: boolean | undefined;
  /** Called on Escape inside the trap (the trap does not close anything itself). */
  onEscape?: ((event: KeyboardEvent) => void) | undefined;
}

/** Active traps, innermost last: only the last one acts, so traps can nest. */
/**
 * True when `target` sits in a floating layer that belongs to the trap although
 * it is rendered elsewhere in the document: a Select list, a menu, a date
 * picker or a popover opened from inside and portalled to `<body>`. Such a
 * layer is recognised by the positioning wrapper Radix puts around it, or by
 * an `aria-controls` / `aria-owns` inside the trap that names it.
 */
function inOwnedLayer(container: HTMLElement, target: Element): boolean {
  if (target.closest('[data-radix-popper-content-wrapper]')) return true;
  for (let el: Element | null = target; el && el !== container.ownerDocument.body;) {
    if (el.id) {
      const id = typeof CSS !== 'undefined' && CSS.escape ? CSS.escape(el.id) : el.id;
      try {
        if (container.querySelector(`[aria-controls~="${id}"], [aria-owns~="${id}"]`)) return true;
      } catch {
        // An id no selector can hold: not a layer this trap can name.
      }
    }
    el = el.parentElement;
  }
  return false;
}

const stack: object[] = [];

/**
 * Keeps keyboard focus inside the element `ref` points to while `active`:
 * Tab and Shift+Tab wrap around, focus that escapes (a click outside, a
 * script) is brought back, and focus returns to where it was when the trap
 * turns off. Use it for custom popups, drawers and full-screen editors that
 * are not built on `Dialog` (which already traps focus).
 */
export function useFocusTrap(
  ref: RefObject<HTMLElement | null>,
  { active = true, initialFocus = 'first', returnFocus = true, onEscape }: UseFocusTrapOptions = {},
): void {
  const onEscapeRef = useRef(onEscape);
  onEscapeRef.current = onEscape;
  const initialRef = useRef(initialFocus);
  initialRef.current = initialFocus;
  const returnRef = useRef(returnFocus);
  returnRef.current = returnFocus;

  useEffect(() => {
    const container = ref.current;
    if (!active || !container) return;
    const token = {};
    stack.push(token);
    const doc = container.ownerDocument;
    const previous = doc.activeElement instanceof HTMLElement ? doc.activeElement : null;
    const isTop = () => stack[stack.length - 1] === token;

    const focusContainer = () => {
      if (!container.hasAttribute('tabindex')) container.setAttribute('tabindex', '-1');
      container.focus({ preventScroll: true });
    };
    const focusFirst = () => {
      const first = getFocusableElements(container)[0];
      if (first) first.focus();
      else focusContainer();
    };

    const initial = initialRef.current;
    if (!container.contains(doc.activeElement)) {
      if (initial === 'first') focusFirst();
      else if (initial === 'container') focusContainer();
      else if (initial && typeof initial === 'object') {
        if (initial.current) initial.current.focus();
        else focusFirst();
      }
    }

    let lastInside: HTMLElement | null =
      doc.activeElement instanceof HTMLElement && container.contains(doc.activeElement)
        ? doc.activeElement
        : null;

    const onKeyDown = (e: KeyboardEvent) => {
      if (!isTop()) return;
      if (e.key === 'Escape' && onEscapeRef.current) {
        onEscapeRef.current(e);
        return;
      }
      if (e.key !== 'Tab') return;
      const items = getFocusableElements(container);
      if (items.length === 0) {
        e.preventDefault();
        focusContainer();
        return;
      }
      const first = items[0] as HTMLElement;
      const last = items[items.length - 1] as HTMLElement;
      const current = doc.activeElement;
      const inside = current instanceof Node && container.contains(current);
      // A popup opened from inside handles Tab itself.
      if (!inside && current instanceof Element && inOwnedLayer(container, current)) return;
      if (e.shiftKey && (!inside || current === first || current === container)) {
        e.preventDefault();
        last.focus();
      } else if (!e.shiftKey && (!inside || current === last)) {
        e.preventDefault();
        first.focus();
      }
    };

    const onFocusIn = (e: FocusEvent) => {
      if (!isTop()) return;
      const target = e.target;
      if (target instanceof HTMLElement && container.contains(target)) {
        lastInside = target;
        return;
      }
      if (target instanceof Element && inOwnedLayer(container, target)) return;
      // Focus escaped: bring it back to where it was inside.
      if (lastInside && lastInside.isConnected && container.contains(lastInside))
        lastInside.focus();
      else focusFirst();
    };

    doc.addEventListener('keydown', onKeyDown, true);
    doc.addEventListener('focusin', onFocusIn, true);
    return () => {
      doc.removeEventListener('keydown', onKeyDown, true);
      doc.removeEventListener('focusin', onFocusIn, true);
      const i = stack.indexOf(token);
      if (i !== -1) stack.splice(i, 1);
      if (returnRef.current && previous && previous.isConnected) {
        // After the DOM update that turned the trap off, so the target is focusable again.
        const restore = () => previous.focus({ preventScroll: true });
        if (typeof queueMicrotask === 'function') queueMicrotask(restore);
        else restore();
      }
    };
  }, [active, ref]);
}

export interface FocusTrapProps extends HTMLAttributes<HTMLDivElement>, UseFocusTrapOptions {}

/**
 * A `<div>` that keeps keyboard focus inside itself while `active`, and
 * returns focus when it turns off or unmounts. Headless: it adds no visual
 * style. Prefer `Dialog`, `Sheet` or `Popover`, which trap focus already; use
 * this only for custom surfaces that are not built on them.
 */
export const FocusTrap = forwardRef<HTMLDivElement, FocusTrapProps>(function FocusTrap(
  { active = true, initialFocus, returnFocus, onEscape, className, ...rest },
  forwarded: ForwardedRef<HTMLDivElement>,
) {
  const ref = useRef<HTMLDivElement | null>(null);
  useFocusTrap(ref, { active, initialFocus, returnFocus, onEscape });
  const setRef = useComposedRef(ref, forwarded);
  return (
    <div
      ref={setRef}
      {...rest}
      className={cx(ROOT, className)}
      data-active={active ? '' : undefined}
    />
  );
});
