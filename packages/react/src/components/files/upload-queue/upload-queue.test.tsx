import { describe, expect, it, vi } from 'vitest';
import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { useState } from 'react';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { UploadQueue, countUploads, totalUploadProgress, type UploadQueueItem } from './index.js';

const ITEMS: UploadQueueItem[] = [
  { id: 'a', name: 'contract.pdf', size: 1024 * 1024, status: 'done' },
  { id: 'b', name: 'photo.jpg', size: 3 * 1024 * 1024, status: 'uploading', progress: 40 },
  { id: 'c', name: 'notes.txt', size: 2048, status: 'queued' },
  {
    id: 'd',
    name: 'video.mov',
    size: 900,
    status: 'failed',
    error: 'File is too large for this workspace.',
  },
  { id: 'e', name: 'old.zip', status: 'cancelled' },
];
const live = () => document.querySelector('[aria-live="polite"]')?.textContent ?? '';

describe('upload helpers', () => {
  it('counts by status and totals progress weighted by size', () => {
    expect(countUploads(ITEMS)).toEqual({
      total: 5,
      queued: 1,
      uploading: 1,
      done: 1,
      failed: 1,
      cancelled: 1,
    });
    const live = ITEMS.slice(0, 3);
    expect(totalUploadProgress(live)).toBe(
      Math.round((1024 * 1024 * 100 + 3 * 1024 * 1024 * 40) / (4 * 1024 * 1024 + 2048)),
    );
    expect(
      totalUploadProgress([
        { id: 'x', name: 'x', status: 'uploading', progress: 50 },
        { id: 'y', name: 'y', status: 'done' },
      ]),
    ).toBe(75);
    expect(totalUploadProgress([])).toBe(0);
  });
});

describe('UploadQueue', () => {
  it('lists uploads with status, progress, sizes and a summary, without axe violations', async () => {
    render(
      <UploadQueue items={ITEMS} onCancel={() => {}} onRetry={() => {}} onRemove={() => {}} />,
    );
    const region = screen.getByRole('region', { name: 'Uploads' });
    expect(region).toHaveClass('mn-upload-queue');
    expect(
      within(region).getByText('1 of 4 uploaded · 1 failed · 1 cancelled'),
    ).toBeInTheDocument();
    expect(screen.getByRole('progressbar', { name: 'Total upload progress' })).toBeInTheDocument();
    const photo = screen.getByRole('progressbar', { name: 'Upload progress of photo.jpg' });
    expect(photo).toHaveAttribute('aria-valuenow', '40');
    expect(screen.getByText('Uploading… 40%')).toBeInTheDocument();
    expect(screen.getByText('Waiting')).toBeInTheDocument();
    expect(screen.getByText('Uploaded')).toBeInTheDocument();
    expect(screen.getByText('File is too large for this workspace.')).toBeInTheDocument();
    expect(screen.getByText('Cancelled')).toBeInTheDocument();
    expect(screen.getByText('3 MB')).toBeInTheDocument();
    await expectNoA11yViolations();
  });

  it('offers cancel, retry and remove by status, with named buttons', async () => {
    const user = userEvent.setup();
    const onCancel = vi.fn();
    const onRetry = vi.fn();
    const onRemove = vi.fn();
    render(<UploadQueue items={ITEMS} onCancel={onCancel} onRetry={onRetry} onRemove={onRemove} />);
    await user.click(screen.getByRole('button', { name: 'Cancel upload of photo.jpg' }));
    expect(onCancel).toHaveBeenCalledWith('b');
    expect(screen.getByRole('button', { name: 'Cancel upload of notes.txt' })).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Retry upload of video.mov' }));
    expect(onRetry).toHaveBeenCalledWith('d');
    expect(screen.getByRole('button', { name: 'Retry upload of old.zip' })).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Retry upload of contract.pdf' })).toBeNull();
    await user.click(screen.getByRole('button', { name: 'Remove contract.pdf from the list' }));
    expect(onRemove).toHaveBeenCalledWith('a');
    expect(screen.queryByRole('button', { name: 'Remove photo.jpg from the list' })).toBeNull();
  });

  it('hides actions without handlers and retry for non-retryable failures', () => {
    render(<UploadQueue items={[{ ...ITEMS[3]!, retryable: false }]} onRetry={() => {}} />);
    expect(screen.queryAllByRole('button')).toHaveLength(0);
  });

  it('announces completion, failures and the end of the batch', () => {
    const { rerender } = render(<UploadQueue items={ITEMS.slice(1, 3)} />);
    expect(live()).toBe('');
    rerender(
      <UploadQueue
        items={[
          { ...ITEMS[1]!, status: 'done' },
          { ...ITEMS[2]!, status: 'uploading', progress: 10 },
        ]}
      />,
    );
    expect(live()).toBe('photo.jpg uploaded.');
    rerender(
      <UploadQueue
        items={[
          { ...ITEMS[1]!, status: 'done' },
          { ...ITEMS[2]!, status: 'failed', error: 'Network error' },
        ]}
      />,
    );
    expect(live()).toBe('notes.txt failed: Network error. Uploads finished: 1 uploaded, 1 failed.');
    expect(screen.queryByRole('progressbar', { name: 'Total upload progress' })).toBeNull();
  });

  it('moves focus to the next row when a row is removed', async () => {
    const user = userEvent.setup();
    function Queue() {
      const [items, setItems] = useState<UploadQueueItem[]>([
        { id: 'a', name: 'a.pdf', status: 'done' },
        { id: 'b', name: 'b.pdf', status: 'done' },
      ]);
      return (
        <UploadQueue
          items={items}
          onRemove={(id) => setItems((l) => l.filter((i) => i.id !== id))}
        />
      );
    }
    render(<Queue />);
    await user.click(screen.getByRole('button', { name: 'Remove a.pdf from the list' }));
    expect(screen.getByRole('button', { name: 'Remove b.pdf from the list' })).toHaveFocus();
    await user.click(screen.getByRole('button', { name: 'Remove b.pdf from the list' }));
    expect(screen.getByRole('heading', { name: 'Uploads' })).toHaveFocus();
  });

  it('clears finished uploads, shows indeterminate progress, thumbnails and custom detail', async () => {
    const user = userEvent.setup();
    const onClear = vi.fn();
    render(
      <UploadQueue
        items={[
          { id: 'a', name: 'a.png', status: 'uploading', thumbnail: 'data:image/png;base64,AAAA' },
          { id: 'b', name: 'b.png', status: 'done', detail: 'Checking for viruses' },
        ]}
        onClearFinished={onClear}
      />,
    );
    expect(
      screen.getByRole('progressbar', { name: 'Upload progress of a.png' }),
    ).not.toHaveAttribute('aria-valuenow');
    expect(document.querySelector('img.mn-upload-queue__thumb')).toHaveAttribute('alt', '');
    expect(screen.getByText('Checking for viruses')).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Clear finished' }));
    expect(onClear).toHaveBeenCalled();
  });

  it('can drop the header and translate messages', () => {
    render(
      <UploadQueue
        items={[{ id: 'a', name: 'a.pdf', status: 'queued' }]}
        showSummary={false}
        messages={{ title: 'Téléversements', queued: 'En attente' }}
      />,
    );
    expect(screen.getByRole('region', { name: 'Téléversements' })).toBeInTheDocument();
    expect(screen.queryByRole('heading')).toBeNull();
    expect(screen.getByText('En attente')).toBeInTheDocument();
  });

  it('renders on the server', () => {
    expect(renderToString(<UploadQueue items={ITEMS} />)).toContain('mn-upload-queue');
  });
});
