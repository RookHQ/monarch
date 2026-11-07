import { describe, expect, it, vi } from 'vitest';
import { useState } from 'react';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { PreJoinLobby, type PreJoinLobbyProps } from './index.js';

const stream = {
  getVideoTracks: () => [{ readyState: 'live' }],
  getAudioTracks: () => [],
  getTracks: () => [],
} as unknown as MediaStream;

function Lobby(props: Partial<PreJoinLobbyProps>) {
  const [micOn, setMicOn] = useState(true);
  const [cameraOn, setCameraOn] = useState(true);
  return (
    <PreJoinLobby
      stream={stream}
      micOn={micOn}
      onMicOnChange={setMicOn}
      cameraOn={cameraOn}
      onCameraOnChange={setCameraOn}
      audioLevel={0.4}
      onJoin={() => {}}
      onCancel={() => {}}
      {...props}
    />
  );
}

describe('PreJoinLobby', () => {
  it('shows a mirrored preview, a mic meter and join toggles', async () => {
    const { container } = render(<Lobby />);
    expect(screen.getByRole('region', { name: 'Join the call' })).toHaveClass('mn-pre-join-lobby');
    const video = container.querySelector('video')!;
    expect(video.srcObject).toBe(stream);
    expect(video).toHaveAttribute('data-mirror');
    const meter = screen.getByRole('meter', { name: 'Microphone level' });
    expect(meter).toHaveAttribute('aria-valuenow', '40');
    expect(screen.getByRole('button', { name: 'Join muted' })).toHaveAttribute(
      'aria-pressed',
      'false',
    );
    await expectNoA11yViolations();
  });

  it('joins with the chosen settings', async () => {
    const onJoin = vi.fn();
    render(<Lobby onJoin={onJoin} />);
    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: 'Join muted' }));
    expect(screen.queryByRole('meter')).not.toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Join with camera off' }));
    expect(screen.getByText('Camera is off')).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Join now' }));
    expect(onJoin).toHaveBeenCalledWith({ micOn: false, cameraOn: false });
  });

  it('is keyboard operable in order', async () => {
    const onCancel = vi.fn();
    render(<Lobby onCancel={onCancel} />);
    const user = userEvent.setup();
    await user.tab();
    expect(screen.getByRole('button', { name: 'Join muted' })).toHaveFocus();
    await user.tab();
    expect(screen.getByRole('button', { name: 'Join with camera off' })).toHaveFocus();
    await user.tab();
    expect(screen.getByRole('button', { name: 'Cancel' })).toHaveFocus();
    await user.keyboard('{Enter}');
    expect(onCancel).toHaveBeenCalledOnce();
  });

  it('hides the camera toggle for audio calls and shows errors', () => {
    render(
      <Lobby
        onCameraOnChange={undefined}
        cameraOn={false}
        error="Microphone access is blocked."
        title="Start a call"
        joinLabel="Start call"
      />,
    );
    expect(screen.getByRole('region', { name: 'Start a call' })).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /camera/ })).not.toBeInTheDocument();
    expect(screen.getByRole('alert')).toHaveTextContent('Microphone access is blocked.');
    expect(screen.getByText('No preview')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Start call' })).toBeInTheDocument();
  });

  it('lists people already in the call and embeds device selects', () => {
    render(
      <Lobby
        participants={[{ name: 'Robin Park' }, { name: 'Alex Chen' }]}
        devicePicker={{
          devices: [{ deviceId: 'mic-1', kind: 'audioinput', label: 'Desk microphone' }],
          audioInputId: null,
          onAudioInputChange: () => {},
        }}
      />,
    );
    expect(screen.getByRole('group', { name: 'People in the call' })).toBeInTheDocument();
    expect(screen.getByText('2 already in the call')).toBeInTheDocument();
    expect(screen.getByRole('combobox', { name: 'Microphone' })).toBeInTheDocument();
  });

  it('can drop its heading inside a titled dialog', () => {
    render(<Lobby title={null} aria-label="Join planning" />);
    expect(screen.queryByRole('heading')).not.toBeInTheDocument();
    expect(screen.getByRole('region', { name: 'Join planning' })).toBeInTheDocument();
  });

  it('shows progress while joining', () => {
    render(<Lobby joining />);
    const join = screen.getByRole('button', { name: 'Joining…' });
    expect(join).toHaveAttribute('aria-busy', 'true');
    expect(screen.getByRole('button', { name: 'Cancel' })).toBeDisabled();
  });

  it('renders on the server', () => {
    const html = renderToString(<Lobby />);
    expect(html).toContain('mn-pre-join-lobby');
  });
});
