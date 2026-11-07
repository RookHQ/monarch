import { describe, expect, it, vi } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { Button } from '../../actions/button/index.js';
import { Celebration, CelebrationPanel } from './index.js';

describe('Celebration', () => {
  it('opens as a named dialog with focus on Continue; Continue closes it and returns focus', async () => {
    const onContinue = vi.fn();
    const onOpenChange = vi.fn();
    render(
      <Celebration
        trigger={<Button>Verify</Button>}
        title="Workspace verified"
        description="Your profile now shows the verified badge."
        onContinue={onContinue}
        onOpenChange={onOpenChange}
      />,
    );
    const user = userEvent.setup();
    const trigger = screen.getByRole('button', { name: 'Verify' });
    await user.click(trigger);
    const dialog = await screen.findByRole('dialog', { name: 'Workspace verified' });
    expect(dialog).toHaveAccessibleDescription('Your profile now shows the verified badge.');
    expect(screen.getByRole('button', { name: 'Continue' })).toHaveFocus();
    expect(dialog.querySelector('.mn-celebration__confetti')).toHaveAttribute(
      'aria-hidden',
      'true',
    );
    expect(dialog.querySelector('.mn-celebration__media')).toHaveAttribute('aria-hidden', 'true');
    // No close icon by default: Continue and Escape are the ways out.
    expect(screen.queryByRole('button', { name: 'Close' })).not.toBeInTheDocument();
    await expectNoA11yViolations(dialog);
    await user.keyboard('{Enter}');
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
    expect(onContinue).toHaveBeenCalledOnce();
    expect(onOpenChange).toHaveBeenLastCalledWith(false);
    expect(trigger).toHaveFocus();
  });

  it('Escape closes it without calling onContinue', async () => {
    const onContinue = vi.fn();
    render(<Celebration defaultOpen title="Done" onContinue={onContinue} />);
    const user = userEvent.setup();
    await screen.findByRole('dialog', { name: 'Done' });
    await user.keyboard('{Escape}');
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
    expect(onContinue).not.toHaveBeenCalled();
  });

  it('is controlled, takes extra actions, a custom label and no decoration', async () => {
    const { rerender } = render(<Celebration open={false} title="Published" />);
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
    rerender(
      <Celebration
        open
        title="Published"
        decoration="none"
        continueLabel="Back to the editor"
        actions={<Button>View page</Button>}
        media={<img src="data:image/gif;base64,R0lGODlhAQABAAAAACw=" alt="" />}
        tone="accent"
      />,
    );
    const dialog = await screen.findByRole('dialog', { name: 'Published' });
    expect(dialog).toHaveAttribute('data-decoration', 'none');
    expect(dialog.querySelector('.mn-celebration__confetti')).toBeNull();
    expect(dialog.querySelector('.mn-celebration__media')).toHaveAttribute('data-tone', 'accent');
    expect(screen.getByRole('button', { name: 'View page' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Back to the editor' })).toBeInTheDocument();
    await expectNoA11yViolations(dialog);
  });

  it('renders the same confetti on the server (no randomness)', () => {
    const a = renderToString(<CelebrationPanel title="Done" />);
    const b = renderToString(<CelebrationPanel title="Done" />);
    expect(a).toBe(b);
    expect(a).toContain('mn-celebration__piece');
  });
});

describe('CelebrationPanel', () => {
  it('announces politely with a heading, description and actions', async () => {
    render(
      <CelebrationPanel
        data-testid="root"
        title="Setup complete"
        description="Invite your team next."
        actions={<Button>Invite people</Button>}
      />,
    );
    const root = screen.getByTestId('root');
    expect(root).toHaveAttribute('role', 'status');
    expect(root).toHaveClass('mn-celebration');
    expect(screen.getByRole('heading', { level: 2, name: 'Setup complete' })).toBeInTheDocument();
    expect(root.querySelectorAll('.mn-celebration__piece')).toHaveLength(12);
    await expectNoA11yViolations();
  });

  it('can be quiet, use another heading level and forward its ref', () => {
    let node: HTMLDivElement | null = null;
    render(
      <CelebrationPanel
        ref={(el) => {
          node = el;
        }}
        data-testid="root"
        title="Setup complete"
        announce={false}
        headingLevel="h3"
      />,
    );
    expect(screen.getByTestId('root')).not.toHaveAttribute('role');
    expect(screen.getByRole('heading', { level: 3 })).toBeInTheDocument();
    expect(node).toBe(screen.getByTestId('root'));
  });
});
