import { createContext, useContext, useState, useEffect, useCallback, useMemo } from 'react';
import Cookies from 'js-cookie';
import axiosInstance from '../utils/axios';

// 创建 Context
const AuthContext = createContext(null);

// Provider 组件
export function AuthProvider({ children }) {
  const [userInfo, setUserInfo] = useState(null);
  const [loading, setLoading] = useState(true);

  const checkAuth = useCallback(async () => {
    const token = Cookies.get('token');
    if (token) {
      try {
        setLoading(true);
        const response = await axiosInstance.get('/auth/me');
        setUserInfo(response.data);
      } catch (error) {
        console.error('Auth check failed:', error);
        Cookies.remove('token');
        setUserInfo(null);
      } finally {
        setLoading(false);
      }
    } else {
      setLoading(false);
    }
  }, []);

  const login = async (loginData) => {
    try {
      const response = await axiosInstance.post('/auth/login', loginData);
      const { token, user } = response.data;
      
      if (token && user) {
        Cookies.set('token', token, { expires: 7 });
        setUserInfo(user);
        return { success: true, user };
      } else {
        throw new Error('登录响应数据格式不正确');
      }
    } catch (error) {
      console.error('Login failed:', error);
      Cookies.remove('token');
      setUserInfo(null);
      return { success: false, error: error.response?.data?.message || error.message };
    }
  };

  const logout = () => {
    Cookies.remove('token');
    setUserInfo(null);
  };

  useEffect(() => {
    checkAuth();
  }, [checkAuth]);

  const value = useMemo(() => ({
    userInfo,
    loading,
    login,
    logout,
    checkAuth,
    isAuthenticated: !!userInfo
  }), [userInfo, loading, login, logout, checkAuth]);

  return (
    <AuthContext.Provider value={value}>
      {children}
    </AuthContext.Provider>
  );
}

// 自定义 Hook
const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within AuthProvider');
  }
  return context;
};

export default useAuth;
export { useAuth };