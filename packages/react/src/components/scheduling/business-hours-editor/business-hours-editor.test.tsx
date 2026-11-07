import { describe, expect, it, vi } from 'vitest';
import { useState } from 'react';
import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import {
  BusinessHoursEditor,
  validateBusinessHours,
  type BusinessHoursEditorProps,
  type BusinessHoursValue,
} from './index.js';

const VALUE: BusinessHoursValue = {
  name: 'Support hours',
  timeZone: 'Europe/London',
  hours: [1, 2, 3, 4, 5].map((day) => ({ day, start: 540, end: 1020 })),
  exceptions: [{ date: '2026-12-25', name: 'Christmas Day' }],
};

function Editor(props: Partial<BusinessHoursEditorProps>) {
  return <BusinessHoursEditor defaultValue={VALUE} locale="en-GB" hourCycle="24" {...props} />;
}

describe('validateBusinessHours', () => {
  it('reports name, open, hours, exception and effective-date problems', () => {
    expect(validateBusinessHours(VALUE)).toEqual({});
    const bad: BusinessHoursValue = {
      timeZone: 'UTC',
      hours: [{ day: 1, start: 600, end: 540 }],
      exceptions: [
        { date: '', name: 'X' },
        { date: '2026-01-01', name: ' ' },
        { date: '2026-01-02', name: 'A', intervals: [{ start: 600, end: 500 }] },
        { date: '2026-01-02', name: 'B' },
      ],
      effectiveFrom: '2026-02-01',
      effectiveTo: '2026-01-01',
    };
    expect(validateBusinessHours(bad, { requireName: true, requireOpen: true })).toEqual({
      name: 'required',
      hours: { 1: 'order' },
      open: 'none',
      exceptions: { 0: 'date', 1: 'name', 2: 'hours', 3: 'duplicate' },
      effective: 'order',
    });
  });
});

describe('BusinessHoursEditor', () => {
  it('renders name, zone, weekly hours and exceptions with no axe violations', async () => {
    render(<Editor />);
    expect(screen.getByRole('textbox', { name: 'Name' })).toHaveValue('Support hours');
    expect(screen.getByRole('combobox', { name: /Time zone/ })).toBeInTheDocument();
    expect(screen.getByRole('group', { name: 'Weekly hours' })).toBeInTheDocument();
    const exceptions = screen.getByRole('group', { name: 'Holidays and exceptions' });
    expect(within(exceptions).getByRole('textbox', { name: 'Name of date 1' })).toHaveValue(
      'Christmas Day',
    );
    expect(within(exceptions).getByRole('combobox', { name: 'Hours on date 1' })).toHaveValue(
      'closed',
    );
    await expectNoA11yViolations();
  });

  it('adds, edits and removes exceptions, reporting value and errors', async () => {
    const onValueChange = vi.fn();
    render(<Editor onValueChange={onValueChange} />);
    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: 'Add date' }));
    const [first, errors] = onValueChange.mock.lastCall!;
    let value = first;
    expect(value.exceptions).toHaveLength(2);
    expect(errors.exceptions).toEqual({ 1: 'date' });
    await user.type(screen.getByRole('textbox', { name: 'Name of date 2' }), 'Closed');
    expect(screen.getByText('Choose a date.')).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Remove Closed' }));
    value = onValueChange.mock.lastCall![0];
    expect(value.exceptions).toEqual(VALUE.exceptions);
  });

  it('switches an exception to special hours and edits them', async () => {
    function Controlled() {
      const [v, setV] = useState(VALUE);
      return <Editor value={v} onValueChange={(next) => setV(next)} />;
    }
    render(<Controlled />);
    const user = userEvent.setup();
    await user.selectOptions(screen.getByRole('combobox', { name: 'Hours on date 1' }), 'open');
    const opens = screen.getByRole('combobox', { name: 'Date 1 opens' });
    expect(opens).toHaveValue('600');
    await user.selectOptions(screen.getByRole('combobox', { name: 'Date 1 closes' }), '540');
    expect(
      screen.getByText('The closing time must be after the opening time.'),
    ).toBeInTheDocument();
  });

  it('shows required name and no-open-hours errors with showErrors', () => {
    render(
      <Editor
        defaultValue={{ timeZone: 'UTC', hours: [], exceptions: [] }}
        validation={{ requireName: true, requireOpen: true }}
        showErrors
      />,
    );
    expect(screen.getByText('Enter a name.')).toBeInTheDocument();
    expect(screen.getByText('Open at least one interval.')).toBeInTheDocument();
    expect(screen.getByText('No holidays or exceptions.')).toBeInTheDocument();
  });

  it('shows effective dates and the default checkbox, and supports read-only', async () => {
    const onValueChange = vi.fn();
    const { rerender } = render(
      <Editor showEffectiveDates showDefault onValueChange={onValueChange} />,
    );
    expect(screen.getByRole('textbox', { name: 'Effective from' })).toBeInTheDocument();
    await userEvent.setup().click(screen.getByRole('checkbox', { name: /Default/ }));
    expect(onValueChange.mock.lastCall![0].isDefault).toBe(true);
    rerender(<Editor readOnly />);
    expect(screen.queryByRole('button', { name: 'Add date' })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /Remove/ })).not.toBeInTheDocument();
  });

  it('renders on the server', () => {
    expect(renderToString(<Editor />)).toContain('mn-business-hours-editor');
  });
});
