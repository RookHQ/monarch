import { describe, expect, it, vi } from 'vitest';
import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { createRef } from 'react';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import type { ActivityEvent } from '../../collaboration/activity-timeline/index.js';
import { ActivityWidget } from './index.js';

const now = '2026-11-07T12:00:00Z';
const events: ActivityEvent[] = Array.from({ length: 8 }, (_, i) => ({
  id: `e${i}`,
  actor: i === 2 ? { name: 'Renewal rule', kind: 'automation' as const } : { name: `Person ${i}` },
  verb: 'updated',
  object: `Record ${i}`,
  time: `2026-11-07T${String(11 - i).padStart(2, '0')}:00:00Z`,
}));

describe('ActivityWidget', () => {
  it('is a named region holding the latest events; forwards props and ref', async () => {
    const ref = createRef<HTMLElement>();
    render(
      <ActivityWidget
        ref={ref}
        data-testid="root"
        className="extra"
        events={events}
        now={now}
        locale="en"
        timeZone="UTC"
        viewAllHref="/activity"
      />,
    );
    const root = screen.getByTestId('root');
    expect(root).toHaveClass('mn-widget', 'mn-activity-widget', 'extra');
    expect(ref.current).toBe(root);
    expect(screen.getByRole('region', { name: 'Recent activity' })).toBe(root);
    const list = screen.getByRole('list', { name: 'Recent activity' });
    expect(within(list).getAllByRole('listitem')).toHaveLength(6);
    expect(screen.getByText('Record 0')).toBeInTheDocument();
    expect(screen.queryByText('Record 7')).not.toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'View all: Recent activity' })).toHaveAttribute(
      'href',
      '/activity',
    );
    await expectNoA11yViolations();
  });

  it('tags automated actors so people and rules are told apart', () => {
    render(<ActivityWidget events={events} now={now} locale="en" timeZone="UTC" />);
    expect(screen.getByText('Automated')).toBeInTheDocument();
  });

  it('honours limit, title and groupByDay', () => {
    render(
      <ActivityWidget
        events={events}
        limit={2}
        title="Team activity"
        groupByDay
        now={now}
        locale="en"
        timeZone="UTC"
      />,
    );
    expect(screen.getByRole('region', { name: 'Team activity' })).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Today' })).toBeInTheDocument();
    expect(screen.getAllByRole('listitem')).toHaveLength(2);
  });

  it('shows the empty text', () => {
    render(<ActivityWidget events={[]} now={now} />);
    expect(screen.getByText('No activity yet')).toBeInTheDocument();
  });

  it('shows a loading skeleton and an error with retry', async () => {
    const onRetry = vi.fn();
    const user = userEvent.setup();
    const { rerender } = render(<ActivityWidget data-testid="root" events={[]} loading />);
    expect(screen.getByTestId('root')).toHaveAttribute('aria-busy', 'true');
    rerender(<ActivityWidget events={[]} error="Activity did not load." onRetry={onRetry} />);
    await user.click(screen.getByRole('button', { name: 'Try again' }));
    expect(onRetry).toHaveBeenCalled();
    await expectNoA11yViolations();
  });

  it('renders on the server', () => {
    const html = renderToString(
      <ActivityWidget events={events} now={now} locale="en" timeZone="UTC" />,
    );
    expect(html).toContain('mn-activity-widget');
    expect(html).toContain('Record 1');
  });
});
