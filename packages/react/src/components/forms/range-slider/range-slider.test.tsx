import { useState } from 'react';
import { describe, expect, it, vi } from 'vitest';
import { fireEvent, render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { Field, FieldLabel } from '../form-field/index.js';
import { RangeSlider } from './index.js';

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

function mockTrack(el: Element, width = 200) {
  el.getBoundingClientRect = () =>
    ({ left: 0, width, top: 0, height: 20, right: width, bottom: 20, x: 0, y: 0 }) as DOMRect;
}

describe('RangeSlider', () => {
  it('is a group of two named sliders whose bounds follow each other', async () => {
    render(
      <Field>
        <FieldLabel>Price</FieldLabel>
        <RangeSlider defaultValue={[20, 80]} />
      </Field>,
    );
    expect(screen.getByRole('group', { name: 'Price' })).toBeInTheDocument();
    const low = screen.getByRole('slider', { name: 'Price Minimum' });
    const high = screen.getByRole('slider', { name: 'Price Maximum' });
    expect(low).toHaveAttribute('aria-valuenow', '20');
    expect(low).toHaveAttribute('aria-valuemin', '0');
    expect(low).toHaveAttribute('aria-valuemax', '80');
    expect(high).toHaveAttribute('aria-valuemin', '20');
    expect(high).toHaveAttribute('aria-valuemax', '100');
    await expectNoA11yViolations();
  });

  it('names thumbs from aria-label and translated messages', () => {
    render(
      <RangeSlider
        aria-label="Preis"
        messages={{ minimum: 'Untergrenze', maximum: 'Obergrenze' }}
      />,
    );
    expect(screen.getByRole('slider', { name: 'Preis Untergrenze' })).toBeInTheDocument();
    expect(screen.getByRole('slider', { name: 'Preis Obergrenze' })).toBeInTheDocument();
  });

  it('moves each thumb with the keyboard and never lets them cross', async () => {
    const onValueChange = vi.fn();
    const onValueCommit = vi.fn();
    render(
      <RangeSlider
        aria-label="Hours"
        defaultValue={[4, 6]}
        max={10}
        onValueChange={onValueChange}
        onValueCommit={onValueCommit}
      />,
    );
    const [low, high] = screen.getAllByRole('slider');
    const user = userEvent.setup();
    await user.tab();
    expect(low).toHaveFocus();
    await user.keyboard('{ArrowRight}{ArrowRight}{ArrowRight}{End}');
    expect(low).toHaveAttribute('aria-valuenow', '6');
    expect(onValueChange).toHaveBeenLastCalledWith([6, 6]);
    await user.tab();
    expect(high).toHaveFocus();
    await user.keyboard('{ArrowLeft}{Home}');
    expect(high).toHaveAttribute('aria-valuenow', '6');
    await user.keyboard('{End}');
    expect(high).toHaveAttribute('aria-valuenow', '10');
    expect(onValueCommit).toHaveBeenLastCalledWith([6, 10]);
    await user.keyboard('{Shift>}{Tab}{/Shift}{Home}');
    expect(low).toHaveAttribute('aria-valuenow', '0');
  });

  it('keeps a minimum gap between the thumbs', async () => {
    render(<RangeSlider aria-label="Range" defaultValue={[40, 60]} step={5} minStepsBetween={2} />);
    const [low, high] = screen.getAllByRole('slider');
    expect(low).toHaveAttribute('aria-valuemax', '50');
    expect(high).toHaveAttribute('aria-valuemin', '50');
    const user = userEvent.setup();
    low!.focus();
    await user.keyboard('{End}');
    expect(low).toHaveAttribute('aria-valuenow', '50');
  });

  it('moves the nearer thumb on click and drags it, committing on release', () => {
    const onValueChange = vi.fn();
    const onValueCommit = vi.fn();
    const { container } = render(
      <RangeSlider
        aria-label="Budget"
        defaultValue={[20, 80]}
        onValueChange={onValueChange}
        onValueCommit={onValueCommit}
      />,
    );
    const control = container.querySelector('.mn-range-slider__control')!;
    mockTrack(control);
    fireEvent.pointerDown(control, { button: 0, clientX: 140, pointerId: 1 }); // 70
    const [low, high] = screen.getAllByRole('slider');
    expect(high).toHaveFocus();
    expect(onValueChange).toHaveBeenLastCalledWith([20, 70]);
    fireEvent.pointerMove(control, { clientX: 10, pointerId: 1 }); // 5, clamped to low
    expect(onValueChange).toHaveBeenLastCalledWith([20, 20]);
    fireEvent.pointerUp(control, { pointerId: 1 });
    expect(onValueCommit).toHaveBeenCalledWith([20, 20]);
    fireEvent.pointerDown(control, { button: 0, clientX: 20, pointerId: 1 }); // 10
    expect(low).toHaveFocus();
    expect(onValueChange).toHaveBeenLastCalledWith([10, 20]);
  });

  it('follows a controlled value, shows the range and submits both ends', () => {
    function Controlled() {
      const [v, setV] = useState<[number, number]>([10, 30]);
      return (
        <form aria-label="Filters">
          <RangeSlider
            aria-label="Age"
            name="age"
            value={v}
            onValueChange={setV}
            showValue
            marks={[
              { value: 0, label: '0' },
              { value: 100, label: '100' },
            ]}
          />
          <button type="button" onClick={() => setV([25, 50])}>
            Reset
          </button>
        </form>
      );
    }
    const { container } = render(<Controlled />);
    expect(container.querySelector('output')).toHaveTextContent('10 – 30');
    fireEvent.click(screen.getByRole('button', { name: 'Reset' }));
    const inputs = container.querySelectorAll<HTMLInputElement>('input[name="age"]');
    expect([...inputs].map((i) => i.value)).toEqual(['25', '50']);
    expect(container.querySelectorAll('.mn-range-slider__mark')).toHaveLength(2);
  });

  it('ignores input when disabled', () => {
    const onValueChange = vi.fn();
    render(<RangeSlider aria-label="Range" disabled onValueChange={onValueChange} />);
    const [low] = screen.getAllByRole('slider');
    expect(low).toHaveAttribute('aria-disabled', 'true');
    fireEvent.keyDown(low!, { key: 'ArrowRight' });
    expect(onValueChange).not.toHaveBeenCalled();
  });

  it('renders on the server', () => {
    const html = renderToString(<RangeSlider aria-label="Range" defaultValue={[5, 9]} />);
    expect(html).toContain('mn-range-slider');
    expect(html).toContain('aria-valuenow="9"');
  });

  it('formats with en-US on the server when no locale is given, never the server default', () => {
    const spy = vi.spyOn(Intl, 'NumberFormat');
    const html = renderToString(
      <RangeSlider aria-label="Budget" defaultValue={[1500, 2500.5]} max={5000} step={0.5} />,
    );
    expect(html).toContain('aria-valuetext="2,500.5"');
    expect(spy.mock.calls.every(([locale]) => locale === 'en-US')).toBe(true);
    spy.mockRestore();
    render(
      <RangeSlider
        aria-label="Budget"
        defaultValue={[1500, 2500.5]}
        max={5000}
        step={0.5}
        locale="de-DE"
      />,
    );
    expect(screen.getAllByRole('slider')[1]).toHaveAttribute('aria-valuetext', '2.500,5');
  });

  it('does not submit its hidden inputs while disabled', () => {
    const { container } = render(<RangeSlider aria-label="Price" name="price" disabled />);
    const hidden = container.querySelectorAll('input[type="hidden"][name="price"]');
    expect(hidden).toHaveLength(2);
    hidden.forEach((input) => expect(input).toBeDisabled());
  });
});
