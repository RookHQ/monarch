import { describe, expect, it, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { createRef } from 'react';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { AiStarterPrompts, type AiStarterPrompt } from './index.js';

const PROMPTS: AiStarterPrompt[] = [
  {
    id: 'sum',
    title: 'Summarise a document',
    prompt: 'Summarise the open document in five bullets.',
  },
  {
    id: 'draft',
    title: 'Draft a reply',
    prompt: 'Draft a short reply to the latest message.',
    icon: <svg data-testid="icon" />,
  },
];

describe('AiStarterPrompts', () => {
  it('renders a named list of buttons named by title and described by prompt', async () => {
    const ref = createRef<HTMLUListElement>();
    render(
      <AiStarterPrompts
        ref={ref}
        data-testid="root"
        className="extra"
        prompts={PROMPTS}
        onSelect={() => {}}
      />,
    );
    const root = screen.getByTestId('root');
    expect(root).toHaveClass('mn-ai-starter-prompts', 'extra');
    expect(root).toHaveAttribute('data-layout', 'grid');
    expect(ref.current).toBe(root);
    expect(screen.getByRole('list', { name: 'Suggested prompts' })).toBe(root);
    expect(screen.getAllByRole('listitem')).toHaveLength(2);
    const button = screen.getByRole('button', { name: 'Summarise a document' });
    expect(button).toHaveAccessibleDescription('Summarise the open document in five bullets.');
    expect(screen.getByTestId('icon').parentElement).toHaveAttribute('aria-hidden', 'true');
    await expectNoA11yViolations();
  });

  it('calls onSelect with the prompt text from the mouse and keyboard', async () => {
    const onSelect = vi.fn();
    const user = userEvent.setup();
    render(<AiStarterPrompts prompts={PROMPTS} onSelect={onSelect} layout="list" />);
    await user.click(screen.getByRole('button', { name: 'Summarise a document' }));
    expect(onSelect).toHaveBeenLastCalledWith(PROMPTS[0]!.prompt, PROMPTS[0]);
    await user.tab();
    await user.keyboard('{Enter}');
    expect(onSelect).toHaveBeenLastCalledWith(PROMPTS[1]!.prompt, PROMPTS[1]);
  });

  it('can hide the prompt, be disabled and be localised', () => {
    render(
      <AiStarterPrompts
        prompts={PROMPTS}
        onSelect={() => {}}
        hidePrompt
        disabled
        messages={{ label: 'Vorschläge' }}
      />,
    );
    expect(screen.getByRole('list', { name: 'Vorschläge' })).toBeInTheDocument();
    const button = screen.getByRole('button', { name: 'Draft a reply' });
    expect(button).toBeDisabled();
    expect(button).not.toHaveAccessibleDescription();
    expect(screen.queryByText(PROMPTS[1]!.prompt)).toBeNull();
  });

  it('renders nothing without prompts', () => {
    const { container } = render(<AiStarterPrompts prompts={[]} onSelect={() => {}} />);
    expect(container).toBeEmptyDOMElement();
  });

  it('renders on the server', () => {
    const html = renderToString(<AiStarterPrompts prompts={PROMPTS} onSelect={() => {}} />);
    expect(html).toContain('mn-ai-starter-prompts');
    expect(html).toContain('Draft a reply');
  });
});
