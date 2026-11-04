import { describe, expect, it, vi } from 'vitest';
import { useState } from 'react';
import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { ChartFrame, chartColor, type ChartTableData } from './index.js';

const TABLE: ChartTableData = {
  columns: [
    { key: 'day', label: 'Day' },
    { key: 'visits', label: 'Visits' },
  ],
  rows: [
    { day: 'Mon', visits: '120' },
    { day: 'Tue', visits: '98' },
  ],
};

describe('ChartFrame', () => {
  it('is a figure named by its title and described by its description', async () => {
    render(
      <ChartFrame title="Visits" description="Last 7 days" table={TABLE} data-testid="root">
        <svg aria-hidden="true" />
      </ChartFrame>,
    );
    const figure = screen.getByRole('figure', { name: 'Visits' });
    expect(figure).toHaveAccessibleDescription('Last 7 days');
    expect(figure).toHaveClass('mn-chart-frame');
    expect(figure).toHaveAttribute('data-status', 'ready');
    await expectNoA11yViolations();
  });

  it('keeps the data table in the page for screen readers while the chart shows', () => {
    render(
      <ChartFrame title="Visits" table={TABLE}>
        <svg aria-hidden="true" />
      </ChartFrame>,
    );
    const table = screen.getByRole('table', { name: 'Visits' });
    expect(table.closest('.mn-visually-hidden')).not.toBeNull();
    expect(within(table).getByRole('rowheader', { name: 'Mon' })).toBeInTheDocument();
    expect(within(table).getByRole('columnheader', { name: 'Visits' })).toBeInTheDocument();
  });

  it('swaps the chart for a visible table with the toggle (uncontrolled)', async () => {
    const onShowTableChange = vi.fn();
    render(
      <ChartFrame title="Visits" table={TABLE} onShowTableChange={onShowTableChange}>
        <svg data-testid="chart" aria-hidden="true" />
      </ChartFrame>,
    );
    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: 'View as table' }));
    expect(onShowTableChange).toHaveBeenCalledWith(true);
    expect(screen.queryByTestId('chart')).toBeNull();
    const region = screen.getByRole('region', { name: 'Visits' });
    expect(region).toHaveAttribute('tabindex', '0');
    expect(within(region).getByRole('table')).toBeInTheDocument();
    expect(screen.getByRole('figure')).toHaveAttribute('data-view', 'table');
    await expectNoA11yViolations();
    await user.click(screen.getByRole('button', { name: 'View as chart' }));
    expect(screen.getByTestId('chart')).toBeInTheDocument();
  });

  it('follows a controlled showTable', async () => {
    function Controlled() {
      const [show, setShow] = useState(true);
      return (
        <>
          <span data-testid="state">{String(show)}</span>
          <ChartFrame title="Visits" table={TABLE} showTable={show} onShowTableChange={setShow}>
            <svg data-testid="chart" aria-hidden="true" />
          </ChartFrame>
        </>
      );
    }
    render(<Controlled />);
    expect(screen.queryByTestId('chart')).toBeNull();
    await userEvent.setup().click(screen.getByRole('button', { name: 'View as chart' }));
    expect(screen.getByTestId('state')).toHaveTextContent('false');
    expect(screen.getByTestId('chart')).toBeInTheDocument();
  });

  it('hides the toggle when tableToggle is off but keeps the hidden table', () => {
    render(
      <ChartFrame title="Visits" table={TABLE} tableToggle={false}>
        <svg aria-hidden="true" />
      </ChartFrame>,
    );
    expect(screen.queryByRole('button')).toBeNull();
    expect(screen.getByRole('table')).toBeInTheDocument();
  });

  it('renders the title as a heading at the given level, and the actions slot', () => {
    render(
      <ChartFrame title="Revenue" headingLevel={2} actions={<button type="button">30 days</button>}>
        <svg aria-hidden="true" />
      </ChartFrame>,
    );
    expect(screen.getByRole('heading', { level: 2, name: 'Revenue' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: '30 days' })).toBeInTheDocument();
  });

  it('shows a busy skeleton while loading', async () => {
    render(<ChartFrame title="Visits" status="loading" table={TABLE} />);
    expect(screen.getByRole('status')).toHaveTextContent('Loading chart');
    expect(document.querySelector('[aria-busy="true"]')).not.toBeNull();
    expect(screen.queryByRole('button', { name: 'View as table' })).toBeNull();
    await expectNoA11yViolations();
  });

  it('shows an empty state with its description and actions', async () => {
    render(
      <ChartFrame
        title="Visits"
        status="empty"
        emptyDescription="Visits appear after the first day."
        emptyActions={<button type="button">Set up tracking</button>}
      />,
    );
    expect(screen.getByText('No data yet')).toBeInTheDocument();
    expect(screen.getByText('Visits appear after the first day.')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Set up tracking' })).toBeInTheDocument();
    await expectNoA11yViolations();
  });

  it('shows an error with Try again when onRetry is set', async () => {
    const onRetry = vi.fn();
    render(<ChartFrame title="Visits" status="error" onRetry={onRetry} />);
    expect(screen.getByText("This chart didn't load.")).toBeInTheDocument();
    await userEvent.setup().click(screen.getByRole('button', { name: 'Try again' }));
    expect(onRetry).toHaveBeenCalledTimes(1);
    await expectNoA11yViolations();
  });

  it('announces an error as an alert without onRetry, with translated labels', () => {
    render(<ChartFrame title="Visits" status="error" labels={{ error: 'Kein Diagramm.' }} />);
    expect(screen.getByRole('alert')).toHaveTextContent('Kein Diagramm.');
  });

  it('renders legend entries as toggles with aria-pressed', async () => {
    const onLegendToggle = vi.fn();
    render(
      <ChartFrame
        title="Visits"
        legend={[
          { key: 'a', label: 'Organic', color: 1 },
          { key: 'b', label: 'Paid', color: 2, hidden: true, dashed: true },
        ]}
        onLegendToggle={onLegendToggle}
      >
        <svg aria-hidden="true" />
      </ChartFrame>,
    );
    const list = screen.getByRole('list', { name: 'Legend' });
    const paid = within(list).getByRole('button', { name: 'Paid' });
    expect(paid).toHaveAttribute('aria-pressed', 'false');
    expect(within(list).getByRole('button', { name: 'Organic' })).toHaveAttribute(
      'aria-pressed',
      'true',
    );
    await userEvent.setup().click(paid);
    expect(onLegendToggle).toHaveBeenCalledWith('b');
    await expectNoA11yViolations();
  });

  it('renders a static legend without toggles', () => {
    render(
      <ChartFrame title="Visits" legend={[{ key: 'a', label: 'Organic', color: 1 }]}>
        <svg aria-hidden="true" />
      </ChartFrame>,
    );
    expect(screen.queryByRole('button')).toBeNull();
    expect(screen.getByText('Organic')).toBeInTheDocument();
  });

  it('maps palette slots to data tokens only', () => {
    expect(chartColor(1)).toBe('var(--mn-color-data-1)');
    expect(chartColor(5)).toBe('var(--mn-color-data-5)');
    expect(chartColor('neutral')).toBe('var(--mn-color-text-subtle)');
  });

  it('renders on the server', () => {
    const html = renderToString(
      <ChartFrame title="Visits" table={TABLE}>
        <svg aria-hidden="true" />
      </ChartFrame>,
    );
    expect(html).toContain('mn-chart-frame');
    expect(html).toContain('View as table');
  });
});
