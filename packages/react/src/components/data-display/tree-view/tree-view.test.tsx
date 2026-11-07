import { useState } from 'react';
import { describe, expect, it, vi } from 'vitest';
import { act, render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { TreeView, type TreeViewItem } from './index.js';

const ITEMS: TreeViewItem[] = [
  {
    id: 'ops',
    label: 'Operations',
    description: 'Business unit',
    children: [
      { id: 'fin', label: 'Finance', children: [{ id: 'ap', label: 'Payables' }] },
      { id: 'legal', label: 'Legal', disabled: true },
      { id: 'facilities', label: 'Facilities' },
    ],
  },
  { id: 'eng', label: 'Engineering', children: [{ id: 'web', label: 'Web' }] },
  { id: 'sales', label: 'Sales' },
];

const item = (name: string) => screen.getByRole('treeitem', { name });

describe('TreeView', () => {
  it('exposes the tree structure', async () => {
    render(<TreeView label="Departments" items={ITEMS} defaultExpanded={['ops']} />);
    const tree = screen.getByRole('tree', { name: 'Departments' });
    expect(tree).toHaveClass('mn-tree-view');
    const ops = item('Operations');
    expect(ops).toHaveAttribute('aria-expanded', 'true');
    expect(ops).toHaveAttribute('aria-level', '1');
    expect(ops).toHaveAttribute('aria-posinset', '1');
    expect(ops).toHaveAttribute('aria-setsize', '3');
    expect(ops).toHaveAccessibleDescription('Business unit');
    expect(ops).toHaveAttribute('tabindex', '0');
    expect(item('Finance')).toHaveAttribute('aria-level', '2');
    expect(item('Finance')).toHaveAttribute('aria-expanded', 'false');
    expect(item('Sales')).not.toHaveAttribute('aria-expanded');
    expect(item('Legal')).toHaveAttribute('aria-disabled', 'true');
    expect(screen.queryByRole('treeitem', { name: 'Payables' })).toBeNull();
    expect(within(ops).getByRole('group')).toBeInTheDocument();
    await expectNoA11yViolations();
  });

  it('moves, expands and collapses with the arrow keys', async () => {
    const user = userEvent.setup();
    render(<TreeView label="Departments" items={ITEMS} />);
    await user.tab();
    expect(item('Operations')).toHaveFocus();
    await user.keyboard('{ArrowRight}');
    expect(item('Operations')).toHaveAttribute('aria-expanded', 'true');
    expect(item('Operations')).toHaveFocus();
    await user.keyboard('{ArrowRight}');
    expect(item('Finance')).toHaveFocus();
    await user.keyboard('{ArrowDown}');
    // Legal is disabled and skipped.
    expect(item('Facilities')).toHaveFocus();
    await user.keyboard('{ArrowLeft}');
    expect(item('Operations')).toHaveFocus();
    await user.keyboard('{ArrowLeft}');
    expect(item('Operations')).toHaveAttribute('aria-expanded', 'false');
    await user.keyboard('{End}');
    expect(item('Sales')).toHaveFocus();
    await user.keyboard('{Home}');
    expect(item('Operations')).toHaveFocus();
    await user.keyboard('{ArrowDown}{ArrowDown}');
    expect(item('Sales')).toHaveFocus();
    expect(item('Sales')).toHaveAttribute('tabindex', '0');
    expect(item('Operations')).toHaveAttribute('tabindex', '-1');
    await user.tab();
    expect(document.body).toHaveFocus();
  });

  it('expands all siblings with * and finds items by typing', async () => {
    const user = userEvent.setup();
    render(<TreeView label="Departments" items={ITEMS} />);
    await user.tab();
    await user.keyboard('*');
    expect(item('Operations')).toHaveAttribute('aria-expanded', 'true');
    expect(item('Engineering')).toHaveAttribute('aria-expanded', 'true');
    await user.keyboard('w');
    expect(item('Web')).toHaveFocus();
    const now = Date.now();
    const clock = vi.spyOn(Date, 'now').mockReturnValue(now + 1000);
    await user.keyboard('f');
    expect(item('Finance')).toHaveFocus();
    await user.keyboard('f');
    expect(item('Facilities')).toHaveFocus();
    clock.mockReturnValue(now + 3000);
    await user.keyboard('fi');
    expect(item('Finance')).toHaveFocus();
    clock.mockRestore();
  });

  it('selects one item and activates with Enter and click', async () => {
    const user = userEvent.setup();
    const onActivate = vi.fn();
    const onSelectedChange = vi.fn();
    render(
      <TreeView
        label="Departments"
        items={ITEMS}
        onActivate={onActivate}
        onSelectedChange={onSelectedChange}
      />,
    );
    await user.tab();
    await user.keyboard('{ArrowDown}{Enter}');
    expect(item('Engineering')).toHaveAttribute('aria-selected', 'true');
    expect(item('Operations')).toHaveAttribute('aria-selected', 'false');
    expect(onActivate).toHaveBeenLastCalledWith(expect.objectContaining({ id: 'eng' }));
    expect(onSelectedChange).toHaveBeenLastCalledWith(['eng']);
    await user.click(screen.getByText('Sales'));
    expect(item('Sales')).toHaveAttribute('aria-selected', 'true');
    expect(item('Sales')).toHaveFocus();
    await user.click(item('Operations').querySelector('.mn-tree-view__toggle') as Element);
    expect(item('Operations')).toHaveAttribute('aria-expanded', 'true');
    expect(item('Operations')).toHaveAttribute('aria-selected', 'false');
  });

  it('supports multiple selection', async () => {
    const user = userEvent.setup();
    render(<TreeView label="Departments" items={ITEMS} selectionMode="multiple" />);
    expect(screen.getByRole('tree')).toHaveAttribute('aria-multiselectable', 'true');
    await user.tab();
    await user.keyboard('[Space]{Shift>}{ArrowDown}{/Shift}');
    expect(item('Operations')).toHaveAttribute('aria-selected', 'true');
    expect(item('Engineering')).toHaveAttribute('aria-selected', 'true');
    await user.keyboard('[Space]');
    expect(item('Engineering')).toHaveAttribute('aria-selected', 'false');
    await user.keyboard('{Control>}a{/Control}');
    expect(item('Sales')).toHaveAttribute('aria-selected', 'true');
  });

  it('works as an outline with no selection and a current item', async () => {
    const user = userEvent.setup();
    const onActivate = vi.fn();
    render(
      <TreeView
        label="Outline"
        variant="outline"
        selectionMode="none"
        defaultExpanded="all"
        current="fin"
        items={ITEMS}
        onActivate={onActivate}
      />,
    );
    expect(item('Payables')).toBeInTheDocument();
    expect(item('Finance')).toHaveAttribute('aria-current', 'true');
    expect(item('Finance')).not.toHaveAttribute('aria-selected');
    await user.tab();
    await user.keyboard('[Space]');
    expect(onActivate).toHaveBeenCalledWith(expect.objectContaining({ id: 'ops' }));
    await expectNoA11yViolations();
  });

  it('loads children lazily and announces failures', async () => {
    const user = userEvent.setup();
    function Lazy({ fail }: { fail?: boolean }) {
      const [items, setItems] = useState<TreeViewItem[]>([
        { id: 'root', label: 'Root', hasChildren: true },
      ]);
      return (
        <TreeView
          label="Lazy"
          items={items}
          onLoadChildren={() =>
            fail
              ? Promise.reject(new Error('x'))
              : Promise.resolve().then(() =>
                  setItems([
                    { id: 'root', label: 'Root', children: [{ id: 'leaf', label: 'Leaf' }] },
                  ]),
                )
          }
        />
      );
    }
    const { unmount } = render(<Lazy />);
    await user.tab();
    await user.keyboard('{ArrowRight}');
    expect(await screen.findByRole('treeitem', { name: 'Leaf' })).toBeInTheDocument();
    expect(item('Root')).not.toHaveAttribute('aria-busy');
    unmount();
    render(<Lazy fail />);
    await user.tab();
    await user.keyboard('{ArrowRight}');
    await waitFor(() =>
      expect(screen.getByRole('status')).toHaveTextContent('Could not load the items in Root.'),
    );
    expect(item('Root')).toHaveAttribute('aria-expanded', 'false');
  });

  it('keeps row actions out of the tab order except on the focused row', async () => {
    const user = userEvent.setup();
    const onEdit = vi.fn();
    render(
      <TreeView
        label="Departments"
        items={ITEMS}
        renderActions={(i, { tabIndex }) => (
          <button type="button" tabIndex={tabIndex} onClick={() => onEdit(i.id)}>
            Edit {i.label}
          </button>
        )}
      />,
    );
    expect(item('Operations')).toHaveAccessibleName('Operations');
    await user.tab();
    await user.tab();
    expect(screen.getByRole('button', { name: 'Edit Operations' })).toHaveFocus();
    await user.keyboard('{ArrowDown}');
    // Arrow keys on an action belong to the action, not the tree.
    expect(screen.getByRole('button', { name: 'Edit Operations' })).toHaveFocus();
    await user.keyboard('{Enter}');
    expect(onEdit).toHaveBeenCalledWith('ops');
    expect(screen.getByRole('button', { name: 'Edit Engineering' })).toHaveAttribute(
      'tabindex',
      '-1',
    );
    await expectNoA11yViolations();
  });

  it('moves focus to the parent when a controlled collapse hides the focused item', async () => {
    const user = userEvent.setup();
    function Controlled() {
      const [expanded, setExpanded] = useState<string[]>(['eng']);
      return (
        <>
          <TreeView label="T" items={ITEMS} expanded={expanded} onExpandedChange={setExpanded} />
          <button type="button" onClick={() => setExpanded([])}>
            Collapse all
          </button>
        </>
      );
    }
    render(<Controlled />);
    await user.click(screen.getByText('Web'));
    expect(item('Web')).toHaveFocus();
    act(() => screen.getByRole('button', { name: 'Collapse all' }).click());
    expect(item('Engineering')).toHaveAttribute('tabindex', '0');
  });

  it('renders on the server', () => {
    const html = renderToString(<TreeView label="T" items={ITEMS} defaultExpanded="all" />);
    expect(html).toContain('role="tree"');
    expect(html).toContain('Payables');
  });
});
