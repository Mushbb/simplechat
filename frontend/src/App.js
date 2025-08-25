import React, { useContext } from 'react';
import './App.css';
import { ToastContainer } from 'react-toastify';
import 'react-toastify/dist/ReactToastify.css';
import Topbar from './components/Topbar';
import AuthModal from './components/AuthModal';
import { AuthContext } from './context/AuthContext';
import { Routes, Route } from 'react-router-dom';
import LobbyPage from './components/LobbyPage';
import ChatPage from './components/ChatPage';
import RegisterModal from './components/RegisterModal';
import MyProfileModal from "./components/MyProfileModal";
import UserProfileModal from './components/UserProfileModal';
import ProtectedRoute from './components/ProtectedRoute';

function App() {
    const { isLoginModalOpen, isRegisterModalOpen, isMyProfileModalOpen, friendModalConfig,
        isUserProfileModalOpen, selectedProfile, modalPosition, closeUserProfileModal} = useContext(AuthContext);

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
