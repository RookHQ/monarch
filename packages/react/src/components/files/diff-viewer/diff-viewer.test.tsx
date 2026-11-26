import { renderToString } from 'react-dom/server';
import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { DiffViewer, diffTextLines, diffStats, diffTextWords } from './index.js';

const BEFORE = [
  'Title',
  'one',
  'two',
  'three',
  'four',
  'five',
  'six',
  'seven',
  'The quick fox',
  'end',
].join('\n');
const AFTER = [
  'Title',
  'one',
  'two',
  'three',
  'four',
  'five',
  'six',
  'seven',
  'The slow fox',
  'end',
  'extra',
].join('\n');

describe('diff helpers', () => {
  it('numbers lines on both sides and counts changes', () => {
    const lines = diffTextLines('a\nb\nc', 'a\nc\nd');
    expect(lines).toEqual([
      { kind: 'same', text: 'a', oldNumber: 1, newNumber: 1 },
      { kind: 'removed', text: 'b', oldNumber: 2 },
      { kind: 'same', text: 'c', oldNumber: 3, newNumber: 2 },
      { kind: 'added', text: 'd', newNumber: 3 },
    ]);
    expect(diffStats(lines)).toEqual({ added: 1, removed: 1 });
    expect(diffTextLines(['block one'], ['block one', 'block two'])).toHaveLength(2);
  });

  it('diffs words inside a line', () => {
    expect(diffTextWords('The quick fox', 'The slow fox')).toEqual([
      { kind: 'same', text: 'The ' },
      { kind: 'removed', text: 'quick' },
      { kind: 'added', text: 'slow' },
      { kind: 'same', text: ' fox' },
    ]);
  });
});

describe('DiffViewer', () => {
  it('shows a unified diff with ins/del, spoken change labels, word highlights and stats', async () => {
    render(
      <DiffViewer before={BEFORE} after={AFTER} beforeLabel="Version 1" afterLabel="Version 2" />,
    );
    const table = screen.getByRole('table', { name: 'Changes from Version 1 to Version 2' });
    expect(screen.getByText('2 added, 1 removed')).toBeInTheDocument();
    const del = table.querySelector('del.mn-diff-viewer__change');
    expect(del).toHaveTextContent('Removed: The quick fox');
    expect(table.querySelector('del.mn-diff-viewer__word')).toHaveTextContent('quick');
    expect(table.querySelector('ins.mn-diff-viewer__word')).toHaveTextContent('slow');
    expect(within(table).getByText('extra').closest('tr')).toHaveAttribute('data-kind', 'added');
    await expectNoA11yViolations();
  });

  it('folds long unchanged runs and expands them on request', async () => {
    const user = userEvent.setup();
    render(<DiffViewer before={BEFORE} after={AFTER} context={1} />);
    expect(screen.queryByText('three')).toBeNull();
    const expand = screen.getByRole('button', { name: 'Show 7 unchanged lines' });
    await user.click(expand);
    expect(screen.getByText('three')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /unchanged/ })).toBeNull();
  });

  it('switches to side by side with a named radio group and pairs changed lines', async () => {
    const user = userEvent.setup();
    const onModeChange = vi.fn();
    const { container } = render(
      <DiffViewer before={'a\nold line'} after={'a\nnew line'} onModeChange={onModeChange} />,
    );
    const group = screen.getByRole('radiogroup', { name: 'Layout' });
    within(group).getByRole('radio', { name: 'Unified' }).focus();
    await user.keyboard('{ArrowRight}');
    expect(onModeChange).toHaveBeenCalledWith('split');
    const root = container.firstElementChild as HTMLElement;
    expect(root).toHaveAttribute('data-mode', 'split');
    expect(screen.getByRole('columnheader', { name: 'Before' })).toBeInTheDocument();
    const row = container.querySelector('tr[data-kind="change"]') as HTMLElement;
    expect(row.querySelector('[data-side="old"]')).toHaveTextContent('Removed: old line');
    expect(row.querySelector('[data-side="new"]')).toHaveTextContent('Added: new line');
    await expectNoA11yViolations();
  });

  it('starts over (folded gaps, position) when the compared texts change', async () => {
    const user = userEvent.setup();
    const { rerender } = render(<DiffViewer before={BEFORE} after={AFTER} context={0} />);
    await user.click(screen.getByRole('button', { name: 'Show 8 unchanged lines' }));
    expect(screen.getByText('three')).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Next change' }));
    await user.click(screen.getByRole('button', { name: 'Next change' }));
    expect(screen.getByRole('status')).toHaveTextContent('Change 2 of 2');

    // The same lines in new arrays keep what the reader expanded.
    rerender(<DiffViewer before={BEFORE.split('\n')} after={AFTER.split('\n')} context={0} />);
    expect(screen.getByText('three')).toBeInTheDocument();

    // Another pair of texts: its first gap is folded, and no stale "Change 2 of 1".
    rerender(<DiffViewer before={'a\nb\nc\nd\ne\nf'} after={'a\nb\nc\nd\ne\nF'} context={0} />);
    expect(screen.queryByText('c')).toBeNull();
    expect(screen.getByRole('button', { name: 'Show 5 unchanged lines' })).toBeInTheDocument();
    expect(screen.getByRole('status')).toBeEmptyDOMElement();
  });

  it('moves between changes and announces the position', async () => {
    const user = userEvent.setup();
    render(<DiffViewer before={BEFORE} after={AFTER} context={0} />);
    await user.click(screen.getByRole('button', { name: 'Next change' }));
    expect(screen.getByRole('status')).toHaveTextContent('Change 1 of 2');
    const first = document.querySelector('[data-hunk="0"]');
    expect(first).toHaveFocus();
    await user.click(screen.getByRole('button', { name: 'Next change' }));
    expect(document.querySelector('[data-hunk="1"]')).toHaveFocus();
    await user.click(screen.getByRole('button', { name: 'Previous change' }));
    expect(screen.getByRole('status')).toHaveTextContent('Change 1 of 2');
  });

  it('says when there are no changes, and renders on the server', () => {
    render(<DiffViewer before="same" after="same" />);
    expect(screen.getByText('No changes.')).toBeInTheDocument();
    const html = renderToString(<DiffViewer before="a" after="b" mode="split" />);
    expect(html).toContain('data-mode="split"');
    expect(html).toContain('<del');
  });
});
