import { describe, expect, it, vi } from 'vitest';
import { act, render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { Button } from '../../../components/actions/button/index.js';
import { CreateWorkspaceDialog } from './index.js';

async function fill(user: ReturnType<typeof userEvent.setup>) {
  await user.type(screen.getByRole('textbox', { name: /Workspace name/ }), 'Acme Inc');
  await user.type(screen.getByRole('textbox', { name: /Workspace username/ }), 'acmeinc');
  await user.click(screen.getByRole('radio', { name: 'Business' }));
}

describe('CreateWorkspaceDialog', () => {
  it('opens from its trigger as a named dialog with the form', async () => {
    const user = userEvent.setup();
    render(
      <CreateWorkspaceDialog
        trigger={<Button>New workspace</Button>}
        description="You can rename it later."
        onSubmit={() => undefined}
      />,
    );
    await user.click(screen.getByRole('button', { name: 'New workspace' }));
    const dialog = screen.getByRole('dialog', { name: 'Create new workspace' });
    expect(dialog).toHaveClass('mn-create-workspace-dialog');
    expect(dialog).toHaveAccessibleDescription('You can rename it later.');
    expect(within(dialog).getByRole('button', { name: 'Create workspace' })).toBeInTheDocument();
    await expectNoA11yViolations();
  });

  it('cannot be dismissed while creating, then closes and returns focus', async () => {
    const user = userEvent.setup();
    let resolve!: (value: { id: string }) => void;
    const onSubmit = vi.fn(() => new Promise<{ id: string }>((r) => (resolve = r)));
    const onSuccess = vi.fn();
    render(
      <CreateWorkspaceDialog
        trigger={<Button>New workspace</Button>}
        onSubmit={onSubmit}
        onSuccess={onSuccess}
      />,
    );
    const trigger = screen.getByRole('button', { name: 'New workspace' });
    await user.click(trigger);
    await fill(user);
    await user.click(screen.getByRole('button', { name: 'Create workspace' }));
    await waitFor(() => expect(onSubmit).toHaveBeenCalled());
    expect(screen.getByRole('dialog')).toHaveAttribute('aria-busy', 'true');
    expect(screen.getByRole('button', { name: 'Cancel' })).toBeDisabled();
    await user.keyboard('{Escape}');
    expect(screen.getByRole('dialog')).toBeInTheDocument();
    await act(async () => resolve({ id: 'w1' }));
    await waitFor(() => expect(screen.queryByRole('dialog')).toBeNull());
    expect(onSuccess).toHaveBeenCalledWith(
      { id: 'w1' },
      expect.objectContaining({ type: 'business' }),
    );
    await waitFor(() => expect(trigger).toHaveFocus());
  });

  it('stays open with the error when creation fails', async () => {
    const user = userEvent.setup();
    render(
      <CreateWorkspaceDialog
        defaultOpen
        onSubmit={() => Promise.reject(new Error('Limit reached'))}
      />,
    );
    await fill(user);
    await user.click(screen.getByRole('button', { name: 'Create workspace' }));
    expect(await screen.findByRole('alert')).toHaveTextContent('Limit reached');
    expect(screen.getByRole('dialog')).toBeInTheDocument();
  });

  it('is controlled and cancels with the Cancel button', async () => {
    const user = userEvent.setup();
    const onOpenChange = vi.fn();
    render(<CreateWorkspaceDialog open onOpenChange={onOpenChange} onSubmit={() => undefined} />);
    await user.click(screen.getByRole('button', { name: 'Cancel' }));
    expect(onOpenChange).toHaveBeenCalledWith(false);
  });

  it('takes translated messages for the dialog and the form', () => {
    render(
      <CreateWorkspaceDialog
        defaultOpen
        messages={{ title: 'Neuer Arbeitsbereich', submit: 'Erstellen' }}
        formMessages={{ nameLabel: 'Name' }}
        onSubmit={() => undefined}
      />,
    );
    expect(screen.getByRole('dialog', { name: 'Neuer Arbeitsbereich' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Erstellen' })).toBeInTheDocument();
    expect(screen.getByRole('textbox', { name: /^Name/ })).toBeInTheDocument();
  });

  it('renders its trigger on the server', () => {
    const html = renderToString(
      <CreateWorkspaceDialog trigger={<Button>New workspace</Button>} onSubmit={() => undefined} />,
    );
    expect(html).toContain('New workspace');
  });
});
