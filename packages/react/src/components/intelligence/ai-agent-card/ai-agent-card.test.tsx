import { describe, expect, it } from 'vitest';
import { render, screen, within } from '@testing-library/react';
import { renderToString } from 'react-dom/server';
import { createRef } from 'react';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { AiAgentCard } from './index.js';

const agent = { id: 'research', name: 'Research agent', role: 'Finds and summarises sources' };

describe('AiAgentCard', () => {
  it('renders a named article with every section and forwards ref and props', async () => {
    const ref = createRef<HTMLElement>();
    render(
      <AiAgentCard
        ref={ref}
        data-testid="root"
        className="extra"
        agent={agent}
        description="Reads documents in this space and answers with citations."
        capabilities={['Summarises', 'Cites sources']}
        tools={[
          { name: 'Search documents', description: 'Full-text search in this space' },
          { name: 'Read file' },
        ]}
        dataAccess={['Documents in this space', 'Your recent notes']}
        owner="Operations team"
        actions={<button type="button">Start a chat</button>}
      />,
    );
    const root = screen.getByTestId('root');
    expect(root.tagName).toBe('ARTICLE');
    expect(root).toHaveClass('mn-ai-agent-card', 'extra');
    expect(ref.current).toBe(root);
    expect(screen.getByRole('article', { name: 'Research agent' })).toBe(root);
    expect(screen.getByRole('heading', { level: 3, name: 'Research agent' })).toBeInTheDocument();
    expect(root).toHaveTextContent('Finds and summarises sources · AI agent');
    const caps = screen.getByRole('list', { name: 'What it can do' });
    expect(within(caps).getAllByRole('listitem')).toHaveLength(2);
    const tools = screen.getByRole('list', { name: 'Tools' });
    expect(within(tools).getAllByRole('listitem')[0]).toHaveTextContent(
      'Search documentsFull-text search in this space',
    );
    expect(screen.getByRole('list', { name: 'Data it can see' })).toHaveTextContent(
      'Your recent notes',
    );
    expect(screen.getByRole('term')).toHaveTextContent('Owner');
    expect(screen.getByRole('definition')).toHaveTextContent('Operations team');
    expect(screen.getByRole('button', { name: 'Start a chat' })).toBeInTheDocument();
    await expectNoA11yViolations();
  });

  it('shows the agent state on its avatar', () => {
    const { container } = render(<AiAgentCard agent={agent} state="working" />);
    expect(container.querySelector('.mn-ai-agent-avatar')).toHaveAttribute('data-state', 'working');
    expect(container.firstElementChild).toHaveAttribute('data-state', 'working');
  });

  it('omits empty sections', () => {
    render(<AiAgentCard agent={agent} capabilities={[]} tools={[]} />);
    expect(screen.queryByRole('list')).not.toBeInTheDocument();
    expect(screen.queryByRole('term')).not.toBeInTheDocument();
  });

  it('compact variant summarises and supports a plain name, div root and external ids', async () => {
    render(
      <AiAgentCard
        variant="compact"
        as="div"
        headingLevel="none"
        nameId="n1"
        descriptionId="d1"
        data-testid="root"
        agent={agent}
        description="Reads documents"
        capabilities={['Summarises']}
        tools={[{ name: 'Search' }, { name: 'Read' }, { name: 'Write' }]}
        dataAccess={['Hidden in compact']}
        owner="Hidden owner"
      />,
    );
    const root = screen.getByTestId('root');
    expect(root.tagName).toBe('DIV');
    expect(root).toHaveAttribute('data-variant', 'compact');
    expect(screen.queryByRole('heading')).not.toBeInTheDocument();
    expect(document.getElementById('n1')).toHaveTextContent('Research agent');
    expect(document.getElementById('d1')).toHaveTextContent('Reads documents');
    expect(root).toHaveTextContent('3 tools');
    expect(screen.getByRole('list', { name: 'What it can do' })).toBeInTheDocument();
    expect(root).not.toHaveTextContent('Hidden in compact');
    expect(root).not.toHaveTextContent('Hidden owner');
    await expectNoA11yViolations();
  });

  it('takes translated strings and a heading level', () => {
    render(
      <AiAgentCard
        agent={agent}
        headingLevel={2}
        tools={[{ name: 'Search' }]}
        messages={{ tools: 'Outils', kind: 'Agent IA' }}
      />,
    );
    expect(screen.getByRole('heading', { level: 2 })).toBeInTheDocument();
    expect(screen.getByRole('list', { name: 'Outils' })).toBeInTheDocument();
    expect(screen.getByRole('article')).toHaveTextContent('Agent IA');
  });

  it('renders on the server', () => {
    const html = renderToString(<AiAgentCard agent={agent} description="Hello" />);
    expect(html).toContain('mn-ai-agent-card');
    expect(html).toContain('Research agent');
  });
});
