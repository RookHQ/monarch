import { describe, expect, it, vi } from 'vitest';
import { act, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { WorkspaceOnboardingForm } from './index.js';

const available = vi.fn(async (u: string) =>
  u === 'takenname' ? { valid: false, message: 'That username is taken' } : { valid: true },
);

async function fill(user: ReturnType<typeof userEvent.setup>, name: string, username: string) {
  await user.type(screen.getByRole('textbox', { name: /Workspace name/ }), name);
  await user.type(screen.getByRole('textbox', { name: /Workspace username/ }), username);
}

describe('WorkspaceOnboardingForm', () => {
  it('renders labelled fields and a type choice', async () => {
    render(<WorkspaceOnboardingForm data-testid="form" onSubmit={() => undefined} />);
    expect(screen.getByTestId('form')).toHaveClass('mn-workspace-onboarding-form');
    expect(screen.getByRole('textbox', { name: /Workspace name/ })).toBeRequired();
    expect(screen.getByRole('textbox', { name: /Workspace username/ })).toHaveAccessibleDescription(
      /Letters and numbers only, 5 to 30 characters./,
    );
    expect(screen.getByRole('group', { name: 'Type' })).toBeInTheDocument();
    expect(screen.getByRole('radio', { name: 'Creator' })).toHaveAccessibleDescription(
      'For individual creators and personal projects',
    );
    await expectNoA11yViolations();
  });

  it('shows rule errors on submit and focuses the first invalid field', async () => {
    const user = userEvent.setup();
    const onSubmit = vi.fn();
    render(<WorkspaceOnboardingForm onSubmit={onSubmit} />);
    await user.click(screen.getByRole('button', { name: 'Continue' }));
    expect(onSubmit).not.toHaveBeenCalled();
    expect(screen.getByText('Name must be at least 4 characters')).toBeInTheDocument();
    expect(screen.getByText('Username must be at least 5 characters')).toBeInTheDocument();
    expect(screen.getByText('Choose a workspace type')).toBeInTheDocument();
    expect(screen.getByRole('textbox', { name: /Workspace name/ })).toHaveFocus();
    await user.type(screen.getByRole('textbox', { name: /Workspace username/ }), 'bad-name');
    expect(screen.getByText('Use letters and numbers only')).toBeInTheDocument();
    await expectNoA11yViolations();
  });

  it('checks availability and blocks a taken username', async () => {
    const user = userEvent.setup();
    const onSubmit = vi.fn();
    render(<WorkspaceOnboardingForm onSubmit={onSubmit} checkUsername={available} />);
    await fill(user, 'Acme Inc', 'takenname');
    await user.click(screen.getByRole('radio', { name: 'Business' }));
    expect(
      await screen.findAllByText('That username is taken', {}, { timeout: 2000 }),
    ).not.toHaveLength(0);
    await user.click(screen.getByRole('button', { name: 'Continue' }));
    expect(onSubmit).not.toHaveBeenCalled();
    expect(screen.getByRole('textbox', { name: /Workspace username/ })).toHaveFocus();
  });

  it('submits trimmed values, shows pending, then calls onSuccess', async () => {
    const user = userEvent.setup();
    let resolve!: (v: { id: string }) => void;
    const onSubmit = vi.fn(() => new Promise<{ id: string }>((r) => (resolve = r)));
    const onSuccess = vi.fn();
    render(
      <WorkspaceOnboardingForm
        onSubmit={onSubmit}
        onSuccess={onSuccess}
        checkUsername={available}
      />,
    );
    await fill(user, '  Acme Inc  ', 'acmeinc');
    await user.click(screen.getByRole('radio', { name: 'Creator' }));
    expect(
      await screen.findAllByText('Username is available', {}, { timeout: 2000 }),
    ).not.toHaveLength(0);
    await user.click(screen.getByRole('button', { name: 'Continue' }));
    expect(onSubmit).toHaveBeenCalledWith({
      name: 'Acme Inc',
      username: 'acmeinc',
      type: 'creator',
    });
    expect(screen.getByRole('button', { name: /Continue/ })).toHaveAttribute('aria-busy', 'true');
    await act(async () => resolve({ id: 'w1' }));
    expect(onSuccess).toHaveBeenCalledWith(
      { id: 'w1' },
      expect.objectContaining({ name: 'Acme Inc' }),
    );
  });

  it('shows the rejection message and keeps the values', async () => {
    const user = userEvent.setup();
    render(
      <WorkspaceOnboardingForm
        types={[]}
        onSubmit={() => Promise.reject(new Error('Workspace limit reached'))}
      />,
    );
    expect(screen.queryByRole('group')).toBeNull();
    await fill(user, 'Acme Inc', 'acmeinc');
    await user.keyboard('{Enter}');
    expect(await screen.findByRole('alert')).toHaveTextContent('Workspace limit reached');
    expect(screen.getByRole('textbox', { name: /Workspace name/ })).toHaveValue('Acme Inc');
  });

  it('takes custom types, rules, labels and a secondary action', async () => {
    const user = userEvent.setup();
    const onSubmit = vi.fn();
    render(
      <WorkspaceOnboardingForm
        types={[
          { value: 'team', label: 'Team' },
          { value: 'school', label: 'School' },
        ]}
        rules={{ nameMinLength: 2 }}
        submitLabel="Create workspace"
        secondaryAction={<button type="button">Cancel</button>}
        messages={{ nameLabel: 'Name' }}
        onSubmit={onSubmit}
      />,
    );
    await user.type(screen.getByRole('textbox', { name: /^Name/ }), 'Ab');
    await user.type(screen.getByRole('textbox', { name: /Workspace username/ }), 'abcde');
    // Arrow keys move between the radios.
    screen.getByRole('radio', { name: 'Team' }).focus();
    await user.keyboard('{ArrowRight}');
    expect(screen.getByRole('radio', { name: 'School' })).toBeChecked();
    await user.click(screen.getByRole('button', { name: 'Create workspace' }));
    await waitFor(() =>
      expect(onSubmit).toHaveBeenCalledWith({ name: 'Ab', username: 'abcde', type: 'school' }),
    );
    expect(screen.getByRole('button', { name: 'Cancel' })).toBeInTheDocument();
  });

  it('renders on the server', () => {
    const html = renderToString(<WorkspaceOnboardingForm onSubmit={() => undefined} />);
    expect(html).toContain('mn-workspace-onboarding-form');
    expect(html).toContain('Workspace username');
  });
});
