import { describe, expect, it, vi } from 'vitest';
import { useState } from 'react';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { Field, FieldLabel } from '../../forms/form-field/index.js';
import { DateRangePicker, type DateRange, type DateRangePickerProps } from './index.js';

const base = { locale: 'en-GB', today: '2026-11-04', numberOfMonths: 1 as const };
const day = (re: RegExp) =>
  screen.getByRole('button', { name: new RegExp(`^(\\p{L}+,? )?${re.source}`, 'u') });

function Labelled(props: DateRangePickerProps) {
  return (
    <Field>
      <FieldLabel>Period</FieldLabel>
      <DateRangePicker {...base} {...props} />
    </Field>
  );
}
const startInput = () => screen.getByRole('textbox', { name: 'Period start date' });
const endInput = () => screen.getByRole('textbox', { name: 'Period end date' });

describe('DateRangePicker', () => {
  it('is a labelled group with start and end inputs', async () => {
    render(<Labelled defaultValue={{ start: '2026-11-02', end: '2026-11-08' }} />);
    expect(screen.getByRole('group', { name: 'Period' })).toBeInTheDocument();
    expect(startInput()).toHaveValue('02/11/2026');
    expect(endInput()).toHaveValue('08/11/2026');
    expect(
      screen.getByRole('button', { name: 'Change dates, 2 Nov 2026 to 8 Nov 2026' }),
    ).toHaveAttribute('aria-haspopup', 'dialog');
    await expectNoA11yViolations();
  });

  it('commits typed dates once both are valid', async () => {
    const user = userEvent.setup();
    const onValueChange = vi.fn();
    render(<Labelled onValueChange={onValueChange} />);
    await user.type(startInput(), '1/11/2026');
    await user.tab();
    expect(onValueChange).not.toHaveBeenCalled();
    await user.type(endInput(), '15/11/2026{Enter}');
    expect(onValueChange).toHaveBeenLastCalledWith({ start: '2026-11-01', end: '2026-11-15' });
  });

  it('flags an end before the start', async () => {
    const user = userEvent.setup();
    const onValueChange = vi.fn();
    const onValidationChange = vi.fn();
    render(<Labelled onValueChange={onValueChange} onValidationChange={onValidationChange} />);
    await user.type(startInput(), '10/11/2026');
    await user.tab();
    await user.type(endInput(), '5/11/2026{Enter}');
    expect(endInput()).toHaveAttribute('aria-invalid', 'true');
    expect(endInput()).toHaveAccessibleDescription('The end date is before the start date.');
    expect(onValueChange).not.toHaveBeenCalled();
    expect(onValidationChange).toHaveBeenLastCalledWith('end-before-start');
    await expectNoA11yViolations();
  });

  it('enforces minimum and maximum length for typed ranges', async () => {
    const user = userEvent.setup();
    render(<Labelled minDays={3} maxDays={7} />);
    await user.type(startInput(), '1/11/2026');
    await user.tab();
    await user.type(endInput(), '2/11/2026{Enter}');
    expect(screen.getByText('Choose at least 3 days.')).toBeInTheDocument();
    await user.clear(endInput());
    await user.type(endInput(), '20/11/2026{Enter}');
    expect(screen.getByText('Choose at most 7 days.')).toBeInTheDocument();
  });

  it('picks a range on the calendar with a hover preview, then closes', async () => {
    const user = userEvent.setup();
    const onValueChange = vi.fn();
    render(<Labelled onValueChange={onValueChange} />);
    await user.click(screen.getByRole('button', { name: 'Choose dates' }));
    expect(screen.getByRole('dialog', { name: 'Choose dates' })).toBeInTheDocument();
    expect(day(/4 November 2026/)).toHaveFocus();
    await user.click(day(/10 November 2026/));
    expect(screen.getByText('Start 10 Nov 2026. Now choose the end date.')).toBeInTheDocument();
    await user.hover(day(/13 November 2026/));
    expect(day(/12 November 2026/).closest('td')).toHaveAttribute('data-preview');
    expect(day(/14 November 2026/).closest('td')).not.toHaveAttribute('data-preview');
    await user.click(day(/13 November 2026/));
    expect(onValueChange).toHaveBeenLastCalledWith({ start: '2026-11-10', end: '2026-11-13' });
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
    expect(startInput()).toHaveFocus();
    expect(endInput()).toHaveValue('13/11/2026');
  });

  it('picks with the keyboard; an end before the anchor swaps them; the preview follows focus', async () => {
    const user = userEvent.setup();
    const onValueChange = vi.fn();
    render(<Labelled onValueChange={onValueChange} />);
    startInput().focus();
    await user.keyboard('{ArrowDown}');
    expect(day(/4 November 2026/)).toHaveFocus();
    await user.keyboard('{Enter}{ArrowLeft}{ArrowLeft}');
    expect(day(/3 November 2026/).closest('td')).toHaveAttribute('data-preview');
    await user.keyboard('{Enter}');
    expect(onValueChange).toHaveBeenLastCalledWith({ start: '2026-11-02', end: '2026-11-04' });
  });

  it('disables days that would break the length limits while picking', async () => {
    const user = userEvent.setup();
    render(<Labelled maxDays={5} />);
    await user.click(screen.getByRole('button', { name: 'Choose dates' }));
    await user.click(day(/10 November 2026/));
    expect(day(/14 November 2026/)).not.toHaveAttribute('aria-disabled');
    expect(day(/15 November 2026/)).toHaveAttribute('aria-disabled', 'true');
    expect(day(/5 November 2026/)).toHaveAttribute('aria-disabled', 'true');
  });

  it('offers presets, disabling those that break the limits', async () => {
    const user = userEvent.setup();
    const onValueChange = vi.fn();
    render(
      <Labelled
        maxDays={31}
        onValueChange={onValueChange}
        presets={[
          { label: 'Last 7 days', lastDays: 7 },
          { label: 'Next 14 days', nextDays: 14 },
          { label: 'Last month', months: -1 },
          { label: 'Last 90 days', lastDays: 90 },
          { label: 'Q4', range: { start: '2026-10-01', end: '2026-12-31' } },
        ]}
      />,
    );
    await user.click(screen.getByRole('button', { name: 'Choose dates' }));
    expect(screen.getByRole('group', { name: 'Quick picks' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Last 90 days' })).toBeDisabled();
    expect(screen.getByRole('button', { name: 'Q4' })).toBeDisabled();
    await user.click(screen.getByRole('button', { name: 'Last month' }));
    expect(onValueChange).toHaveBeenLastCalledWith({ start: '2026-10-01', end: '2026-10-31' });
    await user.click(screen.getByRole('button', { name: /^Change dates/ }));
    expect(screen.getByRole('button', { name: 'Last month' })).toHaveAttribute(
      'aria-pressed',
      'true',
    );
    await user.click(screen.getByRole('button', { name: 'Last 7 days' }));
    expect(onValueChange).toHaveBeenLastCalledWith({ start: '2026-10-29', end: '2026-11-04' });
    await user.click(screen.getByRole('button', { name: /^Change dates/ }));
    await user.click(screen.getByRole('button', { name: 'Next 14 days' }));
    expect(onValueChange).toHaveBeenLastCalledWith({ start: '2026-11-04', end: '2026-11-17' });
  });

  it('clips presets to min and max', async () => {
    const user = userEvent.setup();
    const onValueChange = vi.fn();
    render(
      <Labelled
        max="2026-11-04"
        onValueChange={onValueChange}
        presets={[{ label: 'This month', months: 0 }]}
      />,
    );
    await user.click(screen.getByRole('button', { name: 'Choose dates' }));
    await user.click(screen.getByRole('button', { name: 'This month' }));
    expect(onValueChange).toHaveBeenLastCalledWith({ start: '2026-11-01', end: '2026-11-04' });
  });

  it('respects min and max dates', async () => {
    const user = userEvent.setup();
    render(<Labelled min="2026-11-01" max="2026-11-30" />);
    await user.type(startInput(), '20/10/2026{Enter}');
    expect(screen.getByText('Choose 1 Nov 2026 or later.')).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Choose dates' }));
    expect(screen.getByRole('button', { name: 'Previous month' })).toBeDisabled();
    expect(screen.getByRole('button', { name: 'Next month' })).toBeDisabled();
  });

  it('shows two months when asked, with the range spanning them', async () => {
    const user = userEvent.setup();
    render(
      <Labelled numberOfMonths={2} defaultValue={{ start: '2026-11-25', end: '2026-12-05' }} />,
    );
    await user.click(screen.getByRole('button', { name: /^Change dates/ }));
    expect(screen.getByRole('grid', { name: 'November 2026' })).toBeInTheDocument();
    expect(screen.getByRole('grid', { name: 'December 2026' })).toBeInTheDocument();
    expect(day(/1 December 2026/).closest('td')).toHaveAttribute('aria-selected', 'true');
    await expectNoA11yViolations();
  });

  it('clears both dates', async () => {
    const user = userEvent.setup();
    const onValueChange = vi.fn();
    render(
      <Labelled
        defaultValue={{ start: '2026-11-02', end: '2026-11-08' }}
        onValueChange={onValueChange}
      />,
    );
    await user.click(screen.getByRole('button', { name: 'Clear dates' }));
    expect(onValueChange).toHaveBeenLastCalledWith(null);
    expect(startInput()).toHaveValue('');
    expect(endInput()).toHaveValue('');
    expect(startInput()).toHaveFocus();
  });

  it('controlled value and form fields', async () => {
    const user = userEvent.setup();
    function Controlled() {
      const [value, setValue] = useState<DateRange | null>(null);
      return (
        <>
          <Labelled value={value} onValueChange={setValue} startName="from" endName="to" />
          <button
            type="button"
            onClick={() => setValue({ start: '2027-01-01', end: '2027-01-31' })}
          >
            January
          </button>
        </>
      );
    }
    const { container } = render(<Controlled />);
    await user.click(screen.getByRole('button', { name: 'January' }));
    expect(startInput()).toHaveValue('01/01/2027');
    expect(container.querySelector('input[name="from"]')).toHaveValue('2027-01-01');
    expect(container.querySelector('input[name="to"]')).toHaveValue('2027-01-31');
  });

  it('disabled', () => {
    render(<Labelled disabled defaultValue={{ start: '2026-11-02', end: '2026-11-08' }} />);
    expect(startInput()).toBeDisabled();
    expect(endInput()).toBeDisabled();
    expect(screen.getByRole('button', { name: /^Change dates/ })).toBeDisabled();
  });

  it('renders on the server with one month', () => {
    const html = renderToString(
      <DateRangePicker
        locale="en-GB"
        aria-label="Period"
        defaultValue={{ start: '2026-11-02', end: '2026-11-08' }}
      />,
    );
    expect(html).toContain('mn-date-range-picker');
    expect(html).toContain('value="02/11/2026"');
  });
});
