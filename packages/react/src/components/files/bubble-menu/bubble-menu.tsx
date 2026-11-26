'use client';

import {
  forwardRef,
  useCallback,
  useEffect,
  useReducer,
  useRef,
  useState,
  type CSSProperties,
  type HTMLAttributes,
  type KeyboardEvent,
  type ReactNode,
} from 'react';
import { useIsomorphicLayoutEffect } from '../../../internal/layout-effect.js';
import type { Editor } from '@tiptap/core';
import { cx } from '../../../internal/cx.js';
import {
  EDITOR_TOOLBAR_INLINE_ITEMS,
  EditorToolbar,
  type EditorToolbarMessages,
} from '../editor-toolbar/index.js';

const useIsoLayoutEffect = typeof window === 'undefined' ? useEffect : useIsomorphicLayoutEffect;

/** Every string the bubble menu shows or announces. Override them to translate. */
export interface BubbleMenuMessages {
  /** Name of the group around custom children. Default "Selection actions". */
  label: string;
  /** Strings for the default formatting toolbar. */
  toolbar?: Partial<EditorToolbarMessages> | undefined;
}

export const DEFAULT_BUBBLE_MENU_MESSAGES: BubbleMenuMessages = {
  label: 'Selection actions',
};

export interface BubbleMenuProps extends Omit<HTMLAttributes<HTMLDivElement>, 'children'> {
  /** The Tiptap editor whose selection the menu follows. Renders nothing while `null`. */
  editor: Editor | null;
  /**
   * What the menu holds. Default: an EditorToolbar with `EDITOR_TOOLBAR_INLINE_ITEMS`
   * (bold, italic, strikethrough, code, link). Custom children are wrapped in a
   * group named by `messages.label`.
   */
  children?: ReactNode;
  /**
   * Decides whether the menu shows for the current state. Default: the editor is
   * editable and has a non-empty text selection outside a code block.
   */
  shouldShow?: ((editor: Editor) => boolean) | undefined;
  /** Above the selection (default, flips below near the top of the viewport) or below it. */
  placement?: 'top' | 'bottom' | undefined;
  /** Gap between the selection and the menu, in pixels. Default 8. */
  offset?: number | undefined;
  /** Override any string. */
  messages?: Partial<BubbleMenuMessages> | undefined;
}

/** The default `shouldShow`: a non-empty text selection in an editable editor, not in code. */
export function defaultBubbleMenuShouldShow(editor: Editor): boolean {
  const { selection, doc } = editor.state;
  if (!editor.isEditable || selection.empty) return false;
  if (editor.isActive('codeBlock')) return false;
  // A node selection (an image, an embed) is not text to format.
  if ('node' in selection) return false;
  return doc.textBetween(selection.from, selection.to, ' ').trim().length > 0;
}

const VIEWPORT_MARGIN = 8;

/**
 * A small toolbar that floats over the selected text in a Tiptap editor, for
 * inline formatting or actions such as "Comment". It appears without taking
 * focus; Alt+F10 moves focus into it and Escape returns to the text with the
 * selection kept.
 */
export const BubbleMenu = forwardRef<HTMLDivElement, BubbleMenuProps>(function BubbleMenu(
  {
    editor,
    children,
    shouldShow = defaultBubbleMenuShouldShow,
    placement = 'top',
    offset = 8,
    messages: messagesProp,
    className,
    style,
    onKeyDown,
    ...rest
  },
  ref,
) {
  const m = { ...DEFAULT_BUBBLE_MENU_MESSAGES, ...messagesProp };
  const [, rerender] = useReducer((n: number) => n + 1, 0);
  const menu = useRef<HTMLDivElement | null>(null);
  const [position, setPosition] = useState<{ top: number; left: number; side: string } | null>(
    null,
  );
  // Escape in the text hides the menu until the selection changes.
  const [dismissed, setDismissed] = useState<string | null>(null);
  // Hidden while the pointer is still dragging out a selection.
  const [selecting, setSelecting] = useState(false);
  const shouldShowRef = useRef(shouldShow);
  shouldShowRef.current = shouldShow;

  const setRefs = useCallback(
    (node: HTMLDivElement | null) => {
      menu.current = node;
      if (typeof ref === 'function') ref(node);
      else if (ref) ref.current = node;
    },
    [ref],
  );

  const selectionKey = editor ? `${editor.state.selection.from}:${editor.state.selection.to}` : '';

  // Focus anywhere in the editor, the menu, or a popover the menu opened.
  const hasFocus = () => {
    if (!editor || typeof document === 'undefined') return false;
    const active = document.activeElement;
    if (!active || active === document.body) return false;
    return (
      editor.view.dom.contains(active) ||
      Boolean(menu.current?.contains(active)) ||
      Boolean(active.closest('.mn-editor-toolbar__link-popover, [data-mn-bubble-menu-keep]'))
    );
  };

  const visible =
    Boolean(editor) &&
    !editor?.isDestroyed &&
    dismissed !== selectionKey &&
    !selecting &&
    hasFocus() &&
    shouldShowRef.current(editor as Editor);

  useEffect(() => {
    if (!editor) return;
    editor.on('transaction', rerender);
    editor.on('focus', rerender);
    // Blur fires before focus lands; re-check once it has.
    const later = () => setTimeout(rerender, 0);
    editor.on('blur', later);
    document.addEventListener('focusin', rerender);
    window.addEventListener('scroll', rerender, true);
    window.addEventListener('resize', rerender);
    return () => {
      editor.off('transaction', rerender);
      editor.off('focus', rerender);
      editor.off('blur', later);
      document.removeEventListener('focusin', rerender);
      window.removeEventListener('scroll', rerender, true);
      window.removeEventListener('resize', rerender);
    };
  }, [editor]);

  // Alt+F10 from the text moves into the menu; Escape there hides it.
  const live = useRef({ visible, selectionKey });
  live.current = { visible, selectionKey };
  useEffect(() => {
    if (!editor) return;
    const dom = editor.view.dom;
    const onKey = (e: globalThis.KeyboardEvent) => {
      if (!live.current.visible) return;
      if (e.key === 'F10' && e.altKey) {
        e.preventDefault();
        const target =
          menu.current?.querySelector<HTMLElement>('[tabindex="0"]') ??
          menu.current?.querySelector<HTMLElement>('button, [href], input, [tabindex]');
        target?.focus();
      } else if (e.key === 'Escape') {
        setDismissed(live.current.selectionKey);
      }
    };
    const onDown = () => {
      setSelecting(true);
      document.addEventListener('pointerup', onUp, { once: true });
    };
    const onUp = () => setSelecting(false);
    dom.addEventListener('keydown', onKey);
    dom.addEventListener('pointerdown', onDown);
    return () => {
      dom.removeEventListener('keydown', onKey);
      dom.removeEventListener('pointerdown', onDown);
      document.removeEventListener('pointerup', onUp);
    };
  }, [editor]);

  useIsoLayoutEffect(() => {
    if (!visible || !editor || !menu.current) {
      if (position) setPosition(null);
      return;
    }
    const { from, to } = editor.state.selection;
    let start: { top: number; bottom: number; left: number };
    let end: { top: number; bottom: number; right: number };
    try {
      start = editor.view.coordsAtPos(from);
      end = editor.view.coordsAtPos(to, -1);
    } catch {
      return;
    }
    const rect = menu.current.getBoundingClientRect();
    const top = Math.min(start.top, end.top);
    const bottom = Math.max(start.bottom, end.bottom);
    const sameLine = Math.abs(start.top - end.top) < 2;
    const center = sameLine ? (start.left + end.right) / 2 : start.left + rect.width / 2;
    const roomAbove = top - rect.height - offset >= VIEWPORT_MARGIN;
    const side = placement === 'top' && roomAbove ? 'top' : 'bottom';
    const nextTop = side === 'top' ? top - rect.height - offset : bottom + offset;
    const maxLeft = window.innerWidth - rect.width - VIEWPORT_MARGIN;
    const nextLeft = Math.max(VIEWPORT_MARGIN, Math.min(center - rect.width / 2, maxLeft));
    if (
      !position ||
      position.top !== nextTop ||
      position.left !== nextLeft ||
      position.side !== side
    )
      setPosition({ top: nextTop, left: nextLeft, side });
  });

  if (!editor || !visible) return null;

  const handleKeyDown = (e: KeyboardEvent<HTMLDivElement>) => {
    onKeyDown?.(e);
    if (e.defaultPrevented || e.key !== 'Escape') return;
    e.preventDefault();
    // Back to the text; the selection is still there.
    editor.commands.focus();
  };

  return (
    // Escape and mouse-down handling for the controls inside; the controls are the keyboard targets.
    // eslint-disable-next-line jsx-a11y/no-static-element-interactions
    <div
      ref={setRefs}
      {...rest}
      {...(children ? { role: 'group', 'aria-label': m.label } : {})}
      className={cx('mn-bubble-menu', className)}
      data-side={position?.side ?? placement}
      data-positioned={position ? '' : undefined}
      style={
        {
          top: position?.top ?? 0,
          left: position?.left ?? 0,
          ...style,
        } as CSSProperties
      }
      onKeyDown={handleKeyDown}
      // Clicking the menu's padding must not move the editor's selection.
      onMouseDown={(e) => {
        if (e.target === e.currentTarget) e.preventDefault();
      }}
    >
      {children ?? (
        <EditorToolbar
          editor={editor}
          items={EDITOR_TOOLBAR_INLINE_ITEMS}
          messages={m.toolbar}
          className="mn-bubble-menu__toolbar"
        />
      )}
    </div>
  );
});
