import { describe, expect, it, vi } from 'vitest';
import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { createRef, useState } from 'react';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { InsightStrip, sortInsights, type InsightStripItem } from './index.js';

const insights: InsightStripItem[] = [
  {
    id: 'a',
    severity: 'info',
    origin: 'rule',
    title: 'Four items renew next month',
    why: 'Renewal dates fall in December.',
  },
  {
    id: 'b',
    severity: 'critical',
    origin: 'rule',
    title: 'Two items expired',
    why: 'Their end dates have passed.',
  },
  {
    id: 'c',
    severity: 'warning',
    origin: 'ai',
    title: 'Usage is falling',
    why: 'Weekly use fell 30% in a month.',
  },
];

function Dismissable() {
  const [list, setList] = useState(insights);
  return (
    <InsightStrip
      title="Needs attention"
      insights={list}
      onDismiss={(id) => setList((l) => l.filter((i) => i.id !== id))}
    />
  );
}

describe('InsightStrip', () => {
  it('renders a named region with a list of callouts, most severe first; forwards props and ref', async () => {
    const ref = createRef<HTMLElement>();
    render(
      <InsightStrip
        ref={ref}
        data-testid="root"
        className="extra"
        title="Needs attention"
        insights={insights}
      />,
    );
    const root = screen.getByTestId('root');
    expect(root).toHaveClass('mn-insight-strip', 'extra');
    expect(ref.current).toBe(root);
    expect(screen.getByRole('region', { name: 'Needs attention' })).toBe(root);
    const groups = within(screen.getByRole('list')).getAllByRole('group');
    expect(groups.map((g) => g.getAttribute('data-severity'))).toEqual([
      'critical',
      'warning',
      'info',
    ]);
    // Callout titles are one heading level below the strip's.
    expect(
      screen.getByRole('heading', { level: 3, name: 'Critical: Two items expired' }),
    ).toBeInTheDocument();
    await expectNoA11yViolations();
  });

  it('labels rule-based and AI insights honestly', () => {
    render(<InsightStrip insights={insights} />);
    const groups = screen.getAllByRole('group');
    expect(within(groups[0]!).getByText('Rule-based')).toBeInTheDocument();
    expect(within(groups[1]!).getByText('AI insight')).toBeInTheDocument();
  });

  it('keeps the given order with sort={false}', () => {
    render(<InsightStrip insights={insights} sort={false} />);
    expect(screen.getAllByRole('group').map((g) => g.getAttribute('data-severity'))).toEqual([
      'info',
      'critical',
      'warning',
    ]);
    expect(sortInsights(insights).map((i) => i.id)).toEqual(['b', 'c', 'a']);
  });

  it('limits to max with a Show more toggle', async () => {
    const user = userEvent.setup();
    render(<InsightStrip insights={insights} max={2} />);
    expect(screen.getAllByRole('group')).toHaveLength(2);
    const toggle = screen.getByRole('button', { name: 'Show 1 more' });
    expect(toggle).toHaveAttribute('aria-expanded', 'false');
    await user.click(toggle);
    expect(screen.getAllByRole('group')).toHaveLength(3);
    expect(screen.getByRole('button', { name: 'Show fewer' })).toHaveAttribute(
      'aria-expanded',
      'true',
    );
    await expectNoA11yViolations();
  });

  it('moves focus to the next dismiss button after a dismiss, then to the heading', async () => {
    const user = userEvent.setup();
    render(<Dismissable />);
    const first = screen.getAllByRole('button', { name: 'Dismiss insight' })[0]!;
    await user.click(first);
    expect(screen.getAllByRole('group')).toHaveLength(2);
    expect(screen.getAllByRole('button', { name: 'Dismiss insight' })[0]).toHaveFocus();
    await user.click(screen.getAllByRole('button', { name: 'Dismiss insight' })[0]!);
    await user.click(screen.getAllByRole('button', { name: 'Dismiss insight' })[0]!);
    expect(screen.getByRole('heading', { name: 'Needs attention' })).toHaveFocus();
    expect(screen.getByText('Nothing needs attention right now.')).toBeInTheDocument();
  });

  it('shows loading, error and empty states', async () => {
    const onRetry = vi.fn();
    const user = userEvent.setup();
    const { rerender, container } = render(
      <InsightStrip data-testid="root" insights={[]} loading />,
    );
    expect(screen.getByRole('img', { name: 'Loading insights' })).toBeInTheDocument();
    expect(screen.getByTestId('root')).toHaveAttribute('aria-busy', 'true');
    rerender(<InsightStrip insights={[]} error="Insights did not load." onRetry={onRetry} />);
    await user.click(screen.getByRole('button', { name: 'Try again' }));
    expect(onRetry).toHaveBeenCalled();
    rerender(<InsightStrip insights={[]} empty={null} />);
    expect(container).toBeEmptyDOMElement();
  });

  it('renders on the server', () => {
    const html = renderToString(
      <InsightStrip title="Needs attention" insights={insights} max={1} />,
    );
    expect(html).toContain('mn-insight-strip');
    expect(html).toContain('Two items expired');
    expect(html).not.toContain('Four items renew');
  });
});
