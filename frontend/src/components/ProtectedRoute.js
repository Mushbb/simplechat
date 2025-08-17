import React, { useContext } from 'react';
import { Navigate, useLocation } from 'react-router-dom';
import { AuthContext } from '../context/AuthContext';

// 이 컴포넌트는 자식 컴포넌트(children)를 props로 받습니다.
function ProtectedRoute({ children }) {
    const { user, loading } = useContext(AuthContext);
    const location = useLocation();

    // 1. AuthContext가 세션 확인을 하는 동안 잠시 기다립니다.
    if (loading) {
        // 로딩 중임을 나타내는 UI를 보여줄 수 있습니다. (예: 스피너)
        return <div>Loading...</div>;
    }

    // 2. 로딩이 끝났는데 로그인한 유저가 없다면, 로비 페이지로 보냅니다.
    if (!user) {
        // 'replace' 옵션은 브라우저 히스토리에 현재 경로를 남기지 않아,
        // 뒤로가기 버튼을 눌렀을 때 다시 이 페이지로 돌아오는 것을 방지합니다.
        return <Navigate to="/" replace />;
    }

    // 3. 로그인한 유저가 있다면, 원래 보여주려던 페이지(children)를 그대로 보여줍니다.
    return children;
}

export default ProtectedRoute;