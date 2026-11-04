import { describe, expect, it, vi } from 'vitest';
import { createRef, useState } from 'react';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { Button } from '../../actions/button/index.js';
import { SectionHeaderLink } from '../section-header/index.js';
import { SectionCard } from './index.js';

describe('SectionCard', () => {
  it('is a region labelled by its heading, with actions, link, body and footer', async () => {
    const onAdd = vi.fn();
    const ref = createRef<HTMLElement>();
    render(
      <SectionCard
        ref={ref}
        title="Open tasks"
        description="Tasks due this week."
        meta="12 tasks"
        icon={<svg data-testid="icon" />}
        actions={<Button onClick={onAdd}>Add task</Button>}
        link={<SectionHeaderLink href="/tasks">View all tasks</SectionHeaderLink>}
        footer={<span>Updated just now</span>}
        data-testid="card"
      >
        <p>Write the brief</p>
      </SectionCard>,
    );
    const region = screen.getByRole('region', { name: 'Open tasks' });
    expect(ref.current).toBe(region);
    expect(region).toHaveClass('mn-section-card');
    expect(region).toHaveAttribute('data-state', 'ready');
    expect(screen.getByRole('heading', { level: 2, name: 'Open tasks' })).toBeInTheDocument();
    expect(screen.getByTestId('icon').parentElement).toHaveAttribute('aria-hidden', 'true');
    expect(screen.getByText('Write the brief')).toBeVisible();
    expect(screen.getByText('Updated just now').parentElement).toHaveClass(
      'mn-section-card__footer',
    );
    const user = userEvent.setup();
    await user.tab();
    await user.keyboard('{Enter}');
    expect(onAdd).toHaveBeenCalledOnce();
    await user.tab();
    expect(screen.getByRole('link', { name: 'View all tasks' })).toHaveFocus();
    await expectNoA11yViolations();
  });

  it('shows loading, empty and error states instead of the children', async () => {
    const onRetry = vi.fn();
    const { rerender } = render(
      <SectionCard title="Invoices" state="loading" loadingLabel="Loading invoices">
        <p>rows</p>
      </SectionCard>,
    );
    const region = screen.getByRole('region', { name: 'Invoices' });
    expect(region).toHaveAttribute('aria-busy', 'true');
    expect(screen.getByRole('status')).toHaveTextContent('Loading invoices');
    expect(screen.queryByText('rows')).toBeNull();

    rerender(
      <SectionCard title="Invoices" state="empty" emptyDescription="Invoices you send appear here.">
        <p>rows</p>
      </SectionCard>,
    );
    expect(region).not.toHaveAttribute('aria-busy');
    expect(screen.getByText('Nothing here yet')).toBeInTheDocument();
    expect(screen.getByText('Invoices you send appear here.')).toBeInTheDocument();

    rerender(
      <SectionCard title="Invoices" state="empty" empty={<p>Custom empty</p>}>
        <p>rows</p>
      </SectionCard>,
    );
    expect(screen.getByText('Custom empty')).toBeInTheDocument();

    rerender(
      <SectionCard
        title="Invoices"
        state="error"
        errorDescription="Check your connection."
        onRetry={onRetry}
      >
        <p>rows</p>
      </SectionCard>,
    );
    expect(screen.getByText('This section didn’t load')).toBeInTheDocument();
    await userEvent.setup().click(screen.getByRole('button', { name: /try again/i }));
    expect(onRetry).toHaveBeenCalledOnce();
    expect(region).toHaveAttribute('data-state', 'error');
    await expectNoA11yViolations();
  });

  it('busy keeps the content and marks the section busy', () => {
    render(
      <SectionCard title="Traffic" busy>
        <p>chart</p>
      </SectionCard>,
    );
    expect(screen.getByRole('region', { name: 'Traffic' })).toHaveAttribute('aria-busy', 'true');
    expect(screen.getByText('chart')).toBeInTheDocument();
  });

  it('collapsible (uncontrolled): the title is a disclosure button', async () => {
    const onOpenChange = vi.fn();
    render(
      <SectionCard
        title="Activity"
        collapsible
        footer={<span>footer</span>}
        onOpenChange={onOpenChange}
      >
        <p>events</p>
      </SectionCard>,
    );
    const toggle = screen.getByRole('button', { name: 'Activity' });
    expect(toggle.closest('h2')).not.toBeNull();
    expect(toggle).toHaveAttribute('aria-expanded', 'true');
    const body = document.getElementById(toggle.getAttribute('aria-controls') ?? '');
    expect(body).toContainElement(screen.getByText('events'));
    const user = userEvent.setup();
    await user.tab();
    expect(toggle).toHaveFocus();
    await user.keyboard('{Enter}');
    expect(toggle).toHaveAttribute('aria-expanded', 'false');
    expect(body).not.toBeVisible();
    expect(screen.queryByText('footer')).toBeNull();
    expect(onOpenChange).toHaveBeenLastCalledWith(false);
    await user.keyboard(' ');
    expect(toggle).toHaveAttribute('aria-expanded', 'true');
    expect(screen.getByText('events')).toBeVisible();
    await expectNoA11yViolations();
  });

  it('collapsible (controlled) follows `open` and starts closed with defaultOpen=false', async () => {
    function Controlled() {
      const [open, setOpen] = useState(false);
      return (
        <>
          <SectionCard title="Notes" collapsible open={open} onOpenChange={setOpen}>
            <p>note</p>
          </SectionCard>
          <span data-testid="state">{String(open)}</span>
        </>
      );
    }
    render(<Controlled />);
    const toggle = screen.getByRole('button', { name: 'Notes' });
    expect(toggle).toHaveAttribute('aria-expanded', 'false');
    await userEvent.setup().click(toggle);
    expect(screen.getByTestId('state')).toHaveTextContent('true');
    expect(toggle).toHaveAttribute('aria-expanded', 'true');

    render(
      <SectionCard title="Later" collapsible defaultOpen={false}>
        <p>hidden</p>
      </SectionCard>,
    );
    expect(screen.getByRole('button', { name: 'Later' })).toHaveAttribute('aria-expanded', 'false');
    expect(screen.getByText('hidden')).not.toBeVisible();
  });

  it('exposes tone and flush, honours headingLevel and headingId', () => {
    render(
      <SectionCard title="Danger zone" tone="danger" flush headingLevel="h3" headingId="dz">
        <p>x</p>
      </SectionCard>,
    );
    const region = screen.getByRole('region', { name: 'Danger zone' });
    expect(region).toHaveAttribute('data-tone', 'danger');
    expect(region).toHaveAttribute('data-flush');
    expect(region).toHaveAttribute('aria-labelledby', 'dz');
    expect(screen.getByRole('heading', { level: 3 })).toHaveAttribute('id', 'dz');
  });

  it('renders on the server', () => {
    const html = renderToString(
      <SectionCard title="Pages" collapsible footer="f" state="empty">
        <p>x</p>
      </SectionCard>,
    );
    expect(html).toContain('mn-section-card');
    expect(html).toContain('aria-expanded="true"');
  });
});
