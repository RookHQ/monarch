import { describe, expect, it, vi } from 'vitest';
import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { createRef } from 'react';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { TodayPanel, type TodayTask } from './index.js';

const today = '2026-11-07';
const events = [
  { id: 'e1', title: 'Weekly review', start: '2026-11-07T10:00:00Z', end: '2026-11-07T10:30:00Z' },
  { id: 'e2', title: 'Planning', start: '2026-11-07T14:00:00Z', end: '2026-11-07T15:00:00Z' },
];
const tasks: TodayTask[] = [
  { id: 't1', title: 'Send the proposal', detail: 'Northwind', href: '/tasks/t1' },
  { id: 't2', title: 'Approve 3 requests', overdue: true, due: '2 days late' },
  { id: 't3', title: 'Update the forecast', done: true },
];

describe('TodayPanel', () => {
  it('is a region with today’s date, agenda and tasks; forwards props and ref', async () => {
    const ref = createRef<HTMLElement>();
    render(
      <TodayPanel
        ref={ref}
        data-testid="root"
        className="extra"
        today={today}
        timeZone="UTC"
        locale="en-GB"
        events={events}
        tasks={tasks}
      />,
    );
    const root = screen.getByTestId('root');
    expect(root).toHaveClass('mn-today-panel', 'extra');
    expect(ref.current).toBe(root);
    expect(screen.getByRole('region', { name: 'Today' })).toBe(root);
    expect(root.querySelector('time.mn-today-panel__date')).toHaveTextContent(
      'Saturday 7 November',
    );
    expect(screen.getByRole('heading', { level: 3, name: 'Schedule' })).toBeInTheDocument();
    expect(screen.getByText('Weekly review')).toBeInTheDocument();
    expect(screen.getByRole('heading', { level: 3, name: 'Due today' })).toBeInTheDocument();
    await expectNoA11yViolations();
  });

  it('lists overdue tasks first with an Overdue tag and links tasks with href', () => {
    render(<TodayPanel today={today} tasks={tasks} />);
    const items = within(screen.getByRole('list')).getAllByRole('listitem');
    expect(items[0]).toHaveTextContent('Approve 3 requests');
    expect(within(items[0]!).getByText('Overdue')).toBeInTheDocument();
    expect(items[0]).toHaveTextContent('2 days late');
    expect(screen.getByRole('link', { name: 'Send the proposal' })).toHaveAttribute(
      'href',
      '/tasks/t1',
    );
  });

  it('ticks tasks off with named checkboxes', async () => {
    const onTaskToggle = vi.fn();
    const user = userEvent.setup();
    render(<TodayPanel today={today} tasks={tasks} onTaskToggle={onTaskToggle} />);
    const box = screen.getByRole('checkbox', { name: 'Mark "Send the proposal" done' });
    expect(screen.getByRole('checkbox', { name: 'Mark "Update the forecast" done' })).toBeChecked();
    box.focus();
    await user.keyboard(' ');
    expect(onTaskToggle).toHaveBeenCalledWith('t1', true);
    await expectNoA11yViolations();
  });

  it('limits tasks and links to the rest', () => {
    render(<TodayPanel today={today} tasks={tasks} maxTasks={1} tasksHref="/my-work" />);
    expect(within(screen.getByRole('list')).getAllByRole('listitem')).toHaveLength(1);
    expect(screen.getByRole('link', { name: '2 more' })).toHaveAttribute('href', '/my-work');
  });

  it('shows a single empty state when nothing is scheduled or due, and per-section text otherwise', () => {
    const { rerender } = render(
      <TodayPanel data-testid="root" today={today} events={[]} tasks={[]} />,
    );
    expect(screen.getByTestId('root')).toHaveAttribute('data-state', 'empty');
    expect(screen.getByText('Nothing scheduled and nothing due today.')).toBeInTheDocument();
    rerender(<TodayPanel today={today} events={[]} tasks={tasks} />);
    expect(screen.getByText('Nothing scheduled')).toBeInTheDocument();
  });

  it('shows loading and error states', async () => {
    const onRetry = vi.fn();
    const user = userEvent.setup();
    const { rerender } = render(<TodayPanel data-testid="root" today={today} loading />);
    expect(screen.getByTestId('root')).toHaveAttribute('aria-busy', 'true');
    expect(screen.getByRole('img', { name: 'Loading today' })).toBeInTheDocument();
    rerender(<TodayPanel today={today} error="Today did not load." onRetry={onRetry} />);
    await user.click(screen.getByRole('button', { name: 'Try again' }));
    expect(onRetry).toHaveBeenCalled();
  });

  it('honours title, headingLevel and messages', () => {
    render(
      <TodayPanel
        today={today}
        tasks={tasks}
        title="Needs you today"
        headingLevel={3}
        showDate={false}
        messages={{ tasks: 'Tareas', overdue: 'Atrasada' }}
      />,
    );
    expect(screen.getByRole('heading', { level: 3, name: 'Needs you today' })).toBeInTheDocument();
    expect(screen.getByRole('heading', { level: 4, name: 'Tareas' })).toBeInTheDocument();
    expect(screen.getByText('Atrasada')).toBeInTheDocument();
  });

  it('renders on the server', () => {
    const html = renderToString(
      <TodayPanel today={today} timeZone="UTC" locale="en" events={events} tasks={tasks} />,
    );
    expect(html).toContain('mn-today-panel');
    expect(html).toContain('Weekly review');
    expect(html).toContain('Approve 3 requests');
  });
});
