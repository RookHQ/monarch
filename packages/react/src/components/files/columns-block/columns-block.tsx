'use client';

import {
  Children,
  forwardRef,
  useRef,
  type CSSProperties,
  type HTMLAttributes,
  type KeyboardEvent,
  type PointerEvent as ReactPointerEvent,
} from 'react';
import { Node, mergeAttributes } from '@tiptap/core';
import { TextSelection } from '@tiptap/pm/state';
import {
  NodeViewContent,
  NodeViewWrapper,
  ReactNodeViewRenderer,
  type NodeViewProps,
} from '@tiptap/react';
import { cx } from '../../../internal/cx.js';
import { useDirection } from '../../../internal/direction.js';

/** Smallest and largest share of the first of two columns. */
export const COLUMNS_MIN_RATIO = 0.2;
export const COLUMNS_MAX_RATIO = 0.8;
const STEP = 0.05;

/** Keeps a stored ratio inside 0.2–0.8 (0.5 when it is not a number). */
export function clampColumnsRatio(value: unknown): number {
  const n = typeof value === 'number' ? value : Number(value);
  if (!Number.isFinite(n)) return 0.5;
  return Math.min(COLUMNS_MAX_RATIO, Math.max(COLUMNS_MIN_RATIO, Math.round(n * 1000) / 1000));
}

/** Strings the columns' resize handle announces. */
export interface ColumnsBlockMessages {
  /** Name of the resize handle. Default "Resize columns". */
  resize: string;
  /** Spoken value of the handle. Default "First column 50%". */
  value: (percent: number) => string;
}

export const DEFAULT_COLUMNS_BLOCK_MESSAGES: ColumnsBlockMessages = {
  resize: 'Resize columns',
  value: (percent) => `First column ${percent}%`,
};

export interface ColumnsBlockProps extends HTMLAttributes<HTMLDivElement> {
  /**
   * Share of the width for the first of two columns, 0.2–0.8. Default 0.5.
   * Three or more columns are always equal.
   */
  ratio?: number | undefined;
}

/**
 * Side-by-side columns inside a document. Each child is one column; they
 * stack into one column when the container is narrow. In a Tiptap editor use
 * `ColumnsBlockExtension`; on a static page render this directly.
 */
export const ColumnsBlock = forwardRef<HTMLDivElement, ColumnsBlockProps>(function ColumnsBlock(
  { ratio = 0.5, className, style, children, ...rest },
  ref,
) {
  const items = Children.toArray(children);
  return (
    <div
      ref={ref}
      {...rest}
      className={cx('mn-columns-block', className)}
      data-columns={items.length}
      style={
        {
          '--mn-columns-block-ratio': String(clampColumnsRatio(ratio)),
          ...style,
        } as CSSProperties
      }
    >
      <div className="mn-columns-block__grid">
        {items.map((child, i) => (
          <div key={i} className="mn-columns-block__column">
            {child}
          </div>
        ))}
      </div>
    </div>
  );
});

/* ------------------------------------------------------------- extension */

/** Options for `ColumnsBlockExtension.configure()`. */
export interface ColumnsBlockExtensionOptions {
  messages: Partial<ColumnsBlockMessages> | undefined;
  HTMLAttributes: Record<string, unknown>;
}

declare module '@tiptap/core' {
  interface Commands<ReturnType> {
    columns: {
      /** Inserts two (default) or three empty columns after the block at the caret. */
      insertColumns: (options?: { count?: 2 | 3 }) => ReturnType;
      /** Sets the first column's share of two columns (0.2–0.8). */
      setColumnsRatio: (ratio: number) => ReturnType;
    };
  }
}

/** One column: any blocks. */
export const ColumnNode = Node.create({
  name: 'column',
  content: 'block+',
  isolating: true,
  parseHTML() {
    return [{ tag: 'div[data-type="column"]' }];
  },
  renderHTML({ HTMLAttributes }) {
    return [
      'div',
      mergeAttributes(HTMLAttributes, { 'data-type': 'column', class: 'mn-columns-block__column' }),
      0,
    ];
  },
});

function ColumnsNodeView({ node, editor, extension, updateAttributes }: NodeViewProps) {
  const readDir = useDirection();
  const options = extension.options as ColumnsBlockExtensionOptions;
  const m = { ...DEFAULT_COLUMNS_BLOCK_MESSAGES, ...options.messages };
  const wrapper = useRef<HTMLDivElement | null>(null);
  const ratio = clampColumnsRatio(node.attrs.ratio);
  const live = useRef(ratio);
  const count = node.childCount;
  const percent = Math.round(ratio * 100);

  const isRtl = () => readDir(wrapper.current) === 'rtl';

  const onKeyDown = (e: KeyboardEvent<HTMLDivElement>) => {
    const rtl = isRtl();
    let next: number | null = null;
    if (e.key === (rtl ? 'ArrowLeft' : 'ArrowRight')) next = ratio + STEP;
    else if (e.key === (rtl ? 'ArrowRight' : 'ArrowLeft')) next = ratio - STEP;
    else if (e.key === 'Home') next = COLUMNS_MIN_RATIO;
    else if (e.key === 'End') next = COLUMNS_MAX_RATIO;
    else if (e.key === 'Enter') next = 0.5;
    if (next === null) return;
    e.preventDefault();
    e.stopPropagation();
    updateAttributes({ ratio: clampColumnsRatio(next) });
  };

  // The ratio follows the pointer through a CSS variable and is written to
  // the document once, on release, so a drag is one undo step.
  const onPointerDown = (event: ReactPointerEvent<HTMLDivElement>) => {
    if (!editor.isEditable || event.button !== 0) return;
    event.preventDefault();
    const handle = event.currentTarget;
    const el = wrapper.current;
    if (!el) return;
    handle.setPointerCapture?.(event.pointerId);
    const rtl = isRtl();
    const onMove = (e: PointerEvent) => {
      const rect = el.getBoundingClientRect();
      if (!rect.width) return;
      const raw = rtl
        ? (rect.right - e.clientX) / rect.width
        : (e.clientX - rect.left) / rect.width;
      live.current = clampColumnsRatio(raw);
      el.style.setProperty('--mn-columns-block-ratio', String(live.current));
    };
    const onUp = () => {
      handle.removeEventListener('pointermove', onMove);
      handle.removeEventListener('pointerup', onUp);
      handle.removeEventListener('pointercancel', onUp);
      el.style.removeProperty('--mn-columns-block-ratio');
      updateAttributes({ ratio: live.current });
    };
    live.current = ratio;
    handle.addEventListener('pointermove', onMove);
    handle.addEventListener('pointerup', onUp);
    handle.addEventListener('pointercancel', onUp);
  };

  return (
    <NodeViewWrapper
      ref={wrapper}
      className="mn-columns-block"
      data-type="columns"
      data-columns={count}
      data-editor=""
      style={{ '--mn-columns-block-ratio': String(ratio) } as CSSProperties}
    >
      <NodeViewContent className="mn-columns-block__grid" />
      {editor.isEditable && count === 2 ? (
        // A focusable separator is the APG window splitter, an interactive widget.
        // eslint-disable-next-line jsx-a11y/no-noninteractive-element-interactions
        <div
          role="separator"
          // eslint-disable-next-line jsx-a11y/no-noninteractive-tabindex
          tabIndex={0}
          contentEditable={false}
          aria-orientation="vertical"
          aria-label={m.resize}
          aria-valuemin={COLUMNS_MIN_RATIO * 100}
          aria-valuemax={COLUMNS_MAX_RATIO * 100}
          aria-valuenow={percent}
          aria-valuetext={m.value(percent)}
          className="mn-columns-block__handle"
          onKeyDown={onKeyDown}
          onPointerDown={onPointerDown}
        />
      ) : null}
    </NodeViewWrapper>
  );
}

/**
 * The Tiptap node for ColumnsBlock: `columns` holding two or three `column`
 * nodes of blocks, with a `ratio` attribute for the first of two columns.
 * Renders `<div data-type="columns" data-ratio="0.5"><div
 * data-type="column">…</div>…</div>` in HTML. While editable, two columns
 * get a resize handle (an APG window splitter). Adds `insertColumns` and
 * `setColumnsRatio`.
 */
export const ColumnsBlockExtension = Node.create<ColumnsBlockExtensionOptions>({
  name: 'columns',
  group: 'block',
  content: 'column column column?',
  defining: true,
  isolating: true,
  addOptions() {
    return { messages: undefined, HTMLAttributes: {} };
  },
  addExtensions() {
    return [ColumnNode];
  },
  addAttributes() {
    return {
      ratio: {
        default: 0.5,
        parseHTML: (el: HTMLElement) => clampColumnsRatio(el.getAttribute('data-ratio') ?? 0.5),
        renderHTML: (attrs: { ratio?: unknown }) => ({
          'data-ratio': String(clampColumnsRatio(attrs.ratio)),
        }),
      },
    };
  },
  parseHTML() {
    return [{ tag: 'div[data-type="columns"]' }];
  },
  renderHTML({ HTMLAttributes }) {
    return [
      'div',
      mergeAttributes(this.options.HTMLAttributes, HTMLAttributes, {
        'data-type': 'columns',
        class: 'mn-columns-block',
      }),
      0,
    ];
  },
  addCommands() {
    return {
      insertColumns:
        (options) =>
        ({ state, tr, dispatch }) => {
          const { schema, selection } = state;
          const columns = schema.nodes[this.name];
          const column = schema.nodes.column;
          const paragraph = schema.nodes.paragraph;
          if (!columns || !column || !paragraph) return false;
          const count = options?.count === 3 ? 3 : 2;
          const node = columns.create(
            { ratio: 0.5 },
            Array.from({ length: count }, () => column.create(null, paragraph.create())),
          );
          const { $from } = selection;
          // Replace an empty paragraph at the caret, otherwise insert after the top-level block.
          const emptyParagraph =
            $from.depth > 0 && $from.parent.type === paragraph && $from.parent.content.size === 0;
          if (dispatch) {
            let at: number;
            if (emptyParagraph) {
              at = $from.before();
              tr.replaceWith(at, $from.after(), node);
            } else {
              at = $from.depth ? $from.after(1) : selection.to;
              tr.insert(at, node);
            }
            tr.setSelection(TextSelection.near(tr.doc.resolve(at + 3)));
            dispatch(tr.scrollIntoView());
          }
          return true;
        },
      setColumnsRatio:
        (ratio) =>
        ({ commands }) =>
          commands.updateAttributes(this.name, { ratio: clampColumnsRatio(ratio) }),
    };
  },
  addNodeView() {
    return ReactNodeViewRenderer(ColumnsNodeView);
  },
});
