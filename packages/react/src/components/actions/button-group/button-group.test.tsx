import { describe, expect, it, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { Button, IconButton } from '../button/index.js';
import { ButtonGroup } from './index.js';

describe('ButtonGroup', () => {
  it('is a labelled group whose buttons each keep their own Tab stop', async () => {
    const user = userEvent.setup();
    const onNext = vi.fn();
    render(
      <ButtonGroup aria-label="Pages">
        <Button>Previous</Button>
        <Button onClick={onNext}>Next</Button>
        <IconButton label="More pages" icon={<svg />} />
      </ButtonGroup>,
    );
    const group = screen.getByRole('group', { name: 'Pages' });
    expect(group).toHaveClass('mn-button-group');
    expect(group).toHaveAttribute('data-orientation', 'horizontal');
    expect(group).toHaveAttribute('data-attached');
    await user.tab();
    expect(screen.getByRole('button', { name: 'Previous' })).toHaveFocus();
    await user.tab();
    expect(screen.getByRole('button', { name: 'Next' })).toHaveFocus();
    await user.keyboard('{Enter}');
    expect(onNext).toHaveBeenCalledOnce();
    await user.tab();
    expect(screen.getByRole('button', { name: 'More pages' })).toHaveFocus();
    await expectNoA11yViolations();
  });

  it('exposes orientation, spacing and block as data attributes', () => {
    render(
      <ButtonGroup aria-label="Zoom" orientation="vertical" attached={false} block>
        <Button>Zoom in</Button>
        <Button>Zoom out</Button>
      </ButtonGroup>,
    );
    const group = screen.getByRole('group', { name: 'Zoom' });
    expect(group).toHaveAttribute('data-orientation', 'vertical');
    expect(group).not.toHaveAttribute('data-attached');
    expect(group).toHaveAttribute('data-block');
  });

  it('forwards its ref and native attributes', () => {
    const ref = { current: null as HTMLDivElement | null };
    render(<ButtonGroup ref={ref} id="actions" data-testid="g" />);
    expect(ref.current).toBe(screen.getByTestId('g'));
    expect(ref.current).toHaveAttribute('id', 'actions');
  });

  it('renders on the server', () => {
    const html = renderToString(
      <ButtonGroup aria-label="Pages">
        <Button>Previous</Button>
      </ButtonGroup>,
    );
    expect(html).toContain('role="group"');
    expect(html).toContain('mn-button-group');
  });
});
