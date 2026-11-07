import { describe, expect, it, vi } from 'vitest';
import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { AiSourceList, type AiSourceListItem } from './index.js';

const make = (n: number, extra: Partial<AiSourceListItem> = {}): AiSourceListItem => ({
  type: 'source',
  id: `s${n}`,
  title: `Source title ${n}`,
  url: `https://example.com/${n}`,
  kind: 'web',
  ...extra,
});

function withoutUrl({ url: _url, ...rest }: AiSourceListItem): AiSourceListItem {
  return rest;
}

describe('AiSourceList', () => {
  it('is a labelled section listing sources with links, meta and snippets', async () => {
    render(
      <AiSourceList
        data-testid="root"
        sources={[
          make(1, { snippet: 'A short excerpt.' }),
          withoutUrl(make(2, { kind: 'record', product: 'CRM' })),
        ]}
      />,
    );
    const root = screen.getByRole('region', { name: 'Sources' });
    expect(root).toHaveClass('mn-ai-source-list');
    const link = screen.getByRole('link', { name: 'Source title 1 (opens in a new tab)' });
    expect(link).toHaveAttribute('href', 'https://example.com/1');
    expect(link).toHaveAttribute('rel', 'noopener noreferrer nofollow');
    expect(screen.getByText('A short excerpt.')).toBeInTheDocument();
    expect(screen.getByText('CRM · Record')).toBeInTheDocument();
    expect(screen.getByText('Source title 2').tagName).toBe('SPAN');
    expect(screen.getByRole('list', { name: 'Sources' })).toBeInTheDocument();
    expect(root).toHaveTextContent('[1]');
    await expectNoA11yViolations();
  });

  it('groups into Used and Considered', async () => {
    render(<AiSourceList sources={[make(1), make(2, { used: false }), make(3, { used: true })]} />);
    const used = screen.getByRole('list', { name: 'Used (2)' });
    const considered = screen.getByRole('list', { name: 'Considered (1)' });
    expect(within(used).getAllByRole('listitem')).toHaveLength(2);
    expect(within(considered).getByText('Source title 2')).toBeInTheDocument();
    // Numbers follow the original order, matching citation markers.
    expect(considered).toHaveTextContent('[2]');
    await expectNoA11yViolations();
  });

  it('collapses past three and expands with an aria-expanded toggle', async () => {
    render(<AiSourceList sources={[1, 2, 3, 4, 5].map((n) => make(n))} />);
    expect(screen.getAllByRole('listitem')).toHaveLength(3);
    const toggle = screen.getByRole('button', { name: 'Show all 5 sources' });
    expect(toggle).toHaveAttribute('aria-expanded', 'false');
    const user = userEvent.setup();
    await user.click(toggle);
    expect(screen.getAllByRole('listitem')).toHaveLength(5);
    expect(toggle).toHaveAttribute('aria-expanded', 'true');
    expect(toggle).toHaveTextContent('Show fewer');
    toggle.focus();
    await user.keyboard('{Enter}');
    expect(screen.getAllByRole('listitem')).toHaveLength(3);
  });

  it('does not collapse at three or fewer', () => {
    render(<AiSourceList sources={[make(1), make(2), make(3)]} />);
    expect(screen.queryByRole('button')).toBeNull();
  });

  it('makes titles buttons that call onOpen', async () => {
    const onOpen = vi.fn();
    const sources = [make(1)];
    render(<AiSourceList sources={sources} onOpen={onOpen} />);
    const user = userEvent.setup();
    await user.tab();
    const title = screen.getByRole('button', { name: 'Source title 1' });
    expect(title).toHaveFocus();
    await user.keyboard('{Enter}');
    expect(onOpen).toHaveBeenCalledWith(sources[0]);
  });

  it('never links to unsafe URLs or loads unsafe favicons', () => {
    const { container } = render(
      <AiSourceList
        sources={[make(1, { url: 'javascript:alert(1)', favicon: 'javascript:alert(1)' })]}
      />,
    );
    expect(screen.queryByRole('link')).toBeNull();
    expect(container.querySelector('img')).toBeNull();
  });

  it('uses translated messages', () => {
    render(<AiSourceList sources={[make(1)]} messages={{ heading: 'Quellen' }} />);
    expect(screen.getByRole('region', { name: 'Quellen' })).toBeInTheDocument();
  });

  it('renders on the server', () => {
    const html = renderToString(
      <AiSourceList sources={[make(1), make(2, { favicon: 'https://example.com/f.ico' })]} />,
    );
    expect(html).toContain('mn-ai-source-list');
    expect(html).toContain('<img');
  });
});
