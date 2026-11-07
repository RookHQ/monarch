import { describe, expect, it } from 'vitest';
import { act, render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { FreshnessIndicator, freshnessStatus } from './index.js';

const NOW = '2026-03-10T12:00:00Z';
const HOUR = 3_600_000;

describe('FreshnessIndicator', () => {
  it('shows the age and a fresh status', async () => {
    render(
      <FreshnessIndicator
        data-testid="root"
        updatedAt="2026-03-10T11:57:00Z"
        now={NOW}
        locale="en"
        source="Directory"
        staleAfter={HOUR}
      />,
    );
    const root = screen.getByTestId('root');
    expect(root).toHaveClass('mn-freshness-indicator');
    expect(root).toHaveAttribute('data-status', 'fresh');
    expect(root).toHaveTextContent('Directory · Updated 3 minutes ago');
    await expectNoA11yViolations();
  });

  it('turns stale and expired with a written status word', () => {
    const { rerender } = render(
      <FreshnessIndicator
        data-testid="r"
        updatedAt="2026-03-10T09:00:00Z"
        now={NOW}
        locale="en"
        staleAfter={HOUR}
        expiredAfter={24 * HOUR}
      />,
    );
    expect(screen.getByTestId('r')).toHaveAttribute('data-status', 'stale');
    expect(screen.getByText('Out of date')).toBeInTheDocument();
    rerender(
      <FreshnessIndicator
        data-testid="r"
        updatedAt="2026-03-01T09:00:00Z"
        now={NOW}
        locale="en"
        staleAfter={HOUR}
        expiredAfter={24 * HOUR}
      />,
    );
    expect(screen.getByTestId('r')).toHaveAttribute('data-tone', 'danger');
    expect(screen.getByRole('status')).toHaveTextContent('Very out of date');
  });

  it('shows never, checking and error states and details', () => {
    const { rerender } = render(<FreshnessIndicator data-testid="r" updatedAt={null} />);
    expect(screen.getByTestId('r')).toHaveTextContent('Never updated');
    rerender(<FreshnessIndicator data-testid="r" updatedAt={null} status="checking" />);
    expect(screen.getByTestId('r')).toHaveTextContent('Checking…');
    rerender(
      <FreshnessIndicator
        data-testid="r"
        updatedAt="2026-03-10T11:00:00Z"
        now={NOW}
        locale="en"
        status="error"
        error="Timed out"
        details={[
          'Events on',
          <a key="f" href="#f">
            2 failures
          </a>,
        ]}
      />,
    );
    const root = screen.getByTestId('r');
    expect(root.querySelector('p')).toHaveTextContent(
      /^Updated 1 hour ago.* · , Timed out · , Events on · , 2 failures$/,
    );
    expect(screen.getByRole('status')).toHaveTextContent('Timed out');
    expect(screen.getByRole('link', { name: '2 failures' })).toBeInTheDocument();
  });

  it('refreshes from the keyboard and announces the outcome', async () => {
    const user = userEvent.setup();
    let resolve: () => void = () => {};
    const onRefresh = () => new Promise<void>((r) => (resolve = r));
    render(<FreshnessIndicator updatedAt={NOW} now={NOW} onRefresh={onRefresh} />);
    const button = screen.getByRole('button', { name: 'Refresh' });
    button.focus();
    await user.keyboard('{Enter}');
    expect(screen.getByRole('button', { name: 'Refreshing…' })).toHaveAttribute(
      'aria-busy',
      'true',
    );
    expect(document.activeElement).toBe(screen.getByRole('button', { name: 'Refreshing…' }));
    await act(async () => resolve());
    expect(screen.getByRole('status')).toHaveTextContent('Refreshed.');
    expect(screen.getByRole('button', { name: 'Refresh' })).toBeInTheDocument();
    await expectNoA11yViolations();
  });

  it('announces a failed refresh', async () => {
    const user = userEvent.setup();
    render(
      <FreshnessIndicator
        updatedAt={NOW}
        now={NOW}
        onRefresh={() => Promise.reject(new Error('x'))}
      />,
    );
    await user.click(screen.getByRole('button', { name: 'Refresh' }));
    expect(await screen.findByText('Refresh failed.')).toBeInTheDocument();
  });

  it('computes status', () => {
    expect(freshnessStatus(undefined)).toBe('never');
    expect(freshnessStatus('nope')).toBe('never');
    expect(freshnessStatus(0, { now: 10, staleAfter: 5 })).toBe('stale');
    expect(freshnessStatus(0, { now: 10, staleAfter: 5, expiredAfter: 8 })).toBe('expired');
    expect(freshnessStatus(0, { now: 4, staleAfter: 5 })).toBe('fresh');
  });

  it('renders on the server', () => {
    const html = renderToString(
      <FreshnessIndicator
        updatedAt="2026-03-10T11:00:00Z"
        now={NOW}
        locale="en"
        variant="banner"
      />,
    );
    expect(html).toContain('mn-freshness-indicator');
    expect(html).toContain('1 hour ago');
  });
});
