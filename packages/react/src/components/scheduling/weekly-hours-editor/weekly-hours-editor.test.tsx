import { describe, expect, it, vi } from 'vitest';
import { useState } from 'react';
import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { WeeklyHoursEditor, validateWeeklyHours, type WeeklyHoursInterval } from './index.js';

const NINE_TO_FIVE: WeeklyHoursInterval[] = [
  { day: 1, start: 540, end: 1020 },
  { day: 2, start: 540, end: 1020 },
];

describe('validateWeeklyHours', () => {
  it('reports range, order and overlap problems per day', () => {
    expect(validateWeeklyHours(NINE_TO_FIVE)).toEqual({});
    expect(
      validateWeeklyHours([
        { day: 1, start: 540, end: 720 },
        { day: 1, start: 700, end: 800 },
        { day: 2, start: 600, end: 600 },
        { day: 3, start: 0, end: 1500 },
        { day: 4, start: 540, end: 720 },
        { day: 4, start: 720, end: 1440 },
      ]),
    ).toEqual({ 1: 'overlap', 2: 'order', 3: 'range' });
  });
});

// Many user-event steps plus axe: allow for a loaded CI runner.
describe('WeeklyHoursEditor', { timeout: 15_000 }, () => {
  it('switches a day on with the default interval (uncontrolled)', async () => {
    const onValueChange = vi.fn();
    render(<WeeklyHoursEditor locale="en-GB" onValueChange={onValueChange} />);
    expect(screen.getByRole('group', { name: 'Weekly hours' })).toHaveClass(
      'mn-weekly-hours-editor',
    );
    const user = userEvent.setup();
    const monday = screen.getByRole('switch', { name: 'Monday' });
    expect(monday).toHaveAttribute('aria-checked', 'false');
    await user.click(monday);
    expect(monday).toHaveAttribute('aria-checked', 'true');
    expect(onValueChange).toHaveBeenLastCalledWith([{ day: 1, start: 540, end: 1020 }], {});
    expect(screen.getByRole('combobox', { name: 'Monday interval 1 start' })).toHaveValue('540');
    expect(screen.getByRole('combobox', { name: 'Monday interval 1 end' })).toHaveValue('1020');
    await expectNoA11yViolations();
  });

  it('adds, edits and removes intervals', async () => {
    const onValueChange = vi.fn();
    render(
      <WeeklyHoursEditor
        locale="en-GB"
        defaultValue={[{ day: 3, start: 540, end: 720 }]}
        onValueChange={onValueChange}
      />,
    );
    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: 'Add Wednesday interval' }));
    expect(onValueChange).toHaveBeenLastCalledWith(
      [
        { day: 3, start: 540, end: 720 },
        { day: 3, start: 780, end: 840 },
      ],
      {},
    );
    await user.selectOptions(
      screen.getByRole('combobox', { name: 'Wednesday interval 2 end' }),
      '1080',
    );
    expect(onValueChange.mock.lastCall?.[0]).toContainEqual({ day: 3, start: 780, end: 1080 });
    await user.click(screen.getByRole('button', { name: 'Remove Wednesday interval 1' }));
    expect(onValueChange).toHaveBeenLastCalledWith([{ day: 3, start: 780, end: 1080 }], {});
    expect(screen.queryByRole('combobox', { name: 'Wednesday interval 2 start' })).toBeNull();
  });

  it('flags overlapping intervals with an alert tied to the controls', async () => {
    const onValueChange = vi.fn();
    render(
      <WeeklyHoursEditor
        locale="en-GB"
        defaultValue={[
          { day: 1, start: 540, end: 720 },
          { day: 1, start: 780, end: 900 },
        ]}
        onValueChange={onValueChange}
      />,
    );
    const user = userEvent.setup();
    await user.selectOptions(
      screen.getByRole('combobox', { name: 'Monday interval 2 start' }),
      '660',
    );
    expect(onValueChange.mock.lastCall?.[1]).toEqual({ 1: 'overlap' });
    const alert = screen.getByRole('alert');
    expect(alert).toHaveTextContent('Intervals must not overlap');
    const start = screen.getByRole('combobox', { name: 'Monday interval 2 start' });
    expect(start).toHaveAttribute('aria-invalid', 'true');
    expect(start).toHaveAccessibleDescription('Intervals must not overlap');
    await expectNoA11yViolations();
  });

  it('copies a day to every day', async () => {
    const onValueChange = vi.fn();
    render(
      <WeeklyHoursEditor
        locale="en-GB"
        defaultValue={[
          { day: 2, start: 480, end: 720 },
          { day: 2, start: 780, end: 960 },
        ]}
        onValueChange={onValueChange}
      />,
    );
    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: 'Copy to all days from Tuesday' }));
    const value = onValueChange.mock.lastCall?.[0] as WeeklyHoursInterval[];
    expect(value).toHaveLength(14);
    expect(value.filter((r) => r.day === 7)).toEqual([
      { day: 7, start: 480, end: 720 },
      { day: 7, start: 780, end: 960 },
    ]);
    expect(
      screen.getAllByRole('switch').every((s) => s.getAttribute('aria-checked') === 'true'),
    ).toBe(true);
  });

  it('restores a day’s intervals when it is switched back on', async () => {
    render(
      <WeeklyHoursEditor
        locale="en-GB"
        defaultValue={[
          { day: 5, start: 600, end: 660 },
          { day: 5, start: 720, end: 780 },
        ]}
      />,
    );
    const user = userEvent.setup();
    const friday = screen.getByRole('switch', { name: 'Friday' });
    await user.click(friday);
    const row = friday.closest('li') as HTMLElement;
    expect(within(row).getByText('Unavailable')).toBeInTheDocument();
    await user.click(friday);
    expect(within(row).getAllByRole('combobox')).toHaveLength(4);
    expect(screen.getByRole('combobox', { name: 'Friday interval 2 start' })).toHaveValue('720');
  });

  it('is controlled: the value only changes when the parent passes a new one', async () => {
    function Controlled() {
      const [value, setValue] = useState<WeeklyHoursInterval[]>(NINE_TO_FIVE);
      return (
        <>
          <WeeklyHoursEditor locale="en-GB" value={value} onValueChange={setValue} name="hours" />
          <output data-testid="out">{value.length}</output>
        </>
      );
    }
    const { container } = render(<Controlled />);
    const user = userEvent.setup();
    await user.click(screen.getByRole('switch', { name: 'Tuesday' }));
    expect(screen.getByTestId('out')).toHaveTextContent('1');
    const hidden = container.querySelector(
      'input[type="hidden"][name="hours"]',
    ) as HTMLInputElement;
    expect(JSON.parse(hidden.value)).toEqual([{ day: 1, start: 540, end: 1020 }]);

    const onValueChange = vi.fn();
    render(
      <WeeklyHoursEditor locale="en-GB" value={[]} onValueChange={onValueChange} legend="Fixed" />,
    );
    const fixed = screen.getByRole('group', { name: 'Fixed' });
    await user.click(within(fixed).getByRole('switch', { name: 'Monday' }));
    expect(onValueChange).toHaveBeenCalledTimes(1);
    expect(within(fixed).getByRole('switch', { name: 'Monday' })).toHaveAttribute(
      'aria-checked',
      'false',
    );
  });

  it('orders days by weekStartsOn and moves through controls with Tab', async () => {
    render(
      <WeeklyHoursEditor
        locale="en-US"
        weekStartsOn={0}
        defaultValue={NINE_TO_FIVE}
        hourCycle="12"
      />,
    );
    expect(screen.getAllByRole('switch')).toHaveLength(7);
    const labels = Array.from(document.querySelectorAll('.mn-weekly-hours-editor__day-name')).map(
      (l) => l.textContent,
    );
    expect(labels[0]).toBe('Sunday');
    expect(labels[6]).toBe('Saturday');
    expect(screen.getByRole('combobox', { name: 'Monday interval 1 start' })).toHaveDisplayValue(
      '9:00 AM',
    );
    const user = userEvent.setup();
    await user.tab();
    expect(screen.getByRole('switch', { name: 'Sunday' })).toHaveFocus();
    await user.tab();
    expect(screen.getByRole('switch', { name: 'Monday' })).toHaveFocus();
    await user.tab();
    expect(screen.getByRole('combobox', { name: 'Monday interval 1 start' })).toHaveFocus();
  });

  it('shows text without controls when read-only, and disables everything when disabled', () => {
    render(
      <>
        <WeeklyHoursEditor
          locale="en-GB"
          legend="Support hours"
          readOnly
          defaultValue={NINE_TO_FIVE}
          timeZoneLabel="Times in Europe/London"
        />
        <WeeklyHoursEditor locale="en-GB" legend="Locked" disabled defaultValue={NINE_TO_FIVE} />
      </>,
    );
    const ro = screen.getByRole('group', { name: 'Support hours' });
    expect(within(ro).queryByRole('switch')).toBeNull();
    expect(within(ro).getAllByText('09:00 – 17:00')).toHaveLength(2);
    expect(within(ro).getByText('Times in Europe/London')).toBeInTheDocument();
    const locked = screen.getByRole('group', { name: 'Locked' });
    expect(within(locked).getByRole('switch', { name: 'Monday' })).toBeDisabled();
    expect(
      within(locked).getByRole('combobox', { name: 'Monday interval 1 start' }),
    ).toBeDisabled();
  });

  it('renders on the server', () => {
    const html = renderToString(<WeeklyHoursEditor locale="en-GB" defaultValue={NINE_TO_FIVE} />);
    expect(html).toContain('mn-weekly-hours-editor');
    expect(html).toContain('Monday');
  });
});
