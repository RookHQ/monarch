'use client';

import { forwardRef, useId, useState, type HTMLAttributes, type ReactNode } from 'react';
import { cx } from '../../../internal/cx.js';
import { ChevronDownGlyph } from '../../../internal/icons.js';
import { dayLabel, groupByDay } from '../../../internal/day-groups.js';
import { Avatar } from '../../identity/avatar/index.js';
import { RelativeTime, type DateInput } from '../../data-display/relative-time/index.js';
import { LoadMore } from '../../data-grid/load-more/index.js';
import { Skeleton } from '../../feedback/skeleton/index.js';

export type ActivityActorKind = 'person' | 'automation' | 'system';
export type ActivityTone = 'neutral' | 'accent' | 'info' | 'success' | 'warning' | 'danger';

export interface ActivityActor {
  name: string;
  avatarSrc?: string | null;
  /** `automation` and `system` get an "Automated" tag, so readers can tell people from rules. Default `person`. */
  kind?: ActivityActorKind;
  seed?: string;
}

export interface ActivityEvent {
  id: string;
  /** Who did it. Leave out for events with no actor ("Ticket created from email"). */
  actor?: ActivityActor;
  /** What they did: "changed the status of", "commented on". */
  verb: ReactNode;
  /** What it was done to: a link, a name, "Open → Closed". */
  object?: ReactNode;
  /** When it happened. A Date, epoch milliseconds or an ISO 8601 string. */
  time: DateInput;
  /** Marker icon. Decorative. Default: a dot, or the actor's avatar with `markers="avatar"`. */
  icon?: ReactNode;
  /** Marker colour. Default `neutral`. */
  tone?: ActivityTone;
  /** Extra tags after the sentence, e.g. a source or stage. */
  meta?: ReactNode;
  /** Expandable details: a comment body, a field diff, a payload. Shown behind a disclosure button. */
  details?: ReactNode;
  /** Content always shown under the sentence, e.g. a comment. */
  body?: ReactNode;
}

export interface ActivityTimelineMessages {
  /** Name of the list when it is not grouped by day. Default "Activity". */
  label: string;
  today: string;
  yesterday: string;
  /** Tag for automation and system actors. Default "Automated". */
  automated: string;
  showDetails: string;
  hideDetails: string;
  /** Default "No activity yet". */
  empty: string;
  /** Spoken name of the loading skeleton. Default "Loading activity". */
  loading: string;
}

const DEFAULT_MESSAGES: ActivityTimelineMessages = {
  label: 'Activity',
  today: 'Today',
  yesterday: 'Yesterday',
  automated: 'Automated',
  showDetails: 'Show details',
  hideDetails: 'Hide details',
  empty: 'No activity yet',
  loading: 'Loading activity',
};

export interface ActivityTimelineProps extends Omit<HTMLAttributes<HTMLDivElement>, 'children'> {
  /** Events, newest first (or oldest first: order is kept). */
  events: readonly ActivityEvent[];
  /** Group under Today, Yesterday and dates. Default `true`. */
  groupByDay?: boolean;
  /** Filter controls above the feed (TabsFilter, FilterBar, a Select). The product filters `events`. */
  filters?: ReactNode;
  /** `dot` (default) or the actor's `avatar` as the marker. */
  markers?: 'dot' | 'avatar';
  /** `compact` tightens spacing for side panels. */
  density?: 'default' | 'compact';
  /** Open details, controlled. */
  expandedIds?: readonly string[];
  defaultExpandedIds?: readonly string[];
  onExpandedChange?: (ids: string[]) => void;
  /** First load: shows a skeleton. */
  loading?: boolean;
  /** Failure, e.g. an InlineErrorRetry. Replaces the feed. */
  error?: ReactNode;
  /** Replaces the default empty text. */
  empty?: ReactNode;
  /** Another page exists. Shows LoadMore after the feed. */
  hasMore?: boolean;
  loadingMore?: boolean;
  loadMoreError?: boolean;
  onLoadMore?: () => void;
  /** Heading level of the day labels. Default `h3`. */
  dayHeadingLevel?: 'h2' | 'h3' | 'h4' | 'h5' | 'h6';
  now?: DateInput;
  locale?: string;
  timeZone?: string;
  messages?: Partial<ActivityTimelineMessages>;
}

/**
 * A vertical feed of events: actor, verb, object, time and a marker, grouped
 * by day, with expandable details, a filter slot and load more. For
 * record histories, audit trails in context and project activity.
 */
export const ActivityTimeline = forwardRef<HTMLDivElement, ActivityTimelineProps>(
  function ActivityTimeline(
    {
      events,
      groupByDay: grouping = true,
      filters,
      markers = 'dot',
      density = 'default',
      expandedIds,
      defaultExpandedIds = [],
      onExpandedChange,
      loading = false,
      error,
      empty,
      hasMore = false,
      loadingMore = false,
      loadMoreError = false,
      onLoadMore,
      dayHeadingLevel: Heading = 'h3',
      now,
      locale,
      timeZone,
      messages: messagesProp,
      className,
      'aria-label': ariaLabel,
      ...rest
    },
    ref,
  ) {
    const m = { ...DEFAULT_MESSAGES, ...messagesProp };
    const id = useId();
    const [inner, setInner] = useState<string[]>(() => [...defaultExpandedIds]);
    const expanded = expandedIds ?? inner;
    const toggle = (eventId: string) => {
      const next = expanded.includes(eventId)
        ? expanded.filter((x) => x !== eventId)
        : [...expanded, eventId];
      if (expandedIds === undefined) setInner(next);
      onExpandedChange?.(next);
    };
    const label = ariaLabel ?? m.label;
    const timeProps = {
      ...(now !== undefined ? { now } : {}),
      ...(locale !== undefined ? { locale } : {}),
      ...(timeZone !== undefined ? { timeZone } : {}),
    };

    const item = (event: ActivityEvent) => {
      const open = expanded.includes(event.id);
      const detailsId = `${id}-details-${event.id.replace(/[^\w-]/g, '_')}`;
      const automated = event.actor && (event.actor.kind ?? 'person') !== 'person';
      const marker =
        event.icon ??
        (markers === 'avatar' && event.actor ? (
          <Avatar
            name={event.actor.name}
            src={event.actor.avatarSrc ?? null}
            size="xs"
            decorative
            {...(event.actor.seed !== undefined ? { seed: event.actor.seed } : {})}
          />
        ) : null);
      return (
        <li
          key={event.id}
          className="mn-activity-timeline__item"
          data-tone={event.tone ?? 'neutral'}
          data-marker={marker ? 'icon' : 'dot'}
        >
          <span className="mn-activity-timeline__marker" aria-hidden>
            {marker}
          </span>
          <div className="mn-activity-timeline__content">
            <p className="mn-activity-timeline__sentence">
              {event.actor ? (
                <span className="mn-activity-timeline__actor">{event.actor.name}</span>
              ) : null}
              {event.actor ? ' ' : null}
              <span className="mn-activity-timeline__verb">{event.verb}</span>
              {event.object ? (
                <>
                  {' '}
                  <span className="mn-activity-timeline__object">{event.object}</span>
                </>
              ) : null}
            </p>
            <div className="mn-activity-timeline__meta">
              <RelativeTime
                className="mn-activity-timeline__time"
                date={event.time}
                {...timeProps}
              />
              {automated ? <span className="mn-activity-timeline__tag">{m.automated}</span> : null}
              {event.meta}
            </div>
            {event.body ? <div className="mn-activity-timeline__body">{event.body}</div> : null}
            {event.details ? (
              <>
                <button
                  type="button"
                  className="mn-activity-timeline__toggle"
                  aria-expanded={open}
                  aria-controls={detailsId}
                  onClick={() => toggle(event.id)}
                >
                  {open ? m.hideDetails : m.showDetails}
                  <ChevronDownGlyph aria-hidden data-open={open || undefined} />
                </button>
                <div id={detailsId} className="mn-activity-timeline__details" hidden={!open}>
                  {open ? event.details : null}
                </div>
              </>
            ) : null}
          </div>
        </li>
      );
    };

    let feed: ReactNode;
    if (loading) {
      feed = (
        <div className="mn-activity-timeline__skeleton" role="img" aria-label={m.loading}>
          {[0.7, 0.5, 0.8].map((w, i) => (
            <div key={i} className="mn-activity-timeline__skeleton-row">
              <Skeleton shape="circle" width="0.75rem" height="0.75rem" />
              <Skeleton shape="text" width={`${w * 100}%`} />
            </div>
          ))}
        </div>
      );
    } else if (error) {
      feed = <div className="mn-activity-timeline__error">{error}</div>;
    } else if (events.length === 0) {
      feed = <div className="mn-activity-timeline__empty">{empty ?? m.empty}</div>;
    } else if (grouping) {
      feed = groupByDay(events, (e) => e.time, timeZone).map((g) => {
        const headingId = `${id}-day-${g.key}`;
        return (
          <div key={g.key} className="mn-activity-timeline__day">
            <Heading id={headingId} className="mn-activity-timeline__day-label">
              {dayLabel(
                g.date,
                { now, locale, timeZone },
                { today: m.today, yesterday: m.yesterday },
              )}
            </Heading>
            <ol className="mn-activity-timeline__list" aria-labelledby={headingId}>
              {g.items.map(item)}
            </ol>
          </div>
        );
      });
    } else {
      feed = (
        <ol className="mn-activity-timeline__list" aria-label={label}>
          {events.map(item)}
        </ol>
      );
    }

    return (
      <div
        ref={ref}
        {...rest}
        className={cx('mn-activity-timeline', className)}
        data-density={density}
      >
        {filters ? <div className="mn-activity-timeline__filters">{filters}</div> : null}
        {feed}
        {hasMore && onLoadMore && !loading && !error ? (
          <LoadMore
            hasMore
            loading={loadingMore}
            error={loadMoreError}
            onLoadMore={onLoadMore}
            loadedCount={events.length}
            showCount={false}
            noun={{ one: 'event', other: 'events' }}
          />
        ) : null}
      </div>
    );
  },
);
