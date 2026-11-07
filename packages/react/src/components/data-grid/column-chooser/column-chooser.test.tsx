import { useState } from 'react';
import { describe, expect, it, vi } from 'vitest';
import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { DataTable, useDataTableColumns, type DataTableColumn } from '../data-table/index.js';
import { ColumnChooser, type ColumnChooserProps } from './index.js';

const COLUMNS = [
  { id: 'name', label: 'Name', hideable: false },
  { id: 'status', label: 'Status' },
  { id: 'owner', label: 'Owner' },
];

function Controlled(props: Partial<ColumnChooserProps>) {
  const [visibility, setVisibility] = useState<Record<string, boolean>>({ owner: false });
  const [order, setOrder] = useState<string[]>(['name', 'status', 'owner']);
  return (
    <ColumnChooser
      columns={COLUMNS}
      visibility={visibility}
      onVisibilityChange={setVisibility}
      order={order}
      onOrderChange={setOrder}
      {...props}
    />
  );
}

describe('ColumnChooser', () => {
  it('is a named group with a count and toggles columns', async () => {
    const user = userEvent.setup();
    render(<Controlled />);
    const group = screen.getByRole('group', { name: 'Columns' });
    expect(group).toHaveClass('mn-column-chooser');
    expect(group).toHaveAccessibleDescription('2 of 3 shown');
    expect(screen.getByRole('checkbox', { name: 'Name' })).toBeDisabled();
    await user.click(screen.getByRole('checkbox', { name: 'Owner' }));
    expect(screen.getByRole('checkbox', { name: 'Owner' })).toBeChecked();
    expect(group).toHaveAccessibleDescription('3 of 3 shown');
    expect(screen.queryByRole('button', { name: 'Show all' })).toBeNull();
    await expectNoA11yViolations();
  });

  it('reorders by keyboard, keeps focus and announces', async () => {
    const user = userEvent.setup();
    render(<Controlled />);
    const down = screen.getByRole('button', { name: 'Move Name down' });
    down.focus();
    await user.keyboard('{Enter}');
    expect(screen.getByRole('status')).toHaveTextContent('Name moved to position 2 of 3.');
    expect(screen.getByRole('button', { name: 'Move Name down' })).toHaveFocus();
    await user.keyboard('{Enter}');
    // Now last: the down button disables and focus moves to its partner.
    expect(screen.getByRole('button', { name: 'Move Name down' })).toBeDisabled();
    expect(screen.getByRole('button', { name: 'Move Name up' })).toHaveFocus();
    expect(
      within(screen.getByRole('list'))
        .getAllByRole('listitem')
        .map((li) => li.textContent?.split('Always')[0]),
    ).toEqual(['Status', 'Owner', 'Name']);
  });

  it('searches when there are many columns and hides move buttons while filtering', async () => {
    const user = userEvent.setup();
    render(<Controlled searchThreshold={3} />);
    await user.type(screen.getByRole('searchbox', { name: 'Find a column' }), 'sta');
    expect(screen.getAllByRole('checkbox')).toHaveLength(1);
    expect(screen.queryByRole('button', { name: /Move/ })).toBeNull();
    await user.clear(screen.getByRole('searchbox'));
    await user.type(screen.getByRole('searchbox'), 'zzz');
    expect(screen.getByText('No columns match.')).toBeInTheDocument();
  });

  it('keeps the last visible column, shows all and resets', async () => {
    const user = userEvent.setup();
    const onReset = vi.fn();
    render(
      <Controlled
        columns={[
          { id: 'a', label: 'A' },
          { id: 'b', label: 'B' },
        ]}
        visibility={{ b: false }}
        onVisibilityChange={vi.fn()}
        onReset={onReset}
      />,
    );
    expect(screen.getByRole('checkbox', { name: 'A' })).toBeDisabled();
    await user.click(screen.getByRole('button', { name: 'Reset' }));
    expect(onReset).toHaveBeenCalledOnce();
  });

  it('drives a DataTable through useDataTableColumns', async () => {
    interface Row {
      id: string;
      name: string;
      status: string;
    }
    const columns: DataTableColumn<Row>[] = [
      { id: 'name', header: 'Name' },
      { id: 'status', header: 'Status' },
    ];
    function Table() {
      const cols = useDataTableColumns(columns);
      return (
        <>
          <ColumnChooser {...cols.chooserProps} />
          <DataTable
            caption="Records"
            columns={columns}
            rows={[{ id: '1', name: 'Alpha', status: 'Open' }]}
            getRowId={(r) => r.id}
            {...cols.tableProps}
          />
        </>
      );
    }
    const user = userEvent.setup();
    render(<Table />);
    await user.click(screen.getByRole('button', { name: 'Move Status up' }));
    const headers = screen.getAllByRole('columnheader').map((h) => h.textContent);
    expect(headers[0]).toMatch(/^Status/);
    await user.click(screen.getByRole('checkbox', { name: 'Name' }));
    expect(screen.queryByRole('columnheader', { name: /Name/ })).toBeNull();
    await user.click(screen.getByRole('button', { name: 'Reset' }));
    expect(screen.getAllByRole('columnheader')[0]).toHaveTextContent(/^Name/);
  });

  it('renders on the server', () => {
    expect(renderToString(<Controlled />)).toContain('mn-column-chooser');
  });
});
