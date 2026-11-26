import { afterEach, describe, expect, it, vi } from 'vitest';
import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { PasskeyManager, type Passkey } from './index.js';

const NOW = '2026-03-10T12:00:00Z';
const KEYS: Passkey[] = [
  {
    id: 'k1',
    name: 'Work laptop',
    createdAt: '2026-01-10T12:00:00Z',
    lastUsedAt: '2026-03-10T09:00:00Z',
    synced: true,
  },
  { id: 'k2', name: 'Security key', createdAt: '2026-03-01T12:00:00Z' },
];

afterEach(() => {
  delete (window as { PublicKeyCredential?: unknown }).PublicKeyCredential;
});

describe('PasskeyManager', () => {
  it('lists passkeys with times and a synced badge', async () => {
    render(
      <PasskeyManager
        data-testid="root"
        passkeys={KEYS}
        supported
        now={NOW}
        locale="en"
        onAdd={() => undefined}
        onRemove={() => undefined}
      />,
    );
    expect(screen.getByTestId('root')).toHaveClass('mn-passkey-manager');
    const rows = within(screen.getByRole('list', { name: 'Your passkeys' })).getAllByRole(
      'listitem',
    );
    expect(rows[0]).toHaveTextContent('Work laptop');
    expect(rows[0]).toHaveTextContent('Synced');
    expect(rows[0]).toHaveTextContent('Last used 3 hours ago');
    expect(rows[1]).toHaveTextContent('Never used');
    expect(screen.getByRole('button', { name: 'Add a passkey' })).toBeEnabled();
    await expectNoA11yViolations();
  });

  it('detects missing WebAuthn support after mount and disables Add', async () => {
    const { rerender } = render(<PasskeyManager passkeys={KEYS} onAdd={() => undefined} />);
    expect(await screen.findByText(/This browser can’t create passkeys/)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Add a passkey' })).toBeDisabled();
    (window as { PublicKeyCredential?: unknown }).PublicKeyCredential = function () {};
    rerender(<PasskeyManager passkeys={KEYS} onAdd={() => undefined} supported />);
    expect(screen.getByRole('button', { name: 'Add a passkey' })).toBeEnabled();
  });

  it('detects support from window.PublicKeyCredential', async () => {
    (window as { PublicKeyCredential?: unknown }).PublicKeyCredential = function () {};
    render(<PasskeyManager passkeys={KEYS} onAdd={() => undefined} />);
    await waitFor(() =>
      expect(screen.getByRole('list').closest('.mn-passkey-manager')).toHaveAttribute(
        'data-supported',
        'true',
      ),
    );
    expect(screen.queryByText(/can’t create passkeys/)).toBeNull();
  });

  it('adds with a pending state, and treats a cancelled prompt as neutral', async () => {
    const user = userEvent.setup();
    const cancelled = Object.assign(new Error('The operation was not allowed.'), {
      name: 'NotAllowedError',
    });
    const onAdd = vi
      .fn<() => Promise<void>>()
      .mockRejectedValueOnce(cancelled)
      .mockRejectedValueOnce(new Error('Server rejected the passkey'))
      .mockResolvedValueOnce();
    render(<PasskeyManager passkeys={KEYS} supported onAdd={onAdd} />);
    const add = screen.getByRole('button', { name: 'Add a passkey' });
    await user.click(add);
    expect(
      await screen.findByText('Passkey setup was cancelled. Nothing was changed.'),
    ).toBeInTheDocument();
    expect(screen.queryByRole('alert')).toBeNull();
    await user.click(add);
    expect(await screen.findByRole('alert')).toHaveTextContent('Server rejected the passkey');
    await user.click(add);
    await waitFor(() =>
      expect(screen.getAllByRole('status').at(-1)).toHaveTextContent('Passkey added'),
    );
    expect(onAdd).toHaveBeenCalledTimes(3);
  });

  it('renames in place with the keyboard', async () => {
    const user = userEvent.setup();
    const onRename = vi.fn(() => Promise.resolve());
    render(<PasskeyManager passkeys={KEYS} supported onRename={onRename} />);
    await user.click(screen.getByRole('button', { name: /Name of Work laptop/ }));
    const input = screen.getByRole('textbox', { name: 'Name of Work laptop' });
    await user.clear(input);
    await user.type(input, 'Studio laptop{Enter}');
    expect(onRename).toHaveBeenCalledWith('k1', 'Studio laptop');
  });

  it('explains an empty name with its own message, not the field label', async () => {
    const user = userEvent.setup();
    const onRename = vi.fn();
    const { unmount } = render(<PasskeyManager passkeys={KEYS} supported onRename={onRename} />);
    await user.click(screen.getByRole('button', { name: /Name of Work laptop/ }));
    const input = screen.getByRole('textbox', { name: 'Name of Work laptop' });
    await user.clear(input);
    await user.keyboard('{Enter}');
    expect(await screen.findByText('Enter a name.')).toBeInTheDocument();
    expect(onRename).not.toHaveBeenCalled();
    unmount();
    render(
      <PasskeyManager
        passkeys={KEYS}
        supported
        onRename={onRename}
        messages={{ renameEmpty: 'Saisissez un nom.' }}
      />,
    );
    await user.click(screen.getByRole('button', { name: /Name of Work laptop/ }));
    await user.clear(screen.getByRole('textbox', { name: 'Name of Work laptop' }));
    await user.keyboard('{Enter}');
    expect(await screen.findByText('Saisissez un nom.')).toBeInTheDocument();
  });

  it('confirms before removing and warns when it is the last sign-in method', async () => {
    const user = userEvent.setup();
    const onRemove = vi.fn(() => Promise.resolve());
    render(
      <PasskeyManager
        passkeys={[KEYS[0] as Passkey]}
        supported
        lastSignInMethod
        onRemove={onRemove}
      />,
    );
    const button = screen.getByRole('button', { name: 'Remove Work laptop' });
    await user.click(button);
    const dialog = await screen.findByRole('alertdialog', { name: 'Remove Work laptop?' });
    expect(dialog).toHaveTextContent('This is your only way to sign in.');
    expect(within(dialog).getByRole('button', { name: 'Cancel' })).toHaveFocus();
    await user.click(within(dialog).getByRole('button', { name: 'Remove passkey' }));
    expect(onRemove).toHaveBeenCalledWith('k1');
    await waitFor(() => expect(screen.queryByRole('alertdialog')).toBeNull());
    expect(screen.getAllByRole('status').at(-1)).toHaveTextContent('Removed Work laptop');
  });

  it('shows the empty state and translated messages', () => {
    render(<PasskeyManager passkeys={[]} supported messages={{ emptyTitle: 'Keine Passkeys' }} />);
    expect(screen.getByText('Keine Passkeys')).toBeInTheDocument();
  });

  it('renders on the server without touching window', () => {
    const html = renderToString(
      <PasskeyManager passkeys={KEYS} now={NOW} locale="en" onAdd={() => undefined} />,
    );
    expect(html).toContain('mn-passkey-manager');
    expect(html).not.toContain('data-supported');
  });
});
