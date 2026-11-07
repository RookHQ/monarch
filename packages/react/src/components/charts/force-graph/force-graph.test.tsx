import { describe, expect, it, vi } from 'vitest';
import { fireEvent, render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { ForceGraph, forceLayout, type ForceGraphProps } from './index.js';

if (typeof window.PointerEvent === 'undefined') {
  class PointerEventStub extends MouseEvent {
    pointerType: string;
    pointerId: number;
    constructor(type: string, init: PointerEventInit = {}) {
      super(type, init);
      this.pointerType = init.pointerType ?? 'mouse';
      this.pointerId = init.pointerId ?? 1;
    }
  }
  window.PointerEvent = PointerEventStub as unknown as typeof PointerEvent;
}

const NODES = [
  { id: 'a', label: 'Handbook', group: 'ops', description: 'Operations space' },
  { id: 'b', label: 'Onboarding', group: 'ops' },
  { id: 'c', label: 'Security policy', group: 'eng' },
  { id: 'd', label: 'Release notes', group: 'eng' },
  { id: 'e', label: 'Archive' },
];
const EDGES = [
  { source: 'a', target: 'b', kind: 'parent' },
  { source: 'a', target: 'c', kind: 'related' },
  { source: 'c', target: 'd' },
  { source: 'd', target: 'zzz' },
];
const GROUPS = [
  { key: 'ops', label: 'Operations' },
  { key: 'eng', label: 'Engineering' },
];
const KINDS = [
  { key: 'parent', label: 'Parent of', color: 1 as const },
  { key: 'related', label: 'Related', color: 3 as const, dashed: true },
];

function graph(props: Partial<ForceGraphProps> = {}) {
  return (
    <ForceGraph
      title="Documents"
      nodes={NODES}
      edges={EDGES}
      groups={GROUPS}
      edgeKinds={KINDS}
      {...props}
    />
  );
}

const canvas = () => screen.getByRole('group', { name: /5 nodes and 3 links/ });
const live = () => document.querySelector('[role="status"]')!;

describe('forceLayout', () => {
  it('is deterministic, separates nodes and keeps linked nodes closer', () => {
    const nodes = NODES.map((n) => ({ id: n.id }));
    const one = forceLayout(nodes, EDGES);
    const two = forceLayout(nodes, EDGES);
    expect([...one.entries()]).toEqual([...two.entries()]);
    const d = (x: string, y: string) =>
      Math.hypot(one.get(x)!.x - one.get(y)!.x, one.get(x)!.y - one.get(y)!.y);
    for (const x of ['a', 'b', 'c', 'd', 'e'])
      for (const y of ['a', 'b', 'c', 'd', 'e']) if (x < y) expect(d(x, y)).toBeGreaterThan(20);
    expect(d('a', 'b')).toBeLessThan(d('b', 'd'));
  });

  it('keeps pinned nodes in place', () => {
    const out = forceLayout(
      [{ id: 'a', fx: 10, fy: 20 }, { id: 'b' }],
      [{ source: 'a', target: 'b' }],
    );
    expect(out.get('a')).toEqual({ x: 10, y: 20 });
  });
});

describe('ForceGraph', () => {
  it('draws nodes, valid links, group and link-kind legends and a summary', async () => {
    const { container } = render(graph());
    expect(container.querySelectorAll('.mn-force-graph__node')).toHaveLength(5);
    expect(container.querySelectorAll('.mn-force-graph__edge')).toHaveLength(3);
    expect(canvas()).toHaveAccessibleName(
      '5 nodes and 3 links in 2 groups. Most connected: Handbook (2).',
    );
    expect(container.querySelector('[data-node="e"]')).toHaveAttribute('data-orphan');
    expect(container.querySelector('[data-node="c"]')).toHaveStyle({
      '--_c': 'var(--mn-color-data-2)',
    });
    expect(container.querySelector('[data-kind="related"]')).toHaveAttribute('data-dashed');
    expect(screen.getByRole('list', { name: 'Legend' })).toHaveTextContent('OperationsEngineering');
    expect(screen.getByRole('list', { name: 'Link types' })).toHaveTextContent('Parent ofRelated');
    expect(screen.getByRole('button', { name: 'Zoom in' })).toBeInTheDocument();
    await expectNoA11yViolations();
  });

  it('lists every node with its group and connections in the table', () => {
    render(graph());
    const table = screen.getByRole('table');
    const row = within(table).getByRole('rowheader', { name: 'Handbook' }).closest('tr')!;
    expect(row).toHaveTextContent('Operations');
    expect(row).toHaveTextContent('Onboarding (Parent of), Security policy (Related)');
    expect(
      within(table).getByRole('rowheader', { name: 'Archive' }).closest('tr'),
    ).toHaveTextContent('No group');
  });

  it('moves between nodes with the arrow keys and selects one', async () => {
    const onNodeSelect = vi.fn();
    render(graph({ onNodeSelect }));
    const user = userEvent.setup();
    for (let i = 0; i < 8 && document.activeElement !== canvas(); i++) await user.tab();
    expect(canvas()).toHaveFocus();
    // Keyboard focus starts at the first node by name.
    expect(live()).toHaveTextContent('Archive, No group. 0 connections');
    await user.keyboard('{End}');
    expect(live()).toHaveTextContent(
      'Security policy, Engineering. 2 connections: Handbook (Related), Release notes',
    );
    await user.keyboard('{Home}');
    const seen = new Set<string>();
    for (const key of ['{ArrowRight}', '{ArrowDown}', '{ArrowLeft}', '{ArrowUp}', '{ArrowRight}']) {
      await user.keyboard(key);
      seen.add(live().textContent ?? '');
    }
    expect(seen.size).toBeGreaterThan(1);
    await user.keyboard('{Enter}');
    expect(onNodeSelect).toHaveBeenCalledTimes(1);
    const announced = live().textContent ?? '';
    expect(announced.startsWith(onNodeSelect.mock.calls[0]![0].label)).toBe(true);
    await user.keyboard('{Escape}');
    expect(live()).toHaveTextContent('');
    await expectNoA11yViolations();
  });

  it('zooms with the buttons and keys, and resets', async () => {
    const { container } = render(graph());
    const user = userEvent.setup();
    const pos = () => container.querySelector('[data-node="a"]')!.getAttribute('transform');
    const before = pos();
    await user.click(screen.getByRole('button', { name: 'Zoom in' }));
    expect(pos()).not.toBe(before);
    await user.click(screen.getByRole('button', { name: 'Reset view' }));
    expect(pos()).toBe(before);
    canvas().focus();
    await user.keyboard('-');
    expect(pos()).not.toBe(before);
    await user.keyboard('0');
    expect(pos()).toBe(before);
    for (let i = 0; i < 8; i++) await user.click(screen.getByRole('button', { name: 'Zoom out' }));
    expect(screen.getByRole('button', { name: 'Zoom out' })).toBeDisabled();
  });

  it('highlights a hovered node and its neighbours, and selects on click', () => {
    const onNodeSelect = vi.fn();
    const { container } = render(graph({ onNodeSelect, selectedId: 'b' }));
    const node = container.querySelector('[data-node="a"] .mn-force-graph__dot')!;
    fireEvent.pointerMove(node, { pointerType: 'mouse' });
    expect(container.querySelector('.mn-chart__tooltip')).toHaveTextContent('Handbook');
    expect(container.querySelector('.mn-chart__tooltip')).toHaveTextContent('Operations space');
    expect(container.querySelector('[data-node="d"]')).toHaveAttribute('data-dimmed');
    expect(container.querySelector('[data-node="b"]')).not.toHaveAttribute('data-dimmed');
    expect(container.querySelector('[data-node="b"]')).toHaveAttribute('data-selected');
    expect(container.querySelectorAll('.mn-force-graph__edge[data-highlight]')).toHaveLength(2);
    fireEvent.click(node);
    expect(onNodeSelect).toHaveBeenCalledWith(NODES[0]);
  });

  it('drags a node without selecting it, and pans the background', () => {
    const onNodeSelect = vi.fn();
    const { container } = render(graph({ onNodeSelect }));
    const dot = container.querySelector('[data-node="b"] .mn-force-graph__dot')!;
    const pos = (id: string) =>
      container.querySelector(`[data-node="${id}"]`)!.getAttribute('transform');
    const b0 = pos('b');
    const a0 = pos('a');
    fireEvent.pointerDown(dot, { clientX: 100, clientY: 100, pointerId: 1 });
    fireEvent.pointerMove(dot, { clientX: 160, clientY: 130, pointerId: 1 });
    fireEvent.pointerUp(dot, { clientX: 160, clientY: 130, pointerId: 1 });
    fireEvent.click(dot);
    expect(onNodeSelect).not.toHaveBeenCalled();
    expect(pos('b')).not.toBe(b0);
    expect(pos('a')).toBe(a0);
    const svg = container.querySelector('svg.mn-force-graph__svg')!;
    fireEvent.pointerDown(svg, { clientX: 10, clientY: 10, pointerId: 2 });
    fireEvent.pointerMove(svg, { clientX: 40, clientY: 10, pointerId: 2 });
    fireEvent.pointerUp(svg, { clientX: 40, clientY: 10, pointerId: 2 });
    expect(pos('a')).not.toBe(a0);
  });

  it('derives groups from the nodes and accepts translated words', () => {
    render(
      graph({
        groups: undefined,
        labels: { summary: (n, e) => `${n} documents, ${e} liens`, zoomIn: 'Agrandir' },
      }),
    );
    expect(screen.getByRole('group', { name: '5 documents, 3 liens' })).toBeInTheDocument();
    expect(screen.getByRole('list', { name: 'Legend' })).toHaveTextContent('opseng');
    expect(screen.getByRole('button', { name: 'Agrandir' })).toBeInTheDocument();
  });

  it('shows the empty state without nodes', () => {
    const { container } = render(graph({ nodes: [], edges: [] }));
    expect(container.querySelector('figure')).toHaveAttribute('data-status', 'empty');
  });

  it('renders on the server, identically to the first client render', () => {
    const html = renderToString(graph());
    expect(html).toContain('mn-force-graph');
    expect(renderToString(graph())).toBe(html);
  });
});
