import React, { createContext, useState, useEffect, useRef } from 'react';
import axiosInstance from '../api/axiosInstance';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

const SERVER_URL = 'http://10.50.131.25:8080';

const AuthContext = createContext();

function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [isLoginModalOpen, setIsLoginModalOpen] = useState(false);
  const [isRegisterModalOpen, setIsRegisterModalOpen] = useState(false);
  const [isMyProfileModalOpen, setIsMyProfileModalOpen] = useState(false);
  const [loading, setLoading] = useState(true);
  const [notifications, setNotifications] = useState([]);
  
  const [friends, setFriends] = useState([]); // 친구 목록 상태
  const [friendModalConfig, setFriendModalConfig] = useState({
        isOpen: false,
        title: '친구 목록', // 모달의 제목
        onFriendClick: null, // 친구를 클릭했을 때 실행할 함수
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
  const openFriendListModal = ({ title, onFriendClick }) => {
        setFriendModalConfig({
            isOpen: true,
            title: title || '친구 목록', // 제목이 없으면 기본값 사용
            onFriendClick: onFriendClick,
        });
  };
  const closeFriendListModal = () => {
        setFriendModalConfig({ isOpen: false, title: '친구 목록', onFriendClick: null });
  };
    const openUserProfileModal = (profileData, position) => {
        setSelectedProfile(profileData);
        setModalPosition(position);
        setIsUserProfileModalOpen(true);
    };
    
    const closeUserProfileModal = () => {
        setIsUserProfileModalOpen(false);
        setSelectedProfile(null);
    };
    
    // 알림용 웹소켓 연결 Effect
    useEffect(() => {
        if (user) {
            // 1. 초기 친구 요청 목록 가져오기
            axiosInstance.get('/api/friends/requests/pending')
                .then(response => setNotifications(response.data))
                .catch(error => console.error('Failed to fetch pending requests', error));
            
            // 2. 웹소켓 연결
            const socket = new SockJS(`${SERVER_URL}/ws`);
            const stompClient = new Client({
                webSocketFactory: () => socket,
                onConnect: () => {
                    // 3. 사용자 전용 알림 채널 구독
                    stompClient.subscribe(`/user/queue/notifications`, (message) => {
                        const notification = JSON.parse(message.body);
                        
                        if (notification.type === 'FRIEND_REQUEST') {
                            const friendRequest = notification.payload;
                            setNotifications(prev =>
                                prev.find(n => n.userId === friendRequest.userId) ? prev : [...prev, friendRequest]
                            );
                        } else if (notification.type === 'PRESENCE_UPDATE') {
                            const { userId, isOnline } = notification.payload;
                            
                            // setFriends 함수를 사용해 친구 목록의 특정 친구 상태만 업데이트
                            setFriends(prevFriends =>
                                prevFriends.map(friend =>
                                    friend.userId === userId
                                        ? { ...friend, conn: isOnline ? 'CONNECT' : 'DISCONNECT' } // ID가 같으면 conn 상태 업데이트
                                        : friend // 다르면 그대로 유지
                                )
                            );
                        }
                    });
                },
            });
            stompClient.activate();
            stompClientRef.current = stompClient;
            
            // 4. 로그아웃 시 연결 해제
            return () => {
                if (stompClient?.active) {
                    stompClient.deactivate();
                }
            };
        } else {
            // 로그아웃 시 알림 비우기
            setNotifications([]);
        }
    }, [user]);
    
    const acceptFriendRequest = async (requesterId) => {
        try {
            await axiosInstance.put(`/api/friends/requests/${requesterId}/accept`);
            alert('친구 요청을 수락했습니다.');
            setNotifications(prev => prev.filter(n => n.userId !== requesterId));
        } catch (error) {
            alert('친구 요청 수락에 실패했습니다.');
        }
    };
    
    const rejectFriendRequest = async (requesterId) => {
        try {
            await axiosInstance.delete(`/api/friends/requests/${requesterId}/reject`);
            alert('친구 요청을 거절했습니다.');
            setNotifications(prev => prev.filter(n => n.userId !== requesterId));
        } catch (error) {
            alert('친구 요청 거절에 실패했습니다.');
        }
    };

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
    }
  };

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
        notifications,
        acceptFriendRequest,
        rejectFriendRequest,
        friendModalConfig,
        openFriendListModal,
        closeFriendListModal,
        friends, // friends 상태 전달
        setFriends, // setFriends 함수 전달
        removeFriend, // removeFriend 함수 전달
        isUserProfileModalOpen,
        selectedProfile,
        modalPosition,
        openUserProfileModal,
        closeUserProfileModal,
    };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export { AuthContext, AuthProvider };