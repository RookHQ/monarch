'use client';

import {
  forwardRef,
  useCallback,
  useState,
  type HTMLAttributes,
  type KeyboardEvent,
  type ReactElement,
  type ReactNode,
  type Ref,
} from 'react';
import { cx } from '../../../internal/cx.js';
import { useDirection } from '../../../internal/direction.js';

export type SegmentedControlSize = 'sm' | 'md' | 'lg';

export interface SegmentedOption<T extends string = string> {
  value: T;
  /** Visible text, and the accessible name. Required even when `hideLabel` is set. */
  label: string;
  /** Decorative icon before the label. */
  icon?: ReactNode;
  /** Show only the icon; `label` becomes the accessible name and the tooltip. */
  hideLabel?: boolean;
  /** A count after the label ("Open 12"). Included in the accessible name. */
  count?: number | string;
  /** Short extra text after the label, for example "Save 20%". */
  suffix?: ReactNode;
  disabled?: boolean;
}

type ControlledValue<V> =
  | { value: V; defaultValue?: never }
  | { value?: never; defaultValue?: V }
  | { value?: undefined; defaultValue?: V };

interface SharedProps<T extends string> extends Omit<
  HTMLAttributes<HTMLDivElement>,
  'defaultValue' | 'onChange' | 'dir'
> {
  options: readonly SegmentedOption<T>[];
  size?: SegmentedControlSize;
  /** Stretch to the container and share the width equally. */
  block?: boolean;
  /** Disable every option. */
  disabled?: boolean;
  /** Text direction. Left and Right arrows follow it. Default: inherited from the page. */
  dir?: 'ltr' | 'rtl';
}

/** The group must have an accessible name: a visible heading's id, or a label. */
type AccessibleName =
  | { 'aria-label': string; 'aria-labelledby'?: string }
  | { 'aria-labelledby': string; 'aria-label'?: string };

export type SegmentedControlProps<T extends string = string> = SharedProps<T> &
  AccessibleName &
  ControlledValue<T> & {
    onValueChange?: (value: T) => void;
    /** Submits the selected value with a form under this name. */
    name?: string;
    /** Requires a selection (sets aria-required). */
    required?: boolean;
  };

/** The keys that move focus, mapped to a step. `null` = not a navigation key. */
function stepFor(key: string, dir: 'ltr' | 'rtl'): number | 'first' | 'last' | null {
  const forward = dir === 'rtl' ? 'ArrowLeft' : 'ArrowRight';
  const back = dir === 'rtl' ? 'ArrowRight' : 'ArrowLeft';
  if (key === forward || key === 'ArrowDown') return 1;
  if (key === back || key === 'ArrowUp') return -1;
  if (key === 'Home') return 'first';
  if (key === 'End') return 'last';
  return null;
}

/** Index of the next enabled option, wrapping around; -1 when none is enabled. */
function nextEnabled(
  options: readonly SegmentedOption[],
  from: number,
  step: number | 'first' | 'last',
  allDisabled: boolean,
): number {
  if (allDisabled) return -1;
  const n = options.length;
  if (step === 'first') return options.findIndex((o) => !o.disabled);
  if (step === 'last') {
    for (let i = n - 1; i >= 0; i--) if (!options[i]?.disabled) return i;
    return -1;
  }
  for (let k = 1; k <= n; k++) {
    const i = (((from + step * k) % n) + n) % n;
    if (!options[i]?.disabled) return i;
  }
  return -1;
}

function focusOption(root: HTMLElement | null, index: number) {
  root?.querySelectorAll<HTMLButtonElement>('[data-index]')[index]?.focus();
}

function OptionContent({ option }: { option: SegmentedOption }) {
  return (
    <>
      {option.icon ? (
        <span className="mn-segmented-control__icon" aria-hidden>
          {option.icon}
        </span>
      ) : null}
      <span className={option.hideLabel ? 'mn-visually-hidden' : 'mn-segmented-control__label'}>
        {option.label}
      </span>
      {/* Spaces keep the accessible name readable: "Open 12", not "Open12". */}
      {option.count !== undefined ? (
        <>
          {' '}
          <span className="mn-segmented-control__count">{option.count}</span>
        </>
      ) : null}
      {option.suffix ? (
        <>
          {' '}
          <span className="mn-segmented-control__suffix">{option.suffix}</span>
        </>
      ) : null}
    </>
  );
}

function useControllable<V>(value: V | undefined, defaultValue: V, onChange?: (v: V) => void) {
  const [inner, setInner] = useState(defaultValue);
  const controlled = value !== undefined;
  const current = controlled ? value : inner;
  const set = useCallback(
    (next: V) => {
      if (!controlled) setInner(next);
      onChange?.(next);
    },
    [controlled, onChange],
  );
  return [current, set] as const;
}

/**
 * A single choice from 2–5 short options, shown as one connected control:
 * view switches (list/board), periods (monthly/yearly), filters. It is a
 * radio group (WAI-ARIA APG): one Tab stop, arrow keys move and select,
 * Home/End jump. For several independent on/off choices use ToggleGroup;
 * when each option shows its own panel use Tabs.
 */
function SegmentedControlInner<T extends string>(
  {
    options,
    value: valueProp,
    defaultValue,
    onValueChange,
    size = 'md',
    block = false,
    disabled = false,
    dir,
    name,
    required,
    className,
    ...rest
  }: SegmentedControlProps<T>,
  ref: Ref<HTMLDivElement>,
) {
  const readDir = useDirection(dir);
  const [value, setValue] = useControllable<T | undefined>(
    valueProp,
    defaultValue,
    onValueChange as (v: T | undefined) => void,
  );
  const selectedIndex = options.findIndex((o) => o.value === value);
  // The one Tab stop: the selected option, or the first enabled one.
  const tabStop =
    selectedIndex >= 0 && !options[selectedIndex]?.disabled
      ? selectedIndex
      : nextEnabled(options, 0, 'first', disabled);

  const handleKeyDown = (event: KeyboardEvent<HTMLButtonElement>, from: number) => {
    const step = stepFor(event.key, readDir(event.currentTarget));
    if (step === null) return;
    const to = nextEnabled(options, from, step, disabled);
    const option = options[to];
    if (!option) return;
    event.preventDefault();
    focusOption(event.currentTarget.parentElement, to);
    // Radio pattern: selection follows focus.
    if (option.value !== value) setValue(option.value);
  };

  return (
    <div
      ref={ref}
      role="radiogroup"
      {...rest}
      dir={dir}
      aria-disabled={disabled || undefined}
      aria-required={required || undefined}
      className={cx('mn-segmented-control', className)}
      data-size={size}
      data-block={block || undefined}
      data-disabled={disabled || undefined}
    >
      {options.map((option, index) => {
        const checked = index === selectedIndex;
        const off = disabled || option.disabled;
        return (
          <button
            key={option.value}
            type="button"
            role="radio"
            aria-checked={checked}
            tabIndex={index === tabStop ? 0 : -1}
            disabled={off || undefined}
            title={option.hideLabel ? option.label : undefined}
            data-index={index}
            data-state={checked ? 'on' : 'off'}
            data-icon-only={option.hideLabel || undefined}
            className="mn-segmented-control__item"
            onKeyDown={(event) => handleKeyDown(event, index)}
            onClick={() => {
              if (!checked) setValue(option.value);
            }}
          >
            <OptionContent option={option} />
          </button>
        );
      })}
      {name !== undefined ? (
        <input type="hidden" name={name} value={value ?? ''} disabled={disabled || undefined} />
      ) : null}
    </div>
  );
}

export const SegmentedControl = forwardRef(SegmentedControlInner) as <T extends string = string>(
  props: SegmentedControlProps<T> & { ref?: Ref<HTMLDivElement> },
) => ReactElement;
