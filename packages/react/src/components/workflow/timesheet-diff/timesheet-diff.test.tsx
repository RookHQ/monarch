import { describe, expect, it } from 'vitest';
import { render, screen, within } from '@testing-library/react';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import {
  TimesheetDiff,
  diffTimesheetEntries,
  timesheetDiffTotals,
  type TimesheetDiffEntry,
} from './index.js';

const before: TimesheetDiffEntry[] = [
  { id: 'e1', date: '2026-01-12', minutes: 120, project: 'Website refresh', activity: 'Design' },
  { id: 'e2', date: '2026-01-13', minutes: 60, project: 'Internal', billable: false },
  { id: 'e3', date: '2026-01-14', minutes: 90, project: 'Mobile app' },
];
const after: TimesheetDiffEntry[] = [
  { id: 'e1', date: '2026-01-12', minutes: 180, project: 'Website refresh', activity: 'Design' },
  { id: 'e3', date: '2026-01-14', minutes: 90, project: 'Mobile app' },
  { id: 'e4', date: '2026-01-15', minutes: 45, project: 'Mobile app', task: 'APP-7' },
];

describe('diffTimesheetEntries', () => {
  it('finds added, removed and changed entries with their fields', () => {
    const diff = diffTimesheetEntries(before, after);
    expect(diff.added.map((e) => e.id)).toEqual(['e4']);
    expect(diff.removed.map((e) => e.id)).toEqual(['e2']);
    expect(diff.changed).toEqual([
      { id: 'e1', fields: ['minutes'], before: before[0], after: after[0] },
    ]);
  });

  it('totals the change, leaving out non-billable time from billable', () => {
    expect(timesheetDiffTotals(diffTimesheetEntries(before, after))).toEqual({
      total: 45 + 60 - 60,
      billable: 45 + 60,
    });
  });
});

describe('TimesheetDiff', () => {
  it('labels every change in words, with deltas and totals', async () => {
    render(
      <TimesheetDiff
        {...diffTimesheetEntries(before, after)}
        subtitle="since version 2"
        locale="en-GB"
      />,
    );
    const region = screen.getByRole('region', { name: 'What changed since version 2' });
    expect(region).toHaveClass('mn-timesheet-diff');
    const items = within(region).getAllByRole('listitem');
    expect(items).toHaveLength(3);
    expect(items[0]).toHaveAttribute('data-kind', 'added');
    expect(items[0]).toHaveTextContent('Added+45mThu 15 Jan · 45m · Mobile app · APP-7');
    expect(items[1]).toHaveAttribute('data-kind', 'removed');
    expect(items[1]).toHaveTextContent('Removed−1hTue 13 Jan · 1h · Internal · non-billable');
    expect(items[1]!.querySelector('del')).toBeInTheDocument();
    expect(items[2]).toHaveAttribute('data-kind', 'changed');
    expect(items[2]).toHaveTextContent(
      'Changed time+1hBefore: Mon 12 Jan · 2h · Website refresh · DesignAfter: Mon 12 Jan · 3h · Website refresh · Design',
    );
    expect(region).toHaveTextContent('Total +45m · billable +1h 45m');
    await expectNoA11yViolations();
  });

  it('says when nothing changed', () => {
    render(<TimesheetDiff added={[]} removed={[]} changed={[]} data-testid="d" />);
    expect(screen.getByText('No changes.')).toBeInTheDocument();
    expect(screen.getByTestId('d')).toHaveAttribute('data-empty');
    expect(screen.getByTestId('d')).toHaveTextContent('Total 0m · billable 0m');
  });

  it('supports a custom title, heading level, entry renderer and hidden billable', () => {
    render(
      <TimesheetDiff
        {...diffTimesheetEntries(before, after)}
        title="Changes in this resubmission"
        headingLevel={4}
        showBillable={false}
        renderEntry={(e) => `${e.id}: ${e.minutes}`}
      />,
    );
    expect(
      screen.getByRole('heading', { level: 4, name: 'Changes in this resubmission' }),
    ).toBeInTheDocument();
    expect(screen.getByText('e4: 45')).toBeInTheDocument();
    expect(screen.getByText('Total +45m')).toBeInTheDocument();
  });

  it('can hide the totals and translate strings', () => {
    render(
      <TimesheetDiff
        added={[after[2]!]}
        removed={[]}
        changed={[]}
        showTotals={false}
        messages={{ title: 'Änderungen', added: 'Neu', noProject: 'Kein Projekt' }}
      />,
    );
    expect(screen.getByRole('heading', { name: 'Änderungen' })).toBeInTheDocument();
    expect(screen.getByText('Neu')).toBeInTheDocument();
    expect(screen.queryByText(/^Total/)).toBeNull();
  });

  it('renders on the server', () => {
    const html = renderToString(<TimesheetDiff {...diffTimesheetEntries(before, after)} />);
    expect(html).toContain('mn-timesheet-diff');
    expect(html).toContain('data-kind="changed"');
  });
});
