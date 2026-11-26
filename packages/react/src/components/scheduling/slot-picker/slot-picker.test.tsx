import { describe, expect, it, vi } from 'vitest';
import { useState } from 'react';
import { act, render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { hydrateRoot } from 'react-dom/client';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { SlotPicker, type SlotPickerProps, type SlotPickerSlot } from './index.js';
import { hydrationErrors } from '../../../../test/hydration.js';

const SLOTS: SlotPickerSlot[] = [
  { start: '2026-11-04T09:00:00Z' },
  { start: '2026-11-04T09:30:00Z', seatsLeft: 2 },
  { start: '2026-11-04T10:00:00Z', disabled: true },
  { start: '2026-11-04T10:30:00Z' },
];

function Picker(props: Partial<SlotPickerProps>) {
  return (
    <SlotPicker
      slots={SLOTS}
      defaultDay="2026-11-04"
      today="2026-11-02"
      timeZone="UTC"
      locale="en-GB"
      hourCycle="24"
      {...props}
    />
  );
}

describe('SlotPicker', () => {
  it('shows the day, its times in the zone and seats left, with no axe violations', async () => {
    render(<Picker />);
    expect(screen.getByRole('heading', { name: 'Wednesday 4 November' })).toBeInTheDocument();
    const group = screen.getByRole('group', { name: 'Available times on Wednesday 4 November' });
    expect(group).toBeInTheDocument();
    expect(screen.getByRole('button', { name: '09:30 2 left' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: '10:00' })).toBeDisabled();
    expect(screen.getByRole('status')).toHaveTextContent('3 times on Wednesday 4 November');
    expect(screen.getByText(/Times shown in/)).toBeInTheDocument();
    await expectNoA11yViolations();
  });

  it('formats times in the given zone', () => {
    render(<Picker timeZone="Asia/Kolkata" />);
    expect(screen.getByRole('button', { name: '14:30' })).toBeInTheDocument();
  });

  it('selects a time on click and reports the slot', async () => {
    const onValueChange = vi.fn();
    render(<Picker onValueChange={onValueChange} />);
    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: '09:00' }));
    expect(onValueChange).toHaveBeenCalledWith('2026-11-04T09:00:00Z', SLOTS[0]);
    expect(screen.getByRole('button', { name: '09:00' })).toHaveAttribute('aria-pressed', 'true');
  });

  it('is one tab stop; arrows skip disabled times; Enter selects', async () => {
    const onValueChange = vi.fn();
    render(<Picker onValueChange={onValueChange} />);
    const user = userEvent.setup();
    const buttons = screen.getAllByRole('button', { pressed: false });
    expect(buttons.filter((b) => b.tabIndex === 0)).toHaveLength(1);
    screen.getByRole('button', { name: '09:00' }).focus();
    await user.keyboard('{ArrowRight}');
    expect(screen.getByRole('button', { name: '09:30 2 left' })).toHaveFocus();
    await user.keyboard('{ArrowRight}');
    expect(screen.getByRole('button', { name: '10:30' })).toHaveFocus();
    await user.keyboard('{Home}');
    expect(screen.getByRole('button', { name: '09:00' })).toHaveFocus();
    await user.keyboard('{End}{Enter}');
    expect(onValueChange).toHaveBeenLastCalledWith('2026-11-04T10:30:00Z', SLOTS[3]);
  });

  it('moves Up and Down by one row of the laid-out grid', async () => {
    const { container } = render(<Picker />);
    // Two columns: slots 0 and 1 on the first row, 2 and 3 on the second.
    container.querySelectorAll<HTMLElement>('[data-slot]').forEach((b, i) => {
      vi.spyOn(b, 'getBoundingClientRect').mockReturnValue({
        top: Math.floor(i / 2) * 40,
        left: (i % 2) * 90,
        width: 80,
        height: 32,
      } as DOMRect);
    });
    const user = userEvent.setup();
    screen.getByRole('button', { name: '09:30 2 left' }).focus();
    await user.keyboard('{ArrowDown}');
    expect(screen.getByRole('button', { name: '10:30' })).toHaveFocus();
    await user.keyboard('{ArrowUp}');
    expect(screen.getByRole('button', { name: '09:30 2 left' })).toHaveFocus();
    // Below 09:00 is the disabled 10:00 and nothing further: focus stays.
    screen.getByRole('button', { name: '09:00' }).focus();
    await user.keyboard('{ArrowDown}');
    expect(screen.getByRole('button', { name: '09:00' })).toHaveFocus();
  });

  it('mirrors Left and Right in RTL', async () => {
    render(
      <div dir="rtl">
        <Picker />
      </div>,
    );
    const user = userEvent.setup();
    screen.getByRole('button', { name: '09:00' }).focus();
    await user.keyboard('{ArrowLeft}');
    expect(screen.getByRole('button', { name: '09:30 2 left' })).toHaveFocus();
    await user.keyboard('{ArrowRight}');
    expect(screen.getByRole('button', { name: '09:00' })).toHaveFocus();
  });

  it('clears on a second press only with allowDeselect', async () => {
    const onValueChange = vi.fn();
    render(<Picker defaultValue={SLOTS[0]!.start} allowDeselect onValueChange={onValueChange} />);
    await userEvent.setup().click(screen.getByRole('button', { name: '09:00' }));
    expect(onValueChange).toHaveBeenCalledWith(null, null);
  });

  it('asks for a day first and reports the day picked', async () => {
    const onDayChange = vi.fn();
    render(<Picker defaultDay={null} defaultMonth="2026-11-01" onDayChange={onDayChange} />);
    expect(screen.getByText('Choose a day to see available times.')).toBeInTheDocument();
    await userEvent
      .setup()
      .click(screen.getByRole('button', { name: /^Thursday,? 5 November 2026/ }));
    expect(onDayChange).toHaveBeenCalledWith('2026-11-05');
  });

  it('disables days outside availableDays', () => {
    render(<Picker availableDays={['2026-11-04', '2026-11-06']} />);
    expect(screen.getByRole('button', { name: /^Thursday,? 5 November 2026/ })).toHaveAttribute(
      'aria-disabled',
      'true',
    );
  });

  it('shows loading, empty, error and off-list states', async () => {
    const onRetry = vi.fn();
    const onChooseOutside = vi.fn();
    const { rerender } = render(<Picker loading />);
    expect(screen.getByRole('status')).toHaveTextContent('Loading available times');
    expect(screen.queryByRole('group', { name: /Available times/ })).not.toBeInTheDocument();

    rerender(<Picker slots={[]} onChooseOutside={onChooseOutside} />);
    expect(screen.getByText(/No times are available/)).toBeInTheDocument();
    await userEvent.setup().click(screen.getByRole('button', { name: 'Choose another time' }));
    expect(onChooseOutside).toHaveBeenCalled();

    rerender(<Picker error="Could not load times." onRetry={onRetry} />);
    expect(screen.getByText('Could not load times.')).toBeInTheDocument();
    await userEvent.setup().click(screen.getByRole('button', { name: 'Try again' }));
    expect(onRetry).toHaveBeenCalled();

    rerender(<Picker value="2026-11-04T13:00:00Z" />);
    expect(screen.getByText('13:00 is not an open time on this day.')).toBeInTheDocument();
  });

  it('offers a zone picker and the business-zone note', async () => {
    function Zoned() {
      const [zone, setZone] = useState('UTC');
      return <Picker timeZone={zone} onTimeZoneChange={setZone} businessTimeZone="Asia/Kolkata" />;
    }
    render(<Zoned />);
    expect(screen.getByRole('combobox', { name: 'Time zone' })).toBeInTheDocument();
    expect(screen.getByText(/Times are converted from/)).toBeInTheDocument();
    await expectNoA11yViolations();
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
    const ui = <Picker locale={undefined} hourCycle={undefined} weekStartsOn={1} />;
    const container = document.createElement('div');
    try {
      const html = renderToString(ui);
      expect(html).toContain('Wednesday, November 4');
      machineLocale = 'en-GB';
      container.innerHTML = html;
      document.body.append(container);
      let root: ReturnType<typeof hydrateRoot> | undefined;
      await act(async () => {
        root = hydrateRoot(container, ui);
      });
      expect(hydrationErrors(error)).toEqual([]);
      expect(container.innerHTML).toContain('Wednesday 4 November');
      act(() => root?.unmount());
    } finally {
      container.remove();
      spy.mockRestore();
      error.mockRestore();
    }
  });

  it('renders on the server', () => {
    const html = renderToString(<Picker />);
    expect(html).toContain('mn-slot-picker');
    expect(html).toContain('09:30');
  });
});
