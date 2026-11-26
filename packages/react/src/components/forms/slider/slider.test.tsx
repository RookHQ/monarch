import { useState } from 'react';
import { describe, expect, it, vi } from 'vitest';
import { fireEvent, render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { Field, FieldDescription, FieldLabel } from '../form-field/index.js';
import {
  Slider,
  defaultLargeStep,
  percentOf,
  sliderKeyValue,
  snapToStep,
  valueFromPointer,
} from './index.js';

// jsdom has no PointerEvent; a MouseEvent subclass carries clientX and button.
if (typeof window.PointerEvent === 'undefined') {
  class PointerEventStub extends MouseEvent {
    pointerId: number;
    constructor(type: string, init: PointerEventInit = {}) {
      super(type, init);
      this.pointerId = init.pointerId ?? 1;
    }
  }
  window.PointerEvent = PointerEventStub as unknown as typeof PointerEvent;
}

function mockTrack(el: Element, left = 0, width = 200) {
  el.getBoundingClientRect = () =>
    ({
      left,
      width,
      top: 0,
      height: 20,
      right: left + width,
      bottom: 20,
      x: left,
      y: 0,
    }) as DOMRect;
}

describe('slider helpers', () => {
  it('snaps to the step without drift and clamps', () => {
    expect(snapToStep(0.30000000000000004, 0, 1, 0.1)).toBe(0.3);
    expect(snapToStep(7, 0, 100, 5)).toBe(5);
    expect(snapToStep(8, 0, 100, 5)).toBe(10);
    expect(snapToStep(130, 0, 100, 5)).toBe(100);
    expect(snapToStep(4, 1, 10, 2)).toBe(5);
  });

  it('maps keys, flipping arrows in RTL', () => {
    const o = { min: 0, max: 10, step: 1, largeStep: 5 };
    expect(sliderKeyValue('ArrowRight', 3, o)).toBe(4);
    expect(sliderKeyValue('ArrowUp', 3, o)).toBe(4);
    expect(sliderKeyValue('ArrowLeft', 3, o)).toBe(2);
    expect(sliderKeyValue('ArrowLeft', 3, { ...o, rtl: true })).toBe(4);
    expect(sliderKeyValue('PageUp', 8, o)).toBe(10);
    expect(sliderKeyValue('Home', 8, o)).toBe(0);
    expect(sliderKeyValue('End', 2, o)).toBe(10);
    expect(sliderKeyValue('a', 2, o)).toBeNull();
  });

  it('reads the pointer position and percentages', () => {
    const rect = { left: 100, width: 200 };
    expect(valueFromPointer(200, rect, { min: 0, max: 100, step: 1 })).toBe(50);
    expect(valueFromPointer(150, rect, { min: 0, max: 100, step: 1, rtl: true })).toBe(75);
    expect(valueFromPointer(0, rect, { min: 0, max: 100, step: 1 })).toBe(0);
    expect(percentOf(25, 0, 50)).toBe(50);
    expect(defaultLargeStep(0, 100, 1)).toBe(10);
    expect(defaultLargeStep(0, 1, 0.25)).toBe(0.25);
  });
});

describe('Slider', () => {
  it('is an APG slider labelled and described by its Field', async () => {
    render(
      <Field>
        <FieldLabel>Volume</FieldLabel>
        <Slider defaultValue={40} />
        <FieldDescription>Applies to alerts.</FieldDescription>
      </Field>,
    );
    const thumb = screen.getByRole('slider', { name: 'Volume' });
    expect(thumb).toHaveAttribute('aria-valuenow', '40');
    expect(thumb).toHaveAttribute('aria-valuemin', '0');
    expect(thumb).toHaveAttribute('aria-valuemax', '100');
    expect(thumb).toHaveAttribute('aria-valuetext', '40');
    expect(thumb).toHaveAttribute('aria-orientation', 'horizontal');
    expect(thumb).toHaveAccessibleDescription('Applies to alerts.');
    await expectNoA11yViolations();
  });

  it('moves with arrows, PageUp/PageDown and Home/End, and commits each key', async () => {
    const onValueChange = vi.fn();
    const onValueCommit = vi.fn();
    render(
      <Slider
        aria-label="Opacity"
        defaultValue={50}
        step={5}
        onValueChange={onValueChange}
        onValueCommit={onValueCommit}
      />,
    );
    const thumb = screen.getByRole('slider');
    const user = userEvent.setup();
    await user.tab();
    expect(thumb).toHaveFocus();
    await user.keyboard('{ArrowRight}{ArrowUp}');
    expect(thumb).toHaveAttribute('aria-valuenow', '60');
    await user.keyboard('{ArrowLeft}{ArrowDown}');
    expect(thumb).toHaveAttribute('aria-valuenow', '50');
    await user.keyboard('{PageUp}');
    expect(thumb).toHaveAttribute('aria-valuenow', '60');
    await user.keyboard('{PageDown}{PageDown}');
    expect(thumb).toHaveAttribute('aria-valuenow', '40');
    await user.keyboard('{End}');
    expect(thumb).toHaveAttribute('aria-valuenow', '100');
    await user.keyboard('{ArrowRight}');
    expect(thumb).toHaveAttribute('aria-valuenow', '100');
    await user.keyboard('{Home}');
    expect(onValueChange).toHaveBeenLastCalledWith(0);
    expect(onValueCommit).toHaveBeenLastCalledWith(0);
  });

  it('flips the arrow keys in a right-to-left layout', async () => {
    render(
      <div dir="rtl">
        <Slider aria-label="Level" defaultValue={5} max={10} />
      </div>,
    );
    const thumb = screen.getByRole('slider');
    const user = userEvent.setup();
    thumb.focus();
    await user.keyboard('{ArrowLeft}');
    expect(thumb).toHaveAttribute('aria-valuenow', '6');
  });

  it('formats the spoken value and shows it on request', () => {
    render(
      <Slider
        aria-label="Discount"
        defaultValue={0.25}
        max={1}
        step={0.05}
        showValue
        formatValue={(v) => `${Math.round(v * 100)}%`}
      />,
    );
    expect(screen.getByRole('slider')).toHaveAttribute('aria-valuetext', '25%');
    expect(screen.getByText('25%', { selector: 'output' })).toBeInTheDocument();
  });

  it('jumps to a clicked point, drags, and commits on release', () => {
    const onValueChange = vi.fn();
    const onValueCommit = vi.fn();
    const { container } = render(
      <Slider aria-label="Price" onValueChange={onValueChange} onValueCommit={onValueCommit} />,
    );
    const control = container.querySelector('.mn-slider__control')!;
    mockTrack(control);
    fireEvent.pointerDown(control, { button: 0, clientX: 50, pointerId: 1 });
    expect(screen.getByRole('slider')).toHaveFocus();
    expect(onValueChange).toHaveBeenLastCalledWith(25);
    fireEvent.pointerMove(control, { clientX: 150, pointerId: 1 });
    expect(onValueChange).toHaveBeenLastCalledWith(75);
    expect(onValueCommit).not.toHaveBeenCalled();
    fireEvent.pointerUp(control, { clientX: 150, pointerId: 1 });
    expect(onValueCommit).toHaveBeenCalledWith(75);
    fireEvent.pointerMove(control, { clientX: 10, pointerId: 1 });
    expect(onValueChange).toHaveBeenCalledTimes(2);
  });

  it('follows a controlled value, renders marks and submits with a name', () => {
    function Controlled() {
      const [v, setV] = useState(2);
      return (
        <form aria-label="Settings">
          <Slider
            aria-label="Rating"
            name="rating"
            min={1}
            max={5}
            value={v}
            onValueChange={setV}
            marks={[{ value: 1, label: 'Low' }, { value: 3 }, { value: 5, label: 'High' }]}
          />
          <button type="button" onClick={() => setV(4)}>
            Set 4
          </button>
        </form>
      );
    }
    const { container } = render(<Controlled />);
    expect(screen.getByText('Low')).toBeInTheDocument();
    expect(container.querySelectorAll('.mn-slider__mark')).toHaveLength(3);
    expect(container.querySelector('input[name="rating"]')).toHaveValue('2');
    fireEvent.click(screen.getByRole('button', { name: 'Set 4' }));
    expect(screen.getByRole('slider')).toHaveAttribute('aria-valuenow', '4');
    expect(container.querySelector('input[name="rating"]')).toHaveValue('4');
  });

  it('ignores keys and pointers when disabled (also from a Field)', () => {
    const onValueChange = vi.fn();
    const { container } = render(
      <Field disabled>
        <FieldLabel>Speed</FieldLabel>
        <Slider defaultValue={10} onValueChange={onValueChange} />
      </Field>,
    );
    const thumb = screen.getByRole('slider', { name: 'Speed' });
    expect(thumb).toHaveAttribute('aria-disabled', 'true');
    expect(thumb).toHaveAttribute('tabindex', '-1');
    fireEvent.keyDown(thumb, { key: 'ArrowRight' });
    const control = container.querySelector('.mn-slider__control')!;
    mockTrack(control);
    fireEvent.pointerDown(control, { button: 0, clientX: 150, pointerId: 1 });
    expect(onValueChange).not.toHaveBeenCalled();
  });

  it('renders on the server', () => {
    const html = renderToString(<Slider aria-label="Zoom" defaultValue={30} />);
    expect(html).toContain('mn-slider');
    expect(html).toContain('aria-valuenow="30"');
  });

  it('formats with en-US on the server when no locale is given, never the server default', () => {
    const spy = vi.spyOn(Intl, 'NumberFormat');
    const html = renderToString(
      <Slider aria-label="Budget" defaultValue={1234.5} max={5000} step={0.5} />,
    );
    expect(html).toContain('aria-valuetext="1,234.5"');
    expect(spy.mock.calls.every(([locale]) => locale === 'en-US')).toBe(true);
    spy.mockRestore();
    render(
      <Slider aria-label="Budget" defaultValue={1234.5} max={5000} step={0.5} locale="de-DE" />,
    );
    expect(screen.getByRole('slider')).toHaveAttribute('aria-valuetext', '1.234,5');
  });

  it('does not submit its hidden input while disabled', () => {
    const { container } = render(<Slider aria-label="Volume" name="volume" disabled />);
    expect(container.querySelector('input[type="hidden"][name="volume"]')).toBeDisabled();
  });
});
