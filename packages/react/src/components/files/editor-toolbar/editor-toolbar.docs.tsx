import { EditorContent, useEditor } from '@tiptap/react';
import StarterKit from '@tiptap/starter-kit';
import {
  EDITOR_TOOLBAR_INLINE_ITEMS,
  EditorToolbar,
  type EditorToolbarItem,
} from '@rookcom/monarch-react/editor';
import type { ComponentDocs } from '../../../internal/component-docs.js';

const DOC = `<h2>Meeting notes</h2><p>Agree the <strong>release date</strong> and who owns the help article.</p><ul><li><p>Draft the announcement</p></li><li><p>Review the checklist</p></li></ul>`;

function Demo({ items, disabled }: { items?: readonly EditorToolbarItem[]; disabled?: boolean }) {
  const editor = useEditor({
    immediatelyRender: false,
    extensions: [StarterKit.configure({ link: { openOnClick: false } })],
    content: DOC,
    editorProps: { attributes: { 'aria-label': 'Meeting notes', role: 'textbox' } },
  });
  return (
    <div
      style={{
        display: 'grid',
        gap: 8,
        boxSizing: 'border-box',
        width: '100%',
        maxWidth: 640,
        padding: 8,
        border: '1px solid var(--mn-color-border-default)',
        borderRadius: 8,
      }}
    >
      <EditorToolbar editor={editor} items={items} disabled={disabled} />
      <EditorContent editor={editor} />
    </div>
  );
}

const highlight: EditorToolbarItem = {
  id: 'clear',
  label: 'Clear formatting',
  // An icon, not text: a visible "Tx" would have to be part of the button's name.
  icon: (
    <svg
      width={16}
      height={16}
      viewBox="0 0 16 16"
      fill="none"
      stroke="currentColor"
      strokeWidth={1.75}
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
      focusable="false"
    >
      <path d="M3 4.5V3h9v1.5M7.5 3 5 13M3.5 13h3M10 10l3.5 3.5M13.5 10 10 13.5" />
    </svg>
  ),
  isDisabled: (editor) => editor.state.selection.empty,
  run: (editor) => editor.chain().focus().unsetAllMarks().clearNodes().run(),
};

export default {
  id: 'editor-toolbar',
  title: 'EditorToolbar',
  summary:
    'A formatting toolbar for any Tiptap editor: undo, text styles, headings, lists, quotes, code and links, with the APG toolbar keyboard.',
  sections: {
    purpose: {
      summary:
        'Gives a Tiptap editor an accessible, token-themed formatting toolbar that reflects the formatting at the caret and accepts product controls. Needed by: Rook Dossier (document editor top bar) and Rook Tickets (`src/components/tickets/detail/RichEditor.tsx`, a hand-made `role="toolbar"` row).',
      useWhen: [
        'The product builds its own Tiptap editor (BlockEditor, a custom document view) and needs formatting controls.',
        'A product action belongs next to the formatting buttons, such as "Insert callout" or "Clear formatting".',
        'The selection toolbar of a BubbleMenu needs a different set of controls.',
      ],
      avoidWhen: [
        'Use RichTextEditor when a ready-made editor with its own toolbar is enough.',
        'Use Toolbar for actions that are not about formatting text (filters, bulk actions).',
      ],
    },
    designPrinciples: {
      items: [
        'One keyboard pattern: the APG toolbar (one Tab stop, arrow keys, Home and End), the same as every Monarch toolbar.',
        'The editor stays in charge: buttons do not take focus on mouse down, so the selection is kept and the command applies to it.',
        'Only what the schema allows: a control whose node or mark is not in the editor is left out instead of failing silently.',
        'Quiet chrome: muted icons with an accent tint for active formatting, so the text stays the focus.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'Renders `.mn-editor-toolbar` (`role="toolbar"`) with `.mn-editor-toolbar__tool` buttons and `.mn-editor-toolbar__separator` between groups. `ref` goes to the root.',
        'Import from `@rookcom/monarch-react/editor`; it needs the `@tiptap/*` peer dependencies.',
        '`EDITOR_TOOLBAR_DEFAULT_ITEMS` holds every built-in control; `EDITOR_TOOLBAR_INLINE_ITEMS` only the inline marks and link, for a selection toolbar.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        '`items` mixes built-in ids (`undo`, `redo`, `paragraph`, `heading`, `bold`, `italic`, `strike`, `code`, `link`, `bulletList`, `orderedList`, `taskList`, `blockquote`, `codeBlock`), `"|"` separators and custom `{ id, label, icon, run, isActive?, isDisabled? }` objects.',
        '`heading` expands to one button per entry of `headingLevels` (default `[1, 2, 3]`).',
        '`linkOpen` / `onLinkOpenChange` control the link form, so a ⌘/Ctrl+K shortcut in the editor can open it.',
        'Migrating from a hand-made toolbar (`ToolbarButton active={editor.isActive("bold")} onClick={…}`): pass the editor and the list of ids; `label` becomes `messages`, `active` and `onClick` are derived. Custom buttons become custom items.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'items',
          values: ['EDITOR_TOOLBAR_DEFAULT_ITEMS', 'EDITOR_TOOLBAR_INLINE_ITEMS', 'a custom list'],
          description: 'Which controls appear, in order, with `"|"` for separators.',
        },
      ],
      notes: ['There are no visual variants; place it in a bar, a panel or a BubbleMenu.'],
    },
    states: {
      items: [
        {
          name: 'Loading',
          attribute: 'data-loading',
          description:
            'Before the editor exists (server, first render) the buttons render inert with `aria-disabled`.',
        },
        {
          name: 'Active formatting',
          attribute: 'aria-pressed="true"',
          description: 'Accent-tinted button for the marks and blocks at the caret.',
        },
        {
          name: 'Unavailable control',
          attribute: 'aria-disabled="true" on a tool',
          description:
            'Undo with nothing to undo, or a custom item whose `isDisabled` returns true. Stays focusable.',
        },
        {
          name: 'Disabled',
          attribute: 'data-disabled, aria-disabled on the toolbar',
          description: 'Every control is inert.',
        },
      ],
    },
    interaction: {
      items: [
        'Clicking a button applies the command to the editor selection and puts focus back in the text.',
        'Pressed states follow the caret on every editor transaction.',
        'The link button opens a popover with an address field: bare domains get `https://`, email addresses `mailto:`, unsafe schemes are refused; an empty field removes the link.',
        'Tooltips show the keyboard shortcut once the platform is known (after mount), e.g. "Bold (⌘B)".',
      ],
    },
    keyboard: {
      items: [
        { keys: 'Tab', action: 'Enters the toolbar on the last-used button; Tab again leaves it.' },
        {
          keys: 'ArrowRight / ArrowLeft',
          action: 'Next or previous button, wrapping (mirrored in right-to-left).',
        },
        { keys: 'Home / End', action: 'First or last button.' },
        { keys: 'Enter / Space', action: 'Runs the button.' },
        { keys: 'Enter (link form)', action: 'Applies the link.' },
        { keys: 'Escape (link form)', action: 'Closes the form and returns to the text.' },
      ],
      notes: [
        'The editor shortcuts (⌘/Ctrl+B and so on) belong to the Tiptap extensions; the toolbar announces them through `aria-keyshortcuts`.',
      ],
    },
    accessibility: {
      role: 'toolbar',
      name: '`aria-label`, or `messages.label` (default "Formatting").',
      items: [
        'APG toolbar: roving tabindex, `aria-orientation="horizontal"`, groups split by `role="separator"`.',
        'Toggles expose `aria-pressed`; plain actions (undo, redo, custom items without `isActive`) do not.',
        'Every button is icon-only with an `aria-label` and a `title`; shortcuts are exposed in `aria-keyshortcuts`.',
        'Unavailable buttons use `aria-disabled` and stay focusable, so keyboard users can still discover them.',
        'The link field is a labelled input with an error message; focus returns to the text when the popover closes.',
      ],
    },
    responsive: {
      items: [
        'Buttons wrap onto further rows in narrow containers; nothing scrolls the page sideways at 390px.',
        'On coarse pointers buttons grow to 44px squares.',
        'The link popover is never wider than the viewport minus 2rem.',
      ],
    },
    theme: {
      items: [
        'Tokens: `--mn-color-text-muted`, `--mn-color-bg-muted` (hover), `--mn-color-accent-subtle`/`-strong` (pressed), `--mn-color-focus-ring`, `--mn-color-border-subtle` (separators).',
        'Forced colours: pressed buttons get a ButtonText outline, unavailable ones GrayText.',
        'Hover transitions are removed under `prefers-reduced-motion`.',
      ],
    },
    examples: {
      items: [
        { title: 'Every control', stack: true, render: () => <Demo /> },
        {
          title: 'Inline marks only',
          stack: true,
          render: () => <Demo items={EDITOR_TOOLBAR_INLINE_ITEMS} />,
        },
        {
          title: 'With a custom control',
          stack: true,
          render: () => <Demo items={['bold', 'italic', 'link', '|', 'heading', '|', highlight]} />,
        },
        {
          title: 'Disabled',
          stack: true,
          render: () => <Demo disabled items={['bold', 'italic']} />,
        },
      ],
    },
    doDont: {
      do: [
        'Offer only the formatting the product can render and store.',
        'Give custom items a clear `label`; it is the button’s only name.',
        'Use `isDisabled` for commands the current selection cannot run, rather than hiding them.',
      ],
      dont: [
        'Do not put non-formatting actions such as Save or Share in the formatting toolbar.',
        'Do not render two formatting toolbars for one editor in the same view.',
      ],
    },
    related: {
      items: [
        { id: 'rich-text-editor', reason: 'A complete editor with this kind of toolbar built in.' },
        { id: 'bubble-menu', reason: 'Shows a toolbar over the selected text.' },
        { id: 'block-editor', reason: 'The block editor that uses this toolbar.' },
        { id: 'toolbar', reason: 'A general toolbar for non-editor actions.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: ['button', 'form-field', 'text-field', 'popover'],
      notes: [
        'Button, Field, Input and Popover make the link form. `@tiptap/core` is type-only; the editor comes from the product.',
      ],
    },
    testing: {
      unit: [
        'Named toolbar with every default control, separators and one Tab stop; controls missing from the schema are skipped.',
        'Arrow keys, Home and End with wrapping; arrows reverse in right-to-left.',
        'Marks and blocks toggle and `aria-pressed` follows the selection.',
        'Undo is unavailable with no history, then undo and redo work.',
        'Custom items run, read `isActive` and `isDisabled`.',
        'Link popover refuses `javascript:`, normalises a bare domain; `linkOpen` controls it.',
        'Disabled toolbar is inert; messages translate names; shortcuts appear after mount.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
