/**
 * Ids for rows a list editor creates itself.
 *
 * Editors number their new rows from a counter that starts again on every
 * mount, while `useId` gives the same prefix to the same place in the tree.
 * A list saved in one visit and edited in the next would therefore get a
 * second row with an id that is already in use, and rows are keyed, updated
 * and removed by id. `freshId` skips every id that is taken.
 */
export function freshId(
  prefix: string,
  counter: { current: number },
  taken: Iterable<string>,
): string {
  const used = taken instanceof Set ? (taken as Set<string>) : new Set(taken);
  let id: string;
  do {
    counter.current += 1;
    id = `${prefix}${counter.current}`;
  } while (used.has(id));
  return id;
}
