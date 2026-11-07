import { describe, expect, it, vi } from 'vitest';
import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { createRef, useState } from 'react';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { AiSuggestedActions, aiActionNeedsApproval, type AiSuggestedAction } from './index.js';

const ACTIONS: AiSuggestedAction[] = [
  { id: 'remind', label: 'Send the renewal reminder', description: 'Renewal is in 14 days.' },
  {
    id: 'task',
    label: 'Create a follow-up task',
    risk: 'medium',
  },
  {
    id: 'close',
    label: 'Close 4 stale tickets',
    risk: 'destructive',
    consequences: ['4 tickets will be closed'],
    confirmLabel: 'Close tickets',
  },
  { id: 'email', label: 'Email the account team', requiresApproval: true },
];

function deferred() {
  let resolve!: () => void;
  let reject!: (e: unknown) => void;
  const promise = new Promise<void>((res, rej) => {
    resolve = res;
    reject = rej;
  });
  return { promise, resolve, reject };
}

describe('aiActionNeedsApproval', () => {
  it('is true for requiresApproval and for destructive risk', () => {
    expect(aiActionNeedsApproval({ id: 'a', label: 'A' })).toBe(false);
    expect(aiActionNeedsApproval({ id: 'a', label: 'A', risk: 'high' })).toBe(false);
    expect(aiActionNeedsApproval({ id: 'a', label: 'A', requiresApproval: true })).toBe(true);
    expect(aiActionNeedsApproval({ id: 'a', label: 'A', risk: 'destructive' })).toBe(true);
  });
});

describe('AiSuggestedActions', () => {
  it('renders a named group marked as AI with one Run button per action', async () => {
    const ref = createRef<HTMLDivElement>();
    render(
      <AiSuggestedActions
        ref={ref}
        data-testid="root"
        className="extra"
        actions={ACTIONS}
        onRun={() => {}}
        onDismiss={() => {}}
      />,
    );
    const root = screen.getByTestId('root');
    expect(root).toHaveClass('mn-ai-suggested-actions', 'extra');
    expect(ref.current).toBe(root);
    expect(screen.getByRole('group', { name: 'Suggested actions' })).toBe(root);
    expect(root).toHaveTextContent('Suggested by AI');
    expect(
      screen.getByRole('heading', { level: 3, name: 'Suggested actions' }),
    ).toBeInTheDocument();
    const run = screen.getByRole('button', { name: 'Run Send the renewal reminder' });
    expect(run).toHaveAccessibleDescription('Renewal is in 14 days.');
    expect(screen.getAllByRole('listitem')).toHaveLength(4);
    expect(
      screen.getByRole('button', { name: 'Dismiss “Send the renewal reminder”' }),
    ).toBeInTheDocument();
    await expectNoA11yViolations();
  });

  it('shows risk and approval as text', () => {
    render(<AiSuggestedActions actions={ACTIONS} onRun={() => {}} />);
    const items = screen.getAllByRole('listitem');
    expect(items[0]).not.toHaveTextContent(/risk|approval/i);
    expect(items[1]).toHaveTextContent('Medium risk');
    expect(items[1]).not.toHaveTextContent('Needs approval');
    expect(items[2]).toHaveTextContent('Destructive');
    expect(items[2]).toHaveTextContent('Needs approval');
    expect(items[3]).toHaveTextContent('Needs approval');
  });

  it('labels rule-based suggestions honestly', () => {
    render(<AiSuggestedActions actions={ACTIONS} onRun={() => {}} origin="rule" />);
    expect(screen.getByRole('group')).toHaveTextContent('Rule-based');
    expect(screen.getByRole('group')).not.toHaveTextContent('Suggested by AI');
  });

  it('runs a direct action from the keyboard and keeps focus on its Done label', async () => {
    const user = userEvent.setup();
    const onRun = vi.fn();
    render(<AiSuggestedActions actions={ACTIONS} onRun={onRun} />);
    await user.tab();
    expect(screen.getByRole('button', { name: 'Run Send the renewal reminder' })).toHaveFocus();
    await user.keyboard('{Enter}');
    expect(onRun).toHaveBeenCalledWith('remind');
    const item = screen.getAllByRole('listitem')[0]!;
    expect(item).toHaveAttribute('data-status', 'done');
    expect(within(item).queryByRole('button')).toBeNull();
    expect(within(item).getByText('Done')).toHaveFocus();
    expect(screen.getByRole('status')).toHaveTextContent('“Send the renewal reminder” done.');
  });

  it('shows Running… while the promise is pending, then failed on rejection', async () => {
    const user = userEvent.setup();
    const d = deferred();
    render(<AiSuggestedActions actions={ACTIONS} onRun={() => d.promise} />);
    await user.click(screen.getByRole('button', { name: 'Run Send the renewal reminder' }));
    const button = screen.getByRole('button', { name: 'Running… Send the renewal reminder' });
    expect(button).toHaveAttribute('aria-busy', 'true');
    d.reject(new Error('nope'));
    expect(await screen.findByRole('alert')).toHaveTextContent('That didn’t work. Try again.');
    expect(screen.getAllByRole('listitem')[0]).toHaveAttribute('data-status', 'failed');
    expect(screen.getByRole('button', { name: 'Run Send the renewal reminder' })).toBeEnabled();
  });

  it('asks for approval before running, and cancelling runs nothing and returns focus', async () => {
    const user = userEvent.setup();
    const onRun = vi.fn();
    render(<AiSuggestedActions actions={ACTIONS} onRun={onRun} />);
    const run = screen.getByRole('button', { name: 'Run Email the account team' });
    await user.click(run);
    const dialog = await screen.findByRole('alertdialog', {
      name: 'Run “Email the account team”?',
    });
    expect(dialog).toHaveTextContent('Check what this action will do before it runs.');
    expect(within(dialog).getByRole('button', { name: 'Cancel' })).toHaveFocus();
    await user.keyboard('{Escape}');
    await waitFor(() => expect(screen.queryByRole('alertdialog')).toBeNull());
    expect(onRun).not.toHaveBeenCalled();
    expect(run).toHaveFocus();
  });

  it('runs a destructive action only after a deliberate confirm, in the danger tone', async () => {
    const user = userEvent.setup();
    const onRun = vi.fn(() => Promise.resolve());
    render(<AiSuggestedActions actions={ACTIONS} onRun={onRun} />);
    await user.click(screen.getByRole('button', { name: 'Run Close 4 stale tickets' }));
    const dialog = await screen.findByRole('alertdialog', { name: 'Run “Close 4 stale tickets”?' });
    expect(dialog).toHaveTextContent('4 tickets will be closed');
    const confirm = within(dialog).getByRole('button', { name: 'Close tickets' });
    expect(confirm).toHaveAttribute('data-variant', 'danger');
    expect(confirm).not.toHaveFocus();
    await expectNoA11yViolations();
    await user.click(confirm);
    expect(onRun).toHaveBeenCalledWith('close');
    await waitFor(() => expect(screen.queryByRole('alertdialog')).toBeNull());
    const item = screen.getAllByRole('listitem')[2]!;
    expect(item).toHaveAttribute('data-status', 'done');
    await waitFor(() => expect(within(item).getByText('Done')).toHaveFocus());
  });

  it('shows the error inside the approval dialog when the run fails', async () => {
    const user = userEvent.setup();
    render(
      <AiSuggestedActions
        actions={ACTIONS}
        onRun={() => Promise.reject(new Error('Mail is down.'))}
      />,
    );
    await user.click(screen.getByRole('button', { name: 'Run Email the account team' }));
    const dialog = await screen.findByRole('alertdialog');
    await user.click(within(dialog).getByRole('button', { name: 'Run action' }));
    expect(await within(dialog).findByText('Mail is down.')).toBeInTheDocument();
    expect(screen.getByRole('alertdialog')).toBeInTheDocument();
  });

  it('moves focus to the next action when one is dismissed', async () => {
    const user = userEvent.setup();
    function Demo() {
      const [actions, setActions] = useState(ACTIONS);
      return (
        <AiSuggestedActions
          actions={actions}
          onRun={() => {}}
          onDismiss={(id) => setActions((a) => a.filter((x) => x.id !== id))}
        />
      );
    }
    render(<Demo />);
    await user.click(screen.getByRole('button', { name: 'Dismiss “Send the renewal reminder”' }));
    expect(screen.getByRole('button', { name: 'Run Create a follow-up task' })).toHaveFocus();
    await user.click(screen.getByRole('button', { name: 'Dismiss “Email the account team”' }));
    expect(screen.getByRole('button', { name: 'Run Close 4 stale tickets' })).toHaveFocus();
    await user.click(screen.getByRole('button', { name: 'Dismiss “Create a follow-up task”' }));
    await user.click(screen.getByRole('button', { name: 'Dismiss “Close 4 stale tickets”' }));
    expect(screen.getByRole('heading', { name: 'Suggested actions' })).toHaveFocus();
    expect(screen.getByText('No suggestions right now.')).toBeInTheDocument();
  });

  it('respects a controlled status', () => {
    render(
      <AiSuggestedActions
        actions={[
          { id: 'a', label: 'A', status: 'running' },
          { id: 'b', label: 'B', status: 'done' },
        ]}
        onRun={() => {}}
      />,
    );
    expect(screen.getByRole('button', { name: 'Running… A' })).toHaveAttribute('aria-busy', 'true');
    expect(screen.getAllByRole('listitem')[1]).toHaveTextContent('Done');
  });

  it('accepts a title, heading level and messages', () => {
    render(
      <AiSuggestedActions
        actions={[{ id: 'a', label: 'Relancer', risk: 'high' }]}
        onRun={() => {}}
        title="Prochaines étapes"
        headingLevel={2}
        messages={{
          run: 'Lancer',
          risk: { high: 'Risque élevé' },
          origin: { ai: 'Proposé par l’IA' },
        }}
      />,
    );
    expect(
      screen.getByRole('heading', { level: 2, name: 'Prochaines étapes' }),
    ).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Lancer Relancer' })).toBeInTheDocument();
    expect(screen.getByRole('group')).toHaveTextContent('Risque élevé');
    expect(screen.getByRole('group')).toHaveTextContent('Proposé par l’IA');
  });

  it('renders on the server', () => {
    const html = renderToString(<AiSuggestedActions actions={ACTIONS} onRun={() => {}} />);
    expect(html).toContain('mn-ai-suggested-actions');
    expect(html).toContain('Send the renewal reminder');
    expect(html).toContain('Suggested by AI');
  });
});
