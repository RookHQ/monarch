import { useState } from 'react';
import { AiMemoryChip, type AiMemory } from '@rookcom/monarch-react/ai';
import type { ComponentDocs } from '../../../internal/component-docs.js';

const sample: AiMemory = {
  id: 'm1',
  text: 'Prefers summaries under 200 words',
  entity: { kind: 'person', name: 'Ada Lovelace' },
};

function EditableDemo() {
  const [memory, setMemory] = useState<AiMemory | null>(sample);
  if (!memory) return <p>Forgotten.</p>;
  return (
    <AiMemoryChip
      memory={memory}
      onEdit={(_, text) => setMemory({ ...memory, text })}
      onForget={() => setMemory(null)}
    />
  );
}

export default {
  id: 'ai-memory-chip',
  title: 'AiMemoryChip',
  summary: 'One thing the AI remembers, "Remembered: …", with inline edit and forget.',
  sections: {
    purpose: {
      summary:
        'Shows people, at the moment it happens, that the AI saved something to memory, and lets them correct or remove it right there instead of hunting through settings.',
      useWhen: [
        'An assistant reply saved a memory and the thread should say so.',
        'A list of memories needs editable rows (`variant="row"`), as in AiMemoryPanel.',
      ],
      avoidWhen: [
        'You need the full list with search and bulk forget: use AiMemoryPanel.',
        'You are showing what the AI can see in this turn only: use AiContextChips.',
      ],
    },
    designPrinciples: {
      items: [
        'Memory is never silent: whatever the AI keeps is shown, in plain language.',
        'Correcting is as easy as seeing: Edit and Forget sit on the chip itself.',
        'The chip only reports decisions; the app stores and deletes memories.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'Native `<div>` attributes pass through and the ref is forwarded to the root, which is `role="group"` named by `messages.label`.',
        '`onEdit(id, text)` receives the trimmed text and is never called with empty or unchanged text.',
        '`shortMemoryText(text, max?)` shortens text for button names; `AI_MEMORY_CHIP_MESSAGES` holds the English strings.',
      ],
    },
    typedProperties: { source: 'typescript' },
    variants: {
      items: [
        {
          prop: 'variant',
          values: ['chip', 'row'],
          description:
            '`chip` is an inline pill with the Bot glyph and the "Remembered:" prefix. `row` is a full-width row without the prefix, with `leading` and `meta` slots. Exposed as `data-variant`.',
        },
      ],
    },
    states: {
      items: [
        { name: 'Read only', description: 'No callbacks: just the text, no buttons.' },
        {
          name: 'Editable',
          description: 'Edit and Forget buttons appear when `onEdit` and `onForget` are given.',
        },
        {
          name: 'Editing',
          attribute: 'data-editing',
          description:
            'A text field with Save and Cancel replaces the text. Empty text marks the field invalid and disables Save.',
        },
      ],
    },
    interaction: {
      items: [
        'Edit swaps the text for a field with the text selected; Save or Enter saves, Cancel or Escape restores the text.',
        'Focus returns to the Edit button after saving or cancelling.',
        'Forget calls `onForget` straight away; pair it with an undo toast in the app if you need a way back.',
      ],
    },
    keyboard: {
      items: [
        { keys: 'Tab', action: 'Moves between Edit and Forget, and through the editor.' },
        { keys: 'Enter (in the field)', action: 'Saves the edit.' },
        { keys: 'Shift+Enter (in the field)', action: 'Adds a new line.' },
        { keys: 'Escape (in the field)', action: 'Cancels the edit and returns focus to Edit.' },
      ],
    },
    accessibility: {
      role: 'group',
      name: '`messages.label`, default "Memory"',
      items: [
        'The "Remembered:" prefix is real text, so the memory is identified as AI-kept to assistive technology.',
        'Edit and Forget names include a shortened copy of the text ("Forget memory: Prefers summaries…"), so each is unique in a list.',
        'The field is named "Memory" and described by the Enter / Escape hint; empty text sets `aria-invalid`.',
        'The Bot glyph is decorative.',
      ],
    },
    responsive: {
      items: [
        'The text wraps (long words break), the buttons stay at the end, and nothing scrolls sideways at 390px.',
        'On coarse pointers the buttons grow to 44px.',
      ],
    },
    theme: {
      items: [
        'The chip uses `--mn-color-ai-subtle` with an `--mn-color-ai-border` outline; the prefix and glyph use `--mn-color-ai-strong`.',
        'In forced colours the chip is a CanvasText outline on Canvas.',
      ],
    },
    examples: {
      items: [
        { title: 'Read only', render: () => <AiMemoryChip memory={sample} /> },
        { title: 'Edit and forget', render: () => <EditableDemo /> },
        {
          title: 'Row',
          stack: true,
          render: () => (
            <AiMemoryChip
              variant="row"
              memory={sample}
              meta="Updated 3 Oct 2026"
              onEdit={() => {}}
              onForget={() => {}}
            />
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Show a chip in the thread whenever the AI saves a memory.',
        'Write memories as short, plain statements.',
        'Offer an undo after Forget when the memory cannot be recovered.',
      ],
      dont: [
        'Do not save memories without showing them.',
        'Do not hide Edit or Forget from the person the memory is about.',
      ],
    },
    related: {
      items: [
        { id: 'ai-memory-panel', reason: 'Every memory, grouped, with search and bulk forget.' },
        { id: 'ai-context-chips', reason: 'What the AI sees in this turn, not what it keeps.' },
        { id: 'undo-toast', reason: 'A way back after Forget.' },
      ],
    },
    dependencies: { packages: ['react'], components: ['button', 'text-field'] },
    testing: {
      unit: [
        'Prefix, text and entity; row variant with leading and meta; ref and prop forwarding.',
        'Forget calls back with the id through a uniquely named button.',
        'Enter saves trimmed text and returns focus to Edit; Escape cancels; Shift+Enter adds a line.',
        'Empty text is invalid and not saved; unchanged text is not saved; strings localise.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
