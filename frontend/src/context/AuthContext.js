import React, { createContext, useState, useEffect } from 'react';
import axiosInstance from '../api/axiosInstance';

const SERVER_URL = 'http://10.50.131.25:8080';

const AuthContext = createContext();

function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [isLoginModalOpen, setIsLoginModalOpen] = useState(false);
  const [isRegisterModalOpen, setIsRegisterModalOpen] = useState(false);
  const [isMyProfileModalOpen, setIsMyProfileModalOpen] = useState(false);
  const [loading, setLoading] = useState(true);

  // 앱이 처음 시작될 때 세션을 확인하는 로직
  useEffect(() => {
    const checkSession = async () => {
      try {
        const response = await fetch(`${SERVER_URL}/auth/session`, {
          credentials: 'include',
        });
        if (response.ok) {
          const userData = await response.json();
          setUser(userData);
        }
      } catch (error) {
        console.error('Session check failed:', error);
      } finally {
          setLoading(false); // ✅ 2. 세션 확인이 끝나면 로딩 상태를 false로 변경
      }
    };
    checkSession();
  }, []); // 컴포넌트가 처음 마운트될 때 한 번만 실행

  const openLoginModal = () => setIsLoginModalOpen(true);
  const closeLoginModal = () => setIsLoginModalOpen(false);
  const openRegisterModal = () => setIsRegisterModalOpen(true);
  const closeRegisterModal = () => setIsRegisterModalOpen(false);
  const openProfileModal = () => setIsMyProfileModalOpen(true);
  const closeProfileModal = () => setIsMyProfileModalOpen(false);

  const login = async (username, password) => {
    try {
      const response = await fetch(`${SERVER_URL}/auth/login`, {
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
        closeLoginModal();
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
      await fetch(`${SERVER_URL}/auth/logout`, { method: 'POST', credentials: 'include' });
    } finally {
      setUser(null);
        window.location.reload();
    }
  };

    // ✅ 회원가입 함수 추가
    const register = async (username, nickname, password) => {
        try {
            const response = await axiosInstance.post(`${SERVER_URL}/auth/register`, { username, nickname, password });
            setUser(response.data); // 회원가입 성공 시 바로 로그인 처리
            closeRegisterModal();
            alert('회원가입 성공! 환영합니다.');
        } catch (error) {
            console.error('Register failed:', error);
            alert(error.response?.data?.message || '회원가입에 실패했습니다.');
        }
    };

    // ✅ 회원 탈퇴 함수 추가
    const deleteAccount = async () => {
        if (!window.confirm('정말로 계정을 삭제하시겠습니까? 이 작업은 되돌릴 수 없습니다.')) {
            return;
        }
        try {
            await axiosInstance.delete(`${SERVER_URL}/auth/delete`);
            setUser(null); // 로그아웃 처리
            alert('계정이 성공적으로 삭제되었습니다.');
        } catch (error) {
            console.error('Delete account failed:', error);
            alert(error.response?.data?.message || '계정 삭제에 실패했습니다.');
        }
    };

    // ✅ 프로필 업데이트 함수 추가
    const updateUser = async (nickname, statusMessage, imageFile) => {
        try {
            // 1. 닉네임, 상태 메시지 먼저 업데이트
            const profileUpdateResponse = await axiosInstance.put('/user/profile', { nickname, statusMessage });

            // 2. 만약 새 이미지가 선택되었다면, 이미지도 업로드
            if (imageFile) {
                const formData = new FormData();
                formData.append('profileImage', imageFile);
                await axiosInstance.post('/user/profile/image', formData);
            }

            // 3. Context의 user state를 업데이트하여 Topbar 등에 즉시 반영
            setUser(prevUser => ({ ...prevUser, nickname: profileUpdateResponse.data.nickname }));

            alert('프로필이 성공적으로 업데이트되었습니다.');
            closeProfileModal();

        } catch (error) {
            console.error('Update profile failed:', error);
            alert(error.response?.data?.message || '프로필 업데이트에 실패했습니다.');
        }
    };

    // Context로 전달할 값들
    const value = {
        user,
        loading,
        login,
        logout,
        register, // 추가
        deleteAccount, // 추가
        isLoginModalOpen,
        openLoginModal,
        closeLoginModal,
        isRegisterModalOpen, // 추가
        openRegisterModal, // 추가
        closeRegisterModal, // 추가
        isMyProfileModalOpen, // 추가
        openProfileModal, // 추가
        closeProfileModal, // 추가
        updateUser,
    };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export { AuthContext, AuthProvider };