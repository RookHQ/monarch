import { describe, expect, it, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { createRef } from 'react';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { KPIRow, type KPIRowItem } from './index.js';

const items: KPIRowItem[] = [
  {
    id: 'open',
    label: 'Open',
    value: 128,
    format: 'number',
    locale: 'en',
    change: { value: 0.12, goodDirection: 'down', locale: 'en' },
    caption: 'vs last 30 days',
  },
  { id: 'sla', label: 'Within target', value: 0.94, format: 'percent', locale: 'en' },
  { id: 'csat', label: 'Satisfaction', value: null },
];

describe('KPIRow', () => {
  it('renders a named group of stat cards with their changes; forwards props and ref', async () => {
    const ref = createRef<HTMLDivElement>();
    render(
      <KPIRow ref={ref} data-testid="root" className="extra" label="This month" items={items} />,
    );
    const root = screen.getByTestId('root');
    expect(root).toHaveClass('mn-kpi-row', 'mn-stat-group', 'extra');
    expect(ref.current).toBe(root);
    expect(screen.getByRole('group', { name: 'This month' })).toBe(root);
    expect(screen.getByText('Open')).toBeInTheDocument();
    expect(screen.getByText('128')).toBeInTheDocument();
    expect(screen.getByText('94%')).toBeInTheDocument();
    // An increase where down is good is spoken as worse, not by colour alone.
    expect(root).toHaveTextContent(/worse/);
    // A missing value is spoken, not left blank.
    expect(screen.getByText('No data')).toBeInTheDocument();
    await expectNoA11yViolations();
  });

  it('shows placeholder cards while the first load runs', async () => {
    render(<KPIRow data-testid="root" label="Key figures" items={[]} loading loadingCount={3} />);
    const root = screen.getByTestId('root');
    expect(root).toHaveAttribute('aria-busy', 'true');
    expect(root).toHaveAttribute('data-state', 'loading');
    expect(root.querySelectorAll('.mn-kpi-row__item')).toHaveLength(3);
    await expectNoA11yViolations();
  });

  it('keeps labels and shows value placeholders while refreshing known items', () => {
    render(<KPIRow data-testid="root" items={items} loading />);
    expect(screen.getByText('Open')).toBeInTheDocument();
    expect(screen.queryByText('128')).not.toBeInTheDocument();
  });

  it('replaces the row with an error and Try again', async () => {
    const onRetry = vi.fn();
    const user = userEvent.setup();
    render(
      <KPIRow data-testid="root" items={items} error="Figures did not load." onRetry={onRetry} />,
    );
    expect(screen.getByTestId('root')).toHaveAttribute('data-state', 'error');
    expect(screen.getByText('Figures did not load.')).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Try again' }));
    expect(onRetry).toHaveBeenCalledTimes(1);
    await expectNoA11yViolations();
  });

  it('makes a scrolling row a keyboard stop', async () => {
    const user = userEvent.setup();
    render(<KPIRow data-testid="root" label="Types" items={items} overflow="scroll" />);
    await user.tab();
    expect(screen.getByTestId('root')).toHaveFocus();
    expect(screen.getByTestId('root')).toHaveAttribute('data-overflow', 'scroll');
  });

  it('passes size and variant to every card and sets columns', () => {
    render(<KPIRow data-testid="root" items={items} size="sm" variant="plain" columns={3} />);
    const cards = screen.getByTestId('root').querySelectorAll('.mn-stat');
    expect(cards).toHaveLength(3);
    cards.forEach((card) => {
      expect(card).toHaveAttribute('data-size', 'sm');
      expect(card).toHaveAttribute('data-variant', 'plain');
    });
    expect(screen.getByTestId('root')).toHaveAttribute('data-columns');
  });

  it('renders on the server', () => {
    const html = renderToString(<KPIRow label="This month" items={items} />);
    expect(html).toContain('mn-kpi-row');
    expect(html).toContain('Within target');
  });
});
