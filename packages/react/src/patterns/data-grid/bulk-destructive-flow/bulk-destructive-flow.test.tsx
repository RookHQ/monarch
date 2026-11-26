import type { ReactElement } from 'react';
import { describe, expect, it, vi } from 'vitest';
import { act, render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { Button } from '../../../components/actions/button/index.js';
import { BulkDestructiveFlow, type BulkDestructiveFlowProps } from './index.js';

const noun = { one: 'lead', other: 'leads' };

type FlowProps = Omit<Partial<BulkDestructiveFlowProps>, 'trigger'> & {
  trigger?: ReactElement | null;
};

function Flow({ trigger = <Button>Open</Button>, ...props }: FlowProps) {
  return (
    <BulkDestructiveFlow
      {...(trigger ? { trigger } : {})}
      count={37}
      noun={noun}
      sample={['Ada Park', 'Ben Ortiz', 'Cleo Ngata']}
      onDelete={() => Promise.resolve()}
      {...props}
    />
  );
}

function dialog() {
  return screen.getByRole('dialog');
}

function deleteButton(name: string | RegExp) {
  return within(dialog()).getByRole('button', { name });
}

describe('BulkDestructiveFlow', () => {
  it('shows the sample, requires the count to be typed, runs and reports success', async () => {
    const user = userEvent.setup();
    const onDelete = vi.fn(() => Promise.resolve());
    render(<Flow onDelete={onDelete} />);
    await user.click(screen.getByRole('button', { name: 'Open' }));
    expect(screen.getByRole('dialog', { name: 'Delete 37 leads' })).toBeInTheDocument();
    expect(dialog()).toHaveTextContent('Ada Park');
    expect(dialog()).toHaveTextContent('+34 more');
    await expectNoA11yViolations();

    const action = deleteButton('Delete 37 leads');
    expect(action).toBeDisabled();
    expect(action).toHaveAccessibleDescription('Type the number to continue.');
    await user.type(screen.getByLabelText(/to confirm/), '37');
    expect(action).toBeEnabled();
    expect(action).toHaveAttribute('data-variant', 'danger');
    await user.click(action);

    expect(onDelete).toHaveBeenCalledTimes(1);
    expect(await screen.findByRole('heading', { name: '37 leads deleted' })).toHaveFocus();
    expect(within(dialog()).getByRole('button', { name: 'Done' })).toBeInTheDocument();
  });

  it('does not ask for the count at or below typeCountAbove', async () => {
    const user = userEvent.setup();
    render(<Flow defaultOpen count={1} sample={['Ada Park']} trigger={null} />);
    expect(screen.queryByLabelText(/to confirm/)).toBeNull();
    expect(dialog()).not.toHaveTextContent('more');
    await user.click(deleteButton('Delete 1 lead'));
    expect(await screen.findByRole('heading', { name: '1 lead deleted' })).toBeInTheDocument();
  });

  it('shows progress while running and locks the dialog', async () => {
    const user = userEvent.setup();
    let report!: (done: number) => void;
    let finish!: () => void;
    const onDelete = (r: (done: number) => void) =>
      new Promise<void>((resolve) => {
        report = r;
        finish = resolve;
      });
    render(<Flow defaultOpen count={5} trigger={null} onDelete={onDelete} />);
    await user.click(deleteButton('Delete 5 leads'));
    expect(dialog()).toHaveAttribute('aria-busy', 'true');
    expect(screen.getByRole('progressbar', { name: 'Deletion progress' })).toBeInTheDocument();
    act(() => report(2));
    expect(dialog()).toHaveTextContent('2 of 5 deleted');
    expect(within(dialog()).getByRole('button', { name: 'Cancel' })).toBeDisabled();
    await user.keyboard('{Escape}');
    expect(screen.getByRole('dialog')).toBeInTheDocument();
    await act(async () => finish());
    expect(await screen.findByRole('heading', { name: '5 leads deleted' })).toBeInTheDocument();
  });

  it('reports a partial failure and retries the failed items', async () => {
    const user = userEvent.setup();
    const failed = [{ id: 'b', label: 'Ben Ortiz', error: 'Locked by an open invoice.' }];
    const onRetryFailed = vi.fn(() => Promise.resolve({ failed: [] }));
    render(
      <Flow
        defaultOpen
        count={3}
        trigger={null}
        onDelete={() => Promise.resolve({ failed })}
        onRetryFailed={onRetryFailed}
      />,
    );
    await user.click(deleteButton('Delete 3 leads'));
    expect(
      await screen.findByRole('heading', { name: '2 of 3 leads deleted' }),
    ).toBeInTheDocument();
    expect(dialog()).toHaveTextContent('1 lead could not be deleted.');
    expect(dialog()).toHaveTextContent('Locked by an open invoice.');
    await expectNoA11yViolations();

    await user.click(deleteButton('Retry 1 lead'));
    expect(screen.getByRole('heading', { name: 'Try the rest again' })).toHaveFocus();
    expect(within(dialog()).queryByRole('button', { name: 'Back' })).toBeNull();
    await user.click(deleteButton('Retry 1 lead'));
    expect(onRetryFailed).toHaveBeenCalledWith(failed, expect.any(Function));
    expect(await screen.findByRole('heading', { name: '3 leads deleted' })).toBeInTheDocument();
  });

  it('shows the error and stays when onDelete rejects', async () => {
    const user = userEvent.setup();
    render(
      <Flow
        defaultOpen
        count={2}
        trigger={null}
        onDelete={() => Promise.reject(new Error('The server did not respond.'))}
      />,
    );
    await user.click(deleteButton('Delete 2 leads'));
    expect(await screen.findByRole('alert')).toHaveTextContent('The server did not respond.');
    expect(screen.getByRole('heading', { name: 'Check before you delete' })).toBeInTheDocument();
    expect(screen.queryByRole('progressbar')).toBeNull();
  });

  it('step-up: cancelling stays without deleting; verifying runs the deletion', async () => {
    const user = userEvent.setup();
    const onDelete = vi.fn(() => Promise.resolve());
    const onVerify = vi.fn(() => Promise.resolve());
    render(
      <Flow
        defaultOpen
        count={2}
        trigger={null}
        onDelete={onDelete}
        stepUp={{ methods: ['password'], onVerify }}
      />,
    );
    await user.click(deleteButton('Delete 2 leads'));
    const stepUp = await screen.findByRole('dialog', { name: 'Confirm it’s you' });
    await user.click(within(stepUp).getByRole('button', { name: 'Cancel' }));
    await waitFor(() =>
      expect(screen.queryByRole('dialog', { name: 'Confirm it’s you' })).toBeNull(),
    );
    expect(onDelete).not.toHaveBeenCalled();
    expect(screen.getByRole('dialog', { name: 'Delete 2 leads' })).toBeInTheDocument();

    await user.click(deleteButton('Delete 2 leads'));
    const again = await screen.findByRole('dialog', { name: 'Confirm it’s you' });
    await user.type(within(again).getByLabelText(/^Password/, { selector: 'input' }), 'secret');
    await user.click(within(again).getByRole('button', { name: 'Confirm' }));
    expect(onVerify).toHaveBeenCalledWith({ method: 'password', password: 'secret' });
    expect(await screen.findByRole('heading', { name: '2 leads deleted' })).toBeInTheDocument();
    expect(onDelete).toHaveBeenCalledTimes(1);
  });

  it('uses plural rules and messages', () => {
    render(
      <Flow
        defaultOpen
        count={1}
        trigger={null}
        noun={(c) => (c === 1 ? 'Datensatz' : 'Datensätze')}
        messages={{ title: (c, n) => `${n} löschen (${c})` }}
      />,
    );
    expect(screen.getByRole('dialog', { name: 'Datensatz löschen (1)' })).toBeInTheDocument();
  });

  it('renders on the server', () => {
    expect(() => renderToString(<Flow />)).not.toThrow();
    expect(renderToString(<Flow defaultOpen trigger={null} />)).toBeTypeOf('string');
  });
});
