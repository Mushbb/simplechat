import React, { createContext, useState, useEffect } from 'react';

const AuthContext = createContext();

function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [isModalOpen, setIsModalOpen] = useState(false);

  // 앱이 처음 시작될 때 세션을 확인하는 로직
  useEffect(() => {
    const checkSession = async () => {
      try {
        const response = await fetch('/auth/session', {
          credentials: 'include',
        });
        if (response.ok) {
          const userData = await response.json();
          setUser(userData);
        }
      } catch (error) {
        console.error('Session check failed:', error);
      }
    };
    checkSession();
  }, []); // 컴포넌트가 처음 마운트될 때 한 번만 실행

  const openModal = () => setIsModalOpen(true);
  const closeModal = () => setIsModalOpen(false);

  const login = async (username, password) => {
    try {
      const response = await fetch('/auth/login', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        credentials: 'include',
        body: JSON.stringify({ username, password }),
      });

      if (response.ok) {
        const userData = await response.json();
        setUser(userData);
        closeModal();
      } else {
        const errorData = await response.json();
        alert(errorData.message || '로그인에 실패했습니다.');
      }
    } catch (error) {
      console.error('Login failed:', error);
      alert('로그인 중 오류가 발생했습니다.');
    }
  };

  const logout = async () => {
    try {
      await fetch('/auth/logout', { method: 'POST', credentials: 'include' });
    } finally {
      setUser(null);
    }
  };

  const value = { user, login, logout, isModalOpen, openModal, closeModal };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export { AuthContext, AuthProvider };