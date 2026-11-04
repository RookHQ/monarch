import { describe, expect, it, vi } from 'vitest';
import { useState } from 'react';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { Field, FieldDescription, FieldLabel } from '../../forms/form-field/index.js';
import { SecretField } from './index.js';

const input = () => document.querySelector('input') as HTMLInputElement;

function mockClipboard() {
  const spy = vi.fn(() => Promise.resolve());
  Object.defineProperty(navigator, 'clipboard', { configurable: true, value: { writeText: spy } });
  return spy;
}

describe('SecretField', () => {
  it('is masked by default and reveals with a pressed toggle', async () => {
    const user = userEvent.setup();
    render(<SecretField label="API key" defaultValue="sk_test_123" />);
    expect(input()).toHaveAttribute('type', 'password');
    const toggle = screen.getByRole('button', { name: 'Show API key' });
    expect(toggle).toHaveAttribute('aria-pressed', 'false');
    await user.click(toggle);
    expect(input()).toHaveAttribute('type', 'text');
    expect(toggle).toHaveAttribute('aria-pressed', 'true');
    // The name stays stable; aria-pressed carries the state (APG toggle button).
    expect(screen.getByRole('button', { name: 'Show API key' })).toBe(toggle);
    await user.click(toggle);
    expect(input()).toHaveAttribute('type', 'password');
  });

  it('toggles from the keyboard', async () => {
    const user = userEvent.setup();
    render(<SecretField label="Bot token" defaultValue="xoxb" />);
    await user.tab();
    expect(input()).toHaveFocus();
    await user.tab();
    const toggle = screen.getByRole('button', { name: 'Show Bot token' });
    expect(toggle).toHaveFocus();
    await user.keyboard(' ');
    expect(input()).toHaveAttribute('type', 'text');
    await user.keyboard('{Enter}');
    expect(input()).toHaveAttribute('type', 'password');
  });

  it('supports controlled reveal', async () => {
    const user = userEvent.setup();
    const onRevealedChange = vi.fn();
    const { rerender } = render(
      <SecretField
        label="Secret"
        defaultValue="x"
        revealed={false}
        onRevealedChange={onRevealedChange}
      />,
    );
    await user.click(screen.getByRole('button', { name: 'Show Secret' }));
    expect(onRevealedChange).toHaveBeenCalledWith(true);
    expect(input()).toHaveAttribute('type', 'password');
    rerender(
      <SecretField label="Secret" defaultValue="x" revealed onRevealedChange={onRevealedChange} />,
    );
    expect(input()).toHaveAttribute('type', 'text');
  });

  it('works as a controlled input', async () => {
    const user = userEvent.setup();
    function Demo() {
      const [v, setV] = useState('');
      return (
        <>
          <SecretField label="Signing secret" value={v} onChange={(e) => setV(e.target.value)} />
          <output>{v.length}</output>
        </>
      );
    }
    render(<Demo />);
    const toggle = screen.getByRole('button', { name: 'Show Signing secret' });
    expect(toggle).toBeDisabled();
    await user.type(input(), 'whsec');
    expect(document.querySelector('output')).toHaveTextContent('5');
    expect(toggle).toBeEnabled();
  });

  it('copies the current value without revealing it', async () => {
    const user = userEvent.setup();
    const writeText = mockClipboard();
    const onCopy = vi.fn();
    render(<SecretField label="API key" defaultValue="sk_live_abc" copyable onCopy={onCopy} />);
    await user.click(screen.getByRole('button', { name: 'Copy API key' }));
    expect(writeText).toHaveBeenCalledWith('sk_live_abc');
    expect(onCopy).toHaveBeenCalledWith('sk_live_abc');
    expect(input()).toHaveAttribute('type', 'password');
  });

  it('copies what the user typed in uncontrolled mode', async () => {
    const user = userEvent.setup();
    render(<SecretField label="Token" copyable />);
    await user.type(input(), 'typed');
    const writeText = mockClipboard();
    await user.click(screen.getByRole('button', { name: 'Copy Token' }));
    expect(writeText).toHaveBeenCalledWith('typed');
  });

  it('"set, not shown": empty, described, nothing to reveal or copy', async () => {
    const user = userEvent.setup();
    render(<SecretField label="Jira token" isSet copyable />);
    const box = input();
    expect(box).toHaveValue('');
    expect(box).toHaveAttribute('placeholder', 'Saved. Enter a new value to replace it');
    expect(box).toHaveAccessibleDescription('A value is saved and hidden. Leave blank to keep it.');
    expect(screen.getByRole('button', { name: 'Show Jira token' })).toBeDisabled();
    expect(screen.getByRole('button', { name: 'Copy Jira token' })).toBeDisabled();
    await user.type(box, 'new');
    expect(screen.getByRole('button', { name: 'Show Jira token' })).toBeEnabled();
    await expectNoA11yViolations();
  });

  it('keeps password managers and autocorrect away', () => {
    render(<SecretField label="Key" />);
    const box = input();
    expect(box).toHaveAttribute('autocomplete', 'off');
    expect(box).toHaveAttribute('spellcheck', 'false');
    expect(box).toHaveAttribute('autocapitalize', 'off');
    expect(box).toHaveAttribute('data-1p-ignore');
    expect(box).toHaveAttribute('data-lpignore', 'true');
  });

  it('inside a Field, uses the FieldLabel and merges descriptions', async () => {
    render(
      <Field invalid>
        <FieldLabel>Webhook secret</FieldLabel>
        <SecretField label="Webhook secret" isSet />
        <FieldDescription>Stored encrypted.</FieldDescription>
      </Field>,
    );
    const box = screen.getByLabelText('Webhook secret', { selector: 'input' });
    expect(box).not.toHaveAttribute('aria-label');
    expect(box).toHaveAttribute('aria-invalid', 'true');
    expect(box.getAttribute('aria-describedby')?.split(' ')).toHaveLength(2);
    await expectNoA11yViolations();
  });

  it('forwards the ref to the input and can hide the toggle', () => {
    let el: HTMLInputElement | null = null;
    render(
      <SecretField
        label="Key"
        revealable={false}
        ref={(n) => {
          el = n;
        }}
      />,
    );
    expect(el).toBe(input());
    expect(screen.queryByRole('button')).toBeNull();
  });

  it('renders on the server', () => {
    const html = renderToString(<SecretField label="API key" defaultValue="x" copyable />);
    expect(html).toContain('mn-secret-field');
    expect(html).toContain('type="password"');
  });
});
