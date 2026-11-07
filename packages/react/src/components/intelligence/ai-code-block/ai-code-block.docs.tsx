import { useState } from 'react';
import { AiCodeBlock } from '@rookcom/monarch-react/ai';
import type { ComponentDocs } from '../../../internal/component-docs.js';

const SNIPPET = `import { formatDate } from './dates';

// Show the due date in the reader's locale.
export function dueLabel(task: { due: string }) {
  const days = Math.round((Date.parse(task.due) - Date.now()) / 86_400_000);
  return days < 0 ? 'Overdue' : \`Due \${formatDate(task.due)}\`;
}
`;

const DIFF = `--- a/settings.json
+++ b/settings.json
@@ -1,4 +1,4 @@
 {
-  "retries": 3,
+  "retries": 5,
   "timeoutMs": 2000
 }
`;

function WithActions() {
  const [last, setLast] = useState<string | null>(null);
  return (
    <div style={{ display: 'grid', gap: 'var(--mn-space-2)', width: '100%' }}>
      <AiCodeBlock
        code={"SELECT id, title\nFROM tasks\nWHERE status = 'open'\nORDER BY due_at;"}
        language="sql"
        onApply={() => setLast('Applied the query.')}
        onInsert={() => setLast('Inserted the query.')}
      />
      <p style={{ margin: 0, fontSize: 'var(--mn-text-xs)' }}>{last ?? 'Nothing applied yet.'}</p>
    </div>
  );
}

export default {
  id: 'ai-code-block',
  title: 'AiCodeBlock',
  summary:
    'Code with a filename or language header, copy, wrap toggle, line numbers, light highlighting, diffs and optional Apply / Insert actions.',
  sections: {
    purpose: {
      summary:
        'Shows a code sample so people can read, copy and reuse it: a header names the file or language, the code scrolls sideways inside its own labelled region, and optional Apply and Insert actions hand the code back to the product. AiMarkdown uses it for every fenced code block. Needed by: AiMarkdown, AiStreamingText, AiMessageView.',
      useWhen: [
        'An AI answer, a tool result or documentation contains code or a command.',
        'A suggested change should be shown as a diff with clear added and removed lines.',
        'The product can apply or insert the code (into an editor, a query box, a settings file).',
      ],
      avoidWhen: [
        'One short value to copy (an ID, a URL): use CopyField.',
        'Editable code: use a code editor; AiCodeBlock is read-only.',
        'A short identifier inside a sentence: use inline code in AiMarkdown.',
      ],
    },
    designPrinciples: {
      items: [
        'Never scrolls the page sideways: long lines scroll inside the block, or wrap with the toggle.',
        'Diff meaning never depends on colour: every changed line has a visible + or − marker and a hidden "Added" / "Removed" prefix.',
        'Light, dependency-free highlighting for common languages; anything unknown is plain text rather than guessed.',
        'Usable outside AI: nothing in it is AI-specific.',
        'Copy copies exactly `code`: line numbers and diff markers are never part of it.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'AiCodeBlock forwards its ref to the root `<div>`.',
        '`tokenizeCode(code, language)` returns `{ type, text }[]` tokens (`keyword`, `string`, `comment`, `number`, `tag`, `attr`, `property`, `plain`); `resolveCodeLanguage(id)` says which grammar applies, or null.',
        '`codeLanguageName(id)` gives the header name ("ts" → "TypeScript"). `AI_CODE_BLOCK_MESSAGES` holds the English defaults.',
        'Highlighted languages: js, jsx, ts, tsx, json, python, bash/sh, css/scss, html/xml/svg, sql, go and rust (with common aliases).',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: ['Other `<div>` attributes pass through to the root.'],
    },
    variants: {
      items: [
        {
          prop: 'diff',
          values: ['true', 'false'],
          description: 'Diff mode. On by default when `language` is "diff" or "patch".',
        },
        {
          prop: 'lineNumbers',
          values: ['true', 'false'],
          description: 'Adds a line-number gutter (hidden from screen readers and selection).',
        },
        {
          prop: 'wrap / defaultWrap',
          values: ['true', 'false'],
          description:
            'Wrap long lines instead of scrolling sideways. The header toggle changes it.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Wrapped',
          attribute: 'data-wrap',
          description: 'Lines wrap; the toggle is pressed (`aria-pressed="true"`).',
        },
        {
          name: 'Diff',
          attribute: 'data-diff',
          description: 'Lines carry `data-diff="add|remove|hunk|meta|context"`.',
        },
        {
          name: 'Line numbers',
          attribute: 'data-line-numbers',
          description: 'The gutter is shown.',
        },
        {
          name: 'Incomplete',
          attribute: 'data-incomplete',
          description:
            'The code is still streaming: Apply and Insert are disabled, Copy still works.',
        },
        {
          name: 'Copied',
          attribute: 'data-state="copied"',
          description: 'From CopyButton, on the copy button.',
        },
      ],
    },
    interaction: {
      items: [
        'Copy writes `code` to the clipboard and announces "Copied to clipboard".',
        'The wrap toggle switches between wrapping and sideways scrolling; `onWrapChange` reports it.',
        'Apply and Insert call `onApply(code)` / `onInsert(code)`; the product decides what happens and how to confirm it.',
        'The code region takes focus so it can be scrolled with the arrow keys.',
      ],
    },
    keyboard: {
      items: [
        {
          keys: 'Tab',
          action: 'Moves through the wrap toggle, Copy, Insert, Apply, then the code region.',
        },
        { keys: 'Enter / Space', action: 'Activates the focused button.' },
        { keys: 'Arrow keys', action: 'Scroll the focused code region.' },
      ],
    },
    accessibility: {
      role: 'region (the code area)',
      name: 'The filename, else "<Language> code", else "Code" (`messages.label`).',
      items: [
        'The wrap toggle is a button with `aria-pressed`.',
        'Copy, Apply and Insert include the block name as visually hidden text ("Apply app.ts"), so several blocks on a page have distinct names.',
        'Diff lines start with a hidden "Added:" or "Removed:"; the visible +/− markers are `aria-hidden`.',
        'Line numbers are `aria-hidden` and not selectable.',
        'Forced colours: the diff markers use Highlight, so changes stay visible without the backgrounds.',
      ],
    },
    responsive: {
      items: [
        'The header wraps its actions under the title on narrow screens; the title truncates with an ellipsis.',
        'Long lines scroll inside the block, never the page, down to 390px wide.',
        'Header buttons are at least 44px tall on coarse pointers.',
      ],
    },
    theme: {
      items: [
        'Surfaces use `--mn-color-bg-subtle` and `--mn-color-bg-muted`; syntax colours use the status "strong" tokens and `--mn-color-ai-strong`, which keep text contrast in every theme.',
        'Diffs use `--mn-color-ai-diff-add`, `--mn-color-ai-diff-remove` and their `-strong` variants.',
        'Logical properties throughout, so the header mirrors in right-to-left layouts (the code itself stays left-to-right as written).',
      ],
    },
    examples: {
      items: [
        {
          title: 'Filename, line numbers',
          stack: true,
          render: () => (
            <AiCodeBlock code={SNIPPET} language="ts" filename="due-label.ts" lineNumbers />
          ),
        },
        {
          title: 'Diff',
          stack: true,
          render: () => <AiCodeBlock code={DIFF} language="diff" filename="settings.json" />,
        },
        {
          title: 'Apply and Insert',
          stack: true,
          render: () => <WithActions />,
        },
        {
          title: 'Wrapped shell command',
          stack: true,
          render: () => (
            <AiCodeBlock
              code="npx example-cli deploy --project sample-project --region eu-west --confirm --verbose"
              language="bash"
              defaultWrap
            />
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Pass `filename` when the code belongs to a file; it names the region and the buttons.',
        'Confirm what Apply did (a toast, an inline message, an undo).',
        'Set `incomplete` while code is still streaming so it cannot be applied half-written.',
      ],
      dont: [
        'Do not add the diff markers to `code` yourself; use `diff` with a unified diff.',
        'Do not run or evaluate the code on Apply without the person confirming it.',
        'Do not use it for editable code.',
      ],
    },
    related: {
      items: [
        { id: 'ai-markdown', reason: 'Renders fenced code in answers through AiCodeBlock.' },
        { id: 'copy-button', reason: 'The copy control used in the header.' },
        { id: 'copy-field', reason: 'For a single value to copy rather than a block of code.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: ['button', 'copy-button'],
      notes: ['Tokens: `--mn-font-mono`, `--mn-color-ai-diff-*`, `--mn-color-ai-strong`.'],
    },
    testing: {
      unit: [
        'Tokenizes keywords, strings, comments and numbers for js/ts, python, json, sql, html, css, bash, rust and go; unknown languages stay plain.',
        'Shows the language, a labelled focusable region and highlighted tokens; Tab reaches the toolbar.',
        'Names the region and buttons after the filename.',
        'Copies the exact code and announces it.',
        'Wrap toggle works uncontrolled and controlled, with `aria-pressed`.',
        'Diff lines get kinds, visible +/− markers and hidden Added / Removed text; line numbers are hidden.',
        'Apply and Insert receive the code and are disabled while incomplete.',
        'Messages replace every string.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
