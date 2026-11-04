import { describe, expect, it, vi } from 'vitest';
import { useState } from 'react';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { Field, FieldError, FieldLabel } from '../form-field/index.js';
import { OTPPinInput } from './index.js';

const slots = () =>
  Array.from(document.querySelectorAll('.mn-otp-pin-input__slot')).map((n) => n.textContent);

describe('OTPPinInput', () => {
  it('is one labelled field with one-time-code autofill and a numeric keypad', async () => {
    render(<OTPPinInput aria-label="6-digit code" />);
    const input = screen.getByRole('textbox', { name: '6-digit code' });
    expect(input).toHaveAttribute('autocomplete', 'one-time-code');
    expect(input).toHaveAttribute('inputmode', 'numeric');
    expect(input).toHaveAttribute('pattern', '[0-9]*');
    expect(document.querySelectorAll('.mn-otp-pin-input__slot')).toHaveLength(6);
    expect(document.querySelector('.mn-otp-pin-input__slots')).toHaveAttribute(
      'aria-hidden',
      'true',
    );
    await expectNoA11yViolations();
  });

  it('fills slots as digits are typed, drops other characters and calls onComplete once', async () => {
    const onComplete = vi.fn();
    const onValueChange = vi.fn();
    render(<OTPPinInput aria-label="Code" onComplete={onComplete} onValueChange={onValueChange} />);
    const user = userEvent.setup();
    const input = screen.getByRole('textbox');
    await user.type(input, '12a3');
    expect(input).toHaveValue('123');
    expect(slots()).toEqual(['1', '2', '3', '', '', '']);
    expect(document.querySelector('[data-active]')).toBe(
      document.querySelectorAll('.mn-otp-pin-input__slot')[3],
    );
    await user.type(input, '4567');
    expect(input).toHaveValue('123456');
    expect(onComplete).toHaveBeenCalledTimes(1);
    expect(onComplete).toHaveBeenCalledWith('123456');
    expect(onValueChange).toHaveBeenLastCalledWith('123456');
    expect(input.closest('.mn-otp-pin-input')).toHaveAttribute('data-complete');
  });

  it('deletes with Backspace', async () => {
    render(<OTPPinInput aria-label="Code" defaultValue="1234" />);
    const user = userEvent.setup();
    await user.click(screen.getByRole('textbox'));
    await user.keyboard('{Backspace}{Backspace}');
    expect(screen.getByRole('textbox')).toHaveValue('12');
  });

  it('accepts a pasted code with spaces or dashes and replaces the value', async () => {
    const onComplete = vi.fn();
    render(<OTPPinInput aria-label="Code" defaultValue="99" onComplete={onComplete} />);
    const user = userEvent.setup();
    await user.click(screen.getByRole('textbox'));
    await user.paste('482 - 913');
    expect(screen.getByRole('textbox')).toHaveValue('482913');
    expect(onComplete).toHaveBeenCalledWith('482913');
  });

  it('appends a pasted fragment', async () => {
    render(<OTPPinInput aria-label="Code" defaultValue="12" />);
    const user = userEvent.setup();
    await user.click(screen.getByRole('textbox'));
    await user.paste('34');
    expect(screen.getByRole('textbox')).toHaveValue('1234');
  });

  it('supports other lengths, alphanumeric codes and groups', async () => {
    render(<OTPPinInput aria-label="Recovery code" length={8} mode="alphanumeric" groupSize={4} />);
    const user = userEvent.setup();
    const input = screen.getByRole('textbox');
    expect(input).toHaveAttribute('inputmode', 'text');
    await user.type(input, 'ab-cd12ef9');
    expect(input).toHaveValue('ABCD12EF');
    expect(document.querySelectorAll('.mn-otp-pin-input__slot')).toHaveLength(8);
    expect(document.querySelectorAll('.mn-otp-pin-input__separator')).toHaveLength(1);
  });

  it('masks a PIN with dots and a password input', async () => {
    render(<OTPPinInput aria-label="PIN" length={4} mask />);
    const user = userEvent.setup();
    const input = screen.getByLabelText('PIN');
    expect(input).toHaveAttribute('type', 'password');
    await user.type(input, '2580');
    expect(slots()).toEqual(['', '', '', '']);
    expect(document.querySelectorAll('.mn-otp-pin-input__dot')).toHaveLength(4);
  });

  it('works controlled and can be reset by the parent', async () => {
    function Controlled() {
      const [code, setCode] = useState('');
      return (
        <>
          <OTPPinInput aria-label="Code" value={code} onValueChange={setCode} />
          <button type="button" onClick={() => setCode('')}>
            Reset
          </button>
        </>
      );
    }
    render(<Controlled />);
    const user = userEvent.setup();
    await user.type(screen.getByRole('textbox'), '777');
    expect(slots().slice(0, 3)).toEqual(['7', '7', '7']);
    await user.click(screen.getByRole('button', { name: 'Reset' }));
    expect(screen.getByRole('textbox')).toHaveValue('');
  });

  it('reads Field context for its name, error and disabled state', async () => {
    render(
      <Field invalid>
        <FieldLabel>Verification code</FieldLabel>
        <OTPPinInput />
        <FieldError>That code has expired.</FieldError>
      </Field>,
    );
    const input = screen.getByRole('textbox', { name: 'Verification code' });
    expect(input).toHaveAttribute('aria-invalid', 'true');
    expect(input).toHaveAccessibleDescription('That code has expired.');
    expect(input.closest('.mn-otp-pin-input')).toHaveAttribute('data-invalid');
    await expectNoA11yViolations();

    render(<OTPPinInput aria-label="Locked" disabled />);
    expect(screen.getByRole('textbox', { name: 'Locked' })).toBeDisabled();
  });

  it('renders on the server', () => {
    const html = renderToString(<OTPPinInput aria-label="Code" defaultValue="12" />);
    expect(html).toContain('mn-otp-pin-input');
    expect(html).toContain('one-time-code');
  });
});
