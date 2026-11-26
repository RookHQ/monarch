import { afterEach, describe, expect, it, vi } from 'vitest';
import { act, render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { createRef } from 'react';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { RecordingIndicator } from './index.js';

afterEach(() => vi.useRealTimers());

describe('RecordingIndicator', () => {
  it('shows REC with ticking time, read as "Recording"', async () => {
    vi.useFakeTimers({ now: new Date('2026-01-01T10:00:00Z') });
    const ref = createRef<HTMLDivElement>();
    const { container } = render(
      <RecordingIndicator ref={ref} recording since="2026-01-01T09:58:55Z" />,
    );
    expect(ref.current).toHaveClass('mn-recording-indicator');
    expect(ref.current).toHaveAttribute('data-recording');
    expect(screen.getByText('REC')).toHaveAttribute('aria-hidden', 'true');
    expect(screen.getByText('Recording')).toHaveClass('mn-visually-hidden');
    const time = container.querySelector('time')!;
    expect(time).toHaveTextContent('01:05');
    act(() => {
      vi.advanceTimersByTime(2000);
    });
    expect(time).toHaveTextContent('01:07');
    expect(time).toHaveAttribute('dateTime', 'PT67S');
    expect(screen.getByRole('status')).toHaveTextContent('');
    vi.useRealTimers();
    await expectNoA11yViolations();
  });

  it('announces start, pause, resume and stop once each', () => {
    const { rerender, container } = render(<RecordingIndicator recording={false} />);
    const status = screen.getByRole('status');
    expect(container.querySelector('.mn-recording-indicator__badge')).toBeNull();
    rerender(<RecordingIndicator recording />);
    expect(status).toHaveTextContent('Recording started');
    rerender(<RecordingIndicator recording paused elapsed={5000} />);
    expect(status).toHaveTextContent('Recording paused');
    expect(screen.getByText('Paused')).toBeInTheDocument();
    expect(screen.getByText('Recording, Paused')).toBeInTheDocument();
    expect(container.firstElementChild).toHaveAttribute('data-paused');
    rerender(<RecordingIndicator recording elapsed={5000} />);
    expect(status).toHaveTextContent('Recording resumed');
    rerender(<RecordingIndicator recording={false} />);
    expect(status).toHaveTextContent('Recording stopped');
  });

  it('shows a given elapsed time without ticking and can hide the time', () => {
    vi.useFakeTimers();
    const { container, rerender } = render(<RecordingIndicator recording elapsed={3_725_000} />);
    expect(container.querySelector('time')).toHaveTextContent('1:02:05');
    act(() => {
      vi.advanceTimersByTime(5000);
    });
    expect(container.querySelector('time')).toHaveTextContent('1:02:05');
    rerender(<RecordingIndicator recording elapsed={1000} showElapsed={false} />);
    expect(container.querySelector('time')).toBeNull();
    rerender(<RecordingIndicator recording />);
    expect(container.querySelector('time')).toBeNull();
  });

  it('stops from a named button', async () => {
    const user = userEvent.setup();
    const onStop = vi.fn();
    render(<RecordingIndicator recording onStop={onStop} variant="overlay" />);
    await user.tab();
    expect(screen.getByRole('button', { name: 'Stop recording' })).toHaveFocus();
    await user.keyboard('{Enter}');
    expect(onStop).toHaveBeenCalled();
    await expectNoA11yViolations();
  });

  it('translates', () => {
    const { rerender } = render(
      <RecordingIndicator
        recording={false}
        messages={{ started: 'Enregistrement lancé', rec: 'ENR' }}
      />,
    );
    rerender(
      <RecordingIndicator recording messages={{ started: 'Enregistrement lancé', rec: 'ENR' }} />,
    );
    expect(screen.getByRole('status')).toHaveTextContent('Enregistrement lancé');
    expect(screen.getByText('ENR')).toBeInTheDocument();
  });

  it('renders on the server with 00:00', () => {
    const html = renderToString(<RecordingIndicator recording since={0} />);
    expect(html).toContain('mn-recording-indicator');
    expect(html).toContain('00:00');
  });
});
