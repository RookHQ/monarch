import { describe, expect, it, vi } from 'vitest';
import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { DnsProviderGuide, DnsRecordTable, relativeDnsName, type DnsRecord } from './index.js';

const records: DnsRecord[] = [
  {
    type: 'CNAME',
    name: 'book.example.com',
    value: 'domains.example.net',
    purpose: 'routing',
    ok: false,
  },
  {
    type: 'TXT',
    name: '_verify.book.example.com',
    value: 'site-verification=4f1a9c',
    purpose: 'verification',
    ok: true,
  },
];

function mockClipboard() {
  const spy = vi.fn(() => Promise.resolve());
  Object.defineProperty(navigator, 'clipboard', { configurable: true, value: { writeText: spy } });
  Object.defineProperty(document, 'execCommand', { configurable: true, value: () => false });
  return spy;
}

describe('relativeDnsName', () => {
  it('gives names relative to the zone, @ for the apex', () => {
    expect(relativeDnsName('book.example.com', 'example.com')).toBe('book');
    expect(relativeDnsName('example.com', 'example.com')).toBe('@');
    expect(relativeDnsName('example.com.', 'Example.com')).toBe('@');
    expect(relativeDnsName('other.org', 'example.com')).toBe('other.org');
    expect(relativeDnsName('book.example.com', undefined)).toBe('book.example.com');
  });
});

describe('DnsRecordTable', () => {
  it('renders one card per record with purpose, status and copyable fields', async () => {
    render(<DnsRecordTable records={records} zone="example.com" data-testid="root" />);
    expect(screen.getByTestId('root')).toHaveClass('mn-dns-record-table');
    const list = screen.getByRole('list', { name: 'DNS records for example.com' });
    const cards = within(list).getAllByRole('listitem');
    expect(cards).toHaveLength(2);
    expect(
      within(cards[0]!).getByRole('heading', { name: 'Points your address here' }),
    ).toBeInTheDocument();
    expect(within(cards[0]!).getByText('Waiting')).toBeInTheDocument();
    expect(within(cards[1]!).getByText('Found')).toBeInTheDocument();
    expect(
      within(cards[0]!).getByRole('textbox', { name: 'name of the CNAME record' }),
    ).toHaveValue('book');
    expect(within(cards[0]!).getByText('Full name: book.example.com')).toBeInTheDocument();
    expect(screen.getByText('1 of 2 records found.')).toHaveAttribute('role', 'status');
    await expectNoA11yViolations();
  });

  it('copies a record value from the keyboard', async () => {
    const user = userEvent.setup();
    const writeText = mockClipboard();
    const onCopy = vi.fn();
    render(<DnsRecordTable records={records} zone="example.com" onCopy={onCopy} />);
    const button = screen.getByRole('button', {
      name: 'Copy value of the CNAME record book',
    });
    button.focus();
    await user.keyboard('{Enter}');
    expect(writeText).toHaveBeenCalledWith('domains.example.net');
    expect(onCopy).toHaveBeenCalledWith('domains.example.net');
  });

  it('tabs through name and value fields in order', async () => {
    const user = userEvent.setup();
    render(<DnsRecordTable records={records.slice(0, 1)} zone="example.com" />);
    await user.tab();
    expect(screen.getByRole('textbox', { name: 'name of the CNAME record' })).toHaveFocus();
    await user.tab();
    expect(screen.getByRole('button', { name: 'Copy name of the CNAME record' })).toHaveFocus();
    await user.tab();
    expect(screen.getByRole('textbox', { name: 'value of the CNAME record book' })).toHaveFocus();
  });

  it('table layout has column headers and a focusable scroll region', async () => {
    render(
      <DnsRecordTable
        layout="table"
        records={[
          { type: 'MX', name: 'example.com', value: 'mx.example.net', priority: 10, ttl: 3600 },
        ]}
        zone="example.com"
      />,
    );
    const region = screen.getByRole('region', { name: 'DNS records for example.com' });
    expect(region).toHaveAttribute('tabindex', '0');
    const headers = screen.getAllByRole('columnheader').map((h) => h.textContent);
    expect(headers).toEqual(['Type', 'Name', 'Value', 'Priority', 'TTL']);
    expect(screen.getByRole('textbox', { name: 'name of the MX record' })).toHaveValue('@');
    expect(screen.queryByText(/records found/)).toBeNull();
    await expectNoA11yViolations();
  });

  it('takes translated messages and a custom purpose', () => {
    render(
      <DnsRecordTable
        records={[
          { type: 'TXT', name: 'mail.example.com', value: 'v=spf1 ~all', purpose: 'Sender' },
        ]}
        messages={{ value: 'Wert', label: () => 'Einträge' }}
      />,
    );
    expect(screen.getByRole('list', { name: 'Einträge' })).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Sender' })).toBeInTheDocument();
    expect(screen.getByText('Wert')).toBeInTheDocument();
  });

  it('renders on the server', () => {
    const html = renderToString(<DnsRecordTable records={records} zone="example.com" />);
    expect(html).toContain('mn-dns-record-table');
    expect(html).toContain('domains.example.net');
  });
});

describe('DnsProviderGuide', () => {
  it('shows steps for the picked provider and switches with the select', async () => {
    const user = userEvent.setup();
    const onProviderChange = vi.fn();
    render(
      <DnsProviderGuide
        record={records[0]!}
        zone="example.com"
        onProviderChange={onProviderChange}
      />,
    );
    const select = screen.getByRole('combobox', { name: 'Where is your DNS managed?' });
    expect(select).toHaveValue('cloudflare');
    expect(screen.getByText(/Proxy status/)).toBeInTheDocument();
    await user.selectOptions(select, 'route53');
    expect(onProviderChange).toHaveBeenCalledWith('route53');
    expect(screen.getByText(/Leave the routing policy/)).toBeInTheDocument();
    await expectNoA11yViolations();
  });

  it('accepts custom providers and renders on the server', () => {
    const providers = [{ id: 'x', label: 'Example DNS', steps: () => ['Add it.'] }];
    const html = renderToString(<DnsProviderGuide record={records[0]!} providers={providers} />);
    expect(html).toContain('Example DNS');
    expect(html).toContain('Add it.');
  });
});
