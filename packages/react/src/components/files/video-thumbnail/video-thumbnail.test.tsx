import { afterEach, describe, expect, it, vi } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { captureVideoFrame, formatVideoDuration, VideoThumbnail } from './index.js';

const POSTER = 'data:image/png;base64,iVBORw0KGgo=';

/** Makes created <video> elements behave like a loaded clip, and canvases draw. */
function fakeMedia({ taint = false } = {}) {
  const realCreate = document.createElement.bind(document);
  const spy = vi.spyOn(document, 'createElement').mockImplementation(((tag: string) => {
    const el = realCreate(tag);
    if (tag === 'video') {
      const video = el as HTMLVideoElement;
      Object.defineProperty(video, 'duration', { value: 20, configurable: true });
      Object.defineProperty(video, 'videoWidth', { value: 320, configurable: true });
      Object.defineProperty(video, 'videoHeight', { value: 180, configurable: true });
      let current = 0;
      Object.defineProperty(video, 'currentTime', {
        configurable: true,
        get: () => current,
        set: (v: number) => {
          current = v;
          queueMicrotask(() => video.dispatchEvent(new Event('seeked')));
        },
      });
      video.load = () => {
        if (video.getAttribute('src'))
          queueMicrotask(() => video.dispatchEvent(new Event('loadeddata')));
      };
    }
    if (tag === 'canvas') {
      const canvas = el as HTMLCanvasElement;
      canvas.getContext = (() => ({
        drawImage: () => {},
      })) as unknown as HTMLCanvasElement['getContext'];
      canvas.toDataURL = () => {
        if (taint) throw new DOMException('Tainted canvases may not be exported.', 'SecurityError');
        return 'data:image/jpeg;base64,FRAME';
      };
    }
    return el;
  }) as typeof document.createElement);
  return spy;
}

afterEach(() => vi.restoreAllMocks());

describe('formatVideoDuration', () => {
  it('formats seconds', () => {
    expect(formatVideoDuration(5)).toBe('0:05');
    expect(formatVideoDuration(65)).toBe('1:05');
    expect(formatVideoDuration(3725)).toBe('1:02:05');
  });
});

describe('captureVideoFrame', () => {
  it('seeks, draws and returns a data URL', async () => {
    fakeMedia();
    await expect(captureVideoFrame('clip.mp4')).resolves.toBe('data:image/jpeg;base64,FRAME');
  });
  it('rejects when the canvas is tainted', async () => {
    fakeMedia({ taint: true });
    await expect(captureVideoFrame('clip.mp4')).rejects.toThrow(/Tainted/);
  });
  it('rejects when aborted', async () => {
    const controller = new AbortController();
    controller.abort();
    await expect(captureVideoFrame('clip.mp4', { signal: controller.signal })).rejects.toThrow(
      'Aborted',
    );
  });
});

describe('VideoThumbnail', () => {
  it('shows a given poster with alt text, play icon and duration', async () => {
    render(<VideoThumbnail thumbnailUrl={POSTER} alt="Product tour" duration={65} />);
    const img = screen.getByRole('img', { name: 'Product tour' });
    expect(img).toHaveAttribute('src', POSTER);
    expect(img.closest('.mn-video-thumbnail')).toHaveAttribute('data-state', 'ready');
    expect(screen.getByText('1:05')).toBeInTheDocument();
    expect(document.querySelector('.mn-video-thumbnail__play')).toHaveAttribute(
      'aria-hidden',
      'true',
    );
    await expectNoA11yViolations();
  });

  it('captures a frame when there is no poster', async () => {
    fakeMedia();
    render(<VideoThumbnail src="clip.mp4" alt="Launch clip" />);
    expect(screen.getByRole('img', { name: 'Launch clip' })).toHaveClass(
      'mn-video-thumbnail__placeholder',
    );
    await waitFor(() =>
      expect(screen.getByRole('img', { name: 'Launch clip' })).toHaveAttribute(
        'src',
        'data:image/jpeg;base64,FRAME',
      ),
    );
  });

  it('keeps the placeholder and reports a failed capture', async () => {
    fakeMedia({ taint: true });
    const onCaptureError = vi.fn();
    render(<VideoThumbnail src="clip.mp4" alt="Launch clip" onCaptureError={onCaptureError} />);
    await waitFor(() => expect(onCaptureError).toHaveBeenCalledOnce());
    const placeholder = screen.getByRole('img', { name: 'Launch clip' });
    expect(placeholder.closest('.mn-video-thumbnail')).toHaveAttribute('data-state', 'placeholder');
  });

  it('is a named button with onPlay, operable from the keyboard', async () => {
    const onPlay = vi.fn();
    render(
      <VideoThumbnail thumbnailUrl={POSTER} alt="Product tour" duration={65} onPlay={onPlay} />,
    );
    const button = screen.getByRole('button', { name: 'Play Product tour, duration 1:05' });
    const user = userEvent.setup();
    await user.tab();
    expect(button).toHaveFocus();
    await user.keyboard('{Enter}');
    await user.keyboard(' ');
    expect(onPlay).toHaveBeenCalledTimes(2);
    expect(button.querySelector('img')).toHaveAttribute('alt', '');
    await expectNoA11yViolations();
  });

  it('falls back to a generic name and translates strings', () => {
    render(
      <VideoThumbnail
        thumbnailUrl={POSTER}
        alt=" "
        onPlay={() => {}}
        messages={{ play: (a) => `${a} abspielen`, fallbackAlt: 'Video' }}
      />,
    );
    expect(screen.getByRole('button', { name: 'Video abspielen' })).toBeInTheDocument();
  });

  it('renders the placeholder on the server', () => {
    const html = renderToString(<VideoThumbnail src="clip.mp4" alt="Launch clip" />);
    expect(html).toContain('mn-video-thumbnail__placeholder');
    expect(html).toContain('aspect-ratio:16 / 9');
  });
});
