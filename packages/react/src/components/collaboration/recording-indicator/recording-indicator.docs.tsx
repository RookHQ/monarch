import { useState } from 'react';
import { Button } from '@rookcom/monarch-react';
import { RecordingIndicator } from '@rookcom/monarch-react/collaboration';
import type { ComponentDocs } from '../../../internal/component-docs.js';
// The scenario of the primary example, shared with iOS and Android (CROSS-PLATFORM.md section 8).
import fixture from './recording-indicator.fixture.json';

/** Opens 1 minute 23 seconds into a recording; Start and Stop then run a live one. */
function RecordingDemo() {
  const [since, setSince] = useState<number | null>(null);
  const [opening, setOpening] = useState(true);
  const recording = opening || since !== null;
  return (
    <div style={{ display: 'flex', flexWrap: 'wrap', gap: 12, alignItems: 'center' }}>
      <Button
        size="sm"
        onClick={() => {
          setOpening(false);
          setSince(recording ? null : Date.now());
        }}
      >
        {recording ? fixture.data.stop : fixture.data.start}
      </Button>
      <RecordingIndicator
        recording={recording}
        {...(opening ? { elapsed: fixture.data.elapsedMillis } : { since: since ?? undefined })}
      />
    </div>
  );
}

export default {
  id: 'recording-indicator',
  title: 'RecordingIndicator',
  summary: 'A REC badge with the recorded time; start, pause and stop are announced once.',
  sections: {
    purpose: {
      summary:
        'Tells everyone in a call or session that it is being recorded, for how long, and lets the person recording stop it. Needed by: Rook Chat calls (`components/calls/CallOverlay.tsx`, `CallTimer.tsx`), and screen or voice recording in Rook Intellect and Rook Tickets.',
      useWhen: [
        'A call, meeting or screen session is being recorded.',
        'A voice note or screen capture is in progress.',
      ],
      avoidWhen: [
        'A plain call duration without recording: use CallTimer.',
        'Live broadcast status: use a status badge with its own wording.',
      ],
    },
    designPrinciples: {
      items: [
        'Recording is never only red: the badge says “REC” (and “Recording” to screen readers) and the dot pulses.',
        'Changes are announced once (“Recording started”); the ticking time is never announced.',
        'Paused is a distinct state: the dot becomes a square, the badge says “Paused” and the time stops.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        '`since` ticks every second after hydration (the server renders `00:00`); `elapsed` shows a time the product tracks, for example excluding pauses.',
        'Time format is `mm:ss`, or `h:mm:ss` after an hour (`formatCallDuration`).',
        'The ref is the root `<div>`, which stays mounted when not recording so the stop announcement is heard.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        'Migrating from Rook Chat’s `CallTimer` next to a red dot: `since` is the same; set `recording` from the call state and move the stop action to `onStop`.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'variant',
          values: ['badge', 'overlay'],
          description:
            '`badge` (default) on surfaces; `overlay` on video with a dark translucent fill.',
        },
        {
          prop: 'showElapsed',
          values: ['true', 'false'],
          description: 'Show the recorded time (default when `since` or `elapsed` is set).',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Recording',
          attribute: 'data-recording',
          description: 'The badge is shown and the dot pulses.',
        },
        {
          name: 'Paused',
          attribute: 'data-paused',
          description: 'Square dot, “Paused”, time stopped.',
        },
        {
          name: 'Not recording',
          description: 'Nothing is visible; the live region stays for the stop announcement.',
        },
      ],
    },
    interaction: {
      items: [
        'With `onStop`, a stop button follows the badge. Confirm in the product if stopping cannot be undone.',
      ],
    },
    keyboard: {
      items: [
        { keys: 'Tab', action: 'Moves to the stop button (when present).' },
        { keys: 'Enter / Space', action: 'Stops recording.' },
      ],
    },
    accessibility: {
      role: 'status (announcements); button (stop)',
      name: 'The badge reads “Recording” (or “Recording, Paused”) and the time; the button is “Stop recording”.',
      items: [
        'The visible “REC” is hidden from screen readers, which hear the full word.',
        'The time is a `<time>` with an ISO 8601 duration.',
        'The pulsing dot stops under reduced motion.',
      ],
    },
    responsive: {
      items: [
        'The badge never wraps; it is about 90px wide with the time.',
        'The stop button is 44px on coarse pointers.',
      ],
    },
    theme: {
      items: [
        'Badge: `--mn-color-danger-subtle` with `--mn-color-danger-strong` text; dot `--mn-color-danger-solid`.',
        'Overlay: `--mn-color-overlay-scrim` with `--mn-color-text-inverse`.',
        'Forced colours: `CanvasText` border and dot.',
      ],
    },
    examples: {
      items: [
        { title: 'Start and stop', render: () => <RecordingDemo /> },
        {
          title: 'Paused, with stop, and on video',
          render: () => (
            <div style={{ display: 'flex', flexWrap: 'wrap', gap: 12, alignItems: 'center' }}>
              <RecordingIndicator recording paused elapsed={754_000} />
              <RecordingIndicator recording elapsed={3_725_000} onStop={() => {}} />
              <div
                style={{ padding: 12, borderRadius: 8, background: 'var(--mn-color-bg-inverse)' }}
              >
                <RecordingIndicator recording variant="overlay" elapsed={42_000} />
              </div>
            </div>
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Show it to everyone in the session, not only the person recording.',
        'Keep it visible for the whole recording, including when the call is minimised.',
      ],
      dont: [
        'Do not hide the badge to save space; recording must always be visible.',
        'Do not announce the time every second.',
      ],
    },
    related: {
      items: [
        { id: 'call-stage', reason: 'Exports CallTimer and `formatCallDuration`.' },
        { id: 'call-controls', reason: 'Where the record action usually lives.' },
        { id: 'mini-call', reason: 'Keep the indicator in the minimised call too.' },
      ],
    },
    dependencies: { packages: ['react'], components: ['call-stage'] },
    testing: {
      unit: [
        'REC hidden and “Recording” spoken; time ticks from `since` with an ISO duration; no announcement on mount.',
        'Start, pause, resume and stop announced once each.',
        'Given elapsed does not tick; time can be hidden.',
        'Stop button from the keyboard; overlay variant; translated; axe; server rendering at 00:00.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
