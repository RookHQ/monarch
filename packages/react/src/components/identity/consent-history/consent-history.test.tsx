import { describe, expect, it, vi } from 'vitest';
import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { DEFAULT_CONSENT_CATEGORIES, type ConsentRecord } from '../consent-manager/index.js';
import { ConsentHistory } from './index.js';

const records: ConsentRecord[] = [
  {
    id: 'r1',
    regime: 'opt-in',
    policyVersion: '2026-01',
    categories: { necessary: true, preferences: false, analytics: false, marketing: false },
    method: 'reject-all',
    gpc: false,
    at: '2026-01-10T09:30:00.000Z',
  },
  {
    id: 'r3',
    regime: 'opt-out',
    policyVersion: '2026-03',
    categories: { necessary: true, analytics: true, marketing: false, vendor_x: true },
    method: 'custom',
    gpc: true,
    at: '2026-03-02T18:05:00.000Z',
  },
  {
    id: 'r2',
    regime: 'opt-in',
    policyVersion: '2026-02',
    categories: { necessary: true, preferences: true, analytics: true, marketing: true },
    method: 'accept-all',
    gpc: false,
    at: '2026-02-14T12:00:00.000Z',
  },
];

const fixed = { locale: 'en-GB', timeZone: 'Europe/Berlin' } as const;

describe('ConsentHistory', () => {
  it('lists records newest first with dates in the given locale and time zone', async () => {
    render(<ConsentHistory records={records} categories={DEFAULT_CONSENT_CATEGORIES} {...fixed} />);
    const table = screen.getByRole('table', { name: 'Consent history' });
    const rows = within(table).getAllByRole('row').slice(1);
    expect(rows).toHaveLength(3);
    // 18:05 UTC is 19:05 in Berlin in March.
    expect(rows[0]).toHaveTextContent('2 Mar 2026, 19:05');
    expect(rows[1]).toHaveTextContent('14 Feb 2026, 13:00');
    expect(rows[2]).toHaveTextContent('10 Jan 2026, 10:30');
    expect(within(rows[0]!).getByText('2 Mar 2026, 19:05')).toHaveAttribute(
      'datetime',
      '2026-03-02T18:05:00.000Z',
    );
    await expectNoA11yViolations();
  });

  it('describes method, regime, version and categories in words', () => {
    render(<ConsentHistory records={records} categories={DEFAULT_CONSENT_CATEGORIES} {...fixed} />);
    const [, newest, middle, oldest] = screen.getAllByRole('row');
    expect(newest).toHaveTextContent('Custom choice');
    expect(newest).toHaveTextContent('Browser privacy signal present');
    expect(newest).toHaveTextContent('Could refuse (opt-out)');
    expect(newest).toHaveTextContent('2026-03');
    expect(middle).toHaveTextContent('Accepted all');
    expect(oldest).toHaveTextContent('Rejected all');
    expect(oldest).toHaveTextContent('Asked before use (opt-in)');

    const items = within(newest!).getAllByRole('listitem');
    // Listed categories in the product's order, then unknown ids as is.
    expect(items.map((i) => i.textContent)).toEqual([
      'Strictly necessary: ·Allowed',
      'Analytics: ·Allowed',
      'Marketing: ·Not allowed',
      'vendor_x: ·Allowed',
    ]);
    expect(items[2]).toHaveAttribute('data-allowed', 'false');
  });

  it('shows raw ids without categories and accepts reworded messages', () => {
    render(
      <ConsentHistory
        records={[records[0]!]}
        {...fixed}
        messages={{ methods: { 'reject-all': 'Refused everything' } as never, denied: 'Off' }}
      />,
    );
    expect(screen.getByText('Refused everything')).toBeInTheDocument();
    expect(screen.getByText('analytics')).toBeInTheDocument();
    expect(screen.getAllByText('Off')).toHaveLength(3);
  });

  it('shows the empty state and no download button without records', async () => {
    render(<ConsentHistory records={[]} onExport={() => {}} />);
    expect(
      screen.getByRole('heading', { name: 'No consent choices recorded', level: 3 }),
    ).toBeVisible();
    expect(screen.queryByRole('table')).toBeNull();
    expect(screen.queryByRole('button')).toBeNull();
    await expectNoA11yViolations();
  });

  it('runs onExport, busy while the promise is pending and with an error when it fails', async () => {
    const user = userEvent.setup();
    let reject!: (error: Error) => void;
    const onExport = vi.fn(
      () =>
        new Promise<void>((_, r) => {
          reject = r;
        }),
    );
    render(
      <ConsentHistory
        records={records}
        {...fixed}
        onExport={onExport}
        exportLabel="Download as CSV"
      />,
    );
    const button = screen.getByRole('button', { name: 'Download as CSV' });
    await user.click(button);
    expect(onExport).toHaveBeenCalledOnce();
    expect(button).toHaveAttribute('aria-busy', 'true');
    await user.click(button);
    expect(onExport).toHaveBeenCalledOnce();
    reject(new Error('nope'));
    expect(await screen.findByRole('alert')).toHaveTextContent('The download did not start.');
    expect(button).not.toHaveAttribute('aria-busy');
    expect(button).toHaveFocus();
  });

  it('can show relative time against a fixed now', () => {
    render(
      <ConsentHistory
        records={[records[1]!]}
        {...fixed}
        showRelative
        now="2026-03-05T18:05:00.000Z"
      />,
    );
    expect(screen.getByText('3 days ago')).toBeInTheDocument();
  });

  it('forwards props and renders on the server', () => {
    const html = renderToString(
      <ConsentHistory
        records={records}
        categories={DEFAULT_CONSENT_CATEGORIES}
        {...fixed}
        data-testid="root"
      />,
    );
    expect(html).toContain('mn-consent-history');
    expect(html).toContain('2 Mar 2026, 19:05');
    expect(html).toContain('Not allowed');
  });
});
