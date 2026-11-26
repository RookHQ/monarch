import { describe, expect, it, vi } from 'vitest';
import { useState } from 'react';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { ToggleButton } from './index.js';

describe('ToggleButton', () => {
  it('toggles aria-pressed on click, Enter and Space without changing its name', async () => {
    const user = userEvent.setup();
    const onPressedChange = vi.fn();
    render(<ToggleButton onPressedChange={onPressedChange}>Show archived</ToggleButton>);
    const button = screen.getByRole('button', { name: 'Show archived' });
    expect(button).toHaveAttribute('aria-pressed', 'false');
    expect(button).toHaveAttribute('data-state', 'off');
    expect(button).toHaveAttribute('type', 'button');
    await user.click(button);
    expect(button).toHaveAttribute('aria-pressed', 'true');
    expect(button).toHaveAttribute('data-state', 'on');
    expect(onPressedChange).toHaveBeenLastCalledWith(true);
    await user.keyboard('{Enter}');
    expect(button).toHaveAttribute('aria-pressed', 'false');
    await user.keyboard(' ');
    expect(button).toHaveAttribute('aria-pressed', 'true');
    expect(screen.getByRole('button', { name: 'Show archived' })).toBe(button);
    await expectNoA11yViolations();
  });

  it('is controlled with pressed and onPressedChange', async () => {
    const user = userEvent.setup();
    function Controlled() {
      const [on, setOn] = useState(true);
      return (
        <>
          <ToggleButton pressed={on} onPressedChange={setOn} aria-label="Bold">
            B
          </ToggleButton>
          <output>{on ? 'bold' : 'regular'}</output>
        </>
      );
    }
    render(<Controlled />);
    const button = screen.getByRole('button', { name: 'Bold' });
    expect(button).toHaveAttribute('aria-pressed', 'true');
    await user.click(button);
    expect(button).toHaveAttribute('aria-pressed', 'false');
    expect(screen.getByText('regular')).toBeInTheDocument();
  });

  it('stays unchanged when the controlled owner rejects the change', async () => {
    const user = userEvent.setup();
    render(
      <ToggleButton pressed={false} onPressedChange={() => {}}>
        Pin
      </ToggleButton>,
    );
    const button = screen.getByRole('button', { name: 'Pin' });
    await user.click(button);
    expect(button).toHaveAttribute('aria-pressed', 'false');
  });

  it('does not toggle while disabled or when onClick prevents the default', async () => {
    const user = userEvent.setup();
    const onPressedChange = vi.fn();
    render(
      <>
        <ToggleButton disabled onPressedChange={onPressedChange}>
          Disabled
        </ToggleButton>
        <ToggleButton onClick={(e) => e.preventDefault()} onPressedChange={onPressedChange}>
          Prevented
        </ToggleButton>
      </>,
    );
    await user.click(screen.getByRole('button', { name: 'Disabled' }));
    await user.click(screen.getByRole('button', { name: 'Prevented' }));
    expect(onPressedChange).not.toHaveBeenCalled();
  });

  it('maps the outline variant and keeps Button sizing', () => {
    render(
      <ToggleButton variant="outline" size="sm" defaultPressed>
        Grid
      </ToggleButton>,
    );
    const button = screen.getByRole('button', { name: 'Grid' });
    expect(button).toHaveClass('mn-button', 'mn-toggle-button');
    expect(button).toHaveAttribute('data-toggle-variant', 'outline');
    expect(button).toHaveAttribute('data-size', 'sm');
    expect(button).toHaveAttribute('aria-pressed', 'true');
  });

  it('renders on the server', () => {
    const html = renderToString(<ToggleButton defaultPressed>Bold</ToggleButton>);
    expect(html).toContain('mn-toggle-button');
    expect(html).toContain('aria-pressed="true"');
  });
});
