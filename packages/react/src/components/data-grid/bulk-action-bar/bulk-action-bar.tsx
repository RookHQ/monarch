'use client';

import {
  forwardRef,
  useCallback,
  useEffect,
  useLayoutEffect,
  useRef,
  useState,
  type HTMLAttributes,
  type KeyboardEvent as ReactKeyboardEvent,
  type ReactNode,
  type RefObject,
} from 'react';
import { cx } from '../../../internal/cx.js';
import { ChevronDownGlyph, CloseGlyph } from '../../../internal/icons.js';
import { Button } from '../../actions/button/index.js';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from '../../overlays/dropdown-menu/index.js';

export interface BulkAction {
  id: string;
  label: string;
  /** Decorative icon before the label. */
  icon?: ReactNode;
  /**
   * Runs the action on the selection. Return a promise to show the button as
   * busy (and block the other actions) until it settles.
   */
  onSelect: () => unknown;
  /** `danger` for destructive actions. Confirm them in the handler (ConfirmDialog). */
  tone?: 'default' | 'danger';
  disabled?: boolean;
}

export interface BulkActionBarMessages {
  /** Visible count and announcement, e.g. "3 selected" or "3 of 120 selected". */
  selected: (count: number, total?: number) => string;
  /** Announced when the selection is emptied. */
  cleared: string;
  /** Clear button. */
  clear: string;
  /** Overflow menu button. */
  more: string;
  /** Accessible name of the toolbar. */
  label: string;
}

export const defaultBulkActionBarMessages: BulkActionBarMessages = {
  selected: (count, total) =>
    total !== undefined ? `${count} of ${total} selected` : `${count} selected`,
  cleared: 'Selection cleared',
  clear: 'Clear selection',
  more: 'More',
  label: 'Bulk actions',
};

export interface BulkActionBarProps extends Omit<HTMLAttributes<HTMLDivElement>, 'children'> {
  /** Number of selected items. The bar is shown while it is above 0. */
  count: number;
  /** Total items that could be selected, for "3 of 120 selected". */
  totalCount?: number;
  /** Empties the selection (DataTable: set `selectedRowIds` to `[]`). */
  onClear: () => void;
  /** Actions shown as buttons; those beyond `maxVisibleActions` go into a "More" menu. */
  actions?: readonly BulkAction[];
  /** Buttons shown before the overflow menu. Default 3. */
  maxVisibleActions?: number;
  /** Extra controls in the toolbar, e.g. a property picker. */
  children?: ReactNode;
  /** `sticky` keeps the bar at the bottom of the scroll area. Default `inline`. */
  placement?: 'inline' | 'sticky';
  /** An action is running: every action is blocked. */
  busy?: boolean;
  /** Where focus goes after "Clear selection". Default: the bar's container. */
  returnFocusRef?: RefObject<HTMLElement | null>;
  messages?: Partial<BulkActionBarMessages>;
}

const useLayoutEffectSafe = typeof window === 'undefined' ? useEffect : useLayoutEffect;
const TEXT_ENTRY =
  'input:not([type="checkbox"]):not([type="radio"]),textarea,select,[contenteditable="true"]';

/**
 * Toolbar that appears while items are selected: the count, actions with an
 * overflow menu, and "Clear selection". The count is announced politely.
 * Arrow keys move between its controls (one tab stop).
 */
export const BulkActionBar = forwardRef<HTMLDivElement, BulkActionBarProps>(function BulkActionBar(
  {
    count,
    totalCount,
    onClear,
    actions = [],
    maxVisibleActions = 3,
    children,
    placement = 'inline',
    busy = false,
    returnFocusRef,
    messages,
    className,
    ...rest
  },
  ref,
) {
  const m = { ...defaultBulkActionBarMessages, ...messages };
  const rootRef = useRef<HTMLDivElement | null>(null);
  const toolbarRef = useRef<HTMLDivElement | null>(null);
  const [pending, setPending] = useState<string | null>(null);
  const [activeIndex, setActiveIndex] = useState(0);
  const [announcement, setAnnouncement] = useState('');
  const previous = useRef(count);
  const setRoot = useCallback(
    (el: HTMLDivElement | null) => {
      rootRef.current = el;
      if (typeof ref === 'function') ref(el);
      else if (ref) ref.current = el;
    },
    [ref],
  );

  // Announce changes to the count (not the first render).
  useEffect(() => {
    if (previous.current === count) return;
    previous.current = count;
    setAnnouncement(count > 0 ? m.selected(count, totalCount) : m.cleared);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [count, totalCount]);

  const blocked = busy || pending !== null;
  const visible = actions.slice(0, maxVisibleActions);
  const overflow = actions.slice(maxVisibleActions);

  const run = (a: BulkAction) => {
    if (blocked || a.disabled) return;
    const result = a.onSelect();
    if (result && typeof (result as Promise<unknown>).then === 'function') {
      setPending(a.id);
      (result as Promise<unknown>).then(
        () => setPending(null),
        () => setPending(null),
      );
    }
  };

  // Roving tabindex across the toolbar's controls.
  const items = () =>
    toolbarRef.current
      ? Array.from(
          toolbarRef.current.querySelectorAll<HTMLElement>(
            'button:not([disabled]),[href],input:not([disabled]),select:not([disabled]),[role="combobox"]',
          ),
        ).filter((el) => !el.closest('[aria-hidden="true"]'))
      : [];
  useLayoutEffectSafe(() => {
    const list = items();
    const i = Math.min(activeIndex, list.length - 1);
    list.forEach((el, n) => {
      el.tabIndex = n === i ? 0 : -1;
    });
  });
  const onKeyDown = (e: ReactKeyboardEvent<HTMLDivElement>) => {
    const t = e.target as HTMLElement;
    if (t.matches(TEXT_ENTRY)) return;
    const list = items();
    const i = list.indexOf(t);
    if (i === -1) return;
    const rtl = getComputedStyle(e.currentTarget).direction === 'rtl';
    const fwd = rtl ? 'ArrowLeft' : 'ArrowRight';
    const back = rtl ? 'ArrowRight' : 'ArrowLeft';
    let next = -1;
    if (e.key === fwd) next = (i + 1) % list.length;
    else if (e.key === back) next = (i - 1 + list.length) % list.length;
    else if (e.key === 'Home') next = 0;
    else if (e.key === 'End') next = list.length - 1;
    if (next === -1) return;
    e.preventDefault();
    setActiveIndex(next);
    list[next]?.focus();
  };

  const clear = () => {
    onClear();
    setActiveIndex(0);
    const target = returnFocusRef?.current ?? rootRef.current;
    target?.focus();
  };

  const open = count > 0;
  return (
    <div
      ref={setRoot}
      tabIndex={-1}
      {...rest}
      className={cx('mn-bulk-action-bar', className)}
      data-state={open ? 'open' : 'closed'}
      data-placement={placement}
    >
      <span className="mn-visually-hidden" role="status">
        {announcement}
      </span>
      {open ? (
        <div
          ref={toolbarRef}
          role="toolbar"
          aria-label={m.label}
          aria-busy={blocked || undefined}
          className="mn-bulk-action-bar__bar"
          onKeyDown={onKeyDown}
          onFocus={(e) => {
            const i = items().indexOf(e.target as HTMLElement);
            if (i !== -1 && i !== activeIndex) setActiveIndex(i);
          }}
        >
          <span className="mn-bulk-action-bar__count">{m.selected(count, totalCount)}</span>
          <div className="mn-bulk-action-bar__actions">
            {visible.map((a, i) => (
              <Button
                key={a.id}
                size="sm"
                variant={a.tone === 'danger' ? 'danger-subtle' : 'secondary'}
                leadingIcon={a.icon}
                loading={pending === a.id}
                disabled={a.disabled || (blocked && pending !== a.id)}
                data-rank={i}
                className="mn-bulk-action-bar__action"
                onClick={() => run(a)}
              >
                {a.label}
              </Button>
            ))}
            {overflow.length ? (
              <DropdownMenu>
                <DropdownMenuTrigger asChild>
                  <Button
                    size="sm"
                    variant="secondary"
                    disabled={blocked}
                    trailingIcon={<ChevronDownGlyph />}
                  >
                    {m.more}
                  </Button>
                </DropdownMenuTrigger>
                <DropdownMenuContent align="end">
                  {overflow.map((a) => (
                    <DropdownMenuItem
                      key={a.id}
                      icon={a.icon}
                      {...(a.tone ? { tone: a.tone } : {})}
                      disabled={a.disabled || blocked}
                      onSelect={() => run(a)}
                    >
                      {a.label}
                    </DropdownMenuItem>
                  ))}
                </DropdownMenuContent>
              </DropdownMenu>
            ) : null}
            {children}
          </div>
          <Button
            size="sm"
            variant="ghost"
            className="mn-bulk-action-bar__clear"
            leadingIcon={<CloseGlyph />}
            onClick={clear}
          >
            {m.clear}
          </Button>
        </div>
      ) : null}
    </div>
  );
});
