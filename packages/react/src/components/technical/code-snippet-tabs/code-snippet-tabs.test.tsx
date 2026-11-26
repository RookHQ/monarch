import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { CodeSnippetTabs, type CodeSnippet } from './index.js';

const snippets: CodeSnippet[] = [
  { label: 'cURL', language: 'bash', code: 'curl https://api.example.com/v1/items' },
  { label: 'JavaScript', language: 'js', code: "await fetch('https://api.example.com/v1/items');" },
  { label: 'Python', language: 'python', code: "requests.get('https://api.example.com/v1/items')" },
];

beforeEach(() => {
  const store = new Map<string, string>();
  vi.stubGlobal('localStorage', {
    getItem: (k: string) => store.get(k) ?? null,
    setItem: (k: string, v: string) => void store.set(k, v),
    removeItem: (k: string) => void store.delete(k),
    clear: () => store.clear(),
  });
});
afterEach(() => vi.unstubAllGlobals());

describe('CodeSnippetTabs', () => {
  it('renders one named tab per snippet over CodeTabs and passes axe', async () => {
    render(<CodeSnippetTabs snippets={snippets} label="Request examples" data-testid="root" />);
    const root = screen.getByTestId('root');
    expect(root).toHaveClass('mn-code-snippet-tabs', 'mn-code-tabs');
    const tablist = screen.getByRole('tablist', { name: 'Request examples' });
    expect(
      within(tablist)
        .getAllByRole('tab')
        .map((t) => t.textContent),
    ).toEqual(['cURL', 'JavaScript', 'Python']);
    expect(
      within(screen.getByRole('tabpanel')).getByRole('region', { name: 'cURL, Request examples' }),
    ).toBeInTheDocument();
    await expectNoA11yViolations();
  });

  it('switches with arrow keys and copies the visible snippet', async () => {
    const onValueChange = vi.fn();
    const user = userEvent.setup();
    render(<CodeSnippetTabs snippets={snippets} onValueChange={onValueChange} />);
    await user.tab();
    expect(screen.getByRole('tab', { name: 'cURL' })).toHaveFocus();
    await user.keyboard('{ArrowRight}');
    expect(onValueChange).toHaveBeenCalledWith('JavaScript');
    await user.click(within(screen.getByRole('tabpanel')).getByRole('button', { name: /Copy/ }));
    await waitFor(async () => expect(await navigator.clipboard.readText()).toBe(snippets[1]!.code));
  });

  it('keeps sets with the same syncKey on the same language and remembers it', async () => {
    const user = userEvent.setup();
    render(
      <>
        <CodeSnippetTabs snippets={snippets} syncKey="docs" label="First" />
        <CodeSnippetTabs snippets={snippets.slice(0, 2)} syncKey="docs" label="Second" />
        <CodeSnippetTabs snippets={snippets} label="Unsynced" />
      </>,
    );
    const first = screen.getByRole('tablist', { name: 'First' });
    const second = screen.getByRole('tablist', { name: 'Second' });
    const unsynced = screen.getByRole('tablist', { name: 'Unsynced' });
    await user.click(within(first).getByRole('tab', { name: 'JavaScript' }));
    expect(within(second).getByRole('tab', { name: 'JavaScript' })).toHaveAttribute(
      'aria-selected',
      'true',
    );
    expect(within(unsynced).getByRole('tab', { name: 'cURL' })).toHaveAttribute(
      'aria-selected',
      'true',
    );
    expect(window.localStorage.getItem('mn-code-snippet-tabs:docs')).toBe('JavaScript');
    // A set without that language keeps its own choice.
    await user.click(within(first).getByRole('tab', { name: 'Python' }));
    expect(within(second).getByRole('tab', { name: 'JavaScript' })).toHaveAttribute(
      'aria-selected',
      'true',
    );
  });

  it('restores the remembered language after mount', async () => {
    window.localStorage.setItem('mn-code-snippet-tabs:docs', 'Python');
    render(<CodeSnippetTabs snippets={snippets} syncKey="docs" />);
    await waitFor(() =>
      expect(screen.getByRole('tab', { name: 'Python' })).toHaveAttribute('aria-selected', 'true'),
    );
  });

  it('can be controlled', () => {
    const { rerender } = render(<CodeSnippetTabs snippets={snippets} value="Python" />);
    expect(screen.getByRole('tab', { name: 'Python' })).toHaveAttribute('aria-selected', 'true');
    rerender(<CodeSnippetTabs snippets={snippets} value="cURL" />);
    expect(screen.getByRole('tab', { name: 'cURL' })).toHaveAttribute('aria-selected', 'true');
  });

  it('renders on the server without reading storage', () => {
    const html = renderToString(<CodeSnippetTabs snippets={snippets} syncKey="docs" />);
    expect(html).toContain('mn-code-snippet-tabs');
    expect(html).toContain('curl https://api.example.com');
  });
});
