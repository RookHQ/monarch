import { describe, expect, it, vi } from 'vitest';
import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { ShareDialog, type ShareDialogProps, type ShareLink, type SharePerson } from './index.js';

const ROLES = [
  { value: 'viewer', label: 'Viewer' },
  { value: 'editor', label: 'Editor' },
];

const PEOPLE: SharePerson[] = [
  { id: 'u1', name: 'Alex Morgan', email: 'alex@example.com', role: 'editor', isSelf: true },
  { id: 'u2', name: 'Sam Lee', email: 'sam@example.com', role: 'viewer' },
  { id: 'u3', name: 'Owner Person', role: 'editor', roleLocked: true, removeLocked: true },
];

const LINKS: ShareLink[] = [
  { id: 'l1', url: 'https://example.com/s/abc', label: '/s/abc', views: 3, hasPassword: true },
  { id: 'l2', url: 'https://example.com/s/old', label: '/s/old', status: 'revoked', views: 1 },
];

function setup(props: Partial<ShareDialogProps> = {}) {
  return render(<ShareDialog itemName="Quarterly plan" defaultOpen roles={ROLES} {...props} />);
}

describe('ShareDialog', () => {
  it('opens from its trigger with a named dialog and returns focus on close', async () => {
    const user = userEvent.setup();
    render(
      <ShareDialog
        itemName="Quarterly plan"
        trigger={<button type="button">Share</button>}
        description="People you add can open it."
        link="https://example.com/d/1"
      />,
    );
    const trigger = screen.getByRole('button', { name: 'Share' });
    await user.click(trigger);
    const dialog = screen.getByRole('dialog', { name: 'Share “Quarterly plan”' });
    expect(dialog).toHaveAccessibleDescription('People you add can open it.');
    expect(within(dialog).getByRole('textbox', { name: 'Link' })).toHaveValue(
      'https://example.com/d/1',
    );
    await expectNoA11yViolations();
    await user.keyboard('{Escape}');
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
    expect(trigger).toHaveFocus();
  });

  it('invites picked emails with the chosen role and announces it', async () => {
    const user = userEvent.setup();
    const onInvite = vi.fn().mockResolvedValue(undefined);
    setup({ onInvite, inviteOptions: [{ value: 'u9', label: 'Jordan Kim' }] });
    const invite = screen.getByRole('button', { name: 'Invite' });
    expect(invite).toBeDisabled();
    await user.selectOptions(
      screen.getByRole('combobox', { name: 'Role for new people' }),
      'editor',
    );
    const input = screen.getByRole('combobox', { name: 'Invite people' });
    await user.type(input, 'Jor');
    await user.click(await screen.findByRole('option', { name: /Jordan Kim/ }));
    await user.type(input, 'new@example.com');
    await user.click(await screen.findByRole('option', { name: /Invite new@example.com/ }));
    await user.click(invite);
    expect(onInvite).toHaveBeenCalledWith(['u9', 'new@example.com'], 'editor');
    await waitFor(() =>
      expect(screen.getByText('2 people invited.')).toHaveAttribute('role', 'status'),
    );
  });

  it('keeps the picks and shows the error when inviting fails', async () => {
    const user = userEvent.setup();
    const onInvite = vi.fn().mockRejectedValue(new Error('Invites are paused.'));
    setup({ onInvite, inviteOptions: [{ value: 'u9', label: 'Jordan Kim' }] });
    await user.type(screen.getByRole('combobox', { name: 'Invite people' }), 'Jor');
    await user.click(await screen.findByRole('option', { name: /Jordan Kim/ }));
    await user.click(screen.getByRole('button', { name: 'Invite' }));
    expect(await screen.findByText('Invites are paused.')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Invite' })).toBeEnabled();
  });

  it('lists people with roles, locks the owner and confirms removal', async () => {
    const user = userEvent.setup();
    const onRoleChange = vi.fn();
    const onRemovePerson = vi.fn();
    setup({ people: PEOPLE, onRoleChange, onRemovePerson });
    const list = screen.getByRole('list');
    expect(within(list).getAllByRole('listitem')).toHaveLength(3);
    expect(
      screen.queryByRole('combobox', { name: 'Role for Alex Morgan' }),
    ).not.toBeInTheDocument();
    expect(
      screen.queryByRole('combobox', { name: 'Role for Owner Person' }),
    ).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Remove Owner Person' })).not.toBeInTheDocument();
    await user.selectOptions(screen.getByRole('combobox', { name: 'Role for Sam Lee' }), 'editor');
    expect(onRoleChange).toHaveBeenCalledWith('u2', 'editor');
    await user.click(screen.getByRole('button', { name: 'Remove Sam Lee' }));
    const confirm = await screen.findByRole('alertdialog', { name: 'Remove Sam Lee?' });
    await user.click(within(confirm).getByRole('button', { name: 'Remove' }));
    await waitFor(() => expect(onRemovePerson).toHaveBeenCalledWith('u2'));
  });

  it('changes general access and restores it when the change fails', async () => {
    const user = userEvent.setup();
    const onAccessChange = vi
      .fn()
      .mockResolvedValueOnce(undefined)
      .mockRejectedValueOnce(new Error('Not allowed.'));
    setup({
      access: 'invited',
      accessOptions: [
        {
          value: 'invited',
          label: 'Only people invited',
          description: 'Only people above can open it.',
        },
        { value: 'workspace', label: 'Anyone in the workspace' },
      ],
      onAccessChange,
    });
    const select = screen.getByRole('combobox', { name: 'Who can open it' });
    expect(select).toHaveAccessibleDescription('Only people above can open it.');
    await user.selectOptions(select, 'workspace');
    expect(onAccessChange).toHaveBeenCalledWith('workspace');
    expect(await screen.findByText('General access: Anyone in the workspace.')).toBeInTheDocument();
    await user.selectOptions(select, 'invited');
    expect(await screen.findByText('Not allowed.')).toBeInTheDocument();
    expect(select).toHaveValue('workspace');
  });

  it('creates a link with a password and expiry, validating the password', async () => {
    const user = userEvent.setup();
    const onCreateLink = vi.fn().mockResolvedValue(undefined);
    setup({ links: [], onCreateLink });
    expect(screen.getByText('No share links yet.')).toBeInTheDocument();
    const password = screen.getByLabelText(/Password/);
    await user.type(password, 'ab');
    expect(screen.getByText('Use at least 4 characters, or leave it blank.')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Create link' })).toBeDisabled();
    await user.type(password, 'cd');
    await user.selectOptions(screen.getByRole('combobox', { name: 'Expires' }), '30');
    await user.click(screen.getByRole('button', { name: 'Create link' }));
    expect(onCreateLink).toHaveBeenCalledWith({ password: 'abcd', expiresInDays: 30 });
    await waitFor(() => expect(password).toHaveValue(''));
    expect(screen.getByText('Link created.')).toBeInTheDocument();
  });

  it('shows link details and confirms revoking an active link', async () => {
    const user = userEvent.setup();
    const onRevokeLink = vi.fn();
    setup({ links: LINKS, onRevokeLink });
    expect(screen.getByText('3 views · Password')).toBeInTheDocument();
    expect(screen.getByText('1 view · Revoked')).toBeInTheDocument();
    expect(screen.getAllByRole('button', { name: /^Revoke/ })).toHaveLength(1);
    expect(screen.getByRole('button', { name: 'Copy /s/abc' })).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Revoke /s/abc' }));
    const confirm = await screen.findByRole('alertdialog', { name: 'Revoke this link?' });
    await user.click(within(confirm).getByRole('button', { name: 'Revoke link' }));
    await waitFor(() => expect(onRevokeLink).toHaveBeenCalledWith('l1'));
  });

  it('passes axe with every section', async () => {
    setup({
      onInvite: () => {},
      people: PEOPLE,
      onRoleChange: () => {},
      access: 'invited',
      accessOptions: [{ value: 'invited', label: 'Only people invited' }],
      onAccessChange: () => {},
      link: 'https://example.com/d/1',
      links: LINKS,
      onCreateLink: () => {},
      onRevokeLink: () => {},
      messages: { invite: 'Send invite' },
    });
    expect(screen.getByRole('button', { name: 'Send invite' })).toBeInTheDocument();
    await expectNoA11yViolations();
  });

  it('renders on the server', () => {
    expect(() =>
      renderToString(
        <ShareDialog itemName="Plan" trigger={<button type="button">Share</button>} />,
      ),
    ).not.toThrow();
    expect(
      renderToString(
        <ShareDialog itemName="Plan" trigger={<button type="button">Share</button>} />,
      ),
    ).toContain('Share');
  });
});
