import { describe, expect, it, vi } from 'vitest';
import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { CareersJobList, type CareerJob } from './index.js';

const jobs: CareerJob[] = [
  {
    id: 'j1',
    title: 'Product Designer',
    department: 'Design',
    location: 'Remote',
    employmentType: 'Full time',
    salary: 'USD 90,000 – 120,000',
    summary: 'Shape how teams plan their work.',
    responsibilities: ['Design new flows', 'Run research sessions'],
    qualifications: ['Three years of product design'],
    mustHaveSkills: ['Prototyping', 'Research'],
    niceToHaveSkills: ['Motion'],
    benefits: ['Learning budget'],
    applyHref: '#apply-j1',
  },
  { id: 'j2', title: 'Support Specialist', location: 'Lisbon', employmentType: 'Part time' },
];

describe('CareersJobList', () => {
  it('lists the roles with a heading, count and card details', async () => {
    render(<CareersJobList jobs={jobs} />);
    const section = screen.getByRole('region', { name: 'Open roles' });
    expect(within(section).getByText('2 positions')).toBeInTheDocument();
    expect(
      within(section).getByRole('heading', {
        level: 3,
        name: /^Product Designer ?, View details$/,
      }),
    ).toBeInTheDocument();
    expect(within(section).getByText('USD 90,000 – 120,000')).toBeInTheDocument();
    expect(within(section).getByRole('list', { name: 'Skills' })).toHaveTextContent('Prototyping');
    await expectNoA11yViolations();
  });

  it('opens the details dialog from the keyboard with apply links, and returns focus', async () => {
    const user = userEvent.setup();
    render(
      <CareersJobList jobs={jobs} organizationName="Northwind" contactEmail="jobs@example.com" />,
    );
    const open = screen.getByRole('button', { name: /^Product Designer ?, View details$/ });
    open.focus();
    await user.keyboard('{Enter}');
    const dialog = await screen.findByRole('dialog', { name: 'Product Designer' });
    expect(within(dialog).getByRole('heading', { name: 'What you’ll do' })).toBeInTheDocument();
    expect(within(dialog).getByText('Learning budget')).toBeInTheDocument();
    expect(within(dialog).getByRole('link', { name: 'Apply now' })).toHaveAttribute(
      'href',
      '#apply-j1',
    );
    expect(within(dialog).getByRole('link', { name: 'Email instead' })).toHaveAttribute(
      'href',
      'mailto:jobs@example.com?subject=Application%3A%20Product%20Designer',
    );
    expect(within(dialog).getByText('Applying to Northwind')).toBeInTheDocument();
    await expectNoA11yViolations();
    await user.keyboard('{Escape}');
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
    expect(open).toHaveFocus();
  });

  it('falls back to email, then to Close', async () => {
    const user = userEvent.setup();
    const { rerender } = render(<CareersJobList jobs={jobs} contactEmail="jobs@example.com" />);
    await user.click(screen.getByRole('button', { name: /Support Specialist/ }));
    expect(await screen.findByRole('link', { name: 'Apply by email' })).toBeInTheDocument();
    rerender(<CareersJobList jobs={jobs} />);
    const dialog = screen.getByRole('dialog');
    const closes = within(dialog).getAllByRole('button', { name: 'Close' });
    await user.click(closes[0]!);
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
  });

  it('is controllable for deep links', async () => {
    const onOpenJobChange = vi.fn();
    render(<CareersJobList jobs={jobs} openJobId="j1" onOpenJobChange={onOpenJobChange} />);
    expect(await screen.findByRole('dialog', { name: 'Product Designer' })).toBeInTheDocument();
    await userEvent.setup().keyboard('{Escape}');
    expect(onOpenJobChange).toHaveBeenCalledWith(null);
  });

  it('shows the empty state, hides the header and translates', () => {
    const { rerender } = render(<CareersJobList jobs={[]} />);
    expect(screen.getByText('No open roles right now. Check back soon.')).toBeInTheDocument();
    rerender(<CareersJobList jobs={jobs} hideHeader messages={{ title: 'Vagas' }} />);
    expect(screen.getByRole('region', { name: 'Vagas' })).toBeInTheDocument();
    expect(screen.queryByRole('heading', { level: 2 })).not.toBeInTheDocument();
  });

  it('renders on the server', () => {
    const html = renderToString(<CareersJobList jobs={jobs} columns={3} />);
    expect(html).toContain('mn-careers');
    expect(html).toContain('Product Designer');
  });
});
