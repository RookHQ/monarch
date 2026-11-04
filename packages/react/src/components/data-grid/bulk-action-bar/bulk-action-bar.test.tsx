import { useRef, useState } from 'react';
import { describe, expect, it, vi } from 'vitest';
import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { DataTable } from '../data-table/index.js';
import { BulkActionBar, type BulkAction } from './index.js';

const rows = [
  { id: 'a', name: 'Alpha' },
  { id: 'b', name: 'Bravo' },
  { id: 'c', name: 'Charlie' },
];

function WithTable({ actions }: { actions: BulkAction[] }) {
  const [ids, setIds] = useState<string[]>([]);
  return (
    <>
      <DataTable
        columns={[{ id: 'name', header: 'Name' }]}
        rows={rows}
        getRowId={(r) => r.id}
        getRowLabel={(r) => r.name}
        caption="Items"
        selectable
        selectedRowIds={ids}
        onSelectedRowIdsChange={setIds}
      />
      <BulkActionBar
        count={ids.length}
        totalCount={rows.length}
        onClear={() => setIds([])}
        actions={actions}
        maxVisibleActions={2}
      />
    </>
  );
}

const barStatus = () =>
  within(document.querySelector<HTMLElement>('.mn-bulk-action-bar')!).getByRole('status');

describe('BulkActionBar', () => {
  it('is hidden until rows are selected in a DataTable, then announces the count', async () => {
    render(<WithTable actions={[{ id: 'archive', label: 'Archive', onSelect: vi.fn() }]} />);
    expect(screen.queryByRole('toolbar')).toBeNull();
    const user = userEvent.setup();
    await user.click(screen.getByRole('checkbox', { name: 'Select Bravo' }));
    expect(screen.getByRole('toolbar', { name: 'Bulk actions' })).toBeInTheDocument();
    expect(
      screen.getByText('1 of 3 selected', { selector: '.mn-bulk-action-bar__count' }),
    ).toBeVisible();
    expect(barStatus()).toHaveTextContent('1 of 3 selected');
    await user.click(screen.getByRole('checkbox', { name: 'Select all rows on this page' }));
    expect(barStatus()).toHaveTextContent('3 of 3 selected');
    await expectNoA11yViolations();
  });

  it('clears the selection, announces it and keeps focus in place', async () => {
    render(<WithTable actions={[]} />);
    const user = userEvent.setup();
    await user.click(screen.getByRole('checkbox', { name: 'Select Alpha' }));
    await user.click(screen.getByRole('button', { name: 'Clear selection' }));
    expect(screen.queryByRole('toolbar')).toBeNull();
    expect(barStatus()).toHaveTextContent('Selection cleared');
    expect(screen.getByRole('checkbox', { name: 'Select Alpha' })).toHaveAttribute(
      'aria-checked',
      'false',
    );
    expect(document.activeElement).toHaveClass('mn-bulk-action-bar');
  });

  it('returns focus to returnFocusRef after clearing', async () => {
    function Demo() {
      const ref = useRef<HTMLButtonElement>(null);
      const [count, setCount] = useState(2);
      return (
        <>
          <button ref={ref} type="button">
            List
          </button>
          <BulkActionBar count={count} onClear={() => setCount(0)} returnFocusRef={ref} />
        </>
      );
    }
    render(<Demo />);
    await userEvent.setup().click(screen.getByRole('button', { name: 'Clear selection' }));
    expect(screen.getByRole('button', { name: 'List' })).toHaveFocus();
  });

  it('runs actions, puts extra actions in an overflow menu, and blocks while a promise runs', async () => {
    let resolve!: () => void;
    const onExport = vi.fn(() => new Promise<void>((r) => (resolve = r)));
    const onTag = vi.fn();
    const onDelete = vi.fn();
    render(
      <BulkActionBar
        count={3}
        onClear={() => {}}
        maxVisibleActions={2}
        actions={[
          { id: 'export', label: 'Export', onSelect: onExport },
          { id: 'tag', label: 'Tag', onSelect: onTag },
          { id: 'delete', label: 'Delete', tone: 'danger', onSelect: onDelete },
        ]}
      />,
    );
    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: 'Export' }));
    expect(onExport).toHaveBeenCalledOnce();
    expect(screen.getByRole('button', { name: 'Export' })).toHaveAttribute('aria-busy', 'true');
    expect(screen.getByRole('button', { name: 'Tag' })).toBeDisabled();
    expect(screen.getByRole('toolbar')).toHaveAttribute('aria-busy', 'true');
    resolve();
    await waitFor(() => expect(screen.getByRole('button', { name: 'Tag' })).toBeEnabled());
    expect(screen.queryByRole('button', { name: 'Delete' })).toBeNull();
    await user.click(screen.getByRole('button', { name: 'More' }));
    await user.click(await screen.findByRole('menuitem', { name: 'Delete' }));
    expect(onDelete).toHaveBeenCalledOnce();
  });

  it('is one tab stop with arrow-key movement (roving tabindex)', async () => {
    render(
      <>
        <BulkActionBar
          count={2}
          onClear={() => {}}
          actions={[
            { id: 'a', label: 'Assign', onSelect: () => {} },
            { id: 'm', label: 'Move', onSelect: () => {} },
          ]}
        />
        <button type="button">After</button>
      </>,
    );
    const user = userEvent.setup();
    await user.tab();
    expect(screen.getByRole('button', { name: 'Assign' })).toHaveFocus();
    await user.keyboard('{ArrowRight}');
    expect(screen.getByRole('button', { name: 'Move' })).toHaveFocus();
    await user.keyboard('{End}');
    expect(screen.getByRole('button', { name: 'Clear selection' })).toHaveFocus();
    await user.keyboard('{ArrowRight}');
    expect(screen.getByRole('button', { name: 'Assign' })).toHaveFocus();
    await user.keyboard('{ArrowLeft}');
    expect(screen.getByRole('button', { name: 'Clear selection' })).toHaveFocus();
    await user.tab();
    expect(screen.getByRole('button', { name: 'After' })).toHaveFocus();
    await user.tab({ shift: true });
    expect(screen.getByRole('button', { name: 'Clear selection' })).toHaveFocus();
  });

  it('accepts translated messages and a sticky placement', () => {
    render(
      <BulkActionBar
        count={4}
        onClear={() => {}}
        placement="sticky"
        messages={{
          selected: (n) => `${n} ausgewählt`,
          clear: 'Auswahl aufheben',
          label: 'Sammelaktionen',
        }}
      />,
    );
    expect(screen.getByRole('toolbar', { name: 'Sammelaktionen' })).toBeInTheDocument();
    expect(screen.getByText('4 ausgewählt')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Auswahl aufheben' })).toBeInTheDocument();
    expect(document.querySelector('.mn-bulk-action-bar')).toHaveAttribute(
      'data-placement',
      'sticky',
    );
  });

  it('renders on the server', () => {
    const html = renderToString(
      <BulkActionBar
        count={2}
        onClear={() => {}}
        actions={[{ id: 'x', label: 'Archive', onSelect: () => {} }]}
      />,
    );
    expect(html).toContain('mn-bulk-action-bar');
    expect(html).toContain('2 selected');
    expect(renderToString(<BulkActionBar count={0} onClear={() => {}} />)).toContain(
      'data-state="closed"',
    );
  });
});
