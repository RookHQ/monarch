'use client';

import {
  createContext,
  forwardRef,
  useCallback,
  useContext,
  useEffect,
  useRef,
  useState,
  type ButtonHTMLAttributes,
  type HTMLAttributes,
  type KeyboardEvent,
  type ReactElement,
  type ReactNode,
  type Ref,
} from 'react';
import { useIsomorphicLayoutEffect } from '../../../internal/layout-effect.js';
import { cx } from '../../../internal/cx.js';
import { ToggleButton, type ToggleButtonVariant } from '../toggle-button/index.js';
import { useDirection } from '../../../internal/direction.js';

export type ToggleGroupSize = 'xs' | 'sm' | 'md' | 'lg';
export type ToggleGroupOrientation = 'horizontal' | 'vertical';

/** One option of the `options` shorthand. */
export interface ToggleGroupOption<T extends string = string> {
  value: T;
  /** Visible text, and the accessible name. Required even when `hideLabel` is set. */
  label: string;
  /** Decorative icon before the label. */
  icon?: ReactNode;
  /** Show only the icon; `label` becomes the accessible name and the tooltip. */
  hideLabel?: boolean;
  /** A count after the label ("Open 12"). Included in the accessible name. */
  count?: number | string;
  disabled?: boolean;
}

/** The group must have an accessible name: a visible heading's id, or a label. */
type AccessibleName =
  | { 'aria-label': string; 'aria-labelledby'?: string }
  | { 'aria-labelledby': string; 'aria-label'?: string };

interface SharedProps<T extends string> extends Omit<
  HTMLAttributes<HTMLDivElement>,
  'defaultValue' | 'onChange' | 'dir' | 'role'
> {
  /**
   * Shorthand: render one item per option instead of `ToggleGroupItem`
   * children. Same shape as the options of SegmentedControl.
   */
  options?: readonly ToggleGroupOption<T>[];
  /** `ToggleGroupItem` elements (ignored when `options` is given). */
  children?: ReactNode;
  /** Style of every item. Default `default`. */
  variant?: ToggleButtonVariant;
  /** Size of every item. Default `md`. */
  size?: ToggleGroupSize;
  /** Arrow keys follow it: Left/Right when horizontal, Up/Down when vertical. Default `horizontal`. */
  orientation?: ToggleGroupOrientation;
  /** Arrow keys wrap from the last item to the first. Default `true`. */
  loop?: boolean;
  /** Disable every item. */
  disabled?: boolean;
  /** Text direction. Left and Right arrows follow it. Default: inherited from the page. */
  dir?: 'ltr' | 'rtl';
}

/** One pressed item at a time. The value is `''` when nothing is pressed. */
export interface ToggleGroupSingleProps<T extends string = string> extends SharedProps<T> {
  type: 'single';
  /** Controlled value: the pressed item, or `''`. */
  value?: NoInfer<T> | '';
  /** Initial value when uncontrolled. */
  defaultValue?: NoInfer<T> | '';
  /** Called with the pressed item, or `''` when it was released. */
  onValueChange?: ((value: NoInfer<T> | '') => void) | undefined;
  /** Keep one item pressed: pressing the pressed item does not release it. Default `false`. */
  required?: boolean;
}

/** Any number of pressed items. */
export interface ToggleGroupMultipleProps<T extends string = string> extends SharedProps<T> {
  /** Default `multiple`. */
  type?: 'multiple';
  /** Controlled value: the pressed items. */
  value?: readonly NoInfer<T>[];
  /** Initial value when uncontrolled. */
  defaultValue?: readonly NoInfer<T>[];
  /** Called with the pressed items, in option order when `options` is used. */
  onValueChange?: ((value: NoInfer<T>[]) => void) | undefined;
  required?: never;
}

export type ToggleGroupProps<T extends string = string> = (
  ToggleGroupSingleProps<T> | ToggleGroupMultipleProps<T>
) &
  AccessibleName;

interface GroupContextValue {
  pressed: (value: string) => boolean;
  toggle: (value: string) => void;
  focusable: (value: string) => boolean;
  onItemFocus: (value: string) => void;
  onItemKeyDown: (event: KeyboardEvent<HTMLButtonElement>) => void;
  variant: ToggleButtonVariant;
  size: ToggleGroupSize;
  disabled: boolean;
}

const GroupContext = createContext<GroupContextValue | null>(null);

const useIsoLayoutEffect = typeof window === 'undefined' ? useEffect : useIsomorphicLayoutEffect;

const ITEM = '[data-mn-toggle-group-item]';

function enabledItems(root: HTMLElement | null): HTMLButtonElement[] {
  if (!root) return [];
  return Array.from(root.querySelectorAll<HTMLButtonElement>(ITEM)).filter(
    (el) => !el.disabled && el.closest('[data-mn-toggle-group]') === root,
  );
}

function ToggleGroupInner<T extends string>(props: ToggleGroupProps<T>, ref: Ref<HTMLDivElement>) {
  const {
    type = 'multiple',
    options,
    children,
    variant = 'default',
    size = 'md',
    orientation = 'horizontal',
    loop = true,
    disabled = false,
    dir,
    value: valueProp,
    defaultValue,
    onValueChange,
    required = false,
    className,
    ...rest
  } = props as ToggleGroupSingleProps<T> &
    Omit<ToggleGroupMultipleProps<T>, 'type' | 'value' | 'defaultValue' | 'onValueChange'> & {
      type?: 'single' | 'multiple';
      value?: T | '' | readonly T[];
      defaultValue?: T | '' | readonly T[];
      onValueChange?: (value: never) => void;
    };
  const readDir = useDirection(dir);
  const single = type === 'single';
  const toList = (v: T | '' | readonly T[] | undefined): readonly string[] =>
    v === undefined || v === '' ? [] : typeof v === 'string' ? [v] : v;

  const [inner, setInner] = useState<readonly string[]>(() => toList(defaultValue));
  const controlled = valueProp !== undefined;
  const current = controlled ? toList(valueProp) : inner;
  const rootRef = useRef<HTMLDivElement | null>(null);
  // The roving tab stop: the last focused item, else the first pressed one,
  // else the first enabled one (resolved from the DOM after render).
  const [active, setActive] = useState<string | null>(null);
  const [stop, setStop] = useState<string | null>(null);

  const commit = useCallback(
    (next: readonly string[]) => {
      if (!controlled) setInner(next);
      const emit = onValueChange as ((v: unknown) => void) | undefined;
      emit?.(single ? (next[0] ?? '') : [...next]);
    },
    [controlled, onValueChange, single],
  );

  const toggle = useCallback(
    (value: string) => {
      const on = current.includes(value);
      if (single) {
        if (on && required) return;
        commit(on ? [] : [value]);
        return;
      }
      if (on) {
        commit(current.filter((v) => v !== value));
        return;
      }
      const order = options?.map((o) => o.value as string);
      const next = [...current, value];
      commit(order ? order.filter((v) => next.includes(v)) : next);
    },
    [current, single, required, commit, options],
  );

  useIsoLayoutEffect(() => {
    const items = enabledItems(rootRef.current);
    const values = items.map((el) => el.dataset.value ?? '');
    const next =
      (active !== null && values.includes(active) ? active : null) ??
      values.find((v) => current.includes(v)) ??
      values[0] ??
      null;
    if (next !== stop) setStop(next);
  });

  const onItemKeyDown = (event: KeyboardEvent<HTMLButtonElement>) => {
    const rtl = readDir(event.currentTarget) === 'rtl';
    const horizontal = orientation === 'horizontal';
    const forward = horizontal ? (rtl ? 'ArrowLeft' : 'ArrowRight') : 'ArrowDown';
    const back = horizontal ? (rtl ? 'ArrowRight' : 'ArrowLeft') : 'ArrowUp';
    const items = enabledItems(rootRef.current);
    const from = items.indexOf(event.currentTarget);
    let to: number;
    if (event.key === forward) to = from + 1;
    else if (event.key === back) to = from - 1;
    else if (event.key === 'Home') to = 0;
    else if (event.key === 'End') to = items.length - 1;
    else return;
    event.preventDefault();
    if (to < 0) to = loop ? items.length - 1 : 0;
    if (to >= items.length) to = loop ? 0 : items.length - 1;
    items[to]?.focus();
  };

  const context: GroupContextValue = {
    pressed: (value) => current.includes(value),
    toggle,
    // Before the first layout pass (server render) every item is a tab stop,
    // so the group stays reachable without JavaScript.
    focusable: (value) => stop === null || stop === value,
    onItemFocus: setActive,
    onItemKeyDown,
    variant,
    size,
    disabled,
  };

  return (
    <div
      ref={(node) => {
        rootRef.current = node;
        if (typeof ref === 'function') ref(node);
        else if (ref) (ref as { current: HTMLDivElement | null }).current = node;
      }}
      role="group"
      {...rest}
      dir={dir}
      aria-disabled={disabled || undefined}
      data-mn-toggle-group=""
      data-type={type}
      data-orientation={orientation}
      data-size={size}
      data-disabled={disabled || undefined}
      className={cx('mn-toggle-group', className)}
    >
      <GroupContext.Provider value={context}>
        {options
          ? options.map((option) => (
              <ToggleGroupItem
                key={option.value}
                value={option.value}
                disabled={option.disabled}
                aria-label={option.hideLabel ? option.label : undefined}
                title={option.hideLabel ? option.label : undefined}
                data-icon-only={option.hideLabel || undefined}
              >
                {option.icon ? (
                  <span className="mn-toggle-group__icon" aria-hidden>
                    {option.icon}
                  </span>
                ) : null}
                {option.hideLabel ? null : <span>{option.label}</span>}
                {option.count !== undefined && !option.hideLabel ? (
                  <span className="mn-toggle-group__count">{option.count}</span>
                ) : null}
              </ToggleGroupItem>
            ))
          : children}
      </GroupContext.Provider>
    </div>
  );
}

/**
 * A set of toggle buttons with one Tab stop, for formatting toolbars, list
 * filters and view options. `type="multiple"` (the default) lets any number
 * be pressed; `type="single"` keeps at most one pressed. Arrow keys move
 * between items, Home and End jump to the ends, Enter or Space toggles.
 * Each item is a button with `aria-pressed` inside a labelled group.
 */
export const ToggleGroup = forwardRef(ToggleGroupInner) as <T extends string = string>(
  props: ToggleGroupProps<T> & { ref?: Ref<HTMLDivElement> },
) => ReactElement;

export interface ToggleGroupItemProps extends Omit<
  ButtonHTMLAttributes<HTMLButtonElement>,
  'value' | 'type' | 'aria-pressed'
> {
  /** The value this item adds to the group's value when pressed. */
  value: string;
  /** Decorative icon before the label. */
  leadingIcon?: ReactNode;
}

/** One toggle inside a ToggleGroup. Give icon-only items an `aria-label`. */
export const ToggleGroupItem = forwardRef<HTMLButtonElement, ToggleGroupItemProps>(
  function ToggleGroupItem(
    { value, disabled, leadingIcon, className, onFocus, onKeyDown, onClick, ...rest },
    ref,
  ) {
    const group = useContext(GroupContext);
    if (!group) throw new Error('ToggleGroupItem must be rendered inside a ToggleGroup.');
    const off = group.disabled || disabled;
    return (
      <ToggleButton
        ref={ref}
        {...rest}
        leadingIcon={leadingIcon}
        variant={group.variant}
        size={group.size}
        disabled={off || undefined}
        pressed={group.pressed(value)}
        tabIndex={group.focusable(value) ? 0 : -1}
        data-value={value}
        data-mn-toggle-group-item=""
        className={cx('mn-toggle-group__item', className)}
        onFocus={(event) => {
          onFocus?.(event);
          group.onItemFocus(value);
        }}
        onKeyDown={(event) => {
          onKeyDown?.(event);
          if (!event.defaultPrevented) group.onItemKeyDown(event);
        }}
        onClick={(event) => {
          onClick?.(event);
          // ToggleButton skips its own toggle when the default is prevented.
          if (!event.defaultPrevented) group.toggle(value);
        }}
        onPressedChange={() => {}}
      />
    );
  },
);
