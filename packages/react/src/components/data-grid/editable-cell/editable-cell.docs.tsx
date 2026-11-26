import { useState } from 'react';
import { EditableCell, type EditableCellValue } from '@rookcom/monarch-react/data';
import type { ComponentDocs } from '../../../internal/component-docs.js';

function TextCell() {
  const [value, setValue] = useState<EditableCellValue>('Quarterly review');
  return <EditableCell label="Title" value={value} onCommit={setValue} />;
}

function NumberCell() {
  const [value, setValue] = useState<EditableCellValue>(12);
  return (
    <EditableCell
      type="number"
      label="Quantity"
      value={value}
      min={1}
      max={99}
      required
      onCommit={setValue}
    />
  );
}

function SelectCell() {
  const [value, setValue] = useState<EditableCellValue>('open');
  return (
    <EditableCell
      type="select"
      label="Status"
      value={value}
      options={[
        { value: 'open', label: 'Open' },
        { value: 'review', label: 'In review' },
        { value: 'done', label: 'Done' },
      ]}
      onCommit={setValue}
    />
  );
}

function DateCell() {
  const [value, setValue] = useState<EditableCellValue>('2026-03-31');
  return <EditableCell type="date" label="Due" value={value} onCommit={setValue} />;
}

function AsyncCell() {
  const [value, setValue] = useState<EditableCellValue>('Sam Lee');
  return (
    <EditableCell
      label="Owner"
      value={value}
      validate={(v) =>
        typeof v === 'string' && v.length < 2 ? 'Use at least 2 characters.' : null
      }
      onCommit={(next) =>
        new Promise<void>((resolve) =>
          setTimeout(() => {
            setValue(next);
            resolve();
          }, 600),
        )
      }
    />
  );
}

export default {
  id: 'editable-cell',
  title: 'EditableCell',
  summary: 'A table cell that edits in place: text, number, select or date, with validation.',
  sections: {
    purpose: {
      summary:
        'Lets people fix one value without opening a form: Enter or F2 opens an editor in the cell, Enter saves, Escape cancels. It parses numbers and dates, checks bounds, required values and your own rules, and keeps the editor open with the message when a save fails. DataGrid uses it for editable columns. Needed by: DataGrid; no app copy recorded yet.',
      useWhen: [
        'A table or list cell holds one value people often correct.',
        'Inside DataTable `cell` renderers, or DataGrid editable columns.',
      ],
      avoidWhen: [
        'Several related fields change together: open a form or a Sheet.',
        'Free text in a page heading: use InlineEditText.',
      ],
    },
    designPrinciples: {
      items: [
        'View and editor share padding and border, so the text does not move.',
        'Errors float under the editor so the row height stays put.',
        'The product owns persistence: `onCommit` may be async and may reject.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        '`parseEditableCellDraft(type, draft)` is exported: how drafts become values.',
        'Values: strings for text and select, numbers for number, `YYYY-MM-DD` for date, `null` for empty.',
        'The ref goes to the root `<div>`.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        'With `trigger="none"` a parent owns focus and drives `editing`; `onEditingChange(false, reason)` reports `commit`, `cancel` or `blur`.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'type',
          values: ['text', 'number', 'select', 'date'],
          description: 'The editor: text input, decimal input, native select or native date input.',
        },
        {
          prop: 'trigger',
          values: ['button', 'none'],
          description: '`none` when a grid owns focus and opens the editor.',
        },
      ],
    },
    states: {
      items: [
        { name: 'Viewing', attribute: 'data-state="viewing"', description: 'Shows the value.' },
        {
          name: 'Editing',
          attribute: 'data-state="editing"',
          description: 'The editor has focus.',
        },
        {
          name: 'Saving',
          attribute: 'data-saving',
          description: 'A spinner; the editor is read-only.',
        },
        {
          name: 'Invalid',
          attribute: 'data-invalid',
          description: 'A message under the editor; `aria-invalid` on the input.',
        },
        { name: 'Empty', attribute: 'data-empty', description: 'Shows the placeholder.' },
        { name: 'Disabled', attribute: 'data-disabled', description: 'Not editable.' },
      ],
    },
    interaction: {
      items: [
        'Click, Enter or F2 opens the editor with the value selected.',
        'Enter saves; leaving the field saves too (`saveOnBlur`).',
        'A value that fails parsing, bounds, `required` or `validate` keeps the editor open with the message.',
        'A rejected `onCommit` keeps the editor open with the error and the draft.',
      ],
    },
    keyboard: {
      items: [
        { keys: 'Enter / F2', action: 'Opens the editor.' },
        { keys: 'Enter', action: 'Saves.' },
        { keys: 'Escape', action: 'Cancels and returns focus to the cell.' },
        { keys: 'Tab', action: 'Saves (on blur) and moves on.' },
      ],
    },
    accessibility: {
      role: 'button (view), then textbox, combobox or date input',
      name: 'View: the value plus “, Edit {label}”. Editor: `label`.',
      items: [
        'Errors are `role="alert"` and linked with `aria-describedby`.',
        'Saving sets `aria-busy` and shows a labelled spinner.',
        'Required editors set `aria-required`.',
      ],
    },
    responsive: {
      items: [
        'Fills the cell width; text truncates with an ellipsis in view mode.',
        'On touch screens view and editor are at least 44 px tall; native select and date pickers open the platform UI.',
      ],
    },
    theme: {
      items: [
        'Uses input, focus-ring and danger tokens, so it follows every theme.',
        'Forced colours: ButtonText borders and a Highlight focus ring.',
      ],
    },
    examples: {
      items: [
        { title: 'Text', render: () => <TextCell /> },
        { title: 'Number with bounds', render: () => <NumberCell /> },
        { title: 'Select', render: () => <SelectCell /> },
        { title: 'Date', render: () => <DateCell /> },
        { title: 'Async save and validation', render: () => <AsyncCell /> },
      ],
    },
    doDont: {
      do: [
        'Give each cell a specific `label` ("Status for Acme"), not just the column name.',
        'Reject the promise from `onCommit` when the server refuses, so the person keeps their draft.',
      ],
      dont: [
        'Do not open a confirmation for every edit; Escape already cancels.',
        'Do not use it for long text: use InlineEditText with `multiline`.',
      ],
    },
    related: {
      items: [
        { id: 'data-grid', reason: 'Editable columns use it.' },
        { id: 'inline-edit-text', reason: 'In-place text editing outside tables.' },
        { id: 'data-table', reason: 'Render it from a column `cell`.' },
      ],
    },
    dependencies: { packages: ['react'], components: ['spinner'] },
    testing: {
      unit: [
        'Enter opens, Enter saves and returns focus; F2 opens, Escape cancels.',
        'Number parsing, min/max and required messages keep the editor open.',
        'Custom validation and a rejected save keep the draft with the message.',
        'Select and date editors; save on blur.',
        'trigger="none" with a typed initial draft; disabled; draft parsing.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
