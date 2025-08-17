import React, { useContext } from 'react';
import './App.css';
import Topbar from './components/Topbar';
import AuthModal from './components/AuthModal';
import { AuthContext } from './context/AuthContext';
import { Routes, Route } from 'react-router-dom'; // useParams는 이제 ChatPage에서만 쓰므로 여기서 삭제
import LobbyPage from './components/LobbyPage';
import ChatPage from './components/ChatPage'; // 1. 실제 ChatPage 컴포넌트를 import 합니다.
import RegisterModal from './components/RegisterModal';
import MyProfileModal from "./components/MyProfileModal"; // RegisterModal import
import ProtectedRoute from './components/ProtectedRoute';

// --- 페이지 컴포넌트 (임시) ---
// 이제 모든 페이지가 실제 파일로 분리되었으므로 임시 코드는 모두 삭제합니다.
// --------------------------------

function App() {
    const { isLoginModalOpen, isRegisterModalOpen, isMyProfileModalOpen } = useContext(AuthContext);

  return (
    <div className="app-container">
        {isLoginModalOpen && <AuthModal />}
        {isRegisterModalOpen && <RegisterModal />} {/* ✅ 회원가입 모달 렌더링 */}.
        {isMyProfileModalOpen && <MyProfileModal />}
      
      <Topbar />
      <main className="main-content">
        <Routes>
          {/* 2. 각 Route의 element가 이제 실제 컴포넌트를 가리킵니다. */}
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
