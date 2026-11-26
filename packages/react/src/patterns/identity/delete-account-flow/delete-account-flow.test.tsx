import { describe, expect, it, vi } from 'vitest';
import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { Button } from '../../../components/actions/button/index.js';
import { DeleteAccountFlow, type DeleteAccountFlowProps, type OwnedWorkspace } from './index.js';

const EMAIL = 'sam@example.com';

const reasons = [
  { value: 'not-needed', label: 'I no longer need it' },
  { value: 'other-tool', label: 'I use another tool' },
];

const owned: OwnedWorkspace[] = [
  { id: 'w1', name: 'North studio', memberCount: 4 },
  { id: 'w2', name: 'Side project', memberCount: 1 },
];

function Flow(props: Partial<DeleteAccountFlowProps>) {
  return (
    <DeleteAccountFlow
      trigger={<Button variant="danger">Delete my account</Button>}
      confirmPhrase={EMAIL}
      onDelete={() => {}}
      {...props}
    />
  );
}

const flowDialog = () => screen.getByRole('dialog', { name: 'Delete account' });
const continueButton = () => within(flowDialog()).getByRole('button', { name: 'Continue' });
const confirmField = () => screen.getByRole('textbox', { name: new RegExp(EMAIL) });

describe('DeleteAccountFlow', () => {
  it('walks review, export, reason and confirm to the farewell screen', async () => {
    const user = userEvent.setup();
    const onDelete = vi.fn(() => Promise.resolve());
    const onExport = vi.fn();
    const onDone = vi.fn();
    render(<Flow onDelete={onDelete} onExport={onExport} reasons={reasons} onDone={onDone} />);
    await user.click(screen.getByRole('button', { name: 'Delete my account' }));
    const dialog = flowDialog();
    expect(dialog).toHaveClass('mn-delete-account-flow');
    expect(dialog).toHaveTextContent('Step 1 of 4');
    await expectNoA11yViolations(dialog);

    await user.click(continueButton());
    expect(screen.getByRole('heading', { name: 'Download your data' })).toHaveFocus();
    await user.click(screen.getByRole('button', { name: 'Export data' }));
    expect(onExport).toHaveBeenCalledOnce();
    expect(await screen.findByText('Export started.')).toBeInTheDocument();

    await user.click(continueButton());
    expect(screen.getByRole('heading', { name: 'Why are you leaving?' })).toHaveFocus();
    await user.click(screen.getByRole('combobox', { name: 'Reason' }));
    await user.click(await screen.findByRole('option', { name: 'I use another tool' }));
    await user.type(
      screen.getByRole('textbox', { name: 'Anything else you want to tell us?' }),
      '  Too many tabs.  ',
    );
    await user.click(continueButton());

    const action = within(dialog).getByRole('button', { name: 'Delete account' });
    expect(action).toBeDisabled();
    expect(action).toHaveAccessibleDescription('Type the confirmation text to continue.');
    await user.type(confirmField(), EMAIL);
    await user.click(action);
    expect(onDelete).toHaveBeenCalledWith({ reason: 'other-tool', details: 'Too many tabs.' });
    expect(await screen.findByRole('heading', { name: 'Account deleted' })).toHaveFocus();
    await user.click(screen.getByRole('button', { name: 'Done' }));
    await waitFor(() => expect(screen.queryByRole('dialog')).toBeNull());
    expect(onDone).toHaveBeenCalledOnce();
  });

  it('blocks while the person owns workspaces alone and offers an action for each', async () => {
    const user = userEvent.setup();
    const onResolveWorkspace = vi.fn();
    const { rerender } = render(
      <Flow defaultOpen ownedWorkspaces={owned} onResolveWorkspace={onResolveWorkspace} />,
    );
    expect(flowDialog()).toHaveAttribute('data-blocked', 'true');
    const list = screen.getByRole('list', { name: /only owner/ });
    expect(within(list).getAllByRole('listitem')).toHaveLength(2);
    expect(list).toHaveTextContent('4 members');
    expect(list).toHaveTextContent('1 member');
    expect(continueButton()).toBeDisabled();
    expect(continueButton()).toHaveAccessibleDescription(
      'Transfer or delete the workspaces you own first.',
    );
    await expectNoA11yViolations(flowDialog());
    await user.click(screen.getByRole('button', { name: 'Transfer or delete Side project' }));
    expect(onResolveWorkspace).toHaveBeenCalledWith(owned[1]);

    rerender(<Flow defaultOpen ownedWorkspaces={[]} onResolveWorkspace={onResolveWorkspace} />);
    expect(continueButton()).toBeEnabled();
  });

  it('renderWorkspaceAction replaces the default button', () => {
    render(
      <Flow
        defaultOpen
        ownedWorkspaces={owned}
        onResolveWorkspace={() => {}}
        renderWorkspaceAction={(ws) => <Button size="sm">Transfer {ws.name}</Button>}
      />,
    );
    expect(screen.getByRole('button', { name: 'Transfer North studio' })).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /Transfer or delete/ })).toBeNull();
  });

  it('skips the optional steps and sends no reason when none is given', async () => {
    const user = userEvent.setup();
    const onDelete = vi.fn();
    render(<Flow defaultOpen onDelete={onDelete} />);
    expect(flowDialog()).toHaveTextContent('Step 1 of 2');
    await user.click(continueButton());
    await user.type(confirmField(), ` ${EMAIL} `);
    await user.keyboard('{Enter}');
    expect(onDelete).toHaveBeenCalledWith({});
  });

  it('type-to-confirm is case-sensitive', async () => {
    const user = userEvent.setup();
    render(<Flow defaultOpen />);
    await user.click(continueButton());
    await user.type(confirmField(), EMAIL.toUpperCase());
    expect(within(flowDialog()).getByRole('button', { name: 'Delete account' })).toBeDisabled();
  });

  it('step-up: cancel stays and does not delete; success deletes', async () => {
    const user = userEvent.setup();
    const onDelete = vi.fn();
    const onVerify = vi.fn();
    render(<Flow defaultOpen onDelete={onDelete} stepUp={{ onVerify }} />);
    await user.click(continueButton());
    await user.type(confirmField(), EMAIL);
    const action = within(flowDialog()).getByRole('button', { name: 'Delete account' });
    await user.click(action);
    let stepUp = await screen.findByRole('dialog', { name: 'Confirm it’s you' });
    expect(stepUp).toHaveAccessibleDescription(
      'Deleting your account needs you to confirm your identity.',
    );
    await user.keyboard('{Escape}');
    await waitFor(() =>
      expect(screen.queryByRole('dialog', { name: 'Confirm it’s you' })).toBeNull(),
    );
    expect(onDelete).not.toHaveBeenCalled();
    expect(screen.getByRole('heading', { name: 'Confirm deletion' })).toBeInTheDocument();

    await user.click(action);
    stepUp = await screen.findByRole('dialog', { name: 'Confirm it’s you' });
    await user.type(within(stepUp).getByLabelText(/^Password/), 'secret');
    await user.click(within(stepUp).getByRole('button', { name: 'Confirm' }));
    expect(await screen.findByRole('heading', { name: 'Account deleted' })).toBeInTheDocument();
    expect(onVerify).toHaveBeenCalledOnce();
    expect(onDelete).toHaveBeenCalledOnce();
  });

  it('a failed delete shows the error and stays on the confirm step', async () => {
    const user = userEvent.setup();
    render(
      <Flow defaultOpen onDelete={() => Promise.reject(new Error('Try again in a minute.'))} />,
    );
    await user.click(continueButton());
    await user.type(confirmField(), EMAIL);
    await user.click(within(flowDialog()).getByRole('button', { name: 'Delete account' }));
    expect(await screen.findByText('Try again in a minute.')).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Confirm deletion' })).toBeInTheDocument();
  });

  it('takes every string from messages', () => {
    render(
      <Flow
        defaultOpen
        ownedWorkspaces={owned}
        messages={{ title: 'Konto löschen', memberCount: (_n, f) => `${f} Mitglieder` }}
      />,
    );
    expect(screen.getByRole('dialog', { name: 'Konto löschen' })).toHaveTextContent('4 Mitglieder');
  });

  it('renders on the server', () => {
    expect(() => renderToString(<Flow ownedWorkspaces={owned} reasons={reasons} />)).not.toThrow();
  });
});
