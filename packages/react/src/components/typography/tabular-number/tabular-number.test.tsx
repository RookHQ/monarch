import { describe, expect, it } from 'vitest';
import { createRef } from 'react';
import { render, screen } from '@testing-library/react';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { TabularNumber, formatTabularNumber } from './index.js';

describe('formatTabularNumber', () => {
  it('formats with Intl.NumberFormat options and a locale', () => {
    expect(formatTabularNumber(1234.5, { locale: 'en-US', minimumFractionDigits: 2 })).toBe(
      '1,234.50',
    );
    expect(formatTabularNumber(1234.5, { locale: 'de-DE' })).toBe('1.234,5');
    expect(formatTabularNumber(0.125, { locale: 'en-US', style: 'percent' })).toBe('13%');
    expect(formatTabularNumber(12n, { locale: 'en-US', signDisplay: 'always' })).toBe('+12');
  });

  it('falls back instead of throwing for a locale the runtime rejects', () => {
    expect(() => formatTabularNumber(1234.5, { locale: 'en_US' })).not.toThrow();
    expect(() => render(<TabularNumber value={1234.5} locale="en_US" />)).not.toThrow();
    // A bad option is still reported.
    expect(() => formatTabularNumber(1, { style: 'currency' })).toThrow();
  });

  it('reuses formatters for the same options', () => {
    const a = formatTabularNumber(1, { locale: 'en-US', style: 'currency', currency: 'EUR' });
    const b = formatTabularNumber(1, { locale: 'en-US', style: 'currency', currency: 'EUR' });
    expect(a).toBe(b);
    expect(a).toBe('€1.00');
  });
});

describe('TabularNumber', () => {
  it('formats a value, keeps the raw value and forwards ref and props', async () => {
    const ref = createRef<HTMLSpanElement>();
    render(
      <p>
        Total:{' '}
        <TabularNumber
          ref={ref}
          data-testid="n"
          value={48210.5}
          locale="en-US"
          format={{ minimumFractionDigits: 2 }}
        />
      </p>,
    );
    const n = screen.getByTestId('n');
    expect(n).toBe(ref.current);
    expect(n).toHaveClass('mn-tabular-number');
    expect(n).toHaveTextContent('48,210.50');
    expect(n).toHaveAttribute('data-value', '48210.5');
    await expectNoA11yViolations();
  });

  it('wraps already formatted children when no value is given', () => {
    render(<TabularNumber data-testid="n">12:04:59</TabularNumber>);
    expect(screen.getByTestId('n')).toHaveTextContent('12:04:59');
    expect(screen.getByTestId('n')).not.toHaveAttribute('data-value');
  });

  it('shows an em dash with an accessible name for a missing or invalid value', async () => {
    render(
      <ul>
        <li>
          <TabularNumber data-testid="null" value={null} />
        </li>
        <li>
          <TabularNumber data-testid="nan" value={Number.NaN} messages={{ emptyLabel: 'n/a' }} />
        </li>
      </ul>,
    );
    const empty = screen.getByTestId('null');
    expect(empty).toHaveAttribute('data-empty');
    expect(empty).toHaveTextContent('—No value');
    expect(empty.querySelector('[aria-hidden]')).toHaveTextContent('—');
    expect(screen.getByTestId('nan')).toHaveTextContent('n/a');
    await expectNoA11yViolations();
  });

  it('renders on the server with the given locale', () => {
    const html = renderToString(
      <TabularNumber value={0.42} locale="en-US" format={{ style: 'percent' }} />,
    );
    expect(html).toContain('mn-tabular-number');
    expect(html).toContain('42%');
  });
});
