import { useRef, useState } from 'react';
import { Button } from '@rookcom/monarch-react';
import { AiInlinePrompt } from '@rookcom/monarch-react/ai';
import type { ComponentDocs } from '../../../internal/component-docs.js';

const PARAGRAPH =
  'The team shipped the new onboarding flow in March. Early numbers suggest more people finish setup on their first visit, although the sample is still small and support tickets have not changed much yet.';

function SelectionDemo() {
  const paragraph = useRef<HTMLParagraphElement>(null);
  const button = useRef<HTMLButtonElement>(null);
  const [open, setOpen] = useState(false);
  const [selection, setSelection] = useState('');
  const [rect, setRect] = useState<DOMRect | null>(null);
  const [last, setLast] = useState('');

  const openFromSelection = () => {
    const sel = window.getSelection();
    const text = sel?.toString().trim() ?? '';
    if (!sel || !text || !paragraph.current?.contains(sel.anchorNode)) return;
    setSelection(text);
    setRect(sel.getRangeAt(0).getBoundingClientRect());
    setOpen(true);
  };

  return (
    <div style={{ display: 'grid', gap: 12 }}>
      {/* Pointer shortcut only; the button below is the keyboard path. */}
      {/* eslint-disable-next-line jsx-a11y/no-noninteractive-element-interactions */}
      <p ref={paragraph} onMouseUp={openFromSelection} style={{ margin: 0 }}>
        {PARAGRAPH}
      </p>
      <div>
        <Button
          ref={button}
          size="sm"
          onClick={() => {
            setSelection(PARAGRAPH);
            setRect(null);
            setOpen(true);
          }}
        >
          Edit paragraph with AI
        </Button>
      </div>
      {last ? <p style={{ margin: 0 }}>Sent: “{last}”</p> : null}
      <AiInlinePrompt
        anchor={() => rect ?? button.current?.getBoundingClientRect() ?? null}
        open={open}
        onOpenChange={setOpen}
        context={selection}
        suggestions={['Shorten', 'Make it more formal', 'Fix spelling and grammar']}
        onSubmit={setLast}
      />
    </div>
  );
}

export default {
  id: 'ai-inline-prompt',
  title: 'AiInlinePrompt',
  summary: '⌘K-style prompt that floats over a text selection and sends an instruction about it.',
  sections: {
    purpose: {
      summary:
        'Lets people ask AI to act on what they are looking at, right where it is: select text (or a block), type “make this shorter”, send. It floats next to the selection instead of sending people to a separate assistant panel.',
      useWhen: [
        'Editing documents, emails or notes where AI rewrites a selection.',
        'A keyboard shortcut (⌘K / Ctrl+K) or a toolbar button asks AI about the current block.',
      ],
      avoidWhen: [
        'Open-ended conversations: use AiPromptInput in AiAssistantPanel.',
        'Suggesting a value for one form field: use AiAutofillField.',
      ],
    },
    designPrinciples: {
      items: [
        'Appears at the work, not across the screen; flips and shifts to stay visible.',
        'Shows what it will act on (a short excerpt of the selection).',
        'Never sends on its own: Enter or the Send button only.',
        'Closes as easily as it opens, and gives focus back exactly where it was.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'Built on Popover (Radix) with a virtual anchor; Popover content props such as `side`, `align` and `sideOffset` pass through.',
        '`anchor` can be a function so the panel follows the selection while the page scrolls.',
        'The product applies the result (often with AiEditProposal or AiInlineDiff); this component only collects the prompt.',
      ],
    },
    typedProperties: { source: 'typescript' },
    variants: {
      items: [],
      notes: [
        'No visual variants. `suggestions` adds quick-prompt buttons; `context` adds the excerpt.',
      ],
    },
    states: {
      items: [
        { name: 'Open', attribute: 'data-state', description: '`open` or `closed`, from Popover.' },
        {
          name: 'Pending',
          attribute: 'data-pending',
          description: 'Field and buttons are disabled; Send shows a spinner.',
        },
        { name: 'Empty', description: 'Send is disabled until the field has non-space text.' },
      ],
    },
    interaction: {
      items: [
        'Opening focuses the field (with `defaultValue` placed before the caret).',
        'Enter sends the trimmed prompt; Shift+Enter adds a line; IME composition is respected.',
        'A suggestion sends its text at once.',
        'After sending it closes (unless `closeOnSubmit` is false). Escape or clicking outside closes without sending.',
        'Focus returns to the element focused before opening, or to `returnFocusTo`.',
      ],
    },
    keyboard: {
      items: [
        { keys: 'Enter', action: 'Sends the prompt.' },
        { keys: 'Shift+Enter', action: 'New line.' },
        { keys: 'Tab / Shift+Tab', action: 'Moves between the field, Send and suggestions.' },
        { keys: 'Escape', action: 'Closes and returns focus.' },
      ],
      notes: [
        'The product decides the opening shortcut (for example ⌘K); see AiLauncher’s `parseAiShortcut` for a helper.',
      ],
    },
    accessibility: {
      role: 'dialog (non-modal)',
      name: '“Ask AI”',
      items: [
        'The field is named “Prompt” and described by the selection excerpt and the key hint.',
        'The Bot glyph marks it as AI visibly; it is decorative to assistive technology because the name says it.',
        'The Send button is an icon button with a text name.',
      ],
    },
    responsive: {
      items: [
        'At most 28rem wide and never wider than the screen minus 2rem.',
        'Field grows to 10rem then scrolls. Touch targets are 44px on coarse pointers, where the key hint is hidden.',
        'Logical properties for RTL; floating position mirrors with `align`.',
      ],
    },
    theme: {
      items: [
        'Panel: Popover surface with an `--mn-color-ai-border` edge.',
        'Field focus: `--mn-color-ai-solid` ring; glyph `--mn-color-ai-strong`.',
        'Forced colours: CanvasText borders and a Highlight outline on focus.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Over a selection (select text, or use the button)',
          stack: true,
          render: () => <SelectionDemo />,
        },
      ],
    },
    doDont: {
      do: [
        'Offer a keyboard way to open it (a shortcut or a visible button), not only text selection.',
        'Show the result as a proposal people can accept or reject.',
      ],
      dont: [
        'Do not apply changes without review.',
        'Do not open it automatically on every selection; wait for an intent such as a shortcut or a toolbar click.',
      ],
    },
    related: {
      items: [
        { id: 'ai-edit-proposal', reason: 'Show the rewrite for review.' },
        { id: 'ai-inline-diff', reason: 'Inline accept/reject of changes.' },
        { id: 'ai-prompt-input', reason: 'The full composer for conversations.' },
        { id: 'popover', reason: 'The floating layer it is built on.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: ['popover', 'button'],
    },
    testing: {
      unit: [
        'Named dialog with focused field and described context.',
        'Enter sends trimmed text, Shift+Enter adds a line, empty prompts are blocked, then it closes and returns focus.',
        'Escape closes and returns focus; suggestions send; `closeOnSubmit`; pending and `defaultValue`.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
