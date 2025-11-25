import React, { createContext, useState, useEffect, useRef, useCallback } from 'react';
import axiosInstance from '../api/axiosInstance';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { toast } from 'react-toastify';
import NotificationToast from '../components/NotificationToast';

/**
 * @file 인증 관련 상태와 함수를 전역적으로 관리하고 제공하는 컨텍스트입니다.
 */

const SERVER_URL = axiosInstance.getUri();

/**
 * 인증 컨텍스트 객체입니다.
 * 이 컨텍스트를 통해 `user`, `loading`, `login`, `logout` 등의 값에 접근할 수 있습니다.
 * @type {React.Context<AuthContextType>}
 */
const AuthContext = createContext();

/**
 * @typedef {object} User
 * @property {number} userId - 사용자의 고유 ID.
 * @property {string} username - 사용자 계정 이름.
 * @property {string} nickname - 사용자의 닉네임.
 */

/**
 * @typedef {object} AuthContextType
 * @property {User|null} user - 현재 로그인된 사용자 정보. 비로그인 시 null.
 * @property {boolean} loading - 세션 확인 등 초기 인증 상태를 로딩 중인지 여부.
 * @property {boolean} isAdmin - 현재 사용자가 관리자인지 여부.
 * @property {(username, password) => Promise<void>} login - 로그인 함수.
 * @property {() => Promise<void>} logout - 로그아웃 함수.
 * @property {(username, nickname, password) => Promise<void>} register - 회원가입 함수.
 * @property {() => Promise<void>} deleteAccount - 회원탈퇴 함수.
 * @property {(nickname, statusMessage, imageFile) => Promise<void>} updateUser - 사용자 프로필 업데이트 함수.
 * @property {() => void} forceLogout - 서버 연결 끊김 등 예외 상황에서 강제 로그아웃을 처리하는 함수.
 */

/**
 * 인증 상태를 제공하는 React 컴포넌트입니다.
 * 애플리케이션의 최상위 레벨을 감싸서 하위 모든 컴포넌트가 인증 상태에 접근할 수 있도록 합니다.
 * @param {object} props
 * @param {React.ReactNode} props.children - 이 Provider가 감쌀 자식 컴포넌트들.
 * @param {Function} props.navigate - 리다이렉션을 위한 navigate 함수 (React Router).
 * @returns {JSX.Element} AuthContext.Provider
 */
function AuthProvider({ children, navigate }) {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    /**
     * 컴포넌트 마운트 시 서버 세션을 확인하여 로그인 상태를 복원합니다.
     */
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

  /**
   * 사용자 로그인을 처리합니다.
   * @param {string} username - 사용자 이름.
   * @param {string} password - 비밀번호.
   * @returns {Promise<void>}
   * @throws {Error} 로그인 실패 시 에러를 다시 던집니다.
   */
  const login = async (username, password) => {
    try {
        const response = await axiosInstance.post('/auth/login', { username, password });
        setUser(response.data);
    } catch (error) {
      console.error('Login failed:', error);
      alert('로그인 중 오류가 발생했습니다.');
      throw error; // 컴포넌트에서 에러를 추가적으로 처리할 수 있도록 다시 던짐
    }
  };

  /**
   * 사용자 로그아웃을 처리합니다.
   * 서버에 로그아웃 요청을 보낸 후, 로컬의 사용자 상태를 초기화합니다.
   * @returns {Promise<void>}
   */
  const logout = async () => {
    try {
      await axiosInstance.post('/auth/logout');
    } finally {
      setUser(null);
    }
  };

  /**
   * 서버와의 연결이 끊기는 등 예외적인 상황에서 강제로 로그아웃을 처리하는 콜백 함수입니다.
   */
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

  /**
   * 신규 사용자 회원가입을 처리합니다.
   * @param {string} username - 사용자 이름.
   * @param {string} nickname - 닉네임.
   * @param {string} password - 비밀번호.
   * @returns {Promise<void>}
   * @throws {Error} 회원가입 실패 시 에러를 다시 던집니다.
   */
  const register = async (username, nickname, password) => {
      try {
          const response = await axiosInstance.post(`/auth/register`, { username, nickname, password });
          setUser(response.data);
          alert('회원가입 성공! 환영합니다.');
      } catch (error) {
          console.error('Register failed:', error);
          alert(error.response?.data?.message || '회원가입에 실패했습니다.');
          throw error;
      }
  };

  /**
   * 현재 로그인된 사용자의 계정을 삭제합니다.
   * @returns {Promise<void>}
   */
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

  /**
   * 사용자 프로필 정보(닉네임, 상태 메시지, 프로필 이미지)를 업데이트합니다.
   * @param {string} nickname - 새 닉네임.
   * @param {string} statusMessage - 새 상태 메시지.
   * @param {File|null} imageFile - 새 프로필 이미지 파일.
   * @returns {Promise<void>}
   */
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

  /**
   * 현재 사용자가 관리자인지 확인합니다. (userId가 0인 경우 관리자로 간주)
   * @type {boolean}
   */
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