import { useState } from 'react';
import { Button } from '@rookcom/monarch-react';
import { AiContextChips, type AiContextChip } from '@rookcom/monarch-react/ai';
import type { ComponentDocs } from '../../../internal/component-docs.js';

const CONTEXT: AiContextChip[] = [
  { id: 'page', kind: 'page', label: 'Project overview', removable: false, tokens: 640 },
  {
    id: 'sel',
    kind: 'selection',
    label: 'Selected text',
    description: '“The launch moves to the second week of May…”',
    tokens: 90,
  },
  { id: 'file', kind: 'file', label: 'Quarterly plan.pdf', tokens: 4200 },
  { id: 'rec', kind: 'record', label: 'Account 1042', tokens: 310 },
  { id: 'person', kind: 'person', label: 'Ana Lopez', tokens: 60 },
];

function RemovableDemo() {
  const [items, setItems] = useState(CONTEXT);
  return (
    <div style={{ display: 'grid', gap: 'var(--mn-space-2)', width: '100%', maxWidth: '40rem' }}>
      <AiContextChips
        items={items}
        empty="Nothing attached. The assistant only sees your message."
        onRemove={(id) => setItems((list) => list.filter((i) => i.id !== id))}
      />
      {items.length < CONTEXT.length ? (
        <Button size="sm" variant="ghost" onClick={() => setItems(CONTEXT)}>
          Reset
        </Button>
      ) : null}
    </div>
  );
}

export default {
  id: 'ai-context-chips',
  title: 'AiContextChips',
  summary: 'Removable chips showing what the AI will see, with token weight and a total.',
  sections: {
    purpose: {
      summary:
        'Makes the context of the next prompt visible: which files, selection, page, records, apps and people the AI will read, and how much of the context window each takes. People can remove anything they do not want sent.',
      useWhen: [
        'Above or inside a prompt input when context is attached automatically or through @ mentions.',
        'In an assistant panel that reads the current page or selection.',
      ],
      avoidWhen: [
        'Plain file attachments with upload progress: use AiAttachmentTray.',
        'Showing how full the whole context window is: use AiContextWindowMeter.',
        'Filter tags that change a list: use tag-input or a filter bar.',
      ],
    },
    designPrinciples: {
      items: [
        'Transparency first: nothing is sent silently; every piece of context is a visible chip.',
        'Kind is shown by a glyph and spoken as a prefix ("File: Quarterly plan.pdf"), never by colour alone.',
        'Weight is a number plus three ticks, so heavy items stand out without a colour code.',
        'Removal never loses focus: it moves to the next chip, the previous one, or a fallback such as the prompt.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'Native `<div>` attributes pass through and the ref is forwarded to the root.',
        '`formatAiContextTokens(count, locale?)` formats compact numbers ("1.2K"); `aiContextChipWeight(tokens, reference)` returns `light`, `medium` (a fifth or more) or `heavy` (half or more).',
        '`AiContextKindGlyph` draws the glyph for a kind, for use in your own menus.',
        '`messages.kind` merges per key; `AI_CONTEXT_CHIPS_MESSAGES` holds the English defaults.',
      ],
    },
    typedProperties: { source: 'typescript' },
    variants: {
      items: [
        {
          prop: 'items[].kind',
          values: ['file', 'selection', 'page', 'record', 'app', 'person'],
          description: 'Sets the glyph and the spoken prefix. Exposed as `data-kind` on the chip.',
        },
      ],
      notes: [
        'There is one visual style: AI-subtle chips, so context reads as part of the AI feature.',
      ],
    },
    states: {
      items: [
        {
          name: 'Weight',
          attribute: 'data-weight="light|medium|heavy" on the chip',
          description:
            'From the chip’s share of the total, or of `budget` when set. Only chips with `tokens` have it.',
        },
        {
          name: 'Not removable',
          description: '`removable: false` (or no `onRemove`) hides the remove button.',
        },
        {
          name: 'Empty',
          attribute: 'data-empty on the root',
          description: 'No list; shows `empty` when given, otherwise nothing.',
        },
      ],
    },
    interaction: {
      items: [
        'The remove button calls `onRemove(id)`. When the chip leaves `items`, focus moves to the next chip’s remove button, else the previous one, else `fallbackFocusRef` (or the root).',
        '"<label> removed from context" is announced politely.',
        'Long names are truncated visually; the full name (or `description`) is the tooltip and is always read in full.',
      ],
    },
    keyboard: {
      items: [
        { keys: 'Tab / Shift+Tab', action: 'Moves through the remove buttons.' },
        { keys: 'Enter / Space', action: 'Removes the focused chip.' },
      ],
    },
    accessibility: {
      role: 'list',
      name: '"Context" (messages.label)',
      items: [
        'Each chip reads as "<Kind>: <label>, <n> tokens"; the glyph and ticks are hidden.',
        'Remove buttons are named "Remove <label> from context".',
        'Removal is announced in a polite status region.',
        'The total is plain text after the list.',
      ],
    },
    responsive: {
      items: [
        'Chips wrap onto new lines; names truncate at 16rem (10rem below 30rem), so there is no horizontal scroll at 390px.',
        'On coarse pointers remove buttons are 44px.',
      ],
    },
    theme: {
      items: [
        'Chips: `--mn-color-ai-subtle` fill, `--mn-color-ai-border` border, glyph and filled ticks in `--mn-color-ai-strong`.',
        'Token text in `--mn-color-text-muted`; heavy chips use default text and medium weight.',
        'In forced colours chips use Canvas and CanvasText and empty ticks use GrayText.',
      ],
    },
    examples: {
      items: [
        { title: 'Removable context with weights', stack: true, render: () => <RemovableDemo /> },
        {
          title: 'Read-only, against a budget',
          stack: true,
          render: () => <AiContextChips items={CONTEXT} budget={8000} />,
        },
      ],
    },
    doDont: {
      do: [
        'Show every piece of context the AI will receive, including automatic ones like the current page.',
        'Give token estimates when context is large enough to matter.',
        'Pass `fallbackFocusRef` pointing at the prompt so removing the last chip returns there.',
      ],
      dont: [
        'Do not send context that is not shown as a chip.',
        'Do not use chips for upload progress or errors: use AiAttachmentTray.',
        'Do not rely on the tick colour alone: the token number is the information.',
      ],
    },
    related: {
      items: [
        { id: 'ai-mention-context', reason: 'The @ menu that adds chips.' },
        { id: 'ai-attachment-tray', reason: 'Uploaded files with progress and errors.' },
        { id: 'ai-context-window-meter', reason: 'How full the whole context window is.' },
        { id: 'ai-prompt-input', reason: 'The input the chips sit above.' },
      ],
    },
    dependencies: { packages: ['react'], components: ['button'] },
    testing: {
      unit: [
        '`formatAiContextTokens` and `aiContextChipWeight` thresholds.',
        'Named list, spoken kind prefix, tokens, weights (total and budget) and total.',
        'Non-removable chips; remove from the keyboard; focus to next, previous and fallback; announcement.',
        'Empty state and messages.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
