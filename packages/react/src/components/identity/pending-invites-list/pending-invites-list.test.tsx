import { describe, expect, it, vi } from 'vitest';
import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { PendingInvitesList, type PendingInvite } from './index.js';

const NOW = '2026-03-10T12:00:00Z';
const INVITES: PendingInvite[] = [
  {
    id: 'i1',
    email: 'ana@example.com',
    role: 'Member',
    invitedBy: 'Sam Rivera',
    sentAt: '2026-03-08T12:00:00Z',
    expiresAt: '2026-03-15T12:00:00Z',
  },
  {
    id: 'i2',
    email: 'bo@example.com',
    role: 'Admin',
    sentAt: '2026-02-20T12:00:00Z',
    expiresAt: '2026-02-27T12:00:00Z',
  },
];

describe('PendingInvitesList', () => {
  it('lists invitations with role, sender, times and expiry', async () => {
    render(
      <PendingInvitesList
        data-testid="root"
        invites={INVITES}
        now={NOW}
        locale="en"
        onResend={() => undefined}
        onRevoke={() => undefined}
      />,
    );
    expect(screen.getByTestId('root')).toHaveClass('mn-pending-invites-list');
    expect(screen.getByRole('heading', { name: 'Pending invitations' })).toBeInTheDocument();
    const rows = within(screen.getByRole('list', { name: 'Pending invitations' })).getAllByRole(
      'listitem',
    );
    expect(rows[0]).toHaveTextContent('ana@example.com');
    expect(rows[0]).toHaveTextContent('Member');
    expect(rows[0]).toHaveTextContent('Invited by Sam Rivera');
    expect(rows[0]).toHaveTextContent('Sent 2 days ago');
    expect(rows[0]).toHaveTextContent('Expires in 5 days');
    expect(rows[1]).toHaveAttribute('data-expired', 'true');
    expect(rows[1]).toHaveTextContent('Expired');
    expect(
      screen.getByRole('button', { name: 'Revoke invitation to bo@example.com' }),
    ).toBeInTheDocument();
    await expectNoA11yViolations();
  });

  it('resends with a pending state and announces it', async () => {
    const user = userEvent.setup();
    let finish: () => void = () => undefined;
    const onResend = vi.fn(() => new Promise<void>((resolve) => (finish = resolve)));
    render(<PendingInvitesList invites={INVITES} now={NOW} onResend={onResend} />);
    const button = screen.getByRole('button', { name: 'Resend invitation to ana@example.com' });
    await user.click(button);
    expect(onResend).toHaveBeenCalledWith('i1');
    await waitFor(() => expect(button).toHaveAttribute('aria-busy', 'true'));
    finish();
    await waitFor(() => expect(button).toHaveTextContent('Sent'));
    expect(screen.getByRole('status')).toHaveTextContent(
      'Invitation sent again to ana@example.com',
    );
  });

  it('shows a resend failure on the row', async () => {
    const user = userEvent.setup();
    render(
      <PendingInvitesList
        invites={INVITES}
        now={NOW}
        onResend={() => Promise.reject(new Error('Mailbox full'))}
      />,
    );
    await user.click(screen.getByRole('button', { name: 'Resend invitation to bo@example.com' }));
    expect(await screen.findByRole('alert')).toHaveTextContent('Mailbox full');
  });

  it('confirms before revoking, from the keyboard, and returns focus', async () => {
    const user = userEvent.setup();
    const onRevoke = vi.fn(() => Promise.resolve());
    render(<PendingInvitesList invites={INVITES} now={NOW} onRevoke={onRevoke} />);
    const button = screen.getByRole('button', { name: 'Revoke invitation to ana@example.com' });
    button.focus();
    await user.keyboard('{Enter}');
    const dialog = await screen.findByRole('alertdialog', {
      name: 'Revoke the invitation to ana@example.com?',
    });
    expect(within(dialog).getByRole('button', { name: 'Cancel' })).toHaveFocus();
    await user.keyboard('{Escape}');
    await waitFor(() => expect(screen.queryByRole('alertdialog')).toBeNull());
    expect(onRevoke).not.toHaveBeenCalled();
    await waitFor(() => expect(button).toHaveFocus());
    await user.keyboard('{Enter}');
    const again = await screen.findByRole('alertdialog');
    await user.click(within(again).getByRole('button', { name: 'Revoke invitation' }));
    expect(onRevoke).toHaveBeenCalledWith('i1');
    await waitFor(() =>
      expect(screen.getByRole('status')).toHaveTextContent('Invitation to ana@example.com revoked'),
    );
  });

  it('copies the invite link for live invitations only', async () => {
    const user = userEvent.setup();
    const getInviteLink = vi.fn(() => 'https://example.com/join/abc');
    render(<PendingInvitesList invites={INVITES} now={NOW} getInviteLink={getInviteLink} />);
    const copy = screen.getAllByRole('button', { name: /Copy link/ });
    expect(copy).toHaveLength(1);
    await user.click(copy[0] as HTMLElement);
    expect(getInviteLink).toHaveBeenCalledWith('i1');
  });

  it('shows loading, empty and error states, and translated messages', () => {
    const { rerender } = render(<PendingInvitesList invites={[]} state="loading" />);
    expect(screen.getByRole('region', { name: 'Pending invitations' })).toHaveAttribute(
      'aria-busy',
      'true',
    );
    rerender(<PendingInvitesList invites={[]} />);
    expect(screen.getByText('No pending invitations')).toBeInTheDocument();
    rerender(<PendingInvitesList invites={[]} state="error" onRetry={() => undefined} />);
    expect(screen.getByText('Couldn’t load invitations')).toBeInTheDocument();
    rerender(
      <PendingInvitesList invites={INVITES} now={NOW} messages={{ title: 'Offene Einladungen' }} />,
    );
    expect(screen.getByRole('heading', { name: 'Offene Einladungen' })).toBeInTheDocument();
  });

  it('renders on the server', () => {
    const html = renderToString(<PendingInvitesList invites={INVITES} now={NOW} locale="en" />);
    expect(html).toContain('mn-pending-invites-list');
    expect(html).toContain('ana@example.com');
  });
});
