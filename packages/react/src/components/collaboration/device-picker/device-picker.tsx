'use client';

import { forwardRef, useCallback, useEffect, useRef, useState, type HTMLAttributes } from 'react';
import { cx } from '../../../internal/cx.js';
import { Button } from '../../actions/button/index.js';
import { InlineNotice } from '../../feedback/inline-notice/index.js';
import { Field, FieldLabel, NativeSelect } from '../../forms/form-field/index.js';

/* ----------------------------------------------------------------- hook */

/** A media device, as `enumerateDevices()` returns it (or test data shaped like it). */
export interface MediaDeviceOption {
  deviceId: string;
  kind: MediaDeviceKind;
  /** Empty until the person has allowed camera or microphone access. */
  label: string;
  groupId?: string | undefined;
}

/** A browser permission state, or `unsupported` where the Permissions API cannot tell. */
export type MediaPermission = 'granted' | 'denied' | 'prompt' | 'unsupported';

export interface MediaDevicesState {
  /** False on the server and in browsers without `navigator.mediaDevices`. */
  supported: boolean;
  /** Whether audio output selection (`HTMLMediaElement.setSinkId`) works here. */
  canSelectOutput: boolean;
  devices: MediaDeviceOption[];
  permissions: { microphone: MediaPermission; camera: MediaPermission };
  /** True when devices are listed without names, which means access has not been allowed yet. */
  labelsHidden: boolean;
  /** Re-reads the device list. */
  refresh: () => Promise<void>;
  /**
   * Asks for access (briefly opens and stops the devices) so names appear.
   * Rejects with the browser's error when the person refuses.
   */
  requestAccess: (kinds?: { audio?: boolean; video?: boolean }) => Promise<void>;
}

export interface UseMediaDevicesOptions {
  /** Skip enumeration, for example while a picker is closed. Default true. */
  enabled?: boolean | undefined;
}

async function queryPermission(name: 'microphone' | 'camera'): Promise<MediaPermission> {
  try {
    const status = await navigator.permissions.query({ name: name as PermissionName });
    return status.state;
  } catch {
    return 'unsupported';
  }
}

/**
 * Lists cameras, microphones and speakers with `navigator.mediaDevices`,
 * refreshes when devices are plugged in or removed, and reports permission
 * states. Everything is feature-detected inside effects, so it is SSR-safe
 * and returns `supported: false` on the server.
 */
export function useMediaDevices({
  enabled = true,
}: UseMediaDevicesOptions = {}): MediaDevicesState {
  const [supported, setSupported] = useState(false);
  const [canSelectOutput, setCanSelectOutput] = useState(false);
  const [devices, setDevices] = useState<MediaDeviceOption[]>([]);
  const [permissions, setPermissions] = useState<MediaDevicesState['permissions']>({
    microphone: 'unsupported',
    camera: 'unsupported',
  });
  const mounted = useRef(true);

  const readPermissions = useCallback(async () => {
    if (typeof navigator === 'undefined' || !navigator.permissions?.query) return;
    const [microphone, camera] = await Promise.all([
      queryPermission('microphone'),
      queryPermission('camera'),
    ]);
    if (mounted.current) setPermissions({ microphone, camera });
  }, []);

  const refresh = useCallback(async () => {
    const md = typeof navigator === 'undefined' ? undefined : navigator.mediaDevices;
    if (!md?.enumerateDevices) return;
    try {
      const list = await md.enumerateDevices();
      if (!mounted.current) return;
      setDevices(
        list.map((d) => ({
          deviceId: d.deviceId,
          kind: d.kind,
          label: d.label,
          groupId: d.groupId,
        })),
      );
    } catch {
      if (mounted.current) setDevices([]);
    }
    await readPermissions();
  }, [readPermissions]);

  const requestAccess = useCallback(
    async ({ audio = true, video = true }: { audio?: boolean; video?: boolean } = {}) => {
      const md = typeof navigator === 'undefined' ? undefined : navigator.mediaDevices;
      if (!md?.getUserMedia) throw new Error('Media devices are not available.');
      try {
        const stream = await md.getUserMedia({ audio, video });
        stream.getTracks().forEach((t) => t.stop());
      } finally {
        await refresh();
      }
    },
    [refresh],
  );

  useEffect(() => {
    mounted.current = true;
    return () => {
      mounted.current = false;
    };
  }, []);

  useEffect(() => {
    if (!enabled || typeof navigator === 'undefined') return;
    const md = navigator.mediaDevices;
    setSupported(!!md?.enumerateDevices);
    setCanSelectOutput(
      typeof HTMLMediaElement !== 'undefined' && 'setSinkId' in HTMLMediaElement.prototype,
    );
    if (!md?.enumerateDevices) return;
    void refresh();
    const onChange = () => void refresh();
    md.addEventListener?.('devicechange', onChange);
    return () => md.removeEventListener?.('devicechange', onChange);
  }, [enabled, refresh]);

  const labelsHidden = devices.length > 0 && devices.every((d) => !d.label);
  return { supported, canSelectOutput, devices, permissions, labelsHidden, refresh, requestAccess };
}

/* --------------------------------------------------------------- picker */

/** Visible and announced strings. Override them to translate. */
export interface DevicePickerMessages {
  microphone: string;
  camera: string;
  speaker: string;
  systemDefault: string;
  unnamed: (kind: 'microphone' | 'camera' | 'speaker', index: number) => string;
  unsupported: string;
  labelsHidden: string;
  allowAccess: string;
  denied: string;
  accessFailed: string;
}

export const DEFAULT_DEVICE_PICKER_MESSAGES: DevicePickerMessages = {
  microphone: 'Microphone',
  camera: 'Camera',
  speaker: 'Speaker',
  systemDefault: 'System default',
  unnamed: (kind, index) =>
    `${kind === 'microphone' ? 'Microphone' : kind === 'camera' ? 'Camera' : 'Speaker'} ${index}`,
  unsupported: 'This browser can’t list cameras and microphones.',
  labelsHidden: 'Allow camera and microphone access to see device names.',
  allowAccess: 'Allow access',
  denied:
    'Camera or microphone access is blocked. Allow it in the browser’s site settings, then try again.',
  accessFailed: 'Access wasn’t allowed.',
};

export interface DevicePickerProps extends Omit<HTMLAttributes<HTMLDivElement>, 'onChange'> {
  /** Selected microphone id; `null` or `''` means the system default. */
  audioInputId?: string | null | undefined;
  /** Shows the microphone select. */
  onAudioInputChange?: ((deviceId: string) => void) | undefined;
  /** Selected camera id; `null` or `''` means the system default. */
  videoInputId?: string | null | undefined;
  /** Shows the camera select. */
  onVideoInputChange?: ((deviceId: string) => void) | undefined;
  /** Selected speaker id; `null` or `''` means the system default. */
  audioOutputId?: string | null | undefined;
  /** Shows the speaker select, where the browser supports choosing an output. */
  onAudioOutputChange?: ((deviceId: string) => void) | undefined;
  /**
   * Devices to list instead of asking the browser, for example from the
   * product's own media layer. When set, the picker does not enumerate.
   */
  devices?: MediaDeviceOption[] | undefined;
  /** Replaces the default access request (getUserMedia, then stop). */
  onRequestAccess?: (() => void | Promise<void>) | undefined;
  /** Control height. Default `md`; `sm` suits a popover during a call. */
  size?: 'sm' | 'md' | undefined;
  disabled?: boolean | undefined;
  /** Override visible and announced strings. */
  messages?: Partial<DevicePickerMessages> | undefined;
}

type Kind = 'microphone' | 'camera' | 'speaker';
const KIND: Record<Kind, MediaDeviceKind> = {
  microphone: 'audioinput',
  camera: 'videoinput',
  speaker: 'audiooutput',
};

/**
 * Microphone, camera and speaker selects for a lobby or a call's settings.
 * Lists devices with `navigator.mediaDevices` (or a `devices` list you pass),
 * refreshes when hardware changes, and explains missing names and blocked
 * permissions with a way to allow access.
 */
export const DevicePicker = forwardRef<HTMLDivElement, DevicePickerProps>(function DevicePicker(
  {
    audioInputId,
    onAudioInputChange,
    videoInputId,
    onVideoInputChange,
    audioOutputId,
    onAudioOutputChange,
    devices: devicesProp,
    onRequestAccess,
    size = 'md',
    disabled = false,
    messages: messagesProp,
    className,
    ...rest
  },
  ref,
) {
  const m = { ...DEFAULT_DEVICE_PICKER_MESSAGES, ...messagesProp };
  const media = useMediaDevices({ enabled: !devicesProp });
  const devices = devicesProp ?? media.devices;
  const [busy, setBusy] = useState(false);
  const [failed, setFailed] = useState(false);

  const controlled = !!devicesProp;
  const supported = controlled || media.supported;
  const labelsHidden = controlled
    ? devices.length > 0 && devices.every((d) => !d.label)
    : media.labelsHidden;
  const denied =
    !controlled &&
    (media.permissions.microphone === 'denied' || media.permissions.camera === 'denied');
  const showOutput = !!onAudioOutputChange && (controlled || media.canSelectOutput);

  const allow = async () => {
    setBusy(true);
    setFailed(false);
    try {
      if (onRequestAccess) await onRequestAccess();
      else
        await media.requestAccess({
          audio: !!onAudioInputChange || !!onAudioOutputChange,
          video: !!onVideoInputChange,
        });
    } catch {
      setFailed(true);
    } finally {
      setBusy(false);
    }
  };

  const select = (kind: Kind, value: string | null | undefined, onChange: (id: string) => void) => {
    const list = devices.filter((d) => d.kind === KIND[kind] && d.deviceId);
    const current = value ?? '';
    const known = current === '' || list.some((d) => d.deviceId === current);
    return (
      <Field key={kind} disabled={disabled} className="mn-device-picker__field" data-kind={kind}>
        <FieldLabel>{m[kind]}</FieldLabel>
        <NativeSelect
          size={size}
          value={current}
          onChange={(event) => onChange(event.target.value)}
        >
          <option value="">{m.systemDefault}</option>
          {!known && <option value={current}>{m.unnamed(kind, 1)}</option>}
          {list.map((d, i) => (
            <option key={d.deviceId} value={d.deviceId}>
              {d.label || m.unnamed(kind, i + 1)}
            </option>
          ))}
        </NativeSelect>
      </Field>
    );
  };

  return (
    <div ref={ref} {...rest} data-size={size} className={cx('mn-device-picker', className)}>
      {!supported ? (
        <InlineNotice tone="neutral">{m.unsupported}</InlineNotice>
      ) : denied ? (
        <InlineNotice tone="warning" live>
          {m.denied}
        </InlineNotice>
      ) : labelsHidden ? (
        <InlineNotice
          tone="info"
          live
          action={
            <Button size="sm" loading={busy} disabled={disabled} onClick={() => void allow()}>
              {m.allowAccess}
            </Button>
          }
        >
          {failed ? m.accessFailed : m.labelsHidden}
        </InlineNotice>
      ) : null}
      {onAudioInputChange && select('microphone', audioInputId, onAudioInputChange)}
      {onVideoInputChange && select('camera', videoInputId, onVideoInputChange)}
      {showOutput && onAudioOutputChange && select('speaker', audioOutputId, onAudioOutputChange)}
    </div>
  );
});
