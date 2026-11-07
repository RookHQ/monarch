import { describe, expect, it, vi } from 'vitest';
import { useState } from 'react';
import { act, fireEvent, render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { BacklogPlanner, moveBacklogItem, type BacklogBucket, type BacklogMove } from './index.js';

interface Issue {
  id: string;
  title: string;
  points: number | null;
}

const BUCKETS: BacklogBucket<Issue>[] = [
  {
    id: 's1',
    title: 'Sprint 14',
    subtitle: '12 Jan – 23 Jan',
    capacity: 8,
    items: [
      { id: 'a', title: 'Fix login', points: 3 },
      { id: 'b', title: 'Export CSV', points: 5 },
    ],
  },
  { id: 's2', title: 'Sprint 15', capacity: 10, items: [], empty: 'Drag items here.' },
  {
    id: 'backlog',
    title: 'Backlog',
    items: [
      { id: 'c', title: 'Dark mode', points: 2 },
      { id: 'd', title: 'Audit log', points: null },
    ],
  },
];

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

const ids = (b: BacklogBucket<Issue>[]) =>
  b.map((x) => `${x.id}:${x.items.map((i) => i.id).join('')}`);

function Planner(props: {
  onMove?: (m: BacklogMove<Issue>, next: BacklogBucket<Issue>[]) => unknown;
  canMove?: (i: Issue, to: string) => boolean;
}) {
  const [buckets, setBuckets] = useState(BUCKETS);
  return (
    <BacklogPlanner
      aria-label="Planning"
      buckets={buckets}
      getItemId={(i) => i.id}
      getItemLabel={(i) => i.title}
      getItemPoints={(i) => i.points}
      renderItem={(i) => <span>{i.title}</span>}
      {...(props.canMove ? { canMove: (i: Issue, to: string) => props.canMove!(i, to) } : {})}
      onMove={(move, next) => {
        const r = props.onMove?.(move, next);
        if (r instanceof Promise) return r.then(() => setBuckets(next));
        setBuckets(next);
        return undefined;
      }}
    />
  );
}

const handle = (name: string) => screen.getByRole('button', { name: `Move ${name}` });
const bucket = (name: string) => screen.getByRole('region', { name });

describe('moveBacklogItem', () => {
  it('moves items between and within buckets, and ignores unknown targets', () => {
    const get = (i: Issue) => i.id;
    expect(ids(moveBacklogItem(BUCKETS, 'c', { bucketId: 's2', index: 0 }, get))).toEqual([
      's1:ab',
      's2:c',
      'backlog:d',
    ]);
    expect(ids(moveBacklogItem(BUCKETS, 'a', { bucketId: 's1', index: 1 }, get))).toEqual([
      's1:ba',
      's2:',
      'backlog:cd',
    ]);
    expect(moveBacklogItem(BUCKETS, 'zz', { bucketId: 's2', index: 0 }, get)).toBe(BUCKETS);
  });
});

describe('BacklogPlanner', () => {
  it('shows buckets with counts, points, capacity and empty text', async () => {
    render(<Planner />);
    const s1 = bucket('Sprint 14');
    expect(s1).toHaveClass('mn-backlog-planner__bucket');
    expect(s1).toHaveTextContent('2 items · 8 points');
    expect(within(s1).getByRole('meter', { name: 'Sprint 14: 8 of 8 points' })).toBeInTheDocument();
    expect(bucket('Sprint 15')).toHaveTextContent('Drag items here.');
    expect(bucket('Backlog')).toHaveTextContent('2 items · 2 points · 1 unestimated');
    expect(within(bucket('Backlog')).getByText('Not estimated')).toBeInTheDocument();
    expect(handle('Fix login')).toHaveAccessibleDescription(/Press Space or Enter to pick up/);
    await expectNoA11yViolations();
  });

  it('moves with the keyboard across buckets and saves once', async () => {
    const onMove = vi.fn();
    render(<Planner onMove={onMove} />);
    const user = userEvent.setup();
    handle('Export CSV').focus();
    await user.keyboard(' ');
    expect(handle('Export CSV')).toHaveAttribute('aria-pressed', 'true');
    expect(screen.getByRole('status')).toHaveTextContent(
      'Picked up Export CSV. Sprint 14, position 2 of 2.',
    );
    await user.keyboard('{ArrowDown}');
    expect(screen.getByRole('status')).toHaveTextContent(
      'Export CSV moved to Sprint 15, position 1 of 1.',
    );
    expect(handle('Export CSV')).toHaveFocus();
    await user.keyboard('{ArrowDown}');
    expect(screen.getByRole('status')).toHaveTextContent(
      'Export CSV moved to Backlog, position 1 of 3.',
    );
    expect(onMove).not.toHaveBeenCalled();
    await user.keyboard('{Enter}');
    expect(onMove).toHaveBeenCalledOnce();
    expect(onMove.mock.calls[0]![0]).toMatchObject({
      itemId: 'b',
      fromBucketId: 's1',
      fromIndex: 1,
      toBucketId: 'backlog',
      toIndex: 0,
    });
    expect(bucket('Sprint 14')).toHaveTextContent('1 item · 3 points');
    expect(bucket('Backlog')).toHaveTextContent('3 items · 7 points');
  });

  it('cancels with Escape and restores the order', async () => {
    const onMove = vi.fn();
    render(<Planner onMove={onMove} />);
    const user = userEvent.setup();
    handle('Fix login').focus();
    await user.keyboard(' {ArrowDown}{Escape}');
    expect(onMove).not.toHaveBeenCalled();
    expect(screen.getByRole('status')).toHaveTextContent(
      'Move cancelled. Fix login returned to where it was.',
    );
    const rows = within(bucket('Sprint 14')).getAllByRole('listitem');
    expect(rows[0]).toHaveTextContent('Fix login');
  });

  it('skips buckets refused by canMove', async () => {
    const onMove = vi.fn();
    render(<Planner onMove={onMove} canMove={(_i, to) => to !== 's2'} />);
    const user = userEvent.setup();
    handle('Export CSV').focus();
    await user.keyboard(' {ArrowDown}');
    expect(screen.getByRole('status')).toHaveTextContent('moved to Backlog');
    expect(bucket('Sprint 15')).toHaveAttribute('data-refused');
    await user.keyboard(' ');
    expect(onMove.mock.calls[0]![0]).toMatchObject({ toBucketId: 'backlog' });
  });

  it('moves with the Move to menu, disabling refused buckets', async () => {
    const onMove = vi.fn();
    render(<Planner onMove={onMove} canMove={(_i, to) => to !== 'backlog'} />);
    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: 'Move Fix login to…' }));
    expect(await screen.findByRole('menuitem', { name: 'Backlog' })).toHaveAttribute(
      'data-disabled',
    );
    await user.click(screen.getByRole('menuitem', { name: 'Sprint 15' }));
    expect(onMove.mock.calls[0]![0]).toMatchObject({ itemId: 'a', toBucketId: 's2', toIndex: 0 });
    expect(bucket('Sprint 15')).toHaveTextContent('Fix login');
  });

  it('rolls back and announces a failed save', async () => {
    let reject!: (e: Error) => void;
    render(<Planner onMove={() => new Promise((_, r) => (reject = r))} />);
    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: 'Move Dark mode to…' }));
    await user.click(await screen.findByRole('menuitem', { name: 'Sprint 15' }));
    const row = within(bucket('Sprint 15')).getByText('Dark mode').closest('li')!;
    expect(row).toHaveAttribute('aria-busy', 'true');
    await act(async () => reject(new Error('nope')));
    expect(within(bucket('Backlog')).getByText('Dark mode')).toBeInTheDocument();
    expect(screen.getByRole('status')).toHaveTextContent('Dark mode could not be moved.');
  });

  it('flags a bucket over capacity in words', () => {
    render(
      <BacklogPlanner
        buckets={[{ id: 's', title: 'Sprint', capacity: 4, items: BUCKETS[0]!.items }]}
        getItemId={(i) => i.id}
        getItemLabel={(i) => i.title}
        getItemPoints={(i) => i.points}
        renderItem={(i) => i.title}
      />,
    );
    expect(bucket('Sprint')).toHaveAttribute('data-over-capacity');
    expect(screen.getByText('Planned work is over capacity by 4 points.')).toBeInTheDocument();
    // Read-only without onMove: no handles or menus.
    expect(screen.queryByRole('button', { name: /^Move/ })).toBeNull();
  });

  it('collapses buckets', async () => {
    render(<Planner />);
    const toggle = screen.getByRole('button', { name: 'Backlog' });
    expect(toggle).toHaveAttribute('aria-expanded', 'true');
    await userEvent.click(toggle);
    expect(toggle).toHaveAttribute('aria-expanded', 'false');
    expect(within(bucket('Backlog')).queryByText('Dark mode')).toBeNull();
  });

  it('moves with a pointer drag from the handle', () => {
    const onMove = vi.fn();
    render(<Planner onMove={onMove} />);
    // jsdom has no layout: give buckets and rows rectangles.
    const rect = (top: number, height: number) =>
      ({
        top,
        bottom: top + height,
        height,
        left: 0,
        right: 300,
        width: 300,
        x: 0,
        y: top,
        toJSON() {},
      }) as DOMRect;
    const place = (el: Element, top: number, height: number) => {
      (el as HTMLElement).getBoundingClientRect = () => rect(top, height);
    };
    place(bucket('Sprint 14'), 0, 120);
    place(bucket('Sprint 15'), 140, 80);
    place(bucket('Backlog'), 240, 120);
    const rowOf = (t: string) => screen.getByText(t).closest('li')!;
    place(rowOf('Fix login'), 40, 40);
    place(rowOf('Export CSV'), 80, 40);
    place(rowOf('Dark mode'), 280, 40);
    place(rowOf('Audit log'), 320, 40);
    const grip = handle('Fix login');
    fireEvent.pointerDown(grip, { pointerId: 1, button: 0, clientX: 10, clientY: 60 });
    fireEvent.pointerMove(grip, { pointerId: 1, clientX: 10, clientY: 62 });
    expect(onMove).not.toHaveBeenCalled();
    fireEvent.pointerMove(grip, { pointerId: 1, clientX: 10, clientY: 310 });
    fireEvent.pointerUp(handle('Fix login'), { pointerId: 1, clientX: 10, clientY: 310 });
    expect(onMove).toHaveBeenCalledOnce();
    expect(onMove.mock.calls[0]![0]).toMatchObject({
      itemId: 'a',
      toBucketId: 'backlog',
      toIndex: 1,
    });

    // A second drag cancelled with Escape saves nothing.
    const grip2 = handle('Export CSV');
    fireEvent.pointerDown(grip2, { pointerId: 2, button: 0, clientX: 10, clientY: 100 });
    fireEvent.pointerMove(grip2, { pointerId: 2, clientX: 10, clientY: 170 });
    fireEvent.keyDown(window, { key: 'Escape' });
    fireEvent.pointerMove(grip2, { pointerId: 2, clientX: 10, clientY: 300 });
    fireEvent.pointerUp(grip2, { pointerId: 2, clientX: 10, clientY: 300 });
    expect(onMove).toHaveBeenCalledOnce();
    expect(screen.getByRole('status')).toHaveTextContent('Move cancelled. Export CSV returned');
  });

  it('renders on the server', () => {
    const html = renderToString(
      <BacklogPlanner
        buckets={BUCKETS}
        getItemId={(i) => i.id}
        getItemLabel={(i) => i.title}
        renderItem={(i) => i.title}
        onMove={() => {}}
      />,
    );
    expect(html).toContain('mn-backlog-planner');
  });
});
