import { useEffect, useState } from 'react';
import { Button } from '@rookcom/monarch-react';
import { AiVoiceMode, type AiVoiceCaption, type AiVoiceModeState } from '@rookcom/monarch-react/ai';
import type { ComponentDocs } from '../../../internal/component-docs.js';
// The scenario of the primary example, shared with iOS and Android (CROSS-PLATFORM.md section 8).
import fixture from './ai-voice-mode.fixture.json';

const AGENT = { name: fixture.data.agent.name, role: 'Scheduling assistant' };

const CAPTIONS: AiVoiceCaption[] = fixture.data.captions.map((caption) => ({
  id: caption.id,
  speaker: caption.speaker as AiVoiceCaption['speaker'],
  text: caption.text,
  final: caption.state === 'done',
}));

/** One step of the scripted call: the state, how long it lasts and the captions by then. */
interface Beat {
  state: AiVoiceModeState;
  ms: number;
  captions: AiVoiceCaption[];
}

const SCRIPT: Beat[] = [
  { state: 'connecting', ms: 1200, captions: [] },
  { state: 'listening', ms: 1500, captions: [] },
  {
    state: 'listening',
    ms: 1500,
    captions: [{ id: 'a', speaker: 'user', text: 'What is on my calendar', final: false }],
  },
  {
    state: 'thinking',
    ms: 1500,
    captions: [
      { id: 'a', speaker: 'user', text: 'What is on my calendar this afternoon?', final: true },
    ],
  },
  {
    state: 'speaking',
    ms: 1500,
    captions: [
      { id: 'a', speaker: 'user', text: 'What is on my calendar this afternoon?', final: true },
      { id: 'b', speaker: 'assistant', text: 'You have two meetings:', final: false },
    ],
  },
  {
    state: 'speaking',
    ms: 2000,
    captions: [
      { id: 'a', speaker: 'user', text: 'What is on my calendar this afternoon?', final: true },
      {
        id: 'b',
        speaker: 'assistant',
        text: 'You have two meetings: the design review at two, and a planning call at four.',
        final: true,
      },
    ],
  },
];

const IDLE: Beat = { state: 'connecting', ms: 1200, captions: [] };

/** A scripted call: no microphone, no audio, no network. The page plays the product's part. */
function ScriptedCall({ size }: { size?: 'md' | 'compact' }) {
  const [step, setStep] = useState(-1);
  const [ended, setEnded] = useState(false);
  const [muted, setMuted] = useState(false);
  const [seconds, setSeconds] = useState(0);
  const [level, setLevel] = useState(0);
  const beat = SCRIPT[Math.min(Math.max(step, 0), SCRIPT.length - 1)] ?? IDLE;
  const running = step >= 0 && !ended;
  const atEnd = step >= SCRIPT.length - 1;
  const state: AiVoiceModeState = ended
    ? 'ended'
    : atEnd
      ? muted
        ? 'muted'
        : 'listening'
      : beat.state;

  useEffect(() => {
    if (!running || atEnd) return;
    const t = setTimeout(() => setStep((s) => s + 1), beat.ms);
    return () => clearTimeout(t);
  }, [running, atEnd, beat.ms, step]);

  useEffect(() => {
    if (!running) return;
    const clock = setInterval(() => setSeconds((s) => s + 1), 1000);
    // A stand-in for the level your audio pipeline measures.
    const meter = setInterval(() => setLevel((l) => (l > 0.6 ? 0.2 : l + 0.25)), 160);
    return () => {
      clearInterval(clock);
      clearInterval(meter);
    };
  }, [running]);

  if (step < 0) {
    return (
      <Button size="sm" onClick={() => setStep(0)}>
        Start the scripted call
      </Button>
    );
  }
  return (
    <AiVoiceMode
      state={state}
      muted={muted}
      level={level}
      agent={AGENT}
      captions={beat.captions}
      elapsedSeconds={seconds}
      {...(size ? { size } : {})}
      onMuteChange={setMuted}
      onInterrupt={() => setStep(SCRIPT.length - 1)}
      onEnd={() => setEnded(true)}
      onRestart={() => {
        setEnded(false);
        setMuted(false);
        setSeconds(0);
        setStep(0);
      }}
    />
  );
}

export default {
  id: 'ai-voice-mode',
  title: 'AiVoiceMode',
  summary:
    'A live spoken conversation with an assistant: state, captions, mute, interrupt and end.',
  sections: {
    purpose: {
      summary:
        'The surface for a live, two-way spoken conversation with an AI assistant. It shows whose turn it is in shape and text, captions what both sides say, and gives the person explicit controls to mute, interrupt, hide captions and end. It is UI only: your product owns the microphone, the audio and the connection, and tells the component what state the call is in.',
      useWhen: [
        'People talk with an assistant and it talks back, turn by turn, over a live connection your product manages.',
        'You can report the call state and, ideally, an audio level and captions for both sides.',
        'Voice sits inside an assistant panel: use `size="compact"`.',
      ],
      avoidWhen: [
        'One-way dictation into a prompt: use AiVoiceInput.',
        'A typed conversation: use AiConversation or AiThread.',
        'A call between people: this has no participants, video or screen sharing.',
      ],
    },
    designPrinciples: {
      items: [
        'The state is always in words under the visual; the visual changes shape as well as colour (bars, halo, turning arc, struck-through microphone, error mark).',
        'Calm by default: one visual, moved only by the audio level you pass, and still under reduced motion.',
        'Nothing happens without a press. The component never opens a microphone, plays audio or connects; ending is a deliberate button, never Escape.',
        'Captions are on by default because they are an accessibility need, and only settled sentences are announced.',
        'Controls appear only while they can act: Interrupt while the assistant thinks or speaks, Mute and End while the call is live.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'The ref goes to the root `<section>`; native attributes pass through.',
        '`level` is written to the `--mn-ai-voice-mode-level` custom property on the root, clamped to 0..1, and only while `listening` or `speaking`. Updating it re-renders the component, so 10 to 20 updates a second is plenty.',
        'Mute is controlled: `onMuteChange(next)` asks, and you answer with `state="muted"` or the `muted` prop. Use `muted` when the microphone stays off while the assistant thinks or speaks.',
        'Keep a caption’s `id` the same while it is refined and when it turns `final`.',
        '`formatAiVoiceDuration(seconds, locale)` is exported for showing the same clock elsewhere.',
        'A control is rendered only when its callback is given (`onMuteChange`, `onInterrupt`, `onEnd`, `onRestart`, `onRetry`). The Captions toggle always shows while the call is live.',
        '`createRealtimeVoiceSession(options)` is an `AiVoiceSession` over WebRTC for any service. You give it the signalling, `connect(offer, signal)`, which sends the offer to your service and returns the answer; optional `iceServers`; `audio` constraints for the microphone; `mapEvent(raw)`, which turns a data-channel message into session events (transcripts, `thinking`, `speaking`, `listening`); and `encodeInterrupt()`, the message that tells the service to stop its reply. It plays the remote audio track through an audio element, mutes by disabling the microphone track, and returns `send(message)` for your own data-channel messages.',
        'Levels are measured, not guessed: an analyser on the microphone while `listening` and one on the remote audio while `speaking`, reported as `level` events with `source: "input" | "output"` every `levelIntervalMs` (80 by default). `useAiVoiceSession` passes them to `level`, and sets the level to 0 in every other state.',
        'Barge-in (`bargeIn`, on by default): a voice-activity detector on the microphone calls `interrupt()` when the person starts talking while the assistant speaks. `interrupt()` silences playback at once, sends `encodeInterrupt()` and returns to `listening`; the sound comes back when the service reports `speaking` again or after `interruptHoldMs`.',
        'A dropped connection is rebuilt with exponential backoff and jitter (`reconnect`: `maxAttempts` 5, `baseDelayMs` 500, `maxDelayMs` 8000, `graceMs` 3000) while the state is `reconnecting`; the microphone stays open and `connect` is called again. After the last attempt the session fails with the `network` error. `stop()` and an aborted `signal` stop the microphone tracks, close the data channel, the peer connection and the audio context, and abort the signal given to `connect`.',
        '`createPcmAudioPlayer({ sampleRate, channels, onDrain })` plays audio that arrives in chunks rather than as a media track (PCM16 or float samples; `decodeBase64Pcm16` reads the base64 form): `enqueue(chunk)` schedules each chunk exactly where the last one ends, `flush()` silences it at once for barge-in, `stop()` also closes its audio context, and `level`, `playing` and `bufferedMs` report on it.',
        '`createVoiceActivityDetector({ threshold, ratio, attackMs, releaseMs })` is the detector barge-in uses, on loudness alone: feed it `process(level, nowMs)` with levels from `measureAudioLevel(samples)`. It follows the room noise, and it cannot tell a voice from another loud sound.',
        'For tests and other runtimes every browser API can be replaced: `createPeerConnection`, `getUserMedia`, `createAudioContext`, `createAudioElement` and `random` (jitter).',
      ],
    },
    typedProperties: { source: 'typescript' },
    variants: {
      items: [
        {
          prop: 'size',
          values: ['md', 'compact'],
          description:
            '`md` centres a large visual above the text. `compact` puts a small visual beside the text, with small buttons and a shorter captions log, for an assistant panel.',
        },
        {
          prop: 'state',
          values: [
            'connecting',
            'listening',
            'thinking',
            'speaking',
            'muted',
            'reconnecting',
            'ended',
            'error',
          ],
          description: 'Sets the visual, the state text and which controls show.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Call state',
          attribute: 'data-state',
          description:
            'The current `state`. `aria-busy="true"` while connecting, thinking or reconnecting.',
        },
        {
          name: 'Muted',
          attribute: 'data-muted; aria-pressed on the Mute button',
          description:
            'The microphone is off. In a state other than `muted`, "Your microphone is muted" shows beside the state text.',
        },
        {
          name: 'Captions',
          attribute: 'data-captions="on|off"; aria-pressed on the Captions button',
          description: 'Whether the captions log shows.',
        },
        {
          name: 'Interim caption',
          attribute: 'data-final="false" (on the caption)',
          description: 'Words that may still change: italic, quieter, hidden from the live log.',
        },
        { name: 'Size', attribute: 'data-size', description: '`md` or `compact`.' },
        {
          name: 'Ended',
          description:
            'Shows "Conversation ended", the duration when `elapsedSeconds` is given, what was said, and Start again when `onRestart` is given.',
        },
        {
          name: 'Error',
          description: 'Shows the `error` text and Retry when `onRetry` is given.',
        },
      ],
    },
    interaction: {
      items: [
        'Mute calls `onMuteChange(!muted)`. It only asks: mute the microphone, then report it back.',
        'Interrupt calls `onInterrupt`; stop the assistant’s audio and reply there. When the button goes away while focused, focus moves to Mute.',
        'Captions shows or hides the log (`captionsVisible`, `defaultCaptionsVisible`, `onCaptionsVisibleChange`).',
        'End calls `onEnd`. Nothing else ends the call.',
        'The captions log follows the newest line until the person scrolls up, and follows again once they return to the end.',
        'Start again and Retry call `onRestart` and `onRetry`.',
      ],
    },
    keyboard: {
      items: [
        {
          keys: 'Tab',
          action: 'Moves through the captions log, Mute, Interrupt, Captions and End.',
        },
        { keys: 'Enter / Space', action: 'Activates the focused button.' },
        {
          keys: 'Space',
          action:
            'Toggles mute when focus is inside the component but not on a control (for example on the captions log, or on the surface after a click). Holding it down toggles once.',
        },
        { keys: 'Arrow keys / Page Up / Page Down', action: 'Scroll the focused captions log.' },
        { keys: 'Escape', action: 'Does nothing here: ending a call must be deliberate.' },
      ],
    },
    accessibility: {
      role: 'group (a named `<section>`)',
      name: '"Voice conversation with <assistant name>" (`messages.label`)',
      items: [
        'The state is visible text, not only the visual; the visual is decorative (`aria-hidden`).',
        'State changes are announced once each through a polite `status` region. The first state is not announced, and neither are flips between Listening and Speaking: the audio itself says whose turn it is.',
        'Captions are a `role="log"` with `aria-live="polite"`. Interim captions are hidden from assistive technology and updated in place; each caption is added to the log, and so announced, once, when it becomes final.',
        'Every caption names its speaker in text ("You" or the assistant’s name).',
        'Mute and Captions are toggle buttons with `aria-pressed` and a name that does not change, so a screen reader hears "Mute, pressed" rather than a contradictory "Unmute, pressed".',
        'The captions log is focusable so it can be scrolled from the keyboard.',
        'The clock is a `<time>` with a visually hidden "Call time" prefix; it is not a live region.',
        'Under `prefers-reduced-motion` the visual keeps its shape per state but does not move, whatever the level.',
        'Buttons are at least 24px, and 44px on coarse pointers.',
      ],
    },
    responsive: {
      items: [
        'Controls wrap; at 30rem and below (screen or `mn-ai` container) they share the width and the visual shrinks.',
        'Captions wrap long words and scroll inside a capped height; the page never scrolls sideways.',
        'RTL: logical properties throughout.',
      ],
    },
    theme: {
      items: [
        'Visual: `--mn-color-ai-subtle`, `--mn-color-ai-border`, `--mn-color-ai-strong`; filled with `--mn-color-ai-solid` / `--mn-color-ai-on-solid` while speaking; neutral tokens when muted or ended; danger tokens on error.',
        'Pressed toggles use `--mn-color-ai-subtle` / `--mn-color-ai-strong`; End is a danger Button.',
        'Forced colours: CanvasText outlines and shapes, Highlight on pressed toggles.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Listening, with captions',
          stack: true,
          render: () => (
            <AiVoiceMode
              state={fixture.data.state as AiVoiceModeState}
              level={fixture.data.level}
              agent={AGENT}
              captions={CAPTIONS}
              elapsedSeconds={fixture.data.elapsedSeconds}
              onMuteChange={() => {}}
              onInterrupt={() => {}}
              onEnd={() => {}}
            />
          ),
        },
        {
          title: 'Speaking: Interrupt appears',
          stack: true,
          render: () => (
            <AiVoiceMode
              state="speaking"
              level={0.8}
              agent={AGENT}
              captions={CAPTIONS.slice(0, 2)}
              elapsedSeconds={51}
              onMuteChange={() => {}}
              onInterrupt={() => {}}
              onEnd={() => {}}
            />
          ),
        },
        {
          title: 'Compact, thinking',
          stack: true,
          render: () => (
            <AiVoiceMode
              size="compact"
              state="thinking"
              agent={AGENT}
              captions={CAPTIONS.slice(0, 1)}
              elapsedSeconds={47}
              onMuteChange={() => {}}
              onInterrupt={() => {}}
              onEnd={() => {}}
            />
          ),
        },
        {
          title: 'Muted and reconnecting',
          stack: true,
          render: () => (
            <>
              <AiVoiceMode
                size="compact"
                state="muted"
                agent={AGENT}
                defaultCaptionsVisible={false}
                elapsedSeconds={75}
                onMuteChange={() => {}}
                onEnd={() => {}}
              />
              <AiVoiceMode
                size="compact"
                state="reconnecting"
                agent={AGENT}
                defaultCaptionsVisible={false}
                elapsedSeconds={80}
                onMuteChange={() => {}}
                onEnd={() => {}}
              />
            </>
          ),
        },
        {
          title: 'Ended and error',
          stack: true,
          render: () => (
            <>
              <AiVoiceMode state="ended" agent={AGENT} elapsedSeconds={245} onRestart={() => {}} />
              <AiVoiceMode
                state="error"
                agent={AGENT}
                error="The connection was lost and could not be restored."
                onRetry={() => {}}
              />
            </>
          ),
        },
        {
          title: 'Scripted call (no microphone needed)',
          stack: true,
          render: () => <ScriptedCall />,
        },
      ],
    },
    doDont: {
      do: [
        'Drive AiVoiceMode from an `AiVoiceSession`: `createBrowserVoiceSession` for a quick start, or your own realtime connection behind the same interface, and spread `useAiVoiceSession(session).props`.',
        'Tell people before they start that browser speech recognition may send their voice to a cloud service run by the browser maker (it does in some browsers); offer a realtime session through your own service where that matters.',
        'Privacy: with `createRealtimeVoiceSession` the microphone audio leaves the device and goes to the service you connect to for the whole call, not only while the person speaks. Say so, and name the service, before the button that starts the call; say whether audio or transcripts are kept; and mute by default where the product allows it.',
        'Recommend headphones, or keep the browser’s echo cancellation on (the default `audio` constraints), when barge-in is on: otherwise the assistant can hear and interrupt itself.',
        'Start the call from an explicit button in your product, and ask for the microphone only then.',
        'Send captions for both sides, interim and final, with stable ids.',
        'Report `reconnecting` as soon as audio stops flowing, so silence is never mistaken for thinking.',
        'Make Interrupt stop the audio at once, then move to `listening`.',
      ],
      dont: [
        'Do not start a voice session on page load: the microphone prompt must follow a click.',
        'Do not end the call on Escape, on blur or on a click outside.',
        'Do not hide the Captions toggle or turn captions off by default.',
        'Do not drive `level` from a timer when you have no real level; leave it out and the visual stays calm.',
        'Do not put service keys in the page for `connect`: send the offer to your own server, which talks to the voice service and returns the answer.',
        'Do not keep a session alive after the surface closes: call `stop()` (or abort its `signal`) so the microphone light goes out.',
        'Do not treat `ended` as a place to send the transcript anywhere without asking.',
      ],
    },
    related: {
      items: [
        { id: 'ai-voice-input', reason: 'One-way dictation into a prompt.' },
        { id: 'ai-assistant-panel', reason: 'Where the compact size sits.' },
        { id: 'ai-agent-avatar', reason: 'The assistant’s picture inside.' },
        { id: 'ai-thinking-indicator', reason: 'The thinking state of a typed reply.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: ['button', 'ai-agent-avatar', 'ai-voice-input'],
    },
    testing: {
      unit: [
        'Renders a named region with state text, clock and controls; ref, class and native props forwarding; text for all eight states; the level is clamped and only passed on while listening or speaking.',
        'Mute reports the wanted value with `aria-pressed`; the `muted` prop shows a muted microphone while the assistant speaks.',
        'Space toggles mute inside the surface but not on another control, on repeat, with modifiers or once the call is over; Escape never ends the call.',
        'Interrupt shows only while thinking or speaking, works from the keyboard, and hands focus to Mute when it goes away.',
        'Captions toggle controlled and uncontrolled; interim captions are hidden from the log and updated in place, final ones are added as new nodes; the log follows the newest line unless scrolled up.',
        'State changes are announced once, never the first state or listening/speaking flips; errors include their text.',
        'Ended shows the duration and Start again; error shows the text and Retry; compact size; translated strings and locale digits; `formatAiVoiceDuration`.',
        '`createBrowserVoiceSession` listens with interim and final transcripts, hands final turns to `onUserTurn`, speaks replies and stops speaking on barge-in or Interrupt; errors map to `not-supported` and `not-allowed`.',
        '`useAiVoiceSession` gives AiVoiceMode its state, captions, level, mute and handlers; Interrupt calls `session.interrupt`.',
        '`createRealtimeVoiceSession` with a fake peer connection, microphone, audio context and audio element: the offer goes to `connect` and the answer is applied; the remote track plays; data-channel messages become transcripts and states; levels come from the microphone and remote analysers and reach AiVoiceMode; mute disables the track; `interrupt` silences playback, sends `encodeInterrupt` and releases the hold; barge-in fires from the detector and never while muted; a failed or disconnected connection is rebuilt with backoff and gives up with `network`; `stop`, an aborted signal and a stop during start-up release everything; blocked microphone, failed first connection and missing browser API map to error codes; `speak` sends `encodeText`.',
        '`measureAudioLevel`, `createVoiceActivityDetector` (attack, release, noise floor, reset), `decodeBase64Pcm16`, `pcm16ToFloat32` and `createPcmAudioPlayer` (gapless start times, level, buffered time, `onDrain`, `flush`, `stop`, interleaved channels, shared context, no audio context).',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
