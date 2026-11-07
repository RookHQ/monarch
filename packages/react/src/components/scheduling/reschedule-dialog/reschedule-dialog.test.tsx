import { describe, expect, it, vi } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { Button } from '../../actions/button/index.js';
import { RescheduleDialog, type RescheduleDialogProps } from './index.js';
import { instantToWallTime, wallTimeToInstant } from './reschedule-dialog.zone.js';

function deferred() {
  let resolve!: () => void;
  let reject!: (error: unknown) => void;
  const promise = new Promise<void>((res, rej) => {
    resolve = res;
    reject = rej;
  });
  return { promise, resolve, reject };
}

const SLOTS = [{ start: '2026-11-05T09:00:00Z' }, { start: '2026-11-05T10:00:00Z' }];

function Move(props: Partial<RescheduleDialogProps>) {
  return (
    <RescheduleDialog
      trigger={<Button>Reschedule</Button>}
      subject="Consultation"
      currentStart="2026-11-04T09:30:00Z"
      timeZone="UTC"
      locale="en-GB"
      hourCycle="24"
      slotPicker={{ slots: SLOTS, defaultDay: '2026-11-05', today: '2026-11-02' }}
      onConfirm={() => {}}
      {...props}
    />
  );
}

describe('RescheduleDialog', () => {
  it('opens from the trigger, names the current time, and has no axe violations', async () => {
    render(<Move />);
    const user = userEvent.setup();
    const trigger = screen.getByRole('button', { name: 'Reschedule' });
    await user.click(trigger);
    const dialog = await screen.findByRole('dialog', { name: 'Reschedule' });
    expect(dialog).toHaveAccessibleDescription(
      /Consultation · Currently Wed, 4 Nov 2026, 09:30 UTC/,
    );
    expect(
      screen.getByRole('group', { name: /Available times on Thursday 5 November/ }),
    ).toBeInTheDocument();
    await expectNoA11yViolations(dialog);
    await user.keyboard('{Escape}');
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
    expect(trigger).toHaveFocus();
  });

  it('asks for a time before confirming, then sends the slot and reason', async () => {
    const onConfirm = vi.fn();
    render(<Move defaultOpen onConfirm={onConfirm} />);
    const user = userEvent.setup();
    const dialog = await screen.findByRole('dialog');
    const confirm = () =>
      [...dialog.querySelectorAll('button')].find((b) => b.textContent === 'Reschedule')!;
    await user.click(confirm());
    expect(screen.getByText('Choose a new time first.')).toBeInTheDocument();
    expect(onConfirm).not.toHaveBeenCalled();
    await user.click(screen.getByRole('button', { name: '10:00' }));
    await user.type(screen.getByRole('textbox', { name: /Reason/ }), ' Clash  ');
    await user.click(confirm());
    expect(onConfirm).toHaveBeenCalledWith({
      start: '2026-11-05T10:00:00Z',
      outsideHours: false,
      reason: 'Clash',
    });
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
  });

  it('stays open and pending while the promise runs, and shows a rejection', async () => {
    const d = deferred();
    render(<Move defaultOpen onConfirm={() => d.promise} />);
    const user = userEvent.setup();
    const dialog = await screen.findByRole('dialog');
    await user.click(screen.getByRole('button', { name: '09:00' }));
    const confirm = [...dialog.querySelectorAll('button')].find(
      (b) => b.textContent === 'Reschedule',
    )!;
    await user.click(confirm);
    expect(dialog).toHaveAttribute('aria-busy', 'true');
    await user.keyboard('{Escape}');
    expect(screen.getByRole('dialog')).toBeInTheDocument();
    d.reject(new Error('That time was just taken.'));
    expect(await screen.findByText('That time was just taken.')).toBeInTheDocument();
    expect(screen.getByRole('dialog')).toBeInTheDocument();
  });

  it('switches to a typed time outside working hours and converts it from the zone', async () => {
    const onConfirm = vi.fn();
    render(<Move defaultOpen allowOutsideHours timeZone="Asia/Kolkata" onConfirm={onConfirm} />);
    const user = userEvent.setup();
    const dialog = await screen.findByRole('dialog');
    await user.click(screen.getByRole('switch', { name: /outside working hours/ }));
    expect(screen.queryByRole('group', { name: /Available times/ })).not.toBeInTheDocument();
    const confirm = [...dialog.querySelectorAll('button')].find(
      (b) => b.textContent === 'Reschedule',
    )!;
    await user.click(confirm);
    // Defaults to the current start: 09:30 UTC is 15:00 in Kolkata.
    expect(onConfirm).toHaveBeenCalledWith({
      start: '2026-11-04T09:30:00.000Z',
      outsideHours: true,
    });
  });

  it('converts wall times both ways across zones', () => {
    expect(wallTimeToInstant('2026-11-04T15:00', 'Asia/Kolkata')).toBe('2026-11-04T09:30:00.000Z');
    expect(wallTimeToInstant('2026-07-01T09:00', 'Europe/London')).toBe('2026-07-01T08:00:00.000Z');
    expect(instantToWallTime('2026-11-04T09:30:00Z', 'America/New_York')).toBe('2026-11-04T04:30');
  });

  it('renders on the server', () => {
    expect(renderToString(<Move />)).toContain('Reschedule');
  });
});
