import { describe, expect, it, vi } from 'vitest';
import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { createRef } from 'react';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import {
  AiRelatedEntities,
  sortAiRelatedEntities,
  type AiEntityRelation,
  type AiRelatedEntity,
} from './index.js';

const ENTITIES: AiRelatedEntity[] = [
  { id: 'p1', label: 'Jordan Lee', kind: 'person', weight: 0.6 },
  { id: 'o1', label: 'Northwind', kind: 'org', weight: 0.9 },
  { id: 'd1', label: 'Q3 plan', kind: 'document' },
  { id: 'x1', label: 'Harbor site', kind: 'facility', weight: 0.3 },
];
const RELATIONS: AiEntityRelation[] = [
  { from: 'p1', to: 'o1', label: 'works at' },
  { from: 'd1', to: 'o1' },
];

describe('sortAiRelatedEntities', () => {
  it('sorts by weight, keeping unweighted entities last in order', () => {
    expect(sortAiRelatedEntities(ENTITIES).map((e) => e.id)).toEqual(['o1', 'p1', 'x1', 'd1']);
  });
});

describe('AiRelatedEntities', () => {
  it('renders a ranked, named list with kinds, weights and relations', async () => {
    const ref = createRef<HTMLDivElement>();
    render(
      <AiRelatedEntities
        ref={ref}
        data-testid="root"
        className="extra"
        entities={ENTITIES}
        relations={RELATIONS}
      />,
    );
    const root = screen.getByTestId('root');
    expect(root).toHaveClass('mn-ai-related-entities', 'extra');
    expect(root).toHaveAttribute('data-variant', 'list');
    expect(ref.current).toBe(root);
    const list = screen.getByRole('list', { name: 'Related entities' });
    const items = within(list).getAllByRole('listitem');
    expect(
      items.map((i) => i.querySelector('.mn-ai-related-entities__label')?.textContent),
    ).toEqual(['Northwind', 'Jordan Lee', 'Harbor site', 'Q3 plan']);
    expect(items[0]).toHaveTextContent('Organization');
    expect(items[0]).toHaveTextContent('Relevance 90%');
    expect(items[0]).toHaveTextContent('Related to: works at Jordan Lee, Q3 plan');
    expect(items[1]).toHaveTextContent('Related to: works at Northwind');
    // Unknown kinds are shown as given.
    expect(items[2]).toHaveTextContent('facility');
    expect(screen.queryByRole('button')).toBeNull();
    await expectNoA11yViolations();
  });

  it('makes list entries buttons that call onSelect by click and keyboard', async () => {
    const user = userEvent.setup();
    const onSelect = vi.fn();
    render(<AiRelatedEntities entities={ENTITIES} onSelect={onSelect} />);
    const buttons = screen.getAllByRole('button');
    expect(buttons).toHaveLength(4);
    await user.click(buttons[1]!);
    expect(onSelect).toHaveBeenLastCalledWith(ENTITIES[0]);
    await user.tab({ shift: true });
    expect(buttons[0]).toHaveFocus();
    await user.keyboard('{Enter}');
    expect(onSelect).toHaveBeenLastCalledWith(ENTITIES[1]);
    await expectNoA11yViolations();
  });

  it('draws a radial graph with named nodes and edges', async () => {
    const { container } = render(
      <AiRelatedEntities
        data-testid="root"
        variant="graph"
        entities={ENTITIES}
        relations={RELATIONS}
      />,
    );
    expect(screen.getByTestId('root')).toHaveAttribute('data-variant', 'graph');
    const graph = screen.getByRole('group', { name: 'Related entities' });
    const nodes = within(graph).getAllByRole('img');
    expect(nodes).toHaveLength(4);
    // The highest weight is in the centre.
    expect(nodes[0]).toHaveAttribute('data-center');
    expect(nodes[0]).toHaveAccessibleName(
      'Northwind. Organization. Relevance 90%. Related to: works at Jordan Lee, Q3 plan',
    );
    expect(nodes[0]!.querySelector('circle')).toHaveAttribute('cx', '160');
    expect(container.querySelectorAll('.mn-ai-related-entities__edge')).toHaveLength(2);
    await expectNoA11yViolations();
  });

  it('honours centerId and selects graph nodes with Enter and Space', async () => {
    const user = userEvent.setup();
    const onSelect = vi.fn();
    render(
      <AiRelatedEntities
        variant="graph"
        centerId="d1"
        entities={ENTITIES}
        relations={RELATIONS}
        onSelect={onSelect}
      />,
    );
    const nodes = screen.getAllByRole('button');
    expect(nodes[0]).toHaveAccessibleName(expect.stringContaining('Q3 plan'));
    expect(nodes[0]).toHaveAttribute('data-center');
    await user.tab();
    expect(nodes[0]).toHaveFocus();
    await user.keyboard('{Enter}');
    expect(onSelect).toHaveBeenLastCalledWith(ENTITIES[2]);
    await user.tab();
    await user.keyboard(' ');
    expect(onSelect).toHaveBeenLastCalledWith(ENTITIES[1]);
    await user.click(nodes[2]!);
    expect(onSelect).toHaveBeenLastCalledWith(ENTITIES[0]);
  });

  it('falls back to the list above maxGraphNodes', () => {
    render(
      <AiRelatedEntities
        data-testid="root"
        variant="graph"
        maxGraphNodes={3}
        entities={ENTITIES}
      />,
    );
    expect(screen.getByTestId('root')).toHaveAttribute('data-variant', 'list');
    expect(screen.getByRole('list')).toBeInTheDocument();
  });

  it('shows an empty message and localises strings', () => {
    const { rerender } = render(<AiRelatedEntities entities={[]} />);
    expect(screen.getByText('No related entities')).toBeInTheDocument();
    rerender(
      <AiRelatedEntities
        entities={ENTITIES.slice(0, 2)}
        relations={RELATIONS}
        messages={{
          label: 'Linked',
          kind: (k) => k.toUpperCase(),
          relatedTo: 'Linked to',
          weight: (w) => `score ${w}`,
        }}
      />,
    );
    const list = screen.getByRole('list', { name: 'Linked' });
    expect(list).toHaveTextContent('ORG');
    expect(list).toHaveTextContent('score 0.9');
    expect(list).toHaveTextContent('Linked to: works at Jordan Lee');
  });

  it('renders on the server', () => {
    expect(renderToString(<AiRelatedEntities entities={ENTITIES} />)).toContain(
      'mn-ai-related-entities__list',
    );
    expect(
      renderToString(
        <AiRelatedEntities variant="graph" entities={ENTITIES} relations={RELATIONS} />,
      ),
    ).toContain('<svg');
  });
});
