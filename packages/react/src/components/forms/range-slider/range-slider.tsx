'use client';

import {
  forwardRef,
  useId,
  useRef,
  useState,
  type CSSProperties,
  type HTMLAttributes,
  type KeyboardEvent,
  type PointerEvent,
} from 'react';
import { cx } from '../../../internal/cx.js';
import { numberFormat, useClientLocale } from '../../../internal/dates/client-time.js';
import { useFieldContext, useFieldControl } from '../form-field/index.js';
import { roundTo, decimalPlaces } from '../number-input/index.js';
import {
  defaultLargeStep,
  isRtl,
  percentOf,
  sliderKeyValue,
  snapToStep,
  valueFromPointer,
  type SliderMark,
} from '../slider/index.js';

/** A low and a high value, low first. */
export type RangeValue = readonly [number, number];

/** Interface text, for translation. */
export interface RangeSliderMessages {
  /** Added to the group label to name the low thumb. Default "Minimum". */
  minimum: string;
  /** Added to the group label to name the high thumb. Default "Maximum". */
  maximum: string;
  /** The shown range when `showValue` is set. Default "20 – 80". */
  range: (low: string, high: string) => string;
}

export const RANGE_SLIDER_MESSAGES: RangeSliderMessages = {
  minimum: 'Minimum',
  maximum: 'Maximum',
  range: (low, high) => `${low} – ${high}`,
};

export interface RangeSliderProps extends Omit<
  HTMLAttributes<HTMLDivElement>,
  'onChange' | 'defaultValue' | 'dir'
> {
  /** Controlled `[low, high]`. */
  value?: RangeValue | undefined;
  /** Uncontrolled initial `[low, high]`. Default `[min, max]`. */
  defaultValue?: RangeValue | undefined;
  /** Called on every change, while dragging and on each key press. */
  onValueChange?: ((value: [number, number]) => void) | undefined;
  /** Called once the user finishes: on pointer release and after each key press. Save here. */
  onValueCommit?: ((value: [number, number]) => void) | undefined;
  /** Lowest value. Default 0. */
  min?: number | undefined;
  /** Highest value. Default 100. */
  max?: number | undefined;
  /** Distance between allowed values. Default 1. */
  step?: number | undefined;
  /** Distance for PageUp / PageDown. Default a tenth of the range, on the step grid. */
  largeStep?: number | undefined;
  /** Smallest gap between the thumbs, in steps. Default 0 (they may meet but never cross). */
  minStepsBetween?: number | undefined;
  /** Labelled points under the track. */
  marks?: readonly SliderMark[] | undefined;
  /** Spoken (and shown) value of each thumb. Default `Intl.NumberFormat(locale)`. */
  formatValue?: ((value: number) => string) | undefined;
  /** BCP 47 locale for the default `formatValue`. */
  locale?: string | undefined;
  /** Show the formatted range at the end of the track. Default `false`. */
  showValue?: boolean | undefined;
  disabled?: boolean | undefined;
  /** Marks the value invalid (also inherited from a surrounding Field). */
  invalid?: boolean | undefined;
  /** Form field name; renders two hidden inputs with this name, low then high. */
  name?: string | undefined;
  /** Track and thumb size. Default `md`. Exposed as `data-size`. */
  size?: 'sm' | 'md' | undefined;
  /** Interface text, for translation. */
  messages?: Partial<RangeSliderMessages> | undefined;
}

/**
 * Chooses a low and a high value on one track (a price or date range). Each
 * thumb is its own WAI-ARIA `slider`, named from the group label plus
 * "Minimum" or "Maximum"; the thumbs can meet but never cross, and each
 * thumb's `aria-valuemin`/`aria-valuemax` reflect the other thumb. Clicking
 * the track moves the nearer thumb.
 */
export const RangeSlider = forwardRef<HTMLDivElement, RangeSliderProps>(function RangeSlider(
  {
    value: valueProp,
    defaultValue,
    onValueChange,
    onValueCommit,
    min = 0,
    max = 100,
    step = 1,
    largeStep,
    minStepsBetween = 0,
    marks,
    formatValue,
    locale,
    showValue = false,
    disabled: disabledProp,
    invalid,
    name,
    size = 'md',
    messages: messagesProp,
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
  const messages = { ...RANGE_SLIDER_MESSAGES, ...messagesProp };
  const autoId = useId();
  const field = useFieldContext();
  const control = useFieldControl(
    { id, disabled: disabledProp, 'aria-describedby': ariaDescribedBy },
    { invalid },
  );
  const disabled = !!control.disabled;
  const rootId = control.id ?? `mn-range-slider-${autoId}`;
  const gap = roundTo(minStepsBetween * step, decimalPlaces(step));

  const normalise = (v: RangeValue): [number, number] => {
    const low = snapToStep(Math.min(v[0], v[1]), min, max, step);
    const high = snapToStep(Math.max(v[0], v[1]), min, max, step);
    return [low, high];
  };
  const [internal, setInternal] = useState<[number, number]>(() =>
    normalise(defaultValue ?? [min, max]),
  );
  const value: RangeValue = valueProp ?? internal;
  const [low, high] = value;
  const latest = useRef<[number, number]>([low, high]);
  latest.current = [low, high];
  const thumbs = useRef<(HTMLDivElement | null)[]>([]);
  const dragging = useRef<0 | 1 | null>(null);

  // Without a `locale` the server and the hydrating client both use en-US, then the viewer's own.
  const resolvedLocale = useClientLocale(locale);
  const format = formatValue ?? ((n: number) => numberFormat(resolvedLocale).format(n));
  const big = largeStep ?? defaultLargeStep(min, max, step);
  const groupLabel = ariaLabelledBy ?? (ariaLabel ? rootId : field ? field.labelId : undefined);

  /** Bounds of thumb `index` given the other thumb. */
  const boundsOf = (index: 0 | 1, current: RangeValue) =>
    index === 0
      ? { min, max: Math.max(min, roundTo(current[1] - gap, decimalPlaces(step))) }
      : { min: Math.min(max, roundTo(current[0] + gap, decimalPlaces(step))), max };

  const change = (index: 0 | 1, raw: number) => {
    const current = latest.current;
    const b = boundsOf(index, current);
    const v = Math.min(b.max, Math.max(b.min, raw));
    if (v === current[index]) return null;
    const next: [number, number] = index === 0 ? [v, current[1]] : [current[0], v];
    latest.current = next;
    if (valueProp === undefined) setInternal(next);
    onValueChange?.(next);
    return next;
  };

  const handleKeyDown = (index: 0 | 1) => (e: KeyboardEvent<HTMLDivElement>) => {
    if (disabled) return;
    const b = boundsOf(index, value);
    const next = sliderKeyValue(e.key, value[index], {
      min: b.min,
      max: b.max,
      step,
      largeStep: big,
      rtl: isRtl(e.currentTarget),
    });
    if (next === null) return;
    e.preventDefault();
    const committed = change(index, next);
    if (committed) onValueCommit?.(committed);
  };

  const fromEvent = (e: PointerEvent<HTMLDivElement>) =>
    valueFromPointer(e.clientX, e.currentTarget.getBoundingClientRect(), {
      min,
      max,
      step,
      rtl: isRtl(e.currentTarget),
    });

  const handlePointerDown = (e: PointerEvent<HTMLDivElement>) => {
    if (disabled || e.button !== 0) return;
    e.preventDefault();
    const v = fromEvent(e);
    const [a, b] = latest.current;
    let index: 0 | 1;
    if (v <= a) index = 0;
    else if (v >= b) index = a === b && b === max ? 0 : 1;
    else index = v - a <= b - v ? 0 : 1;
    dragging.current = index;
    e.currentTarget.setPointerCapture?.(e.pointerId);
    thumbs.current[index]?.focus();
    change(index, v);
  };
  const handlePointerMove = (e: PointerEvent<HTMLDivElement>) => {
    if (dragging.current === null) return;
    change(dragging.current, fromEvent(e));
  };
  const handlePointerUp = (e: PointerEvent<HTMLDivElement>) => {
    if (dragging.current === null) return;
    dragging.current = null;
    e.currentTarget.releasePointerCapture?.(e.pointerId);
    onValueCommit?.([...latest.current]);
  };

  const from = percentOf(low, min, max);
  const to = percentOf(high, min, max);

  return (
    <div
      ref={ref}
      role="group"
      {...rest}
      id={rootId}
      aria-label={ariaLabel}
      aria-labelledby={ariaLabel ? undefined : groupLabel}
      className={cx('mn-range-slider', className)}
      style={{ ...style, '--_from': `${from}%`, '--_to': `${to}%` } as CSSProperties}
      data-size={size}
      data-disabled={disabled || undefined}
      data-invalid={control['aria-invalid'] ? '' : undefined}
    >
      <div className="mn-range-slider__body">
        <div
          className="mn-range-slider__control"
          onPointerDown={handlePointerDown}
          onPointerMove={handlePointerMove}
          onPointerUp={handlePointerUp}
          onPointerCancel={handlePointerUp}
        >
          <div className="mn-range-slider__track">
            <div className="mn-range-slider__range" />
          </div>
          {([0, 1] as const).map((index) => {
            const b = boundsOf(index, value);
            const labelId = `mn-range-slider-${autoId}-${index}`;
            return (
              <div
                key={index}
                ref={(el) => {
                  thumbs.current[index] = el;
                }}
                role="slider"
                tabIndex={disabled ? -1 : 0}
                className="mn-range-slider__thumb"
                data-thumb={index === 0 ? 'low' : 'high'}
                style={{ '--_at': `${index === 0 ? from : to}%` } as CSSProperties}
                aria-labelledby={[groupLabel, labelId].filter(Boolean).join(' ')}
                aria-describedby={control['aria-describedby']}
                aria-invalid={control['aria-invalid']}
                aria-valuemin={b.min}
                aria-valuemax={b.max}
                aria-valuenow={value[index]}
                aria-valuetext={format(value[index])}
                aria-orientation="horizontal"
                aria-disabled={disabled || undefined}
                onKeyDown={handleKeyDown(index)}
              />
            );
          })}
          <span id={`mn-range-slider-${autoId}-0`} hidden>
            {messages.minimum}
          </span>
          <span id={`mn-range-slider-${autoId}-1`} hidden>
            {messages.maximum}
          </span>
        </div>
        {marks?.length ? (
          <div className="mn-range-slider__marks" aria-hidden>
            {marks.map((mark) => (
              <span
                key={mark.value}
                className="mn-range-slider__mark"
                style={{ '--_at': `${percentOf(mark.value, min, max)}%` } as CSSProperties}
                data-active={(mark.value >= low && mark.value <= high) || undefined}
              >
                {mark.label !== undefined ? (
                  <span className="mn-range-slider__mark-label">{mark.label}</span>
                ) : null}
              </span>
            ))}
          </div>
        ) : null}
      </div>
      {showValue ? (
        <output className="mn-range-slider__value" aria-hidden>
          {messages.range(format(low), format(high))}
        </output>
      ) : null}
      {name ? (
        <>
          <input type="hidden" name={name} value={String(low)} disabled={disabled || undefined} />
          <input type="hidden" name={name} value={String(high)} disabled={disabled || undefined} />
        </>
      ) : null}
    </div>
  );
});
