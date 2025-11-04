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
  const [isLoginModalOpen, setIsLoginModalOpen] = useState(false);
  const [isRegisterModalOpen, setIsRegisterModalOpen] = useState(false);
  const [isMyProfileModalOpen, setIsMyProfileModalOpen] = useState(false);
  const [loading, setLoading] = useState(true);
  
  const [friends, setFriends] = useState([]); // 친구 목록 상태
  const [friendModalConfig, setFriendModalConfig] = useState({
        isOpen: false,
        title: '친구 목록', // 모달의 제목
        onFriendClick: null, // 친구를 클릭했을 때 실행할 함수
        position: { top: 0, left: 0 } // ✨ config 객체에 position을 포함
  });
  const [isUserProfileModalOpen, setIsUserProfileModalOpen] = useState(false);
  const [selectedProfile, setSelectedProfile] = useState(null);
  const [modalPosition, setModalPosition] = useState({ top: 0, left: 0 });
  
  const stompClientRef = useRef(null);
  
  const openLoginModal = () => setIsLoginModalOpen(true);
  const closeLoginModal = () => setIsLoginModalOpen(false);
  const openRegisterModal = () => setIsRegisterModalOpen(true);
  const closeRegisterModal = () => setIsRegisterModalOpen(false);
  const openProfileModal = () => setIsMyProfileModalOpen(true);
  const closeProfileModal = () => setIsMyProfileModalOpen(false);
  const closeFriendListModal = useCallback(() => { // 👈 useCallback으로 감싸기
      setFriendModalConfig({ isOpen: false, title: '친구 목록', onFriendClick: null });
  }, []);
    // 👈 변경: 함수 이름을 toggleFriendListModal로 바꾸고 토글 로직 추가
    const toggleFriendListModal = useCallback(({ title, onFriendClick, position }) => {
        // 만약 모달이 이미 열려 있다면, 닫기만 함
        if (friendModalConfig.isOpen) {
            closeFriendListModal();
        } else { // 모달이 닫혀 있다면, 열기
            setFriendModalConfig({
                isOpen: true,
                title: title || '친구 목록',
                onFriendClick: onFriendClick,
                position: position // 여기에 저장!
            });
        }
    }, [friendModalConfig.isOpen, closeFriendListModal]); // 의존성 배열 추가
    
    // ✨ 신규/변경: UserProfileModal을 여닫는 함수
    const openUserProfileModal = (profileData, position) => {
        setSelectedProfile(profileData);
        if (position) {
            setModalPosition(position);
        }
        setIsUserProfileModalOpen(true);
    };
    
    const closeUserProfileModal = () => {
        setIsUserProfileModalOpen(false);
        setSelectedProfile(null);
    };
    
  // 앱이 처음 시작될 때 세션을 확인하는 로직
  useEffect(() => {
    const checkSession = async () => {
      try {
          // ✨ 수정 후 axiosInstance 코드
          const response = await axiosInstance.get('/auth/session');
            // axios는 reponse.ok 체크가 내장되어 있고, json() 변환도 자동으로 해줍니다.
            // 상태 코드가 2xx가 아니면 자동으로 catch 블록으로 에러를 던집니다.
          setUser(response.data);
      } catch (error) {
        console.error('Session check failed:', error);
      } finally {
          setLoading(false); // ✅ 2. 세션 확인이 끝나면 로딩 상태를 false로 변경
      }
    };
    checkSession();
  }, []); // 컴포넌트가 처음 마운트될 때 한 번만 실행
    
  // 👇 친구 삭제 함수 추가
  const removeFriend = async (friendId) => {
    if (!window.confirm("정말로 친구를 삭제하시겠습니까?")) {
        return;
    }
    try {
        await axiosInstance.delete(`/api/friends/${friendId}`);
        // 상태에서 삭제된 친구를 제거
        setFriends(prevFriends => prevFriends.filter(f => f.userId !== friendId));
        alert("친구를 삭제했습니다.");
    } catch (error) {
        console.error("Failed to remove friend:", error);
        alert("친구 삭제에 실패했습니다.");
    }
  };

  const login = async (username, password) => {
    try {
        const response = await axiosInstance.post('/auth/login', { username, password });
        setUser(response.data);
        closeLoginModal();
        
    } catch (error) {
      console.error('Login failed:', error);
      alert('로그인 중 오류가 발생했습니다.');
    }
  };

  const logout = async () => {
    try {
      await axiosInstance.post('/auth/logout');
    } finally {
      setUser(null);
    }
  };
    
    // ✨ 1. 연결 끊김 시 호출될 강제 로그아웃 함수를 새로 만듭니다.
    const forceLogout = useCallback(() => {
        if (window.location.pathname === '/') {
            return;
        }
        
        // 이미 로그아웃 상태이면 아무것도 하지 않음
        if (!user) return;
        
        console.error("서버와의 모든 연결이 끊어져 강제 로그아웃됩니다.");
        toast.error("서버와의 연결이 끊겼습니다. 다시 로그인해주세요.");
        
        // 기존 로그아웃 함수를 호출하여 상태를 정리
        logout();
        window.location.href = '/';
        
    }, [user]); // user 상태에 의존
  
    // ✅ 회원가입 함수 추가
    const register = async (username, nickname, password) => {
        try {
            const response = await axiosInstance.post(`/auth/register`, { username, nickname, password });
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
            await axiosInstance.delete(`/auth/delete`);
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

    const isAdmin = user?.userId === 0;

    // Context로 전달할 값들
    const value = {
        isAdmin, // 추가
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
        friendModalConfig,
        toggleFriendListModal,
        closeFriendListModal,
        friends, // friends 상태 전달
        setFriends, // setFriends 함수 전달
        removeFriend, // removeFriend 함수 전달
        isUserProfileModalOpen,
        selectedProfile,
        modalPosition,
        openUserProfileModal,
        closeUserProfileModal,
        forceLogout,
    };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export { AuthContext, AuthProvider };