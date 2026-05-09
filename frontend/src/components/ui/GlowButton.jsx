import React from 'react';
import { Button } from 'antd';

/**
 * GlowButton - 发光按钮
 * @prop {'primary'|'secondary'|'ghost'} variant - 按钮变体
 * @prop {ReactNode} children - 按钮内容
 * @prop {boolean} loading - 加载状态
 */
const GlowButton = ({
  children,
  variant = 'primary',
  loading = false,
  ...buttonProps
}) => {
  const variantStyles = {
    primary: {
      background: 'var(--gradient-accent)',
      border: 'none',
      color: '#000',
      fontWeight: 600,
    },
    secondary: {
      background: 'var(--color-bg-elevated)',
      borderColor: 'var(--color-accent)',
      color: 'var(--color-accent)',
      borderWidth: 1,
      borderStyle: 'solid',
    },
    ghost: {
      background: 'transparent',
      borderColor: 'transparent',
      color: 'var(--color-text-primary)',
    },
  };

  const style = variantStyles[variant] || variantStyles.primary;

  return (
    <Button
      type={variant === 'primary' ? 'primary' : 'default'}
      loading={loading}
      className={`transition-all duration-300 hover:shadow-[var(--shadow-glow)] active:translate-y-0`}
      style={style}
      {...buttonProps}
    >
      {children}
    </Button>
  );
};

export default GlowButton;
