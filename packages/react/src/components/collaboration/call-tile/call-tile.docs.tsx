import { useState } from 'react';
import { Button } from '@rookcom/monarch-react';
import { CallQualityBars, CallTile } from '@rookcom/monarch-react/collaboration';
import type { ComponentDocs } from '../../../internal/component-docs.js';

const box = { width: 280, height: 160 };

function PinnableTile() {
  const [pinned, setPinned] = useState(false);
  return (
    <div style={box}>
      <CallTile name="Robin Park" pinned={pinned} onPinnedChange={setPinned} quality="good" />
    </div>
  );
}

function ReactingTile() {
  const [reaction, setReaction] = useState<{ emoji: string; at: number } | null>(null);
  return (
    <div style={{ display: 'grid', gap: 8 }}>
      <div style={box}>
        <CallTile name="Alex Chen" speaking reaction={reaction} />
      </div>
      <Button onClick={() => setReaction({ emoji: '👏', at: Date.now() })}>Send a reaction</Button>
    </div>
  );
}

export default {
  id: 'call-tile',
  title: 'CallTile',
  summary: 'One call participant: video or avatar, name, mic, hand, connection, reactions and pin.',
  sections: {
    purpose: {
      summary:
        'Shows one person in a call with everything others need to know at a glance: are they on camera, muted, talking, presenting, connected well, waiting to speak. Needed by: Chat.',
      useWhen: [
        'Inside CallStage and MiniCall (they render it for you).',
        'A custom call layout that still needs consistent participant tiles.',
      ],
      avoidWhen: [
        'A list of people in a meeting without media: use MemberRow or Avatar.',
        'Playing a recorded video: use a `<video>` with controls.',
      ],
    },
    designPrinciples: {
      items: [
        'Pictures only: the `<video>` is always muted, so audio plays from one place and muting a tile never mutes the person.',
        'Status is text first: "Muted", "Poor connection" and "raised a hand" are accessible names, not just colours.',
        'Your own camera is mirrored, a shared screen never is, and screens letterbox instead of cropping.',
        'Actions appear on hover and focus, and stay visible on touch screens.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        '`CallTile` forwards its ref to the root `role="group"` element named after the participant.',
        '`CallVideo` is the muted, inline `<video>` that attaches a `MediaStream` as `srcObject` after mount; use it for previews.',
        '`CallQualityBars` draws the three-bar connection grade with an accessible label.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        '`showVideo` defaults to true when `stream` has a live video track; pass false while connecting or in low data mode.',
        '`fullscreen` is `true` (Fullscreen API on the tile), `false`, or your own handler.',
        '`reaction` is `{ emoji, at }`; a new `at` replays it and reactions older than four seconds are ignored.',
        'Migrating from the app copy (Rook Chat `CallTile`): `name`, `avatarUrl`, `stream`, `showVideo`, `self`, `micOff`, `speaking`, `screen`, `status`, `quality`, `reaction`, `pinned`, `size` and `videoRef` keep their names; `hand` → `handRaised`; `statusTone="warn" | "error"` → `"warning" | "danger"`; `onPin()` → `onPinnedChange(next)`; `VideoView` → `CallVideo`; `QualityBars grade` → `CallQualityBars quality`. Member lookups stay in the product: pass the resolved name and picture.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'size',
          values: ['small', 'normal', 'large'],
          description:
            '`small` is the filmstrip size (16:9, smaller avatar, no actions). Exposed as `data-size`.',
        },
        {
          prop: 'statusTone',
          values: ['neutral', 'warning', 'danger'],
          description: 'Colour of the status text in the name bar.',
        },
        {
          prop: 'quality',
          values: ['good', 'fair', 'poor', 'unknown'],
          description: 'Connection bars; `unknown` shows none.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Video',
          attribute: 'data-video',
          description: 'Showing the stream instead of the avatar.',
        },
        {
          name: 'Speaking',
          attribute: 'data-speaking',
          description: 'A success-coloured ring around the tile (or the avatar).',
        },
        {
          name: 'Pinned',
          attribute: 'data-pinned',
          description: 'Pinned to the main spot; the pin button reads "Unpin" and stays visible.',
        },
        {
          name: 'Screen',
          attribute: 'data-screen',
          description: 'Showing a shared screen, letterboxed.',
        },
      ],
    },
    interaction: {
      items: [
        'Pin toggles with `onPinnedChange(!pinned)`.',
        'Full screen uses the Fullscreen API on the tile (or your handler); pressing it again while full screen exits.',
        'A reaction floats up for three seconds and is announced politely.',
      ],
    },
    keyboard: {
      items: [
        {
          keys: 'Tab',
          action: 'Reaches the Pin and Full screen buttons, which become visible on focus.',
        },
        { keys: 'Enter / Space', action: 'Activates the focused button.' },
      ],
    },
    accessibility: {
      role: 'group',
      name: 'The participant’s name ("Sam (you)" for yourself)',
      items: [
        'Muted, sharing, hand and connection icons are `role="img"` with names.',
        'Pin is a toggle (`aria-pressed`) named "Pin Robin Park" so several tiles stay distinguishable.',
        'Reactions are announced through a polite live region; the floating emoji itself is hidden.',
        'The avatar is decorative because the group already carries the name.',
      ],
    },
    responsive: {
      items: [
        'Fills its container; `small` keeps 16:9.',
        'The name truncates with an ellipsis; status and bars never wrap.',
        'Action buttons are 44px and always visible on coarse pointers.',
      ],
    },
    theme: {
      items: [
        'The tile uses `--mn-color-bg-emphasis`; the name bar and actions sit on a translucent `--mn-color-bg-inverse` so they read over any video.',
        'Speaking uses `--mn-color-success-solid`; bars use success, warning and danger solids.',
        'Forced colours: borders and system colours, a Highlight border while speaking. Reduced motion: no transitions and no floating animation.',
      ],
    },
    examples: {
      items: [
        {
          title: 'States',
          render: () => (
            <div
              style={{
                display: 'grid',
                gridTemplateColumns: 'repeat(auto-fill, minmax(200px, 1fr))',
                gap: 8,
              }}
            >
              <div style={{ height: 130 }}>
                <CallTile name="Sam Lee" self micOff />
              </div>
              <div style={{ height: 130 }}>
                <CallTile name="Robin Park" speaking quality="good" />
              </div>
              <div style={{ height: 130 }}>
                <CallTile name="Alex Chen" handRaised quality="fair" />
              </div>
              <div style={{ height: 130 }}>
                <CallTile
                  name="Jordan Diaz"
                  status="Reconnecting…"
                  statusTone="warning"
                  quality="poor"
                />
              </div>
            </div>
          ),
          stack: true,
        },
        { title: 'Pin and full screen', render: () => <PinnableTile /> },
        { title: 'Reactions', render: () => <ReactingTile /> },
        {
          title: 'Connection bars',
          render: () => (
            <div style={{ display: 'flex', gap: 16 }}>
              <CallQualityBars quality="good" />
              <CallQualityBars quality="fair" />
              <CallQualityBars quality="poor" />
            </div>
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Play remote audio from a separate audio element; tiles are always muted.',
        'Pass `showVideo={false}` until the peer is connected, so a frozen frame is never shown.',
        'Give every tile the person’s real name; it names the tile and its buttons.',
      ],
      dont: [
        'Do not rely on the speaking ring alone to say who is talking; it is a visual hint.',
        'Do not mirror shared screens or other people’s cameras.',
      ],
    },
    related: {
      items: [
        { id: 'call-stage', reason: 'Lays tiles out in a grid or spotlight.' },
        { id: 'mini-call', reason: 'A single tile in a small floating window.' },
        { id: 'avatar', reason: 'What the tile shows when there is no video.' },
      ],
    },
    dependencies: { packages: ['react'], components: ['avatar', 'call-controls'] },
    testing: {
      unit: [
        'Avatar without video, named group, muted and quality icons, status tone; axe passes.',
        'Attaches the stream as srcObject, muted; mirrors your camera; letterboxes and never mirrors a screen.',
        'showVideo={false} wins over a live stream.',
        'Pin is keyboard reachable, toggles aria-pressed and reports the next state; custom full-screen handler; small tiles have no actions.',
        'Raised hand is named; recent reactions are announced and cleared; stale ones are ignored.',
        'messages translate strings; CallVideo forwards its ref; renders on the server.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
