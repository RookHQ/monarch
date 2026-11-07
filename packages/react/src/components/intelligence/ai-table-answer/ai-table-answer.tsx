'use client';

import {
  forwardRef,
  memo,
  useEffect,
  useId,
  useMemo,
  useRef,
  useState,
  type HTMLAttributes,
  type ReactNode,
} from 'react';
import { cx } from '../../../internal/cx.js';
import { BotGlyph, ChevronDownGlyph } from '../../../internal/icons.js';
import { Button } from '../../actions/button/index.js';
import { CopyButton } from '../../actions/copy-button/index.js';

/** How a column's values are compared, aligned and formatted. */
export type AiTableColumnType = 'text' | 'number' | 'date';

/** A table row: column key to value. Values are untrusted model output. */
export type AiTableRow = Readonly<Record<string, unknown>>;

export interface AiTableColumn {
  /** The key of the value in each row. */
  key: string;
  /** Header text. Default: the key. */
  label?: string | undefined;
  /** Default: inferred from the values (see `inferColumnType`). */
  type?: AiTableColumnType | undefined;
  /** Default: `end` for numbers, `start` otherwise. */
  align?: 'start' | 'end' | undefined;
  /** Custom cell content. Sorting and CSV still use the raw value. */
  render?: ((value: unknown, row: AiTableRow) => ReactNode) | undefined;
}

export interface AiTableAnswerMessages {
  /** Visible provenance label in the header. */
  aiTable: string;
  /** Row count, e.g. "12 rows". */
  rowCount: (count: number) => string;
  /** Label of the copy button. */
  copyCsv: string;
  /** Label of the copy button after copying. */
  copied: string;
  /** Label of the open-as-artifact button. */
  openAsArtifact: string;
  /** Announced politely after a sort. */
  sorted: (column: string, direction: 'ascending' | 'descending') => string;
  /** Read for an empty cell. */
  empty: string;
  /** Shown when there are no rows and nothing is streaming. */
  noRows: string;
  /** Read in the placeholder row while streaming. */
  loadingRows: string;
  /** Announced politely when streaming finishes. */
  complete: string;
}

export const AI_TABLE_ANSWER_MESSAGES: AiTableAnswerMessages = {
  aiTable: 'AI table',
  rowCount: (count) => `${count} ${count === 1 ? 'row' : 'rows'}`,
  copyCsv: 'Copy as CSV',
  copied: 'Copied',
  openAsArtifact: 'Open as artifact',
  sorted: (column, direction) => `Sorted by ${column}, ${direction}.`,
  empty: 'Empty',
  noRows: 'No rows.',
  loadingRows: 'More rows are being generated',
  complete: 'Table complete.',
};

export interface AiTableAnswerProps extends Omit<HTMLAttributes<HTMLDivElement>, 'title'> {
  /** The rows, usually parsed from the model's output. */
  rows: readonly AiTableRow[];
  /** Column order, labels and types. Default: every key in the rows, types inferred. */
  columns?: readonly AiTableColumn[] | undefined;
  /** Rows are still arriving: shows a placeholder row and marks the table busy. */
  streaming?: boolean | undefined;
  /** Visible title, also the table caption. */
  title?: ReactNode | undefined;
  /** Allow sorting by clicking headers. Default true. */
  sortable?: boolean | undefined;
  /** Show "Copy as CSV". Default true. */
  copyable?: boolean | undefined;
  /** Shows "Open as artifact", e.g. to open the table in AiArtifactCanvas. */
  onOpenAsArtifact?: (() => void) | undefined;
  /** Locale for numbers and dates. Default: the runtime's. */
  locale?: string | undefined;
  /** Strings, for localisation. */
  messages?: Partial<AiTableAnswerMessages> | undefined;
}

const NUMBER_RE = /^[-+−]?(\d{1,3}(,\d{3})+|\d+)?(\.\d+)?%?$/;
const DATE_RE = /^\d{4}-\d{2}-\d{2}([T ]\d{2}:\d{2}(:\d{2}(\.\d+)?)?(Z|[+-]\d{2}:?\d{2})?)?$/;

function isBlank(v: unknown): boolean {
  return v === undefined || v === null || (typeof v === 'string' && v.trim() === '');
}

function toNumber(v: unknown): number | null {
  if (typeof v === 'number') return Number.isFinite(v) ? v : null;
  if (typeof v !== 'string') return null;
  const s = v.trim();
  if (!s || !NUMBER_RE.test(s) || !/\d/.test(s)) return null;
  const n = Number(s.replace(/,/g, '').replace('−', '-').replace(/%$/, ''));
  return Number.isFinite(n) ? n : null;
}

function toTime(v: unknown): number | null {
  if (v instanceof Date) return Number.isNaN(v.getTime()) ? null : v.getTime();
  if (typeof v !== 'string' || !DATE_RE.test(v.trim())) return null;
  const t = Date.parse(v.trim());
  return Number.isNaN(t) ? null : t;
}

/**
 * Infers a column type from its values, ignoring blanks: `number` when every
 * value is a number or a numeric string ("1,200", "-3.5", "12%"), `date` when
 * every value is a `Date` or an ISO date string, otherwise `text`.
 */
export function inferColumnType(values: readonly unknown[]): AiTableColumnType {
  const present = values.filter((v) => !isBlank(v));
  if (present.length === 0) return 'text';
  if (present.every((v) => toNumber(v) !== null)) return 'number';
  if (present.every((v) => toTime(v) !== null)) return 'date';
  return 'text';
}

/** Columns for every key in the rows, in first-seen order, with inferred types. */
export function inferColumns(rows: readonly AiTableRow[]): AiTableColumn[] {
  const keys: string[] = [];
  const seen = new Set<string>();
  for (const row of rows)
    for (const key of Object.keys(row))
      if (!seen.has(key)) {
        seen.add(key);
        keys.push(key);
      }
  return keys.map((key) => ({ key, type: inferColumnType(rows.map((r) => r[key])) }));
}

function cellText(v: unknown): string {
  if (isBlank(v)) return '';
  if (typeof v === 'string') return v;
  if (typeof v === 'number' || typeof v === 'boolean' || typeof v === 'bigint') return String(v);
  if (v instanceof Date) return Number.isNaN(v.getTime()) ? '' : v.toISOString();
  try {
    return JSON.stringify(v) ?? '';
  } catch {
    return String(v);
  }
}

function csvField(v: unknown): string {
  let s = cellText(v);
  // Neutralise spreadsheet formulas in untrusted text (CSV injection), but
  // keep real numbers such as "-3" intact.
  if (typeof v === 'string' && /^[=+\-@\t\r]/.test(s) && toNumber(s) === null) s = `'${s}`;
  return /[",\r\n]|^\s|\s$/.test(s) ? `"${s.replace(/"/g, '""')}"` : s;
}

/**
 * Serialises rows to RFC 4180 CSV: a header row of labels (or keys), CRLF line
 * endings, fields quoted when they contain commas, quotes, line breaks or edge
 * spaces, and quotes doubled. Text that a spreadsheet would run as a formula
 * (`=`, `+`, `-`, `@`) is prefixed with `'`. Default columns: `inferColumns(rows)`.
 */
export function toCsv(
  rows: readonly AiTableRow[],
  columns: readonly Pick<AiTableColumn, 'key' | 'label'>[] = inferColumns(rows),
): string {
  const lines = [columns.map((c) => csvField(c.label ?? c.key))];
  for (const row of rows) lines.push(columns.map((c) => csvField(row[c.key])));
  return lines.map((l) => l.join(',')).join('\r\n');
}

function compare(a: unknown, b: unknown, type: AiTableColumnType, collator: Intl.Collator): number {
  if (type === 'number') return (toNumber(a) ?? 0) - (toNumber(b) ?? 0);
  if (type === 'date') return (toTime(a) ?? 0) - (toTime(b) ?? 0);
  return collator.compare(cellText(a), cellText(b));
}

interface ResolvedColumn extends AiTableColumn {
  label: string;
  type: AiTableColumnType;
  align: 'start' | 'end';
}

interface RowProps {
  row: AiTableRow;
  columns: readonly ResolvedColumn[];
  locale: string | undefined;
  emptyLabel: string;
}

function formatCell(v: unknown, col: ResolvedColumn, locale: string | undefined): ReactNode {
  if (typeof v === 'number' && col.type === 'number')
    return new Intl.NumberFormat(locale).format(v);
  if (v instanceof Date && col.type === 'date' && !Number.isNaN(v.getTime()))
    return new Intl.DateTimeFormat(locale, { dateStyle: 'medium', timeZone: 'UTC' }).format(v);
  return cellText(v);
}

// Memoised per row: rows that have arrived are not re-rendered as more stream in.
const Row = memo(function Row({ row, columns, locale, emptyLabel }: RowProps) {
  return (
    <tr className="mn-ai-table-answer__row">
      {columns.map((col) => {
        const v = row[col.key];
        return (
          <td
            key={col.key}
            className="mn-ai-table-answer__cell"
            data-align={col.align}
            data-type={col.type}
          >
            {isBlank(v) ? (
              <>
                <span aria-hidden="true">—</span>
                <span className="mn-visually-hidden">{emptyLabel}</span>
              </>
            ) : col.render ? (
              col.render(v, row)
            ) : (
              formatCell(v, col, locale)
            )}
          </td>
        );
      })}
    </tr>
  );
});

/**
 * A table the model produced, shown as a real table: column types inferred
 * (numbers align to the end and sort numerically, ISO dates sort by time),
 * sortable headers, "Copy as CSV" and an optional "Open as artifact". Rows
 * can stream in. Scrolls sideways inside its own focusable region on narrow
 * screens, so the page never does.
 */
export const AiTableAnswer = forwardRef<HTMLDivElement, AiTableAnswerProps>(function AiTableAnswer(
  {
    rows,
    columns: columnsProp,
    streaming = false,
    title,
    sortable = true,
    copyable = true,
    onOpenAsArtifact,
    locale,
    messages: messagesProp,
    className,
    ...rest
  },
  ref,
) {
  const m = { ...AI_TABLE_ANSWER_MESSAGES, ...messagesProp };
  const labelId = useId();
  const titleId = useId();
  const [sort, setSort] = useState<{ key: string; dir: 'ascending' | 'descending' } | null>(null);
  const [announcement, setAnnouncement] = useState('');
  const wasStreaming = useRef(streaming);

  const columns = useMemo<ResolvedColumn[]>(() => {
    const base = columnsProp ?? inferColumns(rows);
    return base.map((c) => {
      const type = c.type ?? inferColumnType(rows.map((r) => r[c.key]));
      return {
        ...c,
        label: c.label ?? c.key,
        type,
        align: c.align ?? (type === 'number' ? 'end' : 'start'),
      };
    });
  }, [columnsProp, rows]);

  const sortedRows = useMemo(() => {
    const indexed = rows.map((row, i) => ({ row, i }));
    if (!sort) return indexed;
    const col = columns.find((c) => c.key === sort.key);
    if (!col) return indexed;
    const collator = new Intl.Collator(locale, { numeric: true, sensitivity: 'base' });
    const sign = sort.dir === 'ascending' ? 1 : -1;
    return indexed.sort((a, b) => {
      const av = a.row[col.key];
      const bv = b.row[col.key];
      // Blanks always go last, whatever the direction.
      if (isBlank(av) || isBlank(bv))
        return isBlank(av) === isBlank(bv) ? a.i - b.i : isBlank(av) ? 1 : -1;
      return sign * compare(av, bv, col.type, collator) || a.i - b.i;
    });
  }, [rows, columns, sort, locale]);

  useEffect(() => {
    if (wasStreaming.current && !streaming) setAnnouncement(m.complete);
    wasStreaming.current = streaming;
  }, [streaming, m.complete]);

  const toggleSort = (col: ResolvedColumn) => {
    const dir: 'ascending' | 'descending' =
      sort?.key === col.key && sort.dir === 'ascending' ? 'descending' : 'ascending';
    setSort({ key: col.key, dir });
    setAnnouncement(m.sorted(col.label, dir));
  };

  const empty = rows.length === 0 && !streaming;

  return (
    <div
      ref={ref}
      {...rest}
      className={cx('mn-ai-table-answer', className)}
      data-streaming={streaming || undefined}
    >
      <div className="mn-ai-table-answer__header">
        <div className="mn-ai-table-answer__titles">
          <span id={labelId} className="mn-ai-table-answer__eyebrow">
            <BotGlyph aria-hidden="true" />
            {m.aiTable}
          </span>
          {title ? (
            <span id={titleId} className="mn-ai-table-answer__title">
              {title}
            </span>
          ) : null}
          <span className="mn-ai-table-answer__count">{m.rowCount(rows.length)}</span>
        </div>
        <div className="mn-ai-table-answer__actions">
          {copyable ? (
            <CopyButton
              size="sm"
              variant="ghost"
              label={m.copyCsv}
              copiedLabel={m.copied}
              disabled={rows.length === 0}
              value={() =>
                toCsv(
                  sortedRows.map((x) => x.row),
                  columns,
                )
              }
            />
          ) : null}
          {onOpenAsArtifact ? (
            <Button size="sm" variant="secondary" onClick={onOpenAsArtifact} disabled={streaming}>
              {m.openAsArtifact}
            </Button>
          ) : null}
        </div>
      </div>
      {/* Focusable so keyboard users can scroll wide tables. */}
      <div
        className="mn-ai-table-answer__scroll"
        role="region"
        aria-labelledby={title ? `${labelId} ${titleId}` : labelId}
        // eslint-disable-next-line jsx-a11y/no-noninteractive-tabindex
        tabIndex={0}
      >
        <table className="mn-ai-table-answer__table" aria-busy={streaming || undefined}>
          <caption className="mn-visually-hidden">
            {m.aiTable}
            {title ? <>: {title}</> : null}
          </caption>
          <thead>
            <tr>
              {columns.map((col) => {
                const active = sort?.key === col.key;
                return (
                  <th
                    key={col.key}
                    scope="col"
                    className="mn-ai-table-answer__head"
                    data-align={col.align}
                    aria-sort={sortable ? (active ? sort.dir : 'none') : undefined}
                  >
                    {sortable ? (
                      <button
                        type="button"
                        className="mn-ai-table-answer__sort"
                        data-sorted={active ? sort.dir : undefined}
                        onClick={() => toggleSort(col)}
                      >
                        <span>{col.label}</span>
                        <ChevronDownGlyph
                          aria-hidden="true"
                          className="mn-ai-table-answer__sort-icon"
                        />
                      </button>
                    ) : (
                      col.label
                    )}
                  </th>
                );
              })}
            </tr>
          </thead>
          <tbody>
            {sortedRows.map(({ row, i }) => (
              <Row key={i} row={row} columns={columns} locale={locale} emptyLabel={m.empty} />
            ))}
            {streaming ? (
              <tr className="mn-ai-table-answer__pending">
                <td colSpan={Math.max(1, columns.length)}>
                  <span className="mn-ai-table-answer__pending-bar" aria-hidden="true" />
                  <span className="mn-visually-hidden">{m.loadingRows}</span>
                </td>
              </tr>
            ) : null}
            {empty ? (
              <tr>
                <td colSpan={Math.max(1, columns.length)} className="mn-ai-table-answer__empty">
                  {m.noRows}
                </td>
              </tr>
            ) : null}
          </tbody>
        </table>
      </div>
      <span
        className="mn-visually-hidden mn-ai-table-answer__live"
        role="status"
        aria-live="polite"
      >
        {announcement}
      </span>
    </div>
  );
});
