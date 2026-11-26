'use client';

import { forwardRef, useEffect, useState, type HTMLAttributes } from 'react';
import { cx } from '../../../internal/cx.js';
import { useAudioLevel } from '../pre-join-lobby/index.js';

/** Every string an audio level meter announces. */
export interface AudioLevelMeterMessages {
  /** Accessible name of the meter. */
  label: string;
  /** Value text, from the level 0–100. */
  value: (percent: number) => string;
  /** Value text when the microphone is muted. */
  muted: string;
}

export const AUDIO_LEVEL_METER_MESSAGES: AudioLevelMeterMessages = {
  label: 'Microphone level',
  value: (percent) => `${percent}%`,
  muted: 'Muted',
};

/** Level above which the reduced-motion meter shows sound as on. */
const SPEAKING = 0.08;

function useReducedMotion(): boolean {
  const [reduced, setReduced] = useState(false);
  useEffect(() => {
    if (typeof window === 'undefined' || typeof window.matchMedia !== 'function') return;
    const query = window.matchMedia('(prefers-reduced-motion: reduce)');
    const sync = () => setReduced(query.matches);
    sync();
    query.addEventListener?.('change', sync);
    return () => query.removeEventListener?.('change', sync);
  }, []);
  return reduced;
}

export interface AudioLevelMeterProps extends Omit<HTMLAttributes<HTMLDivElement>, 'children'> {
  /** A stream with an audio track; its loudness is measured with the Web Audio API (`useAudioLevel`). */
  stream?: MediaStream | null | undefined;
  /** The level 0–1 to show, when the product measures it itself. Takes precedence over `stream`. */
  level?: number | undefined;
  /** The microphone is off: the meter is empty and says “Muted”. Measuring stops. */
  muted?: boolean | undefined;
  /** `bars` (default): rising segments. `bar`: one thin horizontal bar. */
  variant?: 'bars' | 'bar' | undefined;
  /** Number of segments in the `bars` variant. Default 5. */
  segments?: number | undefined;
  /** Size of the `bars` variant. Default `md`. */
  size?: 'sm' | 'md' | undefined;
  /**
   * Hide the meter from assistive technology, when the same information is
   * conveyed elsewhere (for example a call tile that already says who is speaking).
   */
  decorative?: boolean | undefined;
  /** Translated strings. */
  messages?: Partial<AudioLevelMeterMessages> | undefined;
}

/**
 * Shows how loud a microphone is, from a MediaStream (Web Audio AnalyserNode)
 * or a level the product measures. Under reduced motion it only shows whether
 * there is sound, without the moving level.
 */
export const AudioLevelMeter = forwardRef<HTMLDivElement, AudioLevelMeterProps>(
  function AudioLevelMeter(
    {
      stream,
      level: levelProp,
      muted = false,
      variant = 'bars',
      segments = 5,
      size = 'md',
      decorative = false,
      messages: messagesProp,
      className,
      ...rest
    },
    ref,
  ) {
    const m = { ...AUDIO_LEVEL_METER_MESSAGES, ...messagesProp };
    const measured = useAudioLevel(stream, !muted && levelProp === undefined);
    const reduced = useReducedMotion();
    const raw = muted ? 0 : (levelProp ?? measured);
    const level = Math.min(1, Math.max(0, Number.isFinite(raw) ? raw : 0));
    const speaking = level >= SPEAKING;
    // Reduced motion: no moving level, only whether there is sound.
    const shown = reduced ? (speaking ? 1 : 0) : level;
    const percent = Math.round(level * 100);
    const count = Math.max(1, Math.round(segments));
    const lit = Math.round(shown * count);

    const a11y = decorative
      ? { 'aria-hidden': true as const }
      : {
          role: 'meter',
          'aria-label': m.label,
          'aria-valuemin': 0,
          'aria-valuemax': 100,
          'aria-valuenow': percent,
          'aria-valuetext': muted ? m.muted : m.value(percent),
        };

    return (
      <div
        ref={ref}
        {...a11y}
        {...rest}
        className={cx('mn-audio-level-meter', className)}
        data-variant={variant}
        data-size={size}
        data-muted={muted || undefined}
        data-speaking={speaking || undefined}
        data-reduced-motion={reduced || undefined}
      >
        {variant === 'bar' ? (
          <span className="mn-audio-level-meter__track">
            <span
              className="mn-audio-level-meter__fill"
              style={{ transform: `scaleX(${shown})` }}
            />
          </span>
        ) : (
          Array.from({ length: count }, (_, i) => (
            <span
              key={i}
              className="mn-audio-level-meter__segment"
              data-on={i < lit || undefined}
              style={{ height: `${Math.round(((i + 1) / count) * 100)}%` }}
            />
          ))
        )}
      </div>
    );
  },
);
