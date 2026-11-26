import type { AiSpeechRecognitionLike, AiSpeechResult } from './ai-voice-input.js';

export interface RecordingRecognitionOptions {
  /**
   * Turns the recorded audio into text, usually by sending it to your own
   * speech-to-text endpoint. Return an empty string when nothing was said.
   */
  transcribe: (audio: Blob, info: { lang: string; signal: AbortSignal }) => Promise<string>;
  /** Preferred recording types, first supported wins. Default: WebM/Opus, then MP4, then the browser's default. */
  mimeTypes?: readonly string[];
  /** Stops recording after this long, so a forgotten microphone does not record forever. Default 120000 (2 minutes). */
  maxDurationMs?: number;
}

const DEFAULT_TYPES = ['audio/webm;codecs=opus', 'audio/webm', 'audio/mp4'];

/** True where the browser can record the microphone (MediaRecorder and getUserMedia). */
export function canRecordAudio(): boolean {
  return (
    typeof window !== 'undefined' &&
    typeof window.MediaRecorder === 'function' &&
    !!navigator.mediaDevices?.getUserMedia
  );
}

function result(text: string): AiSpeechResult {
  return Object.assign([{ transcript: text }], { isFinal: true }) as AiSpeechResult;
}

/**
 * A recognizer for `AiVoiceInput`'s `createRecognition` that works where the
 * browser has no speech recognition: it records the microphone and hands the
 * audio to `transcribe` when the person stops. There are no interim words; the
 * text arrives once, as final. Returns null where recording is unavailable.
 *
 * ```tsx
 * const createRecognition = () => {
 *   const Browser = getSpeechRecognition();
 *   return Browser ? new Browser() : createRecordingRecognition({ transcribe });
 * };
 * <AiVoiceInput createRecognition={createRecognition} onTranscript={append} />
 * ```
 */
export function createRecordingRecognition(
  options: RecordingRecognitionOptions,
): AiSpeechRecognitionLike | null {
  if (!canRecordAudio()) return null;
  const { transcribe, mimeTypes = DEFAULT_TYPES, maxDurationMs = 120_000 } = options;

  let stream: MediaStream | null = null;
  let recorder: MediaRecorder | null = null;
  let chunks: Blob[] = [];
  let timer: ReturnType<typeof setTimeout> | undefined;
  let controller: AbortController | null = null;
  /** Set by `abort()`: the recording is thrown away and nothing is transcribed. */
  let discarded = false;
  let active = false;

  const release = () => {
    clearTimeout(timer);
    stream?.getTracks().forEach((track) => track.stop());
    stream = null;
    recorder = null;
  };
  const end = () => {
    active = false;
    recognition.onend?.();
  };
  const fail = (error: unknown) => {
    release();
    recognition.onerror?.({
      error: error instanceof DOMException || error instanceof Error ? error.name : 'audio-capture',
    });
    end();
  };

  const recognition: AiSpeechRecognitionLike = {
    lang: '',
    continuous: false,
    interimResults: false,
    onresult: null,
    onerror: null,
    onend: null,
    start() {
      if (active) return;
      active = true;
      discarded = false;
      chunks = [];
      navigator.mediaDevices.getUserMedia({ audio: true }).then((media) => {
        if (discarded) {
          media.getTracks().forEach((track) => track.stop());
          return end();
        }
        stream = media;
        const type = mimeTypes.find((t) => window.MediaRecorder.isTypeSupported?.(t));
        recorder = new window.MediaRecorder(media, type ? { mimeType: type } : undefined);
        recorder.ondataavailable = (event) => {
          if (event.data.size > 0) chunks.push(event.data);
        };
        recorder.onerror = () => fail(new Error('audio-capture'));
        recorder.onstop = () => {
          const audio = new Blob(chunks, { type: recorder?.mimeType || type || 'audio/webm' });
          release();
          if (discarded || audio.size === 0) return end();
          controller = new AbortController();
          transcribe(audio, { lang: recognition.lang, signal: controller.signal }).then(
            (text) => {
              if (!discarded && text.trim())
                recognition.onresult?.({ resultIndex: 0, results: [result(text.trim())] });
              end();
            },
            () => {
              if (!discarded) recognition.onerror?.({ error: 'network' });
              end();
            },
          );
        };
        recorder.start();
        timer = setTimeout(() => recognition.stop(), maxDurationMs);
      }, fail);
    },
    stop() {
      if (recorder && recorder.state !== 'inactive') recorder.stop();
      else if (active && !recorder) discarded = true;
    },
    abort() {
      discarded = true;
      controller?.abort();
      if (recorder && recorder.state !== 'inactive') recorder.stop();
      else release();
    },
  };
  return recognition;
}
