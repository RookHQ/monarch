import { PreviewRows, type PreviewRow } from '@rookcom/monarch-react/data';
import type { ComponentDocs } from '../../../internal/component-docs.js';

const COLUMNS = [
  { id: 'name', label: 'Name' },
  { id: 'email', label: 'Email' },
  { id: 'country', label: 'Country' },
];

const ROWS: PreviewRow[] = [
  { row: 1, values: { name: 'Ana Ruiz', email: 'ana@example.com', country: 'Spain' } },
  {
    row: 2,
    values: { name: '', email: 'not-an-email', country: 'France' },
    errors: [
      { field: 'Email', message: 'Not an email address' },
      { field: 'Name', message: 'Required' },
    ],
  },
  {
    row: 3,
    values: { name: 'Sam Lee', email: 'sam@example.com', country: 'Atlantis' },
    warnings: [{ field: 'Country', message: 'Not recognised; left empty' }],
  },
  {
    row: 4,
    values: { name: 'Kim Park', email: 'kim@example.com', country: 'Korea' },
    status: 'duplicate',
    note: <a href="#record-9">Matches an existing record</a>,
  },
  { row: 5, values: { name: 'Lee Chen', email: 'lee@example.com', country: 'Canada' } },
];

export default {
  id: 'preview-rows',
  title: 'PreviewRows',
  summary: 'An import file’s rows with what will happen to each and why.',
  sections: {
    purpose: {
      summary:
        'Shows, before anything is saved, how each row of an imported file will be treated: ready, imported with warnings, skipped for errors, or a duplicate, with the values that will be used and every problem by field. Needed by: Intellect (portfolio import), Atlas and Dots (CSV import), and ImportWizard.',
      useWhen: [
        'An import or bulk update has a dry-run step and people must see row-level problems before they commit.',
        'Inside ImportWizard’s preview step, or on its own after a server-side validation.',
      ],
      avoidWhen: [
        'Showing records that already exist: use DataTable.',
        'Reporting what an import did after the fact: use ImportWizard’s result report.',
      ],
    },
    designPrinciples: {
      items: [
        'Invalid rows are never silently dropped: every row is listed and counted.',
        'Status is a word and an icon, never colour alone; problems name the field they belong to.',
        'Validation stays in the product: rows arrive with their `errors`, `warnings` and `status`.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'Renders a `<div>` (ref) with optional TabsFilter tabs and a DataTable (compact, cards on narrow screens).',
        '`previewRowStatus(row)` and `countPreviewRows(rows)` are exported for summaries elsewhere.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        'Migrating from the app copy (Intellect `PreviewRows`): each `results[i]` becomes `{ row, values: { title, type, jurisdiction, owner, stage }, errors, warnings, status: ok ? undefined : "error" }` (merge `raw` values in the product); the fixed headers become `columns`. The table/card switch is built in.',
        'From Atlas `PreviewTable`: `status: "invalid"` → `"error"`, `duplicate_existing` / `duplicate_in_file` → `"duplicate"`, the existing-domain link → `note`; the All / Errors / Duplicates buttons are the built-in filter tabs.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'layout',
          values: ['auto', 'table', 'cards'],
          description: '`auto` (default) shows a table and switches to cards on narrow screens.',
        },
        {
          prop: 'filterable',
          values: ['true', 'false'],
          description: 'Filter tabs with counts, shown only when some rows are not ready.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Ready',
          attribute: 'data-status="ready" (badge)',
          description: 'Success badge “Ready”.',
        },
        {
          name: 'Warning',
          attribute: 'data-status="warning"',
          description: 'Warning badge; warnings listed in warning text.',
        },
        {
          name: 'Error',
          attribute: 'data-status="error"',
          description: 'Danger badge; errors listed in danger text. These rows are skipped.',
        },
        {
          name: 'Duplicate',
          attribute: 'data-status="duplicate"',
          description: 'Info badge and an optional `note` (link to the match).',
        },
        {
          name: 'Filtered',
          attribute: 'data-filter',
          description: 'Only rows with the chosen result; an empty filter offers to show all.',
        },
        { name: 'Empty', description: '“No rows to preview” when the file has no data rows.' },
      ],
    },
    interaction: {
      items: [
        'Choosing a filter tab shows only rows with that result and resets to the first page.',
        'Files longer than `pageSize` rows get a Pagination footer.',
      ],
    },
    keyboard: {
      items: [
        { keys: 'Tab', action: 'Moves to the filter tabs, links in notes and the pagination.' },
        {
          keys: 'ArrowLeft / ArrowRight',
          action: 'Moves between filter tabs and applies the filter.',
        },
      ],
    },
    accessibility: {
      role: 'tablist (filter) and table',
      name: '`caption` names the table; the tabs are named by `messages.filter`.',
      items: [
        'Each problem is prefixed with a visually hidden “Error:” or “Warning:”.',
        'Row numbers are row headers, so screen readers announce “Row 4” with each cell.',
        'The status icon is decorative; the status word is the badge text.',
      ],
    },
    responsive: {
      items: [
        'Cards below 640 px: one card per row with every column labelled.',
        'Long values wrap; the tabs scroll sideways if they do not fit.',
      ],
    },
    theme: {
      items: [
        'Badges use the success, warning, danger and info tones; problems use `--mn-color-danger-strong` and `--mn-color-warning-strong`.',
        'Forced colours: problems and row numbers in CanvasText; the badge text still names the status.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Preview with problems',
          stack: true,
          render: () => (
            <PreviewRows caption="Preview of contacts.csv" rows={ROWS} columns={COLUMNS} />
          ),
        },
        {
          title: 'Cards',
          stack: true,
          render: () => (
            <PreviewRows
              caption="Preview"
              rows={ROWS.slice(0, 3)}
              columns={COLUMNS}
              layout="cards"
              filterable={false}
            />
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Say which field each problem is about.',
        'Show the values that will be saved (after trimming and lookups), not only the raw text.',
      ],
      dont: [
        'Do not hide rows with errors from the preview.',
        'Do not save anything before the person confirms the import.',
      ],
    },
    related: {
      items: [
        {
          id: 'import-wizard',
          reason: 'The full upload → map → preview → import flow that uses it.',
        },
        { id: 'data-table', reason: 'The table it is built on.' },
        { id: 'duplicate-panel', reason: 'Reviewing duplicates of saved records.' },
      ],
    },
    dependencies: { packages: ['react'], components: ['badge', 'data-table', 'tabs'] },
    testing: {
      unit: [
        'Lists every row with row number, result, values (dash when empty), field-tagged problems and notes.',
        'Filter tabs with counts; arrow keys change the filter.',
        'Hides the filter when every row is ready.',
        'Pages long files; empty state.',
        'Status and count helpers.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
