import React, { createContext, useContext, useState, useEffect, useMemo } from 'react';
import { ConfigProvider, theme as antTheme } from 'antd';
import { getAntdDarkToken, getAntdLightToken } from './antdTokens';

const ThemeContext = createContext(undefined);

export const ThemeProvider = ({ children }) => {
  const [theme, setTheme] = useState('dark');

  // Load theme from localStorage on mount, or follow system preference
  useEffect(() => {
    const savedTheme = localStorage.getItem('theme');
    if (savedTheme) {
      setTheme(savedTheme);
      document.documentElement.setAttribute('data-theme', savedTheme);
    } else {
      // First visit: detect and follow system preference
      const prefersDark = window.matchMedia('(prefers-color-scheme: dark)').matches;
      const defaultTheme = prefersDark ? 'dark' : 'light';
      setTheme(defaultTheme);
      document.documentElement.setAttribute('data-theme', defaultTheme);
    }
  }, []);

  // Listen for system preference changes in real-time (only when user hasn't manually set)
  useEffect(() => {
    const mediaQuery = window.matchMedia('(prefers-color-scheme: dark)');
    const handleChange = (e) => {
      // Only auto-follow if user hasn't manually set a preference
      if (!localStorage.getItem('theme')) {
        const newTheme = e.matches ? 'dark' : 'light';
        setTheme(newTheme);
        document.documentElement.setAttribute('data-theme', newTheme);
      }
    };
    mediaQuery.addEventListener('change', handleChange);
    return () => mediaQuery.removeEventListener('change', handleChange);
  }, []);

  // Toggle theme function
  const toggleTheme = () => {
    const nextTheme = theme === 'dark' ? 'light' : 'dark';
    setTheme(nextTheme);
    localStorage.setItem('theme', nextTheme);
    document.documentElement.setAttribute('data-theme', nextTheme);
  };

  // Set specific theme
  const setThemeMode = (mode) => {
    setTheme(mode);
    localStorage.setItem('theme', mode);
    document.documentElement.setAttribute('data-theme', mode);
  };

  // Ant Design theme token based on current theme
  const antdToken = useMemo(() => {
    return theme === 'dark' ? getAntdDarkToken() : getAntdLightToken();
  }, [theme]);

  // Context value
  const value = useMemo(() => ({
    theme,
    isDark: theme === 'dark',
    isLight: theme === 'light',
    toggleTheme,
    setThemeMode,
  }), [theme, toggleTheme, setThemeMode]);

  return (
    <ThemeContext.Provider value={value}>
      <ConfigProvider
        theme={{
          algorithm: theme === 'dark' ? antTheme.darkAlgorithm : antTheme.defaultAlgorithm,
          token: antdToken,
          components: {
            Layout: {
              siderBg: 'var(--color-bg-secondary)',
              headerBg: 'var(--color-bg-secondary)',
              bodyBg: 'var(--color-bg-primary)',
              triggerBg: 'var(--color-bg-elevated)',
            },
            Menu: {
              darkItemBg: 'transparent',
              darkItemSelectedBg: 'var(--color-accent-muted)',
              darkItemHoverBg: 'var(--color-accent-muted)',
              darkSubMenuItemBg: 'transparent',
              itemSelectedColor: 'var(--color-accent)',
            },
            Card: {
              colorBgContainer: theme === 'dark' ? '#141820' : '#ffffff',
              colorBorderSecondary: theme === 'dark' ? 'rgba(255,255,255,0.1)' : 'rgba(0,0,0,0.1)',
            },
            Button: {
              primaryShadow: '0 2px 8px rgba(0, 212, 255, 0.3)',
            },
            Input: {
              colorBgContainer: theme === 'dark' ? '#141820' : '#ffffff',
              activeBorderColor: '#00d4ff',
              hoverBorderColor: '#33ddff',
            },
            Select: {
              colorBgContainer: theme === 'dark' ? '#141820' : '#ffffff',
              colorBgElevated: theme === 'dark' ? '#1a1f2e' : '#ffffff',
            },
            Table: {
              colorBgContainer: theme === 'dark' ? '#141820' : '#ffffff',
              headerBg: theme === 'dark' ? '#1a1f2e' : '#f8fafc',
              rowHoverBg: theme === 'dark' ? 'rgba(0, 212, 255, 0.05)' : 'rgba(2, 132, 199, 0.05)',
            },
            Modal: {
              contentBg: theme === 'dark' ? '#141820' : '#ffffff',
              headerBg: theme === 'dark' ? '#141820' : '#ffffff',
            },
            Dropdown: {
              colorBgElevated: theme === 'dark' ? '#1a1f2e' : '#ffffff',
            },
            Popover: {
              colorBgElevated: theme === 'dark' ? '#1a1f2e' : '#ffffff',
            },
          },
        }}
      >
        {children}
      </ConfigProvider>
    </ThemeContext.Provider>
  );
};

export const useTheme = () => {
  const context = useContext(ThemeContext);
  if (context === undefined) {
    throw new Error('useTheme must be used within a ThemeProvider');
  }
  return context;
};

export default ThemeProvider;
