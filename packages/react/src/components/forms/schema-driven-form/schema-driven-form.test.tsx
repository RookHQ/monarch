import { describe, expect, it, vi } from 'vitest';
import { useState } from 'react';
import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import {
  SchemaDrivenForm,
  formatSchemaValue,
  isSchemaFieldVisible,
  schemaDefaultValues,
  validateSchemaForm,
  type SchemaDrivenFormSchema,
  type SchemaDrivenFormValues,
} from './index.js';

const SCHEMA: SchemaDrivenFormSchema = [
  {
    type: 'section',
    id: 'contact',
    title: 'Contact',
    description: 'How we reach you.',
    fields: [
      { type: 'text', name: 'name', label: 'Full name', required: true, maxLength: 40 },
      {
        type: 'text',
        name: 'email',
        label: 'Email',
        inputType: 'email',
        pattern: '[^@\\s]+@[^@\\s]+\\.[^@\\s]+',
        patternMessage: 'Enter an email address.',
      },
    ],
  },
  {
    type: 'section',
    id: 'details',
    title: 'Details',
    fields: [
      { type: 'number', name: 'seats', label: 'Seats', min: 1, max: 50, integer: true },
      {
        type: 'select',
        name: 'plan',
        label: 'Plan',
        options: [
          { value: 'basic', label: 'Basic' },
          { value: 'team', label: 'Team' },
        ],
        required: true,
      },
      {
        type: 'text',
        name: 'team',
        label: 'Team name',
        required: true,
        visibleWhen: { field: 'plan', equals: 'team' },
      },
      { type: 'date', name: 'start', label: 'Start date' },
      { type: 'textarea', name: 'notes', label: 'Notes', description: 'Optional context.' },
      { type: 'switch', name: 'updates', label: 'Product updates', defaultValue: true },
      { type: 'checkbox', name: 'terms', label: 'I accept the terms', required: true },
    ],
  },
];

describe('SchemaDrivenForm helpers', () => {
  it('derives default values', () => {
    expect(schemaDefaultValues(SCHEMA)).toEqual({
      name: '',
      email: '',
      seats: null,
      plan: '',
      team: '',
      start: null,
      notes: '',
      updates: true,
      terms: false,
    });
  });

  it('evaluates data and function conditions', () => {
    const v: SchemaDrivenFormValues = { plan: 'team', seats: 3, notes: '' };
    expect(isSchemaFieldVisible(undefined, v)).toBe(true);
    expect(isSchemaFieldVisible({ field: 'plan', equals: 'team' }, v)).toBe(true);
    expect(isSchemaFieldVisible({ field: 'plan', notEquals: 'team' }, v)).toBe(false);
    expect(isSchemaFieldVisible({ field: 'plan', oneOf: ['basic'] }, v)).toBe(false);
    expect(isSchemaFieldVisible({ field: 'notes', filled: false }, v)).toBe(true);
    expect(isSchemaFieldVisible({ field: 'seats', filled: true }, v)).toBe(true);
    expect(isSchemaFieldVisible((x) => x.seats === 3, v)).toBe(true);
  });

  it('validates only visible fields, with every built-in rule', () => {
    const values = { ...schemaDefaultValues(SCHEMA), email: 'nope', seats: 2.5 };
    expect(validateSchemaForm(SCHEMA, values)).toEqual({
      name: 'This field is required.',
      email: 'Enter an email address.',
      seats: 'Enter a whole number.',
      plan: 'This field is required.',
      terms: 'Turn this on to continue.',
    });
    const more = validateSchemaForm(SCHEMA, {
      ...values,
      name: 'x'.repeat(41),
      email: 'a@b.co',
      seats: 99,
      plan: 'team',
      terms: true,
    });
    expect(more).toEqual({
      name: 'Use 40 characters or fewer.',
      seats: 'Enter 50 or less.',
      team: 'This field is required.',
    });
    expect(validateSchemaForm(SCHEMA, { ...values, seats: Number.NaN }).seats).toBe(
      'Enter a number.',
    );
  });

  it('formats values for display', () => {
    const fields = SCHEMA.flatMap((s) => (s.type === 'section' ? s.fields : []));
    const f = (name: string) => fields.find((x) => x.name === name)!;
    expect(formatSchemaValue(f('plan'), 'team')).toBe('Team');
    expect(formatSchemaValue(f('terms'), true)).toBe('Yes');
    expect(formatSchemaValue(f('updates'), false)).toBe('No');
    expect(formatSchemaValue(f('notes'), '')).toBe('—');
    expect(formatSchemaValue(f('seats'), 1200, { locale: 'en-US' })).toBe('1,200');
    expect(formatSchemaValue(f('start'), '2026-03-04', { locale: 'en-GB' })).toBe('4 Mar 2026');
  });
});

describe('SchemaDrivenForm', () => {
  it('renders labelled controls in fieldset sections', async () => {
    render(<SchemaDrivenForm schema={SCHEMA} aria-label="Sign up" locale="en-GB" />);
    const contact = screen.getByRole('group', { name: 'Contact' });
    expect(contact).toHaveAccessibleDescription('How we reach you.');
    expect(within(contact).getByRole('textbox', { name: /Full name/ })).toBeRequired();
    expect(screen.getByRole('textbox', { name: 'Email' })).toHaveAttribute('type', 'email');
    expect(screen.getByRole('textbox', { name: 'Seats' })).toHaveAttribute('inputmode', 'numeric');
    expect(screen.getByRole('combobox', { name: /Plan/ })).toBeInTheDocument();
    expect(screen.getByRole('textbox', { name: 'Notes' })).toHaveAccessibleDescription(
      'Optional context.',
    );
    expect(screen.getByRole('switch', { name: 'Product updates' })).toBeChecked();
    expect(screen.getByRole('checkbox', { name: 'I accept the terms' })).not.toBeChecked();
    expect(screen.getByRole('textbox', { name: 'Start date' })).toBeInTheDocument();
    // Conditional field hidden until the plan is Team.
    expect(screen.queryByRole('textbox', { name: /Team name/ })).not.toBeInTheDocument();
    await expectNoA11yViolations();
  });

  it('shows a field error on blur, and conditional fields when their condition holds', async () => {
    const user = userEvent.setup();
    render(<SchemaDrivenForm schema={SCHEMA} locale="en-GB" />);
    const name = screen.getByRole('textbox', { name: /Full name/ });
    await user.click(name);
    await user.tab();
    expect(name).toHaveAttribute('aria-invalid', 'true');
    expect(name).toHaveAccessibleDescription('This field is required.');
    await user.type(name, 'Ada');
    expect(name).not.toHaveAttribute('aria-invalid');
    await user.selectOptions(screen.getByRole('combobox', { name: /Plan/ }), 'team');
    expect(screen.getByRole('textbox', { name: /Team name/ })).toBeInTheDocument();
    await expectNoA11yViolations();
  });

  it('validates on submit, focuses the first invalid field, then submits visible values', async () => {
    const user = userEvent.setup();
    const onSubmit = vi.fn();
    render(
      <SchemaDrivenForm schema={SCHEMA} onSubmit={onSubmit} locale="en-GB" validateOn="submit" />,
    );
    await user.click(screen.getByRole('button', { name: 'Save' }));
    expect(onSubmit).not.toHaveBeenCalled();
    expect(screen.getByRole('textbox', { name: /Full name/ })).toHaveFocus();
    expect(screen.getByRole('checkbox')).toHaveAccessibleDescription('Turn this on to continue.');

    await user.type(screen.getByRole('textbox', { name: /Full name/ }), 'Ada Lovelace');
    await user.type(screen.getByRole('textbox', { name: 'Seats' }), '12');
    await user.selectOptions(screen.getByRole('combobox', { name: /Plan/ }), 'basic');
    await user.click(screen.getByRole('checkbox'));
    await user.click(screen.getByRole('switch'));
    await user.click(screen.getByRole('button', { name: 'Save' }));
    expect(onSubmit).toHaveBeenCalledWith({
      name: 'Ada Lovelace',
      email: '',
      seats: 12,
      plan: 'basic',
      start: null,
      notes: '',
      updates: false,
      terms: true,
    });
    // `team` is hidden, so it is not submitted.
    expect(onSubmit.mock.calls[0]?.[0]).not.toHaveProperty('team');
  });

  it('keeps partial number text and reports text that is not a number', async () => {
    const user = userEvent.setup();
    const onValuesChange = vi.fn();
    render(
      <SchemaDrivenForm
        schema={[{ type: 'number', name: 'rate', label: 'Rate' }]}
        onValuesChange={onValuesChange}
      />,
    );
    const rate = screen.getByRole('textbox', { name: 'Rate' });
    await user.type(rate, '-1.');
    expect(rate).toHaveValue('-1.');
    expect(onValuesChange).toHaveBeenLastCalledWith({ rate: -1 }, 'rate');
    await user.clear(rate);
    await user.type(rate, 'abc');
    await user.tab();
    expect(rate).toHaveAccessibleDescription('Enter a number.');
  });

  it('follows controlled values', async () => {
    const user = userEvent.setup();
    function Demo() {
      const [values, setValues] = useState<SchemaDrivenFormValues>({ name: 'Ada' });
      return (
        <>
          <SchemaDrivenForm
            schema={[{ type: 'text', name: 'name', label: 'Name' }]}
            values={values}
            onValuesChange={(v) => setValues({ name: String(v.name).toUpperCase() })}
          />
          <output>{String(values.name)}</output>
        </>
      );
    }
    render(<Demo />);
    const input = screen.getByRole('textbox', { name: 'Name' });
    expect(input).toHaveValue('Ada');
    await user.type(input, 'x');
    expect(input).toHaveValue('ADAX');
  });

  it('shows product errors and a rejected submit', async () => {
    const user = userEvent.setup();
    render(
      <SchemaDrivenForm
        schema={[{ type: 'text', name: 'slug', label: 'Address' }]}
        errors={{ slug: 'That address is taken.' }}
        submitLabel="Create"
        secondaryActions={<button type="button">Cancel</button>}
        onSubmit={() => Promise.reject(new Error('The server is busy.'))}
      />,
    );
    expect(screen.getByRole('textbox', { name: 'Address' })).toHaveAccessibleDescription(
      'That address is taken.',
    );
    await user.click(screen.getByRole('button', { name: 'Create' }));
    expect(await screen.findByRole('alert')).toHaveTextContent('The server is busy.');
    expect(screen.getByRole('button', { name: 'Cancel' })).toBeInTheDocument();
  });

  it('is busy while an async submit runs, and can be disabled', async () => {
    const user = userEvent.setup();
    let resolve!: () => void;
    const { rerender } = render(
      <SchemaDrivenForm
        schema={[{ type: 'text', name: 'a', label: 'A' }]}
        onSubmit={() => new Promise<void>((r) => (resolve = r))}
      />,
    );
    await user.click(screen.getByRole('button', { name: 'Save' }));
    expect(screen.getByRole('button', { name: 'Save' })).toHaveAttribute('aria-busy', 'true');
    expect(screen.getByRole('textbox')).toBeDisabled();
    resolve();
    await waitFor(() => expect(screen.getByRole('textbox')).not.toBeDisabled());
    rerender(
      <SchemaDrivenForm
        schema={[{ type: 'text', name: 'a', label: 'A' }]}
        disabled
        submitLabel={null}
      />,
    );
    expect(screen.getByRole('textbox')).toBeDisabled();
    expect(screen.queryByRole('button')).not.toBeInTheDocument();
  });

  it('shows values as a description list when read-only', async () => {
    render(
      <SchemaDrivenForm
        schema={SCHEMA}
        readOnly
        locale="en-GB"
        defaultValues={{ name: 'Ada', plan: 'team', team: 'Analytics', terms: true }}
      />,
    );
    const terms = screen.getAllByRole('term').map((t) => t.textContent);
    expect(terms).toContain('Team name');
    expect(screen.getByText('Analytics')).toBeInTheDocument();
    expect(screen.queryByRole('textbox')).not.toBeInTheDocument();
    await expectNoA11yViolations();
  });

  it('translates messages', async () => {
    const user = userEvent.setup();
    render(
      <SchemaDrivenForm
        schema={[{ type: 'text', name: 'a', label: 'A', required: true }]}
        messages={{ required: 'Pflichtfeld.', submit: 'Speichern' }}
      />,
    );
    await user.click(screen.getByRole('button', { name: 'Speichern' }));
    expect(screen.getByRole('textbox')).toHaveAccessibleDescription('Pflichtfeld.');
  });

  it('renders on the server', () => {
    const html = renderToString(<SchemaDrivenForm schema={SCHEMA} locale="en-GB" />);
    expect(html).toContain('mn-schema-driven-form');
    expect(html).toContain('<fieldset');
  });
});
