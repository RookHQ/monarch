import { useState } from 'react';
import { describe, expect, it, vi } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { TableToolbar, type TableToolbarProps } from './index.js';

const COLUMNS = [
  { id: 'name', label: 'Name', hideable: false },
  { id: 'status', label: 'Status' },
];

function Demo(props: Partial<TableToolbarProps> & { initialSelected?: number }) {
  const { initialSelected = 0, ...rest } = props;
  const [query, setQuery] = useState('');
  const [view, setView] = useState('table');
  const [visibility, setVisibility] = useState<Record<string, boolean>>({});
  const [selected, setSelected] = useState(initialSelected);
  return (
    <>
      <TableToolbar
        search={{ value: query, onValueChange: setQuery }}
        filters={<button type="button">Status</button>}
        view={{ value: view, onValueChange: setView, views: ['table', 'board'] }}
        columns={{ columns: COLUMNS, visibility, onVisibilityChange: setVisibility }}
        actions={<button type="button">New record</button>}
        resultCount={1204}
        selectedCount={selected}
        totalCount={1204}
        onClearSelection={() => setSelected(0)}
        bulkActions={[{ id: 'archive', label: 'Archive', onSelect: vi.fn() }]}
        {...rest}
      />
      <button type="button" onClick={() => setSelected(3)}>
        Select three
      </button>
      <output data-testid="query">{query}</output>
      <output data-testid="view">{view}</output>
    </>
  );
}

describe('TableToolbar', () => {
  it('composes search, filters, count, view switcher, column menu and actions', async () => {
    const user = userEvent.setup();
    render(<Demo data-testid="root" />);
    const root = screen.getByRole('group', { name: 'Table controls' });
    expect(root).toBe(screen.getByTestId('root'));
    expect(root).toHaveClass('mn-table-toolbar');
    expect(screen.getByText('1,204 results')).toHaveAttribute('role', 'status');

    await user.tab();
    const search = screen.getByRole('searchbox', { name: 'Search' });
    expect(search).toHaveFocus();
    await user.keyboard('acme');
    expect(screen.getByTestId('query')).toHaveTextContent('acme');
    await user.tab();
    expect(screen.getByRole('button', { name: 'Clear search' })).toHaveFocus();
    await user.tab();
    expect(screen.getByRole('button', { name: 'Status' })).toHaveFocus();
    await user.tab();
    expect(screen.getByRole('radio', { name: 'Table' })).toHaveFocus();
    await user.keyboard('{ArrowRight}');
    expect(screen.getByTestId('view')).toHaveTextContent('board');
    await user.tab();
    expect(screen.getByRole('button', { name: /^Columns ?, 2 of 2 shown$/ })).toHaveFocus();
    await user.tab();
    expect(screen.getByRole('button', { name: 'New record' })).toHaveFocus();
    await expectNoA11yViolations();
  });

  it('swaps search and filters for the bulk selection area while rows are selected', async () => {
    const user = userEvent.setup();
    render(<Demo />);
    expect(screen.queryByRole('toolbar', { name: 'Bulk actions' })).toBeNull();
    await user.click(screen.getByRole('button', { name: 'Select three' }));
    const bulk = screen.getByRole('toolbar', { name: 'Bulk actions' });
    expect(bulk).toHaveTextContent('3 of 1204 selected');
    expect(screen.queryByRole('searchbox')).toBeNull();
    expect(screen.getByRole('group', { name: 'Table controls' })).toHaveAttribute('data-selecting');
    await expectNoA11yViolations();
    await user.click(screen.getByRole('button', { name: 'Clear selection' }));
    await waitFor(() => expect(screen.getByRole('searchbox')).toBeVisible());
    expect(screen.queryByRole('toolbar', { name: 'Bulk actions' })).toBeNull();
  });

  it('renders only what it is given and accepts messages', () => {
    render(
      <TableToolbar
        aria-label="Invoice controls"
        search={{ defaultValue: '' }}
        resultCount={1}
        messages={{ search: 'Find invoices', results: (n) => `${n} invoice` }}
      />,
    );
    expect(screen.getByRole('group', { name: 'Invoice controls' })).toBeInTheDocument();
    expect(screen.getByRole('searchbox', { name: 'Find invoices' })).toBeInTheDocument();
    expect(screen.getByText('1 invoice')).toHaveAttribute('role', 'status');
    expect(screen.queryByRole('radiogroup')).toBeNull();
    expect(screen.queryByRole('button', { name: /Columns/ })).toBeNull();
  });

  it('renders on the server', () => {
    const html = renderToString(<Demo />);
    expect(html).toContain('mn-table-toolbar');
    expect(html).toContain('1,204 results');
  });
});
