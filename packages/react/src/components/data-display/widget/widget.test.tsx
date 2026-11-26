import { describe, expect, it, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { createRef } from 'react';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { Widget } from './index.js';

describe('Widget', () => {
  it('is a region named by its title and described by its description; forwards props and ref', async () => {
    const ref = createRef<HTMLElement>();
    render(
      <Widget
        ref={ref}
        data-testid="root"
        className="extra"
        title="Team load"
        description="Open tickets per agent"
        period="Last 30 days"
      >
        <p>Body</p>
      </Widget>,
    );
    const root = screen.getByTestId('root');
    expect(root).toHaveClass('mn-widget', 'extra');
    expect(ref.current).toBe(root);
    const region = screen.getByRole('region', { name: 'Team load' });
    expect(region).toBe(root);
    expect(region).toHaveAccessibleDescription('Open tickets per agent');
    expect(screen.getByRole('heading', { level: 2, name: 'Team load' })).toBeInTheDocument();
    expect(screen.getByText('Last 30 days')).toBeInTheDocument();
    expect(screen.getByText('Body')).toBeInTheDocument();
    await expectNoA11yViolations();
  });

  it('honours headingLevel and span', () => {
    render(
      <Widget data-testid="root" title="Pipeline" headingLevel={3} span={2}>
        x
      </Widget>,
    );
    expect(screen.getByRole('heading', { level: 3, name: 'Pipeline' })).toBeInTheDocument();
    expect(screen.getByTestId('root')).toHaveAttribute('data-span', '2');
  });

  it('shows a named placeholder and aria-busy while loading, hiding the body', async () => {
    render(
      <Widget data-testid="root" title="Revenue" loading>
        <p>Body</p>
      </Widget>,
    );
    const root = screen.getByTestId('root');
    expect(root).toHaveAttribute('aria-busy', 'true');
    expect(root).toHaveAttribute('data-state', 'loading');
    expect(screen.getByRole('img', { name: 'Loading Revenue' })).toBeInTheDocument();
    expect(screen.queryByText('Body')).not.toBeInTheDocument();
    await expectNoA11yViolations();
  });

  it('shows an error with Try again that calls onRetry', async () => {
    const onRetry = vi.fn();
    const user = userEvent.setup();
    render(
      <Widget data-testid="root" title="Revenue" error="Revenue did not load." onRetry={onRetry}>
        <p>Body</p>
      </Widget>,
    );
    expect(screen.getByTestId('root')).toHaveAttribute('data-state', 'error');
    expect(screen.getByText('Revenue did not load.')).toBeInTheDocument();
    expect(screen.queryByText('Body')).not.toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Try again' }));
    expect(onRetry).toHaveBeenCalledTimes(1);
    await expectNoA11yViolations();
  });

  it('uses the default error message as an alert when error is true and there is no retry', () => {
    render(<Widget title="Revenue" error />);
    expect(screen.getByRole('alert')).toHaveTextContent('This section could not be loaded.');
  });

  it('shows the default or a custom empty state', () => {
    const { rerender } = render(
      <Widget data-testid="root" title="Escalations" empty>
        <p>Body</p>
      </Widget>,
    );
    expect(screen.getByTestId('root')).toHaveAttribute('data-state', 'empty');
    expect(screen.getByText('Nothing to show yet')).toBeInTheDocument();
    rerender(
      <Widget title="Escalations" empty={<p>No escalations in 30 days.</p>}>
        <p>Body</p>
      </Widget>,
    );
    expect(screen.getByText('No escalations in 30 days.')).toBeInTheDocument();
    expect(screen.queryByText('Body')).not.toBeInTheDocument();
  });

  it('opens the overflow menu from the keyboard and runs an item', async () => {
    const onRefresh = vi.fn();
    const user = userEvent.setup();
    render(
      <Widget
        title="Team load"
        menu={[
          { id: 'refresh', label: 'Refresh', onSelect: onRefresh },
          {
            id: 'remove',
            label: 'Remove',
            onSelect: () => {},
            tone: 'danger',
            separatorBefore: true,
          },
        ]}
      >
        x
      </Widget>,
    );
    const trigger = screen.getByRole('button', { name: 'More actions for Team load' });
    trigger.focus();
    await user.keyboard('{Enter}');
    const item = await screen.findByRole('menuitem', { name: 'Refresh' });
    expect(screen.getByRole('menuitem', { name: 'Remove' })).toBeInTheDocument();
    await user.click(item);
    expect(onRefresh).toHaveBeenCalledTimes(1);
    expect(trigger).toHaveFocus();
  });

  it('adds a view-all link whose name includes the widget title', () => {
    render(
      <Widget title="Escalations" viewAllHref="/reports">
        x
      </Widget>,
    );
    const link = screen.getByRole('link', { name: 'View all: Escalations' });
    expect(link).toHaveAttribute('href', '/reports');
  });

  it('shows when the data was updated and a custom footer', () => {
    render(
      <Widget
        title="Revenue"
        updatedAt="2026-11-07T09:57:00Z"
        now="2026-11-07T10:00:00Z"
        locale="en"
        footer={<span>Excludes trials.</span>}
      >
        x
      </Widget>,
    );
    expect(screen.getByText('Excludes trials.')).toBeInTheDocument();
    expect(screen.getByText(/Updated/)).toHaveTextContent('Updated 3 minutes ago');
  });

  it('translates through messages', () => {
    render(
      <Widget
        title="Ventas"
        viewAllHref="/v"
        messages={{ viewAll: 'Ver lista', viewAllLabel: (t) => `Ver lista (${t})` }}
      >
        x
      </Widget>,
    );
    expect(screen.getByRole('link', { name: 'Ver lista (Ventas)' })).toBeInTheDocument();
  });

  it('renders on the server', () => {
    const html = renderToString(
      <Widget
        title="Team load"
        updatedAt="2026-11-07T09:00:00Z"
        now="2026-11-07T10:00:00Z"
        locale="en"
      >
        Body
      </Widget>,
    );
    expect(html).toContain('mn-widget');
    expect(html).toContain('Team load');
    expect(html).toContain('1 hour ago');
  });
});
