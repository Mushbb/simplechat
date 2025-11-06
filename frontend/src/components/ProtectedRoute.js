import React, { useContext, useEffect } from 'react';
import { Navigate, useLocation, useParams } from 'react-router-dom';
import { AuthContext } from '../context/AuthContext';
import { RoomContext } from '../context/RoomContext'; // Import RoomContext
import axiosInstance from '../api/axiosInstance';

function ProtectedRoute({ children }) {
    const { user, loading } = useContext(AuthContext);
    const { joinedRooms, joinRoomAndConnect } = useContext(RoomContext); // Get from RoomContext
    const location = useLocation();
    const { roomId } = useParams();
    
    useEffect(() => {
        if (!loading && user && roomId) {
            const numericRoomId = Number(roomId);
            const isMember = joinedRooms?.some(room => room.id === numericRoomId); // Optional chaining
            if (!isMember) {
                console.log(`URL로 직접 접속: #${numericRoomId} 방에 참여를 시도합니다.`);
                const enterViaUrl = async () => {
                    try {
                        await axiosInstance.post(`/room/${numericRoomId}/users`, { password: '' });
                        const roomResponse = await axiosInstance.get(`/room/list`);
                        const targetRoom = roomResponse.data.find(room => room.id === numericRoomId);
                        if (targetRoom) {
                            joinRoomAndConnect(targetRoom);
                        }
                    } catch (error) {
                        console.error("URL을 통한 방 참여 실패:", error);
                        alert(error.response?.data?.message || "방에 입장할 수 없습니다. 로비로 이동합니다.");
                    }
                };
                enterViaUrl();
            }
        }
    }, [loading, user, roomId, joinedRooms, joinRoomAndConnect]);
    
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