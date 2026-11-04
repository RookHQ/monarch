'use client';

import { forwardRef, useId, useState, type HTMLAttributes, type ReactNode } from 'react';
import { cx } from '../../../internal/cx.js';
import { Avatar } from '../../../components/identity/avatar/index.js';
import { Button } from '../../../components/actions/button/index.js';
import {
  RelativeTime,
  type DateInput,
} from '../../../components/data-display/relative-time/index.js';
import {
  DescriptionItem,
  DescriptionList,
} from '../../../components/data-display/description-list/index.js';
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '../../../components/data-grid/data-table/index.js';
import { LoadMore } from '../../../components/data-grid/load-more/index.js';
import { Skeleton } from '../../../components/feedback/skeleton/index.js';
import {
  Sheet,
  SheetBody,
  SheetContent,
  SheetDescription,
  SheetHeader,
  SheetTitle,
} from '../../../components/overlays/sheet/index.js';

export type AuditActorKind = 'person' | 'automation' | 'system' | 'api';

export interface AuditLogActor {
  name: string;
  avatarSrc?: string | null;
  /** `automation`, `system` and `api` get a tag, so readers can tell people from rules and keys. Default `person`. */
  kind?: AuditActorKind;
  /** Secondary line in the details drawer, e.g. an email address or key name. */
  detail?: ReactNode;
  seed?: string;
}

/** One changed field, for the before and after table in the details drawer. */
export interface AuditFieldChange {
  field: ReactNode;
  /** The old value. Leave out for a field that was set for the first time. */
  before?: ReactNode;
  /** The new value. Leave out for a field that was cleared. */
  after?: ReactNode;
}

export interface AuditLogEvent {
  id: string;
  /** When it happened. A Date, epoch milliseconds or an ISO 8601 string. */
  time: DateInput;
  /** Who did it. Leave out when unknown; `messages.unknownActor` is shown. */
  actor?: AuditLogActor;
  /** What happened, in words: "Changed a role", "Signed in". */
  action: ReactNode;
  /** What it happened to: a member, a setting, a record. */
  target?: ReactNode;
  /** Source IP address. */
  ip?: string;
  /** Device or client, e.g. "Firefox on macOS". */
  device?: ReactNode;
  /** Approximate location, when the product records one. */
  location?: ReactNode;
  /** Where the change came from, e.g. a Badge "API" or "Import". */
  source?: ReactNode;
  /** Why it was done, when the actor gave a reason. */
  reason?: ReactNode;
  /** Field-level changes, shown as a before and after table. */
  changes?: readonly AuditFieldChange[];
  /** Anything else for the drawer, e.g. a raw payload in a `<pre>`. */
  details?: ReactNode;
}

export type AuditLogColumn = 'time' | 'actor' | 'action' | 'target' | 'origin';

export interface AuditLogViewerMessages {
  /** Accessible name of the table. Default "Audit log". */
  caption: string;
  time: string;
  actor: string;
  action: string;
  target: string;
  /** Column for IP and device. Default "IP and device". */
  origin: string;
  /** Details button. Default "Details". */
  details: string;
  export: string;
  exporting: string;
  ipAddress: string;
  device: string;
  location: string;
  source: string;
  reason: string;
  changes: string;
  field: string;
  before: string;
  after: string;
  /** An empty before or after value. Default "None". */
  none: string;
  /** Tags for non-person actors. */
  automation: string;
  system: string;
  api: string;
  unknownActor: string;
  /** Default "No events match". */
  empty: string;
  loading: string;
  /** Drawer close button. */
  close: string;
}

const DEFAULT_MESSAGES: AuditLogViewerMessages = {
  caption: 'Audit log',
  time: 'Time',
  actor: 'Actor',
  action: 'Action',
  target: 'Target',
  origin: 'IP and device',
  details: 'Details',
  export: 'Export CSV',
  exporting: 'Exporting…',
  ipAddress: 'IP address',
  device: 'Device',
  location: 'Location',
  source: 'Source',
  reason: 'Reason',
  changes: 'Changes',
  field: 'Field',
  before: 'Before',
  after: 'After',
  none: 'None',
  automation: 'Automation',
  system: 'System',
  api: 'API',
  unknownActor: 'Unknown',
  empty: 'No events match',
  loading: 'Loading audit events',
  close: 'Close',
};

const ALL_COLUMNS: readonly AuditLogColumn[] = ['time', 'actor', 'action', 'target', 'origin'];

export interface AuditLogViewerProps extends Omit<HTMLAttributes<HTMLDivElement>, 'children'> {
  /** Loaded events, newest first. */
  events: readonly AuditLogEvent[];
  /** Filter controls (FilterBar, Select, SearchInput). The product filters or refetches `events`. */
  filters?: ReactNode;
  /** Shows an Export button. Build the file with `auditLogToCsv` or on the server. */
  onExport?: () => void;
  /** The export is running: the button shows a spinner. */
  exporting?: boolean;
  /** Columns to show, in order. Default all five. Hidden columns stay in the details drawer. */
  columns?: readonly AuditLogColumn[];
  /** The event whose details drawer is open, controlled. */
  selectedId?: string | null;
  defaultSelectedId?: string | null;
  onSelectedChange?: (id: string | null) => void;
  /** Extra drawer content for an event, after the built-in fields. */
  renderDetails?: (event: AuditLogEvent) => ReactNode;
  /** Cursor pagination: another page exists. */
  hasMore?: boolean;
  loadingMore?: boolean;
  loadMoreError?: boolean;
  onLoadMore?: () => void;
  /** First load: shows a skeleton. */
  loading?: boolean;
  /** Failure, e.g. an InlineErrorRetry or a permission note. Replaces the table. */
  error?: ReactNode;
  /** Replaces the default empty text, e.g. an EmptyState with a clear-filters action. */
  empty?: ReactNode;
  /** `compact` table rows. Default `comfortable`. */
  density?: 'compact' | 'comfortable';
  now?: DateInput;
  locale?: string;
  timeZone?: string;
  messages?: Partial<AuditLogViewerMessages>;
}

function toDate(input: DateInput): Date {
  return input instanceof Date ? input : new Date(input);
}

function iso(input: DateInput): string | undefined {
  const date = toDate(input);
  return Number.isNaN(date.getTime()) ? undefined : date.toISOString();
}

function absolute(input: DateInput, locale?: string, timeZone?: string): string {
  const date = toDate(input);
  if (Number.isNaN(date.getTime())) return '—';
  return new Intl.DateTimeFormat(locale, {
    dateStyle: 'medium',
    timeStyle: 'medium',
    ...(timeZone ? { timeZone } : {}),
  }).format(date);
}

/**
 * Builds a CSV file (RFC 4180) from rows of cells, for an audit log export.
 * Quotes cells that need it and defuses spreadsheet formulas (a cell that
 * starts with `=`, `+`, `-`, `@`, tab or carriage return is prefixed with `'`).
 */
export function auditLogToCsv(rows: ReadonlyArray<ReadonlyArray<unknown>>): string {
  const cell = (value: unknown): string => {
    let text =
      value === null || value === undefined
        ? ''
        : value instanceof Date
          ? value.toISOString()
          : String(value);
    if (/^[=+\-@\t\r]/.test(text)) text = `'${text}`;
    return /[",\r\n]/.test(text) ? `"${text.replace(/"/g, '""')}"` : text;
  };
  return rows.map((row) => row.map(cell).join(',')).join('\r\n');
}

/**
 * Security and audit events in a table: time, actor, action, target, IP and
 * device, with a details drawer (field changes, reason, payload), a filters
 * slot, an export action and cursor pagination.
 */
export const AuditLogViewer = forwardRef<HTMLDivElement, AuditLogViewerProps>(
  function AuditLogViewer(
    {
      events,
      filters,
      onExport,
      exporting = false,
      columns = ALL_COLUMNS,
      selectedId: selectedProp,
      defaultSelectedId = null,
      onSelectedChange,
      renderDetails,
      hasMore = false,
      loadingMore = false,
      loadMoreError = false,
      onLoadMore,
      loading = false,
      error,
      empty,
      density = 'comfortable',
      now,
      locale,
      timeZone,
      messages: messagesProp,
      className,
      ...rest
    },
    ref,
  ) {
    const m = { ...DEFAULT_MESSAGES, ...messagesProp };
    const id = useId();
    const [selectedInner, setSelectedInner] = useState<string | null>(defaultSelectedId);
    const selectedId = selectedProp !== undefined ? selectedProp : selectedInner;
    const select = (next: string | null) => {
      if (selectedProp === undefined) setSelectedInner(next);
      onSelectedChange?.(next);
    };
    const selected = selectedId ? events.find((e) => e.id === selectedId) : undefined;
    const safe = (v: string) => v.replace(/[^\w-]/g, '_');
    const detailsButtonId = (eventId: string) => `${id}-details-${safe(eventId)}`;
    const actionTextId = (eventId: string) => `${id}-action-${safe(eventId)}`;
    const timeProps = {
      ...(now !== undefined ? { now } : {}),
      ...(locale !== undefined ? { locale } : {}),
      ...(timeZone !== undefined ? { timeZone } : {}),
    };
    const kindLabel = (kind: AuditActorKind | undefined) =>
      kind === 'automation'
        ? m.automation
        : kind === 'system'
          ? m.system
          : kind === 'api'
            ? m.api
            : null;

    const actorCell = (actor: AuditLogActor | undefined) => {
      if (!actor) return <span className="mn-audit-log-viewer__muted">{m.unknownActor}</span>;
      const tag = kindLabel(actor.kind);
      return (
        <span className="mn-audit-log-viewer__actor">
          <Avatar
            name={actor.name}
            src={actor.avatarSrc ?? null}
            size="xs"
            decorative
            {...(actor.seed !== undefined ? { seed: actor.seed } : {})}
          />
          <span className="mn-audit-log-viewer__actor-name">{actor.name}</span>
          {tag ? <span className="mn-audit-log-viewer__tag">{tag}</span> : null}
        </span>
      );
    };

    const header: Record<AuditLogColumn, string> = {
      time: m.time,
      actor: m.actor,
      action: m.action,
      target: m.target,
      origin: m.origin,
    };

    const cell = (column: AuditLogColumn, event: AuditLogEvent) => {
      switch (column) {
        case 'time':
          return (
            <span className="mn-audit-log-viewer__time">
              <time dateTime={iso(event.time)} suppressHydrationWarning>
                {absolute(event.time, locale, timeZone)}
              </time>
            </span>
          );
        case 'actor':
          return actorCell(event.actor);
        case 'action':
          return (
            <span className="mn-audit-log-viewer__action">
              <span id={actionTextId(event.id)}>{event.action}</span>
              {event.source ? (
                <span className="mn-audit-log-viewer__source">{event.source}</span>
              ) : null}
              {columns.includes('time') ? (
                // Shown only in narrow containers, where the Time column is hidden.
                <time
                  className="mn-audit-log-viewer__time-inline"
                  dateTime={iso(event.time)}
                  suppressHydrationWarning
                >
                  {absolute(event.time, locale, timeZone)}
                </time>
              ) : null}
            </span>
          );
        case 'target':
          return event.target ?? <span className="mn-audit-log-viewer__muted">—</span>;
        case 'origin':
          return event.ip || event.device ? (
            <span className="mn-audit-log-viewer__origin">
              {event.ip ? <span className="mn-audit-log-viewer__ip">{event.ip}</span> : null}
              {event.device ? (
                <span className="mn-audit-log-viewer__device">{event.device}</span>
              ) : null}
            </span>
          ) : (
            <span className="mn-audit-log-viewer__muted">—</span>
          );
      }
    };

    let body: ReactNode;
    if (loading) {
      body = (
        <div className="mn-audit-log-viewer__skeleton" role="img" aria-label={m.loading}>
          {[0, 1, 2, 3].map((i) => (
            <Skeleton key={i} shape="rect" height="2.25rem" />
          ))}
        </div>
      );
    } else if (error) {
      body = <div className="mn-audit-log-viewer__error">{error}</div>;
    } else if (events.length === 0) {
      body = <div className="mn-audit-log-viewer__empty">{empty ?? m.empty}</div>;
    } else {
      body = (
        // aria-label, not a visually hidden caption: an absolutely positioned caption escapes
        // the table's scroll wrapper and widens the page on phones.
        <Table density={density} className="mn-audit-log-viewer__table" aria-label={m.caption}>
          <TableHeader>
            <TableRow>
              {columns.map((c) => (
                <TableHead key={c} scope="col" data-column={c}>
                  {header[c]}
                </TableHead>
              ))}
              <TableHead scope="col" data-column="details">
                <span className="mn-visually-hidden">{m.details}</span>
              </TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {events.map((event) => {
              const actionId = actionTextId(event.id);
              const buttonId = detailsButtonId(event.id);
              return (
                <TableRow key={event.id} selected={event.id === selectedId}>
                  {columns.map((c) => (
                    <TableCell key={c} data-column={c}>
                      {cell(c, event)}
                    </TableCell>
                  ))}
                  <TableCell data-column="details">
                    <Button
                      id={buttonId}
                      size="xs"
                      variant="ghost"
                      aria-haspopup="dialog"
                      aria-labelledby={
                        columns.includes('action') ? `${buttonId} ${actionId}` : undefined
                      }
                      onClick={() => select(event.id)}
                    >
                      {m.details}
                    </Button>
                  </TableCell>
                </TableRow>
              );
            })}
          </TableBody>
        </Table>
      );
    }

    const changes = selected?.changes ?? [];
    const value = (v: ReactNode) =>
      v === undefined || v === null || v === '' ? (
        <span className="mn-audit-log-viewer__muted">{m.none}</span>
      ) : (
        v
      );

    return (
      <div ref={ref} {...rest} className={cx('mn-audit-log-viewer', className)}>
        {filters || onExport ? (
          <div className="mn-audit-log-viewer__toolbar">
            <div className="mn-audit-log-viewer__filters">{filters}</div>
            {onExport ? (
              <Button size="sm" loading={exporting} onClick={onExport}>
                {exporting ? m.exporting : m.export}
              </Button>
            ) : null}
          </div>
        ) : null}
        {body}
        {hasMore && onLoadMore && !loading && !error ? (
          <LoadMore
            hasMore
            loading={loadingMore}
            error={loadMoreError}
            onLoadMore={onLoadMore}
            loadedCount={events.length}
            noun={{ one: 'event', other: 'events' }}
          />
        ) : null}
        <Sheet open={selected !== undefined} onOpenChange={(open) => (open ? null : select(null))}>
          {selected ? (
            <SheetContent
              size="md"
              closeLabel={m.close}
              className="mn-audit-log-viewer__sheet"
              // Return focus to the row's Details button, also when the product opened the drawer.
              onCloseAutoFocus={(event) => {
                const button = document.getElementById(detailsButtonId(selected.id));
                if (button) {
                  event.preventDefault();
                  button.focus();
                }
              }}
            >
              <SheetHeader>
                <SheetTitle>{selected.action}</SheetTitle>
                <SheetDescription>
                  <time dateTime={iso(selected.time)}>
                    {absolute(selected.time, locale, timeZone)}
                  </time>
                  {' · '}
                  <RelativeTime date={selected.time} tooltip={false} {...timeProps} />
                </SheetDescription>
              </SheetHeader>
              <SheetBody>
                <DescriptionList columns={1} layout="inline">
                  <DescriptionItem term={m.actor}>
                    <span className="mn-audit-log-viewer__stack">
                      {actorCell(selected.actor)}
                      {selected.actor?.detail ? (
                        <span className="mn-audit-log-viewer__muted">{selected.actor.detail}</span>
                      ) : null}
                    </span>
                  </DescriptionItem>
                  {selected.target ? (
                    <DescriptionItem term={m.target}>{selected.target}</DescriptionItem>
                  ) : null}
                  {selected.ip ? (
                    <DescriptionItem term={m.ipAddress}>
                      <span className="mn-audit-log-viewer__ip">{selected.ip}</span>
                    </DescriptionItem>
                  ) : null}
                  {selected.device ? (
                    <DescriptionItem term={m.device}>{selected.device}</DescriptionItem>
                  ) : null}
                  {selected.location ? (
                    <DescriptionItem term={m.location}>{selected.location}</DescriptionItem>
                  ) : null}
                  {selected.source ? (
                    <DescriptionItem term={m.source}>{selected.source}</DescriptionItem>
                  ) : null}
                  {selected.reason ? (
                    <DescriptionItem term={m.reason}>{selected.reason}</DescriptionItem>
                  ) : null}
                </DescriptionList>
                {changes.length > 0 ? (
                  <div className="mn-audit-log-viewer__changes">
                    <h3 className="mn-audit-log-viewer__section-title" id={`${id}-changes`}>
                      {m.changes}
                    </h3>
                    <Table density="compact" aria-labelledby={`${id}-changes`}>
                      <TableHeader>
                        <TableRow>
                          <TableHead scope="col">{m.field}</TableHead>
                          <TableHead scope="col">{m.before}</TableHead>
                          <TableHead scope="col">{m.after}</TableHead>
                        </TableRow>
                      </TableHeader>
                      <TableBody>
                        {changes.map((c, i) => (
                          <TableRow key={i}>
                            <TableHead scope="row" className="mn-audit-log-viewer__field">
                              {c.field}
                            </TableHead>
                            <TableCell data-change="before">{value(c.before)}</TableCell>
                            <TableCell data-change="after">{value(c.after)}</TableCell>
                          </TableRow>
                        ))}
                      </TableBody>
                    </Table>
                  </div>
                ) : null}
                {selected.details ? (
                  <div className="mn-audit-log-viewer__details">{selected.details}</div>
                ) : null}
                {renderDetails ? renderDetails(selected) : null}
              </SheetBody>
            </SheetContent>
          ) : null}
        </Sheet>
      </div>
    );
  },
);
