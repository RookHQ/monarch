import { describe, expect, it, vi } from 'vitest';
import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { Button } from '../../../components/actions/button/index.js';
import { LeaveWorkspaceFlow, type LeaveWorkspaceFlowProps } from './index.js';

function deferred() {
  let resolve!: () => void;
  let reject!: (error: unknown) => void;
  const promise = new Promise<void>((res, rej) => {
    resolve = res;
    reject = rej;
  });
  return { promise, resolve, reject };
}

function Flow(props: Partial<LeaveWorkspaceFlowProps>) {
  return (
    <LeaveWorkspaceFlow
      trigger={<Button variant="danger">Leave workspace</Button>}
      workspaceName="Northfield"
      apps={['Projects', 'Docs']}
      onLeave={() => undefined}
      {...props}
    />
  );
}

describe('LeaveWorkspaceFlow', () => {
  it('confirms with consequences, focuses Cancel and runs onLeave', async () => {
    const user = userEvent.setup();
    const onLeave = vi.fn();
    render(<Flow onLeave={onLeave} />);
    const trigger = screen.getByRole('button', { name: 'Leave workspace' });
    await user.click(trigger);
    const dialog = screen.getByRole('alertdialog', { name: 'Leave Northfield?' });
    expect(dialog).toHaveClass('mn-leave-workspace-flow', 'mn-confirm-dialog');
    expect(dialog).toHaveAttribute('data-tone', 'danger');
    expect(dialog).toHaveTextContent('You lose access to Projects and Docs');
    expect(dialog).toHaveTextContent('What you created stays');
    expect(dialog).toHaveTextContent('You need a new invite to come back');
    expect(within(dialog).getByRole('button', { name: 'Cancel' })).toHaveFocus();
    await expectNoA11yViolations();
    const leave = within(dialog).getByRole('button', { name: 'Leave workspace' });
    expect(leave).toHaveAttribute('data-variant', 'danger');
    await user.click(leave);
    expect(onLeave).toHaveBeenCalledTimes(1);
    await waitFor(() => expect(screen.queryByRole('alertdialog')).toBeNull());
    expect(trigger).toHaveFocus();
  });

  it('keeps the dialog pending and shows a failure', async () => {
    const user = userEvent.setup();
    const d = deferred();
    render(<Flow defaultOpen onLeave={() => d.promise} />);
    const dialog = screen.getByRole('alertdialog');
    await user.click(within(dialog).getByRole('button', { name: 'Leave workspace' }));
    expect(dialog).toHaveAttribute('aria-busy', 'true');
    await user.keyboard('{Escape}');
    expect(screen.getByRole('alertdialog')).toBeInTheDocument();
    d.reject(new Error('You have unsaved exports.'));
    expect(await screen.findByText('You have unsaved exports.')).toBeInTheDocument();
  });

  it('is blocked for the last owner: explains why, offers the fix, has no leave button', async () => {
    const user = userEvent.setup();
    const onLeave = vi.fn();
    const onFix = vi.fn();
    render(
      <Flow
        isLastOwner
        onLeave={onLeave}
        fixAction={
          <Button variant="primary" onClick={onFix}>
            Transfer ownership
          </Button>
        }
      />,
    );
    await user.click(screen.getByRole('button', { name: 'Leave workspace' }));
    const dialog = screen.getByRole('dialog', { name: 'You can’t leave yet' });
    expect(dialog).toHaveAttribute('data-blocked');
    expect(dialog).toHaveAccessibleDescription(/You are the only owner/);
    expect(within(dialog).queryByRole('button', { name: 'Leave workspace' })).toBeNull();
    await expectNoA11yViolations();
    await user.click(within(dialog).getByRole('button', { name: 'Transfer ownership' }));
    expect(onFix).toHaveBeenCalledTimes(1);
    await user.click(within(dialog).getByRole('button', { name: 'Close' }));
    await waitFor(() => expect(screen.queryByRole('dialog')).toBeNull());
    expect(onLeave).not.toHaveBeenCalled();
  });

  it('a product reason blocks too and Escape closes it', async () => {
    const user = userEvent.setup();
    const onCancel = vi.fn();
    render(
      <Flow
        defaultOpen
        blockedReason="Your account is managed by your organisation."
        onCancel={onCancel}
      />,
    );
    expect(screen.getByRole('dialog')).toHaveAccessibleDescription(
      'Your account is managed by your organisation.',
    );
    await user.keyboard('{Escape}');
    expect(onCancel).toHaveBeenCalledTimes(1);
    await waitFor(() => expect(screen.queryByRole('dialog')).toBeNull());
  });

  it('translates through messages and accepts custom consequences', () => {
    render(
      <Flow
        defaultOpen
        consequences={[{ kind: 'removed', label: 'Your seat is released' }]}
        messages={{ confirm: 'Quitter', title: () => 'Quitter cet espace ?' }}
      />,
    );
    const dialog = screen.getByRole('alertdialog', { name: 'Quitter cet espace ?' });
    expect(dialog).toHaveTextContent('Your seat is released');
    expect(within(dialog).getByRole('button', { name: 'Quitter' })).toBeInTheDocument();
  });

  it('renders on the server', () => {
    expect(renderToString(<Flow />)).toContain('Leave workspace');
    expect(renderToString(<Flow isLastOwner />)).toContain('Leave workspace');
  });
});
