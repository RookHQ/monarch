'use client';

import {
  forwardRef,
  useEffect,
  useRef,
  useState,
  type CSSProperties,
  type HTMLAttributes,
  type KeyboardEvent as ReactKeyboardEvent,
  type ReactNode,
  type SVGProps,
} from 'react';
import { cx } from '../../../internal/cx.js';
import { CloseGlyph, ErrorGlyph, RetryGlyph } from '../../../internal/icons.js';
import { useControllable } from '../../../internal/use-controllable.js';
import { Button } from '../../actions/button/index.js';
import { AiAgentAvatar } from '../ai-agent-avatar/index.js';
import type { AiAgentRef } from '../ai-conversation/index.js';

/**
 * Where the spoken conversation is. `connecting`: the call is being set up.
 * `listening`: the assistant hears the person. `thinking`: it is preparing a
 * reply. `speaking`: it is talking. `muted`: the person's microphone is off.
 * `reconnecting`: the connection dropped and is being restored. `ended`: the
 * call is over. `error`: it failed.
 */
export type AiVoiceModeState =
  | 'connecting'
  | 'listening'
  | 'thinking'
  | 'speaking'
  | 'muted'
  | 'reconnecting'
  | 'ended'
  | 'error';

/** Who said a caption. */
export type AiVoiceSpeaker = 'user' | 'assistant';

/** One line of live captions. */
export interface AiVoiceCaption {
  /** Stable id. Keep it the same while an interim caption is refined and when it becomes final. */
  id: string;
  /** Who is talking. */
  speaker: AiVoiceSpeaker;
  /** The words so far. */
  text: string;
  /**
   * False while the words may still change (shown, updated in place, not
   * announced); true once settled (announced once to assistive technology).
   */
  final: boolean;
}

/** `md` stands on its own; `compact` fits inside an assistant panel. */
export type AiVoiceModeSize = 'md' | 'compact';

/** Every visible and assistive-technology string. English defaults. */
export interface AiVoiceModeMessages {
  /** Accessible name of the whole surface, from the assistant's name. */
  label: (agentName: string) => string;
  /** Name used for the assistant when no `agent` is given. */
  assistant: string;
  /** Speaker label on the person's own captions. */
  you: string;
  /** Always-visible state text, also announced when the state changes. */
  states: Record<AiVoiceModeState, string>;
  /** Shown beside the state text when the microphone is muted in a state other than `muted`. */
  microphoneMuted: string;
  /** Mute toggle (pressed while the microphone is muted). */
  mute: string;
  /** Interrupt button, shown while the assistant is thinking or speaking. */
  interrupt: string;
  /** Captions toggle (pressed while captions show). */
  captions: string;
  /** End button. */
  end: string;
  /** Start again button on the `ended` state. */
  restart: string;
  /** Retry button on the `error` state. */
  retry: string;
  /** Accessible name of the captions log. */
  captionsLabel: string;
  /** Shown in the captions log before anyone has spoken. */
  captionsEmpty: string;
  /** Visually hidden prefix read before the running time, e.g. "Call time". */
  elapsed: string;
  /** Duration line on the `ended` state, from the formatted time ("4:05"). */
  lasted: (duration: string) => string;
  /** Error text when `error` is not given. */
  errorFallback: string;
  /** Announced when the state becomes `error`, from the state text and the error text. */
  announceError: (state: string, error: string) => string;
}

export const AI_VOICE_MODE_MESSAGES: AiVoiceModeMessages = {
  label: (agentName) => `Voice conversation with ${agentName}`,
  assistant: 'Assistant',
  you: 'You',
  states: {
    connecting: 'Connecting…',
    listening: 'Listening…',
    thinking: 'Thinking…',
    speaking: 'Speaking…',
    muted: 'Muted',
    reconnecting: 'Reconnecting…',
    ended: 'Conversation ended',
    error: 'Something went wrong',
  },
  microphoneMuted: 'Your microphone is muted',
  mute: 'Mute',
  interrupt: 'Interrupt',
  captions: 'Captions',
  end: 'End',
  restart: 'Start again',
  retry: 'Retry',
  captionsLabel: 'Captions',
  captionsEmpty: 'Captions appear here as you talk.',
  elapsed: 'Call time',
  lasted: (duration) => `Lasted ${duration}`,
  errorFallback: 'The voice conversation stopped unexpectedly.',
  announceError: (state, error) => `${state}: ${error}`,
};

/**
 * Formats a whole number of seconds as a clock, `4:05` or `1:02:09`, with the
 * locale's digits.
 */
export function formatAiVoiceDuration(seconds: number, locale?: string): string {
  const total = Number.isFinite(seconds) ? Math.max(0, Math.floor(seconds)) : 0;
  const lead = new Intl.NumberFormat(locale, { useGrouping: false });
  const pad = new Intl.NumberFormat(locale, { minimumIntegerDigits: 2, useGrouping: false });
  const h = Math.floor(total / 3600);
  const m = Math.floor((total % 3600) / 60);
  const s = total % 60;
  return h > 0
    ? `${lead.format(h)}:${pad.format(m)}:${pad.format(s)}`
    : `${lead.format(m)}:${pad.format(s)}`;
}

/** ISO 8601 duration for `<time dateTime>`. */
function isoDuration(seconds: number): string {
  const total = Number.isFinite(seconds) ? Math.max(0, Math.floor(seconds)) : 0;
  return `PT${total}S`;
}

export interface AiVoiceModeProps extends Omit<HTMLAttributes<HTMLElement>, 'onError'> {
  /**
   * Where the call is. The product owns the microphone, the audio and the
   * connection and reports the state here; the component only shows it.
   */
  state: AiVoiceModeState;
  /**
   * Live audio level, 0 to 1, of whoever is talking (the person while
   * `listening`, the assistant while `speaking`). Drives the central visual
   * through the `--mn-ai-voice-mode-level` custom property; ignored in other
   * states and under reduced motion. Default 0.
   */
  level?: number | undefined;
  /** The assistant: its `name` labels the surface and its captions, `avatar` is its picture. */
  agent?: AiAgentRef | undefined;
  /**
   * Live captions, oldest first. Interim captions (`final: false`) are shown
   * and updated in place; each is announced once, when it becomes final.
   */
  captions?: readonly AiVoiceCaption[] | undefined;
  /** Whether captions show (controlled). */
  captionsVisible?: boolean | undefined;
  /** Whether captions show at first (uncontrolled). Default true: captions are an accessibility need. */
  defaultCaptionsVisible?: boolean | undefined;
  /** Called with the new value when the person turns captions on or off. */
  onCaptionsVisibleChange?: ((visible: boolean) => void) | undefined;
  /**
   * Whether the person's microphone is muted. Default: `state === 'muted'`.
   * Pass it when the microphone can stay muted while the assistant thinks or speaks.
   */
  muted?: boolean | undefined;
  /**
   * Called with the wanted value when the person presses Mute or Space. Shows
   * the Mute toggle while the call is live. Mute the microphone, then report
   * it back through `state` or `muted`.
   */
  onMuteChange?: ((muted: boolean) => void) | undefined;
  /** Shows Interrupt while the assistant is thinking or speaking; stop its reply in the handler. */
  onInterrupt?: (() => void) | undefined;
  /** Shows End while the call is live. Only this button ends the call; Escape does not. */
  onEnd?: (() => void) | undefined;
  /** Shows "Start again" on the `ended` state. */
  onRestart?: (() => void) | undefined;
  /** Shows Retry on the `error` state. */
  onRetry?: (() => void) | undefined;
  /** How long the call has run, in seconds. Shown as a clock, and as the duration once `ended`. */
  elapsedSeconds?: number | undefined;
  /** What went wrong, shown on the `error` state. Default: a general sentence. */
  error?: ReactNode;
  /** `md` (default) stands on its own; `compact` fits inside an assistant panel. */
  size?: AiVoiceModeSize | undefined;
  /** BCP 47 locale for the clock's digits. Default: the runtime's. */
  locale?: string | undefined;
  /** Extra content under the controls, e.g. a microphone picker. */
  children?: ReactNode;
  /** Replaces any of the English strings; `states` may be partial. */
  messages?:
    | (Partial<Omit<AiVoiceModeMessages, 'states'>> & {
        states?: Partial<Record<AiVoiceModeState, string>> | undefined;
      })
    | undefined;
}

const glyph = {
  width: 16,
  height: 16,
  viewBox: '0 0 16 16',
  fill: 'none',
  stroke: 'currentColor',
  strokeWidth: 1.5,
  strokeLinecap: 'round',
  strokeLinejoin: 'round',
  'aria-hidden': true,
  focusable: false,
} as const;

function MicGlyph(props: SVGProps<SVGSVGElement>) {
  return (
    <svg {...glyph} {...props}>
      <rect x="5.75" y="1.75" width="4.5" height="8" rx="2.25" />
      <path d="M3.25 7.5a4.75 4.75 0 0 0 9.5 0M8 12.25v2" />
    </svg>
  );
}

function MicOffGlyph(props: SVGProps<SVGSVGElement>) {
  return (
    <svg {...glyph} {...props}>
      <path d="M10.25 6.5V4a2.25 2.25 0 0 0-4.3-.93M5.75 5.75V7.5a2.25 2.25 0 0 0 3.6 1.8" />
      <path d="M3.25 7.5a4.75 4.75 0 0 0 7.9 3.55M12.75 7.5c0 .5-.08 1-.22 1.44M8 12.25v2M2 2l12 12" />
    </svg>
  );
}

function CaptionsGlyph(props: SVGProps<SVGSVGElement>) {
  return (
    <svg {...glyph} {...props}>
      <rect x="1.75" y="3.25" width="12.5" height="9.5" rx="2" />
      <path d="M7 7a1.5 1.5 0 1 0 0 2M11.5 7a1.5 1.5 0 1 0 0 2" />
    </svg>
  );
}

function StopGlyph(props: SVGProps<SVGSVGElement>) {
  return (
    <svg {...glyph} {...props}>
      <rect x="4" y="4" width="8" height="8" rx="1.5" />
    </svg>
  );
}

/** Controls that already use Space themselves. */
const INTERACTIVE =
  'button, a[href], input, select, textarea, summary, audio, video, [contenteditable]:not([contenteditable="false"]), [role="button"], [role="link"], [role="checkbox"], [role="switch"], [role="tab"], [role="menuitem"], [role="option"], [role="slider"], [role="textbox"]';

/** How close to the end (in CSS pixels) still counts as "at the newest caption". */
const STICK_THRESHOLD = 24;

/**
 * The surface for a live, two-way spoken conversation with an AI assistant: a
 * calm central visual and always-visible text for the state, live captions,
 * and explicit Mute, Interrupt, Captions and End controls. It is UI only. The
 * product owns the microphone, the audio and the connection and reports the
 * state through props; nothing is recorded, sent or ended without a button
 * press.
 */
export const AiVoiceMode = forwardRef<HTMLElement, AiVoiceModeProps>(function AiVoiceMode(
  {
    state,
    level = 0,
    agent,
    captions,
    captionsVisible: captionsVisibleProp,
    defaultCaptionsVisible = true,
    onCaptionsVisibleChange,
    muted: mutedProp,
    onMuteChange,
    onInterrupt,
    onEnd,
    onRestart,
    onRetry,
    elapsedSeconds,
    error,
    size = 'md',
    locale,
    children,
    messages: messagesProp,
    className,
    style,
    onKeyDown,
    ...rest
  },
  ref,
) {
  const { states: _s, ...overrides } = messagesProp ?? {};
  const m = { ...AI_VOICE_MODE_MESSAGES, ...overrides };
  const states = { ...AI_VOICE_MODE_MESSAGES.states, ...messagesProp?.states };

  const [captionsVisible, setCaptionsVisible] = useControllable(
    captionsVisibleProp,
    defaultCaptionsVisible,
    onCaptionsVisibleChange,
  );

  const live = state !== 'ended' && state !== 'error';
  const muted = live && (mutedProp ?? state === 'muted');
  const canInterrupt = Boolean(onInterrupt) && (state === 'thinking' || state === 'speaking');
  const voiced = state === 'listening' || state === 'speaking';
  const busy = state === 'connecting' || state === 'thinking' || state === 'reconnecting';
  const clamped = voiced && Number.isFinite(level) ? Math.min(1, Math.max(0, level)) : 0;

  const agentName = agent?.name || m.assistant;
  const agentRef: AiAgentRef = agent ?? { name: agentName };
  const stateText = states[state];
  const errorText = error ?? m.errorFallback;
  const duration =
    elapsedSeconds !== undefined ? formatAiVoiceDuration(elapsedSeconds, locale) : undefined;
  const lines = captions ?? [];
  const showLog = captionsVisible && (live || lines.length > 0);

  // Announce state changes once each, never the initial state. Flips between
  // listening and speaking stay silent: the audio itself says whose turn it is.
  const previous = useRef<AiVoiceModeState | null>(null);
  const [announcement, setAnnouncement] = useState('');
  useEffect(() => {
    const before = previous.current;
    previous.current = state;
    if (before === null || before === state) return;
    if (state === 'listening' || state === 'speaking') {
      setAnnouncement('');
      return;
    }
    setAnnouncement(
      state === 'error' && typeof errorText === 'string'
        ? m.announceError(stateText, errorText)
        : stateText,
    );
    // Only a state change is news.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [state]);

  // Keep the newest caption in view unless the person scrolled up to read.
  const log = useRef<HTMLDivElement>(null);
  const stuck = useRef(true);
  const last = lines[lines.length - 1];
  const tail = last ? `${lines.length}:${last.id}:${last.text.length}:${last.final}` : '';
  useEffect(() => {
    const el = log.current;
    if (el && stuck.current) el.scrollTop = el.scrollHeight;
  }, [tail, showLog]);

  // Interrupt goes away when the assistant stops. If it had focus, hand focus
  // to Mute rather than dropping it on the page.
  const muteButton = useRef<HTMLButtonElement>(null);
  const interruptFocused = useRef(false);
  useEffect(() => {
    if (canInterrupt || !interruptFocused.current) return;
    interruptFocused.current = false;
    muteButton.current?.focus();
  }, [canInterrupt]);

  const canMute = live && Boolean(onMuteChange);
  const handleKeyDown = (event: ReactKeyboardEvent<HTMLElement>) => {
    onKeyDown?.(event);
    if (event.defaultPrevented || !canMute) return;
    if (event.key !== ' ' || event.altKey || event.ctrlKey || event.metaKey || event.shiftKey)
      return;
    const target = event.target as Element | null;
    if (target?.closest?.(INTERACTIVE)) return;
    // Space would otherwise scroll the page (or the captions) under the call.
    event.preventDefault();
    if (!event.repeat) onMuteChange?.(!muted);
  };

  const buttonSize = size === 'compact' ? 'sm' : 'md';
  const vars = { ['--mn-ai-voice-mode-level' as string]: String(clamped) } as CSSProperties;

  return (
    // Space toggles mute from anywhere inside the surface that is not a control.
    // eslint-disable-next-line jsx-a11y/no-noninteractive-element-interactions
    <section
      ref={ref}
      // A named group, not a region: a page may show several, and landmarks must be unique.
      role="group"
      aria-label={m.label(agentName)}
      tabIndex={-1}
      {...rest}
      className={cx('mn-ai-voice-mode', className)}
      data-state={state}
      data-size={size}
      data-muted={muted || undefined}
      data-captions={captionsVisible ? 'on' : 'off'}
      aria-busy={busy || undefined}
      style={{ ...style, ...vars }}
      onKeyDown={handleKeyDown}
    >
      <div className="mn-ai-voice-mode__stage">
        <div className="mn-ai-voice-mode__visual" aria-hidden="true">
          <span className="mn-ai-voice-mode__halo" />
          <span className="mn-ai-voice-mode__orb">
            {state === 'error' ? (
              <ErrorGlyph className="mn-ai-voice-mode__glyph" />
            ) : state === 'muted' ? (
              <MicOffGlyph className="mn-ai-voice-mode__glyph" />
            ) : voiced ? (
              <span className="mn-ai-voice-mode__bars">
                <span className="mn-ai-voice-mode__bar" />
                <span className="mn-ai-voice-mode__bar" />
                <span className="mn-ai-voice-mode__bar" />
                <span className="mn-ai-voice-mode__bar" />
                <span className="mn-ai-voice-mode__bar" />
              </span>
            ) : busy ? (
              <span className="mn-ai-voice-mode__dots">
                <span className="mn-ai-voice-mode__dot" />
                <span className="mn-ai-voice-mode__dot" />
                <span className="mn-ai-voice-mode__dot" />
              </span>
            ) : (
              <span className="mn-ai-voice-mode__rest" />
            )}
          </span>
        </div>

        <div className="mn-ai-voice-mode__summary">
          <div className="mn-ai-voice-mode__agent">
            <AiAgentAvatar
              agent={agentRef}
              size={size === 'compact' ? 'xs' : 'sm'}
              decorative
              className="mn-ai-voice-mode__avatar"
            />
            <span className="mn-ai-voice-mode__name">{agentName}</span>
            {live && duration !== undefined ? (
              <span className="mn-ai-voice-mode__elapsed">
                <span className="mn-visually-hidden">{m.elapsed}: </span>
                <time dateTime={isoDuration(elapsedSeconds ?? 0)}>{duration}</time>
              </span>
            ) : null}
          </div>
          <p className="mn-ai-voice-mode__state">
            <span className="mn-ai-voice-mode__state-text">{stateText}</span>
            {muted && state !== 'muted' ? (
              <span className="mn-ai-voice-mode__muted-note">
                <MicOffGlyph />
                {m.microphoneMuted}
              </span>
            ) : null}
          </p>
          {state === 'error' ? <p className="mn-ai-voice-mode__error">{errorText}</p> : null}
          {state === 'ended' && duration !== undefined ? (
            <p className="mn-ai-voice-mode__duration">
              <time dateTime={isoDuration(elapsedSeconds ?? 0)}>{m.lasted(duration)}</time>
            </p>
          ) : null}
        </div>
      </div>

      {showLog ? (
        <div
          ref={log}
          className="mn-ai-voice-mode__captions"
          role="log"
          aria-live="polite"
          aria-relevant="additions"
          aria-label={m.captionsLabel}
          // A scrollable region must be reachable by keyboard.
          // eslint-disable-next-line jsx-a11y/no-noninteractive-tabindex
          tabIndex={0}
          onScroll={(event) => {
            const el = event.currentTarget;
            stuck.current = el.scrollHeight - el.scrollTop - el.clientHeight <= STICK_THRESHOLD;
          }}
        >
          {lines.length === 0 ? (
            <p className="mn-ai-voice-mode__captions-empty">{m.captionsEmpty}</p>
          ) : (
            lines.map((line) => (
              // An interim caption is hidden from assistive technology and
              // updated in place. When it becomes final the key changes, so a
              // new node is added and the log announces it exactly once.
              <p
                key={line.final ? line.id : `${line.id}:interim`}
                className="mn-ai-voice-mode__caption"
                data-speaker={line.speaker}
                data-final={line.final ? 'true' : 'false'}
                aria-hidden={line.final ? undefined : true}
              >
                <span className="mn-ai-voice-mode__speaker">
                  {line.speaker === 'user' ? m.you : agentName}
                </span>{' '}
                <span className="mn-ai-voice-mode__caption-text" dir="auto">
                  {line.text}
                </span>
              </p>
            ))
          )}
        </div>
      ) : null}

      {live ? (
        <div className="mn-ai-voice-mode__controls">
          {onMuteChange ? (
            <Button
              ref={muteButton}
              size={buttonSize}
              variant="secondary"
              className="mn-ai-voice-mode__mute"
              aria-pressed={muted}
              data-pressed={muted || undefined}
              leadingIcon={muted ? <MicOffGlyph /> : <MicGlyph />}
              onClick={() => onMuteChange(!muted)}
            >
              {m.mute}
            </Button>
          ) : null}
          {canInterrupt ? (
            <Button
              size={buttonSize}
              variant="secondary"
              className="mn-ai-voice-mode__interrupt"
              leadingIcon={<StopGlyph />}
              onFocus={() => {
                interruptFocused.current = true;
              }}
              onBlur={() => {
                interruptFocused.current = false;
              }}
              onClick={onInterrupt}
            >
              {m.interrupt}
            </Button>
          ) : null}
          <Button
            size={buttonSize}
            variant="ghost"
            className="mn-ai-voice-mode__captions-toggle"
            aria-pressed={captionsVisible}
            data-pressed={captionsVisible || undefined}
            leadingIcon={<CaptionsGlyph />}
            onClick={() => setCaptionsVisible(!captionsVisible)}
          >
            {m.captions}
          </Button>
          {onEnd ? (
            <Button
              size={buttonSize}
              variant="danger"
              className="mn-ai-voice-mode__end"
              leadingIcon={<CloseGlyph />}
              onClick={onEnd}
            >
              {m.end}
            </Button>
          ) : null}
        </div>
      ) : (state === 'ended' && onRestart) || (state === 'error' && onRetry) ? (
        <div className="mn-ai-voice-mode__controls">
          <Button
            size={buttonSize}
            variant="secondary"
            leadingIcon={<RetryGlyph />}
            onClick={state === 'ended' ? onRestart : onRetry}
          >
            {state === 'ended' ? m.restart : m.retry}
          </Button>
        </div>
      ) : null}

      {children}

      <span
        className="mn-visually-hidden mn-ai-voice-mode__live"
        role="status"
        aria-live="polite"
        aria-atomic="true"
      >
        {announcement}
      </span>
    </section>
  );
});
