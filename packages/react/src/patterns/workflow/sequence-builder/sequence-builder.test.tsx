import { describe, expect, it, vi } from 'vitest';
import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { useState } from 'react';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import {
  SequenceBuilder,
  findTemplateVariables,
  renderSequenceTemplate,
  validateSequence,
  type SequenceBuilderProps,
  type SequenceChannel,
  type SequenceStep,
  type SequenceVariable,
} from './index.js';

const CHANNELS: SequenceChannel[] = [
  { value: 'email', label: 'Email', hasSubject: true },
  { value: 'chat', label: 'Chat message', maxLength: 20 },
];
const VARIABLES: SequenceVariable[] = [
  { token: 'firstName', label: 'First name', sample: 'Sam' },
  { token: 'product.name', label: 'Product name', sample: 'Atlas' },
];
const START: SequenceStep[] = [
  {
    id: 'a',
    channel: 'email',
    delayMinutes: 0,
    subject: 'Hello {{firstName}}',
    body: 'Meet {{product.name}}.',
  },
  { id: 'b', channel: 'chat', delayMinutes: 2880, body: 'Any thoughts?' },
];

function Harness({
  initial = START,
  onSteps,
  ...props
}: Partial<SequenceBuilderProps> & {
  initial?: SequenceStep[];
  onSteps?: (s: SequenceStep[]) => void;
}) {
  const [steps, setSteps] = useState(initial);
  return (
    <SequenceBuilder
      channels={CHANNELS}
      variables={VARIABLES}
      {...props}
      value={steps}
      onChange={(s) => {
        setSteps(s);
        onSteps?.(s);
      }}
    />
  );
}

describe('SequenceBuilder', () => {
  it('renders steps with wait, subject and message, without axe violations', async () => {
    render(<Harness />);
    expect(screen.getByRole('list', { name: 'Sequence steps' })).toBeInTheDocument();
    const s1 = screen.getByRole('group', { name: 'Step 1, Email' });
    expect(s1).toHaveTextContent('Sent straight away');
    expect(within(s1).getByRole('textbox', { name: /Subject/ })).toHaveValue('Hello {{firstName}}');
    const s2 = screen.getByRole('group', { name: 'Step 2, Chat message' });
    expect(s2).toHaveTextContent('Sent 2d after the previous step');
    expect(within(s2).queryByRole('textbox', { name: /Subject/ })).toBeNull();
    expect(within(s2).getByRole('spinbutton', { name: 'Wait before sending' })).toBeInTheDocument();
    await expectNoA11yViolations();
  });

  it('inserts a variable where the caret is', async () => {
    const user = userEvent.setup();
    const onSteps = vi.fn();
    render(
      <Harness
        onSteps={onSteps}
        initial={[{ id: 'a', channel: 'chat', delayMinutes: 0, body: 'Hi !' }]}
      />,
    );
    const body = screen.getByRole<HTMLTextAreaElement>('textbox', { name: /Message/ });
    await user.click(body);
    body.setSelectionRange(3, 3);
    body.dispatchEvent(new Event('select', { bubbles: true }));
    await user.click(screen.getByRole('button', { name: 'Insert First name' }));
    expect(onSteps).toHaveBeenLastCalledWith([
      { id: 'a', channel: 'chat', delayMinutes: 0, body: 'Hi {{firstName}}!' },
    ]);
    await waitFor(() => expect(body).toHaveFocus());
  });

  it('adds steps per channel, removes and reorders them', async () => {
    const user = userEvent.setup();
    const onSteps = vi.fn();
    render(<Harness onSteps={onSteps} />);
    await user.click(screen.getByRole('button', { name: 'Add Email step' }));
    expect(onSteps.mock.lastCall?.[0][2]).toMatchObject({
      channel: 'email',
      delayMinutes: 1440,
      subject: '',
      body: '',
    });
    await user.click(screen.getByRole('button', { name: 'Remove step 3' }));
    await user.click(screen.getByRole('button', { name: 'Move Step 2, Chat message up' }));
    expect(onSteps.mock.lastCall?.[0].map((s: SequenceStep) => s.id)).toEqual(['b', 'a']);
  });

  it('edits the wait as a duration', async () => {
    const user = userEvent.setup();
    const onSteps = vi.fn();
    render(<Harness onSteps={onSteps} />);
    const wait = within(screen.getByRole('group', { name: 'Step 2, Chat message' })).getByRole(
      'spinbutton',
      {
        name: 'Wait before sending',
      },
    );
    await user.clear(wait);
    await user.type(wait, '4h{Enter}');
    expect(onSteps.mock.lastCall?.[0][1].delayMinutes).toBe(240);
  });

  it('shows problems and unknown variables', () => {
    render(
      <SequenceBuilder
        value={[
          { id: 'x', channel: 'email', delayMinutes: 0, subject: '', body: 'Hi' },
          { id: 'y', channel: 'chat', delayMinutes: 60, body: 'Hello {{lastName}}' },
        ]}
        onChange={() => {}}
        channels={CHANNELS}
        variables={VARIABLES}
        showErrors
      />,
    );
    expect(screen.getByRole('textbox', { name: /Subject/ })).toHaveAccessibleDescription(
      'Write a subject line.',
    );
    expect(screen.getByText('Unknown variables: lastName')).toBeInTheDocument();
  });

  it('previews messages with sample values', async () => {
    const user = userEvent.setup();
    render(<Harness />);
    const s1 = screen.getByRole('group', { name: 'Step 1, Email' });
    await user.click(within(s1).getByText('Preview with sample data'));
    expect(within(s1).getByText('Hello Sam')).toBeVisible();
    expect(within(s1).getByText('Meet Atlas.')).toBeVisible();
  });

  it('helpers find, render and validate', () => {
    expect(findTemplateVariables('{{a}} {{ b.c }} {{a}}')).toEqual(['a', 'b.c']);
    expect(renderSequenceTemplate('Hi {{a}} {{z}}', { a: 'Jo' })).toBe('Hi Jo {{z}}');
    expect(validateSequence(START, CHANNELS, VARIABLES)).toEqual({});
    expect(
      validateSequence(
        [{ id: 'c', channel: 'chat', delayMinutes: 0, body: 'x'.repeat(21) }],
        CHANNELS,
      ),
    ).toEqual({ c: 'length' });
  });

  it('empty state and server rendering', () => {
    render(<SequenceBuilder value={[]} onChange={() => {}} channels={CHANNELS} />);
    expect(screen.getByText('No steps yet')).toBeInTheDocument();
    expect(
      renderToString(<SequenceBuilder value={START} onChange={() => {}} channels={CHANNELS} />),
    ).toContain('mn-sequence-builder');
  });
});
