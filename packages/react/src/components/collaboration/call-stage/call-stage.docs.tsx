import { useState } from 'react';
import {
  CallControls,
  CallStage,
  type CallParticipant,
  type CallSurface,
  type CallViewMode,
} from '@rookcom/monarch-react/collaboration';
import type { ComponentDocs } from '../../../internal/component-docs.js';

const STARTED = Date.now() - 4 * 60_000;

const me: CallParticipant = { id: 'me', name: 'Sam Lee', self: true };
const robin: CallParticipant = { id: 'robin', name: 'Robin Park', speaking: true, quality: 'good' };
const team: CallParticipant[] = [
  me,
  robin,
  { id: 'alex', name: 'Alex Chen', micOff: true, quality: 'fair' },
  { id: 'jordan', name: 'Jordan Diaz', handRaised: true, quality: 'good' },
];

function Meeting() {
  const [micOn, setMicOn] = useState(true);
  const [mode, setMode] = useState<CallViewMode>('auto');
  const [pinnedId, setPinnedId] = useState<string | null>(null);
  const [surface, setSurface] = useState<CallSurface>('stage');
  const people = team.map((p) => (p.self ? { ...p, micOff: !micOn } : p));
  return (
    <div
      style={{
        height: surface === 'stage' ? 460 : 300,
        maxWidth: surface === 'stage' ? 'none' : 360,
      }}
    >
      <CallStage
        title="Weekly planning"
        startedAt={STARTED}
        participants={people}
        mode={mode}
        pinnedId={pinnedId}
        onPinnedIdChange={setPinnedId}
        activeSpeakerId="robin"
        surface={surface}
        onSurfaceChange={(next) => setSurface(next === 'mini' ? 'docked' : next)}
        controls={
          <CallControls
            micOn={micOn}
            onMicChange={setMicOn}
            mode={mode}
            onModeChange={setMode}
            minimal={surface !== 'stage'}
            onLeave={() => {}}
          />
        }
      />
    </div>
  );
}

function OneToOne() {
  return (
    <div style={{ height: 320 }}>
      <CallStage
        title="Robin Park"
        startedAt={STARTED}
        participants={[me, robin]}
        controls={<CallControls micOn onMicChange={() => {}} onLeave={() => {}} />}
      />
    </div>
  );
}

function Trouble() {
  return (
    <div style={{ height: 300 }}>
      <CallStage
        title="Support call"
        participants={[me, { id: 'x', name: 'Alex Chen', status: 'Connecting…' }]}
        layout={{ kind: 'grid', ids: ['me', 'x'], cols: 2 }}
        reconnecting
        speakingWhileMuted
        error="A connection couldn’t be established. Check your network, then leave and rejoin."
      />
    </div>
  );
}

export default {
  id: 'call-stage',
  title: 'CallStage',
  summary:
    'The call itself: header with timer, participant tiles in a grid or spotlight, notices and controls.',
  sections: {
    purpose: {
      summary:
        'Lays out everyone on a call, keeps the most important person large (a pin, a shared screen, the speaker or the other person in a one-to-one call) and surfaces connection problems in plain words. It takes MediaStreams and callbacks only; signalling stays in the product. Needed by: Chat.',
      useWhen: [
        'An active audio or video call, full window or docked in a corner.',
        'A picture-in-picture window (`surface="pip"`).',
      ],
      avoidWhen: [
        'A minimised call while people keep working: use MiniCall.',
        'Before joining: use PreJoinLobby.',
      ],
    },
    designPrinciples: {
      items: [
        'Layout is a pure function (`chooseCallLayout`) the product can reuse to decide how much video to request from each person.',
        'Someone else’s screen beats everything except a pin; your own screen never takes the spotlight from you.',
        'Problems are sentences, not icons: "You’re offline. Trying to reconnect…".',
        'The timer ticks without being announced; the head count is announced when it changes.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        '`CallStage` renders a `<section>` named by `title` (or "Call") and forwards its ref there.',
        '`chooseCallLayout({ selfId, ids, mode, pinnedId, sharerId, activeSpeakerId })` returns `{ kind: "grid", ids, cols }` or `{ kind: "spotlight", mainId, strip }`; `callGridColumns(n)` gives the grid width.',
        '`CallTimer since={…}` is the ticking duration (`role="timer"`); `formatCallDuration(ms)` formats it.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        '`participants` are CallTile props plus `id`; mark yourself with `self: true`.',
        'Pass `layout` to override the automatic layout entirely.',
        'Migrating from the app copy (Rook Chat `CallStage`): `active.peers` + member lookups → `participants` (resolve names, pictures, peer state text and quality in the product); `layout` keeps its shape (`kind`, `ids`/`cols`, `mainId`/`strip`) and is now optional; `mode`, `pinnedId`, `surface`, `title` keep their names; `onPin` → `onPinnedIdChange`; `onSurface` → `onSurfaceChange`; `onPiP` → `onPictureInPicture`; `onVideoEl` → `onVideoElement`; `active.call.startedAt` → `startedAt`; `active.lowData` → `lowData`; `active.reconnecting` → `reconnecting`; the failed-peer message → `error` (product copy); `active.speakingWhileMuted` → `speakingWhileMuted`; the built-in `CallControls` → the `controls` slot.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'surface',
          values: ['stage', 'docked', 'pip'],
          description:
            '`stage` fills its container; `docked` and `pip` are compact: at most two grid columns, no tile actions. Exposed as `data-surface`.',
        },
        {
          prop: 'mode',
          values: ['auto', 'grid', 'speaker'],
          description:
            'Automatic spotlights the other person in a one-to-one call; speaker spotlights the active speaker.',
        },
      ],
    },
    states: {
      items: [
        { name: 'Grid / spotlight', attribute: 'data-layout', description: 'The current layout.' },
        {
          name: 'Waiting',
          description: 'Alone on the call: the header says "Waiting for others…".',
        },
        {
          name: 'Reconnecting',
          attribute: 'role="status"',
          description: 'A warning notice while the connection recovers.',
        },
        {
          name: 'Error',
          attribute: 'role="alert"',
          description: 'A danger notice with the product’s explanation.',
        },
        {
          name: 'Muted while speaking',
          attribute: 'role="status"',
          description: 'A hint over the tiles.',
        },
      ],
    },
    interaction: {
      items: [
        'Pin a tile to keep it large; unpin to go back to the automatic layout.',
        'Shrink sends `docked`; from docked, Expand sends `stage` and Minimise sends `mini` (render MiniCall then).',
        'Picture-in-picture calls `onPictureInPicture`; use `onVideoElement` to get the `<video>` to open.',
        'On wide screens the filmstrip moves beside the main tile.',
      ],
    },
    keyboard: {
      items: [
        {
          keys: 'Tab',
          action: 'Header buttons, then each tile’s Pin and Full screen, then the controls.',
        },
        { keys: 'Enter / Space', action: 'Activates the focused button.' },
      ],
      notes: [
        'Call shortcuts (M, V, S, H, F, hold Space) come from `useCallShortcuts` in CallControls, scoped to this region.',
      ],
    },
    accessibility: {
      role: 'region (section)',
      name: '`title`, or `messages.region` ("Call")',
      items: [
        'The title is a level-2 heading inside the region.',
        'The head count is a polite live region; the timer is `role="timer"` so it is not read every second.',
        'Notices use `status` and `alert` roles.',
        'Header buttons are icon buttons with names and tooltips.',
      ],
    },
    responsive: {
      items: [
        'Below 30rem the grid never uses more than two columns; the filmstrip scrolls sideways.',
        'From 64rem the stage puts the filmstrip in a column beside the main tile.',
        'Header buttons are 44px on coarse pointers.',
      ],
    },
    theme: {
      items: [
        'The stage sits on `--mn-color-bg-subtle`; tiles on `--mn-color-bg-emphasis`.',
        'Notices use warning and danger subtle backgrounds with their border and strong text tokens.',
        'Forced colours: a border around the stage and system colours for notices.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Group call with pin, layout and docking',
          render: () => <Meeting />,
          stack: true,
        },
        {
          title: 'One-to-one (the other person is large)',
          render: () => <OneToOne />,
          stack: true,
        },
        { title: 'Connection problems', render: () => <Trouble />, stack: true },
      ],
    },
    doDont: {
      do: [
        'Give the stage a height; it fills its container.',
        'Keep layout logic in `chooseCallLayout` so what you request matches what you show.',
        'Write `error` in the product’s words, with what the person can do.',
      ],
      dont: [
        'Do not put signalling or media capture in the stage; pass streams in.',
        'Do not announce the timer.',
      ],
    },
    related: {
      items: [
        { id: 'call-tile', reason: 'Each participant on the stage.' },
        { id: 'call-controls', reason: 'The toolbar for the `controls` slot.' },
        { id: 'mini-call', reason: 'The minimised form of the call.' },
        { id: 'pre-join-lobby', reason: 'The screen before the stage.' },
      ],
    },
    dependencies: { packages: ['react'], components: ['call-controls', 'call-tile'] },
    testing: {
      unit: [
        'chooseCallLayout: grid by default, pin then remote screen then speaker spotlight, one-to-one spotlight; grid columns.',
        'formatCallDuration and a ticking, named CallTimer.',
        'Titled region with every participant and controls; axe passes; generic name and waiting text when alone.',
        'Pinning from a tile; surface buttons; compact surfaces hide tile actions.',
        'Reconnecting, error, muted-speech and low-data notices; video elements reach onVideoElement.',
        'Renders on the server with 00:00.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
