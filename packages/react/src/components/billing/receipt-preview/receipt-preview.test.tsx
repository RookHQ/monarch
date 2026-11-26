import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { ReceiptPreview } from './index.js';

const createObjectURL = vi.fn(() => 'blob:receipt-1');
const revokeObjectURL = vi.fn();

beforeEach(() => {
  Object.assign(URL, { createObjectURL, revokeObjectURL });
});
afterEach(() => {
  vi.restoreAllMocks();
  createObjectURL.mockClear();
  revokeObjectURL.mockClear();
});

describe('ReceiptPreview', () => {
  it('opens an image from src in a dialog with Download and returns focus on close', async () => {
    const user = userEvent.setup();
    render(<ReceiptPreview mimeType="image/png" src="/r/receipt.png" fileName="taxi.png" />);
    const trigger = screen.getByRole('button', { name: 'View receipt' });
    expect(trigger).toHaveClass('mn-receipt-preview');
    expect(trigger).toHaveAttribute('aria-haspopup', 'dialog');
    await user.click(trigger);
    const dialog = screen.getByRole('dialog', { name: 'Receipt' });
    expect(screen.getByRole('img', { name: 'Receipt' })).toHaveAttribute('src', '/r/receipt.png');
    expect(screen.getByRole('link', { name: 'Download' })).toHaveAttribute('download', 'taxi.png');
    await expectNoA11yViolations(dialog);
    await user.keyboard('{Escape}');
    expect(screen.queryByRole('dialog')).toBeNull();
    expect(trigger).toHaveFocus();
  });

  it('loads a Blob lazily, shows a spinner and revokes the object URL on close', async () => {
    const user = userEvent.setup();
    let finish!: (b: Blob) => void;
    const load = vi.fn(() => new Promise<Blob>((r) => (finish = r)));
    render(<ReceiptPreview mimeType="image/jpeg" load={load} />);
    expect(load).not.toHaveBeenCalled();
    await user.click(screen.getByRole('button', { name: 'View receipt' }));
    expect(screen.getByRole('status', { name: 'Loading receipt' })).toBeInTheDocument();
    finish(new Blob(['x'], { type: 'image/jpeg' }));
    expect(await screen.findByRole('img', { name: 'Receipt' })).toHaveAttribute(
      'src',
      'blob:receipt-1',
    );
    expect(screen.getByRole('link', { name: 'Download' })).toHaveAttribute(
      'download',
      'receipt.jpg',
    );
    await user.click(screen.getByRole('button', { name: 'Close' }));
    expect(revokeObjectURL).toHaveBeenCalledWith('blob:receipt-1');
  });

  it('shows a retryable error when loading fails', async () => {
    const user = userEvent.setup();
    const onLoadError = vi.fn();
    const load = vi
      .fn<() => Promise<string>>()
      .mockRejectedValueOnce(new Error('403'))
      .mockResolvedValueOnce('/signed/receipt.png');
    render(<ReceiptPreview mimeType="image/png" load={load} onLoadError={onLoadError} />);
    await user.click(screen.getByRole('button', { name: 'View receipt' }));
    expect(await screen.findByText('The receipt could not be loaded.')).toBeInTheDocument();
    expect(onLoadError).toHaveBeenCalledTimes(1);
    await user.click(screen.getByRole('button', { name: 'Try again' }));
    expect(await screen.findByRole('img', { name: 'Receipt' })).toHaveAttribute(
      'src',
      '/signed/receipt.png',
    );
  });

  it('explains HEIC photos and other types the browser cannot show', async () => {
    const user = userEvent.setup();
    const { unmount } = render(<ReceiptPreview mimeType="image/heic" src="/r/photo.heic" />);
    await user.click(screen.getByRole('button', { name: 'View receipt' }));
    fireEvent.error(screen.getByRole('img'));
    expect(screen.getByText(/HEIC photo/)).toBeInTheDocument();
    unmount();
    render(<ReceiptPreview mimeType="text/csv" src="/r/data.csv" />);
    await user.click(screen.getByRole('button', { name: 'View receipt' }));
    expect(screen.getByText(/no preview for this file type/)).toBeInTheDocument();
  });

  it('opens a PDF in a new tab by default', async () => {
    const user = userEvent.setup();
    const open = vi.spyOn(window, 'open').mockReturnValue(null);
    render(<ReceiptPreview mimeType="application/pdf" src="/r/bill.pdf" />);
    await user.click(screen.getByRole('button', { name: 'View receipt (opens in a new tab)' }));
    expect(open).toHaveBeenCalledWith('/r/bill.pdf', '_blank', 'noopener');
    expect(screen.queryByRole('dialog')).toBeNull();
  });

  it('points a pre-opened tab at a lazily loaded PDF', async () => {
    const user = userEvent.setup();
    const tab = { opener: {}, location: { href: '' }, close: vi.fn() };
    vi.spyOn(window, 'open').mockReturnValue(tab as unknown as Window);
    const load = vi.fn(async () => new Blob(['%PDF'], { type: 'application/pdf' }));
    render(<ReceiptPreview mimeType="application/pdf" load={load} />);
    await user.click(screen.getByRole('button', { name: /View receipt/ }));
    await waitFor(() => expect(tab.location.href).toBe('blob:receipt-1'));
    expect(tab.opener).toBeNull();
  });

  it('embeds a PDF in the dialog when asked', async () => {
    const user = userEvent.setup();
    render(<ReceiptPreview mimeType="application/pdf" src="/r/bill.pdf" pdf="dialog" />);
    await user.click(screen.getByRole('button', { name: 'View receipt' }));
    expect(screen.getByRole('link', { name: 'Open in a new tab' })).toHaveAttribute(
      'href',
      '/r/bill.pdf',
    );
  });

  it('says when there is no receipt, and uses custom messages', () => {
    const { rerender } = render(<ReceiptPreview mimeType="image/png" src={null} />);
    expect(screen.getByText('No receipt')).toBeInTheDocument();
    expect(screen.queryByRole('button')).toBeNull();
    rerender(<ReceiptPreview mimeType="image/png" src="/a.png" messages={{ view: 'Open bill' }} />);
    expect(screen.getByRole('button', { name: 'Open bill' })).toBeInTheDocument();
  });

  it('supports a controlled open state', async () => {
    const onOpenChange = vi.fn();
    render(<ReceiptPreview mimeType="image/png" src="/a.png" open onOpenChange={onOpenChange} />);
    expect(screen.getByRole('dialog', { name: 'Receipt' })).toBeInTheDocument();
    await userEvent.setup().keyboard('{Escape}');
    expect(onOpenChange).toHaveBeenCalledWith(false);
  });

  it('renders on the server', () => {
    const html = renderToString(<ReceiptPreview mimeType="image/png" src="/a.png" />);
    expect(html).toContain('mn-receipt-preview');
    expect(html).toContain('View receipt');
  });
});
