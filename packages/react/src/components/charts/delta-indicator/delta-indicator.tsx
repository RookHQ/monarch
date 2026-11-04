import { forwardRef, type HTMLAttributes, type ReactNode, type SVGProps } from 'react';
import { cx } from '../../../internal/cx.js';
import { formatNumber } from '../../../internal/format-number.js';
import { formatMoney } from '../../billing/money/index.js';
import { VisuallyHidden } from '../../layout/visually-hidden/index.js';

/** Which way the value moved. */
export type DeltaDirection = 'up' | 'down' | 'flat';
/** Which direction is an improvement: `down` for costs, latency or churn; `neither` when there is no better way. */
export type DeltaGoodDirection = 'up' | 'down' | 'neither';
/** The resulting colour role: `positive` (better), `negative` (worse) or `neutral`. */
export type DeltaTone = 'positive' | 'negative' | 'neutral';
/** How the change is written: `percent` (a ratio, 0.12 is 12%), `number` or `currency`. */
export type DeltaFormat = 'percent' | 'number' | 'currency';

/** The words DeltaIndicator shows and speaks. Override them to translate. */
export interface DeltaLabels {
  /** Spoken before an increase. Default "up". */
  up: string;
  /** Spoken before a decrease. Default "down". */
  down: string;
  /** Shown when the change is zero or inside `flatThreshold`. Default "No change". */
  flat: string;
  /** Spoken after a change in the good direction. Default "better". */
  better: string;
  /** Spoken after a change in the bad direction. Default "worse". */
  worse: string;
  /** Shown when there is nothing to compare with. Default "No earlier data". */
  noData: string;
}

const DEFAULT_LABELS: DeltaLabels = {
  up: 'up',
  down: 'down',
  flat: 'No change',
  better: 'better',
  worse: 'worse',
  noData: 'No earlier data',
};

export interface FormatDeltaOptions {
  /**
   * The change itself. With `format="percent"` it is a ratio (0.12 is 12%).
   * `null` means there is nothing to compare with. Takes precedence over
   * `current` and `previous`.
   */
  value?: number | null;
  /** This period's value; with `previous`, the change is computed. */
  current?: number | null;
  /**
   * The previous period's value. With `format="percent"` the change is
   * `(current − previous) / |previous|`, and a `previous` of 0 has no baseline,
   * so it shows `labels.noData`. Otherwise the change is `current − previous`.
   */
  previous?: number | null;
  /** Default `percent`. */
  format?: DeltaFormat;
  /** ISO 4217 code for `format="currency"`, e.g. `USD`. Required in that format. */
  currency?: string;
  /** Unit written after a `number` change, e.g. `pt` or `ms`. */
  unit?: string;
  /** BCP 47 locale. Default: the runtime locale. */
  locale?: string;
  /** Extra `Intl.NumberFormat` options, e.g. `{ maximumFractionDigits: 0 }`. */
  formatOptions?: Intl.NumberFormatOptions;
  /** Writes the size of the change yourself (no sign); replaces `format`, `unit` and `formatOptions`. */
  formatValue?: (magnitude: number) => string;
  /** Which direction is an improvement. Default `up`. */
  goodDirection?: DeltaGoodDirection;
  /** Changes whose size is at or below this count as flat. Default 0. */
  flatThreshold?: number;
  /** Words to show and speak; merged over the English defaults. */
  labels?: Partial<DeltaLabels>;
  /** Plain-text comparison included in the returned `label`, e.g. "vs last month". */
  comparison?: string;
}

/** What `formatDelta` works out; DeltaIndicator renders exactly this. */
export interface DeltaResult {
  /** The change, or `null` when there is nothing to compare with. */
  value: number | null;
  /** `null` when there is nothing to compare with. */
  direction: DeltaDirection | null;
  tone: DeltaTone;
  /** The visible text: "+12%", "−3 ms", "No change" or "No earlier data". */
  text: string;
  /** The size of the change without a sign, e.g. "12%". Empty when flat or empty. */
  magnitude: string;
  /** The full spoken sentence, e.g. "up 12% vs last month, better". */
  label: string;
}

const MINUS = '−';

function resolveValue(
  value: number | null | undefined,
  current: number | null | undefined,
  previous: number | null | undefined,
  format: DeltaFormat,
): number | null {
  if (value !== undefined) return value !== null && Number.isFinite(value) ? value : null;
  if (current == null || previous == null) return null;
  if (format === 'percent')
    return previous === 0 ? null : (current - previous) / Math.abs(previous);
  return current - previous;
}

/**
 * Works out the direction, tone, visible text and spoken label of a change.
 * Use it for chart summaries and `aria-label`s so they match DeltaIndicator.
 */
export function formatDelta(options: FormatDeltaOptions): DeltaResult {
  const {
    value: rawValue,
    current,
    previous,
    format = 'percent',
    currency,
    unit,
    locale,
    formatOptions,
    formatValue,
    goodDirection = 'up',
    flatThreshold = 0,
    comparison,
  } = options;
  const labels = { ...DEFAULT_LABELS, ...options.labels };
  const value = resolveValue(rawValue, current, previous, format);
  if (value === null) {
    return {
      value: null,
      direction: null,
      tone: 'neutral',
      text: labels.noData,
      magnitude: '',
      label: labels.noData,
    };
  }

  const write = (n: number): string => {
    if (formatValue) return formatValue(n);
    if (format === 'currency') {
      return formatMoney(n, currency ?? '', {
        ...(locale !== undefined && { locale }),
        ...(formatOptions?.minimumFractionDigits !== undefined && {
          minimumFractionDigits: formatOptions.minimumFractionDigits,
        }),
        ...(formatOptions?.maximumFractionDigits !== undefined && {
          maximumFractionDigits: formatOptions.maximumFractionDigits,
        }),
      });
    }
    const text = formatNumber(n, {
      kind: format,
      ...(locale !== undefined && { locale }),
      ...(formatOptions !== undefined && { options: formatOptions }),
    });
    return unit && format === 'number' ? `${text}\u00a0${unit}` : text;
  };

  const size = Math.abs(value);
  // A change that rounds to zero when written ("0%") is flat, not "+0%".
  const flat = size <= flatThreshold || write(size) === write(0);
  const tail = comparison ? ` ${comparison}` : '';
  if (flat) {
    return {
      value,
      direction: 'flat',
      tone: 'neutral',
      text: labels.flat,
      magnitude: '',
      label: `${labels.flat}${tail}`,
    };
  }
  const direction: DeltaDirection = value > 0 ? 'up' : 'down';
  const tone: DeltaTone =
    goodDirection === 'neither' ? 'neutral' : direction === goodDirection ? 'positive' : 'negative';
  const magnitude = write(size);
  const quality = tone === 'positive' ? labels.better : tone === 'negative' ? labels.worse : '';
  return {
    value,
    direction,
    tone,
    text: `${direction === 'up' ? '+' : MINUS}${magnitude}`,
    magnitude,
    label: `${labels[direction]} ${magnitude}${tail}${quality ? `, ${quality}` : ''}`,
  };
}

const glyph = {
  width: '1em',
  height: '1em',
  viewBox: '0 0 16 16',
  fill: 'none',
  stroke: 'currentColor',
  strokeWidth: 2,
  strokeLinecap: 'round',
  strokeLinejoin: 'round',
  'aria-hidden': true,
  focusable: false,
} as const;

function DirectionGlyph({
  direction,
  ...props
}: { direction: DeltaDirection } & SVGProps<SVGSVGElement>) {
  return (
    <svg {...glyph} {...props}>
      {direction === 'up' ? (
        <path d="M4.5 11.5l7-7M5.5 4.5h6v6" />
      ) : direction === 'down' ? (
        <path d="M4.5 4.5l7 7M11.5 5.5v6h-6" />
      ) : (
        <path d="M3.5 8h9" />
      )}
    </svg>
  );
}

export interface DeltaIndicatorProps
  extends
    Omit<HTMLAttributes<HTMLSpanElement>, 'children'>,
    Omit<FormatDeltaOptions, 'comparison'> {
  /** The comparison period after the change, e.g. "vs last month". Shown muted and spoken. */
  comparison?: ReactNode;
  /** `plain` (default) is inline text; `pill` puts the change in a tinted chip. */
  appearance?: 'plain' | 'pill';
  /** `sm` (default) for card footers, `md` next to body text. */
  size?: 'sm' | 'md';
  /** Hides the direction arrow. The sign and the spoken direction remain. */
  hideIcon?: boolean;
}

/**
 * A change between two periods: direction arrow, signed value and an optional
 * comparison. Colour follows `goodDirection`, so a fall in costs is green;
 * the arrow, sign and spoken "up 12% vs last month, better" carry the meaning
 * without colour.
 */
export const DeltaIndicator = forwardRef<HTMLSpanElement, DeltaIndicatorProps>(
  function DeltaIndicator(
    {
      value,
      current,
      previous,
      format,
      currency,
      unit,
      locale,
      formatOptions,
      formatValue,
      goodDirection,
      flatThreshold,
      labels,
      comparison,
      appearance = 'plain',
      size = 'sm',
      hideIcon = false,
      className,
      ...rest
    },
    ref,
  ) {
    const delta = formatDelta({
      ...(value !== undefined && { value }),
      ...(current !== undefined && { current }),
      ...(previous !== undefined && { previous }),
      ...(format !== undefined && { format }),
      ...(currency !== undefined && { currency }),
      ...(unit !== undefined && { unit }),
      ...(locale !== undefined && { locale }),
      ...(formatOptions !== undefined && { formatOptions }),
      ...(formatValue !== undefined && { formatValue }),
      ...(goodDirection !== undefined && { goodDirection }),
      ...(flatThreshold !== undefined && { flatThreshold }),
      ...(labels !== undefined && { labels }),
    });
    const common = {
      ref,
      ...rest,
      className: cx('mn-delta-indicator', className),
      'data-tone': delta.tone,
      'data-appearance': appearance,
      'data-size': size,
    };

    if (delta.direction === null) {
      return (
        <span {...common} data-empty="">
          <span className="mn-delta-indicator__value">{delta.text}</span>
        </span>
      );
    }

    const comparisonNode =
      comparison !== undefined && comparison !== null && comparison !== false ? (
        <span className="mn-delta-indicator__comparison"> {comparison}</span>
      ) : null;

    if (delta.direction === 'flat') {
      return (
        <span {...common} data-direction="flat">
          <span className="mn-delta-indicator__change">
            {hideIcon ? null : (
              <DirectionGlyph direction="flat" className="mn-delta-indicator__icon" />
            )}
            <span className="mn-delta-indicator__value">{delta.text}</span>
          </span>
          {comparisonNode}
        </span>
      );
    }

    const resolvedLabels = { ...DEFAULT_LABELS, ...labels };
    const quality =
      delta.tone === 'positive'
        ? resolvedLabels.better
        : delta.tone === 'negative'
          ? resolvedLabels.worse
          : '';
    return (
      <span {...common} data-direction={delta.direction}>
        <span className="mn-delta-indicator__change" aria-hidden>
          {hideIcon ? null : (
            <DirectionGlyph direction={delta.direction} className="mn-delta-indicator__icon" />
          )}
          <span className="mn-delta-indicator__value">{delta.text}</span>
        </span>
        <VisuallyHidden>{`${resolvedLabels[delta.direction]} ${delta.magnitude}`}</VisuallyHidden>
        {comparisonNode}
        {quality ? <VisuallyHidden>{`, ${quality}`}</VisuallyHidden> : null}
      </span>
    );
  },
);
