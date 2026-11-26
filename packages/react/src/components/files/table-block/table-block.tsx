'use client';

import {
  forwardRef,
  useEffect,
  useRef,
  useState,
  type HTMLAttributes,
  type KeyboardEvent,
  type ReactNode,
} from 'react';
import { Node, mergeAttributes, type Editor } from '@tiptap/core';
import type { Node as PMNode, NodeType, Schema } from '@tiptap/pm/model';
import { TextSelection } from '@tiptap/pm/state';
import {
  addColumnAfter,
  addRowAfter,
  deleteColumn,
  deleteRow,
  deleteTable,
  goToNextCell,
  isInTable,
  rowIsHeader,
  selectedRect,
  tableEditing,
  toggleHeaderRow,
} from '@tiptap/pm/tables';
import {
  NodeViewContent,
  NodeViewWrapper,
  ReactNodeViewRenderer,
  type NodeViewProps,
} from '@tiptap/react';
import { cx } from '../../../internal/cx.js';
import { useDirection } from '../../../internal/direction.js';

/** Strings the table and its controls show or announce. */
export interface TableBlockMessages {
  /** Name of the scrollable region of a static table. Default "Table". */
  region: string;
  /** Name of the controls toolbar. Default "Table". */
  toolbar: string;
  addRow: string;
  addColumn: string;
  deleteRow: string;
  deleteColumn: string;
  headerRow: string;
  deleteTable: string;
}

export const DEFAULT_TABLE_BLOCK_MESSAGES: TableBlockMessages = {
  region: 'Table',
  toolbar: 'Table',
  addRow: 'Add row below',
  addColumn: 'Add column after',
  deleteRow: 'Delete row',
  deleteColumn: 'Delete column',
  headerRow: 'Header row',
  deleteTable: 'Delete table',
};

export interface TableBlockProps extends HTMLAttributes<HTMLDivElement> {
  /** Shown above the table and used as the name of its scroll region. */
  caption?: ReactNode;
  /** Name of the scroll region when there is no plain-text caption. */
  label?: string | undefined;
  /** Content above the table, such as editing controls. */
  toolbar?: ReactNode;
  /** The `<table>`. */
  children?: ReactNode;
}

/**
 * The frame around a table in a document: an optional caption and a region
 * that scrolls sideways (and can be scrolled with the keyboard) when the
 * table is wider than its column. In a Tiptap editor use
 * `TableBlockExtension`; on a static page wrap a `<table>` in this.
 */
export const TableBlock = forwardRef<HTMLDivElement, TableBlockProps>(function TableBlock(
  { caption, label, toolbar, className, children, ...rest },
  ref,
) {
  const name =
    label ?? (typeof caption === 'string' ? caption : DEFAULT_TABLE_BLOCK_MESSAGES.region);
  return (
    <div ref={ref} {...rest} className={cx('mn-table-block', className)}>
      {toolbar}
      {caption ? <div className="mn-table-block__caption">{caption}</div> : null}
      {/* A focusable region so keyboard users can scroll a wide table. */}
      {/* eslint-disable-next-line jsx-a11y/no-noninteractive-tabindex */}
      <div className="mn-table-block__scroll" role="region" aria-label={name} tabIndex={0}>
        {children}
      </div>
    </div>
  );
});

/* ------------------------------------------------------------- schema */

declare module '@tiptap/core' {
  interface Commands<ReturnType> {
    tableBlock: {
      /** Inserts a table (default 3 × 3 with a header row) and puts the caret in its first cell. */
      insertTable: (options?: {
        rows?: number;
        cols?: number;
        withHeaderRow?: boolean;
      }) => ReturnType;
      addRowAfter: () => ReturnType;
      addColumnAfter: () => ReturnType;
      deleteRow: () => ReturnType;
      deleteColumn: () => ReturnType;
      deleteTable: () => ReturnType;
      toggleHeaderRow: () => ReturnType;
      /** Moves to the next (1) or previous (-1) cell. */
      goToNextCell: (direction: 1 | -1) => ReturnType;
    };
  }
}

const cellAttributes = () => ({
  colspan: {
    default: 1,
    parseHTML: (el: HTMLElement) => Math.max(1, Number(el.getAttribute('colspan')) || 1),
    renderHTML: (a: { colspan?: number }) =>
      a.colspan && a.colspan > 1 ? { colspan: a.colspan } : {},
  },
  rowspan: {
    default: 1,
    parseHTML: (el: HTMLElement) => Math.max(1, Number(el.getAttribute('rowspan')) || 1),
    renderHTML: (a: { rowspan?: number }) =>
      a.rowspan && a.rowspan > 1 ? { rowspan: a.rowspan } : {},
  },
  colwidth: { default: null, rendered: false },
});

/** A table body cell (`<td>`). */
export const TableCellNode = Node.create({
  name: 'tableCell',
  content: 'block+',
  isolating: true,
  addAttributes: cellAttributes,
  parseHTML() {
    return [{ tag: 'td' }];
  },
  renderHTML({ HTMLAttributes }) {
    return ['td', HTMLAttributes, 0];
  },
});

/** A table header cell (`<th>`). */
export const TableHeaderNode = Node.create({
  name: 'tableHeader',
  content: 'block+',
  isolating: true,
  addAttributes: cellAttributes,
  parseHTML() {
    return [{ tag: 'th' }];
  },
  renderHTML({ HTMLAttributes }) {
    return ['th', mergeAttributes(HTMLAttributes, { scope: 'col' }), 0];
  },
});

/** A table row (`<tr>`). */
export const TableRowNode = Node.create({
  name: 'tableRow',
  content: '(tableCell | tableHeader)*',
  parseHTML() {
    return [{ tag: 'tr' }];
  },
  renderHTML({ HTMLAttributes }) {
    return ['tr', HTMLAttributes, 0];
  },
});

const TABLE_ROLES: Record<string, string> = {
  table: 'table',
  tableRow: 'row',
  tableCell: 'cell',
  tableHeader: 'header_cell',
};

function createTable(schema: Schema, rows: number, cols: number, header: boolean): PMNode | null {
  const { table, tableRow, tableCell, tableHeader, paragraph } = schema.nodes as Record<
    string,
    NodeType | undefined
  >;
  if (!table || !tableRow || !tableCell || !tableHeader || !paragraph) return null;
  const cell = (type: NodeType) => type.create(null, paragraph.create());
  const out: PMNode[] = [];
  for (let r = 0; r < rows; r++) {
    const type = header && r === 0 ? tableHeader : tableCell;
    out.push(
      tableRow.create(
        null,
        Array.from({ length: cols }, () => cell(type)),
      ),
    );
  }
  return table.create(null, out);
}

/* ------------------------------------------------------------- node view */

interface Control {
  key: keyof Omit<TableBlockMessages, 'region' | 'toolbar'>;
  run: (editor: Editor) => boolean;
  pressed?: boolean;
  danger?: boolean;
}

function useInside(editor: Editor, getPos: NodeViewProps['getPos'], size: number) {
  const [inside, setInside] = useState(false);
  useEffect(() => {
    const check = () => {
      const pos = typeof getPos === 'function' ? getPos() : undefined;
      if (typeof pos !== 'number') return setInside(false);
      const { from, to } = editor.state.selection;
      setInside(from > pos && to < pos + size);
    };
    check();
    editor.on('selectionUpdate', check);
    editor.on('transaction', check);
    return () => {
      editor.off('selectionUpdate', check);
      editor.off('transaction', check);
    };
  }, [editor, getPos, size]);
  return inside;
}

function TableNodeView({ node, editor, extension, getPos }: NodeViewProps) {
  const readDir = useDirection();
  const options = extension.options as TableBlockExtensionOptions;
  const m = { ...DEFAULT_TABLE_BLOCK_MESSAGES, ...options.messages };
  const inside = useInside(editor, getPos, node.nodeSize);
  const [active, setActive] = useState(0);
  const buttons = useRef<(HTMLButtonElement | null)[]>([]);
  const showControls = editor.isEditable && inside;

  let headerOn = false;
  if (showControls && isInTable(editor.state)) {
    try {
      const rect = selectedRect(editor.state);
      headerOn = rowIsHeader(rect.map, rect.table, 0);
    } catch {
      headerOn = false;
    }
  }
  const controls: Control[] = [
    { key: 'addRow', run: (e) => e.commands.addRowAfter() },
    { key: 'addColumn', run: (e) => e.commands.addColumnAfter() },
    { key: 'deleteRow', run: (e) => e.commands.deleteRow() },
    { key: 'deleteColumn', run: (e) => e.commands.deleteColumn() },
    { key: 'headerRow', run: (e) => e.commands.toggleHeaderRow(), pressed: headerOn },
    { key: 'deleteTable', run: (e) => e.chain().focus().deleteTable().run(), danger: true },
  ];

  const onKeyDown = (e: KeyboardEvent<HTMLDivElement>) => {
    const n = controls.length;
    const rtl = readDir(e.currentTarget) === 'rtl';
    let next: number | null = null;
    if (e.key === (rtl ? 'ArrowLeft' : 'ArrowRight')) next = (active + 1) % n;
    else if (e.key === (rtl ? 'ArrowRight' : 'ArrowLeft')) next = (active - 1 + n) % n;
    else if (e.key === 'Home') next = 0;
    else if (e.key === 'End') next = n - 1;
    else if (e.key === 'Escape') {
      e.preventDefault();
      editor.commands.focus();
      return;
    }
    if (next === null) return;
    e.preventDefault();
    setActive(next);
    buttons.current[next]?.focus();
  };

  return (
    <NodeViewWrapper className="mn-table-block" data-type="table" data-active={inside || undefined}>
      {showControls ? (
        <div
          role="toolbar"
          aria-label={m.toolbar}
          aria-orientation="horizontal"
          className="mn-table-block__toolbar"
          contentEditable={false}
          onKeyDown={onKeyDown}
        >
          {controls.map((c, i) => (
            <button
              key={c.key}
              ref={(el) => {
                buttons.current[i] = el;
              }}
              type="button"
              className="mn-table-block__tool"
              data-danger={c.danger || undefined}
              tabIndex={i === active ? 0 : -1}
              aria-pressed={c.pressed === undefined ? undefined : c.pressed}
              onMouseDown={(e) => e.preventDefault()}
              onFocus={() => setActive(i)}
              onClick={() => {
                c.run(editor);
              }}
            >
              {m[c.key]}
            </button>
          ))}
        </div>
      ) : null}
      <div className="mn-table-block__scroll">
        <NodeViewContent<'table'> as="table" className="mn-table-block__table" />
      </div>
    </NodeViewWrapper>
  );
}

/* ------------------------------------------------------------- extension */

/** Options for `TableBlockExtension.configure()`. */
export interface TableBlockExtensionOptions {
  messages: Partial<TableBlockMessages> | undefined;
  HTMLAttributes: Record<string, unknown>;
}

const wrap =
  (fn: (state: Editor['state'], dispatch?: Editor['view']['dispatch'] | undefined) => boolean) =>
  () =>
  ({
    state,
    dispatch,
  }: {
    state: Editor['state'];
    dispatch?: Editor['view']['dispatch'] | undefined;
  }) =>
    fn(state, dispatch);

/**
 * The Tiptap node for TableBlock: `table`, `tableRow`, `tableCell` and
 * `tableHeader` on prosemirror-tables (cell selection, Tab between cells,
 * schema repair), with a controls toolbar while the caret is in the table.
 * Renders a plain `<table><tbody>…</tbody></table>` in HTML. Adds
 * `insertTable`, `addRowAfter`, `addColumnAfter`, `deleteRow`,
 * `deleteColumn`, `deleteTable`, `toggleHeaderRow` and `goToNextCell`.
 */
export const TableBlockExtension = Node.create<TableBlockExtensionOptions>({
  name: 'table',
  group: 'block',
  content: 'tableRow+',
  isolating: true,
  addOptions() {
    return { messages: undefined, HTMLAttributes: {} };
  },
  addExtensions() {
    return [TableRowNode, TableHeaderNode, TableCellNode];
  },
  extendNodeSchema(extension) {
    const role = TABLE_ROLES[extension.name];
    return role ? { tableRole: role } : {};
  },
  parseHTML() {
    return [{ tag: 'table' }];
  },
  renderHTML({ HTMLAttributes }) {
    return [
      'table',
      mergeAttributes(this.options.HTMLAttributes, HTMLAttributes, {
        class: 'mn-table-block__table',
      }),
      ['tbody', 0],
    ];
  },
  addCommands() {
    return {
      insertTable:
        (options) =>
        ({ state, tr, dispatch }) => {
          const rows = Math.min(Math.max(options?.rows ?? 3, 1), 50);
          const cols = Math.min(Math.max(options?.cols ?? 3, 1), 20);
          const table = createTable(state.schema, rows, cols, options?.withHeaderRow ?? true);
          if (!table) return false;
          if (dispatch) {
            const { $from } = state.selection;
            const emptyParagraph =
              $from.depth > 0 &&
              $from.parent.type.name === 'paragraph' &&
              !$from.parent.content.size;
            let at: number;
            if (emptyParagraph) {
              at = $from.before();
              tr.replaceWith(at, $from.after(), table);
            } else {
              at = $from.depth ? $from.after(1) : state.selection.to;
              tr.insert(at, table);
            }
            // table > row > cell > paragraph
            tr.setSelection(TextSelection.near(tr.doc.resolve(at + 4)));
            dispatch(tr.scrollIntoView());
          }
          return true;
        },
      addRowAfter: wrap(addRowAfter),
      addColumnAfter: wrap(addColumnAfter),
      deleteRow: wrap(deleteRow),
      deleteColumn: wrap(deleteColumn),
      deleteTable: wrap(deleteTable),
      toggleHeaderRow: wrap(toggleHeaderRow),
      goToNextCell:
        (direction) =>
        ({ state, dispatch }) =>
          goToNextCell(direction)(state, dispatch),
    };
  },
  addKeyboardShortcuts() {
    return {
      Tab: ({ editor }) => {
        if (!isInTable(editor.state)) return false;
        if (editor.commands.goToNextCell(1)) return true;
        // Last cell: add a row and move into it.
        return editor.chain().addRowAfter().goToNextCell(1).run();
      },
      'Shift-Tab': ({ editor }) => {
        if (!isInTable(editor.state)) return false;
        editor.commands.goToNextCell(-1);
        return true;
      },
      // Alt+F10 in a table (no text selected) moves focus to the table controls.
      'Alt-F10': ({ editor }) => {
        if (!isInTable(editor.state) || !editor.state.selection.empty) return false;
        const at = editor.view.domAtPos(editor.state.selection.from).node;
        const el = at instanceof HTMLElement ? at : at.parentElement;
        const tool = el
          ?.closest('.mn-table-block')
          ?.querySelector<HTMLButtonElement>('.mn-table-block__toolbar [tabindex="0"]');
        if (!tool) return false;
        tool.focus();
        return true;
      },
    };
  },
  addProseMirrorPlugins() {
    return [tableEditing()];
  },
  addNodeView() {
    return ReactNodeViewRenderer(TableNodeView, { contentDOMElementTag: 'tbody' });
  },
});
