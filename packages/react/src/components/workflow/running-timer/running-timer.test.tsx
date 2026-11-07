import { afterEach, describe, expect, it, vi } from 'vitest';
import { act, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { RunningTimer, formatElapsed } from './index.js';

const NOW = new Date(2026, 0, 12, 10, 0, 0).getTime();

afterEach(() => {
  vi.useRealTimers();
});

describe('formatElapsed', () => {
  it('formats seconds as a stopwatch', () => {
    expect(formatElapsed(0)).toBe('0:00:00');
    expect(formatElapsed(249)).toBe('0:04:09');
    expect(formatElapsed(45_000)).toBe('12:30:00');
    expect(formatElapsed(-5)).toBe('0:00:00');
  });
});

describe('RunningTimer panel', () => {
  it('shows a stopped clock with Start, and calls onStart', async () => {
    const onStart = vi.fn();
    render(<RunningTimer data-testid="t" onStart={onStart} now={NOW} />);
    const root = screen.getByTestId('t');
    expect(root).toHaveClass('mn-running-timer');
    expect(root).toHaveAttribute('data-variant', 'panel');
    expect(root).not.toHaveAttribute('data-running');
    expect(screen.getByRole('timer')).toHaveTextContent('0:00:00');
    expect(root).toHaveTextContent('Timer stopped');
    await expectNoA11yViolations();
    await userEvent.click(screen.getByRole('button', { name: 'Start timer' }));
    expect(onStart).toHaveBeenCalledOnce();
  });

  it('disables Start until allowed', () => {
    render(<RunningTimer onStart={() => {}} startDisabled now={NOW} />);
    expect(screen.getByRole('button', { name: 'Start timer' })).toBeDisabled();
  });

  it('shows elapsed time with Stop, Stopped earlier and Discard', async () => {
    const onStop = vi.fn();
    render(
      <RunningTimer
        data-testid="t"
        startedAt={NOW - 3_725_000}
        now={NOW}
        label="Website refresh"
        onStop={onStop}
        onStopAt={() => {}}
        onDiscard={() => {}}
      >
        <input aria-label="Note" />
      </RunningTimer>,
    );
    expect(screen.getByTestId('t')).toHaveAttribute('data-running');
    expect(screen.getByRole('timer')).toHaveTextContent('1:02:05');
    expect(screen.getByText('Website refresh')).toBeInTheDocument();
    expect(screen.getByRole('textbox', { name: 'Note' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'I stopped earlier' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Discard without logging' })).toBeInTheDocument();
    await expectNoA11yViolations();
    await userEvent.click(screen.getByRole('button', { name: 'Stop' }));
    expect(onStop).toHaveBeenCalledOnce();
  });

  it('shows busy state while a promise runs and an alert when it fails', async () => {
    let reject!: (e: Error) => void;
    const onStop = vi.fn(() => new Promise<void>((_, r) => (reject = r)));
    render(<RunningTimer startedAt={NOW - 60_000} now={NOW} onStop={onStop} />);
    const stop = screen.getByRole('button', { name: 'Stop' });
    await userEvent.click(stop);
    expect(stop).toHaveAttribute('aria-busy', 'true');
    await act(async () => reject(new Error('Network down')));
    expect(await screen.findByRole('alert')).toHaveTextContent('Network down');
    expect(stop).not.toHaveAttribute('aria-busy');
  });

  it('confirms before discarding', async () => {
    const onDiscard = vi.fn();
    render(<RunningTimer startedAt={NOW - 60_000} now={NOW} onDiscard={onDiscard} />);
    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: 'Discard without logging' }));
    const dialog = await screen.findByRole('alertdialog', { name: 'Discard the running timer?' });
    expect(onDiscard).not.toHaveBeenCalled();
    await user.click(screen.getByRole('button', { name: 'Discard' }));
    expect(onDiscard).toHaveBeenCalledOnce();
    await waitFor(() => expect(dialog).not.toBeInTheDocument());
  });

  it('asks when the timer stopped and validates the time', async () => {
    const onStopAt = vi.fn();
    const start = NOW - 3 * 3_600_000;
    render(<RunningTimer startedAt={start} now={NOW} onStopAt={onStopAt} />);
    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: 'I stopped earlier' }));
    const dialog = await screen.findByRole('dialog', { name: 'When did you stop?' });
    const input = screen.getByLabelText('I stopped at') as HTMLInputElement;
    // Defaults to an hour after the start.
    expect(dialog).toHaveTextContent('That is 1h 00m of work.');
    await expectNoA11yViolations(dialog);
    await user.clear(input);
    expect(screen.getByText('Choose the time you stopped.')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Save this time' })).toBeDisabled();
    const pad = (n: number) => String(n).padStart(2, '0');
    const local = (ms: number) => {
      const d = new Date(ms);
      return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`;
    };
    await user.type(input, local(start));
    expect(
      screen.getByText('It must be at least a minute after the timer started.'),
    ).toBeInTheDocument();
    await user.clear(input);
    await user.type(input, local(start + 90 * 60_000));
    expect(dialog).toHaveTextContent('That is 1h 30m of work.');
    await user.click(screen.getByRole('button', { name: 'Save this time' }));
    expect(onStopAt).toHaveBeenCalledWith(new Date(start + 90 * 60_000));
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
  });

  it('warns when the timer has run too long', () => {
    render(<RunningTimer startedAt={NOW - 13 * 3_600_000} now={NOW} />);
    expect(screen.getByText(/running for over 12h/)).toBeInTheDocument();
  });

  it('announces start and stop politely, but not the ticking clock', async () => {
    const { rerender } = render(<RunningTimer now={NOW} />);
    const live = () => document.querySelector('[aria-live="polite"]')!;
    expect(live()).toBeEmptyDOMElement();
    rerender(<RunningTimer now={NOW} startedAt={NOW} />);
    expect(live()).toHaveTextContent('Timer running');
    rerender(<RunningTimer now={NOW} />);
    expect(live()).toHaveTextContent('Timer stopped');
    // role="timer" is implicitly aria-live="off".
    expect(screen.getByRole('timer')).not.toHaveAttribute('aria-live');
  });

  it('ticks once a second from the clock', async () => {
    vi.useFakeTimers({ now: NOW });
    render(<RunningTimer startedAt={NOW - 5000} />);
    expect(screen.getByRole('timer')).toHaveTextContent('0:00:05');
    await act(async () => {
      vi.advanceTimersByTime(3000);
    });
    expect(screen.getByRole('timer')).toHaveTextContent('0:00:08');
  });
});

describe('RunningTimer indicator', () => {
  it('renders nothing visible while stopped', () => {
    render(<RunningTimer data-testid="t" variant="indicator" now={NOW} />);
    expect(screen.getByTestId('t')).toHaveAttribute('data-empty');
    expect(screen.queryByRole('button')).toBeNull();
  });

  it('opens, stops and offers a menu', async () => {
    const onOpen = vi.fn();
    const onStop = vi.fn();
    render(
      <RunningTimer
        variant="indicator"
        startedAt={NOW - 125_000}
        now={NOW}
        label="DOC-14"
        onOpen={onOpen}
        onStop={onStop}
        onDiscard={() => {}}
        onStopAt={() => {}}
      />,
    );
    const user = userEvent.setup();
    const open = screen.getByRole('button', {
      name: 'Timer running on DOC-14, 0:02:05 elapsed. Open',
    });
    await expectNoA11yViolations();
    await user.click(open);
    expect(onOpen).toHaveBeenCalledOnce();
    await user.click(screen.getByRole('button', { name: 'Stop timer and log time' }));
    expect(onStop).toHaveBeenCalledOnce();
    await user.click(screen.getByRole('button', { name: 'More timer actions' }));
    expect(await screen.findByRole('menuitem', { name: 'I stopped earlier' })).toBeInTheDocument();
    await user.keyboard('{ArrowDown}');
    expect(screen.getByRole('menuitem', { name: 'I stopped earlier' })).toHaveFocus();
    await user.keyboard('{ArrowDown}');
    expect(screen.getByRole('menuitem', { name: 'Discard without logging' })).toHaveFocus();
    await user.keyboard('{Enter}');
    expect(await screen.findByRole('alertdialog')).toBeInTheDocument();
  });

  it('shows a plain chip without onOpen, and when stopped with showWhenStopped', () => {
    const { rerender } = render(
      <RunningTimer data-testid="t" variant="indicator" startedAt={NOW - 1000} now={NOW} />,
    );
    expect(screen.getByTestId('t')).toHaveTextContent('Timer0:00:01 (Timer running)');
    rerender(<RunningTimer data-testid="t" variant="indicator" showWhenStopped now={NOW} />);
    expect(screen.getByTestId('t')).toHaveTextContent('Timer0:00:00 (Timer stopped)');
  });

  it('accepts translated messages', () => {
    render(
      <RunningTimer
        onStart={() => {}}
        now={NOW}
        messages={{ start: 'Starten', stopped: 'Gestoppt' }}
      />,
    );
    expect(screen.getByRole('button', { name: 'Starten' })).toBeInTheDocument();
    expect(screen.getByText('Gestoppt')).toBeInTheDocument();
  });
});

describe('RunningTimer on the server', () => {
  it('renders both variants with an injected clock', () => {
    const html = renderToString(
      <>
        <RunningTimer startedAt={NOW - 61_000} now={NOW} onStop={() => {}} />
        <RunningTimer variant="indicator" startedAt={NOW - 61_000} now={NOW} />
      </>,
    );
    expect(html).toContain('mn-running-timer');
    expect(html).toContain('0:01:01');
  });
});
