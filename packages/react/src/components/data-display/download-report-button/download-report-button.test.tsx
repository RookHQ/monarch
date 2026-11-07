import { describe, expect, it, vi } from 'vitest';
import { act, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { createRef } from 'react';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { DownloadReportButton, type DownloadResult } from './index.js';

function deferred() {
  let resolve!: (v: DownloadResult | undefined) => void;
  let reject!: (e: unknown) => void;
  const promise = new Promise<DownloadResult | undefined>((res, rej) => {
    resolve = res;
    reject = rej;
  });
  return { promise, resolve, reject };
}

describe('DownloadReportButton', () => {
  it('downloads the single format, stays busy until done and announces it; forwards props and ref', async () => {
    const ref = createRef<HTMLDivElement>();
    const d = deferred();
    const onDownload = vi.fn(() => d.promise);
    const user = userEvent.setup();
    render(
      <DownloadReportButton
        ref={ref}
        data-testid="root"
        className="extra"
        onDownload={onDownload}
      />,
    );
    const root = screen.getByTestId('root');
    expect(root).toHaveClass('mn-download-report-button', 'extra');
    expect(ref.current).toBe(root);
    const button = screen.getByRole('button', { name: 'Download CSV' });
    await user.click(button);
    expect(onDownload).toHaveBeenCalledWith('csv');
    expect(button).toHaveAttribute('aria-busy', 'true');
    expect(screen.getByRole('status')).toHaveTextContent('Preparing CSV…');
    await user.click(button);
    expect(onDownload).toHaveBeenCalledTimes(1);
    await act(async () => d.resolve(undefined));
    expect(screen.getByRole('status')).toHaveTextContent('CSV downloaded');
    expect(button).not.toHaveAttribute('aria-busy');
    expect(root).toHaveAttribute('data-status', 'done');
    await expectNoA11yViolations();
  });

  it('offers several formats in a menu', async () => {
    const onDownload = vi.fn();
    const user = userEvent.setup();
    render(
      <DownloadReportButton
        onDownload={onDownload}
        formats={[
          { id: 'csv', label: 'CSV', description: 'Opens in any spreadsheet' },
          { id: 'xlsx', label: 'Excel' },
          { id: 'pdf', label: 'PDF' },
        ]}
      />,
    );
    const trigger = screen.getByRole('button', { name: 'Download' });
    trigger.focus();
    await user.keyboard('{Enter}');
    expect(await screen.findByRole('menuitem', { name: /CSV/ })).toHaveTextContent(
      'Opens in any spreadsheet',
    );
    await user.click(screen.getByRole('menuitem', { name: 'Excel' }));
    expect(onDownload).toHaveBeenCalledWith('xlsx');
    await waitFor(() => expect(screen.getByRole('status')).toHaveTextContent('Excel downloaded'));
  });

  it('shows progress while it runs', async () => {
    const d = deferred();
    const user = userEvent.setup();
    const { rerender } = render(
      <DownloadReportButton onDownload={() => d.promise} progress={0.4} />,
    );
    await user.click(screen.getByRole('button', { name: 'Download CSV' }));
    expect(screen.getByRole('progressbar', { name: 'Download progress' })).toHaveAttribute(
      'aria-valuenow',
      '40',
    );
    rerender(<DownloadReportButton onDownload={() => d.promise} progress={0.9} />);
    expect(screen.getByRole('progressbar')).toHaveAttribute('aria-valuenow', '90');
    await act(async () => d.resolve(undefined));
    expect(screen.queryByRole('progressbar')).not.toBeInTheDocument();
  });

  it('says when a large file is queued', async () => {
    const user = userEvent.setup();
    render(<DownloadReportButton onDownload={async () => ({ status: 'queued' as const })} />);
    await user.click(screen.getByRole('button', { name: 'Download CSV' }));
    await waitFor(() =>
      expect(screen.getByRole('status')).toHaveTextContent(/The file is being prepared/),
    );
  });

  it('shows a failure with Try again that retries the same format and returns focus', async () => {
    const onDownload = vi
      .fn<(format: string) => Promise<DownloadResult | undefined>>()
      .mockRejectedValueOnce(new Error('503'))
      .mockResolvedValueOnce(undefined);
    const user = userEvent.setup();
    render(
      <DownloadReportButton
        formats={[{ id: 'pdf', label: 'PDF' }]}
        onDownload={onDownload}
        errorMessage={(error, format) => `${format.label} failed: ${(error as Error).message}`}
      />,
    );
    await user.click(screen.getByRole('button', { name: 'Download PDF' }));
    expect(await screen.findByText('PDF failed: 503')).toBeInTheDocument();
    await expectNoA11yViolations();
    await user.click(screen.getByRole('button', { name: 'Try again' }));
    expect(onDownload).toHaveBeenLastCalledWith('pdf');
    await waitFor(() => expect(screen.getByRole('status')).toHaveTextContent('PDF downloaded'));
    expect(screen.getByRole('button', { name: 'Download PDF' })).toHaveFocus();
  });

  it('is disabled when asked', () => {
    render(<DownloadReportButton onDownload={() => {}} disabled />);
    expect(screen.getByRole('button', { name: 'Download CSV' })).toBeDisabled();
  });

  it('translates through messages', () => {
    render(
      <DownloadReportButton onDownload={() => {}} messages={{ single: (f) => `Descargar ${f}` }} />,
    );
    expect(screen.getByRole('button', { name: 'Descargar CSV' })).toBeInTheDocument();
  });

  it('renders on the server', () => {
    const html = renderToString(<DownloadReportButton onDownload={() => {}} />);
    expect(html).toContain('mn-download-report-button');
    expect(html).toContain('Download CSV');
  });
});
