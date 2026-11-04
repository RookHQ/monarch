import { describe, expect, it } from 'vitest';
import { createRef } from 'react';
import { render, screen } from '@testing-library/react';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { Sparkline } from './index.js';

describe('Sparkline', () => {
  it('is decorative without a label', async () => {
    const { container } = render(<Sparkline values={[3, 5, 4, 8]} data-testid="root" />);
    const root = screen.getByTestId('root');
    expect(root).toHaveClass('mn-sparkline');
    expect(root).toHaveAttribute('aria-hidden', 'true');
    expect(root).toHaveAttribute('data-tone', 'current');
    expect(container.querySelector('.mn-sparkline__line')).not.toBeNull();
    // markers="last" by default.
    expect(container.querySelectorAll('.mn-sparkline__dot')).toHaveLength(1);
    await expectNoA11yViolations();
  });

  it('becomes an image with a min / max / latest summary when labelled', async () => {
    render(<Sparkline values={[1200, 900, 1500]} label="Visits" locale="en-US" />);
    expect(
      screen.getByRole('img', { name: 'Visits: latest 1,500, low 900, high 1,500' }),
    ).toBeInTheDocument();
    await expectNoA11yViolations();
  });

  it('takes its tone from thresholds and names the state', () => {
    const { rerender } = render(
      <Sparkline
        values={[120, 180, 420]}
        label="p95 latency"
        thresholds={{ warning: 300, danger: 400 }}
        formatValue={(v) => `${v} ms`}
      />,
    );
    const img = screen.getByRole('img');
    expect(img).toHaveAttribute('data-tone', 'danger');
    expect(img).toHaveAccessibleName(
      'p95 latency: latest 420 ms, low 120 ms, high 420 ms. Critical',
    );
    rerender(
      <Sparkline
        values={[99.9, 99.5, 98]}
        label="Uptime"
        thresholds={{ warning: 99, direction: 'below' }}
      />,
    );
    expect(screen.getByRole('img')).toHaveAttribute('data-tone', 'warning');
    expect(screen.getByRole('img')).toHaveAccessibleName(/Warning$/);
  });

  it('breaks the line at gaps and dots a lone point', () => {
    const { container } = render(<Sparkline values={[1, 2, null, 3, null, 4, 5]} markers="none" />);
    expect(
      container.querySelector('.mn-sparkline__line')!.getAttribute('d')!.match(/M/g),
    ).toHaveLength(2);
    expect(container.querySelectorAll('.mn-sparkline__dot')).toHaveLength(1);
  });

  it('draws higher-is-better ranks upside down with invert', () => {
    const { container, rerender } = render(<Sparkline values={[1, 10]} markers="all" />);
    const normal = [...container.querySelectorAll('circle')].map((c) =>
      Number(c.getAttribute('cy')),
    );
    rerender(<Sparkline values={[1, 10]} markers="all" invert />);
    const inverted = [...container.querySelectorAll('circle')].map((c) =>
      Number(c.getAttribute('cy')),
    );
    expect(normal[0]).toBeGreaterThan(normal[1]!);
    expect(inverted[0]).toBeLessThan(inverted[1]!);
  });

  it('shares a fixed domain so rows compare', () => {
    const { container } = render(
      <Sparkline values={[5, 5]} domain={[0, 10]} markers="all" height={24} />,
    );
    const cy = Number(container.querySelector('circle')!.getAttribute('cy'));
    expect(cy).toBeCloseTo(12, 0);
  });

  it('draws a sparkbar and an area', () => {
    const { container, rerender } = render(<Sparkline variant="bar" values={[2, 0, 3, null, 1]} />);
    expect(container.querySelectorAll('.mn-sparkline__bar')).toHaveLength(4);
    rerender(<Sparkline variant="area" values={[2, 3, 1]} />);
    expect(container.querySelector('.mn-sparkline__area')).not.toBeNull();
  });

  it('says when there is not enough data', () => {
    render(<Sparkline values={[4]} label="Score" labels={{ notEnoughData: 'Too few points' }} />);
    expect(screen.getByText('Too few points')).toBeInTheDocument();
    expect(screen.getByText('Score:')).toHaveClass('mn-visually-hidden');
    expect(document.querySelector('svg')).toBeNull();
  });

  it('uses a palette slot, responsive width and forwards its ref', () => {
    const ref = createRef<HTMLSpanElement>();
    render(<Sparkline ref={ref} values={[1, 2, 3]} color={3} responsive />);
    expect(ref.current).toHaveStyle({ '--_c': 'var(--mn-color-data-3)' });
    expect(ref.current).toHaveAttribute('data-responsive');
    expect(ref.current!.querySelector('svg')).toHaveAttribute('width', '100%');
  });

  it('renders on the server', () => {
    const html = renderToString(<Sparkline values={[1, 3, 2]} label="Trend" responsive />);
    expect(html).toContain('mn-sparkline');
    expect(html).toContain('role="img"');
  });
});
