import { describe, expect, it } from 'vitest';
import { render, screen } from '@testing-library/react';
import { renderToString } from 'react-dom/server';
import { createRef } from 'react';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import {
  FILE_TYPE_FAMILIES,
  FileTypeGlyph,
  FileTypeIcon,
  fileExtension,
  getFileTypeFamily,
} from './index.js';

describe('getFileTypeFamily', () => {
  it('prefers a specific MIME type over the extension', () => {
    expect(getFileTypeFamily({ name: 'scan.bin', type: 'image/png' })).toBe('image');
    expect(getFileTypeFamily({ name: 'x', type: 'application/pdf' })).toBe('pdf');
    expect(
      getFileTypeFamily({
        name: 'a',
        type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
      }),
    ).toBe('spreadsheet');
    expect(getFileTypeFamily({ type: 'text/csv; charset=utf-8' })).toBe('spreadsheet');
    expect(getFileTypeFamily({ type: 'application/zip' })).toBe('archive');
    expect(getFileTypeFamily({ type: 'text/plain' })).toBe('text');
  });

  it('falls back to the extension for generic or missing types', () => {
    expect(getFileTypeFamily({ name: 'Report.DOCX', type: 'application/octet-stream' })).toBe(
      'document',
    );
    expect(getFileTypeFamily('deck.key')).toBe('presentation');
    expect(getFileTypeFamily('clip.mov')).toBe('video');
    expect(getFileTypeFamily('song.flac')).toBe('audio');
    expect(getFileTypeFamily('main.tsx')).toBe('code');
    expect(getFileTypeFamily('notes.md')).toBe('text');
    expect(getFileTypeFamily('backup.tar.gz')).toBe('archive');
    expect(getFileTypeFamily('README')).toBe('other');
    expect(getFileTypeFamily({})).toBe('other');
  });

  it('reads extensions safely', () => {
    expect(fileExtension('a/b/c.PNG')).toBe('png');
    expect(fileExtension('.env')).toBe('');
    expect(fileExtension(undefined)).toBe('');
  });
});

describe('FileTypeIcon', () => {
  it('FileTypeGlyph forwards its ref', () => {
    const ref = createRef<SVGSVGElement>();
    render(<FileTypeGlyph ref={ref} family="pdf" />);
    expect(ref.current?.tagName.toLowerCase()).toBe('svg');
  });

  it('is an image named by its family and forwards ref and props', async () => {
    const ref = createRef<HTMLSpanElement>();
    render(<FileTypeIcon ref={ref} name="q3-summary.pdf" data-testid="icon" />);
    const icon = screen.getByRole('img', { name: 'PDF document' });
    expect(icon).toBe(ref.current);
    expect(icon).toHaveClass('mn-file-type-icon');
    expect(icon).toHaveAttribute('data-family', 'pdf');
    expect(icon).toHaveAttribute('data-size', 'md');
    expect(icon.querySelector('svg')).toHaveAttribute('aria-hidden', 'true');
    await expectNoA11yViolations();
  });

  it('draws every family with a label', async () => {
    render(
      <div>
        {FILE_TYPE_FAMILIES.map((f) => (
          <FileTypeIcon key={f} family={f} size="lg" />
        ))}
      </div>,
    );
    expect(screen.getAllByRole('img')).toHaveLength(FILE_TYPE_FAMILIES.length);
    expect(screen.getByRole('img', { name: 'Spreadsheet' })).toHaveAttribute('data-size', 'lg');
    await expectNoA11yViolations();
  });

  it('takes a custom label, translated messages, or hides itself when decorative', () => {
    const { container, rerender } = render(<FileTypeIcon contentType="video/mp4" label="Clip" />);
    expect(screen.getByRole('img', { name: 'Clip' })).toBeInTheDocument();
    rerender(<FileTypeIcon contentType="video/mp4" messages={{ video: 'Vidéo' }} />);
    expect(screen.getByRole('img', { name: 'Vidéo' })).toBeInTheDocument();
    rerender(<FileTypeIcon contentType="video/mp4" decorative />);
    expect(screen.queryByRole('img')).toBeNull();
    expect(container.firstElementChild).toHaveAttribute('aria-hidden', 'true');
  });

  it('renders on the server', () => {
    const html = renderToString(<FileTypeIcon name="data.csv" />);
    expect(html).toContain('mn-file-type-icon');
    expect(html).toContain('data-family="spreadsheet"');
  });
});
