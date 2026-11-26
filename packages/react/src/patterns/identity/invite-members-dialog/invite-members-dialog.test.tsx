import { describe, expect, it, vi } from 'vitest';
import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { Button } from '../../../components/actions/button/index.js';
import type { AppAccessOption } from '../../../components/identity/app-access-picker/index.js';
import type { RoleOption } from '../../../components/identity/role-picker/index.js';
import { InviteMembersDialog } from './index.js';

const ROLES: RoleOption[] = [
  { value: 'member', label: 'Member', description: 'Can create and edit work.' },
  { value: 'admin', label: 'Admin', description: 'Can manage members and settings.' },
];
const APPS: AppAccessOption[] = [
  { id: 'notes', name: 'Notes' },
  { id: 'tasks', name: 'Tasks' },
];

function setup(props: Partial<Parameters<typeof InviteMembersDialog>[0]> = {}) {
  const onInvite = vi.fn<Parameters<typeof InviteMembersDialog>[0]['onInvite']>(() =>
    Promise.resolve(),
  );
  const utils = render(
    <InviteMembersDialog
      roles={ROLES}
      apps={APPS}
      allowMessage
      onInvite={onInvite}
      trigger={<Button>Invite</Button>}
      {...props}
    />,
  );
  return { onInvite, ...utils };
}

describe('InviteMembersDialog', () => {
  it('opens from its trigger with labelled fields and invites everyone', async () => {
    const user = userEvent.setup();
    const onInvited = vi.fn();
    const { onInvite } = setup({ onInvited });
    const trigger = screen.getByRole('button', { name: 'Invite' });
    await user.click(trigger);
    const dialog = await screen.findByRole('dialog', { name: 'Invite people' });
    expect(dialog).toHaveClass('mn-invite-members-dialog');
    const emails = within(dialog).getByRole('textbox', { name: /Email addresses/ });
    expect(emails).toHaveFocus();
    expect(within(dialog).getByRole('combobox', { name: 'Role' })).toHaveTextContent('Member');
    expect(within(dialog).getByRole('group', { name: 'App access' })).toBeInTheDocument();
    await expectNoA11yViolations();
    await user.type(emails, 'ana@example.com,bo@example.com ');
    await user.click(within(dialog).getByRole('checkbox', { name: /Notes/ }));
    await user.type(within(dialog).getByRole('textbox', { name: /Personal note/ }), ' Welcome! ');
    await user.click(within(dialog).getByRole('button', { name: 'Send 2 invites' }));
    expect(onInvite).toHaveBeenCalledWith({
      emails: ['ana@example.com', 'bo@example.com'],
      role: 'member',
      apps: ['notes'],
      message: 'Welcome!',
    });
    await waitFor(() => expect(screen.queryByRole('dialog')).toBeNull());
    expect(onInvited).toHaveBeenCalledWith(['ana@example.com', 'bo@example.com']);
    expect(screen.getByRole('status')).toHaveTextContent('Invited 2 people');
    await waitFor(() => expect(trigger).toHaveFocus());
  });

  it('splits a pasted list, rejects invalid addresses and requires one', async () => {
    const user = userEvent.setup();
    const { onInvite } = setup({ defaultOpen: true, apps: undefined });
    const emails = await screen.findByRole('textbox', { name: /Email addresses/ });
    await user.click(screen.getByRole('button', { name: 'Send invite' }));
    expect(onInvite).not.toHaveBeenCalled();
    expect(screen.getByText('Add at least one email address.')).toBeInTheDocument();
    expect(emails).toHaveAttribute('aria-invalid', 'true');
    await user.click(emails);
    await user.paste('a@example.com\nb@example.com, not-an-email');
    expect(screen.getByRole('button', { name: 'Remove a@example.com' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Remove b@example.com' })).toBeInTheDocument();
    expect(screen.getByText('“not-an-email” is not an email address.')).toBeInTheDocument();
  });

  it('keeps failed addresses with their reasons and clears the rest', async () => {
    const user = userEvent.setup();
    const onInvite = vi.fn(() =>
      Promise.resolve({ failed: [{ email: 'bo@example.com', message: 'Already a member' }] }),
    );
    setup({ defaultOpen: true, onInvite });
    const emails = await screen.findByRole('textbox', { name: /Email addresses/ });
    await user.type(emails, 'ana@example.com bo@example.com{Enter}');
    await user.click(screen.getByRole('button', { name: 'Send 2 invites' }));
    const alert = await screen.findByRole('alert');
    expect(alert).toHaveTextContent('Invited 1 person. 1 address couldn’t be invited:');
    expect(alert).toHaveTextContent('bo@example.com');
    expect(alert).toHaveTextContent('Already a member');
    await waitFor(() => expect(alert).toHaveFocus());
    expect(screen.queryByRole('button', { name: 'Remove ana@example.com' })).toBeNull();
    expect(screen.getByRole('button', { name: 'Remove bo@example.com' })).toBeInTheDocument();
    expect(screen.getByRole('dialog')).toBeInTheDocument();
  });

  it('shows pending and a rejection message, and changes the role', async () => {
    const user = userEvent.setup();
    let fail: (e: Error) => void = () => undefined;
    const onInvite = vi.fn(() => new Promise<void>((_, reject) => (fail = reject)));
    setup({ defaultOpen: true, onInvite, apps: undefined });
    await user.type(
      await screen.findByRole('textbox', { name: /Email addresses/ }),
      'ana@example.com',
    );
    const role = screen.getByRole('combobox', { name: 'Role' });
    role.focus();
    await user.keyboard('{Enter}');
    await user.click(await screen.findByRole('option', { name: /Admin/ }));
    await user.click(screen.getByRole('button', { name: 'Send invite' }));
    expect(onInvite).toHaveBeenCalledWith({ emails: ['ana@example.com'], role: 'admin', apps: [] });
    expect(screen.getByRole('button', { name: 'Send invite' })).toHaveAttribute(
      'aria-busy',
      'true',
    );
    expect(screen.getByRole('button', { name: 'Cancel' })).toBeDisabled();
    fail(new Error('Server unavailable'));
    expect(await screen.findByText('Server unavailable')).toBeInTheDocument();
    expect(screen.getByRole('dialog')).toBeInTheDocument();
  });

  it('accepts usernames with a custom validator and translated messages', async () => {
    const user = userEvent.setup();
    const { onInvite } = setup({
      defaultOpen: true,
      apps: undefined,
      validateEmail: () => null,
      messages: { title: 'Personen einladen', submit: () => 'Einladen' },
    });
    expect(await screen.findByRole('dialog', { name: 'Personen einladen' })).toBeInTheDocument();
    await user.type(screen.getByRole('textbox', { name: /Email addresses/ }), 'sam');
    await user.click(screen.getByRole('button', { name: 'Einladen' }));
    expect(onInvite).toHaveBeenCalledWith(expect.objectContaining({ emails: ['sam'] }));
  });

  it('renders on the server', () => {
    const html = renderToString(
      <InviteMembersDialog
        roles={ROLES}
        onInvite={() => undefined}
        trigger={<Button>Invite</Button>}
      />,
    );
    expect(html).toContain('Invite');
  });
});
