import { createRef } from 'react';
import { describe, expect, it } from 'vitest';
import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { ComparisonSection, type ComparisonColumn, type ComparisonRow } from './index.js';

const columns: ComparisonColumn[] = [
  { id: 'ours', name: 'Product A', highlighted: true, badge: 'This product' },
  { id: 'sheets', name: 'Spreadsheets' },
];

const rows: ComparisonRow[] = [
  { id: 'history', label: 'Change history', values: { ours: true, sheets: 'Per file' } },
  { id: 'roles', label: 'Roles', group: 'Access', values: { ours: true, sheets: false } },
];

describe('ComparisonSection', () => {
  it('forwards its ref to the section', () => {
    const ref = createRef<HTMLElement>();
    render(<ComparisonSection ref={ref} title="Compare" columns={columns} rows={rows} />);
    expect(ref.current).toHaveClass('mn-comparison-section');
  });

  it('is a region with a comparison table named by the title', async () => {
    render(
      <ComparisonSection
        eyebrow="Compare"
        title="How it compares"
        description="A side-by-side view."
        columns={columns}
        rows={rows}
      />,
    );
    const region = screen
      .getByRole('heading', { level: 2, name: 'How it compares' })
      .closest('section')!;
    expect(region).toHaveClass('mn-comparison-section');
    // One landmark carries the title: the table's scroll region.
    expect(screen.getByRole('region', { name: 'How it compares' })).toHaveClass(
      'mn-pricing-table__scroller',
    );
    const table = within(region).getByRole('table', { name: 'How it compares' });
    expect(within(table).getByRole('columnheader', { name: /Product A/ })).toHaveAttribute(
      'data-highlighted',
    );
    expect(within(table).getByRole('columnheader', { name: /Product A/ })).toHaveTextContent(
      'This product',
    );
    expect(within(table).getByRole('columnheader', { name: 'Access' })).toBeInTheDocument();
    await expectNoA11yViolations();
  });

  it('writes values as text: checks, dashes and plain values', () => {
    render(<ComparisonSection title="Compare" columns={columns} rows={rows} />);
    const history = screen.getByRole('rowheader', { name: 'Change history' }).closest('tr')!;
    const [ours, sheets] = within(history).getAllByRole('cell');
    expect(ours).toHaveTextContent('Included');
    expect(sheets).toHaveTextContent('Per file');
    const roles = screen.getByRole('rowheader', { name: 'Roles' }).closest('tr')!;
    expect(within(roles).getAllByRole('cell')[1]).toHaveTextContent('Not included');
  });

  it('shows best-for cards, small print and actions; the table scroller is keyboard focusable', async () => {
    render(
      <ComparisonSection
        title="Compare"
        caption="Product A and spreadsheets"
        columns={columns}
        rows={rows}
        bestFor={[
          { id: 'a', title: 'Product A suits', description: 'Shared work.', highlighted: true },
          { id: 'b', title: 'Spreadsheets suit', description: 'One-off lists.' },
        ]}
        note="Checked against public documentation."
        actions={<a href="#try">Try it</a>}
      />,
    );
    expect(screen.getByRole('table', { name: 'Product A and spreadsheets' })).toBeInTheDocument();
    expect(screen.getByRole('heading', { level: 3, name: 'Product A suits' })).toBeInTheDocument();
    expect(screen.getByText('Checked against public documentation.')).toBeInTheDocument();
    await userEvent.tab();
    expect(screen.getByRole('region', { name: 'Product A and spreadsheets' })).toHaveFocus();
    await userEvent.tab();
    expect(screen.getByRole('link', { name: 'Try it' })).toHaveFocus();
    await expectNoA11yViolations();
  });

  it('translates its text through messages', () => {
    render(
      <ComparisonSection
        title="Comparar"
        columns={columns}
        rows={rows}
        messages={{ included: 'Incluido', notIncluded: 'No incluido' }}
      />,
    );
    const roles = screen.getByRole('rowheader', { name: 'Roles' }).closest('tr')!;
    expect(within(roles).getAllByRole('cell')[0]).toHaveTextContent('Incluido');
    expect(within(roles).getAllByRole('cell')[1]).toHaveTextContent('No incluido');
  });

  it('renders on the server', () => {
    const html = renderToString(
      <ComparisonSection title="Compare" columns={columns} rows={rows} />,
    );
    expect(html).toContain('mn-comparison-section');
    expect(html).toContain('Change history');
  });
});
