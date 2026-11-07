import { useState } from 'react';
import { DevicePicker, type MediaDeviceOption } from '@rookcom/monarch-react/collaboration';
import type { ComponentDocs } from '../../../internal/component-docs.js';

const sampleDevices: MediaDeviceOption[] = [
  { deviceId: 'mic-desk', kind: 'audioinput', label: 'Desk microphone' },
  { deviceId: 'mic-headset', kind: 'audioinput', label: 'Headset microphone' },
  { deviceId: 'cam-built-in', kind: 'videoinput', label: 'Built-in camera' },
  { deviceId: 'cam-usb', kind: 'videoinput', label: 'USB camera' },
  { deviceId: 'out-speakers', kind: 'audiooutput', label: 'Speakers' },
  { deviceId: 'out-headset', kind: 'audiooutput', label: 'Headset' },
];

function ThisBrowser() {
  const [mic, setMic] = useState<string | null>(null);
  const [cam, setCam] = useState<string | null>(null);
  const [out, setOut] = useState<string | null>(null);
  return (
    <div style={{ maxWidth: 360 }}>
      <DevicePicker
        audioInputId={mic}
        onAudioInputChange={setMic}
        videoInputId={cam}
        onVideoInputChange={setCam}
        audioOutputId={out}
        onAudioOutputChange={setOut}
      />
    </div>
  );
}

function GivenDevices() {
  const [mic, setMic] = useState<string | null>('mic-headset');
  const [cam, setCam] = useState<string | null>(null);
  const [out, setOut] = useState<string | null>(null);
  return (
    <div style={{ maxWidth: 320 }}>
      <DevicePicker
        size="sm"
        devices={sampleDevices}
        audioInputId={mic}
        onAudioInputChange={setMic}
        videoInputId={cam}
        onVideoInputChange={setCam}
        audioOutputId={out}
        onAudioOutputChange={setOut}
      />
    </div>
  );
}

function NoNamesYet() {
  return (
    <div style={{ maxWidth: 360 }}>
      <DevicePicker
        devices={[
          { deviceId: 'a', kind: 'audioinput', label: '' },
          { deviceId: 'b', kind: 'audioinput', label: '' },
        ]}
        audioInputId={null}
        onAudioInputChange={() => {}}
        onRequestAccess={() => new Promise((resolve) => window.setTimeout(resolve, 800))}
      />
    </div>
  );
}

export default {
  id: 'device-picker',
  title: 'DevicePicker',
  summary:
    'Microphone, camera and speaker selects that follow plugged-in hardware and explain permissions.',
  sections: {
    purpose: {
      summary:
        'Lets people choose which microphone, camera and speaker a call uses, before joining and during the call. It lists devices with `navigator.mediaDevices`, refreshes when hardware changes, and explains missing names and blocked permissions. Needed by: Chat.',
      useWhen: [
        'In PreJoinLobby (its `devicePicker` prop) and in a call’s Devices popover.',
        'Any recording or capture feature that should let people pick the input.',
      ],
      avoidWhen: [
        'Choosing between signed-in computers or phones: use DeviceList.',
        'Capturing media: the picker only chooses ids; the product opens the stream.',
      ],
    },
    designPrinciples: {
      items: [
        'Native selects: long device names and mobile pickers work everywhere.',
        '"System default" is always an option and is what an empty id means.',
        'Feature-detected and SSR-safe: nothing touches `navigator` until after hydration.',
        'Say why names are missing (access not allowed yet) and offer one button to fix it.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        '`DevicePicker` forwards its ref to the root `<div>`.',
        '`useMediaDevices({ enabled })` returns `{ supported, canSelectOutput, devices, permissions, labelsHidden, refresh, requestAccess }` for products that build their own UI.',
        '`requestAccess` opens the devices briefly with `getUserMedia` and stops them, so names appear; it rejects if the person refuses.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        'Each select shows only when its change callback is passed. The speaker select also needs browser support for `setSinkId`.',
        'Pass `devices` to list your own devices (tests, a native wrapper); the picker then does not enumerate.',
        'Migrating from the app copy (Rook Chat `DevicePicker`): `audioIn` / `onAudioIn` → `audioInputId` / `onAudioInputChange`; `videoIn` / `onVideoIn` → `videoInputId` / `onVideoInputChange`; `sinkId` / `onSink` → `audioOutputId` / `onAudioOutputChange`; `showCamera={false}` → leave out `onVideoInputChange`; the `deviceVersion` refresh from callManager is built in (`devicechange`).',
      ],
    },
    variants: {
      items: [
        {
          prop: 'size',
          values: ['md', 'sm'],
          description:
            '`sm` uses compact selects for a popover during a call. Exposed as `data-size`.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Names hidden',
          description: 'Devices are listed without names: an info notice with "Allow access".',
        },
        {
          name: 'Access refused',
          description:
            'The request failed: the notice says access wasn’t allowed; the button stays to try again.',
        },
        {
          name: 'Blocked',
          description:
            'The Permissions API reports `denied`: a warning explains how to allow it in site settings.',
        },
        { name: 'Unsupported', description: 'No `navigator.mediaDevices`: a neutral notice.' },
        {
          name: 'Disabled',
          attribute: 'disabled',
          description: 'Every select and the access button are disabled.',
        },
      ],
    },
    interaction: {
      items: [
        'Choosing an option calls the matching callback with the device id ("" for the system default).',
        'Plugging in or removing a device refreshes the lists.',
        'A selected id that is no longer present stays selected as an unnamed device, so the choice is never silently changed.',
      ],
    },
    keyboard: {
      items: [
        { keys: 'Tab', action: 'Moves between the selects and the Allow access button.' },
        { keys: 'Arrow keys / Alt+ArrowDown', action: 'Native select behaviour.' },
      ],
    },
    accessibility: {
      role: 'group of labelled selects (combobox)',
      name: 'Each select is labelled "Microphone", "Camera" or "Speaker" through Field',
      items: [
        'Unnamed devices are numbered ("Microphone 2") instead of blank.',
        'Permission notices are live, so they are announced when they appear.',
      ],
    },
    responsive: {
      items: [
        'Selects fill the width and stack; nothing scrolls sideways at 390px.',
        'Controls meet 44px on coarse pointers through NativeSelect.',
      ],
    },
    theme: {
      items: [
        'Colours come from Field, NativeSelect and InlineNotice, so all four themes and forced colours are handled there.',
      ],
    },
    examples: {
      items: [
        { title: 'This browser’s devices', render: () => <ThisBrowser /> },
        { title: 'Given devices, compact', render: () => <GivenDevices /> },
        { title: 'Before access is allowed', render: () => <NoNamesYet /> },
      ],
    },
    doDont: {
      do: [
        'Re-open the stream with the new device id in the product when a callback fires.',
        'Remember the choice per person (for example in local storage) and pass it back in.',
      ],
      dont: [
        'Do not ask for access on page load; let the person press Allow access or start a call.',
        'Do not hide the system default option.',
      ],
    },
    related: {
      items: [
        { id: 'pre-join-lobby', reason: 'Embeds the picker under the preview.' },
        {
          id: 'call-controls',
          reason: 'Its `onOpenDevices` is where the picker is shown during a call.',
        },
        { id: 'device-list', reason: 'Signed-in devices and sessions, not media hardware.' },
      ],
    },
    dependencies: { packages: ['react'], components: ['button', 'inline-notice', 'form-field'] },
    testing: {
      unit: [
        'Labelled selects with System default, given values and change callbacks; axe passes.',
        'Only selects with callbacks render.',
        'Unnamed devices are numbered; Allow access calls onRequestAccess; a refusal is reported.',
        'Enumerates browser devices and refreshes on devicechange (mocked navigator.mediaDevices).',
        'Default access request uses getUserMedia, stops the tracks and shows names.',
        'Unsupported browsers get a notice; renders on the server without navigator.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
