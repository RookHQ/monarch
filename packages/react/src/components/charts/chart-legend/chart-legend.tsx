'use client';

import {
  forwardRef,
  useRef,
  useState,
  type HTMLAttributes,
  type KeyboardEvent,
  type ReactNode,
  type Ref,
} from 'react';
import { cx } from '../../../internal/cx.js';
import {
  colorStyle,
  defaultChartColor,
  useControllable,
  type ChartColor,
} from '../../../internal/charts/index.js';
import { useDirection } from '../../../internal/direction.js';

/** One legend entry: a series (or segment) name with its colour slot. */
export interface ChartLegendEntry {
  /** Stable id of the series; the value passed to `onHiddenSeriesChange` and `onActiveChange`. */
  key: string;
  /** Visible name. Always shown, so identity never rests on colour alone. */
  label: string;
  /** Palette slot (1–5 or `neutral`). Default: the entry's position, as in every chart. */
  color?: ChartColor | undefined;
  /** Draw the marker dashed (line) or outlined (square), matching a dashed series. */
  dashed?: boolean | undefined;
  /** A value after the label, such as the series total or latest value (already formatted). */
  value?: ReactNode | undefined;
}

/** Marker beside each label: a short line (line and area charts), a square (bars) or a dot. */
export type ChartLegendMarkerShape = 'line' | 'square' | 'dot';

export interface ChartLegendMessages {
  /** Accessible name of the legend. Default "Legend". */
  label: string;
  /** Text of the reset button shown while any series is hidden. Default "Show all". */
  showAll: string;
  /** Spoken name of a toggle. Default "{label}, {value}" when a value is set, else the label. */
  entry: (label: string, value: string | undefined) => string;
}

const DEFAULT_MESSAGES: ChartLegendMessages = {
  label: 'Legend',
  showAll: 'Show all',
  entry: (label, value) => (value ? `${label}, ${value}` : label),
};

export interface ChartLegendProps extends Omit<HTMLAttributes<HTMLElement>, 'children'> {
  /** The entries, in series order. */
  items: readonly ChartLegendEntry[];
  /** Marker shape. Default `line`. Exposed as `data-marker`. */
  marker?: ChartLegendMarkerShape | undefined;
  /** `horizontal` (default) wraps entries in a row; `vertical` stacks them. Exposed as `data-orientation`. */
  orientation?: 'horizontal' | 'vertical' | undefined;
  /**
   * Make each entry a toggle button (`aria-pressed`) that shows or hides its
   * series. The legend becomes a toolbar: one tab stop, arrow keys between
   * entries. Default `false` (a plain list).
   */
  toggleable?: boolean | undefined;
  /** Controlled list of hidden keys. */
  hiddenSeries?: readonly string[] | undefined;
  /** Initial hidden keys when uncontrolled. */
  defaultHiddenSeries?: readonly string[] | undefined;
  /** Called with the new hidden list after a toggle or Show all. */
  onHiddenSeriesChange?: ((hidden: string[]) => void) | undefined;
  /**
   * Stop the last visible series from being hidden, so the chart is never
   * blank. Its toggle stays focusable but is `aria-disabled`. Default `true`.
   */
  keepOneVisible?: boolean | undefined;
  /** Show a "Show all" button while anything is hidden. Default `true` when toggleable. */
  showAllButton?: boolean | undefined;
  /**
   * Called with the entry under the pointer or keyboard focus (`null` on
   * leave), so a chart can emphasise that series.
   */
  onActiveChange?: ((key: string | null) => void) | undefined;
  /** Interface text, for translation. */
  messages?: Partial<ChartLegendMessages> | undefined;
}

function textOf(node: ReactNode): string | undefined {
  return typeof node === 'string' || typeof node === 'number' ? String(node) : undefined;
}

/**
 * A chart legend you can place anywhere: beside a chart, under a group of
 * charts or in a dashboard header. Every entry has its text label and a
 * marker in its palette slot; with `toggleable` each entry shows or hides
 * its series, as a keyboard-friendly toolbar with a Show all reset.
 */
export const ChartLegend = forwardRef<HTMLElement, ChartLegendProps>(function ChartLegend(
  {
    items,
    marker = 'line',
    orientation = 'horizontal',
    toggleable = false,
    hiddenSeries: hiddenProp,
    defaultHiddenSeries: defaultHidden,
    onHiddenSeriesChange: onHiddenChange,
    keepOneVisible = true,
    showAllButton,
    onActiveChange,
    messages,
    className,
    onKeyDown,
    ...rest
  },
  ref,
) {
  const readDir = useDirection();
  const m = { ...DEFAULT_MESSAGES, ...messages };
  const [hidden, setHidden] = useControllable<readonly string[]>(
    hiddenProp,
    defaultHidden ?? [],
    onHiddenChange as ((next: readonly string[]) => void) | undefined,
  );
  const toggle = (key: string) =>
    setHidden(hidden.includes(key) ? hidden.filter((k) => k !== key) : [...hidden, key]);
  const [focusIndex, setFocusIndex] = useState(0);
  const buttons = useRef<(HTMLButtonElement | null)[]>([]);

  const entries = items.map((item, i) => ({
    ...item,
    color: item.color ?? defaultChartColor(i),
    isHidden: hidden.includes(item.key),
  }));
  const visibleCount = entries.filter((e) => !e.isHidden).length;
  const anyHidden = entries.some((e) => e.isHidden);
  const withReset = toggleable && (showAllButton ?? true) && anyHidden;

  const swatch = (color: ChartColor, dashed: boolean | undefined) => (
    <span
      className="mn-chart__swatch"
      data-marker={marker}
      data-dashed={dashed || undefined}
      style={colorStyle(color)}
      aria-hidden="true"
    />
  );

  if (!toggleable) {
    return (
      <ul
        ref={ref as Ref<HTMLUListElement>}
        aria-label={m.label}
        {...rest}
        className={cx('mn-chart-legend', 'mn-chart__legend', className)}
        data-orientation={orientation}
        data-marker={marker}
      >
        {entries.map((e) => (
          <li
            key={e.key}
            className="mn-chart__legend-item"
            onPointerEnter={onActiveChange ? () => onActiveChange(e.key) : undefined}
            onPointerLeave={onActiveChange ? () => onActiveChange(null) : undefined}
          >
            {swatch(e.color, e.dashed)}
            <span className="mn-chart__legend-label">{e.label}</span>
            {e.value !== undefined && e.value !== null ? (
              <span className="mn-chart-legend__value">{e.value}</span>
            ) : null}
          </li>
        ))}
      </ul>
    );
  }

  const count = entries.length + (withReset ? 1 : 0);
  const current = Math.min(focusIndex, Math.max(0, count - 1));
  const focusAt = (i: number) => {
    const next = (i + count) % count;
    setFocusIndex(next);
    buttons.current[next]?.focus();
  };

  const handleKeyDown = (event: KeyboardEvent<HTMLElement>) => {
    onKeyDown?.(event);
    if (event.defaultPrevented || count === 0) return;
    const vertical = orientation === 'vertical';
    const rtl = readDir(event.currentTarget) === 'rtl';
    const forward = vertical ? 'ArrowDown' : rtl ? 'ArrowLeft' : 'ArrowRight';
    const back = vertical ? 'ArrowUp' : rtl ? 'ArrowRight' : 'ArrowLeft';
    if (event.key === forward) focusAt(current + 1);
    else if (event.key === back) focusAt(current - 1);
    else if (event.key === 'Home') focusAt(0);
    else if (event.key === 'End') focusAt(count - 1);
    else return;
    event.preventDefault();
  };

  const showAll = () => {
    // Keys the legend does not know about (series it does not list) stay hidden.
    const keys = new Set(entries.map((e) => e.key));
    setHidden(hidden.filter((k) => !keys.has(k)));
    // The reset button is about to disappear, so focus moves to the first entry.
    setFocusIndex(0);
    buttons.current[0]?.focus();
  };

  return (
    <div
      ref={ref as Ref<HTMLDivElement>}
      role="toolbar"
      aria-label={m.label}
      aria-orientation={orientation}
      {...rest}
      className={cx('mn-chart-legend', 'mn-chart__legend', className)}
      data-orientation={orientation}
      data-marker={marker}
      data-toggleable=""
      onKeyDown={handleKeyDown}
    >
      {entries.map((e, i) => {
        const locked = keepOneVisible && !e.isHidden && visibleCount <= 1;
        const valueText = textOf(e.value);
        return (
          <button
            key={e.key}
            ref={(el) => {
              buttons.current[i] = el;
            }}
            type="button"
            className="mn-chart__legend-item mn-chart__legend-toggle mn-chart-legend__toggle"
            data-hidden={e.isHidden || undefined}
            aria-pressed={!e.isHidden}
            aria-disabled={locked || undefined}
            aria-label={valueText !== undefined ? m.entry(e.label, valueText) : undefined}
            tabIndex={i === current ? 0 : -1}
            onFocus={() => {
              setFocusIndex(i);
              onActiveChange?.(e.key);
            }}
            onBlur={onActiveChange ? () => onActiveChange(null) : undefined}
            onPointerEnter={onActiveChange ? () => onActiveChange(e.key) : undefined}
            onPointerLeave={onActiveChange ? () => onActiveChange(null) : undefined}
            onClick={() => {
              if (!locked) toggle(e.key);
            }}
          >
            {swatch(e.color, e.dashed)}
            <span className="mn-chart__legend-label">{e.label}</span>
            {e.value !== undefined && e.value !== null ? (
              <span className="mn-chart-legend__value">{e.value}</span>
            ) : null}
          </button>
        );
      })}
      {withReset ? (
        <button
          ref={(el) => {
            buttons.current[entries.length] = el;
          }}
          type="button"
          className="mn-chart-legend__reset"
          tabIndex={current === entries.length ? 0 : -1}
          onFocus={() => setFocusIndex(entries.length)}
          onClick={showAll}
        >
          {m.showAll}
        </button>
      ) : null}
    </div>
  );
});
