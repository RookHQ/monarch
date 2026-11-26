import { describe, expect, it } from 'vitest';
import { createRef } from 'react';
import { render, screen } from '@testing-library/react';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { StatusDot } from './index.js';

describe('StatusDot', () => {
  it('renders a decorative dot followed by the visible label', async () => {
    render(
      <main>
        <StatusDot label="Running" tone="success" data-testid="dot" />
      </main>,
    );
    const root = screen.getByTestId('dot');
    expect(root).toHaveClass('mn-status-dot');
    expect(root).toHaveAttribute('data-tone', 'success');
    expect(root).toHaveAttribute('data-size', 'sm');
    expect(root.querySelector('.mn-status-dot__dot')).toHaveAttribute('aria-hidden', 'true');
    expect(screen.getByText('Running')).toHaveClass('mn-status-dot__label');
    expect(root).toHaveTextContent('Running');
    await expectNoA11yViolations();
  });

  it('defaults to the neutral tone and exposes size and pulse', () => {
    render(<StatusDot label="Queued" size="md" pulse data-testid="dot" />);
    const root = screen.getByTestId('dot');
    expect(root).toHaveAttribute('data-tone', 'neutral');
    expect(root).toHaveAttribute('data-size', 'md');
    expect(root).toHaveAttribute('data-pulse');
  });

  it('keeps a hidden label available to assistive technology', async () => {
    render(
      <main>
        <StatusDot label="Offline" tone="danger" hideLabel data-testid="dot" />
      </main>,
    );
    const label = screen.getByText('Offline');
    expect(label).toHaveClass('mn-visually-hidden');
    expect(screen.getByTestId('dot')).toHaveAttribute('data-label-hidden');
    await expectNoA11yViolations();
  });

  it('forwards its ref and native attributes', () => {
    const ref = createRef<HTMLSpanElement>();
    render(<StatusDot ref={ref} label="Paused" tone="warning" title="Paused since 09:00" />);
    expect(ref.current).toHaveAttribute('title', 'Paused since 09:00');
  });

  it('renders on the server', () => {
    const html = renderToString(<StatusDot label="Running" tone="success" />);
    expect(html).toContain('mn-status-dot');
    expect(html).toContain('Running');
  });
});
