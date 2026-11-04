import { describe, expect, it, vi } from 'vitest';
import { createEvent, fireEvent, render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { Field, FieldDescription, FieldLabel } from '../../forms/form-field/index.js';
import { FileUpload, fileMatchesAccept, formatBytes, validateFiles } from './index.js';

const file = (name: string, size: number, type: string) => {
  const f = new File(['x'], name, { type });
  Object.defineProperty(f, 'size', { value: size });
  return f;
};
const PNG = file('photo.png', 2_000, 'image/png');
const BIG = file('scan.png', 20 * 1024 * 1024, 'image/png');
const PDF = file('notes.pdf', 3_000, 'application/pdf');
const input = () => document.querySelector<HTMLInputElement>('input[type="file"]')!;

/** A drag event carrying files (jsdom has no DataTransfer). */
const fileDrag = (
  type: 'dragEnter' | 'dragOver' | 'dragLeave' | 'drop',
  el: Element,
  files: File[],
) => {
  const ev = createEvent[type](el);
  Object.defineProperty(ev, 'dataTransfer', {
    value: {
      types: ['Files'],
      files,
      items: files.map((f) => ({ kind: 'file', type: f.type })),
      dropEffect: 'none',
    },
  });
  fireEvent(el, ev);
};

describe('validateFiles and helpers', () => {
  it('matches accept rules by MIME type, wildcard and extension', () => {
    expect(fileMatchesAccept(PNG, 'image/*')).toBe(true);
    expect(fileMatchesAccept(PNG, '.PNG')).toBe(true);
    expect(fileMatchesAccept(PDF, 'image/png, image/jpeg')).toBe(false);
    expect(fileMatchesAccept(PDF, 'application/pdf')).toBe(true);
    expect(fileMatchesAccept(PDF, '')).toBe(true);
  });

  it('refuses by type, size, custom rule and count, keeping the first valid files', () => {
    const result = validateFiles(
      [PDF, BIG, PNG, file('b.png', 10, 'image/png'), file('c.png', 10, 'image/png')],
      {
        accept: 'image/*',
        maxSize: 10 * 1024 * 1024,
        maxFiles: 3,
        currentCount: 1,
        validate: (f) => (f.name === 'b.png' ? 'b.png is blurry.' : null),
      },
    );
    expect(result.accepted.map((f) => f.name)).toEqual(['photo.png', 'c.png']);
    expect(result.rejected.map((r) => [r.file.name, r.code, r.message])).toEqual([
      ['notes.pdf', 'file-invalid-type', 'notes.pdf is not an allowed file type.'],
      ['scan.png', 'file-too-large', 'scan.png is larger than 10 MB.'],
      ['b.png', 'custom', 'b.png is blurry.'],
    ]);
    const over = validateFiles([PNG, PNG], { maxFiles: 1 });
    expect(over.rejected[0]).toMatchObject({
      code: 'too-many-files',
      message: 'photo.png was not added: up to 1 file.',
    });
  });

  it('formats bytes', () => {
    expect(formatBytes(512)).toBe('512 B');
    expect(formatBytes(1536)).toBe('1.5 KB');
    expect(formatBytes(10 * 1024 * 1024)).toBe('10 MB');
    expect(formatBytes(-1)).toBe('0 B');
  });
});

describe('FileUpload', () => {
  it('renders a drop zone with a browse button and a generated hint, without axe violations', async () => {
    render(
      <FileUpload data-testid="root" maxSize={5 * 1024 * 1024} maxFiles={3} accept="image/*" />,
    );
    expect(screen.getByTestId('root')).toHaveClass('mn-file-upload');
    const browse = screen.getByRole('button', { name: 'Browse files' });
    expect(browse).toHaveAccessibleDescription('Up to 5 MB each · 3 files max');
    expect(input()).toHaveAttribute('accept', 'image/*');
    expect(input()).toHaveAttribute('multiple');
    expect(input()).toHaveAttribute('tabindex', '-1');
    await expectNoA11yViolations();
  });

  it('opens the picker from the button and the keyboard, and emits checked files', async () => {
    const user = userEvent.setup({ applyAccept: false });
    const onFiles = vi.fn();
    const click = vi.spyOn(HTMLInputElement.prototype, 'click');
    render(<FileUpload onFiles={onFiles} accept="image/*" maxSize={10 * 1024 * 1024} />);
    await user.tab();
    expect(screen.getByRole('button', { name: 'Browse files' })).toHaveFocus();
    await user.keyboard('{Enter}');
    expect(click).toHaveBeenCalled();
    click.mockRestore();
    await user.upload(input(), [PNG, PDF, BIG]);
    expect(onFiles).toHaveBeenCalledTimes(1);
    const [accepted, rejected] = onFiles.mock.calls[0]!;
    expect(accepted).toEqual([PNG]);
    expect(rejected).toHaveLength(2);
    expect(screen.getByText('notes.pdf is not an allowed file type.')).toBeInTheDocument();
    expect(screen.getByText('scan.png is larger than 10 MB.')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Browse files' })).toHaveAccessibleDescription(
      'Up to 10 MB each notes.pdf is not an allowed file type. scan.png is larger than 10 MB.',
    );
    expect(screen.getByText('1 file added.')).toBeInTheDocument();
    // The input is cleared so the same file can be chosen again.
    expect(input().value).toBe('');
    await user.upload(input(), [PNG]);
    expect(onFiles).toHaveBeenCalledTimes(2);
    expect(screen.queryByText('notes.pdf is not an allowed file type.')).toBeNull();
  });

  it('takes one file when maxFiles is 1 and refuses the rest', async () => {
    const onFiles = vi.fn();
    render(<FileUpload onFiles={onFiles} maxFiles={1} />);
    expect(input()).not.toHaveAttribute('multiple');
    fileDrag('drop', screen.getByRole('button').closest('.mn-file-upload')!, [PNG, PDF]);
    expect(onFiles.mock.calls[0]![0]).toEqual([PNG]);
    expect(onFiles.mock.calls[0]![1][0]).toMatchObject({ code: 'too-many-files' });
  });

  it('shows drag-over state and accepts dropped files', () => {
    const onFiles = vi.fn();
    render(<FileUpload data-testid="root" onFiles={onFiles} accept="image/png" />);
    const root = screen.getByTestId('root');
    fileDrag('dragEnter', root, [PNG]);
    expect(root).toHaveAttribute('data-drag', 'accept');
    expect(screen.getByText('Drop to upload')).toBeInTheDocument();
    fileDrag('dragOver', root, [PNG]);
    fileDrag('dragLeave', root, [PNG]);
    expect(root).not.toHaveAttribute('data-drag');
    fileDrag('dragEnter', root, [PDF]);
    expect(root).toHaveAttribute('data-drag', 'reject');
    expect(screen.getByText('This file type is not allowed')).toBeInTheDocument();
    fileDrag('drop', root, [PNG]);
    expect(root).not.toHaveAttribute('data-drag');
    expect(onFiles).toHaveBeenCalledWith([PNG], []);
  });

  it('accepts pasted files only when paste is on', () => {
    const onFiles = vi.fn();
    const { rerender } = render(<FileUpload data-testid="root" onFiles={onFiles} />);
    const paste = () => {
      const ev = createEvent.paste(screen.getByRole('button'));
      Object.defineProperty(ev, 'clipboardData', { value: { files: [PNG] } });
      fireEvent(screen.getByRole('button'), ev);
    };
    paste();
    expect(onFiles).not.toHaveBeenCalled();
    rerender(<FileUpload data-testid="root" onFiles={onFiles} paste />);
    paste();
    expect(onFiles).toHaveBeenCalledWith([PNG], []);
  });

  it('is disabled when asked and when the file limit is reached', () => {
    const onFiles = vi.fn();
    const { rerender } = render(<FileUpload data-testid="root" disabled onFiles={onFiles} />);
    expect(screen.getByRole('button', { name: 'Browse files' })).toBeDisabled();
    fileDrag('drop', screen.getByTestId('root'), [PNG]);
    expect(onFiles).not.toHaveBeenCalled();
    rerender(<FileUpload data-testid="root" maxFiles={2} currentCount={2} onFiles={onFiles} />);
    expect(screen.getByRole('button', { name: 'Browse files' })).toBeDisabled();
    expect(screen.getByText('Limit reached: 2 files attached.')).toBeInTheDocument();
    expect(screen.getByTestId('root')).toHaveAttribute('data-disabled');
  });

  it('can leave errors to the product', async () => {
    const user = userEvent.setup({ applyAccept: false });
    render(<FileUpload accept="image/*" showErrors={false} />);
    await user.upload(input(), [PDF]);
    expect(screen.queryByText('notes.pdf is not an allowed file type.')).toBeNull();
  });

  it('takes its name and description from Field', async () => {
    render(
      <Field invalid required>
        <FieldLabel>Resume</FieldLabel>
        <FileUpload hint="PDF up to 5 MB" />
        <FieldDescription>Shared with the hiring team.</FieldDescription>
      </Field>,
    );
    const browse = screen.getByRole('button', { name: 'Resume Browse files' });
    expect(browse).toHaveAccessibleDescription('PDF up to 5 MB Shared with the hiring team.');
    expect(browse).toHaveAttribute('aria-invalid', 'true');
    await expectNoA11yViolations();
  });

  it('supports the compact variant, a custom icon and translated messages', () => {
    render(
      <FileUpload
        data-testid="root"
        variant="compact"
        icon={<svg data-testid="icon" />}
        messages={{ browse: 'Parcourir', label: 'Déposez vos fichiers' }}
      />,
    );
    expect(screen.getByTestId('root')).toHaveAttribute('data-variant', 'compact');
    expect(screen.getByTestId('icon')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Parcourir' })).toBeInTheDocument();
    expect(screen.getByText('Déposez vos fichiers')).toBeInTheDocument();
  });

  it('renders on the server', () => {
    expect(renderToString(<FileUpload />)).toContain('mn-file-upload');
  });
});
