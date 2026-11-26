import { useState } from 'react';
import { describe, expect, it, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import type { EmojiPickerCategory } from '../emoji-picker/index.js';
import { ReactionBar, type ReactionBarItem } from './index.js';

const REACTIONS: ReactionBarItem[] = [
  { emoji: '👍', count: 3, reacted: true, label: 'thumbs up', title: 'You, Ana and Ben' },
  { emoji: '🎉', count: 1, label: 'party popper' },
  { emoji: '👀', count: 0 },
];

const CATEGORIES: EmojiPickerCategory[] = [
  {
    id: 'people',
    emojis: [
      { emoji: '👍', name: 'thumbs up' },
      { emoji: '❤️', name: 'red heart' },
    ],
  },
];

describe('ReactionBar', () => {
  it('names each chip with its emoji and count and hides zero counts', async () => {
    render(
      <ReactionBar reactions={REACTIONS} onReactionToggle={() => {}} categories={CATEGORIES} />,
    );
    const bar = screen.getByRole('toolbar', { name: 'Reactions' });
    expect(bar).toHaveClass('mn-reaction-bar');
    const thumbs = screen.getByRole('button', { name: 'thumbs up, 3 reactions' });
    expect(thumbs).toHaveAttribute('aria-pressed', 'true');
    expect(thumbs).toHaveAccessibleDescription('You, Ana and Ben');
    expect(screen.getByRole('button', { name: 'party popper, 1 reaction' })).toHaveAttribute(
      'aria-pressed',
      'false',
    );
    expect(screen.queryByRole('button', { name: /👀/ })).toBeNull();
    expect(screen.getByRole('button', { name: 'Add reaction' })).toBeInTheDocument();
    await expectNoA11yViolations();
  });

  it('toggles your own reaction with the next state', async () => {
    const onReactionToggle = vi.fn();
    render(<ReactionBar reactions={REACTIONS} onReactionToggle={onReactionToggle} />);
    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: 'thumbs up, 3 reactions' }));
    expect(onReactionToggle).toHaveBeenLastCalledWith('👍', false);
    await user.click(screen.getByRole('button', { name: 'party popper, 1 reaction' }));
    expect(onReactionToggle).toHaveBeenLastCalledWith('🎉', true);
  });

  it('keeps focus in the bar when taking back a reaction removes its chip', async () => {
    const user = userEvent.setup();
    function Host() {
      const [list, setList] = useState<ReactionBarItem[]>([
        { emoji: '🎉', count: 1, reacted: true, label: 'party popper' },
        { emoji: '👍', count: 3, label: 'thumbs up' },
      ]);
      return (
        <ReactionBar
          reactions={list}
          addable={false}
          onReactionToggle={(emoji, next) =>
            setList((all) =>
              all.map((r) =>
                r.emoji === emoji ? { ...r, reacted: next, count: r.count + (next ? 1 : -1) } : r,
              ),
            )
          }
        />
      );
    }
    render(<Host />);
    await user.tab();
    expect(screen.getByRole('button', { name: 'party popper, 1 reaction' })).toHaveFocus();
    await user.keyboard('{Enter}');
    expect(screen.queryByRole('button', { name: /party popper/ })).toBeNull();
    expect(screen.getByRole('button', { name: 'thumbs up, 3 reactions' })).toHaveFocus();
  });

  it('is one tab stop with arrow keys, Home and End', async () => {
    const onReactionToggle = vi.fn();
    render(<ReactionBar reactions={REACTIONS} onReactionToggle={onReactionToggle} />);
    const user = userEvent.setup();
    const thumbs = screen.getByRole('button', { name: 'thumbs up, 3 reactions' });
    const party = screen.getByRole('button', { name: 'party popper, 1 reaction' });
    const add = screen.getByRole('button', { name: 'Add reaction' });
    await user.tab();
    expect(thumbs).toHaveFocus();
    expect(party).toHaveAttribute('tabindex', '-1');
    await user.keyboard('{ArrowRight}');
    expect(party).toHaveFocus();
    await user.keyboard('{End}');
    expect(add).toHaveFocus();
    await user.keyboard('{ArrowRight}');
    expect(thumbs).toHaveFocus();
    await user.keyboard('{ArrowLeft}');
    expect(add).toHaveFocus();
    await user.keyboard('{Home}');
    expect(thumbs).toHaveFocus();
    await user.keyboard('{ArrowRight}{Enter}');
    expect(onReactionToggle).toHaveBeenCalledWith('🎉', true);
    // Tab leaves the toolbar.
    await user.tab();
    expect(document.body).toHaveFocus();
  });

  it('adds a reaction from the emoji picker and returns focus', async () => {
    const onReactionToggle = vi.fn();
    render(
      <ReactionBar
        reactions={REACTIONS}
        onReactionToggle={onReactionToggle}
        categories={CATEGORIES}
      />,
    );
    const user = userEvent.setup();
    const add = screen.getByRole('button', { name: 'Add reaction' });
    await user.click(add);
    await screen.findByRole('dialog', { name: 'Emoji picker' });
    await user.click(screen.getByRole('button', { name: 'red heart' }));
    expect(onReactionToggle).toHaveBeenCalledWith('❤️', true);
    expect(screen.queryByRole('dialog')).toBeNull();
    expect(add).toHaveFocus();
    // Picking an emoji you already reacted with does nothing.
    onReactionToggle.mockClear();
    await user.click(add);
    await screen.findByRole('dialog');
    await user.click(screen.getByRole('button', { name: 'thumbs up' }));
    expect(onReactionToggle).not.toHaveBeenCalled();
  });

  it('is read-only without onReactionToggle and renders nothing when empty', async () => {
    const { container, rerender } = render(<ReactionBar reactions={REACTIONS} />);
    const group = screen.getByRole('group', { name: 'Reactions' });
    expect(group).toHaveAttribute('data-readonly');
    expect(screen.getByRole('img', { name: 'thumbs up, 3 reactions' })).toBeInTheDocument();
    expect(screen.queryByRole('button')).toBeNull();
    await expectNoA11yViolations();
    rerender(<ReactionBar reactions={[]} />);
    expect(container).toBeEmptyDOMElement();
  });

  it('hides the add button at max or when not addable, and translates', () => {
    const { rerender } = render(
      <ReactionBar reactions={REACTIONS} onReactionToggle={() => {}} max={2} />,
    );
    expect(screen.queryByRole('button', { name: 'Add reaction' })).toBeNull();
    rerender(
      <ReactionBar
        reactions={REACTIONS}
        onReactionToggle={() => {}}
        addable={false}
        size="md"
        messages={{ label: 'Reaktionen', reaction: (l, n) => `${l}: ${n}` }}
      />,
    );
    const bar = screen.getByRole('toolbar', { name: 'Reaktionen' });
    expect(bar).toHaveAttribute('data-size', 'md');
    expect(screen.getByRole('button', { name: 'thumbs up: 3' })).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Add reaction' })).toBeNull();
  });

  it('renders on the server', () => {
    const html = renderToString(<ReactionBar reactions={REACTIONS} onReactionToggle={() => {}} />);
    expect(html).toContain('mn-reaction-bar');
    expect(html).toContain('aria-pressed="true"');
  });
});
