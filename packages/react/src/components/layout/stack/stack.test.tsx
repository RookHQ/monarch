import { describe, expect, it } from 'vitest';
import { render, screen } from '@testing-library/react';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { Stack, layoutGap } from './index.js';

describe('Stack', () => {
  it('renders a div with its root class, default gap and alignment', async () => {
    render(
      <Stack data-testid="root">
        <p>One</p>
        <p>Two</p>
      </Stack>,
    );
    const root = screen.getByTestId('root');
    expect(root.tagName).toBe('DIV');
    expect(root).toHaveClass('mn-stack');
    expect(root).toHaveAttribute('data-gap', '4');
    expect(root).toHaveAttribute('data-align', 'stretch');
    expect(root).toHaveAttribute('data-justify', 'start');
    expect(root.style.getPropertyValue('--mn-stack-gap')).toBe('var(--mn-space-4)');
    await expectNoA11yViolations();
  });

  it('maps gap, align, justify and dividers to attributes', () => {
    render(<Stack data-testid="root" gap="1-5" align="center" justify="between" dividers />);
    const root = screen.getByTestId('root');
    expect(root).toHaveAttribute('data-gap', '1-5');
    expect(root.style.getPropertyValue('--mn-stack-gap')).toBe('var(--mn-space-1-5)');
    expect(root).toHaveAttribute('data-align', 'center');
    expect(root).toHaveAttribute('data-justify', 'between');
    expect(root).toHaveAttribute('data-dividers');
  });

  it('renders as another element, keeps list semantics and merges style', async () => {
    render(
      <Stack as="ul" aria-label="Steps" style={{ color: 'inherit' }} data-testid="root">
        <li>First</li>
        <li>Second</li>
      </Stack>,
    );
    const list = screen.getByRole('list', { name: 'Steps' });
    expect(list.tagName).toBe('UL');
    expect(screen.getAllByRole('listitem')).toHaveLength(2);
    expect(list.style.color).toBe('inherit');
    expect(list.style.getPropertyValue('--mn-stack-gap')).toBe('var(--mn-space-4)');
    await expectNoA11yViolations();
  });

  it('forwards its ref and className', () => {
    const ref = { current: null as HTMLElement | null };
    render(<Stack ref={ref} as="section" className="extra" aria-label="Area" />);
    expect(ref.current?.tagName).toBe('SECTION');
    expect(ref.current).toHaveClass('mn-stack', 'extra');
  });

  it('exposes the gap helper', () => {
    expect(layoutGap('12')).toBe('var(--mn-space-12)');
  });

  it('renders on the server', () => {
    const html = renderToString(<Stack gap="2">Body</Stack>);
    expect(html).toContain('mn-stack');
    expect(html).toContain('--mn-stack-gap:var(--mn-space-2)');
  });
});
