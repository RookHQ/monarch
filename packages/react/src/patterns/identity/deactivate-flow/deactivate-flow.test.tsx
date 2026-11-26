import type { ReactElement } from 'react';
import { describe, expect, it, vi } from 'vitest';
import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { Button } from '../../../components/actions/button/index.js';
import { DeactivateFlow, type DeactivateFlowProps } from './index.js';

type FlowProps = Omit<Partial<DeactivateFlowProps>, 'trigger'> & { trigger?: ReactElement | null };

function Flow({ trigger = <Button>Pause</Button>, ...props }: FlowProps) {
  return (
    <DeactivateFlow
      {...(trigger ? { trigger } : {})}
      subject="the partnership"
      reactivateFrom="Settings › Partners"
      consequences={[
        { kind: 'changed', label: 'New referrals are not tracked' },
        { kind: 'kept', label: 'Past payouts stay in reports' },
      ]}
      {...props}
    />
  );
}

describe('DeactivateFlow', () => {
  it('opens a warning confirmation with the reversibility line and consequences', async () => {
    const user = userEvent.setup();
    render(<Flow />);
    await user.click(screen.getByRole('button', { name: 'Pause' }));
    const dialog = screen.getByRole('alertdialog', { name: 'Deactivate the partnership?' });
    expect(dialog).toHaveClass('mn-deactivate-flow');
    expect(dialog).toHaveAttribute('data-tone', 'warning');
    expect(dialog).toHaveAttribute('data-mode', 'deactivate');
    expect(dialog).toHaveTextContent('You can reactivate it at any time from Settings › Partners.');
    expect(screen.getByRole('heading', { name: 'While it is deactivated' })).toBeInTheDocument();
    expect(dialog).toHaveTextContent('New referrals are not tracked');
    expect(within(dialog).getByRole('button', { name: 'Cancel' })).toHaveFocus();
    const confirm = within(dialog).getByRole('button', { name: 'Deactivate' });
    expect(confirm).toHaveAttribute('data-variant', 'primary');
    await expectNoA11yViolations();
  });

  it('passes the reason to onDeactivate and closes', async () => {
    const user = userEvent.setup();
    const onDeactivate = vi.fn(() => Promise.resolve());
    render(<Flow defaultOpen trigger={null} reason onDeactivate={onDeactivate} />);
    await user.type(screen.getByLabelText('Reason (optional)'), '  Contract on hold  ');
    await user.click(screen.getByRole('button', { name: 'Deactivate' }));
    expect(onDeactivate).toHaveBeenCalledWith({ reason: 'Contract on hold' });
    await waitFor(() => expect(screen.queryByRole('alertdialog')).toBeNull());
  });

  it('keeps the dialog open with the error when onDeactivate rejects', async () => {
    const user = userEvent.setup();
    render(
      <Flow
        defaultOpen
        trigger={null}
        onDeactivate={() => Promise.reject(new Error('Could not pause it.'))}
      />,
    );
    await user.click(screen.getByRole('button', { name: 'Deactivate' }));
    expect(await screen.findByRole('alert')).toHaveTextContent('Could not pause it.');
    expect(screen.getByRole('alertdialog')).toBeInTheDocument();
  });

  it('reactivate mode swaps copy and tone and calls onReactivate', async () => {
    const user = userEvent.setup();
    const onDeactivate = vi.fn();
    const onReactivate = vi.fn();
    render(
      <Flow
        defaultOpen
        trigger={null}
        mode="reactivate"
        consequences={[]}
        onDeactivate={onDeactivate}
        onReactivate={onReactivate}
      />,
    );
    const dialog = screen.getByRole('alertdialog', { name: 'Reactivate the partnership?' });
    expect(dialog).toHaveAttribute('data-tone', 'default');
    expect(dialog).not.toHaveTextContent('You can reactivate it');
    await user.click(within(dialog).getByRole('button', { name: 'Reactivate' }));
    expect(onReactivate).toHaveBeenCalledWith({});
    expect(onDeactivate).not.toHaveBeenCalled();
  });

  it('Escape cancels', async () => {
    const user = userEvent.setup();
    const onCancel = vi.fn();
    render(<Flow defaultOpen trigger={null} onCancel={onCancel} />);
    await user.keyboard('{Escape}');
    expect(onCancel).toHaveBeenCalledTimes(1);
  });

  it('takes its strings from messages', () => {
    render(
      <Flow
        defaultOpen
        trigger={null}
        reactivateFrom={null}
        messages={{
          deactivateTitle: (s) => <>Pausieren: {s}</>,
          deactivate: 'Pausieren',
          reversible: () => 'Jederzeit wieder aktivierbar.',
        }}
      />,
    );
    expect(
      screen.getByRole('alertdialog', { name: 'Pausieren: the partnership' }),
    ).toHaveTextContent('Jederzeit wieder aktivierbar.');
    expect(screen.getByRole('button', { name: 'Pausieren' })).toBeInTheDocument();
  });

  it('renders on the server', () => {
    expect(renderToString(<Flow />)).toContain('Pause');
  });
});
