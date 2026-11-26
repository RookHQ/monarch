import type { ReactNode } from 'react';
import { describe, expect, it, vi } from 'vitest';
import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { StatCard, StatCardGroup } from './index.js';

describe('StatCard', () => {
  it('renders semantically', async () => {
    render(
      <main>
        <StatCard
          label="Pipeline"
          value="₹12.4L"
          delta="+12%"
          deltaDirection="up"
          caption="vs last month"
        />
        <StatCard label="Churn" value="2.1%" delta="+0.3%" deltaDirection="up" deltaTone="danger" />
      </main>,
    );
    expect(screen.getAllByText(/increase/)).toHaveLength(2);
    expect(screen.getAllByText(/increase/)[0]).toHaveClass('mn-visually-hidden');
    expect(screen.getByText('+0.3%').closest('.mn-stat__delta')).toHaveAttribute(
      'data-tone',
      'danger',
    );
    await expectNoA11yViolations();
  });

  it('keeps the legacy markup when only the original props are used', () => {
    const { container } = render(
      <StatCard
        label="Won"
        value="12"
        delta="+1"
        deltaDirection="up"
        caption="vs last week"
        visual="v"
      />,
    );
    const root = container.firstElementChild!;
    expect(root.className).toBe('mn-stat');
    expect(root.querySelector('.mn-stat__body')?.tagName).toBe('DIV');
    expect(root.querySelector('.mn-stat__label')?.tagName).toBe('P');
    expect(root.querySelector('.mn-stat__value')?.tagName).toBe('P');
    expect(root.querySelector('.mn-stat__caption')).toHaveTextContent('vs last week');
    expect(root.querySelector('.mn-stat__visual')).toHaveAttribute('aria-hidden', 'true');
    for (const attr of ['data-size', 'data-variant', 'data-tone', 'data-state', 'aria-busy']) {
      expect(root).not.toHaveAttribute(attr);
    }
  });

  it('formats numbers, percentages and currency with Intl', () => {
    render(
      <>
        <StatCard data-testid="n" label="Visits" value={12840} format="number" locale="en-US" />
        <StatCard
          data-testid="p"
          label="Reply rate"
          value={0.0812}
          format="percent"
          locale="en-US"
        />
        <StatCard
          data-testid="c"
          label="MRR"
          value={482000}
          format="currency"
          currency="INR"
          locale="en-IN"
          formatOptions={{ maximumFractionDigits: 0 }}
        />
        <StatCard data-testid="raw" label="Raw" value={12840} />
      </>,
    );
    const value = (id: string) => screen.getByTestId(id).querySelector('.mn-stat__value');
    expect(value('n')).toHaveTextContent('12,840');
    expect(value('p')).toHaveTextContent('8.1%');
    expect(value('c')).toHaveTextContent('₹4,82,000');
    expect(value('c')?.querySelector('.mn-money')).not.toBeNull();
    expect(value('raw')).toHaveTextContent('12840');
  });

  it('gives assistive technology the full value in compact notation', () => {
    render(
      <StatCard
        label="Views"
        value={1_250_000}
        format="number"
        notation="compact"
        locale="en-US"
      />,
    );
    expect(screen.getByText('1.3M')).toHaveAttribute('aria-hidden', 'true');
    expect(screen.getByText('1,250,000')).toHaveClass('mn-visually-hidden');
  });

  it('shows a unit and a hint line', () => {
    render(<StatCard label="p95" value={182} unit="ms" hint="Across 14 endpoints" />);
    expect(screen.getByText('ms')).toHaveClass('mn-stat__unit');
    expect(screen.getByText('Across 14 endpoints')).toHaveClass('mn-stat__hint');
  });

  it('renders a polarity-aware change with the caption as its comparison', () => {
    const { container } = render(
      <StatCard
        label="Cost per lead"
        value="$41"
        change={{ current: 41, previous: 50, goodDirection: 'down' }}
        caption="vs last month"
      />,
    );
    const change = container.querySelector('.mn-delta-indicator')!;
    expect(change).toHaveAttribute('data-tone', 'positive');
    expect(change).toHaveTextContent('−18%');
    expect(change).toHaveTextContent('vs last month');
    expect(container.querySelector('.mn-stat__caption')).toBeNull();
    expect(container.querySelector('.mn-stat__delta')).toBeNull();
  });

  it('opens the help popover from a named button with the keyboard', async () => {
    const user = userEvent.setup();
    render(
      <StatCard
        label="First response"
        value="2h 10m"
        help="Median time from a ticket being opened to the first agent reply."
      />,
    );
    const button = screen.getByRole('button', { name: 'About First response' });
    await user.tab();
    expect(button).toHaveFocus();
    await user.keyboard('{Enter}');
    expect(await screen.findByText(/Median time from a ticket/)).toBeInTheDocument();
    expect(button).toHaveAttribute('aria-expanded', 'true');
    await user.keyboard('{Escape}');
    expect(button).toHaveAttribute('aria-expanded', 'false');
    expect(button).toHaveFocus();
  });

  it('uses helpLabel when the label is not plain text', () => {
    render(
      <StatCard
        label={<span>Uptime</span>}
        value="99.9%"
        help="Last 30 days."
        helpLabel="How uptime is measured"
      />,
    );
    expect(screen.getByRole('button', { name: 'How uptime is measured' })).toBeInTheDocument();
  });

  it('links the label and makes the card a drill-down target', async () => {
    const user = userEvent.setup();
    const { container } = render(
      <StatCard label="Overdue invoices" value="7" href="/invoices?status=overdue" />,
    );
    const link = screen.getByRole('link', { name: 'Overdue invoices' });
    expect(link).toHaveAttribute('href', '/invoices?status=overdue');
    expect(container.firstElementChild).toHaveAttribute('data-linked');
    await user.tab();
    expect(link).toHaveFocus();
    await expectNoA11yViolations();
  });

  it('accepts a router link component', () => {
    function RouterLink({
      children,
      ...props
    }: {
      href: string;
      className?: string;
      children?: ReactNode;
    }) {
      return (
        <a data-router="" {...props}>
          {children}
        </a>
      );
    }
    render(<StatCard label="Leads" value="40" href="/leads" linkComponent={RouterLink} />);
    expect(screen.getByRole('link', { name: 'Leads' })).toHaveAttribute('data-router');
  });

  it('shows a busy placeholder while loading', async () => {
    const { container } = render(
      <StatCard label="Revenue" value="$4,200" delta="+3%" hint="Hint" loading />,
    );
    const root = container.firstElementChild!;
    expect(root).toHaveAttribute('aria-busy', 'true');
    expect(root).toHaveAttribute('data-state', 'loading');
    expect(root.querySelector('.mn-skeleton')).not.toBeNull();
    expect(screen.queryByText('$4,200')).toBeNull();
    expect(screen.queryByText('Hint')).toBeNull();
    expect(screen.getByText('Revenue')).toBeInTheDocument();
    await expectNoA11yViolations();
  });

  it('shows a dash read as "No data" when the value is missing', () => {
    const { container, rerender } = render(<StatCard label="NPS" value={null} />);
    expect(container.firstElementChild).toHaveAttribute('data-state', 'empty');
    expect(screen.getByText('No data')).toHaveClass('mn-visually-hidden');
    rerender(<StatCard label="NPS" value={undefined} emptyLabel="Not enough responses" />);
    expect(screen.getByText('Not enough responses')).toBeInTheDocument();
  });

  it('shows an error, with Try again when onRetry is given', async () => {
    const onRetry = vi.fn();
    const { container, rerender } = render(
      <StatCard label="Signups" value={10} error="Couldn't load signups." />,
    );
    expect(container.firstElementChild).toHaveAttribute('data-state', 'error');
    expect(screen.getByText("Couldn't load signups.")).toBeInTheDocument();
    expect(screen.queryByRole('button')).toBeNull();
    rerender(
      <StatCard label="Signups" value={10} error="Couldn't load signups." onRetry={onRetry} />,
    );
    await userEvent.setup().click(screen.getByRole('button', { name: 'Try again' }));
    expect(onRetry).toHaveBeenCalledTimes(1);
    await expectNoA11yViolations();
  });

  it('renders dt/dd inside a dl with description-list semantics', async () => {
    const { container } = render(
      <StatCard
        semantics="description-list"
        label="Active users"
        value="1,204"
        change={{ value: 0.04 }}
        hint="Signed in this week"
        sparkline={<svg />}
      />,
    );
    const dl = container.querySelector('dl.mn-stat__body')!;
    expect(dl.querySelector('dt')).toHaveTextContent('Active users');
    expect(Array.from(dl.querySelectorAll('dd')).map((d) => d.className)).toEqual([
      'mn-stat__value',
      'mn-stat__footer',
      'mn-stat__hint',
      'mn-stat__sparkline',
    ]);
    expect(dl.querySelector('.mn-stat__sparkline')).toHaveAttribute('aria-hidden', 'true');
    await expectNoA11yViolations();
  });

  it('exposes tone, size and variant as data attributes and hides the icon', () => {
    const { container } = render(
      <StatCard label="Errors" value="12" tone="danger" size="lg" variant="plain" icon={<svg />} />,
    );
    const root = container.firstElementChild!;
    expect(root).toHaveAttribute('data-tone', 'danger');
    expect(root).toHaveAttribute('data-size', 'lg');
    expect(root).toHaveAttribute('data-variant', 'plain');
    expect(root.querySelector('.mn-stat__icon')).toHaveAttribute('aria-hidden', 'true');
  });

  it('forwards its ref', () => {
    let node: HTMLDivElement | null = null;
    render(
      <StatCard
        ref={(el) => {
          node = el;
        }}
        label="A"
        value="1"
      />,
    );
    expect(node).toHaveClass('mn-stat');
  });

  it('renders on the server', () => {
    const html = renderToString(
      <StatCardGroup label="This month" columns={3}>
        <StatCard label="Won" value="12" delta="+1" deltaDirection="up" />
        <StatCard
          label="Cost"
          value={1200}
          format="currency"
          currency="USD"
          help="Total spend."
          change={{ value: -0.1, goodDirection: 'down' }}
        />
      </StatCardGroup>,
    );
    expect(html).toContain('mn-stat');
    expect(html).toContain('mn-stat-group');
  });
});

describe('StatCardGroup', () => {
  it('is a named group with a column count', async () => {
    render(
      <StatCardGroup label="Pipeline" columns={4} data-testid="g">
        <StatCard label="New" value="12" />
        <StatCard label="Won" value="3" />
      </StatCardGroup>,
    );
    const group = screen.getByRole('group', { name: 'Pipeline' });
    expect(group).toHaveAttribute('data-columns');
    expect(group.style.getPropertyValue('--mn-stat-group-columns')).toBe('4');
    expect(group.style.getPropertyValue('--mn-stat-group-columns-narrow')).toBe('2');
    expect(within(group).getByText('Won')).toBeInTheDocument();
    await expectNoA11yViolations();
  });

  it('has no role without a label', () => {
    render(<StatCardGroup data-testid="g" minItemWidth="10rem" />);
    expect(screen.getByTestId('g')).not.toHaveAttribute('role');
    expect(screen.getByTestId('g').style.getPropertyValue('--mn-stat-group-min')).toBe('10rem');
  });
});
