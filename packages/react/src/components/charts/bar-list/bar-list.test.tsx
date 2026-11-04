import { forwardRef, type AnchorHTMLAttributes } from 'react';
import { describe, expect, it, vi } from 'vitest';
import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { BarList, type BarListItem } from './index.js';

const pages: BarListItem[] = [
  { id: 'pricing', label: '/pricing', value: 420 },
  { id: 'home', label: '/', value: 1200 },
  { id: 'docs', label: '/docs', value: 860 },
];

function bars(container: HTMLElement) {
  return Array.from(container.querySelectorAll<HTMLElement>('.mn-bar-list__bar')).map(
    (b) => b.style.width,
  );
}

describe('BarList', () => {
  it('renders a named list, largest first, with text values and decorative bars', async () => {
    const { container } = render(
      <main>
        <BarList label="Top pages" items={pages} locale="en-US" />
      </main>,
    );
    const list = screen.getByRole('list', { name: 'Top pages' });
    const rows = within(list).getAllByRole('listitem');
    expect(rows.map((r) => r.querySelector('.mn-bar-list__text')?.textContent)).toEqual([
      '/',
      '/docs',
      '/pricing',
    ]);
    expect(within(rows[0]!).getByText('1,200')).toBeInTheDocument();
    expect(bars(container)).toEqual(['100%', `${(860 / 1200) * 100}%`, '35%']);
    expect(container.querySelector('.mn-bar-list__track')).toHaveAttribute('aria-hidden', 'true');
    await expectNoA11yViolations();
  });

  it('keeps the given order with sort="none" and reverses with sort="asc"', () => {
    const { rerender } = render(<BarList items={pages} sort="none" />);
    expect(screen.getAllByRole('listitem')[0]).toHaveTextContent('/pricing');
    rerender(<BarList items={pages} sort="asc" />);
    expect(screen.getAllByRole('listitem')[0]).toHaveTextContent('/pricing');
    expect(screen.getAllByRole('listitem')[2]).toHaveTextContent('/');
  });

  it('numbers rows in an ordered list when ranked', () => {
    render(<BarList ranked label="Rivals" items={pages} />);
    const list = screen.getByRole('list', { name: 'Rivals' });
    expect(list.tagName).toBe('OL');
    expect(list.querySelector('.mn-bar-list__rank')).toHaveTextContent('1');
  });

  it('formats percent, currency, compact and units', () => {
    render(
      <>
        <BarList
          data-testid="p"
          format="percent"
          max={1}
          items={[{ label: 'A', value: 0.125 }]}
          locale="en-US"
        />
        <BarList
          data-testid="c"
          format="currency"
          currency="EUR"
          items={[{ label: 'B', value: 1999.5 }]}
          locale="en-US"
        />
        <BarList
          data-testid="k"
          notation="compact"
          items={[{ label: 'C', value: 12_400 }]}
          locale="en-US"
        />
        <BarList data-testid="u" unit="visits" items={[{ label: 'D', value: 3 }]} locale="en-US" />
        <BarList
          data-testid="f"
          formatValue={(v, item) => `${v} for ${String(item.label)}`}
          items={[{ label: 'E', value: 2 }]}
        />
      </>,
    );
    const value = (id: string) =>
      screen.getByTestId(id).querySelector('.mn-bar-list__value')?.textContent;
    expect(value('p')).toBe('12.5%');
    expect(value('c')).toBe('€1,999.50');
    expect(value('k')).toBe('12K');
    expect(value('u')).toBe('3\u00a0visits');
    expect(value('f')).toBe('2 for E');
    expect(bars(screen.getByTestId('p'))).toEqual(['12.5%']);
  });

  it('announces missing values in words and sorts them last', () => {
    const { container } = render(
      <BarList
        items={[
          { label: 'Unaudited', value: null },
          { label: 'Audited', value: 40 },
        ]}
      />,
    );
    const rows = screen.getAllByRole('listitem');
    expect(rows[1]).toHaveTextContent('Unaudited');
    expect(within(rows[1]!).getByText('No value')).toHaveClass('mn-visually-hidden');
    expect(bars(container)).toEqual(['100%', '0%']);
  });

  it('shows a display value instead of the formatted one', () => {
    render(<BarList items={[{ label: 'Email', value: 0.12, displayValue: '12% · 4 of 33' }]} />);
    expect(screen.getByText('12% · 4 of 33')).toBeInTheDocument();
  });

  it('links rows and moves through them with Tab', async () => {
    const user = userEvent.setup();
    render(
      <BarList
        items={[
          { label: 'Google', value: 50, href: '/referrers/google' },
          { label: 'Newsletter', value: 20, href: '/referrers/newsletter' },
        ]}
      />,
    );
    await user.tab();
    expect(screen.getByRole('link', { name: 'Google' })).toHaveFocus();
    await user.tab();
    expect(screen.getByRole('link', { name: 'Newsletter' })).toHaveFocus();
    expect(screen.getByRole('link', { name: 'Newsletter' })).toHaveAttribute(
      'href',
      '/referrers/newsletter',
    );
    await expectNoA11yViolations();
  });

  it('uses a custom link component', async () => {
    const onNavigate = vi.fn();
    const RouterLink = forwardRef<HTMLAnchorElement, AnchorHTMLAttributes<HTMLAnchorElement>>(
      function RouterLink({ href, children, ...props }, ref) {
        return (
          <a
            ref={ref}
            href={href}
            data-router=""
            onClick={(e) => {
              e.preventDefault();
              onNavigate(href);
            }}
            {...props}
          >
            {children}
          </a>
        );
      },
    );
    render(
      <BarList linkComponent={RouterLink} items={[{ label: 'Docs', value: 1, href: '/docs' }]} />,
    );
    const link = screen.getByRole('link', { name: 'Docs' });
    expect(link).toHaveAttribute('data-router');
    await userEvent.setup().click(link);
    expect(onNavigate).toHaveBeenCalledWith('/docs');
  });

  it('cuts to limit with a "Top N of M" footer', () => {
    const { rerender } = render(<BarList items={pages} limit={2} />);
    expect(screen.getAllByRole('listitem')).toHaveLength(2);
    expect(screen.getByText('Top 2 of 3')).toBeInTheDocument();
    rerender(<BarList items={pages} limit={2} overflowLabel="View all 3 pages" />);
    expect(screen.getByText('View all 3 pages')).toBeInTheDocument();
  });

  it('marks highlighted rows', () => {
    render(
      <BarList
        items={[
          { label: 'You', value: 70, highlighted: true },
          { label: 'Rival', value: 80 },
        ]}
      />,
    );
    expect(screen.getByText('You').closest('li')).toHaveAttribute('data-highlighted');
    expect(screen.getByText('Rival').closest('li')).not.toHaveAttribute('data-highlighted');
  });

  it('renders the empty state', () => {
    const { rerender } = render(<BarList items={[]} />);
    expect(screen.getByText('No data yet')).toBeInTheDocument();
    rerender(<BarList items={[]} emptyState="No referrers in this period" />);
    expect(screen.getByText('No referrers in this period')).toBeInTheDocument();
    expect(screen.queryByRole('list')).toBeNull();
  });

  it('shows hidden placeholder rows while loading', async () => {
    render(<BarList data-testid="l" label="Top pages" items={pages} loading loadingRows={3} />);
    const root = screen.getByTestId('l');
    expect(root).toHaveAttribute('aria-busy', 'true');
    expect(root.querySelectorAll('.mn-skeleton')).toHaveLength(6);
    expect(screen.queryByRole('list')).toBeNull();
    expect(screen.getByText('Loading Top pages')).toBeInTheDocument();
    await expectNoA11yViolations();
  });

  it('handles all-zero values without dividing by zero', () => {
    const { container } = render(
      <BarList
        items={[
          { label: 'A', value: 0 },
          { label: 'B', value: 0 },
        ]}
      />,
    );
    expect(bars(container)).toEqual(['0%', '0%']);
  });

  it('forwards ref and layout', () => {
    let node: HTMLDivElement | null = null;
    render(
      <BarList
        ref={(el) => {
          node = el;
        }}
        layout="inline"
        items={pages}
      />,
    );
    expect(node).toHaveAttribute('data-layout', 'inline');
  });

  it('renders on the server', () => {
    const html = renderToString(<BarList label="Top pages" items={pages} />);
    expect(html).toContain('mn-bar-list');
    expect(html).toContain('/pricing');
  });
});
