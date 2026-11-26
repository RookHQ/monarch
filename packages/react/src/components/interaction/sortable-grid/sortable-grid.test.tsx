import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { useState } from 'react';
import { fireEvent, render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { SortableGrid, type SortableGridProps } from './index.js';

interface Tile {
  id: string;
  name: string;
  locked?: boolean;
}
const TILES: Tile[] = ['A', 'B', 'C', 'D', 'E', 'F'].map((n) => ({ id: n.toLowerCase(), name: n }));

const base = {
  'aria-label': 'Shortcuts',
  getKey: (t: Tile) => t.id,
  getLabel: (t: Tile) => t.name,
  renderItem: (t: Tile) => <span>{t.name}</span>,
} satisfies Partial<SortableGridProps<Tile>>;

const order = () =>
  within(screen.getByRole('list', { name: 'Shortcuts' }))
    .getAllByRole('listitem')
    .map((li) => li.textContent);
const handle = (name: string) => screen.getByRole('button', { name: `Reorder ${name}` });
const live = () => document.querySelector('[aria-live="assertive"]');

if (typeof window.PointerEvent === 'undefined') {
  class PointerEventPolyfill extends MouseEvent {
    pointerId: number;
    constructor(type: string, init: PointerEventInit = {}) {
      super(type, init);
      this.pointerId = init.pointerId ?? 0;
    }
  }
  (window as unknown as { PointerEvent: unknown }).PointerEvent = PointerEventPolyfill;
}

/** jsdom has no layout: 3 columns of 100×100 slots by DOM position. */
function stubLayout() {
  const pos = (el: HTMLElement) =>
    el.tagName === 'LI' && el.parentElement ? [...el.parentElement.children].indexOf(el) : -1;
  const props = {
    offsetLeft: (el: HTMLElement) => (pos(el) < 0 ? 0 : (pos(el) % 3) * 100),
    offsetTop: (el: HTMLElement) => (pos(el) < 0 ? 0 : Math.floor(pos(el) / 3) * 100),
    offsetWidth: (el: HTMLElement) => (el.tagName === 'LI' ? 90 : 0),
    offsetHeight: (el: HTMLElement) => (el.tagName === 'LI' ? 90 : 0),
  };
  for (const [name, get] of Object.entries(props))
    Object.defineProperty(HTMLElement.prototype, name, {
      configurable: true,
      get(this: HTMLElement) {
        return get(this);
      },
    });
  return () => {
    const proto = HTMLElement.prototype as unknown as Record<string, unknown>;
    for (const name of Object.keys(props)) Reflect.deleteProperty(proto, name);
  };
}

let restore: () => void;
beforeEach(() => {
  restore = stubLayout();
});
afterEach(() => restore());

describe('SortableGrid', () => {
  it('renders a named list of tiles with labelled, described handles', async () => {
    render(<SortableGrid {...base} defaultItems={TILES} />);
    expect(order()).toEqual(['A', 'B', 'C', 'D', 'E', 'F']);
    const h = handle('B');
    expect(h).toHaveAccessibleDescription(/Press Space or Enter to pick up/);
    expect(h).toHaveAttribute('aria-pressed', 'false');
    expect(document.querySelector('.mn-sortable-grid__list')).toHaveAttribute(
      'data-layout',
      'auto',
    );
    await expectNoA11yViolations();
  });

  it('moves in two dimensions with the keyboard, announcing each step', async () => {
    const user = userEvent.setup();
    const onReorder = vi.fn();
    render(<SortableGrid {...base} defaultItems={TILES} onReorder={onReorder} />);
    handle('A').focus();
    await user.keyboard(' ');
    expect(handle('A')).toHaveAttribute('aria-pressed', 'true');
    expect(live()).toHaveTextContent('Picked up A. Position 1 of 6.');
    await user.keyboard('{ArrowRight}');
    expect(order()).toEqual(['B', 'A', 'C', 'D', 'E', 'F']);
    expect(handle('A')).toHaveFocus();
    expect(live()).toHaveTextContent('A moved to position 2 of 6.');
    await user.keyboard('{ArrowDown}');
    expect(order()).toEqual(['B', 'C', 'D', 'E', 'A', 'F']);
    expect(handle('A')).toHaveFocus();
    await user.keyboard('{ArrowDown}');
    // No row below: stays.
    expect(order()).toEqual(['B', 'C', 'D', 'E', 'A', 'F']);
    expect(onReorder).not.toHaveBeenCalled();
    await user.keyboard(' ');
    expect(onReorder).toHaveBeenCalledTimes(1);
    expect(onReorder.mock.calls[0]![1]).toEqual({ item: TILES[0], from: 0, to: 4 });
    expect(live()).toHaveTextContent('A dropped at position 5 of 6.');
  });

  it('Escape cancels and restores; Home/End; arrows do nothing before pick-up', async () => {
    const user = userEvent.setup();
    const onReorder = vi.fn();
    render(<SortableGrid {...base} defaultItems={TILES} onReorder={onReorder} columns={3} />);
    handle('C').focus();
    await user.keyboard('{ArrowLeft}');
    expect(order()).toEqual(['A', 'B', 'C', 'D', 'E', 'F']);
    await user.keyboard('{Enter}{End}');
    expect(order()).toEqual(['A', 'B', 'D', 'E', 'F', 'C']);
    await user.keyboard('{Home}');
    expect(order()).toEqual(['C', 'A', 'B', 'D', 'E', 'F']);
    await user.keyboard('{Escape}');
    expect(order()).toEqual(['A', 'B', 'C', 'D', 'E', 'F']);
    expect(onReorder).not.toHaveBeenCalled();
    expect(live()).toHaveTextContent('Reorder cancelled. C returned to position 3 of 6.');
  });

  it('uses the columns prop for rows and swaps Left/Right in RTL', async () => {
    const user = userEvent.setup();
    render(
      <div dir="rtl" style={{ direction: 'rtl' }}>
        <SortableGrid {...base} defaultItems={TILES} columns={2} />
      </div>,
    );
    expect(document.querySelector('.mn-sortable-grid__list')).toHaveAttribute(
      'data-layout',
      'columns',
    );
    handle('A').focus();
    await user.keyboard(' {ArrowDown}');
    expect(order()).toEqual(['B', 'C', 'A', 'D', 'E', 'F']);
    await user.keyboard('{ArrowLeft}');
    expect(order()).toEqual(['B', 'C', 'D', 'A', 'E', 'F']);
  });

  it('glides displaced tiles from their old place (FLIP) when the order changes', async () => {
    const user = userEvent.setup();
    render(<SortableGrid {...base} defaultItems={TILES} />);
    const seen: string[] = [];
    const observer = new MutationObserver((records) => {
      for (const r of records) seen.push(r.oldValue ?? '');
    });
    observer.observe(screen.getByRole('list', { name: 'Shortcuts' }), {
      subtree: true,
      attributes: true,
      attributeFilter: ['style'],
      attributeOldValue: true,
    });
    handle('A').focus();
    await user.keyboard(' {ArrowRight}');
    seen.push(...observer.takeRecords().map((r) => r.oldValue ?? ''));
    observer.disconnect();
    // A moved one slot right and B one slot left: each starts from where it was.
    expect(seen.some((v) => v.includes('translate(-100px, 0px)'))).toBe(true);
    expect(seen.some((v) => v.includes('translate(100px, 0px)'))).toBe(true);
  });

  it('keeps a tile that arrives while one is held: the drop commits the current tiles', async () => {
    const user = userEvent.setup();
    const onReorder = vi.fn();
    const { rerender } = render(<SortableGrid {...base} items={TILES} onReorder={onReorder} />);
    handle('A').focus();
    await user.keyboard(' {ArrowRight}');
    rerender(
      <SortableGrid {...base} items={[...TILES, { id: 'g', name: 'G' }]} onReorder={onReorder} />,
    );
    expect(order()).toEqual(['B', 'A', 'C', 'D', 'E', 'F', 'G']);
    await user.keyboard(' ');
    expect(onReorder.mock.calls[0]?.[0].map((t: Tile) => t.id)).toEqual([
      'b',
      'a',
      'c',
      'd',
      'e',
      'f',
      'g',
    ]);
  });

  it('Tab away cancels a keyboard move', async () => {
    const user = userEvent.setup();
    render(
      <>
        <SortableGrid {...base} defaultItems={TILES} />
        <button type="button">After</button>
      </>,
    );
    handle('F').focus();
    await user.keyboard(' {ArrowLeft}');
    expect(order()).toEqual(['A', 'B', 'C', 'D', 'F', 'E']);
    await user.tab();
    expect(order()).toEqual(['A', 'B', 'C', 'D', 'E', 'F']);
  });

  it('controlled: follows items and snaps back when the parent ignores onReorder', async () => {
    const user = userEvent.setup();
    function Controlled() {
      const [items, setItems] = useState(TILES);
      return <SortableGrid {...base} items={items} onReorder={setItems} />;
    }
    const { unmount } = render(<Controlled />);
    handle('A').focus();
    await user.keyboard(' {ArrowRight} ');
    expect(order()).toEqual(['B', 'A', 'C', 'D', 'E', 'F']);
    unmount();
    render(<SortableGrid {...base} items={TILES} />);
    handle('A').focus();
    await user.keyboard(' {ArrowRight} ');
    expect(order()).toEqual(['A', 'B', 'C', 'D', 'E', 'F']);
  });

  it('locks single items or the whole grid', async () => {
    const user = userEvent.setup();
    const tiles = TILES.map((t) => (t.id === 'b' ? { ...t, locked: true } : t));
    const { rerender } = render(
      <SortableGrid {...base} defaultItems={tiles} isItemDisabled={(t) => t.locked === true} />,
    );
    expect(handle('B')).toBeDisabled();
    handle('A').focus();
    await user.keyboard(' {ArrowRight}{ArrowRight} ');
    expect(order()).toEqual(['B', 'C', 'A', 'D', 'E', 'F']);
    rerender(<SortableGrid {...base} defaultItems={tiles} disabled />);
    expect(handle('C')).toBeDisabled();
  });

  it('moves by pointer to the nearest slot in 2D after a threshold', () => {
    const onReorder = vi.fn();
    render(<SortableGrid {...base} defaultItems={TILES} onReorder={onReorder} />);
    const h = handle('A');
    fireEvent.pointerDown(h, { pointerId: 1, button: 0, clientX: 45, clientY: 45 });
    fireEvent.pointerMove(h, { pointerId: 1, clientX: 47, clientY: 46 });
    expect(document.querySelector('.mn-sortable-grid')).not.toHaveAttribute('data-dragging');
    // Centre of A (45,45) moves to near slot 4 (145,145).
    fireEvent.pointerMove(h, { pointerId: 1, clientX: 150, clientY: 140 });
    expect(document.querySelector('.mn-sortable-grid')).toHaveAttribute('data-dragging', 'pointer');
    expect(order()).toEqual(['B', 'C', 'D', 'E', 'A', 'F']);
    expect(screen.getAllByRole('listitem')[4]).toHaveAttribute('data-active', 'pointer');
    fireEvent.pointerUp(h, { pointerId: 1, clientX: 150, clientY: 140 });
    expect(onReorder).toHaveBeenCalledTimes(1);
    expect(onReorder.mock.calls[0]![1]).toMatchObject({ from: 0, to: 4 });
  });

  it('pointercancel cancels a pointer drag', () => {
    const onReorder = vi.fn();
    render(<SortableGrid {...base} defaultItems={TILES} onReorder={onReorder} />);
    const h = handle('A');
    fireEvent.pointerDown(h, { pointerId: 1, button: 0, clientX: 45, clientY: 45 });
    fireEvent.pointerMove(h, { pointerId: 1, clientX: 250, clientY: 45 });
    expect(order()).toEqual(['B', 'C', 'A', 'D', 'E', 'F']);
    fireEvent.pointerCancel(h, { pointerId: 1 });
    expect(order()).toEqual(['A', 'B', 'C', 'D', 'E', 'F']);
    expect(onReorder).not.toHaveBeenCalled();
  });

  it('translates messages and sets the minimum width variable', () => {
    render(
      <SortableGrid
        {...base}
        defaultItems={TILES}
        minItemWidth={160}
        messages={{ handleLabel: (l) => `Mover ${l}` }}
      />,
    );
    expect(screen.getByRole('button', { name: 'Mover A' })).toBeInTheDocument();
    expect(
      (document.querySelector('.mn-sortable-grid__list') as HTMLElement).style.getPropertyValue(
        '--mn-sortable-grid-min',
      ),
    ).toBe('160px');
  });

  it('calls a callback ref once, not again on every render', () => {
    const ref = vi.fn();
    const { rerender } = render(<SortableGrid {...base} ref={ref} defaultItems={TILES} />);
    rerender(<SortableGrid {...base} ref={ref} defaultItems={TILES} disabled />);
    expect(ref).toHaveBeenCalledTimes(1);
    expect(ref).toHaveBeenLastCalledWith(expect.any(HTMLUListElement));
  });

  it('renders on the server', () => {
    const html = renderToString(<SortableGrid {...base} items={TILES} />);
    expect(html).toContain('mn-sortable-grid');
    expect(html).toContain('Reorder A');
  });
});
