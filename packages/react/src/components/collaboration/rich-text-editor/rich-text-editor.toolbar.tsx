'use client';

import { useRef, useState, type FormEvent, type KeyboardEvent, type ReactNode } from 'react';
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
  PaperclipGlyph,
  QuoteGlyph,
  StrikeGlyph,
  TaskListGlyph,
} from '../../../internal/editor-glyphs.js';
import { normalizeLinkInput } from '../../../internal/sanitize.js';
import type { RichTextHeadingLevel } from './rich-text-editor.extensions.js';

/** One toolbar control, or `'|'` for a group separator. */
export type RichTextToolbarItem =
  | 'bold'
  | 'italic'
  | 'strike'
  | 'code'
  | 'heading'
  | 'bulletList'
  | 'orderedList'
  | 'taskList'
  | 'link'
  | 'blockquote'
  | 'codeBlock'
  | 'file'
  | '|';

/** The default toolbar. Controls whose feature is off (no headings, no file hook) are left out. */
export const DEFAULT_TOOLBAR: readonly RichTextToolbarItem[] = [
  'bold',
  'italic',
  'strike',
  'code',
  '|',
  'heading',
  '|',
  'bulletList',
  'orderedList',
  'taskList',
  '|',
  'link',
  'blockquote',
  'codeBlock',
  '|',
  'file',
];

export interface RichTextEditorMessages {
  /** Name of the formatting toolbar. Default "Formatting". */
  toolbar: string;
  bold: string;
  italic: string;
  strike: string;
  code: string;
  /** Label for a heading button. Default "Heading 2". */
  heading: (level: RichTextHeadingLevel) => string;
  bulletList: string;
  orderedList: string;
  taskList: string;
  link: string;
  blockquote: string;
  codeBlock: string;
  file: string;
  /** Link popover. */
  linkUrl: string;
  linkApply: string;
  linkRemove: string;
  linkCancel: string;
  linkInvalid: string;
  /** Visible counter. Default "120 / 500" or "120 characters". */
  count: (count: number, max: number | undefined) => string;
  /** Announced when few characters are left. Default "20 characters left". */
  remaining: (left: number) => string;
  /** Suggestion list states. */
  suggestions: string;
  suggestionsLoading: string;
  suggestionsEmpty: string;
  suggestionsError: string;
  /** Shown on the server and until the editor has loaded. */
  loading: string;
}

const MOD =
  typeof navigator !== 'undefined' && /Mac|iP(hone|ad)/.test(navigator.platform) ? '⌘' : 'Ctrl+';

export const DEFAULT_MESSAGES: RichTextEditorMessages = {
  toolbar: 'Formatting',
  bold: `Bold (${MOD}B)`,
  italic: `Italic (${MOD}I)`,
  strike: 'Strikethrough',
  code: 'Inline code',
  heading: (level) => `Heading ${level}`,
  bulletList: 'Bulleted list',
  orderedList: 'Numbered list',
  taskList: 'Checklist',
  link: `Link (${MOD}K)`,
  blockquote: 'Quote',
  codeBlock: 'Code block',
  file: 'Insert image or file',
  linkUrl: 'Link address',
  linkApply: 'Apply',
  linkRemove: 'Remove link',
  linkCancel: 'Cancel',
  linkInvalid: 'Enter a web address (https://…) or an email address.',
  count: (count, max) =>
    max ? `${count} / ${max}` : `${count} ${count === 1 ? 'character' : 'characters'}`,
  remaining: (left) =>
    left < 0
      ? `${-left} ${left === -1 ? 'character' : 'characters'} over the limit`
      : `${left} ${left === 1 ? 'character' : 'characters'} left`,
  suggestions: 'Suggestions',
  suggestionsLoading: 'Searching…',
  suggestionsEmpty: 'No matches',
  suggestionsError: 'Couldn’t load suggestions.',
  loading: 'Loading editor…',
};

interface ToolbarControl {
  key: string;
  label: string;
  icon: ReactNode;
  /** null for plain buttons (no pressed state). */
  pressed: boolean | null;
  run: () => void;
}

function controlsFor(
  editor: Editor,
  items: readonly RichTextToolbarItem[],
  m: RichTextEditorMessages,
  headingLevels: readonly RichTextHeadingLevel[],
  actions: { openLink: () => void; pickFile: (() => void) | undefined },
): (ToolbarControl | '|')[] {
  const has = (name: string) => Boolean(editor.schema.nodes[name] ?? editor.schema.marks[name]);
  const chain = () => editor.chain().focus();
  const out: (ToolbarControl | '|')[] = [];
  for (const item of items) {
    switch (item) {
      case '|':
        if (out.length && out[out.length - 1] !== '|') out.push('|');
        break;
      case 'bold':
      case 'italic':
      case 'strike':
      case 'code': {
        if (!has(item)) break;
        const icon = { bold: BoldGlyph, italic: ItalicGlyph, strike: StrikeGlyph, code: CodeGlyph }[
          item
        ];
        const Icon = icon;
        out.push({
          key: item,
          label: m[item],
          icon: <Icon />,
          pressed: editor.isActive(item),
          run: () => {
            const c = chain();
            if (item === 'bold') c.toggleBold().run();
            else if (item === 'italic') c.toggleItalic().run();
            else if (item === 'strike') c.toggleStrike().run();
            else c.toggleCode().run();
          },
        });
        break;
      }
      case 'heading':
        if (!has('heading')) break;
        for (const level of headingLevels)
          out.push({
            key: `heading-${level}`,
            label: m.heading(level),
            icon: <HeadingGlyph level={level} />,
            pressed: editor.isActive('heading', { level }),
            run: () => chain().toggleHeading({ level }).run(),
          });
        break;
      case 'bulletList':
        if (has('bulletList'))
          out.push({
            key: item,
            label: m.bulletList,
            icon: <BulletListGlyph />,
            pressed: editor.isActive('bulletList'),
            run: () => chain().toggleBulletList().run(),
          });
        break;
      case 'orderedList':
        if (has('orderedList'))
          out.push({
            key: item,
            label: m.orderedList,
            icon: <OrderedListGlyph />,
            pressed: editor.isActive('orderedList'),
            run: () => chain().toggleOrderedList().run(),
          });
        break;
      case 'taskList':
        if (has('taskList'))
          out.push({
            key: item,
            label: m.taskList,
            icon: <TaskListGlyph />,
            pressed: editor.isActive('taskList'),
            run: () => chain().toggleTaskList().run(),
          });
        break;
      case 'link':
        if (has('link'))
          out.push({
            key: item,
            label: m.link,
            icon: <LinkGlyph />,
            pressed: editor.isActive('link'),
            run: actions.openLink,
          });
        break;
      case 'blockquote':
        if (has('blockquote'))
          out.push({
            key: item,
            label: m.blockquote,
            icon: <QuoteGlyph />,
            pressed: editor.isActive('blockquote'),
            run: () => chain().toggleBlockquote().run(),
          });
        break;
      case 'codeBlock':
        if (has('codeBlock'))
          out.push({
            key: item,
            label: m.codeBlock,
            icon: <CodeBlockGlyph />,
            pressed: editor.isActive('codeBlock'),
            run: () => chain().toggleCodeBlock().run(),
          });
        break;
      case 'file':
        if (actions.pickFile)
          out.push({
            key: item,
            label: m.file,
            icon: <PaperclipGlyph />,
            pressed: null,
            run: actions.pickFile,
          });
        break;
    }
  }
  while (out[out.length - 1] === '|') out.pop();
  return out;
}

export interface EditorToolbarProps {
  editor: Editor;
  items: readonly RichTextToolbarItem[];
  headingLevels: readonly RichTextHeadingLevel[];
  messages: RichTextEditorMessages;
  disabled: boolean;
  linkOpen: boolean;
  onLinkOpenChange: (open: boolean) => void;
  pickFile: (() => void) | undefined;
}

/**
 * The APG toolbar pattern: one Tab stop, Left/Right (and Home/End) move
 * between controls, toggles expose `aria-pressed`. Buttons keep the editor's
 * selection by not taking focus on mouse down.
 */
export function EditorToolbar({
  editor,
  items,
  headingLevels,
  messages: m,
  disabled,
  linkOpen,
  onLinkOpenChange,
  pickFile,
}: EditorToolbarProps) {
  const [active, setActive] = useState(0);
  const buttons = useRef<(HTMLButtonElement | null)[]>([]);
  const controls = controlsFor(editor, items, m, headingLevels, {
    openLink: () => onLinkOpenChange(true),
    pickFile,
  });
  const flat = controls.filter((c): c is ToolbarControl => c !== '|');
  const current = Math.min(active, Math.max(flat.length - 1, 0));

  const onKeyDown = (e: KeyboardEvent<HTMLDivElement>) => {
    const n = flat.length;
    if (!n) return;
    let next: number | null = null;
    const rtl = getComputedStyle(e.currentTarget).direction === 'rtl';
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
      role="toolbar"
      aria-label={m.toolbar}
      aria-orientation="horizontal"
      aria-disabled={disabled || undefined}
      className="mn-rich-text-editor__toolbar"
      onKeyDown={onKeyDown}
    >
      {controls.map((c, i) => {
        if (c === '|')
          return (
            <span
              key={`sep-${i}`}
              role="separator"
              aria-orientation="vertical"
              className="mn-rich-text-editor__separator"
            />
          );
        index += 1;
        const at = index;
        const button = (
          <button
            key={c.key}
            ref={(el) => {
              buttons.current[at] = el;
            }}
            type="button"
            className="mn-rich-text-editor__tool"
            data-tool={c.key}
            aria-label={c.label}
            title={c.label}
            aria-pressed={c.pressed === null ? undefined : c.pressed}
            aria-disabled={disabled || undefined}
            tabIndex={at === current ? 0 : -1}
            onMouseDown={(e) => e.preventDefault()}
            onFocus={() => setActive(at)}
            onClick={() => {
              if (!disabled) c.run();
            }}
          >
            {c.icon}
          </button>
        );
        if (c.key !== 'link') return button;
        return (
          <LinkPopover
            key={c.key}
            editor={editor}
            open={linkOpen}
            onOpenChange={onLinkOpenChange}
            messages={m}
          >
            {button}
          </LinkPopover>
        );
      })}
    </div>
  );
}

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
  messages: RichTextEditorMessages;
  children: ReactNode;
}) {
  const current = (editor.getAttributes('link').href as string | undefined) ?? '';
  return (
    <Popover open={open} onOpenChange={onOpenChange}>
      <PopoverAnchor asChild>{children}</PopoverAnchor>
      <PopoverContent
        className="mn-rich-text-editor__link-popover"
        // Opening from the editor (Mod+K) can briefly leave focus in the
        // text; only a click elsewhere or Escape should close the popover.
        onFocusOutside={(e) => {
          if (editor.view.dom.contains(e.target as Node)) e.preventDefault();
        }}
        onCloseAutoFocus={(e) => {
          e.preventDefault();
          editor.commands.focus();
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
  messages: RichTextEditorMessages;
  onDone: () => void;
}) {
  const [value, setValue] = useState(initial);
  const [invalid, setInvalid] = useState(false);
  const submit = (e: FormEvent) => {
    e.preventDefault();
    e.stopPropagation();
    if (!value.trim()) {
      editor.chain().focus().extendMarkRange('link').unsetLink().run();
      onDone();
      return;
    }
    const href = normalizeLinkInput(value);
    if (!href) {
      setInvalid(true);
      return;
    }
    const { empty } = editor.state.selection;
    if (empty && !editor.isActive('link')) {
      // No text selected: insert the address itself as the link text.
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
    <form className="mn-rich-text-editor__link-form" onSubmit={submit} noValidate>
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
      <div className={cx('mn-rich-text-editor__link-actions')}>
        {initial ? (
          <Button
            type="button"
            variant="ghost"
            size="sm"
            onClick={() => {
              editor.chain().focus().extendMarkRange('link').unsetLink().run();
              onDone();
            }}
          >
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
