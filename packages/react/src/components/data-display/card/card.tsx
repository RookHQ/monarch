import type { HTMLAttributes } from 'react';
import { cx } from '../../../internal/cx.js';

export interface CardProps extends HTMLAttributes<HTMLDivElement> {
  /** `outline` (default) for most content, `raised` for floating panels, `subtle` inside other cards. */
  variant?: 'outline' | 'raised' | 'subtle';
  /** Remove inner padding, e.g. for a card wrapping a table. */
  flush?: boolean;
}

export function Card({ variant = 'outline', flush = false, className, ...rest }: CardProps) {
  return (
    <div
      {...rest}
      className={cx('mn-card', className)}
      data-variant={variant}
      data-flush={flush || undefined}
    />
  );
}

export function CardHeader({ className, ...rest }: HTMLAttributes<HTMLDivElement>) {
  return <div {...rest} className={cx('mn-card__header', className)} />;
}

export interface CardTitleProps extends HTMLAttributes<HTMLHeadingElement> {
  /** Heading level that fits the page outline. Default h3. */
  as?: 'h2' | 'h3' | 'h4';
}

export function CardTitle({ as: Tag = 'h3', className, ...rest }: CardTitleProps) {
  return <Tag {...rest} className={cx('mn-card__title', className)} />;
}

export function CardDescription({ className, ...rest }: HTMLAttributes<HTMLParagraphElement>) {
  return <p {...rest} className={cx('mn-card__description', className)} />;
}

export function CardContent({ className, ...rest }: HTMLAttributes<HTMLDivElement>) {
  return <div {...rest} className={cx('mn-card__content', className)} />;
}

export function CardFooter({ className, ...rest }: HTMLAttributes<HTMLDivElement>) {
  return <div {...rest} className={cx('mn-card__footer', className)} />;
}
