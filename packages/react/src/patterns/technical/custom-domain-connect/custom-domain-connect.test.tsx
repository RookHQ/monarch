import { describe, expect, it, vi } from 'vitest';
import { useState } from 'react';
import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import {
  CustomDomainConnect,
  domainSyntaxError,
  normalizeDomain,
  type CustomDomainRecord,
  type CustomDomainStatus,
} from './index.js';

const RECORDS: CustomDomainRecord[] = [
  { type: 'CNAME', name: 'help', value: 'pages.example.net', purpose: 'Routes traffic' },
  { type: 'TXT', name: '_verify.help', value: 'token-123', found: true, ttl: 300 },
];

// The domain status line (CopyField buttons have their own "Copied" status regions).
const statusText = () =>
  screen
    .getAllByRole('status')
    .find((el) => el.classList.contains('mn-custom-domain-connect__status-text')) as HTMLElement;

const root = () => document.querySelector('.mn-custom-domain-connect') as HTMLElement;

function deferred() {
  let resolve!: () => void;
  let reject!: (e: unknown) => void;
  const promise = new Promise<void>((res, rej) => {
    resolve = res;
    reject = rej;
  });
  return { promise, resolve, reject };
}

describe('normalizeDomain and domainSyntaxError', () => {
  it('strips schemes, paths, ports and trailing dots, and lowercases', () => {
    expect(normalizeDomain('  HTTPS://Help.Example.com/path?q=1 ')).toBe('help.example.com');
    expect(normalizeDomain('help.example.com.')).toBe('help.example.com');
    expect(normalizeDomain('help.example.com:8080')).toBe('help.example.com');
  });

  it('converts international names to punycode', () => {
    expect(normalizeDomain('hilfe.bücher.de')).toBe('hilfe.xn--bcher-kva.de');
    expect(domainSyntaxError('hilfe.bücher.de')).toBeNull();
  });

  it('rejects what is not a host name', () => {
    expect(domainSyntaxError('')).toBe('empty');
    expect(domainSyntaxError('localhost')).toBe('invalid');
    expect(domainSyntaxError('bad_name.example.com')).toBe('invalid');
    expect(domainSyntaxError('-help.example.com')).toBe('invalid');
    expect(domainSyntaxError('help..example.com')).toBe('invalid');
    expect(domainSyntaxError('help.example.1')).toBe('invalid');
    expect(domainSyntaxError('*.example.com')).toBe('invalid');
    expect(domainSyntaxError(`${'a'.repeat(64)}.example.com`)).toBe('invalid');
    expect(domainSyntaxError(`${'abcdefghi.'.repeat(26)}com`)).toBe('tooLong');
    expect(domainSyntaxError('example.com')).toBeNull();
    expect(domainSyntaxError('example.com', { requireSubdomain: true })).toBe('needsSubdomain');
    expect(domainSyntaxError('help.example.com', { requireSubdomain: true })).toBeNull();
  });
});

describe('CustomDomainConnect', () => {
  it('starts with the domain step and validates syntax before calling onConnect', async () => {
    const user = userEvent.setup();
    const onConnect = vi.fn();
    render(<CustomDomainConnect onConnect={onConnect} requireSubdomain />);
    expect(root()).toHaveAttribute('data-step', 'enter');
    const steps = screen.getByRole('navigation', { name: 'Custom domain setup' });
    expect(within(steps).getAllByRole('listitem')[0]).toHaveAttribute('aria-current', 'step');

    await user.click(screen.getByRole('button', { name: 'Connect domain' }));
    const input = screen.getByRole('textbox', { name: /Domain/ });
    expect(input).toHaveAttribute('aria-invalid', 'true');
    expect(input).toHaveAccessibleDescription('Enter a domain.');
    expect(input).toHaveFocus();

    await user.type(input, 'example.com{Enter}');
    expect(input).toHaveAccessibleDescription('Use a subdomain, such as help.example.com.');
    // Typing clears the error.
    await user.type(input, 'x');
    expect(input).not.toHaveAttribute('aria-invalid');
    expect(onConnect).not.toHaveBeenCalled();
    await expectNoA11yViolations();
  });

  it('connects a normalised domain (uncontrolled) and focuses its heading', async () => {
    const user = userEvent.setup();
    const pending = deferred();
    const onConnect = vi.fn(() => pending.promise);
    const onDomainChange = vi.fn();
    render(
      <CustomDomainConnect
        onConnect={onConnect}
        onDomainChange={onDomainChange}
        records={RECORDS}
      />,
    );
    await user.type(screen.getByRole('textbox'), 'https://Help.Example.com/{Enter}');
    expect(onConnect).toHaveBeenCalledWith('help.example.com');
    expect(screen.getByRole('button', { name: /Connect domain/ })).toHaveAttribute(
      'aria-busy',
      'true',
    );
    pending.resolve();
    const heading = await screen.findByRole('heading', { name: 'help.example.com' });
    await waitFor(() => expect(heading).toHaveFocus());
    expect(onDomainChange).toHaveBeenCalledWith('help.example.com');
    expect(statusText()).toHaveTextContent(
      'Add the DNS records below at your DNS provider, then verify.',
    );
    await expectNoA11yViolations();
  });

  it('shows the error from a rejected onConnect under the field', async () => {
    const user = userEvent.setup();
    render(
      <CustomDomainConnect
        onConnect={() => Promise.reject(new Error('That domain is already in use.'))}
      />,
    );
    await user.type(screen.getByRole('textbox'), 'help.example.com{Enter}');
    expect(await screen.findByText('That domain is already in use.')).toBeInTheDocument();
    expect(screen.getByRole('textbox')).toHaveAttribute('aria-invalid', 'true');
  });

  it('runs a product validate check on the normalised domain', async () => {
    const user = userEvent.setup();
    const onConnect = vi.fn();
    render(
      <CustomDomainConnect
        onConnect={onConnect}
        validate={(d) => (d.endsWith('.example.org') ? 'That address is reserved.' : null)}
      />,
    );
    await user.type(screen.getByRole('textbox'), 'a.example.org{Enter}');
    expect(screen.getByText('That address is reserved.')).toBeInTheDocument();
    expect(onConnect).not.toHaveBeenCalled();
  });

  it('lists DNS records with copy buttons, purpose, TTL and found state', async () => {
    render(<CustomDomainConnect domain="help.example.com" onConnect={vi.fn()} records={RECORDS} />);
    const list = screen.getByRole('list', { name: 'DNS records' });
    const items = within(list).getAllByRole('listitem');
    expect(items).toHaveLength(2);
    expect(items[0]).toHaveTextContent('Type: CNAME');
    expect(items[0]).toHaveTextContent('Routes traffic');
    expect(items[0]).not.toHaveAttribute('data-found');
    expect(within(items[0] as HTMLElement).getByRole('textbox', { name: 'Name' })).toHaveValue(
      'help',
    );
    expect(within(items[0] as HTMLElement).getByRole('textbox', { name: 'Value' })).toHaveValue(
      'pages.example.net',
    );
    expect(
      within(items[0] as HTMLElement).getByRole('button', { name: /Copy CNAME record 1 value/ }),
    ).toBeInTheDocument();
    expect(items[1]).toHaveAttribute('data-found', 'true');
    expect(items[1]).toHaveTextContent('Found');
    expect(items[1]).toHaveTextContent('TTL: 300');
    await expectNoA11yViolations();
  });

  it('shows verifying while onVerify runs, then the product status', async () => {
    const user = userEvent.setup();
    const pending = deferred();
    function Demo() {
      const [status, setStatus] = useState<CustomDomainStatus>('pending');
      return (
        <CustomDomainConnect
          domain="help.example.com"
          status={status}
          onConnect={vi.fn()}
          records={RECORDS}
          onVerify={async () => {
            await pending.promise;
            setStatus('active');
          }}
        />
      );
    }
    render(<Demo />);
    await user.click(screen.getByRole('button', { name: 'Verify DNS' }));
    expect(root()).toHaveAttribute('data-step', 'verifying');
    expect(statusText()).toHaveTextContent('Checking your DNS records.');
    expect(within(screen.getByRole('navigation')).getAllByRole('listitem')[2]).toHaveAttribute(
      'aria-current',
      'step',
    );
    pending.resolve();
    await waitFor(() => expect(root()).toHaveAttribute('data-step', 'active'));
    expect(statusText()).toHaveTextContent('Your domain is connected.');
    // Once active, the stepper, records and verify button give way to the result.
    expect(screen.queryByRole('navigation')).not.toBeInTheDocument();
    expect(screen.queryByRole('list', { name: 'DNS records' })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /Verify/ })).not.toBeInTheDocument();
    expect(screen.getByText('Active')).toBeInTheDocument();
  });

  it('shows a failure reason and an error step, and retries with "Check again"', async () => {
    const user = userEvent.setup();
    const onVerify = vi.fn(() => Promise.reject(new Error('DNS lookup timed out.')));
    render(
      <CustomDomainConnect
        domain="help.example.com"
        status="failed"
        failureReason="The CNAME points to another host."
        onConnect={vi.fn()}
        onVerify={onVerify}
        records={RECORDS}
        lastChecked="Checked 2 minutes ago"
      />,
    );
    expect(screen.getByRole('alert')).toHaveTextContent('The CNAME points to another host.');
    expect(within(screen.getByRole('navigation')).getAllByRole('listitem')[2]).toHaveAttribute(
      'data-status',
      'error',
    );
    expect(screen.getByText('Checked 2 minutes ago')).toBeInTheDocument();
    // The ticking "checked" text is outside the live region.
    expect(statusText()).not.toHaveTextContent('Checked');
    await user.click(screen.getByRole('button', { name: 'Check again' }));
    expect(onVerify).toHaveBeenCalledTimes(1);
    expect(await screen.findByText('DNS lookup timed out.')).toBeInTheDocument();
    await expectNoA11yViolations();
  });

  it('removes the domain after confirming, and returns focus to the field', async () => {
    const user = userEvent.setup();
    const onRemove = vi.fn();
    const onDomainChange = vi.fn();
    render(
      <CustomDomainConnect
        defaultDomain="help.example.com"
        onConnect={vi.fn()}
        onRemove={onRemove}
        onDomainChange={onDomainChange}
      />,
    );
    await user.click(screen.getByRole('button', { name: 'Remove domain' }));
    const dialog = await screen.findByRole('alertdialog', { name: 'Remove help.example.com?' });
    // Focus starts on Cancel; Cancel keeps the domain.
    await user.click(within(dialog).getByRole('button', { name: 'Cancel' }));
    expect(onRemove).not.toHaveBeenCalled();
    await user.click(screen.getByRole('button', { name: 'Remove domain' }));
    const again = await screen.findByRole('alertdialog');
    await user.click(within(again).getByRole('button', { name: 'Remove domain' }));
    await waitFor(() => expect(onRemove).toHaveBeenCalledTimes(1));
    expect(onDomainChange).toHaveBeenCalledWith(null);
    await waitFor(() => expect(screen.getByRole('textbox', { name: /Domain/ })).toHaveFocus());
  });

  it('shows product-written provider hints', async () => {
    const user = userEvent.setup();
    render(
      <CustomDomainConnect
        domain="help.example.com"
        onConnect={vi.fn()}
        records={RECORDS}
        providerHints={[
          { id: 'acme', label: 'Acme DNS', steps: ['Open Records.', 'Add the CNAME.'] },
        ]}
      />,
    );
    const select = screen.getByRole('combobox', { name: 'Where is your DNS managed?' });
    expect(screen.queryByRole('list', { name: 'Acme DNS' })).not.toBeInTheDocument();
    await user.selectOptions(select, 'acme');
    const steps = screen.getByRole('list', { name: 'Acme DNS' });
    expect(
      within(steps)
        .getAllByRole('listitem')
        .map((li) => li.textContent),
    ).toEqual(['Open Records.', 'Add the CNAME.']);
    await expectNoA11yViolations();
  });

  it('disables every action when disabled, and translates messages', () => {
    render(
      <CustomDomainConnect
        domain="help.example.com"
        status="failed"
        disabled
        onConnect={vi.fn()}
        onVerify={vi.fn()}
        onRemove={vi.fn()}
        messages={{ remove: 'Entfernen', statusLabels: { failed: 'Fehlgeschlagen' } }}
      />,
    );
    expect(screen.getByRole('button', { name: 'Entfernen' })).toBeDisabled();
    expect(screen.getByRole('button', { name: 'Check again' })).toBeDisabled();
    expect(screen.getByText('Fehlgeschlagen')).toBeInTheDocument();
  });

  it('renders on the server', () => {
    const html = renderToString(
      <CustomDomainConnect domain="help.example.com" onConnect={() => {}} records={RECORDS} />,
    );
    expect(html).toContain('mn-custom-domain-connect');
    expect(html).toContain('pages.example.net');
  });
});
