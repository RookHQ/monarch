import { describe, expect, it, vi } from 'vitest';
import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { RolePicker, type RoleOption } from './index.js';

const ROLES: RoleOption[] = [
  { value: 'viewer', label: 'Viewer', description: 'Can view everything, change nothing.' },
  {
    value: 'member',
    label: 'Member',
    description: 'Can create and edit their own work.',
    capabilities: ['Create projects', 'Comment'],
  },
  { value: 'admin', label: 'Admin', description: 'Can manage members and settings.' },
  {
    value: 'owner',
    label: 'Owner',
    description: 'Full control, including billing.',
    disabled: true,
    disabledReason: 'Only owners can make someone an owner.',
  },
];

describe('RolePicker', () => {
  it('shows every role as a described radio with keyboard arrows', async () => {
    const user = userEvent.setup();
    const onValueChange = vi.fn();
    render(<RolePicker roles={ROLES} defaultValue="member" onValueChange={onValueChange} />);
    const group = screen.getByRole('radiogroup', { name: 'Role' });
    expect(group).toHaveClass('mn-role-picker');
    const member = within(group).getByRole('radio', { name: 'Member' });
    expect(member).toBeChecked();
    expect(member).toHaveAccessibleDescription(
      'Can create and edit their own work. Create projects Comment',
    );
    const owner = within(group).getByRole('radio', { name: 'Owner' });
    expect(owner).toBeDisabled();
    expect(owner).toHaveAccessibleDescription(/Only owners can make someone an owner/);
    await expectNoA11yViolations();
    await user.tab();
    expect(member).toHaveFocus();
    await user.keyboard('{ArrowDown}');
    expect(within(group).getByRole('radio', { name: 'Admin' })).toBeChecked();
    expect(onValueChange).toHaveBeenLastCalledWith('admin');
  });

  it('is controlled and posts its value with a name', async () => {
    const user = userEvent.setup();
    const onValueChange = vi.fn();
    const { container } = render(
      <form>
        <RolePicker roles={ROLES} value="viewer" name="role" onValueChange={onValueChange} />
      </form>,
    );
    await user.click(screen.getByRole('radio', { name: 'Admin' }));
    expect(onValueChange).toHaveBeenCalledWith('admin');
    expect(screen.getByRole('radio', { name: 'Viewer' })).toBeChecked();
    const form = container.querySelector('form') as HTMLFormElement;
    expect(new FormData(form).get('role')).toBe('viewer');
  });

  it('compact layout opens a select with descriptions', async () => {
    const user = userEvent.setup();
    const onValueChange = vi.fn();
    render(
      <RolePicker
        roles={ROLES}
        layout="compact"
        aria-label="Role for Sam"
        defaultValue="viewer"
        onValueChange={onValueChange}
      />,
    );
    const trigger = screen.getByRole('combobox', { name: 'Role for Sam' });
    expect(trigger).toHaveTextContent('Viewer');
    await expectNoA11yViolations();
    trigger.focus();
    await user.keyboard('{Enter}');
    const listbox = await screen.findByRole('listbox');
    expect(listbox).toHaveTextContent('Can manage members and settings.');
    expect(within(listbox).getByRole('option', { name: /Owner/ })).toHaveAttribute(
      'aria-disabled',
      'true',
    );
    await user.click(within(listbox).getByRole('option', { name: /Admin/ }));
    expect(onValueChange).toHaveBeenCalledWith('admin');
    expect(trigger).toHaveTextContent('Admin');
  });

  it('labels, help text, invalid state and translated placeholder', () => {
    render(
      <RolePicker
        roles={ROLES}
        layout="compact"
        invalid
        description="Applies to every app."
        messages={{ label: 'Rolle', placeholder: 'Rolle wählen' }}
      />,
    );
    const trigger = screen.getByRole('combobox', { name: 'Rolle' });
    expect(trigger).toHaveTextContent('Rolle wählen');
    expect(trigger).toHaveAccessibleDescription('Applies to every app.');
    expect(trigger).toHaveAttribute('aria-invalid', 'true');
  });

  it('disables every role', () => {
    render(<RolePicker roles={ROLES} disabled />);
    for (const radio of screen.getAllByRole('radio')) expect(radio).toBeDisabled();
  });

  it('renders on the server', () => {
    expect(renderToString(<RolePicker roles={ROLES} defaultValue="admin" />)).toContain(
      'mn-role-picker',
    );
    expect(renderToString(<RolePicker roles={ROLES} layout="compact" />)).toContain(
      'Choose a role',
    );
  });
});
