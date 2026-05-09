import React from 'react';
import { Typography } from 'antd';

const { Title } = Typography;

/**
 * PageHeader - 页面标题栏（工业风装饰线）
 * @prop {string} title - 页面标题
 * @prop {ReactNode} icon - 标题图标（可选）
 * @prop {ReactNode} actions - 右侧操作区（可选）
 */
const PageHeader = ({ title, icon, actions, className = '' }) => {
  return (
    <div
      className={`mb-6 flex items-center gap-4${className ? ` ${className}` : ''}`}
    >
      <div
        className="h-8 w-1 rounded-sm"
        style={{ background: 'var(--gradient-accent)', boxShadow: 'var(--shadow-glow)' }}
      />
      {icon && (
        <span style={{ color: 'var(--color-accent)', fontSize: 20 }}>{icon}</span>
      )}
      <Title level={2} className="!m-0 tracking-[1px]" style={{ color: 'var(--color-text-primary)' }}>
        {title}
      </Title>
      {actions && <div className="ml-auto">{actions}</div>}
    </div>
  );
};

export default PageHeader;
