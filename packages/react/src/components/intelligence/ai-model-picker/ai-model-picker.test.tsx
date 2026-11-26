import { describe, expect, it, vi } from 'vitest';
import { createRef, useState } from 'react';
import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { AiModelPicker, type AiModelOption, type AiModelPickerProps } from './index.js';

const models: AiModelOption[] = [
  {
    id: 'swift',
    provider: 'Provider A',
    name: 'Swift',
    capabilities: ['tools'],
    contextTokens: 128000,
    speed: 'fast',
    costTier: 1,
  },
  {
    id: 'deep',
    provider: 'Provider A',
    name: 'Deep',
    description: 'Best for long documents',
    capabilities: ['vision', 'tools', 'reasoning'],
    contextTokens: 200000,
    speed: 'slow',
    costTier: 3,
  },
  {
    id: 'own',
    provider: 'Provider B',
    name: 'Own key model',
    capabilities: ['web'],
    requiresKey: true,
    keyConfigured: false,
  },
  {
    id: 'byo',
    provider: 'Provider B',
    name: 'Configured',
    capabilities: [],
    requiresKey: true,
    keyConfigured: true,
  },
];

function Controlled(props: Partial<AiModelPickerProps>) {
  const [value, setValue] = useState<string | undefined>('swift');
  return (
    <AiModelPicker
      models={models}
      value={value}
      {...props}
      onChange={(id, model) => {
        setValue(id);
        props.onChange?.(id, model);
      }}
    />
  );
}

describe('AiModelPicker', () => {
  it('shows the current model on a labelled trigger, forwarding ref and props', async () => {
    const ref = createRef<HTMLButtonElement>();
    render(
      <AiModelPicker
        ref={ref}
        data-testid="root"
        models={models}
        value="deep"
        onChange={() => {}}
      />,
    );
    const trigger = screen.getByRole('button', { name: 'Model Deep Provider A' });
    expect(trigger).toBe(screen.getByTestId('root'));
    expect(trigger).toHaveClass('mn-ai-model-picker');
    expect(ref.current).toBe(trigger);
    expect(trigger).toHaveAttribute('aria-haspopup', 'listbox');
    expect(trigger).toHaveAttribute('aria-expanded', 'false');
    await expectNoA11yViolations();
  });

  it('opens a grouped listbox with capabilities, context, speed, cost and key state', async () => {
    render(<Controlled />);
    await userEvent.click(screen.getByRole('button', { name: /Model/ }));
    const list = screen.getByRole('listbox', { name: 'Model' });
    expect(list).toHaveFocus();
    expect(within(list).getByRole('group', { name: 'Provider A' })).toBeInTheDocument();
    expect(within(list).getByRole('group', { name: 'Provider B' })).toBeInTheDocument();
    const deep = screen.getByRole('option', { name: /Deep/ });
    expect(deep).toHaveAccessibleDescription(
      'Best for long documents Vision Tools Reasoning 200K context · Slower · Cost: high',
    );
    expect(screen.getByRole('option', { name: /Swift/ })).toHaveAttribute('aria-selected', 'true');
    expect(list).toHaveAttribute(
      'aria-activedescendant',
      screen.getByRole('option', { name: /Swift/ }).id,
    );
    expect(screen.getByRole('option', { name: /Own key model/ })).toHaveAttribute(
      'aria-disabled',
      'true',
    );
    expect(screen.getByRole('option', { name: 'Configured Your key' })).toBeInTheDocument();
    expect(screen.getByRole('option', { name: 'Own key model Key needed' })).toBeInTheDocument();
    await expectNoA11yViolations();
  });

  it('moves with arrows, Home, End and typeahead, skips nothing, and picks with Enter', async () => {
    const user = userEvent.setup();
    const onChange = vi.fn();
    render(<Controlled onChange={onChange} />);
    const trigger = screen.getByRole('button', { name: /Model/ });
    await user.click(trigger);
    const list = screen.getByRole('listbox');
    const activeName = () =>
      document.getElementById(list.getAttribute('aria-activedescendant') ?? '')?.textContent;
    await user.keyboard('{ArrowDown}');
    expect(activeName()).toMatch(/^Deep/);
    await user.keyboard('{End}');
    expect(activeName()).toMatch(/^Configured/);
    await user.keyboard('{Home}');
    expect(activeName()).toMatch(/^Swift/);
    await user.keyboard('d');
    expect(activeName()).toMatch(/^Deep/);
    await user.keyboard('{Enter}');
    expect(onChange).toHaveBeenCalledWith('deep', models[1]);
    expect(screen.queryByRole('listbox')).toBeNull();
    expect(trigger).toHaveFocus();
    expect(trigger).toHaveAccessibleName('Model Deep Provider A');
  });

  it('does not pick a model with a missing key; routes it to onConfigureKey when given', async () => {
    const user = userEvent.setup();
    const onChange = vi.fn();
    const onConfigureKey = vi.fn();
    const { rerender } = render(<Controlled onChange={onChange} />);
    await user.click(screen.getByRole('button', { name: /Model/ }));
    await user.click(screen.getByRole('option', { name: /Own key model/ }));
    expect(onChange).not.toHaveBeenCalled();
    expect(screen.getByRole('listbox')).toBeInTheDocument();
    await user.keyboard('{Escape}');
    rerender(<Controlled onChange={onChange} onConfigureKey={onConfigureKey} />);
    await user.click(screen.getByRole('button', { name: /Model/ }));
    const own = screen.getByRole('option', { name: /Own key model/ });
    expect(own).not.toHaveAttribute('aria-disabled');
    expect(own).toHaveAccessibleDescription(/Select to add an API key\./);
    await user.click(own);
    expect(onConfigureKey).toHaveBeenCalledWith(models[2]);
    expect(onChange).not.toHaveBeenCalled();
  });

  it('opens from the trigger with ArrowDown', async () => {
    const user = userEvent.setup();
    render(<Controlled />);
    screen.getByRole('button', { name: /Model/ }).focus();
    await user.keyboard('{ArrowDown}');
    expect(screen.getByRole('listbox')).toHaveFocus();
  });

  it('closes on Escape and returns focus to the trigger', async () => {
    const user = userEvent.setup();
    const onOpenChange = vi.fn();
    render(<Controlled onOpenChange={onOpenChange} />);
    const trigger = screen.getByRole('button', { name: /Model/ });
    await user.click(trigger);
    await user.keyboard('{Escape}');
    expect(screen.queryByRole('listbox')).toBeNull();
    expect(trigger).toHaveFocus();
    expect(onOpenChange.mock.calls).toEqual([[true], [false]]);
  });

  it('shows a placeholder and localised strings', () => {
    render(
      <AiModelPicker
        models={models}
        value={undefined}
        onChange={() => {}}
        messages={{ label: 'Modèle', placeholder: 'Choisir' }}
      />,
    );
    expect(screen.getByRole('button', { name: 'Modèle Choisir' })).toBeInTheDocument();
  });

  it('renders on the server', () => {
    const html = renderToString(
      <AiModelPicker models={models} value="swift" onChange={() => {}} />,
    );
    expect(html).toContain('mn-ai-model-picker');
    expect(html).toContain('Swift');
  });
});
