'use client';

import {
  forwardRef,
  useCallback,
  useEffect,
  useId,
  useImperativeHandle,
  useLayoutEffect,
  useMemo,
  useRef,
  useState,
  type ClipboardEvent as ReactClipboardEvent,
  type DragEvent as ReactDragEvent,
  type HTMLAttributes,
  type KeyboardEvent as ReactKeyboardEvent,
  type ReactNode,
  type Ref,
  type TextareaHTMLAttributes,
} from 'react';
import { IconButton } from '../../actions/button/index.js';
import { AiStopButton } from '../ai-stop-button/index.js';
import type { AiConversationStatus } from '../ai-conversation/index.js';
import { cx } from '../../../internal/cx.js';
import { fileMatchesAccept } from '../../files/file-upload/index.js';
import { CloseGlyph } from '../../../internal/icons.js';
import { PaperclipGlyph } from '../../../internal/editor-glyphs.js';

const useIsoLayoutEffect = typeof window === 'undefined' ? useEffect : useLayoutEffect;

// ------------------------------------------------------------------ types

/** One attached file, as shown in the chips and passed to `onSubmit`. */
export interface AiPromptAttachment {
  /** Stable id for keys and removal. */
  id: string;
  file: File;
}

/** Where attached files came from. */
export type AiPromptAttachSource = 'paste' | 'drop' | 'picker';

/**
 * Receives pasted, dropped or picked files (already filtered by `accept`).
 * Return an array to keep only those files, or nothing to keep them all.
 */
export type AiPromptAttachHandler =
  | ((files: File[], source: AiPromptAttachSource) => File[])
  | ((files: File[], source: AiPromptAttachSource) => void);

/** What `onSubmit` receives besides the text. */
export interface AiPromptSubmitDetails {
  /** The attached files, in the order they were added. */
  files: File[];
}

/** Lets a trigger's menu close itself or replace the typed trigger. */
export interface AiPromptTriggerApi {
  /** Closes the menu. It stays closed until the person types another trigger. */
  close: () => void;
  /**
   * Replaces the trigger and its query (e.g. `/sum`) with `text`, puts the
   * caret after it and closes the menu. Include a trailing space if you want one.
   */
  insert: (text: string) => void;
}

/** A character (or string) that opens a caller-rendered menu when typed at the start of a word. */
export interface AiPromptTrigger {
  /** `/` for commands, `@` for mentions, or any string. */
  char: '/' | '@' | (string & {});
  /** Called with the text typed after the trigger, as it changes (`''` right after the trigger). */
  onTrigger?: (query: string, api: AiPromptTriggerApi) => void;
  /** Called when the trigger ends: the caret left it, a space ended it, or `close` was called. */
  onDismiss?: () => void;
}

/** The active trigger, for `renderTriggerMenu`. */
export interface AiPromptTriggerState extends AiPromptTriggerApi {
  trigger: AiPromptTrigger;
  char: string;
  /** Text typed after the trigger. */
  query: string;
  /** The textarea's id, for `aria-controls` on your listbox. */
  inputId: string;
}

/** Counter states, also on the root as `data-limit`. */
export type AiPromptLimitState = 'ok' | 'warning' | 'over';

/** Every string AiPromptInput shows or announces. */
export interface AiPromptInputMessages {
  placeholder: string;
  send: string;
  stop: string;
  attach: string;
  /** Name of the attachment list. */
  attachments: string;
  removeAttachment: (name: string) => string;
  /** Announced after files are attached. */
  attached: (count: number) => string;
  /** Announced after a file is removed. */
  removed: (name: string) => string;
  /** Shown over the input while files are dragged onto it. */
  dropHint: string;
  /** Screen reader hint for `submitOn: 'enter'`. */
  hintEnter: string;
  /** Screen reader hint for `submitOn: 'mod-enter'`; `mod` is "⌘" on Apple devices, else "Ctrl". */
  hintModEnter: (mod: string) => string;
  /** Screen reader hint added while a response is running. */
  hintStop: string;
  /** The visible counter, e.g. "120 / 4000". */
  counter: (count: number, max: number) => string;
  /** The counter as read by screen readers. */
  counterLabel: (count: number, max: number) => string;
  /** Announced once when the count reaches the warning threshold. */
  counterWarning: (remaining: number) => string;
  /** Announced once when the count goes over `maxLength`. */
  counterOver: (over: number) => string;
  /** The token estimate. */
  tokens: (count: number) => string;
}

/** "⌘" on Apple devices, else "Ctrl". Read after mount so server and client render the same text. */
function useModKey(): string {
  const [mod, setMod] = useState('Ctrl');
  useEffect(() => {
    if (/Mac|iP(hone|ad)/.test(navigator.platform)) setMod('⌘');
  }, []);
  return mod;
}

const DEFAULT_MESSAGES: AiPromptInputMessages = {
  placeholder: 'Send a message…',
  send: 'Send',
  stop: 'Stop',
  attach: 'Attach files',
  attachments: 'Attachments',
  removeAttachment: (name) => `Remove ${name}`,
  attached: (n) => (n === 1 ? '1 file attached' : `${n} files attached`),
  removed: (name) => `${name} removed`,
  dropHint: 'Drop files to attach',
  hintEnter: 'Enter to send, Shift+Enter for a new line.',
  hintModEnter: (mod) => `${mod}+Enter to send.`,
  hintStop: 'Escape stops the response.',
  counter: (count, max) => `${count} / ${max}`,
  counterLabel: (count, max) => `${count} of ${max} characters`,
  counterWarning: (remaining) =>
    remaining === 1 ? '1 character left' : `${remaining} characters left`,
  counterOver: (over) =>
    over === 1 ? '1 character over the limit' : `${over} characters over the limit`,
  tokens: (n) => `≈ ${n} tokens`,
};

export interface AiPromptInputProps extends Omit<
  HTMLAttributes<HTMLDivElement>,
  'defaultValue' | 'onChange' | 'onSubmit' | 'children'
> {
  /** Required accessible name of the textarea, e.g. "Message the assistant". Visually hidden unless `showLabel`. */
  label: string;
  /** Show `label` above the input. Default false. */
  showLabel?: boolean;
  /** Placeholder. Default "Send a message…". */
  placeholder?: string;
  /** Controlled text. Pair with `onValueChange` (or the `usePromptDraft` tuple). */
  value?: string;
  /** Initial text when uncontrolled. */
  defaultValue?: string;
  /** Called with the new text on every change, and with `''` after a submit. */
  onValueChange?: (value: string) => void;
  /**
   * Called on Enter (or ⌘/Ctrl+Enter) or the Send button with the text as typed
   * and the attached files. The input clears at once; if you return a promise
   * that rejects, the text and files come back.
   */
  onSubmit: (text: string, details: AiPromptSubmitDetails) => unknown;
  /** `enter` (default): Enter sends, Shift+Enter adds a line. `mod-enter`: ⌘/Ctrl+Enter sends, Enter adds a line. */
  submitOn?: 'enter' | 'mod-enter';
  /** The conversation status from `useAiConversation`. `submitted` and `streaming` turn Send into Stop. */
  status?: AiConversationStatus;
  /** A response is running (overrides `status`). Send becomes Stop and Escape stops. */
  busy?: boolean;
  /** Called by the Stop button and by Escape in the textarea while busy. */
  onStop?: () => void;
  /** Disables typing and sending. */
  disabled?: boolean;
  /** Why the input is disabled, e.g. "Connect a provider to start". Shown under it and linked with `aria-describedby`. */
  disabledReason?: ReactNode;
  /** Fewest visible rows. Default 1. */
  minRows?: number;
  /** Rows the textarea grows to before it scrolls. Default 8. */
  maxRows?: number;
  /** Character limit for the live counter. Going over blocks sending; the text is never cut. */
  maxLength?: number;
  /** Share of `maxLength` at which the counter turns to warning. Default 0.9. */
  warnAt?: number;
  /** Estimates tokens for the "≈ N tokens" readout. Called on every change, so keep it cheap. */
  countTokens?: (text: string) => number;
  /**
   * Called with pasted, dropped or picked files. Return a list to keep only
   * those (validation), or nothing to keep them all. Setting it (or `accept`)
   * turns on attachments and the attach button.
   */
  onAttach?: AiPromptAttachHandler;
  /** `accept` for the file picker; pasted and dropped files are filtered by it too. */
  accept?: string;
  /** Hide the attach button but keep paste and drop. Default true when attachments are on. */
  showAttachButton?: boolean;
  /** Replaces the attachment chips. */
  renderAttachments?: (
    attachments: AiPromptAttachment[],
    remove: (id: string) => void,
  ) => ReactNode;
  /** Characters that open a caller-rendered menu (slash commands, mentions). */
  triggers?: readonly AiPromptTrigger[];
  /** Renders the menu for the active trigger, positioned above the input. */
  renderTriggerMenu?: (state: AiPromptTriggerState) => ReactNode;
  /** Left side of the bar under the textarea: model picker, mode, extra actions. After the attach button. */
  toolbar?: ReactNode;
  /** Under the input, e.g. a disclaimer. */
  footer?: ReactNode;
  /**
   * Props for the `<textarea>`: `id`, `name`, `autoFocus`, and `role`,
   * `aria-expanded`, `aria-controls`, `aria-activedescendant` for a trigger
   * menu. Your `onKeyDown` runs first; call `preventDefault()` to stop the
   * input acting on that key (e.g. Enter picking a menu item).
   */
  textareaProps?: Omit<
    TextareaHTMLAttributes<HTMLTextAreaElement>,
    'value' | 'defaultValue' | 'disabled' | 'rows' | 'placeholder'
  >;
  /** The `<textarea>` element. */
  textareaRef?: Ref<HTMLTextAreaElement>;
  /** Override any string. */
  messages?: Partial<AiPromptInputMessages>;
}

// ------------------------------------------------------------------ helpers

const isPromise = (v: unknown): v is Promise<unknown> =>
  typeof v === 'object' && v !== null && typeof (v as Promise<unknown>).then === 'function';

let attachmentSeq = 0;
const toAttachments = (files: File[]): AiPromptAttachment[] =>
  files.map((file) => ({ id: `mn-attachment-${++attachmentSeq}`, file }));

/** A trigger found at the caret, from `findTrigger`. */
export interface AiPromptTriggerMatch {
  /** Index into `triggers`. */
  index: number;
  char: string;
  query: string;
  /** Offset of the trigger's first character. */
  start: number;
  /** Caret offset (end of the query). */
  end: number;
}

/**
 * Finds a trigger at the caret: the word before the caret starts with a
 * trigger and the trigger itself is at the start of a word.
 */
export function findTrigger(
  text: string,
  caret: number,
  triggers: readonly { char: string }[],
): AiPromptTriggerMatch | null {
  let start = caret;
  while (start > 0 && !/\s/.test(text.charAt(start - 1))) start--;
  const word = text.slice(start, caret);
  const index = triggers.findIndex((t) => t.char !== '' && word.startsWith(t.char));
  const char = triggers[index]?.char;
  if (char !== undefined) {
    return { index, char, query: word.slice(char.length), start, end: caret };
  }
  return null;
}

// ------------------------------------------------------------------ draft hook

const DRAFT_PREFIX = 'mn-prompt-draft:';

function getStorage(): Storage | null {
  try {
    return typeof window === 'undefined' ? null : window.sessionStorage;
  } catch {
    return null;
  }
}

/**
 * Keeps an unsent prompt in sessionStorage under `key`, so a reload or a
 * route change does not lose it. Returns `[draft, setDraft, clear]`; pass
 * `draft` and `setDraft` to AiPromptInput as `value` and `onValueChange`.
 * SSR-safe: the draft is read after mount, and storage errors (private
 * windows, quotas) are ignored. `null` or `undefined` keys persist nothing.
 */
export function usePromptDraft(
  key: string | null | undefined,
): [draft: string, setDraft: (value: string) => void, clear: () => void] {
  const [draft, setDraftState] = useState('');
  const storageKey = key ? DRAFT_PREFIX + key : null;
  const keyRef = useRef(storageKey);
  keyRef.current = storageKey;

  useEffect(() => {
    if (!storageKey) return;
    try {
      setDraftState(getStorage()?.getItem(storageKey) ?? '');
    } catch {
      setDraftState('');
    }
  }, [storageKey]);

  const setDraft = useCallback((value: string) => {
    setDraftState(value);
    const k = keyRef.current;
    if (!k) return;
    try {
      const s = getStorage();
      if (value) s?.setItem(k, value);
      else s?.removeItem(k);
    } catch {
      /* storage unavailable: keep the in-memory draft */
    }
  }, []);

  const clear = useCallback(() => setDraft(''), [setDraft]);
  return [draft, setDraft, clear];
}

// ------------------------------------------------------------------ component

/**
 * The prompt box for AI conversations: an auto-growing textarea where Enter
 * sends and Shift+Enter adds a line (IME-safe), a Send button that becomes
 * Stop while a response runs (Escape stops too), a disabled state with a
 * visible reason, attachments by paste, drop or picker, trigger characters for
 * slash commands and mentions, a live character counter and token estimate,
 * and toolbar and footer slots. Pair it with `useAiConversation` and
 * `usePromptDraft`.
 */
export const AiPromptInput = forwardRef<HTMLDivElement, AiPromptInputProps>(function AiPromptInput(
  {
    label,
    showLabel = false,
    placeholder,
    value: valueProp,
    defaultValue,
    onValueChange,
    onSubmit,
    submitOn = 'enter',
    status,
    busy: busyProp,
    onStop,
    disabled = false,
    disabledReason,
    minRows = 1,
    maxRows = 8,
    maxLength,
    warnAt = 0.9,
    countTokens,
    onAttach,
    accept,
    showAttachButton = true,
    renderAttachments,
    triggers,
    renderTriggerMenu,
    toolbar,
    footer,
    textareaProps,
    textareaRef,
    messages: messagesProp,
    className,
    ...rest
  },
  ref,
) {
  const m = useMemo(() => ({ ...DEFAULT_MESSAGES, ...messagesProp }), [messagesProp]);
  const modKey = useModKey();
  const uid = `mn-ai-prompt-${useId().replace(/:/g, '')}`;
  const inputId = textareaProps?.id ?? `${uid}-input`;
  const root = useRef<HTMLDivElement>(null);
  useImperativeHandle(ref, () => root.current as HTMLDivElement, []);
  const area = useRef<HTMLTextAreaElement | null>(null);
  const fileInput = useRef<HTMLInputElement>(null);
  const chipList = useRef<HTMLUListElement>(null);

  const [inner, setInner] = useState(defaultValue ?? '');
  const controlled = valueProp !== undefined;
  const value = controlled ? valueProp : inner;
  const setValue = (next: string) => {
    if (!controlled) setInner(next);
    onValueChange?.(next);
  };

  const [attachments, setAttachments] = useState<AiPromptAttachment[]>([]);
  const [dragging, setDragging] = useState(false);
  const dragDepth = useRef(0);
  const [announcement, setAnnouncement] = useState('');
  const composing = useRef(false);

  const busy = busyProp ?? (status === 'submitted' || status === 'streaming');
  const attachEnabled = Boolean(onAttach || accept);

  // ---------------------------------------------------------- auto-grow
  useIsoLayoutEffect(() => {
    const el = area.current;
    if (!el || typeof window === 'undefined') return;
    // The textarea is border-box (see the CSS): scrollHeight is content + padding.
    const cs = window.getComputedStyle(el);
    const px = (v: string) => parseFloat(v) || 0;
    const line = px(cs.lineHeight) || px(cs.fontSize) * 1.5 || 20;
    const padding = px(cs.paddingTop) + px(cs.paddingBottom);
    const border = px(cs.borderTopWidth) + px(cs.borderBottomWidth);
    const min = line * minRows + padding + border;
    const max = line * Math.max(minRows, maxRows) + padding + border;
    el.style.height = 'auto';
    const content = el.scrollHeight + border;
    const next = Math.min(Math.max(content, min), max);
    el.style.height = `${next}px`;
    el.style.overflowY = content > max ? 'auto' : 'hidden';
  }, [value, minRows, maxRows]);

  // ---------------------------------------------------------- counter
  const count = value.length;
  const limit: AiPromptLimitState =
    maxLength === undefined
      ? 'ok'
      : count > maxLength
        ? 'over'
        : count >= Math.floor(maxLength * warnAt)
          ? 'warning'
          : 'ok';
  const lastLimit = useRef<AiPromptLimitState>(limit);
  useEffect(() => {
    const prev = lastLimit.current;
    lastLimit.current = limit;
    if (maxLength === undefined || prev === limit) return;
    // Announce only on the way up; returning to ok is silent.
    if (limit === 'warning' && prev === 'ok') setAnnouncement(m.counterWarning(maxLength - count));
    else if (limit === 'over') setAnnouncement(m.counterOver(count - maxLength));
  }, [limit, count, maxLength, m]);
  const tokens = countTokens ? countTokens(value) : undefined;

  // ---------------------------------------------------------- triggers
  const [active, setActive] = useState<AiPromptTriggerMatch | null>(null);
  const dismissedAt = useRef<number | null>(null);
  const activeRef = useRef(active);
  activeRef.current = active;
  const triggersRef = useRef(triggers);
  triggersRef.current = triggers;

  const setValueRef = useRef(setValue);
  setValueRef.current = setValue;

  const closeTrigger = useCallback(() => {
    const cur = activeRef.current;
    if (!cur) return;
    dismissedAt.current = cur.start;
    activeRef.current = null;
    setActive(null);
    triggersRef.current?.[cur.index]?.onDismiss?.();
  }, []);

  const insertTrigger = useCallback((text: string) => {
    const cur = activeRef.current;
    const el = area.current;
    if (!cur || !el) return;
    const v = el.value;
    const next = v.slice(0, cur.start) + text + v.slice(cur.end);
    const caret = cur.start + text.length;
    activeRef.current = null;
    setActive(null);
    dismissedAt.current = null;
    setValueRef.current(next);
    triggersRef.current?.[cur.index]?.onDismiss?.();
    requestAnimationFrame(() => {
      el.focus();
      el.setSelectionRange(caret, caret);
    });
  }, []);
  const api: AiPromptTriggerApi = useMemo(
    () => ({ close: closeTrigger, insert: insertTrigger }),
    [closeTrigger, insertTrigger],
  );

  const detect = (text: string, caret: number | null) => {
    const list = triggersRef.current;
    if (!list?.length || caret === null) return;
    let found = findTrigger(text, caret, list);
    if (found && dismissedAt.current === found.start) found = null;
    else if (!found || dismissedAt.current !== found.start) dismissedAt.current = null;
    const prev = activeRef.current;
    if (!found) {
      if (prev) {
        activeRef.current = null;
        setActive(null);
        list[prev.index]?.onDismiss?.();
      }
      return;
    }
    if (
      prev &&
      prev.index === found.index &&
      prev.start === found.start &&
      prev.query === found.query
    ) {
      if (prev.end !== found.end) setActive(found);
      return;
    }
    activeRef.current = found;
    setActive(found);
    list[found.index]?.onTrigger?.(found.query, api);
  };

  // ---------------------------------------------------------- attachments
  const addFiles = (files: File[], source: AiPromptAttachSource) => {
    let kept = files.filter((f) => fileMatchesAccept(f, accept));
    if (onAttach) {
      const result = onAttach(kept, source);
      if (Array.isArray(result)) kept = result;
    }
    if (!kept.length) return;
    setAttachments((list) => [...list, ...toAttachments(kept)]);
    setAnnouncement(m.attached(kept.length));
  };

  const removeAttachment = (id: string) => {
    const index = attachments.findIndex((a) => a.id === id);
    const target = attachments[index];
    if (!target) return;
    setAttachments((list) => list.filter((a) => a.id !== id));
    setAnnouncement(m.removed(target.file.name));
    // Keep focus in the list: the next chip's remove button, else the previous, else the textarea.
    const buttons = chipList.current?.querySelectorAll<HTMLButtonElement>('button');
    const next = buttons?.[index + 1] ?? buttons?.[index - 1];
    if (next) next.focus();
    else area.current?.focus();
  };

  // ---------------------------------------------------------- submit
  const canSubmit =
    !disabled && !busy && limit !== 'over' && (value.trim() !== '' || attachments.length > 0);

  const submit = () => {
    if (!canSubmit) return;
    const text = value;
    const files = attachments;
    closeTrigger();
    setValue('');
    setAttachments([]);
    let result: unknown;
    try {
      result = onSubmit(text, { files: files.map((a) => a.file) });
    } catch {
      setValue(text);
      setAttachments(files);
      return;
    }
    if (isPromise(result)) {
      result.catch(() => {
        // Bring the prompt back only if nothing new was typed meanwhile.
        if ((area.current?.value ?? '') === '') {
          setValueRef.current(text);
          setAttachments((list) => (list.length ? list : files));
        }
      });
    }
  };

  const stop = () => {
    onStop?.();
  };

  // ---------------------------------------------------------- events
  const onKeyDown = (e: ReactKeyboardEvent<HTMLTextAreaElement>) => {
    textareaProps?.onKeyDown?.(e);
    if (e.defaultPrevented) return;
    // IME: never act while composing. Safari reports the committing Enter as keyCode 229.
    if (e.nativeEvent.isComposing || composing.current || e.keyCode === 229) return;
    if (e.key === 'Escape') {
      if (activeRef.current) {
        e.preventDefault();
        closeTrigger();
      } else if (busy && onStop) {
        e.preventDefault();
        stop();
      }
      return;
    }
    if (e.key !== 'Enter' || e.altKey) return;
    const mod = e.metaKey || e.ctrlKey;
    const sends = mod || (submitOn === 'enter' && !e.shiftKey);
    if (!sends) return;
    e.preventDefault();
    submit();
  };

  const onPaste = (e: ReactClipboardEvent<HTMLTextAreaElement>) => {
    textareaProps?.onPaste?.(e);
    if (e.defaultPrevented || !attachEnabled) return;
    const data = e.clipboardData;
    const files = Array.from(data?.files ?? []);
    if (!files.length) return;
    // Keep any text that came with the files (e.g. a copied document); take over pure file pastes.
    if (!data.getData('text/plain')) e.preventDefault();
    addFiles(files, 'paste');
  };

  const hasFiles = (e: ReactDragEvent) => Array.from(e.dataTransfer?.types ?? []).includes('Files');
  const dropHandlers =
    attachEnabled && !disabled
      ? {
          onDragEnter: (e: ReactDragEvent<HTMLDivElement>) => {
            if (!hasFiles(e)) return;
            e.preventDefault();
            dragDepth.current++;
            setDragging(true);
          },
          onDragOver: (e: ReactDragEvent<HTMLDivElement>) => {
            if (!hasFiles(e)) return;
            e.preventDefault();
            if (e.dataTransfer) e.dataTransfer.dropEffect = 'copy';
          },
          onDragLeave: (e: ReactDragEvent<HTMLDivElement>) => {
            if (!hasFiles(e)) return;
            dragDepth.current = Math.max(0, dragDepth.current - 1);
            if (dragDepth.current === 0) setDragging(false);
          },
          onDrop: (e: ReactDragEvent<HTMLDivElement>) => {
            const files = Array.from(e.dataTransfer?.files ?? []);
            dragDepth.current = 0;
            setDragging(false);
            if (!files.length) return;
            e.preventDefault();
            addFiles(files, 'drop');
            area.current?.focus();
          },
        }
      : {};

  useImperativeHandle(textareaRef, () => area.current as HTMLTextAreaElement, []);

  // ---------------------------------------------------------- render
  const reasonId = `${uid}-reason`;
  const hintId = `${uid}-hint`;
  const counterId = `${uid}-counter`;
  const describedBy =
    [
      textareaProps?.['aria-describedby'],
      disabled && disabledReason ? reasonId : undefined,
      disabled ? undefined : hintId,
      maxLength !== undefined ? counterId : undefined,
    ]
      .filter(Boolean)
      .join(' ') || undefined;

  const activeTrigger = active ? triggers?.[active.index] : undefined;
  const triggerState: AiPromptTriggerState | null =
    active && activeTrigger
      ? {
          trigger: activeTrigger,
          char: active.char,
          query: active.query,
          inputId,
          ...api,
        }
      : null;

  const showAttach = attachEnabled && showAttachButton;

  return (
    <div
      ref={root}
      {...rest}
      {...dropHandlers}
      className={cx('mn-ai-prompt-input', className)}
      data-busy={busy || undefined}
      data-disabled={disabled || undefined}
      data-dragging={dragging || undefined}
      data-limit={maxLength !== undefined ? limit : undefined}
      data-trigger={active ? active.char : undefined}
    >
      <label
        htmlFor={inputId}
        className={showLabel ? 'mn-ai-prompt-input__label' : 'mn-visually-hidden'}
      >
        {label}
      </label>
      {triggerState && renderTriggerMenu ? (
        <div className="mn-ai-prompt-input__menu">{renderTriggerMenu(triggerState)}</div>
      ) : null}
      <div className="mn-ai-prompt-input__box">
        {attachments.length ? (
          renderAttachments ? (
            renderAttachments(attachments, removeAttachment)
          ) : (
            <ul
              ref={chipList}
              className="mn-ai-prompt-input__attachments"
              aria-label={m.attachments}
            >
              {attachments.map((a) => (
                <li key={a.id} className="mn-ai-prompt-input__chip">
                  <span className="mn-ai-prompt-input__chip-name" title={a.file.name}>
                    {a.file.name}
                  </span>
                  <IconButton
                    size="xs"
                    label={m.removeAttachment(a.file.name)}
                    icon={<CloseGlyph />}
                    className="mn-ai-prompt-input__chip-remove"
                    onClick={() => removeAttachment(a.id)}
                  />
                </li>
              ))}
            </ul>
          )
        ) : null}
        <textarea
          {...textareaProps}
          ref={(el) => {
            area.current = el;
          }}
          id={inputId}
          className={cx('mn-ai-prompt-input__textarea', textareaProps?.className)}
          rows={minRows}
          value={value}
          placeholder={placeholder ?? m.placeholder}
          disabled={disabled}
          aria-describedby={describedBy}
          aria-invalid={limit === 'over' || textareaProps?.['aria-invalid'] || undefined}
          onChange={(e) => {
            textareaProps?.onChange?.(e);
            setValue(e.target.value);
            detect(e.target.value, e.target.selectionStart);
          }}
          onSelect={(e) => {
            textareaProps?.onSelect?.(e);
            const el = e.currentTarget;
            detect(el.value, el.selectionStart === el.selectionEnd ? el.selectionStart : null);
          }}
          onKeyDown={onKeyDown}
          onPaste={onPaste}
          onCompositionStart={(e) => {
            composing.current = true;
            textareaProps?.onCompositionStart?.(e);
          }}
          onCompositionEnd={(e) => {
            composing.current = false;
            textareaProps?.onCompositionEnd?.(e);
          }}
          onBlur={(e) => {
            textareaProps?.onBlur?.(e);
            // Leave the menu open while focus moves into it.
            const to = e.relatedTarget as Node | null;
            if (!to || !root.current?.contains(to)) closeTrigger();
          }}
        />
        <div className="mn-ai-prompt-input__bar">
          <div className="mn-ai-prompt-input__toolbar">
            {showAttach ? (
              <IconButton
                size="sm"
                label={m.attach}
                icon={<PaperclipGlyph />}
                disabled={disabled}
                className="mn-ai-prompt-input__attach"
                onClick={() => fileInput.current?.click()}
              />
            ) : null}
            {toolbar}
          </div>
          <div className="mn-ai-prompt-input__meta">
            {tokens !== undefined ? (
              <span className="mn-ai-prompt-input__tokens">{m.tokens(tokens)}</span>
            ) : null}
            {maxLength !== undefined ? (
              <span className="mn-ai-prompt-input__counter" data-limit={limit}>
                <span aria-hidden>{m.counter(count, maxLength)}</span>
                <span id={counterId} className="mn-visually-hidden">
                  {m.counterLabel(count, maxLength)}
                </span>
              </span>
            ) : null}
          </div>
          <div className="mn-ai-prompt-input__actions">
            {busy ? (
              <AiStopButton
                className="mn-ai-prompt-input__stop"
                showLabel={false}
                messages={{ stop: m.stop }}
                buttonProps={{ 'aria-keyshortcuts': 'Escape' }}
                onStop={() => {
                  stop();
                  area.current?.focus();
                }}
              />
            ) : (
              <IconButton
                size="sm"
                variant="primary"
                label={m.send}
                icon={<SendArrowGlyph />}
                className="mn-ai-prompt-input__send"
                disabled={!canSubmit}
                onClick={() => {
                  submit();
                  area.current?.focus();
                }}
              />
            )}
          </div>
        </div>
        {dragging ? (
          <div className="mn-ai-prompt-input__drop" aria-hidden>
            {m.dropHint}
          </div>
        ) : null}
      </div>
      {attachEnabled ? (
        <input
          ref={fileInput}
          type="file"
          multiple
          hidden
          tabIndex={-1}
          aria-hidden
          {...(accept ? { accept } : {})}
          onChange={(e) => {
            const files = Array.from(e.target.files ?? []);
            e.target.value = '';
            if (files.length) addFiles(files, 'picker');
            area.current?.focus();
          }}
        />
      ) : null}
      <p id={hintId} className="mn-visually-hidden">
        {submitOn === 'enter' ? m.hintEnter : m.hintModEnter(modKey)}
        {busy && onStop ? ` ${m.hintStop}` : ''}
      </p>
      {disabled && disabledReason ? (
        <p id={reasonId} className="mn-ai-prompt-input__reason">
          {disabledReason}
        </p>
      ) : null}
      {footer ? <div className="mn-ai-prompt-input__footer">{footer}</div> : null}
      <span role="status" className="mn-visually-hidden">
        {announcement}
      </span>
    </div>
  );
});

function SendArrowGlyph() {
  return (
    <svg
      width={16}
      height={16}
      viewBox="0 0 16 16"
      fill="none"
      stroke="currentColor"
      strokeWidth={1.75}
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden
      focusable={false}
    >
      <path d="M8 13V3M3.5 7.5 8 3l4.5 4.5" />
    </svg>
  );
}
