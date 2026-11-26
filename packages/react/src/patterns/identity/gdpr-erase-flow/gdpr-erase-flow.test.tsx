import { describe, expect, it, vi } from 'vitest';
import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { Button } from '../../../components/actions/button/index.js';
import { GdprEraseFlow, type GdprEraseFlowProps } from './index.js';

function Flow(props: Partial<GdprEraseFlowProps>) {
  return (
    <GdprEraseFlow
      trigger={<Button variant="danger">Erase personal data</Button>}
      subjectName="Alex Kim"
      confirmPhrase="alex@example.com"
      erased={[{ label: 'Profile and contact details' }, { label: 'messages', count: 214 }]}
      retained={[{ label: 'Invoices', reason: 'Kept for the legally required retention period' }]}
      onErase={() => ({ auditId: 'evt_7Q2K' })}
      {...props}
    />
  );
}

const flowDialog = () => screen.getByRole('dialog', { name: 'Erase personal data' });
const action = () => within(flowDialog()).getByRole('button', { name: 'Erase data' });

async function toConfirm(user: ReturnType<typeof userEvent.setup>) {
  await user.click(within(flowDialog()).getByRole('button', { name: 'Continue' }));
}

describe('GdprEraseFlow', () => {
  it('reviews, requires acknowledgement and the phrase, erases and shows the audit id', async () => {
    const user = userEvent.setup();
    const onErase = vi.fn(() => Promise.resolve({ auditId: 'evt_7Q2K' }));
    const onDone = vi.fn();
    render(<Flow onErase={onErase} onDone={onDone} />);
    await user.click(screen.getByRole('button', { name: 'Erase personal data' }));
    const dialog = flowDialog();
    expect(dialog).toHaveClass('mn-gdpr-erase-flow');
    expect(dialog).toHaveTextContent('Step 1 of 2');
    expect(screen.getByRole('list', { name: 'Will be erased' })).toHaveTextContent('214 messages');
    expect(screen.getByRole('list', { name: 'Will be kept' })).toHaveTextContent(
      'Kept for the legally required retention period',
    );
    expect(dialog).toHaveTextContent('This cannot be undone');
    await expectNoA11yViolations(dialog);

    await toConfirm(user);
    expect(screen.getByRole('heading', { name: 'Confirm the erasure' })).toHaveFocus();
    expect(action()).toBeDisabled();
    expect(action()).toHaveAccessibleDescription(
      'Tick the box and type the confirmation text to continue.',
    );
    await user.type(screen.getByRole('textbox', { name: /alex@example.com/ }), 'alex@example.com');
    expect(action()).toBeDisabled();
    await user.click(screen.getByRole('checkbox', { name: 'I understand this cannot be undone' }));
    expect(action()).toBeEnabled();
    await expectNoA11yViolations(dialog);
    await user.click(action());

    expect(onErase).toHaveBeenCalledOnce();
    expect(await screen.findByRole('heading', { name: 'Personal data erased' })).toHaveFocus();
    expect(screen.getByText('Logged in the audit log')).toBeInTheDocument();
    expect(screen.getByText('evt_7Q2K')).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Done' }));
    await waitFor(() => expect(screen.queryByRole('dialog')).toBeNull());
    expect(onDone).toHaveBeenCalledOnce();
  });

  it('has no audit line when onErase resolves without an id', async () => {
    const user = userEvent.setup();
    render(
      <GdprEraseFlow
        defaultOpen
        subjectName="Alex Kim"
        erased={[{ label: 'Profile' }]}
        onErase={() => Promise.resolve()}
      />,
    );
    await toConfirm(user);
    await user.click(screen.getByRole('checkbox'));
    await user.type(screen.getByRole('textbox', { name: /Alex Kim/ }), 'Alex Kim');
    await user.click(action());
    expect(
      await screen.findByRole('heading', { name: 'Personal data erased' }),
    ).toBeInTheDocument();
    expect(screen.queryByText('Logged in the audit log')).toBeNull();
  });

  it('blocks the review while loading or failed', () => {
    const { rerender } = render(<Flow defaultOpen loading />);
    const next = within(flowDialog()).getByRole('button', { name: 'Continue' });
    expect(next).toBeDisabled();
    expect(next).toHaveAccessibleDescription('The summary must load before you continue.');
    rerender(<Flow defaultOpen error={<p>Could not load the data map.</p>} />);
    expect(screen.getByText('Could not load the data map.')).toBeInTheDocument();
    expect(screen.queryByRole('list', { name: 'Will be kept' })).toBeNull();
    expect(within(flowDialog()).getByRole('button', { name: 'Continue' })).toBeDisabled();
  });

  it('step-up: cancel keeps the data; success erases', async () => {
    const user = userEvent.setup();
    const onErase = vi.fn();
    render(<Flow defaultOpen onErase={onErase} stepUp={{ onVerify: () => {} }} />);
    await toConfirm(user);
    await user.click(screen.getByRole('checkbox'));
    await user.type(screen.getByRole('textbox', { name: /alex@example.com/ }), 'alex@example.com');
    await user.click(action());
    let stepUp = await screen.findByRole('dialog', { name: 'Confirm it’s you' });
    expect(stepUp).toHaveAccessibleDescription(
      'Erasing personal data needs you to confirm your identity.',
    );
    await user.click(within(stepUp).getByRole('button', { name: 'Cancel' }));
    await waitFor(() =>
      expect(screen.queryByRole('dialog', { name: 'Confirm it’s you' })).toBeNull(),
    );
    expect(onErase).not.toHaveBeenCalled();

    await user.click(action());
    stepUp = await screen.findByRole('dialog', { name: 'Confirm it’s you' });
    await user.type(within(stepUp).getByLabelText(/^Password/), 'secret');
    await user.click(within(stepUp).getByRole('button', { name: 'Confirm' }));
    expect(
      await screen.findByRole('heading', { name: 'Personal data erased' }),
    ).toBeInTheDocument();
    expect(onErase).toHaveBeenCalledOnce();
  });

  it('a failed erasure shows the error and stays on the confirm step', async () => {
    const user = userEvent.setup();
    render(
      <Flow defaultOpen onErase={() => Promise.reject(new Error('The erase job is busy.'))} />,
    );
    await toConfirm(user);
    await user.click(screen.getByRole('checkbox'));
    await user.type(screen.getByRole('textbox', { name: /alex@example.com/ }), 'alex@example.com');
    await user.keyboard('{Enter}');
    expect(await screen.findByText('The erase job is busy.')).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Confirm the erasure' })).toBeInTheDocument();
  });

  it('takes every string from messages', () => {
    render(
      <Flow defaultOpen messages={{ title: 'Daten löschen', erasedHeading: 'Wird gelöscht' }} />,
    );
    expect(screen.getByRole('dialog', { name: 'Daten löschen' })).toBeInTheDocument();
    expect(screen.getByRole('list', { name: 'Wird gelöscht' })).toBeInTheDocument();
  });

  it('renders on the server', () => {
    expect(() => renderToString(<Flow />)).not.toThrow();
  });
});
