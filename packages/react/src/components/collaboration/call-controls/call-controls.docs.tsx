import { useState } from 'react';
import {
  CallControlButton,
  CallControls,
  CallIcon,
  type CallViewMode,
} from '@rookcom/monarch-react/collaboration';
import type { ComponentDocs } from '../../../internal/component-docs.js';

function FullToolbar() {
  const [micOn, setMicOn] = useState(true);
  const [cameraOn, setCameraOn] = useState(true);
  const [sharing, setSharing] = useState(false);
  const [hand, setHand] = useState(false);
  const [mode, setMode] = useState<CallViewMode>('auto');
  const [lowData, setLowData] = useState(false);
  const [fullscreen, setFullscreen] = useState(false);
  const [last, setLast] = useState('Nothing yet');
  return (
    <div style={{ display: 'grid', gap: 12 }}>
      <CallControls
        micOn={micOn}
        onMicChange={setMicOn}
        cameraOn={cameraOn}
        onCameraChange={setCameraOn}
        sharing={sharing}
        onSharingChange={setSharing}
        handRaised={hand}
        onHandRaisedChange={setHand}
        onReact={(emoji) => setLast(`Reacted ${emoji}`)}
        mode={mode}
        onModeChange={setMode}
        lowData={lowData}
        onLowDataChange={setLowData}
        onOpenDevices={() => setLast('Opened devices')}
        onOpenStats={() => setLast('Opened connection panel')}
        fullscreen={fullscreen}
        onFullscreenChange={setFullscreen}
        onLeave={() => setLast('Left the call')}
      />
      <p style={{ margin: 0, fontSize: 13 }}>
        Layout: {mode} · Last action: {last}
      </p>
    </div>
  );
}

function MinimalToolbar() {
  const [micOn, setMicOn] = useState(false);
  const [cameraOn, setCameraOn] = useState(false);
  return (
    <CallControls
      minimal
      micOn={micOn}
      onMicChange={setMicOn}
      cameraOn={cameraOn}
      onCameraChange={setCameraOn}
      onLeave={() => {}}
    />
  );
}

function WithExtra() {
  const [recording, setRecording] = useState(false);
  return (
    <CallControls
      micOn
      onMicChange={() => {}}
      extra={
        <CallControlButton
          label={recording ? 'Stop recording' : 'Start recording'}
          pressed={recording}
          tone={recording ? 'active' : 'default'}
          icon={<CallIcon name="activity" />}
          onClick={() => setRecording(!recording)}
        />
      }
      onLeave={() => {}}
    />
  );
}

export default {
  id: 'call-controls',
  title: 'CallControls',
  summary:
    'The call toolbar: mute, camera, share, hand, reactions, a More menu and Leave, plus the shared call button and icons.',
  sections: {
    purpose: {
      summary:
        'Gives every call surface the same controls with the same names, shortcuts and states. Each control appears only when the product passes its callback, so a one-to-one audio call and a video meeting use the same component. Needed by: Chat.',
      useWhen: [
        'Under a CallStage, in a picture-in-picture window (`minimal`), or anywhere a call needs its controls.',
        'A product-specific call action (record, captions): add a `CallControlButton` through `extra` so it matches.',
      ],
      avoidWhen: [
        'Media playback controls for recorded video: those are play/pause controls, not call controls.',
        'General page toolbars: use Button and IconButton.',
      ],
    },
    designPrinciples: {
      items: [
        'Fully controlled: the toolbar shows state and reports intent; the product decides what the call does.',
        'State is visible and announced: a switched-off device is red and `aria-pressed`, sharing is green, a raised hand is yellow.',
        'Leave is always last, red and wider, so it is never hit by accident while reaching for mute.',
        'Shortcuts are hints in the tooltip and are wired separately with `useCallShortcuts`, so they only fire inside the call.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        '`CallControls` renders a `role="toolbar"`; it forwards its ref and spreads other props to that element.',
        '`CallControlButton` is the 44px round icon button used by every call component; `label` is required.',
        '`CallIcon name="…"` draws the built-in call glyphs, so Monarch needs no icon package.',
        '`useCallShortcuts({ root, onToggleMic, onPushToTalk, onToggleCamera, onToggleShare, onToggleHand, onToggleFullscreen })` wires M, V, S, H, F, hold Space and Ctrl/Cmd+Shift+Space.',
        '`DEFAULT_CALL_REACTIONS` and `DEFAULT_CALL_CONTROLS_MESSAGES` are exported for reuse.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        'Each pair (`micOn` + `onMicChange`, `cameraOn` + `onCameraChange`, …) is controlled; leaving out the callback hides the control.',
        'Every visible string is in `messages` (`mute`, `unmute`, `layouts`, `reactWith(emoji)`, `leave`, …).',
        'Migrating from the app copy (Rook Chat `CallControls`): `active.micOn` → `micOn`, `calls.toggleMic()` → `onMicChange`; `active.cameraOn` / `toggleCamera` → `cameraOn` / `onCameraChange`; `active.sharing` / `startShare`/`stopShare` → `sharing` / `onSharingChange`; `active.hand` / `raiseHand` → `handRaised` / `onHandRaisedChange`; `calls.react` → `onReact`; `mode` / `onMode` → `mode` / `onModeChange`; `setLowData` → `onLowDataChange`; the internal devices and stats panels → `onOpenDevices` / `onOpenStats` (render DevicePicker and CallStatsPanel in your own Popover); `fullscreen` / `onFullscreen` → `fullscreen` / `onFullscreenChange`; `onPiP` → `onPictureInPicture`; `calls.leave` → `onLeave`; `minimal` is unchanged. `ControlButton` → `CallControlButton` (`on` → `pressed` + `tone`, `danger` → `tone="danger"`). Error toasts stay in the product.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'minimal',
          values: ['false', 'true'],
          description:
            'Hides hand, reactions and More, for a small window such as picture-in-picture where menus would open in the main window.',
        },
        {
          prop: 'CallControlButton tone',
          values: ['default', 'off', 'active', 'raised', 'danger'],
          description:
            'Exposed as `data-tone`: off is a switched-off device, active an action in progress, raised a hand, danger Leave.',
        },
        {
          prop: 'CallControlButton size',
          values: ['md', 'sm'],
          description: '`md` is 44px. `sm` is 36px with a fine pointer and 44px on touch.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Device off',
          attribute: 'aria-pressed="true", data-tone="off"',
          description: 'Microphone or camera is off; the label says how to turn it back on.',
        },
        {
          name: 'Sharing',
          attribute: 'aria-pressed="true", data-tone="active"',
          description: 'Screen sharing is on.',
        },
        {
          name: 'Hand raised',
          attribute: 'aria-pressed="true", data-tone="raised"',
          description: 'The hand is up.',
        },
        {
          name: 'Disabled',
          attribute: 'disabled',
          description: 'Any CallControlButton can be disabled, for example while joining.',
        },
      ],
    },
    interaction: {
      items: [
        'Clicking a toggle calls its callback with the next value; the label flips once the product updates the prop.',
        'Reactions open a menu of emoji above the toolbar; choosing one calls `onReact(emoji)`.',
        'More opens a menu with the layout as radio items, low data as a checkbox, and Devices, Connection, Full screen and Picture-in-picture as actions.',
        '`useCallShortcuts` adds single-key shortcuts that only fire while focus is inside the call root and never while typing.',
      ],
    },
    keyboard: {
      items: [
        { keys: 'Tab', action: 'Moves into and through the toolbar.' },
        { keys: 'ArrowLeft / ArrowRight', action: 'Moves between controls, wrapping at the ends.' },
        { keys: 'Home / End', action: 'First or last control.' },
        { keys: 'Enter / Space', action: 'Activates the focused control or opens its menu.' },
        {
          keys: 'M, V, S, H, F',
          action:
            'With useCallShortcuts and focus inside the call: mic, camera, share, hand, full screen.',
        },
        {
          keys: 'Hold Space',
          action:
            'With useCallShortcuts: push to talk while focus is on the stage, not on a control.',
        },
        {
          keys: 'Ctrl/Cmd+Shift+Space',
          action: 'With useCallShortcuts: toggles the mic from anywhere on the page.',
        },
      ],
    },
    accessibility: {
      role: 'toolbar',
      name: '`messages.toolbar` ("Call controls")',
      items: [
        'Every control is a native button with an accessible name; the icon is hidden from assistive technology.',
        'Toggles use `aria-pressed`, so screen readers say "Mute, toggle button, not pressed".',
        'The tooltip (`title`) repeats the name with its shortcut for pointer users.',
        'Single-key shortcuts are scoped to the call, meeting WCAG 2.1.4 (character key shortcuts).',
        'Menus are Monarch DropdownMenus with full keyboard support and focus return.',
      ],
    },
    responsive: {
      items: [
        'Controls wrap onto a second row on narrow screens; at 390px the full set fits in two rows.',
        'Buttons are 44px; `sm` buttons grow to 44px on coarse pointers.',
      ],
    },
    theme: {
      items: [
        'Default controls use `--mn-color-bg-muted`; off and Leave use the danger solid, sharing the success solid, the hand the warning solid.',
        'Focus uses `--mn-color-focus-ring`. In forced colours, buttons use system colours and pressed or Leave buttons get a Highlight border.',
      ],
    },
    examples: {
      items: [
        { title: 'Every control', render: () => <FullToolbar />, stack: true },
        { title: 'Minimal (picture-in-picture)', render: () => <MinimalToolbar /> },
        { title: 'A product-specific control', render: () => <WithExtra /> },
      ],
    },
    doDont: {
      do: [
        'Wire `useCallShortcuts` with the call root so the tooltips’ shortcuts work.',
        'Keep Leave last and use `extra` for product controls.',
        'Report failures (camera blocked, share cancelled) with a Toast from the product.',
      ],
      dont: [
        'Do not show a control whose action the product cannot do; leave its callback out.',
        'Do not attach single-key shortcuts to the whole page.',
        'Do not change the colours to mean something else: red always means off or leave.',
      ],
    },
    related: {
      items: [
        { id: 'call-stage', reason: 'The stage this toolbar sits under (its `controls` slot).' },
        {
          id: 'mini-call',
          reason: 'The minimised window, which uses CallControlButton for its own controls.',
        },
        { id: 'device-picker', reason: 'What the product shows from `onOpenDevices`.' },
        { id: 'call-stats-panel', reason: 'What the product shows from `onOpenStats`.' },
        { id: 'dropdown-menu', reason: 'The menus for reactions and More.' },
      ],
    },
    dependencies: { packages: ['react'], components: ['dropdown-menu'] },
    testing: {
      unit: [
        'Toggles flip labels, aria-pressed and tone; Leave is a danger control; axe passes.',
        'Only controls with callbacks render; minimal hides hand, reactions and More.',
        'Arrow keys, Home and End move focus and wrap.',
        'Reaction and More menus call onReact, onModeChange and onOpenDevices; the open menu passes axe.',
        'useCallShortcuts fires only with focus inside the root, never while typing; Ctrl+Shift+Space works anywhere; Space is push-to-talk.',
        'messages translate strings; renders on the server.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
