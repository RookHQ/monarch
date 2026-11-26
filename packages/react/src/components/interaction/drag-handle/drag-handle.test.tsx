import { describe, expect, it, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { DragHandle } from './index.js';

describe('DragHandle', () => {
  it('is a named, described button that is not pressed at rest', async () => {
    render(<DragHandle label="Reorder Design review" />);
    const h = screen.getByRole('button', { name: 'Reorder Design review' });
    expect(h).toHaveClass('mn-drag-handle');
    expect(h).toHaveAttribute('type', 'button');
    expect(h).toHaveAttribute('aria-pressed', 'false');
    expect(h).toHaveAttribute('data-state', 'idle');
    expect(h).toHaveAccessibleDescription(/Press Space or Enter to pick up/);
    await expectNoA11yViolations();
  });

  it('shows the grabbed and dropped states', async () => {
    const { rerender } = render(<DragHandle label="Reorder Notes" state="grabbed" />);
    const h = screen.getByRole('button', { name: 'Reorder Notes' });
    expect(h).toHaveAttribute('aria-pressed', 'true');
    expect(h).toHaveAttribute('data-state', 'grabbed');
    await expectNoA11yViolations();
    rerender(<DragHandle label="Reorder Notes" state="dropped" />);
    expect(h).toHaveAttribute('aria-pressed', 'false');
    expect(h).toHaveAttribute('data-state', 'dropped');
  });

  it('passes key and pointer handlers to the button', async () => {
    const user = userEvent.setup();
    const onKeyDown = vi.fn();
    const onClick = vi.fn();
    render(<DragHandle label="Reorder Tasks" onKeyDown={onKeyDown} onClick={onClick} />);
    await user.tab();
    expect(screen.getByRole('button', { name: 'Reorder Tasks' })).toHaveFocus();
    await user.keyboard(' ');
    expect(onKeyDown).toHaveBeenCalled();
    expect(onClick).toHaveBeenCalled();
  });

  it('can be disabled, sized, oriented and given a custom icon', () => {
    render(
      <DragHandle
        label="Reorder Row"
        disabled
        size="sm"
        axis="both"
        icon={<svg data-testid="custom" aria-hidden />}
      />,
    );
    const h = screen.getByRole('button', { name: 'Reorder Row' });
    expect(h).toBeDisabled();
    expect(h).toHaveAttribute('data-size', 'sm');
    expect(h).toHaveAttribute('data-axis', 'both');
    expect(screen.getByTestId('custom')).toBeInTheDocument();
  });

  it('keeps a shared description and can drop its own', () => {
    render(
      <>
        <p id="shared">Drag tiles to reorder.</p>
        <DragHandle label="Reorder Tile" describe={false} aria-describedby="shared" />
      </>,
    );
    const h = screen.getByRole('button', { name: 'Reorder Tile' });
    expect(h).toHaveAccessibleDescription('Drag tiles to reorder.');
  });

  it('translates its instructions', () => {
    render(
      <DragHandle label="Ordenar Notas" messages={{ instructions: 'Pulse Espacio para mover.' }} />,
    );
    expect(screen.getByRole('button', { name: 'Ordenar Notas' })).toHaveAccessibleDescription(
      'Pulse Espacio para mover.',
    );
  });

  it('forwards its ref', () => {
    const ref = { current: null as HTMLButtonElement | null };
    render(<DragHandle ref={ref} label="Reorder A" />);
    expect(ref.current).toBeInstanceOf(HTMLButtonElement);
  });

  it('renders on the server', () => {
    expect(renderToString(<DragHandle label="Reorder A" />)).toContain('mn-drag-handle');
  });
});
