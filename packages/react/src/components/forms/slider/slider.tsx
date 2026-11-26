'use client';

import {
  forwardRef,
  useRef,
  useState,
  type CSSProperties,
  type HTMLAttributes,
  type KeyboardEvent,
  type PointerEvent,
  type ReactNode,
} from 'react';
import { cx } from '../../../internal/cx.js';
import { useFieldContext, useFieldControl } from '../form-field/index.js';
import { decimalPlaces, roundTo } from '../number-input/index.js';
import { elementDirection, useDirection } from '../../../internal/direction.js';
import { numberFormat, useClientLocale } from '../../../internal/dates/client-time.js';

/** A labelled point under the track. */
export interface SliderMark {
  value: number;
  /** Text under the point. Omit for a tick only. */
  label?: ReactNode;
}

/** Rounds `value` to the nearest `min + k × step` inside `[min, max]`, without float drift. */
export function snapToStep(value: number, min: number, max: number, step: number): number {
  const places = Math.max(decimalPlaces(step), decimalPlaces(min));
  const k = step > 0 ? Math.round((value - min) / step) : 0;
  const snapped = roundTo(min + k * step, places);
  return Math.min(max, Math.max(min, snapped));
}

/** Where `value` sits on the track, as a percentage from 0 to 100. */
export function percentOf(value: number, min: number, max: number): number {
  if (max <= min) return 0;
  return Math.min(100, Math.max(0, ((value - min) / (max - min)) * 100));
}

/**
 * The value under a pointer at `clientX` on a track with bounding box `rect`,
 * snapped to the step. In right-to-left layouts the track runs from the right.
 */
export function valueFromPointer(
  clientX: number,
  rect: { left: number; width: number },
  options: { min: number; max: number; step: number; rtl?: boolean },
): number {
  const { min, max, step, rtl = false } = options;
  if (rect.width <= 0) return min;
  let ratio = (clientX - rect.left) / rect.width;
  ratio = Math.min(1, Math.max(0, rtl ? 1 - ratio : ratio));
  return snapToStep(min + ratio * (max - min), min, max, step);
}

/**
 * The new value for a slider key (WAI-ARIA APG), or `null` when the key does
 * not move the slider. ArrowRight/ArrowUp add `step` (ArrowLeft adds in RTL),
 * PageUp/PageDown move by `largeStep`, Home/End go to `min`/`max`.
 */
export function sliderKeyValue(
  key: string,
  value: number,
  options: { min: number; max: number; step: number; largeStep: number; rtl?: boolean },
): number | null {
  const { min, max, step, largeStep, rtl = false } = options;
  const forward = rtl ? 'ArrowLeft' : 'ArrowRight';
  const backward = rtl ? 'ArrowRight' : 'ArrowLeft';
  let next: number;
  switch (key) {
    case forward:
    case 'ArrowUp':
      next = value + step;
      break;
    case backward:
    case 'ArrowDown':
      next = value - step;
      break;
    case 'PageUp':
      next = value + largeStep;
      break;
    case 'PageDown':
      next = value - largeStep;
      break;
    case 'Home':
      return min;
    case 'End':
      return max;
    default:
      return null;
  }
  return snapToStep(next, min, max, step);
}

/**
 * True when `el` is inside a right-to-left subtree (nearest `dir`, then the
 * computed direction). Read at event time only. Components use `useDirection`
 * so a `DirectionProvider` wins; this stays for callers outside React.
 */
export function isRtl(el: Element | null): boolean {
  return elementDirection(el) === 'rtl';
}

export interface SliderProps extends Omit<
  HTMLAttributes<HTMLDivElement>,
  'onChange' | 'defaultValue' | 'dir'
> {
  /** Controlled value. */
  value?: number | undefined;
  /** Uncontrolled initial value. Default `min`. */
  defaultValue?: number | undefined;
  /** Called on every change, while dragging and on each key press. */
  onValueChange?: ((value: number) => void) | undefined;
  /** Called once the user finishes: on pointer release and after each key press. Save here. */
  onValueCommit?: ((value: number) => void) | undefined;
  /** Lowest value. Default 0. */
  min?: number | undefined;
  /** Highest value. Default 100. */
  max?: number | undefined;
  /** Distance between allowed values. Default 1. */
  step?: number | undefined;
  /** Distance for PageUp / PageDown. Default a tenth of the range, rounded to the step (at least one step). */
  largeStep?: number | undefined;
  /** Labelled points under the track, e.g. `[{ value: 0, label: 'Off' }, { value: 100, label: 'Max' }]`. */
  marks?: readonly SliderMark[] | undefined;
  /** Spoken (and shown) value. Default the number formatted with `Intl.NumberFormat(locale)`. */
  formatValue?: ((value: number) => string) | undefined;
  /** BCP 47 locale for the default `formatValue`. */
  locale?: string | undefined;
  /** Show the formatted value at the end of the track. Default `false`. */
  showValue?: boolean | undefined;
  disabled?: boolean | undefined;
  /** Marks the value invalid (also inherited from a surrounding Field). */
  invalid?: boolean | undefined;
  /** Form field name; renders a hidden input carrying the value. */
  name?: string | undefined;
  /** Track and thumb size. Default `md`. Exposed as `data-size`. */
  size?: 'sm' | 'md' | undefined;
}

/** Default PageUp/PageDown distance: a tenth of the range, on the step grid. */
export function defaultLargeStep(min: number, max: number, step: number): number {
  const tenth = (max - min) / 10;
  return Math.max(step, roundTo(Math.round(tenth / step) * step, decimalPlaces(step)));
}

/**
 * Chooses one number on a track by dragging, clicking or the keyboard. The
 * thumb is a WAI-ARIA `slider`: arrows step, PageUp/PageDown take large
 * steps, Home/End jump to the ends, and the spoken value is formatted.
 * `onValueChange` streams changes; `onValueCommit` fires when the user is done.
 * Inside a Field the thumb takes the Field's label, description and error.
 */
export const Slider = forwardRef<HTMLDivElement, SliderProps>(function Slider(
  {
    value: valueProp,
    defaultValue,
    onValueChange,
    onValueCommit,
    min = 0,
    max = 100,
    step = 1,
    largeStep,
    marks,
    formatValue,
    locale,
    showValue = false,
    disabled: disabledProp,
    invalid,
    name,
    size = 'md',
    className,
    style,
    id,
    'aria-label': ariaLabel,
    'aria-labelledby': ariaLabelledBy,
    'aria-describedby': ariaDescribedBy,
    ...rest
  },
  ref,
) {
  const readDir = useDirection();
  const field = useFieldContext();
  const control = useFieldControl(
    { id, disabled: disabledProp, 'aria-describedby': ariaDescribedBy },
    { invalid },
  );
  const disabled = !!control.disabled;
  const [internal, setInternal] = useState(() => snapToStep(defaultValue ?? min, min, max, step));
  const value = valueProp !== undefined ? valueProp : internal;
  const thumbRef = useRef<HTMLDivElement | null>(null);
  const dragging = useRef(false);
  const latest = useRef(value);
  latest.current = value;

  // Without a `locale` the server and the hydrating client both use en-US, then the viewer's own.
  const resolvedLocale = useClientLocale(locale);
  const format = formatValue ?? ((n: number) => numberFormat(resolvedLocale).format(n));
  const big = largeStep ?? defaultLargeStep(min, max, step);
  const labelledBy = ariaLabelledBy ?? (ariaLabel || !field ? undefined : field.labelId);

  const change = (next: number) => {
    if (next === latest.current) return;
    latest.current = next;
    if (valueProp === undefined) setInternal(next);
    onValueChange?.(next);
  };

  const handleKeyDown = (e: KeyboardEvent<HTMLDivElement>) => {
    if (disabled) return;
    const next = sliderKeyValue(e.key, value, {
      min,
      max,
      step,
      largeStep: big,
      rtl: readDir(e.currentTarget) === 'rtl',
    });
    if (next === null) return;
    e.preventDefault();
    change(next);
    onValueCommit?.(next);
  };

  const fromEvent = (e: PointerEvent<HTMLDivElement>) =>
    valueFromPointer(e.clientX, e.currentTarget.getBoundingClientRect(), {
      min,
      max,
      step,
      rtl: readDir(e.currentTarget) === 'rtl',
    });

  const handlePointerDown = (e: PointerEvent<HTMLDivElement>) => {
    if (disabled || e.button !== 0) return;
    e.preventDefault();
    dragging.current = true;
    e.currentTarget.setPointerCapture?.(e.pointerId);
    thumbRef.current?.focus();
    change(fromEvent(e));
  };
  const handlePointerMove = (e: PointerEvent<HTMLDivElement>) => {
    if (!dragging.current) return;
    change(fromEvent(e));
  };
  const handlePointerUp = (e: PointerEvent<HTMLDivElement>) => {
    if (!dragging.current) return;
    dragging.current = false;
    e.currentTarget.releasePointerCapture?.(e.pointerId);
    onValueCommit?.(latest.current);
  };

  const pct = percentOf(value, min, max);
  const text = format(value);

  return (
    <div
      ref={ref}
      {...rest}
      className={cx('mn-slider', className)}
      style={{ ...style, '--_pct': `${pct}%` } as CSSProperties}
      data-size={size}
      data-disabled={disabled || undefined}
      data-invalid={control['aria-invalid'] ? '' : undefined}
      data-marks={marks?.length ? '' : undefined}
    >
      <div className="mn-slider__body">
        <div
          className="mn-slider__control"
          onPointerDown={handlePointerDown}
          onPointerMove={handlePointerMove}
          onPointerUp={handlePointerUp}
          onPointerCancel={handlePointerUp}
        >
          <div className="mn-slider__track">
            <div className="mn-slider__range" />
          </div>
          <div
            ref={thumbRef}
            role="slider"
            id={control.id}
            tabIndex={disabled ? -1 : 0}
            className="mn-slider__thumb"
            aria-label={ariaLabel}
            aria-labelledby={labelledBy}
            aria-describedby={control['aria-describedby']}
            aria-invalid={control['aria-invalid']}
            aria-valuemin={min}
            aria-valuemax={max}
            aria-valuenow={value}
            aria-valuetext={text}
            aria-orientation="horizontal"
            aria-disabled={disabled || undefined}
            onKeyDown={handleKeyDown}
          />
        </div>
        {marks?.length ? (
          <div className="mn-slider__marks" aria-hidden>
            {marks.map((mark) => (
              <span
                key={mark.value}
                className="mn-slider__mark"
                style={{ '--_at': `${percentOf(mark.value, min, max)}%` } as CSSProperties}
                data-active={mark.value <= value || undefined}
              >
                {mark.label !== undefined ? (
                  <span className="mn-slider__mark-label">{mark.label}</span>
                ) : null}
              </span>
            ))}
          </div>
        ) : null}
      </div>
      {showValue ? (
        <output className="mn-slider__value" aria-hidden>
          {text}
        </output>
      ) : null}
      {name ? (
        <input type="hidden" name={name} value={String(value)} disabled={disabled || undefined} />
      ) : null}
    </div>
  );
});
