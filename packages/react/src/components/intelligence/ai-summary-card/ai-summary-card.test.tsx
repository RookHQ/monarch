import { describe, expect, it, vi } from 'vitest';
import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { createRef } from 'react';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import type { AiSourcePart } from '../ai-conversation/index.js';
import { AiSummaryCard } from './index.js';

const NOW = '2026-03-02T12:00:00Z';
const SUMMARY = 'The account renewed in **January**.\n\n# Next steps\n\n- Book a review';

const sources: AiSourcePart[] = [
  {
    type: 'source',
    id: 's1',
    title: 'Renewal notes',
    url: 'https://example.com/notes',
    kind: 'doc',
  },
  { type: 'source', id: 's2', title: 'CRM record', kind: 'record', product: 'records' },
  { type: 'source', id: 's3', title: 'Unsafe', url: 'javascript:alert(1)', kind: 'web' },
];

describe('AiSummaryCard', () => {
  it('renders a named article marked as an AI summary; forwards props and ref', async () => {
    const ref = createRef<HTMLElement>();
    render(
      <AiSummaryCard
        ref={ref}
        data-testid="root"
        className="extra"
        title="Account summary"
        summary={SUMMARY}
        generatedAt="2026-03-02T09:00:00Z"
        now={NOW}
      />,
    );
    const root = screen.getByTestId('root');
    expect(root).toHaveClass('mn-ai-summary-card', 'extra');
    expect(ref.current).toBe(root);
    const article = screen.getByRole('article', { name: 'Account summary' });
    expect(article).toBe(root);
    expect(article).toHaveTextContent('AI summary');
    expect(article).toHaveAttribute('data-status', 'ready');
    expect(article).not.toHaveAttribute('aria-busy');
    expect(screen.getByRole('heading', { level: 3, name: 'Account summary' })).toBeInTheDocument();
    expect(screen.getByText('January').tagName).toBe('STRONG');
    expect(screen.getByRole('heading', { name: 'Next steps' }).tagName).toBe('H4');
    expect(article).toHaveTextContent('Generated 3 hours ago');
    await expectNoA11yViolations();
  });

  it('never renders raw HTML from the summary', () => {
    const { container } = render(
      <AiSummaryCard title="T" summary={'<img src=x onerror="alert(1)"> text'} />,
    );
    expect(container.querySelector('img')).toBeNull();
    expect(container).toHaveTextContent('<img');
  });

  it('honours headingLevel', () => {
    render(<AiSummaryCard title="T" summary="# Heading" headingLevel={2} />);
    expect(screen.getByRole('heading', { level: 2, name: 'T' })).toBeInTheDocument();
    expect(screen.getByRole('heading', { level: 3, name: 'Heading' })).toBeInTheDocument();
  });

  it('lists sources with safe links only', async () => {
    render(<AiSummaryCard title="T" summary="S" sources={sources} />);
    const list = screen.getByRole('list', { name: 'Sources' });
    expect(list.tagName).toBe('OL');
    const items = within(list).getAllByRole('listitem');
    expect(items).toHaveLength(3);
    const link = within(items[0]!).getByRole('link', { name: 'Renewal notes' });
    expect(link).toHaveAttribute('href', 'https://example.com/notes');
    expect(link).toHaveAttribute('rel', 'noopener noreferrer nofollow');
    expect(items[0]).toHaveTextContent('example.com');
    expect(items[1]).toHaveTextContent('CRM recordrecords');
    expect(within(items[2]!).queryByRole('link')).toBeNull();
    await expectNoA11yViolations();
  });

  it('refreshes from the keyboard; the button is described by the title', async () => {
    const user = userEvent.setup();
    const onRefresh = vi.fn();
    render(<AiSummaryCard title="Account summary" summary="S" onRefresh={onRefresh} />);
    const button = screen.getByRole('button', { name: 'Refresh' });
    expect(button).toHaveAccessibleDescription('Account summary');
    await user.tab();
    expect(button).toHaveFocus();
    await user.keyboard('{Enter}');
    expect(onRefresh).toHaveBeenCalledTimes(1);
  });

  it('is busy while generating and announces start and finish politely, not on mount', () => {
    const { rerender, container } = render(
      <AiSummaryCard title="T" summary="Old" onRefresh={() => {}} />,
    );
    const status = screen.getByRole('status');
    expect(status).toBeEmptyDOMElement();
    rerender(<AiSummaryCard title="T" summary="Old" onRefresh={() => {}} refreshing />);
    const article = screen.getByRole('article');
    expect(article).toHaveAttribute('aria-busy', 'true');
    expect(article).toHaveAttribute('data-status', 'generating');
    expect(screen.getByRole('button', { name: /Refresh/ })).toHaveAttribute('aria-busy', 'true');
    expect(status).toHaveTextContent('Generating summary…');
    expect(container).toHaveTextContent('Old');
    rerender(<AiSummaryCard title="T" summary="New" onRefresh={() => {}} />);
    expect(article).not.toHaveAttribute('aria-busy');
    expect(status).toHaveTextContent('Summary updated.');
  });

  it('shows a placeholder when generating with no summary yet', () => {
    const { container } = render(<AiSummaryCard title="T" summary="" status="generating" />);
    expect(container).toHaveTextContent('Generating summary…');
    expect(container.querySelectorAll('.mn-skeleton')).toHaveLength(3);
    expect(screen.getByRole('status')).toBeEmptyDOMElement();
  });

  it('shows the empty line when there is no summary and nothing is generating', () => {
    render(<AiSummaryCard title="T" summary="  " />);
    expect(screen.getByText('No summary yet.')).toBeInTheDocument();
  });

  it('warns when stale', () => {
    const { container } = render(<AiSummaryCard title="T" summary="S" status="stale" />);
    expect(container).toHaveTextContent(/data changed since this summary was written/);
    expect(screen.getByRole('article')).toHaveAttribute('data-status', 'stale');
  });

  it('announces errors assertively with the detail', async () => {
    render(<AiSummaryCard title="T" summary="" status="error" error="Request timed out." />);
    const alert = screen.getByRole('alert');
    expect(alert).toHaveTextContent('The summary couldn’t be generated.');
    expect(alert).toHaveTextContent('Request timed out.');
    expect(screen.queryByText('No summary yet.')).toBeNull();
    await expectNoA11yViolations();
  });

  it('renders extra actions and accepts messages', () => {
    render(
      <AiSummaryCard
        title="T"
        summary="S"
        onRefresh={() => {}}
        actions={<button type="button">Copy</button>}
        messages={{ aiSummary: 'Résumé IA', refresh: 'Actualiser' }}
      />,
    );
    expect(screen.getByRole('article')).toHaveTextContent('Résumé IA');
    expect(screen.getByRole('button', { name: 'Actualiser' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Copy' })).toBeInTheDocument();
  });

  it('renders on the server', () => {
    const html = renderToString(
      <AiSummaryCard
        title="T"
        summary={SUMMARY}
        generatedAt="2026-03-02T09:00:00Z"
        now={NOW}
        sources={sources}
        status="stale"
      />,
    );
    expect(html).toContain('mn-ai-summary-card');
    expect(html).toContain('AI summary');
    expect(html).toContain('data-status="stale"');
  });
});
