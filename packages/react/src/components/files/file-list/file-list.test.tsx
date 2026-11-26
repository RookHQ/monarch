import { describe, expect, it, vi } from 'vitest';
import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { createRef, useState } from 'react';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { FileList, type FileListItem } from './index.js';

const ITEMS: FileListItem[] = [
  { id: 'a', name: 'alpha.pdf', size: 1000 },
  { id: 'b', name: 'beta.png', size: 2000 },
  { id: 'c', name: 'gamma.xlsx', size: 3000, disabled: true },
  { id: 'd', name: 'delta.txt', size: 4000 },
];

describe('FileList', () => {
  it('renders a named list of rows and opens items', async () => {
    const user = userEvent.setup();
    const onOpen = vi.fn();
    const ref = createRef<HTMLDivElement>();
    render(<FileList ref={ref} items={ITEMS} label="Project files" onOpen={onOpen} />);
    expect(ref.current).toHaveClass('mn-file-list');
    expect(ref.current).toHaveAttribute('data-layout', 'list');
    const list = screen.getByRole('list', { name: 'Project files' });
    expect(within(list).getAllByRole('listitem')).toHaveLength(4);
    await user.tab();
    expect(screen.getByRole('button', { name: 'alpha.pdf' })).toHaveFocus();
    await user.keyboard('{Enter}');
    expect(onOpen).toHaveBeenCalledWith(ITEMS[0], expect.anything());
    await expectNoA11yViolations();
  });

  it('selects one, a Shift range skipping disabled files, all, and clears with Escape', async () => {
    const user = userEvent.setup();
    const onSelectionChange = vi.fn();
    render(
      <FileList
        items={ITEMS}
        selectable
        onSelectionChange={onSelectionChange}
        bulkActions={(ids) => <button type="button">Delete {ids.length}</button>}
      />,
    );
    await user.click(screen.getByRole('checkbox', { name: 'Select alpha.pdf' }));
    expect(onSelectionChange).toHaveBeenLastCalledWith(['a']);
    expect(screen.getByRole('status')).toHaveTextContent('1 file selected');
    expect(screen.getByRole('checkbox', { name: 'Select all files' })).toHaveAttribute(
      'data-state',
      'indeterminate',
    );
    await user.keyboard('{Shift>}');
    await user.click(screen.getByRole('checkbox', { name: 'Select delta.txt' }));
    await user.keyboard('{/Shift}');
    expect(onSelectionChange).toHaveBeenLastCalledWith(['a', 'b', 'd']);
    const toolbar = screen.getByRole('toolbar', { name: 'Actions for selected files' });
    expect(within(toolbar).getByRole('button')).toHaveTextContent('Delete 3');
    expect(screen.getByRole('checkbox', { name: 'Select all files' })).toBeChecked();
    await expectNoA11yViolations();
    await user.click(screen.getByRole('checkbox', { name: 'Select all files' }));
    expect(onSelectionChange).toHaveBeenLastCalledWith([]);
    await user.click(screen.getByRole('checkbox', { name: 'Select all files' }));
    expect(onSelectionChange).toHaveBeenLastCalledWith(['a', 'b', 'd']);
    screen.getByRole('checkbox', { name: 'Select beta.png' }).focus();
    await user.keyboard('{Escape}');
    expect(onSelectionChange).toHaveBeenLastCalledWith([]);
    expect(screen.queryByRole('toolbar')).toBeNull();
  });

  it('can be controlled and renders a grid with per-file actions', async () => {
    const user = userEvent.setup();
    function Controlled() {
      const [ids, setIds] = useState<string[]>(['b']);
      return (
        <FileList
          items={ITEMS}
          layout="grid"
          selectable
          selectedIds={ids}
          onSelectionChange={setIds}
          renderActions={(item) => <button type="button">Share {item.name}</button>}
        />
      );
    }
    const { container } = render(<Controlled />);
    expect(container.firstElementChild).toHaveAttribute('data-layout', 'grid');
    expect(container.querySelector('.mn-file-card')).toHaveAttribute('data-layout', 'card');
    expect(screen.getByRole('checkbox', { name: 'Select beta.png' })).toBeChecked();
    await user.click(screen.getByRole('checkbox', { name: 'Select beta.png' }));
    expect(screen.getByRole('checkbox', { name: 'Select beta.png' })).not.toBeChecked();
    expect(screen.getByRole('button', { name: 'Share gamma.xlsx' })).toBeInTheDocument();
    await expectNoA11yViolations();
  });

  it('shows the empty state and translates', () => {
    const { rerender } = render(<FileList items={[]} />);
    expect(screen.getByText('No files yet.')).toBeInTheDocument();
    rerender(<FileList items={[]} empty={<p>Drop files here</p>} />);
    expect(screen.getByText('Drop files here')).toBeInTheDocument();
    rerender(
      <FileList
        key="filled"
        items={ITEMS}
        selectable
        defaultSelectedIds={['a', 'b']}
        messages={{ selectAll: 'Tout sélectionner', selectedCount: (n) => `${n} sélectionnés` }}
      />,
    );
    expect(screen.getByRole('checkbox', { name: 'Tout sélectionner' })).toBeInTheDocument();
    expect(screen.getByRole('status')).toHaveTextContent('2 sélectionnés');
  });

  it('renders on the server', () => {
    const html = renderToString(<FileList items={ITEMS} selectable label="Files" />);
    expect(html).toContain('mn-file-list');
    expect(html).toContain('alpha.pdf');
  });
});
