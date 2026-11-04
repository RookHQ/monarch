import { useState } from 'react';
import { describe, expect, it, vi } from 'vitest';
import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { ActivityTimeline, type ActivityEvent } from './index.js';

const NOW = '2026-03-02T12:00:00Z';
const EVENTS: ActivityEvent[] = [
  {
    id: 'e1',
    actor: { name: 'Ana Lopez' },
    verb: 'changed the status to',
    object: 'In review',
    time: '2026-03-02T11:00:00Z',
    tone: 'info',
    details: <p>Open → In review</p>,
  },
  {
    id: 'e2',
    actor: { name: 'Auto-assign', kind: 'automation' },
    verb: 'assigned',
    object: 'Ben Okafor',
    time: '2026-03-02T09:00:00Z',
  },
  {
    id: 'e3',
    verb: 'Ticket created from email',
    time: '2026-03-01T15:00:00Z',
    body: <p>Hello, our invoices are missing.</p>,
    meta: <span>Email</span>,
  },
];
const common = { now: NOW, timeZone: 'UTC', locale: 'en-GB' } as const;

describe('ActivityTimeline', () => {
  it('renders ordered lists per day with actor, verb, object and time', async () => {
    render(<ActivityTimeline events={EVENTS} {...common} />);
    expect(screen.getByRole('heading', { name: 'Today', level: 3 })).toBeInTheDocument();
    const today = screen.getByRole('list', { name: 'Today' });
    expect(today.tagName).toBe('OL');
    const items = within(today).getAllByRole('listitem');
    expect(items[0]).toHaveTextContent('Ana Lopez changed the status to In review');
    expect(items[0]!.querySelector('time')).toHaveAttribute('datetime', '2026-03-02T11:00:00.000Z');
    expect(items[0]).toHaveAttribute('data-tone', 'info');
    const yesterday = screen.getByRole('list', { name: 'Yesterday' });
    expect(within(yesterday).getByText('Hello, our invoices are missing.')).toBeInTheDocument();
    expect(within(yesterday).getByText('Email')).toBeInTheDocument();
    await expectNoA11yViolations();
  });

  it('tags automated actors', () => {
    render(<ActivityTimeline events={EVENTS} {...common} />);
    const item = screen.getByText('Auto-assign').closest('li')!;
    expect(within(item).getByText('Automated')).toBeInTheDocument();
    const human = screen.getByText('Ana Lopez').closest('li')!;
    expect(within(human).queryByText('Automated')).toBeNull();
  });

  it('expands details with a disclosure button (uncontrolled)', async () => {
    const onExpandedChange = vi.fn();
    const user = userEvent.setup();
    render(<ActivityTimeline events={EVENTS} onExpandedChange={onExpandedChange} {...common} />);
    const toggle = screen.getByRole('button', { name: 'Show details' });
    expect(toggle).toHaveAttribute('aria-expanded', 'false');
    const region = document.getElementById(toggle.getAttribute('aria-controls')!)!;
    expect(region).not.toBeVisible();
    toggle.focus();
    await user.keyboard('{Enter}');
    expect(toggle).toHaveAttribute('aria-expanded', 'true');
    expect(toggle).toHaveAccessibleName('Hide details');
    expect(region).toBeVisible();
    expect(region).toHaveTextContent('Open → In review');
    expect(onExpandedChange).toHaveBeenLastCalledWith(['e1']);
    await user.keyboard(' ');
    expect(toggle).toHaveAttribute('aria-expanded', 'false');
    expect(onExpandedChange).toHaveBeenLastCalledWith([]);
  });

  it('expanded state can be controlled', async () => {
    function Controlled() {
      const [ids, setIds] = useState<string[]>(['e1']);
      return (
        <>
          <button type="button" onClick={() => setIds([])}>
            Collapse all
          </button>
          <ActivityTimeline
            events={EVENTS}
            expandedIds={ids}
            onExpandedChange={setIds}
            {...common}
          />
        </>
      );
    }
    render(<Controlled />);
    expect(screen.getByText('Open → In review')).toBeVisible();
    await userEvent.click(screen.getByRole('button', { name: 'Collapse all' }));
    expect(screen.getByRole('button', { name: 'Show details' })).toHaveAttribute(
      'aria-expanded',
      'false',
    );
  });

  it('renders the filter slot, an ungrouped list and avatar markers', () => {
    render(
      <ActivityTimeline
        events={EVENTS}
        groupByDay={false}
        markers="avatar"
        filters={<button type="button">Comments only</button>}
        aria-label="Ticket activity"
        {...common}
      />,
    );
    expect(screen.getByRole('button', { name: 'Comments only' })).toBeInTheDocument();
    expect(screen.queryByRole('heading')).toBeNull();
    const list = screen.getByRole('list', { name: 'Ticket activity' });
    expect(within(list).getAllByRole('listitem')).toHaveLength(3);
    const first = within(list).getAllByRole('listitem')[0]!;
    expect(first).toHaveAttribute('data-marker', 'icon');
    expect(first.querySelector('.mn-avatar')).not.toBeNull();
    // No actor: falls back to a dot.
    expect(within(list).getAllByRole('listitem')[2]).toHaveAttribute('data-marker', 'dot');
  });

  it('loading, empty, error and load more', async () => {
    const onLoadMore = vi.fn();
    const { rerender } = render(<ActivityTimeline events={[]} loading {...common} />);
    expect(screen.getByRole('img', { name: 'Loading activity' })).toBeInTheDocument();
    rerender(<ActivityTimeline events={[]} {...common} />);
    expect(screen.getByText('No activity yet')).toBeInTheDocument();
    rerender(<ActivityTimeline events={[]} error={<p>Couldn’t load</p>} {...common} />);
    expect(screen.getByText('Couldn’t load')).toBeInTheDocument();
    rerender(<ActivityTimeline events={EVENTS} hasMore onLoadMore={onLoadMore} {...common} />);
    await userEvent.click(screen.getByRole('button', { name: 'Load more' }));
    expect(onLoadMore).toHaveBeenCalledOnce();
  });

  it('heading level, density and translations', () => {
    render(
      <ActivityTimeline
        events={EVENTS}
        dayHeadingLevel="h4"
        density="compact"
        messages={{ today: 'Hoy', automated: 'Automático' }}
        {...common}
      />,
    );
    expect(screen.getByRole('heading', { name: 'Hoy', level: 4 })).toBeInTheDocument();
    expect(screen.getByText('Automático')).toBeInTheDocument();
    expect(document.querySelector('.mn-activity-timeline')).toHaveAttribute(
      'data-density',
      'compact',
    );
  });

  it('renders on the server', () => {
    const html = renderToString(<ActivityTimeline events={EVENTS} {...common} />);
    expect(html).toContain('mn-activity-timeline');
    expect(html).toContain('Ana Lopez');
  });
});
