import { useState } from 'react';
import { Button } from '@rookcom/monarch-react';
import {
  EmojiPicker,
  EmojiPickerPopover,
  pushRecentEmoji,
  type EmojiSkinTone,
} from '@rookcom/monarch-react/collaboration';
import type { ComponentDocs } from '../../../internal/component-docs.js';

function InlineDemo() {
  const [recents, setRecents] = useState<string[]>(['👍', '🎉', '✅']);
  const [tone, setTone] = useState<EmojiSkinTone>('none');
  const [last, setLast] = useState('');
  return (
    <div style={{ display: 'grid', gap: '0.75rem' }}>
      <div
        style={{
          border: '1px solid var(--mn-color-border-subtle)',
          borderRadius: 'var(--mn-radius-lg)',
          background: 'var(--mn-color-bg-raised)',
        }}
      >
        <EmojiPicker
          recents={recents}
          skinTone={tone}
          onSkinToneChange={setTone}
          onSelect={(emoji) => {
            setLast(emoji);
            setRecents((list) => pushRecentEmoji(list, emoji));
          }}
        />
      </div>
      <p style={{ margin: 0 }}>Last picked: {last || 'none yet'}</p>
    </div>
  );
}

function PopoverDemo() {
  const [text, setText] = useState('Looks good ');
  return (
    <div style={{ display: 'flex', gap: '0.75rem', alignItems: 'center', flexWrap: 'wrap' }}>
      <EmojiPickerPopover side="bottom" onSelect={(emoji) => setText((t) => t + emoji)}>
        <Button size="sm">Add emoji</Button>
      </EmojiPickerPopover>
      <span>{text}</span>
    </div>
  );
}

export default {
  id: 'emoji-picker',
  title: 'EmojiPicker',
  summary:
    'Search, categories, recently used and skin tones, with a compact built-in emoji set and full keyboard navigation.',
  sections: {
    purpose: {
      summary:
        'Lets people add an emoji to a message or react to one without leaving the keyboard or loading a third-party dataset. Monarch ships a compact built-in set of about 500 common emoji with English names and keywords; products that need every emoji pass a full dataset as `categories`. Needed by: Chat (EmojiPicker, EmojiPopover and LazyEmojiPicker for the composer and reactions).',
      useWhen: [
        'Adding a reaction to a message, comment or post.',
        'Inserting an emoji into a composer.',
        'Choosing an icon emoji for a channel, page or space.',
      ],
      avoidWhen: [
        'Choosing from a short, fixed set of reactions: show the reactions as buttons.',
        'Picking a status or meaning: use a Select or SegmentedControl with words.',
      ],
    },
    designPrinciples: {
      items: [
        'No third-party data: the built-in set is part of Monarch, and no request leaves the page.',
        'Keyboard first: the search field is focused on open, Arrow Down enters the grid, arrows move in two dimensions and Enter picks.',
        'One tab stop per region: search, skin tone, category bar and grid each take one Tab.',
        'The product owns memory: recents and the skin tone are props and callbacks, so they can follow the person across devices.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'The ref is forwarded to the picker’s root `<div>`.',
        '`EmojiPickerPopover` wraps the picker in a Popover around the trigger you pass as its child; it takes every picker prop plus `open`, `defaultOpen`, `onOpenChange`, `closeOnSelect`, `side` and `align`.',
        'Helpers: `applySkinTone(emoji, tone)`, `searchEmoji(categories, query, limit?)` (also useful for `:shortcode` completion), `pushRecentEmoji(recents, emoji, max?)` and the built-in `defaultEmojiCategories`.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        'Migrating from the app copy: Chat `EmojiPicker({ me, onPick, autoFocus })` used frimousse with a self-hosted Emojibase dataset and kept recents and skin tone in localStorage under `me`. Use `onSelect` for `onPick`; keep the storage in the product and pass `recents` (update with `pushRecentEmoji`), `skinTone` and `onSkinToneChange`. Chat `EmojiPopover({ trigger, label, onPick, side, align, open, onOpenChange })` becomes `EmojiPickerPopover` with the trigger element as its child (an IconButton with that `label`), `onSelect` and the same `side`, `align`, `open` and `onOpenChange`. `LazyEmojiPicker` is no longer needed: the built-in data is small. To keep the full Emojibase set, map it to `categories`.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'columns',
          values: ['8 (default)', 'any whole number'],
          description: 'Emoji per row; arrow keys follow the same grid.',
        },
        {
          prop: 'showSkinTones / showPreview',
          values: ['true', 'false'],
          description: 'Hide the skin tone control or the name preview for a smaller picker.',
        },
      ],
      notes: ['Use it inline, or in a popover with `EmojiPickerPopover`.'],
    },
    states: {
      items: [
        {
          name: 'Browsing',
          description: 'The category bar marks the category in view with `aria-current`.',
        },
        {
          name: 'Searching',
          attribute: 'data-searching',
          description:
            'One “Search results” group replaces the categories; the count is announced.',
        },
        { name: 'No results', description: '“No emoji found” in place of the grid.' },
        {
          name: 'Choosing a skin tone',
          description: 'The skin tone button is expanded and a radio group of six tones is shown.',
        },
        {
          name: 'Active emoji',
          attribute: 'data-active',
          description: 'The grid’s single tab stop; its name shows in the preview.',
        },
      ],
    },
    interaction: {
      items: [
        'Clicking an emoji calls `onSelect` with the emoji (skin tone applied when it accepts one) and its entry.',
        'Typing filters by name and keywords; every word must match the start of a word.',
        'A category in the bar scrolls the grid to it.',
        'The popover closes after a pick unless `closeOnSelect={false}`, and on Escape or a click outside, returning focus to the trigger.',
      ],
    },
    keyboard: {
      items: [
        { keys: 'Arrow Down (in search)', action: 'Moves into the grid.' },
        { keys: 'Enter (in search)', action: 'Picks the first result.' },
        {
          keys: 'Escape (in search)',
          action: 'Clears the search; a second Escape closes the popover.',
        },
        {
          keys: 'Arrow keys (in grid)',
          action:
            'Move by one emoji or one row, across categories; Arrow Up from the first row returns to search.',
        },
        { keys: 'Home / End', action: 'First or last emoji in the row.' },
        { keys: 'Ctrl + Home / Ctrl + End', action: 'First or last emoji overall.' },
        { keys: 'Page Up / Page Down', action: 'Previous or next category.' },
        { keys: 'Enter / Space', action: 'Picks the focused emoji.' },
        {
          keys: 'Left / Right (category bar)',
          action: 'Moves between categories; Enter jumps to one.',
        },
        {
          keys: 'Arrow keys (skin tones)',
          action: 'Choose a tone; Escape closes the tones and returns to the button.',
        },
      ],
    },
    accessibility: {
      role: 'group (dialog when in EmojiPickerPopover)',
      name: '“Emoji picker” (`messages.label`).',
      items: [
        'Each emoji is a button named by its English name (“thumbs up”); the glyph itself is hidden so screen readers do not read the code points.',
        'Each category is a group labelled by its visible heading.',
        'The number of search results is announced in a polite status region.',
        'The category bar is a toolbar with one tab stop and `aria-current` on the category in view.',
        'Skin tones are a radio group; the button states the current tone and `aria-expanded`.',
        'The preview line is visual only: the focused button already carries the name.',
      ],
    },
    responsive: {
      items: [
        'The picker is as wide as its columns allow and never wider than its container; emoji shrink to fit on narrow screens.',
        'On touch screens emoji, categories and tones grow to 44px, and the popover can use the phone’s full width.',
        'The category bar scrolls sideways inside itself rather than widening the page.',
      ],
    },
    theme: {
      items: [
        'Surfaces use `bg-raised` (sticky headings) and `bg-muted` (hover and focus); the chosen tone and current category use the accent tokens.',
        'Forced colours mark the current category and chosen tone with a `Highlight` outline.',
        'Emoji keep their own colours in every theme.',
      ],
    },
    examples: {
      items: [
        { title: 'Inline with recents and skin tones', render: () => <InlineDemo /> },
        { title: 'In a popover', render: () => <PopoverDemo /> },
      ],
    },
    doDont: {
      do: [
        'Store recents and the skin tone per person, so the picker remembers them across devices.',
        'Give the trigger an accessible name such as “Add reaction”.',
        'Pass a full dataset as `categories` if people expect every emoji.',
      ],
      dont: [
        'Do not load emoji data from a third-party CDN.',
        'Do not use emoji alone to carry meaning such as status or priority.',
      ],
    },
    related: {
      items: [
        { id: 'message-row', reason: 'Shows reactions and hosts the add-reaction trigger.' },
        { id: 'message-composer', reason: 'Inserts picked emoji into the message.' },
        { id: 'popover', reason: 'The floating panel of EmojiPickerPopover.' },
        { id: 'search-input', reason: 'The search field.' },
      ],
    },
    dependencies: { packages: ['react'], components: ['popover', 'search-input'] },
    testing: {
      unit: [
        'Skin tone application, search ranking, recents helper and built-in data integrity.',
        'Named groups, single grid tab stop and two-dimensional arrow navigation across categories and back to search.',
        'Pick with click, Enter and Space; search filtering, announced count, Enter picks first result, empty state.',
        'Recents first and named from the dataset; skin tone radio group with arrows and Escape.',
        'Category bar navigation; preview; translated text.',
        'Popover: focus on open, pick closes and returns focus, Escape clears search before closing, `closeOnSelect={false}`.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
