import { useEffect, useState } from 'react';
import { Button } from '@rookcom/monarch-react';
import { ScreenShareView } from '@rookcom/monarch-react/collaboration';
import type { ComponentDocs } from '../../../internal/component-docs.js';
// The scenario of the primary example, shared with iOS and Android (CROSS-PLATFORM.md section 8).
import fixture from './screen-share-view.fixture.json';

/** Draws a fake slide on a canvas and shares it as a stream, so the example needs no permission. */
function useDemoStream() {
  const [stream, setStream] = useState<MediaStream | null>(null);
  useEffect(() => {
    const canvas = document.createElement('canvas');
    canvas.width = 1280;
    canvas.height = 720;
    const ctx = canvas.getContext('2d');
    if (!ctx || typeof canvas.captureStream !== 'function') return;
    ctx.fillStyle = 'white';
    ctx.fillRect(0, 0, 1280, 720);
    ctx.fillStyle = 'black';
    ctx.font = '600 56px sans-serif';
    ctx.fillText(fixture.data.slide.title, 80, 140);
    ctx.font = '32px sans-serif';
    fixture.data.slide.bullets.forEach((line, i) => ctx.fillText(`• ${line}`, 100, 260 + i * 70));
    const s = canvas.captureStream(1);
    setStream(s);
    return () => s.getTracks().forEach((t) => t.stop());
  }, []);
  return stream;
}

function RemoteShare() {
  const stream = useDemoStream();
  return (
    <div style={{ width: '100%', maxWidth: 640 }}>
      <ScreenShareView stream={stream} presenterName={fixture.data.presenter} />
    </div>
  );
}

function LocalShare() {
  const stream = useDemoStream();
  const [sharing, setSharing] = useState(true);
  return (
    <div style={{ width: '100%', maxWidth: 640 }}>
      {sharing ? (
        <ScreenShareView
          stream={stream}
          presenterName="You"
          isLocal
          onStopSharing={() => setSharing(false)}
        />
      ) : (
        <Button size="sm" onClick={() => setSharing(true)}>
          Share again
        </Button>
      )}
    </div>
  );
}

export default {
  id: 'screen-share-view',
  title: 'ScreenShareView',
  summary:
    'A shared screen with the presenter’s name, fit and zoom, picture-in-picture and full screen.',
  sections: {
    purpose: {
      summary:
        'Shows someone’s shared screen large enough to read, says who is presenting, and lets viewers zoom into detail, pop it out into picture-in-picture or go full screen. Needed by: Rook Chat calls (`components/calls/CallStage.tsx`, `CallTile.tsx` with `screen`, `lib/calls/media.ts` `captureScreen`).',
      useWhen: [
        'A call or meeting shows a participant’s shared screen.',
        'You are presenting and need a clear “you are presenting” state with Stop sharing.',
      ],
      avoidWhen: [
        'Camera tiles: use CallTile.',
        'Recorded video playback: use a video player with captions.',
      ],
    },
    designPrinciples: {
      items: [
        'Fit first: the whole screen is visible; zooming makes the view scroll so detail can be read and panned.',
        'Browser features are detected after mount; controls the browser cannot do are not shown.',
        'Your own share is hidden by default to avoid the mirror tunnel, with an explicit way to look.',
        'The presenter is always named in text, not only by an icon.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'Zoom steps are `SCREEN_SHARE_ZOOM_LEVELS` (100%, 150%, 200%, 300%); control them with `zoom` + `onZoomChange` or start with `defaultZoom`.',
        'The video is CallVideo (muted, inline): play the share’s audio, if any, separately.',
        'Full screen applies to the whole view, so the controls stay available.',
        'The ref is the root `<div>`.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        'Migrating from Rook Chat’s `CallTile` with `screen`: pass `screenStream` as `stream` and the participant name as `presenterName`; set `isLocal` for your own share and move `stopScreenShare` to `onStopSharing`.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'isLocal',
          values: ['true', 'false'],
          description: 'Your own share: “You are presenting”, hidden preview, Stop sharing.',
        },
        {
          prop: 'hideControls',
          values: ['true', 'false'],
          description: 'Hide zoom, picture-in-picture and full screen (for thumbnails).',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Zoomed',
          attribute: 'data-zoomed',
          description: 'Zoom above 100%: the view scrolls.',
        },
        { name: 'Local', attribute: 'data-local', description: 'You are the presenter.' },
        {
          name: 'Full screen',
          attribute: 'data-fullscreen',
          description: 'The view fills the screen without border.',
        },
        { name: 'Waiting', description: '`stream` is null: “Waiting for the shared screen…”.' },
      ],
    },
    interaction: {
      items: [
        'Zoom in and out step through the levels; Fit returns to 100%.',
        'When zoomed, scroll (or drag the scrollbars, or use the arrow keys) to pan.',
        'Picture-in-picture pops the screen into a floating window and the button toggles back.',
      ],
    },
    keyboard: {
      items: [
        { keys: 'Tab', action: 'Moves through the controls, then the shared screen region.' },
        { keys: '+ / -', action: 'Zoom in or out (focus on the shared screen).' },
        { keys: '0', action: 'Fit to view.' },
        { keys: 'Arrow keys', action: 'Pan when zoomed (native scrolling of the focused region).' },
      ],
    },
    accessibility: {
      role: 'region (shared screen); group (controls)',
      name: '“Screen shared by {name}”; controls are “Shared screen controls”.',
      items: [
        'The zoom level is visible and in a polite live region.',
        'The picture-in-picture button exposes `aria-pressed`.',
        'A shared screen is a picture: say what matters out loud or share a document too.',
      ],
    },
    responsive: {
      items: [
        'The view keeps a 16:9 box and fills the width; the bar wraps controls under the name at 390px.',
        'Controls are 44px on coarse pointers.',
      ],
    },
    theme: {
      items: [
        'Bar on `--mn-color-bg-surface`; the viewport is `--mn-color-bg-inverse`, so letterboxing is neutral in every theme.',
        'Forced colours: `CanvasText` borders.',
      ],
    },
    examples: {
      items: [
        { title: 'Someone else presenting', stack: true, render: () => <RemoteShare /> },
        { title: 'You are presenting', stack: true, render: () => <LocalShare /> },
        {
          title: 'Waiting for the stream',
          stack: true,
          render: () => (
            <div style={{ width: '100%', maxWidth: 640 }}>
              <ScreenShareView stream={null} presenterName="Sam Rivera" />
            </div>
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Show the shared screen as the main tile in a call while it is active.',
        'Keep the Stop sharing button visible to the presenter.',
      ],
      dont: [
        'Do not show your own share full size by default.',
        'Do not crop a shared screen (`cover`); text at the edges matters.',
      ],
    },
    related: {
      items: [
        { id: 'call-stage', reason: 'Lays out the share with the participants.' },
        { id: 'call-tile', reason: 'Camera tiles and CallVideo.' },
        { id: 'call-controls', reason: 'The share and stop buttons in the call bar.' },
      ],
    },
    dependencies: { packages: ['react'], components: ['button', 'call-controls', 'call-tile'] },
    testing: {
      unit: [
        'Presenter label, named region, stream attached, contained fit.',
        'Zoom with buttons and keys, limits, fit.',
        'Picture-in-picture only when supported, with pressed state.',
        'Local share hidden by default, Stop sharing and Show preview.',
        'Waiting state, hidden controls, translated; axe; server rendering.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
