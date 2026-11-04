import {
  forwardRef,
  type AnchorHTMLAttributes,
  type ButtonHTMLAttributes,
  type CSSProperties,
  type ReactNode,
  type Ref,
} from 'react';
import { Slot, Slottable } from '@radix-ui/react-slot';
import { cx } from '../../../internal/cx.js';

/**
 * How an event reads at a glance. `confirmed` is solid; `tentative` is hollow
 * or dashed (not yet accepted); `cancelled` and `declined` are muted and
 * struck through. Every non-confirmed status is also part of the chip's
 * accessible name, so it never relies on colour or line style alone.
 */
export type EventChipStatus = 'confirmed' | 'tentative' | 'cancelled' | 'declined';

/**
 * `dot` is one compact line for a month cell or popover list; `bar` has an
 * accent bar and room for a detail line, for agenda rows and all-day rows;
 * `block` fills its container, for events placed in a time grid.
 */
export type EventChipVariant = 'dot' | 'bar' | 'block';

/**
 * One event, as the calendar views (CalendarMonthView, TimeGrid, AgendaList)
 * take it. Plain, serialisable data: the product maps its own records to this.
 */
export interface CalendarEvent {
  /** Stable key. */
  id: string;
  title: string;
  /**
   * Start instant (ISO string or Date). For an all-day event a calendar day
   * (`'2026-11-04'`) is also accepted.
   */
  start: string | Date;
  /**
   * End instant, exclusive. For an all-day event the last day, inclusive
   * (`'2026-11-06'` for an event that runs 4–6 November); defaults to `start`.
   */
  end?: string | Date;
  /** Shown in the all-day row (TimeGrid) and listed first in a day. */
  allDay?: boolean;
  /** Any CSS colour, typically the product's colour for the calendar or service. */
  color?: string;
  status?: EventChipStatus;
  /** A second line where there is room: location, service, organiser. */
  detail?: string;
  /** Renders the chip as a link instead of a button. */
  href?: string;
}

const DEFAULT_STATUS_LABEL: Record<EventChipStatus, string> = {
  confirmed: 'Confirmed',
  tentative: 'Tentative',
  cancelled: 'Cancelled',
  declined: 'Declined',
};

interface EventChipOwnProps {
  /** The event title. Always part of the accessible name. */
  title: ReactNode;
  /**
   * Visible time, short or long: `"09:30"` in a month cell, `"09:30 – 10:15"`
   * in an agenda. Omit for all-day events shown in an all-day row.
   */
  time?: ReactNode;
  /**
   * The full time for assistive technology when `time` is abbreviated, for
   * example `"9:30 to 10:15"` or `"All day"`. When set, the visible `time` is
   * hidden from the accessible name and this text is used instead.
   */
  timeLabel?: string;
  /** Default `confirmed`. Exposed as `data-status`. */
  status?: EventChipStatus;
  /** Text for the status in the accessible name and in `bar`/`block` chips. Defaults to the English status name. */
  statusLabel?: string;
  /** Default `dot`. Exposed as `data-variant`. */
  variant?: EventChipVariant;
  /**
   * The event's colour (any CSS colour). It is only ever the marker, bar and
   * a light tint behind the text; text keeps Monarch's text colours, so it
   * stays readable in every theme. Defaults to the product accent.
   */
  color?: string;
  /** A second, muted line in `bar` and `block` chips. */
  detail?: ReactNode;
  /** The event has ended: drawn faded. Exposed as `data-past`. */
  past?: boolean;
  /**
   * `compact` puts a `block` chip on one line (time, then title) for short
   * events. Default `regular`. Exposed as `data-density`.
   */
  density?: 'regular' | 'compact';
  /**
   * Render the child element (a router link, or a trigger you supply) with
   * the chip's styling and content. The child must be a single element.
   */
  asChild?: boolean;
}

export type EventChipProps = EventChipOwnProps &
  Omit<ButtonHTMLAttributes<HTMLButtonElement>, 'title' | 'color'> &
  Pick<AnchorHTMLAttributes<HTMLAnchorElement>, 'href' | 'target' | 'rel'>;

/**
 * One event in any calendar view: a colour dot or accent bar, the title, the
 * time and its status. It is a button (or a link with `href`) whose accessible
 * name reads "title, time, status", so it can open the product's event detail
 * or popover.
 */
export const EventChip = forwardRef<HTMLButtonElement, EventChipProps>(function EventChip(
  {
    title,
    time,
    timeLabel,
    status = 'confirmed',
    statusLabel,
    variant = 'dot',
    color,
    detail,
    past = false,
    density = 'regular',
    asChild = false,
    href,
    target,
    rel,
    type = 'button',
    className,
    style,
    children,
    ...rest
  },
  ref,
) {
  const label = statusLabel ?? DEFAULT_STATUS_LABEL[status];
  const showStatus = status !== 'confirmed';
  const hasTime = time !== undefined && time !== null && time !== false && time !== '';
  const styles = color ? ({ ['--mn-event-color']: color, ...style } as CSSProperties) : style;
  const text = (n: ReactNode) =>
    typeof n === 'string' || typeof n === 'number' ? String(n) : null;
  // A plain-text name reads the same in every browser and screen reader; with
  // rich (non-string) content the name comes from the content instead.
  const parts = [
    text(title),
    timeLabel ?? (hasTime ? text(time) : ''),
    showStatus ? label : '',
    detail && variant !== 'dot' ? text(detail) : '',
  ];
  const computedName = parts.every((p) => p !== null)
    ? parts.filter(Boolean).join(', ')
    : undefined;
  const common = {
    'aria-label': computedName,
    ...rest,
    className: cx('mn-event-chip', className),
    style: styles,
    'data-variant': variant,
    'data-status': status,
    'data-past': past || undefined,
    'data-density': variant === 'block' ? density : undefined,
  };
  const content = (
    <>
      <span className="mn-event-chip__marker" aria-hidden />
      <span className="mn-event-chip__body">
        <span className="mn-event-chip__title">{title}</span>
        {/* Real spaces between parts: the visible text then reads "title time
            status", which the accessible name contains (WCAG 2.5.3). */}
        {hasTime || timeLabel ? ' ' : null}
        {hasTime ? (
          <span className="mn-event-chip__time" aria-hidden={timeLabel ? true : undefined}>
            {time}
          </span>
        ) : null}
        {timeLabel ? <span className="mn-visually-hidden">{timeLabel}</span> : null}
        {showStatus ? (
          <>
            {' '}
            <span className="mn-event-chip__status">{label}</span>
          </>
        ) : null}
        {detail && variant !== 'dot' ? (
          <>
            {' '}
            <span className="mn-event-chip__detail">{detail}</span>
          </>
        ) : null}
      </span>
    </>
  );

  if (asChild) {
    return (
      <Slot ref={ref} {...common}>
        <Slottable>{children}</Slottable>
        {content}
      </Slot>
    );
  }
  if (href !== undefined) {
    return (
      <a
        ref={ref as unknown as Ref<HTMLAnchorElement>}
        {...(common as AnchorHTMLAttributes<HTMLAnchorElement>)}
        href={href}
        target={target}
        rel={rel}
      >
        {content}
      </a>
    );
  }
  return (
    <button ref={ref} type={type} {...common}>
      {content}
    </button>
  );
});
