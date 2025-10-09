import React from 'react';
import { Navigate, Outlet } from 'react-router-dom';
import Cookies from 'js-cookie';

const PrivateRoute = () => {
  const token = Cookies.get('token');
  
  // 如果有token，则渲染子路由，否则重定向到登录页
  return token ? <Outlet /> : <Navigate to="/login" replace />;
};

export default PrivateRoute;