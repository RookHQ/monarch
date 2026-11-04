import { PageFrame } from '../../layout/page-frame.js';
import { products } from '../../lib/registry.js';

export function ProductsPage() {
  return (
    <PageFrame
      eyebrow="Guides"
      title="Products"
      lead="The Rook applications registered with Monarch. Each one declares a brand accent; Monarch derives its accessible variants from it, so every product passes the same contrast checks."
      toc={false}
    >
      <div className="docs-table-scroll">
        <table className="docs-table">
          <thead>
            <tr>
              <th scope="col">Product</th>
              <th scope="col">Kind</th>
              <th scope="col">Accent</th>
              <th scope="col">Schemes</th>
            </tr>
          </thead>
          <tbody>
            {products.map((p) => (
              <tr key={p.id}>
                <td>
                  <a href={`https://${p.domain}`}>{p.name}</a>
                </td>
                <td>{p.kind}</td>
                <td>
                  <span
                    className="docs-accent-dot"
                    style={{ background: `hsl(${p.theme.accent})` }}
                    aria-hidden
                  />{' '}
                  <code className="docs-code-inline">{p.theme.accent}</code>
                </td>
                <td className="docs-footnote">{p.theme.schemes.join(', ')}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </PageFrame>
  );
}
