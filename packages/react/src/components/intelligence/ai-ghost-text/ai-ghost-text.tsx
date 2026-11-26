'use client';

import {
  forwardRef,
  useCallback,
  useEffect,
  useId,
  useRef,
  useState,
  type HTMLAttributes,
  type ReactNode,
} from 'react';
import { useIsomorphicLayoutEffect } from '../../../internal/layout-effect.js';
import { cx } from '../../../internal/cx.js';
import { elementDirection } from '../../../internal/direction.js';
import { setRefs } from '../../../internal/dismissible.js';
import { useAiCompletion, type UseAiCompletionResult } from './ai-ghost-text.completion.js';
import type {
  AiCompletionAcceptance,
  AiCompletionFunction,
  AiGhostTextAcceptKind,
} from './ai-ghost-text.types.js';

type Field = HTMLTextAreaElement | HTMLInputElement;

/** Every string AiGhostText shows or announces. English defaults. */
export interface AiGhostTextMessages {
  /** Announced politely when a suggestion appears; say how to accept it. */
  announce: (suggestion: string) => string;
  /** Description of the field while a suggestion is showing. */
  instruction: string;
  /** The key shown on the accept hint after the suggestion. */
  acceptKey: string;
  /**
   * Read after `acceptKey` to name the accept hint, which is also a button for
   * touch: "Tab to accept the suggestion". The name starts with the visible key.
   */
  accept: string;
  /** Announced when the suggestion is accepted. */
  accepted: string;
  /** Announced when the suggestion is dismissed with Escape. */
  dismissed: string;
}

export const AI_GHOST_TEXT_MESSAGES: AiGhostTextMessages = {
  announce: (suggestion) =>
    `Suggestion: ${suggestion}. Press Tab to accept it, Escape to dismiss it.`,
  instruction:
    'A suggestion is showing after the cursor. Press Tab to accept it, Control or Alt with the forward arrow to accept one word, Escape to dismiss it.',
  acceptKey: 'Tab',
  accept: 'to accept the suggestion',
  accepted: 'Suggestion accepted.',
  dismissed: 'Suggestion dismissed.',
};

export interface AiGhostTextProps extends HTMLAttributes<HTMLDivElement> {
  /**
   * The field to complete: one `<textarea>` or text `<input>`, or a component
   * that renders one (Textarea, Input, a Field around either). It can be
   * controlled or uncontrolled and keeps its own props and handlers.
   */
  children: ReactNode;
  /**
   * Asks the model for a continuation of the text at the cursor. Not needed
   * when `completion` is given.
   */
  complete?: AiCompletionFunction | undefined;
  /**
   * Completion state from your own `useAiCompletion`, when you want to read or
   * drive it yourself. Replaces `complete`, `debounceMs`, `minChars` and
   * `enabled`.
   */
  completion?: UseAiCompletionResult | undefined;
  /** How long typing must pause before a request is sent, in milliseconds. Default 300. */
  debounceMs?: number | undefined;
  /** Fewest non-space characters before the cursor for a request. Default 3. */
  minChars?: number | undefined;
  /** Turns suggestions off when false. Default true. */
  enabled?: boolean | undefined;
  /**
   * Shows the accept hint (the "Tab" key) after the suggestion. It is also a
   * button, so the suggestion can be accepted by touch. Default true.
   */
  showAcceptHint?: boolean | undefined;
  /** Called after a suggestion, or one word of it, went into the field. */
  onAccept?:
    ((acceptance: AiCompletionAcceptance, kind: AiGhostTextAcceptKind) => void) | undefined;
  /** Called when the person dismisses the suggestion with Escape. */
  onDismiss?: (() => void) | undefined;
  /** Called when `complete` throws or rejects. */
  onCompletionError?: ((error: unknown) => void) | undefined;
  /** Strings, for localisation. */
  messages?: Partial<AiGhostTextMessages> | undefined;
}

const NO_COMPLETION: AiCompletionFunction = () => null;

/** Typography and box properties the overlay copies from the field so the text lines up. */
const MIRRORED = [
  'boxSizing',
  'paddingTop',
  'paddingRight',
  'paddingBottom',
  'paddingLeft',
  'borderTopWidth',
  'borderRightWidth',
  'borderBottomWidth',
  'borderLeftWidth',
  'fontFamily',
  'fontSize',
  'fontWeight',
  'fontStyle',
  'fontVariantNumeric',
  'fontFeatureSettings',
  'letterSpacing',
  'wordSpacing',
  'lineHeight',
  'textTransform',
  'textIndent',
  'textAlign',
  'tabSize',
  'direction',
] as const;

/** Puts `value` in the field so that React, and any other listener, sees an edit. */
function writeField(field: Field, acceptance: AiCompletionAcceptance) {
  const ownerDocument = field.ownerDocument;
  let done = false;
  // Keeps the browser's undo history where the command still exists.
  if (ownerDocument.activeElement === field && typeof ownerDocument.execCommand === 'function') {
    try {
      done = ownerDocument.execCommand('insertText', false, acceptance.inserted);
    } catch {
      done = false;
    }
  }
  if (!done || field.value !== acceptance.text) {
    const proto =
      field.tagName === 'TEXTAREA' ? HTMLTextAreaElement.prototype : HTMLInputElement.prototype;
    const setter = Object.getOwnPropertyDescriptor(proto, 'value')?.set;
    // The prototype setter bypasses React's value tracker, so `onChange` fires.
    if (setter) setter.call(field, acceptance.text);
    else field.value = acceptance.text;
    field.dispatchEvent(new Event('input', { bubbles: true }));
  }
  try {
    field.setSelectionRange(acceptance.cursor, acceptance.cursor);
  } catch {
    // Input types without a text selection (email, number).
  }
}

/** The collapsed cursor of a field, or null when text is selected or it cannot be read. */
function caret(field: Field): number | null {
  let start: number | null;
  let end: number | null;
  try {
    start = field.selectionStart;
    end = field.selectionEnd;
  } catch {
    return null;
  }
  if (start === null || end === null) return field.value.length;
  return start === end ? start : null;
}

/**
 * Inline completion ("ghost text") for a textarea or text input. It shows the
 * model's suggested continuation as dimmed text after the cursor: Tab (or the
 * forward arrow at the end) accepts it, Control or Alt with the forward arrow
 * accepts one word, Escape dismisses it. Requests are debounced, stale ones are
 * aborted, and nothing is suggested while an input method is composing. The
 * suggestion is drawn over the field, so nothing moves when it appears.
 */
export const AiGhostText = forwardRef<HTMLDivElement, AiGhostTextProps>(function AiGhostText(
  {
    children,
    complete,
    completion: completionProp,
    debounceMs,
    minChars,
    enabled = true,
    showAcceptHint = true,
    onAccept,
    onDismiss,
    onCompletionError,
    messages,
    className,
    ...rest
  },
  ref,
) {
  const m = { ...AI_GHOST_TEXT_MESSAGES, ...messages };
  const own = useAiCompletion({
    complete: complete ?? NO_COMPLETION,
    debounceMs,
    minChars,
    enabled: enabled && !completionProp && Boolean(complete),
    onError: onCompletionError,
  });
  const completion = completionProp ?? own;
  const { suggestion, text, cursor, status } = completion;

  const hintId = useId();
  const root = useRef<HTMLDivElement | null>(null);
  const mirror = useRef<HTMLDivElement>(null);
  const [field, setField] = useState<Field | null>(null);
  const [announcement, setAnnouncement] = useState('');

  // Handlers are attached once per field; they read the latest of everything from here.
  const live = useRef({ completion, onAccept, onDismiss, m });
  live.current = { completion, onAccept, onDismiss, m };

  // The field is whatever text control the children render.
  useIsomorphicLayoutEffect(() => {
    const found = root.current?.querySelector<Field>('textarea, input') ?? null;
    if (found !== field) setField(found);
  });

  const take = useCallback((target: Field, kind: AiGhostTextAcceptKind) => {
    const { completion: c, onAccept: accepted, m: strings } = live.current;
    const acceptance = kind === 'word' ? c.acceptWord() : c.accept();
    if (!acceptance) return false;
    writeField(target, acceptance);
    if (kind === 'all') setAnnouncement(strings.accepted);
    accepted?.(acceptance, kind);
    return true;
  }, []);

  useEffect(() => {
    if (!field) return;
    let composing = false;

    const evaluate = () => {
      const c = live.current.completion;
      if (composing || field.disabled || field.readOnly) {
        c.dismiss();
        return;
      }
      const at = caret(field);
      // Only at the end of the text: a suggestion in the middle would cover what follows.
      if (at === null || field.value.slice(at).trim() !== '') c.dismiss();
      else c.update(field.value, at);
    };

    const onInput = (event: Event) => {
      if (composing || (event as InputEvent).isComposing) return;
      evaluate();
    };
    const onCompositionStart = () => {
      composing = true;
      live.current.completion.setComposing(true);
    };
    const onCompositionEnd = () => {
      composing = false;
      live.current.completion.setComposing(false);
      evaluate();
    };
    const onKeyDown = (event: KeyboardEvent) => {
      const c = live.current.completion;
      if (composing || event.isComposing || event.keyCode === 229) return;
      if (c.status !== 'ready' || !c.suggestion) return;
      const forward = elementDirection(field) === 'rtl' ? 'ArrowLeft' : 'ArrowRight';
      const plain = !event.altKey && !event.ctrlKey && !event.metaKey && !event.shiftKey;
      let handled = false;
      if (event.key === 'Tab' && plain) handled = take(field, 'all');
      else if (event.key === 'Escape' && plain) {
        c.dismiss();
        setAnnouncement(live.current.m.dismissed);
        live.current.onDismiss?.();
        handled = true;
      } else if (event.key === forward && caret(field) === c.cursor) {
        if (plain) handled = take(field, 'all');
        else if ((event.ctrlKey || event.altKey) && !event.metaKey && !event.shiftKey)
          handled = take(field, 'word');
      }
      if (handled) {
        event.preventDefault();
        // The key was for the suggestion: a dialog must not also close, nor focus move.
        event.stopPropagation();
      }
    };
    // The caret left the place the suggestion belongs to.
    const onCaretMove = () => {
      const c = live.current.completion;
      if (c.status === 'idle') return;
      if (caret(field) !== c.cursor || field.value !== c.text) c.dismiss();
    };
    const onBlur = () => live.current.completion.dismiss();

    field.addEventListener('input', onInput);
    field.addEventListener('compositionstart', onCompositionStart);
    field.addEventListener('compositionend', onCompositionEnd);
    field.addEventListener('keydown', onKeyDown as EventListener);
    field.addEventListener('keyup', onCaretMove);
    field.addEventListener('pointerup', onCaretMove);
    field.addEventListener('blur', onBlur);
    return () => {
      field.removeEventListener('input', onInput);
      field.removeEventListener('compositionstart', onCompositionStart);
      field.removeEventListener('compositionend', onCompositionEnd);
      field.removeEventListener('keydown', onKeyDown as EventListener);
      field.removeEventListener('keyup', onCaretMove);
      field.removeEventListener('pointerup', onCaretMove);
      field.removeEventListener('blur', onBlur);
      live.current.completion.dismiss();
    };
  }, [field, take]);

  const showing = status === 'ready' && suggestion !== '' && field !== null;

  // A value set from outside (a reset, a controlled change) makes the suggestion stale.
  useIsomorphicLayoutEffect(() => {
    if (field && status !== 'idle' && field.value !== text) live.current.completion.dismiss();
  });

  // The overlay sits exactly over the field and copies its typography and scroll.
  useIsomorphicLayoutEffect(() => {
    const overlay = mirror.current;
    const host = root.current;
    if (!overlay || !host || !field || !showing) return;
    const sync = () => {
      const style = getComputedStyle(field);
      for (const property of MIRRORED) overlay.style[property] = style[property];
      // A scrollbar narrows the field's text box; the overlay gives up the same room.
      const borders = parseFloat(style.borderLeftWidth) + parseFloat(style.borderRightWidth);
      const scrollbar = field.offsetWidth - field.clientWidth - (borders || 0);
      if (scrollbar > 0) {
        const side = style.direction === 'rtl' ? 'paddingLeft' : 'paddingRight';
        overlay.style[side] = `${(parseFloat(style[side]) || 0) + scrollbar}px`;
      }
      const box = field.getBoundingClientRect();
      const origin = host.getBoundingClientRect();
      // Physical on purpose: measured boxes are physical in every writing direction.
      overlay.style.left = `${box.left - origin.left}px`;
      overlay.style.top = `${box.top - origin.top}px`;
      overlay.style.width = `${box.width}px`;
      overlay.style.height = `${box.height}px`;
      overlay.scrollTop = field.scrollTop;
      overlay.scrollLeft = field.scrollLeft;
    };
    sync();
    field.addEventListener('scroll', sync, { passive: true });
    const observer = typeof ResizeObserver === 'function' ? new ResizeObserver(sync) : null;
    observer?.observe(field);
    return () => {
      field.removeEventListener('scroll', sync);
      observer?.disconnect();
    };
  }, [field, showing, suggestion, text, cursor]);

  // The field is described by the instruction only while a suggestion shows.
  useIsomorphicLayoutEffect(() => {
    if (!field) return;
    const ids = (field.getAttribute('aria-describedby') ?? '').split(/\s+/).filter(Boolean);
    const has = ids.includes(hintId);
    if (showing && !has) field.setAttribute('aria-describedby', [...ids, hintId].join(' '));
    else if (!showing && has) {
      const others = ids.filter((id) => id !== hintId);
      if (others.length) field.setAttribute('aria-describedby', others.join(' '));
      else field.removeAttribute('aria-describedby');
    }
  });
  useEffect(
    () => () => {
      if (!field) return;
      const others = (field.getAttribute('aria-describedby') ?? '')
        .split(/\s+/)
        .filter((id) => id && id !== hintId);
      if (others.length) field.setAttribute('aria-describedby', others.join(' '));
      else field.removeAttribute('aria-describedby');
    },
    [field, hintId],
  );

  // A new suggestion is news; the rest of one being typed through or taken by word is not.
  const announced = useRef('');
  useEffect(() => {
    if (!showing) {
      announced.current = '';
      return;
    }
    if (announced.current.endsWith(suggestion)) return;
    announced.current = suggestion;
    setAnnouncement(live.current.m.announce(suggestion));
  }, [showing, suggestion]);

  return (
    <div
      ref={(node) => {
        root.current = node;
        setRefs(node, ref);
      }}
      {...rest}
      className={cx('mn-ai-ghost-text', className)}
      data-status={status}
      data-suggesting={showing || undefined}
    >
      {children}
      <div
        ref={mirror}
        className="mn-ai-ghost-text__overlay"
        data-multiline={field?.tagName === 'TEXTAREA' || undefined}
        hidden={!showing}
      >
        {showing ? (
          <>
            <span className="mn-ai-ghost-text__before" aria-hidden="true">
              {text.slice(0, cursor)}
            </span>
            <span className="mn-ai-ghost-text__suggestion" aria-hidden="true" dir="auto">
              {suggestion}
            </span>
            {showAcceptHint ? (
              <button
                type="button"
                className="mn-ai-ghost-text__accept"
                // Not a tab stop: Tab itself accepts. It is here for touch and for touch screen readers.
                tabIndex={-1}
                // Keeps focus (and the suggestion) in the field.
                onPointerDown={(event) => event.preventDefault()}
                onMouseDown={(event) => event.preventDefault()}
                onClick={() => {
                  if (field) take(field, 'all');
                }}
              >
                {m.acceptKey}
                <span className="mn-visually-hidden"> {m.accept}</span>
              </button>
            ) : null}
          </>
        ) : null}
      </div>
      <span id={hintId} className="mn-visually-hidden">
        {showing ? m.instruction : ''}
      </span>
      <span className="mn-visually-hidden" role="status" aria-live="polite" aria-atomic="true">
        {announcement}
      </span>
    </div>
  );
});
