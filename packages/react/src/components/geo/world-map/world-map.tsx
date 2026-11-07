'use client';

import {
  forwardRef,
  useId,
  useMemo,
  useState,
  type FocusEvent,
  type KeyboardEvent,
  type PointerEvent,
} from 'react';
import {
  ChartLiveRegion,
  ChartTooltip,
  DEFAULT_INTERACTION_LABELS,
  colorStyle,
  createValueFormatter,
  useControllable,
  useElementWidth,
  type ChartColor,
  type ChartInteractionLabels,
  type ChartTableData,
  type ValueFormatter,
} from '../../../internal/charts/index.js';
import {
  ChartFrame,
  type ChartFrameLabels,
  type ChartFrameProps,
} from '../../charts/chart-frame/index.js';
import {
  HeatmapLegend,
  heatmapScale,
  type HeatmapLegendLabels,
} from '../../charts/heatmap/index.js';
import { WORLD_MAP_CENTROIDS, WORLD_MAP_SHAPES, WORLD_MAP_VIEWBOX } from './world-map.data.js';

export { WORLD_MAP_CENTROIDS, WORLD_MAP_SHAPES, WORLD_MAP_VIEWBOX } from './world-map.data.js';

/** An extra line in the tooltip and table, e.g. a second metric. */
export interface WorldMapDetail {
  label: string;
  value: string | number;
}

/** A value for one country. */
export interface WorldMapDatum {
  /** ISO 3166-1 alpha-2 code, e.g. `DE` (case does not matter). */
  country: string;
  value: number;
  /** Extra lines for the tooltip and table columns (users, sessions…). */
  details?: readonly WorldMapDetail[] | undefined;
}

/** A value that belongs to no single country, e.g. "Global" or "EU". Shown as a tile under the map. */
export interface WorldMapExtra {
  key: string;
  label: string;
  value: number;
}

/** `choropleth` fills each country; `bubble` draws a circle sized by value on a neutral map. */
export type WorldMapVariant = 'choropleth' | 'bubble';

/** Words the map adds to ChartFrame's; pass translations here. */
export interface WorldMapLabels
  extends ChartFrameLabels, Partial<ChartInteractionLabels>, HeatmapLegendLabels {
  /** Table column for countries. Default "Country". */
  country?: string | undefined;
  /** Table column and tooltip row for values. Default "Value". */
  value?: string | undefined;
  /** Name of the list of extra values. Default "Other values". */
  extras?: string | undefined;
  /** Note for codes that are not on the map. Default "Not on the map: {names}". */
  unplaced?: ((names: string, total: string) => string) | undefined;
  /** Generated summary; receives the number of countries and the top three as text. */
  summary?: ((countries: number, top: string) => string) | undefined;
}

export interface WorldMapProps extends Omit<
  ChartFrameProps,
  | 'children'
  | 'legend'
  | 'legendMarker'
  | 'onLegendToggle'
  | 'table'
  | 'labels'
  | 'height'
  | 'color'
> {
  /** One value per country. */
  data: readonly WorldMapDatum[];
  /** `choropleth` (default) or `bubble`. Exposed as `data-variant`. */
  variant?: WorldMapVariant | undefined;
  /** Palette slot of the sequential ramp or bubbles. Default 1. */
  color?: ChartColor | undefined;
  /** Three ascending cut points for levels 2–4. Default: quarters of the largest value. */
  thresholds?: readonly number[] | undefined;
  /** Values that belong to no country ("Global", "EU"), shown as tiles under the map and in the table. */
  extras?: readonly WorldMapExtra[] | undefined;
  /** Show the "Less … More" legend (choropleth). Default `true`. */
  showLegend?: boolean | undefined;
  /** Country names. Default: `Intl.DisplayNames` in `locale`, falling back to the code. */
  countryName?: ((code: string) => string) | undefined;
  /** BCP 47 locale for numbers and country names. Pass it when rendering on the server. */
  locale?: string | undefined;
  /** `Intl.NumberFormat` options for values. */
  valueFormat?: Intl.NumberFormatOptions | undefined;
  /** Formats values. Wins over `valueFormat`. */
  formatValue?: ValueFormatter | undefined;
  /** Controlled inspected country code (`null` for none). */
  activeCountry?: string | null | undefined;
  onActiveCountryChange?: ((country: string | null) => void) | undefined;
  /** Click, Enter or Space on a country with a value, for drill-in. */
  onCountrySelect?: ((datum: WorldMapDatum) => void) | undefined;
  /** Replaces the generated text summary (the map's accessible name). */
  summary?: string | undefined;
  labels?: WorldMapLabels | undefined;
}

const W = WORLD_MAP_VIEWBOX.width;
const H = WORLD_MAP_VIEWBOX.height;
const FALLBACK_WIDTH = 640;

const namesCache = new Map<string, Intl.DisplayNames | null>();
function regionNames(locale: string | undefined): Intl.DisplayNames | null {
  const key = locale ?? '';
  if (!namesCache.has(key)) {
    try {
      namesCache.set(key, new Intl.DisplayNames(locale ? [locale] : undefined, { type: 'region' }));
    } catch {
      namesCache.set(key, null);
    }
  }
  return namesCache.get(key) ?? null;
}

/**
 * Values by country on a simplified world map, inside ChartFrame: a
 * choropleth (or bubbles) in one palette colour with a "Less … More" legend,
 * tiles for values that belong to no country, tooltips on hover, touch and
 * keyboard, drill-in and a data table.
 */
export const WorldMap = forwardRef<HTMLElement, WorldMapProps>(function WorldMap(
  {
    data,
    variant = 'choropleth',
    color = 1,
    thresholds,
    extras = [],
    showLegend = true,
    countryName,
    locale,
    valueFormat,
    formatValue,
    activeCountry,
    onActiveCountryChange,
    onCountrySelect,
    summary,
    labels,
    status = 'ready',
    footer,
    ...frame
  },
  ref,
) {
  const hintId = useId();
  const il = {
    ...DEFAULT_INTERACTION_LABELS,
    chart: 'map',
    keyboardHint: 'Use the arrow keys to go through the countries, largest value first.',
    ...labels,
  };
  const words = {
    country: labels?.country ?? 'Country',
    value: labels?.value ?? 'Value',
    extras: labels?.extras ?? 'Other values',
  };
  const fmt = createValueFormatter(locale, valueFormat, formatValue);
  const names = regionNames(locale);
  const nameOf = (code: string) => {
    if (countryName) return countryName(code);
    try {
      return names?.of(code) ?? code;
    } catch {
      return code;
    }
  };
  const [measureRef, measured] = useElementWidth<HTMLDivElement>();
  const px = (measured ?? FALLBACK_WIDTH) / W;

  // Clean data: upper-case codes, finite values, the last entry per code wins.
  const rows = useMemo(() => {
    const m = new Map<string, WorldMapDatum>();
    for (const d of data) {
      if (!Number.isFinite(d.value)) continue;
      const code = d.country.trim().toUpperCase();
      m.set(code, { ...d, country: code });
    }
    return [...m.values()].sort((a, b) => b.value - a.value || a.country.localeCompare(b.country));
  }, [data]);
  const byCode = new Map(rows.map((d) => [d.country, d]));
  const placed = rows.filter((d) => WORLD_MAP_SHAPES[d.country] || WORLD_MAP_CENTROIDS[d.country]);
  const unplaced = rows.filter((d) => !placed.includes(d));
  const scale = heatmapScale(
    rows.map((d) => d.value),
    thresholds,
  );
  const max = Math.max(0, ...rows.map((d) => d.value));

  // Inspection, over the placed countries in value order.
  const [active, setActive] = useControllable<string | null>(
    activeCountry,
    null,
    onActiveCountryChange,
  );
  const [source, setSource] = useState<'pointer' | 'keyboard'>('pointer');
  const current = active ? active.toUpperCase() : null;
  const index = current ? placed.findIndex((d) => d.country === current) : -1;
  const interactive = status === 'ready' && placed.length > 0;
  const go = (i: number, from: 'pointer' | 'keyboard') => {
    const d = placed[Math.max(0, Math.min(placed.length - 1, i))];
    if (!d) return;
    setSource(from);
    setActive(d.country);
  };

  const onKeyDown = (e: KeyboardEvent<HTMLDivElement>) => {
    if (!interactive) return;
    const n = placed.length;
    const page = Math.max(1, Math.ceil(n / 10));
    switch (e.key) {
      case 'ArrowRight':
      case 'ArrowDown':
        go(index < 0 ? 0 : index + 1, 'keyboard');
        break;
      case 'ArrowLeft':
      case 'ArrowUp':
        go(index < 0 ? n - 1 : index - 1, 'keyboard');
        break;
      case 'Home':
        go(0, 'keyboard');
        break;
      case 'End':
        go(n - 1, 'keyboard');
        break;
      case 'PageDown':
        go(Math.max(0, index) + page, 'keyboard');
        break;
      case 'PageUp':
        go(Math.max(0, index) - page, 'keyboard');
        break;
      case 'Escape':
        if (current === null) return;
        setActive(null);
        break;
      case 'Enter':
      case ' ': {
        const d = current ? byCode.get(current) : undefined;
        if (d && onCountrySelect) {
          e.preventDefault();
          onCountrySelect(d);
        }
        return;
      }
      default:
        return;
    }
    e.preventDefault();
  };

  const fromPointer = (e: PointerEvent<HTMLDivElement>) => {
    const code = (e.target as Element).closest?.('[data-country]')?.getAttribute('data-country');
    if (code) {
      setSource('pointer');
      setActive(code);
    } else if (e.pointerType === 'mouse' && source === 'pointer' && current !== null) {
      setActive(null);
    }
  };

  const describe = (code: string) => {
    const d = byCode.get(code);
    const parts = [
      d ? fmt(d.value) : il.noValue,
      ...(d?.details ?? []).map((x) => `${x.label} ${x.value}`),
    ];
    return `${nameOf(code)}: ${parts.join(', ')}`;
  };

  // Summary, table, unplaced note.
  const top = rows
    .slice(0, 3)
    .map((d) => `${nameOf(d.country)} ${fmt(d.value)}`)
    .join(', ');
  const generated = labels?.summary
    ? labels.summary(rows.length, top)
    : rows.length
      ? `${rows.length} ${rows.length === 1 ? 'country' : 'countries'}. Highest: ${top}.`
      : il.noValue;
  const detailLabels: string[] = [];
  for (const d of rows)
    for (const x of d.details ?? [])
      if (!detailLabels.includes(x.label)) detailLabels.push(x.label);
  const table: ChartTableData = {
    columns: [
      { key: '__country', label: words.country },
      { key: '__value', label: words.value },
      ...detailLabels.map((l, i) => ({ key: `d${i}`, label: l })),
    ],
    rows: [
      ...rows.map((d) => {
        const row: Record<string, string> = { __country: nameOf(d.country), __value: fmt(d.value) };
        detailLabels.forEach((l, i) => {
          const x = d.details?.find((y) => y.label === l);
          row[`d${i}`] = x ? String(x.value) : '';
        });
        return row;
      }),
      ...extras.map((x) => {
        const row: Record<string, string> = { __country: x.label, __value: fmt(x.value) };
        detailLabels.forEach((_, i) => (row[`d${i}`] = ''));
        return row;
      }),
    ],
  };
  const unplacedNames = unplaced.map((d) => nameOf(d.country)).join(', ');
  const unplacedTotal = fmt(unplaced.reduce((t, d) => t + d.value, 0));
  const unplacedNote = unplaced.length
    ? labels?.unplaced
      ? labels.unplaced(unplacedNames, unplacedTotal)
      : `Not on the map: ${unplacedNames} (${unplacedTotal})`
    : null;

  const hasData = rows.length > 0 || extras.length > 0;
  const resolvedStatus = status === 'ready' && !hasData ? 'empty' : status;
  const bubble = variant === 'bubble';
  const radius = (v: number) => (max > 0 ? 4 + Math.sqrt(Math.max(0, v) / max) * 22 : 4);

  // Shapes: every country; those with values carry their level.
  const shapes = Object.entries(WORLD_MAP_SHAPES).map(([code, d]) => {
    const datum = byCode.get(code);
    const level = !bubble && datum ? scale.level(datum.value) : null;
    return (
      <path
        key={code}
        className="mn-world-map__country"
        data-country={datum ? code : undefined}
        data-code={code}
        data-level={level ?? undefined}
        data-active={!bubble && code === current ? '' : undefined}
        d={d}
      />
    );
  });
  // Countries with a value but too small for the outline get a dot (choropleth) or a bubble.
  const marks = placed
    .filter((d) => bubble || !WORLD_MAP_SHAPES[d.country])
    .slice()
    .sort((a, b) => b.value - a.value)
    .map((d) => {
      const c = WORLD_MAP_CENTROIDS[d.country];
      if (!c) return null;
      return (
        <circle
          key={d.country}
          className={bubble ? 'mn-world-map__bubble' : 'mn-world-map__dot'}
          data-country={d.country}
          data-level={bubble ? undefined : (scale.level(d.value) ?? undefined)}
          data-active={d.country === current ? '' : undefined}
          cx={c[0]}
          cy={c[1]}
          r={bubble ? radius(d.value) : 3.5}
        />
      );
    });

  const anchorCode = current && byCode.has(current) ? current : null;
  const anchor = anchorCode ? WORLD_MAP_CENTROIDS[anchorCode] : undefined;
  const activeDatum = anchorCode ? byCode.get(anchorCode) : undefined;

  const plotProps = {
    tabIndex: interactive ? 0 : -1,
    onKeyDown: onKeyDown,
    onPointerMove: fromPointer,
    onPointerDown: fromPointer,
    onPointerLeave: (e: PointerEvent<HTMLDivElement>) => {
      if (e.pointerType === 'mouse' && source === 'pointer') setActive(null);
    },
    onFocus: (e: FocusEvent<HTMLDivElement>) => {
      if (e.target !== e.currentTarget || current !== null || !interactive) return;
      if (e.currentTarget.matches(':focus-visible')) go(0, 'keyboard');
    },
    onBlur: (e: FocusEvent<HTMLDivElement>) => {
      if (e.currentTarget.contains(e.relatedTarget as Node | null)) return;
      setActive(null);
    },
    onClick: () => {
      const d = current ? byCode.get(current) : undefined;
      if (d && onCountrySelect) onCountrySelect(d);
    },
  };

  return (
    <ChartFrame
      ref={ref}
      {...frame}
      status={resolvedStatus}
      height={Math.round((FALLBACK_WIDTH * H) / W)}
      table={table}
      labels={labels}
      footer={
        <>
          {showLegend && !bubble && resolvedStatus === 'ready' && rows.length > 0 ? (
            <HeatmapLegend
              scale={scale}
              color={color}
              format={fmt}
              labels={{ ...labels, noValue: il.noValue }}
            />
          ) : null}
          {footer}
        </>
      }
      data-chart="world-map"
    >
      <div
        ref={measureRef}
        className="mn-chart__plot mn-world-map"
        data-variant={variant}
        data-selectable={onCountrySelect ? '' : undefined}
        role="group"
        aria-roledescription={il.chart}
        aria-label={summary ?? generated}
        aria-describedby={hintId}
        style={colorStyle(color)}
        {...plotProps}
      >
        <svg
          className="mn-world-map__svg"
          viewBox={`0 0 ${W} ${H}`}
          preserveAspectRatio="xMidYMid meet"
          aria-hidden="true"
          focusable="false"
        >
          <g className="mn-world-map__countries">{shapes}</g>
          {!bubble && current && byCode.has(current) && WORLD_MAP_SHAPES[current] ? (
            <path className="mn-world-map__outline" d={WORLD_MAP_SHAPES[current]} />
          ) : null}
          <g className="mn-world-map__marks">{marks}</g>
        </svg>
        {anchorCode && anchor && (
          <ChartTooltip
            x={anchor[0] * px}
            y={anchor[1] * px}
            width={W * px}
            title={nameOf(anchorCode)}
            rows={[
              {
                key: 'value',
                label: words.value,
                value: activeDatum ? fmt(activeDatum.value) : il.noValue,
                color,
              },
              ...(activeDatum?.details ?? []).map((x) => ({
                key: `d:${x.label}`,
                label: x.label,
                value: String(x.value),
                color: 'neutral' as const,
              })),
            ]}
          />
        )}
        <span id={hintId} className="mn-visually-hidden">
          {il.keyboardHint}
        </span>
        <ChartLiveRegion>
          {current !== null && source === 'keyboard' ? describe(current) : ''}
        </ChartLiveRegion>
      </div>
      {(extras.length > 0 || unplacedNote) && (
        <div className="mn-world-map__below">
          {extras.length > 0 && (
            <ul className="mn-world-map__extras" aria-label={words.extras}>
              {extras.map((x) => (
                <li key={x.key} className="mn-world-map__extra">
                  <span className="mn-world-map__extra-label">{x.label}</span>
                  <span className="mn-world-map__extra-value">{fmt(x.value)}</span>
                </li>
              ))}
            </ul>
          )}
          {unplacedNote && <p className="mn-world-map__unplaced">{unplacedNote}</p>}
        </div>
      )}
    </ChartFrame>
  );
});
