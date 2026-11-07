import { describe, expect, it, vi } from 'vitest';
import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { DataExportRequest, type DataExportRequestProps } from './index.js';

const scope = [
  { id: 'profile', label: 'Profile', description: 'Name, email and settings.' },
  { id: 'messages', label: 'Messages' },
  { id: 'files', label: 'Files' },
];
const formats = [
  { value: 'json', label: 'JSON' },
  { value: 'csv', label: 'CSV' },
];
const fixed = { locale: 'en-GB', timeZone: 'UTC' } as const;

function Export(props: Partial<DataExportRequestProps>) {
  return (
    <DataExportRequest
      {...fixed}
      scope={scope}
      formats={formats}
      preparationNote="It can take up to two days. We will email you when it is ready."
      onRequest={() => {}}
      {...props}
    />
  );
}

describe('DataExportRequest', () => {
  it('requests an export with the chosen scope and format', async () => {
    const user = userEvent.setup();
    const onRequest = vi.fn();
    render(<Export onRequest={onRequest} />);
    const card = screen.getByRole('group', { name: 'Download your data' });
    expect(card).toHaveTextContent('Not requested');
    expect(card).toHaveTextContent('It can take up to two days.');
    for (const box of screen.getAllByRole('checkbox')) expect(box).toBeChecked();
    await user.click(screen.getByRole('checkbox', { name: 'Messages' }));
    await user.selectOptions(screen.getByRole('combobox', { name: 'Format' }), 'csv');
    await expectNoA11yViolations();
    await user.click(screen.getByRole('button', { name: 'Request export' }));
    expect(onRequest).toHaveBeenCalledWith({ scope: ['profile', 'files'], format: 'csv' });
  });

  it('requires at least one kind of data and focuses the checklist', async () => {
    const user = userEvent.setup();
    const onRequest = vi.fn();
    render(<DataExportRequest {...fixed} scope={scope} onRequest={onRequest} />);
    for (const name of ['Profile', 'Messages', 'Files']) {
      await user.click(screen.getByRole('checkbox', { name }));
    }
    await user.click(screen.getByRole('button', { name: 'Request export' }));
    expect(onRequest).not.toHaveBeenCalled();
    expect(screen.getByRole('checkbox', { name: 'Profile' })).toHaveFocus();
    expect(screen.getByRole('group', { name: 'Include' })).toHaveAccessibleDescription(
      'Choose at least one kind of data.',
    );
    await user.click(screen.getByRole('checkbox', { name: 'Files' }));
    expect(screen.queryByText('Choose at least one kind of data.')).toBeNull();
    await user.click(screen.getByRole('button', { name: 'Request export' }));
    expect(onRequest).toHaveBeenCalledWith({ scope: ['files'] });
  });

  it('is busy while onRequest is pending and shows the rejection', async () => {
    const user = userEvent.setup();
    let reject!: (error: Error) => void;
    const onRequest = vi.fn(
      () =>
        new Promise<void>((_, r) => {
          reject = r;
        }),
    );
    render(<Export onRequest={onRequest} />);
    const button = screen.getByRole('button', { name: 'Request export' });
    await user.click(button);
    expect(button).toHaveAttribute('aria-busy', 'true');
    await user.click(button);
    expect(onRequest).toHaveBeenCalledOnce();
    reject(new Error('Too many requests today.'));
    expect(await screen.findByRole('alert')).toHaveTextContent('Too many requests today.');
    expect(button).not.toHaveAttribute('aria-busy');
  });

  it('shows requested and preparing progress as text, with a progress bar while preparing', async () => {
    const { rerender } = render(
      <Export status="requested" requestedAt="2026-05-04T10:00:00.000Z" />,
    );
    expect(screen.getByText(/We received your request on/)).toHaveTextContent(
      'We received your request on 4 May 2026, 10:00.',
    );
    expect(screen.queryByRole('button')).toBeNull();
    expect(screen.queryByRole('progressbar')).toBeNull();
    rerender(<Export status="preparing" requestedAt="2026-05-04T10:00:00.000Z" />);
    expect(screen.getByText(/being prepared/)).toBeInTheDocument();
    expect(screen.getByRole('progressbar', { name: 'Preparing your export' })).not.toHaveAttribute(
      'aria-valuenow',
    );
    expect(
      screen.getByText('It can take up to two days. We will email you when it is ready.'),
    ).toBeInTheDocument();
    await expectNoA11yViolations();
  });

  it('offers the download when ready, as a link or a button', async () => {
    const user = userEvent.setup();
    const onDownload = vi.fn();
    const { rerender } = render(
      <Export
        status="ready"
        readyAt="2026-05-05T08:00:00.000Z"
        expiresAt="2026-05-12T08:00:00.000Z"
        downloadHref="/exports/abc.zip"
        onDownload={onDownload}
      />,
    );
    const card = screen.getByRole('group', { name: 'Download your data' });
    expect(within(card).getByText('Ready')).toBeInTheDocument();
    expect(card).toHaveTextContent(
      'Your export is ready. Prepared on 5 May 2026, 08:00. You can download it until 12 May 2026, 08:00.',
    );
    const link = screen.getByRole('link', { name: 'Download' });
    expect(link).toHaveAttribute('href', '/exports/abc.zip');
    expect(link).toHaveAttribute('download');
    expect(screen.queryByRole('button', { name: /Request/ })).toBeNull();
    await expectNoA11yViolations();

    rerender(<Export status="ready" onDownload={onDownload} />);
    await user.click(screen.getByRole('button', { name: 'Download' }));
    expect(onDownload).toHaveBeenCalledOnce();
  });

  it('lets the person request again after expiry or failure', () => {
    const { rerender } = render(<Export status="expired" expiresAt="2026-05-12T08:00:00.000Z" />);
    expect(screen.getByText(/The download expired on/)).toHaveTextContent('12 May 2026, 08:00');
    expect(screen.getByRole('button', { name: 'Request a new export' })).toBeInTheDocument();
    rerender(<Export status="failed" failureReason="The archive was too large." />);
    expect(screen.getByText('Failed')).toBeInTheDocument();
    expect(screen.getByText('The archive was too large.')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Request a new export' })).toBeInTheDocument();
  });

  it('takes strings from messages and renders on the server', () => {
    render(
      <Export
        messages={{
          title: 'Export account data',
          request: 'Start export',
          statuses: { idle: 'New' } as never,
        }}
      />,
    );
    expect(screen.getByRole('group', { name: 'Export account data' })).toHaveTextContent('New');
    expect(screen.getByRole('button', { name: 'Start export' })).toBeInTheDocument();
    const html = renderToString(
      <Export status="preparing" requestedAt="2026-05-04T10:00:00.000Z" />,
    );
    expect(html).toContain('mn-data-export-request');
    expect(html).toContain('4 May 2026, 10:00');
  });
});
