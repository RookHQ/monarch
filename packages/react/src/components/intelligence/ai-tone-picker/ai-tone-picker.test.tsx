import { describe, expect, it, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { createRef, useState } from 'react';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { AI_DEFAULT_TONES, AI_TONE_CUSTOM, AiTonePicker, type AiTonePickerProps } from './index.js';

function Controlled(props: Partial<AiTonePickerProps> & { onTone?: (id: string) => void }) {
  const { onTone, ...rest } = props;
  const [tone, setTone] = useState(props.value ?? 'professional');
  return (
    <AiTonePicker
      {...rest}
      value={tone}
      onChange={(id) => {
        setTone(id);
        onTone?.(id);
      }}
    />
  );
}

describe('AiTonePicker', () => {
  it('shows the default tones as a named radio group with descriptions', async () => {
    const ref = createRef<HTMLDivElement>();
    render(
      <AiTonePicker
        ref={ref}
        data-testid="root"
        className="extra"
        value="technical"
        onChange={() => {}}
      />,
    );
    const root = screen.getByTestId('root');
    expect(root).toHaveClass('mn-ai-tone-picker', 'extra');
    expect(root).toHaveAttribute('data-tone', 'technical');
    expect(ref.current).toBe(root);
    expect(screen.getByRole('group', { name: 'Tone' })).toBeInTheDocument();
    const radios = screen.getAllByRole('radio');
    expect(radios.map((r) => r.getAttribute('value'))).toEqual(AI_DEFAULT_TONES.map((t) => t.id));
    const technical = screen.getByRole('radio', { name: /Technical/ });
    expect(technical).toBeChecked();
    expect(technical).toHaveAccessibleDescription('Precise terms, detail and structure.');
    await expectNoA11yViolations();
  });

  it('selects with the arrow keys and clicks', async () => {
    const user = userEvent.setup();
    const onTone = vi.fn();
    render(<Controlled onTone={onTone} />);
    await user.tab();
    expect(screen.getByRole('radio', { name: /Professional/ })).toHaveFocus();
    await user.keyboard('{ArrowDown}');
    expect(onTone).toHaveBeenLastCalledWith('technical');
    expect(screen.getByRole('radio', { name: /Technical/ })).toBeChecked();
    await user.click(screen.getByText('Executive'));
    expect(onTone).toHaveBeenLastCalledWith('executive');
  });

  it('offers a custom tone with a text field', async () => {
    const user = userEvent.setup();
    const onCustomTextChange = vi.fn();
    render(<Controlled allowCustom onCustomTextChange={onCustomTextChange} />);
    expect(screen.queryByRole('textbox')).toBeNull();
    await user.click(screen.getByRole('radio', { name: /Custom/ }));
    const field = screen.getByRole('textbox', { name: 'Describe the tone' });
    await user.type(field, 'Warm');
    expect(field).toHaveValue('Warm');
    expect(onCustomTextChange).toHaveBeenLastCalledWith('Warm');
    expect(screen.getByRole('radio', { name: /Custom/ })).toHaveAttribute('value', AI_TONE_CUSTOM);
    await expectNoA11yViolations();
  });

  it('takes custom tones, a controlled custom text, disabled and messages', () => {
    render(
      <AiTonePicker
        tones={[{ id: 'plain', label: 'Plain', description: 'Short sentences.' }]}
        value="custom"
        allowCustom
        customText="Brief"
        disabled
        onChange={() => {}}
        messages={{ label: 'Stil', custom: 'Eigener', customField: 'Stil beschreiben' }}
      />,
    );
    expect(screen.getByRole('group', { name: 'Stil' })).toBeDisabled();
    expect(screen.getAllByRole('radio')).toHaveLength(2);
    expect(screen.getByRole('textbox', { name: 'Stil beschreiben' })).toHaveValue('Brief');
  });

  it('has a compact select variant', async () => {
    const user = userEvent.setup();
    const onTone = vi.fn();
    render(<Controlled variant="compact" allowCustom onTone={onTone} />);
    const select = screen.getByRole('combobox', { name: 'Tone' });
    expect(select).toHaveValue('professional');
    await user.selectOptions(select, 'casual');
    expect(onTone).toHaveBeenLastCalledWith('casual');
    await user.selectOptions(select, AI_TONE_CUSTOM);
    expect(screen.getByRole('textbox', { name: 'Describe the tone' })).toBeInTheDocument();
    await expectNoA11yViolations();
  });

  it('renders on the server', () => {
    const html = renderToString(<AiTonePicker value="casual" onChange={() => {}} />);
    expect(html).toContain('mn-ai-tone-picker');
    expect(html).toContain('Friendly and conversational.');
  });
});
