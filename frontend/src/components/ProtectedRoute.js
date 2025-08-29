import React, { useContext, useEffect } from 'react';
import { Navigate, useLocation, useParams } from 'react-router-dom';
import { AuthContext } from '../context/AuthContext';
import { ChatContext } from '../context/ChatContext'; // ✨ ChatContext import
import axiosInstance from '../api/axiosInstance'; // ✨ axiosInstance import

// 이 컴포넌트는 자식 컴포넌트(children)를 props로 받습니다.
function ProtectedRoute({ children }) {
    const { user, loading } = useContext(AuthContext);
    const { joinedRooms, joinRoomAndConnect } = useContext(ChatContext);
    const location = useLocation();
    const { roomId } = useParams();
    
    // ✨ URL 직접 접속 처리를 위한 useEffect 추가
    useEffect(() => {
        // 로딩이 끝났고, 로그인 상태이며, 채팅방 URL(/chat/...)로 접속했을 때
        if (!loading && user && roomId) {
            const numericRoomId = Number(roomId);
            // 아직 내가 참여한 방 목록에 이 방이 없다면
            const isMember = joinedRooms.some(room => room.id === numericRoomId);
            if (!isMember) {
                console.log(`URL로 직접 접속: #${numericRoomId} 방에 참여를 시도합니다.`);
                // 서버에 방 정보를 요청하고, 성공 시 방에 참여시킴
                const enterViaUrl = async () => {
                    try {
                        // 공개방이라고 가정하고 비밀번호 없이 입장 시도
                        await axiosInstance.post(`/room/${numericRoomId}/users`, { password: '' });
                        
                        // 방 정보를 다시 가져와서 채팅방 목록에 추가하고 연결
                        const roomResponse = await axiosInstance.get(`/room/list`);
                        const targetRoom = roomResponse.data.find(room => room.id === numericRoomId);
                        if (targetRoom) {
                            joinRoomAndConnect(targetRoom);
                        }
                    } catch (error) {
                        // 비밀번호가 필요하거나 없는 방일 경우
                        console.error("URL을 통한 방 참여 실패:", error);
                        alert(error.response?.data?.message || "방에 입장할 수 없습니다. 로비로 이동합니다.");
                        // 실패 시 로비로 이동시키는 로직은 아래 Navigate에서 처리
                    }
                };
                enterViaUrl();
            }
        }
    }, [loading, user, roomId, joinedRooms, joinRoomAndConnect]); // 의존성 배열
    
    // 1. AuthContext가 세션 확인을 하는 동안 잠시 기다립니다.
    if (loading) {
        // 로딩 중임을 나타내는 UI를 보여줄 수 있습니다. (예: 스피너)
        return <div>Loading...</div>;
    }

    // 2. 로딩이 끝났는데 로그인한 유저가 없다면, 로비 페이지로 보냅니다.
    if (!user) {
        // 'replace' 옵션은 브라우저 히스토리에 현재 경로를 남기지 않아,
        // 뒤로가기 버튼을 눌렀을 때 다시 이 페이지로 돌아오는 것을 방지합니다.
        return <Navigate to="/" state={{ from: location }} replace />;
    }

    // 3. 로그인한 유저가 있다면, 원래 보여주려던 페이지(children)를 그대로 보여줍니다.
    return children;
}

export default ProtectedRoute;