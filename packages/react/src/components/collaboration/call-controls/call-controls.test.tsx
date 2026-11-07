import { describe, expect, it, vi } from 'vitest';
import { useRef, useState } from 'react';
import { fireEvent, render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import {
  CallControlButton,
  CallControls,
  CallIcon,
  useCallShortcuts,
  type CallControlsProps,
  type CallShortcutHandlers,
  type CallViewMode,
} from './index.js';

function Controlled(props: Partial<CallControlsProps>) {
  const [micOn, setMic] = useState(true);
  const [cameraOn, setCamera] = useState(true);
  const [sharing, setSharing] = useState(false);
  const [hand, setHand] = useState(false);
  const [mode, setMode] = useState<CallViewMode>('auto');
  return (
    <CallControls
      micOn={micOn}
      onMicChange={setMic}
      cameraOn={cameraOn}
      onCameraChange={setCamera}
      sharing={sharing}
      onSharingChange={setSharing}
      handRaised={hand}
      onHandRaisedChange={setHand}
      mode={mode}
      onModeChange={setMode}
      onLeave={() => {}}
      {...props}
    />
  );
}

describe('CallControls', () => {
  it('renders a named toolbar of toggles with pressed states', async () => {
    render(<Controlled />);
    const user = userEvent.setup();
    expect(screen.getByRole('toolbar', { name: 'Call controls' })).toHaveClass('mn-call-controls');
    const mute = screen.getByRole('button', { name: 'Mute' });
    expect(mute).toHaveAttribute('aria-pressed', 'false');
    expect(mute).toHaveAttribute('title', 'Mute (M, or hold Space to talk)');
    await user.click(mute);
    const unmute = screen.getByRole('button', { name: 'Unmute' });
    expect(unmute).toHaveAttribute('aria-pressed', 'true');
    expect(unmute).toHaveAttribute('data-tone', 'off');

    await user.click(screen.getByRole('button', { name: 'Turn camera off' }));
    expect(screen.getByRole('button', { name: 'Turn camera on' })).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Share screen' }));
    expect(screen.getByRole('button', { name: 'Stop sharing' })).toHaveAttribute(
      'data-tone',
      'active',
    );
    await user.click(screen.getByRole('button', { name: 'Raise hand' }));
    expect(screen.getByRole('button', { name: 'Lower hand' })).toHaveAttribute(
      'data-tone',
      'raised',
    );
    expect(screen.getByRole('button', { name: 'Leave call' })).toHaveAttribute(
      'data-tone',
      'danger',
    );
    await expectNoA11yViolations();
  });

  it('shows only the controls that have callbacks', () => {
    render(<CallControls micOn onMicChange={() => {}} />);
    expect(screen.getAllByRole('button')).toHaveLength(1);
    expect(screen.queryByRole('button', { name: 'More options' })).not.toBeInTheDocument();
  });

  it('moves focus with arrow keys, Home and End', async () => {
    render(<Controlled />);
    const user = userEvent.setup();
    await user.tab();
    expect(screen.getByRole('button', { name: 'Mute' })).toHaveFocus();
    await user.keyboard('{ArrowRight}');
    expect(screen.getByRole('button', { name: 'Turn camera off' })).toHaveFocus();
    await user.keyboard('{End}');
    expect(screen.getByRole('button', { name: 'Leave call' })).toHaveFocus();
    await user.keyboard('{ArrowRight}');
    expect(screen.getByRole('button', { name: 'Mute' })).toHaveFocus();
    await user.keyboard('{ArrowLeft}');
    expect(screen.getByRole('button', { name: 'Leave call' })).toHaveFocus();
    await user.keyboard('{Home}');
    expect(screen.getByRole('button', { name: 'Mute' })).toHaveFocus();
  });

  it('sends reactions and changes layout from the menus', async () => {
    const onReact = vi.fn();
    const onModeChange = vi.fn();
    const onOpenDevices = vi.fn();
    render(
      <Controlled onReact={onReact} onModeChange={onModeChange} onOpenDevices={onOpenDevices} />,
    );
    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: 'Send a reaction' }));
    await user.click(await screen.findByRole('menuitem', { name: 'React 🎉' }));
    expect(onReact).toHaveBeenCalledWith('🎉');

    await user.click(screen.getByRole('button', { name: 'More options' }));
    const menu = await screen.findByRole('menu');
    expect(screen.getByRole('menuitemradio', { name: 'Automatic' })).toHaveAttribute(
      'aria-checked',
      'true',
    );
    await expectNoA11yViolations(menu);
    await user.click(screen.getByRole('menuitemradio', { name: 'Grid' }));
    expect(onModeChange).toHaveBeenCalledWith('grid');

    await user.click(screen.getByRole('button', { name: 'More options' }));
    await user.click(await screen.findByRole('menuitem', { name: 'Devices' }));
    expect(onOpenDevices).toHaveBeenCalledOnce();
  });

  it('hides hand, reactions and More when minimal', () => {
    render(<Controlled minimal onReact={() => {}} />);
    expect(screen.queryByRole('button', { name: 'Raise hand' })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Send a reaction' })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'More options' })).not.toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Leave call' })).toBeInTheDocument();
  });

  it('translates strings through messages', () => {
    render(
      <CallControls micOn onMicChange={() => {}} messages={{ mute: 'Stumm', toolbar: 'Anruf' }} />,
    );
    expect(screen.getByRole('toolbar', { name: 'Anruf' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Stumm' })).toBeInTheDocument();
  });

  it('renders on the server', () => {
    const html = renderToString(<Controlled onReact={() => {}} />);
    expect(html).toContain('mn-call-controls');
    expect(html).toContain('aria-label="Leave call"');
  });
});

describe('CallControlButton and CallIcon', () => {
  it('is a named button with a decorative icon', async () => {
    render(<CallControlButton label="Record" icon={<CallIcon name="activity" />} />);
    const button = screen.getByRole('button', { name: 'Record' });
    expect(button).toHaveAttribute('type', 'button');
    expect(button.querySelector('svg')).toHaveAttribute('aria-hidden', 'true');
    await expectNoA11yViolations();
  });
});

function ShortcutHost(handlers: Omit<CallShortcutHandlers, 'root'>) {
  const ref = useRef<HTMLDivElement>(null);
  useCallShortcuts({ ...handlers, root: () => ref.current });
  return (
    <div ref={ref}>
      <div tabIndex={-1} data-testid="stage">
        stage
      </div>
      <input aria-label="Notes" />
    </div>
  );
}

describe('useCallShortcuts', () => {
  it('fires single-key shortcuts only with focus inside the call and not while typing', () => {
    const onToggleMic = vi.fn();
    const onToggleCamera = vi.fn();
    const onPushToTalk = vi.fn();
    render(
      <>
        <ShortcutHost
          onToggleMic={onToggleMic}
          onToggleCamera={onToggleCamera}
          onPushToTalk={onPushToTalk}
        />
        <button type="button">Outside</button>
      </>,
    );
    const stage = screen.getByTestId('stage');
    fireEvent.keyDown(stage, { key: 'm' });
    fireEvent.keyDown(stage, { key: 'v' });
    expect(onToggleMic).toHaveBeenCalledOnce();
    expect(onToggleCamera).toHaveBeenCalledOnce();

    fireEvent.keyDown(screen.getByRole('textbox', { name: 'Notes' }), { key: 'm' });
    fireEvent.keyDown(screen.getByRole('button', { name: 'Outside' }), { key: 'm' });
    expect(onToggleMic).toHaveBeenCalledOnce();

    fireEvent.keyDown(screen.getByRole('button', { name: 'Outside' }), {
      key: ' ',
      code: 'Space',
      ctrlKey: true,
      shiftKey: true,
    });
    expect(onToggleMic).toHaveBeenCalledTimes(2);

    fireEvent.keyDown(stage, { key: ' ', code: 'Space' });
    expect(onPushToTalk).toHaveBeenLastCalledWith(true);
    fireEvent.keyUp(stage, { key: ' ', code: 'Space' });
    expect(onPushToTalk).toHaveBeenLastCalledWith(false);
  });
});
