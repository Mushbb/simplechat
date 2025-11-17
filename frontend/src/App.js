import React, { useContext, useEffect, useState } from 'react';
import './App.css';
import './styles/Modals.css';
import { ToastContainer } from 'react-toastify';
import 'react-toastify/dist/ReactToastify.css';
import Topbar from './components/Topbar';
import AuthModal from './components/AuthModal';
import { AuthContext } from './context/AuthContext';
import { ModalContext } from './context/ModalContext';
import { Routes, Route } from 'react-router-dom';
import LobbyPage from './components/LobbyPage';
import ChatPage from './components/ChatPage';
import RegisterModal from './components/RegisterModal';
import MyProfileModal from "./components/MyProfileModal";
import UserProfileModal from './components/UserProfileModal';
import ProtectedRoute from './components/ProtectedRoute';

function App() {
    const { user, loading } = useContext(AuthContext);
    const { isLoginModalOpen, openLoginModal, isRegisterModalOpen, isMyProfileModalOpen, 
        isUserProfileModalOpen, selectedProfile, modalPosition, closeUserProfileModal} = useContext(ModalContext);
    const [initialModalShown, setInitialModalShown] = useState(false);

    useEffect(() => {
        // 로딩이 끝나고, 유저가 없고, 초기 모달이 아직 표시되지 않았다면 로그인 모달을 엽니다.
        if (!loading && !user && !initialModalShown) {
            openLoginModal();
            setInitialModalShown(true); // 모달이 표시되었음을 기록
        }
    }, [loading, user, openLoginModal, initialModalShown]);
    
    return (
    <div className="app-container">
        {/* 모달 렌더링 */}
        {isLoginModalOpen && <AuthModal />}
        {isRegisterModalOpen && <RegisterModal />}
        {isMyProfileModalOpen && <MyProfileModal />}
        {/* ✨ 신규: UserProfileModal을 여기서 중앙 관리합니다. */}
        {isUserProfileModalOpen && (
            <UserProfileModal
                profile={selectedProfile}
                onClose={closeUserProfileModal}
                position={modalPosition}
            />
        )}
      
      <Topbar />
      <main className="main-content">
        <Routes>
          <Route path="/" element={<LobbyPage />} />
            <Route
                path="/chat/:roomId"
                element={
                    <ProtectedRoute>
                        <ChatPage />
                    </ProtectedRoute>
                }
            />
        </Routes>
      </main>
        <ToastContainer
            position="top-left"      // 위치: 좌측 상단
            autoClose={5000}          // 5초 후 자동 닫힘
            hideProgressBar={false}   // 진행바 표시
            newestOnTop={true}        // 새 알림이 위로 올라옴
            closeOnClick              // 클릭하면 닫힘
            rtl={false}
            pauseOnFocusLoss
            draggable
            pauseOnHover
            theme="light"
        />
    </div>
  );
}

export default App;
