import { describe, expect, it, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { HighlightText, SearchResultItem, highlightSegments, queryTerms } from './index.js';

describe('highlightSegments', () => {
  it('matches terms case-insensitively and merges overlaps', () => {
    expect(highlightSegments('Travel policy and travel days', { terms: ['travel'] })).toEqual([
      { text: 'Travel', match: true },
      { text: ' policy and ', match: false },
      { text: 'travel', match: true },
      { text: ' days', match: false },
    ]);
    expect(
      highlightSegments('abcdef', {
        ranges: [
          [1, 3],
          [2, 5],
        ],
      }),
    ).toEqual([
      { text: 'a', match: false },
      { text: 'bcde', match: true },
      { text: 'f', match: false },
    ]);
  });

  it('escapes regular-expression characters in terms', () => {
    expect(highlightSegments('a+b (c)', { terms: ['(c)'] })).toEqual([
      { text: 'a+b ', match: false },
      { text: '(c)', match: true },
    ]);
  });

  it('reads only <mark> markers and keeps other markup as text', () => {
    expect(
      highlightSegments('<b>x</b> <mark>hit</mark> <img src=x onerror=alert(1)>', {
        markTags: true,
      }),
    ).toEqual([
      { text: '<b>x</b> ', match: false },
      { text: 'hit', match: true },
      { text: ' <img src=x onerror=alert(1)>', match: false },
    ]);
  });

  it('splits a query into unique words', () => {
    expect(queryTerms('Travel, travel POLICY!')).toEqual(['travel', 'policy']);
  });

  it('renders untrusted text without creating elements', () => {
    const { container } = render(
      <p>
        <HighlightText text="<img src=x> <mark>a</mark>" markTags />
      </p>,
    );
    expect(container.querySelector('img')).toBeNull();
    expect(container.querySelectorAll('mark')).toHaveLength(1);
    expect(container.textContent).toBe('<img src=x> a');
  });
});

describe('SearchResultItem', () => {
  it('renders a link titled by the result, described by its path and snippet', async () => {
    render(
      <SearchResultItem
        data-testid="root"
        title="Travel policy"
        href="/docs/travel"
        path={['Handbook', 'Policies']}
        snippet="Book travel through the shared calendar."
        terms={['travel']}
      />,
    );
    expect(screen.getByTestId('root')).toHaveClass('mn-search-result-item');
    const link = screen.getByRole('link', { name: 'Travel policy' });
    expect(link).toHaveAttribute('href', '/docs/travel');
    expect(link).toHaveAccessibleDescription(
      /^Location: Handbook ?, Policies Book travel through the shared calendar\.$/,
    );
    expect(screen.getByTestId('root').querySelectorAll('mark')).toHaveLength(2);
    await expectNoA11yViolations();
  });

  it('drops unsafe URLs and falls back to a button when onSelect is set', async () => {
    const onSelect = vi.fn();
    render(<SearchResultItem title="Report" href="javascript:alert(1)" onSelect={onSelect} />);
    expect(screen.queryByRole('link')).toBeNull();
    const button = screen.getByRole('button', { name: 'Report' });
    button.focus();
    await userEvent.setup().keyboard('{Enter}');
    expect(onSelect).toHaveBeenCalledTimes(1);
  });

  it('renders plain text when there is nothing to activate', () => {
    render(<SearchResultItem title="Archived" data-testid="root" />);
    expect(screen.queryByRole('button')).toBeNull();
    expect(screen.getByTestId('root')).not.toHaveAttribute('data-interactive');
  });

  it('uses backend ranges and mark markers', () => {
    const { container } = render(
      <SearchResultItem
        title="Quarterly plan"
        titleRanges={[[0, 9]]}
        snippet="The <mark>plan</mark> covers hiring."
        snippetMarkTags
      />,
    );
    const marks = [...container.querySelectorAll('mark')].map((m) => m.textContent);
    expect(marks).toEqual(['Quarterly', 'plan']);
  });

  it('accepts a router link component and a translated path label', () => {
    const Link = vi.fn(({ children, ...props }) => (
      <a data-router="" {...props}>
        {children}
      </a>
    ));
    render(
      <SearchResultItem
        title="Doc"
        href="/doc"
        path={['Wiki']}
        linkComponent={Link}
        messages={{ path: 'Emplacement' }}
      />,
    );
    const link = screen.getByRole('link', { name: 'Doc' });
    expect(link).toHaveAttribute('data-router');
    expect(link).toHaveAccessibleDescription('Emplacement: Wiki');
  });

  it('renders on the server', () => {
    const html = renderToString(<SearchResultItem title="Travel" href="/t" terms={['tra']} />);
    expect(html).toContain('mn-search-result-item');
    expect(html).toContain('<mark');
  });
});
