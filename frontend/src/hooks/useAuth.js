import { useState, useEffect, useCallback } from 'react';
import Cookies from 'js-cookie';
import axiosInstance from '../utils/axios';

const useAuth = () => {
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
        // 清除无效的token
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
      // 确保在登录失败时清除可能存在的无效token
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

  return { 
    userInfo, 
    loading, 
    login, 
    logout, 
    checkAuth,
    isAuthenticated: !!userInfo
  };
};

export default useAuth;
export { useAuth };