import { describe, expect, it, vi } from 'vitest';
import { useState } from 'react';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { Field, FieldLabel } from '../../forms/form-field/index.js';
import { DateTimePicker, type DateTimePickerProps } from './index.js';

const base = { locale: 'en-GB', today: '2026-11-04' };
const day = (re: RegExp) =>
  screen.getByRole('button', { name: new RegExp(`^(\\p{L}+,? )?${re.source}`, 'u') });

function Labelled(props: DateTimePickerProps) {
  return (
    <Field>
      <FieldLabel>Interview</FieldLabel>
      <DateTimePicker {...base} {...props} />
    </Field>
  );
}

const dateInput = () => screen.getByRole('textbox', { name: 'Interview date' });
const timeInput = () => screen.getByRole('textbox', { name: 'Interview time' });

describe('DateTimePicker', () => {
  it('is a labelled group with a date part and a time part', async () => {
    render(<Labelled defaultValue="2026-11-10T14:30" timeZone="IST" />);
    expect(screen.getByRole('group', { name: 'Interview' })).toBeInTheDocument();
    expect(dateInput()).toHaveValue('10/11/2026');
    expect(timeInput()).toHaveValue('14:30');
    expect(screen.getByText('IST')).toBeInTheDocument();
    await expectNoA11yViolations();
  });

  it('shows a 12-hour clock for en-US, or the hourCycle prop', () => {
    const { unmount } = render(<Labelled locale="en-US" defaultValue="2026-11-10T14:30" />);
    expect(timeInput()).toHaveValue('2:30 PM');
    unmount();
    render(<Labelled hourCycle="12" defaultValue="2026-11-10T09:05" />);
    expect(timeInput()).toHaveValue('9:05 am');
  });

  it('accepts a typed time in either clock', async () => {
    const user = userEvent.setup();
    const onValueChange = vi.fn();
    render(<Labelled defaultValue="2026-11-10T09:00" onValueChange={onValueChange} />);
    await user.clear(timeInput());
    await user.type(timeInput(), '3:15 pm{Enter}');
    expect(onValueChange).toHaveBeenLastCalledWith('2026-11-10T15:15');
    expect(timeInput()).toHaveValue('15:15');
    await user.clear(timeInput());
    await user.type(timeInput(), '0830');
    await user.tab();
    expect(onValueChange).toHaveBeenLastCalledWith('2026-11-10T08:30');
  });

  it('steps the time with the arrow keys, snapping to the step', async () => {
    const user = userEvent.setup();
    const onValueChange = vi.fn();
    render(<Labelled defaultValue="2026-11-10T09:07" step={15} onValueChange={onValueChange} />);
    timeInput().focus();
    await user.keyboard('{ArrowUp}');
    expect(onValueChange).toHaveBeenLastCalledWith('2026-11-10T09:15');
    await user.keyboard('{ArrowUp}');
    expect(onValueChange).toHaveBeenLastCalledWith('2026-11-10T09:30');
    await user.keyboard('{ArrowDown}{ArrowDown}{ArrowDown}');
    expect(onValueChange).toHaveBeenLastCalledWith('2026-11-10T08:45');
  });

  it('picking a date first uses the default time', async () => {
    const user = userEvent.setup();
    const onValueChange = vi.fn();
    render(<Labelled defaultTime="10:00" onValueChange={onValueChange} />);
    await user.click(screen.getByRole('button', { name: 'Choose date' }));
    await user.click(day(/12 November 2026/));
    expect(onValueChange).toHaveBeenLastCalledWith('2026-11-12T10:00');
    expect(timeInput()).toHaveValue('10:00');
  });

  it('typing a time first uses today', async () => {
    const user = userEvent.setup();
    const onValueChange = vi.fn();
    render(<Labelled onValueChange={onValueChange} />);
    await user.type(timeInput(), '16:00{Enter}');
    expect(onValueChange).toHaveBeenLastCalledWith('2026-11-04T16:00');
  });

  it('flags an unreadable time without changing the value', async () => {
    const user = userEvent.setup();
    const onValueChange = vi.fn();
    const onValidationChange = vi.fn();
    render(
      <Labelled
        defaultValue="2026-11-10T09:00"
        onValueChange={onValueChange}
        onValidationChange={onValidationChange}
      />,
    );
    await user.clear(timeInput());
    await user.type(timeInput(), '25:00');
    await user.tab();
    expect(timeInput()).toHaveAttribute('aria-invalid', 'true');
    expect(timeInput()).toHaveAccessibleDescription('Enter a time like 14:30.');
    expect(onValueChange).not.toHaveBeenCalled();
    expect(onValidationChange).toHaveBeenLastCalledWith('invalid-time');
    await expectNoA11yViolations();
  });

  it('enforces min and max on the date and on the time of the boundary day', async () => {
    const user = userEvent.setup();
    const onValueChange = vi.fn();
    render(
      <Labelled
        min="2026-11-04T13:00"
        max="2026-11-30T17:00"
        defaultValue="2026-11-10T09:00"
        onValueChange={onValueChange}
      />,
    );
    // A date picked on the min day clamps the time up to the minimum.
    await user.click(screen.getByRole('button', { name: /^Change date/ }));
    expect(day(/3 November 2026/)).toHaveAttribute('aria-disabled', 'true');
    await user.click(day(/4 November 2026/));
    expect(onValueChange).toHaveBeenLastCalledWith('2026-11-04T13:00');
    await user.clear(timeInput());
    await user.type(timeInput(), '12:00{Enter}');
    expect(timeInput()).toHaveAccessibleDescription('Choose 13:00 or later.');
    await user.clear(dateInput());
    await user.type(dateInput(), '01/12/2026{Enter}');
    expect(dateInput()).toHaveAccessibleDescription('Choose 30 Nov 2026 or earlier.');
  });

  it('clears when the date is cleared', async () => {
    const user = userEvent.setup();
    const onValueChange = vi.fn();
    render(<Labelled defaultValue="2026-11-10T09:00" onValueChange={onValueChange} />);
    await user.click(screen.getByRole('button', { name: 'Clear date' }));
    expect(onValueChange).toHaveBeenLastCalledWith(null);
  });

  it('controlled value and form submission', async () => {
    const user = userEvent.setup();
    function Controlled() {
      const [value, setValue] = useState<string | null>('2026-11-10T09:00');
      return (
        <form aria-label="Schedule">
          <Labelled name="startsAt" value={value} onValueChange={setValue} />
          <button type="button" onClick={() => setValue('2026-12-01T18:45')}>
            Move
          </button>
        </form>
      );
    }
    const { container } = render(<Controlled />);
    await user.click(screen.getByRole('button', { name: 'Move' }));
    expect(dateInput()).toHaveValue('01/12/2026');
    expect(timeInput()).toHaveValue('18:45');
    expect(container.querySelector('input[name="startsAt"]')).toHaveValue('2026-12-01T18:45');
  });

  it('works without a Field through aria-label', () => {
    render(<DateTimePicker {...base} aria-label="Starts" />);
    expect(screen.getByRole('group', { name: 'Starts' })).toBeInTheDocument();
    expect(screen.getByRole('textbox', { name: 'date' })).toBeInTheDocument();
    expect(screen.getByRole('textbox', { name: 'time' })).toBeInTheDocument();
  });

  it('disabled', () => {
    render(<Labelled disabled defaultValue="2026-11-10T09:00" />);
    expect(dateInput()).toBeDisabled();
    expect(timeInput()).toBeDisabled();
  });

  it('renders on the server', () => {
    const html = renderToString(<Labelled defaultValue="2026-11-10T14:30" />);
    expect(html).toContain('mn-date-time-picker');
    expect(html).toContain('value="14:30"');
  });
});
