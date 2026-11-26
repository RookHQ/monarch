import { describe, expect, it, vi } from 'vitest';
import { useRef, useState } from 'react';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { RovingFocusGroup, useRovingFocus, type RovingFocusGroupProps } from './index.js';

function Tools(props: Partial<RovingFocusGroupProps>) {
  return (
    <RovingFocusGroup role="toolbar" aria-label="Formatting" {...props}>
      <button type="button">Bold</button>
      <button type="button">Italic</button>
      <button type="button" disabled>
        Strike
      </button>
      <button type="button">Link</button>
    </RovingFocusGroup>
  );
}

const btn = (name: string) => screen.getByRole('button', { name });

describe('RovingFocusGroup', () => {
  it('puts exactly one item in the tab order', async () => {
    render(<Tools />);
    expect(screen.getByRole('toolbar', { name: 'Formatting' })).toHaveAttribute(
      'aria-orientation',
      'horizontal',
    );
    expect(btn('Bold')).toHaveAttribute('tabindex', '0');
    expect(btn('Italic')).toHaveAttribute('tabindex', '-1');
    expect(btn('Link')).toHaveAttribute('tabindex', '-1');
    await expectNoA11yViolations();
  });

  it('moves with Left/Right, skips disabled items, loops, and Home/End jump', async () => {
    const user = userEvent.setup();
    render(
      <>
        <Tools />
        <button type="button">After</button>
      </>,
    );
    await user.tab();
    expect(btn('Bold')).toHaveFocus();
    await user.keyboard('{ArrowRight}');
    expect(btn('Italic')).toHaveFocus();
    await user.keyboard('{ArrowRight}');
    expect(btn('Link')).toHaveFocus();
    await user.keyboard('{ArrowRight}');
    expect(btn('Bold')).toHaveFocus();
    await user.keyboard('{ArrowLeft}');
    expect(btn('Link')).toHaveFocus();
    await user.keyboard('{Home}');
    expect(btn('Bold')).toHaveFocus();
    await user.keyboard('{End}');
    expect(btn('Link')).toHaveFocus();
    // Tab leaves the group, and coming back lands on the last active item.
    await user.tab();
    expect(btn('After')).toHaveFocus();
    await user.tab({ shift: true });
    expect(btn('Link')).toHaveFocus();
  });

  it('stops at the ends without loop, and ignores Up/Down when horizontal', async () => {
    const user = userEvent.setup();
    render(<Tools loop={false} />);
    await user.tab();
    await user.keyboard('{ArrowLeft}');
    expect(btn('Bold')).toHaveFocus();
    await user.keyboard('{ArrowDown}');
    expect(btn('Bold')).toHaveFocus();
    await user.keyboard('{End}{ArrowRight}');
    expect(btn('Link')).toHaveFocus();
  });

  it('vertical orientation uses Up/Down', async () => {
    const user = userEvent.setup();
    render(<Tools orientation="vertical" role="toolbar" />);
    await user.tab();
    await user.keyboard('{ArrowDown}');
    expect(btn('Italic')).toHaveFocus();
    await user.keyboard('{ArrowRight}');
    expect(btn('Italic')).toHaveFocus();
    await user.keyboard('{ArrowUp}');
    expect(btn('Bold')).toHaveFocus();
  });

  it('swaps Left and Right in rtl', async () => {
    const user = userEvent.setup();
    render(<Tools dir="rtl" />);
    await user.tab();
    await user.keyboard('{ArrowLeft}');
    expect(btn('Italic')).toHaveFocus();
  });

  it('moves in two dimensions as a grid', async () => {
    const user = userEvent.setup();
    const days = ['1', '2', '3', '4', '5', '6', '7', '8'];
    render(
      <RovingFocusGroup orientation="grid" columns={3} role="group" aria-label="Days">
        {days.map((d) => (
          <button key={d} type="button" disabled={d === '5'} data-mn-roving-item="">
            {d}
          </button>
        ))}
      </RovingFocusGroup>,
    );
    await user.tab();
    expect(btn('1')).toHaveFocus();
    await user.keyboard('{ArrowDown}');
    expect(btn('4')).toHaveFocus();
    await user.keyboard('{ArrowRight}');
    // 5 is disabled, so Right skips to 6 within the row.
    expect(btn('6')).toHaveFocus();
    await user.keyboard('{ArrowRight}');
    expect(btn('6')).toHaveFocus();
    await user.keyboard('{ArrowDown}');
    // No item below 6: focus stays.
    expect(btn('6')).toHaveFocus();
    await user.keyboard('{Home}');
    expect(btn('4')).toHaveFocus();
    await user.keyboard('{Control>}{End}{/Control}');
    expect(btn('8')).toHaveFocus();
    await user.keyboard('{Control>}{Home}{/Control}');
    expect(btn('1')).toHaveFocus();
    await user.keyboard('{ArrowUp}');
    expect(btn('1')).toHaveFocus();
  });

  it('leaves arrow keys to text fields', async () => {
    const user = userEvent.setup();
    render(
      <RovingFocusGroup role="toolbar" aria-label="Search tools">
        <input aria-label="Find" />
        <button type="button">Go</button>
      </RovingFocusGroup>,
    );
    await user.tab();
    const input = screen.getByRole('textbox', { name: 'Find' });
    expect(input).toHaveFocus();
    await user.keyboard('ab{ArrowLeft}');
    expect(input).toHaveFocus();
  });

  it('only manages marked items when some are marked, and ignores nested groups', async () => {
    const user = userEvent.setup();
    render(
      <RovingFocusGroup role="toolbar" aria-label="Outer">
        <button type="button" data-mn-roving-item="">
          A
        </button>
        <button type="button">Unmarked</button>
        <RovingFocusGroup role="toolbar" aria-label="Inner" data-mn-roving-item="">
          <button type="button">X</button>
          <button type="button">Y</button>
        </RovingFocusGroup>
      </RovingFocusGroup>,
    );
    expect(btn('Unmarked')).not.toHaveAttribute('tabindex');
    expect(btn('X')).toHaveAttribute('tabindex', '0');
    expect(btn('Y')).toHaveAttribute('tabindex', '-1');
    await user.tab();
    expect(btn('A')).toHaveFocus();
  });

  it('starts on the selected item and reports the active index', async () => {
    const user = userEvent.setup();
    const onActiveIndexChange = vi.fn();
    render(
      <RovingFocusGroup
        role="radiogroup"
        aria-label="Size"
        onActiveIndexChange={onActiveIndexChange}
      >
        <button type="button" role="radio" aria-checked="false">
          Small
        </button>
        <button type="button" role="radio" aria-checked="true">
          Medium
        </button>
      </RovingFocusGroup>,
    );
    expect(screen.getByRole('radio', { name: 'Medium' })).toHaveAttribute('tabindex', '0');
    await user.tab();
    await user.keyboard('{ArrowLeft}');
    expect(screen.getByRole('radio', { name: 'Small' })).toHaveFocus();
    expect(onActiveIndexChange).toHaveBeenLastCalledWith(0);
  });

  it('picks up items added later', async () => {
    const user = userEvent.setup();
    function Growing() {
      const [n, setN] = useState(1);
      return (
        <>
          <button type="button" onClick={() => setN((x) => x + 1)}>
            Add
          </button>
          <RovingFocusGroup role="toolbar" aria-label="Items">
            {Array.from({ length: n }, (_, i) => (
              <button key={i} type="button">{`Item ${i + 1}`}</button>
            ))}
          </RovingFocusGroup>
        </>
      );
    }
    render(<Growing />);
    await user.click(btn('Add'));
    expect(btn('Item 2')).toHaveAttribute('tabindex', '-1');
  });

  it('works as a hook on any element', async () => {
    const user = userEvent.setup();
    function Hooked() {
      const ref = useRef<HTMLUListElement>(null);
      const { onKeyDown } = useRovingFocus(ref, { orientation: 'vertical' });
      return (
        // eslint-disable-next-line jsx-a11y/no-noninteractive-element-interactions
        <ul ref={ref} onKeyDown={onKeyDown} aria-label="Links">
          <li>
            <a href="#1">One</a>
          </li>
          <li>
            <a href="#2">Two</a>
          </li>
        </ul>
      );
    }
    render(<Hooked />);
    await user.tab();
    await user.keyboard('{ArrowDown}');
    expect(screen.getByRole('link', { name: 'Two' })).toHaveFocus();
  });

  it('calls a callback ref once, not again on every render', () => {
    const ref = vi.fn();
    const { rerender } = render(
      <RovingFocusGroup ref={ref} aria-label="Tools">
        <button type="button">One</button>
      </RovingFocusGroup>,
    );
    rerender(
      <RovingFocusGroup ref={ref} aria-label="Tools">
        <button type="button">Two</button>
      </RovingFocusGroup>,
    );
    expect(ref).toHaveBeenCalledTimes(1);
    expect(ref).toHaveBeenLastCalledWith(expect.any(HTMLDivElement));
  });

  it('renders on the server', () => {
    const html = renderToString(<Tools />);
    expect(html).toContain('mn-roving-focus-group');
    expect(html).toContain('data-orientation="horizontal"');
  });
});
