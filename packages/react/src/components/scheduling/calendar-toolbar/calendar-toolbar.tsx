import { forwardRef, type HTMLAttributes, type ReactNode } from 'react';
import { cx } from '../../../internal/cx.js';
import { ChevronRightGlyph } from '../../../internal/icons.js';
import { Button, IconButton } from '../../actions/button/index.js';
import { Spinner } from '../../feedback/spinner/index.js';
import { SegmentedControl } from '../../forms/segmented-control/index.js';

/** The built-in calendar views. Products may add their own ids. */
export type CalendarToolbarView = 'day' | 'week' | 'month' | 'agenda' | (string & {});

/** Every fixed string CalendarToolbar shows, for translation. */
export interface CalendarToolbarMessages {
  today: string;
  /** Accessible name of the previous button, e.g. "Previous week". */
  previous: (unit: string) => string;
  /** Accessible name of the next button, e.g. "Next week". */
  next: (unit: string) => string;
  /** Accessible name of the view switcher. */
  view: string;
  /** Label of each view in the switcher. Unknown ids show the id. */
  views: Record<string, string>;
  /** The unit each view moves by, used in the previous/next names. */
  units: Record<string, string>;
  /** Accessible name of the busy spinner. */
  loading: string;
}

const DEFAULT_MESSAGES: CalendarToolbarMessages = {
  today: 'Today',
  previous: (unit) => `Previous ${unit}`,
  next: (unit) => `Next ${unit}`,
  view: 'Calendar view',
  views: { day: 'Day', week: 'Week', month: 'Month', agenda: 'Agenda' },
  units: { day: 'day', week: 'week', month: 'month', agenda: 'period' },
  loading: 'Loading',
};

export interface CalendarToolbarProps extends Omit<HTMLAttributes<HTMLDivElement>, 'title'> {
  /** The range title, e.g. "November 2026" or "2 – 8 Nov 2026". Announced politely when it changes. */
  title: ReactNode;
  /** Heading level of the title. Default `h2`; use `h1` when the toolbar heads the page. */
  titleAs?: 'h1' | 'h2' | 'h3' | undefined;
  /** The current view. */
  view?: CalendarToolbarView | undefined;
  /** Views offered in the switcher, in order. Default day, week, month, agenda. */
  views?: readonly CalendarToolbarView[] | undefined;
  /** Shows the view switcher and is called with the chosen view. */
  onViewChange?: ((view: CalendarToolbarView) => void) | undefined;
  /** Jump to today. */
  onToday: () => void;
  /** Disable "Today" when the range already contains today. */
  todayDisabled?: boolean | undefined;
  /** Move back one unit of the view. */
  onPrevious: () => void;
  /** Move forward one unit of the view. */
  onNext: () => void;
  /** Disable the previous button, e.g. at the start of a booking window. */
  previousDisabled?: boolean | undefined;
  /** Disable the next button. */
  nextDisabled?: boolean | undefined;
  /** A date picker beside the arrows, e.g. a DatePicker or a button that opens a MiniMonth. */
  datePicker?: ReactNode | undefined;
  /** Shows a small spinner after the title while the range loads. */
  busy?: boolean | undefined;
  /** Actions at the end: filters, "New event", a settings menu. */
  actions?: ReactNode | undefined;
  /** Interface text, for translation. */
  messages?: Partial<CalendarToolbarMessages> | undefined;
}

/**
 * The calendar header every Rook app shares: Today, previous and next, an
 * optional date picker, the range title, a busy indicator, the view switcher
 * and actions. Navigation is the product's: the toolbar only calls back.
 */
export const CalendarToolbar = forwardRef<HTMLDivElement, CalendarToolbarProps>(
  function CalendarToolbar(
    {
      title,
      titleAs: Title = 'h2',
      view = 'week',
      views = ['day', 'week', 'month', 'agenda'],
      onViewChange,
      onToday,
      todayDisabled = false,
      onPrevious,
      onNext,
      previousDisabled = false,
      nextDisabled = false,
      datePicker,
      busy = false,
      actions,
      messages: messagesProp,
      className,
      ...rest
    },
    ref,
  ) {
    const m = {
      ...DEFAULT_MESSAGES,
      ...messagesProp,
      views: { ...DEFAULT_MESSAGES.views, ...messagesProp?.views },
      units: { ...DEFAULT_MESSAGES.units, ...messagesProp?.units },
    };
    const unit = m.units[view] ?? m.units.agenda ?? view;
    const showViews = !!onViewChange && views.length > 1;

    return (
      <div
        ref={ref}
        {...rest}
        className={cx('mn-calendar-toolbar', className)}
        data-busy={busy || undefined}
      >
        <div className="mn-calendar-toolbar__nav">
          <Button size="sm" onClick={onToday} disabled={todayDisabled}>
            {m.today}
          </Button>
          <div className="mn-calendar-toolbar__arrows">
            <IconButton
              size="sm"
              variant="ghost"
              label={m.previous(unit)}
              icon={<ChevronRightGlyph className="mn-calendar-toolbar__prev-icon" />}
              onClick={onPrevious}
              disabled={previousDisabled}
            />
            <IconButton
              size="sm"
              variant="ghost"
              label={m.next(unit)}
              icon={<ChevronRightGlyph className="mn-calendar-toolbar__next-icon" />}
              onClick={onNext}
              disabled={nextDisabled}
            />
          </div>
          {datePicker ? <div className="mn-calendar-toolbar__picker">{datePicker}</div> : null}
          <Title className="mn-calendar-toolbar__title" aria-live="polite" aria-atomic="true">
            {title}
          </Title>
          {busy ? (
            <Spinner size="sm" label={m.loading} className="mn-calendar-toolbar__busy" />
          ) : null}
        </div>
        {showViews || actions ? (
          <div className="mn-calendar-toolbar__end">
            {showViews ? (
              <SegmentedControl
                aria-label={m.view}
                size="sm"
                value={view}
                onValueChange={(v) => onViewChange?.(v)}
                options={views.map((v) => ({ value: v, label: m.views[v] ?? v }))}
              />
            ) : null}
            {actions}
          </div>
        ) : null}
      </div>
    );
  },
);
