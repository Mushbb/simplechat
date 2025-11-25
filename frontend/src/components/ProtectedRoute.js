import React, { useContext, useEffect } from 'react';
import { Navigate, useLocation, useParams } from 'react-router-dom';
import { AuthContext } from '../context/AuthContext';
import { RoomContext } from '../context/RoomContext';
import axiosInstance from '../api/axiosInstance';

/**
 * @file 인증된 사용자만 접근할 수 있는 경로를 보호하는 컴포넌트입니다.
 * 비로그인 사용자는 로그인 페이지로 리디렉션됩니다.
 */

/**
 * 인증이 필요한 라우트를 감싸는 컴포넌트.
 * 사용자가 로그인되어 있지 않으면 홈페이지로 리디렉션합니다.
 * 또한, 사용자가 URL을 통해 채팅방에 직접 접근했을 때 자동으로 방에 참여시키는 로직을 처리합니다.
 * @param {object} props
 * @param {React.ReactNode} props.children - 보호할 라우트의 컴포넌트.
 * @returns {JSX.Element|null} 사용자의 인증 상태에 따라 보호된 컴포넌트, 리디렉션, 또는 로딩 UI를 반환합니다.
 */
function ProtectedRoute({ children }) {
    const { user, loading } = useContext(AuthContext);
    const { joinedRooms, joinRoomAndConnect } = useContext(RoomContext);
    const location = useLocation();
    const { roomId } = useParams();
    
    /**
     * 사용자가 로그인되어 있고, 특정 채팅방 URL로 직접 접근했을 때,
     * 해당 방의 멤버가 아니라면 자동으로 입장을 시도하는 Effect.
     */
    useEffect(() => {
        if (!loading && user && roomId) {
            const numericRoomId = Number(roomId);
            const isMember = joinedRooms?.some(room => room.id === numericRoomId);
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
    
    // AuthContext가 세션 확인을 하는 동안 잠시 기다립니다.
    if (loading) {
        return <div>Loading...</div>;
    }

    // 로딩이 끝났는데 로그인한 유저가 없다면, 홈페이지로 리디렉션합니다.
    if (!user) {
        return <Navigate to="/" state={{ from: location }} replace />;
    }

    // 로그인한 유저가 있다면, 원래 보여주려던 페이지를 그대로 보여줍니다.
    return children;
}

export default ProtectedRoute;