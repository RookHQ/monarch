import { createRef } from 'react';
import { describe, expect, it } from 'vitest';
import { render, screen } from '@testing-library/react';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { LifecycleBadge, formatLifecycleLabel } from './index.js';

describe('LifecycleBadge', () => {
  it('labels and tones the built-in stages', async () => {
    render(
      <div>
        <LifecycleBadge lifecycle="planned" data-testid="planned" />
        <LifecycleBadge lifecycle="preview" data-testid="preview" />
        <LifecycleBadge lifecycle="stable" data-testid="stable" />
        <LifecycleBadge lifecycle="deprecated" data-testid="deprecated" />
      </div>,
    );
    expect(screen.getByTestId('planned')).toHaveTextContent('Planned');
    expect(screen.getByTestId('planned')).toHaveAttribute('data-tone', 'neutral');
    expect(screen.getByTestId('preview')).toHaveAttribute('data-tone', 'info');
    expect(screen.getByTestId('stable')).toHaveTextContent('Stable');
    expect(screen.getByTestId('stable')).toHaveAttribute('data-tone', 'success');
    expect(screen.getByTestId('deprecated')).toHaveAttribute('data-tone', 'warning');
    expect(screen.getByTestId('deprecated')).toHaveAttribute('data-lifecycle', 'deprecated');
    await expectNoA11yViolations();
  });

  it('takes labels from messages and label', () => {
    render(
      <div>
        <LifecycleBadge lifecycle="stable" messages={{ stable: 'Live' }} data-testid="a" />
        <LifecycleBadge lifecycle="stable" label="General availability" data-testid="b" />
      </div>,
    );
    expect(screen.getByTestId('a')).toHaveTextContent('Live');
    expect(screen.getByTestId('b')).toHaveTextContent('General availability');
  });

  it('renders custom stages with a tone, or the humanised key', () => {
    render(
      <div>
        <LifecycleBadge lifecycle="customer" label="Customer" tone="accent" data-testid="a" />
        <LifecycleBadge lifecycle="sales_qualified" data-testid="b" />
      </div>,
    );
    expect(screen.getByTestId('a')).toHaveAttribute('data-tone', 'accent');
    expect(screen.getByTestId('b')).toHaveTextContent('Sales qualified');
    expect(screen.getByTestId('b')).toHaveAttribute('data-tone', 'neutral');
    expect(formatLifecycleLabel('opportunity-won')).toBe('Opportunity won');
  });

  it('hides the dot from assistive technology and can drop it', () => {
    const { rerender } = render(<LifecycleBadge lifecycle="stable" data-testid="a" />);
    const dot = screen.getByTestId('a').querySelector('.mn-lifecycle-badge__dot');
    expect(dot).toHaveAttribute('aria-hidden');
    rerender(<LifecycleBadge lifecycle="stable" dot={false} data-testid="a" />);
    expect(screen.getByTestId('a').querySelector('.mn-lifecycle-badge__dot')).toBeNull();
  });

  it('forwards ref, className and size', () => {
    const ref = createRef<HTMLSpanElement>();
    render(<LifecycleBadge ref={ref} lifecycle="preview" size="sm" className="x" />);
    expect(ref.current).toHaveClass('mn-lifecycle-badge', 'x');
    expect(ref.current).toHaveAttribute('data-size', 'sm');
  });

  it('renders on the server', () => {
    const html = renderToString(<LifecycleBadge lifecycle="preview" />);
    expect(html).toContain('mn-lifecycle-badge');
    expect(html).toContain('Preview');
  });
});
