'use client';

import { forwardRef, useId, useState, type HTMLAttributes, type ReactNode } from 'react';
import { Node, mergeAttributes } from '@tiptap/core';
import { TextSelection } from '@tiptap/pm/state';
import {
  NodeViewContent,
  NodeViewWrapper,
  ReactNodeViewRenderer,
  type NodeViewProps,
} from '@tiptap/react';
import { cx } from '../../../internal/cx.js';
import { ChevronRightGlyph } from '../../../internal/icons.js';

/** Strings the toggle's button announces. */
export interface ToggleBlockMessages {
  /** Name of the button while collapsed. Default "Show section". */
  expand: string;
  /** Name of the button while expanded. Default "Hide section". */
  collapse: string;
  /** Placeholder shown in an empty summary (editor only). Default "Toggle title". */
  summaryPlaceholder: string;
}

export const DEFAULT_TOGGLE_BLOCK_MESSAGES: ToggleBlockMessages = {
  expand: 'Show section',
  collapse: 'Hide section',
  summaryPlaceholder: 'Toggle title',
};

export interface ToggleBlockProps extends Omit<HTMLAttributes<HTMLDivElement>, 'title'> {
  /** The always-visible line. It is the button's accessible name. */
  summary: ReactNode;
  /** Controlled open state. */
  open?: boolean | undefined;
  /** Initial open state when uncontrolled. Default false. */
  defaultOpen?: boolean | undefined;
  /** Called when the button is pressed. */
  onOpenChange?: ((open: boolean) => void) | undefined;
}

/**
 * A collapsible section of a document (an APG disclosure): a summary line
 * with a button that shows or hides the content under it. In a Tiptap editor
 * use `ToggleBlockExtension`; on a static page render this directly.
 */
export const ToggleBlock = forwardRef<HTMLDivElement, ToggleBlockProps>(function ToggleBlock(
  { summary, open: openProp, defaultOpen = false, onOpenChange, className, children, ...rest },
  ref,
) {
  const [inner, setInner] = useState(defaultOpen);
  const open = openProp ?? inner;
  const id = `mn-toggle-block-${useId().replace(/:/g, '')}`;
  return (
    <div
      ref={ref}
      {...rest}
      className={cx('mn-toggle-block', className)}
      data-open={open ? 'true' : 'false'}
    >
      <div className="mn-toggle-block__head">
        <button
          type="button"
          className="mn-toggle-block__trigger"
          aria-expanded={open}
          aria-controls={`${id}-content`}
          onClick={() => {
            const next = !open;
            if (openProp === undefined) setInner(next);
            onOpenChange?.(next);
          }}
        >
          <span className="mn-toggle-block__chevron" aria-hidden>
            <ChevronRightGlyph />
          </span>
          <span className="mn-toggle-block__summary">{summary}</span>
        </button>
      </div>
      <div id={`${id}-content`} className="mn-toggle-block__body" hidden={!open}>
        {children}
      </div>
    </div>
  );
});

/* ------------------------------------------------------------- extension */

/** Options for `ToggleBlockExtension.configure()`. */
export interface ToggleBlockExtensionOptions {
  messages: Partial<ToggleBlockMessages> | undefined;
  HTMLAttributes: Record<string, unknown>;
}

declare module '@tiptap/core' {
  interface Commands<ReturnType> {
    toggleBlock: {
      /**
       * Turns the paragraph at the caret into a toggle whose summary is that
       * paragraph's text, open, with an empty paragraph inside.
       */
      setToggleBlock: () => ReturnType;
    };
  }
}

/** The summary line of a toggle: inline content only. */
export const ToggleSummaryNode = Node.create({
  name: 'toggleSummary',
  content: 'inline*',
  defining: true,
  isolating: true,
  parseHTML() {
    return [{ tag: 'div[data-type="toggle-summary"]' }];
  },
  renderHTML({ HTMLAttributes }) {
    return [
      'div',
      mergeAttributes(HTMLAttributes, {
        'data-type': 'toggle-summary',
        class: 'mn-toggle-block__summary-text',
      }),
      0,
    ];
  },
  addKeyboardShortcuts() {
    return {
      // Enter in the summary opens the toggle and moves into its content.
      Enter: ({ editor }) => {
        const { $from } = editor.state.selection;
        if ($from.parent.type.name !== this.name) return false;
        const toggleDepth = $from.depth - 1;
        const toggle = $from.node(toggleDepth);
        const togglePos = $from.before(toggleDepth);
        // toggle > summary > content > first block
        const contentStart = togglePos + 1 + toggle.child(0).nodeSize + 1;
        const tr = editor.state.tr.setNodeMarkup(togglePos, undefined, {
          ...toggle.attrs,
          open: true,
        });
        tr.setSelection(TextSelection.near(tr.doc.resolve(contentStart + 1)));
        editor.view.dispatch(tr.scrollIntoView());
        return true;
      },
    };
  },
});

/** The collapsible content of a toggle: any blocks. */
export const ToggleContentNode = Node.create({
  name: 'toggleContent',
  content: 'block+',
  isolating: true,
  parseHTML() {
    return [{ tag: 'div[data-type="toggle-content"]' }];
  },
  renderHTML({ HTMLAttributes }) {
    return [
      'div',
      mergeAttributes(HTMLAttributes, {
        'data-type': 'toggle-content',
        class: 'mn-toggle-block__body',
      }),
      0,
    ];
  },
});

function ToggleNodeView({ node, updateAttributes, extension }: NodeViewProps) {
  const options = extension.options as ToggleBlockExtensionOptions;
  const m = { ...DEFAULT_TOGGLE_BLOCK_MESSAGES, ...options.messages };
  const open = Boolean(node.attrs.open);
  const summary = node.firstChild?.textContent.trim() ?? '';
  return (
    <NodeViewWrapper
      className="mn-toggle-block"
      data-type="toggle-block"
      data-open={open ? 'true' : 'false'}
      data-empty-summary={summary ? undefined : 'true'}
    >
      <button
        type="button"
        contentEditable={false}
        className="mn-toggle-block__trigger mn-toggle-block__trigger--icon"
        aria-expanded={open}
        aria-label={
          summary ? `${open ? m.collapse : m.expand}: ${summary}` : open ? m.collapse : m.expand
        }
        onMouseDown={(e) => e.preventDefault()}
        onClick={() => updateAttributes({ open: !open })}
      >
        <span className="mn-toggle-block__chevron" aria-hidden>
          <ChevronRightGlyph />
        </span>
      </button>
      {summary ? null : (
        <span className="mn-toggle-block__placeholder" contentEditable={false} aria-hidden>
          {m.summaryPlaceholder}
        </span>
      )}
      {/* Summary and content are the node's two children; closed content is hidden with CSS so ProseMirror keeps it. */}
      <NodeViewContent className="mn-toggle-block__inner" />
    </NodeViewWrapper>
  );
}

/**
 * The Tiptap node for ToggleBlock: `toggleBlock` with a `toggleSummary`
 * (inline text) and a `toggleContent` (blocks), and an `open` attribute that
 * is saved with the document. Renders `<div data-type="toggle-block"
 * data-open="true">` in HTML. Adds `setToggleBlock`; Enter in the summary
 * opens the toggle and moves into its content.
 */
export const ToggleBlockExtension = Node.create<ToggleBlockExtensionOptions>({
  name: 'toggleBlock',
  group: 'block',
  content: 'toggleSummary toggleContent',
  defining: true,
  addOptions() {
    return { messages: undefined, HTMLAttributes: {} };
  },
  addExtensions() {
    return [ToggleSummaryNode, ToggleContentNode];
  },
  addAttributes() {
    return {
      open: {
        default: true,
        parseHTML: (el: HTMLElement) => el.getAttribute('data-open') !== 'false',
        renderHTML: (attrs: { open?: boolean }) => ({
          'data-open': attrs.open === false ? 'false' : 'true',
        }),
      },
    };
  },
  parseHTML() {
    return [{ tag: 'div[data-type="toggle-block"]' }];
  },
  renderHTML({ HTMLAttributes }) {
    return [
      'div',
      mergeAttributes(this.options.HTMLAttributes, HTMLAttributes, {
        'data-type': 'toggle-block',
      }),
      0,
    ];
  },
  addCommands() {
    return {
      setToggleBlock:
        () =>
        ({ state, tr, dispatch }) => {
          const { $from } = state.selection;
          const { schema } = state;
          const toggle = schema.nodes[this.name];
          const summaryType = schema.nodes.toggleSummary;
          const contentType = schema.nodes.toggleContent;
          const paragraph = schema.nodes.paragraph;
          if (!toggle || !summaryType || !contentType || !paragraph) return false;
          const parent = $from.parent;
          const fromParagraph = parent.type === paragraph;
          const node = toggle.create({ open: true }, [
            summaryType.create(null, fromParagraph ? parent.content : undefined),
            contentType.create(null, paragraph.create()),
          ]);
          if (dispatch) {
            if (fromParagraph) {
              const start = $from.before();
              tr.replaceWith(start, $from.after(), node);
              tr.setSelection(TextSelection.near(tr.doc.resolve(start + 2 + parent.content.size)));
            } else {
              const at = $from.depth ? $from.after(1) : $from.pos;
              tr.insert(at, node);
              tr.setSelection(TextSelection.near(tr.doc.resolve(at + 2)));
            }
            dispatch(tr.scrollIntoView());
          }
          return true;
        },
    };
  },
  addNodeView() {
    return ReactNodeViewRenderer(ToggleNodeView);
  },
});
