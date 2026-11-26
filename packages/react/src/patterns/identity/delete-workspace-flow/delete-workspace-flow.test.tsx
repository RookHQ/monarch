import { describe, expect, it, vi } from 'vitest';
import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { Button } from '../../../components/actions/button/index.js';
import { DeleteWorkspaceFlow, type DeleteWorkspaceFlowProps } from './index.js';

function Flow(props: Partial<DeleteWorkspaceFlowProps>) {
  return (
    <DeleteWorkspaceFlow
      trigger={<Button variant="danger">Delete this workspace</Button>}
      workspaceName="north-studio"
      impact={{ apps: 3, members: 12, records: 1840, subscriptions: 1, domains: 0 }}
      onDelete={() => {}}
      {...props}
    />
  );
}

const flowDialog = () => screen.getByRole('dialog', { name: 'Delete workspace' });
const continueButton = () => within(flowDialog()).getByRole('button', { name: 'Continue' });

describe('DeleteWorkspaceFlow', () => {
  it('walks impact, export, transfer and confirm to the done screen', async () => {
    const user = userEvent.setup();
    const onDelete = vi.fn(() => Promise.resolve());
    const onExport = vi.fn(() => Promise.resolve());
    const onDone = vi.fn();
    render(
      <Flow onDelete={onDelete} onExport={onExport} onTransferInstead={() => {}} onDone={onDone} />,
    );
    await user.click(screen.getByRole('button', { name: 'Delete this workspace' }));

    const dialog = flowDialog();
    expect(dialog).toHaveClass('mn-delete-workspace-flow');
    expect(dialog).toHaveTextContent('Step 1 of 4');
    expect(screen.getByText('apps')).toBeInTheDocument();
    expect(screen.getByText('members lose access')).toBeInTheDocument();
    expect(screen.getByText('active subscription is cancelled')).toBeInTheDocument();
    expect(screen.queryByText(/custom domain/)).toBeNull();
    await expectNoA11yViolations(dialog);

    await user.click(continueButton());
    expect(screen.getByRole('heading', { name: 'Keep a copy' })).toHaveFocus();
    await user.click(screen.getByRole('button', { name: 'Export data' }));
    expect(onExport).toHaveBeenCalledOnce();
    expect(await screen.findByText('Export started.')).toBeInTheDocument();

    await user.click(continueButton());
    expect(screen.getByRole('heading', { name: 'Transfer it instead?' })).toHaveFocus();
    await user.click(continueButton());

    const action = within(dialog).getByRole('button', { name: 'Delete workspace' });
    expect(action).toBeDisabled();
    expect(action).toHaveAttribute('data-variant', 'danger');
    expect(action).toHaveAccessibleDescription('Type the workspace name to continue.');
    await user.type(screen.getByRole('textbox', { name: /north-studio/ }), 'north-studio');
    expect(action).toBeEnabled();
    await user.click(action);

    expect(onDelete).toHaveBeenCalledOnce();
    expect(await screen.findByRole('heading', { name: 'Workspace deleted' })).toHaveFocus();
    expect(screen.getByText('north-studio and everything in it has been deleted.')).toBeVisible();
    expect(onDone).not.toHaveBeenCalled();
    await user.click(screen.getByRole('button', { name: 'Done' }));
    await waitFor(() => expect(screen.queryByRole('dialog')).toBeNull());
    expect(onDone).toHaveBeenCalledOnce();
  });

  it('omits the export and transfer steps when their callbacks are missing', async () => {
    render(<Flow defaultOpen />);
    expect(flowDialog()).toHaveTextContent('Step 1 of 2');
    const user = userEvent.setup();
    await user.click(continueButton());
    expect(screen.getByRole('heading', { name: 'Confirm deletion' })).toHaveFocus();
  });

  it('keeps Continue disabled while the impact loads, and shows the impact error', () => {
    const { rerender } = render(<Flow defaultOpen impactLoading />);
    expect(continueButton()).toBeDisabled();
    expect(screen.getByRole('status')).toHaveTextContent('Checking what will change');
    rerender(<Flow defaultOpen impactError={<p>Could not load the summary.</p>} />);
    expect(screen.getByText('Could not load the summary.')).toBeInTheDocument();
    expect(continueButton()).toBeEnabled();
  });

  it('transfer instead closes the flow without cancelling and calls back', async () => {
    const user = userEvent.setup();
    const onTransferInstead = vi.fn();
    const onCancel = vi.fn();
    const onOpenChange = vi.fn();
    render(
      <Flow
        defaultOpen
        onTransferInstead={onTransferInstead}
        onCancel={onCancel}
        onOpenChange={onOpenChange}
      />,
    );
    await user.click(continueButton());
    await user.click(screen.getByRole('button', { name: 'Transfer ownership instead' }));
    expect(onTransferInstead).toHaveBeenCalledOnce();
    expect(onOpenChange).toHaveBeenLastCalledWith(false);
    expect(onCancel).not.toHaveBeenCalled();
    await waitFor(() => expect(screen.queryByRole('dialog')).toBeNull());
  });

  it('step-up: cancelling stays on the confirm step and does not delete', async () => {
    const user = userEvent.setup();
    const onDelete = vi.fn();
    render(<Flow defaultOpen onDelete={onDelete} stepUp={{ onVerify: () => {} }} />);
    await user.click(continueButton());
    await user.type(screen.getByRole('textbox', { name: /north-studio/ }), 'north-studio');
    await user.click(within(flowDialog()).getByRole('button', { name: 'Delete workspace' }));
    const stepUp = await screen.findByRole('dialog', { name: 'Confirm it’s you' });
    expect(stepUp).toHaveAccessibleDescription(
      'Deleting a workspace needs you to confirm your identity.',
    );
    await user.click(within(stepUp).getByRole('button', { name: 'Cancel' }));
    await waitFor(() =>
      expect(screen.queryByRole('dialog', { name: 'Confirm it’s you' })).toBeNull(),
    );
    expect(onDelete).not.toHaveBeenCalled();
    expect(screen.getByRole('heading', { name: 'Confirm deletion' })).toBeInTheDocument();
    expect(screen.queryByRole('alert')).toBeNull();
  });

  it('step-up: success runs the delete', async () => {
    const user = userEvent.setup();
    const onDelete = vi.fn(() => Promise.resolve());
    const onVerify = vi.fn(() => Promise.resolve());
    render(<Flow defaultOpen onDelete={onDelete} stepUp={{ methods: ['password'], onVerify }} />);
    await user.click(continueButton());
    await user.type(screen.getByRole('textbox', { name: /north-studio/ }), 'north-studio');
    await user.click(within(flowDialog()).getByRole('button', { name: 'Delete workspace' }));
    const stepUp = await screen.findByRole('dialog', { name: 'Confirm it’s you' });
    await user.type(within(stepUp).getByLabelText(/^Password/), 'correct horse');
    await user.click(within(stepUp).getByRole('button', { name: 'Confirm' }));
    expect(onVerify).toHaveBeenCalledWith({ method: 'password', password: 'correct horse' });
    expect(await screen.findByRole('heading', { name: 'Workspace deleted' })).toBeInTheDocument();
    expect(onDelete).toHaveBeenCalledOnce();
  });

  it('a failed delete shows the error on the confirm step', async () => {
    const user = userEvent.setup();
    render(
      <Flow defaultOpen onDelete={() => Promise.reject(new Error('The workspace is locked.'))} />,
    );
    await user.click(continueButton());
    await user.type(screen.getByRole('textbox', { name: /north-studio/ }), 'north-studio');
    await user.keyboard('{Enter}');
    expect(await screen.findByText('The workspace is locked.')).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Confirm deletion' })).toBeInTheDocument();
  });

  it('scheduled deletion: grace period copy with a formatted date', async () => {
    const user = userEvent.setup();
    const date = new Date(2030, 0, 15);
    const formatted = new Intl.DateTimeFormat('en-GB', { dateStyle: 'long' }).format(date);
    render(<Flow defaultOpen gracePeriodDays={30} deletionDate={date} locale="en-GB" />);
    expect(flowDialog()).toHaveAttribute('data-scheduled', 'true');
    expect(
      screen.getByText(
        `north-studio will be scheduled for deletion in 30 days. You can cancel until ${formatted}; after that, this cannot be undone.`,
      ),
    ).toBeInTheDocument();
    await user.click(continueButton());
    await user.type(screen.getByRole('textbox', { name: /north-studio/ }), 'north-studio');
    await user.click(screen.getByRole('button', { name: 'Schedule deletion' }));
    expect(await screen.findByRole('heading', { name: 'Deletion scheduled' })).toBeInTheDocument();
    expect(screen.getByText(new RegExp(`will be deleted on ${formatted}`))).toBeInTheDocument();
  });

  it('every opening clears the typed name', async () => {
    const user = userEvent.setup();
    render(<Flow />);
    await user.click(screen.getByRole('button', { name: 'Delete this workspace' }));
    await user.click(continueButton());
    await user.type(screen.getByRole('textbox', { name: /north-studio/ }), 'north');
    await user.keyboard('{Escape}');
    await waitFor(() => expect(screen.queryByRole('dialog')).toBeNull());
    await user.click(screen.getByRole('button', { name: 'Delete this workspace' }));
    await user.click(continueButton());
    expect(screen.getByRole('textbox', { name: /north-studio/ })).toHaveValue('');
  });

  it('takes every string from messages', () => {
    render(
      <Flow
        defaultOpen
        messages={{ title: 'Arbeitsbereich löschen', apps: () => 'Apps' }}
        wizardMessages={{ continue: 'Weiter' }}
      />,
    );
    expect(screen.getByRole('dialog', { name: 'Arbeitsbereich löschen' })).toBeInTheDocument();
    expect(screen.getByText('Apps')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Weiter' })).toBeInTheDocument();
  });

  it('renders on the server', () => {
    expect(() => renderToString(<Flow />)).not.toThrow();
    expect(renderToString(<Flow defaultOpen />)).not.toContain('undefined');
  });
});
