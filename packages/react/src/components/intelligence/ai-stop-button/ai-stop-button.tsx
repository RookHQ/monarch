'use client';

import {
  forwardRef,
  useEffect,
  useMemo,
  useRef,
  type ButtonHTMLAttributes,
  type HTMLAttributes,
  type SVGProps,
} from 'react';
import { Button } from '../../actions/button/index.js';
import { cx } from '../../../internal/cx.js';

/** Every string AiStopButton shows or announces. */
export interface AiStopButtonMessages {
  /** The stop button's label. Default "Stop". */
  stop: string;
  /** Shown (and announced politely) once the response has stopped. Default "Stopped.". */
  stopped: string;
  /** The action that resumes the response. Default "Continue". */
  continue: string;
}

const DEFAULT_MESSAGES: AiStopButtonMessages = {
  stop: 'Stop',
  stopped: 'Stopped.',
  continue: 'Continue',
};

/** The filled square used for every Stop control in the AI components. */
export function AiStopGlyph(props: SVGProps<SVGSVGElement>) {
  return (
    <svg width={16} height={16} viewBox="0 0 16 16" aria-hidden focusable={false} {...props}>
      <rect x="3.5" y="3.5" width="9" height="9" rx="1.5" fill="currentColor" />
    </svg>
  );
}

export interface AiStopButtonProps extends Omit<HTMLAttributes<HTMLDivElement>, 'children'> {
  /** Called when the person presses Stop. Abort the request here (e.g. `useAiConversation().stop`). */
  onStop?: () => void;
  /**
   * The response has stopped: the button is replaced by "Stopped." and, with
   * `onContinue`, a Continue action. Focus moves to Continue (or the message)
   * if it was on the Stop button.
   */
  stopped?: boolean;
  /** Adds a Continue action to the stopped state, e.g. to regenerate or resume. */
  onContinue?: () => void;
  /** Show the "Stop" text next to the square. When false the text is visually hidden. Default true. */
  showLabel?: boolean;
  /** Button size. Every size keeps a 44px touch target. Default `sm`. */
  size?: 'sm' | 'md';
  /** Disables the Stop button, e.g. while the abort is already in flight. */
  disabled?: boolean;
  /** Extra props for the inner Stop `<button>` (ids, `aria-keyshortcuts`, data attributes). */
  buttonProps?: Omit<ButtonHTMLAttributes<HTMLButtonElement>, 'onClick' | 'children'>;
  /** Override any string. */
  messages?: Partial<AiStopButtonMessages>;
}

/**
 * The visible Stop control for a streaming AI response: a square stop glyph
 * with a visible or visually hidden "Stop" label, and a `stopped` state that
 * says "Stopped." with an optional Continue action. Escape is the caller's job:
 * wire it to the same `onStop` where the prompt or thread has focus
 * (AiPromptInput already does).
 */
export const AiStopButton = forwardRef<HTMLDivElement, AiStopButtonProps>(function AiStopButton(
  {
    onStop,
    stopped = false,
    onContinue,
    showLabel = true,
    size = 'sm',
    disabled,
    buttonProps,
    messages: messagesProp,
    className,
    ...rest
  },
  ref,
) {
  const m = useMemo(() => ({ ...DEFAULT_MESSAGES, ...messagesProp }), [messagesProp]);
  const statusRef = useRef<HTMLSpanElement>(null);
  const continueRef = useRef<HTMLButtonElement>(null);
  // Removing the focused Stop button would drop focus to <body>; remember it had focus.
  const hadFocus = useRef(false);

  useEffect(() => {
    if (!stopped || !hadFocus.current) return;
    hadFocus.current = false;
    (continueRef.current ?? statusRef.current)?.focus();
  }, [stopped]);

  return (
    <div
      ref={ref}
      {...rest}
      className={cx('mn-ai-stop-button', className)}
      data-state={stopped ? 'stopped' : 'running'}
    >
      {stopped ? null : (
        <Button
          {...buttonProps}
          type="button"
          size={size}
          variant="secondary"
          className={cx('mn-ai-stop-button__button', buttonProps?.className)}
          data-label={showLabel ? 'visible' : 'hidden'}
          disabled={disabled}
          onFocus={(e) => {
            hadFocus.current = true;
            buttonProps?.onFocus?.(e);
          }}
          onBlur={(e) => {
            // Only a move to another element clears it; removing the button does not.
            if (e.relatedTarget) hadFocus.current = false;
            buttonProps?.onBlur?.(e);
          }}
          onClick={() => onStop?.()}
        >
          <span className="mn-ai-stop-button__glyph" aria-hidden>
            <AiStopGlyph />
          </span>
          <span className={showLabel ? 'mn-ai-stop-button__label' : 'mn-visually-hidden'}>
            {m.stop}
          </span>
        </Button>
      )}
      <span
        ref={statusRef}
        role="status"
        tabIndex={stopped && !onContinue ? -1 : undefined}
        className="mn-ai-stop-button__status"
      >
        {stopped ? m.stopped : ''}
      </span>
      {stopped && onContinue ? (
        <Button
          ref={continueRef}
          type="button"
          size={size}
          variant="ghost"
          className="mn-ai-stop-button__continue"
          onClick={onContinue}
        >
          {m.continue}
        </Button>
      ) : null}
    </div>
  );
});
