import { describe, expect, it, vi } from 'vitest';
import { useState } from 'react';
import { act, fireEvent, render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { GanttTimeline, shiftGanttDates, type GanttDates, type GanttItem } from './index.js';
import { addDays, daysBetween, mondayOf } from './gantt-timeline.dates.js';

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

const TODAY = '2026-01-14';
const ITEMS: GanttItem[] = [
  { id: 'design', label: 'Design', start: '2026-01-12', end: '2026-01-16', progress: 60 },
  {
    id: 'build',
    label: 'Build',
    start: '2026-01-19',
    end: '2026-01-30',
    conflict: 'Starts before Design ends',
  },
  { id: 'qa', label: 'QA', parentId: 'build', start: '2026-01-26', end: '2026-01-29' },
  { id: 'launch', label: 'Launch', end: '2026-02-02' },
  { id: 'later', label: 'Research', tone: 'neutral' },
];

function Editable(props: { onChange?: (item: GanttItem, dates: GanttDates) => unknown }) {
  const [items, setItems] = useState(ITEMS);
  return (
    <GanttTimeline
      items={items}
      today={TODAY}
      locale="en-GB"
      dependencies={[{ id: 'd1', from: 'design', to: 'build' }]}
      milestones={[{ id: 'm1', label: 'Beta', date: '2026-01-23' }]}
      onChange={(item, dates) => {
        const r = props.onChange?.(item, dates);
        const apply = () =>
          setItems((all) =>
            all.map((i) => (i.id === item.id ? { ...i, start: dates.start, end: dates.end } : i)),
          );
        if (r instanceof Promise) return r.then(apply);
        apply();
        return undefined;
      }}
    />
  );
}

const bar = (name: RegExp) => screen.getByRole('button', { name });

describe('date helpers', () => {
  it('works in UTC days', () => {
    expect(addDays('2026-01-30', 3)).toBe('2026-02-02');
    expect(daysBetween('2026-01-12', '2026-02-02')).toBe(21);
    expect(mondayOf('2026-01-14')).toBe('2026-01-12');
  });

  it('shifts, resizes and never crosses edges', () => {
    const item = { start: '2026-01-12', end: '2026-01-16' };
    expect(shiftGanttDates(item, 'move', 2)).toEqual({ start: '2026-01-14', end: '2026-01-18' });
    expect(shiftGanttDates(item, 'end', -10)).toEqual({ start: '2026-01-12', end: '2026-01-12' });
    expect(shiftGanttDates(item, 'start', 10)).toEqual({ start: '2026-01-16', end: '2026-01-16' });
    expect(shiftGanttDates({ start: null, end: '2026-01-16' }, 'move', 1)).toEqual({
      start: null,
      end: '2026-01-17',
    });
  });
});

describe('GanttTimeline', () => {
  it('draws bars, a today line, milestones and dependencies with accessible names', async () => {
    const { container } = render(<Editable />);
    const root = container.querySelector('.mn-gantt-timeline')!;
    expect(root).toHaveAttribute('data-zoom', 'month');
    expect(screen.getByRole('group', { name: 'Timeline' })).toBeInTheDocument();
    expect(bar(/^Design, 12 Jan 2026 to 16 Jan 2026, 60% complete$/)).toHaveAttribute(
      'data-shape',
      'bar',
    );
    const build = bar(/^Build/);
    expect(build).toHaveAccessibleName(/Conflict: Starts before Design ends/);
    expect(build).toHaveAttribute('data-conflict');
    expect(build).toHaveAccessibleDescription(/Left and right arrows move by a day/);
    expect(bar(/^Launch, 2 Feb 2026$/)).toHaveAttribute('data-shape', 'point');
    expect(screen.getByRole('img', { name: 'Milestone Beta, 23 Jan 2026' })).toBeInTheDocument();
    expect(container.querySelector('.mn-gantt-timeline__today')).toBeInTheDocument();
    expect(container.querySelectorAll('.mn-gantt-timeline__dep')).toHaveLength(1);
    // Nested item and a collapse toggle on its parent.
    expect(screen.getByRole('button', { name: 'Collapse Build' })).toHaveAttribute(
      'aria-expanded',
      'true',
    );
    // Items without dates are listed apart.
    expect(screen.getByText('Unscheduled · 1 without dates')).toBeInTheDocument();
    await expectNoA11yViolations();
  });

  it('moves and resizes from the keyboard and announces it', async () => {
    const onChange = vi.fn();
    render(<Editable onChange={onChange} />);
    const user = userEvent.setup();
    bar(/^Design/).focus();
    await user.keyboard('{ArrowRight}');
    expect(onChange).toHaveBeenLastCalledWith(expect.objectContaining({ id: 'design' }), {
      start: '2026-01-13',
      end: '2026-01-17',
    });
    expect(screen.getByRole('status')).toHaveTextContent('Design: 13 Jan 2026 to 17 Jan 2026');
    await user.keyboard('{Shift>}{ArrowRight}{/Shift}');
    expect(onChange).toHaveBeenLastCalledWith(expect.anything(), {
      start: '2026-01-13',
      end: '2026-01-18',
    });
    await user.keyboard('{Alt>}{ArrowLeft}{/Alt}');
    expect(onChange).toHaveBeenLastCalledWith(expect.anything(), {
      start: '2026-01-12',
      end: '2026-01-18',
    });
    expect(bar(/^Design/)).toHaveFocus();
    await user.keyboard('{ArrowDown}');
    expect(bar(/^Build/)).toHaveFocus();
    await user.keyboard('{ArrowUp}');
    expect(bar(/^Design/)).toHaveFocus();
  });

  it('drags a bar and its edges with a pointer, saving once on release', () => {
    const onChange = vi.fn();
    render(<Editable onChange={onChange} />);
    const design = bar(/^Design/);
    // Month zoom: 16px a day.
    fireEvent.pointerDown(design, { button: 0, clientX: 100, pointerId: 1 });
    fireEvent.pointerMove(window, { clientX: 132, pointerId: 1 });
    expect(onChange).not.toHaveBeenCalled();
    expect(bar(/^Design, 14 Jan 2026 to 18 Jan 2026/)).toHaveAttribute('data-dragging');
    fireEvent.pointerUp(window, { clientX: 132, pointerId: 1 });
    expect(onChange).toHaveBeenCalledOnce();
    expect(onChange).toHaveBeenLastCalledWith(expect.objectContaining({ id: 'design' }), {
      start: '2026-01-14',
      end: '2026-01-18',
    });
    const endHandle = bar(/^Design/).querySelector('[data-edge="end"]')!;
    fireEvent.pointerDown(endHandle, { button: 0, clientX: 200, pointerId: 2 });
    fireEvent.pointerMove(window, { clientX: 248, pointerId: 2 });
    fireEvent.pointerUp(window, { clientX: 248, pointerId: 2 });
    expect(onChange).toHaveBeenLastCalledWith(expect.anything(), {
      start: '2026-01-14',
      end: '2026-01-21',
    });
    // Escape cancels a drag.
    fireEvent.pointerDown(bar(/^Build/), { button: 0, clientX: 100, pointerId: 3 });
    fireEvent.pointerMove(window, { clientX: 180, pointerId: 3 });
    fireEvent.keyDown(window, { key: 'Escape' });
    fireEvent.pointerUp(window, { clientX: 180, pointerId: 3 });
    expect(onChange).toHaveBeenCalledTimes(2);
  });

  it('puts a bar back and announces a failed save', async () => {
    let reject!: (e: Error) => void;
    render(<Editable onChange={() => new Promise((_, r) => (reject = r))} />);
    const user = userEvent.setup();
    bar(/^Design/).focus();
    await user.keyboard('{ArrowRight}');
    expect(bar(/^Design, 13 Jan/)).toHaveAttribute('aria-busy', 'true');
    await act(async () => reject(new Error('locked')));
    expect(bar(/^Design, 12 Jan/)).not.toHaveAttribute('aria-busy');
    expect(screen.getByRole('status')).toHaveTextContent('Design could not be changed.');
  });

  it('opens items on click and Enter, and is read-only without onChange', async () => {
    const onOpen = vi.fn();
    render(<GanttTimeline items={ITEMS} today={TODAY} onOpen={onOpen} locale="en-GB" />);
    const user = userEvent.setup();
    expect(bar(/^Design/)).not.toHaveAttribute('data-editable');
    expect(bar(/^Design/)).not.toHaveAccessibleDescription();
    await user.click(bar(/^Design/));
    expect(onOpen).toHaveBeenCalledWith(ITEMS[0]);
    bar(/^Build/).focus();
    await user.keyboard('{ArrowRight}{Enter}');
    expect(onOpen).toHaveBeenLastCalledWith(ITEMS[1]);
    expect(screen.queryByText(/Drag a bar/)).toBeNull();
  });

  it('zooms, navigates and collapses', async () => {
    const onZoomChange = vi.fn();
    const { container } = render(
      <GanttTimeline items={ITEMS} today={TODAY} locale="en-GB" onZoomChange={onZoomChange} />,
    );
    const user = userEvent.setup();
    const zoom = screen.getByRole('radiogroup', { name: 'Zoom' });
    await user.click(within(zoom).getByRole('radio', { name: 'Quarters' }));
    expect(onZoomChange).toHaveBeenCalledWith('quarter');
    expect(container.querySelector('.mn-gantt-timeline')).toHaveAttribute('data-zoom', 'quarter');
    const range = container.querySelector('.mn-gantt-timeline__range')!;
    const before = range.textContent;
    await user.click(screen.getByRole('button', { name: 'Later' }));
    expect(range.textContent).not.toBe(before);
    await user.click(screen.getByRole('button', { name: 'Today' }));
    expect(range.textContent).toBe(before);
    await user.click(screen.getByRole('button', { name: 'Collapse Build' }));
    expect(screen.queryByRole('button', { name: /^QA/ })).toBeNull();
  });

  it('offers a table view that lists and edits dates', async () => {
    const onChange = vi.fn();
    render(<Editable onChange={onChange} />);
    const user = userEvent.setup();
    await user.click(screen.getByRole('radio', { name: 'Table' }));
    const table = screen.getByRole('table', { name: 'Timeline' });
    const design = within(table).getByRole('rowheader', { name: 'Design' }).closest('tr')!;
    expect(design).toHaveTextContent('5 days');
    expect(design).toHaveTextContent('60%');
    expect(within(table).getByRole('rowheader', { name: /Build/ }).closest('tr')).toHaveTextContent(
      'Design',
    );
    expect(within(table).getByText('Milestone: Beta')).toBeInTheDocument();
    expect(
      within(table).getByRole('rowheader', { name: 'Research' }).closest('tr'),
    ).toHaveTextContent('No dates');
    const end = within(table).getByLabelText('End of Design');
    fireEvent.change(end, { target: { value: '2026-01-20' } });
    fireEvent.blur(end);
    expect(onChange).toHaveBeenLastCalledWith(expect.objectContaining({ id: 'design' }), {
      start: '2026-01-12',
      end: '2026-01-20',
    });
    await expectNoA11yViolations();
  });

  it('shows groups, row meta and off-screen items', () => {
    render(
      <GanttTimeline
        items={[
          {
            id: 'a',
            label: 'Website',
            groupId: 'g',
            start: '2026-01-05',
            end: '2026-02-20',
            meta: '3/8',
          },
          { id: 'b', label: 'Old project', groupId: 'g', start: '2025-01-05', end: '2025-02-20' },
          { id: 'c', label: 'Undated', groupId: 'g' },
        ]}
        groups={[
          { id: 'g', label: 'Growth', start: '2026-01-05', end: '2026-03-01', meta: '3 projects' },
        ]}
        undated="row"
        today={TODAY}
        locale="en-GB"
        toolbar={false}
      />,
    );
    expect(screen.getByText('Growth')).toBeInTheDocument();
    expect(screen.getByText('3 projects')).toBeInTheDocument();
    expect(screen.getByText('3/8')).toBeInTheDocument();
    expect(screen.getByText('Ended 20 Feb 2025')).toBeInTheDocument();
    expect(screen.getByText('No dates')).toBeInTheDocument();
    expect(screen.queryByRole('radiogroup')).toBeNull();
  });

  it('renders on the server without a clock when today is passed', () => {
    const html = renderToString(<GanttTimeline items={ITEMS} today={TODAY} />);
    expect(html).toContain('mn-gantt-timeline');
    expect(html).toContain('mn-gantt-timeline__today');
    const noToday = renderToString(<GanttTimeline items={ITEMS} />);
    expect(noToday).not.toContain('mn-gantt-timeline__today"');
  });
});
