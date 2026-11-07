import { describe, expect, it, vi } from 'vitest';
import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { createRef } from 'react';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import {
  StatusPageView,
  deriveOverallState,
  formatIncidentDuration,
  type StatusPageDay,
  type StatusPageIncident,
  type StatusPageService,
} from './index.js';

function days(
  n: number,
  bad: Partial<Record<number, StatusPageDay['state']>> = {},
): StatusPageDay[] {
  return Array.from({ length: n }, (_, i) => {
    const date = new Date(Date.UTC(2026, 2, 1 + i)).toISOString().slice(0, 10);
    return {
      date,
      state: bad[i] ?? 'operational',
      uptimePercent: bad[i] ? 97.5 : 100,
      checks: 1440,
      avgMs: 120,
      p95Ms: 240,
    };
  });
}

const SERVICES: StatusPageService[] = [
  {
    id: 'api',
    name: 'API',
    group: 'Core',
    state: 'operational',
    uptimePercent: 99.98,
    responseTimeMs: 140,
    history: days(30, { 3: 'degraded' }),
  },
  {
    id: 'web',
    name: 'Web app',
    group: 'Core',
    state: 'degraded',
    uptimePercent: 99.5,
    history: days(30),
  },
  {
    id: 'mail',
    name: 'Email delivery',
    description: 'Outbound notifications',
    state: 'operational',
    uptimePercent: null,
    history: days(30),
  },
];

const ACTIVE: StatusPageIncident[] = [
  {
    id: 'i1',
    title: 'Slow page loads',
    component: 'Web app',
    status: 'acknowledged',
    severity: 'warning',
    openedAt: '2026-03-30T09:00:00Z',
    updates: [
      {
        id: 'u1',
        stage: 'investigating',
        body: 'Looking into slow responses.',
        time: '2026-03-30T09:05:00Z',
      },
    ],
  },
];
const PAST: StatusPageIncident[] = [
  {
    id: 'i0',
    title: 'Login errors',
    status: 'resolved',
    severity: 'critical',
    openedAt: '2026-03-20T10:00:00Z',
    durationSeconds: 3900,
  },
];

const base = {
  title: 'Example status',
  services: SERVICES,
  locale: 'en-GB',
  updatedAt: '2026-03-30T12:00:00Z',
  now: '2026-03-30T12:02:00Z',
} as const;

describe('StatusPageView', () => {
  it('renders the page structure with headings, banner, stats and services', async () => {
    const ref = createRef<HTMLDivElement>();
    render(
      <StatusPageView
        ref={ref}
        {...base}
        activeIncidents={ACTIVE}
        pastIncidents={PAST}
        overallUptimePercent={99.9}
      />,
    );
    expect(ref.current).toHaveClass('mn-status-page-view');
    expect(ref.current).toHaveAttribute('data-state', 'degraded');
    expect(screen.getByRole('heading', { level: 1, name: 'Example status' })).toBeInTheDocument();
    expect(
      screen.getByRole('heading', { level: 2, name: 'Degraded performance' }),
    ).toBeInTheDocument();
    expect(screen.getByRole('heading', { level: 3, name: 'Core' })).toBeInTheDocument();
    const stats = ref.current!.querySelector<HTMLElement>('.mn-status-page-view__stats')!;
    expect(within(stats).getByText('99.90%')).toBeInTheDocument();
    expect(within(stats).getByText('Active incidents').nextSibling).toHaveTextContent('1');
    expect(
      screen.getByRole('img', { name: /API, last 30 days: 1 degraded, 29 operational/ }),
    ).toBeInTheDocument();
    expect(screen.getByText('99.98% uptime')).toBeInTheDocument();
    expect(screen.getByText('Awaiting data')).toBeInTheDocument();
    expect(screen.getByText(/Updated/)).toHaveTextContent('2 minutes ago');
    await expectNoA11yViolations();
  });

  it('shows active and past incidents with their updates', () => {
    render(<StatusPageView {...base} activeIncidents={ACTIVE} pastIncidents={PAST} />);
    const active = screen.getByRole('region', { name: 'Active incidents' });
    const card = within(active).getByRole('article', { name: 'Slow page loads' });
    expect(
      within(card).getByText('Investigating', { selector: '.mn-status-badge__label' }),
    ).toBeInTheDocument();
    expect(within(card).getByRole('list', { name: 'Incident updates' })).toBeInTheDocument();
    const past = screen.getByRole('region', { name: 'Past incidents' });
    expect(within(past).getByRole('article', { name: 'Login errors' })).toHaveTextContent(
      'lasted 1h 5m',
    );
    expect(within(past).getByText(/Critical/)).toBeInTheDocument();
  });

  it('hides incident sections and the incident tile with showIncidents=false', () => {
    render(<StatusPageView {...base} activeIncidents={ACTIVE} showIncidents={false} />);
    expect(screen.queryByRole('region', { name: 'Active incidents' })).toBeNull();
    expect(screen.queryByRole('region', { name: 'Past incidents' })).toBeNull();
    expect(screen.queryByText('Active incidents')).toBeNull();
  });

  it('lists maintenance and the empty states', () => {
    render(
      <StatusPageView
        title="Empty"
        services={[]}
        maintenance={[
          {
            id: 'm1',
            title: 'Database upgrade',
            status: 'scheduled',
            startsAt: '2026-04-02T01:00:00Z',
            endsAt: '2026-04-02T02:00:00Z',
            description: 'Short read-only window.',
          },
        ]}
      />,
    );
    const region = screen.getByRole('region', { name: 'Scheduled maintenance' });
    expect(within(region).getByText('Database upgrade')).toBeInTheDocument();
    expect(within(region).getByText('Scheduled')).toBeInTheDocument();
    expect(screen.getByText('No services on this page yet.')).toBeInTheDocument();
    expect(screen.getByText('No incidents reported.')).toBeInTheDocument();
    expect(
      screen.getByRole('heading', { level: 2, name: 'Awaiting first checks' }),
    ).toBeInTheDocument();
  });

  it('uses renderHistory, the grid layout and heading level', () => {
    const renderHistory = vi.fn((s: StatusPageService, d: readonly StatusPageDay[]) => (
      <span>{`${s.name}: ${d.length} days`}</span>
    ));
    render(
      <StatusPageView {...base} layout="grid" headingLevel={2} renderHistory={renderHistory} />,
    );
    expect(screen.getByText('API: 14 days')).toBeInTheDocument();
    expect(screen.getByRole('heading', { level: 2, name: 'Example status' })).toBeInTheDocument();
    expect(screen.queryByRole('list', { name: 'Legend' })).toBeNull();
  });

  it('subscribes from the keyboard and shows a neutral confirmation', async () => {
    const user = userEvent.setup();
    const onSubscribe = vi.fn().mockResolvedValue(undefined);
    render(<StatusPageView {...base} onSubscribe={onSubscribe} />);
    const input = screen.getByRole('textbox', { name: 'Subscribe to updates' });
    await user.click(input);
    await user.keyboard('  ops@example.com  {Enter}');
    expect(onSubscribe).toHaveBeenCalledWith('ops@example.com');
    expect(await screen.findByRole('status')).toHaveTextContent('Check your inbox');
  });

  it('shows rate-limit and network errors on the field', async () => {
    const user = userEvent.setup();
    const onSubscribe = vi
      .fn()
      .mockResolvedValueOnce('rate-limited')
      .mockRejectedValueOnce(new Error('offline'));
    render(<StatusPageView {...base} onSubscribe={onSubscribe} />);
    const input = screen.getByRole('textbox', { name: 'Subscribe to updates' });
    await user.type(input, 'a@example.com');
    await user.click(screen.getByRole('button', { name: 'Subscribe' }));
    expect(await screen.findByText(/Too many attempts/)).toBeInTheDocument();
    expect(input).toHaveAttribute('aria-invalid', 'true');
    await user.click(screen.getByRole('button', { name: 'Subscribe' }));
    expect(await screen.findByText(/Could not reach/)).toBeInTheDocument();
    await expectNoA11yViolations();
  });

  it('translates messages', () => {
    render(
      <StatusPageView
        {...base}
        overallState="operational"
        messages={{ overall: { operational: 'Alles in Ordnung' }, servicesHeading: 'Dienste' }}
      />,
    );
    expect(screen.getByRole('heading', { name: 'Alles in Ordnung' })).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Dienste' })).toBeInTheDocument();
  });

  it('derives the overall state and formats durations', () => {
    expect(deriveOverallState([{ state: 'operational' }, { state: 'down' }])).toBe('down');
    expect(deriveOverallState([{ state: 'paused' }])).toBe('paused');
    expect(deriveOverallState([])).toBe('unknown');
    expect(formatIncidentDuration(45)).toBe('45s');
    expect(formatIncidentDuration(720)).toBe('12m');
    expect(formatIncidentDuration(7200)).toBe('2h');
  });

  it('renders on the server', () => {
    const html = renderToString(
      <StatusPageView {...base} activeIncidents={ACTIVE} onSubscribe={() => {}} />,
    );
    expect(html).toContain('mn-status-page-view');
    expect(html).toContain('Degraded performance');
    expect(html).toContain('2 minutes ago');
  });
});
