import { describe, expect, it, vi } from 'vitest';
import { fireEvent, render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { RecoveryCodes, formatRecoveryCodes } from './index.js';

const CODES = ['aaaa-1111', 'bbbb-2222', 'cccc-3333', 'dddd-4444'];

describe('RecoveryCodes', () => {
  it('lists codes and marks used ones', async () => {
    render(
      <RecoveryCodes
        data-testid="root"
        codes={[...CODES.slice(0, 3), { code: 'dddd-4444', used: true }]}
      />,
    );
    expect(screen.getByTestId('root')).toHaveClass('mn-recovery-codes');
    const items = within(screen.getByRole('list', { name: 'Recovery codes' })).getAllByRole(
      'listitem',
    );
    expect(items).toHaveLength(4);
    expect(items[3]).toHaveAttribute('data-used', 'true');
    expect(items[3]).toHaveTextContent('dddd-4444 (used)');
    expect(screen.getByText('3 of 4 codes left')).toBeInTheDocument();
    await expectNoA11yViolations();
  });

  it('copies the unused codes and downloads a text file', async () => {
    const user = userEvent.setup();
    const writeText = vi.spyOn(navigator.clipboard, 'writeText');
    const createObjectURL = vi.fn(() => 'blob:codes');
    const revokeObjectURL = vi.fn();
    Object.assign(URL, { createObjectURL, revokeObjectURL });
    render(
      <RecoveryCodes
        codes={[...CODES.slice(0, 2), { code: 'cccc-3333', used: true }]}
        fileName="example-codes.txt"
      />,
    );
    await user.click(screen.getByRole('button', { name: 'Copy codes' }));
    expect(writeText).toHaveBeenCalledWith('aaaa-1111\nbbbb-2222');
    vi.useFakeTimers();
    try {
      fireEvent.click(screen.getByRole('button', { name: 'Download' }));
      expect(createObjectURL).toHaveBeenCalledOnce();
      // Not in the same task: that can cancel the download before it starts.
      expect(revokeObjectURL).not.toHaveBeenCalled();
      vi.advanceTimersByTime(1000);
      expect(revokeObjectURL).toHaveBeenCalledWith('blob:codes');
    } finally {
      vi.useRealTimers();
    }
  });

  it('hides and reveals the codes', async () => {
    const user = userEvent.setup();
    render(<RecoveryCodes codes={CODES} defaultRevealed={false} />);
    expect(screen.queryByRole('list')).toBeNull();
    const toggle = screen.getByRole('button', { name: 'Show codes' });
    expect(toggle).toHaveAttribute('aria-pressed', 'false');
    await user.click(toggle);
    expect(screen.getByRole('list', { name: 'Recovery codes' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Hide codes' })).toHaveAttribute(
      'aria-pressed',
      'true',
    );
  });

  it('confirms before regenerating and announces the new codes', async () => {
    const user = userEvent.setup();
    const onRegenerate = vi.fn(() => Promise.resolve());
    render(<RecoveryCodes codes={CODES} onRegenerate={onRegenerate} />);
    const button = screen.getByRole('button', { name: 'Generate new codes' });
    await user.click(button);
    const dialog = await screen.findByRole('alertdialog', {
      name: 'Generate new recovery codes?',
    });
    expect(dialog).toHaveTextContent('Your current codes will stop working');
    expect(within(dialog).getByRole('button', { name: 'Cancel' })).toHaveFocus();
    await user.keyboard('{Escape}');
    expect(onRegenerate).not.toHaveBeenCalled();
    await waitFor(() => expect(button).toHaveFocus());
    await user.click(button);
    await user.click(
      within(await screen.findByRole('alertdialog')).getByRole('button', {
        name: 'Generate new codes',
      }),
    );
    expect(onRegenerate).toHaveBeenCalledOnce();
    await waitFor(() =>
      expect(screen.getAllByRole('status').at(-1)).toHaveTextContent(
        'New recovery codes generated',
      ),
    );
  });

  it('requires the acknowledgement before continuing', async () => {
    const user = userEvent.setup();
    const onAcknowledge = vi.fn();
    const { rerender } = render(<RecoveryCodes codes={CODES} onAcknowledge={onAcknowledge} />);
    await user.click(screen.getByRole('button', { name: 'Continue' }));
    expect(onAcknowledge).not.toHaveBeenCalled();
    expect(screen.getByRole('alert')).toHaveTextContent('Confirm that you have saved the codes.');
    const box = screen.getByRole('checkbox', { name: 'I have saved my recovery codes' });
    expect(box).toHaveAccessibleDescription('Confirm that you have saved the codes.');
    await user.click(box);
    await user.keyboard('{Tab}{Enter}');
    expect(onAcknowledge).toHaveBeenCalledOnce();
    rerender(<RecoveryCodes codes={['eeee-5555']} onAcknowledge={onAcknowledge} />);
    await waitFor(() => expect(screen.getByRole('checkbox')).not.toBeChecked());
  });

  it('takes translated messages and formats text with a heading', () => {
    render(<RecoveryCodes codes={CODES} messages={{ listLabel: 'Wiederherstellungscodes' }} />);
    expect(screen.getByRole('list', { name: 'Wiederherstellungscodes' })).toBeInTheDocument();
    expect(formatRecoveryCodes(['a', 'b'], 'Example account')).toBe('Example account\n\na\nb');
    expect(formatRecoveryCodes(['a'])).toBe('a');
  });

  it('renders on the server', () => {
    const html = renderToString(<RecoveryCodes codes={CODES} />);
    expect(html).toContain('mn-recovery-codes');
    expect(html).toContain('aaaa-1111');
  });
});
