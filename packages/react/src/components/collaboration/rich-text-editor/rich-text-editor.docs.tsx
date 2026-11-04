import { useState } from 'react';
import { RichTextEditor, type RichTextMentionItem } from '@rookcom/monarch-react/editor';
import type { ComponentDocs } from '../../../internal/component-docs.js';

const PEOPLE: RichTextMentionItem[] = [
  { id: 'p1', label: 'Alex Morgan', description: 'Design' },
  { id: 'p2', label: 'Sam Rivera', description: 'Engineering' },
  { id: 'p3', label: 'Jordan Lee', description: 'Support' },
  { id: 'p4', label: 'Priya Shah', description: 'Product', disabled: true },
];
const searchPeople = (text: string) =>
  PEOPLE.filter((p) => p.label.toLowerCase().includes(text.toLowerCase()));

const NOTES = `<h2>Release checklist</h2><p>Ship the <strong>new onboarding</strong> after the review on Thursday. See the <a href="https://example.com/guide">rollout guide</a>.</p><ul data-type="taskList"><li data-type="taskItem" data-checked="true"><p>Update the help article</p></li><li data-type="taskItem" data-checked="false"><p>Tell the support team</p></li></ul><blockquote><p>Keep the old flow behind a setting for one release.</p></blockquote>`;

function ControlledDemo() {
  const [html, setHtml] = useState('<p>Edit me, then look below.</p>');
  return (
    <div style={{ display: 'grid', gap: 8, width: '100%' }}>
      <RichTextEditor
        aria-label="Summary"
        value={html}
        onValueChange={(v) => setHtml(String(v))}
        toolbar={['bold', 'italic', 'code', '|', 'bulletList', 'orderedList', '|', 'link']}
        maxLength={280}
        minHeight="4rem"
      />
      <code style={{ fontSize: 12, overflowWrap: 'anywhere' }}>{html || '(empty)'}</code>
    </div>
  );
}

export default {
  id: 'rich-text-editor',
  title: 'RichTextEditor',
  summary:
    'A Tiptap rich-text editor: accessible toolbar, Markdown shortcuts, safe paste, links, checklists, mentions, uploads and a character limit.',
  sections: {
    purpose: {
      summary:
        'Edits formatted text (notes, descriptions, replies, documents) with a toolbar and Markdown shortcuts, and hands the product HTML or JSON. It is the editing surface MessageComposer builds on.',
      useWhen: [
        'People write more than a sentence and need headings, lists, links or code.',
        'The product stores formatted text (HTML or Tiptap JSON) and renders it again later.',
        'Mentions or inline images belong in the text itself.',
      ],
      avoidWhen: [
        'Plain text is enough (a title, a short comment): use Textarea, which loads no editor.',
        'The box sends messages: use MessageComposer, which adds Enter-to-send, attachments and drafts.',
        'Only showing stored HTML that never changes, in a list of many items: render the sanitised HTML directly rather than one editor per row.',
      ],
    },
    designPrinciples: {
      items: [
        'The schema is the sanitiser: only nodes and marks the toolbar can produce survive a paste, so pasted documents cannot smuggle styles, scripts or event handlers in.',
        'Every toolbar action has a keyboard path: Markdown shortcuts while typing, ⌘/Ctrl shortcuts, and the toolbar itself is one Tab stop with arrow keys.',
        'The product owns data: search for mentions, uploads and storage are injected; the editor never fetches.',
        'Quiet chrome: the toolbar uses muted icons with an accent tint for active formatting, so the text stays the focus.',
        'Heavy code stays optional: it lives in the `@rookcom/monarch-react/editor` entry, the only one that imports Tiptap (an optional peer dependency), so apps that do not edit rich text never install or load ProseMirror.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'Renders `.mn-rich-text-editor` with a toolbar row (`__bar`, `__toolbar`), the editing surface (`__content` > `.mn-rich-text-editor__prose`, the ProseMirror element with `role="textbox"`), and an optional counter (`__footer`). `ref` goes to the root; `editorRef` gives the imperative handle.',
        '`aria-label`, `aria-labelledby` and `aria-describedby` are applied to the editing surface, not the root.',
        'The handle (`RichTextEditorHandle`) exposes `focus`, `clear`, `getHTML`, `getJSON`, `getText`, `isEmpty`, `setContent`, `insertText`, `insertContent` and the Tiptap `editor`.',
        'Import from `@rookcom/monarch-react/editor` and install the `@tiptap/*` peer dependencies (2.27.x) listed in the package. Styles are part of `@rookcom/monarch-react/styles.css`.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        '`format="html"` (default) emits an HTML string, and `""` for an empty document. HTML is what most APIs store and render, but it is a lossy, larger format and must be sanitised on the server before rendering.',
        '`format="json"` emits Tiptap JSON. It round-trips losslessly (custom attributes, mention ids), is easy to diff and to transform on the server, but needs Tiptap (or a JSON renderer) to display.',
        'A controlled `value` that differs from what the editor last emitted replaces the document (the caret moves to the end). Echoing back the emitted value does nothing, so typing never jumps.',
        '`onFiles(files, source)` may return `{ type: "image", src, alt }` or `{ type: "link", href, text }` items (or a promise of them) to insert; return nothing when the product shows files elsewhere.',
        '`mentions.search(text, { trigger, signal })` is `MentionSearch` from `@rookcom/monarch-react/collaboration`; the list is its `MentionPicker` driven by `useMentionPicker`, so one product search serves both the textarea and the rich-text pickers.',
        'Changing `headingLevels`, `taskList`, `images`, `maxLength`, the mention trigger or `extensions` rebuilds the editor (the text is kept, undo history is not). The placeholder and ARIA update without a rebuild.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'toolbar',
          values: ['RICH_TEXT_DEFAULT_TOOLBAR', 'a custom list', 'false'],
          description:
            'Which controls appear, in order, with `"|"` for separators. Controls whose feature is off (no headings, no `onFiles`) are left out.',
        },
        {
          prop: 'toolbarPosition',
          values: ['top', 'bottom'],
          description: 'Above the text (documents) or below it (replies and composers).',
        },
        {
          prop: 'format',
          values: ['html', 'json'],
          description: 'The shape of `value`, `defaultValue` and `onValueChange`.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Loading',
          attribute: 'data-loading on the root',
          description:
            'On the server and before hydration: a static placeholder of the same size, marked `aria-busy`.',
        },
        {
          name: 'Focused',
          attribute: ':focus-within',
          description: 'Focus-ring border around the whole box, text or toolbar.',
        },
        {
          name: 'Active formatting',
          attribute: 'aria-pressed="true" on a toolbar button',
          description: 'Accent-tinted button for the marks and blocks at the caret.',
        },
        {
          name: 'Invalid',
          attribute: 'data-invalid, aria-invalid on the textbox',
          description: 'Danger border.',
        },
        {
          name: 'Disabled',
          attribute: 'data-disabled, aria-disabled on the textbox and toolbar',
          description: 'The text stays visible on a subtle surface; nothing can be changed.',
        },
        {
          name: 'Read-only',
          attribute: 'data-readonly',
          description:
            'No border, no toolbar, no textbox role: the document reads like content. Links stay clickable.',
        },
        {
          name: 'Near or over the limit',
          attribute: 'data-state="near" | "over" on the counter',
          description:
            'Warning, then danger colour; the remaining count is announced in a polite status region.',
        },
        {
          name: 'Suggestions open',
          attribute: 'aria-controls / aria-activedescendant on the textbox',
          description: 'A listbox of mention suggestions under the caret.',
        },
      ],
    },
    interaction: {
      items: [
        'Markdown shortcuts while typing: `#`–`####` + space for headings (only the allowed levels), `-` or `*` for bullets, `1.` for numbers, `[ ]` for a checklist, `>` for a quote, three backticks for a code block, and `**bold**`, `*italic*`, `` `code` ``, `~~strike~~` inline.',
        'Toolbar buttons keep the selection (they do not take focus on mouse down) and toggle the mark or block at the caret.',
        'The link button and ⌘/Ctrl+K open a popover with an address field. Bare domains get `https://`, email addresses get `mailto:`; `javascript:` and other schemes are refused with a message. Empty clears the link.',
        'Pasted HTML is cleaned (Office markup, style and script blocks removed) and then parsed against the schema; links keep only http, https, mailto and relative targets. URLs typed or pasted become links automatically.',
        'Pasted or dropped files, and files from the toolbar’s file button, go to `onFiles`; what it returns is inserted at the caret.',
        'Typing the mention trigger (default `@`) searches `mentions.search` after a short pause; the request is aborted when the query changes. Choosing a suggestion inserts a mention chip.',
        'With `maxLength`, input that would pass the limit is refused (whole insertions, not truncated).',
        'Clicking the empty space below the text puts the caret at the end.',
      ],
    },
    keyboard: {
      items: [
        { keys: 'Tab (into the toolbar)', action: 'Focuses the last-used toolbar button.' },
        {
          keys: 'ArrowLeft / ArrowRight (toolbar)',
          action: 'Moves between buttons, wrapping at the ends.',
        },
        { keys: 'Home / End (toolbar)', action: 'First or last button.' },
        { keys: 'Enter / Space (toolbar)', action: 'Runs the button.' },
        { keys: '⌘/Ctrl+B, I, Shift+S, E', action: 'Bold, italic, strikethrough, inline code.' },
        { keys: '⌘/Ctrl+Alt+1–4', action: 'Heading level (when allowed).' },
        {
          keys: '⌘/Ctrl+Shift+7 / 8 / 9',
          action: 'Numbered list, bulleted list, checklist.',
        },
        { keys: '⌘/Ctrl+Shift+B', action: 'Quote.' },
        { keys: '⌘/Ctrl+Alt+C', action: 'Code block.' },
        { keys: '⌘/Ctrl+K', action: 'Opens the link popover (Enter applies, Escape closes).' },
        { keys: '⌘/Ctrl+Z, ⌘/Ctrl+Shift+Z', action: 'Undo and redo.' },
        { keys: 'Tab / Shift+Tab (in a list)', action: 'Indents or outdents the item.' },
        {
          keys: 'ArrowDown / ArrowUp (suggestions)',
          action: 'Moves the highlight, skipping unavailable people.',
        },
        { keys: 'Enter / Tab (suggestions)', action: 'Inserts the highlighted mention.' },
        { keys: 'Escape (suggestions)', action: 'Closes the list and keeps the typed text.' },
      ],
      notes: [
        'Outside lists Tab leaves the editor, so keyboard users are never trapped in the text.',
      ],
    },
    accessibility: {
      role: 'textbox (aria-multiline) with a toolbar; listbox for suggestions',
      name: '`aria-label` or `aria-labelledby` on the component, applied to the textbox. The toolbar is named by `messages.toolbar` (default "Formatting").',
      items: [
        'The toolbar follows the APG toolbar pattern: one Tab stop with a roving tabindex, arrow keys, Home and End; toggles expose `aria-pressed`; groups are split by `role="separator"`.',
        'Every toolbar button has a name that includes its shortcut ("Bold (⌘B)") and a tooltip.',
        'The link field is a labelled input in a popover; focus moves into it and returns to the text when it closes.',
        'Mentions follow the APG combobox pattern: focus stays in the text, which points at the highlighted option with `aria-activedescendant`; loading, empty and error states are announced in a status region.',
        'The counter is linked to the textbox with `aria-describedby`; the remaining count is announced politely only near the limit, not on every keystroke.',
        'Read-only content has no textbox role, so screen readers read it as text; give it an `aria-label` to expose it as a named group.',
        'Disabled sets `aria-disabled`; disabled and read-only both set `contenteditable="false"`.',
      ],
    },
    responsive: {
      items: [
        'The toolbar wraps onto a second row in narrow containers; buttons stay 32px square, so they remain easy to hit at 320px.',
        'The text area grows with its content from `minHeight` (default 6rem) and scrolls past `maxHeight`.',
        'Long words, URLs and code wrap or scroll inside the box instead of widening the page.',
        'The suggestion list stays inside the viewport horizontally.',
      ],
    },
    theme: {
      items: [
        'Only semantic tokens: `--mn-color-bg-surface`, `--mn-color-border-input`, `--mn-color-focus-ring`, `--mn-color-accent-subtle`/`-strong` (active buttons, mentions), `--mn-color-bg-muted` (code), `--mn-color-text-link`. Light, dark, high-contrast and product accents apply automatically.',
        'In forced-colours mode active buttons and mentions get a system-colour outline.',
        'Transitions and the suggestion list animation are removed under `prefers-reduced-motion`.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Document notes',
          stack: true,
          render: () => (
            <div style={{ width: '100%', maxWidth: 560 }}>
              <RichTextEditor
                aria-label="Release notes"
                defaultValue={NOTES}
                placeholder="Write notes…"
              />
            </div>
          ),
        },
        {
          title: 'Controlled with a limit',
          stack: true,
          render: () => (
            <div style={{ width: '100%', maxWidth: 560 }}>
              <ControlledDemo />
            </div>
          ),
        },
        {
          title: 'Mentions and uploads',
          stack: true,
          render: () => (
            <div style={{ width: '100%', maxWidth: 560 }}>
              <RichTextEditor
                aria-label="Comment"
                placeholder="Type @ to mention someone, or paste an image"
                mentions={{ search: searchPeople }}
                onFiles={(files) =>
                  files.map((f) => ({
                    type: 'link' as const,
                    href: `https://files.example.com/${encodeURIComponent(f.name)}`,
                    text: f.name,
                  }))
                }
                headingLevels={[]}
                minHeight="4rem"
              />
            </div>
          ),
        },
        {
          title: 'Read-only and disabled',
          stack: true,
          render: () => (
            <div style={{ display: 'grid', gap: 16, width: '100%', maxWidth: 560 }}>
              <RichTextEditor
                aria-label="Published notes"
                readOnly
                defaultValue="<p>Read-only content keeps its <strong>formatting</strong> and <a href='https://example.com'>links</a>, without a toolbar.</p>"
              />
              <RichTextEditor
                aria-label="Locked description"
                disabled
                minHeight="3rem"
                defaultValue="<p>This description is locked while the review is open.</p>"
                toolbar={['bold', 'italic', 'link']}
              />
            </div>
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Sanitise the HTML on the server before storing or rendering it; the editor never produces unsafe markup, but requests can be forged.',
        'Store JSON when the product transforms documents on the server or needs mention ids; store HTML when it only renders them.',
        'Offer only the formatting the product renders: pass a shorter `toolbar` and `headingLevels={[]}` for comments.',
        'Return inserts from `onFiles` only after the upload has a permanent URL.',
      ],
      dont: [
        'Do not render one editor per row of a long list; render stored HTML and open an editor on demand.',
        'Do not rebuild the editor on every render by passing a new `extensions` array; memoise it.',
        'Do not use `blob:` or `data:` URLs as permanent image sources.',
      ],
    },
    related: {
      items: [
        {
          id: 'message-composer',
          reason: 'The reply box built on this editor, with send and attachments.',
        },
        { id: 'text-field', reason: 'Textarea for plain text with no formatting.' },
        {
          id: 'mention-picker',
          reason: 'The suggestion list used here; also mentions in a plain textarea.',
        },
        { id: 'file-upload', reason: 'Uploads outside the text.' },
      ],
    },
    dependencies: {
      packages: [
        'react',
        '@tiptap/react',
        '@tiptap/starter-kit',
        '@tiptap/extension-link',
        '@tiptap/extension-placeholder',
        '@tiptap/extension-character-count',
        '@tiptap/extension-task-list',
        '@tiptap/extension-task-item',
        '@tiptap/extension-image',
        '@tiptap/extension-mention',
      ],
      components: ['button', 'form-field', 'text-field', 'popover', 'mention-picker'],
      notes: [
        'Button, Field, Input and Popover make the link popover; `useMentionPicker` + `MentionPicker` the suggestion list. Tiptap ^2.27.3 as optional peer dependencies of `@rookcom/monarch-react` (tested with 2.27.3); `@tiptap/core` and `@tiptap/suggestion` are type-level and peer dependencies of the extensions.',
        'Bundle cost, measured with esbuild (minified, react, react-dom and Radix external): `import { RichTextEditor } from "@rookcom/monarch-react/editor"` is about 124 KB min+gzip, of which about 117 KB is ProseMirror and Tiptap core; the rest is the Monarch components it composes, shared with the root entry. Unused Tiptap code (bubble and floating menus, tippy.js) is tree-shaken away, and the root entry contains no Tiptap. Mentions, checklists, images and the counter add about 4 KB and are always bundled with the editor, but only enter the schema when enabled.',
      ],
    },
    testing: {
      unit: [
        'Labelled multi-line textbox; toolbar buttons, names and one Tab stop; arrows, Home and End move focus with wrapping.',
        'Toolbar toggles marks and blocks and reflects `aria-pressed`; Markdown and ⌘/Ctrl shortcuts while typing.',
        'Uncontrolled `defaultValue`, controlled `value` replaced by the product, JSON format in and out, `""` for an empty document.',
        'Read-only (no toolbar, no textbox), disabled (inert toolbar, not editable), placeholder, invalid.',
        'Character counter linked to the textbox, near-limit announcement, refused insertions past `maxLength`.',
        'Link popover from ⌘/Ctrl+K: refuses `javascript:`, normalises a bare domain, adds `rel`.',
        'Paste drops scripts, handlers, styles, Office markup and unsafe links and images.',
        '`onFiles` receives picked files and its image and link inserts land in the document.',
        'Mentions: injected search, listbox with `aria-activedescendant`, disabled people skipped, Enter inserts a mention node, Escape dismisses.',
        '`onEditorKeyDown` runs before the editor (Enter suppressed); hidden form input for `name`.',
      ],
      axe: true,
      ssr: true,
      browser: [
        'Visual baselines: /__gallery/rich-text-editor',
        'Cross-browser axe: /components/rich-text-editor',
      ],
    },
  },
} satisfies ComponentDocs;
