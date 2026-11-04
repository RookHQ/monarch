'use client';

import {
  forwardRef,
  useEffect,
  useId,
  useImperativeHandle,
  useMemo,
  useRef,
  useState,
  type CSSProperties,
  type HTMLAttributes,
  type ReactNode,
  type Ref,
} from 'react';
import type { Editor, Extensions, JSONContent } from '@tiptap/core';
import { EditorContent, useEditor } from '@tiptap/react';
import { cx } from '../../../internal/cx.js';
import { cleanPastedHtml, isSafeImageSrc, normalizeLinkInput } from '../../../internal/sanitize.js';
import { buildEditorKit, type RichTextHeadingLevel } from './rich-text-editor.extensions.js';
import { useMentionBridge, type RichTextMentionOptions } from './rich-text-editor.mentions.js';
import {
  DEFAULT_MESSAGES,
  DEFAULT_TOOLBAR,
  EditorToolbar,
  type RichTextEditorMessages,
  type RichTextToolbarItem,
} from './rich-text-editor.toolbar.js';

export type { RichTextHeadingLevel } from './rich-text-editor.extensions.js';
export type {
  RichTextMentionItem,
  RichTextMentionOptions,
  RichTextMentionSearch,
} from './rich-text-editor.mentions.js';
export {
  DEFAULT_TOOLBAR as RICH_TEXT_DEFAULT_TOOLBAR,
  type RichTextEditorMessages,
  type RichTextToolbarItem,
} from './rich-text-editor.toolbar.js';
/** The Tiptap editor behind a RichTextEditor (`editorRef.current.editor`). */
export type RichTextEditorInstance = Editor;
/** Tiptap / ProseMirror document JSON. */
export type RichTextJSON = JSONContent;

/** HTML string or Tiptap/ProseMirror JSON, depending on `format`. */
export type RichTextValue = string | JSONContent;

/** What to put in the document for a file the product has uploaded. */
export type RichTextFileInsert =
  | { type: 'image'; src: string; alt?: string; title?: string }
  | { type: 'link'; href: string; text: string };

/**
 * Receives files pasted, dropped or picked with the toolbar's file button.
 * Upload them, then return what to insert (or nothing, when the product shows
 * them elsewhere, such as an attachment row).
 */
export type RichTextFileHandler = (
  files: File[],
  source: 'paste' | 'drop' | 'picker',
) =>
  // `void` so a handler that only uploads (returns nothing) type-checks.
  // eslint-disable-next-line @typescript-eslint/no-invalid-void-type
  void | readonly RichTextFileInsert[] | Promise<void | readonly RichTextFileInsert[]>;

export interface RichTextChangeDetails {
  /** Plain text of the document, mentions as "@Label". */
  text: string;
  isEmpty: boolean;
  /** Characters counted against `maxLength`. */
  characters: number;
}

/** Imperative access for composers and product shortcuts. */
export interface RichTextEditorHandle {
  /** The Tiptap editor, or null on the server and before it loads. */
  readonly editor: Editor | null;
  focus: (position?: 'start' | 'end') => void;
  /** Empties the document (fires `onValueChange`). */
  clear: () => void;
  getHTML: () => string;
  getJSON: () => JSONContent | null;
  getText: () => string;
  isEmpty: () => boolean;
  /** Replaces the document without firing `onValueChange`. */
  setContent: (value: RichTextValue) => void;
  /** Inserts plain text at the caret (an emoji, a canned phrase). */
  insertText: (text: string) => void;
  /** Inserts HTML or JSON at the caret, parsed against the schema. */
  insertContent: (content: RichTextValue) => void;
}

export interface RichTextEditorProps extends Omit<
  HTMLAttributes<HTMLDivElement>,
  'defaultValue' | 'onChange' | 'children' | 'placeholder'
> {
  /** Controlled document, in `format`. */
  value?: RichTextValue;
  /** Initial document when uncontrolled, in `format`. */
  defaultValue?: RichTextValue;
  /**
   * `html` (default) emits an HTML string, `''` when empty; `json` emits
   * Tiptap JSON. JSON round-trips losslessly and is cheaper to diff; HTML is
   * what most APIs store and render. Sanitise either on the server.
   */
  format?: 'html' | 'json';
  /** Fires on every change with the document in `format`. */
  onValueChange?: (value: RichTextValue, details: RichTextChangeDetails) => void;
  /** Shown in the empty document. */
  placeholder?: string;
  /** Shows the document without the toolbar; nothing can be changed. */
  readOnly?: boolean;
  /** Not editable, toolbar inert; the document stays visible. */
  disabled?: boolean;
  /** Danger border, and `aria-invalid` on the editing surface. */
  invalid?: boolean;
  /** Focus the end of the document when the editor loads. */
  autoFocus?: boolean;
  /** Toolbar controls in order, or `false` for none. Default `RICH_TEXT_DEFAULT_TOOLBAR`. */
  toolbar?: readonly RichTextToolbarItem[] | false;
  /** Toolbar above (default) or below the text. */
  toolbarPosition?: 'top' | 'bottom';
  /** Content at the trailing end of the toolbar row, outside the toolbar's arrow-key group. */
  toolbarEnd?: ReactNode;
  /** Heading levels offered and allowed. Default `[2, 3]`; `[]` removes headings. */
  headingLevels?: readonly RichTextHeadingLevel[];
  /** Allow checklists. Default true. */
  taskList?: boolean;
  /** Typing stops at this many characters. */
  maxLength?: number;
  /** Show the character counter. Default: shown when `maxLength` is set. */
  showCharacterCount?: boolean;
  /** Upload hook for pasted, dropped and picked files. Adds the toolbar's file button. */
  onFiles?: RichTextFileHandler;
  /** `accept` for the file picker, e.g. `image/*`. */
  fileAccept?: string;
  /** Allow images in the document. Default: true when `onFiles` is set. */
  images?: boolean;
  /** @mentions from a product search. */
  mentions?: RichTextMentionOptions;
  /**
   * Runs before the editor's own key handling. Return true to stop it, for
   * example Enter-to-send. Not called while a suggestion list is open.
   */
  onEditorKeyDown?: (event: KeyboardEvent, editor: Editor) => boolean | undefined;
  /** Extra Tiptap extensions the product owns (custom nodes, marks, shortcuts). Change identity only to rebuild. */
  extensions?: Extensions;
  /** Minimum height of the text area. Default 6rem. */
  minHeight?: CSSProperties['minHeight'];
  /** The text area scrolls above this height. Default none. */
  maxHeight?: CSSProperties['maxHeight'];
  /** Submits the HTML with a form under this name. */
  name?: string;
  /** Imperative handle (focus, clear, insertText, getHTML, …). */
  editorRef?: Ref<RichTextEditorHandle>;
  messages?: Partial<RichTextEditorMessages>;
}

const NO_HEADINGS: readonly RichTextHeadingLevel[] = [];
const DEFAULT_HEADINGS: readonly RichTextHeadingLevel[] = [2, 3];

const asKey = (v: RichTextValue | undefined) =>
  v === undefined ? undefined : typeof v === 'string' ? v : JSON.stringify(v);

/**
 * A Tiptap rich-text editor themed by Monarch tokens: an accessible
 * formatting toolbar, Markdown shortcuts, safe paste, links, checklists,
 * @mentions, an upload hook and a character limit. Renders a static
 * placeholder on the server; the editor loads after hydration.
 */
export const RichTextEditor = forwardRef<HTMLDivElement, RichTextEditorProps>(
  function RichTextEditor(
    {
      value,
      defaultValue,
      format = 'html',
      onValueChange,
      placeholder = '',
      readOnly = false,
      disabled = false,
      invalid = false,
      autoFocus = false,
      toolbar = DEFAULT_TOOLBAR,
      toolbarPosition = 'top',
      toolbarEnd,
      headingLevels = DEFAULT_HEADINGS,
      taskList = true,
      maxLength,
      showCharacterCount,
      onFiles,
      fileAccept,
      images,
      mentions,
      onEditorKeyDown,
      extensions,
      minHeight,
      maxHeight,
      name,
      editorRef,
      messages: messagesProp,
      className,
      style,
      'aria-label': ariaLabel,
      'aria-labelledby': ariaLabelledBy,
      'aria-describedby': ariaDescribedBy,
      ...rest
    },
    ref,
  ) {
    const m = useMemo(() => ({ ...DEFAULT_MESSAGES, ...messagesProp }), [messagesProp]);
    const baseId = `mn-rte-${useId().replace(/:/g, '')}`;
    const countId = `${baseId}-count`;
    const [linkOpen, setLinkOpen] = useState(false);
    const [html, setHtml] = useState('');
    const [characters, setCharacters] = useState(0);
    const fileInput = useRef<HTMLInputElement>(null);
    const mention = useMentionBridge(mentions, m);

    // Latest props for the editor's callbacks, which are bound once.
    const hasLinkTool = toolbar !== false && toolbar.includes('link') && !readOnly;
    const live = useRef({ onValueChange, onEditorKeyDown, onFiles, format, mention, hasLinkTool });
    live.current = { onValueChange, onEditorKeyDown, onFiles, format, mention, hasLinkTool };
    const lastEmitted = useRef(asKey(value ?? defaultValue));
    const editorInstance = useRef<Editor | null>(null);

    const allowImages = images ?? Boolean(onFiles);
    const levels = headingLevels.length ? headingLevels : NO_HEADINGS;
    const trigger = mentions ? (mentions.trigger ?? '@') : null;
    const kitKey = JSON.stringify([levels, taskList, allowImages, trigger, maxLength]);
    const showCount = showCharacterCount ?? maxLength !== undefined;

    // Read-only content is plain content: no textbox role, so no textbox ARIA
    // (the root becomes a named group instead when a label is given).
    const attributes = {
      role: readOnly ? undefined : 'textbox',
      'aria-multiline': readOnly ? undefined : 'true',
      'aria-label': readOnly ? undefined : ariaLabel,
      'aria-labelledby': readOnly ? undefined : ariaLabelledBy,
      'aria-describedby': readOnly
        ? undefined
        : [ariaDescribedBy, showCount && maxLength ? countId : undefined]
            .filter(Boolean)
            .join(' ') || undefined,
      'aria-disabled': disabled ? 'true' : undefined,
      'aria-invalid': invalid ? 'true' : undefined,
      class: 'mn-rich-text-editor__prose',
      ...(mention?.fieldAttributes ?? {}),
    };
    const placeholderRef = useRef(placeholder);
    placeholderRef.current = placeholder;
    const attrsRef = useRef(attributes);
    attrsRef.current = attributes;

    const insertResults = (results: unknown) => {
      const ed = editorInstance.current;
      if (!ed || ed.isDestroyed || !Array.isArray(results)) return;
      // One insertion, so a just-inserted image is never replaced by the next item.
      const content: JSONContent[] = [];
      for (const r of results as readonly RichTextFileInsert[]) {
        if (r.type === 'image') {
          if (ed.schema.nodes.image && isSafeImageSrc(r.src))
            content.push({
              type: 'image',
              attrs: { src: r.src, alt: r.alt ?? null, title: r.title ?? null },
            });
        } else {
          const href = normalizeLinkInput(r.href);
          if (href)
            content.push(
              { type: 'text', text: r.text, marks: [{ type: 'link', attrs: { href } }] },
              { type: 'text', text: ' ' },
            );
        }
      }
      if (content.length) ed.chain().focus().insertContent(content).run();
    };
    const handleFiles = (files: File[], source: 'paste' | 'drop' | 'picker') => {
      const fn = live.current.onFiles;
      if (!fn || files.length === 0) return false;
      void Promise.resolve(fn(files, source)).then(insertResults, () => undefined);
      return true;
    };
    const handleFilesRef = useRef(handleFiles);
    handleFilesRef.current = handleFiles;

    const emit = (ed: Editor) => {
      const isEmpty = ed.isEmpty;
      const count = (ed.storage.characterCount?.characters?.() as number | undefined) ?? 0;
      setCharacters(count);
      const htmlOut = isEmpty ? '' : ed.getHTML();
      setHtml(htmlOut);
      const out: RichTextValue = live.current.format === 'json' ? ed.getJSON() : htmlOut;
      lastEmitted.current = asKey(out);
      live.current.onValueChange?.(out, { text: ed.getText(), isEmpty, characters: count });
    };

    const editor = useEditor(
      {
        immediatelyRender: false,
        shouldRerenderOnTransaction: true,
        editable: !readOnly && !disabled,
        autofocus: autoFocus ? 'end' : false,
        // On a rebuild (kit options changed) keep what is already written.
        content: editorInstance.current?.getJSON() ?? value ?? defaultValue ?? '',
        extensions: [
          ...buildEditorKit({
            placeholder: () => placeholderRef.current,
            headingLevels: levels,
            maxLength,
            taskList,
            images: allowImages,
            mention: mention
              ? { trigger: trigger ?? '@', suggestion: mention.suggestion }
              : undefined,
          }),
          ...(extensions ?? []),
        ],
        editorProps: {
          attributes: () =>
            Object.fromEntries(
              Object.entries(attrsRef.current).filter(([, v]) => v !== undefined),
            ) as Record<string, string>,
          transformPastedHTML: cleanPastedHtml,
          handleKeyDown: (_view, event) => {
            const ed = editorInstance.current;
            if (!ed || live.current.mention?.isOpen()) return false;
            if (
              live.current.hasLinkTool &&
              (event.metaKey || event.ctrlKey) &&
              !event.altKey &&
              event.key.toLowerCase() === 'k'
            ) {
              event.preventDefault();
              setLinkOpen(true);
              return true;
            }
            if (live.current.onEditorKeyDown?.(event, ed) === true) {
              event.preventDefault();
              return true;
            }
            return false;
          },
          handlePaste: (_view, event) => {
            const files = Array.from(event.clipboardData?.files ?? []);
            if (handleFilesRef.current(files, 'paste')) {
              event.preventDefault();
              return true;
            }
            return false;
          },
          handleDrop: (_view, event, _slice, moved) => {
            if (moved) return false;
            const files = Array.from((event as DragEvent).dataTransfer?.files ?? []);
            if (handleFilesRef.current(files, 'drop')) {
              event.preventDefault();
              return true;
            }
            return false;
          },
        },
        onCreate: ({ editor: ed }) => {
          editorInstance.current = ed;
          setCharacters((ed.storage.characterCount?.characters?.() as number | undefined) ?? 0);
          setHtml(ed.isEmpty ? '' : ed.getHTML());
        },
        onUpdate: ({ editor: ed }) => emit(ed),
        onDestroy: () => {
          editorInstance.current = null;
        },
      },
      [kitKey, extensions],
    );
    editorInstance.current = editor;

    // Editable follows readOnly / disabled without rebuilding.
    useEffect(() => {
      if (editor && editor.isEditable !== (!readOnly && !disabled))
        editor.setEditable(!readOnly && !disabled, false);
    }, [editor, readOnly, disabled]);

    // ARIA on the editing surface follows props and the suggestion list.
    const attrKey = JSON.stringify([attributes, placeholder]);
    useEffect(() => {
      if (editor && !editor.isDestroyed) editor.view.updateState(editor.view.state);
    }, [editor, attrKey]);

    // Controlled value: apply outside changes, ignore our own echoes.
    useEffect(() => {
      if (!editor || value === undefined) return;
      const incoming = asKey(value);
      if (incoming === lastEmitted.current) return;
      const current =
        format === 'json'
          ? JSON.stringify(editor.getJSON())
          : editor.isEmpty
            ? ''
            : editor.getHTML();
      lastEmitted.current = incoming;
      if (incoming === current) return;
      editor.commands.setContent(value, false);
      setCharacters((editor.storage.characterCount?.characters?.() as number | undefined) ?? 0);
      setHtml(editor.isEmpty ? '' : editor.getHTML());
    }, [editor, value, format]);

    useImperativeHandle(
      editorRef,
      (): RichTextEditorHandle => ({
        get editor() {
          return editorInstance.current;
        },
        focus: (position = 'end') => editorInstance.current?.commands.focus(position),
        clear: () => {
          editorInstance.current?.commands.clearContent(true);
        },
        getHTML: () => {
          const ed = editorInstance.current;
          return !ed || ed.isEmpty ? '' : ed.getHTML();
        },
        getJSON: () => editorInstance.current?.getJSON() ?? null,
        getText: () => editorInstance.current?.getText() ?? '',
        isEmpty: () => editorInstance.current?.isEmpty ?? true,
        setContent: (v) => {
          const ed = editorInstance.current;
          if (!ed) return;
          ed.commands.setContent(v, false);
          lastEmitted.current = asKey(v);
          setCharacters((ed.storage.characterCount?.characters?.() as number | undefined) ?? 0);
          setHtml(ed.isEmpty ? '' : ed.getHTML());
        },
        insertText: (text) => {
          editorInstance.current?.chain().focus().insertContent({ type: 'text', text }).run();
        },
        insertContent: (content) => {
          editorInstance.current?.chain().focus().insertContent(content).run();
        },
      }),
      [],
    );

    const editable = !readOnly;
    const items = toolbar === false || !editable ? null : toolbar;
    const left = maxLength !== undefined ? maxLength - characters : null;
    const near = left !== null && maxLength !== undefined && left <= Math.max(10, maxLength * 0.1);
    const toolbarRow =
      items || toolbarEnd ? (
        <div className="mn-rich-text-editor__bar" data-position={toolbarPosition}>
          {items && editor ? (
            <EditorToolbar
              editor={editor}
              items={items}
              headingLevels={levels}
              messages={m}
              disabled={disabled}
              linkOpen={linkOpen}
              onLinkOpenChange={setLinkOpen}
              pickFile={onFiles && !disabled ? () => fileInput.current?.click() : undefined}
            />
          ) : (
            <div className="mn-rich-text-editor__toolbar" aria-hidden data-placeholder="" />
          )}
          {toolbarEnd ? <div className="mn-rich-text-editor__bar-end">{toolbarEnd}</div> : null}
        </div>
      ) : null;

    return (
      <div
        ref={ref}
        {...rest}
        {...(readOnly && (ariaLabel || ariaLabelledBy)
          ? { role: 'group', 'aria-label': ariaLabel, 'aria-labelledby': ariaLabelledBy }
          : {})}
        className={cx('mn-rich-text-editor', className)}
        style={
          {
            ...(minHeight !== undefined
              ? {
                  '--mn-rich-text-editor-min-height':
                    typeof minHeight === 'number' ? `${minHeight}px` : minHeight,
                }
              : {}),
            ...(maxHeight !== undefined
              ? {
                  '--mn-rich-text-editor-max-height':
                    typeof maxHeight === 'number' ? `${maxHeight}px` : maxHeight,
                }
              : {}),
            ...style,
          } as CSSProperties
        }
        data-readonly={readOnly || undefined}
        data-disabled={disabled || undefined}
        data-invalid={invalid || undefined}
        data-loading={editor ? undefined : ''}
      >
        {toolbarPosition === 'top' ? toolbarRow : null}
        {/* A pointer convenience only: the text itself is the keyboard target. */}
        {/* eslint-disable-next-line jsx-a11y/no-static-element-interactions */}
        <div
          className="mn-rich-text-editor__content"
          onMouseDown={(e) => {
            // Clicking the padding below the text puts the caret at the end.
            if (e.target === e.currentTarget && editor && editable && !disabled) {
              e.preventDefault();
              editor.commands.focus('end');
            }
          }}
        >
          {editor ? (
            <EditorContent editor={editor} />
          ) : (
            <div
              className="mn-rich-text-editor__prose mn-rich-text-editor__static"
              aria-busy="true"
            >
              <p data-empty={placeholder ? '' : undefined}>{placeholder || ' '}</p>
              <span className="mn-visually-hidden">{m.loading}</span>
            </div>
          )}
        </div>
        {toolbarPosition === 'bottom' ? toolbarRow : null}
        {showCount && editable ? (
          <div className="mn-rich-text-editor__footer">
            <span
              id={countId}
              className="mn-rich-text-editor__count"
              data-state={left !== null && left < 0 ? 'over' : near ? 'near' : undefined}
            >
              {m.count(characters, maxLength)}
            </span>
            <span className="mn-visually-hidden" role="status">
              {near && left !== null ? m.remaining(left) : ''}
            </span>
          </div>
        ) : null}
        {onFiles ? (
          <input
            ref={fileInput}
            type="file"
            multiple
            hidden
            tabIndex={-1}
            {...(fileAccept ? { accept: fileAccept } : {})}
            onChange={(e) => {
              const files = Array.from(e.target.files ?? []);
              e.target.value = '';
              handleFiles(files, 'picker');
            }}
          />
        ) : null}
        {name ? <input type="hidden" name={name} value={html} /> : null}
        {mention?.list}
      </div>
    );
  },
);
