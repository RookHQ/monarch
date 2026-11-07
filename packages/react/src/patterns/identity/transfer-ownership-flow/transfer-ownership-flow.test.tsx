import { describe, expect, it, vi } from 'vitest';
import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { Button } from '../../../components/actions/button/index.js';
import {
  TransferOwnershipFlow,
  type TransferOwnershipCandidate,
  type TransferOwnershipFlowProps,
} from './index.js';

const candidates: TransferOwnershipCandidate[] = [
  { id: 'me', name: 'Sam Rivera', email: 'sam@example.com', role: 'owner' },
  { id: 'u1', name: 'Ada Lovelace', email: 'ada@example.com', role: 'admin' },
  { id: 'u2', name: 'Grace Hopper', email: 'grace@example.com', role: 'member' },
  {
    id: 'u3',
    name: 'Alan Turing',
    email: 'alan@example.com',
    role: 'admin',
    ineligibleReason: 'Invite not accepted yet',
  },
];

function Flow(props: Partial<TransferOwnershipFlowProps>) {
  return (
    <TransferOwnershipFlow
      trigger={<Button>Transfer ownership</Button>}
      candidates={candidates}
      currentOwnerId="me"
      onTransfer={() => Promise.resolve()}
      {...props}
    />
  );
}

async function pick(user: ReturnType<typeof userEvent.setup>, name: string) {
  await user.click(screen.getByRole('combobox', { name: 'New owner' }));
  await user.click(await screen.findByRole('option', { name: new RegExp(name) }));
}

function primary(name: string) {
  return within(screen.getByRole('dialog')).getByRole('button', { name });
}

describe('TransferOwnershipFlow', () => {
  it('picks a member, reviews, confirms and shows the new owner', async () => {
    const user = userEvent.setup();
    const onTransfer = vi.fn(() => Promise.resolve());
    render(
      <Flow
        onTransfer={onTransfer}
        yourRoleOptions={[
          { value: 'admin', label: 'Admin' },
          { value: 'member', label: 'Member' },
        ]}
      />,
    );
    await user.click(screen.getByRole('button', { name: 'Transfer ownership' }));
    const dialog = screen.getByRole('dialog', { name: 'Transfer ownership' });
    expect(dialog).toHaveClass('mn-transfer-ownership-flow');
    expect(primary('Continue')).toBeDisabled();
    expect(primary('Continue')).toHaveAccessibleDescription('Choose a member to continue.');
    await expectNoA11yViolations();

    await user.click(screen.getByRole('combobox', { name: 'New owner' }));
    const options = await screen.findAllByRole('option');
    expect(options.map((o) => o.textContent)).not.toContain(expect.stringContaining('Sam'));
    await user.click(screen.getByRole('option', { name: /Ada Lovelace/ }));
    await user.click(primary('Continue'));

    expect(screen.getByRole('heading', { name: 'What changes' })).toHaveFocus();
    expect(screen.getByText('Your role changes to Admin')).toBeInTheDocument();
    expect(screen.getByText('Ada Lovelace can delete the workspace')).toBeInTheDocument();
    await user.selectOptions(
      screen.getByRole('combobox', { name: 'Your role after the transfer' }),
      'member',
    );
    expect(screen.getByText('Your role changes to Member')).toBeInTheDocument();
    await user.click(primary('Continue'));

    const confirm = primary('Transfer ownership');
    expect(confirm).toHaveAttribute('data-variant', 'primary');
    await user.click(confirm);
    expect(onTransfer).toHaveBeenCalledWith({ memberId: 'u1', yourRole: 'member' });
    expect(
      await screen.findByRole('heading', { name: 'Ada Lovelace is now the owner' }),
    ).toHaveFocus();
    await user.click(primary('Done'));
    await waitFor(() => expect(screen.queryByRole('dialog')).toBeNull());
  });

  it('only lets eligible members be picked and says why the others cannot', async () => {
    const user = userEvent.setup();
    render(<Flow defaultOpen eligibleRoles={['admin']} />);
    await user.click(screen.getByRole('combobox', { name: 'New owner' }));
    const grace = await screen.findByRole('option', { name: /Grace Hopper/ });
    expect(grace).toHaveAttribute('aria-disabled', 'true');
    expect(grace).toHaveTextContent('Only admins can become the owner');
    const alan = screen.getByRole('option', { name: /Alan Turing/ });
    expect(alan).toHaveAttribute('aria-disabled', 'true');
    expect(alan).toHaveTextContent('Invite not accepted yet');
    expect(screen.getByRole('option', { name: /Ada Lovelace/ })).not.toHaveAttribute(
      'aria-disabled',
      'true',
    );
  });

  it('explains when nobody can become the owner', () => {
    render(<Flow defaultOpen candidates={[candidates[0]!]} />);
    expect(screen.getByText(/no other member who can become the owner/)).toBeInTheDocument();
    expect(primary('Continue')).toBeDisabled();
  });

  it('waits for acceptance and can cancel the pending transfer', async () => {
    const user = userEvent.setup();
    const onCancelTransfer = vi.fn(() => Promise.resolve());
    render(<Flow defaultOpen requiresAcceptance onCancelTransfer={onCancelTransfer} />);
    await pick(user, 'Grace Hopper');
    await user.click(primary('Continue'));
    expect(screen.getByText('You become an admin')).toBeInTheDocument();
    await user.click(primary('Continue'));
    await user.click(primary('Transfer ownership'));
    expect(
      await screen.findByRole('heading', { name: 'Waiting for Grace Hopper to accept' }),
    ).toBeInTheDocument();
    await user.click(primary('Cancel transfer'));
    expect(onCancelTransfer).toHaveBeenCalledTimes(1);
    expect(
      await screen.findByRole('heading', { name: /The transfer was cancelled/ }),
    ).toBeInTheDocument();
  });

  it('shows a failed transfer on the confirm step and allows a retry', async () => {
    const user = userEvent.setup();
    const onTransfer = vi
      .fn()
      .mockRejectedValueOnce(new Error('The server is busy.'))
      .mockResolvedValueOnce(undefined);
    render(<Flow defaultOpen onTransfer={onTransfer} />);
    await pick(user, 'Ada Lovelace');
    await user.click(primary('Continue'));
    await user.click(primary('Continue'));
    await user.click(primary('Transfer ownership'));
    expect(await screen.findByText('The server is busy.')).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Confirm the transfer' })).toBeInTheDocument();
    await user.click(primary('Transfer ownership'));
    expect(await screen.findByRole('heading', { name: /is now the owner/ })).toBeInTheDocument();
  });

  it('asks for step-up first; cancelling it keeps the person on the confirm step', async () => {
    const user = userEvent.setup();
    const onTransfer = vi.fn(() => Promise.resolve());
    const onVerify = vi.fn(() => Promise.resolve());
    render(<Flow defaultOpen onTransfer={onTransfer} stepUp={{ onVerify }} />);
    await pick(user, 'Ada Lovelace');
    await user.click(primary('Continue'));
    await user.click(primary('Continue'));
    await user.click(primary('Transfer ownership'));

    const stepUp = await screen.findByRole('dialog', { name: 'Confirm it’s you' });
    await user.click(within(stepUp).getByRole('button', { name: 'Cancel' }));
    await waitFor(() =>
      expect(screen.queryByRole('dialog', { name: 'Confirm it’s you' })).toBeNull(),
    );
    expect(onTransfer).not.toHaveBeenCalled();
    expect(screen.getByRole('heading', { name: 'Confirm the transfer' })).toBeInTheDocument();

    await user.click(primary('Transfer ownership'));
    const again = await screen.findByRole('dialog', { name: 'Confirm it’s you' });
    await user.type(within(again).getByLabelText(/^Password/), 'secret');
    await user.click(within(again).getByRole('button', { name: 'Confirm' }));
    expect(onVerify).toHaveBeenCalledWith({ method: 'password', password: 'secret' });
    expect(await screen.findByRole('heading', { name: /is now the owner/ })).toBeInTheDocument();
    expect(onTransfer).toHaveBeenCalledWith({ memberId: 'u1' });
  });

  it('renders on the server', () => {
    expect(() => renderToString(<Flow />)).not.toThrow();
    expect(renderToString(<Flow defaultOpen />)).toBeDefined();
  });
});
