'use client';

/**
 * Voice sessions: one interface for any speech back end (a realtime model
 * connection, a WebRTC call, or the browser's own speech services), and a
 * browser implementation with barge-in. No vendor SDK. AiVoiceMode shows a
 * session through `useAiVoiceSession`.
 */
import { useCallback, useEffect, useRef, useState } from 'react';
import type { AiVoiceCaption, AiVoiceModeState, AiVoiceSpeaker } from './ai-voice-mode.js';
import { getSpeechRecognition, type AiSpeechRecognitionLike } from '../ai-voice-input/index.js';

/** Why a voice session failed. */
export type AiVoiceErrorCode =
  'not-supported' | 'not-allowed' | 'network' | 'no-speech' | 'unknown';

/** What a voice session reports. */
export type AiVoiceSessionEvent =
  | { type: 'state'; state: AiVoiceModeState }
  /** Words heard or spoken. Interim (`final: false`) text may still change under the same id. */
  | { type: 'transcript'; id: string; speaker: AiVoiceSpeaker; text: string; final: boolean }
  /**
   * Audio level of whoever is talking, 0 to 1. Sessions that measure real
   * audio say whose it is: `input` (the microphone) or `output` (the assistant).
   */
  | { type: 'level'; level: number; source?: 'input' | 'output' }
  | { type: 'muted'; muted: boolean }
  | { type: 'error'; code: AiVoiceErrorCode };

/**
 * A spoken conversation, whatever carries it. Realtime implementations
 * stream audio both ways; the browser one recognises speech and reads
 * replies aloud with `speak`.
 */
export interface AiVoiceSession {
  /** Where the session is now. */
  readonly state: AiVoiceModeState;
  /** Starts listening. Call it from a click: browsers ask for the microphone. */
  start(): void;
  /** Ends the session. */
  stop(): void;
  /** Stops the assistant talking at once and listens again (barge-in). */
  interrupt(): void;
  setMuted(muted: boolean): void;
  /** Reads a reply aloud (where the session does its own speech output). */
  speak(text: string): void;
  /** Listens to events; returns the unsubscribe function. */
  subscribe(listener: (event: AiVoiceSessionEvent) => void): () => void;
}

// ------------------------------------------------------------ browser

/** Recognition with the optional `speechstart` event, used for barge-in where the browser has it. */
type Recognition = AiSpeechRecognitionLike & { onspeechstart?: (() => void) | null };

/** The part of `speechSynthesis` the session uses. */
export interface AiSpeechSynthesisLike {
  speak(utterance: SpeechSynthesisUtterance): void;
  cancel(): void;
  readonly speaking: boolean;
  getVoices?(): SpeechSynthesisVoice[];
}

export interface BrowserVoiceSessionOptions {
  /** BCP 47 language for recognition and speech. Default: the page's `lang`, else the browser's. */
  lang?: string;
  /** Called with each final thing the person said, e.g. `conversation.send`. */
  onUserTurn?: (text: string) => void;
  /**
   * Barge-in: when the person starts talking while the assistant speaks,
   * its speech stops. Default true. Use headphones, or the assistant can
   * hear itself.
   */
  bargeIn?: boolean;
  /** Voice name from `speechSynthesis.getVoices()`. Default: the browser's for `lang`. */
  voice?: string;
  /** Speaking rate, 0.5 to 2. Default 1. */
  rate?: number;
  /** For tests and other environments: makes the recognition object. */
  createRecognition?: () => AiSpeechRecognitionLike;
  /** For tests and other environments: the speech synthesiser. */
  synthesis?: AiSpeechSynthesisLike;
  /** For tests: makes an utterance. */
  createUtterance?: (text: string) => SpeechSynthesisUtterance;
}

/** True when the browser can recognise speech (and so `createBrowserVoiceSession` can listen). */
export function isBrowserVoiceSupported(): boolean {
  return getSpeechRecognition() !== null;
}

const ERROR_CODES: Record<string, AiVoiceErrorCode> = {
  'not-allowed': 'not-allowed',
  'service-not-allowed': 'not-allowed',
  'audio-capture': 'not-allowed',
  network: 'network',
  'no-speech': 'no-speech',
};

/**
 * A voice session on the browser's speech services: speech recognition for
 * the person, `speechSynthesis` for the assistant, and barge-in (talking
 * over the assistant stops it). Recognition in some browsers sends audio to
 * a cloud service run by the browser maker; say so before starting.
 */
export function createBrowserVoiceSession(
  options: BrowserVoiceSessionOptions = {},
): AiVoiceSession {
  const listeners = new Set<(event: AiVoiceSessionEvent) => void>();
  const emit = (event: AiVoiceSessionEvent) => listeners.forEach((l) => l(event));
  let state: AiVoiceModeState = 'ended';
  let muted = false;
  let recognition: Recognition | null = null;
  let live = false;
  let turn = 0;
  let replies = 0;
  const bargeIn = options.bargeIn ?? true;
  const pageLang = typeof document !== 'undefined' ? document.documentElement.lang : '';
  const lang =
    options.lang ||
    pageLang ||
    (typeof navigator !== 'undefined' ? navigator.language : '') ||
    'en';
  const synthesis: AiSpeechSynthesisLike | null =
    options.synthesis ??
    (typeof window !== 'undefined' && 'speechSynthesis' in window ? window.speechSynthesis : null);

  const setState = (next: AiVoiceModeState) => {
    if (state === next) return;
    state = next;
    emit({ type: 'state', state: next });
  };

  const listen = () => {
    if (!live || muted || !recognition) return;
    try {
      recognition.start();
    } catch {
      // Already started.
    }
  };

  const interrupt = () => {
    if (synthesis?.speaking || state === 'speaking' || state === 'thinking') synthesis?.cancel();
    if (live) setState(muted ? 'muted' : 'listening');
  };

  const make = (): Recognition | null => {
    if (options.createRecognition) return options.createRecognition();
    const Ctor = getSpeechRecognition();
    return Ctor ? new Ctor() : null;
  };

  return {
    get state() {
      return state;
    },
    start() {
      recognition = make();
      if (!recognition) {
        setState('error');
        emit({ type: 'error', code: 'not-supported' });
        return;
      }
      live = true;
      setState('connecting');
      recognition.lang = lang;
      recognition.continuous = true;
      recognition.interimResults = true;
      recognition.onspeechstart = () => {
        if (bargeIn && state === 'speaking') interrupt();
      };
      recognition.onresult = (event) => {
        for (let i = event.resultIndex; i < event.results.length; i++) {
          const result = event.results[i];
          if (!result) continue;
          const text = (result[0]?.transcript ?? '').trim();
          if (!text) continue;
          // Talking over the assistant stops it.
          if (bargeIn && state === 'speaking') interrupt();
          if (state !== 'speaking') setState('listening');
          emit({
            type: 'transcript',
            id: `user-${turn}`,
            speaker: 'user',
            text,
            final: result.isFinal,
          });
          if (result.isFinal) {
            turn += 1;
            setState('thinking');
            options.onUserTurn?.(text);
          }
        }
      };
      recognition.onerror = (event) => {
        const code = ERROR_CODES[event.error] ?? (event.error === 'aborted' ? null : 'unknown');
        if (!code || code === 'no-speech') return;
        live = false;
        setState('error');
        emit({ type: 'error', code });
      };
      // Continuous recognition still stops now and then: start it again while live.
      recognition.onend = () => {
        if (live && !muted) listen();
      };
      listen();
      setState('listening');
    },
    stop() {
      live = false;
      synthesis?.cancel();
      recognition?.abort();
      recognition = null;
      setState('ended');
    },
    interrupt,
    setMuted(next) {
      muted = next;
      emit({ type: 'muted', muted: next });
      if (!live) return;
      if (next) {
        recognition?.stop();
        if (state === 'listening') setState('muted');
      } else {
        listen();
        if (state === 'muted') setState('listening');
      }
    },
    speak(text) {
      if (!synthesis || !text.trim()) {
        if (live) setState(muted ? 'muted' : 'listening');
        return;
      }
      const id = `assistant-${++replies}`;
      const utterance = options.createUtterance
        ? options.createUtterance(text)
        : new SpeechSynthesisUtterance(text);
      utterance.lang = lang;
      if (options.rate !== undefined) utterance.rate = Math.min(2, Math.max(0.5, options.rate));
      const voice = options.voice
        ? synthesis.getVoices?.().find((v) => v.name === options.voice)
        : undefined;
      if (voice) utterance.voice = voice;
      utterance.onboundary = () => emit({ type: 'level', level: 0.6 });
      utterance.onend = () => {
        emit({ type: 'level', level: 0 });
        if (live && state === 'speaking') setState(muted ? 'muted' : 'listening');
      };
      emit({ type: 'transcript', id, speaker: 'assistant', text, final: true });
      setState('speaking');
      synthesis.speak(utterance);
    },
    subscribe(listener) {
      listeners.add(listener);
      return () => listeners.delete(listener);
    },
  };
}

// ------------------------------------------------------------ hook

/** AiVoiceMode's props for a session, ready to spread. */
export interface AiVoiceSessionProps {
  state: AiVoiceModeState;
  level: number;
  muted: boolean;
  captions: AiVoiceCaption[];
  onInterrupt: () => void;
  onMuteChange: (muted: boolean) => void;
  onEnd: () => void;
  onRestart: () => void;
}

/** What `useAiVoiceSession` returns. */
export interface AiVoiceSessionView {
  /** Spread on AiVoiceMode: `<AiVoiceMode {...voice.props} />`. */
  props: AiVoiceSessionProps;
  /** Why the session failed, to pick the `error` text you show. */
  errorCode: AiVoiceErrorCode | undefined;
}

/** Keeps the latest captions; interim ones are replaced in place, by id. */
function addCaption(list: AiVoiceCaption[], next: AiVoiceCaption, max: number): AiVoiceCaption[] {
  const i = list.findIndex((c) => c.id === next.id);
  const out = i >= 0 ? list.map((c, j) => (j === i ? next : c)) : [...list, next];
  return out.length > max ? out.slice(out.length - max) : out;
}

/**
 * Follows a voice session for AiVoiceMode: its state, level, captions and
 * mute, plus handlers wired to the session (Interrupt calls
 * `session.interrupt`). Spread its props: `<AiVoiceMode {...voice.props} />`.
 */
export function useAiVoiceSession(
  session: AiVoiceSession,
  options: { maxCaptions?: number } = {},
): AiVoiceSessionView {
  const max = options.maxCaptions ?? 50;
  const [state, setState] = useState<AiVoiceModeState>(session.state);
  const [level, setLevel] = useState(0);
  const [muted, setMuted] = useState(false);
  const [captions, setCaptions] = useState<AiVoiceCaption[]>([]);
  const [error, setError] = useState<AiVoiceErrorCode | undefined>();
  const ref = useRef(session);
  ref.current = session;

  useEffect(() => {
    setState(session.state);
    return session.subscribe((event) => {
      switch (event.type) {
        case 'state':
          setState(event.state);
          if (event.state !== 'error') setError(undefined);
          // The meter belongs to whoever is talking; nobody is in the other states.
          if (event.state !== 'listening' && event.state !== 'speaking') setLevel(0);
          break;
        case 'level':
          // Measured levels arrive many times a second: skip changes too small to see.
          setLevel((before) =>
            event.level === 0 || Math.abs(before - event.level) >= 0.01 ? event.level : before,
          );
          break;
        case 'muted':
          setMuted(event.muted);
          break;
        case 'transcript':
          setCaptions((list) =>
            addCaption(
              list,
              { id: event.id, speaker: event.speaker, text: event.text, final: event.final },
              max,
            ),
          );
          break;
        case 'error':
          setError(event.code);
          break;
      }
    });
  }, [session, max]);

  const onInterrupt = useCallback(() => ref.current.interrupt(), []);
  const onMuteChange = useCallback((next: boolean) => ref.current.setMuted(next), []);
  const onEnd = useCallback(() => ref.current.stop(), []);
  const onRestart = useCallback(() => {
    setCaptions([]);
    ref.current.start();
  }, []);

  return {
    props: { state, level, muted, captions, onInterrupt, onMuteChange, onEnd, onRestart },
    errorCode: error,
  };
}
