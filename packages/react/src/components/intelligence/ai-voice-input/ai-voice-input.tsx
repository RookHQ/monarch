'use client';

import {
  forwardRef,
  useCallback,
  useEffect,
  useId,
  useRef,
  useState,
  type HTMLAttributes,
  type KeyboardEvent as ReactKeyboardEvent,
} from 'react';
import { cx } from '../../../internal/cx.js';
import { Button } from '../../actions/button/index.js';

/** One recognition alternative, as in the Web Speech API. */
export interface AiSpeechAlternative {
  transcript: string;
}

/** One recognition result: alternatives plus whether it is final. */
export interface AiSpeechResult extends ArrayLike<AiSpeechAlternative> {
  isFinal: boolean;
}

/** The subset of the Web Speech API `SpeechRecognition` this component uses. */
export interface AiSpeechRecognitionLike {
  lang: string;
  continuous: boolean;
  interimResults: boolean;
  start(): void;
  stop(): void;
  abort(): void;
  onresult: ((event: { resultIndex: number; results: ArrayLike<AiSpeechResult> }) => void) | null;
  onerror: ((event: { error: string }) => void) | null;
  onend: (() => void) | null;
}

/**
 * The browser's speech recognition constructor (`SpeechRecognition` or the
 * prefixed `webkitSpeechRecognition`), or null when unavailable or on the server.
 */
export function getSpeechRecognition(): (new () => AiSpeechRecognitionLike) | null {
  if (typeof window === 'undefined') return null;
  const w = window as unknown as Record<string, unknown>;
  const ctor = w.SpeechRecognition ?? w.webkitSpeechRecognition;
  return typeof ctor === 'function' ? (ctor as new () => AiSpeechRecognitionLike) : null;
}

/**
 * Where dictation is. `idle`: ready. `starting`: waiting for the microphone
 * (and permission). `listening`: hearing speech. `denied`: microphone access is
 * blocked. `error`: something else went wrong.
 */
export type AiVoiceInputState = 'idle' | 'starting' | 'listening' | 'denied' | 'error';

/** Press to start and press again to stop (`toggle`), or hold to talk (`push-to-talk`). */
export type AiVoiceInputMode = 'toggle' | 'push-to-talk';

export interface AiVoiceInputMessages {
  /** Name of the button in `toggle` mode (with `aria-pressed`). */
  dictate: string;
  /** Name of the button in `push-to-talk` mode. */
  holdToTalk: string;
  /** Status while waiting for the microphone. */
  starting: string;
  /** Status while listening. */
  listening: string;
  /** Announced when listening stops. */
  stopped: string;
  /** Shown when microphone access is blocked, with guidance. */
  denied: string;
  /** Shown when nothing was heard. */
  noSpeech: string;
  /** Shown when no microphone is available. */
  noMicrophone: string;
  /** Shown for other errors. */
  error: string;
  /** Name of the live transcript preview. */
  transcript: string;
  /** Hint read with the button: Escape stops. */
  escapeHint: string;
}

export const AI_VOICE_INPUT_MESSAGES: AiVoiceInputMessages = {
  dictate: 'Dictate',
  holdToTalk: 'Hold to talk',
  starting: 'Starting microphone…',
  listening: 'Listening…',
  stopped: 'Stopped listening.',
  denied: 'Microphone access is blocked. Allow it in your browser’s site settings, then try again.',
  noSpeech: 'Nothing was heard. Try again and speak after the tone.',
  noMicrophone: 'No microphone was found. Connect one and try again.',
  error: 'Dictation stopped unexpectedly. Try again.',
  transcript: 'Transcript',
  escapeHint: 'Press Escape to stop.',
};

export interface AiVoiceInputProps extends HTMLAttributes<HTMLDivElement> {
  /**
   * Called with recognised text. Interim guesses come with `final: false` and
   * may change; each finished phrase comes once with `final: true`. Put final
   * text into the prompt; never send it automatically.
   */
  onTranscript: (text: string, info: { final: boolean }) => void;
  /** BCP 47 language of the speech, e.g. `en-US`. Default: the browser's. */
  lang?: string | undefined;
  /** Default `toggle`. */
  mode?: AiVoiceInputMode | undefined;
  /** Show a live input level while listening (asks for the microphone with getUserMedia). Default true. */
  levelMeter?: boolean | undefined;
  /** Show the interim transcript under the button. Default true. */
  showTranscript?: boolean | undefined;
  /** Disables the button. */
  disabled?: boolean | undefined;
  /** Called whenever the state changes. */
  onStateChange?: ((state: AiVoiceInputState) => void) | undefined;
  /**
   * Creates the recognizer. Default: the browser's Web Speech API. Pass your
   * own (e.g. a server-backed recognizer) to support more browsers. When
   * neither exists the component renders nothing.
   */
  createRecognition?: (() => AiSpeechRecognitionLike | null) | undefined;
  /** Button size. Default `md`. */
  size?: 'sm' | 'md' | undefined;
  /** Strings, for localisation. */
  messages?: Partial<AiVoiceInputMessages> | undefined;
}

function MicGlyph() {
  return (
    <svg
      width={16}
      height={16}
      viewBox="0 0 16 16"
      fill="none"
      stroke="currentColor"
      strokeWidth={1.5}
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden
      focusable={false}
    >
      <rect x="5.75" y="1.75" width="4.5" height="8" rx="2.25" />
      <path d="M3.25 7.5a4.75 4.75 0 0 0 9.5 0M8 12.25v2" />
    </svg>
  );
}

function prefersReducedMotion(): boolean {
  return (
    typeof window !== 'undefined' &&
    typeof window.matchMedia === 'function' &&
    window.matchMedia('(prefers-reduced-motion: reduce)').matches
  );
}

const DENIED_ERRORS = new Set([
  'not-allowed',
  'service-not-allowed',
  'NotAllowedError',
  'SecurityError',
]);

/**
 * A microphone button that dictates into a prompt with the Web Speech API:
 * press to start and again to stop, or hold to talk. Shows a live level
 * meter (static under reduced motion), the words as they are recognised, and
 * clear guidance when microphone access is blocked. It only hands text back
 * through `onTranscript`; it never sends anything. Renders nothing where
 * speech recognition is unavailable.
 */
export const AiVoiceInput = forwardRef<HTMLDivElement, AiVoiceInputProps>(function AiVoiceInput(
  {
    onTranscript,
    lang,
    mode = 'toggle',
    levelMeter = true,
    showTranscript = true,
    disabled = false,
    onStateChange,
    createRecognition,
    size = 'md',
    messages: messagesProp,
    className,
    ...rest
  },
  ref,
) {
  const m = { ...AI_VOICE_INPUT_MESSAGES, ...messagesProp };
  const [supported, setSupported] = useState(false);
  const [state, setStateRaw] = useState<AiVoiceInputState>('idle');
  const [message, setMessage] = useState('');
  const [preview, setPreview] = useState('');
  const [announcement, setAnnouncement] = useState('');
  const [animated, setAnimated] = useState(false);
  const hintId = useId();

  const recognition = useRef<AiSpeechRecognitionLike | null>(null);
  const audio = useRef<{ stream?: MediaStream; ctx?: AudioContext; frame?: number } | null>(null);
  const meter = useRef<HTMLSpanElement>(null);
  const stateRef = useRef(state);
  const callbacks = useRef({ onTranscript, onStateChange });
  callbacks.current = { onTranscript, onStateChange };

  const setState = useCallback((next: AiVoiceInputState) => {
    if (stateRef.current === next) return;
    stateRef.current = next;
    setStateRaw(next);
    callbacks.current.onStateChange?.(next);
  }, []);

  // Feature detection after mount, so the server and first client render agree.
  useEffect(() => {
    setSupported(!!createRecognition || !!getSpeechRecognition());
  }, [createRecognition]);

  const stopMeter = useCallback(() => {
    const a = audio.current;
    audio.current = null;
    if (!a) return;
    if (a.frame !== undefined) cancelAnimationFrame(a.frame);
    a.stream?.getTracks().forEach((t) => t.stop());
    void a.ctx?.close().catch(() => {});
    meter.current?.style.setProperty('--mn-ai-voice-input-level', '0');
  }, []);

  const fail = useCallback(
    (error: string) => {
      const denied = DENIED_ERRORS.has(error);
      setState(denied ? 'denied' : 'error');
      setMessage(
        denied
          ? m.denied
          : error === 'no-speech'
            ? m.noSpeech
            : error === 'audio-capture' || error === 'NotFoundError'
              ? m.noMicrophone
              : m.error,
      );
      recognition.current?.abort();
      recognition.current = null;
      stopMeter();
    },
    [m.denied, m.noSpeech, m.noMicrophone, m.error, setState, stopMeter],
  );

  /** Starts the level meter; returns false when there is none to wait for. */
  const startMeter = useCallback((): boolean => {
    if (!levelMeter || typeof navigator === 'undefined' || !navigator.mediaDevices?.getUserMedia)
      return false;
    const session: { stream?: MediaStream; ctx?: AudioContext; frame?: number } = {};
    audio.current = session;
    navigator.mediaDevices
      .getUserMedia({ audio: true })
      .then((stream) => {
        if (audio.current !== session) {
          stream.getTracks().forEach((t) => t.stop());
          return;
        }
        session.stream = stream;
        setState('listening');
        const reduced = prefersReducedMotion();
        setAnimated(!reduced);
        const Ctx =
          window.AudioContext ??
          (window as unknown as { webkitAudioContext?: typeof AudioContext }).webkitAudioContext;
        if (reduced || !Ctx) return;
        const ctx = new Ctx();
        session.ctx = ctx;
        const analyser = ctx.createAnalyser();
        analyser.fftSize = 256;
        ctx.createMediaStreamSource(stream).connect(analyser);
        const data = new Uint8Array(analyser.fftSize);
        const tick = () => {
          analyser.getByteTimeDomainData(data);
          let sum = 0;
          for (const v of data) sum += ((v - 128) / 128) ** 2;
          const level = Math.min(1, Math.sqrt(sum / data.length) * 4);
          meter.current?.style.setProperty('--mn-ai-voice-input-level', level.toFixed(3));
          session.frame = requestAnimationFrame(tick);
        };
        tick();
      })
      .catch((err: unknown) => {
        if (audio.current !== session) return;
        const name = err instanceof Error ? err.name : '';
        // Only a refusal stops dictation; other meter failures just hide the meter.
        if (DENIED_ERRORS.has(name)) fail(name);
        else {
          audio.current = null;
          setState('listening');
        }
      });
    return true;
  }, [levelMeter, fail, setState]);

  const start = useCallback(() => {
    if (disabled || recognition.current) return;
    const rec = createRecognition
      ? createRecognition()
      : (() => {
          const Ctor = getSpeechRecognition();
          return Ctor ? new Ctor() : null;
        })();
    if (!rec) return;
    recognition.current = rec;
    if (lang) rec.lang = lang;
    rec.continuous = mode === 'toggle';
    rec.interimResults = true;
    setPreview('');
    setMessage('');
    setState('starting');
    let committed = '';
    rec.onresult = (event) => {
      if (recognition.current !== rec) return;
      setState('listening');
      let interim = '';
      for (let i = event.resultIndex; i < event.results.length; i += 1) {
        const result = event.results[i];
        const text = result?.[0]?.transcript ?? '';
        if (!result || !text) continue;
        if (result.isFinal) {
          committed = `${committed}${committed && !/^\s/.test(text) ? ' ' : ''}${text}`.trimStart();
          callbacks.current.onTranscript(text.trim(), { final: true });
        } else interim += text;
      }
      if (interim) callbacks.current.onTranscript(interim.trim(), { final: false });
      setPreview(`${committed}${committed && interim ? ' ' : ''}${interim.trim()}`);
    };
    rec.onerror = (event) => {
      if (recognition.current !== rec || event.error === 'aborted') return;
      fail(event.error);
    };
    rec.onend = () => {
      if (recognition.current !== rec) return;
      recognition.current = null;
      stopMeter();
      if (stateRef.current === 'starting' || stateRef.current === 'listening') {
        setState('idle');
        setAnnouncement(m.stopped);
      }
    };
    try {
      rec.start();
    } catch {
      recognition.current = null;
      fail('start');
      return;
    }
    setAnnouncement(m.listening);
    // With a meter, wait for the microphone stream (and permission) before
    // showing "Listening"; a first result also moves the state on.
    if (!startMeter()) setState('listening');
  }, [
    disabled,
    createRecognition,
    lang,
    mode,
    setState,
    fail,
    stopMeter,
    startMeter,
    m.stopped,
    m.listening,
  ]);

  const stop = useCallback(() => {
    const rec = recognition.current;
    if (!rec) return;
    rec.stop();
  }, []);

  // Clean up on unmount.
  useEffect(
    () => () => {
      recognition.current?.abort();
      recognition.current = null;
      stopMeter();
    },
    [stopMeter],
  );

  if (!supported) return null;

  const active = state === 'starting' || state === 'listening';
  const ptt = mode === 'push-to-talk';
  const onButtonKeyDown = (event: ReactKeyboardEvent<HTMLButtonElement>) => {
    if (event.key === 'Escape' && recognition.current) {
      event.preventDefault();
      event.stopPropagation();
      stop();
      return;
    }
    if (!ptt || event.repeat) return;
    if (event.key === ' ' || event.key === 'Enter') {
      event.preventDefault();
      start();
    }
  };
  const onButtonKeyUp = (event: ReactKeyboardEvent<HTMLButtonElement>) => {
    if (ptt && (event.key === ' ' || event.key === 'Enter')) {
      event.preventDefault();
      stop();
    }
  };
  const statusText = state === 'starting' ? m.starting : state === 'listening' ? m.listening : '';

  return (
    <div
      ref={ref}
      {...rest}
      className={cx('mn-ai-voice-input', className)}
      data-state={state}
      data-mode={mode}
      data-animated={(active && animated) || undefined}
    >
      <div className="mn-ai-voice-input__controls">
        <Button
          type="button"
          size={size}
          variant={active ? 'primary' : 'ghost'}
          className="mn-ai-voice-input__button"
          aria-pressed={active}
          aria-describedby={active ? hintId : undefined}
          disabled={disabled}
          leadingIcon={<MicGlyph />}
          onClick={ptt ? undefined : () => (active ? stop() : start())}
          onPointerDown={
            ptt
              ? (event) => {
                  if (event.button !== 0) return;
                  event.currentTarget.setPointerCapture?.(event.pointerId);
                  start();
                }
              : undefined
          }
          onPointerUp={ptt ? stop : undefined}
          onPointerCancel={ptt ? stop : undefined}
          onKeyDown={onButtonKeyDown}
          onKeyUp={onButtonKeyUp}
        >
          <span className="mn-ai-voice-input__label">{ptt ? m.holdToTalk : m.dictate}</span>
        </Button>
        {active ? (
          <span ref={meter} className="mn-ai-voice-input__meter" aria-hidden="true">
            <span className="mn-ai-voice-input__bar" />
            <span className="mn-ai-voice-input__bar" />
            <span className="mn-ai-voice-input__bar" />
            <span className="mn-ai-voice-input__bar" />
            <span className="mn-ai-voice-input__bar" />
          </span>
        ) : null}
        {statusText ? <span className="mn-ai-voice-input__status">{statusText}</span> : null}
        <span id={hintId} className="mn-visually-hidden">
          {m.escapeHint}
        </span>
      </div>
      {showTranscript && preview ? (
        <p className="mn-ai-voice-input__transcript">
          <span className="mn-visually-hidden">{m.transcript}: </span>
          {preview}
        </p>
      ) : null}
      {message ? (
        <p
          className="mn-ai-voice-input__message"
          role={state === 'denied' ? 'alert' : 'status'}
          data-kind={state}
        >
          {message}
        </p>
      ) : null}
      <span className="mn-visually-hidden mn-ai-voice-input__live" role="status" aria-live="polite">
        {announcement}
      </span>
    </div>
  );
});
