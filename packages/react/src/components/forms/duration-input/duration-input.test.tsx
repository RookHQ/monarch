import { useState } from 'react';
import { describe, expect, it, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { Field, FieldLabel } from '../form-field/index.js';
import { DurationInput, formatDuration, parseDuration } from './index.js';

describe('parseDuration / formatDuration', () => {
  it.each([
    ['90', 90],
    ['90m', 90],
    ['45 min', 45],
    ['1h 30m', 90],
    ['1h30m', 90],
    ['1h30', 90],
    ['1.5h', 90],
    ['1,5h', 90],
    ['1:30', 90],
    ['2 hours 15 minutes', 135],
    ['1 hr', 60],
    ['1d 4h', 1680],
    ['  2H ', 120],
  ])('reads %s as %i minutes', (text, minutes) => {
    expect(parseDuration(text)).toBe(minutes);
  });

  it('returns null for empty text and NaN for unreadable text', () => {
    expect(parseDuration('   ')).toBeNull();
    for (const bad of ['abc', '1x', 'h', '1:75', '-5', '1h 30q'])
      expect(parseDuration(bad)).toBeNaN();
  });

  it('uses minutesPerDay for days', () => {
    expect(parseDuration('1d', { minutesPerDay: 480 })).toBe(480);
  });

  it('formats in h and m, or with days', () => {
    expect(formatDuration(0)).toBe('0m');
    expect(formatDuration(45)).toBe('45m');
    expect(formatDuration(90)).toBe('1h 30m');
    expect(formatDuration(120)).toBe('2h');
    expect(formatDuration(1560)).toBe('26h');
    expect(formatDuration(1560, { largestUnit: 'd' })).toBe('1d 2h');
    expect(formatDuration(600, { largestUnit: 'd', minutesPerDay: 480 })).toBe('1d 2h');
  });
});

describe('DurationInput', () => {
  it('commits typed text on blur and shows it back formatted', async () => {
    const onValueChange = vi.fn();
    render(
      <Field>
        <FieldLabel>Estimate</FieldLabel>
        <DurationInput onValueChange={onValueChange} />
      </Field>,
    );
    const input = screen.getByRole('spinbutton', { name: 'Estimate' });
    const user = userEvent.setup();
    await user.type(input, '1.5h');
    expect(onValueChange).not.toHaveBeenCalled();
    await user.tab();
    expect(onValueChange).toHaveBeenCalledWith(90);
    expect(input).toHaveValue('1h 30m');
    expect(input).toHaveAttribute('aria-valuenow', '90');
    expect(input).toHaveAttribute('aria-valuetext', '1 hour 30 minutes');
    await expectNoA11yViolations();
  });

  it('flags unreadable text, explains it, keeps it, and Escape restores the value', async () => {
    const onValueChange = vi.fn();
    render(<DurationInput aria-label="Estimate" defaultValue={30} onValueChange={onValueChange} />);
    const input = screen.getByRole('spinbutton');
    const user = userEvent.setup();
    await user.clear(input);
    await user.type(input, 'soon{Enter}');
    expect(onValueChange).not.toHaveBeenCalled();
    expect(input).toHaveAttribute('aria-invalid', 'true');
    expect(input).toHaveValue('soon');
    const message = screen.getByRole('alert');
    expect(message).toHaveTextContent('Use a duration like 1h 30m');
    expect(input.getAttribute('aria-describedby')).toContain(message.id);
    await expectNoA11yViolations();
    await user.keyboard('{Escape}');
    expect(input).toHaveValue('30m');
    expect(input).not.toHaveAttribute('aria-invalid');
  });

  it('rejects values outside min and max', async () => {
    render(<DurationInput aria-label="Break" min={15} max={120} />);
    const input = screen.getByRole('spinbutton');
    const user = userEvent.setup();
    await user.type(input, '3h{Enter}');
    expect(screen.getByRole('alert')).toHaveTextContent('Enter between 15m and 2h.');
    expect(input).toHaveAttribute('aria-valuemin', '15');
    expect(input).toHaveAttribute('aria-valuemax', '120');
  });

  it('steps with the arrow keys, snapping to the step, and clamps', async () => {
    const onValueChange = vi.fn();
    render(
      <DurationInput
        aria-label="Length"
        defaultValue={50}
        max={120}
        onValueChange={onValueChange}
      />,
    );
    const input = screen.getByRole('spinbutton');
    const user = userEvent.setup();
    input.focus();
    await user.keyboard('{ArrowUp}');
    expect(input).toHaveValue('1h');
    await user.keyboard('{ArrowDown}{ArrowDown}');
    expect(input).toHaveValue('30m');
    await user.keyboard('{PageUp}');
    expect(input).toHaveValue('1h'); // next whole hour
    await user.keyboard('{PageUp}{PageUp}');
    expect(input).toHaveValue('2h'); // clamped to max
    await user.keyboard('{Home}');
    expect(input).toHaveValue('0m');
    await user.keyboard('{ArrowDown}');
    expect(input).toHaveValue('0m'); // clamped to min
    await user.keyboard('{End}');
    expect(input).toHaveValue('2h');
    expect(onValueChange).toHaveBeenLastCalledWith(120);
  });

  it('clears to null and follows a controlled value', async () => {
    function Controlled() {
      const [v, setV] = useState<number | null>(60);
      return (
        <>
          <DurationInput aria-label="Duration" value={v} onValueChange={setV} />
          <button type="button" onClick={() => setV(25)}>
            Set 25
          </button>
          <output>{String(v)}</output>
        </>
      );
    }
    render(<Controlled />);
    const input = screen.getByRole('spinbutton');
    const user = userEvent.setup();
    await user.clear(input);
    await user.tab();
    expect(screen.getByRole('status')).toHaveTextContent('null');
    expect(input).not.toHaveAttribute('aria-valuenow');
    await user.click(screen.getByRole('button', { name: 'Set 25' }));
    expect(input).toHaveValue('25m');
  });

  it('accepts a custom format and ignores keys when read-only', async () => {
    render(
      <DurationInput
        aria-label="Duration"
        readOnly
        defaultValue={90}
        format={(m) => `${m} minutes`}
      />,
    );
    const input = screen.getByRole('spinbutton');
    expect(input).toHaveValue('90 minutes');
    const user = userEvent.setup();
    input.focus();
    await user.keyboard('{ArrowUp}');
    expect(input).toHaveValue('90 minutes');
  });

  it('renders on the server', () => {
    const html = renderToString(<DurationInput aria-label="Duration" defaultValue={75} />);
    expect(html).toContain('mn-duration-input');
    expect(html).toContain('1h 15m');
  });
});
