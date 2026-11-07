import { describe, expect, it, vi } from 'vitest';
import { createRef } from 'react';
import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { AiTableAnswer, inferColumnType, inferColumns, toCsv } from './index.js';

const rows = [
  { region: 'North', revenue: '1,200', updated: '2026-03-04' },
  { region: 'South', revenue: 950, updated: '2026-01-15' },
  { region: 'East', revenue: '12,000', updated: '2026-02-20' },
];

function live() {
  return document.querySelector('.mn-ai-table-answer__live');
}

function bodyColumn(index: number) {
  const body = screen.getAllByRole('rowgroup')[1] as HTMLElement;
  return within(body)
    .getAllByRole('row')
    .map((r) => within(r).getAllByRole('cell')[index]?.textContent);
}

describe('inferColumnType / inferColumns', () => {
  it('detects numbers, dates and text, ignoring blanks', () => {
    expect(inferColumnType([1, '2,300', '-4.5', '12%', null, ''])).toBe('number');
    expect(inferColumnType(['2026-01-01', '2026-02-03T10:00:00Z', new Date(0)])).toBe('date');
    expect(inferColumnType(['12', 'twelve'])).toBe('text');
    expect(inferColumnType([])).toBe('text');
    expect(inferColumns(rows)).toEqual([
      { key: 'region', type: 'text' },
      { key: 'revenue', type: 'number' },
      { key: 'updated', type: 'date' },
    ]);
  });
});

describe('toCsv', () => {
  it('escapes quotes, commas, line breaks and edge spaces with CRLF rows', () => {
    const csv = toCsv(
      [{ a: 'He said "hi"', b: 'x,y', c: 'line\nbreak', d: ' padded', e: null }],
      [{ key: 'a', label: 'A' }, { key: 'b' }, { key: 'c' }, { key: 'd' }, { key: 'e' }],
    );
    expect(csv).toBe('A,b,c,d,e\r\n"He said ""hi""","x,y","line\nbreak"," padded",');
  });

  it('neutralises formulas in text but keeps negative numbers', () => {
    expect(toCsv([{ a: '=SUM(A1)', b: '-3', c: -3, d: '@cmd' }])).toBe(
      "a,b,c,d\r\n'=SUM(A1),-3,-3,'@cmd",
    );
  });
});

describe('AiTableAnswer', () => {
  it('renders a labelled, scrollable table with inferred alignment, forwarding ref and props', async () => {
    const ref = createRef<HTMLDivElement>();
    render(
      <AiTableAnswer
        ref={ref}
        data-testid="root"
        title="Revenue by region"
        rows={rows}
        locale="en-US"
      />,
    );
    const root = screen.getByTestId('root');
    expect(root).toHaveClass('mn-ai-table-answer');
    expect(ref.current).toBe(root);
    const region = screen.getByRole('region', { name: 'AI table Revenue by region' });
    expect(region).toHaveAttribute('tabindex', '0');
    expect(screen.getByRole('table', { name: 'AI table: Revenue by region' })).toBeInTheDocument();
    expect(screen.getByText('3 rows')).toBeInTheDocument();
    expect(screen.getByRole('columnheader', { name: 'revenue' })).toHaveAttribute(
      'data-align',
      'end',
    );
    expect(bodyColumn(1)).toEqual(['1,200', '950', '12,000']);
    await expectNoA11yViolations();
  });

  it('sorts numerically and by date from the keyboard, with aria-sort and an announcement', async () => {
    const user = userEvent.setup();
    render(<AiTableAnswer rows={rows} />);
    const revenue = screen.getByRole('button', { name: 'revenue' });
    revenue.focus();
    await user.keyboard('{Enter}');
    expect(screen.getByRole('columnheader', { name: 'revenue' })).toHaveAttribute(
      'aria-sort',
      'ascending',
    );
    expect(bodyColumn(1)).toEqual(['950', '1,200', '12,000']);
    expect(live()).toHaveTextContent('Sorted by revenue, ascending.');
    await user.keyboard(' ');
    expect(bodyColumn(1)).toEqual(['12,000', '1,200', '950']);
    await user.click(screen.getByRole('button', { name: 'updated' }));
    expect(bodyColumn(0)).toEqual(['South', 'East', 'North']);
    expect(screen.getByRole('columnheader', { name: 'revenue' })).toHaveAttribute(
      'aria-sort',
      'none',
    );
  });

  it('copies the sorted rows as CSV', async () => {
    const user = userEvent.setup();
    const writeText = vi.spyOn(navigator.clipboard, 'writeText');
    render(
      <AiTableAnswer
        rows={rows}
        columns={[
          { key: 'region', label: 'Region' },
          { key: 'revenue', label: 'Revenue' },
        ]}
      />,
    );
    await user.click(screen.getByRole('button', { name: 'Revenue' }));
    await user.click(screen.getByRole('button', { name: 'Copy as CSV' }));
    expect(writeText).toHaveBeenCalledWith(
      'Region,Revenue\r\nSouth,950\r\nNorth,"1,200"\r\nEast,"12,000"',
    );
  });

  it('streams rows with a busy table, then announces completion; opens as artifact', async () => {
    const onOpen = vi.fn();
    const { rerender } = render(
      <AiTableAnswer rows={rows.slice(0, 1)} streaming onOpenAsArtifact={onOpen} />,
    );
    expect(screen.getByRole('table')).toHaveAttribute('aria-busy', 'true');
    expect(screen.getByText('More rows are being generated')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Open as artifact' })).toBeDisabled();
    rerender(<AiTableAnswer rows={rows} onOpenAsArtifact={onOpen} />);
    expect(screen.getByRole('table')).not.toHaveAttribute('aria-busy');
    expect(live()).toHaveTextContent('Table complete.');
    await userEvent.click(screen.getByRole('button', { name: 'Open as artifact' }));
    expect(onOpen).toHaveBeenCalledTimes(1);
    await expectNoA11yViolations();
  });

  it('shows empty cells and an empty table, and never renders model HTML', () => {
    const { rerender, container } = render(
      <AiTableAnswer rows={[{ a: '<img src=x onerror=alert(1)>', b: null }]} sortable={false} />,
    );
    expect(container.querySelector('img')).toBeNull();
    expect(screen.getByText('Empty')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'a' })).toBeNull();
    rerender(<AiTableAnswer rows={[]} />);
    expect(screen.getByText('No rows.')).toBeInTheDocument();
  });

  it('renders on the server', () => {
    const html = renderToString(<AiTableAnswer rows={rows} streaming />);
    expect(html).toContain('mn-ai-table-answer');
    expect(html).toContain('aria-busy="true"');
  });
});
