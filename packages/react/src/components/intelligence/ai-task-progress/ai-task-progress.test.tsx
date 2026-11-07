import { describe, expect, it, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { createRef } from 'react';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import type { AiStep } from '../ai-chain-of-steps/index.js';
import { AiTaskProgress } from './index.js';

const NOW = Date.UTC(2026, 0, 1, 12, 0, 0);
const STARTED = NOW - 3 * 60_000;

const steps: AiStep[] = [
  { id: 'a', label: 'Collect the documents', state: 'done' },
  { id: 'b', label: 'Summarise each one', state: 'active' },
  { id: 'c', label: 'Write the overview', state: 'pending' },
  { id: 'd', label: 'Check the figures', state: 'pending' },
];

describe('AiTaskProgress', () => {
  it('renders a labelled region with status, bar, timing and steps', async () => {
    const ref = createRef<HTMLElement>();
    render(
      <AiTaskProgress
        ref={ref}
        data-testid="root"
        className="extra"
        title="Summarising documents"
        steps={steps}
        startedAt={STARTED}
        now={NOW}
        etaMs={4 * 60_000}
        status="running"
      />,
    );
    const root = screen.getByTestId('root');
    expect(root).toHaveClass('mn-ai-task-progress', 'extra');
    expect(ref.current).toBe(root);
    expect(screen.getByRole('region', { name: 'Summarising documents' })).toBe(root);
    expect(root).toHaveAttribute('data-status', 'running');
    expect(root).toHaveAttribute('aria-busy', 'true');
    expect(root).toHaveTextContent('Running');
    const bar = screen.getByRole('progressbar', { name: 'Summarising documents progress' });
    expect(bar).toHaveAttribute('aria-valuenow', '25');
    expect(bar).toHaveAttribute('aria-valuetext', '25% complete');
    expect(root).toHaveTextContent('Started 3 minutes ago');
    expect(root).toHaveTextContent('About 4 minutes left');
    expect(screen.getByRole('list', { name: 'Steps' })).toBeInTheDocument();
    await expectNoA11yViolations();
  });

  it('uses an explicit progress value and is indeterminate without one or steps', () => {
    const { rerender } = render(
      <AiTaskProgress title="Task" startedAt={STARTED} now={NOW} status="running" progress={0.6} />,
    );
    expect(screen.getByRole('progressbar')).toHaveAttribute('aria-valuenow', '60');
    rerender(<AiTaskProgress title="Task" startedAt={STARTED} now={NOW} status="running" />);
    expect(screen.getByRole('progressbar')).not.toHaveAttribute('aria-valuenow');
  });

  it('offers cancel, minimize and a notify toggle while running', async () => {
    const user = userEvent.setup();
    const onCancel = vi.fn();
    const onMinimize = vi.fn();
    const onNotifyChange = vi.fn();
    render(
      <AiTaskProgress
        title="Task"
        startedAt={STARTED}
        now={NOW}
        status="running"
        onCancel={onCancel}
        onMinimize={onMinimize}
        onNotifyChange={onNotifyChange}
      />,
    );
    const notify = screen.getByRole('button', { name: 'Notify me when done' });
    expect(notify).toHaveAttribute('aria-pressed', 'false');
    await user.click(notify);
    expect(onNotifyChange).toHaveBeenCalledWith(true);
    await user.tab();
    expect(screen.getByRole('button', { name: 'Cancel' })).toHaveFocus();
    await user.keyboard('{Enter}');
    expect(onCancel).toHaveBeenCalledTimes(1);
    await user.click(screen.getByRole('button', { name: 'Minimize' }));
    expect(onMinimize).toHaveBeenCalledTimes(1);
    await expectNoA11yViolations();
  });

  it('hides cancel and notify once the task has finished', () => {
    render(
      <AiTaskProgress
        title="Task"
        startedAt={STARTED}
        now={NOW}
        status="done"
        onCancel={() => {}}
        onNotifyChange={() => {}}
      />,
    );
    expect(screen.queryByRole('button', { name: 'Cancel' })).toBeNull();
    expect(screen.queryByRole('button', { name: 'Notify me when done' })).toBeNull();
    expect(screen.getByRole('progressbar')).toHaveAttribute('aria-valuenow', '100');
  });

  it('announces status changes politely, failures assertively, never the initial status', () => {
    const { rerender } = render(
      <AiTaskProgress title="Import" startedAt={STARTED} now={NOW} status="running" />,
    );
    const status = screen.getByRole('status');
    const alert = screen.getByRole('alert');
    expect(status).toBeEmptyDOMElement();
    rerender(<AiTaskProgress title="Import" startedAt={STARTED} now={NOW} status="done" />);
    expect(status).toHaveTextContent('Import: Done');
    rerender(<AiTaskProgress title="Import" startedAt={STARTED} now={NOW} status="failed" />);
    expect(alert).toHaveTextContent('Import: Failed');
    expect(status).toBeEmptyDOMElement();
  });

  it('accepts translated strings', () => {
    render(
      <AiTaskProgress
        title="Tâche"
        startedAt={STARTED}
        now={NOW}
        status="paused"
        onCancel={() => {}}
        messages={{ statuses: { paused: 'En pause' }, cancel: 'Annuler' }}
      />,
    );
    expect(screen.getByText('En pause')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Annuler' })).toBeInTheDocument();
  });

  it('renders on the server', () => {
    const html = renderToString(
      <AiTaskProgress title="Task" steps={steps} startedAt={STARTED} now={NOW} status="running" />,
    );
    expect(html).toContain('mn-ai-task-progress');
    expect(html).toContain('3 minutes ago');
  });
});
