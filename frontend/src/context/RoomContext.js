import React, { createContext, useState, useContext, useEffect, useCallback, useMemo } from 'react';
import { useNavigate } from 'react-router-dom';
import { AuthContext } from './AuthContext';
import { ChatContext } from './ChatContext'; // Import ChatContext
import axiosInstance from '../api/axiosInstance';

const RoomContext = createContext();

function RoomProvider({ children }) {
    const { user, loading } = useContext(AuthContext);
    const { usersByRoom } = useContext(ChatContext); // Get usersByRoom from ChatContext
    const navigate = useNavigate();

    const [rawRooms, setRawRooms] = useState([]); // API로부터 받은 원본 방 목록
    const [activeRoomId, setActiveRoomId] = useState(null);
    const [unreadRooms, setUnreadRooms] = useState(new Set());

    // API로부터 방 목록을 가져오는 함수
    const fetchRooms = useCallback(async () => {
        if (!user) return;
        try {
            const response = await axiosInstance.get('/room/list');
            setRawRooms(response.data);
        } catch (error) {
            console.error('Failed to fetch rooms:', error);
        }
    }, [user]);

    // 사용자가 로그인했거나, usersByRoom 상태가 변경될 때 방 목록을 새로고침
    useEffect(() => {
        fetchRooms();
    }, [fetchRooms, usersByRoom]);

    // 원본 방 목록과 실시간 사용자 수를 조합하여 최종 방 목록(rooms)을 생성
    const rooms = useMemo(() => {
        return rawRooms.map(room => ({
            ...room,
            connCount: usersByRoom[room.id]?.filter(u => u.conn === 'CONNECT').length || 0,
        }));
    }, [rawRooms, usersByRoom]);

    const joinedRooms = useMemo(() => rooms.filter(room => room.isMember), [rooms]);

    const joinRoomAndConnect = useCallback(async (room) => {
        const isAlreadyMember = joinedRooms.some(r => r.id === room.id);
        if (isAlreadyMember) {
            console.log(`[RoomContext] Already a member of room #${room.id}.`);
            return;
        }
        // Optimistically add to UI, actual connection is handled by ChatContext
        setRawRooms(prev => [...prev, { ...room, isMember: true }]);
    }, [joinedRooms]);

    const exitRoom = async (roomId) => {
        try {
            await axiosInstance.delete(`/room/${roomId}/users`);
            setRawRooms(prev => prev.filter(room => room.id !== roomId));
            navigate('/');
        } catch (error) {
            console.error("Failed to exit room:", error);
            alert(error.response?.data?.message || "방에서 나가는 데 실패했습니다.");
        }
    };

    const deleteRoom = async (roomId) => {
        try {
            await axiosInstance.delete(`/room/${roomId}`);
            setRawRooms(prev => prev.filter(room => room.id !== roomId));
            navigate('/');
        } catch (error) {
            console.error("Failed to delete room:", error);
            alert(error.response?.data?.message || "방 삭제에 실패했습니다.");
        }
    };

    useEffect(() => {
        if (activeRoomId && unreadRooms.has(activeRoomId)) {
            setUnreadRooms(prev => {
                const newSet = new Set(prev);
                newSet.delete(activeRoomId);
                return newSet;
            });
        }
    }, [activeRoomId, unreadRooms]);

    const value = {
        rooms, // 최종적으로 계산된 방 목록
        joinedRooms, // 참여한 방 목록
        activeRoomId,
        setActiveRoomId,
        unreadRooms,
        setUnreadRooms,
        joinRoomAndConnect,
        exitRoom,
        deleteRoom,
        fetchRooms, // 로비에서 수동으로 새로고침할 수 있도록 전달
    };

    return (
        <RoomContext.Provider value={value}>
            {children}
        </RoomContext.Provider>
    );
}

export { RoomContext, RoomProvider };
