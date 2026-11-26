/**
 * Best-effort parsing of JSON that is still arriving, such as a component's
 * props or a tool's arguments while they stream: open strings, arrays and
 * objects are closed, and a dangling key or comma is dropped. Never throws.
 */

/** Parses complete JSON, or the longest sensible reading of a prefix of it. `undefined` when nothing can be read. */
export function parsePartialJson(text: string): unknown {
  const trimmed = text.trim();
  if (!trimmed) return undefined;
  try {
    return JSON.parse(trimmed);
  } catch {
    // Not complete yet: close it below.
  }
  const stack: ('{' | '[')[] = [];
  let inString = false;
  let escaped = false;
  for (const ch of trimmed) {
    if (inString) {
      if (escaped) escaped = false;
      else if (ch === '\\') escaped = true;
      else if (ch === '"') inString = false;
      continue;
    }
    if (ch === '"') inString = true;
    else if (ch === '{' || ch === '[') stack.push(ch);
    else if (ch === '}' || ch === ']') stack.pop();
  }
  let head = trimmed;
  if (inString) head += escaped ? '\\"' : '"';
  const close = () =>
    stack
      .slice()
      .reverse()
      .map((c) => (c === '{' ? '}' : ']'))
      .join('');
  // Try progressively shorter readings: drop a dangling `,`, `:` or key.
  for (let attempt = 0; attempt < 4; attempt++) {
    try {
      return JSON.parse(head + close());
    } catch {
      const cut = head
        .replace(/,\s*$/, '')
        .replace(/,?\s*"(?:[^"\\]|\\.)*"\s*:\s*$/, '')
        .replace(/:\s*(?:t|tr|tru|f|fa|fal|fals|n|nu|nul|-?\d*\.?)$/, ': null')
        .replace(/,\s*"(?:[^"\\]|\\.)*"$/, '');
      if (cut === head) break;
      head = cut;
    }
  }
  return undefined;
}
