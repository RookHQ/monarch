'use client';

import { forwardRef, useMemo, useState, type HTMLAttributes, type ReactNode } from 'react';
import { cx } from '../../../internal/cx.js';
import { ErrorGlyph, SuccessGlyph, WarningGlyph, CopyGlyph } from '../../../internal/icons.js';
import { Badge } from '../../data-display/badge/index.js';
import { TabsFilter } from '../../navigation/tabs/index.js';
import { DataTable, type DataTableColumn } from '../data-table/index.js';

/** What will happen to a row: imported, imported with warnings, skipped for errors, or a duplicate. */
export type PreviewRowStatus = 'ready' | 'warning' | 'error' | 'duplicate';

/** A problem with one row, optionally tied to a field. */
export interface PreviewRowIssue {
  /** Field label, e.g. "Email". */
  field?: string | undefined;
  message: string;
}

/** One row of the file as the import will treat it. */
export interface PreviewRow {
  /** 1-based data row number in the file (the header row is not counted). */
  row: number;
  /** Cell values by column id. Missing values show a dash. */
  values: Readonly<Record<string, ReactNode>>;
  /** Overrides the status worked out from `errors` and `warnings`, e.g. `duplicate`. */
  status?: PreviewRowStatus | undefined;
  errors?: readonly (PreviewRowIssue | string)[] | undefined;
  warnings?: readonly (PreviewRowIssue | string)[] | undefined;
  /** Extra content in the issues cell, e.g. a link to the existing record a duplicate matches. */
  note?: ReactNode;
}

export interface PreviewRowsColumn {
  id: string;
  /** Header text. */
  label: string;
  numeric?: boolean;
}

export type PreviewRowsFilter = 'all' | PreviewRowStatus;

export interface PreviewRowsMessages {
  /** Name of the filter tabs. */
  filter: string;
  all: string;
  /** Status names, used for the badge and the filter tabs. */
  status: Record<PreviewRowStatus, string>;
  /** Header of the row-number column. */
  row: string;
  /** Header of the status column. */
  result: string;
  /** Header of the issues column. */
  issues: string;
  /** Row label for the card layout and assistive text: "Row 4". */
  rowLabel: (row: number) => string;
  emptyTitle: string;
  emptyDescription: string;
  /** Empty state when a filter matches nothing. */
  filteredTitle: string;
}

export const defaultPreviewRowsMessages: PreviewRowsMessages = {
  filter: 'Show rows',
  all: 'All',
  status: { ready: 'Ready', warning: 'Warning', error: 'Error', duplicate: 'Duplicate' },
  row: 'Row',
  result: 'Result',
  issues: 'Issues',
  rowLabel: (row) => `Row ${row}`,
  emptyTitle: 'No rows to preview',
  emptyDescription: 'The file has a header row but no data rows.',
  filteredTitle: 'No rows with this result',
};

/** The status of a row: `status` if set, otherwise `error` when it has errors, `warning` when it has warnings, else `ready`. */
export function previewRowStatus(row: PreviewRow): PreviewRowStatus {
  if (row.status) return row.status;
  if (row.errors?.length) return 'error';
  if (row.warnings?.length) return 'warning';
  return 'ready';
}

/** Counts rows by status. */
export function countPreviewRows(rows: readonly PreviewRow[]): Record<PreviewRowStatus, number> {
  const counts: Record<PreviewRowStatus, number> = { ready: 0, warning: 0, error: 0, duplicate: 0 };
  for (const r of rows) counts[previewRowStatus(r)] += 1;
  return counts;
}

const TONE: Record<PreviewRowStatus, 'success' | 'warning' | 'danger' | 'info'> = {
  ready: 'success',
  warning: 'warning',
  error: 'danger',
  duplicate: 'info',
};
const ICON: Record<PreviewRowStatus, ReactNode> = {
  ready: <SuccessGlyph />,
  warning: <WarningGlyph />,
  error: <ErrorGlyph />,
  duplicate: <CopyGlyph />,
};
const ORDER: PreviewRowStatus[] = ['error', 'warning', 'duplicate', 'ready'];

const issueText = (i: PreviewRowIssue | string) => (typeof i === 'string' ? { message: i } : i);

export interface PreviewRowsProps extends Omit<HTMLAttributes<HTMLDivElement>, 'children'> {
  rows: readonly PreviewRow[];
  /** The data columns to show, in order, between Result and Issues. */
  columns: readonly PreviewRowsColumn[];
  /** Accessible name of the table, e.g. "Preview of contacts.csv". */
  caption: string;
  /** Show filter tabs with counts when some rows are not ready. Default true. */
  filterable?: boolean;
  /** Current filter (controlled). */
  filter?: PreviewRowsFilter;
  defaultFilter?: PreviewRowsFilter;
  onFilterChange?: ((filter: PreviewRowsFilter) => void) | undefined;
  /** Rows per page. Default 50; `0` shows every row. */
  pageSize?: number;
  /** `auto` (default) switches to cards on narrow screens; `table` or `cards` force one. */
  layout?: 'auto' | 'table' | 'cards';
  messages?: Partial<Omit<PreviewRowsMessages, 'status'>> & {
    status?: Partial<PreviewRowsMessages['status']>;
  };
}

/**
 * The rows of an import file as they will be imported: each row's number,
 * result (ready, warning, error, duplicate), chosen values and problems,
 * with filter tabs and counts. A table on wide screens and cards on phones.
 */
export const PreviewRows = forwardRef<HTMLDivElement, PreviewRowsProps>(function PreviewRows(
  {
    rows,
    columns,
    caption,
    filterable = true,
    filter: filterProp,
    defaultFilter = 'all',
    onFilterChange,
    pageSize = 50,
    layout = 'auto',
    messages,
    className,
    ...rest
  },
  ref,
) {
  const m: PreviewRowsMessages = {
    ...defaultPreviewRowsMessages,
    ...messages,
    status: { ...defaultPreviewRowsMessages.status, ...messages?.status },
  };
  const [innerFilter, setInnerFilter] = useState<PreviewRowsFilter>(defaultFilter);
  const filter = filterProp ?? innerFilter;
  const setFilter = (next: PreviewRowsFilter) => {
    if (filterProp === undefined) setInnerFilter(next);
    onFilterChange?.(next);
  };
  const counts = useMemo(() => countPreviewRows(rows), [rows]);
  const shown = useMemo(
    () => (filter === 'all' ? rows : rows.filter((r) => previewRowStatus(r) === filter)),
    [rows, filter],
  );
  const hasProblems = counts.error + counts.warning + counts.duplicate > 0;

  const tableColumns: DataTableColumn<PreviewRow>[] = [
    {
      id: '__row',
      header: m.row,
      numeric: true,
      width: 72,
      rowHeader: true,
      accessor: (r) => r.row,
      cell: (r) => <span className="mn-preview-rows__row">{r.row}</span>,
    },
    {
      id: '__status',
      header: m.result,
      cell: (r) => {
        const s = previewRowStatus(r);
        return (
          <Badge tone={TONE[s]} className="mn-preview-rows__status" data-status={s}>
            <span className="mn-preview-rows__status-icon" aria-hidden="true">
              {ICON[s]}
            </span>
            {m.status[s]}
          </Badge>
        );
      },
    },
    ...columns.map<DataTableColumn<PreviewRow>>((c) => ({
      id: c.id,
      header: c.label,
      ...(c.numeric ? { numeric: true } : {}),
      accessor: (r) => r.values[c.id],
      cell: (r) => {
        const v = r.values[c.id];
        return v === null || v === undefined || v === '' ? (
          <span className="mn-preview-rows__empty">—</span>
        ) : (
          <span className="mn-preview-rows__value">{v}</span>
        );
      },
    })),
    {
      id: '__issues',
      header: m.issues,
      cell: (r) => {
        const errors = (r.errors ?? []).map(issueText);
        const warnings = (r.warnings ?? []).map(issueText);
        if (!errors.length && !warnings.length && !r.note) return null;
        return (
          <div className="mn-preview-rows__issues">
            {errors.length || warnings.length ? (
              <ul className="mn-preview-rows__issue-list">
                {errors.map((e, i) => (
                  <li key={`e${i}`} className="mn-preview-rows__issue" data-kind="error">
                    <span className="mn-visually-hidden">{m.status.error}: </span>
                    {e.field ? <span className="mn-preview-rows__field">{e.field}: </span> : null}
                    {e.message}
                  </li>
                ))}
                {warnings.map((w, i) => (
                  <li key={`w${i}`} className="mn-preview-rows__issue" data-kind="warning">
                    <span className="mn-visually-hidden">{m.status.warning}: </span>
                    {w.field ? <span className="mn-preview-rows__field">{w.field}: </span> : null}
                    {w.message}
                  </li>
                ))}
              </ul>
            ) : null}
            {r.note ? <div className="mn-preview-rows__note">{r.note}</div> : null}
          </div>
        );
      },
    },
  ];

  const options = [
    { value: 'all', label: m.all, count: rows.length },
    ...ORDER.filter((s) => counts[s] > 0 || filter === s).map((s) => ({
      value: s,
      label: m.status[s],
      count: counts[s],
      countTone:
        s === 'error'
          ? ('danger' as const)
          : s === 'warning'
            ? ('warning' as const)
            : ('neutral' as const),
    })),
  ];

  const table = (
    <DataTable
      caption={caption}
      columns={tableColumns}
      rows={shown}
      getRowId={(r) => String(r.row)}
      getRowLabel={(r) => m.rowLabel(r.row)}
      density="compact"
      layout={layout}
      {...(pageSize > 0 && shown.length > pageSize ? { pageSize } : {})}
      filtered={filter !== 'all'}
      onClearFilters={() => setFilter('all')}
      messages={{
        emptyTitle: m.emptyTitle,
        emptyDescription: m.emptyDescription,
        filteredTitle: m.filteredTitle,
      }}
    />
  );

  return (
    <div ref={ref} {...rest} className={cx('mn-preview-rows', className)} data-filter={filter}>
      {filterable && (hasProblems || filter !== 'all') ? (
        <TabsFilter
          aria-label={m.filter}
          options={options}
          value={filter}
          onValueChange={(v) => setFilter(v as PreviewRowsFilter)}
          scrollable
          panelClassName="mn-preview-rows__panel"
        >
          {table}
        </TabsFilter>
      ) : (
        table
      )}
    </div>
  );
});
