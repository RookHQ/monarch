import { describe, expect, it, vi } from 'vitest';
import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import {
  ImportWizard,
  parseCsv,
  suggestMapping,
  toImportRecords,
  type ImportField,
  type ImportReport,
} from './index.js';
import { toCsv } from './import-wizard.csv.js';

const FIELDS: ImportField[] = [
  { id: 'name', label: 'Name', required: true },
  { id: 'email', label: 'Email', aliases: ['E-mail address'] },
  { id: 'country', label: 'Country', description: 'Two-letter code.' },
];

const CSV =
  'Full name;E-mail address;Notes\r\n"Ruiz, Ana";ana@example.com;x\r\n;bad;y\r\nSam Lee;sam@example.com;"say ""hi"""\r\n';

const fileOf = (text: string, name = 'contacts.csv') =>
  new File([text], name, { type: 'text/csv' });
const input = () => document.querySelector<HTMLInputElement>('input[type="file"]')!;

describe('ImportWizard', () => {
  it('runs upload → map → preview → import → results', async () => {
    const user = userEvent.setup();
    const onImport = vi.fn(async (): Promise<ImportReport> => ({
      created: 1,
      updated: 0,
      failed: 1,
      errors: [{ row: 3, field: 'Email', message: 'Rejected by the server' }],
    }));
    const onComplete = vi.fn();
    render(
      <ImportWizard
        fields={FIELDS}
        onImport={onImport}
        onComplete={onComplete}
        validate={(records) =>
          records.map((r) =>
            r.values.email && !r.values.email.includes('@')
              ? { errors: [{ field: 'Email', message: 'Not an email address' }] }
              : r.values.name === 'Sam Lee'
                ? { status: 'duplicate' as const }
                : {},
          )
        }
        duplicateRules={[
          { value: 'skip', label: 'Skip them' },
          { value: 'update', label: 'Update them', description: 'Fill empty fields only.' },
        ]}
      />,
    );
    expect(screen.getByRole('heading', { name: 'Choose a file' })).toBeInTheDocument();
    expect(screen.getByRole('navigation', { name: 'Import steps' })).toBeInTheDocument();
    await expectNoA11yViolations();

    await user.upload(input(), fileOf(CSV));
    const mapHeading = await screen.findByRole('heading', { name: 'Match your columns' });
    expect(mapHeading).toHaveFocus();
    expect(screen.getByText(/contacts\.csv · 3 rows/)).toBeInTheDocument();
    // Email matched through its alias; Name is unmatched and required.
    expect(screen.getByRole('combobox', { name: 'Email' })).toHaveValue('E-mail address');
    expect(screen.getByRole('combobox', { name: /^Name/ })).toHaveValue('');
    await user.click(screen.getByRole('button', { name: 'Check rows' }));
    expect(screen.getByRole('alert')).toHaveTextContent('Choose a column for Name.');
    await user.selectOptions(screen.getByRole('combobox', { name: /^Name/ }), 'Full name');
    expect(screen.getByText('Example: Ruiz, Ana')).toBeInTheDocument();
    await expectNoA11yViolations();
    await user.click(screen.getByRole('button', { name: 'Check rows' }));

    const previewHeading = await screen.findByRole('heading', { name: 'Check the rows' });
    expect(previewHeading).toHaveFocus();
    const table = screen.getByRole('table', { name: 'Preview of contacts.csv' });
    const rows = within(table).getAllByRole('row');
    expect(rows).toHaveLength(4);
    expect(rows[2]).toHaveTextContent('Name is required');
    expect(rows[2]).toHaveTextContent('Not an email address');
    expect(rows[3]).toHaveTextContent('Duplicate');
    await user.click(screen.getByRole('radio', { name: 'Update them' }));
    expect(screen.getByRole('radio', { name: 'Update them' })).toHaveAccessibleDescription(
      'Fill empty fields only.',
    );
    await user.click(screen.getByRole('button', { name: 'Import 2 rows' }));

    const resultsHeading = await screen.findByRole('heading', { name: 'Import finished' });
    expect(resultsHeading).toHaveFocus();
    expect(onImport).toHaveBeenCalledWith(
      [
        { row: 1, values: { name: 'Ruiz, Ana', email: 'ana@example.com' } },
        { row: 3, values: { name: 'Sam Lee', email: 'sam@example.com' } },
      ],
      expect.objectContaining({ fileName: 'contacts.csv', duplicateRule: 'update' }),
    );
    expect(onComplete).toHaveBeenCalled();
    expect(screen.getByText('Created').nextSibling).toHaveTextContent('1');
    expect(screen.getByText('Row 3 · Email · Rejected by the server')).toBeInTheDocument();
    await expectNoA11yViolations();
    await user.click(screen.getByRole('button', { name: 'Import another file' }));
    expect(screen.getByRole('heading', { name: 'Choose a file' })).toHaveFocus();
  });

  it('reports unreadable, empty and oversized files', async () => {
    const user = userEvent.setup();
    render(<ImportWizard fields={FIELDS} onImport={vi.fn()} maxRows={1} />);
    await user.upload(input(), fileOf('Name\r\n'));
    expect(await screen.findByRole('alert')).toHaveTextContent('This file has no data rows');
    await user.upload(input(), fileOf('Name\nA\nB\n'));
    expect(await screen.findByRole('alert')).toHaveTextContent('more than 1 rows');
  });

  it('keeps the preview when the import fails, and can go back', async () => {
    const user = userEvent.setup();
    const onImport = vi
      .fn()
      .mockRejectedValueOnce(new Error('Server busy'))
      .mockResolvedValueOnce({ created: 2 });
    const onCancel = vi.fn();
    render(<ImportWizard fields={FIELDS} onImport={onImport} onCancel={onCancel} />);
    await user.upload(input(), fileOf('name,email\nA,a@x.test\nB,b@x.test\n'));
    await screen.findByRole('heading', { name: 'Match your columns' });
    await user.click(screen.getByRole('button', { name: 'Check rows' }));
    await screen.findByRole('heading', { name: 'Check the rows' });
    await user.click(screen.getByRole('button', { name: 'Import 2 rows' }));
    expect(await screen.findByRole('alert')).toHaveTextContent('Server busy');
    expect(screen.getByRole('heading', { name: 'Check the rows' })).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Back' }));
    expect(screen.getByRole('heading', { name: 'Match your columns' })).toHaveFocus();
    await user.click(screen.getByRole('button', { name: 'Cancel' }));
    expect(onCancel).toHaveBeenCalled();
  });

  it('refuses to import when every row has errors', async () => {
    const user = userEvent.setup();
    render(
      <ImportWizard
        fields={FIELDS}
        onImport={vi.fn()}
        validate={(r) => r.map(() => ({ errors: ['Bad'] }))}
      />,
    );
    await user.upload(input(), fileOf('name\nA\n'));
    await screen.findByRole('heading', { name: 'Match your columns' });
    await user.click(screen.getByRole('button', { name: 'Check rows' }));
    await screen.findByRole('heading', { name: 'Check the rows' });
    expect(screen.getByRole('button', { name: 'Import 0 rows' })).toBeDisabled();
    expect(screen.getByText(/No rows can be imported/)).toBeInTheDocument();
  });

  it('parses CSV and maps headers', () => {
    expect(parseCsv('﻿a,b\r\n"x, y","say ""hi"""\n\n"multi\nline",2')).toEqual({
      headers: ['a', 'b'],
      rows: [
        ['x, y', 'say "hi"'],
        ['multi\nline', '2'],
      ],
    });
    expect(parseCsv('a\tb\n1\t2').rows).toEqual([['1', '2']]);
    expect(
      toCsv([
        ['a', 'b,c'],
        [1, 'q"'],
      ]),
    ).toBe('a,"b,c"\r\n1,"q"""');
    const mapping = suggestMapping(FIELDS, ['NAME', 'e-mail address', 'Land']);
    expect(mapping).toEqual({ name: 'NAME', email: 'e-mail address' });
    expect(
      toImportRecords({ headers: ['NAME', 'x'], rows: [[' A ', '1']] }, mapping, FIELDS),
    ).toEqual([{ row: 1, values: { name: 'A' } }]);
  });

  it('renders on the server', () => {
    const html = renderToString(<ImportWizard fields={FIELDS} onImport={vi.fn()} />);
    expect(html).toContain('mn-import-wizard');
    expect(html).toContain('Choose a file');
  });
});
