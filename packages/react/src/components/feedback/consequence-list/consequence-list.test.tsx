import { describe, expect, it, vi } from 'vitest';
import { createRef } from 'react';
import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { Button } from '../../actions/button/index.js';
import { ConsequenceList, type ConsequenceItem } from './index.js';

const items: ConsequenceItem[] = [
  { id: 'tasks', kind: 'removed', count: 1240, label: 'tasks will be deleted' },
  { id: 'access', kind: 'changed', label: 'Members lose access immediately' },
  {
    id: 'invoices',
    kind: 'kept',
    label: 'Invoices are kept for 7 years',
    detail: 'Required for accounting records.',
  },
  { id: 'links', kind: 'warning', label: 'Shared links stop working' },
];

describe('ConsequenceList', () => {
  it('renders a list named by its heading, forwards ref and props', async () => {
    const ref = createRef<HTMLDivElement>();
    render(<ConsequenceList ref={ref} data-testid="root" heading="What happens" items={items} />);
    const root = screen.getByTestId('root');
    expect(root).toHaveClass('mn-consequence-list');
    expect(ref.current).toBe(root);
    expect(screen.getByRole('heading', { level: 3, name: 'What happens' })).toBeInTheDocument();
    const list = screen.getByRole('list', { name: 'What happens' });
    expect(within(list).getAllByRole('listitem')).toHaveLength(4);
    await expectNoA11yViolations();
  });

  it('gives every item an icon and a spoken kind, so colour is not the only signal', () => {
    render(<ConsequenceList items={items} />);
    const [removed, changed, kept, warning] = screen.getAllByRole('listitem');
    expect(removed).toHaveAttribute('data-kind', 'removed');
    expect(removed).toHaveTextContent('Removed: 1,240 tasks will be deleted');
    expect(changed).toHaveTextContent('Changed: Members lose access immediately');
    expect(kept).toHaveTextContent('Kept: Invoices are kept for 7 years');
    expect(kept).toHaveTextContent('Required for accounting records.');
    expect(warning).toHaveTextContent('Warning: Shared links stop working');
    for (const li of [removed, changed, kept, warning]) {
      expect(li!.querySelector('svg')).toHaveAttribute('aria-hidden', 'true');
    }
  });

  it('formats counts with Intl.NumberFormat in the given locale', () => {
    render(<ConsequenceList locale="de-DE" items={[items[0]!]} />);
    expect(document.querySelector('.mn-consequence-list__count')).toHaveTextContent('1.240');
  });

  it('groups by kind in a fixed order under sub-headings, with translatable labels', async () => {
    render(
      <ConsequenceList
        heading="What happens"
        headingLevel="h2"
        groupBy="kind"
        items={[items[2]!, items[0]!, items[3]!]}
        messages={{ groupKept: 'Stays' }}
      />,
    );
    const headings = screen.getAllByRole('heading', { level: 3 }).map((h) => h.textContent);
    expect(headings).toEqual(['Will be deleted', 'Before you continue', 'Stays']);
    const deleted = screen.getByRole('list', { name: 'Will be deleted' });
    expect(deleted).toHaveTextContent('1,240 tasks will be deleted');
    expect(deleted).not.toHaveTextContent('Removed:');
    expect(screen.queryByRole('list', { name: 'Will change' })).toBeNull();
    await expectNoA11yViolations();
  });

  it('shows skeleton rows and a status while loading', async () => {
    render(
      <ConsequenceList data-testid="root" items={items} loading loadingRows={2} heading="Impact" />,
    );
    const root = screen.getByTestId('root');
    expect(root).toHaveAttribute('aria-busy', 'true');
    expect(root).toHaveAttribute('data-state', 'loading');
    expect(screen.getByRole('status')).toHaveTextContent('Checking what will change');
    expect(screen.queryByRole('list')).toBeNull();
    expect(root.querySelectorAll('.mn-consequence-list__skeleton-row')).toHaveLength(2);
    await expectNoA11yViolations();
  });

  it('shows the error slot instead of the items, and its retry is reachable by keyboard', async () => {
    const onRetry = vi.fn();
    render(
      <ConsequenceList
        data-testid="root"
        items={items}
        error={
          <p role="alert">
            Could not load the impact. <Button onClick={onRetry}>Retry</Button>
          </p>
        }
      />,
    );
    expect(screen.getByTestId('root')).toHaveAttribute('data-state', 'error');
    expect(screen.getByRole('alert')).toHaveTextContent('Could not load the impact.');
    expect(screen.queryByRole('list')).toBeNull();
    const user = userEvent.setup();
    await user.tab();
    expect(screen.getByRole('button', { name: 'Retry' })).toHaveFocus();
    await user.keyboard('{Enter}');
    expect(onRetry).toHaveBeenCalledOnce();
    await expectNoA11yViolations();
  });

  it('is not focusable itself', async () => {
    render(<ConsequenceList items={items} />);
    await userEvent.setup().tab();
    expect(document.body).toHaveFocus();
  });

  it('renders on the server', () => {
    const html = renderToString(<ConsequenceList items={items} heading="What happens" />);
    expect(html).toContain('mn-consequence-list');
    expect(html).toContain('1,240');
    expect(renderToString(<ConsequenceList items={[]} loading />)).toContain('aria-busy');
  });
});
