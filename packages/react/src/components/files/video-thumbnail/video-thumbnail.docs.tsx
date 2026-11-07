import { useState } from 'react';
import { VideoThumbnail } from '@rookcom/monarch-react/data';
import type { ComponentDocs } from '../../../internal/component-docs.js';

/** A neutral poster drawn as an inline SVG data URL, so the docs load no remote media. */
const POSTER = `data:image/svg+xml;utf8,${encodeURIComponent(
  '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 320 180"><defs><linearGradient id="g" x1="0" y1="0" x2="1" y2="1"><stop offset="0" stop-color="steelblue"/><stop offset="1" stop-color="seagreen"/></linearGradient></defs><rect width="320" height="180" fill="url(#g)"/><circle cx="250" cy="50" r="28" fill="white" fill-opacity=".35"/><path d="M0 150 L90 90 L160 140 L230 100 L320 160 V180 H0Z" fill="white" fill-opacity=".45"/></svg>',
)}`;

function Playable() {
  const [plays, setPlays] = useState(0);
  return (
    <div style={{ display: 'grid', gap: 8, maxWidth: 320 }}>
      <VideoThumbnail
        thumbnailUrl={POSTER}
        alt="Studio tour"
        duration={95}
        onPlay={() => setPlays((n) => n + 1)}
      />
      <p style={{ margin: 0, fontSize: 13 }}>Played {plays} times</p>
    </div>
  );
}

export default {
  id: 'video-thumbnail',
  title: 'VideoThumbnail',
  summary: 'A video poster: a given thumbnail or a captured frame, with play icon and duration.',
  sections: {
    purpose: {
      summary:
        'Shows what a video is before it plays, in grids, cards and previews. Uses the product’s thumbnail when there is one, otherwise captures a frame in the browser (no server work, no dependency). Needed by: Links.',
      useWhen: [
        'A grid or list of video items that open or play on click.',
        'A preview of an uploaded clip before it is published.',
      ],
      avoidWhen: [
        'Playing the video inline: use a `<video>` with controls.',
        'Still images: use an `<img>` or a Card.',
      ],
    },
    designPrinciples: {
      items: [
        'A real frame beats a generic icon: capture one when no poster exists.',
        'Never broken: if capture fails (no CORS access, bad file) the placeholder stays and `onCaptureError` reports why.',
        'One name: as a button it reads "Play Studio tour, duration 1:35", and the image inside stays silent.',
        'Server-rendered as a placeholder with the right aspect ratio, so the layout does not jump.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        '`VideoThumbnail` renders a `<div>`, or a `<button>` when `onPlay` is set, and forwards its ref to it.',
        '`captureVideoFrame(src, { time, crossOrigin, type, quality, signal })` resolves with a data URL of one frame; use it to make posters on upload.',
        '`formatVideoDuration(seconds)` formats `m:ss` or `h:mm:ss`.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        '`alt` is required: it names the image, or the play button.',
        'Frame capture needs the video served with CORS headers (or same origin); otherwise the placeholder stays.',
        'Migrating from the app copy (Links `VideoThumbnail`): `videoUrl` → `src`; `thumbnailUrl`, `alt` and `showPlayIcon` keep their names (`alt` is now required); `className` sizes it as before but height now follows `aspectRatio` (default `16 / 9`); add `onPlay` to make it a button instead of wrapping it in one.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'onPlay',
          values: ['undefined', 'function'],
          description: 'With it the thumbnail is a button named "Play <alt>".',
        },
        { prop: 'fit', values: ['cover', 'contain'], description: 'Crop to fill, or letterbox.' },
        {
          prop: 'showPlayIcon',
          values: ['true', 'false'],
          description: 'The round play icon over the poster.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Ready',
          attribute: 'data-state="ready"',
          description: 'A poster or captured frame is showing.',
        },
        {
          name: 'Loading',
          attribute: 'data-state="loading"',
          description: 'Capturing a frame; the placeholder pulses.',
        },
        {
          name: 'Placeholder',
          attribute: 'data-state="placeholder"',
          description: 'No poster and no frame: a play glyph on a dark background.',
        },
        {
          name: 'Disabled',
          attribute: 'disabled',
          description: 'As a button, it can be disabled.',
        },
      ],
    },
    interaction: {
      items: [
        'With `onPlay`, clicking or pressing Enter or Space calls it.',
        'Without `onPlay` it is not interactive.',
      ],
    },
    keyboard: {
      items: [{ keys: 'Tab, then Enter / Space', action: 'With `onPlay`: focus and play.' }],
      notes: ['Without `onPlay` there is nothing to focus.'],
    },
    accessibility: {
      role: 'img, or button with `onPlay`',
      name: '`alt`, or "Play <alt>, duration <m:ss>" as a button',
      items: [
        'The play icon is decorative; the duration is part of the button’s name and hidden as text.',
        'An empty `alt` falls back to `messages.fallbackAlt` ("Video") rather than an unnamed control.',
      ],
    },
    responsive: { items: ['Fills its container’s width; height follows `aspectRatio`.'] },
    theme: {
      items: [
        'Background `--mn-color-bg-inverse`; the play disc is a translucent `--mn-color-bg-surface` with `--mn-shadow-md`.',
        'Forced colours: borders and system colours. Reduced motion: no pulse and no hover scale.',
      ],
    },
    examples: {
      items: [
        {
          title: 'With a poster and duration',
          render: () => (
            <div style={{ maxWidth: 320 }}>
              <VideoThumbnail thumbnailUrl={POSTER} alt="Product overview" duration={65} />
            </div>
          ),
        },
        { title: 'As a play button', render: () => <Playable /> },
        {
          title: 'Placeholder (no poster, nothing to capture)',
          render: () => (
            <div style={{ maxWidth: 240 }}>
              <VideoThumbnail alt="Clip still processing" aspectRatio="9 / 16" />
            </div>
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Store a poster with the video (for example from `captureVideoFrame` at upload) and pass it as `thumbnailUrl`.',
        'Write `alt` as what the video is about, not "video thumbnail".',
      ],
      dont: [
        'Do not wrap a thumbnail with `onPlay` in another button or link.',
        'Do not rely on capture for videos on other origins without CORS.',
      ],
    },
    related: {
      items: [
        { id: 'media-upload', reason: 'Uploading the video in the first place.' },
        { id: 'card', reason: 'A container for a thumbnail with a title and actions.' },
      ],
    },
    dependencies: { packages: ['react'], components: [] },
    testing: {
      unit: [
        'formatVideoDuration; captureVideoFrame resolves a frame, rejects a tainted canvas and an abort.',
        'Given poster with alt, duration and decorative play icon; axe passes.',
        'Captures a frame when there is no poster; keeps the placeholder and reports a failed capture.',
        'Button with onPlay: named with duration, keyboard operable, silent image; axe passes.',
        'Fallback name and messages; server render shows the placeholder with the aspect ratio.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
