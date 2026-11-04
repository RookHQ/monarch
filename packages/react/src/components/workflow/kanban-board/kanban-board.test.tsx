import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { act, fireEvent, render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { useState } from 'react';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { KanbanBoard, type KanbanColumn, type KanbanBoardProps } from './index.js';
import { columnAt, edgeSpeed, indexAt, locate, moveCard } from './kanban-board.drag.js';

// jsdom has no PointerEvent: a MouseEvent with a pointerId is enough here.
if (typeof window.PointerEvent === 'undefined') {
  class PointerEventPolyfill extends MouseEvent {
    pointerId: number;
    pointerType: string;
    constructor(type: string, init: PointerEventInit = {}) {
      super(type, init);
      this.pointerId = init.pointerId ?? 0;
      this.pointerType = init.pointerType ?? 'mouse';
    }
  }
  (window as unknown as { PointerEvent: unknown }).PointerEvent = PointerEventPolyfill;
}

interface Task {
  id: string;
  title: string;
}

const COLUMNS: KanbanColumn<Task>[] = [
  {
    id: 'todo',
    title: 'To do',
    items: [
      { id: 't1', title: 'Write brief' },
      { id: 't2', title: 'Draft outline' },
      { id: 't3', title: 'Book venue' },
    ],
  },
  { id: 'doing', title: 'In progress', wipLimit: 2, items: [{ id: 't4', title: 'Review copy' }] },
  { id: 'done', title: 'Done', items: [] },
];

function Board(props: Partial<KanbanBoardProps<Task>>) {
  return (
    <KanbanBoard<Task>
      aria-label="Launch plan"
      defaultColumns={COLUMNS}
      getItemId={(t) => t.id}
      getItemLabel={(t) => t.title}
      renderCard={(t) => <span>{t.title}</span>}
      {...props}
    />
  );
}

const list = (name: string) => screen.getByRole('list', { name });
const titlesIn = (name: string) =>
  within(list(name))
    .queryAllByRole('listitem')
    .map((li) => li.textContent);
const handle = (card: string) => screen.getByRole('button', { name: `Move ${card}` });
const live = () => (document.querySelector('[aria-live="assertive"]')?.textContent ?? '').trim();

describe('KanbanBoard', () => {
  it('renders labelled columns, counts and WIP state without axe violations', async () => {
    const { container } = render(
      <Board
        defaultColumns={[
          ...COLUMNS.slice(0, 1),
          {
            ...COLUMNS[1]!,
            items: [...COLUMNS[1]!.items, { id: 'x', title: 'X' }, { id: 'y', title: 'Y' }],
          },
          COLUMNS[2]!,
        ]}
      />,
    );
    const root = screen.getByRole('region', { name: 'Launch plan' });
    expect(root).toHaveClass('mn-kanban-board');
    expect(screen.getByRole('heading', { level: 3, name: 'To do' })).toBeInTheDocument();
    expect(titlesIn('To do')).toEqual(['Write brief', 'Draft outline', 'Book venue']);
    const doing = container.querySelector('[data-column-id="doing"]')!;
    expect(doing).toHaveAttribute('data-wip', 'over');
    expect(doing).toHaveTextContent('3 cards, limit 2, over the limit');
    expect(container.querySelector('[data-column-id="done"]')).toHaveTextContent('No cards');
    expect(handle('Write brief')).toHaveAttribute('aria-describedby');
    await expectNoA11yViolations();
  });

  it('moves a card within a column from the keyboard and announces each step', async () => {
    const user = userEvent.setup();
    const onMove = vi.fn();
    render(<Board onMove={onMove} />);
    handle('Write brief').focus();
    await user.keyboard(' ');
    expect(live()).toBe('Picked up Write brief. To do, position 1 of 3.');
    expect(handle('Write brief')).toHaveAttribute('aria-pressed', 'true');
    await user.keyboard('{ArrowDown}');
    expect(live()).toBe('Write brief: To do, position 2 of 3.');
    expect(titlesIn('To do')).toEqual(['Draft outline', 'Write brief', 'Book venue']);
    expect(onMove).not.toHaveBeenCalled();
    await user.keyboard('{End}');
    expect(live()).toBe('Write brief: To do, position 3 of 3.');
    await user.keyboard('{Enter}');
    expect(live()).toBe('Write brief dropped in To do, position 3 of 3.');
    expect(onMove).toHaveBeenCalledTimes(1);
    expect(onMove.mock.calls[0]![0]).toMatchObject({
      itemId: 't1',
      fromColumnId: 'todo',
      fromIndex: 0,
      toColumnId: 'todo',
      toIndex: 2,
    });
    expect(titlesIn('To do')).toEqual(['Draft outline', 'Book venue', 'Write brief']);
    expect(handle('Write brief')).toHaveFocus();
    expect(handle('Write brief')).toHaveAttribute('aria-pressed', 'false');
  });

  it('moves a card across columns with the arrow keys and keeps focus on it', async () => {
    const user = userEvent.setup();
    const onMove = vi.fn();
    render(<Board onMove={onMove} />);
    handle('Draft outline').focus();
    await user.keyboard(' {ArrowRight}');
    expect(live()).toBe('Draft outline: In progress, position 2 of 2.');
    expect(handle('Draft outline')).toHaveFocus();
    await user.keyboard('{ArrowRight}');
    expect(live()).toBe('Draft outline: Done, position 1 of 1.');
    await user.keyboard('{ArrowRight}');
    expect(live()).toBe('Draft outline: Done, position 1 of 1.');
    await user.keyboard('{ArrowLeft}{Home} ');
    expect(onMove.mock.calls[0]![0]).toMatchObject({
      itemId: 't2',
      fromColumnId: 'todo',
      fromIndex: 1,
      toColumnId: 'doing',
      toIndex: 0,
    });
    const next = onMove.mock.calls[0]![1] as KanbanColumn<Task>[];
    expect(next.find((c) => c.id === 'doing')!.items.map((t) => t.id)).toEqual(['t2', 't4']);
    expect(titlesIn('In progress')).toEqual(['Draft outline', 'Review copy']);
    expect(handle('Draft outline')).toHaveFocus();
  });

  it('mirrors left and right in a right-to-left board', async () => {
    const user = userEvent.setup();
    render(
      <div dir="rtl" style={{ direction: 'rtl' }}>
        <Board />
      </div>,
    );
    handle('Write brief').focus();
    await user.keyboard(' {ArrowLeft}');
    expect(live()).toBe('Write brief: In progress, position 1 of 2.');
  });

  it('cancels with Escape and with Tab, restoring the order', async () => {
    const user = userEvent.setup();
    const onMove = vi.fn();
    render(<Board onMove={onMove} />);
    handle('Write brief').focus();
    await user.keyboard(' {ArrowDown}{ArrowRight}{Escape}');
    expect(live()).toBe('Move cancelled. Write brief returned to To do, position 1 of 3.');
    expect(titlesIn('To do')).toEqual(['Write brief', 'Draft outline', 'Book venue']);
    expect(handle('Write brief')).toHaveFocus();
    await user.keyboard(' {ArrowDown}');
    await user.tab();
    expect(titlesIn('To do')).toEqual(['Write brief', 'Draft outline', 'Book venue']);
    expect(onMove).not.toHaveBeenCalled();
  });

  it('skips columns refused by canMove and announces when none is left', async () => {
    const user = userEvent.setup();
    const canMove = vi.fn((_t: Task, to: string) => to !== 'doing');
    render(<Board canMove={canMove} />);
    handle('Write brief').focus();
    await user.keyboard(' ');
    const doing = document.querySelector('[data-column-id="doing"]')!;
    expect(doing).toHaveAttribute('data-drop', 'blocked');
    expect(doing).toHaveTextContent('Not allowed here');
    await user.keyboard('{ArrowRight}');
    expect(live()).toBe('Write brief: Done, position 1 of 1.');
    await user.keyboard('{Escape}');

    render(<Board canMove={() => false} />, {
      container: document.body.appendChild(document.createElement('div')),
    });
    const second = screen.getAllByRole('button', { name: 'Move Write brief' })[1]!;
    second.focus();
    await user.keyboard(' {ArrowRight}');
    expect(document.querySelectorAll('[aria-live="assertive"]')[1]!.textContent).toBe(
      'Write brief cannot move to In progress.',
    );
  });

  it('refuses a full column when WIP limits are enforced', async () => {
    const user = userEvent.setup();
    render(
      <Board
        enforceWipLimits
        defaultColumns={[
          COLUMNS[0]!,
          { ...COLUMNS[1]!, items: [...COLUMNS[1]!.items, { id: 'x', title: 'Extra' }] },
          COLUMNS[2]!,
        ]}
      />,
    );
    expect(document.querySelector('[data-column-id="doing"]')).toHaveAttribute('data-wip', 'at');
    handle('Write brief').focus();
    await user.keyboard(' {ArrowRight}');
    expect(live()).toBe('Write brief: Done, position 1 of 1.');
  });

  it('keeps an optimistic move while pending and rolls back when it fails', async () => {
    const user = userEvent.setup();
    let reject!: (e: unknown) => void;
    const onMoveError = vi.fn();
    render(
      <Board
        onMove={() =>
          new Promise((_, r) => {
            reject = r;
          })
        }
        onMoveError={onMoveError}
      />,
    );
    handle('Write brief').focus();
    await user.keyboard(' {ArrowRight}{ArrowRight} ');
    expect(titlesIn('Done')).toEqual(['Write brief']);
    const card = document.querySelector('[data-card-id="t1"]')!;
    expect(card).toHaveAttribute('data-pending');
    expect(card).toHaveAttribute('aria-busy', 'true');
    await act(async () => reject(new Error('Network')));
    expect(titlesIn('Done')).toEqual([]);
    expect(titlesIn('To do')).toEqual(['Write brief', 'Draft outline', 'Book venue']);
    expect(live()).toBe('Could not move Write brief. It is back in To do.');
    expect(onMoveError).toHaveBeenCalledWith(
      expect.any(Error),
      expect.objectContaining({ itemId: 't1' }),
    );
  });

  it('commits an uncontrolled move when its promise resolves', async () => {
    const user = userEvent.setup();
    let resolve!: () => void;
    render(
      <Board
        onMove={() =>
          new Promise<void>((r) => {
            resolve = r;
          })
        }
      />,
    );
    handle('Book venue').focus();
    await user.keyboard(' {ArrowRight} ');
    await act(async () => resolve());
    expect(titlesIn('In progress')).toEqual(['Review copy', 'Book venue']);
    expect(document.querySelector('[data-card-id="t3"]')).not.toHaveAttribute('data-pending');
  });

  it('works controlled: the product applies the columns from onMove', async () => {
    const user = userEvent.setup();
    function Controlled() {
      const [cols, setCols] = useState(COLUMNS);
      return <Board columns={cols} onMove={(_, next) => setCols(next)} />;
    }
    render(<Controlled />);
    handle('Review copy').focus();
    await user.keyboard(' {ArrowRight} ');
    expect(titlesIn('Done')).toEqual(['Review copy']);
    expect(titlesIn('In progress')).toEqual([]);
  });

  it('holds a resolved controlled move until the next columns arrive', async () => {
    const user = userEvent.setup();
    let resolve!: () => void;
    const { rerender } = render(
      <Board
        columns={COLUMNS}
        onMove={() =>
          new Promise<void>((r) => {
            resolve = r;
          })
        }
      />,
    );
    handle('Review copy').focus();
    await user.keyboard(' {ArrowRight} ');
    await act(async () => resolve());
    expect(titlesIn('Done')).toEqual(['Review copy']);
    // The product never applied the move: its next data wins.
    rerender(<Board columns={COLUMNS.map((c) => ({ ...c }))} />);
    expect(titlesIn('Done')).toEqual([]);
  });

  it('offers a Move to menu as a single-pointer alternative', async () => {
    const user = userEvent.setup();
    const onMove = vi.fn();
    render(<Board moveMenu onMove={onMove} canMove={(_t, to) => to !== 'done'} />);
    await user.click(screen.getByRole('button', { name: 'Move Write brief to…' }));
    expect(screen.getByRole('menuitem', { name: 'Move to Done' })).toHaveAttribute(
      'aria-disabled',
      'true',
    );
    await user.click(screen.getByRole('menuitem', { name: 'Move to In progress' }));
    expect(onMove.mock.calls[0]![0]).toMatchObject({ toColumnId: 'doing', toIndex: 1 });
    expect(titlesIn('In progress')).toEqual(['Review copy', 'Write brief']);
    expect(live()).toBe('Write brief dropped in In progress, position 2 of 2.');
    await waitFor(() => expect(handle('Write brief')).toHaveFocus());
  });

  it('collapses columns (uncontrolled and controlled)', async () => {
    const user = userEvent.setup();
    const onChange = vi.fn();
    render(<Board collapsible onCollapsedColumnIdsChange={onChange} />);
    const toggle = screen.getByRole('button', { name: 'Collapse Done' });
    expect(toggle).toHaveAttribute('aria-expanded', 'true');
    await user.click(toggle);
    expect(onChange).toHaveBeenCalledWith(['done']);
    expect(screen.getByRole('button', { name: 'Expand Done' })).toHaveAttribute(
      'aria-expanded',
      'false',
    );
    expect(document.querySelector('[data-column-id="done"]')).toHaveAttribute('data-collapsed');
    // Keyboard moves pass over collapsed columns.
    handle('Review copy').focus();
    await user.keyboard(' {ArrowRight}');
    expect(live()).toBe('Review copy: In progress, position 1 of 1.');
    await user.keyboard('{Escape}');
    await expectNoA11yViolations();
  });

  it('honours controlled collapsed ids', () => {
    render(<Board collapsible collapsedColumnIds={['todo']} />);
    expect(screen.getByRole('button', { name: 'Expand To do' })).toBeInTheDocument();
    expect(
      document.getElementById(
        screen.getByRole('button', { name: 'Expand To do' }).getAttribute('aria-controls')!,
      ),
    ).not.toBeVisible();
  });

  it('disables moving for locked cards and a disabled board', () => {
    const { rerender } = render(<Board isItemDisabled={(t) => t.id === 't4'} />);
    expect(handle('Review copy')).toBeDisabled();
    expect(handle('Write brief')).toBeEnabled();
    rerender(<Board disabled />);
    expect(handle('Write brief')).toBeDisabled();
  });

  it('renders the slot with card state and column actions and footer', () => {
    const renderCard = vi.fn((t: Task) => <span>{t.title}</span>);
    render(
      <Board
        renderCard={renderCard}
        defaultColumns={[
          {
            ...COLUMNS[0]!,
            actions: <button type="button">Add to To do</button>,
            footer: <span>Footer</span>,
          },
        ]}
      />,
    );
    expect(screen.getByRole('button', { name: 'Add to To do' })).toBeInTheDocument();
    expect(screen.getByText('Footer')).toBeInTheDocument();
    expect(renderCard).toHaveBeenCalledWith(COLUMNS[0]!.items[0], {
      columnId: 'todo',
      index: 0,
      dragging: false,
      overlay: false,
      pending: false,
      disabled: false,
    });
  });

  it('accepts translated messages', async () => {
    const user = userEvent.setup();
    render(
      <Board messages={{ handleLabel: (c) => `Déplacer ${c}`, pickedUp: (c) => `Pris ${c}` }} />,
    );
    screen.getByRole('button', { name: 'Déplacer Write brief' }).focus();
    await user.keyboard(' ');
    expect(live()).toBe('Pris Write brief');
  });

  it('renders on the server', () => {
    const html = renderToString(<Board />);
    expect(html).toContain('mn-kanban-board');
    expect(html).toContain('Write brief');
  });
});

describe('KanbanBoard pointer dragging', () => {
  // Columns 200px wide with 20px gaps; cards 40px tall from y = 50, 10px apart.
  const original = Element.prototype.getBoundingClientRect;
  beforeEach(() => {
    Element.prototype.getBoundingClientRect = function (this: Element) {
      const box = (left: number, top: number, w: number, h: number) =>
        ({
          left,
          top,
          right: left + w,
          bottom: top + h,
          width: w,
          height: h,
          x: left,
          y: top,
        }) as DOMRect;
      const section = this.closest('[data-column-id]');
      const columns = [...document.querySelectorAll('[data-column-id]')];
      const ci = section ? columns.indexOf(section) : -1;
      if (this.matches('[data-column-id]')) return box(ci * 220, 0, 200, 600);
      if (this.matches('li[data-card-id]')) {
        const i = [...this.parentElement!.children].indexOf(this);
        return box(ci * 220 + 8, 50 + i * 50, 184, 40);
      }
      return original.call(this);
    };
  });
  afterEach(() => {
    Element.prototype.getBoundingClientRect = original;
  });

  const down = (el: Element, x: number, y: number, extra: PointerEventInit = {}) =>
    fireEvent.pointerDown(el, { pointerId: 1, button: 0, clientX: x, clientY: y, ...extra });
  const move = (x: number, y: number) =>
    fireEvent.pointerMove(window, { pointerId: 1, clientX: x, clientY: y });
  const up = (x: number, y: number) =>
    fireEvent.pointerUp(window, { pointerId: 1, clientX: x, clientY: y });

  it('drags a card into another column and position', () => {
    const onMove = vi.fn();
    render(<Board onMove={onMove} />);
    down(handle('Write brief'), 20, 60);
    move(22, 60); // under the threshold: not a drag yet
    expect(document.querySelector('.mn-kanban-board__overlay')).toBeNull();
    move(240, 60);
    expect(live()).toBe('Write brief: In progress, position 1 of 2.');
    expect(document.querySelector('.mn-kanban-board__overlay')).toHaveAttribute(
      'aria-hidden',
      'true',
    );
    expect(document.querySelector('[data-column-id="doing"]')).toHaveAttribute('data-over');
    expect(titlesIn('In progress')).toEqual(['Write brief', 'Review copy']);
    move(240, 130);
    expect(titlesIn('In progress')).toEqual(['Review copy', 'Write brief']);
    up(240, 130);
    expect(onMove.mock.calls[0]![0]).toMatchObject({
      itemId: 't1',
      fromColumnId: 'todo',
      toColumnId: 'doing',
      toIndex: 1,
    });
    expect(document.querySelector('.mn-kanban-board__overlay')).toBeNull();
    expect(live()).toBe('Write brief dropped in In progress, position 2 of 2.');
  });

  it('reorders within a column', () => {
    const onMove = vi.fn();
    render(<Board onMove={onMove} />);
    down(handle('Write brief'), 20, 60);
    move(20, 140);
    up(20, 140);
    expect(onMove.mock.calls[0]![0]).toMatchObject({ toColumnId: 'todo', toIndex: 1 });
  });

  it('cancels when dropped outside any column, on a refusing column, or with Escape', () => {
    const onMove = vi.fn();
    render(<Board onMove={onMove} canMove={(_t, to) => to !== 'done'} />);
    down(handle('Write brief'), 20, 60);
    move(900, 60);
    up(900, 60);
    expect(live()).toMatch(/^Move cancelled/);

    down(handle('Write brief'), 20, 60);
    move(460, 60);
    expect(document.querySelector('[data-column-id="done"]')).toHaveAttribute(
      'data-drop',
      'blocked',
    );
    up(460, 60);
    expect(live()).toBe(
      'Write brief cannot move to Done. Move cancelled. Write brief returned to To do, position 1 of 3.',
    );

    down(handle('Write brief'), 20, 60);
    move(240, 60);
    fireEvent.keyDown(window, { key: 'Escape' });
    expect(titlesIn('To do')).toEqual(['Write brief', 'Draft outline', 'Book venue']);
    up(240, 60);
    expect(onMove).not.toHaveBeenCalled();
  });

  it('drags from the card body with dragFrom="card", but not from its controls or by touch', () => {
    const onMove = vi.fn();
    const onClick = vi.fn();
    render(
      <Board
        dragFrom="card"
        onMove={onMove}
        renderCard={(t) => (
          <span>
            {t.title}
            <button type="button" onClick={onClick}>
              Open {t.title}
            </button>
          </span>
        )}
      />,
    );
    down(screen.getByRole('button', { name: 'Open Write brief' }), 20, 60);
    move(240, 60);
    expect(document.querySelector('.mn-kanban-board__overlay')).toBeNull();
    up(240, 60);

    const body = screen.getByText('Draft outline', { selector: 'span' });
    down(body, 20, 110, { pointerType: 'touch' });
    move(240, 110);
    expect(document.querySelector('.mn-kanban-board__overlay')).toBeNull();
    up(240, 110);

    down(body, 20, 110);
    move(240, 110);
    up(240, 110);
    expect(onMove).toHaveBeenCalledTimes(1);
    // The click that ends a drag does not reach the card.
    fireEvent.click(screen.getByRole('button', { name: 'Open Draft outline' }));
    expect(onClick).not.toHaveBeenCalled();
  });

  it('drops into a collapsed column at its end and focuses its toggle', () => {
    const onMove = vi.fn();
    render(<Board collapsible defaultCollapsedColumnIds={['doing']} onMove={onMove} />);
    down(handle('Write brief'), 20, 60);
    move(240, 300);
    up(240, 300);
    expect(onMove.mock.calls[0]![0]).toMatchObject({ toColumnId: 'doing', toIndex: 1 });
    expect(screen.getByRole('button', { name: 'Expand In progress' })).toHaveFocus();
  });
});

describe('kanban drag helpers', () => {
  const cols = [
    { id: 'a', items: ['1', '2', '3'] },
    { id: 'b', items: ['4'] },
  ];
  const key = (s: string) => s;

  it('locates and moves cards, clamping the index and ignoring unknown targets', () => {
    expect(locate(cols, '4', key)).toEqual({ columnId: 'b', index: 0, item: '4' });
    expect(locate(cols, 'zz', key)).toBeNull();
    expect(moveCard(cols, '1', { columnId: 'b', index: 9 }, key)).toEqual([
      { id: 'a', items: ['2', '3'] },
      { id: 'b', items: ['4', '1'] },
    ]);
    expect(moveCard(cols, '3', { columnId: 'a', index: 0 }, key)[0]!.items).toEqual([
      '3',
      '1',
      '2',
    ]);
    expect(moveCard(cols, 'zz', { columnId: 'a', index: 0 }, key)).toBe(cols);
    expect(moveCard(cols, '1', { columnId: 'zz', index: 0 }, key)).toBe(cols);
    // Replaying a move on data that already has it is a no-op in effect.
    const once = moveCard(cols, '1', { columnId: 'b', index: 1 }, key);
    expect(moveCard(once, '1', { columnId: 'b', index: 1 }, key)).toEqual(once);
  });

  it('hit-tests columns and positions, and scrolls near edges', () => {
    const box = (left: number, right: number) => ({ left, right, top: 0, bottom: 10 });
    expect(
      columnAt(
        [
          { id: 'a', box: box(0, 100) },
          { id: 'b', box: box(120, 220) },
        ],
        150,
      ),
    ).toBe('b');
    expect(columnAt([{ id: 'a', box: box(0, 100) }], 110)).toBeNull();
    const cards = [0, 50, 100].map((top) => ({ left: 0, right: 10, top, bottom: top + 40 }));
    expect(indexAt(cards, 10)).toBe(0);
    expect(indexAt(cards, 75)).toBe(2);
    expect(indexAt(cards, 500)).toBe(3);
    expect(edgeSpeed(0, 500, 10)).toBeLessThan(0);
    expect(edgeSpeed(0, 500, 490)).toBeGreaterThan(0);
    expect(edgeSpeed(0, 500, 250)).toBe(0);
    expect(edgeSpeed(0, 500, 900)).toBe(0);
  });
});
