import { afterEach, describe, expect, it, vi } from 'vitest';
import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { DevicePicker, type MediaDeviceOption } from './index.js';

const devices: MediaDeviceOption[] = [
  { deviceId: 'mic-1', kind: 'audioinput', label: 'Desk microphone' },
  { deviceId: 'mic-2', kind: 'audioinput', label: 'Headset microphone' },
  { deviceId: 'cam-1', kind: 'videoinput', label: 'Built-in camera' },
  { deviceId: 'spk-1', kind: 'audiooutput', label: 'Headphones' },
];

const original = Object.getOwnPropertyDescriptor(navigator, 'mediaDevices');
function mockMediaDevices(list: Partial<MediaDeviceInfo>[], getUserMedia = vi.fn()) {
  const listeners = new Set<() => void>();
  const md = {
    enumerateDevices: vi.fn(async () => list as MediaDeviceInfo[]),
    getUserMedia,
    addEventListener: (_: string, fn: () => void) => listeners.add(fn),
    removeEventListener: (_: string, fn: () => void) => listeners.delete(fn),
  };
  Object.defineProperty(navigator, 'mediaDevices', { configurable: true, value: md });
  return { md, fire: () => listeners.forEach((fn) => fn()) };
}

afterEach(() => {
  if (original) Object.defineProperty(navigator, 'mediaDevices', original);
  else delete (navigator as { mediaDevices?: unknown }).mediaDevices;
});

describe('DevicePicker', () => {
  it('lists given devices in labelled selects with a system default', async () => {
    const onAudioInputChange = vi.fn();
    render(
      <DevicePicker
        devices={devices}
        audioInputId={null}
        onAudioInputChange={onAudioInputChange}
        videoInputId="cam-1"
        onVideoInputChange={() => {}}
        onAudioOutputChange={() => {}}
      />,
    );
    const mic = screen.getByRole('combobox', { name: 'Microphone' });
    expect(mic).toHaveValue('');
    expect(
      within(mic).getByRole('option', { name: 'System default', selected: true }),
    ).toBeInTheDocument();
    expect(screen.getByRole('combobox', { name: 'Camera' })).toHaveValue('cam-1');
    expect(screen.getByRole('combobox', { name: 'Speaker' })).toBeInTheDocument();
    await userEvent.setup().selectOptions(mic, 'mic-2');
    expect(onAudioInputChange).toHaveBeenCalledWith('mic-2');
    await expectNoA11yViolations();
  });

  it('shows only the selects that have callbacks', () => {
    render(<DevicePicker devices={devices} onAudioInputChange={() => {}} />);
    expect(screen.getAllByRole('combobox')).toHaveLength(1);
  });

  it('names unlabelled devices and offers to allow access', async () => {
    const onRequestAccess = vi.fn();
    render(
      <DevicePicker
        devices={[
          { deviceId: 'a', kind: 'audioinput', label: '' },
          { deviceId: 'b', kind: 'audioinput', label: '' },
        ]}
        onAudioInputChange={() => {}}
        onRequestAccess={onRequestAccess}
      />,
    );
    expect(screen.getByRole('option', { name: 'Microphone 2' })).toBeInTheDocument();
    expect(
      screen.getByText('Allow camera and microphone access to see device names.'),
    ).toBeInTheDocument();
    await userEvent.setup().click(screen.getByRole('button', { name: 'Allow access' }));
    expect(onRequestAccess).toHaveBeenCalledOnce();
  });

  it('reports a refused access request', async () => {
    render(
      <DevicePicker
        devices={[{ deviceId: 'a', kind: 'audioinput', label: '' }]}
        onAudioInputChange={() => {}}
        onRequestAccess={() => Promise.reject(new Error('NotAllowedError'))}
      />,
    );
    await userEvent.setup().click(screen.getByRole('button', { name: 'Allow access' }));
    expect(await screen.findByText('Access wasn’t allowed.')).toBeInTheDocument();
  });

  it('enumerates browser devices and refreshes on devicechange', async () => {
    const list: Partial<MediaDeviceInfo>[] = [
      { deviceId: 'mic-1', kind: 'audioinput', label: 'Desk microphone', groupId: 'g' },
    ];
    const { md, fire } = mockMediaDevices(list);
    render(<DevicePicker onAudioInputChange={() => {}} />);
    expect(await screen.findByRole('option', { name: 'Desk microphone' })).toBeInTheDocument();
    list.push({ deviceId: 'mic-2', kind: 'audioinput', label: 'USB microphone', groupId: 'h' });
    fire();
    expect(await screen.findByRole('option', { name: 'USB microphone' })).toBeInTheDocument();
    expect(md.enumerateDevices).toHaveBeenCalledTimes(2);
  });

  it('requests access with getUserMedia and stops the tracks', async () => {
    const stop = vi.fn();
    const list: Partial<MediaDeviceInfo>[] = [{ deviceId: 'mic-1', kind: 'audioinput', label: '' }];
    const getUserMedia = vi.fn(async () => {
      list[0] = { ...list[0], label: 'Desk microphone' };
      return { getTracks: () => [{ stop }] } as unknown as MediaStream;
    });
    mockMediaDevices(list, getUserMedia);
    render(<DevicePicker onAudioInputChange={() => {}} />);
    await userEvent.setup().click(await screen.findByRole('button', { name: 'Allow access' }));
    expect(getUserMedia).toHaveBeenCalledWith({ audio: true, video: false });
    expect(stop).toHaveBeenCalled();
    await waitFor(() =>
      expect(screen.getByRole('option', { name: 'Desk microphone' })).toBeInTheDocument(),
    );
  });

  it('explains when the browser cannot list devices', async () => {
    Object.defineProperty(navigator, 'mediaDevices', { configurable: true, value: undefined });
    render(<DevicePicker onAudioInputChange={() => {}} />);
    expect(
      await screen.findByText('This browser can’t list cameras and microphones.'),
    ).toBeInTheDocument();
  });

  it('renders on the server without touching navigator', () => {
    const html = renderToString(<DevicePicker onAudioInputChange={() => {}} />);
    expect(html).toContain('mn-device-picker');
    expect(html).toContain('System default');
  });
});
