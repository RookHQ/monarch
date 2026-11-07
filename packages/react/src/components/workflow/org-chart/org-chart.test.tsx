import { describe, expect, it, vi } from 'vitest';
import { useState } from 'react';
import { act, fireEvent, render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { OrgChart, type OrgChartNode } from './index.js';

const NODES: OrgChartNode[] = [
  { id: 'ceo', name: 'Alex Kim', title: 'Chief executive' },
  { id: 'cto', parentId: 'ceo', name: 'Sam Lee', title: 'Engineering lead', meta: 'Engineering' },
  { id: 'cfo', parentId: 'ceo', name: 'Riya Shah', title: 'Finance lead', dashed: true },
  { id: 'dev1', parentId: 'cto', name: 'Jo Park', title: 'Engineer' },
  { id: 'dev2', parentId: 'cto', name: 'Ola Berg', title: 'Engineer' },
  { id: 'vac', parentId: 'cto', name: 'Designer', kind: 'vacancy' },
  { id: 'acc', parentId: 'cfo', name: 'Mia Chen', title: 'Accountant' },
];

const item = (name: RegExp | string) => screen.getByRole('treeitem', { name });

describe('OrgChart', () => {
  it('renders an ARIA tree with levels, expansion and hidden facts', async () => {
    render(<OrgChart nodes={NODES} data-testid="o" />);
    expect(screen.getByTestId('o')).toHaveClass('mn-org-chart');
    expect(screen.getByTestId('o')).toHaveAttribute('data-layout', 'auto');
    const tree = screen.getByRole('tree', { name: 'Organisation chart' });
    expect(tree).toHaveAccessibleDescription(/arrow keys/);
    const ceo = item(/^Alex Kim/);
    expect(ceo).toHaveAttribute('aria-level', '1');
    expect(ceo).toHaveAttribute('aria-expanded', 'true');
    expect(ceo).toHaveAccessibleName('Alex Kim Chief executive , 2 direct reports');
    expect(ceo).toHaveAttribute('tabindex', '0');
    // Top two levels open by default.
    expect(item(/^Sam Lee/)).toHaveAttribute('aria-expanded', 'true');
    expect(item(/^Designer/)).toHaveAccessibleName(/Open position/);
    expect(item(/^Riya Shah/)).toHaveAccessibleName(/temporary reporting line/);
    expect(item(/^Jo Park/)).toHaveAttribute('aria-level', '3');
    expect(item(/^Jo Park/)).toHaveAttribute('tabindex', '-1');
    await expectNoA11yViolations();
  });

  it('follows the tree keyboard model', async () => {
    const onSelect = vi.fn();
    render(<OrgChart nodes={NODES} onSelect={onSelect} zoomable={false} />);
    const user = userEvent.setup();
    await user.tab();
    expect(item(/^Alex Kim/)).toHaveFocus();
    await user.keyboard('{ArrowDown}');
    expect(item(/^Sam Lee/)).toHaveFocus();
    await user.keyboard('{ArrowLeft}');
    expect(item(/^Sam Lee/)).toHaveAttribute('aria-expanded', 'false');
    expect(screen.queryByRole('treeitem', { name: /^Jo Park/ })).toBeNull();
    await user.keyboard('{ArrowRight}');
    expect(item(/^Sam Lee/)).toHaveAttribute('aria-expanded', 'true');
    await user.keyboard('{ArrowRight}');
    expect(item(/^Jo Park/)).toHaveFocus();
    await user.keyboard('{ArrowLeft}');
    expect(item(/^Sam Lee/)).toHaveFocus();
    await user.keyboard('{End}');
    expect(item(/^Mia Chen/)).toHaveFocus();
    await user.keyboard('{Home}');
    expect(item(/^Alex Kim/)).toHaveFocus();
    expect(item(/^Alex Kim/)).toHaveAttribute('tabindex', '0');
    await user.keyboard('{Enter}');
    expect(onSelect).toHaveBeenCalledWith(NODES[0]);
    await user.click(item(/^Ola Berg/));
    expect(onSelect).toHaveBeenLastCalledWith(NODES[4]);
  });

  it('loads a branch before expanding it', async () => {
    function Lazy() {
      const [nodes, setNodes] = useState<OrgChartNode[]>([
        { id: 'a', name: 'Alex Kim', childCount: 2 },
      ]);
      return (
        <OrgChart
          nodes={nodes}
          zoomable={false}
          defaultExpanded={[]}
          onLoadChildren={() =>
            Promise.resolve().then(() =>
              setNodes((n) => [
                ...n,
                { id: 'b', parentId: 'a', name: 'Jo Park' },
                { id: 'c', parentId: 'a', name: 'Ola Berg' },
              ]),
            )
          }
        />
      );
    }
    render(<Lazy />);
    const user = userEvent.setup();
    await user.tab();
    expect(item(/^Alex Kim/)).toHaveAttribute('aria-expanded', 'false');
    await user.keyboard('{ArrowRight}');
    expect(await screen.findByRole('treeitem', { name: /^Jo Park/ })).toBeInTheDocument();
    expect(item(/^Alex Kim/)).toHaveAttribute('aria-expanded', 'true');
  });

  it('announces a branch that fails to load', async () => {
    render(
      <OrgChart
        nodes={[{ id: 'a', name: 'Alex Kim', childCount: 3 }]}
        defaultExpanded={[]}
        zoomable={false}
        onLoadChildren={() => Promise.reject(new Error('offline'))}
      />,
    );
    const user = userEvent.setup();
    await user.tab();
    await user.keyboard('{ArrowRight}');
    expect(await screen.findByText('Could not load Alex Kim’s team.')).toBeInTheDocument();
    expect(item(/^Alex Kim/)).toHaveAttribute('aria-expanded', 'false');
  });

  it('expands with the mouse toggle and supports controlled expansion', async () => {
    const onExpandedChange = vi.fn();
    render(<OrgChart nodes={NODES} expanded={['ceo']} onExpandedChange={onExpandedChange} />);
    expect(screen.queryByRole('treeitem', { name: /^Jo Park/ })).toBeNull();
    const toggle = item(/^Sam Lee/).parentElement!.querySelector('.mn-org-chart__toggle')!;
    expect(toggle).toHaveAttribute('aria-hidden', 'true');
    expect(toggle).toHaveTextContent('3');
    await userEvent.click(toggle);
    expect(onExpandedChange).toHaveBeenCalledWith(['ceo', 'cto']);
  });

  it('opens the path to focusId and marks it current', () => {
    render(<OrgChart nodes={NODES} defaultExpanded={[]} focusId="acc" />);
    expect(item(/^Mia Chen/)).toHaveAttribute('aria-current', 'true');
    expect(item(/^Mia Chen/)).toHaveAttribute('tabindex', '0');
  });

  it('zooms with buttons and announces the level', async () => {
    render(<OrgChart nodes={NODES} toolbar={<button type="button">Export</button>} />);
    const user = userEvent.setup();
    const zoom = screen.getByRole('group', { name: 'Zoom' });
    expect(within(zoom).getByText('100%')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Reset zoom' })).toBeDisabled();
    await user.click(screen.getByRole('button', { name: 'Zoom in' }));
    expect(within(zoom).getByText('110%')).toBeInTheDocument();
    const canvas = document.querySelector<HTMLElement>('.mn-org-chart__canvas')!;
    expect(canvas.style.getPropertyValue('--mn-org-chart-zoom')).toBe('1.1');
    await user.click(screen.getByRole('button', { name: 'Reset zoom' }));
    expect(within(zoom).getByText('100%')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Export' })).toBeInTheDocument();
  });

  it('requests moves by dropping onto a manager, onto the background, and with M', async () => {
    const onMoveRequest = vi.fn();
    render(<OrgChart nodes={NODES} onMoveRequest={onMoveRequest} />);
    expect(screen.getByText(/Drag a person onto their new manager/)).toBeInTheDocument();
    const data = new Map<string, string>();
    const dataTransfer = {
      setData: (k: string, v: string) => data.set(k, v),
      getData: (k: string) => data.get(k) ?? '',
      effectAllowed: 'move',
    };
    const jo = item(/^Jo Park/);
    expect(jo).toHaveAttribute('draggable', 'true');
    fireEvent.dragStart(jo, { dataTransfer });
    // Dropping onto one's own manager or into one's own team is refused.
    fireEvent.dragOver(item(/^Sam Lee/), { dataTransfer });
    expect(item(/^Sam Lee/)).not.toHaveAttribute('data-drop');
    fireEvent.dragOver(item(/^Riya Shah/), { dataTransfer });
    expect(item(/^Riya Shah/)).toHaveAttribute('data-drop');
    fireEvent.drop(item(/^Riya Shah/), { dataTransfer });
    expect(onMoveRequest).toHaveBeenCalledWith(NODES[3], 'cfo');

    fireEvent.dragStart(item(/^Sam Lee/), { dataTransfer });
    fireEvent.dragOver(item(/^Jo Park/), { dataTransfer });
    expect(item(/^Jo Park/)).not.toHaveAttribute('data-drop');
    fireEvent.drop(document.querySelector('.mn-org-chart__viewport')!, { dataTransfer });
    expect(onMoveRequest).toHaveBeenLastCalledWith(NODES[1], null);

    act(() => item(/^Ola Berg/).focus());
    await userEvent.keyboard('m');
    expect(onMoveRequest).toHaveBeenLastCalledWith(NODES[4], undefined);
    expect(item(/^Designer/)).not.toHaveAttribute('draggable');
  });

  it('uses renderNode and translated messages', () => {
    render(
      <OrgChart
        nodes={NODES.slice(0, 2)}
        layout="list"
        zoomable={false}
        renderNode={(n) => <strong>{n.name.toUpperCase()}</strong>}
        messages={{ label: 'Organigramm', reports: (n) => `${n} Berichte` }}
      />,
    );
    expect(screen.getByRole('tree', { name: 'Organigramm' })).toBeInTheDocument();
    expect(item(/^ALEX KIM/)).toHaveAccessibleName(/^ALEX KIM\s?, 1 Berichte$/);
    expect(screen.queryByRole('group', { name: 'Zoom' })).toBeNull();
  });

  it('renders on the server', () => {
    const html = renderToString(
      <OrgChart nodes={NODES} dottedLines={[{ from: 'acc', to: 'cto' }]} />,
    );
    expect(html).toContain('mn-org-chart');
    expect(html).toContain('role="tree"');
  });
});
