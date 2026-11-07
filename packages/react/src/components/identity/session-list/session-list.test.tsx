import { describe, expect, it, vi } from 'vitest';
import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { SessionList, describeUserAgent, type SessionListItem } from './index.js';

const NOW = '2026-03-10T12:00:00Z';
const SESSIONS: SessionListItem[] = [
  {
    id: 's1',
    label: 'Chrome on macOS',
    current: true,
    signedInAt: '2026-03-09T12:00:00Z',
    lastActiveAt: NOW,
  },
  {
    id: 's2',
    label: 'Safari on iPhone',
    kind: 'mobile',
    detail: 'Lisbon, Portugal',
    signedInAt: '2026-03-01T12:00:00Z',
    lastActiveAt: '2026-03-10T09:00:00Z',
  },
  { id: 's3', label: 'Firefox on Linux' },
];

describe('SessionList', () => {
  it('lists sessions with the current badge and times', async () => {
    render(<SessionList data-testid="root" sessions={SESSIONS} now={NOW} locale="en" />);
    expect(screen.getByTestId('root')).toHaveClass('mn-session-list');
    expect(screen.getByRole('heading', { name: 'Where you’re signed in' })).toBeInTheDocument();
    const list = screen.getByRole('list', { name: 'Signed-in sessions' });
    const rows = within(list).getAllByRole('listitem');
    expect(rows).toHaveLength(3);
    expect(rows[0]).toHaveTextContent('This device');
    expect(rows[0]).toHaveTextContent('Signed in yesterday');
    expect(rows[0]).not.toHaveTextContent('Last active');
    expect(rows[1]).toHaveTextContent('Lisbon, Portugal');
    expect(rows[1]).toHaveTextContent('Last active 3 hours ago');
    expect(screen.queryByRole('button')).toBeNull();
    await expectNoA11yViolations();
  });

  it('confirms before signing one session out, then announces it and returns focus', async () => {
    const user = userEvent.setup();
    const onSignOut = vi.fn(() => Promise.resolve());
    render(<SessionList sessions={SESSIONS} onSignOut={onSignOut} now={NOW} locale="en" />);
    const button = screen.getByRole('button', { name: 'Sign out Safari on iPhone' });
    await user.click(button);
    const dialog = screen.getByRole('alertdialog', { name: 'Sign out Safari on iPhone?' });
    expect(within(dialog).getByRole('button', { name: 'Cancel' })).toHaveFocus();
    await expectNoA11yViolations();
    await user.click(within(dialog).getByRole('button', { name: 'Sign out' }));
    expect(onSignOut).toHaveBeenCalledWith('s2');
    await waitFor(() => expect(screen.queryByRole('alertdialog')).toBeNull());
    expect(screen.getByRole('status')).toHaveTextContent('Signed out Safari on iPhone');
    await waitFor(() => expect(button).toHaveFocus());
  });

  it('uses different copy for the current session and keeps the dialog open on failure', async () => {
    const user = userEvent.setup();
    const onSignOut = vi.fn(() => Promise.reject(new Error('Network down')));
    render(<SessionList sessions={SESSIONS} onSignOut={onSignOut} now={NOW} locale="en" />);
    await user.click(screen.getByRole('button', { name: 'Sign out Chrome on macOS' }));
    const dialog = screen.getByRole('alertdialog', { name: 'Sign out of this device?' });
    await user.click(within(dialog).getByRole('button', { name: 'Sign out' }));
    expect(await within(dialog).findByText('Network down')).toBeInTheDocument();
    expect(screen.getByRole('alertdialog')).toBeInTheDocument();
  });

  it('signs out every other session with their ids', async () => {
    const user = userEvent.setup();
    const onSignOutOthers = vi.fn();
    render(
      <SessionList sessions={SESSIONS} onSignOutOthers={onSignOutOthers} now={NOW} locale="en" />,
    );
    await user.click(screen.getByRole('button', { name: 'Sign out other devices' }));
    const dialog = screen.getByRole('alertdialog', { name: 'Sign out other devices?' });
    expect(dialog).toHaveTextContent('2 other devices will be signed out');
    await user.keyboard('{Tab}{Enter}');
    expect(onSignOutOthers).toHaveBeenCalledWith(['s2', 's3']);
    expect(screen.getByRole('status')).toHaveTextContent('Signed out 2 other devices');
  });

  it('calls straight through when confirm is false', async () => {
    const user = userEvent.setup();
    const onSignOut = vi.fn();
    render(<SessionList sessions={SESSIONS} onSignOut={onSignOut} confirm={false} />);
    await user.click(screen.getByRole('button', { name: 'Sign out Firefox on Linux' }));
    expect(onSignOut).toHaveBeenCalledWith('s3');
    expect(screen.queryByRole('alertdialog')).toBeNull();
  });

  it('shows loading, empty and error states', async () => {
    const user = userEvent.setup();
    const onRetry = vi.fn();
    const { rerender } = render(<SessionList sessions={[]} state="loading" />);
    expect(screen.getByRole('region', { name: 'Where you’re signed in' })).toHaveAttribute(
      'aria-busy',
      'true',
    );
    rerender(<SessionList sessions={[]} />);
    expect(screen.getByText('No active sessions')).toBeInTheDocument();
    rerender(<SessionList sessions={[]} state="error" onRetry={onRetry} />);
    expect(screen.getByText('Couldn’t load your sessions')).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: /try again/i }));
    expect(onRetry).toHaveBeenCalled();
  });

  it('takes translated messages and a footnote', () => {
    render(
      <SessionList
        sessions={SESSIONS}
        onSignOut={() => undefined}
        footnote="Open apps can stay signed in for a few minutes."
        messages={{ title: 'Sitzungen', signOutLabel: (l) => `Abmelden: ${l}` }}
      />,
    );
    expect(screen.getByRole('heading', { name: 'Sitzungen' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Abmelden: Firefox on Linux' })).toBeInTheDocument();
    expect(screen.getByText('Open apps can stay signed in for a few minutes.')).toBeInTheDocument();
  });

  it('describes user-agent strings', () => {
    expect(
      describeUserAgent(
        'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Safari/537.36',
      ),
    ).toEqual({ label: 'Chrome on macOS', browser: 'Chrome', os: 'macOS', kind: 'desktop' });
    expect(
      describeUserAgent(
        'Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X) AppleWebKit/605.1.15 Version/17.0 Mobile/15E148 Safari/604.1',
      ).kind,
    ).toBe('mobile');
    expect(describeUserAgent(null)).toMatchObject({ label: 'Unknown device', kind: 'unknown' });
  });

  it('renders on the server', () => {
    const html = renderToString(<SessionList sessions={SESSIONS} now={NOW} locale="en" />);
    expect(html).toContain('mn-session-list');
    expect(html).toContain('Safari on iPhone');
  });
});
