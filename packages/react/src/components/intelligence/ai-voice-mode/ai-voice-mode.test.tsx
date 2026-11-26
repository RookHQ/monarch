import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { act, fireEvent, render, renderHook, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { createRef } from 'react';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import {
  AI_VOICE_MODE_MESSAGES,
  AiVoiceMode,
  formatAiVoiceDuration,
  type AiVoiceCaption,
  type AiVoiceModeState,
  createBrowserVoiceSession,
  createPcmAudioPlayer,
  createRealtimeVoiceSession,
  createVoiceActivityDetector,
  decodeBase64Pcm16,
  measureAudioLevel,
  pcm16ToFloat32,
  useAiVoiceSession,
  type AiAudioContextLike,
  type AiSpeechSynthesisLike,
  type AiVoiceSessionEvent,
  type RealtimeVoiceSessionOptions,
} from './index.js';
import type { AiSpeechRecognitionLike } from '../ai-voice-input/index.js';

const agent = { name: 'Atlas' };

const captions: AiVoiceCaption[] = [
  { id: 'c1', speaker: 'user', text: 'What is on my calendar today?', final: true },
  { id: 'c2', speaker: 'assistant', text: 'You have two meetings', final: false },
];

/** Gives the jsdom element a scroll geometry. */
function layout(el: HTMLElement, scrollHeight: number, clientHeight: number) {
  Object.defineProperty(el, 'scrollHeight', { configurable: true, value: scrollHeight });
  Object.defineProperty(el, 'clientHeight', { configurable: true, value: clientHeight });
}

describe('AiVoiceMode', () => {
  it('renders a named region with the state in text, a clock and the controls', async () => {
    const ref = createRef<HTMLElement>();
    render(
      <AiVoiceMode
        ref={ref}
        data-testid="root"
        className="extra"
        state="listening"
        level={0.5}
        agent={agent}
        elapsedSeconds={65}
        captions={captions}
        onMuteChange={() => {}}
        onInterrupt={() => {}}
        onEnd={() => {}}
      />,
    );
    const root = screen.getByTestId('root');
    expect(root).toHaveClass('mn-ai-voice-mode', 'extra');
    expect(ref.current).toBe(root);
    expect(screen.getByRole('group', { name: 'Voice conversation with Atlas' })).toBe(root);
    expect(root).toHaveAttribute('data-state', 'listening');
    expect(root).toHaveAttribute('data-size', 'md');
    expect(root.style.getPropertyValue('--mn-ai-voice-mode-level')).toBe('0.5');
    expect(screen.getByText('Listening…')).toBeVisible();
    expect(screen.getByText('1:05')).toHaveAttribute('datetime', 'PT65S');
    expect(screen.getByRole('button', { name: 'Mute' })).toHaveAttribute('aria-pressed', 'false');
    expect(screen.getByRole('button', { name: 'Captions' })).toHaveAttribute(
      'aria-pressed',
      'true',
    );
    expect(screen.getByRole('button', { name: 'End' })).toBeInTheDocument();
    await expectNoA11yViolations();
  });

  it('shows text for every state and only passes the level on while someone is talking', () => {
    const all: AiVoiceModeState[] = [
      'connecting',
      'listening',
      'thinking',
      'speaking',
      'muted',
      'reconnecting',
      'ended',
      'error',
    ];
    const { rerender } = render(<AiVoiceMode data-testid="root" state="connecting" level={2} />);
    for (const state of all) {
      rerender(<AiVoiceMode data-testid="root" state={state} level={2} />);
      const root = screen.getByTestId('root');
      expect(root).toHaveAttribute('data-state', state);
      expect(root).toHaveTextContent(AI_VOICE_MODE_MESSAGES.states[state]);
      const voiced = state === 'listening' || state === 'speaking';
      // Clamped to 1 while voiced, 0 otherwise.
      expect(root.style.getPropertyValue('--mn-ai-voice-mode-level')).toBe(voiced ? '1' : '0');
    }
    rerender(<AiVoiceMode data-testid="root" state="connecting" />);
    expect(screen.getByTestId('root')).toHaveAttribute('aria-busy', 'true');
  });

  it('toggles mute from the button and reports the wanted value', async () => {
    const user = userEvent.setup();
    const onMuteChange = vi.fn();
    const { rerender } = render(<AiVoiceMode state="listening" onMuteChange={onMuteChange} />);
    await user.click(screen.getByRole('button', { name: 'Mute' }));
    expect(onMuteChange).toHaveBeenLastCalledWith(true);

    rerender(<AiVoiceMode data-testid="root" state="muted" onMuteChange={onMuteChange} />);
    const mute = screen.getByRole('button', { name: 'Mute' });
    expect(mute).toHaveAttribute('aria-pressed', 'true');
    expect(screen.getByTestId('root')).toHaveAttribute('data-muted');
    expect(screen.getByText('Muted', { selector: '.mn-ai-voice-mode__state-text' })).toBeVisible();
    await user.click(mute);
    expect(onMuteChange).toHaveBeenLastCalledWith(false);
    await expectNoA11yViolations();
  });

  it('shows a muted microphone while the assistant speaks when `muted` is passed', () => {
    render(<AiVoiceMode state="speaking" muted onMuteChange={() => {}} />);
    expect(screen.getByText('Speaking…')).toBeVisible();
    expect(screen.getByText('Your microphone is muted')).toBeVisible();
    expect(screen.getByRole('button', { name: 'Mute' })).toHaveAttribute('aria-pressed', 'true');
  });

  it('toggles mute with Space inside the surface, but not on another control', async () => {
    const user = userEvent.setup();
    const onMuteChange = vi.fn();
    const onEnd = vi.fn();
    render(<AiVoiceMode state="listening" onMuteChange={onMuteChange} onEnd={onEnd} />);
    const log = screen.getByRole('log', { name: 'Captions' });
    log.focus();
    await user.keyboard(' ');
    expect(onMuteChange).toHaveBeenCalledTimes(1);
    expect(onMuteChange).toHaveBeenLastCalledWith(true);

    // On End, Space presses End and leaves the microphone alone.
    screen.getByRole('button', { name: 'End' }).focus();
    await user.keyboard(' ');
    expect(onEnd).toHaveBeenCalledTimes(1);
    expect(onMuteChange).toHaveBeenCalledTimes(1);

    // On Mute itself, Space toggles once (the button's own click), not twice.
    screen.getByRole('button', { name: 'Mute' }).focus();
    await user.keyboard(' ');
    expect(onMuteChange).toHaveBeenCalledTimes(2);
  });

  it('ignores Space once the call is over, held-down repeats and modified presses', async () => {
    const user = userEvent.setup();
    const onMuteChange = vi.fn();
    const { rerender } = render(
      <AiVoiceMode data-testid="root" state="listening" onMuteChange={onMuteChange} />,
    );
    const root = screen.getByTestId('root');
    root.focus();
    expect(root).toHaveFocus();
    fireEvent.keyDown(root, { key: ' ', repeat: true });
    fireEvent.keyDown(root, { key: ' ', shiftKey: true });
    expect(onMuteChange).not.toHaveBeenCalled();
    rerender(<AiVoiceMode data-testid="root" state="ended" onMuteChange={onMuteChange} />);
    await user.keyboard(' ');
    expect(onMuteChange).not.toHaveBeenCalled();
  });

  it('never ends the call on Escape', async () => {
    const user = userEvent.setup();
    const onEnd = vi.fn();
    const onMuteChange = vi.fn();
    render(<AiVoiceMode state="speaking" onEnd={onEnd} onMuteChange={onMuteChange} />);
    screen.getByRole('log').focus();
    await user.keyboard('{Escape}');
    screen.getByRole('button', { name: 'End' }).focus();
    await user.keyboard('{Escape}');
    expect(onEnd).not.toHaveBeenCalled();
    expect(onMuteChange).not.toHaveBeenCalled();
    await user.keyboard('{Enter}');
    expect(onEnd).toHaveBeenCalledTimes(1);
  });

  it('offers Interrupt only while the assistant is thinking or speaking', async () => {
    const user = userEvent.setup();
    const onInterrupt = vi.fn();
    const props = { onInterrupt, onMuteChange: () => {} };
    const { rerender } = render(<AiVoiceMode state="listening" {...props} />);
    expect(screen.queryByRole('button', { name: 'Interrupt' })).toBeNull();
    rerender(<AiVoiceMode state="thinking" {...props} />);
    expect(screen.getByRole('button', { name: 'Interrupt' })).toBeInTheDocument();
    rerender(<AiVoiceMode state="speaking" {...props} />);
    const interrupt = screen.getByRole('button', { name: 'Interrupt' });
    interrupt.focus();
    await user.keyboard('{Enter}');
    expect(onInterrupt).toHaveBeenCalledTimes(1);

    // When it goes away under the person's focus, focus moves to Mute.
    rerender(<AiVoiceMode state="listening" {...props} />);
    expect(screen.queryByRole('button', { name: 'Interrupt' })).toBeNull();
    expect(screen.getByRole('button', { name: 'Mute' })).toHaveFocus();
  });

  it('shows captions by default and hides them with the toggle (uncontrolled)', async () => {
    const user = userEvent.setup();
    const onCaptionsVisibleChange = vi.fn();
    render(
      <AiVoiceMode
        data-testid="root"
        state="listening"
        captions={captions}
        onCaptionsVisibleChange={onCaptionsVisibleChange}
      />,
    );
    expect(screen.getByRole('log', { name: 'Captions' })).toBeInTheDocument();
    const toggle = screen.getByRole('button', { name: 'Captions' });
    await user.click(toggle);
    expect(onCaptionsVisibleChange).toHaveBeenLastCalledWith(false);
    expect(screen.queryByRole('log')).toBeNull();
    expect(toggle).toHaveAttribute('aria-pressed', 'false');
    expect(screen.getByTestId('root')).toHaveAttribute('data-captions', 'off');
    await user.click(toggle);
    expect(onCaptionsVisibleChange).toHaveBeenLastCalledWith(true);
    expect(screen.getByRole('log')).toBeInTheDocument();
  });

  it('follows a controlled `captionsVisible`', async () => {
    const user = userEvent.setup();
    const onCaptionsVisibleChange = vi.fn();
    const { rerender } = render(
      <AiVoiceMode
        state="listening"
        captions={captions}
        captionsVisible={false}
        onCaptionsVisibleChange={onCaptionsVisibleChange}
      />,
    );
    expect(screen.queryByRole('log')).toBeNull();
    await user.click(screen.getByRole('button', { name: 'Captions' }));
    expect(onCaptionsVisibleChange).toHaveBeenCalledWith(true);
    // Still hidden until the owner says so.
    expect(screen.queryByRole('log')).toBeNull();
    rerender(<AiVoiceMode state="listening" captions={captions} captionsVisible />);
    expect(screen.getByRole('log')).toBeInTheDocument();
  });

  it('keeps interim captions out of the live log until they are final', async () => {
    const { rerender } = render(<AiVoiceMode state="speaking" agent={agent} captions={captions} />);
    const log = screen.getByRole('log', { name: 'Captions' });
    expect(log).toHaveAttribute('aria-live', 'polite');
    expect(log).toHaveAttribute('aria-relevant', 'additions');
    expect(within(log).getByText('You')).toBeInTheDocument();
    expect(within(log).getByText('Atlas')).toBeInTheDocument();

    const interim = within(log).getByText('You have two meetings').closest('p')!;
    expect(interim).toHaveAttribute('aria-hidden', 'true');
    expect(interim).toHaveAttribute('data-final', 'false');

    // More interim words: the same node is updated in place.
    rerender(
      <AiVoiceMode
        state="speaking"
        agent={agent}
        captions={[captions[0]!, { ...captions[1]!, text: 'You have two meetings today' }]}
      />,
    );
    expect(within(log).getByText('You have two meetings today').closest('p')).toBe(interim);

    // Final: a new node is added to the log, which is what gets announced.
    rerender(
      <AiVoiceMode
        state="speaking"
        agent={agent}
        captions={[
          captions[0]!,
          { ...captions[1]!, text: 'You have two meetings today.', final: true },
        ]}
      />,
    );
    const final = within(log).getByText('You have two meetings today.').closest('p')!;
    expect(final).not.toBe(interim);
    expect(final).not.toHaveAttribute('aria-hidden');
    expect(interim).not.toBeInTheDocument();
    await expectNoA11yViolations();
  });

  it('scrolls to the newest caption unless the person scrolled up', () => {
    const { rerender } = render(<AiVoiceMode state="listening" captions={captions} />);
    const log = screen.getByRole('log');
    const more = (n: number): AiVoiceCaption[] => [
      ...captions,
      ...Array.from({ length: n }, (_, i) => ({
        id: `m${i}`,
        speaker: 'assistant' as const,
        text: `Line ${i}`,
        final: true,
      })),
    ];

    layout(log, 400, 100);
    rerender(<AiVoiceMode state="listening" captions={more(1)} />);
    expect(log.scrollTop).toBe(400);

    // The person scrolls up to read.
    log.scrollTop = 50;
    fireEvent.scroll(log);
    layout(log, 600, 100);
    rerender(<AiVoiceMode state="listening" captions={more(2)} />);
    expect(log.scrollTop).toBe(50);

    // Back at the end, following resumes.
    log.scrollTop = 500;
    fireEvent.scroll(log);
    layout(log, 800, 100);
    rerender(<AiVoiceMode state="listening" captions={more(3)} />);
    expect(log.scrollTop).toBe(800);
  });

  it('announces state changes once, but not the first state or listening/speaking flips', () => {
    const { rerender } = render(<AiVoiceMode state="connecting" />);
    const status = screen.getByRole('status');
    expect(status).toHaveAttribute('aria-live', 'polite');
    expect(status).toBeEmptyDOMElement();
    rerender(<AiVoiceMode state="listening" />);
    expect(status).toBeEmptyDOMElement();
    rerender(<AiVoiceMode state="thinking" />);
    expect(status).toHaveTextContent('Thinking…');
    rerender(<AiVoiceMode state="speaking" />);
    expect(status).toBeEmptyDOMElement();
    rerender(<AiVoiceMode state="listening" />);
    expect(status).toBeEmptyDOMElement();
    rerender(<AiVoiceMode state="muted" />);
    expect(status).toHaveTextContent('Muted');
    rerender(<AiVoiceMode state="reconnecting" />);
    expect(status).toHaveTextContent('Reconnecting…');
    // A re-render in the same state (a new level) says nothing new.
    rerender(<AiVoiceMode state="reconnecting" level={0.3} />);
    expect(status).toHaveTextContent('Reconnecting…');
    rerender(<AiVoiceMode state="error" error="The connection was lost." />);
    expect(status).toHaveTextContent('Something went wrong: The connection was lost.');
  });

  it('shows the duration and an optional Start again once ended', async () => {
    const user = userEvent.setup();
    const onRestart = vi.fn();
    const { rerender } = render(
      <AiVoiceMode
        state="ended"
        elapsedSeconds={245}
        captions={captions}
        onMuteChange={() => {}}
        onEnd={() => {}}
      />,
    );
    expect(screen.getByText('Conversation ended')).toBeVisible();
    expect(screen.getByText('Lasted 4:05')).toHaveAttribute('datetime', 'PT245S');
    expect(screen.queryByRole('button', { name: 'Mute' })).toBeNull();
    expect(screen.queryByRole('button', { name: 'End' })).toBeNull();
    expect(screen.queryByRole('button', { name: 'Start again' })).toBeNull();
    // What was said stays readable.
    expect(screen.getByRole('log')).toHaveTextContent('What is on my calendar today?');

    rerender(<AiVoiceMode state="ended" elapsedSeconds={245} onRestart={onRestart} />);
    expect(screen.queryByRole('log')).toBeNull();
    await user.click(screen.getByRole('button', { name: 'Start again' }));
    expect(onRestart).toHaveBeenCalledTimes(1);
    await expectNoA11yViolations();
  });

  it('shows the error with Retry', async () => {
    const user = userEvent.setup();
    const onRetry = vi.fn();
    const { rerender } = render(<AiVoiceMode state="error" onRetry={onRetry} />);
    expect(screen.getByText('Something went wrong')).toBeVisible();
    expect(screen.getByText(AI_VOICE_MODE_MESSAGES.errorFallback)).toBeVisible();
    rerender(<AiVoiceMode state="error" error="The microphone is in use." onRetry={onRetry} />);
    expect(screen.getByText('The microphone is in use.')).toBeVisible();
    await user.click(screen.getByRole('button', { name: 'Retry' }));
    expect(onRetry).toHaveBeenCalledTimes(1);
    await expectNoA11yViolations();
  });

  it('has a compact size', async () => {
    render(
      <AiVoiceMode
        data-testid="root"
        size="compact"
        state="speaking"
        agent={agent}
        captions={captions}
        onMuteChange={() => {}}
        onInterrupt={() => {}}
        onEnd={() => {}}
      />,
    );
    expect(screen.getByTestId('root')).toHaveAttribute('data-size', 'compact');
    expect(screen.getByRole('button', { name: 'Interrupt' })).toHaveAttribute('data-size', 'sm');
    await expectNoA11yViolations();
  });

  it('accepts translated strings and a locale', () => {
    render(
      <AiVoiceMode
        state="muted"
        elapsedSeconds={3725}
        locale="ar-EG"
        captions={[{ id: 'a', speaker: 'user', text: 'Bonjour', final: true }]}
        onMuteChange={() => {}}
        onEnd={() => {}}
        messages={{
          label: (name) => `Conversation vocale avec ${name}`,
          assistant: 'Assistant',
          you: 'Vous',
          states: { muted: 'Micro coupé' },
          mute: 'Couper le micro',
          end: 'Terminer',
          captions: 'Sous-titres',
          captionsLabel: 'Sous-titres en direct',
        }}
      />,
    );
    expect(
      screen.getByRole('group', { name: 'Conversation vocale avec Assistant' }),
    ).toBeInTheDocument();
    expect(screen.getByText('Micro coupé')).toBeVisible();
    expect(screen.getByRole('button', { name: 'Couper le micro' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Terminer' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Sous-titres' })).toBeInTheDocument();
    expect(screen.getByRole('log', { name: 'Sous-titres en direct' })).toHaveTextContent('Vous');
    expect(screen.getByText(formatAiVoiceDuration(3725, 'ar-EG'))).toBeInTheDocument();
  });

  it('formats durations as a clock', () => {
    expect(formatAiVoiceDuration(0, 'en')).toBe('0:00');
    expect(formatAiVoiceDuration(65.9, 'en')).toBe('1:05');
    expect(formatAiVoiceDuration(3725, 'en')).toBe('1:02:05');
    expect(formatAiVoiceDuration(-4, 'en')).toBe('0:00');
    expect(formatAiVoiceDuration(Number.NaN, 'en')).toBe('0:00');
    expect(formatAiVoiceDuration(65, 'ar-EG')).not.toBe('1:05');
  });

  it('passes native props through and calls a consumer onKeyDown first', async () => {
    const user = userEvent.setup();
    const onMuteChange = vi.fn();
    render(
      <AiVoiceMode
        data-testid="root"
        id="call"
        state="listening"
        onMuteChange={onMuteChange}
        onKeyDown={(event) => event.preventDefault()}
      >
        <p>Microphone: built-in</p>
      </AiVoiceMode>,
    );
    expect(screen.getByTestId('root')).toHaveAttribute('id', 'call');
    expect(screen.getByText('Microphone: built-in')).toBeInTheDocument();
    screen.getByRole('log').focus();
    await user.keyboard(' ');
    expect(onMuteChange).not.toHaveBeenCalled();
  });

  it('renders on the server', () => {
    const html = renderToString(
      <AiVoiceMode
        state="speaking"
        level={0.4}
        agent={agent}
        captions={captions}
        elapsedSeconds={12}
        locale="en"
        onMuteChange={() => {}}
        onEnd={() => {}}
      />,
    );
    expect(html).toContain('mn-ai-voice-mode');
    expect(html).toContain('Speaking…');
    expect(html).toContain('0:12');
    expect(html).toContain('What is on my calendar today?');
  });
});

describe('voice sessions', () => {
  function fakes() {
    const recognition: AiSpeechRecognitionLike & { onspeechstart: (() => void) | null } = {
      lang: '',
      continuous: false,
      interimResults: false,
      start: vi.fn(),
      stop: vi.fn(),
      abort: vi.fn(),
      onresult: null,
      onerror: null,
      onend: null,
      onspeechstart: null,
    };
    const synth = { speaking: false, speak: vi.fn(), cancel: vi.fn() };
    const synthesis: AiSpeechSynthesisLike = {
      get speaking() {
        return synth.speaking;
      },
      speak: (u) => {
        synth.speaking = true;
        synth.speak(u);
      },
      cancel: () => {
        synth.speaking = false;
        synth.cancel();
      },
    };
    const utterance = (text: string) => ({ text }) as unknown as SpeechSynthesisUtterance;
    const say = (text: string, isFinal: boolean) =>
      recognition.onresult?.({
        resultIndex: 0,
        results: [{ isFinal, length: 1, 0: { transcript: text } }],
      });
    return { recognition, synth, synthesis, utterance, say };
  }

  it('listens, hands final turns on, speaks replies and stops speaking when interrupted', () => {
    const f = fakes();
    const onUserTurn = vi.fn();
    const session = createBrowserVoiceSession({
      lang: 'en-GB',
      onUserTurn,
      createRecognition: () => f.recognition,
      synthesis: f.synthesis,
      createUtterance: f.utterance,
    });
    const { result } = renderHook(() => useAiVoiceSession(session));
    act(() => session.start());
    expect(f.recognition.continuous).toBe(true);
    expect(result.current.props.state).toBe('listening');
    act(() => f.say('What is', false));
    act(() => f.say('What is the time', true));
    expect(onUserTurn).toHaveBeenCalledWith('What is the time');
    expect(result.current.props.captions).toEqual([
      { id: 'user-0', speaker: 'user', text: 'What is the time', final: true },
    ]);
    expect(result.current.props.state).toBe('thinking');
    act(() => session.speak('It is noon.'));
    expect(result.current.props.state).toBe('speaking');
    act(() => result.current.props.onInterrupt());
    expect(f.synth.cancel).toHaveBeenCalled();
    expect(result.current.props.state).toBe('listening');
  });

  it('barge-in: talking over the assistant stops its speech', () => {
    const f = fakes();
    const session = createBrowserVoiceSession({
      createRecognition: () => f.recognition,
      synthesis: f.synthesis,
      createUtterance: f.utterance,
    });
    session.start();
    session.speak('A long answer');
    expect(session.state).toBe('speaking');
    f.say('stop', false);
    expect(f.synth.cancel).toHaveBeenCalled();
    expect(session.state).toBe('listening');
  });

  it('reports unsupported browsers and blocked microphones as errors', () => {
    const unsupported = createBrowserVoiceSession({
      createRecognition: () => null as unknown as AiSpeechRecognitionLike,
    });
    const events: string[] = [];
    unsupported.subscribe((e) => events.push(e.type === 'error' ? e.code : e.type));
    unsupported.start();
    expect(events).toContain('not-supported');
    const f = fakes();
    const blocked = createBrowserVoiceSession({
      createRecognition: () => f.recognition,
      synthesis: f.synthesis,
    });
    const { result } = renderHook(() => useAiVoiceSession(blocked));
    act(() => blocked.start());
    act(() => f.recognition.onerror?.({ error: 'not-allowed' }));
    expect(result.current.props.state).toBe('error');
    expect(result.current.errorCode).toBe('not-allowed');
  });

  it('wires AiVoiceMode: Interrupt reaches the session', async () => {
    const f = fakes();
    const session = createBrowserVoiceSession({
      createRecognition: () => f.recognition,
      synthesis: f.synthesis,
      createUtterance: f.utterance,
    });
    function Voice() {
      const voice = useAiVoiceSession(session);
      return <AiVoiceMode {...voice.props} agent={agent} />;
    }
    render(<Voice />);
    act(() => {
      session.start();
      session.speak('Hello there');
    });
    await userEvent.click(screen.getByRole('button', { name: 'Interrupt' }));
    expect(f.synth.cancel).toHaveBeenCalled();
    expect(session.state).toBe('listening');
  });
});

describe('realtime voice session', () => {
  beforeEach(() => vi.useFakeTimers());
  afterEach(() => vi.useRealTimers());

  class FakeTrack {
    enabled = true;
    kind = 'audio';
    stop = vi.fn();
  }
  class FakeChannel {
    readyState: RTCDataChannelState = 'open';
    sent: unknown[] = [];
    onmessage: ((event: { data: unknown }) => void) | null = null;
    send = (message: unknown) => {
      this.sent.push(message);
    };
    close = vi.fn(() => {
      this.readyState = 'closed';
    });
  }
  class FakePeer {
    connectionState: RTCPeerConnectionState = 'new';
    iceGatheringState: RTCIceGatheringState = 'complete';
    localDescription: RTCSessionDescriptionInit | null = null;
    remoteDescription: RTCSessionDescriptionInit | null = null;
    onconnectionstatechange: (() => void) | null = null;
    ontrack: ((event: { streams: unknown[]; track: unknown }) => void) | null = null;
    channel = new FakeChannel();
    tracks: unknown[] = [];
    closed = false;
    label = '';
    constructor(public configuration: RTCConfiguration) {}
    addTrack(track: unknown) {
      this.tracks.push(track);
    }
    createDataChannel(label: string) {
      this.label = label;
      return this.channel;
    }
    async createOffer() {
      return { type: 'offer' as const, sdp: 'offer-sdp' };
    }
    async setLocalDescription(description: RTCSessionDescriptionInit) {
      this.localDescription = description;
    }
    async setRemoteDescription(description: RTCSessionDescriptionInit) {
      this.remoteDescription = description;
    }
    close() {
      this.closed = true;
      this.connectionState = 'closed';
    }
    addEventListener() {}
    removeEventListener() {}
    set(state: RTCPeerConnectionState) {
      this.connectionState = state;
      this.onconnectionstatechange?.();
    }
    message(data: unknown) {
      this.channel.onmessage?.({ data: typeof data === 'string' ? data : JSON.stringify(data) });
    }
  }

  function rig(extra: Partial<RealtimeVoiceSessionOptions> = {}) {
    const track = new FakeTrack();
    const stream = {
      getTracks: () => [track],
      getAudioTracks: () => [track],
    } as unknown as MediaStream;
    const peers: FakePeer[] = [];
    const analysers: { level: number; fftSize: number }[] = [];
    const sources: { connect: ReturnType<typeof vi.fn>; disconnect: ReturnType<typeof vi.fn> }[] =
      [];
    const context = {
      state: 'running' as AudioContextState,
      close: vi.fn(async () => {}),
      createMediaStreamSource: () => {
        const source = { connect: vi.fn(), disconnect: vi.fn() };
        sources.push(source);
        return source as unknown as MediaStreamAudioSourceNode;
      },
      createAnalyser: () => {
        const analyser = {
          level: 0,
          fftSize: 0,
          getFloatTimeDomainData(buffer: Float32Array) {
            buffer.fill(analyser.level);
          },
        };
        analysers.push(analyser);
        return analyser as unknown as AnalyserNode;
      },
    };
    const element = {
      autoplay: false,
      muted: false,
      srcObject: null as unknown,
      play: vi.fn(async () => {}),
      pause: vi.fn(),
    };
    const getUserMedia = vi.fn(async (_constraints: MediaStreamConstraints) => stream);
    const connect = vi.fn(
      async (_offer: RTCSessionDescriptionInit, _signal: AbortSignal) =>
        ({ type: 'answer', sdp: 'answer-sdp' }) as RTCSessionDescriptionInit,
    );
    const events: AiVoiceSessionEvent[] = [];
    const session = createRealtimeVoiceSession({
      connect,
      getUserMedia,
      createPeerConnection: (configuration) => {
        const peer = new FakePeer(configuration);
        peers.push(peer);
        return peer as unknown as RTCPeerConnection;
      },
      createAudioContext: () => context,
      createAudioElement: () => element as unknown as HTMLAudioElement,
      random: () => 0.5,
      ...extra,
    });
    session.subscribe((event) => events.push(event));
    const settle = () => vi.advanceTimersByTimeAsync(0);
    const up = async () => {
      session.start();
      await settle();
      peers[0]?.set('connected');
    };
    const peer = () => peers[peers.length - 1] as FakePeer;
    return {
      session,
      track,
      stream,
      peers,
      peer,
      analysers,
      sources,
      context,
      element,
      getUserMedia,
      connect,
      events,
      settle,
      up,
    };
  }

  const mapEvent = (raw: unknown): AiVoiceSessionEvent | null => {
    const event = raw as { kind?: string; id?: string; text?: string; final?: boolean };
    if (event.kind === 'speaking') return { type: 'state', state: 'speaking' };
    if (event.kind === 'thinking') return { type: 'state', state: 'thinking' };
    if (event.kind === 'listening') return { type: 'state', state: 'listening' };
    if (event.kind === 'heard')
      return {
        type: 'transcript',
        id: event.id ?? 'u',
        speaker: 'user',
        text: event.text ?? '',
        final: event.final ?? true,
      };
    return null;
  };

  it('opens the microphone, exchanges the offer for an answer and listens once connected', async () => {
    const r = rig({
      iceServers: [{ urls: 'stun:stun.example.test' }],
      audio: { echoCancellation: true },
      dataChannelLabel: 'control',
    });
    r.session.start();
    expect(r.session.state).toBe('connecting');
    await r.settle();
    expect(r.getUserMedia).toHaveBeenCalledWith({ audio: { echoCancellation: true } });
    expect(r.peer().configuration).toEqual({ iceServers: [{ urls: 'stun:stun.example.test' }] });
    expect(r.peer().tracks).toEqual([r.track]);
    expect(r.peer().label).toBe('control');
    expect(r.connect).toHaveBeenCalledTimes(1);
    expect(r.connect.mock.calls[0]?.[0]).toEqual({ type: 'offer', sdp: 'offer-sdp' });
    expect(r.connect.mock.calls[0]?.[1]).toBeInstanceOf(AbortSignal);
    expect(r.peer().remoteDescription).toEqual({ type: 'answer', sdp: 'answer-sdp' });
    expect(r.session.state).toBe('connecting');
    r.peer().set('connected');
    expect(r.session.state).toBe('listening');

    const remote = { id: 'remote' };
    r.peer().ontrack?.({ streams: [remote], track: {} });
    expect(r.element.srcObject).toBe(remote);
    expect(r.element.autoplay).toBe(true);
    expect(r.element.play).toHaveBeenCalled();
    r.session.stop();
  });

  it('turns data-channel messages into transcripts and states, and ignores the rest', async () => {
    const r = rig({ mapEvent });
    await r.up();
    r.peer().message({ kind: 'heard', id: 'u1', text: 'Hello', final: false });
    r.peer().message({ kind: 'thinking' });
    r.peer().message({ kind: 'unknown' });
    r.peer().message('not json');
    r.peer().message({ kind: 'speaking' });
    expect(r.events).toContainEqual({
      type: 'transcript',
      id: 'u1',
      speaker: 'user',
      text: 'Hello',
      final: false,
    });
    expect(r.session.state).toBe('speaking');
    r.peer().message({ kind: 'listening' });
    expect(r.session.state).toBe('listening');
    r.session.stop();
  });

  it('reports real levels: the microphone while listening, the assistant while speaking', async () => {
    const r = rig({ mapEvent, levelIntervalMs: 50, levelGain: 2, bargeIn: false });
    await r.up();
    r.peer().ontrack?.({ streams: [{ id: 'remote' }], track: {} });
    const [mic, remote] = r.analysers;
    if (!mic || !remote) throw new Error('no analysers');
    mic.level = 0.2;
    remote.level = 0.4;
    await vi.advanceTimersByTimeAsync(50);
    expect(r.events).toContainEqual(
      expect.objectContaining({ type: 'level', source: 'input', level: expect.closeTo(0.4, 5) }),
    );
    r.peer().message({ kind: 'speaking' });
    await vi.advanceTimersByTimeAsync(50);
    expect(r.events).toContainEqual(
      expect.objectContaining({ type: 'level', source: 'output', level: expect.closeTo(0.8, 5) }),
    );
    r.session.stop();
  });

  it('feeds useAiVoiceSession, so AiVoiceMode shows the measured level', async () => {
    const r = rig({ mapEvent, levelIntervalMs: 50, levelGain: 1 });
    function Voice() {
      const voice = useAiVoiceSession(r.session);
      return <AiVoiceMode {...voice.props} agent={agent} data-testid="voice" />;
    }
    render(<Voice />);
    await act(async () => {
      await r.up();
    });
    const mic = r.analysers[0];
    if (!mic) throw new Error('no analyser');
    mic.level = 0.5;
    await act(async () => {
      await vi.advanceTimersByTimeAsync(50);
    });
    expect(screen.getByTestId('voice').style.getPropertyValue('--mn-ai-voice-mode-level')).toBe(
      '0.5',
    );
    act(() => r.session.stop());
    expect(screen.getByTestId('voice').style.getPropertyValue('--mn-ai-voice-mode-level')).toBe(
      '0',
    );
  });

  it('mutes and unmutes the microphone track', async () => {
    const r = rig();
    await r.up();
    r.session.setMuted(true);
    expect(r.track.enabled).toBe(false);
    expect(r.session.state).toBe('muted');
    expect(r.events).toContainEqual({ type: 'muted', muted: true });
    r.session.setMuted(false);
    expect(r.track.enabled).toBe(true);
    expect(r.session.state).toBe('listening');
    r.session.stop();
  });

  it('interrupt silences playback, tells the service and listens again', async () => {
    const r = rig({
      mapEvent,
      encodeInterrupt: () => JSON.stringify({ type: 'cancel' }),
      interruptHoldMs: 400,
    });
    await r.up();
    r.peer().ontrack?.({ streams: [{ id: 'remote' }], track: {} });
    r.peer().message({ kind: 'speaking' });
    r.session.interrupt();
    expect(r.element.muted).toBe(true);
    expect(r.peer().channel.sent).toEqual(['{"type":"cancel"}']);
    expect(r.session.state).toBe('listening');
    // The service starting a new reply brings the sound back at once.
    r.peer().message({ kind: 'speaking' });
    expect(r.element.muted).toBe(false);
    // Without word from the service, the hold ends by itself.
    r.session.interrupt();
    expect(r.element.muted).toBe(true);
    await vi.advanceTimersByTimeAsync(400);
    expect(r.element.muted).toBe(false);
    r.session.stop();
  });

  it('barge-in: the person talking over the assistant interrupts it', async () => {
    const r = rig({
      mapEvent,
      encodeInterrupt: () => 'stop',
      levelIntervalMs: 50,
      voiceActivity: { threshold: 0.05, attackMs: 100 },
    });
    await r.up();
    r.peer().ontrack?.({ streams: [{ id: 'remote' }], track: {} });
    r.peer().message({ kind: 'speaking' });
    const mic = r.analysers[0];
    if (!mic) throw new Error('no analyser');
    mic.level = 0.01;
    await vi.advanceTimersByTimeAsync(200);
    expect(r.session.state).toBe('speaking');
    mic.level = 0.3;
    await vi.advanceTimersByTimeAsync(200);
    expect(r.peer().channel.sent).toEqual(['stop']);
    expect(r.session.state).toBe('listening');
    r.session.stop();
  });

  it('does not barge in while the microphone is muted', async () => {
    const r = rig({ mapEvent, encodeInterrupt: () => 'stop', levelIntervalMs: 50 });
    await r.up();
    r.session.setMuted(true);
    r.peer().message({ kind: 'speaking' });
    const mic = r.analysers[0];
    if (!mic) throw new Error('no analyser');
    mic.level = 0.6;
    await vi.advanceTimersByTimeAsync(500);
    expect(r.peer().channel.sent).toEqual([]);
    expect(r.session.state).toBe('speaking');
    r.session.stop();
  });

  it('rebuilds a failed connection with backoff and keeps the microphone', async () => {
    const r = rig({ reconnect: { baseDelayMs: 100, maxDelayMs: 1000, jitter: 0 } });
    await r.up();
    r.peer().set('failed');
    expect(r.session.state).toBe('reconnecting');
    expect(r.peers[0]?.closed).toBe(true);
    expect(r.track.stop).not.toHaveBeenCalled();
    await vi.advanceTimersByTimeAsync(99);
    expect(r.peers).toHaveLength(1);
    await vi.advanceTimersByTimeAsync(1);
    expect(r.peers).toHaveLength(2);
    expect(r.connect).toHaveBeenCalledTimes(2);
    expect(r.getUserMedia).toHaveBeenCalledTimes(1);
    // The second attempt waits twice as long.
    r.peer().set('failed');
    await vi.advanceTimersByTimeAsync(199);
    expect(r.peers).toHaveLength(2);
    await vi.advanceTimersByTimeAsync(1);
    expect(r.peers).toHaveLength(3);
    r.peer().set('connected');
    expect(r.session.state).toBe('listening');
    r.session.stop();
  });

  it('waits for a disconnected connection to recover before rebuilding it', async () => {
    const r = rig({ reconnect: { graceMs: 1000, baseDelayMs: 100, jitter: 0 } });
    await r.up();
    r.peer().set('disconnected');
    expect(r.session.state).toBe('reconnecting');
    r.peer().set('connected');
    expect(r.session.state).toBe('listening');
    await vi.advanceTimersByTimeAsync(5000);
    expect(r.peers).toHaveLength(1);
    r.peer().set('disconnected');
    await vi.advanceTimersByTimeAsync(1100);
    expect(r.peers).toHaveLength(2);
    r.session.stop();
  });

  it('gives up with a network error after the last attempt', async () => {
    const r = rig({ reconnect: { maxAttempts: 2, baseDelayMs: 10, jitter: 0 } });
    await r.up();
    r.connect.mockRejectedValue(new Error('offline'));
    r.peer().set('failed');
    await vi.advanceTimersByTimeAsync(1000);
    expect(r.connect).toHaveBeenCalledTimes(3);
    expect(r.session.state).toBe('error');
    expect(r.events).toContainEqual({ type: 'error', code: 'network' });
    expect(r.track.stop).toHaveBeenCalled();
    expect(r.context.close).toHaveBeenCalled();
  });

  it('fails at once when reconnecting is turned off', async () => {
    const r = rig({ reconnect: false });
    await r.up();
    r.peer().set('failed');
    expect(r.session.state).toBe('error');
    expect(r.events).toContainEqual({ type: 'error', code: 'network' });
  });

  it('stop releases the microphone, the connection, playback and the audio context', async () => {
    const r = rig();
    await r.up();
    r.peer().ontrack?.({ streams: [{ id: 'remote' }], track: {} });
    const signal = r.connect.mock.calls[0]?.[1];
    r.session.stop();
    expect(r.session.state).toBe('ended');
    expect(r.track.stop).toHaveBeenCalledTimes(1);
    expect(r.peer().closed).toBe(true);
    expect(r.peer().channel.close).toHaveBeenCalled();
    expect(r.element.pause).toHaveBeenCalled();
    expect(r.element.srcObject).toBeNull();
    expect(r.context.close).toHaveBeenCalled();
    expect(r.sources.every((s) => s.disconnect.mock.calls.length === 1)).toBe(true);
    expect(signal?.aborted).toBe(true);
    const count = r.events.length;
    await vi.advanceTimersByTimeAsync(1000);
    expect(r.events).toHaveLength(count);
    expect(r.session.send('late')).toBe(false);
  });

  it('stopping while the microphone is still opening releases it when it arrives', async () => {
    const r = rig();
    r.session.start();
    r.session.stop();
    await r.settle();
    expect(r.track.stop).toHaveBeenCalled();
    expect(r.peers).toHaveLength(0);
    expect(r.session.state).toBe('ended');
  });

  it('ends when the caller aborts', async () => {
    const controller = new AbortController();
    const r = rig({ signal: controller.signal });
    await r.up();
    controller.abort();
    expect(r.session.state).toBe('ended');
    expect(r.track.stop).toHaveBeenCalled();
  });

  it('reports a blocked microphone, a failed first connection and a missing browser API', async () => {
    const blocked = rig();
    blocked.getUserMedia.mockRejectedValue(
      Object.assign(new Error('denied'), { name: 'NotAllowedError' }),
    );
    blocked.session.start();
    await blocked.settle();
    expect(blocked.session.state).toBe('error');
    expect(blocked.events).toContainEqual({ type: 'error', code: 'not-allowed' });

    const refused = rig();
    refused.connect.mockRejectedValue(new Error('503'));
    refused.session.start();
    await refused.settle();
    expect(refused.events).toContainEqual({ type: 'error', code: 'network' });
    expect(refused.track.stop).toHaveBeenCalled();

    const events: AiVoiceSessionEvent[] = [];
    const bare = createRealtimeVoiceSession({ connect: async (offer) => offer });
    bare.subscribe((event) => events.push(event));
    bare.start();
    await vi.advanceTimersByTimeAsync(0);
    expect(events).toContainEqual({ type: 'error', code: 'not-supported' });
  });

  it('speak sends the encoded text, and without a mapper the remote loudness sets the state', async () => {
    const r = rig({ encodeText: (text) => `say:${text}`, levelIntervalMs: 50, bargeIn: false });
    await r.up();
    r.session.speak('Hello');
    expect(r.peer().channel.sent).toEqual(['say:Hello']);
    r.peer().ontrack?.({ streams: [{ id: 'remote' }], track: {} });
    const remote = r.analysers[1];
    if (!remote) throw new Error('no analyser');
    remote.level = 0.3;
    await vi.advanceTimersByTimeAsync(200);
    expect(r.session.state).toBe('speaking');
    remote.level = 0;
    await vi.advanceTimersByTimeAsync(1000);
    expect(r.session.state).toBe('listening');
    r.session.stop();
  });
});

describe('voice audio helpers', () => {
  it('measures level from floats, PCM16 and bytes', () => {
    expect(measureAudioLevel(new Float32Array(0))).toBe(0);
    expect(measureAudioLevel(new Float32Array([0.5, -0.5, 0.5, -0.5]))).toBeCloseTo(0.5);
    expect(measureAudioLevel(new Int16Array([16384, -16384]))).toBeCloseTo(0.5);
    expect(measureAudioLevel(new Uint8Array([128, 128]))).toBe(0);
    expect(measureAudioLevel(new Uint8Array([192, 64]))).toBeCloseTo(0.5);
    expect(measureAudioLevel([Number.NaN])).toBe(0);
  });

  it('detects speech after the attack time and its end after the release time', () => {
    const onSpeechStart = vi.fn();
    const onSpeechEnd = vi.fn();
    const vad = createVoiceActivityDetector({
      threshold: 0.05,
      attackMs: 100,
      releaseMs: 300,
      onSpeechStart,
      onSpeechEnd,
    });
    expect(vad.process(0.01, 0)).toBe(false);
    // A click shorter than the attack time is not speech.
    expect(vad.process(0.4, 50)).toBe(false);
    expect(vad.process(0.01, 80)).toBe(false);
    expect(vad.process(0.4, 100)).toBe(false);
    expect(vad.process(0.4, 200)).toBe(true);
    expect(onSpeechStart).toHaveBeenCalledTimes(1);
    // A short pause between words keeps it going.
    expect(vad.process(0.01, 300)).toBe(true);
    expect(vad.process(0.4, 400)).toBe(true);
    expect(vad.process(0.01, 500)).toBe(true);
    expect(vad.process(0.01, 800)).toBe(false);
    expect(onSpeechEnd).toHaveBeenCalledTimes(1);
    expect(vad.speaking).toBe(false);
  });

  it('raises its threshold in a noisy room', () => {
    const vad = createVoiceActivityDetector({ threshold: 0.02, ratio: 3, attackMs: 0 });
    for (let t = 0; t < 2000; t += 20) vad.process(0.05, t);
    expect(vad.noiseFloor).toBeGreaterThan(0.03);
    // Louder than the fixed threshold, but not three times the room noise.
    expect(vad.speaking).toBe(false);
    expect(vad.process(0.4, 2000)).toBe(true);
    vad.reset();
    expect(vad.speaking).toBe(false);
    expect(vad.noiseFloor).toBe(0);
  });

  it('decodes PCM16 from base64 and converts it to floats', () => {
    const bytes = new Uint8Array(new Int16Array([0, 16384, -32768]).buffer);
    const base64 = btoa(String.fromCharCode(...bytes));
    expect(Array.from(decodeBase64Pcm16(base64))).toEqual([0, 16384, -32768]);
    expect(Array.from(pcm16ToFloat32(new Int16Array([0, 16384, -32768])))).toEqual([0, 0.5, -1]);
  });

  function fakeContext() {
    const sources: {
      buffer: { duration: number } | null;
      connect: ReturnType<typeof vi.fn>;
      disconnect: ReturnType<typeof vi.fn>;
      start: ReturnType<typeof vi.fn>;
      stop: ReturnType<typeof vi.fn>;
      onended: (() => void) | null;
    }[] = [];
    const written: Float32Array[] = [];
    const context = {
      currentTime: 0,
      state: 'running' as AudioContextState,
      destination: {} as AudioDestinationNode,
      close: vi.fn(async () => {}),
      resume: vi.fn(async () => {}),
      createBuffer: (channels: number, frames: number, rate: number) => {
        const data = Array.from({ length: channels }, () => new Float32Array(frames));
        written.push(...data);
        return {
          duration: frames / rate,
          getChannelData: (channel: number) => data[channel] as Float32Array,
        } as unknown as AudioBuffer;
      },
      createBufferSource: () => {
        const source = {
          buffer: null,
          connect: vi.fn(),
          disconnect: vi.fn(),
          start: vi.fn(),
          stop: vi.fn(),
          onended: null,
        };
        sources.push(source);
        return source as unknown as AudioBufferSourceNode;
      },
    };
    return { context, sources, written };
  }

  it('schedules chunks back to back with no gap and reports the level of what plays', () => {
    const f = fakeContext();
    const onDrain = vi.fn();
    const player = createPcmAudioPlayer({
      sampleRate: 1000,
      leadMs: 0,
      onDrain,
      createAudioContext: () => f.context,
    });
    expect(player.playing).toBe(false);
    player.enqueue(new Int16Array(500).fill(16384));
    player.enqueue(new Float32Array(250).fill(0.25));
    player.enqueue(new Uint8Array(new Int16Array(250).fill(-32768).buffer));
    expect(f.sources.map((s) => s.start.mock.calls[0]?.[0])).toEqual([0, 0.5, 0.75]);
    expect(f.written[0]?.[0]).toBeCloseTo(0.5);
    expect(f.written[2]?.[0]).toBe(-1);
    expect(player.playing).toBe(true);
    expect(player.bufferedMs).toBeCloseTo(1000);
    expect(player.level).toBeCloseTo(0.5);
    f.context.currentTime = 0.6;
    expect(player.level).toBeCloseTo(0.25);
    expect(player.bufferedMs).toBeCloseTo(400);

    f.sources[0]?.onended?.();
    f.sources[1]?.onended?.();
    expect(onDrain).not.toHaveBeenCalled();
    f.sources[2]?.onended?.();
    expect(onDrain).toHaveBeenCalledTimes(1);
    expect(player.playing).toBe(false);
    // After a pause the next chunk starts from now, not from the old end.
    f.context.currentTime = 5;
    player.enqueue(new Int16Array(100));
    expect(f.sources[3]?.start.mock.calls[0]?.[0]).toBe(5);
    player.stop();
  });

  it('flush stops everything at once without draining, and stop closes its own context', () => {
    const f = fakeContext();
    const onDrain = vi.fn();
    const player = createPcmAudioPlayer({
      sampleRate: 1000,
      onDrain,
      createAudioContext: () => f.context,
    });
    player.enqueue(new Int16Array(500));
    player.enqueue(new Int16Array(500));
    player.flush();
    expect(f.sources.every((s) => s.stop.mock.calls.length === 1)).toBe(true);
    expect(f.sources.every((s) => s.onended === null)).toBe(true);
    expect(player.playing).toBe(false);
    expect(player.bufferedMs).toBe(0);
    expect(onDrain).not.toHaveBeenCalled();
    player.enqueue(new Int16Array(100));
    expect(player.playing).toBe(true);
    player.stop();
    expect(f.context.close).toHaveBeenCalledTimes(1);
    player.enqueue(new Int16Array(100));
    expect(f.sources).toHaveLength(3);
  });

  it('splits interleaved channels, leaves a shared context open and does nothing without one', () => {
    const f = fakeContext();
    const player = createPcmAudioPlayer({
      sampleRate: 1000,
      channels: 2,
      audioContext: f.context,
    });
    player.enqueue(new Float32Array([0.1, 0.9, 0.2, 0.8]));
    expect(Array.from(f.written[0] ?? []).map((v) => Math.round(v * 10))).toEqual([1, 2]);
    expect(Array.from(f.written[1] ?? []).map((v) => Math.round(v * 10))).toEqual([9, 8]);
    player.stop();
    expect(f.context.close).not.toHaveBeenCalled();

    const silent = createPcmAudioPlayer({
      createAudioContext: () => null as unknown as AiAudioContextLike,
    });
    silent.enqueue(new Int16Array(10));
    expect(silent.playing).toBe(false);
    expect(silent.level).toBe(0);
    silent.stop();
  });
});
