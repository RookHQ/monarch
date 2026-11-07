import { describe, expect, it } from 'vitest';
import { render, screen, within } from '@testing-library/react';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { CallStatsPanel, formatCallBytes, formatCallRate, type CallPeerStats } from './index.js';

const usage = { sentBytes: 6_000_000, receivedBytes: 8_680_000, upBps: 64_000, downBps: 1_500_000 };
const peers: CallPeerStats[] = [
  {
    id: 'a',
    name: 'Robin Park',
    rttMs: 42.4,
    inboundLossPct: 0.4,
    outboundLossPct: 1.25,
    quality: 'good',
  },
  { id: 'b', name: 'Alex Chen', quality: 'unknown' },
];

describe('formatCallBytes / formatCallRate', () => {
  it('formats sizes and rates', () => {
    expect(formatCallBytes(512)).toBe('512 B');
    expect(formatCallBytes(1536)).toBe('1.5 KB');
    expect(formatCallBytes(14_680_000)).toBe('14 MB');
    expect(formatCallRate(800)).toBe('800 bps');
    expect(formatCallRate(64_000)).toBe('64 kbps');
    expect(formatCallRate(1_500_000)).toBe('1.5 Mbps');
  });
});

describe('CallStatsPanel', () => {
  it('shows data use and a per-person table', async () => {
    render(
      <CallStatsPanel
        usage={usage}
        peers={peers}
        lowData
        footnote="Routed through your workspace relay."
      />,
    );
    expect(screen.getByRole('heading', { name: 'Data used this call' })).toBeInTheDocument();
    expect(screen.getByText('14 MB total · up 64 kbps · down 1.5 Mbps')).toBeInTheDocument();
    expect(screen.getByText('Low data mode is on: audio only.')).toBeInTheDocument();
    const table = screen.getByRole('table', { name: 'Connection to each person' });
    const robin = within(table).getByRole('row', { name: /Robin Park/ });
    expect(within(robin).getByText('42 ms')).toBeInTheDocument();
    expect(within(robin).getByText('1.3%')).toBeInTheDocument();
    expect(within(robin).getByRole('img', { name: 'Good connection' })).toBeInTheDocument();
    const alex = within(table).getByRole('row', { name: /Alex Chen/ });
    expect(within(alex).getAllByText('–')).toHaveLength(2);
    expect(within(alex).getByText('Not measured yet')).toBeInTheDocument();
    expect(screen.getByText('Routed through your workspace relay.')).toBeInTheDocument();
    await expectNoA11yViolations();
  });

  it('says when no one else is connected', () => {
    render(<CallStatsPanel usage={usage} peers={[]} />);
    expect(screen.queryByRole('table')).not.toBeInTheDocument();
    expect(screen.getByText('No one else is connected yet.')).toBeInTheDocument();
  });

  it('translates strings through messages', () => {
    render(
      <CallStatsPanel
        usage={usage}
        peers={peers}
        messages={{ dataUsed: 'Datenverbrauch', person: 'Person*' }}
      />,
    );
    expect(screen.getByRole('heading', { name: 'Datenverbrauch' })).toBeInTheDocument();
    expect(screen.getByRole('columnheader', { name: 'Person*' })).toBeInTheDocument();
  });

  it('renders on the server', () => {
    expect(renderToString(<CallStatsPanel usage={usage} peers={peers} />)).toContain(
      'mn-call-stats-panel',
    );
  });
});
