import { describe, expect, it, vi } from 'vitest';
import { createEvent, fireEvent, render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { createRef } from 'react';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { DropZone } from './index.js';

const file = (name: string, size: number, type: string) => {
  const f = new File(['x'], name, { type });
  Object.defineProperty(f, 'size', { value: size });
  return f;
};
const PNG = file('photo.png', 2_000, 'image/png');
const PDF = file('notes.pdf', 3_000, 'application/pdf');
const BIG = file('huge.png', 50_000_000, 'image/png');

/** A drag event carrying files (jsdom has no DataTransfer). */
const fileDrag = (
  type: 'dragEnter' | 'dragOver' | 'dragLeave' | 'drop',
  el: Element | Window,
  files: File[],
) => {
  const ev = createEvent[type](el as Element);
  Object.defineProperty(ev, 'dataTransfer', {
    value: {
      types: ['Files'],
      files,
      items: files.map((f) => ({ kind: 'file', type: f.type })),
      dropEffect: 'none',
    },
  });
  fireEvent(el as Element, ev);
  return ev;
};

describe('DropZone', () => {
  it('wraps content with an upload button that opens the picker', async () => {
    const user = userEvent.setup();
    const onFiles = vi.fn();
    const ref = createRef<HTMLDivElement>();
    render(
      <DropZone ref={ref} onFiles={onFiles} toolbar={<span>PNG or PDF</span>}>
        <p>Content</p>
      </DropZone>,
    );
    expect(ref.current).toHaveClass('mn-drop-zone');
    expect(ref.current).toHaveAttribute('data-scope', 'region');
    expect(screen.getByText('Content')).toBeInTheDocument();
    const input = ref.current!.querySelector<HTMLInputElement>('input[type="file"]')!;
    const click = vi.spyOn(input, 'click');
    await user.tab();
    expect(screen.getByRole('button', { name: 'Upload files' })).toHaveFocus();
    await user.keyboard('{Enter}');
    expect(click).toHaveBeenCalled();
    await user.upload(input, [PNG]);
    expect(onFiles).toHaveBeenCalledWith([PNG], []);
    expect(screen.getByRole('status')).toHaveTextContent('1 file added.');
    expect(input.value).toBe('');
    await expectNoA11yViolations();
  });

  it('shows the overlay while files are dragged over and takes dropped files', () => {
    const onFiles = vi.fn();
    const { container } = render(
      <DropZone onFiles={onFiles}>
        <p>List</p>
      </DropZone>,
    );
    const root = container.firstElementChild!;
    const enter = fileDrag('dragEnter', root, [PNG]);
    expect(enter.defaultPrevented).toBe(true);
    expect(root).toHaveAttribute('data-drag', 'accept');
    expect(screen.getByText('Drop files to upload').closest('[aria-hidden]')).not.toBeNull();
    expect(fileDrag('dragOver', root, [PNG]).defaultPrevented).toBe(true);
    fileDrag('dragLeave', root, [PNG]);
    expect(root).not.toHaveAttribute('data-drag');
    fileDrag('dragEnter', root, [PNG]);
    fileDrag('drop', root, [PNG, PDF]);
    expect(root).not.toHaveAttribute('data-drag');
    expect(onFiles).toHaveBeenCalledWith([PNG, PDF], []);
  });

  it('flags disallowed types while dragging and lists refused files', async () => {
    const onFiles = vi.fn();
    const { container } = render(
      <DropZone onFiles={onFiles} accept="image/*" maxSize={1_000_000} />,
    );
    const root = container.firstElementChild!;
    fileDrag('dragEnter', root, [PDF]);
    expect(root).toHaveAttribute('data-drag', 'reject');
    expect(screen.getByText('These files are not allowed here')).toBeInTheDocument();
    fileDrag('drop', root, [PDF, BIG, PNG]);
    expect(onFiles).toHaveBeenCalledWith(
      [PNG],
      expect.arrayContaining([expect.objectContaining({ file: PDF, code: 'file-invalid-type' })]),
    );
    expect(screen.getByText('notes.pdf is not an allowed file type.')).toBeInTheDocument();
    expect(screen.getByText(/huge\.png is larger than/)).toBeInTheDocument();
    expect(screen.getByRole('status')).toHaveTextContent('1 file added. 2 files were not added.');
    expect(screen.getByRole('button', { name: 'Upload files' })).toHaveAccessibleDescription(
      /notes\.pdf/,
    );
    await expectNoA11yViolations();
  });

  it('takes one file when not multiple, and ignores drags when disabled or full', () => {
    const onFiles = vi.fn();
    const { container, rerender } = render(<DropZone onFiles={onFiles} multiple={false} />);
    const root = container.firstElementChild!;
    fileDrag('drop', root, [PNG, PDF]);
    expect(onFiles).toHaveBeenLastCalledWith(
      [PNG],
      [expect.objectContaining({ file: PDF, code: 'too-many-files' })],
    );
    rerender(<DropZone onFiles={onFiles} disabled />);
    fileDrag('dragEnter', root, [PNG]);
    expect(root).not.toHaveAttribute('data-drag');
    expect(root).toHaveAttribute('data-disabled');
    expect(screen.getByRole('button')).toBeDisabled();
    rerender(<DropZone onFiles={onFiles} maxFiles={2} currentCount={2} />);
    expect(screen.getByRole('button')).toBeDisabled();
    expect(onFiles).toHaveBeenCalledTimes(1);
  });

  it('cancels the browser default for a file dropped while disabled or full, without taking it', () => {
    const onFiles = vi.fn();
    const { container, rerender } = render(<DropZone onFiles={onFiles} disabled />);
    const root = container.firstElementChild!;
    // Without preventDefault the browser opens the dropped file and leaves the page.
    expect(fileDrag('dragOver', root, [PNG]).defaultPrevented).toBe(true);
    expect(fileDrag('drop', root, [PNG]).defaultPrevented).toBe(true);
    rerender(<DropZone onFiles={onFiles} maxFiles={1} currentCount={1} />);
    expect(fileDrag('dragOver', root, [PNG]).defaultPrevented).toBe(true);
    expect(fileDrag('drop', root, [PNG]).defaultPrevented).toBe(true);
    rerender(<DropZone scope="page" onFiles={onFiles} disabled />);
    expect(fileDrag('dragEnter', window, [PNG]).defaultPrevented).toBe(true);
    expect(root).not.toHaveAttribute('data-drag');
    expect(fileDrag('drop', window, [PNG]).defaultPrevented).toBe(true);
    expect(onFiles).not.toHaveBeenCalled();
  });

  it('listens to the whole window in page scope and cleans up on unmount', () => {
    const onFiles = vi.fn();
    const { container, unmount } = render(
      <DropZone scope="page" onFiles={onFiles} showButton={false} />,
    );
    const root = container.firstElementChild!;
    expect(screen.queryByRole('button')).toBeNull();
    fileDrag('dragEnter', window, [PNG]);
    expect(root).toHaveAttribute('data-drag', 'accept');
    fileDrag('drop', window, [PNG]);
    expect(onFiles).toHaveBeenCalledWith([PNG], []);
    unmount();
    const ev = fileDrag('dragOver', window, [PNG]);
    expect(ev.defaultPrevented).toBe(false);
  });

  it('leaves a drop to the nested zone that handled it, in page scope', () => {
    const onPage = vi.fn();
    const onInner = vi.fn();
    render(
      <DropZone scope="page" onFiles={onPage} showButton={false}>
        <DropZone onFiles={onInner} data-testid="inner" />
      </DropZone>,
    );
    fileDrag('dragEnter', screen.getByTestId('inner'), [PNG]);
    fileDrag('drop', screen.getByTestId('inner'), [PNG]);
    expect(onInner).toHaveBeenCalledTimes(1);
    expect(onPage).not.toHaveBeenCalled();
    // The page overlay still clears.
    expect(document.querySelector('[data-scope="page"]')).not.toHaveAttribute('data-drag');
    // A drop outside the nested zone is still the page's.
    fileDrag('drop', window, [PDF]);
    expect(onPage).toHaveBeenCalledWith([PDF], []);
  });

  it('ignores drags that carry no files and translates', () => {
    const { container } = render(<DropZone messages={{ browse: 'Téléverser' }} />);
    const root = container.firstElementChild!;
    fireEvent.dragEnter(root);
    expect(root).not.toHaveAttribute('data-drag');
    expect(screen.getByRole('button', { name: 'Téléverser' })).toBeInTheDocument();
  });

  it('renders on the server', () => {
    const html = renderToString(
      <DropZone scope="page">
        <p>Files</p>
      </DropZone>,
    );
    expect(html).toContain('mn-drop-zone');
    expect(html).toContain('Upload files');
  });
});
