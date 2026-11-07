import { describe, expect, it, vi } from 'vitest';
import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { createRef, useState } from 'react';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import {
  SlashMenu,
  filterSlashMenuItems,
  findSlashQuery,
  groupSlashMenuItems,
  useSlashMenu,
  type SlashMenuItem,
} from './index.js';

const ITEMS: SlashMenuItem[] = [
  {
    id: 'h1',
    label: 'Heading 1',
    description: 'Large section title',
    group: 'Text',
    keywords: ['title'],
  },
  { id: 'quote', label: 'Quote', description: 'Indented quotation', group: 'Text' },
  { id: 'bullets', label: 'Bulleted list', description: 'Simple list', group: 'Lists' },
  {
    id: 'todo',
    label: 'Checklist',
    description: 'Items to tick off',
    group: 'Lists',
    keywords: ['todo'],
  },
  {
    id: 'table',
    label: 'Table',
    description: 'Needs a wider screen',
    group: 'Insert',
    disabled: true,
  },
];

function Harness({ onSelect }: { onSelect: (item: SlashMenuItem) => string | undefined }) {
  const [value, setValue] = useState('');
  const slash = useSlashMenu({
    items: ITEMS,
    value,
    onChange: setValue,
    onSelect,
    groupOrder: ['Text', 'Lists', 'Insert'],
  });
  return (
    <div>
      <label htmlFor="composer">Message</label>
      <textarea id="composer" {...slash.textareaProps} />
      <button type="button" onClick={slash.openMenu}>
        Insert
      </button>
      {slash.open ? <SlashMenu {...slash.menuProps} /> : null}
    </div>
  );
}

describe('helpers', () => {
  it('filters with the same ranking as the AI slash commands', () => {
    expect(filterSlashMenuItems(ITEMS, '').map((i) => i.id)).toEqual(ITEMS.map((i) => i.id));
    expect(filterSlashMenuItems(ITEMS, 'li').map((i) => i.id)).toEqual(['bullets', 'todo']);
    expect(filterSlashMenuItems(ITEMS, 'ToDo').map((i) => i.id)).toEqual(['todo']);
    expect(filterSlashMenuItems(ITEMS, 'q').map((i) => i.id)).toEqual(['quote']);
    expect(filterSlashMenuItems(ITEMS, 'zzz')).toEqual([]);
  });

  it('groups in the given order, then first-seen order', () => {
    const groups = groupSlashMenuItems([...ITEMS, { id: 'x', label: 'Other' }], ['Lists']);
    expect(groups.map((g) => g.label)).toEqual(['Lists', 'Text', 'Insert', undefined]);
  });

  it('finds a slash at the start of a line only by default', () => {
    expect(findSlashQuery('/he', 3)).toEqual({ query: 'he', start: 0, end: 3 });
    expect(findSlashQuery('hi\n/', 4)).toEqual({ query: '', start: 3, end: 4 });
    expect(findSlashQuery('1/2', 3)).toBeNull();
    expect(findSlashQuery('say /he', 7)).toBeNull();
    expect(findSlashQuery('say /he', 7, { position: 'word-start' })).toEqual({
      query: 'he',
      start: 4,
      end: 7,
    });
    expect(findSlashQuery('/a b', 4)).toBeNull();
  });
});

describe('SlashMenu', () => {
  it('renders a named listbox with labelled groups and forwards its ref', async () => {
    const ref = createRef<HTMLDivElement>();
    render(
      <SlashMenu
        ref={ref}
        data-testid="root"
        items={ITEMS}
        activeId="quote"
        onSelect={() => {}}
        groupOrder={['Text', 'Lists', 'Insert']}
      />,
    );
    expect(ref.current).toBe(screen.getByTestId('root'));
    expect(ref.current).toHaveClass('mn-slash-menu');
    const list = screen.getByRole('listbox', { name: 'Insert block' });
    expect(within(list).getByRole('group', { name: 'Lists' })).toBeInTheDocument();
    const quote = within(list).getByRole('option', { name: 'Quote' });
    expect(quote).toHaveAttribute('aria-selected', 'true');
    expect(quote).toHaveAccessibleDescription('Indented quotation');
    expect(within(list).getByRole('option', { name: 'Table' })).toHaveAttribute(
      'aria-disabled',
      'true',
    );
    expect(screen.getByRole('status')).toHaveTextContent('5 options');
    await expectNoA11yViolations();
  });

  it('shows the empty message and accepts custom strings', () => {
    render(
      <SlashMenu
        items={[]}
        query="zz"
        onSelect={() => {}}
        messages={{ empty: (q) => `No match for ${q}` }}
      />,
    );
    expect(screen.getByText('No match for /zz')).toBeInTheDocument();
    expect(screen.queryByRole('listbox')).not.toBeInTheDocument();
  });

  it('picks on click but never a disabled item', async () => {
    const user = userEvent.setup();
    const onSelect = vi.fn();
    render(<SlashMenu items={ITEMS} onSelect={onSelect} />);
    await user.click(screen.getByRole('option', { name: 'Table' }));
    expect(onSelect).not.toHaveBeenCalled();
    await user.click(screen.getByRole('option', { name: 'Checklist' }));
    expect(onSelect).toHaveBeenCalledWith(ITEMS[3]);
  });
});

describe('useSlashMenu', () => {
  it('opens on a slash, moves with the arrows across groups and replaces the query', async () => {
    const user = userEvent.setup();
    const onSelect = vi.fn((item: SlashMenuItem) => (item.id === 'bullets' ? '- ' : undefined));
    render(<Harness onSelect={onSelect} />);
    const box = screen.getByRole('textbox', { name: 'Message' });
    await user.type(box, '/');
    const list = screen.getByRole('listbox', { name: 'Insert block' });
    expect(box).toHaveAttribute('aria-controls', list.id);
    expect(box).toHaveAttribute('aria-autocomplete', 'list');
    const first = screen.getByRole('option', { name: 'Heading 1' });
    expect(box).toHaveAttribute('aria-activedescendant', first.id);

    await user.keyboard('{ArrowDown}{ArrowDown}');
    const bullets = screen.getByRole('option', { name: 'Bulleted list' });
    expect(box).toHaveAttribute('aria-activedescendant', bullets.id);
    await user.keyboard('{Enter}');
    expect(onSelect).toHaveBeenCalledWith(ITEMS[2]);
    expect(box).toHaveValue('- ');
    expect(screen.queryByRole('listbox')).not.toBeInTheDocument();
    expect(box).toHaveFocus();
  });

  it('filters while typing, skips disabled items and wraps', async () => {
    const user = userEvent.setup();
    render(<Harness onSelect={() => undefined} />);
    const box = screen.getByRole('textbox', { name: 'Message' });
    await user.type(box, '/l');
    expect(screen.getAllByRole('option').map((o) => o.textContent)).toEqual([
      'Heading 1Large section title',
      'Bulleted listSimple list',
      'ChecklistItems to tick off',
      'TableNeeds a wider screen',
    ]);
    expect(box).toHaveAttribute(
      'aria-activedescendant',
      screen.getByRole('option', { name: 'Heading 1' }).id,
    );
    // Up from the first wraps to the last enabled item, skipping the disabled Table.
    await user.keyboard('{ArrowUp}');
    expect(box).toHaveAttribute(
      'aria-activedescendant',
      screen.getByRole('option', { name: 'Checklist' }).id,
    );
    await user.keyboard('{Tab}');
    expect(box).toHaveValue('');
    expect(box).toHaveFocus();
  });

  it('closes on Escape and reopens when the query changes', async () => {
    const user = userEvent.setup();
    render(<Harness onSelect={() => undefined} />);
    const box = screen.getByRole('textbox', { name: 'Message' });
    await user.type(box, '/q');
    expect(screen.getByRole('listbox')).toBeInTheDocument();
    await user.keyboard('{Escape}');
    expect(screen.queryByRole('listbox')).not.toBeInTheDocument();
    expect(box).not.toHaveAttribute('aria-controls');
    await user.keyboard('u');
    expect(screen.getByRole('option', { name: 'Quote' })).toBeInTheDocument();
  });

  it('does not open mid-line and lets Enter through when closed', async () => {
    const user = userEvent.setup();
    render(<Harness onSelect={() => undefined} />);
    const box = screen.getByRole('textbox', { name: 'Message' });
    await user.type(box, 'a/b{Enter}c');
    expect(screen.queryByRole('listbox')).not.toBeInTheDocument();
    expect(box).toHaveValue('a/b\nc');
  });

  it('opens from a button by inserting the trigger', async () => {
    const user = userEvent.setup();
    render(<Harness onSelect={() => undefined} />);
    await user.click(screen.getByRole('button', { name: 'Insert' }));
    expect(screen.getByRole('textbox', { name: 'Message' })).toHaveValue('/');
    expect(screen.getByRole('listbox')).toBeInTheDocument();
  });
});

describe('SSR', () => {
  it('renders on the server', () => {
    const html = renderToString(<SlashMenu items={ITEMS} onSelect={() => {}} />);
    expect(html).toContain('mn-slash-menu');
    expect(html).toContain('role="listbox"');
    expect(renderToString(<Harness onSelect={() => undefined} />)).toContain('textarea');
  });
});
