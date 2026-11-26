import { describe, expect, it } from 'vitest';
import { fireEvent, render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { createRef } from 'react';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { FilePreview, filePreviewKind } from './index.js';

describe('filePreviewKind', () => {
  it('maps families to preview kinds', () => {
    expect(filePreviewKind({ name: 'a.png' })).toBe('image');
    expect(filePreviewKind({ name: 'a', type: 'application/pdf' })).toBe('pdf');
    expect(filePreviewKind({ name: 'a.ts' })).toBe('text');
    expect(filePreviewKind({ name: 'a.webm' })).toBe('video');
    expect(filePreviewKind({ name: 'a.mp3' })).toBe('audio');
    expect(filePreviewKind({ name: 'a.docx' })).toBe('none');
  });
});

describe('FilePreview', () => {
  it('shows an image with alt text and falls back when it fails', async () => {
    const ref = createRef<HTMLDivElement>();
    render(<FilePreview ref={ref} name="map.png" src="https://example.com/map.png" />);
    expect(ref.current).toHaveClass('mn-file-preview');
    expect(ref.current).toHaveAttribute('data-kind', 'image');
    const img = screen.getByRole('img', { name: 'map.png' });
    await expectNoA11yViolations();
    fireEvent.error(img);
    expect(ref.current).toHaveAttribute('data-kind', 'none');
    expect(ref.current).toHaveAttribute('data-failed');
    expect(screen.getByText('The preview could not be loaded.')).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Download map.png' })).toHaveAttribute(
      'download',
      'map.png',
    );
  });

  it('shows a PDF in the browser viewer with a download fallback inside', () => {
    const { container } = render(<FilePreview name="spec.pdf" src="/files/spec.pdf" />);
    const object = container.querySelector('object')!;
    expect(object).toHaveAttribute('type', 'application/pdf');
    expect(object).toHaveAttribute('data', '/files/spec.pdf');
    expect(object).toHaveAttribute('title', 'Preview of spec.pdf');
    expect(object.querySelector('a[download]')).toHaveAttribute('href', '/files/spec.pdf');
  });

  it('shows given text in a focusable scroll area, or a sandboxed frame from src', async () => {
    const user = userEvent.setup();
    const { container, rerender } = render(<FilePreview name="notes.md" text={'# Title\nBody'} />);
    const pre = screen.getByLabelText('Preview of notes.md');
    expect(pre.tagName).toBe('PRE');
    expect(pre).toHaveTextContent('# Title Body');
    await user.tab();
    expect(pre).toHaveFocus();
    await expectNoA11yViolations();
    rerender(<FilePreview name="log.txt" src="/files/log.txt" />);
    const frame = container.querySelector('iframe')!;
    expect(frame).toHaveAttribute('sandbox', '');
    expect(frame).toHaveAttribute('title', 'Preview of log.txt');
  });

  it('plays video with captions and audio with controls', async () => {
    const { container, rerender } = render(
      <FilePreview name="demo.mp4" src="/v/demo.mp4" captionsSrc="/v/demo.vtt" />,
    );
    const video = container.querySelector('video')!;
    expect(video).toHaveAttribute('controls');
    expect(video).toHaveAccessibleName('Preview of demo.mp4');
    expect(container.querySelector('track')).toHaveAttribute('kind', 'captions');
    rerender(<FilePreview name="memo.m4a" src="blob:https://example.com/1" />);
    expect(container.querySelector('audio')).toHaveAttribute('controls');
    await expectNoA11yViolations();
  });

  it('falls back for unsupported types, unsafe URLs and missing sources', async () => {
    const { container, rerender } = render(
      <FilePreview name="contract.docx" src="https://example.com/c.docx" />,
    );
    expect(screen.getByText('There is no preview for this file type.')).toBeInTheDocument();
    expect(screen.getByRole('img', { name: 'Document' })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Download contract.docx' })).toBeInTheDocument();
    await expectNoA11yViolations();
    rerender(<FilePreview name="x.png" src="javascript:alert(1)" />);
    expect(container.querySelector('img.mn-file-preview__image')).toBeNull();
    expect(screen.queryByRole('link')).toBeNull();
    rerender(<FilePreview name="x.png" fallback={<p>Ask the owner for access</p>} />);
    expect(screen.getByText('Ask the owner for access')).toBeInTheDocument();
  });

  it('can force a kind, always show the download, and translate', () => {
    const { container } = render(
      <FilePreview
        name="photo"
        kind="image"
        src="/p"
        downloadHref="/p?download=1"
        alwaysShowDownload
        messages={{ download: 'Télécharger', downloadName: (n) => `Télécharger ${n}` }}
      />,
    );
    expect(container.querySelector('img')).not.toBeNull();
    const link = screen.getByRole('link', { name: 'Télécharger photo' });
    expect(link).toHaveAttribute('href', '/p?download=1');
    expect(link).toHaveTextContent('Télécharger');
  });

  it('renders on the server', () => {
    const html = renderToString(<FilePreview name="a.pdf" src="/a.pdf" />);
    expect(html).toContain('mn-file-preview');
    expect(html).toContain('application/pdf');
  });
});
