import { describe, expect, it, vi } from 'vitest';
import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import {
  WebhookDeliveries,
  filterWebhookDeliveries,
  type WebhookDelivery,
  type WebhookDeliveryFilters,
} from './index.js';

const NOW = new Date('2026-03-02T12:00:00Z');

const deliveries: WebhookDelivery[] = [
  {
    id: 'd1',
    status: 'succeeded',
    eventType: 'order.created',
    eventId: 'evt_100',
    responseCode: 200,
    durationMs: 182,
    attempts: 1,
    at: '2026-03-02T11:58:00Z',
  },
  {
    id: 'd2',
    status: 'failed',
    eventType: 'order.refunded',
    eventId: 'evt_101',
    responseCode: 500,
    error: 'The endpoint answered 500',
    attempts: 6,
    at: '2026-03-02T11:00:00Z',
  },
  {
    id: 'd3',
    status: 'pending',
    eventType: 'order.created',
    eventId: 'evt_102',
    attempts: 0,
    at: '2026-03-02T11:59:00Z',
    nextAttemptAt: '2026-03-02T12:05:00Z',
  },
];

describe('WebhookDeliveries', () => {
  it('renders a named table with status, event, response, attempts and time', async () => {
    render(<WebhookDeliveries deliveries={deliveries} now={NOW} locale="en" />);
    const table = screen.getByRole('table', { name: 'Webhook deliveries' });
    const rows = within(table).getAllByRole('row');
    expect(rows).toHaveLength(4);
    expect(within(rows[1]!).getByText('Delivered')).toBeInTheDocument();
    expect(within(rows[1]!).getByText('HTTP 200 · 182 ms')).toBeInTheDocument();
    expect(within(rows[2]!).getByText('The endpoint answered 500')).toBeInTheDocument();
    expect(within(rows[3]!).getByText('Not attempted yet')).toBeInTheDocument();
    expect(within(rows[3]!).getByText(/Next attempt/)).toBeInTheDocument();
    expect(screen.getAllByRole('button', { name: 'Copy event ID' })).toHaveLength(3);
    await expectNoA11yViolations();
  });

  it('filters by status and event ID on its own and clears the filters', async () => {
    const user = userEvent.setup();
    const onFiltersChange = vi.fn();
    render(
      <WebhookDeliveries
        deliveries={deliveries}
        now={NOW}
        eventTypes={['order.created', 'order.refunded']}
        onFiltersChange={onFiltersChange}
      />,
    );
    await user.selectOptions(screen.getByRole('combobox', { name: 'Status' }), 'failed');
    expect(screen.getAllByRole('row')).toHaveLength(2);
    expect(onFiltersChange).toHaveBeenLastCalledWith({
      status: 'failed',
      eventType: '',
      query: '',
    });
    await user.click(screen.getByRole('button', { name: 'Clear filters' }));
    expect(screen.getAllByRole('row')).toHaveLength(4);

    await user.type(screen.getByRole('searchbox', { name: 'Find by event ID' }), 'evt_999');
    expect(screen.queryByRole('table')).toBeNull();
    expect(screen.getByText('No deliveries match')).toBeInTheDocument();
    await expectNoA11yViolations();
  });

  it('leaves filtering to the server with manualFiltering', async () => {
    const user = userEvent.setup();
    const onFiltersChange = vi.fn();
    function Server() {
      return (
        <WebhookDeliveries
          deliveries={deliveries}
          manualFiltering
          filters={{ status: 'failed', eventType: '', query: '' }}
          onFiltersChange={onFiltersChange}
          now={NOW}
        />
      );
    }
    render(<Server />);
    expect(screen.getAllByRole('row')).toHaveLength(4);
    await user.click(screen.getByRole('button', { name: 'Clear filters' }));
    expect(onFiltersChange).toHaveBeenCalledWith({ status: '', eventType: '', query: '' });
  });

  it('redelivers a row, tracks the promise and announces the outcome', async () => {
    const user = userEvent.setup();
    let resolve: () => void = () => undefined;
    const onRedeliver = vi.fn(
      () =>
        new Promise<void>((r) => {
          resolve = r;
        }),
    );
    render(<WebhookDeliveries deliveries={deliveries} now={NOW} onRedeliver={onRedeliver} />);
    // Pending deliveries cannot be redelivered by default.
    expect(screen.queryByRole('button', { name: 'Redeliver order.created evt_102' })).toBeNull();
    const button = screen.getByRole('button', { name: 'Redeliver order.refunded evt_101' });
    await user.click(button);
    expect(onRedeliver).toHaveBeenCalledWith(deliveries[1]);
    expect(button).toHaveAttribute('aria-busy', 'true');
    await user.click(button);
    expect(onRedeliver).toHaveBeenCalledOnce();
    resolve();
    await vi.waitFor(() => expect(button).not.toHaveAttribute('aria-busy'));
    expect(screen.getByText('Redelivery queued.')).toBeInTheDocument();
  });

  it('announces a failed redelivery', async () => {
    const user = userEvent.setup();
    render(
      <WebhookDeliveries
        deliveries={deliveries}
        now={NOW}
        onRedeliver={() => Promise.reject(new Error('nope'))}
      />,
    );
    await user.click(screen.getByRole('button', { name: 'Redeliver order.created evt_100' }));
    expect(await screen.findByText('Could not redeliver. Try again.')).toBeInTheDocument();
  });

  it('opens details from the keyboard and renders extra row actions', async () => {
    const user = userEvent.setup();
    const onSelect = vi.fn();
    render(
      <WebhookDeliveries
        deliveries={deliveries.slice(0, 1)}
        now={NOW}
        filterable={false}
        onSelect={onSelect}
        renderActions={(d) => <button type="button">Discard {d.eventId}</button>}
      />,
    );
    await user.tab();
    expect(
      screen.getByRole('button', { name: 'View delivery of order.created evt_100' }),
    ).toHaveFocus();
    await user.keyboard('{Enter}');
    expect(onSelect).toHaveBeenCalledWith(deliveries[0]);
    expect(screen.getByRole('button', { name: 'Discard evt_100' })).toBeInTheDocument();
    await expectNoA11yViolations();
  });

  it('shows counts, loading, error and empty states', async () => {
    const user = userEvent.setup();
    const onRetry = vi.fn();
    const { rerender } = render(
      <WebhookDeliveries deliveries={[]} loading counts={{ succeeded: 12, failed: 3 }} />,
    );
    expect(screen.getByText('12 delivered · 3 failed')).toBeInTheDocument();
    expect(screen.getByText('Loading deliveries')).toBeInTheDocument();
    rerender(<WebhookDeliveries deliveries={[]} error onRetry={onRetry} />);
    expect(screen.getByText('Deliveries did not load.')).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Try again' }));
    expect(onRetry).toHaveBeenCalled();
    rerender(
      <WebhookDeliveries
        deliveries={[]}
        emptyAction={<button type="button">Send a test event</button>}
      />,
    );
    expect(screen.getByText('No deliveries yet')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Send a test event' })).toBeInTheDocument();
  });

  it('pages with Load more', async () => {
    const user = userEvent.setup();
    const onLoadMore = vi.fn();
    render(<WebhookDeliveries deliveries={deliveries} now={NOW} hasMore onLoadMore={onLoadMore} />);
    await user.click(screen.getByRole('button', { name: /Load more/ }));
    expect(onLoadMore).toHaveBeenCalled();
  });

  it('takes custom statuses and messages', () => {
    render(
      <WebhookDeliveries
        deliveries={[{ ...deliveries[0]!, status: 'dead' }]}
        statuses={{ dead: { label: 'Dead letter', tone: 'danger' } }}
        messages={{ caption: 'Outgoing requests' }}
        showDestination
        now={NOW}
      />,
    );
    expect(screen.getByRole('table', { name: 'Outgoing requests' })).toBeInTheDocument();
    expect(screen.getAllByText('Dead letter').length).toBeGreaterThan(0);
    expect(screen.getByRole('columnheader', { name: 'Destination' })).toBeInTheDocument();
  });

  it('filterWebhookDeliveries matches event IDs exactly and event types by substring', () => {
    const f = (patch: Partial<WebhookDeliveryFilters>) =>
      filterWebhookDeliveries(deliveries, { status: '', eventType: '', query: '', ...patch }).map(
        (d) => d.id,
      );
    expect(f({ query: 'evt_10' })).toEqual([]);
    expect(f({ query: 'EVT_101' })).toEqual(['d2']);
    expect(f({ query: 'refund' })).toEqual(['d2']);
    expect(f({ eventType: 'order.created' })).toEqual(['d1', 'd3']);
  });

  it('renders on the server', () => {
    const html = renderToString(<WebhookDeliveries deliveries={deliveries} now={NOW} />);
    expect(html).toContain('mn-webhook-deliveries');
    expect(html).toContain('order.refunded');
  });
});
