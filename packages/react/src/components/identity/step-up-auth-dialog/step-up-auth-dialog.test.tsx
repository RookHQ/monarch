import { describe, expect, it, vi } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { Button } from '../../actions/button/index.js';
import {
  StepUpAuthDialog,
  useStepUpAuth,
  type StepUpAuthDialogProps,
  type StepUpCredential,
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

function StepUp(props: Partial<StepUpAuthDialogProps>) {
  return (
    <StepUpAuthDialog
      trigger={<Button variant="danger">Delete workspace</Button>}
      onVerify={() => {}}
      {...props}
    />
  );
}

describe('StepUpAuthDialog', () => {
  it('opens as a dialog with focus in the password field; Escape cancels and restores focus', async () => {
    const onCancel = vi.fn();
    render(<StepUp onCancel={onCancel} />);
    const user = userEvent.setup();
    const trigger = screen.getByRole('button', { name: 'Delete workspace' });
    await user.click(trigger);
    const dialog = await screen.findByRole('dialog', { name: 'Confirm it’s you' });
    expect(dialog).toHaveAccessibleDescription(
      'This action needs you to confirm your identity again.',
    );
    expect(screen.queryByRole('alertdialog')).not.toBeInTheDocument();
    const password = screen.getByLabelText(/^Password/);
    expect(password).toHaveFocus();
    expect(password).toHaveAttribute('type', 'password');
    expect(password).toHaveAttribute('autocomplete', 'current-password');
    expect(screen.queryByRole('radiogroup')).not.toBeInTheDocument();
    await expectNoA11yViolations(dialog);
    await user.keyboard('{Escape}');
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
    expect(onCancel).toHaveBeenCalledOnce();
    expect(trigger).toHaveFocus();
  });

  it('shows and hides the password with a pressed toggle', async () => {
    render(<StepUp defaultOpen />);
    const user = userEvent.setup();
    const password = await screen.findByLabelText(/^Password/);
    const toggle = screen.getByRole('button', { name: 'Show password' });
    expect(toggle).toHaveAttribute('aria-pressed', 'false');
    await user.click(toggle);
    expect(password).toHaveAttribute('type', 'text');
    expect(toggle).toHaveAttribute('aria-pressed', 'true');
    await user.click(toggle);
    expect(password).toHaveAttribute('type', 'password');
  });

  it('asks for a password before calling onVerify', async () => {
    const onVerify = vi.fn();
    render(<StepUp defaultOpen onVerify={onVerify} />);
    const user = userEvent.setup();
    const password = await screen.findByLabelText(/^Password/);
    await user.keyboard('{Enter}');
    expect(onVerify).not.toHaveBeenCalled();
    expect(password).toHaveAttribute('aria-invalid', 'true');
    expect(password).toHaveAccessibleDescription('Enter your password.');
    await user.type(password, 'hunter22');
    expect(password).not.toHaveAttribute('aria-invalid');
  });

  it('verifies a password: busy while checking, then closes and calls onVerified', async () => {
    const work = deferred();
    const onVerify = vi.fn((_: StepUpCredential) => work.promise);
    const onVerified = vi.fn();
    const onOpenChange = vi.fn();
    render(
      <StepUp
        defaultOpen
        onVerify={onVerify}
        onVerified={onVerified}
        onOpenChange={onOpenChange}
      />,
    );
    const user = userEvent.setup();
    const password = await screen.findByLabelText(/^Password/);
    await user.type(password, 'correct horse{Enter}');
    expect(onVerify).toHaveBeenCalledWith({ method: 'password', password: 'correct horse' });
    const dialog = screen.getByRole('dialog');
    expect(dialog).toHaveAttribute('aria-busy', 'true');
    expect(screen.getByRole('button', { name: 'Cancel' })).toBeDisabled();
    expect(password).toHaveAttribute('readonly');
    await user.keyboard('{Escape}');
    expect(screen.getByRole('dialog')).toBeInTheDocument();
    work.resolve();
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
    expect(onVerified).toHaveBeenCalledOnce();
    expect(onOpenChange).toHaveBeenLastCalledWith(false);
  });

  it('shows a rejected verification assertively and keeps focus in the field', async () => {
    const onVerify = vi.fn(async () => {
      throw new Error('That password is not right.');
    });
    render(<StepUp defaultOpen onVerify={onVerify} />);
    const user = userEvent.setup();
    const password = await screen.findByLabelText(/^Password/);
    await user.type(password, 'wrong');
    await user.click(screen.getByRole('button', { name: 'Confirm' }));
    const alert = await screen.findByRole('alert');
    expect(alert).toHaveTextContent('That password is not right.');
    expect(password).toHaveFocus();
    expect(password).toHaveAttribute('aria-invalid', 'true');
    expect(password).toHaveAccessibleDescription('That password is not right.');
    expect(screen.getByRole('dialog')).toBeInTheDocument();
    await expectNoA11yViolations(screen.getByRole('dialog'));
  });

  it('uses a generic message or errorMessage for other rejections', async () => {
    const { unmount } = render(<StepUp defaultOpen onVerify={() => Promise.reject('nope')} />);
    const user = userEvent.setup();
    await user.type(await screen.findByLabelText(/^Password/), 'x{Enter}');
    expect(await screen.findByRole('alert')).toHaveTextContent('That did not work. Try again.');
    unmount();
    render(
      <StepUp
        defaultOpen
        onVerify={() => {
          throw new Error('raw');
        }}
        errorMessage={() => 'Mapped message'}
      />,
    );
    await user.type(await screen.findByLabelText(/^Password/), 'x{Enter}');
    expect(await screen.findByRole('alert')).toHaveTextContent('Mapped message');
  });

  it('TOTP: submits itself when 6 digits are entered; a failure clears the code', async () => {
    const onVerify = vi
      .fn<(credential: StepUpCredential) => Promise<void>>()
      .mockRejectedValueOnce(new Error('That code is not valid.'))
      .mockResolvedValueOnce(undefined);
    const onVerified = vi.fn();
    render(<StepUp defaultOpen methods={['totp']} onVerify={onVerify} onVerified={onVerified} />);
    const user = userEvent.setup();
    const code = await screen.findByRole('textbox', { name: /Authentication code/ });
    expect(code).toHaveFocus();
    expect(code).toHaveAttribute('autocomplete', 'one-time-code');
    expect(code).toHaveAccessibleDescription('Enter the 6-digit code from your authenticator app.');
    await user.type(code, '123456');
    expect(onVerify).toHaveBeenCalledWith({ method: 'totp', code: '123456' });
    expect(await screen.findByRole('alert')).toHaveTextContent('That code is not valid.');
    expect(code).toHaveValue('');
    expect(code).toHaveFocus();
    await user.type(code, '654321');
    expect(onVerify).toHaveBeenLastCalledWith({ method: 'totp', code: '654321' });
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
    expect(onVerified).toHaveBeenCalledOnce();
  });

  it('TOTP: Confirm with an incomplete code asks for all digits', async () => {
    const onVerify = vi.fn();
    render(<StepUp defaultOpen methods={['totp']} onVerify={onVerify} />);
    const user = userEvent.setup();
    const code = await screen.findByRole('textbox', { name: /Authentication code/ });
    await user.type(code, '123');
    await user.click(screen.getByRole('button', { name: 'Confirm' }));
    expect(onVerify).not.toHaveBeenCalled();
    expect(screen.getByText('Enter all 6 digits.')).toBeInTheDocument();
    expect(code).toHaveFocus();
  });

  it('passkey: one button calls onVerify with the passkey method', async () => {
    const onVerify = vi.fn();
    render(<StepUp defaultOpen methods={['passkey']} onVerify={onVerify} />);
    const user = userEvent.setup();
    const button = await screen.findByRole('button', { name: 'Use passkey' });
    expect(button).toHaveFocus();
    expect(screen.queryByRole('textbox')).not.toBeInTheDocument();
    await user.click(button);
    expect(onVerify).toHaveBeenCalledWith({ method: 'passkey' });
  });

  it('switches between methods in preference order with a radio group', async () => {
    const onVerify = vi.fn();
    render(<StepUp defaultOpen methods={['passkey', 'password', 'totp']} onVerify={onVerify} />);
    const user = userEvent.setup();
    const group = await screen.findByRole('radiogroup', { name: 'Verification method' });
    const radios = screen.getAllByRole('radio');
    expect(radios.map((r) => r.textContent)).toEqual(['Passkey', 'Password', 'Authenticator app']);
    expect(radios[0]).toHaveAttribute('aria-checked', 'true');
    expect(group).toBeInTheDocument();
    await user.click(screen.getByRole('radio', { name: 'Password' }));
    expect(screen.getByLabelText(/^Password/, { selector: 'input' })).toBeInTheDocument();
    await user.keyboard('{ArrowRight}');
    expect(screen.getByRole('textbox', { name: /Authentication code/ })).toBeInTheDocument();
    await expectNoA11yViolations(screen.getByRole('dialog'));
  });

  it('renders a forgot link or button', async () => {
    const onForgot = vi.fn();
    const { unmount } = render(<StepUp defaultOpen forgotHref="/recover" />);
    expect(await screen.findByRole('link', { name: 'Forgot your password?' })).toHaveAttribute(
      'href',
      '/recover',
    );
    unmount();
    render(<StepUp defaultOpen onForgot={onForgot} messages={{ forgot: 'Reset password' }} />);
    const user = userEvent.setup();
    await user.click(await screen.findByRole('button', { name: 'Reset password' }));
    expect(onForgot).toHaveBeenCalledOnce();
  });

  it('starts clean on every opening', async () => {
    render(
      <StepUp methods={['password', 'totp']} onVerify={() => Promise.reject(new Error('No'))} />,
    );
    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: 'Delete workspace' }));
    await user.click(await screen.findByRole('radio', { name: 'Authenticator app' }));
    await user.click(screen.getByRole('radio', { name: 'Password' }));
    await user.type(screen.getByLabelText(/^Password/, { selector: 'input' }), 'x{Enter}');
    await screen.findByRole('alert');
    await user.click(screen.getByRole('button', { name: 'Cancel' }));
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
    await user.click(screen.getByRole('button', { name: 'Delete workspace' }));
    const password = await screen.findByLabelText(/^Password/, { selector: 'input' });
    expect(password).toHaveValue('');
    expect(screen.queryByRole('alert')).not.toBeInTheDocument();
  });

  it('useStepUpAuth resolves true when verified and false when cancelled, returning focus', async () => {
    const results: boolean[] = [];
    function Demo() {
      const { requireStepUp, element } = useStepUpAuth();
      return (
        <>
          <Button
            onClick={async () => {
              results.push(
                await requireStepUp({
                  methods: ['password'],
                  title: 'Confirm to revoke keys',
                  onVerify: async () => {},
                }),
              );
            }}
          >
            Revoke keys
          </Button>
          {element}
        </>
      );
    }
    render(<Demo />);
    const user = userEvent.setup();
    const trigger = screen.getByRole('button', { name: 'Revoke keys' });
    await user.click(trigger);
    await screen.findByRole('dialog', { name: 'Confirm to revoke keys' });
    await user.type(screen.getByLabelText(/^Password/, { selector: 'input' }), 'pw{Enter}');
    await waitFor(() => expect(results).toEqual([true]));
    await waitFor(() => expect(trigger).toHaveFocus());
    await user.click(trigger);
    await screen.findByRole('dialog');
    await user.keyboard('{Escape}');
    await waitFor(() => expect(results).toEqual([true, false]));
    await waitFor(() => expect(trigger).toHaveFocus());
  });

  it('renders on the server (closed and open)', () => {
    expect(renderToString(<StepUp />)).toContain('Delete workspace');
    expect(() => renderToString(<StepUp defaultOpen />)).not.toThrow();
  });
});
