import { afterEach, describe, expect, it, vi } from 'vitest';
import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { createRef } from 'react';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { ScreenShareView } from './index.js';

const STREAM = { id: 'screen', getTracks: () => [] } as unknown as MediaStream;

afterEach(() => {
  vi.restoreAllMocks();
  delete (document as { pictureInPictureEnabled?: boolean }).pictureInPictureEnabled;
  delete (HTMLVideoElement.prototype as { requestPictureInPicture?: unknown })
    .requestPictureInPicture;
});

describe('ScreenShareView', () => {
  it('labels the presenter and attaches the stream to a named region', async () => {
    const ref = createRef<HTMLDivElement>();
    const { container } = render(
      <ScreenShareView ref={ref} stream={STREAM} presenterName="Priya Shah" />,
    );
    expect(ref.current).toHaveClass('mn-screen-share-view');
    expect(screen.getByText('Priya Shah is presenting')).toBeInTheDocument();
    expect(screen.getByRole('region', { name: 'Screen shared by Priya Shah' })).toBeInTheDocument();
    const video = container.querySelector('video')!;
    expect(video.srcObject).toBe(STREAM);
    expect(video).toHaveAttribute('data-fit', 'contain');
    expect(screen.getByRole('group', { name: 'Shared screen controls' })).toBeInTheDocument();
    await expectNoA11yViolations();
  });

  it('zooms with buttons and keys, and fits again', async () => {
    const user = userEvent.setup();
    const onZoomChange = vi.fn();
    const { container } = render(
      <ScreenShareView stream={STREAM} presenterName="Priya" onZoomChange={onZoomChange} />,
    );
    const root = container.firstElementChild!;
    expect(screen.getByRole('button', { name: 'Zoom out' })).toBeDisabled();
    expect(screen.getByRole('button', { name: 'Fit to view' })).toBeDisabled();
    await user.click(screen.getByRole('button', { name: 'Zoom in' }));
    expect(onZoomChange).toHaveBeenLastCalledWith(1.5);
    expect(screen.getByText('150%')).toBeInTheDocument();
    expect(root).toHaveAttribute('data-zoomed');
    expect(container.querySelector('video')!.style.width).toBe('150%');
    screen.getByRole('region').focus();
    await user.keyboard('+++');
    expect(onZoomChange).toHaveBeenLastCalledWith(3);
    expect(screen.getByRole('button', { name: 'Zoom in' })).toBeDisabled();
    await user.keyboard('-');
    expect(onZoomChange).toHaveBeenLastCalledWith(2);
    await user.keyboard('0');
    expect(root).not.toHaveAttribute('data-zoomed');
    await user.click(screen.getByRole('button', { name: 'Zoom in' }));
    await user.click(screen.getByRole('button', { name: 'Fit to view' }));
    expect(onZoomChange).toHaveBeenLastCalledWith(1);
  });

  it('leaves the browser’s own zoom shortcuts (Ctrl or Cmd with +, - and 0) alone', () => {
    const onZoomChange = vi.fn();
    render(<ScreenShareView stream={STREAM} presenterName="Priya" onZoomChange={onZoomChange} />);
    const region = screen.getByRole('region');
    for (const init of [
      { key: '+', ctrlKey: true },
      { key: '-', metaKey: true },
      { key: '0', ctrlKey: true },
    ]) {
      // fireEvent returns false when the handler called preventDefault().
      expect(fireEvent.keyDown(region, init)).toBe(true);
    }
    expect(onZoomChange).not.toHaveBeenCalled();
  });

  it('offers picture-in-picture only where supported', async () => {
    const user = userEvent.setup();
    const { unmount } = render(<ScreenShareView stream={STREAM} presenterName="Priya" />);
    expect(screen.queryByRole('button', { name: 'Picture-in-picture' })).toBeNull();
    unmount();
    (document as { pictureInPictureEnabled?: boolean }).pictureInPictureEnabled = true;
    const request = vi.fn(() => Promise.resolve({}));
    (HTMLVideoElement.prototype as { requestPictureInPicture?: unknown }).requestPictureInPicture =
      request;
    render(<ScreenShareView stream={STREAM} presenterName="Priya" />);
    const pip = await screen.findByRole('button', { name: 'Picture-in-picture' });
    expect(pip).toHaveAttribute('aria-pressed', 'false');
    await user.click(pip);
    expect(request).toHaveBeenCalled();
    await waitFor(() =>
      expect(screen.getByRole('button', { name: 'Exit picture-in-picture' })).toHaveAttribute(
        'aria-pressed',
        'true',
      ),
    );
  });

  it('hides your own share by default and offers Stop sharing', async () => {
    const user = userEvent.setup();
    const onStopSharing = vi.fn();
    const { container } = render(
      <ScreenShareView stream={STREAM} presenterName="Me" isLocal onStopSharing={onStopSharing} />,
    );
    expect(screen.getByText('You are presenting')).toBeInTheDocument();
    expect(container.querySelector('video')).toBeNull();
    expect(screen.getByText(/preview is hidden/)).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Stop sharing' }));
    expect(onStopSharing).toHaveBeenCalled();
    await user.click(screen.getByRole('button', { name: 'Show preview' }));
    expect(container.querySelector('video')).not.toBeNull();
    await expectNoA11yViolations();
  });

  it('waits for a stream, can hide controls and translates', () => {
    const { rerender } = render(
      <ScreenShareView stream={null} presenterName="Priya" messages={{ waiting: 'En attente…' }} />,
    );
    expect(screen.getByText('En attente…')).toBeInTheDocument();
    expect(screen.queryByRole('button')).toBeNull();
    rerender(<ScreenShareView stream={STREAM} presenterName="Priya" hideControls />);
    expect(screen.queryByRole('button')).toBeNull();
  });

  it('renders on the server', () => {
    const html = renderToString(<ScreenShareView stream={null} presenterName="Priya" />);
    expect(html).toContain('mn-screen-share-view');
    expect(html).toContain('Priya is presenting');
  });
});
