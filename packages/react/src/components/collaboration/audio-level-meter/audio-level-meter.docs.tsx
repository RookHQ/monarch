import { useEffect, useState } from 'react';
import { AudioLevelMeter } from '@rookcom/monarch-react/collaboration';
import type { ComponentDocs } from '../../../internal/component-docs.js';

/** Simulates a voice so the example moves without asking for the microphone. */
function SimulatedVoice({ variant }: { variant?: 'bars' | 'bar' }) {
  const [level, setLevel] = useState(0);
  useEffect(() => {
    let t = 0;
    const timer = setInterval(() => {
      t += 1;
      setLevel(Math.max(0, Math.sin(t / 3) * 0.5 + Math.sin(t * 1.7) * 0.25));
    }, 120);
    return () => clearInterval(timer);
  }, []);
  return <AudioLevelMeter level={level} variant={variant} />;
}

export default {
  id: 'audio-level-meter',
  title: 'AudioLevelMeter',
  summary:
    'Shows how loud a microphone is, from a MediaStream or a measured level; static under reduced motion.',
  sections: {
    purpose: {
      summary:
        'Shows whether the microphone picks up sound and how loud it is, so people can check their mic before and during a call. Needed by: Rook Chat calls (`lib/calls/speaker.ts` `LevelMeter`, `components/calls/PreJoinLobby.tsx`, `CallTile.tsx`), device settings and voice recording.',
      useWhen: [
        'A pre-join screen or device settings let people test their microphone.',
        'A call tile or control shows that someone is speaking.',
      ],
      avoidWhen: [
        'Showing progress or quota: use Progress.',
        'Showing who is the active speaker in a grid: the call tile’s speaking ring already does that; use the meter `decorative` inside it if both are shown.',
      ],
    },
    designPrinciples: {
      items: [
        'Measure locally with the Web Audio API (`useAudioLevel` from PreJoinLobby); the stream is only read, never played.',
        'Respect reduced motion: the level stops moving and the meter only shows whether there is sound.',
        'Muted is said in words (“Muted”), not only by grey bars.',
        'Products that already measure levels (for example per participant) pass `level` instead of a stream.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        '`level` (0–1) takes precedence over `stream`; measuring stops while `muted` is true or `level` is set.',
        '`useAudioLevel(stream, enabled)` is exported by PreJoinLobby for custom displays.',
        'The ref is the root `<div>`.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        'Migrating from Rook Chat’s `LevelMeter` class: pass each participant’s measured level as `level`, and `isSpeaking` becomes the `data-speaking` attribute (threshold 0.08).',
      ],
    },
    variants: {
      items: [
        {
          prop: 'variant',
          values: ['bars', 'bar'],
          description:
            '`bars` (default): rising segments, for controls and tiles. `bar`: a thin full-width bar, for settings.',
        },
        {
          prop: 'size',
          values: ['sm', 'md'],
          description: 'Height of the bars variant (12 or 16px).',
        },
        { prop: 'segments', values: ['1–n'], description: 'Number of bars. Default 5.' },
      ],
    },
    states: {
      items: [
        {
          name: 'Speaking',
          attribute: 'data-speaking',
          description: 'The level is above the speaking threshold.',
        },
        {
          name: 'Muted',
          attribute: 'data-muted',
          description: 'Empty, greyed, value text “Muted”.',
        },
        {
          name: 'Reduced motion',
          attribute: 'data-reduced-motion',
          description: 'All bars on while there is sound, none otherwise; no transitions.',
        },
      ],
    },
    interaction: { items: [], notes: ['Not interactive: it is a read-only meter.'] },
    keyboard: { items: [], notes: ['Not focusable; it has no actions.'] },
    accessibility: {
      role: 'meter (none when `decorative`)',
      name: '“Microphone level” by default; value text is the percentage or “Muted”.',
      items: [
        'The meter’s value changes are not announced live, which would be noisy; screen reader users read it on demand.',
        'Use `decorative` when the same state is already spoken elsewhere.',
        'Colour is not the only signal: the number of lit bars and the value text carry the level.',
      ],
    },
    responsive: {
      items: [
        'The bars variant has a fixed small size; the bar variant fills its container (at least 64px).',
      ],
    },
    theme: {
      items: [
        'Lit bars use `--mn-color-success-solid`; unlit bars `--mn-color-border-strong`; muted `--mn-color-text-disabled`.',
        'Forced colours: lit bars in `Highlight`, unlit in `GrayText`.',
      ],
    },
    examples: {
      items: [
        { title: 'Bars (simulated voice)', render: () => <SimulatedVoice /> },
        {
          title: 'Bar variant in settings',
          stack: true,
          render: () => (
            <div style={{ width: '100%', maxWidth: 320 }}>
              <SimulatedVoice variant="bar" />
            </div>
          ),
        },
        {
          title: 'Muted and small',
          render: () => (
            <div style={{ display: 'flex', gap: 16, alignItems: 'center' }}>
              <AudioLevelMeter level={0.7} muted />
              <AudioLevelMeter level={0.5} size="sm" />
            </div>
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Pass the same stream you send, so the meter shows what others will hear.',
        'Set `muted` when the track is disabled, so the meter says so.',
      ],
      dont: [
        'Do not create a meter per participant from remote streams when the server already reports levels; pass `level`.',
        'Do not use the meter as the only mute indicator.',
      ],
    },
    related: {
      items: [
        {
          id: 'pre-join-lobby',
          reason: 'Exports `useAudioLevel` and shows a level before joining.',
        },
        { id: 'device-picker', reason: 'Choose the microphone being measured.' },
        { id: 'call-tile', reason: 'Shows participants, with speaking state.' },
      ],
    },
    dependencies: { packages: ['react'], components: ['pre-join-lobby'] },
    testing: {
      unit: [
        'Named meter with value and text; segments follow the level and clamp.',
        'Muted text and empty bars; decorative mode.',
        'Bar variant scale.',
        'Reduced motion shows on or off only.',
        'Measures a stream with a mocked AudioContext and stops when muted; translated; axe; server rendering.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
