'use client';

import {
  forwardRef,
  useEffect,
  useId,
  useImperativeHandle,
  useMemo,
  useRef,
  useState,
  type HTMLAttributes,
  type ReactNode,
  type Ref,
} from 'react';
import type { Editor, JSONContent } from '@tiptap/core';
import { Button, IconButton } from '../../actions/button/index.js';
import { SegmentedControl } from '../../forms/segmented-control/index.js';
import { cx } from '../../../internal/cx.js';
import {
  RichTextEditor,
  type RichTextEditorHandle,
  type RichTextFileHandler,
  type RichTextMentionOptions,
  type RichTextToolbarItem,
  type RichTextValue,
} from '../rich-text-editor/index.js';
import {
  CloseGlyph,
  PaperclipGlyph,
  PencilGlyph,
  ReplyGlyph,
  SendGlyph,
} from '../../../internal/editor-glyphs.js';

/** What `onSend` receives. */
export interface MessageComposerMessage {
  /** Sanitise on the server before storing or rendering. `''` when only attachments are sent. */
  html: string;
  /** Plain text, mentions as "@Label": for previews, notifications and search. */
  text: string;
  json: JSONContent | null;
  /** The selected mode's value, when `modes` is set. */
  mode?: string;
}

/** One composer mode, such as a public reply or an internal note. */
export interface MessageComposerMode {
  value: string;
  label: string;
  /** `note` gives the composer the private-note treatment (warning surface). */
  tone?: 'default' | 'note';
  /** Placeholder while this mode is selected. */
  placeholder?: string;
  /** Send button label while this mode is selected, e.g. "Add note". */
  sendLabel?: string;
}

export interface MessageComposerReplyTo {
  author: string;
  /** A short quote of the message being answered. */
  excerpt?: ReactNode;
}

export type MessageComposerDraftStatus = 'idle' | 'saving' | 'saved' | 'error';

export interface MessageComposerMessages {
  send: string;
  save: string;
  sending: string;
  cancel: string;
  attach: string;
  /** Name of the mode switch. Default "Message type". */
  modes: string;
  replyingTo: (author: string) => string;
  dismissReply: string;
  editing: string;
  /** Shown under the box. */
  hintEnter: string;
  hintModEnter: string;
  /** Hints while `editing`. */
  hintEditEnter: string;
  hintEditModEnter: string;
  draftSaving: string;
  draftSaved: string;
  draftError: string;
}

const MOD =
  typeof navigator !== 'undefined' && /Mac|iP(hone|ad)/.test(navigator.platform) ? '⌘' : 'Ctrl';

const DEFAULT_MESSAGES: MessageComposerMessages = {
  send: 'Send',
  save: 'Save',
  sending: 'Sending…',
  cancel: 'Cancel',
  attach: 'Attach files',
  modes: 'Message type',
  replyingTo: (author) => `Replying to ${author}`,
  dismissReply: 'Stop replying',
  editing: 'Editing message',
  hintEnter: 'Enter to send, Shift+Enter for a new line',
  hintModEnter: `${MOD}+Enter to send`,
  hintEditEnter: 'Enter to save, Escape to cancel',
  hintEditModEnter: `${MOD}+Enter to save, Escape to cancel`,
  draftSaving: 'Saving draft…',
  draftSaved: 'Draft saved',
  draftError: 'Draft not saved',
};

/** The composer's default toolbar: inline formatting, lists, links, quote, code. */
export const MESSAGE_COMPOSER_TOOLBAR: readonly RichTextToolbarItem[] = [
  'bold',
  'italic',
  'strike',
  'code',
  '|',
  'bulletList',
  'orderedList',
  '|',
  'link',
  'blockquote',
  'codeBlock',
];

export interface MessageComposerProps extends Omit<
  HTMLAttributes<HTMLDivElement>,
  'defaultValue' | 'onChange' | 'onSubmit' | 'placeholder'
> {
  /** Accessible name of the text box, e.g. "Message #design" or "Reply to customer". */
  label: string;
  placeholder?: string;
  /** Controlled HTML. */
  value?: string;
  /** Initial HTML (a restored draft, or the message being edited). */
  defaultValue?: string;
  onValueChange?: (html: string) => void;
  /**
   * Called with the message. Return a promise to show the pending state; the
   * box clears when it resolves and keeps the text when it rejects.
   */
  onSend: (message: MessageComposerMessage) => unknown;
  /** Controlled pending state, for products that track sending themselves. */
  sending?: boolean;
  /** `enter` (default): Enter sends, Shift+Enter adds a line. `mod-enter`: ⌘/Ctrl+Enter sends. */
  sendOn?: 'enter' | 'mod-enter';
  /** Send button text. Default "Send", or "Save" while `editing`. */
  sendLabel?: string;
  /** Allows sending with no text, because the attachment row has files. */
  hasAttachments?: boolean;
  /** The attachment row (uploads in progress, chips), rendered above the text. */
  attachments?: ReactNode;
  /** Receives pasted, dropped and picked files. Adds the attach button. */
  onFiles?: RichTextFileHandler;
  /** `accept` for the attach button's file picker. */
  fileAccept?: string;
  /** Renders the product's emoji picker; call `insert` with the chosen emoji. */
  emoji?: (insert: (text: string) => void) => ReactNode;
  /** Extra controls before the send button (schedule, macros, AI assist). */
  actions?: ReactNode;
  /** Persists the draft. Called after typing pauses, and with `''` after sending. */
  onDraftChange?: (html: string) => void;
  /** Pause before `onDraftChange`. Default 600 ms. */
  draftDebounceMs?: number;
  /** Shown under the box: the product's draft save state. */
  draftStatus?: MessageComposerDraftStatus;
  /** Edit an existing message: "Save" label, Escape or Cancel calls `onCancelEdit`. */
  editing?: boolean;
  onCancelEdit?: () => void;
  /** The message being answered, shown above the text. */
  replyTo?: MessageComposerReplyTo | undefined;
  onDismissReply?: () => void;
  /** Someone is typing. Called at most once per `typingIntervalMs` while the text changes. */
  onTyping?: () => void;
  /** Typing stopped: the box was emptied, sent or left. */
  onTypingStop?: () => void;
  /** Default 3000 ms. */
  typingIntervalMs?: number;
  /** Arrow Up in an empty box, e.g. to edit your last message. */
  onEditLast?: () => void;
  disabled?: boolean;
  /** Why the composer is disabled, shown under it and linked to the text box. */
  disabledReason?: ReactNode;
  /** Mode switch (public reply / internal note). Product-supplied. */
  modes?: readonly MessageComposerMode[];
  mode?: string;
  defaultMode?: string;
  onModeChange?: (mode: string) => void;
  /** Extra content beside the mode switch, e.g. the audience ("To: customer@…"). */
  modeAside?: ReactNode;
  maxLength?: number;
  showCharacterCount?: boolean;
  mentions?: RichTextMentionOptions;
  /** Formatting controls, or `false` for none. Default `MESSAGE_COMPOSER_TOOLBAR`. */
  toolbar?: readonly RichTextToolbarItem[] | false;
  /** Show the send-key hint. Default true. */
  showHint?: boolean;
  autoFocus?: boolean;
  /** The underlying editor's handle (focus, insertText, clear, …). */
  editorRef?: Ref<RichTextEditorHandle>;
  messages?: Partial<MessageComposerMessages>;
}

const isPromise = (v: unknown): v is Promise<unknown> =>
  typeof v === 'object' && v !== null && typeof (v as Promise<unknown>).then === 'function';

/**
 * The chat and ticket reply box: a compact RichTextEditor with Enter to send,
 * an attachment row, emoji and action slots, a send button with a pending
 * state, draft persistence, edit and reply-to modes, typing callbacks and a
 * product-supplied mode switch (public reply / internal note).
 */
export const MessageComposer = forwardRef<HTMLDivElement, MessageComposerProps>(
  function MessageComposer(
    {
      label,
      placeholder,
      value,
      defaultValue,
      onValueChange,
      onSend,
      sending,
      sendOn = 'enter',
      sendLabel,
      hasAttachments = false,
      attachments,
      onFiles,
      fileAccept,
      emoji,
      actions,
      onDraftChange,
      draftDebounceMs = 600,
      draftStatus = 'idle',
      editing = false,
      onCancelEdit,
      replyTo,
      onDismissReply,
      onTyping,
      onTypingStop,
      typingIntervalMs = 3000,
      onEditLast,
      disabled = false,
      disabledReason,
      modes,
      mode: modeProp,
      defaultMode,
      onModeChange,
      modeAside,
      maxLength,
      showCharacterCount,
      mentions,
      toolbar = MESSAGE_COMPOSER_TOOLBAR,
      showHint = true,
      autoFocus,
      editorRef,
      messages: messagesProp,
      className,
      ...rest
    },
    ref,
  ) {
    const m = useMemo(() => ({ ...DEFAULT_MESSAGES, ...messagesProp }), [messagesProp]);
    const id = `mn-composer-${useId().replace(/:/g, '')}`;
    const handle = useRef<RichTextEditorHandle>(null);
    useImperativeHandle(editorRef, () => handle.current as RichTextEditorHandle, []);
    const fileInput = useRef<HTMLInputElement>(null);
    const [pendingState, setPending] = useState(false);
    const pending = sending ?? pendingState;
    const [empty, setEmpty] = useState(() => !(value ?? defaultValue));
    const [modeState, setModeState] = useState(defaultMode ?? modes?.[0]?.value);
    const mode = modeProp ?? modeState;
    const current = modes?.find((x) => x.value === mode);

    const draftTimer = useRef<ReturnType<typeof setTimeout> | undefined>(undefined);
    const draftPending = useRef<string | null>(null);
    const lastTyping = useRef(0);
    const typingActive = useRef(false);
    const live = useRef({ onDraftChange, onTyping, onTypingStop });
    live.current = { onDraftChange, onTyping, onTypingStop };

    const flushDraft = () => {
      clearTimeout(draftTimer.current);
      if (draftPending.current !== null) {
        const html = draftPending.current;
        draftPending.current = null;
        live.current.onDraftChange?.(html);
      }
    };
    // Never lose a draft that was still waiting for its debounce.
    useEffect(() => () => flushDraft(), []);

    const stopTyping = () => {
      if (!typingActive.current) return;
      typingActive.current = false;
      lastTyping.current = 0;
      live.current.onTypingStop?.();
    };

    const isEmpty = value !== undefined ? value === '' || value === '<p></p>' : empty;
    const canSend = !disabled && !pending && (!isEmpty || hasAttachments);

    const send = () => {
      const h = handle.current;
      if (!h || !canSend) return;
      const message: MessageComposerMessage = {
        html: h.getHTML(),
        text: h.getText(),
        json: h.isEmpty() ? null : h.getJSON(),
        ...(mode !== undefined ? { mode } : {}),
      };
      stopTyping();
      const done = () => {
        h.clear();
        clearTimeout(draftTimer.current);
        draftPending.current = null;
        live.current.onDraftChange?.('');
      };
      let result: unknown;
      try {
        result = onSend(message);
      } catch {
        return;
      }
      if (isPromise(result)) {
        setPending(true);
        result.then(
          () => {
            setPending(false);
            done();
          },
          () => setPending(false),
        );
      } else done();
    };
    const sendRef = useRef(send);
    sendRef.current = send;

    const onEditorKeyDown = (event: KeyboardEvent, editor: Editor) => {
      if (event.isComposing) return false;
      const mod = event.metaKey || event.ctrlKey;
      if (event.key === 'Enter' && !event.altKey) {
        const plain = !event.shiftKey && !mod;
        if (mod || (sendOn === 'enter' && plain && !editor.isActive('codeBlock'))) {
          sendRef.current();
          return true;
        }
        return false;
      }
      if (event.key === 'Escape' && editing && onCancelEdit) {
        onCancelEdit();
        return true;
      }
      if (event.key === 'ArrowUp' && onEditLast && editor.isEmpty) {
        onEditLast();
        return true;
      }
      return false;
    };

    const onChange = (v: RichTextValue, details: { isEmpty: boolean }) => {
      const html = typeof v === 'string' ? v : '';
      setEmpty(details.isEmpty);
      onValueChange?.(html);
      if (live.current.onDraftChange) {
        draftPending.current = html;
        clearTimeout(draftTimer.current);
        draftTimer.current = setTimeout(flushDraft, draftDebounceMs);
      }
      if (details.isEmpty) stopTyping();
      else {
        const now = Date.now();
        if (now - lastTyping.current >= typingIntervalMs) {
          lastTyping.current = now;
          typingActive.current = true;
          live.current.onTyping?.();
        }
      }
    };

    const reasonId = `${id}-reason`;
    const hintId = `${id}-hint`;
    const describedBy =
      [
        disabled && disabledReason ? reasonId : undefined,
        showHint && !disabled ? hintId : undefined,
      ]
        .filter(Boolean)
        .join(' ') || undefined;
    const label_ = sendLabel ?? current?.sendLabel ?? (editing ? m.save : m.send);
    const status =
      draftStatus === 'saving'
        ? m.draftSaving
        : draftStatus === 'saved'
          ? m.draftSaved
          : draftStatus === 'error'
            ? m.draftError
            : '';

    const actionRow = (
      <>
        {emoji && !disabled ? emoji((text) => handle.current?.insertText(text)) : null}
        {onFiles ? (
          <IconButton
            size="sm"
            label={m.attach}
            icon={<PaperclipGlyph />}
            disabled={disabled}
            onClick={() => fileInput.current?.click()}
          />
        ) : null}
        {actions}
        {editing && onCancelEdit ? (
          <Button size="sm" variant="ghost" onClick={onCancelEdit}>
            {m.cancel}
          </Button>
        ) : null}
        <Button
          size="sm"
          variant="primary"
          className="mn-message-composer__send"
          leadingIcon={editing ? undefined : <SendGlyph />}
          loading={pending}
          disabled={!canSend && !pending}
          onClick={send}
        >
          {pending ? m.sending : label_}
        </Button>
      </>
    );

    return (
      <div
        ref={ref}
        {...rest}
        className={cx('mn-message-composer', className)}
        data-tone={current?.tone === 'note' ? 'note' : undefined}
        data-editing={editing || undefined}
        data-disabled={disabled || undefined}
      >
        {modes && modes.length > 0 ? (
          <div className="mn-message-composer__modes">
            <SegmentedControl
              size="sm"
              aria-label={m.modes}
              options={modes.map((x) => ({ value: x.value, label: x.label }))}
              value={mode ?? modes[0]?.value ?? ''}
              disabled={disabled}
              onValueChange={(v) => {
                setModeState(v);
                onModeChange?.(v);
              }}
            />
            {modeAside ? <div className="mn-message-composer__mode-aside">{modeAside}</div> : null}
          </div>
        ) : null}
        {editing || replyTo ? (
          <div className="mn-message-composer__context">
            <span className="mn-message-composer__context-icon" aria-hidden>
              {editing ? <PencilGlyph /> : <ReplyGlyph />}
            </span>
            <div className="mn-message-composer__context-text">
              <span className="mn-message-composer__context-title">
                {editing ? m.editing : replyTo ? m.replyingTo(replyTo.author) : null}
              </span>
              {!editing && replyTo?.excerpt ? (
                <span className="mn-message-composer__context-excerpt">{replyTo.excerpt}</span>
              ) : null}
            </div>
            {!editing && replyTo && onDismissReply ? (
              <IconButton
                size="sm"
                label={m.dismissReply}
                icon={<CloseGlyph />}
                onClick={() => {
                  onDismissReply();
                  handle.current?.focus();
                }}
              />
            ) : null}
          </div>
        ) : null}
        {attachments ? <div className="mn-message-composer__attachments">{attachments}</div> : null}
        <RichTextEditor
          className="mn-message-composer__editor"
          aria-label={label}
          aria-describedby={describedBy}
          editorRef={handle}
          {...(value !== undefined ? { value } : {})}
          {...(defaultValue !== undefined ? { defaultValue } : {})}
          onValueChange={onChange}
          placeholder={current?.placeholder ?? placeholder ?? ''}
          disabled={disabled}
          // Editing moves focus to the message being edited, as the person asked to edit it.
          // eslint-disable-next-line jsx-a11y/no-autofocus
          autoFocus={autoFocus ?? editing}
          toolbar={toolbar}
          toolbarPosition="bottom"
          toolbarEnd={actionRow}
          headingLevels={[]}
          taskList={false}
          minHeight="2.75rem"
          maxHeight="15rem"
          {...(maxLength !== undefined ? { maxLength } : {})}
          {...(showCharacterCount !== undefined ? { showCharacterCount } : {})}
          {...(mentions ? { mentions } : {})}
          {...(onFiles ? { onFiles, images: false } : {})}
          onEditorKeyDown={onEditorKeyDown}
          onBlur={(e) => {
            if (!e.currentTarget.contains(e.relatedTarget as Node | null)) stopTyping();
          }}
        />
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
              if (files.length) void onFiles(files, 'picker');
            }}
          />
        ) : null}
        <div className="mn-message-composer__footer">
          {disabled && disabledReason ? (
            <p id={reasonId} className="mn-message-composer__reason">
              {disabledReason}
            </p>
          ) : showHint ? (
            <p id={hintId} className="mn-message-composer__hint">
              {editing
                ? sendOn === 'enter'
                  ? m.hintEditEnter
                  : m.hintEditModEnter
                : sendOn === 'enter'
                  ? m.hintEnter
                  : m.hintModEnter}
            </p>
          ) : null}
          <p className="mn-message-composer__status" role="status" data-status={draftStatus}>
            {status}
          </p>
        </div>
      </div>
    );
  },
);
