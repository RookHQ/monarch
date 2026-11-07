import { forwardRef, type HTMLAttributes, type ReactNode } from 'react';
import { cx } from '../../../internal/cx.js';

/** Common entity kinds. Any other string is allowed and shown as given. */
export type AiRelatedEntityKind =
  'person' | 'org' | 'project' | 'document' | 'record' | 'place' | 'other' | (string & {});

/** One entity the AI found related to the current content. */
export interface AiRelatedEntity {
  id: string;
  /** Name shown, e.g. "Jordan Lee". */
  label: string;
  /** What it is, e.g. `person`. */
  kind: AiRelatedEntityKind;
  /** How strongly it relates, from 0 to 1. Sorts the list and sizes graph nodes. */
  weight?: number | undefined;
}

/** A link between two entities, by id. */
export interface AiEntityRelation {
  from: string;
  to: string;
  /** How they relate, e.g. "works at". */
  label?: string | undefined;
}

/** Every string AiRelatedEntities shows or announces. English defaults. */
export interface AiRelatedEntitiesMessages {
  /** Accessible name of the list or graph. */
  label: string;
  /** Visible name of a kind. */
  kind: (kind: AiRelatedEntityKind) => string;
  /** Read for a weight, e.g. "Relevance 80%". */
  weight: (weight: number) => string;
  /** One relation as read in a list, e.g. "works at Acme" or "Acme". */
  relation: (other: string, label: string | undefined) => string;
  /** Introduces an entity's relations, e.g. "Related to". */
  relatedTo: string;
  /** Shown when there are no entities. */
  empty: string;
}

const KIND_LABELS: Record<string, string> = {
  person: 'Person',
  org: 'Organization',
  project: 'Project',
  document: 'Document',
  record: 'Record',
  place: 'Place',
  other: 'Other',
};

const DEFAULT_MESSAGES: AiRelatedEntitiesMessages = {
  label: 'Related entities',
  kind: (kind) => KIND_LABELS[kind] ?? kind,
  weight: (weight) =>
    `Relevance ${new Intl.NumberFormat(undefined, { style: 'percent', maximumFractionDigits: 0 }).format(weight)}`,
  relation: (other, label) => (label ? `${label} ${other}` : other),
  relatedTo: 'Related to',
  empty: 'No related entities',
};

export interface AiRelatedEntitiesProps extends Omit<HTMLAttributes<HTMLDivElement>, 'onSelect'> {
  /** The entities to show. The list is sorted by `weight`, highest first. */
  entities: readonly AiRelatedEntity[];
  /** Links between entities. Shown under each list item and as lines in the graph. */
  relations?: readonly AiEntityRelation[] | undefined;
  /**
   * `list` (default): a ranked list. `graph`: a radial diagram with the
   * centre entity in the middle and the others around it. The graph falls
   * back to the list when there are more than `maxGraphNodes` entities.
   */
  variant?: 'list' | 'graph' | undefined;
  /** Graph only: the entity in the middle. Default: the one with the highest weight. */
  centerId?: string | undefined;
  /** Graph only: above this many entities the list is shown instead. Default 12. */
  maxGraphNodes?: number | undefined;
  /** Makes each entity a button that calls this with the entity. */
  onSelect?: ((entity: AiRelatedEntity) => void) | undefined;
  /** Strings, for localisation. */
  messages?: Partial<AiRelatedEntitiesMessages> | undefined;
}

function clamp01(n: number | undefined) {
  return n === undefined || !Number.isFinite(n) ? undefined : Math.min(1, Math.max(0, n));
}

/** Entities sorted by weight, highest first; entities without a weight keep their order after weighted ones. */
export function sortAiRelatedEntities(entities: readonly AiRelatedEntity[]): AiRelatedEntity[] {
  return entities
    .map((entity, index) => ({ entity, index }))
    .sort((a, b) => {
      const wa = clamp01(a.entity.weight) ?? -1;
      const wb = clamp01(b.entity.weight) ?? -1;
      return wb - wa || a.index - b.index;
    })
    .map((x) => x.entity);
}

/** Point on a circle, starting at the top and going clockwise. */
function polar(cx0: number, cy0: number, r: number, i: number, n: number) {
  const angle = -Math.PI / 2 + (2 * Math.PI * i) / n;
  return { x: cx0 + r * Math.cos(angle), y: cy0 + r * Math.sin(angle) };
}

function round(n: number) {
  return Math.round(n * 100) / 100;
}

const SIZE = 320;
const MID = SIZE / 2;
const RING = 112;

/**
 * Entities (people, organisations, projects, records) the AI found related to
 * the current content, ranked by relevance with their relations. A list by
 * default; `variant="graph"` draws a simple radial diagram (no physics) whose
 * nodes are still buttons with full names, and falls back to the list when
 * there are too many entities.
 */
export const AiRelatedEntities = forwardRef<HTMLDivElement, AiRelatedEntitiesProps>(
  function AiRelatedEntities(
    {
      entities,
      relations = [],
      variant = 'list',
      centerId,
      maxGraphNodes = 12,
      onSelect,
      messages: messagesProp,
      className,
      ...rest
    },
    ref,
  ) {
    const m = { ...DEFAULT_MESSAGES, ...messagesProp };
    const sorted = sortAiRelatedEntities(entities);
    const byId = new Map(entities.map((e) => [e.id, e]));

    const relationsOf = (id: string) =>
      relations.flatMap((r) => {
        const otherId = r.from === id ? r.to : r.to === id ? r.from : undefined;
        const other = otherId !== undefined ? byId.get(otherId) : undefined;
        return other ? [m.relation(other.label, r.label)] : [];
      });

    const fullName = (entity: AiRelatedEntity) => {
      const parts = [entity.label, m.kind(entity.kind)];
      const w = clamp01(entity.weight);
      if (w !== undefined) parts.push(m.weight(w));
      const rel = relationsOf(entity.id);
      if (rel.length) parts.push(`${m.relatedTo}: ${rel.join(', ')}`);
      return parts.join('. ');
    };

    const graph = variant === 'graph' && entities.length > 0 && entities.length <= maxGraphNodes;
    const rendered = graph ? 'graph' : 'list';

    let body: ReactNode;
    if (entities.length === 0) {
      body = <p className="mn-ai-related-entities__empty">{m.empty}</p>;
    } else if (graph && sorted[0]) {
      const center = (centerId !== undefined ? byId.get(centerId) : undefined) ?? sorted[0];
      const ring = sorted.filter((e) => e !== center);
      const pos = new Map<string, { x: number; y: number }>([[center.id, { x: MID, y: MID }]]);
      ring.forEach((e, i) => pos.set(e.id, polar(MID, MID, RING, i, ring.length)));
      const radius = (e: AiRelatedEntity) =>
        e === center ? 26 : 14 + Math.round((clamp01(e.weight) ?? 0.5) * 8);

      body = (
        <svg
          className="mn-ai-related-entities__graph"
          viewBox={`0 0 ${SIZE} ${SIZE}`}
          role="group"
          aria-label={m.label}
        >
          <g className="mn-ai-related-entities__edges" aria-hidden>
            {relations.map((r, i) => {
              const a = pos.get(r.from);
              const b = pos.get(r.to);
              if (!a || !b) return null;
              return (
                <line
                  key={`${r.from}-${r.to}-${i}`}
                  className="mn-ai-related-entities__edge"
                  x1={round(a.x)}
                  y1={round(a.y)}
                  x2={round(b.x)}
                  y2={round(b.y)}
                />
              );
            })}
          </g>
          {[center, ...ring].map((e) => {
            const p = pos.get(e.id) ?? { x: MID, y: MID };
            const r = radius(e);
            const below = p.y >= MID - 1;
            const content = (
              <>
                <circle
                  className="mn-ai-related-entities__node"
                  cx={round(p.x)}
                  cy={round(p.y)}
                  r={r}
                />
                <text
                  className="mn-ai-related-entities__node-label"
                  x={round(p.x)}
                  y={round(below ? p.y + r + 14 : p.y - r - 6)}
                  textAnchor="middle"
                  aria-hidden
                >
                  {e.label.length > 18 ? `${e.label.slice(0, 17)}…` : e.label}
                </text>
              </>
            );
            const common = {
              className: 'mn-ai-related-entities__graph-item',
              'data-center': e === center || undefined,
              'data-kind': e.kind,
              'aria-label': fullName(e),
            };
            return onSelect ? (
              <g
                key={e.id}
                {...common}
                role="button"
                tabIndex={0}
                onClick={() => onSelect(e)}
                onKeyDown={(ev) => {
                  if (ev.key === 'Enter' || ev.key === ' ') {
                    ev.preventDefault();
                    onSelect(e);
                  }
                }}
              >
                {content}
              </g>
            ) : (
              <g key={e.id} {...common} role="img">
                {content}
              </g>
            );
          })}
        </svg>
      );
    } else {
      body = (
        <ul className="mn-ai-related-entities__list" aria-label={m.label}>
          {sorted.map((e) => {
            const w = clamp01(e.weight);
            const rel = relationsOf(e.id);
            const inner = (
              <>
                <span className="mn-ai-related-entities__main">
                  <span className="mn-ai-related-entities__label">{e.label}</span>
                  <span className="mn-ai-related-entities__kind">{m.kind(e.kind)}</span>
                </span>
                {rel.length ? (
                  <span className="mn-ai-related-entities__relations">
                    {m.relatedTo}: {rel.join(', ')}
                  </span>
                ) : null}
                {w !== undefined ? (
                  <span className="mn-ai-related-entities__weight">
                    <span className="mn-visually-hidden">{m.weight(w)}</span>
                    <span className="mn-ai-related-entities__weight-track" aria-hidden>
                      <span
                        className="mn-ai-related-entities__weight-fill"
                        style={{ inlineSize: `${w * 100}%` }}
                      />
                    </span>
                  </span>
                ) : null}
              </>
            );
            return (
              <li key={e.id} className="mn-ai-related-entities__item" data-kind={e.kind}>
                {onSelect ? (
                  <button
                    type="button"
                    className="mn-ai-related-entities__entity"
                    onClick={() => onSelect(e)}
                  >
                    {inner}
                  </button>
                ) : (
                  <div className="mn-ai-related-entities__entity">{inner}</div>
                )}
              </li>
            );
          })}
        </ul>
      );
    }

    return (
      <div
        ref={ref}
        {...rest}
        className={cx('mn-ai-related-entities', className)}
        data-variant={rendered}
        data-interactive={onSelect ? '' : undefined}
      >
        {body}
      </div>
    );
  },
);
