import { describe, expect, it } from 'vitest';
import { render, screen } from '@testing-library/react';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { DeltaIndicator, formatDelta } from './index.js';

/** The text a screen reader reads: everything except aria-hidden subtrees. */
function spoken(el: Element): string {
  const collect = (node: Node): string => {
    if (node.nodeType === Node.TEXT_NODE) return node.textContent ?? '';
    if (node instanceof Element && node.getAttribute('aria-hidden') === 'true') return '';
    return Array.from(node.childNodes).map(collect).join('');
  };
  return collect(el).replace(/\s+/g, ' ').trim();
}

describe('DeltaIndicator', () => {
  it('shows a signed percentage and speaks direction, comparison and quality', async () => {
    render(
      <main>
        <DeltaIndicator data-testid="d" value={0.12} comparison="vs last month" locale="en-US" />
      </main>,
    );
    const root = screen.getByTestId('d');
    expect(root).toHaveClass('mn-delta-indicator');
    expect(root).toHaveAttribute('data-direction', 'up');
    expect(root).toHaveAttribute('data-tone', 'positive');
    expect(screen.getByText('+12%').closest('[aria-hidden]')).not.toBeNull();
    expect(spoken(root)).toBe('up 12% vs last month, better');
    expect(root.querySelector('svg')).toHaveAttribute('aria-hidden', 'true');
    await expectNoA11yViolations();
  });

  it('treats a fall as good when goodDirection is down (costs)', () => {
    render(<DeltaIndicator data-testid="d" value={-0.08} goodDirection="down" locale="en-US" />);
    const root = screen.getByTestId('d');
    expect(root).toHaveAttribute('data-direction', 'down');
    expect(root).toHaveAttribute('data-tone', 'positive');
    expect(screen.getByText('−8%')).toBeInTheDocument();
    expect(spoken(root)).toBe('down 8%, better');
  });

  it('is neutral in colour when no direction is better', () => {
    render(<DeltaIndicator data-testid="d" value={3} format="number" goodDirection="neither" />);
    expect(screen.getByTestId('d')).toHaveAttribute('data-tone', 'neutral');
    expect(spoken(screen.getByTestId('d'))).toBe('up 3');
  });

  it('computes a percentage from current and previous', () => {
    render(<DeltaIndicator data-testid="d" current={150} previous={200} locale="en-US" />);
    expect(screen.getByText('−25%')).toBeInTheDocument();
    expect(screen.getByTestId('d')).toHaveAttribute('data-tone', 'negative');
    expect(spoken(screen.getByTestId('d'))).toBe('down 25%, worse');
  });

  it('computes an absolute change with a unit', () => {
    render(
      <DeltaIndicator
        data-testid="d"
        current={180}
        previous={225}
        format="number"
        unit="ms"
        goodDirection="down"
        locale="en-US"
      />,
    );
    expect(screen.getByTestId('d').querySelector('.mn-delta-indicator__value')?.textContent).toBe(
      '\u221245\u00a0ms',
    );
    expect(screen.getByTestId('d')).toHaveAttribute('data-tone', 'positive');
  });

  it('formats a currency change through Money', () => {
    render(<DeltaIndicator value={1250} format="currency" currency="USD" locale="en-US" />);
    expect(screen.getByText('+$1,250.00')).toBeInTheDocument();
  });

  it('uses a custom formatter', () => {
    render(<DeltaIndicator value={-2} formatValue={(n) => `${n} places`} goodDirection="down" />);
    expect(screen.getByText('−2 places')).toBeInTheDocument();
  });

  it('shows "No change" for zero, inside the dead band, and when it rounds to zero', () => {
    render(
      <>
        <DeltaIndicator data-testid="a" value={0} comparison="vs last week" />
        <DeltaIndicator data-testid="b" value={0.004} flatThreshold={0.005} />
        <DeltaIndicator data-testid="c" value={0.0001} locale="en-US" />
      </>,
    );
    for (const id of ['a', 'b', 'c']) {
      expect(screen.getByTestId(id)).toHaveAttribute('data-direction', 'flat');
      expect(screen.getByTestId(id)).toHaveAttribute('data-tone', 'neutral');
    }
    expect(spoken(screen.getByTestId('a'))).toBe('No change vs last week');
  });

  it('shows the no-data label when there is nothing to compare', () => {
    render(
      <>
        <DeltaIndicator data-testid="a" value={null} />
        <DeltaIndicator data-testid="b" current={5} previous={0} />
        <DeltaIndicator data-testid="c" current={5} previous={null} />
        <DeltaIndicator data-testid="d" value={Number.NaN} />
      </>,
    );
    for (const id of ['a', 'b', 'c', 'd']) {
      expect(screen.getByTestId(id)).toHaveAttribute('data-empty');
      expect(screen.getByTestId(id)).toHaveTextContent('No earlier data');
    }
  });

  it('translates every word through labels', () => {
    render(
      <DeltaIndicator
        data-testid="d"
        value={0.5}
        locale="de-DE"
        labels={{ up: 'gestiegen', better: 'besser' }}
      />,
    );
    expect(spoken(screen.getByTestId('d'))).toBe('gestiegen 50 %, besser');
  });

  it('supports the pill appearance, sizes and hiding the arrow', () => {
    render(<DeltaIndicator data-testid="d" value={0.1} appearance="pill" size="md" hideIcon />);
    const root = screen.getByTestId('d');
    expect(root).toHaveAttribute('data-appearance', 'pill');
    expect(root).toHaveAttribute('data-size', 'md');
    expect(root.querySelector('svg')).toBeNull();
  });

  it('forwards its ref and native attributes', () => {
    let node: HTMLSpanElement | null = null;
    render(
      <DeltaIndicator
        ref={(el) => {
          node = el;
        }}
        id="x"
        value={1}
      />,
    );
    expect(node).toBeInstanceOf(HTMLSpanElement);
    expect(node!.id).toBe('x');
  });

  it('formatDelta returns the same text and label', () => {
    expect(
      formatDelta({ value: -0.031, comparison: 'vs Q2', goodDirection: 'down', locale: 'en-US' }),
    ).toEqual({
      value: -0.031,
      direction: 'down',
      tone: 'positive',
      text: '−3.1%',
      magnitude: '3.1%',
      label: 'down 3.1% vs Q2, better',
    });
    expect(formatDelta({ value: null }).direction).toBeNull();
  });

  it('renders on the server', () => {
    const html = renderToString(<DeltaIndicator value={0.2} comparison="vs last year" />);
    expect(html).toContain('mn-delta-indicator');
    expect(html).toContain('vs last year');
  });
});
