import { describe, expect, it, vi } from 'vitest';
import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { createRef, useState } from 'react';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import {
  AiSpendCap,
  aiSpendAmountFromMajor,
  aiSpendAmountToMajor,
  aiSpendCapState,
  formatAiSpendAmount,
  type AiSpendCapProps,
  type AiSpendCapSettings,
} from './index.js';

/** Intl separates amount and symbol with no-break spaces in some locales. */
const plain = (text: string) => text.replace(/[\u00a0\u202f]/g, ' ');

const base: AiSpendCapProps = {
  spent: 1240,
  limit: 5000,
  currency: 'GBP',
  period: 'month',
  locale: 'en-GB',
};

describe('aiSpendCapState', () => {
  it('maps spending to ok, warning, reached and no-limit', () => {
    expect(aiSpendCapState({ spent: 1240, limit: 5000 })).toBe('ok');
    expect(aiSpendCapState({ spent: 4000, limit: 5000 })).toBe('warning');
    expect(aiSpendCapState({ spent: 3999, limit: 5000 })).toBe('ok');
    expect(aiSpendCapState({ spent: 2500, limit: 5000, warnAt: 0.5 })).toBe('warning');
    expect(aiSpendCapState({ spent: 5000, limit: 5000 })).toBe('reached');
    expect(aiSpendCapState({ spent: 9000, limit: 5000 })).toBe('reached');
    expect(aiSpendCapState({ spent: 0, limit: 0 })).toBe('reached');
    expect(aiSpendCapState({ spent: 9000, limit: null })).toBe('no-limit');
  });
});

describe('money helpers', () => {
  it('formats integer minor units in two locales without floats', () => {
    expect(formatAiSpendAmount(1240, { currency: 'GBP', locale: 'en-GB' })).toBe('£12.40');
    expect(plain(formatAiSpendAmount(1240, { currency: 'EUR', locale: 'de-DE' }))).toBe('12,40 €');
    expect(plain(formatAiSpendAmount(123456789, { currency: 'EUR', locale: 'de-DE' }))).toBe(
      '1.234.567,89 €',
    );
    // 0.1 + 0.2 style drift cannot happen: the integer is never divided.
    expect(formatAiSpendAmount(30, { currency: 'USD', locale: 'en-US' })).toBe('$0.30');
    expect(formatAiSpendAmount(500, { currency: 'JPY', locale: 'en-US' })).toBe('¥500');
  });

  it('formats micros and converts typed amounts to integers', () => {
    expect(
      formatAiSpendAmount(12_400_000, { currency: 'GBP', unit: 'micros', locale: 'en-GB' }),
    ).toBe('£12.40');
    expect(formatAiSpendAmount(4_500, { currency: 'USD', unit: 'micros', locale: 'en-US' })).toBe(
      '$0.00',
    );
    expect(aiSpendAmountFromMajor(19.99, 'USD')).toBe(1999);
    expect(aiSpendAmountFromMajor(0.07, 'USD', 'micros')).toBe(70_000);
    expect(aiSpendAmountFromMajor(500, 'JPY')).toBe(500);
    expect(aiSpendAmountToMajor(1999, 'USD')).toBe(19.99);
    expect(aiSpendAmountToMajor(12_400_000, 'GBP', 'micros')).toBe(12.4);
  });
});

describe('AiSpendCap', () => {
  it('renders a named region with the amount, a meter and the state in text', async () => {
    const ref = createRef<HTMLElement>();
    render(
      <AiSpendCap
        {...base}
        ref={ref}
        data-testid="root"
        className="extra"
        resetsAt="2026-12-01T00:00:00Z"
        timeZone="UTC"
      />,
    );
    const root = screen.getByTestId('root');
    expect(root).toHaveClass('mn-ai-spend-cap', 'extra');
    expect(ref.current).toBe(root);
    expect(screen.getByRole('group', { name: 'AI spending limit' })).toBe(root);
    expect(root).toHaveAttribute('data-state', 'ok');
    expect(root).toHaveAttribute('data-behaviour', 'pause');
    expect(screen.getByText('£12.40 of £50.00 this month')).toBeInTheDocument();
    const meter = screen.getByRole('meter', { name: 'Spent against the limit' });
    expect(meter).toHaveAttribute('aria-valuetext', '£12.40 of £50.00 this month');
    expect(screen.getByText('Within limit')).toBeInTheDocument();
    expect(screen.getByText('£37.60 left')).toBeInTheDocument();
    expect(screen.getByText('Warns at 80%')).toBeInTheDocument();
    expect(screen.getByText('At the limit, AI pauses and asks')).toBeInTheDocument();
    expect(screen.getByText('Resets 1 December')).toHaveAttribute(
      'datetime',
      '2026-12-01T00:00:00.000Z',
    );
    await expectNoA11yViolations();
  });

  it('formats every amount in the given locale and currency', () => {
    render(
      <AiSpendCap
        spent={1240}
        limit={5000}
        currency="EUR"
        period="week"
        locale="de-DE"
        breakdown={[{ id: 'a', label: 'Research agent', spent: 900 }]}
      />,
    );
    const meter = screen.getByRole('meter');
    expect(plain(meter.getAttribute('aria-valuetext') ?? '')).toBe('12,40 € of 50,00 € this week');
    const rows = screen.getByRole('list', { name: 'Where it was spent' });
    expect(plain(within(rows).getByRole('listitem').textContent ?? '')).toBe(
      'Research agent9,00 €',
    );
  });

  it('shows the amount only, with no meter, when there is no limit', () => {
    render(<AiSpendCap {...base} limit={null} data-testid="root" />);
    expect(screen.getByTestId('root')).toHaveAttribute('data-state', 'no-limit');
    expect(screen.getByText('£12.40 spent this month')).toBeInTheDocument();
    expect(screen.getByText('No limit set')).toBeInTheDocument();
    expect(screen.queryByRole('meter')).toBeNull();
    expect(screen.getByRole('button', { name: 'Set a limit' })).toBeInTheDocument();
  });

  it('announces a change of state once, never the first state or a change of amount', () => {
    const { rerender } = render(<AiSpendCap {...base} />);
    const status = screen.getByRole('status');
    expect(status).toBeEmptyDOMElement();
    rerender(<AiSpendCap {...base} spent={1300} />);
    expect(status).toBeEmptyDOMElement();
    rerender(<AiSpendCap {...base} spent={4200} />);
    expect(status).toHaveTextContent(
      'AI spending limit: Close to the limit. £42.00 of £50.00 this month',
    );
    rerender(<AiSpendCap {...base} spent={4300} />);
    expect(status).toHaveTextContent('£42.00 of £50.00 this month');
    rerender(<AiSpendCap {...base} spent={5000} />);
    expect(status).toHaveTextContent('AI spending limit: Limit reached.');
  });

  it('opens the form, edits every setting and saves one settings object', async () => {
    const user = userEvent.setup();
    const onSave = vi.fn();
    const onEditingChange = vi.fn();
    render(<AiSpendCap {...base} onSave={onSave} onEditingChange={onEditingChange} />);
    await user.click(screen.getByRole('button', { name: 'Edit limit' }));
    expect(onEditingChange).toHaveBeenLastCalledWith(true);
    const form = screen.getByRole('form', { name: 'Edit AI spending limit' });
    const amount = within(form).getByRole('spinbutton', { name: 'Limit' });
    expect(amount).toHaveFocus();
    expect(amount).toHaveValue('£50.00');
    expect(amount).toHaveAccessibleDescription('£12.40 spent this month so far.');
    await user.clear(amount);
    await user.type(amount, '75.5');
    await user.selectOptions(screen.getByRole('combobox', { name: 'Warn me at' }), '0.9');
    const group = screen.getByRole('radiogroup', { name: 'When the limit is reached' });
    expect(within(group).getByRole('radio', { name: 'Pause and ask' })).toBeChecked();
    expect(
      within(group).getByRole('radio', { name: 'Keep going and tell me' }),
    ).toHaveAccessibleDescription(
      'AI carries on and you are notified. Spending can go over the limit.',
    );
    await user.click(within(group).getByRole('radio', { name: 'Keep going and tell me' }));
    await expectNoA11yViolations();
    expect(onSave).not.toHaveBeenCalled();
    await user.click(screen.getByRole('button', { name: 'Save' }));
    expect(onSave).toHaveBeenCalledTimes(1);
    expect(onSave).toHaveBeenCalledWith({ limit: 7550, warnAt: 0.9, behaviour: 'notify' });
    expect(screen.queryByRole('form')).toBeNull();
    expect(onEditingChange).toHaveBeenLastCalledWith(false);
    expect(screen.getByRole('button', { name: 'Edit limit' })).toHaveFocus();
  });

  it('saves with Enter from the amount field and reads amounts typed in the locale', async () => {
    const user = userEvent.setup();
    const onSave = vi.fn();
    render(
      <AiSpendCap
        spent={1240}
        limit={5000}
        currency="EUR"
        period="month"
        locale="de-DE"
        unit="minor"
        defaultEditing
        onSave={onSave}
      />,
    );
    const amount = screen.getByRole('spinbutton', { name: 'Limit' });
    await user.clear(amount);
    await user.type(amount, '1.250,50{Enter}');
    expect(onSave).toHaveBeenCalledWith({ limit: 125050, warnAt: 0.8, behaviour: 'pause' });
  });

  it('saves micros as integers', async () => {
    const user = userEvent.setup();
    const onSave = vi.fn();
    render(
      <AiSpendCap
        {...base}
        unit="micros"
        spent={12_400_000}
        limit={50_000_000}
        defaultEditing
        onSave={onSave}
      />,
    );
    const amount = screen.getByRole('spinbutton', { name: 'Limit' });
    expect(amount).toHaveValue('£50.00');
    await user.clear(amount);
    await user.type(amount, '60.25{Enter}');
    expect(onSave).toHaveBeenCalledWith({ limit: 60_250_000, warnAt: 0.8, behaviour: 'pause' });
  });

  it('saves the warn-at share the form shows when the saved one cannot be offered', async () => {
    const user = userEvent.setup();
    const onSave = vi.fn();
    render(<AiSpendCap {...base} warnAt={1} onSave={onSave} />);
    await user.click(screen.getByRole('button', { name: 'Edit limit' }));
    // 100% is not a choice: the nearest one is selected, and that is what is saved.
    expect(screen.getByRole('combobox', { name: 'Warn me at' })).toHaveValue('0.9');
    await user.click(screen.getByRole('button', { name: 'Save' }));
    expect(onSave).toHaveBeenCalledWith({ limit: 5000, warnAt: 0.9, behaviour: 'pause' });
  });

  it('cancels without saving and throws the draft away', async () => {
    const user = userEvent.setup();
    const onSave = vi.fn();
    render(<AiSpendCap {...base} onSave={onSave} />);
    await user.click(screen.getByRole('button', { name: 'Edit limit' }));
    const amount = screen.getByRole('spinbutton', { name: 'Limit' });
    await user.clear(amount);
    await user.type(amount, '90');
    await user.click(screen.getByRole('button', { name: 'Cancel' }));
    expect(onSave).not.toHaveBeenCalled();
    expect(screen.queryByRole('form')).toBeNull();
    await user.click(screen.getByRole('button', { name: 'Edit limit' }));
    expect(screen.getByRole('spinbutton', { name: 'Limit' })).toHaveValue('£50.00');
  });

  it('rejects an empty, zero, unreadable or too large amount', async () => {
    const user = userEvent.setup();
    const onSave = vi.fn();
    render(<AiSpendCap {...base} maxLimit={20000} defaultEditing onSave={onSave} />);
    const amount = screen.getByRole('spinbutton', { name: 'Limit' });
    const save = screen.getByRole('button', { name: 'Save' });

    await user.clear(amount);
    await user.click(save);
    expect(
      screen.getByText('Enter an amount. To have no limit, use Remove limit.'),
    ).toBeInTheDocument();
    expect(amount).toBeInvalid();
    expect(amount).toHaveFocus();

    await user.type(amount, '0');
    expect(screen.queryByText(/To have no limit/)).toBeNull();
    await user.click(save);
    expect(screen.getByText('Enter an amount greater than zero.')).toBeInTheDocument();

    await user.clear(amount);
    await user.type(amount, 'lots');
    await user.click(save);
    expect(screen.getByText('Enter an amount, such as 50.')).toBeInTheDocument();
    expect(amount).toBeInvalid();

    await user.clear(amount);
    await user.type(amount, '200.01');
    await user.click(save);
    expect(screen.getByText('Enter £200.00 or less.')).toBeInTheDocument();
    expect(amount).toHaveAccessibleDescription(expect.stringContaining('Enter £200.00 or less.'));

    expect(onSave).not.toHaveBeenCalled();
    await user.clear(amount);
    await user.type(amount, '200');
    await user.click(save);
    expect(onSave).toHaveBeenCalledWith({ limit: 20000, warnAt: 0.8, behaviour: 'pause' });
  });

  it('asks for confirmation before saving a limit below what is already spent', async () => {
    const user = userEvent.setup();
    const onSave = vi.fn();
    render(<AiSpendCap {...base} defaultEditing onSave={onSave} />);
    const amount = screen.getByRole('spinbutton', { name: 'Limit' });
    await user.clear(amount);
    await user.type(amount, '10');
    await user.click(screen.getByRole('button', { name: 'Save' }));
    expect(onSave).not.toHaveBeenCalled();
    expect(
      screen.getByText(
        'This is below the £12.40 already spent this month, so AI will pause straight away. Save again to confirm.',
      ),
    ).toBeInTheDocument();

    // Changing the amount withdraws the confirmation.
    await user.clear(amount);
    await user.type(amount, '11');
    expect(screen.getByRole('button', { name: 'Save' })).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Save' }));
    expect(onSave).not.toHaveBeenCalled();

    await user.click(screen.getByRole('button', { name: 'Save anyway' }));
    expect(onSave).toHaveBeenCalledWith({ limit: 1100, warnAt: 0.8, behaviour: 'pause' });
  });

  it('removes the limit only after an inline confirmation', async () => {
    const user = userEvent.setup();
    const onSave = vi.fn();
    const confirmSpy = vi.spyOn(window, 'confirm');
    render(<AiSpendCap {...base} warnAt={0.9} behaviour="notify" defaultEditing onSave={onSave} />);
    await user.click(screen.getByRole('button', { name: 'Remove limit' }));
    const confirm = screen.getByRole('group', { name: 'Remove the spending limit?' });
    expect(confirm).toHaveAccessibleDescription(
      'AI will be able to spend without a cap until a new limit is set.',
    );
    expect(screen.queryByRole('button', { name: 'Save' })).toBeNull();
    const keep = within(confirm).getByRole('button', { name: 'Keep limit' });
    expect(keep).toHaveFocus();
    await expectNoA11yViolations();

    await user.keyboard('{Enter}');
    expect(onSave).not.toHaveBeenCalled();
    expect(screen.queryByRole('group', { name: 'Remove the spending limit?' })).toBeNull();
    const remove = screen.getByRole('button', { name: 'Remove limit' });
    expect(remove).toHaveFocus();

    await user.keyboard('{Enter}');
    await user.click(
      within(screen.getByRole('group', { name: 'Remove the spending limit?' })).getByRole(
        'button',
        {
          name: 'Remove limit',
        },
      ),
    );
    expect(onSave).toHaveBeenCalledWith({ limit: null, warnAt: 0.9, behaviour: 'notify' });
    expect(confirmSpy).not.toHaveBeenCalled();
    expect(screen.queryByRole('form')).toBeNull();
    confirmSpy.mockRestore();
  });

  it('offers no way to remove a limit that is not set', () => {
    render(<AiSpendCap {...base} limit={null} defaultEditing />);
    expect(screen.getByRole('spinbutton', { name: 'Limit' })).toHaveValue('');
    expect(screen.queryByRole('button', { name: 'Remove limit' })).toBeNull();
  });

  it('shows a paused notice at the limit, and "Raise limit" opens the form', async () => {
    const user = userEvent.setup();
    const { rerender } = render(<AiSpendCap {...base} spent={5210} data-testid="root" />);
    expect(screen.getByTestId('root')).toHaveAttribute('data-state', 'reached');
    expect(screen.getByText('Limit reached')).toBeInTheDocument();
    expect(screen.getByText('£2.10 over the limit')).toBeInTheDocument();
    expect(screen.getByText('AI is paused.')).toBeInTheDocument();
    expect(
      screen.getByText(/The £50.00 limit for this month has been reached/),
    ).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Raise limit' }));
    expect(screen.getByRole('spinbutton', { name: 'Limit' })).toHaveFocus();
    expect(screen.queryByRole('button', { name: 'Raise limit' })).toBeNull();

    rerender(<AiSpendCap {...base} spent={5210} behaviour="notify" data-testid="root" />);
    expect(screen.queryByText('AI is paused.')).toBeNull();
    expect(screen.getByText('At the limit, AI keeps going and tells you')).toBeInTheDocument();
  });

  it('is read-only without canEdit, says who can change it and can request an increase', async () => {
    const user = userEvent.setup();
    const onRequestIncrease = vi.fn();
    const { rerender } = render(
      <AiSpendCap
        {...base}
        spent={5000}
        canEdit={false}
        editing
        onRequestIncrease={onRequestIncrease}
        data-testid="root"
      />,
    );
    expect(screen.getByTestId('root')).toHaveAttribute('data-readonly', 'true');
    expect(screen.queryByRole('button', { name: 'Edit limit' })).toBeNull();
    expect(screen.queryByRole('button', { name: 'Raise limit' })).toBeNull();
    expect(screen.queryByRole('form')).toBeNull();
    expect(screen.getByText('Only a workspace admin can change this limit.')).toBeInTheDocument();
    const request = screen.getAllByRole('button', { name: 'Request a higher limit' });
    expect(request).toHaveLength(1);
    await user.tab();
    expect(request[0]).toHaveFocus();
    await user.keyboard('{Enter}');
    expect(onRequestIncrease).toHaveBeenCalledTimes(1);
    await expectNoA11yViolations();

    rerender(<AiSpendCap {...base} canEdit={false} onRequestIncrease={onRequestIncrease} />);
    expect(screen.getAllByRole('button', { name: 'Request a higher limit' })).toHaveLength(1);
    rerender(<AiSpendCap {...base} canEdit={false} />);
    expect(screen.queryByRole('button')).toBeNull();
  });

  it('can be controlled: the form follows `editing` and only asks to close', async () => {
    const user = userEvent.setup();
    const onEditingChange = vi.fn();
    const onSave = vi.fn();
    function Controlled() {
      const [settings, setSettings] = useState<AiSpendCapSettings>({
        limit: 5000,
        warnAt: 0.8,
        behaviour: 'pause',
      });
      const [editing, setEditing] = useState(false);
      return (
        <AiSpendCap
          {...base}
          {...settings}
          editing={editing}
          onEditingChange={(next) => {
            onEditingChange(next);
            setEditing(next);
          }}
          onSave={(next) => {
            onSave(next);
            setSettings(next);
          }}
        />
      );
    }
    const { rerender } = render(<AiSpendCap {...base} editing onEditingChange={onEditingChange} />);
    await user.click(screen.getByRole('button', { name: 'Cancel' }));
    expect(onEditingChange).toHaveBeenCalledWith(false);
    expect(screen.getByRole('form')).toBeInTheDocument();

    rerender(<Controlled />);
    await user.click(screen.getByRole('button', { name: 'Edit limit' }));
    const amount = screen.getByRole('spinbutton', { name: 'Limit' });
    await user.clear(amount);
    await user.type(amount, '15');
    // Nothing changes until Save.
    expect(screen.getByText('£12.40 of £50.00 this month')).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Save' }));
    expect(onSave).toHaveBeenCalledWith({ limit: 1500, warnAt: 0.8, behaviour: 'pause' });
    expect(screen.getByText('£12.40 of £15.00 this month')).toBeInTheDocument();
    expect(screen.getByText('Close to the limit')).toBeInTheDocument();
    expect(screen.getByRole('status')).toHaveTextContent('Close to the limit');
  });

  it('locks the form while saving', () => {
    render(<AiSpendCap {...base} editing saving />);
    expect(screen.getByRole('spinbutton', { name: 'Limit' })).toBeDisabled();
    expect(screen.getByRole('button', { name: 'Cancel' })).toBeDisabled();
    expect(screen.getByRole('button', { name: 'Save' })).toHaveAttribute('aria-busy', 'true');
  });

  it('accepts translated strings and custom warn-at options', async () => {
    const user = userEvent.setup();
    render(
      <AiSpendCap
        spent={1240}
        limit={5000}
        currency="EUR"
        period="month"
        locale="fr-FR"
        warnAt={0.6}
        warnAtOptions={[0.5, 0.7]}
        messages={{
          title: 'Plafond de dépenses IA',
          period: () => 'ce mois-ci',
          state: () => 'Dans la limite',
          spentOfLimit: (spent, limit, period) => `${spent} sur ${limit} ${period}`,
          edit: 'Modifier',
          save: 'Enregistrer',
          warnAtLabel: 'Me prévenir à',
          warnAtOption: (percent) => percent,
        }}
      />,
    );
    expect(screen.getByRole('group', { name: 'Plafond de dépenses IA' })).toBeInTheDocument();
    expect(screen.getByText('Dans la limite')).toBeInTheDocument();
    expect(plain(screen.getByRole('meter').getAttribute('aria-valuetext') ?? '')).toBe(
      '12,40 € sur 50,00 € ce mois-ci',
    );
    await user.click(screen.getByRole('button', { name: 'Modifier' }));
    expect(screen.getByRole('button', { name: 'Enregistrer' })).toBeInTheDocument();
    const select = screen.getByRole('combobox', { name: 'Me prévenir à' });
    expect(select).toHaveValue('0.6');
    expect(
      within(select)
        .getAllByRole('option')
        .map((o) => plain(o.textContent ?? '')),
    ).toEqual(['50 %', '60 %', '70 %']);
  });

  it('renders on the server', () => {
    const html = renderToString(
      <AiSpendCap {...base} defaultEditing resetsAt="2026-12-01T00:00:00Z" timeZone="UTC" />,
    );
    expect(html).toContain('mn-ai-spend-cap');
    expect(html).toContain('role="meter"');
    expect(html).toContain('£12.40 of £50.00 this month');
    expect(html).toContain('Resets 1 December');
  });
});
