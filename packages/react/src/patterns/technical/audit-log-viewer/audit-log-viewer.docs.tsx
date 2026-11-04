import { useState } from 'react';
import { Badge, Select, SelectContent, SelectItem, SelectTrigger } from '@rookcom/monarch-react';
import {
  AuditLogViewer,
  auditLogToCsv,
  type AuditLogEvent,
} from '@rookcom/monarch-react/collaboration';
import type { ComponentDocs } from '../../../internal/component-docs.js';

const NOW = '2026-03-02T12:00:00Z';
/** Built lazily: docs files cannot create JSX at module scope. */
const sampleEvents = (): (AuditLogEvent & { area: string })[] => [
  {
    id: 'a1',
    area: 'members',
    time: '2026-03-02T11:04:12Z',
    actor: { name: 'Ana Lopez', detail: 'ana@example.com' },
    action: 'Changed a member’s role',
    target: 'Ben Okafor',
    ip: '203.0.113.24',
    device: 'Firefox on macOS',
    reason: 'Moved to the platform team',
    changes: [
      { field: 'Role', before: 'Member', after: 'Admin' },
      { field: 'Team', before: 'Web', after: 'Platform' },
    ],
  },
  {
    id: 'a2',
    area: 'security',
    time: '2026-03-02T09:47:51Z',
    actor: { name: 'Chloé Martin', detail: 'chloe@example.com' },
    action: 'Signed in',
    ip: '198.51.100.8',
    device: 'Safari on iPhone',
    location: 'Lisbon, Portugal (approximate)',
  },
  {
    id: 'a3',
    area: 'settings',
    time: '2026-03-02T03:00:00Z',
    actor: { name: 'Retention policy', kind: 'automation' },
    action: 'Deleted 14 expired exports',
    target: 'Exports',
    source: <Badge size="sm">Scheduled</Badge>,
    details: <pre>{JSON.stringify({ policy: 'exports-30d', deleted: 14 }, null, 2)}</pre>,
  },
  {
    id: 'a4',
    area: 'settings',
    time: '2026-03-01T15:22:09Z',
    actor: { name: 'Deploy key', kind: 'api', detail: 'Key ending 7f3a' },
    action: 'Updated a webhook',
    target: 'Billing events',
    ip: '192.0.2.44',
    device: 'API client',
    changes: [
      {
        field: 'URL',
        before: 'https://hooks.example.com/v1',
        after: 'https://hooks.example.com/v2',
      },
    ],
  },
];

function download(name: string, text: string) {
  const url = URL.createObjectURL(new Blob([text], { type: 'text/csv' }));
  const a = document.createElement('a');
  a.href = url;
  a.download = name;
  a.click();
  URL.revokeObjectURL(url);
}

function Demo() {
  const [area, setArea] = useState('all');
  const events = area === 'all' ? sampleEvents() : sampleEvents().filter((e) => e.area === area);
  return (
    <div style={{ width: '100%' }}>
      <AuditLogViewer
        events={events}
        filters={
          <Select value={area} onValueChange={setArea}>
            <SelectTrigger aria-label="Area" style={{ width: '12rem' }} />
            <SelectContent>
              <SelectItem value="all">All areas</SelectItem>
              <SelectItem value="members">Members</SelectItem>
              <SelectItem value="security">Security</SelectItem>
              <SelectItem value="settings">Settings</SelectItem>
            </SelectContent>
          </Select>
        }
        onExport={() =>
          download(
            'audit-log.csv',
            auditLogToCsv([
              ['time', 'actor', 'action', 'ip'],
              ...events.map((e) => [new Date(e.time), e.actor?.name, String(e.action), e.ip]),
            ]),
          )
        }
        hasMore
        onLoadMore={() => {}}
        now={NOW}
        timeZone="UTC"
        locale="en-GB"
      />
    </div>
  );
}

export default {
  id: 'audit-log-viewer',
  title: 'AuditLogViewer',
  summary:
    'Security and audit events in a table: actor, action, target, IP and device, a details drawer, filters, export and paging.',
  sections: {
    purpose: {
      summary:
        'Shows who did what, to what, from where and when, for workspace admins and security reviews. Each row opens a drawer with the full record: field-level before and after, reason and any payload.',
      useWhen: [
        'Workspace or product settings › Audit log.',
        'Security events: sign-ins, key use, permission changes.',
        'Any immutable change history that needs export.',
      ],
      avoidWhen: [
        'A record’s own history in context: use ActivityTimeline.',
        'Editable data: audit entries are read-only.',
      ],
    },
    designPrinciples: {
      items: [
        'Absolute times in the table (audits are compared across days); relative time in the drawer for context.',
        'People, automations, systems and API keys are told apart in text, never colour alone.',
        'Changes are a structured before and after, not raw JSON; raw payloads go in `details` when needed.',
        'Filtering, fetching and the export file are the product’s: the viewer offers slots and callbacks.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'Built on Monarch Table, Sheet, DescriptionList and LoadMore.',
        '`auditLogToCsv(rows)` builds an RFC 4180 CSV and defuses spreadsheet formulas; use it for client-side exports of what is loaded, or export on the server for everything.',
        '`selectedId` / `defaultSelectedId` / `onSelectedChange` control the open drawer, e.g. to deep-link an event.',
        'The ref is forwarded to the root `<div>`.',
      ],
    },
    typedProperties: { source: 'typescript' },
    variants: {
      items: [
        {
          prop: 'columns',
          values: ["['time', 'actor', 'action', 'target', 'origin']", 'any subset'],
          description: 'Columns shown, in order. Hidden fields still appear in the drawer.',
        },
        { prop: 'density', values: ['comfortable', 'compact'], description: 'Table row height.' },
      ],
    },
    states: {
      items: [
        { name: 'Loading', description: 'Skeleton rows in place of the table.' },
        {
          name: 'Error',
          description: 'The `error` node (for example a permission note) replaces the table.',
        },
        {
          name: 'Empty',
          description: '“No events match”, or the `empty` node (an EmptyState with Clear filters).',
        },
        {
          name: 'Selected',
          attribute: 'aria-selected="true"',
          description: 'The row whose drawer is open.',
        },
        {
          name: 'Exporting',
          description: 'The export button shows a spinner and stays focusable.',
        },
      ],
    },
    interaction: {
      items: [
        'Details opens a side drawer with every field, the changes table, `details` and `renderDetails(event)`.',
        'Closing the drawer (Escape, Close or outside click) returns focus to the row’s Details button, also when the product opened it.',
        'Export calls `onExport`; LoadMore calls `onLoadMore` for the next cursor page.',
      ],
    },
    keyboard: {
      items: [
        {
          keys: 'Tab',
          action: 'Moves through filters, Export, each row’s Details button and Load more.',
        },
        { keys: 'Enter / Space', action: 'Opens the details drawer for the row.' },
        { keys: 'Escape', action: 'Closes the drawer.' },
      ],
    },
    accessibility: {
      role: 'table, with a dialog for details',
      name: 'The table is labelled “Audit log” (`messages.caption`); the drawer is named by the event’s action.',
      items: [
        'Each Details button is named with its row’s action (“Details Changed a member’s role”).',
        'The changes table uses row headers for field names.',
        'Times are `<time datetime>` elements.',
        'Actor kinds are text tags (“Automation”, “System”, “API”).',
      ],
    },
    responsive: {
      items: [
        'A container query hides IP and device below 44rem; below 32rem the target is hidden and the time moves under the action. Everything stays in the drawer.',
        'The toolbar wraps; the table scrolls horizontally inside its wrapper if it still does not fit.',
        'The drawer becomes full width on phones (Sheet).',
      ],
    },
    theme: {
      items: [
        'Table tokens from DataTable; IP addresses in `font-mono`; before values in `danger-strong`, after values in `success-strong`.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Workspace audit log',
          render: () => <Demo />,
        },
      ],
    },
    doDont: {
      do: [
        'Write actions as short sentences in the past tense.',
        'Record and show the reason when the product asks for one.',
        'Show approximate locations honestly (“approximate”).',
      ],
      dont: [
        'Do not let admins edit or delete entries from this view.',
        'Do not show secrets or full tokens in `details`.',
        'Do not export only the loaded page without saying so; export on the server for the full range.',
      ],
    },
    related: {
      items: [
        { id: 'activity-timeline', reason: 'History in the context of one record.' },
        { id: 'data-table', reason: 'The table primitives it uses.' },
        { id: 'sheet', reason: 'The details drawer.' },
        { id: 'filter-bar', reason: 'Filters for the `filters` slot.' },
        { id: 'load-more', reason: 'Cursor pagination.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: [
        'avatar',
        'button',
        'relative-time',
        'description-list',
        'data-table',
        'load-more',
        'skeleton',
        'sheet',
      ],
    },
    testing: {
      unit: [
        'Labelled table with all columns, actor kinds and unknown actors.',
        'Details buttons named by their row.',
        'Drawer with fields, the changes table and payload; Escape closes and returns focus.',
        'Controlled selection and `renderDetails`.',
        'Filters slot, export (and exporting), cursor pagination.',
        'Column choice keeps hidden fields in the drawer; loading, error and empty.',
        '`auditLogToCsv` quoting, escaping and formula defusing.',
      ],
      axe: true,
      ssr: true,
      browser: [
        'Visual baselines: /__gallery/audit-log-viewer (table and open drawer)',
        'Cross-browser axe: /components/audit-log-viewer',
      ],
    },
  },
} satisfies ComponentDocs;
