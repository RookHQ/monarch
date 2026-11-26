import { afterEach, describe, expect, it, vi } from 'vitest';
import { fireEvent, render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { createRef, useRef, useState } from 'react';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { AiPromptInput } from '../ai-prompt-input/index.js';
import {
  AiAttachmentTray,
  aiAttachmentStatus,
  aiAttachmentTrayFiles,
  type AiAttachmentTrayFile,
} from './index.js';

const FILES: AiAttachmentTrayFile[] = [
  { id: 'a', name: 'report.pdf', size: 2_400_000, type: 'application/pdf' },
  { id: 'b', name: 'chart.png', size: 51_200, type: 'image/png', progress: 0.45 },
  { id: 'c', name: 'notes.txt', size: 900, type: 'text/plain', error: 'File is too large' },
  {
    id: 'd',
    name: 'photo.jpg',
    size: 1000,
    type: 'image/jpeg',
    previewUrl: 'https://example.com/photo.jpg',
  },
];

function Controlled({ initial = FILES }: { initial?: AiAttachmentTrayFile[] }) {
  const [files, setFiles] = useState(initial);
  const input = useRef<HTMLInputElement>(null);
  return (
    <>
      <input ref={input} aria-label="Prompt" />
      <AiAttachmentTray
        files={files}
        fallbackFocusRef={input}
        onRemove={(id) => setFiles((l) => l.filter((f) => f.id !== id))}
      />
    </>
  );
}

afterEach(() => vi.restoreAllMocks());

describe('aiAttachmentStatus / aiAttachmentTrayFiles', () => {
  it('derives the status', () => {
    expect(aiAttachmentStatus({ id: 'x', name: 'x', size: 1, type: '' })).toBe('ready');
    expect(aiAttachmentStatus({ id: 'x', name: 'x', size: 1, type: '', progress: 1 })).toBe(
      'ready',
    );
    expect(aiAttachmentStatus({ id: 'x', name: 'x', size: 1, type: '', progress: 0 })).toBe(
      'uploading',
    );
    expect(
      aiAttachmentStatus({ id: 'x', name: 'x', size: 1, type: '', progress: 0.5, error: true }),
    ).toBe('error');
  });

  it('maps prompt attachments', () => {
    const file = new File(['abc'], 'a.txt', { type: 'text/plain' });
    expect(aiAttachmentTrayFiles([{ id: '1', file }])).toEqual([
      { id: '1', name: 'a.txt', size: 3, type: 'text/plain', file },
    ]);
  });
});

describe('AiAttachmentTray', () => {
  it('lists files with size, progress, errors and safe thumbnails', async () => {
    const ref = createRef<HTMLDivElement>();
    const onRetry = vi.fn();
    render(
      <AiAttachmentTray
        ref={ref}
        data-testid="root"
        className="extra"
        files={[
          ...FILES,
          {
            id: 'e',
            name: 'evil.png',
            size: 1,
            type: 'image/png',
            previewUrl: 'javascript:alert(1)',
          },
        ]}
        onRemove={() => {}}
        onRetry={onRetry}
      />,
    );
    const root = screen.getByTestId('root');
    expect(root).toHaveClass('mn-ai-attachment-tray', 'extra');
    expect(ref.current).toBe(root);
    const items = within(screen.getByRole('list', { name: 'Attachments' })).getAllByRole(
      'listitem',
    );
    expect(items).toHaveLength(5);
    expect(items[0]).toHaveAttribute('data-status', 'ready');
    expect(items[0]).toHaveTextContent('PDF');
    expect(items[0]).toHaveTextContent('2.3 MB');
    expect(items[1]).toHaveAttribute('data-status', 'uploading');
    expect(items[1]).toHaveAttribute('aria-busy', 'true');
    expect(items[1]).toHaveTextContent('Uploading 45%');
    expect(screen.getByRole('progressbar', { name: 'Uploading chart.png' })).toHaveAttribute(
      'aria-valuenow',
      '45',
    );
    expect(items[2]).toHaveAttribute('data-status', 'error');
    expect(items[2]).toHaveTextContent('File is too large');
    const retry = screen.getByRole('button', { name: 'Retry uploading notes.txt' });
    expect(retry).toHaveAccessibleDescription('File is too large');
    await userEvent.click(retry);
    expect(onRetry).toHaveBeenCalledWith('c');
    expect(items[3]?.querySelector('img')).toHaveAttribute('src', 'https://example.com/photo.jpg');
    expect(items[4]?.querySelector('img')).toBeNull();
    expect(screen.getAllByRole('button', { name: /^Remove / })).toHaveLength(5);
    await expectNoA11yViolations();
  });

  it('hides Retry without onRetry and uses the default error text', () => {
    render(
      <AiAttachmentTray
        files={[{ id: 'x', name: 'a.csv', size: 10, type: 'text/csv', error: true }]}
        onRemove={() => {}}
      />,
    );
    expect(screen.queryByRole('button', { name: /Retry/ })).toBeNull();
    expect(screen.getByRole('listitem')).toHaveTextContent('Upload failed');
  });

  it('removes from the keyboard and keeps focus in the tray, then falls back', async () => {
    const user = userEvent.setup();
    render(<Controlled initial={FILES.slice(0, 2)} />);
    screen.getByRole('button', { name: 'Remove report.pdf' }).focus();
    await user.keyboard('{Enter}');
    expect(screen.getByRole('button', { name: 'Remove chart.png' })).toHaveFocus();
    expect(screen.getByRole('status')).toHaveTextContent('report.pdf removed');
    await user.keyboard(' ');
    expect(screen.getByRole('textbox', { name: 'Prompt' })).toHaveFocus();
  });

  it('announces finished uploads politely and failures assertively', () => {
    const file: AiAttachmentTrayFile = { id: 'u', name: 'data.csv', size: 10, type: 'text/csv' };
    const { rerender } = render(
      <AiAttachmentTray files={[{ ...file, progress: 0.2 }]} onRemove={() => {}} />,
    );
    rerender(<AiAttachmentTray files={[{ ...file, progress: 1 }]} onRemove={() => {}} />);
    expect(screen.getByRole('status')).toHaveTextContent('data.csv uploaded');
    rerender(
      <AiAttachmentTray files={[{ ...file, error: 'Network error' }]} onRemove={() => {}} />,
    );
    expect(screen.getByRole('alert')).toHaveTextContent('data.csv: Network error');
  });

  it('makes and revokes object URLs for image files', () => {
    const create = vi.fn(() => 'blob:preview-1');
    const revoke = vi.fn();
    Object.assign(URL, { createObjectURL: create, revokeObjectURL: revoke });
    const file = new File(['x'], 'pic.png', { type: 'image/png' });
    const { unmount } = render(
      <AiAttachmentTray files={aiAttachmentTrayFiles([{ id: 'p', file }])} onRemove={() => {}} />,
    );
    expect(create).toHaveBeenCalledWith(file);
    expect(screen.getByRole('listitem').querySelector('img')).toHaveAttribute(
      'src',
      'blob:preview-1',
    );
    unmount();
    expect(revoke).toHaveBeenCalledWith('blob:preview-1');
  });

  it('works as AiPromptInput renderAttachments', async () => {
    const user = userEvent.setup();
    Object.assign(URL, { createObjectURL: () => 'blob:x', revokeObjectURL: () => {} });
    render(
      <AiPromptInput
        label="Message"
        onSubmit={() => {}}
        onAttach={() => {}}
        renderAttachments={(attachments, remove) => (
          <AiAttachmentTray files={aiAttachmentTrayFiles(attachments)} onRemove={remove} />
        )}
      />,
    );
    const box = screen.getByRole('textbox', { name: 'Message' });
    const file = new File(['hello'], 'hello.txt', { type: 'text/plain' });
    fireEvent.paste(box, { clipboardData: { files: [file], getData: () => '' } });
    expect(await screen.findByRole('list', { name: 'Attachments' })).toHaveTextContent('hello.txt');
    await user.click(screen.getByRole('button', { name: 'Remove hello.txt' }));
    expect(screen.queryByRole('list', { name: 'Attachments' })).toBeNull();
    expect(box).toHaveFocus();
  });

  it('renders on the server', () => {
    const html = renderToString(<AiAttachmentTray files={FILES} onRemove={() => {}} />);
    expect(html).toContain('mn-ai-attachment-tray');
    expect(html).toContain('report.pdf');
  });
});
