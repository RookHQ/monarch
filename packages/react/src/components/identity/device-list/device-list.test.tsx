import { describe, expect, it, vi } from 'vitest';
import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { DeviceList, type DeviceListItem } from './index.js';

const NOW = '2026-03-10T12:00:00Z';
const DEVICES: DeviceListItem[] = [
  {
    id: 'd1',
    name: 'Work laptop',
    os: 'macOS',
    browser: 'Chrome',
    location: '203.0.113.7',
    lastUsedAt: NOW,
    firstSeenAt: '2026-01-10T12:00:00Z',
    current: true,
  },
  {
    id: 'd2',
    name: 'Phone',
    kind: 'mobile',
    os: 'Android',
    lastUsedAt: '2026-03-10T10:00:00Z',
  },
  { id: 'd3', name: '', kind: 'tablet', blocked: true },
];

describe('DeviceList', () => {
  it('lists devices with system, location, times and badges', async () => {
    render(<DeviceList data-testid="root" devices={DEVICES} now={NOW} locale="en" />);
    expect(screen.getByTestId('root')).toHaveClass('mn-device-list');
    const rows = within(screen.getByRole('list', { name: 'Devices' })).getAllByRole('listitem');
    expect(rows[0]).toHaveTextContent('macOS · Chrome · 203.0.113.7');
    expect(rows[0]).toHaveTextContent('This device');
    expect(rows[0]).toHaveTextContent('Last used now');
    expect(rows[1]).toHaveTextContent('Last used 2 hours ago');
    expect(rows[2]).toHaveTextContent('Unknown device');
    expect(rows[2]).toHaveTextContent('Blocked');
    expect(rows[2]).toHaveAttribute('data-blocked', 'true');
    await expectNoA11yViolations();
  });

  it('removes a device after confirming, never the current one', async () => {
    const user = userEvent.setup();
    const onRemove = vi.fn(() => Promise.resolve());
    render(<DeviceList devices={DEVICES} onRemove={onRemove} now={NOW} locale="en" />);
    expect(screen.queryByRole('button', { name: 'Remove Work laptop' })).toBeNull();
    const button = screen.getByRole('button', { name: 'Remove Phone' });
    button.focus();
    await user.keyboard('{Enter}');
    const dialog = screen.getByRole('alertdialog', { name: 'Remove Phone?' });
    expect(dialog).toHaveAttribute('data-tone', 'warning');
    await expectNoA11yViolations();
    await user.click(within(dialog).getByRole('button', { name: 'Remove' }));
    expect(onRemove).toHaveBeenCalledWith('d2');
    await waitFor(() => expect(screen.queryByRole('alertdialog')).toBeNull());
    expect(screen.getByRole('status')).toHaveTextContent('Removed Phone');
    await waitFor(() => expect(button).toHaveFocus());
  });

  it('cancelling with Escape removes nothing', async () => {
    const user = userEvent.setup();
    const onRemove = vi.fn();
    render(<DeviceList devices={DEVICES} onRemove={onRemove} />);
    await user.click(screen.getByRole('button', { name: 'Remove Unknown device' }));
    await user.keyboard('{Escape}');
    expect(screen.queryByRole('alertdialog')).toBeNull();
    expect(onRemove).not.toHaveBeenCalled();
  });

  it('calls straight through without confirmation and renders extra actions', async () => {
    const user = userEvent.setup();
    const onRemove = vi.fn();
    render(
      <DeviceList
        devices={DEVICES}
        onRemove={onRemove}
        confirm={false}
        renderActions={(d) => <button type="button">Rename {d.name || 'device'}</button>}
      />,
    );
    await user.click(screen.getByRole('button', { name: 'Remove Phone' }));
    expect(onRemove).toHaveBeenCalledWith('d2');
    expect(screen.getByRole('button', { name: 'Rename Work laptop' })).toBeInTheDocument();
  });

  it('shows loading, empty and error states', () => {
    const { rerender } = render(<DeviceList devices={[]} state="loading" />);
    expect(screen.getByRole('region', { name: 'Devices' })).toHaveAttribute('aria-busy', 'true');
    rerender(<DeviceList devices={[]} />);
    expect(screen.getByText('No devices yet')).toBeInTheDocument();
    rerender(<DeviceList devices={[]} state="error" onRetry={() => undefined} />);
    expect(screen.getByText('Couldn’t load your devices')).toBeInTheDocument();
  });

  it('takes translated messages', () => {
    render(<DeviceList devices={DEVICES} messages={{ title: 'Geräte', blocked: 'Gesperrt' }} />);
    expect(screen.getByRole('heading', { name: 'Geräte' })).toBeInTheDocument();
    expect(screen.getByText('Gesperrt')).toBeInTheDocument();
  });

  it('renders on the server', () => {
    const html = renderToString(<DeviceList devices={DEVICES} now={NOW} locale="en" />);
    expect(html).toContain('mn-device-list');
    expect(html).toContain('Work laptop');
  });
});
