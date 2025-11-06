import React, { createContext, useState, useCallback } from 'react';

const ModalContext = createContext();

function ModalProvider({ children }) {
    // Auth modals
    const [isLoginModalOpen, setIsLoginModalOpen] = useState(false);
    const [isRegisterModalOpen, setIsRegisterModalOpen] = useState(false);

    // Profile modals
    const [isMyProfileModalOpen, setIsMyProfileModalOpen] = useState(false);
    const [isUserProfileModalOpen, setIsUserProfileModalOpen] = useState(false);
    const [selectedProfile, setSelectedProfile] = useState(null);
    const [modalPosition, setModalPosition] = useState({ top: 0, left: 0 });

    // Friend list modal
    const [friendModalConfig, setFriendModalConfig] = useState({
        isOpen: false,
        title: '친구 목록',
        onFriendClick: null,
        position: { top: 0, left: 0 }
    });

    // --- Auth Modal Controls ---
    const openLoginModal = () => setIsLoginModalOpen(true);
    const closeLoginModal = () => setIsLoginModalOpen(false);
    const openRegisterModal = () => setIsRegisterModalOpen(true);
    const closeRegisterModal = () => setIsRegisterModalOpen(false);

    // --- Profile Modal Controls ---
    const openProfileModal = () => setIsMyProfileModalOpen(true);
    const closeProfileModal = () => setIsMyProfileModalOpen(false);

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

    // --- Friend List Modal Controls ---
    const closeFriendListModal = useCallback(() => {
        setFriendModalConfig({ isOpen: false, title: '친구 목록', onFriendClick: null });
    }, []);

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
