export function Button(props: { label: string }) {
  return <button className="bg-primary text-primary-foreground">{props.label}</button>;
}
