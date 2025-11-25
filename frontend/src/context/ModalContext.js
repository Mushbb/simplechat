import React, { createContext, useState, useCallback } from 'react';

/**
 * @file 애플리케이션 전역의 모든 모달(Modal) 상태를 관리하고,
 * 모달을 열고 닫는 함수를 제공하는 컨텍스트입니다.
 */

/**
 * @typedef {object} Position
 * @property {number} top
 * @property {number} left
 */

/**
 * @typedef {object} FriendModalConfig
 * @property {boolean} isOpen - 친구 목록 모달의 열림 상태.
 * @property {string} title - 모달의 제목.
 * @property {Function | null} onFriendClick - 친구 클릭 시 실행될 콜백 함수.
 * @property {Position | undefined} position - 모달의 위치.
 */

/**
 * @typedef {object} ModalContextType
 * @property {boolean} isLoginModalOpen
 * @property {boolean} isRegisterModalOpen
 * @property {boolean} isMyProfileModalOpen
 * @property {boolean} isUserProfileModalOpen
 * @property {object | null} selectedProfile - 사용자 프로필 모달에 표시할 프로필 데이터.
 * @property {Position} modalPosition - 프로필 모달의 위치.
 * @property {FriendModalConfig} friendModalConfig - 친구 목록 모달의 설정 객체.
 * @property {() => void} openLoginModal
 * @property {() => void} closeLoginModal
 * @property {() => void} openRegisterModal
 * @property {() => void} closeRegisterModal
 * @property {() => void} openProfileModal
 * @property {() => void} closeProfileModal
 * @property {(profileData: object, position?: Position) => void} openUserProfileModal
 * @property {() => void} closeUserProfileModal
 * @property {(config: { title?: string, onFriendClick?: Function, position?: Position }) => void} toggleFriendListModal
 * @property {() => void} closeFriendListModal
 */

/**
 * 모달 컨텍스트 객체입니다.
 * @type {React.Context<ModalContextType>}
 */
const ModalContext = createContext();

/**
 * 모달 관련 상태와 제어 함수를 제공하는 React 컴포넌트입니다.
 * @param {object} props
 * @param {React.ReactNode} props.children - 이 Provider가 감쌀 자식 컴포넌트들.
 * @returns {JSX.Element} ModalContext.Provider
 */
function ModalProvider({ children }) {
    // --- State Definitions ---

    // 인증 모달 상태
    const [isLoginModalOpen, setIsLoginModalOpen] = useState(false);
    const [isRegisterModalOpen, setIsRegisterModalOpen] = useState(false);

    // 프로필 모달 상태
    const [isMyProfileModalOpen, setIsMyProfileModalOpen] = useState(false);
    const [isUserProfileModalOpen, setIsUserProfileModalOpen] = useState(false);
    const [selectedProfile, setSelectedProfile] = useState(null);
    const [modalPosition, setModalPosition] = useState({ top: 0, left: 0 });

    // 친구 목록 모달 상태
    const [friendModalConfig, setFriendModalConfig] = useState({
        isOpen: false,
        title: '친구 목록',
        onFriendClick: null,
        position: { top: 0, left: 0 }
    });

    // --- Modal Control Functions ---

    /** 로그인 모달을 엽니다. */
    const openLoginModal = () => setIsLoginModalOpen(true);
    /** 로그인 모달을 닫습니다. */
    const closeLoginModal = () => setIsLoginModalOpen(false);
    /** 회원가입 모달을 엽니다. */
    const openRegisterModal = () => setIsRegisterModalOpen(true);
    /** 회원가입 모달을 닫습니다. */
    const closeRegisterModal = () => setIsRegisterModalOpen(false);

    /** 내 프로필 모달을 엽니다. */
    const openProfileModal = () => setIsMyProfileModalOpen(true);
    /** 내 프로필 모달을 닫습니다. */
    const closeProfileModal = () => setIsMyProfileModalOpen(false);

    /**
     * 다른 사용자의 프로필 모달을 엽니다.
     * @param {object} profileData - 표시할 사용자의 프로필 데이터.
     * @param {Position} [position] - 모달이 표시될 위치.
     */
    const openUserProfileModal = (profileData, position) => {
        setSelectedProfile(profileData);
        if (position) {
            setModalPosition(position);
        }
        setIsUserProfileModalOpen(true);
    };

    /** 다른 사용자의 프로필 모달을 닫습니다. */
    const closeUserProfileModal = () => {
        setIsUserProfileModalOpen(false);
        setSelectedProfile(null);
    };

    /** 친구 목록 모달을 닫습니다. */
    const closeFriendListModal = useCallback(() => {
        setFriendModalConfig({ isOpen: false, title: '친구 목록', onFriendClick: null });
    }, []);

    /**
     * 친구 목록 모달을 토글(열기/닫기)합니다.
     * @param {object} options
     * @param {string} [options.title] - 모달에 표시할 제목.
     * @param {Function} [options.onFriendClick] - 친구 클릭 시 실행될 콜백.
     * @param {Position} [options.position] - 모달이 표시될 위치.
     */
    const toggleFriendListModal = useCallback(({ title, onFriendClick, position }) => {
        if (friendModalConfig.isOpen) {
            closeFriendListModal();
        } else {
            setFriendModalConfig({
                isOpen: true,
                title: title || '친구 목록',
                onFriendClick: onFriendClick,
                position: position
            });
        }
    }, [friendModalConfig.isOpen, closeFriendListModal]);

    const value = {
        isLoginModalOpen,
        isRegisterModalOpen,
        isMyProfileModalOpen,
        isUserProfileModalOpen,
        selectedProfile,
        modalPosition,
        friendModalConfig,
        openLoginModal,
        closeLoginModal,
        openRegisterModal,
        closeRegisterModal,
        openProfileModal,
        closeProfileModal,
        openUserProfileModal,
        closeUserProfileModal,
        toggleFriendListModal,
        closeFriendListModal,
    };

    return (
        <ModalContext.Provider value={value}>
            {children}
        </ModalContext.Provider>
    );
}

export { ModalContext, ModalProvider };
