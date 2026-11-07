'use client';
import { forwardRef, type ReactNode } from 'react';
import { cx } from '../../../internal/cx.js';
import {
  ActivityTimeline,
  type ActivityEvent,
  type ActivityTimelineMessages,
} from '../../collaboration/activity-timeline/index.js';
import type { DateInput } from '../relative-time/index.js';
import { Widget, type WidgetProps } from '../widget/index.js';

/** The words ActivityWidget shows. Override them to translate. */
export interface ActivityWidgetMessages {
  /** Default title. */
  title: string;
  /** Shown when there is no activity. */
  empty: string;
}

export const ACTIVITY_WIDGET_MESSAGES: ActivityWidgetMessages = {
  title: 'Recent activity',
  empty: 'No activity yet',
};

export interface ActivityWidgetProps extends Omit<
  WidgetProps,
  'title' | 'children' | 'empty' | 'skeleton' | 'messages'
> {
  /** Events, newest first. Only the first `limit` are shown. */
  events: readonly ActivityEvent[];
  /** Replaces the title ("Recent activity"). */
  title?: ReactNode;
  /** Most events shown; the view-all link leads to the rest. Default `6`. */
  limit?: number | undefined;
  /** `dot` (default) or the actor's `avatar` as each event's marker. */
  markers?: 'dot' | 'avatar' | undefined;
  /** Group under Today, Yesterday and dates. Default `false` (a short feed reads better as one list). */
  groupByDay?: boolean | undefined;
  /** IANA time zone for times and day groups. */
  timeZone?: string | undefined;
  /** Replaces the default empty text. */
  emptyContent?: ReactNode;
  /** Strings, for localisation. */
  messages?: Partial<ActivityWidgetMessages> | undefined;
  /** Strings passed to the ActivityTimeline. */
  timelineMessages?: Partial<ActivityTimelineMessages> | undefined;
  /** Reference time for relative times, for byte-identical server rendering. */
  now?: DateInput | undefined;
}

/**
 * The recent-activity card on a dashboard: a Widget holding a compact
 * ActivityTimeline of the latest events, with a link to the full log.
 */
export const ActivityWidget = forwardRef<HTMLElement, ActivityWidgetProps>(function ActivityWidget(
  {
    events,
    title,
    limit = 6,
    markers = 'dot',
    groupByDay = false,
    timeZone,
    emptyContent,
    messages: messagesProp,
    timelineMessages,
    now,
    locale,
    loading,
    className,
    ...rest
  },
  ref,
) {
  const messages = { ...ACTIVITY_WIDGET_MESSAGES, ...messagesProp };
  const shown = limit > 0 ? events.slice(0, limit) : events;
  const heading = title ?? messages.title;
  const timelineLabel = typeof heading === 'string' ? heading : messages.title;
  const shared = {
    markers,
    groupByDay,
    density: 'compact' as const,
    messages: { label: timelineLabel, empty: messages.empty, ...timelineMessages },
    ...(timeZone !== undefined && { timeZone }),
    ...(locale !== undefined && { locale }),
    ...(now !== undefined && { now }),
  };
  return (
    <Widget
      ref={ref}
      {...rest}
      title={heading}
      loading={loading}
      skeleton={<ActivityTimeline {...shared} events={[]} loading />}
      now={now}
      locale={locale}
      className={cx('mn-activity-widget', className)}
    >
      <ActivityTimeline
        {...shared}
        className="mn-activity-widget__timeline"
        events={shown}
        {...(emptyContent !== undefined && { empty: emptyContent })}
      />
    </Widget>
  );
});
