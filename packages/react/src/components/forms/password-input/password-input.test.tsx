import { useState } from 'react';
import { describe, expect, it, vi } from 'vitest';
import { fireEvent, render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { Field, FieldDescription, FieldLabel } from '../form-field/index.js';
import { PasswordInput } from './index.js';

describe('PasswordInput', () => {
  it('is a labelled password field with a reveal toggle', async () => {
    render(
      <Field>
        <FieldLabel>Password</FieldLabel>
        <PasswordInput />
        <FieldDescription>At least 8 characters.</FieldDescription>
      </Field>,
    );
    const input = screen.getByLabelText('Password');
    expect(input).toHaveAttribute('type', 'password');
    expect(input).toHaveAttribute('autocomplete', 'current-password');
    expect(input).toHaveAccessibleDescription('At least 8 characters.');
    const toggle = screen.getByRole('button', { name: 'Show password' });
    expect(toggle).toHaveAttribute('aria-pressed', 'false');
    expect(toggle).toHaveAttribute('aria-controls', input.id);
    await expectNoA11yViolations();
  });

  it('reveals and hides with the mouse and the keyboard, keeping the value', async () => {
    const onVisibleChange = vi.fn();
    render(<PasswordInput aria-label="Password" onVisibleChange={onVisibleChange} />);
    const input = screen.getByLabelText('Password');
    const user = userEvent.setup();
    await user.type(input, 'correct horse');
    await user.tab();
    const toggle = screen.getByRole('button', { name: 'Show password' });
    expect(toggle).toHaveFocus();
    await user.keyboard('{Enter}');
    expect(input).toHaveAttribute('type', 'text');
    expect(toggle).toHaveAttribute('aria-pressed', 'true');
    expect(input).toHaveValue('correct horse');
    expect(onVisibleChange).toHaveBeenLastCalledWith(true);
    await user.keyboard(' ');
    expect(input).toHaveAttribute('type', 'password');
    await user.click(toggle);
    expect(input).toHaveAttribute('type', 'text');
    expect(toggle).toHaveFocus();
    // The name never changes; aria-pressed carries the state.
    expect(toggle).toHaveAccessibleName('Show password');
    await expectNoA11yViolations();
  });

  it('maps purpose to autocomplete and lets an explicit value win', () => {
    const { rerender } = render(<PasswordInput aria-label="New password" purpose="new" />);
    expect(screen.getByLabelText('New password')).toHaveAttribute('autocomplete', 'new-password');
    rerender(<PasswordInput aria-label="New password" purpose="new" autoComplete="off" />);
    expect(screen.getByLabelText('New password')).toHaveAttribute('autocomplete', 'off');
  });

  it('can be controlled', async () => {
    function Controlled() {
      const [visible, setVisible] = useState(true);
      return (
        <>
          <PasswordInput aria-label="Secret" visible={visible} onVisibleChange={setVisible} />
          <output>{String(visible)}</output>
        </>
      );
    }
    render(<Controlled />);
    expect(screen.getByLabelText('Secret')).toHaveAttribute('type', 'text');
    await userEvent.setup().click(screen.getByRole('button', { name: 'Show password' }));
    expect(screen.getByRole('status')).toHaveTextContent('false');
    expect(screen.getByLabelText('Secret')).toHaveAttribute('type', 'password');
  });

  it('warns while Caps Lock is on and links the hint', () => {
    render(<PasswordInput aria-label="Password" />);
    const input = screen.getByLabelText('Password');
    // jsdom reads getModifierState('CapsLock') from the modifierCapsLock init flag.
    fireEvent.keyUp(input, { key: 'A', modifierCapsLock: true });
    const hint = screen.getByText('Caps Lock is on.');
    expect(input.getAttribute('aria-describedby')).toContain(hint.id);
    fireEvent.blur(input);
    expect(screen.queryByText('Caps Lock is on.')).toBeNull();
  });

  it('takes disabled and invalid from the Field and translated text', () => {
    render(
      <Field disabled invalid>
        <FieldLabel>Passwort</FieldLabel>
        <PasswordInput messages={{ show: 'Passwort anzeigen' }} />
      </Field>,
    );
    const input = screen.getByLabelText('Passwort');
    expect(input).toBeDisabled();
    expect(input).toHaveAttribute('aria-invalid', 'true');
    expect(screen.getByRole('button', { name: 'Passwort anzeigen' })).toBeDisabled();
  });

  it('renders on the server', () => {
    const html = renderToString(<PasswordInput aria-label="Password" purpose="new" />);
    expect(html).toContain('mn-password-input');
    expect(html).toContain('new-password');
  });
});
