import { describe, expect, it, vi } from 'vitest';
import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { createRef } from 'react';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import {
  AiPromptTemplatePicker,
  extractPromptVariables,
  fillPromptTemplate,
  promptTemplateVariables,
  type AiPromptTemplate,
} from './index.js';

const TEMPLATES: AiPromptTemplate[] = [
  {
    id: 'reply',
    title: 'Customer reply',
    description: 'Answer a customer question',
    body: 'Write a reply to {{customer}} about {{ topic }}. Keep it {{length}}.',
    variables: [
      { name: 'customer', label: 'Customer name', required: true },
      { name: 'topic', label: 'Topic', required: true, multiline: true },
      { name: 'length', label: 'Length', placeholder: 'short', defaultValue: 'short' },
    ],
  },
  {
    id: 'summary',
    title: 'Meeting summary',
    body: 'Summarize {{meeting_notes}} in five bullet points.',
  },
  { id: 'plain', title: 'Plain recap', body: 'Recap this conversation.' },
];

describe('template helpers', () => {
  it('extracts distinct variables in order', () => {
    expect(extractPromptVariables('{{a}} {{ b }} {{a}} {{c.d}} {x}')).toEqual(['a', 'b', 'c.d']);
  });

  it('fills values, keeps missing blanks and removes empty ones', () => {
    expect(fillPromptTemplate('Hi {{ name }}, {{x}}!{{y}}', { name: 'Ana', y: '' })).toBe(
      'Hi Ana, {{x}}!',
    );
    // Values are inserted literally, never re-parsed.
    expect(fillPromptTemplate('{{a}}', { a: '{{b}} $&' })).toBe('{{b}} $&');
  });

  it('reads variables from the body when none are given', () => {
    expect(promptTemplateVariables(TEMPLATES[1] as AiPromptTemplate)).toEqual([
      { name: 'meeting_notes', label: 'Meeting notes', required: true },
    ]);
  });
});

describe('AiPromptTemplatePicker', () => {
  it('shows a named gallery and a preview with blanks', async () => {
    const ref = createRef<HTMLDivElement>();
    const user = userEvent.setup();
    render(
      <AiPromptTemplatePicker
        ref={ref}
        data-testid="root"
        className="extra"
        templates={TEMPLATES}
        onUse={() => {}}
      />,
    );
    const root = screen.getByTestId('root');
    expect(root).toHaveClass('mn-ai-prompt-template-picker', 'extra');
    expect(root).toHaveAttribute('data-step', 'pick');
    expect(ref.current).toBe(root);
    expect(screen.getByRole('radiogroup', { name: 'Prompt templates' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Continue' })).toBeDisabled();
    expect(root).toHaveTextContent('Choose a template to continue.');
    await user.click(screen.getByRole('radio', { name: 'Customer reply' }));
    const preview = screen.getByRole('region', { name: 'Template preview' });
    expect(preview).toHaveTextContent(
      'Write a reply to Customer name about Topic. Keep it Length.',
    );
    expect(screen.getByRole('button', { name: 'Continue' })).toBeEnabled();
    await expectNoA11yViolations();
  });

  it('fills the blanks, validates required ones and uses the filled text', async () => {
    const user = userEvent.setup();
    const onUse = vi.fn();
    render(<AiPromptTemplatePicker templates={TEMPLATES} onUse={onUse} />);
    await user.click(screen.getByRole('radio', { name: 'Customer reply' }));
    await user.click(screen.getByRole('button', { name: 'Continue' }));
    const heading = screen.getByRole('heading', { level: 3, name: 'Fill in “Customer reply”' });
    expect(heading).toHaveFocus();
    expect(screen.getByRole('textbox', { name: /Length/ })).toHaveValue('short');
    expect(screen.getByText('Optional')).toBeInTheDocument();
    await expectNoA11yViolations();

    await user.click(screen.getByRole('button', { name: 'Use template' }));
    expect(onUse).not.toHaveBeenCalled();
    const customer = screen.getByRole('textbox', { name: /Customer name/ });
    expect(customer).toHaveFocus();
    expect(customer).toHaveAttribute('aria-invalid', 'true');
    expect(customer).toHaveAccessibleDescription('Enter customer name.');

    await user.type(customer, 'Ana');
    expect(customer).not.toHaveAttribute('aria-invalid', 'true');
    const topic = screen.getByRole('textbox', { name: /Topic/ });
    expect(topic.tagName).toBe('TEXTAREA');
    await user.type(topic, 'the delayed order');
    expect(screen.getByRole('region', { name: 'Prompt preview' })).toHaveTextContent(
      'Write a reply to Ana about the delayed order. Keep it short.',
    );
    await user.click(screen.getByRole('button', { name: 'Use template' }));
    expect(onUse).toHaveBeenCalledWith(
      'Write a reply to Ana about the delayed order. Keep it short.',
      {
        template: TEMPLATES[0],
        values: { customer: 'Ana', topic: 'the delayed order', length: 'short' },
      },
    );
  });

  it('submits with Enter, goes back with focus on Continue and keeps the selection', async () => {
    const user = userEvent.setup();
    const onUse = vi.fn();
    render(<AiPromptTemplatePicker templates={TEMPLATES} onUse={onUse} />);
    await user.click(screen.getByRole('radio', { name: 'Meeting summary' }));
    await user.click(screen.getByRole('button', { name: 'Continue' }));
    await user.click(screen.getByRole('button', { name: 'Back' }));
    expect(screen.getByRole('button', { name: 'Continue' })).toHaveFocus();
    expect(screen.getByRole('radio', { name: 'Meeting summary' })).toBeChecked();
    await user.keyboard('{Enter}');
    await user.type(screen.getByRole('textbox', { name: /Meeting notes/ }), 'the notes{Enter}');
    expect(onUse).toHaveBeenCalledWith('Summarize the notes in five bullet points.', {
      template: TEMPLATES[1],
      values: { meeting_notes: 'the notes' },
    });
  });

  it('uses a template without blanks directly, and cancels', async () => {
    const user = userEvent.setup();
    const onUse = vi.fn();
    const onCancel = vi.fn();
    render(
      <AiPromptTemplatePicker
        templates={TEMPLATES}
        defaultTemplateId="plain"
        onUse={onUse}
        onCancel={onCancel}
      />,
    );
    await user.click(screen.getByRole('button', { name: 'Use template' }));
    expect(onUse).toHaveBeenCalledWith('Recap this conversation.', {
      template: TEMPLATES[2],
      values: {},
    });
    await user.click(screen.getByRole('button', { name: 'Cancel' }));
    expect(onCancel).toHaveBeenCalled();
  });

  it('filters by search and shows an empty state', async () => {
    const user = userEvent.setup();
    const { unmount } = render(<AiPromptTemplatePicker templates={TEMPLATES} onUse={() => {}} />);
    await user.type(screen.getByRole('searchbox', { name: 'Search templates' }), 'meeting');
    const group = screen.getByRole('radiogroup');
    expect(within(group).getAllByRole('radio')).toHaveLength(1);
    unmount();
    render(<AiPromptTemplatePicker templates={[]} onUse={() => {}} />);
    expect(screen.getByText('No templates yet.')).toBeInTheDocument();
  });

  it('renders on the server', () => {
    const html = renderToString(
      <AiPromptTemplatePicker templates={TEMPLATES} defaultTemplateId="reply" onUse={() => {}} />,
    );
    expect(html).toContain('mn-ai-prompt-template-picker');
    expect(html).toContain('Customer reply');
  });
});
