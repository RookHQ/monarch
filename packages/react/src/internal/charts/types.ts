import type { ChartColor } from './palette.js';

/** One plotted measure. `key` names the field read from each datum. */
export interface ChartSeries {
  /** Field read from each datum; also the series' stable id (legend, hidden list). */
  key: string;
  /** Human name, shown in the legend, tooltip, table header and summary. */
  label: string;
  /**
   * Palette slot. Default: the series' position (1, 2, …). Pin it when a
   * filter can remove series, so colours follow the entity and survivors are
   * never repainted.
   */
  color?: ChartColor | undefined;
  /**
   * Dashed stroke (lines) or hatched fill (bars) as a second encoding besides
   * colour, for example a comparison period or a forecast.
   */
  dashed?: boolean | undefined;
}

/** A row of chart data: the x (or category) field plus one numeric field per series. */
export type ChartDatum = Record<string, unknown>;

/** A legend entry. */
export interface ChartLegendItem {
  key: string;
  label: string;
  color: ChartColor;
  dashed?: boolean | undefined;
  /** Hidden series are listed with a struck-through label and an unpressed toggle. */
  hidden?: boolean | undefined;
}

/** Marker drawn beside a legend label: a line for line charts, a square for bars. */
export type ChartLegendMarker = 'line' | 'square';

/** Column of the accessible data table. */
export interface ChartTableColumn {
  key: string;
  label: string;
  /** `end` for numbers (default for every column but the first). */
  align?: 'start' | 'end' | undefined;
}

/** The data-table fallback: every value the chart shows, as text. */
export interface ChartTableData {
  /** Caption read before the table; defaults to the chart title. */
  caption?: string | undefined;
  /** The first column is the row header (`<th scope="row">`). */
  columns: ChartTableColumn[];
  rows: Record<string, string | number>[];
}
