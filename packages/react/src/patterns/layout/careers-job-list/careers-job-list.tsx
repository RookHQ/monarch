'use client';

import { forwardRef, useId, useRef, useState, type ElementType, type HTMLAttributes } from 'react';
import {
  Dialog,
  DialogBody,
  DialogContent,
  DialogHeader,
  DialogTitle,
} from '../../../components/overlays/dialog/index.js';
import { Button } from '../../../components/actions/button/index.js';
import { Badge } from '../../../components/data-display/badge/index.js';
import { cx } from '../../../internal/cx.js';
import { ChevronRightGlyph } from '../../../internal/icons.js';

/** An open role on a public careers page. Strings are shown as given (format salaries in the app). */
export interface CareerJob {
  id: string;
  title: string;
  department?: string | null | undefined;
  location?: string | null | undefined;
  /** "Full time", "Contract". */
  employmentType?: string | null | undefined;
  /** "3+ years". */
  experience?: string | null | undefined;
  /** Already formatted, e.g. "USD 90,000 – 120,000". */
  salary?: string | null | undefined;
  /** One or two sentences, on the card and at the top of the details. */
  summary?: string | null | undefined;
  /** Longer description in the details (plain text, line breaks kept). */
  description?: string | null | undefined;
  responsibilities?: string[] | undefined;
  qualifications?: string[] | undefined;
  mustHaveSkills?: string[] | undefined;
  niceToHaveSkills?: string[] | undefined;
  benefits?: string[] | undefined;
  /** The hosted application form for this role. Without it, `contactEmail` is offered. */
  applyHref?: string | null | undefined;
}

export interface CareersJobListMessages {
  title: string;
  /** "3 positions". */
  count: (n: number) => string;
  empty: string;
  /** Opens the details; read after the job title. */
  viewDetails: string;
  responsibilities: string;
  qualifications: string;
  mustHave: string;
  niceToHave: string;
  benefits: string;
  apply: string;
  applyByEmail: string;
  emailInstead: string;
  /** Footer note in the details. */
  applyingTo: (organization: string) => string;
  /** Subject of the application email. */
  emailSubject: (jobTitle: string) => string;
  close: string;
  /** Names the skill list on a card. */
  skills: string;
}

const DEFAULT_MESSAGES: CareersJobListMessages = {
  title: 'Open roles',
  count: (n) => `${n} ${n === 1 ? 'position' : 'positions'}`,
  empty: 'No open roles right now. Check back soon.',
  viewDetails: 'View details',
  responsibilities: 'What you’ll do',
  qualifications: 'What we’re looking for',
  mustHave: 'Must-have skills',
  niceToHave: 'Nice to have',
  benefits: 'Benefits',
  apply: 'Apply now',
  applyByEmail: 'Apply by email',
  emailInstead: 'Email instead',
  applyingTo: (o) => `Applying to ${o}`,
  emailSubject: (t) => `Application: ${t}`,
  close: 'Close',
  skills: 'Skills',
};

export interface CareersJobListProps extends Omit<HTMLAttributes<HTMLElement>, 'title'> {
  jobs: CareerJob[];
  /** Cards per row on wide screens. Default 2. */
  columns?: 1 | 2 | 3 | undefined;
  /** The hiring organisation, in the details footer. */
  organizationName?: string | undefined;
  /** Applications by email: the fallback without `applyHref`, and "Email instead" beside it. */
  contactEmail?: string | null | undefined;
  /** The job whose details are open (controlled), e.g. from `?job=`. */
  openJobId?: string | null | undefined;
  onOpenJobChange?: ((jobId: string | null) => void) | undefined;
  /** Heading level of the section title. Default `h2`; job titles are one level below. */
  headingAs?: 'h2' | 'h3' | undefined;
  /** Hide the section heading and count (when the page already has them). */
  hideHeader?: boolean | undefined;
  /** Router link for apply links. Default `a`. */
  linkAs?: ElementType | undefined;
  /** Interface text, for translation. */
  messages?: Partial<CareersJobListMessages> | undefined;
}

function Meta({ job }: { job: CareerJob }) {
  const parts = [job.location, job.employmentType, job.experience, job.salary].filter(
    (p): p is string => !!p,
  );
  if (!parts.length) return null;
  return (
    <ul className="mn-careers__meta">
      {parts.map((p) => (
        <li key={p}>{p}</li>
      ))}
    </ul>
  );
}

function ListSection({ title, items }: { title: string; items?: string[] | undefined }) {
  if (!items?.length) return null;
  return (
    <section className="mn-careers__detail-section">
      <h3 className="mn-careers__detail-heading">{title}</h3>
      <ul className="mn-careers__bullets">
        {items.map((item, i) => (
          <li key={i}>{item}</li>
        ))}
      </ul>
    </section>
  );
}

function SkillSection({
  title,
  items,
  tone,
}: {
  title: string;
  items?: string[] | undefined;
  tone: 'accent' | 'neutral';
}) {
  if (!items?.length) return null;
  return (
    <section className="mn-careers__detail-section">
      <h3 className="mn-careers__detail-heading">{title}</h3>
      <ul className="mn-careers__skills">
        {items.map((s) => (
          <li key={s}>
            <Badge tone={tone}>{s}</Badge>
          </li>
        ))}
      </ul>
    </section>
  );
}

/**
 * The open-roles section of a public careers page: a grid of job cards
 * (department, title, location, type, salary, skills) that open a details
 * dialog with responsibilities, requirements, benefits and Apply. The app
 * passes the jobs with their apply links; the list never fetches.
 */
export const CareersJobList = forwardRef<HTMLElement, CareersJobListProps>(function CareersJobList(
  {
    jobs,
    columns = 2,
    organizationName,
    contactEmail,
    openJobId,
    onOpenJobChange,
    headingAs: Heading = 'h2',
    hideHeader = false,
    linkAs: Link = 'a',
    messages: messagesProp,
    className,
    ...rest
  },
  ref,
) {
  const messages = { ...DEFAULT_MESSAGES, ...messagesProp };
  const titleId = useId();
  const [innerOpen, setInnerOpen] = useState<string | null>(null);
  const openId = openJobId !== undefined ? openJobId : innerOpen;
  const setOpen = (id: string | null) => {
    if (openJobId === undefined) setInnerOpen(id);
    onOpenJobChange?.(id);
  };
  const open = jobs.find((j) => j.id === openId) ?? null;
  // The details open from a card button, not a DialogTrigger: remember it to return focus.
  const openers = useRef(new Map<string, HTMLButtonElement>());
  const JobHeading = Heading === 'h2' ? 'h3' : 'h4';
  const mailto = (job: CareerJob) =>
    contactEmail
      ? `mailto:${contactEmail}?subject=${encodeURIComponent(messages.emailSubject(job.title))}`
      : null;

  return (
    <section
      ref={ref}
      {...rest}
      aria-labelledby={hideHeader ? undefined : titleId}
      aria-label={hideHeader ? messages.title : undefined}
      className={cx('mn-careers', className)}
      data-columns={columns}
    >
      {hideHeader ? null : (
        <div className="mn-careers__header">
          <Heading id={titleId} className="mn-careers__title">
            {messages.title}
          </Heading>
          <p className="mn-careers__count">{messages.count(jobs.length)}</p>
        </div>
      )}
      {jobs.length === 0 ? (
        <p className="mn-careers__empty">{messages.empty}</p>
      ) : (
        <ul className="mn-careers__grid">
          {jobs.map((job) => {
            const skills = job.mustHaveSkills?.length ? job.mustHaveSkills : job.niceToHaveSkills;
            return (
              <li key={job.id} className="mn-careers__item">
                <article className="mn-careers__card">
                  {job.department ? (
                    <p className="mn-careers__department">{job.department}</p>
                  ) : null}
                  <JobHeading className="mn-careers__job-title">
                    <button
                      type="button"
                      className="mn-careers__open"
                      aria-haspopup="dialog"
                      ref={(node) => {
                        if (node) openers.current.set(job.id, node);
                        else openers.current.delete(job.id);
                      }}
                      onClick={() => setOpen(job.id)}
                    >
                      {job.title}
                      <span className="mn-visually-hidden">, {messages.viewDetails}</span>
                    </button>
                  </JobHeading>
                  <Meta job={job} />
                  {job.summary ? <p className="mn-careers__summary">{job.summary}</p> : null}
                  {skills?.length ? (
                    <ul className="mn-careers__skills" aria-label={messages.skills}>
                      {skills.slice(0, 4).map((s) => (
                        <li key={s}>
                          <Badge tone="neutral" size="sm">
                            {s}
                          </Badge>
                        </li>
                      ))}
                    </ul>
                  ) : null}
                  <ChevronRightGlyph className="mn-careers__arrow" aria-hidden />
                </article>
              </li>
            );
          })}
        </ul>
      )}
      <Dialog open={open !== null} onOpenChange={(next) => !next && setOpen(null)}>
        {open ? (
          <DialogContent
            size="lg"
            closeLabel={messages.close}
            aria-describedby={undefined}
            onCloseAutoFocus={(event) => {
              const opener = openers.current.get(open.id);
              if (opener) {
                event.preventDefault();
                opener.focus();
              }
            }}
            className="mn-careers__dialog"
            footer={
              <div className="mn-careers__dialog-footer">
                {organizationName ? (
                  <p className="mn-careers__applying">{messages.applyingTo(organizationName)}</p>
                ) : (
                  <span />
                )}
                <div className="mn-careers__actions">
                  {open.applyHref && mailto(open) ? (
                    <Button variant="ghost" asChild>
                      <a href={mailto(open) ?? ''}>{messages.emailInstead}</a>
                    </Button>
                  ) : null}
                  {open.applyHref ? (
                    <Button asChild>
                      <Link href={open.applyHref}>{messages.apply}</Link>
                    </Button>
                  ) : mailto(open) ? (
                    <Button asChild>
                      <a href={mailto(open) ?? ''}>{messages.applyByEmail}</a>
                    </Button>
                  ) : (
                    <Button variant="secondary" onClick={() => setOpen(null)}>
                      {messages.close}
                    </Button>
                  )}
                </div>
              </div>
            }
          >
            <DialogHeader>
              {open.department ? <p className="mn-careers__department">{open.department}</p> : null}
              <DialogTitle>{open.title}</DialogTitle>
              <Meta job={open} />
            </DialogHeader>
            <DialogBody className="mn-careers__details">
              {open.summary ? <p className="mn-careers__prose">{open.summary}</p> : null}
              {open.description && open.description !== open.summary ? (
                <p className="mn-careers__prose">{open.description}</p>
              ) : null}
              <ListSection title={messages.responsibilities} items={open.responsibilities} />
              <ListSection title={messages.qualifications} items={open.qualifications} />
              <SkillSection title={messages.mustHave} items={open.mustHaveSkills} tone="accent" />
              <SkillSection
                title={messages.niceToHave}
                items={open.niceToHaveSkills}
                tone="neutral"
              />
              <ListSection title={messages.benefits} items={open.benefits} />
            </DialogBody>
          </DialogContent>
        ) : null}
      </Dialog>
    </section>
  );
});
