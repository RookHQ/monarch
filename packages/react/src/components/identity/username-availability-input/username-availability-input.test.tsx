import { describe, expect, it, vi } from 'vitest';
import { useState } from 'react';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { Field, FieldDescription, FieldLabel } from '../../forms/form-field/index.js';
import { UsernameAvailabilityInput, type AsyncValidator } from './index.js';

const taken = new Set(['admin', 'support']);
const check: AsyncValidator = async (value) => {
  await new Promise((r) => setTimeout(r, 5));
  if (value.length < 3) return { valid: false, message: 'Use at least 3 characters.' };
  return taken.has(value) ? { valid: false } : { valid: true };
};

const statusRegion = () =>
  document.querySelector('.mn-username-availability-input > [role="status"]');

describe('UsernameAvailabilityInput', () => {
  it('debounces the check, shows checking, then announces the result', async () => {
    const validate = vi.fn(check);
    // Wide enough that keystrokes on a loaded CI machine never fall outside it.
    render(<UsernameAvailabilityInput aria-label="Handle" validate={validate} debounce={300} />);
    const user = userEvent.setup();
    const input = screen.getByRole('textbox', { name: 'Handle' });
    await user.type(input, 'maya');
    expect(screen.getByText('Checking availability…')).toBeInTheDocument();
    // "Checking" is shown but not announced.
    expect(statusRegion()).toHaveTextContent('');
    await waitFor(() => expect(statusRegion()).toHaveTextContent('Available'));
    expect(validate).toHaveBeenCalledTimes(1);
    expect(validate.mock.calls[0]?.[0]).toBe('maya');
    expect(input).toHaveAccessibleDescription('Available');
    expect(input).not.toHaveAttribute('aria-invalid');
    expect(input.closest('.mn-username-availability-input')).toHaveAttribute(
      'data-status',
      'valid',
    );
    await expectNoA11yViolations();
  });

  it('marks unavailable values invalid with the default or the validator message', async () => {
    render(<UsernameAvailabilityInput aria-label="Handle" validate={check} debounce={0} />);
    const user = userEvent.setup();
    const input = screen.getByRole('textbox');
    await user.type(input, 'admin');
    await waitFor(() => expect(statusRegion()).toHaveTextContent('Not available'));
    expect(input).toHaveAttribute('aria-invalid', 'true');
    await user.clear(input);
    await user.type(input, 'ab');
    await waitFor(() => expect(statusRegion()).toHaveTextContent('Use at least 3 characters.'));
  });

  it('aborts stale checks so a late answer never wins', async () => {
    const signals: AbortSignal[] = [];
    const slowFirst: AsyncValidator = (value, signal) => {
      signals.push(signal);
      const delay = value === 'ab' ? 60 : 5;
      return new Promise((r) => setTimeout(() => r({ valid: value !== 'ab' }), delay));
    };
    render(<UsernameAvailabilityInput aria-label="Handle" validate={slowFirst} debounce={0} />);
    const user = userEvent.setup();
    const input = screen.getByRole('textbox');
    await user.type(input, 'ab');
    await new Promise((r) => setTimeout(r, 10));
    await user.type(input, 'c');
    await waitFor(() => expect(statusRegion()).toHaveTextContent('Available'));
    await new Promise((r) => setTimeout(r, 80));
    expect(statusRegion()).toHaveTextContent('Available');
    expect(signals.slice(0, -1).every((s) => s.aborted)).toBe(true);
  });

  it('shows an error with a retry when the check fails', async () => {
    let fail = true;
    const flaky: AsyncValidator = async () => {
      if (fail) throw new Error('network');
      return { valid: true };
    };
    render(<UsernameAvailabilityInput aria-label="Handle" validate={flaky} debounce={0} />);
    const user = userEvent.setup();
    await user.type(screen.getByRole('textbox'), 'lee');
    await waitFor(() => expect(statusRegion()).toHaveTextContent('Couldn’t check availability.'));
    fail = false;
    await user.click(screen.getByRole('button', { name: 'Try again' }));
    await waitFor(() => expect(statusRegion()).toHaveTextContent('Available'));
  });

  it('is idle when empty or when the validator returns null', async () => {
    const onStatusChange = vi.fn();
    const skipCurrent: AsyncValidator = (v) => (v === 'current' ? null : { valid: true });
    render(
      <UsernameAvailabilityInput
        aria-label="Handle"
        defaultValue="current"
        validate={skipCurrent}
        debounce={0}
        onStatusChange={onStatusChange}
      />,
    );
    await waitFor(() => expect(onStatusChange).toHaveBeenLastCalledWith('idle', undefined));
    expect(screen.getByRole('textbox')).not.toHaveAttribute('aria-describedby');
    const user = userEvent.setup();
    await user.type(screen.getByRole('textbox'), 'x');
    await waitFor(() => expect(onStatusChange).toHaveBeenLastCalledWith('valid', undefined));
    await user.clear(screen.getByRole('textbox'));
    await waitFor(() => expect(onStatusChange).toHaveBeenLastCalledWith('idle', undefined));
  });

  it('applies transform and works controlled', async () => {
    function Controlled() {
      const [v, setV] = useState('');
      return (
        <>
          <UsernameAvailabilityInput
            aria-label="Workspace address"
            value={v}
            onValueChange={setV}
            transform={(raw) => raw.toLowerCase().replace(/[^a-z0-9-]/g, '-')}
            validate={check}
            debounce={0}
          />
          <output>{v}</output>
        </>
      );
    }
    render(<Controlled />);
    const user = userEvent.setup();
    await user.type(screen.getByRole('textbox'), 'North Wind');
    expect(screen.getByRole('textbox')).toHaveValue('north-wind');
    expect(document.querySelector('output')).toHaveTextContent('north-wind');
  });

  it('includes the prefix and Field wiring in the description', async () => {
    render(
      <Field>
        <FieldLabel>Address</FieldLabel>
        <UsernameAvailabilityInput prefix="example.com/" validate={check} debounce={0} />
        <FieldDescription>Letters, numbers and hyphens.</FieldDescription>
      </Field>,
    );
    const input = screen.getByRole('textbox', { name: 'Address' });
    expect(input).toHaveAccessibleDescription('example.com/ Letters, numbers and hyphens.');
    await userEvent.setup().type(input, 'acme');
    await waitFor(() =>
      expect(input).toHaveAccessibleDescription(
        'example.com/ Available Letters, numbers and hyphens.',
      ),
    );
    await expectNoA11yViolations();
  });

  it('renders on the server', () => {
    const html = renderToString(
      <UsernameAvailabilityInput aria-label="Handle" defaultValue="a" validate={check} />,
    );
    expect(html).toContain('mn-username-availability-input');
    expect(html).toContain('data-status="idle"');
  });
});
