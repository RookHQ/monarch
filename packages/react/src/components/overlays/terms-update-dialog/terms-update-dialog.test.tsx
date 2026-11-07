import { describe, expect, it, vi } from 'vitest';
import { useState } from 'react';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { formatEffectiveDate, TermsUpdateDialog, type TermsUpdateDialogProps } from './index.js';

function deferred() {
  let resolve!: () => void;
  let reject!: (error: unknown) => void;
  const promise = new Promise<void>((res, rej) => {
    resolve = res;
    reject = rej;
  });
  return { promise, resolve, reject };
}

function Update(props: Partial<TermsUpdateDialogProps>) {
  return (
    <TermsUpdateDialog
      defaultOpen
      locale="en-GB"
      summary="We now explain how long we keep support messages."
      effectiveDate="2026-12-01"
      documents={[
        { label: 'Terms of Service', href: '/legal/terms', version: '2026-12' },
        { label: 'Privacy Policy', href: '/legal/privacy' },
      ]}
      onAccept={() => {}}
      {...props}
    />
  );
}

describe('TermsUpdateDialog', () => {
  it('opens as a labelled dialog with summary, effective date and document links', async () => {
    render(<Update />);
    const dialog = await screen.findByRole('dialog', { name: 'We’ve updated our terms' });
    expect(dialog).toHaveAttribute('data-mode', 'blocking');
    expect(dialog).toHaveAccessibleDescription('We now explain how long we keep support messages.');
    expect(screen.getByText('Effective from 1 December 2026')).toHaveAttribute(
      'datetime',
      '2026-12-01',
    );
    expect(screen.getByRole('link', { name: 'Terms of Service' })).toHaveAttribute(
      'href',
      '/legal/terms',
    );
    expect(screen.getByText('(version 2026-12)')).toBeInTheDocument();
    expect(screen.getByRole('region', { name: 'Read the full documents' })).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Close' })).not.toBeInTheDocument();
    await expectNoA11yViolations(dialog);
  });

  it('blocking: Escape and outside clicks do not dismiss it, and focus stays inside', async () => {
    const onOpenChange = vi.fn();
    render(<Update onOpenChange={onOpenChange} />);
    const user = userEvent.setup();
    const dialog = await screen.findByRole('dialog');
    await user.keyboard('{Escape}');
    await user.click(document.querySelector('.mn-overlay')!);
    expect(screen.getByRole('dialog')).toBeInTheDocument();
    expect(onOpenChange).not.toHaveBeenCalled();
    for (let i = 0; i < 4; i++) {
      await user.tab();
      expect(dialog.contains(document.activeElement)).toBe(true);
    }
  });

  it('blocking: accepting closes it', async () => {
    const onAccept = vi.fn();
    render(<Update onAccept={onAccept} />);
    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: 'Accept and continue' }));
    expect(onAccept).toHaveBeenCalledOnce();
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
  });

  it('blocking: the decline action is the only other way out and does not close it', async () => {
    const onDecline = vi.fn();
    const onAccept = vi.fn();
    render(<Update onDecline={onDecline} declineLabel="Sign out" onAccept={onAccept} />);
    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: 'Sign out' }));
    expect(onDecline).toHaveBeenCalledOnce();
    expect(onAccept).not.toHaveBeenCalled();
    expect(screen.getByRole('dialog')).toBeInTheDocument();
  });

  it('async accept: pending locks the dialog, success closes it', async () => {
    const work = deferred();
    const onOpenChange = vi.fn();
    render(
      <Update
        onAccept={() => work.promise}
        onDecline={() => {}}
        declineLabel="Sign out"
        onOpenChange={onOpenChange}
      />,
    );
    const user = userEvent.setup();
    const accept = screen.getByRole('button', { name: 'Accept and continue' });
    await user.click(accept);
    expect(screen.getByRole('dialog')).toHaveAttribute('aria-busy', 'true');
    expect(accept).toHaveAttribute('aria-busy', 'true');
    expect(accept).toHaveFocus();
    expect(screen.getByRole('button', { name: 'Sign out' })).toBeDisabled();
    await user.click(accept);
    work.resolve();
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
    expect(onOpenChange).toHaveBeenCalledTimes(1);
    expect(onOpenChange).toHaveBeenCalledWith(false);
  });

  it('async accept: a rejection shows the error, keeps it open and allows retry', async () => {
    let attempt = 0;
    const onAccept = vi.fn(async () => {
      attempt += 1;
      if (attempt === 1) throw new Error('We could not save your choice.');
    });
    render(<Update onAccept={onAccept} />);
    const user = userEvent.setup();
    const accept = screen.getByRole('button', { name: 'Accept and continue' });
    await user.click(accept);
    const alert = await screen.findByRole('alert');
    expect(alert).toHaveTextContent('We could not save your choice.');
    expect(accept).toHaveAccessibleDescription('We could not save your choice.');
    expect(accept).not.toHaveAttribute('aria-busy');
    await user.click(accept);
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
    expect(onAccept).toHaveBeenCalledTimes(2);
  });

  it('shows a generic message for non-Error rejections, and a controlled error', async () => {
    const { rerender } = render(<Update onAccept={() => Promise.reject('nope')} />);
    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: 'Accept and continue' }));
    expect(await screen.findByRole('alert')).toHaveTextContent('That did not work. Try again.');
    rerender(<Update error="Saving is paused." />);
    expect(screen.getByRole('alert')).toHaveTextContent('Saving is paused.');
  });

  it('informational: dismissible with Escape, and "Got it" accepts', async () => {
    function Notice({ onAccept }: { onAccept: () => void }) {
      const [open, setOpen] = useState(true);
      return (
        <>
          <button type="button" onClick={() => setOpen(true)}>
            Show notice
          </button>
          <Update mode="informational" open={open} onOpenChange={setOpen} onAccept={onAccept} />
        </>
      );
    }
    const onAccept = vi.fn();
    render(<Notice onAccept={onAccept} />);
    const user = userEvent.setup();
    expect(await screen.findByRole('dialog')).toHaveAttribute('data-mode', 'informational');
    expect(screen.getByRole('button', { name: 'Close' })).toBeInTheDocument();
    await user.keyboard('{Escape}');
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
    expect(onAccept).not.toHaveBeenCalled();
    await user.click(screen.getByRole('button', { name: 'Show notice' }));
    await user.click(await screen.findByRole('button', { name: 'Got it' }));
    expect(onAccept).toHaveBeenCalledOnce();
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
  });

  it('formats dates with Intl and the locale, keeping calendar dates in every time zone', () => {
    expect(formatEffectiveDate('2026-12-01', 'en-US').text).toBe('December 1, 2026');
    expect(formatEffectiveDate('2026-12-01', 'de-DE').text).toBe('1. Dezember 2026');
    const instant = formatEffectiveDate(new Date('2026-12-01T12:00:00Z'), 'en-GB');
    expect(instant.iso).toBe('2026-12-01T12:00:00.000Z');
    expect(formatEffectiveDate('not a date').text).toBe('not a date');
  });

  it('renders on the server (closed and open) without throwing', () => {
    expect(() => renderToString(<Update defaultOpen={false} />)).not.toThrow();
    expect(() => renderToString(<Update />)).not.toThrow();
  });
});
