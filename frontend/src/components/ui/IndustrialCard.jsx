import React from 'react';

/**
 * IndustrialCard - 工业风卡片容器
 * @prop {string} className - 额外类名
 * @prop {ReactNode} children - 子内容
 * @prop {boolean} glowBorder - 是否显示发光边框（默认 false）
 * @prop {boolean} glassmorphism - 是否启用毛玻璃效果（默认 true）
 */
const IndustrialCard = ({
  children,
  className = '',
  glowBorder = false,
  glassmorphism = true,
  ...rest
}) => {
  return (
    <div
      className={`rounded-xl border p-4 transition-all duration-300 ${
        glassmorphism ? 'glassmorphism' : ''
      } ${glowBorder ? 'glow-border-hover' : ''} ${className}`}
      style={{
        background: glassmorphism ? undefined : 'var(--color-bg-card)',
        borderColor: glassmorphism ? undefined : 'var(--color-border)',
      }}
      {...rest}
    >
      {children}
    </div>
  );
};

export default IndustrialCard;
