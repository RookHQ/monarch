import { afterEach, describe, expect, it, vi } from 'vitest';
import { useState } from 'react';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { MediaUpload, mediaKindOf, readVideoDuration, type MediaUploadProps } from './index.js';

const image = (name = 'photo.png', size = 1000) =>
  new File([new Uint8Array(size)], name, { type: 'image/png' });
const video = (name = 'clip.mp4', size = 1000) =>
  new File([new Uint8Array(size)], name, { type: 'video/mp4' });

function deferred<T>() {
  let resolve!: (v: T) => void;
  let reject!: (e: unknown) => void;
  const promise = new Promise<T>((res, rej) => {
    resolve = res;
    reject = rej;
  });
  return { promise, resolve, reject };
}

const fileInput = () => document.querySelector<HTMLInputElement>('input[type="file"]')!;

function Controlled(props: Partial<MediaUploadProps>) {
  const [value, setValue] = useState<string | null>(null);
  return (
    <MediaUpload
      label="Cover image"
      value={value}
      onChange={(url) => setValue(url)}
      onUpload={async () => 'https://cdn.example.com/cover.png'}
      {...props}
    />
  );
}

afterEach(() => vi.restoreAllMocks());

describe('mediaKindOf', () => {
  it('detects video by type or extension', () => {
    expect(mediaKindOf('video/mp4')).toBe('video');
    expect(mediaKindOf('https://x.test/a.webm?v=2')).toBe('video');
    expect(mediaKindOf('data:video/mp4;base64,AA')).toBe('video');
    expect(mediaKindOf('https://x.test/a.png')).toBe('image');
  });
});

describe('MediaUpload', () => {
  it('uploads a chosen image, shows progress, then previews it and moves focus to Replace', async () => {
    const upload = deferred<string>();
    let report: (n: number) => void = () => {};
    const onUpload = vi.fn((_file: File, ctx: { onProgress: (n: number) => void }) => {
      report = ctx.onProgress;
      return upload.promise;
    });
    render(<Controlled onUpload={onUpload} maxImageSize={10 * 1024 * 1024} />);
    expect(screen.getByText('PNG, JPG, GIF or WebP up to 10 MB')).toBeInTheDocument();
    await expectNoA11yViolations();
    const user = userEvent.setup();
    await user.upload(fileInput(), image());
    expect(onUpload).toHaveBeenCalledWith(
      expect.any(File),
      expect.objectContaining({ kind: 'image' }),
    );
    const bar = await screen.findByRole('progressbar', { name: 'Uploading photo.png' });
    report(40);
    await waitFor(() => expect(bar).toHaveAttribute('aria-valuenow', '40'));
    expect(screen.getByText('Uploading photo.png · 40%')).toBeInTheDocument();
    upload.resolve('https://cdn.example.com/cover.png');
    const img = await screen.findByRole('img', { name: 'Uploaded image' });
    expect(img).toHaveAttribute('src', 'https://cdn.example.com/cover.png');
    expect(screen.getByText('Image uploaded')).toBeInTheDocument();
    await waitFor(() =>
      expect(screen.getByRole('button', { name: 'Replace image' })).toHaveFocus(),
    );
    await expectNoA11yViolations();
  });

  it('removes the media and returns focus to the chooser', async () => {
    const onChange = vi.fn();
    render(
      <MediaUpload
        label="Cover image"
        defaultValue="https://cdn.example.com/cover.png"
        onChange={onChange}
        onUpload={async () => ''}
      />,
    );
    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: 'Remove image' }));
    expect(onChange).toHaveBeenCalledWith(null, { kind: null });
    await waitFor(() => expect(screen.getByRole('button', { name: /Choose image/ })).toHaveFocus());
    expect(screen.getByText('Image removed')).toBeInTheDocument();
  });

  it('refuses a video with the product’s reason when videos are unavailable', async () => {
    const onUpload = vi.fn();
    render(<Controlled onUpload={onUpload} videoUnavailable="Videos need a different plan." />);
    expect(screen.getByText('Videos need a different plan.')).toBeInTheDocument();
    await userEvent.setup({ applyAccept: false }).upload(fileInput(), video());
    expect(
      screen.getByText('clip.mp4 is a video. Only images can be added here.'),
    ).toBeInTheDocument();
    expect(onUpload).not.toHaveBeenCalled();
  });

  it('checks size per kind and video length before uploading', async () => {
    const onUpload = vi.fn(async () => 'https://cdn.example.com/clip.mp4');
    const realCreate = document.createElement.bind(document);
    vi.spyOn(document, 'createElement').mockImplementation(((tag: string) => {
      const el = realCreate(tag);
      if (tag === 'video') {
        Object.defineProperty(el, 'duration', { value: 120 });
        Object.defineProperty(el, 'src', {
          set: () =>
            queueMicrotask(() => (el as HTMLVideoElement).onloadedmetadata?.(new Event('x'))),
        });
      }
      return el;
    }) as typeof document.createElement);
    URL.createObjectURL = vi.fn(() => 'blob:x');
    URL.revokeObjectURL = vi.fn();
    render(
      <Controlled
        kinds={['image', 'video']}
        maxVideoSize={500}
        maxVideoDuration={90}
        onUpload={onUpload}
      />,
    );
    expect(screen.getByText('MP4, MOV or WebM up to 500 B, 1:30 min at most')).toBeInTheDocument();
    const user = userEvent.setup();
    await user.upload(fileInput(), video('big.mp4', 1000));
    expect(screen.getByText('big.mp4 is larger than 500 B.')).toBeInTheDocument();
    await user.upload(fileInput(), video('long.mp4', 100));
    expect(await screen.findByText('long.mp4 is longer than 1:30 min.')).toBeInTheDocument();
    expect(onUpload).not.toHaveBeenCalled();
    expect(URL.revokeObjectURL).toHaveBeenCalledWith('blob:x');
  });

  it('shows an upload error and lets the person cancel a running upload', async () => {
    const onUpload = vi.fn(
      (_f: File, { signal }: { signal: AbortSignal }) =>
        new Promise<string>((_, reject) => {
          signal.addEventListener('abort', () => reject(new Error('aborted')));
        }),
    );
    const { rerender } = render(<Controlled onUpload={onUpload} />);
    const user = userEvent.setup();
    await user.upload(fileInput(), image());
    await user.click(await screen.findByRole('button', { name: 'Cancel upload' }));
    expect(onUpload.mock.calls[0]?.[1].signal.aborted).toBe(true);
    expect(screen.getByText('Upload cancelled')).toBeInTheDocument();
    expect(screen.queryByRole('alert')).not.toBeInTheDocument();

    rerender(<Controlled onUpload={() => Promise.reject(new Error('Storage is full.'))} />);
    await user.upload(fileInput(), image());
    expect(await screen.findByText('Storage is full.')).toBeInTheDocument();
  });

  it('runs transform first and shows warnings from the result', async () => {
    const small = image('small.png', 10);
    const transform = vi.fn(async () => small);
    const onUpload = vi.fn(async () => ({
      url: 'https://cdn.example.com/a.png',
      warnings: ['Metadata was removed.'],
    }));
    render(<Controlled transform={transform} onUpload={onUpload} />);
    await userEvent.setup().upload(fileInput(), image('large.png', 5000));
    expect(transform).toHaveBeenCalledWith(expect.any(File), 'image');
    await waitFor(() => expect(onUpload).toHaveBeenCalledWith(small, expect.anything()));
    expect(await screen.findByText('Metadata was removed.')).toBeInTheDocument();
  });

  it('previews a video value with controls', () => {
    render(
      <MediaUpload
        label="Clip"
        value="https://cdn.example.com/clip.webm"
        onUpload={async () => ''}
      />,
    );
    const el = document.querySelector('video.mn-media-upload__media');
    expect(el).toHaveAttribute('controls');
    expect(screen.getByRole('button', { name: 'Replace video' })).toBeInTheDocument();
  });

  it('readVideoDuration rejects outside the browser APIs', async () => {
    const original = URL.createObjectURL;
    // @ts-expect-error simulate a missing API
    URL.createObjectURL = undefined;
    await expect(readVideoDuration(new Blob())).rejects.toThrow();
    URL.createObjectURL = original;
  });

  it('renders on the server', () => {
    const html = renderToString(<MediaUpload label="Cover image" onUpload={async () => ''} />);
    expect(html).toContain('mn-media-upload');
    expect(html).toContain('Choose image');
  });
});
