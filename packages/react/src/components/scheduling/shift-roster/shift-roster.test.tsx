import { describe, expect, it, vi } from 'vitest';
import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import {
  ShiftRoster,
  type ShiftRosterMember,
  type ShiftRosterProps,
  type ShiftRosterShift,
  type ShiftRosterValue,
} from './index.js';

const SHIFTS: ShiftRosterShift[] = [
  { id: 'm', name: 'Morning', start: 360, end: 840, color: '#2f7d6d' },
  { id: 'n', name: 'Night', shortName: 'N', start: 1320, end: 360 },
];
const MEMBERS: ShiftRosterMember[] = [
  { id: 'a', name: 'Alex Kim', description: 'Support' },
  { id: 'b', name: 'Sam Lee' },
];
const VALUE: ShiftRosterValue = {
  a: { '2026-11-02': 'm', '2026-11-03': 'n', '2026-11-04': null },
  b: { '2026-11-02': 'm' },
};

function Roster(props: Partial<ShiftRosterProps>) {
  return (
    <ShiftRoster
      members={MEMBERS}
      shifts={SHIFTS}
      from="2026-11-02"
      defaultValue={VALUE}
      today="2026-11-03"
      locale="en-GB"
      hourCycle="24"
      {...props}
    />
  );
}

describe('ShiftRoster', () => {
  it('renders a named grid with shifts, off days, totals and no axe violations', async () => {
    render(<Roster />);
    const grid = screen.getByRole('grid', { name: 'Shift roster' });
    expect(within(grid).getAllByRole('columnheader')).toHaveLength(9);
    expect(
      screen.getByRole('button', { name: 'Alex Kim, Monday 2 November: Morning 06:00–14:00' }),
    ).toBeInTheDocument();
    expect(
      screen.getByRole('button', {
        name: 'Alex Kim, Tuesday 3 November: Night 22:00–06:00, overnight',
      }),
    ).toHaveTextContent('N22:00–06:00');
    expect(
      screen.getByRole('button', { name: 'Alex Kim, Wednesday 4 November: Off' }),
    ).toBeInTheDocument();
    expect(
      screen.getByRole('button', { name: 'Sam Lee, Tuesday 3 November: Not assigned' }),
    ).toBeInTheDocument();
    const alexRow = screen.getByRole('rowheader', { name: /Alex Kim/ }).closest('tr')!;
    expect(alexRow).toHaveTextContent('16 h');
    const foot = grid.querySelector('tfoot')!;
    expect(foot).toHaveTextContent('Working21');
    await expectNoA11yViolations();
  });

  it('assigns a shift from the cell menu and reports the change', async () => {
    const onValueChange = vi.fn();
    render(<Roster onValueChange={onValueChange} />);
    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: /Sam Lee, Tuesday 3 November/ }));
    const menu = await screen.findByRole('menu');
    expect(within(menu).getByText('Sam Lee, Tuesday 3 November')).toBeInTheDocument();
    await user.click(within(menu).getByRole('menuitemradio', { name: /Night/ }));
    expect(onValueChange).toHaveBeenCalledWith(
      { ...VALUE, b: { '2026-11-02': 'm', '2026-11-03': 'n' } },
      { memberId: 'b', day: '2026-11-03', shiftId: 'n' },
    );
    expect(
      screen.getByRole('button', { name: /Sam Lee, Tuesday 3 November: Night/ }),
    ).toBeInTheDocument();
  });

  it('sets a day off and clears an assignment', async () => {
    const onValueChange = vi.fn();
    render(<Roster onValueChange={onValueChange} />);
    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: /Sam Lee, Monday 2 November/ }));
    await user.click(await screen.findByRole('menuitemradio', { name: 'Off' }));
    expect(onValueChange).toHaveBeenLastCalledWith(expect.anything(), {
      memberId: 'b',
      day: '2026-11-02',
      shiftId: null,
    });
    await user.click(screen.getByRole('button', { name: /Sam Lee, Monday 2 November: Off/ }));
    await user.click(await screen.findByRole('menuitem', { name: 'Clear' }));
    expect(onValueChange).toHaveBeenLastCalledWith(expect.anything(), {
      memberId: 'b',
      day: '2026-11-02',
      shiftId: undefined,
    });
  });

  it('is one tab stop; arrows move without opening the menu; Delete clears', async () => {
    const onValueChange = vi.fn();
    const { container } = render(<Roster onValueChange={onValueChange} />);
    const user = userEvent.setup();
    expect(container.querySelectorAll('[data-cell][tabindex="0"]')).toHaveLength(1);
    container.querySelector<HTMLElement>('[data-cell="0:0"]')!.focus();
    await user.keyboard('{ArrowDown}');
    expect(container.querySelector('[data-cell="1:0"]')).toHaveFocus();
    expect(screen.queryByRole('menu')).not.toBeInTheDocument();
    await user.keyboard('{End}');
    expect(container.querySelector('[data-cell="1:6"]')).toHaveFocus();
    await user.keyboard('{Control>}{Home}{/Control}{Delete}');
    expect(onValueChange).toHaveBeenCalledWith(expect.anything(), {
      memberId: 'a',
      day: '2026-11-02',
      shiftId: undefined,
    });
    await user.keyboard('{Enter}');
    expect(await screen.findByRole('menu')).toBeInTheDocument();
  });

  it('is view-only with readOnly and supports member clicks', async () => {
    const onMemberClick = vi.fn();
    const { container } = render(
      <Roster readOnly onMemberClick={onMemberClick} days={3} showTotals={false} />,
    );
    expect(screen.queryByRole('button', { name: /Monday 2 November/ })).not.toBeInTheDocument();
    expect(container.querySelector('[data-cell="0:0"]')).toHaveTextContent('Morning 06:00–14:00');
    expect(screen.getAllByRole('columnheader')).toHaveLength(4);
    await userEvent.setup().click(screen.getByRole('button', { name: /Alex Kim/ }));
    expect(onMemberClick).toHaveBeenCalledWith(MEMBERS[0]);
    await expectNoA11yViolations();
  });

  it('shows loading, error and empty states', () => {
    const { rerender, container } = render(<Roster loading />);
    expect(container.firstElementChild).toHaveAttribute('aria-busy', 'true');
    rerender(<Roster error="Could not load the roster." />);
    expect(screen.getByText('Could not load the roster.')).toBeInTheDocument();
    rerender(<Roster members={[]} />);
    expect(screen.getByText('No one to schedule')).toBeInTheDocument();
  });

  it('renders on the server', () => {
    expect(renderToString(<Roster />)).toContain('mn-shift-roster');
  });
});
