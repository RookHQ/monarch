import { describe, expect, it, vi } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { Button } from '../../actions/button/index.js';
import { WizardDialog, type WizardDialogProps, type WizardStep } from './index.js';

function deferred() {
  let resolve!: () => void;
  let reject!: (error: unknown) => void;
  const promise = new Promise<void>((res, rej) => {
    resolve = res;
    reject = rej;
  });
  return { promise, resolve, reject };
}

const baseSteps: WizardStep[] = [
  { id: 'review', title: 'Review what is removed', content: <p>Items and history.</p> },
  { id: 'export', title: 'Export your data', content: <p>Download a copy first.</p> },
  { id: 'confirm', title: 'Confirm', continueLabel: 'Delete workspace', tone: 'danger' },
];

function Flow(props: Partial<WizardDialogProps>) {
  return (
    <WizardDialog
      trigger={<Button>Delete workspace</Button>}
      title="Delete workspace"
      steps={baseSteps}
      {...props}
    />
  );
}

describe('WizardDialog', () => {
  it('walks forward and back, shows progress and focuses each step heading', async () => {
    const user = userEvent.setup();
    render(<Flow />);
    await user.click(screen.getByRole('button', { name: 'Delete workspace' }));
    const dialog = screen.getByRole('dialog', { name: 'Delete workspace' });
    expect(dialog).toHaveTextContent('Step 1 of 3');
    await expectNoA11yViolations();
    await user.click(screen.getByRole('button', { name: 'Continue' }));
    expect(screen.getByRole('heading', { name: 'Export your data' })).toHaveFocus();
    expect(dialog).toHaveTextContent('Step 2 of 3');
    await user.click(screen.getByRole('button', { name: 'Back' }));
    expect(screen.getByRole('heading', { name: 'Review what is removed' })).toHaveFocus();
  });

  it('runs the last step, calls onComplete and closes', async () => {
    const user = userEvent.setup();
    const onComplete = vi.fn();
    render(<Flow defaultOpen defaultStep="confirm" onComplete={onComplete} />);
    const action = screen
      .getAllByRole('button', { name: 'Delete workspace' })
      .find((b) => b.closest('[role="dialog"]'))!;
    expect(action).toHaveAttribute('data-variant', 'danger');
    await user.click(action);
    expect(onComplete).toHaveBeenCalledTimes(1);
    await waitFor(() => expect(screen.queryByRole('dialog')).toBeNull());
  });

  it('blocks Continue with a reason until the step allows it', () => {
    render(
      <Flow
        defaultOpen
        steps={[{ id: 'a', title: 'A', canContinue: false, blockedReason: 'Pick a member first.' }]}
      />,
    );
    const next = screen.getByRole('button', { name: 'Continue' });
    expect(next).toBeDisabled();
    expect(next).toHaveAccessibleDescription('Pick a member first.');
  });

  it('async step: pending locks the dialog; a rejection shows the error and stays', async () => {
    const user = userEvent.setup();
    const d = deferred();
    render(
      <Flow
        defaultOpen
        steps={[
          { id: 'a', title: 'Step A', onContinue: () => d.promise },
          { id: 'b', title: 'Step B' },
        ]}
      />,
    );
    await user.click(screen.getByRole('button', { name: 'Continue' }));
    expect(screen.getByRole('dialog')).toHaveAttribute('aria-busy', 'true');
    expect(screen.getByRole('button', { name: 'Cancel' })).toBeDisabled();
    await user.keyboard('{Escape}');
    expect(screen.getByRole('dialog')).toBeInTheDocument();
    d.reject(new Error('The server is busy.'));
    expect(await screen.findByText('The server is busy.')).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Step A' })).toBeInTheDocument();
  });

  it('returning false stays on the step without an error', async () => {
    const user = userEvent.setup();
    render(
      <Flow
        defaultOpen
        steps={[
          { id: 'a', title: 'Step A', onContinue: () => false },
          { id: 'b', title: 'Step B' },
        ]}
      />,
    );
    await user.click(screen.getByRole('button', { name: 'Continue' }));
    expect(screen.getByRole('heading', { name: 'Step A' })).toBeInTheDocument();
    expect(screen.queryByRole('alert')).toBeNull();
  });

  it('a done step has one close button and is not counted in the progress', async () => {
    const user = userEvent.setup();
    const onCancel = vi.fn();
    render(
      <Flow
        defaultOpen
        onCancel={onCancel}
        steps={[
          { id: 'a', title: 'Step A' },
          { id: 'b', title: 'Step B' },
          { id: 'done', title: 'Workspace deleted', kind: 'done' },
        ]}
      />,
    );
    expect(screen.getByRole('dialog')).toHaveTextContent('Step 1 of 2');
    await user.click(screen.getByRole('button', { name: 'Continue' }));
    await user.click(screen.getByRole('button', { name: 'Continue' }));
    expect(screen.getByRole('heading', { name: 'Workspace deleted' })).toHaveFocus();
    expect(screen.queryByRole('button', { name: 'Cancel' })).toBeNull();
    expect(screen.queryByText(/Step \d of/)).toBeNull();
    await user.click(screen.getByRole('button', { name: 'Done' }));
    expect(onCancel).not.toHaveBeenCalled();
  });

  it('Escape cancels, calls onCancel, and every opening starts at the first step', async () => {
    const user = userEvent.setup();
    const onCancel = vi.fn();
    render(<Flow onCancel={onCancel} />);
    await user.click(screen.getByRole('button', { name: 'Delete workspace' }));
    await user.click(screen.getByRole('button', { name: 'Continue' }));
    await user.keyboard('{Escape}');
    expect(onCancel).toHaveBeenCalledTimes(1);
    await user.click(screen.getByRole('button', { name: 'Delete workspace' }));
    expect(screen.getByRole('heading', { name: 'Review what is removed' })).toBeInTheDocument();
  });

  it('translates every string through messages', () => {
    render(
      <Flow
        defaultOpen
        defaultStep="export"
        messages={{
          back: 'Zurück',
          cancel: 'Abbrechen',
          continue: 'Weiter',
          stepOf: (c, t) => `${c}/${t}`,
        }}
      />,
    );
    expect(screen.getByRole('button', { name: 'Zurück' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Abbrechen' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Weiter' })).toBeInTheDocument();
    expect(screen.getByRole('dialog')).toHaveTextContent('2/3');
  });

  it('renders on the server', () => {
    expect(renderToString(<Flow />)).toContain('Delete workspace');
  });
});
