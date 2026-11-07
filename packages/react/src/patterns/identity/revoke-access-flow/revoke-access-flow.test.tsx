import type { ReactElement } from 'react';
import { describe, expect, it, vi } from 'vitest';
import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { Button } from '../../../components/actions/button/index.js';
import { RevokeAccessFlow, type RevokeAccessFlowProps } from './index.js';

const NOW = '2026-03-10T12:00:00Z';

type FlowProps = Omit<Partial<RevokeAccessFlowProps>, 'trigger'> & {
  trigger?: ReactElement | null;
};

function Flow({ trigger = <Button>Open</Button>, ...props }: FlowProps) {
  return (
    <RevokeAccessFlow
      {...(trigger ? { trigger } : {})}
      kind="api-key"
      items={[
        {
          id: 'k1',
          label: 'Build server',
          detail: 'sk_live_…4f2a',
          lastUsed: '2026-03-10T09:00:00Z',
        },
      ]}
      now={NOW}
      locale="en"
      onRevoke={() => Promise.resolve()}
      {...props}
    />
  );
}

function dialog() {
  return screen.getByRole('alertdialog');
}

describe('RevokeAccessFlow', () => {
  it('shows the item, last use and kind copy; focus starts on Cancel', async () => {
    const user = userEvent.setup();
    render(<Flow />);
    await user.click(screen.getByRole('button', { name: 'Open' }));
    expect(screen.getByRole('alertdialog', { name: 'Revoke API key?' })).toHaveAttribute(
      'data-tone',
      'danger',
    );
    const list = screen.getByRole('list', { name: 'To revoke' });
    expect(list).toHaveTextContent('Build server');
    expect(list).toHaveTextContent('sk_live_…4f2a');
    expect(list).toHaveTextContent('Last used 3 hours ago');
    expect(dialog()).toHaveTextContent('Apps and scripts using this key stop working immediately');
    expect(within(dialog()).getByRole('button', { name: 'Cancel' })).toHaveFocus();
    expect(within(dialog()).getByRole('button', { name: 'Revoke key' })).toHaveAttribute(
      'data-variant',
      'danger',
    );
    await expectNoA11yViolations();
  });

  it('revokes every id and closes', async () => {
    const user = userEvent.setup();
    const onRevoke = vi.fn(() => Promise.resolve());
    render(
      <Flow
        defaultOpen
        trigger={null}
        kind="session"
        items={[
          { id: 's1', label: 'Firefox on Linux', lastUsed: null },
          { id: 's2', label: 'Safari on iPhone' },
        ]}
        onRevoke={onRevoke}
      />,
    );
    expect(dialog()).toHaveAttribute('data-tone', 'warning');
    expect(dialog()).toHaveAccessibleName('Sign out 2 sessions?');
    expect(dialog()).toHaveTextContent('Never used');
    await user.click(within(dialog()).getByRole('button', { name: 'Sign out 2 sessions' }));
    expect(onRevoke).toHaveBeenCalledWith(['s1', 's2']);
    await waitFor(() => expect(screen.queryByRole('alertdialog')).toBeNull());
  });

  it('requires the phrase when requireConfirmPhrase is set', async () => {
    const user = userEvent.setup();
    const onRevoke = vi.fn();
    render(
      <Flow defaultOpen trigger={null} requireConfirmPhrase="Build server" onRevoke={onRevoke} />,
    );
    const action = within(dialog()).getByRole('button', { name: 'Revoke key' });
    expect(action).toBeDisabled();
    await user.type(screen.getByRole('textbox', { name: /to confirm/ }), 'Build server');
    expect(action).toBeEnabled();
    await user.click(action);
    expect(onRevoke).toHaveBeenCalledWith(['k1']);
  });

  it('shows the error and stays open when onRevoke rejects', async () => {
    const user = userEvent.setup();
    render(
      <Flow
        defaultOpen
        trigger={null}
        onRevoke={() => Promise.reject(new Error('Revocation failed.'))}
      />,
    );
    await user.click(within(dialog()).getByRole('button', { name: 'Revoke key' }));
    expect(await screen.findByRole('alert')).toHaveTextContent('Revocation failed.');
    expect(dialog()).toBeInTheDocument();
  });

  it('step-up: cancelling keeps the dialog without revoking; verifying revokes', async () => {
    const user = userEvent.setup();
    const onRevoke = vi.fn(() => Promise.resolve());
    const onVerify = vi.fn(() => Promise.resolve());
    render(
      <Flow
        defaultOpen
        trigger={null}
        kind="oauth-client"
        items={[{ id: 'c1', label: 'Reporting app' }]}
        stepUp={{ methods: ['password'], onVerify }}
        onRevoke={onRevoke}
      />,
    );
    await user.click(within(dialog()).getByRole('button', { name: 'Revoke client' }));
    const stepUp = await screen.findByRole('dialog', { name: 'Confirm it’s you' });
    await user.click(within(stepUp).getByRole('button', { name: 'Cancel' }));
    await waitFor(() => expect(screen.queryByRole('dialog')).toBeNull());
    expect(onRevoke).not.toHaveBeenCalled();
    expect(dialog()).toBeInTheDocument();

    await user.click(within(dialog()).getByRole('button', { name: 'Revoke client' }));
    const again = await screen.findByRole('dialog', { name: 'Confirm it’s you' });
    await user.type(within(again).getByLabelText(/^Password/, { selector: 'input' }), 'pw');
    await user.click(within(again).getByRole('button', { name: 'Confirm' }));
    expect(onVerify).toHaveBeenCalledWith({ method: 'password', password: 'pw' });
    await waitFor(() => expect(onRevoke).toHaveBeenCalledWith(['c1']));
    await waitFor(() => expect(screen.queryByRole('alertdialog')).toBeNull());
  });

  it('takes copy from messages per kind', () => {
    render(
      <Flow
        defaultOpen
        trigger={null}
        kind="device"
        messages={{ kinds: { device: { title: () => 'Gerät entfernen?' } }, cancel: 'Abbrechen' }}
      />,
    );
    expect(dialog()).toHaveAccessibleName('Gerät entfernen?');
    expect(screen.getByRole('button', { name: 'Remove device' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Abbrechen' })).toBeInTheDocument();
  });

  it('renders on the server', () => {
    expect(renderToString(<Flow />)).toContain('Open');
  });
});
