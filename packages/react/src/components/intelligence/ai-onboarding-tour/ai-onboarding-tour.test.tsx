import { describe, expect, it, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { AiOnboardingTour, type AiOnboardingTourStep } from './index.js';

const STEPS: AiOnboardingTourStep[] = [
  { title: 'What it can see', body: 'The page you are on and files you attach.' },
  { title: 'What it can do', body: 'Summarise, draft and answer questions.', target: '#target' },
  { title: 'How to turn it off', body: 'Use the switch in settings at any time.' },
];

describe('AiOnboardingTour', () => {
  it('renders the first step as a labelled region with a counter', async () => {
    render(
      <AiOnboardingTour
        steps={STEPS}
        onFinish={() => {}}
        onDisable={() => {}}
        data-testid="root"
      />,
    );
    const root = screen.getByTestId('root');
    expect(root).toHaveClass('mn-ai-onboarding-tour');
    expect(screen.getByRole('region', { name: 'What it can see' })).toHaveAccessibleDescription(
      'The page you are on and files you attach.',
    );
    expect(screen.getByText('Step 1 of 3')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Back' })).not.toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Turn off AI' })).toBeInTheDocument();
    await expectNoA11yViolations();
  });

  it('moves with Back and Next, focusing the new heading, and finishes on the last step', async () => {
    const user = userEvent.setup();
    const onFinish = vi.fn();
    const onStepChange = vi.fn();
    render(
      <AiOnboardingTour
        steps={STEPS}
        onFinish={onFinish}
        onDisable={() => {}}
        onStepChange={onStepChange}
      />,
    );
    await user.click(screen.getByRole('button', { name: 'Next' }));
    expect(screen.getByRole('heading', { name: 'What it can do' })).toHaveFocus();
    expect(onStepChange).toHaveBeenLastCalledWith(1);
    await user.click(screen.getByRole('button', { name: 'Back' }));
    expect(screen.getByRole('heading', { name: 'What it can see' })).toHaveFocus();
    await user.click(screen.getByRole('button', { name: 'Next' }));
    await user.click(screen.getByRole('button', { name: 'Next' }));
    expect(screen.getByText('Step 3 of 3')).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Got it' }));
    expect(onFinish).toHaveBeenCalledTimes(1);
  });

  it('offers Turn off AI on every step', async () => {
    const user = userEvent.setup();
    const onDisable = vi.fn();
    render(
      <AiOnboardingTour steps={STEPS} defaultStep={2} onFinish={() => {}} onDisable={onDisable} />,
    );
    await user.click(screen.getByRole('button', { name: 'Turn off AI' }));
    expect(onDisable).toHaveBeenCalledTimes(1);
  });

  it('outlines and releases the step target', async () => {
    const user = userEvent.setup();
    render(
      <>
        <button id="target" type="button">
          Ask
        </button>
        <AiOnboardingTour steps={STEPS} onFinish={() => {}} onDisable={() => {}} />
      </>,
    );
    const target = screen.getByRole('button', { name: 'Ask' });
    expect(target).not.toHaveAttribute('data-mn-ai-tour-target');
    await user.click(screen.getByRole('button', { name: 'Next' }));
    expect(target).toHaveAttribute('data-mn-ai-tour-target');
    await user.click(screen.getByRole('button', { name: 'Next' }));
    expect(target).not.toHaveAttribute('data-mn-ai-tour-target');
  });

  it('floating is a non-modal dialog; Escape and the close button skip', async () => {
    const user = userEvent.setup();
    const onSkip = vi.fn();
    render(
      <AiOnboardingTour
        steps={STEPS}
        placement="floating"
        onFinish={() => {}}
        onDisable={() => {}}
        onSkip={onSkip}
      />,
    );
    const dialog = screen.getByRole('dialog', { name: 'What it can see' });
    expect(dialog).not.toHaveAttribute('aria-modal');
    await user.click(screen.getByRole('button', { name: 'Skip introduction' }));
    expect(onSkip).toHaveBeenCalledTimes(1);
    screen.getByRole('button', { name: 'Next' }).focus();
    await user.keyboard('{Escape}');
    expect(onSkip).toHaveBeenCalledTimes(2);
    await expectNoA11yViolations();
  });

  it('is controlled by step and takes strings', () => {
    render(
      <AiOnboardingTour
        steps={STEPS}
        step={1}
        onFinish={() => {}}
        onDisable={() => {}}
        messages={{ stepOf: (s, t) => `${s}/${t}`, disable: 'Disable assistant', next: 'Continue' }}
      />,
    );
    expect(screen.getByText('2/3')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Disable assistant' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Continue' })).toBeInTheDocument();
  });

  it('renders on the server', () => {
    const html = renderToString(
      <AiOnboardingTour steps={STEPS} onFinish={() => {}} onDisable={() => {}} />,
    );
    expect(html).toContain('mn-ai-onboarding-tour');
    expect(html).toContain('What it can see');
  });
});
