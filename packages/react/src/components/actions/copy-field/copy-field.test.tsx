import { describe, expect, it, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { Field, FieldLabel } from '../../forms/form-field/index.js';
import { CopyField } from './index.js';

function mockClipboard(writeText: (text: string) => Promise<void>) {
  const spy = vi.fn(writeText);
  Object.defineProperty(navigator, 'clipboard', { configurable: true, value: { writeText: spy } });
  Object.defineProperty(document, 'execCommand', { configurable: true, value: () => false });
  return spy;
}

describe('CopyField', () => {
  it('shows the value read-only and names both controls from the label', async () => {
    render(<CopyField label="Webhook URL" value="https://example.com/hooks/abc" />);
    const box = screen.getByRole('textbox', { name: 'Webhook URL' });
    expect(box).toHaveValue('https://example.com/hooks/abc');
    expect(box).toHaveAttribute('readonly');
    expect(screen.getByRole('button', { name: 'Copy Webhook URL' })).toBeInTheDocument();
    await expectNoA11yViolations();
  });

  it('copies the value with the button', async () => {
    const user = userEvent.setup();
    const writeText = mockClipboard(() => Promise.resolve());
    const onCopy = vi.fn();
    render(
      <CopyField label="Record value" value="v=spf1 include:example.com ~all" onCopy={onCopy} />,
    );
    await user.click(screen.getByRole('button', { name: 'Copy Record value' }));
    expect(writeText).toHaveBeenCalledWith('v=spf1 include:example.com ~all');
    expect(onCopy).toHaveBeenCalledOnce();
    expect(screen.getByRole('button', { name: 'Copied Record value' })).toBeInTheDocument();
    expect(screen.getByRole('status')).toHaveTextContent('Copied to clipboard');
  });

  it('selects the whole value on focus, so it can be copied by hand', async () => {
    const user = userEvent.setup();
    render(<CopyField label="Token" value="abcdef" />);
    await user.tab();
    const box = screen.getByRole('textbox') as HTMLInputElement;
    expect(box).toHaveFocus();
    expect([box.selectionStart, box.selectionEnd]).toEqual([0, 6]);
    await user.tab();
    expect(screen.getByRole('button', { name: 'Copy Token' })).toHaveFocus();
  });

  it('on failure, focuses and selects the value and reports the error', async () => {
    const user = userEvent.setup();
    mockClipboard(() => Promise.reject(new Error('denied')));
    const onCopyError = vi.fn();
    render(
      <CopyField
        label="Feed link"
        value="webcal://example.com/feed.ics"
        onCopyError={onCopyError}
      />,
    );
    await user.click(screen.getByRole('button'));
    const box = screen.getByRole('textbox') as HTMLInputElement;
    expect(onCopyError).toHaveBeenCalledOnce();
    expect(box).toHaveFocus();
    expect(box.selectionEnd).toBe(box.value.length);
  });

  it('inside a Field, the FieldLabel names the text box', async () => {
    render(
      <Field>
        <FieldLabel>Embed code</FieldLabel>
        <CopyField label="Embed code" value="<script src=x></script>" multiline />
      </Field>,
    );
    const box = screen.getByRole('textbox', { name: 'Embed code' });
    expect(box.tagName).toBe('TEXTAREA');
    expect(box).not.toHaveAttribute('aria-label');
    await expectNoA11yViolations();
  });

  it('icon-only keeps a full accessible name', () => {
    render(<CopyField label="API key ID" value="key_123" iconOnly />);
    expect(screen.getByRole('button', { name: 'Copy API key ID' })).toHaveAttribute(
      'data-icon-only',
    );
  });

  it('disables both controls', () => {
    render(<CopyField label="Link" value="x" disabled />);
    expect(screen.getByRole('textbox')).toBeDisabled();
    expect(screen.getByRole('button')).toBeDisabled();
  });

  it('renders on the server', () => {
    const html = renderToString(<CopyField label="Link" value="https://example.com" />);
    expect(html).toContain('mn-copy-field');
    expect(html).toContain('readOnly');
  });
});
