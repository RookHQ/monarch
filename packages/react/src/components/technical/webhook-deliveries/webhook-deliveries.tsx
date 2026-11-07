'use client';

import {
  forwardRef,
  useCallback,
  useEffect,
  useMemo,
  useRef,
  useState,
  type HTMLAttributes,
  type ReactNode,
} from 'react';
import { cx } from '../../../internal/cx.js';
import { isThenable } from '../../../internal/dialog-helpers.js';
import {
  ClockGlyph,
  DotGlyph,
  ErrorGlyph,
  HelpGlyph,
  RetryGlyph,
  SuccessGlyph,
} from '../../../internal/icons.js';
import { Button } from '../../actions/button/index.js';
import { CopyButton } from '../../actions/copy-button/index.js';
import {
  Table,
  TableBody,
  TableCaption,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '../../data-grid/data-table/index.js';
import { LoadMore } from '../../data-grid/load-more/index.js';
import { RelativeTime, type DateInput } from '../../data-display/relative-time/index.js';
import { StatusBadge, type StatusRegistry } from '../../data-display/status-badge/index.js';
import { EmptyState } from '../../feedback/empty-state/index.js';
import { InlineErrorRetry } from '../../feedback/inline-error-retry/index.js';
import { Skeleton } from '../../feedback/skeleton/index.js';
import { NativeSelect } from '../../forms/form-field/index.js';
import { Input } from '../../forms/text-field/index.js';

/** The delivery states Monarch labels out of the box. Products may use their own keys with `statuses`. */
export type WebhookDeliveryStatus =
  'pending' | 'sending' | 'succeeded' | 'retrying' | 'failed' | 'gone';

/** One attempt (or series of attempts) to deliver one event to one endpoint. */
export interface WebhookDelivery {
  /** Stable id of the delivery. */
  id: string;
  /** A key of `statuses` (built in: pending, sending, succeeded, retrying, failed, gone). */
  status: WebhookDeliveryStatus | (string & {});
  /** The event name, e.g. `invoice.paid`. */
  eventType: string;
  /** The event's id, shown with a copy button. */
  eventId?: string | undefined;
  /** Where it was sent, when one list covers several endpoints or channels. */
  destination?: ReactNode;
  /** HTTP status the endpoint answered with. `null` when there was no answer. */
  responseCode?: number | null | undefined;
  /** What went wrong, in plain words ("No response within 10 seconds"). */
  error?: ReactNode;
  /** How long the last attempt took, in milliseconds. */
  durationMs?: number | null | undefined;
  /** Attempts made so far. */
  attempts: number;
  /** When it was last attempted or delivered (or created, if never attempted). */
  at: DateInput;
  /** When the next automatic retry happens, for pending or retrying deliveries. */
  nextAttemptAt?: DateInput | null | undefined;
}

/** The filter values. An empty string means "any". */
export interface WebhookDeliveryFilters {
  status: string;
  eventType: string;
  /** Text matched against the event id (exactly) and the event type (contains). */
  query: string;
}

/** Every visible string, with English defaults. */
export interface WebhookDeliveriesMessages {
  caption: string;
  status: string;
  event: string;
  destination: string;
  response: string;
  attempts: string;
  when: string;
  actions: string;
  anyStatus: string;
  anyEventType: string;
  eventTypeFilter: string;
  search: string;
  searchPlaceholder: string;
  clearFilters: string;
  redeliver: string;
  /** Accessible name of a row's redeliver button. */
  redeliverLabel: (delivery: WebhookDelivery) => string;
  redeliverQueued: string;
  redeliverFailed: string;
  copyEventId: string;
  viewDetails: (delivery: WebhookDelivery) => string;
  notAttempted: string;
  httpStatus: (code: number) => string;
  duration: (ms: number) => string;
  nextAttempt: (time: ReactNode) => ReactNode;
  emptyTitle: string;
  emptyDescription: string;
  filteredTitle: string;
  filteredDescription: string;
  loading: string;
  loadError: string;
  noun: { one: string; other: string };
  endOfList: string;
  /** One part of the counts line, e.g. "12 delivered". */
  count: (count: number, label: string) => string;
}

const DEFAULT_MESSAGES: WebhookDeliveriesMessages = {
  caption: 'Webhook deliveries',
  status: 'Status',
  event: 'Event',
  destination: 'Destination',
  response: 'Response',
  attempts: 'Attempts',
  when: 'When',
  actions: 'Actions',
  anyStatus: 'Any status',
  anyEventType: 'Any event type',
  eventTypeFilter: 'Event type',
  search: 'Find by event ID',
  searchPlaceholder: 'Event ID',
  clearFilters: 'Clear filters',
  redeliver: 'Redeliver',
  redeliverLabel: (d) => `Redeliver ${d.eventType}${d.eventId ? ` ${d.eventId}` : ''}`,
  redeliverQueued: 'Redelivery queued.',
  redeliverFailed: 'Could not redeliver. Try again.',
  copyEventId: 'Copy event ID',
  viewDetails: (d) => `View delivery of ${d.eventType}${d.eventId ? ` ${d.eventId}` : ''}`,
  notAttempted: 'Not attempted yet',
  httpStatus: (code) => `HTTP ${code}`,
  duration: (ms) => `${Math.round(ms)} ms`,
  nextAttempt: (time) => <>Next attempt {time}</>,
  emptyTitle: 'No deliveries yet',
  emptyDescription: 'Deliveries appear here when an event this endpoint subscribes to happens.',
  filteredTitle: 'No deliveries match',
  filteredDescription: 'Change or clear the filters. Event IDs must match exactly.',
  loading: 'Loading deliveries',
  loadError: 'Deliveries did not load.',
  noun: { one: 'delivery', other: 'deliveries' },
  endOfList: 'End of the delivery log',
  count: (count, label) => `${count.toLocaleString()} ${label.toLowerCase()}`,
};

/** The built-in delivery statuses. Override labels or add keys with the `statuses` prop. */
export const webhookDeliveryStatuses = {
  pending: { label: 'Pending', tone: 'neutral', icon: <ClockGlyph /> },
  sending: { label: 'Sending', tone: 'info', icon: <DotGlyph /> },
  succeeded: { label: 'Delivered', tone: 'success', icon: <SuccessGlyph /> },
  retrying: { label: 'Retrying', tone: 'warning', icon: <RetryGlyph /> },
  failed: { label: 'Failed', tone: 'danger', icon: <ErrorGlyph /> },
  gone: { label: 'Gave up', tone: 'danger', icon: <HelpGlyph /> },
} as const satisfies StatusRegistry<WebhookDeliveryStatus>;

const EMPTY_FILTERS: WebhookDeliveryFilters = { status: '', eventType: '', query: '' };

/** Applies filters the way WebhookDeliveries does when it filters on its own. */
export function filterWebhookDeliveries(
  deliveries: readonly WebhookDelivery[],
  filters: WebhookDeliveryFilters,
): WebhookDelivery[] {
  const q = filters.query.trim().toLowerCase();
  return deliveries.filter(
    (d) =>
      (!filters.status || d.status === filters.status) &&
      (!filters.eventType || d.eventType === filters.eventType) &&
      (!q || d.eventId?.toLowerCase() === q || d.eventType.toLowerCase().includes(q)),
  );
}

export interface WebhookDeliveriesProps extends Omit<HTMLAttributes<HTMLDivElement>, 'onSelect'> {
  /** The deliveries to show, newest first. */
  deliveries: readonly WebhookDelivery[];
  /** Status labels, tones and icons, merged over `webhookDeliveryStatuses`. */
  statuses?: StatusRegistry | undefined;
  /**
   * Redeliver one delivery. Return a promise and the row's button shows a
   * spinner until it settles; the outcome is announced. Omit to hide the action.
   */
  onRedeliver?: ((delivery: WebhookDelivery) => unknown) | undefined;
  /** Which rows can be redelivered. Default: every row that is not pending or sending. */
  canRedeliver?: ((delivery: WebhookDelivery) => boolean) | undefined;
  /** Open a delivery's details (for example in a LogDrawer). Makes the event name a button. */
  onSelect?: ((delivery: WebhookDelivery) => void) | undefined;
  /** Extra actions per row, after Redeliver (for example a Discard button). */
  renderActions?: ((delivery: WebhookDelivery) => ReactNode) | undefined;
  /** Show the filter row. Default true. */
  filterable?: boolean;
  /** Controlled filter values. */
  filters?: WebhookDeliveryFilters | undefined;
  /** Uncontrolled initial filter values. */
  defaultFilters?: Partial<WebhookDeliveryFilters> | undefined;
  /** Called when a filter changes. */
  onFiltersChange?: ((filters: WebhookDeliveryFilters) => void) | undefined;
  /**
   * The server filters (send the filters with the next request): the
   * component shows `deliveries` as given. Default false: it filters them itself.
   */
  manualFiltering?: boolean;
  /** Statuses offered in the status filter. Default: every key of the merged registry. */
  filterStatuses?: readonly string[] | undefined;
  /** Event types offered in the event-type filter. The filter is hidden when empty. */
  eventTypes?: readonly string[] | undefined;
  /** Show the Destination column (each delivery's `destination`). Default false. */
  showDestination?: boolean;
  /** Totals per status, shown above the table as "12 delivered · 3 retrying". */
  counts?: Partial<Record<string, number>> | undefined;
  /** First page is loading: shows skeleton rows. */
  loading?: boolean;
  /** The list failed to load: shows this message (or `messages.loadError` for `true`) with Try again. */
  error?: ReactNode;
  /** Called by Try again after a load error. */
  onRetry?: (() => unknown) | undefined;
  /** There is another page. Shows a Load more footer. */
  hasMore?: boolean;
  /** Loads the next page. */
  onLoadMore?: (() => void) | undefined;
  /** The next page is loading. */
  loadingMore?: boolean;
  /** Loading the next page failed. */
  loadMoreError?: boolean;
  /** Shown in the first-run empty state, e.g. a "Send a test event" button. */
  emptyAction?: ReactNode;
  /** Locale for numbers and times. Default the browser's. */
  locale?: string | undefined;
  /** "Now" for relative times (tests and server rendering). */
  now?: DateInput | undefined;
  /** Replaces any of the default English strings. */
  messages?: Partial<WebhookDeliveriesMessages> | undefined;
}

/**
 * The delivery log of a webhook endpoint (or any outbound integration): status,
 * event, response, attempts and timing per delivery, with filters, cursor
 * paging, copyable event IDs and a per-row Redeliver. Data and actions come
 * from the product; nothing is fetched here.
 */
export const WebhookDeliveries = forwardRef<HTMLDivElement, WebhookDeliveriesProps>(
  function WebhookDeliveries(
    {
      deliveries,
      statuses: statusesProp,
      onRedeliver,
      canRedeliver,
      onSelect,
      renderActions,
      filterable = true,
      filters: filtersProp,
      defaultFilters,
      onFiltersChange,
      manualFiltering = false,
      filterStatuses,
      eventTypes,
      showDestination = false,
      counts,
      loading = false,
      error,
      onRetry,
      hasMore = false,
      onLoadMore,
      loadingMore = false,
      loadMoreError = false,
      emptyAction,
      locale,
      now,
      messages: messagesProp,
      className,
      ...rest
    },
    ref,
  ) {
    const messages = useMemo(() => ({ ...DEFAULT_MESSAGES, ...messagesProp }), [messagesProp]);
    const statuses = useMemo<StatusRegistry>(
      () => ({ ...webhookDeliveryStatuses, ...statusesProp }),
      [statusesProp],
    );
    const [filtersState, setFiltersState] = useState<WebhookDeliveryFilters>({
      ...EMPTY_FILTERS,
      ...defaultFilters,
    });
    const filters = filtersProp ?? filtersState;
    const setFilters = useCallback(
      (patch: Partial<WebhookDeliveryFilters>) => {
        const next = { ...filters, ...patch };
        if (filtersProp === undefined) setFiltersState(next);
        onFiltersChange?.(next);
      },
      [filters, filtersProp, onFiltersChange],
    );
    const filtered = Boolean(filters.status || filters.eventType || filters.query.trim());
    const rows = useMemo(
      () => (manualFiltering ? [...deliveries] : filterWebhookDeliveries(deliveries, filters)),
      [deliveries, filters, manualFiltering],
    );

    const [pending, setPending] = useState<ReadonlySet<string>>(() => new Set());
    const [announcement, setAnnouncement] = useState('');
    const mounted = useRef(true);
    useEffect(() => {
      mounted.current = true;
      return () => {
        mounted.current = false;
      };
    }, []);
    const redeliver = (d: WebhookDelivery) => {
      if (!onRedeliver || pending.has(d.id)) return;
      let result: unknown;
      try {
        result = onRedeliver(d);
      } catch {
        setAnnouncement(messages.redeliverFailed);
        return;
      }
      if (!isThenable(result)) {
        setAnnouncement(messages.redeliverQueued);
        return;
      }
      setPending((s) => new Set(s).add(d.id));
      const settle = (text: string) => {
        if (!mounted.current) return;
        setPending((s) => {
          const next = new Set(s);
          next.delete(d.id);
          return next;
        });
        setAnnouncement(text);
      };
      result.then(
        () => settle(messages.redeliverQueued),
        () => settle(messages.redeliverFailed),
      );
    };
    const redeliverable = (d: WebhookDelivery) =>
      canRedeliver ? canRedeliver(d) : d.status !== 'pending' && d.status !== 'sending';
    const hasActions = Boolean(onRedeliver || renderActions);
    const statusKeys = filterStatuses ?? Object.keys(statuses);
    const countLine = counts
      ? Object.entries(counts)
          .filter((entry): entry is [string, number] => typeof entry[1] === 'number')
          .map(([key, n]) => messages.count(n, statuses[key]?.label ?? key))
          .join(' · ')
      : '';

    const label = (text: string) => (
      // Visible only when rows are stacked; the column header names the cell for assistive technology.
      <span className="mn-webhook-deliveries__cell-label" aria-hidden="true">
        {text}
      </span>
    );
    const time = (value: DateInput) => (
      <RelativeTime
        date={value}
        tooltip
        {...(locale ? { locale } : {})}
        {...(now !== undefined ? { now } : {})}
      />
    );

    const outcome = (d: WebhookDelivery) => {
      const parts: ReactNode[] = [];
      if (d.responseCode != null) parts.push(messages.httpStatus(d.responseCode));
      if (d.durationMs != null) parts.push(messages.duration(d.durationMs));
      const main =
        parts.length > 0
          ? parts.join(' · ')
          : d.error
            ? null
            : d.attempts === 0
              ? messages.notAttempted
              : '—';
      return (
        <span className="mn-webhook-deliveries__outcome">
          {main ? <span>{main}</span> : null}
          {d.error ? <span className="mn-webhook-deliveries__error">{d.error}</span> : null}
        </span>
      );
    };

    let body: ReactNode;
    if (loading) {
      body = (
        <div className="mn-webhook-deliveries__loading" aria-busy="true">
          <span className="mn-visually-hidden" role="status">
            {messages.loading}
          </span>
          {[0, 1, 2, 3].map((i) => (
            <Skeleton key={i} shape="text" height="2.5rem" />
          ))}
        </div>
      );
    } else if (error) {
      body = (
        <InlineErrorRetry onRetry={onRetry ?? (() => undefined)}>
          {error === true ? messages.loadError : error}
        </InlineErrorRetry>
      );
    } else if (rows.length === 0) {
      body = filtered ? (
        <EmptyState
          size="compact"
          kind="filtered"
          title={messages.filteredTitle}
          description={messages.filteredDescription}
          onClearFilters={() => setFilters(EMPTY_FILTERS)}
          clearFiltersLabel={messages.clearFilters}
        />
      ) : (
        <EmptyState
          size="compact"
          title={messages.emptyTitle}
          description={messages.emptyDescription}
          actions={emptyAction}
        />
      );
    } else {
      body = (
        <Table className="mn-webhook-deliveries__table" density="compact">
          <TableCaption className="mn-visually-hidden">{messages.caption}</TableCaption>
          <TableHeader className="mn-webhook-deliveries__thead">
            <TableRow>
              <TableHead>{messages.status}</TableHead>
              <TableHead>{messages.event}</TableHead>
              {showDestination ? <TableHead>{messages.destination}</TableHead> : null}
              <TableHead>{messages.response}</TableHead>
              <TableHead numeric>{messages.attempts}</TableHead>
              <TableHead>{messages.when}</TableHead>
              {hasActions ? (
                <TableHead>
                  <span className="mn-visually-hidden">{messages.actions}</span>
                </TableHead>
              ) : null}
            </TableRow>
          </TableHeader>
          <TableBody>
            {rows.map((d) => {
              const busy = pending.has(d.id);
              const next =
                d.status === 'pending' || d.status === 'retrying' ? d.nextAttemptAt : null;
              return (
                <TableRow key={d.id} className="mn-webhook-deliveries__row" data-status={d.status}>
                  <TableCell className="mn-webhook-deliveries__status">
                    {label(messages.status)}
                    <StatusBadge status={d.status} statuses={statuses} size="sm" />
                  </TableCell>
                  <TableCell className="mn-webhook-deliveries__event">
                    {label(messages.event)}
                    <span className="mn-webhook-deliveries__event-text">
                      {onSelect ? (
                        <button
                          type="button"
                          className="mn-webhook-deliveries__type mn-webhook-deliveries__select"
                          aria-label={messages.viewDetails(d)}
                          onClick={() => onSelect(d)}
                        >
                          {d.eventType}
                        </button>
                      ) : (
                        <code className="mn-webhook-deliveries__type">{d.eventType}</code>
                      )}
                      {d.eventId ? (
                        <span className="mn-webhook-deliveries__id">
                          <code>{d.eventId}</code>
                          <CopyButton
                            value={d.eventId}
                            label={messages.copyEventId}
                            iconOnly
                            size="xs"
                            variant="ghost"
                          />
                        </span>
                      ) : null}
                    </span>
                  </TableCell>
                  {showDestination ? (
                    <TableCell>
                      {label(messages.destination)}
                      <span>{d.destination ?? '—'}</span>
                    </TableCell>
                  ) : null}
                  <TableCell>
                    {label(messages.response)}
                    {outcome(d)}
                  </TableCell>
                  <TableCell numeric>
                    {label(messages.attempts)}
                    <span>{d.attempts.toLocaleString(locale)}</span>
                  </TableCell>
                  <TableCell className="mn-webhook-deliveries__when">
                    {label(messages.when)}
                    <span>{next != null ? messages.nextAttempt(time(next)) : time(d.at)}</span>
                  </TableCell>
                  {hasActions ? (
                    <TableCell className="mn-webhook-deliveries__actions">
                      <span className="mn-webhook-deliveries__action-row">
                        {onRedeliver && redeliverable(d) ? (
                          <Button
                            size="sm"
                            variant="secondary"
                            leadingIcon={<RetryGlyph />}
                            loading={busy}
                            aria-label={messages.redeliverLabel(d)}
                            onClick={() => redeliver(d)}
                          >
                            {messages.redeliver}
                          </Button>
                        ) : null}
                        {renderActions?.(d)}
                      </span>
                    </TableCell>
                  ) : null}
                </TableRow>
              );
            })}
          </TableBody>
        </Table>
      );
    }

    const showFilters = filterable;
    return (
      <div
        ref={ref}
        {...rest}
        className={cx('mn-webhook-deliveries', className)}
        data-loading={loading || undefined}
        data-filtered={filtered || undefined}
      >
        {showFilters ? (
          <div
            className="mn-webhook-deliveries__filters"
            role="group"
            aria-label={messages.caption}
          >
            <NativeSelect
              aria-label={messages.status}
              size="sm"
              value={filters.status}
              onChange={(e) => setFilters({ status: e.currentTarget.value })}
              options={[
                { value: '', label: messages.anyStatus },
                ...statusKeys.map((key) => ({ value: key, label: statuses[key]?.label ?? key })),
              ]}
              wrapperClassName="mn-webhook-deliveries__filter"
            />
            {eventTypes && eventTypes.length > 0 ? (
              <NativeSelect
                aria-label={messages.eventTypeFilter}
                size="sm"
                value={filters.eventType}
                onChange={(e) => setFilters({ eventType: e.currentTarget.value })}
                options={[
                  { value: '', label: messages.anyEventType },
                  ...eventTypes.map((t) => ({ value: t, label: t })),
                ]}
                wrapperClassName="mn-webhook-deliveries__filter"
              />
            ) : null}
            <Input
              type="search"
              size="sm"
              aria-label={messages.search}
              placeholder={messages.searchPlaceholder}
              value={filters.query}
              onChange={(e) => setFilters({ query: e.currentTarget.value })}
              autoComplete="off"
              spellCheck={false}
              className="mn-webhook-deliveries__search"
            />
            {filtered ? (
              <Button size="sm" variant="ghost" onClick={() => setFilters(EMPTY_FILTERS)}>
                {messages.clearFilters}
              </Button>
            ) : null}
          </div>
        ) : null}
        {countLine ? (
          <p className="mn-webhook-deliveries__counts" aria-live="polite">
            {countLine}
          </p>
        ) : null}
        {body}
        {!loading && !error && rows.length > 0 && onLoadMore ? (
          <LoadMore
            hasMore={hasMore}
            loading={loadingMore}
            error={loadMoreError}
            onLoadMore={onLoadMore}
            loadedCount={rows.length}
            noun={messages.noun}
            endLabel={messages.endOfList}
          />
        ) : null}
        <span className="mn-visually-hidden" role="status" aria-live="polite">
          {announcement}
        </span>
      </div>
    );
  },
);
