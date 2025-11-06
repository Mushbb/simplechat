import React, { createContext, useState, useEffect, useRef, useCallback } from 'react';
import axiosInstance from '../api/axiosInstance';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { toast } from 'react-toastify';                  // ✨ 신규: toast 함수 import
import NotificationToast from '../components/NotificationToast'; // ✨ 신규: 방금 만든 컴포넌트 import
const SERVER_URL = axiosInstance.getUri();

const AuthContext = createContext();

function AuthProvider({ children, navigate }) {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const checkSession = async () => {
      try {
          const response = await axiosInstance.get('/auth/session');
          setUser(response.data);
      } catch (error) {
        console.error('Session check failed:', error);
      } finally {
          setLoading(false);
      }
    };
    checkSession();
  }, []);

  const login = async (username, password) => {
    try {
        const response = await axiosInstance.post('/auth/login', { username, password });
        setUser(response.data);
    } catch (error) {
      console.error('Login failed:', error);
      alert('로그인 중 오류가 발생했습니다.');
      throw error; // Re-throw to allow components to handle it
    }
  };

  const logout = async () => {
    try {
      await axiosInstance.post('/auth/logout');
    } finally {
      setUser(null);
    }
  };

  const forceLogout = useCallback(() => {
      if (window.location.pathname === '/') {
          return;
      }
      if (!user) return;
      console.error("서버와의 모든 연결이 끊어져 강제 로그아웃됩니다.");
      toast.error("서버와의 연결이 끊겼습니다. 다시 로그인해주세요.");
      logout();
      window.location.href = '/';
  }, [user]);

  const register = async (username, nickname, password) => {
      try {
          const response = await axiosInstance.post(`/auth/register`, { username, nickname, password });
          setUser(response.data);
          // Note: closeRegisterModal will be called from the component
          alert('회원가입 성공! 환영합니다.');
      } catch (error) {
          console.error('Register failed:', error);
          alert(error.response?.data?.message || '회원가입에 실패했습니다.');
          throw error;
      }
  };

  const deleteAccount = async () => {
      if (!window.confirm('정말로 계정을 삭제하시겠습니까? 이 작업은 되돌릴 수 없습니다.')) {
          return;
      }
      try {
          await axiosInstance.delete(`/auth/delete`);
          setUser(null);
          alert('계정이 성공적으로 삭제되었습니다.');
      } catch (error) {
          console.error('Delete account failed:', error);
          alert(error.response?.data?.message || '계정 삭제에 실패했습니다.');
      }
  };

  const updateUser = async (nickname, statusMessage, imageFile) => {
      try {
          const profileUpdateResponse = await axiosInstance.put('/user/profile', { nickname, statusMessage });
          if (imageFile) {
              const formData = new FormData();
              formData.append('profileImage', imageFile);
              await axiosInstance.post('/user/profile/image', formData);
          }
          setUser(prevUser => ({ ...prevUser, nickname: profileUpdateResponse.data.nickname }));
          alert('프로필이 성공적으로 업데이트되었습니다.');
      } catch (error) {
          console.error('Update profile failed:', error);
          alert(error.response?.data?.message || '프로필 업데이트에 실패했습니다.');
      }
  };

  const isAdmin = user?.userId === 0;

  const value = {
      user,
      loading,
      isAdmin,
      login,
      logout,
      register,
      deleteAccount,
      updateUser,
      forceLogout,
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export { AuthContext, AuthProvider };