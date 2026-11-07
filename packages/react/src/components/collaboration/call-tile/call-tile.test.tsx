import { describe, expect, it, vi } from 'vitest';
import { act, render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { CallQualityBars, CallTile, CallVideo } from './index.js';

const fakeStream = (video = true) =>
  ({
    getVideoTracks: () => (video ? [{ readyState: 'live' }] : []),
    getAudioTracks: () => [],
    getTracks: () => [],
  }) as unknown as MediaStream;

describe('CallTile', () => {
  it('shows the avatar without video and names the tile', async () => {
    render(
      <CallTile
        name="Robin Park"
        micOff
        quality="poor"
        status="Reconnecting…"
        statusTone="warning"
      />,
    );
    const tile = screen.getByRole('group', { name: 'Robin Park' });
    expect(tile).toHaveClass('mn-call-tile');
    expect(tile).not.toHaveAttribute('data-video');
    expect(tile.querySelector('video')).toBeNull();
    expect(screen.getByRole('img', { name: 'Muted' })).toBeInTheDocument();
    expect(screen.getByRole('img', { name: 'Poor connection' })).toHaveAttribute(
      'data-quality',
      'poor',
    );
    expect(screen.getByText('Reconnecting…')).toHaveAttribute('data-tone', 'warning');
    await expectNoA11yViolations();
  });

  it('attaches a stream with live video and mirrors your own camera', () => {
    const stream = fakeStream();
    const { container } = render(<CallTile name="Sam" self stream={stream} />);
    const video = container.querySelector('video')!;
    expect(video).toHaveClass('mn-call-video');
    expect(video.srcObject).toBe(stream);
    expect(video.muted).toBe(true);
    expect(video).toHaveAttribute('data-mirror');
    expect(screen.getByRole('group', { name: 'Sam (you)' })).toHaveAttribute('data-video');
  });

  it('letterboxes a shared screen and never mirrors it', () => {
    const { container } = render(<CallTile name="Sam" self screen stream={fakeStream()} />);
    const video = container.querySelector('video')!;
    expect(video).toHaveAttribute('data-fit', 'contain');
    expect(video).not.toHaveAttribute('data-mirror');
    expect(screen.getByRole('img', { name: 'Sharing screen' })).toBeInTheDocument();
  });

  it('respects showVideo={false} even with a live stream', () => {
    const { container } = render(<CallTile name="Sam" stream={fakeStream()} showVideo={false} />);
    expect(container.querySelector('video')).toBeNull();
  });

  it('pins and unpins with a keyboard-reachable button', async () => {
    const onPinnedChange = vi.fn();
    const { rerender } = render(<CallTile name="Robin" onPinnedChange={onPinnedChange} />);
    const user = userEvent.setup();
    await user.tab();
    const pin = screen.getByRole('button', { name: 'Pin Robin' });
    expect(pin).toHaveFocus();
    expect(pin).toHaveAttribute('aria-pressed', 'false');
    await user.keyboard('{Enter}');
    expect(onPinnedChange).toHaveBeenCalledWith(true);
    rerender(<CallTile name="Robin" pinned onPinnedChange={onPinnedChange} />);
    await user.click(screen.getByRole('button', { name: 'Unpin Robin' }));
    expect(onPinnedChange).toHaveBeenLastCalledWith(false);
    expect(screen.getByRole('button', { name: 'Show Robin full screen' })).toBeInTheDocument();
  });

  it('uses a custom full-screen handler and hides actions on small tiles', async () => {
    const onFullscreen = vi.fn();
    const { rerender } = render(<CallTile name="Robin" fullscreen={onFullscreen} />);
    await userEvent.setup().click(screen.getByRole('button', { name: 'Show Robin full screen' }));
    expect(onFullscreen).toHaveBeenCalledOnce();
    rerender(<CallTile name="Robin" size="small" onPinnedChange={() => {}} />);
    expect(screen.queryByRole('button')).not.toBeInTheDocument();
  });

  it('shows a raised hand and announces a recent reaction, then clears it', () => {
    vi.useFakeTimers();
    try {
      render(<CallTile name="Robin" handRaised reaction={{ emoji: '🎉', at: Date.now() }} />);
      expect(screen.getByRole('img', { name: 'Robin raised a hand' })).toBeInTheDocument();
      expect(screen.getByText('Robin reacted 🎉')).toBeInTheDocument();
      act(() => {
        vi.advanceTimersByTime(3100);
      });
      expect(screen.queryByText('Robin reacted 🎉')).not.toBeInTheDocument();
    } finally {
      vi.useRealTimers();
    }
  });

  it('ignores stale reactions', () => {
    render(<CallTile name="Robin" reaction={{ emoji: '👍', at: Date.now() - 10_000 }} />);
    expect(screen.queryByText(/reacted/)).not.toBeInTheDocument();
  });

  it('translates strings through messages', () => {
    render(
      <CallTile name="Robin" self micOff messages={{ you: (n) => `${n} (du)`, muted: 'Stumm' }} />,
    );
    expect(screen.getByRole('group', { name: 'Robin (du)' })).toBeInTheDocument();
    expect(screen.getByRole('img', { name: 'Stumm' })).toBeInTheDocument();
  });

  it('renders on the server', () => {
    const html = renderToString(<CallTile name="Robin" stream={fakeStream()} quality="good" />);
    expect(html).toContain('mn-call-tile');
    expect(html).toContain('<video');
  });
});

describe('CallVideo and CallQualityBars', () => {
  it('forwards the video ref and renders nothing for unknown quality', () => {
    const ref = vi.fn();
    render(<CallVideo ref={ref} stream={null} />);
    expect(ref).toHaveBeenCalledWith(expect.any(HTMLVideoElement));
    const { container } = render(<CallQualityBars quality="unknown" />);
    expect(container).toBeEmptyDOMElement();
  });
});
