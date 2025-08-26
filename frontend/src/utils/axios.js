import axios from 'axios';
import Cookies from 'js-cookie';
import { message } from 'antd';

// Create axios instance
const axiosInstance = axios.create({
  baseURL: process.env.REACT_APP_API_BASE_URL || 'http://localhost:8080',
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
    console.error('API Error:', {
      status: error.response?.status,
      url: error.config?.url,
      message: error.response?.data?.message || error.message,
      data: error.response?.data,
    });

    // Handle common error scenarios
    if (error.response) {
      const { status, data } = error.response;
      
      switch (status) {
        case 401:
          // Unauthorized - clear token and redirect to login
          Cookies.remove('token');
          message.error('Session expired, please login again');
          window.location.href = '/login';
          break;
          
        case 403:
          // Forbidden
          message.error('Access denied');
          break;
          
        case 404:
          // Not found
          message.error('Resource not found');
          break;
          
        case 500:
          // Server error
          message.error('Server error, please try again later');
          break;
          
        default:
          // Other errors
          const errorMessage = data?.message || `Request failed with status ${status}`;
          message.error(errorMessage);
      }
    } else if (error.request) {
      // Network error
      console.error('Network Error:', error.request);
      message.error('Network connection error, please check your connection');
    } else {
      // Other errors
      console.error('Error:', error.message);
      message.error('An unexpected error occurred');
    }

    return Promise.reject(error);
  }
);

export default axiosInstance;