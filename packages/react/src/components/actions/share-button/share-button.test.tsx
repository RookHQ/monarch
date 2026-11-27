import { afterEach, describe, expect, it, vi } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { ShareButton, emailShareTarget, type ShareTarget } from './index.js';

const URL_ = 'https://example.com/reports/42';

function mockShare(impl?: (data: unknown) => Promise<void>, canShare?: (data: unknown) => boolean) {
  const share = vi.fn(impl ?? (() => Promise.resolve()));
  Object.defineProperty(navigator, 'share', { configurable: true, value: share });
  Object.defineProperty(navigator, 'canShare', {
    configurable: true,
    value: canShare ? vi.fn(canShare) : undefined,
  });
  return share;
}

function removeShare() {
  // jsdom has no Web Share API; make sure no earlier test left one behind.
  delete (navigator as { share?: unknown }).share;
  delete (navigator as { canShare?: unknown }).canShare;
}

afterEach(removeShare);

describe('ShareButton', () => {
  it('shows the panel from the controlled open prop', async () => {
    const onOpenChange = vi.fn();
    render(<ShareButton url={URL_} title="Report" open onOpenChange={onOpenChange} />);
    expect(await screen.findByRole('textbox')).toHaveValue(URL_);
    await userEvent.keyboard('{Escape}');
    expect(onOpenChange).toHaveBeenCalledWith(false);
  });

  it('never shares on render and uses the system share sheet on click', async () => {
    const user = userEvent.setup();
    const share = mockShare();
    const onShare = vi.fn();
    render(<ShareButton url={URL_} title="Q3 report" text="Numbers" onShare={onShare} />);
    expect(share).not.toHaveBeenCalled();
    const button = screen.getByRole('button', { name: 'Share' });
    expect(button).not.toHaveAttribute('aria-haspopup');
    await user.click(button);
    expect(share).toHaveBeenCalledWith({ url: URL_, title: 'Q3 report', text: 'Numbers' });
    await waitFor(() => expect(onShare).toHaveBeenCalledWith({ method: 'native' }));
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
  });

  it('treats a cancelled share sheet as a cancel, not an error', async () => {
    const user = userEvent.setup();
    mockShare(() => Promise.reject(Object.assign(new Error('cancel'), { name: 'AbortError' })));
    const onShareCancel = vi.fn();
    const onShareError = vi.fn();
    render(<ShareButton url={URL_} onShareCancel={onShareCancel} onShareError={onShareError} />);
    await user.click(screen.getByRole('button', { name: 'Share' }));
    await waitFor(() => expect(onShareCancel).toHaveBeenCalledOnce());
    expect(onShareError).not.toHaveBeenCalled();
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
  });

  it('falls back to the panel when the share sheet fails or cannot share the data', async () => {
    const user = userEvent.setup();
    mockShare(() => Promise.reject(new Error('NotAllowed')));
    const onShareError = vi.fn();
    const { unmount } = render(<ShareButton url={URL_} onShareError={onShareError} />);
    await user.click(screen.getByRole('button', { name: 'Share' }));
    expect(await screen.findByRole('dialog', { name: 'Share' })).toBeInTheDocument();
    expect(onShareError).toHaveBeenCalledWith(expect.any(Error));
    unmount();

    const share = mockShare(undefined, () => false);
    render(<ShareButton url={URL_} />);
    await user.click(screen.getByRole('button', { name: 'Share' }));
    expect(await screen.findByRole('dialog', { name: 'Share' })).toBeInTheDocument();
    expect(share).not.toHaveBeenCalled();
  });

  it('opens a panel with the link, copy and safe targets when there is no share sheet; Escape returns focus', async () => {
    const user = userEvent.setup();
    const writeText = vi.fn(() => Promise.resolve());
    Object.defineProperty(navigator, 'clipboard', { configurable: true, value: { writeText } });
    const onShare = vi.fn();
    const unsafe: ShareTarget = {
      id: 'bad',
      label: 'Bad',
      href: () => 'javascript:alert(1)',
    };
    render(
      <ShareButton
        url={URL_}
        title="Q3 report"
        targets={[emailShareTarget(), unsafe]}
        onShare={onShare}
      />,
    );
    const button = screen.getByRole('button', { name: 'Share' });
    await waitFor(() => expect(button).toHaveAttribute('aria-haspopup', 'dialog'));
    expect(button).toHaveAttribute('aria-expanded', 'false');
    await user.click(button);
    const dialog = await screen.findByRole('dialog', { name: 'Share' });
    expect(button).toHaveAttribute('aria-expanded', 'true');
    expect(screen.getByRole('textbox', { name: 'Link' })).toHaveValue(URL_);
    const email = screen.getByRole('link', { name: 'Email' });
    expect(email).toHaveAttribute(
      'href',
      `mailto:?subject=Q3%20report&body=${encodeURIComponent(URL_)}`,
    );
    expect(email).toHaveAttribute('rel', 'noopener noreferrer');
    expect(screen.queryByRole('link', { name: 'Bad' })).not.toBeInTheDocument();
    await expectNoA11yViolations(dialog);

    await user.click(screen.getByRole('button', { name: 'Copy link' }));
    expect(writeText).toHaveBeenCalledWith(URL_);
    expect(onShare).toHaveBeenCalledWith({ method: 'copy' });

    await user.keyboard('{Escape}');
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
    expect(button).toHaveFocus();
  });

  it('always uses the panel with mode="panel" and supports icon-only with custom messages', async () => {
    const user = userEvent.setup();
    const share = mockShare();
    render(
      <ShareButton
        url={URL_}
        mode="panel"
        iconOnly
        icon={<svg />}
        messages={{ label: 'Share report', panelLabel: 'Share report', copy: 'Copy' }}
      />,
    );
    await user.click(screen.getByRole('button', { name: 'Share report' }));
    expect(await screen.findByRole('dialog', { name: 'Share report' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Copy' })).toBeInTheDocument();
    expect(share).not.toHaveBeenCalled();
  });

  it('translates the copy failure text of the panel', async () => {
    Object.defineProperty(navigator, 'clipboard', {
      configurable: true,
      value: { writeText: () => Promise.reject(new Error('refused')) },
    });
    Object.defineProperty(document, 'execCommand', { configurable: true, value: () => false });
    const user = userEvent.setup();
    Object.defineProperty(navigator, 'clipboard', {
      configurable: true,
      value: { writeText: () => Promise.reject(new Error('refused')) },
    });
    render(
      <ShareButton
        url={URL_}
        mode="panel"
        messages={{
          copy: 'Kopieren',
          copyFailed: 'Nicht kopiert',
          copyFailedAnnouncement: 'Kopieren fehlgeschlagen.',
        }}
      />,
    );
    await user.click(screen.getByRole('button', { name: 'Share' }));
    await user.click(screen.getByRole('button', { name: 'Kopieren' }));
    expect(await screen.findByText('Nicht kopiert')).toBeInTheDocument();
    expect(screen.getByText('Kopieren fehlgeschlagen.')).toBeInTheDocument();
    expect(screen.queryByText("Couldn't copy")).toBeNull();
  });

  it('renders on the server without touching navigator', () => {
    const html = renderToString(<ShareButton url={URL_} />);
    expect(html).toContain('mn-share-button');
    expect(html).toContain('Share');
  });
});
