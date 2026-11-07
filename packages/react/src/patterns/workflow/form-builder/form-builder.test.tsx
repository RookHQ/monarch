import { describe, expect, it, vi } from 'vitest';
import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { useState } from 'react';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import type { FormFieldDefinition } from '../../../components/workflow/field-renderer/index.js';
import {
  FormBuilder,
  formFieldToConditionField,
  validateFormDefinition,
  type FormBuilderProps,
} from './index.js';

const START: FormFieldDefinition[] = [
  {
    id: 'where',
    type: 'radio',
    label: 'Where will you join from?',
    required: true,
    options: [
      { value: 'remote', label: 'Remote' },
      { value: 'office', label: 'Office' },
    ],
  },
  {
    id: 'desk',
    type: 'short-text',
    label: 'Desk number',
    conditions: [{ id: 'c1', field: 'where', operator: 'eq', value: 'office' }],
  },
];

function Harness({
  initial = START,
  onFields,
  ...props
}: Partial<FormBuilderProps> & {
  initial?: FormFieldDefinition[];
  onFields?: (f: FormFieldDefinition[]) => void;
}) {
  const [fields, setFields] = useState(initial);
  let n = 0;
  return (
    <FormBuilder
      createId={(kind) => `${kind}-${++n}-${fields.length}`}
      {...props}
      value={fields}
      onChange={(f) => {
        setFields(f);
        onFields?.(f);
      }}
    />
  );
}

describe('FormBuilder', () => {
  it('shows the palette, the field list and the selected field settings, without axe violations', async () => {
    render(<Harness />);
    expect(screen.getByRole('tab', { name: 'Build', selected: true })).toBeInTheDocument();
    expect(screen.getByRole('region', { name: 'Add a field' })).toBeInTheDocument();
    const list = screen.getByRole('list', { name: 'Form fields' });
    expect(within(list).getAllByRole('listitem')).toHaveLength(2);
    expect(
      screen.getByRole('button', { name: 'Edit Where will you join from?', pressed: true }),
    ).toBeInTheDocument();
    const settings = screen.getByRole('region', { name: 'Single choice settings' });
    expect(within(settings).getByRole('textbox', { name: /Question/ })).toHaveValue(
      'Where will you join from?',
    );
    expect(within(settings).getByRole('textbox', { name: 'Option 2' })).toHaveValue('Office');
    await expectNoA11yViolations();
  });

  it('adds a field after the selected one and focuses its question', async () => {
    const user = userEvent.setup();
    const onFields = vi.fn();
    render(<Harness onFields={onFields} />);
    await user.click(screen.getByRole('button', { name: 'Dropdown' }));
    const next = onFields.mock.lastCall?.[0] as FormFieldDefinition[];
    expect(next.map((f) => f.type)).toEqual(['radio', 'select', 'short-text']);
    expect(next[1]!.options).toHaveLength(2);
    const question = screen.getByRole('textbox', { name: /Question/ });
    await waitFor(() => expect(question).toHaveFocus());
    await user.type(question, 'Team');
    expect((onFields.mock.lastCall?.[0] as FormFieldDefinition[])[1]!.label).toBe('Team');
  });

  it('edits settings: required, options and limits', async () => {
    const user = userEvent.setup();
    const onFields = vi.fn();
    render(<Harness onFields={onFields} />);
    await user.click(screen.getByRole('button', { name: 'Add option' }));
    expect((onFields.mock.lastCall?.[0] as FormFieldDefinition[])[0]!.options).toHaveLength(3);
    await user.click(screen.getByRole('button', { name: 'Remove option Remote' }));
    expect(
      (onFields.mock.lastCall?.[0] as FormFieldDefinition[])[0]!.options!.map((o) => o.label),
    ).toEqual(['Office', 'Option 3']);
    await user.click(screen.getByRole('button', { name: 'Edit Desk number' }));
    await user.click(screen.getByRole('checkbox', { name: 'Required' }));
    await user.type(screen.getByRole('spinbutton', { name: 'Maximum characters' }), '8');
    const desk = (onFields.mock.lastCall?.[0] as FormFieldDefinition[])[1]!;
    expect(desk).toMatchObject({ required: true, maxLength: 8 });
  });

  it('only earlier fields can drive conditions', async () => {
    const user = userEvent.setup();
    render(<Harness />);
    // The first field's conditions can only list the later field as unavailable.
    await user.click(screen.getByRole('button', { name: 'Add condition' }));
    const option = screen.getByRole('option', { name: 'Desk number (comes after this field)' });
    expect(option).toBeDisabled();
  });

  it('removes a field with undo, and drops conditions that depended on it', async () => {
    const user = userEvent.setup();
    const onFields = vi.fn();
    render(<Harness onFields={onFields} />);
    await user.click(screen.getByRole('button', { name: 'Remove Where will you join from?' }));
    const after = onFields.mock.lastCall?.[0] as FormFieldDefinition[];
    expect(after).toHaveLength(1);
    expect(after[0]!.conditions).toBeUndefined();
    expect(screen.getByRole('status')).toHaveTextContent('Removed Where will you join from?.');
    await user.click(screen.getByRole('button', { name: 'Undo' }));
    expect(onFields.mock.lastCall?.[0]).toEqual(START);
  });

  it('duplicates and reorders fields', async () => {
    const user = userEvent.setup();
    const onFields = vi.fn();
    render(<Harness onFields={onFields} />);
    await user.click(screen.getByRole('button', { name: 'Duplicate Desk number' }));
    const dup = onFields.mock.lastCall?.[0] as FormFieldDefinition[];
    expect(dup.map((f) => f.label)).toEqual([
      'Where will you join from?',
      'Desk number',
      'Desk number (copy)',
    ]);
    const handle = screen.getByRole('button', { name: 'Reorder Desk number (copy)' });
    handle.focus();
    await user.keyboard(' ');
    await user.keyboard('{ArrowUp}');
    await user.keyboard(' ');
    expect((onFields.mock.lastCall?.[0] as FormFieldDefinition[]).map((f) => f.label)).toEqual([
      'Where will you join from?',
      'Desk number (copy)',
      'Desk number',
    ]);
  });

  it('shows problems when asked', () => {
    render(
      <FormBuilder
        value={[{ id: 'x', type: 'select', label: '', options: [] }]}
        onChange={() => {}}
        showErrors
        errors={{}}
      />,
    );
    expect(screen.getAllByText('Write the question.').length).toBeGreaterThan(0);
    expect(screen.getByRole('textbox', { name: /Question/ })).toHaveAttribute(
      'aria-invalid',
      'true',
    );
  });

  it('previews the form with show-if logic', async () => {
    const user = userEvent.setup();
    render(<Harness />);
    await user.click(screen.getByRole('tab', { name: 'Preview' }));
    expect(screen.queryByRole('textbox', { name: 'Desk number' })).toBeNull();
    await user.click(screen.getByRole('radio', { name: 'Office' }));
    expect(screen.getByRole('textbox', { name: 'Desk number' })).toBeInTheDocument();
    await expectNoA11yViolations();
  });

  it('read-only hides editing controls; empty state', () => {
    const { rerender } = render(<FormBuilder value={START} onChange={() => {}} readOnly />);
    expect(screen.queryByRole('region', { name: 'Add a field' })).toBeNull();
    expect(screen.queryByRole('button', { name: 'Remove Where will you join from?' })).toBeNull();
    expect(screen.getByRole('textbox', { name: /Question/ })).toBeDisabled();
    rerender(<FormBuilder value={[]} onChange={() => {}} />);
    expect(screen.getByText('This form is empty')).toBeInTheDocument();
  });

  it('helpers map fields to conditions and validate definitions', () => {
    expect(formFieldToConditionField(START[0]!)).toMatchObject({ value: 'where', type: 'select' });
    expect(formFieldToConditionField({ id: 's', type: 'section', label: 'S' })).toBeNull();
    expect(validateFormDefinition(START)).toEqual({});
    expect(validateFormDefinition([START[1]!, START[0]!])).toEqual({ desk: 'condition-order' });
  });

  it('renders on the server', () => {
    const html = renderToString(<FormBuilder value={START} onChange={() => {}} />);
    expect(html).toContain('mn-form-builder');
  });
});
