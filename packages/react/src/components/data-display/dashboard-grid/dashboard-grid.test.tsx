import { describe, expect, it, vi } from 'vitest';
import { act, fireEvent, render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { createRef, useState } from 'react';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { Widget } from '../widget/index.js';
import { DashboardGrid, resolveDashboardLayout, type DashboardGridItem } from './index.js';

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

const items: DashboardGridItem[] = [
  {
    id: 'kpis',
    label: 'Key figures',
    span: 'full',
    content: <Widget title="Key figures">a</Widget>,
  },
  { id: 'load', label: 'Team load', content: <Widget title="Team load">b</Widget> },
  { id: 'csat', label: 'Satisfaction', span: 2, content: <Widget title="Satisfaction">c</Widget> },
];

function Controlled({ onLayoutChange }: { onLayoutChange?: (l: string[]) => void }) {
  const [layout, setLayout] = useState<string[]>(['kpis', 'load', 'csat']);
  return (
    <DashboardGrid
      items={items}
      layout={layout}
      editing
      onLayoutChange={(next) => {
        setLayout(next);
        onLayoutChange?.(next);
      }}
    />
  );
}

describe('DashboardGrid', () => {
  it('lays out children as given, forwarding props and ref', async () => {
    const ref = createRef<HTMLDivElement>();
    render(
      <DashboardGrid ref={ref} data-testid="root" className="extra" aria-label="Overview">
        <Widget title="One">1</Widget>
        <Widget title="Two" span={2}>
          2
        </Widget>
      </DashboardGrid>,
    );
    const root = screen.getByTestId('root');
    expect(root).toHaveClass('mn-dashboard-grid', 'extra');
    expect(ref.current).toBe(root);
    expect(root).toHaveAttribute('data-columns', '3');
    expect(screen.getByRole('group', { name: 'Overview' })).toBeInTheDocument();
    expect(screen.getByRole('region', { name: 'Two' })).toHaveAttribute('data-span', '2');
    await expectNoA11yViolations();
  });

  it('renders items as a named list in layout order, clamping spans to the columns', async () => {
    render(<DashboardGrid items={items} layout={['csat', 'kpis']} columns={2} />);
    const list = screen.getByRole('list', { name: 'Dashboard' });
    const cells = within(list).getAllByRole('listitem');
    expect(cells.map((c) => c.textContent?.slice(-1))).toEqual(['c', 'a', 'b']);
    expect(cells[0]).toHaveAttribute('data-span', 'full');
    expect(cells[2]).not.toHaveAttribute('data-span');
    expect(screen.queryByRole('button')).not.toBeInTheDocument();
    await expectNoA11yViolations();
  });

  it('resolves stored layouts against the current items', () => {
    expect(resolveDashboardLayout(items, ['gone', 'csat', 'csat'])).toEqual([
      'csat',
      'kpis',
      'load',
    ]);
    expect(resolveDashboardLayout(items, undefined)).toEqual(['kpis', 'load', 'csat']);
  });

  it('shows handles and move buttons in arrange mode', async () => {
    render(<DashboardGrid items={items} editing />);
    const handle = screen.getByRole('button', { name: 'Move Team load' });
    expect(handle).toHaveAccessibleDescription(/Press Space or Enter to pick up/);
    expect(screen.getByRole('button', { name: 'Move Key figures earlier' })).toBeDisabled();
    expect(screen.getByRole('button', { name: 'Move Satisfaction later' })).toBeDisabled();
    await expectNoA11yViolations();
  });

  it('moves a widget with the keyboard, announces each step and reports the layout on drop', async () => {
    const onLayoutChange = vi.fn();
    const user = userEvent.setup();
    render(<Controlled onLayoutChange={onLayoutChange} />);
    const handle = screen.getByRole('button', { name: 'Move Team load' });
    handle.focus();
    await user.keyboard(' ');
    expect(handle).toHaveAttribute('aria-pressed', 'true');
    expect(screen.getByRole('status')).toHaveTextContent('Picked up Team load. Position 2 of 3.');
    await user.keyboard('{ArrowUp}');
    expect(screen.getByRole('status')).toHaveTextContent('Team load moved to position 1 of 3.');
    expect(screen.getByRole('button', { name: 'Move Team load' })).toHaveFocus();
    expect(onLayoutChange).not.toHaveBeenCalled();
    await user.keyboard('{Enter}');
    expect(onLayoutChange).toHaveBeenCalledWith(['load', 'kpis', 'csat']);
    expect(screen.getByRole('status')).toHaveTextContent('Team load dropped at position 1 of 3.');
    expect(within(screen.getByRole('list')).getAllByRole('listitem')[0]).toHaveTextContent('b');
  });

  it('cancels a keyboard move with Escape and restores the order', async () => {
    const onLayoutChange = vi.fn();
    const user = userEvent.setup();
    render(<Controlled onLayoutChange={onLayoutChange} />);
    screen.getByRole('button', { name: 'Move Key figures' }).focus();
    await user.keyboard('{Enter}{End}');
    expect(within(screen.getByRole('list')).getAllByRole('listitem')[2]).toHaveTextContent('a');
    await user.keyboard('{Escape}');
    expect(onLayoutChange).not.toHaveBeenCalled();
    expect(within(screen.getByRole('list')).getAllByRole('listitem')[0]).toHaveTextContent('a');
    expect(screen.getByRole('status')).toHaveTextContent(
      'Move cancelled. Key figures returned to position 1 of 3.',
    );
  });

  it('moves with the single-click buttons and keeps focus on them', async () => {
    const onLayoutChange = vi.fn();
    const user = userEvent.setup();
    render(<Controlled onLayoutChange={onLayoutChange} />);
    await user.click(screen.getByRole('button', { name: 'Move Key figures later' }));
    expect(onLayoutChange).toHaveBeenLastCalledWith(['load', 'kpis', 'csat']);
    expect(screen.getByRole('button', { name: 'Move Key figures later' })).toHaveFocus();
    await user.click(screen.getByRole('button', { name: 'Move Key figures later' }));
    expect(onLayoutChange).toHaveBeenLastCalledWith(['load', 'csat', 'kpis']);
    // At the end the button is disabled, so focus moves to the handle.
    expect(screen.getByRole('button', { name: 'Move Key figures' })).toHaveFocus();
  });

  it('works uncontrolled from defaultLayout', async () => {
    const user = userEvent.setup();
    render(<DashboardGrid items={items} defaultLayout={['csat']} editing />);
    await user.click(screen.getByRole('button', { name: 'Move Satisfaction later' }));
    const cells = within(screen.getByRole('list')).getAllByRole('listitem');
    expect(cells.map((c) => c.textContent?.slice(-1))).toEqual(['a', 'c', 'b']);
  });

  it('reorders by pointer drag over another widget', () => {
    const onLayoutChange = vi.fn();
    render(<Controlled onLayoutChange={onLayoutChange} />);
    const cells = within(screen.getByRole('list')).getAllByRole('listitem');
    cells.forEach((cell, i) => {
      cell.getBoundingClientRect = () =>
        ({
          left: 0,
          right: 100,
          top: i * 100,
          bottom: i * 100 + 99,
          width: 100,
          height: 99,
        }) as DOMRect;
    });
    const handle = screen.getByRole('button', { name: 'Move Satisfaction' });
    act(() => {
      fireEvent.pointerDown(handle, { button: 0, pointerId: 1, clientX: 50, clientY: 250 });
    });
    act(() => {
      fireEvent.pointerMove(handle, { pointerId: 1, clientX: 50, clientY: 40 });
    });
    act(() => {
      fireEvent.pointerUp(handle, { pointerId: 1, clientX: 50, clientY: 40 });
    });
    expect(onLayoutChange).toHaveBeenCalledWith(['csat', 'kpis', 'load']);
  });

  it('treats a press on the handle without movement as no move', () => {
    const onLayoutChange = vi.fn();
    render(<Controlled onLayoutChange={onLayoutChange} />);
    const handle = screen.getByRole('button', { name: 'Move Team load' });
    fireEvent.pointerDown(handle, { button: 0, pointerId: 1, clientX: 5, clientY: 5 });
    fireEvent.pointerUp(handle, { pointerId: 1, clientX: 6, clientY: 5 });
    expect(onLayoutChange).not.toHaveBeenCalled();
  });

  it('translates through messages', () => {
    render(
      <DashboardGrid
        items={items}
        editing
        messages={{ label: 'Tablero', handleLabel: (l) => `Mover ${l}` }}
      />,
    );
    expect(screen.getByRole('list', { name: 'Tablero' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Mover Team load' })).toBeInTheDocument();
  });

  it('renders on the server', () => {
    const html = renderToString(<DashboardGrid items={items} editing />);
    expect(html).toContain('mn-dashboard-grid');
    expect(html).toContain('Move Team load');
  });
});
