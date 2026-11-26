import { describe, expect, it, vi } from 'vitest';
import { act, render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { IncomingCall } from './index.js';

const caller = { name: 'Robin Park' };

describe('IncomingCall', () => {
  it('is a named alert dialog with Decline and Answer', async () => {
    const onAccept = vi.fn();
    const onDecline = vi.fn();
    render(<IncomingCall caller={caller} onAccept={onAccept} onDecline={onDecline} />);
    const dialog = screen.getByRole('alertdialog', { name: 'Incoming call from Robin Park' });
    expect(dialog).toHaveAccessibleDescription('Incoming call');
    expect(dialog).toHaveAttribute('data-placement', 'floating');
    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: 'Answer' }));
    expect(onAccept).toHaveBeenCalledWith({ video: false });
    await user.click(screen.getByRole('button', { name: 'Decline' }));
    expect(onDecline).toHaveBeenCalledOnce();
    await expectNoA11yViolations();
  });

  it('offers audio-only for a video call', async () => {
    const onAccept = vi.fn();
    render(<IncomingCall caller={caller} kind="video" onAccept={onAccept} onDecline={() => {}} />);
    expect(
      screen.getByRole('alertdialog', { name: 'Incoming video call from Robin Park' }),
    ).toBeInTheDocument();
    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: 'Answer with audio only' }));
    expect(onAccept).toHaveBeenLastCalledWith({ video: false });
    await user.click(screen.getByRole('button', { name: 'Answer with video' }));
    expect(onAccept).toHaveBeenLastCalledWith({ video: true });
  });

  it('does not take focus unless asked, and is keyboard operable', async () => {
    const onAccept = vi.fn();
    const { rerender } = render(
      <IncomingCall caller={caller} onAccept={onAccept} onDecline={() => {}} />,
    );
    expect(document.body).toHaveFocus();
    const user = userEvent.setup();
    await user.tab();
    expect(screen.getByRole('button', { name: 'Decline' })).toHaveFocus();
    await user.tab();
    await user.keyboard('{Enter}');
    expect(onAccept).toHaveBeenCalledOnce();
    rerender(<></>);
    render(<IncomingCall caller={caller} focusAnswer onAccept={() => {}} onDecline={() => {}} />);
    expect(screen.getByRole('button', { name: 'Answer' })).toHaveFocus();
  });

  it('disables answering while joining', () => {
    render(
      <IncomingCall
        caller={caller}
        kind="video"
        joining
        onAccept={() => {}}
        onDecline={() => {}}
      />,
    );
    expect(screen.getByRole('button', { name: 'Answer with video' })).toBeDisabled();
    expect(screen.getByRole('button', { name: 'Answer with audio only' })).toBeDisabled();
    expect(screen.getByRole('button', { name: 'Decline' })).toBeEnabled();
  });

  it('times out once', () => {
    vi.useFakeTimers();
    try {
      const onTimeout = vi.fn();
      render(
        <IncomingCall
          caller={caller}
          timeoutMs={30_000}
          onTimeout={onTimeout}
          onAccept={() => {}}
          onDecline={() => {}}
        />,
      );
      act(() => {
        vi.advanceTimersByTime(29_999);
      });
      expect(onTimeout).not.toHaveBeenCalled();
      act(() => {
        vi.advanceTimersByTime(1);
      });
      expect(onTimeout).toHaveBeenCalledOnce();
    } finally {
      vi.useRealTimers();
    }
  });

  it('translates strings and shows a detail line', () => {
    render(
      <IncomingCall
        caller={caller}
        placement="inline"
        detail="Project room"
        onAccept={() => {}}
        onDecline={() => {}}
        messages={{ decline: 'Ablehnen', label: (n) => `Anruf von ${n}` }}
      />,
    );
    expect(screen.getByRole('alertdialog', { name: 'Anruf von Robin Park' })).toHaveAttribute(
      'data-placement',
      'inline',
    );
    expect(screen.getByRole('button', { name: 'Ablehnen' })).toBeInTheDocument();
    expect(screen.getByText('Project room')).toBeInTheDocument();
  });

  it('renders on the server', () => {
    const html = renderToString(
      <IncomingCall caller={caller} onAccept={() => {}} onDecline={() => {}} />,
    );
    expect(html).toContain('mn-incoming-call');
  });
});
