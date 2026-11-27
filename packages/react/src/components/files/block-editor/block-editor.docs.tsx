import { useState, type ComponentProps } from 'react';
import { BlockEditor, PageOutline } from '@rookcom/monarch-react/editor';
import type { ComponentDocs } from '../../../internal/component-docs.js';
// The scenario of the primary example, shared with iOS and Android (CROSS-PLATFORM.md section 8).
import fixture from './block-editor.fixture.json';

type FixtureBlock = (typeof fixture.data.blocks)[number];

/** The marks of a text: `code` and `bold` name the part of `text` to mark up. */
function inline(text: string, marks: { code?: string; bold?: string }): string {
  let out = text;
  if (marks.code) out = out.replace(marks.code, `<code>${marks.code}</code>`);
  if (marks.bold) out = out.replace(marks.bold, `<strong>${marks.bold}</strong>`);
  return out;
}

/** The document of the fixture as the editor's HTML. Consecutive tasks share one list. */
function toHtml(blocks: FixtureBlock[]): string {
  const parts: string[] = [];
  let tasks: string[] = [];
  const flush = () => {
    if (tasks.length) parts.push(`<ul data-type="taskList">${tasks.join('')}</ul>`);
    tasks = [];
  };
  for (const block of blocks) {
    if (block.type === 'task') {
      tasks.push(
        `<li data-type="taskItem" data-checked="${String(block.checked)}"><p>${block.text}</p></li>`,
      );
      continue;
    }
    flush();
    if (block.type === 'heading') parts.push(`<h${block.level}>${block.text}</h${block.level}>`);
    else if (block.type === 'paragraph')
      parts.push(`<p>${inline(block.text ?? '', { code: block.code as string })}</p>`);
    else if (block.type === 'callout')
      parts.push(`<div data-type="callout" data-tone="${block.tone}"><p>${block.text}</p></div>`);
    else if (block.type === 'columns')
      parts.push(
        `<div data-type="columns" data-ratio="0.5">${(block.columns ?? [])
          .map(
            (column) =>
              `<div data-type="column"><p>${inline(column.text, { bold: column.bold })}</p></div>`,
          )
          .join('')}</div>`,
      );
    else if (block.type === 'table') {
      const [head = [], ...body] = block.rows ?? [];
      const row = (cells: string[], tag: string) =>
        `<tr>${cells.map((cell) => `<${tag}><p>${cell}</p></${tag}>`).join('')}</tr>`;
      parts.push(
        `<table><tbody>${row(head, 'th')}${body.map((cells) => row(cells, 'td')).join('')}</tbody></table>`,
      );
    } else if (block.type === 'toggle')
      parts.push(
        `<div data-type="toggle-block" data-open="${String(block.open)}"><div data-type="toggle-summary">${block.text}</div><div data-type="toggle-content"><p>${block.body}</p></div></div>`,
      );
  }
  flush();
  return parts.join('');
}

const DOC = toHtml(fixture.data.blocks);

function WithOutline() {
  const [editor, setEditor] = useState<ComponentProps<typeof PageOutline>['editor']>(null);
  return (
    <div
      style={{
        display: 'grid',
        gridTemplateColumns: 'minmax(0, 1fr) 12rem',
        gap: 24,
        width: '100%',
      }}
    >
      <BlockEditor aria-label={fixture.data.label} defaultValue={DOC} onEditorReady={setEditor} />
      <PageOutline editor={editor} showStats messages={{ title: fixture.data.outline.title }} />
    </div>
  );
}

function JsonDemo() {
  const [json, setJson] = useState('');
  return (
    <div style={{ display: 'grid', gap: 8, width: '100%' }}>
      <BlockEditor
        aria-label="Notes"
        format="json"
        blocks={['callout', 'toggle']}
        toolbar={['bold', 'italic', 'link', '|', 'bulletList', 'taskList']}
        defaultValue="<p>Only callouts and toggles are offered here.</p>"
        onValueChange={(v) => setJson(JSON.stringify(v))}
      />
      <code style={{ fontSize: 12, overflowWrap: 'anywhere' }}>{json || '(edit to see JSON)'}</code>
    </div>
  );
}

export default {
  id: 'block-editor',
  title: 'BlockEditor',
  summary:
    'A Notion-style document editor on Tiptap: blocks from a / menu, drag handles, a formatting toolbar and a selection menu, with HTML or JSON output.',
  sections: {
    purpose: {
      summary:
        'Edits whole documents built from blocks (text, headings, lists, checklists, quotes, code, images, callouts, toggles, columns, tables, embeds) and hands the product HTML or JSON. Needed by: Rook Dossier (src/components/document/DocumentEditor.tsx with the kit in src/lib/tiptap/kit.ts, blocks.ts, extensions.ts, slashSuggestion.ts, slashCommands.ts and the node views in src/components/document/nodeViews/); Rook Tickets’ RichEditor (src/components/tickets/detail/RichEditor.tsx) for long descriptions.',
      useWhen: [
        'People write and arrange long documents: wiki pages, briefs, specs, help articles.',
        'Layout blocks (callouts, toggles, columns, tables, embeds) belong in the document itself.',
      ],
      avoidWhen: [
        'Comments, replies and descriptions: use RichTextEditor or MessageComposer, which are lighter.',
        'Plain text: use Textarea.',
      ],
    },
    designPrinciples: {
      items: [
        'Built on RichTextEditor, so paste cleaning, safe links, mentions, uploads, controlled values and server rendering behave exactly the same.',
        'Every block is its own Monarch component and Tiptap extension (CalloutBlock, ToggleBlock, ColumnsBlock, TableBlock, EmbedBlock); BlockEditor only wires them together, and products can use them in any Tiptap editor.',
        'Every pointer action has a keyboard path: the / menu, Alt+Shift+arrows to move blocks, a block-actions menu on the handle, Alt+F10 for the selection menu.',
        'Stored HTML stays safe: embeds save links, never frames; unknown attributes are dropped by the schema.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'Renders `.mn-block-editor` > `__toolbar` (EditorToolbar), `__body` (the RichTextEditor surface and `__handle-wrap` > `__handle`), the slash menu (`__slash`, a SlashMenu) and a BubbleMenu.',
        '`editorRef` is the RichTextEditor handle (`getHTML`, `getJSON`, `setContent`, `focus`, …); `onEditorReady` gives the Tiptap editor, e.g. for PageOutline.',
        'Import from `@rookcom/monarch-react/editor`.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        '`blocks` picks the custom blocks (`callout`, `toggle`, `columns`, `table`, `embed`, `image`); `image` also needs `onFiles`.',
        '`slashItems` adds product entries: a SlashMenuItem plus `run(editor)` and optional `isAvailable(editor)`.',
        '`format`, `value`, `defaultValue`, `onValueChange`, `mentions`, `maxLength` and `name` are RichTextEditor’s.',
        'Changing `blocks`, `headingLevels`, `embedProviders` or `extensions` rebuilds the editor (the text is kept, undo history is not).',
        'Migrating from Dossier’s kit: node names differ (`calloutBlock` → `callout`, `embedBlock` → `embed`, `toggleBlock` keeps its name with `data-type` HTML); convert stored JSON once, or map the class-based HTML with the parse rules described on each block’s page.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'toolbar',
          values: ['EDITOR_TOOLBAR_DEFAULT_ITEMS', 'a custom list', 'false'],
          description: 'Formatting controls above the document.',
        },
        { prop: 'format', values: ['html', 'json'], description: 'Shape of the value.' },
        {
          prop: 'bubbleMenu / dragHandles / slashMenu',
          values: ['true', 'false'],
          description: 'Turn each aid on or off.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Loading',
          attribute: 'data-loading on the inner editor',
          description: 'On the server and before hydration, a static placeholder.',
        },
        {
          name: 'Slash menu open',
          attribute: 'aria-controls / aria-activedescendant on the textbox',
          description: 'The block menu under the caret.',
        },
        {
          name: 'Block handle',
          description: 'Shown beside the hovered block, or the block with the caret.',
        },
        {
          name: 'Read-only',
          attribute: 'data-readonly',
          description: 'No toolbar, handles or menus; blocks render as content.',
        },
        {
          name: 'Disabled',
          attribute: 'data-disabled',
          description: 'Not editable; the document stays visible.',
        },
      ],
    },
    interaction: {
      items: [
        'Typing `/` at the start of a line or after a space opens the block menu; typing filters it; Enter, Tab or a click inserts the block in place of the typed text.',
        'Dragging a block’s handle moves the block; dropping shows ProseMirror’s drop cursor.',
        'Clicking the handle opens Move up, Move down, Duplicate and Delete. Delete is undone with ⌘/Ctrl+Z.',
        'Selecting text shows the selection menu with inline formatting and links.',
        'Markdown shortcuts work as in RichTextEditor (`#`, `-`, `1.`, `[ ]`, `>`, three backticks).',
      ],
    },
    keyboard: {
      items: [
        { keys: '/', action: 'Opens the block menu.' },
        {
          keys: 'ArrowUp / ArrowDown, Enter or Tab, Escape (block menu)',
          action: 'Move, insert, close.',
        },
        { keys: 'Alt+Shift+ArrowUp / ArrowDown', action: 'Moves the current block up or down.' },
        {
          keys: 'Tab (from the text)',
          action: 'Reaches the current block’s handle; Enter opens its actions.',
        },
        { keys: '⌘/Ctrl+K', action: 'Opens the link form in the toolbar.' },
        {
          keys: 'Alt+F10',
          action:
            'Moves focus to the selection menu (text selected) or the table controls (in a table).',
        },
        { keys: 'Tab / Shift+Tab (in a table)', action: 'Next or previous cell.' },
      ],
    },
    accessibility: {
      role: 'textbox (aria-multiline) with a toolbar, a listbox for the block menu and a menu for block actions',
      name: '`aria-label` or `aria-labelledby`, applied to the textbox. The handle is named "Block actions: <block type>".',
      items: [
        'The block menu follows the APG combobox pattern: focus stays in the text and the highlighted option is `aria-activedescendant`.',
        'Drag and drop is never the only way: moving, duplicating and deleting are in the handle menu and on shortcuts.',
        'Each block keeps its own semantics (disclosure buttons, window splitter, table headers, framed content titles).',
      ],
    },
    responsive: {
      items: [
        'The handle gutter narrows on small screens; handles and menu items are 44px on coarse pointers.',
        'Columns stack, tables and diffs scroll sideways, the block menu stays inside the viewport at 390px.',
        'The toolbar sticks to the top of the scrolling area and wraps.',
      ],
    },
    theme: {
      items: [
        'Borderless document surface on `--mn-color-bg-surface`; toolbar border `--mn-color-border-subtle`; handle `--mn-color-text-subtle` on `--mn-color-bg-muted`.',
        'Each block uses its own tokens; forced colours and reduced motion are handled per block.',
      ],
    },
    examples: {
      items: [
        { title: 'Document with outline', stack: true, render: () => <WithOutline /> },
        { title: 'Fewer blocks, JSON output', stack: true, render: () => <JsonDemo /> },
        {
          title: 'Read-only',
          stack: true,
          render: () => (
            <div style={{ width: '100%', maxWidth: 640 }}>
              <BlockEditor aria-label="Published brief" readOnly defaultValue={DOC} />
            </div>
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Sanitise stored HTML on the server, or store JSON.',
        'Offer only the blocks your renderer supports (`blocks`).',
        'Pair it with PageOutline for long documents.',
      ],
      dont: [
        'Do not use it for short comments.',
        'Do not add `@tiptap/extension-table` alongside it; tables are built in.',
        'Do not pass a new `extensions` array on every render.',
      ],
    },
    related: {
      items: [
        {
          id: 'rich-text-editor',
          reason: 'The editor it builds on; lighter for comments and descriptions.',
        },
        { id: 'slash-menu', reason: 'The block menu it shows.' },
        { id: 'editor-toolbar', reason: 'The formatting toolbar.' },
        { id: 'bubble-menu', reason: 'The selection menu.' },
        { id: 'page-outline', reason: 'Headings and scroll-spy beside the document.' },
        { id: 'diff-viewer', reason: 'Compare versions of a document.' },
      ],
    },
    dependencies: {
      packages: ['react', '@tiptap/core', '@tiptap/pm', '@tiptap/suggestion'],
      components: [
        'rich-text-editor',
        'slash-menu',
        'dropdown-menu',
        'editor-toolbar',
        'bubble-menu',
        'callout-block',
        'toggle-block',
        'columns-block',
        'table-block',
        'embed-block',
      ],
      notes: [
        'All Tiptap packages are optional peer dependencies of `@rookcom/monarch-react`; nothing beyond them is needed.',
      ],
    },
    testing: {
      unit: [
        'Labelled textbox, toolbar and placeholder.',
        'Slash menu: opens on /, filters, ARIA on the textbox, arrows, Enter inserts, Escape closes and keeps the text.',
        'Inserts callout, toggle, columns, table and embed from the menu; `blocks` limits the menu; product slash items run.',
        'Moves blocks with Alt+Shift+arrows; handle menu duplicates and deletes.',
        'Toolbar formatting with JSON output; read-only hides every aid.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
