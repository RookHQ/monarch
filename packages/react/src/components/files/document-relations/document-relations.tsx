'use client';

import {
  forwardRef,
  useId,
  useMemo,
  useRef,
  useState,
  type HTMLAttributes,
  type KeyboardEvent as ReactKeyboardEvent,
  type ReactNode,
} from 'react';
import { cx } from '../../../internal/cx.js';
import { Button } from '../../actions/button/index.js';
import { Field, FieldLabel, NativeSelect } from '../../forms/form-field/index.js';
import { Input } from '../../forms/text-field/index.js';
import {
  Dialog,
  DialogBody,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from '../../overlays/dialog/index.js';

/** A kind of relation and its label. `inverse` is how it reads from the other document. */
export interface DocumentRelationType {
  value: string;
  label: string;
  /** The type as seen from the target (parent-of ↔ child-of). Default: itself. */
  inverse?: string | undefined;
}

export const DEFAULT_DOCUMENT_RELATION_TYPES: DocumentRelationType[] = [
  { value: 'related-to', label: 'Related to', inverse: 'related-to' },
  { value: 'parent-of', label: 'Parent of', inverse: 'child-of' },
  { value: 'child-of', label: 'Child of', inverse: 'parent-of' },
  { value: 'supersedes', label: 'Supersedes', inverse: 'superseded-by' },
  { value: 'superseded-by', label: 'Superseded by', inverse: 'supersedes' },
  { value: 'implements', label: 'Implements', inverse: 'specified-by' },
  { value: 'specified-by', label: 'Specified by', inverse: 'implements' },
];

/** A document on the other end of a relation, or one that can be linked. */
export interface DocumentRelationTarget {
  id: string;
  title: string;
  /** Where it opens. */
  href?: string | undefined;
  /** A second line, for example its space or folder. */
  description?: string | undefined;
}

/** One relation of this document. */
export interface DocumentRelation {
  id: string;
  /** A `value` from `types`. */
  type: string;
  /**
   * `outgoing` (default): this document → target, read as `type`.
   * `incoming`: target → this document, read as the inverse type.
   */
  direction?: 'outgoing' | 'incoming' | undefined;
  target: DocumentRelationTarget;
}

/** Visible and announced strings. Override them to translate. */
export interface DocumentRelationsMessages {
  label: string;
  add: string;
  dialogTitle: string;
  dialogDescription: string;
  typeLabel: string;
  searchLabel: string;
  searchPlaceholder: string;
  results: string;
  noResults: string;
  remove: (title: string) => string;
  added: (title: string) => string;
  removed: (title: string) => string;
  failed: string;
  none: string;
}

export const DEFAULT_DOCUMENT_RELATIONS_MESSAGES: DocumentRelationsMessages = {
  label: 'Relations',
  add: 'Add relation',
  dialogTitle: 'Add a relation',
  dialogDescription: 'Choose how this document connects to another one.',
  typeLabel: 'Relation',
  searchLabel: 'Document',
  searchPlaceholder: 'Search documents…',
  results: 'Matching documents',
  noResults: 'No matching documents.',
  remove: (title) => `Remove relation to ${title}`,
  added: (title) => `Relation to ${title} added`,
  removed: (title) => `Relation to ${title} removed`,
  failed: 'That didn’t work. Try again.',
  none: 'No relations yet.',
};

export interface DocumentRelationsProps extends HTMLAttributes<HTMLDivElement> {
  /** This document's relations. */
  relations: DocumentRelation[];
  /** Relation types and their inverses. Default: related, parent/child, supersedes, implements. */
  types?: DocumentRelationType[] | undefined;
  /** Documents that can be linked. Filtered by the search text unless `onSearch` is set. */
  candidates?: DocumentRelationTarget[] | undefined;
  /** Search on the server instead: called as the person types; update `candidates`. */
  onSearch?: ((query: string) => void) | undefined;
  /** Shows "Add relation". Reject to keep the dialog open with an error. */
  onAdd?: ((targetId: string, type: string) => void | Promise<void>) | undefined;
  /** Shows a remove button on each relation. */
  onRemove?: ((relationId: string) => void | Promise<void>) | undefined;
  /** Renders the target link, for example with the router's Link. Default: `<a href>`. */
  renderLink?: ((target: DocumentRelationTarget, children: ReactNode) => ReactNode) | undefined;
  /** Type chosen when the dialog opens. Default `related-to` (or the first type). */
  defaultType?: string | undefined;
  /** Shows "No relations yet." when there are none. Default false. */
  showEmpty?: boolean | undefined;
  /** Override visible and announced strings. */
  messages?: Partial<DocumentRelationsMessages> | undefined;
}

function ArrowGlyph({ type }: { type: string }) {
  const d =
    type === 'parent-of'
      ? 'M12 19V5M5 12l7-7 7 7'
      : type === 'child-of'
        ? 'M12 5v14M19 12l-7 7-7-7'
        : type === 'implements'
          ? 'M20 6 9 17l-5-5'
          : 'M5 12h14M12 5l7 7-7 7';
  return (
    <svg
      viewBox="0 0 24 24"
      width="14"
      height="14"
      fill="none"
      stroke="currentColor"
      strokeWidth={2}
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
      focusable="false"
    >
      <path d={d} />
    </svg>
  );
}

function CloseGlyph() {
  return (
    <svg
      viewBox="0 0 24 24"
      width="14"
      height="14"
      fill="none"
      stroke="currentColor"
      strokeWidth={2}
      strokeLinecap="round"
      aria-hidden="true"
      focusable="false"
    >
      <path d="M18 6 6 18M6 6l12 12" />
    </svg>
  );
}

/**
 * Typed links between documents ("Parent of", "Supersedes", "Related to"),
 * read the right way round from either side, with a dialog to add one by
 * searching and a remove button on each. Data and callbacks in; routing goes
 * through `renderLink`.
 */
export const DocumentRelations = forwardRef<HTMLDivElement, DocumentRelationsProps>(
  function DocumentRelations(
    {
      relations,
      types = DEFAULT_DOCUMENT_RELATION_TYPES,
      candidates = [],
      onSearch,
      onAdd,
      onRemove,
      renderLink,
      defaultType,
      showEmpty = false,
      messages: messagesProp,
      className,
      ...rest
    },
    ref,
  ) {
    const m = { ...DEFAULT_DOCUMENT_RELATIONS_MESSAGES, ...messagesProp };
    const byValue = useMemo(() => new Map(types.map((t) => [t.value, t])), [types]);
    const initialType =
      defaultType ?? (byValue.has('related-to') ? 'related-to' : (types[0]?.value ?? ''));
    const [open, setOpen] = useState(false);
    const [type, setType] = useState(initialType);
    const [query, setQuery] = useState('');
    const [pending, setPending] = useState<string | null>(null);
    const [error, setError] = useState<string | null>(null);
    const [removing, setRemoving] = useState<string | null>(null);
    const [announce, setAnnounce] = useState('');
    const listRef = useRef<HTMLUListElement>(null);
    const addRef = useRef<HTMLButtonElement>(null);
    const resultsRef = useRef<HTMLUListElement>(null);
    const resultsId = useId();
    const labelId = useId();

    const shown = useMemo(() => {
      const q = query.trim().toLowerCase();
      const related = new Set(relations.map((r) => r.target.id));
      const list = candidates.filter((c) => !related.has(c.id));
      if (onSearch || !q) return list.slice(0, 12);
      return list.filter((c) => c.title.toLowerCase().includes(q)).slice(0, 12);
    }, [candidates, query, onSearch, relations]);

    const labelOf = (relation: DocumentRelation) => {
      const value =
        relation.direction === 'incoming'
          ? (byValue.get(relation.type)?.inverse ?? relation.type)
          : relation.type;
      return { value, label: byValue.get(value)?.label ?? value };
    };

    const link = (target: DocumentRelationTarget) => {
      const children = <span className="mn-document-relations__title">{target.title}</span>;
      if (renderLink) return renderLink(target, children);
      return target.href ? (
        <a href={target.href} className="mn-document-relations__link">
          {children}
        </a>
      ) : (
        children
      );
    };

    const add = async (target: DocumentRelationTarget) => {
      if (!onAdd || pending) return;
      setPending(target.id);
      setError(null);
      try {
        await onAdd(target.id, type);
        setOpen(false);
        setQuery('');
        setAnnounce(m.added(target.title));
      } catch (e) {
        setError(e instanceof Error && e.message ? e.message : m.failed);
      } finally {
        setPending(null);
      }
    };

    const remove = async (relation: DocumentRelation, index: number) => {
      if (!onRemove || removing) return;
      setRemoving(relation.id);
      setError(null);
      try {
        await onRemove(relation.id);
        setAnnounce(m.removed(relation.target.title));
        // Keep focus in the list: the next relation's remove button, else Add.
        window.requestAnimationFrame(() => {
          const buttons = listRef.current?.querySelectorAll<HTMLButtonElement>(
            '.mn-document-relations__remove',
          );
          const next = buttons?.[Math.min(index, (buttons?.length ?? 1) - 1)];
          (next ?? addRef.current)?.focus();
        });
      } catch (e) {
        setError(e instanceof Error && e.message ? e.message : m.failed);
      } finally {
        setRemoving(null);
      }
    };

    const onResultsKey = (event: ReactKeyboardEvent<HTMLElement>) => {
      if (event.key !== 'ArrowDown' && event.key !== 'ArrowUp') return;
      const buttons = Array.from(
        resultsRef.current?.querySelectorAll<HTMLButtonElement>('button') ?? [],
      );
      if (!buttons.length) return;
      event.preventDefault();
      const index = buttons.indexOf(document.activeElement as HTMLButtonElement);
      const next =
        event.key === 'ArrowDown'
          ? buttons[Math.min(buttons.length - 1, index + 1)]
          : index <= 0
            ? undefined
            : buttons[index - 1];
      if (next) next.focus();
      else
        (
          event.currentTarget.ownerDocument.getElementById(
            `${resultsId}-search`,
          ) as HTMLInputElement | null
        )?.focus();
    };

    return (
      <div
        ref={ref}
        role="group"
        aria-labelledby={labelId}
        {...rest}
        className={cx('mn-document-relations', className)}
      >
        <span id={labelId} className="mn-visually-hidden">
          {m.label}
        </span>
        {relations.length > 0 ? (
          <ul ref={listRef} className="mn-document-relations__list">
            {relations.map((relation, index) => {
              const { value, label } = labelOf(relation);
              return (
                <li key={relation.id} className="mn-document-relations__chip" data-type={value}>
                  <span className="mn-document-relations__type">
                    <ArrowGlyph type={value} />
                    {label}
                  </span>
                  {link(relation.target)}
                  {onRemove ? (
                    <button
                      type="button"
                      className="mn-document-relations__remove"
                      aria-label={m.remove(relation.target.title)}
                      title={m.remove(relation.target.title)}
                      disabled={removing === relation.id}
                      aria-busy={removing === relation.id || undefined}
                      onClick={() => void remove(relation, index)}
                    >
                      <CloseGlyph />
                    </button>
                  ) : null}
                </li>
              );
            })}
          </ul>
        ) : showEmpty ? (
          <p className="mn-document-relations__empty">{m.none}</p>
        ) : null}

        {onAdd ? (
          <Dialog
            open={open}
            onOpenChange={(next) => {
              if (pending) return;
              setOpen(next);
              if (next) {
                setType(initialType);
                setError(null);
              } else setQuery('');
            }}
          >
            <DialogTrigger asChild>
              <Button ref={addRef} size="sm" variant="ghost" className="mn-document-relations__add">
                {m.add}
              </Button>
            </DialogTrigger>
            <DialogContent size="sm" busy={!!pending}>
              <DialogHeader>
                <DialogTitle>{m.dialogTitle}</DialogTitle>
                <DialogDescription>{m.dialogDescription}</DialogDescription>
              </DialogHeader>
              <DialogBody className="mn-document-relations__dialog">
                <Field>
                  <FieldLabel>{m.typeLabel}</FieldLabel>
                  <NativeSelect
                    value={type}
                    onChange={(e) => setType(e.target.value)}
                    options={types.map((t) => ({ value: t.value, label: t.label }))}
                  />
                </Field>
                <Field id={`${resultsId}-search`}>
                  <FieldLabel>{m.searchLabel}</FieldLabel>
                  <Input
                    type="search"
                    value={query}
                    placeholder={m.searchPlaceholder}
                    aria-controls={resultsId}
                    autoComplete="off"
                    onKeyDown={onResultsKey}
                    onChange={(e) => {
                      setQuery(e.target.value);
                      onSearch?.(e.target.value);
                    }}
                  />
                </Field>
                {shown.length > 0 ? (
                  <ul
                    ref={resultsRef}
                    id={resultsId}
                    aria-label={m.results}
                    className="mn-document-relations__results"
                  >
                    {shown.map((c) => (
                      <li key={c.id}>
                        <button
                          type="button"
                          className="mn-document-relations__result"
                          disabled={!!pending}
                          aria-busy={pending === c.id || undefined}
                          onKeyDown={onResultsKey}
                          onClick={() => void add(c)}
                        >
                          <span className="mn-document-relations__result-title">{c.title}</span>
                          {c.description ? (
                            <span className="mn-document-relations__result-description">
                              {c.description}
                            </span>
                          ) : null}
                        </button>
                      </li>
                    ))}
                  </ul>
                ) : (
                  <p id={resultsId} className="mn-document-relations__empty" role="status">
                    {m.noResults}
                  </p>
                )}
                {error && open ? (
                  <p className="mn-document-relations__error" role="alert">
                    {error}
                  </p>
                ) : null}
              </DialogBody>
            </DialogContent>
          </Dialog>
        ) : null}
        {error && !open ? (
          <p className="mn-document-relations__error" role="alert">
            {error}
          </p>
        ) : null}
        <span className="mn-visually-hidden" aria-live="polite">
          {announce}
        </span>
      </div>
    );
  },
);
