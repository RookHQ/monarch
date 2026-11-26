import { describe, expect, it, vi } from 'vitest';
import { useState } from 'react';
import { act, render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { hydrateRoot } from 'react-dom/client';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { MiniMonth } from './index.js';
import { hydrationErrors } from '../../../../test/hydration.js';

const base = { locale: 'en-GB', today: '2026-11-04', weekStartsOn: 1 as const };
/** A day button by its full name, e.g. day(/9 November 2026/) or day(/Monday,? 9 November 2026/). */
const day = (name: RegExp | string) =>
  screen.getByRole('button', {
    name: typeof name === 'string' ? name : new RegExp(`^(\\p{L}+,? )?${name.source}`, 'u'),
  });

describe('MiniMonth', () => {
  it('is a labelled grid of the month with weekday headers', async () => {
    render(<MiniMonth {...base} defaultValue="2026-11-10" />);
    const grid = screen.getByRole('grid', { name: 'November 2026' });
    const headers = within(grid).getAllByRole('columnheader');
    expect(headers.map((h) => h.textContent)).toEqual([
      'Mon',
      'Tue',
      'Wed',
      'Thu',
      'Fri',
      'Sat',
      'Sun',
    ]);
    expect(headers[0]).toHaveAttribute('abbr', 'Monday');
    expect(within(grid).getAllByRole('row')).toHaveLength(7);
    await expectNoA11yViolations();
  });

  it('marks the selection with aria-selected and today with aria-current', () => {
    render(<MiniMonth {...base} defaultValue="2026-11-10" />);
    const selected = day(/10 November 2026/);
    expect(selected.closest('td')).toHaveAttribute('aria-selected', 'true');
    expect(day(/11 November 2026/).closest('td')).toHaveAttribute('aria-selected', 'false');
    expect(day(/4 November 2026/)).toHaveAttribute('aria-current', 'date');
    expect(selected).not.toHaveAttribute('aria-current');
  });

  it('has one Tab stop: the selected day, else today', async () => {
    const user = userEvent.setup();
    const { unmount } = render(
      <>
        <MiniMonth {...base} value="2026-11-10" />
        <button type="button">After</button>
      </>,
    );
    const tabbable = screen
      .getAllByRole('button')
      .filter((b) => b.getAttribute('tabindex') === '0' && b.dataset.date);
    expect(tabbable).toHaveLength(1);
    await user.tab(); // previous month
    await user.tab(); // month title (opens the month picker)
    await user.tab(); // next month
    await user.tab();
    expect(day(/10 November 2026/)).toHaveFocus();
    await user.tab();
    expect(screen.getByRole('button', { name: 'After' })).toHaveFocus();
    unmount();
    render(<MiniMonth {...base} value={null} />);
    expect(day(/4 November 2026/)).toHaveAttribute('tabindex', '0');
  });

  it('moves by day and week with the arrow keys, and to the week ends with Home and End', async () => {
    const user = userEvent.setup();
    render(<MiniMonth {...base} defaultValue="2026-11-11" />);
    day(/11 November 2026/).focus();
    await user.keyboard('{ArrowRight}');
    expect(day(/12 November 2026/)).toHaveFocus();
    await user.keyboard('{ArrowDown}');
    expect(day(/19 November 2026/)).toHaveFocus();
    await user.keyboard('{ArrowLeft}{ArrowUp}');
    expect(day(/11 November 2026/)).toHaveFocus();
    await user.keyboard('{Home}');
    expect(day(/Monday,? 9 November 2026/)).toHaveFocus();
    await user.keyboard('{End}');
    expect(day(/Sunday,? 15 November 2026/)).toHaveFocus();
    expect(day(/15 November 2026/)).toHaveAttribute('tabindex', '0');
  });

  it('crosses into the next and previous month, changing the visible month', async () => {
    const user = userEvent.setup();
    const onMonthChange = vi.fn();
    render(<MiniMonth {...base} defaultValue="2026-11-30" onMonthChange={onMonthChange} />);
    day(/30 November 2026/).focus();
    await user.keyboard('{ArrowRight}');
    expect(screen.getByRole('grid', { name: 'December 2026' })).toBeInTheDocument();
    expect(day(/1 December 2026/)).toHaveFocus();
    expect(onMonthChange).toHaveBeenLastCalledWith('2026-12-01');
    await user.keyboard('{ArrowLeft}');
    expect(day(/30 November 2026/)).toHaveFocus();
  });

  it('PageUp/PageDown move a month and Shift+PageUp/PageDown a year, clamping the day', async () => {
    const user = userEvent.setup();
    render(<MiniMonth {...base} defaultValue="2026-01-31" />);
    day(/31 January 2026/).focus();
    await user.keyboard('{PageDown}');
    expect(day(/28 February 2026/)).toHaveFocus();
    await user.keyboard('{PageUp}');
    expect(day(/28 January 2026/)).toHaveFocus();
    await user.keyboard('{Shift>}{PageDown}{/Shift}');
    expect(day(/28 January 2027/)).toHaveFocus();
    await user.keyboard('{Shift>}{PageUp}{/Shift}');
    expect(day(/28 January 2026/)).toHaveFocus();
  });

  it('selects with click, Enter and Space', async () => {
    const user = userEvent.setup();
    const onValueChange = vi.fn();
    render(<MiniMonth {...base} onValueChange={onValueChange} />);
    await user.click(day(/12 November 2026/));
    expect(day(/12 November 2026/).closest('td')).toHaveAttribute('aria-selected', 'true');
    await user.keyboard('{ArrowRight}{Enter}');
    await user.keyboard('{ArrowRight} ');
    expect(onValueChange.mock.calls.map((c) => c[0])).toEqual([
      '2026-11-12',
      '2026-11-13',
      '2026-11-14',
    ]);
  });

  it('controlled mode follows the value prop only', async () => {
    const user = userEvent.setup();
    const onValueChange = vi.fn();
    render(<MiniMonth {...base} value="2026-11-10" onValueChange={onValueChange} />);
    await user.click(day(/12 November 2026/));
    expect(onValueChange).toHaveBeenCalledWith('2026-11-12');
    expect(day(/10 November 2026/).closest('td')).toHaveAttribute('aria-selected', 'true');
    expect(day(/12 November 2026/).closest('td')).toHaveAttribute('aria-selected', 'false');
  });

  it('controlled month', async () => {
    const user = userEvent.setup();
    function Controlled() {
      const [month, setMonth] = useState('2026-11-01');
      return (
        <>
          <MiniMonth {...base} month={month} onMonthChange={setMonth} />
          <output>{month}</output>
        </>
      );
    }
    render(<Controlled />);
    await user.click(screen.getByRole('button', { name: 'Next month' }));
    expect(screen.getByRole('grid', { name: 'December 2026' })).toBeInTheDocument();
    expect(screen.getByText('2026-12-01')).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Previous month' }));
    await user.click(screen.getByRole('button', { name: 'Previous month' }));
    expect(screen.getByRole('grid', { name: 'October 2026' })).toBeInTheDocument();
  });

  it('respects min and max: no navigation past them, keys clamp, days are disabled', async () => {
    const user = userEvent.setup();
    const onValueChange = vi.fn();
    render(
      <MiniMonth
        {...base}
        min="2026-11-03"
        max="2026-12-20"
        defaultValue="2026-11-05"
        onValueChange={onValueChange}
      />,
    );
    expect(screen.getByRole('button', { name: 'Previous month' })).toBeDisabled();
    const before = day(/2 November 2026/);
    expect(before).toHaveAttribute('aria-disabled', 'true');
    expect(before).not.toHaveAttribute('data-unavailable');
    await user.click(before);
    expect(onValueChange).not.toHaveBeenCalled();
    day(/5 November 2026/).focus();
    await user.keyboard('{ArrowUp}');
    expect(day(/3 November 2026/)).toHaveFocus();
    await user.keyboard('{Shift>}{PageDown}{/Shift}');
    expect(day(/20 December 2026/)).toHaveFocus();
    expect(screen.getByRole('button', { name: 'Next month' })).toBeDisabled();
  });

  it('isDateDisabled days stay focusable but cannot be chosen', async () => {
    const user = userEvent.setup();
    const onValueChange = vi.fn();
    const weekend = (d: string) => [0, 6].includes(new Date(`${d}T00:00Z`).getUTCDay());
    render(<MiniMonth {...base} isDateDisabled={weekend} onValueChange={onValueChange} />);
    day(/Friday,? 6 November 2026/).focus();
    await user.keyboard('{ArrowRight}');
    expect(day(/Saturday,? 7 November 2026/)).toHaveFocus();
    await user.keyboard('{Enter}');
    expect(onValueChange).not.toHaveBeenCalled();
    expect(day(/Saturday,? 7 November 2026/).closest('td')).toHaveAttribute(
      'aria-disabled',
      'true',
    );
  });

  it('starts the week from the locale or the prop', () => {
    const { unmount } = render(<MiniMonth locale="en-US" today="2026-11-04" />);
    expect(screen.getAllByRole('columnheader')[0]).toHaveTextContent('Sun');
    unmount();
    render(<MiniMonth locale="en-US" today="2026-11-04" weekStartsOn={6} />);
    expect(screen.getAllByRole('columnheader')[0]).toHaveTextContent('Sat');
  });

  it('shows ISO week numbers as row headers', async () => {
    render(<MiniMonth {...base} showWeekNumbers />);
    const rowHeaders = screen.getAllByRole('rowheader');
    expect(rowHeaders[0]).toHaveAccessibleName('Week 44');
    expect(rowHeaders[1]).toHaveTextContent('45');
    await expectNoA11yViolations();
  });

  it('draws event dots and adds their description to the day name', () => {
    render(
      <MiniMonth
        {...base}
        getDayInfo={(d) =>
          d === '2026-11-12'
            ? { dots: ['accent', 'warning', 'danger', 'info'], description: '4 events' }
            : undefined
        }
      />,
    );
    const busy = day(/12 November 2026, 4 events$/);
    expect(busy.querySelectorAll('.mn-mini-month__dot')).toHaveLength(3);
  });

  it('highlights a range and a hover preview, reporting hovered days', async () => {
    const user = userEvent.setup();
    const onDayHover = vi.fn();
    render(
      <MiniMonth
        {...base}
        range={{ start: '2026-11-10', end: '2026-11-12' }}
        rangePreview={{ start: '2026-11-12', end: '2026-11-14' }}
        onDayHover={onDayHover}
      />,
    );
    const cell = (n: number) => day(new RegExp(`${n} November 2026`)).closest('td');
    expect(cell(10)).toHaveAttribute('data-range-start');
    expect(cell(11)).toHaveAttribute('aria-selected', 'true');
    expect(cell(11)).toHaveAttribute('data-in-range');
    expect(cell(12)).toHaveAttribute('data-range-end');
    expect(cell(13)).toHaveAttribute('data-preview');
    expect(cell(13)).toHaveAttribute('aria-selected', 'false');
    await user.hover(day(/20 November 2026/));
    expect(onDayHover).toHaveBeenLastCalledWith('2026-11-20');
    await user.unhover(day(/20 November 2026/));
    await user.unhover(screen.getAllByRole('grid')[0]!);
    expect(onDayHover).toHaveBeenLastCalledWith(null);
  });

  it('shows several months with focus moving between them', async () => {
    const user = userEvent.setup();
    render(<MiniMonth {...base} numberOfMonths={2} defaultMonth="2026-11-01" />);
    expect(screen.getAllByRole('grid').map((g) => g.getAttribute('aria-labelledby'))).toHaveLength(
      2,
    );
    expect(screen.getByRole('grid', { name: 'December 2026' })).toBeInTheDocument();
    expect(screen.getAllByRole('button', { name: 'Next month' })).toHaveLength(1);
    day(/30 November 2026/).focus();
    await user.keyboard('{ArrowRight}');
    expect(day(/1 December 2026/)).toHaveFocus();
    expect(screen.getByRole('grid', { name: 'November 2026' })).toBeInTheDocument();
    await user.keyboard('{PageDown}');
    expect(day(/1 January 2027/)).toHaveFocus();
    expect(screen.getByRole('grid', { name: 'December 2026' })).toBeInTheDocument();
    expect(screen.queryByRole('grid', { name: 'November 2026' })).not.toBeInTheDocument();
  });

  it('reverses Left and Right in right-to-left text', async () => {
    const user = userEvent.setup();
    render(
      <div dir="rtl" style={{ direction: 'rtl' }}>
        <MiniMonth {...base} defaultValue="2026-11-11" />
      </div>,
    );
    day(/11 November 2026/).focus();
    await user.keyboard('{ArrowLeft}');
    expect(day(/12 November 2026/)).toHaveFocus();
  });

  it('focusOnMount focuses the current day on mount', () => {
    render(<MiniMonth {...base} defaultValue="2026-11-18" focusOnMount />);
    expect(day(/18 November 2026/)).toHaveFocus();
  });

  it('keeps focus on the month buttons, so they can be pressed again from the keyboard', async () => {
    render(<MiniMonth {...base} defaultValue="2026-11-10" />);
    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: 'Next month' }));
    expect(screen.getByRole('button', { name: 'Next month' })).toHaveFocus();
    await user.keyboard('{Enter}');
    expect(screen.getByRole('grid', { name: 'January 2027' })).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Previous month' }));
    expect(screen.getByRole('button', { name: 'Previous month' })).toHaveFocus();
  });

  it('does not steal focus later when a key was pressed at the min date', async () => {
    render(<MiniMonth {...base} defaultValue="2026-11-10" min="2026-11-10" />);
    const user = userEvent.setup();
    day(/10 November 2026/).focus();
    // Clamped to `min`: focus stays where it is.
    await user.keyboard('{ArrowLeft}');
    expect(day(/10 November 2026/)).toHaveFocus();
    const next = screen.getByRole('button', { name: 'Next month' });
    await user.click(next);
    expect(screen.getByRole('grid', { name: 'December 2026' })).toBeInTheDocument();
    expect(next).toHaveFocus();
  });

  it('hydrates without a mismatch when the server and browser locales differ', async () => {
    let machineLocale = 'en-US';
    const original = Intl.DateTimeFormat.prototype.resolvedOptions;
    const spy = vi
      .spyOn(Intl.DateTimeFormat.prototype, 'resolvedOptions')
      .mockImplementation(function (this: Intl.DateTimeFormat) {
        return { ...original.call(this), locale: machineLocale };
      });
    const error = vi.spyOn(console, 'error').mockImplementation(() => {});
    const ui = <MiniMonth today="2026-11-04" weekStartsOn={1} defaultValue="2026-11-10" />;
    const container = document.createElement('div');
    try {
      const html = renderToString(ui);
      expect(html).toContain('Tuesday, November 10, 2026');
      machineLocale = 'en-GB';
      container.innerHTML = html;
      document.body.append(container);
      let root: ReturnType<typeof hydrateRoot> | undefined;
      await act(async () => {
        root = hydrateRoot(container, ui);
      });
      expect(hydrationErrors(error)).toEqual([]);
      expect(container.innerHTML).toContain('10 November 2026');
      act(() => root?.unmount());
    } finally {
      container.remove();
      spy.mockRestore();
      error.mockRestore();
    }
  });

  it('renders on the server with an explicit locale', () => {
    const html = renderToString(<MiniMonth {...base} defaultValue="2026-11-10" />);
    expect(html).toContain('mn-mini-month');
    expect(html).toContain('November 2026');
    expect(html).toContain('aria-current="date"');
  });
  describe('month and year pickers', () => {
    const title = () => screen.getByRole('button', { name: /^November 2026, choose month/ });

    it('opens a month picker from the title and jumps to the chosen month', async () => {
      const user = userEvent.setup();
      const onMonthChange = vi.fn();
      render(<MiniMonth {...base} onMonthChange={onMonthChange} />);
      await user.click(title());
      expect(screen.getByRole('group', { name: 'Months' })).toBeInTheDocument();
      expect(screen.getByRole('button', { name: 'November 2026' })).toHaveAttribute(
        'aria-pressed',
        'true',
      );
      await user.click(screen.getByRole('button', { name: 'March 2026' }));
      expect(onMonthChange).toHaveBeenCalledWith('2026-03-01');
      expect(screen.getByRole('grid', { name: 'March 2026' })).toBeInTheDocument();
      expect(screen.queryByRole('group', { name: 'Months' })).not.toBeInTheDocument();
      // Focus returns to the title so keyboard users keep their place.
      expect(screen.getByRole('button', { name: /^March 2026, choose month/ })).toHaveFocus();
    });

    it('goes back 30 years in a few clicks through the year picker', async () => {
      const user = userEvent.setup();
      render(<MiniMonth {...base} />);
      await user.click(title());
      await user.click(screen.getByRole('button', { name: '2026, Choose year' }));
      expect(screen.getByRole('group', { name: 'Years' })).toBeInTheDocument();
      expect(screen.getByText('2016 – 2027')).toBeInTheDocument();
      await user.click(screen.getByRole('button', { name: 'Previous years' }));
      await user.click(screen.getByRole('button', { name: 'Previous years' }));
      expect(screen.getByText('1992 – 2003')).toBeInTheDocument();
      await user.click(screen.getByRole('button', { name: '1996' }));
      // Choosing a year opens that year's months; the month then opens the days.
      expect(screen.getByRole('button', { name: '1996, Choose year' })).toBeInTheDocument();
      await user.click(screen.getByRole('button', { name: 'June 1996' }));
      expect(screen.getByRole('grid', { name: 'June 1996' })).toBeInTheDocument();
    });

    it('steps the year in the month picker and works from the keyboard', async () => {
      const user = userEvent.setup();
      render(<MiniMonth {...base} />);
      await user.click(title());
      await user.click(screen.getByRole('button', { name: 'Previous year' }));
      expect(screen.getByRole('button', { name: '2025, Choose year' })).toBeInTheDocument();
      screen.getByRole('button', { name: 'November 2025' }).focus();
      await user.keyboard('{ArrowLeft}{ArrowUp}');
      expect(screen.getByRole('button', { name: 'July 2025' })).toHaveFocus();
      await user.keyboard('{PageUp}');
      expect(screen.getByRole('button', { name: '2024, Choose year' })).toBeInTheDocument();
      await user.keyboard('{Home}{Enter}');
      expect(screen.getByRole('grid', { name: 'January 2024' })).toBeInTheDocument();
    });

    it('disables months and years outside min and max', async () => {
      const user = userEvent.setup();
      render(<MiniMonth {...base} min="2026-03-10" max="2027-01-20" />);
      await user.click(title());
      expect(screen.getByRole('button', { name: 'February 2026' })).toBeDisabled();
      expect(screen.getByRole('button', { name: 'March 2026' })).toBeEnabled();
      expect(screen.getByRole('button', { name: 'Previous year' })).toBeDisabled();
      await user.click(screen.getByRole('button', { name: '2026, Choose year' }));
      expect(screen.getByRole('button', { name: '2025' })).toBeDisabled();
      expect(screen.getByRole('button', { name: '2027' })).toBeEnabled();
      expect(screen.getByRole('button', { name: 'Next years' })).toBeDisabled();
    });

    it('has no accessibility violations while a picker is open', async () => {
      const user = userEvent.setup();
      render(<MiniMonth {...base} />);
      await user.click(title());
      await expectNoA11yViolations();
      await user.click(screen.getByRole('button', { name: '2026, Choose year' }));
      await expectNoA11yViolations();
    });
  });
});
