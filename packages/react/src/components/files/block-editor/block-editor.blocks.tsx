import { Extension, type AnyExtension, type Editor, type Range } from '@tiptap/core';
import type { Node as PMNode } from '@tiptap/pm/model';
import { NodeSelection, Plugin, PluginKey, TextSelection } from '@tiptap/pm/state';
import Suggestion, { type SuggestionKeyDownProps, type SuggestionProps } from '@tiptap/suggestion';
import type { SlashMenuItem } from '../../collaboration/slash-menu/index.js';
import { CalloutBlockExtension } from '../callout-block/index.js';
import { ColumnsBlockExtension } from '../columns-block/index.js';
import { EmbedBlockExtension, type EmbedProvider } from '../embed-block/index.js';
import { TableBlockExtension } from '../table-block/index.js';
import { ToggleBlockExtension } from '../toggle-block/index.js';
import {
  BulletListGlyph,
  CodeBlockGlyph,
  HeadingGlyph,
  OrderedListGlyph,
  QuoteGlyph,
  TaskListGlyph,
} from '../../../internal/editor-glyphs.js';
import { ColumnsGlyph, InfoGlyph, ChevronRightGlyph, PlusGlyph } from '../../../internal/icons.js';

/** The custom blocks a BlockEditor can offer besides text, headings, lists, quotes and code. */
export type BlockEditorBlock = 'callout' | 'toggle' | 'columns' | 'table' | 'embed' | 'image';

/** Every custom block, the default for `blocks`. */
export const BLOCK_EDITOR_BLOCKS: readonly BlockEditorBlock[] = [
  'callout',
  'toggle',
  'columns',
  'table',
  'embed',
  'image',
];

/** One entry of the slash menu: a SlashMenuItem plus what it does. */
export interface BlockEditorSlashItem extends SlashMenuItem {
  /** Runs after the typed `/query` has been removed. */
  run: (editor: Editor) => void;
  /** Hide the item when this returns false (e.g. the schema lacks the node). */
  isAvailable?: ((editor: Editor) => boolean) | undefined;
}

/** Labels and descriptions of the built-in slash items. */
export interface BlockEditorSlashMessages {
  groupText: string;
  groupLists: string;
  groupBlocks: string;
  groupMedia: string;
  text: [string, string];
  heading: (level: number) => [string, string];
  bulletList: [string, string];
  orderedList: [string, string];
  taskList: [string, string];
  quote: [string, string];
  code: [string, string];
  callout: [string, string];
  toggle: [string, string];
  columns2: [string, string];
  columns3: [string, string];
  table: [string, string];
  embed: [string, string];
  image: [string, string];
}

export const DEFAULT_BLOCK_EDITOR_SLASH_MESSAGES: BlockEditorSlashMessages = {
  groupText: 'Text',
  groupLists: 'Lists',
  groupBlocks: 'Blocks',
  groupMedia: 'Media',
  text: ['Text', 'Plain paragraph'],
  heading: (level) => [
    `Heading ${level}`,
    level === 1 ? 'Page title' : level === 2 ? 'Section heading' : 'Subsection heading',
  ],
  bulletList: ['Bulleted list', 'A simple list'],
  orderedList: ['Numbered list', 'A list in order'],
  taskList: ['Checklist', 'Items to tick off'],
  quote: ['Quote', 'A quotation'],
  code: ['Code block', 'Monospaced code'],
  callout: ['Callout', 'A highlighted note'],
  toggle: ['Toggle', 'A section that can be collapsed'],
  columns2: ['Two columns', 'Blocks side by side'],
  columns3: ['Three columns', 'Blocks in three columns'],
  table: ['Table', 'Rows and columns'],
  embed: ['Embed', 'A video, design or demo from a link'],
  image: ['Image', 'Upload a picture'],
};

const has = (editor: Editor, name: string) => Boolean(editor.schema.nodes[name]);

/** The built-in slash items for the blocks that are enabled. */
export function buildSlashItems(
  m: BlockEditorSlashMessages,
  headingLevels: readonly number[],
  actions: { pickImage: (() => void) | undefined },
): BlockEditorSlashItem[] {
  const item = (
    id: string,
    [label, description]: [string, string],
    group: string,
    icon: SlashMenuItem['icon'],
    run: (e: Editor) => void,
    isAvailable?: (e: Editor) => boolean,
    keywords?: string[],
  ): BlockEditorSlashItem => ({ id, label, description, group, icon, run, isAvailable, keywords });
  const out: BlockEditorSlashItem[] = [
    item(
      'text',
      m.text,
      m.groupText,
      <span aria-hidden>¶</span>,
      (e) => e.chain().focus().setParagraph().run(),
      undefined,
      ['paragraph', 'p'],
    ),
    ...headingLevels.map((level) =>
      item(
        `heading-${level}`,
        m.heading(level),
        m.groupText,
        <HeadingGlyph level={level} />,
        (e) =>
          e
            .chain()
            .focus()
            .setHeading({ level: level as 1 | 2 | 3 | 4 })
            .run(),
        (e) => has(e, 'heading'),
        [`h${level}`, 'title'],
      ),
    ),
    item(
      'quote',
      m.quote,
      m.groupText,
      <QuoteGlyph />,
      (e) => e.chain().focus().setBlockquote().run(),
      (e) => has(e, 'blockquote'),
      ['blockquote'],
    ),
    item(
      'code',
      m.code,
      m.groupText,
      <CodeBlockGlyph />,
      (e) => e.chain().focus().setCodeBlock().run(),
      (e) => has(e, 'codeBlock'),
      ['pre'],
    ),
    item(
      'bullet-list',
      m.bulletList,
      m.groupLists,
      <BulletListGlyph />,
      (e) => e.chain().focus().toggleBulletList().run(),
      (e) => has(e, 'bulletList'),
      ['ul'],
    ),
    item(
      'ordered-list',
      m.orderedList,
      m.groupLists,
      <OrderedListGlyph />,
      (e) => e.chain().focus().toggleOrderedList().run(),
      (e) => has(e, 'orderedList'),
      ['ol'],
    ),
    item(
      'task-list',
      m.taskList,
      m.groupLists,
      <TaskListGlyph />,
      (e) => e.chain().focus().toggleTaskList().run(),
      (e) => has(e, 'taskList'),
      ['todo', 'checkbox'],
    ),
    item(
      'callout',
      m.callout,
      m.groupBlocks,
      <InfoGlyph />,
      (e) => e.chain().focus().setCallout().run(),
      (e) => has(e, 'callout'),
      ['note', 'tip', 'warning'],
    ),
    item(
      'toggle',
      m.toggle,
      m.groupBlocks,
      <ChevronRightGlyph />,
      (e) => e.chain().focus().setToggleBlock().run(),
      (e) => has(e, 'toggleBlock'),
      ['details', 'collapse'],
    ),
    item(
      'columns-2',
      m.columns2,
      m.groupBlocks,
      <ColumnsGlyph />,
      (e) => e.chain().focus().insertColumns({ count: 2 }).run(),
      (e) => has(e, 'columns'),
      ['layout'],
    ),
    item(
      'columns-3',
      m.columns3,
      m.groupBlocks,
      <ColumnsGlyph />,
      (e) => e.chain().focus().insertColumns({ count: 3 }).run(),
      (e) => has(e, 'columns'),
      ['layout'],
    ),
    item(
      'table',
      m.table,
      m.groupBlocks,
      <ColumnsGlyph />,
      (e) => e.chain().focus().insertTable().run(),
      (e) => has(e, 'table'),
      ['grid'],
    ),
    item(
      'embed',
      m.embed,
      m.groupMedia,
      <PlusGlyph />,
      (e) => e.chain().focus().insertEmbed().run(),
      (e) => has(e, 'embed'),
      ['video', 'youtube', 'figma', 'iframe'],
    ),
  ];
  if (actions.pickImage) {
    const pick = actions.pickImage;
    out.push(
      item(
        'image',
        m.image,
        m.groupMedia,
        <PlusGlyph />,
        () => pick(),
        (e) => has(e, 'image'),
        ['picture', 'photo', 'upload'],
      ),
    );
  }
  return out;
}

/** The Tiptap extensions for the enabled custom blocks. */
export function blockExtensions(
  blocks: readonly BlockEditorBlock[],
  embedProviders: readonly EmbedProvider[] | undefined,
): AnyExtension[] {
  const out: AnyExtension[] = [];
  if (blocks.includes('callout')) out.push(CalloutBlockExtension);
  if (blocks.includes('toggle')) out.push(ToggleBlockExtension);
  if (blocks.includes('columns')) out.push(ColumnsBlockExtension);
  if (blocks.includes('table')) out.push(TableBlockExtension);
  if (blocks.includes('embed'))
    out.push(
      embedProviders
        ? EmbedBlockExtension.configure({ providers: embedProviders })
        : EmbedBlockExtension,
    );
  return out;
}

/* ------------------------------------------------------------- moving blocks */

/** Position of the top-level block around the selection, or null. */
export function currentBlockPos(editor: Editor): number | null {
  const { selection } = editor.state;
  if (selection instanceof NodeSelection && selection.$from.depth === 0) return selection.from;
  const { $from } = selection;
  return $from.depth >= 1 ? $from.before(1) : null;
}

/** Moves the top-level block at `pos` one place up or down. Returns the new position, or null. */
export function moveBlock(editor: Editor, pos: number, dir: -1 | 1): number | null {
  const { doc, tr } = editor.state;
  const $pos = doc.resolve(pos);
  if ($pos.depth !== 0) return null;
  const index = $pos.index(0);
  const node = doc.child(index);
  const neighbour = doc.maybeChild(index + dir);
  if (!neighbour) return null;
  const inner = editor.state.selection.from - pos;
  tr.delete(pos, pos + node.nodeSize);
  const target = dir === -1 ? pos - neighbour.nodeSize : pos + neighbour.nodeSize;
  tr.insert(target, node);
  const caret = Math.min(Math.max(target + inner, target + 1), target + node.nodeSize - 1);
  tr.setSelection(TextSelection.near(tr.doc.resolve(caret)));
  editor.view.dispatch(tr.scrollIntoView());
  return target;
}

/** Inserts a copy of the top-level block at `pos` after it. */
export function duplicateBlock(editor: Editor, pos: number): void {
  const node: PMNode | null = editor.state.doc.nodeAt(pos);
  if (!node) return;
  const tr = editor.state.tr.insert(pos + node.nodeSize, node.copy(node.content));
  tr.setSelection(TextSelection.near(tr.doc.resolve(pos + node.nodeSize + 1)));
  editor.view.dispatch(tr.scrollIntoView());
}

/** Deletes the top-level block at `pos` (undo restores it). */
export function deleteBlock(editor: Editor, pos: number): void {
  const node = editor.state.doc.nodeAt(pos);
  if (!node) return;
  const tr = editor.state.tr.delete(pos, pos + node.nodeSize);
  if (!tr.doc.childCount) {
    const p = editor.schema.nodes.paragraph;
    if (p) tr.insert(0, p.create());
  }
  tr.setSelection(TextSelection.near(tr.doc.resolve(Math.min(pos, tr.doc.content.size))));
  editor.view.dispatch(tr.scrollIntoView());
}

/* ------------------------------------------------------------- bridge */

/** What the slash suggestion reports to React. */
export interface SlashState {
  query: string;
  range: Range;
  rect: { top: number; left: number; bottom: number } | null;
}

export interface BridgeCallbacks {
  onEditor: (editor: Editor | null) => void;
  onSlash: (state: SlashState | null) => void;
  onSlashKey: (event: KeyboardEvent) => boolean;
  onLinkShortcut: () => boolean;
  attributes: () => Record<string, string>;
  slashEnabled: () => boolean;
}

export const BLOCK_SLASH_KEY = new PluginKey('mnBlockEditorSlash');

/**
 * Connects the BlockEditor to its Tiptap editor: reports the editor
 * instance, runs the `/` suggestion, adds block shortcuts and lets React put
 * ARIA on the editing surface.
 */
export function createBridge(cb: { current: BridgeCallbacks }) {
  return Extension.create({
    name: 'mnBlockEditorBridge',
    priority: 200,
    onCreate() {
      cb.current.onEditor(this.editor);
    },
    onDestroy() {
      cb.current.onEditor(null);
    },
    addKeyboardShortcuts() {
      const move = (dir: -1 | 1) => () => {
        const pos = currentBlockPos(this.editor);
        return pos === null ? false : moveBlock(this.editor, pos, dir) !== null;
      };
      return {
        'Alt-Shift-ArrowUp': move(-1),
        'Alt-Shift-ArrowDown': move(1),
        'Mod-k': () => cb.current.onLinkShortcut(),
      };
    },
    addProseMirrorPlugins() {
      const report = (p: SuggestionProps) => {
        const r = p.clientRect?.();
        cb.current.onSlash({
          query: p.query,
          range: p.range,
          rect: r ? { top: r.top, left: r.left, bottom: r.bottom } : null,
        });
      };
      const suggestion = Suggestion({
        editor: this.editor,
        pluginKey: BLOCK_SLASH_KEY,
        char: '/',
        allowSpaces: false,
        allow: ({ state, range }) => {
          if (!cb.current.slashEnabled()) return false;
          const $from = state.doc.resolve(range.from);
          return $from.parent.type.name !== 'codeBlock';
        },
        items: () => [],
        render: () => ({
          onStart: report,
          onUpdate: report,
          onKeyDown: ({ event }: SuggestionKeyDownProps) => cb.current.onSlashKey(event),
          onExit: () => cb.current.onSlash(null),
        }),
      });
      return [
        suggestion,
        // ARIA for the open menu (aria-controls, aria-activedescendant) on the editing surface.
        new Plugin({
          props: { attributes: () => cb.current.attributes() },
        }),
      ];
    },
  });
}
