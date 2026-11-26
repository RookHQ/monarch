import { describe, expect, it, vi } from 'vitest';
import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { createRef } from 'react';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { StatusGlyph, StatusPicker, type StatusOption } from './index.js';

const STATUSES: StatusOption[] = [
  { id: 'todo', name: 'Todo', category: 'unstarted' },
  { id: 'triage', name: 'Triage', category: 'backlog' },
  { id: 'doing', name: 'In progress', category: 'started' },
  { id: 'review', name: 'In review', category: 'started' },
  { id: 'done', name: 'Done', category: 'completed' },
  { id: 'dropped', name: 'Dropped', category: 'cancelled' },
];

describe('StatusPicker', () => {
  it('StatusGlyph forwards its ref', () => {
    const ref = createRef<SVGSVGElement>();
    render(<StatusGlyph ref={ref} category="started" />);
    expect(ref.current).toHaveClass('mn-status-picker__glyph');
  });

  it('names the trigger with the status and shows glyph and text', async () => {
    const ref = createRef<HTMLButtonElement>();
    const { container } = render(<StatusPicker ref={ref} statuses={STATUSES} value="review" />);
    const trigger = screen.getByRole('button', { name: 'Status: In review' });
    expect(trigger).toBe(ref.current);
    expect(trigger).toHaveClass('mn-status-picker');
    expect(trigger).toHaveAttribute('data-category', 'started');
    expect(trigger).toHaveAttribute('aria-haspopup', 'menu');
    expect(trigger).toHaveTextContent('In review');
    expect(container.querySelector('svg[data-category="started"]')).toHaveAttribute(
      'aria-hidden',
      'true',
    );
    await expectNoA11yViolations();
  });

  it('opens from the keyboard with grouped radio items and picks one', async () => {
    const user = userEvent.setup();
    const onValueChange = vi.fn();
    render(<StatusPicker statuses={STATUSES} value="todo" onValueChange={onValueChange} />);
    screen.getByRole('button').focus();
    await user.keyboard('{Enter}');
    const menu = await screen.findByRole('menu');
    const labels = Array.from(menu.querySelectorAll('.mn-menu__label'));
    expect(labels.map((l) => l.textContent)).toEqual([
      'Backlog',
      'Not started',
      'In progress',
      'Completed',
      'Cancelled',
    ]);
    expect(within(menu).getByRole('menuitemradio', { name: 'Todo' })).toHaveAttribute(
      'aria-checked',
      'true',
    );
    await waitFor(() => expect(within(menu).getAllByRole('menuitemradio')[0]).toHaveFocus());
    await user.keyboard('{ArrowDown}{ArrowDown}');
    expect(within(menu).getByRole('menuitemradio', { name: 'In progress' })).toHaveFocus();
    await expectNoA11yViolations(menu);
    await user.keyboard('{Enter}');
    expect(onValueChange).toHaveBeenCalledWith('doing');
    await waitFor(() => expect(screen.queryByRole('menu')).toBeNull());
    expect(screen.getByRole('button')).toHaveFocus();
  });

  it('has a compact glyph-only trigger that is still named, and a none state', () => {
    const { rerender } = render(
      <StatusPicker statuses={STATUSES} value="done" showLabel={false} size="sm" />,
    );
    const trigger = screen.getByRole('button', { name: 'Status: Done' });
    expect(trigger).toHaveAttribute('data-icon-only');
    expect(trigger).not.toHaveTextContent('Done');
    rerender(<StatusPicker statuses={STATUSES} value={null} />);
    expect(screen.getByRole('button', { name: 'Status: No status' })).toBeInTheDocument();
  });

  it('translates and disables', () => {
    render(
      <StatusPicker
        statuses={STATUSES}
        value="todo"
        disabled
        messages={{ trigger: (s) => `Statut : ${s}` }}
      />,
    );
    expect(screen.getByRole('button', { name: 'Statut : Todo' })).toBeDisabled();
  });

  it('draws a glyph per category', () => {
    const { container } = render(
      <>
        {(['backlog', 'unstarted', 'started', 'completed', 'cancelled'] as const).map((c) => (
          <StatusGlyph key={c} category={c} />
        ))}
      </>,
    );
    expect(container.querySelectorAll('svg')).toHaveLength(5);
  });

  it('renders on the server', () => {
    const html = renderToString(<StatusPicker statuses={STATUSES} value="doing" />);
    expect(html).toContain('mn-status-picker');
    expect(html).toContain('In progress');
  });
});
