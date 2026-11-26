import { afterEach, describe, expect, it, vi } from 'vitest';
import { act, render, renderHook, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { createRef, useState } from 'react';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import {
  AiBackgroundTasks,
  formatAiBackgroundTaskElapsed,
  groupAiBackgroundTasks,
  type AiBackgroundTask,
  useAiRunNotifications,
} from './index.js';

const NOW = '2026-01-01T12:00:00.000Z';

const TASKS: AiBackgroundTask[] = [
  {
    id: 'done',
    title: 'Draft the release notes',
    agent: 'Writing agent',
    status: 'succeeded',
    startedAt: '2026-01-01T11:40:00.000Z',
    endedAt: '2026-01-01T11:55:00.000Z',
    result: 'Draft ready, 420 words.',
  },
  {
    id: 'run',
    title: 'Summarise 40 contracts',
    agent: 'Research agent',
    status: 'running',
    progress: 0.4,
    step: 'Reading contract 16 of 40',
    startedAt: '2026-01-01T11:56:48.000Z',
  },
  { id: 'queue', title: 'Translate the summary', status: 'queued' },
  {
    id: 'ask',
    title: 'Book the venue',
    agent: 'Planning agent',
    status: 'needs-input',
    step: 'Which date do you prefer?',
    startedAt: '2026-01-01T11:50:00.000Z',
  },
  {
    id: 'fail',
    title: 'Import the spreadsheet',
    status: 'failed',
    startedAt: '2026-01-01T11:30:00.000Z',
    endedAt: '2026-01-01T11:31:00.000Z',
    error: 'The file could not be read.',
  },
  { id: 'index', title: 'Index the workspace', status: 'running' },
];

afterEach(() => {
  vi.useRealTimers();
  vi.unstubAllGlobals();
});

describe('groupAiBackgroundTasks', () => {
  it('groups and sorts without changing the input', () => {
    const input = [...TASKS];
    const groups = groupAiBackgroundTasks(input);
    expect(input).toEqual(TASKS);
    expect(groups.needsInput.map((t) => t.id)).toEqual(['ask']);
    // Running before queued; a task without a start time after one with.
    expect(groups.running.map((t) => t.id)).toEqual(['run', 'index', 'queue']);
    // Most recently ended first.
    expect(groups.finished.map((t) => t.id)).toEqual(['done', 'fail']);
  });

  it('puts the longest wait first and tasks without a usable time last', () => {
    const groups = groupAiBackgroundTasks([
      { id: 'b', title: 'B', status: 'needs-input', startedAt: '2026-01-01T11:00:00Z' },
      { id: 'x', title: 'X', status: 'needs-input', startedAt: 'not a date' },
      { id: 'a', title: 'A', status: 'needs-input', startedAt: '2026-01-01T10:00:00Z' },
      { id: 'c', title: 'C', status: 'cancelled' },
      { id: 'd', title: 'D', status: 'cancelled', endedAt: '2026-01-01T09:00:00Z' },
    ]);
    expect(groups.needsInput.map((t) => t.id)).toEqual(['a', 'b', 'x']);
    expect(groups.finished.map((t) => t.id)).toEqual(['d', 'c']);
    expect(groupAiBackgroundTasks([])).toEqual({ needsInput: [], running: [], finished: [] });
  });
});

describe('formatAiBackgroundTaskElapsed', () => {
  it('formats seconds, minutes and hours', () => {
    expect(formatAiBackgroundTaskElapsed(42_000, 'en')).toBe('42 sec');
    expect(formatAiBackgroundTaskElapsed(192_000, 'en')).toBe('3 min 12 sec');
    expect(formatAiBackgroundTaskElapsed(3_840_000, 'en')).toBe('1 hr 4 min');
    expect(formatAiBackgroundTaskElapsed(-5_000, 'en')).toBe('0 sec');
  });
});

describe('AiBackgroundTasks', () => {
  it('renders a trigger with counts in text and forwards ref, class and props', async () => {
    const ref = createRef<HTMLDivElement>();
    render(
      <AiBackgroundTasks
        ref={ref}
        data-testid="root"
        className="extra"
        tasks={TASKS}
        now={NOW}
        locale="en"
      />,
    );
    const root = screen.getByTestId('root');
    expect(root).toHaveClass('mn-ai-background-tasks', 'extra');
    expect(ref.current).toBe(root);
    expect(root).toHaveAttribute('data-busy', 'true');
    expect(root).toHaveAttribute('data-attention', 'true');
    const trigger = screen.getByRole('button', {
      name: 'Background tasks 2 running 1 needs you',
    });
    expect(trigger).toHaveAttribute('aria-expanded', 'false');
    expect(screen.queryByRole('dialog')).toBeNull();
    await expectNoA11yViolations();
  });

  it('shows the queued count when nothing runs, and no badge when idle', () => {
    const { rerender } = render(
      <AiBackgroundTasks tasks={[{ id: 'q', title: 'Queued task', status: 'queued' }]} />,
    );
    expect(screen.getByRole('button', { name: 'Background tasks 1 queued' })).toBeInTheDocument();
    rerender(<AiBackgroundTasks tasks={[]} />);
    expect(screen.getByRole('button', { name: 'Background tasks' })).toBeInTheDocument();
  });

  it('opens from the keyboard, focuses the named panel and closes with Escape', async () => {
    const user = userEvent.setup();
    const onOpenChange = vi.fn();
    render(<AiBackgroundTasks tasks={TASKS} now={NOW} locale="en" onOpenChange={onOpenChange} />);
    await user.tab();
    const trigger = screen.getByRole('button', { name: /Background tasks/ });
    expect(trigger).toHaveFocus();
    await user.keyboard('{Enter}');
    const panel = screen.getByRole('dialog', { name: 'Background tasks' });
    expect(panel).toHaveFocus();
    expect(onOpenChange).toHaveBeenLastCalledWith(true);
    expect(trigger).toHaveAttribute('aria-expanded', 'true');
    await expectNoA11yViolations();
    await user.keyboard('{Escape}');
    expect(screen.queryByRole('dialog')).toBeNull();
    expect(onOpenChange).toHaveBeenLastCalledWith(false);
    expect(trigger).toHaveFocus();
  });

  it('is controllable through open and onOpenChange', async () => {
    const user = userEvent.setup();
    const onOpenChange = vi.fn();
    const { rerender } = render(
      <AiBackgroundTasks tasks={TASKS} now={NOW} open={false} onOpenChange={onOpenChange} />,
    );
    await user.click(screen.getByRole('button', { name: /Background tasks/ }));
    expect(onOpenChange).toHaveBeenCalledWith(true);
    expect(screen.queryByRole('dialog')).toBeNull();
    rerender(<AiBackgroundTasks tasks={TASKS} now={NOW} open onOpenChange={onOpenChange} />);
    expect(screen.getByRole('dialog', { name: 'Background tasks' })).toBeInTheDocument();
  });

  it('groups tasks under headed lists with status text, agent, bar and timing', async () => {
    render(<AiBackgroundTasks inline tasks={TASKS} now={NOW} locale="en" />);
    expect(screen.getByRole('group', { name: 'Background tasks' })).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /Background tasks/ })).toBeNull();
    expect(screen.getAllByRole('heading').map((h) => h.textContent)).toEqual([
      'Background tasks',
      'Needs you (1)',
      'Running (3)',
      'Finished (2)',
    ]);

    const running = screen.getByRole('list', { name: 'Running (3)' });
    const rows = within(running).getAllByRole('listitem');
    expect(rows).toHaveLength(3);
    const first = rows[0]!;
    expect(first).toHaveAttribute('data-status', 'running');
    expect(first).toHaveTextContent('Summarise 40 contracts');
    expect(first).toHaveTextContent('Research agent');
    expect(first).toHaveTextContent('Running for 3 min 12 sec');
    expect(first).toHaveTextContent('Reading contract 16 of 40');
    const bar = within(first).getByRole('progressbar', { name: 'Summarise 40 contracts progress' });
    expect(bar).toHaveAttribute('aria-valuenow', '40');
    expect(bar).toHaveAttribute('aria-valuetext', '40%');

    // Unknown progress: an indeterminate bar. Queued: status text, no bar.
    expect(within(rows[1]!).getByRole('progressbar')).not.toHaveAttribute('aria-valuenow');
    expect(rows[2]!).toHaveTextContent('Queued');
    expect(within(rows[2]!).queryByRole('progressbar')).toBeNull();

    const finished = screen.getByRole('list', { name: 'Finished (2)' });
    expect(finished).toHaveTextContent('Done');
    expect(finished).toHaveTextContent('Draft ready, 420 words.');
    expect(finished).toHaveTextContent('Ended 5 minutes ago');
    expect(finished).toHaveTextContent('Failed');
    expect(finished).toHaveTextContent('The file could not be read.');
    expect(within(finished).queryByRole('progressbar')).toBeNull();
    await expectNoA11yViolations();
  });

  it('honours indeterminate over a progress value', () => {
    render(
      <AiBackgroundTasks
        inline
        now={NOW}
        tasks={[{ id: 'a', title: 'Task', status: 'running', progress: 0.5, indeterminate: true }]}
      />,
    );
    expect(screen.getByRole('progressbar')).not.toHaveAttribute('aria-valuenow');
  });

  it('offers each action only where it applies and reports the task', async () => {
    const user = userEvent.setup();
    const on = {
      onOpen: vi.fn(),
      onCancel: vi.fn(),
      onRetry: vi.fn(),
      onRespond: vi.fn(),
      onDismiss: vi.fn(),
      onClearFinished: vi.fn(),
    };
    render(<AiBackgroundTasks inline tasks={TASKS} now={NOW} locale="en" {...on} />);
    const byId = (id: string) => TASKS.find((t) => t.id === id);

    expect(screen.getAllByRole('button', { name: /^Open: / })).toHaveLength(TASKS.length);
    expect(screen.getAllByRole('button', { name: /^Cancel: / })).toHaveLength(4);
    expect(screen.getAllByRole('button', { name: /^Retry: / })).toHaveLength(1);
    expect(screen.getAllByRole('button', { name: /^Respond: / })).toHaveLength(1);
    expect(screen.getAllByRole('button', { name: /^Dismiss: / })).toHaveLength(2);

    await user.click(screen.getByRole('button', { name: 'Respond: Book the venue' }));
    expect(on.onRespond).toHaveBeenCalledWith(byId('ask'));
    await user.click(screen.getByRole('button', { name: 'Cancel: Summarise 40 contracts' }));
    expect(on.onCancel).toHaveBeenCalledWith(byId('run'));
    await user.click(screen.getByRole('button', { name: 'Cancel: Translate the summary' }));
    expect(on.onCancel).toHaveBeenLastCalledWith(byId('queue'));
    await user.click(screen.getByRole('button', { name: 'Retry: Import the spreadsheet' }));
    expect(on.onRetry).toHaveBeenCalledWith(byId('fail'));
    await user.click(screen.getByRole('button', { name: 'Open: Draft the release notes' }));
    expect(on.onOpen).toHaveBeenCalledWith(byId('done'));
    await user.click(screen.getByRole('button', { name: 'Dismiss: Draft the release notes' }));
    expect(on.onDismiss).toHaveBeenCalledWith(byId('done'));
    await user.click(screen.getByRole('button', { name: 'Clear finished' }));
    expect(on.onClearFinished).toHaveBeenCalledWith([byId('done'), byId('fail')]);
    for (const fn of Object.values(on)) expect(fn.mock.calls.length).toBeLessThanOrEqual(2);
    await expectNoA11yViolations();
  });

  it('renders no action buttons without callbacks, and nothing runs on its own', () => {
    render(<AiBackgroundTasks inline tasks={TASKS} now={NOW} />);
    expect(screen.queryAllByRole('button')).toHaveLength(0);
  });

  it('works from the keyboard and keeps focus in the panel when a row goes away', async () => {
    const user = userEvent.setup();
    function Tray() {
      const [tasks, setTasks] = useState(TASKS.filter((t) => t.id === 'done' || t.id === 'run'));
      return (
        <AiBackgroundTasks
          inline
          tasks={tasks}
          now={NOW}
          onDismiss={(task) => setTasks((all) => all.filter((t) => t.id !== task.id))}
          onCancel={() => {}}
        />
      );
    }
    render(<Tray />);
    await user.tab();
    expect(screen.getByRole('button', { name: 'Cancel: Summarise 40 contracts' })).toHaveFocus();
    await user.tab();
    const dismiss = screen.getByRole('button', { name: 'Dismiss: Draft the release notes' });
    expect(dismiss).toHaveFocus();
    await user.keyboard(' ');
    expect(screen.queryByText('Draft the release notes')).toBeNull();
    expect(screen.getByRole('group', { name: 'Background tasks' })).toHaveFocus();
  });

  it('does not pull focus into the panel on a later update after an action that kept its button', async () => {
    const user = userEvent.setup();
    const onOpen = vi.fn();
    const one = TASKS.filter((t) => t.id === 'run');
    const { rerender } = render(<AiBackgroundTasks inline tasks={one} now={NOW} onOpen={onOpen} />);
    await user.click(screen.getByRole('button', { name: 'Open: Summarise 40 contracts' }));
    expect(onOpen).toHaveBeenCalledTimes(1);
    // The person moves on; a progress update arrives later.
    (document.activeElement as HTMLElement).blur();
    expect(document.body).toHaveFocus();
    rerender(
      <AiBackgroundTasks
        inline
        tasks={one.map((t) => ({ ...t, progress: 0.6 }))}
        now={NOW}
        onOpen={onOpen}
      />,
    );
    expect(document.body).toHaveFocus();
    expect(screen.getByRole('group', { name: 'Background tasks' })).not.toHaveFocus();
  });

  it('lets task text written by a model set its own direction', () => {
    render(<AiBackgroundTasks inline tasks={TASKS} now={NOW} />);
    expect(screen.getByText('Summarise 40 contracts')).toHaveAttribute('dir', 'auto');
    expect(screen.getByText('The file could not be read.')).toHaveAttribute('dir', 'auto');
  });

  it('announces a task finishing, failing or needing input once, never progress', () => {
    const base: AiBackgroundTask[] = [
      { id: 'a', title: 'Import', status: 'running', progress: 0.1 },
      { id: 'b', title: 'Export', status: 'running' },
      { id: 'c', title: 'Old task', status: 'failed' },
    ];
    const { rerender } = render(<AiBackgroundTasks tasks={base} now={NOW} />);
    const status = screen.getByRole('status');
    expect(status).toBeEmptyDOMElement();

    // Progress and step ticks are silent.
    rerender(
      <AiBackgroundTasks
        now={NOW}
        tasks={[{ ...base[0]!, progress: 0.6, step: 'Step 2' }, base[1]!, base[2]!]}
      />,
    );
    expect(status).toBeEmptyDOMElement();

    // A task that arrives already finished is not news; a cancel is not announced.
    rerender(
      <AiBackgroundTasks
        now={NOW}
        tasks={[
          { ...base[0]!, status: 'succeeded' },
          { ...base[1]!, status: 'cancelled' },
          base[2]!,
          { id: 'd', title: 'Arrived done', status: 'succeeded' },
        ]}
      />,
    );
    expect(status).toHaveTextContent(/^Import: Done$/);

    rerender(
      <AiBackgroundTasks
        now={NOW}
        tasks={[
          { ...base[0]!, status: 'succeeded' },
          { ...base[1]!, status: 'needs-input' },
          { ...base[2]!, status: 'running' },
        ]}
      />,
    );
    expect(status).toHaveTextContent(/^Export: Needs your input$/);

    rerender(
      <AiBackgroundTasks
        now={NOW}
        tasks={[
          { ...base[0]!, status: 'succeeded' },
          { ...base[1]!, status: 'failed' },
          { ...base[2]!, status: 'succeeded' },
        ]}
      />,
    );
    expect(status).toHaveTextContent(/^Export: Failed\. Old task: Done$/);
    expect(status).toHaveAttribute('aria-live', 'polite');
  });

  it('ticks the elapsed time while a task runs and stops when none does', () => {
    vi.useFakeTimers();
    vi.setSystemTime(new Date('2026-01-01T12:00:00.000Z'));
    const running: AiBackgroundTask[] = [
      { id: 'a', title: 'Import', status: 'running', startedAt: '2026-01-01T11:59:30.000Z' },
    ];
    const { rerender } = render(<AiBackgroundTasks inline tasks={running} locale="en" />);
    expect(screen.getByText('Running for 30 sec')).toBeInTheDocument();
    act(() => {
      vi.advanceTimersByTime(2000);
    });
    expect(screen.getByText('Running for 32 sec')).toBeInTheDocument();
    expect(vi.getTimerCount()).toBe(1);

    rerender(
      <AiBackgroundTasks
        inline
        locale="en"
        tasks={[{ ...running[0]!, status: 'succeeded', endedAt: '2026-01-01T12:00:02.000Z' }]}
      />,
    );
    expect(vi.getTimerCount()).toBe(0);
  });

  it('does not tick while the popover is closed, with a fixed now, or under reduced motion', () => {
    vi.useFakeTimers();
    vi.setSystemTime(new Date('2026-01-01T12:00:00.000Z'));
    const running: AiBackgroundTask[] = [
      { id: 'a', title: 'Import', status: 'running', startedAt: '2026-01-01T11:59:30.000Z' },
    ];
    const closed = render(<AiBackgroundTasks tasks={running} />);
    expect(vi.getTimerCount()).toBe(0);
    closed.unmount();

    const fixed = render(<AiBackgroundTasks inline tasks={running} now={NOW} locale="en" />);
    expect(vi.getTimerCount()).toBe(0);
    fixed.unmount();

    vi.stubGlobal('matchMedia', (query: string) => ({
      matches: query.includes('prefers-reduced-motion'),
      media: query,
      addEventListener: () => {},
      removeEventListener: () => {},
    }));
    render(<AiBackgroundTasks inline tasks={running} locale="en" />);
    expect(screen.getByText('Running for 30 sec')).toBeInTheDocument();
    expect(vi.getTimerCount()).toBe(0);
    act(() => {
      vi.advanceTimersByTime(5000);
    });
    expect(screen.getByText('Running for 30 sec')).toBeInTheDocument();
  });

  it('shows an empty state and hides Clear finished when nothing has finished', () => {
    const { rerender } = render(<AiBackgroundTasks inline tasks={[]} onClearFinished={() => {}} />);
    expect(screen.getByText('No background tasks')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Clear finished' })).toBeNull();
    expect(screen.queryByRole('list')).toBeNull();
    rerender(
      <AiBackgroundTasks
        inline
        tasks={[{ id: 'a', title: 'Import', status: 'running' }]}
        onClearFinished={() => {}}
      />,
    );
    expect(screen.queryByRole('button', { name: 'Clear finished' })).toBeNull();
  });

  it('accepts translated strings, a locale and a heading level', () => {
    render(
      <AiBackgroundTasks
        inline
        tasks={TASKS}
        now={NOW}
        locale="fr"
        headingLevel={3}
        onRespond={() => {}}
        onClearFinished={() => {}}
        messages={{
          title: 'Tâches en arrière-plan',
          groups: { 'needs-input': 'À vous' },
          statuses: { running: 'En cours' },
          respond: 'Répondre',
          clearFinished: 'Effacer les tâches terminées',
          elapsed: (d) => `Depuis ${d}`,
        }}
      />,
    );
    expect(
      screen.getByRole('heading', { level: 3, name: 'Tâches en arrière-plan' }),
    ).toBeInTheDocument();
    expect(screen.getByRole('heading', { level: 4, name: 'À vous (1)' })).toBeInTheDocument();
    // Untranslated keys keep their English defaults.
    expect(screen.getByRole('heading', { level: 4, name: 'Finished (2)' })).toBeInTheDocument();
    expect(screen.getAllByText('En cours')).toHaveLength(2);
    expect(screen.getByRole('button', { name: 'Répondre: Book the venue' })).toBeInTheDocument();
    expect(
      screen.getByRole('button', { name: 'Effacer les tâches terminées' }),
    ).toBeInTheDocument();
    expect(screen.getByText(/^Depuis 3\smin/)).toBeInTheDocument();
    expect(screen.getByText(/il y a 5\sminutes/)).toBeInTheDocument();
  });

  it('renders on the server, with and without a reference time', () => {
    const closed = renderToString(<AiBackgroundTasks tasks={TASKS} now={NOW} />);
    expect(closed).toContain('mn-ai-background-tasks');
    expect(closed).toContain('2 running');
    const inline = renderToString(<AiBackgroundTasks inline tasks={TASKS} now={NOW} locale="en" />);
    expect(inline).toContain('Running for 3 min 12 sec');
    // No clock on the server: elapsed text waits for the client rather than mismatching.
    const noClock = renderToString(<AiBackgroundTasks inline tasks={TASKS} locale="en" />);
    expect(noClock).toContain('Summarise 40 contracts');
    expect(noClock).not.toContain('Running for');
  });
});

describe('useAiRunNotifications', () => {
  afterEach(() => {
    vi.unstubAllGlobals();
    Object.defineProperty(document, 'visibilityState', { configurable: true, value: 'visible' });
  });

  function stubNotification(permission: NotificationPermission) {
    const shown: { title: string; options?: NotificationOptions }[] = [];
    class FakeNotification {
      static permission = permission;
      static requestPermission = vi.fn(async () => {
        FakeNotification.permission = 'granted';
        return 'granted' as NotificationPermission;
      });
      onclick: (() => void) | null = null;
      constructor(title: string, options?: NotificationOptions) {
        shown.push({ title, ...(options ? { options } : {}) });
      }
      close() {}
    }
    vi.stubGlobal('Notification', FakeNotification);
    return { shown, FakeNotification };
  }

  it('uses a system notification only while hidden and allowed; otherwise the in-app fallback', async () => {
    const { shown, FakeNotification } = stubNotification('default');
    const onInApp = vi.fn();
    const { result } = renderHook(() => useAiRunNotifications({ onInApp }));
    expect(result.current.permission).toBe('default');
    expect(result.current.notify({ id: 't1', title: 'Report', body: 'Done' })).toBe('in-app');
    await act(async () => {
      await result.current.requestPermission();
    });
    expect(FakeNotification.requestPermission).toHaveBeenCalledTimes(1);
    expect(result.current.permission).toBe('granted');
    // Visible page: still in-app.
    expect(result.current.notify({ id: 't1', title: 'Report' })).toBe('in-app');
    Object.defineProperty(document, 'visibilityState', { configurable: true, value: 'hidden' });
    expect(result.current.notify({ id: 't1', title: 'Report', body: 'Done' })).toBe('system');
    expect(shown).toEqual([{ title: 'Report', options: { tag: 't1', body: 'Done' } }]);
  });

  it('reports unsupported without the Notification API', () => {
    vi.stubGlobal('Notification', undefined);
    const { result } = renderHook(() => useAiRunNotifications());
    expect(result.current.permission).toBe('unsupported');
    expect(result.current.notify({ id: 'x', title: 'y' })).toBe('none');
  });

  it('AiBackgroundTasks calls notify when a task finishes', () => {
    const notify = vi.fn();
    const task: AiBackgroundTask = {
      id: 'n1',
      title: 'Summarise',
      status: 'running',
      startedAt: NOW,
    };
    const { rerender } = render(
      <AiBackgroundTasks tasks={[task]} inline notify={notify} now={NOW} />,
    );
    rerender(
      <AiBackgroundTasks
        tasks={[{ ...task, status: 'succeeded', endedAt: NOW, result: '40 summaries' }]}
        inline
        notify={notify}
        now={NOW}
      />,
    );
    expect(notify).toHaveBeenCalledWith({
      id: 'n1',
      title: 'Summarise',
      body: expect.stringContaining('40 summaries'),
    });
  });
});
