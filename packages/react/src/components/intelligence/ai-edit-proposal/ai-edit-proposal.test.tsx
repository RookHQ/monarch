import { describe, expect, it, vi } from 'vitest';
import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { createRef } from 'react';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import type { AiEditProposalPart } from '../ai-conversation/index.js';
import { AiEditProposal, diffWords, type AiDiffSegment, type AiEditHunk } from './index.js';

const part = (state: AiEditProposalPart['state'] = 'proposed'): AiEditProposalPart => ({
  type: 'edit-proposal',
  id: 'p1',
  target: 'doc-1',
  diff: null,
  state,
});

const hunks: AiEditHunk[] = [
  { id: 'h1', label: 'Title', before: 'Quarterly plan', after: 'Quarterly roadmap' },
  { id: 'h2', label: 'Summary', before: 'We ship in May.', after: 'We ship in June.' },
  { id: 'h3', before: 'Owner: Sam', after: 'Owner: Sam Lee' },
];

/** Rebuilds both sides of a diff, which must give back the inputs. */
function sides(segments: AiDiffSegment[]) {
  return {
    before: segments
      .filter((s) => s.type !== 'insert')
      .map((s) => s.text)
      .join(''),
    after: segments
      .filter((s) => s.type !== 'delete')
      .map((s) => s.text)
      .join(''),
  };
}

describe('diffWords', () => {
  it('diffs single words', () => {
    expect(diffWords('The quick fox', 'The slow fox')).toEqual([
      { type: 'equal', text: 'The ' },
      { type: 'delete', text: 'quick' },
      { type: 'insert', text: 'slow' },
      { type: 'equal', text: ' fox' },
    ]);
  });

  it('joins changes separated only by whitespace', () => {
    expect(diffWords('the red cat sat', 'a blue cat sat')).toEqual([
      { type: 'delete', text: 'the red' },
      { type: 'insert', text: 'a blue' },
      { type: 'equal', text: ' cat sat' },
    ]);
  });

  it('treats punctuation as its own token', () => {
    expect(diffWords('Ship in May.', 'Ship in May!')).toEqual([
      { type: 'equal', text: 'Ship in May' },
      { type: 'delete', text: '.' },
      { type: 'insert', text: '!' },
    ]);
  });

  it('handles empty, identical, added and removed text', () => {
    expect(diffWords('', '')).toEqual([]);
    expect(diffWords('same text', 'same text')).toEqual([{ type: 'equal', text: 'same text' }]);
    expect(diffWords('', 'new')).toEqual([{ type: 'insert', text: 'new' }]);
    expect(diffWords('old', '')).toEqual([{ type: 'delete', text: 'old' }]);
    expect(diffWords('a c', 'a b c')).toEqual([
      { type: 'equal', text: 'a ' },
      { type: 'insert', text: 'b ' },
      { type: 'equal', text: 'c' },
    ]);
  });

  it.each([
    ['The quick brown fox jumps', 'A quick red fox leaps over'],
    ['naïve café don’t', 'naive café do not'],
    ['line one\nline two', 'line one\n\nline 2'],
    ['x '.repeat(2000), 'y '.repeat(2000)],
  ])('reconstructs both sides of %#', (a, b) => {
    expect(sides(diffWords(a, b))).toEqual({ before: a, after: b });
  });
});

describe('AiEditProposal', () => {
  it('renders the proposal with word diffs, forwards props and the ref', async () => {
    const ref = createRef<HTMLDivElement>();
    render(
      <AiEditProposal
        ref={ref}
        part={part()}
        hunks={hunks}
        data-testid="root"
        className="extra"
        onAcceptHunk={() => {}}
        onRejectHunk={() => {}}
        onAcceptAll={() => {}}
        onRejectAll={() => {}}
      />,
    );
    const root = screen.getByTestId('root');
    expect(root).toHaveClass('mn-ai-edit-proposal', 'extra');
    expect(root).toHaveAttribute('data-state', 'proposed');
    expect(ref.current).toBe(root);
    expect(screen.getByRole('group', { name: 'Suggested edits' })).toBe(root);
    expect(root).toHaveTextContent('AI generated');
    expect(root).toHaveTextContent('3 changes');
    const title = screen.getByRole('listitem', { name: 'Title' });
    expect(title.querySelector('del')).toHaveTextContent('Deleted: plan');
    expect(title.querySelector('ins')).toHaveTextContent('Inserted: roadmap');
    expect(screen.getByRole('listitem', { name: 'Change 3 of 3' })).toBeInTheDocument();
    expect(screen.getAllByRole('button', { name: 'Accept' })[0]).toHaveAccessibleDescription(
      'Title',
    );
    await expectNoA11yViolations();
  });

  it('accepts and rejects hunks, announces it and moves focus on', async () => {
    const user = userEvent.setup();
    const onAcceptHunk = vi.fn();
    const onRejectHunk = vi.fn();
    render(
      <AiEditProposal
        part={part()}
        hunks={hunks}
        onAcceptHunk={onAcceptHunk}
        onRejectHunk={onRejectHunk}
      />,
    );
    const title = screen.getByRole('listitem', { name: 'Title' });
    await user.click(within(title).getByRole('button', { name: 'Accept' }));
    expect(onAcceptHunk).toHaveBeenCalledWith('h1');
    expect(title).toHaveAttribute('data-status', 'accepted');
    expect(title).toHaveTextContent('Accepted');
    expect(within(title).queryByRole('button')).toBeNull();
    expect(screen.getByRole('status')).toHaveTextContent('Change accepted. 2 left.');
    expect(screen.getAllByRole('button', { name: 'Accept' })[0]).toHaveFocus();
    expect(screen.getByText('2 of 3 changes left')).toBeInTheDocument();

    // Keyboard: Tab to Reject on the focused hunk, Enter.
    await user.tab();
    expect(screen.getAllByRole('button', { name: 'Reject' })[0]).toHaveFocus();
    await user.keyboard('{Enter}');
    expect(onRejectHunk).toHaveBeenCalledWith('h2');
    expect(screen.getByRole('listitem', { name: 'Summary' })).toHaveAttribute(
      'data-status',
      'rejected',
    );
    await user.click(screen.getByRole('button', { name: 'Accept' }));
    expect(onAcceptHunk).toHaveBeenLastCalledWith('h3');
    // None left: focus goes to the title.
    expect(screen.getByText('Suggested edits')).toHaveFocus();
    expect(screen.getByRole('status')).toHaveTextContent('Change accepted. No changes left.');
  });

  it('accepts or rejects all undecided hunks', async () => {
    const user = userEvent.setup();
    const onAcceptAll = vi.fn();
    const onRejectAll = vi.fn();
    const { rerender } = render(
      <AiEditProposal
        part={part()}
        hunks={[{ ...hunks[0]!, status: 'rejected' }, hunks[1]!]}
        onAcceptAll={onAcceptAll}
        onRejectAll={onRejectAll}
      />,
    );
    await user.click(screen.getByRole('button', { name: 'Accept all' }));
    expect(onAcceptAll).toHaveBeenCalledTimes(1);
    expect(screen.getByRole('listitem', { name: 'Title' })).toHaveAttribute(
      'data-status',
      'rejected',
    );
    expect(screen.getByRole('listitem', { name: 'Summary' })).toHaveAttribute(
      'data-status',
      'accepted',
    );
    expect(screen.queryByRole('button', { name: 'Accept all' })).toBeNull();
    expect(screen.getByRole('status')).toHaveTextContent('All changes accepted.');
    expect(screen.getByText('Suggested edits')).toHaveFocus();

    rerender(
      <AiEditProposal
        key="again"
        part={part()}
        hunks={hunks}
        onAcceptAll={onAcceptAll}
        onRejectAll={onRejectAll}
      />,
    );
    await user.click(screen.getByRole('button', { name: 'Reject all' }));
    expect(onRejectAll).toHaveBeenCalledTimes(1);
    for (const item of screen.getAllByRole('listitem')) {
      expect(item).toHaveAttribute('data-status', 'rejected');
    }
  });

  it('is read-only once applied, and offers Revert', async () => {
    const user = userEvent.setup();
    const onRevert = vi.fn();
    render(
      <AiEditProposal
        part={part('applied')}
        hunks={[hunks[0]!, { ...hunks[1]!, status: 'rejected' }]}
        onAcceptHunk={() => {}}
        onAcceptAll={() => {}}
        onRevert={onRevert}
      />,
    );
    expect(screen.queryByRole('button', { name: /Accept/ })).toBeNull();
    expect(screen.getByText('These edits were applied.')).toBeInTheDocument();
    expect(screen.getByText('Applied')).toBeInTheDocument();
    expect(screen.getByRole('listitem', { name: 'Title' })).toHaveAttribute(
      'data-status',
      'accepted',
    );
    expect(screen.getByRole('listitem', { name: 'Summary' })).toHaveAttribute(
      'data-status',
      'rejected',
    );
    await user.click(screen.getByRole('button', { name: 'Revert' }));
    expect(onRevert).toHaveBeenCalledTimes(1);
    await expectNoA11yViolations();
  });

  it.each(['rejected', 'reverted'] as const)('shows the %s state without actions', (state) => {
    render(
      <AiEditProposal
        part={part(state)}
        hunks={hunks}
        onAcceptHunk={() => {}}
        onRevert={() => {}}
      />,
    );
    expect(screen.queryByRole('button')).toBeNull();
    expect(screen.getByText(`These edits were ${state}.`)).toBeInTheDocument();
  });

  it('shows unchanged hunks and localises strings', () => {
    render(
      <AiEditProposal
        part={part()}
        title="Edits to the brief"
        hunks={[{ id: 'x', before: 'Same', after: 'Same' }]}
        messages={{ unchanged: 'Identical', states: { proposed: 'Pending' } }}
      />,
    );
    expect(screen.getByRole('group', { name: 'Edits to the brief' })).toBeInTheDocument();
    expect(screen.getByText('Pending')).toBeInTheDocument();
    expect(screen.getByRole('listitem')).toHaveTextContent('Same (Identical)');
  });

  it('renders on the server', () => {
    const html = renderToString(
      <AiEditProposal part={part()} hunks={hunks} onAcceptHunk={() => {}} />,
    );
    expect(html).toContain('mn-ai-edit-proposal');
    expect(html).toContain('<del');
  });
});
