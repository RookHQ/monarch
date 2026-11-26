import { describe, expect, it, vi } from 'vitest';
import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import {
  ColumnMapping,
  columnMappingIssues,
  suggestColumnMapping,
  toFieldMapping,
  type ColumnMappingField,
} from './index.js';

const FIELDS: ColumnMappingField[] = [
  { id: 'name', label: 'Name', required: true },
  { id: 'email', label: 'Email', required: true, aliases: ['E-mail', 'Mail'] },
  {
    id: 'phone',
    label: 'Phone number',
    description: 'International format, e.g. +44 20 7946 0000',
  },
  { id: 'company', label: 'Company' },
];

const SOURCES = [
  { name: 'Full Name', samples: ['Sam Lee', 'Ana Ruiz'] },
  { name: 'E-mail', samples: ['sam@example.com'] },
  { name: 'Phone', samples: [] },
  { name: 'Notes' },
];

describe('ColumnMapping', () => {
  it('suggests fields, labels each picker by its column and shows example values', async () => {
    render(<ColumnMapping sources={SOURCES} fields={FIELDS} data-testid="root" />);
    expect(screen.getByTestId('root')).toHaveClass('mn-column-mapping');
    const list = screen.getByRole('list', { name: 'Column mapping' });
    expect(within(list).getAllByRole('listitem')).toHaveLength(4);
    const email = screen.getByRole('combobox', { name: 'Field for E-mail' });
    expect(email).toHaveValue('email');
    expect(email).toHaveAccessibleDescription('sam@example.com Matched');
    const name = screen.getByRole('combobox', { name: 'Field for Full Name' });
    expect(name).toHaveValue('name');
    expect(name).toHaveAccessibleDescription('Sam Lee, Ana Ruiz Suggested');
    const phone = screen.getByRole('combobox', { name: 'Field for Phone' });
    expect(phone).toHaveValue('phone');
    expect(phone).toHaveAccessibleDescription(
      'No values Matched International format, e.g. +44 20 7946 0000',
    );
    expect(screen.getByRole('combobox', { name: 'Field for Notes' })).toHaveValue('');
    expect(screen.getByRole('status')).toHaveTextContent('3 of 4 columns mapped');
    expect(screen.queryByRole('alert')).toBeNull();
    await expectNoA11yViolations();
  });

  it('reports missing required fields and duplicates, and resets to suggestions', async () => {
    const user = userEvent.setup();
    const onValueChange = vi.fn();
    render(<ColumnMapping sources={SOURCES} fields={FIELDS} onValueChange={onValueChange} />);
    const email = screen.getByRole('combobox', { name: 'Field for E-mail' });
    await user.selectOptions(email, '');
    expect(onValueChange).toHaveBeenLastCalledWith({
      'Full Name': 'name',
      'E-mail': undefined,
      Phone: 'phone',
    });
    expect(screen.getByRole('alert')).toHaveTextContent('Map a column to: Email.');
    await user.selectOptions(screen.getByRole('combobox', { name: 'Field for Notes' }), 'name');
    const notes = screen.getByRole('combobox', { name: 'Field for Notes' });
    expect(notes).toHaveAttribute('aria-invalid', 'true');
    expect(notes).toHaveAccessibleDescription('No values Name is also chosen for Full Name.');
    await user.click(screen.getByRole('button', { name: 'Use suggestions' }));
    expect(email).toHaveValue('email');
    expect(notes).toHaveValue('');
    expect(screen.queryByRole('button', { name: 'Use suggestions' })).toBeNull();
    await user.click(screen.getByRole('button', { name: 'Don’t import any' }));
    expect(screen.getByRole('status')).toHaveTextContent('0 of 4 columns mapped');
    expect(screen.getByRole('alert')).toHaveTextContent('Map a column to: Name, Email.');
    await expectNoA11yViolations();
  });

  it('is keyboard operable and can hide issues until asked', async () => {
    const user = userEvent.setup();
    render(
      <ColumnMapping
        sources={['Mail']}
        fields={FIELDS}
        defaultValue={{}}
        showIssues={false}
        bulkActions={false}
      />,
    );
    expect(screen.queryByRole('alert')).toBeNull();
    await user.tab();
    const picker = screen.getByRole('combobox', { name: 'Field for Mail' });
    expect(picker).toHaveFocus();
    await user.selectOptions(picker, 'email');
    expect(picker).toHaveValue('email');
    expect(screen.getByText('Matched')).toBeInTheDocument();
  });

  it('suggests by exact name, alias and similarity, one column per field', () => {
    const s = suggestColumnMapping(
      ['email', 'Mail', 'Company name', 'Adress', 'zzz'],
      [...FIELDS, { id: 'address', label: 'Address' }],
    );
    expect(s.email).toEqual({ fieldId: 'email', confidence: 'exact', score: 1 });
    expect(s.Mail).toBeUndefined();
    expect(s['Company name']?.fieldId).toBe('company');
    expect(s['Company name']?.confidence).toBe('similar');
    expect(s.Adress?.fieldId).toBe('address');
    expect(s.zzz).toBeUndefined();
  });

  it('converts to a field mapping and lists issues', () => {
    const value = { A: 'name', B: 'name', C: undefined };
    expect(toFieldMapping(value)).toEqual({ name: 'A' });
    const issues = columnMappingIssues(value, FIELDS);
    expect(issues.missing.map((f) => f.id)).toEqual(['email']);
    expect(issues.duplicates).toEqual({ name: ['A', 'B'] });
  });

  it('renders columns that share a header without duplicate React keys', () => {
    const error = vi.spyOn(console, 'error').mockImplementation(() => {});
    try {
      render(<ColumnMapping sources={['Name', '', '']} fields={FIELDS} />);
      expect(screen.getAllByRole('listitem')).toHaveLength(3);
      expect(screen.getByRole('combobox', { name: 'Field for #2' })).toBeInTheDocument();
      expect(screen.getByRole('combobox', { name: 'Field for #3' })).toBeInTheDocument();
      expect(error).not.toHaveBeenCalled();
    } finally {
      error.mockRestore();
    }
  });

  it('renders on the server', () => {
    const html = renderToString(<ColumnMapping sources={SOURCES} fields={FIELDS} />);
    expect(html).toContain('mn-column-mapping');
    expect(html).toContain('Field for E-mail');
  });
});
