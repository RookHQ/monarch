import { describe, expect, it, vi } from 'vitest';
import { useState } from 'react';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { AiInlinePrompt, type AiInlinePromptProps } from './index.js';

const rect = {
  x: 10,
  y: 10,
  width: 100,
  height: 20,
  top: 10,
  left: 10,
  right: 110,
  bottom: 30,
  toJSON: () => ({}),
} as DOMRect;

function Harness(props: Partial<AiInlinePromptProps>) {
  const [open, setOpen] = useState(false);
  return (
    <>
      <button type="button" onClick={() => setOpen(true)}>
        Edit with AI
      </button>
      <AiInlinePrompt
        anchor={() => rect}
        onSubmit={() => {}}
        {...props}
        open={open}
        onOpenChange={(next) => {
          setOpen(next);
          props.onOpenChange?.(next);
        }}
      />
    </>
  );
}

describe('AiInlinePrompt', () => {
  it('opens as a named dialog with the field focused and the context shown', async () => {
    const user = userEvent.setup();
    render(<Harness context="The quarterly numbers improved." data-testid="root" />);
    await user.click(screen.getByRole('button', { name: 'Edit with AI' }));
    const dialog = screen.getByRole('dialog', { name: 'Ask AI' });
    expect(dialog).toBe(screen.getByTestId('root'));
    expect(dialog).toHaveClass('mn-ai-inline-prompt');
    const field = screen.getByRole('textbox', { name: 'Prompt' });
    expect(field).toHaveFocus();
    expect(field).toHaveAccessibleDescription(
      'Selection: The quarterly numbers improved. Enter to send · Shift+Enter for a new line · Esc to close',
    );
    await expectNoA11yViolations();
  });

  it('submits the trimmed prompt with Enter, keeps Shift+Enter as a new line, and closes', async () => {
    const user = userEvent.setup();
    const onSubmit = vi.fn();
    render(<Harness onSubmit={onSubmit} />);
    const opener = screen.getByRole('button', { name: 'Edit with AI' });
    await user.click(opener);
    const field = screen.getByRole('textbox', { name: 'Prompt' });
    expect(screen.getByRole('button', { name: 'Send' })).toBeDisabled();
    await user.keyboard('{Enter}');
    expect(onSubmit).not.toHaveBeenCalled();
    await user.type(field, '  Make it shorter{Shift>}{Enter}{/Shift}please ');
    expect(field).toHaveValue('  Make it shorter\nplease ');
    await user.keyboard('{Enter}');
    expect(onSubmit).toHaveBeenCalledWith('Make it shorter\nplease');
    expect(screen.queryByRole('dialog')).toBeNull();
    await waitFor(() => expect(opener).toHaveFocus());
  });

  it('closes on Escape and returns focus to the element focused before', async () => {
    const user = userEvent.setup();
    const onOpenChange = vi.fn();
    render(<Harness onOpenChange={onOpenChange} />);
    const opener = screen.getByRole('button', { name: 'Edit with AI' });
    await user.click(opener);
    await user.keyboard('{Escape}');
    expect(screen.queryByRole('dialog')).toBeNull();
    expect(onOpenChange).toHaveBeenLastCalledWith(false);
    await waitFor(() => expect(opener).toHaveFocus());
  });

  it('submits a suggestion, and stays open when closeOnSubmit is false', async () => {
    const user = userEvent.setup();
    const onSubmit = vi.fn();
    render(
      <Harness
        onSubmit={onSubmit}
        suggestions={['Shorten', 'Fix spelling']}
        closeOnSubmit={false}
      />,
    );
    await user.click(screen.getByRole('button', { name: 'Edit with AI' }));
    expect(screen.getByRole('group', { name: 'Suggestions' })).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Fix spelling' }));
    expect(onSubmit).toHaveBeenCalledWith('Fix spelling');
    expect(screen.getByRole('dialog')).toBeInTheDocument();
  });

  it('disables input while pending and starts from defaultValue', async () => {
    const user = userEvent.setup();
    render(<Harness pending defaultValue="Translate" messages={{ inputLabel: 'Consigne' }} />);
    await user.click(screen.getByRole('button', { name: 'Edit with AI' }));
    expect(screen.getByRole('textbox', { name: 'Consigne' })).toHaveValue('Translate');
    expect(screen.getByRole('textbox', { name: 'Consigne' })).toBeDisabled();
  });

  it('renders nothing on the server while closed', () => {
    const html = renderToString(
      <AiInlinePrompt anchor={rect} open={false} onOpenChange={() => {}} onSubmit={() => {}} />,
    );
    expect(html).toBe('');
  });
});
