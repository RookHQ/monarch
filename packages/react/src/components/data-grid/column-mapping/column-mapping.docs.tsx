import { useState } from 'react';
import {
  ColumnMapping,
  toFieldMapping,
  type ColumnMappingField,
  type ColumnMappingValue,
} from '@rookcom/monarch-react/data';
import type { ComponentDocs } from '../../../internal/component-docs.js';
// The scenario of the primary example, shared with iOS and Android (CROSS-PLATFORM.md section 8).
import fixture from './column-mapping.fixture.json';

const FIELDS: ColumnMappingField[] = fixture.data.fields;

const SOURCES = fixture.data.sources;

function Controlled() {
  const [value, setValue] = useState<ColumnMappingValue | undefined>(undefined);
  return (
    <div style={{ display: 'grid', gap: 12 }}>
      <ColumnMapping sources={SOURCES} fields={FIELDS} onValueChange={setValue} />
      <code>{JSON.stringify(toFieldMapping(value ?? {}))}</code>
    </div>
  );
}

export default {
  id: 'column-mapping',
  title: 'ColumnMapping',
  summary: 'Map imported columns to fields, with example values and suggestions.',
  sections: {
    purpose: {
      summary:
        'Imports fail when a column lands in the wrong field. ColumnMapping lists each source column with example values and a field picker, pre-filled from name, alias and close-match suggestions, and flags required fields nobody maps and fields chosen twice. It is the mapping step of ImportWizard as a component of its own. Needed by: Atlas (`features/imports/components/ImportWizard.tsx` mapping step), Dots (`components/crm/ImportWizard.tsx`, `LeadImportDialog.tsx`).',
      useWhen: [
        'An import or integration needs people to confirm which column is which.',
        'Your own import flow where ImportWizard does not fit.',
      ],
      avoidWhen: [
        'A full CSV import (upload, map, preview, results): use ImportWizard.',
        'Choosing which columns a table shows: use ColumnMenu.',
      ],
    },
    designPrinciples: {
      items: [
        'Suggestions are pre-filled but always visible as “Matched” or “Suggested” badges, never silent.',
        'Each field is suggested once; exact matches win over close ones.',
        'Problems are listed in words, not only in colour.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'Helpers: `suggestColumnMapping(sources, fields, threshold?)`, `mappingFromSuggestions`, `toFieldMapping(value)` (field id → column, the shape of ImportWizard’s `ImportMapping`), `columnMappingIssues(value, fields)` and `normalizeColumnName`.',
        '`ColumnMappingField` has the same shape as ImportWizard’s `ImportField`.',
        'The ref goes to the root `<div>`.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        'The value is keyed by source column (column → field id). Use `toFieldMapping` for field-keyed code such as `toImportRecords`.',
        'Migrating from the app copies (Atlas and Dots import mapping selects): `mapping[field]` state → `value` keyed by column (or flip with `toFieldMapping`); server auto-mapping → `suggestions`, or put server names in `aliases`; the “name required” alert → built in via `required: true`.',
      ],
    },
    variants: {
      items: [
        { prop: 'size', values: ['sm', 'md'], description: 'Picker size. Default `md`.' },
        {
          prop: 'showIssues',
          values: ['true', 'false'],
          description: 'Hide problems until the person tries to continue.',
        },
      ],
    },
    states: {
      items: [
        { name: 'Mapped', attribute: 'data-mapped', description: 'The column has a field.' },
        {
          name: 'Duplicate',
          attribute: 'data-invalid (row)',
          description: 'Its field is also chosen for another column; message under the picker.',
        },
        {
          name: 'Incomplete',
          attribute: 'data-invalid (root)',
          description: 'A required field is unmapped or a field is chosen twice.',
        },
      ],
    },
    interaction: {
      items: [
        'Choosing a field calls `onValueChange` with the whole mapping.',
        '“Use suggestions” restores the suggestions; “Don’t import any” clears every picker.',
      ],
    },
    keyboard: {
      items: [
        { keys: 'Tab', action: 'Moves between the pickers and buttons.' },
        { keys: 'Arrow keys / Alt+ArrowDown', action: 'Native select behaviour in each picker.' },
      ],
    },
    accessibility: {
      role: 'list of column rows with native selects',
      name: 'Each picker: “Field for {column}”; the list: “Column mapping”.',
      items: [
        'Pickers are described by the example values, the suggestion badge, field help and any duplicate message.',
        'The mapped count is a polite status; missing required fields are an alert.',
        'Duplicate pickers set `aria-invalid`.',
      ],
    },
    responsive: {
      items: [
        'Three columns on wide screens; below 40rem each row stacks (name, examples, picker).',
        'Native selects open the platform picker on phones.',
      ],
    },
    theme: {
      items: [
        'Uses border, muted text, Badge and danger tokens.',
        'Forced colours: problem rows and the missing notice get CanvasText borders.',
      ],
    },
    examples: {
      items: [{ title: 'Contacts import', stack: true, render: () => <Controlled /> }],
    },
    doDont: {
      do: [
        'Add `aliases` for the names your exports and partners use.',
        'Pass two or three sample values so people can tell similar columns apart.',
      ],
      dont: [
        'Do not continue the import while `columnMappingIssues` reports problems.',
        'Do not hide the suggestion badges.',
      ],
    },
    related: {
      items: [
        { id: 'import-wizard', reason: 'The full import flow.' },
        { id: 'preview-rows', reason: 'The next step: check rows before importing.' },
        { id: 'form-field', reason: 'NativeSelect, the picker.' },
      ],
    },
    dependencies: { packages: ['react'], components: ['badge', 'button', 'form-field'] },
    testing: {
      unit: [
        'Suggests fields; pickers named per column and described by samples, badges and help.',
        'Missing required and duplicate problems; use suggestions; clear all.',
        'Keyboard operation; hidden issues.',
        'Suggestion matching (exact, alias, similar, typo); field mapping and issues helpers.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
