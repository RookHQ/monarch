import { useState } from 'react';
import {
  AiVoiceInput,
  type AiSpeechRecognitionLike,
  type AiSpeechResult,
} from '@rookcom/monarch-react/ai';
import type { ComponentDocs } from '../../../internal/component-docs.js';

/** A scripted recognizer so the demo works in every browser, with no microphone. */
function scriptedRecognition(): AiSpeechRecognitionLike {
  const words = 'Summarise the open support tickets from this week'.split(' ');
  const timers: ReturnType<typeof setTimeout>[] = [];
  const rec: AiSpeechRecognitionLike = {
    lang: '',
    continuous: false,
    interimResults: false,
    onresult: null,
    onerror: null,
    onend: null,
    start() {
      words.forEach((_, i) => {
        timers.push(
          setTimeout(
            () => {
              const last = i === words.length - 1;
              const result = [
                { transcript: words.slice(0, i + 1).join(' ') },
              ] as unknown as AiSpeechResult;
              result.isFinal = last;
              rec.onresult?.({ resultIndex: 0, results: [result] });
              if (last) rec.stop();
            },
            250 * (i + 1),
          ),
        );
      });
    },
    stop() {
      timers.forEach(clearTimeout);
      rec.onend?.();
    },
    abort() {
      timers.forEach(clearTimeout);
    },
  };
  return rec;
}

function Composer({ scripted }: { scripted?: boolean }) {
  const [text, setText] = useState('');
  return (
    <div style={{ display: 'grid', gap: 8, maxWidth: 480 }}>
      <textarea
        aria-label="Prompt"
        rows={3}
        value={text}
        onChange={(e) => setText(e.target.value)}
        style={{ font: 'inherit', padding: 8 }}
      />
      <AiVoiceInput
        levelMeter={!scripted}
        createRecognition={scripted ? scriptedRecognition : undefined}
        onTranscript={(chunk, { final }) => {
          if (final) setText((t) => (t ? `${t} ${chunk}` : chunk));
        }}
      />
    </div>
  );
}

export default {
  id: 'ai-voice-input',
  title: 'AiVoiceInput',
  summary:
    'Dictate a prompt with the Web Speech API: toggle or hold to talk, level meter, permission guidance.',
  sections: {
    purpose: {
      summary:
        'Lets people speak a prompt instead of typing it. Recognised words appear as they are heard and are handed to the composer; nothing is ever sent automatically, so people can review and edit before sending.',
      useWhen: [
        'Next to AiPromptInput (in its toolbar) where speaking is faster than typing: on phones, or for long requests.',
        'Hands-busy contexts such as field work, where push-to-talk is natural.',
      ],
      avoidWhen: [
        'Voice conversations with spoken replies: this only dictates text.',
        'Recording audio files to upload: use FileUpload or a recorder.',
      ],
    },
    designPrinciples: {
      items: [
        'Never auto-send: dictation fills the prompt, people send.',
        'Hidden where unsupported, rather than a button that cannot work.',
        'Permission problems come with a fix (“Allow it in your browser’s site settings”), not a dead end.',
        'Motion is optional: the level meter is static under reduced motion.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'The ref goes to the root `<div>`; native attributes pass through.',
        '`createRecognition` accepts any object matching `AiSpeechRecognitionLike`, e.g. a server-backed recognizer for browsers without the Web Speech API.',
        '`getSpeechRecognition()` returns the browser constructor or null, for feature checks elsewhere.',
        'The level meter uses `getUserMedia` and an `AnalyserNode`; it stops the microphone tracks when listening ends or the component unmounts.',
      ],
    },
    typedProperties: { source: 'typescript' },
    variants: {
      items: [
        {
          prop: 'mode',
          values: ['toggle', 'push-to-talk'],
          description:
            '`toggle` (default): press to start, press again (or Escape) to stop; continuous recognition. `push-to-talk`: listens while the button (or Space/Enter) is held. Exposed as `data-mode`.',
        },
        {
          prop: 'size',
          values: ['sm', 'md'],
          description: 'Button size; `sm` fits a prompt toolbar.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'State',
          attribute: 'data-state',
          description:
            '`idle`, `starting` (waiting for the microphone and permission), `listening`, `denied` (guidance shown), `error`.',
        },
        {
          name: 'Pressed',
          attribute: 'aria-pressed',
          description: '`true` while starting or listening.',
        },
        {
          name: 'Animated',
          attribute: 'data-animated',
          description: 'Present while the live level meter runs (never under reduced motion).',
        },
      ],
    },
    interaction: {
      items: [
        'Interim words show under the button and reach `onTranscript` with `final: false`; each finished phrase arrives once with `final: true`.',
        'Recognition ends on its own after silence in some browsers; the button returns to idle and “Stopped listening.” is announced.',
        'Errors stop listening: blocked microphone shows guidance; “no speech” and “no microphone” have their own messages.',
      ],
    },
    keyboard: {
      items: [
        { keys: 'Enter / Space', action: 'Toggle mode: starts or stops dictation.' },
        { keys: 'Hold Space / Enter', action: 'Push-to-talk: listens while held.' },
        { keys: 'Escape', action: 'Stops listening while focus is in the component.' },
      ],
    },
    accessibility: {
      role: 'button (toggle, aria-pressed)',
      name: '“Dictate” or “Hold to talk”; described by “Press Escape to stop.” while listening',
      items: [
        'Start and stop are announced politely once each; the transcript is not a live region, so words are not read one by one.',
        'Blocked access is an `alert`; other problems are a polite `status`.',
        'The level meter is decorative (`aria-hidden`); the visible “Listening…” text carries the state.',
        'The transcript preview has a visually hidden “Transcript:” prefix.',
      ],
    },
    responsive: {
      items: [
        'Controls wrap; the transcript wraps long words. Works at 390px.',
        'Button is 44px on coarse pointers; push-to-talk disables touch panning on the button so holding works.',
        'Logical properties for RTL.',
      ],
    },
    theme: {
      items: [
        'Listening: button in `--mn-color-ai-solid` / `--mn-color-ai-on-solid`; meter bars `--mn-color-ai-strong`.',
        'Transcript: dashed `--mn-color-ai-border` on `--mn-color-ai-subtle`.',
        'Blocked guidance: warning tokens.',
        'Forced colours: Highlight button while listening, CanvasText bars.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Scripted demo (works without a microphone)',
          stack: true,
          render: () => <Composer scripted />,
        },
        {
          title: 'Your browser’s speech recognition (hidden where unsupported)',
          stack: true,
          render: () => <Composer />,
        },
        {
          title: 'Push-to-talk',
          render: () => (
            <AiVoiceInput
              mode="push-to-talk"
              createRecognition={scriptedRecognition}
              levelMeter={false}
              onTranscript={() => {}}
            />
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Put final text into the prompt and keep focus where people expect it.',
        'Set `lang` to the language people are likely to speak.',
      ],
      dont: [
        'Do not send the prompt when dictation ends.',
        'Do not start listening without a press.',
      ],
    },
    related: {
      items: [
        { id: 'ai-prompt-input', reason: 'The composer it fills.' },
        { id: 'ai-prompt-toolbar', reason: 'Where the button usually sits.' },
        { id: 'ai-stop-button', reason: 'Stopping a streaming answer.' },
      ],
    },
    dependencies: { packages: ['react'], components: ['button'] },
    testing: {
      unit: [
        'Hidden without a recognizer; uses the prefixed browser constructor when present.',
        'Toggle start/stop, interim and final transcripts, state sequence and announcements.',
        'Escape stops; push-to-talk listens only while Space is held.',
        'Waits for the microphone before listening and stops its tracks on unmount; blocked access shows an alert; other errors are polite.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
