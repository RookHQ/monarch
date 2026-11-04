'use client';

import {
  useEffect,
  useLayoutEffect,
  useRef,
  useState,
  type ButtonHTMLAttributes,
  type HTMLAttributes,
  type ReactNode,
} from 'react';
import { cx } from '../../../internal/cx.js';
import { CloseGlyph, FilterGlyph, PlusGlyph } from '../../../internal/icons.js';
import { Button } from '../../../components/actions/button/index.js';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from '../../../components/overlays/dropdown-menu/index.js';
import {
  Popover,
  PopoverContent,
  PopoverTrigger,
} from '../../../components/overlays/popover/index.js';
import {
  Sheet,
  SheetBody,
  SheetContent,
  SheetHeader,
  SheetTitle,
  SheetTrigger,
} from '../../../components/overlays/sheet/index.js';

const useLayoutEffectSafe = typeof window === 'undefined' ? useEffect : useLayoutEffect;

export interface FilterChipProps extends Omit<HTMLAttributes<HTMLSpanElement>, 'children'> {
  /** Filter field, e.g. "Status". In toggle mode, the chip's label, e.g. "Unread". */
  name: string;
  /** Current value, e.g. "Open". Optional for toggle chips. */
  value?: ReactNode;
  /** Removes the filter. The remove button is labelled "Remove <name> filter". */
  onRemove?: () => void;
  /** Accessible name of the remove button. Default "Remove <name> filter". */
  removeLabel?: string;
  /**
   * Toggle mode: the whole chip is a pressed/unpressed button (`aria-pressed`),
   * for quick filters such as "Assigned to me". Controlled.
   */
  pressed?: boolean;
  /** Initial pressed state (uncontrolled toggle mode). */
  defaultPressed?: boolean;
  onPressedChange?: (pressed: boolean) => void;
  /** Count shown after the label, e.g. matching items. */
  count?: number;
  /**
   * Editor shown in a popover when the chip is pressed (pickers for the value).
   * The name and value become the popover trigger.
   */
  editor?: ReactNode;
  /** Popover open state (controlled). */
  open?: boolean;
  onOpenChange?: (open: boolean) => void;
  /** Accessible name of the popover. Default "<name> filter". */
  contentLabel?: string;
}

/** One active filter: removable, editable in a popover, or a pressed toggle. */
export function FilterChip({
  name,
  value,
  onRemove,
  removeLabel,
  pressed: pressedProp,
  defaultPressed,
  onPressedChange,
  count,
  editor,
  open,
  onOpenChange,
  contentLabel,
  className,
  ...rest
}: FilterChipProps) {
  const toggle =
    pressedProp !== undefined || defaultPressed !== undefined || onPressedChange !== undefined;
  const [innerPressed, setInnerPressed] = useState(defaultPressed ?? false);
  const pressed = pressedProp ?? innerPressed;
  const countNode =
    count !== undefined ? <span className="mn-filter-chip__count">{count}</span> : null;

  if (toggle) {
    return (
      <button
        type="button"
        {...(rest as ButtonHTMLAttributes<HTMLButtonElement>)}
        aria-pressed={pressed}
        data-variant="toggle"
        data-pressed={pressed || undefined}
        className={cx('mn-filter-chip', className)}
        onClick={(e) => {
          rest.onClick?.(e as never);
          if (pressedProp === undefined) setInnerPressed(!pressed);
          onPressedChange?.(!pressed);
        }}
      >
        <span className="mn-filter-chip__label">{name}</span>
        {value !== undefined ? <span className="mn-filter-chip__value">{value}</span> : null}
        {countNode}
      </button>
    );
  }

  const label = (
    <>
      <span className="mn-filter-chip__name" data-has-value={value !== undefined || undefined}>
        {name}
      </span>
      {value !== undefined ? <span className="mn-filter-chip__value">{value}</span> : null}
      {countNode}
    </>
  );
  return (
    <span
      {...rest}
      className={cx('mn-filter-chip', className)}
      data-editable={editor ? '' : undefined}
    >
      {editor ? (
        <Popover
          {...(open !== undefined ? { open } : {})}
          {...(onOpenChange ? { onOpenChange } : {})}
        >
          <PopoverTrigger className="mn-filter-chip__edit">{label}</PopoverTrigger>
          <PopoverContent
            aria-label={contentLabel ?? `${name} filter`}
            className="mn-filter-chip__popover"
          >
            {editor}
          </PopoverContent>
        </Popover>
      ) : (
        label
      )}
      {onRemove ? (
        <button
          type="button"
          className="mn-filter-chip__remove"
          aria-label={removeLabel ?? `Remove ${name} filter`}
          onClick={onRemove}
        >
          <CloseGlyph />
        </button>
      ) : null}
    </span>
  );
}

export interface FilterBarMessages {
  /** Visible active-filter count, e.g. "2 filters". */
  active: (count: number) => string;
  /** Button that opens the filter sheet on narrow screens. */
  filtersButton: (count: number) => string;
  sheetTitle: string;
  done: string;
}

export const defaultFilterBarMessages: FilterBarMessages = {
  active: (n) => (n === 1 ? '1 filter' : `${n} filters`),
  filtersButton: (n) => (n > 0 ? `Filters (${n})` : 'Filters'),
  sheetTitle: 'Filters',
  done: 'Done',
};

export interface FilterBarProps extends HTMLAttributes<HTMLDivElement> {
  /** Accessible name of the group. */
  label?: string;
  /** Called by the "Clear all" button; the button is hidden when omitted. */
  onClearAll?: () => void;
  /** Hide "Clear all" when there is nothing to clear. */
  clearAllLabel?: string;
  /** Slot for an "Add filter" control (FilterMenu, or your own). */
  add?: ReactNode;
  /** Search field on the leading edge, e.g. a SearchInput with `debounce`. */
  search?: ReactNode;
  /** Controls on the trailing edge: sort, display options, saved views. */
  end?: ReactNode;
  /** Result summary, e.g. "24 results". Announced politely when it changes. */
  resultCount?: ReactNode;
  /** Number of applied filters. Shows "2 filters" and is used by the narrow-screen button. */
  activeCount?: number;
  /** Expose the bar as a search landmark (`role="search"`) instead of a group. */
  landmark?: boolean;
  /** Keep chips on one line that scrolls sideways; the pressed chip scrolls into view. */
  scrollable?: boolean;
  /**
   * Below this width (px) of the bar, chips, the add control and "Clear all"
   * move into a bottom sheet opened by a "Filters (n)" button.
   */
  collapseBelow?: number;
  messages?: Partial<FilterBarMessages>;
  children?: ReactNode;
}

/** Filter row: search, FilterChips, an add control, "Clear all", a result count and trailing controls. */
export function FilterBar({
  label = 'Active filters',
  onClearAll,
  clearAllLabel = 'Clear all',
  add,
  search,
  end,
  resultCount,
  activeCount,
  landmark = false,
  scrollable = false,
  collapseBelow,
  messages,
  className,
  children,
  ...rest
}: FilterBarProps) {
  const m = { ...defaultFilterBarMessages, ...messages };
  const ref = useRef<HTMLDivElement | null>(null);
  const chipsRef = useRef<HTMLDivElement | null>(null);
  const [width, setWidth] = useState<number>();
  const [sheetOpen, setSheetOpen] = useState(false);

  useLayoutEffectSafe(() => {
    const el = ref.current;
    if (collapseBelow === undefined || !el || typeof ResizeObserver === 'undefined') return;
    const ro = new ResizeObserver((entries) => {
      const w = entries[0]?.contentRect.width;
      if (w !== undefined) setWidth(Math.round(w));
    });
    ro.observe(el);
    setWidth(Math.round(el.getBoundingClientRect().width) || undefined);
    return () => ro.disconnect();
  }, [collapseBelow]);

  useEffect(() => {
    if (!scrollable) return;
    const pressed = chipsRef.current?.querySelector<HTMLElement>('[aria-pressed="true"]');
    pressed?.scrollIntoView?.({ block: 'nearest', inline: 'nearest' });
  });

  const collapsed = collapseBelow !== undefined && width !== undefined && width < collapseBelow;
  const clear = onClearAll ? (
    <button type="button" className="mn-filter-bar__clear" onClick={onClearAll}>
      {clearAllLabel}
    </button>
  ) : null;
  const active =
    activeCount !== undefined && activeCount > 0 && !collapsed ? (
      <span className="mn-filter-bar__active">{m.active(activeCount)}</span>
    ) : null;
  const trailing =
    resultCount !== undefined || end ? (
      <div className="mn-filter-bar__end">
        {resultCount !== undefined ? (
          <span className="mn-filter-bar__result" role="status">
            {resultCount}
          </span>
        ) : null}
        {end}
      </div>
    ) : null;

  return (
    <div
      ref={ref}
      {...rest}
      role={landmark ? 'search' : 'group'}
      aria-label={label}
      className={cx('mn-filter-bar', className)}
      data-scrollable={scrollable || undefined}
      data-collapsed={collapsed || undefined}
    >
      {search ? <div className="mn-filter-bar__search">{search}</div> : null}
      {collapsed ? (
        <Sheet open={sheetOpen} onOpenChange={setSheetOpen}>
          <SheetTrigger asChild>
            <Button size="sm" variant="secondary" leadingIcon={<FilterGlyph />}>
              {m.filtersButton(activeCount ?? 0)}
            </Button>
          </SheetTrigger>
          <SheetContent
            side="bottom"
            aria-describedby={undefined}
            footer={
              <>
                {clear}
                <Button variant="primary" onClick={() => setSheetOpen(false)}>
                  {m.done}
                </Button>
              </>
            }
          >
            <SheetHeader>
              <SheetTitle>{m.sheetTitle}</SheetTitle>
            </SheetHeader>
            <SheetBody>
              <div className="mn-filter-bar__sheet">
                {children}
                {add}
              </div>
            </SheetBody>
          </SheetContent>
        </Sheet>
      ) : scrollable ? (
        <>
          {active}
          <div ref={chipsRef} className="mn-filter-bar__chips">
            {children}
          </div>
          {add}
          {clear}
        </>
      ) : (
        <>
          {active}
          {children}
          {add}
          {clear}
        </>
      )}
      {trailing}
    </div>
  );
}

export interface FilterMenuOption {
  value: string;
  label: string;
  icon?: ReactNode;
  disabled?: boolean;
}

export interface FilterMenuProps {
  /** Filter fields the user can add. */
  options: readonly FilterMenuOption[];
  /** A field was chosen; open its editor (for example a FilterChip with `editor` and `open`). */
  onSelect: (value: string) => void;
  /** Trigger text. Default "Add filter". */
  label?: string;
  size?: 'xs' | 'sm' | 'md';
  disabled?: boolean;
  className?: string;
}

/** "Add filter" menu listing the filter fields; put it in FilterBar's `add` slot. */
export function FilterMenu({
  options,
  onSelect,
  label = 'Add filter',
  size = 'xs',
  disabled,
  className,
}: FilterMenuProps) {
  return (
    <DropdownMenu>
      <DropdownMenuTrigger asChild disabled={disabled}>
        <Button
          size={size}
          variant="ghost"
          leadingIcon={<PlusGlyph />}
          className={cx('mn-filter-bar__add', className)}
        >
          {label}
        </Button>
      </DropdownMenuTrigger>
      <DropdownMenuContent>
        {options.map((o) => (
          <DropdownMenuItem
            key={o.value}
            icon={o.icon}
            disabled={o.disabled}
            onSelect={() => onSelect(o.value)}
          >
            {o.label}
          </DropdownMenuItem>
        ))}
      </DropdownMenuContent>
    </DropdownMenu>
  );
}

/** Filter state as plain values: one string or several per key; empty means "not set". */
export type FilterParams = Record<string, string | readonly string[] | null | undefined>;

/**
 * Reads filters from URL search params. Repeated keys (`?status=open&status=new`)
 * become arrays; keys not listed are ignored. Router-agnostic: pass
 * `location.search` or your router's params.
 */
export function readFilterParams(
  params: URLSearchParams | string,
  keys: readonly string[],
): Record<string, string[]> {
  const p = typeof params === 'string' ? new URLSearchParams(params) : params;
  const out: Record<string, string[]> = {};
  for (const k of keys) {
    const all = p.getAll(k).filter((v) => v !== '');
    if (all.length) out[k] = all;
  }
  return out;
}

/**
 * Writes filters into a copy of `params`, replacing each key in `filters` and
 * removing empty ones. Other keys (page, tab) are kept. Returns the new params.
 */
export function writeFilterParams(
  params: URLSearchParams | string,
  filters: FilterParams,
): URLSearchParams {
  const p = new URLSearchParams(typeof params === 'string' ? params : params.toString());
  for (const [k, v] of Object.entries(filters)) {
    p.delete(k);
    const list = v === null || v === undefined ? [] : typeof v === 'string' ? [v] : v;
    for (const item of list) if (item !== '') p.append(k, item);
  }
  return p;
}
