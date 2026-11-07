import { describe, expect, it } from 'vitest';
import { render, screen, within } from '@testing-library/react';
import { renderToString } from 'react-dom/server';
import { createRef } from 'react';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { AiMultiAgentRoster, type AiRosterMember } from './index.js';

const members: AiRosterMember[] = [
  {
    agent: { id: 'lead', name: 'Coordinator', role: 'Plans the work' },
    state: 'working',
    task: 'Splitting the request',
  },
  { agent: { id: 'research', name: 'Research agent' }, state: 'waiting', task: 'Needs access' },
  { agent: { id: 'writer', name: 'Writing agent' }, state: 'idle' },
];

describe('AiMultiAgentRoster', () => {
  it('lists every agent with its role, task and state as text', async () => {
    const ref = createRef<HTMLDivElement>();
    render(<AiMultiAgentRoster ref={ref} agents={members} data-testid="root" className="x" />);
    const root = screen.getByTestId('root');
    expect(ref.current).toBe(root);
    expect(root).toHaveClass('mn-ai-multi-agent-roster', 'x');
    const items = within(screen.getByRole('list', { name: 'Agents' })).getAllByRole('listitem');
    expect(items).toHaveLength(3);
    expect(items[0]).toHaveTextContent('Coordinator (AI agent)Plans the work');
    expect(items[0]).toHaveTextContent('Splitting the request, Working');
    expect(items[1]).toHaveAttribute('data-state', 'waiting');
    expect(items[1]).toHaveTextContent('Waiting for you');
    expect(items[2]).toHaveTextContent('Idle');
    expect(root).toHaveTextContent('3 agents · 1 working · 1 waiting for you');
    // Pictures are silent in the list: the text already says the state.
    expect(screen.queryByRole('img')).toBeNull();
    await expectNoA11yViolations();
  });

  it('announces state changes, not the initial state', () => {
    const { rerender } = render(<AiMultiAgentRoster agents={members} />);
    const status = screen.getByRole('status');
    expect(status).toBeEmptyDOMElement();
    rerender(
      <AiMultiAgentRoster
        agents={members.map((m) =>
          m.agent.id === 'writer'
            ? { ...m, state: 'working' }
            : m.agent.id === 'research'
              ? { ...m, state: 'error' }
              : m,
        )}
      />,
    );
    expect(status).toHaveTextContent('Research agent: Error. Writing agent: Working');
  });

  it('stacks pictures, attention first, with the rest behind +N', async () => {
    render(<AiMultiAgentRoster agents={members} variant="stack" max={2} />);
    const list = screen.getByRole('list', { name: 'Agents' });
    const imgs = within(list).getAllByRole('img');
    expect(imgs.map((i) => i.getAttribute('aria-label'))).toEqual([
      'Research agent, AI agent, Waiting for you',
      'Coordinator, AI agent, Working',
    ]);
    const more = within(list).getAllByRole('listitem')[2]!;
    expect(more).toHaveTextContent('+1');
    expect(more).toHaveTextContent('Writing agent, Idle');
    await expectNoA11yViolations();
  });

  it('shows an empty state, hides the summary and uses translated strings', () => {
    const { rerender } = render(
      <AiMultiAgentRoster agents={[]} messages={{ empty: 'Aucun agent.' }} />,
    );
    expect(screen.getByText('Aucun agent.')).toBeInTheDocument();
    rerender(
      <AiMultiAgentRoster
        agents={members.slice(0, 1)}
        showSummary={false}
        announce={false}
        messages={{ label: 'Agents IA', agentKind: 'agent IA', states: { working: 'En cours' } }}
      />,
    );
    expect(screen.getByRole('list', { name: 'Agents IA' })).toHaveTextContent(
      'Coordinator (agent IA)',
    );
    expect(screen.getByText('En cours')).toBeInTheDocument();
    expect(screen.queryByText(/1 agent/)).toBeNull();
    expect(screen.queryByRole('status')).toBeNull();
  });

  it('renders on the server', () => {
    const html = renderToString(<AiMultiAgentRoster agents={members} variant="stack" />);
    expect(html).toContain('mn-ai-multi-agent-roster');
    expect(html).toContain('Coordinator, AI agent, Working');
  });
});
