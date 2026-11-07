import { describe, expect, it, vi } from 'vitest';
import { act, render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { Button } from '../../../components/actions/button/index.js';
import { TwoFactorSetup, type TwoFactorSetupProps } from './index.js';

async function next(user: ReturnType<typeof userEvent.setup>) {
  await user.click(screen.getByRole('button', { name: 'Next' }));
  await waitFor(() => expect(screen.getByRole('textbox', { name: '6-digit code' })).toHaveFocus());
}

const CODES = ['a1b2-c3d4', 'e5f6-g7h8', 'j9k0-m1n2', 'p3q4-r5s6'];

function Setup(props: Partial<TwoFactorSetupProps>) {
  return (
    <TwoFactorSetup
      trigger={<Button>Turn on</Button>}
      secret="JBSWY3DPEHPK3PXPJBSW"
      qrCodeSrc="data:image/png;base64,AAAA"
      accountName="sam@example.com"
      onVerify={() => Promise.resolve(CODES)}
      {...props}
    />
  );
}

describe('TwoFactorSetup', () => {
  it('starts on the scan step with the QR code and grouped key', async () => {
    const user = userEvent.setup();
    render(<Setup />);
    await user.click(screen.getByRole('button', { name: 'Turn on' }));
    const dialog = screen.getByRole('dialog', { name: 'Set up two-step verification' });
    expect(dialog).toHaveClass('mn-two-factor-setup');
    expect(dialog).toHaveAttribute('data-step', 'scan');
    expect(dialog).toHaveTextContent('Step 1 of 3');
    expect(
      screen.getByRole('img', { name: 'QR code for your authenticator app' }),
    ).toBeInTheDocument();
    expect(dialog).toHaveTextContent('sam@example.com');
    expect(screen.getByRole('textbox', { name: 'Setup key' })).toHaveValue(
      'JBSW Y3DP EHPK 3PXP JBSW',
    );
    expect(screen.getByRole('heading', { name: 'Set up two-step verification' })).toHaveFocus();
    await expectNoA11yViolations();
  });

  it('verifies the code, shows recovery codes and finishes after acknowledging', async () => {
    const user = userEvent.setup();
    const onVerify = vi.fn(() => Promise.resolve(CODES));
    const onComplete = vi.fn();
    render(<Setup onVerify={onVerify} onComplete={onComplete} />);
    await user.click(screen.getByRole('button', { name: 'Turn on' }));
    await user.click(screen.getByRole('button', { name: 'Next' }));
    const code = screen.getByRole('textbox', { name: '6-digit code' });
    await waitFor(() => expect(code).toHaveFocus());
    await user.keyboard('123456');
    expect(onVerify).toHaveBeenCalledWith('123456');
    const list = await screen.findByRole('list', { name: 'Recovery codes' });
    expect(within(list).getAllByRole('listitem')).toHaveLength(4);
    expect(screen.getByRole('dialog')).toHaveTextContent('Step 3 of 3');
    await waitFor(() =>
      expect(screen.getByRole('heading', { name: 'Save your recovery codes' })).toHaveFocus(),
    );
    await expectNoA11yViolations();
    await user.click(screen.getByRole('button', { name: 'Done' }));
    expect(screen.getByText('Confirm that you have saved the codes.')).toBeInTheDocument();
    expect(onComplete).not.toHaveBeenCalled();
    await user.click(screen.getByRole('checkbox', { name: 'I have saved my recovery codes' }));
    await user.click(screen.getByRole('button', { name: 'Done' }));
    expect(onComplete).toHaveBeenCalled();
    await waitFor(() => expect(screen.queryByRole('dialog')).toBeNull());
  });

  it('reports an incomplete or rejected code and clears it', async () => {
    const user = userEvent.setup();
    const onVerify = vi.fn(() => Promise.reject(new Error('Invalid code')));
    render(<Setup onVerify={onVerify} />);
    await user.click(screen.getByRole('button', { name: 'Turn on' }));
    await next(user);
    await user.keyboard('12');
    await user.click(screen.getByRole('button', { name: 'Verify' }));
    expect(screen.getByText('Enter all 6 digits.')).toBeInTheDocument();
    expect(onVerify).not.toHaveBeenCalled();
    await user.type(screen.getByRole('textbox', { name: '6-digit code' }), '3456');
    expect(await screen.findByText('Invalid code')).toBeInTheDocument();
    expect(screen.getByRole('textbox', { name: '6-digit code' })).toHaveValue('');
    expect(screen.getByRole('dialog')).toHaveAttribute('data-step', 'verify');
  });

  it('finishes straight after verifying when there are no recovery codes', async () => {
    const user = userEvent.setup();
    const onComplete = vi.fn();
    render(<Setup onVerify={() => undefined} onComplete={onComplete} />);
    await user.click(screen.getByRole('button', { name: 'Turn on' }));
    await next(user);
    await user.keyboard('654321');
    expect(onComplete).toHaveBeenCalled();
    await waitFor(() => expect(screen.queryByRole('dialog')).toBeNull());
  });

  it('uses recoveryCodes from props, Back returns to scanning, Escape cancels', async () => {
    const user = userEvent.setup();
    const onCancel = vi.fn();
    render(
      <Setup
        defaultOpen
        trigger={undefined}
        recoveryCodes={['x1', 'x2']}
        onVerify={() => undefined}
        onCancel={onCancel}
      />,
    );
    await user.click(screen.getByRole('button', { name: 'Next' }));
    await user.click(screen.getByRole('button', { name: 'Back' }));
    expect(screen.getByRole('dialog')).toHaveAttribute('data-step', 'scan');
    await next(user);
    await user.keyboard('111111');
    expect(await screen.findByRole('list', { name: 'Recovery codes' })).toHaveTextContent('x1');
    await user.keyboard('{Escape}');
    expect(onCancel).toHaveBeenCalled();
  });

  it('cannot be dismissed while verifying', async () => {
    const user = userEvent.setup();
    let resolve!: (v: string[]) => void;
    render(
      <Setup
        defaultOpen
        trigger={undefined}
        onVerify={() => new Promise<string[]>((r) => (resolve = r))}
      />,
    );
    await next(user);
    await user.keyboard('222222');
    expect(screen.getByRole('dialog')).toHaveAttribute('aria-busy', 'true');
    await user.keyboard('{Escape}');
    expect(screen.getByRole('dialog')).toBeInTheDocument();
    await act(async () => resolve(CODES));
    expect(await screen.findByRole('list', { name: 'Recovery codes' })).toBeInTheDocument();
  });

  it('takes translated messages', async () => {
    render(
      <Setup
        defaultOpen
        trigger={undefined}
        messages={{ scanTitle: 'Zwei-Faktor einrichten', next: 'Weiter' }}
      />,
    );
    expect(screen.getByRole('dialog', { name: 'Zwei-Faktor einrichten' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Weiter' })).toBeInTheDocument();
  });

  it('renders its trigger on the server', () => {
    expect(renderToString(<Setup />)).toContain('Turn on');
  });
});
