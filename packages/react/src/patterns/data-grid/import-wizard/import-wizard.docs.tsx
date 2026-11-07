import { ImportWizard, type ImportField, type ImportRowCheck } from '@rookcom/monarch-react/data';
import type { ComponentDocs } from '../../../internal/component-docs.js';

const FIELDS: ImportField[] = [
  { id: 'name', label: 'Name', required: true, aliases: ['Full name'] },
  { id: 'email', label: 'Email', aliases: ['E-mail', 'Email address'] },
  { id: 'company', label: 'Company', aliases: ['Organisation', 'Business'] },
  { id: 'country', label: 'Country', description: 'Two-letter code, e.g. DE.' },
];

const EXISTING = new Set(['sam@example.com']);

function checkRows(records: { values: Record<string, string> }[]): ImportRowCheck[] {
  return records.map(({ values }) => {
    if (values.email && !/^[^@\s]+@[^@\s]+$/.test(values.email))
      return { errors: [{ field: 'Email', message: 'Not an email address' }] };
    if (values.email && EXISTING.has(values.email))
      return { status: 'duplicate', note: 'Matches an existing contact' };
    if (values.country && values.country.length !== 2)
      return { warnings: [{ field: 'Country', message: 'Not a two-letter code; left empty' }] };
    return {};
  });
}

const TEMPLATE =
  'data:text/csv;charset=utf-8,' +
  encodeURIComponent(
    'Full name,E-mail,Organisation,Country\nAna Ruiz,ana@example.com,Example Ltd,ES\n',
  );

function Demo() {
  return (
    <ImportWizard
      fields={FIELDS}
      maxSize={2 * 1024 * 1024}
      maxRows={5000}
      templateHref={TEMPLATE}
      validate={checkRows}
      duplicateRules={[
        { value: 'skip', label: 'Skip them', description: 'Existing records stay as they are.' },
        {
          value: 'update',
          label: 'Fill empty fields',
          description: 'Only blank fields are filled.',
        },
        {
          value: 'overwrite',
          label: 'Overwrite',
          description: 'Values in the file replace saved values.',
        },
      ]}
      onImport={(records) =>
        new Promise((resolve) =>
          setTimeout(
            () => resolve({ created: records.length, updated: 0, skipped: 0, failed: 0 }),
            900,
          ),
        )
      }
    />
  );
}

export default {
  id: 'import-wizard',
  title: 'ImportWizard',
  summary: 'Upload a CSV, map its columns, preview every row, import, and see what happened.',
  sections: {
    purpose: {
      summary:
        'One import flow for every product: choose a file, match its columns to fields, check every row before anything is saved, choose what to do with duplicates, import and read a report of what was created, updated, skipped and failed. Needed by: Atlas (domains), Dots (clients), Intellect (portfolio).',
      useWhen: [
        'People bring records from a spreadsheet or another system as a CSV file.',
        'The product can validate rows before saving (a dry run) and report row-level results.',
      ],
      avoidWhen: [
        'Uploading files to keep as attachments: use FileUpload.',
        'Editing many existing records at once: use DataTable with BulkActionBar.',
      ],
    },
    designPrinciples: {
      items: [
        'Nothing is saved before the person sees every row and presses Import.',
        'No row is dropped silently: errors are listed, counted, and can be downloaded as a CSV.',
        'Parsing and auto-matching are built in; validation and saving are callbacks, so the product (and its server) stays the source of truth.',
        'Each step moves focus to its heading so keyboard and screen reader users always know where they are.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'Renders a `<div>` (ref) with a Stepper and one `<section>` per step. Other div attributes pass through.',
        'Helpers: `parseCsv(text, delimiter?)` (RFC 4180, delimiter detected), `normalizeHeader`, `suggestMapping(fields, headers)` and `toImportRecords(table, mapping, fields)`.',
        '`validate(records, { fileName, mapping })` returns one `ImportRowCheck` per record (`errors`, `warnings`, `status`, `values`, `note`). Required fields are checked first.',
        '`onImport(records, { fileName, mapping, duplicateRule })` receives every record that is not an error and resolves to an `ImportReport`.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        'Migrating from Dots `ImportWizard`: the server `inspect` call is replaced by the built-in parser and `suggestMapping` (put server field names in `aliases`); `validate` calls the `validate` mode and maps `issues` to checks; `onImport` calls `commit` with the import id and returns `{ created, skipped: duplicates, failed: errors, errors: issues }`; `onDuplicate` skip/create → `duplicateRules`; the 2 MB limit → `maxSize`. The Modal stays in Dots: render the wizard inside a Dialog.',
        'Migrating from Atlas `ImportWizard`: `IMPORT_FIELDS` → `fields` (`domain` is `required`); `usePreviewImport` → `validate` (rows to checks, `duplicate_existing` / `duplicate_in_file` → `status: "duplicate"`); `RULES` → `duplicateRules`; `useCommitImport` → `onImport` (resolve once the job finishes); totals → `ImportReport`; the template download → `templateHref`; “View domains” → `resultActions`. URL resumption of a job stays in Atlas.',
        'Migrating from Intellect’s import page: its PreviewRows step is the built-in preview; its parser can be replaced by `parseCsv` or passed as `parseFile`.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'duplicateRules',
          values: ['undefined', 'ImportDuplicateRule[]'],
          description:
            'Adds a radio group on the preview step for rows that match existing records.',
        },
        {
          prop: 'headingLevel',
          values: ['h2', 'h3', 'h4'],
          description: 'Level of each step title; use `h3` inside a Dialog or a page section.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Upload',
          attribute: 'data-step="upload"',
          description: 'FileUpload zone, limits and an optional template link.',
        },
        {
          name: 'Map',
          attribute: 'data-step="map"',
          description: 'One select per field with an example value; required fields are marked.',
        },
        {
          name: 'Preview',
          attribute: 'data-step="preview"',
          description: 'PreviewRows, duplicate rule and the Import button with the row count.',
        },
        {
          name: 'Importing',
          attribute: 'aria-busy',
          description: 'Indeterminate progress bar; Back and Cancel are disabled.',
        },
        {
          name: 'Results',
          attribute: 'data-step="results"',
          description: 'Totals, problem rows and a CSV download.',
        },
        {
          name: 'Error',
          description:
            'A danger Alert (`role="alert"`) for unreadable files, failed checks or a failed import; the step stays so the person can retry.',
        },
      ],
    },
    interaction: {
      items: [
        'Choosing or dropping a file reads it, rejects files with no header, no rows or too many rows, and suggests a mapping.',
        'Check rows refuses to continue until every required field has a column.',
        'Import sends only rows without errors; it is disabled when there are none.',
        'Back returns to the previous step with the choices kept; Import another file starts over.',
      ],
    },
    keyboard: {
      items: [
        {
          keys: 'Tab',
          action:
            'Moves through the file button, mapping selects, preview tabs, radios and step buttons.',
        },
        { keys: 'Enter / Space', action: 'Activates buttons and opens the file chooser.' },
        { keys: 'Arrow keys', action: 'Change the duplicate rule and the preview filter tab.' },
      ],
    },
    accessibility: {
      role: 'Stepper navigation plus one labelled region per step',
      name: 'Each step section is labelled by its heading, which receives focus when the step opens.',
      items: [
        'Busy states are announced through `role="status"` text; errors through `role="alert"`.',
        'Mapping selects are named by their field and described by the field help and an example value.',
        'Duplicate rules are a native radio group with each option described by its explanation.',
      ],
    },
    responsive: {
      items: [
        'Mapping fields flow into one column at 390 px; step buttons share the row and stretch on narrow screens.',
        'The preview switches to cards on narrow screens (PreviewRows).',
        'On touch screens buttons and radio options are at least 44 px tall.',
      ],
    },
    theme: {
      items: [
        'Built from Stepper, FileUpload, Field, PreviewRows, Alert and Progress, so it follows every theme.',
        'The chosen duplicate rule uses `--mn-color-accent-subtle` and `--mn-color-accent-border`; failures use `--mn-color-danger-strong`.',
        'Forced colours: bordered options and totals in CanvasText, the chosen rule outlined in Highlight.',
      ],
    },
    examples: {
      items: [{ title: 'Contacts import', stack: true, render: () => <Demo /> }],
    },
    doDont: {
      do: [
        'Validate on the server in `validate`, and again when saving.',
        'Give fields `aliases` for the column names people really use.',
        'Make `onImport` idempotent so a retry after a failure does not create duplicates.',
      ],
      dont: [
        'Do not save anything in `validate`.',
        'Do not hide rows with errors or skip them without saying so.',
      ],
    },
    related: {
      items: [
        { id: 'preview-rows', reason: 'The preview step on its own.' },
        { id: 'file-upload', reason: 'The upload zone it uses.' },
        { id: 'stepper', reason: 'The step indicator it uses.' },
        { id: 'duplicate-panel', reason: 'Reviewing duplicates after an import.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: [
        'alert',
        'button',
        'file-upload',
        'form-field',
        'preview-rows',
        'progress-bar',
        'stepper',
      ],
    },
    testing: {
      unit: [
        'Full flow: upload, alias auto-matching, required mapping check, preview with required, custom and duplicate rows, duplicate rule, import of non-error rows, report, import another; focus moves to each step heading.',
        'Rejects files with no data rows or too many rows.',
        'A failed import keeps the preview with an error; Back and Cancel.',
        'Import is disabled when every row has errors.',
        'CSV parsing (BOM, quotes, line breaks, tabs), CSV writing and mapping helpers.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
