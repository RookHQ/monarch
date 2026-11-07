import { describe, expect, it, vi } from 'vitest';
import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { createRef } from 'react';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { AiAgentRunTimeline, aiRunTotals, type AiRunEvent } from './index.js';

const T = Date.UTC(2026, 0, 1, 12, 0, 0);

const events: AiRunEvent[] = [
  {
    id: 'tool',
    kind: 'tool',
    name: 'search_notes',
    startedAt: T + 1200,
    endedAt: T + 2000,
    detail: 'Found 3 notes',
  },
  {
    id: 'model',
    kind: 'model',
    name: 'Planner',
    startedAt: T,
    endedAt: T + 1200,
    inputTokens: 1200,
    outputTokens: 300,
    costMicros: 4500,
  },
  {
    id: 'answer',
    kind: 'model',
    name: 'Writer',
    startedAt: T + 2000,
    endedAt: T + 4000,
    inputTokens: 2000,
    outputTokens: 800,
    costMicros: 12_000,
  },
];

describe('AiAgentRunTimeline', () => {
  it('renders a labelled group with totals and events sorted by start', async () => {
    const ref = createRef<HTMLDivElement>();
    render(
      <AiAgentRunTimeline
        ref={ref}
        events={events}
        locale="en-US"
        data-testid="root"
        className="extra"
      />,
    );
    const root = screen.getByTestId('root');
    expect(root).toHaveClass('mn-ai-agent-run-timeline', 'extra');
    expect(ref.current).toBe(root);
    expect(screen.getByRole('group', { name: 'Agent run' })).toBe(root);
    expect(root).toHaveTextContent('Total time4.0 s');
    expect(root).toHaveTextContent('Input tokens3,200');
    expect(root).toHaveTextContent('Output tokens1,100');
    expect(root).toHaveTextContent('Cost$0.0165');
    const items = within(screen.getByRole('list', { name: 'Run events' })).getAllByRole('listitem');
    expect(items.map((i) => i.getAttribute('data-kind'))).toEqual(['model', 'tool', 'model']);
    expect(within(items[0]!).getByRole('button')).toHaveAccessibleName(/Model\s*Planner\s*1\.2 s/);
    await expectNoA11yViolations();
  });

  it('positions bars on the shared time axis', () => {
    const { container } = render(<AiAgentRunTimeline events={events} />);
    const bars = container.querySelectorAll<HTMLElement>('.mn-ai-agent-run-timeline__bar');
    expect(bars[1]!.style.insetInlineStart).toBe('30%');
    expect(bars[1]!.style.inlineSize).toBe('20%');
  });

  it('expands an event with the keyboard to show its detail', async () => {
    const user = userEvent.setup();
    render(<AiAgentRunTimeline events={events} />);
    const toggle = screen.getByRole('button', { name: /search_notes/ });
    expect(toggle).toHaveAttribute('aria-expanded', 'false');
    expect(screen.queryByText('Found 3 notes')).not.toBeVisible();
    toggle.focus();
    await user.keyboard('{Enter}');
    expect(toggle).toHaveAttribute('aria-expanded', 'true');
    expect(screen.getByText('Found 3 notes')).toBeVisible();
    expect(document.getElementById(toggle.getAttribute('aria-controls')!)).toHaveTextContent(
      '+1.2 s',
    );
    await user.keyboard(' ');
    expect(toggle).toHaveAttribute('aria-expanded', 'false');
    await expectNoA11yViolations();
  });

  it('switches to a table with the same data', async () => {
    const user = userEvent.setup();
    const onViewChange = vi.fn();
    render(<AiAgentRunTimeline events={events} locale="en-US" onViewChange={onViewChange} />);
    await user.click(screen.getByRole('radio', { name: 'Table' }));
    expect(onViewChange).toHaveBeenCalledWith('table');
    const table = screen.getByRole('table', { name: 'Agent run' });
    const rows = within(table).getAllByRole('row');
    expect(rows).toHaveLength(4);
    expect(rows[1]).toHaveTextContent('PlannerModel+0 ms1.2 s1,200300$0.0045');
    expect(within(table).getByRole('rowheader', { name: 'search_notes' })).toBeInTheDocument();
    await expectNoA11yViolations();
  });

  it('marks running events and ends them at `now`', () => {
    render(
      <AiAgentRunTimeline
        events={[{ id: 'r', kind: 'model', name: 'Writer', startedAt: T }]}
        now={T + 3000}
      />,
    );
    expect(screen.getByRole('button', { name: /Writer/ })).toHaveTextContent('Running');
    expect(
      aiRunTotals([{ id: 'r', kind: 'model', name: 'W', startedAt: T }], T + 3000),
    ).toMatchObject({ durationMs: 3000, hasCost: false });
  });

  it('shows an empty message and translated strings', () => {
    render(
      <AiAgentRunTimeline
        events={[]}
        defaultView="table"
        messages={{ empty: 'Aucun événement', views: { table: 'Tableau' } }}
      />,
    );
    expect(screen.getByText('Aucun événement')).toBeInTheDocument();
    expect(screen.getByRole('radio', { name: 'Tableau' })).toBeChecked();
  });

  it('renders on the server', () => {
    const html = renderToString(<AiAgentRunTimeline events={events} locale="en-US" />);
    expect(html).toContain('mn-ai-agent-run-timeline');
    expect(html).toContain('search_notes');
  });
});
