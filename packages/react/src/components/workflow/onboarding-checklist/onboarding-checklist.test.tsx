import { describe, expect, it, vi } from 'vitest';
import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { useState } from 'react';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { OnboardingChecklist, type OnboardingStep } from './index.js';

const STEPS: OnboardingStep[] = [
  { id: 'profile', title: 'Complete your profile', done: true },
  {
    id: 'invite',
    title: 'Invite your team',
    description: 'Projects move faster with people in them.',
    done: false,
    action: <button type="button">Invite people</button>,
  },
  {
    id: 'connect',
    title: 'Connect a calendar',
    done: false,
    action: <a href="#connect">Connect</a>,
  },
];

describe('OnboardingChecklist', () => {
  it('shows steps, done state, progress and the current step, without axe violations', async () => {
    render(
      <OnboardingChecklist
        title="Get started"
        description="Three steps to set up."
        steps={STEPS}
      />,
    );
    const region = screen.getByRole('region', { name: 'Get started' });
    expect(region).toHaveClass('mn-onboarding-checklist');
    expect(screen.getByRole('heading', { level: 2, name: 'Get started' })).toBeInTheDocument();
    const bar = screen.getByRole('progressbar', { name: 'Setup progress' });
    expect(bar).toHaveAttribute('aria-valuetext', '1 of 3 done');
    const items = within(screen.getByRole('list')).getAllByRole('listitem');
    expect(items[0]).toHaveTextContent('Complete your profile, Done');
    expect(items[0]).toHaveAttribute('data-done');
    expect(items[1]).toHaveAttribute('aria-current', 'step');
    expect(items[1]).toHaveTextContent('Invite your team, Not done');
    expect(screen.getByText('Projects move faster with people in them.')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Invite people' })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Connect' })).toBeInTheDocument();
    await expectNoA11yViolations();
  });

  it('hides the action and description of done steps', () => {
    render(<OnboardingChecklist title="Get started" steps={[{ ...STEPS[1]!, done: true }]} />);
    expect(screen.queryByRole('button', { name: 'Invite people' })).toBeNull();
    expect(screen.queryByText('Projects move faster with people in them.')).toBeNull();
  });

  it('collapses uncontrolled from the toggle and the keyboard', async () => {
    const user = userEvent.setup();
    const onCollapsedChange = vi.fn();
    render(
      <OnboardingChecklist
        title="Get started"
        steps={STEPS}
        onCollapsedChange={onCollapsedChange}
      />,
    );
    const toggle = screen.getByRole('button', { name: 'Get started', expanded: true });
    await user.click(toggle);
    expect(toggle).toHaveAttribute('aria-expanded', 'false');
    expect(onCollapsedChange).toHaveBeenLastCalledWith(true);
    expect(screen.queryByRole('list')).toBeNull();
    // Progress stays visible while collapsed.
    expect(screen.getByRole('progressbar')).toBeInTheDocument();
    toggle.focus();
    await user.keyboard('{Enter}');
    expect(toggle).toHaveAttribute('aria-expanded', 'true');
    expect(screen.getByRole('list')).toBeVisible();
  });

  it('works controlled, and without a toggle', async () => {
    const user = userEvent.setup();
    function Controlled() {
      const [collapsed, setCollapsed] = useState(true);
      return (
        <OnboardingChecklist
          title="Get started"
          steps={STEPS}
          collapsed={collapsed}
          onCollapsedChange={setCollapsed}
        />
      );
    }
    const { unmount } = render(<Controlled />);
    expect(screen.queryByRole('list')).toBeNull();
    await user.click(screen.getByRole('button', { name: 'Get started' }));
    expect(screen.getByRole('list')).toBeInTheDocument();
    unmount();
    render(
      <OnboardingChecklist
        title="Get started"
        steps={STEPS}
        collapsible={false}
        defaultCollapsed
      />,
    );
    expect(screen.queryByRole('button', { name: 'Get started' })).toBeNull();
    expect(screen.getByRole('list')).toBeInTheDocument();
  });

  it('is dismissible when the product handles it', async () => {
    const user = userEvent.setup();
    const onDismiss = vi.fn();
    const { rerender } = render(<OnboardingChecklist title="Get started" steps={STEPS} />);
    expect(screen.queryByRole('button', { name: 'Dismiss checklist' })).toBeNull();
    rerender(<OnboardingChecklist title="Get started" steps={STEPS} onDismiss={onDismiss} />);
    await user.click(screen.getByRole('button', { name: 'Dismiss checklist' }));
    expect(onDismiss).toHaveBeenCalledTimes(1);
  });

  it('shows the complete state when every step is done', () => {
    const done = STEPS.map((s) => ({ ...s, done: true }));
    const { container, rerender } = render(
      <OnboardingChecklist title="Get started" steps={done} />,
    );
    expect(container.firstChild).toHaveAttribute('data-complete');
    expect(screen.getByText('All done. You are set up.')).toBeInTheDocument();
    expect(screen.getByRole('progressbar')).toHaveAttribute('data-tone', 'success');
    expect(document.querySelector('[aria-current]')).toBeNull();
    rerender(<OnboardingChecklist title="Get started" steps={done} completeContent="Nice work." />);
    expect(screen.getByText('Nice work.')).toBeInTheDocument();
  });

  it('handles an empty list, heading level and translated messages', () => {
    render(
      <OnboardingChecklist
        title="Premiers pas"
        steps={[]}
        headingLevel={3}
        messages={{ progress: (d, t) => `${d} sur ${t}`, progressLabel: 'Progression' }}
      />,
    );
    expect(screen.getByRole('heading', { level: 3, name: 'Premiers pas' })).toBeInTheDocument();
    expect(screen.getByRole('progressbar', { name: 'Progression' })).toHaveAttribute(
      'aria-valuetext',
      '0 sur 0',
    );
    expect(screen.queryByText('All done. You are set up.')).toBeNull();
  });

  it('renders on the server', () => {
    expect(renderToString(<OnboardingChecklist title="Get started" steps={STEPS} />)).toContain(
      'mn-onboarding-checklist',
    );
  });
});
