import { forwardRef, type HTMLAttributes } from 'react';
import { cx } from '../../../internal/cx.js';

export interface CardProps extends HTMLAttributes<HTMLDivElement> {
  /** `outline` (default) for most content, `raised` for floating panels, `subtle` inside other cards. */
  variant?: 'outline' | 'raised' | 'subtle';
  /** Remove inner padding, e.g. for a card wrapping a table. */
  flush?: boolean;
}

export const Card = forwardRef<HTMLDivElement, CardProps>(function Card(
  { variant = 'outline', flush = false, className, ...rest },
  ref,
) {
  return (
    <div
      {...rest}
      ref={ref}
      className={cx('mn-card', className)}
      data-variant={variant}
      data-flush={flush || undefined}
    />
  );
});
Card.displayName = 'Card';

export const CardHeader = forwardRef<HTMLDivElement, HTMLAttributes<HTMLDivElement>>(
  function CardHeader({ className, ...rest }, ref) {
    return <div {...rest} ref={ref} className={cx('mn-card__header', className)} />;
  },
);
CardHeader.displayName = 'CardHeader';

export interface CardTitleProps extends HTMLAttributes<HTMLHeadingElement> {
  /** Heading level that fits the page outline. Default h3. */
  as?: 'h2' | 'h3' | 'h4';
}

export const CardTitle = forwardRef<HTMLHeadingElement, CardTitleProps>(function CardTitle(
  { as: Tag = 'h3', className, ...rest },
  ref,
) {
  return <Tag {...rest} ref={ref} className={cx('mn-card__title', className)} />;
});
CardTitle.displayName = 'CardTitle';

export const CardDescription = forwardRef<
  HTMLParagraphElement,
  HTMLAttributes<HTMLParagraphElement>
>(function CardDescription({ className, ...rest }, ref) {
  return <p {...rest} ref={ref} className={cx('mn-card__description', className)} />;
});
CardDescription.displayName = 'CardDescription';

export const CardContent = forwardRef<HTMLDivElement, HTMLAttributes<HTMLDivElement>>(
  function CardContent({ className, ...rest }, ref) {
    return <div {...rest} ref={ref} className={cx('mn-card__content', className)} />;
  },
);
CardContent.displayName = 'CardContent';

export const CardFooter = forwardRef<HTMLDivElement, HTMLAttributes<HTMLDivElement>>(
  function CardFooter({ className, ...rest }, ref) {
    return <div {...rest} ref={ref} className={cx('mn-card__footer', className)} />;
  },
);
CardFooter.displayName = 'CardFooter';
