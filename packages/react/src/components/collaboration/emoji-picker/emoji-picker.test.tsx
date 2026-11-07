import { describe, expect, it, vi } from 'vitest';
import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import {
  EmojiPicker,
  EmojiPickerPopover,
  applySkinTone,
  defaultEmojiCategories,
  pushRecentEmoji,
  searchEmoji,
  type EmojiPickerCategory,
} from './index.js';

const small: EmojiPickerCategory[] = [
  {
    id: 'smileys',
    emojis: [
      { emoji: '😀', name: 'grinning face', keywords: ['happy'] },
      { emoji: '😂', name: 'face with tears of joy', keywords: ['laugh'] },
      { emoji: '🙂', name: 'slightly smiling face' },
      { emoji: '😉', name: 'winking face' },
      { emoji: '😊', name: 'smiling face with smiling eyes' },
    ],
  },
  {
    id: 'people',
    emojis: [
      { emoji: '👍', name: 'thumbs up', keywords: ['like'], skinTones: true },
      { emoji: '👋', name: 'waving hand', keywords: ['hello'], skinTones: true },
    ],
  },
];

describe('helpers', () => {
  it('applies skin tones after the first code point', () => {
    expect(applySkinTone('👍', 'none')).toBe('👍');
    expect(applySkinTone('👍', 'medium')).toBe('👍\u{1F3FD}');
    expect(applySkinTone('✌️', 'dark')).toBe('✌\u{1F3FF}');
  });

  it('searches names and keywords by word prefix, name matches first', () => {
    expect(searchEmoji(small, 'like').map((e) => e.emoji)).toEqual(['👍']);
    expect(searchEmoji(small, 'smil').map((e) => e.emoji)).toEqual(['😊', '🙂']);
    expect(searchEmoji(small, '')).toEqual([]);
    expect(searchEmoji(defaultEmojiCategories, 'thumbs up')[0]?.emoji).toBe('👍');
  });

  it('keeps recents unique, newest first and bounded', () => {
    expect(pushRecentEmoji(['🎉', '👍'], '👍')).toEqual(['👍', '🎉']);
    expect(pushRecentEmoji(['a', 'b', 'c'], 'd', 3)).toEqual(['d', 'a', 'b']);
  });

  it('ships a built-in set with unique emoji per category and no empty names', () => {
    for (const c of defaultEmojiCategories) {
      expect(c.emojis.length).toBeGreaterThan(20);
      expect(new Set(c.emojis.map((e) => e.emoji)).size).toBe(c.emojis.length);
      for (const e of c.emojis) expect(e.name.length).toBeGreaterThan(0);
    }
  });
});

describe('EmojiPicker', () => {
  it('renders a named group with search, categories and labelled emoji', async () => {
    render(<EmojiPicker data-testid="p" categories={small} onSelect={() => {}} />);
    const picker = screen.getByRole('group', { name: 'Emoji picker' });
    expect(picker).toHaveClass('mn-emoji-picker');
    expect(screen.getByRole('searchbox', { name: 'Search emoji' })).toBeInTheDocument();
    expect(screen.getByRole('toolbar', { name: 'Emoji categories' })).toBeInTheDocument();
    const smileys = screen.getByRole('group', { name: 'Smileys & emotion' });
    expect(within(smileys).getAllByRole('button')).toHaveLength(5);
    expect(screen.getByRole('button', { name: 'thumbs up' })).toBeInTheDocument();
    await expectNoA11yViolations();
  });

  it('has one tab stop in the grid and moves with arrow keys', async () => {
    const user = userEvent.setup();
    render(<EmojiPicker categories={small} columns={3} onSelect={() => {}} />);
    const search = screen.getByRole('searchbox');
    await user.click(search);
    await user.keyboard('{ArrowDown}');
    expect(screen.getByRole('button', { name: 'grinning face' })).toHaveFocus();
    await user.keyboard('{ArrowRight}');
    expect(screen.getByRole('button', { name: 'face with tears of joy' })).toHaveFocus();
    await user.keyboard('{ArrowDown}');
    // Row 2 has 2 items in a 3-column grid: 😉 sits under 😂.
    expect(screen.getByRole('button', { name: 'smiling face with smiling eyes' })).toHaveFocus();
    await user.keyboard('{ArrowDown}');
    // Into the next category, same column clamped.
    expect(screen.getByRole('button', { name: 'waving hand' })).toHaveFocus();
    await user.keyboard('{ArrowUp}');
    expect(screen.getByRole('button', { name: 'smiling face with smiling eyes' })).toHaveFocus();
    await user.keyboard('{Control>}{End}{/Control}');
    expect(screen.getByRole('button', { name: 'waving hand' })).toHaveFocus();
    await user.keyboard('{Control>}{Home}{/Control}');
    expect(screen.getByRole('button', { name: 'grinning face' })).toHaveFocus();
    await user.keyboard('{ArrowUp}');
    expect(search).toHaveFocus();
    const grinning = screen.getByRole('button', { name: 'grinning face' });
    expect(grinning).toHaveAttribute('tabindex', '0');
    expect(screen.getByRole('button', { name: 'winking face' })).toHaveAttribute('tabindex', '-1');
  });

  it('picks with Enter, Space and click', async () => {
    const user = userEvent.setup();
    const onSelect = vi.fn();
    render(<EmojiPicker categories={small} onSelect={onSelect} />);
    await user.click(screen.getByRole('button', { name: 'winking face' }));
    expect(onSelect).toHaveBeenLastCalledWith(
      '😉',
      expect.objectContaining({ name: 'winking face' }),
    );
    await user.keyboard('{ArrowRight}{Enter}');
    expect(onSelect).toHaveBeenLastCalledWith('😊', expect.anything());
    await user.keyboard(' ');
    expect(onSelect).toHaveBeenCalledTimes(3);
  });

  it('filters by search, announces the count and picks the first result with Enter', async () => {
    const user = userEvent.setup();
    const onSelect = vi.fn();
    render(<EmojiPicker categories={small} onSelect={onSelect} />);
    await user.type(screen.getByRole('searchbox'), 'hello');
    expect(screen.queryByRole('toolbar')).not.toBeInTheDocument();
    const results = screen.getByRole('group', { name: 'Search results' });
    expect(within(results).getAllByRole('button')).toHaveLength(1);
    expect(screen.getByRole('status')).toHaveTextContent('1 emoji found');
    await user.keyboard('{Enter}');
    expect(onSelect).toHaveBeenCalledWith('👋', expect.anything());
    await user.clear(screen.getByRole('searchbox'));
    await user.type(screen.getByRole('searchbox'), 'zzzz');
    expect(screen.getByText('No emoji found')).toBeInTheDocument();
    expect(screen.getByRole('status')).toHaveTextContent('0 emoji found');
  });

  it('shows recents first and names them from the dataset', () => {
    render(<EmojiPicker categories={small} recents={['👍\u{1F3FD}', '🦄']} onSelect={() => {}} />);
    const recent = screen.getByRole('group', { name: 'Recently used' });
    const buttons = within(recent).getAllByRole('button');
    expect(buttons.map((b) => b.getAttribute('aria-label'))).toEqual(['thumbs up', '🦄']);
    expect(buttons[0]).toHaveTextContent('👍\u{1F3FD}');
  });

  it('chooses a skin tone from a radio group and applies it', async () => {
    const user = userEvent.setup();
    const onSelect = vi.fn();
    const onSkinToneChange = vi.fn();
    render(
      <EmojiPicker categories={small} onSelect={onSelect} onSkinToneChange={onSkinToneChange} />,
    );
    const toggle = screen.getByRole('button', { name: 'Skin tone: Default' });
    expect(toggle).toHaveAttribute('aria-expanded', 'false');
    await user.click(toggle);
    const group = screen.getByRole('radiogroup', { name: 'Skin tone' });
    expect(within(group).getByRole('radio', { name: 'Default' })).toHaveAttribute(
      'aria-checked',
      'true',
    );
    within(group).getByRole('radio', { name: 'Default' }).focus();
    await user.keyboard('{ArrowRight}');
    expect(onSkinToneChange).toHaveBeenLastCalledWith('light');
    expect(within(group).getByRole('radio', { name: 'Light' })).toHaveFocus();
    await user.click(within(group).getByRole('radio', { name: 'Medium' }));
    expect(screen.queryByRole('radiogroup')).not.toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Skin tone: Medium' })).toHaveFocus();
    await user.click(screen.getByRole('button', { name: 'thumbs up' }));
    expect(onSelect).toHaveBeenLastCalledWith('👍\u{1F3FD}', expect.anything());
    await user.click(screen.getByRole('button', { name: 'grinning face' }));
    expect(onSelect).toHaveBeenLastCalledWith('😀', expect.anything());
  });

  it('closes the skin tones with Escape without leaving the picker', async () => {
    const user = userEvent.setup();
    render(<EmojiPicker categories={small} onSelect={() => {}} />);
    await user.click(screen.getByRole('button', { name: /Skin tone/ }));
    within(screen.getByRole('radiogroup')).getByRole('radio', { name: 'Default' }).focus();
    await user.keyboard('{Escape}');
    expect(screen.queryByRole('radiogroup')).not.toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Skin tone/ })).toHaveFocus();
  });

  it('moves between categories in the category bar', async () => {
    const user = userEvent.setup();
    render(<EmojiPicker categories={small} onSelect={() => {}} />);
    const first = screen.getByRole('button', { name: 'Smileys & emotion' });
    expect(first).toHaveAttribute('aria-current', 'true');
    first.focus();
    await user.keyboard('{ArrowRight}');
    const people = screen.getByRole('button', { name: 'People & body' });
    expect(people).toHaveFocus();
    await user.keyboard('{Enter}');
    expect(people).toHaveAttribute('aria-current', 'true');
    expect(screen.getByRole('button', { name: 'thumbs up' })).toHaveAttribute('tabindex', '0');
  });

  it('shows a preview of the focused emoji and takes translated text', async () => {
    const user = userEvent.setup();
    render(
      <EmojiPicker
        categories={small}
        onSelect={() => {}}
        showSkinTones={false}
        messages={{ search: 'Buscar emoji', categories: { smileys: 'Caras' } as never }}
      />,
    );
    expect(screen.getByRole('searchbox', { name: 'Buscar emoji' })).toBeInTheDocument();
    expect(screen.getByRole('group', { name: 'Caras' })).toBeInTheDocument();
    expect(screen.getByRole('group', { name: 'People & body' })).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /Skin tone/ })).not.toBeInTheDocument();
    await user.hover(screen.getByRole('button', { name: 'winking face' }));
    expect(document.querySelector('.mn-emoji-picker__preview')).toHaveTextContent('winking face');
  });

  it('renders on the server', () => {
    const html = renderToString(<EmojiPicker onSelect={() => {}} />);
    expect(html).toContain('mn-emoji-picker');
    expect(html).toContain('thumbs up');
  });
});

describe('EmojiPickerPopover', () => {
  it('opens from the trigger, focuses search, picks, closes and returns focus', async () => {
    const user = userEvent.setup();
    const onSelect = vi.fn();
    render(
      <EmojiPickerPopover categories={small} onSelect={onSelect}>
        <button type="button">Add reaction</button>
      </EmojiPickerPopover>,
    );
    const trigger = screen.getByRole('button', { name: 'Add reaction' });
    await user.click(trigger);
    const dialog = await screen.findByRole('dialog', { name: 'Emoji picker' });
    expect(within(dialog).getByRole('searchbox')).toHaveFocus();
    await expectNoA11yViolations();
    await user.keyboard('{ArrowDown}{Enter}');
    expect(onSelect).toHaveBeenCalledWith('😀', expect.anything());
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
    expect(trigger).toHaveFocus();
  });

  it('closes with Escape (after clearing the search) and can stay open on select', async () => {
    const user = userEvent.setup();
    const onSelect = vi.fn();
    render(
      <EmojiPickerPopover categories={small} onSelect={onSelect} closeOnSelect={false}>
        <button type="button">Emoji</button>
      </EmojiPickerPopover>,
    );
    await user.click(screen.getByRole('button', { name: 'Emoji' }));
    await user.click(await screen.findByRole('button', { name: 'thumbs up' }));
    expect(screen.getByRole('dialog')).toBeInTheDocument();
    await user.type(screen.getByRole('searchbox'), 'wave');
    await user.keyboard('{Escape}');
    expect(screen.getByRole('searchbox')).toHaveValue('');
    expect(screen.getByRole('dialog')).toBeInTheDocument();
    await user.keyboard('{Escape}');
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
  });
});
