import { describe, expect, it, vi } from 'vitest';
import { useState } from 'react';
import { act, render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { hydrateRoot } from 'react-dom/client';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { TimeOffPanel, type TimeOffEntry, type TimeOffPanelProps } from './index.js';
import { hydrationErrors } from '../../../../test/hydration.js';

const NOW = new Date('2026-11-04T12:00:00Z');
const ENTRIES: TimeOffEntry[] = [
  {
    id: 'a',
    personId: 'p1',
    personName: 'Alex Kim',
    allDay: true,
    start: '2026-11-09',
    end: '2026-11-13',
    type: 'Annual leave',
    reason: 'Family visit',
  },
  { id: 'b', personId: null, allDay: true, start: '2026-12-25', end: '2026-12-25' },
  {
    id: 'c',
    personId: 'p2',
    personName: 'Sam Lee',
    allDay: false,
    start: '2026-11-05T09:00:00Z',
    end: '2026-11-05T13:00:00Z',
    status: 'Pending',
  },
  {
    id: 'old',
    personId: 'p2',
    personName: 'Sam Lee',
    allDay: true,
    start: '2026-10-01',
    end: '2026-10-02',
  },
];

function Panel(props: Partial<TimeOffPanelProps>) {
  return (
    <TimeOffPanel
      entries={ENTRIES}
      timeZone="UTC"
      locale="en-GB"
      hourCycle="24"
      now={NOW}
      people={[
        { value: 'p1', label: 'Alex Kim' },
        { value: 'p2', label: 'Sam Lee' },
      ]}
      {...props}
    />
  );
}

describe('TimeOffPanel', () => {
  it('lists upcoming time off with badges and no axe violations', async () => {
    render(<Panel onRemove={() => {}} onAdd={() => {}} />);
    const region = screen.getByRole('region', { name: 'Time off' });
    const items = within(region).getAllByRole('listitem');
    expect(items).toHaveLength(3);
    expect(items[0]).toHaveTextContent('Alex KimAnnual leave');
    expect(items[0]).toHaveTextContent(
      'Mon, 9 Nov 2026 – Fri, 13 Nov 2026 (all day) · Family visit',
    );
    expect(items[1]).toHaveTextContent('EveryoneClosure');
    expect(items[2]).toHaveTextContent('Thu, 5 Nov 2026, 09:00 – 13:00');
    expect(items[2]).toHaveTextContent('Pending');
    await expectNoA11yViolations();
  });

  it('shows past entries when asked', async () => {
    const onShowPastChange = vi.fn();
    render(<Panel onShowPastChange={onShowPastChange} />);
    await userEvent.setup().click(screen.getByRole('checkbox', { name: 'Include past' }));
    expect(onShowPastChange).toHaveBeenCalledWith(true);
    expect(screen.getAllByRole('listitem')).toHaveLength(4);
    expect(screen.getAllByRole('listitem')[3]).toHaveTextContent('Past');
  });

  it('removes after a danger confirmation', async () => {
    const onRemove = vi.fn();
    render(<Panel onRemove={onRemove} canRemove={(e) => e.personId !== null} />);
    const user = userEvent.setup();
    expect(screen.getAllByRole('button', { name: /Remove time off/ })).toHaveLength(2);
    await user.click(screen.getByRole('button', { name: /Remove time off for Alex Kim/ }));
    const dialog = await screen.findByRole('alertdialog', { name: 'Remove this time off?' });
    expect(dialog).toHaveTextContent('Alex Kim · Mon, 9 Nov 2026');
    await user.click(within(dialog).getByRole('button', { name: 'Remove' }));
    expect(onRemove).toHaveBeenCalledWith(ENTRIES[0]);
  });

  it('adds all-day time off through the dialog', async () => {
    const onAdd = vi.fn();
    render(
      <Panel onAdd={onAdd} allowClosure types={[{ value: 'annual', label: 'Annual leave' }]} />,
    );
    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: 'Add time off' }));
    const dialog = await screen.findByRole('dialog', { name: 'Add time off' });
    await expectNoA11yViolations(dialog);
    await user.selectOptions(within(dialog).getByRole('combobox', { name: 'Who' }), 'Everyone');
    expect(within(dialog).getByText('A closure blocks bookings for everyone.')).toBeInTheDocument();
    await user.type(within(dialog).getByRole('textbox', { name: /Reason/ }), 'Office move');
    await user.click(within(dialog).getByRole('button', { name: 'Add time off' }));
    expect(onAdd).toHaveBeenCalledWith({
      personId: null,
      allDay: true,
      start: '2026-11-04',
      end: '2026-11-04',
      type: 'annual',
      reason: 'Office move',
    });
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
  });

  it('adds timed time off converted from the zone, and keeps the form on failure', async () => {
    const onAdd = vi.fn().mockRejectedValueOnce(new Error('Overlaps existing time off.'));
    render(<Panel onAdd={onAdd} timeZone="Asia/Kolkata" />);
    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: 'Add time off' }));
    const dialog = await screen.findByRole('dialog');
    await user.click(within(dialog).getByRole('checkbox', { name: 'All day' }));
    await user.click(within(dialog).getByRole('button', { name: 'Add time off' }));
    expect(onAdd).toHaveBeenCalledWith({
      personId: 'p1',
      allDay: false,
      start: '2026-11-04T03:30:00.000Z',
      end: '2026-11-04T11:30:00.000Z',
    });
    expect(await within(dialog).findByText('Overlaps existing time off.')).toBeInTheDocument();
    expect(screen.getByRole('dialog')).toBeInTheDocument();
  });

  it('shows loading, error with retry, and empty states', async () => {
    const onRetry = vi.fn();
    const { rerender } = render(<Panel loading />);
    expect(screen.getByRole('region')).toHaveAttribute('aria-busy', 'true');
    rerender(<Panel error="Could not load time off." onRetry={onRetry} />);
    await userEvent.setup().click(screen.getByRole('button', { name: 'Try again' }));
    expect(onRetry).toHaveBeenCalled();
    rerender(<Panel entries={[]} />);
    expect(screen.getByText('No time off planned')).toBeInTheDocument();
  });

  it('shows unreadable dates as given instead of throwing', () => {
    render(
      <Panel
        entries={[
          {
            id: 'x',
            personId: 'p1',
            personName: 'Alex Kim',
            allDay: true,
            start: 'soon',
            end: 'later',
          },
          {
            id: 'y',
            personId: 'p2',
            personName: 'Sam Lee',
            allDay: false,
            start: 'nope',
            end: 'nope',
          },
        ]}
      />,
    );
    expect(screen.getByText('soon – later (all day)')).toBeInTheDocument();
    expect(screen.getByText('nope')).toBeInTheDocument();
  });

  it('moves focus to a neighbouring entry when one is removed', async () => {
    function Removable() {
      const [entries, setEntries] = useState(ENTRIES);
      return (
        <Panel
          entries={entries}
          onRemove={(entry) => setEntries((list) => list.filter((e) => e.id !== entry.id))}
        />
      );
    }
    render(<Removable />);
    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: /Remove time off for Sam Lee/ }));
    const dialog = await screen.findByRole('alertdialog', { name: 'Remove this time off?' });
    await user.click(within(dialog).getByRole('button', { name: 'Remove' }));
    await waitFor(() => expect(screen.queryByRole('alertdialog')).not.toBeInTheDocument());
    // Sam Lee was the last row: focus lands on the row that is now last.
    await waitFor(() =>
      expect(screen.getByRole('button', { name: /Remove time off for Everyone/ })).toHaveFocus(),
    );
  });

  it('hydrates without a mismatch when the server and browser locales differ', async () => {
    let machineLocale = 'en-US';
    const original = Intl.DateTimeFormat.prototype.resolvedOptions;
    const spy = vi
      .spyOn(Intl.DateTimeFormat.prototype, 'resolvedOptions')
      .mockImplementation(function (this: Intl.DateTimeFormat) {
        return { ...original.call(this), locale: machineLocale };
      });
    const error = vi.spyOn(console, 'error').mockImplementation(() => {});
    const ui = <Panel locale={undefined} hourCycle={undefined} />;
    const container = document.createElement('div');
    try {
      const html = renderToString(ui);
      expect(html).toContain('Mon, Nov 9, 2026');
      machineLocale = 'en-GB';
      container.innerHTML = html;
      document.body.append(container);
      let root: ReturnType<typeof hydrateRoot> | undefined;
      await act(async () => {
        root = hydrateRoot(container, ui);
      });
      expect(hydrationErrors(error)).toEqual([]);
      expect(container.innerHTML).toContain('Mon, 9 Nov 2026');
      act(() => root?.unmount());
    } finally {
      container.remove();
      spy.mockRestore();
      error.mockRestore();
    }
  });

  it('renders on the server', () => {
    expect(renderToString(<Panel />)).toContain('mn-time-off-panel');
  });
});
