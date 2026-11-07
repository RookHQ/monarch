import { describe, expect, it, vi } from 'vitest';
import { useState } from 'react';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import {
  BookingFlow,
  useBookingFlow,
  type BookingFlowProps,
  type BookingFlowStep,
} from './index.js';

function deferred() {
  let resolve!: () => void;
  let reject!: (error: unknown) => void;
  const promise = new Promise<void>((res, rej) => {
    resolve = res;
    reject = rej;
  });
  return { promise, resolve, reject };
}

function ServiceCards({ onPick }: { onPick: (name: string) => void }) {
  const flow = useBookingFlow();
  return (
    <button
      type="button"
      onClick={() => {
        onPick('Consultation');
        flow.next();
      }}
    >
      Consultation
    </button>
  );
}

function Flow(props: Partial<BookingFlowProps>) {
  const [service, setService] = useState<string | null>(null);
  const [slot, setSlot] = useState<string | null>(null);
  const steps: BookingFlowStep[] = [
    {
      id: 'service',
      label: 'Service',
      title: 'Choose a service',
      content: <ServiceCards onPick={setService} />,
      summary: service,
      hideContinue: true,
    },
    {
      id: 'time',
      label: 'Date and time',
      title: 'Choose a date and time',
      slotPicker: {
        slots: [{ start: '2026-11-05T09:00:00Z' }],
        defaultDay: '2026-11-05',
        today: '2026-11-02',
        timeZone: 'UTC',
        locale: 'en-GB',
        hourCycle: '24',
        value: slot,
        onValueChange: setSlot,
      },
      summary: slot ? '5 Nov, 09:00' : null,
      complete: slot !== null,
    },
    { id: 'review', label: 'Review', title: 'Review your booking', content: <p>All set.</p> },
  ];
  return <BookingFlow steps={steps} confirmation={<h2>You are booked</h2>} {...props} />;
}

describe('BookingFlow', () => {
  it('shows the first step with progress and no axe violations', async () => {
    render(<Flow />);
    expect(screen.getByRole('navigation', { name: 'Booking steps' })).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Choose a service' })).toBeInTheDocument();
    expect(screen.getByText('Step 1 of 3')).toBeInTheDocument();
    expect(screen.getByText('Service', { selector: '[aria-current="step"]' })).toBeInTheDocument();
    await expectNoA11yViolations();
  });

  it('advances from content, moves focus to the heading and announces the step', async () => {
    render(<Flow />);
    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: 'Consultation' }));
    const heading = screen.getByRole('heading', { name: 'Choose a date and time' });
    expect(heading).toHaveFocus();
    expect(document.querySelector('.mn-booking-flow__status')).toHaveTextContent(
      'Step 2 of 3: Choose a date and time',
    );
    // Summary panel lists the completed step.
    expect(screen.getByRole('complementary', { name: 'Summary' })).toHaveTextContent(
      'ServiceConsultation',
    );
    // Continue is blocked until a time is chosen.
    expect(screen.getByRole('button', { name: 'Continue' })).toBeDisabled();
    await user.click(screen.getByRole('button', { name: '09:00' }));
    await user.click(screen.getByRole('button', { name: 'Continue' }));
    expect(screen.getByRole('heading', { name: 'Review your booking' })).toHaveFocus();
    await expectNoA11yViolations();
  });

  it('goes back and changes a completed step', async () => {
    const onStepChange = vi.fn();
    render(<Flow defaultStep="review" onStepChange={onStepChange} />);
    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: 'Back to Date and time' }));
    expect(onStepChange).toHaveBeenLastCalledWith('time');
    await user.click(screen.getByRole('button', { name: /Service, completed/ }));
    expect(onStepChange).toHaveBeenLastCalledWith('service');
    expect(screen.getByRole('heading', { name: 'Choose a service' })).toHaveFocus();
  });

  it('submits on the last step: pending, error, then confirmation', async () => {
    const d1 = deferred();
    const d2 = deferred();
    const onSubmit = vi.fn().mockReturnValueOnce(d1.promise).mockReturnValueOnce(d2.promise);
    render(<Flow defaultStep="review" onSubmit={onSubmit} />);
    const user = userEvent.setup();
    const submit = screen.getByRole('button', { name: 'Confirm booking' });
    await user.click(submit);
    expect(submit).toHaveAttribute('aria-busy', 'true');
    d1.reject(new Error('That time was just taken.'));
    expect(await screen.findByText('That time was just taken.')).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Confirm booking' }));
    d2.resolve();
    expect(await screen.findByRole('heading', { name: 'You are booked' })).toBeInTheDocument();
    await waitFor(() =>
      expect(document.querySelector('.mn-booking-flow__status')).toHaveTextContent(
        'Booking complete',
      ),
    );
  });

  it('lists every step in the list layout', async () => {
    render(<Flow layout="list" defaultStep="time" />);
    expect(screen.getByRole('heading', { name: 'Choose a date and time' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Change Service' })).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Review' })).toBeInTheDocument();
    await expectNoA11yViolations();
  });

  it('shows the unavailable content instead of the steps', () => {
    render(<Flow unavailable={<p>Nothing to book right now.</p>} />);
    expect(screen.getByText('Nothing to book right now.')).toBeInTheDocument();
    expect(screen.queryByRole('navigation')).not.toBeInTheDocument();
  });

  it('throws a clear error when useBookingFlow is used outside', () => {
    function Lost() {
      useBookingFlow();
      return null;
    }
    vi.spyOn(console, 'error').mockImplementation(() => {});
    expect(() => render(<Lost />)).toThrow('useBookingFlow must be used inside a BookingFlow.');
  });

  it('renders on the server', () => {
    const html = renderToString(<Flow />);
    expect(html).toContain('mn-booking-flow');
    expect(html).toContain('Choose a service');
  });
});
