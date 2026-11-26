'use client';

import {
  forwardRef,
  useEffect,
  useId,
  useMemo,
  useReducer,
  useRef,
  useState,
  type DragEvent,
  type MouseEvent,
  type ReactNode,
  type Ref,
} from 'react';
import type { Editor, Extensions } from '@tiptap/core';
import { NodeSelection } from '@tiptap/pm/state';
import {
  RichTextEditor,
  type RichTextEditorHandle,
  type RichTextEditorProps,
  type RichTextFileHandler,
  type RichTextFileInsert,
  type RichTextHeadingLevel,
} from '../../collaboration/rich-text-editor/index.js';
import {
  SlashMenu,
  filterSlashMenuItems,
  slashMenuOptionId,
  useSlashMenuKeys,
  type SlashMenuMessages,
} from '../../collaboration/slash-menu/index.js';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from '../../overlays/dropdown-menu/index.js';
import {
  EDITOR_TOOLBAR_DEFAULT_ITEMS,
  EditorToolbar,
  type EditorToolbarItem,
  type EditorToolbarMessages,
} from '../editor-toolbar/index.js';
import { BubbleMenu } from '../bubble-menu/index.js';
import type { EmbedProvider } from '../embed-block/index.js';
import { cx } from '../../../internal/cx.js';
import { isSafeImageSrc } from '../../../internal/sanitize.js';
import {
  BLOCK_EDITOR_BLOCKS,
  DEFAULT_BLOCK_EDITOR_SLASH_MESSAGES,
  blockExtensions,
  buildSlashItems,
  createBridge,
  currentBlockPos,
  deleteBlock,
  duplicateBlock,
  moveBlock,
  type BlockEditorBlock,
  type BlockEditorSlashItem,
  type BlockEditorSlashMessages,
  type BridgeCallbacks,
  type SlashState,
} from './block-editor.blocks.js';

export {
  BLOCK_EDITOR_BLOCKS,
  DEFAULT_BLOCK_EDITOR_SLASH_MESSAGES,
  type BlockEditorBlock,
  type BlockEditorSlashItem,
  type BlockEditorSlashMessages,
} from './block-editor.blocks.js';

/** Imperative access: the RichTextEditor handle (focus, getHTML, getJSON, setContent, …). */
export type BlockEditorHandle = RichTextEditorHandle;

/** Every string the block editor itself shows or announces. */
export interface BlockEditorMessages {
  /** Placeholder of the empty document. */
  placeholder: string;
  /** Name of a block's handle, e.g. "Block actions: Heading 2". */
  handle: (block: string) => string;
  /** Hint read with the handle. */
  handleHint: string;
  moveUp: string;
  moveDown: string;
  duplicate: string;
  delete: string;
  /** Names of block types for the handle. */
  blockNames: Record<string, string>;
  /** Built-in slash items. */
  slash: Partial<BlockEditorSlashMessages>;
  /** The slash menu listbox. */
  slashMenu: Partial<SlashMenuMessages>;
  /** The formatting toolbar. */
  toolbar: Partial<EditorToolbarMessages>;
}

export const DEFAULT_BLOCK_EDITOR_MESSAGES: BlockEditorMessages = {
  placeholder: 'Write, or type / for blocks',
  handle: (block) => `Block actions: ${block}`,
  handleHint: 'Drag to move, or press Enter for actions. Alt+Shift+Up or Down moves the block.',
  moveUp: 'Move up',
  moveDown: 'Move down',
  duplicate: 'Duplicate',
  delete: 'Delete block',
  blockNames: {
    paragraph: 'Paragraph',
    heading: 'Heading',
    bulletList: 'Bulleted list',
    orderedList: 'Numbered list',
    taskList: 'Checklist',
    blockquote: 'Quote',
    codeBlock: 'Code block',
    image: 'Image',
    callout: 'Callout',
    toggleBlock: 'Toggle',
    columns: 'Columns',
    table: 'Table',
    embed: 'Embed',
  },
  slash: {},
  slashMenu: {},
  toolbar: {},
};

export interface BlockEditorProps extends Pick<
  RichTextEditorProps,
  | 'value'
  | 'defaultValue'
  | 'format'
  | 'onValueChange'
  | 'readOnly'
  | 'disabled'
  | 'invalid'
  | 'autoFocus'
  | 'maxLength'
  | 'showCharacterCount'
  | 'mentions'
  | 'name'
  | 'fileAccept'
  | 'minHeight'
  | 'maxHeight'
  | 'aria-label'
  | 'aria-labelledby'
  | 'aria-describedby'
  | 'className'
  | 'style'
  | 'id'
> {
  /** Shown in the empty document. Default "Write, or type / for blocks". */
  placeholder?: string | undefined;
  /** Custom blocks to offer. Default every one in `BLOCK_EDITOR_BLOCKS`. */
  blocks?: readonly BlockEditorBlock[] | undefined;
  /** Heading levels. Default `[1, 2, 3]`. */
  headingLevels?: readonly RichTextHeadingLevel[] | undefined;
  /** Formatting toolbar items above the document, or `false` for none. Default `EDITOR_TOOLBAR_DEFAULT_ITEMS`. */
  toolbar?: readonly EditorToolbarItem[] | false | undefined;
  /** Show the formatting menu over selected text. Default true. */
  bubbleMenu?: boolean | undefined;
  /** Show drag handles beside blocks. Default true. */
  dragHandles?: boolean | undefined;
  /** Open the `/` menu. Default true. */
  slashMenu?: boolean | undefined;
  /** Extra slash items, after the built-in ones. */
  slashItems?: readonly BlockEditorSlashItem[] | undefined;
  /** Group headings of the slash menu in display order. */
  slashGroupOrder?: readonly string[] | undefined;
  /** Sites embeds may frame. Default `DEFAULT_EMBED_PROVIDERS`. */
  embedProviders?: readonly EmbedProvider[] | undefined;
  /** Upload hook for pasted, dropped and picked files (enables images and the Image slash item). */
  onFiles?: RichTextFileHandler | undefined;
  /** Extra Tiptap extensions. Memoise: a new array rebuilds the editor. */
  extensions?: Extensions | undefined;
  /** Content at the end of the toolbar row. */
  toolbarEnd?: ReactNode;
  /** Imperative handle. */
  editorRef?: Ref<BlockEditorHandle> | undefined;
  /** Called with the Tiptap editor once it exists (and null when destroyed), e.g. for PageOutline. */
  onEditorReady?: ((editor: Editor | null) => void) | undefined;
  messages?: Partial<BlockEditorMessages> | undefined;
}

const DEFAULT_LEVELS: readonly RichTextHeadingLevel[] = [1, 2, 3];

/**
 * A Notion-style document editor on Tiptap: paragraphs, headings, lists,
 * checklists, quotes, code, images, callouts, toggles, columns, tables and
 * embeds; a `/` menu to insert blocks, drag handles and keyboard shortcuts to
 * move them, a formatting toolbar and a selection menu. Emits HTML or JSON.
 * Built on RichTextEditor, so paste cleaning, links, mentions and uploads
 * behave the same.
 */
export const BlockEditor = forwardRef<HTMLDivElement, BlockEditorProps>(function BlockEditor(
  {
    placeholder,
    blocks = BLOCK_EDITOR_BLOCKS,
    headingLevels = DEFAULT_LEVELS,
    toolbar = EDITOR_TOOLBAR_DEFAULT_ITEMS,
    bubbleMenu = true,
    dragHandles = true,
    slashMenu = true,
    slashItems,
    slashGroupOrder,
    embedProviders,
    onFiles,
    fileAccept,
    extensions,
    toolbarEnd,
    editorRef,
    onEditorReady,
    messages,
    readOnly = false,
    disabled = false,
    className,
    style,
    id,
    ...rich
  },
  ref,
) {
  const m: BlockEditorMessages = {
    ...DEFAULT_BLOCK_EDITOR_MESSAGES,
    ...messages,
    blockNames: { ...DEFAULT_BLOCK_EDITOR_MESSAGES.blockNames, ...messages?.blockNames },
  };
  const slashMessages = { ...DEFAULT_BLOCK_EDITOR_SLASH_MESSAGES, ...m.slash };
  const listId = `mn-block-editor-${useId().replace(/:/g, '')}-slash`;
  const [editor, setEditor] = useState<Editor | null>(null);
  const [slash, setSlash] = useState<SlashState | null>(null);
  const [dismissedAt, setDismissedAt] = useState<number | null>(null);
  const [linkOpen, setLinkOpen] = useState(false);
  const [hoverPos, setHoverPos] = useState<number | null>(null);
  // The block a handle menu was opened for; kept while the menu has focus.
  const [menuPos, setMenuPos] = useState<number | null>(null);
  // Focus anywhere in the editor (text, handle): the caret block keeps its handle.
  const [focusWithin, setFocusWithin] = useState(false);
  const [, rerender] = useReducer((n: number) => n + 1, 0);
  const body = useRef<HTMLDivElement>(null);
  const fileInput = useRef<HTMLInputElement>(null);
  const handleRef = useRef<RichTextEditorHandle | null>(null);
  const editable = !readOnly && !disabled;
  const useImages = blocks.includes('image') && Boolean(onFiles);

  const allItems = useMemo(() => {
    const builtIn = buildSlashItems(slashMessages, headingLevels, {
      pickImage: useImages ? () => fileInput.current?.click() : undefined,
    });
    return [...builtIn, ...(slashItems ?? [])];
    // eslint-disable-next-line react-hooks/exhaustive-deps -- messages are compared by value below.
  }, [JSON.stringify(m.slash), headingLevels, slashItems, useImages]);
  const available = editor ? allItems.filter((i) => i.isAvailable?.(editor) ?? true) : [];
  const open = Boolean(slash) && editable && slash?.range.from !== dismissedAt;
  const matches = open && slash ? filterSlashMenuItems(available, slash.query) : [];

  const close = () => setDismissedAt(slash?.range.from ?? null);
  const pick = (item: { id: string }) => {
    const chosen = available.find((i) => i.id === item.id);
    if (!editor || !slash || !chosen || chosen.disabled) return;
    editor.chain().focus().deleteRange(slash.range).run();
    chosen.run(editor);
  };
  const keys = useSlashMenuKeys({
    items: matches,
    onSelect: pick,
    onClose: open ? close : undefined,
    groupOrder: slashGroupOrder,
  });
  const expanded = open && matches.length > 0;
  const activeOption =
    expanded && keys.active ? slashMenuOptionId(listId, keys.active.id) : undefined;

  // Callbacks the Tiptap bridge reads; always the latest render's values.
  const live = useRef<BridgeCallbacks>(null as unknown as BridgeCallbacks);
  live.current = {
    onEditor: (ed) => {
      setEditor(ed);
      onEditorReady?.(ed);
    },
    onSlash: (s) => {
      setSlash(s);
      if (!s) setDismissedAt(null);
    },
    onSlashKey: (event) => (open ? keys.handleKeyDown(event) : false),
    onLinkShortcut: () => {
      if (!toolbar || !toolbar.includes('link')) return false;
      setLinkOpen(true);
      return true;
    },
    attributes: (): Record<string, string> =>
      expanded
        ? {
            'aria-controls': listId,
            'aria-autocomplete': 'list',
            ...(activeOption ? { 'aria-activedescendant': activeOption } : {}),
          }
        : {},
    slashEnabled: () => slashMenu,
  };

  // Push ARIA changes to the editing surface.
  useEffect(() => {
    if (editor && !editor.isDestroyed) editor.view.updateState(editor.view.state);
  }, [editor, expanded, activeOption]);

  // Overlays follow every change.
  useEffect(() => {
    if (!editor) return;
    const onTransaction = ({ transaction }: { transaction: { docChanged: boolean } }) => {
      // Positions shift when the document changes; the pointer re-targets on its next move.
      if (transaction.docChanged) {
        setHoverPos(null);
        setMenuPos(null);
      }
      rerender();
    };
    editor.on('transaction', onTransaction);
    return () => {
      editor.off('transaction', onTransaction);
    };
  }, [editor]);

  const bridge = useMemo(() => createBridge(live), []);
  const allExtensions = useMemo(
    () => [bridge, ...blockExtensions(blocks, embedProviders), ...(extensions ?? [])],
    // eslint-disable-next-line react-hooks/exhaustive-deps -- rebuild only when the block list changes.
    [bridge, blocks.join(','), embedProviders, extensions],
  );

  /* ---------------------------------------------------------- drag handle */

  const view = editor && !editor.isDestroyed ? editor.view : null;
  const blockPos =
    menuPos ??
    hoverPos ??
    (editor && (focusWithin || editor.isFocused) ? currentBlockPos(editor) : null);
  const blockNode = editor && blockPos !== null ? editor.state.doc.nodeAt(blockPos) : null;
  let handleTop: number | null = null;
  if (view && blockNode && blockPos !== null && body.current) {
    const dom = view.nodeDOM(blockPos);
    if (dom instanceof HTMLElement) {
      handleTop = dom.getBoundingClientRect().top - body.current.getBoundingClientRect().top;
    }
  }
  const blockName = blockNode
    ? blockNode.type.name === 'heading'
      ? `${m.blockNames.heading ?? 'Heading'} ${String(blockNode.attrs.level)}`
      : (m.blockNames[blockNode.type.name] ?? blockNode.type.name)
    : '';

  const onMouseMove = (e: MouseEvent<HTMLDivElement>) => {
    if (!view || !editable || !dragHandles) return;
    let el = e.target as HTMLElement | null;
    if (el?.closest('.mn-block-editor__handle-wrap')) return;
    while (el && el.parentElement !== view.dom) el = el.parentElement;
    if (!el) return;
    let found: number | null = null;
    view.state.doc.forEach((_node, offset) => {
      if (found === null && view.nodeDOM(offset) === el) found = offset;
    });
    if (found !== hoverPos) setHoverPos(found);
  };

  const onDragStart = (e: DragEvent<HTMLButtonElement>) => {
    if (!view || blockPos === null || !blockNode) return;
    const selection = NodeSelection.create(view.state.doc, blockPos);
    view.dispatch(view.state.tr.setSelection(selection));
    const slice = selection.content();
    // ProseMirror moves the node on drop when it knows what is being dragged.
    (view as unknown as { dragging: unknown }).dragging = { slice, move: true };
    const dom = view.nodeDOM(blockPos);
    e.dataTransfer.effectAllowed = 'move';
    e.dataTransfer.setData('text/plain', blockNode.textContent);
    if (dom instanceof HTMLElement) e.dataTransfer.setDragImage?.(dom, 0, 0);
  };

  /* ---------------------------------------------------------- images */

  const insertFiles = async (files: File[]) => {
    if (!onFiles || !files.length) return;
    const result = await Promise.resolve(onFiles(files, 'picker'));
    if (!Array.isArray(result)) return;
    const content = (result as readonly RichTextFileInsert[])
      .filter(
        (r): r is Extract<RichTextFileInsert, { type: 'image' }> =>
          r.type === 'image' && isSafeImageSrc(r.src),
      )
      .map((r) => ({
        type: 'image',
        attrs: { src: r.src, alt: r.alt ?? null, title: r.title ?? null },
      }));
    if (content.length && editor && !editor.isDestroyed)
      editor.chain().focus().insertContent(content).run();
  };

  const setRefs = (h: RichTextEditorHandle | null) => {
    handleRef.current = h;
    if (typeof editorRef === 'function') editorRef(h);
    else if (editorRef) (editorRef as { current: RichTextEditorHandle | null }).current = h;
  };

  const showHandle = dragHandles && editable && handleTop !== null && blockPos !== null;
  const menuPosition = slash?.rect;

  return (
    // Focus tracking only (which block keeps its handle); nothing here is a control.
    // eslint-disable-next-line jsx-a11y/no-static-element-interactions
    <div
      ref={ref}
      id={id}
      style={style}
      className={cx('mn-block-editor', className)}
      data-readonly={readOnly || undefined}
      data-disabled={disabled || undefined}
      onFocus={() => setFocusWithin(true)}
      onBlur={(e) => {
        const next = e.relatedTarget as Node | null;
        if (!next || !e.currentTarget.contains(next)) setFocusWithin(false);
      }}
    >
      {toolbar !== false && editable ? (
        <div className="mn-block-editor__toolbar">
          <EditorToolbar
            editor={editor}
            items={toolbar}
            headingLevels={headingLevels}
            linkOpen={linkOpen}
            onLinkOpenChange={setLinkOpen}
            messages={m.toolbar}
          />
          {toolbarEnd ? <div className="mn-block-editor__toolbar-end">{toolbarEnd}</div> : null}
        </div>
      ) : null}
      {/* Pointer tracking for the drag handle only; the keyboard path is the handle button and Alt+Shift+arrows. */}
      {/* eslint-disable-next-line jsx-a11y/no-static-element-interactions */}
      <div
        ref={body}
        className="mn-block-editor__body"
        onMouseMove={onMouseMove}
        onMouseLeave={() => setHoverPos(null)}
      >
        <RichTextEditor
          {...rich}
          readOnly={readOnly}
          disabled={disabled}
          placeholder={placeholder ?? m.placeholder}
          toolbar={false}
          headingLevels={headingLevels}
          taskList
          {...(useImages ? { images: true } : {})}
          {...(onFiles ? { onFiles } : {})}
          {...(fileAccept ? { fileAccept } : {})}
          extensions={allExtensions}
          editorRef={setRefs}
          className="mn-block-editor__surface"
        />
        {showHandle ? (
          <div className="mn-block-editor__handle-wrap" style={{ top: handleTop ?? 0 }}>
            <DropdownMenu onOpenChange={(o) => setMenuPos(o ? blockPos : null)}>
              <DropdownMenuTrigger asChild>
                <button
                  type="button"
                  className="mn-block-editor__handle"
                  aria-label={m.handle(blockName)}
                  title={m.handleHint}
                  draggable
                  onDragStart={onDragStart}
                  onDragEnd={() => setHoverPos(null)}
                >
                  <svg width="12" height="16" viewBox="0 0 12 16" aria-hidden focusable="false">
                    {[3, 8, 13].map((y) =>
                      [4, 8].map((x) => (
                        <circle key={`${x}-${y}`} cx={x} cy={y} r="1.25" fill="currentColor" />
                      )),
                    )}
                  </svg>
                </button>
              </DropdownMenuTrigger>
              <DropdownMenuContent
                onCloseAutoFocus={(e) => {
                  e.preventDefault();
                  if (editor && !editor.isDestroyed) editor.commands.focus();
                }}
              >
                <DropdownMenuItem
                  shortcut="⌥⇧↑"
                  onSelect={() => editor && blockPos !== null && moveBlock(editor, blockPos, -1)}
                  disabled={
                    !editor ||
                    blockPos === null ||
                    editor.state.doc.resolve(blockPos).index(0) === 0
                  }
                >
                  {m.moveUp}
                </DropdownMenuItem>
                <DropdownMenuItem
                  shortcut="⌥⇧↓"
                  onSelect={() => editor && blockPos !== null && moveBlock(editor, blockPos, 1)}
                  disabled={
                    !editor ||
                    blockPos === null ||
                    editor.state.doc.resolve(blockPos).index(0) >= editor.state.doc.childCount - 1
                  }
                >
                  {m.moveDown}
                </DropdownMenuItem>
                <DropdownMenuItem
                  onSelect={() => editor && blockPos !== null && duplicateBlock(editor, blockPos)}
                >
                  {m.duplicate}
                </DropdownMenuItem>
                <DropdownMenuSeparator />
                <DropdownMenuItem
                  tone="danger"
                  onSelect={() => editor && blockPos !== null && deleteBlock(editor, blockPos)}
                >
                  {m.delete}
                </DropdownMenuItem>
              </DropdownMenuContent>
            </DropdownMenu>
          </div>
        ) : null}
      </div>
      {open && slash ? (
        <SlashMenu
          className="mn-block-editor__slash"
          style={
            menuPosition
              ? {
                  position: 'fixed',
                  top: menuPosition.bottom + 4,
                  // Keep the menu inside the viewport on narrow screens.
                  left: Math.max(
                    8,
                    Math.min(
                      menuPosition.left,
                      (typeof window === 'undefined' ? Infinity : window.innerWidth) - 360,
                    ),
                  ),
                }
              : undefined
          }
          items={matches}
          onSelect={pick}
          activeId={keys.active?.id ?? null}
          onActiveChange={keys.setActiveId}
          groupOrder={slashGroupOrder}
          query={slash.query}
          listId={listId}
          messages={m.slashMenu}
        />
      ) : null}
      {bubbleMenu && editable ? <BubbleMenu editor={editor} /> : null}
      {useImages ? (
        <input
          ref={fileInput}
          type="file"
          accept={fileAccept ?? 'image/*'}
          multiple
          hidden
          tabIndex={-1}
          onChange={(e) => {
            const files = Array.from(e.target.files ?? []);
            e.target.value = '';
            void insertFiles(files);
          }}
        />
      ) : null}
    </div>
  );
});
