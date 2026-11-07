'use client';

import { forwardRef, useEffect, useId, useState, type HTMLAttributes, type ReactNode } from 'react';
import { cx } from '../../../internal/cx.js';
import { Button } from '../../actions/button/index.js';
import { Progress } from '../../data-display/progress-bar/index.js';
import { Avatar, AvatarGroup } from '../../identity/avatar/index.js';
import { CallControlButton, CallIcon } from '../call-controls/index.js';
import { CallVideo } from '../call-tile/index.js';
import { DevicePicker, type DevicePickerProps } from '../device-picker/index.js';

/* ---------------------------------------------------------- audio level */

type AudioContextCtor = typeof AudioContext;

/**
 * The loudness of a stream's microphone, 0–1, sampled about eight times a
 * second with the Web Audio API. Returns 0 on the server, in browsers without
 * Web Audio, and while `enabled` is false. Only reads the stream; it never
 * plays it.
 */
export function useAudioLevel(stream: MediaStream | null | undefined, enabled = true): number {
  const [level, setLevel] = useState(0);
  useEffect(() => {
    setLevel(0);
    if (!enabled || !stream || typeof window === 'undefined') return;
    if (typeof stream.getAudioTracks !== 'function' || stream.getAudioTracks().length === 0) return;
    const Ctor: AudioContextCtor | undefined =
      window.AudioContext ??
      (window as unknown as { webkitAudioContext?: AudioContextCtor }).webkitAudioContext;
    if (!Ctor) return;
    let ctx: AudioContext;
    try {
      ctx = new Ctor();
    } catch {
      return;
    }
    const analyser = ctx.createAnalyser();
    analyser.fftSize = 512;
    const source = ctx.createMediaStreamSource(stream);
    source.connect(analyser);
    const data = new Float32Array(analyser.fftSize);
    const timer = window.setInterval(() => {
      analyser.getFloatTimeDomainData(data);
      let sum = 0;
      for (const v of data) sum += v * v;
      setLevel(Math.min(1, Math.sqrt(sum / data.length) * 6));
    }, 120);
    return () => {
      window.clearInterval(timer);
      source.disconnect();
      void ctx.close().catch(() => undefined);
    };
  }, [stream, enabled]);
  return level;
}

/* ---------------------------------------------------------------- lobby */

/** Visible and announced strings. Override them to translate. */
export interface PreJoinLobbyMessages {
  title: string;
  preview: string;
  cameraOff: string;
  startingCamera: string;
  noPreview: string;
  joinMuted: string;
  joinWithMic: string;
  joinCameraOff: string;
  joinWithCamera: string;
  micLevel: string;
  alreadyIn: (count: number) => string;
  participants: string;
  cancel: string;
  join: string;
  joining: string;
}

export const DEFAULT_PRE_JOIN_LOBBY_MESSAGES: PreJoinLobbyMessages = {
  title: 'Join the call',
  preview: 'Your camera preview',
  cameraOff: 'Camera is off',
  startingCamera: 'Starting camera…',
  noPreview: 'No preview',
  joinMuted: 'Join muted',
  joinWithMic: 'Join with mic on',
  joinCameraOff: 'Join with camera off',
  joinWithCamera: 'Join with camera on',
  micLevel: 'Microphone level',
  alreadyIn: (count) => `${count} already in the call`,
  participants: 'People in the call',
  cancel: 'Cancel',
  join: 'Join now',
  joining: 'Joining…',
};

export interface PreJoinLobbyProps extends Omit<HTMLAttributes<HTMLElement>, 'title'> {
  /**
   * The preview stream the product captured (camera and microphone). Hand
   * the same stream to the call on join so the browser does not ask twice.
   */
  stream?: MediaStream | null | undefined;
  /** Join with the microphone on. */
  micOn: boolean;
  onMicOnChange: (next: boolean) => void;
  /** Join with the camera on. */
  cameraOn?: boolean | undefined;
  /** Shows the camera toggle. Leave out for audio-only calls. */
  onCameraOnChange?: ((next: boolean) => void) | undefined;
  /**
   * Microphone level 0–1 for the meter. Default: measured from `stream` with
   * the Web Audio API (`useAudioLevel`).
   */
  audioLevel?: number | undefined;
  /** Shows device selects under the preview. */
  devicePicker?: DevicePickerProps | undefined;
  /** People already in the call. */
  participants?: { name: string; avatarUrl?: string | null | undefined }[] | undefined;
  /**
   * Heading. Default "Join the call". Pass `null` when a Dialog already has a
   * DialogTitle, and name the lobby with `aria-label` or `aria-labelledby`.
   */
  title?: ReactNode;
  /** A problem getting the preview (for example access blocked), shown as an alert. */
  error?: ReactNode;
  /** Joins with the chosen settings. */
  onJoin: (settings: { micOn: boolean; cameraOn: boolean }) => void;
  /** Shows a Cancel button. */
  onCancel?: (() => void) | undefined;
  /** While joining: the join button shows progress. */
  joining?: boolean | undefined;
  /** Join button text, for example "Start call". Default "Join now". */
  joinLabel?: string | undefined;
  /** Override visible and announced strings. */
  messages?: Partial<PreJoinLobbyMessages> | undefined;
}

/**
 * The pre-join screen: see yourself, check the microphone is picking you up,
 * choose devices, and join with the camera or microphone off. Takes the
 * preview stream and callbacks; it never captures media itself. Place it in
 * a Dialog or a page.
 */
export const PreJoinLobby = forwardRef<HTMLElement, PreJoinLobbyProps>(function PreJoinLobby(
  {
    stream = null,
    micOn,
    onMicOnChange,
    cameraOn = false,
    onCameraOnChange,
    audioLevel,
    devicePicker,
    participants = [],
    title,
    error,
    onJoin,
    onCancel,
    joining = false,
    joinLabel,
    messages: messagesProp,
    className,
    ...rest
  },
  ref,
) {
  const m = { ...DEFAULT_PRE_JOIN_LOBBY_MESSAGES, ...messagesProp };
  const titleId = useId();
  const measured = useAudioLevel(stream, micOn && audioLevel === undefined);
  const level = audioLevel ?? measured;
  const hasVideo =
    !!stream && typeof stream.getVideoTracks === 'function' && stream.getVideoTracks().length > 0;
  const showVideo = cameraOn && hasVideo;

  return (
    <section
      ref={ref}
      aria-labelledby={title === null ? undefined : titleId}
      {...rest}
      data-joining={joining || undefined}
      className={cx('mn-pre-join-lobby', className)}
    >
      {title === null ? null : (
        <h2 id={titleId} className="mn-pre-join-lobby__title">
          {title ?? m.title}
        </h2>
      )}
      <div className="mn-pre-join-lobby__preview" role="group" aria-label={m.preview}>
        {showVideo ? (
          <CallVideo stream={stream} mirror className="mn-pre-join-lobby__video" />
        ) : (
          <p className="mn-pre-join-lobby__placeholder">
            {error ? m.noPreview : cameraOn ? m.startingCamera : m.cameraOff}
          </p>
        )}
        {micOn && (
          <Progress
            semantics="meter"
            size="sm"
            tone="success"
            value={Math.round(level * 100)}
            label={m.micLevel}
            valueText={`${Math.round(level * 100)}%`}
            className="mn-pre-join-lobby__meter"
          />
        )}
        <div className="mn-pre-join-lobby__toggles">
          <CallControlButton
            label={micOn ? m.joinMuted : m.joinWithMic}
            pressed={!micOn}
            tone={micOn ? 'default' : 'off'}
            icon={<CallIcon name={micOn ? 'mic' : 'mic-off'} />}
            onClick={() => onMicOnChange(!micOn)}
          />
          {onCameraOnChange && (
            <CallControlButton
              label={cameraOn ? m.joinCameraOff : m.joinWithCamera}
              pressed={!cameraOn}
              tone={cameraOn ? 'default' : 'off'}
              icon={<CallIcon name={cameraOn ? 'camera' : 'camera-off'} />}
              onClick={() => onCameraOnChange(!cameraOn)}
            />
          )}
        </div>
      </div>

      {error ? (
        <div className="mn-pre-join-lobby__error" role="alert">
          {error}
        </div>
      ) : null}

      {devicePicker ? <DevicePicker size="sm" {...devicePicker} /> : null}

      {participants.length > 0 && (
        <div className="mn-pre-join-lobby__others">
          <AvatarGroup label={m.participants} max={5} size="xs">
            {participants.map((p, i) => (
              <Avatar key={`${p.name}-${i}`} name={p.name} src={p.avatarUrl ?? null} />
            ))}
          </AvatarGroup>
          <span>{m.alreadyIn(participants.length)}</span>
        </div>
      )}

      <div className="mn-pre-join-lobby__actions">
        {onCancel && (
          <Button variant="ghost" onClick={onCancel} disabled={joining}>
            {m.cancel}
          </Button>
        )}
        <Button variant="primary" loading={joining} onClick={() => onJoin({ micOn, cameraOn })}>
          {joining ? m.joining : (joinLabel ?? m.join)}
        </Button>
      </div>
    </section>
  );
});
