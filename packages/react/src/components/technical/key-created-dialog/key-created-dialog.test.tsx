import { describe, expect, it, vi } from 'vitest';
import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { useState } from 'react';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { Button } from '../../actions/button/index.js';
import { KeyCreatedDialog, type KeyCreatedDialogProps } from './index.js';

function mockClipboard() {
  const spy = vi.fn(() => Promise.resolve());
  Object.defineProperty(navigator, 'clipboard', { configurable: true, value: { writeText: spy } });
  Object.defineProperty(document, 'execCommand', { configurable: true, value: () => false });
  return spy;
}

function Demo(props: Partial<KeyCreatedDialogProps>) {
  const [open, setOpen] = useState(false);
  return (
    <>
      <Button onClick={() => setOpen(true)}>Create key</Button>
      <KeyCreatedDialog
        open={open}
        onOpenChange={setOpen}
        secret="sk_live_51a9c0f2e7b4"
        name="Production server"
        {...props}
      />
    </>
  );
}

describe('KeyCreatedDialog', () => {
  it('shows the value, a warning and the acknowledgement, named by its title', async () => {
    const user = userEvent.setup();
    render(<Demo />);
    await user.click(screen.getByRole('button', { name: 'Create key' }));
    const dialog = screen.getByRole('dialog', { name: 'Copy your new key' });
    expect(dialog).toHaveClass('mn-key-created-dialog');
    expect(within(dialog).getByRole('textbox', { name: 'Production server' })).toHaveValue(
      'sk_live_51a9c0f2e7b4',
    );
    expect(
      within(dialog).getByText('Copy it now. It will not be shown again.'),
    ).toBeInTheDocument();
    expect(
      within(dialog).getByRole('checkbox', { name: 'I have saved this API key somewhere safe.' }),
    ).not.toBeChecked();
    // No close button until acknowledged.
    expect(within(dialog).queryByRole('button', { name: 'Close' })).toBeNull();
    await expectNoA11yViolations();
  });

  it('copies the value', async () => {
    const user = userEvent.setup();
    const writeText = mockClipboard();
    const onCopy = vi.fn();
    render(<Demo onCopy={onCopy} />);
    await user.click(screen.getByRole('button', { name: 'Create key' }));
    await user.click(screen.getByRole('button', { name: 'Copy API key' }));
    expect(writeText).toHaveBeenCalledWith('sk_live_51a9c0f2e7b4');
    expect(onCopy).toHaveBeenCalledWith('sk_live_51a9c0f2e7b4');
  });

  it('cannot be closed by Escape or Done until acknowledged, then closes and returns focus', async () => {
    const user = userEvent.setup();
    const onDone = vi.fn();
    render(<Demo onDone={onDone} />);
    const trigger = screen.getByRole('button', { name: 'Create key' });
    await user.click(trigger);
    await user.keyboard('{Escape}');
    expect(screen.getByRole('dialog')).toBeInTheDocument();

    const done = screen.getByRole('button', { name: 'Done' });
    expect(done).not.toHaveAttribute('data-ready');
    await user.click(done);
    const checkbox = screen.getByRole('checkbox');
    expect(checkbox).toHaveFocus();
    expect(
      screen
        .getAllByRole('status')
        .some(
          (n) => n.textContent === 'Confirm that you have saved it before you close this dialog.',
        ),
    ).toBe(true);
    expect(checkbox).toHaveAccessibleDescription(
      'Confirm that you have saved it before you close this dialog.',
    );
    expect(onDone).not.toHaveBeenCalled();

    await user.keyboard(' ');
    expect(checkbox).toBeChecked();
    expect(screen.getByRole('button', { name: 'Close' })).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Done' }));
    expect(screen.queryByRole('dialog')).toBeNull();
    expect(onDone).toHaveBeenCalledOnce();
    expect(trigger).toHaveFocus();
  });

  it('closes with Escape once acknowledged', async () => {
    const user = userEvent.setup();
    const onDone = vi.fn();
    render(<Demo onDone={onDone} />);
    await user.click(screen.getByRole('button', { name: 'Create key' }));
    await user.click(screen.getByRole('checkbox'));
    await user.keyboard('{Escape}');
    expect(screen.queryByRole('dialog')).toBeNull();
    expect(onDone).toHaveBeenCalledOnce();
  });

  it('resets the acknowledgement when reopened', async () => {
    const user = userEvent.setup();
    render(<Demo />);
    await user.click(screen.getByRole('button', { name: 'Create key' }));
    await user.click(screen.getByRole('checkbox'));
    await user.click(screen.getByRole('button', { name: 'Done' }));
    await user.click(screen.getByRole('button', { name: 'Create key' }));
    expect(screen.getByRole('checkbox')).not.toBeChecked();
  });

  it('a public value needs no acknowledgement', async () => {
    const user = userEvent.setup();
    render(<Demo kind="public" label="Publishable key" />);
    await user.click(screen.getByRole('button', { name: 'Create key' }));
    expect(screen.queryByRole('checkbox')).toBeNull();
    expect(screen.getByText('You can view this again later.')).toBeInTheDocument();
    await user.keyboard('{Escape}');
    expect(screen.queryByRole('dialog')).toBeNull();
  });

  it('masks the value behind a reveal toggle and takes custom strings', async () => {
    const user = userEvent.setup();
    render(
      <Demo
        masked
        label="Signing secret"
        title="Endpoint created"
        description="Use the secret to verify each request."
        messages={{ done: 'Finish' }}
      >
        <p>The old secret keeps working for 24 hours.</p>
      </Demo>,
    );
    await user.click(screen.getByRole('button', { name: 'Create key' }));
    const dialog = screen.getByRole('dialog', { name: 'Endpoint created' });
    expect(dialog).toHaveAccessibleDescription('Use the secret to verify each request.');
    const input = within(dialog).getByLabelText('Production server');
    expect(input).toHaveAttribute('type', 'password');
    await user.click(within(dialog).getByRole('button', { name: 'Show Signing secret' }));
    expect(input).toHaveAttribute('type', 'text');
    expect(
      within(dialog).getByText('The old secret keeps working for 24 hours.'),
    ).toBeInTheDocument();
    expect(within(dialog).getByRole('button', { name: 'Finish' })).toBeInTheDocument();
    await expectNoA11yViolations();
  });

  it('renders on the server', () => {
    expect(() => renderToString(<KeyCreatedDialog open secret="sk_test_1" />)).not.toThrow();
    expect(renderToString(<Demo />)).toContain('Create key');
  });
});
