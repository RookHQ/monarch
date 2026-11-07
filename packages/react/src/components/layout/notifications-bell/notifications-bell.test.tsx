import { describe, expect, it, vi } from 'vitest';
import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { NotificationsBell } from './index.js';
import type { NotificationItem } from '../../collaboration/notification-center/index.js';

const now = new Date('2026-03-02T12:00:00Z');
const items: NotificationItem[] = [
  { id: 'n1', title: 'Ana Lopez assigned you a task', time: '2026-03-02T11:30:00Z' },
  { id: 'n2', title: 'Weekly report is ready', time: '2026-03-02T09:00:00Z', read: true },
  { id: 'n3', title: 'Sam Lee replied to your comment', time: '2026-03-01T16:00:00Z' },
];

describe('NotificationsBell', () => {
  it('names the icon button with the unread count and shows a badge', async () => {
    render(<NotificationsBell items={items} now={now} data-testid="bell" />);
    const bell = screen.getByRole('button', { name: 'Notifications, 2 unread' });
    expect(bell).toHaveClass('mn-notifications-bell');
    expect(bell).toHaveAttribute('data-testid', 'bell');
    expect(bell).toHaveTextContent('2');
    await expectNoA11yViolations();
  });

  it('opens the panel, closes on select and returns focus', async () => {
    const user = userEvent.setup();
    const onSelect = vi.fn();
    const onMarkRead = vi.fn();
    render(
      <NotificationsBell items={items} now={now} onSelect={onSelect} onMarkRead={onMarkRead} />,
    );
    const bell = screen.getByRole('button', { name: /Notifications/ });
    bell.focus();
    await user.keyboard('{Enter}');
    const dialog = await screen.findByRole('dialog', { name: 'Notifications' });
    await expectNoA11yViolations();
    await user.click(within(dialog).getByRole('button', { name: /Ana Lopez assigned you a task/ }));
    expect(onMarkRead).toHaveBeenCalledWith('n1');
    expect(onSelect).toHaveBeenCalledWith(expect.objectContaining({ id: 'n1' }));
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
    expect(bell).toHaveFocus();
  });

  it('row variant shows the label and count; collapsed hides the label', () => {
    const { rerender } = render(<NotificationsBell items={items} variant="row" now={now} />);
    const row = screen.getByRole('button', { name: 'Notifications, 2 unread' });
    expect(row).toHaveAttribute('data-variant', 'row');
    expect(row).toHaveTextContent('Notifications2');
    rerender(<NotificationsBell items={items} variant="row" collapsed now={now} />);
    expect(screen.getByRole('button', { name: 'Notifications, 2 unread' })).toHaveAttribute(
      'data-collapsed',
    );
    expect(screen.queryByText('Notifications')).not.toBeInTheDocument();
  });

  it('uses the server unread count and shows 99+', () => {
    render(<NotificationsBell items={items} unreadCount={120} now={now} />);
    const bell = screen.getByRole('button', { name: 'Notifications, 120 unread' });
    expect(bell).toHaveTextContent('99+');
  });

  it('has a See all link that closes the panel', async () => {
    const user = userEvent.setup();
    render(<NotificationsBell items={items} now={now} seeAllHref="#all" defaultOpen />);
    const link = await screen.findByRole('link', { name: 'See all notifications' });
    expect(link).toHaveAttribute('href', '#all');
    await user.click(link);
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
  });

  it('translates', () => {
    render(
      <NotificationsBell
        items={[]}
        messages={{ title: 'Avisos', triggerLabel: (n) => `Avisos (${n})` }}
      />,
    );
    expect(screen.getByRole('button', { name: 'Avisos (0)' })).toBeInTheDocument();
  });

  it('renders on the server', () => {
    const html = renderToString(<NotificationsBell items={items} now={now} />);
    expect(html).toContain('mn-notifications-bell');
    expect(html).toContain('Notifications, 2 unread');
  });
});
