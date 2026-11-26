import { useState } from 'react';
import { ReactionBar, type ReactionBarItem } from '@rookcom/monarch-react/collaboration';
import type { ComponentDocs } from '../../../internal/component-docs.js';

const QUICK = ['👍', '❤️', '😄', '🎉', '👀', '✅'];

const START: ReactionBarItem[] = [
  { emoji: '👍', count: 4, reacted: true, label: 'thumbs up', title: 'You, Priya, Tom and Lee' },
  { emoji: '🎉', count: 2, label: 'party popper', title: 'Ana and Ben' },
  { emoji: '👀', count: 1, label: 'eyes', title: 'Sam' },
];

function ReactionDemo() {
  const [reactions, setReactions] = useState(START);
  const toggle = (emoji: string, reacted: boolean) =>
    setReactions((list) => {
      const found = list.find((r) => r.emoji === emoji);
      if (!found) return [...list, { emoji, count: 1, reacted: true }];
      return list
        .map((r) =>
          r.emoji === emoji ? { ...r, reacted, count: r.count + (reacted ? 1 : -1) } : r,
        )
        .filter((r) => r.count > 0);
    });
  return <ReactionBar reactions={reactions} onReactionToggle={toggle} quickReactions={QUICK} />;
}

export default {
  id: 'reaction-bar',
  title: 'ReactionBar',
  summary:
    'Emoji reactions with counts: toggle your own, add more from the emoji picker, one tab stop with arrow keys, and every chip named with its count.',
  sections: {
    purpose: {
      summary:
        'Lets people react to a message, comment or post with emoji and see how others reacted. Needed by: Rook Chat (MessageItem.tsx: `QUICK_REACTIONS`, `toggleReaction`, the "More reactions" picker), Rook Base (post and comment like counts in c/[slug]/posts/[postId]).',
      useWhen: [
        'Under messages, comments, posts and updates where lightweight feedback replaces a reply.',
        'A read-only summary of reactions, for example in a digest or a search result.',
      ],
      avoidWhen: [
        'A single like or upvote: use ToggleButton with a count.',
        'Formal decisions or votes: use DecisionCard.',
        'Inside MessageRow, which renders its own reaction chips; use its `reactionAction` slot for an add button.',
      ],
    },
    designPrinciples: {
      items: [
        'The product owns the data: the bar reports `onReactionToggle(emoji, reacted)` and renders whatever `reactions` it gets back.',
        'Your reaction is shown pressed (`aria-pressed`) in the accent colour, so it is clear what clicking will undo.',
        "Who reacted is one hover away (`title`) and is also the chip's accessible description.",
        'Zero-count reactions disappear; there are no empty chips.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'Without `onReactionToggle` the bar is read-only: a named group of images, with no add button, and nothing at all when there are no reactions.',
        'Picking an emoji you already reacted with does nothing; picking one someone else used, or a new one, calls `onReactionToggle(emoji, true)`.',
        "`quickReactions` fill the picker's Recent row; `categories` and `pickerMessages` pass through to EmojiPicker.",
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        "Same item shape as MessageRow's `MessageReaction` (`emoji`, `count`, `reacted`, `label`, `title`), so the same data feeds both.",
      ],
    },
    variants: {
      items: [
        {
          prop: 'size',
          values: ['sm', 'md'],
          description: 'Chip size for messages and comments, or posts. Exposed as `data-size`.',
        },
        {
          prop: 'addable',
          values: ['true', 'false'],
          description: 'The "+" button that opens the emoji picker.',
        },
        {
          prop: 'max',
          values: ['number'],
          description: 'Hides the add button once this many distinct reactions exist.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Reacted',
          attribute: 'data-reacted, aria-pressed="true"',
          description: 'Accent border, tint and text.',
        },
        {
          name: 'Read-only',
          attribute: 'data-readonly',
          description: 'Chips are images, not buttons.',
        },
        {
          name: 'Picker open',
          description: 'The emoji picker pops over the add button; focus returns to it on close.',
        },
        { name: 'Focus', description: 'Chips show the Monarch focus ring on `:focus-visible`.' },
      ],
    },
    interaction: {
      items: [
        'Clicking a chip toggles your reaction; the product updates the count.',
        'The add button opens EmojiPickerPopover, which focuses its search, closes on pick or Escape and returns focus.',
        'Hovering a chip shows who reacted.',
      ],
    },
    keyboard: {
      items: [
        { keys: 'Tab', action: 'Moves into the bar (one tab stop) and out again.' },
        {
          keys: 'ArrowRight / ArrowLeft',
          action:
            'Next or previous chip, wrapping, including the add button (reversed in right-to-left).',
        },
        { keys: 'Home / End', action: 'First chip or the add button.' },
        { keys: 'Enter / Space', action: 'Toggles the focused reaction, or opens the picker.' },
        { keys: 'Escape', action: 'Closes the picker (after clearing its search).' },
      ],
    },
    accessibility: {
      role: 'toolbar of toggle buttons (read-only: group of images)',
      name: '`messages.label` ("Reactions"); each chip "thumbs up, 4 reactions" from `label` (default: the emoji) and `count`.',
      items: [
        'Follows the WAI-ARIA toolbar pattern with a roving tabindex.',
        'The emoji glyph and count are `aria-hidden`; the name says both in words.',
        '`title` (who reacted) becomes the accessible description.',
        'The add button is an IconButton named "Add reaction".',
      ],
    },
    responsive: {
      items: [
        'Chips wrap onto more lines; nothing scrolls sideways at 320px.',
        'On coarse pointers each chip and the add button are at least 44px.',
        'Gaps use logical properties; arrow keys follow the reading direction.',
      ],
    },
    theme: {
      items: [
        "Surface, border and accent tokens, as on MessageRow's chips; follows light, dark and high-contrast themes.",
        'Forced colours: chips get a CanvasText border, your reactions a thicker Highlight border.',
        'Reduced motion: hover transitions become instant.',
      ],
    },
    examples: {
      items: [
        { title: 'Interactive', render: () => <ReactionDemo /> },
        {
          title: 'Read-only',
          render: () => <ReactionBar reactions={START} />,
        },
        {
          title: 'Post size with a limit',
          render: () => (
            <ReactionBar reactions={START} onReactionToggle={() => {}} size="md" max={3} />
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Pass a spoken `label` for each emoji ("thumbs up"), so names are words, not glyphs.',
        'Put "You" first in `title` when you reacted.',
        'Update counts optimistically and roll back if saving fails.',
      ],
      dont: [
        'Show reactions with a count of zero.',
        'Use reactions as a voting or approval mechanism.',
        'Render a second add button beside the bar.',
      ],
    },
    related: {
      items: [
        { id: 'emoji-picker', reason: 'The picker behind the add button.' },
        { id: 'message-row', reason: 'Messages with reactions built in.' },
        { id: 'comment-thread', reason: 'A discussion that can carry reactions per comment.' },
        { id: 'post-card', reason: 'Posts that show reaction summaries.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: ['button', 'emoji-picker'],
    },
    testing: {
      unit: [
        'Chip names with counts, pressed state, description from `title`, zero counts hidden; no axe violations.',
        'Toggle reports the next state.',
        'Toolbar keyboard: one tab stop, arrows wrap, Home and End, Enter toggles, Tab leaves.',
        'Add from the picker, focus returns; picking your own reaction does nothing.',
        'Read-only group and empty render; `max`, `addable`, size and translated messages.',
        'Renders on the server.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
