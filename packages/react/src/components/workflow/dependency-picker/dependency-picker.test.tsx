import { describe, expect, it, vi } from 'vitest';
import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { createRef, useState } from 'react';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import {
  DependencyPicker,
  type DependencyItem,
  type DependencyLink,
  type DependencyRelation,
} from './index.js';

const ITEMS: DependencyItem[] = [
  {
    id: '1',
    key: 'STR-1',
    title: 'Design the export dialog',
    status: { id: 'd', name: 'Done', category: 'completed' },
  },
  { id: '2', key: 'STR-2', title: 'Write the PDF renderer', href: '/i/STR-2' },
  { id: '3', key: 'STR-3', title: 'Add export to the menu' },
  { id: '4', key: 'STR-4', title: 'This issue' },
];
const LINKS: DependencyLink[] = [
  { id: 'l1', relation: 'blocked-by', item: ITEMS[0]! },
  { id: 'l2', relation: 'relates', item: ITEMS[1]! },
];

function Controlled(props: { onAdd?: (r: DependencyRelation, i: DependencyItem) => void }) {
  const [links, setLinks] = useState<DependencyLink[]>(LINKS);
  return (
    <DependencyPicker
      value={links}
      items={ITEMS}
      excludeIds={['4']}
      onAdd={(relation, item) => {
        props.onAdd?.(relation, item);
        setLinks((l) => [...l, { id: `n${item.id}`, relation, item }]);
      }}
      onRemove={(link) => setLinks((l) => l.filter((x) => x.id !== link.id))}
    />
  );
}

describe('DependencyPicker', () => {
  it('groups links under relation headings with status in words', async () => {
    const ref = createRef<HTMLDivElement>();
    render(<DependencyPicker ref={ref} value={LINKS} />);
    expect(ref.current).toHaveClass('mn-dependency-picker');
    const blocked = screen.getByRole('region', { name: 'Blocked by' });
    expect(within(blocked).getByText('Design the export dialog')).toBeInTheDocument();
    expect(within(blocked).getByText('Status: Done')).toBeInTheDocument();
    expect(screen.getByRole('heading', { level: 3, name: 'Related to' })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Write the PDF renderer' })).toHaveAttribute(
      'href',
      '/i/STR-2',
    );
    expect(screen.queryByRole('combobox')).toBeNull();
    await expectNoA11yViolations();
  });

  it('adds a link with the chosen relation from the keyboard, excluding linked and excluded items', async () => {
    const user = userEvent.setup();
    const onAdd = vi.fn();
    render(<Controlled onAdd={onAdd} />);
    await user.selectOptions(screen.getByRole('combobox', { name: 'Relation' }), 'blocks');
    const search = screen.getByRole('combobox', { name: 'Find an item to link' });
    await user.type(search, 'export');
    const list = await screen.findByRole('listbox');
    const labels = within(list)
      .getAllByRole('option')
      .map((o) => o.textContent);
    expect(labels).toEqual([expect.stringContaining('Add export to the menu')]);
    await user.keyboard('{ArrowDown}{Enter}');
    expect(onAdd).toHaveBeenCalledWith('blocks', ITEMS[2]);
    expect(
      within(screen.getByRole('region', { name: 'Blocks' })).getByText('Add export to the menu'),
    ).toBeInTheDocument();
    expect(screen.getByRole('status')).toHaveTextContent(
      'Blocks STR-3 Add export to the menu added.',
    );
    const fresh = screen.getByRole('combobox', { name: 'Find an item to link' });
    expect(fresh).toHaveValue('');
    await waitFor(() => expect(fresh).toHaveFocus());
    await expectNoA11yViolations();
  });

  it('removes a link with a named button and announces it', async () => {
    const user = userEvent.setup();
    render(<Controlled />);
    await user.click(
      screen.getByRole('button', {
        name: 'Remove “Blocked by” link to STR-1 Design the export dialog',
      }),
    );
    expect(screen.queryByRole('region', { name: 'Blocked by' })).toBeNull();
    expect(screen.getByRole('status')).toHaveTextContent(
      'Blocked by STR-1 Design the export dialog removed.',
    );
  });

  it('keeps focus in the picker when a link is removed', async () => {
    const user = userEvent.setup();
    render(<Controlled />);
    await user.click(
      screen.getByRole('button', {
        name: 'Remove “Blocked by” link to STR-1 Design the export dialog',
      }),
    );
    expect(screen.getByRole('button', { name: /^Remove “Related to” link/ })).toHaveFocus();
    await user.click(screen.getByRole('button', { name: /^Remove “Related to” link/ }));
    // No links left: focus goes to the search, not to the top of the page.
    expect(screen.getByRole('combobox', { name: 'Find an item to link' })).toHaveFocus();
  });

  it('searches a server and drops excluded results', async () => {
    const user = userEvent.setup();
    const onSearch = vi.fn(async (q: string) =>
      ITEMS.filter((i) => i.title.toLowerCase().includes(q)),
    );
    render(<DependencyPicker value={[]} onAdd={() => {}} onSearch={onSearch} excludeIds={['3']} />);
    expect(screen.getByText('No linked items.')).toBeInTheDocument();
    await user.type(screen.getByRole('combobox', { name: 'Find an item to link' }), 'export');
    await waitFor(() => expect(onSearch).toHaveBeenCalledWith('export', expect.anything()));
    const list = await screen.findByRole('listbox');
    await waitFor(() =>
      expect(
        within(list)
          .getAllByRole('option')
          .map((o) => o.textContent),
      ).toEqual([expect.stringContaining('Design the export dialog')]),
    );
  });

  it('limits relations, translates, and disables', () => {
    render(
      <DependencyPicker
        value={[]}
        onAdd={() => {}}
        relations={['blocks', 'blocked-by']}
        disabled
        messages={{ relationLabel: 'Relation (fr)', relations: { blocks: 'Bloque' } }}
      />,
    );
    const select = screen.getByRole('combobox', { name: 'Relation (fr)' });
    expect(select).toBeDisabled();
    expect(
      within(select)
        .getAllByRole('option')
        .map((o) => o.textContent),
    ).toEqual(['Bloque', 'Blocked by']);
  });

  it('renders on the server', () => {
    const html = renderToString(<DependencyPicker value={LINKS} onAdd={() => {}} items={ITEMS} />);
    expect(html).toContain('mn-dependency-picker');
    expect(html).toContain('Blocked by');
  });
});
