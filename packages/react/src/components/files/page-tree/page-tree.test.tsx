import { describe, expect, it, vi } from 'vitest';
import { useState } from 'react';
import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { PageTree, type PageTreeGroup, type PageTreeMove, type PageTreeProps } from './index.js';

const GROUPS: PageTreeGroup[] = [
  {
    id: 'g1',
    label: 'Introduction',
    pages: [
      { id: 'p1', title: 'Overview' },
      { id: 'p2', title: 'Goals' },
    ],
  },
  { id: 'g2', label: 'Details', pages: [{ id: 'p3', title: 'Timeline' }] },
  { id: 'g3', label: 'Appendix', pages: [] },
];

function Editor(props: Partial<PageTreeProps> & { onMove?: (move: PageTreeMove) => void }) {
  const [groups, setGroups] = useState(GROUPS);
  const { onMove, ...rest } = props;
  return (
    <PageTree
      groups={groups}
      activePageId="p1"
      onReorder={(next, move) => {
        setGroups(next);
        onMove?.(move);
      }}
      {...rest}
    />
  );
}

describe('PageTree (navigation)', () => {
  it('lists grouped pages with running numbers and the current page', async () => {
    const onSelectPage = vi.fn();
    render(<PageTree groups={GROUPS} activePageId="p2" onSelectPage={onSelectPage} />);
    const nav = screen.getByRole('navigation', { name: 'Pages' });
    const intro = within(nav).getByRole('group', { name: 'Introduction' });
    expect(
      within(intro)
        .getAllByRole('button')
        .map((b) => b.textContent),
    ).toEqual(['01Overview', '02Goals']);
    expect(within(nav).getByRole('button', { name: 'Timeline' })).toBeInTheDocument();
    expect(within(intro).getByRole('button', { name: /Goals/ })).toHaveAttribute(
      'aria-current',
      'page',
    );
    expect(screen.getByText('No pages yet.')).toBeInTheDocument();
    await userEvent.setup().click(within(intro).getByRole('button', { name: /Overview/ }));
    expect(onSelectPage).toHaveBeenCalledWith('p1');
    await expectNoA11yViolations();
  });

  it('renders links, through renderLink when given', () => {
    const linked = GROUPS.map((g) => ({
      ...g,
      pages: g.pages.map((p) => ({ ...p, href: `/doc/${p.id}` })),
    }));
    const { rerender } = render(<PageTree groups={linked} activePageId="p1" numbered={false} />);
    expect(screen.getByRole('link', { name: 'Overview' })).toHaveAttribute('href', '/doc/p1');
    expect(screen.getByRole('link', { name: 'Overview' })).toHaveAttribute('aria-current', 'page');
    rerender(
      <PageTree
        groups={linked}
        renderLink={(page, { children, ...props }) => (
          <a href={`#${page.id}`} {...props}>
            {children}
          </a>
        )}
      />,
    );
    expect(screen.getByRole('link', { name: /Goals/ })).toHaveAttribute('href', '#p2');
  });

  it('collapses to a toggle', async () => {
    const onCollapsedChange = vi.fn();
    const { rerender } = render(<PageTree groups={GROUPS} onCollapsedChange={onCollapsedChange} />);
    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: 'Hide pages' }));
    expect(onCollapsedChange).toHaveBeenCalledWith(true);
    rerender(<PageTree groups={GROUPS} collapsed onCollapsedChange={onCollapsedChange} />);
    expect(screen.getByRole('button', { name: 'Show pages' })).toHaveAttribute(
      'aria-expanded',
      'false',
    );
    expect(screen.queryByText('Overview')).not.toBeInTheDocument();
  });
});

describe('PageTree (organiser)', () => {
  it('reorders pages within a group from the keyboard', async () => {
    const onMove = vi.fn();
    render(<Editor onMove={onMove} />);
    const user = userEvent.setup();
    const handle = screen.getByRole('button', { name: 'Reorder Overview' });
    handle.focus();
    await user.keyboard(' ');
    await user.keyboard('{ArrowDown}');
    await user.keyboard(' ');
    expect(onMove).toHaveBeenCalledWith({
      kind: 'page',
      id: 'p1',
      fromGroupId: 'g1',
      toGroupId: 'g1',
      toIndex: 1,
    });
    const intro = screen.getByRole('group', { name: 'Introduction' });
    expect(
      Array.from(intro.querySelectorAll('.mn-page-tree__page')).map((b) => b.textContent),
    ).toEqual(['01Goals', '02Overview']);
    await expectNoA11yViolations();
  });

  it('moves a page to another group from its menu', async () => {
    const onMove = vi.fn();
    render(<Editor onMove={onMove} />);
    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: 'Actions for Goals' }));
    await user.click(await screen.findByRole('menuitem', { name: 'Appendix' }));
    expect(onMove).toHaveBeenCalledWith({
      kind: 'page',
      id: 'p2',
      fromGroupId: 'g1',
      toGroupId: 'g3',
      toIndex: 0,
    });
    expect(
      within(screen.getByRole('group', { name: 'Appendix' })).getByText('Goals'),
    ).toBeInTheDocument();
    expect(screen.getByText('Goals moved to Appendix.')).toBeInTheDocument();
  });

  it('moves groups up and down', async () => {
    const onMove = vi.fn();
    render(<Editor onMove={onMove} />);
    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: 'Actions for Details' }));
    await user.click(await screen.findByRole('menuitem', { name: 'Move group up' }));
    expect(onMove).toHaveBeenCalledWith({ kind: 'group', id: 'g2', toIndex: 0 });
    const labels = screen.getAllByRole('group').map((g) => g.getAttribute('aria-labelledby'));
    expect(document.getElementById(labels[0]!)).toHaveTextContent('Details');
    expect(screen.getByText('Details moved to position 1 of 3.')).toBeInTheDocument();
  });

  it('renames and adds groups through a prompt', async () => {
    const onRenameGroup = vi.fn(async () => {});
    const onAddGroup = vi.fn(async () => {});
    render(<Editor onRenameGroup={onRenameGroup} onAddGroup={onAddGroup} />);
    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: 'Actions for Details' }));
    await user.click(await screen.findByRole('menuitem', { name: 'Rename group' }));
    const field = await screen.findByRole('textbox', { name: 'Group name' });
    await user.clear(field);
    await user.type(field, 'Plan{Enter}');
    await waitFor(() => expect(onRenameGroup).toHaveBeenCalledWith('g2', 'Plan'));

    await user.click(screen.getByRole('button', { name: 'Add group' }));
    const name = await screen.findByRole('textbox', { name: 'Group name' });
    expect(name).toHaveValue('New group');
    await user.clear(name);
    await user.type(name, 'Risks{Enter}');
    await waitFor(() => expect(onAddGroup).toHaveBeenCalledWith('Risks'));
  });

  it('deletes a group with pages only after typing its name', async () => {
    const onDeleteGroup = vi.fn(async () => {});
    render(<Editor onDeleteGroup={onDeleteGroup} />);
    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: 'Actions for Introduction' }));
    await user.click(await screen.findByRole('menuitem', { name: 'Delete group' }));
    const dialog = await screen.findByRole('alertdialog', {
      name: 'Delete the “Introduction” group?',
    });
    expect(dialog).toHaveTextContent('Its 2 pages are deleted too');
    const confirmButton = within(dialog).getByRole('button', { name: 'Delete group' });
    expect(confirmButton).toBeDisabled();
    await user.type(within(dialog).getByRole('textbox'), 'Introduction');
    await user.click(confirmButton);
    await waitFor(() => expect(onDeleteGroup).toHaveBeenCalledWith('g1'));
  });

  it('deletes a page after confirmation and hides delete for the last page', async () => {
    const onDeletePage = vi.fn(async () => {});
    const { rerender } = render(<Editor onDeletePage={onDeletePage} />);
    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: 'Actions for Timeline' }));
    await user.click(await screen.findByRole('menuitem', { name: 'Delete page' }));
    const dialog = await screen.findByRole('alertdialog', { name: 'Delete “Timeline”?' });
    await user.click(within(dialog).getByRole('button', { name: 'Delete page' }));
    await waitFor(() => expect(onDeletePage).toHaveBeenCalledWith('p3'));

    rerender(
      <PageTree
        groups={[{ id: 'g', label: 'Only', pages: [{ id: 'x', title: 'Solo' }] }]}
        onDeletePage={onDeletePage}
      />,
    );
    expect(screen.queryByRole('button', { name: 'Actions for Solo' })).not.toBeInTheDocument();
  });

  it('adds pages, including the first page of an empty group', async () => {
    const onAddPage = vi.fn();
    render(<Editor onAddPage={onAddPage} />);
    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: 'Add page to Details' }));
    await user.click(screen.getByRole('button', { name: 'Add first page' }));
    expect(onAddPage.mock.calls).toEqual([['g2'], ['g3']]);
  });

  it('translates strings and shows untitled pages', () => {
    render(
      <PageTree
        groups={[{ id: 'g', label: 'Teil 1', pages: [{ id: 'x', title: '  ' }] }]}
        messages={{ label: 'Seiten', untitled: 'Ohne Titel' }}
      />,
    );
    expect(screen.getByRole('navigation', { name: 'Seiten' })).toBeInTheDocument();
    expect(screen.getByText('Ohne Titel')).toBeInTheDocument();
  });

  it('renders on the server, read-only and editable', () => {
    expect(renderToString(<PageTree groups={GROUPS} activePageId="p1" />)).toContain(
      'mn-page-tree',
    );
    expect(renderToString(<Editor />)).toContain('mn-reorderable-list');
  });
});
