import { describe, expect, it, vi } from 'vitest';
import { useState } from 'react';
import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { MemberList, MemberRow } from './index.js';

const ROLES = [
  { value: 'owner', label: 'Owner' },
  { value: 'admin', label: 'Admin' },
  { value: 'member', label: 'Member' },
];

describe('MemberRow', () => {
  it('shows identity, read-only role and markers inside a list', async () => {
    render(
      <MemberList aria-label="Members">
        <MemberRow
          name="Priya Raman"
          email="priya@example.com"
          meta="Joined 3 March"
          roleOptions={ROLES}
          memberRole="admin"
          badges={<span>Guest</span>}
          presence="online"
        />
        <MemberRow name="Sam Lee" email="sam@example.com" isSelf memberRole="Owner" />
      </MemberList>,
    );
    const list = screen.getByRole('list', { name: 'Members' });
    const [first, second] = within(list).getAllByRole('listitem') as [HTMLElement, HTMLElement];
    expect(first).toHaveAttribute('data-status', 'active');
    expect(first).toHaveTextContent('Priya Raman (Online)Guestpriya@example.comJoined 3 March');
    // No select without onRoleChange: the role label is text.
    expect(within(first).queryByRole('combobox')).not.toBeInTheDocument();
    expect(first).toHaveTextContent('Admin');
    expect(second).toHaveAttribute('data-self');
    expect(second).toHaveTextContent('Sam Lee(You)');
    expect(second).toHaveTextContent('Owner');
    await expectNoA11yViolations();
  });

  it('changes the role through a labelled select (uncontrolled)', async () => {
    const user = userEvent.setup();
    const onRoleChange = vi.fn();
    render(
      <MemberList>
        <MemberRow
          name="Priya Raman"
          roleOptions={ROLES}
          defaultMemberRole="member"
          onMemberRoleChange={onRoleChange}
        />
      </MemberList>,
    );
    const select = screen.getByRole('combobox', { name: 'Role for Priya Raman' });
    expect(select).toHaveValue('member');
    await user.selectOptions(select, 'admin');
    expect(onRoleChange).toHaveBeenCalledWith('admin');
    expect(select).toHaveValue('admin');
    await expectNoA11yViolations();
  });

  it('restores the previous role and shows the error when the change fails', async () => {
    const user = userEvent.setup();
    render(
      <MemberList>
        <MemberRow
          name="Priya Raman"
          roleOptions={ROLES}
          defaultMemberRole="member"
          onMemberRoleChange={() => Promise.reject(new Error('Only owners can make admins.'))}
        />
      </MemberList>,
    );
    const select = screen.getByRole('combobox');
    await user.selectOptions(select, 'admin');
    expect(await screen.findByRole('alert')).toHaveTextContent('Only owners can make admins.');
    expect(select).toHaveValue('member');
    expect(select).not.toBeDisabled();
  });

  it('follows a controlled role and is busy while the change runs', async () => {
    const user = userEvent.setup();
    let resolve!: () => void;
    function Demo() {
      const [role, setRole] = useState('member');
      return (
        <MemberList>
          <MemberRow
            name="Priya Raman"
            roleOptions={ROLES}
            memberRole={role}
            onMemberRoleChange={(next) =>
              new Promise<void>((r) => {
                resolve = () => {
                  setRole(next);
                  r();
                };
              })
            }
          />
        </MemberList>
      );
    }
    render(<Demo />);
    const select = screen.getByRole('combobox');
    await user.selectOptions(select, 'owner');
    // Controlled: the value waits for the product.
    expect(select).toHaveValue('member');
    expect(select).toBeDisabled();
    expect(select).toHaveAttribute('aria-busy', 'true');
    resolve();
    await waitFor(() => expect(select).toHaveValue('owner'));
    expect(select).not.toBeDisabled();
  });

  it('hides the role select and Remove on your own row by default', () => {
    render(
      <MemberList>
        <MemberRow
          name="Sam Lee"
          isSelf
          roleOptions={ROLES}
          memberRole="owner"
          onMemberRoleChange={vi.fn()}
          onRemove={vi.fn()}
        />
      </MemberList>,
    );
    expect(screen.queryByRole('combobox')).not.toBeInTheDocument();
    expect(screen.queryByRole('button')).not.toBeInTheDocument();
    expect(screen.getByText('Owner')).toBeInTheDocument();
  });

  it('resends an invite and announces it', async () => {
    const user = userEvent.setup();
    const onResend = vi.fn(() => Promise.resolve());
    render(
      <MemberList>
        <MemberRow
          name="ali@example.com"
          status="invited"
          onResend={onResend}
          meta="Expires in 6 days"
        />
      </MemberList>,
    );
    expect(screen.getByText('Invited')).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Resend invite, ali@example.com' }));
    expect(onResend).toHaveBeenCalledTimes(1);
    expect(await screen.findByText('Invite sent again.')).toHaveAttribute('role', 'status');
    await expectNoA11yViolations();
  });

  it('confirms before removing, by keyboard, and shows rejections in the dialog', async () => {
    const user = userEvent.setup();
    const onRemove = vi
      .fn()
      .mockRejectedValueOnce(new Error('Could not remove them.'))
      .mockResolvedValueOnce(undefined);
    render(
      <MemberList>
        <MemberRow
          name="Priya Raman"
          onRemove={onRemove}
          removeConsequences={['3 open tickets go back to the queue']}
        />
      </MemberList>,
    );
    const button = screen.getByRole('button', { name: 'Remove Priya Raman' });
    button.focus();
    await user.keyboard('{Enter}');
    const dialog = await screen.findByRole('alertdialog', { name: 'Remove Priya Raman?' });
    expect(dialog).toHaveTextContent('3 open tickets go back to the queue');
    expect(within(dialog).getByRole('button', { name: 'Cancel' })).toHaveFocus();
    await user.click(within(dialog).getByRole('button', { name: 'Remove' }));
    expect(await within(dialog).findByText('Could not remove them.')).toBeInTheDocument();
    await user.click(within(dialog).getByRole('button', { name: 'Remove' }));
    await waitFor(() => expect(screen.queryByRole('alertdialog')).not.toBeInTheDocument());
    expect(onRemove).toHaveBeenCalledTimes(2);
  });

  it('revokes invitations and marks suspended members', async () => {
    const user = userEvent.setup();
    const { rerender } = render(
      <MemberList>
        <MemberRow name="ali@example.com" status="invited" onRemove={vi.fn()} />
      </MemberList>,
    );
    await user.click(screen.getByRole('button', { name: 'Revoke invite for ali@example.com' }));
    expect(
      await screen.findByRole('alertdialog', { name: 'Revoke the invite for ali@example.com?' }),
    ).toBeInTheDocument();
    await user.keyboard('{Escape}');
    rerender(
      <MemberList>
        <MemberRow name="Jo Park" status="suspended" />
      </MemberList>,
    );
    expect(screen.getByRole('listitem')).toHaveAttribute('data-status', 'suspended');
    expect(screen.getByText('Suspended')).toBeInTheDocument();
  });

  it('disables every control, renders as a div, and translates messages', () => {
    render(
      <MemberRow
        as="div"
        name="Priya Raman"
        roleOptions={ROLES}
        memberRole="member"
        status="invited"
        onMemberRoleChange={vi.fn()}
        onResend={vi.fn()}
        onRemove={vi.fn()}
        disabled
        messages={{ invited: 'Eingeladen', roleLabel: (n) => `Rolle von ${n}` }}
      />,
    );
    expect(screen.getByRole('combobox', { name: 'Rolle von Priya Raman' })).toBeDisabled();
    expect(screen.getByText('Eingeladen')).toBeInTheDocument();
    for (const b of screen.getAllByRole('button')) expect(b).toBeDisabled();
    expect(document.querySelector('div.mn-member-row')).not.toBeNull();
  });

  it('renders on the server', () => {
    const html = renderToString(
      <MemberList>
        <MemberRow name="Priya Raman" email="priya@example.com" />
      </MemberList>,
    );
    expect(html).toContain('mn-member-row');
    expect(html).toContain('<ul');
  });
});
