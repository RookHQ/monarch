'use client';

import {
  forwardRef,
  useId,
  useState,
  type ComponentPropsWithoutRef,
  type CSSProperties,
  type ElementRef,
  type ReactElement,
  type ReactNode,
} from 'react';
import { cx } from '../../../internal/cx.js';
import { ClockGlyph } from '../../../internal/icons.js';
import { formatIsoDate, isIsoDate, type HourCycle } from '../../../internal/dates/index.js';
import { Badge, type BadgeProps } from '../../data-display/badge/index.js';
import { Popover, PopoverContent, PopoverTrigger } from '../../overlays/popover/index.js';
import type { CalendarEvent, EventChipStatus } from '../event-chip/index.js';

/** One labelled row of details, e.g. hosts, location or a reference. */
export interface EventPopoverDetail {
  /** What the row is. Visually hidden when an icon is given, read by screen readers. */
  label: string;
  value: ReactNode;
  /** Decorative icon shown instead of the visible label. */
  icon?: ReactNode | undefined;
}

/** Every fixed string EventPopover shows, for translation. */
export interface EventPopoverMessages {
  /** Hidden label of the time row. */
  time: string;
  /** Shown for an all-day event. */
  allDay: string;
  /** Badge text for each status. */
  status: Record<EventChipStatus, string>;
}

const DEFAULT_MESSAGES: EventPopoverMessages = {
  time: 'Time',
  allDay: 'All day',
  status: {
    confirmed: 'Confirmed',
    tentative: 'Tentative',
    cancelled: 'Cancelled',
    declined: 'Declined',
  },
};

const STATUS_TONE: Record<EventChipStatus, NonNullable<BadgeProps['tone']>> = {
  confirmed: 'success',
  tentative: 'warning',
  cancelled: 'neutral',
  declined: 'danger',
};

export interface EventPopoverProps extends Omit<
  ComponentPropsWithoutRef<typeof PopoverContent>,
  'title' | 'children' | 'content'
> {
  /** The trigger: one focusable element, usually an EventChip. Rendered with `asChild`. */
  children: ReactElement;
  /** Controlled open state. */
  open?: boolean | undefined;
  /** Uncontrolled initial open state. */
  defaultOpen?: boolean | undefined;
  onOpenChange?: ((open: boolean) => void) | undefined;
  /** The event to show. Its title, times, colour, status and detail fill the popover. */
  event?: CalendarEvent | undefined;
  /** Heading. Default `event.title`. */
  title?: ReactNode | undefined;
  /** Second line under the heading, e.g. the service or calendar. Default `event.detail`. */
  subtitle?: ReactNode | undefined;
  /** Start instant (or ISO day for all-day). Default `event.start`. */
  start?: string | Date | undefined;
  /** End instant (or last ISO day for all-day, inclusive). Default `event.end`. */
  end?: string | Date | undefined;
  /** All-day event. Default `event.allDay`. */
  allDay?: boolean | undefined;
  /** Swatch colour (any CSS colour). Default `event.color`. */
  color?: string | undefined;
  /** Status badge. Default from `event.status`; pass a node for product statuses. */
  status?: EventChipStatus | ReactNode | undefined;
  /** IANA zone for times. Default: the runtime's zone. */
  timeZone?: string | undefined;
  /** Locale for dates and times. */
  locale?: string | undefined;
  /** Force a 12- or 24-hour clock. */
  hourCycle?: HourCycle | undefined;
  /** Labelled rows under the time: hosts, location, reference. */
  details?: readonly EventPopoverDetail[] | undefined;
  /** A note under the details, e.g. a cancellation reason or a privacy notice. */
  note?: ReactNode | undefined;
  /** Footer actions. A function receives `close` so an action can close the popover first. */
  actions?: ReactNode | ((close: () => void) => ReactNode) | undefined;
  /** Interface text, for translation. */
  messages?: Partial<EventPopoverMessages> | undefined;
}

const isStatus = (value: unknown): value is EventChipStatus =>
  value === 'confirmed' || value === 'tentative' || value === 'cancelled' || value === 'declined';

function formatWhen(
  start: string | Date,
  end: string | Date | undefined,
  allDay: boolean,
  zone: string | undefined,
  locale: string | undefined,
  hourCycle: HourCycle | undefined,
  allDayLabel: string,
): { date: string; time: string } {
  const dayOpts: Intl.DateTimeFormatOptions = {
    weekday: 'short',
    day: 'numeric',
    month: 'short',
    year: 'numeric',
  };
  if (allDay) {
    const fmt = (v: string | Date) =>
      typeof v === 'string' && isIsoDate(v)
        ? formatIsoDate(v, locale, dayOpts)
        : new Intl.DateTimeFormat(locale, { ...dayOpts, timeZone: zone }).format(new Date(v));
    const first = fmt(start);
    const last = end ? fmt(end) : first;
    return { date: last === first ? first : `${first} – ${last}`, time: allDayLabel };
  }
  const s = new Date(start);
  const e = end ? new Date(end) : null;
  const timeOpts: Intl.DateTimeFormatOptions = {
    hour: hourCycle === '24' ? '2-digit' : 'numeric',
    minute: '2-digit',
    timeZone: zone,
  };
  if (hourCycle) timeOpts.hourCycle = hourCycle === '12' ? 'h12' : 'h23';
  const dateF = new Intl.DateTimeFormat(locale, { ...dayOpts, timeZone: zone });
  const timeF = new Intl.DateTimeFormat(locale, timeOpts);
  const zoneName =
    new Intl.DateTimeFormat(locale, { timeZone: zone, timeZoneName: 'short' })
      .formatToParts(s)
      .find((p) => p.type === 'timeZoneName')?.value ?? '';
  const date = dateF.format(s);
  if (!e) return { date, time: `${timeF.format(s)} ${zoneName}`.trim() };
  const endDate = dateF.format(e);
  return endDate === date
    ? { date, time: `${timeF.format(s)} – ${timeF.format(e)} ${zoneName}`.trim() }
    : {
        date: `${date} ${timeF.format(s)} –`,
        time: `${endDate} ${timeF.format(e)} ${zoneName}`.trim(),
      };
}

/**
 * Quick view of a calendar event, opened from its chip: colour, title,
 * status, date and time in the stated zone, labelled detail rows, a note and
 * actions. Built on Popover, so it is non-modal, dismisses on Escape or an
 * outside click and returns focus to the chip.
 */
export const EventPopover = forwardRef<ElementRef<typeof PopoverContent>, EventPopoverProps>(
  function EventPopover(
    {
      children,
      open: openProp,
      defaultOpen = false,
      onOpenChange,
      event,
      title,
      subtitle,
      start,
      end,
      allDay,
      color,
      status,
      timeZone,
      locale,
      hourCycle,
      details,
      note,
      actions,
      messages: messagesProp,
      className,
      style,
      collisionPadding = 12,
      ...rest
    },
    ref,
  ) {
    const m = {
      ...DEFAULT_MESSAGES,
      ...messagesProp,
      status: { ...DEFAULT_MESSAGES.status, ...messagesProp?.status },
    };
    const [openState, setOpenState] = useState(defaultOpen);
    const open = openProp ?? openState;
    const setOpen = (next: boolean) => {
      if (openProp === undefined) setOpenState(next);
      onOpenChange?.(next);
    };
    const titleId = useId();
    const heading = title ?? event?.title;
    const sub = subtitle ?? event?.detail;
    const from = start ?? event?.start;
    const to = end ?? event?.end;
    const swatch = color ?? event?.color;
    const st = status ?? event?.status;
    const when =
      from !== undefined
        ? formatWhen(
            from,
            to,
            allDay ?? event?.allDay ?? false,
            timeZone,
            locale,
            hourCycle,
            m.allDay,
          )
        : null;
    const footer = typeof actions === 'function' ? actions(() => setOpen(false)) : actions;

    return (
      <Popover open={open} onOpenChange={setOpen}>
        <PopoverTrigger asChild>{children}</PopoverTrigger>
        <PopoverContent
          ref={ref}
          aria-labelledby={titleId}
          collisionPadding={collisionPadding}
          {...rest}
          className={cx('mn-event-popover', className)}
          style={swatch ? ({ ['--mn-event-color']: swatch, ...style } as CSSProperties) : style}
          data-status={isStatus(st) ? st : undefined}
        >
          <div className="mn-event-popover__head">
            <span
              className="mn-event-popover__swatch"
              aria-hidden="true"
              data-color={swatch ? '' : undefined}
            />
            <div className="mn-event-popover__heading">
              <p id={titleId} className="mn-event-popover__title">
                {heading}
              </p>
              {sub ? <p className="mn-event-popover__subtitle">{sub}</p> : null}
            </div>
            {st !== undefined && st !== null ? (
              isStatus(st) ? (
                <Badge tone={STATUS_TONE[st]} className="mn-event-popover__status">
                  {m.status[st]}
                </Badge>
              ) : (
                <span className="mn-event-popover__status">{st}</span>
              )
            ) : null}
          </div>
          {when || (details && details.length > 0) || note ? (
            <div className="mn-event-popover__body">
              {when || (details && details.length > 0) ? (
                <dl className="mn-event-popover__details">
                  {when ? (
                    <div className="mn-event-popover__row">
                      <dt className="mn-visually-hidden">{m.time}</dt>
                      <dd>
                        <span className="mn-event-popover__icon" aria-hidden="true">
                          <ClockGlyph />
                        </span>
                        <span>{when.date}</span>{' '}
                        <span className="mn-event-popover__muted">· {when.time}</span>
                      </dd>
                    </div>
                  ) : null}
                  {details?.map((d, i) => (
                    <div key={`${d.label}-${i}`} className="mn-event-popover__row">
                      <dt className={d.icon ? 'mn-visually-hidden' : 'mn-event-popover__label'}>
                        {d.label}
                      </dt>
                      <dd>
                        {d.icon ? (
                          <span className="mn-event-popover__icon" aria-hidden="true">
                            {d.icon}
                          </span>
                        ) : null}
                        <span>{d.value}</span>
                      </dd>
                    </div>
                  ))}
                </dl>
              ) : null}
              {note ? <div className="mn-event-popover__note">{note}</div> : null}
            </div>
          ) : null}
          {footer ? <div className="mn-event-popover__actions">{footer}</div> : null}
        </PopoverContent>
      </Popover>
    );
  },
);
