import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { act, render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import {
  AiVoiceInput,
  canRecordAudio,
  createRecordingRecognition,
  getSpeechRecognition,
  type AiSpeechRecognitionLike,
  type AiSpeechResult,
} from './index.js';

class FakeRecognition implements AiSpeechRecognitionLike {
  static last: FakeRecognition | null = null;
  lang = '';
  continuous = false;
  interimResults = false;
  started = false;
  onresult: AiSpeechRecognitionLike['onresult'] = null;
  onerror: AiSpeechRecognitionLike['onerror'] = null;
  onend: AiSpeechRecognitionLike['onend'] = null;
  constructor() {
    FakeRecognition.last = this;
  }
  start() {
    this.started = true;
  }
  stop() {
    this.started = false;
    this.onend?.();
  }
  abort() {
    this.started = false;
  }
  emit(results: [string, boolean][], resultIndex = 0) {
    const list = results.map(([text, isFinal]) => {
      const r = [{ transcript: text }] as unknown as AiSpeechResult;
      r.isFinal = isFinal;
      return r;
    });
    act(() => this.onresult?.({ resultIndex, results: list }));
  }
  fail(error: string) {
    act(() => this.onerror?.({ error }));
  }
}

const create = () => new FakeRecognition();

afterEach(() => {
  FakeRecognition.last = null;
  vi.unstubAllGlobals();
});

describe('AiVoiceInput', () => {
  it('renders nothing when speech recognition is unavailable', () => {
    expect(getSpeechRecognition()).toBeNull();
    const { container } = render(<AiVoiceInput onTranscript={() => {}} />);
    expect(container).toBeEmptyDOMElement();
  });

  it('uses the browser recognizer when present', () => {
    vi.stubGlobal('webkitSpeechRecognition', FakeRecognition);
    expect(getSpeechRecognition()).toBe(FakeRecognition);
    render(<AiVoiceInput onTranscript={() => {}} />);
    expect(screen.getByRole('button', { name: 'Dictate' })).toBeInTheDocument();
  });

  it('toggles dictation, reports interim and final text, and never sends', async () => {
    const user = userEvent.setup();
    const onTranscript = vi.fn();
    const onStateChange = vi.fn();
    render(
      <AiVoiceInput
        data-testid="root"
        lang="en-GB"
        onTranscript={onTranscript}
        onStateChange={onStateChange}
        createRecognition={create}
      />,
    );
    const root = screen.getByTestId('root');
    expect(root).toHaveClass('mn-ai-voice-input');
    const button = screen.getByRole('button', { name: 'Dictate' });
    expect(button).toHaveAttribute('aria-pressed', 'false');
    await expectNoA11yViolations();

    await user.click(button);
    const rec = FakeRecognition.last as FakeRecognition;
    expect(rec.started).toBe(true);
    expect(rec.lang).toBe('en-GB');
    expect(rec.continuous).toBe(true);
    expect(rec.interimResults).toBe(true);
    expect(button).toHaveAttribute('aria-pressed', 'true');
    expect(button).toHaveAccessibleDescription('Press Escape to stop.');
    expect(root).toHaveAttribute('data-state', 'listening');
    expect(
      screen.getByText('Listening…', { selector: '.mn-ai-voice-input__status' }),
    ).toBeInTheDocument();

    rec.emit([['hello wor', false]]);
    expect(onTranscript).toHaveBeenLastCalledWith('hello wor', { final: false });
    expect(screen.getByText(/hello wor/)).toHaveClass('mn-ai-voice-input__transcript');
    rec.emit([['hello world', true]]);
    expect(onTranscript).toHaveBeenLastCalledWith('hello world', { final: true });
    await expectNoA11yViolations();

    await user.click(button);
    expect(rec.started).toBe(false);
    expect(button).toHaveAttribute('aria-pressed', 'false');
    expect(root).toHaveAttribute('data-state', 'idle');
    expect(document.querySelector('.mn-ai-voice-input__live')).toHaveTextContent(
      'Stopped listening.',
    );
    expect(onStateChange.mock.calls.map((c) => c[0])).toEqual(['starting', 'listening', 'idle']);
  });

  it('stops with Escape', async () => {
    const user = userEvent.setup();
    render(<AiVoiceInput onTranscript={() => {}} createRecognition={create} />);
    await user.click(screen.getByRole('button', { name: 'Dictate' }));
    await user.keyboard('{Escape}');
    expect(FakeRecognition.last?.started).toBe(false);
    expect(screen.getByRole('button', { name: 'Dictate' })).toHaveAttribute(
      'aria-pressed',
      'false',
    );
  });

  it('push-to-talk listens only while Space is held', async () => {
    const user = userEvent.setup();
    render(<AiVoiceInput mode="push-to-talk" onTranscript={() => {}} createRecognition={create} />);
    const button = screen.getByRole('button', { name: 'Hold to talk' });
    button.focus();
    await user.keyboard('[Space>]');
    const rec = FakeRecognition.last as FakeRecognition;
    expect(rec.started).toBe(true);
    expect(rec.continuous).toBe(false);
    expect(button).toHaveAttribute('aria-pressed', 'true');
    await user.keyboard('[/Space]');
    expect(rec.started).toBe(false);
    expect(button).toHaveAttribute('aria-pressed', 'false');
  });

  it('shows guidance when microphone access is blocked', async () => {
    const user = userEvent.setup();
    render(<AiVoiceInput onTranscript={() => {}} createRecognition={create} />);
    await user.click(screen.getByRole('button', { name: 'Dictate' }));
    FakeRecognition.last?.fail('not-allowed');
    expect(screen.getByRole('alert')).toHaveTextContent(/Microphone access is blocked/);
    expect(screen.getByRole('button', { name: 'Dictate' })).toHaveAttribute(
      'aria-pressed',
      'false',
    );
    await expectNoA11yViolations();
  });

  it('explains other errors politely', async () => {
    const user = userEvent.setup();
    render(
      <AiVoiceInput
        onTranscript={() => {}}
        createRecognition={create}
        messages={{ noSpeech: 'Rien entendu.' }}
      />,
    );
    await user.click(screen.getByRole('button', { name: 'Dictate' }));
    FakeRecognition.last?.fail('no-speech');
    expect(screen.getByText('Rien entendu.')).toHaveAttribute('role', 'status');
  });

  it('waits for the microphone before listening, and treats a refused meter as denied', async () => {
    const user = userEvent.setup();
    const stopTrack = vi.fn();
    let resolve: (s: MediaStream) => void = () => {};
    const getUserMedia = vi.fn(() => new Promise<MediaStream>((r) => (resolve = r)));
    vi.stubGlobal('navigator', { ...navigator, mediaDevices: { getUserMedia } });
    const { unmount } = render(
      <AiVoiceInput data-testid="root" onTranscript={() => {}} createRecognition={create} />,
    );
    await user.click(screen.getByRole('button', { name: 'Dictate' }));
    expect(screen.getByTestId('root')).toHaveAttribute('data-state', 'starting');
    expect(screen.getByText('Starting microphone…')).toBeInTheDocument();
    await act(async () =>
      resolve({ getTracks: () => [{ stop: stopTrack }] } as unknown as MediaStream),
    );
    expect(screen.getByTestId('root')).toHaveAttribute('data-state', 'listening');
    unmount();
    expect(stopTrack).toHaveBeenCalled();

    getUserMedia.mockImplementationOnce(() =>
      Promise.reject(Object.assign(new Error('blocked'), { name: 'NotAllowedError' })),
    );
    render(<AiVoiceInput onTranscript={() => {}} createRecognition={create} />);
    await user.click(screen.getByRole('button', { name: 'Dictate' }));
    expect(await screen.findByRole('alert')).toHaveTextContent(/Microphone access is blocked/);
  });

  it('renders nothing on the server', () => {
    expect(
      renderToString(<AiVoiceInput onTranscript={() => {}} createRecognition={create} />),
    ).toBe('');
  });
});

describe('recording recognizer', () => {
  class FakeRecorder {
    static last: FakeRecorder | null = null;
    static isTypeSupported = (type: string) => type === 'audio/webm';
    state: 'inactive' | 'recording' = 'inactive';
    mimeType: string;
    ondataavailable: ((event: { data: Blob }) => void) | null = null;
    onstop: (() => void) | null = null;
    onerror: (() => void) | null = null;
    constructor(_stream: unknown, options?: { mimeType?: string }) {
      this.mimeType = options?.mimeType ?? '';
      FakeRecorder.last = this;
    }
    start() {
      this.state = 'recording';
    }
    stop() {
      this.state = 'inactive';
      this.ondataavailable?.({ data: new Blob(['audio'], { type: this.mimeType }) });
      this.onstop?.();
    }
  }

  const tick = () => new Promise((resolve) => setTimeout(resolve, 0));

  describe('createRecordingRecognition', () => {
    const stopTrack = vi.fn();
    const getUserMedia = vi.fn();

    beforeEach(() => {
      stopTrack.mockClear();
      getUserMedia.mockReset().mockResolvedValue({ getTracks: () => [{ stop: stopTrack }] });
      vi.stubGlobal('MediaRecorder', FakeRecorder);
      Object.defineProperty(navigator, 'mediaDevices', {
        configurable: true,
        value: { getUserMedia },
      });
    });
    afterEach(() => {
      vi.unstubAllGlobals();
      Reflect.deleteProperty(navigator, 'mediaDevices');
    });

    it('records, transcribes on stop, reports one final result, then ends and frees the microphone', async () => {
      const transcribe = vi.fn(async () => '  Hello there  ');
      const rec = createRecordingRecognition({ transcribe })!;
      const onresult = vi.fn();
      const onend = vi.fn();
      Object.assign(rec, { onresult, onend, lang: 'en-GB' });
      rec.start();
      await tick();
      expect(FakeRecorder.last?.mimeType).toBe('audio/webm');
      rec.stop();
      await tick();
      expect(transcribe).toHaveBeenCalledWith(
        expect.any(Blob),
        expect.objectContaining({ lang: 'en-GB' }),
      );
      const event = onresult.mock.calls[0]?.[0];
      expect(event.results[0][0].transcript).toBe('Hello there');
      expect(event.results[0].isFinal).toBe(true);
      expect(onend).toHaveBeenCalledTimes(1);
      expect(stopTrack).toHaveBeenCalled();
    });

    it('abort throws the recording away: nothing is transcribed', async () => {
      const transcribe = vi.fn(async () => 'never');
      const rec = createRecordingRecognition({ transcribe })!;
      const onend = vi.fn();
      rec.onend = onend;
      rec.start();
      await tick();
      rec.abort();
      await tick();
      expect(transcribe).not.toHaveBeenCalled();
      expect(onend).toHaveBeenCalledTimes(1);
    });

    it('reports a blocked microphone by its error name, and a failed transcription as network', async () => {
      getUserMedia.mockRejectedValueOnce(new DOMException('blocked', 'NotAllowedError'));
      const denied = createRecordingRecognition({ transcribe: async () => '' })!;
      const onerror = vi.fn();
      denied.onerror = onerror;
      denied.start();
      await tick();
      expect(onerror).toHaveBeenCalledWith({ error: 'NotAllowedError' });

      const failing = createRecordingRecognition({
        transcribe: async () => {
          throw new Error('503');
        },
      })!;
      const onerror2 = vi.fn();
      failing.onerror = onerror2;
      failing.start();
      await tick();
      failing.stop();
      await tick();
      expect(onerror2).toHaveBeenCalledWith({ error: 'network' });
    });

    it('is null where the browser cannot record', () => {
      vi.unstubAllGlobals();
      expect(canRecordAudio()).toBe(false);
      expect(createRecordingRecognition({ transcribe: async () => '' })).toBeNull();
    });
  });
});
