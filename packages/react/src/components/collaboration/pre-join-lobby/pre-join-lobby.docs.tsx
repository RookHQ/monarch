import { useState } from 'react';
import {
  Button,
  Dialog,
  DialogBody,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from '@rookcom/monarch-react';
import { PreJoinLobby, type MediaDeviceOption } from '@rookcom/monarch-react/collaboration';
import type { ComponentDocs } from '../../../internal/component-docs.js';
// The scenario of the primary example, shared with iOS and Android (CROSS-PLATFORM.md section 8).
import fixture from './pre-join-lobby.fixture.json';

const devices: MediaDeviceOption[] = [
  {
    deviceId: fixture.data.microphone.id,
    kind: 'audioinput',
    label: fixture.data.microphone.label,
  },
  { deviceId: fixture.data.camera.id, kind: 'videoinput', label: fixture.data.camera.label },
];

/**
 * A steady level, standing in for a real microphone in the docs. A level that
 * moved on a timer would restart the meter's transition forever, so the
 * preview would never settle (screenshots, axe) and would distract readers.
 */
const DEMO_LEVEL = 0.4;

function Lobby({ onDone, inDialog = false }: { onDone?: () => void; inDialog?: boolean }) {
  const [micOn, setMicOn] = useState(fixture.data.micOn);
  const [cameraOn, setCameraOn] = useState(fixture.data.cameraOn);
  const [mic, setMic] = useState<string | null>(fixture.data.microphone.id);
  const [cam, setCam] = useState<string | null>(fixture.data.camera.id);
  const [joining, setJoining] = useState(false);
  const level = micOn ? DEMO_LEVEL : 0;
  return (
    <PreJoinLobby
      // In a dialog the dialog title names it; a second region with the same name would only
      // repeat it (and clash with the inline example's region on this page).
      title={inDialog ? null : fixture.data.title}
      micOn={micOn}
      onMicOnChange={setMicOn}
      cameraOn={cameraOn}
      onCameraOnChange={setCameraOn}
      audioLevel={level}
      devicePicker={{
        devices,
        audioInputId: mic,
        onAudioInputChange: setMic,
        videoInputId: cam,
        onVideoInputChange: setCam,
      }}
      participants={fixture.data.participants.map((name) => ({ name }))}
      joining={joining}
      onJoin={() => {
        setJoining(true);
        window.setTimeout(() => {
          setJoining(false);
          onDone?.();
        }, 1200);
      }}
      onCancel={onDone}
    />
  );
}

function InDialog() {
  const [open, setOpen] = useState(true);
  return (
    <Dialog open={open} onOpenChange={setOpen}>
      <DialogTrigger asChild>
        <Button variant="primary">{fixture.data.trigger}</Button>
      </DialogTrigger>
      <DialogContent size="md">
        <DialogHeader>
          <DialogTitle>{fixture.data.title}</DialogTitle>
        </DialogHeader>
        <DialogBody>
          <Lobby inDialog onDone={() => setOpen(false)} />
        </DialogBody>
      </DialogContent>
    </Dialog>
  );
}

function AudioOnlyWithError() {
  const [micOn, setMicOn] = useState(true);
  return (
    <PreJoinLobby
      title="Start a call"
      joinLabel="Start call"
      micOn={micOn}
      onMicOnChange={setMicOn}
      audioLevel={0}
      error="Microphone access is blocked. Allow it in the browser’s site settings, then try again."
      onJoin={() => {}}
      onCancel={() => {}}
    />
  );
}

export default {
  id: 'pre-join-lobby',
  title: 'PreJoinLobby',
  summary:
    'The pre-join screen: camera preview, mic level, device choice and join muted or with camera off.',
  sections: {
    purpose: {
      summary:
        'Lets people check how they look and sound and choose their devices before anyone else sees or hears them, then join with the microphone or camera off if they prefer. It takes the preview stream and callbacks; it never captures media itself. Needed by: Chat.',
      useWhen: ['Before starting or joining an audio or video call.'],
      avoidWhen: [
        'Answering a ringing call: use IncomingCall (answering is already a deliberate choice).',
        'Changing devices during a call: show DevicePicker from CallControls.',
      ],
    },
    designPrinciples: {
      items: [
        'Nothing is shared until Join: the preview is local only.',
        'Hand the same stream to the call on join, so the browser does not ask for access twice.',
        'Show the microphone working (a level meter), not just a toggle.',
        'Joining shows progress on the button; a failure is the product’s `error`.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        '`PreJoinLobby` renders a `<section>` named by its heading and forwards its ref there. Place it in a Dialog or on a page.',
        '`useAudioLevel(stream, enabled)` measures a stream’s microphone loudness (0–1) with Web Audio; the lobby uses it when `audioLevel` is not given.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        '`micOn`/`onMicOnChange` and `cameraOn`/`onCameraOnChange` are controlled; leave out `onCameraOnChange` for audio-only calls.',
        '`devicePicker` takes DevicePicker props and renders it in the compact size.',
        'Migrating from the app copy (Rook Chat `PreJoinLobby`): the Dialog and `useCallState().lobby` stay in the product (wrap the lobby in DialogContent); `captureLocal` stays in the product and its result → `stream`; local `micOn`/`cameraOn` state → `micOn`/`onMicOnChange`, `cameraOn`/`onCameraOnChange`; `LevelMeter` → built-in `useAudioLevel` or `audioLevel`; `devices` state → `devicePicker`; `target.call.participants` + member lookup → `participants`; `error` text → `error`; `calls.join`/`calls.start` → `onJoin({ micOn, cameraOn })`; `calls.closeLobby` → `onCancel`; `joining` unchanged; the "Start call" label → `joinLabel`; `readPrefs`/`writePrefs` stay in the product.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'onCameraOnChange',
          values: ['undefined', 'function'],
          description: 'Without it the camera toggle is hidden (audio calls).',
        },
      ],
    },
    states: {
      items: [
        { name: 'Camera off / starting / no preview', description: 'The preview area says which.' },
        {
          name: 'Muted',
          attribute: 'aria-pressed="true"',
          description: 'The mic toggle reads "Join with mic on" and the meter is hidden.',
        },
        {
          name: 'Error',
          attribute: 'role="alert"',
          description: 'The product’s message under the preview.',
        },
        {
          name: 'Joining',
          attribute: 'data-joining, aria-busy',
          description: 'The join button shows a spinner; Cancel is disabled.',
        },
      ],
    },
    interaction: {
      items: [
        'Toggles call `onMicOnChange` / `onCameraOnChange`; the product re-captures the preview if needed.',
        'Join calls `onJoin({ micOn, cameraOn })`; Cancel calls `onCancel`.',
        'Changing a device in the picker calls the picker callbacks; re-capture with the new id.',
      ],
    },
    keyboard: {
      items: [
        { keys: 'Tab', action: 'Mic toggle, camera toggle, device selects, Cancel, Join.' },
        { keys: 'Enter / Space', action: 'Activates the focused control.' },
      ],
    },
    accessibility: {
      role: 'region (section)',
      name: 'The heading (`title`, default "Join the call")',
      items: [
        'The preview is a group named "Your camera preview".',
        'The microphone level is a `role="meter"` named "Microphone level" with a percentage.',
        'Toggles say what joining will do ("Join muted") and use `aria-pressed`.',
      ],
    },
    responsive: {
      items: [
        'The preview keeps 16:9 at any width; toggles stay centred over it.',
        'Below 30rem Cancel and Join share the row equally; all controls are 44px.',
      ],
    },
    theme: {
      items: [
        'The preview sits on `--mn-color-bg-emphasis`; the meter is a success-toned Progress meter.',
        'Errors use danger subtle, border and strong tokens. Forced colours add borders.',
      ],
    },
    examples: {
      items: [
        { title: 'In a dialog', stage: { align: 'center' }, render: () => <InDialog /> },
        {
          title: 'Inline, with devices and people already in the call',
          render: () => <Lobby />,
          stack: true,
        },
        {
          title: 'Audio call with an access error',
          render: () => <AudioOnlyWithError />,
          stack: true,
        },
      ],
    },
    doDont: {
      do: [
        'Stop the preview tracks if the person cancels.',
        'Remember "join muted" and "camera off" choices for next time in the product.',
      ],
      dont: [
        'Do not start sending media before Join.',
        'Do not open a second getUserMedia on join; reuse the preview stream.',
      ],
    },
    related: {
      items: [
        { id: 'device-picker', reason: 'The device selects under the preview.' },
        { id: 'call-stage', reason: 'What follows after joining.' },
        { id: 'incoming-call', reason: 'For calls that ring rather than being joined.' },
        { id: 'dialog', reason: 'The usual container for the lobby.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: [
        'button',
        'progress-bar',
        'avatar',
        'call-controls',
        'call-tile',
        'device-picker',
      ],
    },
    testing: {
      unit: [
        'Mirrored preview with the stream attached, a named meter with its value, and toggles; axe passes.',
        'Join reports the chosen settings; the meter hides when muted; camera off text.',
        'Keyboard order through toggles and Cancel.',
        'Audio calls hide the camera toggle; error alert; custom title and join label.',
        'People already in the call and an embedded DevicePicker.',
        'Joining shows a busy button and disables Cancel; renders on the server.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
