import { Button } from '@rookcom/monarch-react';
import { MergeDialog, type MergeRecord } from '@rookcom/monarch-react/data';
import type { ComponentDocs } from '../../../internal/component-docs.js';

const RECORDS: MergeRecord[] = [
  {
    id: 'a',
    title: 'Example Ltd',
    description: 'Created 12 Feb 2025 · 3 projects',
    values: {
      name: 'Example Ltd',
      email: 'hello@example.com',
      phone: '',
      website: 'example.com',
      city: 'Leeds',
    },
  },
  {
    id: 'b',
    title: 'Example Limited',
    description: 'Created 4 Jun 2025 · 1 invoice',
    values: {
      name: 'Example Limited',
      email: 'hello@example.com',
      phone: '+44 20 0000 0000',
      website: 'example.com',
      city: 'York',
    },
  },
];

const FIELDS = [
  { id: 'name', label: 'Name' },
  { id: 'email', label: 'Email' },
  { id: 'phone', label: 'Phone' },
  { id: 'website', label: 'Website' },
  { id: 'city', label: 'City' },
];

export default {
  id: 'merge-dialog',
  title: 'MergeDialog',
  summary: 'Merge duplicate records: pick the record to keep and the value to keep per field.',
  sections: {
    purpose: {
      summary:
        'Combines two or more duplicate records into one. People choose which record survives and, for every field where the records differ, which value is kept; matching fields are listed once. The merge itself runs in the product. Needed by: Dots (clients and leads).',
      useWhen: [
        'Two or more records describe the same thing and their data must be combined.',
        'People need to see the differences before an action that cannot be undone from the app.',
      ],
      avoidWhen: [
        'Deleting a duplicate without keeping any of its data: use ConfirmDialog.',
        'Moving one record under another without combining fields: use a picker in a Dialog.',
      ],
    },
    designPrinciples: {
      items: [
        'Sensible defaults: the kept record’s values win, and its blank fields are filled from the others.',
        'Only real choices are asked: identical fields are shown once, read-only.',
        'Destructive by design: danger button, focus on Cancel, outside clicks ignored, and the dialog stays open and busy until the merge settles.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'Renders a Dialog with `role="alertdialog"` (ref to its content) and a form. Controlled with `open` / `onOpenChange`, or opened by `trigger`.',
        '`onMerge(selection)` receives `{ primaryId, mergedIds, fields }` where `fields` maps each field id to the record whose value survives. Return a promise to keep the dialog busy; a rejection shows its message.',
        '`defaultMergeChoice(field, records, primaryId)` is the default rule, exported for server-side previews.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        'Migrating from the app copy (Dots `MergeDialog`): the account search moves to Dots (pick the target with an EntityPicker, then open this dialog with `records={[source, target]}` and `defaultPrimaryId={target.id}`); the server dry-run counts become `consequences` (“3 invoices move across”); `preview.warnings` become `blockers`; the POST to `/merge` goes in `onMerge` (pass `selection.fields` to the server); the toast and redirect stay in Dots after the promise resolves.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'size',
          values: ['md', 'lg', 'xl'],
          description: 'Dialog width. Default `lg`; full screen on phones.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Choosing',
          description:
            'Kept record and value radios; the chosen card is accent-tinted (`data-checked`).',
        },
        {
          name: 'Empty value',
          attribute: 'data-empty',
          description: 'The option reads “Empty” in subtle italic text.',
        },
        {
          name: 'Blocked',
          description: '`blockers` are listed in a warning Alert and the Merge button is disabled.',
        },
        {
          name: 'Merging',
          attribute: 'aria-busy',
          description: 'Merge shows a spinner; Cancel, Escape and the close button do nothing.',
        },
        {
          name: 'Failed',
          description:
            'A danger Alert (`role="alert"`) with the error; choices are kept for a retry.',
        },
      ],
    },
    interaction: {
      items: [
        'Choosing another record to keep updates the default value of every field you have not picked yourself.',
        'Merge calls `onMerge`; the dialog closes when it resolves and returns focus to where it was opened from.',
        'Each opening starts from the defaults.',
      ],
    },
    keyboard: {
      items: [
        {
          keys: 'Tab / Shift+Tab',
          action: 'Moves between the radio groups and the buttons; focus starts on Cancel.',
        },
        {
          keys: 'Arrow keys',
          action: 'Choose within a radio group (record to keep, or a field’s value).',
        },
        { keys: 'Enter', action: 'Submits the merge from the Merge button.' },
        { keys: 'Escape', action: 'Cancels, unless the merge is running.' },
      ],
    },
    accessibility: {
      role: 'alertdialog',
      name: 'Its title (“Merge records”), described by the explanation.',
      items: [
        'Each field is a fieldset whose legend is the field name; each option is named by its value and “from <record>”.',
        'Record options are described by their secondary line.',
        'Failures are announced assertively and linked to the Merge button with `aria-describedby`.',
      ],
    },
    responsive: {
      items: [
        'Full screen below 640 px; options stack into one column at 390 px and long values wrap.',
        'On touch screens every option is at least 44 px tall.',
      ],
    },
    theme: {
      items: [
        'Chosen options use `--mn-color-accent-subtle` and `--mn-color-accent-border`; the Merge button is the danger Button.',
        'Forced colours: CanvasText borders, chosen options outlined in Highlight.',
        'Option transitions stop when reduced motion is requested.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Two clients',
          render: () => (
            <MergeDialog
              trigger={<Button variant="secondary">Merge duplicates</Button>}
              records={RECORDS}
              fields={FIELDS}
              consequences={[
                '1 invoice moves to the kept record.',
                'The other record is archived, not deleted.',
              ]}
              onMerge={() => new Promise((resolve) => setTimeout(resolve, 800))}
            />
          ),
        },
        {
          title: 'Blocked',
          render: () => (
            <MergeDialog
              trigger={<Button variant="secondary">Merge (blocked)</Button>}
              records={RECORDS}
              fields={FIELDS}
              blockers={['Both records have an active subscription. Cancel one first.']}
              onMerge={() => {}}
            />
          ),
        },
      ],
    },
    doDont: {
      do: [
        'List what moves and what happens to the other records in `consequences`.',
        'Validate on the server again when merging; the dialog only collects choices.',
      ],
      dont: [
        'Do not delete the merged records outright without saying so.',
        'Do not ask for fields that are identical.',
      ],
    },
    related: {
      items: [
        { id: 'duplicate-panel', reason: 'Finding the duplicates to merge.' },
        { id: 'confirm-dialog', reason: 'A plain confirmation without choices.' },
        { id: 'dialog', reason: 'The primitive it is built on.' },
      ],
    },
    dependencies: { packages: ['react'], components: ['alert', 'button', 'dialog'] },
    testing: {
      unit: [
        'Alertdialog with focus on Cancel; kept-record radios with descriptions; identical fields shown once.',
        'Empty values on the kept record default to another record’s value; arrow keys change a field’s choice.',
        'Submits `{ primaryId, mergedIds, fields }` and returns focus after closing.',
        'Changing the kept record updates untouched defaults.',
        'Busy while merging (Cancel and Escape blocked), then shows the failure.',
        'Blockers disable Merge.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
