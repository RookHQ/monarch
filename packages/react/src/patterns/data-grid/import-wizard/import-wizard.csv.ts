/** A parsed table: the header row and the data rows, as text. */
export interface ImportTable {
  headers: string[];
  rows: string[][];
}

/** Picks the delimiter that splits the first line into the most fields: comma, semicolon or tab. */
function detectDelimiter(text: string): string {
  const firstLine = text.slice(0, text.search(/\r?\n|$/));
  let best = ',';
  let bestCount = 0;
  for (const d of [',', ';', '\t']) {
    const count = firstLine.split(d).length - 1;
    if (count > bestCount) {
      best = d;
      bestCount = count;
    }
  }
  return best;
}

/**
 * Parses delimited text (RFC 4180 CSV): quoted fields, doubled quotes inside
 * quotes, line breaks inside quotes, CRLF or LF line ends and a leading byte
 * order mark. The delimiter is detected from the first line unless given.
 * Blank lines are skipped. The first row becomes `headers` (trimmed).
 */
export function parseCsv(input: string, delimiter?: string): ImportTable {
  const text = input.charCodeAt(0) === 0xfeff ? input.slice(1) : input;
  const d = delimiter ?? detectDelimiter(text);
  const records: string[][] = [];
  let record: string[] = [];
  let field = '';
  let quoted = false;
  let i = 0;
  const endRecord = () => {
    record.push(field);
    field = '';
    if (!(record.length === 1 && record[0] === '')) records.push(record);
    record = [];
  };
  while (i < text.length) {
    const ch = text[i] as string;
    if (quoted) {
      if (ch === '"') {
        if (text[i + 1] === '"') {
          field += '"';
          i += 2;
          continue;
        }
        quoted = false;
        i += 1;
        continue;
      }
      field += ch;
      i += 1;
      continue;
    }
    if (ch === '"' && field === '') {
      quoted = true;
      i += 1;
    } else if (ch === d) {
      record.push(field);
      field = '';
      i += 1;
    } else if (ch === '\r' || ch === '\n') {
      endRecord();
      i += ch === '\r' && text[i + 1] === '\n' ? 2 : 1;
    } else {
      field += ch;
      i += 1;
    }
  }
  if (field !== '' || record.length) endRecord();
  const [head = [], ...rows] = records;
  return { headers: head.map((h) => h.trim()), rows };
}

const cell = (v: string) => (/[",\r\n]/.test(v) ? `"${v.replace(/"/g, '""')}"` : v);

/** Builds CSV text from rows of cells (CRLF line ends, quoted where needed). */
export function toCsv(rows: readonly (readonly (string | number)[])[]): string {
  return rows.map((r) => r.map((v) => cell(String(v))).join(',')).join('\r\n');
}

/** Lower-cases and strips everything but letters and digits: "E-mail Address" → "emailaddress". */
export function normalizeHeader(value: string): string {
  return value
    .normalize('NFKD')
    .toLowerCase()
    .replace(/[^\p{L}\p{N}]/gu, '');
}
