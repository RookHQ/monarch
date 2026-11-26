import { describe, expect, it, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { useState } from 'react';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { Field, FieldLabel } from '../../forms/form-field/index.js';
import { Input } from '../../forms/text-field/index.js';
import { AiAutofillField, type AiAutofillFieldProps, type AiAutofillState } from './index.js';

function Company(props: Partial<AiAutofillFieldProps> & { initial?: AiAutofillState }) {
  const { initial = 'suggested', ...rest } = props;
  const [value, setValue] = useState('Northwind Ltd');
  const [state, setState] = useState<AiAutofillState>(initial);
  return (
    <AiAutofillField
      value="Northwind Ltd"
      state={state}
      confidence={0.42}
      source="invoice.pdf"
      label="company"
      onAccept={() => setState('accepted')}
      onClear={() => {
        setValue('');
        setState('cleared');
      }}
      onEdit={() => setState('edited')}
      {...rest}
    >
      <Field>
        <FieldLabel>Company</FieldLabel>
        <Input value={value} onChange={(e) => setValue(e.target.value)} />
      </Field>
    </AiAutofillField>
  );
}

describe('AiAutofillField', () => {
  it('marks a suggested value with the AI badge, confidence and source', async () => {
    const { container } = render(<Company />);
    const root = container.querySelector('.mn-ai-autofill-field');
    expect(root).toHaveAttribute('data-state', 'suggested');
    expect(screen.getByText('AI suggested')).toBeInTheDocument();
    expect(screen.getByText('Needs check')).toBeInTheDocument();
    expect(screen.getByText(/From invoice\.pdf/)).toBeInTheDocument();
    const input = screen.getByRole('textbox', { name: 'Company' });
    expect(input).toHaveAccessibleDescription(
      /AI suggested.*Needs check, 42% confidence.*invoice\.pdf/,
    );
    await expectNoA11yViolations();
  });

  it('accept keeps the value, drops the actions and refocuses the control', async () => {
    const user = userEvent.setup();
    render(<Company />);
    await user.click(screen.getByRole('button', { name: 'Accept suggested company' }));
    expect(screen.getByText('AI filled')).toBeInTheDocument();
    expect(screen.queryByText('Needs check')).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /Accept/ })).not.toBeInTheDocument();
    expect(screen.getByRole('textbox', { name: 'Company' })).toHaveFocus();
  });

  it('clear empties the field and removes the AI marking', async () => {
    const user = userEvent.setup();
    render(<Company />);
    await user.click(screen.getByRole('button', { name: 'Clear suggested company' }));
    const input = screen.getByRole('textbox', { name: 'Company' });
    expect(input).toHaveValue('');
    expect(input).toHaveFocus();
    expect(screen.queryByText('AI suggested')).not.toBeInTheDocument();
    expect(input).not.toHaveAttribute(
      'aria-describedby',
      expect.stringContaining('mn-ai-autofill'),
    );
  });

  it('typing makes it "Edited by you" and shows the original suggestion', async () => {
    const user = userEvent.setup();
    const onEdit = vi.fn();
    function Edited() {
      const [state, setState] = useState<AiAutofillState>('suggested');
      return (
        <Company
          state={state}
          onEdit={() => {
            onEdit();
            setState('edited');
          }}
        />
      );
    }
    render(<Edited />);
    await user.type(screen.getByRole('textbox', { name: 'Company' }), ' Group');
    expect(onEdit).toHaveBeenCalledTimes(1);
    expect(screen.getByText('Edited by you')).toBeInTheDocument();
    expect(screen.getByText('AI suggested:')).toBeInTheDocument();
    expect(screen.getByRole('textbox')).toHaveValue('Northwind Ltd Group');
  });

  it('passes aria-describedby to a render-function child', () => {
    render(
      <AiAutofillField value="42" state="accepted" onAccept={() => {}} onClear={() => {}}>
        {(control) => (
          <>
            <label htmlFor="qty">Quantity</label>
            <input id="qty" {...control} />
          </>
        )}
      </AiAutofillField>,
    );
    expect(screen.getByRole('textbox', { name: 'Quantity' })).toHaveAccessibleDescription(
      'AI filled',
    );
    // Without `label`, the actions keep their visible names.
    expect(screen.queryByRole('button')).not.toBeInTheDocument();
  });

  it('accepts strings', () => {
    render(
      <Company
        messages={{ badge: { suggested: 'KI-Vorschlag' }, accept: 'Übernehmen' }}
        label={undefined}
        confidence={0.9}
      />,
    );
    expect(screen.getByText('KI-Vorschlag')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Übernehmen' })).toBeInTheDocument();
    expect(screen.getByText('High confidence')).toBeInTheDocument();
  });

  it('renders on the server', () => {
    const html = renderToString(<Company />);
    expect(html).toContain('mn-ai-autofill-field');
    expect(html).toContain('AI suggested');
  });
});
