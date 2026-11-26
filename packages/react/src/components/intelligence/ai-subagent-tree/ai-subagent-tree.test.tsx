import { describe, expect, it, vi } from 'vitest';
import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { createRef, useState } from 'react';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import {
  AiSubagentTree,
  countSubagentStatuses,
  defaultExpandedSubagentIds,
  flattenSubagentTree,
  type AiSubagentNode,
} from './index.js';

const AGENTS: AiSubagentNode[] = [
  {
    id: 'lead',
    name: 'Lead',
    role: 'Plans the report and hands out the work',
    status: 'working',
    activity: 'Waiting for the researcher',
    children: [
      {
        id: 'research',
        name: 'Researcher',
        role: 'Find the latest filings',
        status: 'working',
        activity: 'Reading the third filing',
        stepsDone: 3,
        stepsTotal: 7,
        toolCalls: 12,
        startedAt: '2026-01-01T12:01:00Z',
        children: [
          {
            id: 'fetch',
            name: 'Fetcher',
            status: 'done',
            activity: 'Should not show',
            summary: 'Downloaded 3 filings',
            startedAt: '2026-01-01T12:01:00Z',
            endedAt: '2026-01-01T12:04:00Z',
          },
          { id: 'parse', name: 'Parser', status: 'waiting', summary: 'Not shown yet' },
        ],
      },
      { id: 'write', name: 'Writer', status: 'needs-input' },
      {
        id: 'check',
        name: 'Checker',
        status: 'done',
        summary: 'All figures match',
        children: [{ id: 'lint', name: 'Linter', status: 'done' }],
      },
    ],
  },
];

const item = (id: string) => {
  const el = screen.getAllByRole('treeitem').find((row) => row.dataset.id === id);
  if (!el) throw new Error(`No visible tree item "${id}"`);
  return el;
};
const visibleIds = () => screen.getAllByRole('treeitem').map((row) => row.dataset.id);

/** Replaces one agent's fields anywhere in the tree. */
function patch(
  agents: readonly AiSubagentNode[],
  id: string,
  change: Partial<AiSubagentNode>,
): AiSubagentNode[] {
  return agents.map((a) => ({
    ...a,
    ...(a.id === id ? change : {}),
    ...(a.children ? { children: patch(a.children, id, change) } : {}),
  }));
}

describe('AiSubagentTree', () => {
  it('renders a named tree with levels, positions and a run summary', async () => {
    const ref = createRef<HTMLDivElement>();
    render(<AiSubagentTree ref={ref} data-testid="root" className="extra" agents={AGENTS} />);
    const root = screen.getByTestId('root');
    expect(root).toHaveClass('mn-ai-subagent-tree', 'extra');
    expect(ref.current).toBe(root);
    expect(screen.getByRole('tree', { name: 'Agents' })).toBeInTheDocument();
    expect(root).toHaveTextContent('7 agents: 2 working, 1 needs you, 1 waiting, 3 done');

    expect(item('lead')).toHaveAttribute('aria-level', '1');
    expect(item('lead')).toHaveAttribute('aria-setsize', '1');
    expect(item('lead')).toHaveAttribute('aria-expanded', 'true');
    expect(item('write')).toHaveAttribute('aria-level', '2');
    expect(item('write')).toHaveAttribute('aria-setsize', '3');
    expect(item('write')).toHaveAttribute('aria-posinset', '2');
    expect(item('write')).not.toHaveAttribute('aria-expanded');
    expect(item('fetch')).toHaveAttribute('aria-level', '3');
    await expectNoA11yViolations();
  });

  it('shows status in words, activity while working, progress, times and a result when ended', () => {
    render(<AiSubagentTree agents={AGENTS} locale="en-GB" timeZone="UTC" />);
    const researcher = item('research');
    expect(researcher).toHaveAccessibleName(/^Researcher Working/);
    expect(researcher).toHaveTextContent('Find the latest filings');
    expect(researcher).toHaveTextContent('Reading the third filing');
    expect(researcher).toHaveTextContent('3 of 7 steps');
    expect(researcher).toHaveTextContent('12 tool calls');
    expect(researcher).toHaveTextContent('Started 12:01');
    expect(item('write')).toHaveTextContent('Needs input');
    const fetcher = item('fetch');
    expect(fetcher).toHaveTextContent('Done');
    expect(fetcher).toHaveTextContent('Downloaded 3 filings');
    expect(fetcher).toHaveTextContent('12:01 to 12:04');
    expect(fetcher).not.toHaveTextContent('Should not show');
    expect(item('parse')).toHaveTextContent('Waiting');
    expect(item('parse')).not.toHaveTextContent('Not shown yet');
  });

  it('expands agents that are working or need input by default, and the rest stay closed', () => {
    render(<AiSubagentTree agents={AGENTS} />);
    expect(visibleIds()).toEqual(['lead', 'research', 'fetch', 'parse', 'write', 'check']);
    expect(item('research')).toHaveAttribute('aria-expanded', 'true');
    expect(item('check')).toHaveAttribute('aria-expanded', 'false');
  });

  it('opens an agent once when it becomes active, and keeps it closed after the person closes it', async () => {
    const user = userEvent.setup();
    const { rerender } = render(<AiSubagentTree agents={AGENTS} />);
    rerender(<AiSubagentTree agents={patch(AGENTS, 'lint', { status: 'working' })} />);
    expect(item('check')).toHaveAttribute('aria-expanded', 'true');
    expect(item('lint')).toBeInTheDocument();
    await user.click(item('check').querySelector('.mn-tree-view__toggle') as HTMLElement);
    expect(item('check')).toHaveAttribute('aria-expanded', 'false');
    rerender(<AiSubagentTree agents={patch(AGENTS, 'lint', { status: 'needs-input' })} />);
    expect(item('check')).toHaveAttribute('aria-expanded', 'false');
  });

  it('has one tab stop and moves with Up, Down, Home and End', async () => {
    const user = userEvent.setup();
    render(<AiSubagentTree agents={AGENTS} />);
    await user.tab();
    expect(item('lead')).toHaveFocus();
    await user.keyboard('{ArrowDown}');
    expect(item('research')).toHaveFocus();
    await user.keyboard('{ArrowDown}{ArrowDown}{ArrowDown}');
    expect(item('write')).toHaveFocus();
    await user.keyboard('{ArrowUp}');
    expect(item('parse')).toHaveFocus();
    await user.keyboard('{End}');
    expect(item('check')).toHaveFocus();
    await user.keyboard('{ArrowDown}');
    expect(item('check')).toHaveFocus();
    await user.keyboard('{Home}');
    expect(item('lead')).toHaveFocus();
    await user.keyboard('{ArrowUp}');
    expect(item('lead')).toHaveFocus();
    await user.keyboard('{ArrowDown}');
    expect(item('research')).toHaveAttribute('tabindex', '0');
    expect(screen.getAllByRole('treeitem').filter((row) => row.tabIndex === 0)).toHaveLength(1);
    // Nothing is selected, so Tab leaves the component.
    await user.tab();
    expect(document.body).toHaveFocus();
  });

  it('expands and collapses with Right and Left, and moves to the first child or the parent', async () => {
    const user = userEvent.setup();
    render(<AiSubagentTree agents={AGENTS} />);
    await user.tab();
    await user.keyboard('{End}');
    expect(item('check')).toHaveFocus();
    await user.keyboard('{ArrowRight}');
    expect(item('check')).toHaveAttribute('aria-expanded', 'true');
    expect(item('check')).toHaveFocus();
    await user.keyboard('{ArrowRight}');
    expect(item('lint')).toHaveFocus();
    // Right on a leaf does nothing.
    await user.keyboard('{ArrowRight}');
    expect(item('lint')).toHaveFocus();
    await user.keyboard('{ArrowLeft}');
    expect(item('check')).toHaveFocus();
    await user.keyboard('{ArrowLeft}');
    expect(item('check')).toHaveAttribute('aria-expanded', 'false');
    expect(visibleIds()).not.toContain('lint');
    await user.keyboard('{ArrowLeft}');
    expect(item('lead')).toHaveFocus();
    await user.keyboard('{ArrowLeft}');
    expect(item('lead')).toHaveAttribute('aria-expanded', 'false');
    expect(visibleIds()).toEqual(['lead']);
    // Left on a closed top-level agent does nothing.
    await user.keyboard('{ArrowLeft}');
    expect(item('lead')).toHaveFocus();
  });

  it('finds an agent by typing the start of its name', async () => {
    const user = userEvent.setup();
    render(<AiSubagentTree agents={AGENTS} />);
    await user.tab();
    await user.keyboard('w');
    expect(item('write')).toHaveFocus();
    const now = Date.now();
    const clock = vi.spyOn(Date, 'now').mockReturnValue(now + 2000);
    await user.keyboard('pa');
    expect(item('parse')).toHaveFocus();
    clock.mockRestore();
  });

  it('selects with Enter, Space and click, and reports the id (uncontrolled)', async () => {
    const user = userEvent.setup();
    const onSelect = vi.fn();
    render(<AiSubagentTree agents={AGENTS} onSelect={onSelect} />);
    await user.tab();
    // Moving focus does not select.
    await user.keyboard('{ArrowDown}');
    expect(onSelect).not.toHaveBeenCalled();
    expect(item('research')).toHaveAttribute('aria-selected', 'false');
    await user.keyboard('{Enter}');
    expect(item('research')).toHaveAttribute('aria-selected', 'true');
    expect(onSelect).toHaveBeenLastCalledWith(
      'research',
      expect.objectContaining({ id: 'research' }),
    );
    await user.keyboard('{End} ');
    expect(item('check')).toHaveAttribute('aria-selected', 'true');
    expect(item('research')).toHaveAttribute('aria-selected', 'false');
    expect(onSelect).toHaveBeenLastCalledWith(
      'check',
      expect.objectContaining({ name: 'Checker' }),
    );
    await user.click(screen.getByText('Writer'));
    expect(item('write')).toHaveAttribute('aria-selected', 'true');
    expect(onSelect).toHaveBeenCalledTimes(3);
  });

  it('follows a controlled selectedId', async () => {
    const user = userEvent.setup();
    const onSelect = vi.fn();
    const { rerender } = render(
      <AiSubagentTree agents={AGENTS} selectedId="write" onSelect={onSelect} />,
    );
    expect(item('write')).toHaveAttribute('aria-selected', 'true');
    await user.click(screen.getByText('Researcher'));
    expect(onSelect).toHaveBeenCalledWith('research', expect.anything());
    expect(item('write')).toHaveAttribute('aria-selected', 'true');
    rerender(<AiSubagentTree agents={AGENTS} selectedId="research" onSelect={onSelect} />);
    expect(item('research')).toHaveAttribute('aria-selected', 'true');
    rerender(<AiSubagentTree agents={AGENTS} selectedId={null} onSelect={onSelect} />);
    expect(screen.queryByRole('treeitem', { selected: true })).toBeNull();
  });

  it('follows controlled expandedIds and reports changes; defaultExpandedIds seeds them', async () => {
    const user = userEvent.setup();
    const onExpandedChange = vi.fn();
    function Controlled() {
      const [ids, setIds] = useState<string[]>(['lead']);
      return (
        <AiSubagentTree
          agents={AGENTS}
          expandedIds={ids}
          onExpandedChange={(next) => {
            onExpandedChange(next);
            setIds(next);
          }}
        />
      );
    }
    const { unmount } = render(<Controlled />);
    expect(visibleIds()).toEqual(['lead', 'research', 'write', 'check']);
    await user.tab();
    await user.keyboard('{ArrowDown}{ArrowRight}');
    expect(onExpandedChange).toHaveBeenLastCalledWith(['lead', 'research']);
    expect(visibleIds()).toContain('fetch');
    unmount();

    const fixed = vi.fn();
    render(<AiSubagentTree agents={AGENTS} expandedIds={[]} onExpandedChange={fixed} />);
    expect(visibleIds()).toEqual(['lead']);
    await user.click(item('lead').querySelector('.mn-tree-view__toggle') as HTMLElement);
    expect(fixed).toHaveBeenCalledWith(['lead']);
    expect(visibleIds()).toEqual(['lead']);
  });

  it('seeds expansion from defaultExpandedIds', () => {
    render(<AiSubagentTree agents={AGENTS} defaultExpandedIds={['lead', 'check']} />);
    expect(visibleIds()).toEqual(['lead', 'research', 'write', 'check', 'lint']);
  });

  it('offers Stop, Open and Respond for the selected agent only when they apply', async () => {
    const user = userEvent.setup();
    const onStop = vi.fn();
    const onOpen = vi.fn();
    const onRespond = vi.fn();
    render(
      <AiSubagentTree agents={AGENTS} onStop={onStop} onOpen={onOpen} onRespond={onRespond} />,
    );
    // Nothing selected: no actions, and nothing was called.
    expect(screen.queryByRole('button')).toBeNull();

    await user.tab();
    await user.keyboard('{ArrowDown}{Enter}');
    const bar = screen.getByRole('group', { name: 'Actions for Researcher' });
    expect(within(bar).queryByRole('button', { name: 'Respond' })).toBeNull();
    // Tab goes from the tree to the actions.
    await user.tab();
    expect(within(bar).getByRole('button', { name: 'Open' })).toHaveFocus();
    await user.keyboard('{Enter}');
    expect(onOpen).toHaveBeenCalledWith('research', expect.objectContaining({ id: 'research' }));
    await user.tab();
    expect(within(bar).getByRole('button', { name: 'Stop' })).toHaveFocus();
    expect(onStop).not.toHaveBeenCalled();
    await user.keyboard(' ');
    expect(onStop).toHaveBeenCalledTimes(1);
    expect(onStop).toHaveBeenCalledWith('research', expect.objectContaining({ id: 'research' }));
    // Focus returns to the agent's row, so it is not lost when Stop goes away.
    expect(item('research')).toHaveFocus();

    await user.click(screen.getByText('Writer'));
    const writer = screen.getByRole('group', { name: 'Actions for Writer' });
    expect(within(writer).queryByRole('button', { name: 'Stop' })).toBeNull();
    await user.click(within(writer).getByRole('button', { name: 'Respond' }));
    expect(onRespond).toHaveBeenCalledWith('write', expect.objectContaining({ id: 'write' }));

    await user.click(screen.getByText('Parser'));
    expect(screen.getByRole('button', { name: 'Stop' })).toBeInTheDocument();

    await user.click(screen.getByText('Checker'));
    const checker = screen.getByRole('group', { name: 'Actions for Checker' });
    expect(within(checker).getAllByRole('button')).toHaveLength(1);
    expect(within(checker).getByRole('button', { name: 'Open' })).toBeInTheDocument();
    await expectNoA11yViolations();
  });

  it('links an agent to its conversation part and offers Show in conversation', async () => {
    const user = userEvent.setup();
    const onOpenPart = vi.fn();
    const linked = patch(AGENTS, 'check', { partId: 'call-3', messageId: 'msg-9' });
    const { container, rerender } = render(
      <AiSubagentTree agents={linked} onOpenPart={onOpenPart} />,
    );
    const agent = container.querySelector('[data-part-id="call-3"]');
    expect(agent).toHaveAttribute('data-message-id', 'msg-9');
    expect(item('check')).toContainElement(agent as HTMLElement);
    // An agent without a link offers nothing.
    await user.click(screen.getByText('Writer'));
    expect(screen.queryByRole('button', { name: 'Show in conversation' })).toBeNull();
    await user.click(screen.getByText('Checker'));
    const bar = screen.getByRole('group', { name: 'Actions for Checker' });
    await user.click(within(bar).getByRole('button', { name: 'Show in conversation' }));
    expect(onOpenPart).toHaveBeenCalledWith(
      { partId: 'call-3', messageId: 'msg-9' },
      expect.objectContaining({ id: 'check' }),
    );
    rerender(
      <AiSubagentTree
        agents={linked}
        onOpenPart={onOpenPart}
        messages={{ openPart: 'Im Verlauf zeigen' }}
      />,
    );
    expect(screen.getByRole('button', { name: 'Im Verlauf zeigen' })).toBeInTheDocument();
  });

  it('isolates the direction of names, roles, activity and results', () => {
    render(<AiSubagentTree agents={AGENTS} defaultExpandedIds={['lead', 'research']} />);
    expect(screen.getByText('Researcher').tagName).toBe('BDI');
    expect(screen.getByText('Find the latest filings')).toHaveAttribute('dir', 'auto');
    expect(screen.getByText('Reading the third filing')).toHaveAttribute('dir', 'auto');
    expect(screen.getByText('Downloaded 3 filings')).toHaveAttribute('dir', 'auto');
  });

  it('shows no action bar when no callback applies', () => {
    render(<AiSubagentTree agents={AGENTS} defaultSelectedId="check" onStop={() => {}} />);
    expect(item('check')).toHaveAttribute('aria-selected', 'true');
    expect(screen.queryByRole('group')).toBeNull();
    expect(screen.queryByRole('button')).toBeNull();
  });

  it('announces each status change once, politely, and never the first render', () => {
    const { rerender } = render(<AiSubagentTree agents={AGENTS} />);
    const live = screen
      .getAllByRole('status')
      .find((el) => el.getAttribute('aria-atomic') === 'true') as HTMLElement;
    expect(live).toHaveAttribute('aria-live', 'polite');
    expect(live).toBeEmptyDOMElement();

    const next = patch(patch(AGENTS, 'research', { status: 'done' }), 'write', {
      status: 'working',
    });
    rerender(<AiSubagentTree agents={next} />);
    expect(live).toHaveTextContent('Researcher: Done Writer: Working');
    expect(screen.getByText('7 agents: 2 working, 1 waiting, 4 done')).toBeInTheDocument();

    // The same statuses again (a new array, as a product re-rendering would pass) say nothing new.
    const again = patch(next, 'parse', { status: 'failed' });
    rerender(<AiSubagentTree agents={again} />);
    expect(live).toHaveTextContent('Parser: Failed');
    expect(live).not.toHaveTextContent('Researcher');

    // A sub-agent that joins already needing input is announced; one that joins waiting is not.
    const joined = patch(again, 'write', {
      children: [
        { id: 'ask', name: 'Asker', status: 'needs-input' },
        { id: 'idle', name: 'Idler', status: 'waiting' },
      ],
    });
    rerender(<AiSubagentTree agents={joined} />);
    expect(live).toHaveTextContent('Asker: Needs input');
    expect(live).not.toHaveTextContent('Idler');
  });

  it('exports helpers that flatten the tree, count statuses and pick the default expansion', () => {
    const flat = flattenSubagentTree(AGENTS);
    expect(flat.map((f) => f.agent.id)).toEqual([
      'lead',
      'research',
      'fetch',
      'parse',
      'write',
      'check',
      'lint',
    ]);
    expect(flat[3]).toMatchObject({ level: 3, parentId: 'research', position: 2, setSize: 2 });
    expect(flat[0]).toMatchObject({ level: 1, parentId: null, position: 1, setSize: 1 });
    expect(countSubagentStatuses(AGENTS)).toEqual({
      total: 7,
      waiting: 1,
      working: 2,
      'needs-input': 1,
      done: 3,
      failed: 0,
      stopped: 0,
    });
    expect(countSubagentStatuses([])).toMatchObject({ total: 0, working: 0 });
    expect(defaultExpandedSubagentIds(AGENTS)).toEqual(['lead', 'research']);
    // A finished parent stays open while something under it is still active.
    expect(defaultExpandedSubagentIds(patch(AGENTS, 'lint', { status: 'needs-input' }))).toEqual([
      'lead',
      'research',
      'check',
    ]);
  });

  it('accepts translated strings and formats numbers for the locale', () => {
    const big = patch(AGENTS, 'research', { toolCalls: 1234, stepsTotal: undefined });
    render(
      <AiSubagentTree
        agents={big}
        label="Agents IA"
        locale="de-DE"
        defaultSelectedId="research"
        onStop={() => {}}
        messages={{
          statuses: { working: 'En cours' },
          statusCounts: { working: (_, n) => `${n} en cours` },
          agentCount: (_, n) => `${n} agents IA`,
          toolCalls: (_, n) => `${n} appels`,
          stop: 'Arrêter',
          actionsFor: (name) => `Actions pour ${name}`,
        }}
      />,
    );
    expect(screen.getByRole('tree', { name: 'Agents IA' })).toBeInTheDocument();
    expect(screen.getByText(/^7 agents IA: 2 en cours, 1 needs you/)).toBeInTheDocument();
    expect(item('research')).toHaveTextContent('En cours');
    expect(item('research')).toHaveTextContent('1.234 appels');
    expect(item('research')).toHaveTextContent('3 steps');
    expect(
      within(screen.getByRole('group', { name: 'Actions pour Researcher' })).getByRole('button', {
        name: 'Arrêter',
      }),
    ).toBeInTheDocument();
  });

  it('says so when there are no agents', async () => {
    render(<AiSubagentTree data-testid="root" agents={[]} />);
    expect(screen.getByTestId('root')).toHaveAttribute('data-empty', 'true');
    expect(screen.getByText('No agents yet')).toBeInTheDocument();
    expect(screen.queryByRole('tree')).toBeNull();
    await expectNoA11yViolations();
  });

  it('renders on the server', () => {
    const html = renderToString(
      <AiSubagentTree agents={AGENTS} locale="en-GB" timeZone="UTC" defaultSelectedId="write" />,
    );
    expect(html).toContain('mn-ai-subagent-tree');
    expect(html).toContain('7 agents');
    expect(html).toContain('Reading the third filing');
    expect(html).not.toContain('Linter');
  });
});
