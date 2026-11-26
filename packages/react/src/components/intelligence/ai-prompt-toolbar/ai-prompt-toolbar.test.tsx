import { describe, expect, it, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { createRef, useState } from 'react';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { AiPromptInput } from '../ai-prompt-input/index.js';
import { AiPromptToolbar, AiPromptToolbarButton, AiPromptToolbarToggle } from './index.js';

const Icon = () => <svg width={16} height={16} aria-hidden />;

describe('AiPromptToolbar', () => {
  it('is a named group that forwards props and ref', async () => {
    const ref = createRef<HTMLDivElement>();
    render(
      <AiPromptToolbar ref={ref} data-testid="root" className="extra">
        <AiPromptToolbarButton icon={<Icon />}>Attach</AiPromptToolbarButton>
      </AiPromptToolbar>,
    );
    const root = screen.getByTestId('root');
    expect(root).toHaveClass('mn-ai-prompt-toolbar', 'extra');
    expect(ref.current).toBe(root);
    expect(screen.getByRole('group', { name: 'Prompt options' })).toBe(root);
    await expectNoA11yViolations();
  });

  it('takes a custom label', () => {
    render(<AiPromptToolbar label="Options" />);
    expect(screen.getByRole('group', { name: 'Options' })).toBeInTheDocument();
  });
});

describe('AiPromptToolbarButton', () => {
  it('shows its label, or hides it and keeps it as name and tooltip', async () => {
    const onClick = vi.fn();
    const ref = createRef<HTMLButtonElement>();
    render(
      <AiPromptToolbar>
        <AiPromptToolbarButton icon={<Icon />} onClick={onClick}>
          Templates
        </AiPromptToolbarButton>
        <AiPromptToolbarButton ref={ref} icon={<Icon />} hideLabel>
          Voice
        </AiPromptToolbarButton>
      </AiPromptToolbar>,
    );
    const templates = screen.getByRole('button', { name: 'Templates' });
    expect(templates).toHaveTextContent('Templates');
    expect(templates).toHaveAttribute('data-variant', 'ghost');
    expect(templates).toHaveAttribute('data-size', 'sm');
    await userEvent.click(templates);
    expect(onClick).toHaveBeenCalledTimes(1);
    const voice = screen.getByRole('button', { name: 'Voice' });
    expect(ref.current).toBe(voice);
    expect(voice).toHaveTextContent('');
    expect(voice).toHaveAttribute('title', 'Voice');
    expect(voice).toHaveAttribute('data-icon-only');
    await expectNoA11yViolations();
  });
});

describe('AiPromptToolbarToggle', () => {
  it('toggles aria-pressed from the keyboard when uncontrolled', async () => {
    const user = userEvent.setup();
    const onPressedChange = vi.fn();
    render(
      <AiPromptToolbarToggle icon={<Icon />} onPressedChange={onPressedChange}>
        Search the web
      </AiPromptToolbarToggle>,
    );
    const toggle = screen.getByRole('button', { name: 'Search the web' });
    expect(toggle).toHaveAttribute('aria-pressed', 'false');
    expect(toggle).toHaveAttribute('data-state', 'off');
    await user.tab();
    expect(toggle).toHaveFocus();
    await user.keyboard(' ');
    expect(toggle).toHaveAttribute('aria-pressed', 'true');
    expect(toggle).toHaveAttribute('data-state', 'on');
    expect(onPressedChange).toHaveBeenLastCalledWith(true);
    await user.keyboard('{Enter}');
    expect(toggle).toHaveAttribute('aria-pressed', 'false');
    await expectNoA11yViolations();
  });

  it('follows the controlled value', async () => {
    const onPressedChange = vi.fn();
    render(
      <AiPromptToolbarToggle pressed onPressedChange={onPressedChange}>
        Deep research
      </AiPromptToolbarToggle>,
    );
    const toggle = screen.getByRole('button', { name: 'Deep research' });
    await userEvent.click(toggle);
    expect(onPressedChange).toHaveBeenCalledWith(false);
    expect(toggle).toHaveAttribute('aria-pressed', 'true');
  });

  it('works inside AiPromptInput’s toolbar slot', async () => {
    const user = userEvent.setup();
    const onSubmit = vi.fn();
    function Demo() {
      const [web, setWeb] = useState(false);
      return (
        <AiPromptInput
          label="Message"
          onSubmit={(text) => onSubmit(text, web)}
          toolbar={
            <AiPromptToolbar>
              <AiPromptToolbarToggle pressed={web} onPressedChange={setWeb}>
                Search the web
              </AiPromptToolbarToggle>
            </AiPromptToolbar>
          }
        />
      );
    }
    render(<Demo />);
    await user.click(screen.getByRole('button', { name: 'Search the web' }));
    await user.click(screen.getByRole('textbox', { name: 'Message' }));
    await user.keyboard('hello{Enter}');
    expect(onSubmit).toHaveBeenCalledWith('hello', true);
    await expectNoA11yViolations();
  });
});

it('renders on the server', () => {
  const html = renderToString(
    <AiPromptToolbar>
      <AiPromptToolbarToggle defaultPressed>Search the web</AiPromptToolbarToggle>
    </AiPromptToolbar>,
  );
  expect(html).toContain('mn-ai-prompt-toolbar');
  expect(html).toContain('aria-pressed="true"');
});
