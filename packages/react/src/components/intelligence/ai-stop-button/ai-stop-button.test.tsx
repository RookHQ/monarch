import { useState } from 'react';
import { describe, expect, it, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { AiStopButton, type AiStopButtonProps } from './index.js';

function Harness({ noContinue, ...props }: Partial<AiStopButtonProps> & { noContinue?: boolean }) {
  const [stopped, setStopped] = useState(false);
  return (
    <AiStopButton
      stopped={stopped}
      onStop={() => setStopped(true)}
      {...(noContinue ? {} : { onContinue: () => setStopped(false) })}
      {...props}
    />
  );
}

describe('AiStopButton', () => {
  it('renders a Stop button with its root class and forwards props', async () => {
    const onStop = vi.fn();
    render(<AiStopButton data-testid="root" onStop={onStop} />);
    const root = screen.getByTestId('root');
    expect(root).toHaveClass('mn-ai-stop-button');
    expect(root).toHaveAttribute('data-state', 'running');
    await userEvent.setup().click(screen.getByRole('button', { name: 'Stop' }));
    expect(onStop).toHaveBeenCalledTimes(1);
    await expectNoA11yViolations();
  });

  it('keeps the accessible name when the label is visually hidden', () => {
    render(<AiStopButton showLabel={false} messages={{ stop: 'Stop generating' }} />);
    const button = screen.getByRole('button', { name: 'Stop generating' });
    expect(button).toHaveAttribute('data-label', 'hidden');
    expect(button.querySelector('.mn-visually-hidden')).toHaveTextContent('Stop generating');
  });

  it('works from the keyboard and moves focus to Continue once stopped', async () => {
    const user = userEvent.setup();
    render(<Harness />);
    await user.tab();
    expect(screen.getByRole('button', { name: 'Stop' })).toHaveFocus();
    await user.keyboard('{Enter}');
    expect(screen.queryByRole('button', { name: 'Stop' })).not.toBeInTheDocument();
    expect(screen.getByRole('status')).toHaveTextContent('Stopped.');
    expect(screen.getByRole('button', { name: 'Continue' })).toHaveFocus();
    await user.keyboard(' ');
    expect(screen.getByRole('button', { name: 'Stop' })).toBeInTheDocument();
    await expectNoA11yViolations();
  });

  it('moves focus to the status text when there is no Continue action', async () => {
    const user = userEvent.setup();
    render(<Harness noContinue />);
    await user.click(screen.getByRole('button', { name: 'Stop' }));
    expect(screen.getByRole('status')).toHaveFocus();
    expect(screen.queryByRole('button')).not.toBeInTheDocument();
  });

  it('does not take focus when stopped from elsewhere (e.g. Escape in the prompt)', () => {
    const { rerender } = render(<AiStopButton onContinue={() => {}} />);
    rerender(<AiStopButton stopped onContinue={() => {}} />);
    expect(screen.getByRole('button', { name: 'Continue' })).not.toHaveFocus();
  });

  it('uses custom strings and passes props to the inner button', () => {
    render(
      <AiStopButton
        stopped
        onContinue={() => {}}
        messages={{ stopped: 'Halted.', continue: 'Resume' }}
      />,
    );
    expect(screen.getByRole('status')).toHaveTextContent('Halted.');
    expect(screen.getByRole('button', { name: 'Resume' })).toBeInTheDocument();
    render(<AiStopButton buttonProps={{ 'aria-keyshortcuts': 'Escape' }} />);
    expect(screen.getByRole('button', { name: 'Stop' })).toHaveAttribute(
      'aria-keyshortcuts',
      'Escape',
    );
  });

  it('renders on the server', () => {
    const html = renderToString(<AiStopButton />);
    expect(html).toContain('mn-ai-stop-button');
    expect(html).toContain('Stop');
    expect(renderToString(<AiStopButton stopped />)).toContain('Stopped.');
  });
});
