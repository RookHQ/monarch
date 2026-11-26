import { useState } from 'react';
import { describe, expect, it, vi } from 'vitest';
import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { DataTable, useDataTableColumns, type DataTableColumn } from '../data-table/index.js';
import { ColumnMenu, orderColumns } from './index.js';

const COLUMNS = [
  { id: 'name', label: 'Name', hideable: false },
  { id: 'status', label: 'Status' },
  { id: 'owner', label: 'Owner' },
  { id: 'updated', label: 'Updated' },
];

function Controlled(props: { onReset?: () => void; showAll?: boolean }) {
  const [visibility, setVisibility] = useState<Record<string, boolean>>({ updated: false });
  return (
    <ColumnMenu
      columns={COLUMNS}
      visibility={visibility}
      onVisibilityChange={setVisibility}
      order={['name', 'owner', 'status', 'updated']}
      {...props}
    />
  );
}

describe('ColumnMenu', () => {
  it('opens from the keyboard and toggles columns while staying open', async () => {
    const user = userEvent.setup();
    render(<Controlled />);
    const trigger = screen.getByRole('button', { name: /^Columns ?, 3 of 4 shown$/ });
    trigger.focus();
    await user.keyboard('{Enter}');
    const menu = await screen.findByRole('menu');
    const items = within(menu).getAllByRole('menuitemcheckbox');
    expect(items.map((i) => i.textContent)).toEqual(['Name', 'Owner', 'Status', 'Updated']);
    expect(items[0]).toHaveAttribute('aria-disabled', 'true');
    expect(items[3]).toHaveAttribute('aria-checked', 'false');
    await waitFor(() => expect(items[1]).toHaveFocus());
    await user.keyboard('{ArrowDown}{ArrowDown}{Enter}');
    expect(screen.getByRole('menuitemcheckbox', { name: 'Updated' })).toHaveAttribute(
      'aria-checked',
      'true',
    );
    expect(screen.getByRole('menu')).toBeInTheDocument();
    await user.keyboard('{ArrowUp}[Space]');
    expect(screen.getByRole('menuitemcheckbox', { name: 'Status' })).toHaveAttribute(
      'aria-checked',
      'false',
    );
    await expectNoA11yViolations(menu);
    await user.keyboard('{Escape}');
    await waitFor(() => expect(screen.queryByRole('menu')).toBeNull());
    expect(screen.getByRole('button', { name: /^Columns ?, 3 of 4 shown$/ })).toHaveFocus();
  });

  it('keeps the last visible column', async () => {
    const user = userEvent.setup();
    render(
      <ColumnMenu
        columns={[
          { id: 'a', label: 'A' },
          { id: 'b', label: 'B' },
        ]}
        visibility={{ b: false }}
        onVisibilityChange={() => {}}
      />,
    );
    await user.click(screen.getByRole('button', { name: /Columns/ }));
    expect(await screen.findByRole('menuitemcheckbox', { name: 'A' })).toHaveAttribute(
      'aria-disabled',
      'true',
    );
    expect(screen.getByRole('menuitemcheckbox', { name: 'B' })).not.toHaveAttribute(
      'aria-disabled',
    );
  });

  it('shows all and resets', async () => {
    const user = userEvent.setup();
    const onReset = vi.fn();
    render(<Controlled onReset={onReset} showAll />);
    await user.click(screen.getByRole('button', { name: /Columns/ }));
    await user.click(await screen.findByRole('menuitem', { name: 'Show all columns' }));
    expect(screen.getByRole('menuitemcheckbox', { name: 'Updated' })).toHaveAttribute(
      'aria-checked',
      'true',
    );
    expect(screen.queryByRole('menuitem', { name: 'Show all columns' })).toBeNull();
    await user.click(screen.getByRole('menuitem', { name: 'Reset columns' }));
    expect(onReset).toHaveBeenCalledOnce();
    await waitFor(() => expect(screen.queryByRole('menu')).toBeNull());
  });

  it('drives a DataTable through useDataTableColumns', async () => {
    interface Row {
      id: string;
      name: string;
      status: string;
    }
    const columns: DataTableColumn<Row>[] = [
      { id: 'name', header: 'Name', hideable: false },
      { id: 'status', header: 'Status' },
    ];
    function Table() {
      const cols = useDataTableColumns(columns);
      return (
        <>
          <ColumnMenu {...cols.chooserProps} />
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
    expect(screen.getByRole('columnheader', { name: /Status/ })).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: /Columns/ }));
    await user.click(await screen.findByRole('menuitemcheckbox', { name: 'Status' }));
    expect(screen.queryByRole('columnheader', { name: /Status/ })).toBeNull();
  });

  it('orders columns', () => {
    expect(orderColumns(COLUMNS, ['owner', 'ghost', 'name']).map((c) => c.id)).toEqual([
      'owner',
      'name',
      'status',
      'updated',
    ]);
    expect(orderColumns(COLUMNS, undefined)).toEqual(COLUMNS);
  });

  it('renders on the server', () => {
    const html = renderToString(<Controlled />);
    expect(html).toContain('Columns');
  });
});
