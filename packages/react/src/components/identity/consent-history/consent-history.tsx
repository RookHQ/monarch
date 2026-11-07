'use client';

import {
  forwardRef,
  useEffect,
  useId,
  useMemo,
  useRef,
  useState,
  type HTMLAttributes,
  type ReactNode,
} from 'react';
import { cx } from '../../../internal/cx.js';
import { isThenable } from '../../../internal/dialog-helpers.js';
import { CheckGlyph, CloseGlyph } from '../../../internal/icons.js';
import { Button } from '../../actions/button/index.js';
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '../../data-grid/data-table/index.js';
import { RelativeTime, type DateInput } from '../../data-display/relative-time/index.js';
import { EmptyState } from '../../feedback/empty-state/index.js';
import type {
  ConsentCategory,
  ConsentMethod,
  ConsentRecord,
  ConsentRegime,
} from '../consent-manager/index.js';

/** Every visible string of ConsentHistory. Pass any subset to translate or reword. */
export interface ConsentHistoryMessages {
  /** How each method of deciding reads in the list. */
  methods: Record<ConsentMethod, string>;
  /** How each regime reads in the list. */
  regimes: Record<ConsentRegime, string>;
  /** Column headings (and the labels of each value when rows are stacked). */
  columnDate: string;
  columnMethod: string;
  columnPolicyVersion: string;
  columnRegime: string;
  columnCategories: string;
  /** Text after a category that was allowed. */
  allowed: string;
  /** Text after a category that was refused. */
  denied: string;
  /** Extra line when a Global Privacy Control signal was present but the method was not `gpc`. */
  gpcPresent: string;
  /** Title of the empty state. */
  emptyTitle: string;
  /** Description of the empty state. */
  emptyDescription: string;
  /** Shown when `onExport` rejects. */
  exportFailed: string;
}

const DEFAULT_MESSAGES: ConsentHistoryMessages = {
  methods: {
    'accept-all': 'Accepted all',
    'reject-all': 'Rejected all',
    custom: 'Custom choice',
    gpc: 'Browser privacy signal',
    acknowledged: 'Acknowledged',
    withdrawn: 'Withdrew consent',
  },
  regimes: {
    'opt-in': 'Asked before use (opt-in)',
    'opt-out': 'Could refuse (opt-out)',
    notice: 'Notice only',
  },
  columnDate: 'Date',
  columnMethod: 'Choice',
  columnPolicyVersion: 'Policy version',
  columnRegime: 'Rules applied',
  columnCategories: 'Categories',
  allowed: 'Allowed',
  denied: 'Not allowed',
  gpcPresent: 'Browser privacy signal present',
  emptyTitle: 'No consent choices recorded',
  emptyDescription:
    'Choices about cookies and similar storage will appear here once they are made.',
  exportFailed: 'The download did not start. Try again.',
};

const DEFAULT_DATE_FORMAT: Intl.DateTimeFormatOptions = { dateStyle: 'medium', timeStyle: 'short' };

type Heading = 'h2' | 'h3' | 'h4' | 'h5' | 'h6';
const NEXT_LEVEL: Record<Heading, 'h3' | 'h4' | 'h5' | 'h6' | 'p'> = {
  h2: 'h3',
  h3: 'h4',
  h4: 'h5',
  h5: 'h6',
  h6: 'p',
};

export interface ConsentHistoryProps extends Omit<HTMLAttributes<HTMLDivElement>, 'title'> {
  /** The recorded decisions, in any order. They are shown newest first. */
  records: readonly ConsentRecord[];
  /**
   * The product's categories, so ids are shown with their labels and in this
   * order. Ids a record has that are not listed are shown as the raw id, after
   * the listed ones.
   */
  categories?: readonly ConsentCategory[];
  /** Heading above the list. Default "Consent history". */
  title?: ReactNode;
  /** Heading element. Default `h2`. */
  headingLevel?: Heading;
  /** A sentence under the heading. */
  description?: ReactNode;
  /** BCP 47 locale for dates, e.g. `en-GB`. Default: the runtime locale. Pass it for server rendering. */
  locale?: string;
  /** IANA time zone for dates, e.g. `Europe/Berlin`. Default: the viewer's zone. Pass it for server rendering. */
  timeZone?: string;
  /** `Intl.DateTimeFormat` options for the date column. Default `{ dateStyle: 'medium', timeStyle: 'short' }`. */
  dateFormat?: Intl.DateTimeFormatOptions;
  /** Also show how long ago each decision was ("3 days ago") under the date. Default `false`. */
  showRelative?: boolean;
  /** Reference time for `showRelative`. Pass the same value on server and client. */
  now?: DateInput;
  /**
   * Shows a download button in the header. The product builds the file (CSV,
   * JSON) itself. Return a promise to show the button as busy until it
   * settles; a rejection shows `messages.exportFailed`.
   */
  onExport?: () => unknown;
  /** Label of the download button. Name the format, e.g. "Download as CSV". Default "Download history". */
  exportLabel?: string;
  /** Replaces the default empty state. */
  empty?: ReactNode;
  messages?: Partial<ConsentHistoryMessages>;
}

function time(value: string): number {
  const t = Date.parse(value);
  return Number.isNaN(t) ? Number.NEGATIVE_INFINITY : t;
}

/** The category ids of a record, in the order of `categories`, then any others. */
function categoryRows(
  record: ConsentRecord,
  categories: readonly ConsentCategory[] | undefined,
): { id: string; label: string; allowed: boolean }[] {
  const known = categories ?? [];
  const listed = known
    .filter((c) => c.id in record.categories)
    .map((c) => ({ id: c.id, label: c.label, allowed: record.categories[c.id] === true }));
  const seen = new Set(listed.map((c) => c.id));
  const rest = Object.keys(record.categories)
    .filter((id) => !seen.has(id))
    .map((id) => ({ id, label: id, allowed: record.categories[id] === true }));
  return [...listed, ...rest];
}

/**
 * A person's recorded consent decisions, newest first: when, how (accepted
 * all, custom choice, browser privacy signal…), which policy version and
 * rules applied, and every category allowed or not, each with an icon and
 * text. A table on wide containers; each decision becomes a stacked card
 * below 40rem (640px). For a privacy page or an admin view.
 */
export const ConsentHistory = forwardRef<HTMLDivElement, ConsentHistoryProps>(
  function ConsentHistory(
    {
      records,
      categories,
      title = 'Consent history',
      headingLevel: HeadingTag = 'h2',
      description,
      locale,
      timeZone,
      dateFormat = DEFAULT_DATE_FORMAT,
      showRelative = false,
      now,
      onExport,
      exportLabel = 'Download history',
      empty,
      messages: messagesProp,
      className,
      ...rest
    },
    ref,
  ) {
    const messages: ConsentHistoryMessages = {
      ...DEFAULT_MESSAGES,
      ...messagesProp,
      methods: { ...DEFAULT_MESSAGES.methods, ...messagesProp?.methods },
      regimes: { ...DEFAULT_MESSAGES.regimes, ...messagesProp?.regimes },
    };
    const headingId = `mn-consent-history-${useId()}`;
    const [exporting, setExporting] = useState(false);
    const [exportError, setExportError] = useState(false);
    const mounted = useRef(true);
    useEffect(() => {
      mounted.current = true;
      return () => {
        mounted.current = false;
      };
    }, []);

    const sorted = useMemo(() => [...records].sort((a, b) => time(b.at) - time(a.at)), [records]);
    const formatter = useMemo(
      () => new Intl.DateTimeFormat(locale, { ...dateFormat, ...(timeZone ? { timeZone } : {}) }),
      [locale, timeZone, dateFormat],
    );

    const runExport = () => {
      if (!onExport || exporting) return;
      setExportError(false);
      let result: unknown;
      try {
        result = onExport();
      } catch {
        setExportError(true);
        return;
      }
      if (!isThenable(result)) return;
      setExporting(true);
      Promise.resolve(result).then(
        () => {
          if (mounted.current) setExporting(false);
        },
        () => {
          if (!mounted.current) return;
          setExporting(false);
          setExportError(true);
        },
      );
    };

    const hasRecords = sorted.length > 0;
    const label = (text: string) => (
      // Visible only when rows are stacked; the column header names the cell for assistive technology.
      <span className="mn-consent-history__cell-label" aria-hidden="true">
        {text}
      </span>
    );

    return (
      <div
        ref={ref}
        {...rest}
        className={cx('mn-consent-history', className)}
        data-empty={hasRecords ? undefined : ''}
      >
        <div className="mn-consent-history__header">
          <div className="mn-consent-history__heading">
            <HeadingTag id={headingId} className="mn-consent-history__title">
              {title}
            </HeadingTag>
            {description ? <p className="mn-consent-history__description">{description}</p> : null}
          </div>
          {onExport && hasRecords ? (
            <Button
              size="sm"
              loading={exporting}
              onClick={runExport}
              className="mn-consent-history__export"
            >
              {exportLabel}
            </Button>
          ) : null}
        </div>
        {exportError ? (
          <p role="alert" className="mn-consent-history__error">
            {messages.exportFailed}
          </p>
        ) : null}
        {hasRecords ? (
          <Table aria-labelledby={headingId} role="table" className="mn-consent-history__table">
            <TableHeader role="rowgroup" className="mn-consent-history__thead">
              <TableRow role="row">
                <TableHead role="columnheader">{messages.columnDate}</TableHead>
                <TableHead role="columnheader">{messages.columnMethod}</TableHead>
                <TableHead role="columnheader">{messages.columnPolicyVersion}</TableHead>
                <TableHead role="columnheader">{messages.columnRegime}</TableHead>
                <TableHead role="columnheader">{messages.columnCategories}</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody role="rowgroup">
              {sorted.map((record) => {
                const when = new Date(record.at);
                const valid = !Number.isNaN(when.getTime());
                return (
                  <TableRow
                    key={record.id}
                    role="row"
                    className="mn-consent-history__row"
                    data-method={record.method}
                  >
                    <TableCell role="cell" className="mn-consent-history__date">
                      {label(messages.columnDate)}
                      <span className="mn-consent-history__value">
                        <time dateTime={record.at} suppressHydrationWarning>
                          {valid ? formatter.format(when) : record.at}
                        </time>
                        {showRelative && valid ? (
                          <RelativeTime
                            date={when}
                            {...(now !== undefined ? { now } : {})}
                            {...(locale ? { locale } : {})}
                            tooltip={false}
                            className="mn-consent-history__relative"
                          />
                        ) : null}
                      </span>
                    </TableCell>
                    <TableCell role="cell">
                      {label(messages.columnMethod)}
                      <span className="mn-consent-history__value">
                        <span className="mn-consent-history__method">
                          {messages.methods[record.method] ?? record.method}
                        </span>
                        {record.gpc && record.method !== 'gpc' ? (
                          <span className="mn-consent-history__note">{messages.gpcPresent}</span>
                        ) : null}
                      </span>
                    </TableCell>
                    <TableCell role="cell">
                      {label(messages.columnPolicyVersion)}
                      <span className="mn-consent-history__value mn-consent-history__version">
                        {record.policyVersion}
                      </span>
                    </TableCell>
                    <TableCell role="cell">
                      {label(messages.columnRegime)}
                      <span className="mn-consent-history__value">
                        {messages.regimes[record.regime] ?? record.regime}
                      </span>
                    </TableCell>
                    <TableCell role="cell">
                      {label(messages.columnCategories)}
                      <ul className="mn-consent-history__value mn-consent-history__categories">
                        {categoryRows(record, categories).map((c) => (
                          <li
                            key={c.id}
                            className="mn-consent-history__category"
                            data-allowed={c.allowed ? 'true' : 'false'}
                          >
                            <span className="mn-consent-history__category-icon" aria-hidden="true">
                              {c.allowed ? <CheckGlyph /> : <CloseGlyph />}
                            </span>
                            <span className="mn-consent-history__category-label">{c.label}</span>
                            <span className="mn-visually-hidden">: </span>
                            <span className="mn-consent-history__category-sep" aria-hidden="true">
                              ·
                            </span>
                            <span className="mn-consent-history__category-state">
                              {c.allowed ? messages.allowed : messages.denied}
                            </span>
                          </li>
                        ))}
                      </ul>
                    </TableCell>
                  </TableRow>
                );
              })}
            </TableBody>
          </Table>
        ) : (
          (empty ?? (
            <EmptyState
              size="compact"
              headingLevel={NEXT_LEVEL[HeadingTag]}
              title={messages.emptyTitle}
              description={messages.emptyDescription}
              className="mn-consent-history__empty"
            />
          ))
        )}
      </div>
    );
  },
);
