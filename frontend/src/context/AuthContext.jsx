import { useState, useEffect } from 'react';
import api from '../services/api';
import { AuthContext } from './auth-context';

export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const token = localStorage.getItem('token');
    const savedUser = localStorage.getItem('user');

    if (token && savedUser) {
      setUser(JSON.parse(savedUser));
    }
    setLoading(false);

    const markPasswordChangeRequired = () => {
      setUser((current) => {
        if (!current) return current;
        const updatedUser = { ...current, mustChangePassword: true };
        localStorage.setItem('user', JSON.stringify(updatedUser));
        return updatedUser;
      });
    };
    window.addEventListener('fms:password-change-required', markPasswordChangeRequired);
    return () => window.removeEventListener('fms:password-change-required', markPasswordChangeRequired);
  }, []);

  const login = async (credentials) => {
    const res = await api.post('/auth/login', credentials);
    const userData = res.data.result || res.data;

    localStorage.setItem('token', userData.token);
    localStorage.setItem('user', JSON.stringify(userData));

    setUser(userData);
    return userData;
  };

  const register = async (data) => {
    const res = await api.post('/auth/register', data);
    return res.data;
  };

  const changePassword = async (data) => {
    const res = await api.post('/auth/change-password', data);
    const userData = res.data.result || res.data;
    localStorage.setItem('token', userData.token);
    localStorage.setItem('user', JSON.stringify(userData));
    setUser(userData);
    return userData;
  };

  const logout = () => {
    localStorage.removeItem('token');
    localStorage.removeItem('user');
    setUser(null);
  };

  return (
    <AuthContext.Provider value={{ user, login, register, changePassword, logout, loading }}>
      {children}
    </AuthContext.Provider>
  );
};
