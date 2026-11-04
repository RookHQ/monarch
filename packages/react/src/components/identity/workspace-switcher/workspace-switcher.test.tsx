import { describe, expect, it, vi } from 'vitest';
import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { WorkspaceSwitcher, type WorkspaceOption } from './index.js';

const workspaces: WorkspaceOption[] = [
  { id: 'w1', name: 'Acme', handle: 'acme' },
  { id: 'w2', name: 'Northwind', handle: 'northwind', keywords: 'nw' },
  { id: 'w3', name: 'Globex' },
];

describe('WorkspaceSwitcher', () => {
  it('names the trigger with the current workspace and shows its handle', async () => {
    render(<WorkspaceSwitcher workspaces={workspaces} defaultValue="w1" />);
    const trigger = screen.getByRole('button', { name: 'Workspace: Acme @acme' });
    expect(trigger).toHaveTextContent('@acme');
    await expectNoA11yViolations();
  });

  it('lists workspaces with the current one selected and switches', async () => {
    const user = userEvent.setup();
    const onValueChange = vi.fn();
    render(
      <WorkspaceSwitcher workspaces={workspaces} defaultValue="w1" onValueChange={onValueChange} />,
    );
    await user.click(screen.getByRole('button', { name: 'Workspace: Acme @acme' }));
    const dialog = await screen.findByRole('dialog', { name: 'Workspace' });
    const list = within(dialog).getByRole('listbox');
    expect(within(list).getByRole('option', { name: /Acme/ })).toHaveAttribute(
      'aria-selected',
      'true',
    );
    await expectNoA11yViolations();
    await user.click(within(list).getByRole('option', { name: /Globex/ }));
    expect(onValueChange).toHaveBeenCalledWith('w3');
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
    expect(screen.getByRole('button', { name: 'Workspace: Globex' })).toHaveFocus();
  });

  it('searches names, handles and keywords from the keyboard', async () => {
    const user = userEvent.setup();
    const onValueChange = vi.fn();
    render(
      <WorkspaceSwitcher workspaces={workspaces} defaultValue="w1" onValueChange={onValueChange} />,
    );
    screen.getByRole('button', { name: 'Workspace: Acme @acme' }).focus();
    await user.keyboard('{Enter}');
    await user.keyboard('nw');
    const list = await screen.findByRole('listbox');
    expect(within(list).getAllByRole('option')).toHaveLength(1);
    await user.keyboard('{ArrowDown}{Enter}');
    expect(onValueChange).toHaveBeenCalledWith('w2');
  });

  it('choosing the current workspace again changes nothing', async () => {
    const user = userEvent.setup();
    const onValueChange = vi.fn();
    render(<WorkspaceSwitcher workspaces={workspaces} value="w1" onValueChange={onValueChange} />);
    await user.click(screen.getByRole('button', { name: 'Workspace: Acme @acme' }));
    await user.click(await screen.findByRole('option', { name: /Acme/ }));
    expect(onValueChange).not.toHaveBeenCalled();
  });

  it('is controlled', async () => {
    const user = userEvent.setup();
    const onValueChange = vi.fn();
    const { rerender } = render(
      <WorkspaceSwitcher workspaces={workspaces} value="w1" onValueChange={onValueChange} />,
    );
    await user.click(screen.getByRole('button', { name: 'Workspace: Acme @acme' }));
    await user.click(await screen.findByRole('option', { name: /Northwind/ }));
    expect(onValueChange).toHaveBeenCalledWith('w2');
    expect(screen.getByRole('button', { name: 'Workspace: Acme @acme' })).toBeInTheDocument();
    rerender(<WorkspaceSwitcher workspaces={workspaces} value="w2" />);
    expect(
      screen.getByRole('button', { name: 'Workspace: Northwind @northwind' }),
    ).toBeInTheDocument();
  });

  it('Create workspace and All workspaces', async () => {
    const user = userEvent.setup();
    const onCreate = vi.fn();
    render(
      <WorkspaceSwitcher
        workspaces={workspaces}
        defaultValue="w1"
        onCreate={onCreate}
        manageHref="https://id.example.com/workspaces"
      />,
    );
    await user.click(screen.getByRole('button', { name: 'Workspace: Acme @acme' }));
    const dialog = await screen.findByRole('dialog');
    expect(
      within(dialog).getByRole('link', { name: 'All workspaces (opens in a new tab)' }),
    ).toHaveAttribute('href', 'https://id.example.com/workspaces');
    await user.click(within(dialog).getByRole('button', { name: 'Create workspace' }));
    expect(onCreate).toHaveBeenCalled();
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
  });

  it('shows a placeholder when nothing is chosen, and a create link', async () => {
    render(<WorkspaceSwitcher workspaces={[]} createHref="/new" />);
    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: 'Workspace: Choose a workspace' }));
    const dialog = await screen.findByRole('dialog');
    expect(within(dialog).getByText('No workspaces match.')).toBeInTheDocument();
    expect(within(dialog).getByRole('link', { name: 'Create workspace' })).toHaveAttribute(
      'href',
      '/new',
    );
  });

  it('collapsed: avatar only, with the name in a tooltip', async () => {
    render(<WorkspaceSwitcher workspaces={workspaces} defaultValue="w2" collapsed />);
    const trigger = screen.getByRole('button', { name: 'Workspace: Northwind @northwind' });
    expect(trigger).toHaveAttribute('data-collapsed');
    await userEvent.setup().hover(trigger);
    expect(await screen.findByRole('tooltip')).toHaveTextContent('Workspace: Northwind');
  });

  it('renders on the server', () => {
    expect(
      renderToString(<WorkspaceSwitcher workspaces={workspaces} defaultValue="w1" />),
    ).toContain('mn-workspace-switcher');
  });
});
