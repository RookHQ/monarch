'use client';

import {
  forwardRef,
  useEffect,
  useReducer,
  useRef,
  useState,
  type FormEvent,
  type HTMLAttributes,
  type KeyboardEvent,
  type ReactNode,
  type SVGProps,
} from 'react';
import type { Editor } from '@tiptap/core';
import { Button } from '../../actions/button/index.js';
import { Field, FieldError, FieldLabel } from '../../forms/form-field/index.js';
import { Input } from '../../forms/text-field/index.js';
import { Popover, PopoverAnchor, PopoverContent } from '../../overlays/popover/index.js';
import { cx } from '../../../internal/cx.js';
import {
  BoldGlyph,
  BulletListGlyph,
  CodeBlockGlyph,
  CodeGlyph,
  HeadingGlyph,
  ItalicGlyph,
  LinkGlyph,
  OrderedListGlyph,
  QuoteGlyph,
  StrikeGlyph,
  TaskListGlyph,
} from '../../../internal/editor-glyphs.js';
import { normalizeLinkInput } from '../../../internal/sanitize.js';
import { useDirection } from '../../../internal/direction.js';

/** Heading levels the toolbar can offer. */
export type EditorToolbarHeadingLevel = 1 | 2 | 3 | 4;

/** The toolbar's own controls, by id. */
export type EditorToolbarBuiltInItem =
  | 'undo'
  | 'redo'
  | 'bold'
  | 'italic'
  | 'strike'
  | 'code'
  | 'link'
  | 'heading'
  | 'paragraph'
  | 'bulletList'
  | 'orderedList'
  | 'taskList'
  | 'blockquote'
  | 'codeBlock';

/** A product-owned control, such as "Comment" or "Insert callout". */
export interface EditorToolbarCustomItem {
  /** Stable id, also written to `data-tool`. */
  id: string;
  /** Accessible name and tooltip. */
  label: string;
  /** Decorative icon; the label is the name. */
  icon: ReactNode;
  /** Makes it a toggle button (`aria-pressed`) that reads its state from the editor. */
  isActive?: ((editor: Editor) => boolean) | undefined;
  /** Shown but cannot be used (`aria-disabled`), e.g. a command the selection does not allow. */
  isDisabled?: ((editor: Editor) => boolean) | undefined;
  /** Runs the control. */
  run: (editor: Editor) => void;
}

/** One toolbar entry: a built-in id, a custom control, or `'|'` for a group separator. */
export type EditorToolbarItem = EditorToolbarBuiltInItem | EditorToolbarCustomItem | '|';

/** Every built-in control, grouped. Controls whose node or mark is not in the schema are left out. */
export const EDITOR_TOOLBAR_DEFAULT_ITEMS: readonly EditorToolbarItem[] = [
  'undo',
  'redo',
  '|',
  'paragraph',
  'heading',
  '|',
  'bold',
  'italic',
  'strike',
  'code',
  'link',
  '|',
  'bulletList',
  'orderedList',
  'taskList',
  '|',
  'blockquote',
  'codeBlock',
];

/** Inline formatting only: what a selection toolbar (BubbleMenu) offers. */
export const EDITOR_TOOLBAR_INLINE_ITEMS: readonly EditorToolbarItem[] = [
  'bold',
  'italic',
  'strike',
  'code',
  '|',
  'link',
];

/** Every string the toolbar shows or announces. Override them to translate. */
export interface EditorToolbarMessages {
  /** Name of the toolbar. Default "Formatting". */
  label: string;
  undo: string;
  redo: string;
  bold: string;
  italic: string;
  strike: string;
  code: string;
  link: string;
  /** Label for one heading button. Default "Heading 2". */
  heading: (level: EditorToolbarHeadingLevel) => string;
  paragraph: string;
  bulletList: string;
  orderedList: string;
  taskList: string;
  blockquote: string;
  codeBlock: string;
  /** Link form field label. */
  linkUrl: string;
  linkApply: string;
  linkRemove: string;
  linkCancel: string;
  linkInvalid: string;
}

export const DEFAULT_EDITOR_TOOLBAR_MESSAGES: EditorToolbarMessages = {
  label: 'Formatting',
  undo: 'Undo',
  redo: 'Redo',
  bold: 'Bold',
  italic: 'Italic',
  strike: 'Strikethrough',
  code: 'Inline code',
  link: 'Link',
  heading: (level) => `Heading ${level}`,
  paragraph: 'Text',
  bulletList: 'Bulleted list',
  orderedList: 'Numbered list',
  taskList: 'Checklist',
  blockquote: 'Quote',
  codeBlock: 'Code block',
  linkUrl: 'Link address',
  linkApply: 'Apply',
  linkRemove: 'Remove link',
  linkCancel: 'Cancel',
  linkInvalid: 'Enter a web address (https://…) or an email address.',
};

export interface EditorToolbarProps extends Omit<HTMLAttributes<HTMLDivElement>, 'children'> {
  /** The Tiptap editor to format. While it is `null` (server, loading) the controls render inert. */
  editor: Editor | null;
  /** Controls in order, with `'|'` for separators. Default `EDITOR_TOOLBAR_DEFAULT_ITEMS`. */
  items?: readonly EditorToolbarItem[] | undefined;
  /** Heading levels the `heading` item expands to, one button each. Default `[1, 2, 3]`. */
  headingLevels?: readonly EditorToolbarHeadingLevel[] | undefined;
  /** Every control is inert (`aria-disabled`) but stays focusable. */
  disabled?: boolean | undefined;
  /** Controlled open state of the link form (for a ⌘/Ctrl+K shortcut in the editor). */
  linkOpen?: boolean | undefined;
  /** Called when the link form opens or closes. */
  onLinkOpenChange?: ((open: boolean) => void) | undefined;
  /** Override any string. */
  messages?: Partial<EditorToolbarMessages> | undefined;
}

const DEFAULT_LEVELS: readonly EditorToolbarHeadingLevel[] = [1, 2, 3];

const svg = {
  width: 16,
  height: 16,
  viewBox: '0 0 16 16',
  fill: 'none',
  stroke: 'currentColor',
  strokeWidth: 1.75,
  strokeLinecap: 'round',
  strokeLinejoin: 'round',
  'aria-hidden': true,
  focusable: false,
} as const;
function UndoGlyph(props: SVGProps<SVGSVGElement>) {
  return (
    <svg {...svg} {...props}>
      <path d="M5.5 3 2.5 6l3 3M2.5 6h7a4 4 0 0 1 0 8H7" />
    </svg>
  );
}
function RedoGlyph(props: SVGProps<SVGSVGElement>) {
  return (
    <svg {...svg} {...props}>
      <path d="M10.5 3l3 3-3 3M13.5 6h-7a4 4 0 0 0 0 8H9" />
    </svg>
  );
}
function ParagraphGlyph(props: SVGProps<SVGSVGElement>) {
  return (
    <svg {...svg} {...props}>
      <path d="M3 3.5h10M8 3.5v9M5.5 12.5h5" />
    </svg>
  );
}

/** Shortcut per built-in control, in `aria-keyshortcuts` form with `Mod` for ⌘ or Ctrl. */
const SHORTCUTS: Partial<Record<string, string>> = {
  undo: 'Mod+Z',
  redo: 'Mod+Shift+Z',
  bold: 'Mod+B',
  italic: 'Mod+I',
  strike: 'Mod+Shift+S',
  code: 'Mod+E',
  link: 'Mod+K',
  paragraph: 'Mod+Alt+0',
  'heading-1': 'Mod+Alt+1',
  'heading-2': 'Mod+Alt+2',
  'heading-3': 'Mod+Alt+3',
  'heading-4': 'Mod+Alt+4',
  orderedList: 'Mod+Shift+7',
  bulletList: 'Mod+Shift+8',
  taskList: 'Mod+Shift+9',
  blockquote: 'Mod+Shift+B',
  codeBlock: 'Mod+Alt+C',
};

/** "Mod+Shift+Z" as people read it: "⌘⇧Z" on Apple platforms, "Ctrl+Shift+Z" elsewhere. */
function shortcutHint(shortcut: string, mac: boolean): string {
  if (!mac) return shortcut.replace('Mod', 'Ctrl');
  const glyph: Record<string, string> = { Mod: '⌘', Shift: '⇧', Alt: '⌥' };
  return shortcut
    .split('+')
    .map((k) => glyph[k] ?? k)
    .join('');
}

/** The platform modifier, known only after mount so server and client markup agree. */
function useIsMac(): boolean | null {
  const [mac, setMac] = useState<boolean | null>(null);
  useEffect(() => {
    setMac(/Mac|iP(hone|ad)/.test(navigator.platform));
  }, []);
  return mac;
}

/** Re-renders on every editor transaction, so pressed states follow the caret. */
function useEditorState(editor: Editor | null) {
  const [, tick] = useReducer((n: number) => n + 1, 0);
  useEffect(() => {
    if (!editor) return;
    editor.on('transaction', tick);
    editor.on('focus', tick);
    editor.on('blur', tick);
    return () => {
      editor.off('transaction', tick);
      editor.off('focus', tick);
      editor.off('blur', tick);
    };
  }, [editor]);
}

interface Control {
  key: string;
  label: string;
  icon: ReactNode;
  /** null for plain buttons (no pressed state). */
  pressed: boolean | null;
  disabled: boolean;
  run: () => void;
}

function has(editor: Editor | null, name: string): boolean {
  if (!editor) return true;
  return Boolean(editor.schema.nodes[name] ?? editor.schema.marks[name]);
}

function buildControls(
  editor: Editor | null,
  items: readonly EditorToolbarItem[],
  levels: readonly EditorToolbarHeadingLevel[],
  m: EditorToolbarMessages,
  openLink: () => void,
): (Control | '|')[] {
  const out: (Control | '|')[] = [];
  const chain = () => editor?.chain().focus();
  const active = (name: string, attrs?: Record<string, unknown>) =>
    editor ? editor.isActive(name, attrs) : false;
  const toggle = (
    key: string,
    node: string,
    label: string,
    icon: ReactNode,
    run: () => void,
  ): void => {
    if (has(editor, node))
      out.push({ key, label, icon, pressed: active(node), disabled: false, run });
  };
  for (const item of items) {
    if (item === '|') {
      if (out.length && out[out.length - 1] !== '|') out.push('|');
      continue;
    }
    if (typeof item === 'object') {
      out.push({
        key: item.id,
        label: item.label,
        icon: item.icon,
        pressed: item.isActive ? (editor ? item.isActive(editor) : false) : null,
        disabled: editor ? Boolean(item.isDisabled?.(editor)) : false,
        run: () => {
          if (editor) item.run(editor);
        },
      });
      continue;
    }
    switch (item) {
      case 'undo':
      case 'redo':
        if (editor && !editor.extensionManager.extensions.some((e) => e.name === 'undoRedo')) break;
        out.push({
          key: item,
          label: m[item],
          icon: item === 'undo' ? <UndoGlyph /> : <RedoGlyph />,
          pressed: null,
          disabled: editor ? !(item === 'undo' ? editor.can().undo() : editor.can().redo()) : false,
          run: () => (item === 'undo' ? chain()?.undo().run() : chain()?.redo().run()),
        });
        break;
      case 'bold':
        toggle(item, item, m.bold, <BoldGlyph />, () => chain()?.toggleBold().run());
        break;
      case 'italic':
        toggle(item, item, m.italic, <ItalicGlyph />, () => chain()?.toggleItalic().run());
        break;
      case 'strike':
        toggle(item, item, m.strike, <StrikeGlyph />, () => chain()?.toggleStrike().run());
        break;
      case 'code':
        toggle(item, item, m.code, <CodeGlyph />, () => chain()?.toggleCode().run());
        break;
      case 'link':
        toggle(item, item, m.link, <LinkGlyph />, openLink);
        break;
      case 'paragraph':
        toggle(item, item, m.paragraph, <ParagraphGlyph />, () => chain()?.setParagraph().run());
        break;
      case 'heading':
        if (!has(editor, 'heading')) break;
        for (const level of levels)
          out.push({
            key: `heading-${level}`,
            label: m.heading(level),
            icon: <HeadingGlyph level={level} />,
            pressed: active('heading', { level }),
            disabled: false,
            run: () => chain()?.toggleHeading({ level }).run(),
          });
        break;
      case 'bulletList':
        toggle(item, item, m.bulletList, <BulletListGlyph />, () =>
          chain()?.toggleBulletList().run(),
        );
        break;
      case 'orderedList':
        toggle(item, item, m.orderedList, <OrderedListGlyph />, () =>
          chain()?.toggleOrderedList().run(),
        );
        break;
      case 'taskList':
        toggle(item, item, m.taskList, <TaskListGlyph />, () => chain()?.toggleTaskList().run());
        break;
      case 'blockquote':
        toggle(item, item, m.blockquote, <QuoteGlyph />, () => chain()?.toggleBlockquote().run());
        break;
      case 'codeBlock':
        toggle(item, item, m.codeBlock, <CodeBlockGlyph />, () => chain()?.toggleCodeBlock().run());
        break;
    }
  }
  while (out[out.length - 1] === '|') out.pop();
  return out;
}

/**
 * A formatting toolbar for any Tiptap editor: undo and redo, text styles,
 * headings, lists, quotes, code and links, plus product controls. Follows the
 * APG toolbar pattern (one Tab stop, arrow keys, Home and End) and keeps the
 * editor's selection when a button is clicked.
 */
export const EditorToolbar = forwardRef<HTMLDivElement, EditorToolbarProps>(function EditorToolbar(
  {
    editor,
    items = EDITOR_TOOLBAR_DEFAULT_ITEMS,
    headingLevels = DEFAULT_LEVELS,
    disabled = false,
    linkOpen: linkOpenProp,
    onLinkOpenChange,
    messages: messagesProp,
    className,
    onKeyDown,
    'aria-label': ariaLabel,
    ...rest
  },
  ref,
) {
  const readDir = useDirection();
  const m = { ...DEFAULT_EDITOR_TOOLBAR_MESSAGES, ...messagesProp };
  useEditorState(editor);
  const mac = useIsMac();
  const [linkOpenState, setLinkOpenState] = useState(false);
  const linkOpen = linkOpenProp ?? linkOpenState;
  const setLinkOpen = (open: boolean) => {
    setLinkOpenState(open);
    onLinkOpenChange?.(open);
  };
  const [active, setActive] = useState(0);
  const buttons = useRef<(HTMLButtonElement | null)[]>([]);
  const inert = disabled || !editor;
  const controls = buildControls(editor, items, headingLevels, m, () => setLinkOpen(true));
  const flat = controls.filter((c): c is Control => c !== '|');
  const current = Math.min(active, Math.max(flat.length - 1, 0));

  const handleKeyDown = (e: KeyboardEvent<HTMLDivElement>) => {
    onKeyDown?.(e);
    if (e.defaultPrevented) return;
    // Keys typed inside the link popover (portalled, but a React child) are not ours.
    if (!(e.target instanceof HTMLButtonElement) || !e.currentTarget.contains(e.target)) return;
    const n = flat.length;
    if (!n) return;
    const rtl = readDir(e.currentTarget) === 'rtl';
    let next: number | null = null;
    if (e.key === (rtl ? 'ArrowLeft' : 'ArrowRight')) next = (current + 1) % n;
    else if (e.key === (rtl ? 'ArrowRight' : 'ArrowLeft')) next = (current - 1 + n) % n;
    else if (e.key === 'Home') next = 0;
    else if (e.key === 'End') next = n - 1;
    if (next === null) return;
    e.preventDefault();
    setActive(next);
    buttons.current[next]?.focus();
  };

  let index = -1;
  return (
    <div
      ref={ref}
      role="toolbar"
      aria-label={ariaLabel ?? m.label}
      aria-orientation="horizontal"
      aria-disabled={disabled || undefined}
      {...rest}
      className={cx('mn-editor-toolbar', className)}
      data-disabled={disabled || undefined}
      data-loading={editor ? undefined : ''}
      onKeyDown={handleKeyDown}
    >
      {controls.map((c, i) => {
        if (c === '|')
          return (
            <span
              key={`sep-${i}`}
              role="separator"
              aria-orientation="vertical"
              className="mn-editor-toolbar__separator"
            />
          );
        index += 1;
        const at = index;
        const off = inert || c.disabled;
        const shortcut = SHORTCUTS[c.key];
        const keys =
          shortcut && mac !== null ? shortcut.replace('Mod', mac ? 'Meta' : 'Control') : undefined;
        const hint = shortcut && mac !== null ? shortcutHint(shortcut, mac) : undefined;
        const button = (
          <button
            key={c.key}
            ref={(el) => {
              buttons.current[at] = el;
            }}
            type="button"
            className="mn-editor-toolbar__tool"
            data-tool={c.key}
            aria-label={c.label}
            title={hint ? `${c.label} (${hint})` : c.label}
            aria-keyshortcuts={keys}
            aria-pressed={c.pressed === null ? undefined : c.pressed}
            aria-disabled={off || undefined}
            tabIndex={at === current ? 0 : -1}
            onMouseDown={(e) => e.preventDefault()}
            onFocus={() => setActive(at)}
            onClick={() => {
              if (!off) c.run();
            }}
          >
            {c.icon}
          </button>
        );
        if (c.key !== 'link' || !editor) return button;
        return (
          <LinkPopover
            key={c.key}
            editor={editor}
            open={linkOpen && !inert}
            onOpenChange={setLinkOpen}
            messages={m}
          >
            {button}
          </LinkPopover>
        );
      })}
    </div>
  );
});

function LinkPopover({
  editor,
  open,
  onOpenChange,
  messages: m,
  children,
}: {
  editor: Editor;
  open: boolean;
  onOpenChange: (open: boolean) => void;
  messages: EditorToolbarMessages;
  children: ReactNode;
}) {
  const current = (editor.getAttributes('link').href as string | undefined) ?? '';
  return (
    <Popover open={open} onOpenChange={onOpenChange}>
      <PopoverAnchor asChild>{children}</PopoverAnchor>
      <PopoverContent
        className="mn-editor-toolbar__link-popover"
        onFocusOutside={(e) => {
          if (editor.view.dom.contains(e.target as Node)) e.preventDefault();
        }}
        onCloseAutoFocus={(e) => {
          e.preventDefault();
          // The editor may be gone by the time the popover finishes closing.
          if (!editor.isDestroyed) editor.commands.focus();
        }}
      >
        {open ? (
          <LinkForm
            editor={editor}
            initial={current}
            messages={m}
            onDone={() => onOpenChange(false)}
          />
        ) : null}
      </PopoverContent>
    </Popover>
  );
}

function LinkForm({
  editor,
  initial,
  messages: m,
  onDone,
}: {
  editor: Editor;
  initial: string;
  messages: EditorToolbarMessages;
  onDone: () => void;
}) {
  const [value, setValue] = useState(initial);
  const [invalid, setInvalid] = useState(false);
  const unset = () => {
    editor.chain().focus().extendMarkRange('link').unsetLink().run();
    onDone();
  };
  const submit = (e: FormEvent) => {
    e.preventDefault();
    e.stopPropagation();
    if (!value.trim()) {
      unset();
      return;
    }
    const href = normalizeLinkInput(value);
    if (!href) {
      setInvalid(true);
      return;
    }
    if (editor.state.selection.empty && !editor.isActive('link')) {
      // No text selected: the address itself becomes the link text.
      editor
        .chain()
        .focus()
        .insertContent({
          type: 'text',
          text: value.trim(),
          marks: [{ type: 'link', attrs: { href } }],
        })
        .run();
    } else {
      editor.chain().focus().extendMarkRange('link').setLink({ href }).run();
    }
    onDone();
  };
  return (
    <form className="mn-editor-toolbar__link-form" onSubmit={submit} noValidate>
      <Field invalid={invalid}>
        <FieldLabel>{m.linkUrl}</FieldLabel>
        <Input
          type="text"
          inputMode="url"
          autoComplete="off"
          // eslint-disable-next-line jsx-a11y/no-autofocus -- the popover exists only to type this.
          autoFocus
          value={value}
          placeholder="https://"
          onChange={(e) => {
            setValue(e.target.value);
            setInvalid(false);
          }}
        />
        <FieldError>{invalid ? m.linkInvalid : null}</FieldError>
      </Field>
      <div className="mn-editor-toolbar__link-actions">
        {initial ? (
          <Button type="button" variant="ghost" size="sm" onClick={unset}>
            {m.linkRemove}
          </Button>
        ) : null}
        <Button type="button" variant="secondary" size="sm" onClick={onDone}>
          {m.linkCancel}
        </Button>
        <Button type="submit" variant="primary" size="sm">
          {m.linkApply}
        </Button>
      </div>
    </form>
  );
}
