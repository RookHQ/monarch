import { describe, expect, it, vi } from 'vitest';
import { forwardRef, createRef, type AnchorHTMLAttributes } from 'react';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { EventChip } from './index.js';

describe('EventChip', () => {
  it('is a button named by title, time and status', async () => {
    render(<EventChip title="Design review" time="09:30" status="tentative" color="#0096d4" />);
    const chip = screen.getByRole('button', { name: 'Design review, 09:30, Tentative' });
    expect(chip).toHaveClass('mn-event-chip');
    expect(chip).toHaveAttribute('type', 'button');
    expect(chip).toHaveAttribute('data-variant', 'dot');
    expect(chip).toHaveAttribute('data-status', 'tentative');
    expect(chip.style.getPropertyValue('--mn-event-color')).toBe('#0096d4');
    await expectNoA11yViolations();
  });

  it('leaves confirmed out of the name and uses timeLabel when the time is abbreviated', () => {
    render(<EventChip title="Stand-up" time="09:00" timeLabel="9:00 to 9:15" />);
    expect(screen.getByRole('button', { name: 'Stand-up, 9:00 to 9:15' })).toHaveAttribute(
      'data-status',
      'confirmed',
    );
  });

  it('shows the status and detail as text in bar and block variants', () => {
    render(
      <>
        <EventChip
          variant="bar"
          title="Quarterly planning"
          time="10:00 – 11:00"
          status="cancelled"
          detail="Room 4"
        />
        <EventChip
          variant="block"
          density="compact"
          title="1:1"
          time="14:00"
          statusLabel="Abgesagt"
          status="declined"
          past
        />
      </>,
    );
    expect(
      screen.getByRole('button', { name: 'Quarterly planning, 10:00 – 11:00, Cancelled, Room 4' }),
    ).toHaveAttribute('data-variant', 'bar');
    const block = screen.getByRole('button', { name: '1:1, 14:00, Abgesagt' });
    expect(block).toHaveAttribute('data-density', 'compact');
    expect(block).toHaveAttribute('data-past');
  });

  it('activates with click, Enter and Space and forwards its ref', async () => {
    const onClick = vi.fn();
    const ref = createRef<HTMLButtonElement>();
    render(<EventChip ref={ref} title="Retro" time="16:00" onClick={onClick} />);
    const user = userEvent.setup();
    await user.click(screen.getByRole('button'));
    await user.tab();
    await user.tab({ shift: true });
    expect(ref.current).toHaveFocus();
    await user.keyboard('{Enter}');
    await user.keyboard(' ');
    expect(onClick).toHaveBeenCalledTimes(3);
  });

  it('renders a link with href, and a router link with asChild', async () => {
    const RouterLink = forwardRef<HTMLAnchorElement, AnchorHTMLAttributes<HTMLAnchorElement>>(
      function RouterLink(props, ref) {
        // Content comes from EventChip through asChild.
        // eslint-disable-next-line jsx-a11y/anchor-has-content
        return <a ref={ref} data-router="" {...props} />;
      },
    );
    render(
      <>
        <EventChip href="/events/1" title="Launch" time="All day" />
        <EventChip asChild title="Offsite" time="08:00">
          <RouterLink href="/events/2" />
        </EventChip>
      </>,
    );
    expect(screen.getByRole('link', { name: 'Launch, All day' })).toHaveAttribute(
      'href',
      '/events/1',
    );
    const routed = screen.getByRole('link', { name: 'Offsite, 08:00' });
    expect(routed).toHaveAttribute('data-router');
    expect(routed).toHaveClass('mn-event-chip');
    expect(screen.queryByRole('button')).toBeNull();
    await expectNoA11yViolations();
  });

  it('takes its name from the content when the title is rich', () => {
    render(
      <EventChip
        title={
          <>
            <strong>Board</strong> meeting
          </>
        }
        time="15:00"
      />,
    );
    const chip = screen.getByRole('button');
    expect(chip).not.toHaveAttribute('aria-label');
    expect(chip.textContent).toContain('Board meeting');
    expect(chip.textContent).toContain('15:00');
  });

  it('renders on the server', () => {
    const html = renderToString(<EventChip title="Review" time="09:00" />);
    expect(html).toContain('mn-event-chip');
    expect(html).toContain('Review');
  });
});
