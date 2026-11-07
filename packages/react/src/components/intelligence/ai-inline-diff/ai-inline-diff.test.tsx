import { describe, expect, it, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { createRef, useState } from 'react';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import type { AiDiffSegment } from '../ai-edit-proposal/index.js';
import { AiInlineDiff, aiInlineDiffChanges, resolveAiInlineDiff } from './index.js';

const segments: AiDiffSegment[] = [
  { type: 'equal', text: 'We ship in ' },
  { type: 'delete', text: 'May' },
  { type: 'insert', text: 'June' },
  { type: 'equal', text: ' with ' },
  { type: 'insert', text: 'two ', id: 'n' },
  { type: 'equal', text: 'teams.' },
  { type: 'delete', text: ' Maybe.', id: 'm' },
];

function Controlled({
  onAccept,
  onReject,
}: {
  onAccept?: (id: string) => void;
  onReject?: (id: string) => void;
}) {
  const [segs, setSegs] = useState(segments);
  return (
    <>
      <AiInlineDiff
        segments={segs}
        onAccept={(id) => {
          onAccept?.(id);
          setSegs((s) => resolveAiInlineDiff(s, id, 'accept'));
        }}
        onReject={(id) => {
          onReject?.(id);
          setSegs((s) => resolveAiInlineDiff(s, id, 'reject'));
        }}
      />
      <div data-testid="text">
        {segs
          .filter((s) => s.type === 'equal')
          .map((s) => s.text)
          .join('')}
      </div>
    </>
  );
}

describe('aiInlineDiffChanges / resolveAiInlineDiff', () => {
  it('groups adjacent removed and added runs into changes', () => {
    expect(aiInlineDiffChanges(segments)).toEqual([
      { id: '1', segmentIndexes: [1, 2], deleted: 'May', inserted: 'June' },
      { id: 'n', segmentIndexes: [4], deleted: '', inserted: 'two ' },
      { id: 'm', segmentIndexes: [6], deleted: ' Maybe.', inserted: '' },
    ]);
  });

  it('splits a run when ids differ', () => {
    const changes = aiInlineDiffChanges([
      { type: 'delete', text: 'a', id: 'x' },
      { type: 'insert', text: 'b', id: 'y' },
    ]);
    expect(changes.map((c) => c.id)).toEqual(['x', 'y']);
  });

  it('accepts and rejects into merged text', () => {
    expect(resolveAiInlineDiff(segments, '1', 'accept').slice(0, 2)).toEqual([
      { type: 'equal', text: 'We ship in June with ' },
      { type: 'insert', text: 'two ', id: 'n' },
    ]);
    expect(resolveAiInlineDiff(segments, '1', 'reject')[0]).toEqual({
      type: 'equal',
      text: 'We ship in May with ',
    });
    expect(resolveAiInlineDiff(segments, 'missing', 'accept')).toBe(segments);
  });
});

describe('AiInlineDiff', () => {
  it('renders marks with spoken prefixes, forwards props and the ref', async () => {
    const ref = createRef<HTMLDivElement>();
    render(
      <AiInlineDiff
        ref={ref}
        segments={segments}
        data-testid="root"
        className="extra"
        onAccept={() => {}}
        onReject={() => {}}
      />,
    );
    const root = screen.getByTestId('root');
    expect(root).toHaveClass('mn-ai-inline-diff', 'extra');
    expect(ref.current).toBe(root);
    expect(screen.getByRole('group', { name: 'AI suggested changes' })).toBe(root);
    expect(root.querySelectorAll('del')).toHaveLength(2);
    expect(root.querySelectorAll('ins')).toHaveLength(2);
    expect(root.querySelector('del')).toHaveTextContent('Deleted: May');
    expect(root.querySelector('ins')).toHaveTextContent('Inserted: June');
    expect(screen.getByText('Change 1 of 3')).toBeInTheDocument();
    // Only the active change has controls.
    expect(screen.getByRole('button', { name: 'Accept change 1 of 3' })).toBeInTheDocument();
    expect(screen.getAllByRole('button', { name: /^Accept/ })).toHaveLength(1);
    await expectNoA11yViolations();
  });

  it('moves between changes with the buttons and arrow keys', async () => {
    const user = userEvent.setup();
    render(<AiInlineDiff segments={segments} onAccept={() => {}} />);
    const changes = () => document.querySelectorAll<HTMLElement>('.mn-ai-inline-diff__change');
    await user.click(screen.getByRole('button', { name: 'Next change' }));
    expect(changes()[1]).toHaveFocus();
    expect(changes()[1]).toHaveAttribute('data-active');
    expect(changes()[1]).toHaveAttribute('tabindex', '0');
    expect(changes()[0]).toHaveAttribute('tabindex', '-1');
    expect(screen.getByText('Change 2 of 3')).toBeInTheDocument();

    await user.keyboard('{ArrowDown}');
    expect(changes()[2]).toHaveFocus();
    await user.keyboard('{ArrowRight}');
    expect(changes()[0]).toHaveFocus(); // wraps
    await user.keyboard('{ArrowLeft}');
    expect(changes()[2]).toHaveFocus();
    await user.keyboard('{Home}');
    expect(changes()[0]).toHaveFocus();
    await user.keyboard('{End}');
    expect(changes()[2]).toHaveFocus();
    await user.keyboard('{ArrowUp}');
    expect(changes()[1]).toHaveFocus();
    await user.click(screen.getByRole('button', { name: 'Previous change' }));
    expect(changes()[0]).toHaveFocus();

    // Tab from the active change reaches its Accept button.
    await user.tab();
    expect(screen.getByRole('button', { name: 'Accept change 1 of 3' })).toHaveFocus();
  });

  it('clicking a change makes it active', async () => {
    const user = userEvent.setup();
    render(<AiInlineDiff segments={segments} onReject={() => {}} />);
    await user.click(screen.getByText('two'));
    expect(screen.getByRole('button', { name: 'Reject change 2 of 3' })).toBeInTheDocument();
  });

  it('accepts and rejects, announces it and keeps focus on the next change', async () => {
    const user = userEvent.setup();
    const onAccept = vi.fn();
    const onReject = vi.fn();
    render(<Controlled onAccept={onAccept} onReject={onReject} />);
    await user.click(screen.getByRole('button', { name: 'Accept change 1 of 3' }));
    expect(onAccept).toHaveBeenCalledWith('1');
    expect(screen.getByTestId('text')).toHaveTextContent('We ship in June with teams.');
    expect(screen.getByRole('status')).toHaveTextContent('Change accepted. 2 left.');
    const active = document.querySelector('.mn-ai-inline-diff__change[data-active]');
    expect(active).toHaveAttribute('data-change-id', 'n');
    expect(active).toHaveFocus();

    await user.tab();
    await user.tab();
    expect(screen.getByRole('button', { name: 'Reject change 1 of 2' })).toHaveFocus();
    await user.keyboard('{Enter}');
    expect(onReject).toHaveBeenCalledWith('n');
    await user.click(screen.getByRole('button', { name: 'Reject change 1 of 1' }));
    expect(onReject).toHaveBeenLastCalledWith('m');
    expect(screen.getByTestId('text')).toHaveTextContent('We ship in June with teams. Maybe.');
    expect(screen.getByText('No suggested changes')).toBeInTheDocument();
    expect(document.querySelector('.mn-ai-inline-diff__content')).toHaveFocus();
    expect(screen.getByRole('status')).toHaveTextContent('Change rejected. No changes left.');
  });

  it('is read-only without handlers, and can hide navigation', async () => {
    render(<AiInlineDiff segments={segments} showNavigation={false} />);
    expect(screen.queryByRole('button')).toBeNull();
    await expectNoA11yViolations();
  });

  it('flips left and right arrows in right-to-left text', async () => {
    const user = userEvent.setup();
    render(
      <div dir="rtl" style={{ direction: 'rtl' }}>
        <AiInlineDiff segments={segments} />
      </div>,
    );
    const changes = document.querySelectorAll<HTMLElement>('.mn-ai-inline-diff__change');
    changes[0]!.focus();
    await user.keyboard('{ArrowLeft}');
    expect(changes[1]).toHaveFocus();
  });

  it('localises strings', () => {
    render(
      <AiInlineDiff
        segments={segments}
        messages={{ label: 'Suggestions', position: (i, n) => `${i}/${n}` }}
      />,
    );
    expect(screen.getByRole('group', { name: 'Suggestions' })).toBeInTheDocument();
    expect(screen.getByText('1/3')).toBeInTheDocument();
  });

  it('renders on the server', () => {
    const html = renderToString(<AiInlineDiff segments={segments} onAccept={() => {}} />);
    expect(html).toContain('mn-ai-inline-diff');
    expect(html).toContain('<ins');
  });
});
