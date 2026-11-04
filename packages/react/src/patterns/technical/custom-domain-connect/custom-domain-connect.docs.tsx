import { useState } from 'react';
import {
  CustomDomainConnect,
  type CustomDomainRecord,
  type CustomDomainStatus,
} from '@rookcom/monarch-react/patterns';
import type { ComponentDocs } from '../../../internal/component-docs.js';

const RECORDS: CustomDomainRecord[] = [
  {
    type: 'CNAME',
    name: 'help',
    value: 'pages.example.net',
    purpose: 'Points your address to the page',
  },
  {
    type: 'TXT',
    name: '_verify.help',
    value: 'verify=4f1c9a2e7b',
    purpose: 'Proves you own the domain',
    ttl: 'Auto',
  },
];

function FlowDemo() {
  const [status, setStatus] = useState<CustomDomainStatus>('pending');
  const [checks, setChecks] = useState(0);
  const wait = () => new Promise((r) => setTimeout(r, 900));
  return (
    <CustomDomainConnect
      requireSubdomain
      inputDescription="The address visitors will type, such as help.example.com."
      status={status}
      records={RECORDS.map((r, i) => (checks > 0 ? { ...r, found: i === 0 || checks > 1 } : r))}
      onConnect={wait}
      onVerify={async () => {
        await wait();
        setChecks((c) => c + 1);
        setStatus(checks > 0 ? 'active' : 'failed');
      }}
      failureReason="The TXT record was not found yet. DNS changes can take a while to appear."
      onRemove={async () => {
        await wait();
        setStatus('pending');
        setChecks(0);
      }}
      onDomainChange={(d) => {
        if (d === null) setStatus('pending');
      }}
    />
  );
}

export default {
  id: 'custom-domain-connect',
  title: 'CustomDomainConnect',
  summary:
    'Connect a custom domain: enter it, copy the DNS records, verify, and see pending, verifying, active or failed.',
  sections: {
    purpose: {
      summary:
        'One flow for every product that serves a page on the customer’s own domain (support pages, booking pages, status pages, careers pages). It handles the steps, syntax checking, copyable records and status; the product runs the DNS checks. Import from `@rookcom/monarch-react/patterns`.',
      useWhen: [
        'A customer points their own domain or subdomain at a page the product hosts.',
        'The product gives DNS records to add and can check them on request or on a timer.',
      ],
      avoidWhen: [
        'Showing a single value to copy, such as an API key or webhook URL: use CopyField or SecretField.',
        'Picking a sub-path or slug on the product’s own domain: use an Input with a prefix.',
      ],
    },
    designPrinciples: {
      items: [
        'Four plain steps (Domain, DNS records, Verify, Live) shown with Stepper, so people know how far along they are.',
        'Mistakes are caught before the network: what people paste (https://, paths, ports, capitals, a trailing dot) is cleaned up, and anything still not a host name is explained.',
        'Every record value is a CopyField, so nothing is retyped by hand.',
        'No provider copy is built in. Menus at DNS providers change, so the product passes provider hints it maintains.',
        'Status is text and a badge, never colour alone; the failure reason is the product’s own words.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'Renders a `<section>` (ref) with `data-step` set to `enter`, `pending`, `verifying`, `active` or `failed`.',
        '`onConnect(domain)` receives the normalised domain; reject with an Error to show its message under the field. `onVerify()` and `onRemove()` may return promises.',
        '`domain` / `defaultDomain` / `onDomainChange` hold the connected domain (`null` before connecting and after removal). `status` comes from the product.',
        '`normalizeDomain(input)` and `domainSyntaxError(input, { requireSubdomain })` are exported for server-side reuse.',
      ],
    },
    typedProperties: { source: 'typescript' },
    variants: {
      items: [
        {
          prop: 'status',
          values: ['pending', 'verifying', 'active', 'failed'],
          description:
            'Set by the product from its DNS checks. While an `onVerify` promise runs the component shows `verifying` on its own.',
        },
        {
          prop: 'requireSubdomain',
          values: ['false', 'true'],
          description:
            '`true` asks for at least three labels (help.example.com). It cannot know public suffixes such as co.uk; add `validate` for exact rules.',
        },
        {
          prop: 'showRecordsWhenActive',
          values: ['false', 'true'],
          description: 'Keep the records visible after the domain is active, for reference.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Enter',
          attribute: 'data-step="enter"',
          description: 'No domain yet: the field and Connect button, step 1 current.',
        },
        {
          name: 'Invalid',
          attribute: 'aria-invalid on the input',
          description:
            'Syntax, `validate` or `onConnect` error under the field; focus returns to it.',
        },
        {
          name: 'Connecting',
          attribute: 'aria-busy on Connect',
          description: 'While the `onConnect` promise runs.',
        },
        {
          name: 'Pending',
          attribute: 'data-step="pending"',
          description: 'Records to add, a “Waiting for DNS” badge and the Verify button.',
        },
        {
          name: 'Verifying',
          attribute: 'data-step="verifying"',
          description: 'Spinner, “Checking your DNS records.”, Verify busy.',
        },
        {
          name: 'Active',
          attribute: 'data-step="active"',
          description: 'Success text and badge; the stepper, records and Verify are hidden.',
        },
        {
          name: 'Failed',
          attribute: 'data-step="failed"',
          description:
            'The Verify step shows an error, the product’s `failureReason` is an alert, and the button reads “Check again”.',
        },
        {
          name: 'Record found',
          attribute: 'data-found="true|false" on a record',
          description: '“Found” or “Not found yet” per record, from the product’s last check.',
        },
        { name: 'Disabled', description: 'Every action disabled, for people who may only view.' },
      ],
    },
    interaction: {
      items: [
        'Connect normalises the text, checks syntax, runs `validate`, then calls `onConnect`. On success focus moves to the domain heading so screen-reader users hear the next step.',
        'Verify calls `onVerify`; Remove opens a ConfirmDialog and calls `onRemove`, then focus returns to the empty domain field.',
        'Choosing a provider shows that provider’s steps, written by the product.',
        'Polling is the product’s job: re-render with a new `status`, `records[].found` and `lastChecked` as checks complete.',
      ],
    },
    keyboard: {
      items: [
        { keys: 'Enter', action: 'In the domain field, connects (it is a form).' },
        {
          keys: 'Tab',
          action: 'Moves through the field, copy buttons, provider picker and actions.',
        },
        {
          keys: 'Enter / Space',
          action: 'Activates Connect, a copy button, Verify or Remove.',
        },
        { keys: 'Escape', action: 'Closes the remove confirmation without removing.' },
      ],
    },
    accessibility: {
      role: 'section (a region landmark only when given an aria-label)',
      name: 'None by default, so several on one page do not repeat a landmark; pass `aria-label` to make it a named region.',
      items: [
        'The stepper is a nav named “Custom domain setup”, then “Setup of {domain}” once connected, with `aria-current="step"` and hidden “Step n of 4” text.',
        'The domain field is a required Field: its error is linked with `aria-describedby` and announced politely.',
        'The status sentence is a polite live region; “last checked” text sits outside it so a ticking time is not announced. Failure reasons are `role="alert"`.',
        'Each copy button is named after its record (“Copy CNAME record 1 value”); record type is read with a hidden “Type:” prefix.',
        'Remove is confirmed in an alertdialog whose focus starts on Cancel.',
        'Verified with axe in jsdom and in Chromium, Firefox and WebKit on /components/custom-domain-connect.',
      ],
    },
    responsive: {
      items: [
        'The domain field and Connect button wrap; under 30rem Connect takes the full width.',
        'Record cards put Name and Value side by side, and stack them under 30rem. Long values scroll inside their CopyField instead of widening the page.',
        'The stepper collapses to “Step n of 4” in narrow containers.',
      ],
    },
    theme: {
      items: [
        'Monarch tokens only: surfaces from `--mn-color-bg-surface` and `--mn-color-bg-subtle`, found records outlined with `--mn-color-success-border`.',
        'Status badges use the warning, info, success and danger tones, so they follow light, dark and high-contrast themes.',
        'Spinners stop under `prefers-reduced-motion: reduce` (Spinner’s own rule).',
      ],
    },
    examples: {
      items: [
        {
          title: 'Full flow (simulated checks)',
          stack: true,
          render: () => <FlowDemo />,
          code: `<CustomDomainConnect
  requireSubdomain
  status={status}
  records={records}
  failureReason={reason}
  onConnect={(domain) => api.addDomain(domain)}
  onVerify={async () => setStatus(await api.checkDomain())}
  onRemove={() => api.removeDomain()}
/>`,
        },
        {
          title: 'Pending, with records and provider hints',
          stack: true,
          render: () => (
            <CustomDomainConnect
              defaultDomain="help.example.com"
              status="pending"
              onConnect={() => {}}
              onVerify={() => {}}
              onRemove={() => {}}
              records={[
                { ...(RECORDS[0] as CustomDomainRecord), found: true },
                { ...(RECORDS[1] as CustomDomainRecord), found: false },
              ]}
              lastChecked="Checked 2 minutes ago"
              providerHints={[
                {
                  id: 'generic',
                  label: 'Most providers',
                  steps: [
                    'Open the DNS settings for example.com.',
                    'Add each record above with its exact type, name and value.',
                    'Save, then come back and verify.',
                  ],
                },
              ]}
            />
          ),
          code: `<CustomDomainConnect
  domain="help.example.com"
  status="pending"
  records={records}            // { type, name, value, purpose?, ttl?, found? }
  lastChecked="Checked 2 minutes ago"
  providerHints={hints}        // written and maintained by the product
  onConnect={…} onVerify={…} onRemove={…}
/>`,
        },
        {
          title: 'Failed with a reason',
          stack: true,
          render: () => (
            <CustomDomainConnect
              defaultDomain="status.example.org"
              status="failed"
              failureReason="status.example.org points to another host. Change the CNAME value to pages.example.net."
              onConnect={() => {}}
              onVerify={() => {}}
              records={RECORDS.slice(0, 1).map((r) => ({ ...r, name: 'status', found: false }))}
            />
          ),
          code: `<CustomDomainConnect domain="status.example.org" status="failed" failureReason="…" … />`,
        },
        {
          title: 'Active',
          stack: true,
          render: () => (
            <CustomDomainConnect
              defaultDomain="book.example.com"
              status="active"
              onConnect={() => {}}
              onRemove={() => {}}
              records={RECORDS}
            />
          ),
          code: `<CustomDomainConnect domain="book.example.com" status="active" onRemove={…} … />`,
        },
      ],
    },
    doDont: {
      do: [
        'Pass the reason a check failed in plain words, with what to change.',
        'Give record purposes (“Proves you own the domain”) so people know why each one is needed.',
        'Keep checking in the background and re-render with the new status; tell people they can leave the page.',
      ],
      dont: [
        'Do not hard-code provider menus in shared code; pass them as `providerHints` from the product, where they can be kept current.',
        'Do not rely on client-side syntax checks for security; the server must validate again.',
        'Do not announce a ticking “checked x seconds ago” time; pass it as `lastChecked`, which is not live.',
      ],
    },
    related: {
      items: [
        { id: 'copy-field', reason: 'One value with a copy button; used for every record here.' },
        { id: 'stepper', reason: 'The step indicator this pattern uses.' },
        { id: 'confirm-dialog', reason: 'Confirms removal.' },
        { id: 'secret-field', reason: 'For tokens that must be hidden until revealed.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: [
        'button',
        'copy-field',
        'badge',
        'alert',
        'spinner',
        'form-field',
        'text-field',
        'confirm-dialog',
        'stepper',
      ],
    },
    testing: {
      unit: [
        '`normalizeDomain` strips schemes, paths, ports and trailing dots, lowercases, and converts international names to punycode.',
        '`domainSyntaxError` rejects empty, single-label, underscore, hyphen-edged, empty-label, numeric-TLD, wildcard, over-long label and over-long names; `requireSubdomain`.',
        'Step 1 validates before `onConnect`, links the error and focuses the field; typing clears it; axe.',
        'Connects the normalised domain (uncontrolled), shows a busy button, focuses the domain heading, reports `onDomainChange`; axe.',
        'Rejected `onConnect` and product `validate` messages under the field.',
        'Records: type, purpose, TTL, found state, named Name and Value copy fields; axe.',
        'Verifying while `onVerify` runs, then active: stepper, records and Verify hidden.',
        'Failed: alert with the reason, error step, “Check again”, rejected verify message, `lastChecked` outside the live region; axe.',
        'Remove: confirm dialog, Cancel keeps the domain, confirm calls `onRemove` and returns focus to the field.',
        'Provider hints picker; disabled; translated messages; renders on the server.',
      ],
      axe: true,
      ssr: true,
      browser: [
        'Visual baselines: /__gallery/custom-domain-connect',
        'Cross-browser axe: /components/custom-domain-connect',
      ],
    },
  },
} satisfies ComponentDocs;
