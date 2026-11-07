import { describe, expect, it, vi } from 'vitest';
import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { DocumentRelations, type DocumentRelation, type DocumentRelationTarget } from './index.js';

const relations: DocumentRelation[] = [
  { id: 'r1', type: 'parent-of', target: { id: 'd2', title: 'Hiring plan', href: '/docs/d2' } },
  {
    id: 'r2',
    type: 'parent-of',
    direction: 'incoming',
    target: { id: 'd3', title: 'Company handbook' },
  },
  { id: 'r3', type: 'supersedes', target: { id: 'd4', title: 'Old travel policy' } },
];
const candidates: DocumentRelationTarget[] = [
  { id: 'd2', title: 'Hiring plan' },
  { id: 'd5', title: 'Budget 2027', description: 'Finance' },
  { id: 'd6', title: 'Brand guidelines', description: 'Marketing' },
];

describe('DocumentRelations', () => {
  it('reads each relation from this document’s side, with links', async () => {
    render(<DocumentRelations relations={relations} onRemove={() => {}} />);
    const group = screen.getByRole('group', { name: 'Relations' });
    const items = within(group).getAllByRole('listitem');
    expect(items[0]).toHaveTextContent('Parent ofHiring plan');
    expect(items[1]).toHaveTextContent('Child ofCompany handbook');
    expect(items[2]).toHaveTextContent('SupersedesOld travel policy');
    expect(screen.getByRole('link', { name: 'Hiring plan' })).toHaveAttribute('href', '/docs/d2');
    expect(
      screen.getByRole('button', { name: 'Remove relation to Old travel policy' }),
    ).toBeInTheDocument();
    await expectNoA11yViolations();
  });

  it('uses renderLink for routing', () => {
    render(
      <DocumentRelations
        relations={relations.slice(0, 1)}
        renderLink={(target, children) => <a href={`#/d/${target.id}`}>{children}</a>}
      />,
    );
    expect(screen.getByRole('link', { name: 'Hiring plan' })).toHaveAttribute('href', '#/d/d2');
  });

  it('adds a relation by type and search, then announces it and returns focus', async () => {
    const onAdd = vi.fn(async () => {});
    render(<DocumentRelations relations={relations} candidates={candidates} onAdd={onAdd} />);
    const user = userEvent.setup();
    const trigger = screen.getByRole('button', { name: 'Add relation' });
    await user.click(trigger);
    const dialog = await screen.findByRole('dialog', { name: 'Add a relation' });
    expect(within(dialog).queryByRole('button', { name: /Hiring plan/ })).not.toBeInTheDocument();
    await user.selectOptions(
      within(dialog).getByRole('combobox', { name: 'Relation' }),
      'implements',
    );
    await user.type(within(dialog).getByRole('searchbox', { name: 'Document' }), 'budg');
    const results = within(dialog).getByRole('list', { name: 'Matching documents' });
    expect(within(results).getAllByRole('button')).toHaveLength(1);
    await user.keyboard('{ArrowDown}');
    expect(within(results).getByRole('button', { name: /Budget 2027/ })).toHaveFocus();
    await expectNoA11yViolations(dialog);
    await user.keyboard('{Enter}');
    expect(onAdd).toHaveBeenCalledWith('d5', 'implements');
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
    expect(screen.getByText('Relation to Budget 2027 added')).toBeInTheDocument();
    expect(trigger).toHaveFocus();
  });

  it('keeps the dialog open with an error when adding fails', async () => {
    render(
      <DocumentRelations
        relations={[]}
        candidates={candidates}
        onAdd={() => Promise.reject(new Error('You can’t link to that document.'))}
      />,
    );
    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: 'Add relation' }));
    await user.click(await screen.findByRole('button', { name: /Brand guidelines/ }));
    expect(await screen.findByRole('alert')).toHaveTextContent('You can’t link to that document.');
    expect(screen.getByRole('dialog')).toBeInTheDocument();
  });

  it('searches on the server with onSearch and shows when nothing matches', async () => {
    const onSearch = vi.fn();
    render(
      <DocumentRelations relations={[]} candidates={[]} onSearch={onSearch} onAdd={() => {}} />,
    );
    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: 'Add relation' }));
    await user.type(await screen.findByRole('searchbox'), 'q');
    expect(onSearch).toHaveBeenCalledWith('q');
    expect(screen.getByRole('status')).toHaveTextContent('No matching documents.');
  });

  it('removes a relation and moves focus to the next one', async () => {
    const onRemove = vi.fn(async () => {});
    const { rerender } = render(<DocumentRelations relations={relations} onRemove={onRemove} />);
    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: 'Remove relation to Company handbook' }));
    expect(onRemove).toHaveBeenCalledWith('r2');
    rerender(<DocumentRelations relations={[relations[0]!, relations[2]!]} onRemove={onRemove} />);
    await waitFor(() =>
      expect(
        screen.getByRole('button', { name: 'Remove relation to Old travel policy' }),
      ).toHaveFocus(),
    );
    expect(screen.getByText('Relation to Company handbook removed')).toBeInTheDocument();
  });

  it('shows an empty message, custom types and translations', () => {
    const { rerender } = render(
      <DocumentRelations relations={[]} showEmpty messages={{ none: 'Keine.' }} />,
    );
    expect(screen.getByText('Keine.')).toBeInTheDocument();
    rerender(
      <DocumentRelations
        relations={[
          { id: 'x', type: 'blocks', direction: 'incoming', target: { id: 'a', title: 'Launch' } },
        ]}
        types={[
          { value: 'blocks', label: 'Blocks', inverse: 'blocked-by' },
          { value: 'blocked-by', label: 'Blocked by', inverse: 'blocks' },
        ]}
      />,
    );
    expect(screen.getByRole('listitem')).toHaveTextContent('Blocked byLaunch');
  });

  it('renders on the server', () => {
    const html = renderToString(<DocumentRelations relations={relations} onAdd={() => {}} />);
    expect(html).toContain('mn-document-relations');
  });
});
