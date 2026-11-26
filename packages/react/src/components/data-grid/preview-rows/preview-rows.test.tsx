import { describe, expect, it, vi } from 'vitest';
import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { PreviewRows, countPreviewRows, previewRowStatus, type PreviewRow } from './index.js';

const COLUMNS = [
  { id: 'name', label: 'Name' },
  { id: 'email', label: 'Email' },
];

const ROWS: PreviewRow[] = [
  { row: 1, values: { name: 'Ana Ruiz', email: 'ana@example.com' } },
  {
    row: 2,
    values: { name: '', email: 'x' },
    errors: [{ field: 'Email', message: 'Not an email address' }, 'Name is required'],
  },
  {
    row: 3,
    values: { name: 'Sam Lee', email: 'sam@example.com' },
    warnings: ['Country not recognised'],
  },
  {
    row: 4,
    values: { name: 'Kim Park', email: 'kim@example.com' },
    status: 'duplicate',
    note: <a href="#r9">Existing record</a>,
  },
];

describe('PreviewRows', () => {
  it('lists every row with its result and problems', async () => {
    render(
      <PreviewRows
        caption="Preview of contacts.csv"
        rows={ROWS}
        columns={COLUMNS}
        layout="table"
      />,
    );
    const table = screen.getByRole('table', { name: 'Preview of contacts.csv' });
    expect(
      within(table)
        .getAllByRole('columnheader')
        .map((h) => h.textContent),
    ).toEqual(['Row', 'Result', 'Name', 'Email', 'Issues']);
    const row2 = within(table).getAllByRole('row')[2] as HTMLElement;
    expect(row2).toHaveTextContent('Error');
    expect(row2).toHaveTextContent('Error: Email: Not an email address');
    expect(row2).toHaveTextContent('Name is required');
    expect(row2).toHaveTextContent('—');
    expect(screen.getByRole('link', { name: 'Existing record' })).toBeInTheDocument();
    await expectNoA11yViolations();
  });

  it('filters by result with counted tabs', async () => {
    const user = userEvent.setup();
    const onFilterChange = vi.fn();
    render(
      <PreviewRows
        caption="Preview"
        rows={ROWS}
        columns={COLUMNS}
        layout="table"
        onFilterChange={onFilterChange}
      />,
    );
    const tabs = screen.getAllByRole('tab');
    expect(tabs.map((t) => t.textContent)).toEqual([
      'All4',
      'Error1',
      'Warning1',
      'Duplicate1',
      'Ready1',
    ]);
    tabs[0]!.focus();
    await user.keyboard('{ArrowRight}');
    expect(onFilterChange).toHaveBeenLastCalledWith('error');
    const table = screen.getByRole('table');
    expect(within(table).getAllByRole('row')).toHaveLength(2);
    expect(within(table).getByText('Name is required')).toBeInTheDocument();
    await expectNoA11yViolations();
  });

  it('hides the filter when every row is ready', () => {
    render(<PreviewRows caption="P" rows={[ROWS[0]!]} columns={COLUMNS} />);
    expect(screen.queryByRole('tablist')).toBeNull();
  });

  it('pages long files and shows an empty state', () => {
    const many: PreviewRow[] = Array.from({ length: 30 }, (_, i) => ({
      row: i + 1,
      values: { name: `N${i}` },
    }));
    const { rerender } = render(
      <PreviewRows caption="P" rows={many} columns={COLUMNS} pageSize={10} layout="table" />,
    );
    expect(within(screen.getByRole('table')).getAllByRole('row')).toHaveLength(11);
    expect(screen.getByRole('navigation')).toBeInTheDocument();
    rerender(<PreviewRows caption="P" rows={[]} columns={COLUMNS} />);
    expect(screen.getByText('No rows to preview')).toBeInTheDocument();
  });

  it('works out statuses and counts', () => {
    expect(ROWS.map(previewRowStatus)).toEqual(['ready', 'error', 'warning', 'duplicate']);
    expect(countPreviewRows(ROWS)).toEqual({ ready: 1, error: 1, warning: 1, duplicate: 1 });
  });

  it('renders on the server', () => {
    const html = renderToString(
      <PreviewRows caption="P" rows={ROWS} columns={COLUMNS} layout="table" />,
    );
    expect(html).toContain('mn-preview-rows');
    expect(html).toContain('Not an email address');
  });
});
