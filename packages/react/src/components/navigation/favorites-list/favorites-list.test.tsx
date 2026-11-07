import { describe, expect, it, vi } from 'vitest';
import { useState, type AnchorHTMLAttributes } from 'react';
import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { FavoriteToggle, FavoritesList, type FavoriteItem } from './index.js';

const items: FavoriteItem[] = [
  { id: 'p1', label: 'Website refresh', href: '/projects/1', current: true },
  { id: 'v2', label: 'My open tasks', href: '/views/2' },
  { id: 'd3', label: 'Launch checklist', href: '/docs/3' },
];

function Editable(props: { onReorder?: boolean }) {
  const [list, setList] = useState(items);
  return (
    <FavoritesList
      items={list}
      onRemove={(item) => setList((l) => l.filter((i) => i.id !== item.id))}
      onReorder={props.onReorder ? setList : undefined}
    />
  );
}

describe('FavoritesList', () => {
  it('is a named navigation of links, marking the current page', async () => {
    render(<FavoritesList items={items} />);
    const nav = screen.getByRole('navigation', { name: 'Favorites' });
    expect(nav).toHaveClass('mn-favorites-list');
    const links = within(nav).getAllByRole('link');
    expect(links.map((l) => l.textContent)).toEqual([
      'Website refresh',
      'My open tasks',
      'Launch checklist',
    ]);
    expect(links[0]).toHaveAttribute('aria-current', 'page');
    expect(links[0]).toHaveAttribute('href', '/projects/1');
    expect(links[1]).not.toHaveAttribute('aria-current');
    // Read-only lists have no edit button.
    expect(within(nav).queryByRole('button')).not.toBeInTheDocument();
    await expectNoA11yViolations();
  });

  it('renders nothing when empty unless an empty message is given', () => {
    const { container, rerender } = render(<FavoritesList items={[]} />);
    expect(container).toBeEmptyDOMElement();
    rerender(<FavoritesList items={[]} empty="Star a page to pin it here." />);
    expect(screen.getByText('Star a page to pin it here.')).toBeInTheDocument();
  });

  it('uses a custom link component', () => {
    function RouterLink(props: AnchorHTMLAttributes<HTMLAnchorElement>) {
      return (
        <a {...props} href={props.href} data-router="">
          {props.children}
        </a>
      );
    }
    render(<FavoritesList items={items} linkAs={RouterLink} />);
    expect(screen.getByRole('link', { name: 'My open tasks' })).toHaveAttribute('data-router');
  });

  it('edit mode: removes items, announces it and keeps focus in the list', async () => {
    render(<Editable />);
    const user = userEvent.setup();
    const edit = screen.getByRole('button', { name: 'Edit favorites' });
    await user.click(edit);
    expect(edit).toHaveAccessibleName('Done');
    expect(screen.queryByRole('link')).not.toBeInTheDocument();
    await expectNoA11yViolations();
    await user.click(screen.getByRole('button', { name: 'Remove My open tasks from favorites' }));
    expect(screen.queryByText('My open tasks')).not.toBeInTheDocument();
    expect(screen.getByRole('status')).toHaveTextContent('My open tasks removed from favorites.');
    await waitFor(() =>
      expect(
        screen.getByRole('button', { name: 'Remove Launch checklist from favorites' }),
      ).toHaveFocus(),
    );
    await user.keyboard('{Enter}');
    await waitFor(() =>
      expect(
        screen.getByRole('button', { name: 'Remove Website refresh from favorites' }),
      ).toHaveFocus(),
    );
    await user.keyboard('{Enter}');
    // The list is empty: the section disappears.
    await waitFor(() => expect(screen.queryByRole('navigation')).not.toBeInTheDocument());
  });

  it('edit mode with onReorder: rows can be moved with the keyboard', async () => {
    render(<Editable onReorder />);
    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: 'Edit favorites' }));
    const handle = screen.getByRole('button', { name: 'Reorder Launch checklist' });
    handle.focus();
    await user.keyboard(' ');
    await user.keyboard('{ArrowUp}');
    await user.keyboard(' ');
    await user.click(screen.getByRole('button', { name: 'Done' }));
    expect(screen.getAllByRole('link').map((l) => l.textContent)).toEqual([
      'Website refresh',
      'Launch checklist',
      'My open tasks',
    ]);
  });

  it('edit mode can be controlled', async () => {
    const onEditingChange = vi.fn();
    const { rerender } = render(
      <FavoritesList
        items={items}
        onRemove={() => {}}
        editing={false}
        onEditingChange={onEditingChange}
      />,
    );
    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: 'Edit favorites' }));
    expect(onEditingChange).toHaveBeenCalledWith(true);
    expect(screen.getAllByRole('link')).toHaveLength(3);
    rerender(<FavoritesList items={items} onRemove={() => {}} editing />);
    expect(screen.queryByRole('link')).not.toBeInTheDocument();
  });

  it('translates its strings and forwards the ref', () => {
    let node: HTMLElement | null = null;
    render(
      <FavoritesList
        ref={(el) => {
          node = el;
        }}
        items={items}
        onRemove={() => {}}
        messages={{ title: 'Favoriten', edit: 'Bearbeiten' }}
      />,
    );
    expect(node).toBe(screen.getByRole('navigation', { name: 'Favoriten' }));
    expect(screen.getByRole('button', { name: 'Bearbeiten' })).toBeInTheDocument();
  });

  it('renders on the server', () => {
    const html = renderToString(<FavoritesList items={items} />);
    expect(html).toContain('mn-favorites-list');
    expect(html).toContain('aria-current="page"');
  });
});

describe('FavoriteToggle', () => {
  it('is a toggle button with a stable name', async () => {
    function Demo() {
      const [on, setOn] = useState(false);
      return <FavoriteToggle pressed={on} onPressedChange={setOn} />;
    }
    render(<Demo />);
    const user = userEvent.setup();
    const button = screen.getByRole('button', { name: 'Favorite' });
    expect(button).toHaveAttribute('aria-pressed', 'false');
    await user.click(button);
    expect(button).toHaveAttribute('aria-pressed', 'true');
    expect(button).toHaveAccessibleName('Favorite');
    await user.keyboard(' ');
    expect(button).toHaveAttribute('aria-pressed', 'false');
    await expectNoA11yViolations();
  });
});
