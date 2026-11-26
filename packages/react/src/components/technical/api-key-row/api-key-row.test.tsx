import { describe, expect, it, vi } from 'vitest';
import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { ApiKeyRow, type ApiKeyRowProps } from './index.js';

const NOW = '2026-06-10T12:00:00Z';

function row(props: Partial<ApiKeyRowProps> = {}) {
  return (
    <ApiKeyRow
      name="Reporting job"
      prefix="sk_live_4f2a"
      createdAt="2026-06-07T12:00:00Z"
      lastUsedAt="2026-06-10T10:00:00Z"
      scopes={['items:read', 'items:write', 'reports:read', 'users:read', 'users:write']}
      now={NOW}
      locale="en-US"
      {...props}
    />
  );
}

describe('ApiKeyRow', () => {
  it('shows name, status, masked prefix, dates and scopes with accessible names', async () => {
    render(
      row({
        'data-testid': 'root',
        onRotate: () => {},
        onRevoke: () => {},
      } as Partial<ApiKeyRowProps>),
    );
    const root = screen.getByTestId('root');
    expect(root).toHaveClass('mn-api-key-row');
    expect(root).toHaveAttribute('data-status', 'active');
    expect(root).toHaveAccessibleName('Reporting job');
    expect(screen.getByText('Active')).toBeInTheDocument();
    expect(screen.getByLabelText('Key starting with sk_live_4f2a')).toHaveTextContent(
      'sk_live_4f2a',
    );
    expect(root).toHaveTextContent('3 days ago');
    expect(root).toHaveTextContent('2 hours ago');
    const scopes = screen.getByRole('list', { name: 'Scopes' });
    const items = within(scopes).getAllByRole('listitem');
    expect(items).toHaveLength(4);
    expect(items[3]).toHaveTextContent('+2');
    expect(items[3]).toHaveTextContent('users:read, users:write');
    expect(screen.getByRole('group', { name: 'Actions for Reporting job' })).toBeInTheDocument();
    await expectNoA11yViolations();
  });

  it('says never used, no scopes, and shows an expiry', () => {
    render(row({ lastUsedAt: null, scopes: [], expiresAt: '2026-06-17T12:00:00Z' }));
    expect(screen.getByText('Never used')).toBeInTheDocument();
    expect(screen.getByText('No scopes')).toBeInTheDocument();
    expect(screen.getByText('Expires')).toBeInTheDocument();
    expect(screen.queryByRole('group')).not.toBeInTheDocument();
  });

  it('confirms before rotating, keyboard only, and returns focus', async () => {
    const onRotate = vi.fn().mockResolvedValue(undefined);
    const user = userEvent.setup();
    render(row({ onRotate }));
    const button = screen.getByRole('button', { name: 'Rotate' });
    button.focus();
    await user.keyboard('{Enter}');
    const dialog = await screen.findByRole('alertdialog', { name: 'Rotate Reporting job?' });
    expect(within(dialog).getByRole('button', { name: 'Cancel' })).toHaveFocus();
    await user.keyboard('{Escape}');
    expect(onRotate).not.toHaveBeenCalled();
    await waitFor(() => expect(button).toHaveFocus());

    await user.click(button);
    await user.click(
      within(await screen.findByRole('alertdialog')).getByRole('button', { name: 'Rotate key' }),
    );
    await waitFor(() => expect(onRotate).toHaveBeenCalledTimes(1));
    await waitFor(() => expect(screen.queryByRole('alertdialog')).not.toBeInTheDocument());
  });

  it('revokes only after typing the phrase, and shows a failure in the dialog', async () => {
    const onRevoke = vi
      .fn()
      .mockRejectedValueOnce(new Error('Network down'))
      .mockResolvedValue(undefined);
    const user = userEvent.setup();
    render(row({ onRevoke, revokeConfirmPhrase: 'Reporting job' }));
    await user.click(screen.getByRole('button', { name: 'Revoke' }));
    const dialog = await screen.findByRole('alertdialog', { name: 'Revoke Reporting job?' });
    expect(dialog).toHaveTextContent('This cannot be undone.');
    await user.click(within(dialog).getByRole('button', { name: 'Revoke key' }));
    expect(onRevoke).not.toHaveBeenCalled();
    await user.type(within(dialog).getByRole('textbox'), 'Reporting job');
    await user.click(within(dialog).getByRole('button', { name: 'Revoke key' }));
    expect(await within(dialog).findByText('Network down')).toBeInTheDocument();
    await user.click(within(dialog).getByRole('button', { name: 'Revoke key' }));
    await waitFor(() => expect(onRevoke).toHaveBeenCalledTimes(2));
    await waitFor(() => expect(screen.queryByRole('alertdialog')).not.toBeInTheDocument());
  });

  it('hides actions for revoked keys and supports li and messages', () => {
    render(
      <ul>
        {row({
          as: 'li',
          status: 'revoked',
          onRevoke: () => {},
          messages: { status: { revoked: 'Widerrufen' } },
        })}
      </ul>,
    );
    expect(screen.getByRole('listitem', { name: 'Reporting job' })).toHaveAttribute(
      'data-status',
      'revoked',
    );
    expect(screen.getByText('Widerrufen')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Revoke' })).not.toBeInTheDocument();
  });

  it('renders on the server', () => {
    const html = renderToString(row({ onRotate: () => {} }));
    expect(html).toContain('mn-api-key-row');
    expect(html).toContain('sk_live_4f2a');
    expect(html).toContain('3 days ago');
  });
});
