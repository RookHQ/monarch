'use client';

import {
  forwardRef,
  useCallback,
  useEffect,
  useLayoutEffect,
  useRef,
  useState,
  type ForwardedRef,
  type HTMLAttributes,
  type KeyboardEvent as ReactKeyboardEvent,
  type ReactNode,
  type SVGProps,
} from 'react';
import { cx } from '../../../internal/cx.js';
import { CheckGlyph, CopyGlyph, PencilGlyph, RetryGlyph } from '../../../internal/icons.js';
import { IconButton } from '../../actions/button/index.js';
import { Tooltip, TooltipProvider } from '../../overlays/tooltip/index.js';

const useIsomorphicLayoutEffect = typeof window === 'undefined' ? useEffect : useLayoutEffect;

const glyph = {
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

function ThumbUpGlyph(props: SVGProps<SVGSVGElement>) {
  return (
    <svg {...glyph} {...props}>
      <path d="M5 7v6.5H2.75V7H5zM5 7l2.5-4.75c.9 0 1.6.75 1.5 1.65L8.75 6.5h3.6a1.25 1.25 0 0 1 1.22 1.5l-.95 4.5a1.25 1.25 0 0 1-1.22 1H5" />
    </svg>
  );
}

function ThumbDownGlyph(props: SVGProps<SVGSVGElement>) {
  return (
    <svg {...glyph} {...props}>
      <path d="M11 9V2.5h2.25V9H11zM11 9l-2.5 4.75c-.9 0-1.6-.75-1.5-1.65L7.25 9.5h-3.6A1.25 1.25 0 0 1 2.43 8l.95-4.5a1.25 1.25 0 0 1 1.22-1H11" />
    </svg>
  );
}

function ShareGlyph(props: SVGProps<SVGSVGElement>) {
  return (
    <svg {...glyph} {...props}>
      <path d="M8 2.5v7.5M5 5.25 8 2.5l3 2.75" />
      <path d="M3.5 8.5v4a1 1 0 0 0 1 1h7a1 1 0 0 0 1-1v-4" />
    </svg>
  );
}

function SpeakerGlyph(props: SVGProps<SVGSVGElement>) {
  return (
    <svg {...glyph} {...props}>
      <path d="M2.5 6.25v3.5h2.25L8 12.5v-9L4.75 6.25H2.5z" />
      <path d="M10.5 5.75a3 3 0 0 1 0 4.5M12.25 4a5.5 5.5 0 0 1 0 8" />
    </svg>
  );
}

function StopGlyph(props: SVGProps<SVGSVGElement>) {
  return (
    <svg {...glyph} {...props}>
      <rect x="4" y="4" width="8" height="8" rx="1.25" />
    </svg>
  );
}

export type AiFeedback = 'up' | 'down';

/** Every visible and assistive-technology string. English defaults. */
export interface AiMessageActionsMessages {
  /** Accessible name of the toolbar. */
  toolbar: string;
  copy: string;
  /** Shown for two seconds after copying. */
  copied: string;
  regenerate: string;
  edit: string;
  /** Thumbs up. */
  good: string;
  /** Thumbs down. */
  bad: string;
  share: string;
  readAloud: string;
  stopReading: string;
}

const DEFAULT_MESSAGES: AiMessageActionsMessages = {
  toolbar: 'Message actions',
  copy: 'Copy',
  copied: 'Copied',
  regenerate: 'Regenerate',
  edit: 'Edit',
  good: 'Good response',
  bad: 'Bad response',
  share: 'Share',
  readAloud: 'Read aloud',
  stopReading: 'Stop reading',
};

export interface AiMessageActionsProps extends Omit<
  HTMLAttributes<HTMLDivElement>,
  'children' | 'onCopy'
> {
  /** The message's plain text: what Copy copies and Read aloud speaks. */
  messageText?: string | undefined;
  /** Shows Copy. Receives `messageText`; may return a promise. Shows "Copied" when it settles. */
  onCopy?: ((text: string) => void | Promise<void>) | undefined;
  /** Shows Regenerate. */
  onRegenerate?: (() => void) | undefined;
  /** Shows Edit (for the person's own messages). */
  onEdit?: (() => void) | undefined;
  /** The current rating, shown as pressed thumbs. */
  feedback?: AiFeedback | undefined;
  /**
   * Shows thumbs up and down. Called with the new rating, or `undefined` when
   * the pressed thumb is pressed again to clear it.
   */
  onFeedback?: ((feedback: AiFeedback | undefined) => void) | undefined;
  /** Shows Share. */
  onShare?: (() => void) | undefined;
  /**
   * Shows Read aloud when the browser has speech synthesis. The toolbar speaks
   * `messageText` itself and toggles the button to Stop; this is called with
   * `true` when speaking starts and `false` when it ends or is stopped.
   */
  onReadAloud?: ((speaking: boolean) => void) | undefined;
  /** Extra controls at the end, e.g. a DropdownMenu trigger. They join the arrow-key order. */
  more?: ReactNode | undefined;
  /**
   * `hover` (default): on pointer devices the toolbar appears when the message
   * is hovered or anything in it has focus; on touch it is always visible.
   * `always`: always visible.
   */
  visibility?: 'hover' | 'always' | undefined;
  /** Labels and tooltips. */
  messages?: Partial<AiMessageActionsMessages> | undefined;
}

function canSpeak(): boolean {
  return (
    typeof window !== 'undefined' &&
    'speechSynthesis' in window &&
    typeof window.SpeechSynthesisUtterance === 'function'
  );
}

function setRef<T>(ref: ForwardedRef<T>, value: T | null) {
  if (typeof ref === 'function') ref(value);
  else if (ref) ref.current = value;
}

const ITEMS = 'button:not([disabled]),[href],[role="menuitem"]';

/**
 * The action row under a chat message: copy, regenerate, edit, rate, share,
 * read aloud and a slot for more. An action appears only when its callback is
 * given. It is one tab stop: arrow keys move between the buttons.
 */
export const AiMessageActions = forwardRef<HTMLDivElement, AiMessageActionsProps>(
  function AiMessageActions(
    {
      messageText = '',
      onCopy,
      onRegenerate,
      onEdit,
      feedback,
      onFeedback,
      onShare,
      onReadAloud,
      more,
      visibility = 'hover',
      messages: messagesProp,
      className,
      onKeyDown,
      onFocus,
      ...rest
    },
    ref,
  ) {
    const m = { ...DEFAULT_MESSAGES, ...messagesProp };
    const rootRef = useRef<HTMLDivElement | null>(null);
    const [active, setActive] = useState(0);
    const [copied, setCopied] = useState(false);
    const [speaking, setSpeaking] = useState(false);
    // Speech support is only known in the browser; render nothing for it on
    // the server and in the first client pass so hydration matches.
    const [speechReady, setSpeechReady] = useState(false);
    useEffect(() => setSpeechReady(canSpeak()), []);

    const readAloudRef = useRef(onReadAloud);
    readAloudRef.current = onReadAloud;
    const utterance = useRef<SpeechSynthesisUtterance | null>(null);

    const stopSpeaking = useCallback(() => {
      if (utterance.current && canSpeak()) {
        utterance.current = null;
        window.speechSynthesis.cancel();
      }
      setSpeaking(false);
    }, []);
    useEffect(
      () => () => {
        if (utterance.current && canSpeak()) window.speechSynthesis.cancel();
      },
      [],
    );

    const copyTimer = useRef<ReturnType<typeof setTimeout>>(undefined);
    useEffect(() => () => clearTimeout(copyTimer.current), []);

    const items = () =>
      rootRef.current
        ? Array.from(rootRef.current.querySelectorAll<HTMLElement>(ITEMS)).filter(
            (el) => !el.closest('[aria-hidden="true"]'),
          )
        : [];

    useIsomorphicLayoutEffect(() => {
      const list = items();
      const i = Math.max(0, Math.min(active, list.length - 1));
      list.forEach((el, n) => {
        el.tabIndex = n === i ? 0 : -1;
      });
    });

    const handleKeyDown = (e: ReactKeyboardEvent<HTMLDivElement>) => {
      onKeyDown?.(e);
      if (e.defaultPrevented) return;
      const list = items();
      const i = list.indexOf(e.target as HTMLElement);
      if (i === -1) return;
      const rtl = getComputedStyle(e.currentTarget).direction === 'rtl';
      const fwd = rtl ? 'ArrowLeft' : 'ArrowRight';
      const back = rtl ? 'ArrowRight' : 'ArrowLeft';
      let next = -1;
      if (e.key === fwd) next = (i + 1) % list.length;
      else if (e.key === back) next = (i - 1 + list.length) % list.length;
      else if (e.key === 'Home') next = 0;
      else if (e.key === 'End') next = list.length - 1;
      if (next === -1) return;
      e.preventDefault();
      setActive(next);
      list[next]?.focus();
    };

    const copy = async () => {
      if (!onCopy) return;
      try {
        await onCopy(messageText);
        setCopied(true);
        clearTimeout(copyTimer.current);
        copyTimer.current = setTimeout(() => setCopied(false), 2000);
      } catch {
        setCopied(false);
      }
    };

    const toggleSpeech = () => {
      if (speaking) {
        stopSpeaking();
        readAloudRef.current?.(false);
        return;
      }
      if (!canSpeak()) return;
      const u = new window.SpeechSynthesisUtterance(messageText);
      const end = () => {
        if (utterance.current !== u) return;
        utterance.current = null;
        setSpeaking(false);
        readAloudRef.current?.(false);
      };
      u.onend = end;
      u.onerror = end;
      window.speechSynthesis.cancel();
      utterance.current = u;
      window.speechSynthesis.speak(u);
      setSpeaking(true);
      readAloudRef.current?.(true);
    };

    const action = (key: string, label: string, icon: ReactNode, props: object) => (
      <Tooltip key={key} content={label}>
        <IconButton
          size="sm"
          label={label}
          icon={icon}
          className="mn-ai-message-actions__button"
          data-action={key}
          {...props}
        />
      </Tooltip>
    );

    return (
      <TooltipProvider>
        <div
          ref={(node) => {
            rootRef.current = node;
            setRef(ref, node);
          }}
          role="toolbar"
          aria-label={m.toolbar}
          aria-orientation="horizontal"
          {...rest}
          className={cx('mn-ai-message-actions', className)}
          data-visibility={visibility}
          onKeyDown={handleKeyDown}
          onFocus={(e) => {
            onFocus?.(e);
            const i = items().indexOf(e.target as HTMLElement);
            if (i !== -1 && i !== active) setActive(i);
          }}
        >
          {onCopy
            ? action('copy', copied ? m.copied : m.copy, copied ? <CheckGlyph /> : <CopyGlyph />, {
                onClick: copy,
                'data-copied': copied || undefined,
              })
            : null}
          {onRegenerate
            ? action('regenerate', m.regenerate, <RetryGlyph />, { onClick: onRegenerate })
            : null}
          {onEdit ? action('edit', m.edit, <PencilGlyph />, { onClick: onEdit }) : null}
          {onFeedback ? (
            <>
              {action('good', m.good, <ThumbUpGlyph />, {
                'aria-pressed': feedback === 'up',
                'data-pressed': feedback === 'up' || undefined,
                onClick: () => onFeedback(feedback === 'up' ? undefined : 'up'),
              })}
              {action('bad', m.bad, <ThumbDownGlyph />, {
                'aria-pressed': feedback === 'down',
                'data-pressed': feedback === 'down' || undefined,
                onClick: () => onFeedback(feedback === 'down' ? undefined : 'down'),
              })}
            </>
          ) : null}
          {onShare ? action('share', m.share, <ShareGlyph />, { onClick: onShare }) : null}
          {onReadAloud && speechReady
            ? action(
                'read-aloud',
                speaking ? m.stopReading : m.readAloud,
                speaking ? <StopGlyph /> : <SpeakerGlyph />,
                { onClick: toggleSpeech, 'data-speaking': speaking || undefined },
              )
            : null}
          {more}
        </div>
      </TooltipProvider>
    );
  },
);
