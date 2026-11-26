import { describe, expect, it } from 'vitest';
import { createRef } from 'react';
import { render, screen } from '@testing-library/react';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { Metric, formatMetricValue } from './index.js';

describe('Metric', () => {
  it('shows the label, a formatted number, its unit and a change', async () => {
    render(
      <main>
        <Metric
          data-testid="m"
          label="Median response"
          value={1234.5}
          locale="en-US"
          unit="ms"
          change={{ value: -0.08, goodDirection: 'down', comparison: 'vs last week' }}
        />
      </main>,
    );
    const root = screen.getByTestId('m');
    expect(root).toHaveClass('mn-metric');
    expect(screen.getByText('Median response')).toHaveClass('mn-metric__label');
    expect(screen.getByText('1,234.5')).toHaveClass('mn-metric__value');
    expect(root.querySelector('.mn-metric__unit')).toHaveTextContent('ms');
    const delta = root.querySelector('.mn-delta-indicator');
    expect(delta).toHaveAttribute('data-tone', 'positive');
    expect(delta).toHaveTextContent('vs last week');
    await expectNoA11yViolations();
  });

  it('formats percent and currency with Intl', () => {
    const { rerender } = render(
      <Metric label="Conversion" value={0.125} format="percent" locale="en-US" />,
    );
    expect(screen.getByText('12.5%')).toBeInTheDocument();
    rerender(
      <Metric label="Revenue" value={1999.5} format="currency" currency="EUR" locale="en-US" />,
    );
    expect(screen.getByText('€1,999.50')).toBeInTheDocument();
  });

  it('gives the full value to assistive technology in compact notation', () => {
    render(<Metric label="Visits" value={12345} notation="compact" locale="en-US" />);
    const short = screen.getByText('12K');
    expect(short).toHaveAttribute('aria-hidden', 'true');
    expect(screen.getByText('12,345')).toHaveClass('mn-visually-hidden');
    expect(short.parentElement).toHaveAttribute('title', '12,345');
  });

  it('shows a dash read as the empty message, and no unit, for a missing value', () => {
    render(
      <Metric
        data-testid="m"
        label="Uptime"
        value={null}
        unit="%"
        messages={{ empty: 'Not measured yet' }}
      />,
    );
    const root = screen.getByTestId('m');
    expect(root).toHaveAttribute('data-empty');
    expect(screen.getByText('Not measured yet')).toHaveClass('mn-visually-hidden');
    expect(root.querySelector('.mn-metric__unit')).toBeNull();
  });

  it('shows text values as given and exposes layout, size and alignment', () => {
    const ref = createRef<HTMLDivElement>();
    render(<Metric ref={ref} label="Plan" value="Team" layout="inline" size="sm" align="end" />);
    expect(screen.getByText('Team')).toBeInTheDocument();
    expect(ref.current).toHaveAttribute('data-layout', 'inline');
    expect(ref.current).toHaveAttribute('data-size', 'sm');
    expect(ref.current).toHaveAttribute('data-align', 'end');
  });

  it('exports the formatter it uses', () => {
    expect(formatMetricValue(0.5, { format: 'percent', locale: 'en-US' })).toBe('50%');
    expect(formatMetricValue(1500, { notation: 'compact', locale: 'en-US' })).toBe('1.5K');
  });

  it('renders on the server', () => {
    const html = renderToString(<Metric label="Open tickets" value={128} locale="en-US" />);
    expect(html).toContain('mn-metric');
    expect(html).toContain('128');
  });
});
