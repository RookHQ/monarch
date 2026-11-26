import { describe, expect, it } from 'vitest';
import { render, screen } from '@testing-library/react';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { Grid } from './index.js';

describe('Grid', () => {
  it('defaults to columns of at least 16rem, filled', async () => {
    render(
      <Grid data-testid="root">
        <div>A</div>
        <div>B</div>
      </Grid>,
    );
    const root = screen.getByTestId('root');
    expect(root).toHaveClass('mn-grid');
    expect(root).toHaveAttribute('data-mode', 'auto');
    expect(root).toHaveAttribute('data-fit', 'fill');
    expect(root.style.getPropertyValue('--mn-grid-min')).toBe('16rem');
    expect(root.style.getPropertyValue('--mn-grid-gap')).toBe('var(--mn-space-4)');
    await expectNoA11yViolations();
  });

  it('takes a minimum item width in pixels or any CSS length, and fit', () => {
    const { rerender } = render(<Grid data-testid="root" minItemWidth={200} fit="fit" />);
    const root = screen.getByTestId('root');
    expect(root.style.getPropertyValue('--mn-grid-min')).toBe('200px');
    expect(root).toHaveAttribute('data-fit', 'fit');
    rerender(<Grid data-testid="root" minItemWidth="12ch" />);
    expect(root.style.getPropertyValue('--mn-grid-min')).toBe('12ch');
  });

  it('uses fixed columns, ignoring the minimum width', () => {
    render(<Grid data-testid="root" columns={3} minItemWidth={200} />);
    const root = screen.getByTestId('root');
    expect(root).toHaveAttribute('data-mode', 'columns');
    expect(root).not.toHaveAttribute('data-fit');
    expect(root.style.getPropertyValue('--mn-grid-cols-base')).toBe('3');
    expect(root.style.getPropertyValue('--mn-grid-min')).toBe('');
  });

  it('sets column counts per breakpoint and skips invalid ones', () => {
    render(<Grid data-testid="root" columns={{ base: 1, md: 2, lg: 3.7, xl: 0 }} />);
    const root = screen.getByTestId('root');
    expect(root.style.getPropertyValue('--mn-grid-cols-base')).toBe('1');
    expect(root.style.getPropertyValue('--mn-grid-cols-sm')).toBe('');
    expect(root.style.getPropertyValue('--mn-grid-cols-md')).toBe('2');
    expect(root.style.getPropertyValue('--mn-grid-cols-lg')).toBe('3');
    expect(root.style.getPropertyValue('--mn-grid-cols-xl')).toBe('');
  });

  it('maps gap, rowGap and align, renders as a list', async () => {
    render(
      <Grid as="ul" aria-label="Projects" gap="6" rowGap="2" align="start">
        <li>One</li>
      </Grid>,
    );
    const list = screen.getByRole('list', { name: 'Projects' });
    expect(list.style.getPropertyValue('--mn-grid-gap')).toBe('var(--mn-space-6)');
    expect(list.style.getPropertyValue('--mn-grid-row-gap')).toBe('var(--mn-space-2)');
    expect(list).toHaveAttribute('data-align', 'start');
    await expectNoA11yViolations();
  });

  it('forwards its ref', () => {
    const ref = { current: null as HTMLElement | null };
    render(<Grid ref={ref} />);
    expect(ref.current).toHaveClass('mn-grid');
  });

  it('renders on the server', () => {
    const html = renderToString(<Grid columns={{ base: 1, md: 3 }}>x</Grid>);
    expect(html).toContain('mn-grid');
    expect(html).toContain('--mn-grid-cols-md:3');
  });
});
