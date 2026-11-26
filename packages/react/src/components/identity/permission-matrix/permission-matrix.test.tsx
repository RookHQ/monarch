import { describe, expect, it, vi } from 'vitest';
import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import {
  PermissionMatrix,
  type PermissionMatrixPermission,
  type PermissionMatrixRole,
} from './index.js';

const ROLES: PermissionMatrixRole[] = [
  { id: 'member', label: 'Member' },
  { id: 'admin', label: 'Admin' },
  { id: 'owner', label: 'Owner', description: 'Has everything' },
];
const PERMISSIONS: PermissionMatrixPermission[] = [
  { id: 'view', label: 'View records', group: 'Records' },
  { id: 'edit', label: 'Edit records', group: 'Records' },
  {
    id: 'invite',
    label: 'Invite members',
    description: 'Send invitations by email',
    group: 'Members',
  },
];
const VALUE = {
  member: ['view'],
  admin: ['view', 'edit', 'invite'],
  owner: ['view', 'edit', 'invite'],
};
const ownerLocked = (roleId: string) =>
  roleId === 'owner' ? 'Owners always have every permission' : undefined;

describe('PermissionMatrix', () => {
  it('renders a grid with row, column and group headers', async () => {
    render(
      <PermissionMatrix
        data-testid="root"
        caption="Workspace roles"
        roles={ROLES}
        permissions={PERMISSIONS}
        defaultValue={VALUE}
        isLocked={ownerLocked}
      />,
    );
    expect(screen.getByTestId('root')).toHaveClass('mn-permission-matrix');
    const grid = screen.getByRole('grid', { name: 'Workspace roles' });
    expect(
      within(grid)
        .getAllByRole('columnheader')
        .map((h) => h.textContent),
    ).toEqual(['Permission', 'Member', 'Admin', 'OwnerHas everything']);
    expect(within(grid).getByRole('rowheader', { name: /Invite members/ })).toBeInTheDocument();
    expect(within(grid).getByText('Records')).toBeInTheDocument();
    const cell = screen.getByRole('checkbox', { name: 'Invite members for Member' });
    expect(cell).not.toBeChecked();
    expect(cell).toHaveAccessibleDescription('Send invitations by email');
    const locked = screen.getByRole('checkbox', { name: 'Edit records for Owner' });
    expect(locked).toHaveAttribute('aria-disabled', 'true');
    expect(locked).toHaveAccessibleDescription('Owners always have every permission');
    await expectNoA11yViolations();
  });

  it('uses one tab stop and moves with arrow, Home and End keys', async () => {
    const user = userEvent.setup();
    render(
      <>
        <PermissionMatrix
          aria-label="Roles"
          roles={ROLES}
          permissions={PERMISSIONS}
          defaultValue={VALUE}
        />
        <button type="button">After</button>
      </>,
    );
    await user.tab();
    expect(screen.getByRole('checkbox', { name: 'View records for Member' })).toHaveFocus();
    await user.keyboard('{ArrowRight}');
    expect(screen.getByRole('checkbox', { name: 'View records for Admin' })).toHaveFocus();
    await user.keyboard('{ArrowDown}{ArrowDown}');
    expect(screen.getByRole('checkbox', { name: 'Invite members for Admin' })).toHaveFocus();
    await user.keyboard('{ArrowDown}');
    expect(screen.getByRole('checkbox', { name: 'Invite members for Admin' })).toHaveFocus();
    await user.keyboard('{Home}');
    expect(screen.getByRole('checkbox', { name: 'Invite members for Member' })).toHaveFocus();
    await user.keyboard('{End}');
    expect(screen.getByRole('checkbox', { name: 'Invite members for Owner' })).toHaveFocus();
    await user.keyboard('{Control>}{Home}{/Control}');
    expect(screen.getByRole('checkbox', { name: 'View records for Member' })).toHaveFocus();
    await user.tab();
    expect(screen.getByRole('button', { name: 'After' })).toHaveFocus();
    await user.tab({ shift: true });
    expect(screen.getByRole('checkbox', { name: 'View records for Member' })).toHaveFocus();
  });

  it('toggles with Space and reports the change', async () => {
    const user = userEvent.setup();
    const onValueChange = vi.fn();
    render(
      <PermissionMatrix
        aria-label="Roles"
        roles={ROLES}
        permissions={PERMISSIONS}
        defaultValue={VALUE}
        onValueChange={onValueChange}
        isLocked={ownerLocked}
      />,
    );
    await user.tab();
    await user.keyboard('{ArrowDown} ');
    expect(onValueChange).toHaveBeenCalledWith(
      expect.objectContaining({ member: ['view', 'edit'] }),
      { roleId: 'member', permissionId: 'edit', granted: true },
    );
    expect(screen.getByRole('checkbox', { name: 'Edit records for Member' })).toBeChecked();
    await user.keyboard('{End} ');
    expect(onValueChange).toHaveBeenCalledTimes(1);
    expect(screen.getByRole('checkbox', { name: 'Edit records for Owner' })).toBeChecked();
  });

  it('is controlled', async () => {
    const user = userEvent.setup();
    const onValueChange = vi.fn();
    render(
      <PermissionMatrix
        aria-label="Roles"
        roles={ROLES}
        permissions={PERMISSIONS}
        value={VALUE}
        onValueChange={onValueChange}
      />,
    );
    await user.click(screen.getByRole('checkbox', { name: 'View records for Admin' }));
    expect(onValueChange.mock.calls[0]?.[1]).toEqual({
      roleId: 'admin',
      permissionId: 'view',
      granted: false,
    });
    expect(screen.getByRole('checkbox', { name: 'View records for Admin' })).toBeChecked();
  });

  it('shows read-only marks with text alternatives', async () => {
    render(
      <PermissionMatrix
        caption="Roles"
        readOnly
        roles={ROLES}
        permissions={PERMISSIONS}
        value={VALUE}
        messages={{ allowed: 'Erlaubt', notAllowed: 'Nicht erlaubt' }}
      />,
    );
    expect(screen.queryByRole('grid')).toBeNull();
    expect(screen.queryByRole('checkbox')).toBeNull();
    const table = screen.getByRole('table', { name: 'Roles' });
    const row = within(table).getByRole('row', { name: /Edit records/ });
    expect(row).toHaveTextContent('Nicht erlaubt');
    expect(row).toHaveTextContent('Erlaubt');
    await expectNoA11yViolations();
  });

  it('renders on the server', () => {
    const html = renderToString(
      <PermissionMatrix aria-label="Roles" roles={ROLES} permissions={PERMISSIONS} value={VALUE} />,
    );
    expect(html).toContain('mn-permission-matrix');
    expect(html).toContain('role="grid"');
  });
});
