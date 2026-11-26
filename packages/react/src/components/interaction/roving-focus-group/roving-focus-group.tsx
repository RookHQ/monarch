'use client';

import {
  forwardRef,
  useCallback,
  useEffect,
  useRef,
  type ForwardedRef,
  type HTMLAttributes,
  type KeyboardEvent as ReactKeyboardEvent,
  type RefObject,
} from 'react';
import { useIsomorphicLayoutEffect } from '../../../internal/layout-effect.js';
import { cx } from '../../../internal/cx.js';
import { useComposedRef } from '../../../internal/compose-refs.js';
import { useDirection } from '../../../internal/direction.js';

/** Root class, declared before any other `mn-` string so dist/manifest.json reads it as the root. */
const ROOT = 'mn-roving-focus-group';

/** Mark an element with this attribute to make it an item of the nearest roving focus group. */
export const ROVING_ITEM_ATTRIBUTE = 'data-mn-roving-item';

/** Which arrow keys move focus: one axis, both axes in one sequence, or a 2D grid. */
export type RovingOrientation = 'horizontal' | 'vertical' | 'both' | 'grid';

export interface UseRovingFocusOptions {
  /**
   * `horizontal` (default, toolbars): Left/Right. `vertical` (menus, lists):
   * Up/Down. `both`: all four arrows step through one sequence. `grid`:
   * Left/Right move within a row and Up/Down move by `columns`.
   */
  orientation?: RovingOrientation | undefined;
  /** Wrap from the last item to the first and back. Default true (ignored in `grid`). */
  loop?: boolean | undefined;
  /** Items per row in `grid` orientation. Without it, rows are measured from the layout. */
  columns?: number | undefined;
  /** Text direction. Left and Right swap in `rtl`. Default: read from the container. */
  dir?: 'ltr' | 'rtl' | undefined;
  /**
   * Which descendants are items. Default: elements with `data-mn-roving-item`
   * or, when there are none, every focusable control in the group.
   */
  itemSelector?: string | undefined;
  /** Called when the active item (the one with tabindex 0) changes, with its index. */
  onActiveIndexChange?: ((index: number) => void) | undefined;
}

/** Every roving container carries this attribute, so nested groups keep their own items. */
const GROUP = '[data-mn-roving-group]';

const CONTROLS =
  'button, a[href], input:not([type="hidden"]), select, textarea, [tabindex], [contenteditable]:not([contenteditable="false"])';

const isDisabled = (el: HTMLElement) =>
  (el as HTMLButtonElement).disabled === true || el.getAttribute('aria-disabled') === 'true';

/** Keys a text field needs for its own caret; arrows there do not move the group. */
function ownsArrows(el: Element | null, key: string) {
  if (!(el instanceof HTMLElement)) return false;
  if (el.isContentEditable || el instanceof HTMLTextAreaElement || el instanceof HTMLSelectElement)
    return true;
  if (el instanceof HTMLInputElement)
    return (
      !['checkbox', 'radio', 'button', 'submit', 'reset', 'image', 'color', 'file'].includes(
        el.type,
      ) &&
      (key === 'ArrowLeft' || key === 'ArrowRight' || key === 'Home' || key === 'End')
    );
  return false;
}

function measuredColumns(items: HTMLElement[]) {
  const first = items[0];
  if (!first) return 1;
  const top = first.offsetTop;
  let n = 0;
  for (const el of items) {
    if (el.offsetTop !== top) break;
    n++;
  }
  return Math.max(1, n);
}

/**
 * Roving tabindex for a container: exactly one item is in the tab order
 * (tabindex 0, the rest -1), the arrow keys move focus between items, and
 * Home/End jump to the ends. Returns the `onKeyDown` handler to put on the
 * container. Items added or removed later are picked up automatically.
 */
export function useRovingFocus(
  ref: RefObject<HTMLElement | null>,
  {
    orientation = 'horizontal',
    loop = true,
    columns,
    dir,
    itemSelector,
    onActiveIndexChange,
  }: UseRovingFocusOptions = {},
): { onKeyDown: (event: ReactKeyboardEvent<HTMLElement>) => void } {
  const readDir = useDirection(dir);
  const active = useRef<HTMLElement | null>(null);
  const onChangeRef = useRef(onActiveIndexChange);
  onChangeRef.current = onActiveIndexChange;

  const items = useCallback((): HTMLElement[] => {
    const root = ref.current;
    if (!root) return [];
    // Items of a nested group belong to that group, not this one.
    const own = (el: HTMLElement) => el.parentElement?.closest(GROUP) === root;
    const select = (sel: string) =>
      Array.from(root.querySelectorAll<HTMLElement>(sel)).filter((el) => own(el));
    if (itemSelector) return select(itemSelector);
    const marked = select(`[${ROVING_ITEM_ATTRIBUTE}]`);
    return marked.length > 0 ? marked : select(CONTROLS);
  }, [ref, itemSelector]);

  const sync = useCallback(() => {
    const list = items();
    const enabled = list.filter((el) => !isDisabled(el));
    let current = active.current;
    if (!current || !enabled.includes(current)) {
      current =
        enabled.find(
          (el) =>
            el.getAttribute('aria-selected') === 'true' ||
            el.getAttribute('aria-checked') === 'true' ||
            el.hasAttribute('aria-current'),
        ) ??
        enabled[0] ??
        null;
      active.current = current;
    }
    for (const el of list) {
      const want = el === current ? '0' : '-1';
      if (el.getAttribute('tabindex') !== want) el.setAttribute('tabindex', want);
    }
  }, [items]);

  useIsomorphicLayoutEffect(() => {
    ref.current?.setAttribute('data-mn-roving-group', '');
    sync();
  });

  useEffect(() => {
    const root = ref.current;
    if (!root) return;
    const onFocusIn = (e: FocusEvent) => {
      const target = e.target as HTMLElement;
      const list = items();
      const item = list.find((el) => el === target || el.contains(target));
      if (!item || isDisabled(item) || item === active.current) return;
      active.current = item;
      sync();
      onChangeRef.current?.(list.indexOf(item));
    };
    root.addEventListener('focusin', onFocusIn);
    const observer =
      typeof MutationObserver === 'function' ? new MutationObserver(() => sync()) : null;
    observer?.observe(root, {
      childList: true,
      subtree: true,
      attributes: true,
      attributeFilter: ['disabled', 'aria-disabled'],
    });
    return () => {
      root.removeEventListener('focusin', onFocusIn);
      observer?.disconnect();
    };
  }, [ref, items, sync]);

  const onKeyDown = useCallback(
    (e: ReactKeyboardEvent<HTMLElement>) => {
      if (e.defaultPrevented || e.altKey || e.metaKey) return;
      const root = ref.current;
      if (!root) return;
      if (ownsArrows(e.target as Element, e.key)) return;
      const list = items();
      const enabled = list.filter((el) => !isDisabled(el));
      const from = enabled.findIndex((el) => el === e.target || el.contains(e.target as Node));
      if (from === -1) return;
      const rtl = readDir(root) === 'rtl';
      const next = rtl ? 'ArrowLeft' : 'ArrowRight';
      const prev = rtl ? 'ArrowRight' : 'ArrowLeft';
      const last = enabled.length - 1;
      let to: number | null = null;

      if (orientation === 'grid') {
        // Disabled cells keep their place in a grid, so step over the full list.
        const all = list;
        const cols = columns ?? measuredColumns(all);
        const at = all.indexOf(enabled[from] as HTMLElement);
        const row = Math.floor(at / cols);
        const step = (start: number, delta: number, bound: (i: number) => boolean) => {
          for (let i = start + delta; bound(i); i += delta) {
            const el = all[i];
            if (el && !isDisabled(el)) return enabled.indexOf(el);
          }
          return null;
        };
        const rowStart = row * cols;
        const rowEnd = Math.min(rowStart + cols, all.length) - 1;
        switch (e.key) {
          case next:
            to = step(at, 1, (i) => i <= rowEnd);
            break;
          case prev:
            to = step(at, -1, (i) => i >= rowStart);
            break;
          case 'ArrowDown':
            to = step(at, cols, (i) => i < all.length);
            break;
          case 'ArrowUp':
            to = step(at, -cols, (i) => i >= 0);
            break;
          case 'Home':
            to = e.ctrlKey ? 0 : step(rowStart - 1, 1, (i) => i <= rowEnd);
            break;
          case 'End':
            to = e.ctrlKey ? last : step(rowEnd + 1, -1, (i) => i >= rowStart);
            break;
          default:
            return;
        }
      } else {
        const forward =
          (orientation !== 'vertical' && e.key === next) ||
          (orientation !== 'horizontal' && e.key === 'ArrowDown');
        const back =
          (orientation !== 'vertical' && e.key === prev) ||
          (orientation !== 'horizontal' && e.key === 'ArrowUp');
        if (forward) to = from === last ? (loop ? 0 : last) : from + 1;
        else if (back) to = from === 0 ? (loop ? last : 0) : from - 1;
        else if (e.key === 'Home') to = 0;
        else if (e.key === 'End') to = last;
        else return;
      }

      e.preventDefault();
      const target = to === null ? undefined : enabled[to];
      if (target && target !== enabled[from]) target.focus();
    },
    [ref, items, orientation, loop, columns, readDir],
  );

  return { onKeyDown };
}

export interface RovingFocusGroupProps
  extends Omit<HTMLAttributes<HTMLDivElement>, 'dir'>, UseRovingFocusOptions {}

/** Roles that accept `aria-orientation`. */
const ORIENTED_ROLES = [
  'toolbar',
  'listbox',
  'tablist',
  'menu',
  'menubar',
  'scrollbar',
  'separator',
  'slider',
];

/**
 * A `<div>` with roving tabindex: Tab enters the group on one item, the arrow
 * keys move between items, Home and End jump to the ends. Headless: give it
 * the role the pattern needs (`toolbar`, `radiogroup`, `listbox`, `grid`) and
 * an accessible name. Text fields inside keep their own arrow keys.
 */
export const RovingFocusGroup = forwardRef<HTMLDivElement, RovingFocusGroupProps>(
  function RovingFocusGroup(
    {
      orientation = 'horizontal',
      loop,
      columns,
      dir,
      itemSelector,
      onActiveIndexChange,
      onKeyDown,
      className,
      ...rest
    },
    forwarded: ForwardedRef<HTMLDivElement>,
  ) {
    const ref = useRef<HTMLDivElement | null>(null);
    const setRef = useComposedRef(ref, forwarded);
    const roving = useRovingFocus(ref, {
      orientation,
      loop,
      columns,
      dir,
      itemSelector,
      onActiveIndexChange,
    });
    const ariaOrientation =
      orientation === 'horizontal' || orientation === 'vertical' ? orientation : undefined;
    const role = rest.role;
    return (
      // Headless composite container: the caller gives the role; keys move focus between its items.
      // eslint-disable-next-line jsx-a11y/no-static-element-interactions
      <div
        ref={setRef}
        aria-orientation={
          role !== undefined && ORIENTED_ROLES.includes(role) ? ariaOrientation : undefined
        }
        {...rest}
        dir={dir}
        className={cx(ROOT, className)}
        data-orientation={orientation}
        onKeyDown={(e) => {
          onKeyDown?.(e);
          roving.onKeyDown(e);
        }}
      />
    );
  },
);
