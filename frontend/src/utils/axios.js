import axios from 'axios';
import Cookies from 'js-cookie';
import { message } from 'antd';

// Create axios instance
const axiosInstance = axios.create({
  baseURL: process.env.REACT_APP_API_BASE_URL || '/api',
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Request interceptor
axiosInstance.interceptors.request.use(
  (config) => {
    const token = Cookies.get('token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    
    // Log request for debugging
    console.log('API Request:', {
      method: config.method?.toUpperCase(),
      url: config.url,
      baseURL: config.baseURL,
      headers: config.headers,
    });
    
    return config;
  },
  (error) => {
    console.error('Request Error:', error);
    return Promise.reject(error);
  }
);

// Response interceptor
axiosInstance.interceptors.response.use(
  (response) => {
    // Log successful response for debugging
    console.log('API Response:', {
      status: response.status,
      url: response.config.url,
      data: response.data,
    });
    
    return response;
  },
  (error) => {
    const originalRequest = error.config;
    
    console.error('API Error:', {
      status: error.response?.status,
      url: error.config?.url,
      message: error.response?.data?.message || error.message,
      data: error.response?.data,
    });

    // Handle common error scenarios - but don't display messages here
    // Let individual components handle error messages to avoid duplicates
    if (error.response) {
      const { status, data } = error.response;
      
      switch (status) {
        case 401:
          // Unauthorized - clear token except for login and auth check endpoints
          if (!originalRequest.url?.includes('/auth/login') && 
              !originalRequest.url?.includes('/auth/me')) {
            Cookies.remove('token');
            // Don't show message here, let component handle it
            // message.error('登录已过期，请重新登录');
            // 重定向到登录页面
            window.location.href = '/login';
          }
          break;
          
        case 403:
          // Forbidden - handled by components
          break;
          
        case 404:
          // Not found - handled by components
          break;
          
        case 500:
          // Server error - handled by components
          break;
          
        default:
          // Other errors - handled by components
          break;
      }
    } else if (error.request) {
      // Network error - handled by components
      console.error('Network Error:', error.request);
    } else {
      // Other errors - handled by components
      console.error('Error:', error.message);
    }

    return Promise.reject(error);
  }
);

export default axiosInstance;