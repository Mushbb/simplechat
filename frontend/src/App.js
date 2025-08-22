import React, { useContext } from 'react';
import './App.css';
import Topbar from './components/Topbar';
import AuthModal from './components/AuthModal';
import { AuthContext } from './context/AuthContext';
import { Routes, Route } from 'react-router-dom';
import LobbyPage from './components/LobbyPage';
import ChatPage from './components/ChatPage';
import RegisterModal from './components/RegisterModal';
import MyProfileModal from "./components/MyProfileModal";
import FriendListModal from './components/FriendListModal';
import ProtectedRoute from './components/ProtectedRoute';

function App() {
    const { isLoginModalOpen, isRegisterModalOpen, isMyProfileModalOpen, friendModalConfig } = useContext(AuthContext);

  return (
    <div className="app-container">
        {/* 모달 렌더링 */}
        {isLoginModalOpen && <AuthModal />}
        {isRegisterModalOpen && <RegisterModal />}
        {isMyProfileModalOpen && <MyProfileModal />}
      
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
    </div>
  );
}

export default App;
