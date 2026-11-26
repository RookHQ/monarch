import { describe, expect, it } from 'vitest';
import { render, screen } from '@testing-library/react';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { Inline } from './index.js';

describe('Inline', () => {
  it('renders a wrapping, centred row with the default gap', async () => {
    render(
      <Inline data-testid="root">
        <button type="button">Save</button>
        <button type="button">Cancel</button>
      </Inline>,
    );
    const root = screen.getByTestId('root');
    expect(root).toHaveClass('mn-inline');
    expect(root).toHaveAttribute('data-wrap', 'wrap');
    expect(root).toHaveAttribute('data-align', 'center');
    expect(root).toHaveAttribute('data-gap', '2');
    expect(root.style.getPropertyValue('--mn-inline-gap')).toBe('var(--mn-space-2)');
    expect(root.style.getPropertyValue('--mn-inline-row-gap')).toBe('var(--mn-space-2)');
    await expectNoA11yViolations();
  });

  it('maps gap, rowGap, align, justify and wrap', () => {
    render(
      <Inline
        data-testid="root"
        gap="4"
        rowGap="1"
        align="baseline"
        justify="between"
        wrap={false}
      />,
    );
    const root = screen.getByTestId('root');
    expect(root.style.getPropertyValue('--mn-inline-gap')).toBe('var(--mn-space-4)');
    expect(root.style.getPropertyValue('--mn-inline-row-gap')).toBe('var(--mn-space-1)');
    expect(root).toHaveAttribute('data-align', 'baseline');
    expect(root).toHaveAttribute('data-justify', 'between');
    expect(root).toHaveAttribute('data-wrap', 'nowrap');
  });

  it('renders as a list', async () => {
    render(
      <Inline as="ul" aria-label="Tags">
        <li>Design</li>
        <li>Review</li>
      </Inline>,
    );
    expect(screen.getByRole('list', { name: 'Tags' }).tagName).toBe('UL');
    await expectNoA11yViolations();
  });

  it('forwards its ref', () => {
    const ref = { current: null as HTMLElement | null };
    render(<Inline ref={ref} as="nav" aria-label="Pages" />);
    expect(ref.current?.tagName).toBe('NAV');
  });

  it('renders on the server', () => {
    expect(renderToString(<Inline>Row</Inline>)).toContain('mn-inline');
  });
});
