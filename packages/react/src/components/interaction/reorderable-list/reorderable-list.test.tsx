import { describe, expect, it, vi } from 'vitest';
import { useState } from 'react';
import { fireEvent, render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { ReorderableList, moveItem, type ReorderableListProps } from './index.js';

interface Stage {
  id: string;
  name: string;
  locked?: boolean;
}
const STAGES: Stage[] = [
  { id: 'new', name: 'New' },
  { id: 'qualified', name: 'Qualified' },
  { id: 'proposal', name: 'Proposal' },
  { id: 'won', name: 'Won' },
];

const base = {
  'aria-label': 'Pipeline stages',
  getKey: (s: Stage) => s.id,
  getLabel: (s: Stage) => s.name,
  renderItem: (s: Stage) => <span>{s.name}</span>,
} satisfies Partial<ReorderableListProps<Stage>>;

const order = () =>
  within(screen.getByRole('list', { name: 'Pipeline stages' }))
    .getAllByRole('listitem')
    .map((li) => li.textContent);
const handle = (name: string) => screen.getByRole('button', { name: `Reorder ${name}` });
const live = () => document.querySelector('[aria-live="assertive"]');

// jsdom has no PointerEvent: a MouseEvent with a pointerId is enough here.
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

function stubLayout() {
  // jsdom has no layout: give every row a 40px slot by its DOM position.
  Object.defineProperty(HTMLElement.prototype, 'offsetTop', {
    configurable: true,
    get(this: HTMLElement) {
      if (this.tagName !== 'LI' || !this.parentElement) return 0;
      return [...this.parentElement.children].indexOf(this) * 40;
    },
  });
  Object.defineProperty(HTMLElement.prototype, 'offsetHeight', {
    configurable: true,
    get(this: HTMLElement) {
      return this.tagName === 'LI' ? 36 : 0;
    },
  });
  return () => {
    delete (HTMLElement.prototype as { offsetTop?: number }).offsetTop;
    delete (HTMLElement.prototype as { offsetHeight?: number }).offsetHeight;
  };
}

describe('ReorderableList', () => {
  it('renders a named list with labelled, described handles', async () => {
    render(<ReorderableList {...base} defaultItems={STAGES} />);
    expect(order()).toEqual(['New', 'Qualified', 'Proposal', 'Won']);
    const h = handle('Qualified');
    expect(h).toHaveAccessibleDescription(/Press Space or Enter to pick up/);
    expect(h).toHaveAttribute('aria-pressed', 'false');
    await expectNoA11yViolations();
  });

  it('moves with the keyboard: Space grabs, arrows move, Space drops, announcing each step', async () => {
    const user = userEvent.setup();
    const onReorder = vi.fn();
    render(<ReorderableList {...base} defaultItems={STAGES} onReorder={onReorder} />);
    handle('New').focus();
    await user.keyboard(' ');
    expect(handle('New')).toHaveAttribute('aria-pressed', 'true');
    expect(live()).toHaveTextContent('Picked up New. Position 1 of 4.');
    await user.keyboard('{ArrowDown}');
    expect(order()).toEqual(['Qualified', 'New', 'Proposal', 'Won']);
    expect(handle('New')).toHaveFocus();
    expect(live()).toHaveTextContent('New moved to position 2 of 4.');
    await user.keyboard('{ArrowDown}');
    expect(live()).toHaveTextContent('New moved to position 3 of 4.');
    // Nothing is committed until the drop.
    expect(onReorder).not.toHaveBeenCalled();
    await user.keyboard(' ');
    expect(live()).toHaveTextContent('New dropped at position 3 of 4.');
    expect(onReorder).toHaveBeenCalledTimes(1);
    expect(onReorder.mock.calls[0]?.[0].map((s: Stage) => s.id)).toEqual([
      'qualified',
      'proposal',
      'new',
      'won',
    ]);
    expect(onReorder.mock.calls[0]?.[1]).toEqual({ item: STAGES[0], from: 0, to: 2 });
    expect(order()).toEqual(['Qualified', 'Proposal', 'New', 'Won']);
    expect(handle('New')).toHaveFocus();
    expect(handle('New')).toHaveAttribute('aria-pressed', 'false');
  });

  it('Escape cancels and restores the original order', async () => {
    const user = userEvent.setup();
    const onReorder = vi.fn();
    render(<ReorderableList {...base} defaultItems={STAGES} onReorder={onReorder} />);
    handle('Proposal').focus();
    await user.keyboard('{Enter}{ArrowUp}{ArrowUp}');
    expect(order()).toEqual(['Proposal', 'New', 'Qualified', 'Won']);
    await user.keyboard('{Escape}');
    expect(order()).toEqual(['New', 'Qualified', 'Proposal', 'Won']);
    expect(live()).toHaveTextContent('Reorder cancelled. Proposal returned to position 3 of 4.');
    expect(onReorder).not.toHaveBeenCalled();
    expect(handle('Proposal')).toHaveFocus();
  });

  it('Home and End move to the ends; moves stop at the edges', async () => {
    const user = userEvent.setup();
    render(<ReorderableList {...base} defaultItems={STAGES} />);
    handle('Qualified').focus();
    await user.keyboard(' {End}');
    expect(order()).toEqual(['New', 'Proposal', 'Won', 'Qualified']);
    await user.keyboard('{ArrowDown}');
    expect(order()).toEqual(['New', 'Proposal', 'Won', 'Qualified']);
    expect(live()).toHaveTextContent('Qualified moved to position 4 of 4.');
    await user.keyboard('{Home} ');
    expect(order()).toEqual(['Qualified', 'New', 'Proposal', 'Won']);
  });

  it('arrow keys do nothing until an item is picked up, and Tab away cancels', async () => {
    const user = userEvent.setup();
    render(
      <>
        <ReorderableList {...base} defaultItems={STAGES} />
        <button type="button">After</button>
      </>,
    );
    handle('New').focus();
    await user.keyboard('{ArrowDown}');
    expect(order()).toEqual(['New', 'Qualified', 'Proposal', 'Won']);
    await user.keyboard(' {ArrowDown}');
    expect(order()).toEqual(['Qualified', 'New', 'Proposal', 'Won']);
    await user.click(screen.getByRole('button', { name: 'After' }));
    expect(order()).toEqual(['New', 'Qualified', 'Proposal', 'Won']);
    expect(live()).toHaveTextContent('Reorder cancelled.');
  });

  it('works controlled: the order is whatever the parent passes back', async () => {
    const user = userEvent.setup();
    function Controlled() {
      const [items, setItems] = useState(STAGES);
      return <ReorderableList {...base} items={items} onReorder={setItems} />;
    }
    render(<Controlled />);
    handle('Won').focus();
    await user.keyboard(' {ArrowUp} ');
    expect(order()).toEqual(['New', 'Qualified', 'Won', 'Proposal']);
    // A controlled list that ignores onReorder snaps back.
    const { unmount } = render(
      <ReorderableList {...base} aria-label="Fixed" items={STAGES} onReorder={() => {}} />,
    );
    const fixed = within(screen.getByRole('list', { name: 'Fixed' }));
    fixed.getByRole('button', { name: 'Reorder New' }).focus();
    await user.keyboard(' {ArrowDown} ');
    expect(fixed.getAllByRole('listitem').map((li) => li.textContent)).toEqual([
      'New',
      'Qualified',
      'Proposal',
      'Won',
    ]);
    unmount();
  });

  it('offers move buttons, disabled at the ends, that keep focus on the moved item', async () => {
    const user = userEvent.setup();
    const onReorder = vi.fn();
    render(<ReorderableList {...base} defaultItems={STAGES} moveButtons onReorder={onReorder} />);
    expect(screen.getByRole('button', { name: 'Move New up' })).toBeDisabled();
    expect(screen.getByRole('button', { name: 'Move Won down' })).toBeDisabled();
    await user.click(screen.getByRole('button', { name: 'Move Qualified up' }));
    expect(order()).toEqual(['Qualified', 'New', 'Proposal', 'Won']);
    expect(live()).toHaveTextContent('Qualified moved to position 1 of 4.');
    // Up is now disabled for Qualified, so focus lands on its handle.
    expect(handle('Qualified')).toHaveFocus();
    await user.click(screen.getByRole('button', { name: 'Move New down' }));
    expect(screen.getByRole('button', { name: 'Move New down' })).toHaveFocus();
    expect(onReorder).toHaveBeenCalledTimes(2);
    await expectNoA11yViolations();
  });

  it('locks disabled items and the whole list', async () => {
    const user = userEvent.setup();
    const items = [...STAGES.slice(0, 3), { id: 'won', name: 'Won', locked: true }];
    const { rerender } = render(
      <ReorderableList
        {...base}
        defaultItems={items}
        moveButtons
        isItemDisabled={(s) => !!s.locked}
      />,
    );
    expect(handle('Won')).toBeDisabled();
    expect(screen.getByRole('button', { name: 'Move Won up' })).toBeDisabled();
    // Others can still move past a locked item.
    handle('Proposal').focus();
    await user.keyboard(' {ArrowDown} ');
    expect(order()).toEqual(['New', 'Qualified', 'Won', 'Proposal']);
    rerender(<ReorderableList {...base} defaultItems={items} disabled />);
    expect(handle('New')).toBeDisabled();
  });

  it('drags with a pointer (mouse, pen or touch) after a small threshold', () => {
    const restore = stubLayout();
    try {
      const onReorder = vi.fn();
      render(<ReorderableList {...base} defaultItems={STAGES} onReorder={onReorder} />);
      const h = handle('New');
      fireEvent.pointerDown(h, { pointerId: 1, button: 0, clientX: 10, clientY: 18 });
      // A press without movement is not a drag.
      fireEvent.pointerMove(h, { pointerId: 1, clientX: 10, clientY: 20 });
      expect(live()?.textContent).toBe('');
      // Past the threshold and beyond the next two rows' centres.
      fireEvent.pointerMove(h, { pointerId: 1, clientX: 10, clientY: 30 });
      expect(live()).toHaveTextContent('Picked up New. Position 1 of 4.');
      fireEvent.pointerMove(h, { pointerId: 1, clientX: 10, clientY: 105 });
      expect(order()).toEqual(['Qualified', 'Proposal', 'New', 'Won']);
      expect(screen.getAllByRole('listitem')[2]).toHaveAttribute('data-active', 'pointer');
      fireEvent.pointerUp(h, { pointerId: 1, clientX: 10, clientY: 105 });
      expect(onReorder).toHaveBeenCalledWith(expect.any(Array), {
        item: STAGES[0],
        from: 0,
        to: 2,
      });
      expect(live()).toHaveTextContent('New dropped at position 3 of 4.');
    } finally {
      restore();
    }
  });

  it('cancels a pointer drag on pointercancel or Escape', () => {
    const restore = stubLayout();
    try {
      const onReorder = vi.fn();
      render(<ReorderableList {...base} defaultItems={STAGES} onReorder={onReorder} />);
      const h = handle('Won');
      fireEvent.pointerDown(h, { pointerId: 2, button: 0, clientX: 0, clientY: 138 });
      fireEvent.pointerMove(h, { pointerId: 2, clientX: 0, clientY: 10 });
      expect(order()).toEqual(['Won', 'New', 'Qualified', 'Proposal']);
      fireEvent.pointerCancel(h, { pointerId: 2 });
      expect(order()).toEqual(['New', 'Qualified', 'Proposal', 'Won']);
      fireEvent.pointerDown(h, { pointerId: 3, button: 0, clientX: 0, clientY: 138 });
      fireEvent.pointerMove(h, { pointerId: 3, clientX: 0, clientY: 10 });
      fireEvent.keyDown(h, { key: 'Escape' });
      expect(order()).toEqual(['New', 'Qualified', 'Proposal', 'Won']);
      expect(onReorder).not.toHaveBeenCalled();
    } finally {
      restore();
    }
  });

  it('accepts translated messages', async () => {
    const user = userEvent.setup();
    render(
      <ReorderableList
        {...base}
        defaultItems={STAGES}
        messages={{
          handleLabel: (l) => `Déplacer ${l}`,
          pickedUp: (l, p, n) => `${l} saisi, ${p} sur ${n}.`,
        }}
      />,
    );
    screen.getByRole('button', { name: 'Déplacer New' }).focus();
    await user.keyboard(' ');
    expect(live()).toHaveTextContent('New saisi, 1 sur 4.');
  });

  it('renders on the server', () => {
    const html = renderToString(<ReorderableList {...base} items={STAGES} moveButtons />);
    expect(html).toContain('mn-reorderable-list');
    expect(html).toContain('Reorder Qualified');
  });
});

describe('moveItem', () => {
  it('moves an item, clamping the target and copying the array', () => {
    const list = ['a', 'b', 'c'];
    expect(moveItem(list, 0, 2)).toEqual(['b', 'c', 'a']);
    expect(moveItem(list, 2, -5)).toEqual(['c', 'a', 'b']);
    expect(moveItem(list, 9, 0)).toEqual(['a', 'b', 'c']);
    expect(list).toEqual(['a', 'b', 'c']);
  });
});
