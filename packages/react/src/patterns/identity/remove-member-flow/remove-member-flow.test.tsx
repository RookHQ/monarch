import { describe, expect, it, vi } from 'vitest';
import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { Button } from '../../../components/actions/button/index.js';
import { ToastProvider } from '../../../components/feedback/toast/index.js';
import type { AssigneePerson } from '../../../components/identity/assignee-picker/index.js';
import { RemoveMemberFlow, type RemoveMemberFlowProps } from './index.js';

const member = { id: 'u9', name: 'Jordan Lee', email: 'jordan@example.com' };
const assignees: AssigneePerson[] = [
  { id: 'u9', name: 'Jordan Lee' },
  { id: 'u1', name: 'Ada Lovelace', email: 'ada@example.com' },
  { id: 'u2', name: 'Grace Hopper', email: 'grace@example.com' },
];
const openWork = [
  { id: 'issues', label: 'open issues', count: 12 },
  { id: 'reviews', label: 'pending reviews', count: 3 },
];

function Flow(props: Partial<RemoveMemberFlowProps>) {
  return (
    <RemoveMemberFlow
      trigger={<Button variant="danger">Remove</Button>}
      member={member}
      openWork={openWork}
      assignees={assignees}
      onRemove={() => Promise.resolve()}
      {...props}
    />
  );
}

function inDialog(name: string | RegExp) {
  return within(screen.getByRole('dialog')).getByRole('button', { name });
}

async function choose(user: ReturnType<typeof userEvent.setup>, picker: string, option: RegExp) {
  await user.click(screen.getByRole('combobox', { name: picker }));
  await user.click(await screen.findByRole('option', { name: option }));
}

describe('RemoveMemberFlow', () => {
  it('reassigns everything at once, confirms and removes', async () => {
    const user = userEvent.setup();
    const onRemove = vi.fn(() => Promise.resolve());
    render(<Flow onRemove={onRemove} />);
    await user.click(screen.getByRole('button', { name: 'Remove' }));
    const dialog = screen.getByRole('dialog', { name: 'Remove Jordan Lee' });
    expect(dialog).toHaveClass('mn-remove-member-flow');
    expect(dialog).toHaveTextContent('Step 1 of 2');
    expect(dialog).toHaveTextContent('12 open issues');
    await expectNoA11yViolations();

    await user.click(screen.getByRole('combobox', { name: 'Reassign all open work to' }));
    const names = (await screen.findAllByRole('option')).map((o) => o.textContent);
    expect(names.some((n) => n?.includes('Jordan'))).toBe(false);
    expect(names[0]).toBe('Leave unassigned');
    await user.click(screen.getByRole('option', { name: /Ada Lovelace/ }));
    await user.click(inDialog('Continue'));

    expect(screen.getByRole('heading', { name: 'Confirm removal' })).toHaveFocus();
    expect(dialog).toHaveTextContent('Jordan Lee loses access to the workspace');
    expect(dialog).toHaveTextContent('12 open issues go to Ada Lovelace');
    expect(dialog).toHaveTextContent('3 pending reviews go to Ada Lovelace');
    const remove = inDialog('Remove member');
    expect(remove).toHaveAttribute('data-variant', 'danger');
    await user.click(remove);
    expect(onRemove).toHaveBeenCalledWith({
      reassignments: [
        { workId: 'issues', assigneeId: 'u1' },
        { workId: 'reviews', assigneeId: 'u1' },
      ],
    });
    await waitFor(() => expect(screen.queryByRole('dialog')).toBeNull());
  });

  it('reassigns per item, including leaving one unassigned', async () => {
    const user = userEvent.setup();
    const onRemove = vi.fn();
    render(<Flow defaultOpen onRemove={onRemove} />);
    await choose(user, 'Reassign all open work to', /Grace Hopper/);
    await user.click(
      screen.getByRole('checkbox', { name: 'Choose a person for each kind of work' }),
    );
    expect(screen.getByRole('combobox', { name: '12 open issues' })).toHaveTextContent(
      'Grace Hopper',
    );
    await choose(user, '3 pending reviews', /Leave unassigned/);
    await user.click(inDialog('Continue'));
    expect(screen.getByRole('dialog')).toHaveTextContent('3 pending reviews are left unassigned');
    await user.click(inDialog('Remove member'));
    expect(onRemove).toHaveBeenCalledWith({
      reassignments: [
        { workId: 'issues', assigneeId: 'u2' },
        { workId: 'reviews', assigneeId: null },
      ],
    });
  });

  it('skips the reassign step when there is no open work', async () => {
    const onRemove = vi.fn();
    const user = userEvent.setup();
    render(<Flow defaultOpen openWork={[]} onRemove={onRemove} />);
    expect(screen.getByRole('heading', { name: 'Confirm removal' })).toBeInTheDocument();
    expect(screen.queryByText(/Step \d of/)).toBeNull();
    expect(screen.getByRole('dialog')).toHaveTextContent('jordan@example.com');
    await expectNoA11yViolations();
    await user.click(inDialog('Remove member'));
    expect(onRemove).toHaveBeenCalledWith({ reassignments: [] });
  });

  it('shows a failed removal on the confirm step', async () => {
    const user = userEvent.setup();
    render(
      <Flow defaultOpen openWork={[]} onRemove={() => Promise.reject(new Error('Try later.'))} />,
    );
    await user.click(inDialog('Remove member'));
    expect(await screen.findByText('Try later.')).toBeInTheDocument();
    expect(screen.getByRole('dialog')).toBeInTheDocument();
  });

  it('asks for step-up first; cancelling it removes nothing', async () => {
    const user = userEvent.setup();
    const onRemove = vi.fn();
    render(
      <Flow
        defaultOpen
        openWork={[]}
        onRemove={onRemove}
        stepUp={{ methods: ['passkey'], onVerify: () => Promise.resolve() }}
      />,
    );
    await user.click(inDialog('Remove member'));
    const stepUp = await screen.findByRole('dialog', { name: 'Confirm it’s you' });
    await user.click(within(stepUp).getByRole('button', { name: 'Cancel' }));
    await waitFor(() =>
      expect(screen.queryByRole('dialog', { name: 'Confirm it’s you' })).toBeNull(),
    );
    expect(onRemove).not.toHaveBeenCalled();
    await user.click(
      within(screen.getByRole('dialog')).getByRole('button', { name: 'Remove member' }),
    );
    const again = await screen.findByRole('dialog', { name: 'Confirm it’s you' });
    await user.click(within(again).getByRole('button', { name: 'Use passkey' }));
    await waitFor(() => expect(onRemove).toHaveBeenCalledTimes(1));
  });

  it('offers Undo after the removal when onUndo is given', async () => {
    const user = userEvent.setup();
    const onUndo = vi.fn();
    render(
      <ToastProvider>
        <Flow defaultOpen openWork={[]} onUndo={onUndo} />
      </ToastProvider>,
    );
    await user.click(inDialog('Remove member'));
    await waitFor(() => expect(screen.queryByRole('dialog')).toBeNull());
    expect(await screen.findByText('Jordan Lee removed')).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Undo' }));
    expect(onUndo).toHaveBeenCalledWith({ reassignments: [] });
  });

  it('needs no ToastProvider without onUndo', () => {
    expect(() => render(<Flow defaultOpen />)).not.toThrow();
  });

  it('renders on the server', () => {
    expect(renderToString(<Flow />)).toContain('Remove');
  });
});
