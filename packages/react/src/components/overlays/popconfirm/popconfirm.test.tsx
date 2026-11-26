import { describe, expect, it, vi } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { Button } from '../../actions/button/index.js';
import { Popconfirm, type PopconfirmProps } from './index.js';

function deferred() {
  let resolve!: () => void;
  let reject!: (error: unknown) => void;
  const promise = new Promise<void>((res, rej) => {
    resolve = res;
    reject = rej;
  });
  return { promise, resolve, reject };
}

function RemoveFilter(props: Partial<PopconfirmProps>) {
  return (
    <Popconfirm
      trigger={<Button>Remove filter</Button>}
      title="Remove this filter?"
      description="The list shows every item again."
      confirmLabel="Remove"
      onConfirm={() => {}}
      {...props}
    />
  );
}

const trigger = () => screen.getByRole('button', { name: 'Remove filter' });

describe('Popconfirm', () => {
  it('opens as a named, described alertdialog with focus on Cancel', async () => {
    render(<RemoveFilter />);
    const user = userEvent.setup();
    await user.click(trigger());
    const dialog = await screen.findByRole('alertdialog', { name: 'Remove this filter?' });
    expect(dialog).toHaveAccessibleDescription('The list shows every item again.');
    expect(dialog).toHaveClass('mn-popover', 'mn-popconfirm');
    expect(dialog).toHaveAttribute('data-tone', 'default');
    await waitFor(() => expect(screen.getByRole('button', { name: 'Cancel' })).toHaveFocus());
    await expectNoA11yViolations(dialog);
  });

  it('Escape cancels and returns focus to the trigger', async () => {
    const onCancel = vi.fn();
    const onConfirm = vi.fn();
    render(<RemoveFilter onCancel={onCancel} onConfirm={onConfirm} />);
    const user = userEvent.setup();
    trigger().focus();
    await user.keyboard('{Enter}');
    await screen.findByRole('alertdialog');
    await user.keyboard('{Escape}');
    await waitFor(() => expect(screen.queryByRole('alertdialog')).toBeNull());
    expect(onCancel).toHaveBeenCalledOnce();
    expect(onConfirm).not.toHaveBeenCalled();
    expect(trigger()).toHaveFocus();
  });

  it('Cancel button cancels', async () => {
    const onCancel = vi.fn();
    render(<RemoveFilter onCancel={onCancel} />);
    const user = userEvent.setup();
    await user.click(trigger());
    await user.click(await screen.findByRole('button', { name: 'Cancel' }));
    await waitFor(() => expect(screen.queryByRole('alertdialog')).toBeNull());
    expect(onCancel).toHaveBeenCalledOnce();
    expect(trigger()).toHaveFocus();
  });

  it('confirms and closes; Tab stays inside while open', async () => {
    const onConfirm = vi.fn();
    render(<RemoveFilter onConfirm={onConfirm} />);
    const user = userEvent.setup();
    await user.click(trigger());
    await screen.findByRole('alertdialog');
    await user.tab();
    expect(screen.getByRole('button', { name: 'Remove' })).toHaveFocus();
    await user.tab();
    expect(screen.getByRole('button', { name: 'Cancel' })).toHaveFocus();
    await user.tab({ shift: true });
    await user.keyboard('{Enter}');
    expect(onConfirm).toHaveBeenCalledOnce();
    await waitFor(() => expect(screen.queryByRole('alertdialog')).toBeNull());
    expect(trigger()).toHaveFocus();
  });

  it('stays open while an async action runs, shows its error and allows a retry', async () => {
    const first = deferred();
    const onConfirm = vi
      .fn()
      .mockReturnValueOnce(first.promise)
      .mockReturnValueOnce(Promise.resolve());
    render(<RemoveFilter onConfirm={onConfirm} tone="danger" />);
    const user = userEvent.setup();
    await user.click(trigger());
    const dialog = await screen.findByRole('alertdialog');
    expect(dialog).toHaveAttribute('data-tone', 'danger');
    await user.click(screen.getByRole('button', { name: 'Remove' }));
    expect(dialog).toHaveAttribute('aria-busy', 'true');
    expect(screen.getByRole('button', { name: 'Cancel' })).toBeDisabled();
    await user.keyboard('{Escape}');
    expect(screen.getByRole('alertdialog')).toBeInTheDocument();
    first.reject(new Error('The filter is in use by a saved view.'));
    expect(await screen.findByRole('alert')).toHaveTextContent(
      'The filter is in use by a saved view.',
    );
    expect(screen.getByRole('button', { name: 'Remove' })).toHaveAccessibleDescription(
      'The filter is in use by a saved view.',
    );
    await user.click(screen.getByRole('button', { name: 'Remove' }));
    await waitFor(() => expect(screen.queryByRole('alertdialog')).toBeNull());
    expect(onConfirm).toHaveBeenCalledTimes(2);
  });

  it('can start on the confirm button', async () => {
    render(<RemoveFilter initialFocus="confirm" />);
    await userEvent.setup().click(trigger());
    await waitFor(() => expect(screen.getByRole('button', { name: 'Remove' })).toHaveFocus());
  });

  it('follows the open prop when controlled', async () => {
    const onOpenChange = vi.fn();
    render(<RemoveFilter open onOpenChange={onOpenChange} />);
    expect(await screen.findByRole('alertdialog')).toBeInTheDocument();
    await userEvent.setup().keyboard('{Escape}');
    expect(onOpenChange).toHaveBeenCalledWith(false);
    expect(screen.getByRole('alertdialog')).toBeInTheDocument();
  });

  it('renders on the server', () => {
    const html = renderToString(<RemoveFilter />);
    expect(html).toContain('Remove filter');
    expect(html).toContain('aria-haspopup="dialog"');
  });
});
