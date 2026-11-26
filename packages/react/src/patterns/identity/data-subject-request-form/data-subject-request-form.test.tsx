import { describe, expect, it, vi } from 'vitest';
import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { DataSubjectRequestForm, type DataSubjectRequestFormProps } from './index.js';

const countries = [
  { code: 'DE', label: 'Germany' },
  { code: 'US-CA', label: 'United States – California' },
];

function Form(props: Partial<DataSubjectRequestFormProps>) {
  return <DataSubjectRequestForm countries={countries} onSubmit={() => 'REQ-1'} {...props} />;
}

type User = ReturnType<typeof userEvent.setup>;

async function fillValid(user: User) {
  await user.click(screen.getByRole('radio', { name: /Get a copy of my data/ }));
  await user.type(screen.getByRole('textbox', { name: 'Email' }), 'alex@example.com');
  await user.selectOptions(
    screen.getByRole('combobox', { name: 'Country or region where the person lives' }),
    'DE',
  );
  await user.click(screen.getByRole('checkbox', { name: 'The information I gave is accurate' }));
}

const submit = () => screen.getByRole('button', { name: 'Send request' });

describe('DataSubjectRequestForm', () => {
  it('renders the default types, fields and labels', async () => {
    render(<Form verificationNote="We will email you a link to confirm it is you." />);
    const types = screen.getByRole('group', { name: 'What would you like to do?' });
    expect(within(types).getAllByRole('radio')).toHaveLength(6);
    for (const radio of within(types).getAllByRole('radio')) expect(radio).not.toBeChecked();
    expect(
      screen.getByRole('radio', { name: 'Opt out of sale or sharing' }),
    ).toHaveAccessibleDescription(
      'Stop your information from being sold or shared for advertising.',
    );
    expect(screen.getByRole('radio', { name: 'Myself' })).toBeChecked();
    expect(screen.getByRole('textbox', { name: /Full name/ })).not.toBeRequired();
    expect(screen.getByRole('textbox', { name: 'Email' })).toBeRequired();
    expect(screen.getByRole('textbox', { name: /Details/ })).toBeInTheDocument();
    expect(screen.getByText('We will email you a link to confirm it is you.')).toBeInTheDocument();
    expect(screen.queryByRole('group', { name: 'Your details as their agent' })).toBeNull();
    await expectNoA11yViolations();
  });

  it('validates on submit, summarises the errors and focuses the first invalid field', async () => {
    const user = userEvent.setup();
    const onSubmit = vi.fn();
    render(<Form onSubmit={onSubmit} />);
    await user.click(submit());
    expect(onSubmit).not.toHaveBeenCalled();
    const summary = screen.getByRole('alert');
    expect(summary).toHaveTextContent('There are problems with 4 answers');
    expect(
      within(summary)
        .getAllByRole('link')
        .map((l) => l.textContent),
    ).toEqual([
      'Choose what you would like to do.',
      'Enter an email address.',
      'Choose a country or region.',
      'Confirm that the information is accurate.',
    ]);
    expect(screen.getAllByRole('radio')[0]).toHaveFocus();
    expect(
      screen.getByRole('group', { name: 'What would you like to do?' }),
    ).toHaveAccessibleDescription('Choose what you would like to do.');
    const email = screen.getByRole('textbox', { name: 'Email' });
    expect(email).toHaveAttribute('aria-invalid', 'true');
    expect(email).toHaveAccessibleDescription(
      'Used to identify the account and to reply about this request. Enter an email address.',
    );
    await expectNoA11yViolations();

    // Summary links move focus to their field.
    await user.click(within(summary).getByRole('link', { name: 'Choose a country or region.' }));
    expect(screen.getByRole('combobox', { name: /Country or region/ })).toHaveFocus();

    // Fixing a field removes it from the summary; the next submit focuses the next problem.
    await user.click(screen.getByRole('radio', { name: /Delete my data/ }));
    await user.type(email, 'not-an-email');
    expect(screen.getByRole('alert')).not.toHaveTextContent('Choose what you would like to do.');
    await user.click(submit());
    expect(email).toHaveFocus();
    expect(screen.getByRole('alert')).toHaveTextContent(
      'Enter an email address like name@example.com.',
    );
  });

  it('requires details for a type that needs them', async () => {
    const user = userEvent.setup();
    const onSubmit = vi.fn();
    render(<Form onSubmit={onSubmit} />);
    await fillValid(user);
    await user.click(screen.getByRole('radio', { name: /Correct my data/ }));
    const details = screen.getByRole('textbox', { name: /Details/ });
    expect(details).toBeRequired();
    await user.click(submit());
    expect(onSubmit).not.toHaveBeenCalled();
    expect(details).toHaveFocus();
    expect(screen.getByRole('alert')).toHaveTextContent('Describe what the request is about.');
  });

  it('shows agent fields for an authorized agent and sends them', async () => {
    const user = userEvent.setup();
    const onSubmit = vi.fn(() => ({ referenceId: 'REQ-7' }));
    render(<Form onSubmit={onSubmit} />);
    await user.click(screen.getByRole('radio', { name: 'Someone I am authorized to act for' }));
    const agent = screen.getByRole('group', { name: 'Your details as their agent' });
    expect(agent).toHaveTextContent('You may be asked for proof that you can act for this person.');
    expect(screen.getByRole('textbox', { name: 'Their email' })).toBeInTheDocument();
    await user.type(screen.getByRole('textbox', { name: 'Their email' }), 'sam@example.com');
    await user.click(screen.getByRole('radio', { name: /Get a copy of my data/ }));
    await user.selectOptions(screen.getByRole('combobox'), 'US-CA');
    await user.click(screen.getByRole('checkbox'));
    await user.click(submit());
    expect(onSubmit).not.toHaveBeenCalled();
    expect(within(agent).getByRole('textbox', { name: 'Your full name' })).toHaveFocus();
    await user.type(within(agent).getByRole('textbox', { name: 'Your full name' }), ' Jo Agent ');
    await user.type(within(agent).getByRole('textbox', { name: 'Your email' }), 'jo@example.com');
    await expectNoA11yViolations();
    await user.click(submit());
    expect(onSubmit).toHaveBeenCalledWith({
      type: 'access',
      name: '',
      email: 'sam@example.com',
      relationship: 'authorized-agent',
      agent: { name: 'Jo Agent', email: 'jo@example.com' },
      country: 'US-CA',
      details: '',
      acknowledged: true,
    });
  });

  it('hides agent fields again when switching back to self', async () => {
    const user = userEvent.setup();
    render(<Form />);
    await user.click(screen.getByRole('radio', { name: 'Someone I am authorized to act for' }));
    expect(screen.getByRole('group', { name: 'Your details as their agent' })).toBeInTheDocument();
    await user.click(screen.getByRole('radio', { name: 'Myself' }));
    expect(screen.queryByRole('group', { name: 'Your details as their agent' })).toBeNull();
    expect(screen.getByRole('textbox', { name: 'Email' })).toBeInTheDocument();
  });

  it('is busy while sending, shows a rejection and keeps the answers', async () => {
    const user = userEvent.setup();
    let reject!: (error: Error) => void;
    const onSubmit = vi.fn(
      () =>
        new Promise((_, r) => {
          reject = r;
        }),
    );
    render(<Form onSubmit={onSubmit} />);
    await fillValid(user);
    await user.click(submit());
    expect(submit()).toHaveAttribute('aria-busy', 'true');
    await user.click(submit());
    expect(onSubmit).toHaveBeenCalledOnce();
    reject(new Error('The service is unavailable. Try again later.'));
    expect(await screen.findByRole('alert')).toHaveTextContent(
      'The service is unavailable. Try again later.',
    );
    expect(submit()).not.toHaveAttribute('aria-busy');
    expect(screen.getByRole('textbox', { name: 'Email' })).toHaveValue('alex@example.com');
  });

  it('shows the reference id and response note on success, with focus on the heading', async () => {
    const user = userEvent.setup();
    const onSubmit = vi.fn(() => Promise.resolve('REQ-2026-0042'));
    render(
      <Form
        onSubmit={onSubmit}
        responseTimeNote="We will reply by email within the time your local rules allow."
        defaultValues={{ email: 'alex@example.com', country: 'DE', type: 'delete' }}
      />,
    );
    expect(screen.getByRole('radio', { name: /Delete my data/ })).toBeChecked();
    await user.click(screen.getByRole('checkbox'));
    await user.click(submit());
    expect(onSubmit).toHaveBeenCalledWith(
      expect.objectContaining({ type: 'delete', email: 'alex@example.com', country: 'DE' }),
    );
    const heading = await screen.findByRole('heading', { name: 'Request received', level: 2 });
    expect(heading).toHaveFocus();
    expect(screen.getByText('REQ-2026-0042')).toBeInTheDocument();
    expect(
      screen.getByText('We will reply by email within the time your local rules allow.'),
    ).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Send request' })).toBeNull();
    await expectNoA11yViolations();
  });

  it('uses a text field without countries, custom types and messages', async () => {
    const user = userEvent.setup();
    const onSubmit = vi.fn();
    render(
      <DataSubjectRequestForm
        onSubmit={onSubmit}
        types={[
          { id: 'portability', label: 'Move my data', description: 'Get it in a portable format.' },
        ]}
        messages={{ submit: 'Submit', countryLabel: 'Where you live' }}
      />,
    );
    await user.click(screen.getByRole('radio', { name: /Move my data/ }));
    await user.type(screen.getByRole('textbox', { name: 'Email' }), 'a@example.com');
    await user.type(screen.getByRole('textbox', { name: 'Where you live' }), 'Ontario, Canada');
    await user.click(screen.getByRole('checkbox'));
    await user.click(screen.getByRole('button', { name: 'Submit' }));
    expect(onSubmit).toHaveBeenCalledWith(
      expect.objectContaining({ type: 'portability', country: 'Ontario, Canada' }),
    );
    // A void result still shows success, without a reference line.
    expect(await screen.findByRole('heading', { name: 'Request received' })).toBeInTheDocument();
    expect(screen.queryByText('Your reference')).toBeNull();
  });

  it('renders on the server', () => {
    const html = renderToString(<Form />);
    expect(html).toContain('mn-data-subject-request-form');
    expect(html).toContain('What would you like to do?');
    expect(html).toContain('Germany');
  });
});
