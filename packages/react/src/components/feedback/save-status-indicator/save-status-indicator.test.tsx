import { describe, expect, it, vi } from 'vitest';
import { act, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { SaveStatusIndicator } from './index.js';

const NOW = new Date('2026-03-02T10:05:00Z');

describe('SaveStatusIndicator', () => {
  it('shows saving, then saved with relative time, and announces only the outcome', async () => {
    vi.useFakeTimers();
    try {
      const { rerender } = render(<SaveStatusIndicator status="idle" now={NOW} locale="en" />);
      const live = screen.getByRole('status');
      expect(live).toBeEmptyDOMElement();
      rerender(<SaveStatusIndicator status="saving" now={NOW} locale="en" />);
      expect(screen.getByText('Saving…')).toBeInTheDocument();
      act(() => void vi.advanceTimersByTime(100));
      expect(live).toBeEmptyDOMElement();
      rerender(
        <SaveStatusIndicator
          status="saved"
          savedAt={new Date('2026-03-02T10:03:00Z')}
          now={NOW}
          locale="en"
        />,
      );
      act(() => void vi.advanceTimersByTime(100));
      expect(live).toHaveTextContent('Saved');
      expect(screen.getByText('2 minutes ago')).toBeInTheDocument();
      expect(document.querySelector('time')).toHaveAttribute(
        'datetime',
        '2026-03-02T10:03:00.000Z',
      );
    } finally {
      vi.useRealTimers();
    }
    await expectNoA11yViolations();
  });

  it('shows the error with a Retry button and announces it', async () => {
    const user = userEvent.setup();
    const onRetry = vi.fn();
    const { rerender } = render(<SaveStatusIndicator status="saving" onRetry={onRetry} />);
    expect(screen.queryByRole('button', { name: 'Retry' })).not.toBeInTheDocument();
    rerender(<SaveStatusIndicator status="error" onRetry={onRetry} />);
    expect(screen.getByText("Couldn't save")).toBeInTheDocument();
    await waitFor(() => expect(screen.getByRole('status')).toHaveTextContent("Couldn't save"));
    await user.click(screen.getByRole('button', { name: 'Retry' }));
    expect(onRetry).toHaveBeenCalledOnce();
    await expectNoA11yViolations();
  });

  it('uses a custom error, custom messages and optional saving announcements', async () => {
    const { rerender } = render(
      <SaveStatusIndicator status="unsaved" announceSaving messages={{ saving: 'Speichern…' }} />,
    );
    expect(screen.getByText('Unsaved changes')).toBeInTheDocument();
    rerender(
      <SaveStatusIndicator status="saving" announceSaving messages={{ saving: 'Speichern…' }} />,
    );
    expect(await screen.findByText('Speichern…', { selector: '[role="status"]' })).toBeVisible();
    rerender(<SaveStatusIndicator status="error" error="You are offline" />);
    expect(screen.getByText('You are offline', { selector: 'span:not([role])' })).toBeVisible();
    expect(screen.getByRole('status').closest('[data-status]')).toHaveAttribute(
      'data-status',
      'error',
    );
  });

  it('hides decorative icons and the separator from assistive technology', () => {
    render(<SaveStatusIndicator status="saved" savedAt={NOW} now={NOW} />);
    const root = document.querySelector('.mn-save-status-indicator')!;
    expect(root.querySelector('.mn-save-status-indicator__icon')).toHaveAttribute('aria-hidden');
    expect(root.querySelector('.mn-save-status-indicator__separator')).toHaveAttribute(
      'aria-hidden',
    );
  });

  it('renders on the server', () => {
    const html = renderToString(
      <SaveStatusIndicator status="saved" savedAt="2026-03-02T10:00:00Z" now={NOW} locale="en" />,
    );
    expect(html).toContain('mn-save-status-indicator');
    expect(html).toContain('Saved');
    expect(html).toContain('5 minutes ago');
  });
});
