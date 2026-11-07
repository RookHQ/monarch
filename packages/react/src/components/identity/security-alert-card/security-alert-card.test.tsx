import { useState } from 'react';
import { describe, expect, it, vi } from 'vitest';
import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { SecurityAlertCard, type SecurityAlert } from './index.js';

const NOW = '2026-03-10T12:00:00Z';
const ALERTS: SecurityAlert[] = [
  {
    id: 'a1',
    title: 'New sign-in from Firefox on Linux',
    description: 'Lisbon, Portugal',
    severity: 'high',
    createdAt: '2026-03-10T11:00:00Z',
    action: { label: 'Review sessions', href: '#sessions' },
  },
  {
    id: 'a2',
    title: 'Password changed',
    severity: 'medium',
    createdAt: '2026-03-09T12:00:00Z',
    read: true,
  },
  { id: 'a3', title: 'Recovery codes viewed', createdAt: '2026-03-01T12:00:00Z' },
];

function Dismissable() {
  const [alerts, setAlerts] = useState(ALERTS);
  return (
    <SecurityAlertCard
      alerts={alerts}
      onDismiss={(id) => setAlerts((all) => all.filter((a) => a.id !== id))}
      now={NOW}
      locale="en"
    />
  );
}

describe('SecurityAlertCard', () => {
  it('lists alerts with severity, time, unread marker and actions', async () => {
    render(<SecurityAlertCard data-testid="root" alerts={ALERTS} now={NOW} locale="en" />);
    expect(screen.getByTestId('root')).toHaveClass('mn-security-alert-card');
    expect(screen.getByText('2 unread')).toBeInTheDocument();
    const rows = within(screen.getByRole('list', { name: 'Security alerts' })).getAllByRole(
      'listitem',
    );
    expect(rows[0]).toHaveAttribute('data-severity', 'high');
    expect(rows[0]).toHaveTextContent('Unread: New sign-in from Firefox on Linux');
    expect(rows[0]).toHaveTextContent('High');
    expect(rows[0]).toHaveTextContent('1 hour ago');
    expect(within(rows[0]!).getByRole('link', { name: 'Review sessions' })).toHaveAttribute(
      'href',
      '#sessions',
    );
    expect(rows[1]).not.toHaveTextContent('Unread');
    expect(rows[2]).toHaveTextContent('Info');
    await expectNoA11yViolations();
  });

  it('marks one or all alerts read', async () => {
    const user = userEvent.setup();
    const onMarkRead = vi.fn();
    const onMarkAllRead = vi.fn();
    render(
      <SecurityAlertCard alerts={ALERTS} onMarkRead={onMarkRead} onMarkAllRead={onMarkAllRead} />,
    );
    expect(screen.getAllByRole('button', { name: 'Mark as read' })).toHaveLength(2);
    await user.click(screen.getAllByRole('button', { name: 'Mark as read' })[0]!);
    expect(onMarkRead).toHaveBeenCalledWith('a1');
    await user.click(screen.getByRole('button', { name: 'Mark all as read' }));
    expect(onMarkAllRead).toHaveBeenCalled();
  });

  it('dismisses with the keyboard, announces it and keeps focus in the list', async () => {
    const user = userEvent.setup();
    render(<Dismissable />);
    screen.getByRole('button', { name: 'Dismiss New sign-in from Firefox on Linux' }).focus();
    await user.keyboard('{Enter}');
    expect(screen.queryByText('New sign-in from Firefox on Linux')).toBeNull();
    expect(screen.getByRole('status')).toHaveTextContent(
      'Dismissed New sign-in from Firefox on Linux',
    );
    await waitFor(() =>
      expect(screen.getByRole('button', { name: 'Dismiss Password changed' })).toHaveFocus(),
    );
  });

  it('runs a button action', async () => {
    const user = userEvent.setup();
    const onClick = vi.fn();
    render(
      <SecurityAlertCard
        alerts={[{ id: 'x', title: 'Check', action: { label: 'Fix', onClick } }]}
      />,
    );
    await user.click(screen.getByRole('button', { name: 'Fix' }));
    expect(onClick).toHaveBeenCalled();
  });

  it('shows loading, empty and error states', () => {
    const { rerender } = render(<SecurityAlertCard alerts={[]} state="loading" />);
    expect(screen.getByRole('region', { name: 'Security alerts' })).toHaveAttribute(
      'aria-busy',
      'true',
    );
    rerender(<SecurityAlertCard alerts={[]} />);
    expect(screen.getByText('No security alerts')).toBeInTheDocument();
    rerender(<SecurityAlertCard alerts={[]} state="error" onRetry={() => undefined} />);
    expect(screen.getByText('Couldn’t load security alerts')).toBeInTheDocument();
  });

  it('takes translated messages, including severity names', () => {
    render(
      <SecurityAlertCard
        alerts={ALERTS}
        messages={{ title: 'Sicherheit', severity: { high: 'Hoch' } }}
      />,
    );
    expect(screen.getByRole('heading', { name: 'Sicherheit' })).toBeInTheDocument();
    expect(screen.getByText('Hoch')).toBeInTheDocument();
    expect(screen.getByText('Medium')).toBeInTheDocument();
  });

  it('renders on the server', () => {
    const html = renderToString(<SecurityAlertCard alerts={ALERTS} now={NOW} locale="en" />);
    expect(html).toContain('mn-security-alert-card');
    expect(html).toContain('Password changed');
  });
});
