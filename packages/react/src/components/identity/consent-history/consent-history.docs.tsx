import {
  ConsentHistory,
  DEFAULT_CONSENT_CATEGORIES,
  type ConsentRecord,
} from '@rookcom/monarch-react';
import type { ComponentDocs } from '../../../internal/component-docs.js';

const wait = (ms: number) => new Promise<void>((resolve) => window.setTimeout(resolve, ms));

const records: ConsentRecord[] = [
  {
    id: 'c-3',
    regime: 'opt-in',
    policyVersion: '2026-04',
    categories: { necessary: true, preferences: true, analytics: true, marketing: false },
    method: 'custom',
    gpc: false,
    at: '2026-04-18T08:42:00.000Z',
  },
  {
    id: 'c-2',
    regime: 'opt-in',
    policyVersion: '2026-01',
    categories: { necessary: true, preferences: true, analytics: true, marketing: true },
    method: 'accept-all',
    gpc: false,
    at: '2026-02-03T16:10:00.000Z',
  },
  {
    id: 'c-1',
    regime: 'opt-in',
    policyVersion: '2026-01',
    categories: { necessary: true, preferences: false, analytics: false, marketing: false },
    method: 'reject-all',
    gpc: false,
    at: '2026-01-12T10:05:00.000Z',
  },
];

const optOutRecords: ConsentRecord[] = [
  {
    id: 'u-2',
    regime: 'opt-out',
    policyVersion: '2026-03',
    categories: { necessary: true, preferences: true, analytics: true, marketing: false },
    method: 'gpc',
    gpc: true,
    at: '2026-03-21T19:30:00.000Z',
  },
  {
    id: 'u-1',
    regime: 'opt-out',
    policyVersion: '2026-03',
    categories: { necessary: true, preferences: true, analytics: true, marketing: true },
    method: 'acknowledged',
    gpc: false,
    at: '2026-03-20T09:00:00.000Z',
  },
];

export default {
  id: 'consent-history',
  title: 'ConsentHistory',
  summary:
    'A person’s recorded consent decisions, newest first, with method, policy version, rules and each category in words.',
  sections: {
    purpose: {
      summary:
        'Shows the ConsentRecord entries a product stored from `onRecord`, so a person can see what they chose and when, and an administrator can answer “what did this person agree to?”. Every value is written out in words: how the decision was made, which policy version and rules applied, and every category allowed or not, with an icon and text. It displays records; it does not judge whether a record is valid in any jurisdiction. The product’s legal team decides the regime per country and the wording.',
      useWhen: [
        'A privacy or account settings page that lists the person’s past consent choices.',
        'An admin or support view of one person’s consent records.',
      ],
      avoidWhen: [
        'Changing consent now: use ConsentPreferences (or the banner).',
        'A general audit trail of many kinds of events: use AuditLogViewer.',
      ],
    },
    designPrinciples: {
      items: [
        'Words, not codes: methods and regimes are written out ("Accepted all", "Asked before use (opt-in)"); unknown category ids are shown as is rather than hidden.',
        'Never colour alone: every category shows a check or cross icon and the text "Allowed" or "Not allowed".',
        'Newest first, whatever order the product passes.',
        'Layout choice: a semantic table on wide containers, because each decision has the same five facts and people compare decisions row by row; below 40rem (640px) each row becomes a stacked card with visible labels, because five columns cannot fit at phone width without sideways scrolling. A description list per record was rejected: it repeats every term for every record on wide screens and loses the column comparison. DataTable’s full grid (sorting, selection, pagination) was not needed, so it is built from the Table primitives in data-table.',
        'Interaction pattern, not legal advice: the component names no law and claims no compliance.',
      ],
    },
    apiReference: { source: 'typescript' },
    typedProperties: {
      source: 'typescript',
      notes: [
        '`records` takes `ConsentRecord[]` from consent-manager, exactly as `onRecord` delivered them.',
        '`messages.methods` and `messages.regimes` are maps keyed by `ConsentMethod` and `ConsentRegime`; pass only the entries you want to change.',
        'Pass `locale` and `timeZone` when rendering on the server so the date text matches on the client.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'showRelative',
          values: ['false', 'true'],
          description: 'Adds "3 days ago" under each date (RelativeTime, without its tooltip).',
        },
        {
          prop: 'onExport',
          values: ['undefined', '() => unknown'],
          description:
            'Adds a download button to the header; the product builds the CSV or JSON file.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Empty',
          attribute: 'data-empty',
          description:
            'No records: an EmptyState (or `empty`) replaces the table and the download button.',
        },
        {
          name: 'Exporting',
          attribute: 'aria-busy (download button)',
          description: 'While the promise from `onExport` is pending; further clicks are ignored.',
        },
        {
          name: 'Export failed',
          attribute: 'role="alert"',
          description:
            '`onExport` threw or rejected: `messages.exportFailed` is shown under the header.',
        },
        {
          name: 'Category allowed / not allowed',
          attribute: 'data-allowed="true|false"',
          description: 'On each category item, next to the icon and text.',
        },
        {
          name: 'Method',
          attribute: 'data-method',
          description: 'On each row, the record’s `method`, for product styling or tests.',
        },
      ],
    },
    interaction: {
      items: [
        'Read-only apart from the optional download button.',
        'With `showRelative`, the relative text updates after hydration; pass `now` for a fixed reference.',
      ],
    },
    keyboard: {
      items: [{ keys: 'Tab', action: 'Moves to the download button, when there is one.' }],
      notes: ['The list itself has no interactive parts.'],
    },
    accessibility: {
      role: 'table (explicit table, row, columnheader and cell roles, kept when stacked)',
      name: 'The heading (`title`), through `aria-labelledby`.',
      items: [
        'The explicit table roles keep row and column semantics when narrow containers turn the cells into blocks.',
        'In the stacked layout the visible cell labels are hidden from assistive technology; the column headers still name each cell.',
        'Each category reads as "Analytics: Allowed"; the icon is decorative.',
        'Dates are `<time datetime>` elements with the record’s ISO value.',
        'An export failure is announced with `role="alert"`.',
      ],
    },
    responsive: {
      items: [
        'A container query, not the viewport: the table stacks when the component itself is narrower than 40rem, so it works in a side panel too.',
        'Below 22rem the label sits above each value.',
        'Long version strings and category ids wrap; there is no sideways scrolling at 390px.',
        'The download button is 44px tall on coarse pointers.',
      ],
    },
    theme: {
      items: [
        'Table styles come from data-table; the allowed icon uses `--mn-color-success-strong`, the not-allowed icon `--mn-color-text-muted`.',
        'Policy versions use `--mn-font-mono`.',
        'Forced-colors mode draws icons and row separators in CanvasText.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Privacy page, with labels from the categories',
          render: () => (
            <ConsentHistory
              records={records}
              categories={DEFAULT_CONSENT_CATEGORIES}
              description="Every choice you made about cookies and similar storage."
              locale="en-GB"
              timeZone="UTC"
            />
          ),
        },
        {
          title: 'Admin view with download and relative time',
          render: () => (
            <ConsentHistory
              records={optOutRecords}
              categories={DEFAULT_CONSENT_CATEGORIES}
              title="Consent records"
              headingLevel="h3"
              showRelative
              onExport={() => wait(800)}
              exportLabel="Download as CSV"
            />
          ),
        },
        {
          title: 'Narrow container (stacked rows)',
          render: () => (
            <div style={{ maxWidth: 360 }}>
              <ConsentHistory
                records={records.slice(0, 2)}
                categories={DEFAULT_CONSENT_CATEGORIES}
                locale="en-GB"
                timeZone="UTC"
              />
            </div>
          ),
        },
        {
          title: 'No records yet',
          render: () => <ConsentHistory records={[]} />,
        },
      ],
    },
    doDont: {
      do: [
        'Store every record from `onRecord` and pass them all; the component sorts them.',
        'Pass the same `categories` as the ConsentProvider so ids show as labels.',
        'Name the file format in `exportLabel` ("Download as CSV").',
        'Have the product’s legal team review the regime and method wording in `messages`.',
      ],
      dont: [
        'Do not edit or drop records before showing them; the list is proof of what was chosen.',
        'Do not add copy that claims the history satisfies a particular law.',
        'Do not rely on the icon colour to say whether a category was allowed.',
      ],
    },
    related: {
      items: [
        { id: 'consent-manager', reason: 'Produces the records through `onRecord`.' },
        { id: 'consent-preferences', reason: 'Where the person changes their choice.' },
        { id: 'data-export-request', reason: 'Download a copy of all of an account’s data.' },
        { id: 'audit-log-viewer', reason: 'A filterable log of many kinds of events.' },
        { id: 'data-table', reason: 'The table primitives it is built from.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: ['button', 'data-table', 'relative-time', 'empty-state'],
      notes: ['Types `ConsentRecord` and `ConsentCategory` come from consent-manager (type-only).'],
    },
    testing: {
      unit: [
        'Sorts newest first and formats dates with a fixed locale (en-GB) and time zone (Europe/Berlin), with `datetime` attributes; axe passes.',
        'Writes method, regime, policy version, the GPC note and each category (product order, then unknown ids) in words.',
        'Shows raw ids without `categories`; `messages` rewords methods and states.',
        'Empty state replaces the table and hides the download button; axe passes.',
        'onExport: busy while pending, ignores repeat clicks, shows an alert on rejection and keeps focus.',
        'Relative time against a fixed `now`.',
        'Renders on the server with the same date text.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
