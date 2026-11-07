import { describe, expect, it, vi } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { Button } from '../../actions/button/index.js';
import type { CalendarEvent } from '../event-chip/index.js';
import { EventPopover, type EventPopoverProps } from './index.js';

const EVENT: CalendarEvent = {
  id: 'e1',
  title: 'Design review',
  start: '2026-11-04T09:30:00Z',
  end: '2026-11-04T10:15:00Z',
  color: '#3366cc',
  status: 'tentative',
  detail: 'Product team',
};

function Peek(props: Partial<EventPopoverProps>) {
  return (
    <EventPopover
      event={EVENT}
      timeZone="UTC"
      locale="en-GB"
      hourCycle="24"
      details={[{ label: 'Reference', value: 'AB12CD' }]}
      {...props}
    >
      <button type="button">Design review</button>
    </EventPopover>
  );
}

describe('EventPopover', () => {
  it('opens from its trigger with the event details and no axe violations', async () => {
    render(<Peek />);
    const user = userEvent.setup();
    const trigger = screen.getByRole('button', { name: 'Design review' });
    await user.click(trigger);
    const dialog = await screen.findByRole('dialog', { name: 'Design review' });
    expect(dialog).toHaveTextContent('Product team');
    expect(dialog).toHaveTextContent('Tentative');
    expect(dialog).toHaveTextContent('Wed, 4 Nov 2026');
    expect(dialog).toHaveTextContent('09:30 – 10:15 UTC');
    expect(dialog).toHaveTextContent('ReferenceAB12CD');
    expect(dialog).toHaveAttribute('data-status', 'tentative');
    await expectNoA11yViolations(dialog);
    await user.keyboard('{Escape}');
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
    expect(trigger).toHaveFocus();
  });

  it('lets actions close the popover first', async () => {
    const onOpen = vi.fn();
    render(
      <Peek
        actions={(close) => (
          <Button
            size="sm"
            onClick={() => {
              close();
              onOpen();
            }}
          >
            Open
          </Button>
        )}
      />,
    );
    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: 'Design review' }));
    await user.click(await screen.findByRole('button', { name: 'Open' }));
    expect(onOpen).toHaveBeenCalled();
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
  });

  it('shows all-day ranges, overrides, icon rows, notes and custom statuses', async () => {
    render(
      <Peek
        defaultOpen
        event={undefined}
        title="Offsite"
        start="2026-11-04"
        end="2026-11-06"
        allDay
        status={<span>Awaiting approval</span>}
        details={[{ label: 'Hosts', value: 'Sam Lee', icon: <svg /> }]}
        note="Reason: venue change."
      />,
    );
    const dialog = await screen.findByRole('dialog', { name: 'Offsite' });
    expect(dialog).toHaveTextContent('Wed, 4 Nov 2026 – Fri, 6 Nov 2026');
    expect(dialog).toHaveTextContent('All day');
    expect(dialog).toHaveTextContent('Awaiting approval');
    expect(dialog).toHaveTextContent('Reason: venue change.');
    expect(screen.getByText('Hosts')).toHaveClass('mn-visually-hidden');
    await expectNoA11yViolations(dialog);
  });

  it('formats times in the given zone', async () => {
    render(<Peek defaultOpen timeZone="Asia/Kolkata" />);
    expect(await screen.findByRole('dialog')).toHaveTextContent('15:00 – 15:45');
  });

  it('renders the trigger on the server', () => {
    expect(renderToString(<Peek />)).toContain('Design review');
  });
});
