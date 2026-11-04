import { useState } from 'react';
import { describe, expect, it, vi } from 'vitest';
import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { AuditLogViewer, auditLogToCsv, type AuditLogEvent } from './index.js';

const EVENTS: AuditLogEvent[] = [
  {
    id: 'a1',
    time: '2026-03-02T11:00:00Z',
    actor: { name: 'Ana Lopez', detail: 'ana@example.com' },
    action: 'Changed a role',
    target: 'Ben Okafor',
    ip: '203.0.113.7',
    device: 'Firefox on macOS',
    reason: 'Team change',
    changes: [
      { field: 'Role', before: 'Member', after: 'Admin' },
      { field: 'Title', after: 'Lead' },
    ],
    details: <pre>{'{ "role": "admin" }'}</pre>,
  },
  {
    id: 'a2',
    time: '2026-03-02T10:00:00Z',
    actor: { name: 'Nightly sync', kind: 'automation' },
    action: 'Removed 3 inactive guests',
  },
  { id: 'a3', time: '2026-03-01T09:00:00Z', action: 'Signed in' },
];
const common = { now: '2026-03-02T12:00:00Z', timeZone: 'UTC', locale: 'en-GB' } as const;

describe('AuditLogViewer', () => {
  it('renders a captioned table with every column', async () => {
    render(<AuditLogViewer events={EVENTS} {...common} />);
    const table = screen.getByRole('table', { name: 'Audit log' });
    const headers = within(table)
      .getAllByRole('columnheader')
      .map((h) => h.textContent);
    expect(headers).toEqual(['Time', 'Actor', 'Action', 'Target', 'IP and device', 'Details']);
    const rows = within(table).getAllByRole('row').slice(1);
    expect(rows).toHaveLength(3);
    expect(rows[0]).toHaveTextContent('Ana Lopez');
    expect(rows[0]).toHaveTextContent('203.0.113.7');
    expect(rows[0]).toHaveTextContent('Firefox on macOS');
    expect(rows[0]!.querySelector('time')).toHaveAttribute('datetime', '2026-03-02T11:00:00.000Z');
    expect(within(rows[1]!).getByText('Automation')).toBeInTheDocument();
    expect(within(rows[2]!).getByText('Unknown')).toBeInTheDocument();
    await expectNoA11yViolations();
  });

  it('Details buttons are named by their row', () => {
    render(<AuditLogViewer events={EVENTS} {...common} />);
    expect(screen.getByRole('button', { name: 'Details Changed a role' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Details Signed in' })).toHaveAttribute(
      'aria-haspopup',
      'dialog',
    );
  });

  it('opens the details drawer with fields, changes and payload, and returns focus', async () => {
    const onSelectedChange = vi.fn();
    const user = userEvent.setup();
    render(<AuditLogViewer events={EVENTS} onSelectedChange={onSelectedChange} {...common} />);
    const button = screen.getByRole('button', { name: 'Details Changed a role' });
    button.focus();
    await user.keyboard('{Enter}');
    expect(onSelectedChange).toHaveBeenLastCalledWith('a1');
    const drawer = screen.getByRole('dialog', { name: 'Changed a role' });
    expect(within(drawer).getByText('ana@example.com')).toBeInTheDocument();
    expect(within(drawer).getByText('Team change')).toBeInTheDocument();
    const changes = within(drawer).getByRole('table', { name: 'Changes' });
    const rows = within(changes).getAllByRole('row').slice(1);
    expect(within(rows[0]!).getByRole('rowheader', { name: 'Role' })).toBeInTheDocument();
    expect(rows[0]).toHaveTextContent('MemberAdmin');
    expect(rows[1]).toHaveTextContent('NoneLead');
    expect(within(drawer).getByText('{ "role": "admin" }')).toBeInTheDocument();
    await expectNoA11yViolations();
    await user.keyboard('{Escape}');
    expect(screen.queryByRole('dialog')).toBeNull();
    expect(onSelectedChange).toHaveBeenLastCalledWith(null);
    expect(button).toHaveFocus();
  });

  it('selection can be controlled', async () => {
    function Controlled() {
      const [id, setId] = useState<string | null>('a2');
      return (
        <>
          <span data-testid="sel">{id ?? 'none'}</span>
          <AuditLogViewer
            events={EVENTS}
            selectedId={id}
            onSelectedChange={setId}
            renderDetails={(e) => <p>Extra for {e.id}</p>}
            {...common}
          />
        </>
      );
    }
    render(<Controlled />);
    expect(screen.getByRole('dialog', { name: 'Removed 3 inactive guests' })).toBeInTheDocument();
    expect(screen.getByText('Extra for a2')).toBeInTheDocument();
    await userEvent.click(screen.getByRole('button', { name: 'Close' }));
    expect(screen.getByTestId('sel')).toHaveTextContent('none');
  });

  it('filters slot, export and cursor pagination', async () => {
    const onExport = vi.fn();
    const onLoadMore = vi.fn();
    const { rerender } = render(
      <AuditLogViewer
        events={EVENTS}
        filters={<input aria-label="Search events" />}
        onExport={onExport}
        hasMore
        onLoadMore={onLoadMore}
        {...common}
      />,
    );
    expect(screen.getByRole('textbox', { name: 'Search events' })).toBeInTheDocument();
    await userEvent.click(screen.getByRole('button', { name: 'Export CSV' }));
    expect(onExport).toHaveBeenCalledOnce();
    expect(screen.getByText('Showing 3 events')).toBeInTheDocument();
    await userEvent.click(screen.getByRole('button', { name: 'Load more' }));
    expect(onLoadMore).toHaveBeenCalledOnce();
    rerender(<AuditLogViewer events={EVENTS} onExport={onExport} exporting {...common} />);
    expect(screen.getByRole('button', { name: /Exporting/ })).toHaveAttribute('aria-busy', 'true');
  });

  it('column choice keeps hidden fields in the drawer', async () => {
    render(<AuditLogViewer events={EVENTS} columns={['time', 'action']} {...common} />);
    const headers = screen.getAllByRole('columnheader').map((h) => h.textContent);
    expect(headers).toEqual(['Time', 'Action', 'Details']);
    await userEvent.click(screen.getByRole('button', { name: 'Details Changed a role' }));
    expect(within(screen.getByRole('dialog')).getByText('203.0.113.7')).toBeInTheDocument();
  });

  it('loading, error and empty states', () => {
    const { rerender } = render(<AuditLogViewer events={[]} loading {...common} />);
    expect(screen.getByRole('img', { name: 'Loading audit events' })).toBeInTheDocument();
    expect(screen.queryByRole('table')).toBeNull();
    rerender(<AuditLogViewer events={[]} error={<p>Only admins can read the audit log.</p>} />);
    expect(screen.getByText('Only admins can read the audit log.')).toBeInTheDocument();
    rerender(<AuditLogViewer events={[]} />);
    expect(screen.getByText('No events match')).toBeInTheDocument();
  });

  it('renders on the server', () => {
    const html = renderToString(<AuditLogViewer events={EVENTS} {...common} />);
    expect(html).toContain('mn-audit-log-viewer');
    expect(html).toContain('203.0.113.7');
  });
});

describe('auditLogToCsv', () => {
  it('quotes, escapes and defuses formulas', () => {
    const csv = auditLogToCsv([
      ['time', 'actor', 'action'],
      [new Date('2026-03-02T11:00:00Z'), 'Lopez, Ana', 'Said "hi"'],
      ['=SUM(A1)', null, 'line\nbreak'],
      [1, undefined, '-1'],
    ]);
    expect(csv.split('\r\n')).toEqual([
      'time,actor,action',
      '2026-03-02T11:00:00.000Z,"Lopez, Ana","Said ""hi"""',
      `'=SUM(A1),,"line\nbreak"`,
      "1,,'-1",
    ]);
  });
});
