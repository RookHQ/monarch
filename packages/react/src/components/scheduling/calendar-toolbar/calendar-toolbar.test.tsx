import { describe, expect, it, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { CalendarToolbar, type CalendarToolbarProps } from './index.js';

function Bar(props: Partial<CalendarToolbarProps>) {
  return (
    <CalendarToolbar
      title="November 2026"
      view="month"
      onToday={() => {}}
      onPrevious={() => {}}
      onNext={() => {}}
      {...props}
    />
  );
}

describe('CalendarToolbar', () => {
  it('names the controls after the view and has no axe violations', async () => {
    render(<Bar onViewChange={() => {}} actions={<button type="button">New event</button>} />);
    expect(screen.getByRole('heading', { level: 2, name: 'November 2026' })).toHaveAttribute(
      'aria-live',
      'polite',
    );
    expect(screen.getByRole('button', { name: 'Previous month' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Next month' })).toBeInTheDocument();
    expect(screen.getByRole('radiogroup', { name: 'Calendar view' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'New event' })).toBeInTheDocument();
    await expectNoA11yViolations();
  });

  it('calls back for today, previous and next', async () => {
    const onToday = vi.fn();
    const onPrevious = vi.fn();
    const onNext = vi.fn();
    render(<Bar view="week" onToday={onToday} onPrevious={onPrevious} onNext={onNext} />);
    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: 'Today' }));
    await user.click(screen.getByRole('button', { name: 'Previous week' }));
    await user.click(screen.getByRole('button', { name: 'Next week' }));
    expect(onToday).toHaveBeenCalledOnce();
    expect(onPrevious).toHaveBeenCalledOnce();
    expect(onNext).toHaveBeenCalledOnce();
  });

  it('switches views with the keyboard', async () => {
    const onViewChange = vi.fn();
    render(<Bar view="week" views={['day', 'week', 'month']} onViewChange={onViewChange} />);
    const user = userEvent.setup();
    screen.getByRole('radio', { name: 'Week' }).focus();
    await user.keyboard('{ArrowRight}');
    expect(onViewChange).toHaveBeenCalledWith('month');
    expect(screen.queryByRole('radio', { name: 'Agenda' })).not.toBeInTheDocument();
  });

  it('hides the switcher without onViewChange and supports custom views and labels', () => {
    const { rerender } = render(<Bar />);
    expect(screen.queryByRole('radiogroup')).not.toBeInTheDocument();
    rerender(
      <Bar
        view="schedule"
        views={['week', 'schedule']}
        onViewChange={() => {}}
        titleAs="h1"
        messages={{ views: { schedule: 'Rota' }, units: { schedule: 'fortnight' }, today: 'Now' }}
      />,
    );
    expect(screen.getByRole('radio', { name: 'Rota' })).toBeChecked();
    expect(screen.getByRole('button', { name: 'Next fortnight' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Now' })).toBeInTheDocument();
    expect(screen.getByRole('heading', { level: 1 })).toBeInTheDocument();
  });

  it('shows busy and disabled states', () => {
    render(
      <Bar busy todayDisabled previousDisabled datePicker={<button type="button">Pick</button>} />,
    );
    expect(screen.getByRole('status', { name: 'Loading' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Today' })).toBeDisabled();
    expect(screen.getByRole('button', { name: 'Previous month' })).toBeDisabled();
    expect(screen.getByRole('button', { name: 'Pick' })).toBeInTheDocument();
  });

  it('renders on the server', () => {
    expect(renderToString(<Bar />)).toContain('mn-calendar-toolbar');
  });
});
