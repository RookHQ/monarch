import { describe, expect, it } from 'vitest';
import { render, screen, within } from '@testing-library/react';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { DomainProgress, domainPollMs, domainStepStates, formatDnsSnapshot } from './index.js';

const NOW = '2026-01-10T12:00:00Z';

describe('domain progress helpers', () => {
  it('derives step states from the stage', () => {
    expect(domainStepStates('dns')).toEqual({ dns: 'current', certificate: 'todo', live: 'todo' });
    expect(domainStepStates('certificate')).toEqual({
      dns: 'done',
      certificate: 'current',
      live: 'todo',
    });
    expect(domainStepStates('page').live).toBe('current');
    expect(domainStepStates('live')).toEqual({ dns: 'done', certificate: 'done', live: 'done' });
  });

  it('marks the step a blocking problem belongs to; info problems do not', () => {
    expect(domainStepStates('dns', [{ message: 'Proxy on' }]).dns).toBe('error');
    expect(domainStepStates('dns', [{ message: 'Wait', severity: 'info' }]).dns).toBe('current');
    expect(
      domainStepStates('certificate', [{ message: 'CAA blocks', step: 'certificate' }]).certificate,
    ).toBe('error');
  });

  it('formats a DNS snapshot and the poll interval', () => {
    expect(formatDnsSnapshot({ cnames: ['a.example.net'] })).toBe('CNAME → a.example.net');
    expect(formatDnsSnapshot({ a: ['192.0.2.1'], aaaa: ['2001:db8::1'] })).toBe(
      'A → 192.0.2.1 · AAAA → 2001:db8::1',
    );
    expect(formatDnsSnapshot({})).toBe('no record yet');
    expect(domainPollMs(undefined)).toBe(15_000);
    expect(domainPollMs('dns', 5)).toBe(15_000);
    expect(domainPollMs('dns', 9999)).toBe(300_000);
    expect(domainPollMs('dns')).toBe(30_000);
    expect(domainPollMs('dns', null)).toBe(false);
    expect(domainPollMs('live', 30)).toBe(false);
  });
});

describe('DomainProgress', () => {
  it('lists the three steps with their state in the accessible text', async () => {
    render(
      <DomainProgress
        host="book.example.com"
        stage="certificate"
        dnsFound={{ cnames: ['domains.example.net'] }}
        checkedAt="2026-01-10T11:58:00Z"
        now={NOW}
        notify="email"
        data-testid="root"
      />,
    );
    expect(screen.getByTestId('root')).toHaveClass('mn-domain-progress');
    const list = screen.getByRole('list', { name: 'Progress for book.example.com' });
    const items = within(list).getAllByRole('listitem');
    expect(items).toHaveLength(3);
    expect(items[0]).toHaveTextContent('DNS record found (done)');
    expect(items[1]).toHaveTextContent('HTTPS certificate (in progress)');
    expect(items[1]).toHaveAttribute('aria-current', 'step');
    expect(items[2]).toHaveTextContent('Live (not started)');
    expect(screen.getByText('Step 2 of 3: HTTPS certificate, in progress.')).toHaveAttribute(
      'role',
      'status',
    );
    expect(screen.getByText(/Public DNS for/)).toHaveTextContent(
      'Public DNS for book.example.com shows CNAME → domains.example.net.',
    );
    expect(screen.getByText(/email you when it is live/)).toBeInTheDocument();
    expect(screen.getByText(/Checked/)).toHaveTextContent('2 minutes ago');
    await expectNoA11yViolations();
  });

  it('shows blocking problems as warnings and marks the step', async () => {
    render(
      <DomainProgress
        host="book.example.com"
        stage="dns"
        problems={[
          { code: 'PROXIED', message: 'Turn off the proxy on this record.' },
          { code: 'WAIT', message: 'Still looking.', severity: 'info' },
        ]}
      />,
    );
    const first = screen.getAllByRole('listitem')[0]!;
    expect(first).toHaveAttribute('data-state', 'error');
    expect(first).toHaveTextContent('(needs attention)');
    expect(screen.getByText('Turn off the proxy on this record.')).toBeInTheDocument();
    expect(screen.queryByText('Still looking.')).toBeNull();
    await expectNoA11yViolations();
  });

  it('shows the first info problem when nothing is blocking', () => {
    render(
      <DomainProgress
        host="book.example.com"
        stage="certificate"
        problems={[{ message: 'Certificate pending.', severity: 'info' }]}
      />,
    );
    expect(screen.getByText('Certificate pending.')).toBeInTheDocument();
  });

  it('hides the checking details once live', () => {
    render(<DomainProgress host="book.example.com" stage="live" checkedAt={NOW} now={NOW} />);
    expect(screen.getByText('Your site loads on book.example.com.')).toBeInTheDocument();
    expect(screen.queryByText(/keep checking/)).toBeNull();
    expect(screen.getByText('Step 3 of 3: Live, done.')).toBeInTheDocument();
  });

  it('takes translated messages', () => {
    render(
      <DomainProgress
        host="book.example.com"
        stage="dns"
        rechecking={false}
        messages={{ liveTitle: () => 'En ligne', stateLabel: () => 'état' }}
      />,
    );
    expect(screen.getByText('En ligne')).toBeInTheDocument();
    expect(screen.queryByText(/keep checking/)).toBeNull();
    expect(screen.getByText(/Step 1 of 3/)).toHaveTextContent('état');
  });

  it('renders on the server', () => {
    const html = renderToString(
      <DomainProgress host="book.example.com" stage="page" checkedAt={NOW} now={NOW} />,
    );
    expect(html).toContain('mn-domain-progress');
    expect(html).toContain('Checking that your site loads on book.example.com.');
  });
});
