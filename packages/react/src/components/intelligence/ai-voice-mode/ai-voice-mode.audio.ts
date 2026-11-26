/**
 * Audio helpers for voice sessions, with no vendor SDK: a level measure, an
 * energy-based voice-activity detector (used for barge-in) and a gapless
 * player for streamed PCM audio. Nothing here opens a microphone or a
 * connection; `createRealtimeVoiceSession` does.
 */

// ------------------------------------------------------------ level

/**
 * Loudness of one block of samples as a number from 0 to 1 (root mean
 * square). Takes float samples (-1 to 1, from `getFloatTimeDomainData`),
 * 16-bit PCM, or unsigned bytes centred on 128 (from `getByteTimeDomainData`).
 */
export function measureAudioLevel(
  samples: Float32Array | Int16Array | Uint8Array | readonly number[],
): number {
  const n = samples.length;
  if (n === 0) return 0;
  const bytes = samples instanceof Uint8Array;
  const pcm16 = samples instanceof Int16Array;
  let sum = 0;
  for (let i = 0; i < n; i++) {
    const raw = samples[i] ?? 0;
    const v = bytes ? (raw - 128) / 128 : pcm16 ? raw / 32768 : raw;
    sum += v * v;
  }
  const rms = Math.sqrt(sum / n);
  return Number.isFinite(rms) ? Math.min(1, rms) : 0;
}

// ------------------------------------------------------------ voice activity

export interface VoiceActivityDetectorOptions {
  /** Lowest level (0 to 1) that can count as speech, whatever the room noise. Default 0.02. */
  threshold?: number | undefined;
  /** Speech must also be this many times louder than the measured room noise. Default 3. */
  ratio?: number | undefined;
  /** The level must stay above the threshold this long before speech starts (milliseconds). Default 120. */
  attackMs?: number | undefined;
  /** The level must stay below the threshold this long before speech ends (milliseconds). Default 500. */
  releaseMs?: number | undefined;
  /** Called once when speech starts. */
  onSpeechStart?: (() => void) | undefined;
  /** Called once when speech ends. */
  onSpeechEnd?: (() => void) | undefined;
}

export interface VoiceActivityDetector {
  /**
   * Feeds one level reading (0 to 1) taken at `nowMs` (any steadily rising
   * clock, e.g. `performance.now()`). Returns whether someone is speaking.
   */
  process(level: number, nowMs: number): boolean;
  /** Whether speech is in progress. */
  readonly speaking: boolean;
  /** The room noise measured so far (0 to 1). */
  readonly noiseFloor: number;
  /** Forgets the speech state and the measured noise. */
  reset(): void;
}

/**
 * A small voice-activity detector that works on loudness alone: speech starts
 * when the level stays above a threshold for `attackMs` and ends when it stays
 * below for `releaseMs`. The threshold follows the room: it is the larger of
 * `threshold` and `ratio` times the measured noise floor. It cannot tell a
 * voice from another loud sound, so use headphones or echo cancellation when
 * it drives barge-in. Pure: it reads no clock and no audio by itself.
 */
export function createVoiceActivityDetector(
  options: VoiceActivityDetectorOptions = {},
): VoiceActivityDetector {
  const threshold = Math.max(0, options.threshold ?? 0.02);
  const ratio = Math.max(1, options.ratio ?? 3);
  const attackMs = Math.max(0, options.attackMs ?? 120);
  const releaseMs = Math.max(0, options.releaseMs ?? 500);
  let speaking = false;
  let noise = 0;
  let seeded = false;
  /** When the level first crossed to the other side of the threshold, or null. */
  let since: number | null = null;

  return {
    process(level, nowMs) {
      const value = Number.isFinite(level) ? Math.min(1, Math.max(0, level)) : 0;
      // The first reading seeds the room noise (never above the fixed
      // threshold, in case it is already speech). After that it falls quickly,
      // rises slowly while quiet and barely at all during a loud stretch, so a
      // steady hum becomes the floor but a sentence does not.
      if (!seeded) {
        noise = Math.min(value, threshold);
        seeded = true;
      }
      const gate = Math.max(threshold, noise * ratio);
      const loud = value >= gate;
      noise += (value - noise) * (value < noise ? 0.5 : loud ? 0.005 : 0.05);
      if (loud === speaking) since = null;
      else {
        since ??= nowMs;
        const held = nowMs - since;
        if (!speaking && held >= attackMs) {
          speaking = true;
          since = null;
          options.onSpeechStart?.();
        } else if (speaking && held >= releaseMs) {
          speaking = false;
          since = null;
          options.onSpeechEnd?.();
        }
      }
      return speaking;
    },
    get speaking() {
      return speaking;
    },
    get noiseFloor() {
      return noise;
    },
    reset() {
      speaking = false;
      noise = 0;
      seeded = false;
      since = null;
    },
  };
}

// ------------------------------------------------------------ PCM

/** 16-bit PCM samples as floats from -1 to 1. */
export function pcm16ToFloat32(samples: Int16Array): Float32Array {
  const out = new Float32Array(samples.length);
  for (let i = 0; i < samples.length; i++) out[i] = (samples[i] ?? 0) / 32768;
  return out;
}

/**
 * Decodes base64 text holding little-endian 16-bit PCM, the form realtime
 * services send audio in. A trailing odd byte is dropped.
 */
export function decodeBase64Pcm16(base64: string): Int16Array {
  const binary = atob(base64);
  const count = binary.length >> 1;
  const out = new Int16Array(count);
  for (let i = 0; i < count; i++) {
    const lo = binary.charCodeAt(i * 2);
    const hi = binary.charCodeAt(i * 2 + 1);
    const value = (hi << 8) | lo;
    out[i] = value >= 0x8000 ? value - 0x10000 : value;
  }
  return out;
}

/** How the samples of a chunk are stored. `pcm16` is little-endian signed 16-bit. */
export type PcmAudioFormat = 'pcm16' | 'float32';

/**
 * One piece of streamed audio. An `Int16Array` is PCM16 and a `Float32Array`
 * float samples; an `ArrayBuffer` or other byte view is read as the player's
 * `format` (PCM16 by default). Several channels are interleaved.
 */
export type PcmAudioChunk = Int16Array | Float32Array | ArrayBuffer | Uint8Array;

/** The part of `AudioContext` the audio helpers use. */
export type AiAudioContextLike = Pick<
  AudioContext,
  'currentTime' | 'destination' | 'createBuffer' | 'createBufferSource'
> & {
  readonly state?: AudioContextState;
  resume?: () => Promise<void>;
  close?: () => Promise<void>;
};

export interface PcmAudioPlayerOptions {
  /** Sample rate of the chunks, in hertz. Default 24000. */
  sampleRate?: number | undefined;
  /** Channels in each chunk, interleaved. Default 1. */
  channels?: number | undefined;
  /** How byte chunks (`ArrayBuffer`, `Uint8Array`) are read. Default `pcm16`. */
  format?: PcmAudioFormat | undefined;
  /** Delay before the first chunk plays, so a late chunk does not click (milliseconds). Default 20. */
  leadMs?: number | undefined;
  /** Called when everything queued has played. Not called by `flush` or `stop`. */
  onDrain?: (() => void) | undefined;
  /** An audio context to play on. The player does not close a context it was given. */
  audioContext?: AiAudioContextLike | undefined;
  /** For tests and other environments: makes the audio context. Default: the browser's. */
  createAudioContext?: (() => AiAudioContextLike) | undefined;
}

export interface PcmAudioPlayer {
  /** Queues a chunk to play right after the ones before it, with no gap. */
  enqueue(chunk: PcmAudioChunk): void;
  /** Stops what is playing and drops what is queued, at once (barge-in). The player stays usable. */
  flush(): void;
  /** `flush`, then releases the audio context the player made. The player cannot be used again. */
  stop(): void;
  /** Whether audio is playing or queued. */
  readonly playing: boolean;
  /** Loudness of the audio playing now, 0 to 1. */
  readonly level: number;
  /** Milliseconds of audio still to play. */
  readonly bufferedMs: number;
}

interface Scheduled {
  source: AudioBufferSourceNode;
  start: number;
  end: number;
  level: number;
}

function defaultAudioContext(): AiAudioContextLike | null {
  if (typeof window === 'undefined') return null;
  const Ctor =
    window.AudioContext ??
    (window as unknown as { webkitAudioContext?: typeof AudioContext }).webkitAudioContext;
  return Ctor ? new Ctor() : null;
}

function toFloat32(chunk: PcmAudioChunk, format: PcmAudioFormat): Float32Array {
  if (chunk instanceof Float32Array) return chunk;
  if (chunk instanceof Int16Array) return pcm16ToFloat32(chunk);
  const bytes = chunk instanceof Uint8Array ? chunk : new Uint8Array(chunk);
  const size = format === 'float32' ? 4 : 2;
  const count = Math.floor(bytes.byteLength / size);
  const view = new DataView(bytes.buffer, bytes.byteOffset, count * size);
  const out = new Float32Array(count);
  for (let i = 0; i < count; i++)
    out[i] =
      format === 'float32' ? view.getFloat32(i * 4, true) : view.getInt16(i * 2, true) / 32768;
  return out;
}

/**
 * Plays audio that arrives in chunks (PCM16 or float samples) through the Web
 * Audio API, each chunk scheduled to start exactly where the one before ends,
 * so there are no gaps or clicks. `flush` silences it at once for barge-in.
 * Where there is no audio context (a server, an old browser) it does nothing.
 */
export function createPcmAudioPlayer(options: PcmAudioPlayerOptions = {}): PcmAudioPlayer {
  const sampleRate = options.sampleRate ?? 24000;
  const channels = Math.max(1, Math.floor(options.channels ?? 1));
  const format = options.format ?? 'pcm16';
  const lead = Math.max(0, options.leadMs ?? 20) / 1000;
  const owned = !options.audioContext;
  let context: AiAudioContextLike | null = options.audioContext ?? null;
  let stopped = false;
  let nextTime = 0;
  let scheduled: Scheduled[] = [];

  const ensure = (): AiAudioContextLike | null => {
    if (stopped) return null;
    context ??= options.createAudioContext ? options.createAudioContext() : defaultAudioContext();
    // Browsers start a context suspended until a click; playing is that moment.
    if (context?.state === 'suspended') void context.resume?.().catch(() => undefined);
    return context;
  };

  const silence = () => {
    const playing = scheduled;
    scheduled = [];
    nextTime = 0;
    for (const item of playing) {
      item.source.onended = null;
      try {
        item.source.stop();
      } catch {
        // Not started yet, or already ended.
      }
      item.source.disconnect();
    }
  };

  return {
    enqueue(chunk) {
      const ctx = ensure();
      if (!ctx) return;
      const samples = toFloat32(chunk, format);
      const frames = Math.floor(samples.length / channels);
      if (frames === 0) return;
      const buffer = ctx.createBuffer(channels, frames, sampleRate);
      for (let c = 0; c < channels; c++) {
        const data = buffer.getChannelData(c);
        if (channels === 1) data.set(samples.subarray(0, frames));
        else for (let i = 0; i < frames; i++) data[i] = samples[i * channels + c] ?? 0;
      }
      const source = ctx.createBufferSource();
      source.buffer = buffer;
      source.connect(ctx.destination);
      const start = Math.max(ctx.currentTime + lead, nextTime);
      const end = start + frames / sampleRate;
      nextTime = end;
      const item: Scheduled = { source, start, end, level: measureAudioLevel(samples) };
      scheduled.push(item);
      source.onended = () => {
        scheduled = scheduled.filter((s) => s !== item);
        source.disconnect();
        if (scheduled.length === 0 && !stopped) {
          nextTime = 0;
          options.onDrain?.();
        }
      };
      source.start(start);
    },
    flush: silence,
    stop() {
      if (stopped) return;
      silence();
      stopped = true;
      if (owned) void context?.close?.().catch(() => undefined);
      context = null;
    },
    get playing() {
      return scheduled.length > 0;
    },
    get level() {
      const now = context?.currentTime ?? 0;
      return scheduled.find((s) => s.start <= now && now < s.end)?.level ?? 0;
    },
    get bufferedMs() {
      if (!context || scheduled.length === 0) return 0;
      return Math.max(0, (nextTime - context.currentTime) * 1000);
    },
  };
}
