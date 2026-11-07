'use client';
import {
  forwardRef,
  useId,
  type ForwardedRef,
  type HTMLAttributes,
  type ReactElement,
  type ReactNode,
  type Ref,
} from 'react';
import { cx } from '../../../internal/cx.js';
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '../../data-grid/data-table/index.js';
import { EmptyState } from '../../feedback/empty-state/index.js';
import { ErrorState, type ErrorKind } from '../../feedback/error-state/index.js';
import { Skeleton } from '../../feedback/skeleton/index.js';
import type { DateInput } from '../relative-time/index.js';
import {
  DownloadReportButton,
  type DownloadReportButtonProps,
} from '../download-report-button/index.js';

/** A primitive cell value from a report API. */
export type ReportCellValue = string | number | boolean | null | undefined;

/** One column of the built-in report table. */
export interface ReportColumn<Row = Record<string, ReportCellValue>> {
  /** The row field this column shows. */
  key: string;
  /** Column heading. */
  label: string;
  /** Right-aligns the column with tabular figures. Default: numbers are detected per cell. */
  numeric?: boolean | undefined;
  /** Draws the cell yourself, e.g. a formatted date, Money or a Badge. */
  render?: ((row: Row) => ReactNode) | undefined;
}

/** The words ReportViewer shows. Override them to translate. */
export interface ReportViewerMessages {
  /** The generated-at line. Receives the formatted date and time. */
  generated: (when: string) => string;
  /** Shown under a table that stops before the last row. */
  truncated: string;
  /** Shown when the report has no rows. */
  empty: string;
  /** Spoken name of the loading placeholder. */
  loading: string;
  /** Accessible name of the filters group. */
  filters: string;
  /** Default error title. */
  error: string;
  /** Title when the person may not open the report (`errorKind="forbidden"`). */
  forbidden: string;
  /** Title when the report does not exist (`errorKind="not-found"`). */
  notFound: string;
  /** A true cell. */
  yes: string;
  /** A false cell. */
  no: string;
  /** Spoken for an empty cell, shown as a dash. */
  missing: string;
}

export const REPORT_VIEWER_MESSAGES: ReportViewerMessages = {
  generated: (when) => `Generated ${when}`,
  truncated: 'Only the first rows are shown. Download the report for every row.',
  empty: 'No rows match these filters.',
  loading: 'Loading report',
  filters: 'Report filters',
  error: 'This report could not be run.',
  forbidden: 'You cannot open this report',
  notFound: 'Report not found',
  yes: 'Yes',
  no: 'No',
  missing: 'None',
};

export interface ReportViewerProps<Row = Record<string, ReportCellValue>> extends Omit<
  HTMLAttributes<HTMLDivElement>,
  'title'
> {
  /** The report's name. The page heading. */
  title: ReactNode;
  /** Level of the title. Default `1` (the report is the page). */
  headingLevel?: 1 | 2 | 3 | undefined;
  /** What the report shows, under the title. */
  description?: ReactNode;
  /** A link back to the report list, above the title. */
  back?: ReactNode;
  /** Header controls before the download button, e.g. a "Group by" SegmentedControl or Print. */
  actions?: ReactNode;
  /** Shows a DownloadReportButton in the header with these props (formats, onDownload, progress). */
  download?: DownloadReportButtonProps | undefined;
  /** Filter controls (PeriodSelector, Selects, a FilterBar). The app filters and re-runs the report. */
  filters?: ReactNode;
  /** When the report was run. Shown as "Generated 7 Nov 2026, 10:00". */
  generatedAt?: DateInput | undefined;
  /** The scope in words, e.g. "All brands · 1 Oct – 31 Oct 2026". */
  scope?: ReactNode;
  /** Tags beside the scope, e.g. a "Contains pay figures" Badge. */
  badges?: ReactNode;
  /** Headline figures above the body, usually a KPIRow. */
  summary?: ReactNode;
  /** A chart above the table. */
  chart?: ReactNode;
  /** Columns of the built-in table. With `rows`, the body is a simple, accessible table. */
  columns?: readonly ReportColumn<Row>[] | undefined;
  /** Rows of the built-in table. */
  rows?: readonly Row[] | undefined;
  /** Stable key per row. Default: the index. */
  getRowKey?: ((row: Row, index: number) => string) | undefined;
  /** The table stops before the last row. Shows `messages.truncated`. */
  truncated?: boolean | undefined;
  /** Footnotes under the body, e.g. how a figure is calculated. */
  notes?: readonly ReactNode[] | undefined;
  /** First run: shows placeholders and sets `aria-busy`. */
  loading?: boolean | undefined;
  /** Re-running with new filters while the previous result stays visible (dimmed, `aria-busy`). */
  refreshing?: boolean | undefined;
  /** Replaces the body with this failure. `true` uses the default text for `errorKind`. */
  error?: ReactNode;
  /** What went wrong: `forbidden` and `not-found` offer no retry. Default `error`. */
  errorKind?: ErrorKind | undefined;
  /** With `error`, shows Try again calling this. */
  onRetry?: (() => void) | undefined;
  /** Replaces the default empty state when there are no rows. */
  empty?: ReactNode;
  /** BCP 47 locale for `generatedAt`. */
  locale?: string | undefined;
  /** IANA time zone for `generatedAt`. */
  timeZone?: string | undefined;
  /** Strings, for localisation. */
  messages?: Partial<ReportViewerMessages> | undefined;
  /** A custom body (a DataTable, a chart, a pivot) instead of the built-in table. */
  children?: ReactNode;
}

function toDate(input: DateInput): Date {
  return input instanceof Date ? input : new Date(input);
}

function ReportViewerInner<Row = Record<string, ReportCellValue>>(
  {
    title,
    headingLevel = 1,
    description,
    back,
    actions,
    download,
    filters,
    generatedAt,
    scope,
    badges,
    summary,
    chart,
    columns,
    rows,
    getRowKey,
    truncated = false,
    notes,
    loading = false,
    refreshing = false,
    error,
    errorKind = 'error',
    onRetry,
    empty,
    locale,
    timeZone,
    messages: messagesProp,
    className,
    children,
    ...rest
  }: ReportViewerProps<Row>,
  ref: ForwardedRef<HTMLDivElement>,
) {
  const messages = { ...REPORT_VIEWER_MESSAGES, ...messagesProp };
  const titleId = useId();
  const Heading = `h${headingLevel}` as const;
  const hasError = !loading && error !== undefined && error !== null && error !== false;

  const cellText = (value: unknown): ReactNode => {
    if (value === null || value === undefined || value === '') {
      return (
        <>
          <span aria-hidden>—</span>
          <span className="mn-visually-hidden">{messages.missing}</span>
        </>
      );
    }
    if (typeof value === 'boolean') return value ? messages.yes : messages.no;
    if (typeof value === 'number') return new Intl.NumberFormat(locale).format(value);
    return value as ReactNode;
  };

  const generated =
    generatedAt !== undefined
      ? new Intl.DateTimeFormat(locale, {
          dateStyle: 'medium',
          timeStyle: 'short',
          ...(timeZone !== undefined && { timeZone }),
        }).format(toDate(generatedAt))
      : undefined;
  const generatedIso = generatedAt !== undefined ? toDate(generatedAt).toISOString() : undefined;

  let body: ReactNode;
  if (loading) {
    body = (
      <div className="mn-report-viewer__skeleton" role="img" aria-label={messages.loading}>
        <Skeleton shape="rect" height="12rem" />
        {[0, 1, 2, 3].map((i) => (
          <Skeleton key={i} shape="text" width={i % 2 ? '80%' : '95%'} />
        ))}
      </div>
    );
  } else if (hasError) {
    const defaultTitle =
      errorKind === 'forbidden'
        ? messages.forbidden
        : errorKind === 'not-found'
          ? messages.notFound
          : messages.error;
    body = (
      <ErrorState
        kind={errorKind}
        headingLevel={`h${Math.min(headingLevel + 1, 6)}` as 'h2' | 'h3' | 'h4'}
        title={defaultTitle}
        {...(error !== true && { description: error })}
        {...(onRetry !== undefined && { onRetry })}
      />
    );
  } else if (children !== undefined && children !== null && children !== false) {
    body = children;
  } else if (columns && rows) {
    body =
      rows.length === 0 ? (
        (empty ?? (
          <EmptyState size="compact" kind="filtered" headingLevel="p" title={messages.empty} />
        ))
      ) : (
        <Table className="mn-report-viewer__table" density="compact" aria-labelledby={titleId}>
          <TableHeader>
            <TableRow>
              {columns.map((column) => (
                <TableHead
                  key={column.key}
                  scope="col"
                  className={column.numeric ? 'mn-report-viewer__numeric' : undefined}
                >
                  {column.label}
                </TableHead>
              ))}
            </TableRow>
          </TableHeader>
          <TableBody>
            {rows.map((row, index) => (
              <TableRow key={getRowKey ? getRowKey(row, index) : index}>
                {columns.map((column) => {
                  const value = (row as Record<string, unknown>)[column.key];
                  const numeric = column.numeric ?? typeof value === 'number';
                  return (
                    <TableCell key={column.key} numeric={numeric}>
                      {column.render ? column.render(row) : cellText(value)}
                    </TableCell>
                  );
                })}
              </TableRow>
            ))}
          </TableBody>
        </Table>
      );
  } else {
    body = empty ?? null;
  }

  const showMeta = !loading && (generated || scope || badges);
  const showResult = !loading && !hasError;

  return (
    <div
      ref={ref}
      aria-busy={loading || refreshing || undefined}
      {...rest}
      className={cx('mn-report-viewer', className)}
      data-state={loading ? 'loading' : hasError ? 'error' : refreshing ? 'refreshing' : undefined}
    >
      <header className="mn-report-viewer__header">
        {back ? <div className="mn-report-viewer__back">{back}</div> : null}
        <div className="mn-report-viewer__title-row">
          <div className="mn-report-viewer__heading">
            <Heading id={titleId} className="mn-report-viewer__title">
              {title}
            </Heading>
            {description ? <p className="mn-report-viewer__description">{description}</p> : null}
          </div>
          {actions || download ? (
            <div className="mn-report-viewer__actions">
              {actions}
              {download ? (
                <DownloadReportButton
                  {...download}
                  disabled={download.disabled ?? (loading || hasError)}
                />
              ) : null}
            </div>
          ) : null}
        </div>
      </header>
      {filters ? (
        <div className="mn-report-viewer__filters" role="group" aria-label={messages.filters}>
          {filters}
        </div>
      ) : null}
      {showMeta ? (
        <p className="mn-report-viewer__meta">
          {scope ? <span className="mn-report-viewer__scope">{scope}</span> : null}
          {generated ? (
            <time className="mn-report-viewer__generated" dateTime={generatedIso}>
              {messages.generated(generated)}
            </time>
          ) : null}
          {badges ? <span className="mn-report-viewer__badges">{badges}</span> : null}
        </p>
      ) : null}
      {showResult && summary ? <div className="mn-report-viewer__summary">{summary}</div> : null}
      {showResult && chart ? <div className="mn-report-viewer__chart">{chart}</div> : null}
      <div className="mn-report-viewer__body">{body}</div>
      {showResult && truncated ? (
        <p className="mn-report-viewer__note">{messages.truncated}</p>
      ) : null}
      {showResult && notes && notes.length > 0 ? (
        <div className="mn-report-viewer__notes">
          {notes.map((note, index) => (
            <p key={index} className="mn-report-viewer__note">
              {note}
            </p>
          ))}
        </div>
      ) : null}
    </div>
  );
}

/**
 * One report on its own page: title, scope and generated time, filters,
 * headline figures, a chart and a table, footnotes and a download button,
 * with loading, refreshing, empty and error states. Data comes from the app.
 */
export const ReportViewer = forwardRef(ReportViewerInner) as <
  Row = Record<string, ReportCellValue>,
>(
  props: ReportViewerProps<Row> & { ref?: Ref<HTMLDivElement> | undefined },
) => ReactElement;
