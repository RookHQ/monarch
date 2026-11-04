import { forwardRef, type HTMLAttributes, type ReactNode } from 'react';
import { cx } from '../../../internal/cx.js';
import { ChevronRightGlyph } from '../../../internal/icons.js';
import { IconButton } from '../../../components/actions/button/index.js';
import {
  DescriptionItem,
  DescriptionList,
} from '../../../components/data-display/description-list/index.js';
import {
  InlineEditText,
  type InlineEditTextProps,
} from '../../../components/forms/inline-edit-text/index.js';

/** One key fact under the record title (owner, created, value). */
export interface RecordHeaderFact {
  /** Stable key. Defaults to the index. */
  id?: string;
  term: ReactNode;
  /** The value. Empty values show the DescriptionList placeholder. */
  value?: ReactNode;
  /** Adds a copy button: `true` copies a text value, a string copies that string. */
  copyable?: boolean | string;
}

/** Previous/next navigation between records of the list the user came from. */
export interface RecordHeaderNavigator {
  /** Zero-based position and list length, shown as "3 of 40". */
  position?: { index: number; total: number };
  /** Opens the previous record. The button is disabled when omitted. */
  onPrevious?: () => void;
  /** Opens the next record. The button is disabled when omitted. */
  onNext?: () => void;
  /** Default "Previous record". Name the record type: "Previous ticket". */
  previousLabel?: string;
  /** Default "Next record". */
  nextLabel?: string;
  /** Formats the position. Default `${index + 1} of ${total}`. */
  formatPosition?: (index: number, total: number) => string;
}

export interface RecordHeaderProps extends Omit<HTMLAttributes<HTMLElement>, 'title'> {
  /** The record's name: a contact, ticket subject or project name. The page heading. */
  title: ReactNode;
  /** An Avatar, or an icon tile for records without a picture. Decorative unless it has its own name. */
  avatar?: ReactNode;
  /** A StatusBadge next to the title. */
  status?: ReactNode;
  /** More badges after the status (priority, tier, "Live chat"). */
  badges?: ReactNode;
  /** A line under the title: the record number, email, "Opened 3 days ago by Ada". */
  subtitle?: ReactNode;
  /** Key facts under the identity block, as a compact DescriptionList. */
  facts?: RecordHeaderFact[];
  /** Record actions: Buttons and a DropdownMenu ("More actions for Ada Lovelace"). */
  actions?: ReactNode;
  /** A BackLink or Breadcrumb above the identity block. */
  back?: ReactNode;
  /** Previous/next record buttons at the top end. */
  navigator?: RecordHeaderNavigator;
  /** A record state notice under the header content: archived, merged, deleted (an InlineNotice or Alert). */
  banner?: ReactNode;
  /** A TabsList or TabsNav for the record's sections. */
  tabs?: ReactNode;
  /**
   * Makes a text `title` editable in place with InlineEditText. Resolve to
   * save; reject to keep the editor open with the error.
   */
  onTitleSave?: (next: string) => void | Promise<void>;
  /** Accessible name of the title editor. Default "Title". */
  titleLabel?: string;
  /** Extra InlineEditText props for the title editor (validate, maxLength, disabled). */
  titleEditProps?: Partial<
    Omit<InlineEditTextProps, 'value' | 'defaultValue' | 'onSave' | 'label'>
  >;
  /** Heading level of the title. `h1` (default) on a record page; `h2` in an inbox detail pane. */
  headingLevel?: 'h1' | 'h2';
}

/**
 * The header of a record detail page: back link and prev/next navigation,
 * avatar, title (optionally editable), status and badges, key facts,
 * actions, a state banner and section tabs. The product supplies the data.
 */
export const RecordHeader = forwardRef<HTMLElement, RecordHeaderProps>(function RecordHeader(
  {
    title,
    avatar,
    status,
    badges,
    subtitle,
    facts,
    actions,
    back,
    navigator,
    banner,
    tabs,
    onTitleSave,
    titleLabel = 'Title',
    titleEditProps,
    headingLevel = 'h1',
    className,
    ...rest
  },
  ref,
) {
  const H = headingLevel;
  const level = headingLevel === 'h1' ? 1 : 2;
  const editable = Boolean(onTitleSave) && typeof title === 'string';
  const heading =
    editable && onTitleSave ? (
      // A heading role on a div: the editor is a block, which an h1 must not contain.
      <div role="heading" aria-level={level} className="mn-record-header__title">
        <InlineEditText
          {...titleEditProps}
          label={titleLabel}
          value={title as string}
          onSave={onTitleSave}
          className={cx('mn-record-header__title-editor', titleEditProps?.className)}
        />
      </div>
    ) : (
      <H className="mn-record-header__title">{title}</H>
    );

  const nav = navigator ? (
    <div className="mn-record-header__navigator">
      {navigator.position && navigator.position.total > 0 ? (
        <span className="mn-record-header__position">
          {(navigator.formatPosition ?? ((i: number, t: number) => `${i + 1} of ${t}`))(
            navigator.position.index,
            navigator.position.total,
          )}
        </span>
      ) : null}
      <IconButton
        size="sm"
        label={navigator.previousLabel ?? 'Previous record'}
        icon={<ChevronRightGlyph className="mn-record-header__prev-glyph" />}
        disabled={!navigator.onPrevious}
        onClick={navigator.onPrevious}
      />
      <IconButton
        size="sm"
        label={navigator.nextLabel ?? 'Next record'}
        icon={<ChevronRightGlyph className="mn-record-header__next-glyph" />}
        disabled={!navigator.onNext}
        onClick={navigator.onNext}
      />
    </div>
  ) : null;

  return (
    <header
      ref={ref}
      {...rest}
      data-editable={editable || undefined}
      className={cx('mn-record-header', className)}
    >
      {back || nav ? (
        <div className="mn-record-header__top">
          {back ? <div className="mn-record-header__back">{back}</div> : null}
          {nav}
        </div>
      ) : null}
      <div className="mn-record-header__main">
        {avatar ? <div className="mn-record-header__avatar">{avatar}</div> : null}
        <div className="mn-record-header__identity">
          <div className="mn-record-header__title-row">
            {heading}
            {status || badges ? (
              <div className="mn-record-header__badges">
                {status}
                {badges}
              </div>
            ) : null}
          </div>
          {subtitle ? <div className="mn-record-header__subtitle">{subtitle}</div> : null}
        </div>
        {actions ? <div className="mn-record-header__actions">{actions}</div> : null}
      </div>
      {facts && facts.length > 0 ? (
        <DescriptionList columns="auto" className="mn-record-header__facts">
          {facts.map((fact, index) => (
            <DescriptionItem
              key={fact.id ?? index}
              term={fact.term}
              {...(fact.copyable !== undefined ? { copyable: fact.copyable } : {})}
            >
              {fact.value}
            </DescriptionItem>
          ))}
        </DescriptionList>
      ) : null}
      {banner ? <div className="mn-record-header__banner">{banner}</div> : null}
      {tabs ? <div className="mn-record-header__tabs">{tabs}</div> : null}
    </header>
  );
});
