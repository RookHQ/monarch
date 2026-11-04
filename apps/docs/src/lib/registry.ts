/** Registry data, read at build time: the declared brand data of each Rook product. */
export interface Product {
  id: string;
  name: string;
  kind: string;
  domain: string;
  theme: { accent: string; schemes: string[] };
}

const productFiles = import.meta.glob<Product>('../../../../registry/products/*.json', {
  eager: true,
  import: 'default',
});

export const products: Product[] = Object.values(productFiles).sort((a, b) =>
  a.name.localeCompare(b.name),
);
