import { DnsProviderGuide, DnsRecordTable, type DnsRecord } from '@rookcom/monarch-react/patterns';
import type { ComponentDocs } from '../../../internal/component-docs.js';

const wide = { width: '100%', maxWidth: '44rem' } as const;

const routing: DnsRecord = {
  type: 'CNAME',
  name: 'book.example.com',
  value: 'domains.example.net',
  purpose: 'routing',
  ok: false,
};

const records: DnsRecord[] = [
  routing,
  {
    type: 'TXT',
    name: '_verify.book.example.com',
    value: 'site-verification=4f1a9c2e',
    purpose: 'verification',
    ok: true,
  },
];

export default {
  id: 'dns-record-table',
  title: 'DnsRecordTable',
  summary: 'DNS records to add at a provider, with copy buttons, purpose and found state.',
  sections: {
    purpose: {
      summary:
        'Shows the DNS records a user must create to connect a custom domain or verify ownership: type, name and value with copy buttons, what each record is for, and whether public DNS already shows it. DnsProviderGuide adds step-by-step instructions for common DNS providers. Needed by: Tickets, Calendar, Recruit, Base, Pulse.',
      useWhen: [
        'A custom domain or verification flow asks the user to add CNAME, TXT, A or MX records.',
        'The product re-checks DNS and can say which records are already found.',
      ],
      avoidWhen: [
        'Editing a live DNS zone (add, edit, delete records): use DataTable with row actions.',
        'One value to copy with no record structure: use CopyField.',
      ],
    },
    designPrinciples: {
      items: [
        'Typed the way providers want it: names relative to the zone (`book`, `@`) with the full name underneath.',
        'Every value is one click to copy, and still selectable by hand if the clipboard is blocked.',
        'Cards by default, because a three-column table does not fit a phone; a compact table for wide admin screens.',
        'Data in, no lookups: the product decides which records exist and whether they are found.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'Also exports `DnsProviderGuide`, `defaultDnsProviders` and `relativeDnsName(name, zone)`.',
        'Forwards its ref and other props to the root `<div>`.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        'Migrating from the app copy (`DnsRecordCards` in Calendar `DomainsManager.tsx`, Recruit `CareerDomainCard.tsx`, Base `admin/[id]/domain/page.tsx`, Pulse `status-pages/[pageId]/page.tsx`, and the inline cards in Tickets `PortalDomain.tsx`): `records` is unchanged (`type`, `name`, `value`, `ok`, `purpose`); `host` plus `relative(name, host)` becomes `zone` (pass the registered domain, e.g. `example.com`); `recordPurposeLabel()` is `messages.purpose`.',
        '`DnsProviderGuide type name fullName value` becomes `DnsProviderGuide record={…} zone={…}`. Pass `providers` to translate or extend the built-in steps.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'layout',
          values: ['cards', 'table'],
          description:
            '`cards` (default) stacks a card per record; `table` is a compact grid that scrolls inside itself when narrow.',
        },
        {
          prop: 'headingLevel',
          values: ['h2', 'h3', 'h4', 'h5'],
          description: 'Level of each card’s purpose heading. Default `h3`.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Found',
          attribute: 'data-ok="true"',
          description: 'Public DNS shows the record. Green "Found" badge and border.',
        },
        {
          name: 'Waiting',
          attribute: 'data-ok="false"',
          description: 'Not seen yet. Neutral "Waiting" badge.',
        },
        {
          name: 'Not checked',
          attribute: 'no data-ok',
          description: '`ok` unset: no badge and no summary.',
        },
      ],
    },
    interaction: {
      items: [
        'Each name and value has a copy button; copying confirms like CopyButton and calls `onCopy`.',
        'Focusing a value selects all of it.',
        'DnsProviderGuide: picking a provider swaps the steps below.',
      ],
    },
    keyboard: {
      items: [
        {
          keys: 'Tab',
          action: 'Moves through each name, its copy button, each value, its copy button.',
        },
        { keys: 'Enter / Space', action: 'On a copy button: copy.' },
        { keys: 'Arrow keys', action: 'Table layout: scroll the focused table region sideways.' },
      ],
    },
    accessibility: {
      role: 'list (cards) or region containing a table',
      name: '`aria-label`, or `messages.label(zone)`: "DNS records for example.com".',
      items: [
        'Each card has a heading (its purpose) and a description list of type, name and value.',
        'Copy boxes and buttons are named per record ("Copy value of the CNAME record book") so they are distinguishable.',
        'Found and waiting are text, not colour alone.',
        'A visually hidden status ("1 of 2 records found.") is announced when the found count changes.',
        'The table layout’s scroll region is focusable so keyboard users can scroll it.',
      ],
    },
    responsive: {
      items: [
        'Cards stack type, name and value under 36rem of container width and put them on one row above it (container query).',
        'Long values wrap or scroll inside their field; the page never scrolls sideways at 390px.',
        'Type, TTL and priority boxes are 44px tall on coarse pointers.',
      ],
    },
    theme: {
      items: [
        'Surfaces, borders and text use `--mn-color-*` tokens and follow every theme.',
        'Values use `--mn-font-mono`.',
        'Forced colours: card and table borders use `CanvasText`.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Cards with found state',
          stack: true,
          render: () => (
            <div style={wide}>
              <DnsRecordTable records={records} zone="example.com" />
            </div>
          ),
        },
        {
          title: 'Compact table',
          stack: true,
          render: () => (
            <div style={wide}>
              <DnsRecordTable
                layout="table"
                zone="example.com"
                records={[
                  {
                    type: 'MX',
                    name: 'example.com',
                    value: 'mx1.example.net',
                    priority: 10,
                    ttl: 3600,
                  },
                  {
                    type: 'TXT',
                    name: 'example.com',
                    value: 'v=spf1 include:example.net ~all',
                    ttl: 3600,
                  },
                ]}
              />
            </div>
          ),
        },
        {
          title: 'Provider guide',
          stack: true,
          render: () => (
            <div style={wide}>
              <DnsProviderGuide record={routing} zone="example.com" />
            </div>
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Pass `zone` so names read the way DNS providers expect.',
        'Show DnsProviderGuide for the first record that is not found yet.',
        'Pair with DomainProgress when the product tracks connection stages.',
      ],
      dont: [
        'Do not set `ok` before the product has actually checked DNS.',
        'Do not use it as a zone editor.',
      ],
    },
    related: {
      items: [
        { id: 'domain-progress', reason: 'Stage-by-stage progress for the same domain.' },
        { id: 'copy-field', reason: 'The copyable boxes it uses.' },
        { id: 'status-badge', reason: 'The found and waiting badges.' },
        { id: 'data-table', reason: 'For editable DNS zones.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: ['copy-field', 'status-badge', 'form-field'],
    },
    testing: {
      unit: [
        '`relativeDnsName` for subdomains, the apex, trailing dots and names outside the zone.',
        'Cards: headings, found/waiting badges, relative names, full name, found summary.',
        'Copy from the keyboard calls the clipboard and `onCopy`.',
        'Tab order through names and values.',
        'Table layout: column headers, optional priority and TTL, focusable scroll region.',
        'Translated messages and custom purposes.',
        'DnsProviderGuide: default provider, switching providers, custom providers.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
