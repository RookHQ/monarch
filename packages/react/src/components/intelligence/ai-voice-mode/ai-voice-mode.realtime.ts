/**
 * A realtime voice session over WebRTC, for any service: the caller supplies
 * the signalling (offer in, answer out) and the meaning of the data-channel
 * messages; this file owns the microphone, the peer connection, playback,
 * levels, barge-in, reconnection and cleanup. No vendor SDK.
 */
import type {
  AiVoiceErrorCode,
  AiVoiceSession,
  AiVoiceSessionEvent,
} from './ai-voice-mode.session.js';
import {
  createVoiceActivityDetector,
  measureAudioLevel,
  type VoiceActivityDetectorOptions,
} from './ai-voice-mode.audio.js';

type VoiceState = AiVoiceSession['state'];

/** What may be sent on the data channel. */
export type RealtimeVoiceMessage = string | ArrayBuffer;

/** How a dropped connection is restored. */
export interface RealtimeVoiceReconnectOptions {
  /** Attempts before the session fails with `network`. Default 5. */
  maxAttempts?: number | undefined;
  /** Wait before the first attempt; it doubles each time (milliseconds). Default 500. */
  baseDelayMs?: number | undefined;
  /** Longest wait between attempts (milliseconds). Default 8000. */
  maxDelayMs?: number | undefined;
  /** How long a `disconnected` connection may recover by itself before it is rebuilt (milliseconds). Default 3000. */
  graceMs?: number | undefined;
  /** Spread of each wait, 0 to 1, so many clients do not retry together. Default 0.2. */
  jitter?: number | undefined;
}

/** The part of `AudioContext` the session uses for levels. */
export type AiRealtimeAudioContextLike = Pick<
  AudioContext,
  'createMediaStreamSource' | 'createAnalyser'
> & {
  readonly state?: AudioContextState;
  resume?: () => Promise<void>;
  close?: () => Promise<void>;
};

export interface RealtimeVoiceSessionOptions {
  /**
   * The signalling: send the offer to your service and return its answer.
   * Abort the request when `signal` aborts. Called again for every reconnect.
   */
  connect: (
    offer: RTCSessionDescriptionInit,
    signal: AbortSignal,
  ) => Promise<RTCSessionDescriptionInit>;
  /** STUN and TURN servers. Default: the browser's. */
  iceServers?: RTCIceServer[] | undefined;
  /** Microphone constraints for `getUserMedia`. Default `true` (the browser's defaults, with echo cancellation). */
  audio?: MediaTrackConstraints | boolean | undefined;
  /**
   * Turns one data-channel message into session events: transcripts, the
   * assistant's state (`thinking`, `speaking`, `listening`), levels, errors.
   * Text messages that are JSON arrive parsed. Return null to ignore one.
   * Without it, `speaking` and `listening` follow the remote audio's loudness.
   */
  mapEvent?:
    | ((raw: unknown) => AiVoiceSessionEvent | readonly AiVoiceSessionEvent[] | null | undefined)
    | undefined;
  /** The message that tells the service to stop its reply; sent on `interrupt` (barge-in). */
  encodeInterrupt?: (() => RealtimeVoiceMessage | null | undefined) | undefined;
  /** The message that asks the service to say `text`; makes `speak` work. */
  encodeText?: ((text: string) => RealtimeVoiceMessage | null | undefined) | undefined;
  /** Label of the data channel. Default `events`. */
  dataChannelLabel?: string | undefined;
  /**
   * Barge-in: when the person starts talking while the assistant speaks, the
   * session interrupts it. Default true. Use headphones or echo cancellation,
   * or the assistant can interrupt itself.
   */
  bargeIn?: boolean | undefined;
  /** Tunes the detector that decides the person started talking. */
  voiceActivity?: VoiceActivityDetectorOptions | undefined;
  /** Restores a dropped connection with backoff. `false` fails at once. */
  reconnect?: RealtimeVoiceReconnectOptions | false | undefined;
  /** Wait for ICE candidates before sending the offer, for services without trickle ICE. Default false. */
  waitForIceGathering?: boolean | undefined;
  /** How often levels are measured and reported (milliseconds). Default 80. */
  levelIntervalMs?: number | undefined;
  /** Scales measured loudness to the 0 to 1 meter. Default 3. */
  levelGain?: number | undefined;
  /** How long playback stays silent after an interrupt when the service does not say it stopped (milliseconds). Default 1000. */
  interruptHoldMs?: number | undefined;
  /** Ends the session when aborted. */
  signal?: AbortSignal | undefined;
  /** For tests and other environments: makes the peer connection. Default: `RTCPeerConnection`. */
  createPeerConnection?: ((configuration: RTCConfiguration) => RTCPeerConnection) | undefined;
  /** For tests and other environments: opens the microphone. Default: `navigator.mediaDevices.getUserMedia`. */
  getUserMedia?: ((constraints: MediaStreamConstraints) => Promise<MediaStream>) | undefined;
  /** For tests and other environments: makes the audio context used for levels. Default: `AudioContext`. */
  createAudioContext?: (() => AiRealtimeAudioContextLike) | undefined;
  /** For tests and other environments: makes the element that plays the assistant. Default: `new Audio()`. */
  createAudioElement?: (() => HTMLAudioElement) | undefined;
  /** For tests: a number from 0 to 1 used for jitter. Default `Math.random`. */
  random?: (() => number) | undefined;
}

/** A realtime session: `AiVoiceSession` plus a way to send your own messages. */
export interface RealtimeVoiceSession extends AiVoiceSession {
  /** Sends a message on the data channel. False when the channel is not open. */
  send(message: RealtimeVoiceMessage): boolean;
}

/** True when the browser has WebRTC and a microphone API. */
export function isRealtimeVoiceSupported(): boolean {
  return (
    typeof RTCPeerConnection !== 'undefined' &&
    typeof navigator !== 'undefined' &&
    typeof navigator.mediaDevices?.getUserMedia === 'function'
  );
}

const MEDIA_ERRORS: Record<string, AiVoiceErrorCode> = {
  NotAllowedError: 'not-allowed',
  SecurityError: 'not-allowed',
  NotFoundError: 'not-allowed',
  NotReadableError: 'not-allowed',
  OverconstrainedError: 'not-allowed',
};

function parseMessage(data: unknown): unknown {
  if (typeof data !== 'string') return data;
  try {
    return JSON.parse(data) as unknown;
  } catch {
    return data;
  }
}

function defaultAudioContext(): AiRealtimeAudioContextLike | null {
  if (typeof window === 'undefined') return null;
  const Ctor =
    window.AudioContext ??
    (window as unknown as { webkitAudioContext?: typeof AudioContext }).webkitAudioContext;
  return Ctor ? new Ctor() : null;
}

interface Meter {
  analyser: AnalyserNode;
  source: MediaStreamAudioSourceNode;
  buffer: Float32Array<ArrayBuffer>;
}

/**
 * A voice session over WebRTC. It opens the microphone, makes a peer
 * connection with a data channel, hands the offer to `connect` and plays the
 * remote audio. Data-channel messages become transcripts and states through
 * `mapEvent`. Levels come from analysers on both directions, the microphone
 * can be muted, `interrupt` silences the assistant and sends
 * `encodeInterrupt`, a dropped connection is rebuilt with backoff (state
 * `reconnecting`), and `stop` releases the microphone, the connection and the
 * audio context. Microphone audio leaves the device: say so before `start`.
 */
export function createRealtimeVoiceSession(
  options: RealtimeVoiceSessionOptions,
): RealtimeVoiceSession {
  const listeners = new Set<(event: AiVoiceSessionEvent) => void>();
  const emit = (event: AiVoiceSessionEvent) => listeners.forEach((l) => l(event));
  const bargeIn = options.bargeIn ?? true;
  const levelInterval = Math.max(16, options.levelIntervalMs ?? 80);
  const gain = options.levelGain ?? 3;
  const holdMs = Math.max(0, options.interruptHoldMs ?? 1000);
  const retry = options.reconnect === false ? null : (options.reconnect ?? {});
  const random = options.random ?? Math.random;
  /** Without a mapper nothing says who is talking, so the remote loudness does. */
  const autoState = !options.mapEvent;

  let state: VoiceState = 'ended';
  let live = false;
  let muted = false;
  let run: AbortController | null = null;
  let mic: MediaStream | null = null;
  let peer: RTCPeerConnection | null = null;
  let channel: RTCDataChannel | null = null;
  let audio: HTMLAudioElement | null = null;
  let context: AiRealtimeAudioContextLike | null = null;
  let micMeter: Meter | null = null;
  let remoteMeter: Meter | null = null;
  let connected = false;
  let attempts = 0;
  let lastLevel = -1;
  let levelTimer: ReturnType<typeof setInterval> | undefined;
  let retryTimer: ReturnType<typeof setTimeout> | undefined;
  let graceTimer: ReturnType<typeof setTimeout> | undefined;
  let holdTimer: ReturnType<typeof setTimeout> | undefined;
  const inputVad = createVoiceActivityDetector(options.voiceActivity);
  const outputVad = createVoiceActivityDetector({ attackMs: 60, releaseMs: 700 });

  const setState = (next: VoiceState) => {
    if (state === next) return;
    state = next;
    emit({ type: 'state', state: next });
  };
  const resting = (): VoiceState => (muted ? 'muted' : 'listening');

  // ---------------------------------------------------------- audio
  const meter = (stream: MediaStream): Meter | null => {
    if (!context) return null;
    try {
      const source = context.createMediaStreamSource(stream);
      const analyser = context.createAnalyser();
      analyser.fftSize = 1024;
      source.connect(analyser);
      return { analyser, source, buffer: new Float32Array(analyser.fftSize) };
    } catch {
      return null;
    }
  };
  const dropMeter = (m: Meter | null) => {
    try {
      m?.source.disconnect();
    } catch {
      // Already disconnected.
    }
  };
  const read = (m: Meter | null): number => {
    if (!m) return 0;
    m.analyser.getFloatTimeDomainData(m.buffer);
    return measureAudioLevel(m.buffer);
  };

  const releaseHold = () => {
    clearTimeout(holdTimer);
    holdTimer = undefined;
    if (audio) audio.muted = false;
  };
  const holdPlayback = () => {
    if (!audio) return;
    audio.muted = true;
    clearTimeout(holdTimer);
    holdTimer = setTimeout(releaseHold, holdMs);
  };

  const attachRemote = (stream: MediaStream) => {
    audio ??= options.createAudioElement
      ? options.createAudioElement()
      : typeof Audio !== 'undefined'
        ? new Audio()
        : null;
    if (audio) {
      audio.autoplay = true;
      audio.srcObject = stream;
      void audio.play()?.catch(() => undefined);
    }
    dropMeter(remoteMeter);
    remoteMeter = meter(stream);
  };

  const tick = () => {
    if (!live) return;
    const now = Date.now();
    const input = muted ? 0 : read(micMeter);
    const output = read(remoteMeter);
    if (!muted) {
      const was = inputVad.speaking;
      const talking = inputVad.process(input, now);
      if (bargeIn && talking && !was && state === 'speaking') interrupt();
    }
    if (autoState && connected && !holdTimer) {
      const speaking = outputVad.process(output, now);
      if (speaking && (state === 'listening' || state === 'muted')) setState('speaking');
      else if (!speaking && state === 'speaking') setState(resting());
    }
    const raw = state === 'speaking' ? output : state === 'listening' ? input : 0;
    const level = Math.min(1, Math.max(0, raw * gain));
    if (level === lastLevel) return;
    lastLevel = level;
    emit({ type: 'level', level, source: state === 'speaking' ? 'output' : 'input' });
  };

  // ---------------------------------------------------------- connection
  const closePeer = () => {
    clearTimeout(graceTimer);
    graceTimer = undefined;
    const pc = peer;
    const dc = channel;
    peer = null;
    channel = null;
    connected = false;
    if (dc) {
      dc.onmessage = null;
      try {
        dc.close();
      } catch {
        // Already closed.
      }
    }
    if (pc) {
      pc.onconnectionstatechange = null;
      pc.ontrack = null;
      try {
        pc.close();
      } catch {
        // Already closed.
      }
    }
  };

  const teardown = () => {
    run?.abort();
    run = null;
    clearInterval(levelTimer);
    clearTimeout(retryTimer);
    clearTimeout(holdTimer);
    levelTimer = retryTimer = holdTimer = undefined;
    closePeer();
    mic?.getTracks().forEach((track) => track.stop());
    mic = null;
    if (audio) {
      audio.pause();
      audio.srcObject = null;
      audio.muted = false;
      audio = null;
    }
    dropMeter(micMeter);
    dropMeter(remoteMeter);
    micMeter = remoteMeter = null;
    void context?.close?.().catch(() => undefined);
    context = null;
    inputVad.reset();
    outputVad.reset();
    attempts = 0;
    lastLevel = -1;
  };

  const fail = (code: AiVoiceErrorCode) => {
    if (!live) return;
    live = false;
    teardown();
    setState('error');
    emit({ type: 'error', code });
  };

  const handle = (event: AiVoiceSessionEvent) => {
    switch (event.type) {
      case 'state':
        if (event.state === 'ended') session.stop();
        else if (event.state === 'error') fail('unknown');
        else {
          if (event.state === 'speaking') releaseHold();
          setState(event.state === 'listening' ? resting() : event.state);
        }
        break;
      case 'muted':
        session.setMuted(event.muted);
        break;
      case 'error':
        fail(event.code);
        break;
      default:
        emit(event);
    }
  };

  const onMessage = (data: unknown) => {
    if (!live || !options.mapEvent) return;
    let mapped: ReturnType<NonNullable<RealtimeVoiceSessionOptions['mapEvent']>>;
    try {
      mapped = options.mapEvent(parseMessage(data));
    } catch {
      return;
    }
    if (!mapped) return;
    const events: readonly AiVoiceSessionEvent[] = Array.isArray(mapped)
      ? (mapped as readonly AiVoiceSessionEvent[])
      : [mapped as AiVoiceSessionEvent];
    for (const event of events) {
      if (!live) return;
      handle(event);
    }
  };

  const gathered = (pc: RTCPeerConnection, signal: AbortSignal) =>
    new Promise<void>((resolve) => {
      if (pc.iceGatheringState === 'complete') return resolve();
      const done = () => {
        clearTimeout(timer);
        pc.removeEventListener('icegatheringstatechange', check);
        signal.removeEventListener('abort', done);
        resolve();
      };
      const check = () => {
        if (pc.iceGatheringState === 'complete') done();
      };
      const timer = setTimeout(done, 3000);
      pc.addEventListener('icegatheringstatechange', check);
      signal.addEventListener('abort', done);
    });

  const scheduleReconnect = (owner: AbortController) => {
    if (!live || run !== owner) return;
    if (!retry || !mic) return fail('network');
    const max = retry.maxAttempts ?? 5;
    if (attempts >= max) return fail('network');
    closePeer();
    setState('reconnecting');
    const base = retry.baseDelayMs ?? 500;
    const cap = retry.maxDelayMs ?? 8000;
    const jitter = Math.min(1, Math.max(0, retry.jitter ?? 0.2));
    const delay = Math.min(cap, base * 2 ** attempts) * (1 + jitter * (random() * 2 - 1));
    attempts += 1;
    clearTimeout(retryTimer);
    retryTimer = setTimeout(
      () => {
        retryTimer = undefined;
        connectPeer(owner).catch(() => {
          if (!owner.signal.aborted) scheduleReconnect(owner);
        });
      },
      Math.max(0, delay),
    );
  };

  const connectPeer = async (owner: AbortController): Promise<void> => {
    const stream = mic;
    if (!stream) return;
    const pc = options.createPeerConnection
      ? options.createPeerConnection({
          ...(options.iceServers ? { iceServers: options.iceServers } : {}),
        })
      : new RTCPeerConnection({
          ...(options.iceServers ? { iceServers: options.iceServers } : {}),
        });
    peer = pc;
    const mine = () => peer === pc && !owner.signal.aborted;
    stream.getTracks().forEach((track) => pc.addTrack(track, stream));
    pc.ontrack = (event) => {
      if (!mine()) return;
      const remote =
        event.streams[0] ??
        (typeof MediaStream !== 'undefined' ? new MediaStream([event.track]) : null);
      if (remote) attachRemote(remote);
    };
    const dc = pc.createDataChannel(options.dataChannelLabel ?? 'events');
    channel = dc;
    dc.onmessage = (event) => {
      if (mine()) onMessage(event.data);
    };
    pc.onconnectionstatechange = () => {
      if (!mine()) return;
      switch (pc.connectionState) {
        case 'connected':
          clearTimeout(graceTimer);
          graceTimer = undefined;
          connected = true;
          attempts = 0;
          if (state === 'connecting' || state === 'reconnecting') setState(resting());
          break;
        case 'disconnected':
          // Often recovers by itself; rebuild only if it does not.
          if (!connected) break;
          setState('reconnecting');
          clearTimeout(graceTimer);
          graceTimer = setTimeout(() => {
            graceTimer = undefined;
            if (mine() && pc.connectionState !== 'connected') scheduleReconnect(owner);
          }, retry?.graceMs ?? 3000);
          break;
        case 'failed':
          if (connected || attempts > 0) scheduleReconnect(owner);
          else fail('network');
          break;
        default:
          break;
      }
    };
    const offer = await pc.createOffer();
    if (!mine()) return;
    await pc.setLocalDescription(offer);
    if (options.waitForIceGathering) await gathered(pc, owner.signal);
    if (!mine()) return;
    const local = pc.localDescription;
    const answer = await options.connect(
      local?.sdp ? { type: local.type, sdp: local.sdp } : offer,
      owner.signal,
    );
    if (!mine()) return;
    await pc.setRemoteDescription(answer);
  };

  const open = async (owner: AbortController) => {
    const getUserMedia =
      options.getUserMedia ??
      (typeof navigator !== 'undefined' && navigator.mediaDevices?.getUserMedia
        ? (constraints: MediaStreamConstraints) => navigator.mediaDevices.getUserMedia(constraints)
        : null);
    if (
      !getUserMedia ||
      (!options.createPeerConnection && typeof RTCPeerConnection === 'undefined')
    )
      return fail('not-supported');
    let stream: MediaStream;
    try {
      stream = await getUserMedia({ audio: options.audio ?? true });
    } catch (e) {
      if (owner.signal.aborted) return;
      const name = e instanceof Error ? e.name : '';
      return fail(MEDIA_ERRORS[name] ?? 'unknown');
    }
    if (owner.signal.aborted || run !== owner) {
      stream.getTracks().forEach((track) => track.stop());
      return;
    }
    mic = stream;
    stream.getAudioTracks().forEach((track) => {
      track.enabled = !muted;
    });
    try {
      context = options.createAudioContext ? options.createAudioContext() : defaultAudioContext();
      if (context?.state === 'suspended') void context.resume?.().catch(() => undefined);
    } catch {
      context = null;
    }
    micMeter = meter(stream);
    levelTimer = setInterval(tick, levelInterval);
    try {
      await connectPeer(owner);
    } catch {
      if (!owner.signal.aborted) fail('network');
    }
  };

  function interrupt() {
    if (!live) return;
    holdPlayback();
    const message = options.encodeInterrupt?.();
    if (message !== null && message !== undefined) session.send(message);
    outputVad.reset();
    if (state === 'speaking' || state === 'thinking') setState(resting());
  }

  const session: RealtimeVoiceSession = {
    get state() {
      return state;
    },
    start() {
      if (live) return;
      teardown();
      if (options.signal?.aborted) return;
      live = true;
      const owner = new AbortController();
      run = owner;
      setState('connecting');
      void open(owner);
    },
    stop() {
      const was = live;
      live = false;
      teardown();
      if (was || state !== 'ended') setState('ended');
    },
    interrupt,
    setMuted(next) {
      muted = next;
      mic?.getAudioTracks().forEach((track) => {
        track.enabled = !next;
      });
      emit({ type: 'muted', muted: next });
      if (next) inputVad.reset();
      if (!live) return;
      if (next && state === 'listening') setState('muted');
      else if (!next && state === 'muted') setState('listening');
    },
    speak(text) {
      if (!live || !text.trim()) return;
      const message = options.encodeText?.(text);
      if (message !== null && message !== undefined) session.send(message);
    },
    send(message) {
      const dc = channel;
      if (!dc || dc.readyState !== 'open') return false;
      try {
        if (typeof message === 'string') dc.send(message);
        else dc.send(message);
        return true;
      } catch {
        return false;
      }
    },
    subscribe(listener) {
      listeners.add(listener);
      return () => listeners.delete(listener);
    },
  };

  options.signal?.addEventListener('abort', () => session.stop(), { once: true });

  return session;
}
