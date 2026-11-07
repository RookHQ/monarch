'use client';

import {
  forwardRef,
  useEffect,
  useId,
  useLayoutEffect,
  useRef,
  useState,
  type ComponentPropsWithoutRef,
  type FormEvent,
  type KeyboardEvent as ReactKeyboardEvent,
} from 'react';
import { cx } from '../../../internal/cx.js';
import { BotGlyph } from '../../../internal/icons.js';
import { Button, IconButton } from '../../actions/button/index.js';
import { Popover, PopoverAnchor, PopoverContent } from '../../overlays/popover/index.js';

/** A rectangle the prompt is positioned against, e.g. a text selection's bounds. */
export type AiInlinePromptAnchor = DOMRect | (() => DOMRect | null);

export interface AiInlinePromptMessages {
  /** Name of the floating panel. */
  label: string;
  /** Placeholder of the prompt field. */
  placeholder: string;
  /** Accessible name of the prompt field. */
  inputLabel: string;
  /** Label of the submit button. */
  submit: string;
  /** Label before the context excerpt, e.g. "Selection". */
  context: string;
  /** Hint under the field. */
  hint: string;
  /** Name of the suggestion group. */
  suggestions: string;
}

export const AI_INLINE_PROMPT_MESSAGES: AiInlinePromptMessages = {
  label: 'Ask AI',
  placeholder: 'Ask AI to edit or write…',
  inputLabel: 'Prompt',
  submit: 'Send',
  context: 'Selection',
  hint: 'Enter to send · Shift+Enter for a new line · Esc to close',
  suggestions: 'Suggestions',
};

type ContentProps = Omit<
  ComponentPropsWithoutRef<typeof PopoverContent>,
  'onSubmit' | 'children' | 'onOpenAutoFocus' | 'onCloseAutoFocus'
>;

export interface AiInlinePromptProps extends ContentProps {
  /**
   * Where to show the prompt: a `DOMRect` (e.g. `range.getBoundingClientRect()`)
   * or a function returning the current one, so it follows scrolling.
   */
  anchor: AiInlinePromptAnchor;
  /** Whether the prompt is shown. */
  open: boolean;
  /** Called to open or close it (Escape, clicking outside, after submit). */
  onOpenChange: (open: boolean) => void;
  /** Called with the trimmed prompt. Never called for an empty prompt. */
  onSubmit: (text: string) => void;
  /** What the prompt acts on, e.g. the selected text. Shown as a short excerpt. */
  context?: string | undefined;
  /** Quick prompts shown as buttons, e.g. "Shorten", "Fix spelling". Clicking one submits it. */
  suggestions?: readonly string[] | undefined;
  /** Text the field starts with each time it opens. */
  defaultValue?: string | undefined;
  /** Close after submitting. Default true. */
  closeOnSubmit?: boolean | undefined;
  /** A request is being sent: the field and buttons are disabled. */
  pending?: boolean | undefined;
  /** Where focus goes when it closes. Default: the element focused when it opened. */
  returnFocusTo?: HTMLElement | null | undefined;
  /** Strings, for localisation. */
  messages?: Partial<AiInlinePromptMessages> | undefined;
}

// useLayoutEffect warns during server rendering; focus work only runs in the browser.
const useIsoLayoutEffect = typeof window === 'undefined' ? useEffect : useLayoutEffect;

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

const EMPTY_RECT = {
  x: 0,
  y: 0,
  width: 0,
  height: 0,
  top: 0,
  left: 0,
  right: 0,
  bottom: 0,
  toJSON: () => ({}),
} as DOMRect;

/**
 * A ⌘K-style prompt that floats over a text selection or any rectangle: type
 * an instruction about the selected content and send it. Positioned with the
 * Popover's floating logic, so it flips and stays on screen. Escape or
 * clicking outside closes it, and focus returns to where it was.
 */
export const AiInlinePrompt = forwardRef<HTMLDivElement, AiInlinePromptProps>(
  function AiInlinePrompt(
    {
      anchor,
      open,
      onOpenChange,
      onSubmit,
      context,
      suggestions,
      defaultValue = '',
      closeOnSubmit = true,
      pending = false,
      returnFocusTo,
      messages: messagesProp,
      className,
      side = 'bottom',
      align = 'start',
      ...rest
    },
    ref,
  ) {
    const m = { ...AI_INLINE_PROMPT_MESSAGES, ...messagesProp };
    const ids = useId();
    const hintId = `${ids}-hint`;
    const contextId = `${ids}-context`;
    const [text, setText] = useState(defaultValue);
    const fieldRef = useRef<HTMLTextAreaElement>(null);
    const previousFocus = useRef<HTMLElement | null>(null);
    const anchorRef = useRef(anchor);
    anchorRef.current = anchor;
    const virtualRef = useRef({
      getBoundingClientRect: (): DOMRect => {
        const a = anchorRef.current;
        return (typeof a === 'function' ? a() : a) ?? EMPTY_RECT;
      },
    });

    // Remember what had focus before the panel takes it (layout effects run
    // before the popover's focus effect), and reset the text on each open.
    useIsoLayoutEffect(() => {
      if (!open) return;
      previousFocus.current =
        document.activeElement instanceof HTMLElement ? document.activeElement : null;
      setText(defaultValue);
    }, [open]);

    // Grow with the text, up to the CSS max height.
    useEffect(() => {
      const el = fieldRef.current;
      if (!el) return;
      el.style.blockSize = 'auto';
      el.style.blockSize = `${el.scrollHeight}px`;
    }, [text, open]);

    const send = (value: string) => {
      const trimmed = value.trim();
      if (!trimmed || pending) return;
      onSubmit(trimmed);
      if (closeOnSubmit) onOpenChange(false);
    };

    const onFormSubmit = (event: FormEvent) => {
      event.preventDefault();
      send(text);
    };

    const onFieldKeyDown = (event: ReactKeyboardEvent<HTMLTextAreaElement>) => {
      if (event.key === 'Enter' && !event.shiftKey && !event.nativeEvent.isComposing) {
        event.preventDefault();
        send(text);
      }
    };

    const excerpt =
      context && context.length > 140 ? `${context.slice(0, 140).trimEnd()}…` : context;

    return (
      <Popover open={open} onOpenChange={onOpenChange}>
        <PopoverAnchor virtualRef={virtualRef} />
        <PopoverContent
          ref={ref}
          aria-label={m.label}
          side={side}
          align={align}
          {...rest}
          className={cx('mn-ai-inline-prompt', className)}
          data-pending={pending || undefined}
          onOpenAutoFocus={(event) => {
            event.preventDefault();
            const el = fieldRef.current;
            if (el) {
              el.focus();
              el.setSelectionRange(el.value.length, el.value.length);
            }
          }}
          onCloseAutoFocus={(event) => {
            event.preventDefault();
            const target = returnFocusTo ?? previousFocus.current;
            if (target?.isConnected) target.focus({ preventScroll: true });
          }}
        >
          <form className="mn-ai-inline-prompt__form" onSubmit={onFormSubmit}>
            {excerpt ? (
              <p id={contextId} className="mn-ai-inline-prompt__context">
                <span className="mn-ai-inline-prompt__context-label">{m.context}:</span>{' '}
                <span className="mn-ai-inline-prompt__context-text">{excerpt}</span>
              </p>
            ) : null}
            <div className="mn-ai-inline-prompt__row">
              <BotGlyph aria-hidden="true" className="mn-ai-inline-prompt__glyph" />
              <textarea
                ref={fieldRef}
                rows={1}
                className="mn-ai-inline-prompt__field"
                aria-label={m.inputLabel}
                aria-describedby={excerpt ? `${contextId} ${hintId}` : hintId}
                placeholder={m.placeholder}
                value={text}
                disabled={pending}
                onChange={(event) => setText(event.target.value)}
                onKeyDown={onFieldKeyDown}
              />
              <IconButton
                type="submit"
                size="sm"
                variant="primary"
                className="mn-ai-inline-prompt__submit"
                label={m.submit}
                icon={<SendArrowGlyph />}
                loading={pending}
                disabled={!text.trim()}
              />
            </div>
            {suggestions?.length ? (
              <div
                role="group"
                aria-label={m.suggestions}
                className="mn-ai-inline-prompt__suggestions"
              >
                {suggestions.map((s) => (
                  <Button
                    key={s}
                    type="button"
                    size="xs"
                    variant="ghost"
                    className="mn-ai-inline-prompt__suggestion"
                    disabled={pending}
                    onClick={() => send(s)}
                  >
                    {s}
                  </Button>
                ))}
              </div>
            ) : null}
            <p id={hintId} className="mn-ai-inline-prompt__hint">
              {m.hint}
            </p>
          </form>
        </PopoverContent>
      </Popover>
    );
  },
);
