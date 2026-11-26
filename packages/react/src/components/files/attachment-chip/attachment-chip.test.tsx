import { describe, expect, it, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { createRef } from 'react';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { AttachmentChip } from './index.js';

describe('AttachmentChip', () => {
  it('shows the name and size and removes from the keyboard', async () => {
    const user = userEvent.setup();
    const onRemove = vi.fn();
    const ref = createRef<HTMLDivElement>();
    render(<AttachmentChip ref={ref} name="brief.pdf" size={2048} onRemove={onRemove} />);
    expect(ref.current).toHaveClass('mn-attachment-chip');
    expect(ref.current).toHaveAttribute('data-status', 'done');
    expect(screen.getByText('brief.pdf')).toHaveAttribute('title', 'brief.pdf');
    expect(screen.getByText('2 KB')).toBeInTheDocument();
    await user.tab();
    expect(screen.getByRole('button', { name: 'Remove brief.pdf' })).toHaveFocus();
    await user.keyboard('{Enter}');
    expect(onRemove).toHaveBeenCalledTimes(1);
    await expectNoA11yViolations();
  });

  it('shows upload progress as a named progress bar', async () => {
    const { container, rerender } = render(
      <AttachmentChip name="photo.png" status="uploading" progress={42.4} />,
    );
    const bar = screen.getByRole('progressbar', { name: 'Uploading photo.png' });
    expect(bar).toHaveAttribute('aria-valuenow', '42');
    expect(screen.getByText('42%')).toBeInTheDocument();
    await expectNoA11yViolations();
    rerender(<AttachmentChip name="photo.png" status="uploading" />);
    expect(screen.getByRole('progressbar')).not.toHaveAttribute('aria-valuenow');
    expect(container.querySelector('[data-indeterminate]')).not.toBeNull();
  });

  it('announces a failure and offers retry', async () => {
    const user = userEvent.setup();
    const onRetry = vi.fn();
    render(
      <AttachmentChip
        name="big.zip"
        status="failed"
        error="Too large"
        onRetry={onRetry}
        onRemove={() => {}}
      />,
    );
    expect(screen.getByRole('alert')).toHaveTextContent('Too large');
    await user.click(screen.getByRole('button', { name: 'Retry big.zip' }));
    expect(onRetry).toHaveBeenCalled();
    await expectNoA11yViolations();
  });

  it('links a finished file only when the URL is safe', () => {
    const { rerender } = render(
      <AttachmentChip name="a.txt" href="https://example.com/a.txt" size={10} />,
    );
    const link = screen.getByRole('link', { name: 'a.txt' });
    expect(link).toHaveAttribute('target', '_blank');
    expect(link).toHaveAccessibleDescription('10 B');
    rerender(<AttachmentChip name="a.txt" href="/files/a.txt" download />);
    expect(screen.getByRole('link')).toHaveAttribute('download', 'a.txt');
    rerender(<AttachmentChip name="a.txt" href="javascript:alert(1)" />);
    expect(screen.queryByRole('link')).toBeNull();
    rerender(<AttachmentChip name="a.txt" href="/files/a.txt" status="uploading" />);
    expect(screen.queryByRole('link')).toBeNull();
  });

  it('shows queued and cancelled states, disables buttons and translates', () => {
    const { rerender } = render(
      <AttachmentChip
        name="x.doc"
        status="queued"
        onRemove={() => {}}
        disabled
        messages={{ queued: 'En attente', remove: (n) => `Retirer ${n}` }}
      />,
    );
    expect(screen.getByText('En attente')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Retirer x.doc' })).toBeDisabled();
    rerender(<AttachmentChip name="x.doc" status="cancelled" />);
    expect(screen.getByText('Cancelled')).toBeInTheDocument();
    expect(screen.queryByRole('button')).toBeNull();
  });

  it('renders on the server', () => {
    const html = renderToString(<AttachmentChip name="notes.md" size={100} onRemove={() => {}} />);
    expect(html).toContain('mn-attachment-chip');
    expect(html).toContain('notes.md');
  });
});
