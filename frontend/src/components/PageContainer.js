import React from 'react';
import { Card } from 'antd';
import { useTheme } from '../theme';

/**
 * PageContainer - 统一页面容器组件
 * 提供工业风设计的页面标题区域和内容卡片
 */
const PageContainer = ({
  title,
  icon,
  actions,
  children,
  className = '',
  style = {},
  headerStyle = {},
  bodyStyle = {},
  noCard = false,
}) => {
  const { theme } = useTheme();

  return (
    <div 
      className={`page-container ${className}`}
      style={{
        animation: 'fade-in 0.3s ease-out',
        ...style
      }}
    >
      {/* 页面标题区域 */}
      <div 
        className="page-header"
        style={{
          marginBottom: 16,
          ...headerStyle
        }}
      >
        <div className="page-title-wrapper">
          {/* 装饰线 */}
          <div className="page-title-decoration" />
          
          {/* 标题和图标 */}
          <div className="page-title-content">
            {icon && (
              <span className="page-title-icon" style={{ color: 'var(--color-accent)' }}>
                {icon}
              </span>
            )}
            <h1 className="page-title">{title}</h1>
          </div>
        </div>

        {/* 操作按钮区域 */}
        {actions && (
          <div className="page-actions">
            {actions}
          </div>
        )}
      </div>

      {/* 页面内容区域 */}
      {noCard ? (
        children
      ) : (
        <Card 
          className="page-content-card"
          style={{
            background: 'var(--color-bg-card)',
            border: '1px solid var(--color-border)',
            borderRadius: 8,
          }}
          bodyStyle={{
            padding: '20px',
          }}
        >
          {/* 顶部装饰线 */}
          <div className="card-top-decoration" />
          {children}
        </Card>
      )}

      {/* 样式 */}
      <style>{`
        .page-container {
          width: 100%;
        }

        .page-header {
          display: flex;
          align-items: center;
          justify-content: space-between;
          gap: 16px;
          flex-wrap: wrap;
        }

        .page-title-wrapper {
          display: flex;
          align-items: center;
          gap: 12px;
        }

        .page-title-decoration {
          width: 4px;
          height: 28px;
          background: linear-gradient(
            180deg,
            var(--color-accent) 0%,
            var(--color-accent-hover) 100%
          );
          border-radius: 2px;
          box-shadow: var(--shadow-glow);
        }

        .page-title-content {
          display: flex;
          align-items: center;
          gap: 10px;
        }

        .page-title-icon {
          font-size: 22px;
          display: flex;
          align-items: center;
        }

        .page-title {
          margin: 0 !important;
          font-size: 20px !important;
          font-weight: 600 !important;
          color: var(--color-text-primary) !important;
          letter-spacing: 0.5px;
          line-height: 1.2 !important;
        }

        .page-actions {
          display: flex;
          align-items: center;
          gap: 8px;
          flex-wrap: wrap;
        }

        .page-content-card {
          position: relative;
          overflow: hidden;
          transition: all 0.3s ease;
        }

        .page-content-card:hover {
          border-color: var(--color-accent-muted);
          box-shadow: 0 4px 20px rgba(0, 0, 0, 0.15);
        }

        .card-top-decoration {
          position: absolute;
          top: 0;
          left: 0;
          right: 0;
          height: 3px;
          background: linear-gradient(
            90deg,
            var(--color-accent) 0%,
            var(--color-accent-hover) 50%,
            transparent 100%
          );
          opacity: 0.8;
        }

        @keyframes fade-in {
          from {
            opacity: 0;
            transform: translateY(8px);
          }
          to {
            opacity: 1;
            transform: translateY(0);
          }
        }

        /* 响应式 */
        @media (max-width: 768px) {
          .page-header {
            flex-direction: column;
            align-items: flex-start;
          }

          .page-title {
            font-size: 18px !important;
          }

          .page-title-decoration {
            height: 24px;
          }
        }
      `}</style>
    </div>
  );
};

export default PageContainer;
