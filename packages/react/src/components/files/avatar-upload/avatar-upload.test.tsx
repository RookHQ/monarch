import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { act, fireEvent, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { createRef } from 'react';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { AvatarUpload, avatarCropRect } from './index.js';

const PNG = new File(['x'], 'me.png', { type: 'image/png' });
const PDF = new File(['x'], 'cv.pdf', { type: 'application/pdf' });
const input = () => document.querySelector<HTMLInputElement>('input[type="file"]')!;

/** Fires load on the crop image with a natural size (jsdom does not decode images). */
const loadImage = (width: number, height: number) => {
  const img = document.querySelector<HTMLImageElement>('.mn-avatar-upload__image')!;
  Object.defineProperty(img, 'naturalWidth', { value: width, configurable: true });
  Object.defineProperty(img, 'naturalHeight', { value: height, configurable: true });
  fireEvent.load(img);
  return img;
};

/** jsdom has no PointerEvent coordinates: send mouse events under pointer names. */
const pointer = (el: Element, type: string, x: number, y: number) => {
  const ev = new MouseEvent(type, { bubbles: true, cancelable: true, clientX: x, clientY: y });
  Object.defineProperty(ev, 'pointerId', { value: 1 });
  fireEvent(el, ev);
};

const drawImage = vi.fn();
beforeEach(() => {
  URL.createObjectURL = vi.fn(() => 'blob:https://example.com/1');
  URL.revokeObjectURL = vi.fn();
  vi.spyOn(HTMLCanvasElement.prototype, 'getContext').mockImplementation(
    () => ({ drawImage, imageSmoothingQuality: 'low' }) as unknown as CanvasRenderingContext2D,
  );
  HTMLCanvasElement.prototype.toBlob = function (cb: BlobCallback, type?: string) {
    cb(new Blob(['png'], { type: type ?? 'image/png' }));
  };
});
afterEach(() => {
  vi.restoreAllMocks();
  drawImage.mockReset();
});

describe('avatarCropRect', () => {
  it('maps the view to a square of the image', () => {
    expect(avatarCropRect({ width: 400, height: 200 }, { zoom: 1, x: 0, y: 0 })).toEqual({
      x: 100,
      y: 0,
      size: 200,
    });
    expect(avatarCropRect({ width: 400, height: 200 }, { zoom: 2, x: 0.5, y: 0 })).toEqual({
      x: 100,
      y: 50,
      size: 100,
    });
  });
});

describe('AvatarUpload', () => {
  it('shows the current picture with change and remove buttons', async () => {
    const user = userEvent.setup();
    const onRemove = vi.fn();
    const ref = createRef<HTMLDivElement>();
    render(<AvatarUpload ref={ref} name="Sam Lee" src="/a.png" onRemove={onRemove} />);
    expect(ref.current).toHaveClass('mn-avatar-upload');
    expect(ref.current).toHaveAttribute('data-shape', 'circle');
    expect(screen.getByRole('img', { name: 'Sam Lee' })).toBeInTheDocument();
    const click = vi.spyOn(input(), 'click');
    await user.tab();
    expect(screen.getByRole('button', { name: 'Change photo' })).toHaveFocus();
    await user.keyboard('{Enter}');
    expect(click).toHaveBeenCalled();
    await user.click(screen.getByRole('button', { name: 'Remove photo' }));
    expect(onRemove).toHaveBeenCalled();
    await expectNoA11yViolations();
  });

  it('refuses files that are not allowed with an alert', async () => {
    const user = userEvent.setup({ applyAccept: false });
    render(<AvatarUpload name="Sam Lee" />);
    expect(screen.getByRole('button', { name: 'Upload photo' })).toBeInTheDocument();
    await user.upload(input(), PDF);
    expect(screen.getByRole('alert')).toHaveTextContent('cv.pdf is not an allowed file type.');
    expect(screen.getByRole('button', { name: 'Upload photo' })).toHaveAccessibleDescription(
      /cv\.pdf/,
    );
  });

  it('crops with the keyboard and slider, then saves a square image', async () => {
    const user = userEvent.setup();
    const onChange = vi.fn();
    render(<AvatarUpload name="Sam Lee" onChange={onChange} outputSize={256} />);
    await user.upload(input(), PNG);
    const area = screen.getByRole('group', { name: 'Photo position' });
    await waitFor(() => expect(area).toHaveFocus());
    expect(area).toHaveAccessibleDescription(/arrow keys/);
    expect(screen.getByRole('button', { name: 'Save photo' })).toBeDisabled();
    const img = loadImage(400, 200);
    await expectNoA11yViolations();

    // Wide image at zoom 1: it can move sideways by half its extra width only.
    await user.keyboard('{Shift>}{ArrowRight}{ArrowRight}{ArrowRight}{ArrowRight}{/Shift}');
    expect(img.style.left).toBe('0%');
    await user.keyboard('{ArrowDown}');
    expect(img.style.top).toBe('0%');
    const zoom = screen.getByRole('slider', { name: 'Zoom' });
    fireEvent.change(zoom, { target: { value: '2' } });
    expect(zoom).toHaveAttribute('aria-valuetext', '200%');
    area.focus();
    await user.keyboard('-');
    expect(zoom).toHaveValue('1.9');
    await user.keyboard('{Home}');
    expect(zoom).toHaveValue('1');

    await user.click(screen.getByRole('button', { name: 'Save photo' }));
    await waitFor(() => expect(onChange).toHaveBeenCalled());
    const [blob, file, crop] = onChange.mock.calls[0]!;
    expect(blob).toBeInstanceOf(Blob);
    expect(file).toBe(PNG);
    expect(crop).toEqual({ x: 100, y: 0, size: 200 });
    expect(drawImage).toHaveBeenCalledWith(img, 100, 0, 200, 200, 0, 0, 256, 256);
    await waitFor(() => expect(screen.getByRole('button', { name: 'Upload photo' })).toHaveFocus());
    expect(URL.revokeObjectURL).toHaveBeenCalled();
  });

  it('pans by dragging', async () => {
    render(<AvatarUpload name="Sam Lee" />);
    const user = userEvent.setup();
    await user.upload(input(), PNG);
    const img = loadImage(200, 400);
    const area = screen.getByRole('group');
    Object.defineProperty(area, 'clientWidth', { value: 200 });
    pointer(area, 'pointerdown', 100, 100);
    pointer(area, 'pointermove', 100, 140);
    pointer(area, 'pointerup', 100, 140);
    pointer(area, 'pointermove', 100, 300);
    expect(parseFloat(img.style.top)).toBeCloseTo(-30);
  });

  it('cancels with Escape and keeps the editor open when saving fails', async () => {
    const user = userEvent.setup();
    const onChange = vi.fn().mockRejectedValue(new Error('network'));
    render(<AvatarUpload name="Sam Lee" shape="square" onChange={onChange} />);
    await user.upload(input(), PNG);
    loadImage(100, 100);
    await waitFor(() => expect(screen.getByRole('group')).toHaveFocus());
    await user.keyboard('{Enter}');
    expect(await screen.findByRole('alert')).toHaveTextContent('The photo could not be saved.');
    expect(screen.getByRole('group')).toBeInTheDocument();
    screen.getByRole('group').focus();
    await user.keyboard('{Escape}');
    expect(screen.queryByRole('group')).toBeNull();
    expect(onChange).toHaveBeenCalledTimes(1);
  });

  it('adds up pointer moves that arrive before a re-render', async () => {
    render(<AvatarUpload name="Sam Lee" />);
    const user = userEvent.setup();
    await user.upload(input(), PNG);
    const img = loadImage(200, 400);
    const area = screen.getByRole('group');
    Object.defineProperty(area, 'clientWidth', { value: 200 });
    pointer(area, 'pointerdown', 100, 100);
    // Two moves in one batch: both deltas count, not only the last one.
    act(() => {
      const move = (y: number) => {
        const ev = new MouseEvent('pointermove', { bubbles: true, clientX: 100, clientY: y });
        Object.defineProperty(ev, 'pointerId', { value: 1 });
        area.dispatchEvent(ev);
      };
      move(120);
      move(140);
    });
    expect(parseFloat(img.style.top)).toBeCloseTo(-30);
  });

  it('saves once when Enter is pressed again while saving, and ignores Escape until it settles', async () => {
    const user = userEvent.setup();
    let finish: () => void = () => {};
    const onChange = vi.fn(() => new Promise<void>((resolve) => (finish = resolve)));
    render(<AvatarUpload name="Sam Lee" onChange={onChange} />);
    await user.upload(input(), PNG);
    loadImage(100, 100);
    const area = screen.getByRole('group');
    await waitFor(() => expect(area).toHaveFocus());
    await user.keyboard('{Enter}');
    await waitFor(() => expect(onChange).toHaveBeenCalledTimes(1));
    area.focus();
    await user.keyboard('{Enter}{Escape}');
    expect(onChange).toHaveBeenCalledTimes(1);
    expect(screen.getByRole('group')).toBeInTheDocument();
    await act(async () => finish());
    await waitFor(() => expect(screen.queryByRole('group')).toBeNull());
    expect(onChange).toHaveBeenCalledTimes(1);
  });

  it('translates and disables', () => {
    render(<AvatarUpload name="Sam Lee" disabled messages={{ upload: 'Ajouter une photo' }} />);
    expect(screen.getByRole('button', { name: 'Ajouter une photo' })).toBeDisabled();
  });

  it('renders on the server', () => {
    const html = renderToString(<AvatarUpload name="Sam Lee" src="/a.png" />);
    expect(html).toContain('mn-avatar-upload');
    expect(html).toContain('Change photo');
  });
});
