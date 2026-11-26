import { createRef } from 'react';
import { describe, expect, it, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { EmptyState } from './index.js';

describe('EmptyState', () => {
  it('renders accessibly with an h3 title by default', async () => {
    render(
      <main>
        <EmptyState title="No tickets yet" description="New tickets from your inbox appear here." />
      </main>,
    );
    expect(screen.getByRole('heading', { name: 'No tickets yet', level: 3 })).toBeInTheDocument();
    await expectNoA11yViolations();
  });

  it('keeps the 0.3 markup when no new props are passed', () => {
    const { container } = render(
      <EmptyState icon={<svg />} title="Empty" actions={<button>Add</button>} />,
    );
    const root = container.firstElementChild!;
    expect(root).toHaveAttribute('data-size', 'inline');
    expect(root).not.toHaveAttribute('data-kind');
    expect(root).not.toHaveAttribute('data-layout');
    expect(root.querySelector('.mn-empty-state__icon')).toHaveAttribute('aria-hidden', 'true');
  });

  it('forwards its ref and native attributes', () => {
    const ref = createRef<HTMLDivElement>();
    render(<EmptyState ref={ref} id="empty" title="Empty" />);
    expect(ref.current).toHaveAttribute('id', 'empty');
  });

  it('uses the heading level the page outline needs, or a paragraph', () => {
    const { rerender } = render(<EmptyState title="Nothing here" headingLevel="h2" />);
    expect(screen.getByRole('heading', { level: 2 })).toHaveTextContent('Nothing here');
    rerender(<EmptyState title="Nothing here" headingLevel="p" />);
    expect(screen.queryByRole('heading')).toBeNull();
    expect(screen.getByText('Nothing here').tagName).toBe('P');
  });

  it('filtered: renders Clear filters first and calls it from the keyboard', async () => {
    const onClear = vi.fn();
    render(
      <EmptyState
        kind="filtered"
        title="No results match"
        onClearFilters={onClear}
        actions={<button type="button">New ticket</button>}
      />,
    );
    const buttons = screen.getAllByRole('button');
    expect(buttons.map((b) => b.textContent)).toEqual(['Clear filters', 'New ticket']);
    const user = userEvent.setup();
    await user.tab();
    expect(buttons[0]).toHaveFocus();
    await user.keyboard('{Enter}');
    expect(onClear).toHaveBeenCalledOnce();
    expect(screen.getByText('No results match').closest('.mn-empty-state')).toHaveAttribute(
      'data-kind',
      'filtered',
    );
    await expectNoA11yViolations();
  });

  it('does not render Clear filters for other kinds, and accepts a custom label', () => {
    const { rerender } = render(<EmptyState title="Empty" onClearFilters={() => {}} />);
    expect(screen.queryByRole('button')).toBeNull();
    rerender(
      <EmptyState
        kind="filtered"
        title="Empty"
        onClearFilters={() => {}}
        clearFiltersLabel="Reset"
      />,
    );
    expect(screen.getByRole('button', { name: 'Reset' })).toBeInTheDocument();
  });

  it('success: shows a decorative check icon unless an icon is given', () => {
    const { container, rerender } = render(<EmptyState kind="success" title="All caught up" />);
    expect(container.querySelector('.mn-empty-state__icon svg')).toBeInTheDocument();
    rerender(<EmptyState kind="success" title="All caught up" icon={<span data-testid="own" />} />);
    expect(screen.getByTestId('own')).toBeInTheDocument();
  });

  it('line layout wraps title and description, compact size is exposed', async () => {
    const { container } = render(
      <main>
        <EmptyState
          layout="line"
          size="compact"
          title="No activity this week"
          description="Activity shows up here."
        />
      </main>,
    );
    const root = container.querySelector('.mn-empty-state')!;
    expect(root).toHaveAttribute('data-layout', 'line');
    expect(root).toHaveAttribute('data-size', 'compact');
    expect(root.querySelector('.mn-empty-state__text')).toHaveTextContent(
      'Activity shows up here.',
    );
    await expectNoA11yViolations();
  });

  it('renders a tip and extra children after the actions', () => {
    render(
      <EmptyState title="No messages" tip="Press N to start a conversation.">
        <ul>
          <li>Invite a teammate</li>
        </ul>
      </EmptyState>,
    );
    expect(screen.getByText('Press N to start a conversation.')).toHaveClass('mn-empty-state__tip');
    expect(screen.getByRole('listitem')).toHaveTextContent('Invite a teammate');
  });

  it('renders on the server', () => {
    const html = renderToString(
      <EmptyState kind="filtered" title="Empty" onClearFilters={() => {}} tip="Tip" />,
    );
    expect(html).toContain('mn-empty-state');
    expect(html).toContain('data-kind="filtered"');
    expect(html).toContain('Clear filters');
  });
});
