import { useState } from 'react';
import { Button } from '@rookcom/monarch-react';
import { MiniCall } from '@rookcom/monarch-react/collaboration';
import type { ComponentDocs } from '../../../internal/component-docs.js';

const STARTED = Date.now() - 12 * 60_000;

function Inline() {
  const [micOn, setMicOn] = useState(true);
  const [last, setLast] = useState('');
  return (
    <div style={{ display: 'grid', gap: 8 }}>
      <MiniCall
        placement="inline"
        participant={{ id: 'robin', name: 'Robin Park', speaking: true, quality: 'good' }}
        startedAt={STARTED}
        count={4}
        micOn={micOn}
        onMicChange={setMicOn}
        onExpand={() => setLast('Expanded')}
        onLeave={() => setLast('Left the call')}
      />
      {last ? <p style={{ margin: 0, fontSize: 13 }}>{last}</p> : null}
    </div>
  );
}

function Floating() {
  const [open, setOpen] = useState(false);
  const [position, setPosition] = useState({ right: 24, bottom: 24 });
  return (
    <>
      <Button onClick={() => setOpen(!open)}>
        {open ? 'Close the mini call' : 'Show a floating mini call'}
      </Button>
      {open && (
        <MiniCall
          participant={{ id: 'alex', name: 'Alex Chen', micOff: true }}
          startedAt={STARTED}
          count={2}
          micOn={false}
          onMicChange={() => {}}
          onExpand={() => setOpen(false)}
          onLeave={() => setOpen(false)}
          position={position}
          onPositionChange={setPosition}
        />
      )}
    </>
  );
}

export default {
  id: 'mini-call',
  title: 'MiniCall',
  summary:
    'A small draggable call window showing whoever is talking, with timer, mute, expand and leave.',
  sections: {
    purpose: {
      summary:
        'Keeps a call going in a corner while people carry on working: it shows the active speaker and the essentials, and can be moved out of the way with a pointer or the keyboard. Needed by: Chat.',
      useWhen: ['The person minimises a CallStage (its `onSurfaceChange("mini")`).'],
      avoidWhen: [
        'The call is the main task: use CallStage.',
        'Picture-in-picture windows: use CallStage with `surface="pip"`.',
      ],
    },
    designPrinciples: {
      items: [
        'Only what matters while working: who is talking, how long, mute, back to the call, leave.',
        'Dragging has a keyboard equivalent: the Move handle and arrow keys.',
        'It never leaves the screen: the position is clamped on drag and on resize.',
        'The product remembers the position; the component reports it with `onPositionChange`.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        '`MiniCall` renders a `role="region"` and forwards its ref there. In `floating` placement it is `position: fixed` using `right` and `bottom`.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        '`participant` has the CallStage participant shape (`id`, `name`, `stream`, …).',
        '`position` is controlled; `defaultPosition` sets the start when uncontrolled. Both are distances from the right and bottom edges.',
        'Migrating from the app copy (Rook Chat `MiniCall`): `active` → `participant` (pick the active speaker, or the first peer, in the product), `startedAt` (from `active.call.startedAt`), `count` (`peers.length + 1`) and `micOn`; `calls.toggleMic()` → `onMicChange`; `onExpand` is unchanged; `calls.leave()` → `onLeave`; `readPrefs().miniPos` / `writePrefs` → `defaultPosition` / `onPositionChange`.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'placement',
          values: ['floating', 'inline'],
          description:
            '`floating` is fixed and draggable; `inline` renders in place without a handle. Exposed as `data-placement`.',
        },
      ],
    },
    states: {
      items: [
        { name: 'Waiting', description: 'No participant yet: "Waiting for others…".' },
        {
          name: 'Dragging',
          attribute: 'data-dragging',
          description: 'While a pointer drag is in progress.',
        },
        {
          name: 'Muted',
          attribute: 'aria-pressed="true"',
          description: 'The mute button reads "Unmute" and turns red.',
        },
      ],
    },
    interaction: {
      items: [
        'Drag anywhere except a button to move it; the position is committed on release.',
        'Double-click (outside the buttons) expands the call.',
        'Mute, Expand and Leave call their callbacks.',
      ],
    },
    keyboard: {
      items: [
        { keys: 'Tab', action: 'Move handle, then Mute, Expand and Leave.' },
        {
          keys: 'Arrow keys (on the Move handle)',
          action: 'Moves the window 24px in that direction, kept on screen.',
        },
        { keys: 'Enter / Space', action: 'Activates the focused button.' },
      ],
    },
    accessibility: {
      role: 'region',
      name: '`messages.region` ("Call (minimised)")',
      items: [
        'The Move handle is a button named "Move call window. Use the arrow keys."',
        'The speaker’s tile is a group named after them, with mute and connection status.',
        'The timer is `role="timer"`, so it is not read every second.',
      ],
    },
    responsive: {
      items: [
        '18rem wide, never wider than the viewport minus 1.5rem. Buttons and the handle are 44px on coarse pointers.',
      ],
    },
    theme: {
      items: [
        'Raised surface with `--mn-shadow-xl` at `--mn-z-overlay`.',
        'Forced colours: a CanvasText border and a Highlight focus outline on the handle.',
      ],
    },
    examples: {
      items: [
        { title: 'Inline', render: () => <Inline /> },
        { title: 'Floating and draggable', render: () => <Floating /> },
      ],
    },
    doDont: {
      do: [
        'Show the active speaker, falling back to the first other person.',
        'Store the position per person so it stays where they put it.',
      ],
      dont: [
        'Do not show more than one mini call.',
        'Do not cover the product’s own primary actions by default; start in the bottom-right corner.',
      ],
    },
    related: {
      items: [
        { id: 'call-stage', reason: 'The full call this minimises.' },
        { id: 'call-tile', reason: 'The speaker view inside.' },
        { id: 'call-controls', reason: 'Source of CallControlButton and CallIcon.' },
      ],
    },
    dependencies: { packages: ['react'], components: ['call-controls', 'call-stage', 'call-tile'] },
    testing: {
      unit: [
        'Named region with the speaker tile, timer, count and working controls; axe passes.',
        'Waiting text and no handle when inline.',
        'Arrow keys on the handle move it and report the clamped position.',
        'Pointer drag shows data-dragging and commits on release.',
        'Double-click expands, but not from its buttons; renders on the server.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
