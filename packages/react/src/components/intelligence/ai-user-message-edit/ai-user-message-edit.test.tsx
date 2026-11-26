import { describe, expect, it, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { createRef } from 'react';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { AiUserMessageEdit } from './index.js';

describe('AiUserMessageEdit', () => {
  it('renders a named form with a focused field, the branch hint and actions', async () => {
    const ref = createRef<HTMLFormElement>();
    render(
      <AiUserMessageEdit
        ref={ref}
        data-testid="root"
        className="extra"
        defaultValue="Summarise the plan"
        onSubmit={() => {}}
        onCancel={() => {}}
      />,
    );
    const root = screen.getByTestId('root');
    expect(root).toHaveClass('mn-ai-user-message-edit', 'extra');
    expect(ref.current).toBe(root);
    expect(screen.getByRole('form', { name: 'Edit message' })).toBe(root);
    const field = screen.getByRole('textbox', { name: 'Edit message' });
    expect(field).toHaveValue('Summarise the plan');
    expect(field).toHaveFocus();
    expect((field as HTMLTextAreaElement).selectionStart).toBe('Summarise the plan'.length);
    expect(field).toHaveAccessibleDescription(
      /Editing will create a new branch.*Enter to send, Shift\+Enter for a new line/,
    );
    expect(screen.getByRole('button', { name: 'Send' })).toBeDisabled();
    expect(screen.getByRole('button', { name: 'Cancel' })).toBeEnabled();
    await expectNoA11yViolations();
  });

  it('sends the trimmed text with Enter and adds a line with Shift+Enter', async () => {
    const onSubmit = vi.fn();
    const user = userEvent.setup();
    render(<AiUserMessageEdit defaultValue="Hello" onSubmit={onSubmit} onCancel={() => {}} />);
    await user.keyboard(' there{Shift>}{Enter}{/Shift}again  ');
    expect(screen.getByRole('textbox')).toHaveValue('Hello there\nagain  ');
    expect(onSubmit).not.toHaveBeenCalled();
    await user.keyboard('{Enter}');
    expect(onSubmit).toHaveBeenCalledWith('Hello there\nagain');
  });

  it('sends with the Send button', async () => {
    const onSubmit = vi.fn();
    const user = userEvent.setup();
    render(<AiUserMessageEdit defaultValue="Hello" onSubmit={onSubmit} onCancel={() => {}} />);
    await user.type(screen.getByRole('textbox'), '!');
    await user.click(screen.getByRole('button', { name: 'Send' }));
    expect(onSubmit).toHaveBeenCalledWith('Hello!');
  });

  it('does not send empty or unchanged text', async () => {
    const onSubmit = vi.fn();
    const user = userEvent.setup();
    render(<AiUserMessageEdit defaultValue="Hello" onSubmit={onSubmit} onCancel={() => {}} />);
    await user.keyboard('{Enter}');
    await user.clear(screen.getByRole('textbox'));
    await user.keyboard('   {Enter}');
    expect(onSubmit).not.toHaveBeenCalled();
    expect(screen.getByRole('button', { name: 'Send' })).toBeDisabled();
  });

  it('cancels with Escape without bubbling, and with the Cancel button', async () => {
    const onCancel = vi.fn();
    const outer = vi.fn();
    const user = userEvent.setup();
    render(
      // eslint-disable-next-line jsx-a11y/no-static-element-interactions
      <div onKeyDown={outer}>
        <AiUserMessageEdit defaultValue="Hello" onSubmit={() => {}} onCancel={onCancel} />
      </div>,
    );
    await user.keyboard('{Escape}');
    expect(onCancel).toHaveBeenCalledTimes(1);
    expect(outer).not.toHaveBeenCalledWith(expect.objectContaining({ key: 'Escape' }));
    await user.click(screen.getByRole('button', { name: 'Cancel' }));
    expect(onCancel).toHaveBeenCalledTimes(2);
  });

  it('can skip focus on mount, be disabled and be localised', () => {
    render(
      <AiUserMessageEdit
        defaultValue="Hallo"
        focusOnMount={false}
        disabled
        onSubmit={() => {}}
        onCancel={() => {}}
        messages={{ label: 'Nachricht bearbeiten', submit: 'Senden', branchHint: 'Neuer Zweig.' }}
      />,
    );
    const field = screen.getByRole('textbox', { name: 'Nachricht bearbeiten' });
    expect(field).not.toHaveFocus();
    expect(field).toBeDisabled();
    expect(screen.getByRole('button', { name: 'Senden' })).toBeDisabled();
    expect(screen.getByText('Neuer Zweig.')).toBeInTheDocument();
  });

  it('renders on the server', () => {
    const html = renderToString(
      <AiUserMessageEdit defaultValue="Hello" onSubmit={() => {}} onCancel={() => {}} />,
    );
    expect(html).toContain('mn-ai-user-message-edit');
    expect(html).toContain('Hello');
  });
});
