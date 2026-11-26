import { describe, expect, it, vi } from 'vitest';
import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { SearchResults, type SearchResultGroup } from './index.js';

const GROUPS: SearchResultGroup[] = [
  {
    id: 'docs',
    label: 'Documents',
    total: 5,
    onShowMore: () => {},
    items: [
      {
        id: 'd1',
        title: 'Travel policy',
        href: '/docs/travel',
        path: ['Handbook'],
        snippet: 'Book travel early.',
      },
      { id: 'd2', title: 'Travel checklist', href: '/docs/checklist' },
    ],
  },
  {
    id: 'people',
    label: 'People',
    items: [{ id: 'p1', title: 'Alex Kim', href: '/people/alex' }],
  },
  { id: 'empty', label: 'Projects', items: [] },
];

describe('SearchResults', () => {
  it('renders labelled groups with counts, a live summary and highlights', async () => {
    const { container } = render(
      <SearchResults data-testid="root" groups={GROUPS} query="travel" />,
    );
    const root = screen.getByTestId('root');
    expect(root).toHaveClass('mn-search-results');
    expect(root).toHaveAttribute('data-state', 'ready');
    expect(screen.getByRole('status')).toHaveTextContent('6 results for “travel”');
    const docs = screen.getByRole('region', { name: 'Documents 5' });
    expect(within(docs).getAllByRole('link')).toHaveLength(2);
    expect(screen.getByRole('region', { name: 'People 1' })).toBeInTheDocument();
    expect(screen.queryByRole('region', { name: /Projects/ })).toBeNull();
    expect(container.querySelectorAll('mark').length).toBeGreaterThan(0);
    expect(screen.getByRole('button', { name: 'Show all 5 in Documents' })).toBeInTheDocument();
    await expectNoA11yViolations();
  });

  it('is one Tab stop; arrows, Home and End move between results', async () => {
    render(
      <>
        <button type="button">Before</button>
        <SearchResults groups={GROUPS} />
      </>,
    );
    const user = userEvent.setup();
    const links = screen.getAllByRole('link');
    expect(links.map((l) => l.tabIndex)).toEqual([0, -1, -1]);
    await user.click(screen.getByRole('button', { name: 'Before' }));
    await user.tab();
    expect(links[0]).toHaveFocus();
    await user.keyboard('{ArrowDown}');
    expect(links[1]).toHaveFocus();
    await user.keyboard('{ArrowDown}');
    expect(links[2]).toHaveFocus();
    await user.keyboard('{ArrowDown}');
    expect(links[2]).toHaveFocus();
    await user.keyboard('{Home}');
    expect(links[0]).toHaveFocus();
    await user.keyboard('{End}');
    expect(links[2]).toHaveFocus();
    await user.keyboard('{ArrowUp}');
    expect(links[1]).toHaveFocus();
    expect(links.map((l) => l.tabIndex)).toEqual([-1, 0, -1]);
  });

  it('calls onSelect with the result and group on Enter', async () => {
    const onSelect = vi.fn();
    render(
      <SearchResults
        results={[
          { id: 'a', title: 'Alpha' },
          { id: 'b', title: 'Beta' },
        ]}
        onSelect={onSelect}
      />,
    );
    const user = userEvent.setup();
    await user.tab();
    await user.keyboard('{ArrowDown}{Enter}');
    expect(onSelect).toHaveBeenCalledWith(expect.objectContaining({ id: 'b' }), undefined);
  });

  it('shows skeletons while loading with no results', async () => {
    render(<SearchResults data-testid="root" loading query="x" />);
    const root = screen.getByTestId('root');
    expect(root).toHaveAttribute('data-state', 'loading');
    expect(root).toHaveAttribute('aria-busy', 'true');
    expect(screen.getByRole('status')).toHaveTextContent('Searching…');
    await expectNoA11yViolations();
  });

  it('keeps results visible while refreshing', () => {
    render(<SearchResults data-testid="root" loading groups={GROUPS} />);
    expect(screen.getByTestId('root')).toHaveAttribute('data-refreshing');
    expect(screen.getAllByRole('link')).toHaveLength(3);
  });

  it('shows an empty state naming the query', async () => {
    render(<SearchResults groups={[]} query="zebra" />);
    expect(screen.getByRole('heading', { name: 'No results for “zebra”' })).toBeInTheDocument();
    expect(screen.getByRole('status')).toHaveTextContent('No results for “zebra”');
    await expectNoA11yViolations();
  });

  it('shows an error with a retry button', async () => {
    const onRetry = vi.fn();
    render(<SearchResults error="The index is unavailable." onRetry={onRetry} />);
    expect(screen.getByRole('alert')).toHaveTextContent('Search failed');
    await userEvent.setup().click(screen.getByRole('button', { name: 'Try again' }));
    expect(onRetry).toHaveBeenCalled();
    await expectNoA11yViolations();
  });

  it('uses message overrides', () => {
    render(
      <SearchResults
        results={[{ id: 'a', title: 'Alpha' }]}
        query="a"
        messages={{ summary: (n) => `${n} résultat` }}
      />,
    );
    expect(screen.getByRole('status')).toHaveTextContent('1 résultat');
  });

  it('renders on the server', () => {
    const html = renderToString(<SearchResults groups={GROUPS} query="travel" />);
    expect(html).toContain('mn-search-results');
    expect(html).toContain('Travel');
  });
});
