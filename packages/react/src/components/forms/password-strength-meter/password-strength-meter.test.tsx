import { useState } from 'react';
import { describe, expect, it } from 'vitest';
import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { Field, FieldLabel } from '../form-field/index.js';
import { PasswordInput } from '../password-input/index.js';
import {
  DEFAULT_PASSWORD_RULES,
  PasswordStrengthMeter,
  scorePassword,
  type PasswordRule,
} from './index.js';

describe('scorePassword', () => {
  it('scores from the rules met and length', () => {
    expect(scorePassword('')).toBe(0);
    expect(scorePassword('abc')).toBe(0);
    expect(scorePassword('abcdefgh')).toBe(0);
    expect(scorePassword('abcdefgH')).toBe(1);
    expect(scorePassword('abcdefgH1')).toBe(2);
    expect(scorePassword('abcdefgH1!')).toBe(3);
    expect(scorePassword('abcdefgH1! long one')).toBe(4);
  });

  it('has four default rules', () => {
    expect(DEFAULT_PASSWORD_RULES.map((r) => r.id)).toEqual(['length', 'case', 'number', 'symbol']);
  });
});

describe('PasswordStrengthMeter', () => {
  it('says the strength and each rule in text, not only colour', async () => {
    render(<PasswordStrengthMeter password="abcdefgH1" />);
    expect(screen.getByText('Password strength: Fair')).toHaveAttribute('aria-live', 'polite');
    const list = screen.getByRole('list', { name: 'Password requirements' });
    const items = within(list).getAllByRole('listitem');
    expect(items.map((li) => li.textContent)).toEqual([
      'Met: At least 8 characters',
      'Met: Upper and lower case letters',
      'Met: A number',
      'Not met: A symbol or space',
    ]);
    expect(items[0]).toHaveAttribute('data-met');
    expect(items[3]).not.toHaveAttribute('data-met');
    await expectNoA11yViolations();
  });

  it('shows "Not entered" while empty with no filled segments', () => {
    const { container } = render(<PasswordStrengthMeter password="" />);
    expect(screen.getByText('Password strength: Not entered')).toBeInTheDocument();
    expect(container.querySelectorAll('[data-filled]')).toHaveLength(0);
    expect(container.firstElementChild).toHaveAttribute('data-empty');
  });

  it('accepts custom rules, a custom score and translated text', () => {
    const rules: PasswordRule[] = [
      { id: 'len', label: 'Mindestens 12 Zeichen', test: (p) => p.length >= 12 },
    ];
    const { container } = render(
      <PasswordStrengthMeter
        password="kurz"
        rules={rules}
        score={() => 4}
        messages={{
          levels: ['Sehr schwach', 'Schwach', 'Mittel', 'Gut', 'Stark'],
          strength: (l) => `Stärke: ${l}`,
          unmet: 'Nicht erfüllt:',
          rulesLabel: 'Anforderungen',
        }}
      />,
    );
    expect(screen.getByText('Stärke: Stark')).toBeInTheDocument();
    expect(screen.getByRole('listitem')).toHaveTextContent('Nicht erfüllt: Mindestens 12 Zeichen');
    expect(container.querySelectorAll('[data-filled]')).toHaveLength(4);
    expect(container.firstElementChild).toHaveAttribute('data-score', '4');
  });

  it('hides the rules on request', () => {
    render(<PasswordStrengthMeter password="x" showRules={false} />);
    expect(screen.queryByRole('list')).toBeNull();
  });

  it('describes a PasswordInput and updates as the user types', async () => {
    function SignUp() {
      const [pw, setPw] = useState('');
      return (
        <Field>
          <FieldLabel>New password</FieldLabel>
          <PasswordInput
            purpose="new"
            value={pw}
            onChange={(e) => setPw(e.target.value)}
            aria-describedby="pw-strength"
          />
          <PasswordStrengthMeter id="pw-strength" password={pw} showRules={false} />
        </Field>
      );
    }
    render(<SignUp />);
    const input = screen.getByLabelText('New password');
    const user = userEvent.setup();
    await user.type(input, 'Tr0ub4dor&3xyz');
    expect(input).toHaveAccessibleDescription('Password strength: Strong');
    await expectNoA11yViolations();
  });

  it('renders on the server', () => {
    const html = renderToString(<PasswordStrengthMeter password="abc" />);
    expect(html).toContain('mn-password-strength-meter');
    expect(html).toContain('Password strength: Very weak');
  });
});
