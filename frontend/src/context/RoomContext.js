import React, { createContext, useState, useContext, useEffect, useCallback, useMemo } from 'react';
import { useNavigate } from 'react-router-dom';
import { AuthContext } from './AuthContext';
import axiosInstance from '../api/axiosInstance';

const RoomContext = createContext();

function RoomProvider({ children }) {
    const { user, loading } = useContext(AuthContext);
    const navigate = useNavigate();

    const [rawRooms, setRawRooms] = useState([]); // API로부터 받은 원본 방 목록
    const [activeRoomId, setActiveRoomId] = useState(null);
    const [unreadRooms, setUnreadRooms] = useState(new Set());

    const fetchRooms = useCallback(async () => {
        if (!user) return;
        try {
            const response = await axiosInstance.get('/room/list');
            setRawRooms(response.data);
        } catch (error) {
            console.error('Failed to fetch rooms:', error);
        }
    }, [user]);

    useEffect(() => {
        if (user) {
            fetchRooms();
        }
    }, [user, fetchRooms]);

    const joinedRooms = useMemo(() => rawRooms.filter(room => room.isMember), [rawRooms]);

    const joinRoomAndConnect = useCallback(async (room) => {
        const isAlreadyMember = joinedRooms.some(r => r.id === room.id);
        if (isAlreadyMember) {
            console.log(`[RoomContext] Already a member of room #${room.id}.`);
            return;
        }
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
        rawRooms, // 원본 방 목록
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
