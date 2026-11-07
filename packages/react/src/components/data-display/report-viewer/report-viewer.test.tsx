import { describe, expect, it, vi } from 'vitest';
import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { createRef } from 'react';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { ReportViewer, type ReportColumn } from './index.js';

type Row = { team: string; headcount: number; remote: boolean; lead: string | null };
const columns: ReportColumn<Row>[] = [
  { key: 'team', label: 'Team' },
  { key: 'headcount', label: 'Headcount' },
  { key: 'remote', label: 'Remote' },
  { key: 'lead', label: 'Lead' },
];
const rows: Row[] = [
  { team: 'Design', headcount: 1200, remote: true, lead: 'Ada Moss' },
  { team: 'Support', headcount: 8, remote: false, lead: null },
];

describe('ReportViewer', () => {
  it('renders the header, meta, filters and a table named by the title; forwards props and ref', async () => {
    const ref = createRef<HTMLDivElement>();
    render(
      <ReportViewer
        ref={ref}
        data-testid="root"
        className="extra"
        title="Headcount by team"
        description="People on the payroll at the end of the period."
        back={<a href="/reports">All reports</a>}
        filters={<button type="button">Department</button>}
        scope="All locations · October 2026"
        generatedAt="2026-11-07T10:00:00Z"
        locale="en-GB"
        timeZone="UTC"
        columns={columns}
        rows={rows}
        notes={['Contractors are excluded.']}
      />,
    );
    const root = screen.getByTestId('root');
    expect(root).toHaveClass('mn-report-viewer', 'extra');
    expect(ref.current).toBe(root);
    expect(
      screen.getByRole('heading', { level: 1, name: 'Headcount by team' }),
    ).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'All reports' })).toBeInTheDocument();
    expect(screen.getByRole('group', { name: 'Report filters' })).toBeInTheDocument();
    expect(screen.getByText('All locations · October 2026')).toBeInTheDocument();
    expect(root.querySelector('time')).toHaveAttribute('datetime', '2026-11-07T10:00:00.000Z');
    expect(root.querySelector('time')).toHaveTextContent(/^Generated 7 Nov 2026/);
    const table = screen.getByRole('table', { name: 'Headcount by team' });
    const cells = within(table).getAllByRole('cell');
    expect(cells[1]).toHaveTextContent('1,200');
    expect(cells[2]).toHaveTextContent('Yes');
    expect(cells[7]).toHaveTextContent('None');
    expect(screen.getByText('Contractors are excluded.')).toBeInTheDocument();
    await expectNoA11yViolations();
  });

  it('downloads through the header button', async () => {
    const onDownload = vi.fn();
    const user = userEvent.setup();
    render(
      <ReportViewer
        title="Leave balances"
        columns={columns}
        rows={rows}
        download={{ onDownload }}
      />,
    );
    await user.click(screen.getByRole('button', { name: 'Download CSV' }));
    expect(onDownload).toHaveBeenCalledWith('csv');
  });

  it('disables the download while loading and shows a placeholder', async () => {
    render(
      <ReportViewer
        data-testid="root"
        title="Leave balances"
        loading
        download={{ onDownload: () => {} }}
        summary={<p>Summary</p>}
      />,
    );
    expect(screen.getByTestId('root')).toHaveAttribute('aria-busy', 'true');
    expect(screen.getByRole('img', { name: 'Loading report' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Download CSV' })).toBeDisabled();
    expect(screen.queryByText('Summary')).not.toBeInTheDocument();
    await expectNoA11yViolations();
  });

  it('keeps the previous result visible while refreshing', () => {
    render(<ReportViewer data-testid="root" title="R" refreshing columns={columns} rows={rows} />);
    const root = screen.getByTestId('root');
    expect(root).toHaveAttribute('data-state', 'refreshing');
    expect(root).toHaveAttribute('aria-busy', 'true');
    expect(screen.getByRole('table')).toBeInTheDocument();
  });

  it('shows errors by kind, with retry only where it can help', async () => {
    const onRetry = vi.fn();
    const user = userEvent.setup();
    const { rerender } = render(
      <ReportViewer title="R" error="The server timed out." onRetry={onRetry} />,
    );
    expect(screen.getByText('This report could not be run.')).toBeInTheDocument();
    expect(screen.getByText('The server timed out.')).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Try again' }));
    expect(onRetry).toHaveBeenCalled();
    rerender(<ReportViewer title="R" error errorKind="forbidden" onRetry={onRetry} />);
    expect(screen.getByText('You cannot open this report')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Try again' })).not.toBeInTheDocument();
  });

  it('shows the empty state, truncation note, custom body and custom cells', () => {
    const { rerender } = render(<ReportViewer title="R" columns={columns} rows={[]} />);
    expect(screen.getByText('No rows match these filters.')).toBeInTheDocument();
    rerender(
      <ReportViewer
        title="R"
        truncated
        columns={[{ key: 'team', label: 'Team', render: (r: Row) => <strong>{r.team}!</strong> }]}
        rows={rows}
      />,
    );
    expect(screen.getByText('Design!')).toBeInTheDocument();
    expect(screen.getByText(/Only the first rows are shown/)).toBeInTheDocument();
    rerender(
      <ReportViewer title="R" chart={<p>Chart</p>}>
        <p>Pivot</p>
      </ReportViewer>,
    );
    expect(screen.getByText('Chart')).toBeInTheDocument();
    expect(screen.getByText('Pivot')).toBeInTheDocument();
  });

  it('renders on the server', () => {
    const html = renderToString(
      <ReportViewer
        title="Headcount"
        columns={columns}
        rows={rows}
        generatedAt="2026-11-07T10:00:00Z"
        locale="en"
        timeZone="UTC"
      />,
    );
    expect(html).toContain('mn-report-viewer');
    expect(html).toContain('Design');
  });
});
