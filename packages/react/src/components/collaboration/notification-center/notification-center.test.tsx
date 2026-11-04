import { useState } from 'react';
import { describe, expect, it, vi } from 'vitest';
import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { NotificationCenter, type NotificationItem } from './index.js';

const NOW = '2026-03-02T12:00:00Z';
const ITEMS: NotificationItem[] = [
  {
    id: 'n1',
    title: 'Ana Lopez mentioned you in Launch plan',
    description: 'Can you review the timeline?',
    time: '2026-03-02T11:30:00Z',
    actor: { name: 'Ana Lopez' },
  },
  {
    id: 'n2',
    title: 'Ben Okafor assigned you a task',
    time: '2026-03-02T08:00:00Z',
    actor: { name: 'Ben Okafor' },
    read: true,
  },
  {
    id: 'n3',
    title: 'Weekly export is ready',
    time: '2026-03-01T09:00:00Z',
    href: '/exports/42',
  },
];

const common = { now: NOW, timeZone: 'UTC', locale: 'en-GB' } as const;
const dialog = () => screen.getByRole('dialog', { name: 'Notifications' });

describe('NotificationCenter', () => {
  it('names the bell with the unread count and opens a labelled panel', async () => {
    render(<NotificationCenter items={ITEMS} {...common} />);
    const bell = screen.getByRole('button', { name: 'Notifications, 2 unread' });
    expect(bell).toHaveAttribute('aria-expanded', 'false');
    expect(bell.querySelector('.mn-notification-center__badge')).toHaveTextContent('2');
    await userEvent.click(bell);
    expect(dialog()).toBeInTheDocument();
    expect(bell).toHaveAttribute('aria-expanded', 'true');
    await expectNoA11yViolations();
  });

  it('caps the badge at 99+ and uses unreadCount from the server', () => {
    render(<NotificationCenter items={ITEMS} unreadCount={240} />);
    const bell = screen.getByRole('button', { name: 'Notifications, 240 unread' });
    expect(bell).toHaveTextContent('99+');
  });

  it('has no badge when everything is read', () => {
    render(<NotificationCenter items={[{ ...ITEMS[1]! }]} />);
    const bell = screen.getByRole('button', { name: 'Notifications' });
    expect(bell.querySelector('.mn-notification-center__badge')).toBeNull();
  });

  it('moves focus into the panel and Escape returns it to the bell', async () => {
    const user = userEvent.setup();
    render(<NotificationCenter items={ITEMS} {...common} />);
    const bell = screen.getByRole('button', { name: /Notifications/ });
    bell.focus();
    await user.keyboard('{Enter}');
    expect(dialog()).toContainElement(document.activeElement as HTMLElement);
    await user.keyboard('{Escape}');
    expect(screen.queryByRole('dialog')).toBeNull();
    expect(bell).toHaveFocus();
  });

  it('groups by day and marks unread items for screen readers', async () => {
    render(<NotificationCenter items={ITEMS} defaultOpen {...common} />);
    const today = screen.getByRole('list', { name: 'Today' });
    const yesterday = screen.getByRole('list', { name: 'Yesterday' });
    expect(within(today).getAllByRole('listitem')).toHaveLength(2);
    expect(within(yesterday).getAllByRole('listitem')).toHaveLength(1);
    expect(
      within(today).getByRole('button', { name: /^Unread: Ana Lopez mentioned you/ }),
    ).toBeInTheDocument();
    expect(
      within(today).getByRole('button', { name: /^Ben Okafor assigned you/ }),
    ).toBeInTheDocument();
    expect(within(yesterday).getByRole('link', { name: /Weekly export/ })).toHaveAttribute(
      'href',
      '/exports/42',
    );
  });

  it('selecting an unread item marks it read and calls onSelect', async () => {
    const onSelect = vi.fn();
    const onMarkRead = vi.fn();
    render(
      <NotificationCenter
        items={ITEMS}
        defaultOpen
        onSelect={onSelect}
        onMarkRead={onMarkRead}
        {...common}
      />,
    );
    await userEvent.click(screen.getByRole('button', { name: /Ana Lopez mentioned you/ }));
    expect(onMarkRead).toHaveBeenCalledWith('n1');
    expect(onSelect).toHaveBeenCalledWith(ITEMS[0]);
    onMarkRead.mockClear();
    await userEvent.click(screen.getByRole('button', { name: /Ben Okafor/ }));
    expect(onMarkRead).not.toHaveBeenCalled();
  });

  it('marks one or all as read', async () => {
    const onMarkRead = vi.fn();
    const onMarkAllRead = vi.fn();
    const user = userEvent.setup();
    render(
      <NotificationCenter
        items={ITEMS}
        defaultOpen
        onMarkRead={onMarkRead}
        onMarkAllRead={onMarkAllRead}
        markReadOnSelect={false}
        {...common}
      />,
    );
    const marks = screen.getAllByRole('button', { name: 'Mark as read' });
    expect(marks).toHaveLength(2);
    await user.click(marks[0]!);
    expect(onMarkRead).toHaveBeenCalledWith('n1');
    await user.click(screen.getByRole('button', { name: 'Mark all as read' }));
    expect(onMarkAllRead).toHaveBeenCalledOnce();
  });

  it('disables Mark all as read when nothing is unread', () => {
    render(
      <NotificationCenter items={[ITEMS[1]!]} defaultOpen onMarkAllRead={() => {}} {...common} />,
    );
    expect(screen.getByRole('button', { name: 'Mark all as read' })).toBeDisabled();
  });

  it('filters with the All / Unread tabs by keyboard (uncontrolled)', async () => {
    const onFilterChange = vi.fn();
    const user = userEvent.setup();
    render(
      <NotificationCenter items={ITEMS} defaultOpen onFilterChange={onFilterChange} {...common} />,
    );
    const all = screen.getByRole('tab', { name: 'All' });
    expect(all).toHaveAttribute('aria-selected', 'true');
    expect(screen.getByRole('tab', { name: 'Unread 2' })).toBeInTheDocument();
    all.focus();
    await user.keyboard('{ArrowRight}');
    expect(onFilterChange).toHaveBeenCalledWith('unread');
    expect(screen.getByRole('tab', { name: 'Unread 2' })).toHaveAttribute('aria-selected', 'true');
    const panel = screen.getByRole('tabpanel');
    expect(within(panel).getAllByRole('listitem')).toHaveLength(2);
    expect(within(panel).queryByText(/Ben Okafor/)).toBeNull();
  });

  it('controlled filter and open', async () => {
    function Controlled() {
      const [open, setOpen] = useState(true);
      return (
        <>
          <span data-testid="open">{String(open)}</span>
          <NotificationCenter
            items={ITEMS}
            open={open}
            onOpenChange={setOpen}
            filter="unread"
            {...common}
          />
        </>
      );
    }
    render(<Controlled />);
    expect(within(screen.getByRole('tabpanel')).getAllByRole('listitem')).toHaveLength(2);
    await userEvent.click(screen.getByRole('tab', { name: 'All' }));
    // Controlled: the tab stays on Unread until the product changes `filter`.
    expect(screen.getByRole('tab', { name: 'Unread 2' })).toHaveAttribute('aria-selected', 'true');
    await userEvent.keyboard('{Escape}');
    expect(screen.getByTestId('open')).toHaveTextContent('false');
  });

  it('shows the empty states', async () => {
    const { unmount } = render(<NotificationCenter items={[]} defaultOpen {...common} />);
    expect(screen.getByRole('heading', { name: 'You’re all caught up' })).toBeInTheDocument();
    unmount();
    render(
      <NotificationCenter items={[ITEMS[1]!]} defaultOpen defaultFilter="unread" {...common} />,
    );
    expect(screen.getByRole('heading', { name: 'No unread notifications' })).toBeInTheDocument();
    await expectNoA11yViolations();
  });

  it('loads more, and shows loading and error states', async () => {
    const onLoadMore = vi.fn();
    const { rerender } = render(
      <NotificationCenter items={ITEMS} defaultOpen hasMore onLoadMore={onLoadMore} {...common} />,
    );
    await userEvent.click(screen.getByRole('button', { name: 'Load more' }));
    expect(onLoadMore).toHaveBeenCalledOnce();
    rerender(<NotificationCenter items={[]} defaultOpen loading {...common} />);
    expect(screen.getByRole('img', { name: 'Loading notifications' })).toBeInTheDocument();
    rerender(<NotificationCenter items={[]} defaultOpen error={<p>Offline</p>} {...common} />);
    expect(screen.getByText('Offline')).toBeInTheDocument();
  });

  it('ungrouped list, footer, custom trigger and translations', async () => {
    render(
      <NotificationCenter
        items={ITEMS}
        groupByDay={false}
        footer={<a href="/settings/notifications">Preferences</a>}
        trigger={<button type="button">Inbox (2)</button>}
        messages={{ title: 'Avisos', all: 'Todo' }}
        {...common}
      />,
    );
    await userEvent.click(screen.getByRole('button', { name: 'Inbox (2)' }));
    expect(screen.getByRole('dialog', { name: 'Avisos' })).toBeInTheDocument();
    expect(screen.getByRole('tab', { name: 'Todo' })).toBeInTheDocument();
    expect(screen.queryByRole('heading', { name: 'Today' })).toBeNull();
    expect(screen.getByRole('link', { name: 'Preferences' })).toBeInTheDocument();
  });

  it('forwards the ref to the bell', () => {
    const ref = { current: null as HTMLButtonElement | null };
    render(<NotificationCenter ref={ref} items={ITEMS} />);
    expect(ref.current).toHaveClass('mn-notification-center');
  });

  it('renders on the server', () => {
    const html = renderToString(<NotificationCenter items={ITEMS} {...common} />);
    expect(html).toContain('mn-notification-center');
    expect(html).toContain('Notifications, 2 unread');
  });
});
