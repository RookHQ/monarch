import { describe, expect, it, vi } from 'vitest';
import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { createRef } from 'react';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import {
  PRIORITY_LEVELS,
  PriorityGlyph,
  PriorityPicker,
  priorityFromNumber,
  priorityToNumber,
} from './index.js';

describe('priority numbers', () => {
  it('round-trips the 0–4 scale', () => {
    expect(priorityFromNumber(1)).toBe('urgent');
    expect(priorityFromNumber(4)).toBe('low');
    expect(priorityFromNumber(null)).toBe('none');
    expect(priorityFromNumber(9)).toBe('none');
    for (const l of PRIORITY_LEVELS) expect(priorityFromNumber(priorityToNumber(l))).toBe(l);
  });
});

describe('PriorityPicker', () => {
  it('PriorityGlyph forwards its ref', () => {
    const ref = createRef<SVGSVGElement>();
    render(<PriorityGlyph ref={ref} priority="high" />);
    expect(ref.current).toHaveClass('mn-priority-picker__glyph');
  });

  it('names the trigger with the priority and shows a distinct glyph', async () => {
    const ref = createRef<HTMLButtonElement>();
    render(<PriorityPicker ref={ref} value="high" />);
    const trigger = screen.getByRole('button', { name: 'Priority: High' });
    expect(trigger).toBe(ref.current);
    expect(trigger).toHaveClass('mn-priority-picker');
    expect(trigger).toHaveAttribute('data-priority', 'high');
    expect(trigger).toHaveTextContent('High');
    await expectNoA11yViolations();
  });

  it('opens from the keyboard and picks a level', async () => {
    const user = userEvent.setup();
    const onValueChange = vi.fn();
    render(<PriorityPicker value="none" onValueChange={onValueChange} />);
    screen.getByRole('button').focus();
    await user.keyboard('{ArrowDown}');
    const menu = await screen.findByRole('menu');
    const items = within(menu).getAllByRole('menuitemradio');
    expect(items.map((i) => i.textContent)).toEqual([
      'Urgent',
      'High',
      'Medium',
      'Low',
      'No priority',
    ]);
    expect(within(menu).getByRole('menuitemradio', { name: 'No priority' })).toHaveAttribute(
      'aria-checked',
      'true',
    );
    await waitFor(() => expect(items[0]).toHaveFocus());
    await expectNoA11yViolations(menu);
    await user.keyboard('{ArrowDown}{Enter}');
    expect(onValueChange).toHaveBeenCalledWith('high');
  });

  it('limits levels, has a glyph-only trigger, translates and disables', () => {
    const { rerender } = render(
      <PriorityPicker value="low" showLabel={false} size="sm" levels={['high', 'low']} />,
    );
    expect(screen.getByRole('button', { name: 'Priority: Low' })).not.toHaveTextContent('Low');
    rerender(<PriorityPicker value={null} disabled messages={{ levels: { none: 'Aucune' } }} />);
    expect(screen.getByRole('button', { name: 'Priority: Aucune' })).toBeDisabled();
  });

  it('draws a different shape for every level', () => {
    const { container } = render(
      <>
        {PRIORITY_LEVELS.map((p) => (
          <PriorityGlyph key={p} priority={p} />
        ))}
      </>,
    );
    const shapes = Array.from(container.querySelectorAll('svg')).map((s) => s.innerHTML);
    expect(new Set(shapes).size).toBe(5);
  });

  it('renders on the server', () => {
    expect(renderToString(<PriorityPicker value="urgent" />)).toContain('Urgent');
  });
});
